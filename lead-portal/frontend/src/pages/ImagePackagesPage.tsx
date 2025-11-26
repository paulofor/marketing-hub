import { useMemo } from "react";
import { Link } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { fetchPendingImagePackages } from "../api";
import { FlowSubmissionImagePackage } from "../types";

function formatDate(dateString?: string | null): string {
  if (!dateString) {
    return "-";
  }

  const parsed = new Date(dateString);
  return Number.isNaN(parsed.getTime()) ? "-" : parsed.toLocaleString();
}

function summarizePrompt(prompt: string): string {
  const cleaned = prompt.trim();
  if (cleaned.length <= 140) {
    return cleaned || "(prompt vazio)";
  }

  return `${cleaned.slice(0, 137)}...`;
}

export default function ImagePackagesPage() {
  const {
    data: imagePackages,
    isLoading,
    isError,
    error
  } = useQuery({
    queryKey: ["pending-image-packages"],
    queryFn: fetchPendingImagePackages
  });

  const pendingCount = useMemo(() => imagePackages?.length ?? 0, [imagePackages]);

  return (
    <div className="page-container">
      <header className="page-header">
        <div>
          <p className="back-link">
            <Link to="/">&larr; Voltar</Link>
          </p>
          <h1>Pacotes de imagens pendentes</h1>
          <p className="page-subtitle">
            Lista dos envios que precisam ser inseridos no pipeline de criação de novas imagens.
          </p>
        </div>
        <div className="pill">{pendingCount} pendente(s)</div>
      </header>

      {isLoading && <p className="flow-message">Carregando pacotes...</p>}
      {isError && (
        <p className="flow-message error">
          {error instanceof Error ? error.message : "Não foi possível carregar os pacotes."}
        </p>
      )}

      {!isLoading && !isError && (
        <div className="card">
          {imagePackages && imagePackages.length > 0 ? (
            <table className="packages-table">
              <thead>
                <tr>
                  <th>Fluxo</th>
                  <th>Envio</th>
                  <th>Status</th>
                  <th>Modelo</th>
                  <th>Prompt</th>
                  <th>Saídas</th>
                  <th>Gerar de graça</th>
                  <th>Criado em</th>
                </tr>
              </thead>
              <tbody>
                {imagePackages.map((pkg: FlowSubmissionImagePackage) => (
                  <tr key={pkg.id}>
                    <td>
                      <div className="table-primary">{pkg.flowSlug ?? "-"}</div>
                      <div className="table-secondary">{pkg.email ?? ""}</div>
                    </td>
                    <td>
                      <div className="table-primary">{pkg.submissionId}</div>
                      <div className="table-secondary">{pkg.name ?? ""}</div>
                    </td>
                    <td>
                      <span className="status-chip">{pkg.status}</span>
                    </td>
                    <td>{pkg.model ?? "-"}</td>
                    <td className="prompt-cell" title={pkg.prompt}>
                      {summarizePrompt(pkg.prompt)}
                    </td>
                    <td>{pkg.plannedOutputs ?? "-"}</td>
                    <td>{pkg.freeImages}</td>
                    <td>{formatDate(pkg.createdAt)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          ) : (
            <p className="flow-message">Nenhum pacote pendente no momento.</p>
          )}
        </div>
      )}
    </div>
  );
}
