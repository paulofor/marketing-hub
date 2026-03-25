# Proposta de evolução do Marketing Hub para experimentação orientada por hipóteses

## Objetivo

Este documento propõe uma evolução do **Marketing Hub** para tornar o fluxo **Nicho → Hipótese → Experimento** mais explícito, mensurável e auditável.

A recomendação central é incorporar ao produto três camadas novas:

1. **Matriz de hipóteses**
2. **Variáveis de experimento como entidade de primeira classe**
3. **Hierarquia formal de métricas de validação**

O objetivo não é alterar a direção atual do sistema, e sim ampliar o que já existe para que o Marketing Hub responda melhor a quatro perguntas em cada experimento:

1. O que acreditávamos antes do teste?
2. Qual variável realmente mudou?
3. Qual métrica valida ou invalida essa crença?
4. O que aprendemos para o próximo teste?

---

## Resumo executivo

O Marketing Hub já possui a base correta para um sistema de experimentação:

- fluxo conceitual de **Nicho → Hipótese → Experimento**
- cadastro e gestão de **criativos**, **landing pages**, **fluxos de lead portal** e **campanhas Meta Ads**
- coleta de **métricas de campanha** e de um **funil operacional padronizado**
- geração de **relatórios objetivos** via snapshot consolidado do experimento

A evolução sugerida neste documento é transformar a hipótese em um objeto mais operacional.

Hoje o sistema já guarda elementos estratégicos como **promessa, problema, persona e mecanismos**. A melhoria proposta é fazer com que cada hipótese também carregue:

- o **recorte exato** sendo testado
- a **variável isolada** do experimento
- a **métrica primária** de decisão
- métricas **secundárias** e **guardrails**
- uma **regra formal de validação / invalidação / iteração**

Com isso, o Marketing Hub deixa de ser apenas um orquestrador de campanhas e passa a ser também um sistema de **aprendizado experimental estruturado**.

---

## Diagnóstico do estado atual

A documentação pública do repositório mostra que o projeto já possui componentes muito relevantes:

### 1. Estrutura conceitual

O `README.md` já explicita o novo fluxo **Nicho → Hipótese → Experimento** e posiciona o experimento como unidade central para campanhas, criativos, landing pages e analytics.

### 2. Relatório objetivo do experimento

O documento `docs/experiment-reporting.md` mostra que o snapshot atual já consolida:

- `experiment`
- `niche`
- `hypothesis`
- `creatives`
- `creativeVariants`
- `landingPages`
- `leadPortalFlows`
- `instantForm`
- `campaignMetric`
- `funnelStages`

Isso é uma base excelente para auditoria histórica e geração de material externo.

### 3. Modelo focado em experimentos

O documento `docs/modelo-dados-experimento.md` já descreve o experimento como eixo de rastreabilidade entre:

- planejamento
- geração por IA
- publicação de mídia
- captação de lead
- funil
- métricas

Além disso, ele já define um **funil operacional de nove etapas**, o que é um ativo importante para a camada de validação.

### 4. Gap principal identificado

O gap não está na inexistência de dados ou entidades. O gap está em tornar explícito:

- **qual hipótese específica** está sendo validada
- **qual variável** foi alterada em cada experimento
- **qual métrica** decide o resultado
- **qual regra de decisão** foi adotada
- **qual aprendizado** fica registrado para o backlog futuro

Em resumo: o projeto já tem boa cobertura operacional, mas ainda pode ficar mais forte como **sistema de experimentação com memória de aprendizado**.

---

## Princípios da evolução proposta

A proposta deste documento segue cinco princípios:

### 1. Uma hipótese precisa ser falsificável

A hipótese deve ser descrita de forma que possa ser validada ou invalidada por dados e feedback.

### 2. Cada experimento deve isolar uma variável principal

Quando o objetivo é aprendizado claro, a plataforma deve registrar qual variável central foi alterada.

### 3. A decisão precisa nascer de uma métrica primária

O experimento deve ter uma métrica que determine seu resultado principal.

### 4. Métricas secundárias e guardrails devem existir formalmente

Nem todo ganho é um ganho real. Uma variação pode subir cliques e piorar custo, abandono ou qualidade do lead.

### 5. O sistema deve registrar aprendizado, não só performance

O Marketing Hub precisa preservar o raciocínio experimental ao longo do tempo, e não apenas os números finais da campanha.

