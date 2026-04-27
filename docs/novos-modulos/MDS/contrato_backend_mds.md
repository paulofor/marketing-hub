# Contrato Backend ↔ MDS (Sprint 1)

## Escopo
Contrato interno implementado apenas para Sprint 1: persistência, orquestração básica de requests e publicação inicial de artefatos pelo backend principal.

## Endpoints internos implementados
Base path: `/api/internal/mds`

### Requests
- `POST /requests`
- `GET /requests/pending`
- `POST /requests/{id}/claim`
- `POST /requests/{id}/heartbeat`
- `POST /requests/{id}/complete`
- `POST /requests/{id}/fail`
- `GET /requests/{id}`

### Artefatos e lineage
- `POST /artifacts/publish-batch`
- `POST /artifacts/{id}/lineage`

### Operação
- `GET /health`

## Persistência (MySQL 5.7 via backend)
Tabelas adicionadas nesta sprint:
- `mds_request`
- `artifact_record`
- `artifact_lineage_edge`
- `source_access_record`
- `mds_processing_event`

## Regras implementadas
- Somente backend persiste em MySQL.
- Claim só aceita request em `PENDING`.
- Batch de artefatos valida status no envelope (`DRAFT`, `VALIDATED`, `APPROVED`).
- Lineage pode ser criado no batch (via `parentArtifactIds`) ou endpoint dedicado.

## Endpoints administrativos da UI (Sprint 0)
Base path: `/api/mds`

### Requests
- `GET /requests` (paginação + filtros: `status`, `from`, `to`, `tenantOrProduct`)
- `GET /requests/{id}`
- `POST /requests/{id}/retry`

### Artefatos e relatório
- `GET /requests/{id}/artifacts`
- `GET /reports/{requestId}`

### Operação
- `GET /health`

## Controle de acesso da camada administrativa
- Endpoints da UI administrativa exigem header `X-User-Role`.
- Perfis aceitos no MVP: `ADMIN`, `MDS_OPERATOR`, `OPS`.


## Atualização Sprint 2 (artefatos + lineage)
- `GET /api/mds/requests/{id}/artifacts` retorna cada artefato com:
  - metadados (`artifactId`, `artifactType`, `schemaVersion`, `version`, `status`)
  - `parentArtifactIds` e `childArtifactIds` resolvidos
  - `content` para visualização do envelope canônico na UI administrativa

## Atualização Sprint 3 (retry operacional)
- `GET /api/mds/requests` e `GET /api/mds/requests/{id}` incluem:
  - `retryEligible` (boolean)
  - `retryReason` (string)
- `POST /api/mds/requests/{id}/retry` aceita apenas requests `FAILED` na operação padrão da UI administrativa.

## Atualização Sprint 4 (observabilidade da UI)
- `GET /api/mds/health` passa a ser consultado periodicamente pela UI administrativa para monitoramento operacional.
- Estratégia de consumo no frontend:
  - polling configurável na lista de requests (`auto-refresh`);
  - cache com `staleTime` e `keepPreviousData` para reduzir recarga visual e chamadas redundantes.
