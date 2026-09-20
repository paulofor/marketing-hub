#!/usr/bin/env python3
"""Detecta quando a produção fica atrás de mudanças da main que exigem deploy."""

from __future__ import annotations

import argparse
from dataclasses import dataclass
from datetime import datetime, timezone
import json
from pathlib import Path
import subprocess
import tempfile
from typing import Any

from musa_pde_watchdog import publication_changed_for_frontend, supported_surfaces_at

DEPLOY_WORKFLOW = "Build & Deploy containers"
CUSTOMER_AGENT_DEPLOY_WORKFLOW = "Customer Agent Worker CI/CD"
MUSA_PDE_DEPLOY_WORKFLOW = "CI - PDE Platform Metodo MUSA"
TARGET_KEYS = {"app": "app_deploy", "frontend": "frontend", "psique": "customer_agent"}


def parse_time(value: str | None) -> datetime | None:
    if not value:
        return None
    return datetime.fromisoformat(value.replace("Z", "+00:00"))


def minutes_between(start: datetime, end: datetime) -> float:
    return max(0.0, (end - start).total_seconds() / 60.0)


class GitRepo:
    def __init__(self, root: Path):
        self.root = root

    def run(self, *args: str, check: bool = True) -> subprocess.CompletedProcess[str]:
        return subprocess.run(
            ["git", *args], cwd=self.root, text=True, capture_output=True, check=check
        )

    def resolve(self, ref: str) -> str:
        return self.run("rev-parse", ref).stdout.strip()

    def exists(self, sha: str) -> bool:
        return self.run("cat-file", "-e", f"{sha}^{{commit}}", check=False).returncode == 0

    def is_ancestor(self, older: str, newer: str) -> bool:
        return self.run("merge-base", "--is-ancestor", older, newer, check=False).returncode == 0

    def commits_between(self, base: str, head: str) -> list[str]:
        result = self.run("rev-list", "--reverse", f"{base}..{head}").stdout.strip()
        return result.splitlines() if result else []

    def commit_time(self, sha: str) -> datetime:
        value = self.run("show", "-s", "--format=%cI", sha).stdout.strip()
        parsed = parse_time(value)
        if parsed is None:
            raise ValueError(f"Commit sem horário: {sha}")
        return parsed


class DeploymentDetector:
    """Reutiliza o classificador canônico do deploy para evitar regras paralelas."""

    def __init__(self, root: Path):
        self.root = root
        self.script = root / "scripts" / "detect-deployment-changes.sh"

    def changed(self, base: str, head: str, target: str) -> bool:
        if target.startswith("musa_pde:"):
            frontend_version = target.split(":", 1)[1]
            return publication_changed_for_frontend(
                self.root, base, head, frontend_version
            )
        if target not in TARGET_KEYS:
            raise ValueError(f"Target desconhecido: {target}")
        with tempfile.NamedTemporaryFile(prefix="freshness-", delete=False) as stream:
            output = Path(stream.name)
        try:
            result = subprocess.run(
                ["bash", str(self.script), base, head, str(output), base, base],
                cwd=self.root,
                text=True,
                capture_output=True,
                check=False,
            )
            if result.returncode != 0:
                raise RuntimeError(
                    "Falha ao classificar mudanças de deploy: "
                    + (result.stderr.strip() or result.stdout.strip() or f"exit {result.returncode}")
                )
            values: dict[str, str] = {}
            for line in output.read_text().splitlines():
                if "=" in line:
                    key, value = line.split("=", 1)
                    values[key] = value
            key = TARGET_KEYS[target]
            if values.get(key) not in {"true", "false"}:
                raise RuntimeError(f"Classificador não produziu {key}=true|false")
            return values[key] == "true"
        finally:
            output.unlink(missing_ok=True)


@dataclass
class LiveDeploy:
    id: int
    sha: str
    status: str
    created_at: datetime
    last_progress_at: datetime
    url: str


