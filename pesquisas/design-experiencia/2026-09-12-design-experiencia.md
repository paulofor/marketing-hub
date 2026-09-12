# Radar de Design de Experiência — 2026-09-12

## Resumo executivo

A rodada de 12 de setembro de 2026 aponta para um princípio comum: **boa UX adaptativa não deve simplesmente adicionar mais inteligência, controle ou intervenção; ela precisa calibrar linguagem, ajuda, participação e recuperação conforme a tarefa e a capacidade atual do usuário**.

Os achados mais úteis de hoje foram:

1. em chatbots, qualidade da linguagem deve ser tratada como parte da qualidade do serviço, não como acabamento estético;
2. após falhas de GenAI em tarefas utilitárias, restaurar percepção de competência parece mais importante do que apenas aumentar warmth/acolhimento;
3. um sistema adaptativo pode ter como objetivo explícito reduzir sua própria intervenção à medida que a autonomia do usuário aumenta;
4. mais controle em ferramentas co-criativas pode elevar novidade e liberdade, mas também aumentar carga cognitiva quando parâmetros e efeitos não são claros;
5. personalização e co-criação são mecanismos diferentes: o sistema pode adaptar-se ao usuário sem que ele participe ativamente da formação da experiência;
6. em contextos sensíveis, feedback multimodal gerado por IA ainda pode exigir uma camada humana de validação substancial.

---

## 1. A linguagem do chatbot é parte do serviço, não apenas do tom

**Descoberta.** O artigo *Language as a sales tool: Chatbot language quality in digital service operations*, publicado em 11 de setembro em *Management & Marketing*, trata explicitamente a qualidade linguística como dimensão operacional do serviço digital.

**Evidência.** O estudo analisou 411 respondentes. Qualidade linguística percebida esteve positivamente associada à confiança (`β = 0,283; p < 0,001`). Qualidade linguística e confiança explicaram conjuntamente 50,7% da variância em satisfação. A intenção de reengajamento esteve mais diretamente associada à satisfação (`β = 0,287; p < 0,001`); o efeito direto de confiança sobre reengajamento tornou-se não significativo quando satisfação entrou no modelo. O construto de qualidade linguística reuniu elementos como correção, clareza semântica, coerência, adequação pragmática e consistência estilística.

**Mecanismo psicológico/comportamental.** Em uma interface conversacional, o usuário não experimenta o modelo diretamente; ele experimenta a resposta. Clareza, coerência e adequação reduzem esforço interpretativo e incerteza e ajudam a transformar capacidade técnica em qualidade percebida de serviço.

**Implicação para produto.** Latência, automação e taxa de resolução são insuficientes como métricas de customer-agent. É útil medir separadamente qualidade linguística e informacional: clareza, coerência com o histórico, completude, adequação à situação e precisão.

**Hipótese/experimento.** Comparar uma variante otimizada principalmente para persona/tom com outra otimizada para clareza, coerência contextual e qualidade informacional. Medir resolução, re-prompt, satisfação, CTA e abandono.

**Riscos/limites.** O estudo é transversal e usa intenção autorrelatada; a amostra é predominantemente jovem e eslovaca. Linguagem muito fluida também pode aumentar confiança em informação errada, portanto qualidade verbal não substitui validação factual.

Fonte primária:
- https://link.springer.com/article/10.1007/s44491-026-00025-6

**Atualização material de card.** Este achado reforça e amplia o card existente `chatbot-commerce-qualidade-informacao-continuidade`, criado em 10/09. Em vez de criar uma chave nova, foi gerada uma nova versão candidata com o mesmo `cardKey`, agora apoiada por duas evidências recentes complementares.

---

## 2. Após falhas utilitárias de GenAI, recuperar competência pode importar mais que aumentar calor social

**Descoberta.** *Service type conditions stance attribution effects on switching after generative AI failures*, publicado em 11 de setembro na *Scientific Reports*, investigou como diferentes formas de enquadrar a IA alteram a reação do usuário depois de falhas, distinguindo tarefas mecânicas/utilitárias de tarefas emocionais.

**Evidência.** O trabalho combinou experimento ERP e experimento de cenário. No estudo de cenário, pistas que favoreciam uma interpretação mais intencional da IA aumentaram percepções de `warmth` e `competence`, mas somente `competence` mediou menor intenção de trocar de serviço. O efeito indireto apareceu em tarefas mecânicas e não nas emocionais; o caminho via warmth não foi significativo.

