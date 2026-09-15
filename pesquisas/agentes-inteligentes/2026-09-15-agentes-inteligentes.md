# Radar diário — agentes mais inteligentes | 2026-09-15

## Síntese da rodada

A leva de 14/09 trouxe novidades realmente úteis. O trabalho mais importante para este radar é **Dream-RSI**, porque mostra uma forma prática de um sistema melhorar a própria estratégia de exploração sem reexecutar caro todo o ambiente: ele reaproveita o histórico de descobertas como um simulador offline para testar políticas novas. Também apareceu **OpenAI4S**, que reforça a importância de estado persistente, proveniência e um ledger append-only em tarefas longas; um trabalho do Google sobre *vibe design* que separa explicitamente exploração de intenção e implementação; e **Atria Dawn**, cujo pipeline de treino liga interações com ferramentas a resultados externamente verificados e cujo estudo de uso mostra humanos migrando de execução para julgamento sobre direção e evidência.

A conclusão arquitetural de hoje é esta: **o AI Hub deveria separar quatro coisas que normalmente ficam misturadas: descoberta de intenção, exploração de hipóteses, execução persistente e avaliação da própria estratégia de busca.**

Não encontrei hoje um novo paper especificamente sobre *missing-context detection* que fosse mais forte do que os trabalhos já cobertos nos últimos dias. Preferi não completar a rodada com material marginal.

---

## 1. Dream-RSI — usar o histórico de exploração como simulador para melhorar o próprio harness

**Tipo:** pesquisa acadêmica / preprint  
**Data:** 14/09/2026  
**Fonte:** https://arxiv.org/abs/2609.14858

Dream-RSI parte de um gargalo típico de self-improving agents: para melhorar uma política de exploração, normalmente é preciso testar repetidamente novas estratégias em rollouts longos e caros. O sistema introduz uma camada leve de orquestração que torna a exploração programável, sem alterar o coding agent de base. A ideia central é transformar árvores históricas de descoberta em um **replay simulator**. Dentro desse simulador, novas políticas de exploração podem ser avaliadas offline usando o que já aconteceu, recebendo feedback barato antes de voltar ao ambiente real.

O ciclo é aproximadamente:

```text
execuções reais
    ↓
árvores de descoberta
    ↓
replay simulator
    ↓
comparar políticas de exploração
    ↓
selecionar política melhor
    ↓
redeploy online
    ↓
novas descobertas entram no simulador
```

Os autores avaliam a abordagem em engenharia de algoritmos, otimização matemática e engenharia de kernels GPU e reportam qualidade de descoberta competitiva ou superior com redução substancial de custo em vários cenários.

### Aplicação ao AI Hub

Essa ideia pode ser aplicada diretamente à camada que estamos chamando de `Requirement Discovery Runtime`.

Hoje, para descobrir aquilo que o usuário não colocou no prompt, o harness pode escolher entre várias estratégias:

```text
buscar no repo
buscar no MCP
consultar memória
inspecionar testes
inspecionar commits
perguntar ao usuário
rodar experimento
```

Em vez de fixar essa política manualmente, eu registraria cada tarefa como uma **Discovery Tree**:

```yaml
discovery_episode:
  prompt: "adicione login Google"

  branches:
    - action: search_repo
      query: "existing auth flow"
      evidence_found: true
      cost: 1800_tokens

    - action: search_memory
      query: "oauth decisions"
      evidence_found: true
      cost: 400_tokens

    - action: ask_user
      executed: false

  hidden_requirements_recovered:
    - preserve_existing_login
    - backend_owns_session

  outcome:
    success: true
```

Depois, uma camada `DiscoveryPolicyEvolver` poderia testar offline políticas como:

```text
repo-first
memory-first
uncertainty-first
cheap-evidence-first
risk-weighted
```

sem repetir toda a execução do projeto.

### Limitação

O paper não demonstra esse mecanismo em requisitos de software nem em recuperação de contexto implícito; a transferência para o AI Hub é uma inferência arquitetural. Além disso, replay só consegue avaliar bem estratégias dentro da região já explorada — ele não substitui exploração real quando surgem situações novas.

---

## 2. OpenAI4S — long-running agents precisam de estado persistente e proveniência fora do contexto do LLM

**Tipo:** pesquisa acadêmica + sistema open-source de engenharia  
**Data:** 14/09/2026  
**Fonte:** https://arxiv.org/abs/2609.15096

OpenAI4S é um agente científico construído em torno do princípio **Code as Action, Science as Sessions**. Em vez de tratar uma tarefa longa como apenas uma sequência de mensagens dentro da janela de contexto, o sistema mantém runtime persistente, sessões recuperáveis, artifacts versionados, checkpoints e um **Action Ledger append-only** com registros de execução por célula.

