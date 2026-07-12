import assert from 'node:assert/strict';
import test from 'node:test';
import request from 'supertest';
import { createApp } from '../src/server.js';

test('health returns service status', async () => {
  const response = await request(createApp()).get('/health').expect(200);
  assert.equal(response.body.service, 'fashion-chat-service');
});

test('chat returns an answer without external credentials when fallback is forced', async () => {
  process.env.FASHION_CHAT_FORCE_FALLBACK = 'true';
  const response = await request(createApp())
    .post('/api/fashion-chat/messages')
    .send({ message: 'Que roupa usar em uma reuniao casual?', customerId: 'teste' })
    .expect(200);

  assert.equal(response.body.mode, 'deterministic_fallback');
  assert.match(response.body.answer, /reuniao|reuni/);
  assert.ok(response.body.sandboxId);
  delete process.env.FASHION_CHAT_FORCE_FALLBACK;
});

test('chat validates required message', async () => {
  await request(createApp()).post('/api/fashion-chat/messages').send({ message: ' ' }).expect(400);
});
