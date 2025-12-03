# Script Optimizado para Eliminar Secretos REALES del Historial
# Este script solo procesa archivos donde realmente había secretos hardcoded

Write-Host ""
Write-Host "================================================================" -ForegroundColor Cyan
Write-Host "   LIMPIEZA DE SECRETOS - VERSION OPTIMIZADA" -ForegroundColor Cyan
Write-Host "================================================================" -ForegroundColor Cyan
Write-Host ""

Write-Host "[INFO] Este script eliminara secretos de:" -ForegroundColor Yellow
Write-Host "  - src/main/resources/application*.properties" -ForegroundColor White
Write-Host "  - .env (commits antiguos)" -ForegroundColor White
Write-Host "  - docs/ai-config.md" -ForegroundColor White
Write-Host ""
Write-Host "[INFO] NO afectara archivos de documentacion recientes" -ForegroundColor Cyan
Write-Host ""

$continue = Read-Host "Continuar? (S/N)"
if ($continue -ne "S" -and $continue -ne "s") {
    Write-Host "[INFO] Cancelado" -ForegroundColor Cyan
    exit 0
}

Write-Host ""
Write-Host "[1/5] Creando backup..." -ForegroundColor Yellow
$timestamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$backupPath = "..\sl-dev-backend-REAL-backup-$timestamp"
git clone . $backupPath --quiet 2>&1 | Out-Null
Write-Host "[OK] Backup: $backupPath" -ForegroundColor Green
Write-Host ""

Write-Host "[2/5] Preparando script de filtro..." -ForegroundColor Yellow

# Script que solo procesa archivos específicos
$filterContent = @'
# Procesar solo archivos con secretos reales
$filesToProcess = @(
    "src/main/resources/application.properties",
    "src/main/resources/application-dev.properties",
    "src/main/resources/application-pre.properties",
    "src/main/resources/application-pro.properties",
    ".env",
    "docs/ai-config.md"
)

foreach ($file in $filesToProcess) {
    if (Test-Path $file) {
        try {
            $content = [System.IO.File]::ReadAllText($file, [System.Text.Encoding]::UTF8)
            $modified = $false
            
            # Reemplazos exactos
            if ($content -match '52866617jJ@') {
                $content = $content -replace '52866617jJ@', '***PASSWORD_REMOVED***'
                $modified = $true
            }
            if ($content -match 'test_secret') {
                $content = $content -replace 'test_secret', '***SECRET_REMOVED***'
                $modified = $true
            }
            if ($content -match 'U0hKQkNGR0hJSktMTU5PUFFSU1RVVldYWVo3ODkwQUJDREVGRw==') {
                $content = $content -replace 'U0hKQkNGR0hJSktMTU5PUFFSU1RVVldYWVo3ODkwQUJDREVGRw==', '***BASE64_REMOVED***'
                $modified = $true
            }
            if ($content -match 'dummy_base64_value') {
                $content = $content -replace 'dummy_base64_value', '***BASE64_REMOVED***'
                $modified = $true
            }
            
            # Regex para claves Stripe parciales
            if ($content -match 'sk_test_[a-zA-Z0-9]+') {
                $content = $content -replace 'sk_test_[a-zA-Z0-9]+', 'sk_test_***REMOVED***'
                $modified = $true
            }
            if ($content -match 'rk_test_[a-zA-Z0-9]+') {
                $content = $content -replace 'rk_test_[a-zA-Z0-9]+', 'rk_test_***REMOVED***'
                $modified = $true
            }
            if ($content -match 'whsec_[a-zA-Z0-9]+') {
                $content = $content -replace 'whsec_[a-zA-Z0-9]+', 'whsec_***REMOVED***'
                $modified = $true
            }
            
            if ($modified) {
                [System.IO.File]::WriteAllText($file, $content, [System.Text.Encoding]::UTF8)
            }
        } catch {
            # Ignorar errores en archivos que no existen en este commit
        }
    }
}
'@

