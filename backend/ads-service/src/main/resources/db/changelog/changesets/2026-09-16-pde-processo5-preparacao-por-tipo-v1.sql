INSERT INTO business_process_definition
(process_code,name,purpose,owner_name,trigger_description,outcome_description,version_number,status,technical_reference,process_type,parent_process_code,execution_scope,diagram_json,created_at,published_at)
SELECT source.process_code,source.name,
 'Comprovar que a versão está pronta para vender, entregar o prometido e operar dentro dos limites financeiros antes de qualquer ativação.',
 source.owner_name,'Produto e jornada aprovados, com tipo cadastrado, ficha aplicável e versões exatas.',
 'Ativação autorizada para a versão comprovada ou bloqueio persistido com causa e retorno à atividade responsável.',
 7,'PUBLISHED','cadeia-produtos-pde-canon.v1 / PROCESSO_5_PREPARACAO_POR_TIPO_V1',source.process_type,source.parent_process_code,source.execution_scope,
 '{"schemaVersion":"PDE_COMMERCIAL_HOMOLOGATION_ACTIVATION_V7","nodes":[{"id":"start","type":"START","label":"Produto e jornada aprovados","owner":"Backend","description":"O backend fixa produto, experimento, versão, tipo e ficha antes de preparar a operação."},{"id":"commercialPreparation","type":"TASK","label":"Preparar operação comercial conforme o tipo","owner":"Backend","description":"Seleciona pelo tipo cadastrado e pela ficha o subprocesso comercial aplicável, sem inferir pelo nome ou formato do produto.","commercialPreparationRouterVersion":"COMMERCIAL_PREPARATION_BY_PRODUCT_TYPE_V1","subprocessRoutes":[{"productTypeCode":"PDE","productTypeInternalName":"Opala","subprocessCode":"opala-commercial-preparation-v1","subprocessVersion":1}]},{"id":"humanExperienceReview","type":"TASK","label":"Validar experiência e valor para o cliente","owner":"Psique","description":"Reutiliza o parecer de Psique produzido na preparação do mesmo produto, experimento, versão e configuração; mudança material exige renovação.","responsibilityDomain":"HUMAN_EXPERIENCE_REVIEW","reuseSubprocessActivityId":"humanExperienceReview","evidenceReuseVersion":"COMMERCIAL_REVIEW_REUSE_V1"},{"id":"commercialIntegrityReview","type":"TASK","label":"Validar integridade comercial","owner":"Têmis","description":"Reutiliza o parecer independente de Têmis da mesma preparação; divergência de anúncio, promessa, preço, checkout ou entrega bloqueia e retorna à origem.","responsibilityDomain":"COMMERCIAL_INTEGRITY_REVIEW","reuseSubprocessActivityId":"commercialIntegrityReview","evidenceReuseVersion":"COMMERCIAL_REVIEW_REUSE_V1"},{"id":"preflight","type":"TASK","label":"Executar homologação técnica do experimento","owner":"Backend","description":"Chama a homologação técnica para comprovar desktop, celular, compra simulada, acesso, entrega, falhas, eventos, segregação e limites, reutilizando evidências ainda vigentes.","subprocessCode":"experiment-homologation-activation"},{"id":"decision","type":"GATEWAY","label":"Preparação, experiência, integridade, técnica e economia estão válidas?","owner":"Backend","description":"Qualquer reprovação ou mudança material bloqueia a ativação e preserva a causa e a atividade responsável."},{"id":"authorization","type":"TASK","label":"Autorizar ativação, orçamento e janela","owner":"Operador humano","description":"Aprova explicitamente a versão, canais, teto financeiro, período e condições de parada; nenhum gate técnico concede gasto."},{"id":"end","type":"END","label":"Ativação autorizada","owner":"Backend","description":"O Processo 6 executa a operação; RUNNING depende da confirmação externa e dos gates de publicação."}],"flows":[{"id":"prepare-by-type","from":"start","to":"commercialPreparation"},{"id":"reuse-psique","from":"commercialPreparation","to":"humanExperienceReview"},{"id":"reuse-themis","from":"humanExperienceReview","to":"commercialIntegrityReview"},{"id":"technical-homologation","from":"commercialIntegrityReview","to":"preflight"},{"id":"commercial-gate","from":"preflight","to":"decision"},{"id":"approved","from":"decision","to":"authorization","label":"Todos os gates vigentes"},{"id":"authorized","from":"authorization","to":"end"},{"id":"rework-preparation","from":"decision","to":"commercialPreparation","label":"Corrigir causa na preparação","kind":"REWORK"},{"id":"rework-experience","from":"decision","to":"humanExperienceReview","label":"Renovar experiência afetada","kind":"REWORK"},{"id":"rework-integrity","from":"decision","to":"commercialIntegrityReview","label":"Renovar integridade afetada","kind":"REWORK"},{"id":"rework-technical","from":"decision","to":"preflight","label":"Renovar homologação afetada","kind":"REWORK"}]}',UTC_TIMESTAMP(),UTC_TIMESTAMP()
