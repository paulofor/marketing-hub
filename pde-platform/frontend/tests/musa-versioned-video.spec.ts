import { expect, test } from '@playwright/test';

test('v6 publica PDE sem video de slides e segue direto para o diagnostico', async ({ page }) => {
  await page.route('**/api/pde/products/metodo-musa-7-dias', async (route) => {
    await route.fulfill({ status: 404, body: 'not found' });
  });

  await page.goto('http://v6.clubemusa.com.br:57180/?mh_preview=qa');

  await expect(page.getByRole('heading', { name: /Descubra em 30 segundos/i })).toBeVisible();
  await expect(page.getByRole('region', { name: 'Vídeo curto Método MUSA' })).toHaveCount(0);
  await expect(page.locator('video.public-hero-video')).toHaveCount(0);
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

  await page.goto('http://v6.clubemusa.com.br:57180/?mh_preview=qa');

  await expect(page.getByRole('heading', { name: /Descubra em 30 segundos/i })).toBeVisible();
  await expect(page.getByRole('region', { name: 'Vídeo curto Método MUSA' })).toHaveCount(0);
  await expect(page.locator('video.public-hero-video')).toHaveCount(0);
});
