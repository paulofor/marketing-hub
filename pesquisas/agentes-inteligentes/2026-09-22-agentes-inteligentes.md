# Radar diário — agentes mais inteligentes | 22/09/2026

A rodada de hoje trouxe quatro trabalhos novos, todos submetidos em **21 de setembro de 2026**, e dois deles atacam diretamente a pergunta central deste radar: **quando o agente deve perceber que o pedido está incompleto e como decidir entre inferir, buscar mais contexto ou perguntar ao usuário**.

A principal mudança de arquitetura que eu faria hoje no AI Hub é introduzir um **Ask-or-Infer Gate** antes do planner. A decisão não seria apenas “tenho contexto suficiente?”, mas também: **qual lacuna ainda existe, qual fonte pode resolvê-la e qual é o valor esperado de perguntar ao usuário em vez de continuar inferindo?**

## 1. CIGAsk — aprender quando perguntar e qual pergunta realmente reduz a ambiguidade

**Tipo:** pesquisa acadêmica / preprint; aceito no EMNLP 2026 Findings.

**Paper:** *When and How Should an Agent Clarify? CIGAsk: Teaching LLMs to Clarify via Counterfactual Information Gain*  
https://arxiv.org/abs/2609.24290

O trabalho parte de uma falha exatamente alinhada ao nosso objetivo: LLMs diante de queries subespecificadas frequentemente escolhem uma interpretação e respondem, em vez de perguntar. Prompting simples não resolve bem: alguns modelos passam a perguntar em excesso, enquanto outros fazem perguntas vagas que não recuperam a informação realmente faltante.

CIGAsk separa duas habilidades:

- **quando perguntar**;
- **como formular uma pergunta que recupere a informação discriminante**.

A primeira é treinada com um bônus assimétrico baseado em ambiguidade. A segunda usa **Counterfactual Information Gain (CIG)**: mede quanto a resposta do usuário à pergunta de esclarecimento aumenta a probabilidade da resposta correta em um modelo de referência congelado.

Nos resultados principais, o CIGAsk-7B alcança **F1 0,795 no PACIFIC**, contra 0,481 de ReAct e 0,581 do SFT sem RL. Ele pergunta em **87,3% dos casos realmente ambíguos**, mas em 26,0% dos casos claros. No AmbigNQ, alcança EM 0,511. A ablação mostra papéis diferentes: retirar o CIG reduz a qualidade pós-esclarecimento; retirar o bônus de ambiguidade derruba fortemente a capacidade de escolher quando perguntar.

### Aplicação ao AI Hub

Eu adicionaria uma camada explícita:

```text
USER REQUEST
     │
     ▼
AMBIGUITY / MISSING-CONTEXT DETECTOR
     │
     ├── infer safely
     ├── retrieve evidence
     └── ask user
            │
            ▼
    QUESTION VALUE ESTIMATOR
```

O ponto novo é que uma pergunta ao usuário não deveria ser gerada apenas porque existe incerteza. Ela deveria ser escolhida por algo semelhante a **expected information gain**.

Exemplo:

```yaml
unknown:
  session_authority

candidate_questions:
  - question: "A sessão deve continuar controlada pelo backend?"
    expected_information_gain: 0.82

  - question: "Quer alguma mudança adicional no login?"
    expected_information_gain: 0.17
```

A primeira pergunta resolve uma decisão arquitetural; a segunda é vaga.

### Limitações

O treino assume datasets com resposta correta, label de ambiguidade e um simulador de usuário cooperativo. O paper testa QA, não software engineering. Para o AI Hub, o mecanismo deveria começar como uma **heurística de runtime**, não como RL próprio.

---

## 2. MemCalib — memória relevante não deve ter sempre o mesmo peso

**Tipo:** pesquisa acadêmica / preprint.

**Paper:** *MemCalib: Benchmarking and Optimizing Memory Use in LLM Agents*  
https://arxiv.org/abs/2609.24259

O trabalho mostra um problema que fica cada vez mais importante à medida que adicionamos memória ao harness: o modelo não erra apenas por não recuperar memória; ele também pode **usar memória demais** ou **usar memória de menos**.

O benchmark representa cada proposição de memória com três níveis ideais de influência:

```text
IGNORE  → não deve afetar a resposta
BOUND   → ajuda localmente
CONTROL → deve governar uma decisão ou restrição importante
```

Em 15 mil exemplos envolvendo assistência geral, saúde e coding, os autores mostram que modelos de fronteira frequentemente apresentam uma política global de memória muito agressiva ou muito conservadora, em vez de calibrar cada fato individualmente. No teste, GPT-5.6-SOL teve o melhor resultado global entre os modelos avaliados, mas ainda ficou com **Exact Calibration de 28,4%** em uma das configurações reportadas. O judge usado na avaliação teve **96,7% de concordância** com uma amostra anotada por humano.

### Aplicação ao AI Hub

Eu alteraria o `MemoryCue` que desenhamos para carregar também um **nível de autoridade esperado**:

```yaml
memory_atom:
  statement: "backend controla a sessão"
  use_level: CONTROL
  provenance: architecture-decision-17
```

ou:

```yaml
memory_atom:
  statement: "na tarefa anterior usamos Redis"
  use_level: BOUND
```

ou:

```yaml
memory_atom:
  statement: "o usuário uma vez preferiu estratégia X"
  use_level: IGNORE
```

Isso evita dois tipos de erro simétricos:

```text
memória velha domina a evidência atual  ← over-use

restrição importante é lembrada mas ignorada ← under-use
```

Para o AI Hub, eu chamaria essa camada de **Memory Influence Policy**.

### Limitações

O método MemCalib-RL exige treino específico e usa LLM-as-a-judge para parte da avaliação. A maior contribuição prática para nós hoje é a **representação Ignore / Bound / Control**, não a necessidade de reproduzir o algoritmo de RL.

---

## 3. EvoPathBench — self-improvement precisa ser avaliado durante a evolução, não apenas no final

**Tipo:** pesquisa acadêmica / preprint.

**Paper:** *Beyond Endpoint Performance: Process-Level Evaluation of Self-Evolving Agents*  
https://arxiv.org/abs/2609.24663

Esse trabalho questiona uma prática comum: deixar o agente evoluir memória/skills e comparar apenas a versão inicial com a versão final.

O EvoPathBench congela os artefatos evolutivos em vários checkpoints e mede três capacidades separadamente:

- generalização para tarefas novas;
- retenção após aprender coisas não relacionadas;
- adaptação quando nova evidência contradiz uma regra antiga.

O achado mais importante é que ganhos próximos da distribuição de treino frequentemente desaparecem sob mudança de distribuição. Também existem caminhos em que uma atualização posterior **apaga uma capacidade previamente adquirida**, e nenhum método testado demonstrou adaptação confiável de regras quando aparece evidência contraditória.

Exemplo dos resultados: SkillOpt melhora no cenário próximo (**+0,763**) mas cai abaixo do baseline em transferência (**-0,273**); SkillGrad mostra padrão semelhante (**+0,555 / -0,383**). SkillBoost apresenta ganho positivo nos dois (**+1,597 / +0,537**), mas mesmo assim a revisão de regras continua problemática.

### Aplicação ao AI Hub

Em vez de avaliar somente:

```text
Harness v37 → score final
```

passaríamos a registrar:

```text
v37.0
  ↓ nova skill
v37.1
  ↓ nova regra de memória
v37.2
  ↓ alteração do router
v37.3
```

E em cada checkpoint mediríamos:

```text
Implicit Requirement Recall
Context Gap Detection
Tool Resolution Accuracy
Completion Accuracy
Memory Calibration
Regression Retention
Rule Revision
```

Isso cria um **Capability Timeline**.

```yaml
capability:
  implicit_requirement_recall:
    v37.0: 0.71
    v37.1: 0.79
    v37.2: 0.78
    v37.3: 0.64   # regression detectada
```

O harness evoluidor então não poderia promover `v37.3`, mesmo que o score global tivesse melhorado em outra dimensão.

### Limitações

O benchmark usa workflows de trading e tarefas controladas. Os números específicos não transferem diretamente para coding agents. O princípio de **avaliar a trajetória da evolução** é o ponto relevante.

---

## 4. GRUET — usar incerteza da trajetória para decidir quando investigar mais

**Tipo:** pesquisa acadêmica / preprint.

