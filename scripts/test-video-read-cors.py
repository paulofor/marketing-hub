#!/usr/bin/env python3
"""Valida CORS de mídia sem acessar AWS, Cloudflare ou configurações publicadas."""
import importlib.util
import pathlib
import unittest

spec = importlib.util.spec_from_file_location('video_cors', pathlib.Path(__file__).with_name('configure-video-read-cors.py'))
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)


class VideoReadCorsTest(unittest.TestCase):
    """Protege a mudança incremental, idempotente e somente de leitura."""

    def test_preserves_existing_rules_and_does_not_mutate_source(self):
        """Não altera regras preexistentes de outros consumidores."""
        current = {'CORSRules': [{'ID': 'other', 'AllowedOrigins': ['https://other.test'], 'AllowedMethods': ['PUT']}]}
        result = module.merge_read_rule(current, ['https://app.test'])
        self.assertEqual(result['CORSRules'][0], current['CORSRules'][0])
        self.assertEqual(len(current['CORSRules']), 1)
        self.assertEqual(result['CORSRules'][1]['AllowedMethods'], ['GET', 'HEAD'])

    def test_idempotence_and_new_explicit_origin(self):
        """Repetição não duplica regras nem elimina a origem anterior."""
        first = module.merge_read_rule({'CORSRules': []}, ['http://127.0.0.1:5173'])
        self.assertEqual(first, module.merge_read_rule(first, ['http://127.0.0.1:5173']))
        self.assertEqual(len(module.merge_read_rule(first, ['https://pde.test'])['CORSRules'][0]['AllowedOrigins']), 2)

    def test_rejects_wildcard_path_and_credentials(self):
        """Bloqueia origens amplas, inválidas ou que carreguem segredos."""
        for origin in ['*', 'https://*.test', 'https://app.test/', 'https://app.test?token=secret', 'https://user:pass@app.test', 'file://local']:
            with self.subTest(origin=origin), self.assertRaises(ValueError):
                module.merge_read_rule({}, [origin])

    def test_rejects_conflicting_owned_rule(self):
        """Recusa uma regra homônima com permissão de escrita."""
        with self.assertRaises(ValueError):
            module.merge_read_rule({'CORSRules':[{'ID':module.RULE_ID,'AllowedMethods':['PUT']}]}, ['https://app.test'])

    def test_detects_concurrent_configuration_change(self):
        """O hash muda se outro operador alterar o conteúdo durante a revisão."""
        before = {'CORSRules': []}
        self.assertNotEqual(module.digest(before), module.digest(module.merge_read_rule(before, ['https://app.test'])))


if __name__ == '__main__':
    unittest.main()
