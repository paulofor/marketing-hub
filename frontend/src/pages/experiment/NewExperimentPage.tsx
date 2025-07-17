import { useState } from "react";
import { useCreateExperiment } from "../../api/experiment/useCreateExperiment";
import { useNiches } from "../../api/niche/useNiches";
import PageTitle from "../../components/PageTitle";

export default function NewExperimentPage() {
  const create = useCreateExperiment();
  const { data: niches } = useNiches();
  const [form, setForm] = useState({
    nicheId: "",
    name: "",
    hypothesis: "",
    kpiTarget: "",
    startDate: "",
    endDate: "",
  });

  const submit = async () => {
    try {
      await create.mutateAsync({
        nicheId: Number(form.nicheId),
        name: form.name,
        hypothesis: form.hypothesis,
        kpiTarget: Number(form.kpiTarget),
        startDate: form.startDate || undefined,
        endDate: form.endDate || undefined,
      });
      setForm({ nicheId: "", name: "", hypothesis: "", kpiTarget: "", startDate: "", endDate: "" });
      alert("Teste salvo!");
    } catch {
      alert("Erro ao salvar Teste");
    }
  };

  return (
    <div>
      <PageTitle>Novo Teste de Nicho</PageTitle>
      <select
        className="form-select mb-2"
        value={form.nicheId}
        onChange={(e) => setForm({ ...form, nicheId: e.target.value })}
      >
        <option value="">Selecione o Nicho</option>
        {Array.isArray(niches) &&
          niches.map((n) => (
            <option key={n.id} value={n.id}>
              {n.name}
            </option>
          ))}
      </select>
      <input
        className="form-control mb-2"
        placeholder="Nome"
        value={form.name}
        onChange={(e) => setForm({ ...form, name: e.target.value })}
      />
      <input
        className="form-control mb-2"
        placeholder="Hipótese"
        value={form.hypothesis}
        onChange={(e) => setForm({ ...form, hypothesis: e.target.value })}
      />
      <input
        className="form-control mb-2"
        placeholder="Meta do KPI"
        type="number"
        value={form.kpiTarget}
        onChange={(e) => setForm({ ...form, kpiTarget: e.target.value })}
      />
      <input
        className="form-control mb-2"
        placeholder="Data de Início"
        type="date"
        value={form.startDate}
        onChange={(e) => setForm({ ...form, startDate: e.target.value })}
      />
      <input
        className="form-control mb-2"
        placeholder="Data de Término"
        type="date"
        value={form.endDate}
        onChange={(e) => setForm({ ...form, endDate: e.target.value })}
      />
      <button className="btn btn-primary" onClick={submit}>
        Salvar
      </button>
    </div>
  );
}
