# Matriz de homologação — partes do prompt nas tarefas v1

## Objetivo

Comprovar que toda nova tarefa executada por modelo preserva e apresenta separadamente a parte do
agente, a parte da atividade e o prompt integral, sem reconstruir dados históricos nem misturar
execuções, produtos ou agentes.

## Decisão

Foram comparadas três alternativas:

1. inferir a divisão por títulos ou marcadores no frontend: baixo esforço, mas frágil e sem valor de
   auditoria;
2. guardar apenas a divisão no frontend: boa apresentação, mas sem fonte persistida e incompatível
   com a regra de verdade da tela;
3. declarar as partes no executor, persistir no backend e validar a composição: maior esforço de
   contrato, com fidelidade, segregação e prevenção de recorrência.

A alternativa 3 é obrigatória porque o worker é a única origem que conhece a composição realmente
enviada ao modelo.

## Matriz

| Dimensão | Cenário | Resultado esperado |
| --- | --- | --- |
| Caminho feliz | Tarefa `MODEL` informa agente, atividade e prompt integral | terminalização aceita e três cards exibidos |
| Composição | Parte ausente, fora de ordem ou alheia ao prompt integral | backend rejeita antes de concluir ou bloquear |
| Legado | Tarefa antiga possui somente prompt integral | histórico preservado e cards indicam ausência legada |
| Determinístico | Tarefa sem modelo registra entrada integral | parte do agente é não aplicável e atividade recebe a entrada persistida |
| Falha pré-modelo | Execução `NOT_STARTED` | nenhuma parte fictícia é criada |
| Integrações | Cada executor que conclui tarefa envia as duas partes | contrato validado por testes do próprio módulo |
| Observabilidade | Consulta de tarefa, processo, documento e histórico por produto | os mesmos valores persistidos aparecem em todos os endpoints |
| Segregação | Duas tarefas de agentes/produtos distintos | nenhuma parte é compartilhada entre `taskId`s |
| Métrica | Cobertura das novas tarefas `MODEL` | 100% com três representações auditáveis; ausência bloqueia |
| Desktop | Chromium 1440 px | cards legíveis, sem overflow horizontal da página |
| Mobile | iPhone 15 Pro e Pixel 7 | conteúdo longo quebra linha e permanece navegável |

## Critérios

- **Continuar:** contratos de todos os produtores aprovados, backend rejeita auditoria incompleta e
  desktop/mobile mostram os três cards sem inferência.
- **Ajustar:** dados íntegros, mas algum prompt longo causa perda de legibilidade, overflow ou
  duplicação visual.
- **Parar:** a divisão exigir reconstrução retroativa ou alterar a autoridade/orquestração dos
  agentes.
