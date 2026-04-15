# OPRM — Plano Único de Desenvolvimento e Integração

## 1. Objetivo do documento

Este documento consolida, em um único lugar, a direção de desenvolvimento do **Occupation Persona Routine Mapper (OPRM)**, com foco especial em fechar a integração do módulo com o backend principal do Marketing Hub.

Ele deve servir como referência única de implementação para:

- arquitetura do módulo
- papel do backend
- contrato de integração
- roadmap por sprint
- artefatos principais
- critérios de pronto
- governança
- riscos e mitigação

Este documento não substitui:
- `docs/canonical/system-governance-canon.v2.md`
- o cânone de artefatos do OPRM
- o histórico de implantação do OPRM
- ADRs necessários
- contratos OpenAPI publicados

Ele consolida a direção operacional para desenvolver a integração do módulo de forma consistente.

---

## 2. Referências obrigatórias

Este plano deve ser interpretado em conjunto com:

- `docs/canonical/system-governance-canon.v2.md`
- `docs/history/oprm-implementation-history.md`
- `docs/canonical/oprm_canonico_artefatos.md`
- `AGENTS.md` da raiz
- `AGENTS.md` local do módulo, se existir

---

## 3. Contexto atual

O OPRM já evoluiu internamente em fases relevantes:

- fase 1: resolução ocupacional e intake estruturado
- fase 2: enriquecimento web
- fase 3: inferência de rotina
- fase 4: integração com o framework dor → resultado → oferta → mecanismo → prova
- fase 5: feedback loop
- preparação parcial de containerização e publicação operacional

Essas fases já produziram:
- estrutura Java 21 + Spring Boot
- artefatos canônicos do domínio
- endpoints internos do módulo
- testes locais do módulo
- containers e arquivos de deploy iniciais

O principal problema atual não está mais no core funcional do OPRM.
O principal problema está na **integração fraca com o backend principal do Marketing Hub**.

---

## 4. Problema principal a resolver

A integração entre OPRM e backend principal ainda está incompleta.

Os principais gaps atuais são:

1. não existe contrato HTTP versionado backend ↔ OPRM
2. o fluxo de jobs ainda não está consolidado no backend principal
3. os artefatos do OPRM ainda não têm persistência end-to-end consistente no backend
4. o feedback loop ainda não está persistido no fluxo principal
5. o histórico por ocupação ainda depende de estado local do processo em partes do fluxo
6. o módulo ainda não opera como worker totalmente integrado ao ecossistema do Marketing Hub
7. a observabilidade operacional ainda não está fechada

Consequência:
- o OPRM funciona localmente e por fases
- mas ainda não está plenamente encaixado como bounded context operacional do sistema

---

## 5. Princípios de arquitetura

### 5.1 O OPRM continua sendo dono do seu domínio
O domínio do OPRM é definido por:
- seus documentos canônicos
- seus artefatos
- seus contratos explícitos
- sua integração com o framework dor → resultado → oferta → mecanismo → prova

### 5.2 O backend não é a verdade conceitual do OPRM
O backend:
- entrega jobs, seeds e contexto
- recebe artefatos e resultados
- persiste os dados
- orquestra o fluxo operacional

Mas o backend **não redefine o significado dos artefatos do OPRM**.

### 5.3 Só o backend acessa o banco
O OPRM:
- não deve acessar o banco diretamente
- não deve criar um segundo sistema de registro
- deve publicar seus resultados via contrato explícito com o backend

### 5.4 Integração por contrato
Toda integração OPRM ↔ backend deve ser:
- explícita
- versionada
- testada
- documentada

### 5.5 Worker orientado a loop/agendamento
O OPRM deve rodar como worker em Java Spring Boot com:
- loop controlado/agendado
- claim de jobs
- processamento por fase
- publicação de artefatos
- update de status
- heartbeat

---

## 6. Arquitetura-alvo da integração

### 6.1 Fluxo-alvo

1. backend cria ou disponibiliza job do OPRM
2. OPRM faz claim do job
3. OPRM busca contexto e seeds necessários
4. OPRM executa as fases internas
5. OPRM publica artefatos no backend
6. backend persiste artefatos, lineage e status
7. OPRM publica status final e métricas do ciclo
8. backend disponibiliza os artefatos para módulos downstream

