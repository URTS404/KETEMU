-- ============================================================================
--  0010 - Titik GPS pada laporan
--
--  Menambahkan kolom latitude/longitude (nullable) ke public.reports supaya
--  laporan yang ditandai di peta bisa ditampilkan kembali pada halaman detail.
--
--  Kedua kolom sengaja nullable: laporan lama dan laporan yang dibuat tanpa
--  penanda peta tetap valid. Constraint "complete" menjaga agar GPS tidak
--  pernah tersimpan setengah (hanya lintang atau hanya bujur).
--
--  Aman dijalankan ulang (add column if not exists + duplicate_object).
-- ============================================================================

alter table public.reports
    add column if not exists latitude  double precision,
    add column if not exists longitude double precision;

do $$
begin
    alter table public.reports
        add constraint reports_latitude_range
        check (latitude is null or (latitude between -90 and 90));
exception when duplicate_object then null;
end $$;

do $$
begin
    alter table public.reports
        add constraint reports_longitude_range
        check (longitude is null or (longitude between -180 and 180));
exception when duplicate_object then null;
end $$;

do $$
begin
    alter table public.reports
        add constraint reports_coordinates_complete
        check ((latitude is null) = (longitude is null));
exception when duplicate_object then null;
end $$;

-- Dipakai bila nanti ada pencarian "barang di sekitar saya".
create index if not exists reports_coordinates_idx
    on public.reports (latitude, longitude)
 where latitude is not null;

-- Verifikasi: harus muncul dua baris (latitude, longitude), is_nullable = YES.
select column_name, data_type, is_nullable
  from information_schema.columns
 where table_schema = 'public'
   and table_name = 'reports'
   and column_name in ('latitude', 'longitude')
 order by column_name;
