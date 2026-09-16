# Radar IA Autônoma — 2026-09-16

**Rodada:** 2026-09-16 18:16 BRT

Nesta rodada há três desenvolvimentos relevantes que ainda não tinham entrado no radar. O mais importante e mais recente é **ScienceBuddy**, submetido em 15/09/2026, porque combina em um mesmo ciclo melhoria persistente do harness e atualização persistente dos pesos do modelo. **Dream-RSI** e **RSIAgent**, ambos de 14/09, também são relevantes e não haviam sido registrados nas rodadas anteriores. Não encontrei hoje um novo caso comercial em produção, no nível LinkedIn/Tencent/Warp/Meta, fechando autonomamente todo o ciclo de promoção.

## Classificação

| Caso | (1) Pesos persistentes | (2) Código/scaffold/harness persistente | (3) Prompts/retrieval/workflows/tools/skills/estratégias | (4) Só memória/contexto | (5) Otimização fortemente conduzida por humanos |
|---|---:|---:|---:|---:|---:|
| **ScienceBuddy** | **Sim** | Não como foco do experimento | **Sim — instruções, skills e contexto** | Não | **Sim/parcial** |
| **Dream-RSI** | Não | **Sim — código da policy de exploração** | **Sim — estratégia de exploração** | Não | Parcial |
| **RSIAgent** | Não | Não | Não há evidência de política persistente alterada | **Sim — principal artefato persistente é memória causal** | Parcial |

## 1. ScienceBuddy — harness e pesos melhorando em ciclos acoplados

**Fonte:** https://arxiv.org/abs/2609.17523

ScienceBuddy introduz um mecanismo chamado **Recursive-in-Recursive Self-Improvement**. Ele alterna duas recursões:

1. **recursão interna:** mantém o modelo congelado e melhora o harness;
2. **recursão externa:** mantém o harness selecionado e atualiza os pesos do modelo via reinforcement learning.

O resultado de uma fase passa a ser o ponto de partida da seguinte. O harness pode reter alterações em instruções, skills e configurações de contexto; depois o modelo é treinado sob esse harness melhorado e o par modelo+harness é reavaliado antes do ciclo seguinte.

### O que melhorou sozinho

No loop de harness, um agente auxiliar diagnostica falhas e propõe alterações limitadas em uma skill, instrução ou configuração de contexto por vez. Ferramentas, infraestrutura, rubricas e evaluators permanecem protegidos. Candidatas são comparadas com a versão atual usando as mesmas tarefas, seeds e orçamento, com regressão sobre tarefas anteriormente bem-sucedidas.

Na fase de model learning, novas trajetórias on-policy são geradas e os pesos são atualizados com RL/GRPO usando recompensas baseadas em rubricas.

### O que persiste

- harness selecionado: instruções, skills e configurações de contexto;
- novo checkpoint do modelo;
- versões anteriores de harness e ambiente são preservadas;
- o par atualizado é levado ao ciclo seguinte.

### Métricas

Começando de Qwen3.5-4B, os autores executaram três ciclos sucessivos. A validação do harness aumentou, respectivamente, de **38,9% para 44,4%**, **34,4% para 46,7%** e **61,1% para 70,0%**. Na mesma sequência, o reward médio de treinamento aumentou de **33,3% para 38,8%**, **44,1% para 60,5%** e **57,8% para 69,8%**.

No teste científico held-out, a acurácia single-attempt aumentou de **42,2% para 73,3%**. Entre todos os problemas, **33,3% passaram de incorretos para corretos**, enquanto **2,2% regrediram de corretos para incorretos**.

Separando os mecanismos, com pesos congelados a adaptação do harness elevou a validação de **31,1% para 51,1% (+20 pp)**. Com o harness congelado, o RL elevou a cobertura pass@4 de **48,3% para 67,8% (+19,5 pp)**.

### Intervenção humana e limite de RSI

O framework, os limites de edição, as tarefas, rubricas e critérios de seleção são desenhados externamente. O próprio paper ressalta que o **reflector permanece fixo**. Portanto o mecanismo que produz melhorias não demonstra ficar mais capaz com as melhorias produzidas. Isso é uma forma forte de coevolução modelo+harness, mas ainda não é RSI forte no sentido de a própria máquina de melhoria se autoaperfeiçoar.

