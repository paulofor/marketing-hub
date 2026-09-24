# Radar diário — agentes mais inteligentes | 24/09/2026

A leva de **23 de setembro** trouxe três trabalhos novos particularmente úteis para o objetivo deste radar: memória curada no momento de uso, controle adaptativo de contexto antes de cada ação e condicionamento explícito ao estado vivo do sistema. Além deles, dois preprints recentes sobre evolução do harness merecem entrar agora porque atacam diretamente um risco que cresce conforme o AI Hub passa a aprender com as próprias trajetórias: **melhorar no caso que acabou de falhar e, ao mesmo tempo, piorar silenciosamente em outros casos**.

O padrão mais forte da rodada é: **não resumir toda experiência cedo demais, não deixar o estado importante depender apenas do prompt e não permitir que uma melhoria local do harness se torne permanente sem evidência de transferência e regressão.**

## 1. Just-in-Time Memory — guardar a trajetória bruta e decidir o que importa quando a próxima tarefa chegar

**Tipo:** pesquisa acadêmica / preprint, submetido em 23/09/2026.

**Just-in-Time Memory: Learning to Curate Task-Adaptive Memory for LLM Agents** questiona o padrão mais comum de memória agentic: ao terminar uma tarefa, o sistema imediatamente resume a trajetória em uma reflexão, workflow ou skill fixa e depois tenta reutilizar esse resumo por similaridade.

O problema é que, no momento da escrita, ainda não sabemos qual detalhe será útil para uma tarefa futura. Uma compactação precoce pode destruir exatamente a informação que uma tarefa posterior precisaria.

O JitMem mantém as trajetórias brutas e transfere a curadoria para **read time**. Quando a nova tarefa chega, recupera experiências candidatas e um curador sintetiza um payload curto especificamente para aquela necessidade. Como o payload é usado imediatamente, seu valor pode ser aprendido a partir do resultado da própria tarefa atual, sem esperar muitas interações futuras para saber se a memória gravada foi boa.

Nos benchmarks ALFWorld, WebShop e τ²-bench, o método superou o melhor baseline de memória de write-time por **16,2, 16,3 e 3,9 pontos absolutos de success rate**, respectivamente. Um resultado importante é que até o curador não treinado já foi competitivo ou superior aos baselines em vários cenários.

**Aplicação ao AI Hub:** eu mudaria a estratégia de memória para separar armazenamento de curadoria:

```text
TASK TRAJECTORY
      ↓
raw durable trace
      ↓
index + provenance
      ↓
      ───────────── future task ─────────────
      ↓
retrieve candidate trajectories
      ↓
JIT MEMORY CURATOR
      ↓
task-specific compact payload
      ↓
agent context
```

Isso evita transformar cedo demais uma trajetória rica em um resumo permanente. Para o AI Hub, eu preservaria no storage pelo menos eventos estruturados, evidências, decisões e resultados; `skills` ou resumos continuariam existindo, mas seriam uma camada derivada e recriável.

**Limitação:** armazenar trajetórias brutas aumenta custo de storage e retrieval. Além disso, os resultados são em ambientes agentic benchmarks, não diretamente em coding agents com grandes repositórios.

Fonte: https://arxiv.org/abs/2609.27334

## 2. State-Grounded Conditioning — o estado vivo deve controlar o agente por fora do prompt

**Tipo:** pesquisa acadêmica / preprint, submetido em 23/09/2026, sob revisão para EACL 2027 Industry Track.

**State-Grounded Conditioning: Wrapping User-Facing LLM Agents Where Direction Depends on Live State** define uma falha chamada **direction drift**: a resposta pode ser coerente e aparentemente completar a tarefa, mas seguir uma direção incompatível com o estado atual do usuário/sistema.

O SGC externaliza esse controle em wrappers estruturados de **Perception, Grounding e Interaction**, com dependências explícitas sobre slices do estado atual, em vez de depender apenas de texto colocado no prompt.

Em aproximadamente 1.000 turnos de 200 sessões, habilitar os três wrappers elevou a grounded accuracy por turno para **96,7%**, contra 61,1% com prompting e 69,8% com um baseline de agent/tool harness. A grounded accuracy no nível da sessão passou de 20,0%/26,5% para **83,5%**, e incidentes de grounding failure caíram cerca de **78%** em relação ao baseline mais forte.

