# Radar de IA Autônoma — 2026-09-09

## Rodada 17:54 — America/Sao_Paulo

Há **quatro desenvolvimentos que passam o filtro nesta rodada**. O principal caso prático é o **IBM watsonx Orchestrate AgentOps**, porque já está em disponibilidade geral e fecha boa parte do ciclo `trace/eval -> diagnóstico -> nova instrução/playbook -> reavaliação -> promoção`, embora a promoção continue humana. Entre os papers de 8 de setembro, o **Experience Funnel** é o avanço conceitual mais forte por alternar evolução rápida de estado textual com consolidação lenta nos pesos; **Procedural Graphs**, de autores ligados ao Google, faz o agente evoluir uma estrutura operacional explícita de procedimentos; e **SE-GoS** mostra que até o retrieval de skills pode aprender com traces anteriores sem mudar pesos nem o conteúdo das próprias skills.

| Caso | (1) Pesos | (2) Código/scaffold/harness | (3) Prompts/retrieval/workflows/tools/skills/estratégias | (4) Só memória | (5) Forte condução humana |
|---|---:|---:|---:|---:|---:|
| **IBM AgentOps** | Não | Não no loop principal | **Sim** | Não | **Sim — gate/promoção** |
| **Experience Funnel** | **Sim** | Não | **Sim — estado textual** | Não | **Sim — treinamento experimental** |
| **Procedural Graphs** | Não | Parcial: estrutura externa de execução | **Sim — principal** | Não | Parcial |
| **SE-GoS** | Não | Não | **Sim — retrieval graph** | Não | Parcial |

## 1. IBM watsonx Orchestrate AgentOps — self-improvement operacional já em GA

A IBM anunciou em **3 de setembro de 2026** que o AgentOps Agent, antes em preview, passou a **Generally Available em 31 de agosto** para agentes nativos do watsonx Orchestrate. O produto executa o ciclo de observabilidade, criação de testes, simulação de usuários, root-cause analysis e otimização. Para a parte de melhoria usa dois mecanismos: **GEPA**, que gera e testa novas instruções, e **ACE**, que cria/organiza um playbook de regras e mede quanto cada regra contribui para o desempenho de avaliação.

Isso entra principalmente na categoria **(3)**: o objeto persistente que muda são **instruções e playbooks/regras externas aos pesos**. Não há indicação pública de que os pesos do LLM sejam atualizados. Também não é categoria (4), porque o resultado do ciclo não é apenas memória recuperada: quando uma nova versão é aprovada, as instruções/regras do agente são efetivamente alteradas para execuções posteriores.

A autonomia ainda é limitada. A descrição oficial da preview deixa claro que o GEPA reavalia as novas instruções e, **quando o usuário está satisfeito com os resultados, permite enviar as instruções atualizadas ao agente**. Portanto o AgentOps faz boa parte do trabalho de diagnóstico e otimização, mas o gate final continua humano. Isso coloca o caso também em **(5)**.

A IBM não publicou, nesta atualização, um uplift quantitativo global de qualidade causado pelo AgentOps; portanto não há uma métrica pública comparável ao +X% dos papers. A evidência forte aqui é de **produto em produção/GA e mecanismo explícito**, não de magnitude causal da melhoria.

O padrão arquitetural reutilizável é:

`production traces/evals -> root-cause analysis -> candidate instruction/playbook -> simulation + deterministic tool checks -> compare -> human promotion -> nova versão persistente`.

Para agentes com MCP, isso encaixa muito bem em mudanças de `AGENTS.md`, instruções de tool, políticas de retry, regras de seleção de ferramenta ou playbooks operacionais. O ponto importante é separar **Optimizer** de **Promoter**.

Fonte oficial:
- https://www.ibm.com/new/announcements/new-in-ibm-watsonx-orchestrate-cross-platform-agent-discovery-custom-evaluation-and-agentops-agent-goes-ga
- https://www.ibm.com/new/announcements/new-in-ibm-watsonx-orchestrate-self-improving-agents-personalized-chat-experiences-and-faster-workflows

## 2. Experience Funnel — experiência externa vira pesos, mas só depois de provar utilidade

O paper **Experience Funnel: A State-Policy Alternating Loop for Self-Evolving Agents**, submetido em **8 de setembro de 2026**, propõe algo que faltava em muitos sistemas anteriores: tratar aprendizado externo e paramétrico como **dois ritmos diferentes do mesmo processo**.

Primeiro, as trajetórias são condensadas em um **estado textual explícito** — procedures, falhas recorrentes e estratégias corretivas. Esse estado pode ser alterado rapidamente e passa por validação held-out. Depois, o sistema compara a execução sem estado, com o estado anterior e com o estado novo para identificar comportamentos que são realmente novos ou continuam úteis. Só essa parte é então destilada para a policy, atualizando seus pesos. Depois da consolidação, a nova dupla `(estado, policy)` volta a produzir rollouts e o ciclo recomeça.

