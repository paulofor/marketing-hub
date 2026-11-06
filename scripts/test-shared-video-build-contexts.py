"""Confere que fixtures que compilam o backend recebem a fonte compartilhada de vídeo."""
from pathlib import Path
import re
import unittest

ROOT = Path(__file__).resolve().parents[1]
DOCKERFILES = (
    "backend/ads-service/Dockerfile.liquibase-mysql57",
    "backend/ads-service/Dockerfile.argos-meta-supervised-session-test",
    "infra/testing/facebook-resumption/Dockerfile.stop-reason-test",
)


def require_shared_source(text):
    """Exige layout monorepo compatível com os caminhos relativos do Maven."""
    if "WORKDIR /workspace/backend/ads-service" not in text:
        raise ValueError("Compilação deve preservar o layout backend/ads-service do monorepo")
    if "COPY shared/video-capabilities /workspace/shared/video-capabilities" not in text:
        raise ValueError("Fixture deve receber a fonte e o recurso compartilhados de vídeo")


def require_root_context(text):
    """Impede que o contexto de módulo esconda a fonte compartilhada da fixture comum."""
    pattern = r"context: ([^\n]+)\n\s+dockerfile: ([^\n]*Dockerfile\.(?:liquibase-mysql57|argos-meta-supervised-session-test))"
    builds = re.findall(pattern, text)
    for context, dockerfile in builds:
        if context.strip() != "../.." or not dockerfile.startswith("backend/ads-service/"):
            raise ValueError("Fixture Maven deve usar o contexto raiz e Dockerfile explícito do backend")
    return len(builds)


class SharedVideoBuildContextsTest(unittest.TestCase):
    """Previne a omissão observada no CI, incluindo outro cenário que usa a mesma imagem."""

    def test_compilers_receive_same_source_and_resource(self):
        for relative in DOCKERFILES:
            with self.subTest(path=relative):
                require_shared_source((ROOT / relative).read_text())

    def test_all_common_compose_builds_preserve_root(self):
        total = 0
        for path in (ROOT / "backend/ads-service").glob("docker-compose*.yml"):
            with self.subTest(path=str(path)):
                total += require_root_context(path.read_text())
        self.assertGreater(total, 1, "Cobrir a fixture original e outros cenários da mesma imagem")

    def test_previous_compiler_layout_is_rejected(self):
        with self.assertRaisesRegex(ValueError, "layout"):
            require_shared_source("WORKDIR /workspace\nCOPY src ./src")
        with self.assertRaisesRegex(ValueError, "fonte"):
            require_shared_source("WORKDIR /workspace/backend/ads-service\nCOPY backend/ads-service/src ./src")

    def test_previous_module_context_is_rejected(self):
        with self.assertRaisesRegex(ValueError, "contexto raiz"):
            require_root_context("context: .\n      dockerfile: Dockerfile.liquibase-mysql57")


if __name__ == "__main__":
    unittest.main()
