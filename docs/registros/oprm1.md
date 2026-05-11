# Registros — OPRM

> Orientação: todos os registros deste documento devem sempre incluir **data e hora no fuso UTC-3**.
> Este documento segue política de **append-only** (não pode ter nenhuma linha apagada; apenas inserções).

- 2026-05-11 14:25:00 (UTC-3): implementação inicial do plano de importação de CNAEs no backend: criado catálogo `oprm_niche_catalog` via Liquibase, endpoint `POST /api/niches/catalog:ingest` com validações de payload e regra de idempotência por `cnaeCode` normalizado, mantendo backend como único ponto de persistência. Resultado esperado: habilitar carga inicial de todos os CNAEs para suportar mapeamento ocupação↔CNAE e ranking de nichos.
- 2026-05-11 15:05:00 (UTC-3): ajuste de arquitetura solicitado: removida regra de negócio de normalização/deduplicação/upsert do backend na ingestão de catálogo CNAE. O backend passou a atuar apenas como camada de persistência do lote recebido (saveAll), mantendo validações de contrato no DTO e constraints de banco. A lógica de negócio deve permanecer no módulo OPRM/coletor.
- 2026-05-11 15:35:00 (UTC-3): implementação do coletor OPRM para ingestão de catálogo CNAE. Criado endpoint `POST /api/oprm-mei/catalog/collect` no módulo `oprm-coletor-mei`, com lógica de negócio de normalização de `cnaeCode`, deduplicação e envio em lotes para `POST /api/niches/catalog:ingest` no backend. Mantida diretriz: negócio no coletor/OPRM e backend apenas persistência.

- 2026-05-11: Adicionado agendamento de ingestão no OPRM coletor (cron 15:10 America/Sao_Paulo) com payload externo configurável.
