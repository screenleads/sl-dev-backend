-- ======================================================================
-- MIGRACIÓN AL NUEVO MODELO DE DOMINIO - ScreenLeads Backend
-- ======================================================================
-- Versión: 1.0.0
-- Fecha: 13 de enero de 2026
-- Descripción: Migración de Customer/Client/PromotionLead al nuevo modelo
-- ======================================================================

-- PASO 1: BACKUP DE SEGURIDAD
-- ======================================================================
-- Ejecutar antes manualmente:
-- mysqldump -u root -p screenleads > backup_pre_migration_$(date +%Y%m%d_%H%M%S).sql
-- O en Windows PowerShell:
-- mysqldump -u root -p screenleads > backup_pre_migration_$((Get-Date).ToString('yyyyMMdd_HHmmss')).sql

-- PASO 2: CREAR TABLAS TEMPORALES PARA BACKUP
-- ======================================================================
CREATE TABLE IF NOT EXISTS _backup_customer AS SELECT * FROM customer;
CREATE TABLE IF NOT EXISTS _backup_client AS SELECT * FROM client WHERE 1=0; -- Si existe
CREATE TABLE IF NOT EXISTS _backup_promotion_lead AS SELECT * FROM promotion_lead;

-- Verificar backups
SELECT 
    (SELECT COUNT(*) FROM _backup_customer) as backup_customer_count,
    (SELECT COUNT(*) FROM _backup_promotion_lead) as backup_promotion_lead_count;

-- PASO 3: CREAR TABLA DE MAPEO TEMPORAL
-- ======================================================================
-- Esta tabla ayudará a mapear old_customer_id -> new_customer_id
CREATE TABLE IF NOT EXISTS _migration_customer_mapping (
    old_customer_id BIGINT,
    old_table_name VARCHAR(50), -- 'customer' o 'client'
    new_customer_id BIGINT,
    email VARCHAR(320),
    phone VARCHAR(50),
    migration_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (old_customer_id, old_table_name)
);

-- PASO 4: DESHABILITAR CONSTRAINTS TEMPORALMENTE
-- ======================================================================
SET FOREIGN_KEY_CHECKS = 0;

-- PASO 5: RENOMBRAR TABLA ANTIGUA CUSTOMER
-- ======================================================================
-- Renombrar la tabla antigua customer para evitar conflictos
ALTER TABLE customer RENAME TO _old_customer;
ALTER TABLE promotion_lead RENAME TO _old_promotion_lead;

-- Si existe tabla client, también renombrarla
-- ALTER TABLE client RENAME TO _old_client;

-- PASO 6: MIGRAR DATOS A NUEVA TABLA CUSTOMER
-- ======================================================================
-- La nueva tabla 'customer' se crea automáticamente por Hibernate/JPA

-- Migrar desde _old_customer
INSERT INTO customer (
    id,
    first_name,
    last_name,
    email,
    phone,
    birth_date,
    gender,
    city,
    postal_code,
    country,
    preferred_language,
    marketing_opt_in,
    marketing_opt_in_at,
    data_processing_consent,
    data_processing_consent_at,
    email_verified,
    phone_verified,
    total_redemptions,
    lifetime_value,
    first_interaction_at,
    created_at,
    updated_at,
    created_by,
    updated_by
)
SELECT
    oc.id,
    oc.first_name,
    oc.last_name,
    oc.email,
    oc.identifier as phone, -- Asumiendo que identifier era el phone
    oc.birth_date,
    NULL as gender, -- No existía en modelo antiguo
    NULL as city,
    NULL as postal_code,
    NULL as country,
    'es' as preferred_language,
    COALESCE(oc.marketing_opt_in, FALSE) as marketing_opt_in,
    oc.marketing_opt_in_at,
    TRUE as data_processing_consent, -- Asumir consentimiento para datos existentes
    oc.created_at as data_processing_consent_at,
    COALESCE(oc.email_verified, FALSE) as email_verified,
    FALSE as phone_verified,
    0 as total_redemptions, -- Se calculará después
    0.00 as lifetime_value,
    oc.created_at as first_interaction_at,
    oc.created_at,
    oc.updated_at,
    oc.created_by,
    oc.updated_by
FROM _old_customer oc;

-- Registrar mapeo
INSERT INTO _migration_customer_mapping (old_customer_id, old_table_name, new_customer_id, email, phone)
SELECT id, 'customer', id, email, identifier FROM _old_customer;

-- PASO 7: MIGRAR CLIENTES DE PROMOTION_LEAD SIN CUSTOMER
-- ======================================================================
-- Identificar leads que no tienen customer asociado y crear customers
INSERT INTO customer (
    first_name,
    last_name,
    email,
    phone,
    preferred_language,
    data_processing_consent,
    data_processing_consent_at,
    email_verified,
    total_redemptions,
    lifetime_value,
    first_interaction_at,
    created_at,
    updated_at
)
SELECT DISTINCT
    opl.first_name,
    opl.last_name,
    opl.email,
    opl.phone,
    'es' as preferred_language,
    TRUE as data_processing_consent,
    opl.created_at as data_processing_consent_at,
    FALSE as email_verified,
    0 as total_redemptions,
    0.00 as lifetime_value,
    opl.created_at as first_interaction_at,
    opl.created_at,
    NOW() as updated_at
