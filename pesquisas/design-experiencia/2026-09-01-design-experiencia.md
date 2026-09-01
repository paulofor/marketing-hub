# Radar diário — Design de Experiências para Produtos Digitais

**Data:** 2026-09-01  
**Escopo:** UX, emotional design, behavioral design, adaptive UX, Generative UI, Human-AI Interaction, agentes, voz, multimodalidade, atenção, haptics e personalização.

## Resumo executivo

A rodada de hoje reforça uma mudança importante: o futuro da experiência digital parece menos centrado em uma interface fixa e mais em um sistema que decide **qual forma de interação é melhor para aquela tarefa e para aquele usuário**. A evidência mais forte desta atualização vem de Generative UI, UX de agentes, voz como sensor implícito de experiência e haptics individualizados.

Os sinais principais são:

1. **Generative UI não é apenas estética:** em tarefas complexas, interfaces geradas dinamicamente foram preferidas a chat linear e reduziram carga cognitiva; porém, em tarefas simples, a UI extra pode atrapalhar.
2. **Gerar interface em uma única tentativa é inferior:** representação estruturada, critérios adaptativos e ciclos de avaliação/refinamento melhoram a experiência.
3. **IA gera interfaces funcionais, mas tende ao convencional:** eficiência e usabilidade aparecem melhores que originalidade e inovação.
4. **UX de agentes exige controle humano explícito:** confirmação, interrupção, auditabilidade, indicação de incerteza e transparência começam a aparecer como componentes centrais da experiência.
5. **A própria voz do usuário pode funcionar como sensor de UX em tempo real**, permitindo adaptação sem depender apenas de questionários posteriores.
6. **Haptics não deve ser desenhado para um “usuário médio”:** respostas emocionais e preferências variam fortemente entre indivíduos.
7. **Experiências multissensoriais também criam novos dark patterns:** vibração pode influenciar decisões e até empurrar usuários para escolhas menos privadas.

---

# Atualização prioritária — 08:07

## 1. Generative UI: quando a interface é gerada para a tarefa

O trabalho **Generative Interfaces for Language Models**, revisado em maio de 2026, compara interfaces geradas dinamicamente com interfaces conversacionais tradicionais. Em vez de sempre devolver texto em um chat, o sistema transforma a intenção do usuário em uma estrutura de interação, gera componentes executáveis e refina o resultado iterativamente.

### Evidência

Na avaliação humana, Generative UI venceu a interface conversacional baseada em Claude 3.7 em **84%** das comparações gerais. O ganho foi especialmente alto em tarefas de informação estruturada: **93,8%** de preferência em análise/visualização de dados e **87,5%** em estratégia/operações de negócio. Em aplicações avançadas de IA/ML, porém, a preferência caiu para **50%**, mostrando que nem toda pergunta precisa virar uma interface.

O estudo também encontrou que:

- refinamento iterativo trouxe cerca de **+14 pontos percentuais** na taxa de vitória em relação à geração one-shot;
- retirar o reward adaptativo reduziu o desempenho geral em cerca de **17 pontos percentuais**;
- 78,5% dos comentários ligados a carga cognitiva/intuição favoreceram Generative UI.

### Mecanismo

**Cognitive offloading:** a interface externa parte da complexidade para o usuário. Em vez de manter toda a estrutura mentalmente em uma conversa longa, ele recebe controles, agrupamentos, estados e visualizações adequados à tarefa.

### Implicação de produto

Não construir “um chatbot para tudo”. Criar um **roteador de experiência**:

`intenção -> complexidade da tarefa -> escolher chat ou UI -> gerar estrutura -> avaliar -> refinar -> entregar`

### Hipótese de experimento

Comparar três variantes para a mesma tarefa:

- A: resposta textual;
- B: UI gerada diretamente em uma tentativa;
- C: UI gerada a partir de representação estruturada + avaliação/refinamento.

Medir tempo até conclusão, erros, abandono, compreensão e preferência.

### Risco/limite

A geração dinâmica aumenta latência, pode criar barreiras de acessibilidade e uma apresentação visual muito polida pode elevar confiança indevida em conteúdo incorreto.

Fonte: https://arxiv.org/html/2508.19227v3

---

## 2. Um agente pode gerar os controles de que precisa durante a conversa

