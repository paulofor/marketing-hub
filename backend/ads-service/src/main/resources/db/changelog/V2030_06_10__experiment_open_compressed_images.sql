--liquibase formatted sql
--changeset repo:2030-06-10-experiment-open-compressed-images dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'experiment' AND column_name IN ('open_images_per_package','compressed_images_per_package');
ALTER TABLE experiment
    ADD COLUMN open_images_per_package INT NULL AFTER images_per_package,
    ADD COLUMN compressed_images_per_package INT NULL AFTER open_images_per_package;
