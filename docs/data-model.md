# MarketingHub Data Model

This document summarizes the current database schema defined in `schema.sql`.
It also highlights the tables used by the [Facebook Ads Worker](../facebook-ads-worker/README.md)
for managing campaigns and tracking their performance.

For a mapping between frontend screens and these entities, see [Frontend Screens and Entities](./frontend-screens-entities.md).

## Tables

### asset

- `id` BIGINT AUTO_INCREMENT PRIMARY KEY
- `type` VARCHAR(20)
- `provider` VARCHAR(20)
- `external_id` VARCHAR(100)
- `status` VARCHAR(20)
- `url` VARCHAR(500)
- `payload` TEXT
- `campaign_id` BIGINT
- `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
- `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP

### course_plan

- `id` BIGINT AUTO_INCREMENT PRIMARY KEY
- `target_audience` VARCHAR(255)
- `transformation` VARCHAR(255)
- `macro_topics` TEXT
- `modules` TEXT
- `objectives` TEXT
- `resources` TEXT
- `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
- `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP

### ai_service

- `id` BIGINT AUTO_INCREMENT PRIMARY KEY
- `name` VARCHAR(255)
- `objective` LONGTEXT
- `url` VARCHAR(255)
- `phase` VARCHAR(255)
- `price` DECIMAL(10,2)
- `cost` DECIMAL(10,2)
- `observation` LONGTEXT
- `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
- `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP

### product

- `id` BIGINT AUTO_INCREMENT PRIMARY KEY
- `niche` VARCHAR(255)
- `avatar` VARCHAR(255)
- `instagram_account_id` BIGINT
- `explicit_pain` LONGTEXT
- `promise` LONGTEXT
- `unique_mechanism` LONGTEXT
- `tripwire` LONGTEXT
- `risk_reversal` LONGTEXT
- `social_proof` LONGTEXT
- `checkout_monetization` LONGTEXT
- `funnel` LONGTEXT
- `creative_volume` LONGTEXT
- `storytelling` LONGTEXT
- `ai_cost` DECIMAL(10,2) DEFAULT 0
- `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
- `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP

### success_product

- `id` BIGINT AUTO_INCREMENT PRIMARY KEY
- `description` LONGTEXT
- `name` VARCHAR(255)
- `novo` BOOLEAN DEFAULT TRUE
- `platform` VARCHAR(20) NOT NULL DEFAULT 'COFRE'
- `generate_niche_hypothesis` BOOLEAN DEFAULT FALSE
- `niche` VARCHAR(255)
- `avatar` VARCHAR(255)
- `audience_type` VARCHAR(255)
- `sales_page_url` VARCHAR(500)
- `instagram_url` VARCHAR(500)
- `facebook_url` VARCHAR(500)
- `youtube_url` VARCHAR(500)
- `instagram_account_id` BIGINT
- `explicit_pain` LONGTEXT
- `promise` LONGTEXT
- `unique_mechanism` LONGTEXT
- `tripwire` LONGTEXT
- `risk_reversal` LONGTEXT
- `social_proof` LONGTEXT
- `checkout_monetization` LONGTEXT
- `sales_funnel` LONGTEXT
- `creative_volume` LONGTEXT
- `storytelling` LONGTEXT
- `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
- `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP

### instagram_post

- `id` BIGINT AUTO_INCREMENT PRIMARY KEY
- `instagram_account_id` BIGINT
- `caption` TEXT
- `media_url` VARCHAR(500)
- `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
- `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP

### market_niche

- `id` BIGINT AUTO_INCREMENT PRIMARY KEY
- `name` VARCHAR(255)
- `description` LONGTEXT
- `demand_volume` LONGTEXT
- `promises` LONGTEXT
- `offers` LONGTEXT
- `hypotheses_to_generate` INT
- `audiences_to_generate` INT
- `base_segmentation` LONGTEXT
- `interests` LONGTEXT
- `demographic_filters` LONGTEXT
- `extra_tips` LONGTEXT
- `chat_dialog_id` BIGINT
- `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
- `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP

### hypothesis

- `id` BINARY(16) PRIMARY KEY
- `experiment_id` BIGINT NOT NULL
- `market_niche_id` BIGINT NOT NULL
- `title` VARCHAR(255) NOT NULL
- `premise_angle_id` BIGINT NOT NULL
- `offer_type` VARCHAR(20) NOT NULL
- `entrega` LONGTEXT
- `price` DECIMAL(6,2)
- `kpi_target_cpl` DECIMAL(7,2) NOT NULL
- `status` VARCHAR(20) DEFAULT 'BACKLOG' NOT NULL
- `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
- `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP

### audience

- `id` BIGINT AUTO_INCREMENT PRIMARY KEY
- `name` VARCHAR(255)
- `description` LONGTEXT
- `prompt` LONGTEXT
- `model` VARCHAR(255)
- `approved` BOOLEAN DEFAULT FALSE
- `market_niche_id` BIGINT
- `hypothesis_id` BINARY(16)
- `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
- `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP

Tracks the audiences generated for each market niche or hypothesis. The
`approved` flag lets the marketing team validate individual segments before
triggering ad set generation.

### experiment

- `id` BIGINT AUTO_INCREMENT PRIMARY KEY
- `niche_id` BIGINT NOT NULL
- `hypothesis_id` BINARY(16) NOT NULL
- `name` VARCHAR(255) NOT NULL
- `hypothesis` VARCHAR(255)
- `facebook_page_id` BIGINT
- `facebook_instant_form_id` BIGINT
- `instagram_account_id` BIGINT
- `kpi_target_cpl` DECIMAL(10,2) DEFAULT 45.00
- `stop_loss_cpl` DECIMAL(10,2) DEFAULT 90.00
- `sample_size` INT DEFAULT 1500
- `baseline_cvr` DECIMAL(5,2) DEFAULT 3.00
- `target_cvr` DECIMAL(5,2) DEFAULT 5.00
- `mde_percent` DECIMAL(5,2) DEFAULT 40.0
- `creatives_to_generate` INT
- `start_date` DATE
- `end_date` DATE
- `status` VARCHAR(20)
- `platform` VARCHAR(50)
- `creative_approved` BOOLEAN DEFAULT FALSE
- `journey_template_id` BIGINT NOT NULL
- `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
- `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP

Defines a marketing experiment for a specific niche and hypothesis. Each
experiment aggregates the creative variants, ad sets and landing pages that
will be executed and measured during the test cycle.

**Relationships**

- `facebook_page_id` → FK `fb_page.id`: optional link that identifies the
  Facebook Page to be used when publishing campaigns for the experiment. When
  defined, the worker can override the account default and push ads using the
  selected page metadata.
- `facebook_instant_form_id` → FK `fb_instant_form.id`: when present, the
  experiment reuses a Meta Instant Form that was planned for the same
  hypothesis, ensuring multiple campaigns can capture leads with the same
  optimized flow.
- `journey_template_id` → FK `journey_template.id`: vínculo obrigatório
  que reaproveita o blueprint de jornada aprovado para nortear campanhas
  e ativações omnichannel geradas a partir do experimento.

### fb_page

- `id` BIGINT AUTO_INCREMENT PRIMARY KEY
- `account_id` BIGINT NOT NULL → FK `fb_account.id`
- `page_id` VARCHAR(128) NOT NULL
- `name` VARCHAR(255) NOT NULL

Stores the Facebook Pages that were authorized for each Ads account. The
combination `(account_id, page_id)` is unique, ensuring the same page is not
registered twice for the same account. Experiments reference this table through
`experiment.facebook_page_id` when a campaign must run on a specific page.

### fb_instant_form

