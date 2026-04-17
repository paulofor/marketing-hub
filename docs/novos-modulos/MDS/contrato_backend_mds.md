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
