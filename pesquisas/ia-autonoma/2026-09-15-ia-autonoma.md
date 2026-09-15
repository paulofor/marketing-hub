# IA autônoma — 2026-09-15

## Rodada 18:08 — America/Sao_Paulo

Nesta rodada, **um avanço novo passa claramente o filtro**: **SkillLift**, submetido ao arXiv em 14/09/2026, melhora skills persistentes de agentes sem alterar os pesos do LLM e ataca um gargalo que apareceu repetidamente nas últimas rodadas: o custo de avaliar cada variante com um rollout completo do agente. Também registro **Atria Dawn** como desenvolvimento adjacente importante, mas em categoria diferente: há mudança persistente de pesos e agentes participando do desenvolvimento de sucessores, porém o ciclo ainda é essencialmente conduzido por humanos e não é continual self-improvement em produção.

| Caso | (1) Pesos | (2) Código/scaffold/harness | (3) Prompts/retrieval/workflows/tools/skills | (4) Só memória | (5) Forte condução humana |
|---|---:|---:|---:|---:|---:|
| **SkillLift** | Não | Não | **Sim — skill persistente** | Não | Parcial: benchmark/oracle definidos por humanos |
| **Atria Dawn** | **Sim** | Não demonstrado como autoevolução persistente | Experiência/tool use alimentam treinamento | Não | **Sim — principal** |

## SkillLift — aprender uma “régua” barata antes de gastar um rollout caro

SkillLift trata uma skill como um artefato externo e persistente — essencialmente um procedimento/prompt reutilizável — que pode ser revisado entre execuções enquanto os pesos do agente permanecem congelados. O problema atacado é econômico: em métodos anteriores, quase toda revisão precisa ser avaliada executando o agente ponta a ponta. Isso faz cada passo de evolução custar um rollout completo e limita o número de variantes que podem ser exploradas.

O mecanismo novo é separar **busca** de **oracle evaluation**. O sistema aprende uma rubric alinhada ao oracle usando comparações/ranking entre skills. No loop interno, essa rubric permanece congelada e funciona como surrogate barato, fornecendo feedback denso para gerar e revisar várias candidatas sem chamar o oracle a cada alteração. Periodicamente, um loop externo executa um pequeno número de rollouts reais, compara o ranking da rubric com o ranking do oracle e reajusta a rubric. A skill vencedora persiste e é usada nas execuções seguintes.

Fluxo simplificado:

```text
skill atual
  → gerar população de variantes
  → rubric barata avalia/revisa candidatos
  → poucas variantes vão para rollout/oracle real
  → alinhar rubric por ranking
  → repetir
  → promover skill vencedora
```

**O que melhorou sozinho:** o conteúdo da skill procedural. **O que persiste:** a skill evoluída (e, durante a otimização, o estado/rubric de avaliação). **Pesos do LLM:** permanecem congelados. **Intervenção humana:** os pesquisadores definem tarefas, verifier/oracle e protocolo; a evolução textual dentro do loop é automática.

Nos experimentos, SkillLift foi avaliado em 147 tarefas — 60 do WildClawBench e 87 do SkillsBench — com GPT-5.4/GPT-5.4-mini, GLM-5.1 e DeepSeek-V4-Pro. O método venceu as seis combinações modelo–benchmark. No SkillsBench, obteve 62,5% com GPT-5.4-mini, 75,1% com GLM-5.1 e 74,3% com DeepSeek-V4-Pro; os respectivos SkillOpt ficaram em 58,2%, 70,5% e 69,0%. Em relação ao teto de skills estáticas, o paper reporta ganhos de +8,8 a +11,5 pontos percentuais no WildClawBench e +16,7 a +24,2 pp no SkillsBench.

O ganho mais importante para sistemas reais é o custo: para atingir 90% do patamar final de desempenho, SkillLift usa **40–70% menos tokens** que SkillOpt e CoEvoSkills. Os ratios de custo mostrados no paper chegam a cerca de 2,7× em favor do SkillLift em algumas configurações. O benefício é maior em tarefas com verificação objetiva; categorias criativas/subjetivas melhoram bem menos, o que mostra que a qualidade do evaluator continua sendo um limite central.

### Padrão arquitetural reutilizável

