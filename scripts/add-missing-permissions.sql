-- Script para agregar recursos faltantes en la tabla app_entity
-- Usuario admin (level 1) debería poder hacer cualquier acción
-- Por lo tanto, todos los niveles de permiso se configuran a 1

-- Verificar si existe la tabla app_entity
-- Si no existe, crearla (esto es por si acaso)
CREATE TABLE IF NOT EXISTS app_entity (
    id BIGSERIAL PRIMARY KEY,
    resource VARCHAR(255) NOT NULL UNIQUE,
    read_level INTEGER,
    create_level INTEGER,
    update_level INTEGER,
    delete_level INTEGER,
    write_level INTEGER
);

-- Insertar recursos faltantes (ON CONFLICT para evitar duplicados)

-- Analytics (NUEVO - el que causaba el error)
INSERT INTO app_entity (resource, read_level, create_level, update_level, delete_level, write_level)
VALUES ('analytics', 1, 1, 1, 1, 1)
ON CONFLICT (resource) DO UPDATE SET
    read_level = 1,
    create_level = 1,
    update_level = 1,
    delete_level = 1,
    write_level = 1;

-- Advice (anuncios)
INSERT INTO app_entity (resource, read_level, create_level, update_level, delete_level, write_level)
VALUES ('advice', 1, 1, 1, 1, 1)
ON CONFLICT (resource) DO UPDATE SET
    read_level = 1,
    create_level = 1,
    update_level = 1,
    delete_level = 1,
    write_level = 1;

-- Device (dispositivos)
INSERT INTO app_entity (resource, read_level, create_level, update_level, delete_level, write_level)
VALUES ('device', 1, 1, 1, 1, 1)
ON CONFLICT (resource) DO UPDATE SET
    read_level = 1,
    create_level = 1,
    update_level = 1,
    delete_level = 1,
    write_level = 1;

-- Media (medios)
INSERT INTO app_entity (resource, read_level, create_level, update_level, delete_level, write_level)
VALUES ('media', 1, 1, 1, 1, 1)
ON CONFLICT (resource) DO UPDATE SET
    read_level = 1,
    create_level = 1,
    update_level = 1,
    delete_level = 1,
    write_level = 1;

-- Promotion (promociones)
INSERT INTO app_entity (resource, read_level, create_level, update_level, delete_level, write_level)
VALUES ('promotion', 1, 1, 1, 1, 1)
ON CONFLICT (resource) DO UPDATE SET
    read_level = 1,
    create_level = 1,
    update_level = 1,
    delete_level = 1,
    write_level = 1;

-- Lead (leads/contactos)
INSERT INTO app_entity (resource, read_level, create_level, update_level, delete_level, write_level)
VALUES ('lead', 1, 1, 1, 1, 1)
ON CONFLICT (resource) DO UPDATE SET
    read_level = 1,
    create_level = 1,
    update_level = 1,
    delete_level = 1,
    write_level = 1;

-- User (usuarios)
INSERT INTO app_entity (resource, read_level, create_level, update_level, delete_level, write_level)
VALUES ('user', 1, 1, 1, 1, 1)
ON CONFLICT (resource) DO UPDATE SET
    read_level = 1,
    create_level = 1,
    update_level = 1,
    delete_level = 1,
    write_level = 1;

-- Media-type (tipos de media)
INSERT INTO app_entity (resource, read_level, create_level, update_level, delete_level, write_level)
VALUES ('media-type', 1, 1, 1, 1, 1)
ON CONFLICT (resource) DO UPDATE SET
    read_level = 1,
    create_level = 1,
    update_level = 1,
    delete_level = 1,
    write_level = 1;

-- Customer (clientes)
INSERT INTO app_entity (resource, read_level, create_level, update_level, delete_level, write_level)
VALUES ('customer', 1, 1, 1, 1, 1)
ON CONFLICT (resource) DO UPDATE SET
    read_level = 1,
    create_level = 1,
    update_level = 1,
    delete_level = 1,
    write_level = 1;

-- Client (clientes OAuth)
INSERT INTO app_entity (resource, read_level, create_level, update_level, delete_level, write_level)
VALUES ('client', 1, 1, 1, 1, 1)
ON CONFLICT (resource) DO UPDATE SET
    read_level = 1,
    create_level = 1,
    update_level = 1,
    delete_level = 1,
    write_level = 1;

-- Fraud Detection (detección de fraude)
INSERT INTO app_entity (resource, read_level, create_level, update_level, delete_level, write_level)
VALUES ('fraud_detection', 1, 1, 1, 1, 1)
ON CONFLICT (resource) DO UPDATE SET
    read_level = 1,
    create_level = 1,
    update_level = 1,
    delete_level = 1,
    write_level = 1;

-- Verificar los recursos insertados
SELECT resource, read_level, create_level, update_level, delete_level, write_level
FROM app_entity
ORDER BY resource;
