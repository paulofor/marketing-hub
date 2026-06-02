import PageTitle from "../../components/PageTitle";
import OprmModuleNavigation from "./OprmModuleNavigation";

const pipelineStages = [
  {
    number: "1",
    title: "Ingestão de Mercado",
    description:
      "Coleta e organiza os dados de mercado que sustentam a leitura de CNAEs e volume de oportunidade.",
  },
  {
    number: "2",
    title: "Score OPRM",
    description:
      "Prioriza os CNAEs com maior potencial para gerar oportunidades vendáveis, usando o Score OPRM como referência.",
  },
  {
    number: "3",
    title: "Enriquecimento Comercial",
    description:
      "Transforma os CNAEs priorizados em candidatos de nicho com dor, resultado e mecanismo para decisão humana.",
  },
];

export default function OprmPipelinePage() {
  return (
    <div className="d-flex flex-column gap-4">
      <header className="d-flex flex-column gap-2">
        <PageTitle>Pipeline OPRM</PageTitle>
        <p className="text-secondary mb-0">
          Visão inicial das etapas do pipeline OPRM. Por enquanto, esta tela
          apresenta apenas os cards das fases principais para orientar a próxima
          evolução do fluxo.
        </p>
      </header>

      <OprmModuleNavigation />

      <section className="row g-3" aria-label="Etapas do pipeline OPRM">
        {pipelineStages.map((stage) => (
          <div className="col-12 col-lg-4" key={stage.number}>
            <article className="card border-0 shadow-sm h-100">
              <div className="card-body d-flex flex-column gap-3">
                <div
                  className="rounded-circle bg-primary text-white d-inline-flex align-items-center justify-content-center fw-bold"
                  style={{ width: 44, height: 44 }}
                  aria-hidden="true"
                >
                  {stage.number}
                </div>
                <div>
                  <h2 className="h5 mb-2">{stage.title}</h2>
                  <p className="text-secondary mb-0">{stage.description}</p>
                </div>
              </div>
            </article>
          </div>
        ))}
      </section>
    </div>
  );
}
