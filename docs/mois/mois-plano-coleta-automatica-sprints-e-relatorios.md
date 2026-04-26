# MOIS — Plano de implementação da coleta automática + documento único de relatórios de conclusão

## Contexto

Objetivo: evoluir o MOIS para coletar referências de sucesso de mercado com janela temporal (semana/mês), consolidar sinais de sucesso e alimentar o fluxo de **Coleta → Extração → Síntese → Aplicação → Teste** com dados mais acionáveis.

Este plano mantém aderência ao eixo central do Marketing Hub: **Dor → Resultado → Mecanismo → Prova → Oferta**.

## Diretriz arquitetural crítica (obrigatória)

> **Regra inegociável:** toda regra de negócio do MOIS deve residir no módulo **`/mois`**.
> O backend principal deve atuar somente como gateway/contrato e camada de leitura/escrita de dados.

---

## Escopo funcional (visão macro)

1. Seleção de fontes de pesquisa (internacionais e locais).
2. Coleta assistida/automática por janela temporal:
   - últimos 7 dias (semana);
   - últimos 30 dias (mês).
3. Definição de critérios objetivos de “sinais de sucesso”.
4. Persistência e rastreabilidade (lineage) por referência.
5. Consolidação dos resultados no workspace MOIS.
6. Relatórios de conclusão por sprint neste mesmo documento.

---

## SPRINT 0 — Descoberta técnica e contratos (1 semana)

### Objetivo
Definir contratos, critérios e limites técnicos antes da implementação.

### Entregas
- catálogo inicial de fontes suportadas (com tipo de acesso/API/política);
- contrato de coleta (request/response) com filtros de tempo (7/30 dias);
- contrato de “sinal de sucesso” (campos mínimos obrigatórios);
- matriz de risco legal/compliance por fonte (termos de uso/rate limits).

### Critérios de pronto
- contratos versionados e revisados;
- definição explícita de fallback quando a fonte não fornecer métrica direta.

---

## SPRINT 1 — Backend base da coleta automática (1 semana)

### Objetivo
Disponibilizar pipeline backend para iniciar coletas por fonte/período.

### Entregas
- endpoint para criar job de coleta automática;
- endpoint para listar jobs e status (queued/running/completed/failed);
- endpoint para listar referências coletadas por job;
- persistência mínima de job, referência bruta e metadados de origem;
- logs estruturados por execução.

### Critérios de pronto
- criação e acompanhamento de jobs funcionando em ambiente local;
- validações de payload e mensagens de erro claras.

---

## SPRINT 2 — Normalização de sucesso e ranking (1 semana)

### Objetivo
Transformar dados heterogêneos em score comparável entre fontes.

### Entregas
- módulo de normalização de métricas (ex.: engajamento relativo, recorrência, evidência);
- score composto de “sinal de sucesso” (0–100);
- ordenação e filtros por período, fonte, nicho e score mínimo;
- marcação de confiança da evidência (baixo/médio/alto).

### Critérios de pronto
- score reproduzível e documentado;
- testes cobrindo cenários de dados ausentes/incompletos.
- backend façade e módulo MOIS aceitando filtros de consulta por fonte, nicho, score mínimo e confiança.

---

## SPRINT 3 — Frontend MOIS (coleta automática) (1 semana)

### Objetivo
Expor a funcionalidade na UI do MOIS com fluxo simples e objetivo.

### Entregas
- página de “Coleta automática” com:
  - seleção de fontes;
  - janela temporal (7/30 dias);
  - nicho/tema;
  - botão de iniciar coleta;
- tabela de resultados com status, score, origem e data;
- ações: importar para Coleta MOIS, descartar, favoritar.

### Critérios de pronto
- UX sem ruído, com estados loading/empty/error;
- botões assíncronos desabilitados com indicador de carregamento;
- links externos abrindo em nova aba.

---

## SPRINT 4 — Integração com Extração e Biblioteca (1 semana)

### Objetivo
Fechar o ciclo operacional: da coleta automática para uso real no MOIS.

### Entregas
- importação de resultados automáticos para referências do workspace;
- ponte para Extração guiada com pré-preenchimento dos campos base;
- geração inicial de blocos de biblioteca a partir das melhores referências;
- rastreabilidade (fonte original → referência MOIS → extração/bloco).

### Critérios de pronto
- clique único para “importar e iniciar extração”;
- lineage visível para auditoria.

---

## SPRINT 5 — Operação, qualidade e rollout (1 semana)

### Objetivo
Garantir estabilidade, observabilidade e adoção segura em produção.

### Entregas
- monitoração de jobs (latência, falhas por fonte, taxa de sucesso);
- retries com backoff e controle de rate limit;
- flags de rollout gradual por workspace;
- documentação de operação e playbook de incidentes.

### Critérios de pronto
- métricas mínimas em produção;
- fallback definido para indisponibilidade de fonte.

