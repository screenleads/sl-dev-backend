# Verificacion CI/CD Setup
Write-Host "Verificando configuracion CI/CD..." -ForegroundColor Cyan

$files = @(
    ".github/workflows/ci-tests.yml",
    ".github/workflows/sonarqube.yml",
    ".github/dependabot.yml",
    "scripts/pre-commit-check.ps1",
    "scripts/sonar-scan.ps1",
    "CI_CD_GUIDE.md"
)

$allOk = $true
foreach ($f in $files) {
    if (Test-Path $f) {
        Write-Host "OK: $f" -ForegroundColor Green
    } else {
        Write-Host "FALTA: $f" -ForegroundColor Red
        $allOk = $false
    }
}

if ($allOk) {
    Write-Host "`nTODO CONFIGURADO!" -ForegroundColor Green
} else {
    Write-Host "`nFaltan archivos" -ForegroundColor Red
}
