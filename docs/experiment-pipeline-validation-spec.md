# Especificação de validação do pipeline de experimento

Esta especificação define os campos obrigatórios, regras de consistência e
checks automáticos que cada job do pipeline precisa atender. Ela consolida as
boas práticas de prompting estruturado recomendadas pela OpenAI e os critérios
mínimos de experiência exigidos pelas plataformas de mídia paga.

> Referências principais: [OpenAI Prompt Engineering Best Practices][1],
> [OpenAI Structured Outputs][2] e [Google Ads – Experiência da landing][3].

## 1. campaign-angle (`CAMPAIGN_ANGLE`)

- **Schema**: objeto `campaignAngle` com os campos `primaryPain`,
  `primaryPromise`, `mechanismSummary`, `proofSummary`, `singleMindedPromise`,
  `primaryCTA`, `landingMatchLine`, `funnelStage`, `tone`, `cta`.
- **Validações**
  - Promessa e dor precisam ser frases completas (mín. 8 caracteres, máx. 320).
  - `primaryCTA` deve ser ação escalável (verbos como *baixar*, *desbloquear*,
    *receber*) — nunca *ligar*, *consultar*, *mentorar*.
  - `landingMatchLine` não pode introduzir promessa nova.
  - `mechanismSummary` não pode afirmar entrega humana.

## 2. ad-copy (`AD_COPY`)

- **Schema**: `primaryTextVariants[]` com exatamente 3 itens (`dor`,
  `resultado`, `prova`). Cada item traz `lengthVariants` (curta, média, longa),
  `placementHint`, `openingHookType`, `headline`, `description`, `ctaText` e o
  bloco `compliance` (3 flags booleanas).
- **Validações**
  - Todos os textos devem citar a mesma promessa central definida no
    `campaign-angle`.
  - `ctaText` precisa espelhar `primaryCTA` (case insensitive, ignorando
    acentuação e pontuação final).
  - `openingHookType` aceita apenas `dor`, `consequência`, `resultado` ou
    `prova`.
  - `placementHint` aceita apenas `feed` ou `stories/reels`.
  - `compliance.*` precisa estar sempre como `true`.

## 3. ad-image-briefing (`AD_IMAGE_BRIEFING`)

- **Schema**: `briefings[]` com 3 itens (um por variação do anúncio) contendo:
  `visualAngle`, `mustMatchAdVariant`, `assetType`, `hierarchy`, `visualBriefing`,
  `safeMargins`, `formatByPlacement`, `messageMatchNotes`, `imageTextMaxWords`,
  `complianceNotes`.
- **Validações**
  - `visualAngle` e `mustMatchAdVariant` devem ser iguais a `dor`, `resultado`
    ou `prova`.
  - `imageTextMaxWords` limitado entre 3 e 12.
  - `assetType` limitado a `estatico`, `carrossel` ou `story-vertical`.
  - `messageMatchNotes` precisa mencionar CTA e promessa principal.

## 4. landing-page-copy (`LANDING_PAGE_COPY`)

- **Schema**: objeto `landingPageCopy` com:
  - `pageGoal`, `messageMatchSource`, `messageMatchNotes`, `primaryCTA`,
    `complianceNotes` (strings);
  - `hero` com `eyebrow`, `headline`, `subheadline`, `promise`,
    `supportingCopy`, `proofBadge`, `microcopy`, `ctaLabel`, `ctaUrl`,
    `ctaMatchNotes`;
  - `bodySections[]` (≥4) cada um contendo `sectionId`, `sectionType`
    (`hero/pain/mechanism/proof/offer/cta/faq/bonus/objection`), `title`,
    `summary`, `bullets[]`, `copy`, `ctaSupport`, `sectionDependsOn`,
    `messageMatchNotes`;
  - `ctaBlocks[]` (≥2) com `placement` (`hero/mid/final/sticky/inline`),
    `ctaVariant` (`primary/secondary/ghost/sticky`), `ctaLabel`, `ctaUrl`,
    `matchAdCta`, `ctaSupport`, `messageMatchNotes`;
  - `faq[]` (≥3) com `question`, `answer`, `objectionTag`;
  - `consistencyChecks[]` (≥2) com `check`, `status` (`PASS/WARN/FAIL`),
    `details`.
- **Validações adicionais**
  - `hero.ctaLabel`, `primaryCTA`, `ctaBlocks[].ctaLabel` e
    `ctaBlocks[].matchAdCta` devem ser idênticos ao CTA do anúncio.
  - `messageMatchSource` precisa citar a headline do anúncio (case insensitive).
  - `bodySections[].sectionDependsOn` aceita apenas `primaryPromise`,
    `mechanismSummary`, `proofSummary` ou `primaryCTA`.
  - `consistencyChecks` deve conter pelo menos os checks `CTA_MATCH`,
    `PROMISE_MATCH` e `GOOGLE_LANDING_BEST_PRACTICES`.
  - `complianceNotes` precisa esclarecer que a entrega é digital/automatizada.

