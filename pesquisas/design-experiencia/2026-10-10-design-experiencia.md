# Radar de Design de Experiência — 2026-10-10

**Janela:** preprints de 7–8/10, liberados na listagem de 9/10. Sábado não teve nova remessa regular do arXiv. Sem repetir os achados do radar de 09/10.

**Tese:** interfaces multimodais precisam de evidências perceptivas e de artefatos intermediários verificáveis; conformidade com a rubrica não é sinônimo de qualidade para o ser humano.

## 1. Estética de voz contextual (CVAM)
**Evidência:** cerca de 3.000 respostas reais/sintéticas, ~10 ouvintes por resposta. No estudo de Jiang et al., macro-F1 para rótulos perceptivos chegou a 50,1%, versus 44,2% de Gemini 3.1 Pro. **Mecanismo proposto:** apreciação de emoção e entrega vocal depende do contexto e varia entre pessoas. **Hipótese prática:** testar vozes de narração com um painel humano e comparar com escolha automatizada, usando adequação percebida, entendimento e abandono. **Limite:** resultado mede concordância com anotações, não satisfação, prazer ou vendas; dados em inglês. Fonte: https://arxiv.org/abs/2610.10868

## 2. Música e iluminação adaptativas (AuraLuxMuse)
**Evidência:** 40 especialistas e 40 espectadores leigos avaliaram apresentações cegas. Especialistas preferiram cues gerados em concerto e entretenimento, mas a diferença média entre cenários não foi significativa; espectadores leigos preferiram a iluminação manual em média. **Mecanismo:** sincronismo percebido por especialistas e experiência estética do público podem divergir. **Hipótese prática:** comparar cues controláveis e iluminação desenhada manualmente em cenas curtas. **Limite:** palco, não anúncios ou vídeo social; não inferir benefício comercial. Fonte: https://arxiv.org/abs/2610.11792

## 3. Edição de vídeo ancorada em keyframe (VINCIE-NExT)
**Evidência:** em 431 clipes do OpenVE-Bench, dez avaliadores humanos preferiram a configuração completa do método à reduzida na aderência à instrução em 38% vs. 24% dos pares (39% empates); em qualidade visual, 15% vs. 14%. **Mecanismo proposto:** referência visual explicita mudança e ajuda a mantê-la entre quadros. **Hipótese prática:** vídeo → quadro editado revisável → propagação, medindo drift, retrabalho e custo comparados à edição direta. **Limite:** não isolou efeito de aprovação humana; falhas no quadro se propagam; vídeos longos não validados. Fonte: https://arxiv.org/abs/2610.12104

## 4. Verificação de alegações em vídeos curtos (MiniVer-V)
**Evidência:** 195 vídeos, 5.510 evidências possíveis; macro-F1 0,510 com 4,5 evidências em média vs. 0,518 com 27,7 da base completa (Claude Sonnet 4). **Mecanismo:** mais documentos relevantes não significam suporte suficiente à alegação. **Hipótese:** Têmis separar vídeo/claim, corroboração externa e status insuficiente, medindo falso aceite e custo. **Limite:** benchmark de checagem de fatos; exige supervisão editorial. Fonte: https://arxiv.org/abs/2610.11233

## 5. Memória apropriada ao momento (Reconsider)
**Evidência:** entrevistas com 14 usuários, 80 cenários e 400 pares de avaliação em cinco modelos; juízes LLM favoreceram procedimento de checagem de memória em +15 e +23 pontos líquidos, não para todos os modelos de maneira robusta. **Mecanismo:** recordar fatos verdadeiros pode ser inconveniente ou invasivo no momento errado. **Hipótese:** verificar relevância, sensibilidade e permissões antes de citar a memória; medir reparos e desconforto. **Limite:** juízes majoritariamente automatizados, não longitudinal humano. Fonte: https://arxiv.org/abs/2610.09470

## 6. Clarificação seletiva antes da geração
**Evidência:** experimento randomizado de Scott-Jackson reportou qualidade composta 32% maior com perguntas estruturadas antes do deliverable, queda de entregas fora da tarefa de 34% para 5% e ~2,4 min adicionais. **Mecanismo:** explicitar objetivo e trade-offs reduz ambiguidade; atualização conceitual de Engage-to-Unlock. **Hipótese:** questionar somente briefing sem público, oferta ou critério de êxito, medindo retrabalho e tempo total. **Limite:** documentos estratégicos experimentais; recordação imediata não foi robusta. Fonte: https://arxiv.org/abs/2610.09593

## 7. Movimento demonstrado por fala e gesto
**Evidência:** *Just Like This* combina fala, apontamento e movimento em 3D; 12 usuários declararam 89% de conclusão contra 47% com voz isolada, e nove preferiram multimodalidade. **Mecanismo:** trajetória e velocidade são difíceis de descrever só em palavras. **Hipótese:** edição de animações com gesto → prévia → confirmação. **Limite:** protótipo pequeno e especializado. Fonte: https://arxiv.org/abs/2610.09099

## 8. Claims sem prova (AI-washing)
**Evidência:** auditoria de 100 startups alemãs e 63 entrevistas encontrou claims sem evidência, desempenho sem metodologia e limites omitidos. **Mecanismo possível:** exagero percebido pode diminuir credibilidade posterior. **Hipótese:** Íris e Têmis exigir fonte para claims de IA e testar clareza/entendimento, sem prometer conversão. **Limite:** não prova efeito causal em consumidores brasileiros. Fonte: https://arxiv.org/abs/2610.11788

## 9. Supervisão streaming (OnTrack)
**Evidência:** em SWE-bench, aumentou AUROC em 0,057 contra similaridade de conteúdo, economizando cerca de 18% do compute de execuções que falhariam numa política de abort; cinco em seis abortos antecipavam falha. **Mecanismo:** detectar loops antes de concluir economiza desperdício. **Hipótese:** testar monitoramento de traces em tempo real versus posterior. **Limite:** experimento técnico, poucos aborts; falso positivo interrompe tarefas úteis. Fonte: https://arxiv.org/abs/2610.12375

## 10. Otimizar nota pode piorar tutoria
**Evidência:** 2.000 cenários, 31 avaliadores educadores; métrica otimizada aumentou de +0,05 a +0,42, enquanto avaliações humanas caíram de 4,46 para 3,03. **Mecanismo:** repetição do comportamento de maior score local empobrece a trajetória. **Hipótese:** no self-improvement, avaliar diversidade temporal, não apenas nota de cada etapa. **Limite:** caso de tutoria, não marketing. Fonte: https://arxiv.org/abs/2610.12125

## Cards selecionados
Guia atual: `harness-library-api/docs/guia-uso-api-cards.md`. Coleções permitidas: `video`, `prazer-audio-visual`, `neuromarketing`, `momentos-de-compra-b2c`.

- `voz-estetica-alinhada-julgamento-ouvintes` → `prazer-audio-visual`; fonte revisada e JSON no mesmo lote.
- `video-edicao-keyframe-demonstracao-visual` → `video`; fonte revisada e JSON no mesmo lote.

Os demais achados não viraram card: Reconsider, MiniVer-V, clarificação e OnTrack são primordialmente harness; gestos 3D e AI-washing não demonstram efeitos comerciais ou perceptivos na coleção; AuraLuxMuse tem distância de domínio e preferência conflitante de públicos; a otimização de tutores pertence a avaliação de agente. Nunca interpretar esses estudos como prova de vendas; validar com eventos reais do funil. Candidatos apenas a DRAFT; nenhuma revisão, ativação ou arquivamento.
