import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useCreateMicroservice } from "../../api/microservice/useCreateMicroservice";
import PageTitle from "../../components/PageTitle";
import { MicroservicePayload } from "../../api/microservice/useCreateMicroservice";

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

  const submit = () => {
    create.mutate(form, { onSuccess: () => navigate("/microservices") });
  };

  return (
    <div>
      <PageTitle>Novo microserviço</PageTitle>
      <div className="row g-3">
        <div className="col-md-6">
          <label className="form-label">Nome</label>
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
          <label className="form-label">Base URL</label>
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
          disabled={create.isPending || !form.name}
        >
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
