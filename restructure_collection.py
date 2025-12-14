import json
import copy

# Read the original file
file_path = r"d:\Projects\2025\ScreenLeads\Repositories\sl-dev-backend\postman\ScreenLeads-AppVersions-Entities.postman_collection.json"

with open(file_path, 'r', encoding='utf-8') as f:
    data = json.load(f)

# Store the original folders
app_versions_folder = data['item'][0]
entities_folder = data['item'][1]

def clone_item_with_api_key_auth(item):
    """Clone an item and convert authentication to API Key"""
    cloned = copy.deepcopy(item)
    
    # Change auth to noauth
    cloned['request']['auth'] = {'type': 'noauth'}
    
    # Ensure header array exists
    if 'header' not in cloned['request'] or cloned['request']['header'] is None:
        cloned['request']['header'] = []
    
    # Add API Key headers
    cloned['request']['header'].append({
        'key': 'X-API-KEY',
        'value': '{{api_key}}',
        'type': 'text'
    })
    
    cloned['request']['header'].append({
        'key': 'client-id',
        'value': '{{client_id}}',
        'type': 'text'
    })
    
    return cloned

# Create JWT Authentication folder
jwt_auth = {
    'name': 'JWT Authentication',
    'item': [
        app_versions_folder,
        entities_folder
    ]
}

# Create API Key Authentication folder
app_versions_api_key_items = []
for item in app_versions_folder['item']:
    cloned = clone_item_with_api_key_auth(item)
    app_versions_api_key_items.append(cloned)

entities_api_key_items = []
for item in entities_folder['item']:
    cloned = clone_item_with_api_key_auth(item)
    entities_api_key_items.append(cloned)

api_key_auth = {
    'name': 'API Key Authentication',
    'item': [
        {
            'name': 'App Versions',
            'item': app_versions_api_key_items
        },
        {
            'name': 'Entities (App Entities)',
            'item': entities_api_key_items
        }
    ]
}

# Update the main item array
data['item'] = [jwt_auth, api_key_auth]

# Save the file
with open(file_path, 'w', encoding='utf-8') as f:
    json.dump(data, f, indent=4, ensure_ascii=False)

print("✓ Collection restructured successfully!")
print("\nNew structure:")
print("- JWT Authentication")
print("  - App Versions (6 requests)")
print("  - Entities (App Entities) (8 requests)")
print("- API Key Authentication")
print("  - App Versions (6 requests)")
print("  - Entities (App Entities) (8 requests)")
