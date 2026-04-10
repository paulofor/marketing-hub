# Modelo Canônico de Artefatos do Pipeline de Experimento

Este documento define o **modelo canônico** dos artefatos do pipeline de experimento, seguindo uma estrutura semelhante ao exemplo fornecido.

## Tabela canônica (artefato → função → campos-chave → origem)

| Artefato | Função | Campos-chave | Origem |
|---|---|---|---|
| `sourceDocument` | Registro bruto da fonte consultada | `sourceType`, `sourceUrl`, `rawText`, `fetchedAt`, `permissionState` | Código |
| `evidenceItem` | Trecho/fato normalizado extraído da fonte | `claim`, `excerpt`, `url`, `citation`, `confidence`, `tags` | Código/LLM |
| `opportunityMap` | Mapa de oportunidade de mercado e demanda | `market`, `problem`, `demandSignals`, `competitionSignals`, `references` | LLM |
| `mechanismSpec` | Definição do mecanismo/solução proposta | `problem`, `causalModel`, `intervention`, `proofBase`, `limitations` | LLM |
| `offerSpec` | Definição estruturada da oferta/produto | `promise`, `scope`, `deliverables`, `constraints`, `pricingIdea` | LLM |
| `campaignAngle` | Ângulo estratégico por variação de comunicação | `visualAngle`, `hook`, `promise`, `objections`, `messageMatch` | LLM |
| `landingPageCopy` | Texto da landing estruturado para validação e renderização | `pageGoal`, `messageMatchSource`, `messageMatchNotes`, `primaryCTA`, `complianceNotes`, `hero`, `bodySections`, `ctaBlocks`, `faq`, `consistencyChecks` | LLM |
| `landingPageWireframe` | Estrutura/layout por seção + contrato do formulário | `variantLayoutId`, `sectionOrder`, `mobilePriorityNotes`, `ctaPlacementNotes`, `formPlacementNotes`, `formSpec`, `consistencyChecks` | LLM |
| `landingImagePlan` | Plano visual e pacote de prompt de imagem por seção | `sectionId`, `imageRole`, `promptPackage`, `altText`, `priority` | LLM |
| `landingCodeBundle` | Entrega final da página renderizada/publicável | `html`, `css`, `js`, `imageRefs`, `metadata` | Builder |

## Artefato do item **Texto da Landing** (`LANDING_PAGE_COPY`)

No pipeline operacional, o item visual **Texto da Landing** corresponde ao artefato `landingPageCopy`, persistido em `experiment.landing_page_copy` e gerado na seção `LANDING_PAGE_COPY`.

### Campos canônicos obrigatórios

- Raiz de `landingPageCopy`:
  - `pageGoal`
  - `messageMatchSource`
  - `messageMatchNotes`
  - `primaryCTA`
  - `complianceNotes`
- Bloco `hero`:
  - `eyebrow`, `headline`, `subheadline`, `promise`, `supportingCopy`
  - `proofBadge`, `microcopy`, `ctaLabel`, `ctaUrl`, `ctaMatchNotes`
- Bloco `bodySections[]` (mínimo 4 itens):
  - `sectionId`, `sectionType`, `title`, `summary`, `bullets`, `copy`
  - `ctaSupport`, `sectionDependsOn`, `messageMatchNotes`
- Bloco `ctaBlocks[]` (mínimo 2 itens):
  - `placement`, `ctaVariant`, `ctaLabel`, `ctaUrl`, `matchAdCta`
  - `ctaSupport`, `messageMatchNotes`
- Bloco `faq[]` (mínimo 3 itens):
  - `question`, `answer`, `objectionTag`
- Bloco `consistencyChecks[]` (mínimo 2 itens):
  - `check`, `status`, `details`

### Regras de validação críticas do item

1. `hero.ctaLabel`, `primaryCTA`, `ctaBlocks[].ctaLabel` e `ctaBlocks[].matchAdCta` devem manter igualdade com o CTA oficial do anúncio.
2. `messageMatchSource` deve citar a headline do anúncio de origem.
3. `bodySections[].sectionDependsOn` deve usar apenas: `primaryPromise`, `mechanismSummary`, `proofSummary` ou `primaryCTA`.
4. `consistencyChecks[]` deve incluir explicitamente: `CTA_MATCH`, `PROMISE_MATCH` e `GOOGLE_LANDING_BEST_PRACTICES`.
5. `complianceNotes` deve deixar explícito que a entrega é digital/automatizada.

