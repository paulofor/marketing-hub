import { chromium } from 'playwright-core';
import { validateVideoDecoder } from './video-frame-extractor.mjs';

const browser = await chromium.launch({
  headless: true,
  args: ['--no-sandbox', '--disable-dev-shm-usage', '--disable-gpu'],
});

try {
  const page = await browser.newPage({ viewport: { width: 390, height: 844 } });
  await page.setContent('<main><h1>Gate visual operacional</h1></main>');
  const screenshot = await page.screenshot({ type: 'png' });
  if (screenshot.length === 0) {
    throw new Error('O navegador não produziu a evidência visual de runtime.');
  }
  const video = await validateVideoDecoder();
  process.stdout.write(
    `${JSON.stringify({ browser: await browser.version(), screenshotBytes: screenshot.length, video })}\n`,
  );
} finally {
  await browser.close();
}