A arquitetura preserva:

```text
ação executada
entrada
resultado
artifact produzido
ambiente
versão
checkpoint
proveniência
```

Em 36 cenários científicos, o sistema obteve score geral de 7,83, contra 5,7–6,4 para um harness de coding generalista avaliado com três modelos de fronteira; os maiores ganhos apareceram justamente em workflows longos e intensivos em computação. Os próprios autores reconhecem que especificação completa do ambiente e rerun perfeito ainda são pontos fracos.

### Aplicação ao AI Hub

Isso reforça uma separação que eu faria explicitamente:

```text
LLM CONTEXT
───────────
contexto temporário da sessão
pode sofrer compaction

PROJECT EXECUTION STATE
───────────────────────
append-only ledger
checkpoints
artifacts
requirements versions
belief versions
tool results
verifier evidence
```

Um agente que recebe hoje:

```text
"continue a implementação do OAuth"
```

não deveria depender de um resumo narrativo para saber onde parou. Ele deveria consultar algo como:

```yaml
execution_state:
  task: AUTH-42
  checkpoint: cp-17
  last_completed_step: add_google_provider
  pending:
    - preserve_password_login_regression
    - configure_production_callback
  evidence:
    - OAuthCallbackIT: pass
    - AuthPasswordIT: not_run
```

Isso reduz diretamente a quantidade de contexto que você precisa repetir e também melhora a capacidade do agente de inferir a próxima ação correta.

### Limitação

Os experimentos estão concentrados em workflows científicos e o benchmark é pequeno. O próprio paper afirma que rerun completo e captura do ambiente ainda não estão resolvidos.

---

## 3. Enabling Creative Exploration for Vibe Design Agents — gerar especificações intermediárias antes de implementar

**Tipo:** pesquisa acadêmica / preprint do Google  
**Data:** 14/09/2026  
**Fonte:** https://arxiv.org/abs/2609.15078

Esse trabalho é de design de interfaces, mas contém uma ideia arquitetural extremamente transferível. Em vez de aumentar a temperatura do modelo e deixar **intenção e implementação variarem simultaneamente**, os autores separam os dois estágios.

Primeiro, um pre-pass gera **especificações estruturadas de direção de design**, acompanhadas de scores de typicality. Um seletor externo escolhe uma direção. Só então o gerador downstream cria a página mantendo os parâmetros de geração fixos.

Em 168 prompts, com 1.255 comparações pareadas por temperatura para cada intervenção, essa separação aumentou a diversidade observada de temas e screenshots. Em um experimento online com mais de 300 mil tarefas, o efeito em exportação de código permaneceu estatisticamente incerto; houve menos feedback negativo, mas mais interações de correção.

### Aplicação ao AI Hub

Eu faria exatamente isso para requisitos ambíguos.

Em vez de:

```text
prompt
  ↓
planner
  ↓
implementation
```

usar:

```text
prompt
  ↓
INTENT HYPOTHESIS GENERATOR
  ↓
3–5 especificações candidatas
  ↓
Evidence / Typicality / Risk scoring
  ↓
seleção
  ↓
planner
  ↓
implementation
```

Exemplo:

```yaml
request:
  "adicione login Google"

intent_candidates:

  - id: I1
    interpretation:
      add_google_as_optional_provider
      preserve_existing_login: true
    typicality: high

  - id: I2
    interpretation:
      replace_existing_login_with_google
      preserve_existing_login: false
    typicality: low

  - id: I3
    interpretation:
      add_google_for_admin_only
    typicality: low
```

O harness então tenta validar as hipóteses contra repo, memória, decisões anteriores e testes. A implementação começa apenas depois que a interpretação foi separada do ato de codificar.

Isso reduz um erro comum: **a primeira interpretação plausível se transformar silenciosamente em requisito**.

### Limitação

O estudo mede exploração estética e não elicitação de requisitos. Além disso, mais alternativas podem aumentar custo e até gerar mais correções; o ganho depende de um bom seletor e de critérios de parada.

---

## 4. Atria Dawn — treinamento por experiência verificável e o papel humano migrando para julgamento

**Tipo:** relatório técnico / preprint de laboratório sobre modelo agentic  
**Data:** 14/09/2026  
**Fonte:** https://arxiv.org/abs/2609.15818

Atria Dawn Preview é um foundation model voltado a pesquisa e engenharia. O detalhe mais relevante para este radar não é o ranking do modelo, mas seu **Verifiable Experience Pipeline**, que conecta interações mediadas por tools a ambientes executáveis e resultados externamente verificados.

