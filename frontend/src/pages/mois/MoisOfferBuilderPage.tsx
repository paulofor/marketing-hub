import { useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { toast } from "react-toastify";
import PageTitle from "../../components/PageTitle";
import { useBuildMoisOffer, useMoisLibraryBlocks } from "../../api/mois/useMoisSprintTwo";

const WORKSPACE_ID = "workspace-default";

export default function MoisOfferBuilderPage() {
  const [currentOfferId, setCurrentOfferId] = useState("");
  const [currentVersion, setCurrentVersion] = useState("");
  const [selectedBlockIds, setSelectedBlockIds] = useState<string[]>([]);

  const blocksQuery = useMoisLibraryBlocks(WORKSPACE_ID);
  const buildMutation = useBuildMoisOffer();

  function toggleBlock(blockId: string) {
    setSelectedBlockIds((prev) =>
      prev.includes(blockId) ? prev.filter((id) => id !== blockId) : [...prev, blockId],
    );
  }

  async function handleGenerate(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    try {
      await buildMutation.mutateAsync({
        workspaceId: WORKSPACE_ID,
        currentOfferId,
        selectedBlockIds,
        currentVersion,
      });
      toast.success("Versão proposta gerada");
    } catch {
      toast.error("Falha ao gerar versão proposta.");
    }
  }

  const checklistEntries = useMemo(() => Object.entries(buildMutation.data?.checklist ?? {}), [buildMutation.data?.checklist]);

  return (
    <div className="d-flex flex-column gap-4">
      <header className="d-flex flex-wrap justify-content-between gap-3">
        <div>
          <PageTitle>Builder de oferta MOIS</PageTitle>
          <p className="text-secondary mb-0">Sprint 2: aplicação de blocos com checklist canônico DRMP-O.</p>
        </div>
        <Link className="btn btn-outline-secondary" to="/mois">
          Voltar ao workspace
        </Link>
      </header>

      <form className="row g-3" onSubmit={handleGenerate}>
        <div className="col-12 col-lg-4">
          <section className="card border-0 shadow-sm h-100">
            <div className="card-body d-flex flex-column gap-2">
              <h2 className="h6">Blocos recomendados</h2>
              {blocksQuery.data?.map((block) => (
                <label className="form-check" key={block.blockId}>
                  <input
                    className="form-check-input"
                    type="checkbox"
                    checked={selectedBlockIds.includes(block.blockId)}
                    onChange={() => toggleBlock(block.blockId)}
                  />
                  <span className="form-check-label">{block.type}: {block.summary}</span>
                </label>
              ))}
            </div>
          </section>
        </div>

        <div className="col-12 col-lg-4">
          <section className="card border-0 shadow-sm h-100">
            <div className="card-body d-flex flex-column gap-2">
              <h2 className="h6">Versão atual</h2>
              <label className="form-label">Oferta atual *</label>
              <input className="form-control" value={currentOfferId} onChange={(event) => setCurrentOfferId(event.target.value)} required />
              <label className="form-label">Texto atual *</label>
              <textarea className="form-control" rows={9} value={currentVersion} onChange={(event) => setCurrentVersion(event.target.value)} required />
            </div>
          </section>
        </div>

        <div className="col-12 col-lg-4">
          <section className="card border-0 shadow-sm h-100">
            <div className="card-body d-flex flex-column gap-2">
              <h2 className="h6">Versão proposta</h2>
              <textarea className="form-control" rows={11} value={buildMutation.data?.proposedVersion ?? ""} readOnly />
            </div>
          </section>
        </div>

        <div className="col-12">
          <section className="card border-0 shadow-sm">
            <div className="card-body d-flex flex-wrap align-items-center justify-content-between gap-3">
              <div className="d-flex flex-wrap gap-2">
                {checklistEntries.length === 0 ? <span className="text-secondary">Checklist será exibido após gerar a versão.</span> : null}
                {checklistEntries.map(([item, done]) => (
                  <span className={`badge ${done ? "text-bg-success" : "text-bg-warning"}`} key={item}>
                    {item}: {done ? "ok" : "pendente"}
                  </span>
                ))}
              </div>
              <button type="submit" className="btn btn-primary d-inline-flex align-items-center gap-2" disabled={buildMutation.isPending}>
                {buildMutation.isPending ? <span className="spinner-border spinner-border-sm" aria-hidden="true" /> : null}
                Gerar versão
              </button>
            </div>
          </section>
        </div>
      </form>
    </div>
  );
}
