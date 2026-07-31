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

function parsePositiveNumberOrNull(value: string) {
  if (value === "") {
    return null;
  }
  const parsed = Number(value);
  return Number.isFinite(parsed) && parsed >= 0 ? parsed : null;
}

export default function EditVpsHostInventoryPage() {
  const { host: encodedHost } = useParams<{ host: string }>();
  const host = encodedHost ? decodeURIComponent(encodedHost) : "";
  const navigate = useNavigate();
  const { data, isLoading, isError } = useVpsHostInventory(host);
  const update = useUpdateVpsHostInventory(host);
  const [form, setForm] = useState<VpsHostInventoryPayload>(emptyForm);
  const [formError, setFormError] = useState<string | null>(null);

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
    if (
      (form.memoryGb !== null &&
        form.memoryGb !== undefined &&
        form.memoryGb < 0) ||
      (form.diskGb !== null && form.diskGb !== undefined && form.diskGb < 0) ||
      (form.monthlyCostBrl !== null &&
        form.monthlyCostBrl !== undefined &&
        form.monthlyCostBrl < 0)
    ) {
      setFormError("Memória, disco e custo mensal não podem ser negativos.");
      return;
    }
    setFormError(null);
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

      {formError ? (
        <div className="alert alert-warning">{formError}</div>
      ) : null}

      <div className="row g-3">
        <div className="col-12">
          <h2 className="h5 mb-0">Identificação</h2>
        </div>
        <div className="col-md-4">
          <label className="form-label" htmlFor="vps-host">
            Host
          </label>
          <input id="vps-host" className="form-control" value={host} disabled />
        </div>
        <div className="col-md-4">
          <label className="form-label" htmlFor="vps-provider-name">
            Provedor
          </label>
          <input
            id="vps-provider-name"
            className="form-control"
            value={form.providerName ?? ""}
            onChange={(event) =>
              setForm({ ...form, providerName: event.target.value })
            }
          />
        </div>
        <div className="col-md-4">
          <label className="form-label" htmlFor="vps-billing-cycle">
            Ciclo de cobrança
          </label>
          <input
            id="vps-billing-cycle"
            className="form-control"
            value={form.billingCycle ?? ""}
            onChange={(event) =>
              setForm({ ...form, billingCycle: event.target.value })
            }
          />
        </div>
        <div className="col-12 mt-4">
          <h2 className="h5 mb-0">Características físicas</h2>
        </div>
        <div className="col-md-4">
          <label className="form-label" htmlFor="vps-cpu">
            CPU
          </label>
          <input
            id="vps-cpu"
            className="form-control"
            placeholder="Ex.: 2 vCPU"
            value={form.cpu ?? ""}
            onChange={(event) => setForm({ ...form, cpu: event.target.value })}
          />
        </div>
        <div className="col-md-4">
          <label className="form-label" htmlFor="vps-memory-gb">
            Memória GB
          </label>
          <input
            id="vps-memory-gb"
            className="form-control"
            type="number"
            min={0}
            step={1}
            value={form.memoryGb ?? ""}
            onChange={(event) =>
              setForm({
                ...form,
                memoryGb: parsePositiveNumberOrNull(event.target.value),
              })
            }
          />
        </div>
        <div className="col-md-4">
          <label className="form-label" htmlFor="vps-disk-gb">
            Disco GB
          </label>
          <input
            id="vps-disk-gb"
            className="form-control"
            type="number"
            min={0}
            step={1}
            value={form.diskGb ?? ""}
            onChange={(event) =>
              setForm({
                ...form,
                diskGb: parsePositiveNumberOrNull(event.target.value),
              })
            }
          />
        </div>
        <div className="col-md-6">
          <label className="form-label" htmlFor="vps-operating-system">
            Sistema operacional
          </label>
          <input
            id="vps-operating-system"
            className="form-control"
            value={form.operatingSystem ?? ""}
            onChange={(event) =>
              setForm({ ...form, operatingSystem: event.target.value })
            }
          />
        </div>
        <div className="col-md-6">
          <label className="form-label" htmlFor="vps-monthly-cost">
            Custo mensal BRL
          </label>
          <input
            id="vps-monthly-cost"
            className="form-control"
            type="number"
            min={0}
            step="0.01"
            value={form.monthlyCostBrl ?? ""}
            onChange={(event) =>
              setForm({
                ...form,
                monthlyCostBrl: parsePositiveNumberOrNull(event.target.value),
              })
            }
          />
        </div>
        <div className="col-12">
          <label className="form-label" htmlFor="vps-provider-evidence">
            Evidência do provedor
          </label>
          <textarea
            id="vps-provider-evidence"
            className="form-control"
            rows={2}
            value={form.providerEvidence ?? ""}
            onChange={(event) =>
              setForm({ ...form, providerEvidence: event.target.value })
            }
          />
        </div>
        <div className="col-12">
          <label className="form-label" htmlFor="vps-physical-specs-evidence">
            Evidência de capacidade física
          </label>
          <textarea
            id="vps-physical-specs-evidence"
            className="form-control"
            rows={2}
            value={form.physicalSpecsEvidence ?? ""}
            onChange={(event) =>
              setForm({ ...form, physicalSpecsEvidence: event.target.value })
            }
          />
        </div>
        <div className="col-12">
          <label className="form-label" htmlFor="vps-cost-evidence">
            Evidência de custo
          </label>
          <textarea
            id="vps-cost-evidence"
            className="form-control"
            rows={2}
            value={form.costEvidence ?? ""}
            onChange={(event) =>
              setForm({ ...form, costEvidence: event.target.value })
            }
          />
        </div>
        <div className="col-12">
          <label className="form-label" htmlFor="vps-notes">
            Observações
          </label>
          <textarea
            id="vps-notes"
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