---

## Proposta 1 — Matriz de hipóteses

### Objetivo

Transformar a hipótese de um bloco estratégico em uma estrutura operacional pronta para testes.

### Conceito

Hoje a hipótese funciona como direção estratégica. A proposta é manter isso, mas acrescentar uma nova camada chamada **Matriz de Hipóteses**.

Essa matriz permite decompor uma hipótese em recortes menores, cada um com:

- dor específica
- resultado esperado
- variável principal
- etapa do funil
- métrica de validação
- regra de decisão

### Estrutura sugerida

#### Nova entidade: `hypothesis_matrix`

Representa a matriz vinculada a uma hipótese.

Campos sugeridos:

- `id`
- `hypothesis_id`
- `name`
- `description`
- `status`
- `created_at`
- `updated_at`

#### Nova entidade: `hypothesis_matrix_row`

Representa cada linha operacional da matriz.

Campos sugeridos:

- `id`
- `hypothesis_matrix_id`
- `segmento`
- `dor_central`
- `resultado_desejado`
- `promessa_profunda`
- `mecanismo_unico`
- `tipo_hipotese`
- `etapa_funil`
- `variavel_principal`
- `resultado_esperado`
- `metrica_primaria`
- `metricas_secundarias_json`
- `guardrails_json`
- `janela_validacao`
- `amostra_minima`
- `regra_decisao`
- `priority_score`
- `status`
- `notes`

### Tipos sugeridos para `tipo_hipotese`

- `PROMISE`
- `PAIN`
- `MECHANISM`
- `PROOF`
- `OFFER`
- `OBJECTION`
- `AUDIENCE`
- `CTA`
- `VISUAL`

### Benefícios

- deixa claro **o que exatamente está sendo testado**
- separa hipótese estratégica de hipótese operacional
- cria backlog estruturado para priorização
- facilita repetição de padrões que funcionam em múltiplos nichos

---

## Proposta 2 — Variáveis de experimento como entidade de primeira classe

### Objetivo

Registrar explicitamente qual variável foi alterada em cada experimento.

### Problema atual

Hoje é possível inferir muita coisa olhando criativos, landing pages e métricas. Mas o sistema pode ficar mais forte se declarar de forma explícita:

- qual é o **controle**
- qual é o **tratamento**
- em qual escopo a mudança ocorreu
- qual era a variável principal isolada

### Estrutura sugerida

#### Nova entidade: `experiment_variable`

Campos sugeridos:

- `id`
- `experiment_id`
- `hypothesis_matrix_row_id`
- `scope`
- `variable_type`
- `control_value`
- `treatment_value`
- `isolation_level`
- `notes`
- `created_at`
- `updated_at`

### Enum sugerido para `scope`

- `AD`
- `LANDING`
- `FORM`
- `EMAIL`
- `CHECKOUT`
- `FUNNEL`

### Enum sugerido para `variable_type`

- `PROMISE`
- `PAIN`
- `MECHANISM`
- `VISUAL_PROOF`
- `CTA`
- `FORM_LENGTH`
- `OFFER`
- `OBJECTION`
- `PROOF_ELEMENT`
- `HOOK`
- `HEADLINE`
- `TARGETING`

### Benefícios

- melhora leitura histórica dos testes
- aumenta clareza analítica do relatório
- reduz ambiguidade sobre o que foi aprendido
- facilita criar scorecards e comparações futuras por variável

---

## Proposta 3 — Hierarquia formal de métricas

### Objetivo

Classificar métricas por função analítica.

### Problema atual

O projeto já possui métricas de campanha e um funil rico. O próximo passo é explicitar o papel de cada métrica:

- qual métrica decide o experimento
- quais ajudam a interpretar
- quais funcionam como guardrails
- quais verificam qualidade de medição

### Estrutura sugerida

#### Nova entidade: `validation_metric`

Campos sugeridos:

- `id`
- `experiment_id`
- `metric_key`
- `metric_role`
- `formula`
- `source`
- `target_direction`
- `minimum_detectable_effect`
- `threshold_min`
- `threshold_max`
- `notes`

### Enum sugerido para `metric_role`

- `PRIMARY`
- `SECONDARY`
- `GUARDRAIL`
- `DATA_QUALITY`

### Lógica recomendada

#### Métrica primária

