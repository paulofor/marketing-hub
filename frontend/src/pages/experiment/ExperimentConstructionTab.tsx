import { CheckCircle2 } from "lucide-react";
import { useState } from "react";
import { useExperimentConstruction } from "../../api/experiment/useExperimentConstruction";
import {
  type PersonalizedSampleFunnel,
  type PersonalizedSampleFunnelTemplate,
  useCreatePersonalizedSampleFunnel,
} from "../../api/product-ai/useProductAiExperimentPreparation";
import type { ProductAiSubtype } from "../../api/experiment/useExperiments";

interface ExperimentConstructionTabProps {
  experimentId?: string;
  productAiSubtype?: ProductAiSubtype | null;
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
  productAiSubtype,
  onSelectTab,
}: ExperimentConstructionTabProps) {
  const { data, isLoading, error } = useExperimentConstruction(experimentId);
  const createFunnel = useCreatePersonalizedSampleFunnel();
  const [template, setTemplate] = useState<PersonalizedSampleFunnelTemplate>(
    "SOCIAL_MEDIA_MICRO_SAMPLE",
  );
  const [createdFunnel, setCreatedFunnel] =
    useState<PersonalizedSampleFunnel | null>(null);

  const handleCreateFunnel = async () => {
    if (!experimentId) return;
    const result = await createFunnel.mutateAsync({ experimentId, template });
    setCreatedFunnel(result);
  };

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
      {productAiSubtype === "AI_PERSONALIZED_SAMPLE" ? (
        <div className="card border-primary-subtle">
          <div className="card-body">
            <h5 className="card-title">Funil reutilizável de microamostra</h5>
            <p className="text-muted small">
              Escolha explicitamente o modelo de coleta. O mesmo contrato pode
              ser usado por outros experimentos sem duplicar páginas ou regras
              de tracking.
            </p>
            <div className="d-flex flex-column flex-md-row gap-2 align-items-md-end">
              <label className="flex-grow-1">
                <span className="form-label">Template do funil</span>
                <select
                  className="form-select"
                  value={template}
                  onChange={(event) =>
                    setTemplate(
                      event.target.value as PersonalizedSampleFunnelTemplate,
                    )
                  }
                >
                  <option value="SOCIAL_MEDIA_MICRO_SAMPLE">
                    Microamostra social — 3 decisões + contato
                  </option>
                  <option value="GENERIC">
                    Amostra personalizada genérica
                  </option>
                  <option value="DECORATION_BY_PHOTO">
                    Decoração por foto
                  </option>
                </select>
              </label>
              <button
                className="btn btn-primary"
                type="button"
                disabled={createFunnel.isPending}
                onClick={handleCreateFunnel}
              >
                {createFunnel.isPending
                  ? "Preparando..."
                  : "Criar ou atualizar funil"}
              </button>
            </div>
            {createFunnel.isError ? (
              <div className="alert alert-danger mt-3 mb-0">
                Não foi possível preparar o funil. Revise o experimento e tente
                novamente.
              </div>
            ) : null}
            {createdFunnel ? (
              <div className="alert alert-success mt-3 mb-0">
                Funil aprovado e publicado:{" "}
                <strong>{createdFunnel.leadPortalFlowSlug}</strong>. Campos:{" "}
                {createdFunnel.dataKeys.join(", ")}.
              </div>
            ) : null}
          </div>
        </div>
      ) : null}
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