O preprint **Macaron-A2UI**, de 24 de maio de 2026, trata Generative UI como camada natural para agentes pessoais. O agente produz linguagem e, simultaneamente, pequenas ações de UI executáveis para coletar informações, refinar preferências, confirmar decisões e organizar vários objetivos.

### Evidência

O melhor modelo alcançou **75,6** no benchmark A2UI-Bench sem receber explicitamente o schema completo da interface, superando o baseline frontier com schema explícito reportado pelos autores.

### Mecanismo

A conversa deixa de ser apenas uma sequência de mensagens. A cada momento, o agente pode selecionar o tipo de interação com menor esforço cognitivo:

`perguntar -> selecionar -> comparar -> confirmar -> executar`

### Implicação de produto

O componente principal de um produto agentic pode não ser uma tela fixa, mas um conjunto de **primitivas de experiência** que o agente combina:

- escolha única/múltipla;
- comparação;
- confirmação;
- timeline;
- formulário mínimo;
- controle de execução;
- resultado visual;
- ação reversível.

### Hipótese de experimento

Em um fluxo de compra, planejamento ou configuração, comparar chat puro com chat + controles gerados somente nos momentos de decisão.

### Risco/limite

Benchmark ainda é controlado e o trabalho é preprint. A capacidade de gerar UI não garante que a escolha do componente seja psicologicamente adequada ao usuário real.

Fonte: https://arxiv.org/abs/2605.24830

---

## 3. IA cria interfaces utilizáveis, mas ainda pouco memoráveis

Um estudo brasileiro apresentado no SEMISH 2026 avaliou protótipos criados por IA e por humanos com **92 participantes**, sem revelar a autoria.

### Evidência

Os protótipos de IA receberam avaliações positivas nas dimensões pragmáticas do UEQ-S, como usabilidade e eficiência, mas avaliações neutras ou negativas nas dimensões hedônicas, incluindo originalidade e inovação.

### Mecanismo

Modelos generativos tendem a recombinar padrões de UI muito frequentes nos dados de treinamento. Isso reduz risco funcional, mas também reduz surpresa, identidade e diferenciação percebida.

### Implicação de produto

Separar o processo em duas funções:

`IA garante coerência/usabilidade -> direção criativa introduz identidade/novidade`

A IA pode ser excelente para o “esqueleto pragmático”, mas não deve automaticamente definir toda a linguagem emocional da experiência.

### Hipótese de experimento

Gerar uma UI-base por IA e depois criar uma segunda versão com uma etapa explícita de “divergência hedônica”, pedindo alternativas incomuns de narrativa, ritmo, microinterações e feedback. Avaliar UEQ-S pragmático e hedônico separadamente.

### Risco/limite

A amostra e os tipos de protótipo restringem generalização. O estudo avalia protótipos, não produtos usados por meses.

Fonte: https://arxiv.org/abs/2605.15124

---

## 4. UX de agentes: controle humano pode ser mais importante que “magia”

Um framework publicado como preprint em 22 de julho de 2026 propõe oito princípios de UX para interação com agentes de IA no trabalho:

1. controle humano;
2. confiabilidade, segurança e robustez;
3. consciência de contexto;
4. transparência;
5. governança de dados;
6. integração com o ecossistema;
7. parceria colaborativa;
8. usabilidade.

### Evidência

Na pequena pesquisa de priorização do estudo, **65%** colocaram controle humano entre seus três princípios mais importantes, **40%** o colocaram em primeiro lugar e **95%** exigiram confirmação humana antes de uma decisão empresarial crítica. Transparência também apareceu fortemente: **75%** queriam ações e resultados facilmente verificáveis e **60%** queriam explicações claras de como/por que o agente chegou ao resultado.

### Mecanismo

À medida que o sistema ganha autonomia, parte da UX migra de “como operar” para **como supervisionar**. A sensação de agência passa a depender de conseguir entender, interromper, confirmar, corrigir e auditar o que a IA faz.

### Implicação de produto

Para agentes, os componentes UX fundamentais podem ser:

`objetivo -> plano -> execução observável -> pontos de confirmação -> possibilidade de interromper -> histórico/auditoria`

### Hipótese de experimento

Comparar um agente “invisível” que executa tudo automaticamente com outro que mostra plano, progresso, incerteza e checkpoints somente em ações de maior risco. Medir confiança calibrada, tempo, taxa de correção e preferência.

### Risco/limite

O framework é preprint, com amostras pequenas e profissionais já experientes em IA; não demonstra causalmente maior adoção em produção.