- `id` BIGINT AUTO_INCREMENT PRIMARY KEY
- `hypothesis_id` BINARY(16) NOT NULL → FK `hypothesis.id`
- `page_id` BIGINT NOT NULL → FK `fb_page.id`
- `form_id` VARCHAR(128) NOT NULL UNIQUE
- `name` VARCHAR(255) NOT NULL
- `status` VARCHAR(50)
- `locale` VARCHAR(12)
- `leads_count` BIGINT
- `created_time` DATETIME
- `updated_time` DATETIME
- `follow_up_action_url` VARCHAR(512)
- `privacy_policy_url` VARCHAR(512)
- `model` VARCHAR(128)
- `prompt` LONGTEXT
- `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP
- `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP

Captures the configuration of Meta Instant Forms (Lead Ads forms) planned for a
hypothesis. Each record stores the metadata returned by the Facebook Marketing
API, including locale, status and lead counts, and tracks the AI worker
prompt/model responsible for the form generation. Experiments can reuse these
forms through `experiment.facebook_instant_form_id` to keep capture journeys
consistent across tests.

### creative

- `id` BIGINT AUTO_INCREMENT PRIMARY KEY
- `experiment_id` BIGINT NOT NULL
- `headline` VARCHAR(255)
- `primary_text` VARCHAR(255)
- `image_url` VARCHAR(500)
- `ad_format` VARCHAR(32)
- `description` VARCHAR(255)
- `call_to_action` VARCHAR(32)
- `destination_url` VARCHAR(512)
- `instagram_user_id` VARCHAR(64)
- `image_hash` VARCHAR(255)
- `video_id` VARCHAR(255)
- `status` VARCHAR(20)

Stores ad creatives generated by the AI Worker for each experiment.
These records can later be sent to the Facebook API and are displayed on
the experiment detail page in the frontend.

### creative_variant

- `id` BIGINT AUTO_INCREMENT PRIMARY KEY
- `experiment_id` BIGINT
- `type` VARCHAR(20)
- `asset_url` VARCHAR(500)
- `titles` LONGTEXT
- `descriptions` LONGTEXT
- `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
- `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP

Stores the individual assets generated for an experiment. The
`experiment_id` column is a foreign key to `experiment.id` and is optional to
allow variants to be created before an experiment is defined.

### ad_set

- `id` BIGINT AUTO_INCREMENT PRIMARY KEY
- `experiment_id` BIGINT
- `location` VARCHAR(255)
- `interests` LONGTEXT
- `lookalikes` LONGTEXT
- `targeting_json` LONGTEXT
- `budget` DECIMAL(10,2)
- `duration_days` INT
- `prompt` LONGTEXT
- `model` VARCHAR(255)
- `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
- `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP

### metric_snapshot

