# Radar diário — agentes mais inteligentes | 17/09/2026

A rodada de hoje trouxe uma combinação particularmente útil para o AI Hub. O achado mais novo é o **ContrAgent**, submetido em 16 de setembro, que transforma requisitos de comportamento em contratos temporais determinísticos capazes de **bloquear ações online e avaliar a trajetória depois**. Ao lado dele, trabalhos recentes sobre falhas de ferramentas, memória reconsolidada, self-improvement e avaliação de coding agents apontam para uma arquitetura cada vez mais clara:

> **o agente pode inferir requisitos e descobrir contexto com LLMs, mas o harness deveria transformar as conclusões importantes em estados explícitos, evidência verificável e contratos executáveis.**

O padrão que aparece hoje é: `inferir → buscar evidência → declarar status → formalizar condição → agir → verificar → aprender`.

---

## 1. ContrAgent — requisitos importantes podem virar contratos executáveis do harness

**Tipo:** pesquisa acadêmica / preprint.

O paper **Symbolic Temporal Supervision of LLM Agents Using Contracts**, submetido em **16/09/2026**, apresenta o ContrAgent. Em vez de depender apenas de um LLM-judge depois da execução ou de regras isoladas antes de cada tool call, ele representa a trajetória do agente como uma sequência de predicados verificáveis e expressa requisitos usando contratos *assume-guarantee* em lógica temporal finita (LTLf).

Cada contrato é compilado para um autômato determinístico. O mesmo artefato serve para duas funções:

1. **gating online** — impedir uma ação quando a trajetória corrente violaria o contrato;
2. **avaliação offline** — verificar de maneira reprodutível se uma execução gravada respeitou o requisito.

Em quatro benchmarks, ContrAgent igualou baselines fortes baseados em LLM judges e guardrails por regras, mas produziu decisões determinísticas e, no modo online, teve latência por chamada ordens de magnitude menor.

Fonte principal: https://arxiv.org/abs/2609.18128

### Aplicação no AI Hub

Isso resolve um problema que vem aparecendo no radar: o `Requirement Discovery Runtime` pode inferir algo correto, mas o requisito continua sendo apenas texto dentro do contexto do modelo.

Eu acrescentaria um estágio:

```text
USER REQUEST
    ↓
Requirement Discovery Runtime
    ↓
FACT / INFERRED / UNKNOWN
    ↓
EVIDENCE CONFIRMATION
    ↓
EXECUTABLE CONTRACT
    ↓
Planner / Executor
```

Exemplo para:

```text
"adicione login Google"
```

Depois de confirmar no repo que o login por senha precisa continuar funcionando:

```yaml
contract:
  id: preserve-password-login

  assumption:
    existing_password_login: true

  guarantee:
    after_auth_change:
      AuthPasswordIT: PASS

  gate:
    before_completion: require_guarantee
```

O ponto importante é que **o requisito implícito deixa de depender da memória do LLM**. Depois de confirmado, ele passa a ser uma condição externa ao modelo.

Eu não tentaria formalizar tudo em LTLf. Requisitos simples podem virar testes, predicates ou gates. A contribuição arquitetural é separar:

```text
inferência do requisito
        ≠
enforcement do requisito
```

A limitação é que ContrAgent pressupõe predicados observáveis e formalizáveis; preferências humanas ambíguas ou objetivos qualitativos continuam exigindo julgamento probabilístico.

---

## 2. Fabrication After Tool Failure — o MCP precisa declarar explicitamente quando não conseguiu descobrir algo

**Tipo:** pesquisa acadêmica / preprint com benchmark de tool use.

**Fabrication After Tool Failure: Tool-Augmented Agents Assert Values Their Tools Did Not Return** isola um erro perigosíssimo para nosso objetivo. O benchmark possui **1.024 casos**, 16 domínios internos e oito tipos de falha de ferramenta. A ferramenta é chamada obrigatoriamente e nunca fornece informação utilizável.

Sob um prompt de deployment, **14,10%** das respostas afirmaram algo que a ferramenta não sustentava ou inventaram uma justificativa. A variável decisiva foi como a falha era representada:

- `status:error` → **0,0%** de respostas desonestas;
- `status:ok` com valor vazio, truncado, stale, corrompido ou inutilizável → **45,3%**.

