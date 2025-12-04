--liquibase formatted sql
--changeset repo:2030-02-10-image-generation-models dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'image_generation_model';
CREATE TABLE image_generation_model (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(64) NOT NULL,
    display_name VARCHAR(128) NOT NULL,
    provider VARCHAR(32) NOT NULL,
    api_model VARCHAR(128) NOT NULL,
    description LONGTEXT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_image_generation_model_code UNIQUE (code)
) ENGINE=InnoDB;

CREATE TABLE image_generation_quality (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    model_id BIGINT NOT NULL,
    code VARCHAR(32) NOT NULL,
    display_name VARCHAR(64) NOT NULL,
    api_quality VARCHAR(32),
    is_default TINYINT(1) NOT NULL DEFAULT 0,
    position INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_image_generation_quality_model FOREIGN KEY (model_id) REFERENCES image_generation_model(id),
    CONSTRAINT uq_image_generation_quality_code UNIQUE (model_id, code)
) ENGINE=InnoDB;

CREATE TABLE image_generation_price (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    quality_id BIGINT NOT NULL,
    orientation VARCHAR(16) NOT NULL,
    width INT NOT NULL,
    height INT NOT NULL,
    size_label VARCHAR(32) NOT NULL,
    unit_price_usd DECIMAL(10,5) NOT NULL,
    preferred TINYINT(1) NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_image_generation_price_quality FOREIGN KEY (quality_id) REFERENCES image_generation_quality(id),
    CONSTRAINT uq_image_generation_price UNIQUE (quality_id, orientation, width, height)
) ENGINE=InnoDB;

-- Populate catalog
INSERT INTO image_generation_model (code, display_name, provider, api_model, description)
VALUES
    ('gpt-image-1', 'GPT Image 1', 'OPENAI', 'gpt-image-1', 'Modelo principal de geração de imagens da OpenAI com suporte a qualidades low/medium/high.'),
    ('gpt-image-1-mini', 'GPT Image 1 Mini', 'OPENAI', 'gpt-image-1-mini', 'Versão otimizada do GPT Image 1 com custo reduzido.'),
    ('dall-e-3', 'DALL·E 3', 'OPENAI', 'dall-e-3', 'Modelo DALL·E 3 para geração de imagens com modos Standard e HD.'),
    ('dall-e-2', 'DALL·E 2', 'OPENAI', 'dall-e-2', 'Modelo DALL·E 2 focado em variações quadradas.');

SET @model_gpt_image_1 = (SELECT id FROM image_generation_model WHERE code = 'gpt-image-1');
SET @model_gpt_image_1_mini = (SELECT id FROM image_generation_model WHERE code = 'gpt-image-1-mini');
SET @model_dalle_3 = (SELECT id FROM image_generation_model WHERE code = 'dall-e-3');
SET @model_dalle_2 = (SELECT id FROM image_generation_model WHERE code = 'dall-e-2');

INSERT INTO image_generation_quality (model_id, code, display_name, api_quality, is_default, position)
VALUES
    (@model_gpt_image_1, 'low', 'Low', 'standard', 0, 10),
    (@model_gpt_image_1, 'medium', 'Medium', 'standard', 1, 20),
    (@model_gpt_image_1, 'high', 'High', 'hd', 0, 30),
    (@model_gpt_image_1_mini, 'low', 'Low', 'standard', 0, 10),
    (@model_gpt_image_1_mini, 'medium', 'Medium', 'standard', 1, 20),
    (@model_gpt_image_1_mini, 'high', 'High', 'standard', 0, 30),
    (@model_dalle_3, 'standard', 'Standard', 'standard', 1, 10),
    (@model_dalle_3, 'hd', 'HD', 'hd', 0, 20),
    (@model_dalle_2, 'standard', 'Standard', 'standard', 1, 10)
ON DUPLICATE KEY UPDATE
    display_name = VALUES(display_name),
    api_quality = VALUES(api_quality),
    is_default = VALUES(is_default),
    position = VALUES(position);

