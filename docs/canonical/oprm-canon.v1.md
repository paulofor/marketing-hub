# OPRM Canon v1 — Ingestão de CNAE e Totalização de Market Size

## Propósito

Este documento define regras canônicas específicas do módulo OPRM para a ingestão CNPJ/CNAE, consolidação de market size por CNAE, pesquisa inicial NichoCNAE de realidade operacional e construção de público-alvo MEI/autônomo quando a origem for levantamento MEI.

## Objetivo

Evitar fechamento prematuro de `import run` que destrói a totalização de market size por CNAE.

## Regra obrigatória — fechamento de run de importação

- É proibido finalizar (`completeRun`/`finalize-latest-started`) uma run OPRM CNPJ/CNAE quando existir ao menos um arquivo de dataset `ESTABELECIMENTOS` em `STARTED`.
- O endpoint `POST /api/oprm/market/import-runs/{runId}/complete` só pode ser chamado após a leitura de **todos** os arquivos da run (todos os arquivos previstos com evento terminal `COMPLETED` ou `FAILED`, sem `STARTED` remanescente).
- É proibida chamada antecipada de `completeRun` (antes de terminar a leitura dos arquivos), mesmo que parte dos arquivos já tenha sido processada.
- Nessa condição, o backend deve bloquear a finalização com erro de conflito (`HTTP 409`) e mensagem explícita de causa-raiz operacional.
- A run deve permanecer aberta para permitir conclusão correta da consolidação de `marketSizes`.

## Critério de efetividade — fechamento de run

- Só é permitido fechamento quando não houver `ESTABELECIMENTOS` em `STARTED`.
- Se houver falha real em arquivos de estabelecimentos, ela deve estar explícita como `FAILED` por evento de arquivo, com erro rastreável, e não por fechamento automático sem execução.

## Regra obrigatória — identificação e consolidação de CNAE

- A identificação de CNAE em `Estabelecimentos*.zip` deve usar o campo de CNAE principal (posição `11`, índice zero-based no split por `;`).
- O CNAE deve ser normalizado para dígitos antes da agregação.
- Linhas sem colunas mínimas esperadas ou sem CNAE principal devem ser contabilizadas como ignoradas e registradas em log.

## Critério de efetividade — identificação de CNAE

- Cada arquivo `ESTABELECIMENTOS` deve gerar log de início, progresso periódico e resumo final com: `linhasLidas`, `linhasValidas`, `linhasIgnoradas` e quantidade de CNAEs consolidados.
- A ausência desses logs invalida a rastreabilidade operacional da totalização e deve ser tratada como não conformidade canônica.

## 🚨 MUITO IMPORTANTE — processamento de arquivos grandes (`Estabelecimentos*.zip`)

- É proibido processar `Estabelecimentos*.zip` carregando conteúdo integral em memória com abordagens equivalentes a `readAllBytes()`/`String` única do arquivo inteiro.
- O processamento de `Estabelecimentos*.zip` deve ser obrigatoriamente em **streaming** (leitura incremental por `ZipEntry` e por linha), mantendo uso de memória previsível.
- A totalização por CNAE (`marketSizes`) deve ser incremental durante a leitura, com agregação em estrutura compacta (mapa por CNAE + contadores), sem materializar todas as linhas.
- O vínculo `cnpjBase -> cnaePrincipal` usado para cruzar `SIMPLES` com `ESTABELECIMENTOS` não pode ser materializado como mapa global de todos os estabelecimentos; deve ser construído e consumido em partições/blocos menores, liberando memória entre blocos.
- Deve existir mecanismo de **checkpoint/progresso** por arquivo para permitir retomada segura após falhas, evitando reprocessamento integral silencioso.
- Em caso de falha por capacidade (ex.: `OutOfMemoryError`), o arquivo deve ser registrado como `FAILED` com causa-raiz explícita no erro operacional e logs com contexto (`runId`, `fileId`, `datasetType`, etapa da leitura).
- A finalização da run (`completeRun`) permanece bloqueada enquanto houver arquivo não terminal; é proibido mascarar falha de leitura grande com fechamento prematuro.

## Critério de efetividade — arquivos grandes

- Cada execução de `ESTABELECIMENTOS` deve registrar, no mínimo:
  - início da leitura da `ZipEntry` com identificador da entry;
  - progresso periódico por volume (ex.: a cada N linhas);
  - resumo final com `linhasLidas`, `linhasValidas`, `linhasIgnoradas`, total de CNAEs agregados e duração;
  - confirmação explícita de publicação/persistência do `marketSizes` do arquivo.
- Se qualquer item acima estiver ausente, a execução deve ser tratada como observabilidade insuficiente para operação de produção.


## Regra obrigatória — pesquisa inicial NichoCNAE de realidade operacional

