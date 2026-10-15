$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$envPath = Join-Path $projectRoot '.env'
if (-not (Test-Path -LiteralPath $envPath)) {
    throw 'Arquivo .env ausente. Copie .env.example para .env e substitua os placeholders por valores locais aleatórios.'
}

$requiredNames = @('POSTGRES_PASSWORD', 'POSTGRES_APP_PASSWORD', 'POSTGRES_MIGRATION_PASSWORD', 'DB_URL', 'DB_USER', 'DB_PASSWORD', 'DB_MIGRATION_USER', 'DB_MIGRATION_PASSWORD', 'RATE_LIMIT_HMAC_KEY', 'OTP_HMAC_KEY', 'OUTBOX_ENCRYPTION_KEY_BASE64', 'OUTBOX_ENCRYPTION_KEY_VERSION')
$optionalDefaults = @{
    OTP_DELIVERY_ENABLED = 'false'
    OTP_OUTBOX_POLL_MS = '1000'
    SMTP_HOST = ''
    SMTP_PORT = '587'
    SMTP_USERNAME = ''
    SMTP_PASSWORD = ''
    OTP_MAIL_FROM = ''
    TWILIO_ACCOUNT_SID = ''
    TWILIO_API_KEY_SID = ''
    TWILIO_API_KEY_SECRET = ''
    TWILIO_MESSAGING_SERVICE_SID = ''
}
$allowedNames = @($requiredNames) + @($optionalDefaults.Keys)
$values = @{}
foreach ($line in Get-Content -LiteralPath $envPath) {
    $trimmed = $line.Trim()
    if ([string]::IsNullOrWhiteSpace($trimmed) -or $trimmed.StartsWith('#')) { continue }
    $separator = $trimmed.IndexOf('=')
    if ($separator -lt 1) { throw 'Linha inválida no .env; use apenas NAME=value.' }
    $name = $trimmed.Substring(0, $separator).Trim()
    $value = $trimmed.Substring($separator + 1)
    if ($name -notin $allowedNames) { throw "Variável não permitida no .env: $name" }
    if ([string]::IsNullOrWhiteSpace($value) -and $name -in $requiredNames) { throw "Variável vazia no .env: $name" }
    $values[$name] = $value
}
foreach ($name in $requiredNames) {
    if (-not $values.ContainsKey($name)) { throw "Variável obrigatória ausente no .env: $name" }
    [Environment]::SetEnvironmentVariable($name, $values[$name], 'Process')
}
foreach ($name in $optionalDefaults.Keys) {
    $value = if ($values.ContainsKey($name)) { $values[$name] } else { $optionalDefaults[$name] }
    [Environment]::SetEnvironmentVariable($name, $value, 'Process')
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
if ($env:OTP_DELIVERY_ENABLED -notin @('true', 'false')) { throw 'OTP_DELIVERY_ENABLED deve ser true ou false.' }
if ($env:OTP_DELIVERY_ENABLED -eq 'true') {
    foreach ($name in @('SMTP_HOST', 'SMTP_USERNAME', 'SMTP_PASSWORD', 'OTP_MAIL_FROM', 'TWILIO_ACCOUNT_SID', 'TWILIO_API_KEY_SID', 'TWILIO_API_KEY_SECRET', 'TWILIO_MESSAGING_SERVICE_SID')) {
        if ([string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable($name, 'Process'))) {
            throw "Entrega OTP habilitada, mas configuração obrigatória ausente: $name"
        }
    }
    $smtpPort = 0
    if (-not [int]::TryParse($env:SMTP_PORT, [ref]$smtpPort) -or $smtpPort -lt 1 -or $smtpPort -gt 65535) {
        throw 'SMTP_PORT deve ser um número entre 1 e 65535.'
    }
    if ($env:TWILIO_ACCOUNT_SID -notmatch '^AC[0-9a-fA-F]{32}$' -or
        $env:TWILIO_API_KEY_SID -notmatch '^SK[0-9a-fA-F]{32}$' -or
        $env:TWILIO_MESSAGING_SERVICE_SID -notmatch '^MG[0-9a-fA-F]{32}$') {
        throw 'Os identificadores de conta/chave/serviço Twilio têm formato inválido.'
    }
}

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

