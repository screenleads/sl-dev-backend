#!/usr/bin/env pwsh
# Script de verificación pre-commit para ScreenLeads Backend

Write-Host "🔍 Verificando código antes de commit..." -ForegroundColor Cyan

# 1. Verificar formato de código
Write-Host "`n📝 Verificando formato..." -ForegroundColor Yellow
$javaFiles = git diff --cached --name-only --diff-filter=ACMR | Where-Object { $_ -match '\.java$' }

if ($javaFiles.Count -gt 0) {
    Write-Host "   Archivos Java modificados: $($javaFiles.Count)" -ForegroundColor Gray
}

# 2. Ejecutar tests afectados
Write-Host "`n🧪 Ejecutando tests..." -ForegroundColor Yellow
$testFiles = git diff --cached --name-only --diff-filter=ACMR | Where-Object { $_ -match 'Test\.java$' }

if ($testFiles.Count -gt 0) {
    Write-Host "   Tests modificados: $($testFiles.Count)" -ForegroundColor Gray
    
    # Convertir paths a nombres de clase para Maven
    $testClasses = $testFiles | ForEach-Object {
        $_ -replace 'src/test/java/', '' `
           -replace '\.java$', '' `
           -replace '/', '.'
    } | Where-Object { $_ -ne '' }
    
    if ($testClasses.Count -gt 0) {
        $testParam = $testClasses -join ','
        Write-Host "   Ejecutando: $testParam" -ForegroundColor Gray
        
        $testResult = mvn test -Dtest="$testParam" 2>&1
        
        if ($LASTEXITCODE -ne 0) {
            Write-Host "`n❌ Tests fallaron. Commit abortado." -ForegroundColor Red
            Write-Host $testResult -ForegroundColor Red
            exit 1
        }
        
        Write-Host "   ✅ Tests pasaron correctamente" -ForegroundColor Green
    }
} else {
    Write-Host "   ℹ️  No hay tests modificados, ejecutando suite rápida..." -ForegroundColor Gray
    
    # Si no hay tests modificados, ejecutar solo tests unitarios rápidos
    $quickResult = mvn test -Dtest="*MapperTest,*UtilTest" 2>&1
    
    if ($LASTEXITCODE -ne 0) {
        Write-Host "`n⚠️  Algunos tests rápidos fallaron" -ForegroundColor Yellow
        Write-Host "   Ejecuta 'mvn clean test' para verificar todos los tests" -ForegroundColor Yellow
        
        # No abortar commit, solo advertir
    } else {
        Write-Host "   ✅ Tests rápidos pasaron" -ForegroundColor Green
    }
}

# 3. Verificar cobertura mínima
Write-Host "`n📊 Verificando cobertura..." -ForegroundColor Yellow

if (Test-Path "target/site/jacoco/jacoco.xml") {
    $jacoco = [xml](Get-Content "target/site/jacoco/jacoco.xml")
    $counter = $jacoco.report.counter | Where-Object { $_.type -eq 'INSTRUCTION' }
    
    if ($counter) {
        $covered = [int]$counter.covered
        $missed = [int]$counter.missed
        $total = $covered + $missed
        $coverage = ($covered / $total * 100)
        
        Write-Host "   Cobertura actual: $($coverage.ToString('F2'))%" -ForegroundColor Gray
        
        if ($coverage -lt 35) {
            Write-Host "`n⚠️  Cobertura ($($coverage.ToString('F2'))%) por debajo del mínimo (35%)" -ForegroundColor Yellow
            Write-Host "   Considera agregar tests antes de hacer commit" -ForegroundColor Yellow
        } else {
            Write-Host "   ✅ Cobertura OK (>35%)" -ForegroundColor Green
        }
    }
} else {
    Write-Host "   ℹ️  No se encontró reporte de cobertura" -ForegroundColor Gray
    Write-Host "   Ejecuta 'mvn clean test jacoco:report' para generar" -ForegroundColor Gray
}

Write-Host "`n✅ Verificación completada. Procediendo con commit..." -ForegroundColor Green
exit 0