def live_deploys(
    payload: dict[str, Any], now: datetime, max_minutes: int, workflow: str = DEPLOY_WORKFLOW
) -> list[LiveDeploy]:
    workflow_runs: list[tuple[dict[str, Any], datetime, datetime]] = []
    for run in payload.get("workflow_runs", []):
        if run.get("name") != workflow or run.get("head_branch") != "main":
            continue
        created = parse_time(run.get("created_at"))
        if created is None:
            continue
        updated = parse_time(run.get("updated_at")) or created
        workflow_runs.append((run, created, updated))

    result: list[LiveDeploy] = []
    for run, created, updated in workflow_runs:
        if run.get("status") == "completed":
            continue
        sha = run.get("head_sha")
        if not isinstance(sha, str):
            continue

        # queue:max preserva todos os SHAs e pode manter o run do HEAD aguardando por mais
        # tempo que a janela. Uma revisão anterior concluída recentemente comprova que a fila
        # continua avançando; uma execução antiga sem esse pulso continua sendo descartada.
        last_progress = max(
            progress
            for _other, other_created, progress in workflow_runs
            if other_created <= created
        )
        if minutes_between(max(updated, last_progress), now) > max_minutes:
            continue
        result.append(
            LiveDeploy(
                id=int(run["id"]),
                sha=sha,
                status=str(run.get("status", "unknown")),
                created_at=created,
                last_progress_at=max(updated, last_progress),
                url=str(run.get("html_url", "")),
            )
        )
    return result


def find_first_relevant_change(
    repo: GitRepo, detector: DeploymentDetector, base: str, head: str, target: str
) -> str | None:
    commits = repo.commits_between(base, head)
    if not commits or not detector.changed(base, head, target):
        return None

    # git diff base..commit pode voltar a ficar limpo após um revert. Por isso não usamos
    # busca binária: acompanhamos as transições e medimos a idade do intervalo pendente atual.
    pending_since = None
    for commit in commits:
        changed = detector.changed(base, commit, target)
        if changed and pending_since is None:
            pending_since = commit
        elif not changed:
            pending_since = None
    return pending_since


def covering_deploy(
    repo: GitRepo,
    detector: DeploymentDetector,
    deploys: list[LiveDeploy],
    first_change: str | None,
    head: str,
    target: str,
) -> LiveDeploy | None:
    for run in sorted(deploys, key=lambda value: value.created_at, reverse=True):
        if not repo.exists(run.sha) or not repo.is_ancestor(run.sha, head):
            continue
        if first_change is None:
            if run.sha == head:
                return run
            continue
        if not repo.is_ancestor(first_change, run.sha):
            continue
        # O run só cobre o target se não existir outra mudança relevante depois do SHA dele.
        if run.sha == head or not detector.changed(run.sha, head, target):
            return run
    return None


def evaluate_target(
    *,
    target: str,
    observed_revision: str,
    head: str,
    repo: GitRepo,
    detector: DeploymentDetector,
    deploys: list[LiveDeploy],
    now: datetime,
    grace_minutes: int,
) -> dict[str, Any]:
    base = (observed_revision or "").strip()
    result: dict[str, Any] = {"target": target, "observed_revision": base, "head": head}

    if not base or base in {"MISSING", "UNKNOWN"} or not repo.exists(base):
        run = covering_deploy(repo, detector, deploys, None, head, target)
        if run:
            result.update(
                status="DEPLOYING",
                reason="revisão de produção ausente/não verificável, mas há deploy da main atual na fila ou em andamento",
                deploy_run_id=run.id,
                deploy_url=run.url,
                deploy_last_progress_at=run.last_progress_at.isoformat(),
            )
        else:
            result.update(
                status="STALE",
                reason="revisão de produção ausente ou não verificável no histórico Git",
            )
        return result

    if not repo.is_ancestor(base, head):
        run = covering_deploy(repo, detector, deploys, None, head, target)
        if run:
            result.update(
                status="DEPLOYING",
                reason="revisão observada diverge da main, com deploy da main atual na fila ou em andamento",
                deploy_run_id=run.id,
                deploy_url=run.url,
                deploy_last_progress_at=run.last_progress_at.isoformat(),
            )
        else:
            result.update(status="STALE", reason="revisão observada não é ancestral da main atual")
        return result

    first_change = find_first_relevant_change(repo, detector, base, head, target)
    if first_change is None:
        result.update(
            status="CURRENT",
            reason="nenhuma mudança pendente que exija deploy deste target",
        )
        return result

    changed_at = repo.commit_time(first_change)
    stale_minutes = minutes_between(changed_at, now)
    result.update(
        first_pending_commit=first_change,
        first_pending_at=changed_at.isoformat(),
        stale_minutes=round(stale_minutes, 1),
    )

    run = covering_deploy(repo, detector, deploys, first_change, head, target)
    if run:
        result.update(
            status="DEPLOYING",
            reason="há deploy na fila ou em andamento que contém a primeira mudança pendente",
            deploy_run_id=run.id,
            deploy_url=run.url,
            deploy_last_progress_at=run.last_progress_at.isoformat(),
        )
    elif stale_minutes <= grace_minutes:
        result.update(
            status="GRACE",
            reason=f"mudança relevante ainda dentro da tolerância de {grace_minutes} minutos",
        )
    else:
        result.update(
            status="STALE",
            reason=f"mudança relevante aguarda produção há mais de {grace_minutes} minutos sem deploy válido em andamento",
        )
    return result


