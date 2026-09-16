#!/usr/bin/env python3
"""Impede uma seleção vazia silenciosa do Spotless quando vários arquivos mudam."""
import os
from pathlib import Path
import re
import subprocess
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[1]


class SpotlessSelectionTest(unittest.TestCase):
    def test_tracked_and_new_java_match_one_regex_without_selecting_other_files(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            module = root / "backend/ads-service"
            tracked = "src/main/java/Teste.java"
            created = "src/test/java/Novo$Contrato.java"
            for name in ["pom.xml", tracked, created]:
                file = module / name
                file.parent.mkdir(parents=True, exist_ok=True)
                file.touch()
            binary = root / "bin"
            binary.mkdir()
            git = binary / "git"
            git.write_text("#!/usr/bin/env python3\nimport sys, os\na=sys.argv\n"
                "if '--show-toplevel' in a: print(os.environ['FIXTURE_ROOT'])\n"
                "elif '--name-only' in a: print('backend/ads-service/src/main/java/Teste.java')\n"
                "elif 'ls-files' in a: print('backend/ads-service/src/test/java/Novo$Contrato.java')\n")
            maven = binary / "mvn"
            maven.write_text("#!/usr/bin/env python3\nimport os,sys,json\nfrom pathlib import Path\n"
                "Path(os.environ['FIXTURE_ROOT'],'arguments.json').write_text(json.dumps(sys.argv[1:]))\n")
            git.chmod(0o755)
            maven.chmod(0o755)
            subprocess.run(["bash", str(ROOT / "scripts/spotless-changed-java.sh"), "--apply"],
                env=dict(os.environ, PATH=str(binary)+os.pathsep+os.environ['PATH'], FIXTURE_ROOT=str(root)),
                capture_output=True, check=True)
            import json
            args = json.loads((root / "arguments.json").read_text())
            expression = next(value.split('=', 1)[1] for value in args if value.startswith('-DspotlessFiles='))
            self.assertIn('spotless:apply', args)
            for name in [tracked, created]:
                self.assertIsNotNone(re.fullmatch(expression, str(module / name)))
            self.assertIsNone(re.fullmatch(expression, str(module / 'src/main/java/Intocado.java')))
            self.assertIsNone(re.fullmatch(expression, str(module / 'src/test/java/NovoXContrato.java')))


if __name__ == '__main__':
    unittest.main()
