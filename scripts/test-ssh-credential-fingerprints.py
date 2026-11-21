"""Test the audit with synthetic data."""

import base64
from contextlib import redirect_stdout
import importlib.util
import io
import os
from pathlib import Path
import subprocess
import tempfile
import unittest
from unittest.mock import patch


spec = importlib.util.spec_from_file_location(
    "audit", Path(__file__).with_name("audit-ssh-credential-fingerprints.py")
)
audit = importlib.util.module_from_spec(spec)
spec.loader.exec_module(audit)
KIND = b"ssh-ed25519"
BLOB = len(KIND).to_bytes(4, "big") + KIND + b"\x00\x00\x00\x20" + bytes(range(32))
PUBLIC = KIND + b" " + base64.b64encode(BLOB) + b"\n"
FIXTURE = "synthetic-key"


class AuditTests(unittest.TestCase):
    def setUp(self):
        temp = tempfile.TemporaryDirectory()
        self.addCleanup(temp.cleanup)
        self.root = Path(temp.name)

    def test_public_fingerprint_and_temporary_file(self):
        public = self.root / "fixture.pub"
        public.write_bytes(PUBLIC)
        expected = subprocess.check_output(["ssh-keygen", "-lf", str(public)]).split()[1]

        def fake_keygen(argv, **options):
            identity = Path(argv[-1])
            self.assertEqual(FIXTURE + "\n", identity.read_text("utf-8"))
            self.assertNotIn(FIXTURE, str(argv))
            self.assertFalse(any(k.startswith("AUDIT_") for k in options["env"]))
            self.assertEqual(options["stdin"], subprocess.DEVNULL)
            if os.name != "nt":
                self.assertEqual(0o600, identity.stat().st_mode & 0o777)
            return subprocess.CompletedProcess(argv, 0, stdout=PUBLIC)

        with patch.dict(os.environ, AUDIT_UNRELATED=FIXTURE):
            actual = audit.inspect_key(FIXTURE, self.root, fake_keygen)
        self.assertEqual(expected.decode(), actual)
        self.assertEqual([public], list(self.root.iterdir()))

    def test_only_valid_public_output_is_reported(self):
        for output in (b"", b"invalid $$$", b"ssh-rsa " + PUBLIC.split()[1]):
            with self.subTest(output=output):
                result = audit.inspect_key(
                    FIXTURE, self.root,
                    lambda argv, **kw: subprocess.CompletedProcess(argv, 0, stdout=output),
                )
                self.assertEqual(audit.INVALID, result)
        self.assertEqual([], list(self.root.iterdir()))

    def test_missing_oversize_and_timeout(self):
        def unexpected(*args, **kwargs):
            self.fail("OpenSSH should not start")

        def timeout(*args, **kwargs):
            raise subprocess.TimeoutExpired("ssh-keygen", 8, stderr=FIXTURE)

        self.assertEqual(audit.MISSING, audit.inspect_key("", self.root, unexpected))
        for invalid in ("a" * 65537, "\ud800"):
            self.assertEqual(audit.INVALID, audit.inspect_key(invalid, self.root, unexpected))
        self.assertEqual(audit.INVALID, audit.inspect_key(FIXTURE, self.root, timeout))
        self.assertEqual([], list(self.root.iterdir()))

    def test_context_and_output(self):
        env = dict(audit.CONTEXT, RUNNER_TEMP=str(self.root), AUDIT_VPS_SSH_CHAVE=FIXTURE)
        with patch.dict(os.environ, env, clear=True):
            with redirect_stdout(io.StringIO()) as output, patch.object(
                audit, "inspect_key", return_value=audit.MISSING
            ) as inspect:
                self.assertEqual(0, audit.main())
                self.assertEqual(len(audit.NAMES), inspect.call_count)
                self.assertNotIn(FIXTURE, output.getvalue())
            for name in audit.CONTEXT:
                with self.subTest(name=name), patch.dict(os.environ, {name: "invalid"}):
                    with redirect_stdout(io.StringIO()), patch.object(audit, "inspect_key") as inspect:
                        self.assertEqual(2, audit.main())
                        inspect.assert_not_called()


if __name__ == "__main__":
    unittest.main()