- A pesquisa inicial do pipeline OPRM NichoCNAE deve operar sempre no modo `ROUTINE_REALITY_RESEARCH`.
- O CNAE deve ser tratado como público amplo demais para oferta e comunicação de alta assertividade; ele é apenas ponto de partida estatístico, fonte de volume e trilha de auditoria.
- O objetivo primário do fluxo NichoCNAE, quando a divulgação prevista for Instagram/Meta Ads, é transformar o CNAE amplo em um pacote de público comportamental anunciável, com desejo amplo, situação de trabalho reconhecível no feed, linguagem simples, sinais de rotina e contexto suficiente para alimentar o pipeline posterior de hipótese. O subnicho operacional continua sendo insumo de contexto, mas não deve virar segmentação estreita nem depender de dor hiper-específica para avançar.
- A pesquisa na internet existe para entender a realidade concreta desse subnicho antes de qualquer tese de produto: tarefas, canais, aquisição de clientes, preço/cobrança, dúvidas, dificuldades, vocabulário, recorrência, evidências públicas e sinais de vida operacional no Brasil.
- O objetivo desse modo é definir um nicho/subnicho operacional suficientemente claro a partir do CNAE, com recorte de público executor, contexto de atuação, rotina observável, canais de aquisição/atendimento, recorrência e sinais públicos mínimos que sirvam como insumo para a próxima fase.
- A pesquisa deve parar na definição qualificada do nicho e na descrição realista da rotina/contexto operacional; ela não deve aprofundar tese de dor, hipótese, mecanismo, produto, campanha, oferta, promessa comercial, landing page ou tese de experimento. Dor, mecanismo e demais aspectos comerciais serão tratados em pipeline posterior próprio.
- Termos de solução, como IA, automação, sistema, app, software, ferramenta, curso ou marketing digital, não podem direcionar a pesquisa inicial quando vierem apenas de enquadramento comercial do nome do candidato.
- O nome operacional usado para pesquisa deve representar o nicho/CNAE de forma neutra; nomes originais contaminados por linguagem de solução devem ser preservados apenas para auditoria e não como fonte de direcionamento da busca.
- A etapa inicial deve priorizar evidências públicas sobre execução do trabalho, problemas operacionais, dúvidas reais, atividades, responsabilidades, limitações, sazonalidade, riscos, custos, retrabalho, gargalos e vocabulário usado pelo próprio mercado brasileiro.
- A aquisição de clientes deve ser pesquisada como eixo obrigatório da realidade operacional do profissional, buscando evidências de captação de clientes, canais usados, indicação, redes sociais, WhatsApp, orçamento, agenda vazia, retorno, fidelização, cancelamento, reativação e recorrência.
- Aquisição de clientes nessa fase significa comportamento observado do MEI/autônomo para manter a operação funcionando; é proibido tratá-la como recomendação de marketing, criação de campanha, oferta, funil, anúncio ou estratégia comercial prescritiva.
- Antes de gerar o seed final, a etapa `oprmNicheResearchSeedBuilder` deve quebrar CNAEs amplos em 3 a 7 subnichos operacionais focados em executor MEI/autônomo, usando CNAE, descrição, volume MEI, score OPRM e a lista de subnichos já materializados para o mesmo CNAE. Cada subnicho deve ser comparado pela clareza do público executor, especificidade do contexto, recorrência observável da rotina, facilidade de localizar evidências públicas, separação de empresa estruturada, potencial como insumo para pipeline posterior e distância mercadológica dos subnichos já existentes; as etapas profundas seguintes devem pesquisar apenas o subnicho mais promissor e ainda não explorado.
- A etapa `oprmNicheResearchSeedBuilder` deve aceitar somente objetivos de query compatíveis com rotina real: `ROUTINE_DISCOVERY`, `ROUTINE_TASK_DISCOVERY`, `OPERATIONAL_DIFFICULTY_DISCOVERY`, `NICHE_OWNER_QUESTION_DISCOVERY`, `FINAL_CUSTOMER_QUESTION_DISCOVERY`, `LANGUAGE_DISCOVERY` e `OPERATIONAL_CONTEXT_DISCOVERY`.
- Decisão operacional vigente: o CNAE é apenas fonte inicial de descoberta e auditoria; o pipeline não deve criar nem materializar o nicho amplo quando houver pesquisa NichoCNAE. A etapa `oprmNicheResearchSeedBuilder` deve escolher um subnicho específico vencedor, com público executor, contexto operacional, canais/rotina observáveis e fronteiras claras do recorte; esse subnicho deve substituir o nome neutro do ciclo para todas as etapas profundas posteriores.
- O subnicho vencedor deve ser escolhido por critérios de definição de nicho: clareza da pessoa executora, especificidade do trabalho, contexto operacional brasileiro, recorrência observável, capacidade de diferenciar de empresa estruturada, disponibilidade de evidências públicas, distância de subnichos já materializados no mesmo CNAE e utilidade como insumo para pipeline posterior de dor/hipótese. Se a IA retornar apenas o CNAE amplo, uma variação genérica do CNAE, um nome curto demais para representar público/contexto/rotina ou um recorte igual/semanticamente próximo de subnicho já existente para o CNAE, a etapa deve ser tratada como inválida em vez de permitir nova materialização repetida.
- Antes de persistir queries para busca, coleta e extração profundas, o backend não deve executar pré-gate determinístico de conteúdo nem bloquear fluxo por avaliação semântica do seed retornado. O executor externo e os gates próprios do fluxo decidem qualidade, reprovação, reprocessamento e próximo movimento; o backend deve apenas validar contrato técnico persistível, aplicar defaults rastreáveis quando necessário, gravar o payload recebido e expor pendências/status/resultados.
- As queries e buscas do pipeline OPRM NichoCNAE devem ser Brasil-first: português do Brasil, mercado brasileiro, preferência por fontes brasileiras, domínios `.br`, instituições brasileiras, entidades setoriais brasileiras, notícias/fóruns brasileiros e páginas que descrevam a rotina do nicho no Brasil.
- A etapa inicial deve priorizar evidências públicas sobre execução do trabalho, operação comercial cotidiana, problemas operacionais, dúvidas reais, atividades, responsabilidades, limitações, sazonalidade, riscos, custos, retrabalho, gargalos e vocabulário usado pelo próprio mercado brasileiro.
- A etapa `oprmNicheResearchSeedBuilder` deve gerar famílias de queries sobre: aquisição de clientes por WhatsApp/Instagram/indicação; agenda, faltas, remarcações e clientes que somem; precificação, cobrança, pacotes e recorrência; materiais, tempo de atendimento e retrabalho; relatos reais de profissionais autônomos em fóruns, vídeos, comentários e perguntas frequentes.
- A etapa `oprmNicheResearchSeedBuilder` e a etapa `oprmSourceSearcher` devem privilegiar evidências de rotina manual do profissional, atendimento real, aquisição/fidelização/recorrência de clientes, dores práticas e emocionais, tarefas concretas do executor e linguagem usada pelo próprio profissional.
- Fontes que vendem solução, como apps, softwares, automações, cursos, templates, sistemas, ferramentas, funis, campanhas ou landing pages, devem ser penalizadas/rebaixadas na seleção e classificadas como risco quando não trouxerem evidência concreta da execução manual do trabalho.
- A etapa `oprmSourceSearcher` deve separar fonte de rotina de fonte de solução: fonte de rotina descreve execução real, tarefas, dificuldades, perguntas, aquisição, agenda, cobrança e linguagem do MEI/autônomo; fonte de solução vende ou promove software, app, ferramenta, curso, automação, funil, campanha, template, landing page, produto ou promessa.
- Somente fonte de rotina pode avançar para `oprmSourceFetcher`, snapshot curto, extração e síntese principal. Fonte de solução deve ser preservada como `CONTAMINATION_RISK` para auditoria e nunca pode virar evidência positiva da dor, do mecanismo ou da oportunidade nesta fase.
- O gate de qualidade do NichoCNAE deve medir qualidade de definição do nicho, não validação profunda de dor vendável. O ciclo deve ser aprovado quando houver público executor claro, contexto operacional específico, rotina/canais/recorrência observáveis, evidências públicas suficientes e ausência de contaminação por solução; validação profunda de dor, urgência, impacto, mecanismo e hipótese pertence ao pipeline posterior.
- Na v2 originada do levantamento MEI, quando o canal de divulgação for Instagram, o fluxo foca obrigatoriamente MEI/autônomo/dono-operador brasileiro em recortes amplos e anunciáveis: a etapa `candidate-generator` deve gerar preferencialmente 8 a 12 candidatos por desejo amplo de feed e situação comportamental, como ganhar dinheiro trabalhando por conta própria, conseguir clientes pelo WhatsApp/Instagram, agenda vazia, preço/cobrança, profissionalização do atendimento, depender menos de plataforma/intermediário e organizar rotina autônoma. O CNAE deve entrar como contexto de linguagem e exemplos, não como trava de microsegmentação. A etapa `candidate-tournament` deve selecionar finalistas pela amplitude anunciável, clareza do desejo, facilidade de criativo, aderência a MEI/autônomo e potencial de teste no Instagram, sem exigir dor específica validada; ausência de dor hiper-específica não deve encerrar o job nessa fase.
- A aquisição de clientes deve ser pesquisada como eixo obrigatório da realidade operacional do profissional, buscando evidências de captação de clientes, canais usados, indicação, redes sociais, WhatsApp, Instagram, orçamento, agenda vazia, retorno, fidelização, cancelamento, reativação e recorrência.
- Aquisição de clientes, preço e cobrança nessa fase significam comportamento observado do MEI/autônomo para manter a operação funcionando; é proibido tratá-los como recomendação de marketing, criação de campanha, oferta, funil, anúncio ou estratégia comercial prescritiva.
- A etapa `oprmNicheResearchSeedBuilder` deve aceitar objetivos de query compatíveis com rotina real e operação comercial cotidiana, incluindo aquisição de clientes, agenda/faltas/remarcações, precificação/cobrança/pacotes/recorrência, materiais/tempo/retrabalho, relatos reais, perguntas recorrentes, linguagem e contexto operacional.
- As queries e buscas do pipeline OPRM NichoCNAE devem ser Brasil-first: português do Brasil, mercado brasileiro, preferência por fontes brasileiras, comunidades, vídeos, comentários, fóruns brasileiros e páginas que descrevam a rotina do nicho no Brasil. CBO, tabelas salariais, páginas institucionais e descrições oficiais devem ser apoio secundário, não o eixo dominante da pesquisa.
- Quando a pesquisa cair em `NEEDS_MORE_RESEARCH` ou `GENERIC`, a UI deve oferecer saída operacional simples para o usuário reexecutar etapas do mesmo job ou iniciar uma nova pesquisa manual completa do CNAE quando a intenção for criar outro subnicho, sem avançar material fraco para hipótese/oferta.
- Quando o mesmo job for reaberto após reprovação do gate de qualidade, a etapa `oprmNicheResearchSeedBuilder` deve receber automaticamente o status anterior, `proximoMovimentoCodigo`, `proximoMovimento` e notas compactas do gate anterior para mudar subnicho, queries e estratégia de fontes, evitando repetir a mesma causa dominante de reprovação.
- A etapa `oprmNicheResearchSeedBuilder` deve rejeitar objetivos e termos que direcionem busca por produto, oferta, campanha ou solução quando não fizerem parte literal da descrição CNAE.
- A etapa `oprmSourceFetcher` deve propagar para o snapshot curto a classificação da fonte definida na busca: `sourceIntent`, `routineEvidenceScore`, `commercialPageRisk` e `solutionLanguageRisk`, sem armazenar HTML completo.

