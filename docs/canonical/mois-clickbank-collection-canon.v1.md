# MOIS — Documento Canônico Unificado da Coleta ClickBank

## 1. Objetivo

Este documento é a fonte única de verdade para a coleta ClickBank no MOIS, consolidando:
- definição do ciclo de coleta e escopo funcional;
- contrato operacional do coletor `mois-clickbank-collector`;
- fetch de referência para consulta GraphQL;
- parâmetros e defaults extraídos do código em produção.

A referência principal para comportamento é o código do submódulo `mois-clickbank-collector`.

## 2. Escopo atual implementado (fonte: código)

A coleta executada hoje pelo endpoint padrão de coleção chama o **Ciclo 3 (GraphQL)**:
- `POST /api/v1/mois-clickbank/collections` delega para `collectThirdCycleGraphql`.
- o limite de produtos é normalizado para `1..50` (default efetivo `10` quando inválido/ausente).
- status possíveis: `COLLECTION_EXECUTED`, `COLLECTION_SKIPPED`, `COLLECTION_ERROR`.

Fluxos existentes no serviço:
1. **Ciclo 1**: coleta pública Top Offers (`collectFirstCycle`) — disponível no serviço, não é o default do endpoint.
2. **Ciclo 2**: coleta derivada de base persistida (`collectSecondCycleFromBackend`) — fluxo complementar.
3. **Ciclo 3**: coleta via GraphQL autenticado (`collectThirdCycleGraphql`) — fluxo default.

## 3. Contrato HTTP do coletor

Base path:
- `/api/v1/mois-clickbank`

Endpoints:
- `GET /api/v1/mois-clickbank/health` → retorna `ok`.
- `POST /api/v1/mois-clickbank/collections` → executa coleta e retorna payload com status/mensagem/produtos.

Observabilidade:
- `GET /internal/ops-monitor/health`
- `GET /internal/ops-monitor/loggers`
- `GET /internal/ops-monitor/logfile`

## 4. Fetch de referência (Ciclo 3 GraphQL)

Endpoint GraphQL:
- URL default: `https://accounts.clickbank.com/graphql`
- Método: `POST`
- Headers mínimos:
  - `accept: application/json`
  - `content-type: application/json`
  - `authorization: Bearer <CLICKBANK_JWT_TOKEN>`

Query utilizada no coletor:

```graphql
query ($parameters: MarketplaceSearchParameters!) {
  marketplaceSearch(parameters: $parameters) {
    hits {
      title
      url
      marketplaceStats {
        category
        gravity
        rank
        sellerVolume
      }
    }
  }
}
```

Exemplo de body (alinhado ao código):

```json
{
  "query": "query ($parameters: MarketplaceSearchParameters!) { marketplaceSearch(parameters: $parameters) { hits { title url marketplaceStats { category gravity rank sellerVolume } } } }",
  "variables": {
    "parameters": {
      "sortField": "rank",
      "sortDescending": false,
      "productAttributes": ["shippable"],
      "resultsPerPage": 25,
      "offset": 0,
      "nicknameMasq": null
    }
  }
}
```

Regras de fallback/skip do Ciclo 3:
- JWT ausente → `COLLECTION_SKIPPED` com motivo `JWT_ABSENT`.
- HTTP `401/403` → `COLLECTION_SKIPPED` com motivo `JWT_EXPIRED_OR_INVALID`.
- retorno sem hits válidos → `COLLECTION_SKIPPED` com motivo `GRAPHQL_EMPTY_RESULT`.
- erro de request/integração → `COLLECTION_SKIPPED` (`REQUEST_ERROR`) ou `COLLECTION_ERROR` em exceção superior.

## 5. Configurações e defaults (application.properties)

Principais chaves:
- `collector.clickbank.graphql-url` (default `https://accounts.clickbank.com/graphql`)
- `collector.clickbank.top-offers-url` (default `https://www.clickbank.com/blog/clickbank-top-offers/`)
- `collector.backend.base-url` (default `http://191.252.181.168:8000`)
- `collector.clickbank.jwt-setting-key` (default `clickbank_access_token_jwt`)
- `collector.scheduler.enabled` (default `false` na operação Hotmart-only; reativar explicitamente apenas quando ClickBank voltar a ser fonte ativa)
- `collector.scheduler.cron` (default `0 0 */2 * * *`)
- `collector.scheduler.max-products` (default `25`)

Porta e app:
- `server.port` default `9096`
- `spring.application.name=mois-clickbank-collector`

## 6. Persistência e rastreabilidade

A coleta gera snapshots de produto e persiste no backend MOIS com metadados de origem.
Para diagnóstico e rastreabilidade, manter logging do payload bruto recebido da fonte antes de transformação.

## 7. Consolidação documental

Este documento substitui, como referência operacional principal, os conteúdos antes espalhados em:
- `docs/mois-clickbank-coletor.md`
- `docs/mois/clickbase-fetch-ciclo-consulta.md`

A partir desta consolidação, novas mudanças de contrato/fluxo devem ser refletidas primeiro no código e, em seguida, neste cânone unificado.
