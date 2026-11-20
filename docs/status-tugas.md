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
| 2 | Room / SQLite (offline-first) | ✅ Selesai | `data/local/` (`ReportEntity`, `OutboxEntity`, `ReportDao`, `OutboxDao`, `KetemuDatabase` v1) + `data/repository/RoomReportRepository.kt`; KSP + Room 2.8.5 aktif di `app/build.gradle.kts`; skema tercatat di `app/schemas/`. Daftar laporan terbaca offline, aksi tulis masuk `outbox` lalu terkirim otomatis |
| 3 | Pemisahan akses STAFF vs ADMIN | ✅ Selesai | `db/0011_admin_management.sql`, `0012_admin_audit.sql`, `0013_demo_admin_staff.sql`, `0014_account_status.sql`, `0015_staff_keys.sql` |
| 4 | Perbaikan klaim + kode pengambilan | ✅ Selesai | trigger `decide_claim` & `apply_claim_to_report` (`db/0001_schema.sql:219` dan `:254`, diperbaiki `db/0009_fix_audit_enum_cast.sql:51`); kode pengambilan `414A0D` saat uji emulator |
| 5 | GPS / peta | ✅ Selesai (dikerjakan rekan) | `db/0010_report_coordinates.sql`, commit `2ed6a60` (peta OpenStreetMap + kolom koordinat) |
| 6 | Pencocokan gambar ("Lens") | ⚠️ Sebagian | `db/0018_auto_match.sql` mencocokkan **kata kunci judul + jarak GPS ≤ 250 m**, bukan isi gambar. Tidak ada ML Kit / model visi di repo |
| 7 | Pop-up notification | ✅ Selesai (dikerjakan rekan) | `db/0016_notifications.sql`, `0017_realtime.sql`, kartu notifikasi Android, sakelar di profil |

Ringkas: **6 selesai, 1 selesai sebagian.**

## Yang benar-benar tersisa

### #2 Room / offline-first — dikerjakan 20 November 2026 (opsi A)

Room dipakai sebagai sumber data yang dibaca UI; Supabase hanya mengisi cache dan
menerima perubahan lewat tabel `outbox`. Bukti uji di emulator:

- Mode pesawat aktif → tutup paksa aplikasi → buka lagi: 10 laporan tetap tampil
  dari SQLite (dulu layar kosong).
- Buat laporan saat mode pesawat → muncul berlabel "Menunggu Sinkron", banner
  "1 laporan menunggu dikirim"; logcat `addReport: … disimpan lokal (PENDING) + antrean`.
- Nyalakan data → logcat `kirim CREATE_REPORT (…) berhasil`, `tarik: 11 baris`,
  label "Menunggu Sinkron" hilang tanpa interaksi pengguna.

Sisa yang masih boleh dikerjakan (tidak menghalangi demo):

| Hal | Keadaan sekarang |
|---|---|
| Penjadwalan sinkron | Timer 20 detik + pemicu saat jaringan pulih. `WorkManager` belum dipakai, jadi antrean tidak dikirim saat aplikasi mati |
| Konflik | "Perubahan lokal menang": baris PENDING tidak ditimpa hasil server sampai terkirim |
| Hapus laporan oleh mahasiswa | Belum ada di UI maupun kebijakan RLS (hapus hanya lewat SQL Editor) |
| Migrasi skema | Versi 1 memakai `fallbackToDestructiveMigration` (data lokal hilang bila skema berubah) — wajib diganti migrasi sungguhan sebelum rilis |

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
- **Room/offline-first untuk laporan** (#2): daftar laporan terbaca tanpa jaringan, aksi tulis masuk antrean `outbox` dan terkirim otomatis — 20 November 2026.
- **Unit test**: 105 `@Test` di `app/src/test` (22 berkas).
- **Serah terima barang + penutupan dua laporan** — 20 November 2026: bagian "Menunggu pengambilan" di layar Verifikasi klaim, dialog kode pengambilan opsional, laporan temuan & laporan hilang pasangannya menjadi `SELESAI_DIKLAIM`, notifikasi ke pengklaim. Fungsi `complete_handover` ada di `db/0028_serah_terima.sql` (belum dijalankan di project Supabase; aplikasi memakai jalur cadangan sampai skrip itu dijalankan).

## Riwayat pembaruan berkas ini

| Tanggal | Kejadian |
|---|---|
| 17 Nov 2026 10.52 | Daftar 7 tugas diminta; urutan kerja disepakati termudah → tersulit |
| 17 Nov 2026 12.07 | Status pertama: #4, #3, #5 selesai; #1, #7, #2, #6 belum |
| 19 Nov 2026 23.05 | Status kedua: lima selesai (#1, #3, #4, #5, #7), #6 sebagian, #2 belum |
| 20 Nov 2026 | Berkas ini dibuat; status diperiksa ulang ke kode pada commit `a1cb094`. Tidak ada perubahan status sejak 19 Nov |
| 20 Nov 2026 13.40 | #2 Room selesai (opsi A) dan diuji di emulator dengan mode pesawat; status menjadi 6 selesai, 1 sebagian |
| 20 Nov 2026 18.40 | Alur klaim dilengkapi tahap serah terima; diuji end-to-end di tiga emulator (Siti, Rahmat, Budi) sampai kedua laporan `SELESAI_DIKLAIM` |
