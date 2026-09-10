# Radar de Neuromarketing, Comportamento e Desejos Digitais

**Data/hora:** 10/09/2026 01:56 — America/Sao_Paulo

## Resumo executivo

Nesta rodada, três achados novos se destacam para o Marketing Hub. O primeiro mostra que consumidores que já usam IA para compras querem que recomendações tragam experiência humana crível, especialmente de consumidores comuns, reviewers e especialistas. O segundo é um preprint diretamente sobre publicidade que reforça um guardrail metodológico importante: diferenças de eye-tracking entre criativos humanos e gerados por IA podem ser explicadas em parte por propriedades visuais do estímulo, como densidade de bordas, e não pela “origem IA” em si. O terceiro combina pesquisa recente sobre agentic commerce com o lançamento do Meta Muse: usuários aceitam IA para pesquisar e alterar decisões, mas continuam muito mais cautelosos quando a ação chega ao pagamento; agentes reais estão sendo desenhados com permissões, aprovação sensível e trilha de auditoria.

---

## 1. Perspectivas de criadores aumentam a confiança em recomendações de compra por IA

### O que aconteceu

Em 9 de setembro de 2026, o IAB publicou a pesquisa global “The Creator Signal: How AI Systems Interpret Influence in Commerce”. O estudo, conduzido pela Attest em maio de 2026, ouviu 2.200 consumidores que haviam usado IA para pesquisa de compras nos três meses anteriores, em Estados Unidos, Reino Unido, Austrália, México e Índia.

Entre esses usuários, 56% disseram preferir recomendações de IA que incorporem perspectivas de criadores ou influenciadores e 65% disseram ficar mais confiantes quando a recomendação é informada por reviews de criadores considerados críveis. A preferência foi maior entre Gen Z e Millennials. Os tipos mais confiáveis não foram necessariamente celebridades: consumidores comuns (52%), reviewers profissionais (43%) e especialistas (37%) ficaram à frente. Credibilidade, consistência, expertise e independência/transparência de patrocínio foram os sinais mais citados de confiança.

### Desejo/comportamento revelado

Quando a IA reduz dezenas de opções a poucas recomendações, o usuário parece querer uma camada de experiência humana real por trás da síntese. O valor não está simplesmente em “ter um influenciador”, mas em evidência de uso, competência e independência.

### Por que importa

Isso conecta diretamente prova social e agentic discovery. Um agente pode encontrar preço e especificação, mas o consumidor pode confiar mais quando a recomendação também consegue explicar o que pessoas reais, reviewers ou especialistas observaram.

### Aplicação no Marketing Hub

Criar `CreatorEvidence` / `HumanExperienceEvidence` ligado à oferta, com:

- tipo da fonte: consumidor, reviewer, especialista;
- experiência real declarada;
- claim sustentado;
- eventual relação comercial/patrocínio;
- URL verificável;
- representação legível por agentes.

Isso complementa `AIReadableSocialProof`, `AgentDiscoverabilityTest` e `AIRecommendationExperienceParity`.

### Experimento sugerido

Para uma mesma oferta, executar o mesmo conjunto de prompts em agentes antes e depois de publicar prova humana verificável em formato acessível:

1. somente página comercial;
2. página + depoimentos genéricos;
3. página + reviews/experiência humana com fonte, contexto e transparência.

Medir `MentionRate`, `CitationRate`, `ClaimAccuracy`, qualidade da justificativa e depois comportamento real no funil quando houver tráfego atribuível.

### Impacto potencial

**Alto e estratégico.** Pode transformar prova social de elemento puramente visual da landing em insumo também para recomendação por IA.

### Limites

A amostra é composta por usuários recentes de IA para compras e não inclui Brasil. É autorrelato e não prova que conteúdo de criadores aumenta conversão.

**Fonte:** https://www.iab.com/news/consumers-want-ai-shopping-recommendations-to-include-trusted-creator-perspectives/

---

## 2. Eye-tracking de anúncios com IA: diferenças de gaze podem vir do desenho do estímulo, não da “origem IA”

### O que aconteceu

Em 9 de setembro de 2026 foi publicado o preprint “Eye-Tracking Analysis of Visual Attention and Preference in Human-Created, AI-Generated, and Human–AI Co-Created Advertisements”.

Trinta e nove participantes completaram 858 trials envolvendo 22 pares de anúncios enquanto o gaze era registrado a 60 Hz. Os anúncios gerados por IA foram escolhidos mais frequentemente do que os humanos no forced-choice; anúncios co-criados humano–IA não diferiram dos humanos na frequência de escolha. Anúncios de IA e co-criados produziram maior contagem de fixações e menores magnitudes sacádicas após correção estatística.

O próprio estudo, porém, encontrou uma diferença estrutural importante: os estímulos de IA e co-criados tinham maior densidade de bordas. Os autores alertam que as diferenças de gaze não podem ser atribuídas unicamente à origem criativa.

### Desejo/comportamento revelado

O dado principal aqui é metodológico, não uma preferência do consumidor. O olho responde ao conteúdo visual concreto, e não a uma etiqueta abstrata “feito por IA”. Complexidade estrutural, saliência e outras propriedades podem alterar exploração visual.

### Por que importa

Sem controlar essas propriedades, o Marketing Hub poderia concluir algo como “criativos de IA recebem mais atenção”, quando na verdade criou imagens visualmente mais densas ou complexas.

### Aplicação no Marketing Hub

Atualizar `AttentionInterpretationGuardrail` para registrar, além do eye-tracking:

- densidade de bordas;
- luminância;
- contraste;
- saliência;
- complexidade visual;
- compreensão da mensagem;
- lembrança da marca;
- preferência;
- comportamento posterior.

Comparações humanas versus IA devem parear essas propriedades ou tratá-las como covariáveis.

