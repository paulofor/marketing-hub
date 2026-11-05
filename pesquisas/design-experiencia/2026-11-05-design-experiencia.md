# Radar Design de Experiência — 2026-11-05

**Experience Engine v66 | Marketing Hub**  
**Rodada:** quinta-feira, 05/11/2026 (horário de Brasília).  
**Recorte:** listagem nova de Human-Computer Interaction do arXiv de 05/11/2026, com artigos submetidos principalmente em 03–04/11/2026. Pré-publicações não equivalem automaticamente a revisão por pares. Priorizei itens não analisados no relatório de 04/11.

## Síntese executiva

A descoberta de maior valor para experiências de personalização é que **a expectativa declarada por uma pessoa sobre um item específico pode acrescentar informação à recomendação algorítmica**, mas perguntar tem custo de atenção e a evidência disponível sofre grande seleção de participantes. Em paralelo, espectadores de vídeos gerados por IA podem **avaliar mal a própria capacidade de reconhecer procedência**, e a certeza excessiva se associa à intenção de compartilhar, sem prova causal.

A rodada identificou ainda riscos de usar modelos como juízes de visualização; o valor de críticas especializadas para corrigir saídas de LLM; limitações das avaliações de credibilidade de texto alterado por IA; e a necessidade de interfaces de transparência orientadas ao contexto e ao público.

## 1. You Know What You Will Like — preferência pontual versus recomendadores

**Fonte:** Chen, Zheng e Zhu, preprint de 04/11/2026. https://arxiv.org/html/2611.06696v1

**Descoberta e evidência:** na base MovieLens Beliefs (2023–2024), 26.020 expectativas de nota foram registradas; 551 delas, de 277 usuários, tiveram nota posterior, pelo menos um dia depois. Na amostra analisável, correlação entre a expectativa da pessoa e a nota posterior de 0,69, comparada a 0,34 do algoritmo MovieLens e 0,54–0,59 de modelos treinados. Incorporar a expectativa ao conjunto de previsores reduziu o erro quadrático médio fora da amostra em 26%. Contudo, 98,8% das solicitações de previsão ficaram sem resposta, e apenas 2,1% dos palpites foram acompanhados de nota posterior elegível. Modelos conseguiram empatar ou superar pessoas em certos subconjuntos de filmes mais conhecidos.

**Mecanismo interpretativo:** a pessoa pode possuir conhecimento contextual de gosto relacionado ao item que o histórico de avaliações não captura; vieses de seleção e exposições anteriores são explicações alternativas.

**Aplicação:** recomendações de conteúdos/produtos digitais podem pedir, de forma opcional, a expectativa atual do usuário sobre um item específico, em vez de inferir todo o gosto somente do comportamento passado.

**Experimento:** A/B entre recomendação comportamental e recomendação comportamental + previsão pontual opcional, medindo taxa de resposta, adequação da recomendação, abandono, satisfação posterior e, separadamente, pagamentos observados.

**Riscos/limites:** MovieLens e usuários altamente auto-selecionados, custo de perguntas, avaliações de filmes não equivalem a compras; sem evidência causal nem efeito de venda. **Card:** neuromarketing, não momentos-de-compra-b2c.

## 2. Sure Enough to Share? — certeza excessiva e vídeos sintéticos

**Fonte:** Xue et al., preprint de 04/11/2026. https://arxiv.org/html/2611.05844v1

**Descoberta e evidência:** estudo online com 973 pessoas avaliando vídeos de cenas cotidianas: 62,5% de acerto ao atribuir origem humana ou sintética. A posição relativa percebida superou a real em 12,8 pontos percentuais. A certeza excessiva sobre cada julgamento mostrou associação positiva com intenções de compartilhamento nos modelos ajustados (b=0,33 em nível individual; b=0,05 por tentativa; ambos p<0,001). Essa associação mudou de direção conforme a origem atribuída ao vídeo.

