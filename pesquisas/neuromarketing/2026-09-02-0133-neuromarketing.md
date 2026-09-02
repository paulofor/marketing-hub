# Neuromarketing e Desejos Digitais — 2026-09-02 01:33

## Resumo executivo

Nesta rodada, quatro novidades merecem entrar no radar do Marketing Hub. A mais prática é o lançamento do **iMotions Online**, que aproxima eye-tracking, expressão facial, respiração e predição por IA de testes remotos de criativos. As outras três reforçam direções que vêm emergindo no projeto: descoberta cada vez mais visual, restrições regulatórias a mecanismos de captura passiva de atenção e agentes comerciais que atuam dentro de limites explícitos definidos pelo usuário.

---

## 1. iMotions leva eye-tracking, expressão facial e respiração para testes remotos de criativos

**O que aconteceu**  
Em 1º de setembro de 2026, a iMotions lançou o **iMotions Online**, uma plataforma browser-based para testar anúncios, vídeos, embalagens e outros estímulos visuais. A solução combina eye-tracking por webcam, análise de expressão facial, monitoramento de respiração, surveys e predição por IA. Há três níveis de uso: predição rápida sem respondentes, testes com respondentes e estudos comportamentais customizados. A empresa diz que a base inclui mais de 100 mil vídeos publicitários testados e mais de 20 milhões de faces analisadas.

**Desejo/comportamento revelado**  
Não é um dado de desejo do consumidor em si, mas muda o custo e a velocidade para observar sinais não verbais de atenção, emoção e confusão em escala remota.

**Por que importa**  
Isso torna viável uma camada intermediária entre geração de criativos por IA e gasto real de mídia. Em vez de escolher variantes apenas por julgamento do modelo ou designer, o Marketing Hub pode combinar predição, comportamento remoto e resultado real da campanha.

**Aplicação no Marketing Hub**  
Criar uma estrutura `CreativeBehavioralTest` ligada a `creative_variant`, contendo, por exemplo:

- `predicted_attention`
- `observed_attention`
- `facial_valence`
- `expressiveness`
- `respiration_response`
- `self_reported_reaction`
- `observed_ctr`
- `observed_cpl`
- `observed_conversion`

O objetivo não seria assumir que biometria causa conversão, mas aprender quais sinais realmente antecipam performance em cada nicho e formato.

**Experimento concreto**  
`30 creative variants -> AI prescreen -> 6 variantes -> teste remoto comportamental -> 3 finalistas -> Meta Ads`.

Depois comparar ranking previsto, ranking comportamental e ranking comercial real.

**Impacto potencial**  
Muito alto. Pode reduzir desperdício de mídia e gerar dataset proprietário de relação entre sinais de atenção/emoção e performance real.

**Cuidado ético**  
Resultados de expressão facial e biometria devem ser usados preferencialmente de forma agregada para avaliação do criativo, não como inferência sensível individual para targeting ou preço.

**Fonte**  
https://imotions.com/about-us/news/imotions-online-launch-press-release/

---

## 2. Gen Z está usando vídeo como mecanismo de busca, não apenas entretenimento

**O que aconteceu**  
Em 2 de setembro, a EMARKETER destacou dados do YouGov: **48% dos usuários de busca online da Gen Z nos EUA procuraram informação em uma plataforma de vídeo nos 30 dias anteriores**, contra 34% do total de usuários de busca. O relatório original pesquisou 1.494 adultos norte-americanos entre 21 de abril e 29 de maio de 2026.

**Desejo/comportamento revelado**  
Parte dos usuários não quer apenas uma resposta textual. Eles preferem **ver a resposta acontecendo**, com demonstração, contexto humano e menor esforço para imaginar o resultado.

**Por que importa**  
Para Meta Ads, Reels e futuros produtos do Marketing Hub, vídeo não deve ser tratado somente como formato publicitário. Ele pode funcionar como **unidade de resposta a uma intenção de busca**.

**Aplicação no Marketing Hub**  
Adicionar um tipo de creative variant chamado `SEARCH_ANSWER_VIDEO`, em que cada vídeo é derivado de uma pergunta real do usuário, por exemplo:

- "como funciona?"
- "quanto tempo leva?"
- "vale a pena?"
- "qual a diferença entre A e B?"
- "o que recebo exatamente?"

Registrar `search_intent` e `question_answered` junto ao criativo.

**Experimento concreto**  
Comparar no mesmo público:

A. vídeo promocional tradicional;  
B. vídeo demonstrativo;  
C. vídeo que começa com uma pergunta real e responde visualmente em 15–30 segundos.

Medir thumb-stop, retenção, CTR, visita à landing e conversão.

**Impacto potencial**  
Alto, principalmente para produtos que exigem demonstração ou explicação rápida.

**Fonte**  
https://www.emarketer.com/content/gen-zers-searching-on-video-platforms--not-just-search-engines

---

## 3. Reguladores começam a atacar diretamente mecanismos de atenção passiva como autoplay

