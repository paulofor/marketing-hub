import { useState } from "react";
import axios from "axios";
import { useAdoptFacebookSuccessor } from "../../api/experiment/useFacebookSuccessor";

interface Props {
  targetExperimentId: string | number;
}

function errorMessage(error: unknown) {
  if (!axios.isAxiosError(error)) {
    return "Não foi possível vincular a origem auditada.";
  }
  const detail = error.response?.data?.detail;
  return typeof detail === "string" && detail.trim()
    ? detail
    : "Não foi possível vincular a origem auditada.";
}

export default function ExperimentFacebookSuccessorAdoptionPanel({
  targetExperimentId,
}: Props) {
  const adoption = useAdoptFacebookSuccessor(targetExperimentId);
  const [sourceExperimentId, setSourceExperimentId] = useState("");
  const [feedback, setFeedback] = useState<string>();
  const parsedSourceId = Number(sourceExperimentId);
  const validSourceId = Number.isInteger(parsedSourceId) && parsedSourceId > 0;

  const adopt = async () => {
    if (!validSourceId) {
      setFeedback("Informe o ID positivo do experimento de origem.");
      return;
    }
    setFeedback(undefined);
    try {
      await adoption.mutateAsync({ sourceExperimentId: parsedSourceId });
      setFeedback(
        "Origem vinculada. A página e o checkout foram reutilizados sem copiar a execução.",
      );
    } catch (error) {
      setFeedback(errorMessage(error));
    }
  };

  return (
    <div className="border rounded p-3 mt-3">
      <strong>Reutilizar superfície comercial auditada</strong>
      <p className="small text-muted mb-2">
        Informe o experimento anterior do mesmo produto. Somente página,
        checkout e linhagem serão adotados; campanha, métricas, público e
        criativos continuam isolados.
      </p>
      <div className="d-flex flex-wrap gap-2 align-items-end">
        <div>
          <label className="form-label" htmlFor="facebookSuccessorSourceId">
            Experimento de origem
          </label>
          <input
            id="facebookSuccessorSourceId"
            className="form-control"
            type="number"
            min="1"
            step="1"
            value={sourceExperimentId}
            onChange={(event) => setSourceExperimentId(event.target.value)}
            placeholder="Ex.: 88"
          />
        </div>
        <button
          type="button"
          className="btn btn-outline-primary btn-sm mb-1"
          disabled={!validSourceId || adoption.isPending}
          onClick={adopt}
        >
          {adoption.isPending
            ? "Vinculando..."
            : "Reutilizar página e checkout"}
        </button>
      </div>
      {feedback ? (
        <div className="small mt-2" role="status">
          {feedback}
        </div>
      ) : null}
    </div>
  );
}
