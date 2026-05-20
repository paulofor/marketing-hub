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
