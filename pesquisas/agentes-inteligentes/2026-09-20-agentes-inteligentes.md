# Radar diário — agentes mais inteligentes | 20/09/2026

## Resumo da rodada

Como hoje é domingo, a listagem recente de `cs.AI` no arXiv ainda tem **18/09/2026** como lote mais recente. Em vez de repetir os trabalhos de harness já cobertos ontem, esta rodada destaca quatro trabalhos ainda não explorados no radar e uma repercussão técnica recente de um trabalho especialmente importante para autoevolução de harnesses.

A principal conclusão de hoje é que o AI Hub precisa de uma camada que faça **profiling semântico das trajetórias**, mantenha **estado epistemológico explícito para cada claim/requisito inferido** e trate provenance como parte do retrieval. Isso fecha um ciclo importante: não basta descobrir contexto oculto; precisamos saber **onde o agente gastou esforço, quais inferências tinham evidência suficiente e quais padrões de falha justificam alterar o harness**.

---

## 1. AgentPProf — profiling semântico para descobrir onde o agente realmente está falhando

**Tipo:** pesquisa acadêmica / preprint + implementação aberta.

**Paper:** AgentPProf: Semantic Profiler for Long Horizon AI Agents  
https://arxiv.org/abs/2609.20301

O AgentPProf parte de uma lacuna prática: traces tradicionais ajudam a depurar uma execução individual, mas não conseguem responder bem perguntas como “qual tipo de subproblema consome mais tokens em dezenas de execuções?” ou “onde se concentra a falha de autenticação em várias trajetórias diferentes?”.

A proposta cria uma **semantic operation stack**. Em vez de agrupar apenas por chamadas de ferramenta ou spans técnicos, o sistema segmenta as trajetórias recursivamente por responsabilidade semântica — por exemplo `deploy system > diagnose authentication` — e então agrega tokens, tempo, acesso a arquivos e rede por essas responsabilidades.

Na avaliação, o sistema obteve **0,764 B3 F1** contra anotações humanas no CodeTraceBench, melhorou a localização de problemas em três benchmarks e, em um caso de otimização derivado do profile, reduziu em **19% os tokens** sem degradar a qualidade da tarefa. O trabalho também mostra análise real de long-running coding sessions e leitura de histories locais de Codex e Claude Code.

### Aplicação ao AI Hub

Hoje os traces do AI Hub provavelmente são mais próximos de:

```text
LLM call
shell
read_file
MCP
LLM call
write_file
```

Eu adicionaria uma camada que produz automaticamente algo como:

```text
implement OAuth
  ├─ inspect current auth
  ├─ infer session authority
  ├─ preserve legacy login
  ├─ configure Google provider
  └─ verify regression
```

Cada operação teria métricas agregáveis:

```yaml
semantic_operation:
  path: implement_oauth/inspect_current_auth
  tokens: 18400
  duration_ms: 92100
  tool_calls: 17
  failures: 4
  evidence_found: 2
```

Isso é especialmente importante para **self-improving harnesses**. Em vez de o evolver receber “essa tarefa falhou”, ele pode receber:

```text
62% do custo das falhas OAuth
fica concentrado em
infer_session_authority
```

A mudança do harness fica muito mais localizada e mensurável.

**Limitação:** a segmentação semântica ainda pode errar e depende de um modelo/heurística para detectar limites de subtarefas. O trabalho mede bem profiling e problem localization, mas não prova que usar o profile para autoevolução do harness produzirá ganhos generalizados.

---

## 2. Refuse, Decompose, Refresh — uma inferência deve poder permanecer “não comprovada”

**Tipo:** pesquisa acadêmica / preprint.

**Paper:** Refuse, Decompose, Refresh: A Claim-Safe Protocol for Closed-Loop AI Evaluation  
https://arxiv.org/abs/2609.20538

O paper ataca um problema epistemológico: uma avaliação pode ser perfeitamente reproduzível e mesmo assim sustentar a **claim errada**, porque o próprio sistema escolhe quais estados observar e quais falhas deixam rastro.

A proposta usa três ações:

- **Refuse:** abstém-se quando não há referência/comparação adequada para sustentar a claim.
- **Decompose:** separa execução do protocolo, admissões operacionais e hipóteses estruturais, em vez de condensar tudo em PASS/FAIL.
- **Refresh:** quando ocorre distribution shift, invalida e recalcula a referência em vez de tratar o desvio automaticamente como evidência de falha.

No conjunto held-out preregistrado havia **1.440 casos**; apenas 55 de 72 unidades regime/componente tinham suporte suficiente para admissão de referência. Ou seja, a **abstenção faz parte do resultado**, e não é tratada como defeito do sistema.

### Aplicação ao AI Hub

Isso se encaixa diretamente no `Task Belief State`.

Hoje uma inferência poderia evoluir silenciosamente de:

```text
“parece que o backend controla a sessão”
```

para FACT.

Eu formalizaria:

```yaml
claim:
  statement: backend_controls_session
  state: INFERRED
  support: PARTIAL
  evidence:
    - SessionService.java
  missing_evidence:
    - gateway_auth_flow
    - integration_test
  promotable_to_fact: false
```

E usaria estados como:

```text
UNKNOWN
INFERRED
SUPPORTED
CONFLICTED
STALE
REFUSED
```

O Requirement Discovery Runtime só poderia converter uma inferência em requisito obrigatório quando o `claim contract` estiver satisfeito.

### Mudança importante

O agente deveria ter permissão explícita para dizer:

```text
“não tenho evidência suficiente para decidir isso”
```

sem que o harness interprete isso automaticamente como falha. Isso reduz a tendência de preencher contexto ausente com uma dedução conveniente.

**Limitação:** o estudo usa um simulador agregado e um problema de avaliação fechado; a tradução para software agents é arquitetural, não uma validação direta em coding benchmarks.

---

## 3. TRACE — retrieval com provenance deve ser uma propriedade estrutural, não um detalhe do prompt

**Tipo:** pesquisa acadêmica; aceito no CIKM 2026; protótipo implantado internamente.

**Paper:** TRACE: Accountable Agentic Retrieval for Source Discovery in Digital Archives  
https://arxiv.org/abs/2609.19897

TRACE trabalha com corpora difíceis: documentos heterogêneos, OCR degradado, múltiplas coleções e necessidade de rastrear exatamente a fonte. O framework é training-free e foi implantado para **24 pesquisadores em seis instituições**.

No HistoriQA-ThirdRepublic, com **1.752 questões**, alcançou **R@10 = 0,856** e **MRR = 0,653**, superando baselines sparse, dense, graph-based e agentic RAG; os maiores ganhos ocorreram em questões **multi-hop e cross-corpus**. O custo reportado fica em aproximadamente **US$ 0,02 por pergunta** na configuração hospedada usada pelos autores.

### Aplicação ao AI Hub

Para descoberta de requisitos implícitos, o retrieval frequentemente cruza várias fontes:

```text
prompt do usuário
+ código
+ testes
+ documentação
+ decisões passadas
+ MCP
+ logs
```

Eu faria cada evidência carregada para o Belief State preservar obrigatoriamente:

```yaml
evidence:
  id: ev-1842
  source_type: repo
  source: src/main/java/.../SessionService.java
  revision: commit_sha
  locator: lines 88-117
  retrieval_query: session authority
  retrieved_at: 2026-09-20T...
  trust_state: VERIFIED_SOURCE
```

Assim o agente não apenas “lembra que encontrou algo”. Ele consegue voltar à fonte e revalidá-la.

### Insight para RAG

O padrão que emerge desta semana é:

```text
similaridade semântica
        ↓
não é suficiente
```

Precisamos combinar:

```text
state-conditioned retrieval
+ missing-complement retrieval
+ provenance
+ source integrity/freshness
```

**Limitação:** TRACE foi desenvolvido para arquivos históricos, não repositórios de software. Os bons resultados de retrieval não garantem o mesmo ganho em code retrieval; o principal valor para o AI Hub é o desenho de accountability/provenance.

---

## 4. Not All AI Agents Are Equal — o budget controller deve olhar CPU, disco e memória, não apenas tokens

**Tipo:** pesquisa acadêmica / estudo empírico de sistemas.

**Paper:** Not All AI Agents Are Equal: Characterizing Resource and Performance Dynamics  
https://arxiv.org/abs/2609.19947

O trabalho mede agentes em RAG, web search e coding e mostra que o mesmo tipo de ferramenta pode apresentar gargalos completamente diferentes conforme a tarefa. Mais CPU ou uma resposta de LLM mais rápida **nem sempre** acelera a execução total.

Com `CPU-aware tool admission` e `task-aware CPU allocation`, os autores reportam melhora de cerca de **5,4×** na latência de tarefas sensíveis a CPU e redução média de cerca de **32%** em múltiplas tarefas.

### Aplicação ao AI Hub

Até agora falamos de `Context Budget Controller` principalmente em termos de tokens e custo de API. Eu ampliaria para:

```yaml
execution_budget:
  token_budget: ...
  llm_latency_budget: ...
  local_cpu_budget: ...
  io_budget: ...
  concurrent_tools: ...
  subagent_slots: ...
```

E o `Harness Policy Router` poderia decidir:

```text
repo-heavy task
→ mais I/O slots, menos subagentes paralelos

CPU-heavy analysis
→ limitar concorrência de outras tools

LLM-bound task
→ paralelizar buscas locais durante a espera
```

Isso é particularmente relevante para um AI Hub que roda sandboxes, MCPs e múltiplos agentes localmente: **test-time compute é também scheduling de sistema**, não apenas “usar mais reasoning tokens”.

**Limitação:** o estudo mede performance de infraestrutura, não inteligência ou descoberta de requisitos. A contribuição para este radar é indireta, mas muito prática para a camada de orchestration.

---

## 5. DarwinX — preserve-and-extend em vez de uma única linhagem de harness