**Paper:** *GRUET: Quantifying Uncertainty of Agentic Reasoning-and-Acting Processes*  
https://arxiv.org/abs/2609.24831

GRUET tenta medir a incerteza não apenas da resposta final, mas de uma trajetória ReAct completa. A ideia é gerar possíveis ramificações de raciocínio em cada turno, representar esse espaço como um grafo e usar sua complexidade como sinal de incerteza.

A avaliação cobre **9 LLMs e 5 benchmarks**, incluindo SWE-bench Verified, SWE-bench Lite e tarefas InterCode. Em média, a estratégia all-turn obteve **AUROC 75,49**, enquanto o melhor baseline comparado ficou em **66,11**. Os autores reportam ganhos médios de **17,11 pontos em AUROC**, **16,42 em AUPRC** e **11,78 em AUARC** sobre o concorrente mais forte. Uma versão muito mais barata olhando apenas o primeiro turno mantém parte relevante do poder discriminativo.

### Aplicação ao AI Hub

Isso pode alimentar diretamente o `Context Sufficiency Gate`:

```text
reasoning trajectory
      │
      ▼
UNCERTAINTY MONITOR
      │
      ├── baixa → continuar
      │
      ├── média → buscar mais evidência
      │
      └── alta → branch / subagent / perguntar
```

Eu não tentaria reproduzir o grafo completo imediatamente. Podemos começar com sinais simples:

```yaml
trajectory_uncertainty:
  competing_hypotheses: 4
  evidence_conflicts: 2
  repeated_replans: 3
  unresolved_unknowns: 2
  confidence: LOW
```

Quando esse score sobe, o harness deixa de gastar tokens na mesma trajetória e escolhe outra ação epistemicamente útil.

### Limitações

GRUET exige amostragem adicional para estimar a incerteza e ainda é um método de avaliação/seleção, não um mecanismo que descobre contexto sozinho. Sua utilidade é funcionar como **gatilho para Active Context Discovery**.

---

## Mudança arquitetural sugerida hoje

Depois destes trabalhos, eu acrescentaria duas peças ao desenho atual:

```text
                         USER REQUEST
                              │
                              ▼
                 REQUIREMENT DISCOVERY
                              │
                              ▼
                   AMBIGUITY DETECTOR
                              │
                              ▼
                      ASK-OR-INFER GATE
                ┌─────────────┼─────────────┐
                │             │             │
              infer        retrieve        ask
                │             │             │
                └─────────────┼─────────────┘
                              ▼
                       BELIEF STATE
                              │
                              ▼
                  MEMORY INFLUENCE POLICY
                 IGNORE / BOUND / CONTROL
                              │
                              ▼
                         PLANNER
                              │
                              ▼
                    TRAJECTORY MONITOR
                    uncertainty / gaps
                              │
                      ┌───────┴───────┐
                      │               │
                   continue       investigate
                                      │
                                      ▼
                             evidence / user
                              │
                              ▼
                        COMPLETION GATE
                              │
                              ▼
                      CAPABILITY TIMELINE
                       per-checkpoint eval
```

## Prioridade prática para o AI Hub

Minha ordem agora seria:

1. **Ask-or-Infer Gate**: distinguir automaticamente entre inferência segura, busca adicional e pergunta ao usuário.
2. **Question Value / Information Gain**: quando precisar perguntar, escolher a pergunta que resolve a maior decisão pendente.
3. **Memory Influence Policy** com `IGNORE / BOUND / CONTROL` por proposição.
4. **Trajectory Uncertainty Monitor** para disparar nova exploração antes de o agente ficar preso em uma linha de raciocínio ruim.
5. **Capability Timeline** para impedir que uma melhoria do harness silenciosamente destrua uma capacidade adquirida antes.

A principal conclusão da rodada é:

> **Um agente capaz de descobrir contexto ausente precisa saber não apenas o que não sabe, mas qual é a melhor ação para reduzir essa incerteza: inferir, recuperar evidência ou perguntar. E, depois de aprender, precisa calibrar o peso de cada memória e verificar se novas mudanças não apagaram capacidades anteriores.**

Hoje não encontrei um post técnico de laboratório/engenharia, publicado desde a rodada anterior, que acrescentasse evidência forte o suficiente para competir com esses quatro trabalhos acadêmicos; preferi não preencher o radar com material secundário.