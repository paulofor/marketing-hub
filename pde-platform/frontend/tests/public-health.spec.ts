import { expect, test, type APIRequestContext } from '@playwright/test';

type PublicHealthContract = {
  slug?: string;
  healthPath?: string;
  requiredTexts?: string[];
  forbiddenTexts?: string[];
};

type SlotDiagnostics = {
  status?: string;
  slot?: string;
  publicUrl?: string;
  experienceVersion?: string;
  image?: string;
  imageTag?: string;
  commitSha?: string;
};

const defaultContract: Required<PublicHealthContract> = {
  slug: 'metodo-musa-7-dias',
  healthPath: '/',
  requiredTexts: [
    'Descubra o detalhe que está tirando elegância do seu look, sem comprar roupa nova.',
    'Estrada MUSA',
    'Ver meu ajuste MUSA',
  ],
  forbiddenTexts: [
    'Application error',
    'Cannot find module',
    'Unexpected token',
    'Failed to fetch dynamically imported module',
  ],
};

function parseList(value: string | undefined) {
  return value
    ? value
        .split('|')
        .map((item) => item.trim())
        .filter(Boolean)
    : [];
}

async function loadContract(request: APIRequestContext) {
  const response = await request.get('/pde-health-contract.json');
  const fileContract = response.ok() ? ((await response.json()) as PublicHealthContract) : {};
  const envRequiredTexts = parseList(process.env.PDE_PUBLIC_HEALTH_REQUIRED_TEXTS);
  const envForbiddenTexts = parseList(process.env.PDE_PUBLIC_HEALTH_FORBIDDEN_TEXTS);

  return {
    slug: process.env.PDE_PUBLIC_HEALTH_PRODUCT_SLUG || fileContract.slug || defaultContract.slug,
    healthPath: process.env.PDE_PUBLIC_HEALTH_PATH || fileContract.healthPath || defaultContract.healthPath,
    requiredTexts: envRequiredTexts.length > 0 ? envRequiredTexts : fileContract.requiredTexts || defaultContract.requiredTexts,
    forbiddenTexts: envForbiddenTexts.length > 0 ? envForbiddenTexts : fileContract.forbiddenTexts || defaultContract.forbiddenTexts,
  };
}

test('health publico renderiza app, javascript e texto comercial', async ({ page, request }) => {
  const pageErrors: string[] = [];
  const contract = await loadContract(request);

  page.on('pageerror', (error) => {
    pageErrors.push(error.message);
  });

  const staticHealth = await request.get('/healthz');
  expect(staticHealth.ok()).toBeTruthy();
  expect(await staticHealth.text()).toContain('"status":"UP"');

  const slotDiagnostics = await request.get('/slot-diagnostics.json');
  expect(slotDiagnostics.ok()).toBeTruthy();
  const diagnostics = (await slotDiagnostics.json()) as SlotDiagnostics;
  expect(diagnostics.status).toBe('UP');
  expect(diagnostics.experienceVersion).toBeTruthy();
  expect(diagnostics.slot).toBeTruthy();
  expect(diagnostics.image).toBeTruthy();
  expect(diagnostics.commitSha).toBeTruthy();

  const response = await page.goto(contract.healthPath, { waitUntil: 'networkidle' });
  expect(response?.ok()).toBeTruthy();

  await expect(page.locator('#root').locator(':scope > *').first()).toBeVisible();
  for (const text of contract.requiredTexts) {
    await expect(page.locator('body'), `Texto obrigatorio ausente no PDE ${contract.slug}: ${text}`).toContainText(text);
  }
  for (const text of contract.forbiddenTexts) {
    await expect(page.locator('body'), `Texto de erro apareceu no PDE ${contract.slug}: ${text}`).not.toContainText(text);
  }
  expect(await page.locator('script[type="module"][src]').count()).toBeGreaterThan(0);

  expect(pageErrors, `Erros de execucao no health publico: ${pageErrors.join(' | ')}`).toEqual([]);
});
