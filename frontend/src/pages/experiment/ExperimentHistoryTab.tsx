import { FormEvent, useState } from "react";
import { toast } from "react-toastify";
import {
  type ExperimentHistoryCategory,
  useCreateExperimentHistoryEvent,
  useExperimentHistoryEvents,
} from "../../api/experiment/useExperimentHistoryEvents";

const labels: Record<ExperimentHistoryCategory, string> = {
  OBSERVACAO: "Observação",
  INCIDENTE: "Incidente",
  DECISAO: "Decisão",
  CORRECAO: "Correção",
  APRENDIZADO: "Aprendizado",
};

export default function ExperimentHistoryTab({
  experimentId,
}: {
  experimentId: string;
}) {
  const history = useExperimentHistoryEvents(experimentId);
  const createEvent = useCreateExperimentHistoryEvent(experimentId);
  const [category, setCategory] =
    useState<ExperimentHistoryCategory>("OBSERVACAO");
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [evidenceJson, setEvidenceJson] = useState("");

  async function submit(event: FormEvent) {
    event.preventDefault();
    try {
      await createEvent.mutateAsync({
        category,
        title,
        description,
        evidenceJson: evidenceJson || null,
      });
      setTitle("");
      setDescription("");
      setEvidenceJson("");
      toast.success("Ocorrência registrada no histórico.");
    } catch {
      toast.error(
        "Não foi possível registrar a ocorrência. Verifique os campos.",
      );
    }
  }

  return (
    <div className="d-flex flex-column gap-3 py-3">
      <div className="card">
        <div className="card-body">
          <h5 className="card-title">Registrar ocorrência</h5>
          <p className="text-muted small">
            Preserve fatos, evidências e decisões sem sobrescrever os
            aprendizados anteriores.
          </p>
          <form className="row g-3" onSubmit={submit}>
            <div className="col-md-4">
              <label className="form-label" htmlFor="history-category">
                Categoria
              </label>
              <select
                id="history-category"
                className="form-select"
                value={category}
                onChange={(e) =>
                  setCategory(e.target.value as ExperimentHistoryCategory)
                }
              >
                {Object.entries(labels).map(([value, label]) => (
                  <option key={value} value={value}>
                    {label}
                  </option>
                ))}
              </select>
            </div>
            <div className="col-md-8">
              <label className="form-label" htmlFor="history-title">
                Título
              </label>
              <input
                id="history-title"
                className="form-control"
                maxLength={191}
                required
                value={title}
                onChange={(e) => setTitle(e.target.value)}
              />
            </div>
            <div className="col-12">
              <label className="form-label" htmlFor="history-description">
                Descrição factual
              </label>
              <textarea
                id="history-description"
                className="form-control"
                rows={4}
                required
                value={description}
                onChange={(e) => setDescription(e.target.value)}
              />
            </div>
            <div className="col-12">
              <label className="form-label" htmlFor="history-evidence">
                Evidências (JSON opcional)
              </label>
              <textarea
                id="history-evidence"
                className="form-control font-monospace"
                rows={3}
                placeholder='{"impressions":182,"clicks":0,"spend":8.35}'
                value={evidenceJson}
                onChange={(e) => setEvidenceJson(e.target.value)}
              />
            </div>
            <div className="col-12">
              <button
                className="btn btn-primary"
                disabled={createEvent.isPending}
              >
                Registrar no experimento
              </button>
            </div>
          </form>
        </div>
      </div>
      <div className="card">
        <div className="card-body">
          <h5 className="card-title">Linha do tempo</h5>
          {history.isLoading ? <p>Carregando histórico…</p> : null}
          {!history.isLoading && !history.data?.length ? (
            <p className="text-muted mb-0">Nenhuma ocorrência registrada.</p>
          ) : null}
          <div className="d-flex flex-column gap-3">
            {history.data?.map((item) => (
              <article className="border-start border-3 ps-3" key={item.id}>
                <div className="d-flex flex-wrap gap-2 align-items-center">
                  <span className="badge text-bg-secondary">
                    {labels[item.category]}
                  </span>
                  <strong>{item.title}</strong>
                  <small className="text-muted">
                    {new Date(item.occurredAt).toLocaleString("pt-BR")}
                  </small>
                </div>
                <p className="mb-1 mt-2" style={{ whiteSpace: "pre-wrap" }}>
                  {item.description}
                </p>
                {item.evidenceJson ? (
                  <pre className="small bg-light border rounded p-2 mb-0 overflow-auto">
                    {JSON.stringify(JSON.parse(item.evidenceJson), null, 2)}
                  </pre>
                ) : null}
              </article>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}
