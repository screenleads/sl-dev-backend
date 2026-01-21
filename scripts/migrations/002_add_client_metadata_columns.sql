-- Añadir columnas faltantes a la tabla client
ALTER TABLE client
    ADD COLUMN IF NOT EXISTS description VARCHAR(500),
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT NOW(),
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT NOW();

-- Crear índice en created_at para consultas por fecha
CREATE INDEX IF NOT EXISTS idx_client_created_at ON client(created_at);
