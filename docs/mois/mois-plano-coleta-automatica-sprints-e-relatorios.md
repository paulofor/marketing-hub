# MOIS — Plano de implementação da coleta automática + documento único de relatórios de conclusão

## Contexto

Objetivo: evoluir o MOIS para coletar referências de sucesso de mercado com janela temporal (semana/mês), consolidar sinais de sucesso e alimentar o fluxo de **Coleta → Extração → Síntese → Aplicação → Teste** com dados mais acionáveis.

Este plano mantém aderência ao eixo central do Marketing Hub: **Dor → Resultado → Mecanismo → Prova → Oferta**.

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
- **Status:** Planejado
- **Período:** a definir
- **Escopo concluído:** pendente
- **Evidências (PRs, commits, testes):** pendente
- **Riscos/pendências:** pendente
- **Próximos passos:** implementar normalização e score na Sprint 2

## Relatório — Sprint 2
- **Status:** Planejado
- **Período:** a definir
- **Escopo concluído:** pendente
- **Evidências (PRs, commits, testes):** pendente
- **Riscos/pendências:** pendente
- **Próximos passos:** entregar interface de coleta automática na Sprint 3

## Relatório — Sprint 3
- **Status:** Planejado
- **Período:** a definir
- **Escopo concluído:** pendente
- **Evidências (PRs, commits, testes):** pendente
- **Riscos/pendências:** pendente
- **Próximos passos:** integrar com extração/biblioteca na Sprint 4

## Relatório — Sprint 4
- **Status:** Planejado
- **Período:** a definir
- **Escopo concluído:** pendente
- **Evidências (PRs, commits, testes):** pendente
- **Riscos/pendências:** pendente
- **Próximos passos:** fechar operação e rollout na Sprint 5

## Relatório — Sprint 5
- **Status:** Planejado
- **Período:** a definir
- **Escopo concluído:** pendente
- **Evidências (PRs, commits, testes):** pendente
- **Riscos/pendências:** pendente
- **Próximos passos:** operação contínua + melhorias incrementais
