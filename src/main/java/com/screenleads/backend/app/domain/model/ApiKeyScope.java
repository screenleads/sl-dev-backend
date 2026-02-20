package com.screenleads.backend.app.domain.model;

/**
 * Scopes predefinidos para API Keys
 * Cada scope define un permiso granular sobre recursos específicos
 */
public enum ApiKeyScope {
    // Customer Management
    CUSTOMERS_READ("customers:read", "Leer información de clientes"),
    CUSTOMERS_WRITE("customers:write", "Crear y actualizar clientes"),
    CUSTOMERS_DELETE("customers:delete", "Eliminar clientes"),
    CUSTOMER_READ("customer:read", "Leer información de clientes (alias)"),
    CUSTOMER_UPDATE("customer:update", "Actualizar clientes (alias)"),
    CUSTOMER_DELETE("customer:delete", "Eliminar clientes (alias)"),

    // Campaign Management
    CAMPAIGNS_READ("campaigns:read", "Leer campañas y promociones"),
    CAMPAIGNS_WRITE("campaigns:write", "Crear y actualizar campañas"),
    CAMPAIGNS_DELETE("campaigns:delete", "Eliminar campañas"),

    // Lead Management
    LEADS_READ("leads:read", "Leer leads capturados"),
    LEADS_WRITE("leads:write", "Crear y actualizar leads"),
    LEADS_DELETE("leads:delete", "Eliminar leads"),

    // Analytics & Reports
    ANALYTICS_READ("analytics:read", "Acceder a estadísticas y reportes"),

    // Notification Templates
    TEMPLATES_READ("templates:read", "Leer plantillas de notificación"),
    TEMPLATES_WRITE("templates:write", "Crear y editar plantillas"),

    // Company Management
    COMPANY_READ("company:read", "Leer información de la compañía"),
    COMPANY_WRITE("company:write", "Actualizar configuración de la compañía"),

    // Webhook Management
    WEBHOOKS_MANAGE("webhooks:manage", "Gestionar webhooks"),

    // User Invitations
    INVITATIONS_READ("invitations:read", "Leer invitaciones de usuarios"),
    INVITATIONS_WRITE("invitations:write", "Crear invitaciones"),

    // Snapshot Management
    SNAPSHOTS_READ("snapshots:read", "Leer snapshots de tracking"),
    SNAPSHOTS_WRITE("snapshots:write", "Crear snapshots"),

    // Media Management
    MEDIA_READ("media:read", "Leer archivos multimedia"),
    MEDIA_WRITE("media:write", "Subir y editar archivos multimedia"),
    MEDIA_DELETE("media:delete", "Eliminar archivos multimedia"),

    // Remarketing & Audiences
    REMARKETING_READ("remarketing:read", "Leer segmentos de audiencia"),
    REMARKETING_CREATE("remarketing:create", "Crear segmentos de audiencia"),
    REMARKETING_UPDATE("remarketing:update", "Actualizar segmentos de audiencia"),
    REMARKETING_DELETE("remarketing:delete", "Eliminar segmentos de audiencia"),

    // Fraud Detection
    FRAUD_DETECTION_READ("fraud_detection:read", "Leer reglas de detección de fraude"),
    FRAUD_DETECTION_CREATE("fraud_detection:create", "Crear reglas de detección de fraude"),
    FRAUD_DETECTION_UPDATE("fraud_detection:update", "Actualizar reglas de detección de fraude"),
    FRAUD_DETECTION_DELETE("fraud_detection:delete", "Eliminar reglas de detección de fraude"),

    // Announcements/Advice
    ADVICE_READ("advice:read", "Leer anuncios y avisos"),
    ADVICE_WRITE("advice:write", "Crear y editar anuncios"),
    ADVICE_DELETE("advice:delete", "Eliminar anuncios");

    private final String value;
    private final String description;

    ApiKeyScope(String value, String description) {
        this.value = value;
        this.description = description;
    }

    public String getValue() {
        return value;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Obtiene el scope a partir de su valor string
     */
    public static ApiKeyScope fromValue(String value) {
        for (ApiKeyScope scope : values()) {
            if (scope.value.equals(value)) {
                return scope;
            }
        }
        throw new IllegalArgumentException("Unknown scope: " + value);
    }

    /**
     * Verifica si un scope string es válido
     */
    public static boolean isValid(String value) {
        try {
            fromValue(value);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
