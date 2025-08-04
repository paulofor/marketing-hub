import { useEffect } from "react";
import { useForm } from "react-hook-form";
import { useNavigate, useParams } from "react-router-dom";
import { useExperiment } from "../../api/experiment/useExperiment";
import { useUpdateExperiment } from "../../api/experiment/useUpdateExperiment";
import { useMetricPresets } from "../../api/experiment/useMetricPresets";
import PageTitle from "../../components/PageTitle";

interface FormData {
  name: string;
  hypothesis: string;
  kpiTarget: string;
  metricPresetId: string;
  sampleSize: string;
  mde: string;
  startDate: string;
  endDate: string;
}

export default function EditExperimentPage() {
  const { id } = useParams<{ id: string }>();
  const expId = id as string;
  const navigate = useNavigate();
  const { data, isLoading } = useExperiment(expId);
  const { data: presets } = useMetricPresets();
  const update = useUpdateExperiment(expId);
  const { register, handleSubmit, reset } = useForm<FormData>();

  useEffect(() => {
    if (data) {
      reset({
        name: data.name || "",
        hypothesis: data.hypothesis || "",
        kpiTarget: data.kpiTarget ? String(data.kpiTarget) : "",
        metricPresetId: data.metricPresetId || "",
        sampleSize: data.sampleSize ? String(data.sampleSize) : "",
        mde: data.mdePercent ? String(data.mdePercent) : "",
        startDate: data.startDate || "",
        endDate: data.endDate || "",
      });
    }
  }, [data, reset]);

  const onSubmit = async (values: FormData) => {
    try {
      await update.mutateAsync({
        name: values.name,
        hypothesis: values.hypothesis,
        kpiTarget: Number(values.kpiTarget),
        metricPresetId: values.metricPresetId || undefined,
        sampleSize: values.sampleSize ? Number(values.sampleSize) : undefined,
        mde: values.mde ? Number(values.mde) : undefined,
        startDate: values.startDate || undefined,
        endDate: values.endDate || undefined,
      });
      navigate(-1);
    } catch {
      alert("Erro ao salvar Teste");
    }
  };

  if (isLoading || !data) return <p>Carregando...</p>;

  return (
    <div>
      <PageTitle>Editar Teste</PageTitle>
      <label className="form-label">Nome</label>
      <input className="form-control mb-2" {...register("name")} />
      <label className="form-label">Hipótese</label>
      <input className="form-control mb-2" {...register("hypothesis")} />
      <label className="form-label">Meta do KPI</label>
      <input
        className="form-control mb-2"
        type="number"
        {...register("kpiTarget")}
      />
      <label className="form-label">Preset de Métricas</label>
      <select className="form-select mb-2" {...register("metricPresetId")}>
        <option value="">Selecione Preset de Métricas</option>
        {Array.isArray(presets) &&
          presets.map((p) => (
            <option key={p.id} value={p.id}>
              {p.name}
            </option>
          ))}
      </select>
      <label className="form-label">Tamanho da amostra</label>
      <input
        className="form-control mb-2"
        type="number"
        {...register("sampleSize")}
      />
      <label className="form-label">MDE %</label>
      <input className="form-control mb-2" type="number" {...register("mde")} />
      <label className="form-label">Data de Início</label>
      <input
        className="form-control mb-2"
        type="date"
        {...register("startDate")}
      />
      <label className="form-label">Data de Término</label>
      <input
        className="form-control mb-2"
        type="date"
        {...register("endDate")}
      />
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
  );
}
