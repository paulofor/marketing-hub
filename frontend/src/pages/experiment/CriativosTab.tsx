import { useState } from "react";
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
import InstagramAdPreview from "../../components/InstagramAdPreview";
import { resolveAssetUrl } from "../../utils/resolveAssetUrl";
import {
  CheckCircle2,
  Copy,
  Edit3,
  Eye,
  Plus,
  Sparkles,
  Trash2,
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
  imageUrl: string;
  pageId: string;
  instagramUserId: string;
  status: string;
}

const ICON_SIZE = 16;

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
  const [showForm, setShowForm] = useState(false);
  const [editing, setEditing] = useState<Creative | null>(null);
  const [form, setForm] = useState<CreativeForm>({
    format: "LINK",
    headline: "",
    primaryText: "",
    description: "",
    cta: "LEARN_MORE",
    destinationUrl: "",
    imageUrl: "",
    pageId: "",
    instagramUserId: "",
    status: "DRAFT",
  });
  const { handleSubmit: handleFormSubmit } = useForm<CreativeForm>();
  const { data: angles } = useAngles();
  const { data: proofs } = useVisualProofs();
  const { data: triggers } = useEmotionalTriggers();
  const [selectedAngle, setSelectedAngle] = useState<string>("");
  const [selectedProof, setSelectedProof] = useState<string>("");
  const [selectedTrigger, setSelectedTrigger] = useState<string>("");
  const patchLabels = useUpdateCreativeLabels(experimentId);
  const create = useCreateCreative(experimentId);
  const update = useUpdateCreative(experimentId);
  const del = useDeleteCreative(experimentId);
  const [showPreview, setShowPreview] = useState(false);
  const requestCreatives = useRequestCreatives(experimentId);

  const fillFormFromCreative = (c: Creative) => {
    setForm({
      format: c.format || "LINK",
      headline: c.headline,
      primaryText: c.primaryText,
      description: c.description || "",
      cta: c.cta || "LEARN_MORE",
      destinationUrl: c.destinationUrl || "",
      imageUrl: c.imageUrl,
      pageId: c.pageId || "",
      instagramUserId: c.instagramUserId || "",
      status: c.status,
    });
  };

  const openNew = () => {
    setEditing(null);
    setForm({
      format: "LINK",
      headline: "",
      primaryText: "",
      description: "",
      cta: "LEARN_MORE",
      destinationUrl: "",
      imageUrl: "",
      pageId: "",
      instagramUserId: "",
      status: "DRAFT",
    });
    setSelectedAngle("");
    setSelectedProof("");
    setSelectedTrigger("");
    setShowForm(true);
  };

  const openEdit = (c: Creative) => {
    setEditing(c);
    fillFormFromCreative(c);
    setShowForm(true);
  };

  const submit = async () => {
    const payload = {
      headline: form.headline,
      primaryText: form.primaryText,
      imageUrl: form.imageUrl,
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
    await del.mutateAsync(c.id);
  };

  const approve = async (c: Creative) => {
    await update.mutateAsync({
      id: c.id,
      headline: c.headline,
      primaryText: c.primaryText,
      imageUrl: c.imageUrl,
      status: "READY",
    });
  };

  const duplicate = (c: Creative) => {
    setEditing(null);
    fillFormFromCreative(c);
    setShowForm(true);
  };

  const upload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    const img = new Image();
    img.onload = async () => {
      if (img.width < 600) {
        alert("Largura mínima 600px");
        return;
      }
      const fd = new FormData();
      fd.append("file", file);
      const res = await fetch("/api/assets", { method: "POST", body: fd });
      const url = await res.text();
      setForm({ ...form, imageUrl: url });
    };
    img.src = URL.createObjectURL(file);
  };

  const generateCreatives = async () => {
    const qtyStr = prompt("Quantos criativos gerar?");
    if (!qtyStr) return;
    const qty = Number(qtyStr);
    if (!qty || qty <= 0) return;
    try {
      await requestCreatives.mutateAsync(qty);
      alert("Solicitação enviada!");
    } catch {
      alert("Erro ao solicitar criativos");
    }
  };

  const totalCreatives = creatives.length;
  const solicitedCreatives = experiment?.creativesToGenerate ?? 0;

  return (
    <div className="mt-3">
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
            onClick={generateCreatives}
            disabled={requestCreatives.isPending}
          >
            {requestCreatives.isPending ? (
              <span className="spinner-border spinner-border-sm" role="status" />
            ) : (
              <Sparkles size={ICON_SIZE} />
            )}
            <span>{requestCreatives.isPending ? "Solicitando..." : "Gerar criativos"}</span>
          </button>
          <button
            type="button"
            className="btn btn-primary d-flex align-items-center gap-2"
            onClick={openNew}
          >
            <Plus size={ICON_SIZE} />
            <span>Novo Criativo</span>
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
            Gere sugestões com IA ou cadastre um novo criativo para começar a testar
            variações.
          </p>
        </div>
      ) : (
        <div className="creative-grid">
          {creatives.map((c) => {
            const imageUrl = c.imageUrl ? resolveAssetUrl(c.imageUrl) : undefined;
            return (
              <article key={c.id} className="creative-card">
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
                  {(c.cta || c.destinationUrl) && (
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
                    </div>
                  )}
                </div>
                <div className="creative-card-footer">
                  <div className="creative-card-actions">
                    <button
                      type="button"
                      className="btn btn-outline-primary btn-sm d-flex align-items-center justify-content-center gap-1"
                      onClick={() => openEdit(c)}
                    >
                      <Edit3 size={ICON_SIZE} />
                      <span>Editar</span>
                    </button>
                    <button
                      type="button"
                      className="btn btn-outline-secondary btn-sm d-flex align-items-center justify-content-center gap-1"
                      onClick={() => duplicate(c)}
                    >
                      <Copy size={ICON_SIZE} />
                      <span>Duplicar</span>
                    </button>
                    <button
                      type="button"
                      className="btn btn-outline-danger btn-sm d-flex align-items-center justify-content-center gap-1"
                      onClick={() => remove(c)}
                    >
                      <Trash2 size={ICON_SIZE} />
                      <span>Excluir</span>
                    </button>
                    {c.status !== "READY" && (
                      <button
                        type="button"
                        className="btn btn-outline-success btn-sm d-flex align-items-center justify-content-center gap-1"
                        onClick={() => approve(c)}
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
                    >
                      <Eye size={ICON_SIZE} />
                      <span>Preview</span>
                    </button>
                  </div>
                </div>
              </article>
            );
          })}
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
                <select
                  className="form-select mb-2"
                  value={form.cta}
                  onChange={(e) => setForm({ ...form, cta: e.target.value })}
                >
                  <option value="LEARN_MORE">LEARN_MORE</option>
                  <option value="SHOP_NOW">SHOP_NOW</option>
                </select>
                <input
                  className="form-control mb-2"
                  placeholder="URL de destino"
                  value={form.destinationUrl}
                  onChange={(e) =>
                    setForm({ ...form, destinationUrl: e.target.value })
                  }
                />
                <input
                  type="file"
                  className="form-control mb-2"
                  onChange={upload}
                />
                <div className="d-flex mb-2">
                  <input
                    className="form-control me-2"
                    placeholder="page_id"
                    value={form.pageId}
                    onChange={(e) =>
                      setForm({ ...form, pageId: e.target.value })
                    }
                  />
                  <input
                    className="form-control"
                    placeholder="instagram_user_id"
                    value={form.instagramUserId}
                    onChange={(e) =>
                      setForm({ ...form, instagramUserId: e.target.value })
                    }
                  />
                </div>
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
