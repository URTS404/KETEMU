-- ============================================================================
--  0016 - Kotak masuk notifikasi
--
--  Satu tabel untuk semua pemberitahuan dalam aplikasi:
--    * konfirmasi unggahan laporan (LOST_REPORT_CREATED / FOUND_REPORT_CREATED),
--    * siaran "barang temuan baru" (NEW_FOUND_ITEM) untuk semua pengguna lain,
--    * perubahan status laporan oleh petugas (REPORT_STATUS_CHANGED),
--    * keputusan klaim (CLAIM_APPROVED / CLAIM_REJECTED).
--
--  Aturan visibilitas (sama dengan AppNotification.visibleTo di sisi klien):
--    * recipient_id terisi -> hanya pemilik baris itu yang melihat,
--    * recipient_id NULL   -> siaran; semua pengguna melihat KECUALI aktor
--                             pembuatnya (agar pengunggah tidak diberi tahu
--                             oleh siarannya sendiri).
--
--  Catatan: trigger di bawah mengirim siaran "barang temuan baru" sebagai satu
--  baris PER pengguna (bukan recipient_id NULL) supaya status "sudah dibaca"
--  tetap terpisah untuk tiap orang. Baris NULL hanya dipakai data lama/demo.
--
--  Pembagian tugas dengan aplikasi:
--    * Konfirmasi barang temuan + siaran "barang temuan baru" dibuat trigger
--      reports_notify_found di server, supaya seluruh pengguna menerimanya
--      walau aplikasinya sedang tertutup.
--    * Notifikasi lain menyangkut satu penerima sehingga dikirim dari
--      perangkat (SupabaseNotificationRepository).
--
--  Jalankan SETELAH 0001 (butuh tabel reports) dan 0002 (butuh is_staff).
--  Aman dijalankan ulang.
-- ============================================================================

-- ---------- 1. Tabel ----------
create table if not exists public.notifications (
    id           uuid primary key default gen_random_uuid(),
    recipient_id uuid references public.profiles (id) on delete cascade,
    actor_id     uuid references public.profiles (id) on delete set null,
    type         text not null,
    title        text not null,
    message      text not null default '',
    report_id    uuid references public.reports (id) on delete cascade,
    is_read      boolean not null default false,
    created_at   timestamptz not null default now()
);

-- Kotak masuk selalu dibaca "milik saya, terbaru dahulu".
create index if not exists notifications_recipient_idx
    on public.notifications (recipient_id, created_at desc);
create index if not exists notifications_broadcast_idx
    on public.notifications (created_at desc)
    where recipient_id is null;

alter table public.notifications enable row level security;

-- ---------- 2. Kebijakan akses ----------
drop policy if exists notifications_read on public.notifications;
create policy notifications_read on public.notifications
for select to authenticated
using (
    recipient_id = auth.uid()
    or recipient_id is null
    or public.is_staff()
);

-- Klien hanya boleh mengirim notifikasi atas namanya sendiri; baris untuk
-- pengguna lain (siaran) dibangkitkan trigger di bawah, bukan dari aplikasi.
drop policy if exists notifications_insert on public.notifications;
create policy notifications_insert on public.notifications
for insert to authenticated
with check (actor_id = auth.uid());

-- "Tandai dibaca" hanya untuk penerima langsung; siaran tidak diubah di server.
drop policy if exists notifications_update on public.notifications;
create policy notifications_update on public.notifications
for update to authenticated
using (recipient_id = auth.uid())
with check (recipient_id = auth.uid());


-- Tidak ada policy DELETE: kotak masuk bersifat catatan, dibersihkan hanya
-- bersama laporannya (on delete cascade).

-- ---------- 3. Penjaga kolom: pengguna hanya boleh mengubah is_read ----------
create or replace function public.guard_notification_columns()
returns trigger
language plpgsql
security definer
set search_path = public, auth
as $$
begin
    if public.is_staff() then
        return new;
    end if;

    new.recipient_id := old.recipient_id;
    new.actor_id := old.actor_id;
    new.type := old.type;
    new.title := old.title;
    new.message := old.message;
    new.report_id := old.report_id;
    new.created_at := old.created_at;
    return new;
end;
$$;

drop trigger if exists notifications_guard_columns on public.notifications;
create trigger notifications_guard_columns
before update on public.notifications
for each row execute function public.guard_notification_columns();

-- ---------- 4. Trigger laporan barang temuan ----------
-- SECURITY DEFINER agar bisa menulis baris siaran yang tidak dimiliki satu
-- pengguna (RLS insert hanya mengizinkan actor_id = auth.uid()).
create or replace function public.notify_found_report()
returns trigger
language plpgsql
security definer
set search_path = public, auth
as $$
begin
    -- Konfirmasi untuk pengunggah: laporannya sudah masuk.
    insert into public.notifications (recipient_id, actor_id, type, title, message, report_id)
    values (
        new.owner_id,
        new.owner_id,
        'FOUND_REPORT_CREATED',
        'Laporan barang temuan terkirim',
        'Terima kasih sudah menitipkan "' || new.title ||
            '". Petugas pos akan memverifikasinya.',
        new.id
    );

    -- Siaran ke pengguna lain: ada barang temuan baru. Dibuat satu baris per
    -- pengguna (bukan recipient_id NULL) supaya status "sudah dibaca" tetap
    -- milik masing-masing penerima.
    insert into public.notifications (recipient_id, actor_id, type, title, message, report_id)
    select
        p.id,
        new.owner_id,
        'NEW_FOUND_ITEM',
        'Barang temuan baru: ' || new.title,
        'Ditemukan di ' || coalesce(nullif(new.last_seen_location, ''), 'lingkungan kampus') ||
            '. Buka detail untuk memeriksa apakah ini barang Anda.',
        new.id
      from public.profiles p
     where p.id <> new.owner_id;

    return new;
end;
$$;

drop trigger if exists reports_notify_found on public.reports;
create trigger reports_notify_found
after insert on public.reports
for each row
when (new.type = 'FOUND' and new.deleted_at is null)
execute function public.notify_found_report();

-- ---------- Verifikasi ----------
select 'tabel notifications' as cek,
       case when count(*) = 1 then 'OK' else 'BELUM' end as status
  from information_schema.tables
 where table_schema = 'public' and table_name = 'notifications'
union all
select 'trigger reports_notify_found',
       case when count(*) = 1 then 'OK' else 'BELUM' end
  from pg_trigger where tgname = 'reports_notify_found'
union all
select 'kebijakan RLS notifications', count(*)::text
  from pg_policies where schemaname = 'public' and tablename = 'notifications';

