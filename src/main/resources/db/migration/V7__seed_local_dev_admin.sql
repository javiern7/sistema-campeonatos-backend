INSERT INTO app_user (
    username,
    email,
    password_hash,
    first_name,
    last_name,
    status
)
SELECT
    'devadmin',
    'devadmin@local.test',
    '$2a$10$fgl9oB47y653SAPDKNNBrOXoVWSn8vDCnDT6bTKPnlDLghJNYIOjS',
    'Dev',
    'Admin',
    'ACTIVE'
WHERE NOT EXISTS (
    SELECT 1
    FROM app_user
    WHERE username = 'devadmin'
);

INSERT INTO app_user_role (user_id, role_id)
SELECT u.id, r.id
FROM app_user u
JOIN app_role r ON r.code = 'SUPER_ADMIN'
WHERE u.username = 'devadmin'
  AND NOT EXISTS (
      SELECT 1
      FROM app_user_role aur
      WHERE aur.user_id = u.id
        AND aur.role_id = r.id
  );
