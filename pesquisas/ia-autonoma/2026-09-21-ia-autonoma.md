# Radar IA autônoma — 2026-09-21

**Rodada:** 18:29 BRT (America/Sao_Paulo)

Nesta rodada, dois casos novos para este radar passam o filtro de relevância: **Designer-RSI** e **EvoOntology**. O primeiro é especialmente forte por aprender skills a partir de tráfego real de usuários sem atualizar pesos nem usar rótulos humanos; o segundo mostra uma camada semântica exposta via MCP que é versionada e refinada com base em trajetórias de execução. Também registro uma atualização oficial da OpenAI como marcador de fronteira: em 21/09/2026, a empresa afirmou que RSI totalmente autônoma ainda não está acontecendo hoje.

## Classificação

| Caso | (1) Pesos persistentes | (2) Código/scaffold/harness | (3) Prompts/retrieval/workflows/tools/MCP/skills/estratégias | (4) Só memória/contexto | (5) Otimização fortemente conduzida por humanos |
|---|---:|---:|---:|---:|---:|
| Designer-RSI | Não | Não como alvo principal | **Sim — skills procedurais** | Não | Parcial |
| EvoOntology | Não | Não no core Python | **Sim — ontology/schema/tool layer via MCP** | Não | Parcial |

---

## 1. Designer-RSI — skills aprendidas de tráfego real, com modelo congelado

**Fonte principal:** https://arxiv.org/abs/2609.22086

O Designer-RSI opera um agente de design profissional que controla mais de 230 ferramentas equivalentes a Photoshop, Illustrator e InDesign. O modelo de base, as ferramentas, o renderer, o evaluator e os papéis de evolução permanecem congelados; **somente a biblioteca de `SKILL.md` muda**.

A evolução ocorre em duas direções:

- **Widening:** detecta subtarefas recorrentes do tráfego que ainda não possuem skill e cria novas skills.
- **Deepening:** identifica skills associadas repetidamente a falhas e as reescreve contrastando execuções ruins com execuções bem-sucedidas da mesma skill.

As mudanças não são aceitas automaticamente. Um **matched replay gate** congela o contexto upstream e compara Candidate vs Incumbent no mesmo conjunto de contextos. Uma mudança só é promovida se corrigir pelo menos um caso e não houver regressão detectada nos casos replayados.

### O que persiste entre execuções

A biblioteca procedural de skills. Em cinco rodadas ela cresce de **76 para 139 skills**. O sistema usa 1.406 briefs de usuários reais e variantes aumentadas, produzindo 1.869 trajetórias avaliadas automaticamente, sem rótulos humanos e sem atualizar pesos.

### Métricas

- GenEval2 com Claude-Sonnet-4: execução bem-sucedida sobe de **72,7% para 99,3%**.
- Qualidade de geração no mesmo benchmark: **+11,99 pontos**.
- Em quatro benchmarks especializados de design, a biblioteca evoluída obtém win rate de **61,8%** com Claude-Sonnet-4 e **67,6%** com Claude-Opus-4.6 contra o mesmo agente sem skills.
- Em 200 briefs held-out do tráfego, widening sozinho chega a 49,4% de win rate, deepening sozinho a 48,6%, e os dois juntos a **58,5%** (p=0,025).
- O gate rejeita **100 de 231** propostas de rewrite e **67 de 136** candidatas de novas skills, mostrando que boa parte das auto-modificações propostas não é segura o suficiente para promoção.

### Limitações

A evolução não é monotônica: uma rodada intermediária (R4) regrediu em parte dos thresholds de completude antes de R5 recuperar desempenho. Isso reforça que `versão mais nova = versão melhor` é uma suposição perigosa.

O Reflector, o Grader e a lógica do gate não aprendem; portanto, isto não é RSI forte. É categoria **(3)**: evolução persistente de skills ao redor de pesos congelados.

### Padrão arquitetural reutilizável

```text
tráfego real
   ↓
trajetórias + outcomes
   ↓
coverage gaps / failure attribution
   ↓
WIDEN: nova skill
ou
DEEPEN: rewrite de skill
   ↓
matched replay
   ↓
no-regression gate
   ↓
PROMOTE / DISCARD
```

O detalhe mais útil é separar **criação de cobertura** de **endurecimento de confiabilidade**. Novas skills aumentam alcance; rewrites subsequentes tornam esse alcance mais estável.

---

## 2. EvoOntology — a camada semântica do agente vira estado treinável exposto por MCP

**Paper:** https://arxiv.org/abs/2609.15779  
**Código/documentação:** https://github.com/ruc-datalab/EvoOntology

O EvoOntology trata a camada semântica de um Data Agent como **estado treinável externo aos pesos**. Essa camada é servida por MCP e contém três partes interligadas:

- **Content Layer:** termos, mappings, constraints, evidence e relações semânticas.
- **Schema Layer:** define quais tipos de objetos e relações a ontologia consegue representar.
- **Tool Layer:** expõe a ontologia ao agente por ferramentas como `browse_semantics` e `resolve_semantics`.

O ciclo é:

