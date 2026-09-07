# Radar diário — agentes mais inteligentes — 07/09/2026

## Resumo executivo

A rodada de hoje reforça quatro ideias que considero diretamente úteis para o AI Hub:

1. **Proatividade precisa ser uma decisão explícita do harness**, não apenas uma característica emergente do LLM. Um agente deve decidir entre ficar em silêncio, perguntar, ajudar ou agir conforme benefício, incerteza, autorização e custo de interrupção.
2. **Disponibilidade de contexto não significa que o agente vai buscá-lo.** Em tarefas longas, modelos deixam de consultar estados relevantes mesmo quando as ferramentas existem e as instruções recomendam fazê-lo.
3. **O agente precisa manter um estado explícito do que sabe, do que acredita e do que ainda é desconhecido.** Trabalhos recentes sobre belief state mostram ganhos quando incerteza é representada externamente em vez de ficar implícita no histórico.
4. **Avaliação precisa incluir requisitos implícitos.** Benchmarks feitos a partir de pedidos reais e incompletos mostram que agentes ainda falham bastante quando precisam inferir o que um usuário razoavelmente esperava, mesmo com modelos de fronteira.

Não encontrei neste domingo/segunda-feira um novo post de engenharia de laboratório que acrescentasse evidência suficientemente distinta dos materiais já cobertos. A edição de hoje é, portanto, predominantemente acadêmica e concentra-se nos trabalhos mais recentes e úteis que ainda não haviam entrado no radar.

---

## 1. Proactive Service Agents — a proatividade como decisão sob incerteza

**Tipo:** survey / framework acadêmico (preprint)  
**Data:** 03/09/2026  
**Fonte:** https://arxiv.org/abs/2609.03727

O paper **“Proactive Service Agents: A Unified Decision Framework, Methods, and Evaluation”** parte de uma crítica diretamente relacionada ao objetivo deste radar: a maioria dos agentes começa a partir de uma instrução explícita e relativamente completa. Em uso real, porém, uma necessidade pode surgir de sinais incompletos — histórico, tela, sensor, progresso travado ou padrão do usuário — antes que o usuário a formule corretamente.

O framework trata o agente como um processo de decisão parcialmente observável e propõe que ele escolha entre modos como:

```text
SILENT
ASK
ASSIST
ACT
```

A decisão considera benefício esperado, custo de interrupção, custo de fazer perguntas, risco de execução, privacidade e autorização. O paper destaca dois conceitos especialmente úteis:

- **valor de esperar**: não agir agora pode ser a melhor decisão se mais evidência provavelmente aparecer;
- **valor de perguntar**: uma pergunta é útil quando a informação obtida muda materialmente a decisão seguinte.

O paper também separa autorização de utilidade. Uma ação não autorizada não deveria se tornar aceitável apenas porque o benefício estimado parece alto.

### Aplicação ao AI Hub

Eu adicionaria uma camada anterior ao planner:

```text
User signal
   │
   ▼
Need / Goal Inference
   │
   ▼
Intervention Gate
   │
   ├── SILENT
   ├── RETRIEVE_CONTEXT
   ├── ASK
   ├── ASSIST
   └── ACT
```

Para uma solicitação como:

> “adicione login Google”

um agente poderia inferir que faltam informações sobre sessão, secrets e fluxo legado. Mas ele não deveria automaticamente perguntar tudo. Primeiro poderia verificar repo, MCP, decisões anteriores e testes. Somente se a incerteza relevante permanecer, passaria para `ASK`.

Isso transforma **“deduzir algo que Paulo não disse”** em um problema operacional: detectar necessidade + estimar incerteza + escolher a intervenção adequada.

### Limitações

É principalmente uma síntese/formalização da literatura, não um novo agente demonstrando ganhos em um benchmark único. O valor para nós é arquitetural: oferece uma linguagem clara para construir e depois avaliar proatividade.

---

## 2. CivBench — ter MCP não significa que o agente vai procurar o que precisa

**Tipo:** benchmark acadêmico / preprint  
**Data:** 02/09/2026  
**Fonte:** https://arxiv.org/abs/2609.02459  
**Código:** https://github.com/lmwilki/civ6-mcp

O **CivBench** conecta agentes a Civilization VI através de **76 ferramentas MCP** e cria episódios com mais de 300 turnos e milhares de chamadas de ferramenta. O ponto que interessa ao nosso projeto não é o jogo, mas o desenho experimental: informações importantes existem no ambiente, porém só entram no contexto se o agente decidir consultá-las.

O paper separa explicitamente:

```text
information available
        ≠
information retrieved
        ≠
information acted upon
```

Os pesquisadores criam duas métricas muito interessantes:

- **Proactive Monitoring Rate (PMR)**: mede se o agente consulta estados estratégicos latentes sem esperar que o problema já esteja evidente;
- **RAG@10**: mede se compromissos declarados pelo agente em suas reflexões realmente viram ações nos dez turnos seguintes.

