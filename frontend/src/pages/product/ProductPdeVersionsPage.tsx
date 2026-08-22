import { useEffect, useMemo, useState } from "react";
import { useQueries } from "@tanstack/react-query";
import axios from "axios";
import { Link, useParams } from "react-router-dom";
import {
  useProductPdeProductionSlots,
  usePublishProductPdeProductionSlot,
  useSaveProductPdeProductionSlot,
  useValidateProductPdeProductionSlot,
} from "../../api/product/usePdeProductionSlots";
import { useProduct } from "../../api/product/useProduct";
import type {
  PdeProductionSlotStatus,
  PostDeployMonitorResponse,
  PostDeployPdeExperienceVersion,
  PostDeployPdeProductionSlot,
} from "../../api/experiment/usePostDeployMonitor";
import PageTitle from "../../components/PageTitle";

const statusLabels: Record<PdeProductionSlotStatus, string> = {
  PLANNED: "Planejado",
  READY: "Pronto",
  ACTIVE: "Ativo",
  PAUSED: "Pausado",
  RETIRED: "Encerrado",
};

const layoutOptions = [
  {
    value: "assisted-service-v1",
    label: "Serviço assistido",
  },
  {
    value: "video-explicativo",
    label: "Vídeo explicativo",
  },
  {
    value: "video-motivacional",
    label: "Vídeo motivacional",
  },
  {
    value: "espelho-antes-de-sair",
    label: "Espelho antes de sair",
  },
  {
    value: "estrada-desejo",
    label: "Estrada do desejo",
  },
  {
    value: "diagnostico-classico",
    label: "Diagnóstico clássico",
  },
  {
    value: "layout-custom-v6",
    label: "Layout custom v6",
  },
];

type NewPdeSlotForm = {
  slotCode: string;
  domain: string;
  experienceVersion: string;
  layoutKey: string;
  sourceExperimentId: string;
  status: PdeProductionSlotStatus;
  notes: string;
};

/** Retorna um cadastro inicial coerente com o produto, sem pressupor que todo PDE seja MUSA. */
export function defaultPdeSlotForm(product?: {
  slug?: string;
  pdeExperienceJson?: string;
}): NewPdeSlotForm {
  if (!product?.slug || product.slug === "metodo-musa-7-dias") {
    return {
      slotCode: "v2",
      domain: "v2.clubemusa.com.br",
      experienceVersion: "musa-pde-entry-v5-estrada-desejo",
      layoutKey: "estrada-desejo",
      sourceExperimentId: "",
      status: "PLANNED",
      notes: "",
    };
  }
  let layoutKey = "assisted-service-v1";
  try {
    const contract = JSON.parse(product.pdeExperienceJson || "{}") as {
      layoutKey?: string;
    };
    if (contract.layoutKey?.trim()) layoutKey = contract.layoutKey.trim();
  } catch {
    // O backend continuará sendo a autoridade para validar o contrato completo.
  }
  return {
    slotCode: "v1",
    domain: `${product.slug}.digicomdigital.com.br`,
    experienceVersion: `${product.slug}-pde-v1`,
    layoutKey,
    sourceExperimentId: "",
    status: "PLANNED",
    notes: "",
  };
}

function hasExplicitTimeZone(value: string) {
  return /(?:z|[+-]\d{2}:?\d{2})$/i.test(value.trim());
}

function formatDate(value?: string | null) {
  if (!value) return "—";
  const date = new Date(
    hasExplicitTimeZone(value) ? value : `${value.replace(" ", "T")}-03:00`,
  );
  if (Number.isNaN(date.getTime())) return "—";
  return date.toLocaleString("pt-BR", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
    timeZone: "America/Sao_Paulo",
  });
}

function formatInteger(value?: number | null) {
  return (value ?? 0).toLocaleString("pt-BR");
}

function formatPercent(value: number) {
  return `${value.toLocaleString("pt-BR", {
    maximumFractionDigits: 1,
    minimumFractionDigits: 0,
  })}%`;
}

function formatDuration(milliseconds?: number | null) {
  const totalSeconds = Math.round((milliseconds ?? 0) / 1000);
  if (totalSeconds <= 0) return "0s";
  const minutes = Math.floor(totalSeconds / 60);
  const seconds = totalSeconds % 60;
  return minutes > 0 ? `${minutes}m ${seconds}s` : `${seconds}s`;
}

