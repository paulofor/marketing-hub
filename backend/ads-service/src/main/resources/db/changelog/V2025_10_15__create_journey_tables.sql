--liquibase formatted sql

--changeset marketinghub:2025-10-15-create-journey-template
CREATE TABLE journey_template (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    description LONGTEXT,
    objective VARCHAR(255),
    preferred_channel VARCHAR(100),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;

--changeset marketinghub:2025-10-15-create-journey-template-phase
CREATE TABLE journey_template_phase (
    template_id BIGINT NOT NULL,
    phase_order INT NOT NULL,
    phase VARCHAR(32) NOT NULL,
    PRIMARY KEY (template_id, phase_order),
    CONSTRAINT fk_journey_template_phase_template FOREIGN KEY (template_id) REFERENCES journey_template(id) ON DELETE CASCADE
) ENGINE=InnoDB;

--changeset marketinghub:2025-10-15-create-journey-template-tag
CREATE TABLE journey_template_tag (
    template_id BIGINT NOT NULL,
    tag VARCHAR(255) NOT NULL,
    PRIMARY KEY (template_id, tag),
    CONSTRAINT fk_journey_template_tag_template FOREIGN KEY (template_id) REFERENCES journey_template(id) ON DELETE CASCADE
) ENGINE=InnoDB;

--changeset marketinghub:2025-10-15-create-journey-template-metadata
CREATE TABLE journey_template_metadata (
    template_id BIGINT NOT NULL,
    meta_key VARCHAR(255) NOT NULL,
    meta_value LONGTEXT,
    PRIMARY KEY (template_id, meta_key),
    CONSTRAINT fk_journey_template_metadata_template FOREIGN KEY (template_id) REFERENCES journey_template(id) ON DELETE CASCADE
) ENGINE=InnoDB;

--changeset marketinghub:2025-10-15-create-journey-step
CREATE TABLE journey_step (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    template_id BIGINT NOT NULL,
    position INT NOT NULL,
    name VARCHAR(255),
    description LONGTEXT,
    phase VARCHAR(32) NOT NULL,
    stimulus_type VARCHAR(32) NOT NULL,
    creative_id BIGINT,
    angle_id BIGINT,
    visual_proof_id BIGINT,
    emotional_trigger_id BIGINT,
    entry_condition VARCHAR(255),
    exit_condition VARCHAR(255),
    delay_minutes INT,
    CONSTRAINT fk_journey_step_template FOREIGN KEY (template_id) REFERENCES journey_template(id) ON DELETE CASCADE,
    CONSTRAINT fk_journey_step_creative FOREIGN KEY (creative_id) REFERENCES creative(id),
    CONSTRAINT fk_journey_step_angle FOREIGN KEY (angle_id) REFERENCES angle(id),
    CONSTRAINT fk_journey_step_visual_proof FOREIGN KEY (visual_proof_id) REFERENCES visual_proof(id),
    CONSTRAINT fk_journey_step_emotional_trigger FOREIGN KEY (emotional_trigger_id) REFERENCES emotional_trigger(id)
) ENGINE=InnoDB;

--changeset marketinghub:2025-10-15-create-journey-step-metadata
CREATE TABLE journey_step_metadata (
    step_id BIGINT NOT NULL,
    meta_key VARCHAR(255) NOT NULL,
    meta_value LONGTEXT,
    PRIMARY KEY (step_id, meta_key),
    CONSTRAINT fk_journey_step_metadata_step FOREIGN KEY (step_id) REFERENCES journey_step(id) ON DELETE CASCADE
) ENGINE=InnoDB;

--changeset marketinghub:2025-10-15-create-journey
CREATE TABLE journey (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    template_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    description LONGTEXT,
    status VARCHAR(32) NOT NULL,
    niche_id BIGINT,
    experiment_id BIGINT,
    segment_reference VARCHAR(255),
    segment_filter LONGTEXT,
    start_at DATETIME,
    end_at DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_journey_template FOREIGN KEY (template_id) REFERENCES journey_template(id),
    CONSTRAINT fk_journey_niche FOREIGN KEY (niche_id) REFERENCES market_niche(id),
    CONSTRAINT fk_journey_experiment FOREIGN KEY (experiment_id) REFERENCES experiment(id)
) ENGINE=InnoDB;

--changeset marketinghub:2025-10-15-create-journey-metadata
CREATE TABLE journey_metadata (
    journey_id BIGINT NOT NULL,
    meta_key VARCHAR(255) NOT NULL,
    meta_value LONGTEXT,
    PRIMARY KEY (journey_id, meta_key),
    CONSTRAINT fk_journey_metadata_journey FOREIGN KEY (journey_id) REFERENCES journey(id) ON DELETE CASCADE
) ENGINE=InnoDB;

--changeset marketinghub:2025-10-15-create-journey-assignment
CREATE TABLE journey_assignment (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    journey_id BIGINT NOT NULL,
    type VARCHAR(20) NOT NULL,
    lead_id BINARY(16),
    segment_identifier VARCHAR(255),
    status VARCHAR(20) NOT NULL,
    current_step_id BIGINT,
    next_step_id BIGINT,
    last_event_at DATETIME,
    context_payload LONGTEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_journey_assignment_journey FOREIGN KEY (journey_id) REFERENCES journey(id) ON DELETE CASCADE,
    CONSTRAINT fk_journey_assignment_lead FOREIGN KEY (lead_id) REFERENCES `lead`(id),
    CONSTRAINT fk_journey_assignment_current_step FOREIGN KEY (current_step_id) REFERENCES journey_step(id),
    CONSTRAINT fk_journey_assignment_next_step FOREIGN KEY (next_step_id) REFERENCES journey_step(id)
) ENGINE=InnoDB;

--changeset marketinghub:2025-10-15-create-journey-event-log
CREATE TABLE journey_event_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    actor_id BINARY(16),
    event_type VARCHAR(100) NOT NULL,
    journey_id BIGINT,
    journey_step_id BIGINT,
    source VARCHAR(100),
    campaign_id VARCHAR(100),
    metadata LONGTEXT,
    value DECIMAL(12,2),
    occurred_at DATETIME NOT NULL,
    received_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_event_log_journey FOREIGN KEY (journey_id) REFERENCES journey(id),
    CONSTRAINT fk_event_log_step FOREIGN KEY (journey_step_id) REFERENCES journey_step(id)
) ENGINE=InnoDB;

--changeset marketinghub:2025-10-15-create-journey-indexes
CREATE INDEX idx_journey_step_template ON journey_step(template_id);
CREATE INDEX idx_journey_template_name ON journey_template(name);
CREATE INDEX idx_journey_status ON journey(status);
CREATE INDEX idx_journey_assignment_journey ON journey_assignment(journey_id);
CREATE INDEX idx_journey_event_log_actor ON journey_event_log(actor_id);
