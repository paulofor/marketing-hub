# Swagger/OpenAPI

Esta pasta é o local único para contratos Swagger/OpenAPI do Marketing Hub.

## Regra de armazenamento

- Todo contrato Swagger/OpenAPI versionado no repositório deve ficar em `docs/swagger`.
- O nome do arquivo deve identificar claramente o módulo ou integração, preferencialmente no formato `<modulo>-swagger.yaml` ou `<modulo>-openapi.yaml`.
- Ao criar, alterar ou remover endpoints, atualize no mesmo PR o contrato correspondente nesta pasta.
- Não mantenha contratos Swagger/OpenAPI espalhados em `docs/canonical`, `docs/novos-modulos`, subpastas do backend ou outras pastas; documentos canônicos e planos devem apenas referenciar o arquivo em `docs/swagger`.

## Contratos atuais

- `docs/swagger/avatar-sales-video-integration-swagger.yaml` — integração Avatar Sales Video.
- `docs/swagger/epm-swagger.yaml` — Experiment Profit Manager.
- `docs/swagger/geralanding-backend-swagger.v1.yaml` — backend GeraLanding.
- `docs/swagger/openapi.yaml` — superfície geral da API Marketing Hub.
- `docs/swagger/openapi_mds_backend_stub.yaml` — stub de integração MDS.
- `docs/swagger/openapi_mois_backend_stub.yaml` — stub de integração MOIS.
- `docs/swagger/oprm-backend-integration-openapi.v1.yaml` — integração OPRM ↔ backend.
- `docs/swagger/oprm-backend-required-endpoints.swagger.yaml` — endpoints obrigatórios do backend para OPRM.
- `docs/swagger/oprm-nichocnae-swagger.yaml` — pipeline OPRM Nicho CNAE.
