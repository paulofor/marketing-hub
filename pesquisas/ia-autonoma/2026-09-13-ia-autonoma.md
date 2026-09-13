# IA Autônoma — 2026-09-13

## Rodada 17:44 BRT

Há três itens relevantes que ainda não tinham entrado no radar e que mudam de forma importante a leitura sobre self-improving agents: **Ouroboros**, **ModularRSI** e **HarnessDev**. Nenhum deles é uma nova publicação de 13/09; são trabalhos/release muito recentes que apareceram nesta varredura e são relevantes demais para omitir. Também apareceu uma peça de infraestrutura prática da Anthropic — **plugin evals para Claude Code** — que não é autoaperfeiçoamento por si só, mas encaixa quase diretamente como gate para evolução de skills/plugins/MCP.

| Caso | (1) Pesos | (2) Código/scaffold/harness | (3) Prompts/retrieval/workflows/tools/skills | (4) Só memória | (5) Forte condução humana |
|---|---:|---:|---:|---:|---:|
| **Ouroboros** | Não | **Sim — principal** | **Sim** | Não | Parcial: revisão/governança externa |
| **ModularRSI** | Não | **Sim — principal** | **Sim** | Não | Sim, no desenho do loop/gates |
| **HarnessDev** | Não | **Sim — objeto principal** | **Sim** | Não | Sim, benchmark/evaluator |
| **Claude Code Plugin Evals** | Não | Não | Infra para avaliar (3) | Não | **Sim — ferramenta de avaliação** |

## 1. Ouroboros — um dos casos mais próximos de self-development persistente do próprio agente

O **Ouroboros: A Self-Developing Frontier Coding Agent with Reviewed Core Evolution** é um dos casos mais fortes encontrados até agora de um agente cujo **próprio código e harness podem mudar persistentemente**. O repositório mutável contém ferramentas, prompts, montagem de contexto, memória e implementação central. Quando uma alteração é aceita, ela entra como commit revisado e **a versão seguinte do agente passa a executar em cima daquele core modificado**.

O sistema descreve dois modos de evolução. No primeiro, chamado de **recursive free evolution**, melhorar o próprio agente é tratado como uma tarefa explícita e uma rodada de evolução pode agendar a rodada seguinte. No segundo, **experience-driven core evolution**, o agente realiza tarefas normais, encontra gargalos, bugs ou padrões ruins durante o uso e transforma parte dessas experiências em mudanças estruturais no próprio core.

Isso é claramente categoria **(2)** e também **(3)**. Não há evidência de atualização persistente dos pesos do modelo-base; a aprendizagem acontece no software que cerca o modelo. Também não é mera memória: o artefato executável muda e a execução futura herda a alteração.

A avaliação reportada é forte: **86,74% no Terminal-Bench 2.1**, **90,69% no OSWorld-Verified** e reward normalizado **0,2301 no CL-Bench** em campanha de cinco rollouts. O trabalho também descreve o experimento vivo **Hope**, com uma linhagem que continua evoluindo separadamente dos snapshots congelados usados nos benchmarks. Uma fonte pública do projeto relata que, nas primeiras 48 horas, o sistema executou 32 ciclos de evolução e avançou o repositório de v4.1 para v6.2.0.

A parte mais importante, porém, é o limite de autonomia: existe uma **fronteira de governança/revisão que o agente não controla**. Interações humanas podem revelar falhas e propostas, mas o agente decide quais mudanças perseguir; commits precisam atravessar revisão e controles externos antes de se tornarem runtime. Portanto isso se aproxima mais de **self-development recursivo governado** do que de RSI forte e irrestrito.

O padrão arquitetural reutilizável é:

```text
trabalho real
→ detectar atrito/falha
→ propor diff no próprio core
→ checks determinísticos
→ revisão independente
→ commit versionado
→ próxima execução usa o novo core
→ nova experiência
```

Uma decisão de arquitetura particularmente boa é separar a **linhagem experimental viva** da **linhagem congelada usada para avaliação**. Isso evita que o benchmark fique correndo atrás de um alvo que muda o tempo inteiro.

## 2. ModularRSI — primeiro decomponha o harness; depois evolua só a parte implicada

O **ModularRSI: Toward Generalizable Harness RSI** foi disponibilizado publicamente no início de setembro com código e um pool de **2.000 instâncias de evolução**. A ideia central é simples e muito compatível com os resultados que vimos em Ecdysis, HarnessEvo e RobustSGPO: **não trate o harness como uma única string ou um único bloco de código**.

