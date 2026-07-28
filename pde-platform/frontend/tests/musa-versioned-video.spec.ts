import { expect, test } from '@playwright/test';

const approvedHeroVideoUrl = '/assets/hls/musa-v6-microexperiencia-visivel/index.m3u8';

test('v6 publica bloco de video nao-slide e segue direto para o diagnostico', async ({ page }) => {
  await page.route('**/api/pde/products/metodo-musa-7-dias', async (route) => {
    await route.fulfill({ status: 404, body: 'not found' });
  });

  await page.goto('http://v6.clubemusa.com.br:57180/?mh_preview=qa', { waitUntil: 'domcontentloaded' });

  await expect(page.getByRole('heading', { name: /look parecer incompleto/i })).toBeVisible();
  await expect(page.getByRole('region', { name: 'Vídeo curto Método MUSA' })).toBeVisible();
  await expect(page.locator('video.public-hero-video')).toHaveCount(1);
  await expect(page.locator('video.public-hero-video')).toHaveAttribute(
    'src',
    approvedHeroVideoUrl,
  );
  await expect(page.locator('video.public-hero-video')).toHaveJSProperty('muted', true);
  await expect(page.locator('video.public-hero-video')).toHaveJSProperty('controls', true);
  await expect(page.locator('video.public-hero-video')).toHaveJSProperty('autoplay', true);
  await expect(page.locator('video.public-hero-video')).toHaveJSProperty('loop', false);
  await expect(page.locator('.public-video-play-badge')).toHaveCount(0);
  await expect(page.locator('.public-video-watch-status')).toHaveCount(0);
  await expect(page.getByRole('region', { name: 'Diagnóstico de Presença' })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Falta acabamento' })).toBeVisible();
  await expect(page.getByRole('button', { name: /Ver meu primeiro passo/i })).toBeDisabled();
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

  await expect(page.getByRole('heading', { name: /look parecer incompleto/i })).toBeVisible();
  await expect(page.getByRole('region', { name: 'Vídeo curto Método MUSA' })).toBeVisible();
  await expect(page.locator('video.public-hero-video')).toHaveAttribute(
    'src',
    approvedHeroVideoUrl,
  );
});
