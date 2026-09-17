# IA Autônoma — 2026-09-17

**Rodada:** 2026-09-17 18:25 (America/Sao_Paulo)

Nesta rodada há **quatro desenvolvimentos novos que passam o filtro**: **EvolveTrade**, **EvoSkill-GUI**, **CERA-MoA** e **CHASE**. Os dois primeiros mostram evolução persistente fora dos pesos; CERA-MoA mostra que routing e agentes podem aprender juntos; CHASE acrescenta uma defesa importante contra um problema que está ficando central: o agente parecer melhor porque aprendeu atalhos do protocolo de avaliação. Não encontrei hoje um novo caso de produção no nível LinkedIn/Tencent/Warp/Meta fechando o ciclo completo de evolução em tráfego real.

| Caso | (1) Pesos | (2) Código/scaffold/harness | (3) Prompts/retrieval/workflows/tools/skills/estratégia | (4) Só memória | (5) Condução humana |
|---|---:|---:|---:|---:|---:|
| **EvolveTrade** | Não | Não | **Sim — policy textual persistente** | Não | Parcial no desenho do sistema |
| **EvoSkill-GUI** | Não | Não | **Sim — skills procedurais estruturadas** | Não | Parcial |
| **CERA-MoA** | **Sim** | Não | **Sim, mas routing é aprendido parametricamente** | Não | **Sim — post-training/RL** |
| **CHASE** | Não | **Sim** | **Sim** | Não | Parcial |

## EvolveTrade — a policy operacional muda com o resultado realizado

O **EvolveTrade: Experience-Driven Policy Refinement for Self-Evolving LLM Trading Agents** mantém o LLM e as ferramentas congelados. Depois de cada intervalo de negociação, um **Policy Agent** lê o trade realizado, o raciocínio por ativo e o resultado, e reescreve a policy textual que será usada nas execuções seguintes. O que persiste não é simplesmente a lembrança de que uma ação deu certo ou errado: persiste uma **regra operacional revisada** sobre quais ferramentas priorizar, quais sinais cruzar e como ajustar exposição.

Em um exemplo reportado no paper, a policy evoluída reduziu a alocação em NVDA para 2,9%, contra 10,7% da versão estática, antes de uma queda de aproximadamente 17% no dia seguinte. A perda diária ficou em -0,03%, contra -1,11% da policy estática. Em 50 dias, a configuração base estática obteve Sharpe 1,82, retorno acumulado 4,51 e drawdown máximo 4,70; a versão estática com tool calling ficou em 2,94 / 8,88 / 4,40; e o EvolveTrade chegou a **Sharpe 4,00, retorno acumulado 10,56 e drawdown 2,96**.

A mudança também aparece no comportamento de tools: chamadas ao ambiente de código sobem de aproximadamente 1 por dia para algo entre 2,6 e 4,5, dependendo do regime, indicando que a policy não está apenas armazenando texto — ela altera de forma mensurável o procedimento de análise. Com custos de transação de 10 bps, o sistema ainda manteve o melhor Sharpe/retorno em três dos seis cenários avaliados e menor turnover que algumas alternativas.

**Classificação:** principalmente **(3)**. Não há mudança dos pesos nem do código do harness. Também não é mera memória, porque a policy operacional é persistentemente reescrita e passa a governar as próximas execuções. Humanos definem o framework, dados e objetivo, mas o refinamento da policy durante a sequência é automatizado.

**Limitações:** os ganhos não são uniformes nos seis regimes e os resultados são de backtest, não de um deployment financeiro vivo em produção.

**Padrão reutilizável:** `trace da decisão + outcome realizado → Policy Refiner → pequeno diff persistente da policy → próxima execução`. Para agentes próprios, isso sugere transformar experiência em **mudança de procedimento**, e não apenas anexar a experiência à memória.

Fonte: https://arxiv.org/abs/2609.17632

## EvoSkill-GUI — skills viram artefatos vivos, modulares e revisáveis

O **Reflect, Revise, Reuse: Training-Free Skill Evolution for GUI Agents (EvoSkill-GUI)** trata skills como pacotes procedurais persistentes que o mesmo agente pode criar, recuperar, executar e revisar em inference time, sem alterar os pesos. Em vez de uma skill ser apenas um texto monolítico, ela pode ser decomposta em partes como **plano, grounding e recuperação/contingência**, permitindo que o sistema atribua o erro a um componente e revise somente aquela parte.

