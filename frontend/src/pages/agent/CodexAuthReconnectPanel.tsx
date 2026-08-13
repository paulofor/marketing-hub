import {
  useCodexAuthReconnect,
  useStartCodexAuthReconnect,
} from "../../api/agent/useCodexAuthReconnect";

interface Props {
  agentId: number;
  nickname: string;
  onClose: () => void;
}

/** Exibe e controla a reconexão sem receber credenciais OAuth. */
export default function CodexAuthReconnectPanel({
  agentId,
  nickname,
  onClose,
}: Props) {
  const reconnect = useCodexAuthReconnect(agentId);
  const start = useStartCodexAuthReconnect();
  const state = reconnect.data;

  return (
    <div
      className="card border-primary mb-4"
      role="region"
      aria-label="Reconexão Codex"
    >
      <div className="card-body">
        <div className="d-flex justify-content-between gap-3">
          <div>
            <h2 className="h5 mb-1">Reconectar Codex de {nickname}</h2>
            <p className="small text-body-secondary">
              O token permanece no executor. O painel recebe somente o link, o
              código temporário e o resultado.
            </p>
          </div>
          <button className="btn-close" aria-label="Fechar" onClick={onClose} />
        </div>

        {!state || ["AUTHENTICATED", "FAILED"].includes(state.status) ? (
          <button
            className="btn btn-primary"
            disabled={start.isPending}
            onClick={() => start.mutate(agentId)}
          >
            {start.isPending ? (
              <span className="spinner-border spinner-border-sm me-2" />
            ) : null}
            Iniciar reconexão
          </button>
        ) : null}

        {state ? (
          <div className="mt-3">
            <span
              className={`badge ${state.status === "AUTHENTICATED" ? "text-bg-success" : state.status === "FAILED" ? "text-bg-danger" : "text-bg-warning"}`}
            >
              {state.status}
            </span>
            {state.status === "AWAITING_CONFIRMATION" &&
            state.verificationUrl &&
            state.userCode ? (
              <div className="alert alert-warning mt-3 mb-0">
                <p>
                  Abra o link em uma nova aba e informe o código temporário:
                </p>
                <a
                  href={state.verificationUrl}
                  target="_blank"
                  rel="noreferrer"
                  className="btn btn-outline-primary me-3"
                >
                  Abrir autenticação OpenAI
                </a>
                <code className="fs-5 user-select-all">{state.userCode}</code>
              </div>
            ) : null}
            {state.detail ? (
              <p className="small mt-2 mb-0">{state.detail}</p>
            ) : null}
          </div>
        ) : null}
        {start.isError ? (
          <div className="alert alert-danger mt-3">
            Não foi possível iniciar a reconexão.
          </div>
        ) : null}
      </div>
    </div>
  );
}
