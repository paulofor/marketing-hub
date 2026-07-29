import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useDiscoveredMicroservices } from "../../api/microservice/useDiscoveredMicroservices";
import {
  OpsMonitorModulePayload,
  useCreateOpsMonitorModule,
} from "../../api/opsMonitor/useCreateOpsMonitorModule";
import PageTitle from "../../components/PageTitle";

const defaultForm: OpsMonitorModulePayload = {
  code: "",
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
};

export default function NewMicroservicePage() {
  const [form, setForm] = useState<OpsMonitorModulePayload>(defaultForm);
  const navigate = useNavigate();
  const create = useCreateOpsMonitorModule();
  const {
    data: discoveredServices = [],
    isLoading: isLoadingDiscovery,
    isFetching: isFetchingDiscovery,
    refetch: refreshDiscovery,
  } = useDiscoveredMicroservices();

  const submit = () => {
    create.mutate(form, { onSuccess: () => navigate("/microservices") });
  };

  const applyDiscovery = (serviceName: string) => {
    const suggestion = discoveredServices.find(
      (service) => service.serviceName === serviceName,
    );

    if (!suggestion) {
      return;
    }

    setForm((current) => ({
      ...current,
      code: current.code || serviceName,
      name: suggestion.serviceName,
      baseUrl: suggestion.baseUrl ?? current.baseUrl,
      healthPath: suggestion.healthCheckPath || current.healthPath || "",
    }));
  };

  return (
    <div>
      <PageTitle>Novo módulo monitorado</PageTitle>
      <div className="card mb-4">
        <div className="card-body">
          <div className="d-flex justify-content-between align-items-center mb-2">
            <div>
              <div className="fw-semibold">Preencher usando docker-compose</div>
              <div className="text-body-secondary small">
                Selecione um serviço para sugerir o código, nome, Base URL e
                healthcheck a partir das portas publicadas.
              </div>
            </div>
            <button
              className="btn btn-outline-secondary btn-sm"
              type="button"
              onClick={() => refreshDiscovery()}
              disabled={isFetchingDiscovery}
            >
              {isFetchingDiscovery && (
                <span
                  className="spinner-border spinner-border-sm me-2"
                  role="status"
                  aria-hidden="true"
                />
              )}
              Atualizar
            </button>
          </div>
          <select
            className="form-select"
            onChange={(event) => applyDiscovery(event.target.value)}
            disabled={
              isLoadingDiscovery ||
              isFetchingDiscovery ||
              !discoveredServices.length
            }
            defaultValue=""
          >
            <option value="" disabled>
              {isLoadingDiscovery
                ? "Carregando serviços do docker-compose..."
                : "Selecione um serviço"}
            </option>
            {discoveredServices.map((service) => (
              <option key={service.serviceName} value={service.serviceName}>
                {service.serviceName} - {service.baseUrl}
              </option>
            ))}
          </select>
          {!isLoadingDiscovery &&
            !isFetchingDiscovery &&
            discoveredServices.length === 0 && (
              <div className="text-body-secondary small mt-2">
                Nenhum serviço encontrado no docker-compose configurado.
              </div>
            )}
        </div>
      </div>
      <div className="row g-3">
        <div className="col-md-4">
          <label className="form-label">
            Código <span className="text-danger">*</span>
          </label>
          <input
            className="form-control"
            value={form.code}
            onChange={(e) => setForm({ ...form, code: e.target.value })}
            placeholder="ex: ai-worker"
          />
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
          <label className="form-label">
            Nome <span className="text-danger">*</span>
          </label>
          <input
            className="form-control"
            value={form.name}
            onChange={(e) => setForm({ ...form, name: e.target.value })}
          />
        </div>
        <div className="col-md-6">
          <label className="form-label">
            Base URL <span className="text-danger">*</span>
          </label>
          <input
            className="form-control"
            value={form.baseUrl}
            onChange={(e) => setForm({ ...form, baseUrl: e.target.value })}
            placeholder="https://api.seuservico.com"
          />
        </div>
        <div className="col-md-6">
          <label className="form-label">Caminho de healthcheck</label>
          <input
            className="form-control"
            value={form.healthPath}
            onChange={(e) => setForm({ ...form, healthPath: e.target.value })}
            placeholder="/health | /status"
          />
        </div>
        <div className="col-md-6">
          <label className="form-label">Caminho de log</label>
          <input
            className="form-control"
            value={form.logPath ?? ""}
            onChange={(e) => setForm({ ...form, logPath: e.target.value })}
            placeholder="/actuator/logfile"
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
            placeholder="tag, versão ou slot"
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
            placeholder="registry/imagem:tag"
          />
        </div>
        <div className="col-md-6">
          <label className="form-label">URL do produto</label>
          <input
            className="form-control"
            value={form.productUrl ?? ""}
            onChange={(e) => setForm({ ...form, productUrl: e.target.value })}
            placeholder="https://produto.exemplo.com"
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
            placeholder="https://produto.exemplo.com?mh_monitor=1"
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
          disabled={
            create.isPending || !form.code || !form.name || !form.baseUrl
          }
        >
          {create.isPending && (
            <span
              className="spinner-border spinner-border-sm me-2"
              role="status"
              aria-hidden="true"
            />
          )}
          {create.isPending ? "Salvando..." : "Salvar"}
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
