import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import axios from "axios";

type ResumeView = {
  id: number;
  status: string;
  totalLimit: number;
  dailyBudget?: number | null;
  startDate?: string | null;
  endDate: string;
  zeroResultSpendLimit?: number | null;
  zeroPurchaseSpendLimit?: number | null;
  purchaseStopCount?: number | null;
  reason: string;
  error?: string | null;
};
type ResumeSummary = {
  applicable: boolean;
  available: boolean;
  blocker?: string | null;
  synchronizedSpend: number;
  dailyBudget: number | null;
  currentLimit: number | null;
  zeroResultStopSpend: number;
  zeroPurchaseStopSpend?: number | null;
  purchaseStopCount?: number | null;
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
  const [dailyBudget, setDailyBudget] = useState("");
  const [limit, setLimit] = useState("");
  const [startDate, setStartDate] = useState("");
  const [endDate, setEndDate] = useState("");
  const [zeroPurchaseSpendLimit, setZeroPurchaseSpendLimit] = useState("");
  const [purchaseStopCount, setPurchaseStopCount] = useState("");
  const [reason, setReason] = useState("");
  const [authorized, setAuthorized] = useState(false);
  const [stopRulesConfirmed, setStopRulesConfirmed] = useState(false);
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
          dailyBudget: Number(dailyBudget),
          startDate,
          endDate,
          zeroResultSpendLimit: Number(zeroPurchaseSpendLimit),
          zeroPurchaseSpendLimit: Number(zeroPurchaseSpendLimit),
          purchaseStopCount: purchaseStopCount
            ? Number(purchaseStopCount)
            : null,
          reason: reason.trim(),
          authorizeSpending: authorized,
          useTotalLimitForZeroResults: false,
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
          Orçamento diário atual: <strong>{money(data.dailyBudget)}</strong> ·
          Teto atual: <strong>{money(data.currentLimit)}</strong>
        </p>
        {data.latest && (
          <div className="alert alert-light border" role="status">
            Pedido #{data.latest.id}: <strong>{data.latest.status}</strong> ·
            {data.latest.dailyBudget != null
              ? ` ${money(data.latest.dailyBudget)}/dia ·`
              : ""}{" "}
            Teto {money(data.latest.totalLimit)} ·{" "}
            {data.latest.startDate
              ? `De ${data.latest.startDate} até ${data.latest.endDate}`
              : `Até ${data.latest.endDate}`}
            {data.latest.zeroPurchaseSpendLimit != null
              ? ` · Parar sem compra em ${money(data.latest.zeroPurchaseSpendLimit)}`
              : ""}
            {data.latest.purchaseStopCount
              ? ` · Parar em ${data.latest.purchaseStopCount} compras`
              : ""}
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
                <label htmlFor="resume-daily-budget" className="form-label">
                  Orçamento diário (R$) *
                </label>
                <input
                  id="resume-daily-budget"
                  className="form-control"
                  type="number"
                  min="0.01"
                  step="0.01"
                  value={dailyBudget}
                  onChange={(e) => setDailyBudget(e.target.value)}
                  required
                  disabled={command.isPending}
                />
              </div>
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
                <label htmlFor="resume-start" className="form-label">
                  Data inicial da retomada *
                </label>
                <input
                  id="resume-start"
                  className="form-control"
                  type="date"
                  value={startDate}
                  onChange={(e) => setStartDate(e.target.value)}
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
              <div className="col-md-4">
                <label htmlFor="resume-zero-result" className="form-label">
                  Parar sem compra em (R$) *
                </label>
                <input
                  id="resume-zero-result"
                  className="form-control"
                  type="number"
                  min="0.01"
                  step="0.01"
                  value={zeroPurchaseSpendLimit}
                  onChange={(e) => setZeroPurchaseSpendLimit(e.target.value)}
                  required
                  disabled={command.isPending}
                />
              </div>
              <div className="col-md-4">
                <label htmlFor="resume-purchase-goal" className="form-label">
                  Parar ao atingir compras
                </label>
                <input
                  id="resume-purchase-goal"
                  className="form-control"
                  type="number"
                  min="1"
                  step="1"
                  value={purchaseStopCount}
                  onChange={(e) => setPurchaseStopCount(e.target.value)}
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
                checked={stopRulesConfirmed}
                onChange={(e) => setStopRulesConfirmed(e.target.checked)}
                required
                disabled={command.isPending}
              />
              <label htmlFor="resume-exception" className="form-check-label">
                Confirmo os limites de parada sem compra e por quantidade de
                compras. As demais proteções continuam válidas. *
              </label>
            </div>
            <button
              type="submit"
              className="btn btn-success mt-3"
              disabled={command.isPending || !authorized || !stopRulesConfirmed}
            >
              {command.isPending && (
                <span
                  className="spinner-border spinner-border-sm me-2"
                  aria-hidden="true"
                />
              )}
              {command.isPending
                ? "Registrando autorização..."
                : "Autorizar retomada"}
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