function dividePercent(value: number, total: number) {
  return total > 0 ? (value / total) * 100 : 0;
}

function clamp(value: number, minimum: number, maximum: number) {
  return Math.min(Math.max(value, minimum), maximum);
}

function findVersionMetrics(
  monitor: PostDeployMonitorResponse | undefined,
  slot: PostDeployPdeProductionSlot,
) {
  if (!monitor?.pde) return undefined;
  return monitor.pde.experienceVersions.find(
    (version) => version.experienceVersion === slot.experienceVersion,
  );
}

function versionHasCommercialValidation(
  slot: PostDeployPdeProductionSlot,
  monitor: PostDeployMonitorResponse | undefined,
) {
  if (slot.status === "ACTIVE") return true;
  if (!monitor) return false;
  return (
    (monitor.metaAds.impressions ?? 0) > 0 ||
    (monitor.metaAds.clicks ?? 0) > 0 ||
    monitor.pde.sessions > 0 ||
    monitor.pde.pdeEntries > 0
  );
}

function getMetricSessions(
  metrics: PostDeployPdeExperienceVersion | undefined,
  monitor: PostDeployMonitorResponse | undefined,
) {
  return metrics?.sessions ?? monitor?.pde.sessions ?? 0;
}

function getMetricEntries(
  metrics: PostDeployPdeExperienceVersion | undefined,
  monitor: PostDeployMonitorResponse | undefined,
) {
  return metrics?.pdeEntries ?? monitor?.pde.pdeEntries ?? 0;
}

function calculatePotentialScore(
  metrics: PostDeployPdeExperienceVersion | undefined,
  monitor: PostDeployMonitorResponse | undefined,
) {
  const sessions = getMetricSessions(metrics, monitor);
  const entries = getMetricEntries(metrics, monitor);
  const firstInteractionClicks =
    metrics?.firstInteractionClicks ??
    (monitor?.pde.presenceMapClicks ?? 0) +
      (monitor?.pde.diagnosticClicks ?? 0);
  const loginStarted = metrics?.loginStarted ?? monitor?.pde.loginStarted ?? 0;
  const paywallViewed =
    metrics?.paywallViewed ?? monitor?.pde.paywallViewed ?? 0;
  const checkoutIntent =
    metrics?.checkoutIntent ?? monitor?.pde.checkoutStarted ?? 0;
  const subscriptionApproved =
    metrics?.subscriptionApproved ?? monitor?.pde.subscriptionApproved ?? 0;
  const averageVisibleMs = monitor?.pde.averageVisibleMsPerSession ?? 0;

  const volumeScore = clamp(sessions / 50, 0, 1) * 15;
  const engagementScore = clamp(averageVisibleMs / 60_000, 0, 1) * 20;
  const interactionScore =
    clamp(dividePercent(firstInteractionClicks, entries) / 35, 0, 1) * 20;
  const loginScore =
    clamp(dividePercent(loginStarted, entries) / 15, 0, 1) * 15;
  const paywallScore =
    clamp(dividePercent(paywallViewed, entries) / 8, 0, 1) * 15;
  const checkoutScore =
    clamp(dividePercent(checkoutIntent, entries) / 3, 0, 1) * 10;
  const purchaseScore = subscriptionApproved > 0 ? 5 : 0;

  return Math.round(
    volumeScore +
      engagementScore +
      interactionScore +
      loginScore +
      paywallScore +
      checkoutScore +
      purchaseScore,
  );
}

function describePotential(score: number, sessions: number) {
  if (sessions === 0) return "Sem tráfego";
  if (score >= 70) return "Forte";
  if (score >= 45) return "Promissor";
  if (score >= 25) return "Fraco";
  return "Baixo sinal";
}

function isPausedSlot(slot: PostDeployPdeProductionSlot) {
  return slot.status === "PAUSED" || slot.status === "RETIRED";
}

function validationBadgeClass(status?: string | null) {
  if (status === "OK") return "text-bg-success";
  if (status === "FAILED") return "text-bg-danger";
  return "text-bg-secondary";
}

function validationLabel(status?: string | null) {
  if (status === "OK") return "Entrega OK";
  if (status === "FAILED") return "Falhou";
  return "Não testada";
}

