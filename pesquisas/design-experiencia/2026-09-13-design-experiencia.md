# Radar de Design de Experiência — 2026-09-13

## Resumo executivo

A rodada de 13 de setembro de 2026 reforça uma ideia central: **experiências com IA ficam melhores quando o sistema calibra percepção, explicação, modalidade e confiança, em vez de simplesmente maximizar naturalidade, imersão ou quantidade de informação**.

Os achados mais úteis de hoje foram:

1. pessoas familiarizadas com IA não necessariamente distinguem melhor conteúdo sintético; treinamento com exemplos rotulados e feedback melhorou discernimento em experimento controlado;
2. explicações humanas tendem a ser seletivas: curtas, suficientes e compostas por argumentos diretamente relacionados;
3. tecnologias mais imersivas, como luvas hápticas e esteiras VR, podem piorar usabilidade e carga cognitiva sem aumentar presença;
4. diálogos de voz artificialmente “perfeitos” podem perder características humanas importantes, como variação de turno, backchannels e mudança contextual de papel;
5. confiança verbal do próprio LLM é um sinal fraco; verificação por afirmação e calibração produziram uma medida de incerteza mais útil, embora ainda imperfeita;
6. um RCT publicado ontem mostra que GenAI pode funcionar melhor quando inserida em um protocolo humano estruturado e delimitado, mas o resultado vem de um contexto clínico sensível e não deve ser generalizado comercialmente;
7. uma revisão de 135 estudos em marketplaces reforça que personalização útil depende não só da precisão algorítmica, mas também de cognição do consumidor, autonomia, transparência e capacidade real de implantação.

---

## 1. Familiaridade com IA não garante discernimento de conteúdo sintético — e julgamento pode ser recalibrado

**Descoberta.** O artigo *Human discernment of artificial intelligence in online markets can be shaped by experience and training*, publicado em 12 de setembro na *AI & SOCIETY*, investigou se uso prévio de IA e uma intervenção curta alteravam a capacidade de distinguir conteúdo humano de conteúdo sintético.

**Evidência.** O estudo analisou 117 adultos dos Estados Unidos, de 18 a 34 anos. A acurácia inicial média foi de 57,4%. Atitudes favoráveis ou desfavoráveis à IA não predisseram significativamente o desempenho, mas maior frequência/variedade de engajamento com IA esteve associada a pior discernimento (`r = -0,29; p = 0,002`). Essa parte é correlacional. Em seguida, 60 participantes receberam exemplos corretamente rotulados como humanos/IA e depois feedback imediato por tentativa com incentivo por acerto; 57 controles tiveram exposição sem rótulo e feedback/incentivo atrasados. O grupo experimental melhorou significativamente nas avaliações intermediária e final, enquanto o controle não melhorou.

**Mecanismo psicológico/comportamental.** Exposição repetida a conteúdo sintético não parece automaticamente ensinar quais pistas são discriminativas. Exemplos com origem conhecida e feedback de resultado podem recalibrar a atenção para pistas mais úteis. Como a intervenção combinou rótulos, feedback e características do incentivo, não é possível atribuir o ganho a um único componente.

**Implicação para produto.** Em revisão de criativos, textos ou interações, não deveríamos assumir que alguém “que usa muita IA” reconhece melhor sinais de artificialidade. Se a origem percebida for importante, medir diretamente a percepção ou calibrar avaliadores com exemplos de ground truth é mais defensável do que confiar em experiência prévia.

**Hipótese/experimento.** Antes de uma sessão de revisão de criativos, comparar revisão normal versus uma calibração de poucos minutos com exemplos de origem conhecida e feedback. Medir acurácia de origem, concordância entre revisores e se essa calibração melhora ou prejudica a avaliação real de qualidade.

**Riscos/limites.** Detectar origem não é o mesmo que avaliar qualidade. A amostra é jovem, norte-americana e o domínio foi aluguel de temporada. O estudo não mede CTR, preferência ou venda.

Fonte:
- https://link.springer.com/article/10.1007/s00146-026-03373-3

