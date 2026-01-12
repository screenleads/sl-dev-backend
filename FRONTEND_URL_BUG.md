# 🐛 Bug Crítico: URLs Malformadas en Frontend

**Fecha**: 11 de enero de 2026  
**Severidad**: 🔴 CRÍTICA  
**Impacto**: CORS errors, endpoints que no funcionan

---

## 🔍 Problema Detectado

El frontend está generando URLs con **doble slash** (`//`) al concatenar la baseURL con los endpoints:

### Ejemplos de URLs Malformadas

```
❌ https://api.pre.screenleads.com//actuator/health
❌ https://api.pre.screenleads.comapp-versions/latest/android
❌ https://api.pre.screenleads.com//devices

✅ https://api.pre.screenleads.com/actuator/health
✅ https://api.pre.screenleads.com/app-versions/latest/android
✅ https://api.pre.screenleads.com/devices
```

---

## 🚨 Consecuencias

1. **CORS Errors**: El navegador bloquea las peticiones porque la URL es diferente
2. **404 Errors**: Los endpoints no se encuentran por la ruta malformada
3. **Failed to load resource**: Errores en la consola del navegador

---

## 💡 Causa Raíz

Concatenación incorrecta de strings al construir las URLs:

```typescript
// ❌ INCORRECTO - Genera doble slash
const baseUrl = 'https://api.pre.screenleads.com/';  // Termina en /
const endpoint = '/actuator/health';                 // Empieza con /
const url = baseUrl + endpoint;  
// Resultado: https://api.pre.screenleads.com//actuator/health

// ❌ TAMBIÉN INCORRECTO - Falta el slash
const baseUrl = 'https://api.pre.screenleads.com';   // Sin /
const endpoint = 'actuator/health';                  // Sin /
const url = baseUrl + endpoint;
// Resultado: https://api.pre.screenleads.comactuator/health
```

---

## ✅ Soluciones

### Solución 1: Normalizar la Configuración (Más Simple)

```typescript
// environment.ts o config.ts
export const environment = {
  apiUrl: 'https://api.pre.screenleads.com'  // ❗ SIN slash al final
};

// En los servicios
export class ApiService {
  private baseUrl = environment.apiUrl;
  
  getHealth() {
    return this.http.get(`${this.baseUrl}/actuator/health`);  // ✅ CON slash
  }
  
  getDevices() {
    return this.http.get(`${this.baseUrl}/devices`);  // ✅ CON slash
  }
}
```

**Regla**: 
- `baseUrl` → **SIN** slash al final
- `endpoint` → **CON** slash al inicio

---

### Solución 2: Crear Función Helper (Más Robusta)

```typescript
// utils/url.helper.ts
export function buildApiUrl(endpoint: string): string {
  const base = environment.apiUrl.replace(/\/+$/, '');    // Quitar slashes finales
  const path = endpoint.replace(/^\/+/, '');              // Quitar slashes iniciales
  return `${base}/${path}`;
}

// Uso en servicios
export class ApiService {
  getHealth() {
    return this.http.get(buildApiUrl('/actuator/health'));
  }
  
  getDevices() {
    return this.http.get(buildApiUrl('devices'));
  }
}
```

---

### Solución 3: Usar HTTP Interceptor (Global)

```typescript
// http-interceptor.service.ts
@Injectable()
export class UrlNormalizationInterceptor implements HttpInterceptor {
  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    // Normalizar URL eliminando slashes duplicados
    const normalizedUrl = req.url.replace(/([^:]\/)\/+/g, '$1');
    
    if (normalizedUrl !== req.url) {
      console.warn(`URL normalizada: ${req.url} -> ${normalizedUrl}`);
      req = req.clone({ url: normalizedUrl });
    }
    
    return next.handle(req);
  }
}

// app.module.ts
providers: [
  {
    provide: HTTP_INTERCEPTORS,
    useClass: UrlNormalizationInterceptor,
    multi: true
  }
]
```

---

## 🔧 Workaround Temporal en Backend

Mientras se corrige el frontend, he implementado en el backend:

1. **UrlNormalizationFilter**: Normaliza automáticamente las URLs con doble slash
2. **Configuración de Tomcat**: Permite caracteres relajados en paths

**⚠️ IMPORTANTE**: Esto es solo temporal. El frontend **DEBE** corregirse.

---

## 📋 Checklist de Corrección

- [ ] Revisar `environment.ts` / `environment.prod.ts`
- [ ] Verificar que `apiUrl` NO termine en `/`
- [ ] Buscar todas las concatenaciones de URL en el código
- [ ] Implementar función `buildApiUrl()` helper
- [ ] Probar todos los endpoints después del cambio
- [ ] Verificar logs de consola (no deben aparecer URLs con `//`)
- [ ] Remover workaround del backend cuando esté corregido

---

## 🔍 Cómo Encontrar el Problema

```bash
# Buscar concatenaciones directas de URLs
grep -r "baseUrl + " src/
grep -r "apiUrl + " src/
grep -r "\${.*}/" src/

# Buscar definiciones de baseUrl/apiUrl
grep -r "apiUrl.*=" src/
grep -r "baseUrl.*=" src/
```

---

## 📊 Prioridad

**🔴 URGENTE**: Esto está bloqueando funcionalidades críticas como:
- Health checks
- Registro de dispositivos  
- Verificación de versiones
- Autenticación

**Tiempo estimado de corrección**: 30 minutos

---

## 🧪 Testing

Después de corregir, verificar en Chrome DevTools (Network tab) que las URLs sean:

```
✅ https://api.pre.screenleads.com/actuator/health
✅ https://api.pre.screenleads.com/devices
✅ https://api.pre.screenleads.com/app-versions/latest/android
```

Y **NO**:

```
❌ https://api.pre.screenleads.com//actuator/health
❌ https://api.pre.screenleads.comapp-versions/latest/android
```

---

**Contacto**: Backend Team  
**Estado**: ⏳ Esperando corrección en Frontend
