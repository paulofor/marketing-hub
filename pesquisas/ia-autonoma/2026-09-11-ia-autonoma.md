# Radar — IA autônoma / agentes que aprendem com experiência

**Data/hora:** 2026-09-11 18:08 BRT (America/Sao_Paulo)

Nesta rodada há **três avanços novos e relevantes**, todos publicados no arXiv em 10/09/2026. O caso mais importante é **Auto-RecSys, da Meta**, porque leva um agente de pesquisa a um ambiente industrial de recomendação com experimentos que duram dias, memória persistente entre servidores, evolução de playbooks e um caso explícito em que o sistema diagnosticou uma limitação do próprio orquestrador, desenhou uma substituição e submeteu uma mudança de código. Os outros dois resultados atacam gargalos que começam a dominar self-improvement: **Ecdysis** tenta evitar que o harness aprenda idiossincrasias de um modelo específico, e **COBRA-Skills** reduz drasticamente o custo de experimentar, selecionar e evoluir skills persistentes.

## Classificação

| Caso | (1) Pesos persistentes | (2) Código/scaffold/harness | (3) Prompts/retrieval/workflows/tools/skills | (4) Só memória/contexto | (5) Forte condução humana |
|---|---:|---:|---:|---:|---:|
| **Auto-RecSys — Meta** | Parcial: checkpoints dos recommenders são treinados, mas os LLMs pesquisadores não se retreinam | **Sim** | **Sim — principal** | Não | Parcial / configurável |
| **Ecdysis** | Não | **Sim — principal** | **Sim** | Não | Parcial, ambiente experimental definido por humanos |
| **COBRA-Skills** | Não | Não | **Sim — principal** | Não | **Sim — design experimental e evaluator definidos por humanos** |

---

## 1. Auto-RecSys — Meta: self-evolution em pesquisa de recommenders industriais

O **Auto-RecSys: Harnessing Autonomous Research Agents for Industry-Scale Recommender Systems** foi submetido em 10/09/2026 por autores da **Meta**. O sistema automatiza o ciclo de pesquisa em recommenders de escala industrial: ideação, implementação, validação, submissão de treinamento, monitoramento, recuperação de falhas e análise de resultados. A motivação é prática: uma única execução de treinamento pode levar dias e consumir centenas de GPU-hours, enquanto experimentos precisam sobreviver a reinícios de servidor, mudanças de ambiente e falhas de infraestrutura.

### O que ele melhora sozinho

O mecanismo possui **dois loops persistentes**:

1. **Execution Evolution Loop** — cada modelo mantém um playbook em linguagem natural. Falhas viram *dead ends*; pipelines que funcionam viram receitas; parâmetros de hardware, package layers, tags de recursos, checkpoints, comandos de validação e sequências de ferramentas passam a orientar execuções posteriores.
2. **Idea Evolution Loop** — o resultado de cada experimento recebe um verdict `positive | neutral | negative`, acompanhado de lições estruturadas. Esse histórico é reutilizado para evitar ideias repetidas, combinar sucessos parciais, abandonar famílias que falham repetidamente e explorar regiões ainda não testadas.

Isso é mais do que memória passiva: o conteúdo persistente **modifica o comportamento operacional das execuções seguintes**.

### O que persiste entre execuções

O sistema mantém estado centralizado acessível por vários servidores: estados por ideia, baseline lifecycle, histórico append-only, backlog, playbooks Markdown, metadata, knowledge base e logs de trajetória. Em vez de deixar o LLM editar tudo livremente, ações sensíveis — transições de estado, APIs, writes e validações — são feitas por scripts determinísticos com schema fixo.

Há inclusive um caso de **categoria (2) real**: durante monitoramento de vários dias, o agente detectou que monitores em background morriam após cerca de 3–5 horas porque cada ciclo acrescentava resultados de tools até estourar a janela de contexto. O sistema **desenhou autonomamente uma solução baseada em cron, implementou-a e submeteu uma mudança de código no orquestrador**, melhorando sua própria infraestrutura, não apenas o recommender pesquisado.

### Métricas observadas

A análise de 31 iterações em um modelo mostrou um ciclo claro de `learn → regress after baseline shift → recover`:

- durante estabilização, **major fixes por iteração caíram de 4,0 para 1,3**;
- após uma mudança grande de baseline, cinco iterações voltaram a exigir correções operacionais;
- depois de absorver as novas lições, as iterações 26–31 ficaram em **0,5 major fix por iteração**, e **5 de 6** completaram sem qualquer correção operacional;
- o playbook acumulou **49 dead ends e 17 padrões erro→correção**;
- numa sessão extrema, o agente executou **970 entradas consecutivas de log / 110 tool calls sem intervenção humana**, diagnosticando falhas, reconstruindo layers e submetendo novamente treinamentos.

