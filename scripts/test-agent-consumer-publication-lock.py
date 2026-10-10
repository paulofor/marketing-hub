#!/usr/bin/env python3
"""Comprova publicação após entrega, falha protegida e ausência de cancelamento."""

import fcntl
import json
import os
import pathlib
import subprocess
import sys
import tempfile
import time
import unittest

SCRIPT = pathlib.Path(__file__).with_name("with-agent-consumer-lock.py")
REVISION = "a" * 40


class PublicationLockTest(unittest.TestCase):
    """Exercita locks reais e comandos locais, sem Docker ou chamada a provedor."""

    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory()
        self.state = pathlib.Path(self.temporary.name)
        self.children = []

    def tearDown(self):
        for process in self.children:
            if process.poll() is None:
                process.terminate()
            process.communicate(timeout=5)
        self.temporary.cleanup()

    def publisher(self, code, timeout=3):
        process = subprocess.Popen([sys.executable, str(SCRIPT), "--state-directory", str(self.state),
                                    "--revision", REVISION, "--wait-seconds", str(timeout), "--",
                                    sys.executable, "-c", code, str(self.state)],
                                   stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
        self.children.append(process)
        return process

    def wait_for(self, path):
        deadline = time.monotonic() + 3
        while not path.exists() and time.monotonic() < deadline:
            time.sleep(0.02)
        self.assertTrue(path.exists())

    def hold_execution(self):
        code = """import pathlib,sys,fcntl,time
p=pathlib.Path(sys.argv[1])
with (p/'consumer.lock').open('a+b') as lock:
 fcntl.lockf(lock,fcntl.LOCK_EX)
 (p/'execution-started').write_text('started')
 while not (p/'finish-execution').exists():time.sleep(0.02)
 (p/'result.json').write_text('{"decision":"APPROVE"}')
"""
        process = subprocess.Popen([sys.executable, "-c", code, str(self.state)],
                                   stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
        self.children.append(process)
        self.wait_for(self.state / "execution-started")
        return process

    def test_waits_for_result_before_replacing_and_resumes_only_success(self):
        current = self.hold_execution()
        publisher = self.publisher("import pathlib,sys;p=pathlib.Path(sys.argv[1]);assert (p/'result.json').exists();(p/'published').write_text('healthy')")
        self.wait_for(self.state / "publisher-pause.json")
        self.assertFalse((self.state / "published").exists())
        self.assertIsNone(current.poll())
        (self.state / "finish-execution").touch()
        current.communicate(timeout=5)
        output, error = publisher.communicate(timeout=5)
        self.assertEqual(publisher.returncode, 0, (output, error))
        self.assertTrue((self.state / "published").exists())
        self.assertFalse((self.state / "publisher-pause.json").exists())

    def test_failure_preserves_pause_and_allows_result_replay(self):
        publisher = self.publisher("import sys;sys.exit(7)")
        publisher.communicate(timeout=5)
        self.assertEqual(publisher.returncode, 7)
        marker = json.loads((self.state / "publisher-pause.json").read_text())
        self.assertEqual(marker["revision"], REVISION)
        with (self.state / "consumer.lock").open("a+b") as lock:
            fcntl.lockf(lock, fcntl.LOCK_EX | fcntl.LOCK_NB)

    def test_timeout_never_cancels_execution_or_releases_pause(self):
        current = self.hold_execution()
        publisher = self.publisher("import pathlib,sys;(pathlib.Path(sys.argv[1])/'published').touch()", timeout=0.1)
        publisher.communicate(timeout=5)
        self.assertNotEqual(publisher.returncode, 0)
        self.assertIsNone(current.poll())
        self.assertTrue((self.state / "publisher-pause.json").exists())
        self.assertFalse((self.state / "published").exists())
        (self.state / "finish-execution").touch()
        current.communicate(timeout=5)
        self.assertTrue((self.state / "result.json").exists())

    def test_does_not_release_replaced_pause(self):
        publisher = self.publisher("import pathlib,sys,json;p=pathlib.Path(sys.argv[1])/'publisher-pause.json';p.write_text(json.dumps({'revision':'b'*40,'publicationId':'other'}))")
        publisher.communicate(timeout=5)
        self.assertNotEqual(publisher.returncode, 0)
        self.assertEqual(json.loads((self.state / "publisher-pause.json").read_text())["publicationId"], "other")


class RuntimeReadinessTest(unittest.TestCase):
    """Comprova revisão, autenticação e saúde antes de liberar a pausa real."""

    def test_runtime_releases_only_after_all_required_proofs(self):
        startup = SCRIPT.parent.parent / "experiment-strategist-worker/scripts/start-validated-runtime.sh"
        for failure in ("none", "image", "auth", "health", "logfile"):
            with self.subTest(failure=failure), tempfile.TemporaryDirectory() as temporary:
                root = pathlib.Path(temporary)
                state = root / "state"
                state.mkdir()
                (root / "experiment-strategist-worker").mkdir()
                executables = root / "bin"
                executables.mkdir()
                docker = """#!/usr/bin/env python3
import os,sys
args=sys.argv[1:]
f=os.environ['FIXTURE_FAILURE']
if args[0]=='inspect':
 if 'State.Status' in args[2]:print('running')
 else:print('wrong-image' if f=='image' else 'marketing-hub/experiment-strategist-worker:'+os.environ['FIXTURE_REVISION'])
if args[0]=='exec' and f=='auth':sys.exit(1)
"""
                curl = """#!/usr/bin/env python3
import os,sys
f=os.environ['FIXTURE_FAILURE']
if sys.argv[-1].endswith('/health'):print('{"status":"DOWN"}' if f=='health' else '{"status":"UP"}')
else:print('500' if f=='logfile' else '200')
"""
                for name, body in {"docker": docker, "curl": curl,
                                   "install": "#!/bin/sh\nexit 0\n",
                                   "sleep": "#!/bin/sh\nexit 0\n"}.items():
                    executable = executables / name
                    executable.write_text(body)
                    executable.chmod(0o700)
                environment = dict(os.environ, PATH=str(executables)+os.pathsep+os.environ['PATH'],
                                   FIXTURE_FAILURE=failure, FIXTURE_REVISION=REVISION)
                result = subprocess.run([sys.executable, str(SCRIPT), "--state-directory", str(state),
                                         "--revision", REVISION, "--", "bash", str(startup),
                                         str(root), REVISION], env=environment,
                                        capture_output=True, text=True, timeout=15)
                if failure == "none":
                    self.assertEqual(result.returncode, 0, result.stderr)
                    self.assertFalse((state / "publisher-pause.json").exists())
                else:
                    self.assertNotEqual(result.returncode, 0)
                    self.assertTrue((state / "publisher-pause.json").exists())


if __name__ == "__main__":
    unittest.main()
