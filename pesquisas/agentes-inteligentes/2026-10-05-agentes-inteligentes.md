# Radar diário — agentes mais inteligentes | 05/10/2026

## Resumo executivo

A listagem de cs.AI do arXiv para **5 de outubro de 2026** já está disponível; os trabalhos mais úteis desta rodada foram submetidos em **2 de outubro**. A convergência é forte: **descobrir contexto implícito precisa ser tratado como um ciclo causal de hipótese → evidência → execução → verificação → atribuição → aprendizagem**, e não como simples aumento de prompt ou memória.

## Recursive Harness Self-Improvement — dois ritmos de evolução

**Tipo:** pesquisa acadêmica / preprint  
**Fonte:** https://arxiv.org/abs/2610.03548

O trabalho propõe task-harness co-evolution. O ciclo online converte falhas intermediárias em skills reutilizáveis; o ciclo pós-tarefa revisa skills, prompts e workflows e só adota candidatos quando geram tarefas válidas mais difíceis dentro de um limite de custo. Os pesos do modelo e os critérios de verificação permanecem fixos. Em matemática, coding e ciência, a acurácia média do solver cai de **100,0% para 54,8% ao longo de 14 rodadas**, indicando que o sistema passou a sintetizar problemas progressivamente mais difíceis; as ablações favorecem a combinação dos dois ritmos.

**Aplicação ao AI Hub:** separar **fast learning** (heurística/skill provisória e scoped durante a execução) de **slow learning** (mutação do harness promovida apenas após held-out, regressões e cost gate).

**Limitação:** o domínio experimental é síntese de dados de raciocínio, não operação de agentes em produção.

## DyadMem — memória relacional de como trabalhar com o usuário

**Tipo:** pesquisa acadêmica / benchmark de memória  
**Fonte:** https://arxiv.org/abs/2610.03020

DyadMem introduz **User-conditioned Relational Agent Memory (URAM)**: não apenas fatos e preferências, mas memória de como um agente específico deve trabalhar com um usuário específico ao longo da relação. O benchmark contém **3.065 episódios, 50.961 sessões e 61.210 instâncias de QA**. Em 16 modelos open-weight e quatro proprietários, Gold-Memory QA é forte, mas Full-Pipeline QA cai bastante; os autores observam baixo capture recall, recall incompleto e problemas de exclusão.

**Aplicação ao AI Hub:** separar USER/PROJECT FACTS, WORKING RELATION e TASK EXPERIENCE. Correções de workflow específicas de um projeto não deveriam virar automaticamente uma skill global nem ser misturadas com fatos.

**Limitação:** o benchmark mede captura/recall e QA; não determina qual storage ou política de retrieval é melhor para coding agents.

## ReFract — intenção depende da perspectiva

**Tipo:** pesquisa acadêmica / benchmark  
**Fonte:** https://arxiv.org/abs/2610.03356

ReFract mede **Perspective Awareness**: a mesma query pode exigir respostas e ações diferentes conforme o papel, conhecimento e ferramentas legítimas do usuário. São **150 casos validados por especialistas** e baseados em suporte real, executados em Text World Models. Os melhores LLMs resolvem no máximo **69%** das tarefas e **mais de 50% das trajetórias** tentam alguma ação incompatível com a perspectiva.

**Aplicação ao AI Hub:** criar um **Perspective State** separado do Goal: quem está pedindo, qual papel/capacidade é implícito, quais fontes pode observar e quais ações são compatíveis. O mesmo princípio vale entre subagentes: verifier não deve se comportar como executor, e research agent não deve transformar descoberta em side effect.

**Limitação:** inferir perspectiva não pode virar licença para inventar intenção; decisões de alto impacto continuam exigindo validação.

## LiteTrajEval — avaliação contínua de traces com budget fixo

**Tipo:** pesquisa acadêmica + evidência de deployment  
**Fonte:** https://arxiv.org/abs/2610.03315

LiteTrajEval cria perfis compactos de regras offline e, online, preprocessa o trace, marca sinais heurísticos de falha e serializa a trajetória dentro de um budget fixo antes de chamar um único judge guiado por rubrica. Em datasets Magentic-One-style e tau-bench-style, melhora o alinhamento de localização de falhas com humanos em cerca de **20–35 pontos percentuais** no Magentic-One e até **23 pontos** no tau-retail, com cerca de **6× menos custo** e **mais de 8× menos tempo** que AgentRx. Os autores também relatam deployment empresarial.

**Aplicação ao AI Hub:** usar um Trace Preprocessor para preservar critical tool calls, state transitions, dependency summary e failure markers; análise completa fica reservada para casos ambíguos ou de alto valor.

