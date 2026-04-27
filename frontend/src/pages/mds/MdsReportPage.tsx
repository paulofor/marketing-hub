import { useMemo } from "react";
import { useParams } from "react-router-dom";
import PageTitle from "../../components/PageTitle";
import { useMdsReport } from "../../api/mds/useMdsAdmin";

function asString(value: unknown, fallback = "-") {
  return typeof value === "string" && value.trim().length > 0 ? value : fallback;
}

function asArray(value: unknown): string[] {
  if (!Array.isArray(value)) {
    return [];
  }
  return value.map((item) => String(item));
}

export default function MdsReportPage() {
  const { requestId } = useParams();
  const id = requestId ? Number(requestId) : null;
  const reportQuery = useMdsReport(id);

  const reportView = useMemo(() => {
    const content = reportQuery.data?.content ?? {};
    return {
      recommendedMechanism: asString(content.recommendedMechanismCandidateKey ?? content.recommendedMechanism),
      selectedEvidence: asArray(content.selectedEvidenceIds ?? content.selectedEvidenceKeys),
      confidenceLevel: asString(content.confidenceLevel),
      limitations: asArray(content.limitations),
      summaryRationale: asString(content.summaryRationale ?? content.rationale),
    };
  }, [reportQuery.data?.content]);

  if (reportQuery.isLoading) {
    return <div className="d-flex justify-content-center py-5"><span className="spinner-border text-primary" aria-hidden="true" /></div>;
  }
  if (reportQuery.isError || !reportQuery.data) {
    return <div className="alert alert-danger mb-0">Não foi possível carregar o relatório MDS.</div>;
  }

  return (
    <div className="d-flex flex-column gap-4">
      <header>
        <PageTitle>MDS · Relatório da request #{reportQuery.data.requestId}</PageTitle>
      </header>

      <section className="card border-0 shadow-sm">
        <div className="card-body d-flex flex-column gap-3">
          <h2 className="h6 mb-0">Resumo executivo</h2>
          <p className="mb-0"><strong>Mecanismo recomendado:</strong> {reportView.recommendedMechanism}</p>
          <p className="mb-0"><strong>Nível de confiança:</strong> {reportView.confidenceLevel}</p>
          <p className="mb-0"><strong>Justificativa resumida:</strong> {reportView.summaryRationale}</p>

          <div>
            <strong>Evidências selecionadas:</strong>
            {reportView.selectedEvidence.length === 0 ? (
              <p className="mb-0 text-secondary">Nenhuma evidência explicitada no relatório.</p>
            ) : (
              <ul className="mb-0 mt-1">
                {reportView.selectedEvidence.map((evidence) => (
                  <li key={evidence}>{evidence}</li>
                ))}
              </ul>
            )}
          </div>

          <div>
            <strong>Limitações:</strong>
            {reportView.limitations.length === 0 ? (
              <p className="mb-0 text-secondary">Sem limitações explícitas no payload.</p>
            ) : (
              <ul className="mb-0 mt-1">
                {reportView.limitations.map((limitation) => (
                  <li key={limitation}>{limitation}</li>
                ))}
              </ul>
            )}
          </div>
        </div>
      </section>

      <section className="card border-0 shadow-sm">
        <div className="card-body d-flex flex-column gap-2">
          <h2 className="h6 mb-0">Visão técnica (payload)</h2>
          <p className="mb-0"><strong>Artefato:</strong> {reportQuery.data.artifactType}</p>
          <p className="mb-0"><strong>Status:</strong> {reportQuery.data.status}</p>
          <p className="mb-0"><strong>Versão:</strong> {reportQuery.data.version}</p>
          <pre className="bg-body-tertiary border rounded-3 p-3 small mb-0" style={{ whiteSpace: "pre-wrap" }}>
            {JSON.stringify(reportQuery.data.content, null, 2)}
          </pre>
        </div>
      </section>
    </div>
  );
}
