# Validação Liquibase no MySQL 5.7

O workflow `.github/workflows/liquibase-mysql57.yml` é a validação canônica dos changelogs do backend no MySQL 5.7.

Ele é executado automaticamente em Pull Requests que alteram changelogs, o validador estático ou o próprio workflow. Também pode ser iniciado manualmente pelo GitHub Actions.

## Etapas obrigatórias

1. Executar `scripts/validate-liquibase-mysql57.sh` para verificar includes relativos, campos temporais e risco do erro MySQL 1093 nos arquivos alterados.
2. Executar `liquibase:validate` sobre o changelog mestre.
3. Executar `liquibase:update` completo em uma instância efêmera `mysql:5.7`, criada vazia para cada job.

O workflow não usa banco, credenciais ou dados de produção.

## Execução manual após publicar uma branch

```bash
gh workflow run liquibase-mysql57.yml --ref <branch>
gh run list --workflow liquibase-mysql57.yml
gh run watch <run-id> --exit-status
```

Antes de publicar uma alteração, execute localmente as verificações possíveis. O workflow do Pull Request é a evidência obrigatória da aplicação integral no MySQL 5.7.
