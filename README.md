# ScreenLeads Backend

Backend API para la plataforma ScreenLeads - Sistema de gestión de pantallas digitales y engagement.

## 🚀 Características

- API REST con Spring Boot 3.5
- Autenticación JWT y API Keys
- Integración con Stripe para pagos
- Firebase Storage para archivos multimedia
- PostgreSQL como base de datos
- WebSocket para comunicación en tiempo real
- Documentación OpenAPI/Swagger
- Actuator para monitoring

## 📋 Requisitos

- Java 17+
- Maven 3.6+
- PostgreSQL 12+
- Firebase Account (para storage)
- Stripe Account (para pagos)

## 🔐 Seguridad - Fase 1 ✅

Este proyecto ha implementado mejoras críticas de seguridad:

- ✅ **Sin credenciales hardcoded** - Todas en variables de entorno
- ✅ **HashiCorp Vault** - Gestión de secretos (opcional)
- ✅ **Dependencias actualizadas** - Sin vulnerabilidades conocidas
- ✅ **Endpoints protegidos** - Actuator y Swagger seguros por ambiente
- ✅ **Configuración por ambiente** - dev, pre, pro

Ver documentación completa en: [`SECURITY_PHASE1_SUMMARY.md`](./SECURITY_PHASE1_SUMMARY.md)

## ⚙️ Configuración

### 1. Variables de Entorno

```bash
# Copiar template de configuración
cp .env.example .env

# Editar con tus valores
nano .env

# Cargar variables de entorno (PowerShell)
.\setup-environment.ps1 -LoadEnvFile

# O manualmente (PowerShell)
Get-Content .env | ForEach-Object {
    if ($_ -match '^([^=]+)=(.*)$') {
        [System.Environment]::SetEnvironmentVariable($matches[1], $matches[2], 'Process')
    }
}
```

### 2. Variables Requeridas

#### Base de Datos
```properties
JDBC_DATABASE_URL=jdbc:postgresql://localhost:5432/sl_db
JDBC_DATABASE_USERNAME=postgres
JDBC_DATABASE_PASSWORD=your_secure_password
```

#### Stripe
```properties
STRIPE_SECRET_KEY=sk_test_your_key
STRIPE_PRICE_ID=price_your_id
STRIPE_WEBHOOK_SECRET=whsec_your_secret
```

#### Firebase
```properties
GOOGLE_CREDENTIALS_BASE64=base64_encoded_service_account_json
FIREBASE_STORAGE_BUCKET=your-project.firebasestorage.app
```

#### JWT
```properties
JWT_SECRET_KEY=base64_encoded_secret_key
JWT_EXPIRATION=86400000
```

### 3. Generar Secretos

```powershell
# JWT Secret (PowerShell)
[Convert]::ToBase64String((1..64 | ForEach-Object { Get-Random -Maximum 256 }))

# Firebase Credentials (PowerShell)
[Convert]::ToBase64String([IO.File]::ReadAllBytes("path/to/serviceAccountKey.json"))
```

## 🏃 Ejecución

### Desarrollo Local

```bash
# Con Maven
mvn spring-boot:run

# Con perfil específico
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Con Java
mvn clean package
java -jar target/app-0.0.1-SNAPSHOT.jar
```

### Docker

```bash
# Build
docker build -t screenleads/backend:latest .

# Run
docker run -d \
  -e JDBC_DATABASE_URL=jdbc:postgresql://host.docker.internal:5432/sl_db \
  -e JDBC_DATABASE_PASSWORD=your_password \
  -e STRIPE_SECRET_KEY=sk_test_... \
  -e JWT_SECRET_KEY=your_jwt_secret \
  -p 3000:3000 \
  screenleads/backend:latest
```

### Heroku

```bash
# Configurar variables
heroku config:set JDBC_DATABASE_URL=jdbc:postgresql://...
heroku config:set JDBC_DATABASE_PASSWORD=...
heroku config:set STRIPE_SECRET_KEY=...
heroku config:set JWT_SECRET_KEY=...
heroku config:set SPRING_PROFILES_ACTIVE=pro

# Deploy
git push heroku main
```

## 📚 API Documentation

### Swagger UI (Solo dev/pre)

```
http://localhost:3000/swagger-ui
```

**Nota:** Swagger está deshabilitado en producción por seguridad.

### Actuator Endpoints

