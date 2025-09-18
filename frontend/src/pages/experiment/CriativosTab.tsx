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

export default function CriativosTab({ experimentId }: Props) {
  const { data } = useCreatives(experimentId);
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

  return (
    <div className="mt-3">
      <button type="button" className="btn btn-primary mb-2" onClick={openNew}>
        Novo Criativo
      </button>
      <button
        type="button"
        className="btn btn-secondary mb-2 ms-2"
        onClick={generateCreatives}
        disabled={requestCreatives.isPending}
      >
        Gerar Criativos
      </button>
      <span className="ms-2">
        Solicitados: {experiment?.creativesToGenerate ?? 0}
      </span>
      <div className="table-responsive">
        <table className="table">
          <thead>
            <tr>
              <th>Imagem</th>
              <th>Headline</th>
              <th>Primary Text</th>
              <th>Status</th>
              <th>Ações</th>
            </tr>
          </thead>
          <tbody>
            {creatives.map((c) => (
              <tr key={c.id}>
                <td>
                  <img
                    src={resolveAssetUrl(c.imageUrl)}
                    alt={c.headline || "Criativo"}
                    style={{ width: 80 }}
                  />
                </td>
                <td>{c.headline}</td>
                <td>{c.primaryText.slice(0, 60)}</td>
                <td>
                  <span
                    className={
                      c.status === "READY"
                        ? "badge bg-success"
                        : "badge bg-secondary"
                    }
                  >
                    {c.status}
                  </span>
                </td>
                <td>
                  <button
                    type="button"
                    className="btn btn-sm btn-outline-primary me-1"
                    onClick={() => {
                      setEditing(c);
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
                      setShowForm(true);
                    }}
                  >
                    🖊
                  </button>
                  <button
                    type="button"
                    className="btn btn-sm btn-outline-secondary me-1"
                    onClick={() => duplicate(c)}
                  >
                    Duplicar
                  </button>
                  <button
                    type="button"
                    className="btn btn-sm btn-outline-danger me-1"
                    onClick={() => remove(c)}
                  >
                    🗑
                  </button>
                  {c.status !== "READY" && (
                    <button
                      type="button"
                      className="btn btn-sm btn-outline-success me-1"
                      onClick={() => approve(c)}
                    >
                      Aprovar
                    </button>
                  )}
                  <button
                    type="button"
                    className="btn btn-sm btn-outline-secondary"
                    onClick={() => startPreview(c)}
                    aria-label="Preview"
                  >
                    👁
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

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
