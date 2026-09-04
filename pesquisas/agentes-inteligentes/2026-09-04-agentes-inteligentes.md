# Radar de Agentes Inteligentes — 2026-09-04

## Resumo executivo

Hoje surgiram sinais particularmente fortes de que a arquitetura de agentes está caminhando para quatro ideias complementares:

1. **transformar instruções vagas em critérios executáveis antes de agir**;
2. **separar conhecimento operacional reutilizável do contexto bruto**;
3. **tratar o harness como uma parte aprendível e específica do modelo, não como um invólucro neutro**;
4. **manter estado, evidência e trajetória de decisão de forma explícita e verificável**.

Para o AI Hub, a principal consequência é que eu evoluiria a ideia do `Requirement & Context Compiler` para um componente mais completo, capaz de produzir não apenas requisitos implícitos, mas também uma **rubrica executável de sucesso**, selecionar habilidades verificadas e registrar a trajetória de decisão.

---

## 1. AutoSciRub — inferir critérios de sucesso antes da execução

**Tipo:** preprint acadêmico  
**Publicado:** 31/08/2026  
**Paper:** *Learning to Evaluate Before Improving: Automatic Rubric Induction for Automatic Research Agents*

O problema estudado é praticamente o mesmo que motivou este radar: tarefas abertas frequentemente não dizem quais análises, métodos, evidências e critérios de sucesso são necessários.

O AutoSciRub propõe uma estratégia **evaluation-first**. Antes de executar a pesquisa, o sistema:

```text
pedido incompleto
   ↓
decomposição em objetivos atômicos
   ↓
busca literatura/dados relevantes
   ↓
criação de critérios específicos e verificáveis
   ↓
execução
   ↓
verificação por critério
   ↓
revisão dirigida
```

### Evidência

Os autores reportam melhora média de **2,08 pontos** em três backbones sob o mesmo harness Codex e **2,95 pontos** em três harnesses usando o mesmo DeepSeek-V4-Flash. Em uma amostra de 20 tarefas do AstaBench E2E Discovery, o ganho médio reportado foi de **16,8 pontos**.

### Limitação

O estudo é focado em agentes científicos. A indução automática de uma rubrica também pode cristalizar uma interpretação errada do objetivo se os critérios inferidos não forem adequadamente ancorados em evidência.

### Aplicação no AI Hub

Eu adicionaria uma etapa chamada `Success Contract Compiler` logo depois do `Requirement Compiler`:

```yaml
goal: implementar login Google
explicit:
  - login via Google
inferred:
  - preservar login existente
  - proteger secrets
unknown:
  - estratégia atual de sessão
success_criteria:
  - callback válido
  - sessão criada
  - usuário persistido/associado corretamente
  - testes existentes continuam passando
  - nenhum secret foi adicionado ao repo
verification:
  - integration test
  - security/config check
  - regression tests
```

A ideia é poderosa porque o agente não apenas deduz "o que falta", mas transforma essas deduções em **critérios que depois precisam ser comprovados**.

**Fonte:** https://arxiv.org/abs/2608.31076

---

## 2. HarnessDev — o próprio agente pode criar/evoluir seu harness, mas os ganhos são instáveis

**Tipo:** preprint acadêmico / benchmark  
**Publicado:** 01/09/2026  
**Paper:** *HarnessDev: Can LLMs Create and Evolve Their Own Agent Harness?*

HarnessDev muda o objeto de avaliação: em vez de avaliar apenas a resposta final, avalia se o modelo consegue **construir e evoluir a infraestrutura que o transforma em agente**.

Há duas fases:

```text
CREATION
modelo recebe seed mínimo
→ constrói harness executável

EVOLUTION
harness executa tarefas
→ recebe feedback
→ modifica o próprio harness
→ nova avaliação
```

### Evidência

O benchmark cobre seis modelos criadores, quatro domínios, cinco benchmarks e **2.207 instâncias downstream**. Os harnesses gerados por modelos ainda ficam claramente atrás dos maduros feitos por humanos em código e busca/pesquisa, embora consigam igualar ou superar referências selecionadas em escrita e experimentação de ML. A evolução melhora alguns resultados, mas de forma instável e com transferência parcial para tarefas ocultas.

