# Modelo de Dados do Experimento (visão focada)

Este documento consolida, em uma única visão, as entidades mais relevantes no
contexto de **experimentos de marketing**: nicho, hipótese, públicos, criativos,
campanha e objetos de ativação/medição.

> Fonte base: estrutura detalhada em `docs/data-model.md`.

## Escopo coberto

- Planejamento do experimento (nicho, hipótese e jornada).
- Geração de públicos e criativos por IA.
- Estrutura de mídia paga (campanha, conjunto de anúncios e anúncios no Meta Ads).
- Coleta de leads no fluxo vinculado ao experimento.

## Diagrama ER (contexto do experimento)

```mermaid
erDiagram
    MARKET_NICHE {
      BIGINT id PK
      VARCHAR name
      LONGTEXT interest_list
      LONGTEXT role_list
      LONGTEXT behavior_list
    }

    HYPOTHESIS {
      BINARY16 id PK
      BIGINT market_niche_id FK
      VARCHAR title
      BOOLEAN approved
      LONGTEXT framework_json
    }

    EXPERIMENT {
      BIGINT id PK
      BIGINT niche_id FK
      BINARY16 hypothesis_id FK
      VARCHAR name
      BIGINT facebook_page_id FK
      BIGINT facebook_instant_form_id FK
      BIGINT lead_portal_flow_id FK
      BIGINT journey_template_id FK
      DECIMAL daily_budget
      VARCHAR status
      VARCHAR stage
      VARCHAR creative_generation_mode
      VARCHAR primary_variable
      VARCHAR primary_metric
      DATE start_date
      DATE end_date
      DATETIME facebook_release_requested_at
    }

    TARGET_AUDIENCE {
      BIGINT id PK
      BIGINT market_niche_id FK
      BINARY16 hypothesis_id FK
      LONGTEXT description
      LONGTEXT prompt
      VARCHAR model
      BOOLEAN approved
    }

    EXPERIMENT_TARGETING_SELECTION {
      BIGINT id PK
      BIGINT experiment_id FK
      VARCHAR candidate_type
      VARCHAR term
      BIGINT targeting_element_id FK
    }

    TARGETING_REQUEST {
      BINARY16 id PK
      BIGINT experiment_id FK
      BIGINT market_niche_id FK
      BINARY16 hypothesis_id FK
      VARCHAR descricao
      VARCHAR locale
      VARCHAR country
      VARCHAR audience_type
      VARCHAR status
      VARCHAR origin
    }

    AD_SET {
      BIGINT id PK
      BIGINT experiment_id FK
      LONGTEXT targeting_json
      DECIMAL budget
      INT duration_days
      LONGTEXT prompt
      VARCHAR model
    }

    EXPERIMENT_ADSET_WORKFLOW {
      BIGINT id PK
      BIGINT experiment_id FK
      ENUM status
      VARCHAR seed_keyword
      VARCHAR seed_locale
      VARCHAR seed_interest_id
      VARCHAR seed_interest_name
      BIGINT seed_audience_lower
      BIGINT seed_audience_upper
      LONGTEXT ai_notes
      LONGTEXT last_error
      DATETIME completed_at
    }

    EXPERIMENT_ADSET_SPEC {
      BIGINT id PK
      BIGINT workflow_id FK
      ENUM slot
      VARCHAR label
      INT age_min
      INT age_max
      LONGTEXT targeting_spec
      VARCHAR validation_status
      VARCHAR reach_status
      BIGINT reach_lower_bound
      BIGINT reach_upper_bound
    }

    CREATIVE {
      BIGINT id PK
      BIGINT experiment_id FK
      VARCHAR type
      LONGTEXT headlines
      LONGTEXT primary_texts
      LONGTEXT prompt
      VARCHAR model
      BOOLEAN approved
    }

    CREATIVE_VARIANT {
      BIGINT id PK
      BIGINT experiment_id FK
      VARCHAR type
      VARCHAR asset_url
      LONGTEXT titles
      LONGTEXT descriptions
    }

    LANDING_PAGE {
      BIGINT id PK
      BIGINT experiment_id FK
      VARCHAR url
      VARCHAR type
      VARCHAR status
    }

    FACEBOOK_ADS_CAMPAIGN {
      CHAR36 id PK
      BIGINT experiment_id FK
      VARCHAR ad_account_id
      VARCHAR name
      VARCHAR objective
      ENUM status
      ENUM budget_mode
      BIGINT daily_budget_minor
    }

    FACEBOOK_ADS_AD_SET {
      CHAR36 id PK
      CHAR36 campaign_id FK
      VARCHAR name
      ENUM status
      LONGTEXT targeting_json
      BIGINT daily_budget_minor
    }

    FACEBOOK_ADS_AD {
      CHAR36 id PK
      CHAR36 adset_id FK
      CHAR36 creative_id FK
      VARCHAR name
      ENUM status
    }

    FACEBOOK_ADS_AD_CREATIVE {
      CHAR36 id PK
      VARCHAR external_id
      VARCHAR page_id
      VARCHAR instagram_user_id
      ENUM kind
    }

    LEAD_PORTAL_FLOW {
      BIGINT id PK
      BIGINT experiment_id FK
      BIGINT market_niche_id FK
      VARCHAR name
      VARCHAR slug
      VARCHAR model
      LONGTEXT prompt
      BOOLEAN approved
    }

    LEAD_PORTAL_SUBMISSION {
      BIGINT id PK
      BIGINT flow_id FK
      BIGINT experiment_id FK
      BINARY16 lead_id FK
      VARCHAR status
      TIMESTAMP submitted_at
    }

    METRIC_SNAPSHOT {
      BIGINT id PK
      BIGINT creative_id FK
      BIGINT ad_set_id FK
      INT impressions
      INT clicks
      DECIMAL cost
      DECIMAL cpa
      DECIMAL roas
    }

    MARKET_NICHE ||--o{ HYPOTHESIS : organiza
    EXPERIMENT ||--o{ TARGETING_REQUEST : solicita
    MARKET_NICHE ||--o{ EXPERIMENT : agrupa
    HYPOTHESIS ||--o{ EXPERIMENT : orienta

    MARKET_NICHE ||--o{ TARGET_AUDIENCE : possui
    MARKET_NICHE ||--o{ LEAD_PORTAL_FLOW : disponibiliza
    HYPOTHESIS ||--o{ TARGET_AUDIENCE : refina

    EXPERIMENT ||--o{ CREATIVE : gera
    EXPERIMENT ||--o{ CREATIVE_VARIANT : detalha
    EXPERIMENT ||--o{ EXPERIMENT_TARGETING_SELECTION : seleciona
    TARGETING_ELEMENT ||--o{ EXPERIMENT_TARGETING_SELECTION : referencia
    EXPERIMENT ||--o{ AD_SET : segmenta
    EXPERIMENT ||--|| EXPERIMENT_ADSET_WORKFLOW : orquestra
    EXPERIMENT_ADSET_WORKFLOW ||--o{ EXPERIMENT_ADSET_SPEC : gera
    EXPERIMENT ||--o{ LANDING_PAGE : direciona

    EXPERIMENT ||--o{ FACEBOOK_ADS_CAMPAIGN : publica
    FACEBOOK_ADS_CAMPAIGN ||--o{ FACEBOOK_ADS_AD_SET : contem
    FACEBOOK_ADS_AD_SET ||--o{ FACEBOOK_ADS_AD : contem
    FACEBOOK_ADS_AD_CREATIVE ||--o{ FACEBOOK_ADS_AD : compoe

    EXPERIMENT ||--o{ LEAD_PORTAL_FLOW : usa
    LEAD_PORTAL_FLOW ||--o{ LEAD_PORTAL_SUBMISSION : recebe
    EXPERIMENT ||--o{ LEAD_PORTAL_SUBMISSION : atribui

    AD_SET ||--o{ METRIC_SNAPSHOT : mede
    CREATIVE ||--o{ METRIC_SNAPSHOT : mede
```

