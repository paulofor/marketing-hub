--liquibase formatted sql
--changeset repo:2037-01-05-sales-video-foundation dbms:mysql splitStatements:true

CREATE TABLE IF NOT EXISTS sales_video_profile (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    product_id BIGINT NOT NULL,
    landing_page_id BIGINT NULL,
    video_kind VARCHAR(32) NOT NULL,
    title VARCHAR(255) NOT NULL,
    persona_name VARCHAR(255) NULL,
    persona_style VARCHAR(255) NULL,
    voice_style VARCHAR(255) NULL,
    language VARCHAR(64) NULL,
    target_duration_seconds INT NULL,
    status VARCHAR(64) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_sales_video_profile_product FOREIGN KEY (product_id) REFERENCES product (id),
    CONSTRAINT fk_sales_video_profile_landing_page FOREIGN KEY (landing_page_id) REFERENCES landing_page (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_sales_video_profile_product ON sales_video_profile (product_id);
CREATE INDEX idx_sales_video_profile_status ON sales_video_profile (status);

CREATE TABLE IF NOT EXISTS sales_video_script (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    profile_id BIGINT NOT NULL,
    version INT NOT NULL,
    script_text LONGTEXT NULL,
    hook_text LONGTEXT NULL,
    cta_text LONGTEXT NULL,
    caption_text LONGTEXT NULL,
    source VARCHAR(32) NOT NULL,
    model VARCHAR(128) NULL,
    prompt LONGTEXT NULL,
    status VARCHAR(32) NOT NULL,
    approved_by VARCHAR(255) NULL,
    approved_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_sales_video_script_profile FOREIGN KEY (profile_id) REFERENCES sales_video_profile (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE UNIQUE INDEX uq_sales_video_script_profile_version
    ON sales_video_script (profile_id, version);

CREATE TABLE IF NOT EXISTS sales_video_job (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    profile_id BIGINT NOT NULL,
    script_id BIGINT NULL,
    provider_family VARCHAR(32) NOT NULL,
    provider_name VARCHAR(128) NULL,
    provider_job_id VARCHAR(255) NULL,
    job_type VARCHAR(32) NOT NULL,
    status VARCHAR(64) NOT NULL,
    progress_percent INT NOT NULL DEFAULT 0,
    failure_code VARCHAR(128) NULL,
    failure_detail LONGTEXT NULL,
    requested_by VARCHAR(255) NULL,
    requested_at DATETIME(6) NOT NULL,
    started_at DATETIME(6) NULL,
    finished_at DATETIME(6) NULL,
    expires_at DATETIME(6) NULL,
    asset_id BIGINT NULL,
    poster_asset_id BIGINT NULL,
    vtt_asset_id BIGINT NULL,
    metadata_json LONGTEXT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_sales_video_job_profile FOREIGN KEY (profile_id) REFERENCES sales_video_profile (id),
    CONSTRAINT fk_sales_video_job_script FOREIGN KEY (script_id) REFERENCES sales_video_script (id),
    CONSTRAINT fk_sales_video_job_asset FOREIGN KEY (asset_id) REFERENCES asset (id),
    CONSTRAINT fk_sales_video_job_poster_asset FOREIGN KEY (poster_asset_id) REFERENCES asset (id),
    CONSTRAINT fk_sales_video_job_vtt_asset FOREIGN KEY (vtt_asset_id) REFERENCES asset (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_sales_video_job_status ON sales_video_job (status);
CREATE INDEX idx_sales_video_job_provider_family_status ON sales_video_job (provider_family, status);
CREATE INDEX idx_sales_video_job_requested_at ON sales_video_job (requested_at);

CREATE TABLE IF NOT EXISTS sales_video_job_event (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    job_id BIGINT NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    old_status VARCHAR(64) NULL,
    new_status VARCHAR(64) NULL,
    message VARCHAR(512) NULL,
    details_json LONGTEXT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_sales_video_job_event_job FOREIGN KEY (job_id) REFERENCES sales_video_job (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_sales_video_job_event_job ON sales_video_job_event (job_id);

CREATE TABLE IF NOT EXISTS landing_video_slot (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    landing_page_id BIGINT NOT NULL,
    profile_id BIGINT NOT NULL,
    slot_name VARCHAR(64) NOT NULL,
    asset_id BIGINT NOT NULL,
    poster_asset_id BIGINT NULL,
    vtt_asset_id BIGINT NULL,
    autoplay TINYINT(1) NOT NULL DEFAULT 1,
    muted TINYINT(1) NOT NULL DEFAULT 1,
    loop_video TINYINT(1) NOT NULL DEFAULT 0,
    controls_enabled TINYINT(1) NOT NULL DEFAULT 1,
    lazy_load TINYINT(1) NOT NULL DEFAULT 1,
    published_at DATETIME(6) NULL,
    published_by VARCHAR(255) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_landing_video_slot_landing_page FOREIGN KEY (landing_page_id) REFERENCES landing_page (id),
    CONSTRAINT fk_landing_video_slot_profile FOREIGN KEY (profile_id) REFERENCES sales_video_profile (id),
    CONSTRAINT fk_landing_video_slot_asset FOREIGN KEY (asset_id) REFERENCES asset (id),
    CONSTRAINT fk_landing_video_slot_poster_asset FOREIGN KEY (poster_asset_id) REFERENCES asset (id),
    CONSTRAINT fk_landing_video_slot_vtt_asset FOREIGN KEY (vtt_asset_id) REFERENCES asset (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE UNIQUE INDEX uq_landing_video_slot_landing_slot
    ON landing_video_slot (landing_page_id, slot_name);
