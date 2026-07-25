import { useState } from "react";
import { Link, useParams } from "react-router-dom";
import {
  useProductPdeProductionSlots,
  useSaveProductPdeProductionSlot,
} from "../../api/product/usePdeProductionSlots";
import { useProduct } from "../../api/product/useProduct";
import type { PdeProductionSlotStatus } from "../../api/experiment/usePostDeployMonitor";
import PageTitle from "../../components/PageTitle";

const statusLabels: Record<PdeProductionSlotStatus, string> = {
  PLANNED: "Planejado",
  READY: "Pronto",
  ACTIVE: "Ativo",
  PAUSED: "Pausado",
  RETIRED: "Encerrado",
};

function hasExplicitTimeZone(value: string) {
  return /(?:z|[+-]\d{2}:?\d{2})$/i.test(value.trim());
}

function formatDate(value?: string | null) {
  if (!value) return "—";
  const date = new Date(
    hasExplicitTimeZone(value) ? value : `${value.replace(" ", "T")}-03:00`,
  );
  if (Number.isNaN(date.getTime())) return "—";
  return date.toLocaleString("pt-BR", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
    timeZone: "America/Sao_Paulo",
  });
}

export default function ProductPdeVersionsPage() {
  const { productId } = useParams();
  const productQuery = useProduct(productId);
  const slotsQuery = useProductPdeProductionSlots(productId);
  const saveSlot = useSaveProductPdeProductionSlot(productId);
  const product = productQuery.data;
  const slots = slotsQuery.data ?? [];
  const [form, setForm] = useState({
    slotCode: "v2",
    domain: "v2.clubemusa.com.br",
    experienceVersion: "musa-pde-entry-v5-estrada-desejo",
    status: "PLANNED" as PdeProductionSlotStatus,
    notes: "",
  });

  if (productQuery.isLoading || slotsQuery.isLoading) {
    return <p className="text-muted">Carregando versões PDE...</p>;
  }

  if (!product) {
    return <div className="alert alert-danger">Produto não encontrado.</div>;
  }

  return (
    <div>
      <div className="d-flex flex-wrap align-items-start justify-content-between gap-3 mb-4">
        <div>
          <PageTitle>Versões PDE do produto</PageTitle>
          <p className="text-muted mb-0">
            {product.name || product.slug} · fonte de verdade para URLs e
            versões produtivas que os experimentos podem medir.
          </p>
        </div>
        <Link className="btn btn-outline-secondary" to="/products">
          Voltar para produtos
        </Link>
      </div>

      <div className="card mb-3">
        <div className="card-body">
          <h2 className="h6 mb-3">Cadastrar versão produtiva</h2>
          <form
            className="row g-2 align-items-end"
            onSubmit={(event) => {
              event.preventDefault();
              saveSlot.mutate({
                productSlug: product.slug || "",
                slotCode: form.slotCode,
                domain: form.domain,
                experienceVersion: form.experienceVersion,
                status: form.status,
                notes: form.notes,
              });
            }}
          >
            <div className="col-12 col-md-2">
              <label className="form-label small fw-semibold" htmlFor="pde-slot-code">
                Slot *
              </label>
              <input
                id="pde-slot-code"
                className="form-control form-control-sm"
                value={form.slotCode}
                onChange={(event) =>
                  setForm((current) => ({ ...current, slotCode: event.target.value }))
                }
                required
              />
            </div>
            <div className="col-12 col-md-3">
              <label className="form-label small fw-semibold" htmlFor="pde-slot-domain">
                Domínio *
              </label>
              <input
                id="pde-slot-domain"
                className="form-control form-control-sm"
                value={form.domain}
                onChange={(event) =>
                  setForm((current) => ({ ...current, domain: event.target.value }))
                }
                required
              />
            </div>
            <div className="col-12 col-md-3">
              <label className="form-label small fw-semibold" htmlFor="pde-slot-version">
                Versão PDE *
              </label>
              <input
                id="pde-slot-version"
                className="form-control form-control-sm"
                value={form.experienceVersion}
                onChange={(event) =>
                  setForm((current) => ({
                    ...current,
                    experienceVersion: event.target.value,
                  }))
                }
                required
              />
            </div>
            <div className="col-12 col-md-2">
              <label className="form-label small fw-semibold" htmlFor="pde-slot-status">
                Status
              </label>
              <select
                id="pde-slot-status"
                className="form-select form-select-sm"
                value={form.status}
                onChange={(event) =>
                  setForm((current) => ({
                    ...current,
                    status: event.target.value as PdeProductionSlotStatus,
                  }))
                }
              >
                {Object.entries(statusLabels).map(([value, label]) => (
                  <option key={value} value={value}>
                    {label}
                  </option>
                ))}
              </select>
            </div>
            <div className="col-12 col-md-2">
              <button
                type="submit"
                className="btn btn-primary btn-sm w-100"
                disabled={saveSlot.isPending}
              >
                {saveSlot.isPending ? "Salvando..." : "Salvar versão"}
              </button>
            </div>
            <div className="col-12">
              <label className="form-label small fw-semibold" htmlFor="pde-slot-notes">
                Observação
              </label>
              <input
                id="pde-slot-notes"
                className="form-control form-control-sm"
                value={form.notes}
                onChange={(event) =>
                  setForm((current) => ({ ...current, notes: event.target.value }))
                }
              />
            </div>
          </form>
        </div>
      </div>

      <div className="card">
        <div className="card-body">
          <h2 className="h6 mb-3">Versões cadastradas</h2>
          <div className="table-responsive">
            <table className="table table-sm align-middle mb-0">
              <thead>
                <tr>
                  <th>Slot</th>
                  <th>Status</th>
                  <th>Versão PDE</th>
                  <th>URL pública</th>
                  <th>Ambiente alvo</th>
                  <th>Experimento origem</th>
                  <th className="text-end">Atualizado</th>
                </tr>
              </thead>
              <tbody>
                {slots.length === 0 ? (
                  <tr>
                    <td colSpan={7} className="text-muted">
                      Nenhuma versão PDE cadastrada para este produto.
                    </td>
                  </tr>
                ) : (
                  slots.map((slot) => (
                    <tr key={slot.id}>
                      <td className="fw-semibold">{slot.slotCode}</td>
                      <td>{statusLabels[slot.status] ?? slot.status}</td>
                      <td className="font-monospace small">{slot.experienceVersion}</td>
                      <td>
                        <a href={slot.publicUrl} target="_blank" rel="noreferrer">
                          {slot.publicUrl}
                        </a>
                        <div className="small text-muted">{slot.domain}</div>
                      </td>
                      <td>{slot.targetEnvironment}</td>
                      <td>{slot.sourceExperimentId ?? "—"}</td>
                      <td className="text-end">{formatDate(slot.updatedAt)}</td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  );
}
