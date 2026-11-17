-- ============================================================================
--  0015 - Kunci staf (undangan sekali pakai) + penjagaan domain kampus
--
--  Alur yang didukung:
--    A. ADMIN menyiapkan kunci untuk email tertentu (reserved_email). Saat
--       orang itu mendaftar, trigger langsung memberi peran STAFF.
--    B. Pengguna mendaftar biasa (USER), lalu menebus kunci dari layar Akun.
--    C. ADMIN menaikkan akun yang sudah ada dengan menebus salah satu kunci.
--
--  Semua jalur memakai fungsi yang sama: redeem_staff_key().
--  Kunci sekali pakai; admin dapat membuat 10 kunci baru dari panel (kunci
--  lama yang belum terpakai otomatis hangus).
--
--  Jalankan SETELAH 0011 (butuh kolom is_banned & helper) dan 0014.
-- ============================================================================

-- ---------- 1. Tabel kunci ----------
create table if not exists public.staff_keys (
    code            text primary key,
    reserved_email  text,
    note            text,
    created_by      uuid references public.profiles(id) on delete set null,
    created_at      timestamptz not null default now(),
    used_by         uuid references public.profiles(id) on delete set null,
    used_at         timestamptz
);

alter table public.staff_keys enable row level security;

drop policy if exists staff_keys_admin_read on public.staff_keys;
create policy staff_keys_admin_read on public.staff_keys
for select to authenticated
using (public.is_admin());

-- Tidak ada policy INSERT/UPDATE/DELETE: kunci hanya berubah lewat fungsi
-- SECURITY DEFINER di bawah, supaya tidak ada jalan menebus kunci sendiri.

-- ---------- 2. Pembuat kode acak ----------
create or replace function public.random_staff_code()
returns text
language sql
volatile
as $$
    select upper(
        substr(md5(random()::text || clock_timestamp()::text), 1, 4) || '-' ||
        substr(md5(random()::text || clock_timestamp()::text), 5, 4)
    );
$$;

-- ---------- 3. Pembuat 10 kunci baru (khusus ADMIN) ----------
create or replace function public.generate_staff_keys(p_count integer default 10)
returns setof public.staff_keys
language plpgsql
security definer
set search_path = public, auth
as $$
declare
    bobot integer := least(greatest(coalesce(p_count, 10), 1), 50);
begin
    if not public.is_admin() then
        raise exception 'Hanya admin yang boleh membuat kunci staf';
    end if;

    -- Kunci lama yang belum terpakai hangus agar total tetap sesuai kuota.
    delete from public.staff_keys where used_by is null;

    insert into public.staff_keys (code, created_by)
    select public.random_staff_code(), auth.uid()
      from generate_series(1, bobot);

    return query
        select * from public.staff_keys order by created_at desc, code;
end;
$$;

-- ---------- 4. Penebusan kunci ----------
-- p_target null  -> untuk diri sendiri (jalur B)
-- p_target diisi -> hanya ADMIN yang boleh (jalur A/C)
create or replace function public.redeem_staff_key(p_code text, p_target uuid default null)
returns boolean
language plpgsql
security definer
set search_path = public, auth
as $$
declare
    target_id uuid := coalesce(p_target, auth.uid());
    target_email text;
    key_row public.staff_keys;
begin
    if auth.uid() is null then
        return false;
    end if;

    if target_id <> auth.uid() and not public.is_admin() then
        raise exception 'Hanya admin yang boleh menaikkan peran akun lain';
    end if;

    select email into target_email from public.profiles where id = target_id;
    if target_email is null then
        return false;
    end if;

    select * into key_row
      from public.staff_keys
     where upper(code) = upper(trim(p_code))
       and used_by is null
       and (reserved_email is null or lower(reserved_email) = lower(target_email))
       for update;

    if key_row.code is null then
        return false;   -- kode salah / sudah dipakai / bukan untuk email ini
    end if;

    -- Penjaga kolom profiles menolak perubahan role dari non-admin; penanda
    -- sesi ini memberi izin khusus selama fungsi berjalan.
    perform set_config('ketemu.staff_grant', 'on', true);
    update public.profiles set role = 'STAFF' where id = target_id;
    perform set_config('ketemu.staff_grant', 'off', true);

    update public.staff_keys
       set used_by = target_id, used_at = now()
     where code = key_row.code;

    return true;
