# Radar Design de Experiência — 2026-11-04

**Experience Engine v65 | Marketing Hub**  
**Data da rodada:** 04/11/2026, horário de Brasília.  
**Critério editorial:** novos estudos de Human-Computer Interaction presentes na listagem do arXiv de 04/11/2026 (submetidos sobretudo em 02–03/11), sem repetir os estudos selecionados em 03/11. Pré-publicações não são automaticamente artigos revisados por pares.

## Síntese executiva

A descoberta central é que **mais explicações e mais opções não necessariamente produzem melhor experiência**. Na música, contexto verbal específico pode abrir interesse por conteúdo novo. Em alertas urgentes, explicar apenas o necessário pode permitir respostas mais rápidas. Em sistemas com agentes, expor decisões alternativas e checkpoints reparáveis pode reduzir dependência acrítica e retrabalho.

Foram selecionados dois candidatos a DRAFT nas coleções **prazer-audio-visual** e **video**. Não foram criados cards artificiais para governança de agentes, UX colaborativa ou haptics, que não possuem coleção própria no contrato vigente.

## 1. Setting the Stage — introduções faladas podem renovar a descoberta musical

**Fonte:** Brett Binst, submetido em 03/11/2026. https://arxiv.org/html/2611.04570v1

**Descoberta e evidência:** em experimento intraindivíduo randomizado com 452 pessoas após exclusões, cada participante ouviu três músicas, com introdução informativa, introdução imersiva ou sem introdução. Foram avaliadas descobertas fora e dentro das preferências musicais usuais. Em relação ao controle, as razões de chances (OR) para descoberta fora do repertório foram 1,73 (imersiva; CrI95% 1,13–2,65) e 2,61 (informativa; 1,70–4,07). Para redescoberta dentro do repertório, OR 3,23 (1,87–5,83) e 8,36 (4,51–16,64).

**Mecanismo:** transporte narrativo e elaboração cognitiva são caminhos interpretativos plausíveis. Os modelos de mediação associaram o primeiro à exploração fora das preferências e o segundo à redescoberta de músicas próximas. Não foram isolados experimentalmente.

**Implicação de produto:** para experiências musicais, pode fazer sentido preparar o ouvinte com uma contextualização falada curta e opcional, especialmente quando seu interesse inicial é baixo.

**Hipótese/experimento:** comparar prévia musical simples, contexto informativo verificável e contexto narrativo, medindo skip, escuta voluntária, interesse e retorno. Em anúncios, medir separadamente eventos comerciais reais.

**Riscos/limites:** 18 músicas selecionadas, escuta experimental obrigatória, autorrelato e ausência de compra, retenção longa ou medição neural de prazer. Exigir licença de áudio e correção factual. **Card:** prazer-audio-visual.

## 2. OmniScribble — geração de animação com checkpoints corrigíveis

**Fonte:** Trinh, To e Le, submetido em 03/11/2026. https://arxiv.org/html/2611.04284v1

**Descoberta e evidência:** protótipo de animação de esboços com decomposição de objetos e trajetórias editáveis. Em comparação intraindivíduo com oito participantes, a mediana de autoria foi 232,68 segundos manual versus 71,68 segundos assistida; redução mediana por participante de 71,44% (p=0,0078). Dos 32 checkpoints, 27 foram aceitos e cinco exigiram intervenção.

**Mecanismo:** tornar decisões de IA visíveis e reparáveis facilita a divisão dinâmica de trabalho entre pessoa e modelo; o mecanismo dos checkpoints não foi isolado contra geração end-to-end opaca.

**Implicação:** para Apolo, investigar cenas curtas com objetos, trajetória, prévia, correção local e renderização reversível.

**Hipótese/experimento:** comparar edição manual, geração integral e checkpoints com controle de objetos. Medir tempo até vídeo aprovado, renderização, qualidade cega, revisões e custo.

**Riscos/limites:** N=8, tarefas simples, sem benchmark end-to-end; a métrica publicada exclui o tempo de renderização e reprodução. Não extrapolar o ganho para vídeos publicitários. **Card:** video.

## 3. Must AI Spell It Out? — alertas mais curtos podem funcionar melhor

**Fonte:** Das, Demberg e Feit, submetido em 02/11/2026. https://arxiv.org/abs/2611.03976

**Descoberta/evidência:** estudo pré-registrado com 114 participantes comparou avisos pragmáticos, que consideram informação já conhecida pelo usuário, com mensagens totalmente explícitas. As pessoas resolveram alertas incompletos quase tão corretamente, porém mais cedo; também identificaram mais problemas e perceberam mais utilidade sem aumento de demanda mental relatada.

**Mecanismo:** aproveitar conhecimento prévio reduz redundância e preserva atenção para próximas ocorrências.

**Aplicação:** dashboards operacionais e alertas do AI Hub podem priorizar alteração, consequência e ação necessária, mantendo acesso a detalhes completos.

