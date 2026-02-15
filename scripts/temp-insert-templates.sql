-- Insertar WELCOME_EMAIL para todas las empresas que no lo tengan
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
    '<!DOCTYPE html><html lang="es"><body>Bienvenido {{userName}} a {{companyName}}</body></html>' AS html_body,
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

-- Insertar CUSTOMER_WELCOME_EMAIL para todas las empresas que no lo tengan
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
    '<!DOCTYPE html><html lang="es"><body>Bienvenido {{customerName}}</body></html>' AS html_body,
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

-- Verificar plantillas creadas
SELECT name, COUNT(*) as count FROM notification_templates WHERE name IN ('WELCOME_EMAIL', 'CUSTOMER_WELCOME_EMAIL') GROUP BY name;
