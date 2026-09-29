# Radar IA Autônoma — 2026-09-29

**Rodada:** 2026-09-29 18:23 (America/Sao_Paulo)

Nesta rodada, quatro desenvolvimentos novos passaram o filtro de relevância: **PluginRSI**, **SkillVine**, **Recursive Harness Distillation (RHD)** e **SCLATE**. Os três primeiros tratam diretamente de melhoria persistente de harness/skills sem alterar o backbone principal; SCLATE entra como infraestrutura de continual learning e como contraponto de mudança persistente de pesos conduzida por humanos.

## Classificação

| Caso | (1) Pesos | (2) Código/scaffold/harness | (3) Prompts/retrieval/workflows/tools/skills/estratégias | (4) Só memória/contexto | (5) Otimização humana |
|---|---:|---:|---:|---:|---:|
| PluginRSI | Não | **Sim — principal** | **Sim** | Não | Parcial |
| SkillVine | Não | Não | **Sim — principal** | Não | Parcial |
| Recursive Harness Distillation | Não | Harness externo | **Sim — playbook persistente** | Não | Parcial |
| SCLATE + post-training | **Sim** | Harness permanece externo | Memória/harness usados durante treino | Não | **Sim — principal** |

## 1. PluginRSI — o harness deixa de evoluir como um monólito e passa a acumular mecanismos reutilizáveis

**Fonte:** https://arxiv.org/abs/2609.32423

PluginRSI representa o harness como uma composição de plugins atomizados. Em vez de reescrever o programa inteiro a cada rodada, ele melhora plugins individualmente, mantém o restante do harness fixo para medir contribuição causal e acumula variantes úteis numa biblioteca compartilhada. Depois existe uma segunda etapa de recomposição global, em que plugins da biblioteca são combinados e o workflow que os coordena pode ser revisto.

O que persiste entre execuções é, portanto, **código de plugins, workflow e biblioteca de mecanismos reutilizáveis**. Não há atualização do backbone durante essa busca; o ganho vem do estado externo do harness.

Nos resultados, em SWE-bench Verified com Kimi-K3 como solver, PluginRSI chegou a **69% no held-out**, contra **63% do Meta-Harness**. Com GPT-5.6 Terra como harness otimizado, o held-out foi **65%**, contra 55% do melhor baseline comparável nessa configuração. No Terminal-Bench 2.1, PluginRSI chegou a **73,3% held-out**, contra **66,7% do Meta-Harness**.

A ablação é importante: remover plugin mutation derruba o held-out de 69% para **57%**; remover a recomposição do harness derruba para **59%**. A melhoria não vem apenas de inventar novos componentes nem apenas de rearranjá-los; os dois loops se complementam.

A biblioteca cresceu de **75 para 132 plugins em 15 passos**. Mais importante: reutilizar a biblioteca já evoluída a partir do harness inicial permitiu chegar a **69% held-out em dois passos**, contra 65% sem reutilização. Isso mostra que a experiência acumulada não serve apenas para o agente atual; ela acelera futuras buscas de harness.

### Padrão arquitetural reutilizável

```text
trace + métrica
      ↓
atribuir ganho/falha a um plugin
      ↓
mutar plugin isoladamente
      ↓
avaliar com resto do harness fixo
      ↓
plugin_library
      ↓
recompor workflow global
      ↓
PROMOTE melhor harness validado
```

Para agentes próprios, isso sugere separar **mecanismos executáveis pequenos** — routing, retrieval policy, tool strategy, memory strategy, verifier, retry logic — em registros versionados e reutilizáveis. Em vez de gerar nova imagem Docker por alteração, o runtime pode carregar a versão ativa de cada plugin a partir de um store externo.

### Limitações

A otimização ainda usa task sets fixos e poucas dezenas de iterações. A seed library inicial também foi curada com intervenção humana e inspeção manual. Transferência para domínios muito diferentes ainda é menor.

## 2. SkillVine — evolução de skills não deve ser uma linha; deve ser um grafo de versões

