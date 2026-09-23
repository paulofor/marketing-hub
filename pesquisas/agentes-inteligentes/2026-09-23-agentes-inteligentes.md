# Radar diário — agentes mais inteligentes | 23/09/2026

A leva publicada em 22 de setembro trouxe trabalhos especialmente relevantes para o objetivo deste radar. O padrão mais forte de hoje é: **não deixar o modelo reconstruir sempre as mesmas decisões dentro do contexto; transformar controle recorrente em mecanismo persistente do harness, mas submeter crenças, mudanças e conclusão a gates verificáveis.**

## 1. Grow the Harness, Not the Context — mover controle recorrente para o harness

**Tipo:** pesquisa acadêmica / preprint, submetido em 22/09/2026.

O trabalho **Grow the Harness, Not the Context: From Strategy-Free Scaffolds to Reusable Specialist Agents** parte de um scaffold sem estratégia fixa. Os traces de execução localizam falhas em superfícies pequenas de código; um otimizador corrige janelas de falhas; e um gate held-out faz rollback quando uma sequência de alterações prejudica capacidades anteriores. As correções aprovadas se acumulam no mesmo harness.

Nos benchmarks BrowseComp-Plus e WebArena-Verified, com modelos de 4B a 120B, o método obteve o melhor sucesso médio em cinco de seis combinações e ficou apenas 0,7 ponto percentual atrás na sexta. Em relação a um agente Tool-Calling, reduziu chamadas ao LLM em 76,0%–91,8% e custo de inferência em 74,4%–98,6%. No WebArena-Verified, o sucesso ficou entre 44,7% e 45,3% mesmo mudando a escala do modelo, enquanto o Tool-Calling caiu para 6,7% com o modelo de 4B.

**Aplicação ao AI Hub:** regras recorrentes como “antes de alterar autenticação, descubra o fluxo existente”, “confirme o owner da sessão” ou “não conclua sem regressão do login legado” não deveriam precisar ser redescobertas pelo LLM em toda tarefa. Elas podem migrar para componentes executáveis e testáveis do harness.

```text
trajectory failure
      ↓
semantic/function attribution
      ↓
small harness patch
      ↓
held-out regression gate
   /                \
accept              rollback
   ↓
persistent harness capability
```

Isso também reforça uma direção prática para o AI Hub: **crescer o harness não precisa significar aumentar prompt/contexto**. A mudança pode viver em componentes carregados dinamicamente e versionados separadamente da imagem do sandbox, evitando que toda evolução dependa de rebuild.

**Limitação:** os autores avaliam web agents, não descoberta de requisitos de software. A transferência para Requirement Discovery é arquitetural, não uma evidência direta desse domínio.

Fonte: https://arxiv.org/abs/2609.26760

## 2. Dual-Frontier — se a crença que guia o plano não estiver suficientemente verificada, compre evidência

**Tipo:** pesquisa acadêmica / preprint, submetido em 22/09/2026.

**Dual-Frontier: When Can an Agent Trust Its World Model?** formaliza um problema central para agentes: quando uma decisão guiada por um world model falha, o trace sozinho pode não dizer se a falha veio da política de decisão ou do próprio modelo do mundo. O trabalho propõe um gate: uma decisão baseada no world model só é aceita quando a vantagem prevista supera um limite certificado para o erro relevante do modelo; caso contrário, o orçamento é direcionado para **verificação do world model**.

O paper apresenta bounds condicionados à ação, extensão closed-loop e reaproveitamento adaptativo de evidência. Em benchmarks de tool use com diferentes backbones, a regra **verify-then-promote** melhora qualidade e confiabilidade das decisões.

**Aplicação ao AI Hub:** o `Belief State` não deveria ser apenas uma tabela de confidence. Toda crença importante poderia ter uma política de promoção:

```yaml
belief:
  statement: backend_controls_session
  status: INFERRED
  decision_impact: HIGH
  verification_required: true
  evidence_gap:
    - gateway_auth_flow
    - integration_test
```

Se a crença tiver alto impacto e baixa sustentação, o próximo passo não é continuar planejando. É comprar evidência via repo, MCP, teste, runtime ou usuário.

