# Modelo Canônico de Artefatos do Pipeline de Experimento

Este documento contém **apenas o esquema canônico** dos artefatos do pipeline.

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

```json
{
  "htmlDocument": "string",
  "formSpec": {
    "formId": "string",
    "title": "string",
    "submitLabel": "string",
    "submitTarget": "string",
    "submitTargetTemplateRule": "Quando usar placeholder de slug, manter exatamente {slug} (sem codificar para %7Bslug%7D).",
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
      "message": "string"
    }
  },
  "summary": "string",
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
