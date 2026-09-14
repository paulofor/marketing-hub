// Confere reprodução da mídia sintética local, sem confundi-la com avaliação de voz real.
const assert = require('node:assert/strict');
const fs = require('node:fs/promises');
const path = require('node:path');
const { pathToFileURL } = require('node:url');
const { chromium, devices } = require('@playwright/test');

(async () => {
  const output = path.resolve(process.argv[2]);
  const browser = await chromium.launch({ executablePath: '/usr/bin/chromium', args: ['--no-sandbox'] });
  const results = [];
  try {
    for (const [name, device] of [
      ['desktop', { viewport: { width: 1440, height: 1000 } }],
      ['iphone', devices['iPhone 15 Pro']],
      ['pixel', devices['Pixel 7']],
    ]) {
      const context = await browser.newContext(device);
      const page = await context.newPage();
      await page.goto(pathToFileURL(path.join(output, 'player.html')).href);
      await page.waitForFunction(() => document.querySelector('video').readyState >= 2);
      const result = await page.evaluate(async () => {
        const video = document.querySelector('video');
        video.muted = true;
        await video.play();
        await new Promise(resolve => setTimeout(resolve, 350));
        const advanced = video.currentTime > 0 && !video.paused;
        video.pause();
        video.currentTime = 6;
        await new Promise(resolve => video.addEventListener('seeked', resolve, { once: true }));
        return { advanced, controls: video.controls, duration: video.duration,
          width: video.videoWidth, height: video.videoHeight, error: video.error?.code ?? null,
          fitsViewport: video.getBoundingClientRect().right <= innerWidth };
      });
      assert.equal(result.advanced, true);
      assert.equal(result.controls, true);
      assert.equal(result.fitsViewport, true);
      assert.equal(result.error, null);
      assert.equal(result.width, 1080);
      assert.equal(result.height, 1920);
      assert.ok(Math.abs(result.duration - 15) < 0.15);
      await page.screenshot({ path: path.join(output, `player-${name}.png`) });
      results.push({ device: name, syntheticTestOnly: true, ...result });
      await context.close();
    }
  } finally { await browser.close(); }
  await fs.writeFile(path.join(output, 'player-results.json'), JSON.stringify(results, null, 2));
  console.log(JSON.stringify(results));
})().catch(error => { console.error(error); process.exitCode = 1; });
