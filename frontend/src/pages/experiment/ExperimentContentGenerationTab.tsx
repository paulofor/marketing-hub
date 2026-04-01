import { useEffect, useMemo, useState } from "react";
import { toast } from "react-toastify";
import * as Tabs from "@radix-ui/react-tabs";
import axios from "axios";
import type { Hypothesis } from "../../api/hypothesis/useHypothesisBoard";

type ContentGenerationSectionKey =
  | "campaign-angle"
  | "ad-copy"
  | "image-prompt"
  | "landing-copy"
  | "landing-layout";

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
];

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
2. A primeira linha deve abrir com dor, consequência ou resultado desejado.
3. O mecanismo deve aparecer só depois do benefício principal.
4. O anúncio não pode parecer consultoria.
5. A promessa precisa ser compatível com ativos digitais gerados por IA.
6. Não usar jargão de tráfego pago.
7. Criar 3 variações:
   - V1 focada na dor
   - V2 focada no resultado
   - V3 focada na prova
8. O CTA deve combinar exatamente com a landing.
9. Entregar texto pensado para Meta Ads.

Formato esperado:
JSON com:
primaryTextVariants [
  {
    "label": "dor|resultado|prova",
    "primaryText": "",
    "headline": "",
    "description": "",
    "ctaText": ""
  }
]`;

const LANDING_LAYOUT_PROMPT_TEMPLATE = `${COMMON_PIPELINE_PROMPT}

Contexto do nicho: {nicho}
Persona: {persona}
Promessa principal: {primaryPromise}
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
1. A página deve ser mobile-first.
2. O hero e o formulário devem aparecer sem exigir muito scroll.
3. O layout deve seguir esta lógica:
   - promessa
   - credibilidade
   - mecanismo simples
   - prova
   - CTA
4. Cada seção deve ter uma função clara.
5. O CTA principal deve reaparecer em pontos estratégicos.
6. O layout deve minimizar atrito e reforçar continuidade com o anúncio.
7. Não criar seções desnecessárias.

