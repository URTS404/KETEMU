-- ============================================================================
--  0028 - Serah terima barang (penutup alur klaim)
--
--  Tujuan: melengkapi rantai status laporan yang berhenti di SIAP_DIAMBIL.
--  Tanpa berkas ini, status SELESAI_DIKLAIM hanya ada di enum dan tidak pernah
--  ditulis oleh kode mana pun, sehingga:
--    * laporan temuan yang sudah diambil pemiliknya tetap berstatus
--      "Siap diambil" selamanya,
--    * laporan hilang milik pemilik tersebut juga tidak pernah ditutup,
--    * petugas pos tidak punya tempat mencatat "barang sudah saya serahkan".
--
--  Isi berkas:
--    1. Kolom penanda serah terima pada `claims`.
--    2. Aksi audit baru SERAH_TERIMA_BARANG.
--    3. Fungsi complete_handover(claim_id, kode) — SECURITY DEFINER.
--
--  Aturan:
--    * Hanya petugas (STAFF/ADMIN) yang boleh memanggil; dicek lewat is_staff().
--    * Klaim harus DISETUJUI dan laporan harus SIAP_DIAMBIL.
--    * Kode pengambilan opsional: bila diisi, harus sama dengan pickup_code.
--      Petugas boleh mengosongkannya ketika sudah memeriksa KTM secara manual.
--    * Semua pasangan di public.matches yang pemiliknya pengklaim ikut ditutup
--      menjadi SELESAI_DIKLAIM, jadi laporan hilang & temuan selesai bersamaan.
--    * Idempoten: memanggil ulang untuk laporan yang sudah selesai tidak gagal.
--
--  Jalankan SETELAH 0001, 0002, 0016, 0018, dan 0025. Aman dijalankan ulang.
-- ============================================================================

-- ---------- 1. Penanda serah terima ----------
alter table public.claims
    add column if not exists handed_over_at timestamptz,
    add column if not exists handed_over_by uuid
        references public.profiles (id) on delete set null;

comment on column public.claims.handed_over_at is
    'Waktu barang benar-benar diserahkan petugas pos ke pengklaim (0028).';
comment on column public.claims.handed_over_by is
    'Petugas yang menyerahkan barang (0028).';

-- ---------- 2. Aksi audit baru ----------
-- Ditulis sebagai pernyataan lepas (bukan di dalam DO/transaksi fungsi) supaya
-- nilainya sudah terpasang sebelum fungsi di bawah dipanggil pengguna.
alter type public.audit_action add value if not exists 'SERAH_TERIMA_BARANG';

-- ---------- 3. Fungsi serah terima ----------
create or replace function public.complete_handover(
    p_claim_id uuid,
    p_code     text default null
)
returns jsonb
language plpgsql
security definer
set search_path = public, auth
as $$
declare
    v_claim       public.claims;
    v_report      public.reports;
    v_expected    text;
    v_actor_name  text;
    v_closed_lost uuid[] := '{}';
