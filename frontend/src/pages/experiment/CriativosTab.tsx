import { useEffect, useState } from "react";
import { useForm } from "react-hook-form";
import { Creative, useCreatives } from "../../api/creative/useCreatives";
import { useCreateCreative } from "../../api/creative/useCreateCreative";
import { useUpdateCreative } from "../../api/creative/useUpdateCreative";
import { useDeleteCreative } from "../../api/creative/useDeleteCreative";
import { useAngles } from "../../api/angle/useAngles";
import { useVisualProofs } from "../../api/visualProof/useVisualProofs";
import { useEmotionalTriggers } from "../../api/emotionalTrigger/useEmotionalTriggers";
import { useUpdateCreativeLabels } from "../../api/creative/useUpdateCreativeLabels";
import { useRequestCreatives } from "../../api/experiment/useRequestCreatives";
import { useExperiment } from "../../api/experiment/useExperiment";
import { useUpdateExperiment } from "../../api/experiment/useUpdateExperiment";
import { useAllFacebookPages } from "../../api/useAllFacebookPages";
import InstagramAdPreview from "../../components/InstagramAdPreview";
import { resolveAssetUrl } from "../../utils/resolveAssetUrl";
import { FACEBOOK_CALL_TO_ACTIONS } from "../../constants/facebookCallToActions";
import {
  AlertTriangle,
  CheckCircle2,
  Edit3,
  Eye,
  Sparkles,
  Trash2,
  X,
  XCircle,
} from "lucide-react";
import "./CriativosTab.css";

interface Props {
  experimentId: string;
}

interface CreativeForm {
  format: string;
  primaryText: string;
  headline: string;
  description: string;
  cta: string;
  destinationUrl: string;
  leadGenFormId: string;
  imageUrl: string;
  instagramUserId: string;
  status: string;
}

const ICON_SIZE = 16;

type FeedbackVariant = "success" | "warning" | "error";

interface FeedbackState {
  variant: FeedbackVariant;
  title: string;
  description?: string;
}

const statusVariant = (status: string) => {
  switch (status) {
    case "READY":
      return "text-bg-success";
    case "DRAFT":
      return "text-bg-secondary";
    default:
      return "text-bg-warning";
  }
};

const statusLabel = (status: string) => {
  switch (status) {
    case "READY":
      return "Aprovado";
    case "DRAFT":
      return "Rascunho";
    default:
      return status;
  }
};

