# Status 7 tugas utama — KETEMU

Berkas ini adalah catatan tetap supaya status pekerjaan tidak bergantung pada ingatan
satu sesi obrolan. Daftar tugasnya berasal dari permintaan pada **17 November 2026
10.52**, dan statusnya selalu **diperiksa ulang ke kode + commit**, bukan dikutip dari
percakapan lama.

**Snapshot terakhir: `a1cb094`, 20 November 2026.**

Cara memakainya: sebelum melanjutkan pekerjaan, baca tabel di bawah; setelah satu tugas
selesai, perbarui barisnya dan ganti baris "Snapshot terakhir" dengan commit baru.

## Daftar asli (17 November 2026)

> 1. Pesan antar user dan staff
> 2. Sqlite room
> 3. Pemisahan akses staff dengan admin
> 4. Perbaikan claim
> 5. GPS (sudah sama teman saya commit terbaru)
> 6. Dan Lens
> 7. Pop up notification (sedang di kerjakan teman saya)
>
> Dari 7 tugas di atas kami filter dari yang tersulit sampai termudah lalu kita
> kerjakan dari yang termudah.

Urutan pengerjaan yang disepakati: #4 → #3 → #5 → #1 → #7 → #2 → #6.

## Status

| # | Tugas | Status | Bukti |
|---|---|---|---|
| 1 | Pesan antar user & staff | ✅ Selesai | `db/0019_messages.sql`, `0020_chat_view.sql`, `0022_chat_images.sql`, `0023_chat_storage.sql`, `0024_message_delete.sql`, `0027_typing_state.sql`; kirim gambar + kompresi, antrean offline (`filesDir/chat_outbox`), hapus pesan, indikator mengetik, gulir otomatis |
| 2 | Room / SQLite (offline-first) | ❌ Belum | `room`, `ksp`, `workManager` baru terdaftar di `gradle/libs.versions.toml`; **belum satu pun dipakai** di `app/build.gradle.kts`. Antrean offline hanya ada untuk chat, belum untuk laporan |
| 3 | Pemisahan akses STAFF vs ADMIN | ✅ Selesai | `db/0011_admin_management.sql`, `0012_admin_audit.sql`, `0013_demo_admin_staff.sql`, `0014_account_status.sql`, `0015_staff_keys.sql` |
| 4 | Perbaikan klaim + kode pengambilan | ✅ Selesai | trigger `decide_claim` & `apply_claim_to_report` (`db/0001_schema.sql:219` dan `:254`, diperbaiki `db/0009_fix_audit_enum_cast.sql:51`); kode pengambilan `414A0D` saat uji emulator |
| 5 | GPS / peta | ✅ Selesai (dikerjakan rekan) | `db/0010_report_coordinates.sql`, commit `2ed6a60` (peta OpenStreetMap + kolom koordinat) |
| 6 | Pencocokan gambar ("Lens") | ⚠️ Sebagian | `db/0018_auto_match.sql` mencocokkan **kata kunci judul + jarak GPS ≤ 250 m**, bukan isi gambar. Tidak ada ML Kit / model visi di repo |
| 7 | Pop-up notification | ✅ Selesai (dikerjakan rekan) | `db/0016_notifications.sql`, `0017_realtime.sql`, kartu notifikasi Android, sakelar di profil |

Ringkas: **5 selesai, 1 selesai sebagian, 1 belum.**

## Yang benar-benar tersisa

### #2 Room / offline-first — satu-satunya tugas inti yang utuh tersisa

Dua pilihan, keduanya sudah pernah dibandingkan:

| Opsi | Isi | Perkiraan | Risiko |
|---|---|---|---|
| **B — antrean ringan** | Laporan yang dibuat offline ditahan sebagai JSON + foto di penyimpanan, tampil berlabel "Menunggu Sinkron", terkirim otomatis saat jaringan kembali. Daftar laporan tetap tidak bisa dibaca offline | 3–4 jam | Rendah — tanpa dependensi build baru (kotlinx.serialization sudah dipakai) |
| **A — Room penuh** | Room sebagai sumber data utama + tabel outbox + WorkManager + penanganan konflik; daftar laporan bisa dibaca offline | 8–12 jam | Tinggi — Room butuh KSP di atas AGP 9 + Kotlin built-in, kombinasi yang sudah beberapa kali membuat build merah |

Catatan: chip "Menunggu Sinkron" sudah ada sejak fase awal desain, tetapi belum pernah
benar-benar berfungsi.

### #6 Pencocokan gambar — ditunda atas keputusan pemilik proyek

Kalau dinaikkan: ML Kit *Image Labeling* di perangkat ± 1 hari (tanpa server), atau
CLIP + pgvector ± 1–2 minggu.

## Selesai di luar 7 tugas itu

- **Supabase Auth penuh**: daftar, login, profil dari tabel `profiles`, peran dari database, penolakan domain non-kampus di sisi server, sesi tersimpan permanen (`SharedPreferences`), pemulihan sesi saat aplikasi dibuka.
- **Blokir akun** end-to-end: admin memblokir → login ditolak dengan pesan khusus (`0014`).
- **Kunci staf sekali pakai**: kuota, dua jalur penebusan (`0015`).
- **Log aktivitas permanen** dari trigger database, dengan dedupe agar satu aksi tidak tampil dua kali.
- **Antrean moderasi & arsip laporan** berbasis database, bukan data contoh.
- **Indikator "sedang menulis…"** (`0027`) beserta gulir otomatis pesan masuk — 20 November 2026.
- **Unit test**: 105 `@Test` di `app/src/test` (22 berkas).

## Riwayat pembaruan berkas ini

| Tanggal | Kejadian |
|---|---|
| 17 Nov 2026 10.52 | Daftar 7 tugas diminta; urutan kerja disepakati termudah → tersulit |
| 17 Nov 2026 12.07 | Status pertama: #4, #3, #5 selesai; #1, #7, #2, #6 belum |
| 19 Nov 2026 23.05 | Status kedua: lima selesai (#1, #3, #4, #5, #7), #6 sebagian, #2 belum |
| 20 Nov 2026 | Berkas ini dibuat; status diperiksa ulang ke kode pada commit `a1cb094`. Tidak ada perubahan status sejak 19 Nov |
