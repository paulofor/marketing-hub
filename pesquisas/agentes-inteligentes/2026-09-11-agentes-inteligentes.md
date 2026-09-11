# Radar diário — agentes mais inteligentes | 11/09/2026

A rodada de hoje trouxe um sinal de engenharia particularmente forte: em 10/09/2026 a OpenAI lançou a **Agents API**, expondo como serviço o mesmo harness-base usado pelo Codex. Isso reforça diretamente uma hipótese que vem aparecendo neste radar: uma parcela crescente da capacidade de agentes não está apenas no modelo, mas no runtime/harness que administra contexto, ferramentas, subagentes, estado e ambiente de execução.

Ao mesmo tempo, novos preprints desta semana mostram três cuidados importantes para um AI Hub: **autoaperfeiçoamento do harness precisa ter espaço de busca controlado**, **skills procedurais podem ser melhores como subagentes isolados**, e **memória relevante não é sinônimo de memória semanticamente parecida**. Finalmente, um estudo de eviction mostra que apagar/compactar evidência pode causar perdas irreversíveis.

## 1. OpenAI Agents API: o Codex harness virou uma camada de infraestrutura reutilizável

**Tipo:** engenharia/produto — OpenAI, 10/09/2026.

A OpenAI colocou em beta público uma Agents API que hospeda e mantém o harness usado por Codex. O serviço oferece sessões longas com compaction automática, tool search que carrega definições de ferramentas sob demanda, programmatic tool calling, suporte a MCP, sandboxes e multi-agent/subagents com contextos separados. A OpenAI afirma manter e evoluir o harness junto com os modelos, com acesso versionado às capacidades.

Uma consequência arquitetural importante para o AI Hub é que talvez não seja necessário reimplementar toda a infraestrutura de execução. O AI Hub pode se tornar uma camada **acima** do Codex harness:

```text
AI HUB — CONTROL PLANE

Requirement / Goal Compiler
Task Belief State
Context & Evidence Router
Skill/Subagent Router
Acceptance Contract
Independent Eval
Harness Evolution Policy
          │
          ▼
OPENAI AGENTS API / CODEX HARNESS — EXECUTION PLANE

context management / compaction
tool search
MCP
programmatic tool calling
subagents
sandbox
long-running sessions
```

Isso preservaria no AI Hub justamente as partes diferenciadoras: inferir requisitos implícitos, descobrir contexto ausente, decidir o que é evidência, construir critérios de aceitação e aprender com execuções anteriores.

**Evidência:** a OpenAI descreve explicitamente o harness como responsável por gestão de contexto, ferramentas e subagentes. Entre os exemplos públicos, um cliente relata melhoria de score de avaliação de 0,71 para 0,85 e redução de 4x na latência de fluxos com subagentes; isso é um depoimento de cliente, não um experimento acadêmico controlado.

**Limitação:** é beta público e o harness gerenciado evolui ao longo do tempo. Para um sistema que aprende com suas próprias execuções, isso exige evals próprios e registro da versão/configuração usada para evitar confundir “o AI Hub melhorou” com “o runtime mudou”.

Fonte: https://openai.com/index/introducing-the-agents-api/

## 2. RobustSGPO: self-improving harness não deve poder editar tudo de uma vez

**Tipo:** preprint acadêmico — submetido em 09/09/2026.

**RobustSGPO: Search-Space Control for Agent Harness Evolution** estuda otimização automática de harnesses com feedback de execução. A contribuição mais útil para o AI Hub não é simplesmente “o agente reescreve seu prompt”, mas **controlar exatamente qual edição é permitida**, construir/verificar o patch e poder continuar a busca a partir do estado atual ou de snapshots preservados.

O estudo usa 120 tarefas, 95 runs e 7.350 tentativas candidatas. Em 30 tarefas held-out, a conclusão passou de **60,0% para 80,0%** e a qualidade de teste de **3,77 para 4,14**, sob orçamento de 20 milhões de tokens.

Eu traduziria isso para um `Harness Evolution Controller`:

```yaml
change_proposal:
  component: context_router
  operation: modify_retrieval_policy
  max_scope: small
  evidence: failure_cluster_17

validation:
  held_out_eval: required
  regression_eval: required

promotion:
  only_if_better: true
  rollback_snapshot: harness_v42
```

