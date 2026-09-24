INSERT INTO business_process_definition
  (process_code, name, purpose, owner_name, trigger_description, outcome_description,
   version_number, status, technical_reference, process_type, parent_process_code,
   execution_scope, diagram_json, created_at, published_at)
SELECT source.process_code,
       source.name,
       CONCAT(source.purpose, ' A aquisicao comercial usa exclusivamente midia paga no Instagram; convite individual, base propria e divulgacao organica nao sao canais executaveis.'),
       source.owner_name,
       source.trigger_description,
       source.outcome_description,
       8,
       'PUBLISHED',
       'docs/canonical/cadeia-produtos-pde-canon.v1.md / paid-instagram-only-v1',
       source.process_type,
       source.parent_process_code,
       source.execution_scope,
       JSON_SET(
         source.diagram_json,
         '$.commercialAcquisitionPolicyVersion', 'PAID_INSTAGRAM_ONLY_V1',
         '$.nodes[1].description', 'Entregar: desejo reconhecivel, publico/situacao, resultado desejado e linguagem real com fonte/data, alem de Instagram Ads (Meta Ads) como unico canal vigente de divulgacao comercial. Aceite: selecionar no maximo uma candidata, fiel ao tipo e a ficha, sem provocar inadequacao e sem propor convite individual, lista propria, contato direto ou divulgacao organica; congelar a referencia e separar fato, hipotese e explicacao concorrente. Medir: visitantes humanos atribuidos a campanha paga -> primeira interacao por variante e janela. Participacao nao comprova compra e esta atividade nao autoriza gasto.',
         '$.nodes[2].description', 'Entregar: preco como hipotese, receita liquida, CAC de Instagram Ads, custo integral por cliente/resultado e custos fixos, cenarios conservador, esperado e intensivo, teto e travas. Aceite: fonte e validade de cada premissa; custo de midia ainda nao aprovado permanece desconhecido, nunca zero; contribuicao e margem atendem a politica. Medir: custo por resultado util, compras liquidas e margem; projecao nao e receita nem autorizacao de gasto.'
       ),
       UTC_TIMESTAMP(6),
       UTC_TIMESTAMP(6)
FROM business_process_definition source
LEFT JOIN business_process_definition existing
  ON existing.process_code=source.process_code AND existing.version_number=8
WHERE source.process_code='pde-commercial-plan-offer'
  AND source.version_number=7
  AND existing.id IS NULL;

INSERT INTO business_process_definition
  (process_code, name, purpose, owner_name, trigger_description, outcome_description,
   version_number, status, technical_reference, process_type, parent_process_code,
   execution_scope, diagram_json, created_at, published_at)
SELECT source.process_code,
       source.name,
       CONCAT(source.purpose, ' Toda divulgacao comercial e preparada para midia paga no Instagram, sem convite individual.'),
       source.owner_name,
       source.trigger_description,
       source.outcome_description,
       9,
       'PUBLISHED',
       'docs/canonical/cadeia-produtos-pde-canon.v1.md / paid-instagram-only-v1',
       source.process_type,
       source.parent_process_code,
       source.execution_scope,
       JSON_SET(
         source.diagram_json,
         '$.commercialAcquisitionPolicyVersion', 'PAID_INSTAGRAM_ONLY_V1',
         '$.nodes[1].description', 'Entregar: desejo escolhido por Atena, linguagem do publico com fonte/data, mensagem, CTA e prova real da versao aprovada para Instagram Ads; explicitar beneficio inicial e beneficio adicional pago, entregaveis, uso, preco, prazo, acesso, suporte e reembolso. Aceite: fidelidade ao tipo, ficha e oferta; nenhuma emocao inferida como fato; convite individual, lista propria, contato direto e divulgacao organica nao sao canais substitutos. Medir: primeira interacao e oferta efetivamente vista -> CTA -> checkout -> compra, por campanha, variante e janela.',
         '$.nodes[2].description', 'Entregar: criativos finais para Instagram Ads no formato aprovado pelo contrato do produto, com identidade publica, promessa, prova, CTA e destino coerentes. Aceite: revisao independente, direitos de uso e correspondencia com oferta, checkout e entrega; nao produzir mensagem de convite individual. Medir: impressoes atribuidas -> primeira interacao e custo por etapa, sem confundir producao com desempenho.',
         '$.nodes[3].description', 'Entregar: destino aprovado que torna a primeira acao evidente e demonstra valor real antes do compromisso, conforme o contrato do produto. Aceite: receber trafego pago do Instagram com continuidade, checkout e recuperacao funcionais em celular; cumprir a promessa inicial e explicar o ganho pago. Medir: visitantes atribuidos -> primeira acao -> beneficio e exposicao efetiva a oferta.',
         '$.nodes[4].description', 'Entregar: Instagram Ads, destino, checkout e acesso versionados com UTM, campanha, criativo e eventos correlacionados ate venda, entrega e primeiro uso. Aceite: identidade oficial do Instagram e publico salvo; segregar QA, bots, duplicidades e historicos diretos; convite individual, lista propria, contato direto ou organico nao suprem aquisicao. Medir: impressao -> visita -> beneficio -> oferta vista -> clique -> checkout -> compra conciliada. Nao publicar nem autorizar gasto.'
       ),
       UTC_TIMESTAMP(6),
       UTC_TIMESTAMP(6)
