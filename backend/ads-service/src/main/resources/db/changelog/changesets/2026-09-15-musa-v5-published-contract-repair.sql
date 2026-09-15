-- Restaura o contrato histórico v5 sem ler o produto global mutável.

-- Fonte imutável: 2026-07-21-product-pde-experience-json.yaml

SET @musa_v5_legacy_contract = '{
  "slug": "metodo-musa-7-dias",
  "name": "Método MUSA - Experiência Guiada de 7 Dias",
  "promise": "Descubra o que sua imagem comunica sem intenção e monte em 7 dias uma presença mais elegante, marcante e coerente sem depender de luxo caro.",
  "audience": "Mulheres urbanas que querem se sentir mais marcantes, alinhadas e seguras usando escolhas acessíveis.",
  "priceLabel": "",
  "theme": {
    "primary": "#7a2444",
    "accent": "#d6a75c",
    "background": "#fff8f3",
    "imageUrl": "/assets/musa-cover.png"
  },
  "diagnostic": {
    "title": "Mapa de Presença MUSA",
    "intro": "Comece pelo momento do espelho: quando você está pronta, mas sente que sua imagem ainda não comunica a mulher que você quer ser vista como.",
    "questions": [
      "Quando você se arruma, o que mais incomoda no resultado final?",
      "Qual sinal você gostaria que sua imagem comunicasse com mais clareza?",
      "Em qual situação dos próximos 7 dias você quer parecer mais alinhada com quem você é?"
    ]
  },
  "missions": [
    {
      "id": "dia-1-ruido-visual",
      "day": 1,
      "title": "Ler o sinal que sua imagem comunica",
      "principle": "A presença cresce quando você identifica o sinal visual que mais distancia sua imagem da mulher que você quer transmitir.",
      "action": "Hoje você não vai tentar mudar tudo. Vista ou separe uma combinação real, olhe roupa, cabelo, pele, perfume e detalhe final, identifique o sinal que deixa sua imagem comum ou desalinhada e escolha uma microação para comunicar mais intenção.",
      "evidence": "Frase preenchida: hoje minha imagem comunica menos intenção quando...",
      "visualCue": "Compare a sensação antes/depois de remover ruído visual ou reforçar um sinal de presença."
    },
    {
      "id": "dia-2-assinatura",
      "day": 2,
      "title": "Criar sua assinatura simples",
      "principle": "Coerência repetida cria reconhecimento sem exigir roupa nova.",
      "action": "Defina 3 sinais que você quer repetir: acabamento do cabelo, cor-base, textura, perfume, acessório ou maquiagem leve.",
      "evidence": "Lista dos 3 sinais escolhidos.",
      "visualCue": "Monte um pequeno painel com seus sinais recorrentes."
    },
    {
      "id": "dia-3-base-acessivel",
      "day": 3,
      "title": "Usar o que já existe melhor",
      "principle": "A mudança fica mais viável quando começa pelo que já está no armário.",
      "action": "Separe 5 peças, 2 acessórios e 1 perfume que já podem sustentar a presença desejada.",
      "evidence": "Inventário simples dos itens reaproveitados.",
      "visualCue": "Organize os itens em uma combinação para uma saída real."
    },
    {
      "id": "dia-4-checklist-12-minutos",
      "day": 4,
      "title": "Fazer o acabamento de 12 minutos",
      "principle": "Rotinas curtas reduzem atrito e aumentam consistência.",
      "action": "Passe pelo checklist cabelo, pele, roupa, perfume, acessório e postura antes de sair.",
      "evidence": "Checklist marcado com o tempo gasto.",
      "visualCue": "Use uma escala de 1 a 5 para medir coerência final."
    },
    {
      "id": "dia-5-compra-inteligente",
      "day": 5,
      "title": "Segurar a compra que não resolve",
      "principle": "Compra boa é aquela que fortalece sua assinatura, não a que compensa insegurança momentânea.",
      "action": "Antes de comprar algo, responda se o item combina com seus 3 sinais de presença.",
      "evidence": "Decisão registrada: comprar, esperar ou descartar.",
      "visualCue": "Compare desejo imediato com utilidade real na sua rotina."
    },
    {
      "id": "dia-6-situacao-chave",
      "day": 6,
      "title": "Preparar sua entrada",
      "principle": "A presença fica mais forte quando é planejada para contextos reais.",
      "action": "Escolha uma ocasião e monte uma composição completa com intenção: roupa, cabelo, pele, perfume e detalhe final.",
      "evidence": "Plano da ocasião com roupa, cabelo, pele, perfume e detalhe final.",
      "visualCue": "Visualize a entrada no ambiente e ajuste o que estiver incoerente."
    },
    {
      "id": "dia-7-plano-pessoal",
      "day": 7,
      "title": "Fechar seu antes e depois",
      "principle": "A transformação continua quando vira padrão simples de repetição.",
      "action": "Monte seu plano de manutenção com sinais, checklist e regra anti-impulso.",
      "evidence": "Plano pessoal preenchido.",
      "visualCue": "Escolha um ritual semanal de 15 minutos para manter sua presença."
    }
  ],
  "supportMaterials": [
    {
      "title": "E-book Método MUSA",
      "type": "PDF",
      "description": "Guia de consulta para entender o método, ver exemplos e revisar sua semana.",
      "url": "/materials/metodo-musa-ebook.pdf"
    },
    {
      "title": "Experiência Guiada MUSA",
      "type": "HTML",
      "description": "Versão navegável da experiência para consultar a ordem, o diagnóstico e as missões de 7 dias.",
      "url": "/materials/experiencia-guiada-musa.html"
    },
    {
      "title": "Plano, Checklists e Templates",
      "type": "CSV",
      "description": "Planilha com a ordem de aplicação, critérios de conclusão e pontos de atenção de cada material.",
      "url": "/materials/plano-checklists-e-templates.csv"
    },
    {
      "title": "Mapa Visual MUSA",
      "type": "Infográfico",
      "description": "Resumo visual do método: coerência, redução de ruído e assinatura pessoal.",
      "url": "/materials/mapa-visual-musa.png"
    }
  ],
  "scientificEvidencePack": {
    "version": "musa-evidence-pack-v1",
    "principles": [
      "A roupa pode influenciar a forma como a pessoa se percebe e se comporta em uma situação.",
      "Escolhas de vestimenta, formalidade e acabamento participam da percepção social e dos primeiros julgamentos.",
      "Coerência visual, intenção e repetição de sinais podem reduzir ruído percebido e facilitar reconhecimento pessoal."
    ],
    "practicalApplications": [
      "Transformar princípios de cognição vestida e percepção social em microdecisões simples de roupa, cor, acabamento, postura e detalhe final.",
      "Orientar a cliente a usar o que já possui antes de comprar novas peças.",
      "Reforçar presença elegante como percepção e coerência, não como garantia universal de aprovação externa."
    ],
    "allowedLanguage": [
      "isso ajuda você a comunicar mais intenção",
      "pode reduzir ruído visual",
      "favorece uma presença mais coerente",
      "ajuda você a se sentir mais alinhada com a imagem que quer transmitir"
    ],
    "forbiddenClaims": [
      "garante elegância",
      "muda como todos vão te ver",
      "transforma sua personalidade",
      "efeito comprovado em qualquer pessoa",
      "substitui autoestima, terapia ou consultoria individual"
    ],
    "references": [
      {
        "authors": "Adam e Galinsky",
        "year": "2012",
        "title": "Enclothed cognition",
        "source": "Journal of Experimental Social Psychology",
        "doi": "10.1016/j.jesp.2012.02.008"
      },
      {
        "authors": "Slepian, Ferber, Gold e Rutchick",
        "year": "2015",
        "title": "The Cognitive Consequences of Formal Clothing",
        "source": "Social Psychological and Personality Science",
        "doi": "10.1177/1948550615579462"
      },
      {
        "authors": "Howlett, Pine, Orakcioglu e Fletcher",
        "year": "2013",
        "title": "The influence of clothing on first impressions",
        "source": "Journal of Fashion Marketing and Management",
        "doi": "10.1108/13612021311305128"
      },
      {
        "authors": "Hester e Hehman",
        "year": "2023",
        "title": "Dress is a Fundamental Component of Person Perception",
        "source": "Personality and Social Psychology Review",
        "doi": "10.1177/10888683231157961"
      }
    ]
  },
  "completionOffer": "Ao concluir os 7 dias, você pode continuar no Clube MUSA com novos desafios mensais de presença, estilo e autocuidado acessível."
}';

