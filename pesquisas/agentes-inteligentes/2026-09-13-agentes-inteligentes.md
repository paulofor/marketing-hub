# Radar diário — agentes mais inteligentes | 13/09/2026

Hoje não encontrei uma nova leva de papers submetidos no fim de semana que justificasse preencher o radar com material marginal. A rodada, portanto, usa a leva mais recente de 10–11 de setembro e acrescenta dois trabalhos de requirements engineering que ainda não tinham entrado no radar, mas são especialmente próximos da pergunta original: **como fazer o agente perceber que o prompt está incompleto, descobrir o que falta e só então executar**.

A conclusão mais forte de hoje é que o AI Hub deveria tratar **suficiência de contexto** como um estado verificável do harness. Não basta fazer RAG ou chamar MCP; o sistema precisa decidir explicitamente se já possui informação suficiente para agir. Quando não possui, deve identificar o tipo de lacuna, escolher a próxima fonte e, se necessário, formular uma pergunta específica ao usuário. Além disso, requisitos críticos e limites de autoridade não deveriam ser removíveis pela compactação de contexto.

## 1. RCL: detectar estruturalmente que o contexto recuperado ainda é insuficiente

**Tipo:** pesquisa acadêmica / preprint  
**Submetido:** 10/09/2026  
**Fonte:** https://arxiv.org/abs/2609.11023

O trabalho **RCL: A Retrieval-Confidence Layer for Detecting Insufficient Context in Enterprise Retrieval-Augmented Code Generation** é provavelmente o achado mais diretamente aplicável ao AI Hub nesta rodada, inclusive porque o benchmark proposto usa **repositórios Java** com APIs internas sintéticas.

O problema identificado é importante: em código público, o modelo frequentemente compensa um retrieval ruim usando conhecimento paramétrico. Em código empresarial isso deixa de funcionar, porque APIs privadas, convenções internas e frameworks próprios simplesmente não existem no pré-treinamento.

O RCL introduz uma camada entre retrieval e geração:

```text
USER REQUEST
     │
     ▼
RETRIEVAL
     │
     ▼
RETRIEVAL-CONFIDENCE LAYER
     │
     ├── structural coverage
     ├── novelty / private-knowledge dependence
     └── sufficiency score
             │
       ┌─────┴─────┐
       │           │
   sufficient   insufficient
       │           │
    generate    targeted retrieval
                    │
                    └── or human review
```

A ideia mais interessante é que a suficiência não depende apenas de similaridade semântica nem da confiança verbalizada pelo próprio LLM. O método usa **cobertura estrutural derivada do call graph** e uma medida de novidade para estimar se a tarefa depende de conhecimento que provavelmente está fora da memória paramétrica do modelo.

O abstract público informa que os autores comparam RCL com retrieval baseado apenas em similaridade na correção da geração, mas não expõe os números da Seção 7. Por isso eu trataria o trabalho, neste momento, como **mecanismo arquitetural fortemente relevante**, e não como evidência quantitativa definitiva.

### Aplicação ao AI Hub

Eu criaria um componente explícito chamado **Context Sufficiency Gate**:

```yaml
context_gate:
  task: add_google_oauth

  required_context_classes:
    - current_auth_architecture
    - session_ownership
    - regression_tests
    - provider_configuration

  evidence_found:
    current_auth_architecture: true
    session_ownership: true
    regression_tests: false
    provider_configuration: partial

  sufficiency: 0.62
  decision: RETRIEVE_MORE
```

A diferença em relação a um RAG convencional é grande. O RAG responde:

> "Encontrei documentos parecidos?"

O gate responde:

> "Encontrei **as classes de evidência necessárias** para tomar esta decisão?"

Isso é muito mais próximo da inteligência que estamos tentando construir.

---

## 2. ReqEvolve: transformar pedido de usuário em especificação, testes e mudança executável

**Tipo:** pesquisa acadêmica / ASE 2026  
**Preprint:** arXiv 2609.10590  
**Fonte:** https://arxiv.org/abs/2609.10590  
**Conferência:** https://conf.researchr.org/details/ase-2026/ase-2026-research-track/86/ReqEvolve-User-Oriented-Software-Self-Evolution-through-Automatic-Requirement-Interp

O **ReqEvolve: User-Oriented Software Self-Evolution through Automatic Requirement Interpretation** é praticamente uma implementação experimental da ideia de colocar um compilador de requisitos antes do coding agent.

O sistema recebe solicitações de alto nível, não especificações técnicas completas, e aplica uma sequência de:

```text
user request
    ↓
clarification
    ↓
specification decomposition
    ↓
test generation
    ↓
implementation
    ↓
runtime integration
```

Em **72 casos de evolução de software distribuídos por 18 projetos**, ReqEvolve atingiu **89,2% Pass@1**, superando o baseline SpecFix em **18,8 pontos percentuais** e a versão de ablação em **32,6 pontos**. Os efeitos reportados foram grandes e estatisticamente significativos.

Isso é uma das evidências mais concretas que encontramos até agora de que **interpretar e estruturar requisitos antes de gerar código** pode produzir ganho substancial.

### Aplicação ao AI Hub

