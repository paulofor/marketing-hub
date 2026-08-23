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

Antes de publicar uma alteração, execute localmente as verificações possíveis. O workflow do Pull Request comprova os contratos estáticos, mas não comprova a aplicação física integral do changelog.
