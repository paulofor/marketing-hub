# Plano mestre — evolução de funis, produtos e personalização

## 1. Metadados

- **Tema:** Experimentos comerciais e venda de infoprodutos
- **Status:** Proposta de implementação
- **Prioridade:** Alta
- **Data:** 24/06/2026
- **Módulos envolvidos:** Backend, Frontend, Facebook Ads Worker, AI Worker, Lead Portal, Email Service e Lead Portal Payments Service
- **Documento canônico relacionado:** `docs/canonical/procedimento-experimento-canon.v1.md`
- **Documentos complementares:**
  - `docs/implementacao/experimentos/especificacao-centro-de-decisao-frontend.md`
  - `docs/implementacao/experimentos/especificacao-pipeline-decisao-ia-v1.md`

---

## 2. Objetivo

Evoluir o Marketing Hub para testar e operar diferentes rotas comerciais sem misturar hipóteses, métricas ou responsabilidades arquiteturais.

O sistema deverá suportar:

1. captura por Meta Instant Form;
2. captura por landing própria;
3. venda direta de produto genérico low-ticket;
4. captura inicial seguida de enriquecimento do perfil;
5. amostra genérica;
6. amostra personalizada;
7. produto genérico;
8. produto personalizado por lead;
9. fallback de personalização para oferta genérica;
10. comparação controlada entre rotas;
11. cálculo de margem e qualidade do funil;
12. recomendação de próximo movimento usando modelos de IA;
13. decisão humana auditável;
14. automação gradual somente após políticas explícitas e dados confiáveis.

O objetivo final não é maximizar leads. É encontrar rotas que gerem **vendas, margem de contribuição e valor percebido**, preservando o eixo:

```text
Dor → Resultado → Mecanismo → Prova → Oferta
```

---

## 3. Diagnóstico do estado atual

### 3.1 Capacidades existentes que devem ser preservadas

O sistema já possui bases importantes:

- `Experiment` vincula nicho, hipótese, Meta Instant Form, Lead Portal, jornada, preço, metas, orçamento e ativos do pipeline;
- existe contrato de uma dor, uma recompensa gratuita, uma promessa e um CTA por experimento;
- o Facebook Ads Worker consegue publicar para destino `ON_AD` ou `WEBSITE`;
- o motor de jornadas suporta Instant Form, landing, Lead Portal, e-mail, WhatsApp e pagamento;
- o funil atual acompanha anúncio, formulário, amostra, checkout, compra e entrega;
- o backend já gera snapshots para relatórios e aprendizado automático;
- o AI Worker já processa solicitações de aprendizado do experimento;
- o Lead Portal já suporta submissão, geração de pacotes, amostra, compra e entrega.

Esses componentes não devem ser substituídos. A evolução deve adicionar contratos claros e reduzir ambiguidades.

### 3.2 Lacunas atuais

As principais lacunas são:

1. `Experiment` não declara formalmente qual rota comercial está testando;
2. produto, oferta, amostra e personalização ainda aparecem misturados em campos e artefatos diferentes;
3. não existe uma entidade superior para comparar experimentos atômicos;
4. a captura Meta atual não persiste um perfil completo de contato e consentimento;
5. o lead não possui identidade consolidada entre Meta, landing, e-mail, checkout e entrega;
6. os eventos comerciais estão distribuídos entre tabelas de origem e projeções do funil;
7. custos de IA, entrega, pagamento e reembolso não formam um ledger econômico único;
8. a tela mostra etapas e métricas, mas ainda não apresenta um pacote completo de decisão;
9. o aprendizado por IA atual é útil, porém ainda não possui pipeline versionado, schemas externos, evidências referenciadas e separação entre recomendação e ação;
10. a regra transitória de standby no primeiro envio conflita com experimentos comparativos que precisam acumular amostra.

### 3.3 Causa-raiz da confusão de produto e funil

O sistema passou a representar cada nova necessidade diretamente no `Experiment`: formulário, landing, recompensa, amostra, produto e checkout. Isso funcionou para validar um fluxo inicial, mas torna difícil combinar rotas sem criar estados implícitos.

A correção não é adicionar mais flags soltas ao experimento. A correção é separar explicitamente:

- estratégia do experimento;
- produto;
- oferta;
- personalização;
- identidade do lead;
- fatos comerciais;
- comparação;
- análise;
- decisão;
- ação.

---

## 4. Princípios arquiteturais obrigatórios

### 4.1 Backend como fonte de verdade

O backend principal deve ser proprietário de:

- contratos de domínio;
- status dos experimentos;
- estratégia comercial;
- comparação entre braços;
- identidade e consentimento do lead;
- eventos comerciais canônicos;
- custos e receitas;
- snapshots de decisão;
- fila e estado dos pipelines;
- recomendações persistidas;
- decisões humanas;
- comandos administrativos e histórico de ações.

Somente o backend acessa o MySQL.

### 4.2 Workers como executores

Os workers devem:

- buscar trabalho em endpoints `pending` oficiais;
- executar somente sua integração ou transformação;
- reportar status, artefatos e resultado ao backend;
- não escolher a próxima etapa;
- não consultar banco diretamente;
- não chamar outro worker ou serviço de apoio diretamente.

### 4.3 Orquestração exclusiva do backend

O backend deve decidir:

- qual etapa está pronta;
- se pré-requisitos foram atendidos;
- se uma execução pode avançar;
- quando uma recomendação pode ser solicitada;
- se uma ação administrativa é permitida;
- qual evento muda o estado do experimento.

O frontend dispara comandos e acompanha resultados. Não orquestra fluxo no navegador.

### 4.4 Artefatos e execução auditáveis

Todo pipeline novo deve persistir:

- `jobId` ou identificador equivalente;
- versão do pipeline;
- etapa;
- tentativa;
- status;
- início e fim;
- entrada estruturada;
- saída estruturada;
- evidências usadas;
- artefatos gerados;
- erro funcional ou técnico;
- modelo e provedor;
- request e response brutos da IA;
- tokens e custo quando disponíveis;
- decisão de avanço tomada pelo backend.

