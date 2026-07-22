import { expect, test } from '@playwright/test';

test('carrega a entrada visual do Clube MUSA', async ({ page }) => {
  const completedGuidance = {
    requestId: 'diagnostico-visual-1',
    productSlug: 'metodo-musa-7-dias',
    missionId: 'diagnostico-presenca-publico',
    guidanceType: 'MUSA_PUBLIC_PRESENCE_DIAGNOSTIC',
    status: 'COMPLETED',
    headline: 'Seu plano começa reduzindo ruído visual',
    summary: 'A Consultora MUSA identificou que sua presença precisa de acabamento simples e repetível.',
    signals: ['Acabamento', 'Intenção', 'Coerência'],
    microActions: [
      'Dia 1: escolha uma ocasião real e retire um excesso visual.',
      'Dia 2: defina uma peça-sinal para repetir com intenção.',
      'Dia 3: combine duas cores de forma mais limpa.',
      'Dia 4: ajuste cabelo, pele ou acessório antes de sair.',
      'Dia 5: monte uma fórmula simples com o que já existe.',
      'Dia 6: repita a fórmula em uma situação importante.',
      'Dia 7: registre sua assinatura MUSA pessoal.',
    ],
    caution: 'Use como orientação prática, sem promessa automática de resultado universal.',
  };

  await page.route('/api/pde/public/presence-diagnostic', async (route) => {
    await route.fulfill({ json: completedGuidance });
  });
  await page.route('/api/pde/public/presence-diagnostic/diagnostico-visual-1', async (route) => {
    await route.fulfill({ json: completedGuidance });
  });
  await page.route('/api/pde/access/magic-link', async (route) => {
    await route.fulfill({
      json: {
        productSlug: 'metodo-musa-7-dias',
        email: 'teste+diagnostico@sandbox.local',
        deliveryStatus: 'SENT',
      },
    });
  });

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
  await page.getByRole('button', { name: /Enviar diagnóstico/i }).click();
  await expect(page.getByRole('heading', { name: /Seu plano começa reduzindo ruído visual/i })).toBeVisible();
  await expect(page.getByText(/Quer saber exatamente como executar esse plano/i)).toBeVisible();
  await page.getByPlaceholder('seuemail@exemplo.com').fill('teste+diagnostico@sandbox.local');
  await page.getByRole('button', { name: /Receber roteiro detalhado dos 7 dias/i }).click();
  await expect(page.getByText(/Enviei para seu e-mail/i)).toBeVisible();

  await page.screenshot({
    path: test.info().outputPath('musa-entry.png'),
    fullPage: true,
  });
});
