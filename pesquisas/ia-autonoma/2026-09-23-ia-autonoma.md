# IA autônoma — rodada de 2026-09-23 17:42 (America/Sao_Paulo)

Nesta rodada há **três avanços relevantes** que passam o filtro: **AIDE²**, **Harness-Zero** e **MiMo-V2.6**. O mais importante é AIDE², porque o relatório técnico completo publicado em 22/09 documenta um loop autônomo de oito dias em que um agente de pesquisa reescreve o código de outro agente de pesquisa, avalia as versões sob orçamento fixo, preserva apenas as melhores e usa cada versão aceita como ponto de partida da próxima rodada. Harness-Zero mostra um mecanismo complementar: **comportamentos descobertos no harness podem ser consolidados nos pesos do modelo e sobreviver à remoção do harness especializado**. MiMo-V2.6 é um caso forte de **mudança persistente de pesos via agentic RL em larga escala**, mas continua sendo otimização essencialmente conduzida por humanos, não recursive self-improvement autônoma.

## Classificação

| Caso | (1) Pesos | (2) Código/scaffold/harness | (3) Prompts/retrieval/workflows/tools/skills/estratégias | (4) Só memória/contexto | (5) Forte condução humana |
|---|---:|---:|---:|---:|---:|
| **AIDE²** | Não | **Sim — principal** | **Sim** | Não | Parcial |
| **Harness-Zero** | **Sim — principal** | Harness é fonte/teacher, não o alvo final | Comportamentos do harness são internalizados | Não | **Sim** |
| **MiMo-V2.6** | **Sim — principal** | Harnesses são ambientes de treino | Indiretamente, via trajetórias agentic RL | Não | **Sim — principal** |

## 1. AIDE² — o caso mais forte da rodada

O paper **Recursive self-improvement of AI research agents**, submetido ao arXiv em **22 de setembro de 2026**, formaliza o sistema AIDE² da Weco AI. O experimento havia sido divulgado em julho, mas o relatório técnico completo agora permite analisar com mais rigor o mecanismo, as métricas, os gates e as limitações.

A arquitetura possui dois loops:

```text
OUTER LOOP
AIDE_human
   ↓
reescreve o código do agente de pesquisa interno
   ↓
Candidate AIDE_k
   ↓

INNER LOOP
Candidate executa tarefas reais de AI R&D
   ↓
private evaluation + fixed cost budget
   ↓
PROMOTE ou REJECT
   ↓
se promovido, Candidate vira o ancestral da próxima rodada
```

O que muda persistentemente é **o código do próprio research agent**: política de busca, controle de contexto, verificação e mecanismos contra reward hacking. Os pesos do modelo permanecem fixos durante esse processo. Cada melhoria aceita é usada como base para a seguinte, portanto há de fato uma cadeia persistente de versões do harness/agente.

A execução ficou **100 outer-loop steps / 8 dias sem intervenção humana**, com cerca de **90% das propostas rejeitadas** e **sete versões sucessivamente aceitas**. O agente final descobriu, entre outras coisas:

- uma nova política de busca baseada em lineages tratadas como braços de um multi-armed bandit;
- fork automático quando uma lineage entra em plateau;
- contexto específico por operador, em vez de concatenar todo o histórico;
- compressão média de prompt de aproximadamente **16×**;
- mecanismos explícitos contra reward hacking.

A validação é especialmente importante. Cada tarefa possui score público e **score privado invisível ao inner agent**. A sobrevivência da candidata depende do score privado e de um orçamento físico fixo de custo; portanto uma candidata não pode vencer simplesmente gastando mais tokens, mais paralelismo ou mais chamadas de modelo.

Em MLE-Bench Lite, os ganhos pareados contra AIDE₀ foram **+0,053 para AIDE₄₇ (p=0,0024)** e **+0,042 para AIDE₈₅ (p=0,0041)**. Os melhores agentes também generalizaram para ALE-Bench Lite e para **WeatherBench 2**, que fica fora da distribuição das tarefas usadas no loop de evolução. No relatório técnico, o melhor agente iguala ou supera um agente de produção ajustado manualmente em quatro avaliações held-out.

Outro efeito interessante apareceu sem ser objetivo explícito: numa família held-out de tarefas, a taxa de reward hacking caiu de **55% para 32%**, ficando sete pontos percentuais abaixo do agente human-engineered usado como referência.

