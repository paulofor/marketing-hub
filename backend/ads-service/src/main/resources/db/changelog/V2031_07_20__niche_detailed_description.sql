--liquibase formatted sql
--changeset repo:2031-07-20-add-niche-detailed-description-columns dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'market_niche' AND column_name = 'detailed_descriptions_to_generate';
-- Cria tabela para descrições detalhadas de nicho e campos de geração
ALTER TABLE market_niche
    ADD COLUMN detailed_descriptions_to_generate INT;

--changeset repo:2031-07-20-add-niche-detailed-description-model-column dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'market_niche' AND column_name = 'detailed_description_model';
ALTER TABLE market_niche
    ADD COLUMN detailed_description_model VARCHAR(191);

--changeset repo:2031-07-20-create-niche-detailed-description-table dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'niche_detailed_description';
CREATE TABLE niche_detailed_description (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    market_niche_id BIGINT NOT NULL,
    title VARCHAR(255),
    description LONGTEXT,
    pains LONGTEXT,
    desires LONGTEXT,
    needs LONGTEXT,
    prompt LONGTEXT,
    model VARCHAR(191),
    cost_usd DECIMAL(10,4),
    input_tokens INT,
    output_tokens INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_niche_detailed_description_niche FOREIGN KEY (market_niche_id) REFERENCES market_niche(id)
);
