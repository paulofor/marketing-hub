# Radar diário — agentes mais inteligentes | 19/09/2026

A rodada de hoje trouxe cinco trabalhos especialmente úteis para o objetivo deste radar. O mais diretamente alinhado à pergunta original — como fazer o agente perceber o que ainda falta saber — é **The Missing Complement**. Ele muda retrieval de “buscar trechos relevantes” para **recuperar o conjunto mínimo de evidências que ainda falta para a próxima decisão**. Os demais trabalhos completam o desenho: um substrato persistente para tarefas de dias/semanas, uma forma mais barata de evoluir o próprio harness, uma camada estrutural para impedir alucinação de ferramentas/MCP e um resultado mostrando que uma restrição escrita no prompt só se torna confiável quando o harness a transforma em parte do planejamento e da verificação.

## 1. The Missing Complement — o retrieval deve perguntar “o que ainda falta para decidir?”

**Tipo:** pesquisa acadêmica / preprint (UC San Diego), submetido em 17/09/2026.

O paper formula **state-conditioned minimal sufficient evidence recovery**. A ideia é simples e muito forte: um agente no meio de uma tarefa já viu muita coisa. Um retriever tradicional continua ranqueando documentos pela semelhança com o issue/prompt e pode gastar o orçamento trazendo variações de uma evidência que o agente já conhece, enquanto deixa faltar outra informação necessária para a decisão atual.

O SERBench captura exatamente esse cenário em **500 estados de decisão de 45 repositórios**. Cada estado registra o que o agente já viu e quais grupos de evidência ainda são necessários. O método MSS-Complement trata retrieval como construção de conjunto: propõe um conjunto, procura explicitamente o que está faltando e entrega 4–8 unidades de fonte intactas dentro de 6.144 tokens.

Com cinco itens, recuperou um conjunto completo de evidências em **73,0%** dos estados contra **61,4%** do baseline Qwen3 embedding + reranking. Com oito itens, ficou em **80,6% contra 72,4%**. O ganho foi maior justamente quando a decisão dependia de várias evidências simultaneamente. Remover apenas um grupo necessário de um conjunto completo reduziu de forma forte a precisão de localização de reparos, mostrando que “quase todo o contexto” ainda pode ser insuficiente.

Aplicação direta ao AI Hub: o `Context Sufficiency Gate` deveria trabalhar sobre um **Evidence Gap State**, não apenas sobre scores de relevância.

```text
Task Belief State
      │
      ▼
NEXT DECISION
      │
      ├─ já sei A
      ├─ já sei B
      └─ falta C + D
              │
              ▼
      COMPLEMENT RETRIEVAL
        repo / MCP / memory
              │
              ▼
   jointly sufficient evidence
```

Uma representação prática poderia ser:

```yaml
decision_state:
  subgoal: preserve_existing_authentication

  established:
    - current_auth_provider
    - session_owner

  unresolved_evidence:
    - regression_behavior
    - callback_constraints

  completion_rule:
    require_all_groups: true
```

Isso é melhor do que “RAG com query melhor”. É o harness mantendo explicitamente **o que a decisão ainda não consegue provar**.

**Limitação:** o benchmark congela estados de coding agents e possui certificados anotados de suficiência; em produção o AI Hub terá de estimar esses grupos de evidência sem possuir o gabarito. Ainda assim, o paper fornece uma métrica excelente para construirmos nosso próprio benchmark de requisitos ocultos.

Fonte: https://arxiv.org/abs/2609.20050

## 2. Levels, Ticks and Cascaded Intelligence — primeiro sobreviver aos resets; depois aprender

**Tipo:** pesquisa acadêmica / arquitetura experimental (Salesforce AI Research), submetido em 17/09/2026.

**An Architecture for Long-Horizon Agents: Levels, Ticks and Cascaded Intelligence** parte de uma observação importante: tarefas de dias ou semanas sobrevivem a qualquer context window, processo e período de atenção humana. Os autores defendem que **continual operation without forgetting deve vir antes de continual learning**.

A arquitetura usa níveis por escala de tempo. Cada nível mantém um **arquivo limitado** que resume o nível inferior; diretivas descem e resumos/escalations sobem. O `tick` é a unidade de autonomia: acorda, lê o estado persistente, aplica regras, decide, age e escreve o checkpoint antes de encerrar. Trabalho só escala para um modelo mais capaz depois que revisão no nível atual falha.

Em uma campanha de **10 dias**, o agente reproduziu um resultado publicado de reinforcement learning com atenção humana apenas uma vez por dia, preservando a tarefa através de resets de contexto e sessões. Conhecimento operacional escrito cedo nesses arquivos modificou comportamento posterior sem alterar pesos do modelo.

