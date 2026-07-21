import { expect, test } from '@playwright/test';

test('carrega a entrada visual do Clube MUSA', async ({ page }) => {
  await page.goto('/');

  await expect(page.getByAltText('Clube MUSA')).toBeVisible();
  await expect(page.getByRole('heading', { name: /sua imagem comunica/i })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Diagnóstico gratuito', exact: true })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Já tenho acesso', exact: true })).toBeVisible();
  await expect(page.getByRole('button', { name: /Ver meu Mapa de Presença/i })).toBeDisabled();
  await page.getByRole('button', { name: /Pareço comum mesmo tentando caprichar/i }).click();
  await page.getByRole('button', { name: /Presença elegante/i }).click();
  await expect(page.getByLabel(/Seu melhor e-mail para receber o Mapa de Presença/i)).toBeVisible();
  await expect(page.getByRole('button', { name: /Ver meu Mapa de Presença/i })).toBeEnabled();
  await expect(page.getByLabel(/Prévia da experiência Método MUSA/i)).toBeVisible();

  await page.screenshot({
    path: test.info().outputPath('musa-entry.png'),
    fullPage: true,
  });
});
