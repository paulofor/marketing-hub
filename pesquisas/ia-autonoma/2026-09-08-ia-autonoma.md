# IA Autônoma — 2026-09-08

## Rodada 18:22 (America/Sao_Paulo)

Há três desenvolvimentos novos e relevantes nesta rodada. O principal é o **AutoLR**, da NetEase, porque é um caso industrial muito próximo do padrão procurado: hipóteses automáticas viram patches de código/configuração em um recommender real, são treinadas e avaliadas, e apenas candidatas aprovadas podem reescrever persistentemente o trunk experimental. Os outros dois avanços são **RISE**, para mudança persistente de pesos por autoextrapolação do próprio progresso de treinamento, e **Online Evolution for Computer-Use Agents**, que mantém o modelo congelado e faz a capacidade evoluir por meio de uma biblioteca externa, versionada e persistente de skills.

| Caso | (1) Pesos | (2) Código/scaffold/harness | (3) Prompts/retrieval/workflows/tools/skills | (4) Só memória | (5) Forte condução humana |
|---|---:|---:|---:|---:|---:|
| **AutoLR — NetEase/DASHEN** | **Sim, nos checkpoints do recommender-alvo** | **Sim** | **Sim** | Não | **Sim, sobretudo nos contratos de avaliação e rollout** |
| **RISE** | **Sim — principal** | Não | Não no mecanismo central | Não | **Sim — treinamento offline desenhado por humanos** |
| **Online Evolution for Computer-Use Agents** | Não | Parcial: artefatos `SKILL.md`, não o executor-base | **Sim — principal** | Não | Parcial |

### 1. AutoLR — pesquisa autônoma que modifica persistentemente um recommender industrial

O **AutoLR: Automating the Path from Research to Launch Review in Industrial Recommender Systems**, da NetEase, descreve um harness usado sobre o sistema de recomendação **DASHEN**. O loop combina um conselho de especialistas LLM, um seletor determinístico de exploração/exploitation, memória estruturada de experimentos, geração de patches, treinamento, avaliação offline e controle explícito de transição de estado. Os agentes LLM podem raciocinar e escrever código, mas os controladores determinísticos mantêm a autoridade sobre execução, extração de métricas, guardrails e mudanças persistentes.

O estado evolutivo inclui um trunk ativo `T_t`, uma base de conhecimento `K_t`, histórico de experimentos `H_t` e orçamento. A cada rodada, o sistema propõe direção de pesquisa, hipótese e patch de código/configuração, treina a candidata e mede os resultados. O controlador classifica a candidata em três estados: **DISCARD**, **PACK** ou **KEEP**. `KEEP` é o único estado que reescreve o repositório de trabalho; nesse caso, a candidata vira o novo trunk e passa a ser a base da rodada seguinte. `PACK` preserva a candidata e sua linhagem sem alterar o trunk. Esse mecanismo é um padrão particularmente útil porque separa “vale guardar” de “vale tornar baseline”.

A classificação é forte em **(2)** e **(3)**: o sistema modifica código/configuração e acumula conhecimento experimental que altera a direção de busca futura. Há também **(1)** no sistema-alvo porque cada variante do recommender é treinada e gera novos checkpoints persistentes; os pesos dos LLMs que fazem o papel de pesquisadores, porém, não são o estado adaptativo do loop.

A auditoria cobre **1.586 avaliações completas** acumuladas ao longo de vários meses em dois cenários de recomendação do DASHEN. Nove Launch Reviews de produção tiveram lifts online positivos. Somando descritivamente métricas heterogêneas reportadas pelos autores, os registros representam aproximadamente **+5,75% em penetração de consumo de conteúdo, +10,83% em tempo total de consumo e +5,55% em visualizações válidas (VV)**; esses números não são um efeito estatístico combinado e devem ser interpretados como soma dos lifts individuais publicados. Em operações rotineiras, o sistema migrou parte do trabalho para uma combinação de DeepSeek-V4-Pro/Flash, com custo de API LLM de aproximadamente **RMB 3–4 por iteração**, excluindo infraestrutura de treinamento.

A autonomia, entretanto, ainda termina antes do rollout total. O AutoLR vai autonomamente de pesquisa/proposta até modificação de código, treinamento, avaliação offline e empacotamento de candidatas, mas **engenheiros ainda selecionam quais candidatas entram em A/B online, e a Launch Review permanece gate humano para rollout amplo**. Resultados online voltam depois à base de conhecimento como evidência de produção.

