import { FormEvent, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import {
  useCreateMoisCollectionJob,
  useDiscardMoisCollectedReference,
  useFavoriteMoisCollectedReference,
  useImportAndStartExtractionMoisCollectedReference,
  useImportMoisCollectedReference,
  useMoisCollectedReferenceLineage,
  useMoisCollectedReferences,
} from "../../api/mois/useMoisCollection";

const WORKSPACE_ID = "workspace-001";
const SOURCE_OPTIONS = ["CLICKBANK", "JVZOO", "HOTMART", "META_AD_LIBRARY"];

export default function MoisAutoCollectionPage() {
  const [selectedSources, setSelectedSources] = useState<string[]>(["CLICKBANK"]);
  const [niche, setNiche] = useState("nutricao");
  const [marketTheme, setMarketTheme] = useState("perda de gordura");
  const [timeWindow, setTimeWindow] = useState<"LAST_7_DAYS" | "LAST_30_DAYS">("LAST_7_DAYS");
  const [minSuccessScore, setMinSuccessScore] = useState(60);
  const [activeJobId, setActiveJobId] = useState("");

  const [sourceFilter, setSourceFilter] = useState("");
  const [confidenceFilter, setConfidenceFilter] = useState("");
  const [selectedLineageReferenceId, setSelectedLineageReferenceId] = useState("");

  const createJob = useCreateMoisCollectionJob();
  const favoriteMutation = useFavoriteMoisCollectedReference();
  const discardMutation = useDiscardMoisCollectedReference();
  const importMutation = useImportMoisCollectedReference();
  const importAndExtractMutation = useImportAndStartExtractionMoisCollectedReference();

  const filters = useMemo(
    () => ({
      source: sourceFilter || undefined,
      niche: niche || undefined,
      minSuccessScore,
      confidenceLevel: confidenceFilter || undefined,
    }),
    [confidenceFilter, minSuccessScore, niche, sourceFilter],
  );

  const referencesQuery = useMoisCollectedReferences(activeJobId, filters);
  const lineageQuery = useMoisCollectedReferenceLineage(activeJobId, selectedLineageReferenceId);

  function toggleSource(source: string) {
    setSelectedSources((prev) => (prev.includes(source) ? prev.filter((item) => item !== source) : [...prev, source]));
  }

  async function handleCreateJob(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const job = await createJob.mutateAsync({
      workspaceId: WORKSPACE_ID,
      niche,
      marketTheme,
      sources: selectedSources,
      timeWindow,
      minSuccessScore,
      locale: "pt-BR",
      country: "BR",
    });
    setActiveJobId(job.jobId);
  }

  return (
    <section className="d-flex flex-column gap-4">
      <header className="d-flex justify-content-between align-items-center">
        <div>
          <h1 className="h3 mb-1">MOIS · Coleta automática</h1>
          <p className="text-secondary mb-0">
            Inicie coletas por janela temporal e aplique ações rápidas em referências com maior sinal de sucesso.
          </p>
        </div>
        <Link className="btn btn-outline-secondary" to="/mois">
          Voltar ao workspace
        </Link>
      </header>

      <form className="card shadow-sm" onSubmit={handleCreateJob}>
        <div className="card-body row g-3">
          <div className="col-md-6">
            <label className="form-label" htmlFor="mois-auto-niche">
              Nicho *
            </label>
            <input
              id="mois-auto-niche"
              className="form-control"
              value={niche}
              onChange={(event) => setNiche(event.target.value)}
              required
            />
          </div>
          <div className="col-md-6">
            <label className="form-label" htmlFor="mois-auto-theme">
              Tema *
            </label>
            <input
              id="mois-auto-theme"
              className="form-control"
              value={marketTheme}
              onChange={(event) => setMarketTheme(event.target.value)}
              required
            />
          </div>

          <div className="col-md-6">
            <label className="form-label" htmlFor="mois-auto-window">
              Janela temporal *
            </label>
            <select
              id="mois-auto-window"
              className="form-select"
              value={timeWindow}
              onChange={(event) => setTimeWindow(event.target.value as "LAST_7_DAYS" | "LAST_30_DAYS")}
              required
            >
              <option value="LAST_7_DAYS">Últimos 7 dias</option>
              <option value="LAST_30_DAYS">Últimos 30 dias</option>
            </select>
          </div>
          <div className="col-md-6">
            <label className="form-label" htmlFor="mois-auto-min-score">
              Score mínimo *
            </label>
            <input
              id="mois-auto-min-score"
              type="number"
              min={0}
              max={100}
              className="form-control"
              value={minSuccessScore}
              onChange={(event) => setMinSuccessScore(Number(event.target.value))}
              required
            />
          </div>

          <div className="col-12">
            <label className="form-label d-block">Fontes *</label>
            <div className="d-flex flex-wrap gap-2">
              {SOURCE_OPTIONS.map((source) => (
                <label key={source} className="btn btn-outline-primary btn-sm">
                  <input
                    type="checkbox"
                    className="form-check-input me-2"
                    checked={selectedSources.includes(source)}
                    onChange={() => toggleSource(source)}
                  />
                  {source}
                </label>
              ))}
            </div>
          </div>

          <div className="col-12 d-flex justify-content-end">
            <button
              type="submit"
              className="btn btn-primary"
              disabled={createJob.isPending || selectedSources.length === 0}
            >
              {createJob.isPending ? (
                <>
                  <span className="spinner-border spinner-border-sm me-2" />
                  Iniciando coleta...
                </>
              ) : (
                "Iniciar coleta"
              )}
            </button>
          </div>
        </div>
      </form>

      <article className="card shadow-sm">
        <div className="card-body">
          <div className="d-flex justify-content-between align-items-center mb-3">
            <h2 className="h5 mb-0">Resultados da coleta</h2>
            {activeJobId ? <span className="badge text-bg-light">Job {activeJobId}</span> : null}
          </div>

          <div className="row g-2 mb-3">
            <div className="col-md-4">
              <select className="form-select" value={sourceFilter} onChange={(event) => setSourceFilter(event.target.value)}>
                <option value="">Todas as fontes</option>
                {SOURCE_OPTIONS.map((source) => (
                  <option key={source} value={source}>
                    {source}
                  </option>
                ))}
              </select>
            </div>
            <div className="col-md-4">
              <select
                className="form-select"
                value={confidenceFilter}
                onChange={(event) => setConfidenceFilter(event.target.value)}
              >
                <option value="">Todas as confianças</option>
                <option value="HIGH">HIGH</option>
                <option value="MEDIUM">MEDIUM</option>
                <option value="LOW">LOW</option>
              </select>
            </div>
          </div>

          {!activeJobId ? <p className="text-secondary mb-0">Crie um job para visualizar resultados.</p> : null}
          {referencesQuery.isLoading ? <p className="text-secondary mb-0">Carregando resultados...</p> : null}
          {referencesQuery.isError ? <p className="text-danger mb-0">Falha ao carregar resultados da coleta.</p> : null}
          {referencesQuery.data && referencesQuery.data.length === 0 ? (
            <p className="text-secondary mb-0">Nenhuma referência encontrada com os filtros atuais.</p>
          ) : null}

          {referencesQuery.data && referencesQuery.data.length > 0 ? (
            <div className="table-responsive">
              <table className="table table-sm align-middle">
                <thead>
                  <tr>
                    <th>Título</th>
                    <th>URL do produto</th>
                    <th>Status</th>
                    <th>Score</th>
                    <th>Origem</th>
                    <th>Data</th>
                    <th>Ações</th>
                  </tr>
                </thead>
                <tbody>
                  {referencesQuery.data.map((item) => {
                    const isActing =
                      favoriteMutation.isPending || discardMutation.isPending || importMutation.isPending || importAndExtractMutation.isPending;
                    return (
                      <tr key={item.referenceId}>
                        <td>
                          <div className="fw-semibold">{item.title}</div>
                          <a href={item.url} target="_blank" rel="noreferrer">
                            Abrir fonte
                          </a>
                        </td>
                        <td>
                          <div className="small text-break">{item.url}</div>
                        </td>
                        <td>
                          <span className="badge text-bg-light">{item.status}</span>
                        </td>
                        <td>{item.successScore}</td>
                        <td>{item.source}</td>
                        <td>{new Date(item.collectedAt).toLocaleString("pt-BR")}</td>
                        <td className="d-flex gap-2">
                          <button
                            type="button"
                            className="btn btn-outline-primary btn-sm"
                            disabled={isActing || item.status === "DISCARDED"}
                            onClick={() => favoriteMutation.mutate({ jobId: item.jobId, referenceId: item.referenceId })}
                          >
                            {favoriteMutation.isPending ? <span className="spinner-border spinner-border-sm" /> : "Favoritar"}
                          </button>
                          <button
                            type="button"
                            className="btn btn-outline-danger btn-sm"
                            disabled={isActing || item.status === "DISCARDED"}
                            onClick={() => discardMutation.mutate({ jobId: item.jobId, referenceId: item.referenceId })}
                          >
                            {discardMutation.isPending ? <span className="spinner-border spinner-border-sm" /> : "Descartar"}
                          </button>
                          <button
                            type="button"
                            className="btn btn-success btn-sm"
                            disabled={isActing || item.status === "IMPORTED"}
                            onClick={() => importMutation.mutate({ jobId: item.jobId, referenceId: item.referenceId })}
                          >
                            {importMutation.isPending ? <span className="spinner-border spinner-border-sm" /> : "Importar"}
                          </button>
                          <button
                            type="button"
                            className="btn btn-primary btn-sm"
                            disabled={isActing || item.status === "IMPORTED"}
                            onClick={() => importAndExtractMutation.mutate({ jobId: item.jobId, referenceId: item.referenceId })}
                          >
                            {importAndExtractMutation.isPending ? (
                              <span className="spinner-border spinner-border-sm" />
                            ) : (
                              "Importar + Extração"
                            )}
                          </button>
                          <button
                            type="button"
                            className="btn btn-outline-secondary btn-sm"
                            onClick={() => setSelectedLineageReferenceId(item.referenceId)}
                          >
                            Ver lineage
                          </button>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          ) : null}

          {selectedLineageReferenceId ? (
            <div className="border rounded p-3 mt-3">
              <h3 className="h6 mb-2">Lineage da referência</h3>
              {lineageQuery.isLoading ? <p className="text-secondary mb-0">Carregando lineage...</p> : null}
              {lineageQuery.isError ? <p className="text-danger mb-0">Não foi possível carregar o lineage.</p> : null}
              {lineageQuery.data ? (
                <ul className="mb-0">
                  <li>Fonte original: {lineageQuery.data.sourceUrl}</li>
                  <li>Referência importada: {lineageQuery.data.importedReferenceId ?? "—"}</li>
                  <li>Extração iniciada: {lineageQuery.data.extractionId ?? "—"}</li>
                  <li>Blocos de biblioteca: {lineageQuery.data.generatedLibraryBlockIds.join(", ") || "—"}</li>
                </ul>
              ) : null}
            </div>
          ) : null}
        </div>
      </article>
    </section>
  );
}
