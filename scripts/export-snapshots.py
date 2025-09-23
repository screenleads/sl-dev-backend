#!/usr/bin/env python3
# -*- coding: utf-8 -*-
import os, sys, json, glob
from pathlib import Path
from datetime import datetime

try:
    from zoneinfo import ZoneInfo  # py>=3.9
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
    return LANG_BY_EXT.get(Path(path).suffix.lower(), "")

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

def write_urls_index(md_files, out_md: Path, repo: str, branch: str, base_dir: Path):
    """
    Genera docs/ai-snapshots-urls.md con enlaces a TODOS los .md debajo de docs/
    (o los definidos por globs), excluyendo el propio índice.
    """
    md_files = [Path(f) for f in md_files]
    md_files = [p for p in md_files if p.resolve() != out_md.resolve()]
    md_files.sort(key=lambda p: p.as_posix())

    lines = []
    lines.append("# Snapshot AI — docs/ai-snapshots-urls.md\n")
    lines.append(f"_Última generación: {_ts_madrid()}_\n")
    lines.append(f"Repositorio: `{repo}` — Rama: `{branch}`\n")
    lines.append(f"Total de archivos: **{len(md_files)}**\n")
    lines.append("---\n")

    if not md_files:
        lines.append("> (No se encontraron archivos Markdown en `docs/`)\n")
    else:
        for p in md_files:
            try:
                label = p.relative_to(base_dir).as_posix()
            except ValueError:
                label = p.as_posix()
            rel = p.as_posix().replace("\\", "/")
            blob = f"https://github.com/{repo}/blob/{branch}/{rel}"
            raw  = f"https://raw.githubusercontent.com/{repo}/{branch}/{rel}"
            lines.append(f"- [{label}]({blob}) — [raw]({raw})")

    out_md.write_text("\n".join(lines) + "\n", encoding="utf-8")
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

    # === Índice de URLs en docs/ ===
    urls_cfg = cfg.get("urls_index", {})
    base_dir = Path(urls_cfg.get("base_dir", "docs"))
    globs_list = urls_cfg.get("globs", [str(base_dir / "*.md"), str(base_dir / "**/*.md")])
    excludes = urls_cfg.get("excludes", [str(base_dir / "ai-snapshots-urls.md")])

    md_files = match_files(globs_list, excludes)
    out_md = base_dir / "ai-snapshots-urls.md"
    write_urls_index(md_files, out_md, repo, branch, base_dir)

    print(f"Done. Total files included across snapshots: {total_files}")

if __name__ == "__main__":
    main()