## Regra obrigatória — NichoCNAE v2 orientado a Instagram

- A divulgação padrão considerada para os candidatos NichoCNAE v2 é Instagram/Meta Ads; portanto, o fluxo deve evitar públicos pequenos demais, dores difíceis de localizar por interesse e recortes que dependam de segmentação manual hiper-específica.
- A pesquisa deve priorizar mercados amplos por comportamento e desejo reconhecível: renda extra, trabalho por conta própria, conseguir clientes, agenda vazia, cobrança/preço, WhatsApp/Instagram, indicação local, profissionalização do atendimento, medo de ficar sem cliente e organização da rotina.
- O material produzido pelo NichoCNAE v2 é insumo do pipeline posterior de hipótese (`dor -> resultado -> oferta`): público, rotina, linguagem, sinais de aquisição/cobrança e limites de evidência. A v2 não deve decidir dor principal, resultado prometido, mecanismo, oferta, campanha ou landing.
- O criativo, a promessa e a landing devem funcionar como filtro de público no pipeline posterior. O papel do OPRM é entregar ângulos comportamentais amplos com exemplos do CNAE, não tentar provar uma microdor rara antes do primeiro teste pago.
- A etapa de query planner não deve encerrar por `NO_RESEARCH_GAIN` apenas porque não há gap específico inicial; quando houver candidatos amplos sem gaps, deve gerar buscas amplas de Instagram/WhatsApp/aquisição/cobrança para criar sinais iniciais de linguagem e desejo.
- A falha controlada por ciclo continua válida, mas a recomendação padrão deve ser abrir o recorte, trocar o desejo central ou encaminhar o material como insumo incompleto para decisão humana, em vez de procurar dor cada vez mais específica ou criar oferta dentro da v2.
- As etapas `commercial-evidence-gate` e `enriched-niche-materializer` não devem compor o catálogo operacional da v2 orientada a Instagram; qualquer decisão de evidência comercial, materialização, dor, resultado ou oferta pertence ao pipeline posterior de hipótese.

## Regra obrigatória — anti-ciclo e uso controlado de IA no NichoCNAE v2

- O pipeline NichoCNAE v2 não deve repetir indefinidamente o mesmo circuito de etapas quando não houver ganho novo de conhecimento sobre o subnicho; repetição sem nova evidência é falha controlada, não pesquisa em andamento.
- Cada job deve manter um orçamento operacional por subnicho, incluindo limite de voltas por etapa, limite de pesquisas sem fontes novas, limite de replanejamentos de query, limite de chamadas de IA e custo máximo em dólar antes de encerrar com decisão auditável.
- O avanço entre etapas deve ser orientado por progresso mensurável: novo subnicho candidato, nova fonte pública útil, nova evidência de rotina, novo sinal de aquisição/cobrança/recorrência, ou eliminação objetiva de um caminho ruim. Sem um desses ganhos, a próxima ação deve ser trocar recorte, concluir como sem evidência suficiente ou pedir decisão humana.
- A IA pode ser usada como ferramenta de decisão e síntese, mas não como motor livre de tentativa infinita. Chamadas de IA devem acontecer somente em pontos de alto impacto: escolher/renomear subnicho, planejar queries quando a busca determinística travar, sintetizar evidências, decidir próximo movimento após reprovação e resumir aprendizado para a tela.
- Antes de qualquer nova chamada de IA por reprocessamento, o executor deve enviar ao prompt o histórico compacto do job: etapas já repetidas, fontes rejeitadas, queries já testadas, causa dominante da reprovação e orçamento restante. A resposta deve obrigatoriamente escolher entre mudar recorte, mudar estratégia de fonte, encerrar sem evidência ou pedir intervenção humana.
- Quando o job repetir o mesmo trio ou circuito de etapas sem avanço funcional, o executor deve registrar `CONTROLLED_FAILURE` com motivo de negócio claro, custo acumulado, evidências tentadas e recomendação objetiva para nova tentativa em outro subnicho, em vez de manter o job aberto.
- A UI deve mostrar a situação em linguagem de negócio: “pesquisa sem ganho novo”, “sem fonte pública suficiente”, “subnicho amplo demais”, “recorte precisa mudar” ou “limite de custo atingido”, evitando que o usuário interprete ciclo técnico como progresso real.
- O uso de IA deve continuar auditado por job/etapa com modelo, service tier, tokens, custo, request e response bruto; jobs sem registro na tabela de auditoria OpenAI devem ser apresentados como execução sem gasto de IA.

## Regra obrigatória — público-alvo MEI/autônomo no NichoCNAE

