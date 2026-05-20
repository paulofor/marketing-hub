# Plano de Implementação — Market-Psychological Fit Score

## 1. Objetivo

Implementar no Marketing Hub uma camada de análise capaz de medir não apenas se um produto tem potencial comercial, mas também **o quanto ele entra na mente do consumidor**.

O score deve avaliar:

- O símbolo mental que o produto representa para a pessoa.
- O quanto o produto aproxima a pessoa de um prazer desejado.
- O quanto o produto afasta a pessoa de uma dor percebida.
- Como cada conceito de comunicação performa dentro de uma hipótese.
- Em quais seções da página de venda o consumidor demonstra interesse, fricção, crença ou intenção de ação.

A ideia central é:

> Não testar apenas produtos. Testar crenças, dores, desejos e símbolos mentais até encontrar a comunicação que faz o mercado se mover.

---

## 2. Visão Conceitual

A arquitetura desejada deve seguir esta hierarquia:

```text
MarketNiche
  └── Hypothesis
        ├── HypothesisSymbolicProfile
        ├── CommunicationConcept A
        │     └── Experiment A
        │           ├── Creative
        │           ├── LandingPage
        │           ├── LandingPageSectionMetrics
        │           └── MarketPsychologicalScore
        ├── CommunicationConcept B
        │     └── Experiment B
        │           ├── Creative
        │           ├── LandingPage
        │           ├── LandingPageSectionMetrics
        │           └── MarketPsychologicalScore
        └── CommunicationConcept C
              └── Experiment C
                    ├── Creative
                    ├── LandingPage
                    ├── LandingPageSectionMetrics
                    └── MarketPsychologicalScore
```

O objetivo é permitir que o sistema responda perguntas como:

- Qual símbolo mental este produto representa?
- Qual dor ele promete evitar?
- Qual prazer ele promete alcançar?
- Qual identidade futura ele oferece?
- Qual conceito de comunicação traduz melhor essa hipótese?
- Qual seção da página gera maior interesse?
- Em qual seção a persuasão quebra?
- A pessoa se reconhece na dor?
- A pessoa acredita no mecanismo?
- A oferta gera valor ou fricção?

---

## 3. Conceitos Principais

### 3.1 HypothesisSymbolicProfile

Representa a leitura simbólica e psicológica de uma hipótese.

Ele responde:

- O que essa hipótese representa para o consumidor?
- Qual dor ela evita?
- Qual prazer ela promete?
- Qual identidade futura ela vende?
- Qual inimigo mental ela combate?

Exemplo:

```json
{
  "mentalSymbol": "controle",
  "functionalMeaning": "Ajuda o concurseiro a estudar com constância",
  "emotionalMeaning": "Alívio da culpa e da sensação de estar atrasado",
  "identityMeaning": "Pessoa disciplinada, no controle e com chance real de aprovação",
  "painAvoided": "Sentir-se travado, culpado e atrasado",
  "pleasureSought": "Sentir progresso, disciplina e confiança",
  "desiredFutureSelf": "Sou uma pessoa consistente",
  "avoidedFutureSelf": "Continuo tentando, mas nunca avanço",
  "enemy": "Depender de motivação"
}
```

---

### 3.2 CommunicationConcept

Representa um conceito de comunicação derivado da hipótese.

Cada experimento deve testar um conceito de comunicação específico, não apenas um anúncio ou criativo isolado.

Exemplo:

```json
{
  "name": "Pare de depender de motivação",
  "coreMessage": "Você não precisa estar motivado para estudar; precisa de um sistema que funcione mesmo nos dias ruins.",
  "mentalSymbol": "controle",
  "dominantEmotion": "alívio",
  "targetForce": "PAIN_AVOIDANCE",
  "beliefToBreak": "Preciso estar motivado para estudar",
  "beliefToInstall": "Preciso de um sistema que funcione mesmo sem motivação",
  "enemy": "motivação como pré-requisito"
}
```

