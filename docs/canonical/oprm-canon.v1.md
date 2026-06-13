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
- O objetivo desse modo é compreender a realidade comercial-operacional do nicho a partir do CNAE: rotina, tarefas, aquisição de clientes, agenda, faltas, remarcações, precificação, cobrança, pacotes, recorrência, materiais, tempo de atendimento, retrabalho, perguntas recorrentes, contexto de trabalho, linguagem orgânica e evidências públicas.
- A pesquisa deve parar na descrição realista da rotina e das dificuldades; ela não deve procurar, inferir, preparar ou validar solução, produto, campanha, oferta, promessa comercial, landing page, hipótese de experimento ou tese de mecanismo comercial.
- Termos de solução, como IA, automação, sistema, app, software, ferramenta, curso ou marketing digital, não podem direcionar a pesquisa inicial quando vierem apenas de enquadramento comercial do nome do candidato.
- O nome operacional usado para pesquisa deve representar o nicho/CNAE de forma neutra; nomes originais contaminados por linguagem de solução devem ser preservados apenas para auditoria e não como fonte de direcionamento da busca.
- A etapa inicial deve priorizar evidências públicas sobre execução do trabalho, operação comercial cotidiana, problemas operacionais, dúvidas reais, atividades, responsabilidades, limitações, sazonalidade, riscos, custos, retrabalho, gargalos e vocabulário usado pelo próprio mercado brasileiro.
- A etapa `oprmNicheResearchSeedBuilder` deve gerar famílias de queries sobre: aquisição de clientes por WhatsApp/Instagram/indicação; agenda, faltas, remarcações e clientes que somem; precificação, cobrança, pacotes e recorrência; materiais, tempo de atendimento e retrabalho; relatos reais de profissionais autônomos em fóruns, vídeos, comentários e perguntas frequentes.
- A aquisição de clientes deve ser pesquisada como eixo obrigatório da realidade operacional do profissional, buscando evidências de captação de clientes, canais usados, indicação, redes sociais, WhatsApp, Instagram, orçamento, agenda vazia, retorno, fidelização, cancelamento, reativação e recorrência.
- Aquisição de clientes, preço e cobrança nessa fase significam comportamento observado do MEI/autônomo para manter a operação funcionando; é proibido tratá-los como recomendação de marketing, criação de campanha, oferta, funil, anúncio ou estratégia comercial prescritiva.
- A etapa `oprmNicheResearchSeedBuilder` deve aceitar objetivos de query compatíveis com rotina real e operação comercial cotidiana, incluindo aquisição de clientes, agenda/faltas/remarcações, precificação/cobrança/pacotes/recorrência, materiais/tempo/retrabalho, relatos reais, perguntas recorrentes, linguagem e contexto operacional.
- As queries e buscas do pipeline OPRM NichoCNAE devem ser Brasil-first: português do Brasil, mercado brasileiro, preferência por fontes brasileiras, comunidades, vídeos, comentários, fóruns brasileiros e páginas que descrevam a rotina do nicho no Brasil. CBO, tabelas salariais, páginas institucionais e descrições oficiais devem ser apoio secundário, não o eixo dominante da pesquisa.
- Quando a pesquisa cair em `NEEDS_MORE_RESEARCH` ou `GENERIC`, a UI deve oferecer saída operacional simples para o usuário abrir nova pesquisa manual do mesmo CNAE, criando um novo ciclo rastreável sem avançar material fraco para hipótese/oferta.
- A etapa `oprmNicheResearchSeedBuilder` deve rejeitar objetivos e termos que direcionem busca por produto, oferta, campanha ou solução quando não fizerem parte literal da descrição CNAE.
- A etapa `oprmSourceFetcher` deve propagar para o snapshot curto a classificação da fonte definida na busca: `sourceIntent`, `routineEvidenceScore`, `commercialPageRisk` e `solutionLanguageRisk`, sem armazenar HTML completo.

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
- A tela `/oprm/pipeline` deve orientar o usuário de que o NichoCNAE está levantando realidade operacional no Brasil e não montando solução ou campanha nessa etapa; ciclos em `FAILED`, `NEEDS_MORE_RESEARCH` ou `GENERIC` devem oferecer ação de novo ciclo manual/reprocessamento.

## Regra obrigatória — materialização final do NichoCNAE em nicho enriquecido

