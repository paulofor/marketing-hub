SET @alcyone_source_reference = 'product:11@agent-validation-v1';

SET @alcyone_agent_plan = JSON_OBJECT(
  'contractVersion', 'PDE_AGENT_VALIDATION_V1',
  'sourceReference', @alcyone_source_reference,
  'hypothesis', 'Para uma ocasião com prazo definido, três combinações visuais prontas a partir das peças informadas podem reduzir comparação manual e acelerar uma decisão utilizável.',
  'prototypeObjective', 'Homologar que o mecanismo entrega três combinações visuais utilizáveis em até dez minutos, sem prompt, inventário completo ou montagem manual externa.',
  'purchaseScene', JSON_OBJECT(
    'trigger', 'Ocasião específica já definida.',
    'deadline', 'Antes da data informada para a ocasião.',
    'costOfError', 'Perder tempo comparando alternativas ou chegar à ocasião sem uma decisão prática; intensidade ainda não comprovada pelo mercado.',
    'budgetEvidence', 'A página oficial da Resolva lista pacotes digitais de 7 looks por R$ 59,90 e 14 looks por R$ 79,90; oferta pública não comprova compra de Alcyone.',
    'failedAttempt', 'Alternativas gratuitas e aplicativos ativos existem; falha anterior do público específico ainda não foi comprovada.',
    'currentPaidBehavior', 'Serviços de montagem e aplicativos com compras internas estão ativos; pagamento pelo público de Alcyone permanece não observado.'
  ),
  'strongestFreeAlternative', 'Pedir opinião a alguém próximo e combinar inspiração manual de redes sociais ou aplicativo gratuito.',
  'prototypeAdvantage', 'Entregar três combinações visuais prontas, contextualizadas pela ocasião e pelas peças informadas, sem exigir que a cliente opere IA ou monte a resposta.',
  'customerValueDelivery', JSON_OBJECT(
    'territories', JSON_ARRAY('RECOGNITION', 'EFFORT_RELIEF'),
    'desiredTransformation', 'Passar de comparação manual para uma decisão visual reconhecível e pronta para uso.',
    'evidenceSourceIds', JSON_ARRAY(
      'public-refresh:resolva-app-2026-09-30',
      'public-refresh:style-dna-2026-09-30',
      'public-refresh:dressly-2026-09-30'
    ),
    'evidencePathways', JSON_ARRAY(
      'Oferta oficial de montagem de looks com preços próximos ao preço hipotético de Alcyone.',
      'Aplicativos oficiais ativos oferecem planejamento de looks por IA e por ocasião.'
    ),
    'readyMadeOutcome', 'Três combinações visuais com justificativas práticas vinculadas à ocasião, preferências, restrições e peças informadas.',
    'minimumCustomerInput', 'Ocasião, data, preferências, restrições práticas e referências de peças disponíveis.',
    'requiresPromptEngineering', FALSE,
    'requiresManualAssembly', FALSE,
    'usableWithoutAiKnowledge', TRUE,
    'customerStepsToValue', 4,
    'timeToUsableResultMinutes', 10,
    'automationBoundary', 'A IA interpreta os critérios e monta as combinações; a cliente apenas fornece contexto mínimo e decide se usará o resultado.'
  ),
  'trafficClass', 'AGENT_VALIDATION',
  'internalMarker', 'mh_internal_test',
  'requiredScenarios', JSON_ARRAY('ADHERENT', 'RECOVERY', 'SAFETY'),
  'requiredDevices', JSON_ARRAY('DESKTOP_1440', 'IPHONE_15_PRO', 'PIXEL_7'),
  'maxReadyResultSeconds', 600,
  'humanEvidenceClaimed', FALSE,
  'commercialEvidenceClaimed', FALSE,
  'paymentEnabled', FALSE,
  'publicationAuthorized', FALSE,
  'campaignAuthorized', FALSE,
  'mediaSpendAuthorizedBrl', 0,
  'sourceMaxAgeDays', 30,
  'continueCriteria', 'A mesma versão passa na homologação técnica, nos três cenários multiagente e na revisão de integridade; custo técnico auditado permanece em até R$ 24 por pacote. Isso libera apenas a preparação da comunicação.',
  'adjustCriteria', 'Corrigir somente a etapa reprovada, criar versão nova quando o artefato mudar e repetir os cenários afetados.',
  'stopCriteria', 'Bloquear diante de risco corporal ou de privacidade, resultado não utilizável em dez minutos, efeito externo indevido, custo acima do teto ou ausência de envelope finito.',
  'sourceRefreshRequired', FALSE,
  'sourceRefreshAction', 'Resolva, Style DNA e Dressly foram reconfirmadas em fontes oficiais em 30/09/2026; repetir apenas quando o prazo de 30 dias vencer ou a página deixar de responder.',
  'criteriaDeclaredAt', '2026-09-30T00:00:00Z',
  'sourceQualityEvaluatedAt', '2026-09-30T00:00:00Z',
  'publicationBoundary', 'Homologação interna por agentes, sem recrutamento, convite, opinião solicitada, contato, publicação, campanha, pagamento ou gasto. Somente mercado voluntário e venda reconciliada podem comprovar demanda.'
);