def overall(targets: list[dict[str, Any]]) -> str:
    statuses = {target["status"] for target in targets}
    if "STALE" in statuses:
        return "STALE"
    if statuses & {"DEPLOYING", "GRACE"}:
        return "PENDING"
    return "FRESH"


def musa_surface_targets(
    report: dict[str, Any], repository_root: Path, head: str
) -> list[dict[str, str]]:
    """Valida o relatório público contra todas as versões suportadas no inventário."""

    expected = supported_surfaces_at(repository_root, head)
    reported = report.get("surfaces")
    if not isinstance(reported, list):
        raise ValueError("Relatório MUSA não contém a lista de superfícies")
    by_target: dict[str, dict[str, Any]] = {}
    for item in reported:
        if not isinstance(item, dict) or not isinstance(item.get("target"), str):
            raise ValueError("Relatório MUSA contém superfície inválida")
        if item["target"] in by_target:
            raise ValueError(f"Relatório MUSA duplicou a superfície {item['target']}")
        by_target[item["target"]] = item
    expected_targets = {item.target for item in expected}
    if set(by_target) != expected_targets:
        raise ValueError(
            "Relatório MUSA não cobre exatamente as superfícies suportadas: "
            f"esperadas={sorted(expected_targets)}, observadas={sorted(by_target)}"
        )
    return [
        {
            "target": f"musa_pde:{expectation.target}",
            "surface": expectation.target,
            "observed_revision": (
                str(by_target[expectation.target].get("revision", "MISSING"))
                if by_target[expectation.target].get("status") == "UP"
                else "MISSING"
            ),
            "probe_status": str(by_target[expectation.target].get("status", "ERROR")),
            "probe_error": str(by_target[expectation.target].get("error", "")),
            "public_url": expectation.public_url,
        }
        for expectation in expected
    ]