FROM business_process_definition source
LEFT JOIN business_process_definition existing
  ON existing.process_code=source.process_code AND existing.version_number=9
WHERE source.process_code='pde-communication-sales-journey'
  AND source.version_number=8
  AND existing.id IS NULL;

INSERT INTO business_process_definition
  (process_code, name, purpose, owner_name, trigger_description, outcome_description,
   version_number, status, technical_reference, process_type, parent_process_code,
   execution_scope, diagram_json, created_at, published_at)
SELECT source.process_code,
       source.name,
       CONCAT(source.purpose, ' A homologacao aceita somente aquisicao paga no Instagram e mantem o gasto dependente de decisao humana.'),
       source.owner_name,
       source.trigger_description,
       source.outcome_description,
       10,
       'PUBLISHED',
       'docs/canonical/cadeia-produtos-pde-canon.v1.md / paid-instagram-only-v1',
       source.process_type,
       source.parent_process_code,
       source.execution_scope,
       JSON_SET(
         source.diagram_json,
         '$.schemaVersion', 'PDE_COMMERCIAL_HOMOLOGATION_ACTIVATION_V10',
         '$.commercialAcquisitionPolicyVersion', 'PAID_INSTAGRAM_ONLY_V1',
         '$.nodes[1].description', 'Seleciona pelo tipo cadastrado e pela ficha o subprocesso comercial aplicavel. A candidata deve usar Instagram Ads (Meta Ads); convite individual, lista propria, contato direto e divulgacao organica bloqueiam a preparacao, sem inferencia pelo nome ou formato do produto.',
         '$.nodes[1].subprocessRoutes', JSON_ARRAY(
           JSON_OBJECT('productTypeCode','PDE','productTypeInternalName','Opala','subprocessCode','opala-commercial-preparation-v1','subprocessVersion',1),
           JSON_OBJECT('productTypeCode','LOW_TICKET_DIGITAL_PRODUCT','productTypeInternalName','Quartzo','subprocessCode','quartzo-commercial-preparation-v1','subprocessVersion',1),
           JSON_OBJECT('productTypeCode','AI_PRODUCT','productTypeInternalName','Safira','subprocessCode','safira-commercial-preparation-v1','subprocessVersion',2)
         ),
         '$.nodes[2].description', 'Reutiliza o parecer de Psique da mesma preparacao e confere a experiencia real recebida pelo trafego pago do Instagram; mudanca material ou canal divergente exige renovacao.',
         '$.nodes[3].description', 'Reutiliza o parecer independente de Temis e confere promessa, criativo, Instagram, publico, preco, checkout e entrega da mesma versao; divergencia bloqueia e retorna a origem.',
         '$.nodes[4].description', 'Chama a homologacao tecnica para comprovar desktop, celular, compra simulada, acesso, entrega, falhas, eventos, segregacao e limites do experimento de Instagram Ads, reutilizando evidencias vigentes.',
         '$.nodes[6].description', 'Aprova explicitamente versao, Instagram Ads, teto financeiro, periodo e condicoes de parada. Nenhum gate tecnico concede gasto; convite individual, lista propria, contato direto ou divulgacao organica nao podem ser autorizados.'
       ),
       UTC_TIMESTAMP(6),
       UTC_TIMESTAMP(6)
FROM business_process_definition source
LEFT JOIN business_process_definition existing
  ON existing.process_code=source.process_code AND existing.version_number=10
WHERE source.process_code='pde-commercial-homologation-activation'
  AND source.version_number=9
  AND existing.id IS NULL;

INSERT INTO business_process_definition
  (process_code, name, purpose, owner_name, trigger_description, outcome_description,
   version_number, status, technical_reference, process_type, parent_process_code,
   execution_scope, diagram_json, created_at, published_at)
