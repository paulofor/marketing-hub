import { FormEvent, useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { useHypothesis } from "../../api/hypothesis/useHypothesis";
import { useCreateHypothesisVersion } from "../../api/hypothesis/useCreateHypothesisVersion";
import PageTitle from "../../components/PageTitle";
import hypothesisIcon from "../../assets/icons/hypothesis-icon.svg";

type VersionForm = {
  problem: string;
  persona: string;
  promise: string;
  mechanism: string;
  uniqueMechanism: string;
  entrega: string;
  successRule: string;
  offerType: string;
  price: string;
};

const emptyForm: VersionForm = {
  problem: "",
  persona: "",
  promise: "",
  mechanism: "",
  uniqueMechanism: "",
  entrega: "",
  successRule: "",
  offerType: "TRIPWIRE",
  price: "",
};

export default function NewHypothesisVersionPage() {
  const { nicheId, hypothesisId } = useParams();
  const navigate = useNavigate();
  const hypothesisQuery = useHypothesis(nicheId, hypothesisId);
  const createVersion = useCreateHypothesisVersion();
  const [form, setForm] = useState<VersionForm>(emptyForm);

  useEffect(() => {
    const source = hypothesisQuery.data;
    if (!source) return;
    setForm({
      problem: source.problem ?? "",
      persona: source.persona ?? "",
      promise: source.promise ?? "",
      mechanism: source.mechanism ?? "",
      uniqueMechanism: source.uniqueMechanism ?? "",
      entrega: source.entrega ?? "",
      successRule: source.successRule ?? "",
      offerType: source.offerType ?? "TRIPWIRE",
      price: source.price?.toString() ?? "",
    });
  }, [hypothesisQuery.data]);

  const update = (field: keyof VersionForm, value: string) =>
    setForm((current) => ({ ...current, [field]: value }));

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    if (!hypothesisId || !nicheId) return;
    const created = await createVersion.mutateAsync({
      sourceId: hypothesisId,
      ...form,
      price: Number(form.price),
    });
    navigate(`/niches/${nicheId}/hypotheses/${created.id}`);
  };

  if (hypothesisQuery.isLoading) return <p>Carregando...</p>;
  if (!hypothesisQuery.data) return <p>Hipótese não encontrada.</p>;

  return (
    <div>
      <PageTitle icon={hypothesisIcon}>Criar nova versão da hipótese</PageTitle>
      <div className="alert alert-info">
        A hipótese {hypothesisQuery.data.title} será preservada. A nova versão
        continuará vinculada ao produto{" "}
        {hypothesisQuery.data.productName ?? "selecionado"}e nascerá em BACKLOG,
        sem criar experimento, publicar ou liberar mídia.
      </div>
      <form onSubmit={submit} className="card">
        <div className="card-body d-flex flex-column gap-3">
          <label className="form-label">
            Persona
            <input
              className="form-control"
              required
              value={form.persona}
              onChange={(e) => update("persona", e.target.value)}
            />
          </label>
          <label className="form-label">
            Problema
            <textarea
              className="form-control"
              required
              value={form.problem}
              onChange={(e) => update("problem", e.target.value)}
            />
          </label>
          <label className="form-label">
            Promessa
            <textarea
              className="form-control"
              value={form.promise}
              onChange={(e) => update("promise", e.target.value)}
            />
          </label>
          <label className="form-label">
            Mecanismo
            <textarea
              className="form-control"
              value={form.mechanism}
              onChange={(e) => update("mechanism", e.target.value)}
            />
          </label>
          <label className="form-label">
            Mecanismo único
            <textarea
              className="form-control"
              value={form.uniqueMechanism}
              onChange={(e) => update("uniqueMechanism", e.target.value)}
            />
          </label>
          <label className="form-label">
            Entrega
            <textarea
              className="form-control"
              required
              value={form.entrega}
              onChange={(e) => update("entrega", e.target.value)}
            />
          </label>
          <label className="form-label">
            Regra de sucesso
            <textarea
              className="form-control"
              value={form.successRule}
              onChange={(e) => update("successRule", e.target.value)}
            />
          </label>
          <div className="row g-3">
            <label className="form-label col-md-6">
              Tipo de oferta
              <select
                className="form-select"
                value={form.offerType}
                onChange={(e) => update("offerType", e.target.value)}
              >
                <option value="TRIPWIRE">Oferta paga de entrada</option>
                <option value="LEAD">Captação de lead</option>
              </select>
            </label>
            <label className="form-label col-md-6">
              Preço (R$)
              <input
                className="form-control"
                type="number"
                min="0.01"
                step="0.01"
                required
                value={form.price}
                onChange={(e) => update("price", e.target.value)}
              />
            </label>
          </div>
          <div className="d-flex gap-2">
            <button
              className="btn btn-primary"
              disabled={createVersion.isPending}
            >
              Criar versão em BACKLOG
            </button>
            <Link
              className="btn btn-outline-secondary"
              to={`/niches/${nicheId}/hypotheses/${hypothesisId}`}
            >
              Cancelar
            </Link>
          </div>
        </div>
      </form>
    </div>
  );
}