## Artefato do item **Layout da Landing** (`LANDING_PAGE_WIREFRAME`)

No pipeline operacional, o item **Layout da Landing** corresponde ao artefato `landingPageWireframe`, persistido em `experiment.landing_page_wireframe` e gerado na seção `LANDING_PAGE_WIREFRAME`.

### Envelope do artefato (saída canônica do pipeline)

- `artifact.artifactType`: `experiment.landing.layout`
- `artifact.artifactVersion`: `v1`
- `artifact.status`: `DRAFT | VALIDATED | APPROVED`
- `artifact.parentArtifactIds`: lista de dependências de artefatos anteriores
- `artifact.content`: payload canônico do wireframe (`landingPageWireframe`)

### Campos canônicos obrigatórios

- Raiz de `landingPageWireframe`:
  - `pageGoal`
  - `variantLayoutId` (`form-first`, `proof-first`, `story-first`)
  - `sectionOrder[]` (mínimo 4 itens)
  - `consistencyChecks[]` (mínimo 2 itens)
  - `formSpec`
- Bloco `sectionOrder[]`:
  - `sectionId`, `sectionName`, `objective`, `contentType`
  - `copySource`, `uiNotes`, `messageMatchDependency`, `sectionDependsOn`
  - `mobilePriorityScore` (1..10), `dropOffRisk` (`baixo`, `medio`, `alto`)
  - `surfaceSpec` (obrigatório)
  - `ctaSlot` (opcional, mas recomendado para seções com CTA)
- Bloco `sectionOrder[].surfaceSpec`:
  - `surfaceToken`
  - `style` (`band`, `solid`, `gradient-soft`, `image-tint`)
  - `contrastMode` (`normal`, `high`, `soft`)
  - `notes`
- Bloco `sectionOrder[].ctaSlot`:
  - `hasCta`
  - `ctaLabel`
  - `ctaVariant` (`hero`, `mid`, `final`, `sticky`, `inline`)
  - `matchAdCta`
  - `notes`
- Notas de apoio:
  - `mobilePriorityNotes`
  - `ctaPlacementNotes`
  - `formPlacementNotes`
- Bloco `formSpec`:
  - `formId`, `title`, `submitLabel`, `submitTarget`
  - `fields[]` com `id`, `name`, `label`, `type`, `required`, `placeholder`, `autocomplete`, `helpText`
  - `consent` com `enabled`, `required`, `label`
  - `successState` com `title`, `message`

### Regras de validação críticas do item

1. O wireframe deve preservar **message match** com a copy de landing e com o CTA oficial do anúncio.
2. `sectionOrder[].sectionId` deve manter rastreabilidade com os demais artefatos (`landingPageCopy`, `landingImagePlan`, `landingCodeBundle`).
3. Cada bloco em `sectionOrder[]` deve declarar `mobilePriorityScore`, `dropOffRisk` e `surfaceSpec`.
4. `formSpec` é a **fonte única da verdade** para os campos do formulário na etapa de `LANDING_PAGE_HTML` (sem inventar/remover/renomear campos).
5. `variantLayoutId` deve ficar restrito aos valores canônicos (`form-first`, `proof-first`, `story-first`).

## Dependência lógica sugerida

```mermaid
flowchart LR
  A[sourceDocument] --> B[evidenceItem]
  B --> C[opportunityMap]
  C --> D[mechanismSpec]
  D --> E[offerSpec]
  E --> F[campaignAngle]
  F --> G[landingPageCopy]
  G --> H[landingPageWireframe]
  H --> I[landingImagePlan]
  I --> J[landingCodeBundle]
```

## Regras de consistência entre artefatos

1. `evidenceItem` deve referenciar a origem em `sourceDocument` por `citation`/`url`.
2. `campaignAngle` deve manter **message match** com `offerSpec` e `mechanismSpec`.
3. `landingPageCopy`, `landingPageWireframe` e `landingImagePlan` devem compartilhar o mesmo `sectionId` para cada seção.
4. `landingCodeBundle` deve manter rastreabilidade de ativos por `imageRefs` e metadados em `metadata`.
5. Todo artefato gerado por IA deve preservar metadados de auditoria (por exemplo: `model` e `prompt`) no fluxo de persistência.

## Exemplo mínimo de payload canônico (JSON)

