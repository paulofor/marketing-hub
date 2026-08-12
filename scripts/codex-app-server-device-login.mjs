#!/usr/bin/env node

import { spawn } from 'node:child_process';
import readline from 'node:readline';

const command = process.env.CODEX_APP_SERVER_COMMAND || 'codex';
const timeoutMs = Number.parseInt(process.env.CODEX_DEVICE_LOGIN_TIMEOUT_MS || '900000', 10);
const child = spawn(command, ['app-server', '--listen', 'stdio://'], {
  env: process.env,
  stdio: ['pipe', 'pipe', 'inherit'],
});
const lines = readline.createInterface({ input: child.stdout });
const pending = new Map();
let nextId = 1;
let loginId;
let finished = false;

function request(method, params) {
  const id = nextId++;
  child.stdin.write(`${JSON.stringify({ method, id, ...(params ? { params } : {}) })}\n`);
  return new Promise((resolve, reject) => pending.set(id, { resolve, reject }));
}

function notify(method, params = {}) {
  child.stdin.write(`${JSON.stringify({ method, params })}\n`);
}

function stop(exitCode, message) {
  if (finished) return;
  finished = true;
  clearTimeout(timeout);
  if (message) (exitCode === 0 ? console.log : console.error)(message);
  lines.close();
  child.kill('SIGTERM');
  process.exitCode = exitCode;
}

lines.on('line', async (line) => {
  let message;
  try {
    message = JSON.parse(line);
  } catch {
    return;
  }
  if (typeof message.id === 'number') {
    const waiter = pending.get(message.id);
    if (!waiter) return;
    pending.delete(message.id);
    if (message.error) waiter.reject(new Error(message.error.message || 'Falha no Codex App Server'));
    else waiter.resolve(message.result || {});
    return;
  }
  if (message.method !== 'account/login/completed' || message.params?.loginId !== loginId) return;
  if (!message.params?.success) {
    stop(1, `Falha na autenticação Codex: ${message.params?.error || 'erro não informado'}`);
    return;
  }
  try {
    const account = await request('account/read', { refreshToken: false });
    if (!account.authMode) throw new Error('Codex App Server não confirmou a conta autenticada');
    stop(0, `Sessão Codex confirmada pelo App Server (modo ${account.authMode}).`);
  } catch (error) {
    stop(1, error.message);
  }
});

child.once('error', (error) => stop(1, `Não foi possível iniciar o Codex App Server: ${error.message}`));
child.once('exit', (code) => {
  if (!finished) stop(code || 1, 'Codex App Server encerrou antes de concluir a autenticação.');
});

const timeout = setTimeout(() => {
  if (loginId) notify('account/login/cancel', { loginId });
  stop(1, 'Tempo esgotado aguardando a confirmação do device code.');
}, timeoutMs);

try {
  await request('initialize', {
    clientInfo: { name: 'marketing_hub', title: 'Marketing Hub', version: '1.0.0' },
  });
  notify('initialized');
  const login = await request('account/login/start', { type: 'chatgptDeviceCode' });
  loginId = login.loginId;
  if (!loginId || !login.verificationUrl || !login.userCode) {
    throw new Error('Codex App Server não devolveu URL e código de autenticação.');
  }
  console.log(`Abra ${login.verificationUrl} e informe o código ${login.userCode}.`);
  console.log('Aguardando a confirmação segura do Codex App Server...');
} catch (error) {
  stop(1, error.message);
}