### 4.5 Dados funcionais separados de metadados técnicos

HTML, produto, amostra, e-mail ou payload público não podem carregar comentários internos, status de pipeline, prompts, IDs técnicos desnecessários ou JSON serializado dentro de texto funcional.

### 4.6 Compatibilidade incremental

A evolução deve:

- manter experimentos existentes legíveis;
- utilizar feature flags;
- permitir dual-read ou dual-write durante migração;
- não exigir migração destrutiva imediata;
- manter o aprendizado legado disponível até a nova decisão v1 estar validada;
- criar novas tabelas antes de remover campos existentes.

---

## 5. Decisões estruturais

## 5.1 Experimento permanece atômico

Um `Experiment` continuará representando uma única materialização comercial:

- uma dor principal;
- uma promessa principal;
- uma oferta principal;
- um CTA principal;
- uma rota comercial;
- uma variável primária;
- uma métrica primária.

Não serão colocados vários braços dentro do mesmo registro de experimento.

### Motivos

- campanha e ativos permanecem atribuídos de forma inequívoca;
- eventos não precisam adivinhar variante;
- cada experimento pode ser pausado ou encerrado separadamente;
- o contrato canônico de promessa única continua válido;
- relatórios existentes permanecem úteis;
- é possível comparar experimentos antigos no futuro.

## 5.2 Comparação será uma camada superior

Criar `ExperimentComparison` para agrupar experimentos comparáveis.

```text
ExperimentComparison
  ├── ExperimentComparisonArm → Experiment A
  └── ExperimentComparisonArm → Experiment B
```

A comparação deve declarar:

- pergunta de negócio;
- variável alterada;
- variáveis controladas;
- métrica primária;
- métricas de guarda;
- janela de observação;
- política de parada;
- critério de prontidão;
- braço de controle;
- braços desafiantes;
- hipótese de resultado esperado.

## 5.3 Estratégia comercial será explícita

Criar `ExperimentStrategy` em relação 1:1 com `Experiment`.

A estratégia declarará a rota, em vez de inferi-la pela combinação de campos.

Enums iniciais:

```text
entryRoute:
- META_INSTANT_FORM
- OWNED_LANDING
- DIRECT_SALES_PAGE
- LEGACY_UNCLASSIFIED

captureMode:
- NONE
- BASIC_CONTACT
- CONTACT_AND_QUALIFICATION

profileEnrichmentMode:
- NONE
- OPTIONAL_AFTER_CAPTURE
- REQUIRED_BEFORE_SAMPLE
- REQUIRED_BEFORE_PURCHASE

sampleMode:
- NONE
- GENERIC
- PERSONALIZED

productMode:
- GENERIC
- PERSONALIZED

fallbackMode:
- NONE
- GENERIC_SAMPLE
- GENERIC_OFFER

primaryConversionGoal:
- LEAD_CAPTURE
- PROFILE_COMPLETION
- SAMPLE_CONSUMPTION
- CHECKOUT_START
- PURCHASE
- PAID_DELIVERY

operatingMode:
- EARLY_SIGNAL
- CONTROLLED_COMPARISON
- SCALE
```

Campos propostos:

```text
experiment_strategy
- id
- experiment_id UNIQUE
- strategy_version
- entry_route
- capture_mode
- profile_enrichment_mode
- sample_mode
- product_mode
- fallback_mode
- primary_conversion_goal
- operating_mode
- decision_window_days
- enrichment_expiration_hours
- fallback_after_hours
- requires_email
- requires_phone
- requires_upload
- active
- created_at
- updated_at
```

### Regra de consistência

Exemplos de validação:

- `DIRECT_SALES_PAGE` não pode exigir captura antes do checkout;
- `PERSONALIZED` exige definição de personalização ativa;
- `META_INSTANT_FORM` exige formulário publicado e associação Meta válida;
- `OWNED_LANDING` exige landing/flow aprovado;
- `REQUIRED_BEFORE_SAMPLE` exige portal de enriquecimento publicado;
- `EARLY_SIGNAL` mantém standby no primeiro envio;
- `CONTROLLED_COMPARISON` usa política de parada própria e não pode ser encerrado pelo primeiro lead sem justificativa de segurança ou orçamento.

## 5.4 Produto e oferta serão entidades diferentes

### Produto

Produto representa o que será entregue.

Evoluir `Product` com:

```text
- name
- product_mode
- fulfillment_mode
- delivery_format
- estimated_generation_cost
- estimated_fulfillment_cost
- delivery_sla_minutes
- status
- version
```

Enums iniciais:

```text
productMode:
- GENERIC
- PERSONALIZED

fulfillmentMode:
- STATIC_ASSET
- GENERATED_PER_LEAD
- HYBRID
```

### Oferta

Criar `CommercialOffer` para representar como um produto será vendido.

```text
commercial_offer
- id
- product_id
- hypothesis_id
- name
- role
- price_brl
- currency_code
- promise
- primary_cta
- checkout_provider
- checkout_configuration_reference
- refund_policy_reference
- status
- version
- created_at
- updated_at
```

Papéis:

```text
- LEAD_MAGNET
- TRIPWIRE
- CORE
- UPSELL
- FALLBACK
```

Vínculo com experimento:

```text
experiment_offer_assignment
- id
- experiment_id
- commercial_offer_id
- assignment_role
- active_from
- active_until
```

O preço operacional deve vir da oferta. O campo de preço atual do experimento permanece temporariamente como projeção compatível.

## 5.5 Personalização terá definição versionada

Não armazenar todo o contrato de personalização em um único texto livre.

Criar:

```text
product_personalization_definition
- id
- product_id
- version
- name
- status
- output_template_reference
- created_at
- updated_at

product_personalization_field
- id
- definition_id
- field_key
- label
- field_type
- source_preference
- required
- display_order
- purpose
- pii_classification
- validation_rule_reference
```

Tipos iniciais:

```text
- TEXT
- LONG_TEXT
- SINGLE_CHOICE
- MULTIPLE_CHOICE
- NUMBER
- DATE
- IMAGE_UPLOAD
- DOCUMENT_UPLOAD
```

