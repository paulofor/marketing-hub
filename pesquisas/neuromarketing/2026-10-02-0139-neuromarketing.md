# Radar de Neuromarketing e Desejos Digitais — 02/10/2026 01:39

## Resumo executivo

Esta rodada encontrou três sinais novos e úteis para o Marketing Hub. O mais acionável é um alerta sobre memória em agentes: usuários relatam tratar chatbots como espaços de confidencialidade e podem mudar o comportamento quando percebem a amplitude da memória. O segundo sinal reforça que a descoberta de produtos está migrando para IA e outros canais fora das propriedades da marca, com evidência comportamental em grande escala. O terceiro reforça que fricção operacional, especialmente troca de canal e excesso de etapas, pode bloquear uma intenção já existente.

Apenas o primeiro achado gerou nova versão de card. Os outros dois reforçam princípios já cobertos ou vêm de contexto específico demais para justificar uma nova regra do harness nesta rodada.

## Achado 1 — Chatbots viram confidentes antes de os usuários entenderem memória e permissões

### O que aconteceu

A Aura publicou em 1 de outubro de 2026 o AI TMI Study, survey online conduzido pela Talker Research com 2.000 adultos dos EUA, de 18 a 64 anos, que usam agentes de IA ou chatbots.

Entre os respondentes, 73% disseram ter compartilhado com IA algo que não publicariam publicamente; 71% disseram ter contado algo que ficariam constrangidos ou desconfortáveis se o parceiro ou amigo mais próximo soubesse; 53% assumem que suas conversas com IA são completamente ou majoritariamente privadas; 64% disseram ter concedido acesso a conta ou informação pessoal sem compreender completamente as permissões; 74% já se surpreenderam com o quanto um chatbot parecia entender a partir do histórico; e 61% disseram que compartilhariam menos informação se a IA lembrasse de tudo.

### Desejo ou comportamento revelado

**Evidência encontrada:** existe uma distância relevante entre o comportamento de divulgação e a compreensão de privacidade, permissões e memória.

**Hipótese interpretativa:** a interface conversacional pode gerar sensação de confidencialidade e proximidade, reduzindo a cautela do usuário. Quando a memória fica explícita ou parece ampla demais, esse benefício pode se converter em sensação de vigilância.

### Por que importa

No customer-agent e no Click-to-WhatsApp, o objetivo não deve ser maximizar a quantidade de informação obtida. O agente precisa resolver a tarefa com o mínimo de dados necessário e preservar sensação de controle.

### Aplicação possível no Marketing Hub

Adicionar um `ContextMemoryGate` com quatro regras:

1. memória mínima e pertinente à tarefa;
2. aviso just-in-time quando uma informação passar a ser reutilizada em conversas futuras;
3. confirmação antes de reutilizar informação de outro contexto;
4. evitar perguntas abertas ou tom de intimidade que estimulem revelações desnecessárias.

### Experimento proposto

Comparar:

- **Controle:** agente atual, com memória silenciosa;
- **Tratamento:** memória mínima + aviso just-in-time + confirmação para reutilização cross-contextual.

Medir repetição de perguntas, abandono, handoff, confiança percebida, quantidade de dados desnecessários compartilhados, CTA, checkout e pagamento reconciliado.

### Impacto potencial

**Médio-alto**, principalmente em agentes de atendimento, qualificação e vendas. A hipótese é reduzir intrusão e risco de privacidade sem perder conveniência.

### Limites

O estudo é autorrelatado, restrito aos EUA e encomendado por uma empresa de segurança digital. As porcentagens não provam que um aviso de memória aumente confiança ou conversão. A evidência experimental anterior sobre relevância contextual de memória não mostrou efeito transacional consistente.

### Fontes

- https://www.aura.com/press/release/new-ai-tmi-study
- https://www.frontiersin.org/journals/psychology/articles/10.3389/fpsyg.2026.1934857/full

---

## Achado 2 — A descoberta de produtos está migrando para fora do site e da busca tradicional

### O que aconteceu

A Salesforce publicou em 30 de setembro de 2026 a quarta edição do State of Commerce. O relatório combina survey com 3.450 profissionais de comércio, survey com 4.689 consumidores e dados comportamentais de mais de 1,5 bilhão de shoppers em 37 países.

Nos dados comportamentais, o tráfego referido por chats de IA cresceu entre 150% e 428% ano contra ano em todos os trimestres medidos. Entre agosto de 2025 e maio de 2026, a descoberta via propriedades próprias da marca caiu 7%, a busca tradicional caiu 15% e novos canais — incluindo assistentes de IA, IA em redes sociais e apps de entrega — cresceram 38%.

