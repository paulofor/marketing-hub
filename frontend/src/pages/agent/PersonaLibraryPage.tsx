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

export default function PersonaLibraryPage() {
  const client = useQueryClient();
  const [name, setName] = useState("");
  const [pain, setPain] = useState("");
  const [progress, setProgress] = useState("");
  const [evidence, setEvidence] = useState("");
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
    </main>
  );
}
