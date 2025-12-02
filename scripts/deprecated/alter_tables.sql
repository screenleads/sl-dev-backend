-- ALTER para tabla client
ALTER TABLE client
    ADD COLUMN IF NOT EXISTS active BOOLEAN NOT NULL DEFAULT true,
    ADD COLUMN IF NOT EXISTS client_id VARCHAR(100),
    ADD COLUMN IF NOT EXISTS name VARCHAR(255);

-- Si client_id ya existe pero no es único, hazlo único
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE table_name='client' AND constraint_type='UNIQUE' AND constraint_name='client_client_id_key'
    ) THEN
        BEGIN
            ALTER TABLE client ADD CONSTRAINT client_client_id_key UNIQUE (client_id);
        EXCEPTION WHEN duplicate_object THEN NULL;
        END;
    END IF;
END$$;

-- ALTER para tabla api_key
ALTER TABLE api_key
    ADD COLUMN IF NOT EXISTS active BOOLEAN NOT NULL DEFAULT true,
    ADD COLUMN IF NOT EXISTS client BIGINT,
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT NOW(),
    ADD COLUMN IF NOT EXISTS expires_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS key VARCHAR(255),
    ADD COLUMN IF NOT EXISTS permissions VARCHAR(255);

-- Si la columna client existe y no tiene FK, agrégala
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE table_name='api_key' AND constraint_type='FOREIGN KEY' AND constraint_name='fk_api_key_client'
    ) THEN
        BEGIN
            ALTER TABLE api_key ADD CONSTRAINT fk_api_key_client FOREIGN KEY (client) REFERENCES client(id) ON DELETE CASCADE;
        EXCEPTION WHEN duplicate_object THEN NULL;
        END;
    END IF;
END$$;
