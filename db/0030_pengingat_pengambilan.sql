-- ============================================================================
--  0030 - Pengingat pengambilan barang di Pos
--
--  Masalah: setelah klaim disetujui, barang menunggu di Pos sampai pemilik
--  datang. Kalau pemilik lupa, tidak ada yang mengingatkan — baik pemilik
--  maupun petugas Pos.
--
--  Aturan yang dipakai:
--    * pengingat ke-1 dikirim 3 hari setelah klaim disetujui,
--    * pengingat ke-2 dikirim 6 hari setelah klaim disetujui,
--    * tidak ada pengingat lagi setelah serah terima tercatat
--      (`claims.handed_over_at` terisi),
--    * setiap tingkat hanya dikirim SEKALI per klaim (dicatat di tabel
--      pickup_reminders supaya menjalankan fungsi berulang kali aman).
--
--  Kenapa di database: pengingat harus tetap terkirim walau aplikasi sedang
--  tertutup. Fungsi dijalankan penjadwal `pg_cron` setiap jam.
--
--  Jalankan SETELAH 0016 (tabel notifications) dan 0028 (kolom serah terima).
--  Aman dijalankan ulang.
-- ============================================================================

-- ---------- 1. Catatan pengingat yang sudah terkirim ----------
create table if not exists public.pickup_reminders (
    claim_id   uuid not null references public.claims (id) on delete cascade,
    level      int  not null check (level between 1 and 2),
    sent_at    timestamptz not null default now(),
    primary key (claim_id, level)
);

alter table public.pickup_reminders enable row level security;

-- Hanya petugas yang perlu melihat catatan ini; pengguna biasa tidak
-- berkepentingan dan notifikasinya sudah tampil di kotak masuk.
drop policy if exists pickup_reminders_staff on public.pickup_reminders;
create policy pickup_reminders_staff on public.pickup_reminders
for select to authenticated
using (public.is_staff());

-- ---------- 2. Fungsi pengirim pengingat ----------
create or replace function public.kirim_pengingat_pengambilan()
returns int
language plpgsql
security definer
set search_path = public
as $$
declare
    baris record;
    jumlah int := 0;
    judul text;
    pesan text;
    kode text;
begin
    for baris in
        select c.id                as claim_id,
               c.claimant_id       as claimant_id,
               c.pickup_code       as pickup_code,
               c.decided_at        as decided_at,
               r.id                as report_id,
               r.title             as judul_barang,
               p.name              as nama_pos,
               case
                   when c.decided_at <= now() - interval '6 days' then 2
                   when c.decided_at <= now() - interval '3 days' then 1
                   else 0
               end                 as level_target
          from public.claims c
          join public.reports r on r.id = c.report_id
          left join public.pos_locations p on p.id = r.pos_id
         where c.status = 'DISETUJUI'
           and c.handed_over_at is null
           and c.decided_at is not null
           and r.status = 'SIAP_DIAMBIL'
           and c.decided_at <= now() - interval '3 days'
    loop
        -- Kirim tingkat yang belum pernah dikirim untuk klaim ini.
        for i in 1..baris.level_target loop
            if exists (
                select 1 from public.pickup_reminders
                 where claim_id = baris.claim_id and level = i
            ) then
                continue;
            end if;

            if i = 1 then
                judul := 'Jangan lupa ambil barangmu';
                pesan := 'Barang "' || baris.judul_barang || '" sudah menunggu di '
                      || coalesce(baris.nama_pos, 'Pos kampus')
                      || '. Tunjukkan kode pengambilan saat mengambil.';
            else
                judul := 'Barangmu masih di Pos';
                pesan := 'Sudah 6 hari barang "' || baris.judul_barang
                      || '" menunggu di ' || coalesce(baris.nama_pos, 'Pos kampus')
                      || '. Kalau bukan kamu, hubungi petugas Pos.';
            end if;

            kode := coalesce(baris.pickup_code, '-');
            if baris.pickup_code is not null then
                pesan := pesan || ' Kode: ' || baris.pickup_code || '.';
            end if;

            insert into public.notifications (recipient_id, type, title, message, report_id)
            values (baris.claimant_id, 'PICKUP_REMINDER', judul, pesan, baris.report_id);

            insert into public.pickup_reminders (claim_id, level)
            values (baris.claim_id, i)
            on conflict do nothing;

            jumlah := jumlah + 1;
        end loop;
    end loop;

    return jumlah;
end $$;

revoke all on function public.kirim_pengingat_pengambilan() from public;
grant execute on function public.kirim_pengingat_pengambilan() to authenticated;

-- ---------- 3. Penjadwalan dengan pg_cron ----------
-- Kalau ekstensi pg_cron belum aktif di project, blok ini dilewati tanpa
-- menggagalkan migrasi (Supabase SQL Editor menjalankannya sebagai satu
-- transaksi, jadi kesalahan di sini tidak boleh membatalkan bagian di atas).
do $$
begin
    if exists (select 1 from pg_available_extensions where name = 'pg_cron') then
        create extension if not exists pg_cron;
    end if;

    if exists (select 1 from pg_extension where extname = 'pg_cron') then
        -- Jam penuh setiap jam; pekerjaan ini murah (hanya menyentuh klaim
        -- yang statusnya DISETUJUI dan belum diambil).
        perform cron.schedule(
            'ketemu-pengingat-pengambilan',
            '0 * * * *',
            $cron$select public.kirim_pengingat_pengambilan();$cron$
        );
    else
        raise notice 'pg_cron tidak tersedia; jalankan select public.kirim_pengingat_pengambilan(); secara manual atau dari cron eksternal';
    end if;
exception when others then
    raise notice 'penjadwalan pg_cron dilewati: %', sqlerrm;
end $$;

-- ---------- Verifikasi ----------
select
    to_regclass('public.pickup_reminders') is not null            as tabel_pengingat_ada,
    exists (
        select 1 from pg_proc
         where proname = 'kirim_pengingat_pengambilan'
    )                                                            as fungsi_pengingat_ada,
    exists (
        select 1 from pg_extension where extname = 'pg_cron'
    )                                                            as pg_cron_aktif;
