import { Link, useParams } from "react-router-dom";
import { useFrameworkImageStatuses } from "../../api/experiment/useFrameworkImageStatuses";

export default function ExperimentFrameworkImageDetailsPage() {
  const { id } = useParams<{ id: string }>();
  const { data, isLoading, isError } = useFrameworkImageStatuses(id);

  return (
    <div className="container-fluid py-3 d-flex flex-column gap-3">
      <div className="d-flex flex-wrap justify-content-between align-items-center gap-2">
        <div>
          <h1 className="h4 mb-1">Detalhe das imagens geradas</h1>
          <p className="text-muted mb-0">
            Prompt (request enviado ao Worker AI), modelo usado, custo e imagem final.
          </p>
        </div>
        <Link to={`/experiments/${id}`} className="btn btn-outline-secondary">
          Voltar ao experimento
        </Link>
      </div>

      {isLoading ? <p className="text-muted mb-0">Carregando imagens...</p> : null}
      {isError ? (
        <div className="alert alert-danger mb-0">Não foi possível carregar o detalhe das imagens.</div>
      ) : null}

      {!isLoading && !isError && (data?.length ?? 0) === 0 ? (
        <p className="text-muted mb-0">Nenhuma imagem encontrada para este experimento.</p>
      ) : null}

      <div className="row g-3">
        {(data ?? []).map((item) => (
          <div className="col-12" key={`${item.planningItemKey}-${item.jobId ?? "sem-job"}`}>
            <div className="card">
              <div className="card-body d-flex flex-column gap-3">
                <div className="d-flex flex-wrap gap-3 justify-content-between">
                  <div>
                    <h2 className="h6 mb-1">{item.sectionName ?? item.planningItemKey}</h2>
                    <div className="small text-muted">Status: {item.status}</div>
                  </div>
                  <div className="small text-muted text-end">
                    <div><strong>Modelo:</strong> {item.model ?? "Não informado"}</div>
                    <div><strong>Custo:</strong> Não informado pelo Worker AI</div>
                  </div>
                </div>

                <div>
                  <h3 className="h6 mb-2">Request enviado ao Worker AI</h3>
                  <pre className="bg-light border rounded p-3 mb-0" style={{ whiteSpace: "pre-wrap" }}>
                    {item.prompt ?? "Prompt não disponível."}
                  </pre>
                </div>

                <div>
                  <h3 className="h6 mb-2">Imagem gerada</h3>
                  {item.webUrl || item.sourceUrl ? (
                    <img
                      src={item.webUrl ?? item.sourceUrl}
                      alt={item.sectionName ?? item.planningItemKey}
                      className="img-fluid rounded border"
                      style={{ maxHeight: 420, objectFit: "contain" }}
                    />
                  ) : (
                    <p className="text-muted mb-0">Imagem ainda não disponível.</p>
                  )}
                </div>
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
