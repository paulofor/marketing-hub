import { expect, test } from '@playwright/test';

test('carrega a entrada visual do Clube MUSA', async ({ page }) => {
  const isVideoHero = process.env.VITE_MUSA_EXPERIENCE_VERSION_OVERRIDE === 'musa-pde-entry-v4-video-hero';
  const requireRealVideo = process.env.REQUIRE_MUSA_HERO_VIDEO === 'true';

  await page.goto('/');

  await expect(page.getByAltText('Clube MUSA')).toBeVisible();
  await expect(
    page.getByRole('heading', {
      name: /Sua imagem comunica a mulher/i,
      level: 1,
    }),
  ).toBeVisible();
  await expect(page.getByRole('button', { name: 'Diagnóstico gratuito', exact: true })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Já tenho acesso', exact: true })).toBeVisible();
  await expect(page.getByRole('button', { name: /Receber meu Mapa de Presença/i })).toBeDisabled();
  await page
    .getByRole('button', {
      name: /Descobrir o que minha imagem comunica hoje/i,
    })
    .click();
  await expect(page.getByLabel(/Seu melhor e-mail para receber o Mapa de Presença/i)).toBeVisible();
  await expect(page.getByRole('button', { name: /Receber meu Mapa de Presença/i })).toBeEnabled();
  if (isVideoHero) {
    await expect(page.getByLabel(/Vídeo da experiência Método MUSA/i)).toBeVisible();
    if (requireRealVideo) {
      const video = page.locator('video.musa-hero-video');
      await expect(video).toBeVisible();
      await expect(video).toHaveAttribute('src', /.+/);
    } else {
      await expect(page.getByText('Prévia visual')).toBeVisible();
    }
  } else {
    await expect(page.getByLabel(/Prévia da experiência Método MUSA/i)).toBeVisible();
  }

  await page.screenshot({
    path: test.info().outputPath('musa-entry.png'),
    fullPage: true,
  });
});
