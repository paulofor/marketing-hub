import type { OprmRoutineCard } from "../../api/oprm/useOprmRoutineSynthesizerDetail";

export type RoutineValueBlockId =
  | "before-service"
  | "during-service"
  | "after-service"
  | "between-clients-admin"
  | "acquisition-retention"
  | "pains-risks"
  | "product-opportunities";

export interface RoutineValueBlock {
  id: RoutineValueBlockId;
  title: string;
  businessQuestion: string;
  opportunityHint: string;
  items: string[];
  emptyLabel: string;
}

interface RoutineValueBlockDefinition {
  id: RoutineValueBlockId;
  title: string;
  businessQuestion: string;
  opportunityHint: string;
  emptyLabel: string;
  payloadKeys: string[];
  fallbackKeywords: string[];
}

const valueBlockDefinitions: RoutineValueBlockDefinition[] = [
  {
    id: "before-service",
    title: "Antes do atendimento",
    businessQuestion:
      "O que o profissional precisa organizar para o cliente chegar?",
    opportunityHint:
      "Produtos digitais podem reduzir faltas, dúvidas e esforço de preparo.",
    emptyLabel: "Sem tarefas específicas antes do atendimento.",
    payloadKeys: [
      "beforeServiceTasks",
      "beforeAttendanceTasks",
      "tasksBeforeService",
      "preServiceTasks",
      "antesDoAtendimento",
      "beforeServiceSummary",
    ],
    fallbackKeywords: [
      "agenda",
      "agendar",
      "marcar",
      "confirmar",
      "confirmação",
      "lembrete",
      "preparar",
      "preparo",
      "orçamento",
      "triagem",
      "briefing",
      "anamnese",
      "separar",
    ],
  },
  {
    id: "during-service",
    title: "Durante o atendimento",
    businessQuestion:
      "Onde o serviço exige execução, decisão e comunicação com o cliente?",
    opportunityHint:
      "Oportunidades aparecem quando há padrão replicável, checklist ou roteiro.",
    emptyLabel: "Sem tarefas específicas durante o atendimento.",
    payloadKeys: [
      "duringServiceTasks",
      "duringAttendanceTasks",
      "tasksDuringService",
      "serviceExecutionTasks",
      "duranteOAtendimento",
      "duringServiceSummary",
    ],
    fallbackKeywords: [
      "atendimento",
      "atender",
      "executar",
      "execução",
      "serviço",
      "cliente",
      "orientar",
      "procedimento",
      "entrega",
      "realizar",
      "aplicar",
      "acompanhar",
    ],
  },
  {
    id: "after-service",
    title: "Depois do atendimento",
    businessQuestion:
      "O que precisa acontecer para gerar retorno, recompra ou indicação?",
    opportunityHint:
      "Produtos digitais fortes ajudam o cliente a voltar, avaliar e recomendar.",
    emptyLabel: "Sem tarefas específicas depois do atendimento.",
    payloadKeys: [
      "afterServiceTasks",
      "afterAttendanceTasks",
      "tasksAfterService",
      "postServiceTasks",
      "depoisDoAtendimento",
      "afterServiceSummary",
    ],
    fallbackKeywords: [
      "pós",
      "depois",
      "retorno",
      "recompra",
      "feedback",
      "avaliação",
      "avaliar",
      "depoimento",
      "cobrar",
      "pagamento",
      "recibo",
      "indicação",
      "fidelizar",
    ],
  },
  {
    id: "between-clients-admin",
    title: "Administração entre clientes",
    businessQuestion:
      "Quais controles consomem tempo fora do atendimento pago?",
    opportunityHint:
      "Aqui surgem guias, planilhas, calendários e processos simples vendáveis.",
    emptyLabel: "Sem tarefas administrativas específicas entre clientes.",
    payloadKeys: [
      "betweenClientsAdministrationTasks",
      "betweenClientsAdminTasks",
      "administrationBetweenClients",
      "adminBetweenClients",
      "administracaoEntreClientes",
      "betweenClientsAdministrationSummary",
    ],
    fallbackKeywords: [
      "administr",
      "financeiro",
      "estoque",
      "compra",
      "fornecedor",
      "nota",
      "controle",
      "organizar",
      "planejar",
      "relatório",
      "caixa",
      "precificação",
      "materiais",
    ],
  },
  {
    id: "acquisition-retention",
    title: "Aquisição/fidelização",
    businessQuestion: "Como o profissional atrai, recupera e mantém clientes?",
    opportunityHint:
      "A dor comercial vira produto quando existe canal, mensagem e cadência.",
    emptyLabel: "Sem sinais específicos de aquisição ou fidelização.",
    payloadKeys: [
      "acquisitionRetentionTasks",
      "customerAcquisitionRetentionTasks",
      "marketingAndRetentionTasks",
      "acquisitionAndLoyaltyTasks",
      "aquisicaoFidelizacao",
      "acquisitionRetentionSummary",
      "channelsSummary",
      "customerBehaviorSummary",
    ],
    fallbackKeywords: [
      "divulgar",
      "instagram",
      "whatsapp",
      "rede social",
      "indicação",
      "captar",
      "cliente novo",
      "reativar",
      "promoção",
      "fideliza",
      "conteúdo",
      "postar",
      "vender",
      "pacote",
    ],
  },
  {
    id: "pains-risks",
    title: "Dores e riscos observados",
    businessQuestion:
      "Que dor reduz renda, energia, previsibilidade ou confiança?",
    opportunityHint:
      "Priorize dores frequentes, concretas e com consequência financeira clara.",
    emptyLabel: "Sem dores ou riscos específicos observados.",
    payloadKeys: [
      "observedPainsAndRisks",
      "painsAndRisks",
      "riskSignals",
      "painSignals",
      "doresERiscosObservados",
      "painsSummary",
      "operationalPainsSummary",
      "emotionalPainsSummary",
      "fearsSummary",
    ],
    fallbackKeywords: [
      "dor",
      "dificuldade",
      "risco",
      "medo",
      "problema",
      "falta",
      "cancelamento",
      "atraso",
      "prejuízo",
      "perda",
      "cansaço",
      "sobrecarga",
      "imprevisto",
    ],
  },
  {
    id: "product-opportunities",
    title: "Oportunidades de produto",
    businessQuestion:
      "Que ativo digital pode tirar esforço ou aumentar venda rapidamente?",
    opportunityHint:
      "Transforme mecanismo plausível em checklist, roteiro, calendário, template ou mini-treinamento.",
    emptyLabel: "Sem oportunidades de produto específicas.",
    payloadKeys: [
      "productOpportunities",
      "digitalProductOpportunities",
      "mechanismOpportunitySignals",
      "opportunities",
      "oportunidadesDeProduto",
      "mechanismOpportunitiesSummary",
      "resultsSummary",
      "dreamsSummary",
    ],
    fallbackKeywords: [
      "oportunidade",
      "mecanismo",
      "resultado",
      "template",
      "checklist",
      "roteiro",
      "calendário",
      "guia",
      "plano",
      "processo",
      "automatizar",
      "aumentar",
      "reduzir",
    ],
  },
];

