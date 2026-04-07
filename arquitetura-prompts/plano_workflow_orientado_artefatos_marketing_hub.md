# Plano de evolução para Workflow Orientado a Artefatos no Marketing Hub

## 1) Contexto e objetivo

Este documento traduz, para execução prática no código atual do Marketing Hub, a diretriz do arquivo `arquitetura_marketing_hub_workflow_ia.md`: evoluir de um pipeline de prompts para um **workflow orientado a artefatos**.

Objetivo central: fazer cada etapa do pipeline produzir, validar, versionar e observar artefatos tipados (com lineage e evidências), reduzindo fragilidade operacional e acelerando iteração com previsibilidade.

---

## 2) Leitura do estado atual da aplicação

### 2.1 Pipeline de experimento (campanha → landing)

O produto já possui pipeline em estágios explícitos (`campaign-angle`, `ad-copy`, `ad-image-briefing`, `landing-page-copy`, `landing-page-wireframe`, `landing-page-image-planning`, `landing-page-html`) com dependência de predecessor entre seções. Isso é um bom alicerce de orquestração.  
Também já existe fila de jobs com status/stage (`PENDING`, `PROCESSING`, `COMPLETED`, `FAILED` e estágios de trânsito OpenAI), claim por worker, timeout de jobs antigos e persistência de request/response por execução.

No AI Worker, existe um cliente dedicado para pipeline de experimento com:
- prefixo global de regras DOR→RESULTADO→MECANISMO→PROVA→AÇÃO;
- instruções por seção;
- hardening de schema JSON (nome, required, additionalProperties);
- retentativa para erros transitórios;
- forçamento de modelo para consistência.

No frontend, já existe página de histórico dos jobs do pipeline com filtro por seção, status e inspeção de prompt/JSON.

**Conclusão:** o pipeline atual já é “multi-etapas com contrato parcial”, mas ainda depende fortemente de JSON livre por seção armazenado em campos de experimento e não em um catálogo canônico de artefatos com versão/linhagem formal.

### 2.2 Pipeline do framework Dor-Resultado-Mecanismo-Prova-Oferta

O framework também já opera por seções (`pain`, `result`, `mechanism`, `proof`, `offer`), incluindo geração de resumo por seção (`summary`).

Pontos fortes já existentes:
- schema por seção no backend;
- snapshots parciais aplicados ao framework da hipótese;
- fila de jobs dedicada e estágios internos;
- registro de gerações com domínio (`hypothesis.framework.<section>`);
- UI com geração por aba, status por seção e exportação de relatório de prompts/respostas.

No entanto, ainda há acoplamento alto com “estrutura da hipótese” e pouco desacoplamento em artefatos independentes reutilizáveis por outros módulos (ex.: pipeline de experimento, pesquisa de mercado, módulo científico e vídeo).

### 2.3 Lacunas para atingir o nível “workflow orientado a artefatos”

1. **Sem registro canônico de artefato**: hoje há jobs e campos finais, mas não uma entidade transversal de artefato (`artifact_type`, `artifact_version`, `parent_artifact_id`, `supersedes_id`, `quality_status`, `evidence_refs`).
2. **Lineage incompleto**: há histórico de job, mas não grafo explícito “este `landing-page-copy` veio destes inputs/versionamentos”.
3. **Validação em camadas limitada**: validação está concentrada no JSON schema; faltam validadores de negócio/evals por seção (ex.: CTA match, message match, compliance semântico, legibilidade).
4. **Reuso entre pipelines ainda fraco**: o framework D-R-M-P-O gera valor, porém sem contrato de exportação para o pipeline de experimento consumir formalmente como artefatos versionados.
5. **Evidência e citação estruturada**: no framework há uso de web_search em partes do prompt, mas sem padrão único de evidência anexada por artefato (fonte, trecho, confiança, data de coleta).
6. **Governança de prompt e schema versionado em conjunto**: o prompt é rastreável por job, porém sem política formal de versionamento acoplado “prompt template vX + schema vY + evaluator set vZ”.

---

## 3) Target state (estado-alvo)

Implementar um núcleo de artefatos que permita:

