# Guía Completa: Eliminar Secretos del Historial de Git

## 📋 Resumen

Esta guía te ayudará a eliminar completamente las credenciales sensibles que fueron accidentalmente commiteadas en el historial de Git del repositorio `sl-dev-backend`.

## ⚠️ ADVERTENCIAS CRÍTICAS

1. **Esto reescribe el historial completo de Git**
2. **Todos los SHA de commits cambiarán**
3. **Requiere force push al remoto**
4. **Todo el equipo debe sincronizar sus copias locales**
5. **Las credenciales expuestas DEBEN rotarse, incluso después de limpiar el historial**

---

## 🔍 Secretos Identificados

Los siguientes secretos fueron encontrados en el historial:

| Tipo | Valor Expuesto | Ubicación | Commits Afectados |
|------|---------------|-----------|-------------------|
| DB Password | `52866617jJ@` | `application-*.properties` | 5 commits |
| JWT Secret | `test_secret` | `application.properties` | 3 commits |
| Firebase Base64 | `U0hKQkNGR...` | `application.properties` | 4 commits |
| Stripe Keys | `sk_test_*`, `rk_test_*`, `whsec_*` | varios | 6 commits |

### Commits Específicos con Credenciales

```bash
# Ver commits que contienen la password
git log -S "52866617jJ@" --all --oneline
# Resultados:
# 1a543cd sp
# d19e626 chore: update AI snapshots [skip ci]
# ba32a29 environments
# 716e475 connection database
# 9128b2b untracked files on main: b98fa26 savepoint advices by devices
```

---

## 🛠️ Métodos de Limpieza

### Opción 1: Script Automatizado (RECOMENDADO)

Hemos creado dos scripts PowerShell que automatizan todo el proceso:

#### Script Simple
```powershell
.\clean-git-history.ps1
```

#### Script Avanzado (con más validaciones)
```powershell
.\clean-secrets-advanced.ps1
```

**Qué hacen estos scripts:**
1. ✅ Crean backup automático del repositorio
2. ✅ Verifican el estado de Git antes de proceder
3. ✅ Ejecutan `git filter-branch` con reemplazos configurados
4. ✅ Limpian referencias antiguas (`git reflog`, `git gc`)
5. ✅ Verifican que los secretos fueron eliminados
6. ✅ Proporcionan instrucciones para los próximos pasos

---

### Opción 2: Manual con git filter-branch

Si prefieres ejecutar el proceso manualmente:

#### Paso 1: Crear Backup

```powershell
cd ..
git clone sl-dev-backend sl-dev-backend-backup-$(Get-Date -Format 'yyyyMMdd')
cd sl-dev-backend
```

#### Paso 2: Ejecutar Filter-Branch

```powershell
$env:FILTER_BRANCH_SQUELCH_WARNING = "1"

git filter-branch --force --index-filter `
  'git ls-files -z | xargs -0 sed -i "s/52866617jJ@/***REMOVED***/g; s/test_secret/***REMOVED***/g; s/U0hKQkNGR0hJSktMTU5PUFFSU1RVVldYWVo3ODkwQUJDREVGRw==/***REMOVED***/g; s/sk_test_[a-zA-Z0-9]*/sk_test_***REMOVED***/g"' `
  --tag-name-filter cat -- --all
```

#### Paso 3: Limpiar Referencias

```powershell
Remove-Item .git/refs/original -Recurse -Force
git reflog expire --expire=now --all
git gc --prune=now --aggressive
```

#### Paso 4: Verificar

```powershell
git log --all -S "52866617jJ@" --oneline
# Debe estar vacío
```

---

### Opción 3: BFG Repo-Cleaner (Más Rápido)

Si tienes Java instalado, BFG es la herramienta más rápida:

#### Instalación

```powershell
# Con Chocolatey (requiere admin)
choco install bfg-repo-cleaner -y

# O descargar manualmente
# https://rtyley.github.io/bfg-repo-cleaner/
```

#### Uso

```powershell
# Crear archivo con secretos a eliminar
@"
52866617jJ@
test_secret
U0hKQkNGR0hJSktMTU5PUFFSU1RVVldYWVo3ODkwQUJDREVGRw==
dummy_base64_value
"@ | Out-File secrets.txt

# Ejecutar BFG
java -jar bfg.jar --replace-text secrets.txt sl-dev-backend

# Limpiar
cd sl-dev-backend
git reflog expire --expire=now --all
git gc --prune=now --aggressive
```

