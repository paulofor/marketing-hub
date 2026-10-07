# Radar Design de Experiência — 2026-10-07

## Síntese

A rodada de hoje converge para uma regra: **mais feedback, mais contexto e mais proatividade não são automaticamente melhores**. Os resultados mais úteis indicam que o valor depende de timing, capacidade de avaliação do usuário, reversibilidade e permissões explícitas.

## 1. JuicyVIS — timing pós-interação vale mais que simplesmente aumentar o efeito

**Descoberta.** *Juicy Interactive Visualization* (2026-10-06) operacionalizou feedback “juicy” por tipo de interação, timing e intensidade, usando 26 protótipos e três estudos online exploratórios.

**Evidência.** Após exclusões, os estudos tiveram 47, 46 e 26 participantes válidos. No Estudo 1, feedback rico elevou engajamento (UES-SF 3,647 vs. 3,224; p=6,871e-5) e experiência estética (BeauVis 5,155 vs. 4,284; p=1,809e-6). No Estudo 2, ao decompor o timing, apenas o feedback pós-interação mostrou efeito positivo significativo no engajamento (3,713 vs. 3,504; p=0,004411). No Estudo 3, aumentar a intensidade de nenhuma até alta não mudou de forma confiável engajamento, estética ou desempenho.

**Mecanismo.** Feedback depois da ação pode fechar o ciclo percepção–ação–consequência: confirma que o sistema registrou a intenção e torna a consequência perceptível. Os dados não sustentam “quanto mais efeito, melhor”.

**Implicação para produto.** No Marketing Hub, tratar microfeedback visual/sonoro como parte da ação: filtro aplicado, variante selecionada, edição concluída ou mudança de estado. Evitar decoração permanente.

**Experimento.** Comparar `feedback mínimo` vs. `feedback pós-ação breve` vs. `feedback pós-ação intenso`, mantendo a funcionalidade igual. Medir repetição de clique, undo, tempo para perceber a mudança, engajamento e distração.

**Riscos/limites.** Preprint exploratório, amostras modestas e domínio de visualização; não mede retenção, CTA, checkout ou venda.

Fonte: https://arxiv.org/abs/2610.08681

## 2. Calibração — explicação permanente não compensa falta de entendimento

**Descoberta.** *Novice Reliance Calibration in AI-Assisted Decision Making* (2026-10-06) estudou novatos usando IA quando não existe feedback imediato de correção.

**Evidência.** O estudo between-subjects teve 110 participantes e 26.460 pontos de decisão em extração de entidades clínicas. Nos braços com IA, a Reliance Calibration Score média foi 0,324; sobredependência foi muito maior que subdependência (0,31 vs. 0,04; p muito menor que 0,001). A condição com explicação — score de confiança + dicionário — não melhorou de forma confiável a calibração global (beta=-0,02; IC 95% [-0,05, 0,02]), e tempo de uso da explicação também não previu RCS menor. Maior entendimento autorrelatado esteve associado a mais rejeição/edição de sugestões erradas (beta=0,51; IC [0,09, 0,94]) e a mais correção de omissões (beta=0,47; IC [0,05, 0,89]).

**Mecanismo.** Explicação é informação; entendimento é a capacidade de julgá-la. Sem modelo mental suficiente, um score de confiança pode virar apenas mais um sinal persuasivo. Confiança subjetiva também não equivale a compreensão.

**Implicação para produto.** Para Têmis/Psique e supervisão humana, não usar confiança ou explicação padrão como selo de correção. Monitorar aceite, edição e reversão por tipo de tarefa e acionar evidência/reflexão quando houver sinais de sobredependência.

**Experimento.** Comparar `explicação sempre visível` vs. `explicação acionada por sinais de risco/miscalibration` + checagem curta de entendimento. Medir aceite de recomendações erradas, rejeição de corretas, edições e latência.

**Riscos/limites.** Domínio clínico, novatos, sessão curta e entendimento autorrelatado; sem evidência comercial. Este achado atualiza materialmente o card existente `monitoramento-metacognitivo-calibra-confianca-ai`.

Fonte: https://arxiv.org/abs/2610.07800

## 3. Jarvis — em agentes proativos, silêncio pode ser a decisão correta

**Descoberta.** *Jarvis: A Proactive Speech Agent for Multi-Party Conversations* acompanha conversas entre várias pessoas e intervém quando o grupo perde ou distorce um fato e não se corrige nos turnos seguintes.

**Evidência.** O sistema é ancorado em documento compartilhado, usa checks determinísticos e mostra a sentença-fonte. No CHI-180-proactive, ficou silencioso em 97% dos casos em que o próprio grupo resolveu o problema; estudo ao vivo com 23 participantes confirmou a direção geral.

**Mecanismo.** O valor da proatividade depende de respeitar uma janela de autocorreção. Intervir cedo demais rouba atenção; tarde demais perde utilidade.

**Implicação/experimento.** Modelar `detecção → esperar autocorreção → intervir se persistir`; comparar nudge imediato, janela curta e agente reativo. Medir interrupções desnecessárias, erros não corrigidos e recuperação.

**Riscos/limites.** Benchmark sintético e estudo ao vivo pequeno. **Não virou card**: é política de voz/harness, sem coleção válida.