### 6.2 Resultado desejado
Ao final da integração:
- o OPRM deixa de ser apenas um módulo local por fases
- o OPRM passa a ser um worker real do Marketing Hub
- o backend vira o canal operacional estável
- os artefatos passam a existir end-to-end no sistema

---

## 7. Contratos mínimos backend ↔ OPRM

O desenvolvimento deve assumir estes contratos mínimos.

### 7.1 Claim de job
Responsabilidade:
- OPRM busca trabalho disponível

Endpoint sugerido:
- `POST /api/oprm/jobs/claim`

Resposta mínima:
- `jobId`
- `jobType`
- `occupationSeedRef`
- `correlationId`
- `parameters`

### 7.2 Consulta de job
Responsabilidade:
- OPRM carrega detalhes do trabalho e referências adicionais

Endpoint sugerido:
- `GET /api/oprm/jobs/{jobId}`

### 7.3 Publicação de artefatos
Responsabilidade:
- OPRM envia artefatos produzidos

Endpoint sugerido:
- `POST /api/oprm/artifacts`

Payload:
- envelope canônico do artefato
- lineage
- confidence
- payload do artefato

### 7.4 Atualização de status do job
Responsabilidade:
- OPRM informa progresso, falha ou conclusão

Endpoint sugerido:
- `POST /api/oprm/jobs/{jobId}/status`

### 7.5 Publicação de feedback loop
Responsabilidade:
- OPRM envia snapshots de recalibração

Endpoint sugerido:
- `POST /api/oprm/feedback`

### 7.6 Heartbeat
Responsabilidade:
- OPRM informa saúde operacional e atividade do worker

Endpoint sugerido:
- `POST /api/oprm/heartbeat`

---

## 8. Modelo de dados operacional no backend

O backend deve receber um modelo operacional mínimo para sustentar a integração.

### 8.1 Tabelas ou entidades sugeridas
- `oprm_job`
- `oprm_job_input`
- `oprm_job_event`
- `oprm_job_artifact_ref`
- `oprm_feedback_snapshot`
- `oprm_feedback_history`

### 8.2 Campos mínimos de job
- `job_id`
- `job_type`
- `job_status`
- `occupation_seed_ref`
- `attempt_count`
- `claimed_by`
- `claimed_at`
- `started_at`
- `finished_at`
- `error_code`
- `error_message`
- `correlation_id`

### 8.3 Estados mínimos do job
- `PENDING`
- `CLAIMED`
- `RUNNING`
- `SUCCEEDED`
- `FAILED`
- `RETRY_WAIT`
- `CANCELLED`

---

## 9. Estrutura técnica do OPRM

O OPRM continua como módulo interno do repo, com diretório próprio e containers próprios.

### 9.1 Componentes principais
- `oprm-worker`
- `oprm-api` (opcional no MVP)
- `oprm-scheduler` (opcional, se não ficar embutido no worker)

### 9.2 Clients internos recomendados
- `BackendJobClient`
- `BackendArtifactPublishClient`
- `BackendFeedbackClient`
- `BackendHeartbeatClient`

### 9.3 Regras
- o loop de claim não deve virar execução concorrente descontrolada
- deve existir timeout de claim
- deve existir controle de retry
- deve existir correlação por `correlation_id`
- publicação de artefato deve ser idempotente quando possível

---

## 10. Estratégia de implementação por sprints

Este plano assume **5 sprints sequenciais**.

---

## Sprint 1 — Contrato oficial backend ↔ OPRM

### Objetivo
Definir a integração oficial e eliminar ambiguidade de comunicação entre backend e OPRM.

### Entregas
- documento OpenAPI da integração
- DTOs comuns de job, artifact, status e feedback
- documentação curta de versionamento do contrato
- atualização do histórico de implantação
- ADR, se necessário

### Itens detalhados
- definir payload de claim
- definir payload de job detail
- definir payload de publish artifact
- definir payload de update status
- definir payload de feedback
- definir políticas de erro HTTP

