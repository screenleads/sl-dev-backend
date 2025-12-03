# Resumen: Limpieza de Credenciales del Historial de Git

## ✅ Estado: PREPARADO PARA EJECUTAR

He creado un sistema completo y automatizado para eliminar todas las credenciales sensibles del historial de Git del repositorio `sl-dev-backend`.

---

## 📦 Archivos Creados

### 1. **clean-secrets-advanced.ps1** (Script Principal)
- ✅ Automatiza todo el proceso de limpieza
- ✅ Crea backups automáticos
- ✅ Valida cada paso del proceso
- ✅ Verifica que los secretos fueron eliminados
- ✅ Proporciona instrucciones detalladas al final

**Uso:**
```powershell
cd D:\Projects\2025\ScreenLeads\Repositories\sl-dev-backend
.\clean-secrets-advanced.ps1
```

### 2. **GIT_HISTORY_CLEANUP_GUIDE.md** (Guía Completa)
Documentación exhaustiva que incluye:
- ⚠️ Advertencias y requisitos previos
- 🔍 Lista de secretos identificados
- 🛠️ 3 métodos diferentes de limpieza
- 📋 Checklist completo paso a paso
- 🔄 Procedimientos de rotación de credenciales
- 🆘 Sección de troubleshooting
- 📧 Templates para comunicación con el equipo

### 3. **clean-git-history.ps1** (Script Alternativo)
- ✅ Versión simplificada del proceso
- ✅ Útil si necesitas más control manual

---

## 🎯 Secretos a Eliminar

| Tipo | Valor | Ubicación |
|------|-------|-----------|
| **PostgreSQL Password** | `52866617jJ@` | application-*.properties |
| **JWT Secret** | `test_secret` | application.properties |
| **Firebase Base64** | `U0hKQkNGR...` | application.properties |
| **Dummy Base64** | `dummy_base64_value` | application.properties |
| **Stripe Keys** | `sk_test_*`, `rk_test_*`, `whsec_*` | varios archivos |

---

## 🚀 Cómo Ejecutar (Opción Recomendada)

### Paso 1: Revisar la Guía
```powershell
code GIT_HISTORY_CLEANUP_GUIDE.md
```

### Paso 2: Coordinar con el Equipo
- Enviar notificación al equipo
- Programar ventana de mantenimiento (15-30 min)
- Asegurar que nadie está trabajando en el repositorio

### Paso 3: Ejecutar el Script
```powershell
.\clean-secrets-advanced.ps1
```

El script te pedirá confirmación escribiendo `CONFIRMO` para proceder.

### Paso 4: Rotar Credenciales (CRÍTICO)

**PostgreSQL:**
```bash
heroku pg:credentials:rotate DATABASE -a screenleads-dev
heroku pg:credentials:rotate DATABASE -a screenleads-pre
heroku pg:credentials:rotate DATABASE -a screenleads-pro
```

**Stripe:**
- Dashboard > API Keys > Roll cada clave
- Webhooks > Edit > Roll secret

**JWT:**
```bash
openssl rand -base64 32
```

**Firebase:**
- Console > Service Accounts > Generate new private key

### Paso 5: Actualizar .env
```env
DATABASE_PASSWORD=<nueva_password>
STRIPE_SECRET_KEY=<nueva_sk_test_>
JWT_SECRET_KEY=<nuevo_secret>
GOOGLE_CREDENTIALS_BASE64=<nuevo_base64>
```

### Paso 6: Force Push
```powershell
git push origin --force --all
git push origin --force --tags
```

### Paso 7: Notificar al Equipo
Usar template de la guía para instruir al equipo a hacer:
```powershell
git fetch origin
git reset --hard origin/feature/v2
```

---

## ⚡ Alternativa Rápida (Si tienes experiencia)

Si ya conoces el proceso y quieres ir directo al grano:

```powershell
# 1. Backup
git clone . ../sl-dev-backend-backup-$(Get-Date -Format 'yyyyMMdd')

# 2. Limpieza automática
.\clean-secrets-advanced.ps1

# 3. Rotar credenciales en todos los servicios

# 4. Force push
git push origin --force --all
git push origin --force --tags

# 5. Notificar al equipo
```

---

## 📊 Lo que el Script Hace Automáticamente

