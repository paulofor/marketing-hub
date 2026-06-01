# Plano de Implantação — EPM / Experiment Profit Manager

## 1. Objetivo deste documento

Este documento orienta a implantação incremental do **EPM — Experiment Profit Manager** dentro do Marketing Hub.

O EPM é o módulo responsável por planejar, controlar e orientar financeiramente os experimentos de validação de produtos digitais.

A implantação deve priorizar um **MVP manual e confiável**, antes de qualquer integração automática com Meta Ads, checkout, Lead Portal ou eventos externos.

O objetivo inicial é simples:

```text
Permitir que o Marketing Hub saiba quanto pode gastar,
quanto já gastou,
quantas vendas precisa para empatar,
e quais experimentos ou hipóteses devem continuar, iterar ou morrer.
```

---

## 2. Princípios de implantação

A implementação deve seguir os princípios gerais do Marketing Hub:

- backend como fonte de verdade;
- MySQL 5.7;
- migrations via Liquibase;
- Java 21 + Spring Boot 3 no backend principal;
- frontend React + TypeScript consumindo contratos do backend;
- implementação incremental;
- evitar integrações externas na primeira versão;
- evitar acoplamento forte com outros módulos no MVP;
- manter rastreabilidade de decisões;
- manter o foco em vendas e lucro, não em contabilidade completa.

O EPM não deve nascer como um sistema financeiro genérico. Ele deve nascer como um **gestor de lucro e perda por experimento**.

---

## 3. Escopo do MVP

### 3.1. Dentro do escopo inicial

O MVP deve permitir:

- criar um plano financeiro de ciclo/mês;
- adicionar nichos ao plano;
- adicionar hipóteses aos nichos;
- criar orçamentos de experimentos;
- informar métricas manualmente;
- calcular custo, receita, lucro bruto e lucro líquido estimado;
- simular ponto de equilíbrio por preço de produto;
- registrar decisão financeira do experimento;
- listar experimentos que devem continuar, iterar ou morrer.

### 3.2. Fora do escopo inicial

Não implementar na primeira versão:

- integração automática com Meta Ads;
- integração automática com Google Ads;
- integração automática com checkout;
- captura automática de eventos do Lead Portal;
- automação completa de decisões;
- filas ou workers próprios para o EPM;
- contabilidade fiscal;
- conciliação financeira;
- emissão de nota fiscal;
- apuração tributária real.

---

## 4. Estratégia de implantação

A implantação será dividida em sprints curtas:

```text
Sprint 1 — Backend mínimo e banco
Sprint 2 — API operacional do EPM
Sprint 3 — Frontend manual do EPM
Sprint 4 — Integração com experimentos existentes
Sprint 5 — Eventos financeiros e regras de decisão
Sprint 6 — Integrações futuras e automação controlada
```

A primeira entrega útil deve acontecer até o final da Sprint 3: uma tela onde seja possível planejar e acompanhar os experimentos manualmente.

---

## 5. Sprint 1 — Backend mínimo e banco

### 5.1. Objetivo

Criar a base persistente do EPM no backend principal.

Nesta sprint, o foco é banco, entidades, repositories e testes básicos.

### 5.2. Tabelas da Sprint 1

Implementar somente:

```text
financial_plan
financial_plan_niche
financial_plan_hypothesis
experiment_budget
experiment_financial_metric
experiment_financial_decision
product_price_scenario
```

Não implementar ainda:

```text
experiment_financial_event
financial_rule
```

Essas tabelas entram depois, quando o módulo precisar receber eventos externos e aplicar regras configuráveis.

### 5.3. Migration Liquibase

Criar migration em YAML, compatível com MySQL 5.7.

Regras:

- usar `databaseChangeLog`;
- usar `preConditions` com `dbms:mysql`;
- usar `splitStatements: true`;
- usar `stripComments: true`;
- usar tipos simples e compatíveis com MySQL 5.7;
- guardar valores monetários em centavos (`BIGINT`);
- evitar `JSON` no MVP para manter compatibilidade ampla;
- usar `TEXT` quando necessário.

### 5.4. Entidades Java

Criar entidades no backend principal, em pacote próprio do EPM.

Sugestão de pacote:

```text
com.marketinghub.epm
```

Ou, se o backend já possuir convenção modular por domínio, seguir a convenção existente.

Entidades iniciais:

