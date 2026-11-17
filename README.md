# KETEMU — Campus Lost & Found Hub

Aplikasi Android untuk melaporkan, mencari, dan mengklaim barang hilang maupun barang temuan di lingkungan kampus (closed-ecosystem, email kampus).
Dibangun dengan **Kotlin + Jetpack Compose**, arsitektur **MVVM** berlapis (UI → ViewModel → Domain → Data), custom **Material 3** theme, dan **Navigation Compose**.

> Proyek UTS mata kuliah **IF570L Lab Mobile Application Programming** — Kelompok 5.

## Fitur

- **Autentikasi email kampus** — login, daftar akun, lupa kata sandi dengan validasi domain `@student.umn.ac.id` / `@umn.ac.id`.
- **Beranda** — statistik laporan, kartu pengumuman, pencarian, dan filter Hilang/Ditemukan.
- **Lapor barang** — formulir hilang/temuan dengan validasi, pemilih kategori, kamera (foto barang), serta penanda titik lokasi di peta OpenStreetMap (GPS atau ketuk peta).
- **Detail laporan** — status barang + timeline "Perjalanan Barang" (Dilaporkan → Diserahkan ke Pos → Siap Diambil → Selesai Diklaim), plus pratinjau titik lokasi dan tombol buka di aplikasi peta.
- **Laporan saya & Pesan** — riwayat laporan milik pengguna dan percakapan dengan petugas Pos Barang Temuan.
- **Profil** — identitas kampus, simulasi peran (USER/STAFF/ADMIN), dan keluar.
- **Notifikasi dua lapis** - kotak masuk in-app (lonceng Beranda) dan kartu di panel notifikasi Android untuk konfirmasi unggahan, barang temuan baru, perubahan status, dan keputusan klaim; notifikasi luar bisa dinyalakan/dimatikan dari **Akun > Pengaturan**. Kabar baru dikirim **Supabase Realtime** (langganan `INSERT` tabel `notifications`) sehingga muncul seketika, dengan polling 30 detik sebagai cadangan.
- **Pencocokan otomatis** - begitu laporan masuk, trigger server (`db/0018_auto_match.sql`) mencocokkannya dengan laporan sebaliknya (kategori sama + kata kunci judul atau jarak GPS <= 250 m), mengirim notifikasi `POSSIBLE_MATCH`, dan mengisi bagian "Mungkin barangmu" di halaman detail.

- **Tema terang/gelap** — palet kampus sendiri (teal + oranye), tipografi dan shape kustom.

## Tech stack

| Lapisan | Teknologi |
|---|---|
| Bahasa | Kotlin 2.4 |
| UI | Jetpack Compose + Material 3 (BOM 2026.09), Compose Navigation |
| Arsitektur | MVVM, `StateFlow`, repository + container manual (tanpa Hilt) |
| Backend | Supabase (Auth, PostgREST, Realtime) — mode demo in-memory bila `local.properties` kosong |
| Gambar | Coil |
| Peta | osmdroid 6.1.20 + OpenStreetMap (tanpa API key / Play Services) |
| Build | AGP 9.4, Gradle 9.6 (wrapper), `compileSdk`/`targetSdk` 37, `minSdk` 26 |
| Test | JUnit4 + `kotlinx-coroutines-test` (84 unit test) |

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

### Build dari terminal (Windows)