### Intervenção humana

Alta na concepção inicial, mas **zero dentro dos 100 passos do experimento**. Humanos definiram o outer agent inicial, benchmarks, orçamento, protocolos e private evaluations. AIDE² então executou o loop sozinho durante oito dias.

### Limitação decisiva

Ainda **não é RSI forte/ignition**. Os autores testaram AIDE₄₇ como outer-loop improver. Ele chegou ao mesmo teto em cerca de 20 passos, enquanto AIDE_human levou cerca de 40, mas a diferença de eficiência **não foi estatisticamente significativa** e o agente descoberto não apresentou desempenho assintoticamente superior. Portanto o sistema melhorou o **research agent**, mas não há evidência convincente de que tenha melhorado recursivamente a própria capacidade de produzir o próximo improver.

### Padrão arquitetural reutilizável

```text
Incumbent Agent
      ↓
Evolver propõe patch no próprio harness
      ↓
Candidate
      ↓
public eval para orientar execução
+
private eval invisível ao Candidate
+
fixed cost budget
      ↓
REJECT | PROMOTE
      ↓
Candidate promovido vira novo Incumbent
```

Para agentes próprios, a parte mais importante é: **o conjunto que decide promoção não deve ser visível para o agente que está sendo otimizado**.

Fontes:
- Paper: https://arxiv.org/abs/2609.26457
- Weco AI: https://www.weco.ai/blog/first-evidence-of-recursive-self-improvement

## 2. Harness-Zero — transformar aprendizado do harness em pesos

**Harness-Zero: Harness Distillation via Agent-as-Harness**, submetido em 21 de setembro, resolve outro problema: uma skill ou harness especializado pode funcionar muito bem, mas o ganho desaparece quando aquele harness não está presente no deployment.

O método cria uma ponte entre os dois níveis:

```text
harness especializado/evoluído
        ↓
harnessing agent observa a proposta do student
        ↓
PASS ou REPLACE
        ↓
trajetória expressa somente no action-space do student
        ↓
SFT
        ↓
modelo aprende o comportamento
        ↓
harness especializado pode ser removido
```

A grande diferença é que o teacher não entrega apenas a resposta final. Ele corrige o student **no action space que o student realmente terá em produção**. Isso torna as trajetórias utilizáveis para fine-tuning e permite internalizar procedimentos antes fornecidos externamente.

Com o harness especializado removido no deployment, o macro-average task success do modelo base passou de **23,3% para 44,3%**, superando inclusive os **41,7%** obtidos pelo base model com o harness especializado ainda conectado. O estudo também identifica 28 comportamentos exclusivos do harness e reporta **82,3% de recuperação média** desses comportamentos depois da distilação.

A ablação em USPTO é importante: simplesmente treinar em trajetórias de um modelo mais forte não produziu o mesmo efeito. O sinal útil vinha das **correções contextualizadas no estado atual do student e expressas na interface que ele realmente utiliza**.

### Classificação

Principalmente **(1)+(5)**. Há mudança persistente nos pesos, mas ela ocorre por SFT dentro de um pipeline desenhado por humanos. Não é um modelo que espontaneamente decide consolidar suas skills nos próprios pesos.

### Por que isso importa para a arquitetura que estamos acompanhando

Isso cria uma arquitetura de duas velocidades muito interessante:

```text
FAST LOOP
skills / prompts / routing / workflows
mudam frequentemente fora dos pesos
        ↓
acumulam evidência
        ↓

SLOW LOOP
comportamentos maduros e recorrentes
        ↓
distillation / fine-tuning
        ↓
viram capacidade do modelo
```

No seu caso, isso também reforça que **skills podem ficar num store externo sem gerar imagem Docker nova**. Só quando uma habilidade amadurecer suficientemente seria necessário consolidá-la no modelo ou no código estático.

Fonte:
- https://arxiv.org/abs/2609.24974

## 3. MiMo-V2.6 — self-improvement nos pesos, mas human-led

A Xiaomi publicou em **22 de setembro** o MiMo-V2.6 e abriu pesos, ambientes e recursos de treinamento. O relatório chama explicitamente o processo de caminho para self-improvement, mas a classificação correta para este radar é **(1)+(5)**: os pesos mudam persistentemente através de reinforcement learning, porém o pipeline, objetivos, ambientes, rewards e decisão de treinamento são construídos e operados por humanos.

