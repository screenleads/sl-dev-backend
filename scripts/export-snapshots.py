#!/usr/bin/env python3
import os, sys, json, glob

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
    ".sh": "bash"
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

    print(f"Done. Total files included: {total_files}")

if __name__ == "__main__":
    main()
