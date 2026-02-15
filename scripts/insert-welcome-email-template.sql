-- ============================================================================
-- Script SQL: Insertar Plantilla de Email de Bienvenida
-- ============================================================================
-- Este script inserta una plantilla de email de bienvenida para cada empresa
-- en la base de datos. La plantilla se puede personalizar desde el dashboard.
-- ============================================================================

-- Opción 1: Insertar plantilla para una empresa específica (reemplazar <COMPANY_ID>)
-- Descomenta y reemplaza <COMPANY_ID> con el ID de tu empresa:
/*
INSERT INTO notification_templates (
    company_id,
    name,
    description,
    channel,
    subject,
    body,
    html_body,
    available_variables,
    is_active,
    sender,
    reply_to,
    usage_count,
    created_at,
    updated_at
) VALUES (
    <COMPANY_ID>, -- Reemplazar con el ID de la empresa
    'WELCOME_EMAIL',
    'Email de bienvenida enviado automáticamente a nuevos usuarios',
    'EMAIL',
    '¡Bienvenido a {{companyName}} en ScreenLeads!',
    'Hola {{userName}}, bienvenido a {{companyName}}. Tu cuenta ha sido creada exitosamente.',
    '<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
</head>
<body style="margin: 0; padding: 0; font-family: ''Segoe UI'', Tahoma, Geneva, Verdana, sans-serif; background-color: #f4f4f4;">
    <table width="100%" cellpadding="0" cellspacing="0" style="background-color: #f4f4f4; padding: 20px;">
        <tr>
            <td align="center">
                <table width="600" cellpadding="0" cellspacing="0" style="background-color: #ffffff; border-radius: 10px; overflow: hidden; box-shadow: 0 4px 6px rgba(0,0,0,0.1);">
                    <tr>
                        <td style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); padding: 50px 20px; text-align: center;">
                            <h1 style="color: #ffffff; margin: 0; font-size: 32px;">🎉 ¡Bienvenido!</h1>
                            <p style="color: #ffffff; margin: 15px 0 0 0; font-size: 18px; opacity: 0.95;">Tu cuenta ha sido creada exitosamente</p>
                        </td>
                    </tr>
                    <tr>
                        <td style="padding: 40px 30px;">
                            <h2 style="color: #333333; margin: 0 0 20px 0; font-size: 24px;">Hola, {{userName}} 👋</h2>
                            <p style="color: #666666; line-height: 1.6; margin: 0 0 20px 0; font-size: 16px;">
                                Te damos la bienvenida a <strong style="color: #667eea;">{{companyName}}</strong> en ScreenLeads.
                                Tu cuenta ha sido configurada y ya puedes empezar a usar nuestra plataforma.
                            </p>
                            <div style="background: linear-gradient(135deg, #f8f9ff 0%, #f0f4ff 100%); border-left: 4px solid #667eea; padding: 20px; border-radius: 8px; margin: 25px 0;">
                                <h3 style="color: #667eea; margin: 0 0 15px 0; font-size: 18px;">📋 Información de tu cuenta</h3>
                                <table width="100%" cellpadding="8" cellspacing="0">
                                    <tr>
                                        <td style="color: #666; font-size: 14px; width: 40%;"><strong>Usuario:</strong></td>
                                        <td style="color: #333; font-size: 14px;">{{userFullName}}</td>
                                    </tr>
                                    <tr>
                                        <td style="color: #666; font-size: 14px;"><strong>Email:</strong></td>
                                        <td style="color: #333; font-size: 14px;">{{userEmail}}</td>
                                    </tr>
                                    <tr>
                                        <td style="color: #666; font-size: 14px;"><strong>Empresa:</strong></td>
                                        <td style="color: #333; font-size: 14px;">{{companyName}}</td>
                                    </tr>
                                    <tr>
                                        <td style="color: #666; font-size: 14px;"><strong>Rol:</strong></td>
                                        <td style="color: #333; font-size: 14px;">{{roleName}}</td>
                                    </tr>
                                </table>
                            </div>
                            <p style="color: #666666; line-height: 1.6; margin: 25px 0 20px 0; font-size: 16px;">
                                Accede al dashboard para comenzar a gestionar tus campañas:
                            </p>
                            <table width="100%" cellpadding="0" cellspacing="0" style="margin: 30px 0;">
                                <tr>
                                    <td align="center">
                                        <a href="{{dashboardUrl}}" style="display: inline-block; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: #ffffff; text-decoration: none; padding: 16px 45px; border-radius: 8px; font-size: 16px; font-weight: bold; box-shadow: 0 4px 6px rgba(102, 126, 234, 0.4);">
                                            🚀 Ir al Dashboard
                                        </a>
                                    </td>
                                </tr>
                            </table>
                            <div style="background-color: #f8f9fa; padding: 25px; border-radius: 8px; margin: 30px 0;">
                                <h3 style="color: #333; margin: 0 0 15px 0; font-size: 18px;">✨ Qué puedes hacer en ScreenLeads:</h3>
                                <ul style="color: #666; line-height: 2; margin: 0; padding-left: 20px; font-size: 15px;">
                                    <li>Gestionar y rastrear campañas de marketing</li>
                                    <li>Analizar el comportamiento de tus visitantes</li>
                                    <li>Capturar y gestionar leads en tiempo real</li>
                                    <li>Crear notificaciones personalizadas</li>
                                    <li>Generar reportes y análisis detallados</li>
                                </ul>
                            </div>
                        </td>
                    </tr>
                    <tr>
                        <td style="background-color: #f8f9fa; padding: 30px; text-align: center; border-top: 1px solid #e9ecef;">
                            <p style="color: #6c757d; margin: 0 0 10px 0; font-size: 14px;">
                                <strong>ScreenLeads</strong> - Tu plataforma de gestión de leads
                            </p>
                            <p style="color: #6c757d; margin: 0; font-size: 12px; line-height: 1.5;">
                                © 2026 ScreenLeads. Todos los derechos reservados.
                            </p>
                        </td>
                    </tr>
                </table>
            </td>
        </tr>
    </table>
</body>
</html>',
    '["userName", "userFullName", "userEmail", "companyName", "roleName", "dashboardUrl"]'::jsonb,
    true,
    'no-reply@api.screenleads.com',
    'support@screenleads.com',
    0,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
*/

