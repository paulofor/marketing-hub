# Radar diário — agentes mais inteligentes | 09/09/2026

Hoje não apareceu um novo paper forte especificamente sobre **inferência de requisitos implícitos**. O único trabalho de 8 de setembro que encontrei com impacto claro para este radar é o **TSBench**, mas a busca também revelou quatro trabalhos muito recentes, ainda não cobertos nas rodadas anteriores, que acrescentam peças importantes ao harness: medir incerteza antes de executar, preservar memória entre trocas de modelo, transformar experiência em skills persistentes e evitar que compaction destrua evidência fina.

A conclusão da rodada é que o AI Hub começa a precisar de uma distinção explícita entre **memória de fatos**, **memória de evidência**, **memória procedural** e **estado de execução**. Além disso, inferências feitas pelo agente deveriam carregar uma estimativa de risco suficiente para permitir que o harness decida se executa, busca mais contexto, gasta mais compute ou pede intervenção.

## TSBench: diagnóstico baseado em evidência supera “reflection” vaga

**Pesquisa acadêmica / preprint — submetido em 8 de setembro de 2026.**

O TSBench avalia agentes de LLM em química de mecanismos. O agente produz uma hipótese estrutural e a submete a uma pipeline de química quântica que retorna um veredito físico, e não apenas uma avaliação textual. Em 546 avaliações de sete modelos sobre 78 reações elementares, a taxa agregada de sucesso subiu de **50,4% para 66,8%** quando os agentes puderam revisar suas tentativas a partir de diagnóstico produzido pelo verificador.

O detalhe mais importante para o nosso caso é que várias respostas erradas eram **localmente plausíveis**. Ou seja, o agente conseguia produzir algo que parecia coerente, mas o caminho global estava errado. Isso é análogo a um agente de software que implementa uma solução elegante, compila, mas viola uma dependência arquitetural ou um requisito implícito.

Aplicação no AI Hub: o `Critic` deveria receber preferencialmente **evidência do ambiente**, não apenas a própria saída do LLM. Em vez de `revise sua implementação`, o fluxo deveria ser `execute → observe → diagnostique → revise`. Para requisitos implícitos, isso significa converter inferências importantes em verificadores concretos sempre que possível.

Fonte: https://arxiv.org/abs/2609.08503

## Speculative Uncertainty: um gate antes de ações caras ou irreversíveis

**Pesquisa acadêmica / EMNLP 2026 Industry Track — submetido em 4 de setembro de 2026.**

O trabalho **How to Speculate about Uncertainty in Agentic Coding?** propõe um mecanismo que estima a probabilidade de falha de um agente de programação **antes da execução**, mesmo quando o agente principal é uma caixa-preta. Um modelo menor avalia a trajetória já gerada e produz um sinal de risco que pode alimentar políticas de roteamento, intervenção humana ou test-time compute adicional.

Em experimentos com Qwen3-Coder-480B e Claude 3.5 Sonnet, um veto pré-execução baseado nesse sinal reduziu a taxa de erro de execução em **6–8 pontos percentuais** e o custo em tokens em **14–19%**, com transferência para benchmarks fora da distribuição sem novo treinamento.

Isso é muito aplicável ao nosso `Task Belief State`. Hoje pensamos em `FACT`, `BELIEF`, `ASSUMPTION` e `UNKNOWN`. Eu acrescentaria um `execution_risk` calculado antes de ações relevantes. Se o plano depende de muitos requisitos inferidos ou de informações de baixa confiança, o harness pode automaticamente aumentar investigação ou verificação antes de tocar no sistema.

Exemplo conceitual:

```yaml
plan_step: update_auth_flow
requirements:
  - preserve_password_login
  - backend_owns_session
confidence: 0.63
execution_risk: high
policy: retrieve_more_context_before_execute
```

Fonte: https://arxiv.org/abs/2609.05274

## Memory portability: memória textual resumida pode ficar acoplada ao modelo que a escreveu

**Pesquisa acadêmica / preprint — submetido em 4 de setembro de 2026.**

**Does Your Agent's Memory Survive a Model Upgrade?** testa algo muito importante para sistemas como o AI Hub: o que acontece quando a memória foi criada por um modelo e depois passa a ser lida por outro.

O estudo compara histórico bruto, RAG, notas naturais comprimidas por um modelo e um knowledge graph de esquema fixo. Em 48 históricos sintéticos, a estrutura de esquema fixo foi praticamente estável depois da troca de modelo, enquanto notas naturais apresentaram mudanças assimétricas de **+9,91 ou −13,28 pontos percentuais**, dependendo da direção da migração. Em índices RAG, misturar embeddings antigos e novos recuperou apenas parte do ganho de uma reindexação completa. Os autores também mostram que conservar a evidência bruta facilita reparar memórias degradadas.

Isso sugere uma decisão arquitetural muito concreta: **decisões críticas do AI Hub não deveriam existir somente como resumos livres produzidos pelo LLM**. O resumo pode existir para leitura, mas fatos e decisões importantes deveriam ser normalizados em uma estrutura estável, com fonte original preservada.

Eu separaria:

```text
Narrative Memory
  resumo legível pelo modelo

Structured Memory
  facts / constraints / decisions / confidence / provenance

Evidence Store
  conversa / arquivo / commit / log / teste original
```

A troca GPT-5.6 → GPT-6 → outro modelo deixa de ser uma migração cega de memória e passa a ser um problema verificável.

Fonte: https://arxiv.org/abs/2609.05339

## Persistent Skills: experiência deveria virar procedimento versionado, não apenas lembrança