Em termos de esforço humano, um ciclo que manualmente consome horas ou dias de atenção ativa cai para **minutos** no modo human-in-the-loop. Os autores afirmam que, com a mesma quantidade de atenção, um pesquisador consegue acompanhar **mais de uma dúzia de ideias** em vez de uma.

### Quanta intervenção humana existe

Há dois modos. No modo interativo, o pesquisador conserva gates em seleção da ideia e review da implementação; o resto é delegado ao agente. No modo autônomo, os checkpoints podem ser pulados e o humano fica restrito a direção de alto nível e escalations. Os autores, porém, recomendam autonomia total principalmente quando o playbook já amadureceu e as ideias são de baixo risco.

### Por que isso importa

Este é um dos casos mais próximos do padrão que estamos procurando porque não é um benchmark curto: é **auto-research em infraestrutura de recommenders de escala industrial, distribuída e multi-dia**, com estado persistente, aprendizado entre sessões e mudanças de workflow baseadas em experiência real.

O padrão reutilizável é:

```text
experiment trace
   ↓
operational attribution
   ↓
{dead end | proven strategy | pipeline recipe}
   ↓
playbook persistente
   ↓
próxima execução
   ↓
resultado científico
   ↓
idea history
   ↓
próxima hipótese
```

Para agentes com MCP, a tradução direta seria manter **dois canais de aprendizagem separados**: um para `como executar` (tool order, schemas, retries, ambiente, recovery) e outro para `o que tentar` (hipóteses, estratégias, combinações já testadas).

Fonte: https://arxiv.org/abs/2609.10922

---

## 2. Ecdysis — corrigir o harness somente quando a falha parece realmente ser do harness

O **Ecdysis: Efficient and Effective Training of Runtime Harnesses for LLM Agents**, também de 10/09/2026, parte de um problema que ficou muito claro nas rodadas anteriores: quando um agente falha, nem toda falha deve virar uma mudança no harness. Às vezes o erro é uma limitação ou idiossincrasia do próprio modelo. Se o Evolver modifica o harness para cada erro individual, ele pode criar regras que funcionam apenas para aquele backbone e prejudicam a generalização.

### O que ele melhora sozinho

Em cada rodada, Ecdysis executa um conjunto de tarefas, agrega falhas **entre várias instâncias**, agrupa padrões recorrentes e usa múltiplos papéis (`Analyst`, `Critic`, `Engineer`, com um `Moderator`) para produzir uma especificação de mudança. Um coding agent altera o harness e a candidata só substitui a versão persistente se melhorar o score de treinamento.

Os **pesos do task model e o runtime environment permanecem fixos por design**. O que muda persistentemente é o harness executável.

### Métricas

Em cinco task models e três datasets, a acurácia média passou de **58,67% no baseline Self-Evolution para 69,56% com Ecdysis + FDCR**, ganho relativo de **18,56%**. Num exemplo com Qwen3-8B em τ²-Airline, a acurácia subiu de **35% para 60%**, Pass@3 de 50% para 80% e Pass³ de 20% para 40%.

O ganho também transferiu para modelos que não haviam sido usados para evoluir o harness: em Qwen3-32B no mesmo subset, o resultado foi de **51,67% para 68,33%**. O treinamento do harness ficou até **1,84× mais rápido** em τ²-Airline. Os autores também mostram desempenho próximo ao full-data usando apenas **25% dos dados de treinamento**.

Uma análise manual quantificou o risco que o método tenta corrigir: o chamado *model-accommodation ratio* caiu de **60,0% no Self-Evolution para 45,5% no Ecdysis**, sugerindo menos adaptações excessivamente específicas ao modelo usado durante a evolução.

### Intervenção humana

A evolução em si pode rodar automaticamente: coleta de traces, agrupamento, diagnóstico multi-role, edição e aceite/rejeição da candidata fazem parte do pipeline. Porém humanos definem benchmark, evaluator, task set, ambiente, base harness e limites do experimento. É evidência experimental, não deployment contínuo em produção.

### Padrão arquitetural reutilizável

O novo detalhe que eu incorporaria é um **gate anterior ao Evolver**:

```text
falha
  ↓
é recorrente entre tarefas/modelos?
  ├─ não → provável limitação local / não aprender ainda
  └─ sim → provável deficiência do harness
             ↓
         gerar patch
             ↓
         validar
             ↓
         persistir
```

Isso é particularmente importante em MCP: se uma tool falhou uma vez porque o modelo montou um argumento ruim, pode ser incorreto alterar permanentemente o schema, descrição ou policy. A mudança deveria ocorrer apenas quando existe **evidência transversal de uma deficiência sistemática**.

Fonte: https://arxiv.org/abs/2609.11677

---

## 3. COBRA-Skills — tratar evolução de skills como problema de exploração/exploração sob orçamento

O **COBRA-Skills: Contextual Bandit-Guided Evolution for Agent Skill Optimization**, submetido em 10/09/2026, ataca outro gargalo: testar cada skill candidata executando o agente é caro. O framework mantém uma **população evolutiva de skills textuais** e usa um contextual bandit para decidir qual candidata merece o próximo orçamento de avaliação.

