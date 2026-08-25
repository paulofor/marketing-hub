SET @rigel_service_scope = '{"includedItems":["Briefing inicial guiado","10 a 20 respostas personalizadas","5 a 10 perguntas de qualificação","3 a 5 follow-ups manuais","Regras de escalonamento","Guia, checklist, revisão humana e entrega"],"excludedItems":["Software, bot ou automação","Integração ou disparo automático","Assinatura ou cobrança recorrente","Garantia de conversão, faturamento ou agenda cheia"],"deadlineStartsWhen":"O prazo começa quando o pagamento estiver confirmado e as informações mínimas do briefing estiverem completas."}';

SET @rigel_promise = 'Após o pagamento confirmado e o briefing mínimo completo, em até 48 horas receba seu atendimento de WhatsApp personalizado e revisado: respostas, perguntas, follow-ups e regras prontas para conduzir cada conversa ao próximo passo com mais clareza e menos improviso.';

SET @rigel_public_proofs = '[{"id":"sample-response","type":"RESPONSE","title":"Resposta inicial","content":"Oi! Passando com calma para saber se ficou alguma dúvida sobre o orçamento de manicure. Se quiser, eu explico o que está incluído antes de você decidir.","items":[],"evidenceLabel":"Interface real · exemplo fictício","source":"kit-whatsapp-tasting-v1:orcamento-sem-resposta:acolhedor"},{"id":"sample-question","type":"QUALIFICATION_QUESTION","title":"Pergunta de qualificação","content":"O que você precisa confirmar primeiro sobre manicure: prazo, disponibilidade ou forma de pagamento?","items":[],"evidenceLabel":"Interface real · exemplo fictício","source":"kit-whatsapp-tasting-v1:orcamento-sem-resposta:acolhedor"},{"id":"sample-follow-ups","type":"FOLLOW_UPS","title":"Três follow-ups manuais","content":"","items":["Se ainda estiver avaliando, posso resumir as opções de manicure em uma mensagem curta.","Quer que eu verifique uma data específica antes de você decidir?","Vou encerrar por aqui para não incomodar. Se quiser retomar a conversa sobre manicure, é só me chamar."],"evidenceLabel":"Interface real · exemplo fictício","source":"kit-whatsapp-tasting-v1:orcamento-sem-resposta:acolhedor"},{"id":"real-offer","type":"OFFER","title":"Implantação personalizada e assistida","content":"R$ 349 em pagamento único, sem recorrência. Você revisa antes de usar; não há bot, disparo em massa ou envio automático.","items":[],"evidenceLabel":"Oferta canônica do experimento 89","source":"marketing-hub:commercial-offer:experiment-89"}]';

SET @rigel_commercial_process = '[{"order":1,"title":"Briefing guiado","description":"Você informa serviços, dúvidas frequentes, políticas, tom e exemplos anonimizados.","timing":"Após a confirmação do pagamento"},{"order":2,"title":"Prévia para validar o tom","description":"Você recebe uma primeira sequência para confirmar a linguagem e a direção antes da entrega completa.","timing":"Em até 12 horas com a entrada completa"},{"order":3,"title":"Entrega completa","description":"Respostas, perguntas, follow-ups, regras, guia e checklist chegam organizados e revisados.","timing":"Em até 48 horas com a entrada completa"},{"order":4,"title":"Primeira aplicação","description":"Você escolhe um bloco pequeno, revisa e usa manualmente no seu atendimento real.","timing":"Na primeira semana de uso"}]';

SET @rigel_commercial_binding = '{"experimentId":89,"primaryCta":"Quero meu atendimento sob medida","priceBrl":349,"billingModel":"ONE_TIME"}';

UPDATE product
SET pde_experience_json = JSON_SET(
      pde_experience_json,
      '$.experienceVersion', 'kit-whatsapp-pronto-pde-v2',
      '$.layoutKey', 'assisted-service-v2',
      '$.funnelVersion', 'pde-assisted-service-v2',
      '$.promise', @rigel_promise,
      '$.serviceScope', JSON_EXTRACT(@rigel_service_scope, '$'),
      '$.publicProofs', JSON_EXTRACT(@rigel_public_proofs, '$'),
      '$.commercialProcess', JSON_EXTRACT(@rigel_commercial_process, '$'),
      '$.commercialBinding', JSON_EXTRACT(@rigel_commercial_binding, '$')
    ),
    primary_cta = 'Quero meu atendimento sob medida',
    updated_at = CURRENT_TIMESTAMP
WHERE slug = 'kit-whatsapp-pronto'
  AND pde_experience_json IS NOT NULL
  AND JSON_VALID(pde_experience_json) = 1;

UPDATE experiment
SET funnel_promise = @rigel_promise,
    primary_cta = 'Quero meu atendimento sob medida',
    updated_at = CURRENT_TIMESTAMP
WHERE id = 89;

UPDATE pde_production_slot
SET experience_version = 'kit-whatsapp-pronto-pde-v2',
    layout_key = 'assisted-service-v2',
    draft_experience_json = JSON_SET(
      draft_experience_json,
      '$.experienceVersion', 'kit-whatsapp-pronto-pde-v2',
      '$.layoutKey', 'assisted-service-v2',
      '$.funnelVersion', 'pde-assisted-service-v2',
      '$.promise', @rigel_promise,
      '$.serviceScope', JSON_EXTRACT(@rigel_service_scope, '$'),
      '$.publicProofs', JSON_EXTRACT(@rigel_public_proofs, '$'),
      '$.commercialProcess', JSON_EXTRACT(@rigel_commercial_process, '$'),
      '$.commercialBinding', JSON_EXTRACT(@rigel_commercial_binding, '$')
    ),
    published_experience_json = JSON_SET(
      published_experience_json,
      '$.experienceVersion', 'kit-whatsapp-pronto-pde-v2',
      '$.layoutKey', 'assisted-service-v2',
      '$.funnelVersion', 'pde-assisted-service-v2',
      '$.promise', @rigel_promise,
      '$.serviceScope', JSON_EXTRACT(@rigel_service_scope, '$'),
      '$.publicProofs', JSON_EXTRACT(@rigel_public_proofs, '$'),
      '$.commercialProcess', JSON_EXTRACT(@rigel_commercial_process, '$'),
      '$.commercialBinding', JSON_EXTRACT(@rigel_commercial_binding, '$')
    ),
    published_by = 'liquibase:rigel-commercial-experience-v2',
    published_at = CURRENT_TIMESTAMP,
    updated_at = CURRENT_TIMESTAMP
WHERE product_slug = 'kit-whatsapp-pronto'
  AND source_experiment_id = 89
  AND draft_experience_json IS NOT NULL
  AND published_experience_json IS NOT NULL
  AND JSON_VALID(draft_experience_json) = 1
  AND JSON_VALID(published_experience_json) = 1;