**Experimento:** comparar alertas completos com alertas contextualizados; medir acertos, falhas críticas, tempo e revisão.

**Riscos/limites:** informações omitidas podem ser decisivas se o sistema presumir erroneamente o que o usuário sabe; teste restrito a monitoramento simulado. **Sem card:** não há coleção para UX de alertas operacionais.

## 4. Agentic Path Maps — visualizar caminhos rejeitados reduz dependência acrítica

**Fonte:** Grunde-McLaughlin et al., submetido em 03/11/2026. https://arxiv.org/abs/2611.04183

**Descoberta/evidência:** estudo com 48 pessoas em análise de dados observou que usuários raramente imaginam alternativas não apresentadas. Mostrar caminhos que o agente não escolheu reduziu sobredependência no resultado da IA.

**Mecanismo:** a interface torna explícitas decisões implícitas e favorece comparação com preferências do usuário.

**Aplicação:** tela de resultados do AI Hub com alternativa escolhida, duas alternativas rejeitadas, diferenças e possibilidade de retomar a decisão.

**Experimento:** mostrar apenas a resposta versus resposta com caminhos alternativos; medir erros aceitos, decisões recuperadas e carga cognitiva.

**Riscos/limites:** excesso de alternativas pode distrair; não comprova ganhos fora da análise de dados. **Sem card:** UX de agentes não é coleção aceita.

## 5. Vibetrace — rastros interativos e skills podem reduzir custo dos agentes de código

**Fonte:** Yan et al., submetido em 03/11/2026. https://arxiv.org/html/2611.04466v1

**Descoberta/evidência:** o sistema permite observar execuções, intervir em pontos anteriores e transformar correções em skills. Em 20 tarefas retidas do SWE-bench Verified com cinco repetições, o modo com feedback elevou pass rate médio de 65% para 68%, reduziu tokens de entrada em 23% e custo em 15,5% versus baseline.

**Mecanismo:** transformar experiência verificada em orientação reutilizável e intervir sem descartar trechos válidos da execução.

**Aplicação:** experimentar forks de execução e checkpoints verificáveis no AI Hub, mantendo testes de regressão e versionamento de skills.

**Experimento:** comparar logs simples e rastros navegáveis com recuperação parcial; medir correção, custo total, falhas e tempo humano.

**Riscos/limites:** apenas 20 tarefas retidas na avaliação quantitativa, feedback simulado em parte do experimento e dependência de modelo/harness específicos. **Sem card:** governança de agentes não possui coleção própria.

## 6. TRACE — memórias podem alterar fontes citadas mesmo com resultados de busca constantes

**Fonte:** Nick Hagar, submetido em 02/11/2026. https://arxiv.org/abs/2611.03939

**Descoberta/evidência:** auditoria controlada mostrou que uma memória declarando preferência por um publicador elevou em 33–39 pontos percentuais sua presença entre as três primeiras fontes citadas. Uma memória de desconfiança de mídia convencional esteve associada a acréscimo de 6,3 pontos percentuais em citações de fontes com desinformação ou teorias conspiratórias, com heterogeneidade por modelo.

**Mecanismo:** o contexto persistente do usuário pode enviesar a busca e seleção de evidências mesmo sem alterar a pergunta original.

**Aplicação:** na pesquisa automática do Marketing Hub, separar preferências de estilo de critérios de qualidade de fonte; auditar variação de fontes quando memórias mudam.

**Experimento:** executar tarefas de pesquisa com memórias neutras, preferências irrelevantes e memórias conflitantes, comparando diversidade, qualidade e verificabilidade.

**Riscos/limites:** estudo no contexto de notícias eleitorais dos EUA; não é medição de persuasão comercial nem de todos os agentes. **Sem card:** auditoria de agentes não se encaixa nas coleções.

## 7. CrowdForge — a influência precisa ser perceptível em experiências audiovisuais coletivas

**Fonte:** Hopkins et al., submetido em 02/11/2026. https://arxiv.org/html/2611.03389v1

**Descoberta/evidência:** duas performances com 19 espectadores no total (14 questionários, 17 registros comportamentais) compararam escolhas de áudio, visuais e ambas. Os participantes interagiram mais com parâmetros cujas consequências percebiam, como andamento musical e mudanças visíveis de personagens. Elementos sonoros finos foram mais ignorados; a combinação de áudio e visual nem sempre aumentou presença social.

**Mecanismo:** observabilidade dos efeitos escolhidos sustenta a sensação de agência, mais que a quantidade de controles.

**Aplicação:** em experiências criativas interativas, mostrar feedback imediato sobre mudanças de ritmo e aparência sem oferecer parâmetros inúteis.

**Experimento:** comparar muitos parâmetros com poucos parâmetros de efeito claramente visível/audível; medir satisfação, participação efetiva e abandono.

