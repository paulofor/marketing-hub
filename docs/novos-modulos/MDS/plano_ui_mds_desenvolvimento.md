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

- **Status:** `PLANEJADO`
- **Período:** `<AAAA-MM-DD até AAAA-MM-DD>`
- **Responsáveis:** `<nomes/time>`
- **Escopo realizado:**
  - `<item>`
- **Endpoints criados/alterados:**
  - `<endpoint>`
- **Telas criadas/alteradas:**
  - `<arquivo frontend>`
- **Testes executados:**
  - `<comando e resultado>`
- **Riscos pendentes:**
  - `<item>`
- **Decisões tomadas:**
  - `<item>`

### Sprint 1 — Registro

- **Status:** `PLANEJADO`
- **Período:** `<AAAA-MM-DD até AAAA-MM-DD>`
- **Responsáveis:** `<nomes/time>`
- **Escopo realizado:**
  - `<item>`
- **Endpoints criados/alterados:**
  - `<endpoint>`
- **Telas criadas/alteradas:**
  - `<arquivo frontend>`
- **Testes executados:**
  - `<comando e resultado>`
- **Riscos pendentes:**
  - `<item>`
- **Decisões tomadas:**
  - `<item>`

### Sprint 2 — Registro

- **Status:** `PLANEJADO`
- **Período:** `<AAAA-MM-DD até AAAA-MM-DD>`
- **Responsáveis:** `<nomes/time>`
- **Escopo realizado:**
  - `<item>`
- **Endpoints criados/alterados:**
  - `<endpoint>`
- **Telas criadas/alteradas:**
  - `<arquivo frontend>`
- **Testes executados:**
  - `<comando e resultado>`
- **Riscos pendentes:**
  - `<item>`
- **Decisões tomadas:**
  - `<item>`

### Sprint 3 — Registro

- **Status:** `PLANEJADO`
- **Período:** `<AAAA-MM-DD até AAAA-MM-DD>`
- **Responsáveis:** `<nomes/time>`
- **Escopo realizado:**
  - `<item>`
- **Endpoints criados/alterados:**
  - `<endpoint>`
- **Telas criadas/alteradas:**
  - `<arquivo frontend>`
- **Testes executados:**
  - `<comando e resultado>`
- **Riscos pendentes:**
  - `<item>`
- **Decisões tomadas:**
  - `<item>`

### Sprint 4 — Registro

- **Status:** `PLANEJADO`
- **Período:** `<AAAA-MM-DD até AAAA-MM-DD>`
- **Responsáveis:** `<nomes/time>`
- **Escopo realizado:**
  - `<item>`
- **Endpoints criados/alterados:**
  - `<endpoint>`
- **Telas criadas/alteradas:**
  - `<arquivo frontend>`
- **Testes executados:**
  - `<comando e resultado>`
- **Riscos pendentes:**
  - `<item>`
- **Decisões tomadas:**
  - `<item>`

---

## 7. Critérios de pronto do documento

- plano de telas descrito com escopo MVP
- separação de responsabilidades explicitada (MDS = regra de negócio; backend = persistência)
- backlog por sprint definido
- bloco de registro por sprint disponível para preenchimento contínuo