Fontes preferenciais:

```text
- META_INSTANT_FORM
- LEAD_PORTAL
- CHECKOUT
- DERIVED
```

O campo `purpose` é obrigatório para explicar por que o dado é coletado e onde será usado.

---

## 6. Jornadas comerciais suportadas

## 6.1 Jornada A — Instant Form + enriquecimento + personalização

```text
Anúncio
  → Meta Instant Form
    → lead básico persistido
      → magic link por e-mail
        → Lead Portal para enriquecimento
          → amostra personalizada
            → oferta personalizada
              → checkout
                → geração e entrega paga
```

Uso recomendado:

- personalização exige vários dados;
- captura inicial precisa ter baixa fricção;
- existe benefício claro em demonstrar resultado individual.

Campos mínimos no Instant Form:

- nome;
- e-mail;
- no máximo uma pergunta qualificadora indispensável.

Dados complexos permanecem no Lead Portal.

## 6.2 Jornada B — Landing própria + amostra + oferta

```text
Anúncio
  → landing com formulário
    → enriquecimento ou upload
      → amostra genérica/personalizada
        → oferta
          → checkout
            → entrega
```

Uso recomendado:

- upload é necessário no primeiro contato;
- há perguntas condicionais;
- a landing precisa explicar melhor a recompensa;
- deseja-se comparar qualidade com Instant Form.

## 6.3 Jornada C — Venda direta genérica

```text
Anúncio
  → página de vendas
    → checkout
      → produto genérico
```

Uso recomendado:

- produto simples;
- promessa facilmente compreendida;
- baixo preço;
- entrega imediata;
- interesse principal é medir compra, não cadastro.

## 6.4 Jornada D — Fallback genérico após abandono

```text
Lead capturado
  → não concluiu enriquecimento dentro da janela
    → oferta ou amostra genérica
      → compra genérica ou retorno à personalização
```

Regras:

- fallback deve estar declarado em `ExperimentStrategy`;
- não pode mudar a promessa central silenciosamente;
- evento de abandono deve ser registrado;
- resultado do fallback deve ser separado do resultado personalizado;
- o usuário deve ver quantos leads seguiram cada ramificação.

---

## 7. Ingestão de Instant Form e identidade do lead

## 7.1 Responsabilidade do Facebook Ads Worker

A integração Meta deve ficar no Facebook Ads Worker.

Fluxo alvo:

1. Meta entrega o webhook ao endpoint público do Facebook Ads Worker;
2. worker valida assinatura e registra log do payload bruto;
3. worker envia o evento bruto ao backend;
4. backend cria uma execução versionada de ingestão;
5. worker busca a execução pendente;
6. worker consulta os detalhes do lead na API da Meta;
7. worker envia payload bruto e saída normalizada ao backend;
8. backend resolve identidade, consentimento, experimento e atribuição;
9. backend enfileira a jornada correta.

O worker não cria lead diretamente no banco e não dispara e-mail diretamente.

## 7.2 Pipeline de ingestão proposto

Nome lógico:

```text
metaleadingestion.v1
```

Executor:

```text
facebook-ads-worker
```

Etapas:

```text
1. LEAD_DETAIL_FETCH
2. LEAD_NORMALIZATION
3. LEAD_IDENTITY_RESOLUTION
4. LEAD_JOURNEY_ENROLLMENT
```

As etapas 1 e 2 são executadas pelo Facebook Ads Worker. As etapas 3 e 4 são decisões/persistências do backend.

O backend controla todas as transições.

## 7.3 Inbox idempotente

Criar uma inbox de integração:

```text
integration_event_inbox
- id
- provider
- event_type
- provider_event_id
- payload_hash
- raw_payload
- signature_status
- processing_status
- received_at
- processed_at
- failure_reason
```

Chave única recomendada:

```text
(provider, event_type, provider_event_id)
```

O payload bruto é preservado para auditoria, com acesso administrativo restrito.

## 7.4 Modelo de identidade

Manter `lead` como agregado principal e adicionar:

```text
lead_identity
- id
- lead_id
- provider
- identity_type
- external_id
- verified_at
- created_at

lead_contact_point
- id
- lead_id
- contact_type
- normalized_value_hash
- protected_value
- verified
- source
- created_at
- updated_at

lead_profile
- id
- lead_id
- personalization_definition_id
- status
- completion_percent
- completed_at
- updated_at

lead_profile_answer
- id
- lead_profile_id
- personalization_field_id
- text_value
- numeric_value
- date_value
- asset_id
- source
- answered_at
```

Não duplicar e-mail, telefone ou nome em payloads de evento quando uma referência ao lead for suficiente.

## 7.5 Consentimento

Criar registro granular:

```text
lead_consent
- id
- lead_id
- purpose
- channel
- status
- policy_version
- source
- evidence_reference
- captured_at
- revoked_at
```

Propósitos iniciais:

```text
- DELIVER_REQUESTED_SAMPLE
- PERSONALIZE_PRODUCT
- TRANSACTIONAL_EMAIL
- MARKETING_EMAIL
- WHATSAPP_MARKETING
- ANALYTICS
```

A entrega solicitada não deve depender de consentimento genérico de marketing.

## 7.6 Magic link

Criar token de acesso com:

- valor aleatório forte;
- somente hash persistido;
- lead e fluxo associados;
- expiração;
- uso único ou rotação;
- revogação;
- registro de abertura e conclusão;
- ausência de PII no URL.

---

## 8. Eventos, atribuição e funil

## 8.1 Fato comercial canônico

Criar uma tabela imutável para os eventos normalizados:

```text
experiment_commercial_event
- id
- event_id UNIQUE
- experiment_id
- comparison_id nullable
- comparison_arm_id nullable
- lead_id nullable
- event_type
- funnel_stage
- source_system
- provider_event_id
- attribution_id nullable
- value_brl nullable
- currency_code nullable
- occurred_at
- received_at
- inbox_event_id nullable
- created_at
```

Chave de idempotência adicional:

```text
(source_system, provider_event_id, event_type)
```

`experiment_funnel_event` e os resumos atuais permanecem como projeções compatíveis durante a migração.

## 8.2 Eventos mínimos

```text
- AD_IMPRESSION
- AD_CLICK
- INSTANT_FORM_OPENED
- LEAD_CAPTURED
- LANDING_VIEWED
- FORM_STARTED
- FORM_SUBMITTED
- PROFILE_STARTED
- PROFILE_COMPLETED
- SAMPLE_REQUESTED
- SAMPLE_GENERATION_STARTED
- SAMPLE_READY
- SAMPLE_DELIVERED
- SAMPLE_VIEWED
- OFFER_VIEWED
- CHECKOUT_STARTED
- PAYMENT_APPROVED
- PAYMENT_REFUNDED
- PAID_GENERATION_STARTED
- PAID_PRODUCT_READY
- PAID_PRODUCT_DELIVERED
- PAID_PRODUCT_ACCESSED
- FALLBACK_TRIGGERED
```

## 8.3 Atribuição

Criar `lead_attribution`:

```text
- id
- lead_id
- experiment_id
- comparison_arm_id nullable
- source
- medium
- campaign
- ad_id
- adset_id
- click_id
- first_touch_at
- last_touch_at
- attribution_model
```

A atribuição inicial v1 deve ser simples e explícita:

- first touch para aquisição;
- last eligible touch para conversão;
- sem atribuição probabilística por modelo na primeira versão.

## 8.4 Funil orientado à rota

A UI não deve mostrar etapas impossíveis como falhas.

Exemplos:

- venda direta não exige `LEAD_CAPTURED`;
- Instant Form não exige visualização de landing antes da captura;
- produto genérico não exige `PROFILE_COMPLETED`;
- amostra inexistente não deve aparecer com taxa zero.

O backend deve devolver um `funnelDefinition` específico da estratégia junto das métricas.

---

## 9. Economia unitária

## 9.1 Ledger de custos

Criar:

```text
experiment_cost_entry
- id
- experiment_id
- comparison_arm_id nullable
- lead_id nullable
- cost_type
- amount_brl
- source_system
- source_reference
- occurred_at
- created_at
```

Tipos:

```text
- MEDIA
- AI_TEXT
- AI_IMAGE
- AI_VIDEO
- STORAGE
- EMAIL
- PAYMENT_FEE
- PERSONALIZED_GENERATION
- FULFILLMENT
- REFUND
- MANUAL_ADJUSTMENT
```

## 9.2 Ledger de receita

Receita deve ser derivada de eventos de pagamento aprovados e estornos.

Não editar receita manualmente sem criar um lançamento de ajuste auditável.

## 9.3 Métricas obrigatórias

Por experimento e por braço:

```text
CTR = cliques / impressões
captureRate = leads / cliques elegíveis
profileCompletionRate = perfis completos / leads capturados
sampleConsumptionRate = amostras vistas / amostras entregues
checkoutRate = checkouts / visualizações de oferta
purchaseRatePerClick = compras / cliques
purchaseRatePerLead = compras / leads
revenuePerClick = receita líquida / cliques
revenuePerLead = receita líquida / leads
aiCostPerLead = custos de IA / leads
personalizationCostPerPurchase = custos personalizados / compras
contributionMargin = receita líquida - custos variáveis
contributionMarginPerClick = contributionMargin / cliques
contributionMarginPerLead = contributionMargin / leads
refundRate = reembolsos / compras
```

A métrica primária recomendada para comparação comercial é:

```text
contributionMarginPerEligibleClick
```

CPL continua sendo diagnóstico, não critério final isolado.

## 9.4 Qualidade do dado

O backend deve emitir flags:

```text
- SPEND_NOT_SYNCED
- PAYMENT_EVENTS_DELAYED
- DUPLICATE_EVENT_DETECTED
- ATTRIBUTION_MISSING
- ARM_TRAFFIC_IMBALANCE
- FUNNEL_EVENT_GAP
- COST_LEDGER_INCOMPLETE
- SAMPLE_TOO_SMALL
- WINDOW_NOT_COMPLETE
- STRATEGY_UNCLASSIFIED
```

Nenhuma recomendação pode declarar decisão pronta enquanto houver bloqueio crítico.

---

## 10. Comparação e método de decisão

## 10.1 Modelo de dados

```text
experiment_comparison
- id
- hypothesis_id
- code
- name
- business_question
- changed_variable
- primary_metric
- status
- observation_window_days
- min_eligible_clicks_per_arm
- min_purchases_per_arm
- stop_loss_brl
- created_by
- created_at
- updated_at

experiment_comparison_arm
- id
- comparison_id
- experiment_id UNIQUE
- arm_code
- role
- expected_traffic_share
- active
- joined_at
```

Papéis:

```text
- CONTROL
- CHALLENGER
```

Status:

```text
- DRAFT
- READY
- RUNNING
- PAUSED
- DECISION_READY
- DECIDED
- INCONCLUSIVE
- CANCELLED
```

## 10.2 Comparabilidade

Antes de ativar, o backend deve validar:

- mesma hipótese;
- mesma janela geográfica;
- mesmo público ou regra explícita de equivalência;
- mesma oferta quando a variável não for oferta;
- mesmo criativo quando a variável não for criativo;
- mesmo preço quando a variável não for preço;
- mesma janela de atribuição;
- mesma métrica primária;
- apenas uma variável principal declarada;
- estratégia compatível com a pergunta de negócio.

O backend deve gerar uma lista de diferenças e classificá-las:

```text
- EXPECTED_DIFFERENCE
- CONTROLLED_EQUIVALENCE
- COMPARABILITY_WARNING
- COMPARABILITY_BLOCKER
```

## 10.3 Nível de evidência

```text
- NO_DATA
- EARLY_SIGNAL
- DIRECTIONAL
- DECISION_READY
```

O nível deve ser calculado de forma determinística com base em:

- tamanho de amostra;
- janela concluída;
- integridade dos dados;
- quantidade de conversões;
- estabilidade da direção;
- intervalo de incerteza;
- diferença mínima relevante configurada.

O modelo de IA não define o nível de evidência.

## 10.4 Política de parada

Políticas iniciais:

```text
- FIRST_VALID_LEAD_STANDBY
- FIXED_WINDOW
- MIN_SAMPLE_AND_WINDOW
- STOP_LOSS
- MANUAL_ONLY
```

A regra atual de primeiro envio permanece default para `EARLY_SIGNAL`.

Antes de ativar `CONTROLLED_COMPARISON`, o cânone deve ser atualizado para permitir uma política diferente e impedir que o primeiro lead pause automaticamente todos os braços.

---

## 11. Apoio à decisão por IA

A implementação detalhada está em:

`docs/implementacao/experimentos/especificacao-pipeline-decisao-ia-v1.md`

Princípios centrais:

1. cálculo de métricas é responsabilidade do backend;
2. o modelo recebe snapshot imutável e IDs de evidência;
3. o modelo interpreta, não recalcula a fonte de verdade;
4. toda afirmação deve referenciar evidências existentes;
5. confiança do modelo não substitui confiança estatística;
6. ausência de dados deve aparecer explicitamente;
7. recomendação não executa ação;
8. usuário aceita, rejeita ou modifica a recomendação;
9. comando de negócio é separado da decisão;
10. toda ação gera histórico auditável.

Recomendações iniciais permitidas:

```text
- KEEP_COLLECTING_DATA
- PAUSE_FOR_DATA_QUALITY
- STOP_BY_STOP_LOSS
- ITERATE_CAPTURE_ROUTE
- ITERATE_PROFILE_ENRICHMENT
- ITERATE_SAMPLE
- ITERATE_OFFER
- ITERATE_PRICE
- ITERATE_CREATIVE
- DECLARE_CONTROL_WINNER
- DECLARE_CHALLENGER_WINNER
- MARK_INCONCLUSIVE
- CREATE_FOLLOW_UP_TEST
```

---

## 12. Frontend e experiência de decisão

A especificação detalhada está em:

`docs/implementacao/experimentos/especificacao-centro-de-decisao-frontend.md`

A interface deve sempre responder:

1. O que está sendo testado?
2. Qual é a única variável alterada?
3. Quais rotas e ofertas cada braço usa?
4. O experimento está realmente publicado e recebendo dados?
5. Os dados estão atualizados e íntegros?
6. Em qual etapa do funil ocorre a maior perda?
7. Quanto cada braço gastou, vendeu e gerou de margem?
8. A amostra é suficiente para decidir?
9. O que as regras determinísticas indicam?
10. O que o modelo recomenda e em quais evidências se apoia?
11. Quais limitações e riscos existem?
12. Qual comando está disponível agora?
13. Quem tomou a última decisão e por quê?
14. O que acontecerá após executar o comando?

O frontend não calcula métricas de negócio. Ele consome DTOs de leitura do backend.

---

## 13. APIs propostas

## 13.1 Estratégia

```text
GET    /api/experiments/{experimentId}/strategy
PUT    /api/experiments/{experimentId}/strategy
GET    /api/experiments/{experimentId}/strategy/diagnostics
```

## 13.2 Produto e oferta

```text
GET    /api/products/{productId}
GET    /api/products/{productId}/personalization-definitions
POST   /api/products/{productId}/personalization-definitions
GET    /api/commercial-offers/{offerId}
POST   /api/experiments/{experimentId}/offer-assignments
```

## 13.3 Comparação

```text
GET    /api/experiment-comparisons
POST   /api/experiment-comparisons
GET    /api/experiment-comparisons/{comparisonId}
PATCH  /api/experiment-comparisons/{comparisonId}
POST   /api/experiment-comparisons/{comparisonId}/arms
GET    /api/experiment-comparisons/{comparisonId}/readiness
POST   /api/experiment-comparisons/{comparisonId}/activate
POST   /api/experiment-comparisons/{comparisonId}/pause
GET    /api/experiment-comparisons/{comparisonId}/dashboard
GET    /api/experiment-comparisons/{comparisonId}/decision-package
```

## 13.4 Decisão

```text
POST   /api/experiment-comparisons/{comparisonId}/decision-runs
GET    /api/experiment-comparisons/{comparisonId}/decision-runs
GET    /api/experiment-decision-runs/{runId}
POST   /api/experiment-decision-runs/{runId}/human-decisions
POST   /api/experiment-decision-runs/{runId}/commands
GET    /api/experiment-decision-runs/{runId}/action-history
```

## 13.5 Ingestão Meta interna

```text
POST   /api/internal/meta-lead-ingestion/v1/events
GET    /api/internal/meta-lead-ingestion/v1/lead-detail/stage-executions/pending
PATCH  /api/internal/meta-lead-ingestion/v1/lead-detail/stage-executions/{jobId}
GET    /api/internal/meta-lead-ingestion/v1/normalization/stage-executions/pending
PATCH  /api/internal/meta-lead-ingestion/v1/normalization/stage-executions/{jobId}
```

Os nomes finais devem ser confirmados no Swagger e nos pacotes do domínio antes da implementação.

---

## 14. Estrutura de pacotes proposta

## 14.1 Backend

```text
com.marketinghub.experiment.strategy
com.marketinghub.experiment.comparison
com.marketinghub.experiment.economics
com.marketinghub.experiment.commercialevent
com.marketinghub.experiment.decision.v1
com.marketinghub.lead.identity
com.marketinghub.lead.consent
com.marketinghub.product.personalization
com.marketinghub.offer
com.marketinghub.metaleadingestion.v1
```

Responsabilidades devem permanecer separadas em controller, service, mapper, dto e repository conforme o padrão atual.

## 14.2 AI Worker

```text
com.marketinghub.worker.experimentdecisionv1.pipeline
com.marketinghub.worker.experimentdecisionv1.pipeline.signaldiagnosis
com.marketinghub.worker.experimentdecisionv1.pipeline.alternativegeneration
com.marketinghub.worker.experimentdecisionv1.pipeline.recommendationreview
```