O paper também documenta um problema que deve virar requisito de arquitetura: o **KEEP ratchet**. Se um threshold offline for ruidoso, uma única avaliação favorável pode mudar o trunk e fazer com que esse baseline possivelmente superestimado seja a referência para todas as gerações seguintes. Na configuração auditada, a calibração de ruído estava desativada e não havia perfil versionado de calibração correspondente. Os autores recomendam que promoção persistente exija ganho material, respeito a métricas protegidas, confirmação compatível com o protocolo e comportamento fail-closed quando a calibração não estiver disponível.

Padrão arquitetural reutilizável:

`produção/evidência → conselho de pesquisa → hipótese → patch escopado → treinamento/eval fixos → guardrails → {DISCARD, PACK, KEEP} → KEEP altera trunk → próxima rodada parte do novo trunk → A/B humano → produção → evidência retorna à memória`.

Para agentes próprios, eu adotaria especialmente dois elementos: **três estados de promoção (`discard/pack/keep`)** e **calibração versionada do evaluator antes de permitir que uma melhora reescreva o baseline**.

Fonte: https://arxiv.org/abs/2609.04871

### 2. RISE — o próprio caminho de melhoria vira um teacher para atualizar os pesos

O **RISE: Recursive Improvement via Self-Extrapolating Policy Distillation** trabalha diretamente com **(1) mudança persistente de pesos**. A ideia é usar a trajetória recente do próprio modelo durante RL com recompensas verificáveis para construir um “teacher futuro” sintético. Em vez de depender de um modelo externo mais forte, o método extrapola a direção entre o checkpoint atual e um anchor anterior, em espaço de parâmetros ou logits, e transforma essa direção de progresso em um alvo token-level mais denso para distilação.

O ciclo é: RLVR produz uma melhoria verificável; RISE estima para onde a policy está se movendo; essa policy extrapolada vira um teacher; o modelo atual destila esse sinal; o novo checkpoint então redefine o teacher na iteração seguinte. Assim, o teacher é atualizado recursivamente conforme o próprio estudante melhora. Os mesmos rollouts podem ser reutilizados, sem amostragem adicional, com overhead de wall-clock reportado em aproximadamente **1,3–1,6×**.

Nos benchmarks de agentes com Qwen2.5-3B-Instruct, o baseline tinha 21,9 em ALFWorld e 0,8% de acurácia no WebShop. GRPO chegou a 75,0 em ALFWorld e 63,3% de acurácia no WebShop. O RISE em espaço de pesos chegou a **84,4 em ALFWorld, 86,3 no WebShop Score e 74,2% de acurácia no WebShop**, ou **+9,4 pontos em ALFWorld e +10,9 pontos de acurácia no WebShop sobre GRPO**. Em matemática, também houve ganhos consistentes em diferentes famílias de modelos.

A limitação conceitual é importante: apesar do nome “recursive improvement”, isso **não é recursive self-improvement forte**. O modelo não escolhe sozinho o objetivo, o dataset, a recompensa, a regra de atualização nem assume o papel do sistema que projetará a próxima geração. É um algoritmo de treinamento humano-projetado que atualiza recursivamente o teacher a partir de checkpoints que mudam.

Há ainda uma evidência útil de segurança: quando os autores tentaram autoextrapolação sem o grounding do RLVR, o treinamento colapsou rapidamente. Em um experimento, MATH-500 caiu para **2,4%**, as respostas cresceram até o limite de 8K tokens e o reward foi a zero. Extrapolação agressiva também ficou perigosa mais tarde no treinamento, levando a quedas de cerca de 15 pontos em certas configurações; por isso o método reduz gradualmente a intensidade de extrapolação.

Padrão reutilizável para agentes que realmente atualizam pesos:

`policy atual → outcome verificável → atualização RL → estimar direção de melhoria → teacher extrapolado → distilação → novo checkpoint → recalcular teacher`.

A lição mais importante é que **auto-referência sozinha não é suficiente**; o ciclo precisa continuar ancorado em resultados verificáveis externos.

Fonte: https://arxiv.org/abs/2609.05295