A principal regra seria: **o agente nunca recebe permissão abstrata para “melhorar o harness”**. Ele recebe permissão para modificar um componente e uma classe de comportamento específica.

**Limitação:** o experimento é concentrado no workflow AgentX de brainstorming. A transferência para coding agents e ambientes muito diferentes ainda precisa ser demonstrada de forma mais ampla.

Fonte: https://arxiv.org/abs/2609.09646

## 3. Subagents vs Agent Skills: a forma de executar conhecimento reutilizável importa tanto quanto o conteúdo

**Tipo:** preprint acadêmico — submetido em 07/09/2026 e circulando nesta semana.

**Subagents vs Agent Skills: Executing Reusable Knowledge for Long-Horizon Agentic Tasks** compara duas maneiras de reutilizar skills:

1. carregar o `SKILL.md` no contexto principal;
2. executar a mesma skill em um subagente com contexto novo e retornar somente o resultado.

O resultado é condicionado pela estrutura da skill. Nas skills originais do SkillsBench, geralmente sem contratos claros de entrada/saída, o modo tradicional de skill iguala ou supera subagentes. Mas quando os autores sintetizam **skills procedurais com contratos explícitos de input/output**, o padrão se inverte e subagentes passam a superar a execução inline. O estudo usa um subconjunto de 64 das 87 tarefas do SkillsBench. Para modelos mais fortes, subagentes reduziram o pico de contexto em mais de 80% das tarefas, embora consumissem mais tokens totais devido à comunicação entre contextos.

Isso sugere que o AI Hub não deveria ter apenas uma `SkillLibrary`, mas um roteador de modo de execução:

```text
Skill encontrada
     │
     ▼
Skill Type?

loosely structured knowledge
→ INLINE SKILL

procedural + clear I/O contract
→ SUBAGENT

procedural + side effects
→ SUBAGENT + verifier/action gate
```

Uma skill do AI Hub deveria começar a declarar:

```yaml
skill: google-oauth-spring

input_contract:
  - repository
  - current_auth_architecture
  - desired_provider

output_contract:
  - implementation_patch
  - evidence
  - tests_executed
  - unresolved_risks
```

Isso reduz a quantidade de conhecimento procedural despejada no contexto principal e cria encapsulamento semelhante ao de módulos de software.

**Limitação:** subagentes aumentam consumo total de tokens e dependem muito da qualidade da decomposição e dos contratos. Para conhecimento difuso que precisa ser combinado com todo o estado da tarefa, carregar a skill no agente principal pode continuar sendo melhor.

Fonte: https://arxiv.org/abs/2609.09233

## 4. MeClear: recuperar “memória parecida” pode piorar o agente

**Tipo:** preprint acadêmico — submetido em 08/09/2026.

**MeClear** ataca uma suposição comum em RAG/memória: se um trecho é semanticamente relevante, ele deve entrar no contexto. O paper mostra o contrário: memórias antigas, conflitantes ou enganadoras podem possuir similaridade alta e mesmo assim ter **utilidade downstream negativa**.

O sistema estima contribuição de memórias ao resultado e cria uma view de execução que suprime seletivamente evidências prejudiciais sem apagar a memória persistente. Em dez pools de diálogos longos, reporta recall do alvo de **85,9%** e recuperação global da tarefa de **82,3%**, melhoria de **25,5 pontos percentuais** sobre o baseline Leave-One-Out.

Para o AI Hub isso sugere separar de vez:

```text
PERSISTENT EVIDENCE STORE
          │
          │ não apagar
          ▼
TASK-CONDITIONED MEMORY VIEW
          │
     ┌────┴────┐
     │         │
 include    suppress
     │         │
     ▼         ▼
ACTIVE CONTEXT
```

Ou seja: uma decisão antiga pode continuar preservada para auditoria, mas ser suprimida da tarefa atual se foi substituída por uma decisão mais recente ou se conflita com evidência de maior autoridade.

**Limitação:** a atribuição cooperativa tem custo adicional e os experimentos estão concentrados em pools de memória de diálogos; seria necessário validar latência/custo em um AI Hub com repo, logs, MCP e milhares de artefatos heterogêneos.

Fonte: https://arxiv.org/abs/2609.09115

## 5. What Eviction Destroys: compaction deve ser uma view, não a verdade do projeto

**Tipo:** preprint acadêmico — submetido em 08/09/2026.

