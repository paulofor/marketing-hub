import { CircleDollarSign, ExternalLink, RefreshCw } from "lucide-react";
import PageTitle from "../../components/PageTitle";
import { useVideoProviderCreditBalances } from "../../api/financial/useVideoProviderCreditBalances";

const statusLabel = {
  AVAILABLE: "Disponível",
  LOW: "Saldo baixo",
  INSUFFICIENT: "Insuficiente",
  DIVERGENT_PROVIDER_REJECTION: "Divergente: provedor recusou",
  NO_PURCHASE_RECORDED: "Sem recarga registrada",
  UNKNOWN_CONSUMPTION: "Consumo incompleto",
} as const;

const statusClass = {
  AVAILABLE: "text-bg-success",
  LOW: "text-bg-warning",
  INSUFFICIENT: "text-bg-danger",
  DIVERGENT_PROVIDER_REJECTION: "text-bg-danger",
  NO_PURCHASE_RECORDED: "text-bg-secondary",
  UNKNOWN_CONSUMPTION: "text-bg-warning",
} as const;

function credits(value: number | null) {
  return value === null ? "Não determinável" : value.toLocaleString("pt-BR");
}

function instant(value: string | null) {
  return value ? new Date(value).toLocaleString("pt-BR") : "Sem registro";
}

function usd(value: number) {
  return value.toLocaleString("pt-BR", {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  });
}