Recursos:

```text
ai-worker/src/main/resources/prompts/experiment-decision/v1/signal-diagnosis.md
ai-worker/src/main/resources/prompts/experiment-decision/v1/signal-diagnosis-schema.json
ai-worker/src/main/resources/prompts/experiment-decision/v1/alternative-generation.md
ai-worker/src/main/resources/prompts/experiment-decision/v1/alternative-generation-schema.json
ai-worker/src/main/resources/prompts/experiment-decision/v1/recommendation-review.md
ai-worker/src/main/resources/prompts/experiment-decision/v1/recommendation-review-schema.json
```

## 14.3 Facebook Ads Worker

```text
com.marketinghub.facebookadsworker.metaleadingestionv1.pipeline
com.marketinghub.facebookadsworker.metaleadingestionv1.pipeline.leaddetail
com.marketinghub.facebookadsworker.metaleadingestionv1.pipeline.normalization
```

## 14.4 Frontend

```text
frontend/src/pages/experiment-comparison
frontend/src/api/experiment-comparison
frontend/src/pages/experiment/components/strategy
frontend/src/pages/experiment/components/economics
frontend/src/pages/experiment/components/decision
```

---

## 15. Fases de implementação

## Fase 0 — Cânone, baseline e proteção

### Entregas

1. atualizar o cânone de experimento com:
   - experimento atômico;
   - comparação como camada superior;
   - modos `EARLY_SIGNAL` e `CONTROLLED_COMPARISON`;
   - separação entre recomendação e ação;
   - política de parada por modo;
2. registrar baseline dos experimentos atuais;
3. criar feature flags;
4. definir dados de migração e rollback;
5. não alterar comportamento produtivo ainda.

### Critérios de aceite

- nenhuma regra nova existe apenas no código;
- o comportamento legado continua sendo o padrão;
- comparação controlada permanece desativada por feature flag;
- plano de rollback documentado.

## Fase 1 — Estratégia, produto, oferta e personalização

### Entregas

1. criar `experiment_strategy`;
2. adicionar enums e validações;
3. evoluir `product`;
4. criar `commercial_offer` e assignments;
5. criar definições de personalização;
6. backfill de experimentos atuais;
7. endpoints administrativos;
8. card de estratégia no frontend.

### Backfill

Regras iniciais:

- Instant Form associado e landing não usada como captura: `META_INSTANT_FORM`;
- Lead Portal/landing usada como captura: `OWNED_LANDING`;
- checkout sem captura: `DIRECT_SALES_PAGE`;
- combinação ambígua: `LEGACY_UNCLASSIFIED`.

Nenhum experimento `LEGACY_UNCLASSIFIED` pode ser republicado até revisão explícita.

### Critérios de aceite

- todo experimento novo possui estratégia válida;
- experimento legado permanece legível;
- UI mostra rota e objetivo;
- nenhum worker precisa ser alterado ainda.

## Fase 2 — Comparação de experimentos

### Entregas

1. criar comparison e arms;
2. validar comparabilidade;
3. criar readiness;
4. criar política de parada;
5. gerar códigos automáticos;
6. listar comparações no frontend;
7. permitir agrupar experimentos existentes.

### Critérios de aceite

- não é possível ativar comparação com bloqueadores;
- diferenças entre braços são explicadas;
- cada experimento pertence a no máximo uma comparação ativa;
- criação não altera publicação dos braços.

## Fase 3 — Ingestão Meta e identidade

### Entregas

1. inbox idempotente;
2. pipeline `metaleadingestion.v1`;
3. webhook no Facebook Ads Worker;
4. busca de detalhes do lead;
5. normalização;
6. identidade e contato;
7. consentimento;
8. magic link;
9. enrollment da jornada pelo backend;
10. observabilidade no frontend.

### Migração do fluxo legado

O endpoint atual de webhook deve permanecer temporariamente:

- marcado como legado;
- sem novas capacidades;
- com telemetria de uso;
- protegido por feature flag;
- removido somente após tráfego integral no pipeline novo.

A chamada externa de boas-vindas atualmente acoplada à persistência deve migrar para comando/outbox consumido pelo canal responsável.

### Critérios de aceite

- reenvio do mesmo evento não duplica lead;
- nome/e-mail são persistidos de forma protegida;
- consentimento possui fonte e versão;
- lead aparece na UI com origem e etapa;
- falha da Meta não perde o evento bruto.

## Fase 4 — Fatos comerciais e economia

### Entregas

1. `experiment_commercial_event`;
2. adaptadores de eventos de cada módulo;
3. ledger de custos;
4. receita e reembolso;
5. projeção do funil atual;
6. DTO de métricas econômicas;
7. flags de qualidade;
8. data freshness.

### Critérios de aceite

- todos os eventos possuem idempotência;
- pagamento aprovado e estorno reconciliam;
- custo de IA entra na margem;
- nenhum cálculo principal acontece no frontend;
- funil antigo e novo são comparados em testes de regressão.

## Fase 5 — Centro de Decisão sem IA

### Entregas

1. dashboard de comparação;
2. funil lado a lado;
3. economia unitária;
4. qualidade do dado;
5. prontidão determinística;
6. histórico de estado;
7. comandos contextuais;
8. responsividade e acessibilidade.

### Critérios de aceite

- usuário consegue decidir manualmente sem consultar logs;
- cada número mostra definição e período;
- N/A é diferente de zero;
- dados atrasados ficam visíveis;
- comandos indisponíveis mostram motivo.

## Fase 6 — Pipeline de decisão por IA v1

### Entregas

1. backend `experiment.decision.v1`;
2. AI Worker `experimentdecisionv1`;
3. prompts e schemas versionados;
4. snapshot imutável;
5. análise de sinais;
6. geração de alternativas;
7. revisão crítica;
8. validação de evidence refs;
9. recomendação final;
10. decisão humana persistida;
11. painel no frontend.

### Critérios de aceite

