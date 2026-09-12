# Radar diário — agentes mais inteligentes | 12/09/2026

A rodada de hoje trouxe um conjunto especialmente coerente de trabalhos recentes. A principal conclusão é que, para um agente descobrir aquilo que o usuário não colocou no prompt, não basta melhorar o `retrieval`. O sistema precisa controlar **como hipóteses viram memória**, **como requisitos e estados mudam durante a execução**, **como relações implícitas entre artefatos são reconstruídas** e **como o resultado final é provado com evidência atual**.

Os trabalhos mais fortes encontrados nesta rodada foram submetidos em 9–10 de setembro de 2026. Não encontrei hoje um novo post técnico de laboratório que acrescentasse evidência mais forte do que esses preprints e o lançamento da Agents API já coberto na rodada anterior; por isso, a edição de hoje é predominantemente acadêmica/preprint.

## 1. Grounding Agent Memory: memória não deveria aprender apenas com a trajetória do agente

**Tipo:** pesquisa acadêmica / preprint  
**Submetido:** 10/09/2026  
**Fonte:** https://arxiv.org/abs/2609.11060

O paper **Grounding Agent Memory: Environment-Probing Curation for Enterprise Agents** ataca um problema diretamente ligado ao AI Hub: quando uma tarefa termina, muitos sistemas usam a própria trajetória do agente para produzir memória. Mas essa trajetória pode conter inferências erradas, caminhos ineficientes, conhecimento incompleto ou informações que já ficaram obsoletas.

A proposta é separar o agente que executa a tarefa de um **Memory Curator** assíncrono. Depois da execução, esse curator recebe a trajetória, o feedback final e memórias relacionadas, mas também ganha acesso somente-leitura a ferramentas do ambiente. Assim, antes de gravar uma memória ele pode conferir a informação no mundo real.

O fluxo proposto é aproximadamente:

```text
TASK AGENT
    │
    ▼
trajectory + result
    │
    ▼
MEMORY CURATOR
    │
    ├── propose candidate memory
    ├── probe environment with read-only tools
    ├── check scope / preconditions / staleness
    └── commit / revise / reject memory
```

Os autores chamam isso de **propose–probe–commit**.

No CLBench, adicionar environment probing aumentou a taxa de sucesso de **39% para 73%**, elevou a recompensa descontada de 8,60 para 22,60, reduziu a média de consultas por pergunta de 8,8 para 4,7 e reduziu o custo do agente de tarefa de US$ 3,38 para US$ 1,68. Em seis ambientes APEX, as comparações memória-versus-baseline foram positivas e o probing apresentou a melhor relação ganho/custo em cinco deles.

### Aplicação ao AI Hub

Eu mudaria a arquitetura da memória para separar claramente quem **usa** memória de quem **escreve** memória:

```text
Executor
  └── memory: READ ONLY

Memory Curator
  ├── memory: CRUD
  ├── repo: READ ONLY
  ├── MCP: READ ONLY
  ├── docs: READ ONLY
  └── logs/tests: READ ONLY
```

Exemplo: após uma tarefa OAuth o executor poderia concluir:

```text
"refresh token deve ficar no backend"
```

O curator não gravaria isso imediatamente. Ele consultaria arquitetura, código, testes e decisões anteriores. Só então produziria algo como:

```yaml
memory:
  statement: backend_owns_refresh_token
  type: architectural_constraint
  status: confirmed
  scope: authentication
  confidence: high
  provenance:
    - docs/auth.md
    - auth-service implementation
    - regression tests
  last_verified: 2026-09-12
```

Esse mecanismo é provavelmente a melhoria mais concreta da rodada para evitar que o agente aprenda uma regra falsa a partir da própria execução.

### Limitação

Os resultados ainda estão concentrados em CLBench e tarefas APEX adaptadas. O paper não demonstra que o mesmo ganho ocorrerá automaticamente em desenvolvimento Java/Spring, mas o princípio de **verificar a memória contra o ambiente antes de persistir** é altamente transferível.

---

## 2. Agent-Integrated Software: requisitos precisam permanecer conectados à execução enquanto o usuário muda de ideia

**Tipo:** pesquisa acadêmica / perspectiva de engenharia de software  
**Submetido:** 10/09/2026  
**Fonte:** https://arxiv.org/abs/2609.11381