- Quando o pipeline NichoCNAE for originado do levantamento MEI, o CNAE deve ser tratado como ponto de partida de segmentação e auditoria, não como definição final do público-alvo.
- O alvo prioritário da pesquisa é o profissional brasileiro que executa pessoalmente o trabalho: MEI, autônomo, trabalhador por conta própria ou dono-operador que depende da própria rotina, agenda, atendimento, cobrança, reputação e aquisição de clientes para faturar.
- A pesquisa inicial deve construir entendimento do comportamento, rotina diária/semanal, tarefas recorrentes, dores práticas, dores emocionais, sonhos, medos, inseguranças, linguagem real e canais usados pelo MEI/autônomo para trabalhar, vender, atender e cobrar.
- A pesquisa deve diferenciar explicitamente `CNAE` de `público-alvo MEI/autônomo`: o CNAE descreve a atividade econômica oficial; o público-alvo descreve a pessoa real, seu modo de trabalho e seu contexto operacional no Brasil.
- É proibido avançar para produto, oferta, campanha, promessa comercial, mecanismo de venda, headline, landing page ou hipótese de experimento durante esta fase de construção de público-alvo; preço, cobrança, pacotes e recorrência podem ser pesquisados somente como prática operacional observada do profissional.
- Fontes brasileiras e recentes devem ser priorizadas sempre que possível: português do Brasil, contexto brasileiro, dados dos últimos 24 meses, domínios ou instituições brasileiras, comunidades brasileiras e sinais atuais do mercado local.
- Fontes antigas só podem ser usadas quando forem estruturais, oficiais ou ainda claramente válidas; nesse caso, o risco de desatualização deve permanecer explícito na análise ou nos artefatos posteriores.
- Evidências sobre empresas estruturadas, franquias, grandes negócios ou fornecedores B2B só podem apoiar contexto secundário; elas não podem substituir a compreensão do profissional que executa o trabalho.

## Regra obrigatória — fontes sociais e comunidades públicas no NichoCNAE

- Redes sociais, comunidades públicas, comentários e fóruns só podem complementar a pesquisa de público-alvo MEI/autônomo quando houver fonte pública, mecanismo permitido, estabilidade operacional e aderência aos termos de uso da plataforma.
- É proibido integrar scraping social amplo, burlar login, captcha, paywall, bloqueio técnico, grupos privados, mensagens privadas ou qualquer fonte sem autorização clara.
- A coleta social/comunitária deve ser opcional, rastreável e desacoplada do pipeline principal; sua ausência não pode bloquear as etapas canônicas de pesquisa, síntese e gate.
- A saída permitida deve ser apenas agregada e comportamental: padrões de linguagem, sinais de dor, sonhos, medos, canais e comportamento de aquisição/atendimento do MEI/autônomo.
- Dados pessoais, identificadores de perfil, comentários integrais desnecessários, contatos, documentos, localização precisa ou qualquer dado sensível não podem ser persistidos.
- Qualquer etapa futura, como `social-behavior-searcher`, deve permanecer desativada por padrão até aprovação fonte a fonte, possuir contrato backend próprio, registrar logs de ingestão do payload bruto e gravar somente sinais agregados/curtos.
- Conteúdo social com linguagem de produto, oferta, campanha, promessa, ferramenta, curso, automação ou IA deve ser tratado como risco de contaminação da fase inicial, não como oportunidade comercial.

## Critério de efetividade — público-alvo MEI/autônomo

- Cada ciclo NichoCNAE originado do levantamento MEI deve deixar claro quem é o MEI/autônomo pesquisado, quais ocupações ou autodenominações aparecem dentro do CNAE e por que esse público representa a pessoa que executa o trabalho.
- A síntese deve destacar comportamento, rotina, dores, sonhos, linguagem e canais reais antes de qualquer discussão posterior sobre solução.
- Qualquer material com produto, oferta, campanha ou promessa comercial deve ser bloqueado ou tratado como contaminação de fase, não como resultado válido da pesquisa inicial.
- A avaliação de qualidade deve considerar aderência a MEI/autônomo brasileiro, evidência comportamental, atualidade das fontes e risco de desvio para empresa estruturada ou linguagem comercial.
- O gate de qualidade só pode liberar materialização quando aquisição de clientes, canais usados, recorrência e comportamento de clientes tiverem evidência útil; placeholders como “Sem evidência suficiente” devem resultar em `NEEDS_MORE_MEI_RESEARCH`, mesmo quando a rotina operacional estiver bem descrita.

## Critério de efetividade — pesquisa inicial NichoCNAE sem viés de solução

- Ciclos de pesquisa inicial devem expor ou persistir o modo `ROUTINE_REALITY_RESEARCH` para rastreabilidade operacional.
- Seeds, queries, fontes, sinais, sínteses e gates da fase inicial devem ser aceitos somente quando servirem à compreensão da rotina real e das dificuldades do nicho no Brasil.
- Snapshots da etapa quatro devem persistir indicadores próprios de intenção da fonte, evidência de rotina, risco comercial e risco de linguagem de solução, mantendo política de armazenamento curto.
- Qualquer conteúdo de solução, produto, campanha, oferta ou hipótese comercial deve pertencer a fluxo posterior, separado e rastreável, depois da conclusão da pesquisa de realidade operacional.
- A tela administrativa `/oprm/pipeline` está obsoleta e deve permanecer desativada no frontend; o acompanhamento e o disparo do pipeline NichoCNAE devem acontecer somente pelo caminho de criação de novo nicho/subnicho a partir do CNAE (`/oprm` e detalhe do CNAE), para evitar operação paralela sem decisão comercial clara.

## Regra obrigatória — materialização final do NichoCNAE em nicho enriquecido

- Após o `oprmRoutineQualityGate` aprovar um cartão com `readyForHypothesis=true`, o pipeline OPRM NichoCNAE deve executar uma etapa final chamada `oprmEnrichedNicheMaterializer`.
- A etapa final só pode liberar materialização, estratégia ou próximos fluxos comerciais quando existir `oprm_mei_audience_profile` rastreável para o `research_cycle_id` ou, em retentativas controladas, para o `market_niche_id` já materializado; sem esse perfil, o ciclo deve permanecer operacionalmente como “Aguardando perfil MEI/autônomo”.
- Essa etapa final deve alimentar obrigatoriamente duas estruturas persistidas: a tabela principal de nichos (`market_niche`) e a tabela de nicho enriquecido (`market_niche_enrichment_profile`).
- A materialização final não deve criar hipótese, experimento, oferta, campanha ou landing page; hipótese será tratada em fluxo próprio posterior.
- O `market_niche` deve receber o cadastro operacional do nicho com nome canônico formado por código e descrição do CNAE, descrição enriquecida, segmentação base, contexto de uso e vínculo `source_cnae_code`/`source_cnae_description`, sem inventar uma oferta; o nome original contaminado deve ficar apenas como auditoria.
- O `market_niche_enrichment_profile` deve preservar apenas dados compatíveis com pesquisa de rotina real vindos do NichoCNAE: CNAE, score OPRM, ciclo de pesquisa, cartão de rotina, nome original para auditoria, nome neutro, modo de pesquisa, rotina, tarefas recorrentes, perguntas, contexto operacional, linguagem pública, evidências, fontes, scores de qualidade e risco de contaminação por linguagem de solução.
- Campos legados de materialização com nomes comerciais, dores, resultados desejados ou mecanismos/oportunidades não fazem parte da saída final do NichoCNAE v3; quando existirem por compatibilidade histórica, não devem ser tratados como verdade final da pesquisa inicial.
- O vínculo entre OPRM e nicho comercial deve permanecer rastreável por `research_cycle_id`, `routine_card_id`, `source_niche_candidate_id` e `market_niche_id`.
- A conclusão da etapa deve atualizar o ciclo para `ENRICHED_NICHE_CREATED`; falhas devem registrar status `ENRICHED_NICHE_FAILED` e mensagem operacional no ciclo.
- No pipeline NichoCNAE v3, a etapa final `persona-routine-materializer` também deve alimentar obrigatoriamente `market_niche` e `market_niche_enrichment_profile`; o `output_payload` da execução é auditoria técnica, não pode ser o único local das informações finais, porque pipelines posteriores consomem dados do nicho.