**Tipo:** pesquisa acadêmica + framework de engenharia aberto; paper originalmente submetido em julho, mas recebeu forte repercussão técnica nesta semana.

**Paper:** DarwinX: Evolving Agent Harnesses Through Natural Selection  
https://arxiv.org/abs/2608.07545

**Beagle:** https://github.com/SalesforceAIResearch/Beagle

DarwinX não melhora continuamente uma única cópia do harness. Mantém uma **população de variantes**, preserva linhagens alternativas e só promove mudanças que aumentam cobertura sem regredir capacidades já adquiridas (`preserve-and-extend`). O modelo base fica congelado.

Nos resultados reportados:

- Terminal-Bench 2.1: +7,7 pontos para 83,2% no mesmo modelo base.
- WebArena-Infinity: 43,5% → **93,0%** audit-clean pass@1.
- Um harness evoluído em Terminal-Bench transferiu sem alteração para SWE-bench Verified.
- O paper resume o ganho médio do loop em aproximadamente **17 pontos** nos quatro cenários estudados.

### Aplicação ao AI Hub

Até agora discutimos algo mais próximo de:

```text
harness v21
   ↓
patch
   ↓
harness v22
   ↓
patch
   ↓
harness v23
```

Eu passaria a preservar variantes:

```text
                 v21
             /    |    \
         v22a   v22b   v22c
           |       \     |
       OAuth      RAG   verifier
           \       |     /
            candidate merge
                  ↓
                v23
```

Uma versão com score global menor pode possuir a única skill que resolve uma classe rara de problemas. Em vez de descartá-la, o sistema mantém o comportamento no `Harness Capability Archive` e tenta recombiná-lo posteriormente.

### Estrutura possível

```yaml
harness_variant:
  id: hv-221
  parent: hv-198
  model: gpt-5.6-sol

  capabilities_added:
    - detect_missing_acceptance_test

  regressions: []

  evals:
    hidden_requirement_recall: 0.88
    completion_accuracy: 0.91
```

**Limitação:** os números são de benchmarks com verifiers claros; ambientes empresariais reais geralmente têm sinais de sucesso mais imperfeitos. A própria aplicabilidade depende de construir uma suíte de evals confiável.

---

# Arquitetura sugerida depois da rodada de hoje

```text
                        USER REQUEST
                             │
                             ▼
                 REQUIREMENT DISCOVERY
                             │
                             ▼
                      BELIEF / CLAIM STATE
           UNKNOWN / INFERRED / SUPPORTED / STALE
                             │
                             ▼
                    EVIDENCE GAP ENGINE
                             │
                             ▼
                   ACCOUNTABLE RETRIEVAL
             repo / docs / memory / MCP / tests
                             │
                      provenance attached
                             │
                             ▼
                  REQUIREMENT / CONTRACT
                             │
                             ▼
                    HARNESS POLICY ROUTER
         model / tokens / CPU / I/O / risk / skills
                             │
                             ▼
                         EXECUTOR
                             │
                             ▼
                         VERIFIER
                             │
                             ▼
                      TRAJECTORY LOG
                             │
                             ▼
                    SEMANTIC PROFILER
            intent / subtask / tokens / failures
                             │
                             ▼
                    FAILURE ATTRIBUTION
                             │
                             ▼
                   HARNESS EVOLUTION
              population + preserve-and-extend
                             │
                             ▼
                     REGRESSION / EVAL
                             │
                       promote / retain
```

## Prioridade prática para o AI Hub

1. **Semantic Operation Stack / semantic trace segmentation**: transformar traces brutos em responsabilidades estáveis e agregáveis.
2. **Claim State explícito**: impedir que uma hipótese sem evidência vire FACT silenciosamente.
3. **Provenance obrigatória no retrieval**: source, revision, locator, freshness e trust state.
4. **Resource-aware Harness Policy Router**: tokens + CPU + I/O + concorrência + subagents.
5. **Harness Capability Archive**: preservar variantes/skills úteis mesmo quando uma versão não é promovida integralmente.

A ideia central da rodada é que a autoevolução fica muito mais segura quando o sistema consegue responder três perguntas antes de alterar o harness:

```text
onde exatamente ele falhou?
qual claim estava sem evidência suficiente?
qual comportamento já funcionava e não pode regredir?
```

Sem essas três respostas, self-improvement tende a virar edição de prompt por tentativa e erro. Com elas, começa a parecer engenharia experimental de verdade.

## Fontes

- arXiv cs.AI recent (lote mais recente em 18/09/2026): https://arxiv.org/list/cs.AI/recent
- AgentPProf: https://arxiv.org/abs/2609.20301
- Refuse, Decompose, Refresh: https://arxiv.org/abs/2609.20538
- TRACE: https://arxiv.org/abs/2609.19897
- Not All AI Agents Are Equal: https://arxiv.org/abs/2609.19947
- DarwinX: https://arxiv.org/abs/2608.07545
- Beagle: https://github.com/SalesforceAIResearch/Beagle