Portanto este é claramente **(1) + (3)**. O que persiste entre execuções são tanto **os pesos da policy** quanto **o estado textual externo**. Não é mera memória, porque parte da experiência deixa de precisar estar no contexto: ela é internalizada no modelo.

No SearchQA, a policy sem estado passou de **58,1% para 61,3%** depois das consolidações aceitas; combinada com o estado final chegou a **62,4%**. No conjunto dos três ambientes, o método alcançou **62,4% no SearchQA, 67,9% no ALFWorld e 42,4% no WebShop**, média de 57,6%, superando o SkillRL (56,2% em média). Uma análise de seleção de experiência mostrou que consolidar apenas comportamentos recém-úteis ou persistentemente úteis chegou a **63,0%**, contra **58,9%** ao destilar experiência sem filtro.

Há limites importantes. O experimento usa **Qwen3.5-4B** como policy em evolução e um **Qwen3.5-27B fixo como teacher** em parte da distilação. O pipeline, datasets, reward e gates são definidos por humanos; isso é aprendizagem automatizada dentro de um protocolo experimental, não RSI aberto. Em cinco rodadas de state evolution, apenas as rodadas 1 e 4 passaram o gate; as demais foram rejeitadas, o que também mostra que o loop não assume que toda nova experiência merece virar mudança persistente.

O padrão reutilizável que eu extrairia é:

`trace -> estado textual rápido -> held-out gate -> descobrir quais comportamentos realmente dependem desse estado -> internalizar só os estáveis nos pesos -> retirar do contexto o que ficou redundante -> repetir`.

Para agentes próprios, isso sugere uma arquitetura em camadas: primeiro aprender em **skills/prompts/playbooks**, onde a mudança é barata e auditável; apenas depois, quando uma regra demonstrar valor repetido e generalizável, considerar **fine-tuning/distillation** para transformá-la em competência paramétrica.

Fonte:
- https://arxiv.org/abs/2609.08919

## 3. Procedural Graphs — o agente evolui o próprio mapa operacional

O paper **Procedural Graphs: Self-Evolving Execution Structures for LLM Agents**, de autores com afiliação ao **Google**, também foi submetido em **8 de setembro**. A ideia é representar procedural knowledge como um grafo explícito de triplets `(procedure, relation, procedure)`. Nós podem representar uma tool, uma skill, um passo de reasoning ou um estado; arestas representam transições permitidas e carregam atributos como `condition`, `guidance` e `pitfalls`.

Durante a execução, o agente localiza em que nó está e recebe apenas o subgrafo relevante para decidir o próximo passo. Depois de um batch de tarefas, um LLM refiner compara trajetórias boas e ruins, adiciona ou remove nós/arestas e altera atributos. Uma candidata só é commitada se mantiver ou melhorar o desempenho em validação independente. E as candidatas rejeitadas entram numa **rejection memory**, para reduzir a chance de o Evolver repetir a mesma alteração ruim.

Eu classificaria isso principalmente como **(3)**, com uma borda para **(2)**: os pesos ficam congelados, mas uma **estrutura externa de execução/harness** realmente muda e persiste entre rodadas. Não é simples memória textual de casos anteriores; é uma política procedural explícita que modifica quais ações e tools o agente tende a executar e em que ordem.

No EnterpriseArena, em dez rodadas de self-evolution, o baseline tinha **0% de full-horizon survival**. A primeira mutação levou a 45% em validação; a segunda a 80%, com tool calls caindo de **17,23 para 3,08 por mês**; a rodada 8 chegou a 90% de survival na validação. O grafo final retornado obteve **85% de survival no test**, contra 0% do baseline. Em HotpotQA, evolução partindo de um skeleton mínimo chegou a **78,79 F1 e 66,30 EM**, e em MultiChallenge o sistema conseguiu reparar um prior humano ruim: a configuração inicial estava em 58,93% overall e a evolução iterativa chegou a **92,86%**.

A limitação é relevante: no EnterpriseArena há apenas **20 episódios por split**, então decisões de aceitar/rejeitar podem depender de um ou dois episódios. O próprio paper alerta que cada rodada deve ser lida como trajetória de busca, não como teste estatístico robusto. Há também custo extra de tokens por causa da geração de guidance.

O padrão reutilizável é muito bom para MCP:

`tool/procedure graph -> execução -> localizar passo atual -> recuperar vizinhança procedural -> agir -> coletar trace -> comparar sucesso/falha -> editar topology + conditions + pitfalls -> held-out gate -> commit/reject`.

