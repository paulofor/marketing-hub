# Modelo Canônico de Artefatos do Pipeline de Experimento

Este documento contém **apenas o esquema canônico** dos artefatos do pipeline.

## Observação importante — independência dos experimentos

- Cada experimento deve ser tratado de forma independente, sem reutilização implícita de artefatos entre experimentos.
- **Critério canônico de exclusividade**: todo artefato gerado direta **ou** indiretamente pelo pipeline de um experimento é considerado **exclusivo daquele experimento**.
- Portanto, para um artefato ser classificado como "não exclusivo", ele não pode ter sido produzido em nenhuma etapa do pipeline do experimento (incluindo derivações, transformações, consolidações ou enriquecimentos).

## Convenção de envelope (`artifact`)

```json
{
  "artifact": {
    "artifactType": "string",
    "artifactVersion": "v1",
    "status": "DRAFT | VALIDATED | APPROVED",
    "parentArtifactIds": ["string"],
    "content": {}
  }
}
```

## `sourceDocument`

```json
{
  "sourceType": "string",
  "sourceUrl": "string",
  "rawText": "string",
  "fetchedAt": "date-time",
  "permissionState": "string"
}
```

## `evidenceItem`

```json
{
  "claim": "string",
  "excerpt": "string",
  "url": "string",
  "citation": "string",
  "confidence": "number",
  "tags": ["string"]
}
```

## `opportunityMap`

```json
{
  "market": "string",
  "problem": "string",
  "demandSignals": ["string"],
  "competitionSignals": ["string"],
  "references": ["string"]
}
```

## `mechanismSpec`

```json
{
  "problem": "string",
  "causalModel": "string",
  "intervention": "string",
  "proofBase": ["string"],
  "limitations": ["string"]
}
```

## `offerSpec`

```json
{
  "promise": "string",
  "scope": "string",
  "deliverables": ["string"],
  "constraints": ["string"],
  "pricingIdea": "string"
}
```

## `campaignAngle`

```json
{
  "visualAngle": "string",
  "hook": "string",
  "promise": "string",
  "objections": ["string"],
  "messageMatch": "string"
}
```

## `landingPageCopy`

```json
{
  "pageGoal": "string",
  "messageMatchSource": "string",
  "messageMatchNotes": "string",
  "primaryCTA": "string",
  "complianceNotes": "string",
  "hero": {
    "eyebrow": "string",
    "headline": "string",
    "subheadline": "string",
    "promise": "string",
    "supportingCopy": "string",
    "proofBadge": "string",
    "microcopy": "string",
    "ctaLabel": "string",
    "ctaUrl": "string",
    "ctaMatchNotes": "string"
  },
  "bodySections": [
    {
      "sectionId": "string",
      "sectionType": "string",
      "title": "string",
      "summary": "string",
      "bullets": ["string"],
      "copy": "string",
      "ctaSupport": "string",
      "sectionDependsOn": "primaryPromise | mechanismSummary | proofSummary | primaryCTA",
      "messageMatchNotes": "string"
    }
  ],
  "ctaBlocks": [
    {
      "placement": "string",
      "ctaVariant": "string",
      "ctaLabel": "string",
      "ctaUrl": "string",
      "matchAdCta": "string",
      "ctaSupport": "string",
      "messageMatchNotes": "string"
    }
  ],
  "faq": [
    {
      "question": "string",
      "answer": "string",
      "objectionTag": "string"
    }
  ],
  "consistencyChecks": [
    {
      "check": "CTA_MATCH | PROMISE_MATCH | GOOGLE_LANDING_BEST_PRACTICES",
      "status": "PASS | FAIL | WARNING",
      "details": "string"
    }
  ]
}
```

## `landingPageWireframe`