---

## Backlog transversal (todas as sprints)

- segurança e secrets (sem credenciais em código);
- conformidade de contratos frontend/backend;
- cobertura de testes unitários e de contrato;
- revisão de aderência aos documentos canônicos;
- revisão de performance e custo operacional.

---

## Cronograma sugerido

- Sprint 0: Semana 1
- Sprint 1: Semana 2
- Sprint 2: Semana 3
- Sprint 3: Semana 4
- Sprint 4: Semana 5
- Sprint 5: Semana 6

---

# Relatórios de conclusão (documento único)

> Regra: ao finalizar cada sprint, registrar o relatório nesta seção (sem criar documento separado).

## Relatório — Sprint 0
- **Status:** Concluído
- **Período:** 25/04/2026
- **Escopo concluído:**
  - catálogo inicial de fontes suportadas com tipo de acesso e risco;
  - contrato de coleta automática com filtros temporais (`LAST_7_DAYS` e `LAST_30_DAYS`);
  - contrato mínimo de sinal de sucesso (`score`, `confidenceLevel`, `evidenceCount`, `primaryReason`);
  - matriz de risco legal/compliance por fonte;
  - fallback explícito para ausência de métrica e falha parcial de coleta.
- **Evidências (PRs, commits, testes):**
  - documento: `docs/mois/mois-sprint-0-descoberta-contratos.md`;
  - commit: `docs(mois): executa sprint 0 com contratos, fontes e matriz de risco`.
- **Riscos/pendências:**
  - validar juridicamente as fontes antes da automação em produção;
  - confirmar limites operacionais por fonte durante a Sprint 1.
- **Próximos passos:** iniciar Sprint 1 com endpoints de criação/listagem de jobs e persistência mínima.

## Relatório — Sprint 1
- **Status:** Concluído
- **Período:** 25/04/2026
- **Escopo concluído:**
  - endpoint para criar job de coleta automática (`POST /api/v1/mois/collection-jobs`);
  - endpoint para listar jobs (`GET /api/v1/mois/collection-jobs`);
  - endpoint para listar referências coletadas por job (`GET /api/v1/mois/collection-jobs/{jobId}/references`);
  - regra de negócio de jobs movida para o módulo MOIS (backend principal atua como gateway/contrato);
  - persistência mínima em memória no módulo MOIS para jobs e referências coletadas;
  - logs estruturados no módulo MOIS ao criar job de coleta.
- **Evidências (PRs, commits, testes):**
  - backend: `MoisController`, `MoisModuleGateway`, `MoisWorkspaceDtos`;
  - módulo MOIS: `MoisDomainController`, `MoisDomainService`, `MoisWorkspaceDtos`;
  - testes: `MoisControllerContractTest`, `MoisDomainControllerTest` e `MoisDomainServiceTest`.
- **Riscos/pendências:**
  - persistência ainda em memória (não relacional), apropriada apenas para fase inicial;
  - referências coletadas ainda são seedadas para contrato/smoke, sem conectores reais de fonte;
  - alinhar próximos endpoints legados de workspace para o mesmo padrão de delegação ao módulo MOIS.
- **Próximos passos:** iniciar Sprint 2 para normalização de sinais de sucesso e ranking comparável.

## Relatório — Sprint 2
- **Status:** Concluído
- **Período:** 26/04/2026
- **Escopo concluído:**
  - normalização de sinal de sucesso com score composto (0–100) no módulo `mois`, usando pesos documentados:
    - `0.45 * engagementRelative`
    - `0.35 * recurrenceScore`
    - `0.20 * evidenceScore`
  - fallback explícito para dados incompletos: quando `evidenceRaw` estiver ausente, o cálculo usa média de `engagementRelative` e `recurrenceScore`;
  - enriquecimento do contrato de referência coletada com:
    - `confidenceLevel` (`LOW|MEDIUM|HIGH`),
    - `rankingPosition`,
    - `engagementRelative`,
    - `recurrenceScore`,
    - `evidenceScore`;
  - ordenação por score + re-ranking determinístico para garantir comparabilidade entre fontes;
  - filtros no endpoint de listagem de referências por job:
    - `source`,
    - `niche`,
    - `minSuccessScore`,
    - `confidenceLevel`;
  - propagação completa dos filtros no backend façade (`/api/v1/mois/*`) até o módulo `mois`.
  - migração das regras de negócio de workspace (dashboard, referências, extração, biblioteca, comparação e build de oferta) para o módulo `mois`, removendo implementação de domínio duplicada no backend.
