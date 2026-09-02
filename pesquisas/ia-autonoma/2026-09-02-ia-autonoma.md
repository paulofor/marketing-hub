# Radar — IA Autoaprendente — 2026-09-02

## 18:00 — atualização relevante

Há **três desenvolvimentos novos que passam o filtro nesta rodada**. O principal avanço arquitetural é o **HarnessEvolve**, porque o objeto de otimização deixa de ser apenas uma skill ou prompt e passa a ser **o harness inteiro** — prompts, skills, ferramentas e lógica de execução — com diagnóstico da primeira divergência e gates independentes antes de persistir uma mudança. O principal alerta de produção é a colaboração **Coval + Phonely/Alma**, porque conecta avaliação de conversas reais de produção ao retreinamento do modelo; porém, publicamente ainda não está demonstrado que geração de variantes, retraining e promoção sejam totalmente automáticos. O terceiro avanço, **Defense-as-Skill**, mostra que a camada de segurança também pode evoluir persistentemente a partir dos rollouts.

| Caso | (1) Pesos | (2) Código/scaffold/harness | (3) Prompt/skills/tools/estratégia | (4) Só memória | (5) Forte estrutura humana |
|---|---:|---:|---:|---:|---:|
| HarnessEvolve | Não no loop | **Sim** | **Sim** | Não | Parcial |
| Coval + Phonely/Alma | **Sim, retraining** | Não demonstrado | Possível, mas não detalhado | Não | **Sim / não esclarecido quanto à autonomia** |
| Defense-as-Skill / SkillSonar | Não | Não no runtime-base | **Sim — política de segurança persistente** | Não | Parcial |

## 1. HarnessEvolve — evolução do harness inteiro com diagnóstico causal por trajetória

O **HarnessEvolve**, submetido em 1º de setembro de 2026 por pesquisadores da Huawei Technologies Shanghai, trabalha com um LLM de execução congelado e separa quatro responsabilidades: execução, avaliação, otimização e gate. O agente executa a tarefa; uma trajetória de referência é produzida com acesso à resposta correta; o sistema alinha a execução que falhou com essa referência e procura **o primeiro ponto de divergência**, tentando localizar a ação causal em vez de refletir genericamente sobre toda a trajetória.

Depois, erros semelhantes são agrupados para identificar padrões sistemáticos. O optimizer pode produzir alterações que atravessam o harness inteiro: `skill.md`, scripts Python dentro das skills, instruções de prompt, especificações de argumentos das tools e código da própria lógica de execução. Portanto este caso entra claramente em **(2) mudança persistente de código/scaffold/harness + (3) evolução persistente de prompts, skills e tools**. Os pesos do modelo de execução não são atualizados durante esse loop.

Uma candidata só persiste depois de passar por dois gates. O **quality gate** rejeita vazamento de dados e crescimento artificial do prompt; o **performance gate** exige melhoria no batch atual sem degradar batches recentes além da tolerância. Ao fim de cada época, snapshots aceitos são comparados em um validation set separado, e somente o melhor continua.

Os resultados são fortes. No CloudCoreNetwork-QA com Qwen3.6-27B, o baseline foi de **43,4%** e o HarnessEvolve chegou a **86,9%**. GEPA ficou em 65,3%, ACE em 59,3% e SkillOpt em 61,9%. Com DeepSeek-V4-Flash, o mesmo benchmark passou de **47,5% para 85,9%**. Em Wireless-QA, os resultados finais foram **89,7% com Qwen** e **92,8% com DeepSeek**.

A ablação é ainda mais importante: retirar a trajetória de referência derrubou CloudCoreNetwork-QA de **86,9% para 57,8%**; retirar o agrupamento de erros caiu para 68,6%; retirar o quality gate, para 80,1%. Isso sugere que a peça mais valiosa não é simplesmente “deixar o LLM editar o harness”, mas fazer **credit assignment explícito até a primeira divergência causal**.

A intervenção humana continua significativa na infraestrutura: pesquisadores definem datasets, splits, ground truth, thresholds, arquitetura dos gates e ferramentas disponíveis. Um dos backbones, Qwen3.6-27B, tinha fine-tuning de domínio prévio; o experimento também usa DeepSeek-V4-Flash sem fine-tuning, mostrando que o ganho do loop não depende de atualizar os pesos durante a evolução.

### Padrão arquitetural reutilizável

`execução → trajetória falha → trajetória de referência → primeira divergência → cluster de causas → patch pequeno no componente culpado → quality gate → performance/regression gate → snapshot → validação held-out → persistir`

Para agentes com MCP, isso é especialmente útil: um erro pode ser atribuído não ao “agente inteiro”, mas a uma camada específica, como descrição da tool, schema de argumentos, transformação de parâmetros, ordem de chamadas, retry, checker ou prompt da skill. O Evolver então modifica apenas esse ponto.

