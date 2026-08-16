param(
    [string]$DbUrl = $env:DB_URL,
    [string]$DbUsername = $env:DB_USERNAME,
    [string]$DbPassword = $env:DB_PASSWORD,
    [string]$AdminUsername = $env:ADMIN_USERNAME,
    [string]$AdminEmail = $env:ADMIN_EMAIL,
    [string]$AdminPassword = $env:ADMIN_PASSWORD,
    [string]$AdminPasswordHash = $env:ADMIN_PASSWORD_HASH,
    [string]$AdminFirstName = $env:ADMIN_FIRST_NAME,
    [string]$AdminLastName = $env:ADMIN_LAST_NAME,
    [string]$SpringSecurityCryptoJar = $env:SPRING_SECURITY_CRYPTO_JAR
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

function Assert-Required {
    param(
        [string]$Name,
        [string]$Value
    )

    if ([string]::IsNullOrWhiteSpace($Value)) {
        throw "Missing required value: $Name. Provide it as a parameter or environment variable."
    }
}

function Convert-ToPostgresConnectionUri {
    param([string]$Url)

    if ($Url.StartsWith('jdbc:postgresql://', [StringComparison]::OrdinalIgnoreCase)) {
        return $Url.Substring(5)
    }

    if ($Url.StartsWith('postgresql://', [StringComparison]::OrdinalIgnoreCase)) {
        return $Url
    }

    throw 'DB_URL must use jdbc:postgresql://... or postgresql://...'
}

function Find-MavenJar {
    param(
        [string]$RepositoryPath,
        [string]$Filter,
        [string]$DisplayName
    )

    if (-not (Test-Path -LiteralPath $RepositoryPath -PathType Container)) {
        throw "$DisplayName was not found in the local Maven repository. Run mvn test once or provide the explicit jar path."
    }

    $jar = Get-ChildItem -LiteralPath $RepositoryPath -Recurse -Filter $Filter |
        Where-Object { $_.Name -notmatch '-(sources|javadoc)\.jar$' } |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1

    if ($null -eq $jar) {
        throw "$DisplayName jar was not found. Run mvn test once or provide the explicit jar path."
    }

    return $jar.FullName
}

function Resolve-ExplicitOrMavenJar {
    param(
        [string]$ExplicitPath,
        [string]$RepositoryPath,
        [string]$Filter,
        [string]$DisplayName
    )

    if (-not [string]::IsNullOrWhiteSpace($ExplicitPath)) {
        if (-not (Test-Path -LiteralPath $ExplicitPath -PathType Leaf)) {
            throw "$DisplayName explicit jar does not exist: $ExplicitPath"
        }

        return (Resolve-Path -LiteralPath $ExplicitPath).Path
    }

    return Find-MavenJar -RepositoryPath $RepositoryPath -Filter $Filter -DisplayName $DisplayName
}

function Resolve-Psql {
    $command = Get-Command psql -ErrorAction SilentlyContinue
    if ($null -ne $command) {
        return $command.Source
    }

    $candidateRoots = @(
        'C:\Program Files\PostgreSQL',
        'C:\Program Files (x86)\PostgreSQL'
    )

    foreach ($candidateRoot in $candidateRoots) {
        if (-not (Test-Path -LiteralPath $candidateRoot -PathType Container)) {
            continue
        }

        $candidate = Get-ChildItem -LiteralPath $candidateRoot -Recurse -Filter 'psql.exe' -ErrorAction SilentlyContinue |
            Where-Object { $_.FullName -match '\\bin\\psql\.exe$' } |
            Sort-Object FullName -Descending |
            Select-Object -First 1

        if ($null -ne $candidate) {
            return $candidate.FullName
        }
    }

    throw 'psql was not found in PATH or under C:\Program Files\PostgreSQL. Install PostgreSQL client tools or add psql.exe to PATH.'
}

function New-BCryptHash {
    param(
        [string]$Password,
        [string]$Classpath
    )

    Assert-Required -Name 'ADMIN_PASSWORD' -Value $Password
    $javac = Get-Command javac -ErrorAction Stop
    $java = Get-Command java -ErrorAction Stop
    $tempDir = Join-Path ([System.IO.Path]::GetTempPath()) ("bootstrap-admin-bcrypt-" + [System.Guid]::NewGuid().ToString('N'))

    New-Item -ItemType Directory -Path $tempDir | Out-Null
    try {
        $sourcePath = Join-Path $tempDir 'BCryptHash.java'
        $javaSource = @'
import org.springframework.security.crypto.bcrypt.BCrypt;

public class BCryptHash {
    public static void main(String[] args) {
        String password = System.getenv("ADMIN_PASSWORD");
        if (password == null || password.isBlank()) {
            System.err.println("ADMIN_PASSWORD is required.");
            System.exit(2);
        }

        System.out.println(BCrypt.hashpw(password, BCrypt.gensalt(12)));
    }
}
'@
        [System.IO.File]::WriteAllText($sourcePath, $javaSource, [System.Text.UTF8Encoding]::new($false))

        & $javac.Source -cp $Classpath -d $tempDir $sourcePath | Out-Null
        if ($LASTEXITCODE -ne 0) {
            throw 'BCrypt helper compilation failed.'
        }

        $previousAdminPassword = $env:ADMIN_PASSWORD
        try {
            $env:ADMIN_PASSWORD = $Password
            $javaOutput = & $java.Source -cp "$tempDir;$Classpath" BCryptHash
        }
        finally {
            $env:ADMIN_PASSWORD = $previousAdminPassword
        }

        if ($LASTEXITCODE -ne 0 -or $null -eq $javaOutput) {
            throw 'BCrypt hash generation failed.'
        }

        $hash = ($javaOutput | Select-Object -First 1).Trim()
        if ([string]::IsNullOrWhiteSpace($hash)) {
            throw 'BCrypt hash generation returned an empty value.'
        }

        return $hash
    }
    finally {
        if (Test-Path -LiteralPath $tempDir) {
            Remove-Item -LiteralPath $tempDir -Recurse -Force
        }
    }
}

Assert-Required -Name 'DB_URL' -Value $DbUrl
Assert-Required -Name 'DB_USERNAME' -Value $DbUsername
Assert-Required -Name 'DB_PASSWORD' -Value $DbPassword
Assert-Required -Name 'ADMIN_USERNAME' -Value $AdminUsername
Assert-Required -Name 'ADMIN_EMAIL' -Value $AdminEmail
Assert-Required -Name 'ADMIN_FIRST_NAME' -Value $AdminFirstName
Assert-Required -Name 'ADMIN_LAST_NAME' -Value $AdminLastName

$psql = Resolve-Psql
$connectionUri = Convert-ToPostgresConnectionUri -Url $DbUrl

if ([string]::IsNullOrWhiteSpace($AdminPasswordHash)) {
    Assert-Required -Name 'ADMIN_PASSWORD' -Value $AdminPassword
    $cryptoRepositoryPath = Join-Path $HOME '.m2\repository\org\springframework\security\spring-security-crypto'
    $cryptoJar = Resolve-ExplicitOrMavenJar -ExplicitPath $SpringSecurityCryptoJar -RepositoryPath $cryptoRepositoryPath -Filter 'spring-security-crypto-*.jar' -DisplayName 'spring-security-crypto'
    $AdminPasswordHash = New-BCryptHash -Password $AdminPassword -Classpath $cryptoJar
}

Assert-Required -Name 'ADMIN_PASSWORD_HASH' -Value $AdminPasswordHash

$previousPgPassword = $env:PGPASSWORD

$sql = @'
\set ON_ERROR_STOP on

BEGIN;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM app_role WHERE code = 'SUPER_ADMIN') THEN
        RAISE EXCEPTION 'Role SUPER_ADMIN does not exist. Run Flyway migrations before bootstrap.';
    END IF;