O paper **Agent-Integrated Software: Interaction Contracts and Continuous Assurance** aborda um problema que os agentes long-running inevitavelmente encontram: enquanto o agente trabalha, o usuário pode mudar o objetivo, alterar um artefato ou revogar uma decisão anterior.

Os autores propõem **Interaction Contracts** entre a intenção do usuário e a execução do agente. Esses contratos registram vínculos da tarefa, autoridade, transições permitidas e evidências necessárias para demonstrar que o estado atual ainda corresponde ao objetivo.

A ideia importante para o AI Hub é que o requisito não deveria existir somente dentro do prompt inicial. Ele deveria existir como um objeto versionado do sistema:

```yaml
task_contract:
  task_id: AUTH-42
  goal_version: 7

  requirements:
    - google_login
    - preserve_password_login

  authority:
    code_write: allowed
    production_deploy: forbidden

  acceptance_evidence:
    - AuthPasswordIT
    - OAuthCallbackIT
    - secret_scan
```

Se você disser no meio do trabalho:

> "não quero mais login por senha"

isso cria `goal_version: 8`.

O planner e qualquer ação futura deveriam então provar que foram derivados da versão atual, e não de uma especificação antiga.

### Aplicação ao AI Hub

Eu criaria uma camada chamada **Intent Contract Store**:

```text
conversation
    │
    ▼
Requirement Compiler
    │
    ▼
Intent Contract vN
    │
    ├── goal
    ├── requirements
    ├── constraints
    ├── authority
    ├── evidence required
    └── dependencies
```

Esse objeto seria independente do histórico textual. Planner, executor e verifier leriam a versão vigente.

Isso complementa muito bem o `Belief State` que já vínhamos desenhando: o belief state representa aquilo que o agente acredita sobre o mundo; o **Intent Contract** representa aquilo que o sistema acredita ser a intenção vigente do usuário.

### Limitação

Este é mais um framework conceitual e agenda de pesquisa do que um benchmark de grande escala. Seu valor aqui é arquitetural, não um ganho percentual diretamente reproduzível.

---

## 3. NetArtifactBench: agentes ainda são muito fracos em reconstruir relações implícitas espalhadas por vários artefatos

**Tipo:** pesquisa acadêmica / benchmark  
**Submetido:** 09/09/2026  
**Fonte:** https://arxiv.org/abs/2609.09849

O paper **Can AI Agents Detect and Repair Artifact Drift in Network Experiments?** é especialmente relevante porque mede uma capacidade muito próxima daquilo que queremos: recuperar relações que **não estão explicitamente escritas em um único lugar**.

O benchmark contém 52 cenários com inconsistências inseridas em artefatos reais. Algumas são contradições diretas; outras exigem perceber relações implícitas distribuídas entre vários arquivos e propagar a correção para todos os lugares afetados.

Foram avaliadas 23 configurações de agentes em três runtimes, totalizando **5.980 outputs**. A taxa média de contratos satisfeitos foi **65,3%**. Porém, quando o reparo exigia **recuperar relações implícitas e propagar mudanças entre artefatos, nenhum runtime ultrapassou 30%**.

Esse resultado é quase uma medição direta da nossa pergunta original.

Um agente pode entender:

```text
arquivo A mudou
```

mas não deduzir:

```text
A altera a premissa de B
B sustenta a configuração de C
logo C também precisa mudar
```

### Aplicação ao AI Hub

Eu acrescentaria um **Artifact Dependency Graph** ao contexto do projeto:

```text
architecture decision
        │
        ▼
config
        │
        ├────► tests
        │
        ├────► documentation
        │
        └────► deployment
```

Cada alteração relevante produziria uma busca de impacto:

```text
changed fact
    │
    ▼
dependency traversal
    │
    ├── explicit dependency
    ├── inferred dependency
    └── unknown relation
```

Isso também sugere uma nova métrica para o benchmark do AI Hub:

```text
Implicit Dependency Recall
```

Não basta medir se o agente encontrou um requisito oculto. Precisamos medir se ele identificou **quais outros artefatos dependem daquele requisito**.

### Limitação

O domínio é rede/experimentos, não software empresarial convencional. Mesmo assim, a dificuldade observada — relações implícitas distribuídas entre artefatos — existe exatamente em repositórios de software grandes.

