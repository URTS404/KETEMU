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

Berkas `0004`–`0009` hanya berisi data contoh dan perbaikan; jalankan bila
project Anda berasal dari versi skema sebelumnya.

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
