const statusLabels: Record<string, string> = {
  COMUNICACAO_E_JORNADA: "Comunicação e jornada",
  CONSTRUCAO_E_APROVACAO: "Construção e aprovação",
  IDEIA_PRIORIZADA_PARA_TESTE: "Ideia priorizada para teste",
  VALIDACAO_COMERCIAL: "Validação comercial",
};

export function formatCommercialStatus(value?: string | null) {
  if (!value?.trim()) return "Sem status";
  const normalized = value.trim().toUpperCase();
  if (statusLabels[normalized]) return statusLabels[normalized];
  return value
    .trim()
    .toLocaleLowerCase("pt-BR")
    .replace(/_/g, " ")
    .replace(/^./, (letter: string) => letter.toLocaleUpperCase("pt-BR"));
}
