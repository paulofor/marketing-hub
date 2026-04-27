# Plano de Implementação de UI do MDS (Documentação de Desenvolvimento)

## 1. Objetivo

Este documento define o plano de implementação da interface administrativa do módulo **MDS (Mechanism Discovery Service)** no Marketing Hub.

A UI deve permitir operação e diagnóstico do fluxo MDS de ponta a ponta, sem romper os contratos canônicos e sem violar os limites de responsabilidade entre módulos.

---

## 2. Princípios obrigatórios de arquitetura

### 2.1 Separação de responsabilidades (regra explícita)

- **No módulo `mds/` ficam as regras de negócio de descoberta de mecanismo**, incluindo formulação de pergunta, busca em fontes, triagem/priorização, composição de candidatos e geração dos artefatos de mecanismo.
- **No backend principal fica toda a leitura e escrita em banco de dados** (MySQL 5.7), além dos contratos de API consumidos pelo frontend.
- O frontend **não** acessa banco de dados e **não** acessa diretamente o módulo `mds/`; toda comunicação passa pelo backend principal.

### 2.2 Aderência canônica

- O envelope de artefatos, status (`DRAFT`, `VALIDATED`, `APPROVED`) e lineage (`parentArtifactIds`) devem permanecer aderentes ao modelo canônico em uso no ecossistema.
- Toda evolução de tela deve preservar rastreabilidade entre request, evidências, mecanismo selecionado e relatório final.

---

## 3. Escopo funcional da UI (MVP)

## 3.1 Tela 1 — Lista de Requests MDS

**Objetivo:** visão operacional da fila e do estado dos processamentos.

**Conteúdo mínimo:**
- requestId
- mercado / dor / resultado esperado
- status
- estágio atual
- tentativa / heartbeat / última atualização
- ação de abrir detalhe

**Filtros mínimos:**
- status
- período
- tenant/produto (quando aplicável)

## 3.2 Tela 2 — Detalhe da Request

**Objetivo:** diagnóstico da execução de uma request específica.

**Conteúdo mínimo:**
- dados de contexto da request
- timeline de estágios
- classificação de falha (recuperável x não recuperável)
- links para artefatos e relatório

## 3.3 Tela 3 — Artefatos e Lineage

**Objetivo:** auditoria funcional do que foi produzido.

**Conteúdo mínimo:**
- lista de artefatos por tipo (sourceDocument, evidenceItem, mechanismCandidate, mechanismSpec, practicalKnowledgePack, mechanismDiscoveryReport)
- visualização do envelope canônico
- lineage básico (pais/filhos)

## 3.4 Tela 4 — Relatório de Descoberta

**Objetivo:** leitura executiva e técnica do resultado do MDS.

**Conteúdo mínimo:**
- mecanismo recomendado
- evidências selecionadas
- nível de confiança e limitações
- justificativa resumida da escolha

---

## 4. Contratos de API para a UI

Para a UI, o backend principal deve expor endpoints administrativos (autenticados/autorizados), sem acesso direto ao banco pelo frontend.

## 4.1 Endpoints sugeridos para frontend

- `GET /api/mds/requests`
- `GET /api/mds/requests/{id}`
- `GET /api/mds/requests/{id}/artifacts`
- `GET /api/mds/reports/{requestId}`
- `POST /api/mds/requests/{id}/retry`
- `GET /api/mds/health`

## 4.2 Regras de contrato

- Paginação e filtros obrigatórios na listagem.
- Payloads de erro devem explicitar campo/regra violados quando houver 4xx.
- Campos de status e estágio devem ser estáveis para uso em badges e timeline.

---

## 5. Plano de implementação por sprint

## Sprint 0 — Contrato e preparação

**Objetivo:** fechar contratos e estrutura-base.

**Entregas previstas:**
- definição dos DTOs de listagem/detalhe/artefatos/relatório
- definição das rotas frontend
- validação de permissões e papéis
- alinhamento com documentos canônicos

## Sprint 1 — Lista e detalhe de requests

**Objetivo:** operação mínima do fluxo.

**Entregas previstas:**
- Tela de lista de requests
- Tela de detalhe com timeline de estágios
- estados de loading/erro/vazio
- testes unitários de componentes e hooks principais

## Sprint 2 — Artefatos e lineage

**Objetivo:** rastreabilidade operacional.

**Entregas previstas:**
- Tela de artefatos por request
- visualização de envelope canônico
- navegação de lineage básico
- testes de contrato frontend↔backend para artefatos

## Sprint 3 — Relatório e ações operacionais

**Objetivo:** fechar ciclo de diagnóstico e ação.

**Entregas previstas:**
- Tela de relatório final
- ação de retry para requests elegíveis
- refinamento de UX para falhas e mensagens operacionais
- testes E2E do fluxo principal

## Sprint 4 — Hardening e governança

**Objetivo:** robustez para operação contínua.

**Entregas previstas:**
- ajustes de observabilidade na UI
- melhoria de performance (polling/caching)
- revisão de acessibilidade e consistência visual
- revisão final de aderência canônica

