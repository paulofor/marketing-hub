import { expect, test } from '@playwright/test';

test('health publico renderiza app, javascript e texto comercial', async ({ page, request }) => {
  const pageErrors: string[] = [];

  page.on('pageerror', (error) => {
    pageErrors.push(error.message);
  });

  const staticHealth = await request.get('/healthz');
  expect(staticHealth.ok()).toBeTruthy();
  expect(await staticHealth.text()).toContain('"status":"UP"');

  const response = await page.goto('/', { waitUntil: 'networkidle' });
  expect(response?.ok()).toBeTruthy();

  await expect(page.locator('#root').locator(':scope > *').first()).toBeVisible();
  await expect(
    page.getByRole('heading', {
      name: /Sua imagem comunica a mulher/i,
      level: 1,
    }),
  ).toBeVisible();
  await expect(page.getByText('Diagnóstico de Presença')).toBeVisible();
  await expect(page.getByRole('button', { name: /Enviar diagnóstico/i })).toBeVisible();
  expect(await page.locator('script[type="module"][src]').count()).toBeGreaterThan(0);

  expect(pageErrors, `Erros de execucao no health publico: ${pageErrors.join(' | ')}`).toEqual([]);
});
