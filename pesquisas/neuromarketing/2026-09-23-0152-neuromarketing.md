# Radar de Neuromarketing e Desejos Digitais — 23/09/2026

**Data/hora:** 2026-09-23 01:52 (America/Sao_Paulo)

## Resumo executivo

Nesta rodada, dois achados novos justificam ação no Marketing Hub:

1. Um novo survey da Gartner mostra que a saturação de conteúdo gerado por IA está deslocando confiança para pessoas reais e reduzindo confiança em mensagens de marca quando a IA é percebida como excesso de volume.
2. O Avast 2026 Safe Tech Report mostra uma lacuna relevante entre autoconfiança no uso de IA e cautela com dados sensíveis, com sinal particularmente forte no recorte brasileiro.

Um terceiro sinal regulatório/operacional reforça guardrails já existentes para agentes de compra: um grupo de grandes bancos alertou que agentes com maior autonomia elevam riscos de fraude, privacidade, escolha e recurso em caso de erro.

---

## 1. Saturação de conteúdo com IA está virando problema de confiança

### Evidência encontrada

Em 22 de setembro de 2026, a Gartner publicou resultados de uma pesquisa com 1.006 consumidores dos Estados Unidos, realizada entre maio e junho de 2026.

- 65% disseram acreditar que marcas estão produzindo conteúdo demais gerado por IA.
- 57% disseram que a prevalência desse conteúdo os tornou menos confiantes nas mensagens das marcas.
- 35% disseram recorrer menos a influenciadores para informação e recomendação de compras por causa da expansão de conteúdo gerado por IA.
- 43% disseram recorrer mais a pessoas reais para informação e recomendação de compras.

A própria Gartner recomenda que marcas usem IA para melhorar relevância, criatividade e experiência, em vez de simplesmente aumentar volume.

### Desejo/comportamento revelado

O sinal não é uma rejeição simples à IA. O consumidor parece estar ficando mais seletivo diante de ambientes saturados por conteúdo sintético e atribuindo valor maior a expertise, procedência e perspectiva humana verificável.

### Hipótese interpretativa

Quanto mais a IA for usada apenas para multiplicar peças semelhantes, maior pode ser o risco de "fadiga sintética": o usuário percebe volume, repetição ou genericidade e passa a descontar a credibilidade da mensagem.

Isso é hipótese. O survey não demonstra causalidade em CTR, CPL ou vendas.

### Aplicação no Marketing Hub

Em `creative_variants`, adicionar uma revisão de qualidade que pergunte:
- a peça adiciona uma informação, prova ou perspectiva realmente distinta?
- existe produto, demonstração ou evidência verificável?
- quando há creator/depoimento, sua procedência é verificável?
- as variantes são diferentes em ângulo e função, e não apenas na aparência?

A IA deve continuar sendo usada para acelerar produção, mas o objetivo passa a ser aumentar **qualidade e diversidade real**, não simplesmente quantidade.

### Experimento concreto

Comparar:
- **A:** família grande de criativos gerados em escala, com variações principalmente cosméticas;
- **B:** família menor, com ângulos realmente distintos, prova real e elementos humanos verificáveis.

Manter oferta, público e investimento equivalentes. Medir confiança percebida separadamente de CTR, CTA, lead, checkout e pagamento reconciliado.

### Impacto potencial

Alto para Meta Ads e creative variants, porque afeta diretamente estratégia de escala criativa. O risco é que geração barata de peças aumente volume mas destrua diferenciação e confiança.

### Limites

- Survey autorrelatado.
- Amostra dos EUA.
- Não mede venda observada.
- A percepção de conteúdo sintético e as regras de disclosure mudam rapidamente.

### Fonte

https://www.gartner.com/en/newsroom/press-releases/2026-09-22-gartner-marketing-survuey-finds-35-percent-of-consumers-rely-on-influencers-less-due-to-ai

---

## 2. Brasil: confiança em IA não significa cautela com dados sensíveis

### Evidência encontrada

O `Avast 2026 Safe Tech Report`, publicado em 22 de setembro, foi conduzido online em 12 mercados — incluindo Brasil — entre 22 de maio e 19 de junho de 2026, com 13.003 adultos. Os dados foram ponderados por idade, gênero e região.

Globalmente:
- 56% disseram que reconheceriam imediatamente uma falsificação feita por IA;
- 52% já compartilharam ou consideraram compartilhar pelo menos uma informação sensível com uma ferramenta de IA;
- 20% já compartilharam ou consideraram compartilhar currículo;
- 20% fizeram o mesmo com documento de trabalho.

No recorte brasileiro reportado pela TI Inside:
- 73% disseram estar certos de que identificariam uma falsificação feita por IA;
- 70% já compartilharam ou consideraram compartilhar ao menos uma informação sensível com IA;
- 30% disseram isso sobre documento de trabalho;
- 30% sobre screenshots de mensagens privadas ou e-mails;
- 28% sobre currículo.

### Desejo/comportamento revelado

A interface conversacional pode gerar uma sensação de informalidade e proximidade que reduz a percepção do risco de compartilhar dados. O usuário pode se sentir competente em IA e, ao mesmo tempo, não perceber claramente retenção, reutilização ou exposição posterior.

### Hipótese interpretativa