Fonte: https://arxiv.org/abs/2610.07506

## 4. Living Dashboards — automatizar adaptação leve, escalar mudanças estruturais

**Descoberta.** *Living Dashboards* permite que views enfraqueçam, reapareçam ou sejam propostas a partir do uso e de consultas em linguagem natural; adicionar ou aposentar views permanece decisão do usuário.

**Evidência.** Em estudo exploratório between-subjects N=12 contra baseline com IA, participantes acertaram mais tarefas, relataram menor workload e maior usabilidade. Os autores observam que as condições diferiam em mais fatores além da adaptação.

**Mecanismo.** Adaptar elementos reversíveis pode reduzir custo de navegação; mudanças no espaço informacional compartilhado exigem agência explícita.

**Implicação/experimento.** Separar `apresentação reversível` de `estado canônico`. Testar dashboard fixo contra adaptação leve automática com confirmação para mudanças estruturais.

**Riscos/limites.** N=12 e causalidade fraca. **Não virou card** por falta de coleção para Adaptive UX/Generative UI.

Fonte: https://arxiv.org/abs/2610.08393

## 5. PAIR — sentir-se entendido não é igual a classificar a emoção corretamente

**Descoberta.** *PAIR* integra inferência afetiva multimodal, appraisal, memória entre sessões e coordenação de fala, cor e avatar.

**Evidência.** Em 14 dias com 19 participantes e 1.093 sessões, valência inicial teve MAE 1,20 na escala SAM de 9 pontos (r=0,68), dominância MAE 1,30 e arousal teve concordância fraca. Perceived understanding teve pouca correspondência com erro numérico de previsão; entrevistas atribuíram personalização/companhia à lembrança relevante, atualização de contexto e diálogo familiar.

**Mecanismo.** Qualidade relacional depende de continuidade e coerência contextual, não só de classificar o estado afetivo instantâneo.

**Implicação/experimento.** Separar `inferência afetiva`, `contexto/memória`, `perceived understanding` e `resultado`; comparar rótulo emocional isolado contra appraisal + memória + pistas coordenadas.

**Riscos/limites.** Apoio emocional, N=19 e alta sensibilidade ética. **Não virou card**: não é apropriado converter achado de suporte emocional em regra comercial.

Fonte: https://arxiv.org/abs/2610.07523

## 6. Agente compartilhado — contexto espacial melhora roteamento, mas aumenta risco de oversharing

**Descoberta.** *Who Is Talking to the Agent?* estudou agentes LLM em ambientes 3D multiusuário, nos quais o sistema decide se uma fala foi dirigida a ele e quais dados pessoais pode usar.

**Evidência.** O corpus LookAway tem 40 sessões, 80 personas e 1.200 turnos; três modelos em cinco condições geraram 18.000 decisões. Quando orientação corporal e destinatário eram congruentes, adicionar orientação elevou acurácia de 56% para 99,5%. Quando orientação e intenção conflitavam, dois modelos seguiram a orientação errada em mais de 85% dos turnos. Disponibilizar ambos os perfis melhorou respostas sobre a pessoa perguntada, mas elevou uso de atributos não revelados na conversa até 45,3% das respostas em um modelo.

**Mecanismo.** Sinais contextuais salientes podem dominar intenção; disponibilidade de informação pode ser confundida com permissão para usá-la.

**Implicação/experimento.** Separar `contexto disponível`, `destinatário inferido` e `dados autorizados neste turno`; testar orientação isolada contra orientação + intenção semântica + gate de visibilidade.

**Riscos/limites.** Ambiente sintético, 3D e modelos específicos. **Não virou card**: arquitetura multimodal/privacidade sem coleção aderente.

Fonte: https://arxiv.org/abs/2610.07732

## Experience Engine v37

`Sinal → contexto → política de intervenção → ação → feedback pós-ação → monitoramento de dependência → autorização/privacidade`

Princípio da rodada: **um sinal útil precisa aparecer no momento em que ajuda a perceber uma consequência, corrigir uma decisão ou recuperar controle; em muitos estados, permanecer silencioso é parte da boa experiência.**

## Cards derivados

1. `feedback-pos-interacao-engajamento-estetico` — coleção `prazer-audio-visual`. Novo card. A evidência humana liga timing pós-interação a engajamento/experiência estética; a aplicação no Hub permanece hipótese de produto.
2. `monitoramento-metacognitivo-calibra-confianca-ai` — coleção `neuromarketing`. Atualização material do card existente, preservando o `cardKey`; o novo estudo acrescenta evidência de que explicações uniformes não garantem calibração e que entendimento está mais ligado à correção de sugestões erradas.

Os achados de Jarvis, Living Dashboards, PAIR e Who Is Talking to the Agent? ficaram sem card por falta de coleção legitimamente aderente ou por contexto ainda estreito.

## Fontes

- https://arxiv.org/abs/2610.08681
- https://arxiv.org/abs/2610.07800
- https://arxiv.org/abs/2610.07506
- https://arxiv.org/abs/2610.08393
- https://arxiv.org/abs/2610.07523
- https://arxiv.org/abs/2610.07732
