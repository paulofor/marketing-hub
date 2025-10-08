import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useCreateAiService } from "../../api/aiService/useCreateAiService";
import PageTitle from "../../components/PageTitle";

export default function NewAiServicePage() {
  const create = useCreateAiService();
  const navigate = useNavigate();
  const [form, setForm] = useState({
    name: "",
    objective: "",
    url: "",
    phase: "",
    price: "",
    cost: "",
    observation: "",
  });

  const submit = () => {
    create.mutate(
      {
        ...form,
        price: Number(form.price),
        cost: Number(form.cost),
        observation: form.observation,
      },
      {
        onSuccess: () => navigate("/ai-services"),
      },
    );
  };

  return (
    <div>
      <PageTitle>Novo Serviço de IA</PageTitle>
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
        placeholder="Fase"
        value={form.phase}
        onChange={(e) => setForm({ ...form, phase: e.target.value })}
      />
      <input
        className="form-control mb-2"
        placeholder="Preço"
        value={form.price}
        onChange={(e) => setForm({ ...form, price: e.target.value })}
      />
      <input
        className="form-control mb-2"
        placeholder="Custo"
        value={form.cost}
        onChange={(e) => setForm({ ...form, cost: e.target.value })}
      />
      <textarea
        className="form-control mb-2"
        placeholder="Observação"
        value={form.observation}
        onChange={(e) => setForm({ ...form, observation: e.target.value })}
        rows={3}
      />
      <button
        className="btn btn-primary"
        onClick={submit}
        disabled={create.isPending}
      >
        {create.isPending ? (
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
