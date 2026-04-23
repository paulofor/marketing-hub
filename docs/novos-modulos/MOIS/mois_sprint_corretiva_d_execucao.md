# MOIS — Sprint corretiva D (hardening e limpeza)

## Objetivo

Concluir o hardening da correção arquitetural pós-Sprint 4, removendo legado duplicado no backend, consolidando ownership de domínio e reforçando o alinhamento operacional entre backend e módulo `mois`.

## Entregas realizadas

1. Remoção do legado de implementação interna de domínio MOIS dentro de `backend/ads-service`:
   - remoção de entidades JPA MOIS no backend;
   - remoção de repositórios MOIS no backend;
   - remoção do serviço legado `MoisApiStubService`;
   - remoção do gateway legado de market-research do backend.
2. Limpeza de configuração não utilizada no backend:
   - remoção do bloco `integrations.mois.market-research.*` de `application.properties`.
3. Limpeza de testes legados:
   - remoção do teste unitário `MoisApiStubServiceTest`, que validava fluxo local antigo do domínio MOIS.

## Decisão explícita de ownership de dados (mandatória)

Para a fase atual de transição, fica formalizada a **Opção 2 (MOIS como bounded context operacional)**:

- execução de domínio MOIS acontece no serviço `mois/`;
- backend atua como façade institucional e integração (`/api/v1/mois/*`);
- tabelas legadas criadas no backend em sprints anteriores passam a ser consideradas **legado transitório**, sem expansão funcional;
- novas evoluções de domínio MOIS devem ocorrer no módulo `mois/`.

## Observabilidade e deploy (revisão)

- endpoint de saúde institucional do módulo permanece em `GET /api/v1/mois/health`;
- endpoint técnico permanece em `GET /actuator/health`;
- serviço `mois` segue com container próprio e healthcheck em `deploy/docker-compose.yml`.

## Resultado arquitetural após a Sprint D

- backend: integração/orquestração institucional de contratos MOIS;
- mois: execução de domínio e evolução funcional do bounded context;
- eliminado o estado híbrido em que backend acumulava implementação interna do MOIS por conveniência.
