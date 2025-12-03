# ================================================================
# Script Avanzado para Eliminar Secretos del Historial de Git
# ================================================================
# Metodo: git filter-branch con sed (index-filter es mas rapido)
# ================================================================

Write-Host ""
Write-Host "================================================================" -ForegroundColor Cyan
Write-Host "       LIMPIEZA AVANZADA DE SECRETOS EN GIT HISTORY" -ForegroundColor Cyan
Write-Host "================================================================" -ForegroundColor Cyan
Write-Host ""

# Variables
$secretPatterns = @{
    "52866617jJ@" = "***PASSWORD_REMOVED***"
    "test_secret" = "***SECRET_REMOVED***"
    "U0hKQkNGR0hJSktMTU5PUFFSU1RVVldYWVo3ODkwQUJDREVGRw==" = "***BASE64_REMOVED***"
    "dummy_base64_value" = "***BASE64_REMOVED***"
}

Write-Host "[PASO 1] Verificando prerrequisitos..." -ForegroundColor Yellow
if (-not (Test-Path ".git")) {
    Write-Host "[ERROR] No estas en un repositorio Git" -ForegroundColor Red
    exit 1
}

Write-Host "[OK] Repositorio Git detectado" -ForegroundColor Green
Write-Host ""

Write-Host "[PASO 2] Mostrando commits que contienen secretos..." -ForegroundColor Yellow
Write-Host ""
foreach ($secret in $secretPatterns.Keys) {
    Write-Host "Buscando: $secret" -ForegroundColor Cyan
    $commits = git log --all --source -S $secret --pretty=format:"%h - %s (%an, %ar)" 2>$null | Select-Object -First 3
    if ($commits) {
        $commits | ForEach-Object { Write-Host "  $_" -ForegroundColor Gray }
    } else {
        Write-Host "  (no encontrado)" -ForegroundColor Green
    }
    Write-Host ""
}

Write-Host "================================================================" -ForegroundColor Cyan
Write-Host "[ADVERTENCIA CRITICA]" -ForegroundColor Red
Write-Host "Este proceso va a:" -ForegroundColor Yellow
Write-Host "  1. Reescribir COMPLETAMENTE el historial de Git" -ForegroundColor White
Write-Host "  2. Cambiar todos los SHA de commits" -ForegroundColor White
Write-Host "  3. Requiere force push al remoto" -ForegroundColor White
Write-Host "  4. Todo el equipo debe re-clonar o hacer reset --hard" -ForegroundColor White
Write-Host ""
Write-Host "REQUISITOS ANTES DE CONTINUAR:" -ForegroundColor Red
Write-Host "  [*] Coordinar con TODO el equipo" -ForegroundColor White
Write-Host "  [*] Asegurar que no hay trabajo sin pushear" -ForegroundColor White
Write-Host "  [*] Tener backup del repositorio" -ForegroundColor White
Write-Host "  [*] Planear rotacion de credenciales" -ForegroundColor White
Write-Host "================================================================" -ForegroundColor Cyan
Write-Host ""

$confirmation = Read-Host "Escribes 'CONFIRMO' para continuar (cualquier otra cosa cancela)"
if ($confirmation -ne "CONFIRMO") {
    Write-Host "[INFO] Operacion cancelada por seguridad" -ForegroundColor Cyan
    exit 0
}

Write-Host ""
Write-Host "[PASO 3] Creando backup de seguridad..." -ForegroundColor Yellow
$timestamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$backupPath = "..\sl-dev-backend-backup-$timestamp"
git clone . $backupPath --quiet
Write-Host "[OK] Backup en: $backupPath" -ForegroundColor Green
Write-Host ""

Write-Host "[PASO 4] Preparando filtro de limpieza..." -ForegroundColor Yellow

# Crear script de reemplazo usando SED-like con PowerShell
$replacementScript = @"
param([string]`$file)

if (Test-Path `$file) {
    `$content = [System.IO.File]::ReadAllText(`$file)
    
    # Reemplazos exactos
    `$content = `$content.Replace('52866617jJ@', '***PASSWORD_REMOVED***')
    `$content = `$content.Replace('test_secret', '***SECRET_REMOVED***')
    `$content = `$content.Replace('U0hKQkNGR0hJSktMTU5PUFFSU1RVVldYWVo3ODkwQUJDREVGRw==', '***BASE64_REMOVED***')
    `$content = `$content.Replace('dummy_base64_value', '***BASE64_REMOVED***')
    
    # Regex para claves parciales
    `$content = `$content -replace 'sk_test_[a-zA-Z0-9]+', 'sk_test_***REMOVED***'
    `$content = `$content -replace 'rk_test_[a-zA-Z0-9]+', 'rk_test_***REMOVED***'
    `$content = `$content -replace 'whsec_[a-zA-Z0-9]+', 'whsec_***REMOVED***'
    
    [System.IO.File]::WriteAllText(`$file, `$content)
}
"@

$replacementScript | Out-File "filter-replace.ps1" -Encoding UTF8
Write-Host "[OK] Script de filtro creado: filter-replace.ps1" -ForegroundColor Green
Write-Host ""

Write-Host "[PASO 5] Ejecutando git filter-branch..." -ForegroundColor Yellow
Write-Host "[INFO] Esto puede tardar 5-15 minutos dependiendo del tamano del repositorio..." -ForegroundColor Cyan
Write-Host ""

