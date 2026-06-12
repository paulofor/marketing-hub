export interface HypothesisPipelineStageConfig {
  slug: "pain" | "result" | "mechanism" | "proof" | "offer";
  number: number;
  stageCode: string;
  title: string;
  startLabel: string;
  startedToast: string;
  startErrorToast: string;
  loadingLabel: string;
  emptyMessage: string;
  description: string;
  blockedMessage?: string;
}

export const HYPOTHESIS_PIPELINE_STAGES: HypothesisPipelineStageConfig[] = [
  {
    slug: "pain",
    number: 1,
    stageCode: "hypothesis-pain",
    title: "Dor do nicho",
    startLabel: "Iniciar construção da dor",
    startedToast: "Etapa Dor iniciada",
    startErrorToast: "Não foi possível iniciar a etapa Dor",
    loadingLabel: "Carregando execuções de dor...",
    emptyMessage:
      "Nenhuma execução de dor iniciada para este nicho. Clique no botão acima para criar o job e acompanhar o resultado.",
    description:
      "Inicie a construção auditável da dor antes de avançar para resultado, mecanismo, prova e oferta.",
  },
  {
    slug: "result",
    number: 2,
    stageCode: "hypothesis-result",
    title: "Resultado desejado",
    startLabel: "Iniciar construção do resultado",
    startedToast: "Etapa Resultado iniciada",
    startErrorToast:
      "Não foi possível iniciar a etapa Resultado. Conclua a dor antes de avançar.",
    loadingLabel: "Carregando execuções de resultado...",
    emptyMessage:
      "Nenhuma execução de resultado iniciada para este nicho. Depois de concluir a dor, clique no botão acima para criar o job e acompanhar o resultado.",
    description:
      "Transforme a dor validada em um resultado claro, desejável e plausível antes de avançar para mecanismo, prova e oferta.",
    blockedMessage: "Conclua a Dor antes de iniciar Resultado",
  },
  {
    slug: "mechanism",
    number: 3,
    stageCode: "hypothesis-mechanism",
    title: "Mecanismo",
    startLabel: "Iniciar construção do mecanismo",
    startedToast: "Etapa Mecanismo iniciada",
    startErrorToast:
      "Não foi possível iniciar a etapa Mecanismo. Conclua o resultado antes de avançar.",
    loadingLabel: "Carregando execuções de mecanismo...",
    emptyMessage:
      "Nenhuma execução de mecanismo iniciada para este nicho. Depois de concluir o resultado, clique no botão acima para criar o job e acompanhar o mecanismo.",
    description:
      "Converta o resultado desejado em um mecanismo plausível antes de avançar para prova e oferta.",
    blockedMessage: "Conclua o Resultado antes de iniciar Mecanismo",
  },
  {
    slug: "proof",
    number: 4,
    stageCode: "hypothesis-proof",
    title: "Prova",
    startLabel: "Iniciar construção da prova",
    startedToast: "Etapa Prova iniciada",
    startErrorToast:
      "Não foi possível iniciar a etapa Prova. Conclua o mecanismo antes de avançar.",
    loadingLabel: "Carregando execuções de prova...",
    emptyMessage:
      "Nenhuma execução de prova iniciada para este nicho. Depois de concluir o mecanismo, clique no botão acima para criar o job e acompanhar a prova.",
    description:
      "Transforme o mecanismo em prova clara e crível antes de empacotar a oferta.",
    blockedMessage: "Conclua o Mecanismo antes de iniciar Prova",
  },
  {
    slug: "offer",
    number: 5,
    stageCode: "hypothesis-offer",
    title: "Oferta",
    startLabel: "Iniciar construção da oferta",
    startedToast: "Etapa Oferta iniciada",
    startErrorToast:
      "Não foi possível iniciar a etapa Oferta. Conclua a prova antes de avançar.",
    loadingLabel: "Carregando execuções de oferta...",
    emptyMessage:
      "Nenhuma execução de oferta iniciada para este nicho. Depois de concluir a prova, clique no botão acima para criar o job e acompanhar a oferta.",
    description:
      "Empacote mecanismo, prova prometida e promessa central em uma oferta clara para venda.",
    blockedMessage: "Conclua a Prova antes de iniciar Oferta",
  },
];
