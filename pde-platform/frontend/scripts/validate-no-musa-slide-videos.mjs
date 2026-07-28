import fs from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const rootDir = path.resolve(__dirname, '..');
const assetsDir = path.join(rootDir, 'public', 'assets');

const forbiddenAssetPaths = [
  'musa-v5-video-explicativo.mp4',
  'musa-v5-video-explicativo-manifest.json',
  'musa-v6-video-motivacional.mp4',
  'musa-v6-video-motivacional-manifest.json',
  path.join('hls', 'musa-v5-video-explicativo'),
  path.join('hls', 'musa-v6-video-motivacional'),
];

const existingForbiddenAssets = [];
for (const assetPath of forbiddenAssetPaths) {
  const fullPath = path.join(assetsDir, assetPath);
  try {
    await fs.access(fullPath);
    existingForbiddenAssets.push(assetPath);
  } catch (error) {
    if (error?.code !== 'ENOENT') {
      throw error;
    }
  }
}

if (existingForbiddenAssets.length > 0) {
  throw new Error(
    [
      'Videos MUSA gerados a partir de slides estao bloqueados.',
      'Use somente videos comerciais produzidos pelo fluxo versionado de video do Marketing Hub.',
      `Assets proibidos encontrados: ${existingForbiddenAssets.join(', ')}`,
    ].join(' '),
  );
}
