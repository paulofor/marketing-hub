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
      DATE start_date
      DATE end_date
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
    MARKET_NICHE ||--o{ EXPERIMENT : agrupa
    HYPOTHESIS ||--o{ EXPERIMENT : orienta

    MARKET_NICHE ||--o{ TARGET_AUDIENCE : possui
    HYPOTHESIS ||--o{ TARGET_AUDIENCE : refina

    EXPERIMENT ||--o{ CREATIVE : gera
    EXPERIMENT ||--o{ CREATIVE_VARIANT : detalha
    EXPERIMENT ||--o{ EXPERIMENT_TARGETING_SELECTION : seleciona
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

4. **Campanha (Meta Ads)**
   - `facebook_ads_campaign` referencia diretamente o experimento.
   - Cada campanha possui `facebook_ads_ad_set` e depois `facebook_ads_ad`.
   - `facebook_ads_ad_creative` é vinculado ao anúncio publicado.

5. **Conversão e medição**
   - `lead_portal_flow` e `lead_portal_submission` registram captação de leads.
   - `metric_snapshot` consolida desempenho por criativo + ad set.

## Observações de implementação

- Registros gerados por processos do Worker IA devem manter `model` e `prompt`
  preenchidos nos objetos aplicáveis (ex.: público, criativo, ad set, fluxo).
- O experimento funciona como eixo de rastreabilidade entre planejamento,
  geração por IA, publicação de mídia e métricas.


## Atualização do fluxo simples de público

- `market_niche` agora mantém listas curadas (`interest_list`, `role_list`, `behavior_list`).
- `experiment_targeting_selection` registra as escolhas feitas na aba de segmentação do experimento.
- O disparo do fluxo simples cria uma `targeting_request` interna para resolver os códigos da Meta Ads.
