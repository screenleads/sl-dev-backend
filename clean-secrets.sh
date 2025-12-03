#!/bin/bash
# Script para eliminar secretos del historial de Git usando bash

echo "================================================================"
echo "      LIMPIEZA DE SECRETOS - VERSION BASH (OPTIMIZADA)"
echo "================================================================"
echo ""

# Verificar que estamos en un repositorio Git
if [ ! -d ".git" ]; then
    echo "[ERROR] No estás en un repositorio Git"
    exit 1
fi

echo "[1/5] Creando backup..."
TIMESTAMP=$(date +%Y%m%d-%H%M%S)
BACKUP_PATH="../sl-dev-backend-bash-backup-$TIMESTAMP"
git clone . "$BACKUP_PATH" --quiet
echo "[OK] Backup creado: $BACKUP_PATH"
echo ""

echo "[2/5] Preparando filtro..."
# Crear script sed para reemplazos
cat > /tmp/sed-replacements.sed << 'EOF'
s/52866617jJ@/***PASSWORD_REMOVED***/g
s/test_secret/***SECRET_REMOVED***/g
s/U0hKQkNGR0hJSktMTU5PUFFSU1RVVldYWVo3ODkwQUJDREVGRw==/***BASE64_REMOVED***/g
s/dummy_base64_value/***BASE64_REMOVED***/g
s/sk_test_[a-zA-Z0-9]\+/sk_test_***REMOVED***/g
s/rk_test_[a-zA-Z0-9]\+/rk_test_***REMOVED***/g
s/whsec_[a-zA-Z0-9]\+/whsec_***REMOVED***/g
EOF
echo "[OK] Script sed creado"
echo ""

echo "[3/5] Ejecutando git filter-branch..."
echo "[WARNING] Esto puede tardar varios minutos..."
echo ""

# Suprimir advertencias
export FILTER_BRANCH_SQUELCH_WARNING=1

# Ejecutar filter-branch con index-filter (más rápido)
git filter-branch -f --index-filter '
    git ls-files -s | grep -E "\.(properties|env|md)$" | while read mode sha stage file; do
        git cat-file blob $sha | sed -f /tmp/sed-replacements.sed | git hash-object -w --stdin | read newsha
        if [ "$newsha" != "$sha" ]; then
            echo "100644 $newsha 0	$file"
        else
            echo "$mode $sha $stage	$file"
        fi
    done | git update-index --index-info
' --tag-name-filter cat -- --all 2>&1 | grep -v "^Rewrite"

echo ""
echo "[OK] Filter-branch completado"
echo ""

echo "[4/5] Limpiando referencias..."
rm -rf .git/refs/original
git reflog expire --expire=now --all
git gc --prune=now --aggressive 2>&1 | grep -v "^"
echo "[OK] Limpieza completada"
echo ""

echo "[5/5] Verificando resultados..."
echo ""

# Verificar secretos en archivos de configuración
FOUND=0
for secret in "52866617jJ@" "test_secret" "U0hKQkNGR0hJSktMTU5PUFFSU1RVVldYWVo3ODkwQUJDREVGRw=="; do
    if git log --all -S "$secret" --pretty=format:"%h" -- "src/main/resources/*.properties" ".env" | head -1 | grep -q .; then
        echo "[!] '$secret' aún aparece en archivos de configuración"
        FOUND=1
    else
        echo "[OK] '$secret' eliminado de archivos de configuración"
    fi
done

echo ""
if [ $FOUND -eq 0 ]; then
    echo "================================================================"
    echo "          ÉXITO - SECRETOS ELIMINADOS CORRECTAMENTE"
    echo "================================================================"
else
    echo "================================================================"
    echo "              ALGUNOS SECRETOS AÚN PRESENTES"
    echo "================================================================"
fi

echo ""
echo "PRÓXIMOS PASOS:"
echo "1. Rotar TODAS las credenciales inmediatamente"
echo "2. Actualizar .env con nuevas credenciales"
echo "3. git push origin --force --all"
echo "4. git push origin --force --tags"
echo "5. Notificar al equipo"
echo ""
echo "Backup guardado en: $BACKUP_PATH"
echo "================================================================"

# Limpiar
rm -f /tmp/sed-replacements.sed
