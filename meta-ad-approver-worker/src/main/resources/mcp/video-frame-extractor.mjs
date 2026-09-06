import { execFile } from 'node:child_process';
import { mkdtemp, readFile, rm, writeFile } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { promisify } from 'node:util';

const execFileAsync = promisify(execFile);
const FFMPEG_PATH = '/usr/local/bin/ffmpeg';
const FFPROBE_PATH = '/usr/local/bin/ffprobe';
const MAX_VIDEO_BYTES = 64 * 1024 * 1024;
const MAX_FRAME_BYTES = 12 * 1024 * 1024;
const PROCESS_TIMEOUT_MS = 120000;

export async function extractRemoteVideoFrames(url, correlation = {}) {
  const requestedAt = new Date().toISOString();
  process.stderr.write(
    `${JSON.stringify({ operation: 'download_video_for_review', phase: 'request', url, requestedAt, ...correlation })}\n`,
  );
  try {
    const response = await fetch(url, { signal: AbortSignal.timeout(120000) });
    process.stderr.write(
      `${JSON.stringify({ operation: 'download_video_for_review', phase: 'response', url, status: response.status, contentType: response.headers.get('content-type'), contentLength: response.headers.get('content-length'), requestedAt, receivedAt: new Date().toISOString(), ...correlation })}\n`,
    );
    if (!response.ok) {
      throw new Error(`Mídia respondeu HTTP ${response.status}`);
    }
    const declaredLength = Number(response.headers.get('content-length'));
    if (Number.isFinite(declaredLength) && declaredLength > MAX_VIDEO_BYTES) {
      throw new Error(
        `Vídeo excede o limite de ${MAX_VIDEO_BYTES} bytes para inspeção.`,
      );
    }
    const bytes = await readLimitedBody(response);
    return await extractVideoFrames(bytes);
  } catch (error) {
    process.stderr.write(
      `${JSON.stringify({ operation: 'download_video_for_review', phase: 'failure', url, requestedAt, failedAt: new Date().toISOString(), error: error instanceof Error ? (error.stack ?? error.message) : String(error), ...correlation })}\n`,
    );
    throw error;
  }
}

export async function validateVideoDecoder() {
  const directory = await mkdtemp(join(tmpdir(), 'temis-video-runtime-'));
  const fixture = join(directory, 'fixture.mp4');
  try {
    await execFileAsync(
      FFMPEG_PATH,
      [
        '-v',
        'error',
        '-f',
        'lavfi',
        '-i',
        'color=c=0x7656ff:s=320x180:r=10:d=1',
        '-c:v',
        'libx264',
        '-pix_fmt',
        'yuv420p',
        '-movflags',
        '+faststart',
        '-y',
        fixture,
      ],
      {
        maxBuffer: MAX_FRAME_BYTES,
        timeout: PROCESS_TIMEOUT_MS,
        killSignal: 'SIGKILL',
      },
    );
    const evidence = await extractVideoFrames(await readFile(fixture));
    let invalidVideoBlocked = false;
    try {
      await extractVideoFrames(Buffer.from('conteudo-invalido'));
    } catch {
      invalidVideoBlocked = true;
    }
    if (!invalidVideoBlocked) {
      throw new Error('O decoder aceitou um arquivo de vídeo inválido.');
    }
    return {
      duration: evidence.duration,
      frameBytes: evidence.frames.map((frame) => frame.length),
      invalidVideoBlocked,
    };
  } finally {
    await rm(directory, { recursive: true, force: true });
  }
}

async function readLimitedBody(response) {
  if (!response.body) {
    throw new Error('Mídia respondeu sem conteúdo para inspeção.');
  }
  const reader = response.body.getReader();
  const chunks = [];
  let total = 0;
  while (true) {
    const { done, value } = await reader.read();
    if (done) break;
    total += value.length;
    if (total > MAX_VIDEO_BYTES) {
      await reader.cancel();
      throw new Error(
        `Vídeo excede o limite de ${MAX_VIDEO_BYTES} bytes para inspeção.`,
      );
    }
    chunks.push(Buffer.from(value));
  }
  return Buffer.concat(chunks, total);
}

async function extractVideoFrames(bytes) {
  if (!Buffer.isBuffer(bytes) || bytes.length === 0) {
    throw new Error('Vídeo vazio recebido para inspeção.');
  }
  if (bytes.length > MAX_VIDEO_BYTES) {
    throw new Error(
      `Vídeo excede o limite de ${MAX_VIDEO_BYTES} bytes para inspeção.`,
    );
  }
  const directory = await mkdtemp(join(tmpdir(), 'temis-video-'));
  const input = join(directory, 'input.mp4');
  try {
    await writeFile(input, bytes, { mode: 0o600 });
    const duration = await probeDuration(input);
    const frames = [];
    for (const position of [0.1, 0.5, 0.9]) {
      frames.push(await extractFrame(input, Math.max(0, duration * position)));
    }
    return { duration, frames };
  } finally {
    await rm(directory, { recursive: true, force: true });
  }
}

async function probeDuration(input) {
  const { stdout } = await execFileAsync(
    FFPROBE_PATH,
    [
      '-v',
      'error',
      '-show_entries',
      'format=duration',
      '-of',
      'default=noprint_wrappers=1:nokey=1',
      input,
    ],
    {
      encoding: 'utf8',
      maxBuffer: 64 * 1024,
      timeout: PROCESS_TIMEOUT_MS,
      killSignal: 'SIGKILL',
    },
  );
  const duration = Number(stdout.trim());
  if (!Number.isFinite(duration) || duration <= 0) {
    throw new Error('FFprobe não identificou uma duração válida para o vídeo.');
  }
  return duration;
}

async function extractFrame(input, second) {
  const { stdout } = await execFileAsync(
    FFMPEG_PATH,
    [
      '-v',
      'error',
      '-ss',
      second.toFixed(3),
      '-i',
      input,
      '-frames:v',
      '1',
      '-f',
      'image2pipe',
      '-vcodec',
      'mjpeg',
      'pipe:1',
    ],
    {
      encoding: 'buffer',
      maxBuffer: MAX_FRAME_BYTES,
      timeout: PROCESS_TIMEOUT_MS,
      killSignal: 'SIGKILL',
    },
  );
  if (!Buffer.isBuffer(stdout) || stdout.length === 0) {
    throw new Error(`FFmpeg não extraiu o quadro em ${second.toFixed(3)}s.`);
  }
  return stdout;
}
