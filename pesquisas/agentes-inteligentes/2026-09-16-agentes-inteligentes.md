# Radar diário — agentes mais inteligentes | 16/09/2026

A rodada de hoje trouxe novidades relevantes. O ponto mais importante é que vários trabalhos novos estão convergindo para uma arquitetura em que o agente **não interpreta o prompt uma única vez e segue em frente**. Em vez disso, o harness mantém um ciclo explícito de **hipóteses de intenção → exploração de contexto → verificação → atualização de memória/skills → replanejamento**.

Para o AI Hub, a principal conclusão de hoje é esta:

> **descobrir contexto ausente deve ser uma atividade ativa e orçada, não um efeito colateral de RAG.**

O harness precisa decidir o que explorar, quando pedir esclarecimento, quando recuperar memória, quando ativar uma skill, quando parar de gastar compute e como verificar se a hipótese sobre a intenção do usuário estava correta.

---

## 1. VisInteract — tratar prompt imperfeito como recuperação de intenção, não como geração one-shot

**Tipo:** pesquisa acadêmica / preprint.

O trabalho **VisInteract: Towards Dynamic Interactive Text-to-Visualization under Imperfect Queries**, submetido em 14 de setembro, parte explicitamente do fato de que solicitações reais são ambíguas, incompletas ou até factualmente erradas. Em vez de assumir que o prompt já especifica a tarefa, o sistema transforma o problema em **interaction-driven intent recovery**.

O benchmark introduz imperfeições controladas nas solicitações e usa um agente de usuário para fornecer feedback multi-turn. O método proposto, Vis-MCTS, explora alternativas de interpretação com Monte Carlo Tree Search, compartilha informações entre rollouts e decompõe o feedback em dimensões como fidelidade aos dados, design e alinhamento com a intenção. Nos experimentos com dois backbones, o método melhora o sucesso end-to-end em **13,40%–16,27%** sobre o melhor baseline interativo e em mais de **5×** sobre abordagens não interativas.

Fonte: https://arxiv.org/abs/2609.15182

### Aplicação no AI Hub

Hoje nosso desenho possui um `Requirement Discovery Runtime`. Eu acrescentaria um estágio explícito anterior:

```text
USER REQUEST
     │
     ▼
INTENT HYPOTHESIS GENERATOR
     │
     ├── interpretação A
     ├── interpretação B
     ├── interpretação C
     └── unknowns
     │
     ▼
EVIDENCE SEEKING
repo / MCP / memory / tests / user
     │
     ▼
INTENT SELECTION
```

Exemplo:

```yaml
request: "adicione login Google"

intent_hypotheses:
  - id: H1
    interpretation: add_google_as_optional_provider
    preserve_existing_login: true
    confidence: 0.72

  - id: H2
    interpretation: replace_existing_login
    confidence: 0.18

  - id: H3
    interpretation: google_login_for_admins_only
    confidence: 0.10
```

O ponto novo é: **a primeira interpretação plausível não vira automaticamente a especificação**. O harness procura evidências capazes de separar as hipóteses.

A limitação é que o domínio experimental é visualização, não engenharia de software. Ainda assim, a formulação do problema é diretamente transferível: prompts humanos reais não deveriam ser tratados como specs completas.

---

## 2. RSIAgent — explorar o ambiente para descobrir restrições escondidas antes da tarefa real

**Tipo:** pesquisa acadêmica / preprint + artefato de engenharia reproduzível.

O **RSIAgent: Autonomous Exploration for Recursive Self-improvement in New Environments** é provavelmente o trabalho mais alinhado à pergunta original deste radar nesta rodada. Ele assume que o agente entra em um ambiente cujo comportamento, ferramentas, convenções, restrições e modos de falha **não estão totalmente presentes no modelo nem no prompt**.

O sistema usa três papéis separados:

```text
Curriculum Agent
→ decide o que vale explorar

Actor Agent
→ interage com o ambiente

Verifier Agent
→ verifica o resultado contra o estado real
```

A exploração é dividida em duas fases. A primeira é **broad exploration**, para mapear estruturas, procedimentos e capacidades diversas. A segunda é **deep exploration**, focada em casos difíceis, restrições escondidas, boundary conditions e dependências causais ainda desconhecidas. O conhecimento validado é consolidado em memória e depois congelado para a avaliação final.

Nos resultados reportados, o score parcial no OSWorld 2.0 passa de **71,97 para 78,98**, e a acurácia binária de **37,80 para 42,68**. No Agent's Last Exam, o score parcial sobe de **83,75 para 84,82**. Na ablação de quatro tarefas, combinar exploração ampla + profunda alcança média de **74,54%**, contra 65,52% usando apenas a fase ampla e 56,50% apenas a profunda.

Fonte: https://arxiv.org/abs/2609.15364  
Artefato: https://github.com/AetherLabsAI/RSIAgent

### Aplicação no AI Hub

Isso sugere algo além de retrieval reativo. O AI Hub poderia executar **micro-explorações deliberadas** quando entra em uma área nova do projeto.

```text
TASK ARRIVES
    │
    ▼
Known environment?
    │
  no / partial
    │
    ▼
EXPLORATION CURRICULUM
    │
    ├── quais arquivos definem este comportamento?
    ├── quais testes codificam regras implícitas?
    ├── que tool/MCP revela o estado real?
    ├── quais caminhos de falha ainda não testei?
    └── quais decisões parecem existir mas não estão confirmadas?
```

Para um pedido como `adicione login Google`, uma fase ampla pode mapear controllers, filtros, sessão, secrets e testes. A fase profunda pode atacar justamente os pontos incertos: refresh token, compatibilidade com login legado, callback, logout e rotação de secrets.

A ideia mais útil é que **o agente pode aprender sobre o ambiente antes de executar a tarefa final**, como uma pessoa experiente faria ao entrar em um sistema desconhecido.

Limitações: o custo de exploração pode ser alto; o próprio paper reconhece dependência da qualidade do verifier e risco de uma memória incorreta propagar erros. Portanto, a exploração deve ser dirigida por risco e por lacunas concretas, não executada indiscriminadamente.

---

## 3. The Router Within — biblioteca de skills não deveria ocupar o contexto inteiro

**Tipo:** pesquisa acadêmica / preprint.

O trabalho **The Router Within: Eliciting Native Skill Routing from a Frozen LLM** estuda um problema que vai aparecer rapidamente no AI Hub: quanto mais skills existirem, pior fica a ideia de colocar metadata de todas elas no prompt.

O método Gavel extrai do próprio backbone um sinal de roteamento para selecionar skills, sem inserir o texto de toda a biblioteca no contexto. Um primeiro estágio barato varre a biblioteca e produz uma shortlist; um segundo estágio usa o próprio modelo para decidir entre os candidatos. Em Qwen3-32B, o paper reporta ganhos de até **13,4 pontos** em tarefas escritas e até **21,9 pontos** quando a necessidade da skill aparece no meio da trajetória.

Fonte: https://arxiv.org/abs/2609.15982

### Aplicação no AI Hub

Mesmo que o mecanismo exato do paper dependa de acesso a hidden states — algo normalmente indisponível em APIs fechadas — a implicação arquitetural é forte:

```text
ERRADO
──────
Prompt
+ metadata de 500 skills
+ tarefa

MELHOR
──────
Task / current trajectory
        │
        ▼
SKILL ROUTER
        │
        ▼
shortlist 3–8 skills
        │
        ▼
load only selected skill
```

No AI Hub eu faria o router considerar não só o prompt inicial, mas também a trajetória corrente. Uma skill pode se tornar relevante **depois** que um erro, log ou arquivo revela uma condição que o prompt não mencionou.

Exemplo:

```text
"adicione login Google"
        ↓
no início:
  oauth-spring

após descobrir sessão legada:
  preserve-legacy-auth

após encontrar Secret Manager:
  gcp-secret-management
```

Ou seja, routing de skills deveria ser **dinâmico durante o rollout**, não apenas uma decisão inicial.

---

## 4. LIMBO — memória é um recurso de inferência e deve competir com raciocínio, tools e verificação

**Tipo:** pesquisa acadêmica / preprint aceito no ICTAI 2026.

O **LIMBO: Lifelong Inference-Time Memory and Budget Optimization for LLM Agents** formaliza um problema que aparece naturalmente em agentes com muita experiência: recuperar memória tem custo. Cada trajetória antiga colocada no contexto compete pelos mesmos tokens usados para raciocínio, tool calls, exploração e verificação.

Em vez de definir uma política fixa de replay, LIMBO aprende online **quando recuperar experiência anterior e quanto orçamento gastar com ela**. Em três backbones no LifelongAgentBench, o método quase iguala os baselines de memória mais fortes usando até **~83% menos custo de inferência**, com redução média de aproximadamente **53%**.

Fonte: https://arxiv.org/abs/2609.14138

### Aplicação no AI Hub

Eu acrescentaria um `Context Budget Controller`:

```text
TOTAL TASK BUDGET
      │
      ├── current repo inspection
      ├── memory retrieval
      ├── skill loading
      ├── reasoning
      ├── tool execution
      └── verification
```

Em vez de sempre recuperar cinco experiências semelhantes, o harness poderia estimar:

```yaml
memory_decision:
  predicted_value: 0.18
  estimated_cost: 4200_tokens
  action: skip
```

ou:

```yaml
memory_decision:
  predicted_value: 0.81
  estimated_cost: 1800_tokens
  action: retrieve
```

Isso é particularmente importante no seu AI Hub porque memória, MCP, skills e subagentes tendem a crescer. **Mais contexto não significa necessariamente mais inteligência.** O recurso relevante é contexto útil por unidade de compute.

---

## 5. Stellar Colosseum — explorar estratégias antes de decompor e enviar para execução

**Tipo:** pesquisa acadêmica / preprint; trabalho integrado ao framework Teamwork do Google Antigravity.

O **Stellar Colosseum** é voltado a matemática e ciência da computação teórica, mas a arquitetura contém três mecanismos muito transferíveis: explorar estratégias alternativas antes de construir a solução, usar um **readiness gate** antes de decompor o problema e direcionar achados do verifier somente à parte da solução afetada.

O harness gera candidatos em paralelo, tenta falsificá-los deliberadamente e só então consolida uma rota. No TCS-Bench, reporta **71,0%** usando Gemini 3.1 Pro e Gemini 3.7 Flash; em uma avaliação Codeforces, a pipeline orientada a prova com feedback de execução resolve **218 de 222** problemas.

Fonte: https://arxiv.org/abs/2609.15983

### Aplicação no AI Hub

Isso sugere que o planner não deveria decompor imediatamente toda solicitação.

```text
USER GOAL
    │
    ▼
STRATEGY EXPLORATION
    │
    ├── estratégia A
    ├── estratégia B
    ├── estratégia C
    │
    ▼
TARGETED FALSIFICATION
    │
    ▼
READINESS GATE
    │
    ├── mature → decompose
    └── weak   → explore more
```

Para software, o falsifier procuraria coisas como:

```text
- qual hipótese arquitetural pode estar errada?
- que comportamento existente esta solução quebraria?
- qual requisito implícito ainda não está coberto?
- qual dependência externa invalida o plano?
```

Essa abordagem reduz um problema comum de long-horizon agents: **decompor cedo demais uma interpretação errada e depois executar muito trabalho sobre a base errada**.

---

## 6. Resultado adicional: mais test-time compute não deve ser alocado como uma sessão única

