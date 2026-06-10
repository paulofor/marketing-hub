import { FormEvent, useMemo, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import PageTitle from "../../components/PageTitle";
import {
  OprmGeneralAudienceSeedType,
  seedStatusLabels,
  seedTypeLabels,
  useCreateOprmGeneralAudienceSeed,
  useOprmGeneralAudienceSeeds,
} from "../../api/oprm/useOprmGeneralAudiences";
import OprmModuleNavigation from "./OprmModuleNavigation";

const seedTypeOptions: OprmGeneralAudienceSeedType[] = [
  "CATEGORY",
  "DESIRE",
  "LIFE_CONTEXT",
  "BEHAVIOR",
  "CHANNEL",
  "PAIN_CLUSTER",
];

function formatDateTime(value: string | undefined) {
  if (!value) {
    return "-";
  }
  return new Date(value).toLocaleString("pt-BR");
}

export default function OprmGeneralAudiencesPage() {
  const navigate = useNavigate();
  const seedsQuery = useOprmGeneralAudienceSeeds();
  const createSeed = useCreateOprmGeneralAudienceSeed();
  const [name, setName] = useState("");
  const [seedType, setSeedType] =
    useState<OprmGeneralAudienceSeedType>("CATEGORY");
  const [marketContext, setMarketContext] = useState("");
  const [businessGoal, setBusinessGoal] = useState("");
  const [riskNotes, setRiskNotes] = useState("");

  const summary = useMemo(() => {
    const seeds = seedsQuery.data ?? [];
    return {
      total: seeds.length,
      ready: seeds.filter((seed) => seed.status === "READY_FOR_RESEARCH")
        .length,
      mapped: seeds.filter((seed) => seed.status === "MAPPED").length,
      pausedOrArchived: seeds.filter(
        (seed) => seed.status === "PAUSED" || seed.status === "ARCHIVED",
      ).length,
    };
  }, [seedsQuery.data]);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const seed = await createSeed.mutateAsync({
      name,
      seedType,
      marketContext,
      businessGoal,
      riskNotes,
      country: "BR",
      language: "pt-BR",
      status: "DRAFT",
    });
    navigate(`/oprm/general-audiences/seeds/${seed.id}`);
  }

  return (
    <div className="d-flex flex-column gap-4">
      <header className="d-flex flex-column gap-2">
        <PageTitle>OPRM — Públicos Gerais</PageTitle>
        <p className="text-secondary mb-0">
          Cadastre sementes amplas, quebre em subnichos e valide dores antes de
          criar qualquer nicho, hipótese ou experimento. Este fluxo é separado
          do NichoCNAE para evitar sobreposição e campanhas genéricas.
        </p>
      </header>

      <OprmModuleNavigation />

      <section className="row g-3">
        <div className="col-md-3">
          <div className="card border-0 shadow-sm h-100">
            <div className="card-body">
              <p className="text-secondary mb-1">Sementes</p>
              <strong className="h3 mb-0">{summary.total}</strong>
            </div>
          </div>
        </div>
        <div className="col-md-3">
          <div className="card border-0 shadow-sm h-100">
            <div className="card-body">
              <p className="text-secondary mb-1">Prontas</p>
              <strong className="h3 mb-0">{summary.ready}</strong>
            </div>
          </div>
        </div>
        <div className="col-md-3">
          <div className="card border-0 shadow-sm h-100">
            <div className="card-body">
              <p className="text-secondary mb-1">Mapeadas</p>
              <strong className="h3 mb-0">{summary.mapped}</strong>
            </div>
          </div>
        </div>
        <div className="col-md-3">
          <div className="card border-0 shadow-sm h-100">
            <div className="card-body">
              <p className="text-secondary mb-1">Pausadas/arquivadas</p>
              <strong className="h3 mb-0">{summary.pausedOrArchived}</strong>
            </div>
          </div>
        </div>
      </section>

      <section className="card border-0 shadow-sm">
        <div className="card-body">
          <h2 className="h5 mb-2">Nova semente manual</h2>
          <p className="text-secondary">
            Use somente mercados amplos que ainda precisam virar subnichos com
            dor, linguagem, isca e pergunta qualificadora.
          </p>
          <form className="row g-3" onSubmit={handleSubmit}>
            <div className="col-md-4">
              <label className="form-label" htmlFor="general-audience-name">
                Nome da semente <span className="text-danger">*</span>
              </label>
              <input
                id="general-audience-name"
                className="form-control"
                value={name}
                onChange={(event) => setName(event.target.value)}
                required
                maxLength={191}
                placeholder="Ex.: Beleza para autônomas"
              />
            </div>
            <div className="col-md-3">
              <label className="form-label" htmlFor="general-audience-type">
                Tipo <span className="text-danger">*</span>
              </label>
              <select
                id="general-audience-type"
                className="form-select"
                value={seedType}
                onChange={(event) =>
                  setSeedType(event.target.value as OprmGeneralAudienceSeedType)
                }
                required
              >
                {seedTypeOptions.map((type) => (
                  <option key={type} value={type}>
                    {seedTypeLabels[type]}
                  </option>
                ))}
              </select>
            </div>
            <div className="col-md-5">
              <label className="form-label" htmlFor="general-audience-market">
                Contexto de mercado
              </label>
              <input
                id="general-audience-market"
                className="form-control"
                value={marketContext}
                onChange={(event) => setMarketContext(event.target.value)}
                placeholder="Ex.: profissionais que dependem de agenda, WhatsApp e Instagram"
              />
            </div>
            <div className="col-md-6">
              <label className="form-label" htmlFor="general-audience-goal">
                Objetivo comercial
              </label>
              <textarea
                id="general-audience-goal"
                className="form-control"
                rows={3}
                value={businessGoal}
                onChange={(event) => setBusinessGoal(event.target.value)}
                placeholder="Qual venda futura este público pode sustentar?"
              />
            </div>
            <div className="col-md-6">
              <label className="form-label" htmlFor="general-audience-risk">
                Risco/compliance
              </label>
              <textarea
                id="general-audience-risk"
                className="form-control"
                rows={3}
                value={riskNotes}
                onChange={(event) => setRiskNotes(event.target.value)}
                placeholder="Promessas proibidas, sensibilidades e cuidados"
              />
            </div>
            {createSeed.isError ? (
              <div className="col-12">
                <div className="alert alert-danger mb-0">
                  Não foi possível criar a semente. Revise os campos e tente
                  novamente.
                </div>
              </div>
            ) : null}
            <div className="col-12">
              <button
                type="submit"
                className="btn btn-primary"
                disabled={createSeed.isPending}
              >
                {createSeed.isPending ? (
                  <span
                    className="spinner-border spinner-border-sm me-2"
                    aria-hidden="true"
                  />
                ) : null}
                Criar semente
              </button>
            </div>
          </form>
        </div>
      </section>

      <section className="card border-0 shadow-sm">
        <div className="card-body">
          <div className="d-flex justify-content-between align-items-start gap-3 mb-3">
            <div>
              <h2 className="h5 mb-1">Sementes cadastradas</h2>
              <p className="text-secondary mb-0">
                Acompanhe a origem ampla antes de aprovar subnichos específicos.
              </p>
            </div>
            <button
              type="button"
              className="btn btn-outline-secondary btn-sm"
              onClick={() => seedsQuery.refetch()}
              disabled={seedsQuery.isFetching}
            >
              {seedsQuery.isFetching ? (
                <span
                  className="spinner-border spinner-border-sm me-2"
                  aria-hidden="true"
                />
              ) : null}
              Atualizar
            </button>
          </div>

          {seedsQuery.isLoading ? (
            <div
              className="spinner-border text-primary"
              role="status"
              aria-label="Carregando sementes"
            />
          ) : null}
          {seedsQuery.isError ? (
            <div className="alert alert-danger">
              Não foi possível carregar as sementes de públicos gerais.
            </div>
          ) : null}
          {!seedsQuery.isLoading && !seedsQuery.isError ? (
            <div className="table-responsive">
              <table className="table table-sm align-middle mb-0">
                <thead>
                  <tr>
                    <th>Semente</th>
                    <th>Tipo</th>
                    <th>Status</th>
                    <th>Contexto</th>
                    <th>Atualização</th>
                    <th>Ação</th>
                  </tr>
                </thead>
                <tbody>
                  {(seedsQuery.data ?? []).length > 0 ? (
                    (seedsQuery.data ?? []).map((seed) => (
                      <tr key={seed.id}>
                        <td>{seed.name}</td>
                        <td>{seedTypeLabels[seed.seedType]}</td>
                        <td>{seedStatusLabels[seed.status]}</td>
                        <td className="text-secondary">
                          {seed.marketContext || "Sem contexto informado"}
                        </td>
                        <td>{formatDateTime(seed.updatedAt)}</td>
                        <td>
                          <Link
                            className="btn btn-sm btn-outline-primary"
                            to={`/oprm/general-audiences/seeds/${seed.id}`}
                          >
                            Abrir
                          </Link>
                        </td>
                      </tr>
                    ))
                  ) : (
                    <tr>
                      <td colSpan={6} className="text-secondary">
                        Nenhuma semente cadastrada ainda.
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          ) : null}
        </div>
      </section>
    </div>
  );
}