SET @alcyone_harness = JSON_OBJECT(
  'format', 'Experiência web restrita, mobile-first, com backend como fonte de verdade, executor de IA desacoplado e armazenamento protegido.',
  'deliverables', JSON_ARRAY(
    'Entrada guiada com ocasião, prazo, preferências, restrições e referências de peças.',
    'Processamento auditável, limitado e idempotente, sem expor prompt ou modelo.',
    'Resultado pronto com três combinações visuais e justificativas práticas.',
    'Acesso restrito, retomada, estados de erro e instrumentação segregada.',
    'Ledger de tentativas, artefatos, modelo, tokens, custo e falhas cobradas.'
  ),
  'valueJourney', JSON_ARRAY(
    'Abrir uma sessão interna isolada com trafficClass AGENT_VALIDATION e marcador mh_internal_test.',
    'Fornecer ocasião, data, preferências, restrições e referências de peças por controles simples.',
    'Validar entrada e quota antes de qualquer chamada faturável.',
    'Gerar três combinações visuais e justificativas em até dez minutos.',
    'Exercitar uso do resultado, recuperação, retomada, limite seguro e checkout simulado sem cobrança.',
    'Persistir eventos e artefatos internos sem contaminá-los com métricas comerciais.'
  ),
  'customerInput', JSON_ARRAY(
    'Ocasião específica e data.',
    'Preferências de estilo e restrições práticas.',
    'Referências de um conjunto mínimo de peças disponíveis.'
  ),
  'blockingCriteria', JSON_ARRAY(
    'Bloquear chamadas faturáveis sem reserva, idempotência, teto de custo e auditoria.',
    'Bloquear qualquer dado real de contato, cobrança, publicação, campanha ou gasto.',
    'Bloquear saída corporalmente julgadora, insegura ou inventada.',
    'Bloquear quando a versão executável e a instrumentação não forem comprovadas.'
  ),
  'privatePrototype', JSON_OBJECT(
    'scope', 'Harness interno para três cenários multiagente isolados da mesma versão, com três visuais iniciais por execução, uma recuperação controlada e efeitos externos nulos.',
    'simpleInput', 'Ocasião, data, preferências, restrições práticas e referências de peças, sem prompt.',
    'readyResult', 'Três combinações visuais prontas com justificativas práticas para a ocasião.',
    'maxValueTimeMinutes', 10,
    'instrumentationEvents', JSON_ARRAY(
      'EXPERIENCE_STARTED',
      'VALUE_MOMENT',
      'READY_RESULT_USED',
      'PREFERRED_OVER_FREE',
      'CHECKOUT_STARTED'
    ),
    'checkoutMode', 'SIMULATED_NO_CHARGE',
    'excludedFromPrototype', JSON_ARRAY(
      'Pessoa recrutada, convidada ou representada como evidência humana.',
      'Pagamento, pedido, venda, receita ou reembolso real.',
      'Contato, publicação, campanha, tráfego comercial, orçamento ou gasto de mídia.',
      'Inventário completo, assinatura, compra de roupas ou garantia social.',
      'Avaliação corporal, médica, terapêutica ou nutricional.'
    )
  ),
  'completionCriteria', JSON_ARRAY(
    'ADHERENT, RECOVERY e SAFETY executados na mesma versão em desktop, iPhone 15 Pro e Pixel 7.',
    'Resultado utilizável em até dez minutos no caminho aderente.',
    'Retomada e erro recuperável funcionais sem conhecimento de IA.',
    'Pedido fora do escopo bloqueado sem inventar resultado.',
    'Eventos, request, response, artefatos, tentativas e custos persistidos e segregados.',
    'Pagamento, publicação, campanha e gasto permanecem desativados.'
  ),
  'accessAndContinuity', JSON_ARRAY(
    'Sessões internas pseudonimizadas e segregadas por cenário, dispositivo e versão.',
    'Retomada sem duplicar chamada, custo ou evento.',
    'Expiração e indisponibilidade apresentadas funcionalmente sem cobrança.'
  ),
  'audiovisualRequired', TRUE,
  'nonAudiovisualSurfaces', JSON_ARRAY(
    'Entrada guiada e limites da experiência.',
    'Processamento, prazo e recuperação.',
    'Resultado, justificativas, uso e checkout simulado.',
    'Acesso, retomada, privacidade e relatório de homologação.'
  )
);

