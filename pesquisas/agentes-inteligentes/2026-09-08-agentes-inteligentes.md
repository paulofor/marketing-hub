# Radar diário — agentes mais inteligentes | 08/09/2026

A rodada de hoje trouxe uma combinação especialmente útil para a pergunta central deste radar: **como fazer um agente perceber o que o usuário não explicitou e, depois de descobrir esse contexto, realmente incorporá-lo ao plano e à execução**.

O ponto novo que emerge hoje é que há pelo menos quatro problemas diferentes, que não deveriam ser tratados como um único “RAG melhor”:

1. detectar que a especificação está incompleta;
2. decidir se deve buscar contexto ou perguntar ao usuário;
3. interpretar sinais indiretos do comportamento do usuário em uma escala temporal adequada;
4. garantir que o plano e a ação continuem derivados do contexto correto depois que novas informações aparecem.

## 1. Ask Before You Optimize: transformar “o que está faltando?” em uma decisão formal

**Tipo:** preprint acadêmico, arXiv 2609.05258, 4 de setembro de 2026.

O paper **Ask Before You Optimize: Dynamic Pre-Formulation Clarification for Interactive Optimization** estuda problemas de otimização descritos em linguagem natural. Na prática, solicitações de negócio frequentemente omitem objetivo, restrições ou regras que mudariam completamente o modelo matemático. O trabalho cria o benchmark **OR-Clarify**, no qual a descrição pública é propositalmente incompleta e alguns “slots” críticos ficam escondidos.

O agente é avaliado não apenas pela resposta final, mas por:

- recuperação dos slots ocultos;
- capacidade de saber quando perguntar;
- capacidade de saber quando parar de perguntar;
- quantidade de suposições silenciosas;
- custo de interação.

O framework **InterOPT** separa explicitamente duas etapas: primeiro identifica lacunas críticas da formulação; depois decide se a próxima ação deve ser perguntar ou declarar que já há informação suficiente.

A contribuição mais importante para o AI Hub é tratar completude como um **estado operacional**, não como uma impressão subjetiva do modelo.

Eu criaria um componente assim:

```text
Preflight Completeness Gate

explicit_requirements
inferred_requirements
unresolved_slots
critical_unknowns
retrievable_unknowns
user_only_unknowns
silent_assumptions
readiness_score
```

Então, antes do planner:

```text
if critical_unknowns can be recovered from repo/MCP/memory:
    retrieve
elif critical_unknowns require user knowledge:
    ask
elif remaining unknowns do not change the plan materially:
    proceed
else:
    block planning
```

A ideia de **silent assumptions** é particularmente importante. O agente não deveria simplesmente preencher uma lacuna porque uma resposta “parece provável”. Toda suposição que pode alterar arquitetura, segurança, dados ou comportamento externo deveria ficar registrada.

**Limitação:** o domínio é Operations Research e a avaliação de clarificação não mede diretamente desenvolvimento de software. Mesmo assim, o mecanismo de “hidden slots + decisão de perguntar/parar” é muito transferível para agentes de engenharia.

Fonte: https://arxiv.org/abs/2609.05258

## 2. Rhythms of Work: o contexto que o usuário não disse pode estar no comportamento anterior, mas precisa ser lido na escala correta

**Tipo:** preprint acadêmico de pesquisadores da Microsoft, arXiv 2609.04556, disponibilizado em 7 de setembro de 2026.

**Rhythms of Work: Multi-Scale Interpretation of Human Behavioral Traces for Workplace Agents** traz uma direção diferente: em vez de perguntar somente “o que existe na memória?”, pergunta **como interpretar o comportamento que cerca o usuário**.

Os autores analisam 667 milhões de eventos atribuídos a humanos em uma grande suíte de produtividade, cobrindo 50 mil usuários e 100 organizações. Eles constroem representações em várias escalas:

```text
raw events
   ↓
operators
   ↓
recurring motifs
   ↓
episodes
   ↓
day-level rhythms
```

O achado central é que **não existe uma única resolução ideal para resumir o comportamento do usuário**. Perguntas diferentes exigem granularidades temporais diferentes. Na validação, a representação multi-resolução melhorou a previsão do próximo episódio do usuário em **17% de macro-F1 relativo** versus uma representação plana baseada em operadores.

Isso é muito relevante para um agente que deve deduzir contexto ausente. Imagine que você peça:

> “continue o trabalho na autenticação.”

O agente poderia consultar diferentes escalas:

```text
últimos minutos:
  arquivos e comandos recentes

última sessão:
  tentativa, erro, decisão e estado final

últimos dias:
  sequência de tarefas do módulo

nível de projeto:
  padrões arquiteturais recorrentes
```

Eu evitaria criar um único “resumo do usuário” ou “resumo do projeto”. Melhor seria uma **Context Pyramid** consultável:

```text
Context Pyramid

L0 raw trace
L1 atomic actions
L2 task episodes
L3 recurring workflows
L4 project rhythms / stable patterns
```

O `Context Router` escolhe a resolução de acordo com a pergunta. Uma dúvida sobre “qual arquivo eu estava alterando?” pede granularidade fina; “como este projeto normalmente trata autenticação?” pede granularidade maior.

**Limitação:** o paper demonstra previsão e interpretação de atividade, não melhoria direta de task success em agentes. Há também uma questão importante de privacidade: traços comportamentais só devem ser usados em contextos autorizados, com escopo claro e minimização de dados.

Fonte: https://arxiv.org/abs/2609.04556

## 3. EComAgentBench v3: encontrar o contexto não significa usá-lo corretamente

**Tipo:** benchmark/preprint acadêmico, versão v3 de 2 de setembro de 2026, arXiv 2606.17698.

O **EComAgentBench** é provavelmente o benchmark mais diretamente alinhado ao nosso tema até agora. Ele distribui a intenção completa do usuário por três fontes:

```text
query visível
+ perfil oculto acessível por ferramenta
+ informação obtida apenas por clarificação
```

São **662 tarefas** e **6.645 rubricas**, com requisitos marcados pela fonte de origem. O agente pode usar até 100 tool calls e 10 turnos de clarificação.

O melhor sistema avaliado chegou a apenas **57,1% de acurácia geral**. Mais interessante: para GPT-5.4, a satisfação das rubricas foi **88,1% para requisitos visíveis no prompt**, mas caiu para **69,8% no perfil** e **70,9% em requisitos de clarificação**.

O resultado mais importante para o nosso desenho é que os autores observam que **quase todos os modelos consultam o perfil**. Portanto, parte significativa do problema não é retrieval. É **context utilization**: o dado foi recuperado, mas não foi integrado de forma confiável à decisão final.

Isso sugere adicionar ao AI Hub um `Intent Assembly Ledger`:

```yaml
requirements:
  - id: R1
    value: preserve_existing_login
    source: project_memory
    status: confirmed
    used_by_plan_steps: [P2, P5]
    verified_by: AuthRegressionTest

  - id: R2
    value: refresh_token_backend_only
    source: architecture_doc
    status: confirmed
    used_by_plan_steps: [P3]
    verified_by: config_inspection
```

Assim podemos distinguir:

```text
retrieved
understood
incorporated_in_plan
executed
verified
```

Isso é melhor do que medir apenas “o agente chamou a ferramenta certa?”.

**Limitação:** é um benchmark de shopping e usa um ambiente altamente controlado. O valor para o AI Hub está principalmente no desenho de avaliação distribuída por fontes e na separação entre recuperar e realmente utilizar contexto.

Fonte: https://arxiv.org/abs/2606.17698

## 4. Fresh Memory, Stale Plans: contexto novo não corrige automaticamente um plano velho

**Tipo:** preprint acadêmico/sistemas, arXiv 2609.03340, 3 de setembro de 2026.

**Fresh Memory, Stale Plans: Dependency-Scoped Validation for Distributed LLM-Agent Memory** identifica uma falha sutil em sistemas multiagente: um executor pode possuir a informação mais recente e ainda assim executar um plano produzido a partir de uma versão antiga do requisito.

Exemplo:

```text
planner lê requirement r3
       ↓
cria plan p(r3)
       ↓
outro agente atualiza requisito para r4
       ↓
executor recebe r4
       ↓
mas ainda executa p(r3)
```

Os autores chamam isso de **stale-plan execution**. O sistema proposto, **PlanFence**, faz cada plano citar exatamente os registros públicos usados em sua derivação. Antes de uma ação externa, o executor valida apenas as dependências que podem afetar aquela ação. Se algum registro mudou, o sistema replana ou bloqueia.

Em **30 workflows controlados** com revisão posterior ao planejamento, o executor baseado apenas em “memória fresca” executou o plano obsoleto em todos os casos; PlanFence completou todos sem ação inválida.

Para o AI Hub, eu adicionaria lineage ao planner:

```yaml
plan_step: deploy-auth-service
based_on:
  - requirement:R17@version_4
  - architecture:auth@commit_ab12
  - decision:oauth-session@2026-08-20
```

