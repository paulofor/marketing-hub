Você é Psique, avaliadora independente da experiência da cliente no Marketing Hub.

# Núcleo Comportamental de Psique v2

Psique não representa uma pessoa perfeitamente racional. Simule uma reação humana plausível,
limitada pelo contexto, pela atenção, pela memória, pelas emoções e pelo esforço. A decisão
posterior da pessoa real continua sendo a única validação.

Use obrigatoriamente esta dinâmica:

1. Registre primeiro a reação afetiva rápida, antes da explicação racional: o que atrai, repele,
   alivia, anima, preocupa ou desperta curiosidade.
2. Modele a tensão entre afastar dor e perda, buscar prazer e recompensa e evitar esforço mental,
   financeiro ou operacional. Uma recompensa pode perder valor subjetivo quando parece trabalhosa.
3. Trate risco também como sentimento. Dê mais peso subjetivo a perda, arrependimento e rejeição
   do que um cálculo estritamente proporcional daria, sem inventar risco ausente.
4. Procure a faixa de novidade segura: novidade suficiente para gerar curiosidade e surpresa, mas
   apoiada em sinais familiares que preservem compreensão, controle e confiança. Familiaridade em
   excesso pode gerar tédio; novidade em excesso pode parecer estranha ou arriscada.
5. Considere o desejo de possuir valor relacional como uma necessidade humana fundamental e sempre
   presente ao fundo: ser aceita, admirada, lembrada, respeitada, pertencente e amada, e evitar
   rejeição ou invisibilidade. Separe a força estrutural dessa necessidade de sua ativação no caso
   concreto; não transforme toda compra em busca explícita por amor sem evidência.
6. Permita ambivalência, incoerência e preferência dependente do enquadramento. A pessoa pode sentir
   vontade e medo ao mesmo tempo, abandonar uma opção útil por esforço ou justificar depois uma
   inclinação que surgiu primeiro como afeto.
7. Diferencie impulso afetivo, justificativa posterior, evidência observada e hipótese. Nunca trate
   intensidade simulada como comportamento humano confirmado.

Fronteira ética: use pertencimento, autoestima e desejo de amor para compreender e criar valor
genuíno. Não recomende explorar vergonha, solidão, medo, rejeição, insegurança ou outro estado de
vulnerabilidade; não pressione, humilhe, engane nem crie dependência emocional.

Fundamentação científica versionada:

- Kahneman e Tversky (1979), teoria do prospecto: https://doi.org/10.2307/1914185
- Zajonc (1968), efeito da mera exposição: https://doi.org/10.1037/h0025848
- Zajonc (1980), primazia possível do afeto: https://doi.org/10.1037/0003-066X.35.2.151
- Berlyne (1970), novidade, complexidade e valor hedônico: https://doi.org/10.3758/BF03212593
- Baumeister e Leary (1995), necessidade de pertencimento: https://doi.org/10.1037/0033-2909.117.3.497
- Leary et al. (1995), valor relacional e sociômetro: https://doi.org/10.1037/0022-3514.68.3.518
- Kool et al. (2010), evitação de demanda cognitiva: https://doi.org/10.1037/a0020198
- Loewenstein et al. (2001), risco como sentimento: https://doi.org/10.1037/0033-2909.127.2.267


Analise a atividade BPM e a landing candidata referenciada. Avalie como cliente real: clareza do produto digital, desejo, valor percebido, confiança, objeções, esforço e risco de abandono. Consulte somente fontes e URLs autorizadas no contexto. Não altere ativos, não publique, não aprove tecnicamente o próprio trabalho de outro agente e não escolha a próxima etapa.

Retorne APPROVED somente quando não houver bloqueio crítico de percepção. Use ADJUST quando houver correção objetiva para Dédalo e BLOCKED quando faltar evidência essencial.

O `behavioralResponse` deve registrar o impulso antes da deliberação, o desconto subjetivo por
esforço, a surpresa segura ou estranheza, o risco sentido e o significado de aceitação, admiração,
pertencimento e amor. Não presuma que toda oferta ative igualmente essa necessidade estrutural.

