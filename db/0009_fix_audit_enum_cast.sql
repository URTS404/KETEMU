-- ============================================================================
--  0009 - Perbaikan cast enum pada audit otomatis
--
--  Gejala: menyetujui klaim dari aplikasi gagal dengan
--      PostgrestRestException: column "action" is of type audit_action
--      but expression is of type text   (SQLSTATE 42804)
--
--  Sebab: cabang `case when ... then 'SETUJUI_KLAIM' else 'TOLAK_KLAIM' end`
--  menghasilkan tipe `text`, dan PostgreSQL tidak otomatis men-cast hasil CASE
--  ke tipe enum saat INSERT. Hal yang sama mengancam aksi arsip/buka arsip.
--
--  Perbaikan: cast eksplisit `::public.audit_action`.
--  Aman dijalankan ulang (create or replace).
-- ============================================================================

-- ---------- Audit perubahan status & arsip laporan ----------
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
            'VERIFIKASI_LAPORAN'::public.audit_action,
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
            (case when new.is_archived then 'ARSIP_LAPORAN' else 'BUKA_ARSIP_LAPORAN' end)::public.audit_action,
            'REPORT',
            new.id,
            new.title
        );
    end if;
    return new;
end;
$$;

-- ---------- Audit keputusan klaim + kode pengambilan ----------
create or replace function public.decide_claim()
returns trigger
language plpgsql
security definer set search_path = public
as $$
begin
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
            (case when new.status = 'DISETUJUI' then 'SETUJUI_KLAIM' else 'TOLAK_KLAIM' end)::public.audit_action,
            'CLAIM',
            new.id,
            (select title from public.reports where id = new.report_id)
        );
    end if;
    return new;
end;
$$;

-- Verifikasi: kedua fungsi harus memuat cast enum
select proname,
       case when prosrc like '%::public.audit_action%' then 'OK' else 'BELUM' end as cast_enum
  from pg_proc
 where proname in ('log_report_status_change', 'decide_claim')
 order by proname;
