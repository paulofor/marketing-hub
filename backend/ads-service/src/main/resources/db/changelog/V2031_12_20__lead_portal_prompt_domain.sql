--liquibase formatted sql

--changeset repo:2031-12-20-lead-portal-domain dbms:mysql
INSERT INTO prompt_domain (code, name, description, created_at, updated_at)
VALUES ('LEAD_PORTAL_FLOW', 'Fluxo do portal do lead', 'Templates para geração de fluxos do portal do lead', NOW(), NOW())
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    description = VALUES(description),
    updated_at = VALUES(updated_at);

--changeset repo:2031-12-20-lead-portal-domain-objects dbms:mysql
INSERT INTO prompt_domain_object (prompt_domain_id, object_type)
SELECT pd.id, 'EXPERIMENT'
FROM prompt_domain pd
WHERE pd.code = 'LEAD_PORTAL_FLOW'
  AND NOT EXISTS (
      SELECT 1 FROM prompt_domain_object pdo
      WHERE pdo.prompt_domain_id = pd.id
        AND pdo.object_type = 'EXPERIMENT'
  );

INSERT INTO prompt_domain_object (prompt_domain_id, object_type)
SELECT pd.id, 'HYPOTHESIS'
FROM prompt_domain pd
WHERE pd.code = 'LEAD_PORTAL_FLOW'
  AND NOT EXISTS (
      SELECT 1 FROM prompt_domain_object pdo
      WHERE pdo.prompt_domain_id = pd.id
        AND pdo.object_type = 'HYPOTHESIS'
  );

INSERT INTO prompt_domain_object (prompt_domain_id, object_type)
SELECT pd.id, 'NICHE'
FROM prompt_domain pd
WHERE pd.code = 'LEAD_PORTAL_FLOW'
  AND NOT EXISTS (
      SELECT 1 FROM prompt_domain_object pdo
      WHERE pdo.prompt_domain_id = pd.id
        AND pdo.object_type = 'NICHE'
  );

--changeset repo:2031-12-20-lead-portal-prompt dbms:mysql
INSERT INTO prompt (name, domain, template, active, created_at, updated_at)
VALUES (
    'Geração de fluxos do portal do lead (padrão)',
    'LEAD_PORTAL_FLOW',
    CONCAT(
        'Gere ${quantity} fluxos para portal de leads em português no formato JSON.\n',
        'Cada item deve conter: "name" (título amigável), "slug" (kebab-case), "description" (objetivo do fluxo) e "questions".\n',
        'Em "questions" informe objetos com as chaves: "title", "dataKey" (snake_case), "type" (TEXT, TEXTAREA, NUMBER, EMAIL, PHONE, DATE, SINGLE_CHOICE, MULTIPLE_CHOICE ou IMAGE_UPLOAD),\n',
        '"required" (booleano), "description", "placeholder" e "options" (array, obrigatório para SINGLE_CHOICE ou MULTIPLE_CHOICE).\n',
        'As perguntas devem ajudar o lead a refletir sobre dores e objetivos, oferecendo opções de resposta realistas quando houver múltipla escolha.\n',
        'Finalize SEMPRE cada fluxo com uma pergunta do tipo IMAGE_UPLOAD solicitando uma foto clara que represente o negócio ou o problema mencionado para gerar materiais de divulgação.\n\n',
        '<#if experiment??>Experimento: ${experiment.name}\n',
        '<#if experiment.hypothesisSummary?has_content>Resumo do experimento: ${experiment.hypothesisSummary}\n</#if>',
        '<#if experiment.leadPortalFlowsToGenerate??>Fluxos solicitados: ${experiment.leadPortalFlowsToGenerate}\n</#if>',
        '</#if>',
        '<#if hypothesis??>Problema do lead: ${hypothesis.problem}\n',
        '<#if hypothesis.promise?has_content>Promessa da solução: ${hypothesis.promise}\n</#if>',
        '<#if hypothesis.persona?has_content>Persona: ${hypothesis.persona}\n</#if>',
        '</#if>',
        '<#if niche??>Nicho: ${niche.name}\n',
        '<#if niche.description?has_content>Descrição: ${niche.description}\n</#if>',
        '<#if niche.baseSegmentation?has_content>Segmentação base: ${niche.baseSegmentation}\n</#if>',
        '<#if niche.interests?has_content>Interesses: ${niche.interests}\n</#if>',
        '</#if>\n',
        'Responda somente com um array JSON válido, sem comentários ou texto adicional.'
    ),
    TRUE,
    NOW(),
    NOW()
)
ON DUPLICATE KEY UPDATE
    template = VALUES(template),
    active = VALUES(active),
    updated_at = VALUES(updated_at);
