import { CheckCircle2 } from "lucide-react";
import { useExperimentConstruction } from "../../api/experiment/useExperimentConstruction";

interface ExperimentConstructionTabProps {
  experimentId?: string;
  onSelectTab?: (tab: string) => void;
}

function formatConstructionValue(value: string) {
  const lines = value.split("\n");
  return lines.map((line, index) => (
    <span key={`${line}-${index}`}>
      {line}
      {index < lines.length - 1 ? <br /> : null}
    </span>
  ));
}

export default function ExperimentConstructionTab({
  experimentId,
  onSelectTab,
}: ExperimentConstructionTabProps) {
  const { data, isLoading, error } = useExperimentConstruction(experimentId);

  if (isLoading) {
    return (
      <div className="card">
        <div className="card-body text-muted small">
          Carregando construção do experimento...
        </div>
      </div>
    );
  }

  if (error || !data) {
    return (
      <div className="alert alert-warning mb-0">
        Não foi possível carregar como este experimento foi construído.
      </div>
    );
  }

  return (
    <div className="d-flex flex-column gap-3">
      <div className="border rounded-3 p-3 bg-light">
        <div className="d-flex flex-wrap justify-content-between gap-2">
          <div>
            <h5 className="mb-1">Construção do experimento manual</h5>
            <p className="text-muted small mb-0">
              Cockpit para conduzir o fluxo Nicho/dor → hipótese → MDS → oferta
              → prova → experimento → FEO → funil.
            </p>
          </div>
          <span className="badge text-bg-warning align-self-start">
            Fluxo manual
          </span>
        </div>
      </div>

      <div className="row g-2">
        {data.flowSteps.map((step, index) => (
          <div className="col-12 col-md-6 col-xl-4" key={step.title}>
            <div
              className={`border rounded-3 p-3 h-100 bg-white ${
                step.validated ? "border-success-subtle" : ""
              }`}
            >
              <div className="d-flex align-items-start justify-content-between gap-2">
                <div>
                  <div className="d-flex align-items-center gap-2 mb-2">
                    <span className="badge text-bg-light">{index + 1}</span>
                    {step.validated ? (
                      <span className="badge text-bg-success d-inline-flex align-items-center gap-1">
                        <CheckCircle2 size={14} aria-hidden="true" />
                        {step.validationLabel ?? "Validado"}
                      </span>
                    ) : null}
                  </div>
                  <h6 className="mb-1">{step.title}</h6>
                </div>
                {onSelectTab ? (
                  <button
                    className="btn btn-sm btn-outline-primary"
                    type="button"
                    onClick={() => onSelectTab(step.tab)}
                  >
                    {step.action}
                  </button>
                ) : null}
              </div>
              <p className="small text-muted mb-0">{step.description}</p>
            </div>
          </div>
        ))}
      </div>

      {data.sections.map((section) => (
        <div className="card" key={section.title}>
          <div className="card-body">
            <div className="mb-3">
              <h5 className="card-title mb-1">{section.title}</h5>
              {section.description ? (
                <p className="text-muted small mb-0">{section.description}</p>
              ) : null}
            </div>
            {section.items.length ? (
              <dl className="row mb-0">
                {section.items.map((item, index) => (
                  <div
                    className="col-12 col-lg-6 mb-3"
                    key={`${section.title}-${item.label}-${index}`}
                  >
                    <dt className="text-muted small fw-semibold">
                      {item.label}
                    </dt>
                    <dd className="mb-0">
                      {formatConstructionValue(item.value)}
                    </dd>
                  </div>
                ))}
              </dl>
            ) : (
              <div className="text-muted small">
                Nenhum dado persistido para esta seção.
              </div>
            )}
          </div>
        </div>
      ))}
    </div>
  );
}