```json
{
  "sourceDocument": {
    "sourceType": "url",
    "sourceUrl": "https://exemplo.com/artigo",
    "rawText": "...",
    "fetchedAt": "2026-04-10T10:00:00Z",
    "permissionState": "allowed"
  },
  "evidenceItem": {
    "claim": "Usuários valorizam implementação rápida",
    "excerpt": "...",
    "url": "https://exemplo.com/artigo",
    "citation": "Estudo X, seção 2",
    "confidence": 0.84,
    "tags": ["velocidade", "valor"]
  },
  "opportunityMap": {
    "market": "PMEs",
    "problem": "Baixa previsibilidade de geração de leads",
    "demandSignals": ["busca crescente", "dor recorrente"],
    "competitionSignals": ["mensagens genéricas"],
    "references": ["evidenceItem:1"]
  },
  "mechanismSpec": {
    "problem": "Conversão inconsistente",
    "causalModel": "Falta de message match entre anúncio e landing",
    "intervention": "Estruturar promessa + prova + CTA por seção",
    "proofBase": ["case interno", "benchmark"],
    "limitations": ["depende de tráfego qualificado"]
  },
  "offerSpec": {
    "promise": "Landing validada em dias, não semanas",
    "scope": "Diagnóstico + copy + layout + publicação",
    "deliverables": ["copy", "layout", "bundle"],
    "constraints": ["janela de campanha curta"],
    "pricingIdea": "setup + recorrência"
  },
  "campaignAngle": {
    "visualAngle": "clareza de resultado",
    "hook": "Pare de desperdiçar clique",
    "promise": "Mais aproveitamento do tráfego pago",
    "objections": ["já tentei antes"],
    "messageMatch": "alto"
  },
  "landingPageCopy": {
    "pageGoal": "Capturar leads qualificados para diagnóstico",
    "messageMatchSource": "headline_ad: Pare de desperdiçar clique",
    "messageMatchNotes": "Promessa e CTA mantidos do anúncio",
    "primaryCTA": "Quero minha landing",
    "complianceNotes": "Entrega 100% digital e automatizada",
    "hero": {
      "eyebrow": "Para PMEs com tráfego pago",
      "headline": "Converta mais com consistência",
      "subheadline": "Organize promessa, prova e CTA sem fricção",
      "promise": "Mais aproveitamento do tráfego pago",
      "supportingCopy": "Aplicação rápida com foco em resultado",
      "proofBadge": "+120 projetos",
      "microcopy": "Sem consultoria presencial",
      "ctaLabel": "Quero minha landing",
      "ctaUrl": "/formulario",
      "ctaMatchNotes": "Mesmo CTA do anúncio"
    },
    "bodySections": [
      {
        "sectionId": "pain-01",
        "sectionType": "pain",
        "title": "Pare de perder cliques",
        "summary": "Seu tráfego existe, mas não converte",
        "bullets": ["mensagem desalinhada", "falta de prova"],
        "copy": "...",
        "ctaSupport": "Ajuste em poucos dias",
        "sectionDependsOn": "primaryPromise",
        "messageMatchNotes": "Conecta com dor do anúncio"
      },
      {
        "sectionId": "mechanism-01",
        "sectionType": "mechanism",
        "title": "Como funciona",
        "summary": "Processo guiado por dados",
        "bullets": ["copy", "layout", "publicação"],
        "copy": "...",
        "ctaSupport": "Fluxo padronizado",
        "sectionDependsOn": "mechanismSummary",
        "messageMatchNotes": "Mesma promessa, mais profundidade"
      },
      {
        "sectionId": "proof-01",
        "sectionType": "proof",
        "title": "Provas objetivas",
        "summary": "Resultados em cenários similares",
        "bullets": ["cases", "métricas"],
        "copy": "...",
        "ctaSupport": "Confiança para avançar",
        "sectionDependsOn": "proofSummary",
        "messageMatchNotes": "Reforça credibilidade"
      },
      {
        "sectionId": "cta-01",
        "sectionType": "cta",
        "title": "Pronto para aplicar",
        "summary": "Comece hoje",
        "bullets": ["setup rápido"],
        "copy": "...",
        "ctaSupport": "Sem complexidade",
        "sectionDependsOn": "primaryCTA",
        "messageMatchNotes": "Fechamento com CTA oficial"
      }
    ],
    "ctaBlocks": [
      {
        "placement": "hero",
        "ctaVariant": "primary",
        "ctaLabel": "Quero minha landing",
        "ctaUrl": "/formulario",
        "matchAdCta": "Quero minha landing",
        "ctaSupport": "Comece agora",
        "messageMatchNotes": "Match total"
      },
      {
        "placement": "final",
        "ctaVariant": "sticky",
        "ctaLabel": "Quero minha landing",
        "ctaUrl": "/formulario",
        "matchAdCta": "Quero minha landing",
        "ctaSupport": "Última chamada",
        "messageMatchNotes": "Match total"
      }
    ],
    "faq": [
      {
        "question": "Preciso de equipe técnica interna?",
        "answer": "Não, o fluxo já entrega os blocos prontos.",
        "objectionTag": "execucao"
      },
      {
        "question": "Quanto tempo para publicar?",
        "answer": "Normalmente em poucos dias.",
        "objectionTag": "prazo"
      },
      {
        "question": "Funciona para nichos diferentes?",
        "answer": "Sim, ajustando copy e prova por contexto.",
        "objectionTag": "aderencia"
      }
    ],
    "consistencyChecks": [
      {
        "check": "CTA_MATCH",
        "status": "PASS",
        "details": "CTA alinhado entre anúncio e landing"
      },
      {
        "check": "PROMISE_MATCH",
        "status": "PASS",
        "details": "Promessa do anúncio refletida no hero"
      },
      {
        "check": "GOOGLE_LANDING_BEST_PRACTICES",
        "status": "PASS",
        "details": "Oferta clara e continuidade pós-clique"
      }
    ]
  },
  "landingPageWireframe": {
    "pageGoal": "Capturar leads qualificados para diagnóstico",
    "variantLayoutId": "form-first",
    "sectionOrder": [
      {
        "sectionId": "hero",
        "sectionName": "Hero + Form",
        "objective": "Reforçar promessa e capturar lead acima da dobra",
        "contentType": "hero",
        "copySource": "landingPageCopy.hero",
        "uiNotes": "Bloco compacto com alto contraste",
        "messageMatchDependency": "headline_ad",
        "sectionDependsOn": "primaryPromise",
        "mobilePriorityScore": 10,
        "dropOffRisk": "baixo",
        "surfaceSpec": {
          "surfaceToken": "surface-base",
          "style": "solid",
          "contrastMode": "high",
          "notes": "Priorizar leitura do título e CTA"
        },
        "ctaSlot": {
          "hasCta": true,
          "ctaLabel": "Quero minha landing",
          "ctaVariant": "hero",
          "matchAdCta": "Quero minha landing",
          "notes": "CTA principal acima da dobra"
        }
      }
    ],
    "mobilePriorityNotes": "Hero e formulário primeiro; FAQ no final.",
    "ctaPlacementNotes": "CTA em hero, meio e fechamento.",
    "formPlacementNotes": "Formulário ancorado no hero para menor atrito.",
    "consistencyChecks": [
      {
        "check": "CTA_MATCH",
        "status": "PASS",
        "details": "CTA do wireframe igual ao anúncio"
      },
      {
        "check": "MESSAGE_MATCH",
        "status": "PASS",
        "details": "Hierarquia e narrativa compatíveis com a copy"
      }
    ],
    "formSpec": {
      "formId": "lead-capture-primary",
      "title": "Receber a prévia do Kit (IA)",
      "submitLabel": "Desbloquear o Kit (receber a prévia gerada por IA)",
      "submitTarget": "#desbloquear",
      "fields": [
        {
          "id": "nome",
          "name": "nome",
          "label": "Nome",
          "type": "text",
          "required": true,
          "placeholder": "Seu nome",
          "autocomplete": "name",
          "helpText": "Como devemos te chamar"
        }
      ],
      "consent": {
        "enabled": true,
        "required": false,
        "label": "Aceito receber a prévia no contato informado."
      },
      "successState": {
        "title": "Prévia enviada",
        "message": "Confira seu e-mail para acessar o material."
      }
    }
  },
  "landingImagePlan": [
    {
      "sectionId": "hero",
      "imageRole": "contextual-proof",
      "promptPackage": {
        "subject": "profissional analisando dashboard",
        "style": "editorial clean",
        "lighting": "natural"
      },
      "altText": "Profissional avaliando indicadores de campanha",
      "priority": "high"
    }
  ],
  "landingCodeBundle": {
    "html": "<html>...</html>",
    "css": "/* ... */",
    "js": "// ...",
    "imageRefs": ["asset://hero-001"],
    "metadata": {
      "version": "1.0.0",
      "generatedAt": "2026-04-10T10:30:00Z"
    }
  }
}
```