---

## 4. EvidenceNet: executar a ação correta não prova que o objetivo foi alcançado

**Tipo:** pesquisa acadêmica / runtime assurance  
**Submetido:** 09/09/2026  
**Fonte:** https://arxiv.org/abs/2609.10181

**Can AI Agents Deliver Verifiable Network-Wide Outcomes Across Authority Boundaries?** apresenta o EvidenceNet, uma camada de garantia de runtime.

O argumento central é simples e extremamente importante: um registro dizendo que uma ação foi executada com sucesso não significa que o **resultado desejado** ocorreu.

```text
API returned 200
        ≠
objective achieved
```

Além disso, evidência correta pode ficar obsoleta depois de outra mudança.

O EvidenceNet usa um **completion contract** que define quais observações precisam existir antes de uma operação ser declarada concluída. Um admission gate verifica se a evidência vem das fontes corretas, ainda está atual e satisfaz as regras da tarefa. Experimentos em redes reais mostram que a camada rejeita conclusão quando as observações são de origem incorreta, foram substituídas ou ficaram stale.

### Aplicação ao AI Hub

Eu transformaria o seu `Verifier` em um verdadeiro **Completion Admission Gate**:

```yaml
completion_contract:
  goal: google_oauth_available

  evidence:
    - source: integration_test
      id: OAuthCallbackIT
      freshness: after_last_code_change

    - source: regression_test
      id: AuthPasswordIT
      freshness: after_last_code_change

    - source: secret_scan
      scope: repository
      freshness: current_commit

  admit_completion_only_if:
    all_evidence_current: true
```

O executor poderia dizer "terminei", mas o AI Hub somente marcaria a tarefa como `DONE` depois que esse contrato fosse satisfeito.

Isso é importante para requisitos implícitos porque o Requirement Compiler pode deduzir algo como "preservar login existente". O completion contract transforma essa dedução em uma obrigação observável.

### Limitação

A validação foi feita em automação de redes e coordenação entre escopos de autoridade. Precisamos adaptar o conceito de evidência/freshness para código, testes, documentação e deploy.

---

## 5. Belief-State Engine: talvez o estado mental do agente não deva ser texto livre

**Tipo:** pesquisa acadêmica / arquitetura de agentes  
**Submetido:** 09/09/2026  
**Fonte:** https://arxiv.org/abs/2609.10036

O **Belief-State Engine (BSE)** parte de um problema recorrente em agentes: quando existe observabilidade parcial, feedback ambíguo pode causar compromissos prematuros e uma única observação pode fazer o modelo se fixar na hipótese errada.

A proposta coloca um módulo de inferência **fora do LLM**. Esse módulo mantém uma distribuição de probabilidade sobre estados ocultos e fornece ao LLM somente o belief state relevante para decidir. O histórico bruto de ações e observações não é entregue ao modelo.

Os autores mostram formalmente que, sob suas premissas, a arquitetura passa a operar como uma política consistente no belief-MDP, e nos testes com Tiger POMDP e um cenário de attack graph melhora retorno, calibração e consistência em comparação com seis baselines, incluindo ReAct e um belief tracker em linguagem natural.

### Aplicação ao AI Hub

Não precisamos implementar um POMDP completo para aproveitar a ideia. Mas eu tornaria o `Task Belief State` **estruturado e externo ao modelo**:

```yaml
beliefs:

  - claim: backend_controls_session
    probability: 0.98
    status: confirmed
    evidence:
      - docs/auth.md
      - SessionService.java

  - claim: password_login_must_be_preserved
    probability: 0.82
    status: inferred_requirement
    evidence:
      - existing_behavior

  - claim: refresh_token_backend_only
    probability: 0.45
    status: unresolved
    evidence: []
```

O planner não precisaria reler todo o histórico para reconstruir esse estado. O harness atualizaria o belief state conforme novas evidências fossem obtidas.

Esse desenho também ajuda a evitar um erro comum: um texto de raciocínio muito convincente acabar sendo tratado como fato. O fato vive fora do LLM e precisa de evidência para aumentar sua confiança.

### Limitação