---

## 6. Registro de execução por sprint (preencher continuamente)

> Use este bloco para registrar a execução real. Não apagar histórico; apenas acrescentar novas entradas.

### Sprint 0 — Registro

- **Status:** `CONCLUÍDO`
- **Período:** `2026-04-27 até 2026-04-27`
- **Responsáveis:** `Codex (implementação assistida)`
- **Escopo realizado:**
  - definição de DTOs administrativos de listagem, detalhe, timeline, artefatos/lineage e retry no backend principal
  - criação dos endpoints administrativos autenticáveis por papel em `/api/mds/*`
  - criação das rotas frontend para lista, detalhe, artefatos e relatório
- **Endpoints criados/alterados:**
  - `GET /api/mds/requests`
  - `GET /api/mds/requests/{id}`
  - `GET /api/mds/requests/{id}/artifacts`
  - `GET /api/mds/reports/{requestId}`
  - `POST /api/mds/requests/{id}/retry`
  - `GET /api/mds/health`
- **Telas criadas/alteradas:**
  - `frontend/src/pages/mds/MdsWorkspacePage.tsx`
  - `frontend/src/pages/mds/MdsRequestDetailPage.tsx`
  - `frontend/src/pages/mds/MdsArtifactsPage.tsx`
  - `frontend/src/pages/mds/MdsReportPage.tsx`
- **Testes executados:**
  - `cd backend/ads-service && mvn -Dtest=MdsAdminControllerContractTest,MdsInternalControllerContractTest test`
  - `cd frontend && npm run build`
- **Riscos pendentes:**
  - validação final de RBAC com fonte oficial de identidade ainda depende da camada global de autenticação
- **Decisões tomadas:**
  - filtro de tenant/produto foi mapeado no MVP para `correlationId`
  - classificação de falha recuperável x não recuperável foi inicialmente heurística para suportar diagnóstico operacional

### Sprint 1 — Registro

- **Status:** `CONCLUÍDO`
- **Período:** `2026-04-27 até 2026-04-27`
- **Responsáveis:** `Codex (implementação assistida)`
- **Escopo realizado:**
  - implementação da Tela 1 (lista de requests) com filtros por status/período/tenant-produto, badges de status e ações para detalhe/artefatos/relatório/retry
  - implementação da Tela 2 (detalhe da request) com diagnóstico, classificação de falha e timeline de estágios
  - finalização dos estados de loading/erro/vazio nas telas principais da sprint
  - inclusão de testes unitários para hooks e páginas principais do fluxo da Sprint 1
- **Endpoints criados/alterados:**
  - `GET /api/mds/requests`
  - `GET /api/mds/requests/{id}`
  - `POST /api/mds/requests/{id}/retry`
- **Telas criadas/alteradas:**
  - `frontend/src/pages/mds/MdsWorkspacePage.tsx`
  - `frontend/src/pages/mds/MdsRequestDetailPage.tsx`
- **Testes executados:**
  - `cd frontend && npm run test -- --run src/api/mds/useMdsAdmin.test.tsx src/pages/mds/MdsWorkspacePage.test.tsx src/pages/mds/MdsRequestDetailPage.test.tsx`
  - `cd frontend && npm run build`
  - `cd backend/ads-service && mvn -Dtest=MdsAdminControllerContractTest test`
- **Riscos pendentes:**
  - dependência de disponibilidade do Maven Central ainda pode bloquear execução de testes Java na infraestrutura
- **Decisões tomadas:**
  - os filtros de período da UI foram padronizados em UTC (`T00:00:00Z` e `T23:59:59Z`) para consistência de consulta
  - os testes de sprint foram concentrados em hooks e componentes principais de lista e detalhe

### Sprint 2 — Registro

- **Status:** `CONCLUÍDO`
- **Período:** `2026-04-27 até 2026-04-27`
- **Responsáveis:** `Codex (implementação assistida)`
- **Escopo realizado:**
  - evolução da tela de artefatos por request com agrupamento por tipo, seleção de item e leitura operacional
  - implementação da visualização de envelope canônico (`artifact`) com `artifactType`, `artifactVersion`, `status`, `parentArtifactIds` e `content`
  - implementação de navegação de lineage básico por pais/filhos com botões de salto entre artefatos
  - ampliação dos contratos backend↔frontend para retorno de conteúdo do artefato no endpoint administrativo de artifacts
  - inclusão de testes de contrato/frontend para artefatos e lineage
- **Endpoints criados/alterados:**
  - `GET /api/mds/requests/{id}/artifacts` (payload enriquecido com `content` + `parentArtifactIds` + `childArtifactIds`)
- **Telas criadas/alteradas:**
  - `frontend/src/pages/mds/MdsArtifactsPage.tsx`
- **Testes executados:**
  - `cd frontend && npm run test -- --run src/api/mds/useMdsAdmin.test.tsx src/pages/mds/MdsArtifactsPage.test.tsx src/pages/mds/MdsWorkspacePage.test.tsx src/pages/mds/MdsRequestDetailPage.test.tsx`
  - `cd frontend && npm run build`
  - `cd backend/ads-service && mvn -Dtest=MdsAdminControllerContractTest test`
