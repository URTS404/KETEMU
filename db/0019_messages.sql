-- ============================================================================
--  0019 - Pesan dua arah pada satu laporan
--
--  Model percakapan:
--    * Satu percakapan = satu laporan (konteks barang selalu terbawa).
--    * Pihak yang mengobrol: PEMILIK laporan (owner_id, dari reports.owner_id)
--      dan LAWAN bicara (peer_id) — pemilik barang atau penemunya, tergantung
--      jenis laporan:
--        - laporan HILANG : owner = mahasiswa yang kehilangan, peer = penemu
--        - laporan TEMUAN : owner = penemu, peer = pemilik yang mengklaim
--    * Petugas (STAFF/ADMIN) selalu boleh MEMBACA semua percakapan dan boleh
--      ikut menulis (memediasi), tetapi tidak menjadi "pihak" percakapan.
--    * Satu laporan + satu lawan bicara = satu baris (unique), jadi membuka
--      tombol "Chat" berkali-kali tidak membuat utas baru.
--
--  Jalankan SETELAH 0018. Aman dijalankan ulang.
-- ============================================================================

-- ---------- 1. Tabel percakapan ----------
create table if not exists public.conversations (
    id              uuid primary key default gen_random_uuid(),
    report_id       uuid not null references public.reports(id) on delete cascade,
    owner_id        uuid not null references public.profiles(id) on delete cascade,
    peer_id         uuid not null references public.profiles(id) on delete cascade,
    created_at      timestamptz not null default now(),
    last_message_at timestamptz not null default now(),
    unique (report_id, peer_id),
    check (owner_id <> peer_id)
);

create index if not exists conversations_owner_idx on public.conversations (owner_id, last_message_at desc);
create index if not exists conversations_peer_idx  on public.conversations (peer_id, last_message_at desc);

-- ---------- 2. Tabel pesan ----------
create table if not exists public.messages (
    id              uuid primary key default gen_random_uuid(),
    conversation_id uuid not null references public.conversations(id) on delete cascade,
    sender_id       uuid not null references public.profiles(id) on delete cascade,
    body            text not null check (char_length(btrim(body)) between 1 and 2000),
    is_template     boolean not null default false,
    created_at      timestamptz not null default now()
);

create index if not exists messages_conversation_idx on public.messages (conversation_id, created_at);

-- ---------- 3. Helper akses ----------
-- Siapa yang boleh melihat satu percakapan: dua pihak + semua petugas.
create or replace function public.can_access_conversation(p_conversation_id uuid)
returns boolean
language sql
stable
security definer
set search_path = public, auth
as $$
    select exists (
        select 1
          from public.conversations c
         where c.id = p_conversation_id
           and (c.owner_id = auth.uid() or c.peer_id = auth.uid() or public.is_staff())
    );
$$;

revoke all on function public.can_access_conversation(uuid) from public;
grant execute on function public.can_access_conversation(uuid) to authenticated;

-- ---------- 4. RLS ----------
alter table public.conversations enable row level security;
alter table public.messages enable row level security;

drop policy if exists conversations_read on public.conversations;
create policy conversations_read on public.conversations
for select to authenticated
using (owner_id = auth.uid() or peer_id = auth.uid() or public.is_staff());

drop policy if exists conversations_insert on public.conversations;
create policy conversations_insert on public.conversations
for insert to authenticated
with check (
    peer_id = auth.uid()
    and public.not_banned()
    and exists (
        select 1 from public.reports r
         where r.id = report_id
           and r.owner_id <> auth.uid()
           and r.is_archived = false
    )
);

drop policy if exists messages_read on public.messages;
create policy messages_read on public.messages
for select to authenticated
using (public.can_access_conversation(conversation_id));

drop policy if exists messages_insert on public.messages;
create policy messages_insert on public.messages
for insert to authenticated
with check (
    sender_id = auth.uid()
    and public.not_banned()
    and public.can_access_conversation(conversation_id)
);

-- Riwayat tidak boleh diubah/dihapus siapa pun lewat API.
-- (Tidak ada policy UPDATE/DELETE untuk messages.)

-- ---------- 5. Buka percakapan dari aplikasi ----------
-- Mengembalikan id percakapan: yang sudah ada dipakai ulang, baru dibuat bila
-- belum ada. Dijalankan sebagai SECURITY DEFINER agar aplikasi tidak perlu
-- tahu owner_id laporan (cukup id laporan yang memang terlihat olehnya).
create or replace function public.start_conversation(p_report_id uuid)
returns uuid
language plpgsql
security definer
set search_path = public, auth
as $$
declare
    v_owner uuid;
    v_archived boolean;
    v_id uuid;
begin
    if auth.uid() is null then
        raise exception 'Harus masuk terlebih dahulu';
    end if;

    if not public.not_banned() then
        raise exception 'Akun diblokir tidak dapat memulai percakapan';
    end if;

    select owner_id, is_archived into v_owner, v_archived
      from public.reports
     where id = p_report_id;

    if v_owner is null then
        raise exception 'Laporan tidak ditemukan';
    end if;

    if v_archived then
        raise exception 'Laporan sudah diarsipkan';
    end if;

    if v_owner = auth.uid() then
        raise exception 'Ini laporan Anda sendiri';
    end if;

    insert into public.conversations (report_id, owner_id, peer_id)
    values (p_report_id, v_owner, auth.uid())
    on conflict (report_id, peer_id) do update
        set last_message_at = public.conversations.last_message_at
    returning id into v_id;

    return v_id;
end;
$$;

revoke all on function public.start_conversation(uuid) from public;
grant execute on function public.start_conversation(uuid) to authenticated;

-- ---------- 6. Selaraskan waktu pesan terakhir ----------
create or replace function public.touch_conversation()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
    update public.conversations
       set last_message_at = new.created_at
     where id = new.conversation_id;
    return new;
end;
$$;

drop trigger if exists messages_touch_conversation on public.messages;
create trigger messages_touch_conversation
after insert on public.messages
for each row execute function public.touch_conversation();

-- ---------- 7. Realtime ----------
-- Realtime tetap menghormati RLS; tiap pengguna hanya menerima baris yang
-- lolos policy di atas (pihak percakapan + petugas).
do $$
declare
    tabel text;
begin
    foreach tabel in array array['conversations', 'messages'] loop
        if exists (select 1 from pg_publication where pubname = 'supabase_realtime') then
            if not exists (
                select 1
                  from pg_publication_tables
                 where pubname = 'supabase_realtime'
                   and schemaname = 'public'
                   and tablename = tabel
            ) then
                execute format('alter publication supabase_realtime add table public.%I', tabel);
            end if;
        else
            execute format('create publication supabase_realtime for table public.%I', tabel);
        end if;
    end loop;
end $$;

-- ---------- Verifikasi ----------
select 'tabel conversations' as cek,
       case when count(*) = 1 then 'OK' else 'BELUM' end as status
  from information_schema.tables
 where table_schema = 'public' and table_name = 'conversations'
union all
select 'tabel messages',
       case when count(*) = 1 then 'OK' else 'BELUM' end
  from information_schema.tables
 where table_schema = 'public' and table_name = 'messages'
union all
select 'fungsi start_conversation',
       case when count(*) = 1 then 'OK' else 'BELUM' end
  from pg_proc where proname = 'start_conversation'
union all
select 'policy messages', count(*)::text
  from pg_policies where schemaname = 'public' and tablename = 'messages'
union all
select 'realtime messages',
       case when count(*) = 1 then 'OK' else 'BELUM' end
  from pg_publication_tables
 where pubname = 'supabase_realtime' and schemaname = 'public' and tablename = 'messages';
