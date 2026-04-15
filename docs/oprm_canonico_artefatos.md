# OPRM — Cânone Inicial de Artefatos

## 1. Objetivo

Este documento define o **cânone inicial de artefatos** do módulo **Occupation Persona Routine Mapper (OPRM)**.

Ele serve para:

- padronizar os artefatos produzidos e consumidos pelo OPRM
- criar uma base estável para implementação incremental
- reduzir drift entre worker, backend e módulos consumidores
- orientar schemas futuros, contratos de API e persistência
- alinhar o OPRM ao framework **dor → resultado → oferta → mecanismo → prova**

Este documento **não** é o schema final dos artefatos.
Ele é a referência canônica inicial para:
- naming
- papel de cada artefato
- envelope comum
- lineage
- regras de evolução

---

## 2. Referências canônicas comuns

Este documento deve ser interpretado em conjunto com:

- `docs/canonical/system-governance-canon.v2.md`
- `oprm/docs/oprm_plano_geral_implementacao.md`  
  ou o caminho equivalente adotado no repositório

Se houver conflito:
1. schema publicado do artefato
2. cânone específico do OPRM
3. cânone global do Marketing Hub
4. implementação atual

Comportamento divergente no código não redefine o artefato; sinaliza drift.

---

## 3. Papel do backend e do OPRM

### 3.1 OPRM
O OPRM é o módulo que:
- processa ocupações
- pesquisa fontes
- infere rotina
- produz sinais e artefatos

### 3.2 Backend
O backend:
- entrega seeds, jobs, contexto e parâmetros
- recebe os artefatos produzidos
- persiste os dados

### 3.3 Regra
O backend **não define o domínio dos artefatos do OPRM**.
O domínio dos artefatos é definido por:
- este documento
- futuros schemas publicados
- contratos explícitos do módulo
- governança canônica do Marketing Hub

---

## 4. Princípios dos artefatos

Todo artefato do OPRM deve seguir estes princípios:

1. **Explícito**
   - o nome do artefato deve refletir sua função

2. **Versionável**
   - a estrutura deve poder evoluir sem ambiguidade

3. **Traçável**
   - todo artefato importante deve guardar lineage e evidência

4. **Separado por camada**
   - observação, inferência, síntese e integração não devem ser misturadas no mesmo artefato sem necessidade

5. **Consumível**
   - o artefato deve ser útil para backend, pipeline e módulos downstream

6. **Auditável**
   - deve ser possível entender de onde o artefato veio e por que foi gerado

---

## 5. Envelope canônico comum

Todo artefato persistido ou transportado pelo OPRM deve usar um envelope comum.

Campos mínimos do envelope:

- `artifact_type`
- `artifact_version`
- `artifact_id`
- `module_name`
- `producer`
- `created_at`
- `correlation_id`
- `trace_id`
- `source_refs`
- `input_refs`
- `payload`
- `status`
- `confidence_score`
- `metadata`

### 5.1 Semântica dos campos

- `artifact_type`  
  nome canônico do artefato

- `artifact_version`  
  versão estrutural do artefato

- `artifact_id`  
  identificador único do artefato

- `module_name`  
  normalmente `oprm`

- `producer`  
  componente ou etapa que produziu o artefato

- `created_at`  
  timestamp de geração

- `correlation_id`  
  identificador do fluxo maior

- `trace_id`  
  identificador técnico de rastreamento

- `source_refs`  
  referências de fontes externas observadas

- `input_refs`  
  referências a artefatos anteriores usados como entrada

- `payload`  
  conteúdo específico do artefato

- `status`  
  estado do artefato dentro do pipeline

- `confidence_score`  
  confiança agregada daquele artefato

- `metadata`  
  espaço para atributos auxiliares não canônicos

---

## 6. Regras de lineage

Artefatos relevantes do OPRM devem preservar lineage.

### 6.1 Lineage mínimo
Todo artefato analítico ou de síntese deve conseguir apontar:
- quais fontes ajudaram a gerar o artefato
- quais artefatos anteriores serviram de base
- qual etapa do módulo produziu o resultado

### 6.2 Regras
- `source_refs` aponta para fontes externas observadas
- `input_refs` aponta para artefatos anteriores do pipeline
- artefato sem lineage suficiente não deve subir como artefato de síntese confiável

---

## 7. Estados canônicos do artefato

Campos possíveis para `status`:

- `DRAFT`
- `GENERATED`
- `VALIDATED`
- `REJECTED`
- `PUBLISHED`
- `SUPERSEDED`

