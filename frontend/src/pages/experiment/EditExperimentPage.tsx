import { useEffect } from "react";
import { useForm } from "react-hook-form";
import { useNavigate, useParams } from "react-router-dom";
import { useExperiment } from "../../api/experiment/useExperiment";
import { useUpdateExperiment } from "../../api/experiment/useUpdateExperiment";
import { useMetricPresets } from "../../api/experiment/useMetricPresets";
import { useFunnels } from "../../api/funnel/useFunnels";
import PageTitle from "../../components/PageTitle";

interface FormData {
  name: string;
  kpiTarget: string;
  metricPresetId: string;
  salesFunnelName: string;
}

export default function EditExperimentPage() {
  const { id } = useParams<{ id: string }>();
  const expId = id as string;
  const navigate = useNavigate();
  const { data, isLoading } = useExperiment(expId);
  const { data: presets } = useMetricPresets();
  const { data: funnels } = useFunnels();
  const update = useUpdateExperiment(expId);
  const { register, handleSubmit, reset } = useForm<FormData>();

  useEffect(() => {
    if (data) {
      reset({
        name: data.name || "",
        kpiTarget: data.kpiTarget ? String(data.kpiTarget) : "",
        metricPresetId: data.metricPresetId || "",
        salesFunnelName: data.salesFunnelName || "",
      });
    }
  }, [data, reset]);

  const onSubmit = async (values: FormData) => {
    try {
      if (!data) return;
      await update.mutateAsync({
        name: values.name,
        hypothesis: data.hypothesis,
        kpiTarget: Number(values.kpiTarget),
        metricPresetId: values.metricPresetId || undefined,
        sampleSize: data.sampleSize ?? undefined,
        mde: data.mdePercent ?? undefined,
        startDate: data.startDate ?? undefined,
        endDate: data.endDate ?? undefined,
        salesFunnelName: values.salesFunnelName,
      });
      navigate(-1);
    } catch {
      alert("Erro ao salvar Experimento");
    }
  };

  if (isLoading || !data) return <p>Carregando...</p>;

  return (
    <div style={{ maxWidth: 480 }}>
      <PageTitle>Editar Experimento</PageTitle>
      <form onSubmit={handleSubmit(onSubmit)} noValidate>
        <label className="form-label" htmlFor="name">
          Nome
        </label>
        <input id="name" className="form-control mb-2" {...register("name")} />
        <label className="form-label">Hipótese</label>
        <p className="form-control-plaintext">{data.hypothesis}</p>
        <label className="form-label" htmlFor="kpiTarget">
          Meta do KPI
        </label>
        <input
          id="kpiTarget"
          className="form-control mb-2"
          type="number"
          {...register("kpiTarget")}
        />
        <label className="form-label" htmlFor="preset">
          Preset de Métricas
        </label>
        <select
          id="preset"
          className="form-select mb-2"
          {...register("metricPresetId")}
        >
          <option value="">Selecione Preset de Métricas</option>
          {Array.isArray(presets) &&
            presets.map((p) => (
              <option key={p.id} value={p.id}>
                {p.name}
              </option>
            ))}
        </select>
        <label className="form-label" htmlFor="salesFunnel">
          Funil de Vendas
        </label>
        <select
          id="salesFunnel"
          className="form-select mb-2"
          {...register("salesFunnelName")}
        >
          <option value="">Nenhum</option>
          {Array.isArray(funnels) &&
            funnels.map((f) => (
              <option key={f.id} value={f.name}>
                {f.name}
              </option>
            ))}
        </select>
        <div className="mt-3 d-flex justify-content-end">
          <button
            type="button"
            className="btn btn-outline-secondary me-2"
            onClick={() => navigate(-1)}
            disabled={update.isPending}
          >
            Cancelar
          </button>
          <button
            type="button"
            className="btn btn-primary"
            disabled={update.isPending}
            onClick={handleSubmit(onSubmit, (errors) => {
              console.log("Validation errors", errors);
            })}
          >
            Salvar
          </button>
        </div>
      </form>
    </div>
  );
}