export default function ProductPdeVersionsPage() {
  const { productId } = useParams();
  const productQuery = useProduct(productId);
  const slotsQuery = useProductPdeProductionSlots(productId);
  const saveSlot = useSaveProductPdeProductionSlot(productId);
  const publishSlot = usePublishProductPdeProductionSlot(productId);
  const validateSlot = useValidateProductPdeProductionSlot(productId);
  const product = productQuery.data;
  const slots = slotsQuery.data ?? [];
  const sourceExperimentIds = Array.from(
    new Set(slots.map((slot) => slot.sourceExperimentId).filter(Boolean)),
  ) as number[];
  const monitorQueries = useQueries({
    queries: sourceExperimentIds.map((experimentId) => ({
      queryKey: [
        "experiment",
        String(experimentId),
        "post-deploy-monitor",
        product?.slug,
      ],
      enabled: Boolean(product?.slug),
      refetchInterval: 60_000,
      queryFn: async () => {
        const { data } = await axios.get<PostDeployMonitorResponse>(
          `/api/experiments/${experimentId}/post-deploy-monitor`,
          { params: { productSlug: product?.slug } },
        );
        return data;
      },
    })),
  });
  const monitorsByExperimentId = new Map<number, PostDeployMonitorResponse>();
  sourceExperimentIds.forEach((experimentId, index) => {
    const monitor = monitorQueries[index]?.data;
    if (monitor) {
      monitorsByExperimentId.set(experimentId, monitor);
    }
  });
  const sortedSlots = [...slots].sort((current, next) => {
    const currentMonitor = current.sourceExperimentId
      ? monitorsByExperimentId.get(current.sourceExperimentId)
      : undefined;
    const nextMonitor = next.sourceExperimentId
      ? monitorsByExperimentId.get(next.sourceExperimentId)
      : undefined;
    const currentValidating = versionHasCommercialValidation(
      current,
      currentMonitor,
    );
    const nextValidating = versionHasCommercialValidation(next, nextMonitor);
    if (currentValidating !== nextValidating) {
      return currentValidating ? -1 : 1;
    }
    return current.slotCode.localeCompare(next.slotCode, "pt-BR", {
      numeric: true,
    });
  });
  const activeSlots = sortedSlots.filter((slot) => !isPausedSlot(slot));
  const pausedSlots = sortedSlots.filter(isPausedSlot);
  const [form, setForm] = useState<NewPdeSlotForm>(() => defaultPdeSlotForm());
  const [defaultProductSlug, setDefaultProductSlug] = useState("");
  const [selectedEditorSlotCode, setSelectedEditorSlotCode] = useState("");
  const selectedEditorSlot = useMemo(
    () => slots.find((slot) => slot.slotCode === selectedEditorSlotCode),
    [selectedEditorSlotCode, slots],
  );
  const [contractDraft, setContractDraft] = useState("");
  const [publishedBy, setPublishedBy] = useState("Marketing Hub");

  useEffect(() => {
    if (!selectedEditorSlotCode && slots.length > 0) {
      setSelectedEditorSlotCode(slots[0].slotCode);
    }
  }, [selectedEditorSlotCode, slots]);

  useEffect(() => {
    if (
      !product?.slug ||
      slots.length > 0 ||
      defaultProductSlug === product.slug
    )
      return;
    setForm(defaultPdeSlotForm(product));
    setDefaultProductSlug(product.slug);
  }, [defaultProductSlug, product, slots.length]);

  useEffect(() => {
    if (!selectedEditorSlot) return;
    setContractDraft(
      selectedEditorSlot.draftExperienceJson ||
        selectedEditorSlot.publishedExperienceJson ||
        product?.pdeExperienceJson ||
        "",
    );
  }, [product?.pdeExperienceJson, selectedEditorSlot]);

  const saveEditorDraft = () => {
    if (!selectedEditorSlot) return;
    saveSlot.mutate({
      productSlug: product?.slug || "",
      slotCode: selectedEditorSlot.slotCode,
      domain: selectedEditorSlot.domain,
      publicUrl: selectedEditorSlot.publicUrl,
      backendUrl: selectedEditorSlot.backendUrl || undefined,
      experienceVersion: selectedEditorSlot.experienceVersion,
      layoutKey: selectedEditorSlot.layoutKey,
      targetEnvironment: selectedEditorSlot.targetEnvironment,
      sourceExperimentId: selectedEditorSlot.sourceExperimentId || undefined,
      status: selectedEditorSlot.status,
      notes: selectedEditorSlot.notes || undefined,
      draftExperienceJson: contractDraft,
    });
  };

  const publishEditorDraft = () => {
    if (!selectedEditorSlot) return;
    publishSlot.mutate({
      slotCode: selectedEditorSlot.slotCode,
      experienceJson: contractDraft,
      publishedBy,
    });
  };

  if (productQuery.isLoading || slotsQuery.isLoading) {
    return <p className="text-muted">Carregando versões PDE...</p>;
  }

  if (!product) {
    return <div className="alert alert-danger">Produto não encontrado.</div>;
  }

  return (
    <div>
      <div className="d-flex flex-wrap align-items-start justify-content-between gap-3 mb-4">
        <div>
          <PageTitle>Versões PDE do produto</PageTitle>
          <p className="text-muted mb-0">
            {product.name || product.slug} · fonte de verdade para URLs e
            versões produtivas que os experimentos podem medir.
          </p>
        </div>
        <div className="d-flex flex-wrap gap-2">
          <Link
            className="btn btn-outline-primary"
            to={`/products/${productId}/pde-videos`}
          >
            Vídeos PDE
          </Link>
          <Link className="btn btn-outline-secondary" to="/products">
            Voltar para produtos
          </Link>
        </div>
      </div>

      <div className="card mb-3">
        <div className="card-body">
          <h2 className="h6 mb-3">Cadastrar versão produtiva</h2>
          <form
            className="row g-2 align-items-end"
            onSubmit={(event) => {
              event.preventDefault();
              saveSlot.mutate({
                productSlug: product.slug || "",
                slotCode: form.slotCode,
                domain: form.domain,
                experienceVersion: form.experienceVersion,
                layoutKey: form.layoutKey,
                sourceExperimentId: form.sourceExperimentId
                  ? Number(form.sourceExperimentId)
                  : undefined,
                status: form.status,
                notes: form.notes,
              });
            }}
          >
            <div className="col-12 col-md-2">
              <label
                className="form-label small fw-semibold"
                htmlFor="pde-slot-code"
              >
                Slot *
              </label>
              <input
                id="pde-slot-code"
                className="form-control form-control-sm"
                value={form.slotCode}
                onChange={(event) =>
                  setForm((current) => ({
                    ...current,
                    slotCode: event.target.value,
                  }))
                }
                required
              />
            </div>
            <div className="col-12 col-md-3">
              <label
                className="form-label small fw-semibold"
                htmlFor="pde-slot-domain"
              >
                Domínio *
              </label>
              <input
                id="pde-slot-domain"
                className="form-control form-control-sm"
                value={form.domain}
                onChange={(event) =>
                  setForm((current) => ({
                    ...current,
                    domain: event.target.value,
                  }))
                }
                required
              />
            </div>
            <div className="col-12 col-md-3">
              <label
                className="form-label small fw-semibold"
                htmlFor="pde-slot-version"
              >
                Versão PDE *
              </label>
              <input
                id="pde-slot-version"
                className="form-control form-control-sm"
                value={form.experienceVersion}
                onChange={(event) =>
                  setForm((current) => ({
                    ...current,
                    experienceVersion: event.target.value,
                  }))
                }
                required
              />
            </div>
            <div className="col-12 col-md-2">
              <label
                className="form-label small fw-semibold"
                htmlFor="pde-slot-layout"
              >
                Layout
              </label>
              <input
                id="pde-slot-layout"
                className="form-control form-control-sm"
                list="pde-layout-options"
                value={form.layoutKey}
                onChange={(event) =>
                  setForm((current) => ({
                    ...current,
                    layoutKey: event.target.value,
                  }))
                }
              />
              <datalist id="pde-layout-options">
                {layoutOptions.map((option) => (
                  <option key={option.value} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </datalist>
            </div>
            <div className="col-12 col-md-2">
              <label
                className="form-label small fw-semibold"
                htmlFor="pde-slot-status"
              >
                Status
              </label>
              <select
                id="pde-slot-status"
                className="form-select form-select-sm"
                value={form.status}
                onChange={(event) =>
                  setForm((current) => ({
                    ...current,
                    status: event.target.value as PdeProductionSlotStatus,
                  }))
                }
              >
                {Object.entries(statusLabels).map(([value, label]) => (
                  <option key={value} value={value}>
                    {label}
                  </option>
                ))}
              </select>
            </div>
            <div className="col-12 col-md-2">
              <label
                className="form-label small fw-semibold"
                htmlFor="pde-slot-experiment"
              >
                Experimento origem
              </label>
              <input
                id="pde-slot-experiment"
                className="form-control form-control-sm"
                type="number"
                min="1"
                value={form.sourceExperimentId}
                onChange={(event) =>
                  setForm((current) => ({
                    ...current,
                    sourceExperimentId: event.target.value,
                  }))
                }
              />
            </div>
            <div className="col-12 col-md-2">
              <button
                type="submit"
                className="btn btn-primary btn-sm w-100"
                disabled={saveSlot.isPending}
              >
                {saveSlot.isPending ? "Salvando..." : "Salvar versão"}
              </button>
            </div>
            <div className="col-12">
              <label
                className="form-label small fw-semibold"
                htmlFor="pde-slot-notes"
              >
                Observação
              </label>
              <input
                id="pde-slot-notes"
                className="form-control form-control-sm"
                value={form.notes}
                onChange={(event) =>
                  setForm((current) => ({
                    ...current,
                    notes: event.target.value,
                  }))
                }
              />
            </div>
          </form>
        </div>
      </div>

      <div className="card">
        <div className="card-body">
          <h2 className="h6 mb-3">Versões cadastradas</h2>
          <p className="text-muted small mb-3">
            Métricas priorizam sinais de potencial comercial: acesso e
            permanência indicam atenção, mas interação, login, paywall, checkout
            e venda mostram avanço real do desconhecimento para desejo de
            compra.
          </p>
          {slots.length === 0 ? (
            <div className="text-muted small">
              Nenhuma versão PDE cadastrada para este produto.
            </div>
          ) : (
            <>
              <SlotTable
                title="Versões ativas, prontas ou planejadas"
                emptyMessage="Nenhuma versão ativa, pronta ou planejada."
                slots={activeSlots}
                monitorsByExperimentId={monitorsByExperimentId}
                onValidate={(slot) => validateSlot.mutate(slot.slotCode)}
                validatingSlotCode={validateSlot.variables}
                isValidating={validateSlot.isPending}
              />
              <SlotTable
                title="Versões pausadas ou encerradas"
                emptyMessage="Nenhuma versão pausada ou encerrada."
                slots={pausedSlots}
                monitorsByExperimentId={monitorsByExperimentId}
                onValidate={(slot) => validateSlot.mutate(slot.slotCode)}
                validatingSlotCode={validateSlot.variables}
                isValidating={validateSlot.isPending}
              />
            </>
          )}
        </div>
      </div>

      <div className="card mt-3">
        <div className="card-body">
          <div className="d-flex flex-wrap align-items-start justify-content-between gap-3 mb-3">
            <div>
              <h2 className="h6 mb-1">Editor/publicador de contrato PDE</h2>
              <p className="text-muted small mb-0">
                Cada slot pode ter um contrato próprio publicado. Use isso para
                testar promessa, perguntas, vídeos, CTA e oferta sem afetar as
                outras URLs.
              </p>
            </div>
            {selectedEditorSlot?.publishedAt && (
              <span className="badge text-bg-success">
                Publicado {formatDate(selectedEditorSlot.publishedAt)}
              </span>
            )}
          </div>
          {slots.length === 0 ? (
            <div className="text-muted small">
              Cadastre uma versão PDE antes de editar o contrato do slot.
            </div>
          ) : (
            <div className="row g-3">
              <div className="col-12 col-md-3">
                <label
                  className="form-label small fw-semibold"
                  htmlFor="pde-editor-slot"
                >
                  Slot *
                </label>
                <select
                  id="pde-editor-slot"
                  className="form-select form-select-sm"
                  value={selectedEditorSlotCode}
                  onChange={(event) =>
                    setSelectedEditorSlotCode(event.target.value)
                  }
                >
                  {slots.map((slot) => (
                    <option key={slot.id} value={slot.slotCode}>
                      {slot.slotCode} · {slot.domain}
                    </option>
                  ))}
                </select>
              </div>
              <div className="col-12 col-md-3">
                <label
                  className="form-label small fw-semibold"
                  htmlFor="pde-editor-published-by"
                >
                  Publicado por
                </label>
                <input
                  id="pde-editor-published-by"
                  className="form-control form-control-sm"
                  value={publishedBy}
                  onChange={(event) => setPublishedBy(event.target.value)}
                />
              </div>
              <div className="col-12 col-md-6">
                <div className="small text-muted">
                  Contrato público:{" "}
                  <a
                    href={`/api/products/public/${product.slug}/pde-experience?slotCode=${selectedEditorSlotCode}`}
                    target="_blank"
                    rel="noreferrer"
                  >
                    abrir JSON publicado do slot
                  </a>
                </div>
                <div className="small text-muted">
                  Versão: {selectedEditorSlot?.experienceVersion || "—"}
                </div>
                <div className="small text-muted">
                  Layout: {selectedEditorSlot?.layoutKey || "—"}
                </div>
              </div>
              <div className="col-12">
                <label
                  className="form-label small fw-semibold"
                  htmlFor="pde-editor-json"
                >
                  Contrato JSON *
                </label>
                <textarea
                  id="pde-editor-json"
                  className="form-control font-monospace"
                  rows={18}
                  value={contractDraft}
                  onChange={(event) => setContractDraft(event.target.value)}
                  required
                />
              </div>
              <div className="col-12 d-flex flex-wrap gap-2">
                <button
                  type="button"
                  className="btn btn-outline-primary btn-sm"
                  onClick={saveEditorDraft}
                  disabled={saveSlot.isPending || !selectedEditorSlot}
                >
                  {saveSlot.isPending ? "Salvando..." : "Salvar rascunho"}
                </button>
                <button
                  type="button"
                  className="btn btn-primary btn-sm"
                  onClick={publishEditorDraft}
                  disabled={publishSlot.isPending || !selectedEditorSlot}
                >
                  {publishSlot.isPending ? "Publicando..." : "Publicar no slot"}
                </button>
                <a
                  className="btn btn-outline-secondary btn-sm"
                  href={selectedEditorSlot?.publicUrl || "#"}
                  target="_blank"
                  rel="noreferrer"
                >
                  Abrir URL pública
                </a>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

function SlotTable({
  title,
  emptyMessage,
  slots,
  monitorsByExperimentId,
  onValidate,
  validatingSlotCode,
  isValidating,
}: {
  title: string;
  emptyMessage: string;
  slots: PostDeployPdeProductionSlot[];
  monitorsByExperimentId: Map<number, PostDeployMonitorResponse>;
  onValidate: (slot: PostDeployPdeProductionSlot) => void;
  validatingSlotCode?: string;
  isValidating: boolean;
}) {
  return (
    <section className="mb-4">
      <h3 className="h6 mb-2">{title}</h3>
      <div className="table-responsive">
        <table className="table table-sm align-middle mb-0">
          <thead>
            <tr>
              <th>Slot</th>
              <th>Status</th>
              <th>Versão PDE</th>
              <th>Layout</th>
              <th>URL pública</th>
              <th>Validação</th>
              <th>Entrega URL</th>
              <th>Acesso</th>
              <th>Permanência</th>
              <th>Avanço no funil</th>
              <th>Score</th>
              <th>Ambiente alvo</th>
              <th>Experimento origem</th>
              <th className="text-end">Atualizado</th>
            </tr>
          </thead>
          <tbody>
            {slots.length === 0 ? (
              <tr>
                <td colSpan={14} className="text-muted">
                  {emptyMessage}
                </td>
              </tr>
            ) : (
              slots.map((slot) => {
                const monitor = slot.sourceExperimentId
                  ? monitorsByExperimentId.get(slot.sourceExperimentId)
                  : undefined;
                const metrics = findVersionMetrics(monitor, slot);
                const sessions = getMetricSessions(metrics, monitor);
                const entries = getMetricEntries(metrics, monitor);
                const firstInteractionClicks =
                  metrics?.firstInteractionClicks ??
                  (monitor?.pde.presenceMapClicks ?? 0) +
                    (monitor?.pde.diagnosticClicks ?? 0);
                const loginStarted =
                  metrics?.loginStarted ?? monitor?.pde.loginStarted ?? 0;
                const paywallViewed =
                  metrics?.paywallViewed ?? monitor?.pde.paywallViewed ?? 0;
                const checkoutIntent =
                  metrics?.checkoutIntent ?? monitor?.pde.checkoutStarted ?? 0;
                const subscriptionApproved =
                  metrics?.subscriptionApproved ??
                  monitor?.pde.subscriptionApproved ??
                  0;
                const score = calculatePotentialScore(metrics, monitor);
                const validating = versionHasCommercialValidation(
                  slot,
                  monitor,
                );

                return (
                  <tr
                    key={slot.id}
                    className={validating ? "table-primary" : undefined}
                  >
                    <td className="fw-semibold">{slot.slotCode}</td>
                    <td>{statusLabels[slot.status] ?? slot.status}</td>
                    <td>
                      <div className="font-monospace small">
                        {slot.experienceVersion}
                      </div>
                      {slot.notes && (
                        <div className="small text-muted">{slot.notes}</div>
                      )}
                    </td>
                    <td>
                      <span className="badge text-bg-light">
                        {slot.layoutKey}
                      </span>
                    </td>
                    <td>
                      <a href={slot.publicUrl} target="_blank" rel="noreferrer">
                        {slot.publicUrl}
                      </a>
                      <div className="small text-muted">{slot.domain}</div>
                    </td>
                    <td>
                      {validating ? (
                        <span className="badge text-bg-primary">
                          Em validação
                        </span>
                      ) : slot.sourceExperimentId ? (
                        <span className="badge text-bg-light">Sem tráfego</span>
                      ) : (
                        <span className="badge text-bg-secondary">
                          Sem experimento
                        </span>
                      )}
                    </td>
                    <td>
                      <div className="d-flex flex-column gap-1">
                        <span
                          className={`badge ${validationBadgeClass(
                            slot.validationStatus,
                          )}`}
                        >
                          {validationLabel(slot.validationStatus)}
                        </span>
                        {slot.validationSummary && (
                          <span className="small text-muted">
                            {slot.validationSummary}
                          </span>
                        )}
                        {slot.validationCheckedAt && (
                          <span className="small text-muted">
                            {formatDate(slot.validationCheckedAt)}
                          </span>
                        )}
                        <button
                          type="button"
                          className="btn btn-outline-primary btn-sm"
                          onClick={() => onValidate(slot)}
                          disabled={
                            isValidating && validatingSlotCode === slot.slotCode
                          }
                        >
                          {isValidating && validatingSlotCode === slot.slotCode
                            ? "Testando..."
                            : "Testar URL"}
                        </button>
                      </div>
                    </td>
                    <td>
                      <div>{formatInteger(entries)} acessos</div>
                      <div className="small text-muted">
                        {formatInteger(sessions)} sessões
                        {monitor?.pde.uniqueVisitors != null
                          ? ` · ${formatInteger(monitor.pde.uniqueVisitors)} visitantes`
                          : ""}
                      </div>
                    </td>
                    <td>
                      <div>
                        {formatDuration(
                          monitor?.pde.averageVisibleMsPerSession,
                        )}
                      </div>
                      <div className="small text-muted">
                        Último evento {formatDate(monitor?.pde.lastEventAt)}
                      </div>
                    </td>
                    <td>
                      <div className="small">
                        Interação: {formatInteger(firstInteractionClicks)} (
                        {formatPercent(
                          dividePercent(firstInteractionClicks, entries),
                        )}
                        )
                      </div>
                      <div className="small">
                        Login: {formatInteger(loginStarted)} · Paywall:{" "}
                        {formatInteger(paywallViewed)}
                      </div>
                      <div className="small">
                        Checkout: {formatInteger(checkoutIntent)} · Vendas:{" "}
                        {formatInteger(subscriptionApproved)}
                      </div>
                    </td>
                    <td>
                      <div className="fw-semibold">{score}/100</div>
                      <div className="small text-muted">
                        {describePotential(score, sessions)}
                      </div>
                    </td>
                    <td>{slot.targetEnvironment}</td>
                    <td>{slot.sourceExperimentId ?? "—"}</td>
                    <td className="text-end">{formatDate(slot.updatedAt)}</td>
                  </tr>
                );
              })
            )}
          </tbody>
        </table>
      </div>
    </section>
  );
}
