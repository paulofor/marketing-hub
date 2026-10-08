import assert from 'node:assert/strict';
import { writeFile } from 'node:fs/promises';

// Testa os contratos reais e locks no banco local; nunca aceita um backend publicado.
const backend = new URL(process.env.VEGA_LOCAL_BACKEND || 'http://127.0.0.1:18080');
assert.ok(['127.0.0.1', 'localhost'].includes(backend.hostname));
const base = new URL('/api/pde/vega/private/v1', backend).href;
const version = 'musa-pde-entry-v12-primeiro-ajuste-aplicavel';
const model = 'vega-deterministic-fixture-v1';
const internal = { 'X-PDE-Internal-Token': 'vega-local-internal-only' };
const input = { occasion: 'Almoço', existingSelection: 'Camisa já disponível', optionalNote: '' };
async function api(path, body, headers = internal, method) {
  const response = await fetch(base + path, { method: method || (body === undefined ? 'GET' : 'POST'), headers: { 'Content-Type': 'application/json', ...headers }, body: body === undefined ? undefined : JSON.stringify(body), signal: AbortSignal.timeout(15000) });
  const text = await response.text();
  return { status: response.status, data: text ? JSON.parse(text) : null };
}
const sessionHeaders = session => ({ 'X-Vega-Session': session.sessionToken });
async function create(cycleId, origin = 'AGENT_VALIDATION') {
  return api('/internal/sessions', { cycleId, origin, prototypeVersion: version, readingNumber: origin === 'HUMAN' ? 1 : null });
}
async function generate(session, value = input) {
  return api('/generate', value, sessionHeaders(session));
}
async function failAttempt(id) {
  assert.equal((await api(`/internal/adjustment/stage-executions/${id}/claim`, {})).status, 200);
  assert.equal((await api(`/internal/adjustment/stage-executions/${id}/request`, { model, request: { provider: 'DETERMINISTIC_FIXTURE' } })).status, 200);
  const result = await api(`/internal/adjustment/stage-executions/${id}/result`, { status: 'FAILED', model, inputTokens: 0, outputTokens: 0, costUsd: 0, error: 'Falha sintética local', rawResponse: { synthetic: true } });
  assert.equal(result.data.status, 'FAILED');
}

assert.equal((await api('/session', undefined, { 'X-Vega-Session': 'invalid-local-token' })).status, 401);
assert.equal((await api('/session', undefined, { 'X-Vega-Session': 'expired-local-token' })).status, 401);
assert.equal((await create(91010)).status, 409);
assert.equal((await api('/internal/adjustment/stage-executions/91999/claim', {})).status, 409);

// Uma reserva pelo ciclo impede que requisições concorrentes ultrapassem 18 sessões.
const concurrent = await Promise.all(Array.from({ length: 24 }, () => create(91007)));
const sessions = concurrent.filter(r => r.status === 200).map(r => r.data);
assert.equal(sessions.length, 18);
assert.equal(concurrent.filter(r => r.status === 409).length, 6);
const secondary = await create(91008);
assert.equal(secondary.status, 200);
assert.equal(secondary.data.experimentId, 91101);
assert.equal((await api('/internal/sessions/' + secondary.data.id, undefined, internal, 'DELETE')).status, 200);
assert.equal((await api('/session', undefined, sessionHeaders(secondary.data))).status, 401);

for (const session of sessions) assert.equal((await api('/start', {}, sessionHeaders(session))).status, 200);
// Cliques repetidos da mesma entrada conservam uma única execução, mesmo sob concorrência.
const clicks = await Promise.all(Array.from({ length: 5 }, () => generate(sessions[0])));
assert.ok(clicks.every(r => r.status === 200));
assert.equal(new Set(clicks.map(r => r.data.executionId)).size, 1);
assert.equal((await generate(sessions[0], { ...input, occasion: 'Trabalho' })).status, 409);
const first = await Promise.all(sessions.map(s => generate(s)));
assert.ok(first.every(r => r.status === 200));
const providerPending = await api('/internal/adjustment/stage-executions/pending');
assert.equal(providerPending.status, 200);
assert.equal(providerPending.data.length, 0);
const fixturePending = await api('/internal/adjustment/stage-executions/pending?mode=FIXTURE');
assert.ok(fixturePending.data.length > 0);
assert.ok(fixturePending.data.every(e => e.context.cycleId === 91007));
assert.ok(fixturePending.data.every(e => e.id !== 91999));
await Promise.all(first.map(r => failAttempt(r.data.executionId)));
const second = await Promise.all(sessions.map(s => generate(s)));
assert.ok(second.every(r => r.status === 200));
await Promise.all(second.map(r => failAttempt(r.data.executionId)));
assert.ok((await Promise.all(sessions.map(s => generate(s)))).every(r => r.status === 409));
await api('/internal/sessions/' + sessions[0].id, undefined, internal, 'DELETE');
assert.equal((await create(91007)).status, 409);

const report = (await api('/internal/cycles/91007/report')).data;
assert.equal(report.readings.length, 18);
const attempts = report.readings.flatMap(r => r.executions);
assert.equal(attempts.length, 36);
assert.ok(attempts.every(e => e.status === 'FAILED' && e.costUsd === 0));
assert.ok(report.readings.every(r => r.origin === 'AGENT_VALIDATION' && Object.keys(r.events).length === 1));
const summary = { status: 'PASS', database: 'MySQL 5.7', concurrentSessionRequests: 24, acceptedSessions: 18, attempts: 36, retryLimitPreserved: true, revokedAccessRejected: true, expiredAccessRejected: true, historicalCycleExcluded: true, separateExecutionIdentity: true, providerCalls: 0, totalFixtureCostUsd: 0, testTrafficOnly: true };
if (process.env.VEGA_LOCAL_BOUNDARY_REPORT) await writeFile(process.env.VEGA_LOCAL_BOUNDARY_REPORT, JSON.stringify(summary, null, 2));
console.log(JSON.stringify(summary));
