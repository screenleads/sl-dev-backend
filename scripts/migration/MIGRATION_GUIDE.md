# Guía de Migración al Nuevo Modelo de Dominio

**Fecha**: 13 de enero de 2026  
**Versión**: 1.0.0

---

## 📋 RESUMEN

Esta guía documenta el proceso completo de migración del modelo de dominio antiguo al nuevo diseño que incluye:

- ✅ **Customer** unificado (reemplaza antiguas Customer/Client)
- ✅ **PromotionRedemption** (reemplaza PromotionLead) con verificación y fraude
- ✅ **UserAction** para tracking completo
- ✅ **CompanyBilling**, **Invoice**, **InvoiceItem**, **BillingEvent** para facturación
- ✅ **DataExport** para remarketing
- ✅ **Device** actualizado con geolocalización completa

---

## ⚠️ ADVERTENCIAS CRÍTICAS

1. **BACKUP OBLIGATORIO**: No ejecutar sin backup completo de la base de datos
2. **DOWNTIME REQUERIDO**: La migración requiere detener la aplicación (estimado: 30-60 minutos)
3. **IRREVERSIBLE**: Una vez completada, solo se puede revertir desde backup
4. **TESTING**: Probar primero en entorno de desarrollo/staging

---

## 🎯 PRE-REQUISITOS

### 1. Verificar Estado Actual

```sql
-- Verificar tablas existentes
SHOW TABLES LIKE '%customer%';
SHOW TABLES LIKE '%promotion_lead%';

-- Contar registros
SELECT COUNT(*) FROM customer;
SELECT COUNT(*) FROM promotion_lead;
SELECT COUNT(*) FROM promotion;
SELECT COUNT(*) FROM device;
```

### 2. Verificar Espacio en Disco

```bash
# Linux/Mac
df -h

# Windows PowerShell
Get-PSDrive C | Select-Object Used,Free
```

Requisito mínimo: **2x el tamaño actual de la base de datos** (para backups y tablas temporales)

### 3. Verificar Permisos de Usuario

```sql
SHOW GRANTS FOR CURRENT_USER();
```

Necesitas permisos para:
- CREATE TABLE
- DROP TABLE
- ALTER TABLE
- INSERT, UPDATE, DELETE
- DISABLE/ENABLE FOREIGN KEY CHECKS

---

## 📝 PLAN DE EJECUCIÓN

### FASE 1: Preparación (15 min)

#### 1.1. Notificar a Usuarios
- Avisar con 24-48 horas de antelación
- Programar ventana de mantenimiento (preferiblemente madrugada o fin de semana)

#### 1.2. Detener la Aplicación

```bash
# Heroku
heroku maintenance:on -a screenleads-backend

# Docker
docker-compose down

# Systemd
sudo systemctl stop screenleads-backend

# Manual (matar proceso Java)
pkill -f screenleads-backend
```

#### 1.3. Backup Completo

```bash
# MySQL
mysqldump -u root -p screenleads \
  --single-transaction \
  --routines \
  --triggers \
  --events \
  > backup_pre_migration_$(date +%Y%m%d_%H%M%S).sql

# PostgreSQL (si usas Postgres)
pg_dump -U postgres screenleads \
  > backup_pre_migration_$(date +%Y%m%d_%H%M%S).sql
```

**Windows PowerShell**:
```powershell
$timestamp = (Get-Date).ToString('yyyyMMdd_HHmmss')
mysqldump -u root -p screenleads > "backup_pre_migration_$timestamp.sql"
```

#### 1.4. Verificar Backup

```bash
# Verificar que el archivo existe y tiene contenido
ls -lh backup_pre_migration_*.sql

# Debe ser > 0 bytes, idealmente varios MB
```

---

### FASE 2: Ejecución de Scripts (20-30 min)

#### 2.1. Conectar a la Base de Datos

```bash
mysql -u root -p screenleads
```

#### 2.2. Ejecutar Script de Migración

```sql
-- Desde el cliente MySQL
SOURCE d:/Projects/2025/ScreenLeads/Repositories/sl-dev-backend/scripts/migration/V1_0_0__Migration_To_New_Domain_Model.sql;
```

O desde línea de comandos:
```bash
mysql -u root -p screenleads < scripts/migration/V1_0_0__Migration_To_New_Domain_Model.sql
```

#### 2.3. Monitorear Progreso

El script mostrará mensajes de progreso para cada paso. Observar:
- ✅ Mensajes de éxito
- ⚠️ Warnings (revisar pero pueden ser aceptables)
- ❌ Errores (detener y revisar)