## Critério de efetividade — nicho enriquecido materializado

- Um cartão aprovado pelo gate só pode ser considerado finalizado quando existir pelo menos um registro correspondente em `market_niche_enrichment_profile` e um `market_niche_id` persistido.
- É permitido gerar múltiplos registros em `market_niche_enrichment_profile` para o mesmo `market_niche_id`, inclusive para o mesmo `routine_card_id` ou `research_cycle_id`, quando houver reprocessamento operacional; cada registro deve preservar a auditoria histórica da execução que o criou.
- Reprocessamentos devem reaproveitar e atualizar o `market_niche` existente pelo vínculo canônico de CNAE (`source_cnae_code`) de forma controlada, atualizando também o perfil enriquecido correspondente quando o mesmo CNAE já tiver materialização final.
- No NichoCNAE v3, o mesmo CNAE deve atualizar o `market_niche` vinculado por `source_cnae_code`; variações de subnicho devem ser preservadas no perfil enriquecido/auditoria, não como nichos soltos sem vínculo de reprocessamento.
- A etapa final, o `marketNicheId`, o `enrichedNicheProfileId` e o status de materialização devem ser exibidos no fluxo de detalhe/criação de novo nicho do CNAE; a tela administrativa `/oprm/pipeline` não deve ser usada para esse acompanhamento.
- Ciclos em `ENRICHED_NICHE_FAILED` devem oferecer ação operacional pelo próprio front-end para reexecutar etapas do mesmo job ou, quando explicitamente necessário, iniciar nova pesquisa rastreável; o usuário não deve ser orientado a corrigir esse caso por acesso direto ao banco de dados.
- O contrato da etapa final deve seguir o padrão de unidade de trabalho fechada: o endpoint `pending` precisa entregar todos os dados necessários para o coletor concluir a materialização sem buscar detalhes adicionais.

## Regra obrigatória — pipeline oficial OPRM NichoCNAE

- O pipeline oficial de pesquisa e materialização de NichoCNAE deve usar o código operacional `oprm-nicho-cnae-pipeline` e o módulo `OPRM`.
- O contrato estrutural do pipeline deve refletir as etapas já implementadas no backend e no coletor OPRM: `routine-research-orchestrator`, `routine-research-cycle`, `niche-research-seed-builder`, `source-searcher`, `source-fetcher`, `signal-extractor`, `routine-synthesizer`, `mei-audience-segmenter`, `routine-quality-gate` e `enriched-niche-materializer`.
- Todas as etapas do pipeline oficial devem permanecer obrigatórias, ativas e executadas pelo módulo `oprm-coletor-mei`, consumindo exclusivamente endpoints OPRM do backend principal.
- O controle de qual etapa deve publicar pendências para um ciclo NichoCNAE deve ficar registrado em `oprm_routine_research_cycle.current_stage_code`. Sempre que o executor concluir ou falhar uma etapa, o callback ao backend deve atualizar esse campo para a próxima etapa canônica, manter a etapa atual em caso de falha recuperável ou limpar o campo quando o ciclo estiver materializado/finalizado; os endpoints `pending` devem usar esse campo como fonte primária para decidir o que entregar.
- Somente as etapas `niche-research-seed-builder` e `mei-audience-segmenter` devem ser marcadas como consumidoras diretas de modelo OpenAI configurável. As demais etapas são orquestração, consulta, coleta pública, extração/síntese/gate determinísticos ou materialização baseada em dados já coletados, e não devem exibir seleção de modelo OpenAI na tela administrativa quando não houver modelo operacional legado configurado.
- Todo campo textual sintético persistido pelo cartão de rotina do pipeline OPRM NichoCNAE, incluindo `mechanismOpportunitiesSummary`, deve usar coluna `LONGTEXT` no banco e respeitar limite operacional máximo de 20000 caracteres antes do envio ao backend. Quando uma etapa usar modelo de IA para gerar qualquer campo persistido com esse limite, o prompt deve declarar explicitamente o limite máximo de 20000 caracteres por campo e a camada determinística deve validar ou compactar o conteúdo antes de concluir a etapa. Campos comerciais de dor, resultado e mecanismo pertencem ao cartão de rotina e aos pipelines posteriores de hipótese/oferta; eles não devem ser persistidos no perfil enriquecido do nicho (`market_niche_enrichment_profile`).
- Antes de expor pendência para a etapa `mei-audience-segmenter`, o backend deve validar o cartão `oprm_niche_routine_card`: `routine_evidence_score` deve ser maior que zero e os textos essenciais de nicho, público executor, rotina/contexto operacional e evidência não podem estar vazios nem conter marcador como “Sem evidência suficiente”. Quando essa definição mínima de nicho estiver ausente, o ciclo deve ser bloqueado para nova pesquisa/aprofundamento com status operacional de necessidade de pesquisa, e o motivo registrado deve ser “cartão sem definição mínima de nicho”, sem tratar como falha genérica.
- O pipeline oficial deve preservar a separação canônica: pesquisa de realidade operacional e materialização de nicho enriquecido não podem criar hipótese, experimento, oferta, campanha ou landing page.
- A etapa `mei-audience-segmenter` deve aceitar apenas perfil comportamental MEI/autônomo: quem é, como trabalha, como consegue clientes, dores, medos, linguagem, canais e evidências. O prompt e as validações determinísticas devem proibir criação de produto, sugestão de solução e qualquer menção a oferta, software, IA, automação ou ferramenta; ao detectar contaminação antes de concluir a etapa, o coletor pode regenerar uma única vez com instrução corretiva antes de registrar falha.
- Quando o usuário solicitar execução manual do pipeline para um CNAE específico, o sistema deve encerrar automaticamente todos os ciclos ainda abertos desse CNAE e iniciar um ciclo completamente novo, mantendo rastreabilidade dos ciclos encerrados e impedindo concorrência entre execuções antigas e a nova solicitação operacional.

## Critério de efetividade — pipeline oficial OPRM NichoCNAE