**Card criado:** `feedback-calibrado-discernimento-conteudo-sintetico`, coleção `neuromarketing`.

---

## 2. Explicações úteis parecem ser seletivas, não exaustivas

**Descoberta.** *Empirically Testing Explanation Preferences in Computational Argumentation*, publicado em 12 de setembro na *Cognitive Computation*, testou empiricamente quais argumentos pessoas escolhem para formar uma explicação.

**Evidência.** Foram recrutadas 301 pessoas via Prolific, de 42 países; cinco respostas extremamente rápidas foram rejeitadas. Em oito cenários, participantes selecionavam argumentos para explicar uma conclusão. Explicações suficientes, compactas e mínimas apareceram mais frequentemente que o esperado nos baselines dos autores. Mesmo após controlar a preferência geral por respostas curtas, explicações seletivas continuaram favorecidas. Argumentos não relacionados foram raramente escolhidos. Quando o framework continha mais informação e exigia um argumento adicional para a explicação mínima, o tamanho das explicações escolhidas aumentou apenas cerca de 25%.

**Mecanismo psicológico/comportamental.** Pessoas parecem filtrar explicações para preservar apenas informação diretamente relacionada. Carga cognitiva é uma explicação plausível: mais conteúdo aumenta custo de processamento sem necessariamente aumentar compreensão. O estudo não isola esse mecanismo causalmente.

**Implicação para produto.** Para agentes, “mostrar todo o raciocínio” não deveria ser o default. Uma política mais promissora é `recomendação → critério principal → evidência relacionada → limite/incerteza`, com aprofundamento expansível.

**Hipótese/experimento.** Comparar justificativa longa, justificativa curta porém genérica e justificativa seletiva com evidência diretamente relacionada. Medir compreensão objetiva, tempo, re-prompts, confiança calibrada e decisão correta.

**Riscos/limites.** Em decisões de maior risco, explicações curtas podem omitir ressalvas essenciais. O experimento tinha stakes baixos e pouca pressão temporal, portanto a preferência pode mudar em cenários críticos.

Fonte:
- https://link.springer.com/article/10.1007/s12559-026-10654-y

**Card atualizado:** reutilizado o `cardKey` `explicacao-ia-especifica-coerente`, coleção `neuromarketing`, em vez de criar uma ideia duplicada.

---

## 3. “Mais imersão” pode piorar a experiência: evidência forte com haptics + VR

**Descoberta.** O artigo *More immersion, better experience? The trade-offs of haptic gloves and treadmill locomotion in virtual museums*, publicado em 12 de setembro, testa diretamente a suposição de que adicionar mais modalidades imersivas melhora a UX.

**Evidência.** O estudo within-subject teve 162 participantes, todos passando por quatro condições que combinavam controle ou luvas hápticas e locomoção por controle ou esteira VR. Foram medidos presença, SUS, carga cognitiva e cybersickness. As luvas hápticas reduziram significativamente usabilidade (`β = -3,59; p < 0,05`) e aumentaram carga cognitiva (`β = 0,23; p < 0,05`). A esteira reduziu carga (`β = -0,36; p < 0,05`) sem efeito independente significativo sobre presença, usabilidade ou cybersickness. Surpreendentemente, a combinação luvas + esteira produziu menor presença relativa (`β = -0,17; p < 0,05`). Cybersickness não mudou significativamente.

**Mecanismo psicológico/comportamental.** Cada modalidade adicional pode aumentar natural mapping, mas também acrescenta calibração, esforço motor, aprendizagem e atenção à tecnologia. Quando o custo de interação supera o ganho sensorial, a tecnologia vira parte da tarefa em vez de desaparecer dentro da experiência.

**Implicação para produto.** Multimodalidade deve ser avaliada pelo ganho líquido, não pela quantidade de canais. Para voz, haptics, gesto ou animação, perguntar: `qual problema esta modalidade resolve? → qual carga ela adiciona? → existe ganho comportamental mensurável?`.

