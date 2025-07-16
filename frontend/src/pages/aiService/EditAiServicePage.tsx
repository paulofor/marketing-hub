import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import PageTitle from "../../components/PageTitle";
import { useAiService } from "../../api/aiService/useAiService";
import { useUpdateAiService } from "../../api/aiService/useUpdateAiService";
import { AiService } from "../../api/aiService/useAiServices";

export default function EditAiServicePage() {
  const { id } = useParams<{ id: string }>();
  const serviceId = Number(id);
  const { data, isLoading } = useAiService(serviceId);
  const update = useUpdateAiService();
  const navigate = useNavigate();
  const [form, setForm] = useState<AiService>({
    id: serviceId,
    name: "",
    objective: "",
    url: "",
    phase: "",
    price: 0,
    cost: 0,
  });

  useEffect(() => {
    if (data) setForm(data);
  }, [data]);

  const submit = () => {
    update.mutate(form, { onSuccess: () => navigate("/ai-services") });
  };

  if (isLoading) return <p>Carregando...</p>;

  return (
    <div>
      <PageTitle>Editar Serviço de IA</PageTitle>
      <label className="form-label">Nome</label>
      <input
        className="form-control mb-2"
        value={form.name}
        onChange={(e) => setForm({ ...form, name: e.target.value })}
      />
      <label className="form-label">Objetivo</label>
      <textarea
        className="form-control mb-2"
        value={form.objective}
        onChange={(e) => setForm({ ...form, objective: e.target.value })}
        rows={3}
      />
      <label className="form-label">URL</label>
      <input
        className="form-control mb-2"
        value={form.url}
        onChange={(e) => setForm({ ...form, url: e.target.value })}
      />
      <label className="form-label">Fase</label>
      <input
        className="form-control mb-2"
        value={form.phase}
        onChange={(e) => setForm({ ...form, phase: e.target.value })}
      />
      <label className="form-label">Preço</label>
      <input
        className="form-control mb-2"
        value={form.price}
        onChange={(e) => setForm({ ...form, price: Number(e.target.value) })}
      />
      <label className="form-label">Custo</label>
      <input
        className="form-control mb-2"
        value={form.cost}
        onChange={(e) => setForm({ ...form, cost: Number(e.target.value) })}
      />
      <button
        className="btn btn-primary"
        onClick={submit}
        disabled={update.isPending}
      >
        {update.isPending ? (
          <>
            <span
              className="spinner-border spinner-border-sm me-2"
              role="status"
            />
            Processando...
          </>
        ) : (
          "Salvar"
        )}
      </button>
    </div>
  );
}