```text
FinancialPlan
FinancialPlanNiche
FinancialPlanHypothesis
ExperimentBudget
ExperimentFinancialMetric
ExperimentFinancialDecision
ProductPriceScenario
```

Cada classe deve ter comentário de responsabilidade básica.
Cada método criado ou alterado deve ter comentário breve explicando o que faz, conforme regra operacional do projeto.

### 5.5. Repositories

Criar repositories para as entidades principais.

Sugestão:

```text
FinancialPlanRepository
FinancialPlanNicheRepository
FinancialPlanHypothesisRepository
ExperimentBudgetRepository
ExperimentFinancialMetricRepository
ExperimentFinancialDecisionRepository
ProductPriceScenarioRepository
```

### 5.6. Testes da Sprint 1

Criar testes mínimos para:

- validar criação de entidades;
- validar persistência básica;
- validar relacionamento entre plano, nicho, hipótese e experimento;
- validar que valores monetários são armazenados em centavos.

### 5.7. Critérios de aceite

A Sprint 1 estará concluída quando:

- migrations executarem sem erro;
- entidades existirem com responsabilidade clara;
- repositories existirem;
- testes básicos passarem;
- não houver dependência de Meta Ads, checkout ou frontend.

---

## 6. Sprint 2 — API operacional do EPM

### 6.1. Objetivo

Criar endpoints mínimos para operar o EPM manualmente.

### 6.2. Endpoints sugeridos

#### Planos financeiros

```text
POST /api/epm/plans
GET  /api/epm/plans
GET  /api/epm/plans/{planId}
PUT  /api/epm/plans/{planId}
```

#### Nichos do plano

```text
POST /api/epm/plans/{planId}/niches
GET  /api/epm/plans/{planId}/niches
GET  /api/epm/niches/{planNicheId}
```

#### Hipóteses do nicho

```text
POST /api/epm/niches/{planNicheId}/hypotheses
GET  /api/epm/niches/{planNicheId}/hypotheses
GET  /api/epm/hypotheses/{planHypothesisId}
```

#### Experimentos da hipótese

```text
POST /api/epm/hypotheses/{planHypothesisId}/experiments
GET  /api/epm/hypotheses/{planHypothesisId}/experiments
GET  /api/epm/experiments/{experimentBudgetId}
PUT  /api/epm/experiments/{experimentBudgetId}
```

#### Métricas manuais

```text
POST /api/epm/experiments/{experimentBudgetId}/metrics
GET  /api/epm/experiments/{experimentBudgetId}/metrics/latest
```

#### Decisões

```text
POST /api/epm/experiments/{experimentBudgetId}/decisions
GET  /api/epm/experiments/{experimentBudgetId}/decisions
```

#### Cenários de preço

```text
POST /api/epm/plans/{planId}/price-scenarios
GET  /api/epm/plans/{planId}/price-scenarios
```

#### Resumo do plano

```text
GET /api/epm/plans/{planId}/summary
```

### 6.3. DTOs sugeridos

Criar DTOs específicos para requests e responses.

Requests:

```text
CreateFinancialPlanRequest
UpdateFinancialPlanRequest
CreatePlanNicheRequest
CreatePlanHypothesisRequest
CreateExperimentBudgetRequest
CreateExperimentMetricRequest
CreateExperimentDecisionRequest
CreateProductPriceScenarioRequest
```

Responses:

```text
FinancialPlanResponse
FinancialPlanNicheResponse
FinancialPlanHypothesisResponse
ExperimentBudgetResponse
ExperimentMetricResponse
ExperimentDecisionResponse
ProductPriceScenarioResponse
FinancialPlanSummaryResponse
```

### 6.4. Services

Criar serviços de aplicação:

```text
FinancialPlanService
FinancialPlanNicheService
FinancialPlanHypothesisService
ExperimentBudgetService
ExperimentMetricService
ExperimentDecisionService
ProductPriceScenarioService
FinancialSummaryService
```

### 6.5. Cálculos mínimos

O backend deve calcular:

```text
plannedTotalBudget = plannedDailyBudget * plannedDurationDays
remainingBudget = spendLimit - actualSpend
grossProfit = revenue - adSpend
estimatedNetProfit = revenue - adSpend - paymentFees - platformFees - aiCost - taxEstimate
ctr = clicks / impressions
cpc = adSpend / clicks
cpl = adSpend / leads
cpa = adSpend / purchases
roas = revenue / adSpend
landingConversion = leads / visitors
purchaseConversion = purchases / visitors
breakEvenSales = ceil(totalBudget / expectedNetRevenuePerSale)
```