SET @alcyone_metrics = JSON_OBJECT(
  'primary', 'A mesma versão aprovada na homologação técnica, nos três cenários multiagente e na revisão de integridade.',
  'leading', JSON_ARRAY(
    'Tempo até resultado utilizável menor ou igual a dez minutos.',
    'Três combinações visuais válidas no caminho aderente.',
    'Recuperação e retomada concluídas sem duplicação.',
    'Limite seguro bloqueia saída fora de escopo.',
    'Custo técnico auditado por tentativa e por pacote.'
  ),
  'delivery', JSON_ARRAY(
    'Request, response bruto, modelo, tokens, custo, tentativas e falhas persistidos.',
    'Artefatos funcionais separados de auditoria e debug.',
    'Tráfego interno excluído de conversão, venda, receita e satisfação.'
  ),
  'continueCriteria', 'Todos os gates multiagente aprovados e custo técnico de até R$ 24 por pacote; avançar apenas para comunicação e preflight comercial.',
  'adjustCriteria', 'Corrigir a etapa causalmente reprovada e revalidar a nova versão.',
  'stopCriteria', 'Parar diante de risco, privacidade não mitigável, efeito externo indevido, resultado inútil ou economia sem envelope finito.'
);

SET @alcyone_market_strategy = JSON_OBJECT(
  'contractVersion', 'MARKET_STRATEGY_V4',
  'status', 'READY_FOR_AGENT_VALIDATION',
  'segment', 'Mulheres no Brasil com ocasião específica e prazo concreto que desejam reduzir o esforço de decidir um look com peças disponíveis.',
  'buyer', 'Mulher que precisa decidir o look antes de uma ocasião e busca uma saída visual pronta; disposição de pagar ainda não comprovada.',
  'problem', 'Converter ocasião, prazo, preferências, restrições e peças disponíveis em combinações coerentes sem pesquisa e montagem manual extensas.',
  'desiredOutcome', 'Receber rapidamente três combinações visuais prontas e justificadas para escolher e usar.',
  'offerThesis', 'Experiência digital limitada a uma ocasião, com três combinações visuais prontas e uma recuperação controlada.',
  'valueMechanism', 'A IA cruza contexto e peças para devolver uma decisão visual pronta, reduzindo comparação manual.',
  'causalHypothesis', 'A saída visual pronta reduz esforço no momento de decisão; somente comportamento voluntário e venda reconciliada testarão seu valor comercial.',
  'positioning', 'Assistente de decisão de look por ocasião, sem prompt, inventário completo ou julgamento corporal.',
  'primaryMetric', 'Gate multiagente aprovado; posteriormente, pagamento reconciliado e contribuição serão as provas comerciais.',
  'continueCriteria', 'Concluir homologação multiagente e preparar comunicação; não alegar demanda antes do mercado.',
  'adjustCriteria', 'Corrigir somente a etapa reprovada preservando público, ocasião e mecanismo.',
  'stopCriteria', 'Parar diante de risco, privacidade não mitigável, resultado inútil ou economia sem envelope finito.',
  'evidenceReferences', JSON_ARRAY(
    'https://www.resolvameulook.com/book-online',
    'https://play.google.com/store/apps/details?id=style.dna.app&hl=pt_BR',
    'https://play.google.com/store/apps/details?id=world.dressly.fashion'
  ),
  'agentValidationPlan', JSON_EXTRACT(@alcyone_agent_plan, '$')
);

