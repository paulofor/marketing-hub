import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import PageTitle from "../../components/PageTitle";
import { useMicroservice } from "../../api/microservice/useMicroservice";
import { useUpdateMicroservice } from "../../api/microservice/useUpdateMicroservice";
import { Microservice } from "../../api/microservice/useMicroservices";

export default function EditMicroservicePage() {
  const { id } = useParams<{ id: string }>();
  const serviceId = Number(id);
  const { data, isLoading } = useMicroservice(serviceId);
  const update = useUpdateMicroservice();
  const navigate = useNavigate();
  const [form, setForm] = useState<Microservice>({
    id: serviceId,
    name: "",
    description: "",
    baseUrl: "",
    category: "",
    status: "ACTIVE",
    owner: "",
    documentationUrl: "",
    healthCheckPath: "",
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
      <PageTitle>Editar microserviço</PageTitle>
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
          />
        </div>
        <div className="col-md-6">
          <label className="form-label">Responsável</label>
          <input
            className="form-control"
            value={form.owner}
            onChange={(e) => setForm({ ...form, owner: e.target.value })}
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
          />
        </div>
        <div className="col-12">
          <label className="form-label">Descrição</label>
          <textarea
            className="form-control"
            rows={4}
            value={form.description}
            onChange={(e) => setForm({ ...form, description: e.target.value })}
          />
        </div>
      </div>
      <div className="mt-4 d-flex gap-2">
        <button
          className="btn btn-primary"
          onClick={submit}
          disabled={update.isPending || !form.name}
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