const genericTaskLabels = new Set([
  "gerenciar rotina e agenda",
  "gerenciar agenda e rotina",
  "organizar rotina e agenda",
]);

function normalizeText(value: string) {
  return value
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .toLowerCase()
    .replace(/[^a-z0-9çãõáéíóúâêôàüñ]+/gi, " ")
    .replace(/\s+/g, " ")
    .trim();
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}

function splitText(value: string) {
  return value
    .split(/\n|;| • | \u2022 | \|/)
    .map((item) => item.replace(/^[-–—*\d.)\s]+/, "").trim())
    .filter(Boolean);
}

function pushUnique(items: string[], seen: Set<string>, rawValue: string) {
  const value = rawValue.trim();
  if (!value) {
    return;
  }
  const normalized = normalizeText(value);
  if (seen.has(normalized)) {
    return;
  }
  seen.add(normalized);
  items.push(value);
}

function textFromSignal(signal: Record<string, unknown>) {
  const keys = [
    "taskLabel",
    "taskSummary",
    "painLabel",
    "painSummary",
    "riskLabel",
    "riskSummary",
    "outcomeLabel",
    "outcomeSummary",
    "mechanismLabel",
    "mechanismSummary",
    "opportunityLabel",
    "opportunitySummary",
    "constraintSummary",
    "constraintType",
    "workaroundSummary",
    "workaroundLabel",
    "signalText",
    "summary",
    "label",
    "title",
    "description",
    "text",
  ];
  const found = keys.find((key) => typeof signal[key] === "string");
  return found ? String(signal[found]) : null;
}

