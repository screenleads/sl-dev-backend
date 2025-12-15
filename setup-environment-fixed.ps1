#!/usr/bin/env pwsh
<#
.SYNOPSIS
    Script para configurar variables de entorno de ScreenLeads Backend

.DESCRIPTION
    Este script ayuda a configurar las variables de entorno necesarias
    para ejecutar la aplicaci?n ScreenLeads Backend de forma segura.

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
    [ValidateSet('dev', 'pre', 'pro')]
    [string]$Environment = 'dev',
    
    [Parameter(Mandatory=$false)]
    [switch]$LoadEnvFile
)

# Colores para output
function Write-ColorOutput {
    param(
        [Parameter(Mandatory=$true)]
        [string]$Message,
        
        [Parameter(Mandatory=$false)]
        [string]$Color = 'White'
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
        Write-ColorOutput "? Error: Archivo .env no encontrado" "Red"
        Write-ColorOutput "?? Ejecuta: cp .env.example .env" "Yellow"
        exit 1
    }
    
    Write-ColorOutput "?? Cargando variables desde .env..." "Green"
    
    Get-Content $envFilePath | ForEach-Object {
        if ($_ -match '^\s*([^#][^=]+)\s*=\s*(.*)$') {
            $key = $matches[1].Trim()
            $value = $matches[2].Trim()
            
            # Remover comillas si existen
            if ($value -match '^"(.*)"$') {
                $value = $matches[1]
            } elseif ($value -match "^'(.*)'$") {
                $value = $matches[1]
            }
            
            [System.Environment]::SetEnvironmentVariable($key, $value, "Process")
            Write-ColorOutput "  ? $key" "Gray"
        }
    }
    
    Write-ColorOutput "`n? Variables de entorno cargadas exitosamente" "Green"
    
} else {
    # Configuraci?n interactiva
    Write-ColorOutput "?? Configurando ambiente: $Environment" "Cyan"
    Write-Host ""
    
    if (-not (Test-Path $envFilePath)) {
        Write-ColorOutput "?? Creando archivo .env desde template..." "Yellow"
        Copy-Item $envExamplePath $envFilePath
        Write-ColorOutput "? Archivo .env creado. Por favor ed?talo con tus valores." "Green"
        Write-Host ""
    }
    
    # Mostrar valores requeridos
    Write-ColorOutput "?? Variables de entorno requeridas:" "Yellow"
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
    
    Write-ColorOutput "Edita el archivo .env y luego ejecuta:" "Yellow"
    Write-ColorOutput "   .\setup-environment.ps1 -LoadEnvFile" "White"
}

Write-Host ""

# Verificar variables cr?ticas si se carg? el archivo
if ($LoadEnvFile) {
    Write-ColorOutput "?? Verificando variables cr?ticas..." "Cyan"
    
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
        Write-ColorOutput "`n??  Las siguientes variables no est?n configuradas correctamente:" "Yellow"
        foreach ($var in $missing) {
            Write-ColorOutput "  - $var" "Red"
        }
        Write-Host ""
    } else {
        Write-ColorOutput "`n? Todas las variables cr?ticas est?n configuradas" "Green"
    }
    
    # Mostrar perfil activo
    $activeProfile = [System.Environment]::GetEnvironmentVariable("SPRING_PROFILES_ACTIVE", "Process")
    if ([string]::IsNullOrWhiteSpace($activeProfile)) {
        $activeProfile = "dev (default)"
    }
    
    Write-ColorOutput "`n?? Perfil activo: $activeProfile" "Cyan"
    
    # Comandos ?tiles
    Write-Host ""
    Write-ColorOutput "?? Comandos ?tiles:" "Yellow"
    Write-Host "  Ejecutar aplicaci?n:"
    Write-ColorOutput "    mvn spring-boot:run" "White"
    Write-Host ""
    Write-Host "  Ejecutar con perfil espec?fico:"
    Write-ColorOutput "    mvn spring-boot:run -Dspring-boot.run.profiles=$Environment" "White"
    Write-Host ""
    Write-Host "  Generar JWT secret:"
    Write-ColorOutput "    [Convert]::ToBase64String((1..64 | ForEach-Object { Get-Random -Maximum 256 }))" "White"
    Write-Host ""
}

Write-ColorOutput "==========================================" "Cyan"
