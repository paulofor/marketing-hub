# Radar diário — agentes mais inteligentes | 18/09/2026

A rodada de hoje foi especialmente forte: a listagem recente do arXiv trouxe vários trabalhos submetidos em **17 de setembro de 2026** diretamente sobre **agent harnesses, context management, stateful retrieval, skill evolution e verificação**. O ponto comum é que o ganho não vem de um único “prompt melhor”, mas de políticas explícitas do harness para decidir **o que manter no contexto, quando planejar, como recuperar experiência passada, onde atribuir uma falha e quando bloquear uma conclusão**.

A principal conclusão para o AI Hub hoje é: **o harness deve ser adaptativo ao modelo, ao orçamento de contexto e ao risco da tarefa**. Componentes como planning, recall, ferramentas estruturadas e verifier não têm valor constante; precisam ser ativados quando há evidência de que ajudam.

## 1. An Empirical Study of Harness Design for Coding Agents

**Tipo:** pesquisa acadêmica / preprint, submetido em 17/09/2026.

Este é um dos trabalhos mais úteis que apareceu desde o início do radar porque faz algo raro: mantém o loop principal fixo e varia **três componentes do harness** — planning, action space e context management — em **176 configurações**, quatro modelos, SWE-Bench Verified e Terminal-Bench 2.1.

Os resultados mostram:

- context management ajuda mais quando a janela é apertada, principalmente evitando context overflow;
- fazer **elision determinística antes de summarization por LLM** foi a estratégia de melhor eficiência geral;
- manter conteúdo removido recuperável via recall adicionou mecanismo, mas foi raramente usado e não melhorou a acurácia sobre elision sozinho;
- planning ajudou modelos mais fracos a chegar até a edição; em modelos mais fortes, funcionou mais como redução de custo do que como aumento de acurácia;
- ferramentas estruturadas ajudam modelos fracos em shell; modelos fortes em bash podem operar bem com interface muito mais simples e barata.

Um detalhe especialmente relevante para a arquitetura do AI Hub: o harness guarda o plano como estado persistente **fora do histórico da conversa** e o injeta a cada turno. Para context recall, observações removidas são salvas em **filesystem** e recuperáveis por `recall_event`.

### Aplicação ao AI Hub

Não faria uma política fixa como:

```text
sempre planning
sempre RAG
sempre recall
sempre 20 tools
```

Faria um `Harness Policy Router`:

```text
modelo + tipo de tarefa + contexto disponível + risco
                         │
                         ▼
                 HARNESS POLICY
        planning: on/off
        recall: on/off
        summarization: threshold
        tools: minimal/structured
        verifier: light/strong
```

Isso também é uma evidência contra colocar complexidade no harness apenas porque ela parece conceitualmente boa. O componente precisa provar valor em evals.

Fonte: https://arxiv.org/abs/2609.20804

## 2. SoL-Pi — o harness sendo melhorado por auto-research loops

**Tipo:** pesquisa acadêmica + engenharia de laboratório (NVLabs), submetido em 17/09/2026.

O SoL-Pi usa agentes para pesquisar melhorias do próprio harness. O processo começou com **152 direções candidatas**, executou loops independentes de pesquisa e retenção, e apenas **quatro mecanismos sobreviveram** para a versão final:

1. **Action Fusion** — funde uma edição com a ação subsequente de testar/executar;
2. **Online Context Compact** — considera compactação em fronteiras semânticas de subtarefas, não apenas por limite global de tokens;
3. **ObservationPack** — arquiva payloads grandes e mantém apenas handle + trecho, preservando recall exato;
4. **Evidence-Preserving Reducer** — delega leitura de logs longos, mas só aceita o resumo se as evidências citadas puderem ser verificadas no log original.

No EdgeBench de 51 tarefas, SoL-Pi ficou próximo ao Pi em qualidade usando GPT-5.6 Sol e Opus 5, mas reduziu o tráfego de tokens em **44,7%–49,0%** e o custo de API em cerca de **um terço**. A página do projeto reporta que o harness mantém aproximadamente 94% do score médio do Pi, com reduções maiores de tokens/custo.

O loop de melhoria também é interessante:

```text
Trajectory Rollouts
      ↓
Map-Reduce Analysis
      ↓
Proposal
      ↓
Implementation
      ↓
Independent Reviewer
      ↓
In-Trajectory Validation
      ↓
Held-Out Validation
```

Só mudanças que preservam um capability floor e melhoram eficiência são mantidas.

### Aplicação ao AI Hub

Esse é praticamente o desenho que eu adotaria para `Harness Evolution`:

```text
traces reais do AI Hub
        ↓
mineração de desperdício/falhas
        ↓
proposta de UM mecanismo
        ↓
implementação isolada
        ↓
review independente
        ↓
regression suite
        ↓
held-out eval
        ↓
commit ou rollback
```

E há uma conexão direta com a sua pergunta de ontem sobre **banco versus arquivos**. O projeto relata três gerações de orquestração:

```text
1. workflow compilado em YAML
2. orchestration code persistente
3. disposable skill loop
```

Eles acabaram preferindo um **template mínimo de skill/loop que é instanciado por experimento e descartado depois**, em vez de fazer crescer indefinidamente um grafo YAML ou um grande coordenador persistente. Isso reforça a ideia de deixar **definição/versionamento do harness em código/configuração** e usar banco apenas para estado, traces, métricas e memória estruturada.

Fontes:
- https://arxiv.org/abs/2609.20519
- https://nvlabs.github.io/SoL-Pi/

## 3. RAFT — retrieval deve considerar o estado atual da trajetória

