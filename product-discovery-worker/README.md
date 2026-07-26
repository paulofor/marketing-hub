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

## Execução local

```bash
npm test
npm start
```
