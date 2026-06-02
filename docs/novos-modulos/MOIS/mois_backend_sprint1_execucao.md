# MOIS — Backend Sprint 1 (fundação)

## Objetivo

Registrar a implementação técnica mínima da Sprint 1 do MOIS no `backend/ads-service`, mantendo o módulo em formato de stub contratual e sem antecipar persistência de banco ou enriquecimentos de sprints posteriores.

## Escopo implementado

- criação do pacote backend `com.marketinghub.mois` com separação em `dto`, `service` e `web`;
- criação do controller REST base `MoisController` com endpoints de contrato inicial;
- criação de DTOs iniciais de request/response alinhados ao `docs/swagger/openapi_mois_backend_stub.yaml`;
- criação de serviço stub (`MoisApiStubService`) para respostas controladas e previsíveis;
- criação de testes de contrato web (`MoisControllerContractTest`) para validações essenciais de Sprint 1.

## Endpoints stub implementados

Base path: `/api/v1/mois`

- `POST /discovery-requests`
- `GET /discovery-requests`
- `GET /discovery-requests/{requestId}`
- `POST /discovery-requests/{requestId}/run`
- `GET /offers`
- `GET /offers/{offerId}`
- `GET /insight-reports`
- `GET /insight-reports/{reportId}`
- `GET /artifacts/{artifactId}`
- `GET /health`

## Decisões importantes

1. **Sem banco na Sprint 1**
   - não foram criadas tabelas, entidades JPA ou migrations;
   - a camada retorna dados stub para validar contratos e bootstrap.

2. **Contrato-first**
   - os DTOs seguem a nomenclatura e estrutura do OpenAPI inicial do MOIS;
   - os códigos HTTP principais (202, 200, 404, 400) já estão modelados.

3. **Compatibilidade incremental**
   - o módulo foi adicionado sem alterar endpoints existentes de outros domínios;
   - o caminho `/api/v1/mois` evita colisões com módulos legados.

## Pendências intencionais para Sprint 2

- persistência inicial (`marketOfferDiscoveryRequest`, snapshots e cards);
- lineage persistido e consultável;
- migrations Liquibase MySQL 5.7;
- integração com infraestrutura reaproveitável de pesquisa/snapshot.