```text
BELIEF
  ↓
decision impact × uncertainty
  ↓
VERIFY GATE
  ├── sufficient → promote and plan
  └── insufficient → acquire evidence
```

Essa é uma evolução do `Ask-or-Infer Gate` dos últimos dias: agora temos um motivo formal para decidir **quando parar de confiar no estado interno e verificar o mundo**.

**Limitação:** a teoria é construída em torno de world models e retorno, não especificamente em requisitos implícitos. A aplicação ao Belief State é uma adaptação conceitual.

Fonte: https://arxiv.org/abs/2609.26293

## 3. The Tasteful Agent — mais reasoning não conserta necessariamente uma decisão ruim de trajetória

**Tipo:** pesquisa acadêmica / preprint, submetido em 22/09/2026.

**The Tasteful Agent: Measuring and Improving Taste in Long-Horizon Tasks** introduz o Taste-Bench. Em vez de avaliar apenas o resultado final, o benchmark captura **forks de decisão**: pontos em que o agente pode seguir caminhos diferentes e um deles produz resultado melhor. As questões são extraídas de tentativas paralelas e desvios observados em trajetórias reais de engenharia e pesquisa.

O melhor modelo avaliado acertou apenas **59,7%** das decisões. O dado mais importante para este radar é que forks cujo fator decisivo aparece mais tarde na trajetória são muito mais difíceis, e **aumentar o orçamento de reasoning não melhora a acurácia**. O trabalho ainda mostra que julgamento baseado em resultados futuros pode ser destilado para outro modelo, melhorando decisões em tarefas não vistas e desempenho end-to-end em SWE-bench Pro held-out.

**Aplicação ao AI Hub:** isso indica que `test-time compute` sozinho não resolve o problema de requisitos implícitos. Se a evidência relevante ainda não entrou no contexto, “pensar mais” pode apenas aprofundar uma direção errada.

Eu criaria um `Fork Evaluator` antes de decisões irreversíveis ou caras:

```text
candidate strategy A
candidate strategy B
candidate strategy C
        ↓
what evidence would distinguish them?
        ↓
retrieve / test / inspect
        ↓
choose branch
```

E transformaria traces anteriores em exemplos de “fork bom versus fork ruim”, não apenas em memória textual.

**Limitação:** “taste” depende dos rollouts disponíveis e da definição de melhor trajetória; o conceito pode herdar vieses do benchmark e do professor usado na destilação.

Fonte: https://arxiv.org/abs/2609.25804

## 4. SWE-Serve — passar nos testes locais não prova que a tarefa está correta em produção

**Tipo:** pesquisa acadêmica / benchmark, submetido em 22/09/2026.

**SWE-Serve: Benchmarking Agentic Engineering For Production Inference Serving** cria 53 tarefas reais de engenharia de serving no SGLang, com hidden functional tests, regression tests, E2E serving tests e performance gates. Entre 11 modelos e 31 configurações de esforço, a melhor configuração atingiu 75% de pass@1.

O resultado especialmente útil para o AI Hub é o gap entre “parece pronto localmente” e “está correto em produção”. Nas 19 tarefas com cobertura E2E, testes end-to-end rejeitaram aproximadamente **um terço dos patches que haviam passado todos os outros testes**: 69,4% passariam sem E2E, mas apenas 45,9% com E2E incluído na avaliação.

**Aplicação ao AI Hub:** o `Completion Gate` deveria ter níveis diferentes de evidência:

```text
local implementation
      ↓
unit/targeted tests
      ↓
regression tests
      ↓
E2E / integration context
      ↓
production-relevant contract
      ↓
DONE
```

Isso é diretamente útil para requisitos implícitos. Muitas vezes o requisito que o usuário não escreveu só se manifesta no ambiente de integração: compatibilidade, lifecycle, sessão, configuração, performance, fallback, observabilidade etc.

**Limitação:** o benchmark é específico de serving de modelos e SGLang, portanto os números não generalizam para todo tipo de software.

Fonte: https://arxiv.org/abs/2609.26777

