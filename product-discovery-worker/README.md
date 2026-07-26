# Product Discovery Worker

Worker operacional da Descoberta de Produtos PDE v1.

## Responsabilidade

- consumir pendências do backend em `/api/internal/product-discovery/productdiscovery/v1/research/stage-executions/pending`;
- pesquisar sinais públicos sem coletar dados pessoais;
- gerar oportunidades PDE com evidências, score e decisão;
- reportar sucesso ou falha ao backend.

O worker não cria produto, hipótese, landing, campanha ou gasto de mídia.

## Variáveis

- `BACKEND_BASE_URL`: URL do backend principal. Padrão: `http://191.252.181.168`.
- `PRODUCT_DISCOVERY_POLL_INTERVAL_MS`: intervalo de polling. Padrão: `60000`.
- `PRODUCT_DISCOVERY_MAX_SEARCH_RESULTS`: máximo de resultados por consulta. Padrão: `8`.
- `PRODUCT_DISCOVERY_SEARCH_PROVIDER`: provedor dedicado de busca. Aceita `brave`,
  `tavily`, `serpapi` ou `duckduckgo`. Quando vazio, o worker escolhe pela primeira
  chave disponível nesta ordem: Brave, Tavily, SerpAPI e DuckDuckGo.
- `BRAVE_SEARCH_API_KEY`: chave da Brave Search API.
- `TAVILY_API_KEY`: chave da Tavily Search API.
- `SERPAPI_API_KEY`: chave da SerpAPI.
- `PRODUCT_DISCOVERY_SEARCH_COUNTRY`: país usado na busca. Padrão: `br`.
- `PRODUCT_DISCOVERY_SEARCH_LANGUAGE`: idioma usado na busca. Padrão: `pt-br`.

## Provedor recomendado

Use Brave como primeiro provedor dedicado (`PRODUCT_DISCOVERY_SEARCH_PROVIDER=brave`)
porque entrega resultados web estruturados a partir de índice próprio e preserva melhor
o sinal bruto de dor, lacuna e concorrência. Tavily é útil quando a pesquisa precisar
de conteúdo mais pronto para agente. SerpAPI é útil quando a validação depender
especificamente do que aparece no Google.

DuckDuckGo fica apenas como fallback sem chave e não deve ser considerado evidência de
escala suficiente para decisões comerciais fortes.

## Execução local

```bash
PRODUCT_DISCOVERY_SEARCH_PROVIDER=brave \
BRAVE_SEARCH_API_KEY=... \
npm test
npm start
```
