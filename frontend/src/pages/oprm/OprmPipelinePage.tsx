import PageTitle from "../../components/PageTitle";
import OprmModuleNavigation from "./OprmModuleNavigation";

const pipelineStages = [
  {
    number: "0",
    title: "Orquestrador de Pesquisa",
    technicalName: "oprmRoutineResearchOrchestrator",
    description:
      "Seleciona automaticamente o próximo nicho CNAE enriquecido com maior score e ainda sem pesquisa de rotina concluída.",
    output: "Nicho marcado como RESEARCH_RUNNING e ciclo de pesquisa criado.",
  },
  {
    number: "1",
    title: "Ciclo de Pesquisa de Rotina",
    technicalName: "oprmRoutineResearchCycle",
    description:
      "Controla a execução completa da pesquisa de rotina do nicho, mantendo CNAE, score, status, contadores e rastreabilidade.",
    output: "Ciclo pai do pipeline pronto para receber as próximas etapas.",
  },
  {
    number: "2",
    title: "Seed de Pesquisa do Nicho",
    technicalName: "oprmNicheResearchSeedBuilder",
    description:
      "Usa IA para transformar o CNAE em nicho operacional, objetos comerciais, suposições iniciais e queries de pesquisa.",
    output: "Seed do nicho e frases de pesquisa para rotina, dores, perguntas e ofertas.",
  },
  {
    number: "3",
    title: "Busca de Fontes",
    technicalName: "oprmSourceSearcher",
    description:
      "Executa as queries planejadas em provedor de busca e registra páginas, documentos e conteúdos públicos candidatos.",
    output: "Fontes candidatas vinculadas ao ciclo e às queries pesquisadas.",
  },
  {
    number: "4",
    title: "Coleta de Fontes",
    technicalName: "oprmSourceFetcher",
    description:
      "Seleciona fontes relevantes e coleta metadados, snippets e trechos curtos sem armazenar HTML completo no MVP.",
    output: "Snapshots leves das fontes para extração de sinais.",
  },
  {
    number: "5",
    title: "Extração de Sinais",
    technicalName: "oprmSignalExtractor",
    description:
      "Extrai sinais estruturados sobre rotina, tarefas comerciais, dores, perguntas, linguagem e oportunidades de mecanismo.",
    output: "Sinais classificados para sustentar a síntese da rotina.",
  },
  {
    number: "6",
    title: "Síntese da Rotina",
    technicalName: "oprmRoutineSynthesizer",
    description:
      "Monta o cartão de rotina do nicho a partir dos sinais extraídos, sem criar oferta, campanha ou landing page.",
    output: "oprm_niche_routine_card com rotina, dores, resultados e evidências.",
  },
  {
    number: "7",
    title: "Gate de Qualidade",
    technicalName: "oprmRoutineQualityGate",
    description:
      "Avalia se o card tem fontes, sinais, especificidade e confiança suficientes para seguir para hipótese comercial.",
    output: "Decisão: pronto para hipótese, precisa de mais pesquisa ou ficou genérico.",
  },
];

export default function OprmPipelinePage() {
  return (
    <div className="d-flex flex-column gap-4">
      <header className="d-flex flex-column gap-2">
        <PageTitle>Pipeline NichoCNAE</PageTitle>
        <p className="text-secondary mb-0">
          Esta tela mostra o pipeline OPRM que transforma um nicho CNAE já
          priorizado em um cartão de rotina pesquisado. Ingestão de mercado,
          cálculo de score e enriquecimento CNAE ficam na aba CNAEs; aqui o foco
          é conhecer como o nicho funciona no dia a dia antes da hipótese
          comercial.
        </p>
      </header>

      <OprmModuleNavigation />

      <section className="card border-0 shadow-sm">
        <div className="card-body">
          <h2 className="h5 mb-2">Fluxo do pipeline de pesquisa da rotina</h2>
          <p className="text-secondary mb-0">
            Entrada: nicho CNAE com score alto. Saída esperada:
            <strong> oprm_niche_routine_card</strong>, que será usado depois pelo
            pipeline de hipótese comercial para trabalhar dor, resultado,
            mecanismo, prova e oferta.
          </p>
        </div>
      </section>

      <section className="row g-3" aria-label="Etapas do pipeline NichoCNAE">
        {pipelineStages.map((stage) => (
          <div className="col-12 col-lg-3" key={stage.number}>
            <article className="card border-0 shadow-sm h-100">
              <div className="card-body d-flex flex-column gap-3">
                <div className="d-flex align-items-start justify-content-between gap-3">
                  <div
                    className="rounded-circle bg-primary text-white d-inline-flex align-items-center justify-content-center fw-bold flex-shrink-0"
                    style={{ width: 44, height: 44 }}
                    aria-hidden="true"
                  >
                    {stage.number}
                  </div>
                  <span className="badge text-bg-light border text-secondary text-wrap">
                    {stage.technicalName}
                  </span>
                </div>
                <div>
                  <h2 className="h5 mb-2">{stage.title}</h2>
                  <p className="text-secondary mb-3">{stage.description}</p>
                  <div className="border-top pt-3">
                    <span className="d-block small fw-semibold text-secondary text-uppercase mb-1">
                      Saída da etapa
                    </span>
                    <p className="mb-0 small">{stage.output}</p>
                  </div>
                </div>
              </div>
            </article>
          </div>
        ))}
      </section>
    </div>
  );
}