**What Eviction Destroys** introduz um teste contrafactual simples: quando o sistema falha depois de descartar memória, restaura-se a evidência correta e a mesma pergunta é executada novamente. Assim é possível distinguir falha de retrieval de perda realmente irreversível causada pela eviction.

No LongMemEval-S, sob retrieval top-k e orçamento de 80k tokens, a fração irreversível entre os erros corrigidos pela restauração ficou em **0,67–0,73** para FIFO, random e redundancy-aware, e **0,60** para eviction baseada em importância do LLM. Com somente 8k tokens, chegou a **1,00 em todas as quatro políticas**.

Este resultado é particularmente importante quando combinado com a Agents API, que oferece compaction automática de sessões. A compaction pode ser excelente para manter a execução andando, mas eu **não a trataria como memória canônica do AI Hub**.

O desenho mais seguro seria:

```text
RAW / CANONICAL EVIDENCE
 repo + decisions + traces + tests + conversations
               │
               ▼
      STRUCTURED PROJECT MEMORY
               │
               ▼
        RETRIEVAL / ROUTING
               │
               ▼
        SESSION COMPACTION
               │
               ▼
        CURRENT CONTEXT
```

Assim, se a sessão compactada perder um detalhe necessário, o agente ainda pode recuperá-lo novamente via MCP/evidence store.

**Limitação:** é um estudo de benchmark conversacional com leitores específicos; não mede diretamente compaction de Codex/Agents API nem tarefas de engenharia reais. A aplicação aqui é uma inferência arquitetural, não uma prova de falha do produto da OpenAI.

Fonte: https://arxiv.org/abs/2609.08279

# O principal insight de hoje

A novidade mais útil não é uma técnica isolada, mas a combinação dos trabalhos:

```text
                    AI HUB

              USER INTENT
                   │
                   ▼
          REQUIREMENT COMPILER
                   │
                   ▼
            TASK BELIEF STATE
                   │
                   ▼
        CONTEXT / MEMORY ROUTER
                   │
       ┌───────────┼───────────┐
       │           │           │
 Evidence      Structured    Skills
  Store         Memory       Library
       │           │           │
       │           │      Skill Router
       │           │       ↙       ↘
       │           │    inline   subagent
       └───────────┼───────────┘
                   ▼
             ACCEPTANCE CONTRACT
                   │
                   ▼
       CODEX / AGENTS API HARNESS
   compaction / tool search / MCP / subagents
                   │
                   ▼
               VERIFIER
                   │
                   ▼
          EXPERIENCE / DIAGNOSIS
                   │
                   ▼
        HARNESS EVOLUTION CONTROLLER
       scoped patch + eval + rollback
```

A recomendação prática para o AI Hub agora é **não competir com o Codex harness em tudo**. Vale investigar usar o novo Agents API como execution substrate e manter no AI Hub a camada que pode ser realmente diferenciadora: entendimento de objetivo, inferência de requisitos implícitos, memória/evidência própria, roteamento de skills, critérios independentes de aceitação e evolução controlada do harness.

Ao mesmo tempo, não delegaria a memória canônica do projeto ao contexto/sessão do runtime. Os trabalhos sobre MeClear e eviction indicam que **mais contexto pode atrapalhar e apagar contexto pode causar perdas irreversíveis**. O ideal é o runtime receber uma view ativa e descartável de uma base de evidência externa e persistente.

## Prioridade sugerida para implementação

1. `Canonical Evidence Store` externo à sessão do agente.
2. `Requirement/Belief State` sobre essa evidência.
3. `SkillExecutionRouter` (`INLINE` vs `SUBAGENT`) baseado em contratos de I/O.
4. Integração experimental do Agents API como execution plane.
5. `Harness Evolution Controller` com patches pequenos, snapshots, held-out evals e rollback.
6. Métricas específicas para verificar se contexto recuperado realmente alterou o plano e a execução.

## Fontes principais

- OpenAI — Agents API, 10/09/2026: https://openai.com/index/introducing-the-agents-api/
- RobustSGPO: https://arxiv.org/abs/2609.09646
- Subagents vs Agent Skills: https://arxiv.org/abs/2609.09233
- MeClear: https://arxiv.org/abs/2609.09115
- What Eviction Destroys: https://arxiv.org/abs/2609.08279