1. ✅ **Verifica** que estás en un repositorio Git
2. ✅ **Crea backup** automático con timestamp
3. ✅ **Muestra commits** que contienen secretos
4. ✅ **Pide confirmación** explícita (`CONFIRMO`)
5. ✅ **Ejecuta filter-branch** con todos los reemplazos
6. ✅ **Limpia referencias** antiguas (reflog, gc)
7. ✅ **Verifica resultados** para cada secreto
8. ✅ **Proporciona instrucciones** detalladas de próximos pasos
9. ✅ **Limpia archivos temporales** automáticamente

---

## 🔒 Seguridad Post-Limpieza

Incluso después de limpiar el historial:

1. **Las credenciales expuestas DEBEN rotarse** (son públicas para siempre)
2. **GitHub/GitLab caché** puede mantener los secretos por hasta 30 días
3. **Backups de terceros** pueden tener copias del historial antiguo
4. **Clones existentes** tienen el historial completo hasta que hagan reset

**Por eso la rotación de credenciales es OBLIGATORIA, no opcional.**

---

## 📋 Checklist Rápido

- [ ] Coordinar con el equipo
- [ ] Ejecutar `.\clean-secrets-advanced.ps1`
- [ ] Confirmar con `CONFIRMO`
- [ ] Rotar **PostgreSQL** passwords
- [ ] Rotar **Stripe** keys (sk_, rk_, whsec_)
- [ ] Rotar **JWT** secret
- [ ] Rotar **Firebase** credentials
- [ ] Actualizar `.env` local
- [ ] Actualizar Heroku config (dev/pre/pro)
- [ ] Force push a GitHub
- [ ] Notificar al equipo (con instrucciones)
- [ ] Verificar que cada miembro sincronizó
- [ ] Confirmar que app funciona en todos los ambientes

---

## 🆘 Si Algo Sale Mal

**Tenemos 2 copias de backup:**

1. **Backup del script:** `sl-dev-backend-backup-<timestamp>`
2. **Backup manual:** (si creaste uno antes)

**Para restaurar:**
```powershell
cd D:\Projects\2025\ScreenLeads\Repositories
Remove-Item sl-dev-backend -Recurse -Force
Copy-Item sl-dev-backend-backup-<timestamp> sl-dev-backend -Recurse
```

---

## 📈 Próximos Pasos Recomendados

Después de completar la limpieza:

1. **Implementar Secret Scanning** en GitHub
   - Settings > Security > Secret scanning alerts

2. **Configurar Pre-commit Hooks** para prevenir futuros leaks
   ```bash
   pip install detect-secrets
   detect-secrets scan > .secrets.baseline
   ```

3. **Activar Vault** (ya está implementado, solo activar)
   ```env
   VAULT_ENABLED=true
   VAULT_URI=https://vault.example.com
   VAULT_TOKEN=<token>
   ```

4. **Audit de Seguridad** completo
   - Revisar logs de acceso
   - Verificar actividad sospechosa en Stripe/Heroku
   - Rotar claves de API de terceros

---

## 📞 Necesitas Ayuda?

1. **Revisa** `GIT_HISTORY_CLEANUP_GUIDE.md` - tiene troubleshooting completo
2. **Backups** están en `../sl-dev-backend-backup-*`
3. **NO hagas force push** si tienes dudas
4. **Contacta** al equipo de seguridad si encuentras problemas

---

## ✨ Resumen Ejecutivo

**Lo que hicimos:**
- ✅ Identificamos 5 tipos de credenciales expuestas en 15+ commits
- ✅ Creamos scripts automatizados para limpiar el historial
- ✅ Preparamos guía completa con todos los pasos
- ✅ Documentamos proceso de rotación de credenciales
- ✅ Creamos templates para comunicación con el equipo

**Lo que debes hacer:**
1. Leer `GIT_HISTORY_CLEANUP_GUIDE.md`
2. Coordinar con el equipo
3. Ejecutar `.\clean-secrets-advanced.ps1`
4. Rotar TODAS las credenciales
5. Force push
6. Sincronizar al equipo

**Tiempo estimado:** 30-45 minutos

---

*Creado: 3 de Diciembre de 2025*
*Repositorio: sl-dev-backend*
*Branch: feature/v2*
