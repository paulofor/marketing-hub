import type { FlowSubmissionImagePackageStatus } from "../../api/leadPortal/useLeadPortalSubmissions";

export interface StatusDetail {
  label: string;
  badgeClass: string;
  title: string;
  description: string;
  icon: "sparkles" | "loader" | "check" | "alert";
}

export const statusDetails: Record<FlowSubmissionImagePackageStatus, StatusDetail> = {
  RECEIVED: {
    label: "Recebido",
    badgeClass: "text-bg-secondary",
    title: "Aguardando entrada na fila",
    description:
      "Pacote recém-registrado pelo Lead Portal. Revise os dados e coloque na fila de geração quando estiver pronto.",
    icon: "sparkles",
  },
  RECENT: {
    label: "Capturado",
    badgeClass: "text-bg-info",
    title: "Pronto para o pipeline",
    description:
      "As referências foram capturadas e estão prontas para serem enviadas ao worker de geração de imagens.",
    icon: "sparkles",
  },
  PROCESSING: {
    label: "Processando",
    badgeClass: "text-bg-warning",
    title: "Geração em andamento",
    description:
      "O worker está criando as variações solicitadas. Acompanhe para garantir que o pacote finalize com sucesso.",
    icon: "loader",
  },
  COMPLETED: {
    label: "Concluído",
    badgeClass: "text-bg-success",
    title: "Pacote finalizado",
    description:
      "O conjunto de imagens foi gerado. Analise o resultado e distribua as variantes aprovadas para a equipe.",
    icon: "check",
  },
  FAILED: {
    label: "Falha ao processar",
    badgeClass: "text-bg-danger",
    title: "Ação necessária",
    description:
      "O worker encontrou um erro ao gerar as imagens. Consulte o motivo para reenviar o pacote ou ajustar o prompt.",
    icon: "alert",
  },
};

export function getStatusDetail(status: FlowSubmissionImagePackageStatus): StatusDetail {
  return statusDetails[status];
}