**Tipo:** pesquisa acadêmica / análise empírica.

O paper **When Agents Slow Down: Understanding LLM Agents' Test-Time Strategies via Elo-per-token Analysis** mede a produtividade marginal do compute durante rollouts muito longos. Os autores observam que agentes inicialmente convertem tokens em progresso de forma eficiente, mas depois entram em uma região de ganhos decrescentes. Ao usar o ponto de inflexão para dividir **100 milhões de tokens** entre sessões paralelas, obtêm +264 Elo sobre uma única sessão longa e +355 Elo sobre dez sessões curtas em um dos benchmarks analisados.

Fonte: https://arxiv.org/abs/2609.15309

### Aplicação no AI Hub

O `Context Budget Controller` não deveria decidir apenas **quanto** compute usar, mas também **como distribuí-lo**:

```text
long single rollout
vs
multiple independent hypotheses
vs
specialized subagents
vs
fresh restart with preserved evidence
```

Quando uma trajetória começa a repetir as mesmas hipóteses, aumentar `max_tokens` pode valer menos do que abrir uma rota independente.

---

# Arquitetura recomendada após a rodada de hoje

A síntese dos trabalhos de hoje sugere esta evolução:

```text
                         USER REQUEST
                              │
                              ▼
                  INTENT HYPOTHESIS LAYER
              alternative interpretations + unknowns
                              │
                              ▼
                 CONTEXT SUFFICIENCY CHECK
                              │
                    enough? / not enough
                              │
                              ▼
                EXPLORATION CURRICULUM AGENT
                   broad → deep discovery
                              │
          ┌───────────────────┼───────────────────┐
          │                   │                   │
         repo                MCP               memory
          │                   │                   │
          └───────────────────┼───────────────────┘
                              ▼
                       TASK BELIEF STATE
                              │
                              ▼
                    DYNAMIC SKILL ROUTER
                              │
                              ▼
                    STRATEGY EXPLORATION
                              │
                              ▼
                      READINESS GATE
                              │
                              ▼
                           PLANNER
                              │
                              ▼
                          EXECUTOR
                              │
                              ▼
                         VERIFIER
                              │
                     new evidence / failure
                              │
                              ▼
                  BELIEF + MEMORY UPDATE
                              │
                              ▼
                   COMPUTE BUDGET ROUTER
                    continue / branch / stop
```

## O que eu implementaria primeiro

Minha prioridade agora seria:

1. **Intent Hypothesis Layer** — produzir interpretações alternativas e não transformar a primeira hipótese em verdade.
2. **Exploration Curriculum** — quando o contexto está incompleto, escolher deliberadamente o que investigar e em que ordem.
3. **Verifier separado** — impedir que observações não verificadas virem memória ou requisito.
4. **Dynamic Skill Router** — carregar skills apenas quando a trajetória indicar necessidade.
5. **Context/Compute Budget Controller** — decidir quanto orçamento vai para memória, exploração, raciocínio e verificação.
6. **Readiness Gate antes da decomposição** — só transformar uma estratégia em plano detalhado quando as hipóteses críticas estiverem suficientemente sustentadas.

O conceito que mais evoluiu nesta rodada é o que poderíamos chamar de **Active Context Discovery**. Até agora estávamos pensando principalmente em detectar lacunas e recuperar informações. Os trabalhos de hoje sugerem algo mais poderoso:

```text
não sei algo
    ↓
gerar hipóteses sobre o que pode estar faltando
    ↓
decidir qual experimento / busca diferencia as hipóteses
    ↓
observar resultado
    ↓
atualizar belief state
    ↓
selecionar skill / estratégia
    ↓
verificar
```

Isso é mais próximo de **investigação** do que de RAG.

Para o seu AI Hub, essa me parece hoje a direção mais promissora para fazer o agente perceber coisas que você não pensou em colocar no prompt sem transformar o sistema em uma máquina que pergunta tudo ao usuário.
