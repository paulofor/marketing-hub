import { useEffect, useState } from "react";
import { useAngles, Angle } from "../api/angle/useAngles";
import { useCreateAngle } from "../api/angle/useCreateAngle";
import { useUpdateAngle } from "../api/angle/useUpdateAngle";
import PageTitle from "../components/PageTitle";

export default function AnglesPage() {
  const { data } = useAngles();
  const [angles, setAngles] = useState<Angle[]>([]);
  const create = useCreateAngle();
  const update = useUpdateAngle();
  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const [editId, setEditId] = useState<number | null>(null);
  const [editName, setEditName] = useState("");
  const [editDescription, setEditDescription] = useState("");

  useEffect(() => {
    if (Array.isArray(data)) setAngles(data);
  }, [data]);

  const disabled = !name.trim();

  const submit = async () => {
    if (disabled) return;
    try {
      const res = await create.mutateAsync({ name, description });
      setAngles([...angles, res]);
      setName("");
      setDescription("");
    } catch {
      alert("Erro ao salvar Angle");
    }
  };

  const confirm = async (id: number) => {
    try {
      const updated = await update.mutateAsync({
        id,
        name: editName,
        description: editDescription,
      });
      setAngles(angles.map((a) => (a.id === id ? updated : a)));
      setEditId(null);
    } catch {
      alert("Erro ao atualizar Angle");
    }
  };
  return (
    <div>
      <PageTitle>Angles</PageTitle>
      <input
        className="form-control mb-2"
        placeholder="Name"
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
        Novo Angle
      </button>
      <ul>
        {angles.map((a) => (
          <li key={a.id} className="mb-1">
            {editId === a.id ? (
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
                <button className="btn btn-success btn-sm me-1" onClick={() => confirm(a.id)}>
                  ✔️
                </button>
                <button className="btn btn-secondary btn-sm" onClick={() => setEditId(null)}>
                  ✖️
                </button>
              </>
            ) : (
              <div>
                <div className="d-flex align-items-start">
                  <div className="flex-grow-1">
                    {a.name}
                    {a.description && (
                      <div className="text-muted small">{a.description}</div>
                    )}
                  </div>
                  <button
                    className="btn btn-link p-0"
                    onClick={() => {
                      setEditId(a.id);
                      setEditName(a.name);
                      setEditDescription(a.description || "");
                    }}>
                    ✏️
                  </button>
                </div>
              </div>
            )}
          </li>
        ))}
      </ul>
    </div>
  );
}