Divisões por zero devem retornar `null` ou zero conforme contrato definido, sem lançar erro inesperado.

### 6.6. Critérios de aceite

A Sprint 2 estará concluída quando:

- endpoints mínimos existirem;
- DTOs existirem;
- services existirem;
- cálculos principais funcionarem;
- testes de service/controller cobrirem os fluxos principais;
- o EPM puder ser operado via API sem frontend.

---

## 7. Sprint 3 — Frontend manual do EPM

### 7.1. Objetivo

Criar a primeira interface do EPM no frontend.

A tela pode ser simples. O objetivo é operação, não sofisticação visual.

### 7.2. Menu

Adicionar item de menu:

```text
EPM
```

Nome visível:

```text
Lucro dos Experimentos
```

### 7.3. Rotas sugeridas

```text
/epm
/epm/plans/:planId
/epm/plans/:planId/simulator
/epm/experiments/:experimentBudgetId
```

### 7.4. Abas da tela principal

A tela do plano pode ter quatro abas:

```text
Visão Geral
Planejamento
Experimentos
Simulador
Decisões
```

### 7.5. Visão Geral

Mostrar:

- orçamento total;
- gasto total;
- receita total;
- lucro bruto;
- lucro líquido estimado;
- nichos ativos;
- hipóteses ativas;
- experimentos rodando;
- experimentos lucrativos;
- experimentos sem sinal;
- hipóteses para reformular.

### 7.6. Planejamento

Permitir:

- criar plano financeiro;
- configurar período;
- configurar orçamento total;
- configurar orçamento diário;
- configurar verba padrão por experimento;
- configurar duração padrão;
- configurar quantidade de experimentos por hipótese;
- adicionar nichos;
- adicionar hipóteses.

### 7.7. Experimentos

Permitir:

- criar orçamento de experimento;
- informar gasto real;
- informar visitantes;
- informar leads;
- informar pedidos de amostra;
- informar cliques no checkout;
- informar vendas;
- informar receita;
- informar taxas e custo de IA estimado;
- visualizar métricas calculadas;
- registrar decisão.

### 7.8. Simulador

Permitir simular:

- preço do produto;
- taxa de pagamento;
- taxa estimada;
- custo fixo por venda;
- orçamento total;
- meta de lucro.

Mostrar:

- receita líquida por venda;
- vendas para empatar;
- vendas para atingir lucro alvo.

### 7.9. Decisões

Mostrar agrupamentos:

```text
Experimentos para matar
Experimentos para iterar
Hipóteses para reformular
Produtos com sinal
Produtos prontos para escala controlada
```

### 7.10. Critérios de aceite

A Sprint 3 estará concluída quando:

- usuário conseguir criar um plano;
- usuário conseguir adicionar nichos e hipóteses;
- usuário conseguir criar experimentos;
- usuário conseguir informar métricas manualmente;
- usuário conseguir ver cálculos de lucro/prejuízo;
- usuário conseguir registrar decisão;
- usuário conseguir usar o simulador de ponto de equilíbrio.

---

## 8. Sprint 4 — Integração com experimentos existentes

### 8.1. Objetivo

Conectar o EPM aos experimentos reais do Marketing Hub sem criar acoplamento excessivo.

### 8.2. Referências externas

O EPM deve poder referenciar, quando existirem:

```text
nicheId
hypothesisId
experimentId
landingId
campaignId
```

Esses campos devem ser opcionais no início.

### 8.3. Fluxo desejado

Quando um experimento comercial for criado no Marketing Hub, o usuário deve poder associá-lo a um orçamento do EPM.

Fluxo:

```text
Hipótese comercial existente
  → Criar experimento
  → Criar ou associar orçamento EPM
  → Rodar campanha
  → Informar métricas
  → Registrar decisão
```

### 8.4. Critérios de aceite

A Sprint 4 estará concluída quando:

- um experimento existente puder ser associado ao EPM;
- a tela do EPM mostrar link ou referência ao experimento;
- a tela do experimento mostrar resumo financeiro básico;
- não houver dependência obrigatória de integração externa.

---

## 9. Sprint 5 — Eventos financeiros e regras de decisão

### 9.1. Objetivo

