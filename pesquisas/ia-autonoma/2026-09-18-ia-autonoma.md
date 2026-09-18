# Radar IA Autônoma — 2026-09-18

**Rodada:** 17:36 (America/Sao_Paulo)

## Resumo executivo

Nesta rodada há quatro novidades relevantes que passam o filtro. Três são novos trabalhos de 17 de setembro que apareceram depois da rodada anterior e atacam exatamente as camadas que vinham emergindo no radar: **skills persistentes com lifecycle**, **índices/retrieval que evoluem sozinhos** e **código do harness descoberto por auto-research**. A quarta é um anúncio oficial da Anthropic que não é self-improvement por si só, mas mede de forma inédita quanto da construção de modelos sucessores já está sendo conduzida por agentes em produção interna.

O principal avanço arquitetural é que o self-improvement está ficando mais parecido com **manutenção de software governada**: mudanças localizadas, registro de versões, testes protegidos, negative controls, held-out realmente isolado e promoção seletiva. Isso é muito mais próximo de um sistema operacional confiável do que de “refletir e reescrever o prompt”.

| Caso | (1) Pesos persistentes | (2) Código/scaffold/harness | (3) Prompts/retrieval/workflows/tools/skills/estratégia | (4) Só memória/contexto | (5) Otimização essencialmente humana |
|---|---:|---:|---:|---:|---:|
| FinSkillOps | Não | Não como objeto principal | **Sim — skills versionadas** | Não | Parcial |
| SELF-INDEX | Não | Não | **Sim — índice/retrieval** | Não | Parcial |
| SoL-Pi | Não | **Sim — principal** | **Sim — mecanismos de execução/contexto** | Não | Parcial |
| Anthropic R&D Automation Index | Indiretamente, em modelos sucessores | Não demonstrado como autoevolução | Não é o objeto principal | Não | **Sim — ainda há supervisão humana** |

## 1. FinSkillOps — self-improvement como manutenção comportamental versionada

O paper **FINSKILLOPS: A Self-Evolving Multi-Agent System for SEC Filing QA**, submetido em 17 de setembro, trata falhas recorrentes em QA financeiro como patches comportamentais de escopo limitado. O sistema diagnostica erros por tipo — período, entidade, evidência, cálculo etc. — e transforma falhas recorrentes em **skills de linguagem natural com condições de aplicabilidade, guards contra falso acionamento, versão e status**.

O que melhora sozinho é o conjunto de skills que orienta decomposição de consulta e síntese final. O LLM base não é retreinado. Cada candidata passa por três tipos de evidência antes de entrar no registry ativo: validação direcionada no modo de falha que motivou a mudança, uma protected suite composta por casos antes corretos e casos de alto risco, e negative controls para impedir que a skill seja aplicada fora do seu escopo. Quando uma versão é promovida, a anterior é supersedida e fica inativa; o registry guarda a origem, validação e resultado dos testes de cada versão.

Esse desenho é muito próximo do tipo de lifecycle que queremos em agentes persistentes: `falha → diagnóstico tipado → candidate skill → targeted validation → protected regression → negative controls → promote/version/retire`.

As métricas são relevantes. No benchmark interno reforçado, retirar a evolução de skills reduz o W-Corr. de **4,55 para 3,70**. Na evolução de 12 rodadas, o non-correct rate caiu de **30,0% para 18,3%** no conjunto de evolução e de **20,0% para 12,5%** no conjunto de monitoramento. Mais importante: somente **6 de 33** skills propostas foram promovidas; cinco candidatas falharam checks do protected set, expondo sete regressões antes de deployment. O registry implantado terminou sem regressões detectadas no conjunto protegido.

A intervenção humana continua importante no desenho do sistema, taxonomy, thresholds e conjuntos de avaliação. O loop de proposta e revisão é automatizado, mas não há evidência de o agente redefinir autonomamente seus próprios critérios de promoção. Portanto isso é categoria **(3)**, não RSI forte.

O padrão reutilizável mais importante é **escopo explícito + negative controls + lifecycle**. Em vez de uma skill global como “sempre faça X”, a candidata deveria declarar `applies_when`, `do_not_apply_when`, origem, parent version, validações e status. Isso reduz o risco de uma correção local degradar tarefas não relacionadas.

Fonte: https://arxiv.org/abs/2609.19680

## 2. SELF-INDEX — o retrieval/RAG começa a se auto-otimizar

O paper **Self-Evolving Search Index**, também submetido em 17 de setembro, é especialmente importante porque move o self-improvement para uma camada que muitas arquiteturas tratam como estática: o próprio índice de retrieval.

O SELF-INDEX mantém o corpus original inalterado, mas evolui persistentemente as **index keys** usadas para representar documentos e memórias. O Optimizer executa três passos: **Self-Diagnosis**, que identifica deficiências a partir dos resultados de retrieval; **Self-Revision**, que altera seletivamente apenas os key sets responsáveis; e **Self-Validation**, que aceita somente revisões que preservam faithfulness, specificity e separation. A versão validada do índice vira o ponto de partida da iteração seguinte.

