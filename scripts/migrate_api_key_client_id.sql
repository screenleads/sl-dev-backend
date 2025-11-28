-- MIGRACIÓN: Corrige el tipo de la columna client_id en api_key y crea la clave foránea

-- 1. Cambia el tipo de la columna client_id a bigint
ALTER TABLE api_key
    ALTER COLUMN client_id TYPE bigint USING client_id::bigint;

-- 2. Crea la clave foránea si no existe
ALTER TABLE api_key
    ADD CONSTRAINT fk_api_key_client
    FOREIGN KEY (client_id) REFERENCES client(id);

-- 3. (Opcional) Verifica la estructura
-- \d api_key
-- \d client

-- 4. (Opcional) Elimina la columna antigua si existe duplicada
-- ALTER TABLE api_key DROP COLUMN client_id_old;
