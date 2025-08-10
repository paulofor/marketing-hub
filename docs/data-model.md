# MarketingHub Data Model

This document summarizes the current database schema defined in `schema.sql`.

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
- `niche` VARCHAR(255)
- `avatar` VARCHAR(255)
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
- `funnel` LONGTEXT
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
- `base_segmentation` LONGTEXT
- `interests` LONGTEXT
- `demographic_filters` LONGTEXT
- `extra_tips` LONGTEXT
- `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
- `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP

### experiment

- `id` BIGINT AUTO_INCREMENT PRIMARY KEY
- `niche_id` BIGINT NOT NULL
- `name` VARCHAR(255) NOT NULL
- `hypothesis` VARCHAR(255)
- `hypothesis_id` BINARY(16)
- `kpi_target_cpl` DECIMAL(10,2) DEFAULT 45.00
- `stop_loss_cpl` DECIMAL(10,2) DEFAULT 90.00
- `sample_size` INT DEFAULT 1500
- `baseline_cvr` DECIMAL(5,2) DEFAULT 3.00
- `target_cvr` DECIMAL(5,2) DEFAULT 5.00
- `mde_percent` DECIMAL(5,2) DEFAULT 40.0
- `start_date` DATE
- `end_date` DATE
- `status` VARCHAR(20)
- `platform` VARCHAR(50)
- `metric_preset_id` VARCHAR(50)
- `sales_funnel_id` BINARY(16)
- `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
- `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP

### creative_variant

- `id` BIGINT AUTO_INCREMENT PRIMARY KEY
- `experiment_id` BIGINT
- `type` VARCHAR(20)
- `asset_url` VARCHAR(500)
- `titles` LONGTEXT
- `descriptions` LONGTEXT
- `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
- `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP

### ad_set

- `id` BIGINT AUTO_INCREMENT PRIMARY KEY
- `experiment_id` BIGINT
- `location` VARCHAR(255)
- `interests` LONGTEXT
- `lookalikes` LONGTEXT
- `budget` DECIMAL(10,2)
- `duration_days` INT
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

### hypothesis

- `id` BINARY(16) PRIMARY KEY
- `market_niche_id` BIGINT NOT NULL
- `title` VARCHAR(255) NOT NULL
- `premise_angle_id` BIGINT NOT NULL
- `offer_type` VARCHAR(20) NOT NULL
- `price` DECIMAL(6,2)
- `kpi_target_cpl` DECIMAL(7,2) NOT NULL
- `status` VARCHAR(20) DEFAULT 'BACKLOG' NOT NULL
- `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
- `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP

### lead

- `id` BINARY(16) PRIMARY KEY
- `leadgen_id` BIGINT UNIQUE
- `instagram_user_id` BIGINT
- `ad_id` BIGINT
- `campaign_id` BIGINT
- `experiment_id` BIGINT
- `captured_at` DATETIME
- `nurture_stage` ENUM('NEW','WARM','HOT') DEFAULT 'NEW'
- `cpl` DECIMAL(10,2)
- `lead_score` INT DEFAULT 0

### outbox

- `id` BIGINT AUTO_INCREMENT PRIMARY KEY
- `aggregate_id` BINARY(16)
- `event_type` VARCHAR(50)
- `payload` TEXT
- `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP
- `processed_at` DATETIME

### sales_funnel

- `id` BINARY(16) PRIMARY KEY
- `name` VARCHAR(100)
- `objective` VARCHAR(255)
- `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP

### funnel_step

