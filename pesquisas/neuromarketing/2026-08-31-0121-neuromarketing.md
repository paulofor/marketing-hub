# Radar de Neuromarketing e Desejos Digitais — 31/08/2026 01:21

Nesta rodada entraram quatro achados que ainda não haviam aparecido no radar. O padrão comum é uma evolução importante para o Marketing Hub: sair de segmentações estáticas e inferências frágeis sobre o usuário e avançar para **sequências comportamentais, sinais verificáveis, voz como interface de confiança e evidência distribuída fora dos canais próprios**.

## 1. Consumer LBM: prever o próximo comportamento, não apenas classificar o usuário

**O que aconteceu**  
A IGAWorks anunciou em 28/08/2026 o **WorksFM**, um foundation model voltado a comportamento do consumidor. A empresa diz que o modelo foi construído sobre dados reais de comportamento cobrindo cerca de **34 milhões de pessoas** e aprende sequências envolvendo uso de apps, pagamentos, visitas offline, respostas a anúncios e consumo de TV. A proposta é inferir o estado atual e a probabilidade do próximo comportamento, em vez de apenas classificar consumidores em segmentos.

**Desejo/comportamento revelado**  
O principal insight não é um desejo declarado do consumidor, mas uma mudança na forma de modelá-lo: intenção parece ser melhor representada como **estado transitório derivado de uma sequência de ações**, e não como uma persona fixa.

**Por que importa**  
Para marketing experimental, isso muda a pergunta de “que tipo de pessoa é esta?” para “em que estado de decisão ela está agora e qual comportamento provavelmente vem a seguir?”. Isso combina com funis, Meta Ads e Click-to-WhatsApp, onde recência, ordem de eventos e mudança de comportamento podem ser mais informativas que um segmento estático.

**Aplicação no Marketing Hub**  
Criar um `BehaviorSequence` + `IntentStateModel`. Eventos como impressão, clique, visita, retorno, abertura do WhatsApp, resposta, abandono, compra e recompra formariam uma sequência temporal. O agente passaria a estimar estados como `DISCOVERY`, `EVALUATION`, `HIGH_INTENT`, `HESITATION`, `DORMANT` e `RETURNING`.

**Experimento/feature**  
Comparar uma política de próxima ação baseada apenas em segmento/persona com outra baseada nos últimos N eventos do usuário. Exemplo: usuários que clicaram, voltaram em menos de 24h e abriram o WhatsApp recebem prova/clareza; usuários que visitaram uma vez recebem educação/descoberta. Medir CPL, avanço no funil, conversão e tempo até conversão.

**Impacto potencial**: **muito alto**. Pode virar uma camada central de decisão dos agentes do Marketing Hub.  
**Limite/ética**: não replicar o modelo com rastreamento cross-domain invasivo. Para o Hub, priorizar eventos first-party, dados consentidos e minimização de dados.

**Fontes**  
- https://www.digitaltoday.co.kr/en/view/97721/iga-works-unveils-worksfm-to-build-consumer-behaviour-ai-foundation-model  
- https://www.igaworks.com/

---

## 2. Voz pode funcionar como um “atalho de confiança” — e o funil deixa de ser linear

**O que aconteceu**  
Uma entrada de referência da Springer publicada online em **30/08/2026**, *Voice Commerce and Conversational Marketing*, propõe o conceito de **Voice-Trust Nexus**. A síntese reúne teoria de riqueza de mídia e transferência de confiança e argumenta que sinais auditivos, como prosódia, podem funcionar como substitutos de parte dos sinais visuais de confiança. O trabalho também propõe pensar a jornada como um **diálogo recursivo**, não apenas como um funil linear.

**Desejo/comportamento revelado**  
Em interfaces conversacionais, o usuário não avalia apenas o conteúdo semântico. Ritmo, entonação, naturalidade e adequação sociolinguística também participam da percepção de competência, proximidade e confiança.

**Por que importa**  
Para agentes de IA do Marketing Hub, especialmente em experiências futuras com voz ou mensagens de áudio no WhatsApp, “qual texto dizer” é só metade da interface. “Como dizer” pode virar variável experimental.

**Aplicação no Marketing Hub**  
Adicionar `VoiceStyle`/`ProsodyProfile` às variantes conversacionais: `CALM_EXPERT`, `WARM_ASSISTANT`, `FAST_UTILITY`, `NEUTRAL`. Registrar também idioma, sotaque/variante regional e velocidade de fala sem inferir atributos sensíveis do usuário.

**Experimento/feature**  
Em um funil Click-to-WhatsApp com áudio opcional, comparar a mesma mensagem em texto, voz neutra e voz calorosa/consultiva. Medir taxa de resposta, continuidade da conversa, CTA e percepção de confiança em pesquisa curta pós-interação.

**Impacto potencial**: **alto**, especialmente para produtos que exigem explicação ou confiança.  
**Limite**: trata-se de uma síntese teórica/reference work, não de um novo ensaio clínico ou experimento causal. Usar como hipótese de design a validar em A/B test.

**Fonte**  
- https://link.springer.com/rwe/10.1007/978-3-031-75316-9_139-1

---

