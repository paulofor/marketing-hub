import { expect, test, type APIRequestContext } from "@playwright/test";

type PublicHealthContract = {
  slug?: string;
  healthPath?: string;
  commercialOfferPath?: string;
  integrationContractPath?: string;
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
  knownPointedDomains?: {
    host?: string;
    observedAddress?: string;
    role?: string;
    experienceVersion?: string;
  }[];
};

type PublicProductContract = {
  publicFirstFold?: {
    headline?: string;
    supportingText?: string;
    videoCtaLabel?: string;
  };
};

type PublicCommercialOffer = {
  primaryCta?: string;
  priceBrl?: number;
  checkoutUrl?: string;
};

type PublicJourneyIntegration = {
  productSlug?: string;
  experienceVersion?: string;
  contractVersion?: string;
  eventsPath?: string;
  loginPath?: string;
  requiredEventTypes?: string[];
  correlationKeys?: string[];
  sourceOfTruth?: string;
  testTrafficPolicy?: string;
};

const defaultContract: Required<PublicHealthContract> = {
  slug: "metodo-musa-7-dias",
  healthPath: "/",
  commercialOfferPath: "",
  integrationContractPath: "",
  requiredTexts: ["Seu primeiro ajuste MUSA"],
  forbiddenTexts: [
    "Application error",
    "Cannot find module",
    "Unexpected token",
    "Failed to fetch dynamically imported module",
    "Domínios conhecidos apontados",
    "Slots versionados do Clube MUSA",
  ],
};

function parseList(value: string | undefined) {
  return value
    ? value
        .split("|")
        .map((item) => item.trim())
        .filter(Boolean)
    : [];
}

async function loadContract(request: APIRequestContext) {
  const response = await request.get("/pde-health-contract.json");
  const fileContract = response.ok()
    ? ((await response.json()) as PublicHealthContract)
    : {};
  const envRequiredTexts = parseList(
    process.env.PDE_PUBLIC_HEALTH_REQUIRED_TEXTS,
  );
  const envForbiddenTexts = parseList(
    process.env.PDE_PUBLIC_HEALTH_FORBIDDEN_TEXTS,
  );

  return {
    slug:
      process.env.PDE_PUBLIC_HEALTH_PRODUCT_SLUG ||
      fileContract.slug ||
      defaultContract.slug,
    healthPath:
      process.env.PDE_PUBLIC_HEALTH_PATH ||
      fileContract.healthPath ||
      defaultContract.healthPath,
    commercialOfferPath:
      fileContract.commercialOfferPath || defaultContract.commercialOfferPath,
    integrationContractPath:
      fileContract.integrationContractPath ||
      defaultContract.integrationContractPath,
    requiredTexts:
      envRequiredTexts.length > 0
        ? envRequiredTexts
        : fileContract.requiredTexts || defaultContract.requiredTexts,
    forbiddenTexts:
      envForbiddenTexts.length > 0
        ? envForbiddenTexts
        : fileContract.forbiddenTexts || defaultContract.forbiddenTexts,
  };
}

async function loadJourneyIntegration(
  request: APIRequestContext,
  integrationContractPath: string,
) {
  if (!integrationContractPath) {
    return null;
  }
  const response = await request.get(integrationContractPath);
  expect(
    response.ok(),
    `Contrato de integracao indisponivel: ${integrationContractPath}`,
  ).toBeTruthy();
  return (await response.json()) as PublicJourneyIntegration;
}

async function loadCommercialOffer(
  request: APIRequestContext,
  commercialOfferPath: string,
) {
  if (!commercialOfferPath) {
    return null;
  }

  const response = await request.get(commercialOfferPath);
  expect(
    response.ok(),
    `Oferta comercial publica indisponivel: ${commercialOfferPath}`,
  ).toBeTruthy();
  const offer = (await response.json()) as PublicCommercialOffer;
  expect(
    offer.primaryCta?.trim(),
    "Oferta comercial sem CTA primario",
  ).toBeTruthy();
  expect(offer.priceBrl, "Oferta comercial sem preco positivo").toBeGreaterThan(
    0,
  );
  expect(
    offer.checkoutUrl?.trim(),
    "Oferta comercial sem checkout",
  ).toBeTruthy();
  return offer;
}

function formatBrl(value: number) {
  return new Intl.NumberFormat("pt-BR", {
    style: "currency",
    currency: "BRL",
    minimumFractionDigits: 0,
    maximumFractionDigits: 2,
  }).format(value);
}

async function loadPublishedFirstFoldTexts(
  request: APIRequestContext,
  slug: string,
  diagnostics: VersionDiagnostics,
) {
  const searchParams = new URLSearchParams();
  if (diagnostics.version || diagnostics.legacySlot) {
    searchParams.set(
      "slotCode",
      diagnostics.version || diagnostics.legacySlot || "",
    );
  }
  if (diagnostics.experienceVersion) {
    searchParams.set("experienceVersion", diagnostics.experienceVersion);
  }
  const query = searchParams.toString();
  const response = await request.get(
    `/api/pde/products/${slug}${query ? `?${query}` : ""}`,
  );
  if (!response.ok()) {
    return [];
  }

  const product = (await response.json()) as PublicProductContract;
  return [
    product.publicFirstFold?.headline,
    product.publicFirstFold?.supportingText,
  ]
    .map((item) => item?.trim())
    .filter((item): item is string => Boolean(item));
}

function removeMutableFallbackTexts(
  requiredTexts: string[],
  publishedFirstFoldTexts: string[],
) {
  if (publishedFirstFoldTexts.length === 0) {
    return requiredTexts;
  }

  const mutableFallbackTexts = new Set([
    "Você se arruma, mas ainda sente que falta presença?",
    "Ver meu primeiro ajuste MUSA",
  ]);
  return requiredTexts.filter((text) => !mutableFallbackTexts.has(text));
}