Isto sugere adicionar uma camada de **surrogate evaluator aprendido** entre Evolver e evaluator caro:

```text
Trace / Failure
  → Evolver cria várias skills candidatas
  → Surrogate/Rubric barato ranqueia e fornece feedback
  → apenas candidatas informativas vão ao evaluator real
  → evaluator real recalibra a rubric
  → held-out / regression
  → promote | archive | rollback
```

Para agentes com MCP, a mesma ideia pode ser aplicada a `SKILL.md`, descrições de tools, routing rules e pequenos workflows. Em vez de executar um benchmark completo para cada alteração textual, usa-se uma régua aprendida para explorar barato e reserva-se o evaluator confiável para recalibrar e decidir promoção. A ressalva é importante: **a rubric não pode virar a autoridade final**, porque ela própria pode se desalinha do objetivo real; o paper mostra que remover o realinhamento com o oracle derruba bastante o desempenho.

Fonte principal: https://arxiv.org/abs/2609.15396

## Atria Dawn — importante para AI-improving-AI, mas ainda não é self-improvement em produção

O paper **Atria Dawn: The Dawn of Agentic Superintelligence**, submetido em 14/09/2026, descreve um foundation agentic language model treinado por um **Verifiable Experience Pipeline** que conecta interações com ferramentas a ambientes executáveis e resultados verificados externamente. O trabalho é relevante para este radar porque os autores estudam explicitamente agentes como participantes no desenvolvimento de seus sucessores.

A classificação correta, porém, é **(1) + (5)**: há mudança persistente nos pesos do modelo treinado, mas o pipeline de treinamento, os objetivos, a seleção de evidências e as decisões de desenvolvimento continuam fortemente humanos. Não há demonstração de um agente em produção que observe seu próprio desempenho, decida retreinar-se, altere autonomamente seu processo de treinamento e promova o novo checkpoint.

O paper avalia Atria Dawn em 16 benchmarks e reporta melhor resultado publicado em cinco deles. Além disso, analisa 769 registros de tarefas de 56 participantes durante o processo real de P&D. Os agentes frequentemente propuseram métodos e implementaram revisões, mas os humanos mantiveram a maior parte das decisões finais e orientaram a exploração. Cerca de um terço das tarefas concluídas com auxílio da IA foi avaliado pelos participantes como inviável sem IA em condições comparáveis.

**O que melhorou sozinho:** não há evidência de self-improvement autônomo fechado; o que existe é experiência agentic verificável sendo convertida por um processo de treinamento humano em um sucessor mais capaz. **O que persiste:** os novos pesos e o conhecimento internalizado no modelo. **Intervenção humana:** alta.

### Padrão reutilizável

O ponto útil para agentes próprios é a separação entre **experiência verificável** e simples log:

```text
agent execution
  → tool trace + artifact + executable outcome
  → external verification
  → experiência elegível para aprendizado
  → treinamento/consolidação posterior
```

Isso reforça a arquitetura de duas velocidades observada nas rodadas anteriores: mudanças rápidas e reversíveis em skills/harness; somente experiências repetidamente verificadas viram candidatas a consolidação em pesos.

Fonte principal: https://arxiv.org/abs/2609.15818

## Síntese da rodada

O avanço realmente novo desta rodada é o **SkillLift**. Ele acrescenta uma peça que faltava à CI/CD de agentes autoaperfeiçoáveis: **como explorar muitas melhorias sem pagar um rollout completo para cada candidata**. Depois de COBRA-Skills, que decide onde gastar o orçamento de avaliação, SkillLift mostra outra estratégia: aprender uma rubric barata e periodicamente recalibrá-la com um oracle caro e confiável.

A arquitetura acumulada fica mais clara:

```text
Execution Trace
  → Credit Attribution
  → Candidate Population
  → cheap learned rubric / surrogate
  → selective real rollouts
  → oracle realignment
  → held-out + regression + integrity gates
  → discard | archive | promote
  → versão persistente
```

Ainda não apareceu nesta rodada um caso convincente de **recursive self-improvement forte** em que a versão melhorada assuma de modo autônomo o mecanismo que cria, avalia e promove sua própria sucessora. Atria Dawn aproxima a IA do desenvolvimento de sucessores, mas mantém a autoridade e o processo de treinamento sob controle humano.