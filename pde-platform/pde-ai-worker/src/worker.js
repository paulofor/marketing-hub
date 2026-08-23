import fs from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const backendUrl = (process.env.PDE_BACKEND_URL ?? 'http://pde-platform-backend:8096').replace(/\/+$/, '');
const pdeInternalToken = (process.env.PDE_INTERNAL_API_TOKEN ?? '').trim();
const pollIntervalMs = Number(process.env.POLL_INTERVAL_MS ?? '4000');
const openaiModel = process.env.OPENAI_MODEL ?? 'gpt-5.5';
const openaiApiKey = await resolveOpenAiApiKey();
const promptDefinitions = {
  MUSA_PUBLIC_PRESENCE_DIAGNOSTIC: {
    dir: path.resolve(__dirname, '../prompts/musa-public-presence-diagnostic'),
    schemaName: 'musa_public_presence_diagnostic',
  },
  MUSA_DAY_1_PRESENCE_DIAGNOSIS: {
    dir: path.resolve(__dirname, '../prompts/musa-daily-guidance'),
    schemaName: 'musa_day_1_presence_diagnosis',
  },
  MUSA_DAY_2_SIGNATURE: {
    dir: path.resolve(__dirname, '../prompts/musa-day-2-signature'),
    schemaName: 'musa_day_2_signature',
  },
  MUSA_DAY_3_WARDROBE_REUSE: {
    dir: path.resolve(__dirname, '../prompts/musa-daily-guidance'),
    schemaName: 'musa_day_3_wardrobe_reuse',
  },
  MUSA_DAY_4_FINISHING_RITUAL: {
    dir: path.resolve(__dirname, '../prompts/musa-daily-guidance'),
    schemaName: 'musa_day_4_finishing_ritual',
  },
  MUSA_DAY_5_ANTI_IMPULSE_DECISION: {
    dir: path.resolve(__dirname, '../prompts/musa-daily-guidance'),
    schemaName: 'musa_day_5_anti_impulse_decision',
  },
  MUSA_DAY_6_OCCASION_ENTRY: {
    dir: path.resolve(__dirname, '../prompts/musa-daily-guidance'),
    schemaName: 'musa_day_6_occasion_entry',
  },
  MUSA_DAY_7_MAINTENANCE_PLAN: {
    dir: path.resolve(__dirname, '../prompts/musa-daily-guidance'),
    schemaName: 'musa_day_7_maintenance_plan',
  },
};

async function main() {
  if (!pdeInternalToken) {
    throw new Error('PDE_INTERNAL_API_TOKEN não configurado no pde-ai-worker');
  }
  console.log(`PDE AI Worker iniciado; backendUrl=${backendUrl}, model=${openaiModel}`);
  while (true) {
    try {
      await processNextPending();
    } catch (error) {
      console.error('Falha no ciclo do worker PDE AI', error);
    }
    await sleep(pollIntervalMs);
  }
}

async function processNextPending() {
  const pending = await fetchJson(`${backendUrl}/api/internal/pde/ai-guidance/stage-executions/pending`, {
    headers: { 'X-PDE-Internal-Token': pdeInternalToken },
  });
  const [execution] = Array.isArray(pending) ? pending : [];
  if (!execution) {
    return;
  }
  console.log(`Orientacao PDE pendente recebida; requestId=${execution.requestId}, guidanceType=${execution.guidanceType}`);
  if (!openaiApiKey) {
    await postResult(execution.requestId, {
      status: 'FAILED',
      errorMessage: 'OPENAI_API_KEY nao configurada no pde-ai-worker',
    });
    return;
  }
  try {
    const result = await generateGuidance(execution);
    await postResult(execution.requestId, result);
  } catch (error) {
    console.error(`Falha ao gerar orientacao PDE; requestId=${execution.requestId}`, error);
    await postResult(execution.requestId, {
      status: 'FAILED',
      model: openaiModel,
      errorMessage: error instanceof Error ? error.message : String(error),
    });
  }
}

async function resolveOpenAiApiKey() {
  const keyFile = process.env.OPENAI_API_KEY_FILE ?? '/run/secrets/openai_api_key';
  try {
    const fileKey = (await fs.readFile(keyFile, 'utf8')).trim();
    if (fileKey) {
      return fileKey;
    }
  } catch (error) {
    if (error?.code !== 'ENOENT') {
      console.warn(`Nao foi possivel ler OPENAI_API_KEY_FILE em ${keyFile}; usando fallback por variavel de ambiente`, error);
    }
  }
  return (process.env.OPENAI_API_KEY ?? '').trim();
}

