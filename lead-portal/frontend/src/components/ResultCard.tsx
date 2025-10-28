import { LeadDetails } from "../types";

interface ResultCardProps {
  lead: LeadDetails;
}

function ResultCard({ lead }: ResultCardProps) {
  const formattedCreatedAt = new Date(lead.createdAt).toLocaleString("pt-BR");
  const formattedCompletedAt = lead.completedAt ? new Date(lead.completedAt).toLocaleString("pt-BR") : null;

  return (
    <section className="result-card">
      <header>
        <h2>Resumo do lead</h2>
        <div className={`result-status ${lead.status === "PROCESSING" ? "processing" : ""}`}>
          {lead.status === "PROCESSING" ? "Processando imagem..." : "Resultado disponível"}
        </div>
      </header>

      <dl>
        <div>
          <dt>Identificador</dt>
          <dd>{lead.id}</dd>
        </div>
        <div>
          <dt>Enviado por</dt>
          <dd>
            {lead.name} · {lead.email}
          </dd>
        </div>
        <div>
          <dt>Enviado em</dt>
          <dd>{formattedCreatedAt}</dd>
        </div>
        {formattedCompletedAt && (
          <div>
            <dt>Processado em</dt>
            <dd>{formattedCompletedAt}</dd>
          </div>
        )}
        {lead.notes && (
          <div>
            <dt>Observações</dt>
            <dd>{lead.notes}</dd>
          </div>
        )}
      </dl>

      <img className="result-image" src={lead.imageUrl} alt={`Imagem enviada por ${lead.name}`} />

      <div>
        <h3>Resultado</h3>
        <p>{lead.result ?? "Estamos analisando sua imagem e notificaremos quando terminar."}</p>
      </div>
    </section>
  );
}

export default ResultCard;
