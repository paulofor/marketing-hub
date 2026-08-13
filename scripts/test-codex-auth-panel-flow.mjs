#!/usr/bin/env node

import assert from 'node:assert/strict';
import { spawn } from 'node:child_process';
import http from 'node:http';
import { once } from 'node:events';
import path from 'node:path';

const callbacks = [];
const server = http.createServer((request, response) => {
  let body = '';
  request.on('data', (chunk) => { body += chunk; });
  request.on('end', () => {
    callbacks.push({ path: request.url, body: JSON.parse(body) });
    response.writeHead(200, { 'Content-Type': 'application/json' });
    response.end('{}');
  });
});
server.listen(0, '127.0.0.1');
await once(server, 'listening');
const address = server.address();
const root = path.resolve(import.meta.dirname, '..');
const child = spawn('node', [path.join(root, 'scripts/codex-app-server-device-login.mjs')], {
  env: {
    ...process.env,
    CODEX_APP_SERVER_COMMAND: path.join(root, 'scripts/fake-codex-app-server.mjs'),
    CODEX_AUTH_RECONNECT_ID: '42',
    CODEX_AUTH_CALLBACK_BASE_URL: `http://127.0.0.1:${address.port}`,
  },
  stdio: ['ignore', 'pipe', 'pipe'],
});
let output = '';
child.stdout.on('data', (chunk) => { output += chunk; });
child.stderr.on('data', (chunk) => { output += chunk; });
const [code] = await once(child, 'exit');
server.close();

assert.equal(code, 0, output);
assert.equal(callbacks.length, 2);
assert.equal(callbacks[0].path, '/api/internal/agents/executor-health/codex-auth/reconnections/42/device-code');
assert.deepEqual(callbacks[0].body, {
  verificationUrl: 'https://auth.openai.com/codex/device',
  userCode: 'TEST-CODE',
});
assert.equal(callbacks[1].path, '/api/internal/agents/executor-health/codex-auth/reconnections/42/completion');
assert.equal(callbacks[1].body.authenticated, true);
assert.equal(JSON.stringify(callbacks).includes('refresh'), false);
console.log('Fluxo seguro de reconexão Codex validado.');
