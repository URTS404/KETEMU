# Database KETEMU v2 (Supabase)

Berkas SQL siap dijalankan manual di **Supabase SQL Editor**. Urutannya penting.

| Berkas | Isi |
|---|---|
| `0001_schema.sql` | Tipe enum, tabel `profiles`, `categories`, `pos_locations`, `reports`, `claims`, `audit_logs`, trigger profil otomatis, trigger audit, trigger kode pengambilan |
| `0002_rls.sql` | Fungsi bantu `is_staff()`/`is_admin()`, Row Level Security semua tabel, penjaga kolom sensitif |
| `0003_seed.sql` | Master data kategori + 4 pos barang temuan |
| `0004_seed_reports.sql` | Laporan contoh untuk demo |
| `0005_promote_staff.sql` | Promosi akun petugas (`STAFF`) |
| `0006_promote_staff_fix.sql` | Perbaikan promosi petugas |
| `0007_seed_claim.sql` | Klaim contoh untuk antrean admin |
| `0008_selfcheck.sql` | Query pemeriksaan cepat setelah setup |
| `0009_fix_audit_enum_cast.sql` | Perbaikan cast enum pada trigger audit |
| `0010_report_coordinates.sql` | Kolom `latitude`/`longitude` untuk titik peta |
| `0011_admin_management.sql` | Pemisahan hak akses STAFF vs ADMIN (hak tambahan khusus admin) |
| `0012_admin_audit.sql` | Audit untuk aksi ADMIN pada akun pengguna |
| `0013_demo_admin_staff.sql` | Data demo untuk memamerkan pemisahan hak STAFF vs ADMIN |
| `0014_account_status.sql` | Pemeriksaan status blokir untuk akun yang sedang login |
| `0015_staff_keys.sql` | Kunci staf (undangan sekali pakai) + penjagaan domain kampus |
| `0016_notifications.sql` | Tabel `notifications`, RLS, dan trigger `reports_notify_found` (konfirmasi barang temuan + siaran barang temuan baru) |
| `0017_realtime.sql` | Mendaftarkan `notifications` ke publication `supabase_realtime` agar kabar baru dikirim seketika (Realtime) |
| `0018_auto_match.sql` | Tabel `matches`, RLS baca-saja, dan trigger `reports_auto_match` (pencocokan otomatis + notifikasi `POSSIBLE_MATCH`) |
| `0019_messages.sql` | Tabel `messages` + akses percakapan dua arah pada satu laporan |
| `0020_chat_view.sql` | View `chat_threads` (ringkasan percakapan per laporan) |
| `0021_healthcheck.sql` | Query pemeriksaan cepat untuk kondisi chat dan notifikasi |
| `0022_chat_images.sql` | Indeks lampiran gambar pada `messages` |
| `0023_chat_storage.sql` | Bucket Storage + policy baca/tulis gambar chat |
| `0024_message_delete.sql` | Policy hapus pesan milik sendiri |
| `0025_seed_demo.sql` | Data contoh tambahan untuk demo dan penilaian |
| `0026_fix_can_access_conversation.sql` | Perbaikan `can_access_conversation` untuk lampiran gambar chat |
| `0027_typing_state.sql` | Tabel + fungsi `set_typing`/`peer_typing` untuk indikator "sedang menulis…" (kedaluwarsa 5 detik) |
| `0028_serah_terima.sql` | Kolom `handed_over_at`/`handed_over_by`, enum audit `SERAH_TERIMA_BARANG`, dan fungsi `complete_handover` (petugas mencatat barang sudah diambil → laporan temuan & hilang ditutup) |
| `0029_claim_resubmit.sql` | Indeks unik parsial `claims_one_active_per_user` menggantikan `claims_unique_active`, agar klaim yang ditolak boleh diajukan ulang |
| `0030_pengingat_pengambilan.sql` | Tabel `pickup_reminders` + fungsi `kirim_pengingat_pengambilan()` yang dijadwalkan `pg_cron` tiap jam: pengingat hari ke-3 dan ke-6 untuk klaim yang disetujui tapi barangnya belum diambil |
| `0031_hapus_laporan.sql` | Kebijakan `reports_delete_own`: pemilik boleh menghapus laporannya sendiri selama tidak ada klaim `MENUNGGU`/`DISETUJUI` |
| `0032_lens.sql` | Cari barang dengan foto: ekstensi pgvector, kolom `photo_embedding`, fungsi `simpan_vektor_foto` & `cari_barang_mirip` |
| `0033_foto_storage_klaim.sql` | Bucket `foto-laporan` + kebijakan unggah/ubah/hapus, dan `claims_delete_own_pending` (pengklaim boleh membatalkan klaim selagi MENUNGGU) |
| `0034_email_kampus.sql` | Trigger `wajib_email_kampus` di `auth.users`: pendaftaran wajib memakai email `@umn.ac.id`/`@student.umn.ac.id`, sementara verifikasi email tetap dimatikan |

## 1. Buat project

1. Buka <https://supabase.com/dashboard> → **New project**.
2. Region: **Southeast Asia (Singapore)** — paling dekat ke Indonesia.
3. Simpan **database password** di tempat aman (tidak dipakai aplikasi, hanya untuk akses DB langsung).
4. Tunggu provisioning selesai (± 2 menit).

