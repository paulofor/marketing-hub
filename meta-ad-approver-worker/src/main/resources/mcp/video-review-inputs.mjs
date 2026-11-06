import { extractRemoteVideoFrames } from './video-frame-extractor.mjs';
import { readFile, writeFile } from 'node:fs/promises';
import { join } from 'node:path';

// Reutiliza o decoder do MCP antes da inferência limitada, sem tools pagas ou parecer sintético.
const input = JSON.parse(await readFile(process.argv[2], 'utf8'));
const outputs = [];
for (const video of input.videos) {
  const evidence = await extractRemoteVideoFrames(video.url, { taskId: input.taskId, assetId: video.assetId }, video.reviewMoments ?? []);
  if (!/^[a-f0-9]{64}$/.test(video.sha256) || evidence.sha256 !== video.sha256)
    throw new Error('Hash do MP4 final divergente; não iniciar revisão.');
  const frames = [];
  for (let i = 0; i < evidence.frames.length; i++) {
    const name = `video-${video.assetId}-frame-${i + 1}.jpg`;
    await writeFile(join(process.argv[3], name), evidence.frames[i], { mode: 0o600 });
    frames.push({ fileName: name, second: evidence.frameTimes[i] });
  }
  outputs.push({ assetId: video.assetId, sha256: evidence.sha256, durationSeconds: evidence.duration,
    byteLength: evidence.byteLength, hashVerified: true, frames });
}
await writeFile(join(process.argv[3], 'video-evidence.json'), JSON.stringify(outputs), { mode: 0o600 });
