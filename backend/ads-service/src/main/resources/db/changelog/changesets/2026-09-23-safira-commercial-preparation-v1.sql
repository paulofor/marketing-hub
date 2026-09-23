INSERT INTO business_process_definition
  (process_code, name, purpose, owner_name, trigger_description, outcome_description,
   version_number, status, technical_reference, process_type, parent_process_code,
   execution_scope, diagram_json, created_at, published_at)
SELECT 'safira-commercial-preparation-v1',
       'Preparar operação comercial Safira',
       'Preparar uma oferta pública de Produto IA com valor inicial útil, continuidade paga clara e margem sustentável.',
       'Backend, Psique, Têmis e Plutus',
       'Produto Safira, experimento comercial e experiência pública exatos foram identificados.',
       'Oferta, economia e pareceres independentes comprovados; mídia e ativação continuam dependentes de preflight e autorização humana.',
       1,
       'PUBLISHED',
       'docs/canonical/product-types-canon.v1.md / safira-commercial-preparation-v1',
       'SUBPROCESS',
       'pde-commercial-homologation-activation',
       'PRODUCT',
       '{"schemaVersion":"SAFIRA_COMMERCIAL_PREPARATION_V1","productTypeCode":"AI_PRODUCT","nodes":[{"id":"start","type":"START","label":"Safira e experimento comercial identificados"},{"id":"journey","type":"TASK","label":"Conferir jornada pública e prova de valor","owner":"Backend","description":"Entregar experiência pública do mesmo experimento, desejo reconhecido com fonte, primeira ação simples no celular, demonstração útil, continuidade paga clara, criativo, público, checkout, entrega, suporte, reembolso e eventos; a validação privada é referência do produto, não evidência humana ou comercial."},{"id":"economics","type":"TASK","label":"Conferir margem e parecer de Plutus","owner":"Backend","description":"Reutilizar plano financeiro LIVE da mesma versão e plano comercial, com IA, infraestrutura, entrega, suporte, mídia, taxas e reembolsos nos cenários conservador, esperado e intensivo."},{"id":"humanExperienceReview","type":"TASK","label":"Homologar experiência de compra e uso do Produto IA","owner":"Psique","description":"Avaliar desejo, esforço, demonstração, diferença paga, segurança, privacidade, compra, entrega e utilidade sem inventar cliente, uso, venda ou resultado.","responsibleAgentKeys":["customer-agent"]},{"id":"commercialIntegrityReview","type":"TASK","label":"Revisar integridade comercial Safira","owner":"Têmis","description":"Conferir promessa, preço, subtipo, anúncio, experiência, checkout, entrega, segurança, custos, suporte e métricas da mesma versão.","responsibleAgentKeys":["meta-ad-approver"]},{"id":"ready","type":"TASK","label":"Concluir preparação comercial Safira","owner":"Backend","description":"Revalidar jornada, economia, fingerprints e pareceres; retornar ao pai sem publicar campanha, autorizar gasto ou alegar venda."},{"id":"end","type":"END","label":"Preparação Safira comprovada; retornar ao processo pai"}],"flows":[{"from":"start","to":"journey"},{"from":"journey","to":"economics"},{"from":"economics","to":"humanExperienceReview"},{"from":"humanExperienceReview","to":"commercialIntegrityReview"},{"from":"commercialIntegrityReview","to":"ready"},{"from":"ready","to":"end"}]}',
       UTC_TIMESTAMP(6),
       UTC_TIMESTAMP(6)
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM business_process_definition
  WHERE process_code = 'safira-commercial-preparation-v1' AND version_number = 1
);

INSERT INTO business_process_activity_definition
  (process_definition_id, activity_id, name, objective, owner_name,
   execution_resource_code, subprocess_code, definition_json, created_at)
SELECT process.id,
       JSON_UNQUOTE(JSON_EXTRACT(process.diagram_json, CONCAT('$.nodes[', node.number, '].id'))),
       JSON_UNQUOTE(JSON_EXTRACT(process.diagram_json, CONCAT('$.nodes[', node.number, '].label'))),
       JSON_UNQUOTE(JSON_EXTRACT(process.diagram_json, CONCAT('$.nodes[', node.number, '].description'))),
       JSON_UNQUOTE(JSON_EXTRACT(process.diagram_json, CONCAT('$.nodes[', node.number, '].owner'))),
       NULL,
       NULL,
       JSON_EXTRACT(process.diagram_json, CONCAT('$.nodes[', node.number, ']')),
       UTC_TIMESTAMP(6)
FROM business_process_definition process
CROSS JOIN (
  SELECT 1 AS number UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5
) node
LEFT JOIN business_process_activity_definition existing
  ON existing.process_definition_id = process.id
 AND existing.activity_id = JSON_UNQUOTE(JSON_EXTRACT(process.diagram_json, CONCAT('$.nodes[', node.number, '].id')))
WHERE process.process_code = 'safira-commercial-preparation-v1'
  AND process.version_number = 1
  AND existing.id IS NULL;

INSERT INTO business_process_definition
  (process_code, name, purpose, owner_name, trigger_description, outcome_description,
   version_number, status, technical_reference, process_type, parent_process_code,
   execution_scope, diagram_json, created_at, published_at)