FROM business_process_definition source
WHERE source.process_code='pde-commercial-homologation-activation' AND source.version_number=6
AND NOT EXISTS (SELECT 1 FROM business_process_definition target WHERE target.process_code=source.process_code AND target.version_number=7);

INSERT INTO business_process_activity_definition
(process_definition_id,activity_id,name,objective,owner_name,execution_resource_code,subprocess_code,definition_json,created_at)
SELECT process.id,
 JSON_UNQUOTE(JSON_EXTRACT(process.diagram_json,CONCAT('$.nodes[',sequence.number,'].id'))),
 JSON_UNQUOTE(JSON_EXTRACT(process.diagram_json,CONCAT('$.nodes[',sequence.number,'].label'))),
 JSON_UNQUOTE(JSON_EXTRACT(process.diagram_json,CONCAT('$.nodes[',sequence.number,'].description'))),
 JSON_UNQUOTE(JSON_EXTRACT(process.diagram_json,CONCAT('$.nodes[',sequence.number,'].owner'))),
 JSON_UNQUOTE(JSON_EXTRACT(process.diagram_json,CONCAT('$.nodes[',sequence.number,'].executionResourceCode'))),
 JSON_UNQUOTE(JSON_EXTRACT(process.diagram_json,CONCAT('$.nodes[',sequence.number,'].subprocessCode'))),
 CAST(JSON_EXTRACT(process.diagram_json,CONCAT('$.nodes[',sequence.number,']')) AS CHAR),UTC_TIMESTAMP()
FROM business_process_definition process
JOIN (SELECT 1 AS number UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 6) sequence ON 1=1
LEFT JOIN business_process_activity_definition existing
 ON existing.process_definition_id=process.id
 AND existing.activity_id=JSON_UNQUOTE(JSON_EXTRACT(process.diagram_json,CONCAT('$.nodes[',sequence.number,'].id')))
WHERE process.process_code='pde-commercial-homologation-activation' AND process.version_number=7 AND existing.id IS NULL;

