--changeset repo:2030-05-27-experiment-send-images-as-zip dbms:mysql
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'experiment' AND COLUMN_NAME = 'send_images_as_zip'
ALTER TABLE experiment
    ADD COLUMN send_images_as_zip TINYINT(1) NOT NULL DEFAULT 1;

--changeset repo:2030-05-27-experiment-send-images-as-zip-backfill dbms:mysql runOnChange:true
UPDATE experiment SET send_images_as_zip = 1 WHERE send_images_as_zip IS NULL;