- A tela administrativa de pipelines deve reconhecer `oprm-nicho-cnae-pipeline` como contrato oficial com dez etapas esperadas, incluindo a segmentação comportamental MEI/autônomo antes do gate de qualidade.
- O diagnóstico do pipeline deve bloquear etapas extras, ausentes, fora de posição ou marcadas como opcionais/inativas.
- A sincronização oficial pode reparar nome, posição, obrigatoriedade, módulo executor e pacote raiz sem sobrescrever descrição operacional ou modelo OpenAI configurado.

## Referência de governança

- Este documento é o cânone específico de OPRM para ingestão de CNAE e totalização de market size.
- As diretrizes gerais do sistema permanecem em `docs/canonical/system-governance-canon.v2.md`.

## Regra obrigatória — ciclo 3 com snapshot canônico fixo para operação

- Para operação atual da ingestão OPRM CNPJ/CNAE, o ciclo vigente é o **ciclo 3**.
- O ciclo 3 deve **apenas cadastrar emails associados a CNAEs**, usando os arquivos `Estabelecimentos*.zip`; ele não deve recalcular market size, score, enriquecimento ou materializar nichos.
- No ciclo 3, o `snapshotDate` canônico deve permanecer **fixo em `2026-06-14`**.
- É proibido alterar automaticamente a data do snapshot para diretórios mais novos durante execução agendada ou manual sem decisão explícita do usuário.
- Qualquer tentativa de execução com `snapshotDate` diferente de `2026-06-14` deve ser tratada como não conformidade operacional e registrada em log com causa-raiz.

## Critério de efetividade — ciclo 3 com snapshot fixo

- Logs de execução devem mostrar explicitamente `ciclo=3`, `snapshotDate=2026-06-14` e a `sourceUrl` usada.
- A base de download deve ser explicitamente `https://dados-abertos-rf-cnpj.casadosdados.com.br/arquivos/2026-06-14/` enquanto o ciclo 3 estiver vigente.
- Antes de iniciar a execução agendada/manual, deve haver validação de acesso HTTP (ex.: `HEAD`) para os arquivos de referência do snapshot (mínimo: `Estabelecimentos0.zip` e `Estabelecimentos1.zip`) com retorno `200`.
- Linhas sem email devem ser ignoradas no ciclo 3 para evitar cadastro de CNAEs sem contato acionável.

## Regra obrigatória — ranking de CNAEs por score OPRM com paginação

- A listagem de ranking de CNAEs por volume (`/oprm/cnaes-volume`) deve ser sempre ordenada por **Score OPRM** em ordem decrescente (maior para menor), mantendo os dados de volume como contexto operacional da oportunidade.
- O endpoint de leitura do ranking deve suportar paginação explícita (parâmetros de página e tamanho), evitando retorno massivo em uma única resposta.
- O tamanho padrão por página para essa visão operacional deve ser de **50 registros por página**.
- A ordenação por Score OPRM deve ser aplicada no backend (SQL/consulta), não no frontend por pós-processamento em memória.

## Critério de efetividade — ranking paginado

- A primeira página precisa trazer os CNAEs com maior `Score OPRM` no snapshot vigente da ingestão.
- Ao navegar entre páginas, a ordenação deve permanecer estável por `Score OPRM` decrescente.
- O texto de apoio na tela deve deixar explícito para o usuário que o ranking está ordenado por Score OPRM e paginado.

## Regra obrigatória — fluxo operacional CNAE → subnicho

- A experiência administrativa do OPRM deve começar pela lista paginada de CNAEs priorizados por Score OPRM.
- Ao selecionar um CNAE, a tela deve exibir todos os subnichos/nichos enriquecidos já criados para esse CNAE antes de oferecer nova execução, reduzindo duplicidade e gasto sem aprendizado.
- O comando manual principal dessa tela deve ser “Criar novo subnicho” e deve disparar a análise do CNAE considerando os nichos já criados para buscar uma oportunidade diferente com potencial de venda.
- Após a criação do ciclo, a UI deve levar o usuário para uma visão dedicada ao subnicho/ciclo recém-criado, exibindo etapas, histórico de jobs apenas desse novo nicho, custo total do subnicho, custo por execução e evolução do pipeline.

## Critério de efetividade — fluxo operacional CNAE → subnicho

- A tela do CNAE não deve misturar histórico de custos de ciclos antigos quando o usuário estiver acompanhando um subnicho específico.
- A visão dedicada do subnicho deve manter rastreabilidade por `researchCycleId` e permitir abrir detalhes das etapas do pipeline.

## Regra obrigatória — lista de nichos enriquecidos por score

- A lista de nichos enriquecidos exibida na tela de CNAEs por Score OPRM deve priorizar os candidatos com maior `opportunityScore` no começo.
- A ordenação deve ser aplicada no backend, com `opportunityScore` em ordem decrescente e `createdAt` em ordem decrescente como desempate estável.
- O frontend deve informar ao usuário que a lista está ordenada pelos maiores scores, mantendo dor, resultado e mecanismo como contexto de decisão.

## Critério de efetividade — nichos enriquecidos por score

- Ao abrir "Nichos já enriquecidos", o primeiro item retornado deve ter score maior ou igual aos demais itens da página.
- Em caso de scores iguais, candidatos enriquecidos mais recentemente aparecem primeiro para preservar rastreabilidade operacional.

## Regra obrigatória — responsabilidade do OPRM no fluxo CNAE → oportunidade

- No fluxo CNAE → score → enriquecimento → candidatos de nicho, o **módulo OPRM** é o único responsável por cálculo de score de oportunidade, seleção de CNAEs prioritários para enriquecimento, pesquisa externa, acionamento de MDS/Worker AI e geração de candidatos de nicho.
- O usuário não deve precisar solicitar manualmente a geração de score de oportunidade quando houver necessidade real de atualização; porém, como a base CNAE muda pouco, os ciclos agendados de score e enriquecimento devem ficar desligados por padrão e só devem ser religados por configuração operacional explícita.
- O enriquecimento de CNAEs com melhor score deve ocorrer em execução agendada separada do cálculo de score quando habilitado, para permitir controle operacional, retentativa e auditoria independentes.
- O backend deve atuar somente como camada de API e persistência para esse fluxo: leitura, gravação, paginação, filtros e validação técnica de contrato. É proibido colocar no backend cálculo de score, enriquecimento, chamada a integrações externas ou regra de negócio de priorização CNAE.
- Cada execução agendada desse fluxo, quando habilitada, deve registrar identificadores de ciclo, no mínimo `cycleId`, `cycleType` e `cycleNumber`, e os logs devem incluir esses identificadores junto com o `cnaeCode` quando aplicável.

## Critério de efetividade — ciclos CNAE de oportunidade

