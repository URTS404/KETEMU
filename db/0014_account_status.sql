-- ============================================================================
--  0014 - Pemeriksaan status blokir untuk akun yang sedang login
--
--  Kenapa perlu: policy RESTRICTIVE `profiles_block_banned` (0011) menyembunyikan
--  baris `profiles` milik akun yang diblokir. Akibatnya aplikasi tidak bisa
--  membaca `is_banned` miliknya sendiri lewat SELECT biasa.
--
--  Fungsi SECURITY DEFINER di bawah menjawab satu pertanyaan saja:
--  "apakah akun yang sedang login sedang diblokir?" Tanpa membocorkan data lain.
--
--  Jalankan setelah 0011. Aman dijalankan ulang.
-- ============================================================================

create or replace function public.my_account_banned()
returns boolean
language sql
stable
security definer
set search_path = public
as $$
    select coalesce(
        (select is_banned from public.profiles where id = auth.uid()),
        false
    );
$$;

revoke all on function public.my_account_banned() from public;
grant execute on function public.my_account_banned() to authenticated;

-- Verifikasi
select 'fungsi my_account_banned' as cek,
       case when count(*) = 1 then 'OK' else 'BELUM' end as status
  from pg_proc where proname = 'my_account_banned';