Adicionar base para automação futura.

### 9.2. Tabelas desta sprint

Implementar:

```text
experiment_financial_event
financial_rule
```

### 9.3. Eventos financeiros

Eventos devem permitir registrar ocorrências como:

```text
AD_SPEND
IMPRESSION
CLICK
LANDING_VISIT
FORM_START
LEAD
SAMPLE_REQUEST
CHECKOUT_CLICK
PURCHASE
REFUND
PAYMENT_FEE
PLATFORM_FEE
AI_COST
TAX_ESTIMATE
MANUAL_ADJUSTMENT
```

Fontes:

```text
META_ADS
GOOGLE_ADS
LEAD_PORTAL
CHECKOUT
AI_WORKER
MANUAL
SYSTEM
```

### 9.4. Regras de decisão

Criar regras simples como configuração persistida.

Exemplos:

```text
IF leads = 0 AND spend >= planned_total_budget THEN KILL_EXPERIMENT
IF leads > 0 AND purchases = 0 THEN ITERATE_OFFER
IF revenue > ad_spend THEN SCALE_CONTROLLED
IF 3 experiments failed AND total_leads = 0 THEN KILL_HYPOTHESIS
IF roas >= 2 THEN SCALE_CONTROLLED
```

### 9.5. Critérios de aceite

A Sprint 5 estará concluída quando:

- eventos puderem ser registrados manualmente ou por API;
- métricas puderem ser recalculadas a partir dos eventos;
- regras financeiras simples puderem sugerir decisões;
- nenhuma decisão automática for executada sem registro e visibilidade.

---

## 10. Sprint 6 — Integrações futuras e automação controlada

### 10.1. Objetivo

Preparar o EPM para receber dados reais de plataformas externas.

### 10.2. Integrações futuras possíveis

- Meta Ads;
- Google Ads;
- checkout/pagamentos;
- Lead Portal;
- AI Worker;
- pixel/eventos de landing;
- dashboard de portfólio de produtos digitais.

### 10.3. Ordem recomendada

A ordem mais segura é:

```text
1. Lead Portal / landing events
2. Checkout / vendas
3. Meta Ads / gasto e campanha
4. AI Worker / custos de geração
5. Regras automáticas de decisão
```

### 10.4. Critérios de aceite

A Sprint 6 só deve começar quando:

- MVP manual estiver útil;
- fluxo de decisão estiver claro;
- campos externos necessários estiverem definidos;
- não houver inconsistência entre experimento, landing, checkout e EPM.

---

## 11. Cálculos obrigatórios

### 11.1. Custo planejado do experimento

```text
planned_total_budget_cents = planned_daily_budget_cents * planned_duration_days
```

### 11.2. Orçamento restante

```text
remaining_budget_cents = spend_limit_cents - actual_spend_cents
```

### 11.3. Lucro bruto

```text
gross_profit_cents = revenue_cents - ad_spend_cents
```

### 11.4. Lucro líquido estimado

```text
estimated_net_profit_cents =
  revenue_cents
  - ad_spend_cents
  - payment_fee_cents
  - platform_fee_cents
  - ai_cost_cents
  - tax_estimate_cents
```

### 11.5. ROAS

```text
roas_decimal = revenue_cents / ad_spend_cents
```

### 11.6. CPL

```text
cpl_cents = ad_spend_cents / leads
```

### 11.7. CPA

```text
cpa_cents = ad_spend_cents / purchases
```

### 11.8. Conversão da landing

```text
landing_conversion_decimal = leads / visitors
```

### 11.9. Conversão de compra

```text
purchase_conversion_decimal = purchases / visitors
```

### 11.10. Ponto de equilíbrio

```text
break_even_sales = ceil(total_budget_cents / expected_net_revenue_per_sale_cents)
```

---

## 12. Regras de decisão iniciais

### 12.1. Experimento sem sinal

Se o experimento gastou o orçamento planejado e não gerou lead, pedido de amostra, clique no checkout ou venda:

```text
Decision: KILL_EXPERIMENT
```

### 12.2. Experimento com lead, mas sem venda

Se houve leads ou pedidos de amostra, mas nenhuma compra:

```text
Decision: ITERATE_OFFER ou ITERATE_LANDING
```

A escolha entre landing e oferta deve considerar:

- taxa de conversão da landing;
- cliques no checkout;
- clareza da promessa;
- qualidade da amostra inicial.

