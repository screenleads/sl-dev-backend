#!/usr/bin/env pwsh
# Script para ejecutar análisis de SonarQube localmente

param(
    [string]$Token = $env:SONAR_TOKEN,
    [string]$HostUrl = "https://sonarcloud.io"
)

Write-Host "🔍 SonarQube Local Analysis" -ForegroundColor Cyan
Write-Host "============================`n" -ForegroundColor Cyan

# Verificar token
if ([string]::IsNullOrEmpty($Token)) {
    Write-Host "❌ Error: SONAR_TOKEN no configurado" -ForegroundColor Red
    Write-Host "`nConfigura el token con:" -ForegroundColor Yellow
    Write-Host '  $env:SONAR_TOKEN = "tu-token-aqui"' -ForegroundColor Gray
    Write-Host "  O pásalo como parámetro: -Token 'tu-token'" -ForegroundColor Gray
    exit 1
}

Write-Host "📦 Paso 1: Compilando proyecto..." -ForegroundColor Yellow
mvn clean compile

if ($LASTEXITCODE -ne 0) {
    Write-Host "`n❌ Compilación fallida" -ForegroundColor Red
    exit 1
}

Write-Host "`n🧪 Paso 2: Ejecutando tests..." -ForegroundColor Yellow
mvn test

if ($LASTEXITCODE -ne 0) {
    Write-Host "`n❌ Tests fallaron" -ForegroundColor Red
    exit 1
}

Write-Host "`n📊 Paso 3: Generando reporte JaCoCo..." -ForegroundColor Yellow
mvn jacoco:report

Write-Host "`n🚀 Paso 4: Analizando con SonarQube..." -ForegroundColor Yellow
Write-Host "   Host: $HostUrl" -ForegroundColor Gray
Write-Host "   Project: screenleads_sl-dev-backend`n" -ForegroundColor Gray

mvn sonar:sonar `
    -Dsonar.token=$Token `
    -Dsonar.host.url=$HostUrl `
    -Dsonar.projectKey=screenleads_sl-dev-backend `
    -Dsonar.projectName="ScreenLeads Backend" `
    -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml

if ($LASTEXITCODE -ne 0) {
    Write-Host "`n❌ Análisis de SonarQube fallido" -ForegroundColor Red
    exit 1
}

Write-Host "`n✅ Análisis completado!" -ForegroundColor Green
Write-Host "`n📈 Ver resultados en:" -ForegroundColor Cyan
Write-Host "   $HostUrl/dashboard?id=screenleads_sl-dev-backend" -ForegroundColor Blue
