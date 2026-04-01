import { useEffect, useMemo, useState } from "react";
import { toast } from "react-toastify";
import * as Tabs from "@radix-ui/react-tabs";
import axios from "axios";
import type { Hypothesis } from "../../api/hypothesis/useHypothesisBoard";
import { CampaignAngleSummary, hasCampaignAngleContent, parseCampaignAnglePayload } from "./campaignAngleParser";
import { AdCopyContent, hasAdCopyContent, parseAdCopyPayload } from "./adCopyParser";

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

  const persistedCampaignAngle = useMemo(
    () => parseCampaignAnglePayload(campaignAngle),
    [campaignAngle],
  );

  const persistedAdCopy = useMemo(() => parseAdCopyPayload(adCopy), [adCopy]);

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
    };

    void loadCampaignAngles();
  }, [experimentId]);

  useEffect(() => {
    const loadAdCopy = async () => {
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
    };

    void loadAdCopy();
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
                        <CampaignAngleSummaryPanel
                          isLoading={isLoadingCampaignAngles}
                          savedAngle={persistedCampaignAngle}
                          fallbackAngle={campaignAngleGenerations[0]?.fields}
                          fallbackTimestamp={campaignAngleGenerations[0]?.createdAt}
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
            <span className={`badge text-bg-${badgeVariant}`}>{badgeLabel}</span>
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
              <div className="col-12 col-xl-4" key={`${variant.label}-${index}`}>
                <div className="border rounded p-3 h-100 bg-light-subtle">
                  <p className="text-uppercase small fw-semibold text-muted mb-2">
                    {variant.label || `V${index + 1}`}
                  </p>
                  <p className="small mb-2"><strong>Texto:</strong> {variant.primaryText || "—"}</p>
                  <p className="small mb-2"><strong>Headline:</strong> {variant.headline || "—"}</p>
                  <p className="small mb-2"><strong>Descrição:</strong> {variant.description || "—"}</p>
                  <p className="small mb-0"><strong>CTA:</strong> {variant.ctaText || "—"}</p>
                </div>
              </div>
            ))}
          </div>
        ) : (
          <div className="alert alert-info mt-3" role="status">
            Nenhum texto do anúncio disponível ainda. Solicite o Worker IA para preencher os blocos.
          </div>
        )}

        {rawText && !hasData ? (
          <details className="mt-3">
            <summary className="small text-muted">Ver conteúdo bruto salvo</summary>
            <pre className="bg-body-secondary rounded p-3 small mb-0">{rawText}</pre>
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
              {generations.length === 1 ? "1 geração" : `${generations.length} gerações`}
            </span>
          ) : null}
        </div>

        {isLoading && generations.length === 0 ? (
          <div className="d-flex align-items-center gap-2 text-muted mt-3">
            <span className="spinner-border spinner-border-sm" role="status" aria-hidden="true" />
            Carregando histórico...
          </div>
        ) : generations.length === 0 ? (
          <p className="text-muted small mb-0 mt-3">
            Assim que você solicitar o Worker IA o histórico aparecerá aqui.
          </p>
        ) : (
          <div className="mt-3 d-flex flex-column gap-2">
            {generations.map((generation) => (
              <div key={generation.id} className="border rounded p-3 bg-body-tertiary">
                <div className="d-flex justify-content-between align-items-start flex-wrap gap-2">
                  <p className="fw-semibold mb-1">{formatDateTime(generation.createdAt)}</p>
                  {generation.model ? (
                    <span className="badge text-bg-light">{generation.model}</span>
                  ) : null}
                </div>
                <div className="mt-2 d-flex flex-column gap-2">
                  {generation.fields?.primaryTextVariants?.map((variant, index) => (
                    <div className="border rounded p-2 bg-white" key={`${generation.id}-${variant.label}-${index}`}>
                      <p className="small fw-semibold mb-1">{variant.label || `V${index + 1}`}</p>
                      <p className="small mb-1"><strong>Texto:</strong> {variant.primaryText || "—"}</p>
                      <p className="small mb-1"><strong>Headline:</strong> {variant.headline || "—"}</p>
                      <p className="small mb-1"><strong>Descrição:</strong> {variant.description || "—"}</p>
                      <p className="small mb-0"><strong>CTA:</strong> {variant.ctaText || "—"}</p>
                    </div>
                  ))}
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
              Estrutura a promessa, dor, mecanismo e prova enviados pelo Worker IA.
            </p>
          </div>
          <div className="text-end">
            <span className={`badge text-bg-${badgeVariant}`}>{badgeLabel}</span>
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
                { label: "Resultado / Promessa", value: resolvedAngle?.primaryPromise },
                { label: "Mecanismo", value: resolvedAngle?.mechanismSummary },
                { label: "Prova", value: resolvedAngle?.proofUsed },
              ].map((block) => (
                <div className="col-12 col-md-6 col-xl-3" key={block.label}>
                  <div className="border rounded p-3 h-100 bg-light-subtle">
                    <p className="text-uppercase small fw-semibold text-muted mb-1">
                      {block.label}
                    </p>
                    <p className="mb-0">
                      {block.value ?? (
                        <span className="text-muted">Ainda não preenchido.</span>
                      )}
                    </p>
                  </div>
                </div>
              ))}
            </div>

            <div className="row g-3 mt-1">
              {[
                { label: "CTA recomendado", value: resolvedAngle?.cta },
                { label: "Estágio de funil", value: resolvedAngle?.funnelStage },
                { label: "Tom sugerido", value: resolvedAngle?.tone },
              ].map((block) => (
                <div className="col-12 col-md-4" key={block.label}>
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
            Nenhum ângulo disponível ainda. Solicite o Worker IA para preencher os blocos.
          </div>
        )}

        {rawText && !hasData ? (
          <details className="mt-3">
            <summary className="small text-muted">Ver conteúdo bruto salvo</summary>
            <pre className="bg-body-secondary rounded p-3 small mb-0">{rawText}</pre>
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

function CampaignAngleHistoryList({ generations, isLoading }: CampaignAngleHistoryListProps) {
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
              <div key={generation.id} className="border rounded p-3 bg-body-tertiary">
                <div className="d-flex justify-content-between align-items-start flex-wrap gap-2">
                  <div>
                    <p className="fw-semibold mb-1">
                      {generation.fields?.primaryPromise ?? "Promessa não estruturada"}
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
                      <p className="text-muted small mb-0 mt-1">{generation.model}</p>
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
                    <strong>Mecanismo:</strong> {generation.fields.mechanismSummary}
                  </p>
                ) : null}
                {generation.fields?.proofUsed ? (
                  <p className="small mb-0 text-muted">
                    <strong>Prova:</strong> {generation.fields.proofUsed}
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
