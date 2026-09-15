# Actions dos ciclos: rollback de SQL bruto — 15/09/2026

## Escopo

Correção local do job **Validar ciclos de aprendizado e vendas no MySQL 5.7** do
workflow `liquibase-mysql57.yml`. Não houve commit, Pull Request, publicação, chamada
de IA paga ou alteração de dados de produção.

## Evidência da causa

- O run [34922782388](https://github.com/paulofor/marketing-hub/actions/runs/34922782388)
  falhou durante `verify-and-rollback` com `RollbackImpossibleException: No inverse to
  liquibase.change.core.RawSQLChange created`.
- O artefato do job mostra que o primeiro changeset revertido era
  `2026-09-11-vega-private-prototype-v1`, ao voltar pelo marco
  `2026-09-12-product-process-automation-v1`.
- A consulta somente leitura ao banco confirmou que Vega já está aplicado com checksum
  `9:31fe8d99dcb8790c102ee06a17f28258`. O checksum calculado após a correção permaneceu
  idêntico.

## Decisão

Foram comparadas três alternativas:

1. Pular Vega ou reduzir o rollback: esconderia a falha e deixaria de provar a reversão
   da automação.
2. Apenas reordenar a fixture: a próxima reversão por marco voltaria a atravessar Vega
   e falharia no mesmo `RawSQLChange`.
3. Declarar o rollback de Vega, preservar seu checksum e testar esse contrato: mantém a
   migração histórica compatível e a matriz de rollback completa. Esta foi a escolha.

## Correção aplicada na sandbox

- O changeset de Vega remove primeiro a tabela filha
  `vega_adjustment_execution_v1` e depois `vega_private_session_v1` no rollback.
- `LearningCycleMigrationVerifierTest` bloqueia qualquer `RawSQLChange` sem rollback
  na fixture e protege o checksum histórico de Vega.
- O runner de persistência executa esse contrato antes da integração física.

## Matriz local executada

O Docker da sandbox isola portas publicadas do host; por isso a execução local usou o
host permitido `sandbox-docker`, enquanto o Actions mantém `127.0.0.1` no runner
GitHub. A diferença é de rede da sandbox, não uma alteração de produto ou do workflow.

Após a última correção, duas rodadas consecutivas e completas para este job passaram:

| Rodada | Resultado |
| --- | --- |
| `learning-cycles-final-round1-aA17so` | Aprovada |
| `learning-cycles-final-round2-8eqVEE` | Aprovada |

Cada rodada aprovou validação estática Liquibase, Spotless, compilação, contratos de
rollback/checksum, testes de Atena, analytics PDE em MySQL, API REST, contexto do ciclo,
migração, rollback, reaplicação, idempotência e revisão de diff.