**Mecanismo interpretativo:** a certeza subjetiva pode afetar o quanto a pessoa questiona a procedência antes de compartilhar, mas certeza e intenção foram observadas conjuntamente; não foi demonstrada causalidade.

**Aplicação:** Apolo/Têmis podem utilizar verificações de origem dos materiais, rótulos verdadeiros e revisão de alegações de autenticidade, incluindo vídeos de cenas aparentemente banais.

**Experimento:** apresentar vídeo com procedência verificada e contexto explícito versus apresentação convencional, medindo reconhecimento correto, confiança calibrada e compartilhamento real, sem inferir conversão a partir de intenção.

**Riscos/limites:** vídeos sem áudio, plataforma experimental, compartilhamento apenas declarado, nenhum teste causal com rótulos e ausência de medidas comerciais. **Card:** video.

## 3. Posts humanos refinados por IA — aparência de credibilidade não valida o conteúdo

**Fonte:** Joos et al., 04/11/2026. https://arxiv.org/html/2611.06660v1

**Descoberta/evidência:** experimento online pré-registrado intraindivíduo com 214 pessoas avaliou posts estilo LinkedIn escritos por humanos versus refinados por IA e sem indicação de origem. Não se detectou efeito médio significativo do refinamento sobre confiança percebida (estimativa +0,016; p=0,777) ou transparência (+0,013; p=0,840). Identificação de alteração por IA foi 49%, aproximadamente ao acaso. Um prompt que adicionava uma ideia não presente no original elevou avaliações, apesar de o conteúdo novo não ter sido verificado.

**Mecanismo:** fluidez e detalhes discursivos podem orientar julgamentos de confiança independentemente da procedência verdadeira; o estudo não isolou completamente quais pistas explicam as avaliações.

**Aplicação:** Têmis deve checar cada informação factual acrescentada à peça por IA; confiança percebida não é evidência de veracidade.

**Experimento:** auditar post original versus post reescrito, com e sem acréscimos substantivos; medir fatos não fundamentados e compreensão, não somente credibilidade declarada.

**Riscos/limites:** textos profissionais não rotulados, sem compra; efeito médio nulo não prova equivalência universal. **Sem novo card:** há conceito correlato registrado no radar anterior sobre confiança factual; estudo permanece documentado sem duplicar card.

## 4. Just the Facts — informação factual importa mais que tom persuasivo

**Fonte:** Costello, Pennycook e Rand, submetido em 03/11/2026. https://arxiv.org/abs/2611.05572

**Descoberta/evidência:** dois experimentos com adultos dos EUA (N total=2.193) testaram intervenções de diálogo com IA sobre crenças conspiratórias. A redução de crenças resistiu a diversas mudanças de enquadramento, extensão e exposição do propósito persuasivo. A intervenção perdeu parte substancial do efeito quando a IA recebeu instrução de persuadir sem apresentar evidências e argumentos factuais.

**Mecanismo:** contraevidência específica e fundamentada parece explicar mais que rapport ou apelo emocional nesse contexto; não transferir como relação quantitativa para publicidade.

**Aplicação:** agentes de orientação comercial devem apoiar comparações em fatos verificáveis, especialmente quando o usuário possui convicções equivocadas sobre produto, prazo e garantia.

**Experimento:** comparar explicações factuais com comunicação somente estilística, medindo correção de compreensão sem incentivar manipulação.

**Riscos/limites:** crenças conspiratórias e contexto político nos EUA, não decisões de compra. **Sem card:** não repetir conceito de confiabilidade factual já catalogado; preservar como atualização contextual no relatório.

## 5. SME-C — críticas de especialistas refinam avaliações de IA

**Fonte:** Ling, Admassu e Rosenthal, preprint de 03/11/2026. https://arxiv.org/html/2611.05749v1

**Descoberta/evidência:** 3.853 críticas de 411 profissionais de finanças sobre 520 consultas em duas rodadas; validação cega posterior com mais 100 especialistas. Refinamentos com base em críticas escritas melhoraram fundamentação de domínio, enquanto refinamentos baseados em votos ajudaram a eliminar conteúdo irrelevante. Ambos superaram respostas-base mais bem classificadas nas métricas avaliadas.

