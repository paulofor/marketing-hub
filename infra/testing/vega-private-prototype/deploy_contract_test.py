"""Valida o tratamento de credenciais e a publicação sem acionar Docker ou hosts reais."""
import contextlib
import importlib.util
import io
import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch

spec = importlib.util.spec_from_file_location('vega_deploy', 'pde-platform/scripts/deploy-vega-private.py')
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)


class DeploymentContractTest(unittest.TestCase):
    """Impede impressão de secrets, fallback vazio e execução fora do Compose privado."""
    def test_missing_internal_secret_blocks(self):
        """Recusa publicação quando a credencial interna não foi configurada."""
        with tempfile.NamedTemporaryFile(mode='w') as key:
            key.write('synthetic-openai-only'); key.flush()
            with self.assertRaises(ValueError):
                module.deployment_environment({'Config': {'Env': []}}, key.name, 'front:v9', 'worker:v9')

    def test_secrets_are_not_written_or_printed(self):
        """Mantém secrets somente no ambiente do processo e exige imagens já construídas."""
        with tempfile.NamedTemporaryFile(mode='w') as key:
            key.write('synthetic-openai-only'); key.flush()
            source = {'Config': {'Env': ['PDE_INTERNAL_API_TOKEN=synthetic-internal-only']}}
            argv = ['deploy', '--compose', 'vega-private.compose.yml', '--openai-key-file', key.name,
                    '--frontend-image', 'front:v9', '--worker-image', 'worker:v9']
            output = io.StringIO()
            with patch('sys.argv', argv), patch.object(module.subprocess, 'check_output', return_value=json.dumps([source]).encode()), patch.object(module.subprocess, 'run') as run, contextlib.redirect_stdout(output):
                module.main()
            self.assertNotIn('synthetic-', output.getvalue())
            for invocation in run.call_args_list:
                self.assertNotIn('synthetic-', ' '.join(invocation.args[0]))
            final = run.call_args_list[-1]
            self.assertEqual(final.args[0][-5:], ['up', '-d', '--no-build', '--pull', 'never'])
            self.assertIn('vega-private', final.args[0])
            self.assertEqual(final.kwargs['env']['VEGA_OPENAI_API_KEY'], 'synthetic-openai-only')


if __name__ == '__main__':
    unittest.main()
