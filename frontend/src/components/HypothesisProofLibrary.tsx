import { useEffect, useMemo, useState } from "react";
import { toast } from "react-toastify";
import { useHypothesisProofs } from "../api/proof/useHypothesisProofs";
import { useCreateHypothesisProof } from "../api/proof/useCreateHypothesisProof";
import { useUpdateProof } from "../api/proof/useUpdateProof";
import { useVisualProofs } from "../api/visualProof/useVisualProofs";
import type { ProofArtifact, ProofStage, ProofStatus } from "../api/proof/types";

const STAGE_OPTIONS: { value: ProofStage; label: string }[] = [
  { value: "AD", label: "Anúncio" },
  { value: "LANDING", label: "Landing" },
  { value: "SAMPLE", label: "Amostra" },
  { value: "SALES", label: "Oferta" },
];

const STATUS_OPTIONS: { value: ProofStatus; label: string }[] = [
  { value: "DRAFT", label: "Rascunho" },
  { value: "APPROVED", label: "Aprovada" },
  { value: "ARCHIVED", label: "Arquivada" },
];

interface ProofFormState {
  visualProofId?: number | null;
  customType: string;
  stage: ProofStage;
  status: ProofStatus;
  assetPlan: string;
  assetUrl: string;
  message: string;
  deliveryNotes: string;
  prompt: string;
  model: string;
}

const EMPTY_FORM: ProofFormState = {
  visualProofId: undefined,
  customType: "",
  stage: "SAMPLE",
  status: "DRAFT",
  assetPlan: "",
  assetUrl: "",
  message: "",
  deliveryNotes: "",
  prompt: "",
  model: "",
};

interface Props {
  hypothesisId?: string;
  onApply?: (proof: ProofArtifact) => void;
  readOnly?: boolean;
}

