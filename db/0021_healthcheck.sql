-- 0021_healthcheck.sql — pemeriksaan menyeluruh, SEKALI JALAN, hanya membaca
--
-- Cara pakai: SQL Editor → New query → tempel seluruh isi berkas ini → Run.
-- Hasilnya SATU tabel (kolom: cek | nilai | status). Tempel tabel itu ke chat.
--
-- Berkas ini TIDAK mengubah apa pun: semua pernyataan di bawah hanya SELECT
-- terhadap katalog PostgreSQL dan tabel aplikasi.

with
-- ── 1. Skrip mana saja yang sudah masuk ─────────────────────────────────────
migrasi as (
    select 'skema inti (reports/claims/audit)' as cek,
           count(*)::text as nilai,
           case when count(*) = 3 then 'OK' else 'BELUM' end as status
    from information_schema.tables
    where table_schema = 'public'
      and table_name in ('reports', 'claims', 'audit_logs')

    union all
    select 'kolom reports.latitude + longitude',
           count(*)::text,
           case when count(*) = 2 then 'OK' else 'BELUM' end
    from information_schema.columns
    where table_schema = 'public' and table_name = 'reports'
      and column_name in ('latitude', 'longitude')

    union all
    select 'kolom profiles.is_banned',
           count(*)::text,
           case when count(*) = 1 then 'OK' else 'BELUM' end
    from information_schema.columns
    where table_schema = 'public' and table_name = 'profiles'
      and column_name = 'is_banned'

    union all
    select 'tabel staff_keys',
           count(*)::text,
           case when count(*) = 1 then 'OK' else 'BELUM' end
    from information_schema.tables
    where table_schema = 'public' and table_name = 'staff_keys'

    union all
    select 'tabel notifications',
           count(*)::text,
           case when count(*) = 1 then 'OK' else 'BELUM' end
    from information_schema.tables
    where table_schema = 'public' and table_name = 'notifications'

    union all
    select 'tabel matches',
           count(*)::text,
           case when count(*) = 1 then 'OK' else 'BELUM' end
    from information_schema.tables
    where table_schema = 'public' and table_name = 'matches'

    union all
    select 'tabel conversations + messages',
           count(*)::text,
           case when count(*) = 2 then 'OK' else 'BELUM' end
    from information_schema.tables
    where table_schema = 'public' and table_name in ('conversations', 'messages')

    union all
    select 'view chat_threads (0020)',
           count(*)::text,
           case when count(*) = 1 then 'OK' else 'BELUM' end
    from information_schema.views
    where table_schema = 'public' and table_name = 'chat_threads'
),

-- ── 2. Trigger penting ──────────────────────────────────────────────────────
trigger_cek as (
    select 'trigger (profil, laporan, klaim, notif, match, konversasi)' as cek,
           count(*)::text as nilai,
           case when count(*) >= 8 then 'OK' else 'KURANG' end as status
    from pg_trigger t
    join pg_class c on c.oid = t.tgrelid
    join pg_namespace n on n.oid = c.relnamespace
    where n.nspname = 'public' and not t.tgisinternal
),

-- ── 3. RLS + jumlah policy ──────────────────────────────────────────────────
rls_cek as (
    select 'tabel tanpa RLS' as cek,
           coalesce(string_agg(c.relname, ', '), 'tidak ada') as nilai,
           case when count(*) = 0 then 'OK' else 'PERIKSA' end as status
    from pg_class c
    join pg_namespace n on n.oid = c.relnamespace
    where n.nspname = 'public' and c.relkind = 'r' and not c.relrowsecurity
),

policy_cek as (
    select 'jumlah policy RLS' as cek,
           count(*)::text as nilai,
           case when count(*) >= 12 then 'OK' else 'KURANG' end as status
    from pg_policies where schemaname = 'public'
),

-- ── 4. Realtime ─────────────────────────────────────────────────────────────
realtime_cek as (
    select 'tabel di publication realtime' as cek,
           coalesce(string_agg(tablename, ', '), 'kosong') as nilai,
           case
               when count(*) filter (where tablename in ('messages', 'conversations')) = 2 then 'OK'
               when count(*) filter (where tablename = 'messages') = 1 then 'SEBAGIAN'
               else 'BELUM'
           end as status
    from pg_publication_tables
    where pubname = 'supabase_realtime' and schemaname = 'public'
),

-- ── 5. Isi tabel (keadaan sekarang) ────────────────────────────────────────
isi as (
    select 'jumlah laporan' as cek, count(*)::text as nilai, '—' as status
    from public.reports
    union all
    select 'laporan siap diambil',
           count(*)::text, '—'
    from public.reports where status = 'SIAP_DIAMBIL' and not is_archived
    union all
    select 'laporan menunggu verifikasi',
           count(*)::text, '—'
    from public.reports where status = 'DILAPORKAN' and not is_archived
    union all
    select 'jumlah klaim', count(*)::text, '—' from public.claims
    union all
    select 'klaim disetujui tanpa kode (harus 0)',
           count(*)::text,
           case when count(*) = 0 then 'OK' else 'PERIKSA' end
    from public.claims
    where status = 'DISETUJUI' and (pickup_code is null or pickup_code = '')
    union all
    select 'jumlah jejak audit', count(*)::text, '—' from public.audit_logs
    union all
    select 'jumlah percakapan', count(*)::text, '—' from public.conversations
    union all
    select 'percakapan tanpa pesan',
           count(*)::text, '—'
    from public.conversations c
    where not exists (select 1 from public.messages m where m.conversation_id = c.id)
    union all
    select 'jumlah pesan', count(*)::text, '—' from public.messages
    union all
    select 'kunci staf tersedia',
           count(*)::text, '—'
    from public.staff_keys where used_at is null
    union all
    select 'kunci staf terpakai',
           count(*)::text, '—'
    from public.staff_keys where used_at is not null
    union all
    select 'akun terdaftar', count(*)::text, '—' from public.profiles
    union all
    select 'akun STAFF/ADMIN',
           count(*)::text, '—'
    from public.profiles where role in ('STAFF', 'ADMIN')
    union all
    select 'akun diblokir',
           count(*)::text, '—'
    from public.profiles where is_banned
),

-- ── 6. Notifikasi & pencocokan ─────────────────────────────────────────────
fitur as (
    select 'notifikasi belum dibaca' as cek, count(*)::text as nilai, '—' as status
    from public.notifications where is_read = false
    union all
    select 'pasangan cocok tersimpan', count(*)::text, '—' from public.matches
),

gabung as (
    select * from migrasi
    union all select * from trigger_cek
    union all select * from rls_cek
    union all select * from policy_cek
    union all select * from realtime_cek
    union all select * from isi
    union all select * from fitur
)

select cek, nilai, status from gabung;