**Mecanismo psicológico/comportamental.** Em uma tarefa utilitária, a falha ameaça principalmente a crença de que o sistema sabe executar. Uma resposta simpática pode suavizar o encontro, mas não necessariamente restaura a expectativa de capacidade. Demonstrar correção e capacidade funcional pode ser mais relevante.

**Implicação para produto.** O recovery de um agente não deveria ser uma mensagem genérica de desculpas. O sistema pode classificar a tarefa e, em falhas utilitárias, usar um padrão como: `reconhecer erro → corrigir concretamente → mostrar evidência/capacidade → oferecer próxima ação verificável`.

**Hipótese/experimento.** Após uma falha equivalente, comparar recuperação centrada em empatia com recuperação centrada em correção + competência. Medir abandono, repetição do contato, resolução, confiança calibrada e satisfação.

**Riscos/limites.** O desfecho principal é intenção de troca, não churn observado. O efeito é dependente do tipo de serviço e não deve ser convertido em regra universal. Pistas antropomórficas também podem elevar expectativas e aumentar decepção em outros contextos.

Fonte primária:
- https://www.nature.com/articles/s41598-026-71320-9

---

## 3. Um sistema adaptativo pode aprender a ajudar menos à medida que o usuário melhora

**Descoberta.** *A hybrid deep learning and meta-reinforcement learning architecture for adaptive AI-assisted fine motor control training in older adults*, publicado em 11 de setembro na *Scientific Reports*, inclui no próprio objetivo de otimização uma tensão importante: maximizar desempenho sem impedir o desenvolvimento de autonomia.

**Evidência.** A arquitetura usa representação de estado a partir da interação com joystick e meta-reinforcement learning. A função de recompensa combina conclusão da tarefa, autonomia do usuário, intensidade da intervenção e progresso de habilidade. Nos resultados reportados, desempenho e aquisição de habilidade melhoraram, enquanto a proporção de autonomia aumentou progressivamente e a intervenção da IA diminuiu ao longo do treinamento.

**Mecanismo psicológico/comportamental.** Ajuda excessiva pode aumentar desempenho imediato e, ao mesmo tempo, impedir que a pessoa exercite a própria capacidade. Se o sistema reconhece progresso, ele pode reduzir scaffolding e devolver dificuldade/controle ao usuário.

**Implicação para produto.** Para agentes que ensinam, orientam ou auxiliam, adicionar um objetivo de **assistance decay**: quando sinais de domínio sobem, diminuir sugestões, completar menos etapas automaticamente e preservar escolhas relevantes para o usuário.

**Hipótese/experimento.** Comparar ajuda constante com ajuda adaptativa que diminui quando o usuário demonstra domínio. Medir conclusão imediata, necessidade de ajuda em tarefas futuras, correções e capacidade de executar sem IA.

**Riscos/limites.** O domínio é treinamento motor de adultos mais velhos e não um produto comercial genérico. O método usa sinais específicos de joystick e uma arquitetura de pesquisa; extrapolar para UX conversacional exige validação própria.

Fonte primária:
- https://www.nature.com/articles/s41598-026-70013-7

**Card.** Não criado: o princípio é forte para arquitetura de agentes e aprendizagem, mas nenhuma coleção atual o representa de forma limpa sem distorção.

---

## 4. Co-criação com IA expõe um paradoxo: mais controle pode aumentar criatividade e carga cognitiva ao mesmo tempo

**Descoberta.** *Co-creating eco-visualizations: a human–AI collaborative pipeline for data-driven visual design*, publicado em 11 de setembro, avaliou uma ferramenta em que designers controlam geração, segmentação, mapeamento de dados e parâmetros visuais em uma sequência co-criativa.

**Evidência.** O estudo teve 13 participantes, 12 deles com background em design, sem treinamento prévio. O NASA-TLX médio foi 59 (`SD=16,1`), indicando carga alta. No UEQ-S, a qualidade pragmática ficou em `-0,96`, enquanto a hedônica ficou em `+1,33`: a ferramenta foi percebida como inovadora/atraente, mas difícil. Relatos apontaram dificuldade para entender finalidade e efeito de parâmetros; os autores sugerem mais orientação contextual e progressive disclosure.

**Mecanismo psicológico/comportamental.** Controle só produz agência quando a pessoa compreende suas consequências. Muitos parâmetros desconhecidos aumentam decisão, exploração e memória de trabalho. O mesmo recurso que aumenta liberdade expressiva pode diminuir fluidez operacional.

**Implicação para produto.** Generative UI e ferramentas de criação deveriam separar controles orientados a resultado de controles técnicos avançados. Mostrar primeiro intenção/efeito (`mais sóbrio`, `mais contraste`, `mais exploração`) e revelar parâmetros finos sob demanda pode preservar poder sem despejar complexidade.

