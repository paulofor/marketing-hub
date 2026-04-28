import {
  type ReactNode,
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
} from "react";
import { toast } from "react-toastify";
import * as Tabs from "@radix-ui/react-tabs";
import axios from "axios";
import { Link } from "react-router-dom";
import type { Hypothesis } from "../../api/hypothesis/useHypothesisBoard";
import {
  CampaignAngleSummary,
  hasCampaignAngleContent,
  parseCampaignAnglePayload,
} from "./campaignAngleParser";
import {
  AdCopyContent,
  hasAdCopyContent,
  parseAdCopyPayload,
} from "./adCopyParser";
import {
  ImagePromptContent,
  hasImagePromptContent,
  parseImagePromptPayload,
} from "./imageBriefingParser";
import {
  LandingCopyBlock,
  LandingCopyContent,
  LandingCopyFormField,
  LandingCopyVersion,
  hasLandingCopyContent,
  parseLandingCopyPayload,
} from "./landingCopyParser";
import {
  LandingLayoutContent,
  hasLandingLayoutContent,
  parseLandingLayoutPayload,
} from "./landingLayoutParser";
import {
  LandingImagePlanningContent,
  hasLandingImagePlanningContent,
  parseLandingImagePlanningPayload,
} from "./landingImagePlanningParser";
import {
  LandingHtmlContent,
  hasLandingHtmlContent,
  parseLandingHtmlPayload,
} from "./landingHtmlParser";
import { extractObjectCandidates } from "./parserUtils";
import { useExperimentPipelineJobs } from "../../api/experiment/useExperimentPipelineJobs";
import { useFrameworkImageStatuses } from "../../api/experiment/useFrameworkImageStatuses";
import { useGenerateFrameworkImages } from "../../api/experiment/useGenerateFrameworkImages";
import CollapsibleJsonViewer from "../../components/CollapsibleJsonViewer";

type ContentGenerationSectionKey =
  | "campaign-angle"
  | "ad-copy"
  | "image-prompt"
  | "landing-copy"
  | "landing-layout"
  | "landing-image-planning"
  | "landing-design-preset"
  | "landing-html";

interface ContentGenerationSection {
  key: ContentGenerationSectionKey;
  label: string;
  description: string;
  defaultQuantity: number;
}

const CONTENT_GENERATION_SECTIONS: ContentGenerationSection[] = [
  {
    key: "campaign-angle",
    label: "Angulo da Campanha",
    description:
      "Defina variações de narrativa para explorar novas entradas de comunicação.",
    defaultQuantity: 3,
  },
  {
    key: "ad-copy",
    label: "Texto do Anuncio",
    description:
      "Gere textos com foco em promessa, objeções e chamada para ação.",
    defaultQuantity: 3,
  },
  {
    key: "image-prompt",
    label: "Prompt da Imagem",
    description:
      "Crie prompts para orientar a geração de criativos visuais coerentes com o ângulo.",
    defaultQuantity: 4,
  },
  {
    key: "landing-copy",
    label: "Texto da Landing",
    description:
      "Produza blocos de copy para título, prova, benefícios e CTA da landing.",
    defaultQuantity: 4,
  },
  {
    key: "landing-layout",
    label: "Layout da Landing",
    description:
      "Sugira estruturas visuais e ordem de seções para a página de conversão.",
    defaultQuantity: 2,
  },
  {
    key: "landing-image-planning",
    label: "Planejamento de Imagens da Landing",
    description:
      "Planeje prompts, posicionamento e direção visual das imagens antes da geração final do HTML.",
    defaultQuantity: 1,
  },
  {
    key: "landing-design-preset",
    label: "Preset de Design da Landing",
    description:
      "Defina tema visual, paleta e presets por seção antes da composição final do HTML.",
    defaultQuantity: 1,
  },
  {
    key: "landing-html",
    label: "HTML da Landing",
    description:
      "Integre copy + layout e gere o HTML final com CSS e scripts para uso no formulário.",
    defaultQuantity: 1,
  },
];

const SECTION_LABEL_BY_KEY: Record<ContentGenerationSectionKey, string> =
  CONTENT_GENERATION_SECTIONS.reduce(
    (acc, section) => ({ ...acc, [section.key]: section.label }),
    {} as Record<ContentGenerationSectionKey, string>,
  );

type RequestUiStatus =
  | "IDLE"
  | "PENDING"
  | "PROCESSING"
  | "COMPLETED"
  | "FAILED"
  | "INVALIDATED";

interface SectionRequestState {
  status: RequestUiStatus;
  requestedAt?: string;
  startedAt?: string;
  completedAt?: string;
  customInstructions?: string;
  errorMessage?: string;
  stageLabel?: string;
  executionSource?: "WORKER_IA" | "LHM" | "LHM_IA";
}

interface ParsedBackendError {
  timestamp?: string;
  message?: string;
  status?: number;
  path?: string;
}

interface SectionInvalidationState {
  sourceSection: string;
  sourceAt?: string;
  sourceTimestamp: number;
  requiresRefresh: boolean;
}

interface AutoQueueState {
  isActive: boolean;
  waitingFor?: ContentGenerationSectionKey;
  pending: ContentGenerationSectionKey[];
}

type LandingImageFlowStatus = "NOT_STARTED" | "STARTED" | "PROCESSING" | "COMPLETED";

const SECTION_REQUEST_INITIAL_STATE: Record<
  ContentGenerationSectionKey,
  SectionRequestState
> = CONTENT_GENERATION_SECTIONS.reduce(
  (acc, section) => ({ ...acc, [section.key]: { status: "IDLE" } }),
  {} as Record<ContentGenerationSectionKey, SectionRequestState>,
);

const AUTO_QUEUE_INITIAL_STATE: AutoQueueState = {
  isActive: false,
  pending: [],
};

const JOB_SECTION_ALIASES: Record<string, ContentGenerationSectionKey> = {
  CAMPAIGN_ANGLE: "campaign-angle",
  AD_COPY: "ad-copy",
  AD_IMAGE_BRIEFING: "image-prompt",
  LANDING_PAGE_COPY: "landing-copy",
  LANDING_PAGE_WIREFRAME: "landing-layout",
  LANDING_PAGE_IMAGE_PLANNING: "landing-image-planning",
  LANDING_PAGE_DESIGN_PRESET: "landing-design-preset",
  LANDING_PAGE_HTML: "landing-html",
};

const STATUS_LABELS: Record<RequestUiStatus, string> = {
  IDLE: "Sem solicitação",
  PENDING: "Na fila",
  PROCESSING: "Em processamento",
  COMPLETED: "Concluída",
  FAILED: "Com erro",
  INVALIDATED: "Dependência alterada",
};

const STATUS_BADGES: Record<RequestUiStatus, string> = {
  IDLE: "secondary",
  PENDING: "info",
  PROCESSING: "warning",
  COMPLETED: "success",
  FAILED: "danger",
  INVALIDATED: "dark",
};

const STAGE_LABELS: Record<string, string> = {
  WAITING_AI_WORKER: "Aguardando AI Worker",
  SENT_TO_OPENAI: "Enviada para OpenAI",
  WAITING_OPENAI: "Aguardando resposta da OpenAI",
  COMPLETED: "Finalizada",
  FAILED: "Falhou",
};

const EXECUTION_SOURCE_LABELS: Record<"WORKER_IA" | "LHM" | "LHM_IA", string> = {
  WORKER_IA: "Worker IA",
  LHM: "LHM inline",
  LHM_IA: "LHM + IA",
};

const EXECUTION_SOURCE_BADGES: Record<"WORKER_IA" | "LHM" | "LHM_IA", string> = {
  WORKER_IA: "primary",
  LHM: "info",
  LHM_IA: "dark",
};

const LANDING_IMAGE_FLOW_LABELS: Record<LandingImageFlowStatus, string> = {
  NOT_STARTED: "Não iniciado",
  STARTED: "Iniciado",
  PROCESSING: "Processando",
  COMPLETED: "Concluído",
};

const LANDING_IMAGE_FLOW_BADGES: Record<LandingImageFlowStatus, string> = {
  NOT_STARTED: "secondary",
  STARTED: "info",
  PROCESSING: "warning",
  COMPLETED: "success",
};

const COMMON_PIPELINE_PROMPT = `Você cria ativos de campanha para o Marketing Hub.

Regras globais:
1. O anúncio e a landing devem ter a mesma promessa central.
2. O CTA do anúncio deve combinar com a ação principal da landing.
3. O material precisa caber no envelope real do produto:
   - pode entregar ativos digitais gerados por IA
   - não pode prometer consultoria, call, gestão humana ou acompanhamento manual
4. Priorize clareza comercial:
   DOR → RESULTADO → MECANISMO → PROVA → AÇÃO
5. Não transforme mecanismo em promessa principal.
6. Não use jargão técnico desnecessário.
7. O público é geral dentro do nicho, com baixa a moderada maturidade em marketing.
8. Sempre escreva pensando em alta escala e geração automatizada.
9. O anúncio deve ser rápido de entender.
10. A landing deve aprofundar a promessa e reduzir ceticismo.`;

const AD_COPY_PROMPT_TEMPLATE = `${COMMON_PIPELINE_PROMPT}

Contexto do nicho: {nicho}

Ângulo da campanha: {campaignAngle}
Dor principal: {primaryPain}
Promessa principal: {primaryPromise}
Mecanismo resumido: {mechanismSummary}
Prova resumida: {proofSummary}

Objetivo do anúncio:
Gerar clique qualificado para a landing page.

Regras:
1. O texto do anúncio deve ser entendido em poucos segundos.
2. A primeira linha deve abrir com dor, consequência, resultado ou prova.
3. O mecanismo deve aparecer só depois do benefício principal.
4. O anúncio não pode parecer consultoria.
5. A promessa precisa ser compatível com ativos digitais gerados por IA.
6. Não usar jargão de tráfego pago.
7. Criar 3 variações:
   - V1 focada na dor
   - V2 focada no resultado
   - V3 focada na prova
8. Para cada variação, entregar 3 comprimentos de texto principal: curta, média e longa.
9. Definir openingHookType por variação com: dor, consequência, resultado ou prova.
10. Definir placementHint por variação com: feed ou stories/reels.
11. Aplicar trava de compliance em todas as variações:
   - sem garantia absoluta
   - sem promessa individual
   - sem linguagem de consultoria
12. O CTA deve combinar exatamente com a landing.
13. Entregar texto pensado para Meta Ads e testável por placement/comprimento.

Formato esperado:
JSON com:
primaryTextVariants [
  {
    "label": "dor|resultado|prova",
    "openingHookType": "dor|consequência|resultado|prova",
    "placementHint": "feed|stories/reels",
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
]`;

const LANDING_COPY_PROMPT_TEMPLATE = `${COMMON_PIPELINE_PROMPT}

Contexto do nicho: {nicho}

Ângulo da campanha: {campaignAngle}
Headline do anúncio clicado: {adHeadline}
Dor principal: {primaryPain}
Promessa principal: {primaryPromise}
Mecanismo resumido: {mechanismSummary}
Prova resumida: {proofSummary}

Objetivo da landing:
Converter o clique em:
- preenchimento de briefing
- geração de amostra
- pedido de prévia

Regras:
1. A landing deve continuar exatamente a promessa do anúncio.
2. Entregue dois modos de copy da landing:
   - landingCurta: versão enxuta para leitura rápida
   - landingCompleta: versão aprofundada para leitura detalhada
3. Inclua messageMatchSource informando qual headline do anúncio esta landing está espelhando.
4. Separe heroPromise de offerPromise, sem misturar proposta de valor com detalhes de oferta.
5. O hero deve deixar claro:
   - para quem é
   - qual transformação entrega
   - qual próximo passo
6. O mecanismo deve ser explicado de forma simples.
7. A prova deve reduzir o medo de “isso é genérico” ou “isso não serve para mim”.
8. O CTA principal deve aparecer no topo e se repetir ao longo da página.
9. O texto deve ser escaneável.
10. Não usar linguagem de consultoria humana.
11. Toda a oferta deve caber no envelope do produto.
12. O formulário deve pedir apenas dados necessários para gerar a amostra.
13. Crie bloco próprio formMicrocopy (headline, suporte e instruções curtas).
14. Crie objectionHandlingSection cobrindo explicitamente:
    - "não é consultoria"
    - "é gerado por IA"
    - "serve para meu caso?"
15. Mantenha alinhamento total entre expectativa do clique e conteúdo entregue na landing.

Formato esperado:
JSON com:
artifact {
  artifactType: "experiment.landing.copy",
  artifactVersion: "v1",
  status: "DRAFT|VALIDATED|APPROVED",
  parentArtifactIds: [],
  content: {
    messageMatchSource,
    landingCurta {
  heroPromise,
  offerPromise,
  heroTitle,
  heroSubtitle,
  heroBullets,
  primaryCTA,
  formMicrocopy,
  formFields,
  benefitsSection,
  howItWorksSection,
  proofSection,
  offerSection,
  objectionHandlingSection,
  faqSection,
      closingCTA
    },
    landingCompleta {
  heroPromise,
  offerPromise,
  heroTitle,
  heroSubtitle,
  heroBullets,
  primaryCTA,
  formMicrocopy,
  formFields,
  benefitsSection,
  howItWorksSection,
  proofSection,
  offerSection,
  objectionHandlingSection,
  faqSection,
      closingCTA
    }
  }
}`;

const LANDING_LAYOUT_PROMPT_TEMPLATE = `${COMMON_PIPELINE_PROMPT}

Contexto do nicho: {nicho}
Dor principal: {primaryPain}
Promessa principal: {primaryPromise}
Mecanismo resumido: {mechanismSummary}
Prova resumida: {proofSummary}
CTA principal: {cta}

Textos da landing já definidos:
- Hero: {heroTitle}
- Subtítulo: {heroSubtitle}
- Benefícios: {benefitsSection}
- Como funciona: {howItWorksSection}
- Prova: {proofSection}
- Oferta: {offerSection}
- FAQ: {faqSection}

Objetivo:
Criar o wireframe textual da landing page.

Regras:
1. A estrutura deve deixar claro, logo no primeiro bloco, para qual nicho a página foi feita.
2. A página deve ser mobile-first.
3. O hero e o formulário devem aparecer sem exigir muito scroll.
4. O wireframe deve ser experimental, não apenas estrutural.
5. Adicione variantLayoutId para cada proposta com um valor entre:
   - form-first
   - proof-first
   - story-first
6. O layout base deve preservar:
   - hero + formulário acima da dobra
   - CTA recorrente
   - FAQ e compliance no footer
7. Cada seção deve ter uma função clara.
8. Adicione mobilePriorityScore por seção (inteiro de 1 a 10) para priorização em telas pequenas.
9. Adicione dropOffRisk por bloco com um valor entre: baixo, médio, alto.
10. Adicione sectionDependsOn para amarrar cada bloco ao dado de campanha:
   - hero ← primaryPromise
   - prova ← proofSummary
   - CTA ← primaryCTA
11. O CTA principal deve reaparecer em pontos estratégicos.
12. O layout deve minimizar atrito e reforçar continuidade com o anúncio.
13. Não usar linguagem de consultoria.
14. Não criar seções desnecessárias.
15. Cada seção deve declarar mediaSlot (none, image, illustration, chart, icon-set ou video-thumb) com a função visual esperada.
16. Cada seção deve declarar compositionNotes com instruções de composição e hierarquia visual para guiar a etapa de HTML.
17. Cada seção deve declarar surfaceSpec com: surfaceToken e notes (âncora estrutural da seção).
18. Use surfaceToken alternando entre surface-base e surface-alt-* para melhorar escaneabilidade entre seções consecutivas.
19. Não definir style/contrastMode no wireframe como fonte primária; esses campos pertencem ao landing-page-design-preset.sectionPresets.
20. Não transformar o layout em HTML final; foque apenas em ordem, hierarquia, slots e leitura mobile-first.
21. Se a estrutura puder servir para qualquer nicho, reescreva até ficar específica para {nicho}.
22. Defina formSpec como contrato estruturado único do formulário com:
    - formId: "lead-capture-primary"
    - title: "Receber a prévia do Kit (IA)"
    - submitLabel: "Desbloquear o Kit (receber a prévia gerada por IA)"
    - submitTarget: "#desbloquear"
    - fields exatamente nesta ordem:
      1) nome (text, required=true, placeholder "Seu nome")
      2) email (email, required=true, placeholder "voce@exemplo.com")
      3) whatsapp (tel, required=false, placeholder "(DDD) 9XXXX-XXXX")
    - consent: enabled=true, required=false, label preenchido
    - successState com title e message.

Formato esperado:
JSON com:
artifact {
  artifactType: "experiment.landing.layout",
  artifactVersion: "v1",
  status: "DRAFT|VALIDATED|APPROVED",
  parentArtifactIds: [],
  content: {
    pageGoal,
    variantLayoutId,
    sectionOrder [
  {
    "sectionName": "",
    "objective": "",
    "contentType": "",
    "uiNotes": "",
    "mediaSlot": "none|image|illustration|chart|icon-set|video-thumb",
    "compositionNotes": "",
    "mobilePriorityScore": 0,
    "dropOffRisk": "baixo|médio|alto",
    "sectionDependsOn": "",
    "surfaceSpec": {
      "surfaceToken": "surface-alt-1",
      "notes": "Seção com fundo alternado para contraste sutil com a anterior"
    }
      }
    ],
    mobilePriorityNotes,
    ctaPlacementNotes,
    formPlacementNotes,
    formSpec
  }
}`;