end;
$$;

revoke all on function public.generate_staff_keys(integer) from public;
grant execute on function public.generate_staff_keys(integer) to authenticated;
revoke all on function public.redeem_staff_key(text, uuid) from public;
grant execute on function public.redeem_staff_key(text, uuid) to authenticated;

-- ---------- 5. Penjaga kolom: izinkan penebusan kunci ----------
create or replace function public.guard_profile_columns()
returns trigger
language plpgsql
security definer
set search_path = public, auth
as $$
begin
    if current_setting('ketemu.staff_grant', true) = 'on' then
        return new;   -- dipanggil oleh redeem_staff_key()
    end if;

    if auth.uid() is not null and not public.is_admin() then
        new.role := old.role;
        new.is_verified := old.is_verified;
        new.reputation := old.reputation;
        new.is_banned := old.is_banned;
    end if;
    return new;
end;
$$;

-- ---------- 6. Pendaftaran: domain kampus wajib + jalur A ----------
create or replace function public.handle_new_user()
returns trigger
language plpgsql
security definer
set search_path = public, auth
as $$
declare
    email_domain text := lower(split_part(coalesce(new.email, ''), '@', 2));
    key_row public.staff_keys;
    assigned_role public.user_role := 'USER'::public.user_role;
begin
    -- Lapis 3 anti-akun palsu: tolak domain di luar kampus, di server.
    if email_domain not in ('umn.ac.id', 'student.umn.ac.id') then
        raise exception 'Email harus memakai domain kampus UMN (@umn.ac.id atau @student.umn.ac.id)';
    end if;

    -- Jalur A: admin sudah menyiapkan kunci untuk email ini.
    select * into key_row
      from public.staff_keys
     where used_by is null
       and reserved_email is not null
       and lower(reserved_email) = lower(coalesce(new.email, ''))
     limit 1;

    if key_row.code is not null then
        assigned_role := 'STAFF'::public.user_role;
    end if;

    insert into public.profiles (id, email, full_name, nim_nidn, phone, role)
    values (
        new.id,
        new.email,
        coalesce(new.raw_user_meta_data ->> 'full_name', split_part(coalesce(new.email, ''), '@', 1)),
        new.raw_user_meta_data ->> 'nim_nidn',
        new.raw_user_meta_data ->> 'phone',
        assigned_role
    )
    on conflict (id) do nothing;

    if key_row.code is not null then
        update public.staff_keys
           set used_by = new.id, used_at = now()
         where code = key_row.code;
    end if;

    return new;
end;
$$;

drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created
after insert on auth.users
for each row execute function public.handle_new_user();

-- ---------- 7. Kunci awal ----------
-- Dibuat lewat SQL Editor (auth.uid() null), jadi tidak memakai cek is_admin().
insert into public.staff_keys (code)
select public.random_staff_code()
  from generate_series(
         1,
         greatest(0, 10 - (select count(*) from public.staff_keys where used_by is null))
       )
on conflict (code) do nothing;

-- ---------- Verifikasi ----------
select 'tabel staff_keys' as cek,
       case when count(*) = 1 then 'OK' else 'BELUM' end as status
  from information_schema.tables
 where table_schema = 'public' and table_name = 'staff_keys'
union all
select 'fungsi redeem_staff_key',
       case when count(*) = 1 then 'OK' else 'BELUM' end
  from pg_proc where proname = 'redeem_staff_key'
union all
select 'kunci tersedia', count(*)::text
  from public.staff_keys where used_by is null;
