import { FormEvent, useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import PageTitle from "../../components/PageTitle";
import {
  CreateGeneralAudienceSubnichePayload,
  OprmGeneralAudienceSeedStatus,
  seedStatusLabels,
  seedTypeLabels,
  subnicheStatusLabels,
  useArchiveOprmGeneralAudienceSeed,
  useCreateOprmGeneralAudienceSubniche,
  useOprmGeneralAudienceSeed,
  useOprmGeneralAudienceSubniches,
  useUpdateOprmGeneralAudienceSeed,
} from "../../api/oprm/useOprmGeneralAudiences";
import OprmModuleNavigation from "./OprmModuleNavigation";

const seedStatusOptions: OprmGeneralAudienceSeedStatus[] = [
  "DRAFT",
  "READY_FOR_RESEARCH",
  "RESEARCHING",
  "MAPPED",
  "PAUSED",
  "ARCHIVED",
];

function parseRouteId(value: string | undefined) {
  const parsed = Number(value);
  return Number.isFinite(parsed) && parsed > 0 ? parsed : undefined;
}

function formatDateTime(value: string | undefined) {
  if (!value) {
    return "-";
  }
  return new Date(value).toLocaleString("pt-BR");
}

function scoreLabel(value: number | null | undefined) {
  return value == null ? "Pendente" : value.toLocaleString("pt-BR");
}

export default function OprmGeneralAudienceSeedDetailPage() {
  const { seedId: seedIdParam } = useParams();
  const navigate = useNavigate();
  const seedId = parseRouteId(seedIdParam);
  const seedQuery = useOprmGeneralAudienceSeed(seedId);
  const subnichesQuery = useOprmGeneralAudienceSubniches(seedId);
  const updateSeed = useUpdateOprmGeneralAudienceSeed(seedId ?? 0);
  const archiveSeed = useArchiveOprmGeneralAudienceSeed(seedId ?? 0);
  const createSubniche = useCreateOprmGeneralAudienceSubniche(seedId ?? 0);
  const [status, setStatus] = useState<OprmGeneralAudienceSeedStatus>("DRAFT");
  const [subnicheForm, setSubnicheForm] =
    useState<CreateGeneralAudienceSubnichePayload>({
      name: "",
      personaSummary: "",
      painSummary: "",
      desiredOutcomeSummary: "",
      languagePatterns: "",
      channelsSummary: "",
      qualificationQuestion: "",
      status: "DISCOVERED",
    });

  useEffect(() => {
    if (seedQuery.data?.status) {
      setStatus(seedQuery.data.status);
    }
  }, [seedQuery.data?.status]);

  if (!seedId) {
    return <div className="alert alert-danger">Semente inválida.</div>;
  }

  async function handleStatusSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    await updateSeed.mutateAsync({ status });
  }

  async function handleArchive() {
    await archiveSeed.mutateAsync();
    setStatus("ARCHIVED");
  }

  async function handleSubnicheSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const subniche = await createSubniche.mutateAsync(subnicheForm);
    setSubnicheForm({
      name: "",
      personaSummary: "",
      painSummary: "",
      desiredOutcomeSummary: "",
      languagePatterns: "",
      channelsSummary: "",
      qualificationQuestion: "",
      status: "DISCOVERED",
    });
    navigate(`/oprm/general-audiences/subniches/${subniche.id}`);
  }

  const seed = seedQuery.data;

  return (
    <div className="d-flex flex-column gap-4">
      <header className="d-flex flex-column gap-2">
        <PageTitle>{seed?.name ?? "Semente de Público Geral"}</PageTitle>
        <p className="text-secondary mb-0">
          Revise a semente como ponto de partida amplo. Ela não deve ser tratada
          como CNAE, campanha ou oferta final.
        </p>
      </header>

      <OprmModuleNavigation />

      {seedQuery.isLoading ? (
        <div
          className="spinner-border text-primary"
          role="status"
          aria-label="Carregando semente"
        />
      ) : null}
      {seedQuery.isError ? (
        <div className="alert alert-danger">
          Não foi possível carregar a semente de público geral.
        </div>
      ) : null}

      {seed ? (
        <>
          <section className="card border-0 shadow-sm">
            <div className="card-body">
              <div className="d-flex justify-content-between align-items-start gap-3 flex-wrap">
                <div>
                  <h2 className="h5 mb-2">Contexto da semente</h2>
                  <p className="text-secondary mb-0">
                    Origem: Público Geral → {seed.name}. Próximo passo: quebrar
                    em subnichos com dores e linguagem real.
                  </p>
                </div>
                <Link
                  className="btn btn-outline-secondary btn-sm"
                  to="/oprm/general-audiences"
                >
                  Voltar para públicos gerais
                </Link>
              </div>
              <dl className="row mt-4 mb-0">
                <dt className="col-md-3">Tipo</dt>
                <dd className="col-md-9">{seedTypeLabels[seed.seedType]}</dd>
                <dt className="col-md-3">Status</dt>
                <dd className="col-md-9">{seedStatusLabels[seed.status]}</dd>
                <dt className="col-md-3">Mercado</dt>
                <dd className="col-md-9">
                  {seed.marketContext || "Sem contexto informado"}
                </dd>
                <dt className="col-md-3">Objetivo comercial</dt>
                <dd className="col-md-9">
                  {seed.businessGoal || "Sem objetivo informado"}
                </dd>
                <dt className="col-md-3">Riscos</dt>
                <dd className="col-md-9">
                  {seed.riskNotes || "Sem risco informado"}
                </dd>
                <dt className="col-md-3">Atualização</dt>
                <dd className="col-md-9">{formatDateTime(seed.updatedAt)}</dd>
              </dl>
            </div>
          </section>

          <section className="card border-0 shadow-sm">
            <div className="card-body">
              <h2 className="h5 mb-3">Controle operacional</h2>
              <form
                className="d-flex gap-2 align-items-end flex-wrap"
                onSubmit={handleStatusSubmit}
              >
                <div>
                  <label className="form-label" htmlFor="seed-status">
                    Status <span className="text-danger">*</span>
                  </label>
                  <select
                    id="seed-status"
                    className="form-select"
                    value={status}
                    onChange={(event) =>
                      setStatus(
                        event.target.value as OprmGeneralAudienceSeedStatus,
                      )
                    }
                    required
                  >
                    {seedStatusOptions.map((option) => (
                      <option key={option} value={option}>
                        {seedStatusLabels[option]}
                      </option>
                    ))}
                  </select>
                </div>
                <button
                  type="submit"
                  className="btn btn-primary"
                  disabled={updateSeed.isPending}
                >
                  {updateSeed.isPending ? (
                    <span
                      className="spinner-border spinner-border-sm me-2"
                      aria-hidden="true"
                    />
                  ) : null}
                  Salvar status
                </button>
                <button
                  type="button"
                  className="btn btn-outline-danger"
                  onClick={handleArchive}
                  disabled={archiveSeed.isPending || seed.status === "ARCHIVED"}
                >
                  {archiveSeed.isPending ? (
                    <span
                      className="spinner-border spinner-border-sm me-2"
                      aria-hidden="true"
                    />
                  ) : null}
                  Arquivar
                </button>
              </form>
              {updateSeed.isError || archiveSeed.isError ? (
                <div className="alert alert-danger mt-3 mb-0">
                  Não foi possível atualizar a semente.
                </div>
              ) : null}
            </div>
          </section>
        </>
      ) : null}

      <section className="card border-0 shadow-sm">
        <div className="card-body">
          <h2 className="h5 mb-2">Cadastrar subnicho manual</h2>
          <p className="text-secondary">
            O subnicho precisa representar uma pessoa específica, uma dor clara
            e uma forma de confirmar se o lead pertence ao público.
          </p>
          <form className="row g-3" onSubmit={handleSubnicheSubmit}>
            <div className="col-md-4">
              <label className="form-label" htmlFor="subniche-name">
                Nome do subnicho <span className="text-danger">*</span>
              </label>
              <input
                id="subniche-name"
                className="form-control"
                value={subnicheForm.name}
                onChange={(event) =>
                  setSubnicheForm((current) => ({
                    ...current,
                    name: event.target.value,
                  }))
                }
                required
                maxLength={191}
                placeholder="Ex.: manicure autônoma"
              />
            </div>
            <div className="col-md-4">
              <label className="form-label" htmlFor="subniche-persona">
                Persona/contexto
              </label>
              <input
                id="subniche-persona"
                className="form-control"
                value={subnicheForm.personaSummary}
                onChange={(event) =>
                  setSubnicheForm((current) => ({
                    ...current,
                    personaSummary: event.target.value,
                  }))
                }
                placeholder="Quem é e qual rotina importa"
              />
            </div>
            <div className="col-md-4">
              <label className="form-label" htmlFor="subniche-question">
                Pergunta qualificadora
              </label>
              <input
                id="subniche-question"
                className="form-control"
                value={subnicheForm.qualificationQuestion}
                onChange={(event) =>
                  setSubnicheForm((current) => ({
                    ...current,
                    qualificationQuestion: event.target.value,
                  }))
                }
                placeholder="Ex.: Você trabalha como manicure hoje?"
              />
            </div>
            <div className="col-md-6">
              <label className="form-label" htmlFor="subniche-pain">
                Dores recorrentes
              </label>
              <textarea
                id="subniche-pain"
                className="form-control"
                rows={3}
                value={subnicheForm.painSummary}
                onChange={(event) =>
                  setSubnicheForm((current) => ({
                    ...current,
                    painSummary: event.target.value,
                  }))
                }
                placeholder="Agenda vazia, clientes somem, medo de cobrar mais..."
              />
            </div>
            <div className="col-md-6">
              <label className="form-label" htmlFor="subniche-language">
                Linguagem e canais
              </label>
              <textarea
                id="subniche-language"
                className="form-control"
                rows={3}
                value={subnicheForm.languagePatterns}
                onChange={(event) =>
                  setSubnicheForm((current) => ({
                    ...current,
                    languagePatterns: event.target.value,
                    channelsSummary: event.target.value,
                  }))
                }
                placeholder="Como esse público fala da dor e onde aparece"
              />
            </div>
            {createSubniche.isError ? (
              <div className="col-12">
                <div className="alert alert-danger mb-0">
                  Não foi possível criar o subnicho. Revise os campos.
                </div>
              </div>
            ) : null}
            <div className="col-12">
              <button
                type="submit"
                className="btn btn-primary"
                disabled={createSubniche.isPending}
              >
                {createSubniche.isPending ? (
                  <span
                    className="spinner-border spinner-border-sm me-2"
                    aria-hidden="true"
                  />
                ) : null}
                Criar subnicho
              </button>
            </div>
          </form>
        </div>
      </section>

      <section className="card border-0 shadow-sm">
        <div className="card-body">
          <div className="d-flex justify-content-between align-items-start gap-3 mb-3">
            <div>
              <h2 className="h5 mb-1">Subnichos derivados</h2>
              <p className="text-secondary mb-0">
                Só aprove subnichos com persona, dor, canais e pergunta de
                triagem claros.
              </p>
            </div>
            <button
              type="button"
              className="btn btn-outline-secondary btn-sm"
              onClick={() => subnichesQuery.refetch()}
              disabled={subnichesQuery.isFetching}
            >
              {subnichesQuery.isFetching ? (
                <span
                  className="spinner-border spinner-border-sm me-2"
                  aria-hidden="true"
                />
              ) : null}
              Atualizar
            </button>
          </div>
          {subnichesQuery.isLoading ? (
            <div
              className="spinner-border text-primary"
              role="status"
              aria-label="Carregando subnichos"
            />
          ) : null}
          {subnichesQuery.isError ? (
            <div className="alert alert-danger">
              Não foi possível carregar os subnichos desta semente.
            </div>
          ) : null}
          {!subnichesQuery.isLoading && !subnichesQuery.isError ? (
            <div className="table-responsive">
              <table className="table table-sm align-middle mb-0">
                <thead>
                  <tr>
                    <th>Subnicho</th>
                    <th>Status</th>
                    <th>Dor</th>
                    <th>Pergunta</th>
                    <th>Score</th>
                    <th>Ação</th>
                  </tr>
                </thead>
                <tbody>
                  {(subnichesQuery.data ?? []).length > 0 ? (
                    (subnichesQuery.data ?? []).map((subniche) => (
                      <tr key={subniche.id}>
                        <td>{subniche.name}</td>
                        <td>{subnicheStatusLabels[subniche.status]}</td>
                        <td className="text-secondary">
                          {subniche.painSummary || "Dor pendente"}
                        </td>
                        <td className="text-secondary">
                          {subniche.qualificationQuestion ||
                            "Pergunta pendente"}
                        </td>
                        <td>{scoreLabel(subniche.opportunityScore)}</td>
                        <td>
                          <Link
                            className="btn btn-sm btn-outline-primary"
                            to={`/oprm/general-audiences/subniches/${subniche.id}`}
                          >
                            Revisar
                          </Link>
                        </td>
                      </tr>
                    ))
                  ) : (
                    <tr>
                      <td colSpan={6} className="text-secondary">
                        Nenhum subnicho cadastrado ainda.
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
