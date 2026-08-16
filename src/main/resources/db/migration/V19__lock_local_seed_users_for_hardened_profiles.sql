UPDATE app_user
SET status = 'LOCKED',
    updated_at = now()
WHERE '${disableLocalSeedUsers}' = 'true'
  AND username IN ('admin', 'devadmin', 'devoperator')
  AND status <> 'LOCKED';