/** Exibe o monitor financeiro comum a todos os produtos que usam provedores de vídeo. */
export default function VideoProviderFinancePage() {
  const balances = useVideoProviderCreditBalances();

  return (
    <div>
      <PageTitle subtitle="Saldo estimado, capacidade e divergências dos provedores de vídeo — independente de produto ou campanha.">
        Financeiro de provedores de vídeo
      </PageTitle>

      <div className="alert alert-info d-flex gap-2 align-items-start">
        <CircleDollarSign size={20} aria-hidden="true" />
        <span>
          O saldo é estimado pelas recargas registradas menos o consumo do
          ledger. Uma recusa do provedor prevalece sobre a estimativa. Esta tela
          não compra créditos nem ativa recarga automática.
        </span>
      </div>

      <div className="d-flex justify-content-end mb-3">
        <button
          type="button"
          className="btn btn-outline-primary"
          disabled={balances.isFetching}
          onClick={() => balances.refetch()}
        >
          {balances.isFetching ? (
            <span
              className="spinner-border spinner-border-sm me-2"
              aria-hidden="true"
            />
          ) : (
            <RefreshCw size={16} className="me-2" aria-hidden="true" />
          )}
          Atualizar monitor
        </button>
      </div>

      {balances.isError ? (
        <div className="alert alert-danger">
          Não foi possível carregar o monitor financeiro.
        </div>
      ) : null}
      {balances.isLoading ? <p>Carregando saldos...</p> : null}

      <div className="row g-3">
        {balances.data?.map((balance) => (
          <div className="col-12 col-xl-6" key={balance.provider}>
            <section
              className="card h-100"
              aria-label={`Saldo ${balance.provider}`}
            >
              <div className="card-body">
                <div className="d-flex justify-content-between align-items-start gap-3 mb-3">
                  <div>
                    <h2 className="h5 mb-1">{balance.provider}</h2>
                    <small className="text-muted">
                      Estimativa financeira auditável
                    </small>
                  </div>
                  <span className={`badge ${statusClass[balance.status]}`}>
                    {statusLabel[balance.status]}
                  </span>
                </div>

                <dl className="row mb-3">
                  <dt className="col-7">Cenas aceitas pelo provedor</dt>
                  <dd className="col-5 text-end fw-semibold">
                    {balance.acceptedSceneRequests.toLocaleString("pt-BR")}
                  </dd>
                  <dt className="col-7">Créditos comprados</dt>
                  <dd className="col-5 text-end">
                    {credits(balance.purchasedCredits)}
                  </dd>
                  <dt className="col-7">Consumo estimado</dt>
                  <dd className="col-5 text-end">
                    {credits(balance.estimatedConsumedCredits)}
                  </dd>
                  <dt className="col-7">Saldo estimado</dt>
                  <dd className="col-5 text-end fw-semibold">
                    {credits(balance.estimatedAvailableCredits)}
                  </dd>
                  <dt className="col-7">Última recarga</dt>
                  <dd className="col-5 text-end">
                    {instant(balance.lastPurchaseAt)}
                  </dd>
                </dl>

                {balance.sceneRequests.length > 0 ? (
                  <div className="table-responsive mb-3">
                    <table className="table table-sm align-middle">
                      <caption>Tasks de cena registradas</caption>
                      <thead>
                        <tr>
                          <th scope="col">Ciclo</th>
                          <th scope="col">Job</th>
                          <th scope="col">Cena</th>
                          <th scope="col">Task do provedor</th>
                          <th scope="col">Modelo/duração</th>
                          <th scope="col">Conciliação financeira</th>
                          <th scope="col">Aceita em</th>
                        </tr>
                      </thead>
                      <tbody>
                        {balance.sceneRequests.map((request) => (
                          <tr key={`${request.jobId}-${request.sceneNumber}`}>
                            <td>{request.productionCycleId ?? "—"}</td>
                            <td>#{request.jobId}</td>
                            <td>
                              {request.sceneNumber}/{request.plannedSceneCount}
                            </td>
                            <td>{request.providerTaskId ?? "Legado sem ID"}</td>
                            <td>
                              {request.model ?? "—"}
                              {request.durationSeconds
                                ? ` · ${request.durationSeconds}s`
                                : ""}
                            </td>
                            <td>
                              {request.settlementStatus === "CHARGED"
                                ? `${request.billedCredits} créditos confirmados · US$ ${usd(request.billedCostUsd ?? 0)}`
                                : request.settlementStatus === "REFUNDED"
                                  ? "Reembolso confirmado · 0 crédito"
                                  : request.settlementStatus ===
                                      "CONTRACTUAL_CHARGE"
                                    ? `${request.billedCredits} créditos pelo contrato · US$ ${usd(request.billedCostUsd ?? 0)}`
                                    : request.settlementStatus ===
                                        "CONTRACTUAL_REFUND"
                                      ? "Reembolso previsto pelo contrato · 0 crédito"
                                      : `${request.estimatedCredits ?? "?"} estimados · pendente`}
                              {request.settlementBasis ===
                              "CONTRACTUAL_RATE_CARD" ? (
                                <small className="d-block text-warning-emphasis">
                                  Não confirmado pelo extrato do provedor
                                </small>
                              ) : null}
                              {request.billingEvidence ? (
                                <small className="d-block text-muted">
                                  {request.billingEvidence}
                                </small>
                              ) : null}
                            </td>
                            <td>{instant(request.acceptedAt)}</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                ) : null}

                {balance.referenceModel ? (
                  <div className="alert alert-light border">
                    Capacidade de referência:{" "}
                    <strong>
                      {credits(balance.estimatedReferenceClips)} clipes
                    </strong>
                    {` de ${balance.referenceClipSeconds}s em ${balance.referenceModel} (${balance.referenceClipCredits} créditos por clipe).`}
                  </div>
                ) : null}

                {balance.lastCreditFailureAt ? (
                  <div className="alert alert-danger mb-3">
                    Última recusa de crédito: job #
                    {balance.lastCreditFailureJobId} em{" "}
                    {instant(balance.lastCreditFailureAt)}.
                    {balance.lastCreditFailureDetail
                      ? ` ${balance.lastCreditFailureDetail}`
                      : ""}
                  </div>
                ) : null}

                {balance.unknownCostAttempts > 0 ? (
                  <div className="alert alert-warning mb-3">
                    {balance.unknownCostAttempts} tentativa(s) sem custo
                    conhecido impedem afirmar o saldo.
                  </div>
                ) : null}

                {balance.creditsUrl ? (
                  <a
                    href={balance.creditsUrl}
                    target="_blank"
                    rel="noopener noreferrer"
                  >
                    Conferir no portal do provedor{" "}
                    <ExternalLink size={14} aria-hidden="true" />
                  </a>
                ) : null}
              </div>
            </section>
          </div>
        ))}
      </div>
    </div>
  );
}