**Hipótese/experimento.** Comparar todos os controles expostos desde o início versus defaults orientados a objetivo + progressive disclosure. Medir tempo, erros, carga, qualidade percebida e diversidade do resultado.

**Riscos/limites.** N=13 e domínio bastante específico; parte da baixa usabilidade decorreu de latência do sistema executado em CPU, não apenas da arquitetura de interação.

Fonte primária:
- https://link.springer.com/article/10.1007/s12652-026-05128-w

**Card.** Não criado. O achado reforça o card existente `progressive-disclosure-carga-cognitiva-ai`, mas a amostra pequena e a sobreposição conceitual não justificam uma nova versão hoje.

---

## 5. Personalização e co-criação não são a mesma coisa

**Descoberta.** *Perceived AI Service Intensity and Behavioral Intentions Toward Hotels: An AI-Enabled Value Realization Model*, publicado em 11 de setembro, separa explicitamente dois mecanismos que produtos digitais frequentemente misturam: **personalização**, quando o serviço se adapta à pessoa, e **value co-creation**, quando a pessoa participa com escolhas, inputs e feedback na formação da experiência.

**Evidência.** O estudo transversal reuniu 1.733 hóspedes de hotéis em 12 cidades da China que haviam usado serviços com IA. Personalização apareceu entre as associações mais fortes com intenções comportamentais, e o modelo manteve personalização e co-criação como construtos separados. Os próprios autores deixam explícito que as relações estruturais são associativas e não comprovam sequência temporal ou causalidade.

**Mecanismo psicológico/comportamental.** Uma experiência pode parecer pessoal porque o sistema inferiu preferências silenciosamente. Isso é diferente de sentir participação e autoria porque o usuário escolheu, corrigiu ou ajudou a moldar o resultado. O primeiro reduz esforço; o segundo pode aumentar agência e compromisso.

**Implicação para produto.** Adaptive UX pode decidir quando inferir silenciosamente e quando pedir participação. Para pequenas preferências reversíveis, adaptar automaticamente pode ser melhor; para decisões de identidade, criatividade ou consequência elevada, permitir que o usuário molde explicitamente o resultado pode ser mais apropriado.

**Hipótese/experimento.** Comparar `personalização automática`, `personalização editável` e `co-criação explícita`, medindo esforço, relevância percebida, sensação de autoria, correções e comportamento posterior.

**Riscos/limites.** Contexto de hotelaria e desenho transversal; intenções não equivalem a comportamento real. Personalização silenciosa também pode criar sensação de vigilância quando usa dados que o usuário não esperava.

Fonte primária:
- https://www.mdpi.com/2673-5768/7/9/295

**Card.** Não criado nesta rodada: apesar da amostra grande, é uma evidência associativa e específica de hospitalidade. Vale acompanhar antes de transformar a distinção em orientação persistente do Marketing Hub.

---

## 6. Em feedback multimodal sensível, “AI generated” ainda não significa “pronto para entregar”

**Descoberta.** *Parent’s AI Coach (PaiCoach)*, publicado em 11 de setembro no *Journal of Autism and Developmental Disorders*, usa IA para analisar interações gravadas e produzir feedback de coaching contextualizado.

**Evidência.** O estudo empregou desenho single-case de múltiplas linhas de base com quatro díades mãe-criança. Houve melhora na fidelidade de implementação dos pais e na responsividade das crianças. Um dado particularmente útil para Human-AI Interaction: na validação humana, **68,6% das análises geradas pela IA foram aceitas sem modificação, enquanto 31,4% exigiram alguma edição especializada**. Os autores concluem que o sistema mostra viabilidade, mas reforçam a importância atual de supervisão humana.

**Mecanismo psicológico/comportamental.** Feedback torna-se mais útil quando é ligado ao comportamento e ao momento específico em que ocorreu, mas análise multimodal automática continua sujeita a erros de interpretação. Quanto maior a consequência da orientação, maior o valor de uma barreira de validação antes da entrega.

**Implicação para produto.** Para agentes multimodais que analisam vídeo, voz ou comportamento, separar `gerar insight` de `autorizar entrega/ação`. Em usos de maior risco, usar revisão humana ou uma etapa independente de verificação, em vez de confiar apenas na naturalidade da explicação produzida.

**Hipótese/experimento.** Em um domínio não clínico, comparar feedback genérico, feedback ancorado em evento/momento e feedback ancorado + verificação, medindo utilidade percebida, erros detectados e ação correta posterior.

