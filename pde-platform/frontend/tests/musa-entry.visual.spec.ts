import { expect, test } from '@playwright/test';

test('carrega a entrada visual do Clube MUSA', async ({ page }) => {
  await page.goto('/');

  await expect(page.getByText('Clube MUSA')).toBeVisible();
  await expect(page.getByRole('heading', { name: /descubra o detalhe/i })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Entrar' })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Primeiro acesso', exact: true })).toBeVisible();
  await expect(page.getByLabel(/Prévia da experiência Método MUSA/i)).toBeVisible();

  await page.screenshot({
    path: test.info().outputPath('musa-entry.png'),
    fullPage: true,
  });
});
