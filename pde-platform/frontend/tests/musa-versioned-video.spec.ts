import { expect, test } from '@playwright/test';

test('v6 publica video motivacional como ativo real', async ({ page }) => {
  await page.route('**/api/pde/products/metodo-musa-7-dias', async (route) => {
    await route.fulfill({ status: 404, body: 'not found' });
  });

  await page.goto('http://v6.clubemusa.com.br:57180/?mh_preview=qa');

  const video = page.locator('video.public-hero-video');
  await expect(video, 'A v6 deve renderizar o player de video no topo do diagnostico publico').toBeVisible();
  await expect(video, 'A v6 deve apontar para o MP4 motivacional gerado no build').toHaveAttribute(
    'src',
    '/assets/musa-v6-video-motivacional.mp4',
  );
  await expect(page.locator('.public-video-play-badge span')).toHaveText('Vídeo rápido');
});