export default function CriativosTab({ experimentId }: Props) {
  const { data, isLoading } = useCreatives(experimentId);
  const creatives = Array.isArray(data) ? data : [];
  const { data: experiment } = useExperiment(experimentId);
  const updateExperimentMutation = useUpdateExperiment(experimentId);
  const [showForm, setShowForm] = useState(false);
  const [editing, setEditing] = useState<Creative | null>(null);
  const [feedback, setFeedback] = useState<FeedbackState | null>(null);
  const [form, setForm] = useState<CreativeForm>({
    format: "LINK",
    headline: "",
    primaryText: "",
    description: "",
    cta: "LEARN_MORE",
    destinationUrl: "",
    leadGenFormId: "",
    imageUrl: "",
    instagramUserId: "",
    status: "DRAFT",
  });
  const [experimentPageId, setExperimentPageId] = useState("");
  const { handleSubmit: handleFormSubmit } = useForm<CreativeForm>();
  const { data: angles } = useAngles();
  const { data: proofs } = useVisualProofs();
  const { data: triggers } = useEmotionalTriggers();
  const [selectedAngle, setSelectedAngle] = useState<string>("");
  const [selectedProof, setSelectedProof] = useState<string>("");
  const [selectedTrigger, setSelectedTrigger] = useState<string>("");
  const [isRequestDialogOpen, setRequestDialogOpen] = useState(false);
  const [requestQuantity, setRequestQuantity] = useState("1");
  const [requestError, setRequestError] = useState<string | null>(null);
  const patchLabels = useUpdateCreativeLabels(experimentId);
  const create = useCreateCreative(experimentId);
  const update = useUpdateCreative(experimentId);
  const del = useDeleteCreative(experimentId);
  const [showPreview, setShowPreview] = useState(false);
  const [processingCreativeId, setProcessingCreativeId] = useState<number | null>(
    null,
  );
  const requestCreatives = useRequestCreatives(experimentId);
  const { data: facebookPages, isLoading: isLoadingFacebookPages } =
    useAllFacebookPages();

  useEffect(() => {
    if (!feedback) return;
    const timeout = window.setTimeout(() => {
      setFeedback(null);
    }, 8000);
    return () => {
      window.clearTimeout(timeout);
    };
  }, [feedback]);

  const dismissFeedback = () => setFeedback(null);

  useEffect(() => {
    setExperimentPageId(
      experiment?.facebookPage?.id ? String(experiment.facebookPage.id) : "",
    );
  }, [experiment?.facebookPage?.id]);

  const handleSavePageId = async () => {
    if (!experiment) return;
    const kpiTargetValue = experiment.kpiTarget ?? experiment.kpiTargetCpl;
    if (kpiTargetValue == null || !experiment.metricPresetId) {
      setFeedback({
        variant: "error",
        title: "Não foi possível salvar a página",
        description:
          "Defina a meta de KPI e o preset de métricas antes de configurar a página do experimento.",
      });
      return;
    }
    const trimmedPageId = experimentPageId.trim();
    const parsedPageId = trimmedPageId === "" ? null : Number(trimmedPageId);
    if (parsedPageId !== null && Number.isNaN(parsedPageId)) {
      setFeedback({
        variant: "error",
        title: "ID da página inválido",
        description: "Selecione uma página válida da lista.",
      });
      return;
    }
    try {
      await updateExperimentMutation.mutateAsync({
        name: experiment.name,
        hypothesis: experiment.hypothesis,
        kpiTarget: Number(kpiTargetValue),
        metricPresetId: experiment.metricPresetId,
        sampleSize: experiment.sampleSize ?? undefined,
        mde: experiment.mdePercent ?? undefined,
        startDate: experiment.startDate ?? undefined,
        endDate: experiment.endDate ?? undefined,
        creativesToGenerate: experiment.creativesToGenerate ?? undefined,
        salesFunnelName: experiment.salesFunnelName ?? null,
        facebookPageId: parsedPageId,
      });
      const selectedPage =
        parsedPageId === null
          ? null
          : facebookPages?.find((page) => page.id === parsedPageId) ?? null;
      setFeedback({
        variant: "success",
        title: "Página atualizada",
        description: selectedPage
          ? `Todos os criativos deste experimento usarão a página ${selectedPage.name}.`
          : "Sem página definida o worker utilizará a página padrão configurada no worker.",
      });
    } catch {
      setFeedback({
        variant: "error",
        title: "Não foi possível salvar a página",
        description: "Tente novamente em instantes.",
      });
    }
  };

  const isSavingPageId = updateExperimentMutation.isPending;

  const fillFormFromCreative = (c: Creative) => {
    setForm({
      format: c.format || "LINK",
      headline: c.headline,
      primaryText: c.primaryText,
      description: c.description || "",
      cta: c.cta || "LEARN_MORE",
      destinationUrl: c.destinationUrl || "",
      leadGenFormId: c.leadGenFormId || "",
      imageUrl: c.imageUrl,
      instagramUserId: c.instagramUserId || "",
      status: c.status,
    });
  };

  const openEdit = (c: Creative) => {
    setEditing(c);
    fillFormFromCreative(c);
    setShowForm(true);
  };

  const openRequestDialog = () => {
    const defaultQty = Math.max(1, experiment?.creativesToGenerate ?? 1);
    setRequestQuantity(String(defaultQty));
    setRequestError(null);
    setRequestDialogOpen(true);
  };

  const closeRequestDialog = () => {
    setRequestDialogOpen(false);
    setRequestError(null);
  };

  const submit = async () => {
    const trimmedDestinationUrl = form.destinationUrl.trim();
    const trimmedLeadGenFormId = form.leadGenFormId.trim();
    const payload = {
      format: form.format,
      headline: form.headline,
      primaryText: form.primaryText,
      imageUrl: form.imageUrl,
      description: form.description,
      cta: form.cta,
      destinationUrl: trimmedDestinationUrl,
      leadGenFormId: trimmedLeadGenFormId,
      instagramUserId: form.instagramUserId,
      status: form.status,
    };
    if (editing) {
      await update.mutateAsync({ id: editing.id, ...payload });
    } else {
      const created = await create.mutateAsync(payload);
      await patchLabels.mutateAsync({
        id: created.id,
        labels: {
          angleId: selectedAngle ? Number(selectedAngle) : undefined,
          visualProofId: selectedProof ? Number(selectedProof) : undefined,
          emotionalTriggerId: selectedTrigger
            ? Number(selectedTrigger)
            : undefined,
        },
      });
    }
    setShowForm(false);
  };

  const startPreview = (c: Creative) => {
    setEditing(c);
    setShowPreview(true);
  };

  const remove = async (c: Creative) => {
    if (!confirm("Excluir criativo?")) return;
    setProcessingCreativeId(c.id);
    try {
      await del.mutateAsync(c.id);
    } catch {
      setFeedback({
        variant: "error",
        title: "Não foi possível excluir o criativo",
        description: "Tente novamente em instantes.",
      });
    } finally {
      setProcessingCreativeId(null);
    }
  };

  const approve = async (c: Creative) => {
    setProcessingCreativeId(c.id);
    try {
      await update.mutateAsync({
        id: c.id,
        format: c.format || "LINK",
        headline: c.headline,
        primaryText: c.primaryText,
      imageUrl: c.imageUrl,
      description: c.description || "",
      cta: c.cta || "LEARN_MORE",
      destinationUrl: c.destinationUrl || "",
      leadGenFormId: c.leadGenFormId || "",
      instagramUserId: c.instagramUserId || "",
      status: "READY",
    });
    } catch {
      setFeedback({
        variant: "error",
        title: "Não foi possível aprovar o criativo",
        description: "Tente novamente em instantes.",
      });
    } finally {
      setProcessingCreativeId(null);
    }
  };

  const upload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    const img = new Image();
    const objectUrl = URL.createObjectURL(file);
    img.onload = async () => {
      if (img.width < 600) {
        setFeedback({
          variant: "warning",
          title: "Imagem muito pequena",
          description:
            "Use arquivos com pelo menos 600px de largura para manter a qualidade dos criativos.",
        });
        URL.revokeObjectURL(objectUrl);
        return;
      }
      const fd = new FormData();
      fd.append("file", file);
      try {
        const res = await fetch("/api/assets", { method: "POST", body: fd });
        if (!res.ok) {
          throw new Error("Upload failed");
        }
        const url = await res.text();
        setForm((prev) => ({ ...prev, imageUrl: url }));
        setFeedback({
          variant: "success",
          title: "Imagem carregada",
          description: "O criativo foi atualizado com a nova imagem.",
        });
      } catch {
        setFeedback({
          variant: "error",
          title: "Não foi possível enviar a imagem",
          description: "Tente novamente em instantes.",
        });
      } finally {
        URL.revokeObjectURL(objectUrl);
      }
    };
    img.onerror = () => {
      setFeedback({
        variant: "error",
        title: "Não foi possível ler a imagem",
        description: "Selecione outro arquivo ou tente novamente.",
      });
      URL.revokeObjectURL(objectUrl);
    };
    img.src = objectUrl;
  };

  const submitRequestCreatives = async () => {
    const qty = Number.parseInt(requestQuantity, 10);
    if (!Number.isInteger(qty) || qty <= 0) {
      setRequestError("Informe um número válido maior que zero.");
      return;
    }
    setRequestError(null);
    try {
      await requestCreatives.mutateAsync(qty);
      setFeedback({
        variant: "success",
        title: "Solicitação enviada",
        description: `Geraremos ${qty} ${qty === 1 ? "criativo" : "criativos"} em breve.`,
      });
      closeRequestDialog();
    } catch {
      setRequestError("Não foi possível enviar o pedido agora. Tente novamente.");
      setFeedback({
        variant: "error",
        title: "Erro ao solicitar criativos",
        description: "Tente novamente em instantes.",
      });
    }
  };

  const totalCreatives = creatives.length;
  const solicitedCreatives = experiment?.creativesToGenerate ?? 0;
  const readyCreatives = creatives.filter((c) => c.status === "READY");
  const pendingCreatives = creatives.filter((c) => c.status !== "READY");
  const creativeSections = [
    {
      id: "approved",
      title: "Aprovados",
      badgeClass: "text-bg-success",
      creatives: readyCreatives,
    },
    {
      id: "pending",
      title: "Aguardando aprovação",
      badgeClass: "text-bg-warning",
      creatives: pendingCreatives,
    },
  ].filter((section) => section.creatives.length > 0);

  const renderCreativeCard = (c: Creative) => {
    const imageUrl = c.imageUrl ? resolveAssetUrl(c.imageUrl) : undefined;
    const isProcessing = processingCreativeId === c.id;
    return (
      <article
        key={c.id}
        className="creative-card"
        aria-busy={isProcessing}
        aria-live={isProcessing ? "polite" : undefined}
      >
        {isProcessing && (
          <div className="creative-card-processing">
            <div className="spinner-border text-primary" role="status">
              <span className="visually-hidden">Processando criativo...</span>
            </div>
          </div>
        )}
        {imageUrl ? (
          <img
            src={imageUrl}
            alt={c.headline || "Criativo"}
            className="creative-card-img"
          />
        ) : (
          <div className="creative-card-placeholder">
            <span className="text-muted">Imagem não disponível</span>
          </div>
        )}
        <div className="creative-card-body">
          <div className="d-flex flex-wrap align-items-center justify-content-between gap-2">
            <span className={`badge rounded-pill ${statusVariant(c.status)}`}>
              {statusLabel(c.status)}
            </span>
            {c.format && (
              <span className="badge rounded-pill text-bg-light text-uppercase text-muted">
                {c.format}
              </span>
            )}
          </div>
          <h3 className="creative-card-headline">
            {c.headline || "Sem headline"}
          </h3>
          <p className="creative-card-text mb-0">{c.primaryText}</p>
          {(c.cta || c.destinationUrl || c.leadGenFormId) && (
            <div className="creative-card-meta small text-muted">
              {c.cta && <span className="me-2">CTA: {c.cta}</span>}
              {c.destinationUrl && (
                <a
                  href={c.destinationUrl}
                  target="_blank"
                  rel="noreferrer"
                  className="text-decoration-none text-muted text-truncate d-block"
                  title={c.destinationUrl}
                >
                  {c.destinationUrl}
                </a>
              )}
              {c.leadGenFormId && (
                <span className="d-block mt-1">Formulário: {c.leadGenFormId}</span>
              )}
            </div>
          )}
        </div>
        <div className="creative-card-footer">
          <div className="creative-card-actions">
            <button
              type="button"
              className="btn btn-outline-primary btn-sm d-flex align-items-center justify-content-center gap-1"
              onClick={() => openEdit(c)}
              disabled={isProcessing}
            >
              <Edit3 size={ICON_SIZE} />
              <span>Editar</span>
            </button>
            <button
              type="button"
              className="btn btn-outline-danger btn-sm d-flex align-items-center justify-content-center gap-1"
              onClick={() => remove(c)}
              disabled={isProcessing}
            >
              <Trash2 size={ICON_SIZE} />
              <span>Excluir</span>
            </button>
            {c.status !== "READY" && (
              <button
                type="button"
                className="btn btn-outline-success btn-sm d-flex align-items-center justify-content-center gap-1"
                onClick={() => approve(c)}
                disabled={isProcessing}
              >
                <CheckCircle2 size={ICON_SIZE} />
                <span>Aprovar</span>
              </button>
            )}
            <button
              type="button"
              className="btn btn-outline-secondary btn-sm d-flex align-items-center justify-content-center gap-1"
              onClick={() => startPreview(c)}
              aria-label="Preview"
              disabled={isProcessing}
            >
              <Eye size={ICON_SIZE} />
              <span>Preview</span>
            </button>
          </div>
        </div>
      </article>
    );
  };

  return (
    <div className="mt-3">
      {feedback && (
        <div
          className={`creative-feedback creative-feedback-${feedback.variant}`}
          role="alert"
          aria-live="polite"
        >
          <div className="creative-feedback-icon" aria-hidden>
            {feedback.variant === "success" ? (
              <CheckCircle2 size={20} />
            ) : feedback.variant === "warning" ? (
              <AlertTriangle size={20} />
            ) : (
              <XCircle size={20} />
            )}
          </div>
          <div className="creative-feedback-content">
            <p className="creative-feedback-title">{feedback.title}</p>
            {feedback.description && (
              <p className="creative-feedback-description">{feedback.description}</p>
            )}
          </div>
          <button
            type="button"
            className="creative-feedback-close"
            onClick={dismissFeedback}
            aria-label="Dispensar aviso"
          >
            <X size={16} />
          </button>
        </div>
      )}
      <div className="mb-4">
        <label className="form-label" htmlFor="experiment-page-id">
          Página do Facebook deste experimento
        </label>
        <div className="d-flex flex-wrap gap-2">
          <select
            id="experiment-page-id"
            className="form-select"
            value={experimentPageId}
            onChange={(e) => setExperimentPageId(e.target.value)}
            disabled={isSavingPageId || isLoadingFacebookPages}
          >
            <option value="">
              {isLoadingFacebookPages
                ? "Carregando páginas cadastradas..."
                : "Nenhuma página selecionada"}
            </option>
            {Array.isArray(facebookPages) &&
              facebookPages.map((page) => (
                <option key={page.id} value={page.id}>
                  {page.name} ({page.pageId})
                </option>
              ))}
          </select>
          <button
            type="button"
            className="btn btn-primary d-flex align-items-center gap-2"
            onClick={handleSavePageId}
            disabled={isSavingPageId || !experiment}
          >
            {isSavingPageId ? (
              <>
                <span
                  className="spinner-border spinner-border-sm"
                  role="status"
                  aria-hidden
                />
                <span>Salvando...</span>
              </>
            ) : (
              <span>Salvar página</span>
            )}
          </button>
        </div>
        <div className="form-text">
          Todos os criativos aprovados publicarão na página selecionada. Deixe
          em branco para usar a página padrão configurada no worker.
        </div>
      </div>
      <div className="creative-toolbar">
        <div>
          <h2 className="h5 mb-1">Biblioteca de criativos</h2>
          <div className="d-flex flex-wrap align-items-center gap-2 text-muted small">
            <span className="badge rounded-pill text-bg-primary">
              {totalCreatives} {totalCreatives === 1 ? "item" : "itens"}
            </span>
            <span className="badge rounded-pill text-bg-info">
              Solicitados: {solicitedCreatives}
            </span>
          </div>
        </div>
        <div className="d-flex flex-wrap gap-2">
          <button
            type="button"
            className="btn btn-outline-secondary d-flex align-items-center gap-2"
            onClick={openRequestDialog}
            disabled={requestCreatives.isPending}
          >
            {requestCreatives.isPending ? (
              <span className="spinner-border spinner-border-sm" role="status" />
            ) : (
              <Sparkles size={ICON_SIZE} />
            )}
            <span>{requestCreatives.isPending ? "Solicitando..." : "Gerar criativos"}</span>
          </button>
        </div>
      </div>

      {isLoading ? (
        <div className="d-flex justify-content-center py-5">
          <div className="spinner-border text-primary" role="status">
            <span className="visually-hidden">Carregando...</span>
          </div>
        </div>
      ) : totalCreatives === 0 ? (
        <div className="creative-empty-state">
          <div className="creative-empty-icon" aria-hidden>
            🎨
          </div>
          <h3 className="h6 fw-semibold mb-1">Nenhum criativo cadastrado</h3>
          <p className="text-muted mb-0">
            Gere sugestões com IA para começar a testar variações e construa seu
            acervo criativo com os resultados aprovados.
          </p>
        </div>
      ) : creativeSections.length === 0 ? (
        <div className="creative-grid">
          {creatives.map(renderCreativeCard)}
        </div>
      ) : (
        <div className="creative-sections">
          {creativeSections.map((section) => (
            <section
              key={section.id}
              className="creative-section"
              aria-labelledby={`${section.id}-title`}
            >
              <div className="creative-section-header">
                <h3 id={`${section.id}-title`} className="creative-section-title">
                  {section.title}
                </h3>
                <span className={`badge rounded-pill ${section.badgeClass}`}>
                  {`${section.creatives.length} ${
                    section.creatives.length === 1 ? "item" : "itens"
                  }`}
                </span>
              </div>
              <div className="creative-grid">
                {section.creatives.map(renderCreativeCard)}
              </div>
            </section>
          ))}
        </div>
      )}

      {isRequestDialogOpen && (
        <div
          className="modal d-block creative-request-modal"
          tabIndex={-1}
          role="dialog"
          aria-modal="true"
        >
          <div className="modal-dialog modal-dialog-centered">
            <div className="modal-content">
              <div className="modal-header">
                <h5 className="modal-title">Gerar novos criativos</h5>
                <button
                  type="button"
                  className="btn-close"
                  onClick={closeRequestDialog}
                  aria-label="Fechar"
                />
              </div>
              <div className="modal-body">
                <div className="creative-request-body">
                  <p className="mb-0 text-muted">
                    Informe quantos novos criativos deseja solicitar para este experimento.
                  </p>
                  <div>
                    <label className="form-label" htmlFor="requestedQuantity">
                      Quantidade de criativos
                    </label>
                    <input
                      id="requestedQuantity"
                      type="number"
                      min={1}
                      className="form-control"
                      value={requestQuantity}
                      onChange={(e) => setRequestQuantity(e.target.value)}
                      disabled={requestCreatives.isPending}
                    />
                  </div>
                  {requestError && (
                    <div className="creative-request-error" role="alert">
                      <XCircle size={18} />
                      <span>{requestError}</span>
                    </div>
                  )}
                </div>
              </div>
              <div className="modal-footer">
                <button
                  type="button"
                  className="btn btn-outline-secondary"
                  onClick={closeRequestDialog}
                >
                  Cancelar
                </button>
                <button
                  type="button"
                  className="btn btn-primary d-flex align-items-center gap-2"
                  onClick={submitRequestCreatives}
                  disabled={requestCreatives.isPending}
                >
                  {requestCreatives.isPending ? (
                    <span className="spinner-border spinner-border-sm" role="status" />
                  ) : (
                    <Sparkles size={ICON_SIZE} />
                  )}
                  <span>{requestCreatives.isPending ? "Enviando..." : "Solicitar"}</span>
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {showForm && (
        <div className="modal d-block" tabIndex={-1}>
          <div className="modal-dialog">
            <div className="modal-content">
              <div className="modal-header">
                <h5 className="modal-title">
                  {editing ? "Editar" : "Novo"} Criativo
                </h5>
                <button
                  type="button"
                  className="btn-close"
                  onClick={() => setShowForm(false)}
                />
              </div>
              <div className="modal-body">
                <select
                  className="form-select mb-2"
                  value={form.format}
                  onChange={(e) => setForm({ ...form, format: e.target.value })}
                >
                  <option value="LINK">LINK</option>
                  <option value="VIDEO">VIDEO</option>
                  <option value="CAROUSEL">CAROUSEL</option>
                </select>
                <textarea
                  className="form-control mb-2"
                  placeholder="Primary Text"
                  maxLength={125}
                  value={form.primaryText}
                  title="máx. 125 caracteres"
                  onChange={(e) =>
                    setForm({ ...form, primaryText: e.target.value })
                  }
                />
                <input
                  className="form-control mb-2"
                  placeholder="Headline"
                  maxLength={40}
                  value={form.headline}
                  title="máx. 40 caracteres"
                  onChange={(e) =>
                    setForm({ ...form, headline: e.target.value })
                  }
                />
                <input
                  className="form-control mb-2"
                  placeholder="Descrição (opcional)"
                  value={form.description}
                  onChange={(e) =>
                    setForm({ ...form, description: e.target.value })
                  }
                />
                <label className="form-label" htmlFor="creative-cta">
                  Chamada para ação
                </label>
                <select
                  id="creative-cta"
                  className="form-select mb-2"
                  value={form.cta}
                  onChange={(e) => setForm({ ...form, cta: e.target.value })}
                >
                  {FACEBOOK_CALL_TO_ACTIONS.map((option) => (
                    <option key={option} value={option}>
                      {option}
                    </option>
                  ))}
                </select>
                <label className="form-label" htmlFor="creative-destination-url">
                  URL de destino
                </label>
                <input
                  id="creative-destination-url"
                  className="form-control mb-2"
                  placeholder="https://exemplo.com/pagina"
                  value={form.destinationUrl}
                  onChange={(e) =>
                    setForm({ ...form, destinationUrl: e.target.value })
                  }
                />
                <label className="form-label" htmlFor="creative-lead-form">
                  ID do formulário de leads (Instagram/Facebook)
                </label>
                <input
                  id="creative-lead-form"
                  className="form-control mb-2"
                  placeholder="Ex.: 123456789012345"
                  value={form.leadGenFormId}
                  onChange={(e) =>
                    setForm({ ...form, leadGenFormId: e.target.value })
                  }
                />
                <div className="form-text mb-2">
                  Informe ao menos uma das opções de destino (URL ou formulário). O worker usará o formulário quando ambos estiverem preenchidos.
                </div>
                <input
                  type="file"
                  className="form-control mb-2"
                  onChange={upload}
                />
                <input
                  className="form-control mb-2"
                  placeholder="instagram_user_id"
                  value={form.instagramUserId}
                  onChange={(e) =>
                    setForm({ ...form, instagramUserId: e.target.value })
                  }
                />
                {!editing && (
                  <>
                    <select
                      className="form-select mb-2"
                      value={selectedAngle}
                      onChange={(e) => setSelectedAngle(e.target.value)}
                    >
                      {Array.isArray(angles) &&
                        angles.map((a) => (
                          <option key={a.id} value={a.id}>
                            {a.name}
                          </option>
                        ))}
                    </select>
                    <select
                      className="form-select mb-2"
                      value={selectedProof}
                      onChange={(e) => setSelectedProof(e.target.value)}
                    >
                      {Array.isArray(proofs) &&
                        proofs.map((p) => (
                          <option key={p.id} value={p.id}>
                            {p.name}
                          </option>
                        ))}
                    </select>
                    <select
                      className="form-select mb-2"
                      value={selectedTrigger}
                      onChange={(e) => setSelectedTrigger(e.target.value)}
                    >
                      {Array.isArray(triggers) &&
                        triggers.map((t) => (
                          <option key={t.id} value={t.id}>
                            {t.name}
                          </option>
                        ))}
                    </select>
                  </>
                )}
                <select
                  className="form-select"
                  value={form.status}
                  onChange={(e) => setForm({ ...form, status: e.target.value })}
                >
                  <option value="DRAFT">DRAFT</option>
                  <option value="READY">READY</option>
                </select>
              </div>
              <div className="modal-footer">
                <button
                  type="button"
                  className="btn btn-secondary"
                  onClick={() => setShowForm(false)}
                >
                  Cancelar
                </button>
                <button
                  type="button"
                  className="btn btn-primary"
                  onClick={handleFormSubmit(
                    async () => {
                      await submit();
                    },
                    (errors) => {
                      console.log("Validation errors", errors);
                    },
                  )}
                >
                  Salvar
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {showPreview && editing && (
        <div className="modal d-block" tabIndex={-1}>
          <div className="modal-dialog">
            <div className="modal-content">
              <div className="modal-header">
                <h5 className="modal-title">Preview</h5>
                <button
                  type="button"
                  className="btn-close"
                  onClick={() => setShowPreview(false)}
                />
              </div>
              <div className="modal-body">
                <InstagramAdPreview creative={editing} />
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
