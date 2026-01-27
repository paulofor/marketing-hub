--liquibase formatted sql
--changeset repo:2034-01-20-drop-audience-targeting-seed dbms:mysql
SET @sql := (
    SELECT IF(COUNT(*) > 0,
              'DROP TABLE audience_targeting_seed',
              'DO 0')
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'audience_targeting_seed'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @sql := NULL;

--changeset repo:2034-01-20-drop-audience-table dbms:mysql
SET @sql := (
    SELECT IF(COUNT(*) > 0,
              'DROP TABLE audience',
              'DO 0')
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'audience'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @sql := NULL;

--changeset repo:2034-01-20-drop-market-niche-audiences-to-generate dbms:mysql
SET @sql := (
    SELECT IF(COUNT(*) > 0,
              'ALTER TABLE market_niche DROP COLUMN audiences_to_generate',
              'DO 0')
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'market_niche'
      AND column_name = 'audiences_to_generate'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @sql := NULL;

--changeset repo:2034-01-20-drop-market-niche-audience-model dbms:mysql
SET @sql := (
    SELECT IF(COUNT(*) > 0,
              'ALTER TABLE market_niche DROP COLUMN audience_model',
              'DO 0')
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'market_niche'
      AND column_name = 'audience_model'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @sql := NULL;

--changeset repo:2034-01-20-add-market-niche-interests-to-generate dbms:mysql
SET @sql := (
    SELECT IF(COUNT(*) = 0,
              'ALTER TABLE market_niche ADD COLUMN interests_to_generate INT',
              'DO 0')
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'market_niche'
      AND column_name = 'interests_to_generate'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @sql := NULL;

--changeset repo:2034-01-20-add-market-niche-job-titles-to-generate dbms:mysql
SET @sql := (
    SELECT IF(COUNT(*) = 0,
              'ALTER TABLE market_niche ADD COLUMN job_titles_to_generate INT',
              'DO 0')
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'market_niche'
      AND column_name = 'job_titles_to_generate'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @sql := NULL;

--changeset repo:2034-01-20-add-market-niche-behaviors-to-generate dbms:mysql
SET @sql := (
    SELECT IF(COUNT(*) = 0,
              'ALTER TABLE market_niche ADD COLUMN behaviors_to_generate INT',
              'DO 0')
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'market_niche'
      AND column_name = 'behaviors_to_generate'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @sql := NULL;

--changeset repo:2034-01-20-add-market-niche-interest-model dbms:mysql
SET @sql := (
    SELECT IF(COUNT(*) = 0,
              'ALTER TABLE market_niche ADD COLUMN interest_model VARCHAR(191)',
              'DO 0')
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'market_niche'
      AND column_name = 'interest_model'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @sql := NULL;

--changeset repo:2034-01-20-add-market-niche-job-title-model dbms:mysql
SET @sql := (
    SELECT IF(COUNT(*) = 0,
              'ALTER TABLE market_niche ADD COLUMN job_title_model VARCHAR(191)',
              'DO 0')
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'market_niche'
      AND column_name = 'job_title_model'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @sql := NULL;

--changeset repo:2034-01-20-add-market-niche-behavior-model dbms:mysql
SET @sql := (
    SELECT IF(COUNT(*) = 0,
              'ALTER TABLE market_niche ADD COLUMN behavior_model VARCHAR(191)',
              'DO 0')
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'market_niche'
      AND column_name = 'behavior_model'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @sql := NULL;

--changeset repo:2034-01-20-create-targeting-element dbms:mysql
SET @sql := (
    SELECT IF(COUNT(*) = 0,
              'CREATE TABLE targeting_element (id BIGINT AUTO_INCREMENT PRIMARY KEY, market_niche_id BIGINT NOT NULL, hypothesis_id BINARY(16) NULL, type VARCHAR(32) NOT NULL, term VARCHAR(255) NOT NULL, description LONGTEXT NULL, prompt LONGTEXT NULL, model VARCHAR(191) NULL, source VARCHAR(32) NULL, status VARCHAR(32) NOT NULL, notes LONGTEXT NULL, last_reviewed_by VARCHAR(191) NULL, meta_id VARCHAR(100) NULL, meta_key VARCHAR(191) NULL, confidence DECIMAL(10,4) NULL, created_at DATETIME(6) NULL, updated_at DATETIME(6) NULL, CONSTRAINT fk_targeting_element_niche FOREIGN KEY (market_niche_id) REFERENCES market_niche(id), CONSTRAINT fk_targeting_element_hypothesis FOREIGN KEY (hypothesis_id) REFERENCES hypothesis(id)) ENGINE=InnoDB',
              'DO 0')
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'targeting_element'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @sql := NULL;

--changeset repo:2034-01-20-create-targeting-element-index-niche-type-status dbms:mysql
SET @sql := (
    SELECT IF(COUNT(*) = 0,
              'CREATE INDEX idx_targeting_element_niche_type_status ON targeting_element (market_niche_id, type, status)',
              'DO 0')
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'targeting_element'
      AND index_name = 'idx_targeting_element_niche_type_status'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @sql := NULL;

--changeset repo:2034-01-20-create-targeting-element-index-hypothesis dbms:mysql
SET @sql := (
    SELECT IF(COUNT(*) = 0,
              'CREATE INDEX idx_targeting_element_hypothesis ON targeting_element (hypothesis_id)',
              'DO 0')
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'targeting_element'
      AND index_name = 'idx_targeting_element_hypothesis'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @sql := NULL;
