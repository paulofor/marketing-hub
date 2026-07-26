import { FormEvent, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import PageTitle from "../../components/PageTitle";
import {
  productDiscoveryStatusLabels,
  useCreateProductDiscoveryCycle,
  useProductDiscoveryCycles,
} from "../../api/productDiscovery/useProductDiscovery";

function formatDateTime(value: string | undefined) {
  if (!value) {
    return "-";
  }
  return new Date(value).toLocaleString("pt-BR");
}

export default function ProductDiscoveryPage() {
  const cyclesQuery = useProductDiscoveryCycles();
  const createCycle = useCreateProductDiscoveryCycle();
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