UPDATE product
SET validation_definition_version='PDE_AGENT_VALIDATION_V1',
    validation_definition_json=JSON_OBJECT(
      'problem', 'Converter ocasião, prazo, preferências, restrições e peças disponíveis em combinações coerentes sem pesquisa e montagem manual extensas.',
      'promise', 'Receber três combinações visuais prontas e justificadas para uma ocasião específica.',
      'mechanism', 'A IA cruza contexto e peças e devolve uma decisão visual pronta.',
      'format', 'Experiência web restrita e mobile-first.',
      'delivery', JSON_EXTRACT(@alcyone_harness, '$'),
      'agentValidationPlan', JSON_EXTRACT(@alcyone_agent_plan, '$'),
      'privatePrototype', JSON_EXTRACT(@alcyone_harness, '$.privatePrototype'),
      'purchaseMomentStatus', 'WAITING_AGENT_HOMOLOGATION',
      'finalCommercialPrioritizationEligible', FALSE,
      'communicationPreparationEligible', FALSE,
      'economics', JSON_SET(
        JSON_REMOVE(JSON_EXTRACT(pde_experience_json, '$.economics'), '$.privateReadingsTarget'),
        '$.agentScenariosTarget', 3,
        '$.commercialSpendAuthorized', FALSE
      ),
      'successEvidence', JSON_EXTRACT(@alcyone_metrics, '$.delivery'),
      'decisionRules', JSON_EXTRACT(@alcyone_metrics, '$'),
      'productIdentity', JSON_EXTRACT(validation_definition_json, '$.productIdentity'),
      'sourceRefreshEvidence', JSON_OBJECT(
        'observedAt', '2026-09-30T00:00:00Z',
        'sourceMaxAgeDays', 30,
        'sourceUrls', JSON_ARRAY(
          'https://www.resolvameulook.com/book-online',
          'https://play.google.com/store/apps/details?id=style.dna.app&hl=pt_BR',
          'https://play.google.com/store/apps/details?id=world.dressly.fashion'
        ),
        'commercialEvidenceClaimed', FALSE
      )
    ),
    pde_experience_json=JSON_OBJECT(
      'contractVersion', 'PDE_HARNESS_PLAN_V1',
      'experienceVersion', 'agent-validation-v1',
      'status', 'AGENT_VALIDATION_PLANNED',
      'validationMode', 'MULTI_AGENT_V1',
      'agentValidationSourceReference', @alcyone_source_reference,
      'lineage', JSON_EXTRACT(pde_experience_json, '$.lineage'),
      'marketStrategy', JSON_EXTRACT(@alcyone_market_strategy, '$'),
      'economics', JSON_SET(
        JSON_REMOVE(JSON_EXTRACT(pde_experience_json, '$.economics'), '$.privateReadingsTarget'),
        '$.agentScenariosTarget', 3,
        '$.commercialSpendAuthorized', FALSE
      ),
      'metrics', JSON_EXTRACT(@alcyone_metrics, '$'),
      'harness', JSON_EXTRACT(@alcyone_harness, '$'),
      'agentValidationPlan', JSON_EXTRACT(@alcyone_agent_plan, '$'),
      'productIdentity', JSON_EXTRACT(pde_experience_json, '$.productIdentity'),
      'sourceRefreshEvidence', JSON_OBJECT(
        'observedAt', '2026-09-30T00:00:00Z',
        'sourceMaxAgeDays', 30,
        'sourceUrls', JSON_ARRAY(
          'https://www.resolvameulook.com/book-online',
          'https://play.google.com/store/apps/details?id=style.dna.app&hl=pt_BR',
          'https://play.google.com/store/apps/details?id=world.dressly.fashion'
        ),
        'commercialEvidenceClaimed', FALSE
      ),
      'publicationBoundary', 'Planejamento e homologação multiagente sem contato, publicação, campanha, pagamento, orçamento ou gasto; agentes não constituem prova de mercado.'
    ),
    commercial_notes=CASE
      WHEN commercial_notes LIKE '%[PDE_AGENT_VALIDATION_V1] Alcyone%'
        THEN commercial_notes
      ELSE CONCAT(
        COALESCE(commercial_notes, ''),
        CASE WHEN COALESCE(commercial_notes, '')='' THEN '' ELSE ' ' END,
        '[PDE_AGENT_VALIDATION_V1] Alcyone migrou do gate humano legado para homologação multiagente; fontes oficiais reconfirmadas em 30/09/2026; nenhuma evidência humana ou comercial alegada.'
      )
    END,
    automatic_execution_enabled=1,
    automatic_execution_changed_at=UTC_TIMESTAMP(6),
    automatic_execution_changed_by='alcyone-agent-validation-v1',
    updated_at=UTC_TIMESTAMP(6)
