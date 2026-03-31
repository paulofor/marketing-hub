import { useMemo, useState } from "react";
import { toast } from "react-toastify";
import * as Tabs from "@radix-ui/react-tabs";
import type { Hypothesis } from "../../api/hypothesis/useHypothesisBoard";

type ContentGenerationSectionKey =
  | "campaign-angle"
  | "ad-copy"
  | "image-prompt"
  | "landing-copy"
  | "landing-layout";

interface ContentGenerationSection {
  key: ContentGenerationSectionKey;
  label: string;
  description: string;
  defaultQuantity: number;
}

const CONTENT_GENERATION_SECTIONS: ContentGenerationSection[] = [
  {
    key: "campaign-angle",
    label: "Angulo da Campanha",
    description:
      "Defina variações de narrativa para explorar novas entradas de comunicação.",
    defaultQuantity: 3,
  },
  {
    key: "ad-copy",
    label: "Texto do Anuncio",
    description:
      "Gere textos com foco em promessa, objeções e chamada para ação.",
    defaultQuantity: 5,
  },
  {
    key: "image-prompt",
    label: "Prompt da Imagem",
    description:
      "Crie prompts para orientar a geração de criativos visuais coerentes com o ângulo.",
    defaultQuantity: 4,
  },
  {
    key: "landing-copy",
    label: "Texto da Landing",
    description:
      "Produza blocos de copy para título, prova, benefícios e CTA da landing.",
    defaultQuantity: 4,
  },
  {
    key: "landing-layout",
    label: "Layout da Landing",
    description:
      "Sugira estruturas visuais e ordem de seções para a página de conversão.",
    defaultQuantity: 2,
  },
];

interface ExperimentContentGenerationTabProps {
  hypothesis?: Hypothesis;
}

function getFrameworkSummary(value?: string) {
  return value?.trim() || "Resumo ainda não preenchido na hipótese.";
}

export default function ExperimentContentGenerationTab({
  hypothesis,
}: ExperimentContentGenerationTabProps) {
  const [activeSection, setActiveSection] =
    useState<ContentGenerationSectionKey>(CONTENT_GENERATION_SECTIONS[0].key);

  const frameworkContext = useMemo(
    () => ({
      pain: getFrameworkSummary(hypothesis?.framework?.pain?.summary),
      result: getFrameworkSummary(hypothesis?.framework?.result?.summary),
      offer: getFrameworkSummary(hypothesis?.framework?.offer?.summary),
    }),
    [
      hypothesis?.framework?.offer?.summary,
      hypothesis?.framework?.pain?.summary,
      hypothesis?.framework?.result?.summary,
    ],
  );

  const currentSection =
    CONTENT_GENERATION_SECTIONS.find(
      (section) => section.key === activeSection,
    ) ?? CONTENT_GENERATION_SECTIONS[0];

  const handleRequest = () => {
    toast.info(
      "Estrutura criada. Assim que os prompts forem definidos, conectaremos esta solicitação ao Worker IA.",
    );
  };

  return (
    <div className="mt-3 d-flex flex-column gap-3">
      <section className="card">
        <div className="card-body">
          <h5 className="card-title mb-1">Contexto do framework da hipótese</h5>
          <p className="text-muted mb-3">
            Esses resumos de Dor-Resultado-Oferta serão enviados junto com cada
            solicitação para orientar o Worker IA.
          </p>
          <div className="row g-3">
            <div className="col-md-4">
              <div className="border rounded p-3 h-100 bg-light-subtle">
                <h6 className="mb-2">Resumo da dor</h6>
                <p className="mb-0 small">{frameworkContext.pain}</p>
              </div>
            </div>
            <div className="col-md-4">
              <div className="border rounded p-3 h-100 bg-light-subtle">
                <h6 className="mb-2">Resumo do resultado</h6>
                <p className="mb-0 small">{frameworkContext.result}</p>
              </div>
            </div>
            <div className="col-md-4">
              <div className="border rounded p-3 h-100 bg-light-subtle">
                <h6 className="mb-2">Resumo da oferta</h6>
                <p className="mb-0 small">{frameworkContext.offer}</p>
              </div>
            </div>
          </div>
        </div>
      </section>

      <Tabs.Root
        value={activeSection}
        onValueChange={(value) =>
          setActiveSection(value as ContentGenerationSectionKey)
        }
      >
        <Tabs.List className="nav nav-pills flex-wrap gap-2">
          {CONTENT_GENERATION_SECTIONS.map((section) => (
            <Tabs.Trigger
              key={section.key}
              value={section.key}
              className="btn btn-outline-primary"
            >
              {section.label}
            </Tabs.Trigger>
          ))}
        </Tabs.List>

        {CONTENT_GENERATION_SECTIONS.map((section) => (
          <Tabs.Content key={section.key} value={section.key} className="mt-3">
            <section className="card">
              <div className="card-body">
                <div className="d-flex justify-content-between align-items-start flex-wrap gap-2">
                  <div>
                    <h5 className="card-title mb-1">{section.label}</h5>
                    <p className="text-muted mb-0">{section.description}</p>
                  </div>
                  <span className="badge text-bg-light">
                    Estrutura pronta para prompt
                  </span>
                </div>

                <div className="row g-3 mt-1">
                  <div className="col-md-4">
                    <label
                      htmlFor={`quantity-${section.key}`}
                      className="form-label"
                    >
                      Quantidade sugerida <span className="text-danger">*</span>
                    </label>
                    <input
                      id={`quantity-${section.key}`}
                      className="form-control"
                      type="number"
                      min={1}
                      defaultValue={section.defaultQuantity}
                      title="Define quantas variações o Worker IA deve gerar nesta aba."
                    />
                  </div>
                  <div className="col-12">
                    <label
                      htmlFor={`prompt-${section.key}`}
                      className="form-label"
                    >
                      Prompt da geração <span className="text-danger">*</span>
                    </label>
                    <textarea
                      id={`prompt-${section.key}`}
                      className="form-control"
                      rows={4}
                      placeholder="Você enviará este prompt depois. A estrutura já está preparada para receber o texto."
                      title="Campo que será enviado ao Worker IA com o contexto do framework da hipótese."
                    />
                    <div className="form-text">
                      A solicitação incluirá automaticamente os resumos de Dor,
                      Resultado e Oferta da hipótese vinculada.
                    </div>
                  </div>
                </div>

                <div className="d-flex justify-content-end mt-4">
                  <button
                    type="button"
                    className="btn btn-primary"
                    onClick={handleRequest}
                  >
                    Solicitar geração por IA
                  </button>
                </div>
              </div>
            </section>
          </Tabs.Content>
        ))}
      </Tabs.Root>

      <small className="text-muted">
        Aba atual: <strong>{currentSection.label}</strong>
      </small>
    </div>
  );
}