- Após o `oprmRoutineQualityGate` aprovar um cartão com `readyForHypothesis=true`, o pipeline OPRM NichoCNAE deve executar uma etapa final chamada `oprmEnrichedNicheMaterializer`.
- A etapa final só pode liberar materialização, estratégia ou próximos fluxos comerciais quando existir `oprm_mei_audience_profile` rastreável para o `research_cycle_id` ou, em retentativas controladas, para o `market_niche_id` já materializado; sem esse perfil, o ciclo deve permanecer operacionalmente como “Aguardando perfil MEI/autônomo”.
- Essa etapa final deve alimentar obrigatoriamente duas estruturas persistidas: a tabela principal de nichos (`market_niche`) e a tabela de nicho enriquecido (`market_niche_enrichment_profile`).
- A materialização final não deve criar hipótese, experimento, oferta, campanha ou landing page; hipótese será tratada em fluxo próprio posterior.
- O `market_niche` deve receber o cadastro operacional do nicho com nome neutro, descrição enriquecida, segmentação base e contexto de uso, sem inventar uma oferta; o nome original contaminado deve ficar apenas como auditoria.
- O `market_niche_enrichment_profile` deve preservar apenas dados compatíveis com pesquisa de rotina real vindos do NichoCNAE: CNAE, score OPRM, ciclo de pesquisa, cartão de rotina, nome original para auditoria, nome neutro, modo de pesquisa, rotina, tarefas recorrentes, dificuldades, perguntas, contexto operacional, linguagem pública, evidências, fontes, scores de qualidade e risco de contaminação por linguagem de solução.
- Campos legados de materialização com nomes comerciais devem receber somente conteúdo operacional compatível ou valor funcional de reprocessamento neutro; não podem eternizar hipótese, oferta, produto, curso, ferramenta, automação ou oportunidade de solução como verdade da pesquisa inicial.
- O vínculo entre OPRM e nicho comercial deve permanecer rastreável por `research_cycle_id`, `routine_card_id`, `source_niche_candidate_id` e `market_niche_id`.
- A conclusão da etapa deve atualizar o ciclo para `ENRICHED_NICHE_CREATED`; falhas devem registrar status `ENRICHED_NICHE_FAILED` e mensagem operacional no ciclo.

## Critério de efetividade — nicho enriquecido materializado

- Um cartão aprovado pelo gate só pode ser considerado finalizado quando existir pelo menos um registro correspondente em `market_niche_enrichment_profile` e um `market_niche_id` persistido.
- É permitido gerar múltiplos registros em `market_niche_enrichment_profile` para o mesmo `market_niche_id`, inclusive para o mesmo `routine_card_id` ou `research_cycle_id`, quando houver reprocessamento operacional; cada registro deve preservar a auditoria histórica da execução que o criou.
- Reprocessamentos devem reaproveitar e atualizar o `market_niche` existente de forma controlada, criando novo perfil enriquecido rastreável em vez de bloquear a execução por idempotência rígida.
- Quando já existir `market_niche_id` associado ao candidato, ao perfil MEI/autônomo ou a materialização anterior do mesmo CNAE/nome neutro, a etapa final deve atualizar esse nicho de forma controlada em vez de criar duplicata.
- A tela `/oprm/pipeline` deve exibir a etapa final e informar o `marketNicheId`, o `enrichedNicheProfileId` e o status de materialização.
- Ciclos em `ENRICHED_NICHE_FAILED` devem oferecer ação operacional pelo próprio front-end para abrir novo ciclo rastreável; o usuário não deve ser orientado a corrigir esse caso por acesso direto ao banco de dados.
- O contrato da etapa final deve seguir o padrão de unidade de trabalho fechada: o endpoint `pending` precisa entregar todos os dados necessários para o coletor concluir a materialização sem buscar detalhes adicionais.

## Regra obrigatória — pipeline oficial OPRM NichoCNAE