## Atualizações Fase 2 — Prova e Oferta Normalizadas

### Provas catalogadas (`proof_artifact`)
- Novo repositório central para provas com colunas `stage`, `status`, `custom_type`, `asset_plan`, `asset_url`, `message`, `delivery_notes`, `prompt` e `model`.
- Ligações opcionais com `hypothesis`, `experiment`, `visual_proof` e `market_niche` permitem reaproveitar o mesmo ativo em diferentes hipóteses/experimentos.
- Cada prova registrada pode ser aplicada diretamente no formulário do framework, reduzindo campos livres e mantendo rastreabilidade.

### Oferta componível (`deliverable_package` ↔ `hypothesis`)
- `deliverable_package` agora aceita `hypothesis_id`, permitindo criar pacotes aprovados antes ou fora de um experimento.
- `hypothesis` ganhou o campo `offer_package_id`, apontando qual pacote oficial representa a oferta ativa daquela hipótese.
- Durante o cadastro/edição o sistema valida se o pacote pertence ao mesmo nicho, evitando combinações incoerentes.

## Escopo coberto


## Leitura rápida das relações

1. **Base estratégica**
   - `market_niche` e `hypothesis` definem o problema/oportunidade testada.
   - `experiment` centraliza a execução e conecta orçamento, período e status.

2. **Públicos e segmentação**
   - `target_audience` nasce de nicho/hipótese e pode ser aprovado antes da mídia.
   - `ad_set` materializa segmentação e orçamento no contexto do experimento.
   - `experiment_adset_workflow` + `experiment_adset_spec` coordenam o playbook automático dos três públicos (slots Designers, Marketing e SMB) e sinalizam quando todos estão com status READY.

