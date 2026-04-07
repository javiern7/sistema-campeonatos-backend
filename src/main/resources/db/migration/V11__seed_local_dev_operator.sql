INSERT INTO app_user (
    username,
    email,
    password_hash,
    first_name,
    last_name,
    status
)
SELECT
    'devoperator',
    'devoperator@local.test',
    '$2a$10$fgl9oB47y653SAPDKNNBrOXoVWSn8vDCnDT6bTKPnlDLghJNYIOjS',
    'Dev',
    'Operator',
    'ACTIVE'
WHERE NOT EXISTS (
    SELECT 1
    FROM app_user
    WHERE username = 'devoperator'
);

INSERT INTO app_user_role (user_id, role_id)
SELECT u.id, r.id
FROM app_user u
JOIN app_role r ON r.code = 'OPERATOR'
WHERE u.username = 'devoperator'
  AND NOT EXISTS (
      SELECT 1
      FROM app_user_role aur
      WHERE aur.user_id = u.id
        AND aur.role_id = r.id
  );