Formato esperado:
JSON com:
pageGoal,
sectionOrder [
  {
    "sectionName": "",
    "objective": "",
    "contentType": "",
    "uiNotes": ""
  }
],
mobilePriorityNotes,
ctaPlacementNotes,
formPlacementNotes`;

const SECTION_PROMPT_DEFAULTS: Partial<
  Record<ContentGenerationSectionKey, string>
> = {
  "ad-copy": AD_COPY_PROMPT_TEMPLATE,
  "landing-layout": LANDING_LAYOUT_PROMPT_TEMPLATE,
};

const SECTION_API_PATHS: Record<ContentGenerationSectionKey, string> = {
  "campaign-angle": "campaign-angle",
  "ad-copy": "ad-copy",
  "image-prompt": "ad-image-briefing",
  "landing-copy": "landing-page-copy",
  "landing-layout": "landing-page-wireframe",
};

interface ExperimentContentGenerationTabProps {
  experimentId: string;
  experimentName?: string;
  hypothesis?: Hypothesis;
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

interface CampaignAngleResponseFields {
  primaryPromise: string;
  primaryPain: string;
  mechanismSummary: string;
  proofUsed: string;
  cta: string;
  funnelStage: string;
  tone: string;
}

interface CampaignAngleGenerationRow extends AiGenerationRecord {
  fields?: CampaignAngleResponseFields;
}

interface PipelineReportRecord extends AiGenerationRecord {
  metadata: {
    sectionKey: ContentGenerationSectionKey;
    sectionLabel: string;
    sectionOrder: number;
  };
}

const REPORT_SECTION_ORDER: ContentGenerationSectionKey[] = [
  "campaign-angle",
  "ad-copy",
  "image-prompt",
  "landing-copy",
  "landing-layout",
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
  });
}

function parseTimestamp(value?: string) {
  if (!value) return undefined;
  const parsed = Date.parse(value);
  return Number.isNaN(parsed) ? undefined : parsed;
}

function extractCampaignAngleFields(
  rawResponse?: string,
): CampaignAngleResponseFields | undefined {
  if (!rawResponse?.trim()) return undefined;

  const safeParseJson = (value: string): unknown => {
    try {
      return JSON.parse(value) as unknown;
    } catch {
      return undefined;
    }
  };

  const pickText = (value: unknown): string | undefined => {
    if (typeof value !== "string") return undefined;
    const trimmed = value.trim();
    return trimmed.length > 0 ? trimmed : undefined;
  };

  const parseEmbeddedJson = (text: string): Record<string, unknown> | undefined => {
    const direct = safeParseJson(text);
    if (direct && typeof direct === "object" && !Array.isArray(direct)) {
      return direct as Record<string, unknown>;
    }

    const fenced = text.match(/```(?:json)?\s*([\s\S]*?)```/i)?.[1]?.trim();
    if (fenced) {
      const parsedFence = safeParseJson(fenced);
      if (parsedFence && typeof parsedFence === "object" && !Array.isArray(parsedFence)) {
        return parsedFence as Record<string, unknown>;
      }
    }

    const firstBrace = text.indexOf("{");
    const lastBrace = text.lastIndexOf("}");
    if (firstBrace >= 0 && lastBrace > firstBrace) {
      const candidate = text.slice(firstBrace, lastBrace + 1);
      const parsedCandidate = safeParseJson(candidate);
      if (
        parsedCandidate &&
        typeof parsedCandidate === "object" &&
        !Array.isArray(parsedCandidate)
      ) {
        return parsedCandidate as Record<string, unknown>;
      }
    }

    return undefined;
  };

  const collectObjects = (value: unknown): Record<string, unknown>[] => {
    if (value == null) return [];

    if (Array.isArray(value)) {
      return value.flatMap((item) => collectObjects(item));
    }

    if (typeof value !== "object") {
      if (typeof value === "string") {
        const embedded = parseEmbeddedJson(value);
        return embedded ? [embedded] : [];
      }
      return [];
    }

    const asRecord = value as Record<string, unknown>;
    const nestedFromStrings = Object.values(asRecord).flatMap((item) =>
      typeof item === "string"
        ? (parseEmbeddedJson(item) ? [parseEmbeddedJson(item)!] : [])
        : [],
    );

    return [asRecord, ...Object.values(asRecord).flatMap((item) => collectObjects(item)), ...nestedFromStrings];
  };

  const parsedRoot = safeParseJson(rawResponse);
  const candidates = collectObjects(parsedRoot ?? rawResponse);

  for (const payload of candidates) {
    const primaryPromise =
      pickText(payload.primaryPromise) ??
      pickText(payload.campaignAngle) ??
      pickText(payload.mainPromise) ??
      "";
    const primaryPain = pickText(payload.primaryPain) ?? pickText(payload.pain) ?? "";
    const mechanismSummary =
      pickText(payload.mechanismSummary) ?? pickText(payload.mechanism) ?? "";
    const proofUsed =
      pickText(payload.proofUsed) ?? pickText(payload.proofSummary) ?? "";
    const cta = pickText(payload.cta) ?? pickText(payload.callToAction) ?? "";
    const funnelStage =
      pickText(payload.funnelStage) ?? pickText(payload.funnelStageName) ?? "";
    const tone = pickText(payload.tone) ?? pickText(payload.toneOfVoice) ?? "";

    if (primaryPromise || primaryPain || mechanismSummary || proofUsed || cta || funnelStage || tone) {
      return {
        primaryPromise,
        primaryPain,
        mechanismSummary,
        proofUsed,
        cta,
        funnelStage,
        tone,
      };
    }
  }

  return undefined;
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

export default function ExperimentContentGenerationTab({
  experimentId,
  experimentName,
  hypothesis,
}: ExperimentContentGenerationTabProps) {
  const [activeSection, setActiveSection] =
    useState<ContentGenerationSectionKey>(CONTENT_GENERATION_SECTIONS[0].key);
  const [isDownloadingReport, setIsDownloadingReport] = useState(false);
  const [campaignAngleGenerations, setCampaignAngleGenerations] = useState<
    CampaignAngleGenerationRow[]
  >([]);
  const [isLoadingCampaignAngles, setIsLoadingCampaignAngles] = useState(false);
  const [isRequestingBySection, setIsRequestingBySection] = useState<
    Record<ContentGenerationSectionKey, boolean>
  >(() =>
    CONTENT_GENERATION_SECTIONS.reduce(
      (acc, section) => ({ ...acc, [section.key]: false }),
      {} as Record<ContentGenerationSectionKey, boolean>,
    ),
  );

  const frameworkContext = useMemo(
    () => ({
      pain: getFrameworkSummary(hypothesis?.framework?.pain?.summary),
      result: getFrameworkSummary(hypothesis?.framework?.result?.summary),
      offer: getFrameworkSummary(hypothesis?.framework?.offer?.summary),
    }),
    [
      hypothesis?.framework?.offer?.summary,
      hypothesis?.framework?.pain?.summary,
      hypothesis?.framework?.result?.summary,
    ],
  );

  const currentSection =
    CONTENT_GENERATION_SECTIONS.find(
      (section) => section.key === activeSection,
    ) ?? CONTENT_GENERATION_SECTIONS[0];

  useEffect(() => {
    const loadCampaignAngles = async () => {
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
            fields: extractCampaignAngleFields(generation.rawResponse),
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
    };

    void loadCampaignAngles();
  }, [experimentId]);

  const handleRequest = async (sectionKey: ContentGenerationSectionKey) => {
    try {
      setIsRequestingBySection((previous) => ({
        ...previous,
        [sectionKey]: true,
      }));

      const sectionPath = SECTION_API_PATHS[sectionKey];
      await axios.post(
        `/api/experiments/${experimentId}/pipeline/${sectionPath}/generate`,
        {
          customInstructions: "Quantidade sugerida: 1",
        },
      );

      toast.success(
        "Solicitação enviada ao backend com sucesso. O Worker IA poderá processar a fila em seguida.",
      );
    } catch {
      toast.error(
        "Não foi possível enviar a solicitação para o backend neste momento.",
      );
    } finally {
      setIsRequestingBySection((previous) => ({
        ...previous,
        [sectionKey]: false,
      }));
    }
  };

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

      const pipelineGenerations: PipelineReportRecord[] = (response.content ?? [])
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
          : pipelineGenerations.flatMap((generation, index) => [
              `### ${index + 1}. ${generation.metadata.sectionLabel}`,
              `- Data: ${formatDateTime(generation.createdAt)}`,
              `- Modelo: ${generation.model ?? "Não informado"}`,
              "",
              "**Prompt**",
              "```text",
              generation.prompt?.trim() || "Sem prompt registrado.",
              "```",
              "",
              "**Resultado**",
              "```text",
              generation.rawResponse?.trim() || "Sem resposta registrada.",
              "```",
              "",
            ])),
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
            Esses resumos de Dor-Resultado-Oferta serão enviados junto com cada
            solicitação para orientar o Worker IA.
          </p>
          <div className="row g-3">
            <div className="col-md-4">
              <div className="border rounded p-3 h-100 bg-light-subtle">
                <h6 className="mb-2">Resumo da dor</h6>
                <p className="mb-0 small">{frameworkContext.pain}</p>
              </div>
            </div>
            <div className="col-md-4">
              <div className="border rounded p-3 h-100 bg-light-subtle">
                <h6 className="mb-2">Resumo do resultado</h6>
                <p className="mb-0 small">{frameworkContext.result}</p>
              </div>
            </div>
            <div className="col-md-4">
              <div className="border rounded p-3 h-100 bg-light-subtle">
                <h6 className="mb-2">Resumo da oferta</h6>
                <p className="mb-0 small">{frameworkContext.offer}</p>
              </div>
            </div>
          </div>
        </div>
      </section>

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
              className="btn btn-outline-primary"
            >
              {section.label}
            </Tabs.Trigger>
          ))}
        </Tabs.List>

        {CONTENT_GENERATION_SECTIONS.map((section) => (
          <Tabs.Content key={section.key} value={section.key} className="mt-3">
            <section className="card">
              <div className="card-body">
                <div className="d-flex justify-content-between align-items-start flex-wrap gap-2">
                  <div>
                    <h5 className="card-title mb-1">{section.label}</h5>
                    <p className="text-muted mb-0">{section.description}</p>
                  </div>
                  <span className="badge text-bg-light">
                    Estrutura pronta para prompt
                  </span>
                </div>

                <div className="row g-3 mt-1">
                  <div className="col-12">
                    {section.key === "campaign-angle" ? (
                      <>
                        <label className="form-label mb-1">
                          Ângulos da campanha obtidos do modelo
                        </label>
                        {isLoadingCampaignAngles ? (
                          <div className="d-flex align-items-center gap-2 text-muted small">
                            <span
                              className="spinner-border spinner-border-sm"
                              role="status"
                              aria-hidden="true"
                            />
                            Carregando ângulos...
                          </div>
                        ) : campaignAngleGenerations.length > 0 ? (
                          <div className="d-flex flex-column gap-2">
                            {campaignAngleGenerations.slice(0, 1).map((generation) => (
                              <div key={generation.id} className="border rounded p-3 bg-light-subtle">
                                <div className="d-flex flex-wrap gap-2 align-items-center small">
                                  <span className="badge text-bg-light">
                                    <strong>Promessa:</strong>{" "}
                                    {generation.fields?.primaryPromise || "—"}
                                  </span>
                                  <span className="badge text-bg-light">
                                    <strong>Dor:</strong> {generation.fields?.primaryPain || "—"}
                                  </span>
                                  <span className="badge text-bg-light">
                                    <strong>Mecanismo:</strong>{" "}
                                    {generation.fields?.mechanismSummary || "—"}
                                  </span>
                                  <span className="badge text-bg-light">
                                    <strong>Prova:</strong> {generation.fields?.proofUsed || "—"}
                                  </span>
                                  <span className="badge text-bg-light">
                                    <strong>CTA:</strong> {generation.fields?.cta || "—"}
                                  </span>
                                  <span className="badge text-bg-light">
                                    <strong>Funil:</strong>{" "}
                                    {generation.fields?.funnelStage || "—"}
                                  </span>
                                  <span className="badge text-bg-light">
                                    <strong>Tom:</strong> {generation.fields?.tone || "—"}
                                  </span>
                                </div>
                                <div className="d-flex flex-wrap gap-2 small text-muted mt-3">
                                  <span>
                                    <strong>Modelo:</strong>{" "}
                                    {generation.model?.trim() || "Não informado"}
                                  </span>
                                  <span>
                                    <strong>Data:</strong> {formatDateTime(generation.createdAt)}
                                  </span>
                                </div>
                              </div>
                            ))}
                          </div>
                        ) : (
                          <p className="text-muted mb-0 small">
                            Ainda não existem ângulos gerados para este
                            experimento.
                          </p>
                        )}
                      </>
                    ) : (
                      <>
                        <label className="form-label mb-1">
                          Prompt da geração <span className="text-danger">*</span>
                        </label>
                        <div className="alert alert-secondary mb-0" role="status">
                          <div className="fw-semibold">
                            Prompt interno aplicado automaticamente.
                          </div>
                          <div className="small mt-1">
                            Esta seção usa apenas instruções codificadas na
                            aplicação e o contexto da hipótese vinculada.
                          </div>
                          {SECTION_PROMPT_DEFAULTS[section.key] ? (
                            <details className="mt-2">
                              <summary className="small">
                                Ver referência do prompt interno desta seção
                              </summary>
                              <pre className="small mb-0 mt-2">
                                {SECTION_PROMPT_DEFAULTS[section.key]}
                              </pre>
                            </details>
                          ) : null}
                        </div>
                      </>
                    )}
                  </div>
                </div>

                <div className="d-flex justify-content-end mt-4">
                  <button
                    type="button"
                    className="btn btn-primary"
                    onClick={() => handleRequest(section.key)}
                    disabled={isRequestingBySection[section.key]}
                  >
                    {isRequestingBySection[section.key] ? (
                      <span className="d-inline-flex align-items-center gap-1">
                        <span
                          className="spinner-border spinner-border-sm"
                          role="status"
                          aria-hidden="true"
                        />
                        Enviando...
                      </span>
                    ) : (
                      "Solicitar geração por IA"
                    )}
                  </button>
                </div>
              </div>
            </section>
          </Tabs.Content>
        ))}
      </Tabs.Root>

      <small className="text-muted">
        Aba atual: <strong>{currentSection.label}</strong>
      </small>
    </div>
  );
}