- **Criação tipada**: todo output de IA vira artefato formal.
- **Versionamento**: novas gerações não sobrescrevem o passado; apenas criam nova versão.
- **Lineage**: cada artefato registra quais artefatos/inputs o originaram.
- **Validação determinística + eval semântica**: schema + regras de negócio + score.
- **Publicação controlada**: apenas artefatos aprovados entram em campos finais do experimento/hipótese.
- **Reuso transversal**: framework D-R-M-P-O abastece pipeline de experimento via IDs de artefatos, não cópia textual ad hoc.

---

## 4) Plano de implementação por trilhas

## Trilha A — Modelo canônico de artefatos (Backend)

Criar um subsistema `ai_artifact` no backend (ads-service) com entidades mínimas:

- `ai_artifact`
  - `id` (UUID)
  - `artifact_type` (ex.: `framework.pain`, `experiment.landing.copy`, `experiment.landing.image_plan`)
  - `reference_type` (`hypothesis`, `experiment`, etc.)
  - `reference_id`
  - `version`
  - `status` (`DRAFT`, `VALIDATED`, `APPROVED`, `REJECTED`, `PUBLISHED`)
  - `content_json` (LONGTEXT)
  - `schema_name`, `schema_version`
  - `prompt_template_id`, `prompt_template_version`
  - `model`
  - `input_tokens`, `output_tokens`, `cost_usd`
  - `created_at`, `created_by`

- `ai_artifact_lineage`
  - `artifact_id`
  - `parent_artifact_id`
  - `relation_type` (`DERIVED_FROM`, `USES_AS_CONTEXT`, `SUPERSEDES`)

- `ai_artifact_evidence`
  - `artifact_id`
  - `source_url`
  - `source_title`
  - `excerpt`
  - `citation`
  - `confidence`
  - `fetched_at`

- `ai_artifact_eval`
  - `artifact_id`
  - `evaluator_key` (ex.: `CTA_MATCH`, `PROMISE_MATCH`, `FRAMEWORK_CLARITY`)
  - `score`
  - `status` (`PASS`, `WARN`, `FAIL`)
  - `details_json`

### Entrega da trilha

- Migrações Liquibase incrementais (MySQL 5 compatível).
- Repositórios + serviços de artefato.
- API interna para registrar artefatos durante `completeJob`.

## Trilha B — Adapter dos pipelines atuais para artefatos

### B1. Pipeline de experimento

No `ExperimentPipelineGenerationService.completeJob`:
- além de aplicar no campo final do experimento, gravar `ai_artifact` por seção;
- vincular lineage com artefatos predecessores;
- anexar request/response normalizados;
- executar evals determinísticas por seção antes de publicar no experimento.

Política recomendada:
- `status=VALIDATED` após schema + validadores de regra;
- `status=APPROVED` após eval mínima por seção;
- somente `APPROVED` pode sincronizar `experiment.<campo>`.

### B2. Framework D-R-M-P-O

No `HypothesisFrameworkGenerationService.completeJob`:
- persistir cada seção gerada como `ai_artifact` tipo `framework.<section>`;
- persistir resumo como `framework.<section>.summary`;
- manter snapshots da hipótese como projeção de leitura, não como única fonte histórica.

Adicionar endpoint para “promover artefato do framework para contexto do experimento”:
- cria vínculo lineage `experiment.campaign_angle DERIVED_FROM framework.offer.summary` (exemplo).

## Trilha C — Contratos e schemas versionados

Criar registry de schemas versionados (backend):
- `artifact_schema_registry` (nome, versão, json_schema, ativo).

Regras:
- mudança breaking → nova versão;
- job salva `schema_name/schema_version` usado;
- worker recebe schema por versão explícita.

Benefício: rollback de prompt/model sem perder compatibilidade de leitura.

## Trilha D — Evals por etapa (determinístico + semântico)

Definir baseline de evals obrigatórias:

### Para framework D-R-M-P-O
- `NO_JARGON_OVERLOAD`
- `RESULT_NOT_MECHANISM`
- `PROOF_CONCRETENESS`
- `OFFER_DELIVERABLE_FIT`

### Para pipeline de experimento
- `CTA_MATCH` (ad ↔ landing)
- `PROMISE_MATCH` (campaign-angle ↔ ad-copy ↔ landing-copy)
- `SECTION_COMPLETENESS`
- `COMPLIANCE_ENVELOPE`
- `IMAGE_PLAN_SECTION_BINDING`