**Limitação:** perfis de regra por domínio podem ser menos estáveis em tarefas muito abertas de descoberta de requisitos implícitos.

## DepGPO — credit attribution pelo grafo de dependências

**Tipo:** pesquisa acadêmica / RL para agentes de terminal  
**Fonte:** https://arxiv.org/abs/2610.03634

DepGPO constrói um **command dependency graph** a partir do trace e caminha para trás dos recursos que o verifier realmente inspecionou. O crédito é atribuído aos writes relevantes e aos reads que os sustentaram, evitando reforçar operações que estavam na trajetória mas não causaram o resultado. Os autores reportam melhora de desempenho e estabilidade em tarefas complexas de terminal.

**Aplicação ao AI Hub:** o Execution Provenance Graph deve registrar dependências read/write até o verifier e alimentar **credit attribution de skills e políticas**. Assim o evoluidor aprende qual busca, evidência ou alteração realmente sustentou o sucesso.

**Limitação:** o paper redistribui gradientes em RL; com modelos fechados, o ganho no AI Hub virá de melhor seleção/promoção de skills e policies.

## JOVE — verificar onde a informação vale mais

**Tipo:** pesquisa acadêmica / test-time orchestration  
**Fonte:** https://arxiv.org/abs/2610.03296

JOVE distribui subtarefas de um task graph entre LLMs heterogêneos quando a qualidade de cada modelo ainda é desconhecida e escolhe quais resultados intermediários merecem **verificação paga**. A verificação é assíncrona e atualiza estimativas futuras; um bônus de **information gain** incorpora o valor de aprender sobre o executor. Em quatro benchmarks, mantém accuracy competitiva reduzindo custo e latência médios em pelo menos **3,17×** frente aos baselines comparados.

**Aplicação ao AI Hub:** criar um **Verification Budget Manager** usando impacto, incerteza, novidade, historical failure rate e information gain para decidir VERIFY / SKIP / DEFER.

**Limitação:** task graphs e custos são relativamente estruturados; tarefas abertas exigem estimativas aproximadas.

## Engenharia nesta rodada

Não encontrei, entre **3 e 5 de outubro**, um post técnico de laboratório/engenharia que acrescentasse algo materialmente novo além do que já entrou nas rodadas de 2–4 de outubro. A documentação de harnesses da Cloudflare atualizada em 2/10 reforça a separação entre runtime durável e harness cognitivo, já registrada anteriormente, então não a repito como novidade.

## Arquitetura resultante

~~~text
USER REQUEST
    ↓
PERSPECTIVE STATE
    ↓
REQUIREMENT DISCOVERY
    ↓
RELATIONAL + TASK MEMORY
    ↓
AGENT EXECUTION
    ↓
EXECUTION DEPENDENCY GRAPH
    ↓
SELECTIVE VERIFICATION
    ↓
TRAJECTORY EVALUATOR
    ↓
CREDIT ATTRIBUTION
    ↓
FAST PROVISIONAL IMPROVEMENT
    ↓
SLOW HARNESS EVOLUTION
held-out / regression / cost
~~~

## Prioridade prática

1. Criar **Perspective State** separado do goal.
2. Adicionar **Relational Agent Memory** para regras específicas da relação usuário/projeto.
3. Fazer o Execution Provenance Graph registrar dependências read/write até o verifier.
4. Implementar um **Lite Trajectory Evaluator** com budget fixo.
5. Usar o grafo de dependências para **credit attribution** de skills.
6. Criar um **Verification Budget Manager** baseado em risco, incerteza e information gain.
7. Separar self-improvement em **fast/provisional** e **slow/promoted**.

## Conclusão

A ideia mais importante de hoje é deixar o loop de aprendizagem mais causal:

~~~text
não:
"esta trajetória deu certo → memorize tudo"

mas:
"qual contexto permitiu a decisão?"
    ↓
"qual passo realmente causou o resultado?"
    ↓
"qual parte é específica deste usuário/projeto?"
    ↓
"qual parte é skill reutilizável?"
    ↓
"qual mudança merece promoção global?"
~~~

Isso aproxima o AI Hub de um sistema que **aprende a descobrir contexto**, em vez de simplesmente acumular contexto.

## Fontes

- https://arxiv.org/list/cs.AI/recent
- https://arxiv.org/abs/2610.03548
- https://arxiv.org/abs/2610.03020
- https://arxiv.org/abs/2610.03356
- https://arxiv.org/abs/2610.03315
- https://arxiv.org/abs/2610.03634
- https://arxiv.org/abs/2610.03296