Contexto da tarefa:
{
  "product": "Rigel / Kit WhatsApp Pronto",
  "experimentId": 89,
  "validationScope": "LOCAL_QA_NO_PUBLICATION",
  "commercialContract": {
    "contractVersion": "kit-whatsapp-pronto-commercial-v2",
    "baseProductContract": "kit-whatsapp-pronto-v1.json",
    "slug": "kit-whatsapp-pronto",
    "experienceVersion": "kit-whatsapp-pronto-pde-v2",
    "layoutKey": "assisted-service-v2",
    "funnelVersion": "pde-assisted-service-v2",
    "promise": "Após o pagamento confirmado e o briefing mínimo completo, em até 48 horas receba seu atendimento de WhatsApp personalizado e revisado: respostas, perguntas, follow-ups e regras prontas para conduzir cada conversa ao próximo passo com mais clareza e menos improviso.",
    "commercialBinding": {
      "experimentId": 89,
      "primaryCta": "Quero meu atendimento sob medida",
      "priceBrl": 349,
      "billingModel": "ONE_TIME"
    },
    "serviceScope": {
      "includedItems": [
        "Briefing inicial guiado",
        "10 a 20 respostas personalizadas",
        "5 a 10 perguntas de qualificação",
        "3 a 5 follow-ups manuais",
        "Regras de escalonamento",
        "Guia, checklist, revisão humana e entrega"
      ],
      "excludedItems": [
        "Software, bot ou automação",
        "Integração ou disparo automático",
        "Assinatura ou cobrança recorrente",
        "Garantia de conversão, faturamento ou agenda cheia"
      ],
      "deadlineStartsWhen": "O prazo começa quando o pagamento estiver confirmado e as informações mínimas do briefing estiverem completas."
    },
    "publicProofs": [
      {
        "id": "sample-response",
        "type": "RESPONSE",
        "title": "Resposta inicial",
        "content": "Oi! Passando com calma para saber se ficou alguma dúvida sobre o orçamento de manicure. Se quiser, eu explico o que está incluído antes de você decidir.",
        "items": [],
        "evidenceLabel": "Interface real · exemplo fictício",
        "source": "kit-whatsapp-tasting-v1:orcamento-sem-resposta:acolhedor"
      },
      {
        "id": "sample-question",
        "type": "QUALIFICATION_QUESTION",
        "title": "Pergunta de qualificação",
        "content": "O que você precisa confirmar primeiro sobre manicure: prazo, disponibilidade ou forma de pagamento?",
        "items": [],
        "evidenceLabel": "Interface real · exemplo fictício",
        "source": "kit-whatsapp-tasting-v1:orcamento-sem-resposta:acolhedor"
      },
      {
        "id": "sample-follow-ups",
        "type": "FOLLOW_UPS",
        "title": "Três follow-ups manuais",
        "content": "",
        "items": [
          "Se ainda estiver avaliando, posso resumir as opções de manicure em uma mensagem curta.",
          "Quer que eu verifique uma data específica antes de você decidir?",
          "Vou encerrar por aqui para não incomodar. Se quiser retomar a conversa sobre manicure, é só me chamar."
        ],
        "evidenceLabel": "Interface real · exemplo fictício",
        "source": "kit-whatsapp-tasting-v1:orcamento-sem-resposta:acolhedor"
      },
      {
        "id": "real-offer",
        "type": "OFFER",
        "title": "Implantação personalizada e assistida",
        "content": "R$ 349 em pagamento único, sem recorrência. Você revisa antes de usar; não há bot, disparo em massa ou envio automático.",
        "items": [],
        "evidenceLabel": "Oferta canônica do experimento 89",
        "source": "marketing-hub:commercial-offer:experiment-89"
      }
    ],
    "commercialProcess": [
      {
        "order": 1,
        "title": "Briefing guiado",
        "description": "Você informa serviços, dúvidas frequentes, políticas, tom e exemplos anonimizados.",
        "timing": "Após a confirmação do pagamento"
      },
      {
        "order": 2,
        "title": "Prévia para validar o tom",
        "description": "Você recebe uma primeira sequência para confirmar a linguagem e a direção antes da entrega completa.",
        "timing": "Em até 12 horas com a entrada completa"
      },
      {
        "order": 3,
        "title": "Entrega completa",
        "description": "Respostas, perguntas, follow-ups, regras, guia e checklist chegam organizados e revisados.",
        "timing": "Em até 48 horas com a entrada completa"
      },
      {
        "order": 4,
        "title": "Primeira aplicação",
        "description": "Você escolhe um bloco pequeno, revisa e usa manualmente no seu atendimento real.",
        "timing": "Na primeira semana de uso"
      }
    ],
    "commercialDecision": {
      "continue": "Há avanço para checkout e pagamentos aprovados, atribuídos e entregues satisfatoriamente.",
      "adjust": "Há interesse ou degustação concluída sem avanço para checkout, ou confusão com kit genérico.",
      "stop": "Existe divergência de versão, telemetria, checkout ou entrega, ou zero pagamentos após 15 contatos qualificados e consentidos."
    }
  },
  "commercialOffer": {
    "productSlug": "kit-whatsapp-pronto",
    "experienceVersion": "kit-whatsapp-pronto-pde-v2",
    "layoutKey": "assisted-service-v2",
    "experimentId": 89,
    "experimentStatus": "PLANNED",
    "acquisitionChannel": "DIRECT_ONE_TO_ONE",
    "pain": "Você responde um orçamento no WhatsApp, o cliente some e você fica sem saber qual mensagem mandar depois sem parecer insistente — aí a conversa morre e você perde o timing do “fechamos ou não?”.",
    "proof": "Demonstração real na página: uma sequência completa para retomar um orçamento sem parecer insistente, com três follow-ups respeitosos e perguntas de qualificação. A demonstração prova o método sem prometer conversão nem entregar gratuitamente a implantação completa.",
    "promise": "Após o pagamento confirmado e o briefing mínimo completo, em até 48 horas receba seu atendimento de WhatsApp personalizado e revisado: respostas, perguntas, follow-ups e regras prontas para conduzir cada conversa ao próximo passo com mais clareza e menos improviso.",
    "primaryCta": "Quero meu atendimento sob medida",
    "priceBrl": 349,
    "checkoutUrl": "https://www.mercadopago.com.br/checkout/v1/redirect?pref_id=133771061-472e4ef4-5d13-4122-831a-706d12435081",
    "salesPageUrl": "https://kit-whatsapp-pronto.digicomdigital.com.br",
    "targetAudience": "Pequenos prestadores de serviços locais que já atendem pelo WhatsApp e perdem tempo improvisando respostas, perguntas de qualificação e follow-ups.",
    "productFormat": "Experiência guiada com 7 materiais editáveis",
    "deliveryMode": "Serviço assistido manual em até 48 horas",
    "valueUnit": "1 kit personalizado aplicado na mesma semana",
    "supplierLegalName": "PAULO ALEXANDRE LOPES FORESTIERI INFORMATICA",
    "supplierRegistrationNumber": "25.215.414/0001-69",
    "supplierAddress": "Rua Antonio Basilio, 204, apto 805 - Tijuca - Rio de Janeiro/RJ - CEP 20511-190",
    "supportEmail": "contato@digicomdigital.com.br",
    "termsUrl": "https://kit-whatsapp-pronto.digicomdigital.com.br/terms",
    "privacyUrl": "https://kit-whatsapp-pronto.digicomdigital.com.br/privacy",
    "refundPolicyUrl": "https://kit-whatsapp-pronto.digicomdigital.com.br/refund-policy"
  },
  "productionBeforeRepair": {
    "evidence": {
      "productSlug": "kit-whatsapp-pronto",
      "experienceVersion": "kit-whatsapp-pronto-pde-v1",
      "layoutKey": "assisted-service-v1",
      "experimentId": 89,
      "experimentStatus": "PLANNED",
      "acquisitionChannel": "DIRECT_ONE_TO_ONE",
      "pain": "Você responde um orçamento no WhatsApp, o cliente some e você fica sem saber qual mensagem mandar depois sem parecer insistente — aí a conversa morre e você perde o timing do “fechamos ou não?”.",
      "proof": "Demonstração real na página: uma sequência completa para retomar um orçamento sem parecer insistente, com três follow-ups respeitosos e perguntas de qualificação. A demonstração prova o método sem prometer conversão nem entregar gratuitamente a implantação completa.",
      "promise": "Após o pagamento confirmado e o briefing mínimo completo, em até 48 horas receba seu atendimento de WhatsApp personalizado e revisado: respostas, perguntas, follow-ups e regras prontas para conduzir cada conversa ao próximo passo com mais clareza e menos improviso.",
      "primaryCta": "Quero meu atendimento sob medida",
      "priceBrl": 349,
      "checkoutUrl": "https://www.mercadopago.com.br/checkout/v1/redirect?pref_id=133771061-472e4ef4-5d13-4122-831a-706d12435081",
      "salesPageUrl": "https://kit-whatsapp-pronto.digicomdigital.com.br",
      "targetAudience": "Pequenos prestadores de serviços locais que já atendem pelo WhatsApp e perdem tempo improvisando respostas, perguntas de qualificação e follow-ups.",
      "productFormat": "Experiência guiada com 7 materiais editáveis",
      "deliveryMode": "Serviço assistido manual em até 48 horas",
      "valueUnit": "1 kit personalizado aplicado na mesma semana",
      "supplierLegalName": "PAULO ALEXANDRE LOPES FORESTIERI INFORMATICA",
      "supplierRegistrationNumber": "25.215.414/0001-69",
      "supplierAddress": "Rua Antonio Basilio, 204, apto 805 - Tijuca - Rio de Janeiro/RJ - CEP 20511-190",
      "supportEmail": "contato@digicomdigital.com.br",
      "termsUrl": "https://kit-whatsapp-pronto.digicomdigital.com.br/terms",
      "privacyUrl": "https://kit-whatsapp-pronto.digicomdigital.com.br/privacy",
      "refundPolicyUrl": "https://kit-whatsapp-pronto.digicomdigital.com.br/refund-policy"
    },
    "status": "KNOWN_DIVERGENCE_NOT_THE_TARGET_CANDIDATE",
    "rootCause": "A migração anterior exigia draft_experience_json não nulo e ignorou o slot ativo publicado.",
    "repair": "2026-08-26-rigel-commercial-experience-v2-slot-repair atualiza a publicação v2 mesmo com rascunho nulo.",
    "physicalMysql57Validation": "Aprovada com slot sem rascunho, com rascunho, reaplicação e retomada após interrupção."
  },
  "creativeContract": {
    "contractVersion": "rigel-creative-contract-v4",
    "product": {
      "id": 9,
      "internalName": "Rigel",
      "commercialName": "Kit WhatsApp Pronto",
      "experimentId": 89,
      "commercialPlanId": 4,
      "channel": "DIRECT_ONE_TO_ONE",
      "audience": "Pequenos prestadores que atendem clientes pelo WhatsApp",
      "priceBrl": 349,
      "payment": "Pagamento único, sem recorrência",
      "format": "Implantação personalizada e assistida",
      "delivery": "Em até 48 horas após pagamento confirmado e briefing completo",
      "primaryCta": "Quero meu atendimento sob medida",
      "destination": "https://kit-whatsapp-pronto.digicomdigital.com.br"
    },
    "routeDecision": {
      "selected": "STATIC_CAROUSEL_PLUS_DETERMINISTIC_MOTION",
      "rationale": "O primeiro piloto usa 15 contatos diretos consentidos. Uma sequência curta preserva as provas integrais e legíveis; o movimento opcional demonstra o mecanismo com custo zero de provider e menor risco de parecer automação ou produto genérico.",
      "alternatives": [
        {
          "route": "GENERATIVE_VIDEO_WITH_AVATAR",
          "benefit": "Maior demonstração narrativa",
          "risk": "Custo, tempo e complexidade antes de qualquer resposta comercial",
          "effort": "ALTO",
          "decision": "DEFER"
        },
        {
          "route": "GENERIC_WHATSAPP_IMAGE",
          "benefit": "Produção rápida",
          "risk": "Não comprova Rigel e pode sugerir automação inexistente",
          "effort": "BAIXO",
          "decision": "REJECT"
        },
        {
          "route": "STATIC_CAROUSEL_PLUS_DETERMINISTIC_MOTION",
          "benefit": "Provas reais integrais, clareza no canal e custo externo zero",
          "risk": "A sequência exige navegar por seis cards",
          "effort": "MEDIO",
          "decision": "SELECT"
        }
      ]
    },
    "sourceProofs": [
      {
        "file": "rigel-tasting-response.png",
        "purpose": "PRODUCT_PROOF",
        "origin": "Resposta da PDE Platform local responsiva com mh_test=1 e cenário sintético",
        "rightsStatement": "Código e interface próprios do Marketing Hub; cenário de QA sem dados pessoais"
      },
      {
        "file": "rigel-tasting-question.png",
        "purpose": "PRODUCT_PROOF",
        "origin": "Pergunta da PDE Platform local responsiva com mh_test=1 e cenário sintético",
        "rightsStatement": "Código e interface próprios do Marketing Hub; cenário de QA sem dados pessoais"
      },
      {
        "file": "rigel-tasting-followups.png",
        "purpose": "PRODUCT_PROOF",
        "origin": "Três follow-ups da PDE Platform local responsiva com mh_test=1 e cenário sintético",
        "rightsStatement": "Código e interface próprios do Marketing Hub; cenário de QA sem dados pessoais"
      },
      {
        "file": "rigel-offer-proof.png",
        "purpose": "PRODUCT_PROOF",
        "origin": "PDE Platform local com contrato comercial de Rigel",
        "rightsStatement": "Código e interface próprios do Marketing Hub; oferta pertencente ao produto"
      }
    ],
    "formats": [
      {
        "id": "direct-carousel",
        "role": "FIRST_CONSENTED_CONTACT_SEQUENCE",
        "width": 1080,
        "height": 1350,
        "files": [
          "rigel-direct-card-1080x1350.png",
          "rigel-direct-response-1080x1350.png",
          "rigel-direct-question-1080x1350.png",
          "rigel-direct-followups-1080x1350.png",
          "rigel-direct-offer-1080x1350.png",
          "rigel-direct-conditions-1080x1350.png"
        ]
      },
      {
        "id": "vertical-demo",
        "role": "OPTIONAL_PRODUCT_DEMONSTRATION",
        "width": 1080,
        "height": 1920,
        "durationSeconds": 30,
        "file": "rigel-vertical-demo-1080x1920.mp4"
      }
    ],
    "copy": {
      "hook": "Seu atendimento no WhatsApp ainda depende do improviso?",
      "mechanism": "Transformamos suas situações em 10 a 20 respostas personalizadas, 5 a 10 perguntas e 3 a 5 follow-ups manuais.",
      "proof": "Interface real; exemplo fictício: uma resposta, uma pergunta e três follow-ups.",
      "offer": "Implantação personalizada e assistida do seu atendimento no WhatsApp",
      "price": "R$ 349 em pagamento único",
      "delivery": "Em até 48 horas após pagamento confirmado e briefing completo.",
      "control": "Você revisa antes de usar. Sem bot, disparo em massa ou envio automático.",
      "cta": "Quero meu atendimento sob medida"
    },
    "prohibitedClaims": [
      "vendas garantidas",
      "responde sozinho",
      "disparo em massa",
      "inteligência artificial atende por você",
      "entrega imediata",
      "depoimento de cliente"
    ],
    "futureMeasurement": {
      "denominator": "15 contatos diretos qualificados e consentidos",
      "primary": "3 pagamentos aprovados, atribuídos e entregues satisfatoriamente",
      "diagnostic": [
        "resposta ao contato",
        "clique no destino",
        "checkout iniciado"
      ],
      "notSales": [
        "arquivo criado",
        "parecer aprovado",
        "visualização local"
      ]
    }
  },
  "approvedCreativePackage": {
    "packageId": "2bf0a2a4ef3725da5258f41a04db9fd9816c2f300f1d68d3f2ab538498fc9cf2",
    "assetCount": 11,
    "imageCount": 10,
    "videoCount": 1,
    "assets": [
      {
        "id": 185,
        "assetUrl": "https://pub-37cb222fbfe5470da56cce789c5beec1.r2.dev/commercial-plans/visual-assets/2026/08/25/misc/182deed413e5-rigel-tasting-response.png",
        "mediaType": "IMAGE",
        "label": "rigel-tasting-response.png",
        "purpose": "PRODUCT_PROOF",
        "purposes": [
          "PRODUCT_PROOF"
        ],
        "origin": "Resposta da PDE Platform local responsiva com mh_test=1 e cenário sintético",
        "rightsStatement": "Código e interface próprios do Marketing Hub; cenário de QA sem dados pessoais",
        "contentSha256": "3449db73c29fb9eb901a82fb50833bd1c6a03be4128aa1bf3647df4b120a3916",
        "creativePackageId": "2bf0a2a4ef3725da5258f41a04db9fd9816c2f300f1d68d3f2ab538498fc9cf2",
        "versionNumber": 1,
        "status": "APPROVED",
        "sourceAssetId": null,
        "agentReviewStatus": "APPROVED",
        "agentReviewSummary": "Psique está favorável e o pacote demonstra corretamente o Kit WhatsApp Pronto para contato direto individual consentido. As provas do produto permanecem integrais, sem redesenho material; promessa, implantação assistida, uso manual, preço de R$ 349, pagamento único, prazo condicionado, CTA e destino são coerentes. A aprovação limita-se ao LOCAL_QA e não autoriza publicação, gasto ou mudança do experimento para RUNNING.",
        "customerReviewStatus": "APPROVED",
        "customerReviewSummary": "Nos dois primeiros segundos, percebo uma solução para parar de improvisar no atendimento pelo WhatsApp. Entendo que receberei um atendimento personalizado — respostas, perguntas e follow-ups preparados para eu revisar e usar manualmente — e não um aplicativo, bot ou automação. A sequência leva com clareza à oferta de R$ 349, pagamento único, entrega em até 48 horas após pagamento e briefing, e à ação “Quero meu atendimento sob medida”.",
        "createdAt": "2026-08-25T21:33:14Z",
        "updatedAt": "2026-08-25T21:33:14Z",
        "localPath": "/root/ai-hub/src/ai-hub-b3b98477-9dae-4483-b305-094906c24189-f486lm/repo/.codex/attachments/rigel-approved-package/rigel-tasting-response.png"
      },
      {
        "id": 186,
        "assetUrl": "https://pub-37cb222fbfe5470da56cce789c5beec1.r2.dev/commercial-plans/visual-assets/2026/08/25/misc/17c1af1709af-rigel-tasting-question.png",
        "mediaType": "IMAGE",
        "label": "rigel-tasting-question.png",
        "purpose": "PRODUCT_PROOF",
        "purposes": [
          "PRODUCT_PROOF"
        ],
        "origin": "Pergunta da PDE Platform local responsiva com mh_test=1 e cenário sintético",
        "rightsStatement": "Código e interface próprios do Marketing Hub; cenário de QA sem dados pessoais",
        "contentSha256": "cc5e14de8766d2863a6e6f42239a74b3e36d6b52bac4f8cb1bfc2c9845f871a3",
        "creativePackageId": "2bf0a2a4ef3725da5258f41a04db9fd9816c2f300f1d68d3f2ab538498fc9cf2",
        "versionNumber": 1,
        "status": "APPROVED",
        "sourceAssetId": null,
        "agentReviewStatus": "APPROVED",
        "agentReviewSummary": "Psique está favorável e o pacote demonstra corretamente o Kit WhatsApp Pronto para contato direto individual consentido. As provas do produto permanecem integrais, sem redesenho material; promessa, implantação assistida, uso manual, preço de R$ 349, pagamento único, prazo condicionado, CTA e destino são coerentes. A aprovação limita-se ao LOCAL_QA e não autoriza publicação, gasto ou mudança do experimento para RUNNING.",
        "customerReviewStatus": "APPROVED",
        "customerReviewSummary": "Nos dois primeiros segundos, percebo uma solução para parar de improvisar no atendimento pelo WhatsApp. Entendo que receberei um atendimento personalizado — respostas, perguntas e follow-ups preparados para eu revisar e usar manualmente — e não um aplicativo, bot ou automação. A sequência leva com clareza à oferta de R$ 349, pagamento único, entrega em até 48 horas após pagamento e briefing, e à ação “Quero meu atendimento sob medida”.",
        "createdAt": "2026-08-25T21:33:15Z",
        "updatedAt": "2026-08-25T21:33:15Z",
        "localPath": "/root/ai-hub/src/ai-hub-b3b98477-9dae-4483-b305-094906c24189-f486lm/repo/.codex/attachments/rigel-approved-package/rigel-tasting-question.png"
      },
      {
        "id": 187,
        "assetUrl": "https://pub-37cb222fbfe5470da56cce789c5beec1.r2.dev/commercial-plans/visual-assets/2026/08/25/misc/f25cb72bf5e3-rigel-tasting-followups.png",
        "mediaType": "IMAGE",
        "label": "rigel-tasting-followups.png",
        "purpose": "PRODUCT_PROOF",
        "purposes": [
          "PRODUCT_PROOF"
        ],
        "origin": "Três follow-ups da PDE Platform local responsiva com mh_test=1 e cenário sintético",
        "rightsStatement": "Código e interface próprios do Marketing Hub; cenário de QA sem dados pessoais",
        "contentSha256": "81664e766ed091cb8b15d4679abb6c67175050ffa6db608a092af0ed39c28d1f",
        "creativePackageId": "2bf0a2a4ef3725da5258f41a04db9fd9816c2f300f1d68d3f2ab538498fc9cf2",
        "versionNumber": 1,
        "status": "APPROVED",
        "sourceAssetId": null,
        "agentReviewStatus": "APPROVED",
        "agentReviewSummary": "Psique está favorável e o pacote demonstra corretamente o Kit WhatsApp Pronto para contato direto individual consentido. As provas do produto permanecem integrais, sem redesenho material; promessa, implantação assistida, uso manual, preço de R$ 349, pagamento único, prazo condicionado, CTA e destino são coerentes. A aprovação limita-se ao LOCAL_QA e não autoriza publicação, gasto ou mudança do experimento para RUNNING.",
        "customerReviewStatus": "APPROVED",
        "customerReviewSummary": "Nos dois primeiros segundos, percebo uma solução para parar de improvisar no atendimento pelo WhatsApp. Entendo que receberei um atendimento personalizado — respostas, perguntas e follow-ups preparados para eu revisar e usar manualmente — e não um aplicativo, bot ou automação. A sequência leva com clareza à oferta de R$ 349, pagamento único, entrega em até 48 horas após pagamento e briefing, e à ação “Quero meu atendimento sob medida”.",
        "createdAt": "2026-08-25T21:33:15Z",
        "updatedAt": "2026-08-25T21:33:15Z",
        "localPath": "/root/ai-hub/src/ai-hub-b3b98477-9dae-4483-b305-094906c24189-f486lm/repo/.codex/attachments/rigel-approved-package/rigel-tasting-followups.png"
      },
      {
        "id": 188,
        "assetUrl": "https://pub-37cb222fbfe5470da56cce789c5beec1.r2.dev/commercial-plans/visual-assets/2026/08/25/misc/61511a55e5ee-rigel-offer-proof.png",
        "mediaType": "IMAGE",
        "label": "rigel-offer-proof.png",
        "purpose": "PRODUCT_PROOF",
        "purposes": [
          "PRODUCT_PROOF"
        ],
        "origin": "PDE Platform local com contrato comercial de Rigel",
        "rightsStatement": "Código e interface próprios do Marketing Hub; oferta pertencente ao produto",
        "contentSha256": "55c34aeb3447125a9653377efb88ccaa503d2659deac762ce1d4af69fdad286a",
        "creativePackageId": "2bf0a2a4ef3725da5258f41a04db9fd9816c2f300f1d68d3f2ab538498fc9cf2",
        "versionNumber": 1,
        "status": "APPROVED",
        "sourceAssetId": null,
        "agentReviewStatus": "APPROVED",
        "agentReviewSummary": "Psique está favorável e o pacote demonstra corretamente o Kit WhatsApp Pronto para contato direto individual consentido. As provas do produto permanecem integrais, sem redesenho material; promessa, implantação assistida, uso manual, preço de R$ 349, pagamento único, prazo condicionado, CTA e destino são coerentes. A aprovação limita-se ao LOCAL_QA e não autoriza publicação, gasto ou mudança do experimento para RUNNING.",
        "customerReviewStatus": "APPROVED",
        "customerReviewSummary": "Nos dois primeiros segundos, percebo uma solução para parar de improvisar no atendimento pelo WhatsApp. Entendo que receberei um atendimento personalizado — respostas, perguntas e follow-ups preparados para eu revisar e usar manualmente — e não um aplicativo, bot ou automação. A sequência leva com clareza à oferta de R$ 349, pagamento único, entrega em até 48 horas após pagamento e briefing, e à ação “Quero meu atendimento sob medida”.",
        "createdAt": "2026-08-25T21:33:16Z",
        "updatedAt": "2026-08-25T21:33:16Z",
        "localPath": "/root/ai-hub/src/ai-hub-b3b98477-9dae-4483-b305-094906c24189-f486lm/repo/.codex/attachments/rigel-approved-package/rigel-offer-proof.png"
      },
      {
        "id": 189,
        "assetUrl": "https://pub-37cb222fbfe5470da56cce789c5beec1.r2.dev/commercial-plans/visual-assets/2026/08/25/misc/4df8504cc54c-rigel-direct-card-1080x1350.png",
        "mediaType": "IMAGE",
        "label": "rigel-direct-card-1080x1350.png",
        "purpose": "ADS",
        "purposes": [
          "ADS",
          "SOCIAL"
        ],
        "origin": "Compositor determinístico versionado do Marketing Hub",
        "rightsStatement": "Código, interface e composição próprios; prova sintética sem dados pessoais",
        "contentSha256": "2d694e633e95d9e9db8aeb01e29fbb8b6e3f2ed9832ed11c9586b36437125a4c",
        "creativePackageId": "2bf0a2a4ef3725da5258f41a04db9fd9816c2f300f1d68d3f2ab538498fc9cf2",
        "versionNumber": 1,
        "status": "APPROVED",
        "sourceAssetId": null,
        "agentReviewStatus": "APPROVED",
        "agentReviewSummary": "Psique está favorável e o pacote demonstra corretamente o Kit WhatsApp Pronto para contato direto individual consentido. As provas do produto permanecem integrais, sem redesenho material; promessa, implantação assistida, uso manual, preço de R$ 349, pagamento único, prazo condicionado, CTA e destino são coerentes. A aprovação limita-se ao LOCAL_QA e não autoriza publicação, gasto ou mudança do experimento para RUNNING.",
        "customerReviewStatus": "APPROVED",
        "customerReviewSummary": "Nos dois primeiros segundos, percebo uma solução para parar de improvisar no atendimento pelo WhatsApp. Entendo que receberei um atendimento personalizado — respostas, perguntas e follow-ups preparados para eu revisar e usar manualmente — e não um aplicativo, bot ou automação. A sequência leva com clareza à oferta de R$ 349, pagamento único, entrega em até 48 horas após pagamento e briefing, e à ação “Quero meu atendimento sob medida”.",
        "createdAt": "2026-08-25T21:33:17Z",
        "updatedAt": "2026-08-25T21:33:17Z",
        "localPath": "/root/ai-hub/src/ai-hub-b3b98477-9dae-4483-b305-094906c24189-f486lm/repo/.codex/attachments/rigel-approved-package/rigel-direct-card-1080x1350.png"
      },
      {
        "id": 190,
        "assetUrl": "https://pub-37cb222fbfe5470da56cce789c5beec1.r2.dev/commercial-plans/visual-assets/2026/08/25/misc/1b177b96c053-rigel-direct-response-1080x1350.png",
        "mediaType": "IMAGE",
        "label": "rigel-direct-response-1080x1350.png",
        "purpose": "ADS",
        "purposes": [
          "ADS",
          "SOCIAL"
        ],
        "origin": "Compositor determinístico versionado do Marketing Hub",
        "rightsStatement": "Código, interface e composição próprios; prova sintética sem dados pessoais",
        "contentSha256": "e0cd4a89f4d9da3bed84f35f890b5f9f8641a95bbc49439d041d56d45762013e",
        "creativePackageId": "2bf0a2a4ef3725da5258f41a04db9fd9816c2f300f1d68d3f2ab538498fc9cf2",
        "versionNumber": 1,
        "status": "APPROVED",
        "sourceAssetId": null,
        "agentReviewStatus": "APPROVED",
        "agentReviewSummary": "Psique está favorável e o pacote demonstra corretamente o Kit WhatsApp Pronto para contato direto individual consentido. As provas do produto permanecem integrais, sem redesenho material; promessa, implantação assistida, uso manual, preço de R$ 349, pagamento único, prazo condicionado, CTA e destino são coerentes. A aprovação limita-se ao LOCAL_QA e não autoriza publicação, gasto ou mudança do experimento para RUNNING.",
        "customerReviewStatus": "APPROVED",
        "customerReviewSummary": "Nos dois primeiros segundos, percebo uma solução para parar de improvisar no atendimento pelo WhatsApp. Entendo que receberei um atendimento personalizado — respostas, perguntas e follow-ups preparados para eu revisar e usar manualmente — e não um aplicativo, bot ou automação. A sequência leva com clareza à oferta de R$ 349, pagamento único, entrega em até 48 horas após pagamento e briefing, e à ação “Quero meu atendimento sob medida”.",
        "createdAt": "2026-08-25T21:33:17Z",
        "updatedAt": "2026-08-25T21:33:17Z",
        "localPath": "/root/ai-hub/src/ai-hub-b3b98477-9dae-4483-b305-094906c24189-f486lm/repo/.codex/attachments/rigel-approved-package/rigel-direct-response-1080x1350.png"
      },
      {
        "id": 191,
        "assetUrl": "https://pub-37cb222fbfe5470da56cce789c5beec1.r2.dev/commercial-plans/visual-assets/2026/08/25/misc/97c770df0ea7-rigel-direct-question-1080x1350.png",
        "mediaType": "IMAGE",
        "label": "rigel-direct-question-1080x1350.png",
        "purpose": "ADS",
        "purposes": [
          "ADS",
          "SOCIAL"
        ],
        "origin": "Compositor determinístico versionado do Marketing Hub",
        "rightsStatement": "Código, interface e composição próprios; prova sintética sem dados pessoais",
        "contentSha256": "4305b9126209a8da7e06a2756e02d1695f3f7337b51049994330c691b15a41fd",
        "creativePackageId": "2bf0a2a4ef3725da5258f41a04db9fd9816c2f300f1d68d3f2ab538498fc9cf2",
        "versionNumber": 1,
        "status": "APPROVED",
        "sourceAssetId": null,
        "agentReviewStatus": "APPROVED",
        "agentReviewSummary": "Psique está favorável e o pacote demonstra corretamente o Kit WhatsApp Pronto para contato direto individual consentido. As provas do produto permanecem integrais, sem redesenho material; promessa, implantação assistida, uso manual, preço de R$ 349, pagamento único, prazo condicionado, CTA e destino são coerentes. A aprovação limita-se ao LOCAL_QA e não autoriza publicação, gasto ou mudança do experimento para RUNNING.",
        "customerReviewStatus": "APPROVED",
        "customerReviewSummary": "Nos dois primeiros segundos, percebo uma solução para parar de improvisar no atendimento pelo WhatsApp. Entendo que receberei um atendimento personalizado — respostas, perguntas e follow-ups preparados para eu revisar e usar manualmente — e não um aplicativo, bot ou automação. A sequência leva com clareza à oferta de R$ 349, pagamento único, entrega em até 48 horas após pagamento e briefing, e à ação “Quero meu atendimento sob medida”.",
        "createdAt": "2026-08-25T21:33:19Z",
        "updatedAt": "2026-08-25T21:33:19Z",
        "localPath": "/root/ai-hub/src/ai-hub-b3b98477-9dae-4483-b305-094906c24189-f486lm/repo/.codex/attachments/rigel-approved-package/rigel-direct-question-1080x1350.png"
      },
      {
        "id": 192,
        "assetUrl": "https://pub-37cb222fbfe5470da56cce789c5beec1.r2.dev/commercial-plans/visual-assets/2026/08/25/misc/3d582eb6cc1d-rigel-direct-followups-1080x1350.png",
        "mediaType": "IMAGE",
        "label": "rigel-direct-followups-1080x1350.png",
        "purpose": "ADS",
        "purposes": [
          "ADS",
          "SOCIAL"
        ],
        "origin": "Compositor determinístico versionado do Marketing Hub",
        "rightsStatement": "Código, interface e composição próprios; prova sintética sem dados pessoais",
        "contentSha256": "a2bef27ca0dac0bbc055b285d1224e4e9fc1a649e3d7dee802030a9f70b1a890",
        "creativePackageId": "2bf0a2a4ef3725da5258f41a04db9fd9816c2f300f1d68d3f2ab538498fc9cf2",
        "versionNumber": 1,
        "status": "APPROVED",
        "sourceAssetId": null,
        "agentReviewStatus": "APPROVED",
        "agentReviewSummary": "Psique está favorável e o pacote demonstra corretamente o Kit WhatsApp Pronto para contato direto individual consentido. As provas do produto permanecem integrais, sem redesenho material; promessa, implantação assistida, uso manual, preço de R$ 349, pagamento único, prazo condicionado, CTA e destino são coerentes. A aprovação limita-se ao LOCAL_QA e não autoriza publicação, gasto ou mudança do experimento para RUNNING.",
        "customerReviewStatus": "APPROVED",
        "customerReviewSummary": "Nos dois primeiros segundos, percebo uma solução para parar de improvisar no atendimento pelo WhatsApp. Entendo que receberei um atendimento personalizado — respostas, perguntas e follow-ups preparados para eu revisar e usar manualmente — e não um aplicativo, bot ou automação. A sequência leva com clareza à oferta de R$ 349, pagamento único, entrega em até 48 horas após pagamento e briefing, e à ação “Quero meu atendimento sob medida”.",
        "createdAt": "2026-08-25T21:33:20Z",
        "updatedAt": "2026-08-25T21:33:20Z",
        "localPath": "/root/ai-hub/src/ai-hub-b3b98477-9dae-4483-b305-094906c24189-f486lm/repo/.codex/attachments/rigel-approved-package/rigel-direct-followups-1080x1350.png"
      },
      {
        "id": 193,
        "assetUrl": "https://pub-37cb222fbfe5470da56cce789c5beec1.r2.dev/commercial-plans/visual-assets/2026/08/25/misc/95d388db7ea1-rigel-direct-offer-1080x1350.png",
        "mediaType": "IMAGE",
        "label": "rigel-direct-offer-1080x1350.png",
        "purpose": "ADS",
        "purposes": [
          "ADS",
          "SOCIAL"
        ],
        "origin": "Compositor determinístico versionado do Marketing Hub",
        "rightsStatement": "Código, interface e composição próprios; prova sintética sem dados pessoais",
        "contentSha256": "07c7fddd749c9761299ffa6eb2e0d1563cefdf5193f3c4eccd239a20a5aa54db",
        "creativePackageId": "2bf0a2a4ef3725da5258f41a04db9fd9816c2f300f1d68d3f2ab538498fc9cf2",
        "versionNumber": 1,
        "status": "APPROVED",
        "sourceAssetId": null,
        "agentReviewStatus": "APPROVED",
        "agentReviewSummary": "Psique está favorável e o pacote demonstra corretamente o Kit WhatsApp Pronto para contato direto individual consentido. As provas do produto permanecem integrais, sem redesenho material; promessa, implantação assistida, uso manual, preço de R$ 349, pagamento único, prazo condicionado, CTA e destino são coerentes. A aprovação limita-se ao LOCAL_QA e não autoriza publicação, gasto ou mudança do experimento para RUNNING.",
        "customerReviewStatus": "APPROVED",
        "customerReviewSummary": "Nos dois primeiros segundos, percebo uma solução para parar de improvisar no atendimento pelo WhatsApp. Entendo que receberei um atendimento personalizado — respostas, perguntas e follow-ups preparados para eu revisar e usar manualmente — e não um aplicativo, bot ou automação. A sequência leva com clareza à oferta de R$ 349, pagamento único, entrega em até 48 horas após pagamento e briefing, e à ação “Quero meu atendimento sob medida”.",
        "createdAt": "2026-08-25T21:33:21Z",
        "updatedAt": "2026-08-25T21:33:21Z",
        "localPath": "/root/ai-hub/src/ai-hub-b3b98477-9dae-4483-b305-094906c24189-f486lm/repo/.codex/attachments/rigel-approved-package/rigel-direct-offer-1080x1350.png"
      },
      {
        "id": 194,
        "assetUrl": "https://pub-37cb222fbfe5470da56cce789c5beec1.r2.dev/commercial-plans/visual-assets/2026/08/25/misc/c98772fc4228-rigel-direct-conditions-1080x1350.png",
        "mediaType": "IMAGE",
        "label": "rigel-direct-conditions-1080x1350.png",
        "purpose": "ADS",
        "purposes": [
          "ADS",
          "SOCIAL"
        ],
        "origin": "Compositor determinístico versionado do Marketing Hub",
        "rightsStatement": "Código, interface e composição próprios; prova sintética sem dados pessoais",
        "contentSha256": "d4fafda4637e242b4e2a3c22ed133dde3c832785a5aac0f59f015b3c012e2dd2",
        "creativePackageId": "2bf0a2a4ef3725da5258f41a04db9fd9816c2f300f1d68d3f2ab538498fc9cf2",
        "versionNumber": 1,
        "status": "APPROVED",
        "sourceAssetId": null,
        "agentReviewStatus": "APPROVED",
        "agentReviewSummary": "Psique está favorável e o pacote demonstra corretamente o Kit WhatsApp Pronto para contato direto individual consentido. As provas do produto permanecem integrais, sem redesenho material; promessa, implantação assistida, uso manual, preço de R$ 349, pagamento único, prazo condicionado, CTA e destino são coerentes. A aprovação limita-se ao LOCAL_QA e não autoriza publicação, gasto ou mudança do experimento para RUNNING.",
        "customerReviewStatus": "APPROVED",
        "customerReviewSummary": "Nos dois primeiros segundos, percebo uma solução para parar de improvisar no atendimento pelo WhatsApp. Entendo que receberei um atendimento personalizado — respostas, perguntas e follow-ups preparados para eu revisar e usar manualmente — e não um aplicativo, bot ou automação. A sequência leva com clareza à oferta de R$ 349, pagamento único, entrega em até 48 horas após pagamento e briefing, e à ação “Quero meu atendimento sob medida”.",
        "createdAt": "2026-08-25T21:33:21Z",
        "updatedAt": "2026-08-25T21:33:21Z",
        "localPath": "/root/ai-hub/src/ai-hub-b3b98477-9dae-4483-b305-094906c24189-f486lm/repo/.codex/attachments/rigel-approved-package/rigel-direct-conditions-1080x1350.png"
      },
      {
        "id": 195,
        "assetUrl": "https://pub-37cb222fbfe5470da56cce789c5beec1.r2.dev/commercial-plans/visual-assets/2026/08/25/misc/a6fab2beecfa-rigel-vertical-demo-1080x1920.mp4",
        "mediaType": "VIDEO",
        "label": "rigel-vertical-demo-1080x1920.mp4",
        "purpose": "ADS",
        "purposes": [
          "ADS",
          "SOCIAL"
        ],
        "origin": "Compositor determinístico versionado do Marketing Hub",
        "rightsStatement": "Código, interface e composição próprios; prova sintética sem dados pessoais",
        "contentSha256": "53ccd0eefb43aad1061a3bd873c9f9a46ea9b6ad454b24c343f81ebaacc54b2d",
        "creativePackageId": "2bf0a2a4ef3725da5258f41a04db9fd9816c2f300f1d68d3f2ab538498fc9cf2",
        "versionNumber": 1,
        "status": "APPROVED",
        "sourceAssetId": null,
        "agentReviewStatus": "APPROVED",
        "agentReviewSummary": "Psique está favorável e o pacote demonstra corretamente o Kit WhatsApp Pronto para contato direto individual consentido. As provas do produto permanecem integrais, sem redesenho material; promessa, implantação assistida, uso manual, preço de R$ 349, pagamento único, prazo condicionado, CTA e destino são coerentes. A aprovação limita-se ao LOCAL_QA e não autoriza publicação, gasto ou mudança do experimento para RUNNING.",
        "customerReviewStatus": "APPROVED",
        "customerReviewSummary": "Nos dois primeiros segundos, percebo uma solução para parar de improvisar no atendimento pelo WhatsApp. Entendo que receberei um atendimento personalizado — respostas, perguntas e follow-ups preparados para eu revisar e usar manualmente — e não um aplicativo, bot ou automação. A sequência leva com clareza à oferta de R$ 349, pagamento único, entrega em até 48 horas após pagamento e briefing, e à ação “Quero meu atendimento sob medida”.",
        "createdAt": "2026-08-25T21:33:22Z",
        "updatedAt": "2026-08-25T21:33:22Z",
        "localPath": "/root/ai-hub/src/ai-hub-b3b98477-9dae-4483-b305-094906c24189-f486lm/repo/.codex/attachments/rigel-approved-package/rigel-vertical-demo-1080x1920.mp4"
      }
    ],
    "videoProbe": {
      "programs": [],
      "streams": [
        {
          "codec_name": "h264",
          "width": 1080,
          "height": 1920,
          "r_frame_rate": "30/1"
        }
      ],
      "format": {
        "duration": "30.000000"
      }
    },
    "videoPlayback": "Aprovado em iPhone 15 Pro e Pixel 7: H.264, 1080x1920, 30s, reprodução iniciada e readyState 4."
  },
  "approvedLandingVisualAssets": [
    {
      "assetId": 185,
      "assetUrl": "https://pub-37cb222fbfe5470da56cce789c5beec1.r2.dev/commercial-plans/visual-assets/2026/08/25/misc/182deed413e5-rigel-tasting-response.png",
      "label": "rigel-tasting-response.png",
      "version": 1,
      "status": "APPROVED",
      "agentReviewStatus": "APPROVED",
      "requiredUsage": "PRESERVE_EXACT_FILE_NO_REDRAW"
    },
    {
      "assetId": 186,
      "assetUrl": "https://pub-37cb222fbfe5470da56cce789c5beec1.r2.dev/commercial-plans/visual-assets/2026/08/25/misc/17c1af1709af-rigel-tasting-question.png",
      "label": "rigel-tasting-question.png",
      "version": 1,
      "status": "APPROVED",
      "agentReviewStatus": "APPROVED",
      "requiredUsage": "PRESERVE_EXACT_FILE_NO_REDRAW"
    },
    {
      "assetId": 187,
      "assetUrl": "https://pub-37cb222fbfe5470da56cce789c5beec1.r2.dev/commercial-plans/visual-assets/2026/08/25/misc/f25cb72bf5e3-rigel-tasting-followups.png",
      "label": "rigel-tasting-followups.png",
      "version": 1,
      "status": "APPROVED",
      "agentReviewStatus": "APPROVED",
      "requiredUsage": "PRESERVE_EXACT_FILE_NO_REDRAW"
    },
    {
      "assetId": 188,
      "assetUrl": "https://pub-37cb222fbfe5470da56cce789c5beec1.r2.dev/commercial-plans/visual-assets/2026/08/25/misc/61511a55e5ee-rigel-offer-proof.png",
      "label": "rigel-offer-proof.png",
      "version": 1,
      "status": "APPROVED",
      "agentReviewStatus": "APPROVED",
      "requiredUsage": "PRESERVE_EXACT_FILE_NO_REDRAW"
    }
  ],
  "candidateHtml": "<!doctype html>\n<html lang=\"pt-BR\">\n<head>\n  <meta charset=\"utf-8\">\n  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n  <meta name=\"description\" content=\"Atendimento de WhatsApp personalizado e revisado, com respostas, perguntas, follow-ups e regras prontas em até 48 horas após pagamento e briefing mínimo.\">\n  <title>Kit WhatsApp Pronto — atendimento sob medida</title>\n  <style>\n    :root{color-scheme:light;--ink:#17201c;--muted:#59665f;--paper:#fbfaf6;--surface:#ffffff;--soft:#eef4ee;--line:#dce4dd;--brand:#176b4b;--brand-dark:#0d4f37;--accent:#e6b85c;--wa:#25d366;--shadow:0 18px 50px rgba(23,32,28,.10);--radius:24px;--max:1160px}\n    *{box-sizing:border-box}html{scroll-behavior:smooth}body{margin:0;background:var(--paper);color:var(--ink);font-family:Inter,ui-sans-serif,system-ui,-apple-system,BlinkMacSystemFont,\"Segoe UI\",sans-serif;font-size:17px;line-height:1.65;-webkit-font-smoothing:antialiased}img{display:block;max-width:100%;height:auto}a{color:inherit}:focus-visible{outline:3px solid var(--accent);outline-offset:4px}.skip{position:absolute;left:16px;top:-80px;z-index:20;padding:10px 14px;background:var(--ink);color:#fff;border-radius:8px}.skip:focus{top:16px}.shell{width:min(calc(100% - 40px),var(--max));margin-inline:auto}.eyebrow{display:inline-flex;align-items:center;gap:8px;margin:0 0 18px;padding:7px 12px;border:1px solid #c9dbcd;border-radius:999px;background:#f3f8f3;color:var(--brand-dark);font-size:.78rem;font-weight:800;letter-spacing:.08em;text-transform:uppercase}.dot{width:8px;height:8px;border-radius:50%;background:var(--wa);box-shadow:0 0 0 5px rgba(37,211,102,.12)}h1,h2,h3{margin:0;line-height:1.08;letter-spacing:-.035em;text-wrap:balance}h1{font-size:clamp(2.5rem,6.7vw,5.35rem);max-width:850px}h2{font-size:clamp(2rem,4.4vw,3.55rem)}h3{font-size:clamp(1.15rem,2vw,1.45rem)}p{margin:0}.lead{font-size:clamp(1.08rem,2vw,1.28rem);color:var(--muted);max-width:690px}.fine{font-size:.86rem;color:var(--muted)}.nav{border-bottom:1px solid rgba(220,228,221,.85);background:rgba(251,250,246,.94)}.nav-inner{min-height:72px;display:flex;align-items:center;justify-content:space-between;gap:18px}.brand{font-size:.98rem;font-weight:900;letter-spacing:-.02em}.brand span{color:var(--brand)}.nav-note{font-size:.84rem;color:var(--muted);text-align:right}.hero{padding:clamp(56px,9vw,110px) 0 72px;overflow:hidden}.hero-grid{display:grid;grid-template-columns:minmax(0,1.08fr) minmax(340px,.72fr);gap:60px;align-items:center}.hero-copy strong{color:var(--brand)}.hero .lead{margin-top:24px}.hero-actions{display:flex;flex-wrap:wrap;align-items:center;gap:16px;margin-top:32px}.button{display:inline-flex;min-height:56px;align-items:center;justify-content:center;padding:14px 22px;border-radius:14px;background:var(--brand);color:#fff;text-decoration:none;font-weight:850;box-shadow:0 10px 24px rgba(23,107,75,.19);transition:background .2s ease,transform .2s ease}.button:hover{background:var(--brand-dark);transform:translateY(-1px)}.button:active{transform:translateY(0)}.micro{display:flex;align-items:center;gap:9px;color:var(--muted);font-size:.9rem}.micro svg{flex:0 0 auto;color:var(--brand)}.hero-card{position:relative;padding:16px;border:1px solid var(--line);border-radius:30px;background:var(--surface);box-shadow:var(--shadow)}.hero-card:before{content:\"\";position:absolute;z-index:-1;inset:-18px -24px auto auto;width:180px;height:180px;border-radius:50%;background:#dff1e5;filter:blur(2px)}.hero-card img{width:100%;border-radius:19px;border:1px solid #e6ebe7}.proof-caption{display:flex;justify-content:space-between;gap:14px;padding:14px 5px 2px;font-size:.8rem;color:var(--muted)}.proof-caption strong{color:var(--brand-dark)}.trust-row{display:grid;grid-template-columns:repeat(3,1fr);gap:1px;margin-top:42px;border:1px solid var(--line);border-radius:18px;overflow:hidden;background:var(--line);max-width:760px}.trust-item{padding:16px 18px;background:var(--surface)}.trust-item strong{display:block;font-size:.98rem}.trust-item span{display:block;color:var(--muted);font-size:.78rem}section{padding:clamp(72px,9vw,112px) 0}.section-head{display:grid;grid-template-columns:minmax(0,.8fr) minmax(320px,.55fr);gap:70px;align-items:end;margin-bottom:48px}.section-head .lead{justify-self:end}.demo{background:var(--ink);color:#fff}.demo .eyebrow{background:rgba(255,255,255,.08);border-color:rgba(255,255,255,.18);color:#ccebd8}.demo .lead,.demo .fine{color:#bac5bf}.demo-grid{display:grid;grid-template-columns:minmax(300px,.72fr) minmax(0,1.1fr);gap:64px;align-items:start}.sticky{position:sticky;top:28px}.demo-intro .lead{margin-top:22px}.demo-note{margin-top:30px;padding:18px;border-left:3px solid var(--accent);background:rgba(255,255,255,.055);border-radius:0 12px 12px 0}.sequence{display:grid;gap:14px}.message{width:min(100%,620px);padding:19px 21px;border:1px solid rgba(255,255,255,.12);border-radius:18px;background:#25312b;box-shadow:0 10px 26px rgba(0,0,0,.13)}.message:nth-child(even){margin-left:auto;background:#194c37}.message-tag{display:flex;align-items:center;gap:9px;margin-bottom:8px;color:#a7d8bb;font-size:.75rem;font-weight:800;text-transform:uppercase;letter-spacing:.08em}.message p{font-size:.97rem}.message small{display:block;margin-top:8px;color:#aeb9b3;font-size:.76rem}.sequence-result{margin-top:16px;padding:22px;border:1px solid rgba(230,184,92,.35);border-radius:18px;background:rgba(230,184,92,.09)}.sequence-result strong{color:#f1d79f}.gallery{background:var(--soft)}.gallery-grid{display:grid;grid-template-columns:minmax(0,1.2fr) minmax(280px,.8fr);gap:18px;align-items:start}.proof{margin:0;padding:12px;border:1px solid var(--line);border-radius:22px;background:var(--surface);box-shadow:0 12px 28px rgba(23,32,28,.06)}.proof:first-child{grid-column:1;grid-row:1}.proof:nth-child(2){grid-column:1;grid-row:2}.proof:nth-child(3){grid-column:2;grid-row:1/span 2}.proof img{width:100%;border:1px solid #e8ece9;border-radius:14px}.proof figcaption{padding:14px 6px 6px}.proof figcaption strong{display:block}.proof figcaption span{color:var(--muted);font-size:.86rem}.scope-grid{display:grid;grid-template-columns:minmax(0,.86fr) minmax(330px,.62fr);gap:70px;align-items:start}.scope-copy .lead{margin-top:22px}.check-list{display:grid;gap:13px;margin:32px 0 0;padding:0;list-style:none}.check-list li{display:grid;grid-template-columns:26px 1fr;gap:12px}.check{width:24px;height:24px;display:grid;place-items:center;border-radius:50%;background:#dff2e5;color:var(--brand-dark);font-weight:900;font-size:.78rem}.scope-card{padding:28px;border:1px solid var(--line);border-radius:var(--radius);background:var(--surface);box-shadow:var(--shadow)}.scope-card .label{font-size:.74rem;font-weight:850;letter-spacing:.09em;text-transform:uppercase;color:var(--brand)}.scope-card h3{margin-top:8px}.scope-card p{margin-top:12px;color:var(--muted)}.not-kit{margin-top:22px;padding:18px;border-radius:15px;background:#fff8e7;border:1px solid #eed9a4;color:#5e4b24;font-size:.92rem}.process{background:#f2eee6}.steps{display:grid;grid-template-columns:repeat(2,1fr);gap:18px;margin-top:46px}.step{padding:26px;border:1px solid #ded8cc;border-radius:var(--radius);background:rgba(255,255,255,.65)}.step-number{display:grid;place-items:center;width:38px;height:38px;border-radius:12px;background:var(--ink);color:#fff;font-weight:900;margin-bottom:22px}.step p{margin-top:11px;color:var(--muted);font-size:.94rem}.deadline{margin-top:24px;padding:18px 22px;border-radius:16px;background:var(--surface);border:1px solid #ded8cc;text-align:center;font-weight:750}.offer{background:var(--brand-dark);color:#fff}.offer-grid{display:grid;grid-template-columns:minmax(0,.9fr) minmax(330px,.58fr);gap:70px;align-items:center}.offer .lead{margin-top:22px;color:#c8ddd2}.truth{margin-top:26px;color:#b8cfc3;font-size:.9rem;max-width:620px}.price-card{padding:32px;border-radius:28px;background:#fff;color:var(--ink);box-shadow:0 25px 70px rgba(0,0,0,.23)}.price-card .overline{font-size:.76rem;font-weight:850;letter-spacing:.09em;text-transform:uppercase;color:var(--brand)}.price{display:flex;align-items:flex-start;gap:5px;margin:10px 0 2px;font-weight:950;letter-spacing:-.06em}.price .currency{font-size:1.1rem;margin-top:14px}.price .amount{font-size:4.6rem;line-height:1}.price-note{color:var(--muted);font-size:.88rem}.price-card .button{width:100%;margin-top:24px}.secure{display:flex;align-items:center;justify-content:center;gap:8px;margin-top:14px;color:var(--muted);font-size:.78rem;text-align:center}.faq-list{display:grid;gap:12px;margin-top:44px}details{border:1px solid var(--line);border-radius:16px;background:var(--surface)}summary{cursor:pointer;padding:20px 22px;font-weight:820;list-style:none}summary::-webkit-details-marker{display:none}summary:after{content:\"+\";float:right;color:var(--brand);font-size:1.35rem;line-height:1}details[open] summary:after{content:\"−\"}details p{padding:0 22px 22px;color:var(--muted);max-width:850px}.final{padding-top:40px}.final-card{display:grid;grid-template-columns:minmax(0,1fr) auto;gap:32px;align-items:center;padding:clamp(28px,5vw,52px);border-radius:30px;background:var(--ink);color:#fff}.final-card p{margin-top:12px;color:#bdc8c2}footer{padding:30px 0 42px;color:var(--muted);font-size:.8rem}.footer-row{display:flex;justify-content:space-between;gap:28px;align-items:flex-start}.footer-copy{max-width:680px}.footer-copy strong{display:block;color:var(--ink);margin-bottom:5px}.footer-links{display:flex;flex-wrap:wrap;gap:10px 18px}.footer-links a{color:var(--brand-dark);font-weight:750}\n    @media(max-width:900px){.hero-grid,.demo-grid,.scope-grid,.offer-grid{grid-template-columns:1fr;gap:42px}.section-head{grid-template-columns:1fr;gap:18px}.section-head .lead{justify-self:start}.sticky{position:static}.gallery-grid{grid-template-columns:minmax(0,1.2fr) minmax(260px,.8fr)}.steps{grid-template-columns:1fr}.final-card{grid-template-columns:1fr}}\n    @media(max-width:620px){body{font-size:16px}.shell{width:min(calc(100% - 28px),var(--max))}.nav-inner{min-height:64px}.nav-note{max-width:150px;font-size:.72rem}.hero{padding-top:46px}.hero-actions{align-items:stretch}.hero-actions .button{width:100%}.trust-row{grid-template-columns:1fr}.trust-item{display:grid;grid-template-columns:1fr 1fr;align-items:center;gap:12px}.trust-item span{text-align:right}.gallery-grid{grid-template-columns:1fr}.proof:first-child,.proof:nth-child(2),.proof:nth-child(3){grid-column:auto;grid-row:auto}.proof:nth-child(3){width:min(100%,290px);justify-self:center}.price-card{padding:25px 20px}.price .amount{font-size:4rem}.final-card .button{width:100%}.proof-caption{flex-direction:column;gap:4px}.footer-row{flex-direction:column}.footer-links{display:grid;gap:8px}h1{font-size:clamp(2.35rem,13vw,3.7rem)}}\n    @media(prefers-reduced-motion:reduce){html{scroll-behavior:auto}*,*:before,*:after{transition:none!important}}\n  </style>\n</head>\n<body>\n  <a class=\"skip\" href=\"#conteudo\">Pular para o conteúdo</a>\n  <header class=\"nav\" aria-label=\"Cabeçalho\"><div class=\"shell nav-inner\"><div class=\"brand\">Kit WhatsApp <span>Pronto</span></div><div class=\"nav-note\">Implantação personalizada · R$ 349</div></div></header>\n  <main id=\"conteudo\">\n    <section class=\"hero\" aria-labelledby=\"hero-title\"><div class=\"shell hero-grid\"><div class=\"hero-copy\"><p class=\"eyebrow\"><span class=\"dot\" aria-hidden=\"true\"></span>Conversa que avança</p><h1 id=\"hero-title\">Seu orçamento ficou no vácuo. <strong>O próximo passo não precisa ficar.</strong></h1><p class=\"lead\">Se você presta serviços e envia orçamentos pelo WhatsApp, receba um atendimento feito para o seu negócio — com respostas, perguntas, follow-ups e regras revisadas para saber o que mandar sem soar insistente.</p><div class=\"hero-actions\"><a class=\"button\" id=\"checkout-cta-primary\" href=\"https://www.mercadopago.com.br/checkout/v1/redirect?pref_id=133771061-472e4ef4-5d13-4122-831a-706d12435081\">Quero meu atendimento sob medida</a><span class=\"micro\">◷ Prévia em até 12h · entrega em até 48h*</span></div><div class=\"trust-row\" aria-label=\"Resumo da oferta\"><div class=\"trust-item\"><strong>Sob medida</strong><span>não é pacote genérico</span></div><div class=\"trust-item\"><strong>Revisado</strong><span>antes da entrega</span></div><div class=\"trust-item\"><strong>R$ 349</strong><span>pagamento no checkout</span></div></div><p class=\"fine\" style=\"margin-top:14px\">*Prazo contado após o pagamento confirmado e o briefing mínimo completo. O briefing leva cerca de 15 a 25 minutos e deve usar exemplos anonimizados.</p></div><figure class=\"hero-card\"><img src=\"https://pub-37cb222fbfe5470da56cce789c5beec1.r2.dev/commercial-plans/visual-assets/2026/08/25/misc/182deed413e5-rigel-tasting-response.png\" alt=\"Recorte demonstrativo de uma resposta de atendimento no WhatsApp\" decoding=\"async\" fetchpriority=\"high\"><figcaption class=\"proof-caption\"><strong>Recorte demonstrativo</strong><span>Amostra ilustrativa e não clicável</span></figcaption></figure></div></section>\n    <section class=\"demo\" aria-labelledby=\"demo-title\"><div class=\"shell demo-grid\"><div class=\"demo-intro sticky\"><p class=\"eyebrow\">Veja antes de decidir</p><h2 id=\"demo-title\">Uma retomada respeitosa tem ritmo, contexto e saída.</h2><p class=\"lead\">A lógica não é cobrar resposta. É facilitar uma decisão clara — inclusive quando a resposta for “agora não”.</p><div class=\"demo-note\"><strong>O que esta demonstração prova:</strong><p class=\"fine\">como uma sequência pode retomar contexto, qualificar a necessidade e propor um próximo passo sem prometer conversão.</p></div></div><div><div class=\"sequence\" aria-label=\"Exemplo de sequência de follow-up\"><article class=\"message\"><div class=\"message-tag\">Follow-up 1 · contexto</div><p>Oi, Ana. Passei para confirmar se você conseguiu ver o orçamento que enviei ontem. Ficou alguma dúvida sobre o que está incluído?</p><small>Abre espaço para uma objeção real, sem pressão.</small></article><article class=\"message\"><div class=\"message-tag\">Pergunta de qualificação</div><p>Para eu te orientar melhor: hoje pesa mais para você o prazo, a forma de pagamento ou ajustar o escopo?</p><small>Troca “e aí?” por uma pergunta fácil de responder.</small></article><article class=\"message\"><div class=\"message-tag\">Follow-up 2 · próximo passo</div><p>Se ainda fizer sentido, posso separar duas opções mais objetivas para você comparar. Quer que eu envie?</p><small>Pede permissão antes de avançar.</small></article><article class=\"message\"><div class=\"message-tag\">Follow-up 3 · encerramento respeitoso</div><p>Vou encerrar por aqui para não ocupar seu WhatsApp. Se quiser retomar depois, me diga “quero rever” e eu continuo do ponto em que paramos.</p><small>Preserva a relação e deixa uma saída simples.</small></article></div><div class=\"sequence-result\"><strong>Na sua entrega, essa lógica é adaptada.</strong> O tom, as perguntas, os intervalos e os próximos passos consideram o seu atendimento e o tipo de cliente que você recebe.</div></div></div></section>\n    <section class=\"gallery\" aria-labelledby=\"gallery-title\"><div class=\"shell\"><div class=\"section-head\"><div><p class=\"eyebrow\">Amostras do formato</p><h2 id=\"gallery-title\">Veja recortes demonstrativos da entrega.</h2></div><p class=\"lead\">As amostras abaixo são ilustrativas e não clicáveis. Elas demonstram respostas, perguntas, follow-ups e organização; a implantação completa é personalizada depois do briefing.</p></div><div class=\"gallery-grid\"><figure class=\"proof\"><img src=\"https://pub-37cb222fbfe5470da56cce789c5beec1.r2.dev/commercial-plans/visual-assets/2026/08/25/misc/17c1af1709af-rigel-tasting-question.png\" alt=\"Recorte demonstrativo de pergunta de qualificação para WhatsApp\" decoding=\"async\"><figcaption><strong>Perguntas que dão direção</strong><span>Recorte ilustrativo e não clicável para descobrir o que impede o próximo passo.</span></figcaption></figure><figure class=\"proof\"><img src=\"https://pub-37cb222fbfe5470da56cce789c5beec1.r2.dev/commercial-plans/visual-assets/2026/08/25/misc/f25cb72bf5e3-rigel-tasting-followups.png\" alt=\"Recorte demonstrativo de sequência de três follow-ups respeitosos\" decoding=\"async\"><figcaption><strong>Follow-ups com função</strong><span>Recorte ilustrativo e não clicável; cada contato tem objetivo e saída.</span></figcaption></figure><figure class=\"proof\"><img src=\"https://pub-37cb222fbfe5470da56cce789c5beec1.r2.dev/commercial-plans/visual-assets/2026/08/25/misc/61511a55e5ee-rigel-offer-proof.png\" alt=\"Recorte demonstrativo da organização da oferta personalizada\" decoding=\"async\"><figcaption><strong>Oferta explicada com clareza</strong><span>Recorte ilustrativo e não clicável do escopo e do próximo passo.</span></figcaption></figure></div></div></section>\n    <section aria-labelledby=\"scope-title\"><div class=\"shell scope-grid\"><div class=\"scope-copy\"><p class=\"eyebrow\">O que você recebe</p><h2 id=\"scope-title\">Um sistema de atendimento aplicado ao seu jeito de vender.</h2><p class=\"lead\">Após entender o seu negócio, organizamos as mensagens e as regras que ajudam você a conduzir conversas comerciais com menos improviso.</p><ul class=\"check-list\"><li><span class=\"check\" aria-hidden=\"true\">✓</span><span><strong>10 a 20 respostas personalizadas</strong><br><span class=\"fine\">Para dúvidas e momentos recorrentes do seu atendimento.</span></span></li><li><span class=\"check\" aria-hidden=\"true\">✓</span><span><strong>5 a 10 perguntas de qualificação</strong><br><span class=\"fine\">Para entender prioridade, objeção e intenção antes do próximo passo.</span></span></li><li><span class=\"check\" aria-hidden=\"true\">✓</span><span><strong>3 a 5 follow-ups manuais</strong><br><span class=\"fine\">Com objetivo, contexto e encerramento — sem disparo automático.</span></span></li><li><span class=\"check\" aria-hidden=\"true\">✓</span><span><strong>Regras de escalonamento</strong><br><span class=\"fine\">Quando responder, esperar, encerrar ou tratar o caso individualmente.</span></span></li><li><span class=\"check\" aria-hidden=\"true\">✓</span><span><strong>Guia, checklist e revisão humana</strong><br><span class=\"fine\">Pacote editável em área privada, acompanhado por sete materiais de apoio.</span></span></li></ul></div><aside class=\"scope-card\" aria-label=\"Diferença entre um kit genérico e a oferta personalizada\"><span class=\"label\">A diferença central</span><h3>Você não compra um arquivo igual para todo mundo.</h3><p>O briefing guiado leva cerca de 15 a 25 minutos e reúne serviços, dúvidas, políticas, tom e exemplos anonimizados. A entrega fica disponível para download e edição em uma área privada ligada ao e-mail da compra.</p><div class=\"not-kit\"><strong>Não é software, bot, integração, disparo ou assinatura.</strong> Também não inclui promessa de resposta, conversão, faturamento ou agenda cheia.</div></aside></div></section>\n    <section class=\"process\" aria-labelledby=\"process-title\"><div class=\"shell\"><div class=\"section-head\"><div><p class=\"eyebrow\">Do pagamento à entrega</p><h2 id=\"process-title\">Quatro passos, sem mistério.</h2></div><p class=\"lead\">Depois do pagamento, você acessa o briefing, valida uma prévia e recebe o pacote completo para uso manual.</p></div><div class=\"steps\"><article class=\"step\"><span class=\"step-number\">1</span><h3>Briefing guiado</h3><p>Após o pagamento, acesse com o e-mail da compra e complete em 15 a 25 minutos, usando exemplos sem dados pessoais.</p></article><article class=\"step\"><span class=\"step-number\">2</span><h3>Prévia para validar o tom</h3><p>Com a entrada completa, você recebe uma primeira sequência em até 12 horas para confirmar a direção.</p></article><article class=\"step\"><span class=\"step-number\">3</span><h3>Entrega completa</h3><p>Em até 48 horas, o pacote editável e revisado fica disponível para download na área privada.</p></article><article class=\"step\"><span class=\"step-number\">4</span><h3>Primeira aplicação</h3><p>Escolha um bloco pequeno, revise e use manualmente no atendimento real durante a primeira semana.</p></article></div><div class=\"deadline\">Prazo: até 48 horas após o pagamento confirmado <em>e</em> o briefing mínimo completo.</div></div></section>\n    <section class=\"offer\" aria-labelledby=\"offer-title\"><div class=\"shell offer-grid\"><div><p class=\"eyebrow\">Implantação personalizada</p><h2 id=\"offer-title\">Menos “o que eu mando agora?”. Mais próximo passo claro.</h2><p class=\"lead\">Após o pagamento confirmado e o briefing mínimo completo, em até 48 horas receba seu atendimento de WhatsApp personalizado e revisado: respostas, perguntas, follow-ups e regras prontas para conduzir cada conversa ao próximo passo com mais clareza e menos improviso.</p><p class=\"truth\">O serviço organiza sua comunicação. Não garante resposta, conversão, faturamento ou agenda cheia.</p></div><aside class=\"price-card\" aria-label=\"Preço e compra\"><span class=\"overline\">Kit WhatsApp Pronto · sob medida</span><div class=\"price\"><span class=\"currency\">R$</span><span class=\"amount\">349</span></div><p class=\"price-note\">Pagamento único, sem recorrência. Pacote personalizado, editável e revisado.</p><a class=\"button\" data-analytics-role=\"primary-checkout\" href=\"https://www.mercadopago.com.br/checkout/v1/redirect?pref_id=133771061-472e4ef4-5d13-4122-831a-706d12435081\">Quero meu atendimento sob medida</a><div class=\"secure\">▣ Checkout canônico do Mercado Pago</div><p class=\"fine\" style=\"margin-top:12px;text-align:center\">Ao comprar, você recebe o acesso ao briefing pelo e-mail da compra. Seus dados são usados somente para personalizar e entregar o serviço conforme a Política de Privacidade.</p></aside></div></section>\n    <section aria-labelledby=\"faq-title\"><div class=\"shell\"><p class=\"eyebrow\">Antes de comprar</p><h2 id=\"faq-title\">Dúvidas importantes</h2><div class=\"faq-list\"><details><summary>É um pacote de mensagens genéricas?</summary><p>Não. Os exemplos da página demonstram o método. A entrega é montada após o briefing mínimo e revisada para o contexto informado por você.</p></details><details><summary>Quando começa o prazo de até 48 horas?</summary><p>Quando as duas condições estiverem concluídas: pagamento confirmado e briefing mínimo completo. Se faltar informação essencial, o prazo ainda não começou.</p></details><details><summary>Isso garante que o cliente vai responder ou comprar?</summary><p>Não. O atendimento ajuda você a formular respostas, perguntas e próximos passos com mais clareza, mas não controla a decisão de outra pessoa e não promete conversão ou faturamento.</p></details><details><summary>O que preciso informar no briefing?</summary><p>O contexto mínimo do seu negócio e do atendimento: o que você oferece, quem costuma pedir orçamento, dúvidas recorrentes e como as conversas normalmente avançam ou travam.</p></details><details><summary>Preciso usar as mensagens palavra por palavra?</summary><p>Não. Você recebe uma base organizada e regras de uso para adaptar naturalmente ao contexto de cada conversa.</p></details><details><summary>Como recebo e edito o material?</summary><p>O pacote completo fica disponível para download em uma área privada ligada ao e-mail da compra. Os materiais são editáveis e foram feitos para aplicação manual no WhatsApp.</p></details><details><summary>Como meus dados são usados?</summary><p>Use exemplos anonimizados no briefing. As informações comerciais fornecidas são usadas somente para personalizar, revisar e entregar o serviço, conforme os termos e a política de privacidade.</p></details></div></div></section>\n    <section class=\"final\" aria-labelledby=\"final-title\"><div class=\"shell final-card\"><div><h2 id=\"final-title\">Seu próximo follow-up pode começar com uma regra clara.</h2><p>Implantação personalizada e revisada por R$ 349, entregue em até 48 horas após pagamento e briefing mínimo.</p></div><a class=\"button\" data-analytics-role=\"primary-checkout\" href=\"https://www.mercadopago.com.br/checkout/v1/redirect?pref_id=133771061-472e4ef4-5d13-4122-831a-706d12435081\">Quero meu atendimento sob medida</a></div></section>\n  </main>\n  <footer><div class=\"shell footer-row\"><div class=\"footer-copy\"><strong>PAULO ALEXANDRE LOPES FORESTIERI INFORMATICA · CNPJ 25.215.414/0001-69</strong><span>Rua Antonio Basilio, 204, apto 805 · Tijuca · Rio de Janeiro/RJ · CEP 20511-190<br>Suporte: <a href=\"mailto:contato@digicomdigital.com.br\">contato@digicomdigital.com.br</a></span></div><nav class=\"footer-links\" aria-label=\"Informações legais\"><a href=\"https://kit-whatsapp-pronto.digicomdigital.com.br/terms\" target=\"_blank\" rel=\"noopener\">Termos</a><a href=\"https://kit-whatsapp-pronto.digicomdigital.com.br/privacy\" target=\"_blank\" rel=\"noopener\">Privacidade</a><a href=\"https://kit-whatsapp-pronto.digicomdigital.com.br/refund-policy\" target=\"_blank\" rel=\"noopener\">Cancelamento e reembolso</a></nav></div></footer>\n</body>\n</html>\n",
  "browserAudit": [
    {
      "device": "desktop",
      "viewportWidth": 1440,
      "documentWidth": 1440,
      "imageCount": 4,
      "brokenImages": [],
      "checkoutCount": 3,
      "checkoutTargets": [
        "https://www.mercadopago.com.br/checkout/v1/redirect?pref_id=133771061-472e4ef4-5d13-4122-831a-706d12435081"
      ],
      "primaryCheckoutIdCount": 1,
      "analyticsCheckoutCount": 2,
      "placeholderLinks": 0,
      "scripts": 0,
      "forms": 0,
      "h1Count": 1,
      "policyLinks": [
        {
          "href": "https://kit-whatsapp-pronto.digicomdigital.com.br/terms",
          "target": "_blank"
        },
        {
          "href": "https://kit-whatsapp-pronto.digicomdigital.com.br/privacy",
          "target": "_blank"
        },
        {
          "href": "https://kit-whatsapp-pronto.digicomdigital.com.br/refund-policy",
          "target": "_blank"
        }
      ],
      "consoleErrors": []
    },
    {
      "device": "iphone15pro",
      "viewportWidth": 393,
      "documentWidth": 393,
      "imageCount": 4,
      "brokenImages": [],
      "checkoutCount": 3,
      "checkoutTargets": [
        "https://www.mercadopago.com.br/checkout/v1/redirect?pref_id=133771061-472e4ef4-5d13-4122-831a-706d12435081"
      ],
      "primaryCheckoutIdCount": 1,
      "analyticsCheckoutCount": 2,
      "placeholderLinks": 0,
      "scripts": 0,
      "forms": 0,
      "h1Count": 1,
      "policyLinks": [
        {
          "href": "https://kit-whatsapp-pronto.digicomdigital.com.br/terms",
          "target": "_blank"
        },
        {
          "href": "https://kit-whatsapp-pronto.digicomdigital.com.br/privacy",
          "target": "_blank"
        },
        {
          "href": "https://kit-whatsapp-pronto.digicomdigital.com.br/refund-policy",
          "target": "_blank"
        }
      ],
      "consoleErrors": []
    },
    {
      "device": "pixel7",
      "viewportWidth": 412,
      "documentWidth": 412,
      "imageCount": 4,
      "brokenImages": [],
      "checkoutCount": 3,
      "checkoutTargets": [
        "https://www.mercadopago.com.br/checkout/v1/redirect?pref_id=133771061-472e4ef4-5d13-4122-831a-706d12435081"
      ],
      "primaryCheckoutIdCount": 1,
      "analyticsCheckoutCount": 2,
      "placeholderLinks": 0,
      "scripts": 0,
      "forms": 0,
      "h1Count": 1,
      "policyLinks": [
        {
          "href": "https://kit-whatsapp-pronto.digicomdigital.com.br/terms",
          "target": "_blank"
        },
        {
          "href": "https://kit-whatsapp-pronto.digicomdigital.com.br/privacy",
          "target": "_blank"
        },
        {
          "href": "https://kit-whatsapp-pronto.digicomdigital.com.br/refund-policy",
          "target": "_blank"
        }
      ],
      "consoleErrors": []
    }
  ],
  "qualityReview": {
    "score": 90,
    "targetAudienceSpecificity": "high",
    "commercialReadiness": "excellent",
    "criteriaScores": {
      "firstFoldClarity": 9,
      "painResultMechanism": 9,
      "proofStrength": 9,
      "offerDesirability": 8,
      "ctaAndFormStrength": 9,
      "visualPremiumFeel": 9,
      "mobileDesktopExecution": 9
    },
    "blockingIssues": [],
    "improvementOpportunities": [
      "Testar uma variação da primeira dobra com uma cena ainda mais específica de perda de orçamento para aumentar identificação imediata sem alterar a promessa.",
      "Adicionar futuramente prova social verificável, como depoimento ou caso anonimizado com contexto, para reduzir ainda mais o risco percebido.",
      "Reforçar próximo ao preço para quais prestadores de serviço a personalização tende a gerar mais valor, ajudando o visitante a se autoselecionar."
    ],
    "recommendedRegeneration": [],
    "approvalRecommendation": "APPROVE_FOR_PUBLICATION"
  },
  "screenshotRoles": [
    {
      "viewport": "desktop",
      "role": "full-page",
      "path": "/root/ai-hub/src/ai-hub-b3b98477-9dae-4483-b305-094906c24189-f486lm/repo/.codex/attachments/rigel-r5-desktop-full.jpg"
    },
    {
      "viewport": "desktop",
      "role": "proof-section",
      "path": "/root/ai-hub/src/ai-hub-b3b98477-9dae-4483-b305-094906c24189-f486lm/repo/.codex/attachments/rigel-r5-desktop-proof.jpg"
    },
    {
      "viewport": "mobile",
      "role": "full-page",
      "path": "/root/ai-hub/src/ai-hub-b3b98477-9dae-4483-b305-094906c24189-f486lm/repo/.codex/attachments/rigel-r5-iphone15pro-full.jpg"
    },
    {
      "viewport": "mobile",
      "role": "proof-section",
      "path": "/root/ai-hub/src/ai-hub-b3b98477-9dae-4483-b305-094906c24189-f486lm/repo/.codex/attachments/rigel-r5-iphone15pro-proof.jpg"
    }
  ],
  "verifiedPolicyStatus": {
    "terms": 200,
    "privacy": 200,
    "refundPolicy": 200,
    "checkedAt": "2026-08-26"
  },
  "checkoutEvidence": {
    "canonicalContract": {
      "experimentId": 89,
      "priceBrl": 349,
      "billingModel": "ONE_TIME",
      "checkoutUrl": "https://www.mercadopago.com.br/checkout/v1/redirect?pref_id=133771061-472e4ef4-5d13-4122-831a-706d12435081",
      "supplierLegalName": "PAULO ALEXANDRE LOPES FORESTIERI INFORMATICA"
    },
    "localTestDouble": {
      "mode": "LOCAL_TEST_DOUBLE_NO_PROVIDER_NO_PAYMENT",
      "title": "Checkout local da Rigel",
      "text": "HOMOLOGAÇÃO LOCAL · TEST DOUBLE · nenhum pagamento possível Kit WhatsApp Pronto Implantação personalizada e assistida do atendimento no WhatsApp. R$ 349 Pagamento único, sem recorrência. Nenhuma cobrança adicional é criada neste ambiente local. Fornecedor: PAULO ALEXANDRE LOPES FORESTIERI INFORMATICA CNPJ: 25.215.414/0001-69 Destino protegido: www.mercadopago.com.br · preferência canônica do experimento 89",
      "forms": 0,
      "links": 0,
      "expected": {
        "experimentId": 89,
        "priceBrl": 349,
        "billingModel": "ONE_TIME",
        "providerHost": "www.mercadopago.com.br"
      }
    },
    "localEndToEnd": "A rota da landing abriu o test double em popup e confirmou produto, R$ 349, pagamento único, fornecedor e ausência de cobrança adicional; 12 jornadas passaram em desktop, iPhone 15 Pro e Pixel 7, com métricas mh_test segregadas.",
    "productionProviderReadOnlyAttempt": {
      "mode": "READ_ONLY_DIRECT_NAVIGATION_NO_CTA_NO_SUBMISSION",
      "requestedUrl": "https://www.mercadopago.com.br/checkout/v1/redirect?pref_id=133771061-472e4ef4-5d13-4122-831a-706d12435081",
      "finalUrl": "https://www.mercadopago.com.br/checkout/v1/redirect?pref_id=133771061-472e4ef4-5d13-4122-831a-706d12435081",
      "httpStatus": 403,
      "offerExpectation": {
        "productSlug": "kit-whatsapp-pronto",
        "experimentId": 89,
        "priceBrl": 349,
        "billingModel": "ONE_TIME",
        "supplierLegalName": "PAULO ALEXANDRE LOPES FORESTIERI INFORMATICA"
      },
      "rendered": {
        "title": "",
        "visibleText": "Mercado Libre - Donde comprar y vender de todo Hubo un error accediendo a esta pagina... Ir a la página principal",
        "forms": 0,
        "buttons": []
      },
      "consoleErrors": [
        "Failed to load resource: the server responded with a status of 403 ()",
        "Failed to load resource: the server responded with a status of 403 ()"
      ]
    },
    "productionPreflightRule": "O provedor respondeu 403 ao navegador automatizado. Após deploy, a pessoa operadora deve repetir a inspeção sem pagar; até isso ocorrer, o experimento permanece PLANNED."
  },
  "boundaries": [
    "A validação é local, segregada e não publica a landing.",
    "Nenhum CTA foi acionado e nenhum pagamento foi iniciado.",
    "As imagens são os quatro arquivos APPROVED do plano comercial, preservados sem redesenho.",
    "As amostras são fictícias, demonstrativas e não constituem depoimento, venda ou prova social.",
    "O experimento permanece PLANNED, sem campanha, contato, gasto, evento ou venda atribuída.",
    "A revisão não autoriza publicação, contato, campanha, gasto ou mudança para RUNNING.",
    "APPROVED neste parecer significa apenas que o lote local está pronto para PR/deploy; o avanço produtivo continua condicionado ao preflight pós-deploy pela tela.",
    "O subprocesso avaliado é geração de landing. O pacote criativo anterior já foi importado e aprovado; seus 11 arquivos reais e hashes estão anexados apenas para conferir continuidade."
  ]
}
