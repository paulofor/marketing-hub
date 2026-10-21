SET @opala_creative_v2 = '# Preparação comercial Opala · creative · v2

Respeite o formato efetivo aprovado na comunicação do próprio experimento.

Para IMAGE, selecione em opalaCommercial.creatives uma única peça vigente que já
tenha imageUrl HTTPS e destinationUrl da candidata. Entregue videoAssetId: null e
copie exatamente headline, primaryText e description dessa peça. Não gere imagem,
não reescreva copy e não reutilize anúncio de outro experimento. Rascunho ainda
depende da revisão comercial independente; READY nesta atividade não o aprova.
Se não houver peça adequada ou houver duas com o mesmo conteúdo, BLOCKED com
causa e atividade de correção. Não invente vídeo para satisfazer esta atividade.

Para vídeo, vincule videoAssetId de um vídeo AD aprovado do próprio experimento.
Entregue headline, primaryText e description fiéis à oferta e comunicação aprovada.
O backend cria o rascunho pelo serviço oficial; Têmis e a aprovação final continuam
obrigatórios. Não gerar nova mídia nem escolher vídeo LANDING_HERO. Se já existir
criativo do vídeo, preserve exatamente sua copy, sem criar nova versão implícita.

O produto vendido é uma experiência útil, personalizada e acessível, apoiada por IA.
Leia processContextJson.opalaCommercial e learningSalesCycle como fontes do mesmo
produto, ciclo, experimento e versão. Reaproveite provas válidas; nunca copie dados de
outro produto ou do experimento predecessor. Compare três alternativas, explicitando
benefício, risco e esforço; escolha a aderente ao contrato aprovado.

Retorne somente o JSON do schema existente. READY significa instrução interna
completa, nunca ativo publicado ou campanha autorizada. Não declare vendas, entrega
real, aprovação humana ou autorização de gasto. Campos de instrução não utilizados:
null, string vazia ou lista vazia. BLOCKED explica causa e atividade necessária.

## Contexto persistido
{{TASK_CONTEXT}}
';

INSERT INTO catalogo_vivo_prompt_version_v1(
  binding_id,version_number,text_content,sha256,status,created_by,created_at,
  reviewed_by,reviewed_at,review_note)
SELECT b.id,2,@opala_creative_v2,SHA2(@opala_creative_v2,256),'REVIEWED',
  'Correção do percurso estático',UTC_TIMESTAMP(6),'Revisão local do contrato Opala',
  UTC_TIMESTAMP(6),'Aceita peça IMAGE exata sem dispensar revisão, orçamento ou homologação.'
FROM catalogo_vivo_binding_v1 b
JOIN business_process_activity_definition a ON a.id=b.activity_definition_id
JOIN business_process_definition p ON p.id=a.process_definition_id
WHERE p.process_code='opala-commercial-preparation-v1' AND p.version_number=1
  AND a.activity_id='creative';

UPDATE catalogo_vivo_binding_v1 b
JOIN business_process_activity_definition a ON a.id=b.activity_definition_id
JOIN business_process_definition p ON p.id=a.process_definition_id
JOIN catalogo_vivo_prompt_version_v1 v ON v.binding_id=b.id AND v.version_number=2
SET b.active_version_id=v.id
WHERE p.process_code='opala-commercial-preparation-v1' AND p.version_number=1
  AND a.activity_id='creative';

INSERT INTO catalogo_vivo_audit_v1(binding_id,version_id,action,operator_name,note,created_at)
SELECT b.id,b.active_version_id,'BUGFIX_ACTIVATED','Correção do percurso estático',
  'Ativa creative v2, preservando schema, tarefas fixadas e revisão comercial independente.',
  UTC_TIMESTAMP(6)
FROM catalogo_vivo_binding_v1 b
JOIN business_process_activity_definition a ON a.id=b.activity_definition_id
JOIN business_process_definition p ON p.id=a.process_definition_id
WHERE p.process_code='opala-commercial-preparation-v1' AND p.version_number=1
  AND a.activity_id='creative';
