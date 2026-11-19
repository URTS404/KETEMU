-- ============================================================================
--  KETEMU v2 — Data contoh untuk demo / penilaian
--
--  Tujuan: beranda tidak kosong saat orang lain membuka aplikasi, dan alur
--  lengkap (lapor → verifikasi → klaim → chat) bisa langsung ditunjukkan.
--
--  Cara pakai: SQL Editor → New query → tempel seluruh berkas → Run.
--  Aman dijalankan berulang: tiap baris hanya dibuat bila judulnya belum ada.
--
--  Prasyarat: 0001..0024 sudah dijalankan, dan akun
--    siti.uas@student.umn.ac.id (USER) serta rahmat@umn.ac.id (ADMIN) ada.
-- ============================================================================

-- ---------- 1. Laporan contoh (milik siti, supaya rahmat bisa memoderasi) ----
insert into public.reports (owner_id, type, title, description, category_id, last_seen_location, pos_id, status)
select p.id, v.tipe::public.report_type, v.judul, v.ket, v.kategori,
       v.lokasi, v.pos, v.status::public.report_status
  from public.profiles p
  cross join (values
    ('LOST', 'Kunci Motor Honda Beat', 'Kunci dengan gantungan boneka biru, hilang saat parkir di Gedung B.', 'LAINNYA', 'Parkiran Gedung B', null, 'DILAPORKAN'),
    ('FOUND', 'Kartu Mahasiswa atas nama Andi', 'KTM ditemukan di lift Gedung C, sudah dititipkan ke Pos Gedung C.', 'DOKUMEN', 'Lift Gedung C', 'pos-gedung-c', 'DISERAHKAN_KE_POS'),
    ('FOUND', 'Tumbler Hitam Tumbler Angsa', 'Tumbler stainless hitam, ada stiker UMN di badan botol.', 'LAINNYA', 'Ruang B-301', 'pos-gedung-b', 'SIAP_DIAMBIL'),
    ('LOST', 'Laptop Sleeve Abu-abu', 'Sleeve laptop 14 inci dengan gantungan kunci karakter di resleting.', 'ELEKTRONIK', 'Gedung A, ruang A-202', null, 'DILAPORKAN'),
    ('FOUND', 'Earphone Kabel Putih', 'Earphone kabel putih dengan mikrofon, ditemukan di meja perpustakaan.', 'ELEKTRONIK', 'Perpustakaan Lt. 3', 'pos-basement', 'SIAP_DIAMBIL'),
    ('FOUND', 'Buku Catatan Bersampul Merah', 'Buku catatan merah berisi catatan kuliah Statistika, nama pemilik tertulis di halaman pertama.', 'DOKUMEN', 'Ruang C-204', 'pos-gedung-c', 'DISERAHKAN_KE_POS'),
    ('LOST', 'Botol Minum Unicorn', 'Botol minum warna ungu bergambar unicorn, hilang setelah kelas pagi.', 'LAINNYA', 'Kantin Gedung D', null, 'DILAPORKAN'),
    ('FOUND', 'Jaket Hoodie Abu-abu', 'Hoodie abu-abu ukuran L tertinggal di kursi lobby Gedung D.', 'LAINNYA', 'Lobby Gedung D', 'pos-gedung-d', 'DISERAHKAN_KE_POS')
  ) as v(tipe, judul, ket, kategori, lokasi, pos, status)
 where p.email = 'siti.uas@student.umn.ac.id'
   and not exists (select 1 from public.reports r where r.title = v.judul);

-- ---------- 2. Dua klaim: satu menunggu, satu sudah disetujui --------------
insert into public.claims (report_id, claimant_id, answer, status)
select r.id,
       (select id from public.profiles where email = 'rahmat@umn.ac.id'),
       'Saya yang menemukan tumbler ini di ruang B-301 saat piket sore.',
       'MENUNGGU'
  from public.reports r
 where r.title = 'Tumbler Hitam Tumbler Angsa'
   and not exists (
        select 1 from public.claims c
         where c.report_id = r.id
           and c.claimant_id = (select id from public.profiles where email = 'rahmat@umn.ac.id')
   );

insert into public.claims (report_id, claimant_id, answer, status, decided_by, decided_at, pickup_code)
select r.id,
       (select id from public.profiles where email = 'rahmat@umn.ac.id'),
       'Isi tasnya: KTM, kartu ATM BCA, dan kunci kos.',
       'DISETUJUI',
       (select id from public.profiles where email = 'rahmat@umn.ac.id'),
       now(),
       'DEMO12'
  from public.reports r
 where r.title = 'Earphone Kabel Putih'
   and not exists (
        select 1 from public.claims c
         where c.report_id = r.id
   );

-- ---------- 3. Satu percakapan contoh beserta pesannya ---------------------
insert into public.conversations (report_id, owner_id, peer_id)
select r.id, r.owner_id,
       (select id from public.profiles where email = 'rahmat@umn.ac.id')
  from public.reports r
 where r.title = 'Tumbler Hitam Tumbler Angsa'
   and not exists (
        select 1 from public.conversations c
         where c.report_id = r.id
           and c.peer_id = (select id from public.profiles where email = 'rahmat@umn.ac.id')
   );

insert into public.messages (conversation_id, sender_id, body, is_template)
select c.id,
       (select id from public.profiles where email = 'rahmat@umn.ac.id'),
       'Halo, saya petugas Pos. Tumbler Anda sudah kami simpan di Pos Gedung B.',
       true
  from public.conversations c
  join public.reports r on r.id = c.report_id
 where r.title = 'Tumbler Hitam Tumbler Angsa'
   and not exists (select 1 from public.messages m where m.conversation_id = c.id);

insert into public.messages (conversation_id, sender_id, body, is_template)
select c.id, c.owner_id,
       'Baik pak, terima kasih. Nanti sore saya ambil ya.',
       false
  from public.conversations c
  join public.reports r on r.id = c.report_id
 where r.title = 'Tumbler Hitam Tumbler Angsa'
   and (select count(*) from public.messages m where m.conversation_id = c.id) = 1;

-- ---------- 4. Ringkasan hasil ---------------------------------------------
select 'laporan contoh' as cek, count(*)::text as nilai
  from public.reports
 where title in ('Kunci Motor Honda Beat','Kartu Mahasiswa atas nama Andi',
                 'Tumbler Hitam Tumbler Angsa','Laptop Sleeve Abu-abu',
                 'Earphone Kabel Putih','Buku Catatan Bersampul Merah',
                 'Botol Minum Unicorn','Jaket Hoodie Abu-abu')
union all
select 'klaim contoh', count(*)::text
  from public.claims
union all
select 'percakapan contoh', count(*)::text
  from public.conversations;
