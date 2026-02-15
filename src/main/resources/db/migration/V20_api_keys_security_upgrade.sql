-- ===================================================================
-- Script de Migración: API Keys Avanzadas
-- Descripción: Agrega seguridad con hash, campos de revocación y scopes
-- Fecha: 2026-02-15
-- ===================================================================

-- IMPORTANTE: Este script NO puede migrar las keys existentes porque están en texto plano
-- Opción 1: Regenerar todas las API keys después de la migración
-- Opción 2: Tabla temporal para keys viejas

-- ===================================================================
-- PASO 1: Agregar nuevas columnas
-- ===================================================================

ALTER TABLE api_key ADD COLUMN IF NOT EXISTS key_hash VARCHAR(60);
ALTER TABLE api_key ADD COLUMN IF NOT EXISTS key_prefix VARCHAR(15);
ALTER TABLE api_key ADD COLUMN IF NOT EXISTS scopes VARCHAR(500);
ALTER TABLE api_key ADD COLUMN IF NOT EXISTS last_used_at TIMESTAMP;
ALTER TABLE api_key ADD COLUMN IF NOT EXISTS usage_count INTEGER DEFAULT 0;
ALTER TABLE api_key ADD COLUMN IF NOT EXISTS revoked_at TIMESTAMP;
ALTER TABLE api_key ADD COLUMN IF NOT EXISTS revoked_reason VARCHAR(500);
ALTER TABLE api_key ADD COLUMN IF NOT EXISTS revoked_by BIGINT;

-- ===================================================================
-- PASO 2: Migrar datos de permissions a scopes (si existe)
-- ===================================================================

UPDATE api_key 
SET scopes = permissions 
WHERE permissions IS NOT NULL AND scopes IS NULL;

-- ===================================================================
-- PASO 3: Generar prefijos para keys existentes (temporal)
-- ===================================================================

-- Para keys existentes, generamos un prefijo basado en su ID
-- NOTA: Estas keys NO funcionarán hasta que se regeneren
UPDATE api_key 
SET key_prefix = CONCAT('sk_migr_', LPAD(id::TEXT, 4, '0'))
WHERE key_prefix IS NULL;

-- Hashear las keys existentes (aunque no son válidas, es para cumplir constraint)
-- IMPORTANTE: Estas keys deben regenerarse manualmente
UPDATE api_key 
SET key_hash = '$2a$10$invalidHashForMigrationPurposesOnly'
WHERE key_hash IS NULL;

-- ===================================================================
-- PASO 4: Agregar Foreign Key para revoked_by
-- ===================================================================

ALTER TABLE api_key 
ADD CONSTRAINT fk_api_key_revoked_by 
FOREIGN KEY (revoked_by) REFERENCES public.user(id) 
ON DELETE SET NULL;

-- ===================================================================
-- PASO 5: Renombrar columnas antiguas (backup)
-- ===================================================================

-- Renombrar la columna key antigua a key_old (por si acaso)
ALTER TABLE api_key RENAME COLUMN key TO key_old;
ALTER TABLE api_key RENAME COLUMN permissions TO permissions_old;

-- ===================================================================
-- PASO 6: Agregar índices para performance
-- ===================================================================

CREATE INDEX IF NOT EXISTS idx_api_key_prefix ON api_key(key_prefix);
CREATE INDEX IF NOT EXISTS idx_api_key_active ON api_key(active);
CREATE INDEX IF NOT EXISTS idx_api_key_revoked_at ON api_key(revoked_at);
CREATE INDEX IF NOT EXISTS idx_api_key_expires_at ON api_key(expires_at);

-- ===================================================================
-- PASO 7: Agregar comentarios a la tabla
-- ===================================================================

