import test from 'node:test';
import assert from 'node:assert/strict';
import { isTerminalResultConsistent } from '../../main/resources/browser/private-result-state-checks.mjs';

for (const item of [
  { id: 'vega-synthetic-91072', resultAvailable: /Seu ajuste continua disponível/, noResult: /nenhum ajuste foi gerado/i, available: 'Seu ajuste continua disponível.', absent: 'Este pedido foi bloqueado e nenhum ajuste foi gerado.' },
  { id: 'other-synthetic-97072', resultAvailable: /Relatório salvo disponível/, noResult: /nenhum relatório foi gerado/i, available: 'Relatório salvo disponível.', absent: 'Nenhum relatório foi gerado.' },
]) {
  test(`${item.id}: detecta afirmação de resultado inexistente e preserva o resultado real`, () => {
    const withoutCard = { state: 'FINISHED', card: null };
    assert.equal(isTerminalResultConsistent(withoutCard, item.available, item), false);
    assert.equal(isTerminalResultConsistent(withoutCard, item.absent, item), true);
    assert.equal(isTerminalResultConsistent(withoutCard, 'Fim da sessão.', item), false);
    const withCard = { state: 'FINISHED', card: { id: item.id } };
    assert.equal(isTerminalResultConsistent(withCard, item.available, item), true);
    assert.equal(isTerminalResultConsistent(withCard, item.absent, item), false);
    assert.equal(isTerminalResultConsistent({ state: 'ACTIVE', card: null }, '', item), true);
  });
}
