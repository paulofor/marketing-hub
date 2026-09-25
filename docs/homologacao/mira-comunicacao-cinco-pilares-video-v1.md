# Homologação — comunicação de Mira e decisão de vídeo v1

Data: 2026-09-25. Escopo: produto Mira #10, experimento #93, comunicação #95 e produção criativa
#64. Esta matriz não autoriza campanha, publicação, provider pago ou gasto.

## Critérios comerciais

| Critério | Entregável vigente | Evidência de aceite | Métrica futura |
|---|---|---|---|
| Desejo reconhecido | Organizar produtos de skincare já disponíveis com menos esforço e clareza | `DESEJO_RECONHECIDO` na tarefa #502, dossiê #36 e estratégia V3 | impressão humana atribuída → primeira interação → início |
| Primeiro passo fácil | Entrada móvel em três passos, progresso, retomada e bloqueio explicável | `PRIMEIRO_PASSO_FACIL`, prova real `mira-private-v3` | início → conclusão → primeiro valor; abandono e tempo |
| Valor antes do compromisso | Demonstração identificada com pixels autorizados da versão real | `VALOR_ANTES_DO_COMPROMISSO`, tarefa técnica #371 e artefatos 95–99 | demonstração vista → CTA → início; uso separado de exibição |
| Continuidade paga | Rotina completa, preço hipotético único de R$ 49, duas tentativas máximas e limites | `CONTINUIDADE_PAGA`; checkout real, pagamento e reembolso continuam como gates | oferta vista → CTA → checkout → compra → entrega → uso |
| Repetição com margem | Uma variável: cartão estático versus vídeo curto; custo de referência até R$ 14 | `REPETICAO_COM_MARGEM`, Plutus e plano #8 | compras líquidas, CAC, custo integral, reembolso, contribuição e margem |

## Alternativas de vídeo

1. Somente cartão estático: baixo custo e prova direta, mas menor clareza do percurso.
2. Geração externa paga: maior riqueza visual, mas exige autorização, orçamento e aumenta risco de
   representar prova inexistente.
3. Demonstração editorial curta com capturas reais: esforço moderado, alta fidelidade e comparação
   limpa com o estático. Escolhida por Íris; Apolo deve apenas registrar a necessidade e bloquear a
   produção até os gates próprios.

## Matriz técnica local

| Caso | Resultado esperado |
|---|---|
| Construção com `harness.audiovisualRequired=false` | conclusão determinística, custo e provider zero |
| Comunicação com harness falso e rota criativa verdadeira | prevalece a rota; bloqueio `AUTHORIZATION_REQUIRED` sem provider |
| Rota ausente, incompleta, histórica ou de outra definição | `MISSING_CONTRACT`; nenhuma inferência |
| Resposta especializada de Apolo | somente identidade e decisão mínima; sem contexto histórico amplo |
| Callback | caminho do contrato, valor, origem, zero tokens/créditos/custo e nenhum efeito externo |
| Retomada da tarefa | no máximo uma execução determinística por tarefa; nenhuma chamada de modelo |
| Tela desktop, iPhone e Pixel | cinco pilares, estado e bloqueio legíveis, sem ação automática de mídia |

Dados de QA, agentes, bots e duplicidades não contam como cliente, compra ou receita. O aceite
técnico desta matriz não comprova vendas nem lucro.
