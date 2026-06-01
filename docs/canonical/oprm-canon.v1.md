# OPRM Canon v1 — Ingestão de CNAE e Totalização de Market Size

## Propósito

Este documento define regras canônicas específicas do módulo OPRM para a ingestão CNPJ/CNAE e consolidação de market size por CNAE.

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
