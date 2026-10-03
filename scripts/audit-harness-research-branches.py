#!/usr/bin/env python3
"""Audita pesquisas e, quando solicitado, remove apenas branches ancestrais da main."""

import argparse
import json
from pathlib import Path
import re
import subprocess
from urllib.parse import quote, urlencode


RESEARCH_PATH = re.compile(
    r"^pesquisas/[a-z0-9]+(?:-[a-z0-9]+)*/(?:"
    r"[A-Za-z0-9][A-Za-z0-9._-]*\.md|"
    r"cards/[A-Za-z0-9][A-Za-z0-9._-]*\.json|"
    r"cards/fontes/[A-Za-z0-9][A-Za-z0-9._-]*\.(?:md|sha256))$"
)
RESEARCH_PREFIXES = ("automation/", "radar-", "radar/")


def is_research_branch(branch):
    """Reconhece as três famílias reais de branches dos produtores de pesquisa."""
    return branch.startswith(RESEARCH_PREFIXES)


def git(repository, *arguments, check=True):
    """Executa Git por argumentos sem interpolar nomes de branch no shell."""
    return subprocess.run(
        ["git", "-C", str(repository), *arguments],
        check=check, capture_output=True, text=True,
    )


def inventory(repository, base):
    """Compara pontas de pesquisa com a base e explicita conflitos de conteúdo e escopo."""
    if git(repository, "rev-parse", "--is-shallow-repository").stdout.strip() == "true":
        raise RuntimeError("Histórico raso: complete o fetch antes de auditar ou remover branches.")
    base_sha = git(repository, "rev-parse", "--verify", f"{base}^{{commit}}").stdout.strip()
    refs = git(repository, "for-each-ref", "--format=%(refname)", "refs/remotes/origin/")
    branches = []
    for ref in sorted(refs.stdout.splitlines()):
        branch = ref.removeprefix("refs/remotes/origin/")
        if not is_research_branch(branch):
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


class GitHub:
    """Consulta proteções, PRs e execuções sem carregar credenciais nos argumentos."""

    def __init__(self, repository):
        if not re.fullmatch(r"[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+", repository):
            raise ValueError("Informe --github-repository no formato proprietário/repositório.")
        self.repository = repository

    def request(self, path, paginate=False):
        """Obtém JSON pela identidade autenticada do gh, incluindo todas as páginas."""
        command = ["gh", "api", f"repos/{self.repository}{path}"]
        if paginate:
            command.extend(["--paginate", "--slurp"])
        result = subprocess.run(command, check=True, capture_output=True, text=True)
        return json.loads(result.stdout)


def remote_head(repository, branch):
    """Confere a referência remota exata e distingue ausência de falha de acesso."""
    result = git(repository, "ls-remote", "--exit-code", "--heads", "origin",
                 f"refs/heads/{branch}", check=False)
    if result.returncode == 2:
        return None
    if result.returncode:
        raise RuntimeError("Não foi possível consultar a referência remota; nenhuma remoção autorizada.")
    return result.stdout.split()[0]


