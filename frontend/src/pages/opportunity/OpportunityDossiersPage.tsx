import { FormEvent, useState } from "react";
import PageTitle from "../../components/PageTitle";
import {
  CreateDossier,
  useCreateOpportunityDossier,
  useDossierAction,
  useOpportunityDossiers,
} from "../../api/opportunityDossiers";

const initial: CreateDossier = {
  title: "",
  ownerAgentKey: "ARGOS",
  targetAudience: "",
  mainPain: "",
  referenceProduct: "",
  aiAdvantage: "",
  proposedOffer: "",
  deliveryModel: "",
  knownRisks: "",
  experimentRecommendation: "",
};
const labels: Record<string, string> = {
  RESEARCHING: "Em pesquisa",
  UNDER_REVIEW: "Em parecer",
  READY_FOR_TEST: "Pronto para decisão",
  APPROVED: "Aprovado",
  DISCARDED: "Descartado",
  CONVERTED_TO_PLAN: "Convertido em plano",
};

export default function OpportunityDossiersPage() {
  const dossiers = useOpportunityDossiers();
  const create = useCreateOpportunityDossier();
  const action = useDossierAction();
  const [form, setForm] = useState(initial);
  const [selected, setSelected] = useState<number | null>(null);
  const [evidenceUrl, setEvidenceUrl] = useState("");
  const [evidenceSummary, setEvidenceSummary] = useState("");
  const current =
    dossiers.data?.find((item) => item.id === selected) ?? dossiers.data?.[0];
  const submit = (event: FormEvent) => {
    event.preventDefault();
    create.mutate(form, {
      onSuccess: (item) => {
        setSelected(item.id);
        setForm(initial);
      },
    });
  };
  const field = (key: keyof CreateDossier, value: string) =>
    setForm((previous) => ({ ...previous, [key]: value }));
  return (
    <div className="container-fluid py-4">
      <PageTitle>Oportunidades</PageTitle>
      <p className="text-muted">
        Argos pesquisa; Atena, Psique, Plutus e Hermes avaliam; somente uma
        decisão humana converte a oportunidade em Plano Comercial.
      </p>
      <div className="row g-4">
        <div className="col-xl-4">
          <form className="card card-body" onSubmit={submit}>
            <h2 className="h5">Novo dossiê</h2>
            {(
              [
                ["title", "Título"],
                ["targetAudience", "Público"],
                ["mainPain", "Dor principal"],
                ["referenceProduct", "Produto comprovado de referência"],
                ["aiAdvantage", "Como a IA entrega melhor"],
              ] as const
            ).map(([key, label]) => (
              <label className="form-label" key={key}>
                {label} *
                <textarea
                  className="form-control"
                  required
                  value={String(form[key] ?? "")}
                  onChange={(e) => field(key, e.target.value)}
                />
              </label>
            ))}
            <label className="form-label">
              Oferta preliminar
              <textarea
                className="form-control"
                value={form.proposedOffer}
                onChange={(e) => field("proposedOffer", e.target.value)}
              />
            </label>
            <label className="form-label">
              Experimento recomendado
              <textarea
                className="form-control"
                value={form.experimentRecommendation}
                onChange={(e) =>
                  field("experimentRecommendation", e.target.value)
                }
              />
            </label>
            <button className="btn btn-primary" disabled={create.isPending}>
              {create.isPending && (
                <span className="spinner-border spinner-border-sm me-2" />
              )}
              Cadastrar dossiê
            </button>
          </form>
        </div>
        <div className="col-xl-8">
          <div className="row g-3">
            {dossiers.data?.map((item) => (
              <div className="col-md-6" key={item.id}>
                <button
                  className="card card-body text-start w-100 h-100"
                  onClick={() => setSelected(item.id)}
                >
                  <strong>{item.title}</strong>
                  <span>{labels[item.status]}</span>
                  <small>Responsável: {item.ownerAgentKey}</small>
                </button>
              </div>
            ))}
          </div>
          {dossiers.isLoading && (
            <div className="spinner-border" aria-label="Carregando" />
          )}
          {current && (
            <section className="card card-body mt-4">
              <div className="d-flex justify-content-between gap-3 flex-wrap">
                <div>
                  <h2 className="h4">{current.title}</h2>
                  <span className="badge text-bg-secondary">
                    {labels[current.status]}
                  </span>
                </div>
                <div className="d-flex gap-2">
                  {current.status === "RESEARCHING" && (
                    <button
                      className="btn btn-outline-primary"
                      disabled={action.isPending}
                      onClick={() =>
                        action.mutate({
                          id: current.id,
                          path: "status",
                          payload: {
                            status: "UNDER_REVIEW",
                            decidedBy: "USER",
                          },
                        })
                      }
                    >
                      Solicitar pareceres
                    </button>
                  )}
                  {current.status === "READY_FOR_TEST" && (
                    <button
                      className="btn btn-success"
                      disabled={action.isPending}
                      onClick={() =>
                        action.mutate({
                          id: current.id,
                          path: "status",
                          payload: { status: "APPROVED", decidedBy: "USER" },
                        })
                      }
                    >
                      Aprovar oportunidade
                    </button>
                  )}
                  {current.status === "APPROVED" && (
                    <button
                      className="btn btn-primary"
                      disabled={action.isPending}
                      onClick={() =>
                        action.mutate({
                          id: current.id,
                          path: "convert",
                          payload: { decidedBy: "USER" },
                        })
                      }
                    >
                      Converter em Plano Comercial
                    </button>
                  )}
                </div>
              </div>
              <hr />
              <h3 className="h6">Evidências ({current.evidence.length})</h3>
              {current.evidence.map((e) => (
                <p key={e.id}>
                  <a href={e.sourceUrl} target="_blank" rel="noreferrer">
                    Fonte
                  </a>{" "}
                  — {e.summary}
                </p>
              ))}
              {current.status === "RESEARCHING" && (
                <form
                  className="border rounded p-3 mb-3"
                  onSubmit={(event) => {
                    event.preventDefault();
                    action.mutate(
                      {
                        id: current.id,
                        path: "evidence",
                        payload: {
                          sourceUrl: evidenceUrl,
                          summary: evidenceSummary,
                          createdBy: "USER",
                        },
                      },
                      {
                        onSuccess: () => {
                          setEvidenceUrl("");
                          setEvidenceSummary("");
                        },
                      },
                    );
                  }}
                >
                  <div className="row g-2">
                    <label className="form-label col-md-5">
                      Fonte *
                      <input
                        className="form-control"
                        type="url"
                        required
                        value={evidenceUrl}
                        onChange={(event) => setEvidenceUrl(event.target.value)}
                      />
                    </label>
                    <label className="form-label col-md-7">
                      Evidência observada *
                      <input
                        className="form-control"
                        required
                        value={evidenceSummary}
                        onChange={(event) =>
                          setEvidenceSummary(event.target.value)
                        }
                      />
                    </label>
                  </div>
                  <button
                    className="btn btn-outline-secondary"
                    disabled={action.isPending}
                  >
                    {action.isPending && (
                      <span className="spinner-border spinner-border-sm me-2" />
                    )}
                    Adicionar evidência
                  </button>
                </form>
              )}
              <h3 className="h6">Pareceres</h3>
              <div className="row g-2">
                {current.reviews.map((r) => (
                  <div className="col-md-6" key={r.id}>
                    <div className="border rounded p-2">
                      <strong>{r.agentKey}</strong>
                      <div>
                        {r.executionStatus === "RUNNING"
                          ? "Trabalhando"
                          : r.executionStatus === "FAILED"
                            ? "Bloqueado"
                            : r.completedAt
                              ? r.decision
                              : "Aguardando"}
                      </div>
                      <small>{r.recommendation}</small>
                      {r.errorMessage && (
                        <small className="d-block text-danger">
                          {r.errorMessage}
                        </small>
                      )}
                      {(r.executionStatus === "FAILED" ||
                        r.executionStatus === "PENDING") &&
                        !r.completedAt && (
                          <button
                            className="btn btn-sm btn-outline-primary mt-2"
                            disabled={action.isPending}
                            onClick={() =>
                              action.mutate({
                                id: current.id,
                                path: `reviews/${r.agentKey}/requeue`,
                                payload: {},
                              })
                            }
                          >
                            {action.isPending && (
                              <span className="spinner-border spinner-border-sm me-2" />
                            )}
                            Reenfileirar {r.agentKey}
                          </button>
                        )}
                      <small className="d-block text-muted">
                        Execução #{r.id}
                      </small>
                    </div>
                  </div>
                ))}
              </div>
              {current.convertedPlanId && (
                <p className="alert alert-success mt-3">
                  Plano Comercial #{current.convertedPlanId} criado e vinculado.
                </p>
              )}
            </section>
          )}
        </div>
      </div>
    </div>
  );
}