**Hipótese/experimento.** Testar uma feature multimodal em três níveis: sem canal extra, canal extra simples e canal extra rico. Medir objetivo da tarefa, workload, erro, tempo e preferência separadamente.

**Riscos/limites.** O domínio é museu VR, com estudantes e hardware especializado. O resultado não diz que haptics é ruim em geral; mostra que hardware mais sofisticado não garante ganho de presença ou UX.

Fonte:
- https://link.springer.com/article/10.1007/s11042-026-21911-5

**Card não criado.** Embora exista aderência possível a `prazer-audio-visual`, o estudo é sobre VR especializado e ainda não oferece uma regra suficientemente direta para o pipeline audiovisual atual do Marketing Hub.

---

## 4. Voz natural não é voz “perfeitamente organizada”

**Descoberta.** *Not your average podcaster*, publicado em 12 de setembro, comparou podcasts humanos com diálogos gerados pelo NotebookLM para estudar turn-taking e papéis conversacionais.

**Evidência.** Nos diálogos analisados, turnos humanos foram muito mais longos e variáveis: hosts humanos tiveram média de 22,3 s e convidados 49,4 s, contra 11,3 s e 14,8 s para hosts e convidados gerados. O efeito do tipo de podcast na duração dos turnos foi significativo (`β = 39,11; p < 0,001`). Overlaps e backchannels apareceram mais nos diálogos humanos. O artigo também descreve maior capacidade humana de mudar dinamicamente entre expert, novice e clarification conforme a conversa evolui; intervenções humanas espontâneas expuseram limitações maiores no diálogo gerado.

**Mecanismo psicológico/comportamental.** Conversação natural não é apenas minimizar silêncio e overlap. Humanos usam backchannels, interrupções pequenas, hesitação, adaptação ao interlocutor e mudanças de papel como sinais de atenção e alinhamento social.

**Implicação para produto.** Em agentes de voz, otimizar apenas “latência mínima + zero sobreposição” pode produzir conversa mecanicamente correta e socialmente rígida. Turnos, silêncio, acknowledgments e interrupções deveriam ser tratados como parte da política conversacional.

**Hipótese/experimento.** Comparar agente com timing uniforme e sem overlaps contra variante com pausas contextuais, backchannels discretos e adaptação de papel. Medir interrupções percebidas, conforto, compreensão e duração voluntária da conversa.

**Riscos/limites.** O estudo é exploratório e usa um corpus muito pequeno de podcasts; ele não mede preferência dos ouvintes nem demonstra causalmente que adicionar overlaps melhora UX.

Fonte:
- https://link.springer.com/article/10.1007/s00146-026-03365-3

**Card não criado.** O achado é promissor para `prazer-audio-visual`, mas a evidência ainda é pequena e descritiva para virar orientação persistente.

---

## 5. Confiança de um LLM deveria ser calibrada por afirmação, não simplesmente declarada

**Descoberta.** *Improving reliability of large language models via claim-level self-verification and uncertainty calibration*, publicado em 12 de setembro, testa um pipeline que quebra respostas em afirmações, procura suporte/contradição e calibra a confiança depois.

**Evidência.** Em um conjunto experimental de 200 exemplos do TruthfulQA, com 60 exemplos usados para calibração e 140 held-out, o método CLAIM-CAL atingiu acurácia observada de 0,757. A contribuição mais forte foi de calibração: o Expected Calibration Error caiu de 0,212 no sinal bruto para 0,038 após regressão isotônica, com intervalo bootstrap de 95% `[0,015, 0,106]`. Em threshold de 0,7, a versão calibrada obteve selective accuracy de 0,824 com cobertura de 0,893. Ainda assim, 22 respostas incorretas permaneceram acima de 0,7 de confiança.

**Mecanismo psicológico/comportamental.** Uma resposta inteira pode misturar afirmações robustas e frágeis. Um único “estou 90% confiante” mascara essa heterogeneidade. Verificação granular e calibração permitem que o sistema identifique partes que merecem revisão, abstention ou comunicação explícita de incerteza.