INSERT INTO business_process_definition
(process_code,name,purpose,owner_name,trigger_description,outcome_description,version_number,status,technical_reference,process_type,parent_process_code,execution_scope,diagram_json,created_at,published_at)
SELECT source.process_code,source.name,source.purpose,source.owner_name,
 'Ativação autorizada pelo Processo 5, com orçamento, canais, versão e critérios vigentes.',source.outcome_description,
 8,'PUBLISHED','cadeia-produtos-pde-canon.v1 / SALES_AFTER_COMMERCIAL_ACTIVATION_V1',source.process_type,source.parent_process_code,source.execution_scope,
 '{"schemaVersion":"PDE_SALES_DELIVERY_LEARNING_V8","nodes":[{"id":"start","type":"START","label":"Ativação autorizada","owner":"Backend","description":"A preparação comercial pertence ao Processo 5; este processo começa somente com operação autorizada ou referência histórica comprovada."},{"id":"entry","type":"GATEWAY","label":"Operação atual ou referência histórica?","owner":"Backend","description":"Adoção histórica exige evento ADOPT_BASELINE e evidência de publicação, sem aprovação retroativa."},{"id":"parallel","type":"GATEWAY","label":"Operação e entrega durante as vendas","owner":"Backend","description":"Ramos paralelos explícitos; a entrega de uma compra não aguarda o encerramento do experimento."},{"id":"sales","type":"GATEWAY","label":"Existem vendas no recorte?","owner":"Backend","description":"Somente fonte válida pode comprovar zero vendas. Nenhuma venda implica entrega não aplicável nesta passagem."},{"id":"optimization","type":"TASK","label":"Operar e otimizar o experimento","owner":"Backend","description":"Delega aquisição, mensuração e teste de variável ao subprocesso canônico.","subprocessCode":"operacao-otimizacao-experimento"},{"id":"delivery","type":"TASK","label":"Entregar cada venda e acompanhar satisfação","owner":"Backend","description":"Delega conciliação, entrega, suporte, satisfação e reembolso ao subprocesso canônico.","subprocessCode":"venda-entrega-satisfacao-cliente"},{"id":"consolidate","type":"TASK","label":"Consolidar resultado comercial","owner":"Backend","description":"O backend concilia automaticamente fontes do experimento. Fonte inválida bloqueia; não é preenchimento manual nem ausência igual a zero."},{"id":"learningCycle","type":"TASK","label":"Conduzir o ciclo de aprendizado e vendas","owner":"Operador responsável pelo produto","description":"Abra ou retome o ciclo deste produto e experimento. Concilie resultados, registre a decisão e siga a atividade orientada.","subprocessCode":"value-chain-learning-sales-cycle"},{"id":"decision","type":"GATEWAY","label":"Qual decisão comercial?","owner":"Backend e operador humano","description":"A decisão persistida determina o retorno pela causa. Continuar coleta respeita janela e orçamento; escala exige nova autorização."},{"id":"join","type":"GATEWAY","label":"Resultados e situação das entregas disponíveis","owner":"Backend","description":"Consolidar situação comercial e entregas, preservando bloqueios e ausência comprovada de vendas."},{"id":"returnTarget","type":"GATEWAY","label":"Retorno ao processo da causa: 2, 3 ou 4","owner":"Backend","description":"A decisão identifica processo e atividade. Mudança comercial usa sucessor; correção técnica preserva o experimento."},{"id":"returnAuthorization","type":"GATEWAY","label":"Processo 5: homologação e autorização","owner":"Backend e operador","description":"Renovar os gates afetados e autorização explícita antes da publicação ou escala; retorno registrado não autoriza gasto por si só."},{"id":"end","type":"END","label":"Ciclo decidido por vendas entregues","owner":"Backend","description":"A decisão e suas evidências ficam auditáveis."}],"flows":[{"id":"identify","from":"start","to":"entry","label":"Fixar produto e experimento"},{"id":"live-entry","from":"entry","to":"parallel","label":"Publicação atual autorizada"},{"id":"historical-entry","from":"entry","to":"consolidate","label":"ADOPT_BASELINE: publicação histórica comprovada; preservar limitações"},{"id":"operate","from":"parallel","to":"optimization","label":"Operação com limites vigentes"},{"id":"delivery-condition","from":"parallel","to":"sales","label":"Entregar cada compra durante a operação"},{"id":"has-sales","from":"sales","to":"delivery","label":"Vendas existentes: entrega obrigatória"},{"id":"no-sales","from":"sales","to":"join","label":"Zero vendas comprovado: entrega não aplicável nesta passagem"},{"id":"operation-snapshot","from":"optimization","to":"join","label":"Rodada de resultados disponível"},{"id":"delivery-snapshot","from":"delivery","to":"join","label":"Situação das entregas registrada"},{"id":"consolidation","from":"join","to":"consolidate","label":"Fontes e limites do mesmo experimento"},{"id":"measurement-ready","from":"consolidate","to":"learningCycle","label":"MEASURE: leitura válida; obrigações de entrega verificadas"},{"id":"measurement-blocked","from":"consolidate","to":"consolidate","label":"MEASUREMENT_BLOCKED: corrigir fonte e repetir conciliação","kind":"REWORK"},{"id":"delivery-blocked","from":"consolidate","to":"delivery","label":"Venda sem entrega comprovada: tratar e reconsolidar","kind":"REWORK"},{"id":"decision-recorded","from":"learningCycle","to":"decision","label":"Decisão, causa, evidência, responsável e destino registrados"},{"id":"continue-collection","from":"decision","to":"optimization","label":"CONTINUE: mesmo experimento, dentro do orçamento e janela","kind":"REWORK"},{"id":"fix-measurement","from":"decision","to":"consolidate","label":"FIX_MEASUREMENT: corrigir e reconciliar a fonte; sem reiniciar campanha","kind":"REWORK"},{"id":"directed-adjustment","from":"decision","to":"returnTarget","label":"ADJUST / REWORK: executar retorno registrado pela causa","kind":"REWORK"},{"id":"delegated-progress","from":"returnTarget","to":"learningCycle","label":"Etapas delegadas concluídas com evidência no ciclo","kind":"REWORK"},{"id":"request-scale","from":"decision","to":"returnAuthorization","label":"SCALE: contribuição e valor comprovados; solicitar autorização","kind":"REWORK"},{"id":"revalidate","from":"returnTarget","to":"returnAuthorization","label":"Melhoria pronta: revalidar a mesma versão"},{"id":"authorized-scale","from":"returnAuthorization","to":"optimization","label":"AUTHORIZE_SCALE: expansão explicitamente autorizada","kind":"REWORK"},{"id":"publication-measurement","from":"returnAuthorization","to":"consolidate","label":"Publicação comprovada após gates próprios"},{"id":"close-cycle","from":"decision","to":"end","label":"STOP / INCONCLUSIVE: preservar resultados e motivos"}],"learningCycleReturns":[{"label":"Estratégia e economia","condition":"A evidência exige rever oferta, preço ou viabilidade. Defina a hipótese do sucessor.","processCode":"pde-commercial-plan-offer"},{"label":"Utilidade do produto","condition":"A cliente não consegue aplicar o resultado ou perceber valor. Corrija a entrega e renove os pareceres.","processCode":"pde-construction-approval"},{"label":"Comunicação e vídeos","condition":"O benefício ou a entrada não são compreendidos. Ajuste mensagem, demonstração e criativo.","processCode":"pde-communication-sales-journey"},{"label":"Homologação e autorização","condition":"Depois dos ajustes, valide a mesma versão e obtenha autorização explícita antes de publicar ou expandir.","processCode":"pde-commercial-homologation-activation"},{"label":"Coleta e medição","condition":"Continuar somente com dados válidos dentro dos limites; corrigir dados inválidos antes de decidir sobre desempenho.","processCode":"pde-sales-delivery-learning"}],"salesFlowVersion":2}',UTC_TIMESTAMP(),UTC_TIMESTAMP()
