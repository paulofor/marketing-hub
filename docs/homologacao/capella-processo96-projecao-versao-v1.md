# Homologação da projeção por versão do Processo 5 de Capella — v1

## Escopo confirmado em 24/09/2026

- produto #7, Capella, tipo canônico `LOW_TICKET_DIGITAL_PRODUCT` (Quartzo);
- cadeia #22, Processo 5 v10, definição #96, referência `experiment:88`;
- execução automática #20 e subprocesso Quartzo #21;
- experimento #88 em `USER_STOPPED`, mídia diária de R$ 20 e teto de R$ 125;
- nenhuma ficha de execução está vinculada ao produto ou à referência; o histórico não foi migrado
  nem recebeu uma ficha inventada;
- tarefa histórica #422 pertence à v6; a prova vigente reutilizada é a tarefa #474 do subprocesso
  Quartzo e a instância do pai é #370.

## Matriz de aceite local

| Caso | Evidência esperada |
| --- | --- |
| v6 bloqueada e v10 ainda sem instância | #422 continua consultável, mas a v10 fica `NOT_STARTED` |
| v10 concluída por reúso | instância #370 governa o estado `COMPLETED` com `REUSED_DIRECT` |
| bloqueio criado na própria v10 | a tarefa atual continua governando o estado e a recuperação |
| tarefa de outra definição na tela | cartão informa que é histórico e não determina a v10 |
| contexto copiado para o AIHUB | cada tarefa declara se pertence à versão selecionada ou ao histórico |
| referência de outro produto | o backend continua recusando a consulta |
| lista compacta | prompts, resultados e evidências extensas continuam carregados só sob demanda |
| desktop, iPhone 15 Pro e Pixel 7 | aviso histórico legível, sem sobreposição nem ação de escrita |

## Limites comerciais preservados

A correção não altera produto, oferta, preço, campanha, orçamento, janela, status Meta ou pareceres.
Ela não autoriza a atividade 5.5. A execução #20 somente pode concluir quando uma pessoa autorizar
explicitamente a retomada financeira pelo contrato vigente; testes e preflight não comprovam venda,
receita ou margem.