O sistema o divide em cinco módulos: **Agent Loop, Observation Management, Tool Use, Context Management e Task Completion Detection**. Trajetórias de sucesso e de falha são comparadas para localizar a região provavelmente responsável. Em seguida, o Evolver modifica apenas o módulo relevante e a candidata passa por três gates: verificação do programa, revisão de diff contra overfitting específico da tarefa e execução/validação. Se falhar, ocorre rollback.

Os pesos do foundation model permanecem congelados. Portanto o caso é **(2) + (3)**. A evolução persiste no código/configuração modular do harness, não em contexto temporário.

Os resultados reportados mostram não apenas ganho no domínio de evolução, mas alguma transferência. No **Terminal-Bench 2.0**, a acurácia passou de **47,57 para 52,43 (+4,86 pontos)**. No **SWE-Bench Verified**, passou de **73,40 para 76,45 (+3,05)**. Mais importante: o harness evoluído em Terminal-Bench elevou SWE-Bench de **73,40 para 75,80**, e o harness evoluído em SWE elevou Terminal-Bench de **47,57 para 49,40**.

Também houve transferência entre modelos no Terminal-Bench: **GLM-5.2 59,55→61,80; MiniMax-2.5 41,57→44,94; DeepSeek-V4-Flash 47,57→52,43**. Isso é relevante porque HarnessDev mostra que muitos harnesses se coadaptam fortemente ao executor. ModularRSI sugere que mudanças menores, contrastivas e modularizadas podem generalizar melhor.

Outro achado útil foi a distribuição dos dados de evolução. Um conjunto com mistura de sucessos e falhas de dificuldade intermediária produziu melhora maior que extremos fáceis/difíceis. Isso reforça a ideia de que o agente aprende melhor quando consegue comparar **“quase funcionou” vs. “funcionou”**, em vez de olhar somente para erros totais.

O mecanismo reutilizável para agentes próprios fica:

```text
trace de sucesso + trace de falha
→ comparação contrastiva
→ atribuir falha a um módulo
→ mutar apenas esse módulo
→ program check
→ diff/overfitting check
→ replay/held-out
→ promover ou rollback
```

A principal limitação é que o algoritmo de evolução, os gates e a autoridade de promoção continuam definidos externamente por humanos. Portanto é **harness RSI limitado**, não RSI forte.

## 3. HarnessDev — evidência negativa importante: criar um harness é mais fácil do que fazê-lo evoluir de forma robusta

O **HarnessDev: Can LLMs Create and Evolve Their Own Agent Harness?**, submetido em 1º de setembro, é importante porque mede diretamente a habilidade de modelos criarem e evoluírem **sistemas executáveis completos**, não apenas prompts. O harness é formalizado em seis partes: loop de execução, política de tools, gerenciamento de contexto, state/memory, lifecycle e verification.

Foram avaliados seis modelos criadores, quatro domínios, cinco benchmarks e **2.207 instâncias downstream**. Na fase de criação, alguns harnesses gerados chegaram perto de sistemas humanos em tarefas específicas e superaram referências selecionadas em writing/ML experimentation, mas ainda ficaram muito atrás em coding/search/research. Um resultado reportado: o melhor criador teve média **67,8**, contra **86,2** da referência humana.

A parte realmente importante para self-improvement é a fase de evolução. Em nove linhagens houve **73 versões oficiais e 64 transições entre versões adjacentes**. Com o próprio runtime do modelo criador, todos os cinco criadores avaliados melhoraram o held-out, de **+1,43 a +4,44 pontos**, média **+3,11**. Porém, quando o executor foi fixado, os ganhos ficaram muito menos estáveis: apenas um dos criadores melhorou de forma clara e um caso regrediu **10,32 pontos**.

Há ainda uma estatística que deveria virar regra de projeto: **a melhoria observada no feedback de desenvolvimento e a melhoria no held-out caminharam na mesma direção em apenas 34 de 64 mudanças, 53,1%**. E apenas **2 das 9 versões finais escolhidas** eram realmente a melhor versão da linhagem no held-out.

Isso mostra que “última versão” ou “versão que parece melhor durante a evolução” não deve automaticamente virar produção. Um Evolver consegue produzir mudanças convincentes que só melhoram o conjunto visível ou que dependem demais do modelo executor usado durante a evolução.

