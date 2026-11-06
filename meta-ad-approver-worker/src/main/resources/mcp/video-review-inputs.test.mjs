import { test } from 'node:test';
import assert from 'node:assert/strict';
import { mkdtemp, writeFile, readFile, rm } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { createHash } from 'node:crypto';
import { createServer } from 'node:http';
import { execFile } from 'node:child_process';
import { promisify } from 'node:util';
const run = promisify(execFile);

// Usa MP4 sintético e HTTP local: nenhuma inferência ou parecer é fabricado.
test('CLI anexa quadros nos instantes pedidos e recusa outro hash antes da revisão', async () => {
  const directory = await mkdtemp(join(tmpdir(), 'temis-frame-contract-'));
  const ffmpeg = process.env.FFMPEG_BIN || 'ffmpeg';
  const ffprobe = process.env.FFPROBE_BIN || 'ffprobe';
  let server;
  try {
    const media = join(directory, 'fixture.mp4');
    await run(ffmpeg, ['-v', 'error', '-y', '-f', 'lavfi', '-i', 'testsrc2=s=320x568:r=10:d=4', '-c:v', 'libx264', '-threads', '1', '-pix_fmt', 'yuv420p', media]);
    const bytes = await readFile(media);
    server = createServer((request, response) => { response.writeHead(200, { 'content-type': 'video/mp4' }); response.end(bytes); });
    await new Promise(resolve => server.listen(0, '127.0.0.1', resolve));
    const input = join(directory, 'input.json');
    const video = { assetId: 91001, sha256: createHash('sha256').update(bytes).digest('hex'), url: `http://127.0.0.1:${server.address().port}/fixture.mp4`, reviewMoments: [0.5, 1.5, 3.5] };
    await writeFile(input, JSON.stringify({ taskId: 91002, videos: [video] }));
    const cli = new URL('./video-review-inputs.mjs', import.meta.url).pathname;
    const options = { env: { ...process.env, FFMPEG_BIN: ffmpeg, FFPROBE_BIN: ffprobe } };
    await run(process.execPath, [cli, input, directory], options);
    const result = JSON.parse(await readFile(join(directory, 'video-evidence.json'), 'utf8'))[0];
    assert.equal(result.sha256, video.sha256); assert.equal(result.hashVerified, true);
    assert.ok(result.frames.length >= 3);
    for (const second of video.reviewMoments) assert.ok(result.frames.some(frame => Math.abs(frame.second - second) < 0.01));
    video.sha256 = 'a'.repeat(64);
    await writeFile(input, JSON.stringify({ taskId: 91002, videos: [video] }));
    await assert.rejects(run(process.execPath, [cli, input, directory], options), /Hash do MP4 final divergente/);
  } finally {
    if (server) await new Promise(resolve => server.close(resolve));
    await rm(directory, { recursive: true, force: true });
  }
});