END $$;

INSERT INTO app_user (
    username,
    email,
    password_hash,
    first_name,
    last_name,
    status
)
VALUES (
    :'admin_username',
    :'admin_email',
    :'admin_password_hash',
    :'admin_first_name',
    :'admin_last_name',
    'ACTIVE'
)
ON CONFLICT (username) DO UPDATE
SET email = EXCLUDED.email,
    password_hash = EXCLUDED.password_hash,
    first_name = EXCLUDED.first_name,
    last_name = EXCLUDED.last_name,
    status = 'ACTIVE',
    updated_at = now();

INSERT INTO app_user_role (user_id, role_id)
SELECT u.id, r.id
FROM app_user u
JOIN app_role r ON r.code = 'SUPER_ADMIN'
WHERE u.username = :'admin_username'
ON CONFLICT DO NOTHING;

COMMIT;
'@

try {
    $env:PGPASSWORD = $DbPassword
    $psqlArgs = @(
        '--no-password',
        "--username=$DbUsername",
        "--set=admin_username=$AdminUsername",
        "--set=admin_email=$AdminEmail",
        "--set=admin_password_hash=$AdminPasswordHash",
        "--set=admin_first_name=$AdminFirstName",
        "--set=admin_last_name=$AdminLastName",
        $connectionUri
    )

    $sql | & $psql @psqlArgs
    if ($LASTEXITCODE -ne 0) {
        throw "psql failed with exit code $LASTEXITCODE."
    }

    Write-Host "Bootstrap admin completed for username '$AdminUsername' with role SUPER_ADMIN."
}
finally {
    $env:PGPASSWORD = $previousPgPassword
    $AdminPassword = $null
    $AdminPasswordHash = $null
}
