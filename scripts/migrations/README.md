# 🗄️ Migraciones de Base de Datos

Este directorio contiene **migraciones SQL manuales** que deben ejecutarse en la base de datos cuando el esquema de Hibernate/JPA no puede gestionar automáticamente ciertos cambios (como añadir columnas con valores por defecto específicos o transformaciones de datos).

---

## 📋 Estado de las Migraciones

| # | Script | Estado | Descripción | Fecha Objetivo |
|---|--------|--------|-------------|----------------|
| 001 | `001_add_api_key_permissions_and_scope.sql` | ⏳ **PENDIENTE** | Añade `company_scope` y `description` a la tabla `api_key` | Pre-deploy sistema de permisos granulares |

---

## 🚀 Cómo Ejecutar una Migración

### Opción 1: Usando psql (PostgreSQL CLI)

```bash
# Conectar a la base de datos
psql -U <usuario> -d <nombre_bd>

# Ejecutar el script
\i scripts/migrations/001_add_api_key_permissions_and_scope.sql

# Verificar los cambios
\d api_key
```

### Opción 2: Usando pgAdmin o DBeaver

1. Abre el script en tu cliente SQL favorito
2. Conéctate a la base de datos correspondiente (**dev**, **pre** o **pro**)
3. Ejecuta el script completo
4. Verifica que las columnas se hayan añadido correctamente

### Opción 3: Desde la terminal (PowerShell/Windows)

```powershell
# Variable de entorno con la URL de conexión
$env:DATABASE_URL = "postgresql://usuario:password@localhost:5432/screenleads_dev"

# Ejecutar con psql
psql $env:DATABASE_URL -f scripts/migrations/001_add_api_key_permissions_and_scope.sql
```

---

## ⚠️ Consideraciones Importantes

### 🎯 Orden de Ejecución

- Las migraciones están numeradas secuencialmente (`001`, `002`, etc.)
- **Deben ejecutarse en orden** en cada entorno
- Una vez ejecutada, marcarla como ✅ **EJECUTADA** en la tabla de arriba

### 🌍 Ejecutar en TODOS los Entornos

Recuerda ejecutar cada migración en **todos** tus entornos:

1. ✅ **DEV** (local) - Primero siempre
2. ✅ **PRE** (preproducción) - Después de validar en DEV
3. ✅ **PRO** (producción) - Solo después de validar en PRE

### 🔒 Backups Antes de Migrar

```bash
# Hacer backup de la BD antes de migrar (ESPECIALMENTE EN PRO)
pg_dump -U usuario -d screenleads_pro > backup_antes_migracion_001.sql
```

### 📝 Registro de Ejecución

Cuando ejecutes una migración, documenta:
- ✅ Fecha de ejecución
- ✅ Entorno (dev/pre/pro)
- ✅ Usuario que la ejecutó
- ✅ Resultado (éxito/errores)

---

## 📚 Migraciones Detalladas

### 001 - Añadir Permisos Granulares a API Keys ⏳ PENDIENTE

**Archivo**: `001_add_api_key_permissions_and_scope.sql`

**Objetivo**: Habilitar el sistema de permisos granulares para API Keys con:
- **`company_scope`**: Permite restringir el acceso de una API Key a una compañía específica (NULL = acceso global)
- **`description`**: Añade un campo descriptivo para identificar fácilmente cada API Key

**Impacto**:
- ✅ No destructivo (solo añade columnas)
- ✅ Compatible con registros existentes (valores NULL permitidos)
- ✅ No requiere downtime

**Prerequisitos**:
- Tabla `api_key` debe existir
- Hibernate debe estar configurado con `spring.jpa.hibernate.ddl-auto=update` o `validate`

**Validación Post-Migración**:
```sql
-- Verificar que las columnas existen
SELECT column_name, data_type, is_nullable 
FROM information_schema.columns 
WHERE table_name = 'api_key' 
AND column_name IN ('company_scope', 'description');

-- Resultado esperado:
-- company_scope | bigint | YES
-- description   | character varying(255) | YES
```

**Rollback** (si es necesario):
```sql
ALTER TABLE api_key DROP COLUMN IF EXISTS company_scope;
ALTER TABLE api_key DROP COLUMN IF EXISTS description;
```

---

## 🔧 Troubleshooting

### Error: "relation api_key does not exist"
- La tabla no existe todavía
- Solución: Ejecuta la aplicación primero para que Hibernate cree las tablas base

### Error: "column already exists"
- La migración ya fue ejecutada
- Solución: Verifica el historial de migraciones

### Error: "permission denied"
- Usuario de BD sin permisos
- Solución: Usa un usuario con permisos `ALTER TABLE`

---

## 📖 Recursos Adicionales

- **Documentación del sistema de permisos**: `docs/API_KEY_PERMISSIONS.md`
- **Guía rápida de API Keys**: `docs/API_KEY_QUICK_START.md`
- **Arquitectura híbrida**: `docs/HYBRID_AUTHENTICATION.md`

---

**Última actualización**: Diciembre 2, 2025
**Maintainer**: ScreenLeads Backend Team
