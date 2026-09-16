UPDATE pde_production_slot
SET draft_experience_json = JSON_SET(
        draft_experience_json,
        '$.commercialAccess',
        JSON_OBJECT(
          'experienceVersion', 'musa-pde-entry-v12-primeiro-ajuste-aplicavel',
          'accessDays', 90,
          'renewal', FALSE,
          'activationTrigger', 'PAYMENT_APPROVED',
          'scope', 'DAYS_2_TO_7_AND_SUPPORT_MATERIALS'
        )
      ),
    updated_at = UTC_TIMESTAMP(6)
WHERE product_slug = 'metodo-musa-7-dias'
  AND slot_code = 'v8'
  AND source_experiment_id = 92
  AND experience_version = 'musa-pde-entry-v12-primeiro-ajuste-aplicavel'
  AND status = 'CANDIDATE'
  AND published_experience_json IS NULL
  AND JSON_UNQUOTE(JSON_EXTRACT(draft_experience_json, '$.experienceVersion')) =
      'musa-pde-entry-v12-primeiro-ajuste-aplicavel'
  AND JSON_UNQUOTE(JSON_EXTRACT(draft_experience_json, '$.commercialCheckout.provider')) = 'PEPPER'
  AND JSON_UNQUOTE(JSON_EXTRACT(draft_experience_json, '$.commercialCheckout.offerReference')) =
      'owm6x';