Tipos possíveis de força psicológica:

```text
PAIN_AVOIDANCE
PLEASURE_PULL
IDENTITY_PULL
BELIEF_SHIFT
TRUST_BUILDING
ACTION_INTENT
```

---

### 3.3 LandingPageSectionMetric

Mede o comportamento do usuário em cada seção da página de venda.

Cada seção da landing page deve ter um papel psicológico claro.

Exemplo:

| Seção | Papel psicológico |
|---|---|
| Hero | Autoidentificação |
| Dor | Evitação da dor |
| Agitação | Intensidade da dor |
| Promessa | Busca de prazer |
| Mecanismo | Crença no caminho |
| Prova | Confiança |
| Oferta | Valor percebido |
| Garantia | Redução de objeção |
| FAQ | Remoção de dúvidas |
| CTA | Intenção de ação |

---

## 4. Modelo de Dados

### 4.1 Tabela: `hypothesis_symbolic_profile`

```sql
create table hypothesis_symbolic_profile (
  id bigint primary key auto_increment,
  hypothesis_id binary(16) not null,

  mental_symbol varchar(191),
  functional_meaning text,
  emotional_meaning text,
  identity_meaning text,

  pain_avoided text,
  pleasure_sought text,
  desired_future_self text,
  avoided_future_self text,
  enemy text,

  pain_avoidance_score decimal(5,2),
  pleasure_pull_score decimal(5,2),
  identity_pull_score decimal(5,2),
  symbolic_clarity_score decimal(5,2),

  created_at timestamp default current_timestamp,
  updated_at timestamp default current_timestamp
);
```

---

### 4.2 Tabela: `communication_concept`

```sql
create table communication_concept (
  id bigint primary key auto_increment,
  hypothesis_id binary(16) not null,
  experiment_id bigint,

  name varchar(255) not null,
  core_message varchar(500),

  mental_symbol varchar(191),
  dominant_emotion varchar(100),
  target_force varchar(50),

  pain_angle text,
  pleasure_angle text,
  identity_angle text,
  belief_to_break text,
  belief_to_install text,
  enemy text,

  created_at timestamp default current_timestamp,
  updated_at timestamp default current_timestamp
);
```

---

### 4.3 Tabela: `landing_page_event`

Tabela para registrar eventos brutos da página.

```sql
create table landing_page_event (
  id bigint primary key auto_increment,
  experiment_id bigint not null,
  landing_page_id bigint not null,

  session_id varchar(100),
  visitor_id varchar(100),

  event_type varchar(100) not null,
  section_key varchar(100),
  section_order int,

  time_on_section_seconds decimal(10,2),
  scroll_depth decimal(5,2),
  metadata json,

  created_at timestamp default current_timestamp
);
```

Eventos principais:

```text
page_view
section_view
section_engaged
section_completed
section_exit
section_reread
cta_click
form_start
form_submit
checkout_start
purchase
```

---

### 4.4 Tabela: `landing_page_section_metric`

Tabela agregada por seção.

```sql
create table landing_page_section_metric (
  id bigint primary key auto_increment,
  experiment_id bigint not null,
  landing_page_id bigint not null,

  section_key varchar(100) not null,
  symbolic_role varchar(100),
  target_force varchar(50),

  views bigint default 0,
  engaged_views bigint default 0,
  completed_views bigint default 0,
  exits bigint default 0,
  rereads bigint default 0,
  cta_clicks bigint default 0,

  avg_time_seconds decimal(10,2),
  engagement_rate decimal(5,2),
  completion_rate decimal(5,2),
  exit_rate decimal(5,2),
  cta_rate decimal(5,2),

  psychological_score decimal(5,2),
  friction_score decimal(5,2),

  created_at timestamp default current_timestamp,
  updated_at timestamp default current_timestamp
);
```

---

### 4.5 Tabela: `experiment_market_psychological_score`

Score final do experimento.

