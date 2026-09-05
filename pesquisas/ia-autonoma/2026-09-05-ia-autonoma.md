# Radar de IA autônoma — 2026-09-05

**Rodada:** 17:54 (America/Sao_Paulo)

Há quatro desenvolvimentos relevantes nesta rodada. Três são trabalhos acadêmicos submetidos em 2 de setembro que não tinham entrado nas rodadas anteriores — SkillGLoW, MASkills e CHIME — e um é uma atualização de produção da Warp, lançada em 3 de setembro. O padrão comum é cada vez mais claro: os pesos do modelo podem ficar congelados enquanto o sistema acumula melhorias persistentes em skills, memória estruturada, retrieval, roteamento e configurações do harness. O ponto novo não é apenas “lembrar o que aconteceu”, e sim atribuir crédito, consolidar experiência em procedimentos reutilizáveis e submeter cada mudança a um gate antes de promovê-la.

| Caso | (1) Pesos persistentes | (2) Código/scaffold/harness | (3) Skills/retrieval/workflow/tools/estratégia | (4) Só memória/contexto | (5) Forte condução humana |
|---|---:|---:|---:|---:|---:|
| SkillGLoW | Não | Não no núcleo | **Sim** | Não | Parcial |
| MASkills | Não | Não no núcleo | **Sim** | Não | Parcial |
| CHIME | Não | Não | **Sim — memória + retrieval + valores evoluem** | Não | Parcial |
| Warp Factory Benchmarks / self-improvement loop | Não demonstrado | Configuração da factory é código | **Sim** | Não | **Sim — gate humano** |

## SkillGLoW — aprender procedimentos, não armazenar tarefas

O SkillGLoW foi submetido em 2 de setembro de 2026 e ataca um problema que aparece quando um agente começa a acumular centenas de skills: uma biblioteca por tarefa cresce indefinidamente e tende a guardar detalhes específicos demais, enquanto um único documento global tende a virar regras genéricas demais para serem úteis.

A solução é usar como unidade de aprendizado a **família procedural**. Durante uma tarefa, o agente gera uma skill local a partir de múltiplas execuções e do feedback do verifier. Depois da rodada, essas skills locais são agrupadas pelo modo de resolver o problema, e não simplesmente pelo tema da tarefa. Cada cluster é comprimido em um prior global contendo somente condições de aplicabilidade, procedimento central e falhas comuns. Detalhes específicos da instância não entram na memória de longo prazo; são regenerados localmente quando uma nova tarefa é executada.

O prior global não é gravado automaticamente. Uma revisão candidata é executada e comparada com a biblioteca atualmente implantada e com o baseline sem skill. Ela só é commitada quando a execução real mostra que não degradou o sistema. Portanto o ciclo é: `execução → skill local → cluster procedural → prior candidato → verifier → commit ou manter versão anterior`.

Os pesos ficam congelados. O que persiste é a biblioteca de procedimentos. Em 12 execuções de melhoria contínua, cobrindo Terminal-Bench-Pro, SWE-bench Verified, ALFWorld e LiveMathematicianBench com três modelos diferentes, os priors globais ganharam em média **17,2 pontos** sobre o baseline sem skills. Com regeneração local, o ganho médio chegou a **18,0 pontos**. A biblioteca ficou **3,6× mais compacta** que um pool por tarefa. Em ALFWorld não visto durante a consolidação, a mesma biblioteca, sem nova edição, elevou o sucesso de **73,9% para 83,9%**.

Isso é categoria (3), não categoria (4): a experiência não fica apenas como memória textual para consulta; ela é transformada, generalizada, testada e promovida como um novo procedimento operacional persistente.

Padrão reutilizável para agentes próprios: manter `skill_local` apenas como evidência transitória; consolidar várias experiências em `skill_family`; remover IDs, nomes e detalhes específicos; testar a versão generalizada contra tarefas reais; persistir somente a candidata que passa no gate. Isso reduz tanto overfitting quanto crescimento descontrolado da biblioteca.

Fonte: https://arxiv.org/abs/2609.02217

