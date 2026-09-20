import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import axios from "axios";

type ResumeView = {
  id: number;
  status: string;
  totalLimit: number;
  endDate: string;
  reason: string;
  error?: string | null;
};
type ResumeSummary = {
  applicable: boolean;
  available: boolean;
  blocker?: string | null;
  synchronizedSpend: number;
  currentLimit: number | null;
  zeroResultStopSpend: number;
  latest?: ResumeView | null;
};
const money = (v: number | null) =>
  v == null
    ? "Não informado"
    : v.toLocaleString("pt-BR", { style: "currency", currency: "BRL" });

/** Apresenta autorização financeira e confirmação da Meta sem inferir o estado da campanha. */
export default function ExperimentCampaignResumptionPanel({
  experimentId,
}: {
  experimentId: string;
}) {
  const queryClient = useQueryClient();
  const [limit, setLimit] = useState("");
  const [endDate, setEndDate] = useState("");
  const [reason, setReason] = useState("");
  const [authorized, setAuthorized] = useState(false);
  const [exception, setException] = useState(false);
  const [message, setMessage] = useState("");
  const endpoint = `/api/facebook-campaign-resumptions/experiments/${experimentId}`;
  const { data } = useQuery<ResumeSummary>({
    queryKey: ["campaign-resumption", experimentId],
    queryFn: async () => (await axios.get<ResumeSummary>(endpoint)).data,
    refetchInterval: (q) =>
      ["PENDING", "RUNNING"].includes(q.state.data?.latest?.status ?? "")
        ? 3000
        : false,
  });
  const command = useMutation({
    mutationFn: async () =>
      (
        await axios.post(endpoint, {
          totalLimit: Number(limit),
          endDate,
          reason: reason.trim(),
          authorizeSpending: authorized,
          useTotalLimitForZeroResults: exception,
        })
      ).data,
    onSuccess: () => {
      setMessage(
        "Autorização registrada. Aguarde a confirmação da Meta antes de considerar a campanha ativa.",
      );
      queryClient.invalidateQueries({
        queryKey: ["campaign-resumption", experimentId],
      });
      queryClient.invalidateQueries({ queryKey: ["experiment", experimentId] });
      queryClient.invalidateQueries({ queryKey: ["experiments"] });
    },
    onError: (error) =>
      setMessage(
        axios.isAxiosError(error)
          ? (error.response?.data?.message ??
              "Não foi possível autorizar a retomada. Confira teto, prazo e requisitos.")
          : "Não foi possível autorizar a retomada.",
      ),
  });
  if (!data?.applicable) return null;
  return (
    <section className="card mt-3" aria-label="Retomada financeira">
      <div className="card-body">
        <h5>Retomar a campanha com limite financeiro</h5>
        <p>
          O teto é acumulado e inclui todo o gasto anterior. A retomada preserva
          a campanha, visitantes e resultados.
        </p>
        <p>
          Gasto sincronizado: <strong>{money(data.synchronizedSpend)}</strong> ·
          Teto atual: <strong>{money(data.currentLimit)}</strong>
        </p>
        {data.latest && (
          <div className="alert alert-light border" role="status">
            Pedido #{data.latest.id}: <strong>{data.latest.status}</strong> ·
            Teto {money(data.latest.totalLimit)} · Até {data.latest.endDate}
            <p className="mb-0">{data.latest.reason}</p>
            {data.latest.error && (
              <p className="text-danger mb-0">{data.latest.error}</p>
            )}
            {data.latest.status === "COMPLETED" && (
              <p className="mb-0">
                A Meta confirmou orçamento, prazo e campanha ativa. Recarregue
                os resultados para acompanhar a coleta.
              </p>
            )}
          </div>
        )}
        {!data.available && <p className="text-muted">{data.blocker}</p>}
        {data.available && (
          <form
            onSubmit={(event) => {
              event.preventDefault();
              setMessage("");
              command.mutate();
            }}
          >
            <div className="row g-3">
              <div className="col-md-4">
                <label htmlFor="resume-cap" className="form-label">
                  Teto acumulado de mídia (R$) *
                </label>
                <input
                  id="resume-cap"
                  className="form-control"
                  type="number"
                  min="0.01"
                  step="0.01"
                  value={limit}
                  onChange={(e) => setLimit(e.target.value)}
                  required
                  disabled={command.isPending}
                />
              </div>
              <div className="col-md-4">
                <label htmlFor="resume-end" className="form-label">
                  Data final da retomada *
                </label>
                <input
                  id="resume-end"
                  className="form-control"
                  type="date"
                  value={endDate}
                  onChange={(e) => setEndDate(e.target.value)}
                  required
                  disabled={command.isPending}
                />
              </div>
            </div>
            <label htmlFor="resume-reason" className="form-label mt-3">
              Motivo da retomada *
            </label>
            <textarea
              id="resume-reason"
              className="form-control"
              minLength={10}
              maxLength={800}
              value={reason}
              onChange={(e) => setReason(e.target.value)}
              required
              disabled={command.isPending}
            />
            <div className="form-check mt-3">
              <input
                id="resume-authorize"
                className="form-check-input"
                type="checkbox"
                checked={authorized}
                onChange={(e) => setAuthorized(e.target.checked)}
                required
                disabled={command.isPending}
              />
              <label htmlFor="resume-authorize" className="form-check-label">
                Autorizo gasto até o teto acumulado informado, incluindo o valor
                já consumido. *
              </label>
            </div>
            <div className="form-check mt-2">
              <input
                id="resume-exception"
                className="form-check-input"
                type="checkbox"
                checked={exception}
                onChange={(e) => setException(e.target.checked)}
                required
                disabled={command.isPending}
              />
              <label htmlFor="resume-exception" className="form-check-label">
                Autorizo este experimento a coletar dados até esse teto mesmo
                sem novos resultados primários. As demais proteções continuam
                válidas. *
              </label>
            </div>
            <button
              type="submit"
              className="btn btn-success mt-3"
              disabled={command.isPending || !authorized || !exception}
            >
              {command.isPending && (
                <span
                  className="spinner-border spinner-border-sm me-2"
                  aria-hidden="true"
                />
              )}
              {command.isPending
                ? "Registrando autorização..."
                : "Autorizar e retomar na Meta"}
            </button>
          </form>
        )}
        {message && (
          <p role="status" className="mt-3 mb-0">
            {message}
          </p>
        )}
      </div>
    </section>
  );
}