3. **Criativos**
   - `creative` guarda peças geradas (com `model` e `prompt`).
   - `creative_variant` representa variações de assets/títulos/descrições.

4. **Lead Portal e geração de imagens**
   - `lead_portal_flow` replica o modelo (`image_prompt_model`) e o tamanho do lote (`image_prompt_batch_size`) definidos no experimento vinculado.
   - Ao atualizar `experiment.image_generation_model` ou `experiment.images_per_package`, os valores são sincronizados e publicados automaticamente para o Lead Portal, garantindo que os pacotes usem exatamente o modelo e a quantidade configurados no experimento.

4. **Campanha (Meta Ads)**
   - `facebook_ads_campaign` referencia diretamente o experimento.
   - Cada campanha possui `facebook_ads_ad_set` e depois `facebook_ads_ad`.
   - `facebook_ads_ad_creative` é vinculado ao anúncio publicado.

5. **Conversão e medição**
   - `lead_portal_flow` e `lead_portal_submission` registram captação de leads.
   - `metric_snapshot` consolida desempenho por criativo + ad set.

### Métricas do Portal do Lead

- `flow_access` guarda cada visita ao fluxo (data/hora, IP, visitor_id quando disponível) e permite contar quantas pessoas visualizaram o formulário do portal.
- `lead_portal_submission` e `flow_submissions` concentram os envios efetivos; a aplicação consolida contatos únicos por experimento para saber quantos completaram o preenchimento.
- Esses agregados são expostos via `LeadPortalMetricsService` e abastecem telas operacionais (por exemplo, `/facebook-campaigns`) com as métricas "Form visto" e "Form enviado".

### Controle de liberação para o Facebook Ads Worker

- O campo `experiment.facebook_release_requested_at` registra quando o operador clicou em **Liberar para o Facebook Ads Worker**.
- `experiment.funnel_reset_at` guarda o carimbo de data/hora do último reset manual realizado diretamente na aba Funil, garantindo que eventos anteriores fiquem ocultos nos resumos.
- Somente experimentos com `status = PLANNED` **e** essa coluna preenchida entram na fila `/api/facebook-campaigns/experiments-ready`.
- A data registrada também serve como baseline do funil: eventos automáticos e manuais anteriores a este instante deixam de ser contabilizados, garantindo que os testes feitos antes da liberação não contaminem os números oficiais.


## Observações de implementação

- Cada registro de `EXPERIMENT` guarda agora `stage`, `primary_variable` e `primary_metric`.
  - `stage` representa a etapa do funil (AD, LANDING, SAMPLE ou SALES) e direciona o que está sendo testado.
  - `primary_variable` descreve o ângulo/variável do experimento em linguagem natural.
  - `primary_metric` registra qual indicador decide o sucesso da hipótese e deve ser tratado como texto legível (ex.: "CTR de link (%)").
- O endpoint `/api/experiment-playbook` provê o playbook canônico por etapa com descrições e sugestões para preencher esses campos.
- Registros gerados por processos do Worker IA devem manter `model` e `prompt`
  preenchidos nos objetos aplicáveis (ex.: público, criativo, ad set, fluxo).
- O experimento funciona como eixo de rastreabilidade entre planejamento,
  geração por IA, publicação de mídia e métricas.


## Atualização do fluxo simples de público

- `market_niche` agora mantém listas curadas (`interest_list`, `role_list`, `behavior_list`).
- `experiment_targeting_selection` registra as escolhas feitas na aba de segmentação do experimento.
- O disparo do fluxo simples cria uma `targeting_request` interna para resolver os códigos da Meta Ads.
- `targeting_request` agora possui `experiment_id` (FK) para rastrear execuções por experimento e permitir expor status/erros operacionais diretamente na UI.

## Funil de vendas do experimento

Para cada experimento passamos a acompanhar um funil operacional padronizado de
nove etapas. As etapas são mantidas em `ExperimentFunnelStage` (código) e os
logs ficam em `experiment_funnel_event`:

- `VISUALIZACAO_ANUNCIO`
- `ACESSO_FORM_LEAD`
- `VISUALIZACAO_FORM`
- `ENVIO_FORM`
- `ABERTURA_EMAIL_AMOSTRA`
- `ACESSO_CHECKOUT`
- `COMPRA`
- `ABERTURA_EMAIL_COMPRA`
- `DOWNLOAD_MATERIAL_PAGO`

A tabela `experiment_funnel_event` guarda eventos manuais/forçados com as
colunas: `id`, `experiment_id` (FK), `lead_id` (FK opcional), `stage`, `source`,
`campaign_code`, `payload` e `occurred_at`.

