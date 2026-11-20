# Lens — catatan Tahap 1 (mesin pencarian foto)

Diperbarui: 20 November 2026. Gambaran singkat: fitur "cari barang dengan foto"
seperti Google Lens, dikerjakan **sepenuhnya di dalam HP** — tanpa layanan awan,
tanpa akun tambahan, tanpa biaya.

## Yang sudah selesai (Tahap 1)

| Lapisan | Berkas | Isi |
|---|---|---|
| Model | `app/src/main/assets/mobilenet_v3_small.tflite` | MobileNetV3 kecil (4,1 MB), keluaran vektor 1024 angka |
| Hitung vektor | `core/visual/PencariVisual.kt` | Muat model TFLite sekali per proses, bitmap → vektor ternormalisasi L2 |
| Ambil foto | `core/visual/VektorFoto.kt` | URI (`content://`) → bitmap (dikecilkan bila > 1600 px) → vektor |
| Penyimpanan | `db/0032_lens.sql` | Kolom `reports.photo_embedding vector(1024)`, indeks HNSW kosinus, dua fungsi |
| Repositori | `ReportRepository.simpanVektorFoto()` + `RoomReportRepository` | Panggil RPC `simpan_vektor_foto` setelah laporan tersimpan |
| Alur buat laporan | `CreateReportViewModel` | Setelah `addReport`, sidik foto dihitung lalu dikirim (gagal = tidak fatal) |
| Wiring | `AppContainer.hitungVektorFoto`, `ViewModelFactories` | Lambda agar ViewModel tidak memegang Context |

## Bukti uji Tahap 1

1. **Model di perangkat** — logcat: `model mobilenet_v3_small.tflite siap: masuk=[1, 224, 224, 3] keluar=[1, 1024]`,
   lalu `uji vektor: dimensi=1024 norma=1.000` (dihitung dari bitmap 32×32 di emulator).
2. **Migrasi database** — lima baris verifikasi di `db/0032_lens.sql` semuanya `true`.
3. **Validasi fungsi** — vektor 3 angka ditolak `22023: vektor harus berisi 1024 angka, diterima 3`.
4. **Pencarian SQL** — vektor sintetis ditanam pada satu laporan temuan:
   `cari_barang_mirip` mengembalikan laporan itu dengan `skor = 100`; dengan vektor
   lain `skor = 0`. Vektor sintetis lalu dikosongkan kembali (`sisa_vektor = 0`).
5. **RPC lewat PostgREST** — `simpan_vektor_foto` dengan id palsu mengembalikan
   `false` (tidak menyentuh baris siapa pun); `cari_barang_mirip` mengembalikan
   baris lengkap dengan `pos_name` dan `skor`.

## Kontrak database

```sql
-- menyimpan sidik foto (hanya pemilik laporan atau petugas)
select public.simpan_vektor_foto(:report_id, :vektor_double_array) -> boolean

-- mencari laporan temuan yang rupanya mirip
select * from public.cari_barang_mirip(:vektor_double_array, p_batas := 20, p_ambang := 0.25)
-- kolom: id, title, photo_url, status, pos_name, created_at, skor (0..100)
```

Syarat pencarian: `type = 'FOUND'`, `is_archived = false`, `photo_embedding`
tidak kosong, dan kemiripan ≥ `p_ambang`. Foto asli tetap di Storage Supabase;
yang disimpan di kolom hanya 1024 angka hasil hitungan perangkat.

## Yang sudah selesai (Tahap 2)

1. Ikon gambar di kolom pencarian Beranda (`HomeScreen.kt`) → `PickVisualMedia`
   membuka galeri, foto dihitung jadi vektor, lalu `cari_barang_mirip` dipanggil.
2. Lembar hasil `CariFotoSheet`: kartu bergambar, label "Mirip 87%", nama pos,
   dan kartu bisa diketuk untuk membuka detail laporan.
3. Aturan **foto wajib untuk laporan bertipe Ditemukan**: tombol Simpan menolak
   laporan temuan tanpa foto (`CreateReportScreen.kt`), dengan hint
   "Foto wajib untuk laporan barang temuan." dan tombol "Pilih dari galeri" di
   samping "Ambil Foto Barang".
4. Kait sementara di `KetemuApp.onCreate` sudah dihapus; model kini dimuat saat
   fitur dipakai.

Bukti uji di `emulator-5554` (Siti): membuat laporan temuan berfoto dari galeri →
logcat `sidik foto <id>: tersimpan`; mencari dengan foto yang sama → logcat
`cariDenganFoto: 1 hasil` dan kartu menampilkan **Mirip 89%**. Laporan uji lalu
dihapus lewat API dan server kembali ke 12 laporan.

## Belum dikerjakan

1. Cadangan pencarian teks (`pg_trgm`) bila laporan belum punya vektor.
2. Menyimpan vektor untuk laporan yang dibuat **offline** (lihat batasan).
3. Unggah foto ke Storage Supabase; saat ini `photo_url` masih URI lokal
   perangkat, jadi foto tidak terlihat di perangkat lain.

## Batasan yang perlu diketahui

- Label/rupa yang dikenali bersifat umum (bentuk & warna), bukan merek.
- Sidik foto hanya dibuat saat laporan dikirim **daring**. Laporan yang disimpan
  offline saat ini tidak menyertakan vektor; ini bisa ditambal dengan menyimpan
  vektor di tabel outbox lalu mengirimnya saat sinkronisasi.
- Model 4,1 MB menambah ukuran APK; bila APK harus kecil, model bisa diunduh
  sekali saat pertama memakai fitur (tetap tanpa akun, tetapi butuh hosting berkas).
- **Peringatan 16 KB page size**: di emulator Android 16/17 (`android-37.2`,
  halaman 16 KB) muncul dialog "This app isn't 16 KB compatible" karena
  `libtensorflowlite_jni.so` dari TensorFlow Lite 2.16.1 belum selaras 16 KB
  (juga `libandroidx.graphics.path.so`). Aplikasi tetap berjalan, tetapi untuk
  rilis perlu naik ke LiteRT/TFLite yang sudah 16 KB-aligned.