```json
{
  "pageGoal": "string",
  "variantLayoutId": "form-first | proof-first | story-first",
  "sectionOrder": [
    {
      "sectionId": "string",
      "sectionName": "string",
      "objective": "string",
      "contentType": "string",
      "copySource": "string",
      "uiNotes": "string",
      "messageMatchDependency": "string",
      "sectionDependsOn": "string",
      "mobilePriorityScore": "1..10",
      "dropOffRisk": "baixo | medio | alto",
      "surfaceSpec": {
        "surfaceToken": "string",
        "style": "band | solid | gradient-soft | image-tint",
        "contrastMode": "normal | high | soft",
        "notes": "string"
      },
      "ctaSlot": {
        "hasCta": "boolean",
        "ctaLabel": "string",
        "ctaVariant": "hero | mid | final | sticky | inline",
        "matchAdCta": "string",
        "notes": "string"
      }
    }
  ],
  "mobilePriorityNotes": "string",
  "ctaPlacementNotes": "string",
  "formPlacementNotes": "string",
  "consistencyChecks": [
    {
      "check": "string",
      "status": "PASS | FAIL | WARNING",
      "details": "string"
    }
  ],
  "formSpec": {
    "formId": "string",
    "title": "string",
    "submitLabel": "string",
    "submitTarget": "string",
    "submitOwnership": "inside-form | external-with-form-attr",
    "fields": [
      {
        "name": "string",
        "label": "string",
        "type": "text | email | tel",
        "required": "boolean",
        "placeholder": "string"
      }
    ],
    "consent": {
      "enabled": "boolean",
      "required": "boolean",
      "label": "string"
    },
    "successState": {
      "title": "string",
      "message": "string"
    }
  }
}
```

## `landingPageImagePlanning`

```json
{
  "pageGoal": "string",
  "visualDirectionSummary": "string",
  "sequencingNotes": "string",
  "ctaIntegrationNotes": "string",
  "images": [
    {
      "sectionId": "string",
      "sectionName": "string",
      "imageBindingKey": "slug",
      "imageRole": "string",
      "conversionRole": "string",
      "emotionalJob": "string",
      "sectionVisualGoal": "string",
      "objective": "string",
      "placement": "hero | benefit | mechanism | proof | offer | faq | cta",
      "hierarchyLevel": "primary | secondary | support",
      "attentionPriority": "high | medium | low",
      "visualWeight": "primary | secondary | support",
      "distanceToCTA": "near | medium | far",
      "supportsFormConversion": "boolean",
      "formRelationNotes": "string",
      "imagePrompt": "string",
      "negativePrompt": "string",
      "generationHints": ["string"],
      "visualStyle": "string",
      "composition": "string",
      "focalPoint": "string",
      "supportingElements": ["string"],
      "mood": "string",
      "messageMatchNotes": "string",
      "complianceNotes": "string",
      "layoutBinding": {
        "preferredDesktopPlacement": "left | right | center | background",
        "preferredMobilePlacement": "above-copy | below-copy | inline | background",
        "desktopAspectRatio": "string",
        "mobileAspectRatio": "string",
        "allowCrop": "boolean",
        "safeCropZones": {
          "top": "0..1",
          "right": "0..1",
          "bottom": "0..1",
          "left": "0..1"
        }
      },
      "dimensions": {
        "desktop": "string",
        "mobile": "string"
      },
      "safeMargins": "string",
      "textOverlayGuidance": "string"
    }
  ],
  "consistencyChecks": [
    {
      "check": "IMAGE_MESSAGE_MATCH | VISUAL_HIERARCHY | CTA_CONTINUITY",
      "status": "PASS | FAIL | WARNING",
      "details": "string"
    }
  ]
}
```

## `landingPageHtml`