function readTextItems(value: unknown): string[] {
  if (typeof value === "string") {
    return splitText(value);
  }
  if (Array.isArray(value)) {
    return value.flatMap((item) => {
      if (typeof item === "string") {
        return splitText(item);
      }
      if (isRecord(item)) {
        const text = textFromSignal(item);
        return text ? splitText(text) : [];
      }
      return [];
    });
  }
  if (isRecord(value)) {
    const nestedItems = readTextItems(
      value.items ?? value.tasks ?? value.signals,
    );
    const summary =
      typeof value.summary === "string" ? splitText(value.summary) : [];
    const label = textFromSignal(value);
    return [...nestedItems, ...summary, ...(label ? splitText(label) : [])];
  }
  return [];
}

function findExplicitBlockItems(
  payload: Record<string, unknown>,
  definition: RoutineValueBlockDefinition,
) {
  const items: string[] = [];
  const seen = new Set<string>();

  definition.payloadKeys.forEach((key) => {
    readTextItems(payload[key]).forEach((item) =>
      pushUnique(items, seen, item),
    );
  });

  if (Array.isArray(payload.routineValueBlocks)) {
    payload.routineValueBlocks.forEach((block) => {
      if (!isRecord(block)) {
        return;
      }
      const blockTitle = normalizeText(
        String(block.title ?? block.label ?? block.blockType ?? block.id ?? ""),
      );
      const definitionTitle = normalizeText(definition.title);
      const matches =
        blockTitle === normalizeText(definition.id) ||
        blockTitle.includes(definitionTitle) ||
        definitionTitle
          .split(" ")
          .some((part) => part.length > 4 && blockTitle.includes(part));
      if (!matches) {
        return;
      }
      readTextItems(
        block.items ?? block.tasks ?? block.signals ?? block.summary,
      ).forEach((item) => pushUnique(items, seen, item));
    });
  }

  return items;
}

function collectCandidateTexts(
  payload: Record<string, unknown>,
  extraSignals: Record<string, unknown>[],
) {
  const topTasks = readTextItems(payload.topTasks);
  const topConstraints = readTextItems(payload.topConstraints);
  const workarounds = readTextItems(payload.workaroundPatterns);
  const summaries = readTextItems(payload.routineSummary);
  return [
    ...topTasks,
    ...topConstraints,
    ...workarounds,
    ...summaries,
    ...extraSignals.flatMap(readTextItems),
  ];
}

function hasKeyword(value: string, keywords: string[]) {
  const normalized = normalizeText(value);
  return keywords.some((keyword) =>
    normalized.includes(normalizeText(keyword)),
  );
}

function fallbackBlockItems(
  candidates: string[],
  definition: RoutineValueBlockDefinition,
) {
  const items: string[] = [];
  const seen = new Set<string>();
  candidates
    .filter((candidate) => hasKeyword(candidate, definition.fallbackKeywords))
    .forEach((candidate) => pushUnique(items, seen, candidate));
  return items;
}

function removeGenericRepetition(blocks: RoutineValueBlock[]) {
  let genericCount = 0;
  return blocks.map((block) => ({
    ...block,
    items: block.items.filter((item) => {
      const normalized = normalizeText(item);
      if (!genericTaskLabels.has(normalized)) {
        return true;
      }
      genericCount += 1;
      return genericCount === 1;
    }),
  }));
}

export function buildRoutineValueBlocks(
  payload: Record<string, unknown> | OprmRoutineCard | null | undefined,
  extraSignals: Record<string, unknown>[] = [],
): RoutineValueBlock[] {
  if (!payload) {
    return valueBlockDefinitions.map((definition) => ({
      ...definition,
      items: [],
    }));
  }

  const normalizedPayload = payload as Record<string, unknown>;
  const candidates = collectCandidateTexts(normalizedPayload, extraSignals);
  const blocks = valueBlockDefinitions.map((definition) => {
    const explicitItems = findExplicitBlockItems(normalizedPayload, definition);
    const fallbackItems = fallbackBlockItems(candidates, definition);
    const items: string[] = [];
    const seen = new Set<string>();
    [...explicitItems, ...fallbackItems].forEach((item) =>
      pushUnique(items, seen, item),
    );
    return { ...definition, items: items.slice(0, 5) };
  });

  return removeGenericRepetition(blocks);
}
