# Vega v17 — controles operacionais próprios

## Causa e decisão

Têmis #864 aceitou as três revisões de Psique (#860, #862 e #863), mas não
encontrou prova individualizada de concorrência, limites e acesso da implementação
contextual v17. O relatório anterior da v14 usa outro serviço e não pode ser
promovido a prova da v17. A complementação de recuperação não cobre esses controles.

Entre reutilizar o relatório antigo, repetir toda a matriz visual e executar somente
os controles faltantes, escolhemos a terceira alternativa. Ela preserva as provas
aceitas e exercita controllers, transações, locks e MySQL 5.7 reais da implementação
atual. O cadastro de produto é simulado; os ciclos e sessões são persistidos localmente.

## Matriz definida antes da execução

| Critério | Aceite |
| --- | --- |
| Sessões concorrentes | 24 solicitações resultam em 18 sessões e seis recusas; revogação não devolve capacidade |
| Operação concorrente e replay | Oito pedidos com a mesma chave preservam uma operação; entrada divergente é recusada |
| Limite por missão | Duas operações distintas por missão; a terceira é recusada sem alteração persistida |
| Conclusão repetida | Missão, progresso, entradas, eventos e operações permanecem idênticos |
| Acesso inválido, expirado e revogado | Leituras e mutações recusadas, sem alteração do agregado |
| Contexto encerrado/incompatível | Nenhuma nova mutação; resultados anteriores permanecem auditáveis |
| Isolamento | Outra sessão não consulta nem altera a orientação da primeira |
| Identidades e compatibilidade | Duas identidades sintéticas independentes na v17; caminho anterior da v16 preservado |
| Efeitos externos | Zero chamadas a modelo, cobrança, publicação, campanha ou dados comerciais |
| Rastreabilidade | Resultado por critério, método, origem, identidade e hash; preservação dos recibos anteriores |

O gerador contextual é síncrono e determinístico; não há callback de provedor a
simular. O controle correspondente é a conclusão idempotente. A prova não renova
aprovações nem altera o experimento por si só. Uma nova revisão de Têmis permanece
necessária após completar os critérios.

## Resultado observado e correção

Na primeira execução, 13 dos 15 casos passaram. Nas duas identidades, um ciclo
encerrado ainda aceitava salvar a reflexão (HTTP 204 em lugar de 409). A validação
de contexto existia somente na geração; os outros comandos não a reutilizavam.
Não há evidência de exploração em produção.

A proteção compartilhada agora confere ciclo aberto, etapa, produto, experimento
e versão antes de iniciar, salvar, concluir, registrar sinais ou conceder acesso
de teste. Leitura histórica, replay da orientação e revogação permanecem disponíveis.
Após a correção, os 15 casos MySQL 5.7 e nove testes do serviço passaram.

Esta é uma correção de integridade do contexto privado de homologação. A interface,
oferta, regras de recomendação, conclusão sem comentário e experiência em contexto
válido permanecem iguais. Não renova aceites, muda hipótese ou concede autorização
comercial. A prova anterior de recuperação fica imutável e vinculada por hash.

O recibo `evidencias/vega-v17-operational/controls-result.json` contém 22 observações
executadas (11 controles em duas identidades), sem credenciais. O novo relatório
operacional combina essas observações com os três cenários de recuperação já
aceitos, preservando origem e horário de cada conjunto. Não simula callback externo:
esta implementação responde de forma síncrona e não chama provedor.

## Aprendizado reutilizável e limite

Antes, uma prova parcial era encaminhada à revisão sem controles da implementação
atual. Agora o contrato contextual exige concorrência, limites, acessos e encerramento
em duas identidades, e os testes MySQL rodam no CI. A métrica técnica é impedir
mutação inválida e nova revisão por ausência desses controles. Os 15 casos passam;
tempo de espera, conversão e contribuição ainda precisam de observação real.
Adotar somente com provas íntegras e revisão independente; reverter qualquer mudança
que altere o caminho válido, sem relaxar a recusa de contextos encerrados.

Validação local consolidada: 205 testes de backend/arquitetura, 17 do empacotador e
14 do workflow aprovados; pacote completo de evidências gerado. Bash e ShellCheck
do helper de pacote aprovado passaram. Os recibos de recuperação e a matriz visual
anterior permanecem preservados; não houve nova chamada paga durante os testes.