- `id` BIGINT AUTO_INCREMENT PRIMARY KEY
- `creative_id` BIGINT
- `ad_set_id` BIGINT
- `impressions` INT
- `clicks` INT
- `cost` DECIMAL(10,2)
- `roas` DECIMAL(10,2)
- `ctr` DOUBLE
- `cpa` DECIMAL(10,2)
- `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP

### landing_page

- `id` BIGINT AUTO_INCREMENT PRIMARY KEY
- `experiment_id` BIGINT NOT NULL
- `url` VARCHAR(500)
- `type` VARCHAR(20)
- `status` VARCHAR(20)
- `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
- `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP

### chat_session

- `id` BIGINT AUTO_INCREMENT PRIMARY KEY
- `user_id` VARCHAR(255)
- `channel` VARCHAR(50)
- `state` VARCHAR(20)
- `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
- `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP

### chat_message

- `id` BIGINT AUTO_INCREMENT PRIMARY KEY
- `session_id` BIGINT
- `origin` VARCHAR(50)
- `content` LONGTEXT
- `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
- `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP

### chat_dialog

- `id` BIGINT AUTO_INCREMENT PRIMARY KEY
- `url` VARCHAR(500)
- `description` LONGTEXT
- `theme` VARCHAR(255)
- `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
- `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP

### prompt_entity

- `id` BIGINT AUTO_INCREMENT PRIMARY KEY
- `name` VARCHAR(255) UNIQUE
- `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
- `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP

### prompt_attribute

- `id` BIGINT AUTO_INCREMENT PRIMARY KEY
- `prompt_entity_id` BIGINT
- `name` VARCHAR(255)
- `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
- `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP

### prompt_entity_description

- `id` BIGINT AUTO_INCREMENT PRIMARY KEY
- `prompt_entity_id` BIGINT
- `description` LONGTEXT
- `active` BOOLEAN
- `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
- `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP

### prompt_attribute_description

- `id` BIGINT AUTO_INCREMENT PRIMARY KEY
- `prompt_attribute_id` BIGINT
- `description` LONGTEXT
- `active` BOOLEAN
- `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
- `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP

### hypothesis_prompt_attribute_description

- `hypothesis_id` BINARY(16)
- `prompt_attribute_description_id` BIGINT

### facebook_ads_campaign

- `id` CHAR(36) PRIMARY KEY
- `external_id` VARCHAR(64)
- `ad_account_id` VARCHAR(64) NOT NULL
- `experiment_id` BIGINT NOT NULL → FK `experiment.id`
- `facebook_account_id` BIGINT NOT NULL → FK `fb_account.id`
- `name` VARCHAR(255) NOT NULL
- `objective` VARCHAR(64) NOT NULL
- `status` ENUM(PAUSED,ACTIVE,ARCHIVED,DELETED) DEFAULT "PAUSED"
- `budget_mode` ENUM("CAMPAIGN","ADSET") NOT NULL
- `daily_budget_minor` BIGINT
- `lifetime_budget_minor` BIGINT
- `api_version` VARCHAR(16)
- `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
- `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP

### facebook_ads_campaign_special_ad_category

- `campaign_id` CHAR(36)
- `category` ENUM(NONE,CREDIT,EMPLOYMENT,HOUSING,ISSUES_ELECTIONS_POLITICS)
- `PRIMARY KEY` (`campaign_id`, `category`)

### facebook_ads_campaign_special_ad_country

- `campaign_id` CHAR(36)
- `country_iso2` CHAR(2)
- `PRIMARY KEY` (`campaign_id`, `country_iso2`)

### facebook_ads_ad_set

- `id` CHAR(36) PRIMARY KEY
- `external_id` VARCHAR(64)
- `campaign_id` CHAR(36)
- `name` VARCHAR(255)
- `status` ENUM(PAUSED,ACTIVE,ARCHIVED,DELETED) DEFAULT "PAUSED"
- `daily_budget_minor` BIGINT
- `lifetime_budget_minor` BIGINT
- `start_time` DATETIME
- `end_time` DATETIME
- `billing_event` VARCHAR(32)
- `optimization_goal` VARCHAR(64)
- `bid_strategy` VARCHAR(64)
- `bid_amount_minor` BIGINT
- `promoted_object_json` LONGTEXT
- `targeting_json` LONGTEXT
- `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
- `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP

### facebook_ads_media_asset

- `id` CHAR(36) PRIMARY KEY
- `kind` ENUM(IMAGE,VIDEO)
- `source_uri` VARCHAR(1024)
- `image_hash` VARCHAR(128)
- `video_id` VARCHAR(64)
- `width` INT
- `height` INT
- `duration_ms` INT
- `checksum` VARCHAR(128)
- `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP

### facebook_ads_ad_creative

- `id` CHAR(36) PRIMARY KEY
- `external_id` VARCHAR(64)
- `page_id` VARCHAR(64)
- `instagram_user_id` VARCHAR(64)
- `kind` ENUM(LINK,VIDEO,CAROUSEL)
- `link_data_json` LONGTEXT
- `video_data_json` LONGTEXT
- `carousel_data_json` LONGTEXT
- `last_preview_url` VARCHAR(1024)
- `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
- `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP

### facebook_ads_ad