---

### FASE 3: Actualizar Código de la Aplicación (10-15 min)

#### 3.1. Actualizar `application.properties` / `application.yml`

**IMPORTANTE**: Asegurar que Hibernate NO recree las tablas:

```properties
# application.properties
spring.jpa.hibernate.ddl-auto=validate
# O si prefieres que actualice columnas pero no borre datos:
# spring.jpa.hibernate.ddl-auto=update
```

#### 3.2. Compilar la Aplicación

```bash
# Maven
./mvnw clean package -DskipTests

# Gradle
./gradlew clean build -x test
```

#### 3.3. Ejecutar Tests Unitarios

```bash
./mvnw test
```

Si hay tests que fallan relacionados con las entidades antiguas, actualizarlos o deshabilitarlos temporalmente.

---

### FASE 4: Verificación (10-15 min)

#### 4.1. Verificación de Datos

```sql
-- Ejecutar consultas de verificación
SOURCE scripts/migration/verify_migration.sql;
```

O manualmente:

```sql
-- 1. Verificar conteos
SELECT 
    'Customers' as tabla,
    COUNT(*) as registros
FROM customer
UNION ALL
SELECT 
    'PromotionRedemptions' as tabla,
    COUNT(*) as registros
FROM promotion_redemption
UNION ALL
SELECT 
    'UserActions' as tabla,
    COUNT(*) as registros
FROM user_action;

-- 2. Verificar integridad referencial
SELECT 
    'Redemptions sin Customer' as issue,
    COUNT(*) as problemas
FROM promotion_redemption
WHERE customer_id IS NULL
UNION ALL
SELECT 
    'Redemptions sin Promotion' as issue,
    COUNT(*) as problemas
FROM promotion_redemption
WHERE promotion_id IS NULL;

-- 3. Verificar emails duplicados
SELECT email, COUNT(*) as duplicados
FROM customer
WHERE email IS NOT NULL
GROUP BY email
HAVING COUNT(*) > 1;
```

#### 4.2. Iniciar la Aplicación

```bash
# Heroku
heroku maintenance:off -a screenleads-backend

# Docker
docker-compose up -d

# Manual
java -jar target/screenleads-backend.jar
```

#### 4.3. Verificar Logs

```bash
# Ver últimas 100 líneas
tail -n 100 logs/application.log

# Heroku
heroku logs --tail -a screenleads-backend

# Docker
docker-compose logs -f backend
```

Buscar:
- ✅ "Started ScreenLeadsApplication" → OK
- ❌ "Error creating bean" → Problema con entidades
- ❌ "Table doesn't exist" → Migración incompleta
- ❌ "Foreign key constraint fails" → Problema de integridad

---

### FASE 5: Testing Funcional (15 min)

#### 5.1. Test de Login (Dashboard)
- [ ] Acceder al dashboard con usuario existente
- [ ] Verificar que se cargan las empresas
- [ ] Revisar lista de dispositivos
- [ ] Ver lista de promociones

#### 5.2. Test de Visualización en Dispositivo
- [ ] Abrir app de dispositivo
- [ ] Verificar que se cargan promociones
- [ ] Verificar que se muestran advices

#### 5.3. Test de Canje
- [ ] Iniciar canje de promoción
- [ ] Rellenar formulario con nuevo email
- [ ] Completar canje
- [ ] Verificar que se crea:
  - [ ] Nuevo Customer
  - [ ] Nuevo PromotionRedemption
  - [ ] Nuevo UserAction

```sql
-- Verificar último canje
SELECT 
    pr.id,
    pr.coupon_code,
    pr.created_at,
    c.email,
    p.name as promotion_name
FROM promotion_redemption pr
JOIN customer c ON c.id = pr.customer_id
JOIN promotion p ON p.id = pr.promotion_id
ORDER BY pr.created_at DESC
LIMIT 5;
```

#### 5.4. Test de Métricas
- [ ] Ver métricas en dashboard
- [ ] Verificar contadores de canjes
- [ ] Revisar gráficas de actividad

---

## 🔄 ROLLBACK (Si algo sale mal)

### Opción 1: Rollback Completo desde Backup

```bash
# 1. Detener aplicación
heroku maintenance:on -a screenleads-backend

# 2. Eliminar base de datos actual
mysql -u root -p -e "DROP DATABASE screenleads; CREATE DATABASE screenleads;"

# 3. Restaurar backup
mysql -u root -p screenleads < backup_pre_migration_20260113_120000.sql

# 4. Revertir código (git)
git checkout HEAD~1  # O el commit antes de los cambios

# 5. Reiniciar aplicación
heroku maintenance:off -a screenleads-backend
```