**Riscos/limites:** amostra pequena, contexto de concerto experimental, sem prova de prazer audiovisual generalizável ou receita. **Sem card:** apesar de aderir tematicamente a prazer-audio-visual, a evidência é inicial e o ganho incremental sobre o card musical é insuficiente nesta rodada.

## 8. SEAM — memórias associativas compartilhadas podem criar descobertas sociais

**Fonte:** Wang et al., submetido em 03/11/2026. https://arxiv.org/abs/2611.04296

**Descoberta/evidência:** protótipo de memória comunitária usado por 46 pessoas durante duas semanas, seguido de workshop com 14; membros compartilharam acontecimentos, consultaram memória e instruíram o sistema a acompanhar rotinas e executar jogos.

**Mecanismo:** memória externa coletiva pode trazer conexões relevantes sem requerer busca manual constante.

**Aplicação:** explorar memória de projetos no AI Hub baseada em ideias e evidências, com controle de acesso por equipe e atribuição de origem.

**Experimento:** comparar pesquisa documental tradicional com recuperação associativa; medir descobertas corretas, referências verificáveis e vazamentos.

**Riscos/limites:** comunidade co-localizada, uso breve e proteção de dados compartilhados ainda a validar. **Sem card:** não há coleção de memória de agentes.

## 9. Just Between Us? — voz discreta e gestos podem coexistir com interação coletiva

**Fonte:** Nilgar, Dietrich e Van Laerhoven, submetido em 03/11/2026. https://arxiv.org/abs/2611.04692

**Descoberta/evidência:** em 20 duplas (40 participantes) jogando com agente simulado por pesquisador, um canal por áudio intra-auricular e inclinação da cabeça funcionou conforme planejado em 39 de 40 turnos de comunicação. Houve percepções variadas de justiça entre quem tinha e quem não tinha acesso à informação.

**Mecanismo:** uma modalidade privada pode reduzir exposição pública, mas também alterar assimetrias sociais de informação.

**Aplicação:** dispositivos vestíveis e assistentes contextuais devem oferecer controles de discrição e clareza sobre quem recebe quais dados.

**Experimento:** testar comunicação pública versus privada em tarefas colaborativas com consentimento explícito.

**Riscos/limites:** protótipo Wizard-of-Oz em jogo, não um produto autônomo de produção. **Sem card:** interação vestível não tem coleção válida.

## 10. Selfrionette — haptics por força nos dedos

**Fonte:** Hashimoto e Hirao, artigo de UIST 2024 disponibilizado no arXiv em 02/11/2026. https://arxiv.org/abs/2611.03052

**Descoberta/evidência:** controlador de força dos dedos para manipular avatares VR e perceber propriedades táteis, como peso e atrito. Os autores relatam realismo comparável ao rastreamento corporal nos cenários de interação testados.

**Mecanismo:** resistência física controlada oferece pistas somatossensoriais para atributos virtuais.

**Aplicação:** acompanhar para interfaces imersivas, sem tratar como solução pronta para mobile ou vídeo.

**Experimento:** comparar sensação de controle e esforço em tarefas de manipulação com e sem feedback de força.

**Riscos/limites:** requer hardware especializado; é artigo de 2024 indexado no arXiv em 2026, e não uma descoberta experimental desta semana. **Sem card:** coleção haptics inexistente.

## Priorização para os produtos

1. **Apolo / audiovisual:** testar checkpoints editáveis em animações simples e introduções faladas apenas quando acrescentarem contexto real.
2. **AI Hub / agentes:** protótipo de rastros navegáveis e alternativas não exploradas, com critérios de aprovação no backend.
3. **Radares de pesquisa:** auditar seleção de fontes contra efeitos indesejados de memórias e preferências persistentes.

## Cards candidatos a DRAFT

Guia consultado na branch main: `harness-library-api/docs/guia-uso-api-cards.md`. Coleções permitidas: `video`, `prazer-audio-visual`, `neuromarketing`, `momentos-de-compra-b2c`.

| cardKey | Coleção | Razão |
| --- | --- | --- |
| `introducoes-faladas-descoberta-musical-serendipidade` | `prazer-audio-visual` | Pesquisa experimental sobre interesse e descoberta na escuta, sem extrapolar a prazer neural ou vendas. |
| `animacao-esbocos-checkpoints-reparaveis` | `video` | Fluxo de autoria de animação audiovisual com controle e correções locais mensuráveis, mas evidência preliminar. |

As fontes revisadas individuais foram geradas antes dos JSONs. O `sourceSha256` de cada card corresponde ao texto UTF-8 exato da fonte versionada. As datas são `publishedOn=2026-11-04` e `validUntil=2026-12-19`. Os arquivos são candidatos para cadastro automático como DRAFT no workflow do repositório; nenhuma revisão, ativação ou arquivamento foi solicitado.

**Fontes:** todos os links acima levam à fonte primária. A listagem de referência é https://arxiv.org/list/cs.HC/new. A publicação de artigos no arXiv não implica revisão por pares.
