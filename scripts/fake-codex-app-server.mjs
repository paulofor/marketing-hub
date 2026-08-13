#!/usr/bin/env node

import readline from 'node:readline';

const lines = readline.createInterface({ input: process.stdin });
lines.on('line', (line) => {
  const request = JSON.parse(line);
  if (request.method === 'initialize') {
    process.stdout.write(`${JSON.stringify({ id: request.id, result: {} })}\n`);
  } else if (request.method === 'account/login/start') {
    process.stdout.write(`${JSON.stringify({ id: request.id, result: { loginId: 'login-test', verificationUrl: 'https://auth.openai.com/codex/device', userCode: 'TEST-CODE' } })}\n`);
    setTimeout(() => process.stdout.write(`${JSON.stringify({ method: 'account/login/completed', params: { loginId: 'login-test', success: true } })}\n`), 50);
  } else if (request.method === 'account/read') {
    process.stdout.write(`${JSON.stringify({ id: request.id, result: { account: { type: 'chatgpt', email: 'test@sandbox.local' }, requiresOpenaiAuth: true } })}\n`);
  }
});
