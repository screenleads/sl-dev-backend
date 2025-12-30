# 🔐 Password Reset - Backend Implementation

## ✅ Implementación Completada

Se ha implementado exitosamente el sistema de recuperación de contraseña en el backend con los siguientes componentes:

### 📦 Archivos Creados

1. **Entidad JPA**
   - `PasswordResetToken.java` - Entidad para gestionar tokens de reset
   - Campos: `id`, `token`, `user`, `expiryDate`, `used`, `createdAt`
   - Métodos de validación: `isExpired()`, `isValid()`

2. **Repositorio**
   - `PasswordResetTokenRepository.java` - Repositorio Spring Data JPA
   - Métodos personalizados para buscar, marcar como usado y limpiar tokens expirados

3. **Servicio de Email**
   - `EmailService.java` - Servicio para envío de emails
   - Template HTML responsive para email de reset
   - Método genérico para envío de emails HTML

4. **DTOs**
   - `ForgotPasswordRequest.java` - Request para solicitar reset (email)
   - `ResetPasswordRequest.java` - Request para resetear password (token + newPassword)
   - `VerifyTokenResponse.java` - Response para verificar validez de token

### 🔄 Archivos Actualizados

1. **AuthenticationService.java**
   - ✅ `forgotPassword()` - Genera token y envía email
   - ✅ `verifyResetToken()` - Verifica si un token es válido
   - ✅ `resetPassword()` - Restablece la contraseña con token válido

2. **AuthController.java**
   - ✅ `POST /auth/forgot-password` - Solicitar recuperación
   - ✅ `GET /auth/verify-reset-token?token=...` - Verificar token
   - ✅ `POST /auth/reset-password` - Resetear contraseña

3. **UserRepository.java**
   - ✅ Añadido método `findByEmail(String email)`

4. **application.properties**
   - ✅ Configuración SMTP añadida
   - ✅ Variable `app.frontend.url` para enlaces en emails

---

## ⚙️ Configuración Requerida

### 1. Variables de Entorno

Añade las siguientes variables de entorno o configura en tu `.env`:

```properties
# Email Configuration
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=tu-email@gmail.com
MAIL_PASSWORD=tu-app-password

# Frontend URL para enlaces en emails
APP_FRONTEND_URL=http://localhost:4200
```

### 2. Configurar Gmail (Ejemplo)

Si usas Gmail, necesitas:

1. **Habilitar "2-Step Verification"** en tu cuenta de Google
2. **Generar una "App Password"**:
   - Ve a: https://myaccount.google.com/apppasswords
   - Selecciona "Mail" y "Other (Custom name)"
   - Genera la contraseña
   - Usa esa contraseña en `MAIL_PASSWORD`

### 3. Otros Proveedores SMTP

#### **SendGrid**
```properties
MAIL_HOST=smtp.sendgrid.net
MAIL_PORT=587
MAIL_USERNAME=apikey
MAIL_PASSWORD=tu-sendgrid-api-key
```

#### **Mailgun**
```properties
MAIL_HOST=smtp.mailgun.org
MAIL_PORT=587
MAIL_USERNAME=postmaster@tu-dominio.mailgun.org
MAIL_PASSWORD=tu-mailgun-password
```

#### **AWS SES**
```properties
MAIL_HOST=email-smtp.us-east-1.amazonaws.com
MAIL_PORT=587
MAIL_USERNAME=tu-aws-smtp-username
MAIL_PASSWORD=tu-aws-smtp-password
```

---

## 📊 Base de Datos

### Migración Automática

La tabla se creará automáticamente con Hibernate DDL Auto:

```sql
CREATE TABLE password_reset_tokens (
    id BIGSERIAL PRIMARY KEY,
    token VARCHAR(255) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    expiry_date TIMESTAMP NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL,
    FOREIGN KEY (user_id) REFERENCES app_user(id)
);

CREATE INDEX ix_password_reset_token ON password_reset_tokens(token);
CREATE INDEX ix_password_reset_user_id ON password_reset_tokens(user_id);
```

---

## 🔐 Características de Seguridad

### ✅ Implementadas

1. **Token UUID único** - Generado con `UUID.randomUUID()`
2. **Expiración de 1 hora** - Configurado en `TOKEN_EXPIRY_HOURS`
3. **Un solo uso** - Campo `used` marca el token como usado
4. **Invalidación de tokens anteriores** - Al generar uno nuevo, se marcan los viejos como usados
5. **No revela si el email existe** - Por seguridad, siempre responde OK
6. **Logs de auditoría** - Registra todas las acciones importantes
7. **Email normalizado** - Convierte a lowercase y trim antes de buscar

### 🔄 Recomendaciones Adicionales (Futuro)

1. **Rate Limiting** - Limitar solicitudes por IP/email
2. **CAPTCHA** - Añadir en formulario de forgot-password
3. **2FA opcional** - Para usuarios que lo requieran
4. **Notificación de cambio** - Email informando que la contraseña fue cambiada
5. **Limpieza automática** - Job scheduled para eliminar tokens expirados

---

## 🧪 Testing

### Endpoints a Probar

#### 1. Solicitar Reset
```bash
POST http://localhost:3000/auth/forgot-password
Content-Type: application/json

{
  "email": "usuario@example.com"
}

# Response: 200 OK (siempre, exista o no el email)
```

