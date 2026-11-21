"""Report public SSH fingerprints on the authorized GitHub runner."""

import base64
import binascii
import hashlib
import os
from pathlib import Path
import subprocess
import tempfile


NAMES = (
    "VPS_SSH_CHAVE",
    "VPS_SSH_KEY",
    "SSH_PRIVATE_KEY",
    "GROWTH_OPERATOR_VPS_SSH_KEY",
    "PDE_PLATFORM_VPS_SSH_KEY",
    "LEAD_PORTAL_PAYMENTS_VPS_SSH_KEY",
)
CONTEXT = {
    "GITHUB_ACTIONS": "true",
    "GITHUB_REPOSITORY": "paulofor/marketing-hub",
    "GITHUB_REF": "refs/heads/main",
    "GITHUB_EVENT_NAME": "workflow_dispatch",
    "GITHUB_ACTOR": "paulofor",
    "GITHUB_TRIGGERING_ACTOR": "paulofor",
    "GITHUB_WORKFLOW_REF": (
        "paulofor/marketing-hub/.github/workflows/"
        "audit-ssh-credential-fingerprints.yml@refs/heads/main"
    ),
}
MISSING = "não disponível no job"
INVALID = "não identificada"


def inspect_key(value, workdir, run=subprocess.run):
    if not value.strip():
        return MISSING
    try:
        data = (value.replace("\r\n", "\n").rstrip("\n") + "\n").encode("utf-8")
        if len(data) > 65536:
            return INVALID
        with tempfile.TemporaryDirectory(dir=workdir) as tmp:
            path = Path(tmp) / "identity"
            fd = os.open(path, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600)
            with os.fdopen(fd, "wb") as key:
                key.write(data)
            # Do not pass audit secrets to the child environment or command line.
            child = {k: os.environ[k] for k in ("PATH", "SystemRoot") if k in os.environ}
            child["SSH_ASKPASS_REQUIRE"] = "never"
            result = run(
                ["ssh-keygen", "-y", "-P", "", "-f", str(path)],
                stdin=subprocess.DEVNULL,
                capture_output=True,
                env=child,
                timeout=8,
                check=True,
            )
        kind, encoded = result.stdout.split()[:2]
        blob = base64.b64decode(encoded, validate=True)
        if not blob.startswith(len(kind).to_bytes(4, "big") + kind):
            return INVALID
        digest = base64.b64encode(hashlib.sha256(blob).digest())
        return "SHA256:" + digest.decode("ascii").rstrip("=")
    except (OSError, ValueError, binascii.Error, subprocess.SubprocessError):
        # The SSH diagnostics are deliberately never printed.
        return INVALID


def main():
    if any(os.environ.get(name) != value for name, value in CONTEXT.items()):
        print("Execução manual do titular na main exigida.")
        return 2
    workdir = os.environ.get("RUNNER_TEMP")
    if not workdir or not Path(workdir).is_dir():
        print("Diretório temporário do runner indisponível.")
        return 2
    failed = False
    for name in NAMES:
        result = inspect_key(os.environ.get("AUDIT_" + name, ""), workdir)
        print(f"{name}: {result}")
        failed |= result == INVALID
    return int(failed)


if __name__ == "__main__":
    raise SystemExit(main())
