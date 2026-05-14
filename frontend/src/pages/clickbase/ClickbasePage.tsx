import { useMemo } from "react";
import PageTitle from "../../components/PageTitle";
import { useClickbaseCollectedProducts } from "../../api/settings/useClickbaseCollectedProducts";

export default function ClickbasePage() {
  const workspaceId = "workspace-001";
  const clickbaseProductsQuery = useClickbaseCollectedProducts(workspaceId, 24);
  const latestJobId = useMemo(() => clickbaseProductsQuery.data?.[0]?.jobId, [clickbaseProductsQuery.data]);
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

  return (
    <div className="mt-3">
      <PageTitle>Clickbase</PageTitle>

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
