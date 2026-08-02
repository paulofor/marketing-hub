import { Megaphone, RefreshCw, Target, TrendingUp } from "lucide-react";
import PageTitle from "../../components/PageTitle";
import { useWinningAdsLibrary } from "../../api/useWinningAdsLibrary";

const PRODUCT_SLUG = "costure-e-venda";

const statusLabels: Record<string, string> = {
  PILOTO: "Piloto",
  WINNER: "Vencedor",
  TESTING: "Em teste",
};

function formatDateTime(value?: string | null) {
  if (!value) {
    return "Sem atualização";
  }
  return new Intl.DateTimeFormat("pt-BR", {
    dateStyle: "short",
    timeStyle: "short",
  }).format(new Date(value));
}

export default function WinningAdsLibraryPage() {
  const libraryQuery = useWinningAdsLibrary(PRODUCT_SLUG);
  const ads = libraryQuery.data?.items ?? [];
  const topScore = ads.length ? Math.max(...ads.map((ad) => ad.score)) : 0;
  const channels = Array.from(new Set(ads.map((ad) => ad.channel))).join(", ");

  return (
    <div className="d-flex flex-column gap-4">
      <header className="d-flex flex-wrap justify-content-between gap-3">
        <div>
          <PageTitle>Biblioteca de Anúncios Vencedores</PageTitle>
          <p className="text-secondary mb-0">
            Piloto Costure e Venda com anúncios reutilizáveis, ângulos,
            criativos, aprendizados e próxima ação de teste.
          </p>
        </div>
        <button
          type="button"
          className="btn btn-outline-primary d-inline-flex align-items-center gap-2"
          onClick={() => void libraryQuery.refetch()}
          disabled={libraryQuery.isFetching}
        >
          {libraryQuery.isFetching ? (
            <span className="spinner-border spinner-border-sm" />
          ) : (
            <RefreshCw size={16} />
          )}
          Atualizar
        </button>
      </header>

      <section className="row g-3">
        <div className="col-md-4">
          <div className="card border-0 shadow-sm h-100">
            <div className="card-body d-flex align-items-center gap-3">
              <span className="rounded bg-primary-subtle text-primary-emphasis p-3">
                <Megaphone size={22} />
              </span>
              <div>
                <p className="text-secondary mb-1">Peças no piloto</p>
                <h3 className="mb-0">{libraryQuery.data?.total ?? 0}</h3>
              </div>
            </div>
          </div>
        </div>
        <div className="col-md-4">
          <div className="card border-0 shadow-sm h-100">
            <div className="card-body d-flex align-items-center gap-3">
              <span className="rounded bg-success-subtle text-success-emphasis p-3">
                <TrendingUp size={22} />
              </span>
              <div>
                <p className="text-secondary mb-1">Maior score</p>
                <h3 className="mb-0">{topScore || "—"}</h3>
              </div>
            </div>
          </div>
        </div>
        <div className="col-md-4">
          <div className="card border-0 shadow-sm h-100">
            <div className="card-body d-flex align-items-center gap-3">
              <span className="rounded bg-warning-subtle text-warning-emphasis p-3">
                <Target size={22} />
              </span>
              <div>
                <p className="text-secondary mb-1">Canais</p>
                <h3 className="fs-5 mb-0">{channels || "—"}</h3>
              </div>
            </div>
          </div>
        </div>
      </section>

      {libraryQuery.isError ? (
        <div className="alert alert-danger" role="alert">
          Não foi possível carregar a Biblioteca de Anúncios Vencedores.
        </div>
      ) : null}

      {libraryQuery.isLoading ? (
        <div className="d-flex align-items-center gap-2 text-secondary">
          <span className="spinner-border spinner-border-sm" />
          Carregando piloto Costure e Venda...
        </div>
      ) : null}

      {!libraryQuery.isLoading && ads.length === 0 ? (
        <div className="alert alert-secondary mb-0">
          Nenhum anúncio vencedor cadastrado para Costure e Venda.
        </div>
      ) : null}

      <section className="row g-3">
        {ads.map((ad) => (
          <article className="col-xl-4 col-lg-6" key={ad.id}>
            <div className="card border-0 shadow-sm h-100">
              <div className="card-body d-flex flex-column gap-3">
                <div className="d-flex justify-content-between gap-3">
                  <div>
                    <span className="badge text-bg-primary mb-2">
                      {statusLabels[ad.winningStatus] ?? ad.winningStatus}
                    </span>
                    <h2 className="fs-5 mb-1">{ad.hook}</h2>
                    <p className="text-secondary small mb-0">
                      {ad.productName} · {ad.format} · {ad.funnelStage}
                    </p>
                  </div>
                  <div className="text-end">
                    <span className="d-block text-secondary small">Score</span>
                    <strong className="fs-4">{ad.score}</strong>
                  </div>
                </div>

                <div>
                  <h3 className="fs-6">Texto principal</h3>
                  <p className="mb-0">{ad.primaryText}</p>
                </div>

                <div>
                  <h3 className="fs-6">Criativo</h3>
                  <p className="mb-0">{ad.creativeBrief}</p>
                </div>

                <div className="border-top pt-3">
                  <h3 className="fs-6">Ângulo e prova</h3>
                  <p className="mb-2">{ad.offerAngle}</p>
                  <p className="text-secondary mb-0">{ad.proofSignal}</p>
                </div>

                <div className="bg-light rounded p-3">
                  <h3 className="fs-6">Aprendizado</h3>
                  <p className="mb-2">{ad.learning}</p>
                  <strong className="d-block mb-1">Próxima ação</strong>
                  <p className="mb-0">{ad.nextAction}</p>
                </div>

                <p className="text-secondary small mb-0 mt-auto">
                  Métrica: {ad.metricSnapshot}
                  <br />
                  Atualizado em {formatDateTime(ad.updatedAt)}
                </p>
              </div>
            </div>
          </article>
        ))}
      </section>
    </div>
  );
}