**Tipo:** pesquisa acadêmica, aceito no EMNLP 2026 Industry Track; submetido ao arXiv em 17/09/2026.

O RAFT observa que RAG tradicional trata casos históricos como documentos estáticos. Em troubleshooting real, porém, o significado de um caso depende de **em que etapa da trajetória estamos**.

O sistema transforma cada caso fechado em uma cadeia dirigida de entradas de timeline e faz retrieval no nível dessas entradas. Quando encontra um estado intermediário semelhante ao caso atual, retorna a trajetória do caso pai ancorada naquele ponto.

Em benchmark sintético derivado de documentação Microsoft Windows Server e em issues reais do Apache Jira, RAFT melhorou `Case Hit` sobre RAG e GraphRAG em todas as etapas do progresso do caso, com ganhos estatisticamente significativos sobre o baseline mais forte; os dados do Jira dão evidência direcional de transferência para casos reais.

### Aplicação ao AI Hub

Isso sugere que a memória de experiências não deveria recuperar apenas por:

```text
"Google OAuth Spring"
```

mas também por **estado da tarefa**:

```yaml
current_state:
  goal: add_external_auth
  discovered:
    existing_login: true
    session_authority: backend
  unresolved:
    refresh_token_policy
  stage: architecture_discovery
```

A consulta então busca experiências anteriores que estavam em **estado semelhante**, e não simplesmente documentos semanticamente parecidos.

Eu adicionaria uma `StateSignature` à memória de trajetórias.

Fonte: https://arxiv.org/abs/2609.20754

## 4. SkillAA — falha deve ser atribuída a um ponto específico da skill antes de alterá-la

**Tipo:** pesquisa acadêmica / preprint, submetido em 17/09/2026.

SkillAA representa applicability, execution e composition de skills em um único grafo. A contribuição mais importante não é apenas usar um grafo, mas fazer **abductive attribution**: comparar execuções bem-sucedidas e falhas para localizar qual objeto do grafo provavelmente causou o problema.

Só a estrutura local atribuída recebe uma proposta de alteração. Depois ela passa por `Local Gate` e `Big Gate` antes de ser aceita, com possibilidade de rollback.

Com GPT-5.6 Sol, o paper reporta **81,5% em SearchQA, 66,7% em LiveMath e 91,2% em DocVQA**, com a maior média observada nas configurações principais do estudo.

### Aplicação ao AI Hub

Em vez de:

```text
falhou OAuth
→ reescrever skill oauth inteira
```

usar:

```text
falha
  ↓
atribuição
  ↓
qual nó/aresta/regra causou?
  ↓
patch local
  ↓
local eval
  ↓
regression eval global
  ↓
commit / rollback
```

Isso encaixa muito bem com o que já vínhamos chamando de **component attribution** e reduz regressões provocadas por self-improvement amplo demais.

Fonte: https://arxiv.org/abs/2609.20455

## 5. How Do Agent Harnesses Create Value? — planning e verifier resolvem problemas diferentes

**Tipo:** pesquisa acadêmica / preprint, submetido em 17/09/2026.

Esse trabalho tenta separar dois mecanismos que normalmente aparecem misturados: **planning guidance** e **release control / verification**.

Em 265 células pareadas do τ²-bench, planos específicos da tarefa melhoraram o sucesso verificado por oracle em **7,17 pontos percentuais**, com maior ganho em tarefas mais complexas. Separadamente, um verifier terminal read-only rejeitou **61% dos episódios inválidos**, embora também tenha segurado **17% dos episódios corretos**, a custo adicional inferior a US$ 0,01 por episódio.

A conclusão é especialmente útil: planning melhora a produção de uma boa trajetória; verifier reduz false pass. Dependendo do custo de aceitar uma solução errada, o valor relativo dos dois componentes muda.

### Aplicação ao AI Hub

Eu manteria esses dois componentes separados:

```text
PLANNER
"como chegar a uma solução?"

VERIFIER
"a solução realmente satisfaz o contrato?"
```

E faria o nível do verifier depender do risco:

```text
baixo risco
→ verifier barato

alto risco
→ verifier independente + evidence gate
```

Isso evita gastar o mesmo orçamento de verificação em todas as tarefas.

Fonte: https://arxiv.org/abs/2609.20474

## O desenho que eu adotaria depois da rodada de hoje

```text
                         USER REQUEST
                              │
                              ▼
                  REQUIREMENT DISCOVERY
                              │
                              ▼
                         TASK STATE
                              │
                              ▼
                    STATEFUL RETRIEVAL
               memory + evidence + trajectories
                              │
                              ▼
                    HARNESS POLICY ROUTER
          model / budget / risk / task complexity
                  │        │        │
               planning   tools   context policy
                  │        │        │
                  └────────┼────────┘
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
                FAILURE / COST ATTRIBUTION
                           │
                           ▼
                  HARNESS AUTO-RESEARCH
                           │
                 isolated patch + eval
                           │
                           ▼
                   commit / rollback
```

A principal mudança conceitual de hoje é esta:

> **o harness não deveria ser uma configuração fixa; deveria ser uma política selecionada para a tarefa atual e melhorada por experimentos isolados com evals.**

Isso também muda a discussão de armazenamento. O material de hoje reforça um desenho híbrido para o AI Hub:

```text
Git / arquivos
→ prompts, policies, skill templates, harness code

MySQL
→ task state, beliefs, trajectories, evals, métricas, versões

filesystem/object storage
→ logs grandes, tool outputs, artifacts, context archive
```

Eu não adicionaria um novo banco especializado agora. O que os trabalhos de hoje mostram é que **a qualidade da representação e da política de acesso importa mais que o produto de banco utilizado**.
