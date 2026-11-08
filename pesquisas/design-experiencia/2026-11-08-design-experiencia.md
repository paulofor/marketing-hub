# Radar de Design de Experiência — 2026-11-08

**Experience Engine v69 | Marketing Hub | 08/11/2026 (domingo)**

## Escopo, novidade e rigor
A consulta de 08/11 confirmou que a listagem mais recente de Human-Computer Interaction do arXiv corresponde à sexta-feira, 06/11/2026 (https://arxiv.org/list/cs.HC/recent). Não foram apresentadas submissões dominicais como se existissem. A rodada revisou trabalhos ainda não destacados dos lotes de 05–06/11 e incorporou resultados científicos complementares anteriores, identificando suas datas. Foram excluídas repetições de IntentWeaver, Souffleur, ControlAnything, TACT, estudos de preferência cinematográfica e o estudo sobre complexidade visual e cortes já registrado no radar de prazer audiovisual de 01/09.

**Síntese:** uma característica desenhada para aumentar a sensação de presença pode também elevar desconfiança; estímulos hápticos podem acelerar uma ação e simultaneamente aumentar a frustração; agentes colaborativos precisam ser avaliados pelo valor da combinação humano–IA, não somente pela nota do agente.

## 1. BobFlow: lógica inspecionável entre pessoa e agente — submetido em 04/11/2026
**Fonte:** Bai, Huang e Li, *BobFlow: Towards Human-Agent Collaborative Data Science Using Dataflows*. https://arxiv.org/abs/2611.05761

- **Descoberta/evidência:** proposta de fluxo de dados como representação comum entre analista e agente ReAct. Os autores reportam aumento de até 18,6 pontos em uma medida de acurácia em benchmarks e custo de inferência pouco acima de um terço do agente baseado em scripts; estudo de usuários aponta melhora na capacidade de inspecionar e verificar análises.
- **Mecanismo (interpretação):** um grafo de transformações externas à conversa reduz a reconstrução mental de passos e torna dependências visíveis.
- **Implicação:** no AI Hub, representar análises, cálculos e decisões como nós tipados com entradas, saídas, versões, logs e possibilidade de intervenção.
- **Hipótese/teste:** comparar fluxo baseado em scripts e grafo de operações para a mesma tarefa, medindo correção humana dos erros, custo por tarefa correta e esforço de revisão.
- **Riscos/limites:** resultados de benchmarks específicos, não estimativas universais de redução de custo nem comprovação de eficácia comercial. **Sem card:** coleção para colaboração/agentes não existe na API.

## 2. Háptica: desempenho operacional e conforto podem divergir — submetido em 04/11/2026
**Fonte:** Brown, Farkhatdinov e Jenkin, *Cognitive and Behavioural Effects of Haptic Feedback for ROV Teleoperation in Hazardous Environments*. https://arxiv.org/abs/2611.06689

- **Descoberta/evidência:** em teleoperação aquática com correntes não visíveis, pistas táteis em touchpad reduziram parte da demanda física/temporal, melhoraram o desempenho autorrelatado e permitiram maior velocidade que a condição somente com corrente; em contrapartida, elevaram demanda mental e frustração.
- **Mecanismo proposto:** uma vibração transforma um risco oculto em informação percebida, mas aprender seu significado consome capacidade cognitiva.
- **Implicação:** avaliar sistemas com feedback tátil em duas dimensões — execução correta e conforto — e oferecer controle de frequência/intensidade.
- **Hipótese/teste:** comparar avisos visuais, hápticos e multimodais em atividade com riscos moderados; medir detecção, erros, velocidade e carga mental.
- **Riscos/limites:** hardware e teleoperação especializados; não prova vantagens em smartphones ou comércio digital. **Sem card:** não é prazer audiovisual nem comportamento de compra.

## 3. Medir a colaboração, não apenas o agente — submetido em 04/11/2026
**Fonte:** Li e Nakahara, *Measuring the collaboration, not the agent*. https://arxiv.org/abs/2611.06487

- **Descoberta/evidência:** propõe cinco medidas de colaboração: complementaridade, eficiência de intervenção, dependência apropriada, retenção de conhecimento e agência humana. Quatro delas são calculáveis por logs; a demonstração foi feita com analista simulado num problema de visualização de dados.
- **Mecanismo:** um modelo pode produzir uma resposta boa e ainda assim eliminar conhecimento específico ou autonomia do operador.
- **Implicação:** no AI Hub, guardar aceitação, rejeição, correção e retomada de decisões de agentes, não apenas taxa de sucesso.
- **Hipótese/teste:** testar métricas com operadores reais e avaliar se predizem erros ou perda de controle.
- **Riscos/limites:** **nenhum experimento com analistas reais foi realizado nesse teste inicial**. Sem card, pois a coleção de avaliação humano–IA não é suportada.

## 4. Pictogramas para reparar transcrições de voz — revisão de 05/11/2026
**Fonte:** Hernandez et al., *Contextual Pictograms for Repairing Multilingual ASR Errors*. https://arxiv.org/abs/2611.05637

- **Descoberta/evidência:** proposta de interface que combina alternativas textuais, repetição de áudio e pictogramas contextualmente conferidos para palavras reconhecidas com incerteza. Versão 2 de 05/11 ainda descreve estudo contrabalanceado futuro, **não resultados já comprovados**.
- **Mecanismo proposto:** representação visual adicional poderia ajudar a detectar erro semântico na transcrição.
- **Implicação:** oferecer ao usuário opções verificáveis para corrigir transcrições ambíguas em interfaces por voz, sem inferir silenciosamente uma intenção.
- **Hipótese/teste:** texto versus texto+áudio versus texto+áudio+pictograma, medindo recuperação correta do comando e tempo adicional.
- **Riscos/limites:** pictograma errado pode confirmar uma interpretação incorreta; idiomas iniciais francês/espanhol, sem validação em PT-BR. Sem card por falta de coleção.

## 5. Expressividade emocional de avatares pode diminuir confiança — artigo publicado em 30/12/2025, volume de 2026
**Fonte:** Stamos et al., *When Digital Human Avatars Emote, Users Pull Back*, *Psychology & Marketing*. https://doi.org/10.1002/mar.70088

- **Descoberta/evidência:** três experimentos sobre avatares em saúde e turismo identificaram menor intenção declarada de uso diante de expressões emocionais mais intensas. A análise encontrou confiança percebida como mediadora estatística e efeito negativo indireto particularmente em pessoas com menor tendência a antropomorfizar.
- **Mecanismo (interpretação):** semelhança humana e emoção não garantem autenticidade ou confiança; para alguns usuários, uma persona expressiva pode criar incongruência e desconforto.
- **Implicação:** em experiências de atendimento com avatar, dar escolha de modalidade e evitar expressividade artificial desnecessária.
- **Hipótese/teste:** randomizar avatar neutro, avatar moderadamente expressivo e avatar mais expressivo, mantendo igual a informação entregue. Medir conclusão de tarefas, confiança calibrada, retorno e vendas reais separadamente.
- **Riscos/limites:** evidência de intenção, **não retenção ou compra real**; diferentes culturas e públicos podem responder de maneira oposta. **Card:** selecionado para `neuromarketing`, conceito de percepção de confiança em UX B2C não equivalente ao card anterior sobre empatia verbal em chatbots.

## 6. Anúncios de IA podem emocionar sem comunicar claramente a marca — publicado em 23/07/2026
**Fonte:** Shitova et al., *Minds Versus Codes*, *Psychology & Marketing*. https://doi.org/10.1002/mar.70235

- **Descoberta/evidência:** investigação exploratória com 20 universitários de 18–34 anos, combinando EEG, eye-tracking e entrevistas. Anúncios feitos com IA provocaram respostas emocionais; participantes também apontaram momentos de fraca ligação entre narrativa e marca. A revelação da origem da peça interferiu em algumas avaliações.
- **Mecanismo possível:** interesse emocional na cena e reconhecimento do propósito da marca são processos que podem divergir.
- **Implicação:** avaliar separadamente emoção relatada, marca lembrada e compreensão da oferta, evitando que o criativo bonito dilua a proposta comercial.
- **Hipótese/teste:** comparar vídeos equivalentes com marca integrada à narrativa versus marca discreta, controlando origem e duração; medir compreensão e eventos reais do funil.
- **Riscos/limites:** amostra muito pequena e qualitativa, medidas EEG não demonstram causalidade de emoção ou ganho comercial. **Sem novo card**, para não duplicar o conceito já representado em 07/11 (novidade sem utilidade de anúncio).

## 7. Prototipagem por IA pode produzir interfaces úteis e convencionais — preprint de 14/05/2026
**Fonte:** Romero et al., *Usable but Conventional: An Empirical Study on the UX of AI-Generated Interface Prototypes*. https://arxiv.org/abs/2605.15124

- **Descoberta/evidência:** 92 pessoas avaliaram protótipos de autoria humana e gerados por IA, sem informação prévia sobre sua origem, usando UEQ-S. O resultado sugere qualidade pragmática favorável para protótipos de IA, mas qualidade hedônica menos expressiva — particularmente originalidade/inovação.
- **Mecanismo (interpretação):** padrões comuns facilitam usabilidade, mas podem produzir experiências percebidas como genéricas.
- **Implicação:** no gerador de landing pages, medir separadamente sucesso da tarefa e diferenciação visual.
- **Hipótese/teste:** IA sem direção estética versus IA com princípios explícitos de identidade e revisão humana, comparando UEQ-S e tarefas reais.
- **Riscos/limites:** avaliação de protótipos, não produtos em uso; não mediu aquisição, conversão ou retenção. **Sem card**, falta coleção de UX/generative UI.

## 8. IA deve reconhecer os limites de verificabilidade e segurança operacional — submetido em 05/11/2026
**Fonte:** Ali et al., *Responsible GenAI Use During Active Armed Conflict: Evidence from Sudan's Emergency Response Rooms*. https://arxiv.org/abs/2611.07874

- **Descoberta/evidência:** survey exploratório com 89 participantes e 15 entrevistas no contexto da resposta humanitária no Sudão. A IA parecia mais valiosa quando expertise humana era difícil de acessar, mas justamente nessas condições havia menos meios de conferir fatos locais em rápida mudança; o próprio acesso à tecnologia podia acarretar riscos.
- **Mecanismo:** utilidade subjetiva não equivale à verificabilidade da resposta; pressão e risco operacional alteram o custo de erros.
- **Implicação:** agentes devem mostrar fontes, data, incerteza e possibilidade de operação manual, inclusive abstendo-se de agir em ambiente crítico não verificável.
- **Hipótese/teste:** em simulação segura, avaliar políticas de recusa/checagem em tarefas com estado mutável.
- **Riscos/limites:** contexto humanitário extremo e pesquisa qualitativa; não generalizar diretamente a marketing nem fazer experimentos em condições reais de risco. **Sem card**, falta coleção de governança de agentes.

## Decisão editorial e aplicação
Guia vigente consultado diretamente em `main`: https://github.com/paulofor/marketing-hub/blob/main/harness-library-api/docs/guia-uso-api-cards.md

Coleções permitidas: `video`, `prazer-audio-visual`, `neuromarketing`, `momentos-de-compra-b2c`.

Foi criada uma fonte revisada individual e um card candidato a DRAFT em `neuromarketing` apenas para a expressividade emocional de avatares. Estudos de BobFlow, háptica, colaboração humano–IA, pictogramas, interfaces generativas e limites da IA em decisões críticas ficaram no relatório porque não existe uma coleção legitimamente aderente. Não foi criado card para vídeos publicitários por sobreposição temática com o conhecimento já registrado.

A prioridade prática é tratar **confiança, conforto, precisão, conclusão de tarefas e conversão** como indicadores distintos. Nenhum desses estudos é prova de sucesso comercial para o Marketing Hub.
