import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { useOpsMonitorModule } from "../../api/opsMonitor/useOpsMonitorModule";
import type { OpsMonitorModule } from "../../api/opsMonitor/useOpsMonitorModules";
import { useUpdateOpsMonitorModule } from "../../api/opsMonitor/useUpdateOpsMonitorModule";
import PageTitle from "../../components/PageTitle";

export default function EditMicroservicePage() {
  const { id } = useParams<{ id: string }>();
  const moduleCode = id ?? "";
  const { data, isLoading } = useOpsMonitorModule(moduleCode);
  const update = useUpdateOpsMonitorModule();
  const navigate = useNavigate();
  const [form, setForm] = useState<OpsMonitorModule>({
    id: 0,
    code: moduleCode,
    name: "",
    type: "SERVICE",
    baseUrl: "",
    healthPath: "/actuator/health",
    logPath: "",
    publishedVersion: "",
    productUrl: "",
    monitoringUrl: "",
    containerImageVersion: "",
    enabled: true,
    criticality: "HIGH",
    offlineThresholdSeconds: 300,
  });

  useEffect(() => {
    if (data) setForm(data);
  }, [data]);

  const submit = () => {
    update.mutate(form, { onSuccess: () => navigate("/microservices") });
  };

  if (isLoading) return <p>Carregando...</p>;

  return (
    <div>
      <PageTitle>Editar módulo monitorado</PageTitle>
      <div className="row g-3">
        <div className="col-md-4">
          <label className="form-label">Código</label>
          <input className="form-control" value={form.code} disabled />
        </div>
        <div className="col-md-4">
          <label className="form-label">Tipo</label>
          <select
            className="form-select"
            value={form.type}
            onChange={(e) => setForm({ ...form, type: e.target.value })}
          >
            <option value="BACKEND">Backend</option>
            <option value="FRONTEND">Frontend</option>
            <option value="WORKER">Worker</option>
            <option value="COLLECTOR">Coletor</option>
            <option value="SERVICE">Serviço</option>
            <option value="PORTAL">Portal</option>
            <option value="PDE">PDE</option>
            <option value="VPS">VPS</option>
          </select>
        </div>
        <div className="col-md-4">
          <label className="form-label">Criticidade</label>
          <select
            className="form-select"
            value={form.criticality}
            onChange={(e) => setForm({ ...form, criticality: e.target.value })}
          >
            <option value="CRITICAL">Crítica</option>
            <option value="HIGH">Alta</option>
            <option value="MEDIUM">Média</option>
            <option value="LOW">Baixa</option>
          </select>
        </div>
        <div className="col-md-6">
          <label className="form-label">Nome</label>
          <input
            className="form-control"
            value={form.name}
            onChange={(e) => setForm({ ...form, name: e.target.value })}
          />
        </div>
        <div className="col-md-6">
          <label className="form-label">Base URL</label>
          <input
            className="form-control"
            value={form.baseUrl}
            onChange={(e) => setForm({ ...form, baseUrl: e.target.value })}
          />
        </div>
        <div className="col-md-6">
          <label className="form-label">Caminho de healthcheck</label>
          <input
            className="form-control"
            value={form.healthPath}
            onChange={(e) => setForm({ ...form, healthPath: e.target.value })}
          />
        </div>
        <div className="col-md-6">
          <label className="form-label">Caminho de log</label>
          <input
            className="form-control"
            value={form.logPath ?? ""}
            onChange={(e) => setForm({ ...form, logPath: e.target.value })}
          />
        </div>
        <div className="col-md-6">
          <label className="form-label">Versão publicada</label>
          <input
            className="form-control"
            value={form.publishedVersion ?? ""}
            onChange={(e) =>
              setForm({ ...form, publishedVersion: e.target.value })
            }
          />
        </div>
        <div className="col-md-6">
          <label className="form-label">Imagem/versão de container</label>
          <input
            className="form-control"
            value={form.containerImageVersion ?? ""}
            onChange={(e) =>
              setForm({ ...form, containerImageVersion: e.target.value })
            }
          />
        </div>
        <div className="col-md-6">
          <label className="form-label">URL do produto</label>
          <input
            className="form-control"
            value={form.productUrl ?? ""}
            onChange={(e) => setForm({ ...form, productUrl: e.target.value })}
          />
        </div>
        <div className="col-md-6">
          <label className="form-label">URL de monitoramento</label>
          <input
            className="form-control"
            value={form.monitoringUrl ?? ""}
            onChange={(e) =>
              setForm({ ...form, monitoringUrl: e.target.value })
            }
          />
        </div>
        <div className="col-md-3">
          <label className="form-label">Limite offline (s)</label>
          <input
            className="form-control"
            type="number"
            min={30}
            value={form.offlineThresholdSeconds}
            onChange={(e) =>
              setForm({
                ...form,
                offlineThresholdSeconds: Number(e.target.value),
              })
            }
          />
        </div>
        <div className="col-md-3 d-flex align-items-end">
          <div className="form-check">
            <input
              className="form-check-input"
              id="enabled"
              type="checkbox"
              checked={form.enabled}
              onChange={(e) => setForm({ ...form, enabled: e.target.checked })}
            />
            <label className="form-check-label" htmlFor="enabled">
              Monitorar ativo
            </label>
          </div>
        </div>
      </div>
      <div className="mt-4 d-flex gap-2">
        <button
          className="btn btn-primary"
          onClick={submit}
          disabled={update.isPending || !form.name || !form.baseUrl}
        >
          {update.isPending ? "Salvando..." : "Salvar"}
        </button>
        <button
          className="btn btn-outline-secondary"
          onClick={() => navigate(-1)}
          type="button"
        >
          Cancelar
        </button>
      </div>
    </div>
  );
}
