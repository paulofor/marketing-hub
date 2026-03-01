--liquibase formatted sql

--changeset repo:2035-03-10-lead-portal-simple-form-style-table dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'lead_portal_simple_form_style';
CREATE TABLE lead_portal_simple_form_style (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    slug VARCHAR(120) NOT NULL,
    description VARCHAR(500),
    text_model VARCHAR(128),
    text_prompt LONGTEXT,
    text_parameters LONGTEXT,
    image_model VARCHAR(128),
    image_prompt LONGTEXT,
    image_negative_prompt LONGTEXT,
    image_parameters LONGTEXT,
    image_batch_size INT,
    image_aspect_ratio VARCHAR(32),
    preview_image_url VARCHAR(512),
    definition LONGTEXT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_lead_portal_simple_form_style_slug UNIQUE (slug)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--changeset repo:2035-03-10-lead-portal-simple-form-style-fk dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'lead_portal_flow' AND column_name = 'simple_form_style_id';
ALTER TABLE lead_portal_flow
    ADD COLUMN simple_form_style_id BIGINT NULL,
    ADD CONSTRAINT fk_lead_portal_flow_simple_form_style
        FOREIGN KEY (simple_form_style_id) REFERENCES lead_portal_simple_form_style(id);
