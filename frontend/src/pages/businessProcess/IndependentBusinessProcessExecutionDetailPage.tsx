import { useRef } from "react";
import { AlertCircle, ArrowLeft, RefreshCw } from "lucide-react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { toast } from "react-toastify";
import {
  useIndependentBusinessProcessExecution,
  useStartIndependentBusinessProcessExecution,
} from "../../api/businessProcess/useIndependentBusinessProcessExecutions";
import PageTitle from "../../components/PageTitle";
import {
  createIndependentExecutionRequestKey,
  IndependentBusinessProcessExecutionDetail,
  independentExecutionRequestError,
} from "./IndependentBusinessProcessExecutionsPage";
import "./IndependentBusinessProcessExecutionsPage.css";

function parseExecutionId(value?: string) {
  const parsed = Number(value);
  return Number.isSafeInteger(parsed) && parsed > 0 ? parsed : undefined;
}

export default function IndependentBusinessProcessExecutionDetailPage() {
  const { executionId: executionIdParam } = useParams();
  const executionId = parseExecutionId(executionIdParam);
  const navigate = useNavigate();
  const query = useIndependentBusinessProcessExecution(executionId);
  const retry = useStartIndependentBusinessProcessExecution();
  const retryCommand = useRef({
    executionId,
    requestKey: createIndependentExecutionRequestKey(),
  });
  if (retryCommand.current.executionId !== executionId) {
    retryCommand.current = {
      executionId,
      requestKey: createIndependentExecutionRequestKey(),
    };
  }
  const execution = query.data?.execution;

  const retryExecution = async () => {
    if (!execution) return;
    try {
      const result = await retry.mutateAsync({
        requestKey: retryCommand.current.requestKey,
        processDefinitionId: execution.processDefinitionId,
        requestedByName: execution.requestedByName,
        input: execution.input,
      });
      toast.success("Nova tentativa criada com a mesma entrada.");
      navigate(`/business-process-executions/${result.execution.id}`);
    } catch (error) {
      toast.error(independentExecutionRequestError(error));
    }
  };

  if (executionId === undefined) {
    return (
      <div className="alert alert-danger" role="alert">
        O identificador desta execução é inválido.
      </div>
    );
  }

  return (
    <div className="independent-process-page">
      <header className="independent-process-detail-page__header">
        <Link
          className="btn btn-outline-secondary independent-process-detail-page__back"
          to="/business-process-executions"
        >
          <ArrowLeft size={17} aria-hidden="true" />
          Voltar às execuções
        </Link>
        <div>
          <span className="independent-process-eyebrow">
            Acompanhamento auditável
          </span>
          <PageTitle>Detalhe da execução #{executionId}</PageTitle>
          <p className="mb-0 text-body-secondary">
            Veja o que aconteceu, a causa de qualquer bloqueio e as evidências
            preservadas pelo backend.
          </p>
        </div>
      </header>

      {execution?.status === "BLOCKED" ? (
        <section
          className="independent-process-blocked-guidance"
          aria-labelledby="independent-process-blocked-title"
        >
          <AlertCircle size={28} aria-hidden="true" />
          <div>
            <h2 id="independent-process-blocked-title">
              Esta tentativa não executou
            </h2>
            <p>
              A tarefa foi criada, mas o agente parou antes de produzir o
              resultado. A causa original está registrada abaixo. Depois da
              correção operacional, crie uma nova tentativa sem perder este
              histórico.
            </p>
          </div>
          <button
            className="btn btn-primary"
            type="button"
            disabled={retry.isPending}
            onClick={retryExecution}
          >
            <RefreshCw
              className={retry.isPending ? "independent-process-spin" : ""}
              size={17}
              aria-hidden="true"
            />
            {retry.isPending ? "Criando tentativa..." : "Tentar novamente"}
          </button>
        </section>
      ) : null}

      <IndependentBusinessProcessExecutionDetail
        loading={query.isLoading}
        error={query.isError}
        detail={query.data}
      />
    </div>
  );
}