- Deve existir rastreabilidade separada para ciclos de score (`CNAE_SCORE`) e ciclos de enriquecimento (`CNAE_ENRICHMENT`).
- Um ciclo de score deve registrar quantidade de CNAEs lidos sem score, quantidade processada, quantidade com falha, versão da regra/algoritmo do OPRM e resumo final.
- Um ciclo de enriquecimento deve registrar critério de seleção por score, quantidade selecionada, fontes externas acionadas, quantidade de candidatos gerados, quantidade com falha e resumo final.
- O frontend deve consumir dados persistidos de ranking, score, ciclos e candidatos via backend, sem disparar cálculo de score como etapa obrigatória do usuário.

## Regra obrigatória — execução das etapas NichoCNAE dentro do próprio módulo OPRM

- As etapas do pipeline OPRM NichoCNAE que precisam acessar modelo de IA devem implementar esse acesso no próprio módulo executor do OPRM, atualmente `oprm-coletor-mei`, e não no `ai-worker`.
- O padrão arquitetural obrigatório para essas etapas é o documento `docs/metodologia/gerado-5-5/arquitetura-pipeline-etapas-archunit.md`, aplicado no módulo executor `oprm-coletor-mei`, nunca no backend principal por padrão.
- O backend principal (`backend/ads-service`) não deve receber núcleo `pipeline`, `StageProcessor`, `StageContext`, `StageResult`, `StageArtifact` ou regras ArchUnit de protocolo padrão módulo para o NichoCNAE; seu papel é expor contratos, persistir estado, publicar pendências e receber callbacks/resultados.
- Cada etapa concreta deve permanecer em pacote próprio no módulo executor, plugável e removível, dependendo apenas do núcleo genérico do pipeline do executor, infraestrutura compartilhada permitida e contratos persistidos/DTOs oficiais.
- É proibido acoplar uma etapa concreta a outra etapa concreta para avançar o fluxo; o encadeamento deve ocorrer por contratos persistidos, artefatos auditáveis, endpoints do backend ou outro contrato oficial.
- O backend principal permanece como fonte de verdade dos dados e contratos; o módulo executor OPRM deve consumir e concluir etapas por endpoints do próprio OPRM/backend, sem acesso direto ao banco.
- Chamadas ao modelo, prompts, validações e mapeamento de resposta devem ficar encapsulados no pacote da etapa concreta que usa IA, preservando isolamento, rastreabilidade e testabilidade.
- Toda chamada OpenAI do OPRM NichoCNAE deve usar Responses API em modo Flex (`service_tier=flex`) e modelo operacional `gpt-5.2`, tanto como fallback local do executor quanto como configuração persistida de etapa no backend.
- Na etapa `niche-research-seed-builder`, o sistema deve confiar no modelo e não deve bloquear o ciclo por validações determinísticas de conteúdo ou completude do payload gerado; quando campos estruturais vierem ausentes, a persistência deve aplicar defaults operacionais rastreáveis para manter a pesquisa avançando para busca, coleta, extração, síntese e gate.
- Campos textuais gerados pelo modelo na etapa `niche-research-seed-builder` não devem ser truncados antes da persistência; o JSON Schema enviado ao modelo deve declarar `maxLength` para campos gravados em colunas curtas do backend, preservando o contrato físico sem perda silenciosa de informação.

## Critério de efetividade — execução modular das etapas NichoCNAE

- Alterações em etapas OPRM NichoCNAE com IA devem citar o pacote da etapa concreta no `oprm-coletor-mei` e o contrato backend consumido/concluído.
- Testes de arquitetura/ArchUnit devem proteger que o núcleo do pipeline não dependa de etapas concretas e que etapas concretas não dependam entre si.
- A implementação ou revisão de uma etapa deve validar se o avanço para a próxima etapa ocorre por dados/contratos oficiais, e não por chamada direta entre classes de etapas.

## Regra obrigatória — independência dos ciclos NichoCNAE

- Cada ciclo do pipeline OPRM NichoCNAE deve ser operacionalmente independente dos ciclos anteriores e posteriores do mesmo CNAE.
- A falha, cancelamento, bloqueio, parada ou pendência residual de um ciclo anterior não pode entrar na fila de execução de um novo ciclo ativo.
- Filas internas de etapas por candidato, fonte, snapshot, sinal, card ou perfil devem restringir a busca a ciclos `RUNNING` com `finishedAt` vazio, salvo rotinas explícitas de diagnóstico, auditoria ou reprocessamento manual.
- Ao reiniciar manualmente um CNAE, o novo ciclo deve conseguir avançar mesmo que existam registros `FOUND`, `PENDING` ou parcialmente processados em ciclos antigos já finalizados.

## Critério de efetividade — independência dos ciclos NichoCNAE

- Endpoints internos de fila não devem retornar unidades de trabalho de ciclos `FAILED`, `STALLED`, `CANCELLED_BY_MANUAL_RESTART` ou com `finishedAt` preenchido.
- Testes de regressão devem cobrir que pendências antigas de ciclo falhado não bloqueiam a etapa equivalente de um ciclo novo em execução.

## Regra obrigatória — publicação do executor OPRM com acesso à chave OpenAI

- O `oprm-coletor-mei`, quando executar etapas NichoCNAE com IA, deve ser publicado no mesmo host do `ai-worker`: `191.252.120.96`.
- A publicação no mesmo host permite reutilizar o arquivo de chave OpenAI já provisionado para o `ai-worker`, sem duplicar segredo em outro VPS.
- O arquivo de chave OpenAI deve ser montado no container OPRM como somente leitura em `/run/secrets/openai_api_key`, usando por padrão o caminho de host `/root/infra/openai-token/openai_api_key`.
- A etapa OPRM deve preferir variável direta de chave quando explicitamente configurada, mas deve aceitar `OPRM_NICHO_CNAE_SEED_BUILDER_OPENAI_API_KEY_FILE`/`OPENAI_API_KEY_FILE` para leitura do segredo montado.

## Critério de efetividade — publicação e segredo OpenAI do executor OPRM

- O workflow de deploy do `oprm-coletor-mei` deve usar `DEPLOY_HOST=191.252.120.96` enquanto a chave OpenAI operacional estiver provisionada no host do `ai-worker`.
- O compose do `oprm-coletor-mei` deve montar o mesmo arquivo de chave OpenAI em modo `ro` e configurar a etapa `oprmNicheResearchSeedBuilder` para ler esse arquivo.
- É proibido commitar a chave OpenAI ou materializar o segredo em `.env` versionado; apenas o caminho do arquivo/volume pode ser versionado.

## Regra obrigatória — ciclos NichoCNAE sem progresso

- Ciclos `RUNNING` do pipeline OPRM NichoCNAE não podem permanecer indefinidamente como execução saudável quando não houver avanço operacional persistido.
- O backend deve marcar como `STALLED` ciclos `RUNNING` sem progresso por mais de 6 horas, considerando ausência simultânea de queries, candidatos de fonte, snapshots e sinais extraídos.
- Ao marcar um ciclo como `STALLED`, o backend deve preencher `finishedAt`, registrar `errorMessage` com orientação operacional e atualizar o candidato de origem para `RESEARCH_STALLED` quando ele existir.
- A correção de um ciclo `STALLED` deve investigar primeiro executor `oprm-coletor-mei`, carregamento do pacote `com.marketinghub.nichocnae`, scheduler da etapa parada e conectividade com o backend antes de abrir novo ciclo.

