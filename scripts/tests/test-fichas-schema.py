"""Valida o acervo do piloto e regressões de integridade, sem executar agentes."""
import copy
import json
import pathlib
import unittest
from jsonschema import Draft202012Validator, FormatChecker

ROOT = pathlib.Path(__file__).resolve().parents[2]
DATA = ROOT / 'backend/ads-service/src/main/resources/fichas/v1'
SCHEMA = json.loads((DATA / 'ficha.schema.json').read_text())
CARDS = sum((json.loads((DATA / name).read_text()) for name in
             ['exemplo-bloqueado.json', 'exemplo-recuperado.json']), [])
VALIDATOR = Draft202012Validator(SCHEMA, format_checker=FormatChecker())


class FichasSchemaTest(unittest.TestCase):
    def test_acervo_e_vinculos_exatos(self):
        Draft202012Validator.check_schema(SCHEMA)
        refs = {(c['id'], c['revisao']) for c in CARDS}
        self.assertEqual(len(refs), len(CARDS))
        for card in CARDS:
            VALIDATOR.validate(card)
            for link in card['vinculos']:
                self.assertIn((link['id'], link['revisao']), refs)
        self.assertEqual(CARDS[2]['id'], CARDS[3]['id'])
        self.assertEqual(CARDS[2]['execucao']['estado'], 'BLOCKED')
        self.assertEqual(CARDS[3]['execucao']['estado'], 'COMPLETED')

    def test_criterio_sem_fonte_nao_confirma_nem_reprova(self):
        for state in ['confirmado', 'falho']:
            card = copy.deepcopy(CARDS[2])
            card['execucao']['criterios'] = [{'titulo': 'Prova', 'estado': state}]
            self.assertFalse(VALIDATOR.is_valid(card))
        for state in ['pendente', 'sem_evidencia']:
            card['execucao']['criterios'][0]['estado'] = state
            self.assertTrue(VALIDATOR.is_valid(card))

    def test_outro_produto_e_entrada_invalida(self):
        card = copy.deepcopy(CARDS[2])
        card.update(produtoId=99, id='teste-produto-99', ambiente='teste')
        card['execucao'].update(ciclo=45, experimento=900)
        VALIDATOR.validate(card)
        for key in ['produtoId', 'revisao', 'origem', 'registradoEm']:
            invalid = copy.deepcopy(card)
            del invalid[key]
            self.assertFalse(VALIDATOR.is_valid(invalid))
        card['origem']['url'] = 'javascript:alert(1)'
        self.assertFalse(VALIDATOR.is_valid(card))

    def test_tipo_errado_e_revisao_invalida(self):
        card = copy.deepcopy(CARDS[0])
        card['tipo'] = 'execucao'
        self.assertFalse(VALIDATOR.is_valid(card))
        card = copy.deepcopy(CARDS[2])
        card['vinculos'][0]['revisao'] = 0
        self.assertFalse(VALIDATOR.is_valid(card))

if __name__ == '__main__':
    unittest.main()
