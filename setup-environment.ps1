#!/usr/bin/env pwsh
<#
.SYNOPSIS
    Script para configurar variables de entorno de ScreenLeads Backend

.DESCRIPTION
    Este script ayuda a configurar las variables de entorno necesarias
    para ejecutar la aplicación ScreenLeads Backend de forma segura.

.PARAMETER Environment
    El ambiente a configurar: dev, pre, pro

.PARAMETER LoadEnvFile
    Cargar variables desde archivo .env

.EXAMPLE
    .\setup-environment.ps1 -Environment dev
    .\setup-environment.ps1 -LoadEnvFile
#>

param(
    [Parameter(Mandatory=$false)]
    [ValidateSet("dev", "pre", "pro")]
    [string]$Environment = "dev",
    
    [Parameter(Mandatory=$false)]
    [switch]$LoadEnvFile
)

# Colores para output
function Write-ColorOutput {
    param(
        [Parameter(Mandatory=$true)]
        [string]$Message,
        
        [Parameter(Mandatory=$false)]
        [string]$Color = "White"
    )
    
    Write-Host $Message -ForegroundColor $Color
}

# Banner
Write-ColorOutput "==========================================" "Cyan"
Write-ColorOutput "  ScreenLeads Backend - Environment Setup" "Cyan"
Write-ColorOutput "==========================================" "Cyan"
Write-Host ""

# Verificar si existe .env
$envFilePath = Join-Path $PSScriptRoot ".env"
$envExamplePath = Join-Path $PSScriptRoot ".env.example"

if ($LoadEnvFile) {
    if (-not (Test-Path $envFilePath)) {
        Write-ColorOutput "[ERROR] Archivo .env no encontrado" "Red"
        Write-ColorOutput "[INFO] Ejecuta: cp .env.example .env" "Yellow"
        exit 1
    }
    
    Write-ColorOutput "[LOADING] Cargando variables desde .env..." "Green"
    
    Get-Content $envFilePath | ForEach-Object {
        $line = $_
        if ($line -match '^\s*([^#][^=]+)\s*=\s*(.*)$') {
            $key = $matches[1].Trim()
            $value = $matches[2].Trim()
            
            # Remover comillas si existen
            if ($value -match '^"(.*)"$') {
                $value = $matches[1]
            }
            if ($value -match "^'(.*)'$") {
                $value = $matches[1]
            }
            
            [System.Environment]::SetEnvironmentVariable($key, $value, "Process")
            Write-ColorOutput "  [OK] $key" "Gray"
        }
    }
    
    Write-Host ""
    Write-ColorOutput "[SUCCESS] Variables de entorno cargadas exitosamente" "Green"
    
} else {
    # Configuración interactiva
    Write-ColorOutput "[CONFIG] Configurando ambiente: $Environment" "Cyan"
    Write-Host ""
    
    if (-not (Test-Path $envFilePath)) {
        Write-ColorOutput "[CREATE] Creando archivo .env desde template..." "Yellow"
        Copy-Item $envExamplePath $envFilePath
        Write-ColorOutput "[SUCCESS] Archivo .env creado. Por favor editalo con tus valores." "Green"
        Write-Host ""
    }
    
    # Mostrar valores requeridos
    Write-ColorOutput "[INFO] Variables de entorno requeridas:" "Yellow"
    Write-Host ""
    
    Write-ColorOutput "DATABASE:" "Cyan"
    Write-Host "  - JDBC_DATABASE_URL"
    Write-Host "  - JDBC_DATABASE_USERNAME"
    Write-Host "  - JDBC_DATABASE_PASSWORD"
    Write-Host ""
    
    Write-ColorOutput "STRIPE:" "Cyan"
    Write-Host "  - STRIPE_SECRET_KEY"
    Write-Host "  - STRIPE_PRICE_ID"
    Write-Host "  - STRIPE_WEBHOOK_SECRET"
    Write-Host ""
    
    Write-ColorOutput "FIREBASE:" "Cyan"
    Write-Host "  - GOOGLE_CREDENTIALS_BASE64"
    Write-Host "  - FIREBASE_STORAGE_BUCKET"
    Write-Host ""
    
    Write-ColorOutput "JWT:" "Cyan"
    Write-Host "  - JWT_SECRET_KEY"
    Write-Host "  - JWT_EXPIRATION"
    Write-Host ""
    
    Write-ColorOutput "[INFO] Edita el archivo .env y luego ejecuta:" "Yellow"
    Write-ColorOutput "   .\setup-environment.ps1 -LoadEnvFile" "White"
}

Write-Host ""

# Verificar variables críticas si se cargó el archivo
if ($LoadEnvFile) {
    Write-ColorOutput "[VERIFY] Verificando variables criticas..." "Cyan"
    
    $criticalVars = @(
        "JDBC_DATABASE_PASSWORD",
        "STRIPE_SECRET_KEY",
        "JWT_SECRET_KEY",
        "GOOGLE_CREDENTIALS_BASE64"
    )
    
    $missing = @()
    foreach ($var in $criticalVars) {
        $value = [System.Environment]::GetEnvironmentVariable($var, "Process")
        if ([string]::IsNullOrWhiteSpace($value) -or $value -like "*your_*" -or $value -like "*placeholder*") {
            $missing += $var
        }
    }
    
    if ($missing.Count -gt 0) {
        Write-Host ""
        Write-ColorOutput "[WARNING] Las siguientes variables no estan configuradas correctamente:" "Yellow"
        foreach ($var in $missing) {
            Write-ColorOutput "  - $var" "Red"
        }
        Write-Host ""
    } else {
        Write-Host ""
        Write-ColorOutput "[SUCCESS] Todas las variables criticas estan configuradas" "Green"
    }
    
    # Mostrar perfil activo
    $activeProfile = [System.Environment]::GetEnvironmentVariable("SPRING_PROFILES_ACTIVE", "Process")
    if ([string]::IsNullOrWhiteSpace($activeProfile)) {
        $activeProfile = "dev (default)"
    }
    
    Write-Host ""
    Write-ColorOutput "[PROFILE] Perfil activo: $activeProfile" "Cyan"
    
    # Comandos útiles
    Write-Host ""
    Write-ColorOutput "[COMMANDS] Comandos utiles:" "Yellow"
    Write-Host "  Ejecutar aplicacion:"
    Write-ColorOutput "    mvn spring-boot:run" "White"
    Write-Host ""
    Write-Host "  Ejecutar con perfil especifico:"
    Write-ColorOutput "    mvn spring-boot:run -Dspring-boot.run.profiles=$Environment" "White"
    Write-Host ""
    Write-Host "  Generar JWT secret:"
    Write-ColorOutput "    [Convert]::ToBase64String((1..64 | ForEach-Object { Get-Random -Maximum 256 }))" "White"
    Write-Host ""
}

Write-ColorOutput "==========================================" "Cyan"