**O que aconteceu**  
Em 1º de setembro, a EMARKETER noticiou que legisladores da Califórnia aprovaram novas restrições para usuários menores de 16 anos, incluindo proibição de recursos considerados viciantes, como autoplay e feeds algorítmicos; o texto ainda depende da aprovação do governador. A análise observa que isso pode reduzir exposição passiva e obrigar marcas a ganhar o clique/play de maneira mais deliberada.

**Desejo/comportamento revelado**  
O mercado está se movendo, por pressão social e regulatória, de **captura automática de atenção** para **atenção consentida/intencional**.

**Por que importa**  
Esse movimento atinge diretamente social video e reforça que criativos devem funcionar mesmo quando o usuário precisa escolher assistir.

**Aplicação no Marketing Hub**  
Criar `ActiveAttentionReadiness` para vídeos, avaliando:

- força da thumbnail/capa;
- clareza da promessa antes do play;
- marca reconhecível sem autoplay;
- entendimento sem áudio;
- primeiro frame significativo;
- CTA compreensível sem depender de exposição prolongada.

**Experimento concreto**  
Para a mesma oferta, testar dois vídeos:

A. otimizado para autoplay/passive scroll;  
B. otimizado para escolha explícita, com thumbnail + promessa + curiosidade clara antes do play.

Mesmo dentro do Meta, isso pode ser simulado usando criativos com capas e hooks diferentes.

**Impacto potencial**  
Médio-alto no curto prazo e alto no longo prazo, sobretudo se restrições semelhantes se espalharem.

**Fonte**  
https://www.emarketer.com/content/autoplay-bans-could-force-brands-rethink-social-video

---

## 4. Índia prepara pagamentos por agentes com limites definidos pelo usuário — validação real do modelo de “permission envelope”

**O que aconteceu**  
Em 1º de setembro, a Reuters informou que a Índia prepara um framework para permitir que agentes de IA façam pequenos pagamentos via UPI sem aprovação individual a cada transação. Os primeiros casos devem envolver compras frequentes e de baixo valor. O desenho em discussão inclui delegação explícita, regras sobre quando e quanto pagar, limites de gasto, trilha de auditoria, checagem de identidade e um framework de responsabilidade.

**Desejo/comportamento revelado**  
O usuário pode aceitar **mais autonomia da IA quando consegue definir antecipadamente o perímetro de ação**. Isso é diferente de entregar controle irrestrito.

**Por que importa**  
É uma validação concreta da ideia de `AgentPermissionEnvelope` que já apareceu neste radar. Não é apenas uma hipótese de UX: uma infraestrutura nacional de pagamentos está caminhando exatamente para esse modelo.

**Aplicação no Marketing Hub**  
Transformar permissões de agentes em primeira classe do sistema:

- `can_recommend`
- `can_compare`
- `can_fill_form`
- `can_apply_discount`
- `can_schedule`
- `can_submit`
- `can_pay`
- `max_transaction_value`
- `max_period_spend`
- `requires_confirmation_above`
- `audit_log_required`

Mesmo antes de pagamentos, isso serve para Click-to-WhatsApp e agentes comerciais: o usuário pode permitir pesquisa e preparação automática, mas exigir confirmação antes de ações irreversíveis.

**Experimento concreto**  
Comparar dois agentes:

A. "Posso ajudar e executar etapas conforme avançamos";  
B. "Você escolhe o que posso fazer: pesquisar, comparar, preencher e preparar; nada é enviado ou pago sem sua confirmação".

Medir confiança, continuidade da conversa, autorização para automação e conversão.

**Impacto potencial**  
Muito alto para a arquitetura futura de agentes e commerce automation.

**Fonte**  
https://www.reuters.com/world/india/india-preparing-rollout-agentic-payments-upi-sources-say-2026-09-01/

---

## Prioridades sugeridas para o Marketing Hub

1. **`CreativeBehavioralTest`** — maior potencial imediato para conectar neuromarketing a performance real de Meta Ads.
2. **`SearchAnswerVideo`** — explorar vídeo como resposta a intenção, não apenas propaganda.
3. **`AgentPermissionEnvelope`** — transformar controle/autonomia em feature explícita da experiência com agentes.
4. **`ActiveAttentionReadiness`** — preparar criativos para um cenário de menor atenção passiva e maior exigência de escolha do usuário.

## Síntese da rodada

A direção comum desta rodada pode ser expressa como:

**prever atenção -> observar comportamento humano -> ganhar atenção intencional -> responder visualmente à intenção -> automatizar dentro de limites explícitos -> ação**.

O ponto mais importante para o Marketing Hub é que **IA preditiva e neuromarketing não deveriam substituir o experimento real**. O melhor desenho parece ser uma cascata: IA reduz o espaço de busca, sinais humanos ajudam a explicar resposta e Meta Ads/resultado comercial confirmam qual hipótese realmente funciona.