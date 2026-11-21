-- Nova definição: não altera execuções, custos, tarefas ou vínculos históricos.
INSERT INTO business_process_definition
(process_code,name,purpose,owner_name,trigger_description,outcome_description,version_number,status,technical_reference,process_type,parent_process_code,execution_scope,diagram_json,created_at,published_at)
SELECT p.process_code,p.name,CONCAT(p.purpose,' Continuidade da proposta por tipo de produto.'),p.owner_name,p.trigger_description,
 'Experiencia completa comprovada contra a proposta fixada; prototipo parcial nao libera comunicacao do produto completo.',
 12,'PUBLISHED','docs/canonical/cadeia-produtos-pde-canon.v1.md',p.process_type,p.parent_process_code,p.execution_scope,
 JSON_SET(p.diagram_json,'$.productValueDeliveryContract','TURMALINA_VALUE_DELIVERY_V1'),UTC_TIMESTAMP(6),UTC_TIMESTAMP(6)
FROM business_process_definition p WHERE p.process_code='pde-construction-approval' AND p.version_number=11;
INSERT INTO business_process_activity_definition
(process_definition_id,activity_id,name,objective,owner_name,execution_resource_code,subprocess_code,definition_json,created_at)
SELECT n.id,a.activity_id,a.name,CONCAT(a.objective,CASE a.activity_id
 WHEN 'journey' THEN ' Turmalina: preservar a Big Idea e os nove campos da base cadastrada em historias, estados de tela e criterios dado/quando/entao. Separar produto completo de recorte; indicar construir, reutilizar ou adiar com evidencias e impacto. Outros tipos conservam seus contratos.'
 WHEN 'deliverables' THEN ' Turmalina: converter as historias fixadas em incrementos pequenos e verificaveis de experiencia visual, consultoria contextual e memoria. Identificar codigo existente compativel, lacunas, integracoes SDK e testes por criterio. READY conclui a especificacao. Outros tipos conservam seus contratos.'
 WHEN 'access' THEN ' Turmalina: preservar as historias e integrar acesso, sessao, consentimento, memoria e retomada pelos SDKs cadastrados. Explicar estados vazio/erro e isolamento. Referenciar a especificacao e a versao da candidata sem afirmar implementacao inexistente. Outros tipos conservam seus contratos.'
 WHEN 'audiovisual' THEN ' Turmalina: representar somente a experiencia e as capacidades comprovadas da mesma especificacao. Manter materiais validos; nao prometer adaptacao ou personalizacao ainda nao implementada. Outros tipos conservam seus contratos.'
 WHEN 'prototypeCorrection' THEN ' Turmalina: corrigir as historias e criterios reprovados na sua origem, preservando os aceitos. Entregar codigo e provas reais pelo fluxo versionado; atualizar a vinculacao da evidencia quando mudar implementacao. Nao reduzir a promessa para passar. Outros tipos conservam seus contratos.'
 WHEN 'technicalHomologation' THEN ' Turmalina: executar os criterios das historias na candidata exata e registrar resultados, falhas, versao e hashes. Cobrir uso, retomada, erros, isolamento e dispositivos. Vincular ao produto/ciclo e a especificacao; recorte nao comprova produto completo. Outros tipos conservam seus contratos.'
 WHEN 'psiqueAdherent' THEN ' Turmalina: percorrer a experiencia real e confrontar cada historia com utilidade, telas, hierarquia visual, esforco e resultado prometido. Examinar capturas e evidencias da candidata exata; registrar referencia da especificacao e prova. Outros tipos conservam seus contratos.'
 WHEN 'psiqueRecovery' THEN ' Turmalina: verificar retomada, adaptacao entre contextos, memoria consentida e recuperacao de erros nas historias reais. Distinguir recuperar dados de entregar a personalizacao prometida; apontar criterio e causa da lacuna. Outros tipos conservam seus contratos.'
 WHEN 'psiqueSafety' THEN ' Turmalina: testar consentimento, isolamento entre clientes, limites do consultor e encerramento/controle de dados. Confrontar limites da Big Idea e base do tipo com estados reais da tela e a prova da mesma versao. Outros tipos conservam seus contratos.'
 WHEN 'commercialIntegrityReview' THEN ' Turmalina: confrontar promessa, mecanismo e limites com todas as historias e provas reais. Rejeitar consultoria ou personalizacao declarada sem implementacao. Preservar parecer independente e identificar exatamente especificacao e prova revisadas. Outros tipos conservam seus contratos.'
 WHEN 'agentValidationGate' THEN ' Turmalina: exigir cobertura completa, execucao dos criterios e revisoes independentes da mesma proposta e candidata. Explicar a primeira lacuna e o responsavel por corrigi-la; nao promover recorte ou contratar revisoes sem implementacao. Outros tipos conservam seus contratos.'
 ELSE '' END),a.owner_name,a.execution_resource_code,a.subprocess_code,
 JSON_SET(a.definition_json,'$.description',CONCAT(a.objective,CASE a.activity_id
 WHEN 'journey' THEN ' Turmalina: preservar a Big Idea e os nove campos da base cadastrada em historias, estados de tela e criterios dado/quando/entao. Separar produto completo de recorte; indicar construir, reutilizar ou adiar com evidencias e impacto. Outros tipos conservam seus contratos.'
 WHEN 'deliverables' THEN ' Turmalina: converter as historias fixadas em incrementos pequenos e verificaveis de experiencia visual, consultoria contextual e memoria. Identificar codigo existente compativel, lacunas, integracoes SDK e testes por criterio. READY conclui a especificacao. Outros tipos conservam seus contratos.'
 WHEN 'access' THEN ' Turmalina: preservar as historias e integrar acesso, sessao, consentimento, memoria e retomada pelos SDKs cadastrados. Explicar estados vazio/erro e isolamento. Referenciar a especificacao e a versao da candidata sem afirmar implementacao inexistente. Outros tipos conservam seus contratos.'
 WHEN 'audiovisual' THEN ' Turmalina: representar somente a experiencia e as capacidades comprovadas da mesma especificacao. Manter materiais validos; nao prometer adaptacao ou personalizacao ainda nao implementada. Outros tipos conservam seus contratos.'
 WHEN 'prototypeCorrection' THEN ' Turmalina: corrigir as historias e criterios reprovados na sua origem, preservando os aceitos. Entregar codigo e provas reais pelo fluxo versionado; atualizar a vinculacao da evidencia quando mudar implementacao. Nao reduzir a promessa para passar. Outros tipos conservam seus contratos.'
 WHEN 'technicalHomologation' THEN ' Turmalina: executar os criterios das historias na candidata exata e registrar resultados, falhas, versao e hashes. Cobrir uso, retomada, erros, isolamento e dispositivos. Vincular ao produto/ciclo e a especificacao; recorte nao comprova produto completo. Outros tipos conservam seus contratos.'
 WHEN 'psiqueAdherent' THEN ' Turmalina: percorrer a experiencia real e confrontar cada historia com utilidade, telas, hierarquia visual, esforco e resultado prometido. Examinar capturas e evidencias da candidata exata; registrar referencia da especificacao e prova. Outros tipos conservam seus contratos.'
 WHEN 'psiqueRecovery' THEN ' Turmalina: verificar retomada, adaptacao entre contextos, memoria consentida e recuperacao de erros nas historias reais. Distinguir recuperar dados de entregar a personalizacao prometida; apontar criterio e causa da lacuna. Outros tipos conservam seus contratos.'
 WHEN 'psiqueSafety' THEN ' Turmalina: testar consentimento, isolamento entre clientes, limites do consultor e encerramento/controle de dados. Confrontar limites da Big Idea e base do tipo com estados reais da tela e a prova da mesma versao. Outros tipos conservam seus contratos.'
 WHEN 'commercialIntegrityReview' THEN ' Turmalina: confrontar promessa, mecanismo e limites com todas as historias e provas reais. Rejeitar consultoria ou personalizacao declarada sem implementacao. Preservar parecer independente e identificar exatamente especificacao e prova revisadas. Outros tipos conservam seus contratos.'
 WHEN 'agentValidationGate' THEN ' Turmalina: exigir cobertura completa, execucao dos criterios e revisoes independentes da mesma proposta e candidata. Explicar a primeira lacuna e o responsavel por corrigi-la; nao promover recorte ou contratar revisoes sem implementacao. Outros tipos conservam seus contratos.'
 ELSE '' END)),UTC_TIMESTAMP(6)
