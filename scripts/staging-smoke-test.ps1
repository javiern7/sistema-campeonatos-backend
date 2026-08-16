param(
    [string]$ApiBaseUrl = $env:API_BASE_URL,
    [string]$Username = $(if ($env:SMOKE_USERNAME) { $env:SMOKE_USERNAME } else { $env:USERNAME }),
    [string]$Password = $(if ($env:SMOKE_PASSWORD) { $env:SMOKE_PASSWORD } else { $env:PASSWORD })
)

$ErrorActionPreference = "Stop"

if ([string]::IsNullOrWhiteSpace($ApiBaseUrl)) {
    throw "Define API_BASE_URL, por ejemplo: https://staging.example.com/api"
}

if ([string]::IsNullOrWhiteSpace($Username) -or [string]::IsNullOrWhiteSpace($Password)) {
    throw "Define USERNAME/PASSWORD o SMOKE_USERNAME/SMOKE_PASSWORD para el usuario staging."
}

$ApiBaseUrl = $ApiBaseUrl.TrimEnd("/")
$script:AccessToken = $null
$script:FirstTournamentId = $null
$script:FirstPublicSlug = $null

function Join-ApiPath {
    param([string]$Path)
    return "$ApiBaseUrl/$($Path.TrimStart('/'))"
}

function Invoke-SmokeStep {
    param(
        [string]$Name,
        [scriptblock]$Action
    )

    Write-Host "[RUN] $Name"
    try {
        $result = & $Action
        Write-Host "[OK]  $Name"
        return $result
    } catch {
        Write-Host "[FAIL] $Name"
        throw
    }
}

function Invoke-ApiGet {
    param(
        [string]$Path,
        [switch]$Authenticated
    )

    $headers = @{}
    if ($Authenticated) {
        $headers.Authorization = "Bearer $script:AccessToken"
    }

    return Invoke-RestMethod -Method Get -Uri (Join-ApiPath $Path) -Headers $headers
}

Invoke-SmokeStep "health" {
    Invoke-RestMethod -Method Get -Uri (Join-ApiPath "/actuator/health")
} | Out-Null

$login = Invoke-SmokeStep "login" {
    $body = @{
        username = $Username
        password = $Password
    } | ConvertTo-Json

    Invoke-RestMethod -Method Post -Uri (Join-ApiPath "/auth/login") -ContentType "application/json" -Body $body
}
$script:AccessToken = $login.data.accessToken

if ([string]::IsNullOrWhiteSpace($script:AccessToken)) {
    throw "Login no devolvio accessToken."
}

Invoke-SmokeStep "session" {
    Invoke-ApiGet "/auth/session" -Authenticated
} | Out-Null

Invoke-SmokeStep "portal publico home" {
    Invoke-ApiGet "/public/home"
} | Out-Null

$publicTournaments = Invoke-SmokeStep "portal publico listado" {
    Invoke-ApiGet "/public/tournaments?page=0&size=5"
}

if ($publicTournaments.data.content.Count -gt 0) {
    $script:FirstPublicSlug = $publicTournaments.data.content[0].slug
    Invoke-SmokeStep "portal publico detalle" {
        Invoke-ApiGet "/public/tournaments/$script:FirstPublicSlug"
    } | Out-Null
} else {
    Write-Host "[SKIP] portal publico detalle: no hay campeonatos publicos."
}

$tournaments = Invoke-SmokeStep "listar campeonatos" {
    Invoke-ApiGet "/tournaments?page=0&size=5" -Authenticated
}

if ($tournaments.data.content.Count -gt 0) {
    $script:FirstTournamentId = $tournaments.data.content[0].id
}

Invoke-SmokeStep "listar equipos" {
    Invoke-ApiGet "/teams?page=0&size=5" -Authenticated
} | Out-Null

Invoke-SmokeStep "listar jugadores" {
    Invoke-ApiGet "/players?page=0&size=5" -Authenticated
} | Out-Null

if ($script:FirstTournamentId) {
    Invoke-SmokeStep "reportes resumen" {
        Invoke-ApiGet "/tournaments/$script:FirstTournamentId/reports/summary" -Authenticated
    } | Out-Null
} else {
    Write-Host "[SKIP] reportes resumen: no hay campeonatos operativos."
}

Invoke-SmokeStep "logout" {
    Invoke-RestMethod -Method Post -Uri (Join-ApiPath "/auth/logout") -Headers @{ Authorization = "Bearer $script:AccessToken" }
} | Out-Null

Write-Host "[DONE] Smoke test staging completado."