-- ============================================================================
-- Opción 2: Insertar plantilla para TODAS las empresas existentes
-- ============================================================================
-- Este script crea una plantilla de bienvenida para cada empresa en la BD
-- que aún no tenga una plantilla con el nombre 'WELCOME_EMAIL'

INSERT INTO notification_templates (
    company_id,
    name,
    description,
    channel,
    subject,
    body,
    html_body,
    available_variables,
    is_active,
    sender,
    reply_to,
    usage_count,
    created_at,
    updated_at
)
SELECT 
    c.id AS company_id,
    'WELCOME_EMAIL' AS name,
    'Email de bienvenida enviado automáticamente a nuevos usuarios' AS description,
    'EMAIL' AS channel,
    '¡Bienvenido a {{companyName}} en ScreenLeads!' AS subject,
    'Hola {{userName}}, bienvenido a {{companyName}}. Tu cuenta ha sido creada exitosamente.' AS body,
    '<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
</head>
<body style="margin: 0; padding: 0; font-family: ''Segoe UI'', Tahoma, Geneva, Verdana, sans-serif; background-color: #f4f4f4;">
    <table width="100%" cellpadding="0" cellspacing="0" style="background-color: #f4f4f4; padding: 20px;">
        <tr>
            <td align="center">
                <table width="600" cellpadding="0" cellspacing="0" style="background-color: #ffffff; border-radius: 10px; overflow: hidden; box-shadow: 0 4px 6px rgba(0,0,0,0.1);">
                    <tr>
                        <td style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); padding: 50px 20px; text-align: center;">
                            <h1 style="color: #ffffff; margin: 0; font-size: 32px;">🎉 ¡Bienvenido!</h1>
                            <p style="color: #ffffff; margin: 15px 0 0 0; font-size: 18px; opacity: 0.95;">Tu cuenta ha sido creada exitosamente</p>
                        </td>
                    </tr>
                    <tr>
                        <td style="padding: 40px 30px;">
                            <h2 style="color: #333333; margin: 0 0 20px 0; font-size: 24px;">Hola, {{userName}} 👋</h2>
                            <p style="color: #666666; line-height: 1.6; margin: 0 0 20px 0; font-size: 16px;">
                                Te damos la bienvenida a <strong style="color: #667eea;">{{companyName}}</strong> en ScreenLeads.
                                Tu cuenta ha sido configurada y ya puedes empezar a usar nuestra plataforma.
                            </p>
                            <div style="background: linear-gradient(135deg, #f8f9ff 0%, #f0f4ff 100%); border-left: 4px solid #667eea; padding: 20px; border-radius: 8px; margin: 25px 0;">
                                <h3 style="color: #667eea; margin: 0 0 15px 0; font-size: 18px;">📋 Información de tu cuenta</h3>
                                <table width="100%" cellpadding="8" cellspacing="0">
                                    <tr>
                                        <td style="color: #666; font-size: 14px; width: 40%;"><strong>Usuario:</strong></td>
                                        <td style="color: #333; font-size: 14px;">{{userFullName}}</td>
                                    </tr>
                                    <tr>
                                        <td style="color: #666; font-size: 14px;"><strong>Email:</strong></td>
                                        <td style="color: #333; font-size: 14px;">{{userEmail}}</td>
                                    </tr>
                                    <tr>
                                        <td style="color: #666; font-size: 14px;"><strong>Empresa:</strong></td>
                                        <td style="color: #333; font-size: 14px;">{{companyName}}</td>
                                    </tr>
                                    <tr>
                                        <td style="color: #666; font-size: 14px;"><strong>Rol:</strong></td>
                                        <td style="color: #333; font-size: 14px;">{{roleName}}</td>
                                    </tr>
                                </table>
                            </div>
                            <p style="color: #666666; line-height: 1.6; margin: 25px 0 20px 0; font-size: 16px;">
                                Accede al dashboard para comenzar a gestionar tus campañas:
                            </p>
                            <table width="100%" cellpadding="0" cellspacing="0" style="margin: 30px 0;">
                                <tr>
                                    <td align="center">
                                        <a href="{{dashboardUrl}}" style="display: inline-block; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: #ffffff; text-decoration: none; padding: 16px 45px; border-radius: 8px; font-size: 16px; font-weight: bold; box-shadow: 0 4px 6px rgba(102, 126, 234, 0.4);">
                                            🚀 Ir al Dashboard
                                        </a>
                                    </td>
                                </tr>
                            </table>
                            <div style="background-color: #f8f9fa; padding: 25px; border-radius: 8px; margin: 30px 0;">
                                <h3 style="color: #333; margin: 0 0 15px 0; font-size: 18px;">✨ Qué puedes hacer en ScreenLeads:</h3>
                                <ul style="color: #666; line-height: 2; margin: 0; padding-left: 20px; font-size: 15px;">
                                    <li>Gestionar y rastrear campañas de marketing</li>
                                    <li>Analizar el comportamiento de tus visitantes</li>
                                    <li>Capturar y gestionar leads en tiempo real</li>
                                    <li>Crear notificaciones personalizadas</li>
                                    <li>Generar reportes y análisis detallados</li>
                                </ul>
                            </div>
                        </td>
                    </tr>
                    <tr>
                        <td style="background-color: #f8f9fa; padding: 30px; text-align: center; border-top: 1px solid #e9ecef;">
                            <p style="color: #6c757d; margin: 0 0 10px 0; font-size: 14px;">
                                <strong>ScreenLeads</strong> - Tu plataforma de gestión de leads
                            </p>
                            <p style="color: #6c757d; margin: 0; font-size: 12px; line-height: 1.5;">
                                © 2026 ScreenLeads. Todos los derechos reservados.
                            </p>
                        </td>
                    </tr>
                </table>
            </td>
        </tr>
    </table>