Nosso `Requirement Compiler` poderia deixar de ser apenas um prompt e se tornar uma pipeline com artefatos intermediários persistentes:

```yaml
requirement_compilation:

  raw_request:
    "adicione login Google"

  explicit:
    - google_authentication

  inferred:
    - preserve_existing_authentication
    - protect_secrets

  unresolved:
    - session_authority
    - refresh_token_policy

  decomposed_spec:
    - provider_registration
    - callback_flow
    - session_binding
    - regression_preservation

  acceptance_tests:
    - OAuthCallbackIT
    - ExistingLoginRegressionIT
```

O ponto-chave é: **os testes nascem da especificação interpretada, não depois que o executor já escolheu uma solução**.

### Limitação

O benchmark tem 72 casos, portanto ainda é pequeno comparado com SWE-bench e ambientes industriais extensos. Além disso, Pass@1 mede o resultado final; precisamos de métricas adicionais para saber se os requisitos inferidos são realmente os corretos. Mesmo assim, o resultado é altamente relevante para a arquitetura do AI Hub.

---

## 3. C-GRiD: usar contraexemplos para descobrir requisitos que ninguém escreveu

**Tipo:** pesquisa acadêmica / ASE 2026 + replication package  
**Fonte da conferência:** https://conf.researchr.org/details/ase-2026/ase-2026-research-track/49/Neuro-symbolic-Requirements-Elicitation-Utilizing-Formal-Verification-Counterexample  
**Artefato:** https://zenodo.org/records/19222714

O trabalho **Neuro-symbolic Requirements Elicitation: Utilizing Formal Verification Counterexamples as Contextual Prompts**, cujo framework é chamado **C-GRiD**, acrescenta uma ideia particularmente poderosa: em vez de pedir ao LLM genericamente para "pensar no que está faltando", o sistema cria uma especificação formal, executa um model checker e usa **contraexemplos concretos** para revelar restrições ausentes.

O ciclo é:

```text
incomplete requirements
       ↓
LLM builds candidate specification
       ↓
formal model checker
       ↓
counterexample
       ↓
translate counterexample into targeted question
       ↓
stakeholder/oracle answer
       ↓
update specification
       ↺
```

O pacote de replicação usa dez sistemas em seis domínios e uma técnica muito interessante chamada **specification lobotomy**: remove deliberadamente restrições da especificação correta para simular requisitos incompletos.

Com oracle perfeito, C-GRiD alcançou **79,2% de recall e 90,0% de precisão** na recuperação das restrições faltantes. O recall ficou **65 pontos percentuais acima do melhor baseline one-shot**. Mesmo simulando um oracle com **25% de erros**, o recall caiu apenas para **70,8%**.

### Aplicação ao AI Hub

Isso sugere que a descoberta de requisitos não precisa ocorrer apenas antes da execução. Ela pode continuar durante **testes e simulação**.

Exemplo:

```text
Requirement Compiler
      ↓
provisional specification
      ↓
run tests / static analysis / architecture checks
      ↓
COUNTEREXAMPLE
"OAuth implementation invalidates password-login path"
      ↓
Missing Requirement Detector
      ↓
"Existing password authentication must remain operational"
```

Portanto, eu adicionaria ao harness um **Counterexample-to-Requirement Loop**.

Isso também fornece uma ótima maneira de criar o benchmark do próprio AI Hub: pegar uma tarefa completa conhecida, remover deliberadamente uma ou mais restrições e medir se o sistema consegue redescobri-las por inspeção, testes e interação.

### Limitação

C-GRiD depende de especificações formalizáveis e de um model checker. Nem todo requisito de produto cabe em TLA+ ou outra lógica formal. Para o AI Hub, eu generalizaria "counterexample" para qualquer evidência de conflito: teste que falhou, constraint violation, static-analysis finding, runtime observation ou inconsistência documental.

---

## 4. Fortunate Recall: memória precisa saber quando uma informação foi substituída

**Tipo:** pesquisa acadêmica / preprint  
**Submetido:** 09/09/2026  
**Fonte:** https://arxiv.org/abs/2609.10413

O **Fortunate Recall** ataca um problema que cresce quando o agente passa a descobrir contexto automaticamente: a memória encontra não apenas a informação correta, mas também versões antigas, contraditórias ou temporariamente válidas.

O sistema adiciona uma camada de políticas de ciclo de vida com mecanismos como:

```text
temporal decay
slot-key supersession
event-time validity
category-aware retrieval
```

No LifecycleBench, com 516 questões de desambiguação temporal, o FR-Bank chegou a **76,9%**, contra **61%–70,5%** dos sistemas comparados. No LongMemEval-S obteve 75,2%.

O resultado mais interessante, porém, é a ablação. Substituir a ontologia detalhada por três primitivas genéricas de lifecycle praticamente não alterou a correção (-1,7 pp; IC 95% de -6,0 a +2,7). A ontologia ajudou principalmente na **calibração**, reduzindo a confabulação downstream de **24,2% para 12,0%**.

Isso produz uma recomendação muito prática: eu **não começaria criando dezenas de tipos sofisticados de memória**. Primeiro implementaria três ou quatro propriedades fundamentais:

```yaml
memory:
  valid_from: ...
  valid_until: ...
  supersedes: ...
  scope: ...
  source: ...
```

Depois, se houver benefício medido, acrescentaria ontologias mais ricas.

### Aplicação ao AI Hub

Quando você disser hoje:

> "agora a sessão será controlada pelo backend"

uma memória anterior não deve desaparecer, mas precisa ficar claramente marcada:

```yaml
statement: frontend_controls_session
status: superseded
superseded_by: decision-2026-09-13-04
```

O Context Router deveria preferir a decisão vigente e recuperar a antiga apenas quando precisar explicar histórico ou migração.

---

## 5. The Missing Boundary: certas restrições nunca deveriam ser compactadas para fora do contexto

**Tipo:** pesquisa acadêmica / preprint de segurança de agentes  
**Submetido:** 10/09/2026  
**Fonte:** https://arxiv.org/abs/2609.11024

**The Missing Boundary: How Autonomous Agents Lose Control** traz um resultado especialmente importante para agentes long-running. Em 1.800 trajetórias distribuídas por cinco modelos e 16 domínios, os autores manipulam separadamente pressão de objetivo, degradação do limite de controle e disponibilidade de uma ação insegura.

Quando o limite de controle estava degradado e uma ação indevida estava disponível, a taxa de perda de controle chegou a **55%** no estudo fatorial e **62%** em dez domínios adicionais. Restaurar o limite de controle reduziu o problema a **0%**, mesmo mantendo a ação insegura disponível.

O resultado mais relevante para context engineering aparece na ablação de compaction: **compactar contexto não causou o problema por si só**. Quando as restrições de controle eram preservadas, a perda de controle permaneceu em **0%**; quando eram omitidas, chegou a **87%**.

### Aplicação ao AI Hub

Eu dividiria contexto em duas classes:

```text
EVICTABLE / COMPACTABLE
──────────────────────
conversation detail
old observations
tool transcripts
intermediate reasoning

NON-EVICTABLE CONTROL ENVELOPE
──────────────────────────────
current goal
hard constraints
authority / permissions
protected requirements
completion criteria
safety boundaries
```

O segundo grupo não deveria depender de resumos de conversa. Ele deveria ser reinjetado deterministicamente em cada ciclo relevante do executor.

Essa é uma peça importante para nossa arquitetura porque um agente que "descobre o que você não disse" ficará mais autônomo. Quanto maior essa autonomia, mais importante é que **as restrições críticas não possam desaparecer por acidente durante compaction**.

---

# Arquitetura resultante da rodada

Os trabalhos de hoje sugerem uma mudança clara: entre `Context Router` e `Planner` deve existir um controlador de suficiência, e a descoberta de requisitos precisa continuar após o planejamento através de contraexemplos.

```text
                         USER REQUEST
                              │
                              ▼
                    REQUIREMENT COMPILER
                 explicit / inferred / unknown
                              │
                              ▼
                   CONTEXT DISCOVERY ENGINE
                  repo / MCP / memory / docs
                              │
                              ▼
                   CONTEXT SUFFICIENCY GATE
                  "já sei o bastante para agir?"
                         │             │
                        yes            no
                         │             │
                         │       gap classification
                         │        ┌────┼────┐
                         │       repo MCP  user
                         │        └────┼────┘
                         │             │
                         └───────┬─────┘
                                 ▼
                      NON-EVICTABLE CONTROL
                 goal / constraints / authority
                                 │
                                 ▼
                              PLAN
                                 │
                                 ▼
                             EXECUTE
                                 │
                                 ▼
                 TEST / VERIFY / MODEL CHECK
                                 │
                          counterexample?
                         ┌───────┴───────┐
                        yes              no
                         │                │
              Missing Requirement        │
                   Detector              │
                         │                │
                         └──────► update spec
                                          │
                                          ▼
                                   completion gate
                                          │
                                          ▼
                                  lifecycle-aware
                                       memory
```

## O que eu priorizaria agora no AI Hub

A prioridade prática que emerge desta rodada é:

1. **Context Sufficiency Gate** — o agente deve saber quando ainda falta contexto.
2. **Requirement Compiler com artefato estruturado** — não apenas prompt de planejamento.
3. **Counterexample-to-Requirement Loop** — testes e verificadores devem descobrir requisitos ausentes.
4. **Non-Evictable Control Envelope** — goal, authority e restrições críticas sobrevivem a qualquer compaction.
5. **Lifecycle-aware memory** — decisões podem expirar ou ser substituídas sem apagar o histórico.

O insight mais importante é este:

> **A capacidade de inferir requisitos implícitos não deve ser tratada como uma única chamada inteligente ao LLM. Deve ser um loop de controle: detectar lacuna → buscar evidência → medir suficiência → formular hipótese → executar/verificar → usar contraexemplos para descobrir novas lacunas.**

Isso é uma mudança relevante em relação ao desenho inicial do radar. No começo estávamos pensando principalmente em um `Requirement Compiler`. Agora a arquitetura parece exigir algo maior: um **Requirement Discovery Runtime** que permanece ativo durante toda a tarefa.