### Opción 2: Rollback Parcial (Restaurar solo tablas)

```sql
-- 1. Eliminar tablas nuevas
DROP TABLE IF EXISTS user_action;
DROP TABLE IF EXISTS data_export;
DROP TABLE IF EXISTS billing_event;
DROP TABLE IF EXISTS invoice_item;
DROP TABLE IF EXISTS invoice;
DROP TABLE IF EXISTS company_billing;
DROP TABLE IF EXISTS promotion_redemption;
DROP TABLE IF EXISTS customer_auth_method;
DROP TABLE IF EXISTS customer;

-- 2. Restaurar tablas antiguas
ALTER TABLE _old_customer RENAME TO customer;
ALTER TABLE _old_promotion_lead RENAME TO promotion_lead;

-- 3. Verificar
SELECT COUNT(*) FROM customer;
SELECT COUNT(*) FROM promotion_lead;
```

---

## 📊 MONITORING POST-MIGRACIÓN

### Primera Semana

Monitorear diariamente:

1. **Logs de Errores**
```bash
grep -i "error\|exception" logs/application.log | tail -50
```

2. **Consultas Lentas**
```sql
-- MySQL
SELECT * FROM mysql.slow_query_log
ORDER BY query_time DESC
LIMIT 10;
```

3. **Crecimiento de Tablas**
```sql
SELECT 
    table_name,
    table_rows,
    ROUND(((data_length + index_length) / 1024 / 1024), 2) AS "Size (MB)"
FROM information_schema.TABLES
WHERE table_schema = 'screenleads'
    AND table_name IN ('customer', 'promotion_redemption', 'user_action')
ORDER BY (data_length + index_length) DESC;
```

4. **Performance de Endpoints**
- Medir tiempos de respuesta de APIs clave
- Comparar con baseline pre-migración

---

## 🗑️ LIMPIEZA (Después de 1 mes sin problemas)

```sql
-- Eliminar tablas de backup
DROP TABLE IF EXISTS _old_customer;
DROP TABLE IF EXISTS _old_promotion_lead;
DROP TABLE IF EXISTS _old_client;
DROP TABLE IF EXISTS _backup_customer;
DROP TABLE IF EXISTS _backup_promotion_lead;
DROP TABLE IF EXISTS _migration_customer_mapping;

-- Verificar espacio liberado
SELECT 
    table_schema AS 'Database',
    ROUND(SUM(data_length + index_length) / 1024 / 1024, 2) AS 'Size (MB)'
FROM information_schema.TABLES
WHERE table_schema = 'screenleads'
GROUP BY table_schema;
```

---

## 📞 CONTACTOS DE EMERGENCIA

- **DBA**: [Nombre y teléfono]
- **DevOps**: [Nombre y teléfono]
- **CTO**: [Nombre y teléfono]

---

## 📝 CHECKLIST FINAL

### Pre-Migración
- [ ] Backup completo realizado y verificado
- [ ] Usuarios notificados
- [ ] Ventana de mantenimiento programada
- [ ] Equipo de soporte en standby
- [ ] Scripts de migración revisados
- [ ] Plan de rollback preparado

### Durante Migración
- [ ] Aplicación detenida
- [ ] Script de migración ejecutado sin errores
- [ ] Verificaciones de integridad OK
- [ ] Código actualizado y compilado

### Post-Migración
- [ ] Aplicación iniciada correctamente
- [ ] Logs sin errores críticos
- [ ] Tests funcionales pasados
- [ ] Primer canje de prueba exitoso
- [ ] Métricas visibles en dashboard
- [ ] Usuarios notificados de fin de mantenimiento

### Primera Semana
- [ ] Monitoring diario de logs
- [ ] Verificación de performance
- [ ] Feedback de usuarios recogido
- [ ] Backups automáticos configurados

### Primera Mes
- [ ] Sin incidencias mayores
- [ ] Performance estable
- [ ] Limpieza de tablas antiguas ejecutada
- [ ] Documentación actualizada

---

## 🎉 SIGUIENTE PASO

Una vez completada la migración exitosamente, continuar con:

**Tarea 11: Actualizar servicios y repositorios**
- Crear repositorios para nuevas entidades
- Refactorizar servicios
- Actualizar controladores y DTOs
- Actualizar tests

Ver: [REDISENO_MODELO_DOMINIO.md](../../REDISENO_MODELO_DOMINIO.md)

---

**FIN DE LA GUÍA**