## MASkills — atribuição de crédito no nível da skill em sistemas multiagente

MASkills, também de 2 de setembro e aceito no EMNLP 2026 Findings, trata um problema mais difícil: quando vários agentes e várias skills colaboram numa execução, um score final do time não diz qual skill ajudou ou prejudicou o resultado.

O framework registra explicitamente cada invocação de skill. Um critic central produz crédito em linguagem natural para cada skill usada, comparando seu efeito com o contrafactual em que aquela skill não tivesse sido chamada. Feedback de várias trajetórias é agregado hierarquicamente e recebe uma espécie de “momentum” em espaço de linguagem: direções de edição que aparecem repetidamente entre ciclos recebem mais peso que críticas isoladas e contraditórias.

A biblioteca pode então sofrer quatro tipos de mutação: **refinement**, **induction**, **consolidation** e **pruning**. Uma skill pode ser corrigida, uma nova skill pode nascer quando a falha não é explicada pelas atuais, skills redundantes podem ser fundidas e skills persistentemente ruins podem ser removidas. Antes de qualquer alteração entrar na biblioteca, a candidata roda em um conjunto held-out; se não mantiver o desempenho dentro da tolerância definida, ocorre rollback.

Novamente os pesos dos atores ficam inalterados; GPT-5.1 atua como optimizer das skills, enquanto GPT-4o-mini e Qwen2.5-7B são usados como atores nos benchmarks. Portanto a classificação principal é (3).

Nos resultados, MASkills alcançou **76,3 F1 no HotpotQA**, contra 69,2 do melhor baseline mostrado; no GAIA atingiu **23,3% de sucesso médio**, contra 20,4 do R1-Searcher; e no LoCoMo multi-hop chegou a **17,22 F1**, contra 12,04 do baseline LoCoMo. A ablação é ainda mais importante: retirar o mecanismo de validation/rollback derrubou o score de **17,2 para 6,6** no LoCoMo multi-hop e de **23,3 para 13,5** no GAIA. Isso mostra que, para evolução contínua, o rollback não é detalhe operacional — é parte central do algoritmo.

Padrão reutilizável: em um sistema multiagente, cada chamada de skill/tool deve gerar um registro identificável (`agent`, `skill`, `tool`, `resultado`, `score`). O evaluator não deve apenas dizer “a execução falhou”; deve tentar responder “qual skill contribuiu para a falha?”. Depois, alterações recorrentes podem ser acumuladas como uma direção de melhoria persistente. O componente só é promovido depois de held-out + rollback.

Fonte: https://arxiv.org/abs/2609.02094

## CHIME — “attribute before memorize”: decidir quem causou o resultado antes de aprender com ele

CHIME, submetido em 2 de setembro, é um caso de fronteira entre memória e otimização. Ele mantém os pesos congelados e evolui uma memória externa, mas essa memória não é apenas um log. Cada item possui condição de aplicabilidade, experiência reutilizável, valor aprendido e contagem de reutilização; esses valores alteram reranking, retenção e pruning. Por isso classifico o sistema em (3), e não em (4).

A inovação central é separar **planning memory** e **execution memory**. Depois da tarefa, um Credit Attribution Gate recebe tarefa, plano, trajetória, resultado e memórias recuperadas e decide se o resultado deve ser atribuído ao planejamento, à execução, aos dois ou a fatores externos. Só depois ocorre a escrita. O banco correspondente pode atualizar valores, fundir experiência, inserir novo conhecimento ou remover itens enganosos.

Nos quatro benchmarks de tarefas longas, a memória é congelada durante a avaliação para testar se aquilo que foi aprendido transfere para tarefas não vistas. CHIME superou o melhor baseline de avaliação em **2,96 pontos percentuais com Qwen3.5-Flash e 3,68 pontos com DeepSeek-V4-Flash**. O sistema chegou ao melhor desempenho armazenando apenas **129 memórias**, contra **3.585** de um baseline forte. Memórias de planejamento tiveram mais que o dobro do impacto observado das memórias de execução em uma análise: 21,7%→50,8% contra 23,7%→35,4%. A memória acumulada também transferiu entre backbones, superando a memória transferida do A-MapReduce em até **4,68 pontos**.

