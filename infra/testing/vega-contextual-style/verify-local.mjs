import assert from 'node:assert/strict';
import { readFile, writeFile } from 'node:fs/promises';

// Credenciais são sintéticas e o verificador recusa qualquer backend remoto.
const [phase, statePath] = process.argv.slice(2);
const base = 'http://127.0.0.1:18086/api/pde/vega/style/v1';
const version = 'musa-pde-entry-v16-modelagem-internacional';
const internal = 'vega-local-internal-only';
async function call(path, body, session, expected = 200, extra = {}) {
  const response = await fetch(base + path, { method: body === undefined ? 'GET' : 'POST',
    headers: { 'Content-Type': 'application/json', 'X-PDE-Internal-Token': internal,
      ...(session ? { 'X-Vega-Style-Session': session } : {}), ...extra },
    ...(body === undefined ? {} : { body: JSON.stringify(body) }) });
  assert.equal(response.status, expected, `${path}: HTTP ${response.status}`);
  return response.json();
}
if (phase === 'before-restart') {
  const session = await call('/internal/sessions', { cycleId: 91002, prototypeVersion: version });
  const other = await call('/internal/sessions', { cycleId: 91002, prototypeVersion: version });
  await call('/start', {}, session.sessionToken);
  const input = { experienceVersion: version, operationKey: 'concurrency-context-0001', answers: {
    occasion: 'almoço', existingSelection: 'camisa, lenço e bolsa; posso usar o lenço no pescoço ou na alça', stylePreference: 'Discrição' } };
  const replies = await Promise.all(Array.from({ length: 6 }, () => call('/access/missions/estilo-dia-1/ai-guidance', input, session.sessionToken)));
  replies.forEach(reply => assert.deepEqual(reply, replies[0]));
  assert.equal(Object.keys((await call('/internal/report', undefined, session.sessionToken)).operations).length, 1);
  await call('/access/ai-guidance/' + input.operationKey, undefined, other.sessionToken, 409);
  await call('/start', {}, session.sessionToken, 403, { Origin: 'https://foreign.invalid' });
  await call('/access/missions/estilo-dia-1/ai-guidance', { ...input, answers: { ...input.answers, occasion: 'videochamada' } }, session.sessionToken, 409);
  await call('/access/missions/estilo-dia-2/ai-guidance', { ...input, operationKey: 'locked-context-0002' }, session.sessionToken, 409);
  const blocked = await call('/access/missions/estilo-dia-1/ai-guidance', { ...input, operationKey: 'blocked-context-0002', answers: { ...input.answers, existingSelection: 'quero comprar outra roupa' } }, session.sessionToken);
  assert.equal(blocked.status, 'BLOCKED'); assert.deepEqual(blocked.microActions, []);
  await writeFile(statePath, JSON.stringify({ session: session.sessionToken, result: replies[0], blocked }), { mode: 0o600 });
  console.log('Concorrência, isolamento, origem, direitos e bloqueio: aprovados em MySQL 5.7.');
} else if (phase === 'after-restart') {
  const state = JSON.parse(await readFile(statePath, 'utf8'));
  for (const result of [state.result, state.blocked]) assert.deepEqual(await call('/access/ai-guidance/' + result.requestId, undefined, state.session), result);
  const report = await call('/internal/report', undefined, state.session);
  assert.equal(Object.keys(report.operations).length, 2);
  assert.ok(Object.values(report.operations).every(op => op.providerCalls === 0));
  assert.equal(report.state, 'BLOCKED');
  await call('/internal/fixtures/expire-session', {}, state.session);
  await call('/access/workspace', undefined, state.session, 401);
  console.log('Reinício preservou resposta válida, bloqueio e auditoria; expiração recusou novo acesso.');
} else throw new Error('Fase de validação desconhecida');
