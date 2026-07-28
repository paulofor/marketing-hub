import { expect, test } from '@playwright/test';

const approvedHeroVideoUrl = '/assets/hls/musa-v6-microexperiencia-visivel/index.m3u8';

test('v6 publica bloco de video nao-slide e segue direto para o diagnostico', async ({ page }) => {
  await page.route('**/api/pde/products/metodo-musa-7-dias', async (route) => {
    await route.fulfill({ status: 404, body: 'not found' });
  });

  await page.goto('http://v6.clubemusa.com.br:57180/?mh_preview=qa', { waitUntil: 'domcontentloaded' });

  await expect(page.getByRole('heading', { name: /tirando elegância/i })).toBeVisible();
  await expect(page.getByRole('region', { name: 'Vídeo curto Método MUSA' })).toBeVisible();
  await expect(page.locator('video.public-hero-video')).toHaveCount(1);
  await expect(page.locator('video.public-hero-video')).toHaveAttribute(
    'src',
    approvedHeroVideoUrl,
  );
  await expect(page.locator('video.public-hero-video')).toHaveJSProperty('muted', false);
  await expect(page.locator('video.public-hero-video')).toHaveJSProperty('controls', true);
  await expect(page.locator('video.public-hero-video')).toHaveJSProperty('autoplay', false);
  await expect(page.locator('video.public-hero-video')).toHaveJSProperty('loop', false);
  await expect(page.locator('video.public-hero-video')).not.toHaveAttribute('poster');
  await expect(page.locator('video.public-hero-video')).toBeVisible();
  await expect(page.locator('video.public-hero-video')).toHaveCSS('pointer-events', 'auto');
  const videoReceivesPointer = await page.locator('video.public-hero-video').evaluate((video) => {
    const rect = video.getBoundingClientRect();
    const elementAtVideoCenter = document.elementFromPoint(rect.left + rect.width / 2, rect.top + rect.height / 2);
    return elementAtVideoCenter === video;
  });
  expect(videoReceivesPointer).toBe(true);
  await expect(page.locator('.public-video-play-badge')).toHaveCount(0);
  await expect(page.locator('.public-video-watch-status')).toHaveCount(0);
  await expect(page.getByRole('region', { name: 'Diagnóstico de Presença' })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Falta acabamento' })).toBeVisible();
  await expect(page.getByRole('button', { name: /Ver meu ajuste MUSA/i })).toBeDisabled();
});

test('v6 bloqueia override global para HLS antigo de slides', async ({ page }) => {
  await page.addInitScript(() => {
    window.__MUSA_RUNTIME_CONFIG__ = {
      VITE_MUSA_EXPERIENCE_VERSION_OVERRIDE: 'musa-pde-entry-v5-video-explicativo',
      VITE_MUSA_HERO_STREAM_URL: '/assets/hls/musa-v5-video-explicativo/index.m3u8',
    };
  });
  await page.route('**/api/pde/products/metodo-musa-7-dias', async (route) => {
    await route.fulfill({ status: 404, body: 'not found' });
  });

  await page.goto('http://v6.clubemusa.com.br:57180/?mh_preview=qa', { waitUntil: 'domcontentloaded' });

  await expect(page.getByRole('heading', { name: /tirando elegância/i })).toBeVisible();
  await expect(page.getByRole('region', { name: 'Vídeo curto Método MUSA' })).toBeVisible();
  await expect(page.locator('video.public-hero-video')).toHaveAttribute(
    'src',
    approvedHeroVideoUrl,
  );
});
