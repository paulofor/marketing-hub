# Radar 26/09/2026 — teste de conteúdo

## Deal Deadline Document Gate
Cena: pessoa já escolheu uma compra e enviou documentos, mas uma condição comercial expira antes da análise administrativa. O produto proposto é apenas organização documental: detectar item faltante, registrar prazo e acompanhar o pacote. Não aprova crédito, não recomenda financiamento e não promete liberação.

Instrumentação: experience_started → document_pack_loaded → missing_requirement_found → microvalue_reached → checkout_started → payment_reconciled → review_completed → deal_preserved.
