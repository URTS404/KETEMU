-- ============================================================================
--  0007 - Contoh klaim untuk menguji sisi tulis (klaim + audit)
--
--  Kenapa perlu: aplikasi belum punya layar pengajuan klaim dari sisi
--  mahasiswa, sehingga tabel `claims` kosong dan alur persetujuan tidak bisa
--  didemokan. Berkas ini menanam SATU klaim contoh supaya panel admin
--  "Verifikasi klaim" bisa diuji ujung ke ujung.
--
--  Yang dibuktikan setelah klaim disetujui di aplikasi:
--    1. trigger `decide_claim` membuat `pickup_code` 6 karakter dari md5(id),
--    2. trigger `apply_claim_to_report` memindahkan status laporan,
--    3. aksi moderator tercatat di `audit_logs`.
--
--  Aman dijalankan ulang (ON CONFLICT tidak menambah baris ganda).
-- ============================================================================

insert into public.claims (report_id, claimant_id, answer, status)
select r.id, p.id,
       'Barang ini milik saya. Ada stiker nama di bagian dalam dan saya bisa '
       || 'menyebutkan isi tasnya: KTM, kartu ATM BCA, dan kunci kos.',
       'MENUNGGU'
  from public.reports r
  join public.profiles p on p.email = 'siti.uas@student.umn.ac.id'
 where r.title = 'Payung Lipat Biru'
on conflict (report_id, claimant_id) do nothing;

-- Verifikasi: harus tampil 1 baris berstatus MENUNGGU
select c.id,
       r.title        as laporan,
       p.full_name    as pengklaim,
       c.status,
       c.pickup_code,
       c.created_at
  from public.claims c
  join public.reports  r on r.id = c.report_id
  join public.profiles p on p.id = c.claimant_id
 order by c.created_at desc;