Um achado especialmente importante: o benefício de um harness depende fortemente do **modelo que o executa**.

### Limitação

Ainda não há evidência de que um agente possa autonomamente evoluir um harness geral e robusto. O risco de overfitting ao conjunto usado para evolução permanece alto.

### Aplicação no AI Hub

Isso sugere não tratar o harness como configuração universal:

```text
HarnessProfile
  model_family
  model_version
  task_family
  tools
  strategies
  measured_effect
  evaluation_set
```

Uma mudança no harness deveria carregar evidência de quais modelos/tarefas ela realmente melhora.

**Fonte:** https://arxiv.org/abs/2609.01437

---

## 3. Repo-To-Skill / DisCo — transformar repositórios em conhecimento operacional reutilizável

**Tipo:** preprint acadêmico  
**Publicado:** 02/09/2026  
**Paper:** *Repo-To-Skill: Distilling GitHub Repositories Into AI4AI Skills*

O paper identifica uma camada que muitos agentes ainda não possuem: **operational knowledge** — saber não só *o que* fazer, mas *como fazer de forma confiável*.

Esse conhecimento existe em repositórios e papers, porém é grande demais e escrito para humanos. O DisCo destila essas fontes em skills compactas e verificadas.

Os autores criaram uma biblioteca com mais de **5.000 skills**, extraídas de 1.000 repositórios de ML e organizadas em 20 áreas e 178 famílias de capacidades.

### Evidência

Com o backbone, harness e orçamento downstream mantidos fixos, adicionar skills produziu os seguintes ganhos reportados:

- **+134,3%** no MLE-bench;
- **+34,4%** no PaperBench;
- **+9,2%** no FrontierCS;
- **+14,0%** no PassNet.

### Limitação

Os resultados são no domínio de pesquisa em ML e com pipeline específico de criação/verificação de skills. Não se pode assumir que qualquer resumo automático de documentação terá o mesmo efeito.

### Aplicação no AI Hub

Seu MCP Server poderia fornecer mais do que documentos crus. Ele poderia expor um `Skill Registry`:

```text
Skill: google-oauth-spring

trigger:
  tarefa envolve Google OAuth + Spring

contains:
  workflow
  constraints
  known pitfalls
  reference files
  verification steps

provenance:
  repo/docs/commits

validated:
  true
```

Assim, quando você escreve apenas "adicione login Google", o harness recupera uma **capacidade operacional verificada**, e não dezenas de páginas de contexto bruto.

**Fonte:** https://arxiv.org/abs/2609.02749

---

## 4. Transfiver — memória como estado compartilhado, explícito e editável

**Tipo:** preprint acadêmico / proposta arquitetural  
**Publicado no arXiv:** 03/09/2026; listado em 04/09/2026  
**Paper:** *Transfiver: Human-AI Co-Inference through a Shared Editable State*

O Transfiver ataca um problema importante de interações longas: informações relevantes vão sendo inferidas implicitamente pelo modelo, mas o usuário não consegue inspecionar ou corrigir diretamente esse estado.

A proposta mantém um estado persistente `S_t` que pode ser atualizado de duas maneiras:

```text
implicit stream update
modelo deduz que uma nova interação cria/altera estado

explicit directed edit
usuário altera diretamente um item do estado
```

O ponto interessante é que uma correção não vira simplesmente "mais uma mensagem" no histórico. Ela modifica o estado que as próximas decisões realmente leem.

### Evidência

Neste estágio, o trabalho é principalmente arquitetural. O resumo não apresenta um benchmark amplo que prove superioridade em tarefas complexas.

### Limitação

Os próprios autores deixam como problema aberto estender o mecanismo para estados naturais, relacionais e grandes.

### Aplicação no AI Hub

Isso encaixa muito bem em uma camada que eu chamaria de `Task Belief State`:

