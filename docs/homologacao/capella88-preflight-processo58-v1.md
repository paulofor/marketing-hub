# Matriz de homologação — Capella #88 / processo técnico #58

Data: 23/09/2026. Escopo: produto #7, experimento #88, processo
`experiment-homologation-activation` v5 e retomada da campanha existente. A execução usa dados
sintéticos localmente; nenhuma venda, entrevista ou receita é fabricada.

## Critérios antes da entrega

| Área | Caminho feliz | Validação e falha | Evidência esperada |
|---|---|---|---|
| Superfícies | Run produtivo vigente e gate de landing aprovado | Produto diferente, gate ausente, referência vazia ou hash Quartzo vencido bloqueiam | Run, versão, resumo, referência, avaliador e impressão dos insumos |
| Compra e entrega | Gate de checkout/entrega aprovado para objetivo `SALES` | Formulário não substitui compra; gate reprovado não avança | Evidência funcional do mesmo run, custo incremental zero |
| Medição | Gates de freshness/deduplicação e canal real aprovados | QA misturado, dado pendente ou canal divergente bloqueiam | Duas referências correlacionadas e qualidade `VALID` |
| Limites financeiros | Diário, teto, janela e paradas coerentes e abaixo de plano em execução/concluído, com Plutus vigente para Quartzo | Valor ausente, plano em rascunho/bloqueado, teto acima do plano ou parecer vencido bloqueiam sem autorizar gasto | Valores persistidos, fingerprint comercial e `spendAuthorizedByThisActivity=false` |
| Idempotência | Mesmo run e mesma impressão não criam nova ocorrência | Mudança real cria nova ocorrência sem apagar histórico | Ocorrência, status, horários e SHA-256 |
| Meta | Teto aceito na campanha ou, abaixo do mínimo, no único ad set diário | Sem suporte nativo justificável não há escrita; HTTP inválido, gasto ausente ou readback divergente mantêm pausa | Request/response sem token, orçamento, teto, prazo e estado relidos |
| Recuperação | Campanha ativa é pausada antes de mutação e só reativada após confirmação | Callback falho executa compensação; campanha já pausada não recebe escrita redundante | Callback de falha e estado `PAUSED` confirmado |
| Interface | Retomada do processo avança uma atividade por vez | Espera indefinida vira bloqueio acionável | Processo 4/4, retorno ao pai e histórico preservado |
| Navegadores | Fluxo administrativo em desktop, iPhone e Pixel | Controles indisponíveis ou texto truncado impedem aceite | Capturas e respostas oficiais do backend |

## Decisão técnica

Foram comparados: atualizar status manualmente, esconder o subprocesso e operacionalizar suas
atividades a partir das provas persistidas. A terceira alternativa preserva o modelo da cadeia, a
ordem BPM, os custos e a explicação ao usuário. Para o teto Meta foram comparados: elevar o limite,
usar apenas monitoramento local e aplicar teto nativo no único conjunto; a terceira alternativa é a
única aderente à autorização de R$ 125 e ao objetivo de controlar margem.

Testes aprovados demonstram o contrato técnico, não vendas ou lucro. O resultado comercial deve
continuar sendo medido por compras líquidas, receita conciliada, CAC, custo integral e margem.
