CREATE TABLE product_type_definition (
    id BIGINT NOT NULL AUTO_INCREMENT,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(191) NOT NULL,
    internal_name VARCHAR(191) NULL,
    description VARCHAR(1000) NULL,
    status VARCHAR(32) NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_product_type_consultants_code (code),
    UNIQUE KEY uk_product_type_consultants_name (name),
    UNIQUE KEY uk_product_type_consultants_internal_name (internal_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE product_type_alias (
    product_type_id BIGINT NOT NULL,
    alias VARCHAR(191) NOT NULL,
    PRIMARY KEY (product_type_id, alias),
    UNIQUE KEY uk_product_type_consultants_alias (alias),
    CONSTRAINT fk_product_type_consultants_alias
      FOREIGN KEY (product_type_id) REFERENCES product_type_definition(id)
      ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE product (
    id BIGINT NOT NULL,
    product_type VARCHAR(191) NULL,
    product_type_id BIGINT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_product_type_consultants_product
      FOREIGN KEY (product_type_id) REFERENCES product_type_definition(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO product_type_definition
  (id, code, name, internal_name, description, status, created_at, updated_at)
VALUES
  (4, 'AI_SANDBOX_CONVERSATIONAL_PRODUCT',
   'Produto IA de atendimento personalizado por sandbox', 'Fluorita',
   'Atendimento conversacional com memória individual, contexto e execução isolada.',
   'ACTIVE', '2026-08-23 10:00:00', '2026-08-23 10:00:00');

INSERT INTO product_type_alias (product_type_id, alias)
VALUES (4, 'PDE - Consultor Especialista por WhatsApp');

INSERT INTO product (id, product_type, product_type_id)
VALUES (8, 'Produto IA de atendimento personalizado por sandbox', 4);