Decide se a hipótese foi validada, invalidada ou ficou inconclusiva.

#### Métricas secundárias

Ajudam a interpretar o efeito.

#### Guardrails

Garantem que o experimento não piorou dimensões importantes.

#### Data quality metrics

Ajudam a detectar leitura fraca ou não confiável.

### Exemplo de aplicação por etapa do funil

| Onde a mudança acontece | Métrica primária recomendada | Métricas secundárias | Guardrails |
|---|---|---|---|
| Criativo do anúncio | CTR de link | CPC, LPV, taxa de acesso ao formulário | frequência, CPL |
| Hero / copy da landing | taxa de início do formulário | scroll, clique CTA | bounce, CPL |
| Formulário | taxa de envio | tempo até envio, abandono | qualidade do lead |
| Oferta / checkout | clique no checkout ou compra | início de checkout, abertura de e-mail | CAC, rejeição |
| E-mail / reativação | clique no link principal | abertura, resposta | unsubscribe |

### Benefícios

- melhora consistência da leitura de resultados
- evita usar métricas distantes demais da mudança feita
- viabiliza relatórios objetivos com lógica mais forte de decisão

---

## Proposta 4 — Decisão formal do experimento

### Objetivo

Registrar o encerramento analítico do experimento.

### Estrutura sugerida

#### Nova entidade: `experiment_decision`

Campos sugeridos:

- `id`
- `experiment_id`
- `primary_metric_value_control`
- `primary_metric_value_treatment`
- `secondary_metrics_snapshot_json`
- `guardrail_status_json`
- `decision`
- `confidence_notes`
- `learning_summary`
- `next_recommendation`
- `decided_by`
- `decided_at`

### Enum sugerido para `decision`

- `VALIDATED`
- `INVALIDATED`
- `INCONCLUSIVE`
- `ITERATE`
- `PAUSED`

### Benefícios

- fecha o ciclo de aprendizado
- facilita retrospectiva de testes
- transforma resultados em backlog acionável

---

## Proposta 5 — Priorização com backlog experimental

### Objetivo

Tornar o board de hipóteses mais útil para priorização.

### Sugestão

Adicionar score simples do tipo **ICE**:

- `impact`
- `confidence`
- `ease`
- `ice_score`

### Benefícios

- melhora ordem de execução do backlog
- reduz priorização baseada apenas em sensação
- ajuda a distinguir testes de alto valor e baixo valor

---

## Proposta 6 — Separar descoberta de otimização

### Objetivo

Diferenciar experimentos de descoberta de mercado dos experimentos de refinamento.

### Enum sugerido para `experiment_mode`

- `DISCOVERY`
- `OPTIMIZATION`

### Regras sugeridas

#### `DISCOVERY`

Usado quando o objetivo principal é validar:

- dor
- promessa
- mecanismo
- oferta
- segmento
- prova

#### `OPTIMIZATION`

Usado quando o objetivo principal é melhorar eficiência de algo já validado:

- CTR
- CPC
- taxa de formulário
- CPL
- CVR
- CAC

### Benefícios

- evita misturar aprendizado de mercado com refinamento operacional
- melhora leitura do backlog e dos relatórios
- ajuda a definir expectativas corretas por experimento

---

## Proposta 7 — Evolução do snapshot de relatório

O snapshot atual já é um ativo importante. A proposta é enriquecer o `payload_snapshot` com blocos adicionais.

### Blocos novos sugeridos no `report-material`

- `hypothesisMatrix`
- `selectedMatrixRow`
- `experimentVariables`
- `validationMetrics`
- `decisionScorecard`
- `experimentMode`
- `prioritization`

### Exemplo de blocos adicionais

```json
{
  "hypothesisMatrix": {
    "id": 12,
    "name": "Personais — captação via Instagram",
    "rows": []
  },
  "selectedMatrixRow": {
    "id": 44,
    "tipoHipotese": "PROMISE",
    "variavelPrincipal": "promessa",
    "resultadoEsperado": "maior taxa de início do formulário"
  },
  "experimentVariables": [
    {
      "scope": "LANDING",
      "variableType": "PROMISE",
      "controlValue": "Tenha mais material para divulgar",
      "treatmentValue": "Aumente suas chances de conquistar novos alunos"
    }
  ],
  "validationMetrics": [
    {
      "metricKey": "FORM_START_RATE",
      "metricRole": "PRIMARY"
    },
    {
      "metricKey": "CPL",
      "metricRole": "GUARDRAIL"
    }
  ],
  "decisionScorecard": {
    "decision": "VALIDATED",
    "learningSummary": "A promessa centrada em resultado gerou maior início de formulário",
    "nextRecommendation": "Testar pain angle mantendo a mesma promessa"
  }
}
```

