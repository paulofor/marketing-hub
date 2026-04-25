import { useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { toast } from "react-toastify";
import PageTitle from "../../components/PageTitle";
import { useCreateMoisReference, useMoisReferences } from "../../api/mois/useMoisReferences";
import type { MoisCreateReferencePayload } from "../../api/mois/types";

const WORKSPACE_ID = "workspace-default";

const INITIAL_FORM: MoisCreateReferencePayload = {
  workspaceId: WORKSPACE_ID,
  niche: "",
  sourceUrl: "",
  assetType: "LANDING_PAGE",
  primaryPromise: "",
  awarenessStage: "PROBLEM_AWARE",
  priceRange: "",
  formatType: "",
  notes: "",
};

export default function MoisReferenceIntakePage() {
  const [form, setForm] = useState<MoisCreateReferencePayload>(INITIAL_FORM);
  const createReference = useCreateMoisReference();
  const referencesQuery = useMoisReferences(WORKSPACE_ID);

  const sourcePreview = useMemo(() => form.sourceUrl.trim(), [form.sourceUrl]);

  async function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    try {
      await createReference.mutateAsync(form);
      toast.success("Referência salva");
      setForm(INITIAL_FORM);
    } catch {
      toast.error("Falha ao salvar referência. Revise os dados e tente novamente.");
    }
  }

  return (
    <div className="d-flex flex-column gap-4">
      <header className="d-flex flex-wrap justify-content-between gap-3">
        <div>
          <PageTitle>Coleta de referências MOIS</PageTitle>
          <p className="text-secondary mb-0">Sprint 1: cadastro e listagem de referências do workspace.</p>
        </div>
        <Link className="btn btn-outline-secondary" to="/mois">
          Voltar ao workspace
        </Link>
      </header>

      <section className="card border-0 shadow-sm">
        <div className="card-body">
          <form className="row g-3" onSubmit={handleSubmit}>
            <div className="col-12 col-lg-6">
              <label className="form-label" htmlFor="mois-niche">Nicho *</label>
              <input
                id="mois-niche"
                className="form-control"
                value={form.niche}
                onChange={(event) => setForm((prev) => ({ ...prev, niche: event.target.value }))}
                required
              />
            </div>

            <div className="col-12 col-lg-6">
              <label className="form-label" htmlFor="mois-url">URL *</label>
              <input
                id="mois-url"
                className="form-control"
                type="url"
                value={form.sourceUrl}
                onChange={(event) => setForm((prev) => ({ ...prev, sourceUrl: event.target.value }))}
                placeholder="https://"
                required
              />
            </div>

            <div className="col-12 col-lg-4">
              <label className="form-label" htmlFor="mois-asset-type">Tipo de ativo *</label>
              <select
                id="mois-asset-type"
                className="form-select"
                value={form.assetType}
                onChange={(event) => setForm((prev) => ({ ...prev, assetType: event.target.value }))}
                required
              >
                <option value="LANDING_PAGE">Landing page</option>
                <option value="VSL">VSL</option>
                <option value="CHECKOUT">Checkout</option>
              </select>
            </div>

            <div className="col-12 col-lg-4">
              <label className="form-label" htmlFor="mois-promise">Promessa principal *</label>
              <input
                id="mois-promise"
                className="form-control"
                value={form.primaryPromise}
                onChange={(event) => setForm((prev) => ({ ...prev, primaryPromise: event.target.value }))}
                required
              />
            </div>

            <div className="col-12 col-lg-4">
              <label className="form-label" htmlFor="mois-awareness">Consciência *</label>
              <select
                id="mois-awareness"
                className="form-select"
                value={form.awarenessStage}
                onChange={(event) => setForm((prev) => ({ ...prev, awarenessStage: event.target.value }))}
                required
              >
                <option value="PROBLEM_AWARE">Problem aware</option>
                <option value="SOLUTION_AWARE">Solution aware</option>
                <option value="PRODUCT_AWARE">Product aware</option>
              </select>
            </div>

            <div className="col-12 col-lg-6">
              <label className="form-label" htmlFor="mois-price">Faixa de preço</label>
              <input
                id="mois-price"
                className="form-control"
                value={form.priceRange}
                onChange={(event) => setForm((prev) => ({ ...prev, priceRange: event.target.value }))}
              />
            </div>

            <div className="col-12 col-lg-6">
              <label className="form-label" htmlFor="mois-format">Formato</label>
              <input
                id="mois-format"
                className="form-control"
                value={form.formatType}
                onChange={(event) => setForm((prev) => ({ ...prev, formatType: event.target.value }))}
              />
            </div>

            <div className="col-12">
              <label className="form-label" htmlFor="mois-notes">Observações</label>
              <textarea
                id="mois-notes"
                className="form-control"
                rows={3}
                value={form.notes}
                onChange={(event) => setForm((prev) => ({ ...prev, notes: event.target.value }))}
              />
            </div>

            <div className="col-12 d-flex flex-wrap justify-content-between align-items-center gap-3">
              <span className="text-secondary small">Workspace: {WORKSPACE_ID}</span>
              <button
                type="submit"
                className="btn btn-primary d-inline-flex align-items-center gap-2"
                disabled={createReference.isPending}
              >
                {createReference.isPending ? <span className="spinner-border spinner-border-sm" aria-hidden="true" /> : null}
                Salvar referência
              </button>
            </div>
          </form>
        </div>
      </section>

      <section className="card border-0 shadow-sm">
        <div className="card-body d-flex flex-column gap-2">
          <h2 className="h6 mb-0">Preview da URL</h2>
          {sourcePreview ? (
            <a href={sourcePreview} target="_blank" rel="noreferrer" className="text-break">
              {sourcePreview}
            </a>
          ) : (
            <p className="text-secondary mb-0">Informe uma URL para habilitar o preview.</p>
          )}
        </div>
      </section>

      <section className="card border-0 shadow-sm">
        <div className="card-body">
          <h2 className="h5">Referências coletadas</h2>
          {referencesQuery.isLoading ? <p className="text-secondary mb-0">Carregando referências...</p> : null}
          {referencesQuery.isError ? <div className="alert alert-danger mb-0">Não foi possível carregar referências.</div> : null}
          {!referencesQuery.isLoading && !referencesQuery.isError ? (
            referencesQuery.data && referencesQuery.data.length > 0 ? (
              <div className="table-responsive">
                <table className="table align-middle mb-0">
                  <thead>
                    <tr>
                      <th>Nicho</th>
                      <th>Tipo</th>
                      <th>Promessa</th>
                      <th>Data</th>
                    </tr>
                  </thead>
                  <tbody>
                    {referencesQuery.data.map((reference) => (
                      <tr key={reference.referenceId}>
                        <td>{reference.niche}</td>
                        <td>{reference.assetType}</td>
                        <td>{reference.primaryPromise}</td>
                        <td>{new Date(reference.createdAt).toLocaleString("pt-BR")}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            ) : (
              <p className="text-secondary mb-0">Nenhuma referência cadastrada até o momento.</p>
            )
          ) : null}
        </div>
      </section>
    </div>
  );
}
