-- ============================================================================
--  0017 - Realtime untuk kotak masuk notifikasi
--
--  Aplikasi berlangganan INSERT tabel public.notifications lewat Supabase
--  Realtime, sehingga kabar baru langsung muncul di lonceng Beranda dan panel
--  notifikasi Android tanpa menunggu polling 30 detik (SupabaseNotificationRepository).
--
--  Yang perlu dilakukan di sisi database: mendaftarkan tabel ke publication
--  `supabase_realtime`. Realtime tetap menghormati RLS, jadi tiap pengguna hanya
--  menerima baris yang lolos policy `notifications_read` (0016).
--
--  Di Dashboard Supabase, Realtime untuk project baru sudah aktif secara bawaan.
--  Bila pernah dimatikan, buka Database -> Replication dan aktifkan publication
--  `supabase_realtime` (setara dengan perintah di bawah).
--
--  Jalankan SETELAH 0016. Aman dijalankan ulang.
-- ============================================================================

-- ---------- 1. Daftarkan tabel pada publication Realtime ----------
-- `add table` tidak punya varian IF NOT EXISTS; periksa daftar anggota dulu.
do $$
begin
    if exists (select 1 from pg_publication where pubname = 'supabase_realtime') then
        if not exists (
            select 1
              from pg_publication_tables
             where pubname    = 'supabase_realtime'
               and schemaname = 'public'
               and tablename  = 'notifications'
        ) then
            execute 'alter publication supabase_realtime add table public.notifications';
        end if;
    else
        execute 'create publication supabase_realtime for table public.notifications';
    end if;
end $$;

-- ---------- Verifikasi ----------
select 'tabel notifications pada publication supabase_realtime' as cek,
       case when count(*) = 1 then 'OK' else 'BELUM' end as status
  from pg_publication_tables
 where pubname = 'supabase_realtime'
   and schemaname = 'public'
   and tablename = 'notifications'
union all
select 'RLS notifications aktif',
       case when rowsecurity then 'OK' else 'BELUM' end
  from pg_tables
 where schemaname = 'public' and tablename = 'notifications';
