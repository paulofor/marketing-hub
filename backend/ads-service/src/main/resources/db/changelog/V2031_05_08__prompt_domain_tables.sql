CREATE TABLE prompt_domain (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(100) NOT NULL,
    name VARCHAR(191) NOT NULL,
    description VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_prompt_domain_code UNIQUE (code)
);

CREATE TABLE prompt_domain_object (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    prompt_domain_id BIGINT NOT NULL,
    object_type VARCHAR(64) NOT NULL,
    CONSTRAINT fk_prompt_domain_object_domain FOREIGN KEY (prompt_domain_id) REFERENCES prompt_domain(id) ON DELETE CASCADE,
    CONSTRAINT uq_prompt_domain_object UNIQUE (prompt_domain_id, object_type)
);

INSERT INTO prompt_domain (code, name, description)
VALUES
    ('NICHE_DETAILED_DESCRIPTION', 'Descrição detalhada de nicho', 'Geração de descrições detalhadas para nichos'),
    ('NICHE_HYPOTHESIS', 'Geração de hipóteses de nicho', 'Geração de hipóteses baseadas em nichos e descrições detalhadas');

INSERT INTO prompt_domain_object (prompt_domain_id, object_type)
SELECT id, 'NICHE'
FROM prompt_domain
WHERE code = 'NICHE_DETAILED_DESCRIPTION';

INSERT INTO prompt_domain_object (prompt_domain_id, object_type)
SELECT id, 'NICHE'
FROM prompt_domain
WHERE code = 'NICHE_HYPOTHESIS';

INSERT INTO prompt_domain_object (prompt_domain_id, object_type)
SELECT id, 'DETAILED_DESCRIPTION'
FROM prompt_domain
WHERE code = 'NICHE_HYPOTHESIS';

INSERT INTO prompt_domain_object (prompt_domain_id, object_type)
SELECT id, 'DIFFERENTIATED_TECHNOLOGY'
FROM prompt_domain
WHERE code = 'NICHE_HYPOTHESIS';

INSERT INTO prompt_domain_object (prompt_domain_id, object_type)
SELECT id, 'HYPOTHESIS'
FROM prompt_domain
WHERE code = 'NICHE_HYPOTHESIS';
