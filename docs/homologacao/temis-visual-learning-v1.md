# Homologação — aprendizado visual governado de Têmis v1

## Objetivo final

Reduzir retrabalho visual sem reduzir qualidade premium, sem permitir que Têmis aprove a própria
experiência e sem confundir eficiência operacional com venda.

## Matriz ponta a ponta

| Área            | Cenário                                                    | Resultado obrigatório                                                                    |
| --------------- | ---------------------------------------------------------- | ---------------------------------------------------------------------------------------- |
| Caminho feliz   | 15 pareceres homogêneos                                    | 10 replays e 5 holdouts ficam congelados em uma execução `PENDING`                       |
| Histórico       | experimento com pareceres anteriores                       | casos são incorporados uma vez sem carregar base64 nem reexecutar provider               |
| Avaliação       | candidata supera baseline em pelo menos 5 pontos           | execução fica `READY_FOR_PROMOTION`, ainda sem orientar produção                         |
| Promoção        | decisão humana explícita                                   | somente jobs novos do mesmo contexto recebem a versão promovida                          |
| Validação       | menos de 15 casos ou IDs divergentes                       | consolidação bloqueada sem criar verdade operacional                                     |
| Segregação      | nicho, produto, finalidade, placement ou formato diferente | caso e playbook não vazam entre contextos                                                |
| Segurança       | provider, gasto ou publicação no replay                    | callback rejeitado e playbook vigente preservado                                         |
| Regressão       | candidata reduz qualidade em aprovado do holdout           | candidata rejeitada                                                                      |
| Integração      | produção de imagem ou retrabalho                           | snapshot contém versão, contexto, regras, proibições e até dois exemplos aprovados       |
| Observabilidade | painel administrativo                                      | exibe runs, estados, métricas e promoção com loading; não atribui vendas                 |
| Métricas        | nova versão em uso                                         | mede primeira tentativa, até três versões, erro recorrente, custo/aprovado e menor score |
| Dados de teste  | testes locais                                              | nenhum provider real, campanha, publicação ou gasto é executado                          |
| Navegadores     | desktop, iPhone 15 Pro e Pixel 7                           | tabelas responsivas, ação acessível e sem overflow global                                |

## Critérios de continuar, ajustar e parar

- **Continuar:** primeira tentativa ≥ 70%, aprovação até três versões, recorrência < 10%, custo por
  aprovado pelo menos 30% menor e score premium preservado.
- **Ajustar:** ganho de holdout abaixo de cinco pontos, amostra insuficiente ou métrica sem melhoria.
- **Parar/reverter:** regressão premium, mistura de contexto, efeito externo, custo maior ou repetição
  de falha bloqueante.
