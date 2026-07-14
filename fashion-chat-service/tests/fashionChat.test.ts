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

test('ready health fails when Codex App Server is not authenticated', async () => {
  const fakeCodexAppServerClient = {
    isReady: () => true,
    health: () => ({ status: 'ready', ready: true, restartAttempts: 0 }),
    readAuthentication: async () => ({ authenticated: false }),
  } as unknown as CodexAppServerClient;

  const response = await request(createApp(fakeCodexAppServerClient)).get('/health/ready').expect(503);
  assert.equal(response.body.status, 'degraded');
  assert.equal(response.body.codexAppServer.authenticated, false);
});

test('ready health passes when Codex App Server is authenticated', async () => {
  const fakeCodexAppServerClient = {
    isReady: () => true,
    health: () => ({ status: 'ready', ready: true, restartAttempts: 0 }),
    readAuthentication: async () => ({ authenticated: true, authMode: 'chatgpt' }),
  } as unknown as CodexAppServerClient;

  const response = await request(createApp(fakeCodexAppServerClient)).get('/health/ready').expect(200);
  assert.equal(response.body.status, 'ok');
  assert.equal(response.body.codexAppServer.authenticated, true);
});

test('account read exposes disconnected Codex authentication state', async () => {
  const fakeCodexAppServerClient = {
    isReady: () => true,
    request: async (method: string) => {
      assert.equal(method, 'account/read');
      return {};
    },
  } as unknown as CodexAppServerClient;

  const response = await request(createApp(fakeCodexAppServerClient))
    .get('/codex-app-server/account/read')
    .expect(200);

  assert.equal(response.body.connected, false);
  assert.equal(response.body.status, 'disconnected');
  assert.equal(response.body.blockReason, 'CODEX_NOT_AUTHENTICATED');
});

test('account login start returns device-code authentication payload', async () => {
  const fakeCodexAppServerClient = {
    isReady: () => true,
    request: async (method: string, params: unknown) => {
      assert.equal(method, 'account/login/start');
      assert.deepEqual(params, { type: 'chatgptDeviceCode' });
      return {
        loginId: 'login-fashion-test',
        verificationUrl: 'https://example.test/activate',
        userCode: 'ABCD-EFGH',
        interval: 5,
      };
    },
  } as unknown as CodexAppServerClient;

  const response = await request(createApp(fakeCodexAppServerClient))
    .post('/codex-app-server/account/login/start')
    .send({})
    .expect(202);

  assert.equal(response.body.status, 'authorization_pending');
  assert.equal(response.body.loginId, 'login-fashion-test');
  assert.equal(response.body.userCode, 'ABCD-EFGH');
});

test('account login cancel requires login id and forwards cancellation', async () => {
  let cancelledLoginId = '';
  const fakeCodexAppServerClient = {
    isReady: () => true,
    request: async (method: string, params: { loginId?: string }) => {
      assert.equal(method, 'account/login/cancel');
      cancelledLoginId = params.loginId ?? '';
      return {};
    },
  } as unknown as CodexAppServerClient;

  await request(createApp(fakeCodexAppServerClient))
    .post('/codex-app-server/account/login/cancel')
    .send({})
    .expect(400);

  const response = await request(createApp(fakeCodexAppServerClient))
    .post('/codex-app-server/account/login/cancel')
    .send({ loginId: 'login-fashion-test' })
    .expect(200);

  assert.equal(cancelledLoginId, 'login-fashion-test');
  assert.equal(response.body.status, 'cancelled');
});

test('account logout clears account and returns current state', async () => {
  const calls: string[] = [];
  const fakeCodexAppServerClient = {
    isReady: () => true,
    request: async (method: string) => {
      calls.push(method);
      if (method === 'account/read') {
        return {};
      }
      return {};
    },
  } as unknown as CodexAppServerClient;

  const response = await request(createApp(fakeCodexAppServerClient))
    .post('/codex-app-server/account/logout')
    .expect(200);

  assert.deepEqual(calls, ['account/logout', 'account/read']);
  assert.equal(response.body.connected, false);
});

test('chat uses local fallback when Codex App Server is unavailable', async () => {
  mockResearchFetch();
  const response = await request(createApp())
    .post('/api/fashion-chat/messages')
    .send({ message: 'Que roupa usar em uma reuniao casual?', customerId: 'teste' })
    .expect(200);

  assert.equal(response.body.mode, 'local_fallback');
  assert.match(response.body.answer, /visual casual|ocasiao/);
  assert.ok(response.body.sandboxId);
  globalThis.fetch = originalFetch;
});

test('chat uses local fallback when Codex account is not authenticated', async () => {
  mockResearchFetch();
  const fakeCodexAppServerClient = {
    isReady: () => true,
    health: () => ({ status: 'ready', ready: true, restartAttempts: 0 }),
    onNotification: () => () => undefined,
    request: async (method: string) => {
      if (method === 'account/read') {
        return {};
      }
      return {};
    },
  } as unknown as CodexAppServerClient;

  const response = await request(createApp(fakeCodexAppServerClient))
    .post('/api/fashion-chat/messages')
    .send({ message: 'Que roupa usar em uma reuniao casual?', customerId: 'teste' })
    .expect(200);

  assert.equal(response.body.mode, 'local_fallback');
  assert.match(response.body.answer, /visual casual|ocasiao/);
  globalThis.fetch = originalFetch;
});

test('chat uses local fallback when Codex thread start does not return thread id', async () => {
  mockResearchFetch();
  const fakeCodexAppServerClient = {
    isReady: () => true,
    health: () => ({ status: 'ready', ready: true, restartAttempts: 0 }),
    onNotification: () => () => undefined,
    request: async (method: string) => {
      if (method === 'account/read') {
        return { authMode: 'chatgpt' };
      }
      if (method === 'thread/start') {
        return {};
      }
      return {};
    },
  } as unknown as CodexAppServerClient;

  const response = await request(createApp(fakeCodexAppServerClient))
    .post('/api/fashion-chat/messages')
    .send({ message: 'Que roupa usar em uma reuniao casual?', customerId: 'teste' })
    .expect(200);

  assert.equal(response.body.mode, 'local_fallback');
  assert.match(response.body.answer, /visual casual|ocasiao/);
  globalThis.fetch = originalFetch;
});

test('chat fallback handles greeting before style recommendation', async () => {
  mockResearchFetch();
  const response = await request(createApp())
    .post('/api/fashion-chat/messages')
    .send({ message: 'oi', customerId: 'teste' })
    .expect(200);

  assert.equal(response.body.mode, 'local_fallback');
  assert.match(response.body.answer, /Me diga a ocasiao/);
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
