import { useMemo, useState } from "react";
import { toast } from "react-toastify";
import { useDeliverablePackagesByHypothesis } from "../api/deliverable/useDeliverablePackagesByHypothesis";
import { useCreateHypothesisDeliverablePackage } from "../api/deliverable/useCreateHypothesisDeliverablePackage";
import { useDeliverablesByNiche } from "../api/deliverable/useDeliverablesByNiche";
import type { DeliverablePackage } from "../api/deliverable/types";
import type { CreateDeliverablePackagePayload } from "../api/deliverable/useCreateDeliverablePackage";

interface Props {
  hypothesisId?: string;
  nicheId?: number | string;
  value?: number | null;
  onChange?: (id: number | null) => void;
  readOnly?: boolean;
}

const EMPTY_PAYLOAD: CreateDeliverablePackagePayload = {
  name: "",
  description: "",
  model: "",
  prompt: "",
  deliverableIds: [],
};

export function HypothesisOfferPackageSelector({
  hypothesisId,
  nicheId,
  value,
  onChange,
  readOnly,
}: Props) {
  const { data: packages, isLoading } = useDeliverablePackagesByHypothesis(hypothesisId);
  const { data: deliverables } = useDeliverablesByNiche(nicheId ? Number(nicheId) : undefined);
  const createPackage = useCreateHypothesisDeliverablePackage(hypothesisId ?? "");
  const [showModal, setShowModal] = useState(false);
  const [form, setForm] = useState<CreateDeliverablePackagePayload>(EMPTY_PAYLOAD);

  const canEdit = Boolean(hypothesisId) && !readOnly;

  const selected = useMemo(
    () => (packages ?? []).find((pack) => pack.id === value),
    [packages, value],
  );

  const handleSubmit = async () => {
    if (!hypothesisId) return;
    if (!form.name.trim()) {
      toast.warn("Informe o nome do pacote");
      return;
    }
    if (!form.prompt.trim()) {
      toast.warn("Informe o prompt utilizado");
      return;
    }
    try {
      const created = await createPackage.mutateAsync({
        ...form,
        deliverableIds: form.deliverableIds,
      });
      toast.success("Pacote criado");
      setShowModal(false);
      setForm(EMPTY_PAYLOAD);
      onChange?.(created.id);
    } catch (error) {
      console.error(error);
      toast.error("Erro ao criar pacote");
    }
  };

  return (
    <div className="mt-3">
      <label className="form-label">Pacote oficial da oferta</label>
      <div className="d-flex gap-2 align-items-center">
        <select
          className="form-select"
          value={value ?? ""}
          disabled={isLoading || (packages?.length ?? 0) === 0}
          onChange={(event) =>
            onChange?.(event.target.value ? Number(event.target.value) : null)
          }
        >
          <option value="">Selecione um pacote</option>
          {(packages ?? []).map((pack) => (
            <option key={pack.id} value={pack.id}>
              {pack.name}
            </option>
          ))}
        </select>
        <button
          type="button"
          className="btn btn-outline-primary btn-sm"
          disabled={!canEdit}
          onClick={() => setShowModal(true)}
        >
          Novo pacote
        </button>
      </div>
      {selected && (
        <div className="card mt-3">
          <div className="card-body">
            <h5 className="card-title h6">{selected.name}</h5>
            {selected.description && <p className="mb-2">{selected.description}</p>}
            {selected.deliverables?.length ? (
              <ul className="mb-2 small">
                {selected.deliverables.map((deliverable) => (
                  <li key={deliverable.id}>{deliverable.title}</li>
                ))}
              </ul>
            ) : (
              <p className="text-muted small mb-0">Nenhum deliverable vinculado.</p>
            )}
            {selected.prompt && (
              <div className="small text-muted">Prompt: {selected.prompt}</div>
            )}
          </div>
        </div>
      )}

      {showModal && (
        <div className="modal fade show d-block" role="dialog">
          <div className="modal-dialog modal-lg" role="document">
            <div className="modal-content">
              <div className="modal-header">
                <h5 className="modal-title">Novo pacote de oferta</h5>
                <button
                  type="button"
                  className="btn-close"
                  aria-label="Fechar"
                  onClick={() => {
                    setShowModal(false);
                    setForm(EMPTY_PAYLOAD);
                  }}
                />
              </div>
              <div className="modal-body">
                <div className="row g-3">
                  <div className="col-md-6">
                    <label className="form-label">Nome</label>
                    <input
                      className="form-control"
                      value={form.name}
                      onChange={(event) =>
                        setForm((prev) => ({ ...prev, name: event.target.value }))
                      }
                    />
                  </div>
                  <div className="col-md-6">
                    <label className="form-label">Modelo</label>
                    <input
                      className="form-control"
                      value={form.model}
                      onChange={(event) =>
                        setForm((prev) => ({ ...prev, model: event.target.value }))
                      }
                    />
                  </div>
                  <div className="col-12">
                    <label className="form-label">Descrição</label>
                    <textarea
                      className="form-control"
                      rows={2}
                      value={form.description}
                      onChange={(event) =>
                        setForm((prev) => ({ ...prev, description: event.target.value }))
                      }
                    />
                  </div>
                  <div className="col-12">
                    <label className="form-label">Prompt utilizado</label>
                    <textarea
                      className="form-control"
                      rows={3}
                      value={form.prompt}
                      onChange={(event) =>
                        setForm((prev) => ({ ...prev, prompt: event.target.value }))
                      }
                    />
                  </div>
                  <div className="col-12">
                    <label className="form-label">Deliverables do nicho</label>
                    <div className="border rounded p-2" style={{ maxHeight: 240, overflowY: "auto" }}>
                      {(deliverables ?? []).length === 0 ? (
                        <p className="text-muted small mb-0">
                          Cadastre deliverables para este nicho antes de montar o pacote.
                        </p>
                      ) : (
                        <ul className="list-unstyled mb-0">
                          {(deliverables ?? []).map((deliverable) => {
                            const checked = form.deliverableIds.includes(deliverable.id);
                            return (
                              <li key={deliverable.id} className="form-check">
                                <input
                                  className="form-check-input"
                                  type="checkbox"
                                  id={`deliverable-${deliverable.id}`}
                                  checked={checked}
                                  onChange={(event) => {
                                    setForm((prev) => ({
                                      ...prev,
                                      deliverableIds: event.target.checked
                                        ? [...prev.deliverableIds, deliverable.id]
                                        : prev.deliverableIds.filter((id) => id !== deliverable.id),
                                    }));
                                  }}
                                />
                                <label className="form-check-label" htmlFor={`deliverable-${deliverable.id}`}>
                                  {deliverable.title}
                                </label>
                              </li>
                            );
                          })}
                        </ul>
                      )}
                    </div>
                  </div>
                </div>
              </div>
              <div className="modal-footer">
                <button
                  type="button"
                  className="btn btn-link"
                  onClick={() => {
                    setShowModal(false);
                    setForm(EMPTY_PAYLOAD);
                  }}
                >
                  Cancelar
                </button>
                <button
                  type="button"
                  className="btn btn-primary"
                  disabled={createPackage.isPending}
                  onClick={handleSubmit}
                >
                  {createPackage.isPending ? "Salvando..." : "Salvar"}
                </button>
              </div>
            </div>
          </div>
          <div className="modal-backdrop fade show" />
        </div>
      )}
    </div>
  );
}
