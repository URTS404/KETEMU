================================================================================
 KETEMU - Campus Lost & Found Hub
 IF570L Lab Mobile Application Programming - Mid Exam (UTS) 2026/2027
================================================================================

1. DESKRIPSI SINGKAT APLIKASI
--------------------------------------------------------------------------------
Ketemu adalah aplikasi Android bergaya "lost & found" untuk lingkungan kampus
(closed-ecosystem) yang membantu mahasiswa dan staf melaporkan, mencari, dan
mengklaim barang hilang maupun barang temuan di area kampus.

Alur utama:
  - Pengguna masuk memakai email kampus.
  - Melaporkan barang HILANG atau barang TEMUAN lengkap dengan foto, kategori,
    dan lokasi terakhir terlihat/ditemukan.
  - Menelusuri daftar laporan di Beranda (lihat semua) dan Aktivitas (milik
    sendiri), dengan pencarian dan filter Hilang/Ditemukan.
  - Membuka Detail laporan untuk melihat status barang dan perjalanannya
    (timeline: Dilaporkan -> Diserahkan ke Pos -> Siap Diambil -> Selesai
    Diklaim).
  - Aplikasi tetap dapat dipakai saat offline (indikator status offline).
  - Akun uji coba tampil langsung di layar Login (tidak perlu membuka kode
    sumber). Bila aplikasi terhubung ke Supabase, gunakan akun berikut:
        Mahasiswa : siti.uas@student.umn.ac.id | sandi: rahasia123
        Staf      : rahmat@umn.ac.id           | sandi: rahasia123
    NIM/NIDN (00000999888 dan 001234567) HANYA kolom identitas, BUKAN kata sandi.
  - Tanpa Supabase (mode demo offline) aplikasi memakai akun lokal:
        Mahasiswa : budi.santoso@student.umn.ac.id | sandi: 00000012345
        Staf      : rahmat@umn.ac.id               | sandi: STAFF-001

Dibangun dengan arsitektur MVVM berlapis
(UI - ViewModel - Domain - Data), Jetpack Compose, dan Navigation Compose.
Saat terhubung ke Supabase, data laporan, klaim, percakapan, dan log aktivitas
tersimpan di PostgreSQL (PostgREST + Realtime + Storage). Bila Supabase belum
dikonfigurasi, aplikasi memakai repository dalam memori dengan data contoh.


2. NAMA APLIKASI & KELOMPOK
--------------------------------------------------------------------------------
Nama Aplikasi  : Ketemu
Nama Kelompok  : Kelompok 5


3. DAFTAR ANGGOTA KELOMPOK
--------------------------------------------------------------------------------

1. Brilliant Hafizd Hermawan (00000130613)
2. Kevin Ivan Setiadi Phang (00000131636)
3. Indra Rizky Raharja (00000133155)
4. Awang Raditya Pratama (00000133275)


4. PEMBAGIAN TUGAS & SISTEM KERJA KELOMPOK
--------------------------------------------------------------------------------
Pembagian tugas disusun berdasarkan aliran kerja (workstream) agar setiap
anggota memiliki lingkup tanggung jawab yang jelas dan tidak tumpang tindih.

  a. Product & Flow Design
     Perumusan ide produk, value proposition, dan alur pengguna end-to-end.
  b. Engineering & UI Design
     Implementasi inti aplikasi, arsitektur MVVM, custom Material 3 theme,
     serta perancangan antarmuka.
  c. Quality Assurance & Debugging
     Pengujian fungsional, penelusuran defect, dan verifikasi build.
  d. Project Coordination & Review
     Perencanaan kerja, pemantauan progres, dan review silang antar anggota.

Sistem kerja kelompok:
  1. Perencanaan
     Ruang lingkup fitur dan kriteria selesai (definition of done) disepakati
     di awal, mengacu pada proposal dan rubrik penilaian.
  2. Pelaksanaan
     Setiap anggota mengerjakan modul/layar sesuai tanggung jawabnya, kemudian
     hasil kerja diintegrasikan secara bertahap ke dalam satu basis kode.
  3. Review
     Setiap penambahan fitur ditinjau anggota lain sebelum digabungkan, dengan
     fokus pada konsistensi tampilan, kelengkapan state, dan kualitas kode.
  4. Verifikasi
     Sebelum build APK final, seluruh alur utama diuji ulang (masuk -> lapor
     barang -> pelacakan status -> detail laporan) untuk memastikan tidak ada
     regresi.
  5. Dokumentasi
     Keputusan teknis serta pembagian tugas dicatat agar dapat
     dipertanggungjawabkan saat presentasi dan penilaian.


5. KONTRIBUSI SPESIFIK SETIAP ANGGOTA
--------------------------------------------------------------------------------

