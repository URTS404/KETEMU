-- 0022_chat_images.sql — pesan berupa gambar pada percakapan
--
-- Cara pakai: SQL Editor → New query → tempel berkas ini → Run.
--
-- Keputusan desain:
--  * Isi gambar TIDAK disimpan di tabel (base64 akan menggemukkan database dan
--    membuat setiap muat percakapan berat). Yang disimpan hanya *path* di
--    Storage: `chat-images/{conversation_id}/{message_id}.webp`.
--  * Satu pesan boleh berupa teks, gambar, atau keduanya — tetapi tidak boleh
--    keduanya kosong.

alter table public.messages
    add column if not exists image_path text,
    add column if not exists image_width int,
    add column if not exists image_height int;

-- Body boleh kosong bila ada gambar.
alter table public.messages
    alter column body drop not null;

-- Tetapi pesan kosong total tetap ditolak.
alter table public.messages
    drop constraint if exists messages_isi_check;

alter table public.messages
    add constraint messages_isi_check check (
        (body is not null and length(btrim(body)) between 1 and 2000)
        or (image_path is not null and length(image_path) > 0)
    );

-- Ukuran gambar dibatasi agar kuota Storage tidak habis oleh satu foto.
alter table public.messages
    drop constraint if exists messages_image_path_check;

alter table public.messages
    add constraint messages_image_path_check check (
        image_path is null
        or (image_path not like 'http%' and length(image_path) < 300)
    );

create index if not exists messages_image_idx
    on public.messages (conversation_id)
    where image_path is not null;

-- ── Verifikasi ───────────────────────────────────────────────────────────────
select 'kolom image_path' as cek, count(*)::text as nilai
from information_schema.columns
where table_schema = 'public' and table_name = 'messages'
  and column_name in ('image_path', 'image_width', 'image_height')
union all
select 'constraint messages_isi_check', count(*)::text
from pg_constraint where conname = 'messages_isi_check'
union all
select 'constraint messages_image_path_check', count(*)::text
from pg_constraint where conname = 'messages_image_path_check';
