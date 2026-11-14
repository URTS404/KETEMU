-- ============================================================================
--  KETEMU v2 — Row Level Security (jalankan KEDUA, setelah 0001_schema.sql)
--
--  Prinsip:
--    * pengguna biasa hanya melihat laporan publik + laporannya sendiri,
--    * STAFF/ADMIN boleh memoderasi dan melihat seluruh laporan,
--    * audit log tidak bisa diubah/dihapus siapa pun lewat API,
--    * pengguna tidak bisa menaikkan perannya sendiri jadi STAFF/ADMIN.
-- ============================================================================

-- ---------- Fungsi bantu (SECURITY DEFINER agar tidak rekursif) ----------
create or replace function public.current_role_name()
returns public.user_role
language sql
stable
security definer
set search_path = public, auth
as $$
    select role from public.profiles where id = auth.uid();
$$;

create or replace function public.is_staff()
returns boolean
language sql
stable
security definer
set search_path = public, auth
as $$
    select coalesce(public.current_role_name() in ('STAFF', 'ADMIN'), false);
$$;

create or replace function public.is_admin()
returns boolean
language sql
stable
security definer
set search_path = public, auth
as $$
    select coalesce(public.current_role_name() = 'ADMIN', false);
$$;

-- ---------- Penjaga kolom sensitif pada profiles ----------
create or replace function public.guard_profile_columns()
returns trigger
language plpgsql
security definer
set search_path = public, auth
as $$
begin
    if not public.is_admin() then
        -- Peran & status verifikasi hanya boleh diubah admin.
        new.role := old.role;
        new.is_verified := old.is_verified;
        new.reputation := old.reputation;
    end if;
    return new;
end;
$$;

drop trigger if exists profiles_guard_columns on public.profiles;
create trigger profiles_guard_columns
before update on public.profiles
for each row execute function public.guard_profile_columns();

-- ---------- Penjaga kolom status laporan ----------
create or replace function public.guard_report_columns()
returns trigger
language plpgsql
security definer
set search_path = public, auth
as $$
begin
    if not public.is_staff() then
        -- Pemilik hanya boleh mengubah isi laporan, bukan status/arsipnya.
        new.status := old.status;
        new.is_archived := old.is_archived;
        new.owner_id := old.owner_id;
    end if;
    return new;
end;
$$;

drop trigger if exists reports_guard_columns on public.reports;
create trigger reports_guard_columns
before update on public.reports
for each row execute function public.guard_report_columns();

-- ============================================================================
--  Aktifkan RLS
-- ============================================================================
alter table public.profiles      enable row level security;
alter table public.reports       enable row level security;
alter table public.claims        enable row level security;
alter table public.audit_logs    enable row level security;
alter table public.categories    enable row level security;
alter table public.pos_locations enable row level security;

-- ---------- profiles ----------
drop policy if exists profiles_select on public.profiles;
create policy profiles_select on public.profiles
for select to authenticated
using (id = auth.uid() or public.is_staff());

drop policy if exists profiles_update_own on public.profiles;
create policy profiles_update_own on public.profiles
for update to authenticated
using (id = auth.uid())
with check (id = auth.uid());

-- ---------- categories & pos_locations ----------
drop policy if exists categories_select on public.categories;
create policy categories_select on public.categories
for select to authenticated using (true);

drop policy if exists categories_write on public.categories;
create policy categories_write on public.categories
for all to authenticated using (public.is_admin()) with check (public.is_admin());

drop policy if exists pos_select on public.pos_locations;
create policy pos_select on public.pos_locations
for select to authenticated using (true);

drop policy if exists pos_write on public.pos_locations;
create policy pos_write on public.pos_locations
for all to authenticated using (public.is_admin()) with check (public.is_admin());

-- ---------- reports ----------
drop policy if exists reports_select on public.reports;
create policy reports_select on public.reports
for select to authenticated
using (
    deleted_at is null
    and (not is_archived or owner_id = auth.uid() or public.is_staff())
);

drop policy if exists reports_insert_own on public.reports;
create policy reports_insert_own on public.reports
for insert to authenticated
with check (owner_id = auth.uid());

drop policy if exists reports_update on public.reports;
create policy reports_update on public.reports
for update to authenticated
using (owner_id = auth.uid() or public.is_staff())
with check (owner_id = auth.uid() or public.is_staff());

-- ---------- claims ----------
drop policy if exists claims_select on public.claims;
create policy claims_select on public.claims
for select to authenticated
using (claimant_id = auth.uid() or public.is_staff());

drop policy if exists claims_insert_own on public.claims;
create policy claims_insert_own on public.claims
for insert to authenticated
with check (claimant_id = auth.uid());

drop policy if exists claims_decide_staff on public.claims;
create policy claims_decide_staff on public.claims
for update to authenticated
using (public.is_staff())
with check (public.is_staff());

-- ---------- audit_logs (append-only) ----------
drop policy if exists audit_select_staff on public.audit_logs;
create policy audit_select_staff on public.audit_logs
for select to authenticated
using (public.is_staff());

drop policy if exists audit_insert_staff on public.audit_logs;
create policy audit_insert_staff on public.audit_logs
for insert to authenticated
with check (public.is_staff());

-- Sengaja TIDAK ada policy UPDATE/DELETE: audit log tidak bisa ditimpa.

-- ============================================================================
--  Catatan operasional
--  Setelah SQL ini dijalankan, promosikan akun petugas menjadi STAFF:
--    update public.profiles set role = 'STAFF' where email = 'rahmat@umn.ac.id';
-- ============================================================================