### Critério de pronto
- contrato publicado
- backend e OPRM alinhados ao mesmo contrato
- nenhum endpoint novo sem documentação

### Riscos
- implementar antes de fechar o contrato
- deixar o contrato “implícito no código”

---

## Sprint 2 — Job orchestration no backend + consumo real no OPRM

### Objetivo
Transformar o OPRM em consumidor real de jobs do backend.

### Entregas
- modelo de job no backend
- endpoints de claim, detail e status
- clients HTTP no OPRM
- loop/agendamento do worker consumindo jobs reais

### Itens detalhados
- criar `oprm_job`
- criar `oprm_job_event`
- criar `oprm_job_input`
- implementar `BackendJobClient`
- implementar `BackendStatusClient`
- configurar scheduler do worker
- tratar lock lógico por job

### Critério de pronto
- backend cria job
- OPRM faz claim
- OPRM executa
- OPRM atualiza status
- sem acesso direto ao banco pelo OPRM

### Riscos
- duplicidade de claim
- scheduler sobrepondo execuções
- job preso sem timeout

---

## Sprint 3 — Publicação remota de artefatos + persistência end-to-end

### Objetivo
Fazer os artefatos do OPRM existirem de verdade no backend principal.

### Entregas
- endpoint de publish artifact
- persistência dos envelopes dos artefatos
- lineage mínimo persistido
- vínculo entre job e artefatos gerados

### Itens detalhados
- persistir `occupationProfileSnapshot`
- persistir `occupationWebSourceSnapshot`
- persistir `occupationPersonaRoutineCard`
- persistir `desiredOutcomeSignal`
- persistir `mechanismOpportunitySignal`
- persistir `dorResultadoOfertaMecanismoProvaInput`

### Critério de pronto
- um job do OPRM produz artefatos reais no backend
- artefatos podem ser consultados por correlation id / occupation / status
- lineage mínimo está preservado

### Riscos
- persistir artefato sem envelope completo
- perder lineage
- backend começar a reinterpretar o payload do OPRM sem contrato

---

## Sprint 4 — Feedback loop persistido e ingestão de métricas downstream

### Objetivo
Fechar o ciclo de aprendizado do OPRM com dados reais do Marketing Hub.

### Entregas
- persistência do feedback loop no backend
- histórico por ocupação persistido
- ingestão de snapshots de performance de hipótese
- recalibração persistida de scores principais

### Itens detalhados
- persistir `occupationFeedbackLoopSnapshot`
- persistir histórico por ocupação
- receber `HypothesisPerformanceSnapshot`
- recalibrar confidence
- recalibrar pain intensity
- recalibrar mechanism fit
- remover dependência de histórico em memória local

### Critério de pronto
- histórico sobrevive a restart do OPRM
- feedback entra e sai pelo backend
- reprocessamentos usam estado persistido

### Riscos
- feedback ficar acoplado a estrutura local do worker
- perder auditabilidade do aprendizado por ocupação

---

## Sprint 5 — Contract testing, observabilidade e hardening operacional

### Objetivo
Tornar a integração robusta o suficiente para operação contínua.

### Entregas
- testes de contrato backend ↔ OPRM
- health, readiness e métricas
- tracing por correlation id
- validação real de docker compose / deploy
- política de imagem com tag imutável
- ajustes de restart policy e ambiente

### Itens detalhados
- Spring Cloud Contract para contratos principais
- healthchecks do OPRM
- métricas de job e publicação
- tracing básico
- validação real do deploy do container
- abandonar dependência exclusiva de `latest`

### Métricas mínimas
- `oprm.jobs.claimed`
- `oprm.jobs.succeeded`
- `oprm.jobs.failed`
- `oprm.artifacts.published`
- `oprm.backend.publish.failures`
- `oprm.loop.duration`
- `oprm.phase.duration`

### Critério de pronto
- quebrar contrato falha no CI
- módulo expõe health e métricas
- deploy validado no fluxo real
- operação não depende de leitura manual de logs para saber se está saudável

### Riscos
- observabilidade superficial
- deploy parcialmente automatizado, mas ainda frágil

---

## 11. Priorização

Se houver limitação de tempo, a ordem de prioridade é:

1. Sprint 1 completa
2. Sprint 2 completa
3. Sprint 3 completa
4. persistência do feedback da Sprint 4
5. contract testing da Sprint 5
6. observabilidade e hardening do deploy da Sprint 5

### Itens que NÃO devem ser prioridade agora
- ampliar número de ocupações
- sofisticar IA/LLM da inferência
- melhorar heurística textual fina
- criar UI nova
- adicionar novas fases funcionais ao OPRM

---

## 12. Critério de pronto da integração forte

A integração só deve ser considerada forte quando todos os itens abaixo forem verdadeiros:

- OPRM recebe job real do backend
- OPRM publica artefatos reais no backend
- backend persiste lineage e status
- feedback loop sobrevive a restart
- contrato está versionado
- contrato está testado no CI
- health e métricas estão ativos
- deploy está validado e repetível

---

## 13. Artefatos afetados por esta integração

A fase de integração deve tratar explicitamente ao menos estes artefatos:

- `occupationSeed`
- `occupationAliasResolution`
- `occupationProfileSnapshot`
- `occupationWebSourceSnapshot`
- `routineTaskPattern`
- `routineConstraintSignal`
- `routinePainSignal`
- `routineWorkaroundSignal`
- `occupationPersonaRoutineCard`
- `desiredOutcomeSignal`
- `mechanismOpportunitySignal`
- `dorResultadoOfertaMecanismoProvaInput`
- `occupationFeedbackLoopSnapshot`

---

## 14. Checklist operacional por sprint

### Sprint 1
- [ ] contrato OpenAPI criado
- [ ] DTOs alinhados
- [ ] versionamento definido
- [ ] histórico atualizado

### Sprint 2
- [ ] modelo de job criado no backend
- [ ] endpoints de claim/detail/status implementados
- [ ] clients HTTP criados no OPRM
- [ ] loop real do worker funcionando

### Sprint 3
- [ ] endpoint de publish artifact implementado
- [ ] envelopes persistidos
- [ ] lineage persistido
- [ ] vínculo job → artifact funcionando

### Sprint 4
- [ ] feedback loop persistido
- [ ] histórico por ocupação persistido
- [ ] ingestão de snapshot downstream funcionando
- [ ] recalibração usando estado persistido

### Sprint 5
- [ ] contract tests criados
- [ ] healthchecks ativos
- [ ] métricas expostas
- [ ] tracing básico ativo
- [ ] deploy validado com compose
- [ ] tags imutáveis adotadas

---

## 15. Riscos e mitigação

### 15.1 Risco: drift entre backend e OPRM
Mitigação:
- contrato OpenAPI
- Spring Cloud Contract
- versionamento explícito

### 15.2 Risco: OPRM virar sistema paralelo de persistência
Mitigação:
- backend único responsável pela persistência principal
- OPRM apenas com cache técnico transitório, se necessário

### 15.3 Risco: lineage incompleto
Mitigação:
- exigir `source_refs` e `input_refs`
- não publicar artefato sintético sem lineage mínimo

### 15.4 Risco: scheduler duplicar execução
Mitigação:
- claim com lock lógico
- timeout de claim
- controle de concorrência

### 15.5 Risco: feedback loop perder estado
Mitigação:
- persistência no backend
- remoção de dependência de memória local

---

## 16. Regras de governança

1. Este plano deve respeitar `docs/canonical/system-governance-canon.v2.md`.
2. O backend continua sendo o único módulo com acesso ao banco.
3. O OPRM continua dono do domínio dos seus artefatos.
4. Integrações devem ser explícitas, versionadas e testadas.
5. Mudanças relevantes exigem atualização do histórico de implantação.
6. Nenhuma melhoria nova do OPRM deve furar a fila da integração estrutural.
7. Primeiro fecha integração; depois evolui sofisticação de inferência.

---

## 17. Definição final

Este plano existe para orientar a transição do OPRM de:

**módulo funcional local por fases**

para:

**worker Spring Boot plenamente integrado ao backend principal do Marketing Hub, com contrato explícito, persistência end-to-end, lineage completo, feedback loop persistido e observabilidade operacional.**