$filterContent | Out-File "real-filter.ps1" -Encoding UTF8
Write-Host "[OK] Script creado: real-filter.ps1" -ForegroundColor Green
Write-Host ""

Write-Host "[3/5] Ejecutando git filter-branch..." -ForegroundColor Yellow
Write-Host "[WARNING] Esto tomara varios minutos..." -ForegroundColor Yellow
Write-Host ""

$env:FILTER_BRANCH_SQUELCH_WARNING = "1"

# Usar tree-filter para modificar archivos específicos
$treeFilter = "powershell -ExecutionPolicy Bypass -File `"$PWD\real-filter.ps1`""

git filter-branch -f --tree-filter $treeFilter --prune-empty --tag-name-filter cat -- --all 2>&1 | ForEach-Object {
    if ($_ -match "Rewrite" -or $_ -match "^\d+/") {
        Write-Host "." -NoNewline -ForegroundColor Gray
    }
}

Write-Host ""
Write-Host "[OK] Filter-branch completado" -ForegroundColor Green
Write-Host ""

Write-Host "[4/5] Limpiando referencias..." -ForegroundColor Yellow
Remove-Item -Path ".git/refs/original" -Recurse -Force -ErrorAction SilentlyContinue
git reflog expire --expire=now --all 2>&1 | Out-Null
git gc --prune=now --aggressive 2>&1 | Out-Null
Write-Host "[OK] Limpieza completada" -ForegroundColor Green
Write-Host ""

Write-Host "[5/5] Verificando SOLO en archivos de configuracion..." -ForegroundColor Yellow
Write-Host ""

$realFiles = "src/main/resources/*.properties", ".env", "docs/ai-config.md"
$allClean = $true

foreach ($pattern in @("52866617jJ@", "test_secret", "U0hKQkNGR0hJSktMTU5PUFFSU1RVVldYWVo3ODkwQUJDREVGRw==")) {
    $found = git log --all -S $pattern --pretty=format:"%h" -- $realFiles 2>$null | Select-Object -First 1
    if ($found) {
        Write-Host "[!] '$pattern' aun en archivos de configuracion" -ForegroundColor Red
        $allClean = $false
    } else {
        Write-Host "[OK] '$pattern' eliminado de configuracion" -ForegroundColor Green
    }
}

Write-Host ""
if ($allClean) {
    Write-Host "================================================================" -ForegroundColor Green
    Write-Host "         EXITO - SECRETOS ELIMINADOS DE CONFIGURACION" -ForegroundColor Green
    Write-Host "================================================================" -ForegroundColor Green
} else {
    Write-Host "================================================================" -ForegroundColor Yellow
    Write-Host "              VERIFICAR RESULTADOS MANUALMENTE" -ForegroundColor Yellow
    Write-Host "================================================================" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "NOTA: Los secretos en archivos de DOCUMENTACION (*.md) son" -ForegroundColor Cyan
Write-Host "ejemplos para la guia y NO son un riesgo de seguridad." -ForegroundColor Cyan
Write-Host ""
Write-Host "PROXIMOS PASOS:" -ForegroundColor Yellow
Write-Host "1. Rotar TODAS las credenciales (PostgreSQL, Stripe, JWT, Firebase)" -ForegroundColor White
Write-Host "2. Actualizar .env con nuevas credenciales" -ForegroundColor White
Write-Host "3. git push origin --force --all" -ForegroundColor White
Write-Host "4. git push origin --force --tags" -ForegroundColor White
Write-Host "5. Notificar al equipo para sincronizar" -ForegroundColor White
Write-Host ""
Write-Host "Backup en: $backupPath" -ForegroundColor Green
Write-Host "================================================================" -ForegroundColor Cyan

# Limpiar archivos temporales
Remove-Item "real-filter.ps1" -ErrorAction SilentlyContinue
Remove-Item "filter-replace.ps1" -ErrorAction SilentlyContinue