Em vez de deixar todas as regras num prompt enorme, o agente mantém um **mapa operacional versionado** que aprende relações como “depois de X, verifique Y”, “não invoque Z antes de W” ou “esta tool costuma falhar neste estado”.

Fonte:
- https://arxiv.org/abs/2609.09153

## 4. SE-GoS — até o retrieval de skills pode evoluir com experiência

O **SE-GoS: Self-Evolving Graph-of-Skills for Skill Library at Scale**, submetido em **8 de setembro**, é particularmente relevante para agentes que começam a acumular centenas ou milhares de skills. Em vez de alterar as skills ou os pesos, ele altera **a infraestrutura que decide quais skills recuperar**.

O sistema evolui três coisas a partir dos traces: **topologia** do grafo de skills, **pesos das arestas** e **descrições usadas para retrieval**. Assim, se execuções anteriores mostram que duas skills costumam ser necessárias em conjunto, ou que determinada relação leva a resultados ruins, o grafo é reforçado, criado ou podado. O algoritmo de retrieval permanece o mesmo e o conteúdo das skills não precisa mudar.

Isso é categoria **(3)** de forma muito clara. O que persiste entre execuções é a **estrutura de retrieval**. Não é categoria (4), porque a experiência histórica foi convertida em parâmetros externos — relações, pesos e descrições — que mudam sistematicamente a seleção futura de skills.

No SkillsBench, uma rodada levou o reward de **52,4% para 59,4%**, ao mesmo tempo em que o consumo caiu de 3,67M para 3,45M tokens por tentativa e ficou cerca de 32% abaixo do full skill loading. Num split disjunto, evoluindo em 50 tarefas e testando em outras 37, o grafo passou de **52,9% para 58,3%** (+5,4 pontos), indicando algum grau de transferência em vez de pura memorização.

Mas apareceu uma limitação extremamente importante para continual learning. Repetir o processo indefinidamente não foi monotônico: **round 0 = 52,4; round 1 = 59,4; round 2 = 59,8; round 3 = 54,0**. Enquanto isso, o grafo cresceu de **863 para 1.502 arestas**. Ou seja, evolução de retrieval também pode sofrer **overfitting e bloat**. Isso reforça que o sistema precisa de gate/rollback por rodada, não apenas de um mecanismo para incorporar mais relações.

Para agentes próprios, o padrão reutilizável seria:

`skill registry -> retrieval graph -> execução -> quais skills realmente ajudaram? -> atualizar relações/pesos/descrições -> held-out/replay -> KEEP ou rollback`.

Isso pode ser aplicado diretamente a MCP: em vez de alterar a tool em si, o agente aprende **quando uma tool é necessária, quais dependências normalmente vêm antes e quais tools se complementam**.

Fonte:
- https://arxiv.org/abs/2609.08228

## O que mudou no desenho geral

A novidade mais importante desta rodada é que o “objeto de aprendizagem” está se tornando cada vez mais granular. Já vimos agentes evoluindo prompts, skills, harnesses e código. Hoje temos evidência nova de evolução em **quatro níveis simultaneamente**:

`instructions/playbooks (IBM) -> procedural graph (Google) -> retrieval graph (SE-GoS) -> policy weights (Experience Funnel)`.

Isso sugere uma hierarquia prática para agentes próprios:

1. **Aprender primeiro fora dos pesos**, em estruturas pequenas e auditáveis — skill, regra, aresta, descrição de tool, routing.
2. **Validar em held-out/replay** antes de persistir.
3. **Consolidar nos pesos apenas o conhecimento que provou ser estável e recorrente**.
4. Manter o que ainda é situacional como estado externo editável.
5. Aplicar versionamento e rollback não apenas a código, mas também a **retrieval graphs e procedural graphs**.

O **IBM AgentOps** é o caso mais próximo de produção desta rodada: o ciclo de melhoria está disponível comercialmente, mas **a promoção ainda depende de decisão humana** e a IBM não divulgou uplift quantitativo global. O **Experience Funnel** é o avanço conceitual mais importante para continual learning porque transforma seletivamente experiência externa em pesos. **Procedural Graphs** e **SE-GoS** reforçam a ideia de que grande parte do autoaperfeiçoamento útil pode ocorrer no harness/context layer sem tocar no backbone.

Ainda **não apareceu nesta rodada um caso convincente de recursive self-improvement forte** em que a versão melhorada assuma automaticamente o papel de Evolver da geração seguinte. Também não encontrei um novo sistema comercial demonstrando publicamente o ciclo completo `tráfego real -> variantes -> avaliação -> promoção automática -> rollback`, sem gate humano externo.
