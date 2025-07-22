import { useEffect, useState } from "react";
import { useVisualProofs, VisualProof } from "../api/visualProof/useVisualProofs";
import { useCreateVisualProof } from "../api/visualProof/useCreateVisualProof";
import { useUpdateVisualProof } from "../api/visualProof/useUpdateVisualProof";
import PageTitle from "../components/PageTitle";

export default function VisualProofsPage() {
  const { data } = useVisualProofs();
  const [proofs, setProofs] = useState<VisualProof[]>([]);
  const create = useCreateVisualProof();
  const update = useUpdateVisualProof();
  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const [editId, setEditId] = useState<number | null>(null);
  const [editName, setEditName] = useState("");
  const [editDescription, setEditDescription] = useState("");

  useEffect(() => {
    if (Array.isArray(data)) setProofs(data);
  }, [data]);

  const disabled = !name.trim();

  const submit = async () => {
    if (disabled) return;
    try {
      const res = await create.mutateAsync({ name, description });
      setProofs([...proofs, res]);
      setName("");
      setDescription("");
    } catch {
      alert("Erro ao salvar Prova Visual");
    }
  };

  const confirm = async (id: number) => {
    try {
      const updated = await update.mutateAsync({
        id,
        name: editName,
        description: editDescription,
      });
      setProofs(proofs.map((p) => (p.id === id ? updated : p)));
      setEditId(null);
    } catch {
      alert("Erro ao atualizar Prova Visual");
    }
  };
  return (
    <div>
      <PageTitle>Provas Visuais</PageTitle>
      <input
        className="form-control mb-2"
        placeholder="Nome"
        value={name}
        onChange={(e) => setName(e.target.value)}
      />
      <textarea
        className="form-control mb-2"
        placeholder="Descrição"
        value={description}
        onChange={(e) => setDescription(e.target.value)}
        rows={3}
      />
      <button className="btn btn-primary mb-3" onClick={submit} disabled={disabled}>
        Nova Prova Visual
      </button>
      <ul>
        {proofs.map((p) => (
          <li key={p.id} className="mb-1">
            {editId === p.id ? (
              <>
                <input
                  className="form-control d-inline-block w-auto me-2"
                  value={editName}
                  onChange={(e) => setEditName(e.target.value)}
                />
                <textarea
                  className="form-control mb-2"
                  placeholder="Descrição"
                  value={editDescription}
                  onChange={(e) => setEditDescription(e.target.value)}
                  rows={2}
                />
                <button className="btn btn-success btn-sm me-1" onClick={() => confirm(p.id)}>
                  ✔️
                </button>
                <button className="btn btn-secondary btn-sm" onClick={() => setEditId(null)}>
                  ✖️
                </button>
              </>
            ) : (
              <div className="d-flex align-items-start">
                <div className="flex-grow-1">
                  {p.name}
                  {p.description && (
                    <div className="text-muted small">{p.description}</div>
                  )}
                </div>
                <button
                  className="btn btn-link p-0"
                  onClick={() => {
                    setEditId(p.id);
                    setEditName(p.name);
                    setEditDescription(p.description || "");
                  }}
                >
                  ✏️
                </button>
              </div>
            )}
          </li>
        ))}
      </ul>
    </div>
  );
}
