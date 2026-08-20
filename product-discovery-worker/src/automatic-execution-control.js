const AGENT_KEY = "market-radar";

/** Cria a verificação fail-closed usada antes de cada pesquisa automática de Argos. */
export function createAutomaticExecutionControl({
  backendBaseUrl,
  fetchImpl = fetch,
  logger = console,
}) {
  return {
    async allowsAutomaticExecution() {
      const url = `${backendBaseUrl}/api/internal/agents/executor-health/${AGENT_KEY}/automatic-execution`;
      try {
        const response = await fetchImpl(url, {
          headers: { Accept: "application/json" },
          signal: AbortSignal.timeout(3000),
        });
        if (!response.ok) {
          throw new Error(`GET ${url} failed with status ${response.status}`);
        }
        const state = await response.json();
        return state?.automaticExecutionEnabled === true;
      } catch (error) {
        logger.error(
          "[product-discovery-worker] PLAY could not be confirmed; Argos remains stopped",
          error,
        );
        return false;
      }
    },
  };
}
