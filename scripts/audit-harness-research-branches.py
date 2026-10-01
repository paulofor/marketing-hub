#!/usr/bin/env python3
"""Inventaria entregas de pesquisa esquecidas em branches sem alterar Git ou produção."""

import argparse
import json
from pathlib import Path
import re
import subprocess


RESEARCH_PATH = re.compile(
    r"^pesquisas/[a-z0-9]+(?:-[a-z0-9]+)*/(?:"
    r"[A-Za-z0-9][A-Za-z0-9._-]*\.md|"
    r"cards/[A-Za-z0-9][A-Za-z0-9._-]*\.json|"
    r"cards/fontes/[A-Za-z0-9][A-Za-z0-9._-]*\.(?:md|sha256))$"
)


def git(repository, *arguments, check=True):
    """Executa Git por argumentos sem interpolar nomes de branch no shell."""
    return subprocess.run(
        ["git", "-C", str(repository), *arguments],
        check=check, capture_output=True, text=True,
    )


def inventory(repository, base):
    """Compara pontas de pesquisa com a base e explicita conflitos de conteúdo e escopo."""
    base_sha = git(repository, "rev-parse", "--verify", f"{base}^{{commit}}").stdout.strip()
    refs = git(repository, "for-each-ref", "--format=%(refname)", "refs/remotes/origin/")
    branches = []
    for ref in sorted(refs.stdout.splitlines()):
        branch = ref.removeprefix("refs/remotes/origin/")
        if not branch.startswith(("automation/", "radar-")):
            continue
        sha = git(repository, "rev-parse", ref).stdout.strip()
        entry = {"branch": branch, "sha": sha, "status": "INTEGRATED", "paths": []}
        ancestor = git(repository, "merge-base", "--is-ancestor", ref, base_sha, check=False)
        if ancestor.returncode not in (0, 1):
            raise RuntimeError(f"Não foi possível conferir ancestralidade de {branch}")
        if ancestor.returncode == 0:
            branches.append(entry)
            continue
        common = git(repository, "merge-base", base_sha, ref, check=False)
        if common.returncode:
            entry.update(status="BLOCKED", reason="Histórico comum ausente; conferir fetch-depth e origem.")
            branches.append(entry)
            continue
        common_sha = common.stdout.strip()
        paths = git(repository, "diff", "--name-only", "-z", f"{common_sha}..{ref}")
        entry["paths"] = paths.stdout.rstrip("\0").split("\0") if paths.stdout else []
        unsafe = [path for path in entry["paths"] if not RESEARCH_PATH.fullmatch(path)]
        if unsafe:
            entry.update(status="BLOCKED", reason="Branch mistura pesquisa com código ou arquivos temporários.", unsafePaths=unsafe)
        elif not entry["paths"]:
            entry["status"] = "INCORPORATED"
        else:
            same = git(repository, "diff", "--quiet", base_sha, ref, "--", *entry["paths"], check=False)
            if same.returncode not in (0, 1):
                raise RuntimeError(f"Não foi possível comparar conteúdo de {branch}")
            entry["status"] = "INCORPORATED" if same.returncode == 0 else "PENDING"
            changed = git(repository, "diff", "--name-only", "-z", common_sha, base_sha, "--", *entry["paths"])
            entry["concurrentPaths"] = changed.stdout.rstrip("\0").split("\0") if changed.stdout else []
            tree = git(repository, "ls-tree", "-r", ref, "--", *entry["paths"])
            if any(line.startswith("120000 ") for line in tree.stdout.splitlines()):
                entry.update(status="BLOCKED", reason="Pesquisa contém link simbólico; revisão de segurança necessária.")
        branches.append(entry)
    return {"baseSha": base_sha, "branches": branches}


def write_summary(report, destination):
    """Publica um resumo legível com identidade da ponta e motivo da pendência."""
    lines = ["## Integração das pesquisas do harness", "", f"Base: `{report['baseSha']}`", "",
             "| Branch | SHA | Situação | Motivo |", "| --- | --- | --- | --- |"]
    for entry in report["branches"]:
        reason = entry.get("reason", "")
        if entry.get("concurrentPaths"):
            reason += " Conteúdo também alterado na main; reconciliar antes do merge."
        values = [entry["branch"], entry["sha"], entry["status"], reason]
        lines.append("| " + " | ".join(value.replace("|", "\\|") for value in values) + " |")
    with Path(destination).open("a") as target:
        target.write("\n".join(lines) + "\n")


def main():
    """Grava a auditoria e falha quando há trabalho pendente, sem realizar merges."""
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--repository", type=Path, default=Path(__file__).resolve().parent.parent)
    parser.add_argument("--base", default="origin/main")
    parser.add_argument("--output", type=Path)
    parser.add_argument("--summary", type=Path)
    parser.add_argument("--fail-pending", action="store_true")
    args = parser.parse_args()
    report = inventory(args.repository, args.base)
    payload = json.dumps(report, ensure_ascii=False, indent=2) + "\n"
    if args.output:
        args.output.write_text(payload)
    else:
        print(payload, end="")
    if args.summary:
        write_summary(report, args.summary)
    unresolved = sum(entry["status"] in ("PENDING", "BLOCKED") for entry in report["branches"])
    print(f"Branches de pesquisa: {len(report['branches'])}; pendências: {unresolved}.")
    if args.fail_pending and unresolved:
        raise SystemExit(1)


if __name__ == "__main__":
    main()
