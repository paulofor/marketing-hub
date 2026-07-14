import { useExperimentConstruction } from "../../api/experiment/useExperimentConstruction";

interface ExperimentConstructionTabProps {
  experimentId?: string;
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
              Leitura consolidada do backend sobre a cadeia, tese, oferta,
              validação e ativos que originaram este teste.
            </p>
          </div>
          <span className="badge text-bg-warning align-self-start">
            Fluxo manual
          </span>
        </div>
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