**Mecanismo:** avaliações humanas específicas podem transformar preferências difusas em rubricas verificáveis e orientar correções mais eficientes.

**Aplicação AI Hub:** construir conjunto de avaliações com críticas objetivas de usuários de domínio para testar agentes, preservando exemplos de falhas reais.

**Experimento:** revisão por voto versus crítica textual estruturada versus combinação, medindo erros reais detectados em resultados de trabalho.

**Riscos/limites:** contexto financeiro, respostas de texto e avaliação offline; não prova aumento de produtividade em qualquer software. **Sem card:** não há coleção de avaliação de agentes.

## 6. Visualization Preference — juiz automático pode apagar divergência humana

**Fonte:** Bakshi, Wang e Shahid, IEEE VIS 2026 Workshop, arXiv de 04/11/2026. https://arxiv.org/html/2611.06272v1

**Descoberta/evidência:** estudo-piloto comparou a preferência de anotadores humanos entre pares de gráficos com julgamentos de modelos de visão. Os modelos tenderam a oferecer vereditos quase determinísticos, comprimindo a variação subjetiva nas escolhas humanas.

**Mecanismo:** uma métrica única de preferência pode ocultar diferentes necessidades cognitivas e de contexto.

**Aplicação:** dashboards e interfaces generativas do Marketing Hub devem avaliar desempenho em tarefas com pessoas, não apenas pontuações de juiz LLM.

**Experimento:** comparar ranking por LLM versus ranking plural por usuários, medindo entendimento da informação e erros de decisão.

**Riscos/limites:** piloto, pares selecionados e sem validação em decisões comerciais. **Sem card:** generative UI/visual analytics sem coleção própria.

## 7. Growing One's AI Tools — ferramentas crescem junto com o trabalho

**Fonte:** Zeming Zhao, preprint de 04/11/2026. https://arxiv.org/html/2611.06132v1

**Descoberta/evidência:** estudo de caso de oito semanas acompanhou uma ferramenta de interpretação profissional criada por seu próprio usuário sem formação em programação: 133 sessões registradas, mais de 20 mil falas reconhecidas, 62 commits e seis versões; uma mudança técnica reduziu a mediana de latência em mais de um terço no mesmo dia.

**Mecanismo:** dificuldades observadas em trabalho real podem orientar ciclos curtos de adaptação, reparo e expansão de ferramentas.

**Aplicação AI Hub:** registrar demanda real, correção e resultado operacional antes de ampliar componentes gerados por IA.

**Experimento:** comparar priorização por solicitação de funcionalidade versus dificuldades observadas e corrigidas no trabalho diário, medindo recorrência e economia.

**Riscos/limites:** apenas um caso, sem controle comparativo. **Sem card:** end-user development fora das coleções.

## 8. Today's Work, Tomorrow's Workforce — eficiência imediata pode reduzir aprendizagem

**Fonte:** Diener et al., preprint teórico de 04/11/2026. https://arxiv.org/html/2611.06346v1

**Descoberta/evidência:** modelo matemático dinâmico de delegação à IA sugere que a preferência por resposta pronta pode reduzir prática necessária à aquisição ou manutenção da habilidade humana. Trata-se de resultado de modelagem, não de um experimento longitudinal com trabalhadores reais.

**Mecanismo:** praticar menos uma competência pode comprometer o julgamento futuro sobre a qualidade da própria IA.

**Aplicação:** reservar exercícios sem IA em competências críticas de supervisão de agentes.

**Experimento:** avaliação longitudinal com e sem prática independente, medindo desempenho sem assistência e custo de supervisão.

**Riscos/limites:** conclusões dependem de pressupostos do modelo. **Sem card:** treinamento e organização do trabalho não possuem coleção.

## 9. Whose Transparency? — transparência varia conforme usuário e situação

