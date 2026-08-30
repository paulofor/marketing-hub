export type AssistedServiceTastingScenario = {
  id: string;
  label: string;
  context: string;
};

export type AssistedServiceTastingTone = {
  id: string;
  label: string;
};

export type AssistedServiceTastingVariant = {
  scenarioId: string;
  toneId: string;
  response: string;
  qualificationQuestion: string;
  followUps: string[];
};

export type AssistedServiceTastingContract = {
  version: string;
  title: string;
  introduction: string;
  serviceLabel: string;
  servicePlaceholder: string;
  privacyHint: string;
  scenarios: AssistedServiceTastingScenario[];
  tones: AssistedServiceTastingTone[];
  variants: AssistedServiceTastingVariant[];
  paidBoundary: string;
  submitLabel: string;
};

const kitWhatsAppTastingV1: AssistedServiceTastingContract = {
  version: "kit-whatsapp-tasting-v1",
  title: "Experimente uma sequência antes de comprar",
  introduction:
    "Informe apenas o serviço, escolha uma situação e o tom. Você receberá uma resposta, uma pergunta de qualificação e três follow-ups para entender o mecanismo da implantação.",
  serviceLabel: "Qual serviço você oferece?",
  servicePlaceholder: "Ex.: manicure ou fotografia",
  privacyHint:
    "Não informe nome, telefone nem mensagem real de cliente. A demonstração funciona com uma descrição genérica do serviço.",
  scenarios: [
    {
      id: "orcamento-sem-resposta",
      label: "Orçamento sem resposta",
      context: "Retomar uma conversa sem pressionar nem presumir interesse.",
    },
    {
      id: "pedido-de-preco",
      label: "Perguntou apenas o preço",
      context:
        "Responder com clareza e descobrir o contexto mínimo antes do próximo passo.",
    },
  ],
  tones: [
    { id: "acolhedor", label: "Acolhedor" },
    { id: "direto", label: "Direto" },
    { id: "profissional", label: "Profissional" },
  ],
  variants: [
    {
      scenarioId: "orcamento-sem-resposta",
      toneId: "acolhedor",
      response:
        "Oi! Passando com calma para saber se ficou alguma dúvida sobre o orçamento de {servico}. Se quiser, eu explico o que está incluído antes de você decidir.",
      qualificationQuestion:
        "O que você precisa confirmar primeiro sobre {servico}: prazo, disponibilidade ou forma de pagamento?",
      followUps: [
        "Se ainda estiver avaliando, posso resumir as opções de {servico} em uma mensagem curta.",
        "Quer que eu verifique uma data específica antes de você decidir?",
        "Vou encerrar por aqui para não incomodar. Se quiser retomar a conversa sobre {servico}, é só me chamar.",
      ],
    },
    {
      scenarioId: "orcamento-sem-resposta",
      toneId: "direto",
      response:
        "Olá! Você conseguiu avaliar o orçamento de {servico}? Posso esclarecer o ponto que falta para definirmos o próximo passo.",
      qualificationQuestion:
        "Para avançar com o {servico}, você precisa confirmar preço, prazo ou disponibilidade?",
      followUps: [
        "Posso reservar uma opção de data enquanto você confirma o {servico}.",
        "Ainda faz sentido manter este orçamento de {servico} em aberto?",
        "Encerrando este atendimento por agora. Quando quiser retomar, responda a esta mensagem.",
      ],
    },
    {
      scenarioId: "orcamento-sem-resposta",
      toneId: "profissional",
      response:
        "Olá! Estou retomando o orçamento de {servico} para confirmar se as condições ficaram claras e se existe alguma informação pendente para sua decisão.",
      qualificationQuestion:
        "Qual critério é prioritário para sua decisão sobre o {servico}: escopo, prazo, agenda ou pagamento?",
      followUps: [
        "Posso enviar um resumo objetivo do escopo e das condições do {servico}.",
        "Caso tenha uma data em mente, verifico a disponibilidade antes de prosseguirmos.",
        "Este orçamento será encerrado por enquanto, mas posso reabri-lo quando você desejar.",
      ],
    },
    {
      scenarioId: "pedido-de-preco",
      toneId: "acolhedor",
      response:
        "Claro! O valor para {servico} depende do que você precisa. Se me contar só um pouco do seu caso, eu explico a opção mais adequada sem compromisso.",
      qualificationQuestion:
        "Qual resultado você espera com {servico} e para quando precisa?",
      followUps: [
        "Com essas duas informações, consigo indicar a opção de {servico} sem fazer você perder tempo.",
        "Quer que eu apresente a alternativa mais simples primeiro?",
        "Se preferir decidir depois, tudo bem. Quando quiser, retomamos por aqui.",
      ],
    },
    {
      scenarioId: "pedido-de-preco",
      toneId: "direto",
      response:
        "O preço do {servico} varia conforme escopo e prazo. Responda duas perguntas rápidas e eu informo a opção correta.",
      qualificationQuestion:
        "Você precisa de qual resultado e em qual prazo para o {servico}?",
      followUps: [
        "Com resultado e prazo definidos, envio o valor aplicável ao seu caso.",
        "Posso começar pela opção essencial de {servico}, se preferir.",
        "Encerrando por agora; responda quando quiser receber a orientação de preço.",
      ],
    },
    {
      scenarioId: "pedido-de-preco",
      toneId: "profissional",
      response:
        "Para informar o valor correto do {servico}, preciso confirmar brevemente o escopo e a data desejada. Assim você recebe uma condição compatível com a necessidade real.",
      qualificationQuestion:
        "Qual é o escopo esperado e qual prazo deve ser considerado para o {servico}?",
      followUps: [
        "Após essa confirmação, apresento valor, itens incluídos e próximos passos do {servico}.",
        "Se desejar, também posso separar a opção essencial da opção completa.",
        "A conversa ficará disponível para retomada quando for conveniente.",
      ],
    },
  ],
  paidBoundary:
    "Esta amostra demonstra o método com uma situação. A implantação paga inclui briefing, adaptação às regras do seu negócio, 10 a 20 respostas, perguntas, follow-ups, escalonamento, revisão humana e entrega completa.",
  submitLabel: "Gerar minha amostra",
};

const tastingContracts: Record<string, AssistedServiceTastingContract> = {
  "kit-whatsapp-pronto": kitWhatsAppTastingV1,
};

/** Resolve a degustação versionada sem acoplar o componente público a um produto específico. */
export function resolveAssistedServiceTastingContract(productSlug: string) {
  return tastingContracts[productSlug] ?? null;
}
