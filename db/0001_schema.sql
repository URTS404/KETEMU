-- ============================================================================
--  KETEMU v2 — Skema dasar (jalankan PERTAMA di Supabase SQL Editor)
--  Tabel mengikuti model domain aplikasi:
--    Report  -> public.reports
--    Claim   -> public.claims
--    AuditLogEntry -> public.audit_logs
--    UserProfile -> public.profiles
-- ============================================================================

-- pgcrypto tidak diperlukan: kode pengambilan memakai md5() bawaan PostgreSQL,
-- sehingga tidak bergantung pada schema `extensions` saat trigger berjalan.

-- ---------- Tipe enum (aman dijalankan ulang) ----------
do $$
begin
    create type public.user_role as enum ('USER', 'STAFF', 'ADMIN');
exception when duplicate_object then null;
end $$;

do $$
begin
    create type public.report_type as enum ('LOST', 'FOUND');
exception when duplicate_object then null;
end $$;

do $$
begin
    create type public.report_status as enum (
        'DILAPORKAN', 'DISERAHKAN_KE_POS', 'SIAP_DIAMBIL', 'SELESAI_DIKLAIM'
    );
exception when duplicate_object then null;
end $$;

do $$
begin
    create type public.claim_status as enum ('MENUNGGU', 'DISETUJUI', 'DITOLAK');
exception when duplicate_object then null;
end $$;

do $$
begin
    create type public.audit_action as enum (
        'VERIFIKASI_LAPORAN', 'ARSIP_LAPORAN', 'BUKA_ARSIP_LAPORAN',
        'SETUJUI_KLAIM', 'TOLAK_KLAIM'
    );
exception when duplicate_object then null;
end $$;

-- ---------- Profil pengguna (1:1 dengan auth.users) ----------
create table if not exists public.profiles (
    id           uuid primary key references auth.users (id) on delete cascade,
    full_name    text        not null,
    email        text        not null,
    nim_nidn     text,
    phone        text,
    role         public.user_role not null default 'USER',
    is_verified  boolean     not null default false,
    reputation   integer     not null default 0,
    created_at   timestamptz not null default now(),
    updated_at   timestamptz not null default now()
);

-- ---------- Master data ----------
create table if not exists public.categories (
    id    text primary key,
    name  text not null
);

create table if not exists public.pos_locations (
    id        text primary key,
    name      text not null,
    building  text not null
);

-- ---------- Laporan barang ----------
create table if not exists public.reports (
    id                    uuid primary key default gen_random_uuid(),
    owner_id              uuid not null references public.profiles (id) on delete cascade,
    type                  public.report_type   not null,
    title                 text not null,
    description           text not null default '',
    category_id           text references public.categories (id),
    photo_url             text,
    last_seen_location    text not null default '',
    pos_id                text references public.pos_locations (id),
    status                public.report_status not null default 'DILAPORKAN',
    is_archived           boolean not null default false,
    verification_question text,
    created_at            timestamptz not null default now(),
    updated_at            timestamptz not null default now(),
    deleted_at            timestamptz
);

create index if not exists reports_owner_idx  on public.reports (owner_id);
create index if not exists reports_status_idx on public.reports (status) where deleted_at is null;

-- ---------- Klaim barang temuan ----------
create table if not exists public.claims (
    id             uuid primary key default gen_random_uuid(),
    report_id      uuid not null references public.reports (id) on delete cascade,
    claimant_id    uuid not null references public.profiles (id) on delete cascade,
    answer         text not null,
    status         public.claim_status not null default 'MENUNGGU',
    decided_by     uuid references public.profiles (id),
    decided_at     timestamptz,
    decision_note  text,
    pickup_code    text,
    created_at     timestamptz not null default now(),
    -- satu klaim aktif per pengguna per laporan
    constraint claims_unique_active unique (report_id, claimant_id)
);

create index if not exists claims_report_idx on public.claims (report_id);

-- ---------- Log aktivitas (append-only) ----------
create table if not exists public.audit_logs (
    id           uuid primary key default gen_random_uuid(),
    actor_id     uuid references public.profiles (id) on delete set null,
    actor_name   text not null default '',
    action       public.audit_action not null,
    target_type  text not null,
    target_id    uuid,
    note         text,
    created_at   timestamptz not null default now()
);

