import fs from 'node:fs/promises';
import { createRequire } from 'node:module';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const require = createRequire(import.meta.url);
const playwrightPackagePath = process.env.NODE_PATH
  ? path.join(process.env.NODE_PATH, '@playwright/test')
  : '@playwright/test';
const { chromium } = require(playwrightPackagePath);

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const rootDir = path.resolve(__dirname, '..');
const assetsDir = path.join(rootDir, 'public', 'assets');
const sourceImage = path.join(assetsDir, 'musa-editorial-presenca.png');

const slides = [
  {
    fileName: 'musa-diagnostic-slide-1.png',
    position: '52% 46%',
    scale: 1.05,
    warmth: 'rgba(122, 36, 68, 0.28)',
    light: 'rgba(247, 228, 198, 0.28)',
    label: 'presenca',
  },
  {
    fileName: 'musa-diagnostic-slide-2.png',
    position: '39% 50%',
    scale: 1.22,
    warmth: 'rgba(111, 122, 82, 0.18)',
    light: 'rgba(255, 248, 243, 0.34)',
    label: 'ruido',
  },
  {
    fileName: 'musa-diagnostic-slide-3.png',
    position: '63% 42%',
    scale: 1.2,
    warmth: 'rgba(122, 36, 68, 0.2)',
    light: 'rgba(214, 167, 92, 0.26)',
    label: 'sinal',
  },
  {
    fileName: 'musa-diagnostic-slide-4.png',
    position: '50% 58%',
    scale: 1.14,
    warmth: 'rgba(47, 42, 44, 0.24)',
    light: 'rgba(243, 201, 193, 0.28)',
    label: 'rotina',
  },
  {
    fileName: 'musa-diagnostic-slide-5.png',
    position: '57% 48%',
    scale: 1.08,
    warmth: 'rgba(122, 36, 68, 0.24)',
    light: 'rgba(247, 228, 198, 0.34)',
    label: 'plano',
  },
];

function buildHtml(slide, sourceDataUrl) {
  return `<!doctype html>
<html>
  <head>
    <meta charset="utf-8" />
    <style>
      html,
      body {
        margin: 0;
        width: 1080px;
        height: 1920px;
        overflow: hidden;
        background: #2f2a2c;
      }

      .frame {
        position: relative;
        width: 1080px;
        height: 1920px;
        overflow: hidden;
        background: #2f2a2c;
      }

      .photo {
        position: absolute;
        inset: 0;
        width: 100%;
        height: 100%;
        object-fit: cover;
        object-position: ${slide.position};
        transform: scale(${slide.scale});
        filter: saturate(1.08) contrast(1.04) brightness(0.96);
      }

      .frame::before {
        content: '';
        position: absolute;
        inset: 0;
        background:
          radial-gradient(circle at 74% 20%, ${slide.light}, transparent 30%),
          linear-gradient(180deg, rgba(47, 42, 44, 0.04) 0%, rgba(47, 42, 44, 0.16) 42%, rgba(32, 20, 25, 0.78) 100%),
          linear-gradient(90deg, ${slide.warmth} 0%, rgba(122, 36, 68, 0.06) 42%, rgba(47, 42, 44, 0.12) 100%);
        z-index: 1;
      }

      .frame::after {
        border: 2px solid rgba(247, 228, 198, 0.38);
        content: '';
        inset: 48px;
        position: absolute;
        z-index: 2;
      }

      .mark {
        position: absolute;
        left: 76px;
        bottom: 154px;
        width: 214px;
        height: 8px;
        background: #d6a75c;
        border-radius: 999px;
        box-shadow: 0 0 34px rgba(214, 167, 92, 0.32);
        z-index: 3;
      }

      .orb {
        position: absolute;
        right: -150px;
        bottom: -210px;
        width: 560px;
        height: 560px;
        border-radius: 50%;
        background: rgba(122, 36, 68, 0.32);
        z-index: 2;
      }

      .signature {
        position: absolute;
        top: 76px;
        left: 76px;
        color: rgba(255, 248, 243, 0.82);
        font: 800 28px Inter, Arial, sans-serif;
        letter-spacing: 0;
        text-transform: uppercase;
        z-index: 3;
      }
    </style>
  </head>
  <body>
    <main class="frame" data-slide="${slide.label}">
      <img class="photo" src="${sourceDataUrl}" />
      <div class="signature">Clube MUSA</div>
      <div class="mark"></div>
      <div class="orb"></div>
    </main>
  </body>
</html>`;
}

const browser = await chromium.launch({
  executablePath: process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH || process.env.CHROMIUM_BIN || process.env.CHROME_BIN || '/usr/bin/chromium',
});

try {
  const sourceBuffer = await fs.readFile(sourceImage);
  const sourceDataUrl = `data:image/png;base64,${sourceBuffer.toString('base64')}`;
  const page = await browser.newPage({ viewport: { width: 1080, height: 1920 }, deviceScaleFactor: 1 });

  for (const slide of slides) {
    await page.setContent(buildHtml(slide, sourceDataUrl), { waitUntil: 'load' });
    await page.waitForFunction(() => {
      const image = document.querySelector('.photo');
      return image instanceof HTMLImageElement && image.complete && image.naturalWidth > 0;
    });
    const outputPath = path.join(assetsDir, slide.fileName);
    await page.screenshot({ path: outputPath, type: 'png', fullPage: false });
  }
} finally {
  await browser.close();
}

await fs.writeFile(
  path.join(assetsDir, 'musa-diagnostic-assets-manifest.json'),
  JSON.stringify(
    {
      source: 'musa-editorial-presenca.png',
      generatedBy: 'scripts/generate-musa-diagnostic-assets.mjs',
      purpose: 'Slides inspiradores para o diagnostico publico MUSA',
      slides: slides.map(({ fileName, label }) => ({ fileName, label })),
    },
    null,
    2,
  ),
  'utf8',
);
