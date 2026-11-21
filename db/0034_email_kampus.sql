-- 0034_email_kampus.sql
--
-- Tujuan
--   Menegakkan syarat "akun hanya untuk warga kampus UMN" di sisi SERVER, bukan
--   hanya di aplikasi. Sebelum ini aturan domain email hanya diperiksa oleh
--   aplikasi Android (BuildConfig.CAMPUS_EMAIL_DOMAIN = "umn.ac.id"), sehingga
--   pendaftaran lewat REST/klien lain bisa memakai email apa pun.
--
--   Verifikasi email TETAP DIMATIKAN (mailer_autoconfirm = true pada Supabase
--   Auth): akun langsung aktif dan langsung bisa dipakai masuk. Yang dijaga di
--   sini hanyalah domain emailnya.
--
-- Aturan domain
--   Boleh : @umn.ac.id (staf/dosen) dan subdomainnya, termasuk @student.umn.ac.id
--   Ditolak: domain lain (gmail.com, yahoo.com, dan sejenisnya)
--
-- Cara pakai
--   Jalankan di Supabase SQL Editor (atau Management API). Trigger hanya berlaku
--   untuk pendaftaran BARU; akun lama tidak terpengaruh.
--
-- Status: sudah terpasang di project ketemu-lost-found. Uji yang pernah dilakukan:
--   POST /auth/v1/signup  email@gmail.com            -> ditolak
--     {"code":"P0001","message":"Email harus memakai domain kampus UMN
--      (@umn.ac.id atau @student.umn.ac.id)"}
--   POST /auth/v1/signup  nama@student.umn.ac.id     -> HTTP 200, akun langsung aktif

create or replace function public.wajib_email_kampus()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
declare
    surel text := lower(coalesce(new.email, ''));
begin
    -- Akun tanpa email (mis. pendaftaran nomor HP) juga ditolak: aplikasi ini
    -- memakai email kampus sebagai identitas tunggal.
    if surel = '' or (surel not like '%@umn.ac.id' and surel not like '%.umn.ac.id') then
        raise exception 'Email harus memakai domain kampus UMN (@umn.ac.id atau @student.umn.ac.id)'
            using errcode = '22023';
    end if;

    return new;
end;
$$;

drop trigger if exists wajib_email_kampus on auth.users;

create trigger wajib_email_kampus
before insert on auth.users
for each row execute function public.wajib_email_kampus();

-- ── Verifikasi ───────────────────────────────────────────────────────────────
select 'fungsi wajib_email_kampus ada' as cek,
       exists (
           select 1 from pg_proc p
           join pg_namespace n on n.oid = p.pronamespace
           where n.nspname = 'public' and p.proname = 'wajib_email_kampus'
       )::text as lulus;

select 'trigger aktif di auth.users' as cek,
       exists (
           select 1 from pg_trigger
           where tgname = 'wajib_email_kampus'
             and tgrelid = 'auth.users'::regclass
             and not tgisinternal
       )::text as lulus;
