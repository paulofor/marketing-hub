import { FormEvent, useEffect, useMemo, useState } from "react";
import PageTitle from "../../components/PageTitle";
import {
  useHotmartAccessTokenSetting,
  useUpdateHotmartAccessTokenSetting,
} from "../../api/settings/useHotmartAccessTokenSetting";
import {
  useHotmartCollectedProducts,
  useHotmartCollectionJobs,
} from "../../api/settings/useHotmartCollectedProducts";

function shouldShowHotmartTokenAlert(
  status?: string | null,
  message?: string | null,
) {
  const normalizedStatus = status ?? "";
  const normalizedMessage = (message ?? "").toLowerCase();
  return (
    normalizedStatus === "COLLECTION_ERROR" ||
    normalizedStatus === "HOTMART_TOKEN_EXPIRED" ||
    normalizedMessage.includes("token jwt da hotmart") ||
    normalizedMessage.includes("invalid_token") ||
    normalizedMessage.includes("expired jwt")
  );
}

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

export default function HotmartPage() {
  const workspaceId = "workspace-001";
  const { data, isLoading, isError } = useHotmartAccessTokenSetting();
  const updateSetting = useUpdateHotmartAccessTokenSetting();
  const hotmartProductsQuery = useHotmartCollectedProducts(workspaceId, 24);
  const hotmartJobsQuery = useHotmartCollectionJobs(workspaceId);
  const [tokenValue, setTokenValue] = useState("");
  const [feedback, setFeedback] = useState<string | null>(null);

  useEffect(() => {
    if (data) {
      setTokenValue(data.value ?? "");
    }
  }, [data?.value]);

  const updatedAt = useMemo(
    () => formatUpdatedAt(data?.updatedAt),
    [data?.updatedAt],
  );
  const latestHotmartJob = useMemo(() => {
    const firstItem = hotmartProductsQuery.data?.[0];
    return {
      id: firstItem?.jobId,
      collectedAt: formatUpdatedAt(firstItem?.collectedAt),
    };
  }, [hotmartProductsQuery.data]);

  const latestHotmartExecution = useMemo(
    () => hotmartJobsQuery.data?.[0],
    [hotmartJobsQuery.data],
  );
  const showHotmartTokenAlert = shouldShowHotmartTokenAlert(
    latestHotmartExecution?.status,
    latestHotmartExecution?.message,
  );

  const hotmartCycleStats = useMemo(() => {
    const products = hotmartProductsQuery.data ?? [];
    const jobs = hotmartJobsQuery.data ?? [];

    const productsByCycle = new Map<string, number>();
    for (const product of products) {
      productsByCycle.set(
        product.jobId,
        (productsByCycle.get(product.jobId) ?? 0) + 1,
      );
    }

    return {
      totalExecutedJobs: jobs.length,
      totalProducts: products.length,
      cycles: jobs.map((job) => ({
        jobId: job.jobId,
        status: job.status,
        createdAt: formatUpdatedAt(job.createdAt),
        productsCount: productsByCycle.get(job.jobId) ?? 0,
      })),
    };
  }, [hotmartJobsQuery.data, hotmartProductsQuery.data]);

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setFeedback(null);

    const trimmed = tokenValue.trim();
    if (!trimmed) {
      setFeedback("Informe o token JWT de acesso da Hotmart.");
      return;
    }

    try {
      await updateSetting.mutateAsync(trimmed);
      setFeedback("Token salvo com sucesso.");
    } catch {
      setFeedback("Não foi possível salvar o token. Tente novamente.");
    }
  };

  return (
    <div className="mt-3">
      <PageTitle>Hotmart</PageTitle>

      <section className="card mt-3 shadow-sm border-0">
        <div className="card-header">
          <h5 className="mb-1">Token de acesso</h5>
          <p className="text-muted small mb-0">
            Cole aqui o JWT obtido no login da Hotmart para uso nas integrações
            deste módulo.
          </p>
        </div>
        <div className="card-body">
          {isLoading ? <p className="mb-0">Carregando token...</p> : null}
          {isError ? (
            <p className="text-danger mb-0">
              Não foi possível carregar o token salvo.
            </p>
          ) : null}

          {!isLoading && !isError ? (
            <form
              onSubmit={handleSubmit}
              className="d-flex flex-column gap-3"
              noValidate
            >
              <div>
                <label
                  htmlFor="hotmartAccessToken"
                  className="form-label fw-semibold"
                >
                  JWT Hotmart <span className="text-danger">*</span>
                </label>
                <textarea
                  id="hotmartAccessToken"
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
                <button
                  type="submit"
                  className="btn btn-primary"
                  disabled={updateSetting.isPending}
                >
                  {updateSetting.isPending ? (
                    <>
                      <span
                        className="spinner-border spinner-border-sm me-2"
                        role="status"
                        aria-hidden="true"
                      />
                      Salvando...
                    </>
                  ) : (
                    "Salvar"
                  )}
                </button>
              </div>
            </form>
          ) : null}

          {feedback ? (
            <div className="alert alert-info mt-3 mb-0">{feedback}</div>
          ) : null}
          {showHotmartTokenAlert ? (
            <div
              className="alert alert-danger border border-danger mt-3 mb-0"
              role="alert"
            >
              <strong>Ação necessária: atualize o token Hotmart.</strong>{" "}
              {latestHotmartExecution?.message ||
                "A última coleta não conseguiu autenticar na Hotmart."}
            </div>
          ) : null}
        </div>
      </section>

      <section className="card mt-3 shadow-sm border-0">
        <div className="card-header">
          <h5 className="mb-1">Resumo de ciclos Hotmart</h5>
          <p className="text-muted small mb-0">
            Mostra a quantidade de jobs executados e o total de produtos
            coletados no período carregado.
          </p>
        </div>
        <div className="card-body">
          {hotmartJobsQuery.isLoading || hotmartProductsQuery.isLoading ? (
            <p className="mb-0">Carregando resumo...</p>
          ) : null}

          {hotmartJobsQuery.isError || hotmartProductsQuery.isError ? (
            <p className="text-danger mb-0">
              Não foi possível carregar o resumo de ciclos.
            </p>
          ) : null}

          {!hotmartJobsQuery.isLoading &&
          !hotmartProductsQuery.isLoading &&
          !hotmartJobsQuery.isError &&
          !hotmartProductsQuery.isError ? (
            <>
              <div className="row g-3 mb-3">
                <div className="col-12 col-md-6">
                  <div className="border rounded p-3 h-100">
                    <div className="text-muted small">Jobs executados</div>
                    <div className="fs-4 fw-semibold">
                      {hotmartCycleStats.totalExecutedJobs}
                    </div>
                  </div>
                </div>
                <div className="col-12 col-md-6">
                  <div className="border rounded p-3 h-100">
                    <div className="text-muted small">
                      Total de produtos (geral)
                    </div>
                    <div className="fs-4 fw-semibold">
                      {hotmartCycleStats.totalProducts}
                    </div>
                  </div>
                </div>
              </div>

              {hotmartCycleStats.cycles.length > 0 ? (
                <div className="table-responsive">
                  <table className="table table-sm align-middle mb-0">
                    <thead>
                      <tr>
                        <th>Ciclo (job)</th>
                        <th>Status</th>
                        <th>Executado em</th>
                        <th className="text-end">Produtos no ciclo</th>
                      </tr>
                    </thead>
                    <tbody>
                      {hotmartCycleStats.cycles.map((cycle) => (
                        <tr key={cycle.jobId}>
                          <td className="small">{cycle.jobId}</td>
                          <td>{cycle.status}</td>
                          <td>{cycle.createdAt}</td>
                          <td className="text-end fw-semibold">
                            {cycle.productsCount}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              ) : (
                <p className="mb-0 text-secondary">
                  Nenhum ciclo encontrado para o workspace.
                </p>
              )}
            </>
          ) : null}
        </div>
      </section>

      <section className="card mt-3 shadow-sm border-0">
        <div className="card-header">
          <h5 className="mb-1">Produtos coletados da Hotmart</h5>
          <p className="text-muted small mb-0">
            Exibe os produtos da última coleta automática registrada para o
            workspace.
          </p>
        </div>
        <div className="card-body">
          {hotmartProductsQuery.isLoading ? (
            <p className="mb-0">Carregando produtos...</p>
          ) : null}
          {hotmartProductsQuery.isError ? (
            <p className="text-danger mb-0">
              Não foi possível carregar os produtos coletados.
            </p>
          ) : null}
          {!hotmartProductsQuery.isLoading &&
          !hotmartProductsQuery.isError &&
          hotmartProductsQuery.data &&
          hotmartProductsQuery.data.length === 0 ? (
            <p className="mb-0 text-secondary">
              Nenhuma coleta Hotmart encontrada até o momento.
            </p>
          ) : null}

          {hotmartProductsQuery.data && hotmartProductsQuery.data.length > 0 ? (
            <>
              <p className="small text-muted mb-3">
                Job mais recente: <strong>{latestHotmartJob.id}</strong> ·
                Data/hora: <strong>{latestHotmartJob.collectedAt}</strong>
              </p>

              {hotmartProductsQuery.data &&
              hotmartProductsQuery.data.length > 0 ? (
                <div className="row g-3">
                  {hotmartProductsQuery.data.map((item) => {
                    const imageUrl = item.imageUrl;
                    const producer = item.producerName;
                    const price = item.price;
                    const currency = item.currency ?? "BRL";
                    const salesPageUrl = item.salesPageUrl?.trim() || null;

                    return (
                      <article
                        key={item.referenceId}
                        className="col-12 col-md-6 col-lg-4"
                      >
                        <div className="card h-100 border">
                          {imageUrl ? (
                            <img
                              src={imageUrl}
                              alt={item.title}
                              className="card-img-top"
                              style={{ objectFit: "cover", maxHeight: "180px" }}
                            />
                          ) : null}
                          <div className="card-body d-flex flex-column">
                            <h6 className="card-title">{item.title}</h6>
                            <p className="small text-muted mb-2">
                              Produtor: {producer || "Não informado"}
                            </p>
                            <p className="small mb-2">
                              Temperatura:{" "}
                              <strong>{item.temperature ?? "—"}</strong>
                            </p>
                            <p className="small mb-2">
                              Página de vendas:{" "}
                              {salesPageUrl ? (
                                <a
                                  href={salesPageUrl}
                                  target="_blank"
                                  rel="noreferrer"
                                >
                                  {salesPageUrl}
                                </a>
                              ) : (
                                <span>Não informada</span>
                              )}
                            </p>
                            <p className="small mb-3">
                              Preço:{" "}
                              <strong>
                                {price
                                  ? `${currency} ${price}`
                                  : "Não informado"}
                              </strong>
                            </p>
                            {salesPageUrl ? (
                              <a
                                href={salesPageUrl}
                                target="_blank"
                                rel="noreferrer"
                                className="btn btn-outline-primary btn-sm mt-auto"
                              >
                                Ver página de vendas
                              </a>
                            ) : (
                              <button
                                type="button"
                                className="btn btn-outline-secondary btn-sm mt-auto"
                                disabled
                              >
                                Página de vendas indisponível
                              </button>
                            )}
                          </div>
                        </div>
                      </article>
                    );
                  })}
                </div>
              ) : null}
            </>
          ) : null}
        </div>
      </section>
    </div>
  );
}
