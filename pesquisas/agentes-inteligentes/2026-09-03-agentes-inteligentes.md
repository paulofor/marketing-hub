# Radar de Agentes Inteligentes — 2026-09-03

## Tema do radar

Como fazer um agente de IA perceber requisitos, riscos, dependências e contexto relevante que o usuário **não colocou explicitamente no prompt**.

A tese central continua sendo: o prompt deve ser tratado como uma **expressão parcial de intenção**, não como uma especificação completa. A inteligência operacional do agente cresce quando o harness consegue descobrir contexto, inferir requisitos, manter memória útil, distinguir fatos de hipóteses, verificar execução e aprender com falhas.

---

# Atualização diária — 03/09/2026

## Resumo executivo

A pesquisa de hoje reforça uma direção muito clara: os agentes mais capazes não estão sendo construídos apenas com prompts melhores. Os trabalhos recentes estão adicionando **camadas especializadas antes, durante e depois da chamada ao LLM**.

Os cinco sinais mais úteis para o AI Hub hoje são:

1. transformar pedidos vagos em requisitos estruturados antes de executar;
2. manter memória com proveniência, em vez de apenas embeddings de conversa;
3. tratar restrições latentes como gatilhos que precisam ser lembrados proativamente;
4. guardar o estado de execução, e não somente fatos semanticamente parecidos;
5. fazer descoberta de contexto e ferramentas sob demanda para evitar context bloat.

Minha conclusão prática é que o próximo componente importante do AI Hub deveria ser algo como um **Requirement & Context Compiler** entre o prompt do usuário e o planner.

---

## 1. Agent Zero Memory — memória com proveniência e múltiplas representações

**Tipo:** preprint acadêmico  
**Publicado:** 30/08/2026

O trabalho *Agent Zero Memory: Provenance-Aware Long-Term Memory for LLM Agents* propõe não depender de uma única forma de memória. O sistema mantém três visões paralelas do histórico:

- linha do tempo episódica de eventos;
- grafo associativo de entidades e eventos;
- memória documental hierárquica de fatos duráveis.

A recuperação começa com um **intent gate**: se a pergunta é autossuficiente, nenhuma memória é carregada. Quando memória é necessária, o sistema roteia a consulta entre as três estruturas e exige proveniência para aquilo que será usado na resposta.

Nos benchmarks LongMemEval e LoCoMo, os autores reportam 95,60% e 93,60%, respectivamente. Um resultado especialmente interessante é que a qualidade varia relativamente pouco entre diferentes modelos-base enquanto o custo por consulta varia muito, sugerindo que uma boa arquitetura de memória pode reduzir a dependência de sempre usar o modelo mais caro.

### Aplicação no AI Hub

Hoje eu não trataria "memória" como uma única tabela ou vector store. Separaria pelo menos:

```text
EpisodicMemory
- o que aconteceu
- quando aconteceu
- em qual tarefa
- resultado

DecisionMemory
- decisão arquitetural
- restrição
- justificativa
- fonte
- validade

Entity/ProjectMemory
- módulos
- serviços
- pessoas/sistemas
- relações
```

E todo item deveria carregar:

```text
source
source_timestamp
confidence
scope
valid_from
valid_until
```

Isso permitiria ao agente diferenciar "Paulo disse que prefere X", "o README afirma Y" e "o agente inferiu Z".

**Fonte:** https://arxiv.org/abs/2608.29606

---

## 2. Requirement-driven sourcing — pedido do usuário como requisito subdeterminado

**Tipo:** preprint acadêmico  
**Publicado:** 24/08/2026

O trabalho *An Interactive Agent for Requirement-Driven Candidate Sourcing* é extremamente próximo do problema que motivou este radar.

Os autores argumentam que um pedido em linguagem natural frequentemente não é apenas uma consulta de busca: é um **requisito subdeterminado**, com restrições implícitas, várias respostas possíveis e critérios de aceitação ausentes.

O sistema proposto não parte imediatamente para busca. Ele passa por um ciclo de:

```text
ELICIT
  ↓
VALIDATE
  ↓
RETRIEVE
  ↓
VERIFY
```

A ideia importante para nós não é recrutamento; é a transformação de uma intenção vaga em uma especificação verificável antes da execução.

### Aplicação no AI Hub

Eu criaria uma etapa obrigatória:

```text
User Prompt
   ↓
Requirement Compiler
   ↓
Structured Task Contract
   ↓
Planner
```

Exemplo de contrato interno:

```yaml
goal: implementar login Google
explicit_requirements:
  - login via Google
inferred_requirements:
  - preservar autenticação existente
  - tratar callback de produção
  - proteger secrets
unknowns:
  - fluxo atual de sessão
  - entidade responsável pelo usuário
acceptance_criteria:
  - login funciona
  - sessão é criada
  - testes existentes continuam passando
evidence_needed:
  - arquitetura de autenticação
  - configuração de ambiente
  - testes atuais
```

