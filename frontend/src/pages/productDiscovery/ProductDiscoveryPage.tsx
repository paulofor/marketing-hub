import { FormEvent, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import PageTitle from "../../components/PageTitle";
import {
  productDiscoveryStatusLabels,
  useArchiveArtificialLegacyEvidence,
  useCreateProductDiscoveryCycle,
  useProductDiscoveryCycles,
  useProductDiscoveryMaturityRanking,
  type ProductDiscoveryResearchTrack,
} from "../../api/productDiscovery/useProductDiscovery";

function formatDateTime(value: string | undefined) {
  if (!value) {
    return "-";
  }
  return new Date(value).toLocaleString("pt-BR");
}

export default function ProductDiscoveryPage() {
  const cyclesQuery = useProductDiscoveryCycles();
  const maturityRankingQuery = useProductDiscoveryMaturityRanking();
  const createCycle = useCreateProductDiscoveryCycle();
  const legacyCleanup = useArchiveArtificialLegacyEvidence();
  const [theme, setTheme] = useState("");
  const [targetAudience, setTargetAudience] = useState("");
  const [acquisitionChannel, setAcquisitionChannel] = useState("Meta Ads");
  const [objective, setObjective] = useState(
    "Encontrar dores grandes, recorrentes e mal atendidas que possam virar PDE com microexperiência vendável.",
  );
  const [commercialConstraints, setCommercialConstraints] = useState(
    "Baixo esforço percebido, valor rápido, sem promessa garantida e com possibilidade de oferta paga em até 7 dias.",
  );
  const [forbiddenCategories, setForbiddenCategories] = useState(
    "Saúde sensível, promessa financeira garantida, jurídico individual, tratamento médico ou coleta de dados pessoais.",
  );

  const summary = useMemo(() => {
    const cycles = cyclesQuery.data ?? [];
    return {
      total: cycles.length,
      researching: cycles.filter((cycle) => cycle.status === "RESEARCHING")
        .length,
      completed: cycles.filter((cycle) => cycle.status === "COMPLETED").length,
      failed: cycles.filter((cycle) => cycle.status === "FAILED").length,
    };
  }, [cyclesQuery.data]);
  const topSuccessProducts = useMemo(
    () => maturityRankingQuery.data?.items.slice(0, 10) ?? [],
    [maturityRankingQuery.data],
  );

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    await createCycle.mutateAsync({
      theme,
      targetAudience,
      country: "BR",
      language: "pt-BR",
      acquisitionChannel,
      commercialConstraints,
      forbiddenCategories,
      objective,
    });
    setTheme("");
    setTargetAudience("");
  }

  async function handleCreateTrackCycle(track: ProductDiscoveryResearchTrack) {
    await createCycle.mutateAsync({
      theme: track.theme,
      targetAudience: track.targetAudience,
      country: "BR",
      language: "pt-BR",
      acquisitionChannel: track.acquisitionChannel,
      commercialConstraints: track.commercialConstraints,
      forbiddenCategories: track.forbiddenCategories,
      objective: track.objective,
    });
  }

  return (
    <div className="d-flex flex-column gap-4">
      <header className="d-flex flex-column gap-2">
        <PageTitle>Descoberta de Produtos PDE</PageTitle>
        <p className="text-secondary mb-0">
          Pesquise dores grandes, recorrentes e mal atendidas antes de criar
          hipótese, oferta, landing ou campanha.
        </p>
      </header>

      <section className="row g-3">
        <div className="col-md-3">
          <div className="card border-0 shadow-sm h-100">
            <div className="card-body">
              <p className="text-secondary mb-1">Ciclos</p>
              <strong className="h3 mb-0">{summary.total}</strong>
            </div>
          </div>
        </div>
        <div className="col-md-3">
          <div className="card border-0 shadow-sm h-100">
            <div className="card-body">
              <p className="text-secondary mb-1">Pesquisando</p>
              <strong className="h3 mb-0">{summary.researching}</strong>
            </div>
          </div>
        </div>
        <div className="col-md-3">
          <div className="card border-0 shadow-sm h-100">
            <div className="card-body">
              <p className="text-secondary mb-1">Concluídos</p>
              <strong className="h3 mb-0">{summary.completed}</strong>
            </div>
          </div>
        </div>
        <div className="col-md-3">
          <div className="card border-0 shadow-sm h-100">
            <div className="card-body">
              <p className="text-secondary mb-1">Falhas</p>
              <strong className="h3 mb-0">{summary.failed}</strong>
            </div>
          </div>
        </div>
      </section>

      <section className="card border-0 shadow-sm">
        <div className="card-body">
          <div className="d-flex flex-column flex-lg-row justify-content-between gap-3">
            <div>
              <h2 className="h5 mb-1">Confiabilidade das evidências legadas</h2>
              <p className="text-secondary mb-0">
                Preserve o histórico, mas retire do ranking ciclos antigos que
                usaram páginas de busca sem resultados como se fossem evidência.
              </p>
            </div>
            <button
              type="button"
              className="btn btn-outline-danger align-self-start"
              onClick={() => legacyCleanup.mutate()}
              disabled={legacyCleanup.isPending}
            >
              {legacyCleanup.isPending ? "Invalidando..." : "Invalidar evidências artificiais"}
            </button>
          </div>
          {legacyCleanup.data ? (
            <div className="alert alert-success mt-3 mb-0" role="status">
              {legacyCleanup.data.archivedCycles} ciclos e {" "}
              {legacyCleanup.data.archivedOpportunities} oportunidades foram
              arquivados sem apagar o histórico.
            </div>
          ) : null}
          {legacyCleanup.isError ? (
            <div className="alert alert-danger mt-3 mb-0" role="alert">
              Não foi possível invalidar as evidências artificiais.
            </div>
          ) : null}
        </div>
      </section>

      <section className="card border-0 shadow-sm">
        <div className="card-body">
          <div className="d-flex flex-column flex-lg-row justify-content-between gap-3 mb-3">
            <div>
              <h2 className="h5 mb-1">
                Top 10 produtos com mais chance de sucesso
              </h2>
              <p className="text-secondary mb-0">
                Visão rápida dos produtos priorizados pelo ranking de
                maturidade comercial do backend.
              </p>
            </div>
            <span className="badge text-bg-primary align-self-start">
              {topSuccessProducts.length} produtos
            </span>
          </div>
          {maturityRankingQuery.isLoading ? (
            <div className="text-secondary">Carregando produtos...</div>
          ) : null}
          {maturityRankingQuery.isError ? (
            <div className="alert alert-danger mb-0">
              Não foi possível carregar o Top 10 de produtos.
            </div>
          ) : null}
          {!maturityRankingQuery.isLoading &&
          !maturityRankingQuery.isError &&
          topSuccessProducts.length === 0 ? (
            <div className="text-secondary">
              Nenhum produto priorizado disponível no momento.
            </div>
          ) : null}
          {topSuccessProducts.length > 0 ? (
            <div className="row g-3">
              {topSuccessProducts.map((item) => (
                <div className="col-md-6 col-xl-4" key={item.position}>
                  <article className="border rounded-2 p-3 h-100">
                    <div className="d-flex justify-content-between align-items-start gap-2 mb-2">
                      <div>
                        <div className="small text-secondary">
                          #{item.position}
                        </div>
                        <h3 className="h6 mb-0">{item.niche}</h3>
                      </div>
                      <span className="badge text-bg-light">
                        {item.maturity}
                      </span>
                    </div>
                    <p className="small text-secondary mb-2">{item.summary}</p>
                    <p className="mb-2">{item.commercialReason}</p>
                    <div className="small">
                      <strong>Próxima ação:</strong> {item.recommendedAction}
                    </div>
                  </article>
                </div>
              ))}
            </div>
          ) : null}
        </div>
      </section>

      <section className="card border-0 shadow-sm">
        <div className="card-body">
          <div className="d-flex justify-content-between align-items-start gap-3 mb-3">
            <div>
              <h2 className="h5 mb-1">Ranking de maturidade comercial</h2>
              <p className="text-secondary mb-0">
                Priorize produtos prontos, oportunidades promissoras e temas
                que ainda precisam de evidência.
              </p>
            </div>
            <button
              type="button"
              className="btn btn-outline-secondary btn-sm"
              onClick={() => maturityRankingQuery.refetch()}
              disabled={maturityRankingQuery.isFetching}
            >
              Atualizar
            </button>
          </div>
          {maturityRankingQuery.isLoading ? (
            <div className="text-secondary">Carregando maturidade...</div>
          ) : null}
          {maturityRankingQuery.isError ? (
            <div className="alert alert-danger">
              Não foi possível carregar o ranking de maturidade.
            </div>
          ) : null}
          {maturityRankingQuery.data ? (
            <div className="d-flex flex-column gap-3">
              <div className="alert alert-primary mb-0">
                <strong>{maturityRankingQuery.data.strategyName}:</strong>{" "}
                {maturityRankingQuery.data.recommendedPriority}
                <div className="small mt-2">
                  {maturityRankingQuery.data.decisionCriterion}
                </div>
              </div>
              <div className="table-responsive">
                <table className="table align-middle">
                  <thead>
                    <tr>
                      <th>#</th>
                      <th>Nicho</th>
                      <th>Maturidade</th>
                      <th>Leitura comercial</th>
                      <th>Próxima ação</th>
                    </tr>
                  </thead>
                  <tbody>
                    {maturityRankingQuery.data.items.map((item) => (
                      <tr key={`${item.position}-${item.niche}`}>
                        <td>{item.position}</td>
                        <td>
                          <strong>{item.niche}</strong>
                          <div className="text-secondary small">
                            {item.summary}
                          </div>
                        </td>
                        <td>
                          <span className="badge text-bg-light">
                            {item.maturity}
                          </span>
                        </td>
                        <td>
                          <div>{item.commercialReason}</div>
                          <div className="small text-secondary mt-1">
                            Evidências: {item.evidence.join(" · ")}
                          </div>
                          <div className="small text-danger mt-1">
                            Travas: {item.guardrails.join(" · ")}
                          </div>
                        </td>
                        <td>{item.recommendedAction}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
              <div>
                <h3 className="h6 mb-3">Próximos ciclos recomendados</h3>
                <div className="row g-3">
                  {maturityRankingQuery.data.recommendedTracks.map((track) => (
                    <div className="col-lg-4" key={track.name}>
                      <article className="border rounded-2 p-3 h-100 d-flex flex-column gap-2">
                        <div>
                          <h4 className="h6 mb-1">{track.name}</h4>
                          <p className="text-secondary small mb-0">
                            {track.focus}
                          </p>
                        </div>
                        <p className="mb-0">{track.reason}</p>
                        <button
                          type="button"
                          className="btn btn-outline-primary btn-sm mt-auto align-self-start"
                          onClick={() => handleCreateTrackCycle(track)}
                          disabled={createCycle.isPending}
                        >
                          {createCycle.isPending ? (
                            <span
                              className="spinner-border spinner-border-sm me-2"
                              aria-hidden="true"
                            />
                          ) : null}
                          Criar ciclo
                        </button>
                      </article>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          ) : null}
        </div>
      </section>

      <section className="card border-0 shadow-sm">
        <div className="card-body">
          <h2 className="h5 mb-2">Novo ciclo de pesquisa</h2>
          <form className="row g-3" onSubmit={handleSubmit}>
            <div className="col-md-4">
              <label className="form-label" htmlFor="product-discovery-theme">
                Tema amplo <span className="text-danger">*</span>
              </label>
              <input
                id="product-discovery-theme"
                className="form-control"
                value={theme}
                onChange={(event) => setTheme(event.target.value)}
                required
                maxLength={191}
                placeholder="Ex.: mulheres que compram roupa online"
              />
            </div>
            <div className="col-md-4">
              <label className="form-label" htmlFor="product-discovery-audience">
                Público desejado
              </label>
              <input
                id="product-discovery-audience"
                className="form-control"
                value={targetAudience}
                onChange={(event) => setTargetAudience(event.target.value)}
                maxLength={191}
                placeholder="Ex.: mulheres 30+ com insegurança de estilo"
              />
            </div>
            <div className="col-md-4">
              <label className="form-label" htmlFor="product-discovery-channel">
                Canal provável
              </label>
              <input
                id="product-discovery-channel"
                className="form-control"
                value={acquisitionChannel}
                onChange={(event) => setAcquisitionChannel(event.target.value)}
                maxLength={120}
              />
            </div>
            <div className="col-md-4">
              <label className="form-label" htmlFor="product-discovery-objective">
                Objetivo
              </label>
              <textarea
                id="product-discovery-objective"
                className="form-control"
                rows={4}
                value={objective}
                onChange={(event) => setObjective(event.target.value)}
              />
            </div>
            <div className="col-md-4">
              <label
                className="form-label"
                htmlFor="product-discovery-constraints"
              >
                Restrições comerciais
              </label>
              <textarea
                id="product-discovery-constraints"
                className="form-control"
                rows={4}
                value={commercialConstraints}
                onChange={(event) =>
                  setCommercialConstraints(event.target.value)
                }
              />
            </div>
            <div className="col-md-4">
              <label
                className="form-label"
                htmlFor="product-discovery-forbidden"
              >
                Categorias proibidas
              </label>
              <textarea
                id="product-discovery-forbidden"
                className="form-control"
                rows={4}
                value={forbiddenCategories}
                onChange={(event) => setForbiddenCategories(event.target.value)}
              />
            </div>
            {createCycle.isError ? (
              <div className="col-12">
                <div className="alert alert-danger mb-0">
                  Não foi possível criar o ciclo. Revise os campos.
                </div>
              </div>
            ) : null}
            <div className="col-12">
              <button
                type="submit"
                className="btn btn-primary"
                disabled={createCycle.isPending}
              >
                {createCycle.isPending ? (
                  <span
                    className="spinner-border spinner-border-sm me-2"
                    aria-hidden="true"
                  />
                ) : null}
                Criar e enviar para pesquisa
              </button>
            </div>
          </form>
        </div>
      </section>

      <section className="card border-0 shadow-sm">
        <div className="card-body">
          <div className="d-flex justify-content-between align-items-start gap-3 mb-3">
            <div>
              <h2 className="h5 mb-1">Ciclos recentes</h2>
              <p className="text-secondary mb-0">
                Acompanhe o que já virou oportunidade e o que ainda precisa de
                evidência.
              </p>
            </div>
            <button
              type="button"
              className="btn btn-outline-secondary btn-sm"
              onClick={() => cyclesQuery.refetch()}
              disabled={cyclesQuery.isFetching}
            >
              Atualizar
            </button>
          </div>
          {cyclesQuery.isLoading ? (
            <div className="text-secondary">Carregando ciclos...</div>
          ) : null}
          {cyclesQuery.isError ? (
            <div className="alert alert-danger">
              Não foi possível carregar os ciclos.
            </div>
          ) : null}
          <div className="table-responsive">
            <table className="table align-middle">
              <thead>
                <tr>
                  <th>Tema</th>
                  <th>Público</th>
                  <th>Status</th>
                  <th>Atualizado</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                {(cyclesQuery.data ?? []).map((cycle) => (
                  <tr key={cycle.id}>
                    <td>
                      <strong>{cycle.theme}</strong>
                      {cycle.errorMessage ? (
                        <div className="text-danger small">
                          {cycle.errorMessage}
                        </div>
                      ) : null}
                    </td>
                    <td>{cycle.targetAudience || "-"}</td>
                    <td>{productDiscoveryStatusLabels[cycle.status]}</td>
                    <td>{formatDateTime(cycle.updatedAt)}</td>
                    <td className="text-end">
                      <Link
                        className="btn btn-outline-primary btn-sm"
                        to={`/product-discovery/cycles/${cycle.id}`}
                      >
                        Ver ranking
                      </Link>
                    </td>
                  </tr>
                ))}
                {!cyclesQuery.isLoading && (cyclesQuery.data ?? []).length === 0 ? (
                  <tr>
                    <td className="text-secondary" colSpan={5}>
                      Nenhum ciclo criado ainda.
                    </td>
                  </tr>
                ) : null}
              </tbody>
            </table>
          </div>
        </div>
      </section>
    </div>
  );
}
