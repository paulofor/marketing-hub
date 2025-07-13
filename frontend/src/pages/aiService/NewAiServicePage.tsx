import { useState } from "react";
import { useCreateAiService } from "../../api/aiService/useCreateAiService";
import PageTitle from "../../components/PageTitle";

export default function NewAiServicePage() {
  const create = useCreateAiService();
  const [form, setForm] = useState({
    name: "",
    objective: "",
    price: "",
    cost: "",
  });

  const submit = () => {
    create.mutate({
      ...form,
      price: Number(form.price),
      cost: Number(form.cost),
    });
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
      <button className="btn btn-primary" onClick={submit}>
        Salvar
      </button>
    </div>
  );
}
