# Radar de Design de Experiência — 18 de novembro de 2026

**Rodada:** 18/11/2026, manhã (America/Sao_Paulo)  
**Escopo:** UX, human–AI interaction, design comportamental, atenção, IA multimodal, generative UI, agentes, voz, haptics e experiências digitais.  
**Fonte de descoberta:** [arXiv cs.HC/new, 18/11/2026](https://arxiv.org/list/cs.HC/new): **33 entradas**, sendo **20 novas submissões**. As datas de submissão originais foram verificadas individualmente: alguns trabalhos que aparecem na listagem atual constam no arXiv como submetidos em setembro; não os chamamos de artigos publicados originalmente em novembro.

## Síntese: três achados prioritários

1. **Para recomendação de compra, mais raciocínio pode elevar a confiança verbalizada do LLM sem elevar a acurácia**. A aderência a requisitos verificáveis precisa prevalecer sobre uma resposta convincente. Há um novo card em `momentos-de-compra-b2c`.
2. **Agentes proativos devem separar permissão para observar da permissão para intervir**. É possível permitir percepção multimodal contínua sem autorizar respostas ou ações a toda hora; silêncios do agente também precisam ser explicáveis.
3. **Avaliar interpretação, e não apenas produção textual**. Agentes de pesquisa podem atender a um tema (“crescimento”) e, sem avisar, adotar critérios distintos; interfaces explicativas de IA podem elevar confiança percebida sem prova simultânea de entendimento objetivo.

Selecionamos **dez estudos** com aplicações e limitações distintas dos achados já documentados em 07–17/11. Nenhum resultado externo é tratado como prova de conversão no Marketing Hub.

## 1. Esforço de raciocínio, confiança e erro em agentes de compra (card)

**Descoberta/evidência:** Krishnamoorthy e Han compararam **dez modelos** com esforço baixo a alto em **496 tarefas WebShop, 500 SQID e 330 WANDS**, com dez alternativas por tarefa. Aumentar esforço geralmente não elevou significativamente a taxa de seleção correta, enquanto elevou confiança declarada em **9/10** modelos no WebShop, **5/10** no SQID e **6/10** no WANDS (comparação baixo→alto). Há exceções pontuais. Erros eram compartilhados: no WebShop/baixo esforço, **47 de 493 tarefas** foram erradas por todos os dez modelos. [Preprint submetido em 17/11/2026](https://arxiv.org/html/2611.22033v1).

**Mecanismo possível:** uma deliberação mais extensa pode soar mais segura sem reparar um requisito secundário não observado. Trata-se de interpretação do comportamento de LLMs, **não evidência direta de psicologia do comprador**.

**Implicação:** no Marketing Hub, verificar tamanho, preço, compatibilidade, versão, conteúdo e outras restrições com dados auditáveis; não usar confiança textual do agente como probabilidade calibrada. O consenso de vários LLMs não substitui a checagem do catálogo.

**Experimento:** esforço alto versus esforço convencional com checklist de requisitos e fontes, no mesmo conjunto de produtos; avaliar escolhas incompatíveis por auditoria cega, confiança indevida, tempo/custo e, em piloto real, cancelamentos, checkout e pagamentos.

**Riscos/limites:** benchmark isolou a decisão de busca e navegação, usando uma resposta rotulada e nove distrações próximas; anotações podem ser discutíveis. Não houve compras reais, ROI ou conversão. Mais esforço ajudou em alguns pares; não afirmar inutilidade universal.

**Decisão editorial:** **card** `calibracao-confianca-agentes-compra-llm` em `momentos-de-compra-b2c`, pela relação direta com recomendação correta e restrições no momento de seleção do produto.

## 2. SpotMe: percepção e participação são controles independentes

**Descoberta/evidência:** *Controlling Proactive Multimodal AI: Separating Perception from Participation*, de Khan, Ali e Huang, apresenta um agente que observa tela, fala e interação, mas pode escolher **Silence, Ask ou Assist**. Um estudo inicial com 14 pessoas orientou o projeto; um segundo com 16 pessoas avaliou o protótipo. Participantes podiam manter canais perceptivos disponíveis e restringir a intervenção. A visualização de decisões suprimidas tornou parte do silêncio compreensível e contestável. [Preprint de 17/11/2026](https://arxiv.org/abs/2611.22431).

**Mecanismo comportamental:** disponibilidade de contexto não equivale a consentimento para participação; uma ajuda espontânea tem custo de interrupção e pode alterar a forma como a pessoa usa tela, voz ou seleção.

**Produto:** no AI Hub, modelar separadamente `perceptionScopes` e `participationPolicy`; implementar opções silenciar, perguntar antes, ajudar e histórico auditável de por que o agente não agiu.

**Experimento:** medir interrupções indesejadas, tarefas resolvidas, erros de autorização e sensação de controle contra modo proativo indiscriminado.

**Limite:** amostras pequenas, avaliação em dashboard cívico; não demonstra melhoria de produtividade geral ou segurança de coleta contínua.

**Sem card:** governança de agente multimodal não corresponde às quatro coleções comerciais aceitas.

## 3. O que o agente interpreta quando pedimos “priorize crescimento”?

**Evidência:** *How Web Research Agents Interpret User Priorities* examinou **36 relatórios** de dois agentes de pesquisa em comparações entre Microsoft e Alphabet. Dos 24 com prioridade explícita, **21** trataram do tema, mas os critérios divergiram: crescimento como velocidade de expansão versus crescimento ponderado por previsibilidade e risco gerou preferências diferentes. [Preprint de 17/11/2026](https://arxiv.org/abs/2611.22989).

**Mecanismo:** prioridades abstratas deixam critérios operacionais implícitos e geram interpretações distintas, mesmo com respostas tematicamente pertinentes.

**Produto/hipótese:** apresentar critérios ajustáveis antes de pesquisa ou ranking (crescimento de receita, margem, risco, estabilidade), e registrar por que cada fonte foi usada; testar divergência entre revisores, reprodutibilidade e retrabalho.

**Risco/limite:** apenas duas empresas, dois agentes, pequeno conjunto de prompts; não é avaliação geral de qualidade financeira ou de investimentos.

**Sem card:** planejamento e critérios de agentes de pesquisa estão fora das quatro coleções.

## 4. Explicações multimodais de IA melhoram confiança percebida — não necessariamente a precisão do julgamento

**Evidência:** Truss, Schmitt e Feuerriegel realizaram experimento **preregistrado, N=128**, em sete cenários e cinco métodos XAI. Adicionar explicações textuais geradas por LLM aumentou **entendimento percebido em 0,35 ponto** e **confiança percebida em 0,26 ponto**, ambos em escala de cinco pontos, com p<0,001. O efeito mediado reportado liga as percepções, mas não prova compreensão objetiva. **Submissão original registrada em 25/09/2026**, embora incluído na listagem de 18/11. [Artigo](https://arxiv.org/abs/2611.21638).

**Mecanismo:** transformar visualizações complexas em narrativa verbal acessível pode reduzir esforço interpretativo, mas fluência pode gerar confiança indevida.

**Produto/hipótese:** comparar gráfico isolado, gráfico com explicação automática e gráfico com explicação ancorada em evidências; medir **acerto objetivo**, reconhecimento de limites e confiança calibrada, não apenas SUS e preferência.

**Risco/limite:** cenário experimental, escalas de percepção; não comprova melhora de decisões nem conversão.

**Sem card:** explicabilidade técnica não se enquadra em neuromarketing só porque modifica confiança percebida.

## 5. Conteúdo infantil: mais intensidade audiovisual aparece associada a mais visualizações

**Evidência:** Chu et al. analisaram **16.375 vídeos de 542 canais infantis** (2015–2026), identificando 12 padrões quantificáveis como cortes rápidos, descontinuidade, variação de paleta e picos de volume. A frequência de cortes cresceu nas cinco categorias estudadas. Algumas medidas de estimulação se associaram ao número de visualizações, variando por gênero. O artigo **consta como submetido em 23/09/2026**, apesar de aparecer na listagem de 18/11. [Estudo](https://arxiv.org/html/2611.21637v1).

**Mecanismo interpretativo:** mudanças sensoriais podem direcionar a atenção involuntária, mas métricas do arquivo não medem estado mental da criança, desenvolvimento, vício ou danos.

**Aplicação:** se houver conteúdos destinados a crianças, auditar ritmo, intensidade luminosa e sonora como critérios de adequação antes de otimizar retenção; revisão humana e controle por faixa etária.

**Experimento:** comparar duas edições apropriadas à mesma idade, medir compreensão do conteúdo e conforto em avaliação ética com responsáveis, em vez de apenas visualizações.

**Riscos/limites:** estudo observacional; diferenças de canal, algoritmo e contexto podem explicar visualizações. Não há evidência de que editar com mais estímulos cause aumento de audiência, nem de efeito clínico.

**Sem card:** aplicação restrita a público infantil, não um caso comercial prioritário do Marketing Hub; manter no relatório e não converter correlação em orientação de maximizar estímulos.

## 6. Métricas visuais de engajamento e seleção de notícias

**Evidência:** D'Ignazi et al. analisaram **1.480 escolhas de 370 pessoas**, em quatro assuntos jornalísticos, com e sem contadores visuais de engajamento. Essas pistas melhoraram significativamente o ajuste de modelos de escolha que também controlavam ranking e alinhamento de opinião. [Preprint de 17/11/2026](https://arxiv.org/abs/2611.23079).

**Mecanismo:** números de interação podem funcionar como pistas de relevância ou prova social e competir com posição e preferência, sem demonstrar que o leitor aceitou o conteúdo como verdadeiro.

**Aplicação/experimento:** avaliar se métricas de popularidade exibidas ao lado de conteúdos alteram a seleção e a diversidade de leitura; medir não somente clique, mas compreensão e avaliação de confiabilidade.

**Limite:** notícias políticas em ambiente controlado, não produtos, vendas ou comportamento de compra. **Sem card** por contexto não aderente a consumo B2C.

## 7. Entrevistar humanos antes ou depois de stakeholders sintéticos muda a interpretação do designer

**Evidência:** Mokryn et al. acompanharam **21 estudantes em oito equipes**, em um curso de projeto, com entrevistas de stakeholders simulados e humanos em sequências diferentes. IA primeiro ofereceu um enquadramento geral a ser reconsiderado; humanos primeiro tornaram a IA mais frequentemente instrumento de revisão de observações já ancoradas na realidade. É **associação exploratória** da ordem com os relatos, não experimento causal robusto. [Preprint de 17/11/2026](https://arxiv.org/abs/2611.22740).

**Mecanismo:** ancoragem em primeiras hipóteses pode afetar como evidência posterior é interpretada.

**Produto/hipótese:** no design do Marketing Hub, separar entrevistas reais com clientes da simulação de personas; testar auditoria de quantos requisitos derivam de usuários reais versus IA.

**Limite:** curso universitário, uma instituição, poucas equipes. **Sem card**: método de pesquisa UX, não um mecanismo específico de compra.

## 8. AI4OSN: copilot de análise social com controle humano explícito

**Evidência:** Wang et al. avaliaram AI4OSN com **18 participantes**; SUS médio **79,7** e baixa carga cognitiva percebida. Participantes iniciantes demonstraram, de forma descritiva, maior concordância com saídas da IA que usuários experientes. [Preprint de 16/11/2026](https://arxiv.org/abs/2611.22006).

**Mecanismo:** sugestões prontas podem reduzir esforço de anotação e simultaneamente incentivar deferência excessiva, especialmente em usuários com pouco repertório para contestá-las.

**Produto/experimento:** exibir critérios de rotulagem, amostras contraditórias e override humano; medir erros de classificação confirmados por especialistas, e não somente confiança.

**Limite:** pesquisa pequena e domínio de análise de redes sociais. **Sem card** por falta de coleção de engenharia de UX/Agentes.

## 9. Validadores de acessibilidade de PDF não detectam todos os defeitos

**Evidência:** PDFa11yMut gerou **23 documentos mutantes** com falhas estruturais confirmadas a partir de cinco PDFs inicialmente aprovados. PAC detectou **4/23 (17,4%)**, Acrobat e veraPDF **5/23 (21,7%)**. Falhas de ordem de leitura e de estrutura assistiva passaram despercebidas. **Submissão original: 15/09/2026**. [Preprint](https://arxiv.org/abs/2611.21628).

**Mecanismo técnico/UX:** validação automatizada pode inspecionar algumas regras estruturais sem reproduzir a experiência de quem usa tecnologia assistiva.

**Produto/hipótese:** combinar verificação automática com teste real de navegação por leitor de tela nos materiais exportados; comparar defeitos reais identificados.

**Limite:** conjunto de 23 falhas artificiais e três ferramentas; não é taxa global de falhas dos validadores. **Sem card** por falta de coleção.

## 10. RelicTouch: vibração dependente do movimento para interpretar imagens

**Evidência:** estudo intraindivíduo com **20 participantes** testou vibração de ponta do dedo, espaçamento e configuração; entrevistas exploratórias com **oito especialistas** complementaram o projeto. Em determinadas condições, exploração com um dedo recebeu avaliações subjetivas superiores às de múltiplos dedos. [Estudo listado em 18/11](https://arxiv.org/abs/2611.21642).

**Mecanismo interpretativo:** diferenças perceptíveis de vibração acopladas ao deslocamento podem fornecer uma representação tátil adicional de bordas e variações luminosas.

**Produto/experimento:** testar protótipos hápticos de exploração de gráficos para acessibilidade, medindo identificação correta, esforço e erros versus controles convencionais.

**Limites:** dispositivo especializado, participantes e tarefas restritas; não prova prazer audiovisual nem aumento de vendas. **Sem card** por aderência insuficiente.

## Experimentos priorizados

- **Prioridade A — Marketing Hub:** checklist verificável de requisitos para agentes de compra; comparar custo por seleção correta e confiança calibrada, sem execução de pagamento sem autorização humana.
- **Prioridade B — AI Hub:** separar escopos de percepção de níveis de participação do agente e disponibilizar histórico das decisões de permanecer silencioso.
- **Prioridade C — UX de agentes:** transformar critérios implícitos de pesquisa e ranking em campos editáveis e rastreáveis; medir reprodutibilidade e justificativas ancoradas em fontes.
- **Prioridade D — audiovisual:** usar métricas de ritmo e intensidade apenas como sinais para revisão contextual, não como incentivo a hiperestimulação.

## Governança dos cards e publicação

Versão vigente do guia consultada na branch `main`: `harness-library-api/docs/guia-uso-api-cards.md`. Coleções permitidas: `video`, `prazer-audio-visual`, `neuromarketing`, `momentos-de-compra-b2c`.

**Um card novo:** `calibracao-confianca-agentes-compra-llm`, coleção `momentos-de-compra-b2c`, porque é diretamente relacionado à qualidade de recomendações de produtos na etapa de escolha. Fonte Markdown revista: `pesquisas/design-experiencia/cards/fontes/2026-11-18-calibracao-confianca-agentes-compras.md`, criada antes do JSON. SHA-256 exato: `f8f69213e9bfb4c10ecb4ac268f3f57f952292c0a3bcf837e16973b0c65c9367`. JSON: `pesquisas/design-experiencia/cards/2026-11-18-confianca-calibrada-agentes-compras.json`. `publishedOn=2026-11-18`, `validUntil=2027-01-02`, `sourceKind=MARKDOWN`; contrato de 16 campos sem extras.

**Achados fortes sem card por falta de coleção:** agentes multimodais proativos, critérios de agentes de pesquisa, explicabilidade/XAI e supervisão humana de pesquisa de redes; também acessibilidade PDF e haptics. O estudo de vídeos infantis permaneceu no relatório por aplicação comercial restrita e risco de transformar associação observacional em prescrição de estimulação. Não foi inventada coleção `design-experiencia`.

Arquivos representam **candidatos a DRAFT**. Não enviar à revisão, ativar ou arquivar. O registro efetivo na API via workflow deve ser confirmado separadamente após a integração, sem presumi-lo pelo merge.
