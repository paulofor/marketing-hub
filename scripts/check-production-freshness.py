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

DEPLOY_WORKFLOW = "Build & Deploy containers"
TARGET_KEYS = {"app": "app_deploy", "frontend": "frontend"}


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
    url: str


def live_deploys(payload: dict[str, Any], now: datetime, max_minutes: int) -> list[LiveDeploy]:
    result: list[LiveDeploy] = []
    for run in payload.get("workflow_runs", []):
        if run.get("name") != DEPLOY_WORKFLOW or run.get("head_branch") != "main":
            continue
        if run.get("status") == "completed":
            continue
        created = parse_time(run.get("created_at"))
        sha = run.get("head_sha")
        if created is None or not isinstance(sha, str):
            continue
        if minutes_between(created, now) > max_minutes:
            continue
        result.append(
            LiveDeploy(
                id=int(run["id"]),
                sha=sha,
                status=str(run.get("status", "unknown")),
                created_at=created,
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

    low, high = 0, len(commits) - 1
    while low < high:
        mid = (low + high) // 2
        if detector.changed(base, commits[mid], target):
            high = mid
        else:
            low = mid + 1
    return commits[low]


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
                reason="revisão de produção ausente/não verificável, mas há deploy da main atual em andamento",
                deploy_run_id=run.id,
                deploy_url=run.url,
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
                reason="revisão observada diverge da main, com deploy da main atual em andamento",
                deploy_run_id=run.id,
                deploy_url=run.url,
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
            reason="há deploy em andamento que contém a primeira mudança pendente",
            deploy_run_id=run.id,
            deploy_url=run.url,
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


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--head", default="HEAD")
    parser.add_argument("--app-revision", required=True)
    parser.add_argument("--frontend-revision", required=True)
    parser.add_argument("--runs-json", required=True)
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
        ]
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
                    "url": run.url,
                }
                for run in deploys
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
