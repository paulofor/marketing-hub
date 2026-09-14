CREATE TABLE image_generation_model (
  id BIGINT NOT NULL AUTO_INCREMENT,
  code VARCHAR(64) NOT NULL,
  display_name VARCHAR(128) NOT NULL,
  provider VARCHAR(32) NOT NULL,
  api_model VARCHAR(128) NOT NULL,
  description VARCHAR(512) NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_image_generation_model_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE image_generation_quality (
  id BIGINT NOT NULL AUTO_INCREMENT,
  model_id BIGINT NOT NULL,
  code VARCHAR(32) NOT NULL,
  display_name VARCHAR(64) NOT NULL,
  api_quality VARCHAR(32) NULL,
  is_default TINYINT(1) NOT NULL DEFAULT 0,
  position INT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_image_generation_quality_model_code (model_id, code),
  CONSTRAINT fk_image_generation_quality_model
    FOREIGN KEY (model_id) REFERENCES image_generation_model (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE image_generation_price (
  id BIGINT NOT NULL AUTO_INCREMENT,
  quality_id BIGINT NOT NULL,
  orientation VARCHAR(16) NOT NULL,
  width INT NOT NULL,
  height INT NOT NULL,
  size_label VARCHAR(32) NOT NULL,
  unit_price_usd DECIMAL(10,5) NOT NULL,
  preferred TINYINT(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_image_generation_price_quality_orientation (quality_id, orientation),
  CONSTRAINT fk_image_generation_price_quality
    FOREIGN KEY (quality_id) REFERENCES image_generation_quality (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO image_generation_model (code, display_name, provider, api_model, description)
VALUES ('gpt-image-2', 'GPT Image 2', 'OPENAI', 'gpt-image-2', 'Registro histórico');
