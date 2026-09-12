#!/usr/bin/env python3
"""Protege a aplicação isolada de imagem sem acessar Docker, SSH ou configurações produtivas."""
import importlib.util
from pathlib import Path
import unittest

spec = importlib.util.spec_from_file_location('application', Path(__file__).with_name('apply-backend.py'))
application = importlib.util.module_from_spec(spec)
spec.loader.exec_module(application)


class ApplicationContract(unittest.TestCase):
    """Verifica preservação e rejeições com configuração totalmente sintética."""

    def setUp(self):
        self.current = {'Config': {'Labels': {'com.docker.compose.service': 'backend', 'com.docker.compose.project.config_files': '/fixture/compose.yml'}, 'Env': ['TOKEN=fixture$value', 'SETTING=active']}}
        self.config = {'services': {'backend': {'container_name': 'marketinghub-backend', 'environment': {'TOKEN': 'old', 'SETTING': 'default'}, 'volumes': ['local:/data'], 'ports': ['80:8000'], 'depends_on': ['worker'], 'build': '.'}, 'worker': {'image': 'fixture'}}}
        self.image = 'marketing-hub/backend:vega-cycle6-012345abcdef'

    def test_preserves_runtime_and_selects_only_backend(self):
        result = application.plan(self.current, self.config, self.image)
        self.assertEqual(['backend'], list(result['services']))
        backend = result['services']['backend']
        self.assertEqual(self.image, backend['image'])
        self.assertEqual('fixture$value', backend['environment']['TOKEN'])
        self.assertEqual('active', backend['environment']['SETTING'])
        self.assertEqual(['local:/data'], backend['volumes'])
        self.assertEqual(['80:8000'], backend['ports'])
        self.assertNotIn('depends_on', backend)
        self.assertNotIn('build', backend)
        self.assertEqual('old', self.config['services']['backend']['environment']['TOKEN'])
        self.assertEqual('fixture$$value', application.literal(result)['services']['backend']['environment']['TOKEN'])

    def test_rejects_different_module_and_unversioned_image(self):
        for image in ['marketing-hub/backend:latest', 'marketing-hub/frontend:vega-cycle6-012345abcdef', 'other:012345abcdef']:
            with self.assertRaises(ValueError):
                application.plan(self.current, self.config, image)
        self.current['Config']['Labels']['com.docker.compose.service'] = 'frontend'
        with self.assertRaises(ValueError):
            application.plan(self.current, self.config, self.image)

    def test_frontend_preserves_configuration_without_touching_backend(self):
        self.current['Config']['Labels']['com.docker.compose.service'] = 'frontend'
        self.config['services']['frontend'] = dict(self.config['services']['backend'], container_name='marketinghub-frontend')
        result = application.plan(self.current, self.config, 'marketing-hub/frontend:vega-cycle6-012345abcdef', 'frontend')
        self.assertEqual(['frontend'], list(result['services']))
        self.assertEqual('fixture$value', result['services']['frontend']['environment']['TOKEN'])
        self.assertIn('backend', self.config['services'])
        with self.assertRaises(ValueError):
            application.plan(self.current, self.config, self.image, 'worker')

    def test_resolves_compose_with_backend_environment_without_copying_it_to_frontend(self):
        backend = {'Config': {'Env': ['BACKEND_SECRET=fixture-secret', 'SETTING=backend']}}
        environment = application.runtime_environment(self.current, backend)
        self.assertEqual('fixture-secret', environment['BACKEND_SECRET'])
        self.assertEqual('active', environment['SETTING'])
        result = application.plan(self.current, self.config, self.image)
        self.assertNotIn('BACKEND_SECRET', result['services']['backend']['environment'])

    def test_rejects_different_container(self):
        self.config['services']['backend']['container_name'] = 'different'
        with self.assertRaises(ValueError):
            application.plan(self.current, self.config, self.image)


if __name__ == '__main__':
    unittest.main()