Isto responde também à nossa discussão de armazenamento: neste trabalho, a peça central do harness é deliberadamente **estado durável em arquivos**, não uma memória escondida dentro do contexto do LLM. O paper descreve protocolos como arquivos nomeados com writer, reader, cadence e checks.

Para o AI Hub eu usaria o princípio, mas não copiaria literalmente a tecnologia. O equivalente poderia ser:

```text
L0  tool/action trace        → artifact/log storage
L1  current execution        → MySQL task_state
L2  current subgoal          → MySQL belief/plan state
L3  daily/project summary    → durable document
L4  project strategy         → versioned policy/docs
```

O ponto não é “arquivo versus banco”. É: **cada camada temporal precisa de um estado externo, limitado e reconstituível que sobreviva à sessão**. Para o seu stack, MySQL pode cumprir parte desse papel e Git/arquivos podem cumprir a parte legível/versionada.

**Limitação:** é uma demonstração profunda, mas pequena: uma campanha de dez dias, não um benchmark amplo de agentes empresariais. O principal valor hoje é arquitetural.

Fonte: https://arxiv.org/abs/2609.19519

## 3. SIFT — autoevolução do harness precisa de um funil barato antes das evals caras

**Tipo:** pesquisa acadêmica / preprint (MIT + Sakana AI), submetido em 17/09/2026.

**Self Improvement via Fast Tree-search (SIFT)** ataca um gargalo que será inevitável se o AI Hub começar a propor mudanças no próprio harness: testar cada modificação candidata em uma suíte grande é caro demais. O sistema usa comparações pareadas por um LLM judge como sinal barato; agrega as vitórias/derrotas com Bradley–Terry e usa o ranking para decidir quais candidatos merecem avaliações completas.

No Polyglot, uma configuração com Qwen3-Coder-30B chegou a **31,1%**, versus 20,0% do agente base, 27,1% do DGM e 30,5% do HGM; outra configuração com judge GPT-5.4 chegou a 32,0%. Com o3-mini + GPT-5.4 judge, chegou a **35,1%** contra 30,7% do DGM. Uma busca Qwen reportada custou cerca de **US$34,3**, 224 CPU-h e 6,7h de wall clock; os autores afirmam aproximadamente um décimo do CPU usado pelo baseline DGM naquela comparação.

O achado que eu levaria para o AI Hub não é “use Bradley–Terry”. É separar:

```text
candidate harness patch
        │
        ▼
CHEAP SCREENING
static review / pairwise judge / targeted probe
        │
        ▼
only promising candidates
        │
        ▼
EXPENSIVE EVAL
hidden-requirement benchmark
regression suite
held-out tasks
```

Assim, um `Harness Evolver` pode explorar dezenas de mudanças sem executar toda a suíte em todas elas.

O próprio paper oferece uma advertência importante: o judge forte é mais capaz que alguns coding backbones; uma única nota escalar pode esconder trade-offs específicos por skill; e candidatos podem tentar relaxar timeout, retries ou até mexer no próprio mecanismo de avaliação. Os autores usam sandbox e allow-list de arquivos editáveis. Para o AI Hub, eu manteria **evaluation code, contracts e benchmark fora do conjunto que o auto-improver pode escrever**.

Fonte: https://arxiv.org/abs/2609.19526

## 4. Closed-World Resolution — MCP precisa resolver `(server, tool, schema)` antes de qualquer gate

**Tipo:** pesquisa acadêmica / estudo de medição + benchmark, submetido em 16/09/2026 e aparecendo na leva recente.

**Closed-World Resolution Against Tool Hallucination in LLM Agents** mede uma falha estrutural: agentes chamam ferramentas que não existem ou usam argumentos que o schema não declara. Isso não é resolvido apenas por tool routing ou por policy gates, porque esses mecanismos normalmente assumem que a chamada já corresponde a uma ferramenta válida.

Em dez modelos, o estudo encontrou **322 alucinações** em superfície de ferramenta convencional. Na extensão MCP, onde vários servidores são fundidos num namespace, mediu **154 alucinações** envolvendo fabricação cross-server, colisões de namespace, shadowing, definições stale e empréstimo de assinatura. No benchmark MCP descrito pelos autores, o resolver fechado rejeitou as 154 emissões medidas antes que chegassem à execução.

Para o AI Hub, eu colocaria uma camada estrutural entre o LLM e o MCP:

```text
LLM proposes tool call
        │
        ▼
RESOLUTION RUNG
  server exists?
  tool exists in that server?
  definition current?
  namespace ambiguous?
  arguments match schema?
        │
        ▼
POLICY / AUTHORITY GATE
        │
        ▼
EXECUTION
        │
        ▼
EFFECT VERIFICATION
```

Eu registraria cada tool por identidade composta e versão:

```yaml
tool_ref:
  server: marketing-hub-db
  tool: query
  schema_version: 7
  manifest_hash: ...
```

