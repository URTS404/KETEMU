# Roadmap KETEMU v2 (UAS)

Dokumen ini mencatat arah pengembangan setelah UTS. Tahap 1 selesai dan bisa
didemokan; tahap 2–4 masih direncanakan.

## Tahap 1 — Panel admin in-app (selesai)

Tujuan: moderator (STAFF/ADMIN) bekerja dari dalam aplikasi Android, tanpa web.

- Dashboard ringkasan: antrean verifikasi, klaim menunggu, laporan diarsipkan, total laporan.
- Moderasi laporan: tombol verifikasi membuka dialog serah-terima — satpam memilih Pos tujuan (pelapor tidak pernah memilih pos), status → *Diserahkan ke Pos*, dan `reports.pos_id` ikut tersimpan; arsip menyembunyikan laporan dari Beranda tetapi datanya tetap tersimpan.
- Verifikasi klaim: jawaban verifikasi pengklaim ditinjau, setuju/tolak; persetujuan menerbitkan kode pengambilan 6 karakter dan memindahkan laporan ke *Siap Diambil*.
- Log aktivitas: append-only, mencatat aktor, aksi, target, dan waktu.
- Penjagaan akses dua lapis: tombol hanya muncul untuk STAFF/ADMIN dan ViewModel menolak aksi bila peran tidak berhak.
- Sumber data masih in-memory (dummy) agar demo tidak bergantung jaringan.

## Tahap 2 — Supabase (minggu 1)

- Project baru + skema SQL: `profiles`, `categories`, `pos_locations`, `reports`, `claims`, `audit_logs`.
- Row Level Security: pengguna hanya mengubah laporannya sendiri; STAFF/ADMIN lewat `profiles.role`; audit hanya bisa ditambah, tidak diubah.
- Auth Supabase menggantikan auth lokal bila `SUPABASE_URL` terisi; auth lokal tetap jadi mode demo offline.
- Storage untuk foto barang.
- **Realtime notifikasi (selesai)**: `SupabaseNotificationRepository` berlangganan `INSERT` tabel `notifications` lewat `db/0017_realtime.sql`, dengan polling 30 detik sebagai cadangan. Perubahan status laporan yang tampil seketika menyusul pada tahap 4.
- **Pencocokan otomatis (selesai)**: trigger `reports_auto_match` (`db/0018_auto_match.sql`) mencocokkan laporan baru dengan laporan aktif berjenis sebaliknya (kategori sama + kata kunci judul atau jarak GPS <= 250 m), mencatat pasangan di `matches`, dan mengirim notifikasi `POSSIBLE_MATCH` lewat Realtime. Halaman detail menampilkan bagian "Mungkin barangmu"; tombol klaim menyusul di tahap 4.


## Tahap 3 — Room offline-first (minggu 2)

- Entity: `reports`, `claims`, `audit_logs`, `outbox`, `profile_cache`.
- Repository Room menggantikan in-memory; outbox ditulis satu transaksi bersama perubahan data.
- `SyncWorker` (WorkManager) mengunggah antrean saat jaringan tersedia, dengan backoff.
- Catatan risiko: Room memerlukan KSP; kompatibilitas KSP dengan AGP 9 (Kotlin built-in) diuji lebih dulu, dengan fallback SQLiteOpenHelper bila gagal.

## Tahap 4 — Klaim end-to-end, realtime, QR, deliverable (minggu 3–4)

- Formulir klaim sisi pengguna + pertanyaan verifikasi pada laporan temuan.
- Unggah foto offline (simpan URI lokal, unggah saat online).
- Kode pengambilan + QR, notifikasi in-app.
- Deliverable: ERD, laporan uji, naskah video demo, APK rilis v2.0.0.