O RL foi escalado em três frentes:

- **1.568 prompts por training step**;
- **2,7–3,7 bilhões de tokens por step**, com contextos de até 1M;
- ambientes heterogêneos de code, general workflows, visual e cyber, usando múltiplos harnesses;
- grader adicional que compara trajetórias do mesmo grupo em vez de tratar todo `pass` como igualmente bom.

O mecanismo **Groupwise Advantage Redistribution (GAR)** é a parte mais reutilizável: entre várias trajetórias que passaram o verifier, um grader compara precisão, minimalidade, aderência à tarefa e qualidade de implementação e redistribui o sinal positivo para as melhores. Isso tenta impedir que RL aprenda comportamentos como patches excessivamente defensivos ou soluções que exploram o evaluator.

No DeepSWE v1.1, o Pro evoluiu de **58,4 para 72,6 average@3**, e o Flash de **48,7 para 65,7** durante o RL. A Xiaomi também abriu um checkpoint menor, MiMo-V2.6-Distill-Qwen-9B, ambientes e pipeline; no exemplo reproduzível divulgado, SWE-bench Verified passou de **61,1 para 66,2**, e o mini-benchmark cyber de **31,3 para 47,0**.

### Limitação

Isto **não é recursive self-improvement autônoma**. O modelo aprende de suas próprias trajetórias e altera seus pesos, mas não decide sozinho qual algoritmo de RL usar, quais ambientes criar, qual reward adotar, quando retreinar ou qual checkpoint promover.

### Padrão arquitetural reutilizável

```text
N trajectories da mesma tarefa
        ↓
verifier elimina falhas
        ↓
quality grader compara sucessos
        ↓
reward redistribuído
        ↓
policy update
```

O ponto importante é que **resultado binário não basta**. Em loops de autoaperfeiçoamento, duas candidatas podem ambas passar e ainda assim uma ser muito pior como comportamento futuro.

Fontes:
- Xiaomi oficial: https://mimo.mi.com/docs/en-US/news/latest/v2-6
- Technical report / overview: https://www.alphaxiv.org/abs/2609.mimo-scaling-reinforcement-learning

## Um não-caso útil: robô que ficou 2× mais rápido, mas não aprendeu persistentemente

O paper **Generalizing Manipulation Skills with a Local Coding Agent**, de 22/09, reporta que um robô controlado por um coding agent ficou aproximadamente **2× mais rápido** ao repetir tarefas já resolvidas e reduziu tool calls pela metade. À primeira vista isso parece self-improvement.

Mas os próprios autores deixam claro que o segundo passe **retoma a mesma sessão**, preservando histórico da conversa, medições e scripts gerados. Eles afirmam que o resultado deve ser interpretado como **in-session reuse, não persistent learning**.

Portanto este caso entra em **categoria (4)**, e não em (2) ou (3). É um ótimo exemplo do motivo pelo qual o radar precisa separar “ficou melhor porque o contexto ainda estava disponível” de “mudou algo persistente que altera execuções futuras independentes”.

Fonte:
- https://arxiv.org/abs/2609.26499

## Síntese da rodada

O avanço mais importante é **AIDE²**, porque ele chega mais perto do que temos chamado de loop genuíno de melhoria persistente de um agente:

```text
agent version N
   ↓
modifica o código do research agent
   ↓
avalia sob orçamento fixo + private eval
   ↓
promove apenas melhoria comprovada
   ↓
agent version N+1
   ↓
repete
```

Porém a fronteira continua clara: o **outer improver ainda é essencialmente fixo**. O teste de ignition não demonstrou de forma estatisticamente convincente que o agente melhorado se tornou também um improver superior. Portanto ainda não temos evidência forte de uma sequência do tipo:

```text
AI₁ melhora AI₂
AI₂ ficou melhor em melhorar AIs
AI₂ produz AI₃ ainda melhor nisso
AI₃ acelera novamente o mecanismo de melhoria
...
```

A rodada também reforça uma arquitetura prática de **duas velocidades**: primeiro aprender rapidamente em harness/skills/routing externos e versionados; depois, quando comportamentos estiverem maduros, usar mecanismos como Harness-Zero para consolidá-los nos pesos. Isso reduz a necessidade de rebuild/deploy a cada aprendizado e mantém mudanças frequentes fora da imagem Docker.