- `id` CHAR(36) PRIMARY KEY
- `external_id` VARCHAR(64)
- `adset_id` CHAR(36)
- `name` VARCHAR(255)
- `creative_id` CHAR(36)
- `status` ENUM(PAUSED,ACTIVE,ARCHIVED,DELETED) DEFAULT "PAUSED"
- `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
- `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP

### facebook_ads_ad_tracking_utm

- `ad_id` CHAR(36) PRIMARY KEY
- `utm_source` VARCHAR(64)
- `utm_medium` VARCHAR(64)
- `utm_campaign` VARCHAR(128)
- `utm_content` VARCHAR(128)
- `utm_term` VARCHAR(128)

### fb_account

- `id` BIGINT AUTO_INCREMENT PRIMARY KEY
- `name` VARCHAR(255)
- `currency` VARCHAR(10)
- `access_token` LONGTEXT
- `token_expires_at` DATETIME
- `token_last_refreshed_at` DATETIME
- `authorized_user_id` VARCHAR(128)
- `authorized_user_name` VARCHAR(255)
- `authorized_user_email` VARCHAR(320)
- `app_id` VARCHAR(255)
- `app_secret` LONGTEXT
- `token_renewal_enabled` TINYINT(1) DEFAULT 0
- `token_renewal_status` VARCHAR(40)
- `token_renewal_last_attempt_at` DATETIME
- `token_renewed_at` DATETIME
- `token_renewal_last_error` LONGTEXT
- `ad_account_id` VARCHAR(64)
- `default_page_id` VARCHAR(128)
- `default_website_url` VARCHAR(512)
- `default_instagram_actor_id` VARCHAR(64)
- `default_creative_message_template` VARCHAR(255)
- `default_call_to_action_type` VARCHAR(64)
- `ad_set_daily_budget` VARCHAR(32)
- `ad_set_billing_event` VARCHAR(64)
- `ad_set_optimization_goal` VARCHAR(64)
- `ad_set_destination_type` VARCHAR(64)
- `ad_set_bid_strategy` VARCHAR(64)
- `ad_set_bid_amount` VARCHAR(32)
- `ad_set_target_country` VARCHAR(32)
- `worker_enabled` TINYINT(1) DEFAULT 0

Registra as credenciais do Facebook Ads configuradas no backend. Quando a
renovação automática está habilitada (`token_renewal_enabled = 1`), o Facebook
Ads Worker monitora `token_expires_at` e solicita um novo token de longa duração
antes do vencimento. Cada tentativa atualiza `token_renewal_status`,
`token_renewal_last_attempt_at`, `token_renewed_at` e armazena mensagens de erro
em `token_renewal_last_error` quando a renovação falha. A conta marcada como
`worker_enabled = 1` é exposta pelo endpoint `GET /api/accounts/facebook/worker-config`
e fornece os parâmetros padrão utilizados pelo `facebook-ads-worker` (ID da conta
de anúncios, token, App ID/Secret, página fallback, orçamento diário etc.).

### ig_account

- `id` BIGINT AUTO_INCREMENT PRIMARY KEY
- `name` VARCHAR(255)
- `handle` VARCHAR(255)
- `account_code` VARCHAR(255)

Centraliza as informações essenciais da conta de Instagram que são exibidas na
interface administrativa. O campo `handle` armazena o nome de usuário exibido
com o símbolo `@`, enquanto `account_code` guarda o identificador interno usado
pelas equipes de atendimento ou suporte. Informações adicionais de campanhas
devem ser fornecidas diretamente nos objetos responsáveis por cada fluxo.

## Diagram