WHERE id=11
  AND internal_name='Alcyone'
  AND validation_definition_version IN ('PDE_PRIVATE_VALIDATION_V1','PDE_AGENT_VALIDATION_V1');

UPDATE product_process_run_v1
SET source_reference=@alcyone_source_reference,
    scope_key=SHA2(CONCAT(
      product_id, '|', process_definition_id, '|', chain_definition_id, '|',
      IFNULL(CAST(learning_cycle_id AS CHAR), 'null'), '|', @alcyone_source_reference
    ), 256),
    reason='Atividade aguardando execução pelo backend sob contrato de homologação multiagente.',
    updated_at=UTC_TIMESTAMP(6),
    last_reconciled_at=UTC_TIMESTAMP(6),
    revision=revision+1
WHERE id=26
  AND product_id=11
  AND source_reference IN ('product:11@private-validation-v1','product:11@agent-validation-v1');

UPDATE business_process_activity_instance
SET source_reference=@alcyone_source_reference,
    updated_at=UTC_TIMESTAMP(6)
WHERE id>=414
  AND source_reference IN ('product:11@private-validation-v1','product:11@agent-validation-v1');

UPDATE agent_task
SET source_reference=@alcyone_source_reference,
    updated_at=UTC_TIMESTAMP(6)
WHERE id>=534
  AND source_reference IN ('product:11@private-validation-v1','product:11@agent-validation-v1');

UPDATE agent_task
SET source_reference=@alcyone_source_reference,
    title='Construir jornada para homologação multiagente · Alcyone',
    description='Entregar jornada do formato aprovado com primeira ação evidente, somente entradas necessárias, três combinações visuais prontas em até dez minutos e benefício aplicável. Preparar ADHERENT, RECOVERY e SAFETY em desktop, iPhone e Pixel; preservar tráfego interno, checkout sem cobrança e efeitos externos nulos. Não recrutar, convidar, solicitar opinião nem alegar evidência humana ou comercial.',
    updated_at=UTC_TIMESTAMP(6)
WHERE id=534
  AND source_reference IN ('product:11@private-validation-v1','product:11@agent-validation-v1');