Outro detalhe revelador: havia mecanismos de estado/controle presentes no código que praticamente **nunca eram ativados durante a execução real**. Em uma análise, vários artefatos declaravam estruturas de State, mas não apareciam eventos de checkpoint em dezenas de milhares de trajetórias. A lição é importante: não basta auditar se o harness contém uma feature; é preciso verificar se ela **realmente dispara nos traces de runtime**.

O padrão arquitetural que eu extrairia é:

```text
candidate harness
→ teste com executor original
→ teste com executores alternativos
→ held-out oculto
→ verificar ativação real dos mecanismos nos traces
→ comparar com versões anteriores da linhagem
→ promover somente se generalizar
```

HarnessDev é principalmente um benchmark de **(2) + (3)** e possui forte componente **(5)** porque humanos definem o protocolo, os datasets e o evaluator. Mesmo assim, ele fornece uma das melhores evidências recentes sobre onde a autoevolução de harness falha.

## 4. Infraestrutura prática: Claude Code Plugin Evals

A Anthropic publicou documentação oficial recente para **plugin evals no Claude Code**. Isso não é um agente autoaperfeiçoável por si só, portanto não deve ser confundido com os três casos acima. Mas fornece quase exatamente a infraestrutura que falta para colocar evolução de **skills/plugins/MCP** atrás de um gate confiável.

`claude plugin eval` pode executar prompts realistas com e sem o plugin, repetir cada condição várias vezes e medir o delta atribuível ao plugin. Há graders determinísticos (`regex`, `tool_used`, `tool_order`, `file_exists`) e graders baseados em modelo. Em CI é possível fixar modelo/judge, exigir threshold mínimo e limitar custo. A ferramenta também suporta mocks/replay para MCP, permitindo testar de forma repetível como uma skill ou plugin usa tools externas.

Para um sistema autoevolutivo, isso permite um pipeline bastante concreto:

```text
trace de produção
→ Evolver propõe nova skill/plugin/MCP policy
→ WITH candidate vs WITHOUT candidate
→ 3+ repetições
→ tool-order / file / deterministic graders
→ MCP mock/replay
→ threshold CI
→ promover ou rollback
```

A contribuição aqui é infraestrutura de avaliação e promoção, não autoaprendizado autônomo. Classificação principal: **(5)**, servindo como gate para sistemas de categoria **(3)**.

## Síntese da rodada

A principal novidade desta varredura é que o tema **“harness que se modifica”** já pode ser separado em três níveis de maturidade:

1. **Ouroboros:** o core realmente muda, commits aceitos viram o runtime seguinte e a evolução pode iniciar novas rodadas — é o caso mais próximo de self-development recursivo, mas ainda com supervisor/review externo.
2. **ModularRSI:** mostra um caminho mais controlado e generalizável — decompor o harness, atribuir a falha, mutar apenas o módulo necessário e provar transferência entre tarefas/modelos.
3. **HarnessDev:** mostra por que isso é difícil — cerca de metade das melhorias aparentes não se comporta da mesma maneira em held-out, e harnesses podem se coadaptar ao executor ou conter controles que nunca são ativados.

A arquitetura que emerge fica mais restritiva e, ao mesmo tempo, mais convincente:

```text
Trace
→ Cross-Instance Attribution
→ selecionar módulo mutável
→ Candidate Diff
→ deterministic checks
→ runtime-activation audit
→ held-out + cross-model replay
→ independent review
→ commit versionado
→ próxima execução usa a versão aprovada
```

Nesta rodada não apareceu um novo caso forte de **(1) atualização persistente de pesos em produção**. Também ainda não apareceu RSI forte no sentido de uma versão melhorada controlar totalmente o mecanismo que cria e promove sua própria sucessora. O que está surgindo é algo mais concreto: **recursive harness improvement com uma fronteira externa imutável de avaliação, governança e rollback**.

## Fontes públicas

- Ouroboros — arXiv: https://arxiv.org/abs/2608.08311
- Ouroboros — repositório público: https://github.com/HKUDS/Ouroboros
- ModularRSI — relatório: https://robotworld.top/en/articles/modularrsi-harness-rsi
- ModularRSI — código: https://github.com/IQuestLab/ModularRSI
- ModularRSI — dataset de 2.000 instâncias: https://huggingface.co/datasets/IQuestLab/ModularRSI_2000_Instances
- HarnessDev — arXiv: https://arxiv.org/abs/2609.01437
- Claude Code Plugin Evals — documentação oficial: https://code.claude.com/docs/en/plugin-evals