```yaml
project: marketing-hub
current_goal: integrar OAuth
facts:
  - auth backend controla sessão
constraints:
  - secrets não vão para o repo
inferences:
  - provável necessidade de preservar login atual
unknowns:
  - estratégia de refresh token
corrections:
  - item: inferred_requirement_12
    old: "frontend persiste token"
    new: "backend é autoridade da sessão"
```

Esse estado deveria ser inspecionável e editável sem reconstruir todo o prompt.

**Fonte:** https://arxiv.org/abs/2609.03797

---

## 5. DNative-Twin — registrar a trajetória de decisão, não apenas o resultado

**Tipo:** preprint acadêmico  
**Publicado:** 03/09/2026; atualizado/listado em 04/09/2026  
**Paper:** *DNative-Twin: Decision Graphs and Digital Twins for Reconstructable Agentic Decisions*

O trabalho argumenta que a saída final de um agente não mostra quais evidências, estados de ferramentas, regras, autorizações e caminhos levaram à decisão.

A proposta registra cada decisão como uma trajetória tipada em grafo e depois consegue **reexecutar/reproduzir** o mecanismo sob condições declaradas.

### Evidência

Em experimento controlado com 300 instâncias injetadas, o recall de divergências não resolvidas foi de **0** apenas com a estrutura básica, subiu para **0,667** quando o estado necessário para replay foi incluído e chegou a **1,0** quando também havia resultados de verificação. O custo é relevante: em conjuntos de 500 a 5.000 casos BPI 2020, o tempo mediano fim a fim reportado aumentou de 0,794 s para 8,889 s.

### Limitação

Há overhead significativo, e o próprio trabalho mostra que um grafo não consegue explicar consequências de estados de ferramenta que nunca foram observados/registrados.

### Aplicação no AI Hub

Para tarefas importantes, eu registraria:

```text
DecisionNode
  goal
  evidence_used[]
  assumptions[]
  constraints_triggered[]
  tool_state[]
  action
  observation
  verification
  resulting_state
```

Isso permitiria ao critic responder algo muito melhor do que "acho que está certo": ele poderia verificar **qual requisito foi atendido por qual evidência e qual ação**.

**Fonte:** https://arxiv.org/abs/2609.03787

---

# Síntese para o AI Hub

A arquitetura que emerge dos trabalhos de hoje é esta:

```text
USER INTENT
    │
    ▼
Requirement Compiler
    │
    ▼
Success Contract / Rubric Induction
    │
    ├── explícito
    ├── inferido
    ├── desconhecido
    └── critérios verificáveis
    │
    ▼
Belief State
(fatos / inferências / correções)
    │
    ▼
Skill + Context Router
(MCP / repo / memória / skills)
    │
    ▼
Planner
    │
    ▼
Executor
    │
    ▼
Verifier
    │
    ▼
Decision Trace
    │
    ▼
Harness / Skill Learning
```

## A mudança mais importante que eu faria agora

Ontem a prioridade era um `Requirement Compiler`. Depois dos trabalhos de hoje, eu o ampliaria para produzir um **Task Contract executável**:

```yaml
goal:
explicit_requirements: []
inferred_requirements: []
unknowns: []
evidence_needed: []
applicable_constraints: []
selected_skills: []
success_criteria: []
verification_plan: []
```

A diferença é crucial:

> inferir um requisito não basta; o agente precisa dizer **como saberá que essa inferência era relevante e que o objetivo foi realmente atingido**.

Isso reduz o risco de o agente simplesmente inventar necessidades adicionais e tratá-las como verdade.

---

## Sinal geral do dia

O resultado mais consistente desta rodada não é "mais reflexão" nem "mais contexto". É **estrutura externa explícita**:

- contexto relevante vira estado;
- know-how vira skill;
- intenção vaga vira contrato/rubrica;
- execução vira trajetória verificável;
- mudanças no harness são medidas por efeito real e por modelo.

Esse conjunto é, na prática, uma definição cada vez mais concreta de **harness engineering**.