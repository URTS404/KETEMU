-- ============================================================================
--  Promosikan akun staf menjadi STAFF (jalankan setelah akun terdaftar)
--
--  Langkah:
--   1. Di aplikasi Ketemu, daftar dengan  rahmat@umn.ac.id  /  STAFF-001
--      (trigger handle_new_user membuat profil dengan role USER).
--   2. Jalankan skrip ini di SQL Editor Supabase.
--   3. Logout, lalu login lagi sebagai rahmat@umn.ac.id — sekarang sesi Supabase
--      membawa role STAFF sehingga RLS mengizinkan moderasi.
--
--  Aman dijalankan ulang.
-- ============================================================================

update public.profiles
   set role = 'STAFF'
 where email = 'rahmat@umn.ac.id';

-- Verifikasi: harus tampil role = STAFF
select email, full_name, role, is_verified
  from public.profiles
 where email in ('rahmat@umn.ac.id', 'siti.uas@student.umn.ac.id')
 order by role desc;
