# MOIS — Documento Canônico Unificado da Coleta ClickBank

## 1. Objetivo

Este documento é a fonte única de verdade para a coleta ClickBank no MOIS, consolidando:
- definição do ciclo de coleta e escopo funcional;
- contrato operacional do coletor `mois-clickbank-collector`;
- fetch de referência para consulta GraphQL;
- parâmetros e defaults extraídos do código em produção.

A referência principal para comportamento é o código do submódulo `mois-clickbank-collector`.

## 1.1 Estado operacional vigente

Desde 2026-09-28, o runtime ClickBank está retirado da produção. A auditoria confirmou que não há
referências ClickBank persistidas, páginas ingeridas ou consumo pelo Product Discovery, enquanto os
três ciclos horários geravam falhas e carga no VPS MOIS. O código e o contrato HTTP permanecem
versionados para reativação controlada, mas nenhuma coleta automática pode ocorrer até que existam:

1. consumidor de evidência ClickBank validado ponta a ponta;
2. lote persistido sem duplicidade de identidade;
3. confirmação de que a evidência entra em uma decisão comercial auditável.

A retirada ou reativação ocorre sob o escopo controlado `mois-clickbank`; a reativação usa apenas
o despacho manual `activate` do workflow do módulo, seguida de homologação do consumidor. O
Compose produtivo impõe o scheduler desabilitado; uma futura cadência exige alteração versionada
após a homologação do consumidor.

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
- `collector.scheduler.enabled` (default `false`; fonte suspensa até existir consumidor validado)
- `collector.scheduler.cron` (default `0 0 * * * *`)
- `collector.scheduler.max-products` (default `25`)
- `collector.clickbank.username-file` e `collector.clickbank.password-file`: arquivos de uma conta dedicada e restrita, montados somente para leitura; quando configurados, prevalecem sobre variáveis legadas.

As credenciais nunca são entregues a Argos, persistidas no backend ou registradas em logs. O coletor fornece somente evidências estruturadas e não pode promover, comprar ou alterar ofertas. MFA ou desafio de acesso bloqueia a homologação sem contorno automático.

Porta e app:
- `server.port` default `9096`
- `spring.application.name=mois-clickbank-collector`

## 6. Persistência e rastreabilidade

A coleta gera snapshots de produto e persiste no backend MOIS com metadados de origem.
Para diagnóstico e rastreabilidade, manter logging do payload bruto recebido da fonte antes de transformação.

## 7. Consolidação documental

## 6.1 Cadência do Radar

Não existe cadência automática ClickBank no estado vigente. Quando o consumidor for reativado e
homologado, a frequência deve ser definida no executor, com limite de custo e evidência de uso; token
ausente ou inválido continua sendo lacuna operacional, nunca sinal negativo de mercado.

Este documento substitui, como referência operacional principal, os conteúdos antes espalhados em:
- `docs/mois-clickbank-coletor.md`
- `docs/mois/clickbase-fetch-ciclo-consulta.md`

A partir desta consolidação, novas mudanças de contrato/fluxo devem ser refletidas primeiro no código e, em seguida, neste cânone unificado.
