import { expect, test } from '@playwright/test';

const productSlug = 'metodo-musa-7-dias';
const v5ExperienceVersion = 'musa-pde-entry-v5-video-explicativo';
const v6ExperienceVersion = 'musa-pde-entry-v6-video-motivacional';
const v7ExperienceVersion = 'musa-pde-entry-v7-espelho-antes-de-sair';
const backendBaseUrl = process.env.PDE_TEST_BACKEND_URL ?? 'http://127.0.0.1:8096';
const frontendBaseUrl = process.env.PDE_TEST_FRONTEND_URL ?? 'http://127.0.0.1:57180';
const internalToken = process.env.PDE_TEST_INTERNAL_TOKEN ?? 'pde-local-internal-test';
const internalHeaders = { 'X-PDE-Internal-Token': internalToken };

function versionedFrontendUrl(version: string, pathAndQuery: string) {
  const url = new URL(pathAndQuery, frontendBaseUrl);
  url.searchParams.set('experienceVersion', version);
  return url.toString();
}

test.beforeEach(async ({ request }) => {
  const response = await request.post(`${backendBaseUrl}/api/pde/access/analytics/${productSlug}/reset-campaign-start`, { headers: internalHeaders });
  expect(response.ok()).toBeTruthy();
});

test('v5, v6 e v7 usam backend PDE local real sem misturar contratos versionados', async ({ page, request }) => {
  await page.goto(versionedFrontendUrl(v5ExperienceVersion, '/?utm_source=local&utm_campaign=v5_local_validation'));
  await expect(page.getByRole('region', { name: 'Vídeo curto Método MUSA' })).toHaveCount(0);
  await expect(page.locator('video.public-hero-video')).toHaveCount(0);
  await expect.poll(async () => {
    const response = await request.get(
      `${backendBaseUrl}/api/pde/access/analytics/${productSlug}/summary?experienceVersion=${encodeURIComponent(v5ExperienceVersion)}`,
    );
    const summary = await response.json();
    return summary.currentExperienceVersion === v5ExperienceVersion && summary.rawTotalEvents > 0;
  }).toBeTruthy();

  await request.post(`${backendBaseUrl}/api/pde/access/analytics/${productSlug}/reset-campaign-start`, { headers: internalHeaders });

  await page.goto(versionedFrontendUrl(v6ExperienceVersion, '/?utm_source=local&utm_campaign=v6_local_validation'));
  await expect(
    page.getByRole('heading', { name: 'Você se arruma, mas ainda sente que falta presença?' }),
  ).toBeVisible();
  await expect(page.getByRole('button', { name: 'Ver meu primeiro ajuste MUSA' })).toBeVisible();
  await expect(page.getByRole('region', { name: 'Vídeo curto Método MUSA' })).toBeVisible();
  await expect(page.locator('video.public-hero-video')).toHaveCount(1);
  await expect(page.locator('video.public-hero-video')).toHaveJSProperty('muted', false);
  await expect(page.locator('video.public-hero-video')).toHaveJSProperty('autoplay', false);
  await expect(page.locator('video.public-hero-video')).toHaveJSProperty('controls', true);
  await expect(page.locator('video.public-hero-video')).not.toHaveAttribute('poster');

  await expect.poll(async () => {
    const response = await request.get(
      `${backendBaseUrl}/api/pde/access/analytics/${productSlug}/summary?experienceVersion=${encodeURIComponent(v6ExperienceVersion)}`,
    );
    const summary = await response.json();
    return summary.currentExperienceVersion === v6ExperienceVersion && summary.rawTotalEvents > 0;
  }).toBeTruthy();

  await request.post(`${backendBaseUrl}/api/pde/access/analytics/${productSlug}/reset-campaign-start`, { headers: internalHeaders });

  await page.goto(versionedFrontendUrl(v7ExperienceVersion, '/?utm_source=local&utm_campaign=v7_local_validation'));
  await expect(page.getByText('Se sua imagem falasse antes de você hoje, qual mensagem ela passaria sem intenção?')).toBeVisible();
  await expect(page.getByRole('button', { name: 'Minha imagem está coerente; quero apenas organizar minhas escolhas' })).toBeVisible();
  await expect(page.getByText('Quando você se olha pronta, o que mais te incomoda?')).toHaveCount(0);
  await expect(page.getByRole('region', { name: 'Vídeo curto Método MUSA' })).toHaveCount(0);
  await expect(page.getByRole('region', { name: 'Privacidade e controle dos dados MUSA' })).toBeVisible();
  await expect(page.getByText(/As sete missões não pedem foto nem texto livre e não enviam suas respostas/)).toBeVisible();
  await expect(page.getByText(/suporte, poderá escrever voluntariamente uma mensagem breve/)).toBeVisible();

  await expect.poll(async () => {
    const response = await request.get(
      `${backendBaseUrl}/api/pde/access/analytics/${productSlug}/summary?experienceVersion=${encodeURIComponent(v7ExperienceVersion)}`,
    );
    const summary = await response.json();
    return summary.currentExperienceVersion === v7ExperienceVersion && summary.rawTotalEvents > 0;
  }).toBeTruthy();
});