#### 2. Verificar Token
```bash
GET http://localhost:3000/auth/verify-reset-token?token=abc-123-def-456

# Response:
{
  "valid": true,
  "message": "Token válido",
  "userEmail": "usuario@example.com"
}
```

#### 3. Resetear Password
```bash
POST http://localhost:3000/auth/reset-password
Content-Type: application/json

{
  "token": "abc-123-def-456",
  "newPassword": "NuevaPassword123"
}

# Response: 200 OK
```

### Casos de Error

| Caso | Response | Mensaje |
|------|----------|---------|
| Token inválido | 200 OK | `valid: false, message: "Token inválido o no encontrado"` |
| Token expirado | 200 OK | `valid: false, message: "Este token ha expirado"` |
| Token ya usado | 200 OK | `valid: false, message: "Este token ya ha sido utilizado"` |
| Reset con token inválido | 400 Bad Request | `"Token inválido o no encontrado"` |
| Reset con token expirado | 400 Bad Request | `"El token ha expirado o ya ha sido utilizado"` |

---

## 📧 Template de Email

El email de recuperación incluye:

- ✅ Header con gradiente y logo
- ✅ Saludo personalizado con nombre de usuario
- ✅ Botón call-to-action destacado
- ✅ Link alternativo para copiar/pegar
- ✅ Advertencia de expiración (1 hora)
- ✅ Nota de seguridad (ignorar si no lo solicitaste)
- ✅ Footer con copyright
- ✅ Diseño responsive (funciona en móvil)

### Preview del Email

```
╔══════════════════════════════════════╗
║         ScreenLeads Dashboard         ║
║                                       ║
║  Recuperación de Contraseña          ║
║                                       ║
║  Hola Juan,                          ║
║                                       ║
║  Hemos recibido una solicitud...     ║
║                                       ║
║  [ Restablecer Contraseña ]          ║
║                                       ║
║  ⚠️ Este enlace expirará en 1 hora   ║
║                                       ║
║  © 2025 ScreenLeads                  ║
╚══════════════════════════════════════╝
```

---

## 🔄 Próximos Pasos

Para completar la funcionalidad, necesitarás:

1. ✅ **Backend COMPLETO** - Ya implementado
2. ⏳ **Frontend (Dashboard)**:
   - Componente `ForgotPasswordComponent`
   - Componente `ResetPasswordComponent`
   - Servicios en `authentication.service.ts`
   - Rutas en `app.routes.ts`
   - Link en login "¿Olvidaste tu contraseña?"

---

## 📝 Dependencias

Asegúrate de tener en tu `pom.xml`:

```xml
<!-- Email Support -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-mail</artifactId>
</dependency>
```

Si no está, añádela y ejecuta:
```bash
mvn clean install
```

---

## 🚀 Deployment

### Heroku Example

```bash
heroku config:set MAIL_HOST=smtp.gmail.com
heroku config:set MAIL_PORT=587
heroku config:set MAIL_USERNAME=tu-email@gmail.com
heroku config:set MAIL_PASSWORD=tu-app-password
heroku config:set APP_FRONTEND_URL=https://tu-dashboard.com
```

### Docker Example

```yaml
environment:
  - MAIL_HOST=smtp.gmail.com
  - MAIL_PORT=587
  - MAIL_USERNAME=${MAIL_USERNAME}
  - MAIL_PASSWORD=${MAIL_PASSWORD}
  - APP_FRONTEND_URL=https://dashboard.screenleads.com
```

---

## ✅ Checklist de Implementación

- [x] Crear entidad `PasswordResetToken`
- [x] Crear repositorio `PasswordResetTokenRepository`
- [x] Crear servicio `EmailService`
- [x] Crear DTOs (Request/Response)
- [x] Actualizar `AuthenticationService`
- [x] Actualizar `AuthController`
- [x] Actualizar `UserRepository`
- [x] Configurar SMTP en `application.properties`
- [x] Documentar configuración
- [ ] Configurar variables de entorno en producción
- [ ] Probar con email real
- [ ] Implementar frontend (siguiente fase)

---

## 🆘 Troubleshooting

### Email no se envía

1. **Verifica credenciales SMTP**
   ```bash
   # Check logs
   grep "Failed to send" logs/application.log
   ```

2. **Gmail bloqueando**
   - Usa App Password, no tu contraseña real
   - Verifica 2FA habilitado
   - Permite "Less secure apps" (no recomendado)

3. **Firewall/Puerto bloqueado**
   ```bash
   telnet smtp.gmail.com 587
   ```

### Token no funciona

1. **Verifica expiración**
   ```sql
   SELECT * FROM password_reset_tokens WHERE token = 'tu-token';
   ```

2. **Check logs**
   ```bash
   grep "Password reset" logs/application.log
   ```

---

## 📚 Referencias

- [Spring Boot Mail](https://docs.spring.io/spring-boot/docs/current/reference/html/io.html#io.email)
- [Gmail SMTP Settings](https://support.google.com/mail/answer/7126229)
- [HTML Email Best Practices](https://www.campaignmonitor.com/dev-resources/guides/coding-html-emails/)

---

**✅ Backend completado exitosamente. Listo para implementar el frontend.**
