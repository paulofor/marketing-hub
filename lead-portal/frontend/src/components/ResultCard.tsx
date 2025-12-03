import { LeadDetails } from "../types";

interface ResultCardProps {
  lead: LeadDetails;
}

function ResultCard({ lead }: ResultCardProps) {
  const formattedCreatedAt = new Date(lead.createdAt).toLocaleString("pt-BR");
  const formattedCompletedAt = lead.completedAt
    ? new Date(lead.completedAt).toLocaleString("pt-BR")
    : null;

  const isProcessing = lead.status !== "COMPLETED";
  const processingMessage = (() => {
    switch (lead.status) {
      case "WATERMARK_PENDING":
        return "Gerando prévias com marca d'água...";
      case "WATERMARKING":
        return "Aplicando marca d'água nas imagens...";
      case "PROCESSING":
        return "Processando imagem...";
      default:
        return "Resultado disponível";
    }
  })();

  const resultMessage = lead.result
    ?? (lead.status === "COMPLETED"
      ? "Sua prévia está pronta!"
      : "Estamos analisando sua imagem e notificaremos quando terminar.");

  return (
    <section className="result-card">
      <header>
        <h2>Resumo do lead</h2>
        <div className={`result-status ${isProcessing ? "processing" : ""}`}>
          {processingMessage}
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

      <img
        className="result-image"
        src={lead.imageUrl}
        alt={`Imagem enviada por ${lead.name}`}
      />

      <div>
        <h3>Resultado</h3>
        <p>{resultMessage}</p>
      </div>
    </section>
  );
}

export default ResultCard;
