import { useQuery } from "@tanstack/react-query";
import { Link, useParams } from "react-router-dom";
import { fetchImageMaterialCase } from "../api";

function AnswersList({ answers }: { answers: Record<string, unknown> }) {
  const entries = Object.entries(answers ?? {});
  if (!entries.length) {
    return <p className="empty-state">Nenhuma resposta capturada.</p>;
  }
  return (
    <div className="answers-grid">
      {entries.map(([key, value]) => (
        <div key={key} className="answer-card">
          <span className="answer-key">{key}</span>
          <span className="answer-value">{formatValue(value)}</span>
        </div>
      ))}
    </div>
  );
}

function formatValue(value: unknown): string {
  if (Array.isArray(value)) {
    return value.join(", ");
  }
  if (value === null || value === undefined) {
    return "—";
  }
  return String(value);
}

function PackageTimeline({
  history
}: {
  history: { status: string; createdAt?: string | null; reason?: string | null }[];
}) {
  if (!history.length) {
    return <p className="empty-state">Sem histórico registrado.</p>;
  }
  return (
    <ol className="timeline">
      {history.map((entry, index) => (
        <li key={`${entry.status}-${index}`}>
          <div className="timeline-entry">
            <div className="timeline-dot" />
            <div>
              <strong>{entry.status}</strong>
              <div className="timeline-meta">
                {entry.createdAt ? new Date(entry.createdAt).toLocaleString("pt-BR") : "Sem data"}
              </div>
              {entry.reason ? <p className="timeline-reason">{entry.reason}</p> : null}
            </div>
          </div>
          {index < history.length - 1 ? <div className="timeline-connector" /> : null}
        </li>
      ))}
    </ol>
  );
}

export default function ImageCasePage() {
  const { submissionId } = useParams<{ submissionId: string }>();
  const query = useQuery({
    queryKey: ["image-case", submissionId],
    queryFn: () => {
      if (!submissionId) {
        throw new Error("ID da submissão é obrigatório");
      }
      return fetchImageMaterialCase(submissionId);
    },
    enabled: Boolean(submissionId)
  });

  return (
    <div className="case-container">
      <header className="case-header">
        <div>
          <p className="dashboard-kicker">Caso específico</p>
          <h1>Submissão {submissionId}</h1>
          <p>Acompanhe o histórico completo para ajudar o suporte quando o profissional relatar algum problema.</p>
        </div>
        <div className="action-group">
          <Link to="/monitoramento/imagens" className="button-secondary">
            Voltar para painel
          </Link>
          <button className="button-primary" onClick={() => query.refetch()} disabled={query.isFetching}>
            Atualizar
          </button>
        </div>
      </header>

      {query.isLoading ? <p className="flow-message">Carregando caso...</p> : null}
      {query.isError ? (
        <p className="flow-message error">{query.error instanceof Error ? query.error.message : "Erro ao carregar caso."}</p>
      ) : null}

      {query.data ? (
        <>
          <section className="case-overview">
            <div className="case-card">
              <h2>Profissional</h2>
              <p className="case-main">{query.data.professionalName}</p>
              <p>{query.data.activityType}</p>
              <p>{query.data.contactSummary}</p>
              <p>{query.data.location || "Local não informado"}</p>
            </div>
            <div className="case-card">
              <h2>Serviços destacados</h2>
              <div className="chip-group">
                {query.data.services.length === 0 ? (
                  <span className="chip muted">Sem serviços informados</span>
                ) : (
                  query.data.services.map((service) => (
                    <span key={service} className="chip">
                      {service}
                    </span>
                  ))
                )}
              </div>
            </div>
            <div className="case-card">
              <h2>Contato</h2>
              <p>{query.data.email}</p>
              <p>{query.data.contactSummary}</p>
            </div>
          </section>

          <section className="answers-section">
            <div className="section-heading">
              <h2>Respostas do formulário</h2>
            </div>
            <AnswersList answers={query.data.answers as Record<string, unknown>} />
          </section>

          <section className="packages-section">
            <div className="section-heading">
              <h2>Pacotes gerados</h2>
              <span>{query.data.packages.length} ocorrências</span>
            </div>
            {query.data.packages.length === 0 ? (
              <p className="empty-state">Ainda não existe nenhum pacote para esta submissão.</p>
            ) : (
              query.data.packages.map((pack) => (
                <article key={pack.packageId} className="package-card">
                  <header className="package-card-header">
                    <div>
                      <p>Pacote #{pack.packageId}</p>
                      <strong>{pack.status}</strong>
                    </div>
                    <div className="package-meta">
                      <span>
                        Criado: {pack.createdAt ? new Date(pack.createdAt).toLocaleString("pt-BR") : "—"}
                      </span>
                      <span>
                        Atualizado: {pack.updatedAt ? new Date(pack.updatedAt).toLocaleString("pt-BR") : "—"}
                      </span>
                    </div>
                  </header>
                  <div className="package-body">
                    <p>
                      <strong>Prompt usado:</strong> {pack.prompt}
                    </p>
                    <p>
                      <strong>Modelo:</strong> {pack.model || "Aguardando resolução"}
                    </p>
                    <p>
                      <strong>Planejado:</strong> {pack.plannedOutputs ?? "-"} imagens em batch
                    </p>
                    <p>
                      <strong>Custo registrado:</strong> {pack.currency ?? "USD"} {pack.totalPrice?.toFixed(2) ?? "0.00"}
                    </p>
                    {pack.failureReason ? (
                      <p className="error-text">Erro reportado: {pack.failureReason}</p>
                    ) : null}
                  </div>
                  <div>
                    <h3>Timeline</h3>
                    <PackageTimeline history={pack.history} />
                  </div>
                </article>
              ))
            )}
          </section>
        </>
      ) : null}
    </div>
  );
}