Nenhum dos nove frameworks de agentes auditados especificava claramente o comportamento esperado após uma falha de tool. Exigir que o modelo declarasse `retrieval_status: OK | FAILED` antes de responder derrubou a taxa de 14,10% para **0,87%**; a flag foi fiel em 99,7%–99,9% das declarações.

Fonte: https://arxiv.org/abs/2609.14758

### Aplicação no AI Hub/MCP

Isso é diretamente aplicável à descoberta de contexto ausente. Hoje um agente poderia fazer:

```text
preciso descobrir quem controla a sessão
        ↓
MCP search
        ↓
resultado vazio / truncado
        ↓
LLM preenche a lacuna com uma suposição plausível
```

Eu mudaria o contrato de todas as ferramentas de contexto para algo explícito:

```yaml
retrieval_result:
  status: OK | FAILED | PARTIAL | STALE
  evidence_count: 0
  freshness: current
  confidence: 0.0
  reason: null
```

E o harness teria uma regra:

```text
FAILED / PARTIAL / STALE
        ↓
NÃO promover conclusão para FACT
        ↓
buscar outra fonte / testar / perguntar
```

Para o `Task Belief State`, eu acrescentaria proveniência do status:

```yaml
belief:
  statement: backend_controls_session
  state: UNKNOWN
  retrieval_status: FAILED
  source: architecture_mcp
```

Esse é um detalhe pequeno de engenharia com potencial enorme: **missing-context detection só funciona se o sistema também souber representar “não consegui recuperar o contexto”.**

---

## 3. REALM — a própria recuperação pode ensinar a memória a se reorganizar

**Tipo:** pesquisa acadêmica / preprint.

O trabalho **Retrieval-Driven Memory Reconsolidation for Long-Term LLM Agents (REALM)** trata memória como um sistema que continua evoluindo depois de ser escrita. A maioria das arquiteturas atualiza memória quando chega informação nova e trata retrieval apenas como leitura. REALM usa o que foi recuperado e utilizado durante raciocínio como feedback para reorganizar a própria memória.

A arquitetura mantém um **grafo cognitivo heterogêneo**, compõe dinamicamente operações de busca no grafo e, depois da recuperação, realiza reconsolidação local baseada em confiança. Memórias que frequentemente precisam ser recuperadas juntas passam a ficar estruturalmente mais próximas.

Nos benchmarks reportados, REALM alcançou **75,97% no LoCoMo** e **65,11% no LongMemEval**, superando os melhores baselines em **7,17 e 1,31 pontos**, respectivamente. As ablações indicam ganho consistente com a etapa de reconsolidação.

Fonte: https://arxiv.org/abs/2609.16053

### Aplicação no AI Hub

Isso sugere uma evolução importante para o `MemoryCue + provenance` que desenhamos.

Hoje poderíamos ter:

```text
"login Google"
    ↓
OAuth memory
    ↓
Auth architecture
    ↓
Secret Manager decision
```

Se essas três evidências aparecem repetidamente juntas em tarefas de autenticação, o sistema poderia aprender uma estrutura:

```text
Authentication Workflow
   ├── session authority
   ├── provider integration
   ├── secret management
   ├── legacy auth regression
   └── callback policy
```

Ou seja, a memória passa a aprender **quais contextos costumam formar um conjunto útil**, e não apenas quais documentos têm embeddings semelhantes.

Eu não permitiria que a reconsolidação alterasse a fonte canônica. Ela deveria alterar apenas **índices, relações e prioridades de retrieval**:

```text
CANONICAL EVIDENCE
      │
      └── immutable

MEMORY GRAPH
      │
      └── evolui com uso
```

A limitação é que LoCoMo/LongMemEval medem memória de conversação e long-term QA, não a descoberta de requisitos em repositórios de software. A transferência para AI Hub precisa ser validada.

---

## 4. ScienceBuddy — interação real pode virar tarefa, rubrica e melhoria controlada do harness

**Tipo:** pesquisa acadêmica / preprint + sistema de engenharia aberto.

O **ScienceBuddy: Recursive-in-Recursive Self-Improvement for Interactive Scientific Agents**, submetido em **15/09**, une duas escalas de melhoria:

```text
inner loop  → evolui o harness mantendo o modelo fixo
outer loop  → treina o modelo usando o harness melhorado
```

