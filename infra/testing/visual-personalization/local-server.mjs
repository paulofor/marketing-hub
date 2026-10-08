import http from 'node:http';
import fs from 'node:fs/promises';
import path from 'node:path';
import zlib from 'node:zlib';
import crypto from 'node:crypto';
import { VisualPersonalizationWorker } from '../../../pde-platform/pde-ai-worker/src/visual-personalization-v1.js';

const root = path.resolve('pde-platform/frontend/dist-alcyone');
const spoolDir = process.env.VISUAL_FIXTURE_SPOOL;
if (!spoolDir || !path.resolve(spoolDir).startsWith(path.resolve('artifacts/visual-personalization') + path.sep))
  throw new Error('Spool local deve ficar nos artefatos da própria homologação.');
process.env.PDE_VISUAL_ALLOW_LOCAL_PROVIDER = 'true';
let calls = 0, callbacks = 0, loseNextCallback = false;
const requests = [];

/** Sintetiza PNG por código; não usa imagens externas nem chamadas de IA. */
function png(input) {
  const crc = buffer => {
    let value = 0xffffffff;
    for (const byte of buffer) {
      value ^= byte;
      for (let bit = 0; bit < 8; bit++) value = value & 1 ? (value >>> 1) ^ 0xedb88320 : value >>> 1;
    }
    return (value ^ 0xffffffff) >>> 0;
  };
  const chunk = (name, data) => {
    const type = Buffer.from(name), out = Buffer.alloc(data.length + 12);
    out.writeUInt32BE(data.length); type.copy(out, 4); data.copy(out, 8);
    out.writeUInt32BE(crc(Buffer.concat([type, data])), out.length - 4); return out;
  };
  const header = Buffer.alloc(13); header.writeUInt32BE(1024); header.writeUInt32BE(1024, 4); header[8] = 8; header[9] = 2;
  const color = crypto.createHash('sha256').update(input).digest();
  const rows = Buffer.alloc(1024 * (1 + 1024 * 3));
  for (let y = 0; y < 1024; y++) for (let x = 0; x < 1024; x++) {
    const at = y * (1 + 1024 * 3) + 1 + x * 3, index = Math.min(2, Math.floor(x / 342)) * 3;
    color.copy(rows, at, index, index + 3);
  }
  return Buffer.concat([Buffer.from([137,80,78,71,13,10,26,10]), chunk('IHDR', header), chunk('IDAT', zlib.deflateSync(rows)), chunk('IEND', Buffer.alloc(0))]).toString('base64');
}

const provider = http.createServer(async (req, res) => {
  const chunks = []; for await (const chunk of req) chunks.push(chunk);
  const request = JSON.parse(Buffer.concat(chunks));
  calls++; requests.push(request);
  const response = { id: 'fixture-native-' + calls, status: 'completed', model: 'gpt-5.6-sol', service_tier: 'flex',
    metadata: request.metadata, tools: request.tools,
    output: [{ type: 'image_generation_call', status: 'completed', result: png(request.input[2].content) }],
    usage: { input_tokens: 1000, output_tokens: 100, input_tokens_details: { cached_tokens: 0 } },
    tool_usage: { image_gen: { input_tokens: 100, output_tokens: 1000,
      input_tokens_details: { text_tokens: 100, image_tokens: 0 }, output_tokens_details: { image_tokens: 1000, text_tokens: 0 } } } };
  res.writeHead(200, { 'Content-Type': 'application/json' }); res.end(JSON.stringify(response));
});
provider.listen(18097, '127.0.0.1');

const fetchImpl = async (url, options) => {
  const response = await fetch(url, options);
  if (url.endsWith('/result')) {
    callbacks++;
    if (loseNextCallback) { loseNextCallback = false; return new Response('callback simulado perdido', { status: 503 }); }
  }
  return response;
};
const worker = () => new VisualPersonalizationWorker({ backendUrl: 'http://127.0.0.1:18096', internalToken: 'visual-local-only',
  apiKey: 'synthetic-local-only', providerUrl: 'http://127.0.0.1:18097/v1/responses', spoolDir, fetchImpl });
const server = http.createServer(async (req, res) => {
  try {
    const url = new URL(req.url, 'http://127.0.0.1:15184');
    if (url.pathname === '/fixture/stats') {
      res.writeHead(200, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ providerCalls: calls, callbacks, requests, spool: await fs.readdir(spoolDir).catch(() => []) })); return;
    }
    if (url.pathname === '/fixture/worker' && req.method === 'POST') {
      loseNextCallback = url.searchParams.get('loseCallback') === 'true';
      await worker().processNextPending(); res.writeHead(200); res.end('{}'); return;
    }
    if (url.pathname.startsWith('/api/pde/')) {
      const upstream = await fetch('http://127.0.0.1:18096' + url.pathname + url.search, { method: req.method, headers: req.headers });
      res.writeHead(upstream.status, { 'Content-Type': 'application/json', 'Cache-Control': 'private, no-store' });
      res.end(await upstream.text()); return;
    }
    const relative = url.pathname === '/' ? 'alcyone.html' : url.pathname.slice(1);
    const file = path.resolve(root, relative);
    if (!file.startsWith(root + path.sep)) { res.writeHead(400); res.end(); return; }
    const mime = file.endsWith('.html') ? 'text/html' : file.endsWith('.js') ? 'text/javascript' : file.endsWith('.css') ? 'text/css' : 'image/png';
    const contents = await fs.readFile(file);
    res.writeHead(200, { 'Content-Type': mime, 'Cache-Control': 'private, no-store', 'Referrer-Policy': 'no-referrer' });
    res.end(contents);
  } catch (error) {
    if (res.headersSent) { res.destroy(error); return; }
    res.writeHead(error.code === 'ENOENT' ? 404 : 503, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ detail: error.message }));
  }
});
server.listen(15184, '127.0.0.1');
const stop = () => { server.close(); provider.close(); process.exit(0); };
process.on('SIGTERM', stop); process.on('SIGINT', stop);