Autoconfiança tecnológica não funciona como proteção de privacidade. Por isso, deixar toda a responsabilidade de autocontrole com o usuário pode falhar justamente quando o agente parece mais útil e confiável.

### Aplicação no Marketing Hub

Criar um `DataMinimizationGate` para Click-to-WhatsApp, formulários conversacionais e futuros agentes:
- pedir somente o dado necessário para a tarefa atual;
- explicar de forma curta por que o dado é necessário;
- preferir campos estruturados a texto livre quando possível;
- evitar pedir documento, print, credencial ou informação pessoal ampla quando não for essencial;
- inserir confirmação/aviso antes de campos mais sensíveis;
- registrar como métrica de risco o envio espontâneo de dados não necessários.

### Experimento concreto

Comparar:
- **A:** fluxo aberto de texto livre;
- **B:** fluxo estruturado e mínimo, com finalidade explícita para cada dado.

Não medir "quem consegue coletar mais". Medir conclusão, abandono, dados desnecessários enviados espontaneamente, confiança percebida e necessidade de suporte. O objetivo é reduzir exposição sem piorar a experiência.

### Impacto potencial

Alto para agentes e Click-to-WhatsApp, porque evita que uma boa UX conversacional gere um efeito colateral de oversharing. Também reduz risco regulatório e de confiança.

### Limites

- Survey autorrelatado.
- "Compartilhou ou considerou compartilhar" mistura comportamento e intenção.
- O recorte brasileiro detalhado foi publicado por fonte secundária com base no estudo.
- Não mede incidentes reais nem conversão comercial.

### Fontes

https://newsroom.gendigital.com/2026-09-22-Avast-Safe-Tech-Report-Reveals-Overconfidence-in-Spotting-Deepfakes-and-Comfort-in-Sharing-Personal-Data-with-Bots

https://tiinside.com.br/en/22/09/2026/70%25-of-Brazilians-have-already-considered-sharing-sensitive-data-with-AI-chatbots./

---

## 3. Agentes de compra: transparência, escolha e recurso estão virando requisitos de confiança

### Evidência encontrada

Em 22 de setembro, Reuters reportou um documento conjunto de NatWest, Bank of America, ING, ASB Bank, Capital One e Commonwealth Bank of Australia sobre agentic commerce. O grupo afirmou que a tecnologia está avançando mais rápido que padrões e proteções ao consumidor.

Os riscos destacados incluem:
- agente pedir dados de cartão e inseri-los diretamente em sites;
- conduzir usuários a meios de pagamento com proteções mais fracas;
- falta de clareza sobre se o agente age realmente no interesse do consumidor;
- dúvida sobre quem responde quando algo dá errado.

Os bancos propõem disclosure quando um agente participa da transação, transparência de decisão, salvaguardas de dados, escolha de serviço e interoperabilidade.

### Desejo/comportamento revelado

Este documento não é pesquisa de preferência do consumidor, portanto não deve ser tratado como prova de desejo. Ele reforça, porém, um problema já observado em pesquisas anteriores: à medida que aumenta a autonomia, confiança depende de limites, atribuição e possibilidade de recurso.

### Aplicação no Marketing Hub

Não criar novo card. O achado reforça `delegacao-agente-com-controle-humano`.

Para agentes com ação comercial futura, manter:
- identidade do agente;
- autorização explícita;
- escopo e limite;
- confirmação antes de pagamento/compromisso;
- registro da ação;
- caminho claro de reversão ou atendimento humano.

### Limites

É um posicionamento de instituições financeiras e não um experimento comportamental nem survey de consumidores.

### Fonte

https://www.reuters.com/legal/litigation/banks-warn-ai-shopping-bots-raise-scam-fraud-data-privacy-risks-2026-09-22/

---

## Cards criados/atualizados

### `autenticidade-criativo-ia` — nova versão

Atualizado porque o survey da Gartner adiciona uma dimensão nova ao card existente: além de aparência sintética, **excesso de conteúdo gerado por IA** pode ser percebido como problema de confiança. A evidência também sugere deslocamento relativo para pessoas reais e expertise verificável.

Fonte revisada:
`pesquisas/neuromarketing/cards/fontes/2026-09-23-autenticidade-criativo-ia-gartner.md`

SHA-256:
`c8ad05a272462aed7358701a801513b73d83694af43c32b3411ea549821ac8de`

Card:
`pesquisas/neuromarketing/cards/2026-09-23-autenticidade-criativo-ia.json`

### `minimizacao-dados-agente-conversacional` — novo card

Criado porque o sinal é forte, diretamente aplicável a agentes/WhatsApp e possui recorte brasileiro. Ele transforma um risco comportamental em uma regra reutilizável de produto: **não depender da prudência do usuário para limitar coleta de dados**.

Fonte revisada:
`pesquisas/neuromarketing/cards/fontes/2026-09-23-minimizacao-dados-agente.md`

SHA-256:
`3bbf3d4d4b615c4ed00a1434e5e6223a951e3b2cf7d5a383219b1f1313a911d6`

Card:
`pesquisas/neuromarketing/cards/2026-09-23-minimizacao-dados-agente-conversacional.json`

A coleção `neuromarketing` continua válida no guia atual da API. Nenhum POST manual foi realizado; os JSONs ficaram no repositório para o fluxo normal de `DRAFT`.
