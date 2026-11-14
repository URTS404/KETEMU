-- ============================================================================
--  KETEMU v2 — Data awal (jalankan KETIGA)
--  Hanya master data. Laporan & klaim dibuat dari aplikasi agar owner_id
--  mengacu ke pengguna Supabase Auth yang benar-benar ada.
-- ============================================================================

insert into public.categories (id, name) values
    ('DOKUMEN',    'Dokumen'),
    ('ELEKTRONIK', 'Elektronik'),
    ('AKSESORIS',  'Aksesoris'),
    ('LAINNYA',    'Lainnya')
on conflict (id) do update set name = excluded.name;

insert into public.pos_locations (id, name, building) values
    ('pos-basement',  'Pos Basement',  'Basement Gedung Utama'),
    ('pos-gedung-b',  'Pos Gedung B',  'Lobby Gedung B, Lt. 1'),
    ('pos-gedung-c',  'Pos Gedung C',  'Pintu Utama Gedung C'),
    ('pos-gedung-d',  'Pos Gedung D',  'Lobby Gedung D, Lt. 1')
on conflict (id) do update
    set name = excluded.name,
        building = excluded.building;

-- ============================================================================
--  Setelah akun petugas mendaftar lewat aplikasi, jalankan (ganti email):
--    update public.profiles set role = 'STAFF', is_verified = true
--     where email = 'rahmat@umn.ac.id';
--    update public.profiles set role = 'ADMIN', is_verified = true
--     where email = 'admin@umn.ac.id';
-- ============================================================================