FROM business_process_definition source
WHERE source.process_code='pde-sales-delivery-learning' AND source.version_number=7
AND NOT EXISTS (SELECT 1 FROM business_process_definition target WHERE target.process_code=source.process_code AND target.version_number=8);

INSERT INTO business_process_activity_definition
(process_definition_id,activity_id,name,objective,owner_name,execution_resource_code,subprocess_code,definition_json,created_at)
SELECT process.id,
 JSON_UNQUOTE(JSON_EXTRACT(process.diagram_json,CONCAT('$.nodes[',sequence.number,'].id'))),
 JSON_UNQUOTE(JSON_EXTRACT(process.diagram_json,CONCAT('$.nodes[',sequence.number,'].label'))),
 JSON_UNQUOTE(JSON_EXTRACT(process.diagram_json,CONCAT('$.nodes[',sequence.number,'].description'))),
 JSON_UNQUOTE(JSON_EXTRACT(process.diagram_json,CONCAT('$.nodes[',sequence.number,'].owner'))),
 JSON_UNQUOTE(JSON_EXTRACT(process.diagram_json,CONCAT('$.nodes[',sequence.number,'].executionResourceCode'))),
 JSON_UNQUOTE(JSON_EXTRACT(process.diagram_json,CONCAT('$.nodes[',sequence.number,'].subprocessCode'))),
 CAST(JSON_EXTRACT(process.diagram_json,CONCAT('$.nodes[',sequence.number,']')) AS CHAR),UTC_TIMESTAMP()
