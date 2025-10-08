#!/usr/bin/env bash
set -euo pipefail

OUT="docs/ai-entities.md"
BRANCH="${GITHUB_REF_NAME:-develop}"

mkdir -p docs

# Cabecera del Markdown
{
  echo "# Entidades JPA — snapshot incrustado"
  echo
  echo "> Snapshot generado desde la rama \`$BRANCH\`. Contiene el **código completo** de cada entidad para revisión."
  echo
  echo "---"
  echo
} > "$OUT"

# Recolecta los .java bajo la carpeta entity/ (ajusta el patrón si cambias la ruta)
mapfile -t FILES < <(git ls-files 'src/main/java/**/domain/model/*.java' | sort)

for f in "${FILES[@]}"; do
  echo "\`\`\`java" >> "$OUT"
  echo "// $f" >> "$OUT"
  cat "$f" >> "$OUT"
  echo "\`\`\`" >> "$OUT"
  echo >> "$OUT"
done

echo "Generado $OUT con ${#FILES[@]} archivo(s)."
