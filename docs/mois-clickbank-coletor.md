# Plano do Coletor MOIS ClickBank

Este documento define um plano inicial para implementar no ClickBank um coletor com a mesma estratégia usada no `mois-hotmart-collector`.

## Objetivo

Criar um microserviço Java 21 + Spring Boot 3 dedicado à coleta de ofertas/produtos no ClickBank, com:

- execução manual via endpoint HTTP;
- execução automática via scheduler (cron);
- configuração por variáveis de ambiente;
- isolamento de deploy em container próprio;
- contrato simples para integração com o MOIS principal.

## Arquitetura proposta (espelho do Hotmart)

- Serviço: `mois-clickbank-collector`
- Linguagem/runtime: Java 21
- Framework: Spring Boot 3
- Dependências base:
  - `spring-boot-starter-web`
  - `spring-boot-starter-validation`
  - `spring-boot-starter-actuator`
  - `spring-boot-starter-test`
  - `playwright` (para automação browser quando necessário)

## Endpoints iniciais

- `GET /api/v1/mois-clickbank/health`
- `POST /api/v1/mois-clickbank/collections`
- `GET /internal/ops-monitor/health`
- `GET /internal/ops-monitor/loggers`

## Fluxo de coleta (baseline)

1. Receber requisição com fonte e limite de produtos.
2. Coletar dados no ClickBank (modo API oficial, quando disponível; fallback browser automation).
3. Normalizar campos mínimos:
   - `id`
   - `title`
   - `description`
   - `currency`
   - `price`
   - `checkoutUrl`
   - `source`
4. Devolver payload padronizado para consumo do MOIS.

## Configuração sugerida

Variáveis de ambiente:

- `MOIS_CLICKBANK_PORT` (default `8097`)
- `COLLECTOR_CLICKBANK_SEARCH_URL`
- `COLLECTOR_CLICKBANK_USERNAME`
- `COLLECTOR_CLICKBANK_PASSWORD`
- `COLLECTOR_CLICKBANK_SESSION_COOKIE`
- `COLLECTOR_SCHEDULER_ENABLED` (default `true`)
- `COLLECTOR_SCHEDULER_CRON` (default `0 0 * * * *`)
- `COLLECTOR_SCHEDULER_SOURCE` (default `clickbank-market`)
- `COLLECTOR_SCHEDULER_MAX_PRODUCTS` (default `25`)
- `COLLECTOR_PLAYWRIGHT_HEADLESS` (default `true`)

## Próximos passos

1. Validar contrato de retorno com o módulo MOIS.
2. Implementar cliente de coleta ClickBank (API/automação).
3. Adicionar testes unitários para controller e service.
4. Versionar fluxo de deploy em workflow dedicado.
5. Definir estratégia de persistência/encaminhamento no backend principal, mantendo o modelo único.