const LANDING_HTML_PROMPT_TEMPLATE = `${COMMON_PIPELINE_PROMPT}

Objetivo:
Unificar os textos da landing + wireframe + planejamento de imagens em um HTML final pronto para renderização no formulário do experimento.

Regras:
1. Entregar documento HTML completo com CSS e JavaScript embutidos.
2. Repetir literalmente o CTA principal definido nas etapas anteriores.
3. Layout mobile-first com formulário acima da dobra sempre que possível.
4. Implementar o envio com o mesmo conceito das páginas manuais:
   - interceptar submit com event.preventDefault();
   - validar form.checkValidity()/form.reportValidity();
   - enviar com fetch(form.action, { method: form.method.toUpperCase(), body: new FormData(form) });
   - desabilitar botão e exibir estado "Enviando..." durante a requisição;
   - mostrar sucesso inline sem redirecionamento e resetar o formulário;
   - tratar erro com feedback claro ao usuário.
5. Incluir bloco de compliance reforçando entrega digital via IA.
6. Consumir explicitamente os artefatos anteriores:
   - texto da landing para narrativa e message match;
   - layout da landing para ordem/hierarquia e mediaSlot;
   - planejamento de imagens para posicionamento visual e altText;
   - wireframe.formSpec para renderizar exatamente os campos e obrigatoriedade do formulário.
7. Aplicar superfícies por origem canônica dividida:
   - data-surface-token vem de wireframe.sectionOrder[i].surfaceSpec.surfaceToken
   - data-surface-style e data-surface-contrast vêm de landingPageDesignPreset.sectionPresets por sectionId
   - usar data-section-id na seção correspondente para binding determinístico.
8. Não inventar estrutura visual fora do layout/plano de imagens sem justificar nos consistencyChecks.
9. É proibido inventar/remover/renomear campos do formulário fora do formSpec.
10. Não usar dependências externas.
11. O form action deve usar exatamente formSpec.submitTarget.

Formato esperado:
JSON com:
artifact {
  artifactType: "experiment.landing.html",
  artifactVersion: "v1",
  status: "DRAFT|VALIDATED|APPROVED",
  parentArtifactIds: [],
  content: {
    htmlDocument,
    summary,
    consistencyChecks
  }
}

Regra obrigatória de imagens:
- Toda tag <img> deve ter src absoluto válido (https://... ou data:image/...).
- Toda tag <img> deve reutilizar altText e placement definidos no planejamento de imagens.
- Toda tag <img> deve declarar data-image-section-id e data-image-binding-key como binding canônico obrigatório do planejamento.
- Toda tag <img> também deve declarar data-image-role (semântico), data-conversion-role, data-attention-priority, data-visual-weight, data-distance-to-cta e data-supports-form-conversion.
- Nunca use caminhos relativos como "/assets/..." ou "./imagem.jpg" no htmlDocument.
- Se não houver imagem final disponível, renderize placeholder visual no CSS sem quebrar layout.`;

const LANDING_IMAGE_PLANNING_PROMPT_TEMPLATE = `${COMMON_PIPELINE_PROMPT}

Objetivo:
Criar o Planejamento de Imagens da Landing antes do HTML final, usando o ângulo da campanha, os textos da landing e o layout aprovado.

Regras:
1. Entregar images[] com no mínimo 4 imagens planejadas para seções reais da landing.
2. Cada imagem precisa incluir:
   - sectionId e sectionName
   - imageBindingKey (curto, estável e canônico)
   - imageRole
   - conversionRole
   - emotionalJob
   - sectionVisualGoal
   - placement (hero|benefit|mechanism|proof|offer|faq|cta)
   - priority (high|medium|low)
   - hierarchyLevel (primary|secondary|support)
   - attentionPriority (high|medium|low)
   - visualWeight (primary|secondary|support)
   - distanceToCTA (near|medium|far)
   - supportsFormConversion
   - formRelationNotes
   - objective
   - imagePrompt
   - altText
   - dimensions.desktop e dimensions.mobile
   - layoutBinding.preferredDesktopPlacement, layoutBinding.preferredMobilePlacement
   - layoutBinding.desktopAspectRatio, layoutBinding.mobileAspectRatio
   - layoutBinding.allowCrop
   - layoutBinding.safeCropZones.top/right/bottom/left
   - messageMatchNotes
3. Sempre incluir negativePrompt, complianceNotes e textOverlayGuidance.
4. Definir sequencingNotes e ctaIntegrationNotes para manter continuidade com o CTA.
5. Manter foco em clareza, informação e atratividade visual sem quebrar o ângulo da campanha.

Formato esperado:
JSON com:
artifact {
  artifactType: "experiment.landing.image_plan",
  artifactVersion: "v1",
  status: "DRAFT|VALIDATED|APPROVED",
  parentArtifactIds: [],
  content: {
    pageGoal,
    visualDirectionSummary,
    sequencingNotes,
    ctaIntegrationNotes,
    images,
    consistencyChecks
  }
}`;

const LANDING_DESIGN_PRESET_PROMPT_TEMPLATE = `${COMMON_PIPELINE_PROMPT}

Criar o Preset de Design da Landing após o planejamento de imagens e antes do HTML final.

Objetivo:
Estruturar tema visual reutilizável (paleta, estilos e presets por seção) mantendo coerência com copy, wireframe e planejamento de imagens.

Regras:
1. Entregar objeto landingPageDesignPreset com:
   - presetId
   - theme.palette
   - sectionPresets[]
   - componentPresets
   - motion
   - consistencyChecks[]
2. sectionPresets deve cobrir as sectionId existentes no wireframe atual.
3. surfaceStyle permitido: band | solid | gradient-soft | image-tint.
4. contrastMode permitido: normal | high | soft.
5. Não gerar HTML/CSS/JS nesta etapa.

Formato esperado:
JSON com envelope canônico:
{
  landingPageDesignPreset: {
    presetId,
    theme,
    sectionPresets,
    componentPresets,
    motion,
    consistencyChecks
  }
}`;

const SECTION_PROMPT_DEFAULTS: Partial<
  Record<ContentGenerationSectionKey, string>
> = {
  "ad-copy": AD_COPY_PROMPT_TEMPLATE,
  "landing-copy": LANDING_COPY_PROMPT_TEMPLATE,
  "landing-layout": LANDING_LAYOUT_PROMPT_TEMPLATE,
  "landing-image-planning": LANDING_IMAGE_PLANNING_PROMPT_TEMPLATE,
  "landing-design-preset": LANDING_DESIGN_PRESET_PROMPT_TEMPLATE,
  "landing-html": LANDING_HTML_PROMPT_TEMPLATE,
};

const SECTION_API_PATHS: Record<ContentGenerationSectionKey, string> = {
  "campaign-angle": "campaign-angle",
  "ad-copy": "ad-copy",
  "image-prompt": "ad-image-briefing",
  "landing-copy": "landing-page-copy",
  "landing-layout": "landing-page-wireframe",
  "landing-image-planning": "landing-page-image-planning",
  "landing-design-preset": "landing-page-design-preset",
  "landing-html": "landing-page-html",
};

interface ExperimentContentGenerationTabProps {
  experimentId: string;
  experimentName?: string;
  hypothesis?: Hypothesis;
  campaignAngle?: string | null;
  adCopy?: string | null;
}

interface AiGenerationRecord {
  id: number;
  domain: string;
  model?: string;
  prompt?: string;
  rawResponse?: string;
  createdAt?: string;
}

interface PageResponse<T> {
  content: T[];
}

interface CampaignAngleGenerationRow extends AiGenerationRecord {
  fields?: CampaignAngleSummary;
}

interface AdCopyGenerationRow extends AiGenerationRecord {
  fields?: AdCopyContent;
}

type SimpleGenerationRow = AiGenerationRecord;

interface PipelineReportRecord extends AiGenerationRecord {
  metadata: {
    sectionKey: ContentGenerationSectionKey;
    sectionLabel: string;
    sectionOrder: number;
  };
}

interface PersistedReportPayloads {
  campaignAngle?: CampaignAngleSummary;
  adCopy?: AdCopyContent;
}

type HistorySectionKey =
  | "image-prompt"
  | "landing-copy"
  | "landing-layout"
  | "landing-image-planning"
  | "landing-design-preset"
  | "landing-html";

const REPORT_SECTION_ORDER: ContentGenerationSectionKey[] = [
  "campaign-angle",
  "ad-copy",
  "image-prompt",
  "landing-copy",
  "landing-layout",
  "landing-image-planning",
  "landing-design-preset",
  "landing-html",
];

const REPORT_DOMAIN_ALIASES: Record<string, ContentGenerationSectionKey> = {
  "campaign-angle": "campaign-angle",
  "ad-copy": "ad-copy",
  "ad-image-briefing": "image-prompt",
  "image-prompt": "image-prompt",
  "landing-page-copy": "landing-copy",
  "landing-copy": "landing-copy",
  "landing-page-wireframe": "landing-layout",
  "landing-layout": "landing-layout",
  "landing-page-image-planning": "landing-image-planning",
  "landing-image-planning": "landing-image-planning",
  "landing-page-design-preset": "landing-design-preset",
  "landing-design-preset": "landing-design-preset",
  "landing-page-html": "landing-html",
  "landing-html": "landing-html",
};

function getFrameworkSummary(value?: string) {
  return value?.trim() || "Resumo ainda não preenchido na hipótese.";
}

function formatDateTime(value?: string) {
  if (!value) return "—";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleString("pt-BR", {
    day: "2-digit",
    month: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit",
  });
}

function formatDateTimeWithTimezone(value?: string) {
  if (!value) return "—";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleString("pt-BR", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit",
    timeZoneName: "short",
  });
}

function parseBackendError(errorMessage?: string): ParsedBackendError | null {
  if (!errorMessage) {
    return null;
  }
  const jsonStart = errorMessage.indexOf("{");
  const jsonEnd = errorMessage.lastIndexOf("}");
  if (jsonStart < 0 || jsonEnd <= jsonStart) {
    return null;
  }
  const raw = errorMessage.slice(jsonStart, jsonEnd + 1);
  try {
    const parsed = JSON.parse(raw) as ParsedBackendError;
    return parsed;
  } catch {
    return null;
  }
}

function parseTimestamp(value?: string) {
  if (!value) return undefined;
  const parsed = Date.parse(value);
  return Number.isNaN(parsed) ? undefined : parsed;
}

function normalizeImageMatchValue(value?: string) {
  return (value ?? "").trim().toLowerCase();
}

function normalizePromptFingerprint(value?: string) {
  return (value ?? "").replace(/\s+/g, " ").trim().toLowerCase();
}

function resolveGeneratedImageUrl(item: FrameworkImageStatusItem) {
  return item.webUrl ?? item.sourceUrl;
}

function mergeGeneratedImagesIntoLandingHtml(
  htmlDocument?: string,
  generatedImages: FrameworkImageStatusItem[] = [],
) {
  if (!htmlDocument?.trim() || generatedImages.length === 0) {
    return htmlDocument ?? "";
  }

  if (typeof window === "undefined" || typeof DOMParser === "undefined") {
    return htmlDocument;
  }

  const parser = new DOMParser();
  const parsed = parser.parseFromString(htmlDocument, "text/html");
  const imageElements = Array.from(parsed.querySelectorAll("img"));
  if (imageElements.length === 0) {
    return htmlDocument;
  }

  const availableImages = generatedImages
    .map((item) => ({
      url: resolveGeneratedImageUrl(item),
      planningItemKey: normalizeImageMatchValue(item.planningItemKey),
      sectionName: normalizeImageMatchValue(item.sectionName),
    }))
    .filter((item) => Boolean(item.url));

  if (availableImages.length === 0) {
    return htmlDocument;
  }

  const consumed = new Set<number>();
  const consumeImage = (index: number) => {
    consumed.add(index);
    return availableImages[index];
  };

  const findFirstUnusedByMatcher = (
    matcher: (candidate: (typeof availableImages)[number]) => boolean,
  ) => {
    const matchIndex = availableImages.findIndex(
      (candidate, index) => !consumed.has(index) && matcher(candidate),
    );
    if (matchIndex === -1) return undefined;
    return consumeImage(matchIndex);
  };

  imageElements.forEach((imgElement) => {
    const matchCandidates = [
      imgElement.getAttribute("data-planning-item-key"),
      imgElement.getAttribute("data-section-name"),
      imgElement.getAttribute("data-image-key"),
      imgElement.id,
      imgElement.getAttribute("alt"),
      imgElement.getAttribute("class"),
    ]
      .map((value) => normalizeImageMatchValue(value ?? undefined))
      .filter((value) => value.length > 0);

    const matched =
      findFirstUnusedByMatcher((candidate) =>
        matchCandidates.some(
          (value) =>
            value === candidate.planningItemKey ||
            value === candidate.sectionName ||
            value.includes(candidate.planningItemKey) ||
            value.includes(candidate.sectionName),
        ),
      ) ?? findFirstUnusedByMatcher(() => true);

    if (matched?.url) {
      imgElement.setAttribute("src", matched.url);
    }
  });

  return parsed.documentElement.outerHTML;
}

function tryFormatJsonBlock(raw: string): string | undefined {
  const trimmed = raw.trim();
  if (!(trimmed.startsWith("{") || trimmed.startsWith("["))) return undefined;
  try {
    return JSON.stringify(JSON.parse(trimmed), null, 2);
  } catch {
    return undefined;
  }
}

function extractReadableResult(rawResponse?: string): string | undefined {
  const trimmed = rawResponse?.trim();
  if (!trimmed) return undefined;

  try {
    const parsed = JSON.parse(trimmed) as {
      output_text?: unknown;
      output?: Array<{
        content?: Array<{
          type?: string;
          text?: unknown;
        }>;
      }>;
    };

    if (typeof parsed.output_text === "string" && parsed.output_text.trim()) {
      return parsed.output_text.trim();
    }

    const outputTextCandidate = parsed.output
      ?.flatMap((item) => item.content ?? [])
      .find((contentItem) => contentItem.type === "output_text");
    if (
      outputTextCandidate &&
      typeof outputTextCandidate.text === "string" &&
      outputTextCandidate.text.trim()
    ) {
      return outputTextCandidate.text.trim();
    }
  } catch {
    return undefined;
  }

  return undefined;
}

function wrapLongLines(content: string, maxLength = 88): string {
  return content
    .split("\n")
    .map((line) => {
      if (line.length <= maxLength) return line;
      const chunks: string[] = [];
      for (let index = 0; index < line.length; index += maxLength) {
        chunks.push(line.slice(index, index + maxLength));
      }
      return chunks.join("\n");
    })
    .join("\n");
}

interface PromptLineItem {
  kind: "text" | "title" | "json";
  content: string;
}

type PromptSegment = {
  type: "text" | "json";
  content: string;
};

function findJsonBoundary(content: string, startIndex: number) {
  const openingChar = content[startIndex];
  const closingChar = openingChar === "{" ? "}" : "]";
  let depth = 0;
  let inString = false;
  let isEscaped = false;

  for (let index = startIndex; index < content.length; index += 1) {
    const currentChar = content[index];

    if (inString) {
      if (isEscaped) {
        isEscaped = false;
        continue;
      }
      if (currentChar === "\\") {
        isEscaped = true;
        continue;
      }
      if (currentChar === '"') {
        inString = false;
      }
      continue;
    }

    if (currentChar === '"') {
      inString = true;
      continue;
    }

    if (currentChar === openingChar) {
      depth += 1;
      continue;
    }

    if (currentChar === closingChar) {
      depth -= 1;
      if (depth === 0) return index;
    }
  }

  return -1;
}

function splitTextWithInlineJson(content: string): PromptSegment[] {
  const segments: PromptSegment[] = [];
  let index = 0;
  let textStart = 0;

  while (index < content.length) {
    const char = content[index];
    const isPotentialJsonStart = char === "{" || char === "[";

    if (!isPotentialJsonStart) {
      index += 1;
      continue;
    }

    const boundary = findJsonBoundary(content, index);
    if (boundary === -1) {
      index += 1;
      continue;
    }

    const candidate = content.slice(index, boundary + 1).trim();
    try {
      JSON.parse(candidate);
    } catch {
      index += 1;
      continue;
    }

    const textBeforeJson = content.slice(textStart, index);
    if (textBeforeJson.trim()) {
      segments.push({ type: "text", content: textBeforeJson.trim() });
    }

    segments.push({
      type: "json",
      content: tryFormatJsonBlock(candidate) ?? candidate,
    });

    index = boundary + 1;
    textStart = index;
  }

  const remainingText = content.slice(textStart);
  if (remainingText.trim()) {
    segments.push({ type: "text", content: remainingText.trim() });
  }

  return segments;
}

