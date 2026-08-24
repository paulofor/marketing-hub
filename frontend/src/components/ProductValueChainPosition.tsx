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
  const subprocess = position?.subprocessPosition;

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
          {subprocess &&
          subprocess.trackingStatus !== "NOT_APPLICABLE" &&
          subprocess.subprocessCount > 0 ? (
            <div className="product-value-chain-position__subprocesses">
              <span className="product-value-chain-position__subprocesses-title">
                Subprocessos para atingir o objetivo
              </span>
              {subprocess.currentSubprocessDefinitionId != null ? (
                <div className="product-value-chain-position__subprocess product-value-chain-position__subprocess--current">
                  <span>Subprocesso atual</span>
                  <Link
                    to={`/business-processes?processId=${subprocess.currentSubprocessDefinitionId}`}
                  >
                    {subprocess.currentSubprocessName}
                  </Link>
                  {subprocess.currentSubprocessObjective ? (
                    <small>
                      Objetivo: {subprocess.currentSubprocessObjective}
                    </small>
                  ) : null}
                </div>
              ) : subprocess.currentActivityName ? (
                <div className="product-value-chain-position__subprocess product-value-chain-position__subprocess--parent">
                  <span>Agora no processo principal</span>
                  <strong>{subprocess.currentActivityName}</strong>
                </div>
              ) : null}
              {subprocess.nextSubprocessDefinitionId != null ? (
                <div className="product-value-chain-position__subprocess product-value-chain-position__subprocess--next">
                  <span>Próximo subprocesso</span>
                  <Link
                    to={`/business-processes?processId=${subprocess.nextSubprocessDefinitionId}`}
                  >
                    {subprocess.nextSubprocessName}
                    <ArrowRight size={15} aria-hidden="true" />
                  </Link>
                  {subprocess.nextSubprocessObjective ? (
                    <small>
                      Objetivo: {subprocess.nextSubprocessObjective}
                    </small>
                  ) : null}
                </div>
              ) : (
                <small className="product-value-chain-position__subprocess-complete">
                  Não há outro subprocesso previsto; o próximo objetivo está no
                  processo principal.
                </small>
              )}
            </div>
          ) : null}
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
