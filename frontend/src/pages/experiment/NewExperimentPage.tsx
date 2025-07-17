import { useState } from "react";
import { useCreateExperiment } from "../../api/experiment/useCreateExperiment";
import PageTitle from "../../components/PageTitle";

export default function NewExperimentPage() {
  const create = useCreateExperiment();
  const [form, setForm] = useState({
    hypothesis: "",
    kpiGoal: "",
    startDate: "",
    endDate: "",
  });

  const submit = async () => {
    try {
      await create.mutateAsync({
        hypothesis: form.hypothesis,
        kpiGoal: Number(form.kpiGoal),
        startDate: form.startDate,
        endDate: form.endDate,
      });
      setForm({ hypothesis: "", kpiGoal: "", startDate: "", endDate: "" });
      alert("Teste salvo!");
    } catch {
      alert("Erro ao salvar Teste");
    }
  };

  return (
    <div>
      <PageTitle>Novo Teste de Nicho</PageTitle>
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
        value={form.kpiGoal}
        onChange={(e) => setForm({ ...form, kpiGoal: e.target.value })}
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