FROM business_process_definition process
JOIN (SELECT 4 AS number UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7) sequence ON 1=1
LEFT JOIN business_process_activity_definition existing
 ON existing.process_definition_id=process.id
 AND existing.activity_id=JSON_UNQUOTE(JSON_EXTRACT(process.diagram_json,CONCAT('$.nodes[',sequence.number,'].id')))
WHERE process.process_code='pde-sales-delivery-learning' AND process.version_number=8 AND existing.id IS NULL;

INSERT INTO business_process_chain_definition
(chain_code,name,purpose,outcome_description,primary_metric,version_number,status,created_at,published_at)
SELECT source.chain_code,source.name,source.purpose,source.outcome_description,source.primary_metric,16,'PUBLISHED',UTC_TIMESTAMP(),UTC_TIMESTAMP()
FROM business_process_chain_definition source
WHERE source.chain_code='pde-value-creation-delivery' AND source.version_number=15
AND NOT EXISTS (SELECT 1 FROM business_process_chain_definition target WHERE target.chain_code=source.chain_code AND target.version_number=16);

INSERT INTO business_process_chain_item
(chain_definition_id,process_definition_id,sequence_number,value_contribution,created_at)
SELECT target.id,
 CASE
  WHEN previous.process_code='pde-commercial-homologation-activation' THEN process5.id
  WHEN previous.process_code='pde-sales-delivery-learning' THEN process6.id
  ELSE item.process_definition_id
 END,
 item.sequence_number,
 CASE
  WHEN previous.process_code='pde-commercial-homologation-activation' THEN 'O backend prepara conforme o tipo; Psique e Têmis validam sem custo duplicado; preflight e decisão humana protegem valor e margem.'
  WHEN previous.process_code='pde-sales-delivery-learning' THEN 'Somente a ativação autorizada inicia operação, venda, entrega, medição e decisão de escala.'
  ELSE item.value_contribution
 END,
 UTC_TIMESTAMP()
FROM business_process_chain_definition source
JOIN business_process_chain_item item ON item.chain_definition_id=source.id
JOIN business_process_definition previous ON previous.id=item.process_definition_id
JOIN business_process_definition process5 ON process5.process_code='pde-commercial-homologation-activation' AND process5.version_number=7
JOIN business_process_definition process6 ON process6.process_code='pde-sales-delivery-learning' AND process6.version_number=8
JOIN business_process_chain_definition target ON target.chain_code=source.chain_code AND target.version_number=16
LEFT JOIN business_process_chain_item existing ON existing.chain_definition_id=target.id AND existing.sequence_number=item.sequence_number
WHERE source.chain_code='pde-value-creation-delivery' AND source.version_number=15 AND existing.id IS NULL;

UPDATE business_process_definition SET status='PUBLISHED',published_at=COALESCE(published_at,UTC_TIMESTAMP())
WHERE (process_code='pde-commercial-homologation-activation' AND version_number=7)
   OR (process_code='pde-sales-delivery-learning' AND version_number=8);
UPDATE business_process_chain_definition SET status='PUBLISHED',published_at=COALESCE(published_at,UTC_TIMESTAMP())
WHERE chain_code='pde-value-creation-delivery' AND version_number=16;