function safeSmokeHealthPath(healthPath: string) {
  const url = new URL(healthPath, "http://pde-smoke.local");
  url.searchParams.set("mh_preview", "qa");
  url.searchParams.set("pde_analytics", "off");
  return `${url.pathname}${url.search}${url.hash}`;
}

test("health publico renderiza app, javascript e texto comercial", async ({
  page,
  request,
}) => {
  const pageErrors: string[] = [];
  const analyticsRequests: string[] = [];
  const contract = await loadContract(request);

  page.on("pageerror", (error) => {
    pageErrors.push(error.message);
  });
  page.on("request", (browserRequest) => {
    if (
      browserRequest.method() === "POST" &&
      browserRequest.url().includes("/api/pde/access/events")
    ) {
      analyticsRequests.push(browserRequest.url());
    }
  });

  const staticHealth = await request.get("/healthz");
  expect(staticHealth.ok()).toBeTruthy();
  expect(await staticHealth.text()).toContain('"status":"UP"');

  const versionDiagnostics = await request.get("/version-diagnostics.json");
  expect(versionDiagnostics.ok()).toBeTruthy();
  const diagnostics = (await versionDiagnostics.json()) as VersionDiagnostics;
  expect(diagnostics.status).toBe("UP");
  expect(diagnostics.experienceVersion).toBeTruthy();
  expect(diagnostics.version).toBeTruthy();
  expect(diagnostics.image).toBeTruthy();
  expect(diagnostics.imageVersionId).toBeTruthy();
  expect(diagnostics.commitSha).toBeTruthy();
  if (contract.slug === "metodo-musa-7-dias") {
    expect(
      diagnostics.knownPointedDomains?.map((domain) => domain.role),
    ).not.toContain("active");
    expect(
      diagnostics.knownPointedDomains?.map((domain) => domain.host),
    ).toEqual(
      expect.arrayContaining([
        "v5.clubemusa.com.br",
        "v6.clubemusa.com.br",
        "v7.clubemusa.com.br",
      ]),
    );
  }
  const publishedFirstFoldTexts = await loadPublishedFirstFoldTexts(
    request,
    contract.slug,
    diagnostics,
  );
  const commercialOffer = await loadCommercialOffer(
    request,
    contract.commercialOfferPath,
  );
  const journeyIntegration = await loadJourneyIntegration(
    request,
    contract.integrationContractPath,
  );
  if (journeyIntegration) {
    expect(journeyIntegration.productSlug).toBe(contract.slug);
    expect(journeyIntegration.experienceVersion).toBe(
      diagnostics.experienceVersion,
    );
    expect(journeyIntegration.contractVersion).toBe(
      "PDE_COMMERCIAL_JOURNEY_EVENTS_V1",
    );
    expect(journeyIntegration.eventsPath).toBe("/api/pde/access/events");
    expect(journeyIntegration.loginPath).toBe("/api/pde/access/login-link");
    expect(journeyIntegration.requiredEventTypes).toEqual(
      expect.arrayContaining([
        "PAGE_VIEW",
        "CHECKOUT_STARTED",
        "PURCHASE_COMPLETED",
        "ACCESS_RELEASED",
        "FIRST_USE",
      ]),
    );
    expect(journeyIntegration.correlationKeys).toEqual(
      expect.arrayContaining([
        "eventId",
        "productSlug",
        "experienceVersion",
        "sessionId",
        "visitorId",
        "accessToken",
      ]),
    );
    expect(journeyIntegration.sourceOfTruth).toBe("pde_funnel_event");
    expect(journeyIntegration.testTrafficPolicy).toContain("INTERNAL_QA");
  }
  const staticRequiredTexts = removeMutableFallbackTexts(
    contract.requiredTexts,
    publishedFirstFoldTexts,
  );

  const response = await page.goto(safeSmokeHealthPath(contract.healthPath), {
    waitUntil: "networkidle",
  });
  expect(response?.ok()).toBeTruthy();

  await expect(
    page.locator("#root").locator(":scope > *").first(),
  ).toBeVisible();
  for (const text of [...publishedFirstFoldTexts, ...staticRequiredTexts]) {
    await expect(
      page.locator("body"),
      `Texto obrigatorio ausente no PDE ${contract.slug}: ${text}`,
    ).toContainText(text);
  }
  if (commercialOffer) {
    const offerCard = page.getByTestId("commercial-offer");
    await expect(offerCard).toContainText(commercialOffer.primaryCta!);
    await expect(offerCard).toContainText(formatBrl(commercialOffer.priceBrl!));
    await expect(
      offerCard.locator(".assisted-pde-checkout-cta"),
    ).toHaveAttribute("href", commercialOffer.checkoutUrl!);
    if (contract.slug === "kit-whatsapp-pronto") {
      const tasting = page.getByTestId("assisted-tasting");
      await expect(tasting).toContainText(
        "Experimente uma sequência antes de comprar",
      );
      await expect(
        tasting.getByRole("button", { name: "Gerar minha amostra" }),
      ).toBeVisible();
    }
  }
  const publicBodyText = await page.locator("body").innerText();
  for (const text of contract.forbiddenTexts) {
    expect(
      publicBodyText,
      `Texto operacional apareceu no PDE publico ${contract.slug}: ${text}`,
    ).not.toContain(text);
  }
  expect(
    await page.locator('script[type="module"][src]').count(),
  ).toBeGreaterThan(0);

  expect(
    pageErrors,
    `Erros de execucao no health publico: ${pageErrors.join(" | ")}`,
  ).toEqual([]);
  expect(
    analyticsRequests,
    "O smoke publico nao pode alterar metricas humanas ou de QA.",
  ).toEqual([]);
});