```text
Build ontology_v0
   ↓
Use em tarefas reais
   ↓
registrar trajetórias
   ↓
diagnóstico + attribution
   ↓
patch localizado em Content / Schema / Tool
   ↓
Parent vs Candidate com mesmo orçamento/contexto
   ↓
Publish ontology_vN+1 ou Reject
```

A mudança é persistente e versionada, mas **não altera pesos do LLM** e não reescreve o core Python do runtime.

### Métricas

No subconjunto de quatro backbones reportado pelo projeto:

| Benchmark | ReAct sem ontologia | Ontologia inicial | EvoOntology |
|---|---:|---:|---:|
| DDR-Bench (10-K), Trajectory-Wise | 69,5 | 81,8 | **89,5** |
| InsightBench, Insight | 53,2 | 54,0 | **54,2** |
| BIRD, Execution Accuracy | 63,6 | 68,7 | **72,4** |

O ponto importante é separar o ganho da construção inicial do ganho da **auto-evolução**. No DDR-Bench, por exemplo, a ontologia inicial já sobe 69,5→81,8; depois a evolução baseada nas trajetórias leva 81,8→89,5.

### O que persiste entre execuções

O workspace versionado `ontology_vN`, as trajetórias, avaliações e checkpoints. A implementação atual usa um diretório `.evoontology/` com JSON/JSONL:

```text
.evoontology/
├── project.json
├── active.json
├── versions/
├── trajectories/
├── evolution/
│   └── run_N/
│       ├── run.json
│       ├── trajectory-sources.json
│       ├── rounds.jsonl
│       └── evaluations/
└── state.json
```

Isso é particularmente relevante para arquitetura de agentes: mostra que o estado evolutivo pode viver **fora dos pesos e fora do core do runtime**, ser carregado dinamicamente e trocar de versão sem trocar o modelo.

### Intervenção humana e limitação importante

A versão atual do plugin **não faz evolução totalmente unattended**. O comando `/evo-evolve` dispara a rodada; o fluxo também prevê confirmação de orçamento e das fontes de trajetória. A documentação diz explicitamente que evolução automática sem supervisão exigiria um worker de background e ainda não faz parte desta versão.

Portanto, eu classifico EvoOntology como **(3) + parcial (5)**: o objeto evolui automaticamente dentro da rodada, mas o ciclo operacional ainda tem gatilho/supervisão humana.

### Padrão arquitetural reutilizável

A ideia mais forte é tratar a camada semântica como um componente independente do agente:

```text
Agent Core
   │
   ├── MCP → Semantic State v17
   │              ↓
   │          trajectories
   │              ↓
   │          candidate v18
   │              ↓
   │          paired eval
   │              ↓
   └────────── active = v18
```

Para um sistema que sofre com rebuild de imagem, eu copiaria o **mecanismo**, mas não necessariamente o formato de armazenamento. Se arquivos versionados entram na imagem Docker, faz mais sentido persistir Content/Schema/Tool state em MySQL ou outro storage externo e deixar na imagem apenas o runtime/MCP estático.

---

## 3. Marcador de fronteira: OpenAI diz que RSI totalmente autônoma ainda não ocorre hoje

**Fonte oficial (21/09/2026):** https://openai.com/index/building-standards-next-phase-ai/

A OpenAI publicou hoje que seu objetivo inclui construir um **automated AI researcher**, iterar com ele em alinhamento e manter pessoas dentro do self-improvement loop. Ao mesmo tempo, afirma explicitamente que **fully autonomous RSI is not happening today** e que não deveria ser perseguida até que possa ser feita com segurança e preservando controle humano.

Isso é útil como referência para nossa classificação: sistemas como Designer-RSI e EvoOntology são self-improving em objetos externos persistentes, mas ainda não demonstram um loop em que a versão nova fica melhor também em melhorar o próprio mecanismo de evolução, avaliação e promoção.

---

## Conclusão da rodada

A novidade mais importante é **Designer-RSI**, porque ele demonstra uma forma bastante próxima do padrão de produção que estamos procurando:

```text
tráfego real
→ detectar lacunas/falhas
→ gerar variantes persistentes
→ medir com replay controlado
→ rejeitar regressões
→ promover somente a vencedora
→ próxima rodada usa o estado novo
```

E EvoOntology acrescenta uma peça especialmente útil para agentes que usam MCP: **não só skills, mas a própria camada semântica pela qual o agente entende dados pode evoluir como estado versionado externo aos pesos**.

A arquitetura geral fica cada vez mais clara:

```text
Runtime estático
    │
    ├── Skills dinâmicas
    ├── Ontologia / retrieval dinâmico
    ├── Routing / policies dinâmicas
    └── Version registry
             ↓
          traces
             ↓
       attribution
             ↓
        candidates
             ↓
   replay / held-out / regression
             ↓
      PROMOTE | DISCARD
```

**Ainda não apareceu RSI forte nesta rodada.** Designer-RSI mantém Reflector/Grader/gate fixos; EvoOntology mantém o core determinístico e o mecanismo de evolução fixos; e a própria OpenAI afirma que RSI totalmente autônoma não está acontecendo hoje.