async function generateGuidance(execution) {
  const promptDefinition = promptDefinitions[execution.guidanceType];
  if (!promptDefinition) {
    throw new Error(`Tipo de orientacao PDE nao suportado pelo worker: ${execution.guidanceType}`);
  }
  const systemPrompt = await fs.readFile(path.join(promptDefinition.dir, 'system.md'), 'utf8');
  const userTemplate = await fs.readFile(path.join(promptDefinition.dir, 'user.md'), 'utf8');
  const schema = JSON.parse(await fs.readFile(path.join(promptDefinition.dir, 'response-schema.json'), 'utf8'));
  const mission = execution.product.missions.find((item) => item.id === execution.missionId) ?? {};
  const scientificEvidencePack = requireScientificEvidencePack(execution);
  const userPrompt = renderTemplate(userTemplate, {
    productName: execution.product.name,
    productPromise: execution.product.promise,
    guidanceType: execution.guidanceType,
    missionDay: String(mission.day ?? ''),
    missionTitle: mission.title ?? execution.missionId,
    missionPrinciple: mission.principle ?? '',
    missionAction: mission.action ?? '',
    missionEvidence: mission.evidence ?? '',
    scientificEvidencePackJson: JSON.stringify(scientificEvidencePack, null, 2),
    previousMissionAnswersJson: JSON.stringify(execution.previousMissionAnswers ?? {}, null, 2),
    answersJson: JSON.stringify(execution.answers ?? {}, null, 2),
  });
  const requestBody = {
    model: openaiModel,
    input: [
      {
        role: 'system',
        content: systemPrompt,
      },
      {
        role: 'user',
        content: userPrompt,
      },
    ],
    text: {
      format: {
        type: 'json_schema',
        name: promptDefinition.schemaName,
        strict: true,
        schema,
      },
    },
  };
  const response = await callOpenAiWithRetry(requestBody, resolveServiceTierAttempts(execution.guidanceType));
  const outputText = extractOutputText(response.body);
  const parsed = JSON.parse(outputText);
  return {
    status: 'COMPLETED',
    headline: parsed.headline,
    summary: parsed.summary,
    signals: parsed.signals,
    microActions: parsed.microActions,
    caution: parsed.caution,
    model: openaiModel,
    serviceTier: response.serviceTier,
    rawRequestJson: JSON.stringify(response.requestBody),
    rawResponseJson: JSON.stringify(response.body),
    inputTokens: response.body.usage?.input_tokens,
    outputTokens: response.body.usage?.output_tokens,
  };
}

function requireScientificEvidencePack(execution) {
  const pack = execution.product?.scientificEvidencePack;
  const requiredLists = ['principles', 'practicalApplications', 'allowedLanguage', 'forbiddenClaims', 'references'];
  const hasRequiredLists = requiredLists.every((field) => Array.isArray(pack?.[field]) && pack[field].length > 0);
  if (!pack?.version || !hasRequiredLists) {
    throw new Error(`Pacote cientifico operacional ausente ou incompleto para orientacao MUSA; requestId=${execution.requestId}`);
  }
  return pack;
}

function resolveServiceTierAttempts(guidanceType) {
  if (guidanceType === 'MUSA_PUBLIC_PRESENCE_DIAGNOSTIC') {
    return ['default', 'flex', 'default'];
  }
  return ['flex', 'flex', 'default'];
}

async function callOpenAiWithRetry(baseRequestBody, attempts) {
  let lastError;
  for (const tier of attempts) {
    const requestBody = tier === 'default'
      ? withoutServiceTier(baseRequestBody)
      : { ...baseRequestBody, service_tier: 'flex' };
    try {
      const response = await fetch('https://api.openai.com/v1/responses', {
        method: 'POST',
        headers: {
          Authorization: `Bearer ${openaiApiKey}`,
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(requestBody),
      });
      const body = await response.json().catch(() => ({}));
      if (!response.ok) {
        const message = body.error?.message ?? `OpenAI retornou HTTP ${response.status}`;
        if (isTransient(response.status, body) && tier !== attempts.at(-1)) {
          lastError = new Error(message);
          continue;
        }
        throw new Error(message);
      }
      return { body, requestBody, serviceTier: tier };
    } catch (error) {
      lastError = error;
      if (tier === attempts.at(-1)) {
        break;
      }
    }
  }
  throw lastError;
}

function extractOutputText(body) {
  if (typeof body.output_text === 'string' && body.output_text.trim()) {
    return body.output_text;
  }
  const textParts = [];
  for (const item of body.output ?? []) {
    for (const content of item.content ?? []) {
      if (content.type === 'output_text' && content.text) {
        textParts.push(content.text);
      }
    }
  }
  const outputText = textParts.join('\n').trim();
  if (!outputText) {
    throw new Error('Resposta OpenAI sem texto estruturado');
  }
  return outputText;
}

function withoutServiceTier(requestBody) {
  const cloned = { ...requestBody };
  delete cloned.service_tier;
  return cloned;
}

function isTransient(status, body) {
  const code = body.error?.code ?? '';
  return status === 408 || status === 429 || status >= 500 || code === 'rate_limit_exceeded';
}

async function postResult(requestId, payload) {
  await fetchJson(`${backendUrl}/api/internal/pde/ai-guidance/stage-executions/${requestId}/result`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'X-PDE-Internal-Token': pdeInternalToken,
    },
    body: JSON.stringify(payload),
  });
}

async function fetchJson(url, options = {}) {
  const response = await fetch(url, options);
  if (!response.ok) {
    throw new Error(`HTTP ${response.status} em ${url}`);
  }
  return response.json();
}

function renderTemplate(template, values) {
  return Object.entries(values).reduce(
    (rendered, [key, value]) => rendered.replaceAll(`{{${key}}}`, value ?? ''),
    template,
  );
}

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

main();
