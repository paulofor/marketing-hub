import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useCreateMicroservice } from "../../api/microservice/useCreateMicroservice";
import PageTitle from "../../components/PageTitle";
import { MicroservicePayload } from "../../api/microservice/useCreateMicroservice";
import { useDiscoveredMicroservices } from "../../api/microservice/useDiscoveredMicroservices";

const defaultForm: MicroservicePayload = {
  name: "",
  description: "",
  baseUrl: "",
  category: "",
  status: "ACTIVE",
  owner: "",
  documentationUrl: "",
  healthCheckPath: "",
};

export default function NewMicroservicePage() {
  const [form, setForm] = useState<MicroservicePayload>(defaultForm);
  const navigate = useNavigate();
  const create = useCreateMicroservice();
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
      name: suggestion.serviceName,
      baseUrl: suggestion.baseUrl ?? current.baseUrl,
      healthCheckPath:
        suggestion.healthCheckPath || current.healthCheckPath || "",
    }));
  };

  return (
    <div>
      <PageTitle>Novo microserviço</PageTitle>
      <div className="card mb-4">
        <div className="card-body">
          <div className="d-flex justify-content-between align-items-center mb-2">
            <div>
              <div className="fw-semibold">Preencher usando docker-compose</div>
              <div className="text-body-secondary small">
                Selecione um serviço para sugerir o nome, Base URL e caminho de
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
              isLoadingDiscovery || isFetchingDiscovery || !discoveredServices.length
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
                {service.serviceName} — {service.baseUrl}
              </option>
            ))}
          </select>
          {!isLoadingDiscovery && !isFetchingDiscovery &&
            discoveredServices.length === 0 && (
              <div className="text-body-secondary small mt-2">
                Nenhum serviço encontrado no docker-compose configurado.
              </div>
            )}
        </div>
      </div>
      <div className="row g-3">
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
        <div className="col-md-3">
          <label className="form-label">Categoria</label>
          <input
            className="form-control"
            value={form.category}
            onChange={(e) => setForm({ ...form, category: e.target.value })}
            placeholder="ex: infraestrutura, IA"
          />
        </div>
        <div className="col-md-3">
          <label className="form-label">Status</label>
          <select
            className="form-select"
            value={form.status}
            onChange={(e) => setForm({ ...form, status: e.target.value })}
          >
            <option value="ACTIVE">Ativo</option>
            <option value="INACTIVE">Inativo</option>
            <option value="DEPRECATED">Descontinuado</option>
          </select>
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
            value={form.healthCheckPath}
            onChange={(e) =>
              setForm({ ...form, healthCheckPath: e.target.value })
            }
            placeholder="/health | /status"
          />
        </div>
        <div className="col-md-6">
          <label className="form-label">Responsável</label>
          <input
            className="form-control"
            value={form.owner}
            onChange={(e) => setForm({ ...form, owner: e.target.value })}
            placeholder="Squad ou pessoa de contato"
          />
        </div>
        <div className="col-md-6">
          <label className="form-label">URL da documentação</label>
          <input
            className="form-control"
            value={form.documentationUrl}
            onChange={(e) =>
              setForm({ ...form, documentationUrl: e.target.value })
            }
            placeholder="https://docs.seuservico.com"
          />
        </div>
        <div className="col-12">
          <label className="form-label">Descrição</label>
          <textarea
            className="form-control"
            rows={4}
            value={form.description}
            onChange={(e) => setForm({ ...form, description: e.target.value })}
            placeholder="Resumo das responsabilidades, SLAs e integrações"
          />
        </div>
      </div>
      <div className="mt-4 d-flex gap-2">
        <button
          className="btn btn-primary"
          onClick={submit}
          disabled={create.isPending || !form.name || !form.baseUrl}
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