Além disso, um **Query Simulator** gera demandas de retrieval ainda não observadas. Assim o sistema não fica apenas reativo às consultas recebidas; ele explora demandas plausíveis e usa essas consultas simuladas para dirigir novas revisões. Isso é categoria **(3)**: evolução persistente de retrieval/RAG, com pesos do agente e corpus congelados.

A parte mais relevante para agentes com memória aparece no LongMemEval-V2. Sem alterar o conteúdo armazenado da memória, apenas melhorando suas chaves de retrieval, o SELF-INDEX elevou o score geral de **0,415 para 0,472 (+13,9%)** em Query→Slice; de **0,448 para 0,503 (+12,4%)** em Query→Slice+Notes; e de **0,532 para 0,581 (+9,2%)** no AgentRunbook-R. O efeito aparece especialmente nas dimensões static, dynamic e workflow, mostrando que uma memória pode continuar contendo exatamente as mesmas experiências e mesmo assim ficar muito mais útil se o índice aprender a expô-las melhor.

Nas search agents do BrowseComp-Plus, o índice evoluído aumentou answer accuracy e evidence recall em todos os backbones/retrievers avaliados e reduziu consistentemente o número de search calls; a análise de custo mostra melhor accuracy com custo online menor. Quando o corpus foi ampliado de 100K para 400K documentos, o SELF-INDEX manteve a accuracy praticamente estável e reduziu levemente o custo por query, enquanto métodos concorrentes ficaram mais sensíveis ao crescimento do corpus.

Uma ablation é particularmente importante: quando o sistema remove Self-Validation e aceita todas as revisões, o nDCG cai abaixo do índice base em todos os tipos de corpus avaliados. Portanto aqui reaparece a regra central do radar: **evolução sem gate não é melhoria confiável**.

O mecanismo reutilizável é direto para RAG/MCP/memória: `retrieval miss → diagnosticar chave/representação responsável → gerar chave candidata → validar faithfulness/especificidade/separação → promover só o key set afetado`. Isso permite ao retrieval aprender sem reescrever documentos e sem retreinar o LLM.

Fonte: https://arxiv.org/abs/2609.19656

## 3. SoL-Pi — auto-research descobre código de harness transferível

**SoL-Pi: Recursively Scaling Auto-Research Loops for Efficient Agent Harness**, de NVIDIA, NTU e MIT, é o caso mais forte da rodada em categoria **(2)**. Um research agent observa traces de um agente executando o harness Pi, propõe modificações de implementação, executa experimentos e submete candidatas a capability gates e efficiency gates. O resultado final não é um prompt melhor, mas **quatro mecanismos concretos de código no harness**.

A busca partiu de **152 direções** distribuídas por contexto, progress, tools, delegation, prompt/policy e improvement/evaluation, passou por **535 ambientes executáveis**, mais de **3.000 runs** e mais de **60.000 interações agent–environment**. Quatro mecanismos sobreviveram à seleção: **Action Fusion**, que combina uma mutação de arquivo com seu comando subsequente; **Online Context Compact**, que compacta o contexto apenas quando a economia projetada supera o custo da reescrita; **ObservationPack**, que arquiva outputs grandes e passa a enviar handles + excerpts; e **Evidence-Preserving Reducer**, que comprime logs com um modelo barato e só aceita o resumo quando verificações determinísticas de schema, hash, exit status e citações passam.

Em EdgeBench, a configuração de eficiência reduziu token traffic em **49,0%** contra Pi usando GPT-5.6 Sol, mantendo **93,7%** do score médio (42,0 vs. 44,8) e reduzindo token cost em **33,2%**. A configuração orientada a performance elevou o score médio de **44,8 para 47,2 (+5,3%)**, ao mesmo tempo reduzindo token traffic em **6,1%**. Aplicada a Opus 5 sem nova busca/adaptação, a configuração de eficiência reduziu token traffic em **44,7%** e custo em **33,5%**, mantendo 94,3% do score de Pi — uma evidência preliminar de transferência entre modelos.

O detalhe arquitetural mais importante é a separação entre search feedback e held-out. Antes da busca, métricas, tolerâncias e critérios são congelados e mantidos fora do controle do optimizer. O EdgeBench só entra depois que a candidata está congelada; falhar no held-out rejeita a candidata, mas o resultado do held-out não volta ao loop para permitir que o sistema “conserte” especificamente esse teste.

Isso é uma resposta direta ao problema visto em HarnessDev/CHASE: `held-out usado como feedback deixa de ser realmente held-out`.