Isto é especialmente importante se seu AI Hub agregar vários MCP Servers. O modelo não deveria poder resolver nomes “por aproximação”.

**Limitação:** a garantia depende de um registry confiável; se o próprio registry estiver errado ou stale, o resolver não descobre isso. Além disso, uma chamada semanticamente errada mas estruturalmente válida ainda exige reasoning/verifier posterior.

Fonte: https://arxiv.org/abs/2609.19425

## 5. SafeHarness — uma restrição entendida pelo modelo pode continuar sendo ignorada no plano

**Tipo:** pesquisa acadêmica / preprint de robótica, submetido em 17/09/2026.

**Coding Agents with an Obstacle-Aware Harness for Safe Robot Manipulation** não é sobre software requirements, mas contém talvez a demonstração mais clara desta rodada de uma distinção crucial. O modelo recebia explicitamente a regra de não tocar no obstáculo e **raciocinava sobre o obstáculo nos traces**, porém continuava colidindo. O problema não era percepção nem ausência da instrução; a restrição simplesmente não virava prioridade operacional no planejamento.

O SafeHarness coloca a restrição onde a decisão ocorre: propõe rota, verifica externamente, replana se necessário e só então executa; também escolhe a geometria do contato considerando o obstáculo. Com GPT-6 no SafeLIBERO, reporta **71,9% de task success e 87,5% de collision avoidance**, contra 31,0% e 59,0% do mesmo agente com skills mas sem os dois harnesses especializados.

A tradução para requisitos implícitos do AI Hub é muito forte:

```text
Requirement discovered
      ↓
written into prompt/context
      ↓
            NÃO BASTA

Requirement discovered
      ↓
attached to affected decision
      ↓
precondition / contract / verifier
      ↓
plan cannot advance without satisfying it
```

Por exemplo, `preserve_existing_login=true` não deveria existir apenas como frase no Task Belief State. Ele deveria produzir algo operacional, como `ExistingLoginRegressionIT must pass before completion` ou um gate que impeça alterações incompatíveis.

**Limitação:** o domínio é robótica física e a restrição é geométrica, muito mais verificável que muitos requisitos de produto. A extrapolação para coding agents é arquitetural, não evidência direta.

Fonte: https://arxiv.org/abs/2609.20822

## O que eu mudaria agora no AI Hub

A novidade mais forte de hoje é que o `Requirement Discovery Runtime` precisa controlar não só **o que sabemos**, mas também **o que ainda falta para a próxima decisão**. Eu evoluiria o desenho para:

```text
                         USER REQUEST
                              │
                              ▼
                 INTENT / REQUIREMENT STATE
                              │
                              ▼
                       TASK BELIEF STATE
                              │
                              ▼
                     NEXT DECISION STATE
                              │
                              ▼
                    EVIDENCE GAP BUILDER
                   "o que ainda falta provar?"
                              │
                              ▼
                 COMPLEMENT RETRIEVAL ROUTER
               repo / MCP / memory / experiments
                              │
                              ▼
                    SUFFICIENCY CERTIFICATE
                              │
                              ▼
                         TASK PLANNER
                              │
                              ▼
               REQUIREMENT ENFORCEMENT GATES
                              │
                              ▼
                           EXECUTOR
                              │
                              ▼
                         VERIFIER
                              │
                              ▼
                    DURABLE TASK STATE
                              │
                              ▼
                 HARNESS IMPROVEMENT LOOP
               cheap screening → held-out eval
```

Minha prioridade de implementação seria: **(1)** `Evidence Gap / Missing Complement` por decisão; **(2)** identidade fechada e versionada para MCP tools; **(3)** requisito confirmado → contract/test/gate, não apenas texto; **(4)** estado persistente que sobreviva a sessões/compaction; **(5)** somente depois, autoevolução do harness com screening barato e evals protegidas.

O principal insight da rodada é:

> Um agente mais inteligente não é o que recupera mais contexto. É o que sabe qual evidência ainda falta para a decisão atual, procura exatamente esse complemento e transforma as restrições confirmadas em mecanismos que não podem ser silenciosamente esquecidos durante a execução.

## Fontes principais

- Feng et al., *The Missing Complement: State-Conditioned Minimal Sufficient Evidence for Coding Agents*: https://arxiv.org/abs/2609.20050
- Nijkamp et al., *An Architecture for Long-Horizon Agents: Levels, Ticks and Cascaded Intelligence*: https://arxiv.org/abs/2609.19519
- Fu et al., *Self Improvement via Fast Tree-search*: https://arxiv.org/abs/2609.19526
- Iyer, *Closed-World Resolution Against Tool Hallucination in LLM Agents*: https://arxiv.org/abs/2609.19425
- Xu et al., *Coding Agents with an Obstacle-Aware Harness for Safe Robot Manipulation*: https://arxiv.org/abs/2609.20822
