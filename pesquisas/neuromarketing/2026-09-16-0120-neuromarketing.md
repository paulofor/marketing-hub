# Radar de Neuromarketing e Desejos Digitais — 2026-09-16 01:20

Data/hora: 2026-09-16 01:20 (America/Sao_Paulo)

## Resumo executivo

A rodada trouxe dois sinais novos e úteis para o Marketing Hub. O principal é um levantamento publicado em 15/09 com 2.501 consumidores em oito mercados, incluindo 350 brasileiros: a confiança em IA varia fortemente conforme a tarefa. O segundo é um estudo peer-reviewed publicado no mesmo dia mostrando que continuidade de uso de IA depende menos da simples presença da tecnologia e mais da efetividade da colaboração, da utilidade percebida, da confirmação das expectativas e da satisfação.

A aplicação prática mais forte é tratar autonomia de agentes como uma política por risco e tipo de tarefa, não como uma escolha binária entre "IA faz tudo" e "humano aprova tudo".

---

## Achado 1 — Consumidores aceitam IA mais facilmente em tarefas específicas e de baixo risco

### Evidência

A Sinch publicou em 15/09/2026 uma pesquisa online com 2.501 consumidores nos Estados Unidos, Austrália, Brasil, Reino Unido, França, Alemanha, México e Espanha.

Resultados relevantes:

- 81% disseram estar confiantes em um assistente de IA para rastreamento de pedidos e atualizações de envio.
- 74% disseram estar confiantes em IA para perguntas antes da compra.
- A confiança caiu para 66% em alterações de conta e 62% em alterações de pagamento ou cobrança.
- 53% confiam mais em recomendação de produto feita por uma pessoa; 47% confiam na IA tanto quanto ou mais.
- 43% citaram privacidade ou uso de dados como principal preocupação.
- Entre os 350 respondentes brasileiros, 78% disseram acreditar que IA tornará as compras de fim de ano mais fáceis em 2026.

### Desejo ou comportamento revelado

**Evidência encontrada:** consumidores não parecem avaliar "IA" de forma uniforme; a confiança muda conforme a tarefa.

**Hipótese interpretativa:** pessoas tendem a aceitar mais autonomia quando a tarefa é clara, útil, reversível e de menor risco. A necessidade de controle aumenta quando a ação toca dinheiro, conta, identidade ou consequências difíceis de reverter.

### Por que importa para o Marketing Hub

Isso é diretamente aplicável a agentes comerciais e ao Click-to-WhatsApp. Um agente não precisa pedir confirmação para cada resposta ou comparação, mas também não deve transformar autonomia em autorização irrestrita.

### Aplicação possível

Implementar uma `DelegationPolicy` baseada em classes de ação:

- **autônoma:** responder dúvida, comparar alternativas, resumir informação, preparar resposta, verificar status;
- **confirmável:** alterar preferência, enviar dado para terceiro, iniciar contato, mudar cadastro;
- **sensível:** pagamento, assinatura, compromisso financeiro, publicação, ação irreversível ou uso ampliado de dados pessoais.

Para ações sensíveis, mostrar o que será feito e exigir confirmação explícita; manter trilha de auditoria e opção clara de intervenção humana.

### Experimento/feature

**Experimento A/B/C em agente de WhatsApp:**

- A: confirmação em toda microetapa;
- B: autonomia em tarefas reversíveis + confirmação apenas em ações sensíveis;
- C: autonomia ampla sem confirmação intermediária.

Medir: conclusão de conversa, tempo até resolução, abandono, necessidade de repetição, solicitação de humano, lead qualificado e eventos comerciais reconciliados.

### Impacto potencial

Alto para UX de agentes e redução de fricção. O achado também é especialmente relevante porque inclui um recorte brasileiro, embora pequeno.

### Limites

A pesquisa é autorrelatada, publicada por uma empresa do setor e não mede comportamento comercial real. O recorte brasileiro tem 350 pessoas e não representa automaticamente toda a população brasileira. Os percentuais de confiança não provam causalidade sobre conversão.

