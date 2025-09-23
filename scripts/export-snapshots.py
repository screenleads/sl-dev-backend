#!/usr/bin/env python3
# -*- coding: utf-8 -*-
import os, sys, json, glob
from pathlib import Path
from datetime import datetime

try:
    # py>=3.9
    from zoneinfo import ZoneInfo
except Exception:
    ZoneInfo = None

LANG_BY_EXT = {
    ".java": "java",
    ".kt": "kotlin",
    ".xml": "xml",
    ".yml": "yaml",
    ".yaml": "yaml",
    ".properties": "properties",
    ".json": "json",
    ".sql": "sql",
    ".md": "markdown",
    ".sh": "bash",
}

def code_lang(path: str) -> str:
    ext = os.path.splitext(path)[1].lower()
    return LANG_BY_EXT.get(ext, "")

def match_files(globs_list, excludes=None):
    files = set()
    for g in globs_list:
        files.update(glob.glob(g, recursive=True))
    files = [f for f in files if os.path.isfile(f)]
    files.sort()
    if excludes:
        import fnmatch
        filtered = []
        for f in files:
            if any(fnmatch.fnmatch(f, ex) for ex in excludes):
                continue
            filtered.append(f)
        files = filtered
    return files

def write_snapshot(output, title, header, files, branch):
    os.makedirs(os.path.dirname(output), exist_ok=True)
    with open(output, "w", encoding="utf-8") as out:
        out.write(f"# {title}\n\n")
        if header:
            out.write(f"> {header}\n\n")
        out.write(f"> Snapshot generado desde la rama `{branch}`. Contiene el **código completo** de cada archivo.\n\n")
        out.write("---\n\n")
        for f in files:
            lang = code_lang(f)
            out.write(f"```{lang}\n// {f}\n")
            with open(f, "r", encoding="utf-8", errors="ignore") as fh:
                out.write(fh.read())
            out.write("\n```\n\n")

def _ts_madrid() -> str:
    now = datetime.utcnow()
    if ZoneInfo:
        try:
            now = datetime.now(ZoneInfo("Europe/Madrid"))
        except Exception:
            pass
    return now.strftime("%Y-%m-%d %H:%M")

def write_urls_index(snapshot_dir: Path, out_md: Path, repo: str, branch: str):
    """
    Genera docs/ai-snapshots-urls.md con una lista de TODOS los .md dentro de docs/ai-snapshots/
    (excluyendo el propio índice). Incluye enlaces blob y raw.
    """
    md_files = sorted(snapshot_dir.rglob("*.md"))
    md_files = [p for p in md_files if p.name != out_md.name]  # excluir el propio índice si estuviera dentro
    lines = []
    lines.append("# Snapshot AI — docs/ai-snapshots-urls.md\n")
    lines.append(f"_Última generación: {_ts_madrid()}_\n")
    lines.append(f"Repositorio: `{repo}` — Rama: `{branch}`\n")
    lines.append(f"Total de snapshots: **{len(md_files)}**\n")
    lines.append("---\n")
    if not md_files:
        lines.append("> (No se encontraron snapshots en `docs/ai-snapshots/`)\n")
    else:
        for p in md_files:
            rel = p.as_posix()
            label = p.relative_to(snapshot_dir).as_posix()
            blob = f"https://github.com/{repo}/blob/{branch}/{rel}"
            raw  = f"https://raw.githubusercontent.com/{repo}/{branch}/{rel}"
            lines.append(f"- [{label}]({blob}) — [raw]({raw})")
    out_md.write_text("\n".join(lines), encoding="utf-8")
    print(f"Wrote {out_md} with {len(md_files)} entries.")

def main():
    cfg_path = "docs/ai-snapshots.json"
    if len(sys.argv) > 1:
        cfg_path = sys.argv[1]
    if not os.path.exists(cfg_path):
        print(f"Config not found: {cfg_path}", file=sys.stderr)
        sys.exit(1)

    with open(cfg_path, "r", encoding="utf-8") as f:
        cfg = json.load(f)

    branch = os.environ.get("GITHUB_REF_NAME", cfg.get("branch_env_fallback", "develop"))
    repo = os.environ.get("GITHUB_REPOSITORY", "screenleads/sl-dev-backend")  # owner/repo

    total_files = 0
    for snap in cfg.get("snapshots", []):
        output = snap["output"]
        title = snap.get("title", f"{cfg.get('title_prefix','Snapshot AI')} — {output}")
        header = snap.get("header", "")
        globs_list = snap.get("globs", [])
        excludes = snap.get("excludes", [])
        files = match_files(globs_list, excludes)
        write_snapshot(output, title, header, files, branch)
        total_files += len(files)
        print(f"Wrote {output} with {len(files)} file(s).")

    # === NUEVO: generar índice de URLs de snapshots ===
    snapshot_dir = Path("docs") / "ai-snapshots"
    out_md = Path("docs") / "ai-snapshots-urls.md"
    write_urls_index(snapshot_dir, out_md, repo, branch)

    print(f"Done. Total files included across snapshots: {total_files}")

if __name__ == "__main__":
    main()
