CREATE TABLE product (
    id BIGINT NOT NULL PRIMARY KEY,
    slug VARCHAR(191) NOT NULL UNIQUE,
    pde_experience_json LONGTEXT NULL,
    updated_at DATETIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE pde_production_slot (
    id BIGINT NOT NULL PRIMARY KEY,
    product_slug VARCHAR(191) NOT NULL,
    source_experiment_id BIGINT NULL,
    experience_version VARCHAR(191) NOT NULL,
    layout_key VARCHAR(191) NOT NULL,
    status VARCHAR(32) NOT NULL,
    draft_experience_json LONGTEXT NULL,
    published_experience_json LONGTEXT NULL,
    published_by VARCHAR(191) NULL,
    published_at DATETIME NULL,
    updated_at DATETIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET @rigel_product_experience = '{"slug":"kit-whatsapp-pronto","experienceVersion":"kit-whatsapp-pronto-pde-v2","layoutKey":"assisted-service-v2","funnelVersion":"pde-assisted-service-v2","serviceScope":{"includedItems":["10 a 20 respostas personalizadas","5 a 10 perguntas de qualificação","3 a 5 follow-ups manuais"]},"missions":[{"id":"entrada-guiada"},{"id":"conferencia-de-completude"},{"id":"diagnostico-humano"},{"id":"microvalor-12h"},{"id":"entrega-completa-48h","deliveryContract":{"sections":[{"id":"responses","title":"Respostas","minItems":10,"maxItems":20},{"id":"qualificationQuestions","title":"Perguntas","minItems":5,"maxItems":10},{"id":"followUps","title":"Follow-ups","minItems":3,"maxItems":5}]}},{"id":"primeira-aplicacao-e-revisao"}]}';

SET @rigel_slot_experience = '{"slug":"kit-whatsapp-pronto","experienceVersion":"kit-whatsapp-pronto-pde-v1","layoutKey":"assisted-service-v1","funnelVersion":"pde-assisted-service-v1","serviceScope":{"includedItems":["10 a 20 respostas personalizadas","5 a 10 perguntas de qualificação","3 a 5 follow-ups manuais"]},"missions":[{"id":"entrada-guiada"},{"id":"conferencia-de-completude"},{"id":"diagnostico-humano"},{"id":"microvalor-12h"},{"id":"entrega-completa-48h","deliveryContract":{"sections":[{"id":"responses","title":"Respostas","minItems":10,"maxItems":20},{"id":"qualificationQuestions","title":"Perguntas","minItems":5,"maxItems":10},{"id":"followUps","title":"Follow-ups","minItems":3,"maxItems":5}]}},{"id":"primeira-aplicacao-e-revisao"}]}';

INSERT INTO product (id, slug, pde_experience_json, updated_at)
VALUES (9, 'kit-whatsapp-pronto', @rigel_product_experience, '2026-08-30 00:00:00');

INSERT INTO pde_production_slot
  (id, product_slug, source_experiment_id, experience_version, layout_key, status,
   draft_experience_json, published_experience_json, published_by, published_at, updated_at)
VALUES
  (7, 'kit-whatsapp-pronto', 89, 'kit-whatsapp-pronto-pde-v1', 'assisted-service-v1',
   'ACTIVE', NULL, @rigel_slot_experience,
   'fixture-v1', '2026-08-22 21:55:06', '2026-08-22 21:55:42');
