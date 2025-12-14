$filePath = "d:\Projects\2025\ScreenLeads\Repositories\sl-dev-backend\postman\ScreenLeads-AppVersions-Entities.postman_collection.json"

# Read the JSON file
$content = Get-Content $filePath -Raw -Encoding UTF8
$json = $content | ConvertFrom-Json

# Store the original folders (make deep copies)
$appVersionsOriginal = $json.item[0] | ConvertTo-Json -Depth 100 | ConvertFrom-Json
$entitiesOriginal = $json.item[1] | ConvertTo-Json -Depth 100 | ConvertFrom-Json

# Function to create API Key version of an item
function ConvertTo-ApiKeyAuth {
    param($item)
    
    # Deep clone
    $cloned = $item | ConvertTo-Json -Depth 100 | ConvertFrom-Json
    
    # Update auth
    $cloned.request.auth = @{ type = "noauth" }
    
    # Create headers list
    $headers = @()
    
    # Keep existing headers except auth-related ones
    if ($cloned.request.header) {
        foreach ($h in $cloned.request.header) {
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
    
    $cloned.request.header = $headers
    
    return $cloned
}

# Create API Key versions
$appVersionsApiKey = @()
foreach ($item in $appVersionsOriginal.item) {
    $appVersionsApiKey += ConvertTo-ApiKeyAuth -item $item
}

$entitiesApiKey = @()
foreach ($item in $entitiesOriginal.item) {
    $entitiesApiKey += ConvertTo-ApiKeyAuth -item $item
}

# Build new structure
$newStructure = @(
    @{
        name = "JWT Authentication"
        item = @(
            $appVersionsOriginal,
            $entitiesOriginal
        )
    },
    @{
        name = "API Key Authentication"
        item = @(
            @{
                name = "App Versions"
                item = $appVersionsApiKey
            },
            @{
                name = "Entities (App Entities)"
                item = $entitiesApiKey
            }
        )
    }
)

# Update the JSON
$json.item = $newStructure

# Save back to file
$outputJson = $json | ConvertTo-Json -Depth 100
$outputJson | Set-Content $filePath -Encoding UTF8

Write-Host "Collection restructured successfully!" -ForegroundColor Green
Write-Host ""
Write-Host "New structure:" -ForegroundColor Cyan
Write-Host "  1. JWT Authentication" -ForegroundColor White
Write-Host "     - App Versions - 6 requests with Bearer token" -ForegroundColor Gray
Write-Host "     - Entities - 8 requests with Bearer token" -ForegroundColor Gray
Write-Host "  2. API Key Authentication" -ForegroundColor White
Write-Host "     - App Versions - 6 requests with X-API-KEY + client-id" -ForegroundColor Gray
Write-Host "     - Entities - 8 requests with X-API-KEY + client-id" -ForegroundColor Gray