- `id` BINARY(16) PRIMARY KEY
- `funnel_id` BINARY(16) NOT NULL
- `order_idx` INT
- `stimulus_type` ENUM('DM','IG_POST_BOOST','FB_AD','WHATSAPP','EMAIL','SMS','PUSH','STORY','WEBINAR','CALL','LANDING')
- `channel` VARCHAR(50)
- `template_id` VARCHAR(50)
- `expected_action` ENUM('OPEN','CLICK','REPLY','VIEW','PURCHASE','REGISTRATION','OPT_IN','OPT_OUT','BOUNCE','SHARE')
- `score_inc` INT
- `revenue_target` DECIMAL(10,2)
- `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
- `is_active` TINYINT(1) DEFAULT 1

### lead_response

- `id` BIGINT AUTO_INCREMENT PRIMARY KEY
- `lead_id` BINARY(16) NOT NULL
- `funnel_step_id` BINARY(16) NOT NULL
- `action` ENUM('OPEN','CLICK','REPLY','VIEW','PURCHASE','REGISTRATION','OPT_IN','OPT_OUT','BOUNCE','SHARE')
- `value` JSON
- `revenue` DECIMAL(10,2)
- `occurred_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP

### step_metric_snapshot

- `id` BIGINT AUTO_INCREMENT PRIMARY KEY
- `funnel_step_id` BINARY(16) NOT NULL
- `impressions` BIGINT
- `responses` BIGINT
- `conversions` BIGINT
- `revenue` DECIMAL(12,2)
- `gross_profit` DECIMAL(12,2)
- `cvr` DECIMAL(6,4)
- `captured_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP

### metric_preset

- `id` VARCHAR(50) PRIMARY KEY
- `name` VARCHAR(100)
- `sample_size` INT
- `stop_loss_factor` DECIMAL(5,2)
- `default_mde_pp` DECIMAL(5,2)

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
    HYPOTHESIS {
        BINARY id PK
        BIGINT market_niche_id FK
    }
    EXPERIMENT {
        BIGINT id PK
        BIGINT niche_id FK
        BINARY hypothesis_id FK
        VARCHAR metric_preset_id FK
        BINARY sales_funnel_id FK
    }
    CREATIVE_VARIANT {
        BIGINT id PK
        BIGINT experiment_id FK
    }
    AD_SET {
        BIGINT id PK
        BIGINT experiment_id FK
    }
    METRIC_SNAPSHOT {
        BIGINT id PK
        BIGINT creative_id FK
        BIGINT ad_set_id FK
    }
    LANDING_PAGE {
        BIGINT id PK
        BIGINT experiment_id FK
    }
    LEAD {
        BINARY id PK
        BIGINT experiment_id FK
    }
    OUTBOX {
        BIGINT id PK
    }
    CHAT_SESSION {
        BIGINT id PK
    }
    CHAT_MESSAGE {
        BIGINT id PK
        BIGINT session_id FK
    }
    SALES_FUNNEL {
        BINARY id PK
    }
    FUNNEL_STEP {
        BINARY id PK
        BINARY funnel_id FK
    }
    LEAD_RESPONSE {
        BIGINT id PK
        BINARY lead_id FK
        BINARY funnel_step_id FK
    }
    STEP_METRIC_SNAPSHOT {
        BIGINT id PK
        BINARY funnel_step_id FK
    }
    METRIC_PRESET {
        VARCHAR id PK
    }

    MARKET_NICHE ||--o{ HYPOTHESIS : guides
    HYPOTHESIS ||--o{ EXPERIMENT : informs
    MARKET_NICHE ||--o{ EXPERIMENT : contains
    EXPERIMENT ||--o{ CREATIVE_VARIANT : has
    EXPERIMENT ||--o{ AD_SET : configures
    EXPERIMENT ||--o{ LANDING_PAGE : uses
    EXPERIMENT ||--o{ LEAD : generates
    SALES_FUNNEL ||--o{ EXPERIMENT : used_by
    SALES_FUNNEL ||--o{ FUNNEL_STEP : includes
    FUNNEL_STEP ||--o{ LEAD_RESPONSE : triggers
    LEAD ||--o{ LEAD_RESPONSE : receives
    FUNNEL_STEP ||--o{ STEP_METRIC_SNAPSHOT : measures
    CREATIVE_VARIANT ||--o{ METRIC_SNAPSHOT : reports
    AD_SET ||--o{ METRIC_SNAPSHOT : tracks
    METRIC_PRESET ||--o{ EXPERIMENT : configures
    CHAT_SESSION ||--o{ CHAT_MESSAGE : includes
```