### 12.3. Experimento com venda e prejuízo controlado

Se houve venda, mas ainda com prejuízo:

```text
Decision: CONTINUE ou ITERATE_CREATIVE
```

O sistema não deve matar automaticamente um experimento que gerou compra real.

### 12.4. Experimento lucrativo

Se receita maior que gasto e há lucro bruto positivo:

```text
Decision: SCALE_CONTROLLED
```

### 12.5. Hipótese sem sinal após 3 experimentos

Se 3 experimentos bem executados não geraram sinal relevante:

```text
Decision: REFORMULATE_HYPOTHESIS ou KILL_HYPOTHESIS
```

---

## 13. Pacote inicial para o Codex — Sprint 1

Prompt recomendado para o Codex:

```text
Implemente a Sprint 1 do módulo EPM — Experiment Profit Manager no backend principal do Marketing Hub.

Objetivo:
Criar a base persistente do EPM com migrations Liquibase, entidades JPA, repositories e testes básicos.

Escopo da Sprint 1:
- financial_plan
- financial_plan_niche
- financial_plan_hypothesis
- experiment_budget
- experiment_financial_metric
- experiment_financial_decision
- product_price_scenario

Não implementar ainda:
- frontend
- controllers
- endpoints públicos
- experiment_financial_event
- financial_rule
- integração com Meta Ads
- integração com checkout
- integração com Lead Portal

Regras obrigatórias:
- seguir MySQL 5.7;
- usar Liquibase YAML;
- valores monetários em centavos usando BIGINT;
- criar entidades com responsabilidade única;
- adicionar comentário de responsabilidade em toda classe Java;
- adicionar comentário breve em métodos novos;
- criar repositories;
- criar testes básicos de persistência;
- não alterar ArquiteturaTest salvo se for explicitamente necessário e justificado.

Critério de aceite:
- migrations executam sem erro;
- entidades e repositories existem;
- testes passam;
- não existe dependência de plataformas externas.
```

---

## 14. Pacote inicial para o Codex — Sprint 2

Prompt recomendado para o Codex após concluir a Sprint 1:

```text
Implemente a Sprint 2 do módulo EPM — API operacional manual.

Objetivo:
Criar services, DTOs e controllers para operar o EPM manualmente via backend.

Escopo:
- criar plano financeiro;
- listar planos;
- consultar plano por id;
- adicionar nicho ao plano;
- adicionar hipótese ao nicho;
- criar orçamento de experimento;
- registrar métricas manuais;
- registrar decisão;
- criar cenários de preço;
- consultar resumo do plano.

Regras:
- não integrar com plataformas externas;
- calcular métricas no backend;
- tratar divisão por zero sem erro inesperado;
- manter contratos simples;
- criar testes de service/controller;
- seguir comentários obrigatórios em classes e métodos Java.
```

---

## 15. Riscos e cuidados

### 15.1. Risco: virar contabilidade completa

Mitigação:

```text
Manter foco em decisão de experimento, não em contabilidade fiscal.
```

### 15.2. Risco: integrar Meta Ads cedo demais

Mitigação:

```text
Primeiro operar manualmente; integrar só depois de validar o fluxo.
```

### 15.3. Risco: acoplar demais ao experimento atual

Mitigação:

```text
Usar referências opcionais para nicheId, hypothesisId e experimentId.
```

### 15.4. Risco: matar hipóteses por amostra insuficiente

Mitigação:

```text
Permitir status INCONCLUSIVE quando o teste não teve tráfego ou dados suficientes.
```

### 15.5. Risco: decisões automáticas perigosas

Mitigação:

```text
Na primeira versão, decisões são registradas e sugeridas, mas não executam ações destrutivas automaticamente.
```

---

## 16. Resultado esperado do MVP

Ao final do MVP, o usuário deve conseguir responder rapidamente:

```text
Quanto estou disposto a gastar neste mês?
Quanto cada nicho pode consumir?
Quanto cada hipótese pode perder?
Quanto custa testar 3 experimentos?
Quantas vendas preciso para empatar?
Qual experimento deu sinal?
Qual experimento queimou dinheiro?
Qual hipótese deve ser reformulada?
Qual produto merece escala controlada?
```

Se o EPM responder essas perguntas com clareza, a primeira versão já estará cumprindo seu papel na fábrica de produtos digitais do Marketing Hub.
