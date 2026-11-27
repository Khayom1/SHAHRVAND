-- Securely create a street-cleanup submission from four owner-uploaded evidence files.
-- Storage bucket is private. Uploads remain under the authenticated user's own folder.
-- No public INSERT policy is added to shahrvand_cleanup_submissions.

create or replace function public.shahrvand_create_cleanup_submission(
  p_before_photo_path text,
  p_before_video_path text,
  p_after_photo_path text,
  p_after_video_path text
)
returns jsonb
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_user_id uuid := auth.uid();
  v_paths text[] := array[
    p_before_photo_path,
    p_before_video_path,
    p_after_photo_path,
    p_after_video_path
  ];
  v_path text;
  v_expected_mime text[];
  v_index integer;
  v_submission_id uuid;
  v_created_at timestamptz;
begin
  if v_user_id is null then
    raise exception 'AUTHENTICATION_REQUIRED' using errcode = '28000';
  end if;

  if exists (
    select 1
    from public.shahrvand_cleanup_submissions s
    where s.user_id = v_user_id
      and s.status = 'pending_review'
  ) then
    raise exception 'CLEANUP_SUBMISSION_ALREADY_PENDING' using errcode = '23505';
  end if;

  if array_position(v_paths, null) is not null then
    raise exception 'ALL_FOUR_EVIDENCE_FILES_REQUIRED' using errcode = '22023';
  end if;

  if cardinality(v_paths) <> 4
     or (select count(distinct value) from unnest(v_paths) as value) <> 4 then
    raise exception 'EVIDENCE_PATHS_MUST_BE_DISTINCT' using errcode = '22023';
  end if;

  v_expected_mime := array[
    'image/jpeg,image/png,image/webp',
    'video/mp4,video/quicktime,video/webm',
    'image/jpeg,image/png,image/webp',
    'video/mp4,video/quicktime,video/webm'
  ];

  for v_index in 1..4 loop
    v_path := v_paths[v_index];

    if length(v_path) > 500
       or v_path <> trim(v_path)
       or left(v_path, length(v_user_id::text) + 1) <> v_user_id::text || '/'
       or v_path like '%..%'
       or v_path like '%//%'
       or position(chr(92) in v_path) > 0
       or left(v_path, 1) = '/' then
      raise exception 'INVALID_EVIDENCE_PATH' using errcode = '22023';
    end if;

    if not exists (
      select 1
      from storage.objects o
      where o.bucket_id = 'shahrvand-cleanup-evidence'
        and o.name = v_path
        and coalesce(o.owner_id, '') = v_user_id::text
        and coalesce(o.metadata ->> 'mimetype', '') =
            any(string_to_array(v_expected_mime[v_index], ','))
        and coalesce((o.metadata ->> 'size')::bigint, 0) > 0
        and coalesce((o.metadata ->> 'size')::bigint, 0) <= 52428800
    ) then
      raise exception 'EVIDENCE_FILE_NOT_FOUND_OR_INVALID' using errcode = 'P0002';
    end if;
  end loop;

  insert into public.shahrvand_cleanup_submissions (
    user_id,
    before_photo_path,
    before_video_path,
    after_photo_path,
    after_video_path,
    status,
    xp_awarded
  ) values (
    v_user_id,
    p_before_photo_path,
    p_before_video_path,
    p_after_photo_path,
    p_after_video_path,
    'pending_review',
    0
  )
  returning id, created_at into v_submission_id, v_created_at;

  return jsonb_build_object(
    'ok', true,
    'submission_id', v_submission_id,
    'status', 'pending_review',
    'submitted_at', v_created_at,
    'xp_awarded', 0
  );
end;
$$;

revoke all on function public.shahrvand_create_cleanup_submission(text, text, text, text) from public, anon;
grant execute on function public.shahrvand_create_cleanup_submission(text, text, text, text) to authenticated;
