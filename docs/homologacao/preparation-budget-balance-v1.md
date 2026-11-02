# Aprovação e saldo de preparação — 02/11/2026

## Evidência e decisão

Capella, produto 7, cadeia 27, ciclo 19, experimento 112: recibo 110 de
31/10/2026 preserva teto cumulativo de US$ 80 e reserva interna de US$ 20.
O custo conhecido já era US$ 67,37689 no aceite. Banco e tela confirmam a
execução 95 pausada em Plutus, atividade `economics`, sem tarefa ativa.
A aprovação não se perdeu: `80 - 67,37689 - 20 = -7,37689`.

O harness da demonstração testava recuperação do recibo, mas não o retorno
de uma aprovação salva sem cobertura. A tela dizia incorretamente que saldo
negativo significava consumo conhecido acima do teto, e o aviso de sucesso
não esclarecia a pendência. O recibo e o guard financeiro estavam corretos.

| Alternativa | Benefício | Risco/limite | Esforço | Decisão |
| --- | --- | --- | --- | --- |
| Explicar somente nesta conversa | Esclarece o caso atual | Outros produtos repetem a confusão | Baixo | Insuficiente para prevenir recorrência |
| Impedir registro de limites sem saldo | Evita um aceite insuficiente | Impede reduzir limites e preservar uma pausa deliberada; muda a política de autorização | Médio | Não adotada |
| Expor cobertura no backend e mostrar recibo e cálculo junto da pendência | Explica aprovação salva, reserva e falta de saldo em qualquer ciclo | Saldo positivo continua sem garantir custo futuro | Baixo | Adotada, sem alterar decisões ou gates |

## Matriz de homologação definida antes dos testes

| Caso | Aceite |
| --- | --- |
| Caso original | Teto 80, reserva 20 e estimativa 67,37689 retornam `NO_BALANCE`, recibo preservado, falta US$ 7,38 arredondada para cima aos centavos; saldo auditado mantém precisão. Sem tarefa ou mudança de limite. |
| Outra identidade | Produto/ciclo/versão diferentes seguem o mesmo cálculo, sem exceção por nome ou ID. |
| Caminho antes válido | Aprovação com saldo positivo mantém recibo, replay idempotente, limites independentes e exigência de revalidação da próxima tarefa. |
| Limites | Saldo zero permanece sem verba para outra tarefa; custo desconhecido não vira zero; aprovação ausente ou histórica não vira vigente. Fração de centavo não oculta insuficiência; ciclo encerrado não pede retomada. |
| Recuperação | Falha de resposta preserva chave e formulário; aprovação sem cobertura mostra o recibo junto da pendência, inclusive após recarregar. |
| Integração e observabilidade | GET e POST oficiais retornam o mesmo estado calculado no backend; referência do recibo, contexto e data visíveis. Testes não chamam modelos nem gravam produção. |
| Desktop e celular | Chromium desktop, iPhone 15 Pro e Pixel 7 emulados mostram o cálculo e o ajuste, sem transbordamento; sem envio ao backend produtivo. |
| Métrica e isolamento | Testes comprovam a distinção entre salvar e poder executar, sem contar simulação como venda, custo conciliado ou aprovação real. |

## Aprendizado reutilizável

Capacidade: demonstração financeira e contrato entregue à operação. Versão:
`preparationBalance` v1, adição compatível ao contrato existente. Hipótese:
mostrar imediatamente a decisão salva e sua cobertura reduz retomadas e
aprovações repetidas sem mudança de causa. Critério local: regressões da
matriz aprovadas e gates preservados. O efeito em tempo parado, repetição e
vendas depende de medição posterior; não há ganho comercial demonstrado.
Reverter a apresentação se divergir do cálculo oficial, preservando recibos.

## Resultados

- 173 testes Java relevantes aprovados, sem falhas ou skips: 24 da demonstração
  financeira (incluindo GET/POST reais com persistência simulada), 19 do parecer,
  26 de vídeos, 12 do guard de correção e 92 de arquitetura. Após ajustar as
  fixtures e acrescentar os limites de centavos/ciclo encerrado, foram repetidos
  somente os 24 testes afetados; os demais resultados continuam válidos.
- 45 testes de interface aprovados: demonstração, link financeiro e quadro de
  execução. TypeScript e build minificado com source maps aprovados; o build
  completo terminou em 42,80 s.
- Jornada local aprovada em Chromium desktop, iPhone 15 Pro e Pixel 7 emulados:
  navegação do card, recibo, recuperação da aprovação, cálculo de insuficiência,
  atualização da leitura e acesso ao ajuste, sem transbordamento horizontal.
  Capturas em `artifacts/execution-finance/*-saved-without-balance.png`.
- O erro inicial do cenário com outra identidade era a fixture incompleta de
  produto sob lock e experimento, corrigida sem relaxar a validação de identidade.
  A simulação de alteração de recibo também passou a incrementar a revisão e
  preservar histórico, conforme o contrato real.
- Não há mudança em SQL, schema, guard de gasto ou decisão financeira. Testes
  usam repositórios e respostas locais, sem modelos externos nem gravação de
  orçamento, tarefa, campanha ou dado comercial em produção.