test('v7 entrega degustação local e acesso único de 90 dias sem fila de IA', async ({ page, request }, testInfo) => {
  const neutralPublicGuidanceResponse = await request.post(`${backendBaseUrl}/api/pde/public/presence-diagnostic`, {
    data: {
      experienceVersion: v7ExperienceVersion,
      answers: {
        mainObstacle: 'Minha imagem está coerente; quero apenas organizar minhas escolhas',
        presenceFocus: 'Rotina comum',
        desiredSignal: 'Elegância discreta',
        startingResource: 'Roupa que já tenho',
      },
    },
  });
  expect(neutralPublicGuidanceResponse.status()).toBe(201);
  const neutralPublicGuidance = await neutralPublicGuidanceResponse.json();
  expect(neutralPublicGuidance.headline).toBe('Sua escolha atual foi preservada');
  expect(neutralPublicGuidance.summary).toContain('não precisa corrigir sua imagem');
  expect(neutralPublicGuidance.inputTokens).toBe(0);
  expect(neutralPublicGuidance.outputTokens).toBe(0);

  const publicGuidanceResponse = await request.post(`${backendBaseUrl}/api/pde/public/presence-diagnostic`, {
    data: {
      experienceVersion: v7ExperienceVersion,
      answers: {
        mainObstacle: 'Falta presença',
        presenceFocus: 'Trabalho ou reunião',
        desiredSignal: 'Elegância discreta',
        startingResource: 'Roupa que já tenho',
      },
    },
  });
  expect(publicGuidanceResponse.status()).toBe(201);
  const publicGuidance = await publicGuidanceResponse.json();
  expect(publicGuidance.status).toBe('COMPLETED');
  expect(publicGuidance.model).toBe('MUSA_LOCAL_RULES_V1');
  expect(publicGuidance.inputTokens).toBe(0);
  expect(publicGuidance.outputTokens).toBe(0);

  const pendingResponse = await request.get(`${backendBaseUrl}/api/internal/pde/ai-guidance/stage-executions/pending`, {
    headers: internalHeaders,
  });
  expect(await pendingResponse.json()).toEqual([]);

  const email = `teste+musa-v7-${testInfo.project.name}-${Date.now()}@sandbox.local`;
  const checkoutResponse = await request.post(`${backendBaseUrl}/api/internal/pde/test-access`, {
    headers: internalHeaders,
    data: { productSlug, email, experienceVersion: v7ExperienceVersion },
  });
  expect(checkoutResponse.status()).toBe(201);
  const access = await checkoutResponse.json();
  for (const legacyPath of ['login', 'register']) {
    const legacyResponse = await request.post(`${backendBaseUrl}/api/pde/access/${legacyPath}`, {
      data: { productSlug, email, experienceVersion: v7ExperienceVersion },
    });
    expect(legacyResponse.status()).toBe(404);
    expect(await legacyResponse.text()).not.toContain(access.token);
  }
  for (const invalidAnswers of [
    { freeText: 'conteúdo arbitrário' },
    { mainObstacle: 'conteúdo arbitrário' },
    { mainObstacle: 'Trabalho ou reunião' },
  ]) {
    const invalidInteraction = await request.post(
      `${backendBaseUrl}/api/pde/access/${access.token}/missions/dia-1-ruido-visual/interactions`,
      { data: { answers: invalidAnswers } },
    );
    expect(invalidInteraction.ok()).toBeFalsy();
  }
  const workspaceResponse = await request.get(`${backendBaseUrl}/api/pde/access/${access.token}/workspace`);
  const workspace = await workspaceResponse.json();
  expect(workspace.subscriptionStatus).toBe('ACTIVE');
  expect(workspace.accessSource).toBe('INTERNAL_QA');
  expect(workspace.experienceVersion).toBe(v7ExperienceVersion);
  const remainingDays = (Date.parse(workspace.accessExpiresAt) - Date.now()) / 86_400_000;
  expect(remainingDays).toBeGreaterThan(89);
  expect(remainingDays).toBeLessThanOrEqual(90);

  await page.goto(versionedFrontendUrl(v7ExperienceVersion, `/access/${access.token}`));
  await expect(page.getByText('Produto ativo')).toBeVisible();
  await expect(page.getByText(/Método MUSA liberado até/)).toBeVisible();
  await expect(page.getByText('Regras locais: suas escolhas não são enviadas para IA ou gerador de vídeo.')).toBeVisible();
  await expect(page.getByRole('region', { name: 'Privacidade e controle dos dados MUSA' })).toBeVisible();
  await page.getByRole('button', { name: 'Manter como está por enquanto' }).click();
  await expect(page.getByRole('heading', { name: 'Sua escolha atual foi preservada' })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Registrar Dia 1 concluído' })).toBeEnabled();
  await page.getByLabel('Como podemos ajudar?').fill('Quero confirmar como retomar minha jornada sem fazer o ajuste de hoje.');
  await page.getByRole('button', { name: 'Pedir suporte' }).click();
  await expect(page.getByText('Você já possui um pedido de suporte aberto.')).toBeVisible();
  await expect(page.getByText(/A equipe responderá pelo e-mail usado na MUSA/)).toBeVisible();
  await page.getByRole('button', { name: 'Registrar Dia 1 concluído' }).click();
  for (let day = 2; day <= 7; day += 1) {
    await page.locator('.mission-tabs button').nth(day - 1).click();
    await page.getByRole('button', { name: 'Manter como está por enquanto' }).click();
    await expect(page.getByRole('heading', { name: 'Sua escolha atual foi preservada' })).toBeVisible();
    await page.getByRole('button', { name: `Registrar Dia ${day} concluído` }).click();
  }
  await expect(page.getByRole('heading', { name: 'Sua jornada MUSA está concluída' })).toBeVisible();

  const materialUrl = workspace.product.supportMaterials[0].url;
  expect(materialUrl).toBe('/materials/musa-v7/mapa-dos-7-sinais.html');
  const unauthenticatedMaterial = await request.get(`${frontendBaseUrl}${materialUrl}`);
  expect(unauthenticatedMaterial.status()).toBe(403);
  const authenticatedMaterial = await request.get(`${frontendBaseUrl}${materialUrl}`, {
    headers: { 'X-PDE-Access-Token': access.token },
  });
  expect(authenticatedMaterial.ok()).toBeTruthy();
  const materialHtml = await authenticatedMaterial.text();
  for (const expectedText of [
    'Mensagem no espelho',
    'Peça-sinal',
    'Estrutura leve',
    'Primeira leitura',
    'Cor com direção',
    'Assinatura pessoal',
    'Fórmula MUSA',
  ]) {
    expect(materialHtml).toContain(expectedText);
  }
  expect(materialHtml).not.toContain('../imagens/');
  const materialPage = await page.context().newPage();
  await materialPage.setContent(materialHtml);
  await expect(materialPage.getByRole('heading', { name: 'Mapa dos 7 Sinais de Presença' })).toBeVisible();
  await expect(materialPage.locator('article')).toHaveCount(7);
  expect(await materialPage.evaluate(() => document.documentElement.scrollWidth <= document.documentElement.clientWidth)).toBeTruthy();
  await materialPage.close();

  const qaSummaryResponse = await request.get(
    `${backendBaseUrl}/api/pde/access/analytics/${productSlug}/summary?includeNonHumanTraffic=true&experienceVersion=${encodeURIComponent(v7ExperienceVersion)}`,
    { headers: internalHeaders },
  );
  expect(qaSummaryResponse.ok()).toBeTruthy();
  const qaSummary = await qaSummaryResponse.json();
  expect(qaSummary.subscriptionApproved).toBe(0);
  expect(qaSummary.accessReleased).toBe(0);

  const correctedEmail = email.replace('@sandbox.local', '+corrigido@sandbox.local');
  const correctionResponsePromise = page.waitForResponse(
    (response) => response.url().includes(`/api/pde/access/${access.token}/privacy-requests`) && response.request().method() === 'POST',
  );
  await page.getByLabel('Corrigir e-mail do acesso').fill(correctedEmail);
  await page.getByRole('button', { name: 'Corrigir meu e-mail' }).click();
  expect((await correctionResponsePromise).ok()).toBeTruthy();
  await expect(page.getByText('E-mail corrigido. Use o novo endereço para retomar sua jornada.')).toBeVisible();
  await expect(
    page.getByLabel('Resumo da Área MUSA').getByText(correctedEmail, { exact: true }),
  ).toBeVisible();

  const downloadPromise = page.waitForEvent('download');
  await page.getByRole('button', { name: 'Baixar meus dados' }).click();
  const download = await downloadPromise;
  expect(download.suggestedFilename()).toContain('meus-dados-musa');

  page.once('dialog', (dialog) => dialog.accept());
  const deletionResponsePromise = page.waitForResponse(
    (response) => response.url().includes(`/api/pde/access/${access.token}/privacy-requests`) && response.request().method() === 'POST',
  );
  await page.getByRole('button', { name: 'Excluir respostas e progresso' }).click();
  expect((await deletionResponsePromise).ok()).toBeTruthy();
  await expect(page.getByText(/Seus dados de uso e progresso foram excluídos/)).toBeVisible();
  expect((await request.get(`${backendBaseUrl}/api/pde/access/${access.token}/workspace`)).status()).toBe(404);
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= document.documentElement.clientWidth)).toBeTruthy();

  const expiredEmail = `teste+musa-v7-expired-${testInfo.project.name}-${Date.now()}@sandbox.local`;
  const expiredAccessResponse = await request.post(`${backendBaseUrl}/api/internal/pde/test-access`, {
    headers: internalHeaders,
    data: { productSlug, email: expiredEmail, experienceVersion: v7ExperienceVersion },
  });
  expect(expiredAccessResponse.status()).toBe(201);
  const expiredAccess = await expiredAccessResponse.json();
  const expirationResponse = await request.post(`${backendBaseUrl}/api/internal/pde/test-access/${expiredAccess.token}/expire`, {
    headers: internalHeaders,
  });
  expect(expirationResponse.ok()).toBeTruthy();
  expect((await expirationResponse.json()).subscriptionStatus).toBe('EXPIRED');

  await page.goto(versionedFrontendUrl(v7ExperienceVersion, `/access/${expiredAccess.token}`));
  await expect(page.getByText('Expirado', { exact: true })).toBeVisible();
  await expect(page.getByRole('region', { name: 'Acesso MUSA expirado' })).toBeVisible();
  await expect(page.getByText(/Sua compra anterior permanece reconhecida/).first()).toBeVisible();
  await expect(page.getByRole('region', { name: 'Oferta de acesso completo MUSA' })).toHaveCount(0);
  await expect(page.getByRole('button', { name: /Liberar por/ })).toHaveCount(0);
  await expect(page.getByText(/Não inclua saúde, intimidade, documentos/)).toBeVisible();
  await expect(page.getByRole('region', { name: 'Privacidade e controle dos dados MUSA' })).toBeVisible();
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= document.documentElement.clientWidth)).toBeTruthy();

  const expiredDeletionResponse = await request.post(
    `${backendBaseUrl}/api/pde/access/${expiredAccess.token}/privacy-requests`,
    { data: { action: 'DELETION' } },
  );
  expect(expiredDeletionResponse.ok()).toBeTruthy();
});
