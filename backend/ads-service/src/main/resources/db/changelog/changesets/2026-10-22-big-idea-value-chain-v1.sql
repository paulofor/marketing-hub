-- Nova definicao; tarefas e cadeias anteriores conservam seus vinculos.
INSERT INTO business_process_definition
(process_code,name,purpose,owner_name,trigger_description,outcome_description,version_number,status,technical_reference,process_type,parent_process_code,execution_scope,diagram_json,created_at,published_at)
SELECT p.process_code,p.name,CONCAT(p.purpose,' Big Idea versionada orienta toda a entrega.'),p.owner_name,p.trigger_description,p.outcome_description,13,'PUBLISHED','docs/canonical/product-big-idea-canon.v1.md',p.process_type,p.parent_process_code,p.execution_scope,
 JSON_SET(p.diagram_json,'$.bigIdeaContractVersion','PRODUCT_BIG_IDEA_V1',
 REPLACE(JSON_UNQUOTE(JSON_SEARCH(p.diagram_json,'one','marketStrategy',NULL,'$.nodes[*].id')),'.id','.description'),
 CONCAT(JSON_UNQUOTE(JSON_EXTRACT(p.diagram_json,REPLACE(JSON_UNQUOTE(JSON_SEARCH(p.diagram_json,'one','marketStrategy',NULL,'$.nodes[*].id')),'.id','.description'))),' PRODUCT_BIG_IDEA_V1: criar ou preservar a Big Idea do produto: ideia central, publico, desejo, promessa, mecanismo, provas e limites. Produto, oferta e comunicacao devem seguir essa referencia; evolucao cria revisao e novo ciclo comercial quando aplicavel. Nao substituir provas nem autorizar gasto.')),
 UTC_TIMESTAMP(6),UTC_TIMESTAMP(6)
FROM business_process_definition p WHERE p.process_code='pde-commercial-plan-offer' AND p.version_number=12;
INSERT INTO business_process_activity_definition
(process_definition_id,activity_id,name,objective,owner_name,execution_resource_code,subprocess_code,definition_json,created_at)
SELECT n.id,a.activity_id,a.name,IF(a.activity_id='marketStrategy',CONCAT(a.objective,' PRODUCT_BIG_IDEA_V1: criar ou preservar a Big Idea do produto: ideia central, publico, desejo, promessa, mecanismo, provas e limites. Produto, oferta e comunicacao devem seguir essa referencia; evolucao cria revisao e novo ciclo comercial quando aplicavel. Nao substituir provas nem autorizar gasto.'),a.objective),a.owner_name,a.execution_resource_code,a.subprocess_code,
 IF(a.activity_id='marketStrategy',JSON_SET(a.definition_json,'$.description',CONCAT(a.objective,' PRODUCT_BIG_IDEA_V1: criar ou preservar a Big Idea do produto: ideia central, publico, desejo, promessa, mecanismo, provas e limites. Produto, oferta e comunicacao devem seguir essa referencia; evolucao cria revisao e novo ciclo comercial quando aplicavel. Nao substituir provas nem autorizar gasto.'),'$.bigIdeaContractVersion','PRODUCT_BIG_IDEA_V1'),a.definition_json),UTC_TIMESTAMP(6)
FROM business_process_activity_definition a
JOIN business_process_definition p ON p.id=a.process_definition_id AND p.process_code='pde-commercial-plan-offer' AND p.version_number=12
JOIN business_process_definition n ON n.process_code=p.process_code AND n.version_number=13;
UPDATE business_process_definition SET status='RETIRED' WHERE process_code='pde-commercial-plan-offer' AND version_number=12;
INSERT INTO business_process_chain_definition
(chain_code,name,purpose,outcome_description,primary_metric,version_number,status,created_at,published_at)
SELECT chain_code,name,CONCAT(purpose,' Big Idea versionada do produto orienta todos os processos.'),outcome_description,primary_metric,27,'PUBLISHED',UTC_TIMESTAMP(6),UTC_TIMESTAMP(6)
FROM business_process_chain_definition WHERE chain_code='pde-value-creation-delivery' AND version_number=26;
INSERT INTO business_process_chain_item (chain_definition_id,process_definition_id,sequence_number,value_contribution,created_at)
SELECT n.id,IF(p.process_code='pde-commercial-plan-offer',np.id,i.process_definition_id),i.sequence_number,
 IF(p.process_code='pde-commercial-plan-offer','Big Idea e estrategia versionadas orientam oferta, produto e comunicacao; limites e evidencias preservados.',i.value_contribution),UTC_TIMESTAMP(6)
FROM business_process_chain_item i
JOIN business_process_chain_definition c ON c.id=i.chain_definition_id AND c.chain_code='pde-value-creation-delivery' AND c.version_number=26
JOIN business_process_chain_definition n ON n.chain_code=c.chain_code AND n.version_number=27
JOIN business_process_definition p ON p.id=i.process_definition_id
JOIN business_process_definition np ON np.process_code='pde-commercial-plan-offer' AND np.version_number=13;
UPDATE business_process_chain_definition SET status='RETIRED' WHERE chain_code='pde-value-creation-delivery' AND version_number=26;
