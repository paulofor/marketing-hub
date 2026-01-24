export interface MissingConfigurationInfo {
  label: string;
  helperText?: string;
}

const missingConfigurationInfo: Record<string, MissingConfigurationInfo> = {
  creativeApproval: { label: "Aprovar pelo menos um criativo" },
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
      "Audiências são os públicos que o worker usa nos conjuntos de anúncios. Pelo menos um público do nicho (ou vinculado à mesma hipótese) precisa estar marcado como Aprovado na aba Públicos para liberar a campanha.",
  },
};

export function getMissingConfigurationInfo(key: string): MissingConfigurationInfo {
  return missingConfigurationInfo[key] || { label: "Revise o experimento" };
}

export function getMissingConfigurationLabel(key: string) {
  return getMissingConfigurationInfo(key).label;
}
