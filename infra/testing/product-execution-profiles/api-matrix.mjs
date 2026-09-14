import assert from "node:assert/strict";
import { writeFile, mkdir } from "node:fs/promises";
export const backend = "http://127.0.0.1:18094";
export async function request(path, body, status = 200) {
  const result = await fetch(backend + path, {
    method: body === undefined ? "GET" : "POST",
    headers: { "content-type": "application/json" },
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  const text = await result.text();
  assert.equal(result.status, status, `${path}: ${text}`);
  return text ? JSON.parse(text) : null;
}
export const base = (id) => `/api/products/${id}/execution-profiles/v1`;
export function payload(id, capability = "PERSONALIZED_IMAGES") {
  return {
    chainId: 94014,
    commercialPlanId: id,
    createdBy: "Homologação local",
    contract: {
      productVersion: "fixture-v1",
      capability,
      purchasedOutcome: "Duas imagens personalizadas utilizáveis",
      inputs: ["Referências consentidas"],
      deliverables: ["Duas imagens"],
      qualityCriteria: ["Fidelidade, legibilidade e acesso"],
      deliveryMode: "Pacote com acesso autenticado",
      revenueModel: "Compra única",
      audiovisualRequired: false,
      includedUnits: 2,
      maximumAttempts: 4,
      costModel: "gpt-image-2.5-sunburst",
      pricingRevision: "fixture-rate-v1",
      usdBrl: 5,
      maximumAttemptCostBrl: 2,
      maximumDeliveryCostBrl: 8,
      minimumContributionBrl: 20,
      productionBudget: {
        costModel: "gpt-image-2.5-sunburst",
        maximumAttempts: 4,
        maximumAttemptCostBrl: 2,
        maximumTotalCostBrl: 8,
      },
      scenarios: ["FAVORABLE", "BASE", "CONSERVATIVE"].map((code) => ({
        code,
        priceBrl: 100,
        acquisitionBrl: 10,
        feesBrl: 5,
        supportBrl: 2,
        storageDeliveryBrl: 1,
        otherCostsBrl: 4,
      })),
    },
  };
}
export async function approve(id, profile) {
  const analysis = await request(
    `${base(id)}/${profile.id}/financial-analysis`,
    {},
  );
  const financialExecutionId = analysis.reviews.find(
    (r) => r.checkpoint === "ANALYSIS",
  ).financialExecutionId;
  await request(`/fixture/finance/${financialExecutionId}/complete`, {});
  for (const checkpoint of [
    "OFFER",
    "DELIVERY_DESIGN",
    "HOMOLOGATION",
    "OPERATION",
  ])
    await request(`${base(id)}/${profile.id}/financial-reviews`, {
      checkpoint,
      financialExecutionId,
      approved: true,
      reviewedBy: "Pessoa de teste",
      rationale:
        "Parecer sintético lido; custos e margem conferidos na mesma revisão.",
    });
  return financialExecutionId;
}

// A importação oferece helpers ao navegador sem executar ou duplicar a matriz.
if (process.argv[1] === new URL(import.meta.url).pathname) {
  const output =
    process.env.PROFILE_TEST_ARTIFACTS ||
    "artifacts/product-execution-profiles/api";
  await mkdir(output, { recursive: true });
  const catalog = await request(`${base(94001)}/catalog`);
  assert.equal(catalog.capabilities.length, 4);
  assert.equal(catalog.chains.length, 1);
  const bad = payload(94001);
  bad.contract.scenarios[0].supportBrl = null;
  await request(base(94001), bad, 400);
  const missingScenario = payload(94001);
  missingScenario.contract.scenarios[0] = null;
  await request(base(94001), missingScenario, 400);
  const missingProduction = payload(94001);
  delete missingProduction.contract.productionBudget;
  await request(base(94001), missingProduction, 400);
  const missingPrivateAttempts = payload(94001);
  delete missingPrivateAttempts.contract.productionBudget.maximumAttempts;
  missingPrivateAttempts.contract.productionBudget.maximumAttemptCostBrl = 0;
  missingPrivateAttempts.contract.productionBudget.maximumTotalCostBrl = 0;
  await request(base(94001), missingPrivateAttempts, 400);
  const created = [];
  for (const [offset, capability] of [
    "PERSONALIZED_IMAGES",
    "PERSONALIZED_IMAGES",
    "GUIDED_EXPERIENCE",
    "AI_TOOL",
    "DIGITAL_PACKAGE",
  ].entries()) {
    const product = 94001 + offset;
    const profile = await request(base(product), payload(product, capability));
    created.push(profile);
    assert.equal(profile.work.filter((w) => !w.applicable).length, 1);
    assert.equal(
      profile.work.find((w) => !w.applicable).activityId,
      "audiovisual",
    );
    assert.equal(new Set(profile.work.map((w) => w.processCode)).size, 6);
    assert.equal(profile.economics[0].fullCostBrl, 30);
    await request(`${base(product)}/${profile.id}/bindings`, {
      sourceReference: `experiment:${product}`,
      learningCycleId: product,
      actor: "Pessoa de teste",
    });
    const before = await request(`/fixture/activities/${product}`);
    assert.equal(before[0].operationalState, "BLOCKED");
    const finance = await approve(product, profile);
    const after = await request(`/fixture/activities/${product}`);
    assert.equal(after[0].executionRequestAvailable, true);
    const again = await request(
      `${base(product)}/${profile.id}/financial-analysis`,
      {},
    );
    assert.equal(
      again.reviews.filter((r) => r.checkpoint === "ANALYSIS").length,
      1,
    );
    assert.equal(
      again.reviews.find((r) => r.checkpoint === "ANALYSIS")
        .financialExecutionId,
      finance,
    );
    const context = await request(`/fixture/context/${product}`);
    assert.equal(context.profileId, profile.id);
    assert.equal(context.productId, product);
    assert.equal(context.sourceReference, `experiment:${product}`);
    assert.deepEqual(context.financialCheckpoints, {
      OFFER: true,
      DELIVERY_DESIGN: true,
      HOMOLOGATION: true,
      OPERATION: true,
    });
    assert.deepEqual(await request(`/fixture/omission/${product}`), {
      omitted: true,
      objectiveAchieved: false,
    });
  }
  assert.notEqual(created[0].productType, created[1].productType);
  assert.equal(created[0].routeName, created[1].routeName);
  await request(`${base(94002)}/${created[0].id}`, undefined, 404);
  const newVersion = await request(base(94001), payload(94001, "AI_TOOL"));
  assert.equal(newVersion.revision, 2);
  await request(
    `${base(94001)}/${newVersion.id}/bindings`,
    {
      sourceReference: "experiment:94001",
      learningCycleId: 94001,
      actor: "Teste",
    },
    409,
  );
  assert.equal(
    (await request("/fixture/context/94001")).profileId,
    created[0].id,
  );
  await request(
    `${base(94001)}/${newVersion.id}/bindings`,
    { sourceReference: "experiment:94002", actor: "Teste" },
    409,
  );
  await request(
    `${base(94001)}/${newVersion.id}/bindings`,
    {
      sourceReference: "experiment:94001",
      learningCycleId: 94002,
      actor: "Teste",
    },
    409,
  );
  await request(
    `${base(94001)}/${newVersion.id}/financial-reviews`,
    {
      checkpoint: "OFFER",
      financialExecutionId: 1,
      approved: true,
      reviewedBy: "Teste",
      rationale: "Parecer de outra revisão",
    },
    409,
  );
  const first = await request("/fixture/reserve/94001", {
    operationKey: "one",
    units: 2,
  });
  await request(
    "/fixture/reserve/94001",
    { operationKey: "one", units: 2 },
    409,
  );
  await request(`/fixture/settle/94001/${first.reservationId}`, {
    actualBrl: null,
    failed: true,
  });
  await request(
    "/fixture/reserve/94001",
    { operationKey: "after-unknown", units: 1 },
    409,
  );
  await request(`/fixture/settle/94001/${first.reservationId}`, {
    actualBrl: 3,
    failed: true,
  });
  await request("/fixture/reserve/94001", { operationKey: "second", units: 2 });
  await request(
    "/fixture/reserve/94001",
    { operationKey: "above", units: 1 },
    409,
  );
  await request(
    "/fixture/reserve/94001",
    {
      operationKey: "different-private-package",
      usageKey: "another-package",
      units: 1,
    },
    409,
  );
  const concurrent = await Promise.all(
    Array.from({ length: 8 }, (_, n) =>
      fetch(backend + "/fixture/reserve/94002", {
        method: "POST",
        headers: { "content-type": "application/json" },
        body: JSON.stringify({ operationKey: `concurrent-${n}`, units: 1 }),
      }),
    ),
  );
  assert.equal(
    concurrent.filter((r) => r.status === 200).length,
    4,
    "A concorrência deve respeitar o lock e o teto de quatro tentativas",
  );
  assert.equal(concurrent.filter((r) => r.status === 409).length, 4);
  await request("/fixture/reserve/94002", {
    operationKey: "segregated-operation",
    units: 1,
    testData: false,
  });
  const third = await request("/fixture/reserve/94003", {
    operationKey: "overrun",
    units: 1,
  });
  await request(`/fixture/settle/94003/${third.reservationId}`, {
    actualBrl: 10,
  });
  await request(
    "/fixture/reserve/94003",
    { operationKey: "after-overrun", units: 1 },
    409,
  );
  await request(
    `/fixture/settle/94004/${third.reservationId}`,
    { actualBrl: 10 },
    409,
  );
  await request("/fixture/plan-version/2", {});
  await request(
    "/fixture/reserve/94004",
    { operationKey: "changed-plan", units: 1 },
    409,
  );
  await request("/fixture/plan-version/1", {});
  for (const [product, capability] of [
    [94008, "DIGITAL_PACKAGE"],
    [94009, "GUIDED_EXPERIENCE"],
    [94010, "AI_TOOL"],
  ]) {
    const data = payload(product, capability);
    if (capability === "AI_TOOL") {
      data.contract.costModel = "fixture-text-ai-model";
    } else {
      data.contract.costModel = "NO_VARIABLE_AI_COST";
      data.contract.maximumAttemptCostBrl = 0;
      data.contract.maximumDeliveryCostBrl = 0;
    }
    const profile = await request(base(product), data);
    assert.equal(
      profile.economics[0].fullCostBrl,
      capability === "AI_TOOL" ? 30 : 22,
    );
    await request(`${base(product)}/${profile.id}/bindings`, {
      sourceReference: `experiment:${product}`,
      learningCycleId: product,
      actor: "Teste de orçamentos separados",
    });
    await approve(product, profile);
    const privateReservation = await request(`/fixture/reserve/${product}`, {
      operationKey: "private-image",
      units: 2,
    });
    assert.ok(privateReservation.reservationId);
    await request(
      `/fixture/reserve/${product}`,
      { operationKey: "wrong-delivery-model", units: 1, testData: false },
      409,
    );
    await request(
      `/fixture/reserve/${product}`,
      {
        operationKey: "delivery-model",
        units: 1,
        testData: false,
        model: data.contract.costModel,
      },
      capability === "AI_TOOL" ? 200 : 409,
    );
  }
  const noProduction = payload(94011);
  Object.assign(noProduction.contract.productionBudget, {
    maximumAttempts: 0,
    maximumAttemptCostBrl: 0,
    maximumTotalCostBrl: 0,
  });
  const privateDisabled = await request(base(94011), noProduction);
  await request(`${base(94011)}/${privateDisabled.id}/bindings`, {
    sourceReference: "experiment:94011",
    learningCycleId: 94011,
    actor: "Teste sem orçamento privado",
  });
  await approve(94011, privateDisabled);
  await request(
    "/fixture/reserve/94011",
    { operationKey: "no-private-budget", units: 1 },
    409,
  );
  const implicitZero = payload(94012);
  implicitZero.contract.maximumAttemptCostBrl = 0;
  implicitZero.contract.maximumDeliveryCostBrl = 0;
  const blockedZero = await request(base(94012), implicitZero);
  assert.ok(blockedZero.blockers.some((b) => b.includes("custos positivos")));
  const incompleteProduction = payload(94013);
  incompleteProduction.contract.productionBudget.maximumTotalCostBrl = 1;
  const blockedProduction = await request(base(94013), incompleteProduction);
  assert.ok(
    blockedProduction.blockers.some((b) => b.includes("orçamento privado")),
  );
  for (const product of [94006, 94007]) {
    const contract = payload(product);
    contract.contract.maximumAttempts = 8;
    contract.contract.maximumDeliveryCostBrl = 16;
    const profile = await request(base(product), contract);
    await request(`${base(product)}/${profile.id}/bindings`, {
      sourceReference: `experiment:${product}`,
      learningCycleId: product,
      actor: "Teste de pacote",
    });
    await approve(product, profile);
    await request(`/fixture/package/${product}/start`, {});
    await request(`/fixture/package/${product}/complete`, { outputs: 1 }, 409);
    await request(`/fixture/package/${product}/complete`, { outputs: 2 });
    let view = await request(`${base(product)}/${profile.id}`);
    assert.equal(view.consumption[0].units, product === 94007 ? 6 : 2);
    assert.equal(view.consumption[0].status, "COST_PENDING");
    assert.equal(view.consumption[0].testData, false);
    await request(`/fixture/package/${product}/start`, {}, 409);
    await request(
      `${base(product)}/${profile.id}/consumption-reconciliations`,
      {
        consumptionId: view.consumption[0].id,
        actualBrl: 3,
        providerReceipt: "estimativa",
        reviewedBy: "Financeiro sintético",
        rationale: "Valor projetado sem comprovante",
        failed: false,
      },
      409,
    );
    view = await request(
      `${base(product)}/${profile.id}/consumption-reconciliations`,
      {
        consumptionId: view.consumption[0].id,
        actualBrl: 3,
        providerReceipt: "internal://fixture/receipt",
        reviewedBy: "Financeiro sintético",
        rationale: "Comprovante sintético conferido",
        failed: false,
      },
    );
    assert.equal(view.consumption[0].status, "SETTLED");
    assert.match(
      view.consumption[0].evidence,
      /HUMAN_RECONCILED_PROVIDER_RECEIPT_V1/,
    );
  }
  const result = {
    status: "PASS",
    created: created.map((p) => ({
      id: p.id,
      productId: p.productId,
      revision: p.revision,
    })),
    concurrency: { accepted: 4, blocked: 4 },
    scope: "SYNTHETIC_ONLY",
    paidCalls: 0,
  };
  await writeFile(
    `${output}/api-results.json`,
    JSON.stringify(result, null, 2),
  );
  console.log(JSON.stringify(result));
}
