import assert from 'node:assert/strict';
import test from 'node:test';
import request from 'supertest';
import type { CodexAppServerClient } from '../src/codexAppServerClient.js';
import { createApp } from '../src/server.js';

const originalFetch = globalThis.fetch;

function mockResearchFetch() {
  globalThis.fetch = async () =>
    new Response('<html></html>', {
      status: 200,
      headers: { 'content-type': 'text/html' },
    });
}

test('health returns service status', async () => {
  const response = await request(createApp()).get('/health').expect(200);
  assert.equal(response.body.service, 'fashion-chat-service');
});

test('chat requires Codex App Server to answer', async () => {
  mockResearchFetch();
  await request(createApp())
    .post('/api/fashion-chat/messages')
    .send({ message: 'Que roupa usar em uma reuniao casual?', customerId: 'teste' })
    .expect(503)
    .expect((response) => {
      assert.equal(response.body.error, 'CODEX_APP_SERVER_UNAVAILABLE');
    });
  globalThis.fetch = originalFetch;
});

test('chat answers only through Codex App Server', async () => {
  mockResearchFetch();
  const listeners = new Map<string, (params: unknown) => void>();
  const fakeCodexAppServerClient = {
    isReady: () => true,
    health: () => ({ status: 'ready', ready: true, restartAttempts: 0 }),
    onNotification: (method: string, listener: (params: unknown) => void) => {
      listeners.set(method, listener);
      return () => listeners.delete(method);
    },
    request: async (method: string) => {
      if (method === 'account/read') {
        return { authMode: 'chatgpt' };
      }
      if (method === 'thread/start') {
        return { threadId: 'thread-fashion-test' };
      }
      if (method === 'turn/start') {
        setTimeout(() => {
          listeners.get('turn/completed')?.({ text: 'Use alfaiataria leve, base neutra e um acessorio de cor.' });
        }, 0);
        return {};
      }
      return {};
    },
  } as unknown as CodexAppServerClient;

  const response = await request(createApp(fakeCodexAppServerClient))
    .post('/api/fashion-chat/messages')
    .send({ message: 'Que roupa usar em uma reuniao casual?', customerId: 'teste' })
    .expect(200);

  assert.equal(response.body.mode, 'codex_app_server');
  assert.match(response.body.answer, /alfaiataria/);
  assert.ok(response.body.sandboxId);
  globalThis.fetch = originalFetch;
});

test('chat validates required message', async () => {
  await request(createApp()).post('/api/fashion-chat/messages').send({ message: ' ' }).expect(400);
});