def prune_integrated(repository, report, github):
    """Remove com lease somente pontas integradas, sem PR aberto nem execução em curso."""
    # A identidade do destino precisa corresponder à API usada para conferir as proteções.
    origin = git(repository, "config", "--get", "remote.origin.url").stdout.strip()
    expected = re.escape(github.repository)
    if not re.fullmatch(rf"(?:https://(?:[^/@]+@)?github\.com/|git@github\.com:){expected}(?:\.git)?/?", origin):
        raise RuntimeError("Origin não corresponde ao repositório GitHub informado; limpeza bloqueada.")
    metadata = github.request("")
    default_branch = metadata["default_branch"]
    if default_branch != "main":
        raise RuntimeError("A limpeza exige main como branch padrão; revisão do contrato necessária.")
    for entry in report["branches"]:
        if entry["status"] != "INTEGRATED" or not is_research_branch(entry["branch"]):
            continue
        cleanup = entry["cleanup"] = {"status": "RETAINED", "reason": ""}
        branch, sha = entry["branch"], entry["sha"]
        try:
            # Revalida a ancestralidade; igualdade de conteúdo não preserva sozinha o histórico.
            ancestor = git(repository, "merge-base", "--is-ancestor", sha, report["baseSha"], check=False)
            if branch == default_branch or ancestor.returncode != 0:
                cleanup["reason"] = "A ponta não é ancestral comprovada da main."
                continue
            if remote_head(repository, default_branch) != report["baseSha"]:
                cleanup["reason"] = "A main mudou após a auditoria; aguardar nova conferência."
                continue
            observed = remote_head(repository, branch)
            if observed is None:
                cleanup.update(status="ALREADY_REMOVED", reason="A referência já foi removida.")
                continue
            if observed != sha:
                cleanup["reason"] = "A branch recebeu novos commits; preservar a nova ponta."
                continue
            branch_info = github.request(f"/branches/{quote(branch, safe='')}")
            if branch_info["protected"] or branch_info["commit"]["sha"] != sha:
                cleanup["reason"] = "Branch protegida ou alterada durante a conferência."
                continue
            query = urlencode({"state": "open", "head": f"{github.repository.split('/')[0]}:{branch}", "per_page": 100})
            if any(github.request(f"/pulls?{query}", paginate=True)):
                cleanup["reason"] = "Existe PR aberto; preservar sua branch."
                continue
            query = urlencode({"branch": branch, "per_page": 100})
            pages = github.request(f"/actions/runs?{query}", paginate=True)
            if any(run["status"] != "completed" for page in pages for run in page["workflow_runs"]):
                cleanup["reason"] = "Existe workflow em curso ou aguardando ação; preservar a branch."
                continue
            # O lease torna a exclusão condicional ao SHA auditado, inclusive numa corrida de push.
            result = git(repository, "push", f"--force-with-lease=refs/heads/{branch}:{sha}",
                         "origin", f":refs/heads/{branch}", check=False)
            if result.returncode:
                if remote_head(repository, branch) != sha:
                    cleanup["reason"] = "A referência mudou antes da exclusão; lease preservou o trabalho."
                else:
                    cleanup.update(status="ERROR", reason="Falha ao remover a referência integrada; conferir acesso e proteção.")
                continue
            cleanup.update(status="DELETED", reason="Ponta preservada como ancestral da main; referência removida com lease.")
        except (subprocess.CalledProcessError, RuntimeError, KeyError, ValueError):
            cleanup.update(status="ERROR", reason="Falha na conferência Git/GitHub; remoção bloqueada por segurança.")
    return report


def write_summary(report, destination):
    """Publica um resumo legível com identidade da ponta e motivo da pendência."""
    lines = ["## Integração das pesquisas do harness", "", f"Base: `{report['baseSha']}`", "",
             "| Branch | SHA | Situação | Limpeza | Motivo |", "| --- | --- | --- | --- | --- |"]
    for entry in report["branches"]:
        reason = entry.get("reason", "")
        if entry.get("concurrentPaths"):
            reason += " Conteúdo também alterado na main; reconciliar antes do merge."
        cleanup = entry.get("cleanup", {})
        if cleanup:
            reason += " " + cleanup["reason"]
        values = [entry["branch"], entry["sha"], entry["status"], cleanup.get("status", "Não solicitada"), reason]
        lines.append("| " + " | ".join(value.replace("|", "\\|") for value in values) + " |")
    with Path(destination).open("a") as target:
        target.write("\n".join(lines) + "\n")


def main():
    """Grava a auditoria, limpa somente quando autorizado e nunca realiza merges."""
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--repository", type=Path, default=Path(__file__).resolve().parent.parent)
    parser.add_argument("--base", default="origin/main")
    parser.add_argument("--output", type=Path)
    parser.add_argument("--summary", type=Path)
    parser.add_argument("--fail-pending", action="store_true")
    parser.add_argument("--prune-integrated", action="store_true",
                        help="Remove somente ancestrais da main sem PR aberto ou workflow em curso.")
    parser.add_argument("--github-repository", help="Repositório para verificar proteções, PRs e workflows.")
    args = parser.parse_args()
    if args.prune_integrated and not args.github_repository:
        parser.error("--prune-integrated exige --github-repository")
    report = inventory(args.repository, args.base)
    if args.prune_integrated:
        prune_integrated(args.repository, report, GitHub(args.github_repository))
    payload = json.dumps(report, ensure_ascii=False, indent=2) + "\n"
    if args.output:
        args.output.write_text(payload)
    else:
        print(payload, end="")
    if args.summary:
        write_summary(report, args.summary)
    unresolved = sum(entry["status"] in ("PENDING", "BLOCKED") for entry in report["branches"])
    print(f"Branches de pesquisa: {len(report['branches'])}; pendências: {unresolved}.")
    errors = sum(entry.get("cleanup", {}).get("status") == "ERROR" for entry in report["branches"])
    deleted = sum(entry.get("cleanup", {}).get("status") == "DELETED" for entry in report["branches"])
    if args.prune_integrated:
        print(f"Branches integradas removidas: {deleted}; erros de limpeza: {errors}.")
    if errors:
        raise SystemExit(1)
    if args.fail_pending and unresolved:
        raise SystemExit(1)


if __name__ == "__main__":
    main()
