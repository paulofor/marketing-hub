SET @rigel_exact_service_scope = '{"includedItems":["Briefing inicial guiado","15 respostas personalizadas","8 perguntas de qualificação","4 follow-ups manuais","Regras de escalonamento","Guia, checklist, revisão humana e entrega"],"excludedItems":["Software, bot ou automação","Integração ou disparo automático","Assinatura ou cobrança recorrente","Garantia de conversão, faturamento ou agenda cheia"],"deadlineStartsWhen":"O prazo começa quando o pagamento estiver confirmado e as informações mínimas do briefing estiverem completas."}';

UPDATE product
SET pde_experience_json = JSON_SET(
      pde_experience_json,
      '$.serviceScope', JSON_EXTRACT(@rigel_exact_service_scope, '$'),
      '$.missions[4].deliveryContract.sections[0].minItems', 15,
      '$.missions[4].deliveryContract.sections[0].maxItems', 15,
      '$.missions[4].deliveryContract.sections[1].minItems', 8,
      '$.missions[4].deliveryContract.sections[1].maxItems', 8,
      '$.missions[4].deliveryContract.sections[2].minItems', 4,
      '$.missions[4].deliveryContract.sections[2].maxItems', 4
    ),
    updated_at = CURRENT_TIMESTAMP
WHERE slug = 'kit-whatsapp-pronto'
  AND pde_experience_json IS NOT NULL
  AND JSON_VALID(pde_experience_json) = 1
  AND JSON_UNQUOTE(JSON_EXTRACT(pde_experience_json, '$.experienceVersion')) = 'kit-whatsapp-pronto-pde-v2'
  AND JSON_UNQUOTE(JSON_EXTRACT(pde_experience_json, '$.missions[4].id')) = 'entrega-completa-48h'
  AND JSON_UNQUOTE(JSON_EXTRACT(pde_experience_json, '$.missions[4].deliveryContract.sections[0].id')) = 'responses'
  AND JSON_UNQUOTE(JSON_EXTRACT(pde_experience_json, '$.missions[4].deliveryContract.sections[1].id')) = 'qualificationQuestions'
  AND JSON_UNQUOTE(JSON_EXTRACT(pde_experience_json, '$.missions[4].deliveryContract.sections[2].id')) = 'followUps';

UPDATE pde_production_slot
SET draft_experience_json = CASE
      WHEN draft_experience_json IS NOT NULL
        AND JSON_VALID(draft_experience_json) = 1
        AND JSON_UNQUOTE(JSON_EXTRACT(draft_experience_json, '$.missions[4].id')) = 'entrega-completa-48h'
        AND JSON_UNQUOTE(JSON_EXTRACT(draft_experience_json, '$.missions[4].deliveryContract.sections[0].id')) = 'responses'
        AND JSON_UNQUOTE(JSON_EXTRACT(draft_experience_json, '$.missions[4].deliveryContract.sections[1].id')) = 'qualificationQuestions'
        AND JSON_UNQUOTE(JSON_EXTRACT(draft_experience_json, '$.missions[4].deliveryContract.sections[2].id')) = 'followUps'
      THEN JSON_SET(
        draft_experience_json,
        '$.serviceScope', JSON_EXTRACT(@rigel_exact_service_scope, '$'),
        '$.missions[4].deliveryContract.sections[0].minItems', 15,
        '$.missions[4].deliveryContract.sections[0].maxItems', 15,
        '$.missions[4].deliveryContract.sections[1].minItems', 8,
        '$.missions[4].deliveryContract.sections[1].maxItems', 8,
        '$.missions[4].deliveryContract.sections[2].minItems', 4,
        '$.missions[4].deliveryContract.sections[2].maxItems', 4
      )
      ELSE draft_experience_json
    END,
    published_experience_json = JSON_SET(
      published_experience_json,
      '$.serviceScope', JSON_EXTRACT(@rigel_exact_service_scope, '$'),
      '$.missions[4].deliveryContract.sections[0].minItems', 15,
      '$.missions[4].deliveryContract.sections[0].maxItems', 15,
      '$.missions[4].deliveryContract.sections[1].minItems', 8,
      '$.missions[4].deliveryContract.sections[1].maxItems', 8,
      '$.missions[4].deliveryContract.sections[2].minItems', 4,
      '$.missions[4].deliveryContract.sections[2].maxItems', 4
    ),
    published_by = 'liquibase:rigel-exact-delivery-contract-v1',
    published_at = CURRENT_TIMESTAMP,
    updated_at = CURRENT_TIMESTAMP
WHERE product_slug = 'kit-whatsapp-pronto'
  AND source_experiment_id = 89
  AND published_experience_json IS NOT NULL
  AND JSON_VALID(published_experience_json) = 1
  AND JSON_UNQUOTE(JSON_EXTRACT(published_experience_json, '$.experienceVersion')) = 'kit-whatsapp-pronto-pde-v2'
  AND JSON_UNQUOTE(JSON_EXTRACT(published_experience_json, '$.missions[4].id')) = 'entrega-completa-48h'
  AND JSON_UNQUOTE(JSON_EXTRACT(published_experience_json, '$.missions[4].deliveryContract.sections[0].id')) = 'responses'
  AND JSON_UNQUOTE(JSON_EXTRACT(published_experience_json, '$.missions[4].deliveryContract.sections[1].id')) = 'qualificationQuestions'
  AND JSON_UNQUOTE(JSON_EXTRACT(published_experience_json, '$.missions[4].deliveryContract.sections[2].id')) = 'followUps';
