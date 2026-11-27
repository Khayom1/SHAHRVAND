-- Reproducible foundation for the private street-cleanup evidence workflow.
-- The table and bucket already exist in the connected production project.
-- IF NOT EXISTS / policy replacement keeps this migration safe for that project
-- while also allowing a clean environment to bootstrap the same feature.

create table if not exists public.shahrvand_cleanup_submissions (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  status text not null default 'pending_review'
    check (status in ('pending_review', 'approved', 'rejected', 'resubmission_required')),
  before_photo_path text not null,
  before_video_path text not null,
  after_photo_path text not null,
  after_video_path text not null,
  latitude double precision,
  longitude double precision,
  captured_at timestamptz,
  admin_telegram_id bigint,
  admin_message_id bigint,
  reviewed_at timestamptz,
  rejection_reason text,
  xp_awarded smallint not null default 0 check (xp_awarded >= 0),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint cleanup_paths_distinct check (
    before_photo_path <> before_video_path and
    before_photo_path <> after_photo_path and
    before_photo_path <> after_video_path and
    before_video_path <> after_photo_path and
    before_video_path <> after_video_path and
    after_photo_path <> after_video_path
  )
);

create index if not exists shahrvand_cleanup_submissions_user_created_idx
  on public.shahrvand_cleanup_submissions (user_id, created_at desc);

alter table public.shahrvand_cleanup_submissions enable row level security;

revoke all on public.shahrvand_cleanup_submissions from anon, public;
revoke insert, update, delete on public.shahrvand_cleanup_submissions from authenticated;
grant select on public.shahrvand_cleanup_submissions to authenticated;

drop policy if exists users_read_own_cleanup_submissions
  on public.shahrvand_cleanup_submissions;
create policy users_read_own_cleanup_submissions
  on public.shahrvand_cleanup_submissions
  for select to authenticated
  using ((select auth.uid()) = user_id);

-- Private evidence bucket. The bucket permits only the file types used by the
-- four evidence slots and caps each object at 50 MiB.
insert into storage.buckets (
  id, name, public, file_size_limit, allowed_mime_types
) values (
  'shahrvand-cleanup-evidence',
  'shahrvand-cleanup-evidence',
  false,
  52428800,
  array[
    'image/jpeg',
    'image/png',
    'image/webp',
    'video/mp4',
    'video/quicktime',
    'video/webm'
  ]::text[]
)
on conflict (id) do update set
  name = excluded.name,
  public = false,
  file_size_limit = excluded.file_size_limit,
  allowed_mime_types = excluded.allowed_mime_types;

drop policy if exists cleanup_evidence_upload_own_folder on storage.objects;
create policy cleanup_evidence_upload_own_folder
  on storage.objects
  for insert to authenticated
  with check (
    bucket_id = 'shahrvand-cleanup-evidence'
    and (storage.foldername(name))[1] = (select auth.uid())::text
  );

drop policy if exists cleanup_evidence_read_own_folder on storage.objects;
create policy cleanup_evidence_read_own_folder
  on storage.objects
  for select to authenticated
  using (
    bucket_id = 'shahrvand-cleanup-evidence'
    and (storage.foldername(name))[1] = (select auth.uid())::text
  );

drop policy if exists cleanup_evidence_delete_own_folder on storage.objects;
create policy cleanup_evidence_delete_own_folder
  on storage.objects
  for delete to authenticated
  using (
    bucket_id = 'shahrvand-cleanup-evidence'
    and (storage.foldername(name))[1] = (select auth.uid())::text
  );
