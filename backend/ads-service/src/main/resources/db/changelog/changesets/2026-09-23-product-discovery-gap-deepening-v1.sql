INSERT INTO business_process_definition
  (process_code, name, purpose, owner_name, trigger_description, outcome_description,
   version_number, status, technical_reference, process_type, parent_process_code,
   execution_scope, diagram_json, created_at, published_at)
SELECT source.process_code,
       source.name,
       'Encontrar situações concretas em que o desejo vira ação, qualificar evidências por candidata e aprofundar lacunas com comportamento passado antes de entregar o dossiê a Atena.',
       source.owner_name,
       source.trigger_description,
       'Dossiê factual com ocasiões de compra, fontes qualificadas, entrevistas consentidas, lacunas e limites auditáveis; somente então pronto para decisão estratégica.',
       7,
       'PUBLISHED',
       'docs/canonical/descoberta-produtos-pde-canon.v1.md / candidate-gap-deepening-v1',
       source.process_type,
       source.parent_process_code,
       source.execution_scope,
       '{"nodes":[{"id":"start","type":"START","label":"Pergunta de mercado recebida","owner":"Backend","description":"O tema, público, canal, restrições, objetivo e origem da pesquisa ficam congelados."},{"id":"marketEvidence","type":"TASK","label":"Reunir evidências factuais e situações de compra","owner":"Argos","description":"Entregar duas ou três candidatas com ocasião concreta, resultado desejado, dificuldade e alternativa usada; qualificar ofertas atuais por preço, entrega, público, aderência e data; distinguir linguagem de vendedores, relatos de clientes, anúncios e artigos científicos. Registrar lacunas por candidata, sem escolher mercado, oferta ou produto. Aceite: fontes rastreáveis, atuais quando a decisão exigir e separadas por função; referência antiga ou indireta permanece marcada. Medir cobertura, atualidade, independência, ofertas aderentes e custo conhecido da coleta.","responsibleAgentKeys":["market-radar"],"responsibilityDomain":"MARKET_EVIDENCE"},{"id":"candidateGapDeepening","type":"TASK","label":"Aprofundar lacunas da candidata","owner":"Argos","description":"Entregar um plano por candidata com pergunta pendente, fonte adequada, evidência necessária, contraponto, consultas executadas e limite de consultas/custo. Reutilizar fatos válidos e confrontá-los com cinco a oito entrevistas consentidas e anônimas sobre comportamento passado, incluindo compra e desistência, ocasião, resultado desejado, dificuldade, alternativa, gasto conhecido e obstáculo residual. Aceite: preservar identidades das candidatas, cobrir todas, registrar o que resolveu ou não cada lacuna e parar sem aprovação quando não houver progresso ou o teto for atingido. As entrevistas orientam hipóteses e não estimam mercado.","responsibleAgentKeys":["market-radar"],"responsibilityDomain":"MARKET_EVIDENCE"},{"id":"candidateFound","type":"GATEWAY","label":"Há candidatas factuais?","owner":"Backend","description":"Sem candidata factual, encerra honestamente sem entrevista, nova busca ou handoff; com duas ou três candidatas, exige evidência comportamental antes do aprofundamento."},{"id":"gate","type":"GATEWAY","label":"Dossiê aprofundado suficiente?","owner":"Backend","description":"Exige as duas atividades, de cinco a oito entrevistas, compra e desistência, cobertura de todas as candidatas, fontes qualificadas e limites comprovados. Lacuna persistente mantém RESEARCH_MORE; o gate não fabrica oportunidade."},{"id":"end","type":"END","label":"Pesquisa encerrada ou dossiê entregue a Atena","owner":"Backend","description":"Sem candidata, a pesquisa termina sem fabricar avanço. Com dossiê factual, Atena pode definir a proposta; Plutus deve validar a economia; Dédalo deve materializar a menor experiência útil; utilidade humana e compra rentável continuam sendo comprovadas somente nos processos posteriores com compras líquidas, reembolsos, custo integral e margem."}],"flows":[{"from":"start","to":"marketEvidence"},{"from":"marketEvidence","to":"candidateFound"},{"from":"candidateFound","to":"candidateGapDeepening","condition":"duas ou três candidatas factuais"},{"from":"candidateFound","to":"end","condition":"nenhuma candidata factual"},{"from":"candidateGapDeepening","to":"gate"},{"from":"gate","to":"end"}],"researchContractVersion":"CANDIDATE_GAP_DEEPENING_V1"}',
       UTC_TIMESTAMP(6),
       UTC_TIMESTAMP(6)
