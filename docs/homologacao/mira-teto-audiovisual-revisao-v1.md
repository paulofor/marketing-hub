# Substituição do teto audiovisual na revisão — v1

## Evidência e decisão

Em 20/10/2026, Mira (produto 10, ciclo 9, experimento 102) permanece em
`VIDEO_APPROVAL`, com vídeos 64 e 63 pendentes e processo 55 pausado. O usuário autorizou
US$ 12 **totais**, incluindo consumo anterior, sem ampliar mídia. A consulta oficial de
`video-budget` retorna o teto anterior de US$ 10 e `canAuthorize=false`: o helper aceita
escrita apenas em `VIDEO_BRIEF`, embora o escopo inclua as revisões. Não houve tentativa
de contornar a validação por banco ou de aprovar as peças.

Alternativas: alterar limites por projeto fragmentaria o teto compartilhado; voltar a
etapa apagaria a posição real e arriscaria repetição; aceitar substituição explícita
na revisão conserva a ocorrência e o ledger existente. Adotada a terceira opção, somente
para teto já vigente da mesma versão e ciclo aberto. Nenhuma inferência ou tarefa é
criada pelo registro. As demais etapas continuam somente leitura.

## Matriz local

| Dimensão                         | Caso                                                                                         | Aceite                                                                                  |
| -------------------------------- | -------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------- |
| Caminho original e outro produto | Mira 10/9/102 e Capella 7/12/105, fixtures sintéticas pela API real                          | Teto de 10 substituído por 12, com dois recibos e mesma etapa                           |
| Compatibilidade                  | Primeira autorização em VIDEO_BRIEF                                                          | Comportamento anterior preservado                                                       |
| Validações                       | Revisão sem teto, versão alterada, cadeia/produto divergente, ciclo encerrado, outras etapas | Nenhuma autorização indevida                                                            |
| Falhas e recuperação             | Repetir a mesma request; revisão obsoleta; conteúdo divergente                               | Um recibo por decisão e conflito sem sobrescrita                                        |
| Financeiro e observabilidade     | Histórico, revisão, referência e flags                                                       | Custos/reservas preservados pelo ledger; mídia e publicação não autorizadas             |
| Interface                        | Desktop, iPhone 15 Pro e Pixel 7                                                             | Novo teto explicado como acumulado; sem valor pré-selecionado ou confirmação automática |
| Continuação                      | Retomada separada do processo                                                                | Revisores e aceites independentes; custo desconhecido continua bloqueante               |

Os doubles locais não acessam provedores pagos nem campanhas. Testes e bots não contam
como tráfego, compras ou contribuição. A hipótese reutilizável é reduzir a espera por
uma atualização financeira já autorizada; medir tempo parado e recorrência, sem inferir
ganho de vendas. Reverter a mudança se permitir primeira autorização na revisão, alterar
etapa, apagar custo ou ampliar mídia. A verificação operacional ocorre após a publicação
pelo PR e preserva o teto de US$ 12, mesmo se ele não cobrir as revisões atuais.

## Resultado local

- 67 testes do backend passaram: teto pela API, continuidade e ferramenta auditiva.
- 17 testes do formulário passaram, incluindo substituição do total e recuperação.
- Build e verificação de tipos do frontend passaram.
- Chromium desktop, iPhone 15 Pro e Pixel 7 passaram com APIs locais simuladas:
  uma gravação, novo total 12, total anterior 10 preservado no histórico e nenhum erro de página.
- Spotless dos dois arquivos Java alterados, Prettier dos componentes e revisão do diff passaram.

Nenhuma chamada de provedor ou venda foi simulada como resultado comercial. O aceite local
comprova a atualização do limite; a revisão real ainda depende de custo conhecido e saldo.