A intervenção humana continua grande na criação dos ambientes, escolha das métricas, tolerâncias e arquitetura do processo. Ainda assim, a descoberta e implementação das candidatas de harness são AI-led. Não há mudança persistente de pesos e não há demonstração de que o mecanismo de pesquisa que produz melhores harnesses também se tornou melhor. Portanto não é RSI forte.

O padrão reutilizável é **broad-to-deep harness research com lineages isoladas e disposable**: muitas hipóteses independentes, refinamento apenas das promissoras, reviewer separado, gates de capacidade/eficiência e held-out unidirecional.

Fonte: https://arxiv.org/abs/2609.20519

## 4. Anthropic — 26% do R&D de modelos já é liderado por Claude, mas isso ainda não é RSI

Em 17 de setembro, a Anthropic publicou o **Anthropic R&D Automation Index**, uma tentativa de medir diretamente quanto do processo de construir modelos sucessores já é realizado por IA. O índice usa a escala de Automation Levels da Epoch AI, de AL0 a AL5. AL4 significa que a IA consegue completar a maior parte de uma tarefa end-to-end a partir de um high-level prompt, com supervisão humana; AL5 significaria operação totalmente autônoma sem humano no loop.

Segundo a Anthropic, em agosto de 2026 Claude **liderava 26%** do trabalho interno de AI R&D e mais de **90%** estava em AL3 ou superior, isto é, pelo menos em modo colaborativo. A empresa é igualmente explícita sobre a limitação: Claude **não operava em AL5 em nenhum subset medido**. Portanto isso deve entrar na categoria **(5)**, e não ser anunciado como recursive self-improvement já alcançado.

O dado operacional também é importante: a Anthropic reporta cerca de **30.000 agentes simultâneos** fazendo pesquisa e engenharia em sua plataforma interna mais usada. Em agosto, mais de um bilhão de decisões desses agentes passaram por monitor online; 0,002% foram bloqueadas. Há também monitoramento offline de todas as ações, com cerca de 100 mil transcripts por semana passando por classifiers e aproximadamente 50 flags de maior prioridade chegando a revisão humana.

Isso não mostra um agente aprendendo persistentemente por experiência, mas mostra que **AI-improving-AI já existe em escala operacional como processo de P&D humano-supervisionado**. O salto que falta para RSI forte continua sendo a redução da autoridade humana sobre escolha de objetivos, desenho dos experimentos, promoção dos sucessores e avaliação.

Fonte oficial: https://www.anthropic.com/institute/measuring-pace-of-ai-development

## Padrão arquitetural extraído desta rodada

Os três papers novos convergem para uma arquitetura de evolução controlada:

```text
live trace / retrieval miss / failed task
        ↓
root-cause attribution
        ↓
small scoped candidate
(skill | index keys | harness mechanism)
        ↓
targeted validation
        ↓
protected regression suite
        ↓
negative controls / integrity checks
        ↓
held-out one-way evaluation
        ↓
DISCARD | PARK | PROMOTE | SUPERSEDE | RETIRE
        ↓
versioned persistent state
```

Isso reforça uma separação útil para sistemas próprios: **core imutável de governança** versus **control plane evolutivo**. Skills, políticas, routing e index keys podem ficar em registry/DB/estado externo versionado e serem ativados sem rebuild; código de harness que demonstrou valor pode virar uma versão de runtime mais estável. O ponto comum não é o formato de armazenamento, e sim preservar lineage, gates, rollback e capacidade de auditar exatamente qual mudança produziu cada resultado.

## Estado do RSI forte

Ainda não apareceu nesta rodada uma demonstração convincente de RSI forte. SoL-Pi melhora o harness, mas o processo/meta-optimizer que descobre as mudanças continua fixo. FinSkillOps melhora skills, mas os gates e a lógica de lifecycle continuam definidos externamente. SELF-INDEX evolui o retrieval, mas não evolui o próprio Optimizer. A Anthropic mostra IA construindo sucessores em escala real, mas ainda com supervisão humana e zero AL5 reportado.

O que está ficando cada vez mais claro é que a trilha prática para sistemas autoaperfeiçoáveis não parece começar por “reescrever os próprios pesos”. Ela está convergindo para **componentes externos versionados + traces + diagnóstico + avaliação independente + promoção controlada**, com consolidação em pesos como uma etapa separada e mais lenta quando houver evidência suficiente.

## Nota de triagem

Também apareceu **Self Improvement via Fast Tree-search (SIFT)** no feed do arXiv de 17/18 de setembro. Ele é relevante tecnicamente — usa LLM-as-a-judge + Bradley-Terry + tree search para filtrar patches de self-modification antes de gastar benchmark completo — mas não foi contado como avanço realmente novo nesta rodada porque há registro do trabalho como paper já publicado/aceito anteriormente em ICLR 2026. Mantive o radar focado no que efetivamente acrescentou evidência nova nesta data.

Fonte do SIFT: https://arxiv.org/abs/2609.19526
