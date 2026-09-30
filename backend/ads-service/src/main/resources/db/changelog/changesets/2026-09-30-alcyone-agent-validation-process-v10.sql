INSERT INTO business_process_definition
  (process_code, name, purpose, owner_name, trigger_description, outcome_description,
   version_number, status, technical_reference, process_type, parent_process_code,
   execution_scope, diagram_json, created_at, published_at)
SELECT source.process_code,
       'Estratégia, economia e homologação multiagente do PDE',
       CONCAT(source.purpose, ' O contrato nasce sem recrutamento, convite, leitura privada ou opinião solicitada; agentes comprovam prontidão e somente o mercado comprova demanda.'),
       source.owner_name,
       source.trigger_description,
       'Estratégia, identidade, economia e arquitetura prontas para construção e homologação multiagente, sem alegar validação humana, venda ou receita.',
       10,
       'PUBLISHED',
       'docs/canonical/pde-validacao-multiagente-canon.v1.md / MARKET_STRATEGY_V4 / PDE_AGENT_VALIDATION_V1',
       source.process_type,
       source.parent_process_code,
       source.execution_scope,
       JSON_SET(
         source.diagram_json,
         '$.schemaVersion', 'PDE_COMMERCIAL_PLAN_OFFER_V10',
         '$.validationContractVersion', 'MARKET_STRATEGY_V4',
         '$.agentValidationContractVersion', 'PDE_AGENT_VALIDATION_V1',
         '$.nodes[1].label', 'Selecionar candidata para homologação multiagente',
         '$.nodes[1].description', 'Entregar estratégia MARKET_STRATEGY_V4 com hipótese, ocasião, alternativa, valor pronto, fontes públicas vigentes e plano PDE_AGENT_VALIDATION_V1. Aceite: exatamente três cenários e três dispositivos, nenhum recrutamento, convite, leitura privada, opinião solicitada, pessoa fictícia, pagamento, publicação, campanha ou gasto. Agentes não constituem prova de mercado.',
         '$.nodes[2].label', 'Limitar economia da homologação',
         '$.nodes[2].description', 'Entregar preço e custo como hipóteses, envelope técnico finito e contribuição projetada. Aceite: custo auditável por pacote, nenhuma cobrança real e nenhum orçamento de mídia autorizado. Medir custo real da construção sem declarar venda ou margem comprovada.',
         '$.nodes[3].label', 'Arquitetar protótipo para cenários multiagente',
         '$.nodes[3].description', 'Entregar jornada, componentes, audiovisual, acesso e instrumentação para homologação ADHERENT, RECOVERY e SAFETY em desktop, iPhone e Pixel. Aceite: resultado pronto em até dez minutos, tráfego interno segregado e efeitos externos nulos.',
         '$.nodes[4].label', 'Contratos aptos à homologação multiagente?',
         '$.nodes[4].description', 'O backend bloqueia contratos humanos ou incompletos. Prontidão técnica permite construir; demanda continuará dependendo de comportamento voluntário, pagamento reconciliado e contribuição positiva.',
         '$.nodes[5].description', 'Materializa produto PLANNED com PDE_AGENT_VALIDATION_V1, sem contato, publicação, campanha, pagamento ou gasto.'
       ),
       UTC_TIMESTAMP(6),
       UTC_TIMESTAMP(6)
FROM business_process_definition source
LEFT JOIN business_process_definition existing
  ON existing.process_code=source.process_code AND existing.version_number=10
WHERE source.process_code='pde-commercial-plan-offer'
  AND source.version_number=9
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
  AND process.version_number=10
  AND JSON_UNQUOTE(JSON_EXTRACT(process.diagram_json, CONCAT('$.nodes[', node.number, '].type')))='TASK'
  AND existing.id IS NULL;

INSERT INTO business_process_chain_definition
  (chain_code, name, purpose, outcome_description, primary_metric,
   version_number, status, created_at, published_at)
SELECT source.chain_code,
       source.name,
       CONCAT(source.purpose, ' O Processo 2 usa MARKET_STRATEGY_V4 e PDE_AGENT_VALIDATION_V1 sem qualquer gate de piloto humano.'),
       source.outcome_description,
       source.primary_metric,
       24,
       'PUBLISHED',
       UTC_TIMESTAMP(6),
       UTC_TIMESTAMP(6)
FROM business_process_chain_definition source
LEFT JOIN business_process_chain_definition existing
  ON existing.chain_code=source.chain_code AND existing.version_number=24
WHERE source.chain_code='pde-value-creation-delivery'
  AND source.version_number=23
  AND existing.id IS NULL;

INSERT INTO business_process_chain_item
  (chain_definition_id, process_definition_id, sequence_number, value_contribution, created_at)
SELECT target.id,
       CASE WHEN replacement.id IS NOT NULL THEN replacement.id ELSE old_process.id END,
       item.sequence_number,
       CASE WHEN old_process.process_code='pde-commercial-plan-offer'
         THEN 'Define estratégia, identidade, economia e arquitetura prontas para homologação por agentes; o mercado permanece responsável por provar demanda e contribuição.'
         ELSE item.value_contribution END,
       UTC_TIMESTAMP(6)
FROM business_process_chain_definition source
JOIN business_process_chain_item item ON item.chain_definition_id=source.id
JOIN business_process_definition old_process ON old_process.id=item.process_definition_id
LEFT JOIN business_process_definition replacement
  ON replacement.process_code=old_process.process_code
 AND replacement.version_number=CASE old_process.process_code
       WHEN 'pde-commercial-plan-offer' THEN 10
       ELSE -1 END
JOIN business_process_chain_definition target
  ON target.chain_code=source.chain_code AND target.version_number=24
LEFT JOIN business_process_chain_item existing
  ON existing.chain_definition_id=target.id AND existing.sequence_number=item.sequence_number
WHERE source.chain_code='pde-value-creation-delivery'
  AND source.version_number=23
  AND existing.id IS NULL;

UPDATE business_process_definition
SET status='RETIRED'
WHERE process_code='pde-commercial-plan-offer' AND version_number=9;

UPDATE business_process_definition
SET status='PUBLISHED', published_at=COALESCE(published_at,UTC_TIMESTAMP(6))
WHERE process_code='pde-commercial-plan-offer' AND version_number=10;

UPDATE business_process_chain_definition
SET status='RETIRED'
WHERE chain_code='pde-value-creation-delivery' AND version_number=23;

UPDATE business_process_chain_definition
SET status='PUBLISHED', published_at=COALESCE(published_at,UTC_TIMESTAMP(6))
WHERE chain_code='pde-value-creation-delivery' AND version_number=24;