- nenhuma métrica é inventada;
- toda recomendação cita evidências válidas;
- request/response/modelo/tokens/custo ficam auditáveis;
- recomendação não executa comando;
- usuário pode aceitar, rejeitar ou modificar;
- falha do modelo não bloqueia leitura determinística.

## Fase 7 — Personalização e fallback

### Entregas

1. Lead Portal renderiza campos por definição versionada;
2. perfil parcial e completo;
3. geração de amostra personalizada;
4. entrega de amostra;
5. oferta personalizada;
6. geração paga;
7. fallback genérico;
8. SLA e estados de fulfillment;
9. custo por geração.

### Critérios de aceite

- dados coletados têm propósito;
- produto usa somente campos autorizados;
- reprocessamento não cobra/gera duplicado;
- fallback é mensurado separadamente;
- lead recebe comunicação clara em falhas.

## Fase 8 — Calibração e automação gradual

### Entregas

1. comparar recomendações com decisões humanas;
2. comparar decisões com resultado posterior;
3. medir taxa de aceitação e acerto;
4. calibrar prompts e thresholds;
5. liberar automações de baixo risco;
6. manter aprovação humana para orçamento, preço, promessa e escala.

### Automações candidatas de baixo risco

- solicitar nova análise quando janela encerrar;
- avisar dado desatualizado;
- criar rascunho de próximo teste;
- sugerir pausa por integração quebrada;
- disparar fallback já aprovado.

### Ações que permanecem humanas na v1

- elevar orçamento;
- trocar preço;
- trocar oferta;
- declarar vencedor;
- encerrar hipótese;
- publicar nova campanha;
- alterar finalidade de dados pessoais.

---

## 16. Sequência recomendada de pull requests

### PR 1 — Cânone e feature flags

- regras de modo operacional;
- comparação como camada superior;
- sem alteração funcional.

### PR 2 — Estratégia do experimento

- tabela, enums, serviços, API, backfill e card de frontend.

### PR 3 — Produto, oferta e personalização

- contratos de produto/oferta;
- definições versionadas;
- sem geração ainda.

### PR 4 — Comparação

- entidade, braços, readiness e tela de criação.

### PR 5 — Ingestão Meta v1

- inbox, contratos backend e execução no Facebook Ads Worker.

### PR 6 — Identidade, consentimento e magic link

- contatos protegidos;
- jornada pós-captura.

### PR 7 — Eventos e atribuição

- fato comercial canônico;
- projeções e migração.

### PR 8 — Economia unitária

- custos, receitas, margem e qualidade.

### PR 9 — Centro de Decisão determinístico

- dashboard completo sem IA.

### PR 10 — Backend de decisão v1

- jobs, stages, snapshot, evidências e human decision.

### PR 11 — AI Worker de decisão v1

- estágios plugáveis, prompts, schemas e auditoria.

### PR 12 — Recomendação no frontend

- evidências, limitações, decisão e histórico.

### PR 13 — Personalização e fallback

- Lead Portal, geração, entrega e custos.

### PR 14 — Calibração e automação segura

- feedback loop e políticas aprovadas.

Cada PR deve ser pequeno o suficiente para rollback independente.

---

## 17. Testes obrigatórios

## 17.1 Backend

- validação de estratégia;
- backfill idempotente;
- comparabilidade;
- idempotência de evento;
- identidade e deduplicação;
- consentimento;
- cálculo de métricas;
- ledger de custo e receita;
- readiness;
- política de parada;
- evidence refs;
- transições de decisão;
- autorização de comandos.

## 17.2 Workers

- contratos `pending`;
- retries técnicos;
- callback de sucesso/falha;
- payload bruto registrado;
- schema de IA;
- request com modo Flex quando OpenAI;
- ausência de acoplamento entre etapas;
- não repetição de processamento concluído.

## 17.3 Frontend

- estados loading/empty/error;
- N/A versus zero;
- bloqueio de comando;
- exibição de data freshness;
- comparação de braços;
- evidências da recomendação;
- decisão humana;
- rotas de acessibilidade e responsividade.

## 17.4 Testes ponta a ponta

Cenários mínimos:

1. Meta Instant Form → enriquecimento → amostra personalizada → compra;
2. landing → amostra genérica → compra;
3. venda direta → compra;
4. abandono → fallback genérico;
5. webhook duplicado;
6. falha temporária da Meta;
7. pagamento duplicado;
8. estorno;
9. comparação com dados insuficientes;
10. recomendação com evidence ref inválida;
11. decisão humana rejeitada;
12. criação do próximo teste a partir de recomendação aceita.

---

## 18. Segurança, LGPD e privacidade

Requisitos mínimos:

- minimização de dados;
- finalidade explícita por campo;
- consentimento separado por propósito/canal;
- valor de contato protegido;
- logs sem PII desnecessária;
- mascaramento de e-mail/telefone na listagem;
- acesso completo somente para perfis autorizados;
- expiração de magic links;
- retenção e exclusão documentadas;
- auditoria de acesso a payload bruto;
- proibição de enviar PII desnecessária ao modelo;
- snapshot de IA preferencialmente pseudonimizado;
- URLs sem e-mail, telefone ou nome.

---

## 19. Observabilidade operacional

O frontend deve consumir dados persistidos. Logs técnicos permanecem para investigação.

Registrar:

- recebimento do webhook;
- validação de assinatura;
- envio à inbox;
- execução de cada estágio;
- chamadas à Meta;
- resolução de identidade;
- enrollment da jornada;
- envio de e-mail;
- geração de amostra/produto;
- checkout e pagamento;
- eventos rejeitados por duplicidade;
- cálculo de snapshot;
- execução de decisão por IA;
- decisão humana;
- comando e resultado.

Alertas operacionais:

- fila parada;
- integração sem sincronização;
- taxa de erro acima do limite;
- evento de pagamento sem atribuição;
- geração acima do SLA;
- custo de IA fora do esperado;
- recomendação sem evidência válida.

---

## 20. Migração e rollback

### Estratégia

