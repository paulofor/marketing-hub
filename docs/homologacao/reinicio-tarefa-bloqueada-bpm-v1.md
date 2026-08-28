# Matriz de homologação — reinício de tarefa bloqueada no BPM v1

## Objetivo e limites

Comprovar localmente que uma atividade de agente em `BLOCKED` oferece **Reiniciar tarefa**, abre uma
nova tentativa auditável e preserva integralmente a tentativa anterior. A homologação usa test
doubles e dados segregados; não chama modelo externo, não publica, não altera orçamento e não gera
gasto.

## Critérios ponta a ponta

| Área | Cenário | Resultado esperado |
| --- | --- | --- |
| Caminho feliz | Backend retorna atividade `BLOCKED`, produto em `PLAY`, processo publicado e experimento válido | `executionRequestAvailable=true` e motivo informa que o histórico será preservado |
| Interface | Abrir o histórico do processo com uma tarefa bloqueada | Botão **Reiniciar tarefa** com ícone de reinício visível na própria atividade |
| Comando | Acionar **Reiniciar tarefa** | `POST .../execution-requests` usa produto, processo e atividade exatos |
| Carregamento | Requisição em andamento | Botão fica desabilitado, mostra spinner e texto **Reiniciando...** |
| Persistência | Nova tentativa aceita | Tentativa anterior continua `BLOCKED`; nova tentativa fica `PENDING` na mesma instância e referência |
| Idempotência | Repetir o comando com tentativa nova ainda ativa | A tarefa `PENDING` é reutilizada; nenhuma tarefa ou custo é duplicado |
| Estados inelegíveis | Atividade `PENDING`, `IN_PROGRESS` ou `COMPLETED` | Botão ausente e chamada direta recusada pelo backend |
| Governança | Processo não publicado, produto em `STOP`, experimento ausente ou readiness reprovado | Botão ausente e motivo real vem do backend |
| Falha HTTP | Backend recusa ou falha | Tela mantém o histórico e apresenta erro sem simular reinício |
| Segregação | Rigel e Vega possuem tarefas bloqueadas distintas | Cada comando usa seu próprio `productId`, experimento, instância e tarefas |
| Observabilidade | Consultar atividade depois do comando | Estado, tentativas, erro anterior, datas, tokens e custo continuam auditáveis |
| Responsividade | Desktop, iPhone 15 Pro e Pixel 7 | Botão acessível, sem overflow e sem ocultar status ou causa do bloqueio |

## Métricas e decisão

- **Esperado:** 100% das atividades bloqueadas elegíveis com comando visível; zero tarefas ativas
  duplicadas; zero perda de histórico; zero mistura entre produtos; zero efeito externo.
- **Continuar:** todos os critérios passam em uma rodada completa quando nenhum defeito for revelado.
- **Ajustar:** qualquer divergência de estado, feedback, responsividade ou auditoria exige correção e
  reinício da matriz; após a última correção, executar duas rodadas completas consecutivas.
- **Parar:** qualquer possibilidade de apagar auditoria, reiniciar atividade concluída, misturar
  produtos, publicar ou gerar gasto sem autorização.
