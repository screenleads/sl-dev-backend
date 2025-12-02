# 📦 Scripts Obsoletos / Deprecated

Esta carpeta contiene scripts SQL que fueron creados en versiones anteriores del proyecto pero **ya no son necesarios o están obsoletos**.

---

## ⚠️ ¡IMPORTANTE!

**NO ejecutes estos scripts** a menos que sepas exactamente qué hacen y por qué los necesitas.

Están archivados aquí por **referencia histórica** únicamente.

---

## 📋 Scripts Archivados

### `create_tables.sql`

**Propósito Original**: Crear las tablas `client` y `api_key` desde cero

**Por qué está obsoleto**:
- ❌ El proyecto usa **Hibernate/JPA** que crea las tablas automáticamente
- ❌ El esquema está **desactualizado** (falta `company_scope`, `description`)
- ❌ No incluye todas las columnas que usa la entidad actual

**Alternativa**:
- Deja que Hibernate maneje la creación de tablas
- Configuración en `application.properties`: `spring.jpa.hibernate.ddl-auto=update`

---

### `alter_tables.sql`

**Propósito Original**: Añadir columnas a tablas existentes (client, api_key)

**Por qué está obsoleto**:
- ❌ Hibernate gestiona los cambios de schema automáticamente
- ❌ El esquema está **desactualizado**
- ❌ Puede causar conflictos con las migraciones automáticas de Hibernate

**Alternativa**:
- Deja que Hibernate actualice el schema
- Para cambios complejos, usa scripts en `scripts/migrations/`

---

## 🗑️ Scripts Eliminados (ya no existen)

Estos scripts fueron **completamente eliminados** porque eran contradictorios o completamente obsoletos:

1. **`migrate_api_key_client_id.sql`**
   - Propósito: Convertir `client_id` de VARCHAR a BIGINT
   - Por qué se eliminó: La entidad actual ya NO usa `client_id`, usa `@ManyToOne Client client`

2. **`remove_client_id_from_api_key.sql`**
   - Propósito: Eliminar columna `client_id`
   - Por qué se eliminó: Contradice el script anterior, el modelo actual ya no tiene esta columna

---

## 🔍 ¿Cuándo usar estos scripts?

**Casi nunca.** Solo en casos excepcionales como:

- 🔧 Debugging histórico de problemas de schema
- 📚 Referencia para entender cómo era el modelo anterior
- 🔄 Recuperación de desastres (muy poco probable)

---

## 🚀 ¿Qué usar en su lugar?

Para gestionar el schema de la base de datos:

1. **Desarrollo**: 
   - `spring.jpa.hibernate.ddl-auto=update` en `application-dev.properties`
   - Hibernate crea/actualiza tablas automáticamente

2. **Producción**:
   - `spring.jpa.hibernate.ddl-auto=validate` en `application-pro.properties`
   - Usa scripts manuales en `scripts/migrations/` para cambios controlados

3. **Migraciones Complejas**:
   - Crea un nuevo script numerado en `scripts/migrations/`
   - Ejemplo: `002_add_new_feature.sql`

---

## 📖 Recursos

- **Migraciones activas**: `scripts/migrations/`
- **Documentación de Hibernate DDL**: https://docs.jboss.org/hibernate/orm/current/userguide/html_single/Hibernate_User_Guide.html#schema-generation

---

**Archivado**: Diciembre 2, 2025
