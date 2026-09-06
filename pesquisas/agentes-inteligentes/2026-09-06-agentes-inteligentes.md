# Radar diário — agentes mais inteligentes — 2026-09-06

## Resumo executivo

Hoje o sinal mais importante é uma mudança de foco: para um agente deduzir o que o usuário não colocou no prompt, o harness precisa tratar **interpretação do objetivo** como uma etapa própria, e não apenas como parte informal do planning.

Como é domingo, a listagem mais recente do arXiv para Computação continua sendo a de sexta-feira, 4 de setembro. Em vez de preencher a rodada com material fraco, priorizei dois preprints recentes da semana e três trabalhos especialmente úteis que ainda não haviam entrado neste radar.

A conclusão prática de hoje para o AI Hub é:

> antes de otimizar a execução, o agente deve operacionalizar o objetivo, decompor o que ele acredita que precisa ser alcançado, explicitar suas hipóteses, construir sinais de verificação e preservar um estado de projeto verificável entre iterações.

---

## 1. Aspire — transformar objetivos vagos em objetivos executáveis

**Tipo:** preprint acadêmico  
**Data:** 31/08/2026 (manuscrito datado de 01/09/2026)  
**Fonte:** https://arxiv.org/abs/2608.31111

### Ideia central

O Aspire estuda uma pergunta muito próxima do objetivo deste radar: o que acontece quando o agente recebe apenas uma direção ampla, sem tarefa, métrica ou critério de sucesso já definidos?

O benchmark fornece ao agente apenas um objetivo em linguagem natural. O próprio agente deve:

- interpretar o objetivo;
- diagnosticar lacunas de capacidade;
- decompor subobjetivos;
- decidir quais dados e métodos usar;
- construir sinais de treinamento e validação;
- decidir quando a melhoria é real.

O trabalho chama isso de **target operationalization**: converter uma intenção ampla em objetivos concretos, sinais de aprendizado e critérios de validação.

### Evidência

O benchmark usa um conjunto oculto, criado por especialistas, com 520 itens cobrindo seis objetivos. Os agentes conseguem executar loops de treinamento e editar seus próprios harnesses, mas os ganhos ainda são instáveis. O melhor harness evoluído automaticamente ainda fica abaixo de uma referência Qwen-Agent construída por engenharia humana.

Um problema recorrente observado pelos autores é o agente escolher proxies inadequados: ele melhora naquilo que decidiu medir, mas a melhora não transfere para a capacidade real escondida na avaliação.

### Limitação

A pesquisa mede evolução de capacidade e harness em um ambiente controlado. Ela não demonstra ainda que um agente consegue inferir corretamente requisitos implícitos de projetos de software reais.

### Aplicação ao AI Hub

Adicionar um estágio explícito antes do planner:

```text
Goal Operationalizer

input:
  user_intent

output:
  interpreted_goal
  inferred_subgoals[]
  assumptions[]
  unknowns[]
  evidence_needed[]
  success_signals[]
```

A regra importante é: **o planner não recebe diretamente o prompt cru**. Ele recebe o prompt + uma representação operacional do objetivo.

---

## 2. Harness-of-Harness — continuidade por evidência, não por contexto gigante

**Tipo:** preprint acadêmico  
**Data:** 01/09/2026  
**Fonte:** https://arxiv.org/abs/2609.01481

### Ideia central

O Harness-of-Harness (HoH) organiza agentes de programação existentes em ciclos repetidos de:

`planning → coding → testing → evidence → next planning`

O detalhe mais importante não é o multiagente. É a forma como o sistema mantém continuidade: ele persiste planos, relatórios, históricos e artefatos no sistema de arquivos e usa **progressive disclosure**. O agente começa com um índice pequeno e abre detalhes somente quando necessário.

O sistema também separa testes produzidos durante a implementação da avaliação independente posterior.

### Evidência

Nos benchmarks GameCraft-Bench, FrontierSWE e ProgramBench, três pares de harness/modelo tiveram ganho relativo médio de 52,25% sobre o harness isolado e máximo de 82,86% após três iterações. No FrontierSWE, a configuração Codex + GPT-5.5 (high) cresceu de 22% para 72,67% após dez loops.

Os autores também executaram mais de 70 iterações autônomas para construir um jogo completo.

### Limitação

É um estudo centrado em engenharia de software e desenvolvimento prolongado. Os números não implicam o mesmo ganho para agentes generalistas ou para tarefas de descoberta de requisitos.

### Aplicação ao AI Hub

Em vez de colocar toda a memória do projeto no contexto:

```text
Project State Index
├── goals
├── decisions
├── verified_capabilities
├── unresolved_questions
├── failed_attempts
├── tests
└── evidence
```

O agente recebe inicialmente apenas esse índice. Quando percebe que um requisito implícito pode depender de uma decisão anterior, chama MCP/retrieval para abrir o artefato específico.

Isso combina diretamente com a ideia de usar o MCP como mecanismo de **descoberta tardia de contexto**.

---

## 3. HarnessEvo — nem toda parte do harness vale a pena otimizar

**Tipo:** preprint acadêmico  
**Submissão original:** 25/06/2026  
**Fonte:** https://arxiv.org/abs/2609.02889

### Ideia central

O HARNESSEVO divide o harness textual em quatro partes independentes:

1. role;
2. task strategy;
3. tool/format rules;
4. reflection/control.

Em vez de melhorar tudo ao mesmo tempo, mede qual parte realmente gera ganho.

### Evidência