```bash
# Health (público)
curl http://localhost:3000/actuator/health

# Info (público)
curl http://localhost:3000/actuator/info

# Metrics (requiere autenticación)
curl http://localhost:3000/actuator/metrics
```

## 🧪 Testing

```bash
# Ejecutar tests
mvn test

# Tests con coverage
mvn clean test jacoco:report

# Análisis de vulnerabilidades
mvn org.owasp:dependency-check-maven:check
```

## 📁 Estructura del Proyecto

```
sl-dev-backend/
├── src/main/java/com/screenleads/backend/app/
│   ├── application/          # Casos de uso y servicios
│   │   ├── service/         # Servicios de negocio
│   │   ├── security/        # Configuración de seguridad
│   │   └── web/             # Controladores REST
│   ├── domain/              # Entidades y lógica de dominio
│   │   ├── model/           # Entidades JPA
│   │   └── repository/      # Repositorios
│   └── infraestructure/     # Infraestructura y configuración
│       ├── config/          # Configuraciones Spring
│       └── vault/           # Integración HashiCorp Vault
├── src/main/resources/
│   ├── application.properties           # Configuración base
│   ├── application-dev.properties       # Desarrollo
│   ├── application-pre.properties       # Pre-producción
│   └── application-pro.properties       # Producción
├── .env.example             # Template de variables de entorno
├── setup-environment.ps1    # Script de configuración
└── SECURITY_PHASE1_SUMMARY.md   # Documentación de seguridad
```

## 🌍 Ambientes

### Development (dev)
- Swagger: ✅ Habilitado
- Actuator: ✅ Todos los endpoints
- Logging: DEBUG
- Database: Local PostgreSQL

### Pre-Production (pre)
- Swagger: ✅ Habilitado con auth
- Actuator: ⚠️ Limitado (health, info, metrics)
- Logging: INFO
- Database: Heroku Postgres

### Production (pro)
- Swagger: ❌ Deshabilitado
- Actuator: 🔒 Mínimo (health, info)
- Logging: WARN
- Database: Heroku Postgres
- Error details: Ocultos

## 🔒 HashiCorp Vault (Opcional)

### Configuración

```properties
vault.enabled=true
vault.address=http://localhost:8200
vault.token=your_vault_token
vault.secret-path=secret/screenleads
```

### Uso

```java
@Autowired
private VaultSecretService vaultService;

Optional<String> secret = vaultService.getSecret("database.password");
```

## 🐛 Troubleshooting

### Error: Missing environment variable

```bash
# Verificar variables cargadas
Get-ChildItem Env: | Where-Object { $_.Name -like '*JDBC*' -or $_.Name -like '*STRIPE*' }

# Recargar variables
.\setup-environment.ps1 -LoadEnvFile
```

### Error: Firebase initialization failed

```bash
# Verificar que el Base64 sea válido
$base64 = $env:GOOGLE_CREDENTIALS_BASE64
[System.Text.Encoding]::UTF8.GetString([System.Convert]::FromBase64String($base64))
```

### Error: JWT signature invalid

```bash
# Generar nuevo secret
$bytes = 1..64 | ForEach-Object { Get-Random -Maximum 256 }
$secret = [Convert]::ToBase64String($bytes)
Write-Host $secret
```

## 📊 Monitoring

### Logs

```bash
# Ver logs en tiempo real (Heroku)
heroku logs --tail

# Ver logs locales
mvn spring-boot:run | grep "ERROR\|WARN"
```

### Health Checks

```bash
# Health endpoint
curl http://localhost:3000/actuator/health

# Respuesta esperada
{
  "status": "UP",
  "components": {
    "db": { "status": "UP" },
    "diskSpace": { "status": "UP" }
  }
}
```

## 🤝 Contribución

1. Crear branch desde `develop`
2. Realizar cambios
3. Ejecutar tests: `mvn test`
4. Crear Pull Request
5. Code review
6. Merge a `develop`

## 📄 Licencia

Propietario - ScreenLeads © 2025

## 🔗 Enlaces Útiles

- [Documentación de Seguridad](./SECURITY_PHASE1_SUMMARY.md)
- [Documentación de API](./docs/)
- [Postman Collections](./postman/)
- [JIRA Project](https://screenleads.atlassian.net)

## 📞 Soporte

- **Email:** devops@screenleads.com
- **Slack:** #backend-support
- **Documentation:** https://docs.screenleads.com