SELECT source.process_code,
       source.name,
       CONCAT(source.purpose, ' A operacao comercial mede exclusivamente a aquisicao paga atribuida ao Instagram.'),
       source.owner_name,
       source.trigger_description,
       source.outcome_description,
       10,
       'PUBLISHED',
       'docs/canonical/cadeia-produtos-pde-canon.v1.md / paid-instagram-only-v1',
       source.process_type,
       source.parent_process_code,
       source.execution_scope,
       JSON_SET(
         source.diagram_json,
         '$.commercialAcquisitionPolicyVersion', 'PAID_INSTAGRAM_ONLY_V1',
         '$.nodes[0].description', 'A operacao comeca somente apos autorizacao do experimento de Instagram Ads ou referencia historica comprovada; convite individual, lista propria, contato direto e divulgacao organica nao iniciam nova operacao.',
         '$.nodes[4].description', 'Delega aquisicao paga no Instagram, mensuracao atribuida e teste de uma variavel ao subprocesso canonico, sempre dentro do teto, janela e condicoes de parada autorizados. Nao criar ou importar contatos individuais.',
         '$.nodes[6].description', 'O backend concilia fontes do experimento de Instagram Ads por campanha, criativo, UTM e versao. Fonte invalida bloqueia; QA, bots, trafego direto, organico ou convite individual nao comprovam aquisicao, compra ou receita.',
         '$.nodes[7].description', 'Abra ou retome o ciclo deste produto e experimento pago. Concilie vendas liquidas, CAC, custo integral, entrega e margem; registre decisao e siga a atividade orientada. Mudanca comercial exige sucessor e nova homologacao; correcao tecnica preserva a iteracao.'
       ),
       UTC_TIMESTAMP(6),
       UTC_TIMESTAMP(6)
FROM business_process_definition source
LEFT JOIN business_process_definition existing
  ON existing.process_code=source.process_code AND existing.version_number=10
WHERE source.process_code='pde-sales-delivery-learning'
  AND source.version_number=9
  AND existing.id IS NULL;

INSERT INTO business_process_definition
  (process_code, name, purpose, owner_name, trigger_description, outcome_description,
   version_number, status, technical_reference, process_type, parent_process_code,
   execution_scope, diagram_json, created_at, published_at)
SELECT source.process_code,
       source.name,
       'Preparar uma oferta publica de Produto IA para aquisicao paga no Instagram, com valor inicial util, continuidade paga clara e margem sustentavel.',
       source.owner_name,
       source.trigger_description,
       source.outcome_description,
       2,
       'PUBLISHED',
       'docs/canonical/product-types-canon.v1.md / safira-commercial-preparation-v2',
       source.process_type,
       source.parent_process_code,
       source.execution_scope,
       JSON_SET(
         source.diagram_json,
         '$.schemaVersion', 'SAFIRA_COMMERCIAL_PREPARATION_V2',
         '$.commercialAcquisitionPolicyVersion', 'PAID_INSTAGRAM_ONLY_V1',
         '$.nodes[0].label', 'Safira e experimento pago no Instagram identificados',
         '$.nodes[1].description', 'Entregar experiencia publica do mesmo experimento, desejo reconhecido com fonte, primeira acao simples no celular, demonstracao util, continuidade paga clara, criativo final para Instagram Ads, identidade oficial, publico salvo, checkout, entrega, suporte, reembolso, UTM e eventos. Aceite: convite individual, lista propria, contato direto ou organico nao suprem a aquisicao; a validacao privada continua somente referencia do produto.',
         '$.nodes[2].description', 'Reutilizar plano financeiro LIVE da mesma versao e plano comercial, com CAC de Instagram Ads, IA, infraestrutura, entrega, suporte, taxas e reembolsos nos cenarios conservador, esperado e intensivo. Custo de midia desconhecido nao e zero e esta atividade nao autoriza gasto.',
         '$.nodes[3].description', 'Avaliar desejo, esforco, demonstracao, diferenca paga, seguranca, privacidade, anuncio do Instagram, compra, entrega e utilidade sem inventar cliente, uso, venda ou resultado.',
         '$.nodes[4].description', 'Conferir promessa, preco, subtipo, anuncio, identidade do Instagram, publico, experiencia, checkout, entrega, seguranca, custos, suporte e metricas atribuidas da mesma versao.',
         '$.nodes[5].description', 'Revalidar jornada paga no Instagram, economia, fingerprints e pareceres; retornar ao pai sem publicar campanha, autorizar gasto ou alegar venda.'
       ),
       UTC_TIMESTAMP(6),
       UTC_TIMESTAMP(6)
FROM business_process_definition source
LEFT JOIN business_process_definition existing
  ON existing.process_code=source.process_code AND existing.version_number=2