FROM business_process_definition source
LEFT JOIN business_process_definition existing
  ON existing.process_code = source.process_code AND existing.version_number = 7
WHERE source.process_code = 'pde-opportunity-discovery'
  AND source.version_number = 6
  AND existing.id IS NULL;

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
CROSS JOIN (SELECT 1 AS number UNION ALL SELECT 2 AS number) node
LEFT JOIN business_process_activity_definition existing
  ON existing.process_definition_id = process.id
 AND existing.activity_id = JSON_UNQUOTE(JSON_EXTRACT(process.diagram_json, CONCAT('$.nodes[', node.number, '].id')))
WHERE process.process_code = 'pde-opportunity-discovery'
  AND process.version_number = 7
  AND existing.id IS NULL;

INSERT INTO business_process_chain_definition
  (chain_code, name, purpose, outcome_description, primary_metric,
   version_number, status, created_at, published_at)
SELECT source.chain_code,
       source.name,
       CONCAT(source.purpose, ' Descoberta aprofunda lacunas por candidata antes da estratégia.'),
       source.outcome_description,
       source.primary_metric,
       19,
       'PUBLISHED',
       UTC_TIMESTAMP(6),
       UTC_TIMESTAMP(6)
FROM business_process_chain_definition source
LEFT JOIN business_process_chain_definition existing
  ON existing.chain_code = source.chain_code AND existing.version_number = 19
WHERE source.chain_code = 'pde-value-creation-delivery'
  AND source.version_number = 18
  AND existing.id IS NULL;

INSERT INTO business_process_chain_item
  (chain_definition_id, process_definition_id, sequence_number, value_contribution, created_at)
SELECT target.id,
       CASE WHEN old_process.process_code = 'pde-opportunity-discovery'
            THEN new_process.id ELSE old_process.id END,
       item.sequence_number,
       item.value_contribution,
       UTC_TIMESTAMP(6)
FROM business_process_chain_definition source
JOIN business_process_chain_item item ON item.chain_definition_id = source.id
JOIN business_process_definition old_process ON old_process.id = item.process_definition_id
LEFT JOIN business_process_definition new_process
  ON new_process.process_code = 'pde-opportunity-discovery' AND new_process.version_number = 7
JOIN business_process_chain_definition target
  ON target.chain_code = source.chain_code AND target.version_number = 19
LEFT JOIN business_process_chain_item existing
  ON existing.chain_definition_id = target.id AND existing.sequence_number = item.sequence_number
WHERE source.chain_code = 'pde-value-creation-delivery'
  AND source.version_number = 18
  AND existing.id IS NULL;

UPDATE business_process_definition
SET status = 'RETIRED'
WHERE process_code = 'pde-opportunity-discovery' AND version_number = 6;

UPDATE business_process_definition
SET status = 'PUBLISHED'
WHERE process_code = 'pde-opportunity-discovery' AND version_number = 7;

UPDATE business_process_chain_definition
SET status = 'RETIRED'
WHERE chain_code = 'pde-value-creation-delivery' AND version_number = 18;

UPDATE business_process_chain_definition
SET status = 'PUBLISHED'
WHERE chain_code = 'pde-value-creation-delivery' AND version_number = 19;
