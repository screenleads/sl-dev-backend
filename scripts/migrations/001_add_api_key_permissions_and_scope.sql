-- Migration: Add permissions and company scope to API Keys
-- Fecha: 2025-12-02
-- Descripción: Agrega campos para permisos granulares y alcance de compañía a las API Keys

-- 1. Agregar columna company_scope (puede ser NULL = acceso global)
ALTER TABLE api_key
ADD COLUMN company_scope BIGINT NULL;

-- 2. Agregar comentario explicativo
COMMENT ON COLUMN api_key.company_scope IS 'ID de compañía para acceso restringido. NULL = acceso global a todas las compañías';

-- 3. Modificar columna permissions para soportar texto más largo
ALTER TABLE api_key
ALTER COLUMN permissions TYPE TEXT;

-- 4. Agregar columna description (opcional, para documentar el propósito de cada key)
ALTER TABLE api_key
ADD COLUMN description VARCHAR(255) NULL;

-- 5. Agregar índice para mejorar performance en filtros por company_scope
CREATE INDEX idx_api_key_company_scope ON api_key(company_scope) WHERE company_scope IS NOT NULL;

-- 6. Agregar foreign key constraint si la tabla company existe
-- (Descomenta si quieres establecer la relación)
-- ALTER TABLE api_key
-- ADD CONSTRAINT fk_api_key_company_scope
-- FOREIGN KEY (company_scope) REFERENCES company(id)
-- ON DELETE CASCADE;

-- Ejemplos de uso:

-- API Key con acceso global y permisos de lectura en snapshots y leads:
-- UPDATE api_key SET 
--   permissions = 'snapshot:read,snapshot:create,lead:read,lead:update',
--   company_scope = NULL,
--   description = 'Integration key with global read access'
-- WHERE id = 1;

-- API Key con acceso restringido a una compañía específica:
-- UPDATE api_key SET 
--   permissions = 'snapshot:*,lead:*,company:read',
--   company_scope = 42,
--   description = 'Full access key for Company ABC (ID: 42)'
-- WHERE id = 2;

-- API Key con permisos de superadmin (todas las acciones, todos los recursos, todas las compañías):
-- UPDATE api_key SET 
--   permissions = '*:*',
--   company_scope = NULL,
--   description = 'Super admin key - use with caution'
-- WHERE id = 3;
