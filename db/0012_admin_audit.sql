-- ============================================================================
--  0012 - Audit untuk aksi ADMIN pada akun pengguna
--
--  Setiap perubahan peran, verifikasi, atau blokir akun dicatat ke
--  `audit_logs` lewat trigger, sehingga jejaknya tidak bisa dihapus aplikasi
--  (tabel audit tidak punya policy UPDATE/DELETE).
--
--  Jalankan SETELAH 0011 (enum baru sudah ditambahkan di sana).
--  Aman dijalankan ulang.
-- ============================================================================

create or replace function public.log_profile_changes()
returns trigger
language plpgsql
security definer
set search_path = public, auth
as $$
declare
    actor uuid := auth.uid();
    actor_name text;
begin
    actor_name := coalesce(
        (select full_name from public.profiles where id = actor),
        'Sistem'
    );

    if new.role is distinct from old.role then
        insert into public.audit_logs (actor_id, actor_name, action, target_type, target_id, note)
        values (actor, actor_name, 'UBAH_PERAN'::public.audit_action, 'PROFILE', new.id,
                old.role || ' → ' || new.role);
    end if;

    if new.is_verified is distinct from old.is_verified then
        insert into public.audit_logs (actor_id, actor_name, action, target_type, target_id, note)
        values (actor, actor_name, 'VERIFIKASI_AKUN'::public.audit_action, 'PROFILE', new.id,
                case when new.is_verified then 'Diverifikasi' else 'Verifikasi dicabut' end);
    end if;

    if new.is_banned is distinct from old.is_banned then
        insert into public.audit_logs (actor_id, actor_name, action, target_type, target_id, note)
        values (actor, actor_name,
                (case when new.is_banned then 'BLOKIR_AKUN' else 'BUKA_BLOKIR_AKUN' end)::public.audit_action,
                'PROFILE', new.id,
                case when new.is_banned then 'Akun diblokir' else 'Blokir dibuka' end);
    end if;

    return new;
end;
$$;

drop trigger if exists profiles_audit_changes on public.profiles;
create trigger profiles_audit_changes
after update on public.profiles
for each row execute function public.log_profile_changes();

-- Verifikasi
select 'trigger profiles_audit_changes' as cek,
       case when count(*) = 1 then 'OK' else 'BELUM' end as status
  from pg_trigger
 where tgname = 'profiles_audit_changes' and not tgisinternal
union all
select 'jenis audit manajemen akun',
       case when count(*) = 4 then 'OK' else count(*)::text end
  from pg_enum e
  join pg_type t on t.oid = e.enumtypid
 where t.typname = 'audit_action'
   and e.enumlabel in ('UBAH_PERAN','VERIFIKASI_AKUN','BLOKIR_AKUN','BUKA_BLOKIR_AKUN');
