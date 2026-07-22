import { expect, test } from '@playwright/test';

test('carrega a entrada visual do Clube MUSA', async ({ page }) => {
  await page.goto('/');

  await expect(
    page.getByRole('heading', {
      name: /Sua imagem comunica a mulher/i,
      level: 1,
    }),
  ).toBeVisible();
  await expect(page.getByText('Diagnóstico de Presença')).toBeVisible();
  await expect(page.getByRole('button', { name: /Enviar diagnóstico/i })).toBeDisabled();
  await page.getByRole('button', { name: 'Trabalho ou reunião' }).click();
  await page.getByRole('button', { name: 'Falta acabamento' }).click();
  await page.getByRole('button', { name: 'Elegância discreta' }).click();
  await page.getByRole('button', { name: 'Pouco tempo' }).click();
  await page.getByRole('button', { name: 'Cabelo e pele' }).click();
  await expect(page.getByRole('button', { name: /Enviar diagnóstico/i })).toBeEnabled();

  await page.screenshot({
    path: test.info().outputPath('musa-entry.png'),
    fullPage: true,
  });
});