### Padrão arquitetural reutilizável

```text
interações reais
  -> extrair tarefa + rubrica
  -> diagnosticar falhas
  -> editar skill/instrução/contexto de forma limitada
  -> paired evaluation + regression gate
  -> promover harness
  -> gerar trajetórias on-policy
  -> RL / consolidação em pesos
  -> reavaliar harness herdado com o novo modelo
  -> próximo ciclo
```

O ponto mais útil é a arquitetura de **duas velocidades**: mudanças rápidas, auditáveis e reversíveis no harness; consolidação mais lenta nos pesos apenas depois que a experiência gera sinal verificável.

## 2. Dream-RSI — transformar traces históricos em um simulador para evoluir a policy

**Paper:** https://arxiv.org/abs/2609.14858  
**Projeto:** https://dream-rsi.com/

Dream-RSI melhora persistentemente a **política de exploração**, deixando o coding agent subjacente inalterado. A policy é código executável que decide onde continuar a busca, quais tentativas executar em paralelo e quando parar.

### O que melhorou sozinho

Uma exploração online gera uma árvore contendo decisões, branches, artefatos, custos e resultados. Em vez de tratar esse histórico apenas como memória textual, o sistema o transforma em um **replay simulator**. Um policy-development agent escreve variantes do código da policy e as testa contra essas árvores sem rerodar os experimentos caros. A melhor candidata volta para a execução online, gera uma nova árvore e amplia o simulador disponível para a rodada seguinte.

```text
online exploration
  -> discovery tree
  -> replay simulator
  -> milhares de policies candidatas
  -> replay barato
  -> selecionar vencedora
  -> deploy online
  -> nova discovery tree
  -> repetir
```

### O que persiste

O artefato persistente realmente otimizado é **o código da exploration policy**. O coding agent, o evaluator, os modelos e as interfaces de execução ficam fixos.

### Métricas

Em oito tarefas distribuídas entre engenharia de algoritmos, otimização matemática e kernels de GPU, o método reporta:

- até **162× menos chamadas do discovery agent** que SimpleTES em Lasso;
- cerca de **1,7× menos chamadas** que uma política de exploração fixa em uma comparação reportada;
- mais de **50× de economia de orçamento** em alguns problemas de otimização matemática versus SimpleTES;
- **1,79×–2,43× menos gerações** para atingir alvos em kernel engineering ou até **2,09× maior desempenho** sob orçamento comparável.

### Limitações

O replay é exato apenas para regiões que já foram exploradas. Ele não consegue revelar branches que nunca existiram no histórico. Além disso, a garantia de que a nova policy não é pior vale no conjunto de mundos/replays acumulados, não necessariamente em uma distribuição futura desconhecida.

Também não é RSI forte: o **policy-development agent é fixo**, assim como o evaluator. O sistema melhora a policy que controla exploração, mas não demonstra melhorar o mecanismo que escreve e seleciona essas policies.

### Padrão arquitetural reutilizável

Este é um padrão muito útil para agentes próprios e MCP:

```text
trace real caro
  -> converter em replay/shadow world
  -> gerar variantes de routing/workflow/policy
  -> executar variantes offline contra traces históricos
  -> regression + custo + qualidade
  -> somente a vencedora ganha rollout real
```

Em vez de testar cada nova policy de routing, sequência de tools ou workflow no ambiente real, pode-se primeiro fazer **shadow evaluation** sobre traces armazenados.

## 3. RSIAgent — exploração autônoma forte, mas a persistência ainda é memória

**Paper:** https://arxiv.org/abs/2609.15364  
**Projeto:** https://aetherlabsai.github.io/RSIAgent/  
**Blog:** https://aetherlabs.ai/articles/rsiagent-autonomous-exploration-for-recursive-self-improvement

RSIAgent merece registro, mas a classificação precisa ser rigorosa. Apesar de usar “Recursive Self-improvement” no nome, **os pesos do modelo e o agent harness permanecem inalterados**. O único artefato persistente é a memória construída autonomamente.