Gradle wrapper memerlukan JDK yang bisa ditemukan. Bila muncul pesan
`JAVA_HOME is not set and no 'java' command could be found`, arahkan ke JDK
bawaan Android Studio lalu jalankan build pada terminal yang sama:

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
gradlew.bat assembleDebug
```

### Jika Clean/Rebuild gagal di Windows

Folder output build yang disalin dari OneDrive/arsip bisa membawa atribut
`ReadOnly`, sehingga Gradle gagal menghapusnya (`Unable to delete directory`).
Proyek ini melepas atribut tersebut otomatis sebelum `clean` dan `preBuild`.

Bila masih gagal, pastikan tidak ada build lain yang sedang berjalan di Android
Studio, lalu hentikan daemon Gradle dan ulangi:

```powershell
gradlew.bat --stop
gradlew.bat clean assembleDebug
```

## Akun uji

Login diperiksa secara lokal, tanpa jaringan. Kata sandi tiap akun sama dengan NIM/NIDN-nya.

| Peran | Email | Sandi |
|---|---|---|
| Mahasiswa | `budi.santoso@student.umn.ac.id` | `00000012345` |
| Staf (Satpam) | `rahmat@umn.ac.id` | `STAFF-001` |

Akun uji juga ditampilkan langsung pada layar Login agar mudah dipakai saat demonstrasi.

## Panel admin (tahap 1 UAS)

Masuk sebagai staf (`rahmat@umn.ac.id` / `STAFF-001`), buka tab **Akun**, lalu tekan **Buka Panel Admin**:

- **Dashboard** — antrean verifikasi, klaim menunggu, laporan diarsipkan, total laporan.
- **Moderasi laporan** — verifikasi laporan baru (status naik ke *Diserahkan ke Pos*) atau arsipkan laporan yang tidak relevan.
- **Verifikasi klaim** — tinjau jawaban verifikasi pengklaim, lalu setujui (kode pengambilan diterbitkan, laporan menjadi *Siap Diambil*) atau tolak.
- **Log aktivitas** — jejak keputusan moderator yang tidak bisa diubah.

Panel ini hanya dirender untuk peran STAFF/ADMIN, dan setiap aksi diperiksa ulang di ViewModel. Data masih in-memory pada tahap ini; rencana Supabase + Room ada di [docs/v2-roadmap.md](docs/v2-roadmap.md).

## Notifikasi (in-app + panel sistem)

Kabar Ketemu tampil di dua tempat dengan isi yang sama:

- **Kotak masuk in-app** - lonceng di Beranda membuka layar Notifikasi (tandai dibaca, buka laporan terkait).
- **Panel notifikasi Android** - kartu di luar aplikasi untuk konfirmasi unggahan, barang temuan baru, perubahan status laporan, dan keputusan klaim.

Pengiriman kabar memakai dua jalur yang saling melengkapi:

1. **Supabase Realtime** - `SupabaseNotificationRepository` berlangganan `INSERT` pada tabel `notifications`; kabar langsung masuk lonceng dan panel sistem tanpa menunggu. Langganan baru dibuka setelah pengguna terautentikasi agar Row Level Security mengirim baris yang benar, dan ditutup saat keluar.
2. **Polling 30 detik** - cadangan bila Realtime tidak aktif (mis. publication belum didaftarkan) atau koneksi terputus. Baris yang sama tidak digandakan (dedupe berdasarkan `id`, terbaru lebih dahulu).

Di sisi database jalankan `db/0016_notifications.sql` (tabel + trigger `reports_notify_found` untuk kabar "barang temuan baru") lalu `db/0017_realtime.sql` (mendaftarkan tabel ke publication `supabase_realtime`). Pada mode demo offline (tanpa `SUPABASE_URL`), notifikasi dibuat langsung di perangkat dan tetap tampil di kedua lapisan.

Aturan yang diuji di `SystemNotifyingNotificationRepositoryTest`:

1. Hanya kabar milik pengguna aktif (staf yang memutuskan klaim tidak menerima kartu milik pengklaim di perangkatnya).
2. Hanya kabar yang belum dibaca; menandai dibaca tidak memunculkan kartu ganda.
3. Riwayat lama (data demo atau baris Supabase saat aplikasi dibuka) tidak dikirim ulang.
4. Sakelar **Notifikasi luar aplikasi** di halaman Akun menghormati pilihan pengguna: saat mati tidak ada kartu baru, kartu yang sudah tampil dibersihkan, dan kabar yang tiba selama sakelar mati tidak menyusul ketika dinyalakan lagi.

Ketuk kartu untuk membuka layar Notifikasi (`launchMode="singleTop"` + extra intent). Di Android 13+ izin `POST_NOTIFICATIONS` diminta sekali setelah login; bila ditolak, kotak masuk in-app tetap berfungsi. Kanal notifikasi bernama **Kabar Ketemu** dan bisa dimatikan dari Pengaturan Android.

Sakelar ada di **Akun > Pengaturan > Notifikasi luar aplikasi**, aktif secara bawaan pada pemasangan baru dan pilihannya disimpan di `SharedPreferences` (`SharedPreferencesSystemNotificationSettings`). Menyalakannya meminta izin `POST_NOTIFICATIONS` bila belum ada; bila izin ditolak, sakelar kembali mati. Ketika izin sudah ada tetapi notifikasi aplikasi diblokir dari Pengaturan Android, layar Akun menampilkan petunjuk dan tombol **Buka pengaturan**. Mematikan sakelar tidak menghapus kotak masuk in-app - kabar tetap tersimpan di lonceng Beranda.

## Pencocokan otomatis (hilang <-> temuan)

Pencocokan dihitung **di server**, bukan di perangkat, supaya tetap berjalan walau aplikasi tertutup dan hasilnya sama untuk semua pengguna:

1. Setiap laporan baru memicu trigger `reports_auto_match` (`db/0018_auto_match.sql`).
2. Kandidat = laporan aktif berjenis sebaliknya, kategori sama, pemilik berbeda, dan judulnya berbagi kata kunci (minimal 4 huruf) **atau** titik GPS-nya berjarak <= 250 m.
3. Pasangan dicatat di tabel `matches` (unik per pasangan, tanpa policy tulis untuk klien) dan pemilik laporan lama menerima notifikasi `POSSIBLE_MATCH` yang ikut terkirim lewat Realtime.
4. Halaman detail menampilkan bagian **"Mungkin barangmu"** (atau "Mungkin pemiliknya" pada laporan temuan) berisi kartu kandidat; ketuk untuk membuka detailnya. Tombol klaim belum ada pada tahap ini (menyusul Tahap 4).

Pada mode demo offline, `DummyData.matches()` menyediakan satu pasangan contoh agar bagian tersebut langsung terlihat saat demo.

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
