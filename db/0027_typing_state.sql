-- 0027_typing_state.sql
-- Indikator "sedang mengetik" per percakapan (tanpa polling pesan).
create table if not exists public.typing_state (
    conversation_id uuid not null references public.conversations(id) on delete cascade,
    user_id         uuid not null references auth.users(id) on delete cascade,
    updated_at      timestamptz not null default now(),
    primary key (conversation_id, user_id)
);

alter table public.typing_state enable row level security;

drop policy if exists typing_select on public.typing_state;
create policy typing_select on public.typing_state
    for select to authenticated
    using (public.can_access_conversation(conversation_id));

-- Tulis hanya untuk diri sendiri, dan hanya bila boleh mengakses utas.
create or replace function public.set_typing(p_conversation_id uuid)
returns void
language plpgsql
security definer
set search_path = public
as $$
begin
    if auth.uid() is null then
        return;
    end if;
    if not public.can_access_conversation(p_conversation_id) then
        return;
    end if;
    insert into public.typing_state (conversation_id, user_id, updated_at)
    values (p_conversation_id, auth.uid(), now())
    on conflict (conversation_id, user_id)
    do update set updated_at = now();
end;
$$;

-- Apakah lawan bicara mengetik dalam 5 detik terakhir?
create or replace function public.peer_typing(p_conversation_id uuid)
returns boolean
language sql
stable
security definer
set search_path = public
as $$
    select exists (
        select 1 from public.typing_state t
        where t.conversation_id = p_conversation_id
          and t.user_id <> coalesce(auth.uid(), '00000000-0000-0000-0000-000000000000'::uuid)
          and t.updated_at > now() - interval ''5 seconds''
    );
$$;

grant execute on function public.set_typing(uuid) to authenticated;
grant execute on function public.peer_typing(uuid) to authenticated;

-- Pemeriksaan
select 'tabel typing_state' as cek, to_regclass('public.typing_state')::text as nilai;
