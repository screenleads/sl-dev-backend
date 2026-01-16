-- =====================================================================
-- API KEYS - Configuración para Nuevos Controladores
-- =====================================================================
-- Fecha: 13 de enero de 2026
-- Contexto: Rediseño del modelo de dominio 2026
-- Controladores: PromotionRedemptionController, CompanyBillingController
-- =====================================================================

-- =====================================================================
-- 1. API KEY DE SOLO LECTURA - REDEMPTIONS (GLOBAL)
-- =====================================================================
-- Uso: Dashboards de reporting, analytics
-- Acceso: Todas las compañías
-- Permisos: Solo consulta de canjes
-- =====================================================================

INSERT INTO api_key (
    `key`,
    client,
    active,
    permissions,
    company_scope,
    description,
    name,
    created_at,
    expires_at
) VALUES (
    'sk_redemption_readonly_abc123xyz789',
    1,  -- Reemplazar con ID de tu cliente
    true,
    'redemption:read',
    NULL,  -- Acceso global a todas las compañías
    'Read-only redemption access for global reporting dashboard',
    'Global Redemption Reader',
    NOW(),
    DATE_ADD(NOW(), INTERVAL 365 DAY)  -- Expira en 1 año
);

-- =====================================================================
-- 2. API KEY DE ESCRITURA - REDEMPTIONS (COMPAÑÍA ESPECÍFICA)
-- =====================================================================
-- Uso: Punto de venta (POS), kiosko, integración externa
-- Acceso: Solo compañía 42
-- Permisos: Crear y actualizar canjes
-- =====================================================================

INSERT INTO api_key (
    `key`,
    client,
    active,
    permissions,
    company_scope,
    description,
    name,
    created_at,
    expires_at
) VALUES (
    'sk_redemption_write_company42_def456',
    1,  -- Reemplazar con ID de tu cliente
    true,
    'redemption:read,redemption:write',
    42,  -- Solo compañía ID 42
    'POS Integration for Company 42 - Create and verify redemptions',
    'Company 42 POS Integration',
    NOW(),
    DATE_ADD(NOW(), INTERVAL 365 DAY)
);

-- =====================================================================
-- 3. API KEY DE VALIDACIÓN DE CUPONES (COMPAÑÍA ESPECÍFICA)
-- =====================================================================
-- Uso: Sistema externo que valida cupones antes de aplicar descuentos
-- Acceso: Solo compañía 15
-- Permisos: Solo lectura de canjes
-- =====================================================================

INSERT INTO api_key (
    `key`,
    client,
    active,
    permissions,
    company_scope,
    description,
    name,
    created_at,
    expires_at
) VALUES (
    'sk_coupon_validator_company15_ghi789',
    2,  -- Reemplazar con ID de tu cliente
    true,
    'redemption:read',
    15,  -- Solo compañía ID 15
    'External coupon validation system for Company 15 stores',
    'Company 15 Coupon Validator',
    NOW(),
    DATE_ADD(NOW(), INTERVAL 180 DAY)  -- Expira en 6 meses
);

-- =====================================================================
-- 4. API KEY FULL ACCESS - REDEMPTIONS (COMPAÑÍA ESPECÍFICA)
-- =====================================================================
-- Uso: Integración completa con sistema ERP/CRM
-- Acceso: Solo compañía 20
-- Permisos: CRUD completo de canjes
-- =====================================================================

INSERT INTO api_key (
    `key`,
    client,
    active,
    permissions,
    company_scope,
    description,
    name,
    created_at,
    expires_at
) VALUES (
    'sk_redemption_full_company20_jkl012',
    3,  -- Reemplazar con ID de tu cliente
    true,
    'redemption:*',  -- Todos los permisos sobre redemptions
    20,  -- Solo compañía ID 20
    'Full redemption management for Company 20 ERP integration',
    'Company 20 ERP Full Access',
    NOW(),
    DATE_ADD(NOW(), INTERVAL 365 DAY)
);

