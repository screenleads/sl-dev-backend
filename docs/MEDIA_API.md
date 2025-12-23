# Media API - Documentación de Endpoints

## Índice
- [Autenticación](#autenticación)
- [Endpoints de Lectura](#endpoints-de-lectura)
- [Endpoints de Subida y Procesamiento](#endpoints-de-subida-y-procesamiento)
- [Endpoints CRUD](#endpoints-crud)
- [Códigos de Estado](#códigos-de-estado)
- [Formatos Soportados](#formatos-soportados)

---

## Autenticación

Todos los endpoints soportan dos tipos de autenticación:

### 1. JWT Authentication
```http
Authorization: Bearer {jwt_token}
```

### 2. API Key Authentication
```http
X-API-KEY: {api_key}
client-id: {client_id}
```

**Permisos requeridos:**
- **Lectura (`media:read`)**: GET /medias, GET /medias/{id}, GET /medias/render/{id}
- **Creación (`media:create`)**: POST /medias/upload, POST /medias/upload-from-url, POST /medias
- **Actualización (`media:update`)**: PUT /medias/{id}
- **Eliminación (`media:delete`)**: DELETE /medias/{id}

---

## Endpoints de Lectura

### GET `/medias`
Obtiene la lista de todos los archivos multimedia.

**Autenticación:** Requerida (permiso `media:read`)

**Parámetros de entrada:** Ninguno

**Respuesta exitosa (200 OK):**
```json
[
  {
    "id": 1,
    "src": "compressed-uuid-image.jpg",
    "thumbnail": "thumb-320-uuid-image.jpg",
    "type": "image",
    "url": "https://storage.googleapis.com/.../compressed-uuid-image.jpg",
    "createdAt": "2025-12-23T10:30:00Z"
  },
  {
    "id": 2,
    "src": "compressed-uuid-video.mp4",
    "thumbnail": "thumb-320-uuid-video.jpg",
    "type": "video",
    "url": "https://storage.googleapis.com/.../compressed-uuid-video.mp4",
    "createdAt": "2025-12-23T11:00:00Z"
  }
]
```

---

### GET `/medias/{id}`
Obtiene la información de un archivo multimedia específico por su ID.

**Autenticación:** No requerida

**Parámetros de entrada:**
| Parámetro | Tipo | Ubicación | Descripción | Requerido |
|-----------|------|-----------|-------------|-----------|
| `id` | Long | Path | ID del archivo multimedia | ✅ |

**Respuesta exitosa (200 OK):**
```json
{
  "id": 1,
  "src": "compressed-uuid-image.jpg",
  "thumbnail": "thumb-320-uuid-image.jpg",
  "type": "image",
  "url": "https://storage.googleapis.com/.../compressed-uuid-image.jpg",
  "createdAt": "2025-12-23T10:30:00Z"
}
```

**Respuesta error (404 Not Found):**
```json
{
  "error": "Media not found"
}
```

---

### GET `/medias/render/{id}`
Descarga el archivo multimedia original.

**Autenticación:** No requerida

**Parámetros de entrada:**
| Parámetro | Tipo | Ubicación | Descripción | Requerido |
|-----------|------|-----------|-------------|-----------|
| `id` | Long | Path | ID del archivo multimedia | ✅ |

**Respuesta exitosa (200 OK):**
- Content-Type: `application/octet-stream`
- Content-Disposition: `attachment; filename="archivo.jpg"`
- Body: Archivo binario

**Respuesta error (404 Not Found):**
Cuando el archivo no existe.

---

## Endpoints de Subida y Procesamiento

### POST `/medias/upload`
Sube un archivo mediante multipart/form-data y lo procesa de forma **síncrona**. Comprime el archivo, genera thumbnails y los sube a Firebase Storage.

**Autenticación:** Requerida (permiso `media:create`)

**Content-Type:** `multipart/form-data`

**Parámetros de entrada:**
| Parámetro | Tipo | Ubicación | Descripción | Requerido |
|-----------|------|-----------|-------------|-----------|
| `file` | File | Form Data | Archivo multimedia (imagen o video) | ✅ |

**Restricciones:**
- Tamaño máximo imágenes: 50 MB
- Tamaño máximo videos: 100 MB
- Formatos soportados: Ver [Formatos Soportados](#formatos-soportados)

**Respuesta exitosa (200 OK):**
```json
{
  "status": "ready",
  "type": "image",
  "url": "https://storage.googleapis.com/.../compressed-uuid-image.jpg",
  "thumbnails": [
    "https://storage.googleapis.com/.../thumbnails/320/thumb-320-uuid-image.jpg",
    "https://storage.googleapis.com/.../thumbnails/640/thumb-640-uuid-image.jpg"
  ],
  "processingTimeMs": 2500
}
```

**Respuesta error (400 Bad Request):**
```json
{
  "error": "Archivo vacío"
}
```

**Respuesta error (413 Payload Too Large):**
```json
{
  "error": "Archivo demasiado grande"
}
```

**Respuesta error (500 Internal Server Error):**
```json
{
  "error": "Fallo procesando archivo",
  "detail": "Error comprimiendo video"
}
```

---

### POST `/medias/upload-from-url`
Descarga un archivo desde una URL pública y lo procesa de forma **síncrona**. Comprime el archivo, genera thumbnails y los sube a Firebase Storage.

**Autenticación:** Requerida (permiso `media:create`)

**Content-Type:** `application/json`

**Parámetros de entrada:**
| Parámetro | Tipo | Ubicación | Descripción | Requerido |
|-----------|------|-----------|-------------|-----------|
| `url` | String | Body (JSON) | URL pública del archivo a procesar | ✅ |

**Body ejemplo:**
```json
{
  "url": "https://example.com/path/to/image.jpg"
}
```

**Restricciones:**
- La URL debe ser accesible públicamente
- Timeout de conexión: 10 segundos
- Timeout de lectura: 30 segundos
- Formatos soportados: Ver [Formatos Soportados](#formatos-soportados)

**Respuesta exitosa (200 OK):**
```json
{
  "status": "ready",
  "type": "video",
  "url": "https://storage.googleapis.com/.../compressed-uuid-video.mp4",
  "thumbnails": [
    "https://storage.googleapis.com/.../thumbnails/320/thumb-320-uuid-video.jpg",
    "https://storage.googleapis.com/.../thumbnails/640/thumb-640-uuid-video.jpg"
  ],
  "processingTimeMs": 3500
}
```

**Respuesta error (400 Bad Request):**
```json
{
  "error": "URL vacía o inválida"
}
```

**Respuesta error (500 Internal Server Error):**
```json
{
  "error": "Fallo procesando archivo desde URL",
  "detail": "Connection timeout"
}
```

---

## Endpoints CRUD

### POST `/medias`
Crea un nuevo registro de media en la base de datos.

**Autenticación:** Requerida (permiso `media:create`)

**Content-Type:** `application/json`

**Parámetros de entrada:**
| Parámetro | Tipo | Ubicación | Descripción | Requerido |
|-----------|------|-----------|-------------|-----------|
| `src` | String | Body | Ruta del archivo fuente | ✅ |
| `thumbnail` | String | Body | Ruta del thumbnail | ❌ |
| `type` | String | Body | Tipo: "image" o "video" | ✅ |
| `url` | String | Body | URL pública del archivo | ❌ |

**Body ejemplo:**
```json
{
  "src": "compressed-uuid-image.jpg",
  "thumbnail": "thumb-320-uuid-image.jpg",
  "type": "image",
  "url": "https://storage.googleapis.com/.../compressed-uuid-image.jpg"
}
```

**Respuesta exitosa (200 OK):**
```json
{
  "id": 1,
  "src": "compressed-uuid-image.jpg",
  "thumbnail": "thumb-320-uuid-image.jpg",
  "type": "image",
  "url": "https://storage.googleapis.com/.../compressed-uuid-image.jpg",
  "createdAt": "2025-12-23T10:30:00Z"
}
```

---

### PUT `/medias/{id}`
Actualiza un registro de media existente.

**Autenticación:** Requerida (permiso `media:update`)

**Content-Type:** `application/json`

**Parámetros de entrada:**
| Parámetro | Tipo | Ubicación | Descripción | Requerido |
|-----------|------|-----------|-------------|-----------|
| `id` | Long | Path | ID del archivo a actualizar | ✅ |
| `src` | String | Body | Ruta del archivo fuente | ❌ |
| `thumbnail` | String | Body | Ruta del thumbnail | ❌ |
| `type` | String | Body | Tipo: "image" o "video" | ❌ |
| `url` | String | Body | URL pública del archivo | ❌ |

**Body ejemplo:**
```json
{
  "src": "compressed-uuid-image-updated.jpg",
  "thumbnail": "thumb-320-uuid-image-updated.jpg",
  "type": "image",
  "url": "https://storage.googleapis.com/.../compressed-uuid-image-updated.jpg"
}
```

**Respuesta exitosa (200 OK):**
```json
{
  "id": 1,
  "src": "compressed-uuid-image-updated.jpg",
  "thumbnail": "thumb-320-uuid-image-updated.jpg",
  "type": "image",
  "url": "https://storage.googleapis.com/.../compressed-uuid-image-updated.jpg",
  "createdAt": "2025-12-23T10:30:00Z",
  "updatedAt": "2025-12-23T12:00:00Z"
}
```

---

### DELETE `/medias/{id}`
Elimina un registro de media de la base de datos.

**Autenticación:** Requerida (permiso `media:delete`)

**Parámetros de entrada:**
| Parámetro | Tipo | Ubicación | Descripción | Requerido |
|-----------|------|-----------|-------------|-----------|
| `id` | Long | Path | ID del archivo a eliminar | ✅ |

**Respuesta exitosa (204 No Content):**
Sin contenido en el body.

---

## Códigos de Estado

| Código | Descripción |
|--------|-------------|
| **200** | OK - Operación exitosa |
| **204** | No Content - Eliminación exitosa |
| **400** | Bad Request - Parámetros inválidos |
| **401** | Unauthorized - Autenticación requerida |
| **403** | Forbidden - Sin permisos suficientes |
| **404** | Not Found - Recurso no encontrado |
| **413** | Payload Too Large - Archivo demasiado grande |
| **500** | Internal Server Error - Error del servidor |

---

## Formatos Soportados

### Imágenes
- **JPEG** (.jpg, .jpeg)
- **PNG** (.png)
- **GIF** (.gif)
- **WebP** (.webp)
- **AVIF** (.avif)
- **HEIC** (.heic)
- **HEIF** (.heif)

**Procesamiento de imágenes:**
- Compresión a calidad 85%
- Redimensionamiento máximo: 1920x1080 (manteniendo aspect ratio)
- Conversión a JPG para thumbnails
- Thumbnails generados: 320px y 640px de ancho

### Videos
- **MP4** (.mp4)
- **AVI** (.avi)
- **MOV** (.mov)
- **MKV** (.mkv)
- **WebM** (.webm)

**Procesamiento de videos:**
- Codec: libx264 (H.264)
- Audio codec: AAC
- Bitrate video: 1 Mbps
- Bitrate audio: 128 kbps
- Frame rate: 30 fps
- Resolución máxima: 1920x1080 (manteniendo aspect ratio)
- Formato salida: MP4
- Thumbnails extraídos del primer frame: 320px y 640px de ancho

---

## Proceso de Trabajo

### Upload tradicional (multipart)
```
1. Cliente → Subir archivo → Servidor
2. Servidor → Guardar en temporal
3. Servidor → Procesar (comprimir + thumbnails)
4. Servidor → Subir a Firebase Storage
5. Servidor → Retornar URLs ← Cliente
```

### Upload desde URL
```
1. Cliente → Enviar URL → Servidor
2. Servidor → Descargar archivo desde URL
3. Servidor → Guardar en temporal
4. Servidor → Procesar (comprimir + thumbnails)
5. Servidor → Subir a Firebase Storage
6. Servidor → Retornar URLs ← Cliente
```

**Nota:** Ambos procesos son **síncronos**, el cliente recibe la respuesta final con todas las URLs una vez completado el procesamiento.

---

## Estructura de URLs en Firebase Storage

### Videos
```
media/videos/compressed-{uuid}-{filename}.mp4
media/videos/thumbnails/320/thumb-320-{uuid}-{filename}.jpg
media/videos/thumbnails/640/thumb-640-{uuid}-{filename}.jpg
```

### Imágenes
```
media/images/compressed-{uuid}-{filename}.jpg
media/images/thumbnails/320/thumb-320-{uuid}-{filename}.jpg
media/images/thumbnails/640/thumb-640-{uuid}-{filename}.jpg
```

---

## Ejemplos de Uso

### Ejemplo 1: Subir imagen con JWT
```bash
curl -X POST https://api.screenleads.com/medias/upload \
  -H "Authorization: Bearer eyJhbGc..." \
  -F "file=@/path/to/image.jpg"
```

### Ejemplo 2: Subir video desde URL con API Key
```bash
curl -X POST https://api.screenleads.com/medias/upload-from-url \
  -H "X-API-KEY: sk_live_xxx" \
  -H "client-id: company_123" \
  -H "Content-Type: application/json" \
  -d '{"url": "https://example.com/video.mp4"}'
```

### Ejemplo 3: Listar todos los archivos
```bash
curl -X GET https://api.screenleads.com/medias \
  -H "Authorization: Bearer eyJhbGc..."
```

### Ejemplo 4: Obtener archivo específico
```bash
curl -X GET https://api.screenleads.com/medias/1
```

### Ejemplo 5: Eliminar archivo
```bash
curl -X DELETE https://api.screenleads.com/medias/1 \
  -H "Authorization: Bearer eyJhbGc..."
```
