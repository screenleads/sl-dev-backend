# 🛠️ Scripts del Proyecto ScreenLeads Backend

Directorio organizado de scripts para mantenimiento, exportación y migraciones del proyecto.

---

## 📂 Estructura

```
scripts/
├── export/                          # 📸 Generación de snapshots para IA
│   ├── export-entities.sh          # Snapshot de entidades JPA
│   ├── export-snapshots.py         # Sistema de snapshots configurables
│   └── README.md                   # Documentación de exportación
│
├── migrations/                      # 🗄️ Migraciones SQL manuales
│   ├── 001_add_api_key_permissions_and_scope.sql
│   └── README.md                   # Guía de migraciones
│
└── deprecated/                      # 📦 Scripts obsoletos (archivo)
    ├── create_tables.sql
    ├── alter_tables.sql
    └── README.md                   # Por qué están obsoletos
```

---

## 🎯 Uso Rápido

### 📸 Generar Snapshots de Documentación

```bash
# Snapshot de entidades JPA
bash scripts/export/export-entities.sh

# Todos los snapshots configurables
python scripts/export/export-snapshots.py docs/ai-snapshots.json
```

Estos scripts se ejecutan **automáticamente** en GitHub Actions en cada push.

---

### 🗄️ Ejecutar Migraciones

```bash
# Ver migraciones pendientes
cat scripts/migrations/README.md

# Ejecutar migración (PostgreSQL)
psql -U usuario -d screenleads_dev -f scripts/migrations/001_add_api_key_permissions_and_scope.sql
```

⚠️ **Importante**: Lee `scripts/migrations/README.md` antes de ejecutar cualquier migración.

---

## 📋 Estado Actual

### ✅ Scripts Activos

| Script | Estado | Uso |
|--------|--------|-----|
| `export/export-entities.sh` | ✅ Activo | GitHub Actions + Manual |
| `export/export-snapshots.py` | ✅ Activo | GitHub Actions + Manual |
| `migrations/001_add_api_key_permissions_and_scope.sql` | ⏳ Pendiente | Manual - **EJECUTAR ANTES DE DEPLOY** |

### 📦 Scripts Archivados

| Script | Estado | Razón |
|--------|--------|-------|
| `deprecated/create_tables.sql` | 📦 Archivado | Hibernate gestiona schema automáticamente |
| `deprecated/alter_tables.sql` | 📦 Archivado | Schema desactualizado |

### 🗑️ Scripts Eliminados

- ❌ `migrate_api_key_client_id.sql` - Modelo actual ya no usa `client_id`
- ❌ `remove_client_id_from_api_key.sql` - Contradictorio y obsoleto

---

## 🤖 GitHub Actions

### Workflows que usan estos scripts:

1. **`.github/workflows/export-entities.yml`**
   - Ejecuta: `scripts/export/export-entities.sh`
   - Genera: `docs/ai-entities.md`
   - Trigger: Push a cualquier rama

2. **`.github/workflows/export-snapshots.yml`**
   - Ejecuta: `scripts/export/export-snapshots.py`
   - Genera: Múltiples snapshots en `docs/`
   - Trigger: Push a cualquier rama

---

## 🔧 Actualizar Workflows después de Reorganización

Los workflows de GitHub Actions deben actualizarse para reflejar las nuevas rutas:

### Cambios necesarios en `.github/workflows/export-entities.yml`:

```yaml
# Cambiar:
- run: scripts/export-entities.sh

# Por:
- run: scripts/export/export-entities.sh
```

### Cambios necesarios en `.github/workflows/export-snapshots.yml`:

```yaml
# Cambiar:
- run: scripts/export-snapshots.py docs/ai-snapshots.json

# Por:
- run: scripts/export/export-snapshots.py docs/ai-snapshots.json
```

---

## 📚 Documentación Detallada

- **`export/README.md`** - Cómo funcionan los snapshots y cómo personalizarlos
- **`migrations/README.md`** - Guía completa de migraciones SQL
- **`deprecated/README.md`** - Por qué ciertos scripts están obsoletos

---

## 🚀 Próximos Pasos

### 1. ✅ Ejecutar Migración Pendiente

```bash
# DEV (local)
psql -U postgres -d screenleads_dev -f scripts/migrations/001_add_api_key_permissions_and_scope.sql

# PRE (staging)
psql -U <usuario> -h pre-db.screenleads.com -d screenleads_pre -f scripts/migrations/001_add_api_key_permissions_and_scope.sql

# PRO (producción) - ⚠️ Con backup previo
pg_dump -U <usuario> -h db.screenleads.com -d screenleads_pro > backup_pre_migracion_001.sql
psql -U <usuario> -h db.screenleads.com -d screenleads_pro -f scripts/migrations/001_add_api_key_permissions_and_scope.sql
```

### 2. ✅ Actualizar GitHub Actions

Modifica los workflows para usar las nuevas rutas:
- `.github/workflows/export-entities.yml`
- `.github/workflows/export-snapshots.yml`

### 3. ✅ Verificar Snapshots

```bash
# Regenerar snapshots localmente
bash scripts/export/export-entities.sh
python scripts/export/export-snapshots.py docs/ai-snapshots.json

# Verificar que se generaron correctamente
ls -la docs/ai-*.md
```

---

## 📞 Soporte

Para más información sobre el sistema de API Keys y permisos:
- `docs/API_KEY_QUICK_START.md`
- `docs/API_KEY_PERMISSIONS.md`
- `docs/HYBRID_AUTHENTICATION.md`

---

**Última actualización**: Diciembre 2, 2025
**Versión**: 2.0 - Reorganización completa