### O que o sistema faz

Um **curriculum agent** decide o que explorar; um **actor** executa ações; um **verifier** checa os resultados. O sistema usa duas fases:

- **Broad Recursive Self-exploration:** explora várias direções em paralelo para descobrir estrutura, ferramentas e procedimentos do ambiente;
- **Deep Recursive Self-exploration:** volta a casos difíceis, restrições escondidas e condições de fronteira.

Resultados verificados são consolidados como relações causais do tipo **ação + condição -> consequência**. Depois a memória é congelada e reutilizada em tarefas seguintes.

### Por que classifico como categoria (4)

Há aprendizagem ativa no sentido de adquirir experiência de modo dirigido, mas não há evidência de uma alteração persistente da policy, do routing, do workflow, das tools, das skills do harness ou dos pesos. O próprio projeto afirma explicitamente que **model weights and agent harness remain unchanged**.

Portanto este caso é mais sofisticado do que memória episódica passiva, mas ainda é principalmente **memória causal persistente**, não otimização persistente do agente/harness.

### Métricas

No OSWorld 2.0, o partial score reportado passou de **71,97 para 78,98 (+7,01 pp)** e o full-task success de **37,80% para 42,68%**. No Agents' Last Exam Near-term, o partial passou de **83,75 para 84,82 (+1,07 pp)** e o full-task success de **49,25% para 50,75%**.

Há ressalvas importantes: as comparações com modelos externos não são matched-budget e alguns agregados combinam entradas avaliadas com RSI com scores baseline retidos. Portanto o dado mais confiável para nosso radar é o ganho do próprio agente com/sem a memória de exploração, não a afirmação de superar universalmente modelos frontier.

### Padrão reutilizável

```text
ambiente desconhecido
  -> curriculum amplo
  -> actor
  -> verifier
  -> memória causal validada
  -> curriculum profundo nos gaps
  -> congelar memória
  -> reutilizar
```

Para transformar isso numa categoria (3) mais forte, eu adicionaria uma etapa posterior:

```text
memória causal validada
  -> compilar candidato em Skill / routing rule / playbook
  -> replay + held-out
  -> promote/rollback
```

Assim a experiência deixa de ser apenas contexto recuperado e passa a modificar explicitamente a política operacional.

## Síntese da rodada

A principal novidade é **ScienceBuddy**, porque reúne no mesmo sistema os dois mecanismos que vêm aparecendo separados no radar: **evolução rápida do harness** e **consolidação lenta nos pesos**.

Dream-RSI acrescenta outra peça importante: traces históricos podem funcionar como **simuladores de replay**, permitindo testar milhares de políticas de exploração sem pagar novamente pelo ambiente real.

RSIAgent é útil principalmente como contraexemplo classificatório: **um agente pode explorar autonomamente, verificar experiência e melhorar bastante com memória, sem que o harness ou o modelo tenham realmente evoluído**. Crescimento de memória não deve ser confundido automaticamente com otimização de policy.

A arquitetura que eu levaria para agentes próprios após esta rodada é:

```text
Live Trace
  -> Task/Rubric Extraction
  -> Credit Attribution
  -> Active Exploration quando faltar evidência
  -> Grounded Replay / Shadow World
  -> Candidate Skill / Policy / Workflow
  -> Fixed Evaluator + Regression Gate
  -> Promote Harness
  -> Fresh On-Policy Experience
  -> opcional: RL/Fine-tuning para consolidar ganhos repetidos em pesos
  -> reavaliar o harness com o novo modelo
```

A regra nova é separar claramente **aquisição de experiência** de **otimização persistente**. Uma memória que cresce é útil, mas não equivale a uma policy, skill, workflow, harness ou modelo que foi efetivamente melhorado e promovido.

Ainda não há, nesta rodada, uma demonstração convincente de **RSI forte** em que a versão melhorada torne também mais capaz o próprio mecanismo que produz, avalia e promove a próxima versão. ScienceBuddy chega perto por acoplar duas recursões, mas mantém o reflector fixo; Dream-RSI evolui a exploration policy, mas mantém fixo o agente que a desenvolve; RSIAgent mantém pesos e harness fixos.