-- Tabla client
CREATE TABLE client (
    id BIGSERIAL PRIMARY KEY,
    active BOOLEAN NOT NULL DEFAULT true,
    client_id VARCHAR(100) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL
);

-- Tabla api_key
CREATE TABLE api_key (
    id BIGSERIAL PRIMARY KEY,
    active BOOLEAN NOT NULL DEFAULT true,
    client BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMP,
    key VARCHAR(255) NOT NULL,
    permissions VARCHAR(255),
    CONSTRAINT fk_api_key_client FOREIGN KEY (client) REFERENCES client(id) ON DELETE CASCADE
);
