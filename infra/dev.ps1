$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$envPath = Join-Path $projectRoot '.env'
if (-not (Test-Path -LiteralPath $envPath)) {
    throw 'Arquivo .env ausente. Copie .env.example para .env e substitua os placeholders por valores locais aleatórios.'
}

$allowedNames = @('POSTGRES_PASSWORD', 'POSTGRES_APP_PASSWORD', 'POSTGRES_MIGRATION_PASSWORD', 'DB_URL', 'DB_USER', 'DB_PASSWORD', 'DB_MIGRATION_USER', 'DB_MIGRATION_PASSWORD', 'RATE_LIMIT_HMAC_KEY', 'OTP_HMAC_KEY', 'OUTBOX_ENCRYPTION_KEY_BASE64', 'OUTBOX_ENCRYPTION_KEY_VERSION')
$values = @{}
foreach ($line in Get-Content -LiteralPath $envPath) {
    $trimmed = $line.Trim()
    if ([string]::IsNullOrWhiteSpace($trimmed) -or $trimmed.StartsWith('#')) { continue }
    $separator = $trimmed.IndexOf('=')
    if ($separator -lt 1) { throw 'Linha inválida no .env; use apenas NAME=value.' }
    $name = $trimmed.Substring(0, $separator).Trim()
    $value = $trimmed.Substring($separator + 1)
    if ($name -notin $allowedNames) { throw "Variável não permitida no .env: $name" }
    if ([string]::IsNullOrWhiteSpace($value)) { throw "Variável vazia no .env: $name" }
    $values[$name] = $value
}
foreach ($name in $allowedNames) {
    if (-not $values.ContainsKey($name)) { throw "Variável obrigatória ausente no .env: $name" }
    [Environment]::SetEnvironmentVariable($name, $values[$name], 'Process')
}
if ($values['DB_USER'] -cne 'peditto_app' -or $values['DB_MIGRATION_USER'] -cne 'peditto_migrator') {
    throw 'DB_USER/DB_MIGRATION_USER devem usar os papéis locais peditto_app e peditto_migrator.'
}
if ($values['POSTGRES_APP_PASSWORD'] -cne $values['DB_PASSWORD'] -or $values['POSTGRES_MIGRATION_PASSWORD'] -cne $values['DB_MIGRATION_PASSWORD']) {
    throw 'As credenciais do compose devem corresponder às credenciais configuradas para aplicação e migrations.'
}
if ($values['RATE_LIMIT_HMAC_KEY'].Length -lt 32) { throw 'RATE_LIMIT_HMAC_KEY deve conter pelo menos 32 caracteres.' }
if ($values['OTP_HMAC_KEY'].Length -lt 32) { throw 'OTP_HMAC_KEY deve conter pelo menos 32 caracteres.' }
try { $outboxKey = [Convert]::FromBase64String($values['OUTBOX_ENCRYPTION_KEY_BASE64']) } catch { throw 'OUTBOX_ENCRYPTION_KEY_BASE64 deve ser Base64 válido.' }
if ($outboxKey.Length -ne 32) { throw 'OUTBOX_ENCRYPTION_KEY_BASE64 deve representar exatamente 32 bytes.' }
if ($values['OUTBOX_ENCRYPTION_KEY_VERSION'] -notmatch '^[A-Za-z0-9._-]{1,80}$') { throw 'OUTBOX_ENCRYPTION_KEY_VERSION inválida.' }

$composeFile = Join-Path $PSScriptRoot 'compose.yaml'
docker compose -f $composeFile up -d --wait
if ($LASTEXITCODE -ne 0) { throw 'PostgreSQL local não iniciou.' }
$rolesSql = Join-Path $PSScriptRoot 'provision-local-db-roles.sql'
Get-Content -LiteralPath $rolesSql | docker compose -f $composeFile exec -T postgres psql -v ON_ERROR_STOP=1 -U peditto -d peditto
if ($LASTEXITCODE -ne 0) { throw 'Não foi possível provisionar os papéis locais do PostgreSQL.' }
Push-Location (Join-Path $projectRoot 'backend')
try {
    mvn spring-boot:run
    if ($LASTEXITCODE -ne 0) { throw 'A aplicação Peditto encerrou com erro.' }
}
finally {
    Pop-Location
}