**Implicação para produto.** Um agente não deveria exibir “confiança” apenas porque o próprio modelo disse estar confiante. Para ações relevantes, considerar `decompor claims → verificar suporte e contradição → calibrar → decidir responder, pedir revisão ou escalar`.

**Hipótese/experimento.** Injetar tarefas com fatos misturados entre verdadeiros e falsos e comparar confiança verbal simples versus verificação granular. Medir false confidence, cobertura e quantidade de escaladas úteis.

**Riscos/limites.** Dataset pequeno, domínio TruthfulQA e calibração pós-hoc. O método continua produzindo erros confiantes e não garante factualidade.

Fonte:
- https://link.springer.com/article/10.1007/s44163-026-02240-w

**Card não criado por falta de coleção válida.** O achado é forte para harness/arquitetura de agentes, mas não cabe honestamente em `video`, `prazer-audio-visual`, `neuromarketing` ou `momentos-de-compra-b2c`.

---

## 6. Um RCT sugere valor para GenAI inserida em protocolo estruturado — mas apenas no domínio estudado

**Descoberta.** Um artigo publicado em 12 de setembro na *npj Digital Medicine* avaliou uma intervenção de sessão única combinando humano e GenAI para ansiedade acadêmica.

**Evidência.** Foram realizados dois ensaios randomizados, totalizando 425 participantes, com controle ativo e waitlist. A intervenção híbrida foi considerada aceitável e segura no contexto estudado e produziu melhoras maiores nos desfechos primários: ansiedade de estado imediata e, em duas semanas, ansiedade acadêmica e procrastinação. Os tamanhos de efeito foram `d = 0,25–0,35` versus controle ativo e `d = 0,43–0,58` versus waitlist. Efeitos em desfechos secundários, como ansiedade generalizada e sintomas depressivos, foram limitados.

**Mecanismo psicológico/comportamental.** O resultado não é evidência de que “conversar livremente com um LLM faz bem”. A IA foi inserida em uma intervenção estruturada, com objetivo, protocolo e limites claros. Isso reforça a hipótese arquitetural de que, em tarefas de consequência elevada, **bounded protocol > chatbot genérico**.

**Implicação para produto.** Fora de saúde, o padrão pode inspirar agentes orientados a processo: delimitar objetivo, sequência, permitted actions, checkpoints e critério de término, em vez de simplesmente abrir um chat e esperar que o modelo improvise.

**Hipótese/experimento.** Em um domínio não clínico, comparar agente livre versus agente com protocolo estruturado e checkpoints para a mesma tarefa. Medir conclusão, erros, desvios e recuperação.

**Riscos/limites.** Este é um estudo de saúde mental e não deve ser extrapolado para diagnóstico, terapia ou qualquer promessa comercial. Há patente pendente relacionada ao componente conversacional. A aplicação ao Marketing Hub é apenas uma hipótese arquitetural.

Fonte:
- https://www.nature.com/articles/s41746-026-03199-9

**Card não criado.** O domínio é clínico/sensível, e transformá-lo em orientação comercial nas coleções atuais seria uma generalização indevida.

---

## 7. Personalização de marketplace precisa ser avaliada em três camadas, não apenas por precisão

**Descoberta.** Uma revisão sistemática e bibliométrica publicada em 12 de setembro sintetizou 135 estudos sobre recomendadores de IA em marketplaces.

**Evidência.** A revisão PRISMA cobre trabalhos de 2007 a 2026 e identifica uma lacuna persistente entre protótipos com boa performance algorítmica e implantação real em marketplace. Os autores organizam a literatura em três dimensões conectadas: arquitetura/sistema de IA, cognição e comportamento do consumidor, e implementação/deployment. Transparência, explicabilidade, privacidade, bias e autonomia aparecem como desafios recorrentes. Cerca de 71% dos trabalhos incluídos eram conference papers, o que limita a força de generalizações de longo prazo.

