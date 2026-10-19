import { useState } from "react";
import {
  connectionPending,
  useAiWorkerConnection,
  useConnectAiWorker,
} from "../../api/ai/useAiWorkerConnection";

/** Conecta a conta operacional no provedor sem receber senha, token ou chave no painel. */
export default function AiWorkerConnectionPanel() {
  const query = useAiWorkerConnection();
  const connect = useConnectAiWorker();
  const [copyMessage, setCopyMessage] = useState("");
  const state = query.data;
  const pending = connectionPending(state?.status);
  const code =
    !query.isError &&
    state?.status === "AWAITING_CONFIRMATION" &&
    state.userCode;
  const officialUrl =
    state?.verificationUrl === "https://auth.openai.com/codex/device";

  async function copyCode() {
    try {
      await navigator.clipboard.writeText(state!.userCode!);
      setCopyMessage("Código copiado.");
    } catch {
      setCopyMessage(
        "Selecione o código abaixo e copie. Você também pode digitá-lo na OpenAI.",
      );
    }
  }

  let message =
    "Conecte a conta ChatGPT operacional para o Worker IA preparar textos e analisar imagens.";
  if (state?.connected)
    message = "Worker IA conectado. A sessão foi confirmada pelo executor.";
  if (state?.status === "REQUESTED")
    message =
      "Pedido registrado. Aguardando o Worker IA preparar o código de conexão.";
  if (state?.status === "STARTING")
    message =
      "Preparando o código de conexão. Esta tela será atualizada automaticamente.";
  if (code)
    message =
      "Abra a OpenAI, entre na conta operacional e informe o código abaixo. Depois volte a esta tela.";
  if (state?.status === "EXPIRED")
    message =
      "O código expirou. Gere um novo código nesta tela para tentar novamente.";
  if (state?.status === "FAILED")
    message =
      "A conexão não foi confirmada. Confira a conta e tente gerar um novo código.";
  if (query.isError)
    message = "Não foi possível consultar a conexão. Tente atualizar o estado.";

  return (
    <section
      className="card border-primary mb-4"
      id="worker-connection"
      aria-label="Conexão do Worker IA"
    >
      <div className="card-body">
        <h2 className="h5">Conectar Worker IA ao ChatGPT</h2>
        <p role="status" aria-live="polite">
          {query.isLoading ? "Consultando conexão..." : message}
        </p>
        {state && !state.workerAvailable && !pending && !query.isError ? (
          <p className="small text-body-secondary">
            Ainda não há uma confirmação recente do Worker. Solicite a conexão e
            acompanhe o resultado aqui.
          </p>
        ) : null}
        {code && officialUrl ? (
          <div className="alert alert-info">
            <ol>
              <li>
                <a
                  className="btn btn-primary my-2"
                  href="https://auth.openai.com/codex/device"
                  target="_blank"
                  rel="noopener noreferrer"
                >
                  Abrir conexão na OpenAI
                </a>
              </li>
              <li>
                Informe este código:{" "}
                <code
                  className="fs-4 user-select-all d-inline-block my-2"
                  aria-label="Código de conexão"
                >
                  {state.userCode}
                </code>
              </li>
              <li>
                Confirme na OpenAI e volte aqui. A confirmação aparece
                automaticamente.
              </li>
            </ol>
            <button
              type="button"
              className="btn btn-outline-primary"
              onClick={copyCode}
            >
              Copiar código
            </button>
            <p className="small mt-2 mb-0">
              Válido até {new Date(state.expiresAt).toLocaleTimeString("pt-BR")}{" "}
              no horário deste dispositivo.
            </p>
            {copyMessage ? (
              <p className="small mb-0" role="status">
                {copyMessage}
              </p>
            ) : null}
          </div>
        ) : null}
        <div className="d-flex gap-2 flex-wrap">
          {!pending ? (
            <button
              type="button"
              className="btn btn-primary"
              disabled={connect.isPending || query.isLoading || query.isError}
              onClick={() => {
                setCopyMessage("");
                connect.mutate();
              }}
            >
              {connect.isPending
                ? "Solicitando conexão..."
                : state?.connected
                  ? "Reconectar conta"
                  : state?.status === "EXPIRED" || state?.status === "FAILED"
                    ? "Gerar novo código"
                    : "Conectar Worker IA"}
            </button>
          ) : null}
          <button
            type="button"
            className="btn btn-outline-secondary"
            disabled={query.isFetching}
            onClick={() => void query.refetch()}
          >
            Atualizar estado
          </button>
        </div>
        {connect.isError ? (
          <p className="text-danger mt-2" role="alert">
            Não foi possível solicitar a conexão. Tente novamente; um pedido já
            salvo será reutilizado.
          </p>
        ) : null}
        <p className="small text-body-secondary mt-3 mb-0">
          A senha é informada apenas na OpenAI. A conexão não inicia gerações
          nem campanhas e não altera os limites dos produtos.
        </p>
      </div>
    </section>
  );
}
