# Riwayat commit — Ketemu (aplikasi Lost & Found kampus)

Repo: <https://github.com/URTS404/KETEMU.git> · branch `main`
Rentang yang dicatat di sini: `2f60111` … `9affd4c` (**29 commit**, 120 berkas, +9.067 / −414 baris).
Titik awal `9d67b7f` adalah "Initial commit" dari kerangka proyek.

Perintah untuk melihat sendiri:

```bash
git log --oneline --graph 2f60111~1..HEAD
git log --stat 2f60111~1..HEAD      # jumlah berkas per commit
```

---

## Fase 1 — Perbaikan build & fondasi antarmuka (7 commit)

| # | Commit | Tanggal | Isi |
|---|---|---|---|
| 1 | `2f60111` | 14 Nov 19:52 | **fix(build)** hapus dependensi `supabase-kt:VERSION` placeholder yang membuat Gradle gagal resolve — proyek akhirnya bisa dibangun |
| 2 | `8438a7a` | 14 Nov 19:52 | **feat(theme)** tema Material 3 kustom (warna brand teal/oranye, tipografi, shape) + logo & ikon adaptif |
| 3 | `8adfecc` | 14 Nov 19:53 | **feat(components)** kartu laporan, chip status, avatar inisial, tampilan loading/kosong/error |
| 4 | `fdb1012` | 14 Nov 19:53 | **feat(data)** model domain + repository in-memory + autentikasi lokal akun dummy |
| 5 | `42e82d7` | 14 Nov 19:53 | **feat(auth)** layar login, daftar, lupa sandi + validasi domain kampus |
| 6 | `8893450` | 14 Nov 19:53 | **feat(screens)** beranda, lapor hilang, laporan saya, pesan, profil + navigasi berargumen |
| 7 | `67b3298` | 14 Nov 19:53 | **docs** README proyek, konfigurasi build, pengecualian berkas lokal (`.gitignore`) |

Tujuan fase ini: aplikasi bisa dijalankan, punya tema sendiri, dan seluruh layar UTS berfungsi dengan data contoh.

## Fase 2 — Panel admin (mode in-memory) (4 commit)

| # | Commit | Tanggal | Isi |
|---|---|---|---|
| 8 | `6c7da48` | 14 Nov 20:12 | **feat(admin)** model `Claim`, `AuditLogEntry`, `ClaimStatus`, `PickupCode`, repository klaim/audit in-memory |
| 9 | `ecbb959` | 14 Nov 20:12 | **feat(admin)** dashboard, moderasi laporan, verifikasi klaim, log aktivitas, daftar pengguna + 5 rute baru |
| 10 | `b478940` | 14 Nov 20:12 | **test(admin)** 17 unit test: keputusan klaim, idempotensi, audit, kode pengambilan, hak akses peran |
| 11 | `50bc644` | 14 Nov 20:12 | **docs** roadmap v2 + catatan panel admin |

Tujuan: peran STAFF/ADMIN punya alur kerja nyata (verifikasi, arsip, putuskan klaim) tanpa bergantung jaringan.

## Fase 3 — Skema database Supabase (1 commit)

| # | Commit | Tanggal | Isi |
|---|---|---|---|
| 12 | `9122190` | 14 Nov 20:15 | **feat(db)** 6 tabel (`profiles`, `categories`, `pos_locations`, `reports`, `claims`, `audit_logs`), enum, trigger (profil otomatis, audit, keputusan klaim), RLS 14 policy, data awal |

Tujuan: aturan bisnis ditegakkan di server, bukan di aplikasi.

## Fase 4 — Redesign UI panel admin (8 commit, oleh Kevin Ivan Setiadi Phang)

| # | Commit | Tanggal | Isi |
|---|---|---|---|
| 13 | `ead5472` | 15 Nov 19:35 | **style(admin)** redesign panel Lost & Found |
| 14 | `c666ed0` | 15 Nov 20:02 | **style(admin)** restyle moderasi laporan |
| 15 | `eb6bfd7` | 15 Nov 20:24 | **style(admin)** restyle log aktivitas |
| 16 | `5690461` | 15 Nov 20:38 | **style(admin)** restyle verifikasi klaim (kartu ringkasan bergradasi) |
| 17 | `b130483` | 15 Nov 20:51 | **style(admin)** restyle daftar pengguna |
| 18 | `159cf69` | 15 Nov 20:56 | **style(admin)** pekatkan ujung gradasi kartu ringkasan |
| 19 | `3bc0ef7` | 15 Nov 21:05 | **feat(admin)** animasi kemunculan isi layar pengguna |
| 20 | `b42f991` | 15 Nov 21:16 | **feat(admin)** animasi muncul berurutan dari atas ke bawah |

## Fase 5 — Integrasi Supabase nyata (9 commit)

| # | Commit | Tanggal | Isi |
|---|---|---|---|
| 21 | `18a7f42` | 14 Nov 20:20 | **fix(db)** kode pengambilan pakai `md5()` — tidak lagi bergantung schema `extensions` (pgcrypto) |
| 22 | `9576ba9` | 14 Nov 20:29 | **feat(auth)** pakai Supabase Auth bila dikonfigurasi, dengan cadangan akun demo lokal |
| 23 | `cc1484f` | 14 Nov 20:49 | **fix(auth)** deteksi sesi lewat `currentSessionOrNull()` setelah daftar |
| 24 | `9812607` | 14 Nov 20:52 | **feat(data)** laporan dibaca dari PostgREST (join pos & profil) + fallback in-memory |
| 25 | `d7cc968` | 16 Nov 08:11 | **fix(db)** penjaga kolom profil mengizinkan operasi administratif dari SQL Editor + skrip promosi staf |
| 26 | `702fabf` | 16 Nov 08:16 | **feat(auth)** peran pengguna dibaca dari tabel `profiles`, bukan metadata auth |
| 27 | `e1c5269` | 16 Nov 08:19 | **test(db)** contoh klaim untuk menguji jalur tulis klaim + audit |
| 28 | `6883911` | 16 Nov 08:21 | **docs(db)** query pemeriksaan mandiri: memastikan seluruh skrip database terpasang |
| 29 | `9affd4c` | 16 Nov 08:25 | **fix(db)** cast enum pada audit trigger klaim/arsip · **fix(admin)** kegagalan server ditangani, aplikasi tidak crash |

Tujuan fase ini: data benar-benar berpindah dari PostgreSQL ke aplikasi, dan aksi moderator benar-benar menulis kembali ke database di bawah kendali RLS.

---

## Ringkasan untuk penilaian

| Aspek | Bukti |
|---|---|
| Riwayat kerja bertahap, bukan sekali jadi | 29 commit bertopik kecil (`feat`/`fix`/`style`/`test`/`docs`) dalam 5 fase |
| Kolaborasi | 8 commit oleh Kevin Ivan Setiadi Phang (redesign panel admin); riwayat digabung dengan `rebase` tanpa menghilangkan commit siapa pun |
| Kualitas | 45 unit test lulus (`gradlew :app:testDebugUnitTest`), APK `assembleDebug` sukses |
| Keamanan data | RLS 14 policy, trigger audit append-only, kolom sensitif dijaga trigger |
| Dokumentasi | `README.txt`, `laporan_uji_runtime_uts.md`, `docs/v2-roadmap.md`, `db/README.md`, `db/0008_selfcheck.sql` |