</body>
</html>' AS html_body,
    '["userName", "userFullName", "userEmail", "companyName", "roleName", "dashboardUrl"]'::jsonb AS available_variables,
    true AS is_active,
    'no-reply@api.screenleads.com' AS sender,
    'support@screenleads.com' AS reply_to,
    0 AS usage_count,
    CURRENT_TIMESTAMP AS created_at,
    CURRENT_TIMESTAMP AS updated_at
FROM companies c
WHERE NOT EXISTS (
    SELECT 1 
    FROM notification_templates nt 
    WHERE nt.company_id = c.id 
    AND nt.name = 'WELCOME_EMAIL'
);

-- ============================================================================
-- Opción 3: Insertar plantilla de bienvenida para CLIENTES (consumidores finales)
-- ============================================================================
-- Este script crea una plantilla de bienvenida para clientes (no usuarios de la plataforma)
-- que se registran a través de promociones

INSERT INTO notification_templates (
    company_id,
    name,
    description,
    channel,
    subject,
    body,
    html_body,
    available_variables,
    is_active,
    sender,
    reply_to,
    usage_count,
    created_at,
    updated_at
)
SELECT 
    c.id AS company_id,
    'CUSTOMER_WELCOME_EMAIL' AS name,
    'Email de bienvenida para clientes que se registran desde promociones' AS description,
    'EMAIL' AS channel,
    '¡Bienvenido a ScreenLeads, {{customerName}}! 🎉' AS subject,
    'Hola {{customerName}}, ¡gracias por registrarte! Ya puedes disfrutar de nuestras promociones exclusivas.' AS body,
    '<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
