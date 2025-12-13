import type { LeadPortalImagePackageLifecycleStatus } from "../../api/leadPortal/useLeadPortalSubmissions";

export interface StatusDetail {
  label: string;
  badgeClass: string;
  title: string;
  description: string;
  icon: "sparkles" | "loader" | "check" | "alert";
}

export const statusDetails: Record<LeadPortalImagePackageLifecycleStatus, StatusDetail> = {
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
  WATERMARK_PENDING: {
    label: "Aguardando marca d'água",
    badgeClass: "text-bg-primary",
    title: "Fila de demonstração",
    description:
      "As imagens foram geradas e estão aguardando a criação das versões com marca d'água para envio ao lead.",
    icon: "sparkles",
  },
  WATERMARKING: {
    label: "Aplicando marca d'água",
    badgeClass: "text-bg-warning",
    title: "Preparando prévias",
    description:
      "O serviço de tratamento está aplicando a marca d'água nas imagens para gerar a prévia segura ao lead.",
    icon: "loader",
  },
  ZIP_GENERATING: {
    label: "Gerando pacote ZIP",
    badgeClass: "text-bg-info",
    title: "Compactando prévias",
    description:
      "As imagens com marca d'água estão sendo reunidas em um arquivo .zip para envio ao lead.",
    icon: "loader",
  },
  SAMPLE_EMAIL_SENDING: {
    label: "Enviando e-mail de amostra",
    badgeClass: "text-bg-primary",
    title: "Preparando entrega",
    description:
      "O pacote já foi gerado e está na fila de disparo do e-mail com a amostra anexada ao lead.",
    icon: "loader",
  },
  SAMPLE_EMAIL_SENT: {
    label: "Amostra enviada",
    badgeClass: "text-bg-success",
    title: "E-mail encaminhado ao lead",
    description:
      "O lead já recebeu um e-mail com as prévias geradas. Acompanhe as interações e respostas.",
    icon: "check",
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

export function getStatusDetail(status: LeadPortalImagePackageLifecycleStatus): StatusDetail {
  return statusDetails[status] ?? statusDetails.RECEIVED;
}
