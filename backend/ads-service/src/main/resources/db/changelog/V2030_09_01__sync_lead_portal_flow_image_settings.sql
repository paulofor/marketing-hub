--liquibase formatted sql
--changeset repo:2030-09-01-sync-lead-portal-flow-image-settings dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'lead_portal_flow';
UPDATE lead_portal_flow flow
JOIN experiment exp ON exp.lead_portal_flow_id = flow.id
LEFT JOIN image_generation_model img_model ON img_model.id = exp.image_model_id
LEFT JOIN image_generation_quality img_quality ON img_quality.id = exp.image_model_quality_id
LEFT JOIN image_generation_model quality_model ON quality_model.id = img_quality.model_id
SET flow.image_prompt_batch_size = exp.images_per_package,
    flow.image_prompt_model = COALESCE(img_model.api_model, quality_model.api_model, flow.image_prompt_model);
