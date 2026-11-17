-- ============================================================================
--  0013 - Data demo untuk memamerkan pemisahan hak STAFF vs ADMIN
--
--  Isi berkas ini:
--    1. `rahmat@umn.ac.id`  -> ADMIN (boleh kelola akun)
--    2. `siti.uas@student.umn.ac.id` -> STAFF (hanya bekerja pada barang)
--    3. satu klaim MENUNGGU pada laporan "Dompet Kulit Hitam" sebagai bahan uji
--       persetujuan klaim + penerbitan kode pengambilan.
--
--  Jalankan SETELAH 0011 dan 0012. Aman dijalankan ulang (idempoten).
-- ============================================================================

-- ---------- 1 & 2. Peran untuk demo ----------
update public.profiles
   set role = 'ADMIN'
 where email = 'rahmat@umn.ac.id';

update public.profiles
   set role = 'STAFF'
 where email = 'siti.uas@student.umn.ac.id';

-- ---------- 3. Klaim contoh yang menunggu keputusan ----------
insert into public.claims (report_id, claimant_id, answer, status)
select r.id,
       p.id,
       'Barang ini milik saya. Di dalam dompet ada KTM atas nama Siti Uas dan kartu ATM BCA.',
       'MENUNGGU'::public.claim_status
  from public.reports r
  join public.profiles p on p.email = 'siti.uas@student.umn.ac.id'
 where r.title = 'Dompet Kulit Hitam'
   and not exists (
       select 1 from public.claims c
        where c.report_id = r.id and c.status = 'MENUNGGU'::public.claim_status
   );

-- ---------- Verifikasi ----------
select 'peran rahmat' as cek, coalesce(max(role::text), 'TIDAK ADA') as status
  from public.profiles where email = 'rahmat@umn.ac.id'
union all
select 'peran siti', coalesce(max(role::text), 'TIDAK ADA')
  from public.profiles where email = 'siti.uas@student.umn.ac.id'
union all
select 'klaim menunggu', count(*)::text
  from public.claims where status = 'MENUNGGU'::public.claim_status;