</head>
<body style="margin: 0; padding: 0; font-family: ''Segoe UI'', Tahoma, Geneva, Verdana, sans-serif; background-color: #f4f4f4;">
    <table width="100%" cellpadding="0" cellspacing="0" style="background-color: #f4f4f4; padding: 20px;">
        <tr>
            <td align="center">
                <table width="600" cellpadding="0" cellspacing="0" style="background-color: #ffffff; border-radius: 10px; overflow: hidden; box-shadow: 0 4px 6px rgba(0,0,0,0.1);">
                    <tr>
                        <td style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); padding: 50px 20px; text-align: center;">
                            <h1 style="color: #ffffff; margin: 0; font-size: 32px;">🎉 ¡Bienvenido!</h1>
                            <p style="color: #ffffff; margin: 15px 0 0 0; font-size: 18px; opacity: 0.95;">Gracias por registrarte</p>
                        </td>
                    </tr>
                    <tr>
                        <td style="padding: 40px 30px;">
                            <h2 style="color: #333333; margin: 0 0 20px 0; font-size: 24px;">Hola, {{customerName}} 👋</h2>
                            <p style="color: #666666; line-height: 1.6; margin: 0 0 20px 0; font-size: 16px;">
                                ¡Nos alegra tenerte con nosotros! Tu registro ha sido completado exitosamente 
                                y ya puedes disfrutar de todas nuestras promociones y ofertas especiales.
                            </p>
                            <div style="background: linear-gradient(135deg, #f8f9ff 0%, #f0f4ff 100%); border-left: 4px solid #667eea; padding: 20px; border-radius: 8px; margin: 25px 0;">
                                <h3 style="color: #667eea; margin: 0 0 15px 0; font-size: 18px;">✨ Beneficios de ser cliente</h3>
                                <ul style="color: #666; line-height: 2; margin: 0; padding-left: 20px; font-size: 15px;">
                                    <li>Acceso a promociones exclusivas</li>
                                    <li>Descuentos especiales en comercios asociados</li>
                                    <li>Notificaciones de ofertas cercanas a tu ubicación</li>
                                    <li>Acumulación de puntos por tus canjes</li>
                                    <li>Recompensas por fidelidad</li>
                                </ul>
                            </div>
                            <p style="color: #666666; line-height: 1.6; margin: 25px 0 20px 0; font-size: 16px;">
                                Mantente atento a tus notificaciones para no perderte ninguna oferta.
                            </p>
                            <div style="background-color: #e8f5e9; border-left: 4px solid #4caf50; padding: 15px; border-radius: 8px; margin-top: 25px;">
                                <p style="margin: 0; color: #2e7d32; font-size: 14px; line-height: 1.6;">
                                    <strong>💡 ¿Necesitas ayuda?</strong><br>
                                    Si tienes alguna pregunta, no dudes en contactarnos. Estamos aquí para ayudarte.
                                </p>
                            </div>
                        </td>
                    </tr>
                    <tr>
                        <td style="background-color: #f8f9fa; padding: 30px; text-align: center; border-top: 1px solid #e9ecef;">
                            <p style="color: #6c757d; margin: 0 0 10px 0; font-size: 14px;">
                                <strong>ScreenLeads</strong> - Tu plataforma de promociones
                            </p>
                            <p style="color: #6c757d; margin: 0; font-size: 12px; line-height: 1.5;">
                                © 2026 ScreenLeads. Todos los derechos reservados.
                            </p>
                            <p style="color: #adb5bd; margin: 10px 0 0 0; font-size: 11px;">
                                Este es un correo automático, por favor no respondas a este mensaje.
                            </p>
                        </td>
                    </tr>
                </table>
            </td>
        </tr>
    </table>