-- =====================================================================
-- 5. API KEY DE SOLO LECTURA - BILLING (COMPAÑÍA ESPECÍFICA)
-- =====================================================================
-- Uso: Dashboard de facturación de cliente, validación de límites
-- Acceso: Solo compañía 10
-- Permisos: Consultar configuración de facturación
-- =====================================================================

INSERT INTO api_key (
    `key`,
    client,
    active,
    permissions,
    company_scope,
    description,
    name,
    created_at,
    expires_at
) VALUES (
    'sk_billing_readonly_company10_mno345',
    4,  -- Reemplazar con ID de tu cliente
    true,
    'billing:read',
    10,  -- Solo compañía ID 10
    'Billing dashboard read access for Company 10',
    'Company 10 Billing Dashboard',
    NOW(),
    DATE_ADD(NOW(), INTERVAL 365 DAY)
);

-- =====================================================================
-- 6. API KEY DE REMARKETING (COMPAÑÍA ESPECÍFICA)
-- =====================================================================
-- Uso: Sistema de remarketing/CRM para exportar datos de clientes
-- Acceso: Solo compañía 25
-- Permisos: Leer clientes/canjes, crear exportaciones
-- =====================================================================

INSERT INTO api_key (
    `key`,
    client,
    active,
    permissions,
    company_scope,
    description,
    name,
    created_at,
    expires_at
) VALUES (
    'sk_remarketing_company25_pqr678',
    5,  -- Reemplazar con ID de tu cliente
    true,
    'customer:read,redemption:read,dataexport:write',
    25,  -- Solo compañía ID 25
    'Remarketing system for Company 25 - Customer data export',
    'Company 25 Remarketing',
    NOW(),
    DATE_ADD(NOW(), INTERVAL 365 DAY)
);

-- =====================================================================
-- 7. API KEY DE WEBHOOK STRIPE (GLOBAL)
-- =====================================================================
-- Uso: Recibir eventos de Stripe y actualizar facturación
-- Acceso: Todas las compañías (eventos de Stripe son globales)
-- Permisos: Crear eventos de facturación, actualizar facturas
-- =====================================================================

INSERT INTO api_key (
    `key`,
    client,
    active,
    permissions,
    company_scope,
    description,
    name,
    created_at,
    expires_at
) VALUES (
    'sk_stripe_webhook_global_stu901',
    6,  -- Reemplazar con ID de tu cliente
    true,
    'billingevent:write,invoice:read,invoice:write',
    NULL,  -- Acceso global (procesa eventos de todas las compañías)
    'Stripe webhook handler - Payment events and invoice updates',
    'Stripe Webhook Handler',
    NOW(),
    DATE_ADD(NOW(), INTERVAL 730 DAY)  -- Expira en 2 años
);

-- =====================================================================
-- 8. API KEY DE APP MÓVIL CLIENTE (COMPAÑÍA ESPECÍFICA)
-- =====================================================================
-- Uso: App móvil para clientes finales
-- Acceso: Solo compañía 30
-- Permisos: Ver canjes propios, solicitar exportaciones GDPR
-- =====================================================================

INSERT INTO api_key (
    `key`,
    client,
    active,
    permissions,
    company_scope,
    description,
    name,
    created_at,
    expires_at
) VALUES (
    'sk_mobile_customer_company30_vwx234',
    7,  -- Reemplazar con ID de tu cliente
    true,
    'redemption:read,customer:read,useraction:write,dataexport:write',
    30,  -- Solo compañía ID 30
    'Mobile customer app for Company 30 - Self-service access',
    'Company 30 Mobile App',
    NOW(),
    DATE_ADD(NOW(), INTERVAL 365 DAY)
);

-- =====================================================================
-- 9. API KEY DE SUPER ADMIN (GLOBAL - DESARROLLO/TESTING)
-- =====================================================================
-- Uso: Testing, desarrollo, administración completa
-- Acceso: Todas las compañías
-- Permisos: TODO
-- ⚠️ ADVERTENCIA: Solo para uso interno, nunca exponer
-- =====================================================================

