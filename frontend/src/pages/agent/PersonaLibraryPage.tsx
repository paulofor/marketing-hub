import { FormEvent, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import { Link } from "react-router-dom";
import CodexExecutionTelemetry from "../../components/CodexExecutionTelemetry";

type Persona = {
  id: number;
  name: string;
  personaKey: string;
  confidenceLevel: string;
  pain: string;
  desiredProgress: string;
  evidenceJson: string;
};

type DigitalObservation = {
  id: number;
  persona: Persona;
  objective: string;
  authorizedSourcesJson: string;
  status: string;
  observationJson?: string;
  simulatedReactionJson?: string;
  commercialHypothesisJson?: string;
  humanConfirmationJson?: string;
  createdAt: string;
};

type CustomerEvaluation = {
  id: number;
  persona: Persona;
  assetType: string;
  assetReference: string;
  simulationVersion: "BASELINE_V1" | "BEHAVIORAL_V1" | "BEHAVIORAL_V2";
  status: string;
  simulatedAssessment?: string;
  hypothesisJson?: string;
  baselineResultJson?: string;
  behavioralResultJson?: string;
  humanResultJson?: string;
  lastError?: string;
  retryCount: number;
  createdAt: string;
};

type MemoryEvidence = {
  id: number;
  personaId: number;
  memoryLayer: string;
  sourceUrl?: string;
  contentType: string;
  sizeBytes: number;
  sha256: string;
  retentionUntil: string;
  createdAt: string;
};

/** Exibe um resumo da falha e preserva o diagnóstico técnico completo sob demanda. */
export function EvaluationFailureDetails({ error }: { error: string }) {
  const summary = error.split("\n")[0];

  return (
    <div className="alert alert-danger py-2 mt-2 mb-0" role="alert">
      <strong>Falha técnica:</strong> {summary}
      <details className="mt-2">
        <summary className="fw-semibold">Ver detalhes técnicos</summary>
        <pre
          className="small bg-body-tertiary border rounded p-2 mt-2 mb-0"
          style={{ overflowWrap: "anywhere", whiteSpace: "pre-wrap" }}
        >
          {error}
        </pre>
      </details>
    </div>
  );
}

export default function PersonaLibraryPage() {
  const client = useQueryClient();
  const [name, setName] = useState("");
  const [pain, setPain] = useState("");
  const [progress, setProgress] = useState("");
  const [evidence, setEvidence] = useState("");
  const [observationPersonaId, setObservationPersonaId] = useState("");
  const [observationObjective, setObservationObjective] = useState("");
  const [observationSources, setObservationSources] = useState("");
  const [evaluationPersonaId, setEvaluationPersonaId] = useState("");
  const [evaluationAssetType, setEvaluationAssetType] = useState("PAGE");
  const [evaluationAssetReference, setEvaluationAssetReference] = useState("");
  const [simulationVersion, setSimulationVersion] = useState<
    "BASELINE_V1" | "BEHAVIORAL_V1" | "BEHAVIORAL_V2"
  >("BEHAVIORAL_V2");
  const [evidencePersonaId, setEvidencePersonaId] = useState("");
  const [memoryLayer, setMemoryLayer] = useState("EXTERNAL_OBSERVATION");
  const [memorySourceUrl, setMemorySourceUrl] = useState("");
  const [memoryFile, setMemoryFile] = useState<File | null>(null);
  const personas = useQuery({
    queryKey: ["customer-personas"],
    queryFn: async () =>
      (await axios.get<Persona[]>("/api/customer-agent/v1/personas")).data,
  });
  const create = useMutation({
    mutationFn: async () =>
      axios.post("/api/customer-agent/v1/personas", {
        personaKey: `persona-${Date.now()}`,
        name,
        confidenceLevel: "HIPOTESE",
        lifeContext: "Contexto inicial a aprofundar com evidências humanas.",
        pain,
        desiredProgress: progress,
        evidenceJson: JSON.stringify([{ source: evidence }]),
      }),
    onSuccess: async () => {
      setName("");
      setPain("");
      setProgress("");
      setEvidence("");
      await client.invalidateQueries({ queryKey: ["customer-personas"] });
    },
  });
  const observations = useQuery({
    queryKey: ["customer-digital-observations"],
    queryFn: async () =>
      (
        await axios.get<DigitalObservation[]>(
          "/api/customer-agent/v1/digital-observations",
        )
      ).data,
  });
  const evaluations = useQuery({
    queryKey: ["customer-agent-evaluations"],
    queryFn: async () =>
      (
        await axios.get<CustomerEvaluation[]>(
          "/api/customer-agent/v1/evaluations",
        )
      ).data,
  });
  const createEvaluation = useMutation({
    mutationFn: async () =>
      axios.post("/api/customer-agent/v1/evaluations", {
        personaId: Number(evaluationPersonaId),
        assetType: evaluationAssetType,
        assetReference: evaluationAssetReference,
        simulationVersion,
      }),
    onSuccess: async () => {
      setEvaluationAssetReference("");
      await client.invalidateQueries({
        queryKey: ["customer-agent-evaluations"],
      });
    },
  });
  const retryEvaluation = useMutation({
    mutationFn: async (id: number) =>
      axios.post(`/api/customer-agent/v1/evaluations/${id}/retry`),
    onSuccess: async () => {
      await client.invalidateQueries({
        queryKey: ["customer-agent-evaluations"],
      });
    },
  });
  const createObservation = useMutation({
    mutationFn: async () =>
      axios.post("/api/customer-agent/v1/digital-observations", {
        personaId: Number(observationPersonaId),
        objective: observationObjective,
        authorizedSourcesJson: JSON.stringify(
          observationSources
            .split("\n")
            .map((source) => source.trim())
            .filter(Boolean),
        ),
        deviceProfile: "MOBILE",
      }),
    onSuccess: async () => {
      setObservationObjective("");
      setObservationSources("");
      await client.invalidateQueries({
        queryKey: ["customer-digital-observations"],
      });
    },
  });
  const memoryEvidence = useQuery({
    queryKey: ["customer-agent-memory-evidence", evidencePersonaId],
    enabled: Boolean(evidencePersonaId),
    queryFn: async () =>
      (
        await axios.get<MemoryEvidence[]>(
          `/api/customer-agent/v1/personas/${evidencePersonaId}/memory-evidence`,
        )
      ).data,
  });
  const uploadMemoryEvidence = useMutation({
    mutationFn: async () => {
      const form = new FormData();
      form.append("memoryLayer", memoryLayer);
      if (memorySourceUrl) form.append("sourceUrl", memorySourceUrl);
      if (memoryFile) form.append("file", memoryFile);
      return axios.post(
        `/api/customer-agent/v1/personas/${evidencePersonaId}/memory-evidence`,
        form,
      );
    },
    onSuccess: async () => {
      setMemoryFile(null);
      setMemorySourceUrl("");
      await client.invalidateQueries({
        queryKey: ["customer-agent-memory-evidence", evidencePersonaId],
      });
    },
  });
  const submit = (event: FormEvent) => {
    event.preventDefault();
    create.mutate();
  };

  return (
    <main className="container py-4">
      <div className="d-flex justify-content-between align-items-center mb-4">
        <div>
          <h1 className="h3 mb-1">Biblioteca de Personas</h1>
          <p className="text-muted mb-0">
            Hipóteses auditáveis para o Agente Cliente.
          </p>
        </div>
        <Link className="btn btn-outline-secondary" to="/agents">
          Voltar aos agentes
        </Link>
      </div>
      <div className="alert alert-info">
        Avaliações simuladas não validam demanda. Somente comportamento humano
        posterior altera a confiança.
      </div>
      <form className="card card-body mb-4" onSubmit={submit}>
        <h2 className="h5">Nova hipótese de persona</h2>
        <div className="row g-3">
          <div className="col-md-6">
            <label className="form-label">Nome</label>
            <input
              className="form-control"
              required
              value={name}
              onChange={(e) => setName(e.target.value)}
            />
          </div>
          <div className="col-md-6">
            <label className="form-label">Fonte da evidência</label>
            <input
              className="form-control"
              required
              value={evidence}
              onChange={(e) => setEvidence(e.target.value)}
              placeholder="Sessão, entrevista, comentário ou pesquisa"
            />
          </div>
          <div className="col-md-6">
            <label className="form-label">Dor observada</label>
            <textarea
              className="form-control"
              required
              value={pain}
              onChange={(e) => setPain(e.target.value)}
            />
          </div>
          <div className="col-md-6">
            <label className="form-label">Progresso desejado</label>
            <textarea
              className="form-control"
              required
              value={progress}
              onChange={(e) => setProgress(e.target.value)}
            />
          </div>
        </div>
        <button
          className="btn btn-primary align-self-start mt-3"
          disabled={create.isPending}
        >
          Cadastrar como hipótese
        </button>
      </form>
      <section className="row g-3">
        {personas.data?.map((persona) => (
          <article className="col-md-6" key={persona.id}>
            <div className="card h-100">
              <div className="card-body">
                <div className="d-flex justify-content-between">
                  <h2 className="h5">{persona.name}</h2>
                  <span className="badge text-bg-warning">
                    {persona.confidenceLevel}
                  </span>
                </div>
                <p>
                  <strong>Dor:</strong> {persona.pain}
                </p>
                <p>
                  <strong>Progresso:</strong> {persona.desiredProgress}
                </p>
              </div>
            </div>
          </article>
        ))}
      </section>
      <section className="card card-body mt-4">
        <h2 className="h5">Avaliação do Agente Cliente</h2>
        <p className="text-muted">
          Solicite uma leitura simulada de uma oferta, página ou vídeo. O
          resultado permanece separado dos dados humanos reais.
        </p>
        <div className="row g-3">
          <div className="col-md-4">
            <label className="form-label">Persona</label>
            <select
              className="form-select"
              value={evaluationPersonaId}
              onChange={(event) => setEvaluationPersonaId(event.target.value)}
            >
              <option value="">Selecione</option>
              {personas.data?.map((persona) => (
                <option key={persona.id} value={persona.id}>
                  {persona.name}
                </option>
              ))}
            </select>
          </div>
          <div className="col-md-3">
            <label className="form-label">Tipo de ativo</label>
            <select
              className="form-select"
              value={evaluationAssetType}
              onChange={(event) => setEvaluationAssetType(event.target.value)}
            >
              <option value="PAGE">Página</option>
              <option value="VIDEO">Vídeo</option>
              <option value="OFFER">Oferta</option>
            </select>
          </div>
          <div className="col-md-3">
            <label className="form-label">Modo da simulação</label>
            <select
              className="form-select"
              value={simulationVersion}
              onChange={(event) =>
                setSimulationVersion(
                  event.target.value as
                    "BASELINE_V1" | "BEHAVIORAL_V1" | "BEHAVIORAL_V2",
                )
              }
            >
              <option value="BEHAVIORAL_V2">
                Psique humana v2 (recomendado)
              </option>
              <option value="BEHAVIORAL_V1">Comportamental v1</option>
              <option value="BASELINE_V1">Baseline atual</option>
            </select>
            <div className="form-text">
              O modo comportamental executa e compara os dois modelos.
            </div>
          </div>
          <div className="col-md-2">
            <label className="form-label">Referência pública do ativo</label>
            <input
              className="form-control"
              maxLength={255}
              value={evaluationAssetReference}
              onChange={(event) =>
                setEvaluationAssetReference(event.target.value)
              }
              placeholder="URL pública ou identificador auditável"
            />
            <div className="form-text">
              Até 255 caracteres. Use uma referência curta e auditável do ativo.
            </div>
          </div>
        </div>
        <button
          className="btn btn-primary align-self-start mt-3"
          disabled={
            createEvaluation.isPending ||
            !evaluationPersonaId ||
            !evaluationAssetReference.trim()
          }
          onClick={() => createEvaluation.mutate()}
          type="button"
        >
          Solicitar avaliação
        </button>
        <div className="row g-3 mt-1">
          {evaluations.data?.map((evaluation) => (
            <article className="col-md-6" key={evaluation.id}>
              <div className="border rounded p-3 h-100">
                <div className="d-flex justify-content-between gap-2">
                  <strong>{evaluation.persona.name}</strong>
                  <span className="badge text-bg-secondary">
                    {evaluation.status}
                  </span>
                </div>
                <div className="small text-muted mt-1">
                  {evaluation.assetType}: {evaluation.assetReference}
                </div>
                <div className="small text-muted">
                  {evaluation.simulationVersion === "BEHAVIORAL_V2"
                    ? "Psique humana v2 comparada ao baseline"
                    : evaluation.simulationVersion === "BEHAVIORAL_V1"
                      ? "Comportamental v1 comparado ao baseline"
                      : "Baseline v1"}
                </div>
                <CodexExecutionTelemetry
                  agentType="CUSTOMER_AGENT"
                  executionId={evaluation.id}
                />
                {evaluation.simulatedAssessment && (
                  <p className="mt-2 mb-1">{evaluation.simulatedAssessment}</p>
                )}
                {evaluation.behavioralResultJson && (
                  <details className="mt-2">
                    <summary className="fw-semibold">
                      Ver simulação e comparação completas
                    </summary>
                    <pre
                      className="small bg-body-tertiary border rounded p-2 mt-2"
                      style={{
                        overflowWrap: "anywhere",
                        whiteSpace: "pre-wrap",
                      }}
                    >
                      {JSON.stringify(
                        JSON.parse(evaluation.behavioralResultJson),
                        null,
                        2,
                      )}
                    </pre>
                  </details>
                )}
                {evaluation.status === "FAILED" && evaluation.lastError && (
                  <EvaluationFailureDetails error={evaluation.lastError} />
                )}
                {evaluation.status === "FAILED" && (
                  <button
                    className="btn btn-outline-primary btn-sm mt-2"
                    disabled={retryEvaluation.isPending}
                    onClick={() => retryEvaluation.mutate(evaluation.id)}
                    type="button"
                  >
                    Reprocessar avaliação
                  </button>
                )}
                {evaluation.retryCount > 0 && (
                  <div className="small text-muted mt-2">
                    Reprocessamentos: {evaluation.retryCount}
                  </div>
                )}
                {evaluation.humanResultJson ? (
                  <div className="alert alert-success py-2 mt-2 mb-0">
                    Resultado humano registrado separadamente.
                  </div>
                ) : (
                  <div className="alert alert-warning py-2 mt-2 mb-0">
                    Avaliação simulada; ainda sem confirmação humana.
                  </div>
                )}
              </div>
            </article>
          ))}
        </div>
      </section>
      <section className="card card-body mt-4">
        <h2 className="h5">Experiência Digital Observacional</h2>
        <p className="text-muted">
          Navegação mobile somente leitura em páginas e feeds públicos
          autorizados. Observações e reações simuladas não validam comportamento
          humano.
        </p>
        <form
          className="row g-3"
          onSubmit={(event) => {
            event.preventDefault();
            createObservation.mutate();
          }}
        >
          <div className="col-md-4">
            <label className="form-label">Persona</label>
            <select
              className="form-select"
              required
              value={observationPersonaId}
              onChange={(event) => setObservationPersonaId(event.target.value)}
            >
              <option value="">Selecione</option>
              {personas.data?.map((persona) => (
                <option key={persona.id} value={persona.id}>
                  {persona.name}
                </option>
              ))}
            </select>
          </div>
          <div className="col-md-8">
            <label className="form-label">Objetivo da navegação</label>
            <input
              className="form-control"
              required
              value={observationObjective}
              onChange={(event) => setObservationObjective(event.target.value)}
              placeholder="Ex.: avaliar identificação, confiança e esforço na jornada MUSA"
            />
          </div>
          <div className="col-12">
            <label className="form-label">
              URLs públicas autorizadas (uma por linha)
            </label>
            <textarea
              className="form-control"
              rows={4}
              required
              value={observationSources}
              onChange={(event) => setObservationSources(event.target.value)}
              placeholder="https://..."
            />
          </div>
          <div className="col-12">
            <button
              className="btn btn-primary"
              disabled={createObservation.isPending}
            >
              Agendar experiência mobile
            </button>
          </div>
        </form>
        <div className="mt-4">
          {observations.data?.map((observation) => (
            <details className="border rounded p-3 mb-2" key={observation.id}>
              <summary>
                {observation.persona.name} — {observation.status} —{" "}
                {new Date(observation.createdAt).toLocaleString("pt-BR")}
              </summary>
              <p className="mt-3">
                <strong>Objetivo:</strong> {observation.objective}
              </p>
              <p>
                <strong>Fontes:</strong> {observation.authorizedSourcesJson}
              </p>
              {observation.observationJson && (
                <p>
                  <strong>Observação externa:</strong>{" "}
                  {observation.observationJson}
                </p>
              )}
              {observation.simulatedReactionJson && (
                <p>
                  <strong>Reação simulada:</strong>{" "}
                  {observation.simulatedReactionJson}
                </p>
              )}
              {observation.commercialHypothesisJson && (
                <p>
                  <strong>Hipótese comercial:</strong>{" "}
                  {observation.commercialHypothesisJson}
                </p>
              )}
              <p>
                <strong>Confirmação humana:</strong>{" "}
                {observation.humanConfirmationJson ?? "Ainda não confirmada"}
              </p>
            </details>
          ))}
        </div>
      </section>
      <section className="card card-body mt-4">
        <h2 className="h5">Memória híbrida e evidências</h2>
        <p className="text-muted">
          Metadados e camadas permanecem no MySQL; arquivos pesados ficam
          privados no S3. Uma simulação nunca é promovida automaticamente a
          resultado humano.
        </p>
        <form
          className="row g-3"
          onSubmit={(event) => {
            event.preventDefault();
            uploadMemoryEvidence.mutate();
          }}
        >
          <div className="col-md-4">
            <label className="form-label">Persona</label>
            <select
              className="form-select"
              required
              value={evidencePersonaId}
              onChange={(event) => setEvidencePersonaId(event.target.value)}
            >
              <option value="">Selecione</option>
              {personas.data?.map((persona) => (
                <option key={persona.id} value={persona.id}>
                  {persona.name}
                </option>
              ))}
            </select>
          </div>
          <div className="col-md-4">
            <label className="form-label">Camada da memória</label>
            <select
              className="form-select"
              value={memoryLayer}
              onChange={(event) => setMemoryLayer(event.target.value)}
            >
              <option value="EXTERNAL_OBSERVATION">Observação externa</option>
              <option value="SIMULATED_INTERPRETATION">
                Interpretação simulada
              </option>
              <option value="COMMERCIAL_HYPOTHESIS">Hipótese comercial</option>
              <option value="HUMAN_RESULT">Resultado humano</option>
              <option value="CONFIRMED_LEARNING">Aprendizado confirmado</option>
            </select>
          </div>
          <div className="col-md-4">
            <label className="form-label">Fonte pública</label>
            <input
              className="form-control"
              type="url"
              value={memorySourceUrl}
              onChange={(event) => setMemorySourceUrl(event.target.value)}
              placeholder="https://..."
            />
          </div>
          <div className="col-12">
            <label className="form-label">
              Screenshot, HTML, vídeo, áudio ou transcrição
            </label>
            <input
              className="form-control"
              type="file"
              required
              onChange={(event) =>
                setMemoryFile(event.target.files?.[0] ?? null)
              }
            />
          </div>
          <div className="col-12">
            <button
              className="btn btn-primary"
              disabled={uploadMemoryEvidence.isPending || !memoryFile}
            >
              Preservar evidência
            </button>
          </div>
        </form>
        <div className="mt-4">
          {memoryEvidence.data?.map((item) => (
            <div className="border rounded p-3 mb-2" key={item.id}>
              <div className="d-flex flex-wrap justify-content-between gap-2">
                <strong>{item.memoryLayer}</strong>
                <span>{(item.sizeBytes / 1024).toFixed(1)} KB</span>
              </div>
              <div className="small text-muted">
                {item.contentType} · SHA-256 {item.sha256.slice(0, 12)}… ·
                retenção até{" "}
                {new Date(item.retentionUntil).toLocaleDateString("pt-BR")}
              </div>
              {item.sourceUrl && (
                <a href={item.sourceUrl} target="_blank" rel="noreferrer">
                  Ver fonte pública
                </a>
              )}
            </div>
          ))}
        </div>
      </section>
    </main>
  );
}
