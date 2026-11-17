-- ============================================================================
--  0011 - Pemisahan hak akses STAFF vs ADMIN
--
--  Aturan yang ditegakkan di server (bukan hanya di aplikasi):
--    * kolom `is_banned` baru, hanya ADMIN yang boleh mengubahnya;
--    * peran & status verifikasi tetap hanya boleh diubah ADMIN;
--    * ADMIN mendapat izin penuh atas tabel `profiles`;
--    * akun yang diblokir tidak bisa membaca/menulis data apa pun.
--
--  Aman dijalankan ulang.
-- ============================================================================

-- ---------- 1. Kolom blokir akun ----------
alter table public.profiles
    add column if not exists is_banned boolean not null default false;

-- ---------- 2. Penjaga kolom sensitif (versi terbaru) ----------
create or replace function public.guard_profile_columns()
returns trigger
language plpgsql
security definer
set search_path = public, auth
as $$
begin
    -- Operasi dari SQL Editor / service role tidak punya auth.uid(): biarkan,
    -- supaya admin tetap bisa menaikkan peran secara manual.
    if auth.uid() is not null and not public.is_admin() then
        -- Peran, verifikasi, reputasi, dan blokir hanya milik admin.
        new.role := old.role;
        new.is_verified := old.is_verified;
        new.reputation := old.reputation;
        new.is_banned := old.is_banned;
    end if;
    return new;
end;
$$;

drop trigger if exists profiles_guard_columns on public.profiles;
create trigger profiles_guard_columns
before update on public.profiles
for each row execute function public.guard_profile_columns();

-- ---------- 3. Izin penuh profiles untuk ADMIN ----------
drop policy if exists profiles_admin_all on public.profiles;
create policy profiles_admin_all on public.profiles
for all to authenticated
using (public.is_admin())
with check (public.is_admin());

-- ---------- 4. Pembantu: apakah pengguna aktif tidak diblokir ----------
create or replace function public.not_banned()
returns boolean
language sql
stable
security definer
set search_path = public
as $$
    select coalesce(
        (select not is_banned from public.profiles where id = auth.uid()),
        true
    );
$$;

-- ---------- 5. Blokir total untuk akun yang diblokir ----------
-- Policy RESTRICTIVE berlaku di atas policy lain: walau policy izin
-- mengizinkan, akun terblokir tetap tidak mendapat baris apa pun.
drop policy if exists profiles_block_banned on public.profiles;
create policy profiles_block_banned on public.profiles
as restrictive for all to authenticated
using (public.not_banned());

drop policy if exists reports_block_banned on public.reports;
create policy reports_block_banned on public.reports
as restrictive for all to authenticated
using (public.not_banned());

drop policy if exists claims_block_banned on public.claims;
create policy claims_block_banned on public.claims
as restrictive for all to authenticated
using (public.not_banned());

drop policy if exists audit_block_banned on public.audit_logs;
create policy audit_block_banned on public.audit_logs
as restrictive for all to authenticated
using (public.not_banned());

-- ---------- 6. Jenis aksi audit untuk manajemen akun ----------
alter type public.audit_action add value if not exists 'UBAH_PERAN';
alter type public.audit_action add value if not exists 'VERIFIKASI_AKUN';
alter type public.audit_action add value if not exists 'BLOKIR_AKUN';
alter type public.audit_action add value if not exists 'BUKA_BLOKIR_AKUN';

-- ---------- Verifikasi ----------
select 'kolom is_banned' as cek,
       case when count(*) = 1 then 'OK' else 'BELUM' end as status
  from information_schema.columns
 where table_schema = 'public' and table_name = 'profiles' and column_name = 'is_banned'
union all
select 'policy profiles_admin_all',
       case when count(*) = 1 then 'OK' else 'BELUM' end
  from pg_policies where schemaname = 'public' and policyname = 'profiles_admin_all'
union all
select 'fungsi not_banned',
       case when count(*) = 1 then 'OK' else 'BELUM' end
  from pg_proc where proname = 'not_banned'
union all
select 'policy blokir (3 tabel)',
       case when count(*) = 3 then 'OK' else count(*)::text end
  from pg_policies
 where schemaname = 'public'
   and policyname in ('reports_block_banned','claims_block_banned','audit_block_banned');