FROM business_process_activity_definition a
JOIN business_process_definition p ON p.id=a.process_definition_id AND p.process_code='pde-construction-approval' AND p.version_number=11
JOIN business_process_definition n ON n.process_code=p.process_code AND n.version_number=12;
-- Atualização por join: diagrama e cards expõem o mesmo contrato, sem erro MySQL 1093.
UPDATE business_process_definition p JOIN business_process_activity_definition a
 ON a.process_definition_id=p.id AND a.activity_id='journey'
SET p.diagram_json=JSON_SET(p.diagram_json,
 REPLACE(JSON_UNQUOTE(JSON_SEARCH(p.diagram_json,'one',a.activity_id,NULL,'$.nodes[*].id')),'.id','.description'),a.objective)
WHERE p.process_code='pde-construction-approval' AND p.version_number=12;
UPDATE business_process_definition p JOIN business_process_activity_definition a
 ON a.process_definition_id=p.id AND a.activity_id='deliverables'
SET p.diagram_json=JSON_SET(p.diagram_json,
 REPLACE(JSON_UNQUOTE(JSON_SEARCH(p.diagram_json,'one',a.activity_id,NULL,'$.nodes[*].id')),'.id','.description'),a.objective)
WHERE p.process_code='pde-construction-approval' AND p.version_number=12;
UPDATE business_process_definition p JOIN business_process_activity_definition a
 ON a.process_definition_id=p.id AND a.activity_id='access'
