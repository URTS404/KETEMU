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

## Belum dikerjakan (Tahap 2)

1. Ikon gambar di kolom pencarian Beranda (`HomeScreen.kt:277`) → pilih foto dari galeri.
2. Lembar hasil: kartu bergambar + "Mirip 87%" + tombol buka detail / "Ini barang saya".
3. Aturan **foto wajib untuk laporan bertipe Ditemukan** (tanpa ini, pencarian
   foto tidak punya pembanding — saat ini 0 dari 12 laporan punya foto).
4. Cadangan pencarian teks (`pg_trgm`) bila laporan belum punya vektor.
5. Hapus kait sementara di `KetemuApp.onCreate` (uji model saat start).

## Batasan yang perlu diketahui

- Label/rupa yang dikenali bersifat umum (bentuk & warna), bukan merek.
- Sidik foto hanya dibuat saat laporan dikirim **daring**. Laporan yang disimpan
  offline saat ini tidak menyertakan vektor; ini bisa ditambal dengan menyimpan
  vektor di tabel outbox lalu mengirimnya saat sinkronisasi.
- Model 4,1 MB menambah ukuran APK; bila APK harus kecil, model bisa diunduh
  sekali saat pertama memakai fitur (tetap tanpa akun, tetapi butuh hosting berkas).