Nos 23 runs admissíveis do piloto, os agentes consultaram estados estratégicos menos frequentemente do que o playbook recomendava. Para um estado que deveria ser verificado a cada 20 turnos, as consultas ocorreram tipicamente a cada 30–75 turnos. Em 7 de 20 derrotas detectáveis, o agente nem sequer consultou o estado relevante dentro da janela de alerta de 20 turnos. O RAG@10 ficou entre **48,2% e 65,8%**, mostrando que boa parte dos compromissos registrados não virou ação de curto prazo.

### Aplicação ao AI Hub

Essa evidência é muito importante para nossa arquitetura. Simplesmente colocar ferramentas no MCP não resolve o problema. Precisamos de um **Attention / Monitoring Controller**.

```text
Task Belief State
       │
       ▼
Attention Controller
       │
       ├── quais estados podem ter mudado?
       ├── quais unknowns continuam importantes?
       ├── quais evidências estão vencidas?
       └── quais compromissos ainda não foram executados?
       │
       ▼
MCP / repo / logs / memory
```

Também criaria um **Commitment Ledger**:

```yaml
commitments:
  - id: preserve-password-login
    source: inferred_requirement
    status: pending
    evidence_required:
      - AuthPasswordIT
    deadline_phase: verification
```

O planner pode dizer muitas coisas corretas e ainda esquecê-las durante a execução. O ledger impede que decisões importantes desapareçam quando o contexto muda.

### Limitações

É um estudo piloto com apenas 23 runs e em um domínio de jogo. Os próprios autores alertam que não é adequado para ranking de modelos. Além disso, o playbook força reflexão estruturada, então o RAG@10 mede fidelidade a compromissos elicitados, não planejamento espontâneo puro.

---

## 3. Belief-Based World Models + Agent-BRACE — tornar “não sei” parte explícita do estado

### 3.1 Towards a Belief-Based World Model for LLM Agents

**Tipo:** pesquisa acadêmica / preprint  
**Data:** 31/08/2026  
**Fonte:** https://arxiv.org/abs/2609.00455

O paper **“Towards a Belief-Based World Model for LLM Agents”** argumenta que simular consequências de ações não basta em ambientes parcialmente observáveis. Antes de perguntar “o que acontecerá se eu fizer X?”, o agente precisa representar **o que ele acredita ser verdade agora e onde ainda existe incerteza**.

A proposta mantém um belief state consultável pelo LLM. O trabalho mostra que expor esse estado de crença melhora o desempenho em tarefas sob observabilidade parcial e complementa world models baseados apenas em simulação.

### 3.2 Agent-BRACE

**Tipo:** pesquisa acadêmica / preprint  
**Data:** 12/05/2026  
**Fonte:** https://arxiv.org/abs/2605.11436

O **Agent-BRACE** dá um formato ainda mais concreto para essa ideia. Ele separa o agente em um modelo de belief state e uma policy. O belief state é uma coleção de afirmações atômicas em linguagem natural, cada uma com um nível verbalizado de certeza, de `certain` até `unknown`.

Nos ambientes long-horizon testados, o método obteve melhora absoluta média de **+14,5%** com Qwen2.5-3B-Instruct e **+5,3%** com Qwen3-4B-Instruct sobre baselines fortes de RL, mantendo uma janela de contexto quase constante. A calibração do belief state também melhora conforme novas evidências aparecem.

### Aplicação ao AI Hub

Isso reforça uma decisão que já vinha aparecendo no nosso radar: o `Requirement Compiler` não deve produzir uma lista plana de requisitos. Ele deveria atualizar um **Task Belief State** explícito.

```yaml
goal:
  value: adicionar login Google
  confidence: certain

beliefs:
  - claim: backend é autoridade da sessão
    confidence: likely
    provenance: architecture-doc

  - claim: login por senha deve continuar funcionando
    confidence: very_likely
    provenance: inferred_from_existing_feature

  - claim: refresh token é armazenado no backend
    confidence: unknown
    provenance: null
    evidence_needed:
      - auth code
      - config
      - prior decisions
```

O ponto essencial é separar:

```text
FACT
BELIEF
ASSUMPTION
UNKNOWN
```

O planner não deveria tratar todos como fatos. `UNKNOWN` deve criar pressão para buscar evidência. `ASSUMPTION` deve aparecer na verificação. `FACT` precisa carregar provenance.

### Limitações

O paper de BB-WM ainda é inicial e não demonstra a abordagem em um agente de engenharia de software de produção. Agent-BRACE foi avaliado em ambientes embodied/parcialmente observáveis e com modelos menores, então os ganhos numéricos não devem ser extrapolados diretamente para modelos frontier e MCPs de software.

---

