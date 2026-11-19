-- 0020_chat_view.sql — ringkasan percakapan untuk layar Pesan
--
-- Kenapa perlu view?
--  1. `profiles` punya RLS ketat: pengguna hanya boleh membaca profilnya sendiri.
--     Akibatnya join `conversations.peer_id -> profiles` mengembalikan NULL saat
--     yang membuka adalah pihak lawan, sehingga nama lawan bicara hilang.
--  2. Nama lawan bicara bergantung pada siapa pembacanya (pemilik laporan vs
--     pihak lain), dan aturan itu lebih aman ditaruh di server.
--  3. isi pesan terakhir perlu satu subquery tambahan.
--
-- View ini memakai `security_invoker = false` (berjalan sebagai pemilik view)
-- supaya bisa membaca `profiles`, tetapi barisnya SUDAH dibatasi oleh klausa
-- WHERE: hanya utas milik pengguna, atau semua utas bila ia petugas.

create or replace view public.chat_threads
with (security_invoker = false) as
select
    c.id,
    c.report_id,
    c.owner_id,
    c.peer_id,
    r.title                                as report_title,
    r.type::text                           as report_type,
    case
        when c.owner_id = auth.uid() then peer_profile.full_name
        else owner_profile.full_name
    end                                    as partner_name,
    case
        when c.owner_id = auth.uid() then peer_profile.role::text
        else owner_profile.role::text
    end                                    as partner_role,
    lm.body                                as last_message,
    c.last_message_at
from public.conversations c
join public.reports r          on r.id = c.report_id
join public.profiles owner_profile on owner_profile.id = c.owner_id
join public.profiles peer_profile  on peer_profile.id = c.peer_id
left join lateral (
    select m.body
    from public.messages m
    where m.conversation_id = c.id
    order by m.created_at desc
    limit 1
) lm on true
where c.owner_id = auth.uid()
   or c.peer_id = auth.uid()
   or public.is_staff();

grant select on public.chat_threads to authenticated;

-- ── Verifikasi ────────────────────────────────────────────────────────────────
select 'view chat_threads'  as cek, count(*)::text as nilai
from information_schema.views
where table_schema = 'public' and table_name = 'chat_threads'
union all
select 'baris chat_threads', count(*)::text from public.chat_threads
union all
select 'pesan terakhir terisi',
       coalesce(sum(case when last_message is null then 0 else 1 end), 0)::text
from public.chat_threads;