E antes de tool calls irreversíveis ou relevantes:

```text
Action Fence

validate dependencies
    ↓
unchanged → execute
changed   → replan
unknown   → block / retrieve
```

Isso é especialmente importante em execuções longas, nas quais você pode alterar um requisito no meio do trabalho ou outro agente pode atualizar documentação, código ou estado compartilhado.

**Limitação:** o paper mede segurança/consistência de ações sob mudança de estado, não aumento geral de task accuracy. É mais uma contribuição de arquitetura de agentes distribuídos.

Fonte: https://arxiv.org/abs/2609.03340

## 5. Resultado adicional: reflection genérica pode valer menos do que feedback real de execução

**Tipo:** preprint experimental, arXiv 2609.03718, 3 de setembro de 2026.

O paper **What Do CAE Simulation Agents Really Need Beyond a Generic Harness?** compara scaffolds especializados com um harness moderno genérico para simulações de engenharia. Em condições controladas, o harness single-agent chegou a **96,4% no FoamBench**, versus **88,2%** para sistemas especializados comparados.

O resultado mais útil para nós está nas ablações: adicionar um prompt explícito de “scripted reflection” manteve exatamente **96,4%**, ou seja, não acrescentou ganho. Já permitir ciclos de execução e reparo aumentou o desempenho de **71,8% para 96,4%**, e fornecer material de domínio relevante elevou um cenário de **80,9% para 96,4%**.

Isso reforça uma direção para o AI Hub:

```text
menos:
"reflita novamente sobre sua resposta"

mais:
execute → observe → compare com critério → obtenha contraexemplo → repare
```

E também reforça que a vantagem de um MCP/context router não é “ter uma camada RAG sofisticada”, mas **garantir que o material correto chegue ao modelo no momento certo**.

**Limitação:** resultado de um domínio técnico específico; não prova que reflection nunca ajuda. Mostra apenas que reflection textual genérica pode ter retorno marginal baixo quando o harness já fornece execução, feedback e reparo.

Fonte: https://arxiv.org/abs/2609.03718

# O que eu mudaria hoje no AI Hub

A arquitetura que vem surgindo nas últimas rodadas ficaria agora assim:

```text
                       USER PROMPT
                           │
                           ▼
                    GOAL INTERPRETER
                           │
                           ▼
              PREFLIGHT COMPLETENESS GATE
                explicit / inferred / unknown
                           │
              ┌────────────┼────────────┐
              │            │            │
          retrieve        ask        proceed
              │            │            │
              └────────────┼────────────┘
                           ▼
                    CONTEXT PYRAMID
        raw → actions → episodes → patterns → project
                           │
                           ▼
                  INTENT ASSEMBLY LEDGER
          requirement + source + confidence + status
                           │
                           ▼
                        PLANNER
               plan steps cite dependencies
                           │
                           ▼
                     ACTION FENCE
           are the plan dependencies still valid?
                           │
                    ┌──────┴──────┐
                    │             │
                  yes           changed
                    │             │
                    ▼             ▼
                 execute        replan
                    │
                    ▼
              EVIDENCE-BASED VERIFY
                    │
                    ▼
                MEMORY UPDATE
```

A principal conclusão de hoje é que **“context discovery” precisa virar uma cadeia auditável**:

```text
context existed
→ agent noticed a gap
→ context was retrieved
→ context was interpreted
→ requirement was assembled
→ plan depended on it
→ action still depended on a current version
→ result was verified
```

Se quisermos medir se o agente realmente ficou melhor em “deduzir aquilo que Paulo não disse”, eu adicionaria ao benchmark do AI Hub estas métricas:

- **Hidden Requirement Recall** — quantos requisitos não explícitos relevantes ele descobriu;
- **Silent Assumption Rate** — quantas decisões críticas ele inventou sem evidência;
- **Clarification Efficiency** — quantas perguntas eram realmente necessárias;
- **Context Utilization Rate** — informação recuperada que efetivamente influenciou o plano;
- **Resolution Match** — se consultou o nível adequado de histórico/contexto;
- **Plan Lineage Validity** — se a ação foi derivada das versões atuais dos requisitos;
- **Evidence-Backed Completion** — quantos critérios foram comprovados por testes ou observações.

Hoje não encontrei um post técnico de laboratório que adicionasse evidência forte o bastante para justificar entrar na rodada. Preferi manter apenas os trabalhos com ganho conceitual ou experimental claro.