begin
    if not public.is_staff() then
        raise exception 'hanya petugas pos yang boleh mencatat serah terima'
            using errcode = '42501';
    end if;

    select * into v_claim from public.claims where id = p_claim_id for update;
    if not found then
        raise exception 'klaim % tidak ditemukan', p_claim_id using errcode = 'P0002';
    end if;
    if v_claim.status <> 'DISETUJUI' then
        raise exception 'klaim belum disetujui (status %)', v_claim.status
            using errcode = '22023';
    end if;

    select * into v_report from public.reports where id = v_claim.report_id for update;
    if not found then
        raise exception 'laporan % tidak ditemukan', v_claim.report_id using errcode = 'P0002';
    end if;

    -- Sudah pernah diserahkan: kembalikan hasil yang sama tanpa mengubah apa pun.
    if v_report.status = 'SELESAI_DIKLAIM' then
        return jsonb_build_object(
            'status', 'sudah_selesai',
            'report_id', v_report.id,
            'closed_lost', to_jsonb(array[]::uuid[])
        );
    end if;
    if v_report.status <> 'SIAP_DIAMBIL' then
        raise exception 'laporan belum siap diambil (status %)', v_report.status
            using errcode = '22023';
    end if;

    v_expected := upper(btrim(coalesce(v_claim.pickup_code, '')));
    if coalesce(btrim(p_code), '') <> '' and upper(btrim(p_code)) <> v_expected then
        raise exception 'kode pengambilan tidak cocok' using errcode = '22023';
    end if;

    update public.claims
       set handed_over_at = now(),
           handed_over_by = auth.uid()
     where id = v_claim.id;

    update public.reports
       set status = 'SELESAI_DIKLAIM',
           updated_at = now()
     where id = v_report.id;

    -- Tutup laporan hilang milik pengklaim yang memang dipasangkan trigger
    -- auto-match dengan laporan temuan ini (bila ada).
    with ditutup as (
        update public.reports r
           set status = 'SELESAI_DIKLAIM',
               updated_at = now()
          from public.matches m
         where m.found_report_id = v_report.id
           and r.id = m.lost_report_id
           and r.owner_id = v_claim.claimant_id
           and r.status <> 'SELESAI_DIKLAIM'
        returning r.id
    )
    select coalesce(array_agg(id), '{}') into v_closed_lost from ditutup;

    select full_name into v_actor_name from public.profiles where id = auth.uid();

    insert into public.notifications (recipient_id, actor_id, type, title, message, report_id)
    values (
        v_claim.claimant_id,
        auth.uid(),
        'REPORT_STATUS_CHANGED',
        'Barang sudah diserahkan',
        'Barang "' || v_report.title || '" sudah kamu ambil di pos. ' ||
            'Laporan hilang dan temuan yang terkait sudah ditutup.',
        v_report.id
    );

    if v_report.owner_id is distinct from v_claim.claimant_id then
        insert into public.notifications (recipient_id, actor_id, type, title, message, report_id)
        values (
            v_report.owner_id,
            auth.uid(),
            'REPORT_STATUS_CHANGED',
            'Barang temuan sudah diambil pemiliknya',
            'Terima kasih sudah menitipkan "' || v_report.title ||
                '". Barang sudah diserahkan ke pemiliknya, jadi laporannya ditutup.',
            v_report.id
        );
    end if;

    insert into public.audit_logs (actor_id, actor_name, action, target_type, target_id, note)
    values (
        auth.uid(),
        coalesce(v_actor_name, 'Petugas pos'),
        'SERAH_TERIMA_BARANG',
        'CLAIM',
        v_claim.id,
        v_report.title
    );

    return jsonb_build_object(
        'status', 'selesai',
        'report_id', v_report.id,
        'closed_lost', to_jsonb(v_closed_lost)
    );
end;
$$;

comment on function public.complete_handover(uuid, text) is
    'Mencatat serah terima barang: temuan -> SELESAI_DIKLAIM, laporan hilang '
    'pasangannya ikut ditutup, pemilik & penemu dinotifikasi (0028).';

grant execute on function public.complete_handover(uuid, text) to authenticated;

-- ---------- 4. Pemeriksaan ----------
select 'kolom serah terima' as cek,
       count(*)::text as hasil
  from information_schema.columns
 where table_schema = 'public'
   and table_name = 'claims'
   and column_name in ('handed_over_at', 'handed_over_by')
union all
select 'fungsi complete_handover',
       count(*)::text
  from pg_proc p
  join pg_namespace n on n.oid = p.pronamespace
 where n.nspname = 'public' and p.proname = 'complete_handover'
union all
select 'aksi audit SERAH_TERIMA_BARANG',
       count(*)::text
  from pg_enum e
  join pg_type t on t.oid = e.enumtypid
 where t.typname = 'audit_action' and e.enumlabel = 'SERAH_TERIMA_BARANG';
