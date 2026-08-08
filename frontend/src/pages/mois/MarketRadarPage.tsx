import { Link } from "react-router-dom";
import PageTitle from "../../components/PageTitle";
import { useMoisSalesLibraryOpportunityRanking } from "../../api/mois/useMoisSalesLibrary";

const WORKSPACE_ID = "workspace-001";

const recommendationLabel: Record<string, string> = {
  PRIORITIZE: "Priorizar validação",
  OBSERVE: "Observar",
  RESEARCH_MORE: "Pesquisar mais",
  SATURATED_REQUIRES_ANGLE: "Buscar diferenciação",
  DO_NOT_PRIORITIZE: "Não priorizar",
};

function score(value?: number) {
  return value == null
    ? "—"
    : value.toLocaleString("pt-BR", { maximumFractionDigits: 1 });
}

/** Exibe oportunidades de mercado consolidadas a partir das evidências auditáveis do MOIS. */
export default function MarketRadarPage() {
  const ranking = useMoisSalesLibraryOpportunityRanking(WORKSPACE_ID, 50);
  const items = ranking.data?.items ?? [];

  return (
    <div className="d-flex flex-column gap-4">
      <header>
        <PageTitle>Radar de produtos e oportunidades</PageTitle>
        <p className="text-body-secondary mb-0">
          Sinais diários de mercado para escolher o que pesquisar ou testar.
          Ranking não comprova vendas: somente resultados próprios validam uma
          oportunidade.
        </p>
      </header>

      <section className="row g-3" aria-label="Cobertura das fontes">
        {[
          [
            "Hotmart",
            "Coleta diária às 05:00",
            "Ativa quando houver credencial válida",
          ],
          [
            "ClickBank",
            "Ciclos alternados a cada hora",
            "Em espera até habilitar credencial",
          ],
          [
            "Meta Ads",
            "Coleta supervisionada",
            "Sem scraping ou contorno da plataforma",
          ],
        ].map(([name, cadence, detail]) => (
          <div className="col-12 col-md-4" key={name}>
            <div className="card h-100">
              <div className="card-body">
                <h2 className="h6">{name}</h2>
                <p className="mb-1">{cadence}</p>
                <p className="small text-body-secondary mb-0">{detail}</p>
              </div>
            </div>
          </div>
        ))}
      </section>

      <section className="card">
        <div className="card-body">
          <div className="d-flex flex-wrap justify-content-between gap-2 mb-3">
            <div>
              <h2 className="h5 mb-1">Oportunidades priorizadas</h2>
              <p className="small text-body-secondary mb-0">
                Página, aquecimento, saturação e recência compõem a prioridade.
              </p>
            </div>
            <Link
              className="btn btn-outline-primary btn-sm"
              to="/mois/sales-pages-library/pipeline"
            >
              Ver operação do radar
            </Link>
          </div>
          {ranking.isLoading ? <p>Carregando evidências...</p> : null}
          {ranking.isError ? (
            <div className="alert alert-warning">
              Não foi possível carregar o radar. Nenhuma oportunidade foi
              inferida.
            </div>
          ) : null}
          {!ranking.isLoading && !ranking.isError && items.length === 0 ? (
            <div className="alert alert-secondary mb-0">
              Ainda não há evidências suficientes. Aguarde uma coleta válida ou
              cadastre uma observação supervisionada da Meta.
            </div>
          ) : null}
          {items.length > 0 ? (
            <div className="table-responsive">
              <table className="table align-middle">
                <thead>
                  <tr>
                    <th>Produto / fonte</th>
                    <th>Score</th>
                    <th>Mercado</th>
                    <th>Decisão</th>
                    <th>Evidência e próxima ação</th>
                  </tr>
                </thead>
                <tbody>
                  {items.map((item) => (
                    <tr key={item.pageId}>
                      <td>
                        <Link
                          to={`/mois/sales-pages-library/${item.pageId}`}
                          className="fw-semibold"
                        >
                          {item.title || `Página #${item.pageId}`}
                        </Link>
                        <div className="small text-body-secondary">
                          {item.source || "Fonte não informada"}
                        </div>
                      </td>
                      <td>
                        <strong>{score(item.combinedCommercialScore)}</strong>
                        <div className="small text-body-secondary">
                          Página {score(item.pageScoreTotal)} · mercado{" "}
                          {score(item.warmupScoreTotal)}
                        </div>
                      </td>
                      <td>
                        {item.marketTemperature}
                        <div className="small text-body-secondary">
                          {item.ecosystemType}
                        </div>
                      </td>
                      <td>
                        <span className="badge text-bg-light">
                          {recommendationLabel[item.recommendation] ||
                            item.recommendation}
                        </span>
                      </td>
                      <td className="small">
                        <div>{item.evidenceSummary}</div>
                        <strong>{item.suggestedNextAction}</strong>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : null}
        </div>
      </section>
    </div>
  );
}