### O que melhora sozinho

A cada rodada, um predictor estima a utilidade de cada skill e um bônus LinearUCB favorece regiões ainda pouco exploradas. O candidato selecionado é executado pelo agente; reward e trajectories atualizam o histórico. Periodicamente, a população evolui por três operadores:

- **regeneration** — cria uma estratégia nova a partir dos traces sem skill;
- **rollout mutation** — revisa uma skill usando seus sucessos e falhas;
- **crossover** — usa uma skill forte como backbone, outras fortes como evidência positiva e skills fracas como evidência negativa.

Skills de baixa prioridade são removidas. Skills novas **não herdam score**: precisam provar valor novamente no mesmo loop de avaliação.

O target model fica congelado; portanto é **categoria (3)** pura.

### Métricas

Em seis benchmarks e três modelos-alvo, COBRA-Skills teve o melhor score médio entre os métodos comparados. Em relação ao agente sem skill, os ganhos médios foram:

- **+13,1 pontos** no Qwen3.6-35B-A3B;
- **+26,9 pontos** no GPT-5.4-Nano;
- **+22,5 pontos** no Gemma-4-26B-A4B-it.

Comparado ao SkillOpt, reduziu o **custo total de otimização em 55–58%** e o custo por ponto de melhoria em **60–69%**, usando apenas **50 exemplos de otimização por benchmark**. No Qwen3.6, o score médio foi **73,5**, contra 69,6 do SkillOpt e 60,4 do baseline sem skill.

O método também generalizou para harnesses diferentes: **68,2 em Claude Code e 72,4 em Codex**. Em self-teaching, trocando o teacher GPT-5.5 pelo próprio Qwen alvo, o score caiu pouco, de **73,5 para 72,5**, enquanto o custo caiu aproximadamente pela metade. Em transferência entre modelos, **34 de 36 combinações** melhoraram o backbone receptor.

### Intervenção humana

Humano ainda define conjunto de dados, evaluator, orçamento, hyperparameters e espaço do experimento. A geração, priorização, mutação, pruning e seleção de candidates é automática durante a execução. Não há evidência de produção online nem alteração de pesos.

### Padrão arquitetural reutilizável

O principal insight não é apenas “evoluir skills”; é **não gastar o mesmo número de testes em todas as candidatas**:

```text
candidate skills
   ↓
reward predictor + uncertainty
   ↓
prioridade = provável valor + valor informativo
   ↓
executar somente a candidata prioritária
   ↓
trace + reward
   ↓
mutate / regenerate / crossover / prune
```

Para um sistema próprio, isso pode ser aplicado a skills, prompts, descrições de tools MCP, retrieval policies ou versões de workflow. O **Evaluator passa a administrar o orçamento de experimentação**, e não só decidir winner/loser depois que tudo já foi executado.

Fonte: https://arxiv.org/abs/2609.11682

---

## Conclusão desta rodada

O avanço mais importante é **Auto-RecSys**, porque mostra um desenho muito próximo de uma organização de agentes que acumula experiência operacional e científica em ambiente industrial. Ele também reforça uma tendência recorrente: grande parte da melhoria persistente está acontecendo **fora dos pesos**, em playbooks, workflows, infraestrutura e regras de execução.

Ecdysis e COBRA-Skills adicionam duas peças que eu consideraria essenciais num harness próprio:

1. **evidence gate antes de aprender** — uma falha isolada não deveria autorizar mudança persistente;
2. **budgeted evolution** — não avaliar toda variante igualmente; investir mais experimentos onde há maior combinação de potencial e incerteza.

Com isso, o pipeline de autoaperfeiçoamento fica mais maduro:

```text
Execution Trace
    ↓
Cross-Instance Attribution
    ↓
Candidate Scope
    ↓
Candidate Population
    ↓
Bandit / Budget Allocator
    ↓
Replay / Evaluation
    ↓
Integrity + Held-out Gates
    ↓
{discard | archive | promote}
    ↓
Playbook / Harness / Skill versioned state
```

Ainda **não apareceu hoje um caso convincente de recursive self-improvement forte** no qual a versão melhorada do agente assuma automaticamente o papel do Evolver que produz sua própria sucessora. O novo paper **The Last AI Built by Humans: Toward Genuine Recursive Self-Improvement** (arXiv:2609.11873, 10/09/2026) é uma proposta/roadmap e revisão de práticas, não nova evidência experimental de RSI completo, portanto não foi promovido à lista principal desta rodada.

## Fontes públicas

- Auto-RecSys — https://arxiv.org/abs/2609.10922
- Ecdysis — https://arxiv.org/abs/2609.11677
- COBRA-Skills — https://arxiv.org/abs/2609.11682
- The Last AI Built by Humans — https://arxiv.org/abs/2609.11873
