// Reproduz o backend e os filtros Runway somente na rede isolada da homologação.
import http from 'node:http';
import fs from 'node:fs';
import assert from 'node:assert/strict';
const fixture = name => JSON.parse(fs.readFileSync(`/tmp/fixtures/gen45-${name}.json`, 'utf8'));
fs.mkdirSync('/tmp/evidence', { recursive: true });
let claimed = false;
const state = { dryRuns: [], callbacks: [], errors: [] };
const persist = () => fs.writeFileSync('/tmp/evidence/result.json', JSON.stringify(state, null, 2));
const server = http.createServer(async (req, res) => {
  const url = new URL(req.url, 'http://mock');
  const reply = (status, body) => { res.writeHead(status, { 'content-type': 'application/json' }); res.end(body === undefined ? undefined : JSON.stringify(body)); };
  try {
    if (req.method === 'GET') {
      if (url.pathname === '/v1/organization') return reply(200, fixture('organization'));
      if (url.pathname.endsWith('/automatic-execution')) return reply(200, { automaticExecutionEnabled: true });
      if (url.pathname.endsWith('/codex-auth/reconnections/pending')) return reply(204);
      if (url.pathname.endsWith('/provider-preflight/pending')) {
        const jobs = claimed ? [] : [fixture('pending')]; claimed = true; return reply(200, jobs);
      }
      if (url.pathname === '/internal/video/jobs') return reply(200, []);
      if (url.pathname === '/health') return reply(200, { status: 'UP' });
    }
    const chunks = []; for await (const chunk of req) chunks.push(chunk);
    const text = Buffer.concat(chunks).toString();
    const body = text ? JSON.parse(text) : null;
    if (req.method === 'POST' && url.pathname === '/v1/generate/video') {
      assert.equal(body.dryRun, true, 'Proibida geração faturável na homologação');
      assert.equal(body.configId, 'qa-gen45');
      assert.equal('negativePrompt' in body.input, false);
      assert.ok(body.input.promptText.length <= 1000);
      const response = fixture('dry-run');
      response.routing.resolvedInput.duration = body.input.duration;
      response.routing.estimatedCost.credits = body.input.duration * 12;
      state.dryRuns.push({ request: body, response }); persist(); return reply(200, response);
    }
    if (req.method === 'POST' && url.pathname === '/api/internal/sales-videos/autonomy/v1/cycles/91014/provider-preflight-result') {
      assert.equal(body.status, 'READY');
      assert.equal(body.estimatedCredits, 180);
      assert.ok(!Array.isArray(JSON.parse(body.quotaSnapshotJson)));
      assert.equal(JSON.parse(body.quotaSnapshotJson).models.length, 1);
      assert.equal(state.dryRuns.length, 2);
      assert.equal(body.payloadSha256.length, 64);
      assert.deepEqual(JSON.parse(body.executionRequestsJson), state.dryRuns.map(({ request }) => { const copy = { ...request }; delete copy.dryRun; return copy; }));
      state.callbacks.push(body); persist(); return reply(204);
    }
    if (req.method === 'POST' && url.pathname === '/api/internal/agents/executor-health') return reply(204);
    if (req.method === 'POST' && url.pathname === '/api/internal/sales-videos/autonomy/v1/apollo/reconcile') return reply(204);
    throw new Error(`Rota não simulada: ${req.method} ${url.pathname}`);
  } catch (error) {
    state.errors.push(error.message); persist(); reply(500, { message: error.message });
  }
});
server.listen(8080, '0.0.0.0');