**Aplicação ao AI Hub:** isso reforça que `Task State` e `Belief State` não devem existir somente como Markdown ou mensagem reenviada ao modelo. Eu criaria slices explícitos:

```yaml
state_slices:
  task:
    completed: [...]
    blocked: [...]
    pending: [...]

  evidence:
    verified: [...]
    missing: [...]
    stale: [...]

  interaction:
    user_constraints: [...]
    approvals: [...]
    unresolved_choices: [...]
```

Antes de cada decisão importante, o harness monta somente os slices relevantes e pode bloquear uma direção incompatível com eles.

Exemplo: se `preserve_existing_login=true` já foi confirmado, o planner não deveria conseguir seguir silenciosamente para uma estratégia que substitua a autenticação existente.

**Limitação:** a avaliação é num agente de coaching em jogo, não em engenharia de software. O ganho quantitativo não pode ser projetado diretamente para o AI Hub; o princípio de externalizar estado, porém, é bastante transferível.

Fonte: https://arxiv.org/abs/2609.27606

## 3. PaMER — o harness pode decidir recuperar ou comprimir contexto antes de cada ação

**Tipo:** pesquisa acadêmica / preprint, submetido em 23/09/2026.

**Memory Control Signals Emerge Before Action in Long Horizon Agents** investiga se o próprio modelo já contém, antes de agir, sinais de que precisa recuperar memória ou compactar contexto. Os autores encontram esses sinais nos hidden states e mostram que eles não são explicados apenas pelo tamanho do contexto ou pela posição na trajetória.

O trabalho propõe **PaMER (Preaction Memory with Evidence Retrieval)**, combinando compressão guiada pelo estado com retrieval de evidência histórica; PaMER+ adiciona seleção de evidência em nível de passo, trazendo apenas a parte histórica necessária para a ação atual. No WorkBuddyBench, o método reduz substancialmente o consumo de contexto mantendo desempenho competitivo.

**Aplicação ao AI Hub:** com APIs fechadas como Sol/Astra, não temos acesso confiável aos hidden states; portanto eu não copiaria o detector do paper. Copiaria o princípio de **memory decision before action**:

```text
before each costly/important action
          ↓
MEMORY NEED GATE
          ↓
 ┌────────┼─────────┐
 │        │         │
keep   retrieve   compact
recent  evidence   history
 │        │         │
 └────────┼─────────┘
          ↓
       ACTION
```

Podemos aproximar esse gate com sinais observáveis: número de unknowns, referências a decisões antigas, mudança de subgoal, repeated replans, conflitos de evidência, tamanho do contexto e risco da próxima ação.

**Limitação:** a parte científica mais nova do paper depende de estados internos do modelo. Em modelos via API, nossa implementação seria uma aproximação no harness e precisaria de avaliação própria.

Fonte: https://arxiv.org/abs/2609.27286

## 4. Self-Healing Harness — uma correção que resolve o erro atual frequentemente quebra algo que já funcionava

**Tipo:** pesquisa acadêmica / preprint, submetido em 21/09/2026; especialmente útil e ainda não destacado neste radar.

**Self-Healing Harness for Runtime Oversight of Agent Self-Modification** trata self-improvement como **admission control**. O agente pode propor regras que alteram seu comportamento futuro, mas elas primeiro vivem num workspace provisório. Só ganham autoridade persistente depois de demonstrar melhoria no caso que motivou a alteração e não regredir além de um limite em casos protegidos.

O ciclo é:

```text
DETECT
  ↓
NOTICE
  ↓
HEAL (candidate rule)
  ↓
VALIDATE
  ↓
provisional → persistent or reject
```

A evidência é muito relevante: em 16 pares de execuções sobre AppWorld, Terminal-Bench e τ²-bench, o gate rejeitou **383 propostas** decididas por replay. Dessas, **211 (55%)** corrigiam a falha que as originou, mas degradavam um caso que antes funcionava.

Isso é uma evidência direta de por que o AI Hub não deveria permitir:

```text
failure
  ↓
LLM writes new rule
  ↓
rule becomes permanent
```

Eu faria:

```text
failure
  ↓
candidate policy/skill
  ↓
matched replay
  ↓
protected regression set
  ↓
corpus-level guard
  ↓
promote / reject / rollback
```

