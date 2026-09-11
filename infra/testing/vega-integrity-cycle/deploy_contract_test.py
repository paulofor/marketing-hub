"""Protege secrets e o escopo da ativação manual já autorizada, sem acessar hosts reais."""
import contextlib
import importlib.util
import io
import json
from pathlib import Path
import unittest
from unittest.mock import patch

spec = importlib.util.spec_from_file_location('deploy_vega', Path(__file__).with_name('deploy-validated-image.py'))
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)


class DeploymentContractTest(unittest.TestCase):
    """Garante publicação de um serviço por vez e segredo literal apenas na memória."""

    def test_preserves_secret_and_only_recreates_selected_service(self):
        """Simula o Docker para verificar os argumentos, a configuração e a conferência final."""
        image = 'marketing-hub/backend:vega393-gate-v12-ui3'
        secret = 'synthetic-${DO_NOT_EXPAND}-only'
        current = {'Config': {'Labels': {'com.docker.compose.service':'backend',
            'com.docker.compose.project':'test-local', 'com.docker.compose.project.working_dir':'/local',
            'com.docker.compose.project.config_files':'/local/compose.yml'},
            'Env': ['PDE_INTERNAL_API_TOKEN='+secret]}, 'Mounts': []}
        config = {'services': {'backend': {'image':'previous', 'environment':{'PDE_INTERNAL_API_TOKEN':'old'},
            'build':'.', 'depends_on':{'other':{}}}, 'other':{'image':'preserve'}}}
        updated = {'Image':'sha256:test', 'State':{'Status':'running'}, 'Config':{'Env':[
            'PDE_INTERNAL_API_TOKEN='+secret, 'PDE_VEGA_PROTOTYPE_VERSION=musa-pde-entry-v12-primeiro-ajuste-aplicavel']}}
        calls = []
        def run(command, **kwargs):
            calls.append((command, kwargs))
        with patch.object(module.subprocess, 'check_output', side_effect=[json.dumps([current]),
                json.dumps(config), json.dumps([updated]), json.dumps([{'Id':'sha256:test'}])]), \
             patch.object(module.subprocess, 'run', side_effect=run), \
             patch('sys.argv',['deploy', '--container','marketinghub-backend','--image',image]), \
             contextlib.redirect_stdout(io.StringIO()) as output:
            module.main()
        command, kwargs = next(c for c in calls if 'up' in c[0])
        self.assertIn('--no-deps', command)
        self.assertIn('--no-build', command)
        payload = json.loads(kwargs['input'])
        self.assertEqual(list(payload['services']), ['backend'])
        self.assertEqual(payload['services']['backend']['environment']['PDE_INTERNAL_API_TOKEN'],
                         'synthetic-$${DO_NOT_EXPAND}-only')
        self.assertNotIn('build', payload['services']['backend'])
        self.assertNotIn(secret, output.getvalue())
        self.assertIn(image, output.getvalue())

    def test_rejects_unvalidated_image_before_accessing_docker(self):
        """Recusa uma imagem diferente da revisão testada sem iniciar qualquer operação."""
        with patch('sys.argv',['deploy','--container','marketinghub-backend','--image','unvalidated:latest']), \
             patch.object(module.subprocess,'check_output') as docker:
            with self.assertRaises(ValueError): module.main()
            docker.assert_not_called()

    def test_frontend_updates_only_its_image_and_preserves_the_network(self):
        """Simula a atualização da interface sem trocar configuração do backend ou rede existente."""
        image = 'marketing-hub/frontend:vega393-gate-v12-ui'
        current = {'Config': {'Labels': {'com.docker.compose.service':'frontend',
            'com.docker.compose.project':'local', 'com.docker.compose.project.working_dir':'/local',
            'com.docker.compose.project.config_files':'/local/compose.yml'}, 'Env':[]}, 'Mounts':[]}
        config = {'services': {'frontend': {'image':'previous', 'networks':['app']},
            'backend': {'image':'preserve'}}, 'networks': {'app': {'name':'existing-app'}}}
        updated = {'Image':'sha256:ui', 'State':{'Status':'running'}, 'Config':{'Env':[]}}
        secret = 'synthetic-backend-only-secret'
        backend = {'Config': {'Labels': {'com.docker.compose.project':'local'},
            'Env':['LEAD_PORTAL_PAYMENTS_AUTH_TOKEN='+secret]}}
        calls = []
        def output(command, **kwargs):
            if command == ['docker','inspect','marketinghub-frontend']:
                return json.dumps([updated if any('up' in call[0] for call in calls) else current])
            if command == ['docker','inspect','marketinghub-backend']:
                return json.dumps([backend])
            if '--format' in command:
                self.assertEqual(kwargs['env']['LEAD_PORTAL_PAYMENTS_AUTH_TOKEN'], secret)
                return json.dumps(config)
            return json.dumps([{'Id':'sha256:ui'}])
        with patch.object(module.subprocess, 'check_output', side_effect=output), \
             patch.object(module.subprocess, 'run', side_effect=lambda command, **kwargs: calls.append((command, kwargs))), \
             patch('sys.argv',['deploy','--container','marketinghub-frontend','--image',image]), \
             contextlib.redirect_stdout(io.StringIO()):
            module.main()
        _, kwargs = next(c for c in calls if 'up' in c[0])
        selected = json.loads(kwargs['input'])
        self.assertEqual(list(selected['services']), ['frontend'])
        self.assertEqual(selected['services']['frontend']['image'], image)
        self.assertEqual(selected['networks']['app']['name'], 'existing-app')
        self.assertNotIn(secret, kwargs['input'].decode())
        self.assertNotIn('LEAD_PORTAL_PAYMENTS_AUTH_TOKEN', selected['services']['frontend']['environment'])


if __name__ == '__main__':
    unittest.main()