INSERT INTO api_key (
    `key`,
    client,
    active,
    permissions,
    company_scope,
    description,
    name,
    created_at,
    expires_at
) VALUES (
    'sk_super_admin_dev_yza567',
    8,  -- Reemplazar con ID de tu cliente
    true,
    '*:*',  -- TODOS los permisos sobre TODOS los recursos
    NULL,  -- Acceso global
    '⚠️ SUPER ADMIN - Development and testing only - DO NOT USE IN PRODUCTION',
    'Super Admin Dev Key',
    NOW(),
    DATE_ADD(NOW(), INTERVAL 30 DAY)  -- Expira en 30 días
);

-- =====================================================================
-- 10. API KEY DE ANALYTICS (GLOBAL - SOLO LECTURA)
-- =====================================================================
-- Uso: Herramientas de BI, dashboards de analytics
-- Acceso: Todas las compañías
-- Permisos: Solo lectura de todos los recursos
-- =====================================================================

INSERT INTO api_key (
    `key`,
    client,
    active,
    permissions,
    company_scope,
    description,
    name,
    created_at,
    expires_at
) VALUES (
    'sk_analytics_global_readonly_bcd890',
    9,  -- Reemplazar con ID de tu cliente
    true,
    '*:read',  -- Solo lectura de todos los recursos
    NULL,  -- Acceso global
    'Global analytics dashboard - Read-only access to all resources',
    'Global Analytics Dashboard',
    NOW(),
    DATE_ADD(NOW(), INTERVAL 365 DAY)
);

-- =====================================================================
-- VERIFICACIÓN: Consultar las API Keys creadas
-- =====================================================================

SELECT 
    id,
    `key`,
    name,
    permissions,
    company_scope,
    CASE 
        WHEN company_scope IS NULL THEN '🌍 GLOBAL'
        ELSE CONCAT('🏢 Company ', company_scope)
    END as scope_display,
    active,
    created_at,
    expires_at,
    CASE 
        WHEN expires_at < NOW() THEN '⚠️ EXPIRED'
        WHEN expires_at < DATE_ADD(NOW(), INTERVAL 30 DAY) THEN '⚠️ EXPIRES SOON'
        ELSE '✅ ACTIVE'
    END as status_display
FROM api_key
ORDER BY created_at DESC
LIMIT 10;

-- =====================================================================
-- LIMPIEZA: Revocar API Keys comprometidas o antiguas
-- =====================================================================

-- Desactivar una API Key específica
-- UPDATE api_key SET active = false WHERE `key` = 'sk_old_key_to_revoke';

-- Desactivar API Keys expiradas
-- UPDATE api_key SET active = false WHERE expires_at < NOW() AND active = true;

-- Eliminar API Keys antiguas (más de 1 año inactivas)
-- DELETE FROM api_key WHERE active = false AND updated_at < DATE_SUB(NOW(), INTERVAL 365 DAY);

-- =====================================================================
-- NOTAS DE SEGURIDAD
-- =====================================================================
-- 
-- 1. COMPANY SCOPE
--    - NULL = Acceso global a todas las compañías
--    - ID = Solo acceso a esa compañía (RECOMENDADO)
-- 
-- 2. PERMISOS
--    - Format: 'resource:action'
--    - Examples: 'redemption:read', 'billing:write', 'customer:*'
--    - Wildcard: '*:*' = super admin (usar con precaución)
-- 
-- 3. EXPIRACIÓN
--    - Siempre establecer expires_at
--    - Keys de producción: 365 días
--    - Keys de desarrollo: 30-90 días
--    - Keys de testing: 7-30 días
-- 
-- 4. ROTACIÓN
--    - Rotar keys cada 6-12 meses
--    - Crear nueva key antes de revocar la antigua
--    - Coordinar con equipos de integración
-- 
-- 5. MONITOREO
--    - Registrar todos los usos de API Keys
--    - Alertar sobre uso anómalo
--    - Revisar permisos regularmente
-- 
-- =====================================================================
