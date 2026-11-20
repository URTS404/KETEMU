-- 0029_claim_resubmit.sql — klaim yang ditolak boleh diajukan ulang
--
-- Cara pakai: SQL Editor → New query → tempel → Run.
--
-- Masalah yang diperbaiki:
--   `db/0001_schema.sql:110` memasang constraint berikut:
--       constraint claims_unique_active unique (report_id, claimant_id)
--   Tanpa syarat status. Akibatnya klaim yang sudah DITOLAK tetap "memakai"
--   satu-satunya slot untuk (laporan, pengklaim) itu, sehingga orang yang sama
--   tidak pernah bisa mengajukan klaim lagi — walaupun barangnya benar miliknya
--   dan petugas salah menilai. Kesalahannya baru ketahuan saat INSERT ditolak
--   dengan pesan duplikat yang membingungkan pengguna.
--
-- Perbaikan:
--   1. Constraint lama dilepas.
--   2. Diganti indeks unik PARSIAL yang hanya berlaku untuk klaim yang masih
--      MENUNGGU atau sudah DISETUJUI.
--   3. Riwayat klaim DITOLAK tetap tersimpan sebagai jejak audit, karena
--      barisnya tidak dihapus — hanya tidak lagi menghalangi pengajuan baru.
--
-- Aman dijalankan ulang.

alter table public.claims
    drop constraint if exists claims_unique_active;

-- Satu klaim aktif per (laporan, pengklaim). Klaim DITOLAK tidak dihitung,
-- sehingga pengajuan ulang setelah penolakan berhasil.
create unique index if not exists claims_one_active_per_user
    on public.claims (report_id, claimant_id)
    where status in ('MENUNGGU', 'DISETUJUI');

-- ── Verifikasi ───────────────────────────────────────────────────────────────
-- Kedua baris harus bernilai true.
select 'constraint lama terlepas' as cek,
       (count(*) = 0)::text as lulus
  from pg_constraint
 where conname = 'claims_unique_active'
union all
select 'indeks unik parsial ada',
       (count(*) > 0)::text
  from pg_indexes
 where schemaname = 'public'
   and tablename = 'claims'
   and indexname = 'claims_one_active_per_user';
