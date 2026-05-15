# Itens obsoletos

## Contexto
Este documento centraliza itens que foram marcados como obsoletos no Marketing Hub para facilitar rastreabilidade, limpeza futura e evitar reativação acidental.

## Frontend — Jornada (obsoleto)

### Status
- **Obsoleto**
- **Data do registro:** 2026-05-15
- **Motivo:** funcionalidade de jornada/jornada de interação removida da superfície do frontend por decisão de produto.

### Itens removidos da superfície da aplicação
- Rotas de jornada removidas do roteador principal:
  - `/journeys`
  - `/journeys/:id`
  - `/journeys/:id/edit`
  - `/journey-templates`
  - `/journey-templates/:id`
  - `/journey-templates/:id/edit`
  - `/journey-templates/new`
- Rotas de jornada de interação removidas:
  - `/interaction-journeys`
  - `/interaction-journeys/new`
  - `/interaction-journeys/:id/edit`
- Seções de navegação removidas do menu lateral:
  - **Interações**
  - **Jornadas**

### Itens arquivados para referência técnica
- Hooks de API movidos para pacote obsoleto:
  - `frontend/src/obsoleto/jornada/useExperimentJourneyAssignments.ts`
  - `frontend/src/obsoleto/jornada/useRebuildExperimentJourney.ts`

### Observação de dependência de dados
Apesar da remoção no frontend, ainda existem dependências de jornada no backend/banco (FKs e modelo), então a remoção total deve ser planejada de forma incremental.