SET p.diagram_json=JSON_SET(p.diagram_json,
 REPLACE(JSON_UNQUOTE(JSON_SEARCH(p.diagram_json,'one',a.activity_id,NULL,'$.nodes[*].id')),'.id','.description'),a.objective)
WHERE p.process_code='pde-construction-approval' AND p.version_number=12;
UPDATE business_process_definition p JOIN business_process_activity_definition a
 ON a.process_definition_id=p.id AND a.activity_id='audiovisual'
SET p.diagram_json=JSON_SET(p.diagram_json,
 REPLACE(JSON_UNQUOTE(JSON_SEARCH(p.diagram_json,'one',a.activity_id,NULL,'$.nodes[*].id')),'.id','.description'),a.objective)
WHERE p.process_code='pde-construction-approval' AND p.version_number=12;
UPDATE business_process_definition p JOIN business_process_activity_definition a
 ON a.process_definition_id=p.id AND a.activity_id='technicalHomologation'
SET p.diagram_json=JSON_SET(p.diagram_json,
 REPLACE(JSON_UNQUOTE(JSON_SEARCH(p.diagram_json,'one',a.activity_id,NULL,'$.nodes[*].id')),'.id','.description'),a.objective)
WHERE p.process_code='pde-construction-approval' AND p.version_number=12;
UPDATE business_process_definition p JOIN business_process_activity_definition a
 ON a.process_definition_id=p.id AND a.activity_id='psiqueAdherent'
SET p.diagram_json=JSON_SET(p.diagram_json,
 REPLACE(JSON_UNQUOTE(JSON_SEARCH(p.diagram_json,'one',a.activity_id,NULL,'$.nodes[*].id')),'.id','.description'),a.objective)
WHERE p.process_code='pde-construction-approval' AND p.version_number=12;
UPDATE business_process_definition p JOIN business_process_activity_definition a
 ON a.process_definition_id=p.id AND a.activity_id='psiqueRecovery'