### 3. Online Evolution for Computer-Use Agents — skills persistentes e versionadas com modelos congelados

O trabalho **From Interaction Traces to Persistent Skills: Online Evolution for Computer-Use Agents** é uma demonstração clara de **(3)**. Os modelos de geração de ações e grounding da GUI ficam congelados. Toda adaptação ocorre por meio de uma **biblioteca externa, persistente e versionada de skills**. Cada iteração usa um snapshot congelado da biblioteca; só depois da execução, com trajetória e feedback do evaluator, o sistema decide criar, editar, apagar ou preservar skills. As mudanças ficam visíveis apenas na iteração seguinte.

O proposer recebe histórico estruturado de até cinco iterações e metadados da biblioteca. Ele pode escolher `create_new`, `edit_existing`, `delete_existing`, `no_op` ou `unresolved`. O coordinator valida a proposta, impõe requisitos mínimos de evidência e evita certos tipos de edição excessiva. O builder então gera um `SKILL.md` completo. A proveniência registra criação, retrieval e revisões, o que torna possível auditar a linhagem da skill.

Depois de um warm-up de cinco iterações começando com biblioteca vazia, os ganhos reportados no OSWorld foram: **GIMP 80,2 vs 74,4 (+5,7 pp)**, **VLC 64,0 vs 45,4 (+18,6 pp)**, **Writer 63,9 vs 52,0 (+11,8 pp)** e **Thunderbird 74,7 vs 62,3 (+12,4 pp)**. Em um run de 35 iterações no GIMP, a biblioteca terminou com 27 skills; **82,4%** dos rollouts válidos chamaram `get_skill`, e **43,3%** dessas chamadas recuperaram uma skill criada originalmente por outra tarefa, evidência concreta de reutilização procedural cross-task.

Mas o estudo também mostra que “edit aceita” não significa “skill melhor”. Uma skill chamada `gimp-add-alpha-channel` sofreu revisões repetidas aceitas pelo sistema, enquanto a tarefa que a originou teve sucesso em apenas **2 de 35 tentativas (5,7%)**. O trabalho ainda não possui um mecanismo forte de consolidação automática/rollback baseado em utilidade downstream.

Há outra limitação metodológica: foi executado apenas um run por par condição-domínio, e diferenças já apareciam durante o warm-up em alguns cenários; por isso não se pode atribuir causalmente todo o gap final à biblioteca de skills.

Padrão reutilizável:

`snapshot congelado da biblioteca → execução → evidência estruturada → proposer {criar/editar/apagar/no-op/unresolved} → validator → commit versionado → próxima iteração`.

Para um harness MCP, eu acrescentaria antes do commit um **downstream utility gate + rollback automático**, porque a principal fragilidade observada foi permitir churn persistente em skills sem prova de que a revisão realmente melhorou as tarefas futuras.

Fonte: https://arxiv.org/abs/2609.04869

## Síntese da rodada

O **AutoLR** é o avanço mais importante porque aproxima muito o padrão acadêmico de self-improvement de um pipeline industrial real: o agente produz mudanças que podem alterar persistentemente código, checkpoints e baseline do sistema-alvo, e isso já foi usado em milhares de avaliações e nove Launch Reviews com resultados positivos. Ainda não é RSI forte, porque os agentes pesquisadores permanecem fixos e humanos mantêm autoridade de A/B e rollout.

O **RISE** mostra uma direção distinta: melhoria recursiva dentro do próprio treinamento dos pesos, mas com um algoritmo fixo e fortemente governado por humanos. E o estudo de **computer-use skills** reforça que uma grande parte da evolução útil pode acontecer totalmente fora dos pesos, por meio de procedimentos persistentes e versionados.

A arquitetura que eu usaria hoje para agentes próprios fica mais refinada:

`Trace → Attribution → Candidate → Deterministic Eval → Calibrated Gate → {DISCARD, PACK, KEEP} → Versioning → Downstream/Held-out Check → Promote/Rollback → Evidence Memory`.

A novidade que eu acrescentaria explicitamente depois desta rodada é: **não deixe um único score reescrever o baseline**. Uma promoção persistente deve exigir avaliação calibrada, e uma candidata promissora que ainda não passou nesse nível deve poder ser preservada como `PACK` sem contaminar o trunk.