**Riscos/limites.** N=4, crianças e contexto clínico/educacional sensível; não é base para automação equivalente em outros domínios. O percentual de edições também depende do sistema e do padrão de validação empregados.

Fonte primária:
- https://link.springer.com/article/10.1007/s10803-026-07519-6

**Card.** Não criado: além da amostra pequena, a evidência vem de um contexto sensível de saúde/desenvolvimento infantil e não deve ser generalizada para o Marketing Hub.

---

## Síntese arquitetural — Experience Engine v12

```text
usuário
   ↓
tarefa + estado + histórico
   ↓
COMMUNICATION QUALITY GATE
   ├── clareza
   ├── coerência
   ├── precisão
   └── adequação pragmática
   ↓
ASSISTANCE POLICY
   ├── intervir
   ├── orientar
   └── reduzir ajuda conforme autonomia cresce
   ↓
CONTROL / GUIDANCE BALANCER
   ├── defaults orientados a resultado
   └── controles avançados sob demanda
   ↓
PERSONALIZATION / CO-CREATION ROUTER
   ├── adaptar silenciosamente quando seguro e reversível
   └── pedir participação quando autoria/critério importa
   ↓
EXECUÇÃO
   ↓
FAILURE RECOVERY ROUTER
   ├── utilitário → correção + competência verificável
   └── emocional → estratégia específica ao contexto
   ↓
HUMAN / INDEPENDENT VALIDATION GATE
   └── proporcional ao risco da consequência
   ↓
resultado + autonomia futura + satisfação + comportamento real
```

O princípio que eu carregaria desta rodada é: **uma boa experiência adaptativa não tenta ajudar ao máximo; ela tenta fornecer a ajuda certa e sair gradualmente do caminho quando o usuário já consegue avançar sozinho**. O mesmo vale para controle, personalização e recuperação de falhas: mais intervenção não é automaticamente melhor.

---

## Cards criados

### 1. `chatbot-commerce-qualidade-informacao-continuidade`

- **Coleção:** `neuromarketing`
- **Tipo:** atualização material de um `cardKey` existente, preservando versionamento da mesma ideia.
- **Por que importa:** a nova evidência adiciona uma dimensão concreta — qualidade linguística — ao princípio já observado de que qualidade funcional e continuidade são fundamentais no customer-agent. Isso torna o card mais acionável para rubrics, avaliação e A/B tests.
- **Fonte revisada:** `pesquisas/design-experiencia/cards/fontes/2026-09-12-chatbot-commerce-qualidade-informacao-continuidade.md`
- **SHA-256:** `7a5bdde30e4dbcde9b41c90c26fd1e41b68cdf3b8ad72fafcd200931f93385f7`
- **JSON:** `pesquisas/design-experiencia/cards/2026-09-12-chatbot-commerce-qualidade-informacao-continuidade.json`

### 2. `recuperacao-falha-genai-competencia-contextual`

- **Coleção:** `neuromarketing`
- **Tipo:** novo candidato.
- **Por que importa:** transforma recuperação de falha em uma hipótese contextual testável. Em tarefas utilitárias, corrigir e restabelecer competência pode ser mais importante do que apenas aumentar calor social.
- **Fonte revisada:** `pesquisas/design-experiencia/cards/fontes/2026-09-12-recuperacao-falha-genai-competencia-contextual.md`
- **SHA-256:** `a062cf86e4f9c7c1bc73c1291f1e0e3f91828fb4407a09d77ad6be51eccfa1c3`
- **JSON:** `pesquisas/design-experiencia/cards/2026-09-12-recuperacao-falha-genai-competencia-contextual.json`

Os dois arquivos são candidatos a `DRAFT`. Nenhuma ação de revisão, ativação ou arquivamento foi executada.

## Achados fortes que não viraram card

- **Assistência adaptativa que diminui com autonomia:** forte princípio arquitetural, mas sem encaixe limpo nas quatro coleções atuais.
- **Paradoxo controle × carga em co-criação:** reforça `progressive-disclosure-carga-cognitiva-ai`, mas não justificou nova versão com N=13 e fatores de latência misturados ao resultado.
- **Personalização ≠ co-criação:** relevante, porém ainda associativo e específico de hotelaria.
- **PaiCoach:** resultado interessante para validação humana de feedback multimodal, mas contexto clínico/infantil e amostra muito pequena tornam inadequada a criação de card comercial persistente.

## Coleções verificadas

O guia atual da Biblioteca do Harness continua aceitando somente: `video`, `prazer-audio-visual`, `neuromarketing` e `momentos-de-compra-b2c`. Não foi criada coleção `design-experiencia`.