### Benefícios

- melhora a utilidade do relatório objetivo
- facilita QA analítico
- torna o material mais útil para serviços externos de geração de relatório

---

## Proposta 8 — Presets de validação por camada do funil

### Objetivo

Padronizar testes recorrentes.

### Nova entidade opcional: `experiment_preset`

Campos sugeridos:

- `id`
- `name`
- `description`
- `scope`
- `default_primary_metric`
- `default_secondary_metrics_json`
- `default_guardrails_json`
- `default_decision_rule`

### Presets sugeridos

- `AD_MESSAGE_FIT`
- `LANDING_MESSAGE_FIT`
- `FORM_FRICTION_TEST`
- `OFFER_TEST`
- `LEAD_QUALITY_TEST`
- `NURTURING_TEST`

### Benefícios

- acelera criação de novos testes
- reduz inconsistência operacional
- facilita onboarding do time

---

## Sugestão de API

### Hipóteses e matriz

- `GET /api/hypotheses/{id}/matrix`
- `POST /api/hypotheses/{id}/matrix`
- `POST /api/hypotheses/{id}/matrix/rows`
- `PATCH /api/hypothesis-matrix-rows/{id}`
- `GET /api/hypothesis-matrix-rows/{id}`

### Variáveis de experimento

- `GET /api/experiments/{id}/variables`
- `POST /api/experiments/{id}/variables`
- `PATCH /api/experiment-variables/{id}`

### Métricas de validação

- `GET /api/experiments/{id}/validation-metrics`
- `POST /api/experiments/{id}/validation-metrics`
- `PATCH /api/validation-metrics/{id}`

### Decisão do experimento

- `GET /api/experiments/{id}/decision`
- `POST /api/experiments/{id}/decision`
- `PATCH /api/experiments/{id}/decision`

### Backlog e priorização

- `GET /api/hypotheses/board?tipo=...&status=...`
- `PATCH /api/hypothesis-matrix-rows/{id}/priority`

---

## Sugestão de evolução da UI

### 1. Tela de hipótese

Adicionar abas:

- **Resumo**
- **Matriz**
- **Backlog experimental**
- **Aprendizados**

### 2. Tela de experimento

Adicionar blocos:

- **Linha da matriz selecionada**
- **Variável principal testada**
- **Métrica primária**
- **Guardrails**
- **Scorecard de decisão**

### 3. Board de hipóteses

Adicionar filtros por:

- tipo de hipótese
- modo do experimento
- score ICE
- status
- aprendizado validado / invalidado / inconclusivo

### 4. Overview / relatório

Adicionar cartão de validação:

- hipótese testada
- variável isolada
- primary metric
- decisão
- resumo do aprendizado
- próximo teste recomendado

---

## Sugestão de evolução do modelo de dados

### Tabelas novas

- `hypothesis_matrix`
- `hypothesis_matrix_row`
- `experiment_variable`
- `validation_metric`
- `experiment_decision`
- `experiment_preset` (opcional)

### Campos novos em tabelas existentes

#### `EXPERIMENT`

Sugestões:

- `mode`
- `hypothesis_matrix_row_id`
- `primary_metric_key`
- `decision_status`
- `learning_summary`

#### `HYPOTHESIS`

Sugestões:

- `hypothesis_type_default`
- `default_success_metric`

#### `EXPERIMENT_REPORT_REQUEST`

Continuar usando `payload_snapshot`, mas enriquecido com os novos blocos.

---

## Scorecard padronizado para relatórios

Recomendação: todo relatório objetivo do experimento deveria terminar com um scorecard padronizado.

### Estrutura sugerida

| Campo | Conteúdo |
|---|---|
| Hipótese | descrição curta |
| Linha da matriz | id + nome |
| Variável testada | promessa / dor / mecanismo / etc. |
| Controle | valor controle |
| Tratamento | valor tratamento |
| Métrica primária | nome + valor |
| Métricas secundárias | principais leituras |
| Guardrails | ok / alerta |
| Decisão | validated / invalidated / inconclusive / iterate |
| Aprendizado | frase curta |
| Próximo passo | recomendação |

