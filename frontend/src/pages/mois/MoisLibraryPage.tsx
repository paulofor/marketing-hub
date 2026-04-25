import { useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { toast } from "react-toastify";
import PageTitle from "../../components/PageTitle";
import { useDuplicateMoisBlock, useFavoriteMoisBlock, useMoisLibraryBlocks } from "../../api/mois/useMoisSprintTwo";

const WORKSPACE_ID = "workspace-default";

export default function MoisLibraryPage() {
  const [niche, setNiche] = useState("");
  const [formatType, setFormatType] = useState("");
  const blocksQuery = useMoisLibraryBlocks(WORKSPACE_ID, niche, formatType);
  const favoriteMutation = useFavoriteMoisBlock();
  const duplicateMutation = useDuplicateMoisBlock();

  const loadingAction = favoriteMutation.isPending || duplicateMutation.isPending;
  const sortedBlocks = useMemo(
    () => (blocksQuery.data ?? []).slice().sort((a, b) => b.score - a.score),
    [blocksQuery.data],
  );

  async function handleFavorite(blockId: string) {
    try {
      await favoriteMutation.mutateAsync(blockId);
      toast.success("Bloco favoritado");
    } catch {
      toast.error("Não foi possível favoritar o bloco.");
    }
  }

  async function handleDuplicate(blockId: string) {
    try {
      await duplicateMutation.mutateAsync(blockId);
      toast.success("Bloco duplicado para oferta");
    } catch {
      toast.error("Não foi possível duplicar o bloco.");
    }
  }

  return (
    <div className="d-flex flex-column gap-4">
      <header className="d-flex flex-wrap justify-content-between gap-3">
        <div>
          <PageTitle>Biblioteca MOIS</PageTitle>
          <p className="text-secondary mb-0">Sprint 2: filtros, favoritos e duplicação para oferta.</p>
        </div>
        <Link className="btn btn-outline-secondary" to="/mois">
          Voltar ao workspace
        </Link>
      </header>

      <section className="card border-0 shadow-sm">
        <div className="card-body row g-3">
          <div className="col-12 col-md-6">
            <label className="form-label">Nicho</label>
            <input className="form-control" value={niche} onChange={(event) => setNiche(event.target.value)} />
          </div>
          <div className="col-12 col-md-6">
            <label className="form-label">Formato</label>
            <input className="form-control" value={formatType} onChange={(event) => setFormatType(event.target.value)} />
          </div>
        </div>
      </section>

      <section className="row g-3">
        {blocksQuery.isLoading ? <p className="text-secondary">Carregando blocos...</p> : null}
        {blocksQuery.isError ? <div className="alert alert-danger">Falha ao carregar biblioteca.</div> : null}
        {!blocksQuery.isLoading && !blocksQuery.isError && sortedBlocks.length === 0 ? (
          <div className="alert alert-secondary">Nenhum bloco para os filtros atuais.</div>
        ) : null}

        {sortedBlocks.map((block) => (
          <div className="col-12 col-lg-6" key={block.blockId}>
            <article className="card border-0 shadow-sm h-100">
              <div className="card-body d-flex flex-column gap-2">
                <div className="d-flex justify-content-between align-items-start gap-2">
                  <div>
                    <h2 className="h6 mb-1">{block.type}</h2>
                    <p className="text-secondary small mb-0">Origem: {block.origin}</p>
                  </div>
                  <span className="badge text-bg-light">Score {(block.score * 100).toFixed(0)}</span>
                </div>
                <p className="mb-0">{block.summary}</p>
                <div className="d-flex flex-wrap gap-2">
                  {block.tags.map((tag) => (
                    <span className="badge rounded-pill text-bg-secondary" key={tag}>
                      {tag}
                    </span>
                  ))}
                </div>
                <div className="d-flex gap-2 mt-auto">
                  <button
                    type="button"
                    className="btn btn-outline-primary btn-sm d-inline-flex align-items-center gap-2"
                    onClick={() => handleFavorite(block.blockId)}
                    disabled={loadingAction}
                  >
                    {favoriteMutation.isPending ? <span className="spinner-border spinner-border-sm" aria-hidden="true" /> : null}
                    Favoritar
                  </button>
                  <button
                    type="button"
                    className="btn btn-outline-secondary btn-sm d-inline-flex align-items-center gap-2"
                    onClick={() => handleDuplicate(block.blockId)}
                    disabled={loadingAction}
                  >
                    {duplicateMutation.isPending ? <span className="spinner-border spinner-border-sm" aria-hidden="true" /> : null}
                    Duplicar para oferta
                  </button>
                </div>
              </div>
            </article>
          </div>
        ))}
      </section>
    </div>
  );
}
