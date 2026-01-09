--liquibase formatted sql

--changeset marketinghub:2031-01-10-create-interaction-journey
CREATE TABLE interaction_journey (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    description LONGTEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;

--changeset marketinghub:2031-01-10-create-interaction-journey-step
CREATE TABLE interaction_journey_step (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    journey_id BIGINT NOT NULL,
    order_index INT NOT NULL,
    title VARCHAR(255) NOT NULL,
    description LONGTEXT,
    CONSTRAINT fk_interaction_step_journey FOREIGN KEY (journey_id) REFERENCES interaction_journey(id) ON DELETE CASCADE
) ENGINE=InnoDB;

--changeset marketinghub:2031-01-10-create-interaction-journey-element
CREATE TABLE interaction_journey_element (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    step_id BIGINT NOT NULL,
    parent_id BIGINT NULL,
    order_index INT NOT NULL,
    label VARCHAR(255) NOT NULL,
    type VARCHAR(100),
    notes LONGTEXT,
    CONSTRAINT fk_interaction_element_step FOREIGN KEY (step_id) REFERENCES interaction_journey_step(id) ON DELETE CASCADE,
    CONSTRAINT fk_interaction_element_parent FOREIGN KEY (parent_id) REFERENCES interaction_journey_element(id) ON DELETE CASCADE
) ENGINE=InnoDB;

--changeset marketinghub:2031-01-10-create-interaction-journey-indexes
CREATE INDEX idx_interaction_step_journey ON interaction_journey_step(journey_id);
CREATE INDEX idx_interaction_element_step ON interaction_journey_element(step_id);
CREATE INDEX idx_interaction_element_parent ON interaction_journey_element(parent_id);