-- Fonte imutável: 2026-07-21-product-musa-pde-single-action-entry.yaml

SET @musa_v5_legacy_contract = JSON_SET(
         @musa_v5_legacy_contract,
         '$.diagnostic.intro',
         'Comece pelo espelho: descubra o primeiro ajuste para sua imagem comunicar mais intenção hoje, usando o que você já tem.',
         '$.diagnostic.questions',
         JSON_ARRAY('O que minha imagem comunica hoje?')
       );

-- Fonte imutável: 2026-07-21-product-musa-pde-experience-version.yaml

SET @musa_v5_legacy_contract = JSON_SET(
         @musa_v5_legacy_contract,
         '$.experienceVersion',
         'musa-pde-entry-v3',
         '$.funnelVersion',
         'musa-membership-funnel-v1'
       );

-- Fonte imutável: 2026-07-26-product-musa-pde-video-explainer-entry.yaml

SET @musa_v5_legacy_contract = JSON_SET(
         @musa_v5_legacy_contract,
         '$.experienceVersion',
         'musa-pde-entry-v5-video-explicativo',
         '$.funnelVersion',
         'musa-membership-funnel-v1',
         '$.diagnostic.intro',
         'Assista ao vídeo inicial, reconheça o ruído que aparece no espelho e receba o primeiro ajuste para sua imagem comunicar mais intenção hoje, usando o que você já tem.'
       );

