# 📸 Scripts de Exportación de Snapshots

Scripts para **generar documentación automática** del código fuente, utilizada por sistemas de IA para tener contexto completo del proyecto.

---

## 📋 Scripts Disponibles

### 1. `export-entities.sh` 🐚

**Propósito**: Genera un snapshot Markdown con todas las entidades JPA del proyecto.

**Ejecución**:
```bash
# Desde la raíz del proyecto
bash scripts/export/export-entities.sh
```

**Output**: 
- `docs/ai-entities.md` - Contiene el código completo de todas las entidades

**Uso**: 
- ✅ GitHub Actions (workflow: `.github/workflows/export-entities.yml`)
- ✅ Manual cuando quieras actualizar el snapshot de entidades

**Dependencias**: 
- `git` (para listar archivos)
- `bash` (shell Unix)

---

### 2. `export-snapshots.py` 🐍

**Propósito**: Sistema avanzado de generación de múltiples snapshots configurables (entidades, controllers, services, DTOs, etc.)

**Ejecución**:
```bash
# Desde la raíz del proyecto
python scripts/export/export-snapshots.py docs/ai-snapshots.json
```

**Configuración**: 
- `docs/ai-snapshots.json` - Define qué archivos exportar y cómo organizarlos

**Output**: 
- Múltiples archivos `.md` en `docs/` según configuración
- `docs/ai-snapshots-urls.md` - Índice con enlaces a todos los snapshots

**Uso**: 
- ✅ GitHub Actions (workflow: `.github/workflows/export-snapshots.yml`)
- ✅ Manual para regenerar todos los snapshots

**Dependencias**: 
- Python 3.9+ (preferible con `zoneinfo` para timestamps)
- `docs/ai-snapshots.json` configurado correctamente

---

## 🤖 GitHub Actions

Estos scripts se ejecutan automáticamente en GitHub Actions:

### Workflow: `export-entities.yml`
- **Trigger**: Push a cualquier rama
- **Acción**: Ejecuta `export-entities.sh`
- **Commit**: Auto-commit de `docs/ai-entities.md` si hay cambios

### Workflow: `export-snapshots.yml`
- **Trigger**: Push a cualquier rama
- **Acción**: Ejecuta `export-snapshots.py`
- **Commit**: Auto-commit de todos los snapshots generados

---

## 🔧 Personalización

### Añadir nuevas entidades al snapshot

No requiere cambios - `export-entities.sh` detecta automáticamente todos los archivos en:
```
src/main/java/**/domain/model/*.java
```

### Añadir nuevos snapshots

Edita `docs/ai-snapshots.json` y añade una nueva entrada:

```json
{
  "snapshots": [
    {
      "output": "docs/ai-new-snapshot.md",
      "title": "Mi Nuevo Snapshot",
      "header": "Descripción del snapshot",
      "globs": [
        "src/main/java/com/screenleads/backend/app/web/controller/**/*.java"
      ],
      "excludes": [
        "**/*Test.java"
      ]
    }
  ]
}
```

---

## 📖 Para Qué Sirve

Estos snapshots proporcionan **contexto completo** a sistemas de IA (como GitHub Copilot, ChatGPT, etc.) para:

- ✅ Entender la estructura completa del proyecto
- ✅ Sugerir código consistente con las entidades existentes
- ✅ Detectar patrones y arquitectura del proyecto
- ✅ Generar código siguiendo las convenciones establecidas

---

## ⚠️ Consideraciones

- Los snapshots se regeneran en cada push (via GitHub Actions)
- Los archivos generados NO deben editarse manualmente
- Si ves conflictos en `docs/ai-*.md`, acepta siempre la versión del Actions

---

**Última actualización**: Diciembre 2, 2025