### 7.1 Regra inicial
No MVP, o mínimo recomendado é usar:
- `GENERATED`
- `VALIDATED`
- `PUBLISHED`
- `REJECTED`

---

## 8. Camadas de artefatos do OPRM

O OPRM deve trabalhar com cinco grupos de artefatos:

1. artefatos de entrada
2. artefatos de observação
3. artefatos de interpretação
4. artefatos de síntese
5. artefatos de integração com o framework

---

## 9. Artefatos de entrada

### 9.1 `occupationSeed`
Representa a intenção inicial de mapear uma ocupação/persona.

Uso:
- iniciar processamento
- informar ocupação, alias, nicho e contexto inicial

Payload sugerido:
- `persona_label`
- `occupation_name`
- `occupation_aliases`
- `niche_name`
- `locale`
- `priority`
- `requested_by`
- `notes`

### 9.2 `occupationAliasResolution`
Representa a resolução entre nome livre e ocupação normalizada.

Uso:
- reduzir ambiguidade
- ligar aliases a uma ocupação tratada pelo módulo

Payload sugerido:
- `raw_label`
- `normalized_label`
- `matched_occupation_name`
- `match_type`
- `match_confidence`
- `resolver_notes`

### 9.3 `occupationSourcePolicyProfile`
Representa a política de coleta para uma ocupação ou grupo de fontes.

Uso:
- governar allowlist
- classificar risco e permissão

Payload sugerido:
- `allowed_domains`
- `blocked_domains`
- `rate_limit_policy`
- `source_risk_level`
- `manual_review_required`
- `notes`

---

## 10. Artefatos de observação

### 10.1 `occupationProfileSnapshot`
Representa o snapshot ocupacional estruturado inicial.

Uso:
- capturar a base ocupacional formal
- servir como esqueleto do restante do pipeline

Payload sugerido:
- `occupation_name`
- `occupation_summary`
- `task_list`
- `skills_list`
- `tools_list`
- `work_context_list`
- `source_system`
- `source_record_ids`

### 10.2 `occupationWebSourceSnapshot`
Representa um snapshot de uma fonte pública complementar.

Uso:
- capturar evidência de enriquecimento público

Payload sugerido:
- `url`
- `source_type`
- `title`
- `captured_at`
- `language`
- `content_hash`
- `extracted_blocks`
- `capture_notes`

### 10.3 `occupationContextSignal`
Representa um sinal específico de contexto operacional observado.

Uso:
- registrar fragmentos de contexto relevantes

Payload sugerido:
- `signal_type`
- `signal_summary`
- `occupation_name`
- `context_label`
- `evidence_excerpt`
- `source_ref`

### 10.4 `occupationTaskEvidence`
Representa uma evidência observada de tarefa real da ocupação.

Uso:
- sustentar inferências posteriores sobre rotina

Payload sugerido:
- `task_label`
- `task_summary`
- `evidence_type`
- `evidence_excerpt`
- `source_ref`
- `task_confidence`

---

## 11. Artefatos de interpretação

### 11.1 `routineTaskPattern`
Representa uma tarefa recorrente inferida na rotina da persona ocupacional.

Uso:
- consolidar tarefas frequentes
- organizar o dia a dia da persona

Payload sugerido:
- `task_label`
- `task_summary`
- `trigger_summary`
- `frequency_signal`
- `time_cost_signal`
- `tooling_summary`
- `evidence_refs`

### 11.2 `routineConstraintSignal`
Representa uma restrição relevante da rotina.

Uso:
- registrar barreiras de tempo, contexto, recurso ou dependência

Payload sugerido:
- `constraint_type`
- `constraint_summary`
- `severity_score`
- `context_summary`
- `evidence_refs`

### 11.3 `routinePainSignal`
Representa uma dor operacional inferida.

Uso:
- alimentar a camada de dor do framework

Payload sugerido:
- `pain_label`
- `pain_summary`
- `pain_type`
- `pain_intensity_score`
- `pain_recurrence_score`
- `workaround_summary`
- `evidence_refs`

### 11.4 `routineWorkaroundSignal`
Representa uma gambiarra, adaptação ou solução informal usada pela persona.

Uso:
- identificar pontos claros de oportunidade

Payload sugerido:
- `workaround_label`
- `workaround_summary`
- `related_task`
- `related_pain`
- `inefficiency_score`
- `evidence_refs`

### 11.5 `desiredOutcomeSignal`
Representa um resultado desejado inferido a partir da rotina e da dor.

Uso:
- alimentar a camada de resultado do framework