Os ganhos acumulam por rodadas. No MobileWorld, Claude-Sonnet-4.6 passa de **58,1% para 67,6%**; Qwen3.6-35B-A3B de **32,4% para 44,8%**; MAI-UI-8B de **28,5% para 37,1%**. Qwen3.6-Plus chega de 56,2% a 68,6% nas três primeiras rodadas e 69,5% após cinco. Em OSWorld, GUI-Owl-1.5-8B sobe de 46,7% para 54,8%, e Qwen3-VL-8B de 23,8% para 34,3%.

O formato estruturado também importa: o pacote multi-arquivo chega a **69,52%**, contra **66,67%** da versão de arquivo único. Remover isolamento de informação entre executor e crítico reduz para 60,95%; remover revisão imediata reduz para 62,86%. Em uma sequência do AndroidWorld, a biblioteca cresceu de 9 para 69 e depois 98 skills, com **56,1% de reutilização**. Erros de planejamento foram frequentemente corrigidos em `plan.md`, erros de grounding em `backup.md` e falta de contingência em `recover.md`.

**Classificação:** **(3)**. O que persiste são skills externas revisadas; não há treinamento dos pesos. Não é simples memória porque as experiências são **compiladas em procedimentos reutilizáveis** que alteram a execução futura.

**Limitação importante:** o trabalho não possui ainda um verifier formal capaz de vetar automaticamente uma edição prejudicial da skill. Uma revisão pode introduzir regressões.

**Padrão reutilizável:** `falha → credit attribution → editar somente plan/grounding/recovery afetado → replay/avaliação → reutilizar a skill em tarefas seguintes`. Para produção, eu acrescentaria obrigatoriamente regression tests e rollback antes de promover a skill revisada.

Fonte: https://arxiv.org/abs/2609.17653

## CERA-MoA — router e agentes aprendem juntos

O **CERA-MoA: Co-Evolving Routing Mechanisms with Continually Learning LLM Agents**, submetido em 16 de setembro, aborda um problema importante em sistemas multiagente: se os agentes vão aprendendo, um router estático passa a ter uma visão desatualizada das capacidades de cada um; e se o router envia sempre tarefas aos mesmos agentes, os demais deixam de receber experiência suficiente para melhorar.

O sistema mantém um estimador de **familiaridade/competência** e faz routing adaptativo para o menor subconjunto de agentes que parece suficiente. Ao mesmo tempo, distribui exemplos de treinamento conforme lacunas de competência. Assim, a especialização emergente dos agentes altera o routing, e o routing altera quais experiências cada agente recebe.

No setup Qwen3-4B, a média ID foi de 49,6 no modelo base, 53,1 com um router estático, 59,4 com GSPO, 61,0 com AT-GRPO e **63,2 com CERA-MoA**. Em OOD, AT-GRPO chegou a 71,5 e CERA-MoA a **72,8**. A versão adaptativa obtém praticamente a mesma acurácia de um top-2 fixo (63,2 vs. 63,3 ID), mas usando em média **367,77 tokens contra 666,37**. O overhead do router ficou em aproximadamente 11,87 ms, menos de 1% do tempo típico de geração reportado.

Uma ablação é especialmente relevante: remover exploração derruba o desempenho para **57,1 ID / 63,9 OOD**, mostrando um caso claro de *policy starvation* — o router para de mandar experiência a alguns agentes, e eles deixam de evoluir. Com exploração, a especialização surge sem que humanos precisem definir manualmente qual agente deve dominar qual área.

**Classificação:** principalmente **(1) + (5)**. Os agentes e o estimador/router aprendem parametricamente durante post-training/RL. Existe evolução de routing, mas a persistência está sobretudo nos parâmetros aprendidos, não em regras textuais ou código autoeditado. Não é evidência de autoaperfeiçoamento online em produção: humanos definem todo o protocolo de treinamento.

**Limitação:** o routing atual se baseia essencialmente no prompt inicial e o trabalho reconhece que acompanhamento dinâmico passo a passo ao longo de uma trajetória multi-turn ainda é trabalho futuro.

**Padrão reutilizável:** `competence estimator → adaptive routing → targeted experience → atualização dos agentes → reestimar competência`. A lição prática é que, em um sistema que aprende, **router e workers não podem ser tratados como componentes independentes**. É preciso também reservar exploração para impedir que um worker deixe de receber oportunidades de aprender.

Fonte: https://arxiv.org/abs/2609.18779

## CHASE — o self-improver também precisa testar se está enganando a própria avaliação