O padrão reutilizável é especialmente útil para MCP: não aprender diretamente de `success=false`. Primeiro classificar a causa — `PLAN`, `TOOL_SELECTION`, `ARGUMENT_PREPARATION`, `TOOL_EXECUTION`, `EXTERNAL` — e só então atualizar a memória ou skill da camada correspondente. Isso impede que um erro de API, por exemplo, contamine uma regra de planejamento que estava correta.

Fonte: https://arxiv.org/abs/2609.02074

## Warp — Factory Benchmarks coloca o evaluator no mesmo plano operacional do harness

A atualização mais prática desta rodada é a Warp. Em 3 de setembro a empresa lançou **Factory Benchmarks** em early access. A factory inteira é descrita em código (`factory.yaml` + definições de agentes), todas as trajetórias são armazenadas com prompt, conversa, estado do Git e artefatos, e uma execução anterior pode ser recriada a partir do estado inicial usando outra configuração. Scorers avaliam dimensões como correção, eficiência, verbosidade e custo.

O benchmark pode variar modelo e, progressivamente, harness. A Warp diz que já utilizou essa infraestrutura internamente para reduzir o custo por PR em aproximadamente **63% nas últimas semanas**, sem perda de qualidade em determinados tipos de tarefas. A mesma infraestrutura alimenta seus self-improvement loops. Além disso, o foreman pode gerar a própria definição do benchmark a partir de runs anteriores e agentes podem criar ou ajustar model routers persistentes, que passam a selecionar configurações diferentes conforme a classe de tarefa.

Há, porém, uma limitação importante para nossa classificação. A própria Warp diz que esses benchmarks são caros e **não recomenda executá-los automaticamente**; eles devem ser disparados quando surge um novo modelo ou quando alguém está considerando mudanças em prompts, skills ou contexto. No workshop de self-improvement de 3 de setembro, a empresa descreve um evaluator que percorre logs e um agente de melhoria que propõe atualizações de Skills **para o time revisar e fazer merge**. Portanto isto é uma combinação de (3) + (5), e ainda não um loop autônomo de `produção → variante → benchmark → promoção` sem humano.

Mesmo assim, o padrão está ficando muito concreto para engenharia: `traces reais → benchmark reproduzível → scorer multidimensional → variante de model/harness/skill/router → replay → comparação Pareto → merge/promoção`. É exatamente a infraestrutura necessária para tornar um self-improvement loop mensurável e versionável.

Fontes: https://www.warp.dev/blog/warp-factory-benchmarks e https://www.warp.dev/events/self-improvement-loops-for-agents

## Síntese arquitetural da rodada

Os quatro casos reforçam uma convergência. Um agente robustamente autoaperfeiçoável não deveria transformar diretamente uma experiência em memória ou skill. O pipeline que está emergindo é:

`Execution Trace → Credit Attribution → Local Evidence → Procedural Consolidation → Candidate Skill/Memory/Router → Held-out/Replay → Gate → Commit ou Rollback`.

SkillGLoW resolve **em que granularidade** guardar o aprendizado: procedimentos por família, não tarefas individuais. MASkills resolve **quem merece o crédito** num sistema com vários agentes e skills. CHIME resolve **qual estágio deve aprender** antes de gravar qualquer memória. A Warp mostra como os mesmos princípios começam a virar infraestrutura de produção: factory-as-code, replay, scorers, A/B de configuração e roteamento persistente.

Não encontrei nesta rodada um novo caso forte de categoria (1), com pesos sendo atualizados continuamente a partir de experiência de produção, nem um novo caso de categoria (2) no nível do HarnessEvolve/HarnessDev em que o próprio código do harness seja autonomamente reescrito e promovido. Também não apareceu um novo caso convincente de recursive self-improvement aberto em que a versão melhorada assuma automaticamente o papel de Evolver e produza a geração seguinte.
