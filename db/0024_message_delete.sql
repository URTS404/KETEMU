-- 0024_message_delete.sql — hapus pesan milik sendiri
--
-- Cara pakai: SQL Editor → New query → tempel → Run.
--
-- Aturan:
--  * Pengirim hanya boleh menghapus pesannya sendiri (sender_id = auth.uid()).
--  * Pesan yang sudah dibaca orang lain tetap boleh dihapus pengirim (kebijakan
--    aplikasi sederhana), tetapi petugas tidak boleh menghapus pesan pengguna.
--  * Pesan antrean lokal tidak menyentuh database sama sekali.

drop policy if exists messages_delete_own on public.messages;
create policy messages_delete_own on public.messages
    for delete to authenticated
    using (
        sender_id = auth.uid()
        and public.not_banned()
    );

-- ── Verifikasi ───────────────────────────────────────────────────────────────
select 'policy hapus pesan' as cek, count(*)::text as nilai
from pg_policies
where schemaname = 'public' and tablename = 'messages' and policyname = 'messages_delete_own';
