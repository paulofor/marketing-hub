-- liquibase formatted sql
-- changeset marketinghub:2025-09-29-link-hypothesis-prompt-attr-desc
CREATE TABLE IF NOT EXISTS hypothesis_prompt_attribute_description (
    hypothesis_id BINARY(16) NOT NULL,
    prompt_attribute_description_id BIGINT NOT NULL,
    PRIMARY KEY (hypothesis_id, prompt_attribute_description_id),
    CONSTRAINT fk_hpah_hypothesis FOREIGN KEY (hypothesis_id) REFERENCES hypothesis(id),
    CONSTRAINT fk_hpah_description FOREIGN KEY (prompt_attribute_description_id) REFERENCES prompt_attribute_description(id)
);
