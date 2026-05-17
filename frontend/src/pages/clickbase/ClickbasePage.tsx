import { FormEvent, useEffect, useMemo, useState } from "react";
import PageTitle from "../../components/PageTitle";
import {
  useClickbaseCollectedProducts,
  useClickbaseCollectionJobs,
} from "../../api/settings/useClickbaseCollectedProducts";
import {
  useClickbankAccessTokenSetting,
  useUpdateClickbankAccessTokenSetting,
} from "../../api/settings/useClickbankAccessTokenSetting";

function formatUpdatedAt(value?: string | null) {
  if (!value) return "—";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "—";
  return date.toLocaleString("pt-BR", {
    day: "2-digit",
    month: "short",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}

export default function ClickbasePage() {
  const workspaceId = "workspace-001";
  const clickbaseProductsQuery = useClickbaseCollectedProducts(workspaceId, 24);
  const clickbaseJobsQuery = useClickbaseCollectionJobs(workspaceId);
  const { data: clickbankSetting, isLoading: isLoadingSetting, isError: isErrorSetting } =
    useClickbankAccessTokenSetting();
  const updateClickbankSetting = useUpdateClickbankAccessTokenSetting();
  const [tokenValue, setTokenValue] = useState("");
  const [feedback, setFeedback] = useState<string | null>(null);

  useEffect(() => {
    if (clickbankSetting) {
      setTokenValue(clickbankSetting.value ?? "");
    }
  }, [clickbankSetting?.value]);

  const updatedAt = useMemo(() => formatUpdatedAt(clickbankSetting?.updatedAt), [clickbankSetting?.updatedAt]);
  const latestJobId = useMemo(() => clickbaseProductsQuery.data?.[0]?.jobId, [clickbaseProductsQuery.data]);

  const latestClickbaseExecution = useMemo(() => clickbaseJobsQuery.data?.[0], [clickbaseJobsQuery.data]);
  const shouldShowTokenRefreshAlert = useMemo(() => {
    if (!latestClickbaseExecution) {
      return false;
    }

    const message = (latestClickbaseExecution.message ?? "").toUpperCase();
    const hasTokenHint =
      message.includes("JWT_EXPIRED_OR_INVALID") ||
      message.includes("TOKEN") ||
      message.includes("403") ||
      message.includes("UNAUTHORIZED") ||
      message.includes("REQUEST BLOCKED");

    return (latestClickbaseExecution.status === "COLLECTION_ERROR" || latestClickbaseExecution.status === "COLLECTION_SKIPPED") && hasTokenHint;
  }, [latestClickbaseExecution]);

  const latestJobDate = useMemo(() => {
    const collectedAt = clickbaseProductsQuery.data?.[0]?.collectedAt;
    if (!collectedAt) {
      return null;
    }
    const parsed = new Date(collectedAt);
    if (Number.isNaN(parsed.getTime())) {
      return null;
    }
    return new Intl.DateTimeFormat("pt-BR", {
      dateStyle: "short",
      timeStyle: "short",
    }).format(parsed);
  }, [clickbaseProductsQuery.data]);

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setFeedback(null);

    const trimmed = tokenValue.trim();
    if (!trimmed) {
      setFeedback("Informe o token JWT de acesso do Clickbank.");
      return;
    }

    try {
      await updateClickbankSetting.mutateAsync(trimmed);
      setFeedback("Token salvo com sucesso.");
    } catch {
      setFeedback("Não foi possível salvar o token. Tente novamente.");
    }
  };

  return (
    <div className="mt-3">
      <PageTitle>Clickbase</PageTitle>

      <section className="card mt-3 shadow-sm border-0">
        <div className="card-header">
          <h5 className="mb-1">Token de acesso</h5>
          <p className="text-muted small mb-0">Cole aqui o JWT do Clickbank para uso nas próximas coletas automáticas.</p>
        </div>
        <div className="card-body">
          {isLoadingSetting ? <p className="mb-0">Carregando token...</p> : null}
          {isErrorSetting ? (
            <p className="text-danger mb-0">Não foi possível carregar o token salvo.</p>
          ) : null}

          {!isLoadingSetting && !isErrorSetting ? (
            <form onSubmit={handleSubmit} className="d-flex flex-column gap-3" noValidate>
              <div>
                <label htmlFor="clickbankAccessToken" className="form-label fw-semibold">
                  JWT Clickbank <span className="text-danger">*</span>
                </label>
                <textarea
                  id="clickbankAccessToken"
                  className="form-control"
                  rows={6}
                  value={tokenValue}
                  onChange={(event) => setTokenValue(event.target.value)}
                  placeholder="Cole aqui o token JWT"
                  required
                />
                <div className="form-text">Última atualização: {updatedAt}</div>
              </div>

              <div className="d-flex gap-2">
                <button type="submit" className="btn btn-primary" disabled={updateClickbankSetting.isPending}>
                  {updateClickbankSetting.isPending ? (
                    <>
                      <span className="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true" />
                      Salvando...
                    </>
                  ) : (
                    "Salvar"
                  )}
                </button>
              </div>
            </form>
          ) : null}

          {feedback ? <div className="alert alert-info mt-3 mb-0">{feedback}</div> : null}

          {shouldShowTokenRefreshAlert ? (
            <div className="alert alert-warning mt-3 mb-0" role="alert">
              <strong>Atenção:</strong> a última coleta indicou falha de autenticação no Graph da ClickBank.
              Atualize o token JWT no campo acima e salve para liberar as próximas coletas.
            </div>
          ) : null}
        </div>
      </section>

      <section className="card mt-3 shadow-sm border-0">
        <div className="card-header">
          <h5 className="mb-1">Produtos coletados</h5>
          <p className="text-muted small mb-0">Exibe os produtos da última coleta automática registrada para o workspace.</p>
        </div>
        <div className="card-body">
          {clickbaseProductsQuery.isLoading ? <p className="mb-0">Carregando produtos...</p> : null}
          {clickbaseProductsQuery.isError ? (
            <p className="text-danger mb-0">Não foi possível carregar os produtos coletados.</p>
          ) : null}
          {!clickbaseProductsQuery.isLoading &&
          !clickbaseProductsQuery.isError &&
          clickbaseProductsQuery.data &&
          clickbaseProductsQuery.data.length === 0 ? (
            <p className="mb-0 text-secondary">Nenhuma coleta Clickbase encontrada até o momento.</p>
          ) : null}

          {clickbaseProductsQuery.data && clickbaseProductsQuery.data.length > 0 ? (
            <>
              <p className="small text-muted mb-3">
                Job mais recente: <strong>{latestJobId}</strong>
              </p>
              <p className="small text-muted mb-3">
                Data do job: <strong>{latestJobDate ?? "Não informada"}</strong>
              </p>
              <div className="row g-3">
                {clickbaseProductsQuery.data.map((item) => (
                  <article key={item.referenceId} className="col-12 col-md-6 col-lg-4">
                    <div className="card h-100 border">
                      <div className="card-body d-flex flex-column">
                        <h6 className="card-title">{item.title || "Produto sem título"}</h6>
                        <p className="small text-muted mb-2">Produtor: {item.producerName || "Não informado"}</p>
                        <p className="small mb-3">
                          Score de sucesso: <strong>{item.successScore ?? "—"}</strong>
                        </p>
                        {item.productUrl ? (
                          <a href={item.productUrl} target="_blank" rel="noreferrer" className="btn btn-outline-primary btn-sm mt-auto">
                            Ver produto
                          </a>
                        ) : (
                          <span className="small text-muted mt-auto">URL do produto não disponível.</span>
                        )}
                      </div>
                    </div>
                  </article>
                ))}
              </div>
            </>
          ) : null}
        </div>
      </section>
    </div>
  );
}