**Limitação:** a escala experimental ainda é pequena e apenas dois intervalos bootstrap de task-completion excluem zero, embora reliability melhore ou empate em todos os pares. Ainda assim, o dado dos 55% de regressões colaterais é um alerta arquitetural forte.

Fonte: https://arxiv.org/abs/2609.24130

## 5. RRSI — self-improvement do harness também precisa de regularização

**Tipo:** pesquisa acadêmica / preprint de Google Cloud AI Research + colaboradores, submetido em 21/09/2026; especialmente útil e ainda não destacado neste radar.

**RRSI: Regularized Recursive Self-Improvement of Agent Harnesses** parte do problema de que evolução automática do harness pode simplesmente **overfit ao conjunto usado para evoluí-lo**. O score local sobe, mas a melhoria desaparece — ou vira regressão — em tarefas não vistas.

RRSI mantém todo o harness editável — prompts, control flow, tools, skills, memory, context management e subagents — mas regulariza o caminho de evolução. Entre os mecanismos estão:

- orçamento de edits que diminui ao longo das rodadas;
- histórico explícito de hipóteses aceitas e rejeitadas;
- exploração forçada de componentes pouco testados quando a evolução estagna;
- critic para detectar leakage/task-specific hacks;
- acceptance que considera ruído e custo;
- pruning de mecanismos pequenos, caros ou que deixaram de contribuir.

Em oito benchmarks de coding, workspace agentic e engineering design, RRSI ganhou até **14,1 pontos** no conjunto usado para evolução e até **4,7 pontos** em benchmarks out-of-distribution, além de usar **30% menos policy tokens** que a evolução não regularizada.

**Aplicação ao AI Hub:** o `Harness Evolution` deveria ter um ledger próprio:

```yaml
harness_experiment:
  hypothesis: "retrieve evidence before auth-plan"
  component: requirement-discovery-router
  diff: ...
  evolve_delta: +0.08
  heldout_delta: +0.03
  token_delta: -0.11
  leakage_check: PASS
  decision: PROMOTE
```

Hipóteses rejeitadas também são conhecimento: não deveriam ser reexperimentadas indefinidamente sem nova evidência.

E eu limitaria o número de mudanças simultâneas por rodada para preservar **atribuição causal**. Se uma versão altera prompt, memory router, verifier e tool surface ao mesmo tempo, mesmo uma melhoria real se torna difícil de explicar.

**Limitação:** ainda é evolução sobre conjuntos de benchmarks relativamente pequenos e finitos. A própria contribuição do paper existe justamente porque esse regime é vulnerável a overfitting; transferência para workflows privados do AI Hub precisa de held-out próprio.

Fonte: https://arxiv.org/abs/2609.24972

## 6. V7 / OpenAI — evidência de engenharia: graph + source links + RAG fallback + MCP

**Tipo:** post técnico/estudo de caso de engenharia publicado pela OpenAI em 21/09/2026; não é paper acadêmico independente.

O case **How V7 gives AI agents institutional memory** descreve uma arquitetura que organiza milhões de arquivos em um **Context Graph** de entidades, relações, fatos e métricas, preservando links/citações para o documento original. O agente consulta esse grafo diretamente e usa RAG sobre os documentos subjacentes quando o grafo não tem informação suficiente. A mesma memória é exposta por **MCP**, e agentes long-running mantêm trocas recentes no contexto ativo e material mais antigo no grafo.

No benchmark HERB reportado pela V7, o sistema de retrieval superou o baseline oficial em **69%** e reduziu hallucinations em queries sem resposta em **38%**. O post também relata que, no harness mais recente, alguns workflows com múltiplas chamadas externas ficaram até **50% mais rápidos**. Como são números reportados pela própria empresa num case da OpenAI, eu os trataria como evidência de engenharia, não como validação científica independente.

O ponto mais aplicável ao AI Hub é a combinação:

```text
structured context graph
       ↓
source-linked facts
       ↓
MCP query surface
       ↓
RAG fallback to originals
       ↓
workflow / agent
```

E existe uma direção futura interessante: V7 está trabalhando para disparar workflows quando fatos do graph mudam e sinalizar análises que ficaram dependentes de informação antiga.

Para o AI Hub, isso sugere um `Staleness/Dependency Graph`:

```text
fact F changed
    ↓
which beliefs depended on F?
    ↓
which plans / reports / memories depended on those beliefs?
    ↓
mark STALE
    ↓
revalidate on next use
```

Isso vai além de RAG: a memória passa a saber **quais conclusões precisam ser revistas quando a fonte muda**.

**Limitação:** é material de produto/case study, com métricas produzidas pela V7; não substitui avaliação controlada.

Fonte: https://openai.com/index/v7/

## Resultado adicional útil — provenance melhora retrieval quando a evidência está espalhada

**When Does Execution Provenance Help Agent Memory Retrieval?**, submetido em 22/09, avalia **2.000 queries** sobre **1.207 trajetórias**. Em vez de chunking fixo, cria unidades alinhadas aos argumentos e outputs de tools e conecta-as por relações de provenance. As unidades alinhadas melhoram `Full Support@2048` em **19,07 pontos** sobre janelas planas de 512 tokens; mantendo candidatos e scores iguais, a propagação no grafo adiciona mais **4,55 pontos**, concentrados nos casos em que a evidência necessária atravessa múltiplos eventos.

Para o AI Hub, o takeaway é simples: **não chunkar trace apenas por tamanho de token**. A unidade natural de memória pode ser um evento causal verificável:

```text
tool request → tool result → derived belief → dependent action
```

Fonte: https://arxiv.org/abs/2609.25913

## Mudança arquitetural que eu faria hoje

A rodada sugere uma arquitetura de memória em três camadas:

```text
                    RAW EXPERIENCE
              trajectories + tool events
                         │
                 immutable provenance
                         │
                         ▼
                 RETRIEVAL / INDEX LAYER
          state + provenance + similarity
                         │
                         ▼
                  JIT MEMORY CURATOR
             task-specific compact payload
                         │
                         ▼
                    ACTIVE CONTEXT
                         │
                 before each action
                         │
                         ▼
                  MEMORY NEED GATE
              keep / retrieve / compact
                         │
                         ▼
                       AGENT
```

E a evolução do harness ficaria separada:

```text
trajectory failure
      ↓
candidate harness change
      ↓
PROVISIONAL AUTHORITY
      ↓
matched replay
      ↓
protected regressions
      ↓
OOD / held-out evaluation
      ↓
complexity + cost check
      ↓
PROMOTE / REJECT / ROLLBACK
```

Minha prioridade prática depois desta rodada seria:

1. **Persistir traces brutos e estruturados**, evitando resumir irreversivelmente a experiência na gravação.
2. Criar um **JIT Memory Curator** que monte memória especificamente para a tarefa/decisão atual.
3. Colocar **Task/Belief State fora do prompt** como estado estruturado, usando wrappers/gates para decisões de alto impacto.
4. Criar um **Memory Need Gate** antes de ações importantes: manter contexto recente, recuperar evidência ou compactar.
5. Toda mudança auto-gerada do harness entra primeiro como **provisória**, com replay e regressão antes de ganhar autoridade persistente.
6. Regularizar evolução: **poucas mudanças por rodada, ledger de hipóteses, held-out real, custo e pruning**.

A conclusão principal de hoje é que o AI Hub não deveria escolher entre **“memória resumida” ou “trajetória bruta”**. O desenho mais promissor é guardar a experiência rica e verificável como fonte durável e criar **memória compacta just-in-time**, orientada pelo estado e pela decisão que está sendo tomada naquele momento.

Isso também resolve parte da discussão recente sobre arquivo versus banco: a experiência durável pode ficar em MySQL + artifacts/logs, enquanto o contexto entregue ao modelo é uma visão transitória produzida pelo curador. **O resumo não precisa ser a verdade armazenada; pode ser uma projeção descartável da verdade armazenada.**

## Fontes

- Just-in-Time Memory: https://arxiv.org/abs/2609.27334
- State-Grounded Conditioning: https://arxiv.org/abs/2609.27606
- Memory Control Signals / PaMER: https://arxiv.org/abs/2609.27286
- Self-Healing Harness: https://arxiv.org/abs/2609.24130
- RRSI: https://arxiv.org/abs/2609.24972
- Execution Provenance and Memory Retrieval: https://arxiv.org/abs/2609.25913
- OpenAI / V7 institutional memory: https://openai.com/index/v7/
