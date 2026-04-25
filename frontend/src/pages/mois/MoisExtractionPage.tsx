import { useState } from "react";
import { Link } from "react-router-dom";
import { toast } from "react-toastify";
import PageTitle from "../../components/PageTitle";
import { useMoisExtractionDraft } from "../../api/mois/useMoisSprintTwo";

const REFERENCE_ID = "reference-demo";

export default function MoisExtractionPage() {
  const [form, setForm] = useState({ pain: "", result: "", mechanism: "", proof: "", offer: "", evidenceItems: "" });
  const extractionMutation = useMoisExtractionDraft(REFERENCE_ID);

  async function handleSaveDraft(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    try {
      await extractionMutation.mutateAsync({
        pain: form.pain,
        result: form.result,
        mechanism: form.mechanism,
        proof: form.proof,
        offer: form.offer,
        evidenceItems: form.evidenceItems
          .split("\n")
          .map((item) => item.trim())
          .filter(Boolean),
      });
      toast.success("Rascunho de extração salvo");
    } catch {
      toast.error("Falha ao salvar extração. Verifique os campos e tente novamente.");
    }
  }

  return (
    <div className="d-flex flex-column gap-4">
      <header className="d-flex flex-wrap justify-content-between gap-3">
        <div>
          <PageTitle>Extração guiada MOIS</PageTitle>
          <p className="text-secondary mb-0">Sprint 2: editor DRMP-O com salvamento de rascunho.</p>
        </div>
        <Link className="btn btn-outline-secondary" to="/mois">
          Voltar ao workspace
        </Link>
      </header>

      <form className="row g-3" onSubmit={handleSaveDraft}>
        <div className="col-12 col-lg-6">
          <label className="form-label">Dor *</label>
          <textarea className="form-control" rows={3} value={form.pain} onChange={(event) => setForm((prev) => ({ ...prev, pain: event.target.value }))} required />
        </div>
        <div className="col-12 col-lg-6">
          <label className="form-label">Resultado *</label>
          <textarea className="form-control" rows={3} value={form.result} onChange={(event) => setForm((prev) => ({ ...prev, result: event.target.value }))} required />
        </div>
        <div className="col-12 col-lg-4">
          <label className="form-label">Mecanismo *</label>
          <textarea className="form-control" rows={3} value={form.mechanism} onChange={(event) => setForm((prev) => ({ ...prev, mechanism: event.target.value }))} required />
        </div>
        <div className="col-12 col-lg-4">
          <label className="form-label">Prova *</label>
          <textarea className="form-control" rows={3} value={form.proof} onChange={(event) => setForm((prev) => ({ ...prev, proof: event.target.value }))} required />
        </div>
        <div className="col-12 col-lg-4">
          <label className="form-label">Oferta *</label>
          <textarea className="form-control" rows={3} value={form.offer} onChange={(event) => setForm((prev) => ({ ...prev, offer: event.target.value }))} required />
        </div>
        <div className="col-12">
          <label className="form-label">Evidências (uma por linha)</label>
          <textarea className="form-control" rows={4} value={form.evidenceItems} onChange={(event) => setForm((prev) => ({ ...prev, evidenceItems: event.target.value }))} />
        </div>

        <div className="col-12 d-flex justify-content-end">
          <button type="submit" className="btn btn-primary d-inline-flex align-items-center gap-2" disabled={extractionMutation.isPending}>
            {extractionMutation.isPending ? <span className="spinner-border spinner-border-sm" aria-hidden="true" /> : null}
            Salvar rascunho
          </button>
        </div>
      </form>
    </div>
  );
}