def attach_musa_probe(
    evaluated: dict[str, Any], surface: dict[str, str]
) -> dict[str, Any]:
    """Impede que um deploy de outra versão esconda uma sonda pública com falha."""

    evaluated.update(
        surface=surface["surface"],
        public_url=surface["public_url"],
        probe_status=surface["probe_status"],
    )
    if surface["probe_status"] != "UP":
        evaluated["status"] = "STALE"
        evaluated["probe_error"] = surface["probe_error"]
        evaluated["reason"] = (
            f"sonda pública da superfície {surface['surface']} falhou: "
            f"{surface['probe_error'] or 'motivo não informado'}"
        )
    return evaluated


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--head", default="HEAD")
    parser.add_argument("--app-revision", required=True)
    parser.add_argument("--frontend-revision", required=True)
    parser.add_argument("--psique-revision", required=True)
    parser.add_argument("--musa-pde-report-json", required=True)
    parser.add_argument("--runs-json", required=True)
    parser.add_argument("--psique-runs-json", required=True)
    parser.add_argument("--musa-pde-runs-json", required=True)
    parser.add_argument("--grace-minutes", type=int, default=30)
    parser.add_argument("--max-deploy-minutes", type=int, default=75)
    parser.add_argument("--now", help="ISO-8601; usado por testes e auditoria")
    parser.add_argument("--output")
    args = parser.parse_args()

    root = Path(__file__).resolve().parent.parent
    repo = GitRepo(root)
    detector = DeploymentDetector(root)
    now = parse_time(args.now) if args.now else datetime.now(timezone.utc)
    if now is None:
        raise ValueError("Horário inválido")
    if args.grace_minutes <= 0 or args.max_deploy_minutes <= 0:
        raise ValueError("Janelas de tempo devem ser positivas")

    try:
        head = repo.resolve(args.head)
        payload = json.loads(Path(args.runs_json).read_text())
        deploys = live_deploys(payload, now, args.max_deploy_minutes)
        psique_payload = json.loads(Path(args.psique_runs_json).read_text())
        psique_deploys = live_deploys(
            psique_payload, now, args.max_deploy_minutes, CUSTOMER_AGENT_DEPLOY_WORKFLOW
        )
        musa_pde_payload = json.loads(Path(args.musa_pde_runs_json).read_text())
        musa_pde_deploys = live_deploys(
            musa_pde_payload, now, args.max_deploy_minutes, MUSA_PDE_DEPLOY_WORKFLOW
        )
        targets = [
            evaluate_target(
                target="app",
                observed_revision=args.app_revision,
                head=head,
                repo=repo,
                detector=detector,
                deploys=deploys,
                now=now,
                grace_minutes=args.grace_minutes,
            ),
            evaluate_target(
                target="frontend",
                observed_revision=args.frontend_revision,
                head=head,
                repo=repo,
                detector=detector,
                deploys=deploys,
                now=now,
                grace_minutes=args.grace_minutes,
            ),
            evaluate_target(
                target="psique",
                observed_revision=args.psique_revision,
                head=head,
                repo=repo,
                detector=detector,
                deploys=psique_deploys,
                now=now,
                grace_minutes=args.grace_minutes,
            ),
        ]
        musa_report = json.loads(Path(args.musa_pde_report_json).read_text())
        for surface in musa_surface_targets(musa_report, root, args.head):
            evaluated = evaluate_target(
                target=surface["target"],
                observed_revision=surface["observed_revision"],
                head=head,
                repo=repo,
                detector=detector,
                deploys=musa_pde_deploys,
                now=now,
                grace_minutes=args.grace_minutes,
            )
            targets.append(attach_musa_probe(evaluated, surface))
        document = {
            "checked_at": now.isoformat(),
            "head": head,
            "grace_minutes": args.grace_minutes,
            "max_deploy_minutes": args.max_deploy_minutes,
            "overall_status": overall(targets),
            "targets": targets,
            "live_deploys": [
                {
                    "id": run.id,
                    "sha": run.sha,
                    "status": run.status,
                    "created_at": run.created_at.isoformat(),
                    "last_progress_at": run.last_progress_at.isoformat(),
                    "url": run.url,
                }
                for run in deploys
            ],
            "live_psique_deploys": [
                {
                    "id": run.id,
                    "sha": run.sha,
                    "status": run.status,
                    "created_at": run.created_at.isoformat(),
                    "last_progress_at": run.last_progress_at.isoformat(),
                    "url": run.url,
                }
                for run in psique_deploys
            ],
            "live_musa_pde_deploys": [
                {
                    "id": run.id,
                    "sha": run.sha,
                    "status": run.status,
                    "created_at": run.created_at.isoformat(),
                    "last_progress_at": run.last_progress_at.isoformat(),
                    "url": run.url,
                }
                for run in musa_pde_deploys
            ],
        }
        exit_code = 1 if document["overall_status"] == "STALE" else 0
    except Exception as error:  # diagnóstico deve virar alerta, não desaparecer num traceback opaco
        document = {
            "checked_at": now.isoformat(),
            "overall_status": "ERROR",
            "error": str(error),
        }
        exit_code = 2

    rendered = json.dumps(document, ensure_ascii=False, indent=2)
    print(rendered)
    if args.output:
        Path(args.output).write_text(rendered + "\n")
    return exit_code


if __name__ == "__main__":
    raise SystemExit(main())