**Fonte:** https://arxiv.org/abs/2609.32731

SkillVine ataca um problema recorrente em self-improving agents: sistemas normalmente mantêm apenas uma cadeia linear de versões da skill library. Se uma atualização intermediária leva o agente a um ótimo local ruim, as próximas versões herdam essa decisão.

O sistema preserva **todas as versões da biblioteca de skills em um grafo recuperável**. Cada nó mantém a biblioteca completa, parent pointer, estado (accepted/rejected/current/best), validation accuracy, casos resolvidos/falhados e número de expansões. Um trunk recebe todos os batches de experiência, enquanto branches retornam a versões históricas promissoras e exploram caminhos alternativos.

O seletor de parent não olha apenas score. Ele favorece versões que:
- ainda têm boa qualidade;
- foram pouco exploradas;
- resolvem casos que o trunk atual falha.

SkillVine também adapta a granularidade da mutação. Se o update do trunk foi aceito, a branch pode aplicar várias edições de uma vez. Se o trunk rejeitou o batch, as branches passam a testar alterações uma a uma, voltando ao último estado aceito quando uma edição piora.

Nos experimentos em cinco benchmarks e dois modelos, SkillVine obteve o **melhor resultado em 9 de 10 combinações benchmark-modelo**. Com DeepSeek-V4-flash liderou os cinco benchmarks. E o detalhe mais forte: **todas as melhores bibliotecas finais foram encontradas em branches, não no trunk linear**.

### Padrão arquitetural reutilizável

```text
skill_v17  ──→ v18 ──→ v19   (trunk)
    │
    ├────────→ v17b ─→ v17c
    │
    └────────→ v17x ─→ v17y  ← BEST
```

Para um sistema próprio, isso muda o schema mental de:

```text
active_version = latest_version
```

para:

```text
version_graph
best_validated_version
active_version
parent_version
solved_cases
failed_cases
expansion_count
```

A consequência prática é importante: **não apagar versões rejeitadas ou antigas**. Elas podem ser bons pontos de partida para uma nova linha de evolução quando a distribuição de tarefas muda.

### Limitações

A promoção ainda depende de validation sets definidos por humanos. É skill evolution automática, mas não RSI forte: o seletor, critérios de aceitação e algoritmo de branching permanecem fixos.

## 3. Recursive Harness Distillation — experiência física vira playbook persistente que é refinado pelo feedback do agente que vai usá-lo

**Fonte:** https://arxiv.org/abs/2609.33378

RHD usa um agente forte como professor e um agente mais barato como executor. O VLA do robô fica congelado, assim como os modelos dos agentes. O que evolui é um **playbook externo** que descreve quando intervir, como intervir e o que verificar depois.

O agente forte primeiro interage com a policy e cria o playbook. Em seguida o agente leve executa tarefas com esse playbook. O agente forte revisa os traces reais do agente leve — sucessos, falhas, intervenções e efeitos — e produz uma nova versão. Cada candidata é avaliada em rollouts do agente leve; só uma versão que atinge o target é aceita. O playbook aceito vira base para a rodada seguinte.

Isso produz um ponto muito relevante: conhecimento não é otimizado apenas para “o professor sabe fazer”, mas para **o destinatário consegue aplicar**.

Em SimplerEnv Bridge:
- GR00T sozinho: **41,7%**
- Luna sem playbook: **43,8%**
- Luna com playbook refinado: **66,7%**
- Astra com o mesmo playbook: **79,2%**

A versão inicial do playbook chegou a piorar Luna de 43,8% para **31,3%**. Só o refinement com feedback do recipient elevou para 66,7%. Isso mostra que “destilar experiência” uma vez não basta; a orientação precisa ser refinada pelo comportamento do agente que realmente vai consumi-la.

No robô físico Franka Panda, em 75 trials, a policy sozinha teve **37,3%** de sucesso; Luna sem playbook teve 0%; Luna com o playbook refinado chegou a **64,0%**.