WHERE source.process_code='safira-commercial-preparation-v1'
  AND source.version_number=1
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
  UNION ALL SELECT 12 UNION ALL SELECT 13 UNION ALL SELECT 14
) node
LEFT JOIN business_process_activity_definition existing
  ON existing.process_definition_id=process.id
 AND existing.activity_id=JSON_UNQUOTE(JSON_EXTRACT(process.diagram_json, CONCAT('$.nodes[', node.number, '].id')))
WHERE ((process.process_code='pde-commercial-plan-offer' AND process.version_number=8)
    OR (process.process_code='pde-communication-sales-journey' AND process.version_number=9)
    OR (process.process_code='pde-commercial-homologation-activation' AND process.version_number=10)
    OR (process.process_code='pde-sales-delivery-learning' AND process.version_number=10)
    OR (process.process_code='safira-commercial-preparation-v1' AND process.version_number=2))
  AND JSON_UNQUOTE(JSON_EXTRACT(process.diagram_json, CONCAT('$.nodes[', node.number, '].type')))='TASK'
  AND existing.id IS NULL;

INSERT INTO business_process_chain_definition
  (chain_code, name, purpose, outcome_description, primary_metric,
   version_number, status, created_at, published_at)
SELECT source.chain_code,
       source.name,
       CONCAT(source.purpose, ' PAID_INSTAGRAM_ONLY_V1: toda divulgacao comercial usa midia paga no Instagram; convites individuais e canais nao atribuidos permanecem apenas historicos.'),
       source.outcome_description,
       source.primary_metric,
       22,
       'PUBLISHED',
       UTC_TIMESTAMP(6),
       UTC_TIMESTAMP(6)
FROM business_process_chain_definition source
LEFT JOIN business_process_chain_definition existing
  ON existing.chain_code=source.chain_code AND existing.version_number=22
WHERE source.chain_code='pde-value-creation-delivery'
  AND source.version_number=21
  AND existing.id IS NULL;

INSERT INTO business_process_chain_item
  (chain_definition_id, process_definition_id, sequence_number, value_contribution, created_at)
SELECT target.id,
       CASE WHEN replacement.id IS NOT NULL THEN replacement.id ELSE old_process.id END,
       item.sequence_number,
       item.value_contribution,
       UTC_TIMESTAMP(6)
FROM business_process_chain_definition source
JOIN business_process_chain_item item ON item.chain_definition_id=source.id
JOIN business_process_definition old_process ON old_process.id=item.process_definition_id
LEFT JOIN business_process_definition replacement
  ON replacement.process_code=old_process.process_code
 AND replacement.version_number=CASE old_process.process_code
       WHEN 'pde-commercial-plan-offer' THEN 8
       WHEN 'pde-communication-sales-journey' THEN 9
       WHEN 'pde-commercial-homologation-activation' THEN 10
       WHEN 'pde-sales-delivery-learning' THEN 10
       ELSE -1 END
JOIN business_process_chain_definition target
  ON target.chain_code=source.chain_code AND target.version_number=22
LEFT JOIN business_process_chain_item existing
  ON existing.chain_definition_id=target.id AND existing.sequence_number=item.sequence_number
WHERE source.chain_code='pde-value-creation-delivery'
  AND source.version_number=21
  AND existing.id IS NULL;

UPDATE business_process_definition SET status='RETIRED' WHERE process_code='pde-commercial-plan-offer' AND version_number=7;
UPDATE business_process_definition SET status='RETIRED' WHERE process_code='pde-communication-sales-journey' AND version_number=8;
UPDATE business_process_definition SET status='RETIRED' WHERE process_code='pde-commercial-homologation-activation' AND version_number=9;
UPDATE business_process_definition SET status='RETIRED' WHERE process_code='pde-sales-delivery-learning' AND version_number=9;
UPDATE business_process_definition SET status='RETIRED' WHERE process_code='safira-commercial-preparation-v1' AND version_number=1;
UPDATE business_process_definition SET status='PUBLISHED' WHERE process_code='pde-commercial-plan-offer' AND version_number=8;
UPDATE business_process_definition SET status='PUBLISHED' WHERE process_code='pde-communication-sales-journey' AND version_number=9;
UPDATE business_process_definition SET status='PUBLISHED' WHERE process_code='pde-commercial-homologation-activation' AND version_number=10;
UPDATE business_process_definition SET status='PUBLISHED' WHERE process_code='pde-sales-delivery-learning' AND version_number=10;
UPDATE business_process_definition SET status='PUBLISHED' WHERE process_code='safira-commercial-preparation-v1' AND version_number=2;
UPDATE business_process_chain_definition SET status='RETIRED' WHERE chain_code='pde-value-creation-delivery' AND version_number=21;
UPDATE business_process_chain_definition SET status='PUBLISHED' WHERE chain_code='pde-value-creation-delivery' AND version_number=22;
