import { expect, test } from "@playwright/test";
import { resolveMusaExperienceContract } from "../src/musaExperiences";

const expectedProductSlug = "metodo-musa-7-dias";
const expectedMissionId = "diagnostico-presenca-publico";
const expectedGuidanceType = "MUSA_PUBLIC_PRESENCE_DIAGNOSTIC";
const musaV7ExperienceVersion = "musa-pde-entry-v7-espelho-antes-de-sair";
const configuredExpectedExperienceVersion =
  process.env.PDE_EXPECTED_EXPERIENCE_VERSION?.trim();
const musaV7QuestionKeys = [
  "desiredSignal",
  "mainObstacle",
  "presenceFocus",
  "startingResource",
];

test("POST publico do diagnostico MUSA cria solicitacao valida", async ({
  request,
}) => {
  const versionQuery = configuredExpectedExperienceVersion
    ? `?experienceVersion=${encodeURIComponent(configuredExpectedExperienceVersion)}`
    : "";
  const contractResponse = await request.get(
    `/api/pde/products/${expectedProductSlug}${versionQuery}`,
  );
  const contractText = await contractResponse.text();
  expect(
    contractResponse.ok(),
    `Contrato publico do MUSA falhou com HTTP ${contractResponse.status()}: ${contractText}`,
  ).toBeTruthy();

  const contract = JSON.parse(contractText);
  expect(contract.slug).toBe(expectedProductSlug);
  expect(contract.experienceVersion).toEqual(expect.any(String));
  expect(contract.experienceVersion.length).toBeGreaterThan(0);
  if (configuredExpectedExperienceVersion) {
    expect(contract.experienceVersion).toBe(
      configuredExpectedExperienceVersion,
    );
  }
  const publishedQuestions = contract.publicDiagnosticQuestions as Array<{
    key: string;
    options: string[];
  }> | null;
  if (contract.experienceVersion === musaV7ExperienceVersion) {
    expect(publishedQuestions).toEqual(expect.any(Array));
    expect(publishedQuestions?.length).toBeGreaterThan(0);
  }
  const questions = (
    publishedQuestions?.length
      ? publishedQuestions
      : resolveMusaExperienceContract(
          contract.experienceVersion,
          contract.layoutKey,
        ).publicDiagnosticQuestions
  ) as Array<{
    key: string;
    options: string[];
  }>;
  expect(questions.length).toBeGreaterThan(0);
  expect(new Set(questions.map(({ key }) => key)).size).toBe(questions.length);
  questions.forEach(({ key, options }) => {
    expect(key).toEqual(expect.any(String));
    expect(options).toEqual(expect.arrayContaining([expect.any(String)]));
  });
  if (contract.experienceVersion === musaV7ExperienceVersion) {
    expect(questions.map(({ key }) => key).sort()).toEqual(musaV7QuestionKeys);
  }
  const answers = Object.fromEntries(
    questions.map(({ key, options }) => [key, options[0]]),
  );

  const response = await request.post("/api/pde/public/presence-diagnostic", {
    data: { answers, experienceVersion: contract.experienceVersion },
  });
  const responseText = await response.text();

  expect(
    response.ok(),
    `POST publico do diagnostico falhou com HTTP ${response.status()}: ${responseText}`,
  ).toBeTruthy();

  const body = JSON.parse(responseText);
  expect(
    body.requestId,
    "Diagnostico publico deve retornar requestId para polling e auditoria",
  ).toEqual(expect.any(String));
  expect(body.productSlug).toBe(expectedProductSlug);
  expect(body.missionId).toBe(expectedMissionId);
  expect(body.guidanceType).toBe(expectedGuidanceType);
  expect(["PENDING", "COMPLETED"]).toContain(body.status);

  const lookup = await request.get(
    `/api/pde/public/presence-diagnostic/${body.requestId}`,
  );
  const lookupText = await lookup.text();
  expect(
    lookup.ok(),
    `GET publico do diagnostico falhou com HTTP ${lookup.status()}: ${lookupText}`,
  ).toBeTruthy();

  const lookupBody = JSON.parse(lookupText);
  expect(lookupBody.requestId).toBe(body.requestId);
  expect(lookupBody.productSlug).toBe(expectedProductSlug);
  expect(lookupBody.guidanceType).toBe(expectedGuidanceType);
  expect(["PENDING", "COMPLETED"]).toContain(lookupBody.status);
});