SELECT source.process_code,
       source.name,
       source.purpose,
       source.owner_name,
       source.trigger_description,
       source.outcome_description,
       9,
       'PUBLISHED',
       'docs/canonical/cadeia-produtos-pde-canon.v1.md / commercial-preparation-safira-v1',
       source.process_type,
       source.parent_process_code,
       source.execution_scope,
       JSON_SET(
         source.diagram_json,
         '$.schemaVersion', 'PDE_COMMERCIAL_HOMOLOGATION_ACTIVATION_V9',
         '$.nodes[1].subprocessRoutes',
         JSON_ARRAY(
           JSON_OBJECT('productTypeCode','PDE','productTypeInternalName','Opala','subprocessCode','opala-commercial-preparation-v1','subprocessVersion',1),
           JSON_OBJECT('productTypeCode','LOW_TICKET_DIGITAL_PRODUCT','productTypeInternalName','Quartzo','subprocessCode','quartzo-commercial-preparation-v1','subprocessVersion',1),
           JSON_OBJECT('productTypeCode','AI_PRODUCT','productTypeInternalName','Safira','subprocessCode','safira-commercial-preparation-v1','subprocessVersion',1)
         )
       ),
       UTC_TIMESTAMP(6),
       UTC_TIMESTAMP(6)
FROM business_process_definition source
LEFT JOIN business_process_definition existing
  ON existing.process_code = source.process_code AND existing.version_number = 9
WHERE source.process_code = 'pde-commercial-homologation-activation'
  AND source.version_number = 8
  AND existing.id IS NULL;

INSERT INTO business_process_activity_definition
  (process_definition_id, activity_id, name, objective, owner_name,
   execution_resource_code, subprocess_code, definition_json, created_at)
SELECT target.id,
       source_activity.activity_id,
       source_activity.name,
       source_activity.objective,
       source_activity.owner_name,
       source_activity.execution_resource_code,
       source_activity.subprocess_code,
       CASE WHEN source_activity.activity_id = 'commercialPreparation'
            THEN JSON_SET(
              source_activity.definition_json,
              '$.subprocessRoutes',
              JSON_ARRAY(
                JSON_OBJECT('productTypeCode','PDE','productTypeInternalName','Opala','subprocessCode','opala-commercial-preparation-v1','subprocessVersion',1),
                JSON_OBJECT('productTypeCode','LOW_TICKET_DIGITAL_PRODUCT','productTypeInternalName','Quartzo','subprocessCode','quartzo-commercial-preparation-v1','subprocessVersion',1),
                JSON_OBJECT('productTypeCode','AI_PRODUCT','productTypeInternalName','Safira','subprocessCode','safira-commercial-preparation-v1','subprocessVersion',1)
              )
            )
            ELSE source_activity.definition_json END,
       UTC_TIMESTAMP(6)
FROM business_process_definition source
JOIN business_process_activity_definition source_activity
  ON source_activity.process_definition_id = source.id
JOIN business_process_definition target
  ON target.process_code = source.process_code AND target.version_number = 9
LEFT JOIN business_process_activity_definition existing
  ON existing.process_definition_id = target.id
 AND existing.activity_id = source_activity.activity_id
WHERE source.process_code = 'pde-commercial-homologation-activation'
  AND source.version_number = 8
  AND existing.id IS NULL;

INSERT INTO business_process_chain_definition
  (chain_code, name, purpose, outcome_description, primary_metric,
   version_number, status, created_at, published_at)
SELECT source.chain_code,
       source.name,
       CONCAT(source.purpose, ' Produtos IA usam preparação Safira antes do preflight e da autorização humana.'),
       source.outcome_description,
       source.primary_metric,
       20,
       'PUBLISHED',
       UTC_TIMESTAMP(6),
       UTC_TIMESTAMP(6)
FROM business_process_chain_definition source
LEFT JOIN business_process_chain_definition existing
  ON existing.chain_code = source.chain_code AND existing.version_number = 20
WHERE source.chain_code = 'pde-value-creation-delivery'
  AND source.version_number = 19
  AND existing.id IS NULL;

INSERT INTO business_process_chain_item
  (chain_definition_id, process_definition_id, sequence_number, value_contribution, created_at)
SELECT target.id,
       CASE WHEN old_process.process_code = 'pde-commercial-homologation-activation'
            THEN new_process.id ELSE old_process.id END,
       item.sequence_number,
       item.value_contribution,
       UTC_TIMESTAMP(6)
FROM business_process_chain_definition source
JOIN business_process_chain_item item ON item.chain_definition_id = source.id
JOIN business_process_definition old_process ON old_process.id = item.process_definition_id
LEFT JOIN business_process_definition new_process
  ON new_process.process_code = 'pde-commercial-homologation-activation'
 AND new_process.version_number = 9
JOIN business_process_chain_definition target
  ON target.chain_code = source.chain_code AND target.version_number = 20
LEFT JOIN business_process_chain_item existing
  ON existing.chain_definition_id = target.id AND existing.sequence_number = item.sequence_number
WHERE source.chain_code = 'pde-value-creation-delivery'
  AND source.version_number = 19
  AND existing.id IS NULL;

UPDATE business_process_definition
SET status = 'RETIRED'
WHERE process_code = 'pde-commercial-homologation-activation' AND version_number = 8;

UPDATE business_process_definition
SET status = 'PUBLISHED'
WHERE (process_code = 'pde-commercial-homologation-activation' AND version_number = 9)
   OR (process_code = 'safira-commercial-preparation-v1' AND version_number = 1);

UPDATE business_process_chain_definition
SET status = 'RETIRED'
WHERE chain_code = 'pde-value-creation-delivery' AND version_number = 19;

UPDATE business_process_chain_definition
SET status = 'PUBLISHED'
WHERE chain_code = 'pde-value-creation-delivery' AND version_number = 20;
