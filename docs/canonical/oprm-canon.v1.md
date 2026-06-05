# OPRM Canon v1 — Ingestão de CNAE e Totalização de Market Size

## Propósito

Este documento define regras canônicas específicas do módulo OPRM para a ingestão CNPJ/CNAE, consolidação de market size por CNAE e pesquisa inicial NichoCNAE de realidade operacional.

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
- O objetivo exclusivo desse modo é compreender a realidade operacional do nicho a partir do CNAE: rotina, tarefas, dificuldades, perguntas recorrentes, contexto de trabalho, linguagem orgânica e evidências públicas.
- A pesquisa deve parar na descrição realista da rotina e das dificuldades; ela não deve procurar, inferir, preparar ou validar solução, produto, campanha, oferta, promessa comercial, landing page, hipótese de experimento ou tese de mecanismo comercial.
- Termos de solução, como IA, automação, sistema, app, software, ferramenta, curso ou marketing digital, não podem direcionar a pesquisa inicial quando vierem apenas de enquadramento comercial do nome do candidato.
- O nome operacional usado para pesquisa deve representar o nicho/CNAE de forma neutra; nomes originais contaminados por linguagem de solução devem ser preservados apenas para auditoria e não como fonte de direcionamento da busca.
- A etapa inicial deve priorizar evidências públicas sobre execução do trabalho, problemas operacionais, dúvidas reais, atividades, responsabilidades, limitações, sazonalidade, riscos, custos, retrabalho, gargalos e vocabulário usado pelo próprio mercado.
- A etapa `oprmNicheResearchSeedBuilder` deve aceitar somente objetivos de query compatíveis com rotina real: `ROUTINE_DISCOVERY`, `ROUTINE_TASK_DISCOVERY`, `OPERATIONAL_DIFFICULTY_DISCOVERY`, `NICHE_OWNER_QUESTION_DISCOVERY`, `FINAL_CUSTOMER_QUESTION_DISCOVERY`, `LANGUAGE_DISCOVERY` e `OPERATIONAL_CONTEXT_DISCOVERY`.
- A etapa `oprmNicheResearchSeedBuilder` deve rejeitar objetivos e termos que direcionem busca por produto, oferta, campanha ou solução quando não fizerem parte literal da descrição CNAE.

## Critério de efetividade — pesquisa inicial NichoCNAE sem viés de solução

- Ciclos de pesquisa inicial devem expor ou persistir o modo `ROUTINE_REALITY_RESEARCH` para rastreabilidade operacional.
- Seeds, queries, fontes, sinais, sínteses e gates da fase inicial devem ser aceitos somente quando servirem à compreensão da rotina real e das dificuldades do nicho.
- Qualquer conteúdo de solução, produto, campanha, oferta ou hipótese comercial deve pertencer a fluxo posterior, separado e rastreável, depois da conclusão da pesquisa de realidade operacional.
- A tela `/oprm/pipeline` deve orientar o usuário de que o NichoCNAE está levantando realidade operacional e não montando solução ou campanha nessa etapa.

## Regra obrigatória — materialização final do NichoCNAE em nicho enriquecido

- Após o `oprmRoutineQualityGate` aprovar um cartão com `readyForHypothesis=true`, o pipeline OPRM NichoCNAE deve executar uma etapa final chamada `oprmEnrichedNicheMaterializer`.
- Essa etapa final deve alimentar obrigatoriamente duas estruturas persistidas: a tabela principal de nichos (`market_niche`) e a tabela de nicho enriquecido (`market_niche_enrichment_profile`).
- A materialização final não deve criar hipótese, experimento, oferta, campanha ou landing page; hipótese será tratada em fluxo próprio posterior.
- O `market_niche` deve receber o cadastro operacional do nicho com nome, descrição enriquecida, segmentação base e contexto de uso, sem inventar uma oferta.
- O `market_niche_enrichment_profile` deve preservar os dados enriquecidos vindos do NichoCNAE: CNAE, score OPRM, ciclo de pesquisa, cartão de rotina, rotina, dores, resultados, oportunidades de mecanismo, evidências, fontes e scores de qualidade.
- O vínculo entre OPRM e nicho comercial deve permanecer rastreável por `research_cycle_id`, `routine_card_id`, `source_niche_candidate_id` e `market_niche_id`.
- A conclusão da etapa deve atualizar o ciclo para `ENRICHED_NICHE_CREATED`; falhas devem registrar status `ENRICHED_NICHE_FAILED` e mensagem operacional no ciclo.

## Critério de efetividade — nicho enriquecido materializado