1. criar estruturas novas;
2. backfill sem remover campos antigos;
3. habilitar leitura nova por feature flag;
4. ativar dual-write de eventos;
5. comparar projeções;
6. migrar tela;
7. migrar workers;
8. encerrar caminho legado somente após estabilidade.

### Rollback

- feature flag restaura UI e leitura antigas;
- campos atuais de `Experiment` permanecem;
- webhook legado continua disponível durante transição;
- recomendações v1 podem ser desativadas sem afetar o funil;
- fatos comerciais novos são append-only e não precisam ser apagados;
- comparações podem ser pausadas sem pausar os experimentos automaticamente.

---

## 21. Riscos e mitigação

| Risco | Mitigação |
|---|---|
| Muitas combinações de funil confundirem o usuário | Estratégias pré-definidas, validação e wizard guiado |
| IA recomendar com dados ruins | Gate determinístico de qualidade e evidência |
| Duplicação de leads/eventos | Inbox e chaves idempotentes |
| Mistura de braços | Experimento atômico e comparison arm explícito |
| Margem incorreta | Ledger por fonte e data freshness |
| PII chegar aos modelos | Snapshot pseudonimizado e whitelist de campos |
| Primeiro lead pausar teste comparativo | Modo operacional e política de parada explícitos no cânone |
| Migração quebrar campanhas atuais | Feature flags e compatibilidade incremental |
| Frontend virar orquestrador | Comandos simples; transições no backend |
| Worker decidir próxima etapa | Backend cria cada execução subsequente |
| Produto personalizado ficar caro | Custo por lead/compra e fallback genérico |

---

## 22. Critérios globais de aceite

A evolução estará pronta quando:

- [ ] todo experimento declara uma estratégia válida;
- [ ] comparação agrupa experimentos atômicos;
- [ ] variável alterada e controles ficam explícitos;
- [ ] Instant Form, landing e venda direta são rotas oficiais;
- [ ] produto e oferta são independentes;
- [ ] personalização possui schema versionado;
- [ ] lead possui identidade e consentimentos auditáveis;
- [ ] Meta ingestion é idempotente;
- [ ] eventos comerciais são canônicos e rastreáveis;
- [ ] custos e receitas formam margem por braço;
- [ ] funil é adaptado à rota;
- [ ] frontend mostra dados, qualidade, prontidão e comandos;
- [ ] IA usa snapshot imutável e evidence refs;
- [ ] recomendação e ação são separadas;
- [ ] decisão humana é persistida;
- [ ] modelos não recebem PII desnecessária;
- [ ] todos os pipelines são versionados e controlados pelo backend;
- [ ] rollback é possível por fase;
- [ ] testes ponta a ponta cobrem as quatro jornadas principais.

---

## 23. Primeiro passo de execução

Implementar somente a **Fase 1 — estratégia do experimento**, sem alterar publicação, ingestão ou decisão por IA.

O primeiro PR deve entregar:

1. tabela `experiment_strategy`;
2. enums;
3. serviço de diagnóstico/validação;
4. backfill seguro;
5. endpoints;
6. card no overview;
7. bloqueio de republicação para estratégia ambígua;
8. testes.

Isso cria o vocabulário necessário para todas as fases seguintes e reduz o risco de adicionar novas automações sobre estados implícitos.

---

## 24. Produto IA visual e personalizado

### 24.1 Decisão

O tipo `Produto IA` deve ser subdividido para que o Marketing Hub consiga testar mecanismos diferentes sem misturar aprendizado comercial.

Subtipos iniciais:

- `AI_VISUAL_PREVIEW`;
- `AI_PERSONALIZED_SAMPLE`;
- `AI_TRANSFORMATION_SIMULATOR`;
- `AI_VISUAL_ASSET_PACK`;
- `AI_IDENTITY_AVATAR_PRODUCT`;
- `AI_REPORT_VISUAL_EVIDENCE`.

O primeiro MVP recomendado e `AI_PERSONALIZED_SAMPLE`, porque testa duas hipoteses importantes ao mesmo tempo:

- imagens geradas por IA podem aumentar impacto, desejo e tangibilizacao do resultado;
- personalizacao visual pode aumentar valor percebido porque o lead recebe algo criado para ele.

### 24.2 Alternativas avaliadas

| Alternativa | Beneficio | Risco | Esforco | Decisao |
|---|---|---|---|---|
| Usar imagens bonitas apenas em anuncio ou pagina | Rapido e barato | Pode melhorar CTR sem melhorar venda | Baixo | Nao e suficiente como estrategia principal |
| Criar exemplos manuais de Produto IA | Ajuda a visualizar possibilidades | Quebra rastreabilidade e dificulta repetir em outro nicho | Baixo/medio | Rejeitado |
| Criar produtos pelo fluxo sistemico com subtipo, hipotese e experimento | Repetivel, auditavel e comparavel | Exige evoluir contratos e telas | Medio | Escolhido |

### 24.3 Regra operacional

Produto nenhum deve nascer por conta propria. O sistema deve ser capaz de explicar como ele foi criado e de recria-lo para outro nicho ou varia-lo para melhoria.

Todo Produto IA visual ou personalizado precisa registrar:

- nicho/contexto;
- hipotese;
- dor;
- resultado;
- mecanismo;
- prova;
- oferta;
- subtipo;
- entrada solicitada ao lead;
- saida prometida;
- prompts/schemas;
- custo estimado por lead ou cliente;
- experimento de validacao;
- metrica primaria;
- regra de parada ou invalidacao.

### 24.4 Experimentos a preparar

Sequencia recomendada:

1. Criar suporte sistemico para declarar subtipo de Produto IA na criacao/estrategia do experimento.
2. Permitir que o fluxo de hipotese/oferta gere um Produto IA `AI_PERSONALIZED_SAMPLE` sem criar ativos manualmente.
3. Criar contrato de amostra personalizada com entrada minima do lead, saida gerada, custo e status.
4. Publicar experimento pequeno comparando amostra personalizada contra rota sem amostra, mantendo uma unica variavel primaria.
5. Medir custo por lead, taxa de visualizacao da amostra, clique para checkout, compra, custo de IA por compra e margem.
