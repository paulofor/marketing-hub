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

A matriz dos oito agentes possui fixture própria e valida incremento dinâmico de `agent_version`,
onze processos sem coautoria, gates independentes de Psique e Têmis, cadeia de valor v8 e
reaplicação sem duplicidade:

```bash
bash backend/ads-service/scripts/validate-agent-responsibility-boundaries-mysql57.sh
```

As consultas desse runner declaram `--default-character-set=utf8mb4`. Sem o charset explícito, o
cliente MySQL 5.7 pode não comparar nomes acentuados como `Dédalo` e `Têmis` com o valor UTF-8
persistido, produzindo falso positivo ou falso negativo na homologação.

## DDL e backfill retomáveis

No MySQL 5.7, operações DDL podem permanecer aplicadas mesmo quando o processo é interrompido antes de o Liquibase registrar o changeset. Por isso, a criação de tabela e o backfill devem ficar em changesets separados. A criação deve aceitar retomada somente quando o schema esperado já existir, e o backfill deve ser idempotente, ignorando registros já materializados sem mascarar divergência estrutural. Quando um changeset já puder ter sido concluído em outro ambiente, preserve também o checksum anterior de forma explícita e teste essa compatibilidade.
