import { useState } from "react";
import { Creative, useCreatives } from "../../api/creative/useCreatives";
import { useCreateCreative } from "../../api/creative/useCreateCreative";
import { useUpdateCreative } from "../../api/creative/useUpdateCreative";
import { useDeleteCreative } from "../../api/creative/useDeleteCreative";
import { usePreviewCreative } from "../../api/creative/usePreviewCreative";

interface Props {
  experimentId: string;
}

export default function CriativosTab({ experimentId }: Props) {
  const { data } = useCreatives(experimentId);
  const creatives = Array.isArray(data) ? data : [];
  const [showForm, setShowForm] = useState(false);
  const [editing, setEditing] = useState<Creative | null>(null);
  const [form, setForm] = useState({
    headline: "",
    primaryText: "",
    imageUrl: "",
    status: "DRAFT",
  });
  const create = useCreateCreative(experimentId);
  const update = editing ? useUpdateCreative(editing.id, experimentId) : null;
  const del = useDeleteCreative(0, experimentId); // id changed dynamically
  const { data: previewHtml, refetch } = usePreviewCreative(
    editing?.id ?? 0,
    false,
  );
  const [showPreview, setShowPreview] = useState(false);

  const openNew = () => {
    setEditing(null);
    setForm({ headline: "", primaryText: "", imageUrl: "", status: "DRAFT" });
    setShowForm(true);
  };

  const submit = async () => {
    if (editing) {
      await update?.mutateAsync(form);
    } else {
      await create.mutateAsync(form);
    }
    setShowForm(false);
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
      setForm({ ...form, imageUrl: url });
    };
    img.src = URL.createObjectURL(file);
  };

  return (
    <div className="mt-3">
      <button className="btn btn-primary mb-2" onClick={openNew}>
        Novo Criativo
      </button>
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
                      c.status === "READY" ? "badge bg-success" : "badge bg-secondary"
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
                      setForm({
                        headline: c.headline,
                        primaryText: c.primaryText,
                        imageUrl: c.imageUrl,
                        status: c.status,
                      });
                      setShowForm(true);
                    }}
                  >
                    🖊
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
                <h5 className="modal-title">{editing ? "Editar" : "Novo"} Criativo</h5>
                <button className="btn-close" onClick={() => setShowForm(false)} />
              </div>
              <div className="modal-body">
                <input type="file" className="form-control mb-2" onChange={upload} />
                <input
                  className="form-control mb-2"
                  placeholder="Headline"
                  maxLength={40}
                  value={form.headline}
                  title="máx. 40 caracteres"
                  onChange={(e) => setForm({ ...form, headline: e.target.value })}
                />
                <textarea
                  className="form-control mb-2"
                  placeholder="Primary Text"
                  maxLength={125}
                  value={form.primaryText}
                  title="máx. 125 caracteres"
                  onChange={(e) => setForm({ ...form, primaryText: e.target.value })}
                />
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
                  className="btn btn-secondary"
                  onClick={() => setShowForm(false)}
                >
                  Cancelar
                </button>
                <button className="btn btn-primary" onClick={submit}>
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
                <button className="btn-close" onClick={() => setShowPreview(false)} />
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