---

## 🔄 Proceso Completo Paso a Paso

### ANTES DE EMPEZAR

- [ ] **Coordinar con el equipo** (Slack, Teams, email)
- [ ] **Verificar que nadie está trabajando** en el repositorio
- [ ] **Documentar el plan de rotación** de credenciales
- [ ] **Programar una ventana de mantenimiento** (15-30 minutos)

### DURANTE EL PROCESO

#### 1. Backup del Repositorio

```powershell
cd D:\Projects\2025\ScreenLeads\Repositories
git clone sl-dev-backend sl-dev-backend-BACKUP-ORIGINAL
```

#### 2. Ejecutar Limpieza

```powershell
cd sl-dev-backend
.\clean-secrets-advanced.ps1
```

Cuando pregunte "Escribes 'CONFIRMO' para continuar", escribe exactamente: **CONFIRMO**

#### 3. Verificar Resultados

El script mostrará:
- ✅ `[OK] '52866617jJ@' eliminado correctamente`
- ✅ `[OK] 'test_secret' eliminado correctamente`
- etc.

Si alguno muestra `[!]`, repite el proceso o contacta a soporte.

#### 4. Rotar Credenciales INMEDIATAMENTE

**a) PostgreSQL (Heroku)**

```bash
# Dev
heroku pg:credentials:rotate DATABASE -a screenleads-dev
heroku config:get DATABASE_URL -a screenleads-dev

# Pre
heroku pg:credentials:rotate DATABASE -a screenleads-pre
heroku config:get DATABASE_URL -a screenleads-pre

# Pro
heroku pg:credentials:rotate DATABASE -a screenleads-pro
heroku config:get DATABASE_URL -a screenleads-pro
```

**b) Stripe**

1. Ir a: https://dashboard.stripe.com/test/apikeys
2. Click en "Roll" para cada clave:
   - Secret key (`sk_test_*`)
   - Restricted key (`rk_test_*`)
3. Ir a: https://dashboard.stripe.com/test/webhooks
4. Editar webhook y hacer "Roll secret" (`whsec_*`)

**c) JWT Secret**

```bash
# Generar nuevo (256 bits)
openssl rand -base64 32
```

**d) Firebase**

1. Ir a: Firebase Console > Project Settings > Service Accounts
2. Click "Generate new private key"
3. Guardar JSON y convertir a Base64:

```powershell
$json = Get-Content firebase-key.json -Raw
[Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($json))
```

#### 5. Actualizar .env

Editar `D:\Projects\2025\ScreenLeads\Repositories\sl-dev-backend\.env`:

```env
# Nuevas credenciales rotadas
DATABASE_PASSWORD=<nueva_password_heroku>
DATABASE_PASSWORD_PRE=<nueva_password_heroku_pre>
DATABASE_PASSWORD_PRO=<nueva_password_heroku_pro>

STRIPE_SECRET_KEY=<nueva_sk_test_...>
STRIPE_RESTRICTED_KEY=<nueva_rk_test_...>
STRIPE_WEBHOOK_SECRET=<nueva_whsec_...>

JWT_SECRET_KEY=<nuevo_secret_256_bits>

GOOGLE_CREDENTIALS_BASE64=<nuevo_base64_firebase>
```

#### 6. Actualizar Configuración de Heroku

```bash
# Dev
heroku config:set JWT_SECRET_KEY="<nuevo>" -a screenleads-dev
heroku config:set STRIPE_SECRET_KEY="<nuevo>" -a screenleads-dev
# ... (repetir para todas las variables y ambientes)

# Pre
heroku config:set JWT_SECRET_KEY="<nuevo>" -a screenleads-pre
# ...

# Pro
heroku config:set JWT_SECRET_KEY="<nuevo>" -a screenleads-pro
# ...
```

#### 7. Commit y Test Local

```powershell
git add .env
git commit -m "chore: update rotated credentials after history cleanup"

# Test local
.\setup-environment.ps1 -LoadEnvFile
mvn clean package
mvn spring-boot:run
```

#### 8. Force Push (CUIDADO)

```powershell
# Push a todas las ramas
git push origin --force --all

# Push a todos los tags
git push origin --force --tags
```

