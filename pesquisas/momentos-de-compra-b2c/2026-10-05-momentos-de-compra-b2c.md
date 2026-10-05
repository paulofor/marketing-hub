# Radar — Momentos de compra B2C iminente no Brasil
**Data:** 2026-10-05

## Universo pesquisado
A exploração foi feita antes de revisar os líderes históricos. Foram considerados **48 momentos brutos** e aprofundadas **23 situações em 16 macrofamílias**: carreira/renda, educação/certificação, casa/moradia, consumo/varejo, tecnologia pessoal, mobilidade, viagem/lazer, finanças administrativas, serviços recorrentes, logística doméstica adulta, pets, beleza/eventos, alimentação/eventos, trabalho autônomo/prosumer, documentação/burocracia e sazonalidade. **18 de 23 (78%)** ficaram fora dos Top 3 dos três dias anteriores.

Padrão novo: **o consumidor já pagou pelo item, frete, fornecedor ou evento, mas ainda existe um último estado operacional que precisa ser confirmado antes de perder prazo, renda ou capacidade de reagir**.

## TOP 5 DESCOBERTAS NOVAS
| # | Momento | Score | Confiança | Estágio |
|---|---|---:|---|---|
| 1 | Work-Critical Supply Delivery Failover | **73/80** | Alta | CANDIDATO A EXPERIMENTO |
| 2 | Pre-Move Appliance Test & Acceptance Gate | **70/80** | Alta/média | CANDIDATO A EXPERIMENTO |
| 3 | Last-Week Wedding Supplier Confirmation Gate | **70/80** | Alta/média | CANDIDATO A EXPERIMENTO |
| 4 | Same-Day Event Supply Rescue | **69/80** | Média/alta | SINAL CONFIRMADO |
| 5 | Vehicle Transfer Document Deadline Rescue | **67/80** | Média/alta | SINAL CONFIRMADO |

## 1. Work-Critical Supply Delivery Failover
**Macrofamília:** trabalho autônomo/prosumer + logística. **Horizonte:** 24h–7d. **Estágio:** CANDIDATO A EXPERIMENTO.

Em 04/10, um consumidor relatou ter pago frete por insumos urgentes de trabalho; a encomenda foi devolvida e ele declarou que teria de pagar outra transportadora. Em outro caso do mesmo dia, o comprador precisava da ferramenta na manhã de 05/10 e afirmou que teria de comprar outro produto se a entrega não ocorresse. Há mercado de courier/motoboy com contratação imediata e preços publicados a partir de cerca de R$11–R$15.

**Custo do erro:** segundo frete/segunda compra + perda de atividade profissional.  
**Alternativa gratuita:** retirada, empréstimo, espera ou busca manual local.  
**Vantagem paga proposta:** reconciliar item/especificação, estoque local e courier com ETA confirmado.  
**Microvalor:** “o pedido original não chega a tempo; há substituto compatível e entrega confirmada antes do compromisso”.  
**Hipótese:** pessoas com insumo de trabalho atrasado e compromisso em até 24h pagarão **R$19–R$49 (hipótese)** para obter substituto compatível + entrega confirmada.  
**Reel:** ferramenta não chegou → trabalho amanhã → estoque local → motoboy → entrega antes do cliente.  
**MVP:** uma cidade, três categorias de insumos, 5–10 fornecedores/couriers.  
**Instrumentação:** experience_started → work_deadline_loaded → original_delivery_state_loaded → replacement_stock_found → courier_eta_confirmed → microvalue_reached → free_alternative_preferred / paid_solution_preferred → checkout_started → payment_reconciled → replacement_received → work_deadline_preserved.  
**Limites:** não recomendar substituição técnica incompatível; exigir validação profissional para itens críticos.

## 2. Pre-Move Appliance Test & Acceptance Gate
**Macrofamília:** casa/moradia + varejo. **Horizonte:** 2–7d / 1–4sem. **Estágio:** CANDIDATO A EXPERIMENTO.

Em 04/10, uma lavadora entregue em 25/09 só foi testada durante a mudança e apresentou falhas graves. Em outro caso, uma lavadora recebida em 26/09 só foi aberta em 03/10 e apresentou avaria. O padrão é: **recebido não significa testado**. Compras a distância têm janela de arrependimento de sete dias; para defeitos em bens duráveis há outras proteções, mas descobrir o problema cedo simplifica a reação. Há instalação profissional de lavadora por **R$100–R$250**, incluindo conexão, nivelamento e teste de ciclo.

**Alternativa gratuita:** abrir/testar imediatamente, pedir ajuda, seguir manual.  
**Vantagem paga proposta:** calcular a janela e agendar instalação/teste antes do cutoff.  
**Microvalor:** “faltam X dias; há técnico disponível para instalar e rodar o teste antes disso”.  
**Hipótese:** consumidores que recebem eletrodoméstico antes da mudança pagarão **R$49–R$99 (hipótese)** ou contratarão instalação de R$100–R$250 para testar antes de perder a janela simples de devolução.  
**Reel:** lavadora chegou → mudança depois → não deixe na caixa → instalar/testar antes do 7º dia.  
**MVP:** uma cidade, lavadora/lava-e-seca, parceiros de instalação.  
**Instrumentação:** experience_started → delivery_date_loaded → return_window_calculated → test_slot_found → microvalue_reached → checkout_started → payment_reconciled → appliance_test_completed → defect_found_before_cutoff / appliance_ready_for_move.  
**Limites:** sem orientação elétrica/hidráulica de risco; profissional quando necessário.