## 4. K-Bench — pedidos reais já são incompletos e o benchmark deveria refletir isso

**Tipo:** benchmark acadêmico / preprint  
**Data:** 21/08/2026  
**Fonte:** https://arxiv.org/abs/2608.21601

O **K-Bench** é especialmente útil para pensar como avaliar nosso AI Hub porque usa **178 primeiros pedidos reais de usuários**, extraídos de aproximadamente 18 mil sessões de uma plataforma científica. Setenta por cento dos pedidos tinham pelo menos um arquivo anexo. Os prompts foram mantidos como chegaram: frequentemente incompletos, heterogêneos e sem uma resposta de referência simples.

Nove modelos executaram os 178 pedidos no mesmo harness, produzindo **1.602 runs**. Três juízes cegos avaliaram oito dimensões. Uma delas, `task_fulfillment`, inclui explicitamente **cobertura dos requisitos explícitos e dos requisitos implícitos razoáveis**.

Os resultados mostram que o problema não está resolvido por modelos frontier. Em 39.934 julgamentos dimensionais/holísticos, **47,6% ficaram abaixo do nível que o benchmark considera aceitável por um cientista com apenas pequenas edições**. O principal failure tag foi **overclaiming**, presente em 31,4% das avaliações.

### Aplicação ao AI Hub

Eu criaria um benchmark baseado nos seus próprios prompts históricos.

Para cada tarefa real:

```text
Original Prompt
       │
       ├── explicit requirements
       ├── reasonable implicit requirements
       ├── hidden project constraints
       ├── context sources that should be consulted
       └── acceptance evidence
```

E avaliaria separadamente:

```text
Implicit Requirement Recall
Context Retrieval Precision
Unknown Detection
Assumption Calibration
Commitment Execution
Regression Avoidance
Evidence Quality
Overclaiming Rate
```

Exemplo:

```text
Prompt original:
"adicione login Google"

Requisito implícito esperado:
preservar login atual

Contexto que deveria ser consultado:
auth architecture + testes existentes

Falha grave:
implementar OAuth e quebrar password login
```

Isso seria muito mais útil para seu projeto do que apenas medir SWE-bench ou taxa genérica de resolução de issues, porque mede precisamente a inteligência que você quer desenvolver.

### Limitações

O benchmark está concentrado em ciência e usa juízes LLM, embora de famílias distintas. Cada configuração foi executada apenas uma vez por tarefa, portanto não separa perfeitamente capacidade de variância estocástica. Além disso, o harness de produção original da plataforma foi retirado; isso é útil para comparar modelos, mas não mede o potencial máximo de um sistema com skills e memória especializados.

---

## Síntese para a arquitetura do AI Hub

A combinação dos trabalhos de hoje sugere que a camada anterior ao planner deve ficar mais rica:

```text
                         USER SIGNAL
                              │
                              ▼
                     GOAL / NEED INFERENCE
                              │
                              ▼
                         BELIEF STATE
                ┌─────────────┼─────────────┐
                │             │             │
              FACTS        BELIEFS       UNKNOWNS
                │             │             │
                └─────────────┼─────────────┘
                              ▼
                    ATTENTION CONTROLLER
                              │
              ┌───────────────┼───────────────┐
              │               │               │
             MCP             repo           memory
              │               │               │
              └───────────────┼───────────────┘
                              ▼
                     INTERVENTION GATE
                    silent / ask / act
                              │
                              ▼
                           PLANNER
                              │
                              ▼
                         COMMITMENT LEDGER
                              │
                              ▼
                           EXECUTOR
                              │
                              ▼
                          VERIFIER
                              │
                              ▼
                     BELIEF STATE UPDATE
```

### Nova conclusão prática

Até agora estávamos tratando `Requirement Compiler` como o principal componente. Depois desta rodada, eu o dividiria em **quatro responsabilidades distintas**:

1. **Goal/Need Inference** — o que o usuário provavelmente pretende alcançar?
2. **Belief State Manager** — o que sabemos, acreditamos, assumimos e ainda desconhecemos?
3. **Attention Controller** — qual contexto precisa ser buscado agora, mesmo que o usuário não tenha pedido?
4. **Intervention Gate** — devemos continuar sozinhos, buscar evidência, perguntar ou agir?

O ganho importante é que o agente deixa de depender de um único prompt para “lembrar de ser inteligente”. A inteligência passa a ser parcialmente **estrutural e observável no harness**.

## Prioridade de implementação sugerida

```text
1. Task Belief State
2. Attention Controller
3. Commitment Ledger
4. Intervention Gate
5. Benchmark de implicit requirements usando prompts reais
6. Só depois self-improving harness
```

A principal frase desta rodada é:

> **Dar acesso ao contexto não basta. O harness precisa representar explicitamente o que ainda é desconhecido e criar mecanismos que obriguem o agente a procurar a informação certa antes de agir.**