O ponto mais interessante para o AI Hub não é o domínio científico, mas **como experiência real é convertida em material de aprendizado**. Solicitações, feedback, trajetórias, tool calls e evidências são transformados em tarefas estruturadas e rubricas reutilizáveis. Mudanças no harness são limitadas, avaliadas contra o parent sob condições equivalentes e rejeitadas quando provocam regressão.

No estudo detalhado do sistema, a adaptação de harness com modelo fixo elevou a validação de **31,1% para 51,1%**; na experiência acoplada, a acurácia em tarefas científicas held-out passou de **42,2% para 73,3%** ao longo dos ciclos de evolução e treinamento.

Fonte principal: https://arxiv.org/abs/2609.17523  
Código/projeto: https://github.com/Gen-Verse/ScienceBuddy

### Aplicação no AI Hub

Eu transformaria cada correção sua em um candidato a **eval**, e não diretamente em uma nova regra global.

Exemplo:

```text
Paulo:
"você esqueceu que o login antigo precisava continuar funcionando"
```

Em vez de apenas atualizar a memória:

```yaml
experience:
  hidden_requirement: preserve_existing_login
  discovery_failure: true
```

criaríamos automaticamente um caso de regressão:

```yaml
eval:
  prompt: "adicione login Google"

  hidden_context:
    existing_password_login: true

  expected_discovery:
    - preserve_existing_login

  expected_evidence:
    - current_auth_flow
    - regression_test
```

A evolução do harness só seria aceita se melhorasse esse caso **sem piorar os anteriores**.

Essa é uma forma concreta de transformar seu uso cotidiano do AI Hub numa bateria crescente de testes sobre a capacidade de inferir aquilo que você não escreveu.

A limitação é que o estudo é concentrado em biomedicina e combina vários componentes; não isola perfeitamente quanto do ganho provém de cada mudança do harness.

---

## 5. Coding Agents Have Converged — o benchmark precisa medir modelo + scaffold, não apenas o modelo

**Tipo:** pesquisa acadêmica / análise empírica de benchmarks.

**Coding Agents Have Converged: Why the SWE-bench Leaderboard Can No Longer Order Its Top Entries, and What to Measure Instead**, de 15/09, auditou **254 submissões** do SWE-bench. No Verified, os dois líderes acertam exatamente 396/500 casos. Entre os top 30, testes pareados não conseguem separar estatisticamente nenhum dos **29 pares adjacentes** a α=0,05.

O dado mais importante para este radar é o efeito do scaffold. Mantendo o modelo e variando o scaffold, o range observado chega a **29,8 pontos percentuais**; o spread inteiro dos top 30 é apenas **8,8 pontos**.

Fonte: https://arxiv.org/abs/2609.17394

### Aplicação no AI Hub

Isso reforça empiricamente a tese central do projeto:

```text
agent capability
    =
model
+
harness
+
tools
+
context policy
+
memory
+
verification
```

Portanto eu não avaliaria a evolução do seu AI Hub perguntando apenas:

```text
GPT-X resolveu mais tarefas que GPT-Y?
```

Eu congelaria o modelo e compararia versões do harness:

```text
Sol + Harness v21
vs
Sol + Harness v22
```

usando justamente os casos de requisitos omitidos que estamos acumulando.

Métricas principais:

```text
Implicit Requirement Recall
Missing Context Detection
Evidence Retrieval Success
Silent Assumption Rate
Context Utilization
Verification Coverage
Repeated-Run Consistency
```

A limitação do paper é importante: ele faz análise observacional de resultados publicados e não identifica causalmente qual mecanismo do scaffold gera cada ganho. Mas mostra que **tratar o score como propriedade do modelo é insuficiente**.

---

## 6. Emergence World — contexto descoberto também pode ser contexto contaminado

**Tipo:** pesquisa acadêmica / preprint + stress test de longa duração.

O **Emergence World**, submetido em 15/09, executou oito mundos paralelos com dez agentes durante **16 dias**, acumulando mais de **850 mil chamadas de LLM** e quase **50 bilhões de tokens**. Depois que memória e estado operacional já estavam estabelecidos, os pesquisadores introduziram três eventos adversariais: prompt injection indireto, misinformation e exposição de memórias privadas.