SET p.diagram_json=JSON_SET(p.diagram_json,
 REPLACE(JSON_UNQUOTE(JSON_SEARCH(p.diagram_json,'one',a.activity_id,NULL,'$.nodes[*].id')),'.id','.description'),a.objective)
WHERE p.process_code='pde-construction-approval' AND p.version_number=12;
UPDATE business_process_definition p JOIN business_process_activity_definition a
 ON a.process_definition_id=p.id AND a.activity_id='psiqueSafety'
SET p.diagram_json=JSON_SET(p.diagram_json,
 REPLACE(JSON_UNQUOTE(JSON_SEARCH(p.diagram_json,'one',a.activity_id,NULL,'$.nodes[*].id')),'.id','.description'),a.objective)
WHERE p.process_code='pde-construction-approval' AND p.version_number=12;
UPDATE business_process_definition p JOIN business_process_activity_definition a
 ON a.process_definition_id=p.id AND a.activity_id='commercialIntegrityReview'
SET p.diagram_json=JSON_SET(p.diagram_json,
 REPLACE(JSON_UNQUOTE(JSON_SEARCH(p.diagram_json,'one',a.activity_id,NULL,'$.nodes[*].id')),'.id','.description'),a.objective)
WHERE p.process_code='pde-construction-approval' AND p.version_number=12;
UPDATE business_process_definition p JOIN business_process_activity_definition a
 ON a.process_definition_id=p.id AND a.activity_id='prototypeCorrection'
SET p.diagram_json=JSON_SET(p.diagram_json,
 REPLACE(JSON_UNQUOTE(JSON_SEARCH(p.diagram_json,'one',a.activity_id,NULL,'$.nodes[*].id')),'.id','.description'),a.objective)
WHERE p.process_code='pde-construction-approval' AND p.version_number=12;
UPDATE business_process_definition p JOIN business_process_activity_definition a
 ON a.process_definition_id=p.id AND a.activity_id='agentValidationGate'
SET p.diagram_json=JSON_SET(p.diagram_json,
 REPLACE(JSON_UNQUOTE(JSON_SEARCH(p.diagram_json,'one',a.activity_id,NULL,'$.nodes[*].id')),'.id','.description'),a.objective)
WHERE p.process_code='pde-construction-approval' AND p.version_number=12;

INSERT INTO business_process_chain_definition
(chain_code,name,purpose,outcome_description,primary_metric,version_number,status,created_at,published_at)
SELECT chain_code,name,CONCAT(purpose,' TURMALINA_VALUE_DELIVERY_V1: proposta, historias e provas preservadas por tipo.'),outcome_description,primary_metric,28,'PUBLISHED',UTC_TIMESTAMP(6),UTC_TIMESTAMP(6)
FROM business_process_chain_definition WHERE chain_code='pde-value-creation-delivery' AND version_number=27;
INSERT INTO business_process_chain_item (chain_definition_id,process_definition_id,sequence_number,value_contribution,created_at)
SELECT n.id,IF(p.process_code='pde-construction-approval',np.id,i.process_definition_id),i.sequence_number,
 IF(p.process_code='pde-construction-approval','Preservar proposta e base do tipo em historias, implementacao e provas; nao promover recorte como produto completo.',i.value_contribution),UTC_TIMESTAMP(6)
FROM business_process_chain_item i
JOIN business_process_chain_definition c ON c.id=i.chain_definition_id AND c.chain_code='pde-value-creation-delivery' AND c.version_number=27
JOIN business_process_chain_definition n ON n.chain_code=c.chain_code AND n.version_number=28
JOIN business_process_definition p ON p.id=i.process_definition_id
JOIN business_process_definition np ON np.process_code='pde-construction-approval' AND np.version_number=12;
UPDATE business_process_definition SET status='RETIRED' WHERE process_code='pde-construction-approval' AND version_number=11;
UPDATE business_process_chain_definition SET status='RETIRED' WHERE chain_code='pde-value-creation-delivery' AND version_number=27;