function buildPromptLineItems(promptUsed: string): PromptLineItem[] {
  const normalized = promptUsed.replace(/\\n/g, "\n").replace(/\/n/g, "\n");

  return splitTextWithInlineJson(normalized).flatMap<PromptLineItem>(
    (segment) => {
      if (segment.type === "json") {
        return [{ kind: "json", content: segment.content }];
      }

      return segment.content.split("\n").map<PromptLineItem>((line) => {
        const trimmed = line.trim();
        if (!trimmed) return { kind: "text", content: "" };

        const inlineJsonMatch = trimmed.match(/^([^:]+):\s*([\[{].*)$/);
        if (inlineJsonMatch) {
          const [, title, jsonRaw] = inlineJsonMatch;
          const formattedJson = tryFormatJsonBlock(jsonRaw);
          if (formattedJson) {
            return {
              kind: "title",
              content: `${title}:__JSON__${formattedJson}`,
            };
          }
        }

        const isTitle = trimmed.endsWith(":") && !trimmed.startsWith("- ");
        return {
          kind: isTitle ? "title" : "text",
          content: line,
        };
      });
    },
  );
}

function getSectionMetadata(domain?: string) {
  if (!domain?.startsWith("experiment.pipeline.")) {
    return undefined;
  }

  const suffix = domain.replace("experiment.pipeline.", "");
  const sectionKey = REPORT_DOMAIN_ALIASES[suffix];
  if (!sectionKey) return undefined;

  const sectionOrder = REPORT_SECTION_ORDER.indexOf(sectionKey);
  if (sectionOrder < 0) return undefined;

  const sectionLabel =
    CONTENT_GENERATION_SECTIONS.find((section) => section.key === sectionKey)
      ?.label ?? sectionKey;

  return {
    sectionKey,
    sectionLabel,
    sectionOrder,
  };
}

export function selectLatestGenerationPerSection(
  generations: PipelineReportRecord[],
): PipelineReportRecord[] {
  const latestBySection = new Map<
    ContentGenerationSectionKey,
    PipelineReportRecord
  >();

  generations.forEach((generation) => {
    const current = latestBySection.get(generation.metadata.sectionKey);
    if (!current) {
      latestBySection.set(generation.metadata.sectionKey, generation);
      return;
    }

    const currentTimestamp = parseTimestamp(current.createdAt) ?? -1;
    const candidateTimestamp = parseTimestamp(generation.createdAt) ?? -1;
    const candidateIsMoreRecent =
      candidateTimestamp > currentTimestamp ||
      (candidateTimestamp === currentTimestamp && generation.id > current.id);

    if (candidateIsMoreRecent) {
      latestBySection.set(generation.metadata.sectionKey, generation);
    }
  });

  return [...latestBySection.values()].sort((a, b) => {
    if (a.metadata.sectionOrder !== b.metadata.sectionOrder) {
      return a.metadata.sectionOrder - b.metadata.sectionOrder;
    }

    return (
      (parseTimestamp(a.createdAt) ?? Number.MAX_SAFE_INTEGER) -
      (parseTimestamp(b.createdAt) ?? Number.MAX_SAFE_INTEGER)
    );
  });
}

export function resolvePersistedReportPayload(
  sectionKey: ContentGenerationSectionKey,
  payloads: PersistedReportPayloads,
): string | undefined {
  switch (sectionKey) {
    case "campaign-angle":
      return payloads.campaignAngle
        ? JSON.stringify(payloads.campaignAngle, null, 2)
        : undefined;
    case "ad-copy":
      return payloads.adCopy
        ? JSON.stringify(payloads.adCopy, null, 2)
        : undefined;
    default:
      return undefined;
  }
}

function getSectionLabel(sectionKey: ContentGenerationSectionKey) {
  return SECTION_LABEL_BY_KEY[sectionKey] ?? sectionKey;
}

function normalizeJobSection(
  value?: string,
): ContentGenerationSectionKey | undefined {
  if (!value) return undefined;
  const direct = JOB_SECTION_ALIASES[value];
  if (direct) return direct;
  const upper = value.toUpperCase();
  return JOB_SECTION_ALIASES[upper];
}

function getReferenceTimestamp(request: SectionRequestState) {
  return (
    parseTimestamp(request.completedAt) ?? parseTimestamp(request.requestedAt)
  );
}

function getWorkerStatus(request: SectionRequestState) {
  if (request.status === "FAILED") {
    return {
      label: "Worker IA retornou erro",
      badge: "danger",
    };
  }

  if (request.status === "COMPLETED") {
    return {
      label: "Worker IA concluiu com sucesso",
      badge: "success",
    };
  }

  if (
    request.status === "PROCESSING" ||
    request.startedAt ||
    request.stageLabel
  ) {
    return {
      label: "Worker IA em processamento",
      badge: "warning",
    };
  }

  if (request.status === "PENDING") {
    return {
      label: "Worker IA aguardando processamento",
      badge: "info",
    };
  }

  return {
    label: "Worker IA ainda não acionado",
    badge: "secondary",
  };
}

function getErrorMessage(error: unknown) {
  if (axios.isAxiosError(error)) {
    const responseMessage =
      typeof error.response?.data?.message === "string"
        ? error.response.data.message
        : undefined;
    return (
      responseMessage ?? error.message ?? "Falha ao processar solicitação."
    );
  }
  if (error instanceof Error) {
    return error.message;
  }
  return "Falha ao processar solicitação.";
}

export default function ExperimentContentGenerationTab({
  experimentId,
  experimentName,
  hypothesis,
  campaignAngle,
  adCopy,
}: ExperimentContentGenerationTabProps) {
  const [activeSection, setActiveSection] =
    useState<ContentGenerationSectionKey>(CONTENT_GENERATION_SECTIONS[0].key);
  const [isDownloadingReport, setIsDownloadingReport] = useState(false);
  const [campaignAngleGenerations, setCampaignAngleGenerations] = useState<
    CampaignAngleGenerationRow[]
  >([]);
  const [isLoadingCampaignAngles, setIsLoadingCampaignAngles] = useState(false);
  const [adCopyGenerations, setAdCopyGenerations] = useState<
    AdCopyGenerationRow[]
  >([]);
  const [isLoadingAdCopy, setIsLoadingAdCopy] = useState(false);
  const [sectionGenerations, setSectionGenerations] = useState<
    Record<HistorySectionKey, SimpleGenerationRow[]>
  >({
    "image-prompt": [],
    "landing-copy": [],
    "landing-layout": [],
    "landing-image-planning": [],
    "landing-design-preset": [],
    "landing-html": [],
  });
  const [isLoadingSectionGenerations, setIsLoadingSectionGenerations] =
    useState<Record<HistorySectionKey, boolean>>({
      "image-prompt": false,
      "landing-copy": false,
      "landing-layout": false,
      "landing-image-planning": false,
      "landing-design-preset": false,
      "landing-html": false,
    });
  const [requestsBySection, setRequestsBySection] = useState<
    Record<ContentGenerationSectionKey, SectionRequestState>
  >(() => ({ ...SECTION_REQUEST_INITIAL_STATE }));
  const [isGeneratingSection, setIsGeneratingSection] = useState(false);
  const [pendingGenerationSection, setPendingGenerationSection] =
    useState<ContentGenerationSectionKey | null>(null);
  const [isGeneratingWithLhm, setIsGeneratingWithLhm] = useState(false);
  const [autoQueue, setAutoQueue] = useState<AutoQueueState>(
    AUTO_QUEUE_INITIAL_STATE,
  );
  const [isAutoQueueRunning, setIsAutoQueueRunning] = useState(false);
  const lastCompletedLandingPlanningRef = useRef<number | null>(null);
  const imageGenerationWaitToastRef = useRef<string | null>(null);

  const jobsQuery = useExperimentPipelineJobs(experimentId);
  const frameworkImageStatusQuery = useFrameworkImageStatuses(experimentId);
  const generateFrameworkImages = useGenerateFrameworkImages(experimentId);
  const frameworkImageStatuses = frameworkImageStatusQuery.data ?? [];
  const hasFrameworkImages = frameworkImageStatuses.length > 0;
  const hasFrameworkImagesInFlight = frameworkImageStatuses.some((item) =>
    isFrameworkImageStatusInFlight(item.status),
  );
  const hasFrameworkImageFailure = frameworkImageStatuses.some(
    (item) => item.status?.toUpperCase() === "FAILED",
  );
  const hasFrameworkImageReady = frameworkImageStatuses.some(
    (item) => item.status?.toUpperCase() === "COMPLETED",
  );
  const shouldWaitLandingFlowForImages =
    autoQueue.isActive &&
    autoQueue.waitingFor === "landing-image-planning" &&
    (autoQueue.pending[0] === "landing-design-preset" ||
      autoQueue.pending[0] === "landing-html") &&
    (frameworkImageStatusQuery.isLoading ||
      !hasFrameworkImages ||
      hasFrameworkImagesInFlight);

  const requestsFromBackend = useMemo(
    () =>
      (jobsQuery.data ?? []).reduce<
        Record<ContentGenerationSectionKey, SectionRequestState>
      >(
        (acc, job) => {
          const sectionKey = normalizeJobSection(job.section);
          if (!sectionKey) {
            return acc;
          }
          if (acc[sectionKey]?.requestedAt) {
            return acc;
          }
          const status: RequestUiStatus =
            job.status === "COMPLETED"
              ? "COMPLETED"
              : job.status === "FAILED"
                ? "FAILED"
                : job.status === "PROCESSING"
                  ? "PROCESSING"
                  : "PENDING";
          const stageLabel =
            STAGE_LABELS[job.stage] ??
            (job.stage ? job.stage.split("_").join(" ") : undefined);
          acc[sectionKey] = {
            status,
            requestedAt: job.createdAt,
            startedAt: job.startedAt,
            completedAt: job.finishedAt,
            customInstructions: job.customInstructions,
            errorMessage: job.errorMessage,
            stageLabel,
            executionSource: job.model === "LHM" ? "LHM" : "WORKER_IA",
          };
          return acc;
        },
        { ...SECTION_REQUEST_INITIAL_STATE },
      ),
    [jobsQuery.data],
  );

  const latestJobIdBySection = useMemo(
    () =>
      (jobsQuery.data ?? []).reduce<
        Partial<Record<ContentGenerationSectionKey, string>>
      >((acc, job) => {
        const sectionKey = normalizeJobSection(job.section);
        if (!sectionKey || acc[sectionKey]) {
          return acc;
        }
        acc[sectionKey] = job.id;
        return acc;
      }, {}),
    [jobsQuery.data],
  );

  const mergedRequestsBySection = useMemo(
    () =>
      CONTENT_GENERATION_SECTIONS.reduce<
        Record<ContentGenerationSectionKey, SectionRequestState>
      >(
        (acc, section) => {
          const backendState = requestsFromBackend[section.key];
          const localState = requestsBySection[section.key];
          acc[section.key] =
            localState.status !== "IDLE" && backendState.status === "IDLE"
              ? localState
              : backendState;
          return acc;
        },
        { ...SECTION_REQUEST_INITIAL_STATE },
      ),
    [requestsFromBackend, requestsBySection],
  );

  const landingImagePlanningRequest =
    mergedRequestsBySection["landing-image-planning"];

  const landingImageFlowStatus = useMemo<LandingImageFlowStatus>(() => {
    if (!landingImagePlanningRequest.requestedAt) {
      return "NOT_STARTED";
    }
    if (!landingImagePlanningRequest.completedAt) {
      return "STARTED";
    }
    if (!hasFrameworkImages) {
      return "STARTED";
    }
    if (hasFrameworkImagesInFlight) {
      return "PROCESSING";
    }
    return "COMPLETED";
  }, [
    hasFrameworkImages,
    hasFrameworkImagesInFlight,
    landingImagePlanningRequest.completedAt,
    landingImagePlanningRequest.requestedAt,
  ]);

  const landingImageFlowMessage = useMemo(() => {
    if (landingImageFlowStatus === "NOT_STARTED") {
      return "Aguardando solicitação do planejamento de imagens.";
    }
    if (landingImageFlowStatus === "STARTED") {
      return "Planejamento finalizado. Disparo da geração das imagens iniciado.";
    }
    if (landingImageFlowStatus === "PROCESSING") {
      return "Geração das imagens em andamento.";
    }
    if (hasFrameworkImageFailure) {
      return "Geração finalizada com falhas em alguns itens.";
    }
    return "Geração das imagens concluída.";
  }, [hasFrameworkImageFailure, landingImageFlowStatus]);

  const lhmHistory = useMemo(
    () =>
      (jobsQuery.data ?? [])
        .filter(
          (job) =>
            normalizeJobSection(job.section) === "landing-html" &&
            job.model === "LHM",
        )
        .slice(0, 3),
    [jobsQuery.data],
  );

  const workerIaHistory = useMemo(
    () =>
      (jobsQuery.data ?? [])
        .filter(
          (job) =>
            normalizeJobSection(job.section) === "landing-html" &&
            job.model !== "LHM",
        )
        .slice(0, 3),
    [jobsQuery.data],
  );

  const invalidationBySection = useMemo(() => {
    const bySection: Partial<
      Record<ContentGenerationSectionKey, SectionInvalidationState>
    > = {};
    let latestDependency: SectionInvalidationState | null = null;

    REPORT_SECTION_ORDER.forEach((sectionKey) => {
      const currentRequest = mergedRequestsBySection[sectionKey];
      const currentTimestamp = getReferenceTimestamp(currentRequest);

      if (
        latestDependency &&
        currentRequest.status !== "PROCESSING" &&
        currentRequest.status !== "PENDING" &&
        (currentTimestamp === undefined ||
          latestDependency.requiresRefresh ||
          currentTimestamp < latestDependency.sourceTimestamp)
      ) {
        bySection[sectionKey] = latestDependency;
      }

      if (
        currentRequest.status === "PROCESSING" ||
        currentRequest.status === "PENDING"
      ) {
        const inFlightTimestamp =
          parseTimestamp(currentRequest.startedAt) ??
          parseTimestamp(currentRequest.requestedAt);
        if (inFlightTimestamp !== undefined) {
          latestDependency = {
            sourceSection: getSectionLabel(sectionKey),
            sourceAt: currentRequest.startedAt ?? currentRequest.requestedAt,
            sourceTimestamp: inFlightTimestamp,
            requiresRefresh: true,
          };
        }
        return;
      }

      if (
        currentTimestamp !== undefined &&
        (!latestDependency ||
          currentTimestamp > latestDependency.sourceTimestamp)
      ) {
        latestDependency = {
          sourceSection: getSectionLabel(sectionKey),
          sourceAt: currentRequest.completedAt ?? currentRequest.requestedAt,
          sourceTimestamp: currentTimestamp,
          requiresRefresh: false,
        };
      }
    });

    return bySection;
  }, [mergedRequestsBySection]);

  const frameworkContext = useMemo(
    () => ({
      pain: getFrameworkSummary(hypothesis?.framework?.pain?.summary),
      result: getFrameworkSummary(hypothesis?.framework?.result?.summary),
      mechanism: getFrameworkSummary(hypothesis?.framework?.mechanism?.summary),
      proof: getFrameworkSummary(hypothesis?.framework?.proof?.summary),
      offer: getFrameworkSummary(hypothesis?.framework?.offer?.summary),
    }),
    [
      hypothesis?.framework?.mechanism?.summary,
      hypothesis?.framework?.offer?.summary,
      hypothesis?.framework?.pain?.summary,
      hypothesis?.framework?.proof?.summary,
      hypothesis?.framework?.result?.summary,
    ],
  );

  const frameworkSummaryCards = useMemo(
    () => [
      {
        key: "pain",
        title: "Dor",
        description: "Problema central que precisa ser resolvido.",
        content: frameworkContext.pain,
      },
      {
        key: "result",
        title: "Resultado",
        description: "Transformação desejada pelo público.",
        content: frameworkContext.result,
      },
      {
        key: "mechanism",
        title: "Mecanismo",
        description: "Como a solução funciona na prática.",
        content: frameworkContext.mechanism,
      },
      {
        key: "proof",
        title: "Prova",
        description: "Evidências que sustentam a promessa.",
        content: frameworkContext.proof,
      },
      {
        key: "offer",
        title: "Oferta",
        description: "Estrutura da entrega e chamada para ação.",
        content: frameworkContext.offer,
      },
    ],
    [frameworkContext],
  );

  const persistedCampaignAngle = useMemo(
    () => parseCampaignAnglePayload(campaignAngle),
    [campaignAngle],
  );

  const persistedAdCopy = useMemo(() => parseAdCopyPayload(adCopy), [adCopy]);

  const promptVersionsBySection = useMemo(() => {
    const sources: {
      key: ContentGenerationSectionKey;
      label: string;
      items: SimpleGenerationRow[];
    }[] = [
      {
        key: "campaign-angle",
        label: getSectionLabel("campaign-angle"),
        items: campaignAngleGenerations,
      },
      {
        key: "ad-copy",
        label: getSectionLabel("ad-copy"),
        items: adCopyGenerations,
      },
      {
        key: "image-prompt",
        label: getSectionLabel("image-prompt"),
        items: sectionGenerations["image-prompt"],
      },
      {
        key: "landing-copy",
        label: getSectionLabel("landing-copy"),
        items: sectionGenerations["landing-copy"],
      },
      {
        key: "landing-layout",
        label: getSectionLabel("landing-layout"),
        items: sectionGenerations["landing-layout"],
      },
      {
        key: "landing-image-planning",
        label: getSectionLabel("landing-image-planning"),
        items: sectionGenerations["landing-image-planning"],
      },
      {
        key: "landing-design-preset",
        label: getSectionLabel("landing-design-preset"),
        items: sectionGenerations["landing-design-preset"],
      },
      {
        key: "landing-html",
        label: getSectionLabel("landing-html"),
        items: sectionGenerations["landing-html"],
      },
    ];

    return sources
      .map((source) => {
        const byFingerprint = new Map<
          string,
          {
            prompt: string;
            occurrences: number;
            latestUsedAt?: string;
            latestTimestamp: number;
            models: Set<string>;
          }
        >();

        source.items.forEach((item) => {
          const prompt = item.prompt?.trim();
          if (!prompt) return;
          const fingerprint = normalizePromptFingerprint(prompt);
          if (!fingerprint) return;
          const parsedTimestamp =
            parseTimestamp(item.createdAt) ?? Number.MIN_SAFE_INTEGER;
          const model = item.model?.trim();
          const current = byFingerprint.get(fingerprint);

          if (!current) {
            byFingerprint.set(fingerprint, {
              prompt,
              occurrences: 1,
              latestUsedAt: item.createdAt,
              latestTimestamp: parsedTimestamp,
              models: new Set(model ? [model] : []),
            });
            return;
          }

          current.occurrences += 1;
          if (parsedTimestamp >= current.latestTimestamp) {
            current.latestTimestamp = parsedTimestamp;
            current.latestUsedAt = item.createdAt;
          }
          if (model) {
            current.models.add(model);
          }
        });

        const versions = Array.from(byFingerprint.values())
          .sort((a, b) => b.latestTimestamp - a.latestTimestamp)
          .map((version) => ({
            prompt: version.prompt,
            occurrences: version.occurrences,
            latestUsedAt: version.latestUsedAt,
            models: Array.from(version.models).sort(),
          }));

        return {
          key: source.key,
          label: source.label,
          versions,
        };
      })
      .filter((section) => section.versions.length > 0);
  }, [adCopyGenerations, campaignAngleGenerations, sectionGenerations]);

  const isLoadingPromptVersions =
    isLoadingCampaignAngles ||
    isLoadingAdCopy ||
    Object.values(isLoadingSectionGenerations).some(Boolean);

  const currentSection =
    CONTENT_GENERATION_SECTIONS.find(
      (section) => section.key === activeSection,
    ) ?? CONTENT_GENERATION_SECTIONS[0];

  const sectionByKey = useMemo(
    () =>
      CONTENT_GENERATION_SECTIONS.reduce<
        Record<ContentGenerationSectionKey, ContentGenerationSection>
      >((acc, section) => {
        acc[section.key] = section;
        return acc;
      }, {} as Record<ContentGenerationSectionKey, ContentGenerationSection>),
    [],
  );

  const loadCampaignAngles = useCallback(async () => {
    try {
      setIsLoadingCampaignAngles(true);
      const { data: response } = await axios.get<PageResponse<AiGenerationRecord>>(
        "/api/ai/generations",
        {
          params: {
            referenceId: experimentId,
            domain: "experiment.pipeline.campaign-angle",
            size: 20,
          },
        },
      );

      const orderedByLatest = [...(response.content ?? [])]
        .map((generation) => ({
          ...generation,
          fields: parseCampaignAnglePayload(generation.rawResponse),
        }))
        .sort(
          (a, b) =>
            (parseTimestamp(b.createdAt) ?? Number.MIN_SAFE_INTEGER) -
            (parseTimestamp(a.createdAt) ?? Number.MIN_SAFE_INTEGER),
        );
      setCampaignAngleGenerations(orderedByLatest);
    } catch {
      toast.error("Não foi possível carregar os ângulos da campanha agora.");
    } finally {
      setIsLoadingCampaignAngles(false);
    }
  }, [experimentId]);

  const loadSection = useCallback(
    async (sectionKey: HistorySectionKey) => {
      try {
        setIsLoadingSectionGenerations((previous) => ({
          ...previous,
          [sectionKey]: true,
        }));
        const { data: response } = await axios.get<
          PageResponse<AiGenerationRecord>
        >("/api/ai/generations", {
          params: {
            referenceId: experimentId,
            domain: `experiment.pipeline.${SECTION_API_PATHS[sectionKey]}`,
            size: 20,
          },
        });

        const orderedByLatest = [...(response.content ?? [])].sort(
          (a, b) =>
            (parseTimestamp(b.createdAt) ?? Number.MIN_SAFE_INTEGER) -
            (parseTimestamp(a.createdAt) ?? Number.MIN_SAFE_INTEGER),
        );

        setSectionGenerations((previous) => ({
          ...previous,
          [sectionKey]: orderedByLatest,
        }));
      } catch {
        toast.error(
          `Não foi possível carregar as gerações da seção "${CONTENT_GENERATION_SECTIONS.find((section) => section.key === sectionKey)?.label ?? sectionKey}".`,
        );
      } finally {
        setIsLoadingSectionGenerations((previous) => ({
          ...previous,
          [sectionKey]: false,
        }));
      }
    },
    [experimentId],
  );

  const loadAdCopy = useCallback(async () => {
    try {
      setIsLoadingAdCopy(true);
      const { data: response } = await axios.get<PageResponse<AiGenerationRecord>>(
        "/api/ai/generations",
        {
          params: {
            referenceId: experimentId,
            domain: "experiment.pipeline.ad-copy",
            size: 20,
          },
        },
      );

      const orderedByLatest = [...(response.content ?? [])]
        .map((generation) => ({
          ...generation,
          fields: parseAdCopyPayload(generation.rawResponse),
        }))
        .sort(
          (a, b) =>
            (parseTimestamp(b.createdAt) ?? Number.MIN_SAFE_INTEGER) -
            (parseTimestamp(a.createdAt) ?? Number.MIN_SAFE_INTEGER),
        );

      setAdCopyGenerations(orderedByLatest);
    } catch {
      toast.error("Não foi possível carregar os textos do anúncio agora.");
    } finally {
      setIsLoadingAdCopy(false);
    }
  }, [experimentId]);

  useEffect(() => {
    void loadCampaignAngles();
  }, [loadCampaignAngles]);

  useEffect(() => {
    const sectionsToLoad: HistorySectionKey[] = [
      "image-prompt",
      "landing-copy",
      "landing-layout",
      "landing-image-planning",
      "landing-design-preset",
      "landing-html",
    ];

    void Promise.all(
      sectionsToLoad.map((sectionKey) => loadSection(sectionKey)),
    );
  }, [loadSection]);

  useEffect(() => {
    void loadAdCopy();
  }, [loadAdCopy]);

  useEffect(() => {
    if (lastCompletedLandingPlanningRef.current !== null) {
      return;
    }
    const existingCompletedAt =
      mergedRequestsBySection["landing-image-planning"].completedAt;
    if (!existingCompletedAt) {
      return;
    }
    const completedTimestamp = parseTimestamp(existingCompletedAt);
    if (completedTimestamp !== undefined) {
      lastCompletedLandingPlanningRef.current = completedTimestamp;
    }
  }, [mergedRequestsBySection]);

  useEffect(() => {
    const request = mergedRequestsBySection["landing-image-planning"];
    const completedTimestamp = parseTimestamp(request.completedAt);
    if (request.status !== "COMPLETED" || completedTimestamp === undefined) {
      return;
    }
    if (
      lastCompletedLandingPlanningRef.current !== null &&
      completedTimestamp <= lastCompletedLandingPlanningRef.current
    ) {
      return;
    }

    lastCompletedLandingPlanningRef.current = completedTimestamp;
    void (async () => {
      try {
        await generateFrameworkImages.mutateAsync();
        toast.success(
          "Planejamento de imagens concluído. Solicitação de geração das imagens enviada automaticamente.",
        );
      } catch (error) {
        toast.error(
          `Planejamento concluído, mas não foi possível solicitar as imagens automaticamente: ${getErrorMessage(error)}`,
        );
      }
    })();
  }, [generateFrameworkImages, mergedRequestsBySection]);

  useEffect(() => {
    if (!autoQueue.isActive || !autoQueue.waitingFor || isAutoQueueRunning) {
      imageGenerationWaitToastRef.current = null;
      return;
    }

    const waitingRequest = mergedRequestsBySection[autoQueue.waitingFor];
    if (waitingRequest.status === "FAILED") {
      setAutoQueue(AUTO_QUEUE_INITIAL_STATE);
      toast.error(
        `Fila automática interrompida: ${getSectionLabel(autoQueue.waitingFor)} finalizou com erro.`,
      );
      return;
    }

    if (waitingRequest.status !== "COMPLETED") {
      return;
    }

    if (autoQueue.pending.length === 0) {
      setAutoQueue(AUTO_QUEUE_INITIAL_STATE);
      imageGenerationWaitToastRef.current = null;
      toast.success("Fila automática finalizada. Todas as etapas foram solicitadas.");
      return;
    }

    const [nextSectionKey, ...remaining] = autoQueue.pending;
    if (
      autoQueue.waitingFor === "landing-image-planning" &&
      (nextSectionKey === "landing-design-preset" ||
        nextSectionKey === "landing-html")
    ) {
      const stillLoadingImageStatuses =
        frameworkImageStatusQuery.isLoading || frameworkImageStatusQuery.isFetching;
      const mustWaitForImages =
        stillLoadingImageStatuses ||
        !hasFrameworkImages ||
        hasFrameworkImagesInFlight;

      if (mustWaitForImages) {
        if (!imageGenerationWaitToastRef.current) {
          imageGenerationWaitToastRef.current = toast.info(
            "Planejamento concluído. Aguardando geração das imagens para continuar com as próximas etapas da landing.",
            { autoClose: 6000 },
          ) as unknown as string;
        }
        return;
      }

      imageGenerationWaitToastRef.current = null;
      if (!hasFrameworkImageReady && !hasFrameworkImageFailure) {
        return;
      }
    }

    const nextSection = sectionByKey[nextSectionKey];
    if (!nextSection) {
      setAutoQueue(AUTO_QUEUE_INITIAL_STATE);
      return;
    }

    setIsAutoQueueRunning(true);
    void (async () => {
      const started = await requestSectionGeneration(nextSection);
      if (!started) {
        setAutoQueue(AUTO_QUEUE_INITIAL_STATE);
      } else {
        setAutoQueue({
          isActive: true,
          waitingFor: nextSectionKey,
          pending: remaining,
        });
      }
      setIsAutoQueueRunning(false);
    })();
  }, [
    autoQueue,
    frameworkImageStatusQuery.isFetching,
    frameworkImageStatusQuery.isLoading,
    hasFrameworkImageFailure,
    hasFrameworkImageReady,
    hasFrameworkImages,
    hasFrameworkImagesInFlight,
    isAutoQueueRunning,
    mergedRequestsBySection,
    requestSectionGeneration,
    sectionByKey,
  ]);

  async function requestSectionGeneration(section: ContentGenerationSection) {
    const nowIso = new Date().toISOString();
    setPendingGenerationSection(section.key);
    setIsGeneratingSection(true);
    setRequestsBySection((previous) => ({
      ...previous,
      [section.key]: {
        status: "PROCESSING",
        requestedAt: nowIso,
      },
    }));

    try {
      await axios.post(
        `/api/experiments/${experimentId}/pipeline/${SECTION_API_PATHS[section.key]}/generate`,
        {
          quantity: section.defaultQuantity,
          promptTemplate: SECTION_PROMPT_DEFAULTS[section.key],
        },
      );
      toast.success(
        `Solicitação de ${section.label.toLowerCase()} enviada para o Worker IA.`,
      );
      await jobsQuery.refetch();

      if (section.key === "campaign-angle") {
        await loadCampaignAngles();
      } else if (section.key === "ad-copy") {
        await loadAdCopy();
      } else {
        await loadSection(section.key);
      }
      return true;
    } catch (error) {
      setRequestsBySection((previous) => ({
        ...previous,
        [section.key]: {
          status: "FAILED",
          requestedAt: nowIso,
          errorMessage: getErrorMessage(error),
        },
      }));
      toast.error(getErrorMessage(error));
      return false;
    } finally {
      setIsGeneratingSection(false);
      setPendingGenerationSection(null);
    }
  }

  const handleGenerateLandingDualHtml = useCallback(async () => {
    const nowIso = new Date().toISOString();
    try {
      setIsGeneratingWithLhm(true);
      setRequestsBySection((previous) => ({
        ...previous,
        "landing-html": {
          status: "PROCESSING",
          requestedAt: nowIso,
          startedAt: nowIso,
          executionSource: "LHM_IA",
          stageLabel: "Gerando duas variantes públicas (LHM + IA)",
        },
      }));
      await axios.post(
        `/api/experiments/${experimentId}/pipeline/landing-page-html/generate-with-lhm`,
      );
      await axios.post(
        `/api/experiments/${experimentId}/pipeline/landing-page-html/generate`,
        {
          quantity: 1,
          promptTemplate: SECTION_PROMPT_DEFAULTS["landing-html"],
        },
      );
      toast.success("Landing HTML gerado nas duas variantes: LHM (determinístico) e IA.");
      setRequestsBySection((previous) => ({
        ...previous,
        "landing-html": {
          status: "COMPLETED",
          requestedAt: previous["landing-html"]?.requestedAt ?? nowIso,
          startedAt: previous["landing-html"]?.startedAt ?? nowIso,
          completedAt: new Date().toISOString(),
          executionSource: "LHM_IA",
          stageLabel: "Finalizada",
        },
      }));
      await jobsQuery.refetch();
      await loadSection("landing-html");
    } catch (error) {
      const parsedError = parseBackendError(getErrorMessage(error));
      const summary = parsedError?.message ?? getErrorMessage(error);
      setRequestsBySection((previous) => ({
        ...previous,
        "landing-html": {
          status: "FAILED",
          requestedAt: previous["landing-html"]?.requestedAt ?? nowIso,
          startedAt: previous["landing-html"]?.startedAt ?? nowIso,
          completedAt: new Date().toISOString(),
          executionSource: "LHM_IA",
          stageLabel: "Falhou",
          errorMessage: summary,
        },
      }));
      toast.error(summary);
    } finally {
      setIsGeneratingWithLhm(false);
    }
  }, [experimentId, jobsQuery, loadSection]);

  const handleGenerateSection = useCallback(
    async (section: ContentGenerationSection) => {
      const sectionIndex = CONTENT_GENERATION_SECTIONS.findIndex(
        (item) => item.key === section.key,
      );
      const queuedSections = CONTENT_GENERATION_SECTIONS.slice(
        sectionIndex + 1,
      ).map((item) => item.key);

      setAutoQueue({
        isActive: queuedSections.length > 0,
        waitingFor: section.key,
        pending: queuedSections,
      });

      const started = await requestSectionGeneration(section);
      if (!started) {
        setAutoQueue(AUTO_QUEUE_INITIAL_STATE);
      } else if (queuedSections.length > 0) {
        toast.info(
          `Fila automática iniciada. As próximas etapas serão solicitadas em sequência (${queuedSections.length} pendente(s)).`,
        );
      }
    },
    [requestSectionGeneration],
  );

  const handleDownloadReport = async () => {
    try {
      setIsDownloadingReport(true);
      const { data: response } = await axios.get<
        PageResponse<AiGenerationRecord>
      >("/api/ai/generations", {
        params: {
          referenceId: experimentId,
          size: 100,
        },
      });

      const sortedPipelineGenerations: PipelineReportRecord[] = (
        response.content ?? []
      )
        .map((item) => ({
          ...item,
          metadata: getSectionMetadata(item.domain),
        }))
        .filter((item): item is PipelineReportRecord => item.metadata != null)
        .sort((a, b) => {
          if (a.metadata.sectionOrder !== b.metadata.sectionOrder) {
            return a.metadata.sectionOrder - b.metadata.sectionOrder;
          }
          return (
            (parseTimestamp(a.createdAt) ?? Number.MAX_SAFE_INTEGER) -
            (parseTimestamp(b.createdAt) ?? Number.MAX_SAFE_INTEGER)
          );
        });
      const pipelineGenerations = selectLatestGenerationPerSection(
        sortedPipelineGenerations,
      );
      const persistedPayloads: PersistedReportPayloads = {
        campaignAngle: persistedCampaignAngle,
        adCopy: persistedAdCopy,
      };

      const reportLines = [
        "# Relatório consolidado do pipeline de conteúdo do experimento",
        "",
        `Experimento: ${experimentName?.trim() || `#${experimentId}`}`,
        `Hipótese vinculada: ${hypothesis?.title?.trim() || "Não informada"}`,
        "",
        "## Prompts e resultados produzidos por seção",
        ...(pipelineGenerations.length === 0
          ? [
              "Nenhuma geração de IA do pipeline de experimento foi encontrada para este experimento.",
            ]
          : pipelineGenerations.flatMap((generation, index) => {
              const persistedPayload = resolvePersistedReportPayload(
                generation.metadata.sectionKey,
                persistedPayloads,
              );

              return [
                `### ${index + 1}. ${generation.metadata.sectionLabel}`,
                `- Data: ${formatDateTime(generation.createdAt)}`,
                `- Modelo: ${generation.model ?? "Não informado"}`,
                "",
                "**Prompt**",
                "```text",
                generation.prompt?.trim() || "Sem prompt registrado.",
                "```",
                "",
                "**Resultado (legível)**",
                "```text",
                extractReadableResult(generation.rawResponse) ??
                  "Não foi possível extrair um resumo legível desta resposta.",
                "```",
                "",
                "**Resposta bruta (JSON)**",
                "```json",
                wrapLongLines(
                  tryFormatJsonBlock(generation.rawResponse ?? "") ??
                    generation.rawResponse?.trim() ??
                    "Sem resposta registrada.",
                ),
                "```",
                "",
                "**Persistido no sistema**",
                ...(persistedPayload
                  ? ["```json", persistedPayload, "```"]
                  : [
                      "Nenhum payload persistido encontrado para esta seção no experimento.",
                    ]),
                "",
              ];
            })),
      ];

      const markdown = reportLines.join("\n");
      const blob = new Blob([markdown], { type: "text/markdown" });
      const url = URL.createObjectURL(blob);
      const downloadLink = document.createElement("a");
      downloadLink.href = url;
      downloadLink.download = `relatorio-pipeline-experimento-${experimentId}.md`;
      downloadLink.click();
      URL.revokeObjectURL(url);
    } catch {
      toast.error("Não foi possível baixar o relatório consolidado agora.");
    } finally {
      setIsDownloadingReport(false);
    }
  };

  return (
    <div className="mt-3 d-flex flex-column gap-3">
      <div className="d-flex justify-content-end">
        <button
          type="button"
          className="btn btn-outline-secondary btn-sm"
          onClick={handleDownloadReport}
          disabled={isDownloadingReport}
        >
          {isDownloadingReport ? (
            <span className="d-inline-flex align-items-center gap-1">
              <span
                className="spinner-border spinner-border-sm"
                role="status"
                aria-hidden="true"
              />
              Gerando relatório...
            </span>
          ) : (
            "Baixar relatório consolidado"
          )}
        </button>
      </div>

      <section className="card">
        <div className="card-body">
          <h5 className="card-title mb-1">Contexto do framework da hipótese</h5>
          <p className="text-muted mb-3">
            Esses resumos de Dor, Resultado, Mecanismo, Prova e Oferta serão
            enviados junto com cada solicitação para orientar o Worker IA.
          </p>
          <div className="row g-3">
            {frameworkSummaryCards.map((card) => (
              <div key={card.key} className="col-12 col-md-6 col-xl">
                <div className="border rounded p-3 h-100 bg-light-subtle d-flex flex-column gap-2">
                  <div>
                    <h6 className="mb-1">
                      Resumo da {card.title.toLowerCase()}
                    </h6>
                    <p className="mb-0 text-muted small">{card.description}</p>
                  </div>
                  <p className="mb-0 small lh-base">{card.content}</p>
                </div>
              </div>
            ))}
          </div>
        </div>
      </section>

      <PipelinePromptVersionsPanel
        versionsBySection={promptVersionsBySection}
        isLoading={isLoadingPromptVersions}
      />

      {autoQueue.isActive && autoQueue.waitingFor ? (
        <div className="alert alert-info py-2 px-3 mb-3" role="status">
          <div className="d-flex flex-wrap align-items-center gap-2">
            <strong>Fila automática ativa:</strong>
            <span>
              aguardando conclusão de{" "}
              <strong>{getSectionLabel(autoQueue.waitingFor)}</strong>.
            </span>
            {shouldWaitLandingFlowForImages ? (
              <span className="text-warning-emphasis">
                Desvio controlado: gerando imagens da landing antes das etapas
                finais.
              </span>
            ) : null}
            <span className="badge text-bg-light">
              Próximas: {autoQueue.pending.length}
            </span>
          </div>
        </div>
      ) : null}

      <div className="alert alert-light border py-2 px-3 mb-3" role="status">
        <div className="d-flex flex-wrap align-items-center gap-2">
          <strong>Fluxo das imagens da landing:</strong>
          <span
            className={`badge text-bg-${LANDING_IMAGE_FLOW_BADGES[landingImageFlowStatus]}`}
          >
            {LANDING_IMAGE_FLOW_LABELS[landingImageFlowStatus]}
          </span>
          <span className="small text-body-secondary">{landingImageFlowMessage}</span>
        </div>
      </div>

      <Tabs.Root
        value={activeSection}
        onValueChange={(value) =>
          setActiveSection(value as ContentGenerationSectionKey)
        }
      >
        <Tabs.List className="nav nav-pills flex-wrap gap-2">
          {CONTENT_GENERATION_SECTIONS.map((section) => (
            <Tabs.Trigger
              key={section.key}
              value={section.key}
              className={`btn ${
                activeSection === section.key
                  ? "btn-primary"
                  : "btn-outline-primary"
              }`}
            >
              {section.label}
            </Tabs.Trigger>
          ))}
        </Tabs.List>

        {CONTENT_GENERATION_SECTIONS.map((section) => (
          <Tabs.Content key={section.key} value={section.key} className="mt-3">
            {(() => {
              const isLandingFlowBlockedByImageGeneration =
                (section.key === "landing-design-preset" ||
                  section.key === "landing-html") &&
                (frameworkImageStatusQuery.isLoading ||
                  hasFrameworkImagesInFlight);
              return (
            <section className="card">
              <div className="card-body">
                <div className="d-flex justify-content-between align-items-start flex-wrap gap-2">
                  <div>
                    <h5 className="card-title mb-1">{section.label}</h5>
                    <p className="text-muted mb-0">{section.description}</p>
                    {section.key === "landing-html" ? (
                      <p className="small text-body-secondary mt-2 mb-0">
                        Nesta etapa, o sistema gera duas versões públicas da landing
                        com a mesma entrada canônica: uma determinística (LHM) e outra via IA.
                      </p>
                    ) : null}
                  </div>
                  <div className="d-flex align-items-start flex-wrap gap-2">
                    {section.key === "landing-html" ? (
                      <button
                        type="button"
                        className="btn btn-outline-primary btn-sm"
                        onClick={() => void handleGenerateLandingDualHtml()}
                        disabled={
                          isGeneratingWithLhm ||
                          isGeneratingSection ||
                          autoQueue.isActive ||
                          isLandingFlowBlockedByImageGeneration
                        }
                        title="Gera as duas versões públicas do HTML final com a mesma entrada: LHM determinístico + IA."
                      >
                        {isGeneratingWithLhm ? (
                          <span className="d-inline-flex align-items-center gap-1">
                            <span
                              className="spinner-border spinner-border-sm"
                              role="status"
                              aria-hidden="true"
                            />
                            Gerando LHM + IA...
                          </span>
                        ) : autoQueue.isActive ? (
                          <span className="d-inline-flex align-items-center gap-1">
                            <span
                              className="spinner-border spinner-border-sm"
                              role="status"
                              aria-hidden="true"
                            />
                            Fila em execução...
                          </span>
                        ) : isLandingFlowBlockedByImageGeneration ? (
                          <span className="d-inline-flex align-items-center gap-1">
                            <span
                              className="spinner-border spinner-border-sm"
                              role="status"
                              aria-hidden="true"
                            />
                            Gerando imagens...
                          </span>
                        ) : (
                          "Gerar HTML (LHM + IA)"
                        )}
                      </button>
                    ) : (
                      <button
                        type="button"
                        className="btn btn-outline-primary btn-sm"
                        onClick={() => void handleGenerateSection(section)}
                        disabled={
                          isGeneratingSection ||
                          isGeneratingWithLhm ||
                          autoQueue.isActive ||
                          isLandingFlowBlockedByImageGeneration
                        }
                        title={`Solicita ao Worker IA a geração da etapa "${section.label}" com prompt canônico do pipeline.`}
                      >
                        {isGeneratingSection &&
                        pendingGenerationSection === section.key ? (
                          <span className="d-inline-flex align-items-center gap-1">
                            <span
                              className="spinner-border spinner-border-sm"
                              role="status"
                              aria-hidden="true"
                            />
                            Gerando com IA...
                          </span>
                        ) : autoQueue.isActive ? (
                          <span className="d-inline-flex align-items-center gap-1">
                            <span
                              className="spinner-border spinner-border-sm"
                              role="status"
                              aria-hidden="true"
                            />
                            Fila em execução...
                          </span>
                        ) : isLandingFlowBlockedByImageGeneration ? (
                          <span className="d-inline-flex align-items-center gap-1">
                            <span
                              className="spinner-border spinner-border-sm"
                              role="status"
                              aria-hidden="true"
                            />
                            Gerando imagens...
                          </span>
                        ) : (
                          "Gerar com IA"
                        )}
                      </button>
                    )}
                    <span className="badge text-bg-light">
                      Estrutura pronta para prompt
                    </span>
                  </div>
                </div>

                <div className="row g-3 mt-1">
                  <div className="col-12">
                    {section.key === "campaign-angle" ? (
                      <>
                        <CampaignAngleSummaryPanel
                          isLoading={isLoadingCampaignAngles}
                          savedAngle={persistedCampaignAngle}
                          fallbackAngle={campaignAngleGenerations[0]?.fields}
                          fallbackTimestamp={
                            campaignAngleGenerations[0]?.createdAt
                          }
                          rawContent={campaignAngle}
                        />
                        <CampaignAngleHistoryList
                          generations={campaignAngleGenerations}
                          isLoading={isLoadingCampaignAngles}
                        />
                      </>
                    ) : section.key === "ad-copy" ? (
                      <>
                        <AdCopySummaryPanel
                          isLoading={isLoadingAdCopy}
                          savedCopy={persistedAdCopy}
                          fallbackCopy={adCopyGenerations[0]?.fields}
                          fallbackTimestamp={adCopyGenerations[0]?.createdAt}
                          rawContent={adCopy}
                        />
                        <AdCopyHistoryList
                          generations={adCopyGenerations}
                          isLoading={isLoadingAdCopy}
                        />
                      </>
                    ) : (
                      <>
                        <GenericGenerationSummaryPanel
                          experimentId={experimentId}
                          section={section}
                          isLoading={isLoadingSectionGenerations[section.key]}
                          latestGeneration={sectionGenerations[section.key][0]}
                          sourceJobId={latestJobIdBySection[section.key]}
                        />
                        <GenericGenerationHistoryList
                          section={section}
                          generations={sectionGenerations[section.key]}
                          isLoading={isLoadingSectionGenerations[section.key]}
                        />
                        {section.key === "landing-image-planning" ? (
                          <FrameworkImageGenerationPanel
                            onGenerate={async () => {
                              try {
                                await generateFrameworkImages.mutateAsync();
                                toast.success(
                                  "Solicitação de geração das imagens enviada para o Worker IA.",
                                );
                              } catch (error) {
                                toast.error(getErrorMessage(error));
                              }
                            }}
                            isGenerating={generateFrameworkImages.isPending}
                            statuses={frameworkImageStatusQuery.data ?? []}
                            isLoading={frameworkImageStatusQuery.isLoading}
                            isError={frameworkImageStatusQuery.isError}
                          />
                        ) : null}
                      </>
                    )}
                  </div>
                </div>

              </div>
            </section>
              );
            })()}
          </Tabs.Content>
        ))}
      </Tabs.Root>

      <section className="card border-0 shadow-sm">
        <div className="card-body">
          <div className="d-flex justify-content-between align-items-start flex-wrap gap-2">
            <div>
              <h5 className="card-title mb-1">
                Acompanhamento das solicitações IA
              </h5>
              <p className="text-muted mb-0">
                Monitoramos o status de cada etapa enviada ao Worker IA e
                avisamos quando uma dependência precisar ser refeita.
              </p>
            </div>
            <span className="badge text-bg-light">
              Atualiza automaticamente
            </span>
          </div>
          {jobsQuery.isLoading ? (
            <p className="small text-muted mt-3 mb-0">
              Carregando histórico das solicitações...
            </p>
          ) : null}
          {jobsQuery.isError ? (
            <p className="small text-danger mt-3 mb-0">
              Não foi possível carregar o histórico das solicitações salvas.
            </p>
          ) : null}
          <div className="d-flex flex-column gap-2 mt-3">
            {CONTENT_GENERATION_SECTIONS.map((section) => {
              const request = mergedRequestsBySection[section.key];
              const invalidation = invalidationBySection[section.key];
              const isLandingImagePlanningSection =
                section.key === "landing-image-planning";
              const effectiveRequestStatus: RequestUiStatus =
                isLandingImagePlanningSection &&
                request.status === "COMPLETED" &&
                landingImageFlowStatus !== "COMPLETED"
                  ? "PROCESSING"
                  : request.status;
              const waitingDependency = invalidation?.requiresRefresh === true;
              const displayStatus: RequestUiStatus =
                waitingDependency
                  ? "PROCESSING"
                  : effectiveRequestStatus === "PROCESSING" ||
                      effectiveRequestStatus === "PENDING"
                    ? "PROCESSING"
                    : invalidation
                      ? "INVALIDATED"
                      : effectiveRequestStatus;
              const workerStatus = getWorkerStatus(request);
              const executionSource = request.executionSource ?? "WORKER_IA";
              const backendError = parseBackendError(request.errorMessage);
              const displayWorkerStatus = waitingDependency
                ? {
                    label: "Aguardando atualização após dependência",
                    badge: "info",
                  }
                : workerStatus;

              return (
                <div
                  key={`pipeline-request-${section.key}`}
                  className="border rounded-2 p-2 d-flex flex-column gap-1"
                >
                  <div className="d-flex flex-wrap align-items-center gap-2">
                    <strong>{section.label}</strong>
                    <span
                      className={`badge text-bg-${STATUS_BADGES[displayStatus]}`}
                    >
                      {STATUS_LABELS[displayStatus]}
                    </span>
                  </div>
                  <small className="text-muted">
                    Solicitado em: {formatDateTime(request.requestedAt)} ·
                    Concluído em: {formatDateTime(request.completedAt)}
                  </small>
                  <div className="d-flex flex-wrap align-items-center gap-2">
                    <span className="small text-body-secondary">
                      Origem da execução:
                    </span>
                    <span
                      className={`badge text-bg-${EXECUTION_SOURCE_BADGES[executionSource]}`}
                    >
                      {EXECUTION_SOURCE_LABELS[executionSource]}
                    </span>
                  </div>
                  <div className="small d-flex flex-column gap-1">
                    <div>
                      <strong>1. Solicitação do usuário:</strong>{" "}
                      {request.requestedAt
                        ? `enviada em ${formatDateTime(request.requestedAt)}.`
                        : "ainda não enviada."}
                    </div>
                    <div className="d-flex flex-wrap align-items-center gap-2">
                      <strong>
                        2. Atendimento do{" "}
                        {EXECUTION_SOURCE_LABELS[executionSource]}:
                      </strong>
                      <span
                        className={`badge text-bg-${displayWorkerStatus.badge}`}
                      >
                        {displayWorkerStatus.label}
                      </span>
                      <span>
                        {waitingDependency
                          ? request.completedAt
                            ? `última execução finalizada em ${formatDateTime(request.completedAt)}`
                            : "aguardando nova execução"
                          : request.startedAt
                          ? `iniciado em ${formatDateTime(request.startedAt)}`
                          : request.status === "PENDING"
                            ? "aguardando início"
                            : "sem início registrado"}
                      </span>
                    </div>
                    <div>
                      <strong>3. Conclusão:</strong>{" "}
                      {waitingDependency
                        ? `aguardando conclusão de ${invalidation?.sourceSection} para atualizar esta etapa.`
                        : isLandingImagePlanningSection &&
                      landingImageFlowStatus !== "COMPLETED"
                        ? "aguardando conclusão da geração das imagens planejadas."
                        : request.completedAt
                        ? `finalizada em ${formatDateTime(request.completedAt)}.`
                        : request.status === "FAILED"
                          ? "fluxo encerrado com erro."
                          : "aguardando finalização."}
                    </div>
                  </div>
                  {isLandingImagePlanningSection ? (
                    <small className="text-body-secondary">
                      Etapa atual: {landingImageFlowMessage}
                    </small>
                  ) : waitingDependency && invalidation ? (
                    <small className="text-body-secondary">
                      Etapa atual: aguardando conclusão de{" "}
                      {invalidation.sourceSection} para atualização automática.
                    </small>
                  ) : request.stageLabel ? (
                    <small className="text-body-secondary">
                      Etapa atual: {request.stageLabel}
                    </small>
                  ) : null}
                  {request.customInstructions ? (
                    <small className="text-body-secondary">
                      Instruções enviadas: {request.customInstructions}
                    </small>
                  ) : null}
                  {section.key === "landing-html" &&
                  shouldWaitLandingFlowForImages ? (
                    <small className="text-warning-emphasis">
                      Fluxo em desvio controlado: estamos gerando as imagens da
                      landing para retomar automaticamente esta etapa ao final.
                    </small>
                  ) : null}
                  {invalidation && !invalidation.requiresRefresh ? (
                    <small className="text-warning-emphasis">
                      {`Dependência atualizada em ${invalidation.sourceSection} (${formatDateTime(
                        invalidation.sourceAt,
                      )}). Gere esta etapa novamente.`}
                    </small>
                  ) : null}
                  {request.errorMessage ? (
                    <small className="text-danger">
                      Último erro: {backendError?.message ?? request.errorMessage}
                      {backendError?.timestamp ? (
                        <>
                          {" "}
                          · retornado pelo backend em{" "}
                          {formatDateTimeWithTimezone(backendError.timestamp)}
                        </>
                      ) : null}
                    </small>
                  ) : null}
                </div>
              );
            })}
          </div>
          {lhmHistory.length > 0 || workerIaHistory.length > 0 ? (
            <div className="mt-3 d-flex flex-column gap-3">
              {[
                {
                  key: "lhm",
                  title: "Solicitações recentes do LHM (HTML da Landing)",
                  description:
                    "Execuções inline do LHM para geração de HTML da landing.",
                  badgeLabel: "LHM inline",
                  badgeClass: "info",
                  containerClass: "bg-info-subtle border-info-subtle",
                  jobs: lhmHistory,
                },
                {
                  key: "worker-ia",
                  title: "Solicitações recentes do Worker IA (HTML da Landing)",
                  description:
                    "Execuções do Worker IA para geração de HTML da landing.",
                  badgeLabel: "Worker IA",
                  badgeClass: "primary",
                  containerClass: "bg-primary-subtle border-primary-subtle",
                  jobs: workerIaHistory,
                },
              ].map((historyGroup) => (
                <div
                  key={historyGroup.key}
                  className={`border rounded-3 p-3 ${historyGroup.containerClass}`}
                >
                  <div className="d-flex flex-wrap align-items-center justify-content-between gap-2">
                    <div>
                      <h6 className="mb-1">{historyGroup.title}</h6>
                      <p className="small text-body-secondary mb-0">
                        {historyGroup.description}
                      </p>
                    </div>
                    <span className={`badge text-bg-${historyGroup.badgeClass}`}>
                      {historyGroup.badgeLabel}
                    </span>
                  </div>
                  {historyGroup.jobs.length > 0 ? (
                    <div className="d-flex flex-column gap-2 mt-3">
                      {historyGroup.jobs.map((job) => {
                        const parsedError = parseBackendError(job.errorMessage);
                        const statusBadge =
                          job.status === "COMPLETED"
                            ? "success"
                            : job.status === "FAILED"
                              ? "danger"
                              : "warning";
                        const statusLabel =
                          job.status === "COMPLETED"
                            ? "Concluída"
                            : job.status === "FAILED"
                              ? "Com erro"
                              : "Em processamento";
                        return (
                          <div
                            key={`${historyGroup.key}-history-${job.id}`}
                            className="bg-body border rounded-3 p-2"
                          >
                            <div className="d-flex flex-wrap align-items-center gap-2">
                              <strong>Tentativa #{job.id.slice(0, 8)}</strong>
                              <span className={`badge text-bg-${statusBadge}`}>{statusLabel}</span>
                            </div>
                            <div className="small mt-1 d-flex flex-column gap-1">
                              <span>
                                Solicitação registrada:{" "}
                                <strong>{formatDateTimeWithTimezone(job.createdAt)}</strong>
                              </span>
                              <span>
                                Início: <strong>{formatDateTimeWithTimezone(job.startedAt)}</strong>
                                {" · "}
                                Fim: <strong>{formatDateTimeWithTimezone(job.finishedAt)}</strong>
                              </span>
                              {parsedError?.timestamp ? (
                                <span>
                                  Erro retornado pelo backend em{" "}
                                  <strong>{formatDateTimeWithTimezone(parsedError.timestamp)}</strong>
                                </span>
                              ) : null}
                              {job.errorMessage ? (
                                <span className="text-danger">
                                  Motivo: {parsedError?.message ?? job.errorMessage}
                                </span>
                              ) : null}
                            </div>
                          </div>
                        );
                      })}
                    </div>
                  ) : (
                    <p className="small text-body-secondary mt-3 mb-0">
                      Nenhuma solicitação recente registrada para esta origem.
                    </p>
                  )}
                </div>
              ))}
            </div>
          ) : null}
        </div>
      </section>

      <small className="text-muted">
        Aba atual: <strong>{currentSection.label}</strong>
      </small>
    </div>
  );
}

interface GenericGenerationSummaryPanelProps {
  experimentId: string;
  section: ContentGenerationSection;
  isLoading: boolean;
  latestGeneration?: SimpleGenerationRow;
  sourceJobId?: string;
}

function GenericGenerationSummaryPanel({
  experimentId,
  section,
  isLoading,
  latestGeneration,
  sourceJobId,
}: GenericGenerationSummaryPanelProps) {
  const defaultPrompt = SECTION_PROMPT_DEFAULTS[section.key];
  const promptUsed = latestGeneration?.prompt?.trim() || defaultPrompt;
  const parsedContent = useMemo(
    () => parseSectionContent(section.key, latestGeneration?.rawResponse),
    [latestGeneration?.rawResponse, section.key],
  );

  const { hasStructuredData, preview } = resolveStructuredPreview(
    section.key,
    parsedContent,
    promptUsed,
    latestGeneration?.createdAt,
    experimentId,
  );
  const fallbackRaw = latestGeneration?.rawResponse?.trim();
  const hasData = hasStructuredData || Boolean(fallbackRaw);
  const badgeVariant = hasData ? "info" : "secondary";
  const badgeLabel = hasData
    ? "Última geração do Worker IA"
    : "Aguardando geração";

  return (
    <div className="card border-0 shadow-sm">
      <div className="card-body">
        <div className="d-flex justify-content-between align-items-start flex-wrap gap-2">
          <div>
            <p className="text-uppercase text-muted small fw-semibold mb-1">
              Saída estruturada do pipeline
            </p>
            <h6 className="mb-1">Conteúdo sintetizado</h6>
            <p className="text-muted small mb-0">
              Exibe a resposta mais recente gerada para esta etapa do pipeline.
            </p>
          </div>
          <div className="text-end">
            <span className={`badge text-bg-${badgeVariant}`}>
              {badgeLabel}
            </span>
            {hasData && latestGeneration?.createdAt ? (
              <p className="text-muted small mb-0 mt-1">
                Última atualização: {formatDateTime(latestGeneration.createdAt)}
              </p>
            ) : null}
            {sourceJobId ? (
              <Link
                to={`/experiments/${experimentId}/pipeline-jobs?section=${SECTION_API_PATHS[section.key]}&jobId=${sourceJobId}`}
                className="btn btn-link btn-sm p-0 mt-1"
              >
                Ver detalhe do job que gerou esta resposta
              </Link>
            ) : null}
          </div>
        </div>

        {isLoading ? (
          <div className="d-flex align-items-center gap-2 text-muted mt-3">
            <span
              className="spinner-border spinner-border-sm"
              role="status"
              aria-hidden="true"
            />
            Carregando conteúdo estruturado...
          </div>
        ) : hasStructuredData && preview ? (
          <div className="mt-3">{preview}</div>
        ) : fallbackRaw ? (
          <div className="border rounded p-3 mt-3 bg-light-subtle">
            <CollapsibleJsonViewer
              content={normalizeRawResponseForJsonViewer(fallbackRaw)}
              emptyMessage="Sem resposta registrada."
            />
          </div>
        ) : (
          <div className="alert alert-info mt-3 mb-0" role="status">
            Nenhum conteúdo disponível ainda. Solicite o Worker IA para
            preencher esta seção.
          </div>
        )}

        <details className="mt-3">
          <summary className="small text-muted">
            Ver referência do prompt interno desta seção
          </summary>
          <div className="alert alert-secondary mt-2 mb-0" role="status">
            <div className="fw-semibold">
              Prompt interno aplicado automaticamente.
            </div>
            <div className="small mt-1">
              Esta seção usa instruções codificadas na aplicação e contexto da
              hipótese.
            </div>
            {defaultPrompt ? (
              <pre
                className="small mb-0 mt-2"
                style={{ whiteSpace: "pre-wrap" }}
              >
                {defaultPrompt}
              </pre>
            ) : (
              <p className="small text-muted mb-0 mt-2">
                Não há template explícito para esta seção no frontend.
              </p>
            )}
          </div>
        </details>
      </div>
    </div>
  );
}

interface GenericGenerationHistoryListProps {
  section: ContentGenerationSection;
  generations: SimpleGenerationRow[];
  isLoading: boolean;
}

function GenericGenerationHistoryList({
  section,
  generations,
  isLoading,
}: GenericGenerationHistoryListProps) {
  return (
    <div className="card border-0 shadow-sm mt-3">
      <div className="card-body">
        <div className="d-flex justify-content-between align-items-center flex-wrap gap-2">
          <div>
            <h6 className="card-title mb-1">Histórico das gerações</h6>
            <p className="text-muted small mb-0">
              Visualize as respostas retornadas para esta etapa do pipeline.
            </p>
          </div>
          {generations.length > 0 ? (
            <span className="badge text-bg-light">
              {generations.length === 1
                ? "1 geração"
                : `${generations.length} gerações`}
            </span>
          ) : null}
        </div>

        {isLoading && generations.length === 0 ? (
          <div className="d-flex align-items-center gap-2 text-muted mt-3">
            <span
              className="spinner-border spinner-border-sm"
              role="status"
              aria-hidden="true"
            />
            Carregando histórico...
          </div>
        ) : generations.length === 0 ? (
          <p className="text-muted small mb-0 mt-3">
            Assim que você solicitar o Worker IA o histórico aparecerá aqui.
          </p>
        ) : (
          <div className="mt-3 d-flex flex-column gap-2">
            {generations.map((generation) => {
              const parsed = parseSectionContent(
                section.key,
                generation.rawResponse,
              );
              const summary = describeGenerationSummary(section.key, parsed);
              return (
                <div
                  key={generation.id}
                  className="border rounded p-3 bg-body-tertiary"
                >
                  <div className="d-flex justify-content-between align-items-start flex-wrap gap-2">
                    <p className="fw-semibold mb-1">
                      {formatDateTime(generation.createdAt)}
                    </p>
                    {generation.model ? (
                      <span className="badge text-bg-light">
                        {generation.model}
                      </span>
                    ) : null}
                  </div>
                  <p className="small text-muted mb-1 mt-2">{summary}</p>
                  {generation.rawResponse ? (
                    <details className="mt-1">
                      <summary className="small text-muted">
                        Ver JSON bruto
                      </summary>
                      <div className="mt-2">
                        <CollapsibleJsonViewer
                          content={normalizeRawResponseForJsonViewer(
                            generation.rawResponse,
                          )}
                          emptyMessage="Sem JSON bruto nesta geração."
                        />
                      </div>
                    </details>
                  ) : (
                    <p className="small text-muted mb-0">
                      Sem resposta registrada.
                    </p>
                  )}
                </div>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
}

function parseSectionContent(
  sectionKey: ContentGenerationSectionKey,
  raw?: string | null,
):
  | ImagePromptContent
  | LandingCopyContent
  | LandingLayoutContent
  | LandingImagePlanningContent
  | LandingHtmlContent
  | undefined {
  if (!raw) return undefined;
  switch (sectionKey) {
    case "image-prompt":
      return parseImagePromptPayload(raw);
    case "landing-copy":
      return parseLandingCopyPayload(raw);
    case "landing-layout":
      return parseLandingLayoutPayload(raw);
    case "landing-image-planning":
      return parseLandingImagePlanningPayload(raw);
    case "landing-design-preset":
      return undefined;
    case "landing-html":
      return parseLandingHtmlPayload(raw);
    default:
      return undefined;
  }
}

function normalizeRawResponseForJsonViewer(
  raw?: string | null,
): string | undefined {
  if (!raw) return undefined;
  const trimmed = raw.trim();
  if (!trimmed) return undefined;
  try {
    JSON.parse(trimmed);
    return trimmed;
  } catch {
    const [candidate] = extractObjectCandidates(trimmed);
    if (!candidate) return trimmed;
    return JSON.stringify(candidate, null, 2);
  }
}

function resolveStructuredPreview(
  sectionKey: ContentGenerationSectionKey,
  parsed?:
    | ImagePromptContent
    | LandingCopyContent
    | LandingLayoutContent
    | LandingImagePlanningContent
    | LandingHtmlContent,
  promptUsed?: string,
  executedAt?: string,
  experimentId?: string,
): { hasStructuredData: boolean; preview?: ReactNode } {
  switch (sectionKey) {
    case "image-prompt": {
      const content = parsed as ImagePromptContent | undefined;
      if (hasImagePromptContent(content)) {
        return {
          hasStructuredData: true,
          preview: (
            <ImagePromptPreview
              content={content}
              promptUsed={promptUsed}
              executedAt={executedAt}
            />
          ),
        };
      }
      return { hasStructuredData: false };
    }
    case "landing-copy": {
      const content = parsed as LandingCopyContent | undefined;
      if (hasLandingCopyContent(content)) {
        return {
          hasStructuredData: true,
          preview: <LandingCopyPreview content={content} />,
        };
      }
      return { hasStructuredData: false };
    }
    case "landing-layout": {
      const content = parsed as LandingLayoutContent | undefined;
      if (hasLandingLayoutContent(content)) {
        return {
          hasStructuredData: true,
          preview: (
            <LandingLayoutPreview
              content={content}
              promptUsed={promptUsed}
              executedAt={executedAt}
            />
          ),
        };
      }
      return { hasStructuredData: false };
    }
    case "landing-image-planning": {
      const content = parsed as LandingImagePlanningContent | undefined;
      if (hasLandingImagePlanningContent(content)) {
        return {
          hasStructuredData: true,
          preview: <LandingImagePlanningPreview content={content} />,
        };
      }
      return { hasStructuredData: false };
    }
    case "landing-design-preset":
      return { hasStructuredData: false };
    case "landing-html": {
      const content = parsed as LandingHtmlContent | undefined;
      if (hasLandingHtmlContent(content)) {
        return {
          hasStructuredData: true,
          preview: (
            <LandingHtmlPreview content={content} experimentId={experimentId} />
          ),
        };
      }
      return { hasStructuredData: false };
    }
    default:
      return { hasStructuredData: false };
  }
}

function describeGenerationSummary(
  sectionKey: ContentGenerationSectionKey,
  content?:
    | ImagePromptContent
    | LandingCopyContent
    | LandingLayoutContent
    | LandingImagePlanningContent
    | LandingHtmlContent,
): string {
  switch (sectionKey) {
    case "image-prompt": {
      const typed = content as ImagePromptContent | undefined;
      if (hasImagePromptContent(typed)) {
        const total = typed.briefings.length;
        return total === 1
          ? "1 briefing visual estruturado."
          : `${total} briefings visuais estruturados.`;
      }
      return "Sem briefings estruturados nesta geração.";
    }
    case "landing-copy": {
      const typed = content as LandingCopyContent | undefined;
      if (hasLandingCopyContent(typed)) {
        const blocks = [typed.landingCurta, typed.landingCompleta].filter(
          Boolean,
        ).length;
        return blocks === 2
          ? "Landing curta e completa atualizadas."
          : "Uma versão de landing estruturada nesta geração.";
      }
      return "Landing sem conteúdo interpretável.";
    }
    case "landing-layout": {
      const typed = content as LandingLayoutContent | undefined;
      if (hasLandingLayoutContent(typed)) {
        const total = typed.sectionOrder?.length ?? 0;
        return total > 0
          ? `${total} blocos desenhados no wireframe.`
          : "Wireframe atualizado sem blocos detalhados.";
      }
      return "Wireframe ainda não estruturado.";
    }
    case "landing-image-planning": {
      const typed = content as LandingImagePlanningContent | undefined;
      if (hasLandingImagePlanningContent(typed)) {
        const total = typed.images.length;
        return total > 0
          ? `${total} imagens planejadas para a landing.`
          : "Planejamento visual registrado sem imagens detalhadas.";
      }
      return "Planejamento de imagens registrado para a landing.";
    }
    case "landing-design-preset":
      return "Preset de design registrado para a landing.";
    case "landing-html": {
      const typed = content as LandingHtmlContent | undefined;
      if (hasLandingHtmlContent(typed)) {
        return typed.htmlDocument
          ? "Landing final em HTML/CSS/JS pronta para uso."
          : "HTML da landing atualizado.";
      }
      return "HTML final ainda não estruturado.";
    }
    default:
      return "Geração registrada.";
  }
}

function PromptUsedDetails({
  promptUsed,
  summaryLabel,
  executedAt,
}: {
  promptUsed?: string;
  summaryLabel?: string;
  executedAt?: string;
}) {
  if (!promptUsed) return null;
  const lineItems = buildPromptLineItems(promptUsed);

  return (
    <details className="mt-2">
      <summary className="small text-primary" role="button">
        {summaryLabel ?? "Ver prompt usado"}
      </summary>
      <div className="border rounded p-2 mt-2 bg-body-tertiary">
        <p className="text-muted small mb-1">
          Texto do prompt enviado ao Worker IA nesta geração:
        </p>
        <p className="small mb-2">
          <strong>Execução do job:</strong> {formatDateTime(executedAt)}
        </p>
        <div className="small d-flex flex-column gap-1">
          {lineItems.map((item, index) => {
            if (item.kind === "json") {
              return (
                <div key={`prompt-line-${index}`} className="small mb-1">
                  <CollapsibleJsonViewer
                    content={item.content}
                    emptyMessage="Sem bloco JSON nesta seção."
                  />
                </div>
              );
            }

            if (item.kind === "title" && item.content.includes(":__JSON__")) {
              const [title, json] = item.content.split(":__JSON__");
              return (
                <div key={`prompt-line-${index}`} className="mb-1">
                  <strong>{title}:</strong>
                  <div className="small mt-1">
                    <CollapsibleJsonViewer
                      content={json}
                      emptyMessage="Sem bloco JSON nesta seção."
                    />
                  </div>
                </div>
              );
            }

            if (item.kind === "title") {
              return (
                <p key={`prompt-line-${index}`} className="mb-0">
                  <strong>{item.content}</strong>
                </p>
              );
            }

            return (
              <p
                key={`prompt-line-${index}`}
                className="mb-0"
                style={{ whiteSpace: "pre-wrap" }}
              >
                {item.content}
              </p>
            );
          })}
        </div>
      </div>
    </details>
  );
}

interface PromptVersionSummary {
  key: string;
  label: string;
  versions: {
    prompt: string;
    occurrences: number;
    latestUsedAt?: string;
    models: string[];
  }[];
}

function PipelinePromptVersionsPanel({
  versionsBySection,
  isLoading,
}: {
  versionsBySection: PromptVersionSummary[];
  isLoading: boolean;
}) {
  return (
    <section className="card">
      <div className="card-body">
        <h5 className="card-title mb-1">Versões de prompts do pipeline</h5>
        <p className="text-muted mb-3">
          Visão consolidada de todos os prompts usados neste experimento, por
          seção do pipeline.
        </p>

        {isLoading ? (
          <div className="d-flex align-items-center gap-2 text-muted small">
            <span className="spinner-border spinner-border-sm" aria-hidden="true" />
            Carregando versões de prompts...
          </div>
        ) : null}

        {!isLoading && versionsBySection.length === 0 ? (
          <div className="alert alert-secondary mb-0" role="status">
            Nenhuma execução com prompt foi encontrada para este experimento.
          </div>
        ) : null}

        {!isLoading && versionsBySection.length > 0 ? (
          <div className="row g-3">
            {versionsBySection.map((section) => (
              <div className="col-12 col-xl-6" key={section.key}>
                <div className="border rounded p-3 h-100 bg-body-tertiary">
                  <div className="d-flex justify-content-between align-items-center gap-2 mb-2">
                    <h6 className="mb-0">{section.label}</h6>
                    <span className="badge text-bg-light">
                      {section.versions.length} versão(ões)
                    </span>
                  </div>
                  <div className="d-flex flex-column gap-2">
                    {section.versions.map((version, index) => (
                      <details key={`${section.key}-${index}`} className="border rounded p-2 bg-body">
                        <summary className="small fw-semibold" role="button">
                          Versão {index + 1} · {version.occurrences} execução(ões)
                        </summary>
                        <p className="small mb-1 mt-2">
                          <strong>Último uso:</strong> {formatDateTime(version.latestUsedAt)}
                        </p>
                        <p className="small mb-2">
                          <strong>Modelos:</strong> {version.models.join(", ") || "—"}
                        </p>
                        <pre className="small bg-body-secondary rounded p-2 mb-0">
                          {version.prompt}
                        </pre>
                      </details>
                    ))}
                  </div>
                </div>
              </div>
            ))}
          </div>
        ) : null}
      </div>
    </section>
  );
}

interface FrameworkImageGenerationPanelProps {
  statuses: FrameworkImageStatusItem[];
  isLoading: boolean;
  isError: boolean;
  isGenerating: boolean;
  onGenerate: () => Promise<void>;
}

type FrameworkImageStatusItem = {
  planningItemKey: string;
  sectionName?: string;
  status: string;
  stage?: string;
  model?: string;
  sourceUrl?: string;
  webUrl?: string;
  errorMessage?: string;
  startedAt?: string;
  finishedAt?: string;
  updatedAt?: string;
};

const FRAMEWORK_IMAGE_STATUS_BADGE: Record<string, string> = {
  PLANNED: "secondary",
  PENDING: "secondary",
  PROCESSING: "warning",
  COMPLETED: "success",
  FAILED: "danger",
};

const FRAMEWORK_IMAGE_STAGE_LABEL: Record<string, string> = {
  WAITING_AI_WORKER: "Aguardando AI Worker",
  CLAIMED: "Job em processamento",
  SENT_TO_OPENAI_BATCH: "Enviada para lote OpenAI",
  WAITING_OPENAI_BATCH: "Aguardando resposta OpenAI",
  OPENAI_IMAGE_READY: "Imagem da OpenAI pronta",
  UPLOADED_TO_CLOUDFLARE: "Upload no Cloudflare concluído",
  NOTIFIED_BACKEND: "Backend notificado",
  WAITING_WEBNIZATION: "Aguardando webnização",
  WEB_READY: "Versão web pronta",
  FAILED: "Falhou",
};

function isFrameworkImageStatusInFlight(status?: string) {
  const normalized = status?.toUpperCase();
  return (
    normalized === "PLANNED" ||
    normalized === "PENDING" ||
    normalized === "PROCESSING"
  );
}

function FrameworkImageGenerationPanel({
  statuses,
  isLoading,
  isError,
  isGenerating,
  onGenerate,
}: FrameworkImageGenerationPanelProps) {
  const sortedStatuses = useMemo(
    () =>
      [...statuses].sort(
        (a, b) =>
          (parseTimestamp(b.updatedAt) ?? Number.MIN_SAFE_INTEGER) -
          (parseTimestamp(a.updatedAt) ?? Number.MIN_SAFE_INTEGER),
      ),
    [statuses],
  );

  return (
    <div className="card border-0 shadow-sm mt-3">
      <div className="card-body">
        <div className="d-flex justify-content-between align-items-start flex-wrap gap-2">
          <div>
            <h6 className="card-title mb-1">Geração das imagens planejadas</h6>
            <p className="text-muted small mb-0">
              Dispare a geração final e acompanhe a timeline de cada item do
              planejamento até a versão web-ready.
            </p>
          </div>
          <button
            type="button"
            className="btn btn-success btn-sm"
            onClick={() => void onGenerate()}
            disabled={isGenerating}
          >
            {isGenerating ? (
              <span className="d-inline-flex align-items-center gap-1">
                <span
                  className="spinner-border spinner-border-sm"
                  role="status"
                  aria-hidden="true"
                />
                Gerando...
              </span>
            ) : (
              "Gerar imagens"
            )}
          </button>
        </div>

        {isLoading ? (
          <div className="d-flex align-items-center gap-2 text-muted mt-3">
            <span className="spinner-border spinner-border-sm" aria-hidden="true" />
            Carregando status de geração...
          </div>
        ) : null}
        {isError ? (
          <div className="alert alert-danger mt-3 mb-0" role="alert">
            Não foi possível carregar o status das imagens do framework.
          </div>
        ) : null}
        {!isLoading && !isError && sortedStatuses.length === 0 ? (
          <div className="alert alert-secondary mt-3 mb-0" role="status">
            Nenhum item encontrado ainda. Gere o planejamento e clique em
            <strong> Gerar imagens</strong> para iniciar o processamento.
          </div>
        ) : null}

        {!isLoading && !isError && sortedStatuses.length > 0 ? (
          <div className="table-responsive mt-3">
            <table className="table table-sm align-middle mb-0">
              <thead>
                <tr>
                  <th>Item</th>
                  <th>Status</th>
                  <th>Etapa atual</th>
                  <th>Modelo</th>
                  <th>Origem</th>
                  <th>Web</th>
                  <th>Atualização</th>
                </tr>
              </thead>
              <tbody>
                {sortedStatuses.map((item) => {
                  const normalizedStatus = item.status?.toUpperCase() ?? "PLANNED";
                  const statusLabel =
                    normalizedStatus === "PLANNED" ? "Planejada" : normalizedStatus;
                  const badge = FRAMEWORK_IMAGE_STATUS_BADGE[normalizedStatus] ?? "light";
                  const stageLabel = item.stage
                    ? FRAMEWORK_IMAGE_STAGE_LABEL[item.stage] ?? item.stage
                    : "Aguardando processamento";

                  return (
                    <tr key={item.planningItemKey}>
                      <td>
                        <div className="fw-semibold">{item.sectionName ?? item.planningItemKey}</div>
                        {item.sectionName &&
                        item.planningItemKey !== item.sectionName ? (
                          <div className="small text-muted">{item.planningItemKey}</div>
                        ) : null}
                      </td>
                      <td>
                        <span className={`badge text-bg-${badge}`}>{statusLabel}</span>
                        {item.errorMessage ? (
                          <div className="small text-danger mt-1">
                            Erro: {item.errorMessage}
                          </div>
                        ) : null}
                      </td>
                      <td className="small">{stageLabel}</td>
                      <td className="small">{item.model ?? "—"}</td>
                      <td className="small">
                        {item.sourceUrl ? (
                          <a href={item.sourceUrl} target="_blank" rel="noreferrer">
                            Ver origem
                          </a>
                        ) : (
                          "—"
                        )}
                      </td>
                      <td className="small">
                        {item.webUrl ? (
                          <a href={item.webUrl} target="_blank" rel="noreferrer">
                            Ver versão final
                          </a>
                        ) : (
                          "—"
                        )}
                      </td>
                      <td className="small">
                        {formatDateTime(item.updatedAt ?? item.finishedAt ?? item.startedAt)}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        ) : null}
      </div>
    </div>
  );
}

function ImagePromptPreview({
  content,
  promptUsed,
  executedAt,
}: {
  content: ImagePromptContent;
  promptUsed?: string;
  executedAt?: string;
}) {
  return (
    <div className="row g-3">
      {content.briefings.map((briefing, index) => (
        <div className="col-12 col-xl-4" key={`briefing-${index}`}>
          <div className="border rounded p-3 bg-light-subtle h-100">
            <div className="d-flex justify-content-between align-items-center mb-2">
              <span className="badge text-bg-primary">
                Briefing {index + 1}
              </span>
              {briefing.assetType ? (
                <span className="badge text-bg-light text-uppercase">
                  {briefing.assetType}
                </span>
              ) : null}
            </div>
            <p className="small mb-1">
              <strong>Mensagem espelhada:</strong>{" "}
              {briefing.mustMatchAdVariant ?? "—"}
            </p>
            <p className="small mb-1">
              <strong>Ângulo visual:</strong> {briefing.visualAngle ?? "—"}
            </p>
            <p className="small mb-1">
              <strong>Briefing visual:</strong> {briefing.visualBriefing ?? "—"}
            </p>
            <p className="small mb-1">
              <strong>Hierarquia:</strong> {briefing.hierarchy ?? "—"}
            </p>
            <p className="small mb-1">
              <strong>Formato por placement:</strong>{" "}
              {briefing.formatByPlacement ?? "—"}
            </p>
            <p className="small mb-1">
              <strong>Limite de palavras:</strong>{" "}
              {briefing.imageTextMaxWords
                ? `${briefing.imageTextMaxWords} palavras`
                : "—"}
            </p>
            {briefing.supportingKeywords?.length ? (
              <div className="small">
                <strong>Palavras-chave:</strong>
                <ul className="mb-0 mt-1">
                  {briefing.supportingKeywords.map((keyword) => (
                    <li key={keyword}>{keyword}</li>
                  ))}
                </ul>
              </div>
            ) : null}
            <PromptUsedDetails
              summaryLabel="Ver prompt usado no briefing"
              promptUsed={promptUsed}
              executedAt={executedAt}
            />
          </div>
        </div>
      ))}
    </div>
  );
}

function LandingCopyPreview({ content }: { content: LandingCopyContent }) {
  return (
    <div className="mt-3">
      {content.messageMatchSource ? (
        <div className="alert alert-warning py-2 mb-3">
          <strong>Message match:</strong> Espelhar headline "
          {content.messageMatchSource}".
        </div>
      ) : null}
      <div className="row g-3">
        <div className="col-12 col-xl-6">
          <LandingCopyVersionCard
            title="Landing curta"
            version={content.landingCurta}
          />
        </div>
        <div className="col-12 col-xl-6">
          <LandingCopyVersionCard
            title="Landing completa"
            version={content.landingCompleta}
          />
        </div>
      </div>
    </div>
  );
}

function LandingCopyVersionCard({
  title,
  version,
}: {
  title: string;
  version?: LandingCopyVersion;
}) {
  if (!version) {
    return (
      <div className="border rounded p-3 bg-body-tertiary h-100">
        <p className="text-muted small mb-0">Versão ainda não disponível.</p>
      </div>
    );
  }

  return (
    <div className="border rounded p-3 bg-body-tertiary h-100 d-flex flex-column gap-2">
      <div>
        <p className="text-uppercase small text-muted mb-1">{title}</p>
        <p className="small mb-0">
          <strong>Promessa:</strong>{" "}
          {version.heroPromise ?? version.offerPromise ?? "—"}
        </p>
      </div>
      {version.heroTitle ? (
        <div>
          <p className="fw-semibold mb-1">{version.heroTitle}</p>
          <p className="text-muted small mb-0">{version.heroSubtitle}</p>
        </div>
      ) : null}
      {version.heroBullets?.length ? (
        <ul className="small ps-3 mb-0">
          {version.heroBullets.map((bullet, bulletIndex) => (
            <li key={`${title}-bullet-${bulletIndex}`}>{bullet}</li>
          ))}
        </ul>
      ) : null}
      {version.primaryCTA ? (
        <p className="small mb-0">
          <strong>CTA principal:</strong> {version.primaryCTA}
        </p>
      ) : null}
      {version.formMicrocopy ? (
        <div className="small">
          <p className="fw-semibold mb-1">Microcopy do formulário</p>
          {version.formMicrocopy.headline && (
            <p className="mb-0">{version.formMicrocopy.headline}</p>
          )}
          {version.formMicrocopy.support && (
            <p className="text-muted mb-0">{version.formMicrocopy.support}</p>
          )}
          {version.formMicrocopy.instructions && (
            <p className="text-muted mb-0">
              {version.formMicrocopy.instructions}
            </p>
          )}
        </div>
      ) : null}
      {version.formFields ? (
        <LandingCopyFormFields fields={version.formFields} />
      ) : null}
      <LandingCopyBlockView
        label="Benefícios"
        block={version.benefitsSection}
      />
      <LandingCopyBlockView
        label="Como funciona"
        block={version.howItWorksSection}
      />
      <LandingCopyBlockView label="Prova" block={version.proofSection} />
      <LandingCopyBlockView label="Oferta" block={version.offerSection} />
      <LandingCopyBlockView
        label="Tratativa de objeções"
        block={version.objectionHandlingSection}
      />
      {version.faqSection?.length ? (
        <div className="small">
          <p className="fw-semibold mb-1">FAQ</p>
          <ul className="mb-0 ps-3">
            {version.faqSection.map((item, faqIndex) => (
              <li key={`${title}-faq-${faqIndex}`}>
                <strong>{item.question ?? "Pergunta"}</strong>
                {item.answer ? <p className="mb-0">{item.answer}</p> : null}
              </li>
            ))}
          </ul>
        </div>
      ) : null}
      {version.closingCTA ? (
        <p className="small mb-0">
          <strong>Fechamento:</strong> {version.closingCTA}
        </p>
      ) : null}
    </div>
  );
}

function LandingCopyFormFields({ fields }: { fields: LandingCopyFormField[] }) {
  return (
    <div className="small">
      <p className="fw-semibold mb-1">Campos sugeridos</p>
      <ul className="mb-0 ps-3">
        {fields.map((field, index) => (
          <li key={`field-${field.label ?? index}`}>
            {field.label ?? "Campo"}
            {field.required ? " (obrigatório)" : ""}
            {field.placeholder ? ` — ${field.placeholder}` : ""}
          </li>
        ))}
      </ul>
    </div>
  );
}

function LandingCopyBlockView({
  label,
  block,
}: {
  label: string;
  block?: LandingCopyBlock;
}) {
  if (!block) return null;
  return (
    <div className="small">
      <p className="fw-semibold mb-1">{label}</p>
      {block.title ? <p className="mb-0">{block.title}</p> : null}
      {block.description ? (
        <p className="text-muted mb-0">{block.description}</p>
      ) : null}
      {block.bullets?.length ? (
        <ul className="mb-0 ps-3">
          {block.bullets.map((item, index) => (
            <li key={`${label}-bullet-${index}`}>{item}</li>
          ))}
        </ul>
      ) : null}
    </div>
  );
}

function LandingLayoutPreview({
  content,
  promptUsed,
  executedAt,
}: {
  content: LandingLayoutContent;
  promptUsed?: string;
  executedAt?: string;
}) {
  return (
    <div className="mt-3 d-flex flex-column gap-3">
      <div className="row g-3">
        <div className="col-12 col-lg-4">
          <div className="border rounded p-3 bg-body-tertiary h-100">
            <p className="text-uppercase small text-muted mb-1">Objetivo</p>
            <p className="fw-semibold mb-1">{content.pageGoal ?? "—"}</p>
            <p className="text-muted small mb-0">
              Variante sugerida: {content.variantLayoutId ?? "—"}
            </p>
          </div>
        </div>
        <div className="col-12 col-lg-4">
          <div className="border rounded p-3 bg-body-tertiary h-100">
            <p className="text-uppercase small text-muted mb-1">CTA</p>
            <p className="small mb-1">
              {content.ctaPlacementNotes ?? "Sem observações"}
            </p>
            <p className="text-uppercase small text-muted mb-1">Formulário</p>
            <p className="small mb-0">
              {content.formPlacementNotes ?? "Sem instruções"}
            </p>
          </div>
        </div>
        <div className="col-12 col-lg-4">
          <div className="border rounded p-3 bg-body-tertiary h-100">
            <p className="text-uppercase small text-muted mb-1">
              Prioridade mobile
            </p>
            <p className="small mb-0">
              {content.mobilePriorityNotes ?? "Sem notas"}
            </p>
          </div>
        </div>
      </div>
      {content.sectionOrder?.length ? (
        <div className="d-flex flex-column gap-2">
          {content.sectionOrder.map((section, index) => (
            <div
              key={`${section.sectionName ?? index}`}
              className="border rounded p-3 bg-body-tertiary"
            >
              <div className="d-flex justify-content-between align-items-center gap-2">
                <strong>{section.sectionName || `Bloco ${index + 1}`}</strong>
                {section.mobilePriorityScore ? (
                  <span className="badge text-bg-light">
                    Prioridade {section.mobilePriorityScore}/10
                  </span>
                ) : null}
              </div>
              {section.objective ? (
                <p className="small mb-1">{section.objective}</p>
              ) : null}
              {section.mediaSlot ? (
                <p className="text-muted small mb-1">
                  Slot de mídia: {section.mediaSlot}
                </p>
              ) : null}
              {section.compositionNotes ? (
                <p className="small mb-1">{section.compositionNotes}</p>
              ) : null}
              {section.sectionDependsOn ? (
                <p className="text-muted small mb-1">
                  Depende de: {section.sectionDependsOn}
                </p>
              ) : null}
              {section.dropOffRisk ? (
                <span className="badge text-bg-warning text-uppercase me-auto">
                  Risco de drop: {section.dropOffRisk}
                </span>
              ) : null}
              <PromptUsedDetails
                summaryLabel="Ver prompt usado neste bloco"
                promptUsed={promptUsed}
                executedAt={executedAt}
              />
            </div>
          ))}
        </div>
      ) : (
        <p className="text-muted small mb-0">
          Nenhum bloco estrutural informado.
        </p>
      )}
    </div>
  );
}

function LandingHtmlPreview({
  content,
  experimentId,
}: {
  content: LandingHtmlContent;
  experimentId?: string;
}) {
  const [isApplying, setIsApplying] = useState(false);
  const canRender = Boolean(content.htmlDocument?.trim());
  const frameworkImageStatusQuery = useFrameworkImageStatuses(experimentId);
  const resolvedGeneratedImages = useMemo(
    () =>
      (frameworkImageStatusQuery.data ?? []).filter(
        (item) => Boolean(item.webUrl || item.sourceUrl),
      ),
    [frameworkImageStatusQuery.data],
  );
  const mergedHtmlPreview = useMemo(
    () =>
      mergeGeneratedImagesIntoLandingHtml(
        content.htmlDocument,
        resolvedGeneratedImages,
      ),
    [content.htmlDocument, resolvedGeneratedImages],
  );
  const deterministicUrl = content.deterministic?.publicUrl?.trim();
  const aiUrl = content.ai?.publicUrl?.trim();

  const handleApplyToForm = async () => {
    if (!experimentId) return;
    try {
      setIsApplying(true);
      const { data } = await axios.post<{
        publicUrl?: string | null;
        facebookPixelId?: string | null;
        pixelAppliedAutomatically?: boolean;
      }>(
        `/api/experiments/${experimentId}/pipeline/landing-page-html/approve-and-publish`,
      );
      const publicationUrl = data?.publicUrl;
      const pixelFeedback =
        data?.pixelAppliedAutomatically && data.facebookPixelId
          ? ` Pixel do nicho aplicado automaticamente (${data.facebookPixelId}).`
          : " Pixel do nicho será aplicado automaticamente ao ficar disponível.";
      toast.success(
        publicationUrl
          ? `Landing aprovada e publicada automaticamente em ${publicationUrl}.${pixelFeedback}`
          : `Landing aprovada e publicação automática iniciada.${pixelFeedback}`,
      );
    } catch (error) {
      toast.error(getErrorMessage(error));
    } finally {
      setIsApplying(false);
    }
  };

  return (
    <div className="d-flex flex-column gap-3 mt-3">
      {content.summary ? (
        <div className="alert alert-info py-2 mb-0">
          <strong>Resumo:</strong> {content.summary}
        </div>
      ) : null}
      <div className="alert alert-light border py-2 mb-0">
        <strong>Procedimento canônico:</strong> esta etapa trabalha com duas versões
        públicas da landing (LHM determinístico + IA) usando a mesma entrada
        canônica e validações equivalentes.
      </div>
      {deterministicUrl || aiUrl ? (
        <div className="card border-0 shadow-sm">
          <div className="card-body">
            <h6 className="card-title mb-2">URLs públicas geradas</h6>
            <div className="d-flex flex-column gap-2 small">
              {deterministicUrl ? (
                <div>
                  <strong>Determinística (LHM): </strong>
                  <a href={deterministicUrl} target="_blank" rel="noreferrer">
                    {deterministicUrl}
                  </a>
                </div>
              ) : null}
              {aiUrl ? (
                <div>
                  <strong>IA: </strong>
                  <a href={aiUrl} target="_blank" rel="noreferrer">
                    {aiUrl}
                  </a>
                </div>
              ) : null}
              {content.canonicalInputHash ? (
                <div className="text-muted">
                  Hash da entrada canônica: <code>{content.canonicalInputHash}</code>
                </div>
              ) : null}
            </div>
          </div>
        </div>
      ) : null}
      <div className="d-flex flex-wrap gap-2">
        <button
          type="button"
          className="btn btn-outline-primary"
          disabled={isApplying || !experimentId || !canRender}
          onClick={handleApplyToForm}
        >
          {isApplying ? (
            <span className="d-inline-flex align-items-center gap-1">
              <span
                className="spinner-border spinner-border-sm"
                role="status"
                aria-hidden="true"
              />
              Aplicando...
            </span>
          ) : (
            "Aprovar e publicar landing"
          )}
        </button>
      </div>
      {canRender ? (
        <div className="border rounded overflow-hidden">
          <iframe
            title="Pré-visualização da landing final"
            srcDoc={mergedHtmlPreview}
            style={{ width: "100%", height: "720px", border: "0" }}
            sandbox="allow-forms allow-modals allow-popups allow-same-origin allow-scripts"
          />
        </div>
      ) : (
        <p className="text-muted small mb-0">
          Sem HTML final estruturado nesta geração.
        </p>
      )}
      <div className="card border-0 shadow-sm">
        <div className="card-body">
          <div className="d-flex justify-content-between align-items-start flex-wrap gap-2">
            <div>
              <h6 className="card-title mb-1">Visão final: landing + imagens</h6>
              <p className="text-muted small mb-0">
                Centraliza o HTML final com as imagens geradas para revisão
                antes da aprovação.
              </p>
            </div>
            <span className="badge text-bg-light">
              {resolvedGeneratedImages.length} imagem(ns) com preview
            </span>
          </div>
          <div className="row g-3 mt-1">
            <div className="col-12 col-xxl-8">
              {canRender ? (
                <div className="border rounded overflow-hidden">
                  <iframe
                    title="Landing final com imagens geradas"
                    srcDoc={mergedHtmlPreview}
                    style={{ width: "100%", height: "560px", border: "0" }}
                    sandbox="allow-forms allow-modals allow-popups allow-same-origin allow-scripts"
                  />
                </div>
              ) : (
                <div className="alert alert-secondary mb-0" role="status">
                  Gere o HTML final para habilitar a visão consolidada.
                </div>
              )}
            </div>
            <div className="col-12 col-xxl-4">
              <div className="border rounded p-3 bg-body-tertiary h-100 d-flex flex-column gap-2">
                <p className="text-uppercase small text-muted fw-semibold mb-1">
                  Imagens geradas
                </p>
                {frameworkImageStatusQuery.isLoading ? (
                  <div className="d-flex align-items-center gap-2 text-muted small">
                    <span
                      className="spinner-border spinner-border-sm"
                      role="status"
                      aria-hidden="true"
                    />
                    Carregando imagens...
                  </div>
                ) : null}
                {frameworkImageStatusQuery.isError ? (
                  <div className="alert alert-danger py-2 mb-0 small" role="alert">
                    Não foi possível carregar as imagens geradas.
                  </div>
                ) : null}
                {!frameworkImageStatusQuery.isLoading &&
                !frameworkImageStatusQuery.isError &&
                resolvedGeneratedImages.length === 0 ? (
                  <div className="alert alert-secondary py-2 mb-0 small" role="status">
                    Nenhuma imagem final disponível ainda. Gere as imagens no
                    bloco de planejamento.
                  </div>
                ) : null}
                {!frameworkImageStatusQuery.isLoading &&
                !frameworkImageStatusQuery.isError &&
                resolvedGeneratedImages.length > 0 ? (
                  <div className="d-flex flex-column gap-2">
                    {resolvedGeneratedImages.map((item) => {
                      const imageUrl = item.webUrl ?? item.sourceUrl;
                      if (!imageUrl) return null;
                      return (
                        <div
                          key={`${item.planningItemKey}-${imageUrl}`}
                          className="border rounded p-2 bg-body"
                        >
                          <img
                            src={imageUrl}
                            alt={item.sectionName ?? item.planningItemKey}
                            className="img-fluid rounded border"
                            style={{ maxHeight: "140px", width: "100%", objectFit: "cover" }}
                          />
                          <p className="small fw-semibold mb-1 mt-2">
                            {item.sectionName ?? item.planningItemKey}
                          </p>
                          <div className="d-flex flex-wrap gap-2">
                            {item.webUrl ? (
                              <a
                                href={item.webUrl}
                                target="_blank"
                                rel="noreferrer"
                                className="small"
                              >
                                Abrir versão web
                              </a>
                            ) : null}
                            {item.sourceUrl ? (
                              <a
                                href={item.sourceUrl}
                                target="_blank"
                                rel="noreferrer"
                                className="small"
                              >
                                Abrir origem
                              </a>
                            ) : null}
                          </div>
                        </div>
                      );
                    })}
                  </div>
                ) : null}
              </div>
            </div>
          </div>
        </div>
      </div>
      {content.htmlDocument ? (
        <details>
          <summary className="small text-muted">Ver HTML bruto</summary>
          <pre className="bg-body-secondary rounded p-3 small mb-0 mt-2">
            {content.htmlDocument}
          </pre>
        </details>
      ) : null}
    </div>
  );
}

function LandingImagePlanningPreview({
  content,
}: {
  content: LandingImagePlanningContent;
}) {
  return (
    <div className="d-flex flex-column gap-3 mt-3">
      <div className="row g-3">
        <div className="col-12 col-lg-6">
          <div className="border rounded p-3 bg-body-tertiary h-100">
            <p className="text-uppercase small text-muted mb-1">
              Objetivo visual
            </p>
            <p className="fw-semibold mb-1">{content.pageGoal ?? "—"}</p>
            <p className="small text-muted mb-0">
              {content.visualDirectionSummary ??
                "Sem resumo da direção visual."}
            </p>
          </div>
        </div>
        <div className="col-12 col-lg-6">
          <div className="border rounded p-3 bg-body-tertiary h-100">
            <p className="text-uppercase small text-muted mb-1">
              Sequenciamento
            </p>
            <p className="small mb-2">
              {content.sequencingNotes ?? "Sem notas."}
            </p>
            <p className="text-uppercase small text-muted mb-1">
              Integração com CTA
            </p>
            <p className="small mb-0">
              {content.ctaIntegrationNotes ?? "Sem notas."}
            </p>
          </div>
        </div>
      </div>

      {content.images.length ? (
        <div className="row g-3">
          {content.images.map((image, index) => (
            <div
              className="col-12 col-xl-6"
              key={`${image.sectionId ?? "img"}-${index}`}
            >
              <div className="border rounded p-3 h-100 bg-light-subtle d-flex flex-column gap-2">
                <div className="d-flex justify-content-between align-items-center gap-2">
                  <strong>{image.sectionName ?? `Imagem ${index + 1}`}</strong>
                  <div className="d-flex align-items-center gap-2">
                    {image.priority ? (
                      <span className="badge text-bg-warning text-uppercase">
                        {image.priority}
                      </span>
                    ) : null}
                    {image.placement ? (
                      <span className="badge text-bg-light text-uppercase">
                        {image.placement}
                      </span>
                    ) : null}
                  </div>
                </div>
                {image.imageUrl ? (
                  <img
                    src={image.imageUrl}
                    alt={
                      image.altText ??
                      image.sectionName ??
                      `Imagem ${index + 1}`
                    }
                    className="img-fluid rounded border"
                    style={{ maxHeight: "220px", objectFit: "cover" }}
                  />
                ) : null}
                <p className="small mb-0">
                  <strong>Objetivo:</strong> {image.objective ?? "—"}
                </p>
                <p className="small mb-0">
                  <strong>Prompt:</strong> {image.imagePrompt ?? "—"}
                </p>
                <p className="small mb-0">
                  <strong>Alt text:</strong> {image.altText ?? "—"}
                </p>
                {image.negativePrompt ? (
                  <p className="small mb-0">
                    <strong>Negative prompt:</strong> {image.negativePrompt}
                  </p>
                ) : null}
                <p className="small mb-0">
                  <strong>Desktop/Mobile:</strong>{" "}
                  {image.desktopDimensions ?? "—"} /{" "}
                  {image.mobileDimensions ?? "—"}
                </p>
              </div>
            </div>
          ))}
        </div>
      ) : (
        <p className="text-muted small mb-0">
          Planejamento estruturado sem imagens com URL de visualização.
        </p>
      )}
    </div>
  );
}

interface AdCopySummaryPanelProps {
  isLoading: boolean;
  savedCopy?: AdCopyContent;
  fallbackCopy?: AdCopyContent;
  fallbackTimestamp?: string;
  rawContent?: string | null;
}

function AdCopySummaryPanel({
  isLoading,
  savedCopy,
  fallbackCopy,
  fallbackTimestamp,
  rawContent,
}: AdCopySummaryPanelProps) {
  const resolvedCopy = savedCopy ?? fallbackCopy;
  const hasData = hasAdCopyContent(resolvedCopy);
  const badgeVariant = savedCopy
    ? "success"
    : fallbackCopy
      ? "info"
      : "secondary";
  const badgeLabel = savedCopy
    ? "Sincronizado com o experimento"
    : fallbackCopy
      ? "Última geração do Worker IA"
      : "Aguardando geração";
  const rawText = rawContent?.trim();

  return (
    <div className="card border-0 shadow-sm">
      <div className="card-body">
        <div className="d-flex justify-content-between align-items-start flex-wrap gap-2">
          <div>
            <p className="text-uppercase text-muted small fw-semibold mb-1">
              Meta Ads • Dor / Resultado / Prova
            </p>
            <h6 className="mb-1">Textos sintetizados</h6>
            <p className="text-muted small mb-0">
              Exibe as variações retornadas pelo Worker IA para uso em anúncio.
            </p>
          </div>
          <div className="text-end">
            <span className={`badge text-bg-${badgeVariant}`}>
              {badgeLabel}
            </span>
            {!savedCopy && fallbackTimestamp ? (
              <p className="text-muted small mb-0 mt-1">
                Última atualização: {formatDateTime(fallbackTimestamp)}
              </p>
            ) : null}
          </div>
        </div>

        {isLoading ? (
          <div className="d-flex align-items-center gap-2 text-muted mt-3">
            <span
              className="spinner-border spinner-border-sm"
              role="status"
              aria-hidden="true"
            />
            Carregando textos estruturados...
          </div>
        ) : hasData ? (
          <div className="row g-3 mt-2">
            {resolvedCopy?.primaryTextVariants?.map((variant, index) => (
              <div
                className="col-12 col-xl-4"
                key={`${variant.label}-${index}`}
              >
                <div className="border rounded p-3 h-100 bg-light-subtle">
                  <p className="text-uppercase small fw-semibold text-muted mb-2">
                    {variant.label || `V${index + 1}`}
                  </p>
                  <p className="small mb-1">
                    <strong>Hook:</strong> {variant.openingHookType || "—"}
                  </p>
                  <p className="small mb-1">
                    <strong>Placement:</strong> {variant.placementHint || "—"}
                  </p>
                  <p className="small mb-1">
                    <strong>Texto curto:</strong>{" "}
                    {variant.lengthVariants?.curta ||
                      variant.primaryText ||
                      "—"}
                  </p>
                  <p className="small mb-1">
                    <strong>Texto médio:</strong>{" "}
                    {variant.lengthVariants?.media || "—"}
                  </p>
                  <p className="small mb-2">
                    <strong>Texto longo:</strong>{" "}
                    {variant.lengthVariants?.longa || "—"}
                  </p>
                  <p className="small mb-2">
                    <strong>Headline:</strong> {variant.headline || "—"}
                  </p>
                  <p className="small mb-2">
                    <strong>Descrição:</strong> {variant.description || "—"}
                  </p>
                  <p className="small mb-1">
                    <strong>CTA:</strong> {variant.ctaText || "—"}
                  </p>
                  <p className="small mb-0">
                    <strong>Compliance:</strong>{" "}
                    {variant.compliance?.semGarantiaAbsoluta &&
                    variant.compliance?.semPromessaIndividual &&
                    variant.compliance?.semLinguagemDeConsultoria
                      ? "OK"
                      : "Revisar"}
                  </p>
                </div>
              </div>
            ))}
          </div>
        ) : (
          <div className="alert alert-info mt-3" role="status">
            Nenhum texto do anúncio disponível ainda. Solicite o Worker IA para
            preencher os blocos.
          </div>
        )}

        {rawText && !hasData ? (
          <details className="mt-3">
            <summary className="small text-muted">
              Ver conteúdo bruto salvo
            </summary>
            <pre className="bg-body-secondary rounded p-3 small mb-0">
              {rawText}
            </pre>
          </details>
        ) : null}
      </div>
    </div>
  );
}

interface AdCopyHistoryListProps {
  generations: AdCopyGenerationRow[];
  isLoading: boolean;
}

function AdCopyHistoryList({ generations, isLoading }: AdCopyHistoryListProps) {
  return (
    <div className="card border-0 shadow-sm mt-3">
      <div className="card-body">
        <div className="d-flex justify-content-between align-items-center flex-wrap gap-2">
          <div>
            <h6 className="card-title mb-1">Histórico das gerações</h6>
            <p className="text-muted small mb-0">
              Visualize os textos do anúncio retornados para este experimento.
            </p>
          </div>
          {generations.length > 0 ? (
            <span className="badge text-bg-light">
              {generations.length === 1
                ? "1 geração"
                : `${generations.length} gerações`}
            </span>
          ) : null}
        </div>

        {isLoading && generations.length === 0 ? (
          <div className="d-flex align-items-center gap-2 text-muted mt-3">
            <span
              className="spinner-border spinner-border-sm"
              role="status"
              aria-hidden="true"
            />
            Carregando histórico...
          </div>
        ) : generations.length === 0 ? (
          <p className="text-muted small mb-0 mt-3">
            Assim que você solicitar o Worker IA o histórico aparecerá aqui.
          </p>
        ) : (
          <div className="mt-3 d-flex flex-column gap-2">
            {generations.map((generation) => (
              <div
                key={generation.id}
                className="border rounded p-3 bg-body-tertiary"
              >
                <div className="d-flex justify-content-between align-items-start flex-wrap gap-2">
                  <p className="fw-semibold mb-1">
                    {formatDateTime(generation.createdAt)}
                  </p>
                  {generation.model ? (
                    <span className="badge text-bg-light">
                      {generation.model}
                    </span>
                  ) : null}
                </div>
                <div className="mt-2 d-flex flex-column gap-2">
                  {generation.fields?.primaryTextVariants?.map(
                    (variant, index) => (
                      <div
                        className="border rounded p-2 bg-white"
                        key={`${generation.id}-${variant.label}-${index}`}
                      >
                        <p className="small fw-semibold mb-1">
                          {variant.label || `V${index + 1}`}
                        </p>
                        <p className="small mb-1">
                          <strong>Hook:</strong>{" "}
                          {variant.openingHookType || "—"}
                        </p>
                        <p className="small mb-1">
                          <strong>Placement:</strong>{" "}
                          {variant.placementHint || "—"}
                        </p>
                        <p className="small mb-1">
                          <strong>Texto curto:</strong>{" "}
                          {variant.lengthVariants?.curta ||
                            variant.primaryText ||
                            "—"}
                        </p>
                        <p className="small mb-1">
                          <strong>Texto médio:</strong>{" "}
                          {variant.lengthVariants?.media || "—"}
                        </p>
                        <p className="small mb-1">
                          <strong>Texto longo:</strong>{" "}
                          {variant.lengthVariants?.longa || "—"}
                        </p>
                        <p className="small mb-1">
                          <strong>Headline:</strong> {variant.headline || "—"}
                        </p>
                        <p className="small mb-1">
                          <strong>Descrição:</strong>{" "}
                          {variant.description || "—"}
                        </p>
                        <p className="small mb-1">
                          <strong>CTA:</strong> {variant.ctaText || "—"}
                        </p>
                        <p className="small mb-0">
                          <strong>Compliance:</strong>{" "}
                          {variant.compliance?.semGarantiaAbsoluta &&
                          variant.compliance?.semPromessaIndividual &&
                          variant.compliance?.semLinguagemDeConsultoria
                            ? "OK"
                            : "Revisar"}
                        </p>
                      </div>
                    ),
                  )}
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}

interface CampaignAngleSummaryPanelProps {
  isLoading: boolean;
  savedAngle?: CampaignAngleSummary;
  fallbackAngle?: CampaignAngleSummary;
  fallbackTimestamp?: string;
  rawContent?: string | null;
}

function CampaignAngleSummaryPanel({
  isLoading,
  savedAngle,
  fallbackAngle,
  fallbackTimestamp,
  rawContent,
}: CampaignAngleSummaryPanelProps) {
  const resolvedAngle = savedAngle ?? fallbackAngle;
  const hasData = hasCampaignAngleContent(resolvedAngle);
  const badgeVariant = savedAngle
    ? "success"
    : fallbackAngle
      ? "info"
      : "secondary";
  const badgeLabel = savedAngle
    ? "Sincronizado com o experimento"
    : fallbackAngle
      ? "Última geração do Worker IA"
      : "Aguardando geração";
  const rawText = rawContent?.trim();

  return (
    <div className="card border-0 shadow-sm">
      <div className="card-body">
        <div className="d-flex justify-content-between align-items-start flex-wrap gap-2">
          <div>
            <p className="text-uppercase text-muted small fw-semibold mb-1">
              Framework Dor → Resultado → Oferta
            </p>
            <h6 className="mb-1">Ângulo sintetizado</h6>
            <p className="text-muted small mb-0">
              Estrutura a promessa, dor, mecanismo e prova enviados pelo Worker
              IA.
            </p>
          </div>
          <div className="text-end">
            <span className={`badge text-bg-${badgeVariant}`}>
              {badgeLabel}
            </span>
            {!savedAngle && fallbackTimestamp ? (
              <p className="text-muted small mb-0 mt-1">
                Última atualização: {formatDateTime(fallbackTimestamp)}
              </p>
            ) : null}
          </div>
        </div>

        {isLoading ? (
          <div className="d-flex align-items-center gap-2 text-muted mt-3">
            <span
              className="spinner-border spinner-border-sm"
              role="status"
              aria-hidden="true"
            />
            Carregando ângulo estruturado...
          </div>
        ) : hasData ? (
          <>
            <div className="row g-3 mt-3">
              {[
                { label: "Dor principal", value: resolvedAngle?.primaryPain },
                {
                  label: "Resultado / Promessa",
                  value: resolvedAngle?.primaryPromise,
                },
                { label: "Mecanismo", value: resolvedAngle?.mechanismSummary },
                { label: "Prova", value: resolvedAngle?.proofSummary },
              ].map((block) => (
                <div className="col-12 col-md-6 col-xl-3" key={block.label}>
                  <div className="border rounded p-3 h-100 bg-light-subtle">
                    <p className="text-uppercase small fw-semibold text-muted mb-1">
                      {block.label}
                    </p>
                    <p className="mb-0">
                      {block.value ?? (
                        <span className="text-muted">
                          Ainda não preenchido.
                        </span>
                      )}
                    </p>
                  </div>
                </div>
              ))}
            </div>

            <div className="row g-3 mt-1">
              {[
                { label: "CTA recomendado", value: resolvedAngle?.cta },
                {
                  label: "Promessa single-minded",
                  value: resolvedAngle?.singleMindedPromise,
                },
                { label: "CTA principal", value: resolvedAngle?.primaryCTA },
                {
                  label: "Linha de match landing",
                  value: resolvedAngle?.landingMatchLine,
                },
                {
                  label: "Estágio de funil",
                  value: resolvedAngle?.funnelStage,
                },
                { label: "Tom sugerido", value: resolvedAngle?.tone },
              ].map((block) => (
                <div className="col-12 col-md-6 col-xl-4" key={block.label}>
                  <div className="border rounded p-3 h-100 bg-light-subtle">
                    <p className="text-uppercase small fw-semibold text-muted mb-1">
                      {block.label}
                    </p>
                    <p className="mb-0">
                      {block.value ?? (
                        <span className="text-muted">Ainda não definido.</span>
                      )}
                    </p>
                  </div>
                </div>
              ))}
            </div>
          </>
        ) : (
          <div className="alert alert-info mt-3" role="status">
            Nenhum ângulo disponível ainda. Solicite o Worker IA para preencher
            os blocos.
          </div>
        )}

        {rawText && !hasData ? (
          <details className="mt-3">
            <summary className="small text-muted">
              Ver conteúdo bruto salvo
            </summary>
            <pre className="bg-body-secondary rounded p-3 small mb-0">
              {rawText}
            </pre>
          </details>
        ) : null}
      </div>
    </div>
  );
}

interface CampaignAngleHistoryListProps {
  generations: CampaignAngleGenerationRow[];
  isLoading: boolean;
}

function CampaignAngleHistoryList({
  generations,
  isLoading,
}: CampaignAngleHistoryListProps) {
  return (
    <div className="card border-0 shadow-sm mt-3">
      <div className="card-body">
        <div className="d-flex justify-content-between align-items-center flex-wrap gap-2">
          <div>
            <h6 className="card-title mb-1">Histórico das gerações</h6>
            <p className="text-muted small mb-0">
              Visualize tudo que o Worker IA já retornou para este experimento.
            </p>
          </div>
          {generations.length > 0 ? (
            <span className="badge text-bg-light">
              {generations.length === 1
                ? "1 geração"
                : `${generations.length} gerações`}
            </span>
          ) : null}
        </div>

        {isLoading && generations.length === 0 ? (
          <div className="d-flex align-items-center gap-2 text-muted mt-3">
            <span
              className="spinner-border spinner-border-sm"
              role="status"
              aria-hidden="true"
            />
            Carregando histórico...
          </div>
        ) : generations.length === 0 ? (
          <p className="text-muted small mb-0 mt-3">
            Assim que você solicitar o Worker IA o histórico aparecerá aqui.
          </p>
        ) : (
          <div className="mt-3 d-flex flex-column gap-2">
            {generations.map((generation) => (
              <div
                key={generation.id}
                className="border rounded p-3 bg-body-tertiary"
              >
                <div className="d-flex justify-content-between align-items-start flex-wrap gap-2">
                  <div>
                    <p className="fw-semibold mb-1">
                      {generation.fields?.primaryPromise ??
                        "Promessa não estruturada"}
                    </p>
                    <p className="text-muted small mb-0">
                      {generation.fields?.primaryPain
                        ? `Dor: ${generation.fields.primaryPain}`
                        : "Dor principal não definida."}
                    </p>
                  </div>
                  <div className="text-end">
                    <span className="badge text-bg-light">
                      {formatDateTime(generation.createdAt)}
                    </span>
                    {generation.model ? (
                      <p className="text-muted small mb-0 mt-1">
                        {generation.model}
                      </p>
                    ) : null}
                  </div>
                </div>
                <div className="mt-2 d-flex flex-wrap gap-2 small">
                  {generation.fields?.funnelStage ? (
                    <span className="badge bg-body-secondary text-dark">
                      Funil: {generation.fields.funnelStage}
                    </span>
                  ) : null}
                  {generation.fields?.cta ? (
                    <span className="badge bg-body-secondary text-dark">
                      CTA: {generation.fields.cta}
                    </span>
                  ) : null}
                  {generation.fields?.tone ? (
                    <span className="badge bg-body-secondary text-dark">
                      Tom: {generation.fields.tone}
                    </span>
                  ) : null}
                </div>
                {generation.fields?.mechanismSummary ? (
                  <p className="small mb-1 mt-2">
                    <strong>Mecanismo:</strong>{" "}
                    {generation.fields.mechanismSummary}
                  </p>
                ) : null}
                {generation.fields?.proofSummary ? (
                  <p className="small mb-0 text-muted">
                    <strong>Prova:</strong> {generation.fields.proofSummary}
                  </p>
                ) : null}
                {generation.fields?.singleMindedPromise ? (
                  <p className="small mb-0 text-muted">
                    <strong>Promessa single-minded:</strong>{" "}
                    {generation.fields.singleMindedPromise}
                  </p>
                ) : null}
                {generation.fields?.primaryCTA ? (
                  <p className="small mb-0 text-muted">
                    <strong>CTA principal:</strong>{" "}
                    {generation.fields.primaryCTA}
                  </p>
                ) : null}
                {generation.fields?.landingMatchLine ? (
                  <p className="small mb-0 text-muted">
                    <strong>Match landing:</strong>{" "}
                    {generation.fields.landingMatchLine}
                  </p>
                ) : null}
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