## Critério de efetividade — ciclos NichoCNAE sem progresso

- A tela operacional não deve tratar `STALLED` como ciclo saudável em execução.
- O log backend deve registrar a quantidade de ciclos marcados como `STALLED` e os identificadores operacionais (`researchCycleId`, `sourceNicheId`, `cnaeCode`).
- O limite de 6 horas deve impedir falso positivo em execuções demoradas, mas revelar parada real do pipeline no mesmo dia operacional.

## Regra obrigatória — dados para Facebook Ads

- O OPRM não deve acessar, materializar ou depender diretamente de entidades, services, repositories ou controllers de targeting/Facebook Ads.
- O papel do OPRM é produzir e registrar no backend dados de público, rotina, linguagem, dores, sinais e evidências com rastreabilidade.
- O Facebook Ads deve buscar esses dados no backend por contrato próprio e decidir, no contexto dele, como transformar os dados em segmentação, resolução Meta, campanha, ad set ou publicação.
- Quando o OPRM gerar sinais úteis para anúncio, esses sinais devem permanecer como dados de origem OPRM ou campos do backend, nunca como escrita direta em tabelas de targeting ou chamada direta a serviços de targeting.

## Critério de efetividade — separação OPRM x Facebook Ads

- Classes em `com.marketinghub.oprm..` não devem importar `com.marketinghub.targeting..` nem `com.marketinghub.repository.jpa.targeting..`.
- O OPRM pode validar se há dados mínimos para coleta futura pelo Facebook Ads, mas não deve criar elemento de targeting nem liberar publicação.
- Se o Facebook Ads precisar de novo formato, o contrato deve nascer no backend e ser consumido pelo módulo Facebook Ads, mantendo o OPRM como produtor de dados e não como publicador.

## Regra obrigatória — limite de responsabilidade do OPRM em confirmações e etapas posteriores

- O OPRM deve tratar a situação de oportunidade, dor, público, rotina e confirmação dentro do seu próprio domínio e persistir os dados no banco por contratos/tabelas OPRM.
- É proibido o pacote `com.marketinghub.oprm..` materializar diretamente fluxos de outros módulos, como Lead Portal, Experimentos, campanhas, ofertas finais ou publicações.
- Quando o OPRM preparar uma confirmação de público/dor, ele deve registrar a situação em tabela OPRM, com identificadores externos apenas como valores simples quando necessário, sem importar entidades, services, DTOs ou repositories de outros módulos.
- Módulos posteriores devem consumir os dados persistidos por contrato oficial/API própria, preservando isolamento e evitando que o OPRM ultrapasse seus limites.

## Critério de efetividade — limite de responsabilidade do OPRM

- Testes de arquitetura devem falhar quando classes em `com.marketinghub.oprm..` dependerem diretamente de pacotes internos que não sejam OPRM ou repositories OPRM, salvo exceções nominais já documentadas.
- Endpoints OPRM de preparação/confirmação devem devolver registros OPRM persistidos, não IDs de entidades criadas em módulos externos.
- O dado persistido pelo OPRM deve ser suficiente para orientar a próxima etapa do fluxo de vendas sem acoplamento direto entre módulos.

## Regra obrigatória — próximo movimento automático após reprovação no gate NichoCNAE

- Toda reprovação da etapa `routine-quality-gate` deve produzir um próximo movimento operacional explícito, persistido nas notas estruturadas de qualidade, para evitar que o pipeline apenas pare sem direcionamento.
- O próximo movimento deve ser escolhido pela causa dominante da reprovação: contaminação de solução, desvio corporativo, fonte antiga, definição fraca de público executor, aquisição/canais observáveis insuficientes, rotina genérica ou mix MEI/autônomo incompleto.
- Quando o gate aprovar o ciclo, o próximo movimento deve apontar para materialização do nicho enriquecido; quando reprovar, deve apontar para a nova pesquisa direcionada ou correção objetiva antes de gastar nova execução profunda.
- Quando a reprovação do gate for recuperável (`NEEDS_MORE_RESEARCH`, `NEEDS_MORE_MEI_RESEARCH`, `OUTDATED_SOURCES`, `TOO_CORPORATE`, `SOLUTION_CONTAMINATED`, `NEEDS_EXECUTOR_ROUTINE_EVIDENCE` ou `GENERIC`), o módulo executor externo responsável pelo OPRM deve decidir e solicitar a reexecução automática de uma ou mais etapas do mesmo job; o backend deve apenas persistir a decisão do gate, expor os dados necessários e reabrir o job existente quando solicitado pelo executor, sem orquestrar sozinho o reprocessamento.
- Cada reexecução automática solicitada pelo executor deve preservar o mesmo `researchCycleId`, o subnicho aprendido e disponibilizar ao seed o status, o próximo movimento e as notas compactadas do gate anterior, para que a próxima execução não perca aprendizado e não repita a mesma causa dominante de reprovação.
- Quando o executor solicitar a reexecução com `triggerSource=AUTO_QUALITY_REPROCESS`, o backend deve limpar os artefatos das etapas que serão refeitas, manter o job/ciclo original como unidade operacional e registrar que a próxima entrada do seed deve usar o aprendizado do gate anterior, mantendo o backend como persistência/contrato e a inteligência operacional no executor.
- O relatório baixável do OPRM deve estar disponível por `researchCycleId` mesmo antes da materialização final, incluindo status atual, gatilho, observações de reexecução, artefatos existentes e ausência explícita dos artefatos que ainda serão gerados novamente.
- A tela operacional deve expor esse próximo movimento em linguagem de negócio, mantendo os detalhes técnicos como apoio e não como principal orientação ao usuário.
- Quando houver reprocessamento automático, a tela do fluxo deve informar claramente ao usuário que o mesmo job foi reaberto pelo módulo executor sem clique manual, qual causa do gate orientou a reexecução e quantas tentativas automáticas já foram usadas.

## Critério de efetividade — próximo movimento automático no gate NichoCNAE

- As notas do gate devem conter pelo menos um código estável de próximo movimento e uma descrição legível para operação.
- Testes unitários devem cobrir que reprovações por solução, fonte antiga, desvio corporativo, definição fraca de público executor, aquisição/canais observáveis insuficientes e rotina genérica recebem movimentos diferentes e coerentes com a causa-raiz.

## Regra de concorrência NichoCNAE v2 por CNAE

- Para cada CNAE, pode existir no máximo um job NichoCNAE v2 operacionalmente aberto ao mesmo tempo.
- Estados considerados abertos: `PENDING`, `RUNNING` e `TECHNICAL_RETRY_SCHEDULED` em `oprm_nichocnae_v2_stage_execution`.
- O backend deve bloquear a criação manual de novo job v2 quando já existir job aberto para o mesmo `cnae_code`, evitando concorrência, custo duplicado de IA e confusão na tela administrativa.
- Jobs antigos presos em estado aberto devem ser encerrados como falha operacional auditável antes de liberar uma nova execução para o CNAE.