-- Fonte imutável: 2026-07-28-musa-v6-approved-hero-video.yaml

SET @musa_v5_legacy_contract = JSON_SET(
         @musa_v5_legacy_contract,
         '$.heroVideos',
         JSON_ARRAY(
           JSON_OBJECT(
             'experienceVersion', 'musa-pde-entry-v6-video-motivacional',
             'placement', 'public_diagnostic_initial_explainer',
             'playbackUrl', '/assets/hls/musa-v6-microexperiencia-visivel/index.m3u8',
             'hlsPlaybackUrl', '/assets/hls/musa-v6-microexperiencia-visivel/index.m3u8',
             'posterUrl', '/assets/musa-editorial-presenca.png',
             'autoplay', true,
             'muted', true,
             'controls', true,
             'loop', false,
             'playsInline', true,
             'source', 'MARKETING_HUB_MANAGED_HLS',
             'assetId', 1935,
             'experimentVideoAssetId', 22,
             'salesVideoProfileId', 35,
             'salesVideoJobId', 20462,
             'reviewStatus', 'APPROVED',
             'status', 'READY'
           )
         )
       );

-- Fonte imutável: 2026-07-28-musa-v6-video-audio-mobile.yaml

SET @musa_v5_legacy_contract = JSON_REMOVE(
         JSON_SET(
           @musa_v5_legacy_contract,
           '$.heroVideos[0].autoplay', false,
           '$.heroVideos[0].muted', false,
           '$.heroVideos[0].controls', true,
           '$.heroVideos[0].loop', false,
           '$.heroVideos[0].playsInline', true
         ),
         '$.heroVideos[0].posterUrl'
       );

-- Layout v5 registrado no cadastro versionado em 30/07.

SET @musa_v5_legacy_contract = JSON_SET(@musa_v5_legacy_contract, '$.layoutKey', 'video-explicativo');

UPDATE pde_production_slot
   SET published_experience_json = @musa_v5_legacy_contract,
       published_by = 'migration:2026-09-15-v5-snapshot',
       published_at = CURRENT_TIMESTAMP,
       updated_at = CURRENT_TIMESTAMP
 WHERE product_slug = 'metodo-musa-7-dias'
   AND slot_code = 'v5'
   AND experience_version = 'musa-pde-entry-v5-video-explicativo'
   AND layout_key = 'video-explicativo'
   AND (published_experience_json IS NULL OR TRIM(published_experience_json) = '')
   AND published_at IS NULL
   AND published_by IS NULL;

SET @musa_v5_legacy_contract = NULL;
