import { useState } from "react";
import { useSearchParams } from "react-router-dom";
import { useCreateExperiment } from "../../api/experiment/useCreateExperiment";
import { useNiches } from "../../api/niche/useNiches";
import { useHypothesesByNiche } from "../../api/hypothesis/useHypothesesByNiche";
import { useMetricPresets } from "../../api/experiment/useMetricPresets";
import PageTitle from "../../components/PageTitle";

export default function NewExperimentPage() {
  const [params] = useSearchParams();
  const nicheIdParam = params.get("nicheId") ?? "";
  const hypothesisIdParam = params.get("hypothesisId") ?? "";
  const create = useCreateExperiment();
  const { data: niches } = useNiches();
  const [form, setForm] = useState({
    nicheId: nicheIdParam,
    name: "",
    hypothesisId: hypothesisIdParam,
    hypothesis: "",
    kpiTarget: "",
    metricPresetId: "",
    sampleSize: "",
    mde: "",
    startDate: "",
    endDate: "",
  });
  const { data: hypotheses } = useHypothesesByNiche(form.nicheId);
  const { data: presets } = useMetricPresets();
  const showNicheSelect = nicheIdParam === "";
  const showHypSelect = hypothesisIdParam === "";

  const submit = async () => {
    try {
      await create.mutateAsync({
        nicheId: Number(form.nicheId),
        hypothesisId: form.hypothesisId || undefined,
        name: form.name,
        hypothesis: form.hypothesis,
        kpiTarget: Number(form.kpiTarget),
        metricPresetId: form.metricPresetId,
        sampleSize: form.sampleSize ? Number(form.sampleSize) : undefined,
        mde: form.mde ? Number(form.mde) : undefined,
        startDate: form.startDate || undefined,
        endDate: form.endDate || undefined,
      });
      setForm({
        nicheId: nicheIdParam,
        hypothesisId: hypothesisIdParam,
        name: "",
        hypothesis: "",
        kpiTarget: "",
        metricPresetId: "",
        sampleSize: "",
        mde: "",
        startDate: "",
        endDate: "",
      });
      alert("Teste salvo!");
    } catch {
      alert("Erro ao salvar Teste");
    }
  };

  return (
    <div>
      <PageTitle>Novo Teste de Nicho</PageTitle>
      {showNicheSelect && (
        <select
          className="form-select mb-2"
          value={form.nicheId}
          onChange={(e) =>
            setForm({ ...form, nicheId: e.target.value, hypothesisId: "" })
          }
        >
          <option value="">Selecione o Nicho</option>
          {Array.isArray(niches) &&
            niches.map((n) => (
              <option key={n.id} value={n.id}>
                {n.name}
              </option>
            ))}
        </select>
      )}
      {showHypSelect && (
        <>
          <select
            className="form-select mb-2"
            value={form.hypothesisId}
            onChange={(e) => setForm({ ...form, hypothesisId: e.target.value })}
          >
            <option value="">Selecione Hipótese</option>
            {Array.isArray(hypotheses) && hypotheses.length > 0 ? (
              hypotheses.map((h) => (
                <option key={h.id} value={h.id}>
                  {h.title}
                </option>
              ))
            ) : (
              <option value="">Não há hipóteses para este nicho</option>
            )}
          </select>
          {Array.isArray(hypotheses) && hypotheses.length === 0 && (
            <button
              type="button"
              className="btn btn-link mb-2"
              onClick={() => (window.location.href = "/hypotheses?open=new")}
            >
              Criar nova hipótese
            </button>
          )}
        </>
      )}
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
      <select
        className="form-select mb-2"
        value={form.metricPresetId}
        onChange={(e) => setForm({ ...form, metricPresetId: e.target.value })}
      >
        <option value="">Selecione Preset de Métricas</option>
        {Array.isArray(presets) &&
          presets.map((p) => (
            <option key={p.id} value={p.id}>
              {p.name}
            </option>
          ))}
      </select>
      <input
        className="form-control mb-2"
        placeholder="Tamanho da amostra"
        type="number"
        value={form.sampleSize}
        onChange={(e) => setForm({ ...form, sampleSize: e.target.value })}
      />
      <input
        className="form-control mb-2"
        placeholder="MDE %"
        type="number"
        value={form.mde}
        onChange={(e) => setForm({ ...form, mde: e.target.value })}
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