### DESPUÉS DEL PROCESO

#### 9. Comunicar al Equipo

Enviar este mensaje al equipo:

```
🚨 ATENCIÓN: Historial de Git Reescrito 🚨

Se ha completado la limpieza de credenciales sensibles del repositorio sl-dev-backend.

ACCIÓN REQUERIDA PARA TODOS:

1. Guardar cambios locales:
   git stash

2. Descargar nueva historia:
   git fetch origin
   git reset --hard origin/feature/v2

3. Restaurar cambios (si los había):
   git stash pop

4. Actualizar .env con las nuevas credenciales
   (se enviarán por canal seguro)

NO intentar hacer merge o pull normal - DEBE ser reset --hard

⏰ Fecha: <hoy>
📧 Dudas: contactar a <tu_nombre>
```

#### 10. Verificación Final

```powershell
# Verificar que GitHub no tiene los secretos
git clone https://github.com/screenleads/sl-dev-backend.git test-clone
cd test-clone
git log -S "52866617jJ@" --all
# Debe estar vacío

cd ..
Remove-Item test-clone -Recurse -Force
```

---

## 📊 Checklist de Verificación

### Pre-Limpieza
- [ ] Backup del repositorio creado
- [ ] Equipo notificado y coordinado
- [ ] Plan de rotación de credenciales listo
- [ ] Ambiente de prueba disponible

### Durante Limpieza
- [ ] Script ejecutado sin errores
- [ ] Todas las verificaciones pasaron (✅)
- [ ] Credenciales rotadas inmediatamente
- [ ] .env actualizado con nuevos valores
- [ ] Tests locales pasaron (mvn test)
- [ ] Aplicación arranca correctamente

### Post-Limpieza
- [ ] Force push completado
- [ ] Equipo sincronizado (cada miembro confirmó)
- [ ] Verificación en GitHub (clone fresco)
- [ ] No se encuentran secretos en el historial
- [ ] Configuración de Heroku actualizada
- [ ] Documentación actualizada

---

## 🆘 Troubleshooting

### "filter-branch completo con advertencias"

**Solución:** Es normal. Verifica que los secretos fueron eliminados:
```powershell
git log -S "52866617jJ@" --all
```

Si está vacío, todo está bien.

---

### "Todavia se encontro 'XXX' en commits"

**Solución:** El patrón no fue reemplazado correctamente.

1. Revisar el script `clean-secrets-advanced.ps1`
2. Verificar que el patrón esté escapado correctamente
3. Ejecutar nuevamente desde el backup

---

### "El equipo no puede hacer pull"

**Solución:** Esto es esperado. Deben hacer `reset --hard`:

```powershell
git fetch origin
git reset --hard origin/feature/v2
```

---

### "Perdí commits locales después del reset"

**Solución:** Recuperar desde reflog:

```powershell
git reflog
git cherry-pick <SHA_del_commit_perdido>
```

---

## 📚 Referencias

- [Git Filter-Branch Documentation](https://git-scm.com/docs/git-filter-branch)
- [BFG Repo-Cleaner](https://rtyley.github.io/bfg-repo-cleaner/)
- [GitHub: Removing Sensitive Data](https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/removing-sensitive-data-from-a-repository)
- [Heroku: Rotating Credentials](https://devcenter.heroku.com/articles/heroku-postgresql#rotating-credentials)
- [Stripe: API Key Management](https://stripe.com/docs/keys)

---

## 📞 Soporte

Si tienes problemas durante el proceso:

1. **NO hagas force push** hasta resolver el problema
2. Consulta la sección Troubleshooting
3. Contacta al equipo de DevOps/Security
4. Restaura desde el backup si es necesario

---

## ✅ Confirmación Final

Una vez completado el proceso:

```powershell
# Verificación final
git log --all --oneline | Select-Object -First 20
git log -S "52866617jJ@" --all
git log -S "test_secret" --all
git log -S "U0hKQkNGR0hJSktMTU5PUFFSU1RVVldYWVo3ODkwQUJDREVGRw==" --all

# Si todos están vacíos, ÉXITO ✅
```

**Credenciales antiguas NUNCA deben usarse de nuevo, incluso si el historial está limpio.**

---

*Última actualización: 3 de Diciembre de 2025*