1. Brilliant Hafizd Hermawan (00000130613) - Product & Flow Design
   - Merumuskan ide dan value proposition aplikasi Ketemu sebagai pusat layanan
     lost & found civitas akademika dalam satu ekosistem terpusat.
   - Menyusun alur aplikasi end-to-end: masuk -> lapor barang -> pelacakan
     status -> pengambilan/klaim, termasuk penetapan empat tahap status barang
     (Dilaporkan, Diserahkan ke Pos, Siap Diambil, Selesai Diklaim).
   - Menetapkan prioritas fitur sebagai acuan ruang lingkup pekerjaan tim.

2. Kevin Ivan Setiadi Phang (00000131636) - Lead Engineer & UI Designer
   - Bertanggung jawab atas implementasi utama aplikasi dengan arsitektur MVVM
     berlapis (ui / domain / data / core).
   - Merancang design system dan custom Material 3 theme (skema warna brand,
     tipografi, dan shape) beserta aset logo, ikon peluncur, dan splash screen.
   - Membangun layar Beranda, Detail Laporan, Buat Laporan, Pesan/Percakapan,
     Profil, dan layar autentikasi, termasuk navigation graph aplikasi.
   - Menyiapkan model domain serta dummy data sebagai sumber data prototipe.

3. Indra Rizky Raharja (00000133155) - Quality Assurance & Debugging
   - Melakukan pengujian fungsional pada setiap layar dan alur navigasi,
     termasuk pengiriman argumen dari layar daftar ke layar detail.
   - Menelusuri dan memperbaiki defect pada layout, state yang tidak
     ter-update, serta validasi form.
   - Menyusun skenario pengujian dan memverifikasi build APK sebelum
     penyerahan.

4. Awang Raditya Pratama (00000133275) - Project Coordinator & Reviewer
   - Mengelola jadwal kerja, memantau progres, dan memastikan hasil kerja
     selaras dengan proposal serta rubrik penilaian.
   - Melakukan review silang terhadap pekerjaan anggota: konsistensi tampilan,
     kelengkapan fitur, dan kerapian struktur kode.
   - Mengoordinasikan kelengkapan deliverables (source code, APK, README, dan
     video demo).


6. RINGKASAN FITUR YANG SUDAH DIBUAT (untuk memetakan ke rubrik)
--------------------------------------------------------------------------------
  - Layar list       : Beranda (Home) dan Aktivitas / Laporan Saya
  - Layar detail     : Detail Laporan + timeline status barang
  - Layar form/input : Buat Laporan (jenis, foto, kategori, lokasi) +
                       Login, Daftar Akun, Lupa Kata Sandi
  - Layar fitur native: pengambilan foto barang (integrasi kamera memakai
                       ActivityResultContracts.TakePicture + izin CAMERA,
                       dengan penanganan saat izin ditolak / kamera tidak
                       tersedia)
  - Layar pilihan bebas: Profil (Akun) dan Pesan
  - Custom Material 3 theme: warna, tipografi, dan shape (lihat ui/theme)
  - State management : ViewModel + StateFlow + rememberSaveable
  - Navigation       : NavHost/NavController dengan argumen reportId
                       (List -> Detail) yang dibaca dinamis
  - Validasi form    : tipe keyboard sesuai pada layar autentikasi
                       (Email/Number/Phone/Password) + validasi domain email
                       kampus dan konfirmasi kata sandi; isError + supporting
                       text; tombol simpan diblokir selama input belum valid
  - Dummy data       : dimodelkan sebagai data class (Report, PosLocation,
                       UserProfile) dengan variasi status


7. ARSITEKTUR & TECH STACK
--------------------------------------------------------------------------------
  - Bahasa          : Kotlin
  - UI              : Jetpack Compose (Material 3)
  - Arsitektur      : MVVM berlapis (ui / domain / data / core)
  - Navigasi        : Navigation Compose (NavHost + NavController)
  - State           : ViewModel + StateFlow, rememberSaveable
  - Asinkron        : Kotlin Coroutines
  - Struktur paket  :
        core/     -> utilitas bersama (network, common state)
        domain/   -> model + kontrak repository (murni, tanpa Android)
        data/     -> implementasi repository + dummy data + sesi
        ui/       -> layar Compose, komponen, tema, navigasi, ViewModel