No ALFWorld, o ganho útil ficou quase todo concentrado em **reflection/control**. O leave-one-in dessa seção acrescentou +0,119 de sucesso. Dividir uniformemente um orçamento de 64 rollouts entre os quatro componentes fez a otimização praticamente congelar; concentrar orçamento na parte de maior crédito levou a 0,761 de sucesso usando metade do orçamento dividido.

No WebShop, entretanto, nenhuma seção trouxe ganho — mostrando que o efeito depende da tarefa.

### Limitação

O experimento usa backbone 7B e benchmarks específicos. Não é evidência de que reflection/control sempre será o componente mais importante.

### Aplicação ao AI Hub

O harness deveria ser modular e medir efeito por componente:

```text
Harness Modules

intent_interpretation
context_retrieval
requirement_inference
planning
reflection_control
verification
memory
```

Cada melhoria proposta deve registrar:

```text
module_changed
failure_pattern
expected_effect
benchmark_before
benchmark_after
```

Assim o AI Hub não “evolui o agente” de forma vaga; ele aprende **onde** a modificação teve valor.

---

## 4. PersonaLink — comprimir histórico é útil, mas não substitui recuperação seletiva

**Tipo:** preprint acadêmico  
**Submissão original:** 25/06/2026  
**Fonte:** https://arxiv.org/abs/2609.02890

### Ideia central

O PersonaLink transforma o histórico de um usuário em uma representação curta e fixa com três campos:

- resumo de preferências;
- poucos exemplos representativos;
- regras de decisão inferidas.

O agente se testa em uma parte do histórico, reescreve a persona a partir de seus erros e mantém a nova versão apenas se ela não piorar.

### Evidência

Em classificação de notícias para 200 usuários, a persona compacta atingiu 0,745–0,755 de acurácia, estatisticamente próxima do BM25 retrieval (0,760–0,765). Em regressão de notas de produtos, entretanto, retrieval foi muito melhor: MAE 0,285 contra 0,455.

O refinamento recursivo também saturou rapidamente: quase todo o ganho apareceu já no primeiro passe.

### Limitação

São apenas dois tipos de tarefa com um modelo 7B congelado. O paper mostra claramente que uma persona condensada não substitui retrieval em problemas que precisam de detalhes finos.

### Aplicação ao AI Hub

Usar **duas memórias complementares**:

```text
ProjectProfile
  stable principles
  architecture summary
  user preferences
  recurring constraints

EvidenceMemory
  raw conversations
  commits
  decisions
  logs
  test results
```

O `ProjectProfile` entra frequentemente no contexto por ser pequeno. O `EvidenceMemory` só é recuperado quando o agente precisa confirmar uma inferência.

Isso evita dois extremos: contexto enorme e persona excessivamente resumida.

---

## 5. Counterexamples as Feedback — crítica genérica é muito inferior a evidência concreta

**Tipo:** preprint acadêmico  
**Submissão original:** 01/07/2026  
**Fonte:** https://arxiv.org/abs/2609.02892

### Ideia central

O A-CEGIS testa self-correction usando **contraexemplos concretos** em vez de pedir simplesmente “revise sua resposta”. Um oráculo determinístico encontra falsos positivos ou falsos negativos e devolve esses exemplos ao agente.

### Evidência

Em 30 tarefas de síntese de regex, feedback por contraexemplos resolveu 90% em até quatro turnos, contra:

- 17% zero-shot;
- 27% self-correction genérica;
- 23% feedback apenas de erro.

Na execução completa com hardening, todas as tarefas foram solucionadas no conjunto oculto até o último turno, embora a robustez após probing direcionado tenha ficado em 77%.

### Limitação

É um domínio estreito e com um oráculo determinístico forte. Muitos problemas de engenharia não oferecem contraexemplo automaticamente.

### Aplicação ao AI Hub

O Critic deve evitar mensagens vagas como:

```text
"revise sua solução"
```

E preferir evidência executável:

```text
Expected:
  login por senha continua funcionando

Observed:
  teste AuthPasswordIT.testLogin retornou 401

Counterexample:
  usuário legado com password_hash válido não autentica após mudança OAuth
```

Isso transforma reflection em **debugging baseado em evidência**.

---

# Arquitetura sugerida após a rodada de hoje

```text
USER INTENT
    │
    ▼
GOAL OPERATIONALIZER
    │
    ├── explicit
    ├── inferred
    ├── assumptions
    ├── unknowns
    └── success signals
    │
    ▼
PROJECT PROFILE
    │
    ▼
CONTEXT / EVIDENCE RETRIEVAL
(MCP / repo / memory / logs)
    │
    ▼
PLANNER
    │
    ▼
EXECUTOR
    │
    ▼
INDEPENDENT VERIFIER
    │
    ├── tests
    ├── counterexamples
    └── evidence
    │
    ▼
PROJECT STATE
    │
    ▼
NEXT ITERATION
```

## Mudança de prioridade para o AI Hub

A prioridade que eu colocaria agora é:

1. **Goal Operationalizer / Requirement Compiler**;
2. **Project State Index + evidence retrieval via MCP**;
3. **Verifier que devolve contraexemplos e evidências concretas**;
4. só depois **self-improving harness**.

A razão é simples: um agente não fica realmente mais inteligente por executar mais loops se ele operacionalizou o objetivo errado. Primeiro ele precisa melhorar sua capacidade de decidir **o que realmente precisa ser alcançado** e **qual evidência provará que conseguiu**.

## Engenharia / posts técnicos

Não encontrei hoje um novo post técnico de laboratório suficientemente forte e distinto dos trabalhos já cobertos nos dias anteriores para justificar inclusão. Mantive a rodada concentrada nos achados com maior impacto arquitetural, em vez de preencher o relatório com material marginal.
