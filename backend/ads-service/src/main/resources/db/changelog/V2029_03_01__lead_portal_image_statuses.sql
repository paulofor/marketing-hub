--liquibase formatted sql
--changeset repo:2029-03-01-lead-portal-image-statuses dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'image_deliverable_package' AND column_name = 'status'

UPDATE image_deliverable_package
SET status = CASE status
    WHEN 'PENDING_PROCESSING' THEN 'RECEIVED'
    WHEN 'PROCESSING' THEN 'PROCESSED'
    WHEN 'COMPLETED' THEN 'GENERATION_NO_WATERMARK'
    ELSE status
END
WHERE status IN ('PENDING_PROCESSING', 'PROCESSING', 'COMPLETED');
