INSERT INTO business_process_definition
  (process_code, name, purpose, owner_name, trigger_description, outcome_description,
   version_number, status, technical_reference, process_type, parent_process_code,
   execution_scope, diagram_json, created_at, published_at)
SELECT source.process_code,
       source.name,
       CONCAT(source.purpose, ' Atena define o nome interno e o tipo catalogado antes de Plutus e Dedalo.'),
       source.owner_name,
       source.trigger_description,
       source.outcome_description,
       9,
       'PUBLISHED',
       'docs/canonical/cadeia-produtos-pde-canon.v1.md / product-identity-v1',
       source.process_type,
       source.parent_process_code,
       source.execution_scope,
       JSON_SET(
         source.diagram_json,
         '$.schemaVersion', 'PDE_COMMERCIAL_PLAN_OFFER_V9',
         '$.productIdentityContractVersion', 'PRODUCT_IDENTITY_V1',
         '$.nodes[1].description', 'Entregar: desejo reconhecivel, publico/situacao, resultado desejado e linguagem real com fonte/data; escolher um nome interno de estrela ainda livre; e classificar o mecanismo em um tipo ACTIVE do catalogo, registrando codigo, mineral interno e justificativa. Aceite: comparar alternativas, selecionar no maximo uma candidata, rejeitar nome provisorio e distinguir tipo de formato. Medir: identidade integra orientacao de Plutus, Dedalo e Iris sem ser tratada como venda ou validacao.',
         '$.nodes[2].description', 'Entregar: preco como hipotese, receita liquida, CAC de Instagram Ads, custo integral por cliente/resultado e custos fixos, respeitando o tipo escolhido por Atena. Aceite: fonte e validade de cada premissa; contribuicao e margem atendem a politica. Medir: custo por resultado util, compras liquidas e margem; projecao nao e receita nem autorizacao de gasto.',
         '$.nodes[3].description', 'Entregar: arquitetura e formato coerentes com a estrategia, a economia e o tipo catalogado por Atena. Aceite: tipo descreve o mecanismo de valor e formato descreve a experiencia; o produto continua privado, instrumentado e sem cobranca real. Medir: tempo ate valor, uso do resultado pronto e sinais predeclarados.'
       ),
       UTC_TIMESTAMP(6),
       UTC_TIMESTAMP(6)
FROM business_process_definition source
LEFT JOIN business_process_definition existing
  ON existing.process_code=source.process_code AND existing.version_number=9
WHERE source.process_code='pde-commercial-plan-offer'
  AND source.version_number=8
  AND existing.id IS NULL;

INSERT INTO business_process_activity_definition
  (process_definition_id, activity_id, name, objective, owner_name,
   execution_resource_code, subprocess_code, definition_json, created_at)
SELECT process.id,
       JSON_UNQUOTE(JSON_EXTRACT(process.diagram_json, CONCAT('$.nodes[', node.number, '].id'))),
       JSON_UNQUOTE(JSON_EXTRACT(process.diagram_json, CONCAT('$.nodes[', node.number, '].label'))),
       JSON_UNQUOTE(JSON_EXTRACT(process.diagram_json, CONCAT('$.nodes[', node.number, '].description'))),
       JSON_UNQUOTE(JSON_EXTRACT(process.diagram_json, CONCAT('$.nodes[', node.number, '].owner'))),
       JSON_UNQUOTE(JSON_EXTRACT(process.diagram_json, CONCAT('$.nodes[', node.number, '].executionResourceCode'))),
       JSON_UNQUOTE(JSON_EXTRACT(process.diagram_json, CONCAT('$.nodes[', node.number, '].subprocessCode'))),
       JSON_EXTRACT(process.diagram_json, CONCAT('$.nodes[', node.number, ']')),
       UTC_TIMESTAMP(6)
FROM business_process_definition process
CROSS JOIN (
  SELECT 0 AS number UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3
  UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7
  UNION ALL SELECT 8 UNION ALL SELECT 9 UNION ALL SELECT 10 UNION ALL SELECT 11
) node
LEFT JOIN business_process_activity_definition existing
  ON existing.process_definition_id=process.id
 AND existing.activity_id=JSON_UNQUOTE(JSON_EXTRACT(process.diagram_json, CONCAT('$.nodes[', node.number, '].id')))
WHERE process.process_code='pde-commercial-plan-offer'
  AND process.version_number=9
  AND JSON_UNQUOTE(JSON_EXTRACT(process.diagram_json, CONCAT('$.nodes[', node.number, '].type')))='TASK'
  AND existing.id IS NULL;

INSERT INTO business_process_chain_definition
  (chain_code, name, purpose, outcome_description, primary_metric,
   version_number, status, created_at, published_at)
SELECT source.chain_code,
       source.name,
       CONCAT(source.purpose, ' PRODUCT_IDENTITY_V1: nome interno e tipo sao escolhidos no Processo 2 antes da economia e da construcao.'),
       source.outcome_description,
       source.primary_metric,
       23,
       'PUBLISHED',
       UTC_TIMESTAMP(6),
       UTC_TIMESTAMP(6)
FROM business_process_chain_definition source
LEFT JOIN business_process_chain_definition existing
  ON existing.chain_code=source.chain_code AND existing.version_number=23
WHERE source.chain_code='pde-value-creation-delivery'
  AND source.version_number=22
  AND existing.id IS NULL;

INSERT INTO business_process_chain_item
  (chain_definition_id, process_definition_id, sequence_number, value_contribution, created_at)
SELECT target.id,
       CASE WHEN replacement.id IS NOT NULL THEN replacement.id ELSE old_process.id END,
       item.sequence_number,
       CASE WHEN old_process.process_code='pde-commercial-plan-offer'
         THEN 'Define estrategia, identidade interna, tipo, economia e formato coerentes antes de construir o produto.'
         ELSE item.value_contribution END,
       UTC_TIMESTAMP(6)
FROM business_process_chain_definition source
JOIN business_process_chain_item item ON item.chain_definition_id=source.id
JOIN business_process_definition old_process ON old_process.id=item.process_definition_id
LEFT JOIN business_process_definition replacement
  ON replacement.process_code=old_process.process_code
 AND replacement.version_number=CASE old_process.process_code
       WHEN 'pde-commercial-plan-offer' THEN 9
       ELSE -1 END
JOIN business_process_chain_definition target
  ON target.chain_code=source.chain_code AND target.version_number=23
LEFT JOIN business_process_chain_item existing
  ON existing.chain_definition_id=target.id AND existing.sequence_number=item.sequence_number
WHERE source.chain_code='pde-value-creation-delivery'
  AND source.version_number=22
  AND existing.id IS NULL;

UPDATE business_process_definition
SET status='RETIRED'
WHERE process_code='pde-commercial-plan-offer' AND version_number=8;

UPDATE business_process_definition
SET status='PUBLISHED'
WHERE process_code='pde-commercial-plan-offer' AND version_number=9;

UPDATE business_process_chain_definition
SET status='RETIRED'
WHERE chain_code='pde-value-creation-delivery' AND version_number=22;

UPDATE business_process_chain_definition
SET status='PUBLISHED'
WHERE chain_code='pde-value-creation-delivery' AND version_number=23;