## 5. landing-page-wireframe (`LANDING_PAGE_WIREFRAME`)

- **Schema**: objeto `landingPageWireframe` com `pageGoal`, `variantLayoutId`
  (`form-first/proof-first/story-first`), `messageMatchSummary`,
  `mobilePriorityNotes`, `ctaPlacementNotes`, `formPlacementNotes`, além de:
  - `sectionOrder[]` (≥4) contendo `sectionId`, `sectionName`, `objective`,
    `contentType` (`hero/form/split/proof/timeline/faq/cta`), `copySource`,
    `uiNotes`, `messageMatchDependency`, `sectionDependsOn`,
    `mobilePriorityScore` (1–10), `dropOffRisk` (`baixo/medio/alto`) e
    `ctaSlot` (objeto com `hasCta`, `ctaLabel`, `ctaVariant`, `matchAdCta`,
    `notes`).
  - Requisito visual mínimo obrigatório no wireframe: `>= 4` imagens planejadas
    no total (somando a página inteira), distribuídas nas seções para manter
    escaneabilidade e prova visual.
  - Regra operacional para garantir o mínimo de imagens: cada seção em
    `sectionOrder[]` deve conter ao menos um elemento visual (`img`) no
    wireframe simplificado desta etapa.
  - `consistencyChecks[]` (≥2) no mesmo formato da copy.
- **Validações adicionais**
  - Sempre incluir checks `CTA_MATCH` e `EXPERIENCE_CONTINUITY`.
  - `ctaSlot.matchAdCta` precisa repetir o CTA oficial sempre que `hasCta=true`.
  - `formPlacementNotes` deve informar em quantos scrolls o formulário aparece e
    se existe versão sticky.
  - `mobilePriorityNotes` precisa citar explicitamente o conteúdo acima da dobra.

## 6. Checks de consistência cruzada

Ao concluir `LANDING_PAGE_COPY` ou `LANDING_PAGE_WIREFRAME`, o worker deve
recalcular os seguintes itens e incluir no array `consistencyChecks`:

1. **CTA_MATCH** – verifica igualdade literal entre CTA do anúncio e CTA(s) da
   landing (hero + blocos extras).
2. **PROMISE_MATCH** – confirma que hero/headline repetem a single-minded
   promise definida no `campaign-angle`.
3. **GOOGLE_LANDING_BEST_PRACTICES** – confere se a landing descreve claramente
   a oferta, demonstra utilidade e mantém continuidade com o anúncio, conforme a
   documentação do Google Ads.[3]
4. **EXPERIENCE_CONTINUITY** (wireframe) – valida posicionamento do CTA,
   formulário e blocos críticos para manter a experiência prometida no anúncio.

Cada check deve registrar `PASS`, `WARN` ou `FAIL` acompanhado de `details` com a
fonte da verificação (ex.: "CTA hero ≠ CTA anúncio").

## 7. Checks mínimos da Fase 4 (anti-regressão prática)

Sem criar nova infraestrutura de eval, o worker também recalcula checks simples
baseados no conteúdo retornado e sobrescreve o mesmo `check` em
`consistencyChecks[]` quando existir:

- **DELIVERABLE_CLARITY** (`landingPageCopy`): detecta oferta genérica sem
  composição concreta de entregáveis.
- **DELIVERABLE_TO_OUTCOME_LINK** (`landingPageCopy`): valida se entregáveis
  são conectados à dor/resultado prometido (e não apenas listados).
- **CTA_SPECIFICITY** (`landingPageCopy`): identifica CTA vaga sem próximo passo
  claro.
- **PREVIEW_CONCRETENESS** (`landingPageImagePlanning` e `landingPageHtml`):
  valida se existe preview/prova visual concreta e vinculada.
- **SECTION_THEME_VARIATION** (`landingPageWireframe` e `landingPageHtml`):
  detecta homogeneidade excessiva de `surfaceSpec`/binding visual entre seções.
- **ARTIFACT_SCHEMA_COMPATIBILITY** (`landingPageCopy`, `landingPageWireframe`,
  `landingPageImagePlanning`, `landingPageHtml`): confirma campos mínimos do
  modelo canônico em `docs/modelo-canonico-artefatos-pipeline-experimento.md`.

## 8. Metadados de experimento

Todo payload deve incluir `experimentMetadata` com `primary_variable`,
`variant_id`, `stage`, `control_or_treatment` e `asset_role`. Esses campos são
usados para rastrear variantes reconhecíveis na camada analítica e continuam
obrigatórios mesmo em respostas de erro.

---

[1]: https://help.openai.com/en/articles/6654000-how-to-use-advanced-prompt-engineering "OpenAI Help Center"
[2]: https://platform.openai.com/docs/guides/structured-outputs "OpenAI Structured Outputs"
[3]: https://support.google.com/google-ads/answer/14086?hl=en "Google Ads Help Center"