Payload sugerido:
- `outcome_label`
- `outcome_summary`
- `linked_pain_refs`
- `impact_score`
- `evidence_refs`

### 11.6 `mechanismOpportunitySignal`
Representa uma oportunidade de mecanismo plausível.

Uso:
- alimentar a camada de mecanismo do framework

Payload sugerido:
- `mechanism_label`
- `mechanism_summary`
- `linked_task_refs`
- `linked_pain_refs`
- `commercial_fit_score`
- `implementation_effort_score`
- `evidence_refs`

---

## 12. Artefato principal de síntese

### 12.1 `occupationPersonaRoutineCard`
É o artefato sintético principal do OPRM.

Uso:
- resumir a rotina operacional da persona ocupacional
- servir como artefato base para módulos downstream

Payload sugerido:
- `persona_label`
- `occupation_name`
- `occupation_aliases`
- `niche_name`
- `routine_summary`
- `top_tasks`
- `top_tools`
- `top_constraints`
- `top_work_contexts`
- `customer_interaction_pattern`
- `revenue_dependency_pattern`
- `admin_burden_pattern`
- `workaround_patterns`
- `pain_signals`
- `desired_outcome_signals`
- `mechanism_opportunity_signals`
- `evidence_refs`
- `source_mix`

---

## 13. Artefatos de integração com o framework

### 13.1 `dorResultadoOfertaMecanismoProvaInput`
Representa o pacote de entrada do OPRM para o framework do Marketing Hub.

Uso:
- fornecer sinais estruturados para geração e refinamento do framework

Payload sugerido:
- `pain_signals`
- `desired_outcome_signals`
- `mechanism_opportunity_signals`
- `evidence_refs`
- `origin_artifact_refs`
- `integration_notes`

### 13.2 `hypothesisDraftInput`
Representa um insumo de hipótese gerado a partir da rotina da persona.

Uso:
- alimentar geração de hipótese e experimento

Payload sugerido:
- `persona_label`
- `pain_summary`
- `desired_outcome_summary`
- `mechanism_summary`
- `proof_angle_summary`
- `evidence_refs`

---

## 14. Regras de naming

### 14.1 Convenção
Usar nomes em `camelCase` no tipo de artefato.

Exemplos:
- `occupationSeed`
- `routinePainSignal`
- `occupationPersonaRoutineCard`

### 14.2 Regra
Tipos de artefato devem ser:
- curtos
- descritivos
- semanticamente estáveis
- sem abreviações obscuras

---

## 15. Regras de versionamento

### 15.1 Versão do artefato
Cada artefato deve ter `artifact_version`.

Formato inicial sugerido:
- `1.0`
- `1.1`
- `2.0`

### 15.2 Regra de evolução
- mudança compatível: incrementa versão menor
- mudança incompatível: incrementa versão maior

### 15.3 Regra inicial do OPRM
Enquanto não houver schema formal publicado, considerar este documento como referência da série inicial `1.x`.

---

## 16. Regras de validação

Um artefato do OPRM só deve ser considerado válido quando:

- possui envelope mínimo completo
- possui `artifact_type` reconhecido
- possui `payload` compatível com o tipo
- possui lineage suficiente
- possui `confidence_score` explícito quando for analítico ou sintético

---

## 17. Regras de consumo por outros módulos

Outros módulos do Marketing Hub não devem depender de campos implícitos ou comportamento inferido do payload.

Devem consumir:
- tipo do artefato
- versão do artefato
- envelope comum
- contrato explícito do payload

---

## 18. Regras de persistência

O backend do Marketing Hub é responsável por persistir os artefatos publicados pelo OPRM.

Consequências:
- o OPRM produz artefatos
- o backend armazena artefatos
- o backend não redefine o significado do artefato
- o contrato do artefato continua pertencendo ao domínio do OPRM e aos cânones publicados

---

## 19. Roadmap recomendado após este documento

Próximos documentos sugeridos:

1. `oprm_artifacts.schema.md`
   - schema mais detalhado por artefato

2. `oprm_artifacts_examples.md`
   - exemplos concretos de payloads

3. `oprm_integration_contracts.md`
   - contratos entre OPRM e backend

4. `oprm_source_policy.md`
   - regras formais de fontes e coleta

---

## 20. Critério final

Se um dado:
- circula entre etapas
- precisa ser persistido
- será consumido por outro módulo
- influencia o framework dor → resultado → oferta → mecanismo → prova

então ele deve virar ou compor um artefato canônico explícito do OPRM.