```sql
create table experiment_market_psychological_score (
  id bigint primary key auto_increment,
  experiment_id bigint not null,

  symbolic_score decimal(5,2),
  psychological_resonance_score decimal(5,2),
  market_interest_score decimal(5,2),
  conversion_intent_score decimal(5,2),
  economic_score decimal(5,2),

  final_score decimal(5,2),
  confidence_factor decimal(4,2),

  diagnosis text,
  recommended_actions json,

  created_at timestamp default current_timestamp,
  updated_at timestamp default current_timestamp
);
```

---

## 5. Instrumentação da Landing Page

Cada seção da página gerada deve receber atributos semânticos.

Exemplo:

```html
<section data-section-key="hero" data-symbolic-role="self_identification">
  ...
</section>

<section data-section-key="pain" data-symbolic-role="pain_avoidance">
  ...
</section>

<section data-section-key="promise" data-symbolic-role="pleasure_pull">
  ...
</section>

<section data-section-key="mechanism" data-symbolic-role="belief_shift">
  ...
</section>

<section data-section-key="offer" data-symbolic-role="action_intent">
  ...
</section>
```

### Tracking sugerido

Usar `IntersectionObserver` no frontend da landing page para detectar:

- Quando a seção entra na tela.
- Quanto tempo o usuário permanece na seção.
- Se ele chega ao final da seção.
- Se ele volta para reler a seção.
- Se ele sai da página naquela seção.
- Se ele clica no CTA a partir daquela seção.

Eventos podem ser enviados em lote para reduzir custo:

```text
POST /api/landing-pages/{id}/events/batch
```

---

## 6. Fórmulas de Score

### 6.1 Score por seção

```text
section_score =
  engagement_rate * 30
+ completion_rate * 25
+ healthy_time_score * 20
+ cta_or_next_section_rate * 15
+ reread_score * 10
- exit_rate_penalty
```

### 6.2 Psychological Resonance Score

```text
psychological_resonance_score =
  self_identification * 0.15
+ pain_avoidance * 0.20
+ pleasure_pull * 0.20
+ belief_shift * 0.15
+ trust * 0.10
+ value_perception * 0.10
+ action_intent * 0.10
```

### 6.3 Market-Psychological Fit Score

```text
final_score =
  symbolic_score * 0.20
+ psychological_resonance_score * 0.30
+ market_interest_score * 0.20
+ conversion_intent_score * 0.20
+ economic_score * 0.10
```

---

## 7. Travas de Realidade

Para o score não ficar abstrato, aplicar limites máximos conforme evidência disponível.

```text
Sem tráfego real → score máximo 30
Sem tracking por seção → score máximo 45
Sem lead → score máximo 60
Sem clique em CTA → score máximo 70
Sem venda → score máximo 80
Com venda e margem positiva → pode chegar a 100
```

Também aplicar fator de confiança:

```text
0.30 = hipótese sem experimento
0.45 = experimento criado, sem dados suficientes
0.60 = tráfego inicial
0.75 = leads e comportamento por seção
0.90 = conversão comercial inicial
1.00 = vendas, CAC, margem e amostra confiável
```

---

## 8. Backend

Criar pacote:

```text
backend/ads-service/src/main/java/com/marketinghub/psychometrics
```

### Entidades

```text
HypothesisSymbolicProfile.java
CommunicationConcept.java
LandingPageEvent.java
LandingPageSectionMetric.java
ExperimentMarketPsychologicalScore.java
```

### Repositories

```text
HypothesisSymbolicProfileRepository.java
CommunicationConceptRepository.java
LandingPageEventRepository.java
LandingPageSectionMetricRepository.java
ExperimentMarketPsychologicalScoreRepository.java
```

### Services

```text
SymbolicProfileService
CommunicationConceptService
LandingPageTrackingService
LandingPageSectionMetricService
PsychologicalScoreService
MarketPsychologicalScoreService
```

