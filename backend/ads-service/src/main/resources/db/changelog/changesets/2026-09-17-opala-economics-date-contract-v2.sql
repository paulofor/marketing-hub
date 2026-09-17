SET @opala_economics_prompt_v2 = '# Plutus · preparação comercial Opala v2

Reavalie a economia do mesmo produto, ciclo, experimento e versão em
processContextJson.opalaCommercial. Considere a experiência personalizada com IA,
consumo por cliente, tentativas, mídia, produção, taxas, entrega, suporte e margem.
Preserve oferta, preço, janela e orçamento autorizados. Não use modelo genérico do
tipo como aprovação específica nem valor ausente como zero. Reuse fontes válidas.

Compare três cenários e três alternativas práticas com benefícios, riscos e esforço.
Use o schema financeiro comercial: economics.offerPriceBrl igual ao preço aprovado;
variableCostPerSaleBrl e contributionPerSaleBrl reconciliáveis. Use economics.deadline
exclusivamente como data ISO YYYY-MM-DD. Quando windowEnd vier como instante ISO com
hora ou fuso, copie somente a data UTC inicial; nunca inclua hora, fuso ou explicação
nesse campo. APPROVE apenas com fontes suficientes, margem positiva e compatibilidade
com o teto. Ausência de dados exige ADJUST, com causa, fonte faltante e ação concreta.
Não autorize publicação, campanha, cobrança ou gasto. Não declare venda ou receita de teste.

## Contexto persistido
{{TASK_CONTEXT}}
';

INSERT INTO catalogo_vivo_prompt_version_v1(
  binding_id,version_number,text_content,sha256,status,created_by,created_at,
  reviewed_by,reviewed_at,review_note)
SELECT b.id,2,@opala_economics_prompt_v2,SHA2(@opala_economics_prompt_v2,256),'REVIEWED',
  'Correção sistêmica da tarefa 435',UTC_TIMESTAMP(6),'Revisão de contrato Opala',
  UTC_TIMESTAMP(6),'Prazo date-only alinhado entre contexto, prompt, schema e validador.'
FROM catalogo_vivo_binding_v1 b
JOIN business_process_activity_definition a ON a.id=b.activity_definition_id
JOIN business_process_definition p ON p.id=a.process_definition_id
WHERE p.process_code='opala-commercial-preparation-v1' AND p.version_number=1
  AND a.activity_id='economics';

UPDATE catalogo_vivo_binding_v1 b
JOIN business_process_activity_definition a ON a.id=b.activity_definition_id
JOIN business_process_definition p ON p.id=a.process_definition_id
JOIN catalogo_vivo_prompt_version_v1 v ON v.binding_id=b.id AND v.version_number=2
SET b.schema_id='prompts/opala-commercial-preparation/v1/economics-schema.json',
    b.schema_sha256='10c671ed11e8559701e77d349a645f86683442c95c0f0fb85aaaa2938d6d1793',
    b.active_version_id=v.id
WHERE p.process_code='opala-commercial-preparation-v1' AND p.version_number=1
  AND a.activity_id='economics';

INSERT INTO catalogo_vivo_audit_v1(binding_id,version_id,action,operator_name,note,created_at)
SELECT b.id,b.active_version_id,'BUGFIX_ACTIVATED','Correção sistêmica da tarefa 435',
  'Ativa prompt e schema econômicos Opala com deadline YYYY-MM-DD antes da inferência; preserva a resposta e o custo históricos.',UTC_TIMESTAMP(6)
FROM catalogo_vivo_binding_v1 b
JOIN business_process_activity_definition a ON a.id=b.activity_definition_id
JOIN business_process_definition p ON p.id=a.process_definition_id
WHERE p.process_code='opala-commercial-preparation-v1' AND p.version_number=1
  AND a.activity_id='economics';
