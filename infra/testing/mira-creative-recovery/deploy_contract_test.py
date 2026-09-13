"""Confere preservação da configuração de publicação sem executar Docker ou SSH."""
import copy
import importlib.util
from pathlib import Path
import unittest
from unittest.mock import patch

spec = importlib.util.spec_from_file_location('mira_apply', Path(__file__).with_name('apply-validated-image.py'))
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)


class ApplyContractTest(unittest.TestCase):
    """Protege escopo de serviço, secrets literais e configuração do container anterior."""

    def test_preserves_settings_and_only_selects_backend(self):
        """A revisão é o único ajuste de configuração autorizado ao recriar o backend."""
        revision = 'a' * 40
        current = {'Name': '/marketinghub-backend', 'Config': {'Env': [
            'SECRET=synthetic-${LITERAL}-only', 'PDE_VEGA_PROTOTYPE_VERSION=preservar'], 'Labels': {
            'com.docker.compose.service': 'backend', 'com.docker.compose.project.config_files': '/repo/compose.yml'}}}
        config = {'services': {'backend': {'image': 'old', 'build': '.', 'depends_on': {'db': {}},
            'environment': {'SECRET': 'previous'}, 'volumes': [{'source': '/data', 'target': '/app/uploads'}],
            'networks': ['app']}, 'frontend': {'image': 'preserve'}}, 'networks': {'app': {'name': 'existing'}}}
        result = module.selected_configuration(copy.deepcopy(config), current,
            'marketing-hub/backend:mira-creative-review-v1-' + revision[:12], revision)
        self.assertEqual(list(result['services']), ['backend'])
        backend = result['services']['backend']
        self.assertEqual(backend['environment']['PDE_VEGA_PROTOTYPE_VERSION'], 'preservar')
        self.assertEqual(backend['volumes'], config['services']['backend']['volumes'])
        self.assertEqual(result['networks'], config['networks'])
        self.assertNotIn('build', backend)
        self.assertNotIn('depends_on', backend)
        self.assertEqual(module.literal(result)['services']['backend']['environment']['SECRET'], 'synthetic-$${LITERAL}-only')

    def test_refuses_another_container_or_image(self):
        """Recusa container homônimo de outro contexto e troca cruzada entre módulos."""
        for name, image in [('/other-backend', 'backend'), ('/marketinghub-backend', 'communication-agent-worker')]:
            current = {'Name': name, 'Config': {'Labels': {'com.docker.compose.service': 'backend'}}}
            with self.assertRaises(ValueError):
                module.selected_configuration({}, current, f'marketing-hub/{image}:mira-creative-review-v1-aaaaaaaaaaaa', 'a' * 40)

    def test_retry_never_replaces_rollback_with_candidate(self):
        """Uma chamada repetida após sucesso não sobrescreve a imagem reservada para retorno."""
        revision = 'a' * 40
        image = 'marketing-hub/backend:mira-creative-review-v1-' + revision[:12]
        with patch('sys.argv', ['apply', '--container', 'marketinghub-backend', '--image', image, '--revision', revision]), \
             patch.object(module, 'read', side_effect=[[{'Image': 'sha256:validated'}],
                [{'Id': 'sha256:validated', 'Config': {'Labels': {'org.opencontainers.image.revision': revision}}}]]), \
             patch.object(module.subprocess, 'run') as mutation, patch('builtins.print'):
            module.main()
        mutation.assert_not_called()


if __name__ == '__main__':
    unittest.main()
