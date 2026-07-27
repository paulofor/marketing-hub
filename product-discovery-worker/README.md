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
- `PRODUCT_DISCOVERY_MAX_SEARCH_RESULTS`: máximo total de resultados públicos usados no ciclo. Padrão: `12`.
- `PRODUCT_DISCOVERY_MIN_SEARCH_QUERIES`: mínimo de consultas diferentes por ciclo antes de encerrar a busca. Padrão: `6`.
- `PRODUCT_DISCOVERY_MAX_SEARCH_QUERIES`: teto de consultas diferentes por ciclo. Padrão: `14`.
- `PRODUCT_DISCOVERY_MAX_RESULTS_PER_QUERY`: máximo de resultados aproveitados por consulta, para evitar que uma única frase domine a evidência. Padrão: `3`.
- `PRODUCT_DISCOVERY_HEALTH_HOST`: host do servidor HTTP de health. Padrão: `0.0.0.0`.
- `PRODUCT_DISCOVERY_HEALTH_PORT`: porta interna do servidor HTTP de health. Padrão: `8080`.
- `PRODUCT_DISCOVERY_HEALTH_PUBLISHED_PORT`: porta publicada no host pelo Compose. Padrão: `18081`.
- `PRODUCT_DISCOVERY_SEARCH_PROVIDER`: provedor dedicado de busca. Aceita `brave`,
  `tavily`, `serpapi` ou `duckduckgo`. Quando vazio, o worker escolhe pela primeira
  chave disponível nesta ordem: Brave, Tavily, SerpAPI e DuckDuckGo.
- `BRAVE_SEARCH_API_KEY`: chave da Brave Search API.
- `BRAVE_SEARCH_API_KEY_FILE`: arquivo com a chave da Brave Search API. Use em
  produção para não expor segredo em variável direta.
- `TAVILY_API_KEY`: chave da Tavily Search API.
- `SERPAPI_API_KEY`: chave da SerpAPI.
- `PRODUCT_DISCOVERY_SEARCH_COUNTRY`: país usado na busca. Padrão: `br`.
- `PRODUCT_DISCOVERY_SEARCH_LANGUAGE`: idioma usado na busca. Padrão: `pt-br`.

## Deploy

O workflow `Product Discovery Worker CI` publica o container no host operacional de
workers `191.252.120.96`.

O deploy roda automaticamente após merge na branch `main` ou `master` quando houver
alteração no worker ou no workflow. A imagem aplicada no host usa tag imutável por
commit (`sha-<commit>`), mantendo também a tag `latest` apenas como conveniência de
registro.

No deploy de produção, o provider padrão é Brave, com busca direcionada ao Brasil
(`PRODUCT_DISCOVERY_SEARCH_COUNTRY=br`, `PRODUCT_DISCOVERY_SEARCH_LANGUAGE=pt-br`).
A chave deve existir no servidor em:

```bash
/root/infra/brave-token/brave_api_key
```

O compose de produção monta esse arquivo como Docker secret em
`/run/secrets/brave_search_api_key` e o worker lê pelo
`BRAVE_SEARCH_API_KEY_FILE`.

O projeto Compose de produção usa o nome `marketinghub-product-discovery-worker`,
para isolar containers, rede e lifecycle dos demais workers.

## Health operacional

O worker expõe `GET /healthz` e `GET /health` na porta interna `8080`. Em
produção, o Compose publica o endpoint apenas em `127.0.0.1:18081` por padrão.
O workflow só conclui o deploy depois que `http://127.0.0.1:18081/healthz`
responde com sucesso no host. A imagem também possui `HEALTHCHECK` Docker interno
contra `/healthz`.

O payload informa o provider ativo, status da chave Brave sem revelar o segredo,
último polling e último ciclo processado:

```json
{
  "service": "product-discovery-worker",
  "status": "UP",
  "activeSearchProvider": "brave",
  "braveSearch": {
    "keyStatus": "CONFIGURED",
    "keySource": "file"
  },
  "lastCycleProcessed": null
}
```

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
