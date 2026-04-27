import { useMemo, useState } from "react";
import { Link, useParams } from "react-router-dom";
import PageTitle from "../../components/PageTitle";
import { useMdsArtifacts } from "../../api/mds/useMdsAdmin";
import type { MdsArtifactItem } from "../../api/mds/types";

function toCanonicalEnvelope(artifact: MdsArtifactItem) {
  return {
    artifact: {
      artifactType: artifact.artifactType,
      artifactVersion: artifact.version,
      status: artifact.status,
      parentArtifactIds: artifact.parentArtifactIds,
      content: artifact.content,
    },
  };
}

export default function MdsArtifactsPage() {
  const { requestId } = useParams();
  const id = requestId ? Number(requestId) : null;
  const artifactsQuery = useMdsArtifacts(id);
  const [selectedArtifactId, setSelectedArtifactId] = useState<number | null>(null);

  const artifactsByType = useMemo(() => {
    const list = artifactsQuery.data?.artifacts ?? [];
    return list.reduce<Record<string, MdsArtifactItem[]>>((acc, artifact) => {
      if (!acc[artifact.artifactType]) {
        acc[artifact.artifactType] = [];
      }
      acc[artifact.artifactType].push(artifact);
      return acc;
    }, {});
  }, [artifactsQuery.data?.artifacts]);

  const selectedArtifact = useMemo(() => {
    const list = artifactsQuery.data?.artifacts ?? [];
    if (!list.length) {
      return null;
    }
    if (selectedArtifactId == null) {
      return list[0];
    }
    return list.find((artifact) => artifact.artifactId === selectedArtifactId) ?? list[0];
  }, [artifactsQuery.data?.artifacts, selectedArtifactId]);

  if (artifactsQuery.isLoading) {
    return <div className="d-flex justify-content-center py-5"><span className="spinner-border text-primary" aria-hidden="true" /></div>;
  }
  if (artifactsQuery.isError || !artifactsQuery.data) {
    return <div className="alert alert-danger mb-0">Não foi possível carregar os artefatos.</div>;
  }

  return (
    <div className="d-flex flex-column gap-4">
      <header className="d-flex flex-wrap justify-content-between align-items-start gap-2">
        <div>
          <PageTitle>MDS · Artefatos da request #{artifactsQuery.data.requestId}</PageTitle>
          <p className="text-secondary mb-0">Auditoria por tipo, envelope canônico e lineage básico entre artefatos.</p>
        </div>
        <Link className="btn btn-outline-dark btn-sm" to={`/mds/reports/${artifactsQuery.data.requestId}`}>
          Ver relatório
        </Link>
      </header>

      <section className="card border-0 shadow-sm">
        <div className="card-body d-flex flex-column gap-3">
          <h2 className="h6 mb-0">Artefatos por tipo</h2>
          {!artifactsQuery.data.artifacts.length ? (
            <div className="alert alert-secondary mb-0">Nenhum artefato foi publicado para esta request.</div>
          ) : (
            <div className="d-grid gap-3" style={{ gridTemplateColumns: "repeat(auto-fill, minmax(280px, 1fr))" }}>
              {Object.entries(artifactsByType).map(([artifactType, artifacts]) => (
                <article className="card border" key={artifactType}>
                  <div className="card-body d-flex flex-column gap-2">
                    <div className="d-flex align-items-center justify-content-between">
                      <h3 className="h6 mb-0">{artifactType}</h3>
                      <span className="badge text-bg-light">{artifacts.length}</span>
                    </div>
                    {artifacts.map((artifact) => (
                      <button
                        key={artifact.artifactId}
                        type="button"
                        className={`btn btn-sm text-start ${selectedArtifact?.artifactId === artifact.artifactId ? "btn-primary" : "btn-outline-secondary"}`}
                        onClick={() => setSelectedArtifactId(artifact.artifactId)}
                      >
                        #{artifact.artifactId} · {artifact.status}
                      </button>
                    ))}
                  </div>
                </article>
              ))}
            </div>
          )}
        </div>
      </section>

      <section className="card border-0 shadow-sm">
        <div className="card-body d-flex flex-column gap-3">
          <h2 className="h6 mb-0">Envelope canônico</h2>
          {!selectedArtifact ? (
            <div className="alert alert-secondary mb-0">Selecione um artefato para visualizar o envelope.</div>
          ) : (
            <>
              <div className="d-flex flex-wrap gap-2 text-secondary small">
                <span><strong>ID:</strong> #{selectedArtifact.artifactId}</span>
                <span><strong>Tipo:</strong> {selectedArtifact.artifactType}</span>
                <span><strong>Schema:</strong> {selectedArtifact.schemaVersion}</span>
                <span><strong>Versão:</strong> {selectedArtifact.version}</span>
              </div>
              <pre className="bg-body-tertiary border rounded-3 p-3 small mb-0" style={{ whiteSpace: "pre-wrap" }}>
                {JSON.stringify(toCanonicalEnvelope(selectedArtifact), null, 2)}
              </pre>
            </>
          )}
        </div>
      </section>

      <section className="card border-0 shadow-sm">
        <div className="card-body d-flex flex-column gap-3">
          <h2 className="h6 mb-0">Lineage básico (pais/filhos)</h2>
          {!selectedArtifact ? (
            <div className="alert alert-secondary mb-0">Sem artefato selecionado para navegação de lineage.</div>
          ) : (
            <div className="row g-3">
              <div className="col-12 col-lg-6">
                <h3 className="h6">Pais</h3>
                {selectedArtifact.parentArtifactIds.length === 0 ? (
                  <div className="alert alert-secondary mb-0">Sem pais vinculados.</div>
                ) : (
                  <div className="d-flex flex-wrap gap-2">
                    {selectedArtifact.parentArtifactIds.map((parentId) => (
                      <button
                        key={parentId}
                        type="button"
                        className="btn btn-outline-secondary btn-sm"
                        onClick={() => setSelectedArtifactId(parentId)}
                      >
                        Abrir #{parentId}
                      </button>
                    ))}
                  </div>
                )}
              </div>
              <div className="col-12 col-lg-6">
                <h3 className="h6">Filhos</h3>
                {selectedArtifact.childArtifactIds.length === 0 ? (
                  <div className="alert alert-secondary mb-0">Sem filhos vinculados.</div>
                ) : (
                  <div className="d-flex flex-wrap gap-2">
                    {selectedArtifact.childArtifactIds.map((childId) => (
                      <button
                        key={childId}
                        type="button"
                        className="btn btn-outline-secondary btn-sm"
                        onClick={() => setSelectedArtifactId(childId)}
                      >
                        Abrir #{childId}
                      </button>
                    ))}
                  </div>
                )}
              </div>
              <div className="col-12">
                {artifactsQuery.data.lineage.length === 0 ? (
                  <div className="alert alert-secondary mb-0">Sem relações de lineage registradas.</div>
                ) : (
                  <ul className="list-group list-group-flush">
                    {artifactsQuery.data.lineage.map((edge) => (
                      <li className="list-group-item px-0" key={edge.id}>
                        #{edge.parentArtifactId} → #{edge.childArtifactId} ({edge.relationType})
                      </li>
                    ))}
                  </ul>
                )}
              </div>
            </div>
          )}
        </div>
      </section>
    </div>
  );
}
