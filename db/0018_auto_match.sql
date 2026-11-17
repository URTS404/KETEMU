-- ============================================================================
--  0018 - Pencocokan otomatis laporan hilang <-> laporan temuan
--
--  Tujuan (Tahap 2 roadmap):
--    * Saat laporan baru masuk, server membandingkannya dengan laporan aktif
--      berjenis SEBALIKNYA pada kategori yang sama. Bila judul berbagi kata
--      kunci ATAU titik GPS berjarak <= 250 m, pasangan dicatat di
--      public.matches.
--    * Pemilik laporan lama menerima notifikasi POSSIBLE_MATCH ("Mungkin
--      barangmu"). Notifikasi ditulis trigger SECURITY DEFINER, sehingga ikut
--      terkirim lewat Realtime (0017) tanpa perubahan di sisi aplikasi.
--    * Halaman detail membaca public.matches untuk menampilkan daftar kandidat.
--      Tombol klaim belum ada pada tahap ini (menyusul Tahap 4).
--
--  Aturan v1 sengaja konservatif:
--    * hanya laporan aktif (deleted_at is null, is_archived = false),
--    * hanya pasangan beda jenis dan beda pemilik,
--    * satu pasangan hanya dicatat sekali (unique), sehingga trigger tidak
--      menumpuk baris/notifikasi yang sama,
--    * klien tidak boleh menulis matches: hanya ada policy SELECT.
--
--  Jalankan SETELAH 0001 (reports), 0002 (is_staff), dan 0016 (notifications).
--  Aman dijalankan ulang.
-- ============================================================================

-- ---------- 1. Tabel pasangan ----------
create table if not exists public.matches (
    id              uuid primary key default gen_random_uuid(),
    lost_report_id  uuid not null references public.reports (id) on delete cascade,
    found_report_id uuid not null references public.reports (id) on delete cascade,
    created_at      timestamptz not null default now(),
    constraint matches_unique_pair unique (lost_report_id, found_report_id),
    constraint matches_different_reports check (lost_report_id <> found_report_id)
);

comment on table public.matches is
    'Pasangan laporan hilang & temuan yang dicocokkan trigger reports_auto_match (0018).';

-- Sisi "laporan saya" selalu ditanyakan lewat salah satu kolom ini.
create index if not exists matches_lost_idx  on public.matches (lost_report_id);
create index if not exists matches_found_idx on public.matches (found_report_id);

alter table public.matches enable row level security;

-- ---------- 2. Kebijakan akses ----------
-- Hanya pemilik salah satu laporan (atau petugas) yang boleh melihat
-- pasangannya. Aturan ini menjaga agar daftar kandidat di halaman detail
-- tidak membocorkan laporan pengguna lain kepada sembarang orang.
drop policy if exists matches_read on public.matches;
create policy matches_read on public.matches
for select to authenticated
using (
    public.is_staff()
    or exists (
        select 1 from public.reports r
         where r.id = lost_report_id and r.owner_id = auth.uid()
    )
    or exists (
        select 1 from public.reports r
         where r.id = found_report_id and r.owner_id = auth.uid()
    )
);

-- Sengaja TIDAK ada policy INSERT/UPDATE/DELETE: pasangan hanya dibangkitkan
-- trigger SECURITY DEFINER di bawah, sehingga klien tidak bisa memalsukan
-- pencocokan.

-- ---------- 3. Pembantu pencocokan ----------
-- Kata kunci judul: pecah pada karakter non-alfanumerik, abaikan kata pendek
-- (<= 3 huruf) yang terlalu umum ("tas", "hp", "di", "yang") agar tidak
-- menghasilkan pencocokan palsu.
create or replace function public.report_title_tokens(p_title text)
returns text[]
language sql
immutable
as $$
    select coalesce(array_agg(distinct token), '{}')
      from regexp_split_to_table(lower(coalesce(p_title, '')), '[^a-z0-9]+') as token
     where length(token) >= 4;
$$;

-- Jarak dua titik GPS dalam meter (haversine). Tanpa PostGIS supaya skema
-- tetap bisa dijalankan di project Supabase gratisan.
create or replace function public.report_distance_meters(
    p_lat1 double precision,
    p_lng1 double precision,
    p_lat2 double precision,
    p_lng2 double precision
)
returns double precision
language sql
immutable
as $$
    select 2 * 6371000 * asin(
        sqrt(least(1, greatest(0,
            power(sin(radians(p_lat2 - p_lat1) / 2), 2)
            + cos(radians(p_lat1)) * cos(radians(p_lat2))
              * power(sin(radians(p_lng2 - p_lng1) / 2), 2)
        )))
    );
$$;

-- ---------- 4. Trigger pencocokan ----------
-- SECURITY DEFINER: menulis public.matches (tanpa policy tulis) dan
-- public.notifications untuk pengguna lain.
create or replace function public.match_opposite_reports()
returns trigger
language plpgsql
security definer
set search_path = public, auth
as $$
declare
    candidate     record;
    notif_title   text;
    notif_message text;
begin
    -- Laporan yang diarsipkan/dihapus tidak layak dicocokkan.
    if new.deleted_at is not null or new.is_archived or new.category_id is null then
        return new;
    end if;

    for candidate in
        select o.id,
               o.owner_id,
               o.title
          from public.reports o
         where o.deleted_at is null
           and o.is_archived = false
           and o.id <> new.id
           and o.owner_id <> new.owner_id
           and o.type <> new.type
           and o.category_id = new.category_id
           and (
                public.report_title_tokens(o.title) && public.report_title_tokens(new.title)
                or (
                    new.latitude is not null
                    and o.latitude is not null
                    and public.report_distance_meters(
                            new.latitude, new.longitude, o.latitude, o.longitude
                        ) <= 250
                )
           )
    loop
        insert into public.matches (lost_report_id, found_report_id)
        values (
            case when new.type = 'LOST' then new.id else candidate.id end,
            case when new.type = 'FOUND' then new.id else candidate.id end
        )
        on conflict (lost_report_id, found_report_id) do nothing;

        -- FOUND hanya true bila baris benar-benar baru, sehingga notifikasi
        -- tidak terkirim ulang untuk pasangan yang sudah tercatat.
        if not found then
            continue;
        end if;

        if new.type = 'FOUND' then
            -- Pemilik laporan hilang diberi tahu ada temuan yang mirip.
            notif_title := 'Mungkin barangmu sudah ditemukan';
            notif_message := 'Ada laporan temuan "' || new.title ||
                '" yang cocok dengan laporan kehilangan Anda. Buka detail untuk memeriksa.';
        else
            -- Penemu diberi tahu ada pemilik yang melaporkan barang serupa.
            notif_title := 'Laporan kehilangan cocok dengan temuanmu';
            notif_message := 'Laporan kehilangan "' || new.title ||
                '" cocok dengan barang temuan Anda. Buka detail untuk memverifikasi.';
        end if;

        insert into public.notifications (recipient_id, actor_id, type, title, message, report_id)
        values (
            candidate.owner_id,
            new.owner_id,
            'POSSIBLE_MATCH',
            notif_title,
            notif_message,
            new.id
        );
    end loop;

    return new;
end;
$$;

drop trigger if exists reports_auto_match on public.reports;
create trigger reports_auto_match
after insert on public.reports
for each row
when (new.deleted_at is null and not new.is_archived and new.category_id is not null)
execute function public.match_opposite_reports();

-- ---------- Verifikasi ----------
select 'tabel matches' as cek,
       case when count(*) = 1 then 'OK' else 'BELUM' end as status
  from information_schema.tables
 where table_schema = 'public' and table_name = 'matches'
union all
select 'trigger reports_auto_match',
       case when count(*) = 1 then 'OK' else 'BELUM' end
  from pg_trigger where tgname = 'reports_auto_match'
union all
select 'kebijakan RLS matches', count(*)::text
  from pg_policies where schemaname = 'public' and tablename = 'matches'
union all
select 'fungsi report_title_tokens',
       case when count(*) = 1 then 'OK' else 'BELUM' end
  from pg_proc where proname = 'report_title_tokens';
