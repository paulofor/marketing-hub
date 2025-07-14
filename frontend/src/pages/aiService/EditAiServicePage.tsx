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
      <input
        className="form-control mb-2"
        placeholder="Nome"
        value={form.name}
        onChange={(e) => setForm({ ...form, name: e.target.value })}
      />
      <textarea
        className="form-control mb-2"
        placeholder="Objetivo"
        value={form.objective}
        onChange={(e) => setForm({ ...form, objective: e.target.value })}
        rows={3}
      />
      <input
        className="form-control mb-2"
        placeholder="URL"
        value={form.url}
        onChange={(e) => setForm({ ...form, url: e.target.value })}
      />
      <input
        className="form-control mb-2"
        placeholder="Preço"
        value={form.price}
        onChange={(e) => setForm({ ...form, price: Number(e.target.value) })}
      />
      <input
        className="form-control mb-2"
        placeholder="Custo"
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
