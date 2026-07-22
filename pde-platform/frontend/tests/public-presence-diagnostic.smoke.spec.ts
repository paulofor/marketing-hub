import { expect, test } from '@playwright/test';

const expectedProductSlug = 'metodo-musa-7-dias';
const expectedMissionId = 'diagnostico-presenca-publico';
const expectedGuidanceType = 'MUSA_PUBLIC_PRESENCE_DIAGNOSTIC';

function smokeRunId() {
  return process.env.GITHUB_RUN_ID || `local-${Date.now()}`;
}

test('POST publico do diagnostico MUSA cria solicitacao valida', async ({ request }) => {
  const answers = {
    presenceFocus: 'Trabalho ou reunião',
    mainObstacle: 'Falta acabamento',
    desiredSignal: 'Elegância discreta',
    mainConstraint: 'Pouco tempo',
    startingResource: 'Roupa que já tenho',
    smokeRunId: `post-deploy-musa-${smokeRunId()}`,
  };

  const response = await request.post('/api/pde/public/presence-diagnostic', {
    data: { answers },
  });
  const responseText = await response.text();

  expect(response.ok(), `POST publico do diagnostico falhou com HTTP ${response.status()}: ${responseText}`).toBeTruthy();

  const body = JSON.parse(responseText);
  expect(body.requestId, 'Diagnostico publico deve retornar requestId para polling e auditoria').toEqual(expect.any(String));
  expect(body.productSlug).toBe(expectedProductSlug);
  expect(body.missionId).toBe(expectedMissionId);
  expect(body.guidanceType).toBe(expectedGuidanceType);
  expect(['PENDING', 'COMPLETED']).toContain(body.status);

  const lookup = await request.get(`/api/pde/public/presence-diagnostic/${body.requestId}`);
  const lookupText = await lookup.text();
  expect(lookup.ok(), `GET publico do diagnostico falhou com HTTP ${lookup.status()}: ${lookupText}`).toBeTruthy();

  const lookupBody = JSON.parse(lookupText);
  expect(lookupBody.requestId).toBe(body.requestId);
  expect(lookupBody.productSlug).toBe(expectedProductSlug);
  expect(lookupBody.guidanceType).toBe(expectedGuidanceType);
  expect(['PENDING', 'COMPLETED']).toContain(lookupBody.status);
});
