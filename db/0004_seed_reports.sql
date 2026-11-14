-- ============================================================================
--  Data contoh laporan untuk uji integrasi PostgREST (jalankan di SQL Editor)
--  Aman dijalankan ulang: tiap baris hanya dibuat bila judulnya belum ada.
-- ============================================================================

insert into public.reports (owner_id, type, title, description, category_id, last_seen_location, pos_id, status)
select p.id, 'LOST', 'Dompet Kulit Hitam',
       'Dompet berisi KTM dan kartu ATM. Terakhir dipakai di kantin Gedung C sebelum salat Jumat.',
       'AKSESORIS', 'Kantin Gedung C', null, 'DILAPORKAN'
  from public.profiles p
 where p.email = 'siti.uas@student.umn.ac.id'
   and not exists (select 1 from public.reports r where r.title = 'Dompet Kulit Hitam');

insert into public.reports (owner_id, type, title, description, category_id, last_seen_location, pos_id, status)
select p.id, 'FOUND', 'Payung Lipat Biru',
       'Payung lipat biru ditemukan di depan Perpustakaan Lantai 2, sudah dititipkan ke Pos Basement.',
       'LAINNYA', 'Perpustakaan Lt. 2', 'pos-basement', 'DISERAHKAN_KE_POS'
  from public.profiles p
 where p.email = 'siti.uas@student.umn.ac.id'
   and not exists (select 1 from public.reports r where r.title = 'Payung Lipat Biru');

-- Cek hasilnya (harus 2 baris)
select r.title, r.type, r.status, r.category_id, r.is_archived, p.full_name as pelapor
  from public.reports r
  join public.profiles p on p.id = r.owner_id
 order by r.created_at desc;