> Cada documento HTML gerado pela etapa `landingPageHtml` traz atributos de dados para vincular os visuais planejados aos ativos finais.
> Os principais atributos são `data-image-section-id` (mapeia o `planning_item_key`), `data-image-binding-key` (chave semântica)
> e `data-image-role`/`data-conversion-role` (indicam o papel do ativo). Esses marcadores permitem que o pipeline substitua
> placeholders genéricos por imagens aprovadas automaticamente durante a publicação das landings.
>
> **Contrato operacional (vigente a partir de 2026-04-22):**
> - a resposta do modelo para a etapa `landingPageHtml` deve ser **HTML puro** (documento final completo);
> - é proibido retornar envelope JSON, markdown, bloco ``` ou campo textual contendo JSON serializado;
> - o backend do Lead Portal **não** deve tentar “desempacotar” HTML de payload misto/JSON: entradas fora de HTML puro devem ser rejeitadas.
> - para atributos canônicos de binding/superfície no HTML (`data-section-id`, `data-surface-token`, `data-surface-style`, `data-surface-contrast`), o backend normaliza artefatos de serialização antes da validação estrita: aspas codificadas (`&quot;`, `&#34;`, `&#x22;`), versões escapadas com barra invertida (ex.: `\\&quot;`) e tokens de quebra de linha escapados (`\\n`, `\\r`, `\\t`). O warning operacional deve permanecer para correção definitiva no worker/prompt.

```json
{
  "htmlDocument": "string",
  "formSpec": {
    "formId": "string",
    "title": "string",
    "submitLabel": "string",
    "submitTarget": "string",
    "submitTargetTemplateRule": "Quando usar placeholder de slug, manter exatamente {slug} (sem codificar para %7Bslug%7D).",
    "submissionRuntime": {
      "mode": "async-fetch",
      "onSubmit": {
        "preventDefault": "boolean",
        "validityGate": "checkValidity+reportValidity",
        "request": {
          "urlSource": "form.action",
          "methodSource": "form.method.toUpperCase()",
          "bodySource": "new FormData(form)",
          "payloadPartName": "payload",
          "payloadPartContentType": "application/json"
        },
        "buttonState": {
          "disableDuringRequest": "boolean",
          "loadingLabel": "string",
          "restoreLabelAfterRequest": "string"
        },
        "successFeedback": {
          "inlineElementId": "string",
          "displayMode": "block|flex|inline-block",
          "resetForm": "boolean",
          "waitForEmailMessage": "string"
        },
        "errorFeedback": {
          "mode": "inline|alert",
          "message": "string"
        }
      }
    },
    "cta": {
      "label": "string",
      "target": "string",
      "variant": "hero | mid | final | sticky | inline"
    },
    "fields": [
      {
        "name": "string",
        "label": "string",
        "type": "text | email | tel",
        "required": "boolean",
        "placeholder": "string"
      }
    ],
    "consent": {
      "enabled": "boolean",
      "required": "boolean",
      "label": "string"
    },
    "successState": {
      "title": "string",
      "message": "string",
      "nextStepHint": "string"
    }
  },
  "summary": "string",
  "imagePlacementContract": {
    "requiredDataAttributes": [
      "data-image-desktop-placement",
      "data-image-mobile-placement"
    ],
    "requiredDesktopClasses": [
      "image-placement-left",
      "image-placement-right",
      "image-placement-center",
      "image-placement-background"
    ],
    "requiredMobileClasses": [
      "image-mobile-above-copy",
      "image-mobile-below-copy",
      "image-mobile-inline",
      "image-mobile-background"
    ]
  },
  "consistencyChecks": [
    {
      "check": "CTA_MATCH | PROMISE_MATCH | IMAGE_PLAN_BINDING | SURFACE_SPEC_BINDING | FORM_SPEC_BINDING | FORM_USABILITY",
      "status": "PASS | FAIL | WARNING",
      "details": "string"
    }
  ]
}
```

## `landingCodeBundle`

```json
{
  "html": "string",
  "css": "string",
  "js": "string",
  "imageRefs": ["string"],
  "metadata": {
    "model": "string",
    "prompt": "string"
  }
}
```

## Fluxo automático do pipeline (referência normativa)

Este documento define o **schema canônico dos artefatos**.

A orquestração da fila automática (ordem de etapas, estados, progressão, bloqueios e retomada)
fica definida no documento:

- `docs/canonical/experiments-automation-flow-canon.v1.md`

Regra prática:

- este arquivo responde a "qual artefato e qual estrutura";
- o cânone de automação responde a "em qual ordem, com quais estados e com quais gatilhos".
