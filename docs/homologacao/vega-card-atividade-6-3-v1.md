# Card de Vega orientado à atividade 6.3

Data: 25/09/2026. Escopo: card do ciclo na tela `/products`, contratos de leitura e navegação.

## Causa confirmada e decisão

A tela pública mostrava o experimento #92 em “Medir vendas e valor entregue”, mas ocultava a
atividade operacional. A API de atividades, a posição oficial e o banco confirmaram o ciclo #2
aberto na cadeia original #14, Processo 6 definição #75 e atividade bloqueada
**6.3 — Consolidar resultado comercial**, responsável Backend. `process-context.nextWork` era nulo
porque não havia delegação a outro processo; o frontend caía numa mensagem genérica e descartava
a atividade já presente no `salesFlow`.

| Alternativa                                                  | Benefício                                     | Risco / esforço                                  | Decisão   |
| ------------------------------------------------------------ | --------------------------------------------- | ------------------------------------------------ | --------- |
| Usar o número do processo da cadeia atual                    | Alteração somente visual                      | Mistura a cadeia v22 com o ciclo histórico v14   | Rejeitada |
| Criar outro endpoint para o card                             | Contrato específico                           | Duplica posição, ciclo e atividade já existentes | Rejeitada |
| Enriquecer o `salesFlow` original e apresentá-lo no fallback | Preserva identidade e resolve todos os ciclos | Pequena evolução de backend e frontend           | Escolhida |

## Matriz de homologação definida antes dos testes

| Critério              | Aceite                                                                       |
| --------------------- | ---------------------------------------------------------------------------- |
| Caso original         | Vega mostra 6.3, nome, Backend, estado bloqueado e motivo persistido         |
| Identidade            | Processo #75 e cadeia #14 permanecem no link mesmo com posição atual #97/v22 |
| Caminho com delegação | `nextWork` continua prevalecendo quando outro processo é responsável         |
| Outros estados        | Atividade interna em andamento e ciclo encerrado permanecem distintos        |
| Falhas                | Erro, resposta divergente e retentativa continuam explícitos                 |
| Integração            | Somente GET; nenhuma tarefa, conciliação, campanha ou gasto                  |
| Dispositivos          | Desktop, iPhone 15 Pro e Pixel 7, sem overflow e com destino acessível       |
| Regressão             | Testes Java/React, tipagem, build, formatação e diff aprovados               |

## Resultado

- Backend: 3.525 testes aprovados, 22 condicionais não executados; Spotless aprovado.
- Frontend: 774 testes aprovados; tipagem, build e formatação aprovados.
- Regressão focada: 14 testes Java e 16 testes React aprovaram a identidade histórica,
  a precedência de `nextWork`, a atividade interna, os estados e a ausência de escrita.
- Navegador local: desktop, iPhone 15 Pro e Pixel 7 aprovaram o card, o destino 6.3,
  falhas/retentativa e os demais produtos; zero requisições de escrita.
- Contrato: Swagger parseável e `git diff --check` aprovado.

A confirmação publicada será vinculada ao PR, SHA da `main`, deploy e capturas de produção.