---

## Exemplo prático: nicho de personal trainer

### Hipótese estratégica

Personal trainers respondem melhor a uma promessa centrada em **atrair mais interesse e aumentar as chances de conquistar novos alunos** do que a uma promessa centrada em **economizar tempo para criar posts**.

### Linha da matriz

| Campo | Valor |
|---|---|
| Segmento | personal trainer autônomo |
| Dor central | o Instagram não gera interesse suficiente |
| Resultado desejado | mais contatos qualificados |
| Promessa profunda | aumentar as chances de conquistar novos alunos |
| Tipo de hipótese | `PROMISE` |
| Etapa do funil | `LANDING` |
| Variável principal | promessa |
| Métrica primária | `FORM_START_RATE` |
| Métricas secundárias | `FORM_SUBMIT_RATE`, `CTR`, `LPV` |
| Guardrails | `CPL`, bounce |
| Regra de decisão | validar se `FORM_START_RATE` subir mantendo guardrails estáveis |

### Variável do experimento

| Campo | Valor |
|---|---|
| Scope | `LANDING` |
| Variable type | `PROMISE` |
| Controle | “Tenha muito material de divulgação sem perder tempo criando” |
| Tratamento | “Aumente suas chances de conquistar novos alunos com um Instagram que valoriza seu trabalho” |

### Resultado esperado

A variação com promessa mais profunda deve gerar maior taxa de início do formulário e melhor engajamento com a proposta.

---

## Roadmap sugerido de implementação

### Fase 1 — Fundação analítica

- criar `hypothesis_matrix` e `hypothesis_matrix_row`
- criar `experiment_variable`
- permitir vincular experimento a uma linha da matriz
- exibir isso na UI

### Fase 2 — Governança de métricas

- criar `validation_metric`
- permitir marcar primary / secondary / guardrail
- exibir scorecard na tela do experimento

### Fase 3 — Decisão e memória

- criar `experiment_decision`
- registrar aprendizado e próxima recomendação
- expor tudo no relatório objetivo

### Fase 4 — Priorização e escala

- adicionar ICE score
- presets por tipo de experimento
- filtros avançados no board

---

## Critérios de sucesso da evolução

A evolução proposta será bem-sucedida se o Marketing Hub passar a permitir, de forma simples e consistente:

1. criar uma hipótese com recortes testáveis
2. declarar qual variável está sendo alterada
3. definir métrica primária, secundárias e guardrails
4. registrar decisão formal do experimento
5. transformar o resultado em aprendizado persistente
6. priorizar backlog com base em impacto, confiança e facilidade

---

## Recomendação final

O Marketing Hub já está próximo de se tornar não apenas uma plataforma de execução de testes, mas uma **plataforma de aprendizado experimental**.

A melhor evolução agora não é adicionar apenas mais campos, e sim tornar explícito o raciocínio por trás de cada teste.

Em termos práticos, a recomendação principal deste documento é:

- manter o fluxo atual **Nicho → Hipótese → Experimento**
- adicionar uma **Matriz de Hipóteses**
- tratar **Variáveis de Experimento** como entidade própria
- formalizar a **Hierarquia de Métricas**
- fechar o ciclo com uma **Decisão de Experimento** e um **Scorecard de Aprendizado**

Essa mudança tende a aumentar:

- clareza do backlog
- qualidade das análises
- consistência da priorização
- reutilização de aprendizados
- maturidade do Marketing Hub como sistema de experimentação

---

## Sugestão de nome do arquivo no repositório

```text
docs/proposta-matriz-hipoteses-validacao.md
```

---

## Referências

### Documentação do próprio projeto

- README do repositório Marketing Hub
- `docs/experiment-reporting.md`
- `docs/modelo-dados-experimento.md`

### Referências externas que embasam a proposta

- CXL — hipótese experimental como crença verificável e estrutura “We believe… We’ll know this when…”
- CXL — priorização de backlog com ICE (Impact, Confidence, Ease)
- Optimizely — distinção entre primary metrics, secondary metrics e monitoring goals / guardrails
- Microsoft Research — trustworthy experimentation com foco em métricas bem desenhadas e taxonomia holística antes, durante e depois do experimento