- **Riscos pendentes:**
  - revisão de performance para requests com alto volume de artefatos e lineage denso
- **Decisões tomadas:**
  - manter endpoint único de artifacts para reduzir round-trips na auditoria da Sprint 2
  - expor `parentArtifactIds` e `childArtifactIds` já resolvidos no backend para simplificar navegação no frontend

### Sprint 3 — Registro

- **Status:** `CONCLUÍDO`
- **Período:** `2026-04-27 até 2026-04-27`
- **Responsáveis:** `Codex (implementação assistida)`
- **Escopo realizado:**
  - evolução da Tela de relatório com leitura executiva (mecanismo, evidências, confiança, limitações, justificativa)
  - refinamento da UX operacional na lista com mensagens explícitas de retry elegível/não elegível e feedback de sucesso/erro da ação
  - ajuste do contrato backend para expor `retryEligible` e `retryReason` em listagem e detalhe
  - inclusão de teste E2E do fluxo principal (lista → detalhe → artefatos → relatório)
- **Endpoints criados/alterados:**
  - `GET /api/mds/requests` (campos novos: `retryEligible`, `retryReason`)
  - `GET /api/mds/requests/{id}` (campos novos: `retryEligible`, `retryReason`)
  - `POST /api/mds/requests/{id}/retry` (elegibilidade explícita por status)
- **Telas criadas/alteradas:**
  - `frontend/src/pages/mds/MdsWorkspacePage.tsx`
  - `frontend/src/pages/mds/MdsReportPage.tsx`
- **Testes executados:**
  - `cd frontend && npm run test -- --run src/api/mds/useMdsAdmin.test.tsx src/pages/mds/MdsWorkspacePage.test.tsx src/pages/mds/MdsRequestDetailPage.test.tsx src/pages/mds/MdsArtifactsPage.test.tsx src/pages/mds/MdsReportPage.test.tsx src/pages/mds/MdsMainFlow.e2e.test.tsx`
  - `cd frontend && npm run build`
  - `cd backend/ads-service && mvn -Dtest=MdsAdminControllerContractTest test`
- **Riscos pendentes:**
  - fluxo de retry para requests `COMPLETED` permanece bloqueado por decisão operacional atual e pode exigir trilha dedicada
- **Decisões tomadas:**
  - retry operacional da Sprint 3 ficou restrito a status `FAILED` para evitar replay indevido de requests concluídas
  - feedback operacional do frontend foi padronizado em alerts persistentes com fechamento manual

### Sprint 4 — Registro

- **Status:** `CONCLUÍDO`
- **Período:** `2026-04-27 até 2026-04-27`
- **Responsáveis:** `Codex (implementação assistida)`
- **Escopo realizado:**
  - implementação de observabilidade da UI com painel de saúde do módulo (`/api/mds/health`) e contadores por status
  - melhoria de performance com polling controlável (auto-refresh on/off), `staleTime` e cache com `keepPreviousData`
  - revisão de acessibilidade/consistência visual (seções com rótulos, `aria-live`, feedbacks operacionais com fechamento manual)
  - validação final de aderência canônica na visualização de artifacts/report e fluxo principal MDS
- **Endpoints criados/alterados:**
  - `GET /api/mds/health` (consumo ativo na UI)
  - ajustes de contrato de requests para governar ação operacional de retry (`retryEligible`, `retryReason`)
- **Telas criadas/alteradas:**
  - `frontend/src/pages/mds/MdsWorkspacePage.tsx`
  - `frontend/src/pages/mds/MdsReportPage.tsx`
  - `frontend/src/pages/mds/MdsArtifactsPage.tsx`
- **Testes executados:**
  - `cd frontend && npm run test -- --run src/api/mds/useMdsAdmin.test.tsx src/pages/mds/MdsWorkspacePage.test.tsx src/pages/mds/MdsRequestDetailPage.test.tsx src/pages/mds/MdsArtifactsPage.test.tsx src/pages/mds/MdsReportPage.test.tsx src/pages/mds/MdsMainFlow.e2e.test.tsx`
  - `cd frontend && npm run build`
  - `cd backend/ads-service && mvn -Dtest=MdsAdminControllerContractTest test`
- **Riscos pendentes:**
  - em cenários de alto volume pode ser necessário paginação adicional no painel administrativo e otimização de bundle frontend
- **Decisões tomadas:**
  - auto-refresh da fila foi mantido habilitado por padrão com opção explícita de desligamento
  - observabilidade mínima na UI passou a ser obrigatória para operação contínua da fila MDS

---

## 7. Critérios de pronto do documento

- plano de telas descrito com escopo MVP
- separação de responsabilidades explicitada (MDS = regra de negócio; backend = persistência)
- backlog por sprint definido
- bloco de registro por sprint disponível para preenchimento contínuo