export function HypothesisProofLibrary({ hypothesisId, onApply, readOnly }: Props) {
  const { data: proofs, isLoading } = useHypothesisProofs(hypothesisId);
  const { data: visualProofs } = useVisualProofs();
  const createProof = useCreateHypothesisProof(hypothesisId ?? "");
  const updateProof = useUpdateProof(hypothesisId);
  const [showModal, setShowModal] = useState(false);
  const [form, setForm] = useState<ProofFormState>(EMPTY_FORM);
  const [editingId, setEditingId] = useState<number | null>(null);

  const canEdit = Boolean(hypothesisId) && !readOnly;

  useEffect(() => {
    if (!showModal) {
      setForm(EMPTY_FORM);
      setEditingId(null);
    }
  }, [showModal]);

  const currentProofs = proofs ?? [];

  const visualOptions = useMemo(
    () =>
      (visualProofs ?? []).map((proof) => ({
        value: proof.id,
        label: proof.name,
      })),
    [visualProofs],
  );

  const handleSubmit = async () => {
    if (!hypothesisId) return;
    const payload = {
      visualProofId: form.visualProofId,
      customType: form.customType || undefined,
      stage: form.stage,
      status: form.status,
      assetPlan: form.assetPlan || undefined,
      assetUrl: form.assetUrl || undefined,
      message: form.message || undefined,
      deliveryNotes: form.deliveryNotes || undefined,
      prompt: form.prompt || undefined,
      model: form.model || undefined,
    };
    try {
      if (editingId) {
        await updateProof.mutateAsync({ id: editingId, ...payload });
        toast.success("Prova atualizada");
      } else {
        await createProof.mutateAsync(payload);
        toast.success("Prova cadastrada");
      }
      setShowModal(false);
    } catch (error) {
      console.error(error);
      toast.error("Não foi possível salvar a prova");
    }
  };

  const openForEdit = (proof: ProofArtifact) => {
    setEditingId(proof.id);
    setForm({
      visualProofId: proof.visualProofId ?? undefined,
      customType: proof.customType ?? "",
      stage: proof.stage ?? "SAMPLE",
      status: proof.status ?? "DRAFT",
      assetPlan: proof.assetPlan ?? "",
      assetUrl: proof.assetUrl ?? "",
      message: proof.message ?? "",
      deliveryNotes: proof.deliveryNotes ?? "",
      prompt: proof.prompt ?? "",
      model: proof.model ?? "",
    });
    setShowModal(true);
  };

  return (
    <div className="mt-4">
      <div className="d-flex justify-content-between align-items-center mb-2">
        <div>
          <h4 className="h6 mb-0">Provas catalogadas</h4>
          <small className="text-muted">
            Reaproveite ativos de prova para preencher o framework.
          </small>
        </div>
        <button
          type="button"
          className="btn btn-outline-primary btn-sm"
          disabled={!canEdit}
          onClick={() => setShowModal(true)}
        >
          Nova prova
        </button>
      </div>
      {!hypothesisId && (
        <div className="alert alert-warning">
          Salve a hipótese para começar a cadastrar provas.
        </div>
      )}
      {isLoading ? (
        <p>Carregando provas...</p>
      ) : currentProofs.length === 0 ? (
        <p className="text-muted">Nenhuma prova cadastrada ainda.</p>
      ) : (
        <div className="row g-3">
          {currentProofs.map((proof) => (
            <div className="col-md-6" key={proof.id}>
              <div className="card h-100 shadow-sm">
                <div className="card-body">
                  <div className="d-flex justify-content-between align-items-start mb-2">
                    <div>
                      <span className="badge bg-secondary me-2">
                        {proof.stageLabel || proof.stage}
                      </span>
                      <span className={`badge ${proof.status === "APPROVED" ? "bg-success" : "bg-light text-dark"}`}>
                        {proof.status === "APPROVED" ? "Aprovada" : proof.status === "ARCHIVED" ? "Arquivada" : "Rascunho"}
                      </span>
                    </div>
                    <div className="d-flex gap-2">
                      <button
                        type="button"
                        className="btn btn-sm btn-outline-secondary"
                        disabled={!canEdit}
                        onClick={() => openForEdit(proof)}
                      >
                        Editar
                      </button>
                      {onApply && (
                        <button
                          type="button"
                          className="btn btn-sm btn-primary"
                          onClick={() => onApply(proof)}
                        >
                          Aplicar
                        </button>
                      )}
                    </div>
                  </div>
                  <h5 className="h6 mb-2">{proof.typeLabel || "Tipo não definido"}</h5>
                  {proof.assetPlan && (
                    <p className="mb-1 small text-muted">{proof.assetPlan}</p>
                  )}
                  {proof.message && <p className="mb-0">{proof.message}</p>}
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {showModal && (
        <div className="modal fade show d-block" role="dialog">
          <div className="modal-dialog modal-lg" role="document">
            <div className="modal-content">
              <div className="modal-header">
                <h5 className="modal-title">
                  {editingId ? "Editar prova" : "Nova prova"}
                </h5>
                <button
                  type="button"
                  className="btn-close"
                  onClick={() => setShowModal(false)}
                  aria-label="Fechar"
                />
              </div>
              <div className="modal-body">
                <div className="row g-3">
                  <div className="col-md-4">
                    <label className="form-label">Estágio</label>
                    <select
                      className="form-select"
                      value={form.stage}
                      onChange={(event) =>
                        setForm((prev) => ({ ...prev, stage: event.target.value as ProofStage }))
                      }
                    >
                      {STAGE_OPTIONS.map((option) => (
                        <option key={option.value} value={option.value}>
                          {option.label}
                        </option>
                      ))}
                    </select>
                  </div>
                  <div className="col-md-4">
                    <label className="form-label">Status</label>
                    <select
                      className="form-select"
                      value={form.status}
                      onChange={(event) =>
                        setForm((prev) => ({ ...prev, status: event.target.value as ProofStatus }))
                      }
                    >
                      {STATUS_OPTIONS.map((option) => (
                        <option key={option.value} value={option.value}>
                          {option.label}
                        </option>
                      ))}
                    </select>
                  </div>
                  <div className="col-md-4">
                    <label className="form-label">Prova visual</label>
                    <select
                      className="form-select"
                      value={form.visualProofId ?? ""}
                      onChange={(event) =>
                        setForm((prev) => ({
                          ...prev,
                          visualProofId:
                            event.target.value === "" ? undefined : Number(event.target.value),
                        }))
                      }
                    >
                      <option value="">Livre</option>
                      {visualOptions.map((option) => (
                        <option key={option.value} value={option.value}>
                          {option.label}
                        </option>
                      ))}
                    </select>
                  </div>
                  <div className="col-md-6">
                    <label className="form-label">Tipo personalizado</label>
                    <input
                      className="form-control"
                      value={form.customType}
                      onChange={(event) =>
                        setForm((prev) => ({ ...prev, customType: event.target.value }))
                      }
                    />
                  </div>
                  <div className="col-md-6">
                    <label className="form-label">URL do ativo</label>
                    <input
                      className="form-control"
                      value={form.assetUrl}
                      onChange={(event) =>
                        setForm((prev) => ({ ...prev, assetUrl: event.target.value }))
                      }
                    />
                  </div>
                  <div className="col-12">
                    <label className="form-label">Plano/descrição do ativo</label>
                    <textarea
                      className="form-control"
                      rows={3}
                      value={form.assetPlan}
                      onChange={(event) =>
                        setForm((prev) => ({ ...prev, assetPlan: event.target.value }))
                      }
                    />
                  </div>
                  <div className="col-12">
                    <label className="form-label">Mensagem principal</label>
                    <textarea
                      className="form-control"
                      rows={3}
                      value={form.message}
                      onChange={(event) =>
                        setForm((prev) => ({ ...prev, message: event.target.value }))
                      }
                    />
                  </div>
                  <div className="col-12">
                    <label className="form-label">Notas de entrega</label>
                    <textarea
                      className="form-control"
                      rows={3}
                      value={form.deliveryNotes}
                      onChange={(event) =>
                        setForm((prev) => ({ ...prev, deliveryNotes: event.target.value }))
                      }
                    />
                  </div>
                  <div className="col-md-6">
                    <label className="form-label">Prompt</label>
                    <textarea
                      className="form-control"
                      rows={2}
                      value={form.prompt}
                      onChange={(event) =>
                        setForm((prev) => ({ ...prev, prompt: event.target.value }))
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
                </div>
              </div>
              <div className="modal-footer">
                <button
                  type="button"
                  className="btn btn-link"
                  onClick={() => setShowModal(false)}
                >
                  Cancelar
                </button>
                <button
                  type="button"
                  className="btn btn-primary"
                  onClick={handleSubmit}
                  disabled={createProof.isPending || updateProof.isPending}
                >
                  {createProof.isPending || updateProof.isPending ? "Salvando..." : "Salvar"}
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