# Suprimir advertencias
$env:FILTER_BRANCH_SQUELCH_WARNING = "1"

# Comando optimizado con --tree-filter
$filterCmd = "Get-ChildItem -Path 'src/main/resources' -Filter 'application*.properties' -Recurse -ErrorAction SilentlyContinue | ForEach-Object { powershell -ExecutionPolicy Bypass -File '$PWD\filter-replace.ps1' -file `$_.FullName }"

try {
    git filter-branch -f --tree-filter $filterCmd --prune-empty --tag-name-filter cat -- --all 2>&1 | ForEach-Object {
        if ($_ -match "Rewrite") {
            Write-Host "." -NoNewline -ForegroundColor Gray
        }
    }
    Write-Host ""
    Write-Host "[OK] filter-branch completado" -ForegroundColor Green
} catch {
    Write-Host ""
    Write-Host "[WARNING] Proceso completado con algunos errores, pero continuando..." -ForegroundColor Yellow
}

Write-Host ""
Write-Host "[PASO 6] Limpiando referencias antiguas..." -ForegroundColor Yellow
Remove-Item -Path ".git/refs/original" -Recurse -Force -ErrorAction SilentlyContinue
git reflog expire --expire=now --all 2>&1 | Out-Null
Write-Host "[OK] Referencias limpiadas" -ForegroundColor Green
Write-Host ""

Write-Host "[PASO 7] Optimizando repositorio..." -ForegroundColor Yellow
git gc --prune=now --aggressive 2>&1 | Out-Null
Write-Host "[OK] Repositorio optimizado" -ForegroundColor Green
Write-Host ""

Write-Host "[PASO 8] Verificando resultados..." -ForegroundColor Yellow
Write-Host ""
$allClean = $true
foreach ($secret in $secretPatterns.Keys) {
    $found = git log --all -S $secret --pretty=format:"%h" 2>$null | Select-Object -First 1
    if ($found) {
        Write-Host "[!] '$secret' todavia aparece en commits" -ForegroundColor Red
        $allClean = $false
    } else {
        Write-Host "[OK] '$secret' eliminado correctamente" -ForegroundColor Green
    }
}
Write-Host ""

if ($allClean) {
    Write-Host "================================================================" -ForegroundColor Green
    Write-Host "           LIMPIEZA EXITOSA - TODOS LOS SECRETOS ELIMINADOS" -ForegroundColor Green
    Write-Host "================================================================" -ForegroundColor Green
} else {
    Write-Host "================================================================" -ForegroundColor Yellow
    Write-Host "           LIMPIEZA PARCIAL - REVISAR RESULTADOS" -ForegroundColor Yellow
    Write-Host "================================================================" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "PROXIMOS PASOS OBLIGATORIOS:" -ForegroundColor Cyan
Write-Host ""
Write-Host "1. ROTAR CREDENCIALES AHORA:" -ForegroundColor Red
Write-Host ""
Write-Host "   a) PostgreSQL (Heroku):" -ForegroundColor Yellow
Write-Host "      heroku pg:credentials:rotate -a screenleads-dev" -ForegroundColor White
Write-Host "      heroku pg:credentials:rotate -a screenleads-pre" -ForegroundColor White
Write-Host "      heroku pg:credentials:rotate -a screenleads-pro" -ForegroundColor White
Write-Host ""
Write-Host "   b) Stripe Keys:" -ForegroundColor Yellow
Write-Host "      https://dashboard.stripe.com/test/apikeys" -ForegroundColor White
Write-Host "      > Roll secret key, Rollable keys, Webhooks" -ForegroundColor Gray
Write-Host ""
Write-Host "   c) JWT Secret (generar nuevo 256-bit):" -ForegroundColor Yellow
Write-Host "      openssl rand -base64 32" -ForegroundColor White
Write-Host ""
Write-Host "   d) Firebase Service Account:" -ForegroundColor Yellow
Write-Host "      Firebase Console > Project Settings > Service Accounts > Generate New Private Key" -ForegroundColor White
Write-Host ""
Write-Host "2. ACTUALIZAR .env CON NUEVAS CREDENCIALES" -ForegroundColor Yellow
Write-Host ""
Write-Host "3. PUSH FORZADO (despues de coordinar con equipo):" -ForegroundColor Yellow
Write-Host "   git push origin --force --all" -ForegroundColor Cyan
Write-Host "   git push origin --force --tags" -ForegroundColor Cyan
Write-Host ""
Write-Host "4. INSTRUCCIONES PARA EL EQUIPO:" -ForegroundColor Yellow
Write-Host "   git fetch origin" -ForegroundColor Cyan
Write-Host "   git reset --hard origin/feature/v2" -ForegroundColor Cyan
Write-Host ""
Write-Host "BACKUP GUARDADO EN:" -ForegroundColor Green
Write-Host "  $backupPath" -ForegroundColor White
Write-Host ""
Write-Host "================================================================" -ForegroundColor Cyan
Write-Host ""

# Limpiar archivos temporales
Remove-Item "filter-replace.ps1" -ErrorAction SilentlyContinue
Remove-Item "replacements.txt" -ErrorAction SilentlyContinue
Remove-Item "filter-script.py" -ErrorAction SilentlyContinue
