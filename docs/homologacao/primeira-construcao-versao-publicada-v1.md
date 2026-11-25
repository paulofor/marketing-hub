# Primeira construção após aposentadoria do BPM

## Matriz de homologação

| Cenário | Critério |
| --- | --- |
| Planejamento aceito, construção v11 aposentada e v12 publicada | O reconciliador confirma o planejamento e indica a adoção. O comando de continuidade registra o recibo, deixa a construção pausada e conserva produto, ciclo, experimento, candidata e cadeia. |
| Retomada pelo painel | O backend registra a passagem, enfileira a construção e entrega a primeira atividade pelo mesmo contrato do executor. O teste usa executor local e referências de teste. |
| Concorrência e resposta perdida | Dois comandos de preparação devolvem a mesma execução e o mesmo recibo. Dois comandos de retomada registram uma única passagem. |
| Provas/contrato variam | Planejamento incompleto, destino fora da fase, contrato estrutural incompatível e proposta desatualizada impedem a preparação. Validar outro produto e versão, sem identificadores fixos. |
| Limites e segregação | Outro produto, ciclo, referência ou cadeia não ganham acesso. STOP, falta de saldo, gates técnicos e aprovações continuam valendo. Não criar tarefas na preparação, aceitar atividades do destino nem copiar custos. |
| Fluxo anterior | Uma construção já iniciada usa a continuidade histórica. Uma versão publicada na cadeia mantém a passagem normal. |
| Uso administrativo | Painel em desktop, iPhone e Android mostra causa, entrada reaproveitada, registro da preparação e caminho de retomada. Fixtures não usam dados de cliente, nem enviam solicitações para produção. |

O aceite local cobre comportamento. Compras, preço, demanda e margem são avaliados pelo ciclo
comercial com suas próprias autorizações e fontes.

## Contrato verificado

A leitura anterior à correção confirmou os aceites 898, 926 e 927 na execução
97, a ausência de ocorrências da construção no experimento 113 e o erro de
publicação no evento 3491. As versões 117 e 127 possuem o mesmo grafo
operacional. A segunda exige critérios descritivos atualizados e inclui o
contrato de entrega por tipo da Turmalina. A comparação não transfere aceites
da construção entre versões.

O backend registra a conclusão do planejamento antes de indicar a adoção.
O recibo associa as fontes de planejamento e o contrato conferido ao destino
pausado. A retomada valida a mesma candidata e as mesmas entradas, libera a
fila e registra o vínculo no diário do Processo 2. Esse escopo cobre a primeira
construção ligada a um experimento em ajuste. Uma mudança estrutural, uma
referência compartilhada ou uma ficha incompatível exigem outro contrato de
continuidade.

Os testes de interface e as duas variantes do cenário responsivo conferiram
o fluxo histórico e o fluxo da primeira construção. Cada variante enviou uma
única preparação e exibiu o mesmo recibo após recarga em desktop, iPhone e
Pixel. O typecheck e o build de produção passaram. O empacotador revalidou
as fontes vigentes de homologação, incluindo a atestação de compatibilidade
da mesma candidata de Mira, sem alterar a prova anterior.

A rodada final conferiu 192 casos do backend, incluindo 2 regressões dedicadas,
39 testes das atividades BPM e 21 testes do catálogo de evidências operacionais.
O teste de integração usa H2 para persistir a fila e o diário e serializar os
comandos no registro do produto. O conciliador executa os comandos reais com
um executor de atividade local simulado. A confirmação de contrato obteve as
três respostas estruturadas sintéticas pelo consumidor de planejamento em uso.
Os 84 testes de frontend e seis testes do conciliador também passaram.
