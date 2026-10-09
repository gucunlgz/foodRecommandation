$envFile = Join-Path $PSScriptRoot '.env'
if (-not (Test-Path -LiteralPath $envFile)) {
    throw '缺少 backend/.env，请先填写 DB_URL、DB_USERNAME 和 DB_PASSWORD。'
}

foreach ($line in Get-Content -LiteralPath $envFile) {
    if ($line -match '^\s*([A-Z_]+)=(.*)$') {
        [Environment]::SetEnvironmentVariable($Matches[1], $Matches[2], 'Process')
    }
}

Push-Location $PSScriptRoot
try {
    mvn spring-boot:run '-Dspring-boot.run.profiles=mysql'
} finally {
    Pop-Location
}