- Um cartão aprovado pelo gate só pode ser considerado finalizado quando existir um registro correspondente em `market_niche_enrichment_profile` e um `market_niche_id` persistido.
- A etapa deve ser idempotente por `routine_card_id` e `research_cycle_id`, evitando duplicar nichos enriquecidos quando houver retentativa do coletor.
- A tela `/oprm/pipeline` deve exibir a etapa final e informar o `marketNicheId`, o `enrichedNicheProfileId` e o status de materialização.
- O contrato da etapa final deve seguir o padrão de unidade de trabalho fechada: o endpoint `pending` precisa entregar todos os dados necessários para o coletor concluir a materialização sem buscar detalhes adicionais.

## Regra obrigatória — pipeline oficial OPRM NichoCNAE

- O pipeline oficial de pesquisa e materialização de NichoCNAE deve usar o código operacional `oprm-nicho-cnae-pipeline` e o módulo `OPRM`.
- O contrato estrutural do pipeline deve refletir as etapas já implementadas no backend e no coletor OPRM: `routine-research-orchestrator`, `routine-research-cycle`, `niche-research-seed-builder`, `source-searcher`, `source-fetcher`, `signal-extractor`, `routine-synthesizer`, `routine-quality-gate` e `enriched-niche-materializer`.
- Todas as etapas do pipeline oficial devem permanecer obrigatórias, ativas e executadas pelo módulo `oprm-coletor-mei`, consumindo exclusivamente endpoints OPRM do backend principal.
- O pipeline oficial deve preservar a separação canônica: pesquisa de realidade operacional e materialização de nicho enriquecido não podem criar hipótese, experimento, oferta, campanha ou landing page.

## Critério de efetividade — pipeline oficial OPRM NichoCNAE

- A tela administrativa de pipelines deve reconhecer `oprm-nicho-cnae-pipeline` como contrato oficial com nove etapas esperadas.
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
- O usuário não deve precisar solicitar manualmente a geração de score de oportunidade; o OPRM deve processar CNAEs sem score ou com score vencido por execução agendada.
- O enriquecimento de CNAEs com melhor score deve ocorrer em execução agendada separada do cálculo de score, para permitir controle operacional, retentativa e auditoria independentes.
- O backend deve atuar somente como camada de API e persistência para esse fluxo: leitura, gravação, paginação, filtros e validação técnica de contrato. É proibido colocar no backend cálculo de score, enriquecimento, chamada a integrações externas ou regra de negócio de priorização CNAE.
- Cada execução agendada desse fluxo deve registrar identificadores de ciclo, no mínimo `cycleId`, `cycleType` e `cycleNumber`, e os logs devem incluir esses identificadores junto com o `cnaeCode` quando aplicável.

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

## Critério de efetividade — execução modular das etapas NichoCNAE

- Alterações em etapas OPRM NichoCNAE com IA devem citar o pacote da etapa concreta no `oprm-coletor-mei` e o contrato backend consumido/concluído.
- Testes de arquitetura/ArchUnit devem proteger que o núcleo do pipeline não dependa de etapas concretas e que etapas concretas não dependam entre si.
- A implementação ou revisão de uma etapa deve validar se o avanço para a próxima etapa ocorre por dados/contratos oficiais, e não por chamada direta entre classes de etapas.

## Regra obrigatória — publicação do executor OPRM com acesso à chave OpenAI

- O `oprm-coletor-mei`, quando executar etapas NichoCNAE com IA, deve ser publicado no mesmo host do `ai-worker`: `191.252.120.96`.
- A publicação no mesmo host permite reutilizar o arquivo de chave OpenAI já provisionado para o `ai-worker`, sem duplicar segredo em outro VPS.
- O arquivo de chave OpenAI deve ser montado no container OPRM como somente leitura em `/run/secrets/openai_api_key`, usando por padrão o caminho de host `/root/infra/openai-token/openai_api_key`.
- A etapa OPRM deve preferir variável direta de chave quando explicitamente configurada, mas deve aceitar `OPRM_NICHO_CNAE_SEED_BUILDER_OPENAI_API_KEY_FILE`/`OPENAI_API_KEY_FILE` para leitura do segredo montado.

## Critério de efetividade — publicação e segredo OpenAI do executor OPRM

- O workflow de deploy do `oprm-coletor-mei` deve usar `DEPLOY_HOST=191.252.120.96` enquanto a chave OpenAI operacional estiver provisionada no host do `ai-worker`.
- O compose do `oprm-coletor-mei` deve montar o mesmo arquivo de chave OpenAI em modo `ro` e configurar a etapa `oprmNicheResearchSeedBuilder` para ler esse arquivo.
- É proibido commitar a chave OpenAI ou materializar o segredo em `.env` versionado; apenas o caminho do arquivo/volume pode ser versionado.
