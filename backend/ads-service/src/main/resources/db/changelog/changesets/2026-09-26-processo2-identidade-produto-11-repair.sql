UPDATE product product_record
JOIN product_type_definition type_definition
  ON type_definition.code='AI_PRODUCT'
 AND type_definition.internal_name='Safira'
 AND type_definition.status='ACTIVE'
SET product_record.internal_name='Alcyone',
    product_record.product_type_id=type_definition.id,
    product_record.product_type=type_definition.name,
    product_record.validation_definition_json=JSON_SET(
      product_record.validation_definition_json,
      '$.productIdentity',
      JSON_OBJECT(
        'contractVersion','PRODUCT_IDENTITY_V1',
        'mode','CREATE',
        'internalName','Alcyone',
        'productTypeCode','AI_PRODUCT',
        'productTypeInternalName','Safira',
        'classificationRationale','A personalizacao por IA constitui o mecanismo de valor; a experiencia web e o formato.'
      )
    ),
    product_record.pde_experience_json=JSON_SET(
      product_record.pde_experience_json,
      '$.productIdentity',
      JSON_OBJECT(
        'contractVersion','PRODUCT_IDENTITY_V1',
        'mode','CREATE',
        'internalName','Alcyone',
        'productTypeCode','AI_PRODUCT',
        'productTypeInternalName','Safira',
        'classificationRationale','A personalizacao por IA constitui o mecanismo de valor; a experiencia web e o formato.'
      )
    ),
    product_record.commercial_notes=CONCAT(
      COALESCE(product_record.commercial_notes,''),
      CASE WHEN COALESCE(product_record.commercial_notes,'')='' THEN '' ELSE ' ' END,
      '[PRODUCT_IDENTITY_V1] Execucao independente #32 corrigida: nome interno Alcyone e tipo AI_PRODUCT/Safira; titulo comercial preservado.'
    ),
    product_record.updated_at=UTC_TIMESTAMP(6)
WHERE product_record.id=11
  AND product_record.internal_name='Decisão de look para uma ocasião específica · PDE planejado #46';
