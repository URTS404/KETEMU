-- ============================================================================
--  KETEMU v2 — 0026: perbaiki can_access_conversation (signed URL gambar chat)
--
--  Gejala yang diperbaiki:
--    Pesan gambar berhasil diunggah dan tersimpan, tetapi gelembungnya kosong
--    di aplikasi. Penyebabnya: policy baca Storage memanggil
--    public.can_access_conversation(id) dan fungsi itu berjalan dengan hak
--    pemanggil, sehingga RLS pada tabel conversations menyembunyikan baris dan
--    fungsi selalu mengembalikan false. Akibatnya createSignedUrl() gagal dan
--    AsyncImage tidak punya URL untuk dimuat.
--
--  Cara pakai: SQL Editor → New query → tempel → Run.
--  Setelah itu tutup & buka aplikasi agar signed URL dibuat ulang.
-- ============================================================================

create or replace function public.can_access_conversation(p_conversation_id uuid)
returns boolean
language sql
stable
security definer
set search_path = public
as $$
    select exists (
        select 1 from public.conversations c
         where c.id = p_conversation_id
           and (
                c.owner_id = auth.uid()
             or c.peer_id  = auth.uid()
             or public.is_staff()
           )
    );
$$;

-- ---------- Verifikasi ------------------------------------------------------
select 'can_access_conversation security definer' as cek,
       case when prosecdef then 'OK' else 'BELUM' end as nilai
  from pg_proc
 where proname = 'can_access_conversation'
 limit 1;

select 'berkas gambar tersimpan' as cek, count(*)::text as nilai
  from storage.objects
 where bucket_id = 'chat-images';