</body>
</html>' AS html_body,
    '["customerName", "customerFullName", "customerEmail", "customerPhone"]'::jsonb AS available_variables,
    true AS is_active,
    'no-reply@api.screenleads.com' AS sender,
    'support@screenleads.com' AS reply_to,
    0 AS usage_count,
    CURRENT_TIMESTAMP AS created_at,
    CURRENT_TIMESTAMP AS updated_at
FROM companies c
WHERE NOT EXISTS (
    SELECT 1 
    FROM notification_templates nt 
    WHERE nt.company_id = c.id 
    AND nt.name = 'CUSTOMER_WELCOME_EMAIL'
);

-- ============================================================================
-- Verificar las plantillas insertadas
-- ============================================================================
-- Plantillas de usuarios internos (staff)
SELECT 
    nt.id,
    nt.name,
    c.name AS company_name,
    nt.channel,
    nt.is_active,
    nt.created_at
FROM notification_templates nt
JOIN companies c ON c.id = nt.company_id
WHERE nt.name = 'WELCOME_EMAIL'
ORDER BY nt.created_at DESC;

-- Plantillas de clientes (consumidores finales)
SELECT 
    nt.id,
    nt.name,
    c.name AS company_name,
    nt.channel,
    nt.is_active,
    nt.created_at
FROM notification_templates nt
JOIN companies c ON c.id = nt.company_id
WHERE nt.name = 'CUSTOMER_WELCOME_EMAIL'
ORDER BY nt.created_at DESC;

-- ============================================================================
-- NOTAS:
-- ============================================================================
-- 1. WELCOME_EMAIL: Para usuarios internos (staff que aceptan invitaciones)
--    Variables disponibles:
--    - {{userName}} : Nombre del usuario
--    - {{userFullName}} : Nombre completo (nombre + apellido)
--    - {{userEmail}} : Email del usuario
--    - {{companyName}} : Nombre de la empresa
--    - {{roleName}} : Rol asignado al usuario
--    - {{dashboardUrl}} : URL del dashboard
--
-- 2. CUSTOMER_WELCOME_EMAIL: Para clientes finales (registro desde promociones)
--    Variables disponibles:
--    - {{customerName}} : Nombre del cliente
--    - {{customerFullName}} : Nombre completo del cliente
--    - {{customerEmail}} : Email del cliente
--    - {{customerPhone}} : Teléfono del cliente
--
-- 3. Las empresas pueden personalizar estas plantillas desde el dashboard
--    en la sección Remarketing → Plantillas
--
-- 4. El sistema automáticamente busca las plantillas por nombre al enviar emails
--
-- 5. Si no encuentra plantilla personalizada, usa una plantilla predeterminada
-- ============================================================================
