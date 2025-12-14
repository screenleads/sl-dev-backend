$ErrorActionPreference = "Stop"

$filePath = "d:\Projects\2025\ScreenLeads\Repositories\sl-dev-backend\postman\ScreenLeads-AppVersions-Entities.postman_collection.json"

# Read JSON
$jsonText = [System.IO.File]::ReadAllText($filePath, [System.Text.Encoding]::UTF8)
$json = ConvertFrom-Json $jsonText

# Function to convert auth to API Key in JSON text
function Convert-ToApiKeyAuth {
    param([string]$jsonText)
    
    $obj = ConvertFrom-Json $jsonText
    
    # Check if this is a request item
    if (-not $obj.PSObject.Properties['request']) {
        return $obj
    }
    
    # Update auth - use Add-Member to ensure property exists
    if ($obj.request.PSObject.Properties['auth']) {
        $obj.request.PSObject.Properties.Remove('auth')
    }
    $obj.request | Add-Member -MemberType NoteProperty -Name 'auth' -Value @{ type = "noauth" } -Force
    
    # Update headers
    $headers = @()
    if ($obj.request.PSObject.Properties['header'] -and $obj.request.header) {
        foreach ($h in $obj.request.header) {
            if ($h.key -ne "Authorization") {
                $headers += $h
            }
        }
    }
    
    # Add API Key headers
    $headers += @{
        key = "X-API-KEY"
        value = "{{api_key}}"
        type = "text"
    }
    $headers += @{
        key = "client-id"
        value = "{{client_id}}"
        type = "text"
    }
    
    if ($obj.request.PSObject.Properties['header']) {
        $obj.request.PSObject.Properties.Remove('header')
    }
    $obj.request | Add-Member -MemberType NoteProperty -Name 'header' -Value $headers -Force
    
    return $obj
}

# Extract originalfolders
$appVersions = $json.item[0]
$entities = $json.item[1]

# Clone items for API Key versions
$appVersionsApiKeyItems = @()
foreach ($item in $appVersions.item) {
    $itemJson = $item | ConvertTo-Json -Depth 100 -Compress
    $converted = Convert-ToApiKeyAuth -jsonText $itemJson
    $appVersionsApiKeyItems += $converted
}

$entitiesApiKeyItems = @()
foreach ($item in $entities.item) {
    $itemJson = $item | ConvertTo-Json -Depth 100 -Compress
    $converted = Convert-ToApiKeyAuth -jsonText $itemJson
    $entitiesApiKeyItems += $converted
}

# Build new structure
$newItem = @(
    @{
        name = "JWT Authentication"
        item = @(
            $appVersions,
            $entities
        )
    },
    @{
        name = "API Key Authentication"
        item = @(
            @{
                name = "App Versions"
                item = $appVersionsApiKeyItems
            },
            @{
                name = "Entities (App Entities)"
                item = $entitiesApiKeyItems
            }
        )
    }
)

$json.item = $newItem

# Write back
$outputJson = $json | ConvertTo-Json -Depth 100
[System.IO.File]::WriteAllText($filePath, $outputJson, [System.Text.Encoding]::UTF8)

Write-Host "Success! Collection restructured." -ForegroundColor Green
Write-Host ""
Write-Host "Structure:" -ForegroundColor Cyan
Write-Host "- JWT Authentication" -ForegroundColor White
Write-Host "  -- App Versions: 6 requests" -ForegroundColor Gray
Write-Host "  -- Entities: 8 requests" -ForegroundColor Gray
Write-Host "- API Key Authentication" -ForegroundColor White
Write-Host "  -- App Versions: 6 requests" -ForegroundColor Gray
Write-Host "  -- Entities: 8 requests" -ForegroundColor Gray