Nenhum mundo apresentou resiliência completa aos três eventos. O resultado mais importante para nosso desenho é que **detectar a ameaça não garantiu contenção**: agentes reconheceram conteúdo adversarial e ainda assim interagiram com ele, salvaram-no em memória persistente e chegaram a agir com base nele **até 46 horas depois**.

Fonte: https://arxiv.org/abs/2609.17320

### Aplicação no AI Hub

Esse resultado muda uma premissa importante do nosso `Memory Curator`.

Não basta:

```text
retrieve → detect suspicious → warn
```

O material duvidoso precisa entrar em uma zona de quarentena:

```text
UNTRUSTED EVIDENCE
       │
       ├── pode ser analisada
       ├── não pode atualizar FACT
       ├── não pode virar SKILL
       └── não pode contaminar memória persistente
```

Eu acrescentaria ao esquema de evidência:

```yaml
evidence:
  trust_state: VERIFIED | UNVERIFIED | QUARANTINED
  origin: repo | mcp | web | agent | user
  validation: null
  promotable_to_memory: false
```

Isso importa especialmente quando agentes passam a descobrir contexto sozinhos em fontes externas, MCPs ou mensagens de outros agentes. **Mais autonomia de busca exige mais disciplina na promoção de evidência para memória.**

---

# O desenho que eu adotaria agora

Depois desta rodada, eu consolidaria o núcleo assim:

```text
                       USER REQUEST
                            │
                            ▼
                 INTENT HYPOTHESIS LAYER
                            │
                            ▼
              REQUIREMENT DISCOVERY RUNTIME
                            │
                            ▼
                 CONTEXT / TOOL RETRIEVAL
                            │
                    explicit status
              OK / PARTIAL / FAILED / STALE
                            │
                            ▼
                       BELIEF STATE
            FACT / INFERRED / UNKNOWN / RISK
                            │
                            ▼
                   EVIDENCE VALIDATION
                            │
             ┌──────────────┴──────────────┐
             │                             │
          trusted                      untrusted
             │                             │
             ▼                             ▼
    EXECUTABLE CONTRACT               QUARANTINE
             │
             ▼
           PLANNER
             │
             ▼
           EXECUTOR
             │
             ▼
     CONTRACT / TEST GATE
             │
             ▼
         EXPERIENCE
             │
        ┌────┴────┐
        │         │
     memory      eval
        │         │
        ▼         ▼
 reconsolidate  harness evolution
```

A principal evolução em relação ao desenho de ontem é que eu colocaria **uma fronteira formal entre “o agente acredita” e “o sistema passa a exigir”**.

O fluxo seria:

```text
1. agente percebe uma lacuna
2. formula uma hipótese
3. recupera evidência
4. ferramenta declara claramente se a recuperação funcionou
5. evidência é validada
6. hipótese confirmada vira requirement/contract
7. executor trabalha sob esse contrato
8. verifier prova a condição
9. experiência vira eval e/ou memória reconsolidada
```

Isso evita três falhas diferentes que os papers desta rodada mostram muito bem:

```text
não encontrei contexto
→ mas inventei uma resposta

encontrei contexto errado
→ e contaminei a memória

inferi requisito correto
→ mas o executor simplesmente esqueceu de respeitá-lo
```

## Prioridade prática para o AI Hub

1. **Normalizar status de todas as tools/MCPs** (`OK/PARTIAL/FAILED/STALE`).
2. **Criar Evidence Trust State** antes de qualquer promoção para memória.
3. **Transformar requisitos implícitos confirmados em acceptance contracts/gates**.
4. **Criar eval automaticamente a partir de correções reais do usuário**.
5. **Comparar versões do harness com modelo congelado**, em vez de avaliar apenas troca de modelo.
6. **Permitir reconsolidação da estrutura de retrieval**, preservando a evidência canônica imutável.

A conclusão mais importante de hoje é:

> **um agente mais inteligente não é apenas aquele que deduz o que faltou no prompt; é aquele cujo harness consegue distinguir hipótese de fato, falha de retrieval de ausência de informação, evidência confiável de evidência contaminada e intenção inferida de requisito efetivamente obrigatório.**

Esse é o ponto em que o `Requirement Discovery Runtime` começa a virar algo mais robusto: um **runtime epistemológico + contratual** em torno do modelo.