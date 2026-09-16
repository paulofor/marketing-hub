# Reparo do status candidato do slot PDE

## Evidência de produção

- O backend publicado no commit `a7e8db2a169180a84aee8bbde37817c9b60a74ae` continuou retornando
  HTTP 500 em `GET /api/business-process-chains/learning-cycles/v1/products/4?chainId=14`.
- O stack trace registrou `No enum constant com.marketinghub.pde.PdeProductionSlotStatus.` ao ler
  o slot do experimento #92.
- O schema real possuía `ENUM('PLANNED','READY','ACTIVE','PAUSED','RETIRED')`; o registro v8 estava
  com `status = ''`, enquanto o código e a migração da candidata exigem `CANDIDATE`.

## Causa e decisão

O `ENUM` físico havia ficado atrás do contrato Java. Acrescentar somente `CANDIDATE` ao `ENUM`
resolveria este caso, mas repetiria o risco a cada novo estado. Aceitar string vazia no Java
mascararia dado inválido. O reparo escolhido restaura `VARCHAR(32)`, já definido pelo changelog
canônico de criação da tabela, e corrige somente o slot v8 ainda não publicado do experimento #92.

## Matriz local

| Cenário | Critério |
|---|---|
| Schema legado | Reproduzir `CANDIDATE` convertido em string vazia no MySQL 5.7 não estrito |
| Reparo | Converter a coluna para `VARCHAR(32)` e recuperar `CANDIDATE` |
| Idempotência | Reaplicar sem duplicar nem alterar a candidata |
| Preservação | Manter v7 e evidências publicadas intactas |
| Promoção | Não rebaixar slot já promovido para `READY` |
| Tela | Após publicação, endpoint dos ciclos deve responder HTTP 200 e exibir o ciclo #2 |

O último critério depende da publicação por PR; nenhum ajuste manual de produção faz parte desta
homologação.

## Resultado local

- Validador estático Liquibase/MySQL 5.7: aprovado.
- Matriz física em `mysql:5.7`: 3 testes aprovados, incluindo reprodução da conversão de
  `CANDIDATE` para string vazia, reparo, reaplicação e preservação da v7.
- Spotless e revisão do diff: aprovados.
- A primeira tentativa da matriz não alcançou o banco porque usou `127.0.0.1`; a repetição com o
  host correto da engine isolada, `sandbox-docker`, executou e aprovou todos os cenários.