### Controllers

Criar controller separado para não inflar `ExperimentController`:

```text
ExperimentPsychometricsController
HypothesisSymbolicProfileController
CommunicationConceptController
LandingPageTrackingController
```

Endpoints sugeridos:

```text
GET  /api/hypotheses/{id}/symbolic-profile
POST /api/hypotheses/{id}/symbolic-profile
PUT  /api/hypotheses/{id}/symbolic-profile

GET  /api/hypotheses/{id}/communication-concepts
POST /api/hypotheses/{id}/communication-concepts
PUT  /api/communication-concepts/{id}

POST /api/landing-pages/{id}/events
POST /api/landing-pages/{id}/events/batch

GET  /api/landing-pages/{id}/section-metrics
POST /api/landing-pages/{id}/section-metrics/recalculate

GET  /api/experiments/{id}/psychological-score
POST /api/experiments/{id}/psychological-score/recalculate
```

---

## 9. Frontend

Criar novas telas/componentes:

```text
frontend/src/pages/psychometrics/HypothesisSymbolicProfilePage.tsx
frontend/src/pages/psychometrics/CommunicationConceptsPage.tsx
frontend/src/pages/experiment/PsychologicalScoreTab.tsx
frontend/src/pages/experiment/LandingSectionMetricsTab.tsx
```

### Aba no experimento

Adicionar uma aba:

```text
Psicologia / Ressonância
```

Exemplo de visualização:

```text
Market-Psychological Fit Score: 72/100

Símbolo mental: controle
Dor evitada: culpa, atraso e sensação de fracasso
Prazer buscado: progresso, disciplina e confiança

Score simbólico: 78
Ressonância psicológica: 74
Interesse de mercado: 68
Intenção de conversão: 59
Economia: 70
```

### Tabela por seção

| Seção | Papel psicológico | Score | Diagnóstico |
|---|---|---:|---|
| Hero | Autoidentificação | 82 | Forte reconhecimento inicial |
| Dor | Evitação da dor | 88 | Dor muito clara |
| Promessa | Busca de prazer | 64 | Desejo moderado |
| Mecanismo | Crença | 51 | Precisa explicar melhor |
| Oferta | Ação | 39 | Fricção alta |

---

## 10. AI Worker

Depois do MVP manual, adicionar rotinas ao AI Worker.

### 10.1 SymbolicProfileScheduler

Fluxo:

```text
1. Busca hipóteses sem symbolic_profile
2. Lê persona, problem, promise, mechanism, uniqueMechanism e frameworkJson
3. Gera mental_symbol, pain_avoided, pleasure_sought, desired_future_self e enemy
4. Salva HypothesisSymbolicProfile
```

### 10.2 PsychologicalDiagnosisScheduler

Fluxo:

```text
1. Busca experimentos com novas métricas de seção
2. Calcula score quantitativo
3. Envia os dados para IA gerar diagnóstico textual
4. Salva diagnosis e recommended_actions
```

Regra importante:

> A IA não deve inventar a nota. A IA explica a nota, identifica gargalos e sugere ações.

---

## 11. Diagnósticos Possíveis

O sistema deve gerar diagnósticos objetivos.

### Exemplo 1 — Falha no mecanismo

```json
{
  "problem": "Alta leitura da promessa, mas queda forte na seção de mecanismo.",
  "interpretation": "A pessoa deseja o resultado, mas não acredita no caminho apresentado.",
  "recommendedActions": [
    "Explicar o mecanismo com mais clareza",
    "Adicionar exemplo concreto",
    "Adicionar prova antes da oferta",
    "Reduzir termos abstratos"
  ]
}
```

### Exemplo 2 — Falha na oferta

```json
{
  "problem": "Boa retenção até a oferta, mas baixa taxa de CTA.",
  "interpretation": "A comunicação gera interesse, mas a oferta não justifica valor suficiente.",
  "recommendedActions": [
    "Reforçar entregáveis",
    "Melhorar ancoragem de preço",
    "Adicionar garantia",
    "Adicionar prova social próxima ao CTA"
  ]
}
```