### Desejo ou comportamento revelado

**Evidência encontrada:** parte crescente da descoberta começa fora do site da marca.

**Hipótese interpretativa:** a primeira competição pode ocorrer antes da landing page, quando um agente ou outra interface decide quais ofertas entram na shortlist.

### Por que importa

O Marketing Hub não pode avaliar uma oferta apenas pelo desempenho da landing. Precisa verificar se produto, preço, entregáveis, limites e diferenciais são recuperáveis e representados corretamente por agentes.

### Aplicação possível no Marketing Hub

Ampliar o `AgentReadableOfferAudit` para verificar:

- presença em shortlist;
- atributos recuperados corretamente;
- diferenciais citados;
- preço e condições;
- fontes usadas;
- erros factuais;
- referral de IA até a landing.

### Experimento proposto

Para a mesma oferta, comparar a página atual com uma versão estruturada para recuperação por agentes, executando um conjunto fixo de prompts em vários modelos e acompanhando shortlist, erros, referral, CTA, lead, checkout e pagamento.

### Impacto potencial

**Alto** para descoberta e consideração, principalmente em produtos de nicho. Nesta rodada não foi criado novo card porque o princípio já está coberto por `ia-gatekeeper-de-compra`; a nova evidência aumenta confiança na tendência, mas não muda suficientemente a regra operacional.

### Limites

Parte dos dados vem de surveys e parte de comportamento agregado da plataforma Salesforce. Crescimento de referral não prova que IA melhore conversão ou qualidade do lead para o Marketing Hub.

### Fonte

- https://www.salesforce.com/ap/news/press-releases/2026/09/30/shoppings-new-first-step-agentic-search-grows-200-as-purchase-journeys-start-in-ai-chats/

---

## Achado 3 — Intenção pode existir e ainda morrer por fricção de etapas e troca de canal

### O que aconteceu

Pesquisa da Datos Insights patrocinada pela SBT, divulgada em 30 de setembro de 2026, ouviu 2.000 consumidores dos EUA responsáveis por ao menos uma obrigação financeira. Sessenta e sete por cento disseram esperar ir de um lembrete de pagamento até o pagamento concluído em três etapas ou menos. Trinta e seis por cento já adiaram pagamento porque o processo era complicado, 25,5% já adiaram por precisar trocar de aplicativo e 47% disseram que seriam mais propensos a pagar imediatamente se o pagamento estivesse incorporado no próprio lembrete.

### Desejo ou comportamento revelado

**Evidência encontrada:** mesmo quando a intenção de pagar já existe, handoffs e passos extras podem interromper a ação.

**Hipótese interpretativa:** em funis comerciais, parte do abandono pode ser operacional, não falta de desejo.

### Por que importa

Isso é relevante para Click-to-WhatsApp, páginas de venda e checkout, especialmente quando o usuário precisa sair do canal, procurar login, copiar dados ou reconstruir contexto.

### Aplicação possível no Marketing Hub

Registrar `step_count`, `channel_handoff_count` e `login_required` como atributos do funil e relacioná-los com abandono e pagamento reconciliado.

### Experimento proposto

Comparar um fluxo com handoff externo e múltiplas etapas contra um fluxo que mantenha confirmação, preço e link de pagamento no menor número de transições possível.

### Impacto potencial

**Médio**, mas a transferência para e-commerce é indireta.

### Limites

O estudo trata de pagamento de obrigações financeiras, não de compra discricionária. É patrocinado por uma empresa de pagamentos, por isso não deve virar regra geral de checkout sem validação própria.

### Fonte

- https://www.aol.com/articles/sbt-research-datos-insights-finds-110000000.html

## Cards desta rodada

### Atualizado: `continuidade-contextual-agente`

Foi criada nova versão porque o achado de hoje acrescenta uma fronteira material à regra de continuidade: não basta a memória ser pertinente; sua existência e amplitude também precisam ser compreensíveis para o usuário.

Fonte revisada:
`pesquisas/neuromarketing/cards/fontes/2026-10-02-continuidade-contextual-agente-memoria-transparente.md`

SHA-256:
`a83dda41cc6e990d1582a7b775ecf15bfc16e2e1fd479df3cb598763951bafa8`

JSON:
`pesquisas/neuromarketing/cards/2026-10-02-continuidade-contextual-agente.json`

Nenhum card adicional foi criado para Salesforce ou Datos Insights para evitar redundância e extrapolação excessiva.
