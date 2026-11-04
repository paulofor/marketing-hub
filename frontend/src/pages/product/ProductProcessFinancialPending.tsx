import { Link, useLocation } from "react-router-dom";
import type { ProcessFinancialPending } from "../../api/businessProcess/useProcessAutomation";

/** Apresenta a decisão financeira fornecida pelo servidor, junto da atividade e do parecer. */
export default function ProductProcessFinancialPending({
  pending,
  unavailable = false,
}: {
  pending: ProcessFinancialPending;
  unavailable?: boolean;
}) {
  const location = useLocation();
  const returnSearch = new URLSearchParams(location.search);
  const financialSearch = new URLSearchParams(
    pending.actionUrl?.split("?")[1]?.split("#")[0],
  );
  const cycleId = financialSearch.get("cycleId");
  const chainId = financialSearch.get("chainId");
  const explicitWithoutCycle =
    returnSearch.get("sourceReference") && !returnSearch.get("learningCycleId");
  if (cycleId && !explicitWithoutCycle)
    returnSearch.set("learningCycleId", cycleId);
  if (chainId) returnSearch.set("chainId", chainId);
  return (
    <section
      className="alert alert-warning mt-3 text-break"
      aria-label="Pendência financeira da atividade"
    >
      <strong>{pending.title}</strong>
      <p className="mt-2 mb-2">{pending.reason}</p>
      <p className="small mb-2">Responsável: {pending.responsible}</p>
      {pending.productVersion && (
        <p className="small mb-2">Versão: {pending.productVersion}</p>
      )}
      {unavailable ? (
        <p role="status" className="mb-0">
          Conferindo a situação financeira. A retomada permanece indisponível
          até a confirmação.
        </p>
      ) : pending.actionUrl ? (
        <Link
          className="btn btn-primary text-wrap"
          to={pending.actionUrl}
          state={{
            returnTo: `${location.pathname}?${returnSearch.toString()}#activity-${pending.activityId}`,
            productVersion: pending.productVersion,
          }}
        >
          {pending.actionLabel}
        </Link>
      ) : null}
      <p className="small mt-2 mb-0">{pending.afterAction}</p>
    </section>
  );
}
