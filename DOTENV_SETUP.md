# 🚀 Configuración de Variables de Entorno con dotenv-java

## ✅ Cambios Realizados

Se ha migrado el sistema de variables de entorno a **dotenv-java** para una gestión más limpia y profesional.

### 📦 Dependencias Añadidas

- **dotenv-java 3.0.2** - Carga automática de variables desde `.env`

### 🗑️ Archivos Eliminados

- ~~`start-dev.ps1`~~ - Ya no es necesario, las variables se cargan automáticamente

### 📁 Archivos Creados

1. **`DotenvConfig.java`** - Configuración para cargar `.env` al iniciar Spring Boot
2. **`META-INF/spring.factories`** - Registro del initializer

---

## 🎯 Cómo Funciona

### 1. Al Iniciar la Aplicación

Spring Boot **automáticamente**:
1. Lee el archivo `.env` en la raíz del proyecto
2. Carga todas las variables de entorno
3. Las hace disponibles para Spring Boot (`@Value`, `Environment`, etc.)

### 2. Orden de Prioridad

Las variables se cargan en este orden (de mayor a menor prioridad):

1. **Variables del sistema operativo** (las que configuras en Windows/Linux)
2. **Archivo `.env`** (desarrollo local)
3. **`application.properties`** (valores por defecto)

---

## 🛠️ Uso en Desarrollo Local

### Paso 1: Configurar `.env`

Edita el archivo **`.env`** en la raíz del proyecto:

```bash
# Database
JDBC_DATABASE_URL=jdbc:postgresql://localhost:5433/sl_db
JDBC_DATABASE_USERNAME=postgres
JDBC_DATABASE_PASSWORD=tu_password

# Email
MAIL_HOST=smtp.ionos.es
MAIL_PORT=587
MAIL_USERNAME=tu-email@screenleads.com
MAIL_PASSWORD=tu_password

# JWT
JWT_SECRET_KEY=tu_secret_key
```

### Paso 2: Ejecutar la Aplicación

Ahora simplemente ejecuta:

```bash
# Desde la raíz del proyecto
./mvnw spring-boot:run
```

O desde tu IDE (IntelliJ, Eclipse, VSCode):
- Run → Spring Boot App

✅ **Las variables se cargarán automáticamente del `.env`**

---

## 🐳 Uso en Docker

En Docker, **NO uses el archivo `.env`**. Pasa las variables directamente:

```yaml
# docker-compose.yml
services:
  backend:
    image: screenleads-backend
    environment:
      - JDBC_DATABASE_URL=jdbc:postgresql://db:5432/sl_db
      - JDBC_DATABASE_USERNAME=postgres
      - JDBC_DATABASE_PASSWORD=${DB_PASSWORD}
      - MAIL_HOST=smtp.ionos.es
      - MAIL_USERNAME=${MAIL_USER}
      - MAIL_PASSWORD=${MAIL_PASS}
```

O con docker run:

```bash
docker run -e JDBC_DATABASE_URL="jdbc:postgresql://..." \
           -e MAIL_HOST="smtp.ionos.es" \
           screenleads-backend
```

---

## ☁️ Uso en Producción (Heroku, AWS, Azure)

En producción, configura las variables en el panel de tu proveedor:

### Heroku
```bash
heroku config:set MAIL_HOST=smtp.ionos.es
heroku config:set MAIL_USERNAME=noreply@screenleads.com
heroku config:set MAIL_PASSWORD=tu_password
```

### AWS Elastic Beanstalk
```bash
aws elasticbeanstalk set-environment-variables \
  --environment-name screenleads-prod \
  --option-settings \
    Namespace=aws:elasticbeanstalk:application:environment,OptionName=MAIL_HOST,Value=smtp.ionos.es
```

### Azure App Service
```bash
az webapp config appsettings set \
  --resource-group screenleads \
  --name screenleads-backend \
  --settings MAIL_HOST=smtp.ionos.es MAIL_USERNAME=noreply@screenleads.com
```

---

## 🔐 Seguridad

### ✅ Buenas Prácticas

1. **NUNCA commitees el `.env` con valores reales**
   - El `.gitignore` ya lo excluye
   - Usa `.env.example` como template

2. **Usa diferentes valores por ambiente**
   - Desarrollo: `.env` local
   - Staging: Variables en la plataforma
   - Producción: Secrets manager (AWS Secrets, Azure Key Vault)

3. **Rota las credenciales periódicamente**
   - Cambia passwords cada 90 días
   - Regenera API keys si hay sospechas de compromiso

---

## 🧪 Testing

### Verificar que las Variables se Cargan

1. Añade un log temporal en `AppApplication.java`:

```java
@SpringBootApplication
public class AppApplication {
    public static void main(String[] args) {
        SpringApplication.run(AppApplication.class, args);
        
        // Temporal: verificar variables
        System.out.println("✅ MAIL_HOST: " + System.getenv("MAIL_HOST"));
        System.out.println("✅ MAIL_USERNAME: " + System.getenv("MAIL_USERNAME"));
    }
}
```

2. Ejecuta y verifica el output:
```
✅ Variables de entorno cargadas desde .env
✅ MAIL_HOST: smtp.ionos.es
✅ MAIL_USERNAME: noreply@screenleads.com
```

---

## ❓ Troubleshooting

### Problema: Variables no se cargan

**Solución 1: Verifica que el `.env` está en la raíz**
```bash
# Debe estar aquí:
sl-dev-backend/
├── .env          ← Aquí
├── pom.xml
└── src/
```

**Solución 2: Reinstala dependencias**
```bash
./mvnw clean install
```

**Solución 3: Limpia la caché de Maven**
```bash
./mvnw dependency:purge-local-repository
./mvnw clean install
```

### Problema: "dotenv-java not found"

Asegúrate de que la dependencia está en `pom.xml`:
```xml
<dependency>
    <groupId>io.github.cdimascio</groupId>
    <artifactId>dotenv-java</artifactId>
    <version>3.0.2</version>
</dependency>
```

---

## 📚 Referencias

- [dotenv-java Documentation](https://github.com/cdimascio/dotenv-java)
- [Spring Boot External Configuration](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.external-config)
- [12-Factor App Config](https://12factor.net/config)

---

## ✅ Ventajas de usar dotenv-java

✅ **Simplicidad** - Un solo comando: `./mvnw spring-boot:run`  
✅ **Compatibilidad** - Funciona en Windows, Linux, Mac  
✅ **Seguridad** - `.env` nunca se commitea  
✅ **Flexibilidad** - Fácil cambiar entre ambientes  
✅ **Estándar** - Usado por Node.js, Python, Ruby, etc.

---

**🎉 Migración completada. Ya no necesitas scripts de PowerShell para cargar variables.**