8. CARA MEMBUKA & MENJALANKAN
--------------------------------------------------------------------------------
Prasyarat: Android Studio versi terbaru (AGP 9 + Kotlin bawaan), JDK 17 atau
lebih baru (JDK bawaan Android Studio sudah memenuhi), dan Android SDK dengan
compileSdk 37. Android Studio akan menawarkan unduhan komponen yang belum ada.

  1. Buka folder project ini di Android Studio (File > Open).
  2. Build debug APK:
        Windows     : gradlew.bat assembleDebug
        macOS/Linux : ./gradlew assembleDebug
     Hasil: app/build/outputs/apk/debug/app-debug.apk
  3. Pasang ke perangkat/emulator: gradlew.bat installDebug
  4. Atau langsung pasang berkas APK yang disertakan pada perangkat Android
     (aktifkan "Install unknown apps" bila diminta).

Catatan build:
  - Output build memakai lokasi standar Gradle (build/ dan app/build/), jadi
    proyek dapat langsung di-build di komputer mana pun tanpa path khusus dan
    tanpa syarat izin tulis ke folder di luar proyek.
  - Berkas local.properties (berisi lokasi Android SDK komputer pembuat) TIDAK
    disertakan dalam arsip; Android Studio membuatnya otomatis saat project
    dibuka. Bila build dijalankan dari terminal dan muncul pesan "SDK location
    not found", tambahkan baris  sdk.dir=<lokasi Android SDK>  pada
    local.properties atau set variabel lingkungan ANDROID_HOME.
  - Bila arsip dijalankan dari folder yang di-sync cloud dan build gagal acak
    karena berkas dikunci proses sinkronisasi, alihkan output ke luar folder
    tersebut dengan menambahkan baris berikut pada gradle.properties:
        ketemu.buildDir=D:/AndroidBuilds/Ketemu
    (lihat komentar "LOKASI OUTPUT BUILD (OPSIONAL)" di build.gradle.kts).
    Tanpa baris itu, pengalihan tidak aktif sama sekali.


9. FILE YANG DISERTAKAN DALAM ARSIP
--------------------------------------------------------------------------------
  - Seluruh source code project (folder project lengkap)
  - app-debug.apk (APK siap pasang)
  - README.txt (berkas ini)
  - Video demo (maksimal 5 menit) yang memetakan setiap kriteria rubrik

  Bila arsip dibuat dalam bentuk ZIP, folder/berkas berikut TIDAK perlu ikut
  (dapat dibuat ulang otomatis oleh Android Studio): build/, app/build/,
  .gradle/, .kotlin/, .idea/, dan local.properties.

================================================================================
10. FITUR YANG SUDAH BERJALAN
--------------------------------------------------------------------------------
Selain daftar laporan (UC utama UTS), aplikasi kini memiliki:

  a. Autentikasi terpusat (Supabase Auth)
     - Daftar/masuk memakai email kampus (domain umn.ac.id / student.umn.ac.id).
     - Sesi disimpan, sehingga aplikasi tidak minta login ulang setiap dibuka.
     - Akun yang diblokir admin tidak dapat masuk ("Akun ini diblokir oleh admin").

  b. Chat penemu - pemilik (Realtime)
     - Tombol "Chat penemu" / "Chat orang kehilangan" pada Detail laporan.
     - Pesan teks + gambar, terkirim seketika lewat Supabase Realtime.
     - Gambar dikompres otomatis (WebP, sisi terpanjang 1280 px) sebelum diunggah.
     - Ketuk gambar di chat untuk memperbesar (cubit 1x-5x, geser, ketuk dua kali).
     - Antrean offline: pesan/gambar yang dikirim tanpa jaringan tampil buram
       dengan ikon putar ulang, lalu terkirim otomatis saat internet kembali.
     - Tekan lama / menu titik-tiga pada pesan sendiri untuk menghapus.
     - Indikator "sedang menulis..." tampil pada lawan bicara (tabel
       `typing_state`, `db/0027_typing_state.sql`) dan hilang 5 detik setelah
       pengirim berhenti mengetik.
     - Daftar pesan menggulir otomatis ke pesan terbaru saat pesan masuk.

  c. Panel admin (pemisahan hak STAFF dan ADMIN)
     STAFF : moderasi laporan, memutuskan klaim (setuju/tolak),
             melihat log aktivitas, melihat statistik.
     ADMIN : semua di atas + kelola pengguna (ubah peran, verifikasi akun,
             blokir/buka blokir) dan membuat kunci staf.

  d. Klaim barang dan kode pengambilan
     - Pengaju klaim menuliskan jawaban verifikasi.
     - Petugas menyetujui/menolak; saat disetujui sistem membuat
       kode pengambilan 6 karakter (mis. 414A0D) dan status laporan berubah
       menjadi Siap Diambil secara otomatis (trigger database).

  e. Kunci staf sekali pakai
     - Admin membuat 10 kunci (format XXXX-XXXX).
     - Calon staf menebus kunci di layar Akun; peran berubah menjadi STAFF.
     - Kunci hangus setelah dipakai dan tercatat siapa pemakainya.

  f. Lokasi & peta
     - Titik koordinat laporan (latitude/longitude) dan peta OpenStreetMap.

  g. Notifikasi
     - Kotak masuk notifikasi + kartu notifikasi Android untuk laporan baru.

  h. Pencocokan awal (berbasis aturan)
     - Barang hilang dan temuan dengan judul serupa ditautkan otomatis
       oleh trigger database (bukan pembandingan gambar).

  i. Log aktivitas (audit trail) permanen
     - Setiap aksi moderator dicatat otomatis oleh trigger database;
       tidak dapat diubah atau dihapus dari aplikasi.