```mermaid
erDiagram
    ASSET {
        BIGINT id PK
    }
    COURSE_PLAN {
        BIGINT id PK
    }
    AI_SERVICE {
        BIGINT id PK
    }
    PRODUCT {
        BIGINT id PK
    }
    SUCCESS_PRODUCT {
        BIGINT id PK
    }
    INSTAGRAM_POST {
        BIGINT id PK
    }
    MARKET_NICHE {
        BIGINT id PK
    }
    AUDIENCE {
        BIGINT id PK
    }
    EXPERIMENT {
        BIGINT id PK
    }
    CREATIVE_VARIANT {
        BIGINT id PK
    }
    AD_SET {
        BIGINT id PK
    }
    METRIC_SNAPSHOT {
        BIGINT id PK
    }
    LANDING_PAGE {
        BIGINT id PK
    }
    CHAT_SESSION {
        BIGINT id PK
    }
    CHAT_MESSAGE {
        BIGINT id PK
    }
    CHAT_DIALOG {
        BIGINT id PK
    }
    PROMPT_ENTITY {
        BIGINT id PK
    }
    PROMPT_ATTRIBUTE {
        BIGINT id PK
    }
    PROMPT_ENTITY_DESCRIPTION {
        BIGINT id PK
    }
    PROMPT_ATTRIBUTE_DESCRIPTION {
        BIGINT id PK
    }
    HYPOTHESIS {
        BINARY(16) id PK
    }
    HYPOTHESIS_PROMPT_ATTRIBUTE_DESCRIPTION {
        BINARY(16) hypothesis_id PK
        BIGINT prompt_attribute_description_id PK
    }

    FACEBOOK_ADS_CAMPAIGN {
        CHAR(36) id PK
    }
    FACEBOOK_ADS_AD_SET {
        CHAR(36) id PK
    }
    FACEBOOK_ADS_AD_CREATIVE {
        CHAR(36) id PK
    }
    FACEBOOK_ADS_AD {
        CHAR(36) id PK
    }
    FACEBOOK_ADS_AD_TRACKING_UTM {
        CHAR(36) ad_id PK
    }
    FB_ACCOUNT {
        BIGINT id PK
    }
    FB_PAGE {
        BIGINT id PK
    }

    FACEBOOK_ADS_CAMPAIGN ||--o{ FACEBOOK_ADS_AD_SET : contains
    FACEBOOK_ADS_AD_SET ||--o{ FACEBOOK_ADS_AD : includes
    FACEBOOK_ADS_AD }o--|| FACEBOOK_ADS_AD_CREATIVE : uses
    FACEBOOK_ADS_AD ||--|| FACEBOOK_ADS_AD_TRACKING_UTM : tracks

    MARKET_NICHE ||--o{ HYPOTHESIS : generates
    MARKET_NICHE ||--o{ EXPERIMENT : contains
    MARKET_NICHE ||--o{ AUDIENCE : has
    HYPOTHESIS ||--o{ EXPERIMENT : tests
    HYPOTHESIS ||--o{ AUDIENCE : defines
    EXPERIMENT ||--o{ CREATIVE_VARIANT : has
    EXPERIMENT ||--o{ AD_SET : configures
    EXPERIMENT ||--o{ LANDING_PAGE : uses
    CREATIVE_VARIANT ||--o{ METRIC_SNAPSHOT : reports
    AD_SET ||--o{ METRIC_SNAPSHOT : tracks
    CHAT_SESSION ||--o{ CHAT_MESSAGE : includes
    PROMPT_ENTITY ||--o{ PROMPT_ATTRIBUTE : defines
    PROMPT_ENTITY ||--o{ PROMPT_ENTITY_DESCRIPTION : described_by
    PROMPT_ATTRIBUTE ||--o{ PROMPT_ATTRIBUTE_DESCRIPTION : described_by
    HYPOTHESIS ||--o{ HYPOTHESIS_PROMPT_ATTRIBUTE_DESCRIPTION : uses
    PROMPT_ATTRIBUTE_DESCRIPTION ||--o{ HYPOTHESIS_PROMPT_ATTRIBUTE_DESCRIPTION : referenced_by
    FB_ACCOUNT ||--o{ FB_PAGE : owns
    FB_PAGE ||--o{ EXPERIMENT : assigned_to
```