**Fonte:** Smith et al., preprint de 03/11/2026. https://arxiv.org/abs/2611.05549

**Descoberta/evidência:** pesquisa internacional com 400 pessoas, entre usuários finais, desenvolvedores e formuladores de políticas, acompanhada de grupo focal. Apenas 28% dos usuários finais consideraram que as práticas atuais atendiam muito bem às suas necessidades, versus 53% de desenvolvedores e 49% de formuladores de políticas.

**Mecanismo:** diferentes tarefas ativam necessidades diferentes de explicação, controle e privacidade.

**Aplicação:** transparência sob demanda sobre dados usados, fontes, ações e limites, não apenas um aviso genérico de uso de IA.

**Experimento:** comparar aviso estático versus explicações contextuais por tarefa; medir compreensão e capacidade real de contestação.

**Riscos/limites:** autorrelatos e diferenças de grupos não demonstram que uma interface específica cause melhora. **Sem card:** transparência de agentes não tem coleção válida.

## 10. ProGram — interfaces progressivas em operações longas

**Fonte:** Jung e Jo, preprint de 04/11/2026. https://arxiv.org/html/2611.06188v1

**Descoberta/evidência:** gramática declarativa para sistemas de análise progressiva, com demonstrações de reimplementação e extensão de interfaces existentes; inclui prioridades temporárias, forks e sincronização entre visualizações. Não reporta ganho comercial controlado.

**Mecanismo proposto:** resultados parciais visíveis permitem acompanhar e reorientar cálculos demorados.

**Aplicação AI Hub:** painéis de execução de agentes com etapas parciais verificadas e controle para alterar prioridade ou interromper atividades desnecessárias.

**Experimento:** comparar execução opaca versus execução progressiva, medindo tempo de identificação de erros, decisões e custo final.

**Riscos/limites:** avaliação de expressividade de arquitetura, não experimento com consumidores. **Sem card:** visualização progressiva fora das coleções.

## Síntese de produto e priorização

1. **Marketing Hub / Psique e Íris:** testar perguntas pontuais de expectativa antes de recomendar um conteúdo, preservando escolha de não responder. Distinguir previsão de gosto e propensão de compra.
2. **Apolo e Têmis:** auditar procedência e contexto de vídeos sintéticos; não usar confiança declarada como garantia de autenticidade.
3. **AI Hub:** instrumentar críticas de especialistas, versões de ferramentas e decisões de interfaces com alternativas visíveis; validar efeitos em tarefas reais.

Arquitetura sugerida: **intenção atual → contexto e expectativa opcional → recomendação/geração → proveniência e verificação → ação consentida → resultado humano e comercial observado**.

## Cards para Harness Library (somente candidatos a DRAFT)

Guia vigente lido em `harness-library-api/docs/guia-uso-api-cards.md` na `main`; coleções válidas: `video`, `prazer-audio-visual`, `neuromarketing`, `momentos-de-compra-b2c`.

| cardKey | Coleção | Justificativa |
| --- | --- | --- |
| `previsao-pessoal-preferencia-vs-recomendador` | `neuromarketing` | Evidência de preferências pontuais e recomendação, sem alegar compra. |
| `sobreconfianca-identificacao-videos-ia-compartilhamento` | `video` | Evidência de percepção de vídeos sintéticos e risco de confundir certeza com procedência real. |

Fontes revisadas curtas separadas para cada card. `sourceKind=MARKDOWN`, `publishedOn=2026-11-05`, `validUntil=2026-12-20`; SHA-256 calculado sobre os bytes UTF-8 exatos antes da criação dos JSONs. Os arquivos serão publicados em `pesquisas/design-experiencia/cards/` como candidatos a DRAFT, sem solicitar revisão, ativação ou arquivamento. A API não deve receber POST manual desta rodada; a publicação automática do repositório pode cadastrar os rascunhos após integração à `main`.

**Fontes:** URLs primárias nos dez tópicos; listagem do dia em https://arxiv.org/list/cs.HC/new. Não confundir preprints com revisões por pares.
