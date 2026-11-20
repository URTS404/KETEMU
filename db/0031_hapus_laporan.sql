-- 0031_hapus_laporan.sql — pemilik boleh menghapus laporannya sendiri
--
-- Cara pakai: SQL Editor → New query → tempel → Run.
--
-- Masalah yang diperbaiki:
--   Sebelumnya tidak ada kebijakan DELETE untuk tabel reports, sehingga
--   mahasiswa tidak bisa membatalkan laporan yang salah kirim, atau laporan
--   hilang yang barangnya sudah ditemukan sendiri. Satu-satunya jalan adalah
--   SQL Editor / service_role.
--
-- Aturan yang dipasang:
--   * hanya pemilik laporan (owner_id = auth.uid()),
--   * hanya bila TIDAK ada klaim yang masih berjalan (MENUNGGU / DISETUJUI).
--     Klaim aktif berarti barang sedang dalam proses serah terima; menghapus
--     laporannya akan menghapus riwayat klaim orang lain.
--   * petugas tetap memakai Arsipkan (menyembunyikan), bukan menghapus, supaya
--     jejak audit tidak hilang.
--
-- Baris klaim, pencocokan, dan notifikasi yang menunjuk laporan ini ikut
-- terhapus otomatis karena foreign key-nya ON DELETE CASCADE.
--
-- Aman dijalankan ulang.

drop policy if exists reports_delete_own on public.reports;
create policy reports_delete_own on public.reports
for delete to authenticated
using (
    owner_id = auth.uid()
    and not exists (
        select 1
          from public.claims c
         where c.report_id = reports.id
           and c.status in ('MENUNGGU', 'DISETUJUI')
    )
);

-- ── Verifikasi ───────────────────────────────────────────────────────────────
-- Harus bernilai true.
select 'kebijakan hapus laporan ada' as cek,
       exists (
           select 1 from pg_policies
            where schemaname = 'public'
              and tablename = 'reports'
              and policyname = 'reports_delete_own'
       )::text as lulus;
