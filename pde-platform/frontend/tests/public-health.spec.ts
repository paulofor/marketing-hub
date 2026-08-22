import { expect, test, type APIRequestContext } from '@playwright/test';

type PublicHealthContract = {
  slug?: string;
  healthPath?: string;
  requiredTexts?: string[];
  forbiddenTexts?: string[];
};

type VersionDiagnostics = {
  status?: string;
  version?: string;
  legacySlot?: string;
  publicUrl?: string;
  experienceVersion?: string;
  image?: string;
  imageVersionId?: string;
  imageTag?: string;
  commitSha?: string;
  knownPointedDomains?: { host?: string; observedAddress?: string; role?: string; experienceVersion?: string }[];
};

type PublicProductContract = {
  publicFirstFold?: {
    headline?: string;
    videoCtaLabel?: string;
  };
};

const defaultContract: Required<PublicHealthContract> = {
  slug: 'metodo-musa-7-dias',
  healthPath: '/',
  requiredTexts: [
    'Seu primeiro ajuste MUSA',
  ],
  forbiddenTexts: [
    'Application error',
    'Cannot find module',
    'Unexpected token',
    'Failed to fetch dynamically imported module',
    'Domínios conhecidos apontados',
    'Slots versionados do Clube MUSA',
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

async function loadPublishedFirstFoldTexts(request: APIRequestContext, slug: string, diagnostics: VersionDiagnostics) {
  const searchParams = new URLSearchParams();
  if (diagnostics.version || diagnostics.legacySlot) {
    searchParams.set('slotCode', diagnostics.version || diagnostics.legacySlot || '');
  }
  if (diagnostics.experienceVersion) {
    searchParams.set('experienceVersion', diagnostics.experienceVersion);
  }
  const query = searchParams.toString();
  const response = await request.get(`/api/pde/products/${slug}${query ? `?${query}` : ''}`);
  if (!response.ok()) {
    return [];
  }

  const product = (await response.json()) as PublicProductContract;
  return [product.publicFirstFold?.headline, product.publicFirstFold?.videoCtaLabel]
    .map((item) => item?.trim())
    .filter((item): item is string => Boolean(item));
}

function removeMutableFallbackTexts(requiredTexts: string[], publishedFirstFoldTexts: string[]) {
  if (publishedFirstFoldTexts.length === 0) {
    return requiredTexts;
  }

  const mutableFallbackTexts = new Set([
    'Você se arruma, mas ainda sente que falta presença?',
    'Ver meu primeiro ajuste MUSA',
  ]);
  return requiredTexts.filter((text) => !mutableFallbackTexts.has(text));
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

  const versionDiagnostics = await request.get('/version-diagnostics.json');
  expect(versionDiagnostics.ok()).toBeTruthy();
  const diagnostics = (await versionDiagnostics.json()) as VersionDiagnostics;
  expect(diagnostics.status).toBe('UP');
  expect(diagnostics.experienceVersion).toBeTruthy();
  expect(diagnostics.version).toBeTruthy();
  expect(diagnostics.image).toBeTruthy();
  expect(diagnostics.imageVersionId).toBeTruthy();
  expect(diagnostics.commitSha).toBeTruthy();
  if (contract.slug === 'metodo-musa-7-dias') {
    expect(diagnostics.knownPointedDomains?.map((domain) => domain.host)).toEqual(
      expect.arrayContaining(['v5.clubemusa.com.br', 'v6.clubemusa.com.br', 'v7.clubemusa.com.br']),
    );
  }
  const publishedFirstFoldTexts = await loadPublishedFirstFoldTexts(request, contract.slug, diagnostics);
  const staticRequiredTexts = removeMutableFallbackTexts(contract.requiredTexts, publishedFirstFoldTexts);

  const response = await page.goto(contract.healthPath, { waitUntil: 'networkidle' });
  expect(response?.ok()).toBeTruthy();

  await expect(page.locator('#root').locator(':scope > *').first()).toBeVisible();
  for (const text of [...publishedFirstFoldTexts, ...staticRequiredTexts]) {
    await expect(page.locator('body'), `Texto obrigatorio ausente no PDE ${contract.slug}: ${text}`).toContainText(text);
  }
  const publicBodyText = await page.locator('body').innerText();
  for (const text of contract.forbiddenTexts) {
    expect(publicBodyText, `Texto operacional apareceu no PDE publico ${contract.slug}: ${text}`).not.toContain(text);
  }
  expect(await page.locator('script[type="module"][src]').count()).toBeGreaterThan(0);

  expect(pageErrors, `Erros de execucao no health publico: ${pageErrors.join(' | ')}`).toEqual([]);
});
