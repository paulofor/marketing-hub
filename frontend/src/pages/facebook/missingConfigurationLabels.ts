export interface MissingConfigurationInfo {
  label: string;
  helperText?: string;
}

const missingConfigurationInfo: Record<string, MissingConfigurationInfo> = {
  creativeApproval: { label: "Aprovar pelo menos um criativo" },
  landingDestination: { label: "Aprovar a landing para definir URL de destino" },
  geraSalesPagePipeline: {
    label: "Publicar página de venda pelo GeraSalesPage",
    helperText:
      "Experimentos low-ticket precisam de uma página de venda publicada e auditada pelo GeraSalesPage antes de entrar em campanha.",
  },
  salesPageAdDestination: {
    label: "Apontar o anúncio para a página de venda",
    helperText:
      "O anúncio low-ticket deve enviar o clique para a página de venda publicada, não direto para o checkout.",
  },
  salesPageAnalyticsCollectors: {
    label: "Republicar página com coletores de métricas",
    helperText:
      "A página de venda precisa coletar page_view, page_load_metric, section_view_time e checkout_click antes de receber tráfego pago.",
  },
  facebookPixel: { label: "Configurar pixel do nicho para venda low-ticket" },
  kpiTargetCpl: { label: "Definir o KPI alvo (CPL)" },
  stopLossCpl: { label: "Definir o stop-loss de CPL" },
  sampleSize: { label: "Informar o tamanho da amostra" },
  startDate: { label: "Definir a data de início" },
  endDate: { label: "Definir a data de término" },
  journeyTemplate: { label: "Vincular um template de jornada" },
  instagramAccount: { label: "Vincular uma conta do Instagram" },
  approvedAudiences: {
    label: "Aprovar pelo menos uma audiência para o nicho",
    helperText:
      "Audiências são os públicos que o worker usa nos conjuntos de anúncios. Pelo menos um público do nicho (ou vinculado à mesma hipótese) precisa estar marcado como Aprovado na aba Públicos do experimento para liberar a campanha.",
  },
  targetingSelections: {
    label: "Salvar pelo menos uma segmentação com ID da Meta",
    helperText:
      "Selecione interesses, cargos ou comportamentos na aba Segmentação, salve a lista e execute o fluxo simples para liberar o público alvo.",
  },
};

export function getMissingConfigurationInfo(key: string): MissingConfigurationInfo {
  return missingConfigurationInfo[key] || { label: "Revise o experimento" };
}

export function getMissingConfigurationLabel(key: string) {
  return getMissingConfigurationInfo(key).label;
}
