import { FormEvent, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import { Link } from "react-router-dom";

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

export default function PersonaLibraryPage() {
  const client = useQueryClient();
  const [name, setName] = useState("");
  const [pain, setPain] = useState("");
  const [progress, setProgress] = useState("");
  const [evidence, setEvidence] = useState("");
  const [observationPersonaId, setObservationPersonaId] = useState("");
  const [observationObjective, setObservationObjective] = useState("");
  const [observationSources, setObservationSources] = useState("");
  const personas = useQuery({
    queryKey: ["customer-personas"],
    queryFn: async () => (await axios.get<Persona[]>("/api/customer-agent/v1/personas")).data,
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
      setName(""); setPain(""); setProgress(""); setEvidence("");
      await client.invalidateQueries({ queryKey: ["customer-personas"] });
    },
  });
  const observations = useQuery({
    queryKey: ["customer-digital-observations"],
    queryFn: async () =>
      (await axios.get<DigitalObservation[]>("/api/customer-agent/v1/digital-observations"))
        .data,
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
      await client.invalidateQueries({ queryKey: ["customer-digital-observations"] });
    },
  });
  const submit = (event: FormEvent) => { event.preventDefault(); create.mutate(); };

  return (
    <main className="container py-4">
      <div className="d-flex justify-content-between align-items-center mb-4">
        <div><h1 className="h3 mb-1">Biblioteca de Personas</h1><p className="text-muted mb-0">Hipóteses auditáveis para o Agente Cliente.</p></div>
        <Link className="btn btn-outline-secondary" to="/agents">Voltar aos agentes</Link>
      </div>
      <div className="alert alert-info">Avaliações simuladas não validam demanda. Somente comportamento humano posterior altera a confiança.</div>
      <form className="card card-body mb-4" onSubmit={submit}>
        <h2 className="h5">Nova hipótese de persona</h2>
        <div className="row g-3">
          <div className="col-md-6"><label className="form-label">Nome</label><input className="form-control" required value={name} onChange={(e) => setName(e.target.value)} /></div>
          <div className="col-md-6"><label className="form-label">Fonte da evidência</label><input className="form-control" required value={evidence} onChange={(e) => setEvidence(e.target.value)} placeholder="Sessão, entrevista, comentário ou pesquisa" /></div>
          <div className="col-md-6"><label className="form-label">Dor observada</label><textarea className="form-control" required value={pain} onChange={(e) => setPain(e.target.value)} /></div>
          <div className="col-md-6"><label className="form-label">Progresso desejado</label><textarea className="form-control" required value={progress} onChange={(e) => setProgress(e.target.value)} /></div>
        </div>
        <button className="btn btn-primary align-self-start mt-3" disabled={create.isPending}>Cadastrar como hipótese</button>
      </form>
      <section className="row g-3">
        {personas.data?.map((persona) => (
          <article className="col-md-6" key={persona.id}><div className="card h-100"><div className="card-body">
            <div className="d-flex justify-content-between"><h2 className="h5">{persona.name}</h2><span className="badge text-bg-warning">{persona.confidenceLevel}</span></div>
            <p><strong>Dor:</strong> {persona.pain}</p><p><strong>Progresso:</strong> {persona.desiredProgress}</p>
          </div></div></article>
        ))}
      </section>
      <section className="card card-body mt-4">
        <h2 className="h5">Experiência Digital Observacional</h2>
        <p className="text-muted">
          Navegação mobile somente leitura em páginas e feeds públicos autorizados. Observações e
          reações simuladas não validam comportamento humano.
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
                <option key={persona.id} value={persona.id}>{persona.name}</option>
              ))}
            </select>
          </div>
          <div className="col-md-8">
            <label className="form-label">Objetivo da navegação</label>
            <input className="form-control" required value={observationObjective} onChange={(event) => setObservationObjective(event.target.value)} placeholder="Ex.: avaliar identificação, confiança e esforço na jornada MUSA" />
          </div>
          <div className="col-12">
            <label className="form-label">URLs públicas autorizadas (uma por linha)</label>
            <textarea className="form-control" rows={4} required value={observationSources} onChange={(event) => setObservationSources(event.target.value)} placeholder="https://..." />
          </div>
          <div className="col-12">
            <button className="btn btn-primary" disabled={createObservation.isPending}>
              Agendar experiência mobile
            </button>
          </div>
        </form>
        <div className="mt-4">
          {observations.data?.map((observation) => (
            <details className="border rounded p-3 mb-2" key={observation.id}>
              <summary>
                {observation.persona.name} — {observation.status} — {new Date(observation.createdAt).toLocaleString("pt-BR")}
              </summary>
              <p className="mt-3"><strong>Objetivo:</strong> {observation.objective}</p>
              <p><strong>Fontes:</strong> {observation.authorizedSourcesJson}</p>
              {observation.observationJson && <p><strong>Observação externa:</strong> {observation.observationJson}</p>}
              {observation.simulatedReactionJson && <p><strong>Reação simulada:</strong> {observation.simulatedReactionJson}</p>}
              {observation.commercialHypothesisJson && <p><strong>Hipótese comercial:</strong> {observation.commercialHypothesisJson}</p>}
              <p><strong>Confirmação humana:</strong> {observation.humanConfirmationJson ?? "Ainda não confirmada"}</p>
            </details>
          ))}
        </div>
      </section>
    </main>
  );
}