Também há uma limitação reveladora: quando Luna é usada como teacher de si própria, o playbook refinado chega a apenas **22,9%**, contra 66,7% quando Astra é o teacher. Ou seja, o loop é recursivo no artefato externo, mas depende de um teacher mais competente.

### Padrão arquitetural reutilizável

```text
strong agent
   ↓
experiência
   ↓
playbook_v1
   ↓
cheap/deployment agent executa
   ↓
recipient traces
   ↓
strong agent revisa playbook
   ↓
eval no próprio recipient
   ↓
playbook_v2
```

Esse padrão é especialmente útil para produção: um modelo caro pode atuar raramente como **teacher/evolver**, enquanto um modelo barato usa o conhecimento persistente no dia a dia.

### Limitações

O teacher é fixo e mais forte; o mecanismo de revisão não melhora a si mesmo. No deployment físico, a policy base também havia sido fine-tuned com demonstrações humanas antes de ser congelada.

## 4. SCLATE — continual learning precisa avaliar modelo, harness e memória como um sistema único

**Fonte:** https://arxiv.org/abs/2609.32391

SCLATE, da Apple, não é um self-improver por si só. Ele é uma infraestrutura para executar e treinar agentes que vivem por várias sessões, com eventos de stop/start, crons, consolidação de memória e persistência de estado numa timeline reproduzível.

A contribuição para este radar é dupla. Primeiro, o paper mostra que **adicionar um sistema de memória não melhora de forma confiável um agente**: modelos diferentes usam de maneiras diferentes o mesmo harness e a mesma memória. Segundo, o framework permite post-training do modelo através do harness e memory system sem modificá-los.

No experimento com Qwen3.5-4B, o modelo pós-treinado:
- leu **6,8× menos linhas de arquivos**;
- obteve **+16,7 pontos** de pass rate em SWE-bench Verified;
- melhorou em até **+11,8 pontos** no MetaClaw held-out;
- passou a escrever registros de memória mais ricos.

### Classificação

Este caso é **(1)+(5)**: os pesos mudam, mas o processo de post-training, scheduler, benchmarks e critérios são definidos por humanos. Não é um agente decidindo autonomamente quando e como se retreinar.

### Padrão arquitetural reutilizável

SCLATE sugere que o objeto de avaliação correto não é:

```text
modelo sozinho
```

mas:

```text
modelo + harness + memory + lifecycle events
```

Para um sistema próprio que evolui skills/harness, isso significa que cada candidata deve ser testada com o mesmo modelo e o mesmo ciclo de memória que haverá em produção, inclusive atravessando reinícios e consolidações.

## Síntese da rodada

O avanço conceitual mais importante de hoje é que a evolução externa ao modelo está ficando **mais modular e mais não linear**:

```text
ontem:
harness_v1 → harness_v2 → harness_v3

agora:
plugin library + recomposition
          +
version graph de skills
          +
recipient-aware refinement
```

Isso reforça uma arquitetura de runtime estático + estado comportamental externo. O que evolui frequentemente — plugins, skills, playbooks, routing, policies e workflows — pode ficar num store versionado. A imagem Docker muda apenas quando o interpretador/runtime realmente precisa mudar.

Uma estrutura de dados reutilizável seria:

```text
artifact_version
---------------
id
artifact_type        // plugin, skill, playbook, workflow
parent_id
status               // candidate, accepted, rejected, active, best
definition
validation_score
solved_cases
failed_cases
source_trace_ids
created_at

artifact_edge
-------------
parent_version_id
child_version_id
relation_type        // mutate, branch, recompose, refine

active_pointer
--------------
artifact_type
scope
active_version_id
best_validated_version_id
```

Ainda **não considero estes casos RSI forte**. PluginRSI evolui os mecanismos, mas o optimizer é fixo; SkillVine evolui o grafo de skills, mas o algoritmo de busca é fixo; RHD refina o playbook, mas depende de um teacher fixo; SCLATE treina pesos por um pipeline humano. O que está avançando rapidamente é a capacidade de **persistir, versionar, testar, recombinar e reutilizar experiência** sem precisar alterar continuamente o backbone.