================================================================================
11. DATABASE (Supabase / PostgreSQL)
--------------------------------------------------------------------------------
Skrip SQL dijalankan berurutan di Supabase -> SQL Editor:

  0001_schema.sql                 tabel, enum, trigger dasar
  0002_rls.sql                   Row Level Security + penjaga kolom
  0003_seed.sql                  4 kategori + 4 pos barang temuan
  0004_seed_reports.sql          contoh laporan (2)
  0005, 0006                     promosi akun staf
  0007_seed_claim.sql            contoh klaim
  0008_selfcheck.sql             12 pemeriksaan mandiri
  0009_fix_audit_enum_cast.sql   perbaikan cast enum audit
  0010_report_coordinates.sql    kolom latitude/longitude
  0011_admin_management.sql      is_banned, audit akun, policy admin
  0012_admin_audit.sql           trigger audit perubahan profil
  0013_demo_admin_staff.sql      akun demo STAFF/ADMIN
  0014_account_status.sql        cek status blokir saat login
  0015_staff_keys.sql            kunci staf sekali pakai
  0016_notifications.sql         tabel + trigger notifikasi
  0017_realtime.sql              aktifkan Realtime
  0018_auto_match.sql            pencocokan judul otomatis
  0019_messages.sql              percakapan + pesan
  0020_chat_view.sql             view chat_threads (nama lawan bicara)
  0021_healthcheck.sql           pemeriksaan kesehatan database
  0022_chat_images.sql           kolom gambar + bucket storage
  0023_chat_storage.sql          policy storage chat-images
  0024_message_delete.sql        izin hapus pesan sendiri
  0025_seed_demo.sql             data demo (8 laporan, 2 klaim, 1 chat)
  0026_fix_can_access_conversation.sql  fungsi akses percakapan (SECURITY DEFINER)

Konfigurasi aplikasi ada di local.properties (tidak ikut repositori):
  SUPABASE_URL=...              URL proyek
  SUPABASE_PUBLISHABLE_KEY=...   kunci publik (aman untuk klien)
Kunci secret/service_role TIDAK boleh dimasukkan ke aplikasi.


================================================================================
12. CARA MENJALANKAN DAN UJI CEPAT
--------------------------------------------------------------------------------
Prasyarat: Android Studio (JDK 17+), Android SDK 37, emulator/HP minSdk 26.

  1. git clone https://github.com/URTS404/KETEMU.git
  2. Buka proyek di Android Studio, biarkan Gradle Sync selesai.
  3. Isi local.properties dengan SUPABASE_URL dan SUPABASE_PUBLISHABLE_KEY
     (bila kosong, aplikasi berjalan dalam mode demo offline).
  4. Jalankan skrip database 0001 - 0026 di SQL Editor Supabase.
  5. Run aplikasi.

Uji cepat (5 menit):
  - Masuk: rahmat@umn.ac.id / rahasia123 (ADMIN).
  - Beranda menampilkan 10 laporan demo.
  - Pesan -> pilih percakapan -> kirim teks dan gambar.
  - Matikan data (mode pesawat) -> kirim pesan -> muncul buram + ikon putar
    ulang -> nyalakan data -> pesan terkirim otomatis.
  - Akun -> Buka Panel Admin -> Moderasi laporan / Verifikasi klaim.
  - Setujui klaim -> muncul kode pengambilan 6 karakter.


================================================================================
13. BATASAN YANG DIKETAHUI
--------------------------------------------------------------------------------
  - Daftar laporan belum dapat dibaca saat offline (chat sudah punya antrean
    offline; laporan baru juga sudah diantrekan, tetapi daftar butuh jaringan).
  - URL gambar chat berlaku 1 jam; bila kedaluwarsa gambar perlu dimuat ulang
    dengan tombol muat ulang di kanan atas percakapan.
  - Pencocokan barang masih berbasis kemiripan judul, belum membandingkan foto.
  - Pelaporan lokasi memakai koordinat dari peta, belum navigasi ke lokasi.