SET @quality_gpt_image1_low = (SELECT q.id FROM image_generation_quality q WHERE q.model_id = @model_gpt_image_1 AND q.code = 'low');
SET @quality_gpt_image1_medium = (SELECT q.id FROM image_generation_quality q WHERE q.model_id = @model_gpt_image_1 AND q.code = 'medium');
SET @quality_gpt_image1_high = (SELECT q.id FROM image_generation_quality q WHERE q.model_id = @model_gpt_image_1 AND q.code = 'high');
SET @quality_gpt_image1_mini_low = (SELECT q.id FROM image_generation_quality q WHERE q.model_id = @model_gpt_image_1_mini AND q.code = 'low');
SET @quality_gpt_image1_mini_medium = (SELECT q.id FROM image_generation_quality q WHERE q.model_id = @model_gpt_image_1_mini AND q.code = 'medium');
SET @quality_gpt_image1_mini_high = (SELECT q.id FROM image_generation_quality q WHERE q.model_id = @model_gpt_image_1_mini AND q.code = 'high');
SET @quality_dalle3_standard = (SELECT q.id FROM image_generation_quality q WHERE q.model_id = @model_dalle_3 AND q.code = 'standard');
SET @quality_dalle3_hd = (SELECT q.id FROM image_generation_quality q WHERE q.model_id = @model_dalle_3 AND q.code = 'hd');
SET @quality_dalle2_standard = (SELECT q.id FROM image_generation_quality q WHERE q.model_id = @model_dalle_2 AND q.code = 'standard');

INSERT INTO image_generation_price (quality_id, orientation, width, height, size_label, unit_price_usd, preferred)
VALUES
    (@quality_gpt_image1_low, 'SQUARE', 1024, 1024, '1024x1024', 0.01100, 1),
    (@quality_gpt_image1_low, 'PORTRAIT', 1024, 1536, '1024x1536', 0.01600, 0),
    (@quality_gpt_image1_low, 'LANDSCAPE', 1536, 1024, '1536x1024', 0.01600, 0),
    (@quality_gpt_image1_medium, 'SQUARE', 1024, 1024, '1024x1024', 0.04200, 1),
    (@quality_gpt_image1_medium, 'PORTRAIT', 1024, 1536, '1024x1536', 0.06300, 0),
    (@quality_gpt_image1_medium, 'LANDSCAPE', 1536, 1024, '1536x1024', 0.06300, 0),
    (@quality_gpt_image1_high, 'SQUARE', 1024, 1024, '1024x1024', 0.16700, 1),
    (@quality_gpt_image1_high, 'PORTRAIT', 1024, 1536, '1024x1536', 0.25000, 0),
    (@quality_gpt_image1_high, 'LANDSCAPE', 1536, 1024, '1536x1024', 0.25000, 0),
    (@quality_gpt_image1_mini_low, 'SQUARE', 1024, 1024, '1024x1024', 0.00500, 1),
    (@quality_gpt_image1_mini_low, 'PORTRAIT', 1024, 1536, '1024x1536', 0.00600, 0),
    (@quality_gpt_image1_mini_low, 'LANDSCAPE', 1536, 1024, '1536x1024', 0.00600, 0),
    (@quality_gpt_image1_mini_medium, 'SQUARE', 1024, 1024, '1024x1024', 0.01100, 1),
    (@quality_gpt_image1_mini_medium, 'PORTRAIT', 1024, 1536, '1024x1536', 0.01500, 0),
    (@quality_gpt_image1_mini_medium, 'LANDSCAPE', 1536, 1024, '1536x1024', 0.01500, 0),
    (@quality_gpt_image1_mini_high, 'SQUARE', 1024, 1024, '1024x1024', 0.03600, 1),
    (@quality_gpt_image1_mini_high, 'PORTRAIT', 1024, 1536, '1024x1536', 0.05200, 0),
    (@quality_gpt_image1_mini_high, 'LANDSCAPE', 1536, 1024, '1536x1024', 0.05200, 0),
    (@quality_dalle3_standard, 'SQUARE', 1024, 1024, '1024x1024', 0.04000, 1),
    (@quality_dalle3_standard, 'PORTRAIT', 1024, 1792, '1024x1792', 0.08000, 0),
    (@quality_dalle3_standard, 'LANDSCAPE', 1792, 1024, '1792x1024', 0.08000, 0),
    (@quality_dalle3_hd, 'SQUARE', 1024, 1024, '1024x1024', 0.08000, 1),
    (@quality_dalle3_hd, 'PORTRAIT', 1024, 1792, '1024x1792', 0.12000, 0),
    (@quality_dalle3_hd, 'LANDSCAPE', 1792, 1024, '1792x1024', 0.12000, 0),
    (@quality_dalle2_standard, 'SQUARE', 256, 256, '256x256', 0.01600, 0),
    (@quality_dalle2_standard, 'SQUARE', 512, 512, '512x512', 0.01800, 0),
    (@quality_dalle2_standard, 'SQUARE', 1024, 1024, '1024x1024', 0.02000, 1)
ON DUPLICATE KEY UPDATE
    unit_price_usd = VALUES(unit_price_usd),
    preferred = VALUES(preferred),
    size_label = VALUES(size_label);
