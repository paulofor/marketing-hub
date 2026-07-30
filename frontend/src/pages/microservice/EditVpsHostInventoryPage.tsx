import { useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import {
  useUpdateVpsHostInventory,
  useVpsHostInventory,
  type VpsHostInventoryPayload,
} from "../../api/microservice/useOperationalInventory";
import PageTitle from "../../components/PageTitle";

const emptyForm: VpsHostInventoryPayload = {
  providerName: "",
  providerEvidence: "",
  cpu: "",
  memoryGb: null,
  diskGb: null,
  operatingSystem: "",
  monthlyCostBrl: null,
  billingCycle: "",
  costEvidence: "",
  physicalSpecsEvidence: "",
  notes: "",
};

function toNumberOrNull(value: string) {
  return value === "" ? null : Number(value);
}

export default function EditVpsHostInventoryPage() {
  const { host: encodedHost } = useParams<{ host: string }>();
  const host = encodedHost ? decodeURIComponent(encodedHost) : "";
  const navigate = useNavigate();
  const { data, isLoading, isError } = useVpsHostInventory(host);
  const update = useUpdateVpsHostInventory(host);
  const [form, setForm] = useState<VpsHostInventoryPayload>(emptyForm);

  useEffect(() => {
    if (data) {
      setForm({
        providerName: data.providerName ?? "",
        providerEvidence: data.providerEvidence ?? "",
        cpu: data.cpu ?? "",
        memoryGb: data.memoryGb ?? null,
        diskGb: data.diskGb ?? null,
        operatingSystem: data.operatingSystem ?? "",
        monthlyCostBrl: data.monthlyCostBrl ?? null,
        billingCycle: data.billingCycle ?? "",
        costEvidence: data.costEvidence ?? "",
        physicalSpecsEvidence: data.physicalSpecsEvidence ?? "",
        notes: data.notes ?? "",
      });
    }
  }, [data]);

  const submit = () => {
    update.mutate(form, {
      onSuccess: () => navigate("/microservices/vps-inventory"),
    });
  };

  if (isLoading) {
    return <p>Carregando host VPS...</p>;
  }

  return (
    <div>
      <div className="d-flex justify-content-between align-items-start gap-3 mb-3">
        <div>
          <PageTitle>Editar VPS</PageTitle>
          <p className="text-body-secondary mb-0">
            Atualize capacidade, custo e evidências usadas no planejamento de
            infraestrutura dos produtos digitais.
          </p>
        </div>
        <Link
          className="btn btn-outline-secondary"
          to="/microservices/vps-inventory"
        >
          Voltar
        </Link>
      </div>

      {isError ? (
        <div className="alert alert-danger">
          Não foi possível carregar este host VPS.
        </div>
      ) : null}

      <div className="row g-3">
        <div className="col-md-4">
          <label className="form-label">Host</label>
          <input className="form-control" value={host} disabled />
        </div>
        <div className="col-md-4">
          <label className="form-label">Provedor</label>
          <input
            className="form-control"
            value={form.providerName ?? ""}
            onChange={(event) =>
              setForm({ ...form, providerName: event.target.value })
            }
          />
        </div>
        <div className="col-md-4">
          <label className="form-label">Ciclo de cobrança</label>
          <input
            className="form-control"
            value={form.billingCycle ?? ""}
            onChange={(event) =>
              setForm({ ...form, billingCycle: event.target.value })
            }
          />
        </div>
        <div className="col-md-4">
          <label className="form-label">CPU</label>
          <input
            className="form-control"
            value={form.cpu ?? ""}
            onChange={(event) => setForm({ ...form, cpu: event.target.value })}
          />
        </div>
        <div className="col-md-4">
          <label className="form-label">Memória GB</label>
          <input
            className="form-control"
            type="number"
            min={0}
            value={form.memoryGb ?? ""}
            onChange={(event) =>
              setForm({ ...form, memoryGb: toNumberOrNull(event.target.value) })
            }
          />
        </div>
        <div className="col-md-4">
          <label className="form-label">Disco GB</label>
          <input
            className="form-control"
            type="number"
            min={0}
            value={form.diskGb ?? ""}
            onChange={(event) =>
              setForm({ ...form, diskGb: toNumberOrNull(event.target.value) })
            }
          />
        </div>
        <div className="col-md-6">
          <label className="form-label">Sistema operacional</label>
          <input
            className="form-control"
            value={form.operatingSystem ?? ""}
            onChange={(event) =>
              setForm({ ...form, operatingSystem: event.target.value })
            }
          />
        </div>
        <div className="col-md-6">
          <label className="form-label">Custo mensal BRL</label>
          <input
            className="form-control"
            type="number"
            min={0}
            step="0.01"
            value={form.monthlyCostBrl ?? ""}
            onChange={(event) =>
              setForm({
                ...form,
                monthlyCostBrl: toNumberOrNull(event.target.value),
              })
            }
          />
        </div>
        <div className="col-12">
          <label className="form-label">Evidência do provedor</label>
          <textarea
            className="form-control"
            rows={2}
            value={form.providerEvidence ?? ""}
            onChange={(event) =>
              setForm({ ...form, providerEvidence: event.target.value })
            }
          />
        </div>
        <div className="col-12">
          <label className="form-label">Evidência de capacidade física</label>
          <textarea
            className="form-control"
            rows={2}
            value={form.physicalSpecsEvidence ?? ""}
            onChange={(event) =>
              setForm({ ...form, physicalSpecsEvidence: event.target.value })
            }
          />
        </div>
        <div className="col-12">
          <label className="form-label">Evidência de custo</label>
          <textarea
            className="form-control"
            rows={2}
            value={form.costEvidence ?? ""}
            onChange={(event) =>
              setForm({ ...form, costEvidence: event.target.value })
            }
          />
        </div>
        <div className="col-12">
          <label className="form-label">Observações</label>
          <textarea
            className="form-control"
            rows={3}
            value={form.notes ?? ""}
            onChange={(event) =>
              setForm({ ...form, notes: event.target.value })
            }
          />
        </div>
      </div>

      <div className="mt-4 d-flex gap-2">
        <button
          className="btn btn-primary"
          type="button"
          onClick={submit}
          disabled={update.isPending || !host}
        >
          {update.isPending ? (
            <>
              <span
                className="spinner-border spinner-border-sm me-2"
                role="status"
                aria-hidden="true"
              />
              Salvando...
            </>
          ) : (
            "Salvar"
          )}
        </button>
        <button
          className="btn btn-outline-secondary"
          type="button"
          onClick={() => navigate(-1)}
        >
          Cancelar
        </button>
      </div>
    </div>
  );
}
