import { resolveSearchConfig, SEARCH_PROVIDERS } from "../src/research.js";

const config = resolveSearchConfig();

if (config.provider !== SEARCH_PROVIDERS.BRAVE) {
  console.error(
    "[product-discovery-worker] preflight falhou: o provedor de busca do runtime não é Brave",
  );
  process.exitCode = 1;
} else if (!config.braveApiKey) {
  console.error(
    "[product-discovery-worker] preflight falhou: o arquivo da credencial Brave está ausente, vazio ou ilegível pelo usuário do runtime",
  );
  process.exitCode = 1;
} else {
  console.log(
    "[product-discovery-worker] preflight de busca aprovado provider=brave credential=CONFIGURED",
  );
}