O **Bad Genius: Counterfactual-Guided Harness Evolution Beyond Task-Specific Shortcuts** introduz o **CHASE**, um mecanismo para evitar que evolução de harness vire benchmark hacking. O Proposer pode modificar prompt, memória de longo prazo, retrieval, policy de tools, policy graph e código de controle ao redor de um modelo-alvo congelado. O problema é que um held-out tradicional por tarefas ainda pode compartilhar atalhos estruturais do mesmo protocolo de avaliação.

CHASE acrescenta um **Challenger** que tenta criar transformações contrafactuais do protocolo preservando a validade sem preservar o atalho. As transformações precisam passar por um validity firewall e depois por confirmação em tarefas separadas. Quando uma transformação revela um shortcut real, ela entra num **archive persistente de counterexamples** e passa a ser uma restrição para todas as futuras versões do harness.

No OfficeQA, RawHarness tinha 27,04 no baseline e 67,98 no released benchmark, mas média de 66,23 no archive e worst-case 64,47. CHASE chegou a **30,37 / 68,86 / 68,42 / 67,98**, respectivamente. Um caso particularmente instrutivo: uma candidata que parecia ganhar **+8,16% no released benchmark** teve **-5,10%** sob o contrafactual. Em uma rodada posterior, candidatas com ganhos aparentes de até **+15,31%** foram rejeitadas por violar constraints; só uma versão posterior conseguiu +4,08% mantendo as restrições.

**Classificação:** **(2) + (3)**. O modelo fica congelado e o objeto de evolução é o harness/policy externa. O mecanismo de Proposer/Challenger e as regras de promoção são definidos por humanos, então a autonomia é limitada ao espaço configurado.

**Por que importa:** vários sistemas vistos nas últimas semanas já possuem Evolver + held-out + promotion gate. CHASE mostra que isso ainda pode ser insuficiente: o agente pode aprender um atalho que atravessa train e held-out porque ambos compartilham o mesmo protocolo.

**Padrão reutilizável:** `candidate → held-out → Challenger modifica o protocolo mantendo a semântica → validity firewall → confirmar em conjunto disjunto → arquivar contraexemplo → tornar a descoberta uma constraint permanente → só então promover`. Em agentes MCP, isso poderia incluir mudar ordem/nomes de tools, inserir respostas semanticamente equivalentes, variar schemas ou perturbadores controlados para verificar se a melhoria depende de um detalhe acidental.

Fonte: https://arxiv.org/abs/2609.18366

## O que muda na arquitetura geral

Depois desta rodada, eu acrescentaria duas peças ao pipeline que vinha se formando.

Primeiro, **procedimento aprendido deve ser modular**:

```text
trace + outcome
    ↓
credit attribution
    ↓
policy / skill / routing component específico
    ↓
candidate
```

Segundo, **a avaliação também precisa ser adversarial e evolutiva**:

```text
candidate
    ↓
replay + held-out
    ↓
counterfactual / protocol stress test
    ↓
regression + integrity gate
    ↓
discard | archive | promote
```

Para multiagentes, acrescentaria um loop paralelo:

```text
competence map
    ↓
adaptive routing
    ↓
targeted experience
    ↓
agent specialization
    ↓
atualizar competence map
```

O insight novo mais importante é que existem agora **dois tipos de coevolução** que precisam ser controlados:

1. **agente ↔ policy/skill/harness**, como EvolveTrade e EvoSkill;
2. **worker ↔ router**, como CERA-MoA.

E CHASE lembra que existe um terceiro adversário silencioso: **agente ↔ evaluator**. Se o Evolver aprende a explorar a régua, o sistema pode mostrar um score maior sem realmente ficar melhor.

Ainda não apareceu nesta rodada uma demonstração nova de **RSI forte**, em que a versão melhorada fique melhor também em produzir a próxima versão e passe a controlar o próprio mecanismo de evolução, avaliação e promoção. Também não encontrei um novo deployment de produção comparável a LinkedIn/Tencent/Warp/Meta fechando autonomamente `experiência real → variante → avaliação → promoção → rollback`.

Um trabalho adjacente, **Infinite-Parameter LLMs: Generating and Adapting Weights from Live Data** (https://arxiv.org/abs/2609.18842), mostra adaptação paramétrica em runtime, mas eu não o promovi como caso principal porque a evidência de persistência apresentada é mais claramente dentro da interação/estado online do que de aprendizado persistente entre execuções independentes, que é o critério central deste radar.
