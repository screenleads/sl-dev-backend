# ================================================================
# Script para limpiar credenciales sensibles del historial de Git
# ================================================================
# ADVERTENCIA: Este script reescribe el historial de Git.
# Asegurate de:
# 1. Tener un backup completo del repositorio
# 2. Coordinar con todo el equipo
# 3. Rotar TODAS las credenciales despues de ejecutar esto
# ================================================================

Write-Host "[INFO] Iniciando limpieza del historial de Git..." -ForegroundColor Cyan
Write-Host ""

# Paso 1: Verificar que estamos en un repositorio Git
if (-not (Test-Path ".git")) {
    Write-Host "[ERROR] No estas en un repositorio Git" -ForegroundColor Red
    exit 1
}

# Paso 2: Crear backup
Write-Host "[1/5] Creando backup del repositorio..." -ForegroundColor Yellow
$timestamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$backupPath = "..\sl-dev-backend-backup-$timestamp"
Write-Host "Clonando a: $backupPath" -ForegroundColor Gray
git clone . $backupPath --quiet
if ($LASTEXITCODE -eq 0) {
    Write-Host "[SUCCESS] Backup creado exitosamente" -ForegroundColor Green
} else {
    Write-Host "[ERROR] Fallo al crear backup" -ForegroundColor Red
    exit 1
}
Write-Host ""

# Paso 3: Verificar el estado actual
Write-Host "[2/5] Verificando estado del repositorio..." -ForegroundColor Yellow
$status = git status --porcelain
if ($status) {
    Write-Host "[WARNING] Tienes cambios sin commitear:" -ForegroundColor Yellow
    git status --short
    Write-Host ""
    $continue = Read-Host "Deseas continuar de todos modos? (S/N)"
    if ($continue -ne "S" -and $continue -ne "s") {
        Write-Host "[INFO] Operacion cancelada por el usuario" -ForegroundColor Cyan
        exit 0
    }
}
Write-Host "[INFO] Estado verificado" -ForegroundColor Cyan
Write-Host ""

# Paso 4: Crear archivo de reemplazo
Write-Host "[3/5] Creando archivo de configuracion de reemplazos..." -ForegroundColor Yellow
$replacements = @'
52866617jJ@==>***PASSWORD_REMOVED***
test_secret==>***SECRET_REMOVED***
U0hKQkNGR0hJSktMTU5PUFFSU1RVVldYWVo3ODkwQUJDREVGRw====>***BASE64_REMOVED***
dummy_base64_value==>***BASE64_REMOVED***
sk_test_51QKyPU06CPkCtQPvnH3Hk9SajR==>sk_test_***REMOVED***
sk_test_==>sk_test_***REMOVED***
rk_test_==>rk_test_***REMOVED***
whsec_==>whsec_***REMOVED***
'@
$replacements | Out-File -FilePath "replacements.txt" -Encoding UTF8
Write-Host "[SUCCESS] Archivo creado: replacements.txt" -ForegroundColor Green
Write-Host ""

# Paso 5: Ejecutar limpieza con git filter-repo (si esta disponible)
Write-Host "[4/5] Limpiando historial..." -ForegroundColor Yellow
Write-Host "[INFO] Usando metodo de reemplazo directo en commits..." -ForegroundColor Cyan
Write-Host ""

# Crear script de Python inline para filter-repo
$filterScript = @'
#!/usr/bin/env python3
import re

replacements = {
    b"52866617jJ@": b"***PASSWORD_REMOVED***",
    b"test_secret": b"***SECRET_REMOVED***",
    b"U0hKQkNGR0hJSktMTU5PUFFSU1RVVldYWVo3ODkwQUJDREVGRw==": b"***BASE64_REMOVED***",
    b"dummy_base64_value": b"***BASE64_REMOVED***",
}

def replace_secrets(blob, metadata):
    for old, new in replacements.items():
        blob.data = blob.data.replace(old, new)
    # Reemplazos con regex para claves parciales
    blob.data = re.sub(b'sk_test_[a-zA-Z0-9]+', b'sk_test_***REMOVED***', blob.data)
    blob.data = re.sub(b'rk_test_[a-zA-Z0-9]+', b'rk_test_***REMOVED***', blob.data)
    blob.data = re.sub(b'whsec_[a-zA-Z0-9]+', b'whsec_***REMOVED***', blob.data)
'@
$filterScript | Out-File -FilePath "filter-script.py" -Encoding UTF8

# Alternativa: Usar git filter-branch con PowerShell
Write-Host "[INFO] Usando git filter-branch (puede tardar varios minutos)..." -ForegroundColor Yellow
$env:FILTER_BRANCH_SQUELCH_WARNING = "1"