create index if not exists audit_logs_created_idx on public.audit_logs (created_at desc);

-- ============================================================================
--  Fungsi bantu & trigger
-- ============================================================================

-- updated_at otomatis
create or replace function public.touch_updated_at()
returns trigger
language plpgsql
as $$
begin
    new.updated_at = now();
    return new;
end;
$$;

drop trigger if exists reports_touch_updated_at on public.reports;
create trigger reports_touch_updated_at
before update on public.reports
for each row execute function public.touch_updated_at();

drop trigger if exists profiles_touch_updated_at on public.profiles;
create trigger profiles_touch_updated_at
before update on public.profiles
for each row execute function public.touch_updated_at();

-- Profil otomatis dibuat saat pengguna mendaftar lewat Supabase Auth
create or replace function public.handle_new_user()
returns trigger
language plpgsql
security definer set search_path = public
as $$
begin
    insert into public.profiles (id, full_name, email, nim_nidn, role)
    values (
        new.id,
        coalesce(new.raw_user_meta_data ->> 'full_name', split_part(new.email, '@', 1)),
        new.email,
        new.raw_user_meta_data ->> 'nim_nidn',
        'USER'
    )
    on conflict (id) do nothing;
    return new;
end;
$$;

drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created
after insert on auth.users
for each row execute function public.handle_new_user();

-- Audit otomatis: perubahan status laporan oleh petugas
create or replace function public.log_report_status_change()
returns trigger
language plpgsql
security definer set search_path = public
as $$
begin
    if new.status is distinct from old.status then
        insert into public.audit_logs (actor_id, actor_name, action, target_type, target_id, note)
        values (
            auth.uid(),
            coalesce((select full_name from public.profiles where id = auth.uid()), 'Sistem'),
            'VERIFIKASI_LAPORAN',
            'REPORT',
            new.id,
            new.title
        );
    end if;

    if new.is_archived is distinct from old.is_archived then
        insert into public.audit_logs (actor_id, actor_name, action, target_type, target_id, note)
        values (
            auth.uid(),
            coalesce((select full_name from public.profiles where id = auth.uid()), 'Sistem'),
            case when new.is_archived then 'ARSIP_LAPORAN' else 'BUKA_ARSIP_LAPORAN' end,
            'REPORT',
            new.id,
            new.title
        );
    end if;
    return new;
end;
$$;

drop trigger if exists reports_audit on public.reports;
create trigger reports_audit
after update on public.reports
for each row execute function public.log_report_status_change();

-- Audit otomatis: keputusan klaim + kode pengambilan
create or replace function public.decide_claim()
returns trigger
language plpgsql
security definer set search_path = public
as $$
begin
    -- kode pengambilan hanya untuk klaim yang disetujui
    if new.status = 'DISETUJUI' and new.pickup_code is null then
        new.pickup_code := upper(substr(md5(new.id::text), 1, 6));
    end if;

    if new.status is distinct from old.status and new.status <> 'MENUNGGU' then
        new.decided_at := coalesce(new.decided_at, now());
        new.decided_by := coalesce(new.decided_by, auth.uid());

        insert into public.audit_logs (actor_id, actor_name, action, target_type, target_id, note)
        values (
            auth.uid(),
            coalesce((select full_name from public.profiles where id = auth.uid()), 'Sistem'),
            case when new.status = 'DISETUJUI' then 'SETUJUI_KLAIM' else 'TOLAK_KLAIM' end,
            'CLAIM',
            new.id,
            (select title from public.reports where id = new.report_id)
        );
    end if;
    return new;
end;
$$;

drop trigger if exists claims_decide on public.claims;
create trigger claims_decide
before update on public.claims
for each row execute function public.decide_claim();

-- Sinkronkan status laporan saat klaim disetujui
create or replace function public.apply_claim_to_report()
returns trigger
language plpgsql
security definer set search_path = public
as $$
begin
    if new.status = 'DISETUJUI' and old.status is distinct from 'DISETUJUI' then
        update public.reports
           set status = 'SIAP_DIAMBIL'
         where id = new.report_id
           and status in ('DILAPORKAN', 'DISERAHKAN_KE_POS');
    end if;
    return new;
end;
$$;

drop trigger if exists claims_apply_to_report on public.claims;
create trigger claims_apply_to_report
after update on public.claims
for each row execute function public.apply_claim_to_report();
