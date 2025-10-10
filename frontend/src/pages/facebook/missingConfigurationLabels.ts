export const missingConfigurationLabels: Record<string, string> = {
  creativeApproval: "Aprovar pelo menos um criativo",
  kpiTargetCpl: "Definir o KPI alvo (CPL)",
  stopLossCpl: "Definir o stop-loss de CPL",
  sampleSize: "Informar o tamanho da amostra",
  startDate: "Definir a data de início",
  endDate: "Definir a data de término",
  journeyTemplate: "Vincular um template de jornada",
  instagramAccount: "Vincular uma conta do Instagram",
};

export function getMissingConfigurationLabel(key: string) {
  return missingConfigurationLabels[key] || "Revise o experimento";
}
