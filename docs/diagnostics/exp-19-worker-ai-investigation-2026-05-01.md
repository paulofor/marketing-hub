# Diagnóstico — Experimento 19 não disponível para Worker AI (2026-05-01)

## Resultado

O experimento **19** possui dois jobs novos pendentes em `WAITING_AI_WORKER` desde `2026-05-01 13:30:32`:

- `CAMPAIGN_ANGLE`
- `AD_COPY`

Também há histórico de falhas 422 em execuções anteriores da esteira (wireframe/copy da landing), mas **o bloqueio atual** é que os jobs recém-criados não foram consumidos pelo Worker AI até o momento da análise.

## Evidências coletadas

- `experiment.id=19` existe e está com status `PLANNED`.
- Filas pendentes globais (`status='PENDING' AND stage='WAITING_AI_WORKER'`) mostram apenas 3 jobs: 2 do experimento 19 e 1 antigo do experimento 13.
- Para o experimento 19, os dois jobs pendentes são os mais recentes e continuam sem transição de status.

## Hipótese de causa raiz operacional

Indica problema de consumo no lado do Worker AI (scheduler/processo não executando ou sem alcançar o backend) e não ausência de jobs no banco.

## Ação recomendada

1. Verificar saúde/runtime do container do Worker AI e scheduler de pipeline.
2. Conferir conectividade do Worker AI com backend principal e credenciais.
3. Após estabilização, reprocessar jobs pendentes do experimento 19.
4. Em paralelo, tratar falhas 422 históricas de contrato para reduzir retrabalho na esteira.


## Correção com base no log enviado (timeline)

Pelos logs do Worker AI enviados, entre **2026-05-01T09:36:00Z** e **2026-05-01T09:42:00Z** o scheduler do pipeline rodou normalmente e registrou repetidamente:

- `Experiment pipeline worker cycle started`
- `Experiment pipeline worker found 0 pending job(s)`

Isso não contradiz o banco: os jobs do experimento 19 em `WAITING_AI_WORKER` que analisamos foram criados às **2026-05-01 13:30:32 (UTC)**, ou seja, **depois** da janela de log compartilhada (09:36–09:42 UTC).

Conclusão ajustada: naquele trecho de log realmente não havia pendências; o problema passa a ser investigar o período **após 13:30:32 UTC** para confirmar se o worker continuou consumindo normalmente.

## Próximo passo objetivo

1. Coletar logs do Worker AI imediatamente após **2026-05-01T13:30:32Z**.
2. Verificar se continuam mensagens `found 0 pending job(s)` mesmo com pendência em banco.
3. Se sim, validar endpoint/backend usado pelo worker para busca de jobs pendentes (filtro/critério de consulta).