- **Evidências (PRs, commits, testes):**
  - backend:
    - `backend/ads-service/src/main/java/com/marketinghub/mois/web/MoisController.java`
    - `backend/ads-service/src/main/java/com/marketinghub/mois/service/MoisModuleGateway.java`
    - `backend/ads-service/src/main/java/com/marketinghub/mois/dto/MoisWorkspaceDtos.java`
    - `backend/ads-service/src/test/java/com/marketinghub/mois/web/MoisControllerContractTest.java`
  - módulo MOIS:
    - `mois/src/main/java/com/marketinghub/mois/service/MoisDomainService.java`
    - `mois/src/main/java/com/marketinghub/mois/web/MoisDomainController.java`
    - `mois/src/main/java/com/marketinghub/mois/dto/MoisWorkspaceDtos.java`
    - `mois/src/test/java/com/marketinghub/mois/service/MoisDomainServiceTest.java`
    - `mois/src/test/java/com/marketinghub/mois/web/MoisDomainControllerTest.java`
- **Riscos/pendências:**
  - persistência de coleta ainda em memória no módulo `mois` (não relacional);
  - conectores reais por fonte ainda não implementados (dados seedados para contrato/smoke);
  - calibragem de pesos de score deve ser revisada com dados reais de produção/homologação.
- **Próximos passos:** executar Sprint 3 com UI de coleta automática orientada por filtros e ranking já normalizado.

## Relatório — Sprint 3
- **Status:** Concluído
- **Período:** 26/04/2026
- **Escopo concluído:**
  - nova página de **Coleta automática** no frontend MOIS com:
    - seleção de fontes;
    - janela temporal (`LAST_7_DAYS` e `LAST_30_DAYS`);
    - nicho e tema;
    - botão de iniciar coleta;
  - tabela de resultados com colunas de status, score, origem e data;
  - ações por referência coletada:
    - importar para referências MOIS,
    - descartar,
    - favoritar;
  - filtros de leitura na UI (fonte, confiança e score mínimo), com estados `loading`, `empty` e `error`;
  - links externos de origem abrindo em nova aba (`target="_blank"` + `rel="noreferrer"`).
- **Evidências (PRs, commits, testes):**
  - frontend:
    - `frontend/src/pages/mois/MoisAutoCollectionPage.tsx`
    - `frontend/src/api/mois/useMoisCollection.ts`
    - `frontend/src/api/mois/types.ts`
    - `frontend/src/pages/mois/MoisWorkspacePage.tsx`
    - `frontend/src/App.tsx`
  - backend/mois (apoio contratual para ações):
    - `backend/ads-service/src/main/java/com/marketinghub/mois/web/MoisController.java`
    - `backend/ads-service/src/main/java/com/marketinghub/mois/service/MoisModuleGateway.java`
    - `mois/src/main/java/com/marketinghub/mois/web/MoisDomainController.java`
    - `mois/src/main/java/com/marketinghub/mois/service/MoisDomainService.java`
- **Riscos/pendências:**
  - persistência das ações de coleta ainda em memória no módulo `mois`;
  - necessidade de consolidar importação com lineage completo na Sprint 4.
- **Próximos passos:** integrar automaticamente com Extração e Biblioteca na Sprint 4.

## Relatório — Sprint 4
- **Status:** Concluído
- **Período:** 26/04/2026
- **Escopo concluído:**
  - importação de referência coletada para o workspace MOIS com atualização de status;
  - ação de **clique único** `Importar + Extração`, iniciando extração draft automaticamente;
  - geração inicial de blocos de biblioteca a partir da referência importada;
  - endpoint de lineage por referência coletada (`source -> importedReference -> extraction -> blocks`);
  - visualização de lineage na UI de coleta automática.
- **Evidências (PRs, commits, testes):**
  - frontend:
    - `frontend/src/pages/mois/MoisAutoCollectionPage.tsx`
    - `frontend/src/api/mois/useMoisCollection.ts`
    - `frontend/src/api/mois/types.ts`
  - backend/mois:
    - `backend/ads-service/src/main/java/com/marketinghub/mois/web/MoisController.java`
    - `backend/ads-service/src/main/java/com/marketinghub/mois/service/MoisModuleGateway.java`
    - `backend/ads-service/src/main/java/com/marketinghub/mois/dto/MoisWorkspaceDtos.java`
    - `mois/src/main/java/com/marketinghub/mois/web/MoisDomainController.java`
    - `mois/src/main/java/com/marketinghub/mois/service/MoisDomainService.java`
- **Riscos/pendências:**
  - lineage ainda em persistência volátil (memória) no módulo `mois`;
  - necessário evoluir para persistência relacional e auditoria completa na Sprint 5.
- **Próximos passos:** estabilizar operação, observabilidade e rollout gradual na Sprint 5.

## Relatório — Sprint 5
- **Status:** Planejado
- **Período:** a definir
- **Escopo concluído:** pendente
- **Evidências (PRs, commits, testes):** pendente
- **Riscos/pendências:** pendente
- **Próximos passos:** operação contínua + melhorias incrementais