**Mecanismo psicológico/comportamental.** Relevância preditiva não garante que uma recomendação seja percebida como útil ou legítima. Controle, confiança, explicabilidade e autonomia mediam a experiência; simultaneamente, infraestrutura e governança determinam se a personalização funciona de forma consistente.

**Implicação para produto.** Para recommendation/adaptive UX, avaliar separadamente: `relevância preditiva`, `resposta humana` e `viabilidade operacional`. Um sistema que ganha offline em ranking pode perder no produto por fadiga algorítmica, falta de confiança ou inconsistência de execução.

**Hipótese/experimento.** Para uma recomendação personalizada, medir não apenas click/conversão, mas relevância percebida, aceitação, correções, opt-out e estabilidade operacional.

**Riscos/limites.** A contribuição principal é síntese e framework conceitual; não é um experimento causal. O corpus depende apenas de Scopus e é dominado por conference papers.

Fonte:
- https://link.springer.com/article/10.1007/s44163-026-02182-3

**Card não criado.** A revisão é útil para arquitetura e estratégia, mas ampla demais para virar uma regra comercial específica sem evidência mais direta.

---

## Síntese arquitetural — Experience Engine v13

```text
usuário
   ↓
intenção + contexto + histórico
   ↓
PERCEPTION CALIBRATION
   ├── ground truth quando disponível
   └── feedback de erro/acerto
   ↓
SELECTIVE EXPLANATION POLICY
   ├── critério principal
   ├── evidência relacionada
   └── detalhe expansível
   ↓
MODALITY COST / BENEFIT GATE
   ├── ganho sensorial
   ├── carga cognitiva
   └── custo de aprendizagem
   ↓
CONVERSATIONAL DYNAMICS
   ├── pausa
   ├── backchannel
   ├── interrupção
   └── papel contextual
   ↓
CLAIM-LEVEL RELIABILITY
   ├── suporte
   ├── contradição
   ├── calibração
   └── escalar/abster
   ↓
BOUNDARY / PROTOCOL
   ↓
experiência
   ↓
resultado + compreensão + autonomia + confiança calibrada
```

O princípio desta rodada é: **não confundir fluidez com qualidade**. Conteúdo que parece humano pode ser sintético; uma explicação longa pode parecer transparente sem ser útil; multimodalidade mais rica pode diminuir UX; voz perfeitamente limpa pode soar menos natural; confiança verbal pode ser mal calibrada. O sistema precisa de mecanismos de calibração e medição para cada uma dessas dimensões.

---

## Cards gerados

### `feedback-calibrado-discernimento-conteudo-sintetico`

- Coleção: `neuromarketing`
- Por que importa: evita usar familiaridade com IA como proxy de percepção e fornece um experimento concreto de calibração para revisão de criativos/mensagens.
- Fonte revisada: `pesquisas/design-experiencia/cards/fontes/2026-09-13-feedback-calibrado-discernimento-conteudo-sintetico.md`
- SHA-256: `42f1689377dc64c63642de819f8c9671a5a3faff5a222f624d916421c9591bf1`
- JSON: `pesquisas/design-experiencia/cards/2026-09-13-feedback-calibrado-discernimento-conteudo-sintetico.json`

### `explicacao-ia-especifica-coerente`

- Coleção: `neuromarketing`
- Tipo: nova versão da ideia já criada em 11/09, com o mesmo `cardKey`.
- Por que importa: adiciona evidência com N=301 de que explicações curtas, seletivas e diretamente relacionadas se aproximam melhor da preferência humana do que despejar argumentos periféricos.
- Fonte revisada: `pesquisas/design-experiencia/cards/fontes/2026-09-13-explicacao-ia-especifica-coerente.md`
- SHA-256: `7dd378460f4e4f5ea5da114103a5e8569ecfcffe3542b9abe827c80844fb4c52`
- JSON: `pesquisas/design-experiencia/cards/2026-09-13-explicacao-ia-especifica-coerente.json`

Os dois arquivos são candidatos a `DRAFT`; nenhuma etapa de revisão, ativação ou arquivamento foi executada.
