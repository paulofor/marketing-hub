--liquibase formatted sql

--changeset repo:2031-08-05-create-prompt-table dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'prompt';
CREATE TABLE prompt (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(191) NOT NULL,
    domain VARCHAR(100) NOT NULL,
    template LONGTEXT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_prompt_domain_name (domain, name),
    INDEX idx_prompt_domain_active (domain, active)
);

--changeset repo:2031-08-05-add-prompt-to-niche-description dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'niche_detailed_description' AND column_name = 'prompt_id';
ALTER TABLE niche_detailed_description
    ADD COLUMN prompt_id BIGINT NULL AFTER market_niche_id,
    ADD CONSTRAINT fk_niche_detailed_description_prompt FOREIGN KEY (prompt_id) REFERENCES prompt(id);

--changeset repo:2031-08-05-seed-default-niche-description-prompt dbms:mysql
INSERT INTO prompt (name, domain, template, active, created_at, updated_at)
VALUES (
    'Descrição detalhada de nicho (padrão)',
    'NICHE_DETAILED_DESCRIPTION',
    CONCAT(
        'Gere ${quantity} descrições detalhadas em formato JSON.\n',
        'Cada item deve conter as chaves: "title", "overview", "pains", "desires", "needs".\n',
        'A chave overview deve ser um parágrafo único que explique dores, desejos e necessidades do público do nicho, pronto para ser reutilizado em outros prompts.\n',
        'As chaves pains, desires e needs devem ser listas (arrays JSON) com frases diretas.\n',
        'Use o seguinte nicho como contexto:\n',
        'Nome: ${niche.name}\n',
        '<#if niche.description?has_content>Descrição: ${niche.description}</#if>\n',
        '<#if niche.baseSegmentation?has_content>Segmentação base: ${niche.baseSegmentation}</#if>\n',
        '<#if niche.interests?has_content>Interesses: ${niche.interests}</#if>\n',
        '<#if niche.demographicFilters?has_content>Filtros demográficos: ${niche.demographicFilters}</#if>\n',
        '<#if niche.extraTips?has_content>Dicas extras: ${niche.extraTips}</#if>\n',
        'Retorne apenas o array JSON com os objetos solicitados, sem texto adicional.'
    ),
    TRUE,
    NOW(),
    NOW()
)
ON DUPLICATE KEY UPDATE
    template = VALUES(template),
    active = VALUES(active),
    updated_at = VALUES(updated_at);

--changeset repo:2031-08-05-backfill-niche-description-prompt dbms:mysql
UPDATE niche_detailed_description
SET prompt_id = (
    SELECT id FROM prompt WHERE domain = 'NICHE_DETAILED_DESCRIPTION' AND active = TRUE LIMIT 1
)
WHERE prompt_id IS NULL;
