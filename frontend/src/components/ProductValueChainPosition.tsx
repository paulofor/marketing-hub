import { ArrowRight, Workflow } from "lucide-react";
import { Link } from "react-router-dom";
import type { ProductValueChainPosition as Position } from "../api/product/useProductValueChainPositions";

type Props = {
  productName: string;
  position?: Position;
  isLoading?: boolean;
  isError?: boolean;
  compact?: boolean;
};

export default function ProductValueChainPosition({
  productName,
  position,
  isLoading = false,
  isError = false,
  compact = false,
}: Props) {
  const identified =
    position?.resolutionStatus === "IDENTIFIED" &&
    position.processDefinitionId != null &&
    Boolean(position.processName);
  const stateClass = identified
    ? ""
    : " product-value-chain-position--unresolved";
  const compactClass = compact ? " product-value-chain-position--compact" : "";

  return (
    <section
      className={`product-value-chain-position${stateClass}${compactClass}`}
      aria-label={`Posição de ${productName} na cadeia de valor`}
    >
      <div className="product-value-chain-position__heading">
        <span>
          <Workflow size={16} aria-hidden="true" />
          Processo atual da cadeia
        </span>
        {identified &&
        position.sequenceNumber != null &&
        position.processCount != null ? (
          <strong>
            Etapa {position.sequenceNumber} de {position.processCount}
          </strong>
        ) : null}
      </div>

      {isLoading && !position ? (
        <p aria-live="polite">Carregando posição...</p>
      ) : isError && !position ? (
        <p>Posição temporariamente indisponível.</p>
      ) : identified ? (
        <>
          <Link
            className="product-value-chain-position__process"
            to={`/business-processes?processId=${position.processDefinitionId}`}
          >
            <span>{position.processName}</span>
            <ArrowRight size={17} aria-hidden="true" />
          </Link>
          <small>
            {position.chainName}
            {position.chainVersion != null
              ? ` · cadeia v${position.chainVersion}`
              : ""}
          </small>
        </>
      ) : (
        <>
          <p>Processo ainda não identificado</p>
          <small>
            {position?.resolutionMessage ||
              "O produto ainda não possui posição vinculada à cadeia vigente."}
          </small>
        </>
      )}
    </section>
  );
}
