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
import {
  useCreateMoisMetaAdInvestigation,
  useGenerateMoisCreativeBrief,
  useMoisMetaAdInvestigations,
  useRegisterSupervisedMetaAdObservation,
} from "../../api/mois/useMoisMetaAdInvestigations";

const WORKSPACE_ID = "workspace-001";
const SOURCE_OPTIONS = [
  { value: "CLICKBANK", available: false },
  { value: "HOTMART", available: true },
  { value: "JVZOO", available: false },
  { value: "META_AD_LIBRARY", available: false },
];

export default function MoisAutoCollectionPage() {
  const [selectedSources, setSelectedSources] = useState<string[]>(["HOTMART"]);
  const [niche, setNiche] = useState("nutricao");
  const [marketTheme, setMarketTheme] = useState("perda de gordura");
  const [timeWindow, setTimeWindow] = useState<"LAST_7_DAYS" | "LAST_30_DAYS">(
    "LAST_7_DAYS",
  );
  const [minSuccessScore, setMinSuccessScore] = useState(60);
  const [activeJobId, setActiveJobId] = useState("");
  const [metaSearchTerms, setMetaSearchTerms] = useState("");
  const [metaInvestigationPlatform, setMetaInvestigationPlatform] = useState<
    "INSTAGRAM" | "FACEBOOK"
  >("INSTAGRAM");
  const [selectedMetaInvestigationId, setSelectedMetaInvestigationId] =
    useState(0);
  const [metaAdReference, setMetaAdReference] = useState("");
  const [metaAdvertiserName, setMetaAdvertiserName] = useState("");
  const [metaLibraryUrl, setMetaLibraryUrl] = useState("");
  const [metaAdText, setMetaAdText] = useState("");
  const [metaObservedPlatform, setMetaObservedPlatform] = useState<
    "INSTAGRAM" | "FACEBOOK"
  >("INSTAGRAM");
  const [metaDestinationUrl, setMetaDestinationUrl] = useState("");
  const [metaPageActive, setMetaPageActive] = useState(false);
  const [metaCommercialSignal, setMetaCommercialSignal] = useState(false);

  const [sourceFilter, setSourceFilter] = useState("");
  const [confidenceFilter, setConfidenceFilter] = useState("");
  const [selectedLineageReferenceId, setSelectedLineageReferenceId] =
    useState("");

  const createJob = useCreateMoisCollectionJob();
  const favoriteMutation = useFavoriteMoisCollectedReference();
  const discardMutation = useDiscardMoisCollectedReference();
  const importMutation = useImportMoisCollectedReference();
  const importAndExtractMutation =
    useImportAndStartExtractionMoisCollectedReference();
  const metaInvestigations = useMoisMetaAdInvestigations(WORKSPACE_ID);
  const createMetaInvestigation = useCreateMoisMetaAdInvestigation();
  const registerMetaObservation = useRegisterSupervisedMetaAdObservation();
  const generateCreativeBrief = useGenerateMoisCreativeBrief();

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
  const lineageQuery = useMoisCollectedReferenceLineage(
    activeJobId,
    selectedLineageReferenceId,
  );

  function toggleSource(source: string) {
    setSelectedSources((prev) =>
      prev.includes(source)
        ? prev.filter((item) => item !== source)
        : [...prev, source],
    );
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
            Inicie coletas por janela temporal e aplique ações rápidas em
            referências com maior sinal de sucesso.
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
              onChange={(event) =>
                setTimeWindow(
                  event.target.value as "LAST_7_DAYS" | "LAST_30_DAYS",
                )
              }
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
              onChange={(event) =>
                setMinSuccessScore(Number(event.target.value))
              }
              required
            />
          </div>

          <div className="col-12">
            <label className="form-label d-block">Fontes *</label>
            <div className="d-flex flex-wrap gap-2">
              {SOURCE_OPTIONS.map((source) => (
                <label
                  key={source.value}
                  className={`btn btn-outline-primary btn-sm ${source.available ? "" : "disabled"}`}
                >
                  <input
                    type="checkbox"
                    className="form-check-input me-2"
                    checked={selectedSources.includes(source.value)}
                    disabled={!source.available}
                    onChange={() => toggleSource(source.value)}
                  />
                  {source.value} {!source.available ? "· em implantação" : ""}
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
            {activeJobId ? (
              <span className="badge text-bg-light">Job {activeJobId}</span>
            ) : null}
          </div>

          <div className="row g-2 mb-3">
            <div className="col-md-4">
              <select
                className="form-select"
                value={sourceFilter}
                onChange={(event) => setSourceFilter(event.target.value)}
              >
                <option value="">Todas as fontes</option>
                {SOURCE_OPTIONS.filter((source) => source.available).map(
                  (source) => (
                    <option key={source.value} value={source.value}>
                      {source.value}
                    </option>
                  ),
                )}
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

          {!activeJobId ? (
            <p className="text-secondary mb-0">
              Crie um job para visualizar resultados.
            </p>
          ) : null}
          {referencesQuery.isLoading ? (
            <p className="text-secondary mb-0">Carregando resultados...</p>
          ) : null}
          {referencesQuery.isError ? (
            <p className="text-danger mb-0">
              Falha ao carregar resultados da coleta.
            </p>
          ) : null}
          {referencesQuery.data && referencesQuery.data.length === 0 ? (
            <p className="text-secondary mb-0">
              Nenhuma referência encontrada com os filtros atuais.
            </p>
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
                      favoriteMutation.isPending ||
                      discardMutation.isPending ||
                      importMutation.isPending ||
                      importAndExtractMutation.isPending;
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
                          <span className="badge text-bg-light">
                            {item.status}
                          </span>
                        </td>
                        <td>{item.successScore}</td>
                        <td>{item.source}</td>
                        <td>
                          {new Date(item.collectedAt).toLocaleString("pt-BR")}
                        </td>
                        <td className="d-flex gap-2">
                          <button
                            type="button"
                            className="btn btn-outline-primary btn-sm"
                            disabled={isActing || item.status === "DISCARDED"}
                            onClick={() =>
                              favoriteMutation.mutate({
                                jobId: item.jobId,
                                referenceId: item.referenceId,
                              })
                            }
                          >
                            {favoriteMutation.isPending ? (
                              <span className="spinner-border spinner-border-sm" />
                            ) : (
                              "Favoritar"
                            )}
                          </button>
                          <button
                            type="button"
                            className="btn btn-outline-danger btn-sm"
                            disabled={isActing || item.status === "DISCARDED"}
                            onClick={() =>
                              discardMutation.mutate({
                                jobId: item.jobId,
                                referenceId: item.referenceId,
                              })
                            }
                          >
                            {discardMutation.isPending ? (
                              <span className="spinner-border spinner-border-sm" />
                            ) : (
                              "Descartar"
                            )}
                          </button>
                          <button
                            type="button"
                            className="btn btn-success btn-sm"
                            disabled={isActing || item.status === "IMPORTED"}
                            onClick={() =>
                              importMutation.mutate({
                                jobId: item.jobId,
                                referenceId: item.referenceId,
                              })
                            }
                          >
                            {importMutation.isPending ? (
                              <span className="spinner-border spinner-border-sm" />
                            ) : (
                              "Importar"
                            )}
                          </button>
                          <button
                            type="button"
                            className="btn btn-primary btn-sm"
                            disabled={isActing || item.status === "IMPORTED"}
                            onClick={() =>
                              importAndExtractMutation.mutate({
                                jobId: item.jobId,
                                referenceId: item.referenceId,
                              })
                            }
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
                            onClick={() =>
                              setSelectedLineageReferenceId(item.referenceId)
                            }
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
              {lineageQuery.isLoading ? (
                <p className="text-secondary mb-0">Carregando lineage...</p>
              ) : null}
              {lineageQuery.isError ? (
                <p className="text-danger mb-0">
                  Não foi possível carregar o lineage.
                </p>
              ) : null}
              {lineageQuery.data ? (
                <ul className="mb-0">
                  <li>Fonte original: {lineageQuery.data.sourceUrl}</li>
                  <li>
                    Referência importada:{" "}
                    {lineageQuery.data.importedReferenceId ?? "—"}
                  </li>
                  <li>
                    Extração iniciada: {lineageQuery.data.extractionId ?? "—"}
                  </li>
                  <li>
                    Blocos de biblioteca:{" "}
                    {lineageQuery.data.generatedLibraryBlockIds.join(", ") ||
                      "—"}
                  </li>
                </ul>
              ) : null}
            </div>
          ) : null}
        </div>
      </article>

      <article className="card shadow-sm">
        <div className="card-body d-flex flex-column gap-3">
          <div>
            <h2 className="h5 mb-1">
              Radar supervisionado de anúncios comerciais
            </h2>
            <p className="text-secondary mb-0">
              No Brasil, registre anúncios reais pela Biblioteca pública e
              reobserve os mesmos IDs para comprovar longevidade. A modelagem só
              é liberada após sinais comprovados.
            </p>
            <div className="alert alert-info small mt-3 mb-0" role="status">
              A API oficial não fornece anúncios comerciais gerais do Brasil.
              Por isso, o Marketing Hub usa observação supervisionada, sem
              raspagem ou evidência fabricada.
            </div>
          </div>
          <form
            className="row g-2"
            onSubmit={async (event) => {
              event.preventDefault();
              await createMetaInvestigation.mutateAsync({
                workspaceId: WORKSPACE_ID,
                searchTerms: metaSearchTerms,
                countryCode: "BR",
                publisherPlatform: metaInvestigationPlatform,
              });
              setMetaSearchTerms("");
            }}
          >
            <div className="col-md-7">
              <input
                className="form-control"
                value={metaSearchTerms}
                onChange={(event) => setMetaSearchTerms(event.target.value)}
                placeholder="Produto, dor ou promessa a investigar"
                required
              />
            </div>
            <div className="col-md-2">
              <label className="visually-hidden" htmlFor="meta-platform">
                Plataforma
              </label>
              <select
                id="meta-platform"
                className="form-select"
                value={metaInvestigationPlatform}
                onChange={(event) =>
                  setMetaInvestigationPlatform(
                    event.target.value as "INSTAGRAM" | "FACEBOOK",
                  )
                }
              >
                <option value="INSTAGRAM">Instagram</option>
                <option value="FACEBOOK">Facebook</option>
              </select>
            </div>
            <div className="col-md-3 d-grid">
              <button
                className="btn btn-primary"
                disabled={createMetaInvestigation.isPending}
              >
                {createMetaInvestigation.isPending
                  ? "Criando..."
                  : "Criar acompanhamento"}
              </button>
            </div>
          </form>
          {(metaInvestigations.data ?? []).length > 0 ? (
            <form
              className="border rounded p-3 row g-3"
              onSubmit={async (event) => {
                event.preventDefault();
                await registerMetaObservation.mutateAsync({
                  investigationId: selectedMetaInvestigationId,
                  observation: {
                    adReference: metaAdReference,
                    advertiserName: metaAdvertiserName,
                    adLibraryUrl: metaLibraryUrl,
                    adText: metaAdText,
                    publisherPlatforms: [metaObservedPlatform],
                    destinationUrl: metaDestinationUrl || undefined,
                    pageActive: metaPageActive,
                    commercialSignal: metaCommercialSignal,
                  },
                });
                setMetaAdReference("");
                setMetaAdvertiserName("");
                setMetaLibraryUrl("");
                setMetaAdText("");
                setMetaDestinationUrl("");
                setMetaPageActive(false);
                setMetaCommercialSignal(false);
              }}
            >
              <div className="col-12">
                <h3 className="h6 mb-1">Complementar com observação real</h3>
                <p className="small text-secondary mb-0">
                  Abra o anúncio na Meta e transcreva apenas o que estiver
                  visível. Uma nova observação do mesmo ID atualiza a
                  longevidade sem duplicar o ativo.
                </p>
              </div>
              <div className="col-md-6">
                <label className="form-label" htmlFor="meta-investigation">
                  Acompanhamento *
                </label>
                <select
                  id="meta-investigation"
                  className="form-select"
                  value={selectedMetaInvestigationId}
                  onChange={(event) =>
                    setSelectedMetaInvestigationId(Number(event.target.value))
                  }
                  required
                >
                  <option value={0} disabled>
                    Selecione
                  </option>
                  {(metaInvestigations.data ?? []).map((item) => (
                    <option key={item.id} value={item.id}>
                      #{item.id} · {item.searchTerms}
                    </option>
                  ))}
                </select>
              </div>
              <div className="col-md-6">
                <label className="form-label" htmlFor="meta-ad-reference">
                  ID do anúncio *
                </label>
                <input
                  id="meta-ad-reference"
                  className="form-control"
                  value={metaAdReference}
                  onChange={(event) => setMetaAdReference(event.target.value)}
                  required
                />
              </div>
              <div className="col-md-6">
                <label className="form-label" htmlFor="meta-observed-platform">
                  Plataforma observada *
                </label>
                <select
                  id="meta-observed-platform"
                  className="form-select"
                  value={metaObservedPlatform}
                  onChange={(event) =>
                    setMetaObservedPlatform(
                      event.target.value as "INSTAGRAM" | "FACEBOOK",
                    )
                  }
                  required
                >
                  <option value="INSTAGRAM">Instagram</option>
                  <option value="FACEBOOK">Facebook</option>
                </select>
              </div>
              <div className="col-md-6">
                <label className="form-label" htmlFor="meta-advertiser">
                  Anunciante *
                </label>
                <input
                  id="meta-advertiser"
                  className="form-control"
                  value={metaAdvertiserName}
                  onChange={(event) =>
                    setMetaAdvertiserName(event.target.value)
                  }
                  required
                />
              </div>
              <div className="col-md-6">
                <label className="form-label" htmlFor="meta-library-url">
                  URL da Biblioteca *
                </label>
                <input
                  id="meta-library-url"
                  type="url"
                  className="form-control"
                  value={metaLibraryUrl}
                  onChange={(event) => setMetaLibraryUrl(event.target.value)}
                  placeholder="https://www.facebook.com/ads/library/..."
                  required
                />
              </div>
              <div className="col-12">
                <label className="form-label" htmlFor="meta-ad-text">
                  Texto principal visível *
                </label>
                <textarea
                  id="meta-ad-text"
                  className="form-control"
                  rows={3}
                  value={metaAdText}
                  onChange={(event) => setMetaAdText(event.target.value)}
                  required
                />
              </div>
              <div className="col-12">
                <label className="form-label" htmlFor="meta-destination-url">
                  Página de destino
                </label>
                <input
                  id="meta-destination-url"
                  type="url"
                  className="form-control"
                  value={metaDestinationUrl}
                  onChange={(event) =>
                    setMetaDestinationUrl(event.target.value)
                  }
                />
              </div>
              <div className="col-md-6 form-check ms-2">
                <input
                  id="meta-page-active"
                  type="checkbox"
                  className="form-check-input"
                  checked={metaPageActive}
                  onChange={(event) => setMetaPageActive(event.target.checked)}
                />
                <label className="form-check-label" htmlFor="meta-page-active">
                  Página abriu e está ativa
                </label>
              </div>
              <div className="col-md-5 form-check ms-2">
                <input
                  id="meta-commercial-signal"
                  type="checkbox"
                  className="form-check-input"
                  checked={metaCommercialSignal}
                  onChange={(event) =>
                    setMetaCommercialSignal(event.target.checked)
                  }
                />
                <label
                  className="form-check-label"
                  htmlFor="meta-commercial-signal"
                >
                  Há preço, oferta ou checkout verificável
                </label>
              </div>
              <div className="col-12 d-flex justify-content-end">
                <button
                  className="btn btn-primary"
                  disabled={
                    registerMetaObservation.isPending ||
                    selectedMetaInvestigationId === 0
                  }
                >
                  {registerMetaObservation.isPending
                    ? "Registrando..."
                    : "Registrar observação"}
                </button>
              </div>
            </form>
          ) : null}
          {metaInvestigations.isError ? (
            <p className="text-danger mb-0">
              Falha ao carregar investigações Meta.
            </p>
          ) : null}
          {(metaInvestigations.data ?? []).map((investigation) => (
            <div key={investigation.id} className="border rounded p-3">
              <div className="d-flex flex-wrap justify-content-between gap-2">
                <strong>{investigation.searchTerms}</strong>
                <span
                  className={`badge ${investigation.gateDecision === "MODELAR" ? "text-bg-success" : investigation.gateDecision === "DESCARTAR" ? "text-bg-danger" : "text-bg-warning"}`}
                >
                  {investigation.gateDecision}
                </span>
              </div>
              <div className="small text-secondary mb-2">
                {investigation.publisherPlatform} · {investigation.status} ·{" "}
                {investigation.adsObserved} observações auditáveis
              </div>
              <div className="alert alert-light border small py-2">
                <strong>
                  {investigation.collection.mode === "OFFICIAL_API"
                    ? "Coleta pela API oficial."
                    : "Coleta supervisionada."}
                </strong>{" "}
                {investigation.collection.reason} Próximo objetivo:{" "}
                {new Date(
                  investigation.collection.nextObservationAt,
                ).toLocaleDateString("pt-BR")}
                .{" "}
                <a
                  href={investigation.collection.searchUrl}
                  target="_blank"
                  rel="noreferrer"
                >
                  Abrir busca oficial
                </a>
              </div>
              <div className="row g-3">
                <div className="col-md-6">
                  <div className="fw-semibold small">Evidências</div>
                  <ul className="small mb-0">
                    {investigation.evidences.map((item) => (
                      <li key={item}>{item}</li>
                    ))}
                  </ul>
                </div>
                <div className="col-md-6">
                  <div className="fw-semibold small">Lacunas</div>
                  <ul className="small mb-0">
                    {investigation.gaps.map((item) => (
                      <li key={item}>{item}</li>
                    ))}
                  </ul>
                </div>
              </div>
              {investigation.gateDecision === "MODELAR" ? (
                <div className="mt-3 d-flex flex-column gap-2">
                  <div className="alert alert-success mb-0 small">
                    <strong>Ficha ética:</strong>{" "}
                    {investigation.ethicalModeling.pain}. Não copiar{" "}
                    {investigation.ethicalModeling.prohibitedCopies.join(", ")}.
                  </div>
                  {investigation.creativeBrief.status === "UNAVAILABLE" ? (
                    <button
                      type="button"
                      className="btn btn-outline-primary align-self-start"
                      disabled={generateCreativeBrief.isPending}
                      onClick={() =>
                        generateCreativeBrief.mutate(investigation.id)
                      }
                    >
                      {generateCreativeBrief.isPending
                        ? "Gerando briefing..."
                        : "Gerar briefing original"}
                    </button>
                  ) : (
                    <div className="card bg-light border-0">
                      <div className="card-body small">
                        <div className="d-flex justify-content-between gap-2 mb-2">
                          <strong>{investigation.creativeBrief.title}</strong>
                          <span className="badge text-bg-warning">
                            Exige Aprovador
                          </span>
                        </div>
                        <p>
                          <strong>Gancho:</strong>{" "}
                          {investigation.creativeBrief.originalHook}
                        </p>
                        <p>
                          <strong>Visual:</strong>{" "}
                          {investigation.creativeBrief.visualDirection}
                        </p>
                        <p>
                          <strong>Oferta:</strong>{" "}
                          {investigation.creativeBrief.offerAngle}
                        </p>
                        <p className="mb-0">
                          <strong>CTA:</strong>{" "}
                          {investigation.creativeBrief.callToAction}
                        </p>
                      </div>
                    </div>
                  )}
                </div>
              ) : null}
            </div>
          ))}
        </div>
      </article>
    </section>
  );
}