Implementação inicial:
- regras deterministicamente codificadas (regex/JSON-path/regras de consistência);
- score simples 0–100 e status PASS/WARN/FAIL;
- bloqueio de publicação quando FAIL crítico.

## Trilha E — UI de artefatos e observabilidade

Adicionar no frontend:
- aba “Artefatos” em hipótese e experimento;
- timeline de versões por tipo de artefato;
- grafo simples de lineage (lista de pais/filhos na V1);
- painel de evals com explicação de FAIL/WARN;
- ação de “promover versão” para publicação.

No mínimo para V1:
- listar artefatos por `reference_id`;
- visualizar JSON + metadados (modelo, custo, tokens, prompt template/version);
- comparar versão N vs N-1.

## Trilha F — PromptOps (template + política)

Evoluir de prompt inline para PromptOps controlado:

- armazenar `prompt_template` e `prompt_template_version`;
- renderizar com contexto explícito (sem concatenação dispersa);
- atrelar schema alvo por template;
- logar hash final do prompt renderizado.

Isso permite:
- auditoria precisa;
- experimento A/B de prompt sem drift oculto;
- rollback seguro.

---

## 5) Sequência de execução recomendada (roadmap de 4 fases)

### Fase 1 — Fundação (2 a 3 sprints)
1. Tabelas `ai_artifact`, `ai_artifact_lineage`, `ai_artifact_eval`.
2. Persistir artefatos no `completeJob` de ambos pipelines.
3. Evals mínimas determinísticas por seção.
4. Endpoint de listagem de artefatos por hipótese/experimento.

### Fase 2 — Governança (1 a 2 sprints)
1. Registry de schema versionado.
2. Prompt template versionado com vínculo a schema.
3. Bloqueio de publicação com FAIL crítico.

### Fase 3 — Reuso transversal (2 sprints)
1. Conexão formal framework → experiment pipeline via lineage.
2. Regras automáticas de contexto (quais artefatos entram como input).
3. Evidência anexada por artefato para seções analíticas.

### Fase 4 — Escala operacional (contínuo)
1. Dashboard de qualidade/custo/latência por tipo de artefato.
2. Regressão de evals em mudança de prompt/model.
3. Catálogo de artefatos para módulos futuros (market research, científico, vídeo).

---

## 6) Critérios de pronto (Definition of Done)

Considerar que o Marketing Hub atingiu nível “workflow orientado a artefatos” quando:

1. **100% das seções de pipeline e framework** gerarem `ai_artifact` versionado.
2. Toda geração tiver **lineage explícita** para inputs principais.
3. Toda seção crítica passar por **schema + eval mínima obrigatória**.
4. Publicação em `experiment`/`hypothesis` ocorrer por **promoção de artefato aprovado**, não escrita direta cega.
5. UI permitir inspeção de versões, custo e eval por artefato.

---

## 7) Riscos e mitigação

- **Risco:** aumento de complexidade e latência.  
  **Mitigação:** começar com artefatos apenas nas seções críticas e evals curtas.

- **Risco:** burocracia de versionamento.  
  **Mitigação:** convenção simples de tipos e versionamento automático incremental por `reference_id + artifact_type`.

- **Risco:** duplicidade entre “campo final” e “artefato”.  
  **Mitigação:** tratar campo final como projeção/publicação; fonte histórica é o artefato.

---

## 8) Próximos passos imediatos (ação prática)

1. Criar ADR interno: “Artifact-first Prompt Workflow no Marketing Hub”.
2. Implementar migração e entidade `ai_artifact` no backend.
3. Adaptar `completeJob` do framework e do pipeline de experimento para gravar artefatos.
4. Entregar endpoint `/api/.../artifacts` para consulta no frontend.
5. Subir V1 de evals críticas (`CTA_MATCH`, `PROMISE_MATCH`, `RESULT_NOT_MECHANISM`).

Com isso, o Marketing Hub sai de “pipeline funcional com JSON por etapa” para “plataforma orientada a artefatos”, preparada para expansão segura em pesquisa de mercado, ciência aplicada e geração multimodal.
