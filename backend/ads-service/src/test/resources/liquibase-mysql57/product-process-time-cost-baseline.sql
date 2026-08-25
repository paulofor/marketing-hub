CREATE TABLE product (
    id BIGINT NOT NULL PRIMARY KEY,
    internal_name VARCHAR(100) NOT NULL,
    commercial_status VARCHAR(100) NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE business_process_definition (
    id BIGINT NOT NULL PRIMARY KEY,
    process_code VARCHAR(100) NOT NULL,
    name VARCHAR(160) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE business_process_chain_definition (
    id BIGINT NOT NULL PRIMARY KEY,
    chain_code VARCHAR(100) NOT NULL,
    version_number INT NOT NULL,
    status VARCHAR(40) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE business_process_chain_item (
    id BIGINT NOT NULL PRIMARY KEY,
    chain_definition_id BIGINT NOT NULL,
    process_definition_id BIGINT NOT NULL,
    sequence_number INT NOT NULL,
    CONSTRAINT fk_process_period_fixture_chain
      FOREIGN KEY (chain_definition_id) REFERENCES business_process_chain_definition(id),
    CONSTRAINT fk_process_period_fixture_definition
      FOREIGN KEY (process_definition_id) REFERENCES business_process_definition(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

INSERT INTO business_process_definition (id, process_code, name)
VALUES
  (39, 'pde-construction-approval', 'Construção e aprovação do PDE'),
  (43, 'pde-communication-sales-journey', 'Comunicação e jornada de venda do PDE'),
  (45, 'pde-commercial-homologation-activation', 'Homologação e ativação comercial do PDE');

INSERT INTO business_process_chain_definition
  (id, chain_code, version_number, status)
VALUES (5, 'pde-value-creation-delivery', 5, 'PUBLISHED');

INSERT INTO business_process_chain_item
  (id, chain_definition_id, process_definition_id, sequence_number)
VALUES
  (1, 5, 39, 3),
  (2, 5, 43, 4),
  (3, 5, 45, 5);

INSERT INTO product
  (id, internal_name, commercial_status, created_at, updated_at)
VALUES
  (4, 'Vega', 'VALIDACAO_COMERCIAL', '2026-08-20 10:00:00', '2026-08-24 10:00:00'),
  (6, 'Sirius', 'CONSTRUCAO_E_APROVACAO', '2026-08-20 10:00:00', '2026-08-23 10:00:00'),
  (9, 'Rigel', 'COMUNICACAO_E_JORNADA', '2026-08-21 10:00:00', '2026-08-25 10:00:00');