FROM _old_promotion_lead opl
WHERE opl.customer_id IS NULL
    AND opl.email IS NOT NULL
    AND NOT EXISTS (
        SELECT 1 FROM customer c WHERE c.email = opl.email
    );

-- PASO 8: INSERTAR AUTH_METHODS
-- ======================================================================
-- Agregar método de autenticación EMAIL para todos los customers con email
INSERT INTO customer_auth_method (customer_id, auth_method)
SELECT id, 'EMAIL'
FROM customer
WHERE email IS NOT NULL
ON DUPLICATE KEY UPDATE auth_method = auth_method;

-- Agregar método de autenticación PHONE para customers con teléfono
INSERT INTO customer_auth_method (customer_id, auth_method)
SELECT id, 'PHONE'
FROM customer
WHERE phone IS NOT NULL
ON DUPLICATE KEY UPDATE auth_method = auth_method;

-- PASO 9: MIGRAR PROMOTION_LEAD A PROMOTION_REDEMPTION
-- ======================================================================
INSERT INTO promotion_redemption (
    promotion_id,
    customer_id,
    device_id,
    coupon_code,
    coupon_status,
    redemption_method,
    ip_address,
    user_agent,
    verified,
    lead_score,
    fraud_status,
    billing_status,
    created_at,
    updated_at,
    created_by,
    updated_by
)
SELECT
    opl.promotion_id,
    -- Buscar customer_id: primero por old customer_id, luego por email
    COALESCE(
        opl.customer_id,
        (SELECT id FROM customer WHERE email = opl.email LIMIT 1)
    ) as customer_id,
    -- Device: intentar inferir del advice_id si existe relación
    (SELECT device_id FROM advice WHERE id = opl.advice_id LIMIT 1) as device_id,
    COALESCE(opl.coupon_code, CONCAT('MIGRATED-', opl.id)) as coupon_code,
    CASE 
        WHEN opl.coupon_status = 'NEW' THEN 'NEW'
        WHEN opl.coupon_status = 'VALID' THEN 'VALID'
        WHEN opl.coupon_status = 'REDEEMED' THEN 'REDEEMED'
        WHEN opl.coupon_status = 'EXPIRED' THEN 'EXPIRED'
        WHEN opl.coupon_status = 'CANCELLED' THEN 'CANCELLED'
        ELSE 'VALID'
    END as coupon_status,
    'EMAIL' as redemption_method, -- Asumir EMAIL por defecto
    NULL as ip_address, -- No disponible en modelo antiguo
    NULL as user_agent,
    FALSE as verified, -- Marcar como no verificado, se puede actualizar después
    75 as lead_score, -- Puntaje por defecto medio-alto para leads existentes
    'CLEAN' as fraud_status, -- Asumir limpio para datos históricos
    'BILLED' as billing_status, -- Asumir ya facturado
    opl.created_at,
    opl.updated_at,
    opl.created_by,
    opl.updated_by
FROM _old_promotion_lead opl
WHERE COALESCE(
        opl.customer_id,
        (SELECT id FROM customer WHERE email = opl.email LIMIT 1)
    ) IS NOT NULL; -- Solo migrar si encontramos un customer

-- PASO 10: ACTUALIZAR MÉTRICAS DE CUSTOMER
-- ======================================================================
-- Actualizar total_redemptions y lifetime_value
UPDATE customer c
SET 
    total_redemptions = (
        SELECT COUNT(*) 
        FROM promotion_redemption pr 
        WHERE pr.customer_id = c.id
    ),
    unique_promotions_redeemed = (
        SELECT COUNT(DISTINCT promotion_id)
        FROM promotion_redemption pr
        WHERE pr.customer_id = c.id
    ),
    last_interaction_at = (
        SELECT MAX(created_at)
        FROM promotion_redemption pr
        WHERE pr.customer_id = c.id
    );

-- PASO 11: ACTUALIZAR MÉTRICAS DE PROMOTION
-- ======================================================================
UPDATE promotion p
SET 
    redemption_count = (
        SELECT COUNT(*)
        FROM promotion_redemption pr
        WHERE pr.promotion_id = p.id
    ),
    unique_users_count = (
        SELECT COUNT(DISTINCT customer_id)
        FROM promotion_redemption pr
        WHERE pr.promotion_id = p.id
    );