## 2. Aktifkan login email

**Authentication → Providers → Email**: aktifkan, dan untuk demo matikan
**Confirm email** agar akun yang didaftarkan langsung bisa dipakai.

**Authentication → URL Configuration** tidak perlu diubah untuk aplikasi Android.

## 3. Jalankan SQL

Di **SQL Editor**, buka berkas lalu copy-paste isinya, tekan **Run**, urut:

1. `db/0001_schema.sql`
2. `db/0002_rls.sql`
3. `db/0003_seed.sql`
4. `db/0010_report_coordinates.sql` — wajib bila memakai fitur titik peta
5. `db/0016_notifications.sql` lalu `db/0017_realtime.sql` — kotak masuk notifikasi dan pengiriman kabar seketika (Realtime)
6. `db/0018_auto_match.sql` - pencocokan otomatis hilang <-> temuan + notifikasi "Mungkin barangmu"
7. `db/0019_messages.sql` lalu `db/0020_chat_view.sql` - pesan dua arah per laporan
8. `db/0022_chat_images.sql` lalu `db/0023_chat_storage.sql` - lampiran foto di chat
9. `db/0024_message_delete.sql` - hapus pesan sendiri
10. `db/0026_fix_can_access_conversation.sql` - wajib bila langkah 8 dipakai
11. `db/0027_typing_state.sql` - indikator "sedang menulis..." di dalam percakapan
12. `db/0028_serah_terima.sql` - serah terima barang oleh petugas pos (menutup laporan temuan + hilang)
13. `db/0029_claim_resubmit.sql` - klaim yang ditolak boleh diajukan ulang
14. `db/0030_pengingat_pengambilan.sql` - pengingat pengambilan otomatis (butuh ekstensi `pg_cron`)
15. `db/0031_hapus_laporan.sql` - pemilik boleh menghapus laporan sendiri
16. `db/0032_lens.sql` - mesin pencarian foto (pgvector)
17. `db/0033_foto_storage_klaim.sql` - bucket foto laporan + batal klaim
18. `db/0034_email_kampus.sql` - syarat email kampus saat mendaftar

Berkas `0004`–`0009` serta `0021` dan `0025` hanya berisi data contoh dan
pemeriksaan; jalankan bila perlu.

Semua berkas aman dijalankan ulang (memakai `create ... if not exists`,
`create or replace`, `drop policy if exists`, dan `on conflict do update`).

## 4. Hubungkan aplikasi

**Project Settings → API** menyediakan dua nilai:

- `Project URL` → `SUPABASE_URL`
- `Publishable key` (atau `anon public`) → `SUPABASE_PUBLISHABLE_KEY`

Isi `local.properties` di root project (berkas ini **tidak** ikut git):

```properties
SUPABASE_URL=https://xxxxxxxxxxxx.supabase.co
SUPABASE_PUBLISHABLE_KEY=sb_publishable_...
```

Selama `SUPABASE_URL` kosong, aplikasi tetap memakai autentikasi lokal + data
in-memory (mode demo offline). Begitu diisi, tahap integrasi Supabase diaktifkan.

> **Jangan pernah** menaruh `service_role` key di aplikasi atau mengirimkannya ke
> percakapan. Kunci itu melewati RLS dan hanya untuk operasi server.

## 5. Promosikan akun petugas

Daftarkan akun petugas lewat aplikasi (email kampus), lalu jalankan di SQL Editor:

```sql
update public.profiles set role = 'STAFF', is_verified = true
 where email = 'rahmat@umn.ac.id';
```

Akun `ADMIN` mengikuti pola yang sama dengan `role = 'ADMIN'`.

## 6. Cek cepat setelah SQL

```sql
-- harus 6 tabel
select table_name from information_schema.tables
 where table_schema = 'public'
 order by table_name;

-- harus sama dengan jumlah tabel (RLS aktif di semua tabel)
select count(*) from pg_tables
 where schemaname = 'public' and rowsecurity = true;

-- seed terisi 4 + 4
select count(*) from public.categories;
select count(*) from public.pos_locations;
```

## Catatan desain

- **Satu sumber kebenaran status**: `reports.status` mengikuti timeline
  `DILAPORKAN → DISERAHKAN_KE_POS → SIAP_DIAMBIL → SELESAI_DIKLAIM`.
- **Klaim disetujui** otomatis memindahkan laporan ke `SIAP_DIAMBIL` lewat trigger
  `apply_claim_to_report`, dan menerbitkan `pickup_code` 6 karakter dari hash id klaim.
- **Audit log append-only**: ditulis oleh trigger (SECURITY DEFINER), tanpa policy
  UPDATE/DELETE, sehingga tidak bisa ditimpa dari klien.
- **Pencegahan eskalasi peran**: trigger `guard_profile_columns` mengunci
  `role`, `is_verified`, dan `reputation` agar hanya bisa diubah ADMIN.
- **Pemilik laporan** tidak bisa mengubah `status`/`is_archived` (dijaga
  `guard_report_columns`), sehingga hanya petugas yang bisa memindahkan timeline.
