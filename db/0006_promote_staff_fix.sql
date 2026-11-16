-- ============================================================================
--  0006 - Naikkan akun staf menjadi STAFF (dan izinkan pengelolaan dari server)
--
--  Mengapa perlu: trigger `profiles_guard_columns` mengembalikan kolom `role`
--  ke nilai lama bila pelakunya bukan admin. Di SQL Editor tidak ada JWT
--  pengguna (`auth.uid()` bernilai null), sehingga UPDATE dari dashboard ikut
--  dibatalkan tanpa pesan error — peran tetap USER.
--
--  Perbaikan: penjaga hanya membatasi permintaan yang datang dari aplikasi
--  (ada JWT). Operasi administratif dari server/SQL Editor tetap boleh, karena
--  RLS `profiles` sudah menutup akses anon & pengguna biasa.
--
--  Jalankan seluruh berkas ini sekali jalan di SQL Editor.
-- ============================================================================

create or replace function public.guard_profile_columns()
returns trigger
language plpgsql
security definer
set search_path = public, auth
as $$
begin
    -- auth.uid() null = permintaan administratif (SQL Editor / service role).
    if auth.uid() is not null and not public.is_admin() then
        new.role := old.role;
        new.is_verified := old.is_verified;
        new.reputation := old.reputation;
    end if;
    return new;
end;
$$;

update public.profiles
   set role = 'STAFF'
 where email = 'rahmat@umn.ac.id';

-- Verifikasi: baris rahmat harus ber-role STAFF
select email, full_name, role, is_verified
  from public.profiles
 where email in ('rahmat@umn.ac.id', 'siti.uas@student.umn.ac.id')
 order by role desc;