A diferença crítica é que **inferred_requirements não são tratados como fatos**. Eles viram hipóteses que o agente tenta verificar em repo, MCP, documentação, banco ou código.

**Fonte:** https://arxiv.org/abs/2608.23501

---

## 3. TriggerBench — lembrar espontaneamente de restrições que não foram repetidas

**Tipo:** pesquisa acadêmica / benchmark  
**Publicado:** junho de 2026

*TriggerBench: Investigating Prospective Memory for Large Language Models* estuda algo diretamente ligado à ideia de "deduzir o que eu esqueci de dizer".

A pesquisa chama de **prospective memory** a capacidade de lembrar e agir sobre uma restrição latente quando a condição apropriada aparece, mesmo sem o usuário repetir a instrução naquele momento.

Exemplo conceitual:

```text
Antes:
"Nunca altere diretamente uma branch protegida."

Muito depois:
"Faça a correção e publique."
```

Um agente com prospective memory deveria recuperar a restrição antiga automaticamente quando percebe que a tarefa envolve publicação no repositório.

O benchmark mostra que isso ainda é difícil: o desempenho cai quando o gatilho é implícito ou quando existem muitas demandas concorrentes. Aumentar reasoning ajuda, mas também pode levar ao comportamento ruim de "lembrar de tudo o tempo todo" e gerar falsos alarmes.

### Aplicação no AI Hub

Isso sugere um componente diferente de RAG convencional:

```text
Constraint Registry

constraint:
  "secrets nunca podem ir para o repo"
trigger:
  task touches authentication/config/deployment
scope:
  marketing-hub
severity:
  critical
```

Antes do planner, o harness verifica quais restrições possuem gatilhos compatíveis com a tarefa atual.

Isso é melhor do que esperar que similaridade vetorial encontre a regra certa.

**Fonte:** https://www.microsoft.com/en-us/research/publication/triggerbench-investigating-prospective-memory-for-large-language-models/

---

## 4. REAgent — melhorar o pedido antes de tentar resolver o problema

**Tipo:** preprint acadêmico  
**Publicado:** 08/04/2026

*REAgent: Requirement-Driven LLM Agents for Software Issue Resolution* parte de uma observação muito útil: agentes de programação normalmente aceitam a descrição de uma issue como se ela fosse uma especificação confiável. Porém, issues frequentemente têm contexto ausente, ambiguidade ou detalhes incompletos.

O REAgent constrói automaticamente **issue-oriented requirements**, identifica requisitos de baixa qualidade e os refina iterativamente antes da geração do patch.

Os autores reportam melhora média de **17,40% no número de issues resolvidas** em relação aos baselines avaliados.

### Aplicação no AI Hub

Esse trabalho dá suporte concreto para uma mudança arquitetural:

**não enviar o prompt diretamente para o executor.**

O fluxo deveria ser:

```text
PROMPT
  ↓
Requirement Analysis
  ↓
Missing Context Detection
  ↓
Context Retrieval
  ↓
Requirement Refinement
  ↓
Plan
  ↓
Execute
```

O agente deve conseguir dizer internamente:

```text
"Ainda não tenho evidência suficiente para afirmar X.
Vou procurar X no projeto antes de planejar."
```

Isso é muito diferente de simplesmente aumentar o system prompt.

**Fonte:** https://arxiv.org/abs/2604.06861

---

## 5. MAGE — memória como estado de execução, não apenas busca semântica

**Tipo:** pesquisa acadêmica  
**Publicado:** junho de 2026

*Beyond Semantic Organization: Memory as Execution State Management for Long-Horizon Agents* argumenta que RAG e memórias organizadas por similaridade semântica não representam bem tarefas longas.

Durante uma execução, existe um estado causal:

```text
objetivo
  ↓
subobjetivo A
  ↓
ação
  ↓
resultado
  ↓
decisão
  ↓
subobjetivo B
```

Recuperar chunks semanticamente parecidos pode misturar caminhos válidos, tentativas fracassadas e estados incompatíveis.

O MAGE organiza a execução como uma árvore hierárquica de estado e oferece operações para crescer, comprimir, validar e revisar caminhos. Os autores reportam melhora de 7,8 a 20,4 pontos percentuais na taxa de sucesso em MemoryArena e redução de 55,1% no consumo de tokens.

### Aplicação no AI Hub

O histórico de uma tarefa deveria ter uma estrutura explícita:

```text
Run
 ├─ Goal
 ├─ Assumptions
 ├─ Retrieved Evidence
 ├─ Plan
 ├─ Step 1
 │   ├─ action
 │   ├─ observation
 │   └─ status
 ├─ Step 2
 ├─ Failed Branches
 └─ Final State
```

Uma branch fracassada pode continuar disponível para aprendizado, mas não deveria contaminar silenciosamente o estado ativo da execução.

**Fonte:** https://www.microsoft.com/en-us/research/publication/beyond-semantic-organization-memory-as-execution-state-management-for-long-horizon-agents/

