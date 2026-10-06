# Continuidade entre processos do ciclo — 06/10/2026

## Evidência e escolha

UI, MCP e contratos confirmam Capella #7, ciclo #5/experimento #98 em ADJUSTMENT:
Atena #591, Plutus #593 e Dédalo #594 concluídos; processo #116, execução #46,
COMPLETED. O evento #32 foi registrado por comando humano. A construção #117 não
possui execução ou tarefa. O motor conclui um processo e o retira da fila, sem
encaminhar o trabalho indicado pelo resolvedor do ciclo. Histórico de subprocessos
com pai demonstra que a fila funciona; falta a passagem entre processos irmãos.

| Alternativa | Benefício | Risco e esforço | Escolha |
| --- | --- | --- | --- |
| Encadear cliques no navegador | Pequena implementação | Depende de tela aberta e transfere coordenação ao frontend | Descartada |
| Novo motor de cadeia | Isolamento | Duplica fila, locks, eventos, pausas e recuperação | Descartada |
| Passagem no motor existente pelo resolvedor do ciclo | Mesma verdade da UI, persistência e idempotência | Alteração restrita com testes transacionais | Adotada |

## Matriz definida antes dos testes

| Cenário | Aceite |
| --- | --- |
| Planejamento aprovado | Conclusão comprovada avança o ciclo e enfileira construção |
| Construção concluída | Comunicação enfileirada no mesmo contexto |
| Comunicação concluída | Registra ajuste comprovado; respeita próximo contrato e autorização |
| Recuperação de processo já concluído | Reutiliza provas, decisão e destino; sem repetir tarefa |
| Outro produto e IDs diferentes | Mesma política sem casos por nome/ID |
| Processo sem ciclo e subprocesso histórico | Comportamento anterior preservado |
| STOP, pausa, ciclo encerrado, identidade divergente | Nenhuma tarefa indevida |
| Replay, concorrência e falha de persistência | Um destino e um recibo; rollback preserva recuperação |
| Integração/observabilidade | Motor, resolvedor e repositórios reais com executores simulados; eventos correlacionados |
| Segregação e métricas | Fixtures locais; nenhum consumo pago, mídia ou venda simulada em produção |
| UI desktop/iPhone/Pixel Chromium | Contratos de acompanhamento existentes legíveis e sem comandos na leitura |

## Resultados locais

- Backend completo: 4.120 casos, zero falhas/erros; 27 ignorados por condições
  e desativações preexistentes das fixtures. Inclui arquitetura, conclusão normal, início
  com provas existentes e recuperação explícita de conclusão, preservando destino
  pausado e contexto histórico. Íris: 43 casos, zero falhas/erros, 2 ignorados.
- Matriz REST/MySQL 5.7: decisão e preparação anteriores preservadas (10 cenários),
  mais passagem completa em dois produtos sintéticos. Aprovação enfileira planejamento;
  suas provas iniciam construção; construção encaminha comunicação; ajuste comprovado
  chega a VIDEO_BRIEF com mídia zero e janela ausente.
- Concorrência, replay e trigger de falha confirmam unicidade e rollback transacional.
  Pausar o destino e reapresentar a origem não retoma o destino nem repete agentes.
- Chromium desktop, iPhone 15 Pro e Pixel 7: leitura sem efeitos, preparação única,
  navegação para planejamento e janela ausente passaram.
- Shell: `bash -n` e ShellCheck passaram. Python: compilação sintática passou.
- Contratos do CI (13 casos), evidências comerciais (15) e modelo visual (16)
  passaram, incluindo a conferência das atestações vigentes.
- Pacote executável: 9 verificações passaram; 4.231 classes idênticas às testadas,
  754 recursos íntegros e catálogo de 518 cartões inicializado no JAR.
- Harness ampliado com o cenário entre processos, reaproveitando o runner existente.
  O double do produto agora usa lock físico no MySQL; a aplicação local importa o
  resolvedor de navegação já usado em produção e valida o vínculo real do experimento.
  Callbacks/modelos são simulados e os dados ficam no banco exclusivo da sandbox.

Esses resultados comprovam continuidade técnica, não prontidão comercial, demanda,
receita ou margem. Os testes não usam API paga nem alteram dados de produção.