- O campo `campaign_code` recebe o valor enviado pelo Lead Portal via parâmetro `campaign`/`utm_campaign`, permitindo atribuir cada etapa às referências de anúncio exibidas na UI.

Além dos eventos explícitos, o backend consolida fontes automáticas por etapa:

- Impressões e cliques: `experiment_campaign_metric`.
- Visualização de formulário: `flow_access` associado ao `lead_portal_flow.slug`.
- Envio de formulário: `lead_portal_submission` (campo `experiment_id`).
- Abertura do e-mail de amostra e download pago: `flow_submission_image_package`
  (campos `email_opened_at` e `images_viewed_at` com `payment_purchase_id`).
- Checkout: `lead_portal_purchase.checkout_accessed_at`, atualizado pelo endpoint público
  `/api/public/lead-portal/purchases/{id}/checkout` antes do redirecionamento ao entrypoint de pagamento.
- Compra: `lead_portal_purchase` (com filtro `payment_approved_at`/`mp_status`).
- Abertura do e-mail de compra: `lead_portal_premium_delivery` associado a
  `email_log.opened_at`.

O endpoint `/api/experiments/{id}/funnel` retorna o agregado por etapa
(`autoCount`, `manualCount`, `totalCount`, `uniqueCount` e `lastEventAt`) usado
na aba **Funil de vendas** da UI.

## Relatórios objetivos de experimento

### EXPERIMENT_REPORT_REQUEST

Tabela responsável por registrar cada solicitação de relatório vinculada a um experimento.

| Campo | Tipo | Descrição |
| --- | --- | --- |
| id | BIGINT PK | Identificador da solicitação |
| experiment_id | BIGINT FK | Referência para `EXPERIMENT` |
| status | VARCHAR(32) | Fila de processamento (`PENDING`, `PROCESSING`, `READY`, `FAILED`) |
| requested_at | DATETIME(6) | Momento em que a coleta foi disparada |
| completed_at | DATETIME(6) | Momento em que a solicitação foi concluída (sucesso ou falha) |
| requested_by | VARCHAR(191) | Identificação livre fornecida no front |
| download_url | VARCHAR(512) | Link do relatório final disponibilizado por serviços externos |
| payload_snapshot | LONGTEXT | JSON com o pacote consolidado de dados do experimento |
| failure_reason | LONGTEXT | Descrição de erro quando `status = FAILED` |
| created_at / updated_at | DATETIME(6) | Auditoria padrão |

**Relacionamentos**

- `EXPERIMENT (1) --- (N) EXPERIMENT_REPORT_REQUEST`: permite múltiplas solicitações históricas por experimento.
- Índices compostos em `(experiment_id, requested_at)` e `(status, requested_at)` garantem listagens rápidas tanto no painel do usuário quanto para os workers que processam a fila.

O `payload_snapshot` armazena o mesmo JSON exposto pelo endpoint `/api/experiments/{id}/report-material`, garantindo que serviços externos tenham acesso estável às informações utilizadas para montar o relatório objetivo.

## Aprendizado fechado do experimento

Para sustentar a Fase 3 do framework, foram criadas duas novas tabelas vinculadas ao contexto do experimento:

- **EXPERIMENT_LEARNING_REQUEST**: registra cada solicitação de aprendizado automático.
  - `experiment_id` (FK) aponta para `EXPERIMENT` e garante cascata ao excluir.
  - `status` usa os valores `PENDING`, `PROCESSING`, `READY`, `FAILED`.
  - `payload_snapshot` guarda um snapshot JSON do experimento (mesmo material usado no relatório objetivo).
  - `result_payload` mantém o JSON estruturado retornado pelo worker para auditoria.
  - `failure_reason`, `requested_by`, `requested_at` e `completed_at` permitem rastreio operacional.

- **EXPERIMENT_LEARNING**: armazena o aprendizado consolidado (o que funcionou, o que travou e o próximo teste).
  - `experiment_id` e `request_id` (FK) preservam a ligação com a solicitação que originou o aprendizado.
  - `niche_id` e `hypothesis_id` facilitam consultas por nicho/hipótese sem joins adicionais.
  - `stage`, `primary_metric` e `metric_signal` sinalizam em qual etapa do funil o aprendizado se aplica.
  - `what_worked`, `what_blocked`, `next_test` e `summary` mantêm os textos principais.
  - `insights_json` e `suggestions_json` armazenam, em JSON, listas tipadas de insights (Dor/Resultado/Mecanismo/Prova/Oferta) e recomendações de backlog.

Essas tabelas alimentam a nova API de "Banco de aprendizados" e o recomendador de backlog por nicho. Cada novo aprendizado nasce de uma `EXPERIMENT_LEARNING_REQUEST` consumida pelo AI Worker, garantindo rastreabilidade e versionamento dos insights.
