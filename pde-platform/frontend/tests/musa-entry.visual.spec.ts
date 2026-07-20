import { expect, test } from '@playwright/test';

test('carrega a entrada visual do Clube MUSA', async ({ page }) => {
  await page.goto('/');

  await expect(page.getByAltText('Clube MUSA')).toBeVisible();
  await expect(page.getByRole('heading', { name: /descubra o detalhe/i })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Diagnóstico gratuito', exact: true })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Já tenho acesso', exact: true })).toBeVisible();
  await expect(page.getByRole('button', { name: /Receber meu resultado gratuito/i })).toBeDisabled();
  await page.getByRole('button', { name: /Roupa sem intenção/i }).click();
  await page.getByRole('button', { name: /Parecer mais elegante/i }).click();
  await expect(page.getByLabel(/Seu melhor e-mail para receber o resultado/i)).toBeVisible();
  await expect(page.getByRole('button', { name: /Receber meu resultado gratuito/i })).toBeEnabled();
  await expect(page.getByLabel(/Prévia da experiência Método MUSA/i)).toBeVisible();

  await page.screenshot({
    path: test.info().outputPath('musa-entry.png'),
    fullPage: true,
  });
});