---

## 6. Sinal de engenharia: harness e context engineering estão virando disciplina própria

**Tipo:** engenharia / experiência de produção, não paper acadêmico

Dois textos recentes da OpenAI reforçam a mesma arquitetura observada nos papers.

Em *Harness engineering: leveraging Codex in an agent-first world*, a equipe relata que um grande `AGENTS.md` se tornou contraproducente. A solução foi tornar o repositório legível para o agente e usar documentação, schemas, testes e regras executáveis como sistema de registro. A orientação é muito próxima de "dar ao agente um mapa, não um manual de mil páginas".

Em julho, no texto sobre GPT-5.6, a OpenAI descreveu **deferred discovery**: ferramentas, MCPs, skills e plugins são apresentados ao modelo somente quando necessários. Isso reduz context bloat e evita que definições irrelevantes disputem atenção com a tarefa.

### Aplicação no AI Hub

Isso sugere que seu MCP Server não deveria despejar todas as ferramentas no contexto inicial.

Melhor arquitetura:

```text
Task
  ↓
Tool/Context Router
  ↓
seleciona categorias relevantes
  ↓
carrega apenas tools/docs necessários
  ↓
agent loop
```

Exemplo:

```text
"corrigir autenticação Google"

router →
  auth architecture
  secret/config tools
  OAuth docs
  tests
  repo search

não carregar →
  Meta Ads
  vídeo
  neuromarketing
  outros módulos sem relação
```

**Fontes:**

- https://openai.com/index/harness-engineering/
- https://openai.com/index/gpt-5-6-frontier-intelligence-efficiency/

---

# O componente que eu adicionaria agora ao AI Hub

A partir desses trabalhos, eu mudaria a arquitetura anterior de sete capacidades para algo mais explícito:

```text
                USER PROMPT
                     │
                     ▼
          ┌─────────────────────┐
          │ INTENT INTERPRETER  │
          │ objetivo real       │
          └──────────┬──────────┘
                     │
                     ▼
       ┌───────────────────────────┐
       │ REQUIREMENT COMPILER      │
       │ explícito / implícito     │
       │ unknowns / critérios      │
       └─────────────┬─────────────┘
                     │
                     ▼
       ┌───────────────────────────┐
       │ CONSTRAINT TRIGGER ENGINE │
       │ regras latentes aplicáveis│
       └─────────────┬─────────────┘
                     │
                     ▼
         ┌───────────────────────┐
         │ CONTEXT / TOOL ROUTER │
         │ MCP / repo / memória  │
         └───────────┬───────────┘
                     │
                     ▼
             ┌──────────────┐
             │   PLANNER    │
             └──────┬───────┘
                    │
                    ▼
             ┌──────────────┐
             │   EXECUTOR   │
             └──────┬───────┘
                    │
                    ▼
        ┌─────────────────────────┐
        │ VERIFIER / CRITIC       │
        │ requisitos satisfeitos? │
        └────────────┬────────────┘
                     │
                     ▼
             MEMORY UPDATE
```

## O ponto mais importante

Eu começaria pelo **Requirement Compiler**, porque ele ataca exatamente o problema original:

> "Como o agente pode perceber algo importante que eu não pensei em colocar no prompt?"

Ele deveria produzir quatro listas antes de qualquer execução:

```text
1. EXPLICIT
   coisas que o usuário realmente pediu

2. INFERRED
   coisas provavelmente necessárias

3. UNKNOWN
   coisas que ainda precisam ser descobertas

4. EVIDENCE
   onde o agente pode verificar cada UNKNOWN/INFERRED
```

Depois disso, o harness tenta resolver sozinho os `UNKNOWN` usando MCP, repo, documentação, memória e ferramentas.

Só deveria perguntar ao usuário quando a informação não puder ser descoberta e a decisão tiver impacto significativo.

---

# Cuidado importante

"Inferir requisito implícito" não deve significar inventar requisito.

Uma boa arquitetura precisa separar claramente:

```text
FACT        → há evidência
INFERENCE   → hipótese plausível
ASSUMPTION  → decisão temporária sem evidência
UNKNOWN     → precisa ser investigado
```

Esse detalhe aparece de formas diferentes em vários dos trabalhos acima e provavelmente será uma das peças mais importantes para tornar agentes autônomos sem torná-los excessivamente confiantes.

---

# Prioridade para implementação

Minha ordem para o AI Hub seria:

1. **Requirement Compiler**;
2. **Context/Tool Router** com descoberta sob demanda;
3. **Decision + Constraint Memory** com proveniência;
4. **Verifier** baseado nos requisitos compilados;
5. **Execution-state memory** para tarefas longas;
6. só depois investir em loops sofisticados de self-reflection/self-improvement.

Essa sequência tende a trazer mais ganho do que simplesmente adicionar mais chamadas de reflexão ao mesmo prompt.
