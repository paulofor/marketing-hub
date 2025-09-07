import { useState } from "react";
import { useForm } from "react-hook-form";
import { Creative, useCreatives } from "../../api/creative/useCreatives";
import { useCreateCreative } from "../../api/creative/useCreateCreative";
import { useUpdateCreative } from "../../api/creative/useUpdateCreative";
import { useDeleteCreative } from "../../api/creative/useDeleteCreative";
import { usePreviewCreative } from "../../api/creative/usePreviewCreative";
import { useRequestCreatives } from "../../api/experiment/useRequestCreatives";
import { useExperiment } from "../../api/experiment/useExperiment";

interface Props {
  experimentId: string;
}

interface CreativeForm {
  format: string;
  primaryText: string;
  headline: string;
  description: string;
  cta: string;
  url: string;
  imageUrl: string;
  videoId: string;
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
  const { register, handleSubmit, reset, setValue } = useForm<CreativeForm>({
    defaultValues: {
      format: "LINK",
      primaryText: "",
      headline: "",
      description: "",
      cta: "",
      url: "",
      imageUrl: "",
      videoId: "",
      pageId: "",
      instagramUserId: "",
      status: "DRAFT",
    },
  });
  const create = useCreateCreative(experimentId);
  const update = editing ? useUpdateCreative(editing.id, experimentId) : null;
  const { data: previewHtml, refetch } = usePreviewCreative(
    editing?.id ?? 0,
    false,
  );
  const [showPreview, setShowPreview] = useState(false);
  const requestCreatives = useRequestCreatives(experimentId);

  const openNew = () => {
    setEditing(null);
    reset();
    setShowForm(true);
  };

  const onSubmit = async (data: CreativeForm) => {
    if (editing) {
      await update?.mutateAsync(data);
    } else {
      await create.mutateAsync(data);
    }
    setShowForm(false);
  };

  const duplicate = async (c: Creative) => {
    const { id, ...rest } = c as any;
    await create.mutateAsync(rest);
  };

  const startPreview = async (c: Creative) => {
    setEditing(c);
    setShowPreview(true);
    await refetch();
  };

  const remove = async (c: Creative) => {
    if (!confirm("Excluir criativo?")) return;
    await useDeleteCreative(c.id, experimentId).mutateAsync();
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
      setValue("imageUrl", url);
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
      <button className="btn btn-primary mb-2" onClick={openNew}>
        Novo Criativo
      </button>
      <button
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
                  <img src={c.imageUrl} style={{ width: 80 }} />
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
                    className="btn btn-sm btn-outline-primary me-1"
                    onClick={() => {
                      setEditing(c);
                      reset(c);
                      setShowForm(true);
                    }}
                  >
                    🖊
                  </button>
                  <button
                    className="btn btn-sm btn-outline-secondary me-1"
                    onClick={() => duplicate(c)}
                  >
                    ⧉
                  </button>
                  <button
                    className="btn btn-sm btn-outline-danger me-1"
                    onClick={() => remove(c)}
                  >
                    🗑
                  </button>
                  <button
                    className="btn btn-sm btn-outline-secondary"
                    onClick={() => startPreview(c)}
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
                  className="btn-close"
                  onClick={() => setShowForm(false)}
                />
              </div>
              <div className="modal-body">
                <select className="form-select mb-2" {...register("format")}>
                  <option value="LINK">LINK</option>
                  <option value="VIDEO">VIDEO</option>
                  <option value="CAROUSEL">CAROUSEL</option>
                </select>
                <textarea
                  className="form-control mb-2"
                  placeholder="Primary Text"
                  maxLength={125}
                  {...register("primaryText")}
                />
                <input
                  className="form-control mb-2"
                  placeholder="Headline"
                  maxLength={40}
                  title="máx. 40 caracteres"
                  {...register("headline")}
                />
                <input
                  className="form-control mb-2"
                  placeholder="Descrição"
                  {...register("description")}
                />
                <input
                  className="form-control mb-2"
                  placeholder="CTA"
                  {...register("cta")}
                />
                <input
                  className="form-control mb-2"
                  placeholder="URL"
                  {...register("url")}
                />
                <input
                  type="file"
                  className="form-control mb-2"
                  onChange={upload}
                />
                <input
                  className="form-control mb-2"
                  placeholder="page_id"
                  {...register("pageId")}
                />
                <input
                  className="form-control mb-2"
                  placeholder="instagram_user_id"
                  {...register("instagramUserId")}
                />
                <select className="form-select" {...register("status")}>
                  <option value="DRAFT">DRAFT</option>
                  <option value="READY">READY</option>
                </select>
              </div>
              <div className="modal-footer">
                <button
                  className="btn btn-secondary"
                  onClick={() => setShowForm(false)}
                >
                  Cancelar
                </button>
                <button
                  className="btn btn-primary"
                  onClick={handleSubmit(
                    onSubmit,
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

      {showPreview && (
        <div className="modal d-block" tabIndex={-1}>
          <div className="modal-dialog modal-xl">
            <div className="modal-content">
              <div className="modal-header">
                <h5 className="modal-title">Preview</h5>
                <button
                  className="btn-close"
                  onClick={() => setShowPreview(false)}
                />
              </div>
              <div className="modal-body">
                <iframe
                  title="preview"
                  style={{ width: "100%", height: "80vh" }}
                  srcDoc={previewHtml || ""}
                />
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