**Pesquisa acadêmica / preprint — submetido em 4 de setembro de 2026.**

**From Interaction Traces to Persistent Skills** transforma trajetórias de execução e feedback do avaliador em uma biblioteca versionada de procedimentos reutilizáveis. O modelo e o stack de grounding permanecem fixos; somente a biblioteca de skills evolui. Após um warm-up sem skills, o sistema com biblioteca evolutiva apresentou média pós-warm-up superior nos quatro domínios OSWorld avaliados, com diferenças entre **5,7 e 18,6 pontos percentuais**.

O paper também mostra uma limitação importante: revisões sucessivas de uma skill não garantem melhoria, e a proveniência revela casos de churn. Portanto, `skill accepted` não deveria significar `skill correta para sempre`.

Para o AI Hub, eu trataria episódios bem-sucedidos como candidatos a `VerifiedSkill`, não como memória comum. Uma skill deveria guardar trigger, pré-condições, passos, restrições, verificadores, proveniência e versão do modelo/harness em que foi validada.

Exemplo:

```yaml
skill: google-oauth-spring
trigger: google oauth + spring
preconditions:
  - existing_session_architecture_loaded
constraints:
  - secrets_not_in_repo
verification:
  - auth_regression_tests
  - callback_test
origin:
  task: auth-2026-08-20
  evidence: commits_and_tests
status: verified
```

Fonte: https://arxiv.org/abs/2609.04869

## KVMem: compaction pode apagar justamente a evidência que permitiria deduzir contexto

**Pesquisa acadêmica / preprint — submetido em 4 de setembro de 2026.**

O **KVMem** aborda long-running agents que acumulam mais contexto do que a janela nativa consegue suportar. Em vez de comprimir tudo em resumos, preserva blocos antigos como estado KV paginado e seleciona dinamicamente quais partes materializar. Nos benchmarks avaliados, em um teste DeepSWE com Qwen3.8-27B, a taxa de sucesso subiu de **43,8% com compaction-only para 48,4%** com KVMem.

O paper é principalmente de sistemas e implantação local, então não prova que a técnica é a melhor escolha para um AI Hub baseado em APIs. Mas a mensagem arquitetural é forte: **compaction é lossy**. Se o requisito implícito que o agente precisava descobrir estava em um detalhe antigo removido pelo resumo, um planner melhor não o recuperará.

Portanto, eu não usaria compaction como fonte canônica. O resumo seria apenas uma view. O histórico/evidência precisa permanecer endereçável e recuperável por um mecanismo separado.

Fonte: https://arxiv.org/abs/2609.04852

## Evidência de laboratório: agentes mais fortes ainda dependem muito de steering humano

**Post técnico / dados internos de laboratório — OpenAI, 6 de setembro de 2026.**

No relatório **Research acceleration: The view inside OpenAI**, a OpenAI mostra que coding agents já são usados em alto volume pelos pesquisadores e realizam tarefas cada vez mais longas e complexas. Porém, a própria análise relata que, nos últimos seis meses, **mais da metade das tarefas bem-sucedidas estimadas em 4–8 horas exigiram pelo menos uma intervenção humana**. O relatório também observa que planejamento de alto nível ainda representa uma fração pequena do output dos agentes.

Isso é uma boa cautela contra a ideia de que um modelo mais forte torna automaticamente desnecessário o harness de inferência de objetivo, contexto e verificação. A autonomia aumenta, mas o steering ainda aparece justamente nas tarefas de horizonte maior.

Para o AI Hub, a consequência é não tentar eliminar a intervenção humana, e sim torná-la **seletiva**: o sistema investiga sozinho primeiro; se a incerteza continuar alta e puder alterar materialmente o resultado, ele pede uma intervenção pequena e bem localizada.

Fonte: https://openai.com/index/research-acceleration-view-inside-openai/

## O que muda na arquitetura do AI Hub nesta rodada

Eu atualizaria o desenho para incluir um **Risk & Evidence Gate** e três classes diferentes de memória:

```text
                       USER INTENT
                            │
                            ▼
                    GOAL OPERATIONALIZER
                            │
                            ▼
                     TASK BELIEF STATE
          facts / beliefs / assumptions / unknowns
                            │
                            ▼
                  CONTEXT & EVIDENCE RETRIEVAL
                            │
             ┌──────────────┼───────────────┐
             │              │               │
      Structured Memory  Evidence Store  Skill Library
             │              │               │
             └──────────────┼───────────────┘
                            ▼
                         PLANNER
                            │
                            ▼
                   RISK & EVIDENCE GATE
                  low risk ───── high risk
                     │               │
                  execute     retrieve / verify /
                              extra compute / ask
                     │               │
                     └───────┬───────┘
                             ▼
                          EXECUTOR
                             │
                             ▼
                    GROUNDED VERIFIER
                             │
                             ▼
                   EXPERIENCE → SKILL CANDIDATE
```

O ponto principal de hoje é que **“deduzir o que o usuário não disse” não deve ser apenas uma habilidade cognitiva do LLM**. O harness deve saber quando uma dedução ainda é frágil demais para virar ação, preservar a evidência original que permite revisá-la e transformar experiências comprovadas em procedimentos reutilizáveis.

Na prática, eu priorizaria agora: `StructuredMemory + EvidenceStore`, depois `RiskGate`, depois `SkillLibrary`. O `Requirement Compiler` continua sendo a porta de entrada, mas essas três peças fazem com que suas inferências deixem de ser apenas texto plausível e passem a fazer parte de um sistema auditável e evolutivo.
