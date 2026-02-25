import { useQuery } from "@tanstack/react-query";
import { FormEvent, useState } from "react";
import { useNavigate } from "react-router-dom";
import { fetchImageMaterialDashboard } from "../api";
import { ImagePackageSummary } from "../types";

function StatCard({ label, value, highlight }: { label: string; value: string | number; highlight?: boolean }) {
  return (
    <div className={`stat-card ${highlight ? "stat-card-highlight" : ""}`}>
      <p>{label}</p>
      <strong>{value}</strong>
    </div>
  );
}

function StatusPill({ status }: { status: string }) {
  return <span className={`status-pill status-${status.toLowerCase()}`}>{status}</span>;
}

function RecentPackagesTable({ packages }: { packages: ImagePackageSummary[] }) {
  const navigate = useNavigate();
  if (!packages.length) {
    return <p className="empty-state">Nenhum pacote disponível ainda.</p>;
  }
  return (
    <div className="packages-table">
      <table>
        <thead>
          <tr>
            <th>Profissional</th>
            <th>Status</th>
            <th>Serviços</th>
            <th>Contato</th>
            <th>Atualizado</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          {packages.map((pack) => (
            <tr key={pack.packageId}>
              <td>
                <div className="table-primary">{pack.professionalName}</div>
                <small>{pack.location || pack.studioName || "Local não informado"}</small>
              </td>
              <td>
                <StatusPill status={pack.status} />
              </td>
              <td>
                <div className="chip-group">
                  {pack.services.slice(0, 3).map((service) => (
                    <span key={service} className="chip">
                      {service}
                    </span>
                  ))}
                  {pack.services.length > 3 ? <span className="chip muted">+{pack.services.length - 3}</span> : null}
                </div>
              </td>
              <td>
                <div className="table-primary">{pack.contactSummary}</div>
                {pack.totalPrice ? (
                  <small>
                    {pack.currency ?? "USD"} {pack.totalPrice.toFixed(2)}
                  </small>
                ) : (
                  <small>Custo a confirmar</small>
                )}
              </td>
              <td>{pack.updatedAt ? new Date(pack.updatedAt).toLocaleString("pt-BR") : "—"}</td>
              <td>
                <button
                  type="button"
                  className="link-button"
                  onClick={() => navigate(`/monitoramento/imagens/casos/${pack.submissionId}`)}
                >
                  Ver caso
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

export default function ImageDashboardPage() {
  const [flowSlug] = useState("formulario-simples-personal-trainer");
  const [caseId, setCaseId] = useState("");
  const navigate = useNavigate();
  const query = useQuery({
    queryKey: ["image-dashboard", flowSlug],
    queryFn: () => fetchImageMaterialDashboard(flowSlug),
    refetchInterval: 30000
  });

  const handleSearch = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!caseId.trim()) {
      return;
    }
    navigate(`/monitoramento/imagens/casos/${caseId.trim()}`);
  };

  return (
    <div className="dashboard-container">
      <header className="dashboard-header">
        <div>
          <p className="dashboard-kicker">Monitoramento</p>
          <h1>Pipeline de imagens sem foto original</h1>
          <p>Acompanhe custos, volumes e investigações das solicitações vindas do formulário simples.</p>
        </div>
        <div>
          <button className="button-secondary" onClick={() => query.refetch()} disabled={query.isFetching}>
            Atualizar agora
          </button>
        </div>
      </header>

      {query.isLoading ? <p className="flow-message">Carregando painel...</p> : null}
      {query.isError ? (
        <p className="flow-message error">{query.error instanceof Error ? query.error.message : "Erro ao carregar painel."}</p>
      ) : null}

      {query.data ? (
        <>
          <section className="dashboard-grid">
            <StatCard label="Submissões" value={query.data.totalSubmissions} />
            <StatCard label="Em fila" value={query.data.packagesQueued} />
            <StatCard label="Em processamento" value={query.data.packagesInProgress} />
            <StatCard label="Concluídos" value={query.data.packagesCompleted} />
            <StatCard label="Falhas" value={query.data.packagesFailed} highlight={query.data.packagesFailed > 0} />
            <StatCard label="Imagens geradas" value={query.data.imagesGenerated} />
            <StatCard label="Batches planejados" value={query.data.plannedImages} />
            <StatCard
              label="Custo estimado (USD)"
              value={query.data.estimatedCostUsd?.toFixed(2) ?? "0.00"}
              highlight
            />
          </section>

          <section className="cost-card">
            <div>
              <h2>Resumo financeiro</h2>
              <p>Pagamentos conciliados por moeda.</p>
            </div>
            <div className="chip-group">
              {query.data.payments.length === 0 ? (
                <span className="chip muted">Nenhum pagamento recebido</span>
              ) : (
                query.data.payments.map((payment) => (
                  <span key={payment.currency} className="chip">
                    {payment.currency}: {payment.amount?.toFixed(2) ?? "0.00"}
                  </span>
                ))
              )}
            </div>
          </section>

          <section className="packages-card">
            <div className="section-heading">
              <h2>Pacotes recentes</h2>
              <span>{query.data.recentPackages.length} registros</span>
            </div>
            <RecentPackagesTable packages={query.data.recentPackages} />
          </section>

          <section className="case-search">
            <h2>Consultar caso específico</h2>
            <form onSubmit={handleSearch} className="case-search-form">
              <input
                type="text"
                placeholder="ID da submissão"
                value={caseId}
                onChange={(event) => setCaseId(event.target.value)}
              />
              <button type="submit" className="button-primary">
                Abrir
              </button>
            </form>
          </section>
        </>
      ) : null}
    </div>
  );
}
