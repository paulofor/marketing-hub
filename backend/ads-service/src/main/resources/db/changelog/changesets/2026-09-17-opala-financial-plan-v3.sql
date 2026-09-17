SET @opala_economics_prompt_v3 = '# Plutus · preparação comercial Opala v3

Revise a economia do mesmo produto, ciclo, experimento e versão em
processContextJson.opalaCommercial. O backend só abre esta tarefa quando financialPlan
está READY, vigente e PROJECTED_VIABLE. Use exclusivamente essa revisão imutável, suas
fontes, assumptions e deterministicEvaluation; não substitua custos ausentes, não use
dados do predecessor e não reconstrua os cálculos.

Compare as três alternativas persistidas — conservadora, base e otimista — por benefício,
risco, esforço e aderência comercial. O cenário BASE é a recomendação operacional do parecer:
economics.offerPriceBrl copia assumptions.priceBrl; economics.maxCacBrl copia
assumptions.maximumCacBrl; expectedRefundPercent copia costs.refundPercent;
contributionPerSaleBrl copia BASE.contributionAfterCacBrl; variableCostPerSaleBrl é o preço
menos essa contribuição. Preserve o teto do ciclo. Use deadline como a menor data UTC entre
assumptions.validUntil e windowEnd, exclusivamente no formato YYYY-MM-DD.

APPROVE exige fontes completas, contribuição positiva e todos os cenários viáveis. Se uma
fonte material estiver apenas estimada, descreva-a como projeção e mantenha sua evidência;
receita realizada ausente não é zero. ADJUST ou REJECT deve apontar causa, impacto e atividade
responsável. Não autorize publicação, campanha, cobrança ou gasto e não declare vendas.

## Contexto persistido
{{TASK_CONTEXT}}
';

INSERT INTO catalogo_vivo_prompt_version_v1(
  binding_id,version_number,text_content,sha256,status,created_by,created_at,
  reviewed_by,reviewed_at,review_note)
SELECT b.id,3,@opala_economics_prompt_v3,SHA2(@opala_economics_prompt_v3,256),'REVIEWED',
  'Correção sistêmica da tarefa 436',UTC_TIMESTAMP(6),'Revisão financeira Opala',
  UTC_TIMESTAMP(6),'Consome a revisão financeira imutável antes da inferência e impede números reconstruídos.'
FROM catalogo_vivo_binding_v1 b
JOIN business_process_activity_definition a ON a.id=b.activity_definition_id
JOIN business_process_definition p ON p.id=a.process_definition_id
WHERE p.process_code='opala-commercial-preparation-v1' AND p.version_number=1
  AND a.activity_id='economics';

UPDATE catalogo_vivo_binding_v1 b
JOIN business_process_activity_definition a ON a.id=b.activity_definition_id
JOIN business_process_definition p ON p.id=a.process_definition_id
JOIN catalogo_vivo_prompt_version_v1 v ON v.binding_id=b.id AND v.version_number=3
SET b.active_version_id=v.id
WHERE p.process_code='opala-commercial-preparation-v1' AND p.version_number=1
  AND a.activity_id='economics';

INSERT INTO catalogo_vivo_audit_v1(binding_id,version_id,action,operator_name,note,created_at)
SELECT b.id,b.active_version_id,'BUGFIX_ACTIVATED','Correção sistêmica da tarefa 436',
  'Ativa o prompt Opala v3; plano, versão, fontes e janela são validados antes da chamada paga.',UTC_TIMESTAMP(6)
FROM catalogo_vivo_binding_v1 b
JOIN business_process_activity_definition a ON a.id=b.activity_definition_id
JOIN business_process_definition p ON p.id=a.process_definition_id
WHERE p.process_code='opala-commercial-preparation-v1' AND p.version_number=1
  AND a.activity_id='economics';
