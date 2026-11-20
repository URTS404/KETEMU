-- ============================================================================
--  0032_lens.sql — "Lens": mencari barang temuan berdasarkan foto.
--
--  Masalah: pencocokan otomatis (db/0018) hanya memakai kata pada judul dan
--  jarak GPS. Dua laporan yang fotonya sama tetapi judulnya berbeda ("Tas" vs
--  "Ransel") tidak pernah dipertemukan.
--
--  Solusi: aplikasi mengubah foto menjadi 1024 angka (embedding MobileNetV3 di
--  dalam HP, tanpa layanan awan), lalu angka itu disimpan di kolom
--  `photo_embedding`. Pencarian foto memakai jarak kosinus pgvector, sehingga
--  yang dibandingkan adalah RUPA barang, bukan untai teks judulnya.
--
--  Catatan keamanan:
--   - Vektor hanya menerima nilai dari pemilik laporan (atau petugas), jadi
--     pengguna lain tidak bisa menimpa sidik foto milik orang lain.
--   - Foto aslinya TIDAK pernah dikirim ke mana pun oleh fitur ini; yang
--     dikirim hanyalah angka vektor hasil hitungan di perangkat.
--   - `security invoker` dipakai supaya RLS tetap berlaku seperti biasa.
--
--  Cara pakai: tempel seluruh berkas ini di SQL Editor Supabase, lalu Run.
-- ============================================================================

create extension if not exists vector;

alter table public.reports
    add column if not exists photo_embedding vector(1024);

comment on column public.reports.photo_embedding is
    'Sidik rupa foto laporan (MobileNetV3, 1024 angka, dinormalisasi L2). Diisi aplikasi lewat simpan_vektor_foto().';

-- HNSW: pencarian tetangga terdekat yang cepat walau baris laporan bertambah.
-- Operator yang dipakai `<=>` (jarak kosinus), jadi kelas ops tertentu.
create index if not exists reports_photo_embedding_idx
    on public.reports using hnsw (photo_embedding vector_cosine_ops);

-- ---------------------------------------------------------------------------
--  Menyimpan sidik foto sebuah laporan.
--  Mengembalikan true bila baris benar-benar diperbarui.
-- ---------------------------------------------------------------------------
create or replace function public.simpan_vektor_foto(
    p_report_id uuid,
    p_vektor    double precision[]
)
returns boolean
language plpgsql
security invoker
set search_path = public
as $$
declare
    jumlah int;
    teks   text;
begin
    if p_vektor is null then
        return false;
    end if;

    jumlah := coalesce(array_length(p_vektor, 1), 0);
    if jumlah <> 1024 then
        raise exception 'vektor harus berisi 1024 angka, diterima %', jumlah
            using errcode = '22023';
    end if;

    teks := '[' || array_to_string(p_vektor, ',') || ']';

    update public.reports
       set photo_embedding = teks::vector
     where id = p_report_id
       and (owner_id = auth.uid() or public.is_staff());

    return found;
end;
$$;

grant execute on function public.simpan_vektor_foto(uuid, double precision[]) to authenticated;

-- ---------------------------------------------------------------------------
--  Mencari laporan temuan yang rupanya mirip dengan vektor kiriman.
--    p_batas  : jumlah maksimum hasil (1..100)
--    p_ambang : batas bawah kemiripan 0..1 (0.25 = cukup longgar)
--  `skor` dikembalikan dalam persen agar mudah ditampilkan ("Mirip 87%").
-- ---------------------------------------------------------------------------
create or replace function public.cari_barang_mirip(
    p_vektor double precision[],
    p_batas  int default 20,
    p_ambang double precision default 0.25
)
returns table (
    id         uuid,
    title      text,
    photo_url  text,
    status     text,
    pos_name   text,
    created_at timestamptz,
    skor       int
)
language sql
stable
security invoker
set search_path = public
as $$
    with kueri as (
        select ('[' || array_to_string(p_vektor, ',') || ']')::vector as v
    )
    select r.id,
           r.title,
           r.photo_url,
           r.status::text,
           p.name,
           r.created_at,
           round((1 - (r.photo_embedding <=> k.v)) * 100)::int as skor
      from public.reports r
      cross join kueri k
      left join public.pos_locations p on p.id = r.pos_id
     where r.type = 'FOUND'
       and r.is_archived = false
       and r.photo_embedding is not null
       and (1 - (r.photo_embedding <=> k.v)) >= p_ambang
     order by r.photo_embedding <=> k.v
     limit least(greatest(coalesce(p_batas, 20), 1), 100);
$$;

grant execute on function public.cari_barang_mirip(double precision[], int, double precision) to authenticated;

-- ---------------------------------------------------------------------------
--  Verifikasi: semua baris harus `true`.
-- ---------------------------------------------------------------------------
select 'ekstensi vector terpasang' as cek,
       exists (select 1 from pg_extension where extname = 'vector') as lulus
union all
select 'kolom photo_embedding ada',
       exists (
           select 1 from information_schema.columns
            where table_schema = 'public'
              and table_name = 'reports'
              and column_name = 'photo_embedding'
       )
union all
select 'indeks vektor ada',
       exists (
           select 1 from pg_indexes
            where schemaname = 'public'
              and indexname = 'reports_photo_embedding_idx'
       )
union all
select 'fungsi simpan_vektor_foto ada',
       exists (select 1 from pg_proc where proname = 'simpan_vektor_foto')
union all
select 'fungsi cari_barang_mirip ada',
       exists (select 1 from pg_proc where proname = 'cari_barang_mirip');