COMMENT ON COLUMN api_key.key_hash IS 'BCrypt hash de la API key (nunca almacenar key en texto plano)';
COMMENT ON COLUMN api_key.key_prefix IS 'Primeros 12 caracteres de la key para identificación visual (ej: sk_live_abc1)';
COMMENT ON COLUMN api_key.scopes IS 'Permisos separados por coma (ej: customers:read,campaigns:write)';
COMMENT ON COLUMN api_key.revoked_at IS 'Timestamp de revocación (NULL = activa)';
COMMENT ON COLUMN api_key.revoked_reason IS 'Motivo de la revocación';
COMMENT ON COLUMN api_key.usage_count IS 'Contador de veces que se ha usado la key';
COMMENT ON COLUMN api_key.last_used_at IS 'Última vez que se usó la key';

-- ===================================================================
-- PASO 8: Insertar scopes de ejemplo (catálogo)
-- ===================================================================

-- Crear tabla para documentar scopes disponibles (opcional)
CREATE TABLE IF NOT EXISTS api_key_scope_catalog (
    scope VARCHAR(100) PRIMARY KEY,
    description VARCHAR(255) NOT NULL,
    category VARCHAR(50) NOT NULL
);

INSERT INTO api_key_scope_catalog (scope, description, category) VALUES
('customers:read', 'Leer información de clientes', 'Customers'),
('customers:write', 'Crear y actualizar clientes', 'Customers'),
('customers:delete', 'Eliminar clientes', 'Customers'),
('campaigns:read', 'Leer campañas y promociones', 'Campaigns'),
('campaigns:write', 'Crear y actualizar campañas', 'Campaigns'),
('campaigns:delete', 'Eliminar campañas', 'Campaigns'),
('leads:read', 'Leer leads capturados', 'Leads'),
('leads:write', 'Crear y actualizar leads', 'Leads'),
('leads:delete', 'Eliminar leads', 'Leads'),
('analytics:read', 'Acceder a estadísticas y reportes', 'Analytics'),
('templates:read', 'Leer plantillas de notificación', 'Templates'),
('templates:write', 'Crear y editar plantillas', 'Templates'),
('company:read', 'Leer información de la compañía', 'Company'),
('company:write', 'Actualizar configuración de la compañía', 'Company'),
('webhooks:manage', 'Gestionar webhooks', 'Webhooks'),
('invitations:read', 'Leer invitaciones de usuarios', 'Invitations'),
('invitations:write', 'Crear invitaciones', 'Invitations'),
('snapshots:read', 'Leer snapshots de tracking', 'Snapshots'),
('snapshots:write', 'Crear snapshots', 'Snapshots')
ON CONFLICT (scope) DO NOTHING;

-- ===================================================================
-- VERIFICACIÓN
-- ===================================================================

SELECT 
    COUNT(*) as total_keys,
    COUNT(CASE WHEN key_hash IS NOT NULL THEN 1 END) as keys_with_hash,
    COUNT(CASE WHEN key_prefix IS NOT NULL THEN 1 END) as keys_with_prefix,
    COUNT(CASE WHEN revoked_at IS NULL AND active = true THEN 1 END) as active_keys
FROM api_key;

-- ===================================================================
-- NOTAS IMPORTANTES PARA EL EQUIPO
-- ===================================================================

-- ⚠️ ATENCIÓN: Las API keys existentes NO funcionarán después de esta migración
-- 
-- ¿Por qué?
-- - La columna 'key' antigua contenía keys en texto plano
-- - Ahora usamos 'key_hash' con BCrypt
-- - No podemos hashear las keys viejas porque BCrypt genera hash distinto cada vez
--
-- ¿Qué hacer?
-- 1. ANTES de la migración: Avisar a todos los usuarios que usan API keys
-- 2. DESPUÉS de la migración: Regenerar todas las API keys desde el dashboard
-- 3. Usar el endpoint POST /api-keys/{id}/rotate para cada key
-- 4. Distribuir las nuevas keys a los sistemas que las usan
--
-- ¿Cómo recuperar keys viejas? (temporal)
-- SELECT id, key_old FROM api_key WHERE key_old IS NOT NULL;
-- 
-- Después de 30 días sin problemas:
-- ALTER TABLE api_key DROP COLUMN key_old;
-- ALTER TABLE api_key DROP COLUMN permissions_old;