-- PASO 12: CREAR USERACTION PARA CANJES HISTÓRICOS
-- ======================================================================
-- Crear un registro de acción por cada canje migrado
INSERT INTO user_action (
    customer_id,
    device_id,
    timestamp,
    action_type,
    entity_type,
    entity_id,
    session_id
)
SELECT
    pr.customer_id,
    pr.device_id,
    pr.created_at as timestamp,
    'REDEEM_PROMOTION' as action_type,
    'Promotion' as entity_type,
    pr.promotion_id as entity_id,
    pr.coupon_code as session_id -- Usar coupon como identificador de sesión
FROM promotion_redemption pr
WHERE pr.created_at >= DATE_SUB(NOW(), INTERVAL 6 MONTH); -- Solo últimos 6 meses

-- PASO 13: HABILITAR CONSTRAINTS
-- ======================================================================
SET FOREIGN_KEY_CHECKS = 1;

-- PASO 14: VERIFICAR INTEGRIDAD
-- ======================================================================
SELECT 'VERIFICACIÓN DE MIGRACIÓN' as step;

SELECT 
    'Customers' as entity,
    (SELECT COUNT(*) FROM _old_customer) as old_count,
    (SELECT COUNT(*) FROM customer) as new_count,
    (SELECT COUNT(*) FROM customer) - (SELECT COUNT(*) FROM _old_customer) as difference;

SELECT 
    'PromotionLeads/Redemptions' as entity,
    (SELECT COUNT(*) FROM _old_promotion_lead) as old_count,
    (SELECT COUNT(*) FROM promotion_redemption) as new_count,
    (SELECT COUNT(*) FROM promotion_redemption) - (SELECT COUNT(*) FROM _old_promotion_lead) as difference;

-- Verificar que no hay redemptions sin customer
SELECT 
    'Redemptions sin Customer' as issue,
    COUNT(*) as count
FROM promotion_redemption pr
WHERE pr.customer_id IS NULL;

-- Verificar que no hay redemptions sin promotion
SELECT 
    'Redemptions sin Promotion' as issue,
    COUNT(*) as count
FROM promotion_redemption pr
WHERE pr.promotion_id IS NULL;

-- Verificar emails únicos en customer
SELECT 
    'Emails duplicados en Customer' as issue,
    COUNT(*) as count
FROM (
    SELECT email, COUNT(*) as cnt
    FROM customer
    WHERE email IS NOT NULL
    GROUP BY email
    HAVING cnt > 1
) dup;

-- PASO 15: REPORT DE MIGRACIÓN
-- ======================================================================
SELECT '=== REPORTE DE MIGRACIÓN ===' as report;

SELECT 
    'Total Customers migrados' as metric,
    COUNT(*) as value
FROM customer;

SELECT 
    'Customers con email' as metric,
    COUNT(*) as value
FROM customer
WHERE email IS NOT NULL;

SELECT 
    'Customers con teléfono' as metric,
    COUNT(*) as value
FROM customer
WHERE phone IS NOT NULL;

SELECT 
    'Total Redemptions migrados' as metric,
    COUNT(*) as value
FROM promotion_redemption;

SELECT 
    'Redemptions con device' as metric,
    COUNT(*) as value
FROM promotion_redemption
WHERE device_id IS NOT NULL;

SELECT 
    'UserActions creados' as metric,
    COUNT(*) as value
FROM user_action;

-- PASO 16: NOTAS IMPORTANTES
-- ======================================================================
/*
NOTA IMPORTANTE: 
- Las tablas _old_customer, _old_promotion_lead, _backup_customer, _backup_promotion_lead
  se mantienen como backup. NO las elimines hasta verificar que todo funciona correctamente.

- Para eliminar las tablas de backup después de verificación (EJECUTAR CON PRECAUCIÓN):
  DROP TABLE IF EXISTS _old_customer;
  DROP TABLE IF EXISTS _old_promotion_lead;
  DROP TABLE IF EXISTS _old_client;
  DROP TABLE IF EXISTS _backup_customer;
  DROP TABLE IF EXISTS _backup_promotion_lead;
  DROP TABLE IF EXISTS _migration_customer_mapping;

- Si necesitas ROLLBACK completo:
  1. Detener la aplicación
  2. Restaurar desde backup: mysql -u root -p screenleads < backup_pre_migration_*.sql
  3. Reiniciar la aplicación

VERIFICACIONES POST-MIGRACIÓN:
1. ✓ Verificar que la aplicación arranca sin errores
2. ✓ Probar login de usuarios del dashboard
3. ✓ Visualizar promociones en devices
4. ✓ Intentar un nuevo canje de promoción
5. ✓ Verificar que se crean correctamente customers nuevos
6. ✓ Revisar logs de la aplicación
7. ✓ Verificar métricas en el dashboard
8. ✓ Probar exportación de datos

SIGUIENTE PASO:
- Actualizar repositorios, servicios y controladores para usar las nuevas entidades
*/

-- FIN DE LA MIGRACIÓN
SELECT 'MIGRACIÓN COMPLETADA EXITOSAMENTE' as status, NOW() as completed_at;