### Exemplo 3 — Dor fraca

```json
{
  "problem": "Baixo engajamento na seção de dor.",
  "interpretation": "A pessoa não está se reconhecendo no problema descrito.",
  "recommendedActions": [
    "Tornar a dor mais específica",
    "Usar linguagem mais próxima da persona",
    "Adicionar situações concretas do cotidiano",
    "Testar outro ângulo de dor"
  ]
}
```

---

## 12. Sequência de Implementação

### Sprint 1 — Perfil simbólico e conceito de comunicação

- Criar tabelas `hypothesis_symbolic_profile` e `communication_concept`.
- Criar entidades JPA.
- Criar repositories.
- Criar services básicos.
- Criar endpoints CRUD.
- Criar tela simples para preencher perfil simbólico.

### Sprint 2 — Tracking da landing page

- Adicionar `data-section-key` e `data-symbolic-role` no HTML gerado.
- Criar tabela `landing_page_event`.
- Criar endpoint `/events/batch`.
- Criar script de tracking com `IntersectionObserver`.
- Registrar eventos de seção.

### Sprint 3 — Métricas por seção

- Criar tabela `landing_page_section_metric`.
- Criar agregador de eventos por seção.
- Calcular engagement rate, completion rate, exit rate e CTA rate.
- Criar primeira versão do `psychological_score` por seção.
- Criar tela de métricas por seção.

### Sprint 4 — Score psicológico do experimento

- Criar tabela `experiment_market_psychological_score`.
- Implementar fórmula de score final.
- Combinar score psicológico com métricas de campanha.
- Aplicar travas de realidade e fator de confiança.
- Criar endpoint de recálculo.
- Criar aba “Psicologia / Ressonância” no experimento.

### Sprint 5 — IA para perfil e diagnóstico

- Criar `SymbolicProfileScheduler` no AI Worker.
- Criar prompt para gerar perfil simbólico da hipótese.
- Criar `PsychologicalDiagnosisScheduler`.
- Gerar diagnóstico textual e ações recomendadas.
- Salvar `diagnosis` e `recommended_actions`.

### Sprint 6 — Comparação entre conceitos

- Criar ranking de conceitos por hipótese.
- Mostrar melhor conceito de comunicação.
- Mostrar maior gargalo da página.
- Comparar dor, prazer, mecanismo, prova e oferta entre experimentos.
- Criar dashboard por hipótese.

---

## 13. MVP Recomendado

O primeiro MVP deve conter apenas o essencial:

```text
1. Perfil simbólico da hipótese
2. Conceito de comunicação por experimento
3. Tracking por seção da landing page
4. Score psicológico por seção
5. Diagnóstico simples do gargalo
```

Com isso, o sistema já poderá responder:

- A pessoa se reconheceu?
- A dor mexeu com ela?
- A promessa gerou desejo?
- O mecanismo convenceu?
- A oferta travou?
- O CTA moveu?

---

## 14. Resultado Esperado

Ao final da implementação, o Marketing Hub passará a medir não apenas performance de mídia, mas também **resposta psicológica do mercado**.

O sistema poderá indicar:

- Qual hipótese tem maior força simbólica.
- Qual conceito de comunicação mais se conecta à mente do consumidor.
- Qual seção da página gera mais interesse.
- Qual seção causa abandono.
- Se o produto está vendendo prazer, alívio, controle, status, pertencimento ou transformação.
- Se a comunicação falha por dor fraca, promessa fraca, mecanismo pouco crível, prova insuficiente ou oferta mal posicionada.

A evolução estratégica é:

> O Marketing Hub deixa de ser apenas uma ferramenta de geração e teste de campanhas e passa a ser um sistema de descoberta de símbolos, desejos, dores e crenças que movem mercados.

