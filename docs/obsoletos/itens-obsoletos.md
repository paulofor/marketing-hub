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

## Frontend — E-mails de amostra no Experimento (obsoleto)

### Status
- **Obsoleto**
- **Data do registro:** 2026-05-16
- **Motivo:** aba de e-mails de amostra aposentada por decisão de produto; fluxo não deve mais ser acessível na tela de experimento.

### Itens removidos da superfície da aplicação
- A tela de detalhe de experimento (`/experiments/:id`) não exibe mais:
  - Aba **E-mails de amostra**.
  - Conteúdo da tab `sample-emails` com o componente `SampleEmailsTab`.

### Sinalização de obsolescência mantida na UI
- No overview do experimento, o campo **E-mail de amostra** permanece visível com valor fixo **Obsoleto** para registrar a descontinuação.