Fonte primária: https://arxiv.org/abs/2607.19941

---

## 5. A voz do usuário pode revelar a experiência antes de ele responder um questionário

O trabalho **Beyond Words: Measuring User Experience through Speech Analysis in Voice User Interfaces**, CHI 2026, recebeu Honorable Mention e avaliou 49 participantes em três personas de assistente de voz e três cenários.

### Evidência

Características da fala, como centroid espectral, jitter e taxa de engajamento, variaram de acordo com UX positiva, neutra e negativa. Modelos de machine learning chegaram a **76,47% de acurácia** na classificação desses níveis a partir da fala.

### Mecanismo

Experiência negativa altera características paralinguísticas: estabilidade vocal, energia/espectro, pausas e engajamento. Assim, o usuário fornece um sinal contínuo de experiência mesmo sem declarar explicitamente “estou frustrado”.

### Implicação de produto

Uma interface de voz pode formar um loop adaptativo:

`fala -> sinais paralinguísticos -> estimativa de estado -> ajuste de velocidade/tom/profundidade -> nova fala`

Isso permite medir experiência **durante** a jornada, não apenas depois.

### Hipótese de experimento

Detectar sinais simples de baixa confiança/frustração e testar uma intervenção mínima: resposta mais curta, confirmação do entendimento ou mudança de velocidade. Comparar com uma versão não adaptativa.

### Risco/limite

Inferir emoções pela voz é probabilístico, pode variar culturalmente e pode criar preocupações de privacidade. O produto deve preferir sinais não lexicais minimizados e deixar clara a coleta quando aplicável.

Fonte: https://doi.org/10.1145/3772318.3791747

---

## 6. Haptics: a mesma vibração não produz a mesma experiência em todas as pessoas

Um artigo do **IEEE Transactions on Haptics**, publicado em 4 de agosto de 2026, modelou desempenho, emoção e preferência em uma tarefa haptics com 30 participantes.

### Evidência

Os modelos populacionais encontraram tendências consistentes de desempenho, mas **emoção e preferência variaram substancialmente entre indivíduos**. Modelos individualizados revelaram padrões que desapareciam quando os dados eram agregados.

### Mecanismo

A percepção tátil é altamente dependente de diferenças individuais, contexto e interpretação. Otimizar apenas a média pode produzir uma experiência tecnicamente correta, mas emocionalmente ruim para subgrupos relevantes.

### Implicação de produto

Haptics pode entrar na mesma lógica da personalização de conteúdo:

`perfil/interação -> intensidade/padrão -> resposta -> ajuste individual`

### Hipótese de experimento

Permitir uma breve calibração inicial de feedback tátil e comparar retenção, preferência e taxa de desativação com uma vibração padrão única.

### Risco/limite

Estudo pequeno e realizado com hardware/tarefa específicos; não implica que toda vibração de smartphone precise de um modelo individual complexo.

Fonte: https://pubmed.ncbi.nlm.nih.gov/42550743/

---

## 7. Alerta ético: “dark haptics” demonstra que vibração também pode manipular escolha

Um estudo sobre **Dark Haptics** mostrou que estímulos táteis desagradáveis associados a determinadas respostas conseguiram influenciar alguns participantes a mudar a escolha e aceitar opções mais invasivas à privacidade.

### Evidência

No experimento com **40 participantes**, feedback vibrotátil alarmante foi aplicado quando usuários rejeitavam opções invasivas de privacidade. Parte deles mudou sua resposta após o estímulo.

### Mecanismo

Feedback tátil captura atenção e cria uma resposta aversiva imediata. Se for associado sistematicamente a uma alternativa, pode funcionar como pressão comportamental sem precisar alterar texto ou aparência visual.

### Implicação de produto

A ética do design de experiência precisa ser **multimodal**. Revisões de dark patterns não podem verificar apenas texto, cor e posição de botões; devem incluir som, voz, vibração e intensidade de animação.

### Hipótese de governança

Criar um teste automático/manual para cada intervenção:

`a modalidade facilita compreensão?`  
`ou cria custo emocional desproporcional para uma das escolhas?`

### Risco/limite

O estudo é exploratório e não quantifica efeitos de longo prazo, mas demonstra que manipulação sensorial é tecnicamente plausível.

Fonte: https://arxiv.org/abs/2504.08471

---

# Síntese aplicada: Experience Engine v2

Os achados de hoje permitem evoluir o conceito anterior de Experience Engine.

