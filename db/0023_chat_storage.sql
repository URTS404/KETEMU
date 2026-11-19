-- 0023_chat_storage.sql — bucket gambar chat + aturan siapa yang boleh
--
-- Cara pakai: SQL Editor → New query → tempel berkas ini → Run.
--
-- Model keamanan:
--  * Bucket **privat** (`public = false`): gambar tidak bisa dibuka tanpa izin.
--  * Path selalu `{conversation_id}/{message_id}.webp`, sehingga aturan cukup
--    memeriksa segmen pertama path = percakapan yang boleh diakses pengguna.
--  * Fungsi `public.can_access_conversation(uuid)` sudah ada sejak 0019
--    (pemilik laporan, lawan bicara, dan petugas).
--  * Unggah hanya boleh oleh peserta percakapan yang tidak diblokir.

insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values ('chat-images', 'chat-images', false, 2097152, array['image/webp', 'image/jpeg', 'image/png'])
on conflict (id) do update
    set public = false,
        file_size_limit = excluded.file_size_limit,
        allowed_mime_types = excluded.allowed_mime_types;

drop policy if exists chat_images_read on storage.objects;
create policy chat_images_read on storage.objects
    for select to authenticated
    using (
        bucket_id = 'chat-images'
        and public.can_access_conversation(((storage.foldername(name))[1])::uuid)
    );

drop policy if exists chat_images_insert on storage.objects;
create policy chat_images_insert on storage.objects
    for insert to authenticated
    with check (
        bucket_id = 'chat-images'
        and public.not_banned()
        and public.can_access_conversation(((storage.foldername(name))[1])::uuid)
    );

drop policy if exists chat_images_delete on storage.objects;
create policy chat_images_delete on storage.objects
    for delete to authenticated
    using (
        bucket_id = 'chat-images'
        and owner = auth.uid()
    );

-- Perkecualian: pemilik file boleh menghapus unggahannya sendiri (dibatasi owner),
-- tetapi tidak boleh mengubah/menimpa file orang lain — karena itu tidak ada
-- policy UPDATE sama sekali (unggah ulang memakai nama file baru).

-- ── Verifikasi ───────────────────────────────────────────────────────────────
select 'bucket chat-images' as cek, count(*)::text as nilai
from storage.buckets where id = 'chat-images'
union all
select 'policy baca gambar', count(*)::text
from pg_policies where schemaname = 'storage' and policyname = 'chat_images_read'
union all
select 'policy unggah gambar', count(*)::text
from pg_policies where schemaname = 'storage' and policyname = 'chat_images_insert'
union all
select 'policy hapus gambar', count(*)::text
from pg_policies where schemaname = 'storage' and policyname = 'chat_images_delete';