Os experimentos são em ambientes POMDP relativamente controlados. A dificuldade de modelar explicitamente todos os estados de um grande projeto de software é muito maior. Para o AI Hub, a melhor adaptação provavelmente é um belief state parcial e orientado aos requisitos críticos, não uma representação completa do mundo.

---

# O que mudou na arquitetura hoje

Somando esses trabalhos aos achados das rodadas anteriores, eu faria o núcleo do AI Hub assim:

```text
                         USER INTENT
                              │
                              ▼
                    REQUIREMENT COMPILER
                              │
                              ▼
                    INTENT CONTRACT STORE
                   goal / requirements / authority
                              │
                              ▼
                       TASK BELIEF STATE
                 facts / hypotheses / unknowns
                              │
                              ▼
                 CONTEXT & ARTIFACT DISCOVERY
             repo / MCP / memory / dependencies
                              │
                              ▼
                    ARTIFACT DEPENDENCY GRAPH
                              │
                              ▼
                           PLANNER
                              │
                              ▼
                           EXECUTOR
                              │
                              ▼
                  COMPLETION ADMISSION GATE
                  current evidence required
                              │
                              ▼
                         TASK CLOSED
                              │
                              ▼
                       MEMORY CURATOR
                              │
                    propose → probe → commit
                              │
                              ▼
                    VERIFIED PROJECT MEMORY
```

A mudança principal é que eu **não deixaria mais o executor gravar diretamente memória durável nem declarar sozinho que uma tarefa terminou**.

Teríamos três autoridades diferentes:

```text
Requirement Compiler
→ interpreta intenção

Executor
→ altera o mundo

Verifier / Curator
→ verifica resultado e transforma experiência em memória
```

Essa separação reduz dois erros muito perigosos:

1. o agente acreditar na própria interpretação errada;
2. o agente transformar essa interpretação errada em memória permanente.

# Insight central da rodada

Até agora estávamos pensando principalmente:

```text
"Como fazer o agente descobrir o contexto que Paulo não colocou no prompt?"
```

A pesquisa de hoje acrescenta uma pergunta ainda mais importante:

```text
"Depois que ele descobriu algo, como sabemos se aquilo é verdade,
continua válido e realmente implica uma mudança na execução?"
```

Eu resumiria o mecanismo ideal assim:

```text
DETECT GAP
    ↓
FORM HYPOTHESIS
    ↓
SEARCH EVIDENCE
    ↓
CHECK CROSS-ARTIFACT RELATIONS
    ↓
UPDATE BELIEF
    ↓
BIND REQUIREMENT TO PLAN
    ↓
EXECUTE
    ↓
VERIFY CURRENT OUTCOME
    ↓
CURATE MEMORY AGAINST THE ENVIRONMENT
```

Essa sequência começa a parecer menos com um simples agente ReAct e mais com um **sistema operacional cognitivo para agentes**.

# Prioridade prática para o AI Hub

Com os achados de hoje, eu priorizaria agora:

1. **Memory Curator separado do executor**, com ferramentas MCP/read-only para validar memórias antes de gravá-las.
2. **Intent Contract versionado**, para que objetivos e requisitos não fiquem presos apenas no histórico do chat.
3. **Artifact Dependency Graph**, começando por relações simples entre decisões, código, configuração, testes e documentação.
4. **Completion Admission Gate**, impedindo `DONE` sem evidência atual.
5. **Task Belief State estruturado**, mantendo fatos, hipóteses, unknowns, confiança e proveniência fora do texto livre do LLM.

Eu colocaria self-improving harness depois dessas peças. Antes de deixar o sistema se autoalterar, vale garantir que ele sabe distinguir **experiência observada, inferência, evidência confirmada e memória durável**.

## Fontes principais

- Grounding Agent Memory: Environment-Probing Curation for Enterprise Agents — https://arxiv.org/abs/2609.11060
- Agent-Integrated Software: Interaction Contracts and Continuous Assurance — https://arxiv.org/abs/2609.11381
- Can AI Agents Detect and Repair Artifact Drift in Network Experiments? — https://arxiv.org/abs/2609.09849
- Can AI Agents Deliver Verifiable Network-Wide Outcomes Across Authority Boundaries? — https://arxiv.org/abs/2609.10181
- Belief-State Engine: Augmenting LLMs for Principled Planning Under Partial Observability — https://arxiv.org/abs/2609.10036
