#!/usr/bin/env node
import { createInterface } from 'node:readline';
import { appendFileSync, readFileSync } from 'node:fs';
import { join } from 'node:path';

const home = process.env.CODEX_HOME;
const config = JSON.parse(readFileSync(join(home, 'fixture.json'), 'utf8'));
let threadId;
createInterface({ input: process.stdin }).on('line', line => {
  const m = JSON.parse(line);
  appendFileSync(join(home, 'calls.jsonl'), `${JSON.stringify(m)}\n`);
  const reply = result => process.stdout.write(`${JSON.stringify({ id: m.id, result })}\n`);
  const event = (method, params) => process.stdout.write(`${JSON.stringify({ method, params: { threadId, ...params } })}\n`);
  if (m.method === 'initialize') reply({ codexHome: home });
  else if (m.method === 'account/read') reply({ account: config.auth === false ? null : { type: config.authType || 'chatgpt' } });
  else if (m.method === 'model/list') reply({ data: [{ model: 'gpt-test' }] });
  else if (m.method === 'account/login/start') {
    config.auth = true;
    reply({ loginId: 'login-fixture', verificationUrl: 'https://auth.openai.com/codex/device', userCode: 'SYNTHETIC' });
    setTimeout(() => event('account/login/completed', { loginId: 'login-fixture', success: true }), 15);
  }
  else if (m.method === 'thread/start') { threadId = `thread-${config.identity || 'a'}`; reply({ thread: { id: threadId } }); }
  else if (m.method === 'turn/start') {
    if (config.mode === 'disconnect') { process.exit(9); }
    reply({ turn: { id: 'turn-1' } });
    if (config.mode === 'tool') {
      process.stdout.write(`${JSON.stringify({ id: 'tool-1', method: 'item/commandExecution/requestApproval', params: { threadId } })}\n`);
      return;
    }
    const item = { type: 'agentMessage', id: 'answer', phase: 'final_answer', text: config.text || '{"ready":true}' };
    event('item/completed', { item: { ...item, id: 'commentary', phase: 'commentary', text: 'Não usar comentário como resposta' } });
    event('item/completed', { item });
    if (!config.missingUsage) event('thread/tokenUsage/updated', { tokenUsage: { total: { inputTokens: 80, outputTokens: 20, totalTokens: 100 } } });
    event('turn/completed', { turn: { id: 'turn-1', status: config.status || 'completed' } });
  }
});
