# Prompts de campanha e landing baseados apenas em `niche_name` + resumos da hipótese

Este documento reúne prompts genéricos para os jobs de campanha e landing usando apenas os dados que vêm da hipótese:

- `niche_name`
- `pain_summary`
- `result_summary`
- `mechanism_summary`
- `proof_summary`
- `offer_summary`

A estrutura foi simplificada para remover completamente o campo `persona`, já que ele não existe no seu pipeline atual. A direção segue boas práticas de prompting da OpenAI: instruções claras no começo, contexto específico e formato de saída bem definido. Também ajuda a manter correspondência entre anúncio e landing, algo importante para a experiência pós-clique. ([OpenAI Help](https://help.openai.com/en/articles/4936848)) ([OpenAI Help PT-BR](https://help.openai.com/pt-br/articles/6654000-boas-pr%C3%A1ticas-de-engenharia-de-prompt-com-a-api-da-openai))

---

## Estrutura mínima de input

```json
{
  "experiment_id": 10,
  "hypothesis_title": "Nome da hipótese",
  "niche_name": "Nome do nicho",
  "pain_summary": "",
  "result_summary": "",
  "mechanism_summary": "",
  "proof_summary": "",
  "offer_summary": "",
  "experiment_metadata": {
    "primary_variable": "",
    "variant_id": "",
    "stage": "",
    "control_or_treatment": ""
  }
}
```

---

## 1. Prompt de `campaign-angle`

```text
Experimento #{{experiment_id}}
Hipótese resumida: {{hypothesis_title}}
Nicho: {{niche_name}}

Resumos da hipótese:
- Dor: {{pain_summary}}
- Resultado: {{result_summary}}
- Mecanismo: {{mechanism_summary}}
- Prova: {{proof_summary}}
- Oferta: {{offer_summary}}

Metadados obrigatórios do experimento:
- primary_variable: {{primary_variable}}
- variant_id: {{variant_id}}
- stage: AD
- control_or_treatment: {{control_or_treatment}}
- asset_role: campaign-angle

Tarefa alvo: campaign-angle

Objetivo:
Gerar o ângulo central da campanha para Meta Ads, usando apenas os resumos da hipótese.

Regras obrigatórias:
1. O anúncio deve deixar claro, logo no primeiro contato, para qual NICHO a peça foi feita.
2. O ângulo precisa filtrar quem não é do nicho.
3. Escolha apenas 1 dor principal e 1 promessa principal.
4. A promessa deve ser simples e entendida em segundos.
5. O mecanismo deve aparecer como apoio, não como manchete principal.
6. A prova deve ser curta e útil para anúncio e landing.
7. O CTA precisa ser compatível com prévia, briefing ou amostra.
8. Não usar linguagem de consultoria.
9. Não usar jargão técnico de marketing.
10. Se a mensagem puder servir para qualquer mercado, reescreva.
11. O resultado deve ser publicável, não brainstorming.

Formato esperado:
{
  "experimentMetadata": {
    "variant_id": "",
    "asset_role": "campaign-angle",
    "stage": "AD",
    "primary_variable": "",
    "control_or_treatment": ""
  },
  "campaignAngle": {
    "audienceFilterLine": "",
    "whoThisIsFor": "",
    "whoThisIsNotFor": "",
    "primaryPain": "",
    "primaryPromise": "",
    "singleMindedPromise": "",
    "mechanismSummary": "",
    "proofSummary": "",
    "primaryCTA": "",
    "landingMatchLine": "",
    "tone": "",
    "funnelStage": ""
  }
}
```

---

## 2. Prompt de `ad-copy`

```text
Experimento #{{experiment_id}}
Hipótese resumida: {{hypothesis_title}}
Nicho: {{niche_name}}

Resumos da hipótese:
- Dor: {{pain_summary}}
- Resultado: {{result_summary}}
- Mecanismo: {{mechanism_summary}}
- Prova: {{proof_summary}}
- Oferta: {{offer_summary}}

Metadados obrigatórios do experimento:
- primary_variable: {{primary_variable}}
- variant_id: {{variant_id}}
- stage: AD
- control_or_treatment: {{control_or_treatment}}
- asset_role: ad-copy

Tarefa alvo: ad-copy

Campaign angle:
{{campaign_angle_json}}

Objetivo do anúncio:
Gerar clique qualificado para a landing page.

Regras obrigatórias:
1. O anúncio deve filtrar explicitamente o nicho logo na primeira linha, no headline ou no gancho principal.
2. O anúncio deve deixar claro para qual NICHO ele foi feito.
3. A copy precisa ser compreendida em poucos segundos.
4. O anúncio deve abrir por:
   - dor
   - resultado
   - prova
5. O mecanismo só entra depois do benefício principal.
6. O CTA deve ser exatamente compatível com a landing.
7. Não usar linguagem de consultoria.
8. Não usar jargão de tráfego pago.
9. Não usar blocos de texto excessivamente longos.
10. Criar 3 variações:
   - V1 focada na dor
   - V2 focada no resultado
   - V3 focada na prova
11. Para cada variação, entregar 3 comprimentos:
   - curta
   - media
   - longa
12. Cada variação deve ter um gancho claramente diferente.
13. Se a peça puder servir para qualquer mercado, reescreva.
14. A copy deve soar como anúncio de feed, não mini landing page.

Formato esperado:
{
  "adCopy": {
    "primaryTextVariants": [
      {
        "label": "dor",
        "audienceFilterLine": "",
        "openingHookType": "dor",
        "placementHint": "feed",
        "lengthVariants": {
          "curta": "",
          "media": "",
          "longa": ""
        },
        "headline": "",
        "description": "",
        "ctaText": "",
        "compliance": {
          "semGarantiaAbsoluta": true,
          "semPromessaIndividual": true,
          "semLinguagemDeConsultoria": true
        }
      },
      {
        "label": "resultado",
        "audienceFilterLine": "",
        "openingHookType": "resultado",
        "placementHint": "stories/reels",
        "lengthVariants": {
          "curta": "",
          "media": "",
          "longa": ""
        },
        "headline": "",
        "description": "",
        "ctaText": "",
        "compliance": {
          "semGarantiaAbsoluta": true,
          "semPromessaIndividual": true,
          "semLinguagemDeConsultoria": true
        }
      },
      {
        "label": "prova",
        "audienceFilterLine": "",
        "openingHookType": "prova",
        "placementHint": "feed",
        "lengthVariants": {
          "curta": "",
          "media": "",
          "longa": ""
        },
        "headline": "",
        "description": "",
        "ctaText": "",
        "compliance": {
          "semGarantiaAbsoluta": true,
          "semPromessaIndividual": true,
          "semLinguagemDeConsultoria": true
        }
      }
    ]
  },
  "experimentMetadata": {
    "primary_variable": "",
    "variant_id": "",
    "stage": "AD",
    "control_or_treatment": "",
    "asset_role": "ad-copy"
  }
}
```

---

## 3. Prompt de `ad-image-briefing`

```text
Experimento #{{experiment_id}}
Hipótese resumida: {{hypothesis_title}}
Nicho: {{niche_name}}

Resumos da hipótese:
- Dor: {{pain_summary}}
- Resultado: {{result_summary}}
- Mecanismo: {{mechanism_summary}}
- Prova: {{proof_summary}}
- Oferta: {{offer_summary}}

Metadados obrigatórios do experimento:
- primary_variable: {{primary_variable}}
- variant_id: {{variant_id}}
- stage: AD
- control_or_treatment: {{control_or_treatment}}
- asset_role: ad-image-briefing

Tarefa alvo: ad-image-briefing

Campaign angle:
{{campaign_angle_json}}

Ad copy:
{{ad_copy_json}}

Objetivo:
Gerar um briefing visual para anúncios Meta Ads que:
- filtrem claramente o nicho
- chamem atenção no feed
- sejam rápidos de entender
- combinem com a landing

Regras obrigatórias:
1. A imagem deve parecer anúncio de feed, não quadro explicativo.
2. A imagem deve ter 1 foco visual principal.
3. A imagem deve filtrar o nicho visualmente.
4. A peça deve parecer claramente feita para o nicho informado.
5. O visual deve ser mais atraente que informativo.
6. Menos texto é melhor.
7. Evite múltiplos cards, excesso de colunas e mini-textos.
8. O texto sobre a imagem deve ser curto e dominante.
9. Se houver mais de uma ideia importante, use carrossel — não uma única arte congestionada.
10. A imagem precisa manter o mesmo CTA e promessa da landing.
11. Não usar visual com cara de software genérico.
12. Não usar visual com cara de apresentação corporativa.
13. Se a imagem puder servir para qualquer mercado, reescreva.
14. A imagem precisa responder em 2 segundos:
   - isso é para quem?
   - qual o benefício principal?
   - qual é a ação?

Formato esperado:
{
  "experimentMetadata": {
    "variant_id": "",
    "asset_role": "ad-image-briefing",
    "stage": "AD",
    "primary_variable": "",
    "control_or_treatment": ""
  },
  "adImageBriefing": {
    "objective": "",
    "singleMindedPromise": "",
    "audienceFilterLine": "",
    "mustVisuallyIdentifyAudience": true,
    "singleFocalPoint": "",
    "maxOverlayLines": 2,
    "imageTextMaxWords": 8,
    "assetType": "single-image",
    "nicheVisualSignal": "",
    "adToLandingConsistency": {
      "promiseMatch": "",
      "ctaMatch": "",
      "complianceMatch": ""
    },
    "globalDesignSystem": {
      "style": "",
      "colorPalette": {
        "primary": "",
        "accent": "",
        "neutral": ""
      },
      "typography": {
        "headline": "",
        "body": "",
        "rules": ""
      },
      "avoid": []
    },
    "variants": [
      {
        "id": "V1",
        "name": "",
        "mustMatchAdVariant": "dor",
        "concept": {
          "idea": "",
          "primaryPainToVisualize": "",
          "visualMetaphor": ""
        },
        "layout": {
          "structure": "",
          "hierarchy": []
        },
        "onImageCopy": {
          "headline": "",
          "subhead": "",
          "badge": "",
          "cta": "",
          "microcopyOptional": ""
        },
        "visualDirections": {
          "imagery": [],
          "background": ""
        },
        "productionNotes": {
          "readability": "",
          "export": []
        }
      },
      {
        "id": "V2",
        "name": "",
        "mustMatchAdVariant": "resultado",
        "concept": {
          "idea": "",
          "primaryPainToVisualize": "",
          "visualMetaphor": ""
        },
        "layout": {
          "structure": "",
          "hierarchy": []
        },
        "onImageCopy": {
          "headline": "",
          "subhead": "",
          "badge": "",
          "cta": "",
          "microcopyOptional": ""
        },
        "visualDirections": {
          "imagery": [],
          "background": ""
        },
        "productionNotes": {
          "readability": "",
          "export": []
        }
      },
      {
        "id": "V3",
        "name": "",
        "mustMatchAdVariant": "prova",
        "concept": {
          "idea": "",
          "primaryPainToVisualize": "",
          "visualMetaphor": ""
        },
        "layout": {
          "structure": "",
          "hierarchy": []
        },
        "onImageCopy": {
          "headline": "",
          "subhead": "",
          "badge": "",
          "cta": "",
          "microcopyOptional": ""
        },
        "visualDirections": {
          "imagery": [],
          "background": ""
        },
        "productionNotes": {
          "readability": "",
          "export": []
        }
      }
    ]
  }
}
```

---

## 4. Prompt de `landing-page-copy`

```text
Experimento #{{experiment_id}}
Hipótese resumida: {{hypothesis_title}}
Nicho: {{niche_name}}

Resumos da hipótese:
- Dor: {{pain_summary}}
- Resultado: {{result_summary}}
- Mecanismo: {{mechanism_summary}}
- Prova: {{proof_summary}}
- Oferta: {{offer_summary}}

Metadados obrigatórios do experimento:
- primary_variable: {{primary_variable}}
- variant_id: {{variant_id}}
- stage: LANDING
- control_or_treatment: {{control_or_treatment}}
- asset_role: landing-page-copy

Tarefa alvo: landing-page-copy

Campaign angle:
{{campaign_angle_json}}

Objetivo:
Gerar a copy da landing alinhada ao anúncio.

Regras obrigatórias:
1. A landing deve repetir claramente para qual nicho ela foi feita.
2. O hero deve manter a mesma promessa do anúncio.
3. O CTA da landing deve ser igual ao CTA do anúncio.
4. A landing deve aprofundar a promessa, não mudar o ângulo.
5. A landing deve usar apenas o que vier dos resumos e do campaign angle.
6. Não usar linguagem de consultoria.
7. Não criar nova estratégia.
8. Se a página puder servir para qualquer mercado, reescreva.

Formato esperado:
{
  "landingPageCopy": {
    "version": "",
    "primaryPromise": "",
    "ctaPrimary": "",
    "ctaSecondary": "",
    "sections": [
      {
        "id": "hero",
        "type": "hero",
        "preheader": "",
        "headline": "",
        "subheadline": "",
        "ctaPrimary": "",
        "ctaSecondary": "",
        "microcopy": "",
        "trustBullets": []
      },
      {
        "id": "problem",
        "type": "problemSection",
        "title": "",
        "bullets": [],
        "transition": ""
      },
      {
        "id": "mechanism",
        "type": "mechanismSection",
        "title": "",
        "steps": [],
        "note": ""
      },
      {
        "id": "proof",
        "type": "proofSection",
        "title": "",
        "content": "",
        "source": ""
      },
      {
        "id": "cta",
        "type": "ctaSection",
        "title": "",
        "subtitle": "",
        "ctaPrimary": "",
        "microcopy": ""
      }
    ]
  }
}
```

---

## Bloco opcional para todos os prompts

```text
O material deve deixar claro, logo no primeiro contato visual ou textual, para qual NICHO ele foi feito.
Se a mensagem ou a imagem puder servir para qualquer mercado, reescreva até ficar específica para o nicho informado em {{niche_name}}.
```
