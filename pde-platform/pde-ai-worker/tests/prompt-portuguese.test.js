import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { describe, it } from 'node:test';

const promptFiles = [
  'prompts/musa-daily-guidance/system.md',
  'prompts/musa-daily-guidance/user.md',
  'prompts/musa-day-2-signature/system.md',
  'prompts/musa-day-2-signature/user.md',
  'prompts/musa-public-presence-diagnostic/system.md',
  'prompts/musa-public-presence-diagnostic/user.md',
];

const forbiddenTerms = [
  'Voce',
  'voce',
  'orientacao',
  'missao',
  'Metodo',
  'Nao',
  'faca',
  'jargao',
  'tecnico',
  'obrigatoria',
  'transformacao',
  'ruido',
  'microdecisoes',
  'coerencia',
  'memoravel',
  'microacao',
  'aplicavel',
  'valido',
  'intima',
  'acessivel',
  'cartao',
  'area',
  'historico',
  'ja',
  'Principio',
  'Acao',
  'Evidencia',
  'acionavel',
  'presenca',
];

describe('prompts da Consultora MUSA', () => {
  for (const promptFile of promptFiles) {
    it(`${promptFile} usa português brasileiro com acentuação`, async () => {
      const content = await readFile(new URL(`../${promptFile}`, import.meta.url), 'utf8');

      for (const term of forbiddenTerms) {
        const unaccentedTerm = new RegExp(`\\b${term}\\b`);
        assert.equal(
          unaccentedTerm.test(content),
          false,
          `Termo sem acentuação encontrado em ${promptFile}: ${term}`,
        );
      }
      assert.match(content, /acentuação correta|português brasileiro/i);
    });
  }

  it('prompts recebem a base científica operacional do produto', async () => {
    for (const promptFile of [
      'prompts/musa-daily-guidance/user.md',
      'prompts/musa-day-2-signature/user.md',
      'prompts/musa-public-presence-diagnostic/user.md',
    ]) {
      const content = await readFile(new URL(`../${promptFile}`, import.meta.url), 'utf8');

      assert.match(content, /{{scientificEvidencePackJson}}/);
      assert.match(content, /afirmações proibidas/);
      assert.match(content, /microações simples/);
    }
  });

  it('worker exige pacote científico antes de montar a chamada OpenAI', async () => {
    const worker = await readFile(new URL('../src/worker.js', import.meta.url), 'utf8');

    assert.match(worker, /requireScientificEvidencePack\(execution\)/);
    assert.match(worker, /scientificEvidencePackJson: JSON\.stringify\(scientificEvidencePack, null, 2\)/);
    assert.match(worker, /Pacote cientifico operacional ausente ou incompleto/);
  });

  it('diagnóstico público usa standard como primeira tentativa por impacto de conversão', async () => {
    const worker = await readFile(new URL('../src/worker.js', import.meta.url), 'utf8');

    assert.match(worker, /MUSA_PUBLIC_PRESENCE_DIAGNOSTIC/);
    assert.match(worker, /return \['default', 'flex', 'default'\]/);
    assert.match(worker, /return \['flex', 'flex', 'default'\]/);
  });
});
