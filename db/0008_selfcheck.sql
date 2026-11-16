-- ============================================================================
--  0008 - Pemeriksaan mandiri: berkas SQL mana yang sudah terpasang?
--
--  Satu query, satu hasil. Bandingkan kolom `nilai` dengan kolom `harapan`.
--  Bila ada yang tidak cocok, jalankan ulang berkas yang disebut di kolom
--  `berkas`. Semua query di sini hanya membaca; tidak mengubah apa pun.
-- ============================================================================

with checks as (
    select 'Tabel public (harus 6)' as cek, '0001_schema.sql' as berkas, '6' as harapan,
           (select count(*)::text from information_schema.tables
             where table_schema = 'public'
               and table_name in ('profiles','categories','pos_locations','reports','claims','audit_logs')) as nilai

    union all
    select 'Policy RLS terpasang', '0002_rls.sql', '>= 12',
           (select count(*)::text from pg_policies where schemaname = 'public')

    union all
    select 'Trigger audit laporan', '0001_schema.sql', '1',
           (select count(*)::text from pg_trigger
             where tgname = 'reports_audit' and not tgisinternal)

    union all
    select 'Trigger keputusan klaim', '0001_schema.sql', '1',
           (select count(*)::text from pg_trigger
             where tgname = 'claims_decide' and not tgisinternal)

    union all
    select 'Penjaga kolom profil', '0002_rls.sql + 0006', '1',
           (select count(*)::text from pg_trigger
             where tgname = 'profiles_guard_columns' and not tgisinternal)

    union all
    select 'Penjaga sudah versi 0006', '0006_promote_staff_fix.sql', '1',
           (select count(*)::text from pg_proc
             where proname = 'guard_profile_columns'
               and prosrc like '%auth.uid() is not null%')

    union all
    select 'Kategori contoh', '0003_seed.sql', '4',
           (select count(*)::text from public.categories)

    union all
    select 'Pos barang temuan', '0003_seed.sql', '4',
           (select count(*)::text from public.pos_locations)

    union all
    select 'Laporan contoh', '0004_seed_reports.sql', '2',
           (select count(*)::text from public.reports)

    union all
    select 'Akun staf berperan STAFF', '0006_promote_staff_fix.sql', '1',
           (select count(*)::text from public.profiles where role = 'STAFF')

    union all
    select 'Klaim contoh', '0007_seed_claim.sql', '1',
           (select count(*)::text from public.claims)

    union all
    select 'Jejak audit', 'otomatis oleh trigger', '>= 1',
           (select count(*)::text from public.audit_logs)
)
select cek,
       harapan,
       nilai,
       case
           when harapan like '>=%' then
               case when nilai::int >= replace(harapan, '>=', '')::int then 'OK' else 'BELUM' end
           else
               case when nilai = harapan then 'OK' else 'BELUM' end
       end as status,
       berkas as jalankan_bila_belum
  from checks;
