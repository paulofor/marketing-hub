# MarketingHub Data Model

This document summarizes the database schema derived from the Liquibase change logs in `backend/ads-service/src/main/resources/db/changelog`.

## Tables

### hypothesis

- `id` BINARY(16) PRIMARY KEY
- `title` VARCHAR(255) NOT NULL
- `premise_angle_id` BIGINT NOT NULL
- `offer_type` VARCHAR(20) NOT NULL
- `price` DECIMAL(6,2)
- `kpi_target_cpl` DECIMAL(7,2) NOT NULL
- `status` VARCHAR(20) DEFAULT 'BACKLOG'
- `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
- `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
- `market_niche_id` BIGINT NOT NULL (FK `fk_hypothesis_niche`)

### experiment

Columns added via change logs:

- `hypothesis_id` BINARY(16) NOT NULL (FK `fk_experiment_hypothesis`)
- `kpi_target_cpl` DECIMAL(10,2) DEFAULT 45.00
- `stop_loss_cpl` DECIMAL(10,2) DEFAULT 90.00
- `sample_size` INT DEFAULT 1500
- `baseline_cvr` DECIMAL(5,2) DEFAULT 3.00
- `target_cvr` DECIMAL(5,2) DEFAULT 5.00
- `mde_percent` DECIMAL(5,2) DEFAULT 40.0
- `metric_preset_id` VARCHAR(50) (FK `fk_experiment_metric_preset`)

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

### sales_funnel

- `id` BINARY(16) PRIMARY KEY
- `experiment_id` BIGINT NOT NULL (FK `fk_funnel_experiment`)
- `name` VARCHAR(100)
- `objective` VARCHAR(255)
- `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP

### funnel_step

- `id` BINARY(16) PRIMARY KEY
- `funnel_id` BINARY(16) NOT NULL (FK `fk_step_funnel`)
- `order_idx` INT
- `stimulus_type` ENUM('DM','EMAIL','IG_POST_BOOST','FB_AD','STORY','WHATSAPP','CALL','SMS','WEBINAR','PUSH')
- `channel` VARCHAR(50)
- `template_id` VARCHAR(50)
- `expected_action` ENUM('OPEN','CLICK','REPLY','PURCHASE')
- `score_inc` INT
- `revenue_target` DECIMAL(10,2)
- `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
- `is_active` TINYINT(1) DEFAULT 1

### lead_response

- `id` BIGINT PRIMARY KEY AUTO_INCREMENT
- `lead_id` BINARY(16) NOT NULL (FK `fk_response_lead`)
- `funnel_step_id` BINARY(16) NOT NULL (FK `fk_response_step`)
- `action` ENUM('OPEN','CLICK','REPLY','PURCHASE')
- `value` JSON
- `revenue` DECIMAL(10,2)
- `occurred_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP

### step_metric_snapshot

- `id` BIGINT PRIMARY KEY AUTO_INCREMENT
- `funnel_step_id` BINARY(16) NOT NULL (FK `fk_snapshot_step`)
- `impressions` BIGINT
- `responses` BIGINT
- `conversions` BIGINT
- `revenue` DECIMAL(12,2)
- `gross_profit` DECIMAL(12,2)
- `cvr` DECIMAL(6,4)
- `captured_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP

### outbox

- `id` BIGINT PRIMARY KEY AUTO_INCREMENT
- `aggregate_id` BINARY(16)
- `event_type` VARCHAR(50)
- `payload` TEXT
- `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
- `processed_at` DATETIME

### metric_preset

- `id` VARCHAR(50) PRIMARY KEY
- `name` VARCHAR(100)
- `sample_size` INT
- `stop_loss_factor` DECIMAL(5,2)
- `default_mde_pp` DECIMAL(5,2)

Additional change logs add foreign keys to existing `creative_variants` and `ad_set` tables linking them to `experiment`.

## Diagram

```mermaid
erDiagram
    HYPOTHESIS {
        BINARY id PK
        BIGINT premise_angle_id
        BIGINT market_niche_id FK
        VARCHAR offer_type
        DECIMAL price
        DECIMAL kpi_target_cpl
        VARCHAR status
        TIMESTAMP created_at
        TIMESTAMP updated_at
    }
    EXPERIMENT {
        BINARY hypothesis_id FK
        DECIMAL kpi_target_cpl
        DECIMAL stop_loss_cpl
        INT sample_size
        DECIMAL baseline_cvr
        DECIMAL target_cvr
        DECIMAL mde_percent
        VARCHAR metric_preset_id FK
    }
    LEAD {
        BINARY id PK
        BIGINT leadgen_id
        BIGINT instagram_user_id
        BIGINT ad_id
        BIGINT campaign_id
        BIGINT experiment_id
        DATETIME captured_at
        ENUM nurture_stage
        DECIMAL cpl
        INT lead_score
    }
    SALES_FUNNEL {
        BINARY id PK
        BIGINT experiment_id FK
        VARCHAR name
        VARCHAR objective
        TIMESTAMP created_at
    }
    FUNNEL_STEP {
        BINARY id PK
        BINARY funnel_id FK
        INT order_idx
        ENUM stimulus_type
        VARCHAR channel
        VARCHAR template_id
        ENUM expected_action
        INT score_inc
        DECIMAL revenue_target
        TIMESTAMP created_at
        TINYINT is_active
    }
    LEAD_RESPONSE {
        BIGINT id PK
        BINARY lead_id FK
        BINARY funnel_step_id FK
        ENUM action
        JSON value
        DECIMAL revenue
        TIMESTAMP occurred_at
    }
    STEP_METRIC_SNAPSHOT {
        BIGINT id PK
        BINARY funnel_step_id FK
        BIGINT impressions
        BIGINT responses
        BIGINT conversions
        DECIMAL revenue
        DECIMAL gross_profit
        DECIMAL cvr
        TIMESTAMP captured_at
    }
    OUTBOX {
        BIGINT id PK
        BINARY aggregate_id
        VARCHAR event_type
        TEXT payload
        DATETIME created_at
        DATETIME processed_at
    }
    METRIC_PRESET {
        VARCHAR id PK
        VARCHAR name
        INT sample_size
        DECIMAL stop_loss_factor
        DECIMAL default_mde_pp
    }
    MARKET_NICHE {
        BIGINT id PK
    }
    MARKET_NICHE ||--o{ HYPOTHESIS : categorizes
    HYPOTHESIS ||--o{ EXPERIMENT : referenced
    EXPERIMENT ||--o{ LEAD : captures
    EXPERIMENT ||--o{ SALES_FUNNEL : drives
    SALES_FUNNEL ||--o{ FUNNEL_STEP : includes
    FUNNEL_STEP ||--o{ LEAD_RESPONSE : receives
    FUNNEL_STEP ||--o{ STEP_METRIC_SNAPSHOT : snapshots
    LEAD ||--o{ LEAD_RESPONSE : acts
    METRIC_PRESET ||--o{ EXPERIMENT : configures
```