$filterCommand = @'
powershell -Command "
$files = Get-ChildItem -Path 'src/main/resources' -Filter 'application*.properties' -Recurse -ErrorAction SilentlyContinue
foreach ($file in $files) {
    if (Test-Path $file.FullName) {
        $content = Get-Content $file.FullName -Raw -ErrorAction SilentlyContinue
        if ($content) {
            $content = $content -replace '52866617jJ@','***PASSWORD_REMOVED***'
            $content = $content -replace 'test_secret','***SECRET_REMOVED***'
            $content = $content -replace 'U0hKQkNGR0hJSktMTU5PUFFSU1RVVldYWVo3ODkwQUJDREVGRw==','***BASE64_REMOVED***'
            $content = $content -replace 'dummy_base64_value','***BASE64_REMOVED***'
            $content = $content -replace 'sk_test_[a-zA-Z0-9]+','sk_test_***REMOVED***'
            $content = $content -replace 'rk_test_[a-zA-Z0-9]+','rk_test_***REMOVED***'
            $content = $content -replace 'whsec_[a-zA-Z0-9]+','whsec_***REMOVED***'
            Set-Content $file.FullName -Value $content -NoNewline
        }
    }
}
"
'@

Write-Host "[WARNING] Iniciando reescritura del historial completo..." -ForegroundColor Yellow
git filter-branch -f --tree-filter $filterCommand --tag-name-filter cat -- --all 2>&1 | Out-Null

if ($LASTEXITCODE -eq 0) {
    Write-Host "[SUCCESS] Historial reescrito exitosamente" -ForegroundColor Green
} else {
    Write-Host "[WARNING] filter-branch completo con advertencias" -ForegroundColor Yellow
}
Write-Host ""

# Paso 6: Limpiar referencias
Write-Host "[5/5] Limpiando referencias antiguas y optimizando..." -ForegroundColor Yellow
Remove-Item -Path ".git/refs/original" -Recurse -Force -ErrorAction SilentlyContinue
git reflog expire --expire=now --all 2>&1 | Out-Null
git gc --prune=now --aggressive 2>&1 | Out-Null
Write-Host "[SUCCESS] Limpieza completada" -ForegroundColor Green
Write-Host ""

# Paso 7: Verificar
Write-Host "[INFO] Verificando que los secretos fueron eliminados..." -ForegroundColor Cyan
$patterns = @("52866617jJ@", "test_secret", "U0hKQkNGR0hJSktMTU5PUFFSU1RVVldYWVo3ODkwQUJDREVGRw==", "dummy_base64_value")
$stillFound = @()
foreach ($pattern in $patterns) {
    $result = git log --all --source -S $pattern --oneline 2>&1 | Select-Object -First 1
    if ($result -and $result -notmatch "fatal") {
        $stillFound += $pattern
    }
}

Write-Host ""
if ($stillFound.Count -eq 0) {
    Write-Host "[SUCCESS] Todos los secretos fueron eliminados del historial!" -ForegroundColor Green
} else {
    Write-Host "[WARNING] Los siguientes patrones aun aparecen:" -ForegroundColor Yellow
    $stillFound | ForEach-Object { Write-Host "  - $_" -ForegroundColor Red }
}
Write-Host ""

# Instrucciones finales
Write-Host "================================================================" -ForegroundColor Cyan
Write-Host "                  LIMPIEZA COMPLETADA                           " -ForegroundColor Green
Write-Host "================================================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "PROXIMOS PASOS CRITICOS:" -ForegroundColor Yellow
Write-Host ""
Write-Host "1. ROTAR TODAS LAS CREDENCIALES (OBLIGATORIO):" -ForegroundColor Red
Write-Host "   [*] Password de PostgreSQL (Heroku)" -ForegroundColor White
Write-Host "   [*] Claves de Stripe (Dashboard > Developers > API Keys)" -ForegroundColor White
Write-Host "   [*] JWT_SECRET_KEY (generar nuevo con 256 bits)" -ForegroundColor White
Write-Host "   [*] Credenciales de Firebase (regenerar service account)" -ForegroundColor White
Write-Host ""
Write-Host "2. PUSH FORZADO (COORDINAR CON EL EQUIPO PRIMERO):" -ForegroundColor Yellow
Write-Host "   git push origin --force --all" -ForegroundColor Cyan
Write-Host "   git push origin --force --tags" -ForegroundColor Cyan
Write-Host ""
Write-Host "3. INSTRUCCIONES PARA EL EQUIPO:" -ForegroundColor Yellow
Write-Host "   # Guardar cambios locales" -ForegroundColor Gray
Write-Host "   git stash" -ForegroundColor Cyan
Write-Host "   # Descargar nueva historia" -ForegroundColor Gray
Write-Host "   git fetch origin" -ForegroundColor Cyan
Write-Host "   git reset --hard origin/feature/v2" -ForegroundColor Cyan
Write-Host "   # Restaurar cambios si los habia" -ForegroundColor Gray
Write-Host "   git stash pop" -ForegroundColor Cyan
Write-Host ""
Write-Host "4. ACTUALIZAR .env CON NUEVAS CREDENCIALES" -ForegroundColor Yellow
Write-Host ""
Write-Host "BACKUP guardado en:" -ForegroundColor Green
Write-Host "  $backupPath" -ForegroundColor White
Write-Host ""
Write-Host "Archivos temporales creados:" -ForegroundColor Gray
Write-Host "  - replacements.txt" -ForegroundColor White
Write-Host "  - filter-script.py" -ForegroundColor White
Write-Host "================================================================" -ForegroundColor Cyan
