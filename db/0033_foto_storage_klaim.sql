-- 0033: foto laporan masuk Storage, pengklaim bisa membatalkan klaim,
--        dan petugas bisa menyimpan alasan penolakan.
--
-- Sebelum ini `reports.photo_url` diisi URI lokal perangkat
-- (content://media/picker/...), sehingga foto hanya terlihat oleh pelapor.
-- Sekarang foto diunggah ke bucket publik `foto-laporan` dan kolomnya berisi
-- URL https yang bisa dibaca semua pengguna.
--
-- Jalankan di Supabase SQL Editor (atau lewat Management API).

-- ── 1. Bucket foto laporan ─────────────────────────────────────────────────
insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values ('foto-laporan', 'foto-laporan', true, 5242880,
        array['image/jpeg', 'image/png', 'image/webp'])
on conflict (id) do update
    set public             = true,
        file_size_limit    = 5242880,
        allowed_mime_types = array['image/jpeg', 'image/png', 'image/webp'];

-- Unggah hanya ke folder milik sendiri: path "foto-laporan/<user_id>/<berkas>".
drop policy if exists foto_laporan_unggah on storage.objects;
create policy foto_laporan_unggah on storage.objects for insert to authenticated
    with check (
        bucket_id = 'foto-laporan'
        and (storage.foldername(name))[1] = auth.uid()::text
    );

drop policy if exists foto_laporan_ubah on storage.objects;
create policy foto_laporan_ubah on storage.objects for update to authenticated
    using (bucket_id = 'foto-laporan' and owner = auth.uid());

drop policy if exists foto_laporan_hapus on storage.objects;
create policy foto_laporan_hapus on storage.objects for delete to authenticated
    using (bucket_id = 'foto-laporan' and owner = auth.uid());

-- ── 2. Alasan penolakan klaim ──────────────────────────────────────────────
alter table public.claims add column if not exists rejection_reason text;

-- ── 3. Pengklaim membatalkan pengajuannya ──────────────────────────────────
-- Batal = hapus baris selama petugas belum memutuskan. Klaim yang sudah
-- DISETUJUI tidak boleh hilang karena sudah menyangkut barang fisik.
drop policy if exists claims_delete_own_pending on public.claims;
create policy claims_delete_own_pending on public.claims for delete to authenticated
    using (claimant_id = auth.uid() and status = 'MENUNGGU');

-- ── Verifikasi ─────────────────────────────────────────────────────────────
select 'bucket foto ada' as cek,
       exists(select 1 from storage.buckets where id = 'foto-laporan')::text as lulus
union all
select 'kolom alasan ada',
       exists(select 1 from information_schema.columns
              where table_schema = 'public' and table_name = 'claims'
                and column_name = 'rejection_reason')::text
union all
select 'kebijakan batal klaim ada',
       exists(select 1 from pg_policies
              where schemaname = 'public' and tablename = 'claims'
                and policyname = 'claims_delete_own_pending')::text
union all
select 'kebijakan unggah foto ada',
       exists(select 1 from pg_policies
              where schemaname = 'storage' and tablename = 'objects'
                and policyname = 'foto_laporan_unggah')::text;
