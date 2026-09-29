# Radar — Momentos de compra B2C iminente no Brasil
**29/09/2026**

## Universo pesquisado
A exploração foi feita antes da revisão do histórico. Foram considerados **45 momentos brutos** e aprofundadas **22 situações em 16 macrofamílias**. **82%** dos aprofundamentos ficaram fora dos Top 3 dos três dias anteriores.

## TOP 5 DESCOBERTAS NOVAS
1. **Work-Car Part Fitment Gate — 72/80.** Autônomo prestes a parar o carro precisa confirmar a aplicação correta da peça antes da oficina. Há sinais recentes de peça incompatível seguida de nova compra. O diferencial é conciliar veículo, peça, estoque e confirmação da oficina.
2. **Invoice Continuity Rescue — 71/80.** Prestador tem cliente aguardando nota, mas o fluxo de emissão está bloqueado por cadastro, migração ou estado administrativo. Como há rota oficial gratuita, o valor depende de identificar o bloqueio e indicar a ação correta.
3. **Workstation Repair-or-Replace Gate — 69/80.** Notebook profissional quebra e o primeiro orçamento é desproporcional ou o diagnóstico é incerto. O microvalor é combinar segunda opinião independente e continuidade temporária de trabalho.
4. **New-Home Independent Reinspection Gate — 68/80.** A construtora informa correções, mas a revistoria mostra pendências. O valor vem de comparação técnica entre vistoria anterior e revistoria antes do aceite.
5. **Urgent Professional Diploma Release Gate — 67/80.** Convocação ou efetivação chega enquanto o diploma não sai. Há pagamento observável por urgência, mas a instituição controla a emissão; por isso permanece SINAL CONFIRMADO.

## TOP 3 GERAL
1. **Sinistro automotivo travado + mobilidade substituta — 79/80.**
2. **Certificate Rescue — operação bloqueada por certificado digital — 79/80.**
3. **Vistoria de saída + cobrança contestável — 79/80.**

Os três permanecem em 79 porque a evidência de 28/09 confirmou os mecanismos sem criar um novo wedge.

## Candidatos rebaixados
Foram rebaixados: declaração de falta de energia para folha de pagamento, falha de internet com troca de operadora, comparação de contrato de financiamento, passagem com data errada, seguro-viagem do cartão, assistência residencial e roupa de evento atrasada. Os três últimos grupos já tinham sido explorados recentemente; os demais ainda falham em demanda paga, vantagem sobre gratuito ou controle de execução.

## Investigar amanhã
Aprofundar **Work-Car Part Fitment Gate**: bases de aplicação, categorias com maior risco de incompatibilidade, confirmação pela oficina e disponibilidade real no mesmo dia. Métrica central: **repair_started_with_correct_part**.

## Primeiro protótipo privado
Testar **Invoice Continuity Rescue** com um fluxo estreito para prestador de serviços. Estados de saída: rota oficial gratuita; pendência identificada; necessidade de profissional; inconclusivo/não cobrar.

Instrumentação mínima: experience_started → blocker_identified → microvalue_reached → free_alternative_preferred/paid_solution_preferred → checkout_started → payment_reconciled → compliant_route_opened → invoice_issued.

Nenhum candidato é tratado como vencedor antes de comportamento real.