O modelo foi avaliado em 16 benchmarks de pesquisa, engenharia e trabalho digital, atingindo o melhor resultado reportado em cinco deles segundo os autores. O estudo também analisa 769 tarefas reais de 56 participantes. Aproximadamente um terço das tarefas concluídas com ajuda de IA foi avaliado pelos participantes como inviável sem IA em condições comparáveis. Os logs indicam que os agentes frequentemente propõem métodos e implementam revisões, enquanto humanos mantêm a maior parte das decisões finais e orientam a exploração com julgamento e feedback.

### Aplicação ao AI Hub

Isso sugere um desenho importante para autonomia:

```text
AGENTE
──────
formula hipóteses
busca contexto
propõe métodos
executa experimentos
implementa revisões
produz evidência

HUMAN AUTHORITY
───────────────
define direção
aceita mudança de objetivo
resolve ambiguidades de preferência
aprova trade-offs irreversíveis
julga evidência crítica
```

Portanto, quando o AI Hub infere um requisito implícito, ele não precisa tratá-lo automaticamente como uma ordem do usuário. Ele pode classificá-lo:

```yaml
inferred_requirement:
  statement: preserve_existing_login
  confidence: 0.88
  evidence:
    - current_tests
    - historical_decision
  authority_level: agent_can_adopt
```

versus:

```yaml
inferred_requirement:
  statement: remove_password_login
  confidence: 0.42
  authority_level: user_confirmation_required
```

Essa distinção entre **inferência factual** e **mudança de intenção** é uma das fronteiras mais importantes para agentes proativos.

### Limitação

O paper é produzido pela própria equipe do modelo e é muito recente; ainda não há replicação independente. Os resultados agregam modelo, treinamento e stack agentic, portanto não permitem atribuir os ganhos especificamente ao pipeline verificável.

---

## O que eu mudaria no AI Hub depois da rodada de hoje

A arquitetura que está emergindo agora fica assim:

```text
                         USER REQUEST
                              │
                              ▼
                  INTENT HYPOTHESIS LAYER
             múltiplas interpretações candidatas
                              │
                              ▼
                 REQUIREMENT DISCOVERY RUNTIME
                 repo / MCP / memory / tests
                              │
                              ▼
                     TASK BELIEF STATE
                              │
                              ▼
                   DISCOVERY POLICY ROUTER
          escolher onde buscar e quanto explorar
                              │
                              ▼
                        TASK PLAN
                              │
                              ▼
                PERSISTENT EXECUTION SESSION
                 Action Ledger / checkpoints
                              │
                              ▼
                    EXTERNAL VERIFIER
                              │
                              ▼
                     EXPERIENCE STORE
                              │
                              ▼
                DISCOVERY REPLAY SIMULATOR
                              │
                              ▼
                POLICY / HARNESS IMPROVEMENT
```

A novidade mais forte de hoje é que eu adicionaria **duas camadas que ainda não estavam tão claras**:

1. `Intent Hypothesis Layer`: gerar explicitamente interpretações alternativas antes de planejar.
2. `Discovery Replay Simulator`: usar traces históricos para melhorar a política que decide onde procurar contexto ausente.

O primeiro reduz **erro de interpretação**. O segundo reduz **custo de aprender a procurar melhor**.

A combinação é particularmente interessante para o objetivo original deste radar:

```text
usuário diz pouco
      ↓
agente gera hipóteses sobre o que pode estar implícito
      ↓
busca evidência para discriminá-las
      ↓
seleciona uma interpretação
      ↓
executa mantendo estado persistente
      ↓
verifica no ambiente
      ↓
registra a trajetória
      ↓
harness aprende a fazer buscas melhores nas próximas tarefas
```

## Prioridade prática

Se eu fosse ordenar a implementação agora:

1. **Intent Hypothesis Layer** sobre o Requirement Discovery Runtime existente.
2. **Action Ledger + checkpoints persistentes** fora do contexto do LLM.
3. **Discovery Tree logging** para registrar como cada requisito implícito foi encontrado.
4. **Replay Simulator offline** para comparar políticas de busca e clarificação.
5. **Human Authority Boundary**, separando fatos que o agente pode inferir de mudanças de intenção que exigem você.

A tendência desta rodada é clara: os agentes mais capazes estão ficando menos dependentes de um único prompt/raciocínio linear e mais dependentes de uma arquitetura em que **hipóteses, evidência, estado, execução e aprendizagem do próprio processo de descoberta são entidades explícitas do harness**.