- O pipeline oficial de pesquisa e materialização de NichoCNAE deve usar o código operacional `oprm-nicho-cnae-pipeline` e o módulo `OPRM`.
- O contrato estrutural do pipeline deve refletir as etapas já implementadas no backend e no coletor OPRM: `routine-research-orchestrator`, `routine-research-cycle`, `niche-research-seed-builder`, `source-searcher`, `source-fetcher`, `signal-extractor`, `routine-synthesizer`, `mei-audience-segmenter`, `routine-quality-gate` e `enriched-niche-materializer`.
- Todas as etapas do pipeline oficial devem permanecer obrigatórias, ativas e executadas pelo módulo `oprm-coletor-mei`, consumindo exclusivamente endpoints OPRM do backend principal.
- Somente as etapas `niche-research-seed-builder` e `mei-audience-segmenter` devem ser marcadas como consumidoras diretas de modelo OpenAI configurável. As demais etapas são orquestração, consulta, coleta pública, extração/síntese/gate determinísticos ou materialização baseada em dados já coletados, e não devem exibir seleção de modelo OpenAI na tela administrativa quando não houver modelo operacional legado configurado.
- Todo campo textual sintético persistido pelo cartão de rotina do pipeline OPRM NichoCNAE, incluindo `mechanismOpportunitiesSummary`, deve usar coluna `LONGTEXT` no banco e respeitar limite operacional máximo de 20000 caracteres antes do envio ao backend. Quando uma etapa usar modelo de IA para gerar qualquer campo persistido com esse limite, o prompt deve declarar explicitamente o limite máximo de 20000 caracteres por campo e a camada determinística deve validar ou compactar o conteúdo antes de concluir a etapa.
- Antes de expor pendência para a etapa `mei-audience-segmenter`, o backend deve validar o cartão `oprm_niche_routine_card`: `routine_evidence_score` e `difficulty_evidence_score` devem ser maiores que zero e os textos essenciais de rotina/dor/evidência não podem estar vazios nem conter marcador como “Sem evidência suficiente”. Quando essa evidência mínima de dor prática estiver ausente, o ciclo deve ser bloqueado para nova pesquisa/aprofundamento com status operacional de necessidade de pesquisa, e o motivo registrado deve ser “cartão sem evidência mínima de dor prática”, sem tratar como falha genérica.
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

## Regra obrigatória — snapshot canônico fixo para operação

- Para operação atual da ingestão OPRM CNPJ/CNAE, o `snapshotDate` canônico deve permanecer **fixo em `2026-05-10`**.
- É proibido alterar automaticamente a data do snapshot para diretórios mais novos durante execução agendada ou manual sem decisão explícita do usuário.
- Qualquer tentativa de execução com `snapshotDate` diferente de `2026-05-10` deve ser tratada como não conformidade operacional e registrada em log com causa-raiz.

## Critério de efetividade — snapshot fixo

- Logs de criação de run devem mostrar explicitamente `snapshotDate=2026-05-10`.
- A base de download deve ser explicitamente `https://dados-abertos-rf-cnpj.casadosdados.com.br/arquivos/2026-05-10/` enquanto essa regra estiver vigente.
- Antes de iniciar a execução agendada/manual, deve haver validação de acesso HTTP (ex.: `HEAD`) para os arquivos de referência do snapshot (mínimo: `Cnaes.zip`, `Empresas1.zip`, `Estabelecimentos1.zip`) com retorno `200`.

## Regra obrigatória — ranking de CNAEs por score OPRM com paginação

- A listagem de ranking de CNAEs por volume (`/oprm/cnaes-volume`) deve ser sempre ordenada por **Score OPRM** em ordem decrescente (maior para menor), mantendo os dados de volume como contexto operacional da oportunidade.
- O endpoint de leitura do ranking deve suportar paginação explícita (parâmetros de página e tamanho), evitando retorno massivo em uma única resposta.
- O tamanho padrão por página para essa visão operacional deve ser de **50 registros por página**.
- A ordenação por Score OPRM deve ser aplicada no backend (SQL/consulta), não no frontend por pós-processamento em memória.

## Critério de efetividade — ranking paginado

- A primeira página precisa trazer os CNAEs com maior `Score OPRM` no snapshot vigente da ingestão.
- Ao navegar entre páginas, a ordenação deve permanecer estável por `Score OPRM` decrescente.
- O texto de apoio na tela deve deixar explícito para o usuário que o ranking está ordenado por Score OPRM e paginado.

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
- O padrão arquitetural obrigatório para essas etapas é o documento `docs/metodologia/gerado-5-5/arquitetura-pipeline-etapas-archunit.md`.
- Cada etapa concreta deve permanecer em pacote próprio, plugável e removível, dependendo apenas do núcleo genérico do pipeline, infraestrutura compartilhada permitida e contratos persistidos/DTOs oficiais.
- É proibido acoplar uma etapa concreta a outra etapa concreta para avançar o fluxo; o encadeamento deve ocorrer por contratos persistidos, artefatos auditáveis, endpoints do backend ou outro contrato oficial.
- O backend principal permanece como fonte de verdade dos dados e contratos; o módulo executor OPRM deve consumir e concluir etapas por endpoints do próprio OPRM/backend, sem acesso direto ao banco.
- Chamadas ao modelo, prompts, validações e mapeamento de resposta devem ficar encapsulados no pacote da etapa concreta que usa IA, preservando isolamento, rastreabilidade e testabilidade.
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
