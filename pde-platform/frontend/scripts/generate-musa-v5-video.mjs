import fs from 'node:fs/promises';
import path from 'node:path';
import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const rootDir = path.resolve(__dirname, '..');
const assetsDir = path.join(rootDir, 'public', 'assets');

const sourceSlides = [
  'musa-diagnostic-slide-1.png',
  'musa-diagnostic-slide-2.png',
  'musa-diagnostic-slide-3.png',
  'musa-diagnostic-slide-4.png',
  'musa-diagnostic-slide-5.png',
].map((fileName) => path.join(assetsDir, fileName));

const videos = [
  {
    output: 'musa-v5-video-explicativo.mp4',
    manifest: 'musa-v5-video-explicativo-manifest.json',
    purpose: 'Video explicativo inicial da versao v5 do Clube MUSA.',
  },
  {
    output: 'musa-v6-video-motivacional.mp4',
    manifest: 'musa-v6-video-motivacional-manifest.json',
    purpose: 'Video motivacional inicial da versao v6 do Clube MUSA.',
  },
];

async function assertSourcesExist() {
  for (const sourceSlide of sourceSlides) {
    await fs.access(sourceSlide);
  }
}

function runFfmpeg(outputVideo) {
  const inputs = sourceSlides.flatMap((sourceSlide) => ['-loop', '1', '-t', '3.2', '-i', sourceSlide]);
  const filter = sourceSlides
    .map((_sourceSlide, index) => `[${index}:v]scale=1080:1920:force_original_aspect_ratio=increase,crop=1080:1920,setsar=1,format=yuv420p[v${index}]`)
    .join(';')
    + `;${sourceSlides.map((_sourceSlide, index) => `[v${index}]`).join('')}concat=n=${sourceSlides.length}:v=1:a=0,`
    + 'fps=30,scale=720:1280[v]';

  const result = spawnSync(
    'ffmpeg',
    [
      '-y',
      ...inputs,
      '-filter_complex',
      filter,
      '-map',
      '[v]',
      '-c:v',
      'libx264',
      '-profile:v',
      'main',
      '-level',
      '3.1',
      '-pix_fmt',
      'yuv420p',
      '-movflags',
      '+faststart',
      outputVideo,
    ],
    { encoding: 'utf8' },
  );

  if (result.status !== 0) {
    throw new Error(`ffmpeg falhou ao gerar o video MUSA: ${result.stderr}`);
  }
}

await assertSourcesExist();
for (const video of videos) {
  const outputVideo = path.join(assetsDir, video.output);
  runFfmpeg(outputVideo);
  await fs.writeFile(
    path.join(assetsDir, video.manifest),
    JSON.stringify(
      {
        generatedBy: 'scripts/generate-musa-v5-video.mjs',
        output: video.output,
        purpose: video.purpose,
        sourceSlides: sourceSlides.map((sourceSlide) => path.basename(sourceSlide)),
        durationSeconds: 16,
        format: 'mp4/h264',
      },
      null,
      2,
    ),
    'utf8',
  );
}
