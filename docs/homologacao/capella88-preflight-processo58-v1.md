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
| Meta | Teto aceito na campanha ou, abaixo do mínimo, diário migrado para a campanha e teto no único ad set sem orçamento próprio | Misturar `daily_budget` e `lifetime_spend_cap`, elevar teto, HTTP inválido, gasto ausente ou readback divergente mantêm pausa | Request/response sem token e orçamento/teto/prazo/estado relidos nos dois níveis |
| Recuperação | Campanha ativa é pausada antes da mutação; retry conclui migração parcial compatível e só reativa após confirmação | Callback falho executa compensação; identidade, múltiplos conjuntos ou valores divergentes bloqueiam | Callback de falha, modo de orçamento e estado `PAUSED` confirmado |
| Interface | Retomada do processo avança uma atividade por vez | Espera indefinida vira bloqueio acionável | Processo 4/4, retorno ao pai e histórico preservado |
| Navegadores | Fluxo administrativo em desktop, iPhone e Pixel | Controles indisponíveis ou texto truncado impedem aceite | Capturas e respostas oficiais do backend |

## Decisão técnica

Foram comparados: atualizar status manualmente, esconder o subprocesso e operacionalizar suas
atividades a partir das provas persistidas. A terceira alternativa preserva o modelo da cadeia, a
ordem BPM, os custos e a explicação ao usuário. Para o teto Meta foram comparados: elevar o limite,
converter o conjunto recorrente em orçamento vitalício e migrar o diário para a campanha com teto
nativo no único conjunto sem orçamento próprio. As duas primeiras violam a autorização ou foram
rejeitadas pela API; a terceira preserva R$ 20/dia e o teto de R$ 125.

Testes aprovados demonstram o contrato técnico, não vendas ou lucro. O resultado comercial deve
continuar sendo medido por compras líquidas, receita conciliada, CAC, custo integral e margem.
