# Protocolo Monitor — inclusão de pipeline versionado no Ops Monitor

## Objetivo

O Protocolo Monitor define o padrão obrigatório para colocar uma versão de pipeline sob acompanhamento operacional do Ops Monitor.

Ele deve ser usado sempre que uma versão de pipeline precisar aparecer na tela de monitoramento não apenas como container online/offline, mas como fluxo de negócio consumindo fila, avançando etapas e evitando pendências antigas invisíveis para a operação.

## Quando aplicar

Aplique este protocolo quando:

1. uma nova versão de pipeline entrar em produção ou piloto operacional;
2. uma versão existente passar a ser a versão ativa do fluxo;
3. houver risco de o container estar saudável enquanto a fila do pipeline não é consumida;
4. o usuário precisar diferenciar saúde HTTP do módulo e saúde real do pipeline.

## Regra obrigatória

Toda versão de pipeline monitorada deve declarar um sinal operacional canônico no Ops Monitor.

Esse sinal deve indicar, no mínimo:

- módulo executor responsável;
- versão do pipeline;
- tabela ou contrato backend que representa a fila/execução;
- status que caracteriza trabalho pendente;
- limite máximo aceitável de pendência sem consumo;
- severidade/criticidade do incidente;
- mensagem operacional com job, entidade de negócio e etapa parada;
- comportamento esperado na tela: `DEGRADED`, incidente aberto ou ambos.

## Separação de responsabilidades

- O backend principal continua sendo a fonte de verdade da fila, execução, status e relatório.
- O módulo executor continua responsável por agendamento, polling, consumo, retries e execução real das etapas.
- O Ops Monitor observa sinais persistidos e/ou heartbeats públicos; ele não deve executar etapa, decidir transição de pipeline ou corrigir fila.
- O frontend apenas apresenta a verdade consolidada pelo backend/monitor, sem inferir localmente se o pipeline está travado.

## Critério mínimo de implementação

Para cada versão de pipeline incluída no Protocolo Monitor, deve existir:

1. consulta ou endpoint capaz de localizar pendências antigas;
2. regra de degradação do módulo executor quando houver pendência acima do limite;
3. incidente sintético ou persistido com `rootSignal` estável;
4. teste unitário/contrato cobrindo incidente e status degradado;
5. registro em `docs/registros/<tema>.md` e `docs/registros/ops-monitor.md`;
6. se a regra for reutilizável por outras versões, documentação neste protocolo ou no documento canônico do pipeline.

## Nomenclatura recomendada

Use nomes explícitos e versionáveis:

- `ROOT_SIGNAL`: `<DOMINIO>_<PIPELINE>_<VERSAO>_QUEUE_STALE` ou nome equivalente já canônico;
- mensagem: deve citar job, entidade de negócio e etapa;
- status de tela: `DEGRADED` quando o módulo responde HTTP mas a fila do pipeline não avança.

## Exemplo vigente

O NichoCNAE v3 aplica este protocolo ao monitorar execuções `PENDING` antigas do backend.

Quando uma execução fica pendente por mais de 6 minutos, o Ops Monitor degrada `oprm-coletor-mei` e lista incidente `OPRM_NICHO_CNAE_V3_QUEUE_STALE`, mostrando job, CNAE e etapa parada.
