-- Cria tabela para descrições detalhadas de nicho e campos de geração
ALTER TABLE market_niche
    ADD COLUMN IF NOT EXISTS detailed_descriptions_to_generate INT;

ALTER TABLE market_niche
    ADD COLUMN IF NOT EXISTS detailed_description_model VARCHAR(191);

CREATE TABLE IF NOT EXISTS niche_detailed_description (
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