Fonte:
https://www.group.sinch.com/media/press-releases-and-news/2026/shoppers-are-cautious-about-ai-but-confident-when-it-has-a-job-to-do/

---

## Achado 2 — A utilidade percebida da IA depende da experiência concreta de colaboração

### Evidência

A *Scientific Reports* publicou em 15/09/2026 um estudo com 271 universitários coreanos. Os autores analisaram os dados com PLS-SEM; o modelo explicou 63,6% da variância na intenção de continuar usando IA generativa.

A efetividade da colaboração humano-IA aumentou diretamente a utilidade percebida e também atuou por meio de confirmação das expectativas. Utilidade, confirmação e satisfação fortaleceram a intenção de continuar usando a IA.

Um resultado interessante foi que o ajuste tarefa-tecnologia não elevou diretamente a utilidade percebida; o efeito apareceu quando a experiência real confirmou a expectativa do usuário.

### Desejo ou comportamento revelado

**Evidência encontrada:** o uso continuado esteve associado à percepção de que a IA realmente colaborou bem e entregou utilidade após o uso.

**Hipótese interpretativa:** "ter IA" não é valor suficiente. O usuário tende a valorar a tecnologia quando percebe uma tarefa resolvida e quando a experiência corresponde ao que foi prometido.

### Por que importa para o Marketing Hub

Nos agentes, a proposta de valor não deveria ser "fale com nossa IA", mas "resolva X com menos esforço". Isso também sugere que a avaliação do agente deve observar resolução da tarefa e satisfação, e não apenas latência, quantidade de mensagens ou uso de recursos de IA.

### Aplicação possível

Para agentes de atendimento e venda:

- declarar claramente a tarefa que o agente consegue resolver;
- evitar prometer autonomia maior que a executável;
- mostrar progresso ou resultado concreto;
- permitir correção fácil quando a IA entendeu algo errado;
- medir satisfação pós-tarefa e repetição de pergunta.

### Experimento/feature

Comparar duas entradas de conversa:

- A: "Converse com nosso assistente de IA";
- B: "Compare opções, tire dúvidas e receba uma recomendação explicada em poucos minutos".

A interface e o modelo permanecem iguais. Medir início da conversa, conclusão da tarefa, satisfação, reabertura e conversão posterior.

### Impacto potencial

Moderado. O princípio pode melhorar posicionamento e avaliação dos agentes, mas deve ser validado em contexto comercial.

### Limites

O estudo ocorreu em ambiente educacional, com estudantes coreanos. É observacional e não prova causalidade em vendas, confiança comercial ou comportamento brasileiro.

Fonte:
https://www.nature.com/articles/s41598-026-71940-1

---

## Card do Marketing Hub

Foi gerada **uma nova versão do card existente**:

`delegacao-agente-com-controle-humano`

A mesma `cardKey` foi preservada porque o achado atualiza a mesma ideia, em vez de criar um conceito novo. A versão anterior já defendia autonomia delimitada; a evidência de hoje acrescenta dois elementos materiais:

1. um gradiente empírico de confiança por tipo de tarefa, com recorte brasileiro;
2. evidência peer-reviewed complementar de que utilidade e continuidade dependem da experiência concreta de colaboração.

Fonte revisada:
`pesquisas/neuromarketing/cards/fontes/2026-09-16-delegacao-agente-controle.md`

SHA-256:
`1c8867a916f8f092bcff9eb165ea829b11742881e381a9dc387a6f801e224def`

Card:
`pesquisas/neuromarketing/cards/2026-09-16-delegacao-agente-controle.json`

A coleção `neuromarketing` permanece aceita pelo guia `harness-library-api/docs/guia-uso-api-cards.md`.

Nenhum POST manual foi realizado para a API. Os arquivos ficaram prontos no repositório para o fluxo automático de DRAFT.

## Fontes

- Sinch, 15/09/2026: https://www.group.sinch.com/media/press-releases-and-news/2026/shoppers-are-cautious-about-ai-but-confident-when-it-has-a-job-to-do/
- Scientific Reports, 15/09/2026: https://www.nature.com/articles/s41598-026-71940-1
