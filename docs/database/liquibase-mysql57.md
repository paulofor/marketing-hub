# Validação estática de Liquibase para MySQL 5.7

O workflow `.github/workflows/liquibase-mysql57.yml` executa somente a validação estática dos changelogs do backend destinados ao MySQL 5.7.

Ele é executado automaticamente em Pull Requests que alteram changelogs, o validador estático ou o próprio workflow. Também pode ser iniciado manualmente pelo GitHub Actions.

## Etapa executada no workflow

Executar `scripts/validate-liquibase-mysql57.sh` para verificar includes relativos, includes duplicados, dependências conhecidas, campos temporais e risco do erro MySQL 1093 nos arquivos alterados.

O workflow não inicia MySQL, não executa `liquibase:update` e não usa banco, credenciais ou dados de produção. A compatibilidade física de uma migração continua sendo responsabilidade da homologação controlada do ambiente antes da publicação em produção.

## Execução manual após publicar uma branch

```bash
gh workflow run liquibase-mysql57.yml --ref <branch>
gh run list --workflow liquibase-mysql57.yml
gh run watch <run-id> --exit-status
```

Antes de publicar uma alteração, execute localmente as verificações possíveis. O workflow do Pull Request comprova os contratos estáticos e executa a matriz física dedicada das migrações críticas que possuam fixture MySQL 5.7 versionada; os demais changelogs ainda exigem o runner físico específico quando indicado na homologação.

Cada job físico prepara `mysql:5.7` com até três tentativas antes de iniciar a fixture. O retry é
restrito ao download da imagem base para tolerar falhas transitórias de rede ou do registry; migração,
assertivas e rollback continuam executados uma única vez e qualquer erro funcional falha imediatamente.

A matriz dos nove agentes possui fixture própria e valida incremento dinâmico de `agent_version`,
onze processos sem coautoria, a inclusão operacional de Íris, gates independentes de Psique e
Têmis, cadeias de valor v8/v9, paridade entre `agent.current_version` e as versões declaradas pelos
executores no contrato canônico de health, a evolução estética v4 de Psique com snapshot e paths
auditáveis, além de reaplicação sem duplicidade:

```bash
bash backend/ads-service/scripts/validate-agent-responsibility-boundaries-mysql57.sh
```

A auditoria acionável e visual das tarefas possui fixture física própria. Ela valida as colunas
universais, os campos `DATETIME`, o vínculo segregado dos links, os snapshots de Psique com
identificadores ASCII, as partes explícitas dos prompts em `agent_task` e nas execuções técnicas de
landing, exclusão em cascata e reaplicação sem duplicidade:

```bash
bash backend/ads-service/scripts/validate-agent-task-actionable-audit-v2-mysql57.sh
```

Os tipos Consultor PWA e Consultor WhatsApp possuem fixture física própria. Ela preserva a Fluorita
e seu produto vinculado, cria a Turmalina, aplica o enriquecimento v2 baseado nas pesquisas, valida
os treze campos da base, os SDKs por canal, microvalor, confiança e a reaplicação retomável sem
duplicar apelidos:

```bash
bash backend/ads-service/scripts/validate-product-type-consultants-v1-mysql57.sh
```

A auditoria BPM da descoberta PDE também valida fisicamente o handoff autônomo. A fixture confirma
o retroativo de maturidade factual, vínculos únicos entre candidata, dossiê e produto, chaves
estrangeiras, retomada após DDL aplicado sem registro, rollback e reaplicação:

```bash
bash backend/ads-service/scripts/validate-product-discovery-bpm-audit-mysql57.sh
```

A amostra individual consentida possui fixture física mínima. Ela valida `DATETIME NOT NULL`, chave
estrangeira, deduplicação por experimento e reaplicação do changelog:

```bash
bash backend/ads-service/scripts/validate-experiment-direct-contact-sample-mysql57.sh
```

O recrutamento inbound consentido possui fixture própria. Ela valida as três tabelas, todos os
campos temporais como `DATETIME`, chaves estrangeiras, deduplicação de visita e pessoa, retomada após
DDL sem ledger, rollback e reaplicação:

```bash
bash backend/ads-service/scripts/validate-experiment-direct-recruitment-mysql57.sh
```

As consultas desse runner declaram `--default-character-set=utf8mb4`. Sem o charset explícito, o
cliente MySQL 5.7 pode não comparar nomes acentuados como `Dédalo` e `Têmis` com o valor UTF-8
persistido, produzindo falso positivo ou falso negativo na homologação.

## DDL e backfill retomáveis

No MySQL 5.7, operações DDL podem permanecer aplicadas mesmo quando o processo é interrompido antes de o Liquibase registrar o changeset. Por isso, a criação de tabela e o backfill devem ficar em changesets separados. A criação deve aceitar retomada somente quando o schema esperado já existir, e o backfill deve ser idempotente, ignorando registros já materializados sem mascarar divergência estrutural. Quando um changeset já puder ter sido concluído em outro ambiente, preserve também o checksum anterior de forma explícita e teste essa compatibilidade.
