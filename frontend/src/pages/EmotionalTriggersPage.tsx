import { useEffect, useState } from "react";
import {
  useEmotionalTriggers,
  EmotionalTrigger,
} from "../api/emotionalTrigger/useEmotionalTriggers";
import { useCreateEmotionalTrigger } from "../api/emotionalTrigger/useCreateEmotionalTrigger";
import { useUpdateEmotionalTrigger } from "../api/emotionalTrigger/useUpdateEmotionalTrigger";
import PageTitle from "../components/PageTitle";

export default function EmotionalTriggersPage() {
  const { data } = useEmotionalTriggers();
  const [triggers, setTriggers] = useState<EmotionalTrigger[]>([]);
  const create = useCreateEmotionalTrigger();
  const update = useUpdateEmotionalTrigger();
  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const [editId, setEditId] = useState<number | null>(null);
  const [editName, setEditName] = useState("");
  const [editDescription, setEditDescription] = useState("");

  useEffect(() => {
    if (Array.isArray(data)) setTriggers(data);
  }, [data]);

  const disabled = !name.trim();

  const submit = async () => {
    if (disabled) return;
    try {
      const res = await create.mutateAsync({ name, description });
      setTriggers([...triggers, res]);
      setName("");
      setDescription("");
    } catch {
      alert("Erro ao salvar Gatilho Emocional");
    }
  };

  const confirm = async (id: number) => {
    try {
      const updated = await update.mutateAsync({
        id,
        name: editName,
        description: editDescription,
      });
      setTriggers(triggers.map((t) => (t.id === id ? updated : t)));
      setEditId(null);
    } catch {
      alert("Erro ao atualizar Gatilho Emocional");
    }
  };
  return (
    <div>
      <PageTitle>Gatilhos Emocionais</PageTitle>
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
      <button
        className="btn btn-primary mb-3"
        onClick={submit}
        disabled={disabled}
      >
        Novo Gatilho Emocional
      </button>
      <ul>
        {triggers.map((t) => (
          <li key={t.id} className="mb-1">
            {editId === t.id ? (
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
                <button
                  className="btn btn-success btn-sm me-1"
                  onClick={() => confirm(t.id)}
                >
                  ✔️
                </button>
                <button
                  className="btn btn-secondary btn-sm"
                  onClick={() => setEditId(null)}
                >
                  ✖️
                </button>
              </>
            ) : (
              <div className="d-flex align-items-start">
                <div className="flex-grow-1">
                  {t.name}
                  {t.description && (
                    <div className="text-muted small">{t.description}</div>
                  )}
                </div>
                <button
                  className="btn btn-link p-0"
                  onClick={() => {
                    setEditId(t.id);
                    setEditName(t.name);
                    setEditDescription(t.description || "");
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