```text
Usuário
   ↓
Intenção + contexto + sinais de interação
   ↓
Estimativa de estado / necessidade
   ↓
Classificador de experiência
   ├─ chat simples
   ├─ Generative UI
   ├─ voz
   ├─ visualização
   ├─ ação do agente
   └─ feedback multimodal
   ↓
Representação estruturada da interação
   ↓
Geração da experiência
   ↓
Avaliação automática + guardrails
   ↓
Entrega
   ↓
Sinais explícitos + implícitos de UX
   ↓
Refinamento / personalização
```

## O ponto mais importante

**Não gerar mais interface; gerar a quantidade certa de interface.**

O sistema deve decidir se o usuário precisa de texto, controles, visualização, confirmação, voz ou simplesmente da execução de uma ação. Essa decisão passa a ser parte central do design.

---

# Experimentos que parecem mais promissores

### A. Chat vs Generative UI seletiva
Ativar UI dinâmica somente quando a tarefa envolver comparação, múltiplas etapas, muitos dados ou decisão.

**Métrica:** conclusão, tempo, erros, preferência e carga cognitiva.

### B. Agent UX com checkpoints adaptativos
Permitir autonomia em ações reversíveis e exigir confirmação em ações irreversíveis/financeiras/externas.

**Métrica:** confiança calibrada, correções, desistência e tempo.

### C. UX implícita por voz
Usar apenas sinais acústicos não lexicais para detectar degradação de experiência e adaptar a próxima resposta.

**Métrica:** recuperação após erro, duração da sessão e avaliação subjetiva.

### D. Haptics personalizados
Permitir calibração de intensidade e padrão em vez de feedback único.

**Métrica:** preferência, desativação do recurso e reconhecimento de eventos.

---

# Fontes da rodada

- Chen et al. — Generative Interfaces for Language Models: https://arxiv.org/html/2508.19227v3
- Kong et al. — Macaron-A2UI: https://arxiv.org/abs/2605.24830
- Romero et al. — Usable but Conventional: https://arxiv.org/abs/2605.15124
- Paimann, Valarini & Juhl — UX Principles for Human-AI Agent Interaction: https://arxiv.org/abs/2607.19941
- Ma et al. — Beyond Words / CHI 2026: https://doi.org/10.1145/3772318.3791747
- Liechty et al. — Modeling Subjective UX in Haptics: https://pubmed.ncbi.nlm.nih.gov/42550743/
- Tang et al. — Dark Haptics: https://arxiv.org/abs/2504.08471

---

# Achados da rodada inicial preservados

A versão inicial deste arquivo já havia registrado cinco linhas de pesquisa que continuam relevantes e serão mantidas como contexto, sem repetição detalhada nesta atualização:

1. **Adaptabilidade inteligente e personalização** como fortes fatores de aceitação em companheiros virtuais com IA.  
   Fonte: https://www.frontiersin.org/journals/psychology/articles/10.3389/fpsyg.2026.1827571/full

2. **Microemoções** como unidade de análise durante pequenas interações, com proposta do método Micro-Emotion Scan.  
   Fonte: https://www.sciencedirect.com/science/article/pii/S2949782526000010

3. Avaliação de **interfaces geradas por IA pela emoção transmitida**, combinando valence-arousal e eye tracking.  
   Fonte: https://www.sciencedirect.com/science/article/pii/S0141938225002987

4. **Design emocional e sustentação de atenção** em ambientes digitais com distrações concorrentes.  
   Fonte: https://www.sciencedirect.com/science/article/pii/S0360131525000818

5. Framework **VITAL**, conectando mudança comportamental, técnicas de intervenção, personalização e gamificação.  
   Fonte: https://www.sciencedirect.com/science/article/pii/S1877050925030595

---

## Direção para as próximas rodadas

Priorizar achados novos sobre:

- quando Generative UI é superior a chat e quando não é;
- modelos de estado do usuário;
- UX de agentes autônomos;
- controle, interrupção e observabilidade de agentes;
- sinais implícitos de frustração/confusão/interesse;
- voz como sensor de UX;
- haptics e experiências multissensoriais;
- microemoções e memória da experiência;
- personalização individual vs. média populacional;
- métricas dinâmicas de UX para sistemas probabilísticos;
- guardrails contra persuasão/manipulação multimodal;
- métodos experimentais que possam ser convertidos em funcionalidades de produtos digitais.