## 5. Type-Safe Is Not Error-Free — decisão estruturada pode continuar semanticamente errada

**Tipo:** pesquisa acadêmica / preprint, submetido em 22/09/2026.

Este paper é um alerta direto para arquiteturas que usam uma camada rápida e tipada de decisão. Os autores testam Jev e modelos semelhantes e mostram que **schema correto não garante interpretação correta do significado das opções**. Em 1.200 decisões de workflow, trocar nomes de opções de `0/1` para `no/yes`, mantendo rubricas e estado iguais, alterou 70,4 respostas a cada 100 e moveu AUC de 0,94 para 0,23 em uma configuração. A taxa de erro de tipo permaneceu em 0%.

**Aplicação ao AI Hub:** se implementarmos routers tipados como:

```text
INFER
RETRIEVE
ASK
STOP
```

não devemos presumir que o modelo segue fielmente a rubrica associada a cada label. Para decisões críticas, eu usaria identificadores semanticamente neutros, rubricas separadas, testes de invariância a renomeação e verificação externa da decisão.

**Limitação:** é um estudo específico de decision heads e naming; não implica que toda saída estruturada seja instável.

Fonte: https://arxiv.org/abs/2609.26758

## Atualização importante de memória persistente

**A Survey on Long-Term Memory Security in LLM Agents**, aceito no EMNLP 2026, recebeu uma nova revisão em **22/09**. A versão atual organiza segurança de memória em seis fases — Write, Store, Retrieve, Execute, Share/Propagate e Forget/Rollback — e defende que provenance, versionamento e políticas de retenção precisam existir desde a gravação. Segurança adicionada apenas no retrieval ou na execução é insuficiente.

Para o AI Hub isso reforça a separação:

```text
candidate memory
      ↓
provenance + trust + version
      ↓
policy-aware storage
      ↓
retrieval
      ↓
execution influence
      ↓
rollback / forget
```

Ou seja: memória persistente precisa ser governada como **estado versionado e reversível**, não como texto confiável acumulado.

Fonte: https://arxiv.org/abs/2604.16548

## Mudança arquitetural que eu faria hoje

A arquitetura consolidada passa a ter um novo ponto central: **Fork + Verification Gate**.

```text
                         USER REQUEST
                              │
                              ▼
                   REQUIREMENT DISCOVERY
                              │
                              ▼
                        BELIEF STATE
                              │
                              ▼
                     DECISION / FORK STATE
                              │
               ┌──────────────┴──────────────┐
               │                             │
        evidence sufficient?          insufficient
               │                             │
              yes                            ▼
               │                    ACTIVE VERIFICATION
               │                  repo / MCP / test / user
               │                             │
               └──────────────┬──────────────┘
                              ▼
                       HARNESS POLICY
                              │
                              ▼
                          EXECUTOR
                              │
                              ▼
                   MULTI-LAYER COMPLETION
                 unit → regression → E2E
                              │
                              ▼
                         TRAJECTORY
                              │
                              ▼
                    FAILURE ATTRIBUTION
                              │
                              ▼
                   SMALL HARNESS PATCH
                              │
                              ▼
                    HELD-OUT / ROLLBACK
```

Minha prioridade prática depois desta rodada seria:

1. **Belief Verification Gate**: crença de alto impacto não entra no plano sem evidência suficiente.
2. **Fork Evaluator**: decisões com múltiplos caminhos devem explicitar qual evidência diferencia as alternativas.
3. **Growing Harness loop**: falhas recorrentes viram pequenos patches persistentes, não mais prompt/contexto.
4. **Completion Gate em camadas**: unitário, regressão, integração/E2E e contratos de produção.
5. **Memory governance desde o write**: provenance, versão, trust state e rollback.

A conclusão principal de hoje é: **“pensar mais” não é equivalente a “descobrir melhor”.** Quando falta evidência, o harness precisa interromper a trajetória, identificar o fork relevante, buscar a informação que discrimina as alternativas e só então promover a crença para uma decisão. E, quando a mesma descoberta passa a se repetir em várias tarefas, ela deve migrar do contexto para uma capacidade persistente e testada do harness.
