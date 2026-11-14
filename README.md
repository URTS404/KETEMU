# KETEMU — Campus Lost & Found Hub

Aplikasi Android untuk melaporkan, mencari, dan mengklaim barang hilang maupun barang temuan di lingkungan kampus (closed-ecosystem, email kampus).
Dibangun dengan **Kotlin + Jetpack Compose**, arsitektur **MVVM** berlapis (UI → ViewModel → Domain → Data), custom **Material 3** theme, dan **Navigation Compose**.

> Proyek UTS mata kuliah **IF570L Lab Mobile Application Programming** — Kelompok 5.

## Fitur

- **Autentikasi email kampus** — login, daftar akun, lupa kata sandi dengan validasi domain `@student.umn.ac.id` / `@umn.ac.id`.
- **Beranda** — statistik laporan, kartu pengumuman, pencarian, dan filter Hilang/Ditemukan.
- **Lapor barang** — formulir hilang/temuan dengan validasi, pemilih kategori, dan kamera (foto barang).
- **Detail laporan** — status barang + timeline "Perjalanan Barang" (Dilaporkan → Diserahkan ke Pos → Siap Diambil → Selesai Diklaim).
- **Laporan saya & Pesan** — riwayat laporan milik pengguna dan percakapan dengan petugas Pos Barang Temuan.
- **Profil** — identitas kampus, simulasi peran (USER/STAFF/ADMIN), dan keluar.
- **Tema terang/gelap** — palet kampus sendiri (teal + oranye), tipografi dan shape kustom.

## Tech stack

| Lapisan | Teknologi |
|---|---|
| Bahasa | Kotlin 2.4 |
| UI | Jetpack Compose + Material 3 (BOM 2026.09), Compose Navigation |
| Arsitektur | MVVM, `StateFlow`, repository + container manual (tanpa Hilt) |
| Gambar | Coil |
| Build | AGP 9.4, Gradle 9.6 (wrapper), `compileSdk`/`targetSdk` 37, `minSdk` 26 |
| Test | JUnit4 + `kotlinx-coroutines-test` (28 unit test) |

## Menjalankan

Prasyarat: Android Studio terbaru (AGP 9), JDK 17+, Android SDK Platform 37.

```bash
# Windows
gradlew.bat assembleDebug
# macOS / Linux
./gradlew assembleDebug
```

Hasil: `app/build/outputs/apk/debug/app-debug.apk`, atau pasang langsung dengan `gradlew.bat installDebug`.
Berkas `local.properties` (lokasi SDK) dibuat otomatis oleh Android Studio dan tidak disertakan di repo.

## Akun uji

Login diperiksa secara lokal, tanpa jaringan. Kata sandi tiap akun sama dengan NIM/NIDN-nya.

| Peran | Email | Sandi |
|---|---|---|
| Mahasiswa | `budi.santoso@student.umn.ac.id` | `00000012345` |
| Staf (Satpam) | `rahmat@umn.ac.id` | `STAFF-001` |

Akun uji juga ditampilkan langsung pada layar Login agar mudah dipakai saat demonstrasi.

## Struktur proyek

```
app/src/main/java/org/umn/application_ketemu/
├── core/        utilitas (validasi email kampus)
├── data/        repository, dummy data, sesi, container dependensi
├── domain/      model + kontrak repository
└── ui/          theme, komponen, navigasi, layar Compose + ViewModel
```

## Tim — Kelompok 5

1. Brilliant Hafizd Hermawan (00000130613)
2. Kevin Ivan Setiadi Phang (00000131636)
3. Indra Rizky Raharja (00000133155)
4. Awang Raditya Pratama (00000133275)

## Dokumen lain

- Dokumen lengkap berorientasi rubrik UTS (deskripsi, pembagian tugas, kontribusi anggota, catatan build) dan berkas APK **tidak** disertakan di dalam repo; keduanya dikumpulkan bersama arsip tugas.
