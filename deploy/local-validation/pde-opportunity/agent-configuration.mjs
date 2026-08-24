const defaultModel = "gpt-5.6-terra";
const supportedModels = new Set([defaultModel]);
const defaultAgentTimeoutMs = 600_000;

/** Resolve somente a configuração própria do processo para evitar herança acidental do ambiente. */
export function resolveOpportunityModel(environment = process.env) {
  const model = String(environment.PDE_OPPORTUNITY_MODEL || defaultModel).trim();
  if (!supportedModels.has(model)) {
    throw new Error(`Modelo PDE não homologado: ${model}`);
  }
  return model;
}

/** Limita a execução completa do agente sem herdar timeout genérico do ambiente. */
export function resolveOpportunityAgentTimeoutMs(environment = process.env) {
  const configured = String(
    environment.PDE_OPPORTUNITY_AGENT_TIMEOUT_MS || defaultAgentTimeoutMs,
  ).trim();
  const timeoutMs = Number(configured);
  if (!Number.isInteger(timeoutMs) || timeoutMs < 30_000 || timeoutMs > 900_000) {
    throw new Error(
      "PDE_OPPORTUNITY_AGENT_TIMEOUT_MS deve ser um inteiro entre 30000 e 900000.",
    );
  }
  return timeoutMs;
}
