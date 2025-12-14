--changeset repo:2030-04-20-experiment-images-per-package dbms:mysql
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'experiment' AND column_name = 'images_per_package';
ALTER TABLE experiment
    ADD COLUMN images_per_package INT NOT NULL DEFAULT 20 AFTER lead_portal_flows_to_generate;