### Experimento sugerido

Gerar pares de creative variants com a mesma mensagem e layouts visualmente pareados, variando processo criativo humano/IA. Comparar:

- ranking por gaze bruto;
- ranking por gaze controlado por propriedades visuais;
- ranking multimétrico com compreensão e lembrança.

Levar os finalistas para Meta Ads e medir CTR, CPL e conversão.

### Impacto potencial

**Muito alto metodologicamente.** Reforça e atualiza o card existente `interpretacao-atencao-eye-tracking`.

### Limites

É preprint não revisado por pares, com 39 participantes. Não mediu CTR, CPL ou vendas. A maior preferência forced-choice por anúncios de IA não pode ser transportada para eficácia comercial.

**Fonte:** https://www.preprints.org/manuscript/202609.0695

---

## 3. Agentic commerce: a IA muda a decisão, mas o consumidor ainda quer controle na hora de agir

### O que aconteceu

Em 9 de setembro, a Visa publicou seu primeiro “Trust Index” para agentic commerce, baseado em survey conduzido pela Harris Poll com 2.065 consumidores dos Estados Unidos. Apenas 23% disseram confiar em GenAI para lidar com pagamentos em seu nome. Quando uma marca de pagamentos conhecida entra na equação, a confiança muda: 61% disseram confiar na Visa para transações agentic.

Também em 9 de setembro, a PYMNTS Intelligence informou que quase 50 milhões de adultos nos EUA já começam pesquisa de varejo com IA. Entre consumidores que usam IA em sites de lojistas, 58% preferem navegar com a tecnologia e concluir a compra por conta própria. Entre os pesquisadores que usaram IA, 83% disseram que a tecnologia mudou pelo menos uma decisão, como preço, marca, produto ou varejista.

O caso de produto mais concreto veio em 8 de setembro: a Meta lançou o Muse, agente pessoal inicialmente nos EUA. O Muse pode navegar, preencher formulários e comprar, mas a Meta afirma que ele pede aprovação antes de ações sensíveis, mostra trilha de auditoria e deixa o usuário definir quais serviços conectar e quanto acesso conceder.

### Desejo/comportamento revelado

O usuário parece separar duas coisas:

**“Ajude-me a decidir e preparar”** de **“execute algo irreversível no meu lugar.”**

A IA já influencia fortemente descoberta e escolha, mas pagamento e outras ações sensíveis exigem confiança adicional, controle explícito e infraestrutura conhecida.

### Por que importa

Isso é diretamente aplicável a Click-to-WhatsApp e aos futuros agentes do Marketing Hub. A melhor experiência pode não ser nem “perguntar confirmação a cada passo” nem “agir sem perguntar”, mas uma autonomia delimitada.

### Aplicação no Marketing Hub

Criar `DelegationPolicy` com:

- `allowed_actions`;
- `sensitive_actions`;
- `requires_confirmation`;
- `max_value`;
- `trusted_execution_provider`;
- `audit_trail`;
- `permission_expiry`;
- `data_use_boundary`.

O agente pode pesquisar, resumir, comparar, preencher rascunhos e preparar checkout; envio, pagamento ou ações irreversíveis exigem confirmação conforme a política.

### Experimento sugerido

Comparar três experiências no WhatsApp:

1. confirmação em cada microetapa;
2. autonomia para etapas reversíveis + confirmação somente em ações sensíveis;
3. automação excessiva.

Medir tempo até conclusão, abandono, necessidade de correção, confiança, pedido de humano e conversão.

### Impacto potencial

**Muito alto para arquitetura de agentes.** É uma ponte entre comportamento do consumidor e design técnico de permissions/harness.

### Limites

Os surveys são dos EUA, autorrelatados e produzidos por empresas com interesse comercial em pagamentos. O Muse é recém-lançado e não prova adoção ou preferência. A aplicação precisa ser validada com usuários brasileiros.

**Fontes:**

- https://usa.visa.com/about-visa/newsroom/press-releases.releaseId.22736.html
- https://www.pymnts.com/news/artificial-intelligence/2026/ai-takes-the-first-step-in-shopping-while-consumers-keep-the-buy-button/
- https://about.fb.com/news/2026/09/introducing-muse-personal-ai-agent/

---

## Cards selecionados nesta rodada

### `prova-humana-em-recomendacao-ia`

Criado porque transforma um achado comportamental novo em uma decisão prática: prova social deve ser verificável e utilizável por agentes, não apenas persuasiva visualmente.

### `interpretacao-atencao-eye-tracking`

Nova versão do card existente. O preprint acrescenta evidência direta em anúncios e um novo guardrail: controlar propriedades visuais antes de atribuir diferenças de gaze à origem humana ou IA.

### `delegacao-agente-com-controle-humano`

Criado porque três sinais convergem em um princípio de produto: IA pode ter autonomia em descoberta e preparação, enquanto ações sensíveis precisam de permissões, confirmação e auditabilidade.

---

## Prioridade recomendada

1. **`DelegationPolicy`** — maior impacto arquitetural imediato para agentes e Click-to-WhatsApp.
2. **`CreatorEvidence` / prova humana legível por agentes** — forte ponte entre social proof e AI discovery.
3. **Atualização do `AttentionInterpretationGuardrail`** — essencial antes de investir em eye-tracking ou biometria em escala.

## Síntese

O princípio desta rodada é:

**IA comprime opções → humanos fornecem evidência de confiança → o olhar precisa ser interpretado com controle experimental → o agente reduz esforço → o usuário reassume o controle nas ações sensíveis.**

O Marketing Hub pode aproveitar isso sem confundir evidência externa com resultado comercial: cada hipótese deve terminar em um evento observável do funil — compreensão, CTA, lead, checkout, pagamento ou satisfação.
