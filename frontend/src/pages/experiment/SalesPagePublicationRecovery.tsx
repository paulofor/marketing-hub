import { useState } from "react";
import { useMutation, useQuery } from "@tanstack/react-query";
import axios from "axios";

interface RecoveryView {
  publicationId: number;
  available: boolean;
  reason: string;
  sourceSha256: string | null;
  salesPageUrl: string | null;
  submittedAt: string | null;
}

export default function SalesPagePublicationRecovery({
  experimentId,
  publicationId,
}: {
  experimentId: number | string;
  publicationId: number;
}) {
  const [confirming, setConfirming] = useState(false);
  const base = `/api/experiments/${experimentId}/gerasalespage/v1/publications/${publicationId}`;
  const recovery = useQuery({
    queryKey: ["sales-page-recovery", experimentId, publicationId],
    queryFn: async () =>
      (await axios.get<RecoveryView>(`${base}/recovery`)).data,
  });
  const submit = useMutation({
    mutationFn: async () =>
      (
        await axios.post<RecoveryView>(`${base}/republish`, {
          expectedSourceSha256: recovery.data?.sourceSha256,
        })
      ).data,
    onSuccess: () => setConfirming(false),
  });
  const error = axios.isAxiosError(submit.error)
    ? (submit.error.response?.data?.message ??
      submit.error.response?.data?.detail)
    : null;

  return (
    <div
      className="border rounded-3 p-3 mt-3"
      aria-label="Recuperar publicação"
    >
      <h6>Atualizar a página publicada</h6>
      <p className="small mb-2">
        Reenvia a mesma versão aprovada para recuperar sua publicação, sem
        refazer o conteúdo ou solicitar geração por IA.
      </p>
      {recovery.isError ? (
        <p role="alert">
          Não foi possível conferir a publicação. Recarregue a tela.
        </p>
      ) : (
        <p className="small text-muted">{recovery.data?.reason}</p>
      )}
      {submit.isError && (
        <p role="alert" className="alert alert-danger">
          {error ||
            "Não foi possível reenviar a página. O histórico foi preservado. Tente novamente após corrigir a integração."}
        </p>
      )}
      {submit.data && (
        <p role="status" className="alert alert-success">
          {submit.data.reason}
        </p>
      )}
      {confirming ? (
        <div className="d-grid gap-2">
          <p className="mb-0">
            Confirmar o reenvio desta mesma página? A revisão da experiência
            continuará pelo processo corrente.
          </p>
          <div className="d-flex gap-2 flex-wrap">
            <button
              className="btn btn-primary btn-sm"
              type="button"
              disabled={submit.isPending || !recovery.data?.available}
              onClick={() => submit.mutate()}
            >
              {submit.isPending ? (
                <>
                  <span className="spinner-border spinner-border-sm me-2" />
                  Reenviando...
                </>
              ) : (
                "Confirmar reenvio"
              )}
            </button>
            <button
              className="btn btn-outline-secondary btn-sm"
              type="button"
              disabled={submit.isPending}
              onClick={() => setConfirming(false)}
            >
              Cancelar
            </button>
          </div>
        </div>
      ) : (
        <button
          type="button"
          className="btn btn-outline-primary btn-sm"
          disabled={recovery.isLoading || !recovery.data?.available}
          onClick={() => {
            submit.reset();
            setConfirming(true);
          }}
        >
          {recovery.isLoading ? (
            <>
              <span className="spinner-border spinner-border-sm me-2" />
              Conferindo...
            </>
          ) : (
            "Reenviar página aprovada"
          )}
        </button>
      )}
    </div>
  );
}