Fonte: [HarnessEvolve no arXiv](https://arxiv.org/abs/2609.00829)

## 2. Coval + Phonely/Alma — produção real alimentando retreinamento

Em 1º de setembro, a Coval e a Phonely anunciaram um loop de melhoria para agentes de voz. A Coval avalia **conversas reais de produção**, identifica onde o agente funciona bem e onde falha, e os achados são enviados de volta ao **Alma**, modelo de voz da Phonely, para **retraining**. O Alma foi treinado em mais de 10 milhões de conversas telefônicas e já opera em produção em grande volume.

Aqui a classificação principal é **(1) mudança persistente de pesos**, com componente forte de **(5)**. Diferentemente dos sistemas que mantêm o modelo congelado e evoluem skills, o anúncio descreve explicitamente que o feedback da produção volta ao modelo para retreiná-lo. Porém a documentação pública **não diz** que cada nova versão seja gerada, treinada, avaliada e promovida sem intervenção humana, nem descreve rollback ou A/B automático. Portanto eu não chamaria isso de recursive self-improvement autônomo.

Também é importante separar as métricas do modelo das métricas do novo loop. Em um benchmark da própria Phonely com 200 turnos reais held-out, Alma marcou **0,779** no score geral, contra 0,752 do GPT-5.6 e 0,714 do GPT-4.1; o score de etiqueta de conversa foi **0,867**, contra 0,770 e 0,700; TTFT mediano foi **182 ms**, contra 997 ms no GPT-5.6 e 490 ms no GPT-4.1. Alma foi fine-tuned em cerca de **3.400 turnos revisados por humanos**. Esses números mostram a qualidade do modelo especializado, mas **não isolam causalmente o ganho produzido pela nova integração Coval → retraining**.

Há ainda uma nuance de privacidade: a página do Alma oferece implantação self-hosted com a garantia de que chamadas do cliente não treinam o modelo. Portanto “aprende com conversas reais” não deve ser interpretado como “toda chamada de todo cliente entra automaticamente no treinamento”.

### Padrão arquitetural reutilizável

`produção real → evaluator contínuo → identificar gaps por tipo → construir dataset/feedback → retraining → avaliação separada → nova versão`

O próximo marco que precisamos procurar é evidência de que a parte final também fecha sozinha: `variante → validação → canary/A-B → promoção/rollback automático`.

Fontes: [Coval + Phonely — DestinationCRM](https://www.destinationcrm.com/Articles/CRM-News/CRM-Across-the-Wire/Coval-Partners-with-Phonely-for-Self-Improving-Agents-176413.aspx), [benchmark oficial do Alma](https://www.phonely.ai/blogs/alma-benchmark), [página oficial do Alma](https://www.phonely.ai/alma)

## 3. Defense-as-Skill — a camada de segurança também aprende persistentemente

O paper **Defense-as-Skill**, submetido em 1º de setembro, parte de um problema que apareceu no radar nos últimos dias: se agents podem instalar skills persistentes, uma skill ruim ou maliciosa também pode criar um comportamento durável. A proposta é transformar o próprio guard de segurança em uma **skill persistente, inspecionável e editável**, chamada `SkillSonar`.

Antes de uma ação sensível, o SkillSonar compara a ação com o limite da tarefa do usuário e decide entre **allow, replan ou confirmação**. A novidade para nosso radar é que essa política não fica estática: uma busca MCTS evolui **o arquivo da skill no disco** usando feedback dos rollouts, medindo sucesso de ataques, utilidade nas tarefas, carga de confirmações e custo de contexto. Os pesos do modelo executor permanecem congelados. Isso entra em **(3) evolução persistente de política/skill**.

No GLM-5, em execuções repetidas, a taxa de sucesso de ataque (ASR) caiu de **0,482 para 0,104 em ID** e de **0,606 para 0,115 em OOD**. A transferência também apareceu em outros modelos: com GPT-5.4, o no-guard ficou em 0,588/0,559 e o SkillSonar em **0,019/0,034** para ID/OOD. O trabalho também mostra que simplesmente colocar o mesmo conteúdo em um prompt não é equivalente: a representação modular como skill reduziu ASR ID/OOD de **0,353/0,437 para 0,104/0,109**, aumentou task utility e reduziu tokens de ~239K para ~188K, cerca de **21%**.

Outro resultado arquitetural importante: deixar o guard entrar apenas via seleção comum de skills foi muito pior; a atribuição explícita da responsabilidade de segurança reduziu ASR de **0,400/0,582 para 0,104/0,109**. Segurança, portanto, não deve depender de o agente “lembrar” de invocar a skill correta.

### Padrão arquitetural reutilizável

`ação proposta → guard skill obrigatório → allow/replan/confirm → resultado do rollout → evaluator de safety + utility → Evolver do guard → nova versão da política → regressão/OOD → persistir`

A consequência para agentes autoaperfeiçoáveis é importante: **não basta evoluir a capacidade; a política de segurança precisa acompanhar a evolução**, mas com responsabilidade explícita e verificação externa.

Fonte: [Defense-as-Skill no arXiv](https://arxiv.org/abs/2609.01487)

## Síntese da rodada

O avanço técnico mais importante é **HarnessEvolve**. Ele reforça um padrão que está ficando cada vez mais claro: o componente decisivo não é a “reflexão” do LLM, e sim **attribution + modificação localizada + gates independentes**. A arquitetura que eu consideraria hoje para agentes próprios é:

`Agent → trace → reference/verifier → first-divergence attribution → Evolver → patch pequeno em harness/skill/tool → quality/safety gate → regression/performance gate → versionamento → promoção`

O alerta de produção mais importante é **Coval + Phonely**: há agora um caso explícito de **conversas reais de produção → avaliação contínua → feedback para retreinamento dos pesos**. Mas ainda falta evidência pública de que esse ciclo tenha promoção totalmente automática e de quanto ele melhora causalmente o modelo a cada rodada.

O **Defense-as-Skill** acrescenta uma segunda linhagem evolutiva que eu passaria a tratar como obrigatória: `capability evolver` e `safety evolver`, ambos avaliados por verificadores que eles próprios não controlam.

Não apareceu nesta rodada evidência convincente de **recursive self-improvement aberto**, no qual a versão melhorada assuma autonomamente o processo que cria a próxima geração e repita o ciclo sem um limite externo. Também não encontrei um novo caso comercial que prove publicamente o ciclo completo `produção → variantes → A/B → seleção → promoção automática → rollback` sem gate humano.