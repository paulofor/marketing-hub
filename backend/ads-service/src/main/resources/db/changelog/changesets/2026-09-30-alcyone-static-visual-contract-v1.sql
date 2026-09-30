SET @alcyone_static_result_fixtures = JSON_OBJECT(
  'contractVersion', 'PDE_STATIC_RESULT_FIXTURES_V1',
  'required', TRUE,
  'mode', 'DETERMINISTIC_HOMOLOGATION_ONLY',
  'artifactType', 'STATIC_IMAGE',
  'count', 3,
  'width', 1024,
  'height', 1024,
  'generator', 'DETERMINISTIC_TEST_ADAPTER',
  'providerCallsAuthorized', 0,
  'externalSideEffects', FALSE,
  'commercialEvidenceEligible', FALSE,
  'purpose', 'Comprovar cartões de combinação, responsividade e estados do harness sem produzir vídeo, consumir provider ou simular evidência humana.'
);

UPDATE product
SET validation_definition_json=JSON_SET(
      validation_definition_json,
      '$.delivery.audiovisualRequired', JSON_EXTRACT('false', '$'),
      '$.delivery.staticResultFixtures', JSON_EXTRACT(@alcyone_static_result_fixtures, '$')
    ),
    pde_experience_json=JSON_SET(
      pde_experience_json,
      '$.harness.audiovisualRequired', JSON_EXTRACT('false', '$'),
      '$.harness.staticResultFixtures', JSON_EXTRACT(@alcyone_static_result_fixtures, '$')
    ),
    commercial_notes=CASE
      WHEN commercial_notes LIKE '%[PDE_STATIC_RESULT_FIXTURES_V1] Alcyone%'
        THEN commercial_notes
      ELSE CONCAT(
        COALESCE(commercial_notes, ''),
        CASE WHEN COALESCE(commercial_notes, '')='' THEN '' ELSE ' ' END,
        '[PDE_STATIC_RESULT_FIXTURES_V1] Alcyone separa três imagens estáticas sintéticas de homologação do pipeline audiovisual pago; zero provider, zero gasto e nenhuma evidência humana ou comercial.'
      )
    END,
    updated_at=UTC_TIMESTAMP(6)
WHERE id=11
  AND internal_name='Alcyone'
  AND validation_definition_version='PDE_AGENT_VALIDATION_V1';
