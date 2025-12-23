# Configuración de Heroku para sl-dev-backend-pre

## Buildpacks Requeridos

La aplicación necesita dos buildpacks en este orden:

1. **FFmpeg Buildpack** (para procesamiento de videos)
2. **Java Buildpack** (para la aplicación Spring Boot)

## Instalación de FFmpeg Buildpack

### Opción 1: Usando Heroku CLI (Recomendado)

```bash
# 1. Agregar el buildpack de FFmpeg
heroku buildpacks:add --index 1 https://github.com/jonathanong/heroku-buildpack-ffmpeg-latest.git -a sl-dev-backend-pre

# 2. Verificar buildpacks
heroku buildpacks -a sl-dev-backend-pre
```

**Output esperado:**
```
=== sl-dev-backend-pre Buildpack URLs
1. https://github.com/jonathanong/heroku-buildpack-ffmpeg-latest.git
2. heroku/java
```

### Opción 2: Desde Heroku Dashboard

1. Ir a: https://dashboard.heroku.com/apps/sl-dev-backend-pre/settings
2. Scroll hasta "Buildpacks"
3. Click "Add buildpack"
4. Pegar: `https://github.com/jonathanong/heroku-buildpack-ffmpeg-latest.git`
5. Click "Save changes"
6. **IMPORTANTE**: Arrastrar el buildpack de FFmpeg para que esté ANTES de `heroku/java`

## Variables de Entorno Requeridas

Verificar que estas variables estén configuradas:

```bash
# Verificar variables de Firebase
heroku config:get FIREBASE_ENABLED -a sl-dev-backend-pre
heroku config:get GOOGLE_CREDENTIALS_BASE64 -a sl-dev-backend-pre
heroku config:get FIREBASE_STORAGE_BUCKET -a sl-dev-backend-pre

# Si falta alguna, configurarla:
heroku config:set FIREBASE_ENABLED=true -a sl-dev-backend-pre
heroku config:set GOOGLE_CREDENTIALS_BASE64="ewogICJ0eX..." -a sl-dev-backend-pre
heroku config:set FIREBASE_STORAGE_BUCKET="screenleads-e7e0b.firebasestorage.app" -a sl-dev-backend-pre
```

## Re-deploy después de agregar buildpack

Después de agregar el buildpack, necesitas hacer un nuevo deploy:

```bash
# Forzar rebuild con buildpack de FFmpeg
git commit --allow-empty -m "chore: rebuild with ffmpeg buildpack"
git push origin main
```

O desde Heroku CLI:
```bash
heroku releases -a sl-dev-backend-pre
heroku releases:rollback v<número_anterior> -a sl-dev-backend-pre
heroku releases:retry -a sl-dev-backend-pre
```

## Verificación

Después del deploy, verificar que FFmpeg esté disponible:

```bash
# Verificar ubicación de FFmpeg
heroku run "which ffmpeg" -a sl-dev-backend-pre
# Output esperado: /app/vendor/ffmpeg/ffmpeg

# Verificar versión de FFmpeg
heroku run "ffmpeg -version" -a sl-dev-backend-pre
# Output esperado: ffmpeg version N-71064...
```

**IMPORTANTE**: La aplicación está configurada para usar automáticamente el FFmpeg del buildpack en Heroku (`/app/vendor/ffmpeg/ffmpeg`). El código de `MediaProcessingService` detecta si está en Heroku y usa el FFmpeg correcto.

## Logs de Verificación

Buscar en los logs:

```bash
heroku logs --tail -a sl-dev-backend-pre
```

Deberías ver:
- `🔥 Iniciando configuración de Firebase...`
- `✅ Firebase inicializado correctamente`
- `📦 Storage Bucket: screenleads-e7e0b.firebasestorage.app`
- Sin errores de `Cannot run program "/tmp/jave/ffmpeg-amd64-3.5.0"`

## Troubleshooting

### Error: "Cannot run program ffmpeg"
- **Causa**: Buildpack no instalado o en orden incorrecto
- **Solución**: Verificar buildpacks con `heroku buildpacks -a sl-dev-backend-pre`

### Error: "Generic error in an external library" durante compresión de video
- **Causa**: FFmpeg no tiene los codecs necesarios o configuración incompatible
- **Solución 1**: Verificar que el buildpack de FFmpeg esté correctamente instalado:
  ```bash
  heroku run "ffmpeg -version" -a sl-dev-backend-pre
  heroku run "ffmpeg -codecs | grep -E 'h264|aac'" -a sl-dev-backend-pre
  ```
- **Solución 2**: Forzar rebuild con buildpack actualizado:
  ```bash
  # Remover buildpack actual
  heroku buildpacks:remove https://github.com/jonathanong/heroku-buildpack-ffmpeg-latest.git -a sl-dev-backend-pre
  
  # Agregar buildpack actualizado
  heroku buildpacks:add --index 1 https://github.com/jonathanong/heroku-buildpack-ffmpeg-latest.git -a sl-dev-backend-pre
  
  # Re-deploy
  git commit --allow-empty -m "chore: rebuild with updated ffmpeg buildpack"
  git push heroku main
  ```
- **Solución 3**: Si persiste, considerar buildpack alternativo:
  ```bash
  heroku buildpacks:add --index 1 https://github.com/kitcast/buildpack-ffmpeg.git -a sl-dev-backend-pre
  ```

### Error: "FirebaseApp with name [DEFAULT] doesn't exist"
- **Causa**: Variable `FIREBASE_ENABLED` no configurada o en `false`
- **Solución**: `heroku config:set FIREBASE_ENABLED=true -a sl-dev-backend-pre`

### Videos no se procesan
- **Causa**: FFmpeg no disponible
- **Solución**: Seguir pasos de instalación de buildpack arriba

### Error: "Error sending frames to consumers"
- **Causa**: Problema con codecs o resolución del video
- **Diagnóstico**: Verificar logs para ver configuración:
  ```bash
  heroku logs --tail -a sl-dev-backend-pre | grep -E "Resolución|codec|bitrate"
  ```
- **Solución**: El código ahora incluye:
  - Uso explícito de `libx264` codec
  - Dimensiones ajustadas a números pares (requerido por h264)
  - Configuración optimizada para entornos con recursos limitados
  - Mejor manejo de errores con logs detallados

## Documentación de Buildpacks

- FFmpeg Buildpack: https://github.com/jonathanong/heroku-buildpack-ffmpeg-latest
- Java Buildpack: https://devcenter.heroku.com/articles/java-support
- Buildpacks Order: https://devcenter.heroku.com/articles/using-multiple-buildpacks-for-an-app