## 3. Benchmark de Emotion AI mostra que “ler emoção pelo rosto” ainda é frágil demais para decisão individual

**O que aconteceu**  
Um benchmark atualizado em **27/08/2026** testou 12 modelos multimodais/vision-capable em 70 fotos faciais rotuladas, repetindo cada imagem cinco vezes. O melhor resultado foi **67% de acerto**, sem diferenças estatisticamente significativas entre os modelos após correção múltipla. O contraste por emoção foi enorme: felicidade foi reconhecida em 89% das respostas, medo em 38% e raiva em apenas 22%. Os modelos também mostraram inconsistência entre repetições.

**Desejo/comportamento revelado**  
O achado é menos sobre um desejo e mais sobre um alerta metodológico: expressões faciais não devem ser tratadas como uma leitura objetiva e precisa do estado interno da pessoa. Sistemas podem transformar ambiguidade em uma falsa sensação de certeza.

**Por que importa**  
O Marketing Hub monitora neuromarketing, emoção e biometria. Este benchmark sugere que usar um LLM multimodal para dizer “este usuário está irritado/ansioso/interessado” e adaptar oferta ou preço em tempo real seria tecnicamente frágil e eticamente problemático.

**Aplicação no Marketing Hub**  
Se algum dia houver facial coding, usar `EmotionSignalConfidence` e trabalhar **agregado por criativo/amostra**, nunca como verdade individual. Preferir métricas observáveis — atenção, clique, abandono, tempo, resposta e conversão — e, quando emoção for relevante, combinar métodos e consentimento explícito.

**Experimento/feature**  
Validar um `CreativeEmotionScore` apenas como sinal auxiliar: comparar a previsão do modelo com autorrelato voluntário e performance real do criativo. Só manter dimensões que mostrem estabilidade e associação replicável com comportamento.

**Impacto potencial**: **alto como guardrail**; evita construir personalização em cima de um sinal sedutor, porém instável.  
**Limite**: benchmark pequeno, com 70 imagens e labels que os próprios autores reconhecem poder conter ambiguidade. Não é estudo peer-reviewed.

**Fonte**  
- https://aimultiple.com/emotion-ai-tools

---

## 4. McKinsey: LLMs quase não citam o site da própria marca — confiança passa a ser distribuída

**O que aconteceu**  
Em pesquisa publicada em **27/08/2026**, a McKinsey relata análise de **25 marcas durante seis meses e 2,6 milhões de citações de LLMs**. A conclusão apresentada é que conteúdo diretamente controlado pelas marcas responde por apenas cerca de **1% das citações** usadas pelos modelos. A mesma pesquisa aponta confiança em IA generativa abaixo de 40%, embora Gen Z e boomers já a usem para descoberta e avaliação de compras.

**Desejo/comportamento revelado**  
O consumidor parece estar aumentando o número de fontes consultadas enquanto confia menos em cada fonte isoladamente. A necessidade latente é **triangulação**: várias evidências coerentes valem mais que uma afirmação forte da própria marca.

**Por que importa**  
Uma landing page correta continua necessária, mas não é suficiente para influenciar o que agentes de IA dizem. Reviews, comunidades, comparadores, conteúdo editorial e outras fontes externas passam a participar da oferta percebida.

**Aplicação no Marketing Hub**  
Evoluir o `ThirdPartyAIAnswerAudit` para um `EvidenceDistributionMap`: para cada claim importante da oferta, mapear onde existe evidência externa e se diferentes LLMs conseguem recuperá-la. Separar `OWNED_EVIDENCE`, `CUSTOMER_EVIDENCE`, `COMMUNITY_EVIDENCE`, `EDITORIAL_EVIDENCE` e `INDEPENDENT_DATA`.

**Experimento/feature**  
Escolher uma oferta e criar duas estratégias durante um ciclo: A) melhorar apenas a landing; B) melhorar a landing + publicar/estimular evidências externas legítimas e verificáveis. Depois consultar periodicamente múltiplos agentes com as mesmas perguntas e medir `ClaimCoverage`, `AnswerAccuracy`, `SourceDiversity` e tráfego/conversão assistidos por IA.

**Impacto potencial**: **muito alto e estratégico**, porque o Marketing Hub passaria a otimizar não apenas páginas e anúncios, mas o **ecossistema de evidências que alimenta decisões humanas e de agentes**.

**Fonte**  
- https://www.mckinsey.com/industries/consumer-packaged-goods/our-insights/consumers-dont-trust-ai-advice-they-turn-to-it-anyway

---

## Síntese para o Marketing Hub

O modelo conceitual desta rodada pode ser resumido assim:

**sequência comportamental → estado de intenção → interação conversacional/voz → evidência verificável → ação → novo estado**

Prioridade sugerida para backlog:

1. `BehaviorSequence + IntentStateModel`
2. `EvidenceDistributionMap`
3. `VoiceStyle / ProsodyProfile`
4. `EmotionSignalConfidence`

O princípio mais forte é: **prever comportamento a partir do que a pessoa faz, validar confiança por evidência distribuída e tratar sinais emocionais inferidos como hipóteses incertas — não como fatos sobre o usuário.**
