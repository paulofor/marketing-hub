import { useState } from "react";
import { useMutation, useQuery } from "@tanstack/react-query";
import axios from "axios";

interface StageRecovery {
  idJob: string | null;
  stageCode: string | null;
  status: string | null;
  available: boolean;
  reason: string;
}

export default function SalesPageStageRecovery({
  experimentId,
}: {
  experimentId: number | string;
}) {
  const [confirming, setConfirming] = useState<string | null>(null);
  const base = `/api/experiments/${experimentId}/gerasalespage/v1/stage-recovery`;
  const recovery = useQuery({
    queryKey: ["sales-page-stage-recovery", experimentId],
    queryFn: async () => (await axios.get<StageRecovery>(base)).data,
    refetchInterval: 15000,
  });
  const retry = useMutation({
    mutationFn: async (failedJobId: string) =>
      (await axios.post<StageRecovery>(base, { failedJobId })).data,
    onSuccess: () => {
      setConfirming(null);
      void recovery.refetch();
    },
  });
  const error = axios.isAxiosError(retry.error)
    ? retry.error.response?.data?.detail || retry.error.response?.data?.message
    : null;
  return (
    <div
      className="border rounded-3 p-3 my-3"
      aria-label="Retomar geração da página"
    >
      <h6>Continuidade da geração</h6>
      {recovery.isLoading && <p role="status">Conferindo a última etapa...</p>}
      {recovery.isError && (
        <p role="alert">
          Não foi possível conferir a geração. Atualize a tela.
        </p>
      )}
      {recovery.data && <p className="small mb-2">{recovery.data.reason}</p>}
      {retry.data && (
        <p role="status" className="alert alert-success">
          {retry.data.reason}
        </p>
      )}
      {retry.isError && (
        <p role="alert" className="alert alert-danger">
          {error ||
            "Não foi possível retomar. O histórico foi preservado; atualize a tela e confira o impedimento."}
        </p>
      )}
      {confirming ? (
        <div>
          <p>
            Confirmar uma nova chamada de IA somente para a etapa interrompida?
            Ela pode gerar custo. As etapas concluídas serão preservadas.
          </p>
          <div className="d-flex flex-wrap gap-2">
            <button
              type="button"
              className="btn btn-primary btn-sm"
              disabled={
                retry.isPending ||
                !recovery.data?.available ||
                !recovery.data?.idJob
              }
              onClick={() => confirming && retry.mutate(confirming)}
            >
              {retry.isPending ? (
                <>
                  <span className="spinner-border spinner-border-sm me-2" />
                  Solicitando...
                </>
              ) : (
                "Confirmar retomada da etapa"
              )}
            </button>
            <button
              type="button"
              className="btn btn-outline-secondary btn-sm"
              disabled={retry.isPending}
              onClick={() => setConfirming(null)}
            >
              Cancelar
            </button>
          </div>
        </div>
      ) : (
        <button
          type="button"
          className="btn btn-outline-primary btn-sm"
          disabled={!recovery.data?.available || retry.isPending}
          onClick={() => {
            retry.reset();
            setConfirming(recovery.data?.idJob ?? null);
          }}
        >
          Retomar etapa interrompida
        </button>
      )}
    </div>
  );
}
