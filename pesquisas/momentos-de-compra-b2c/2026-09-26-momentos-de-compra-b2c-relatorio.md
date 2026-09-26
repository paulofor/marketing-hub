# Radar — Momentos de compra B2C iminente no Brasil
**26/09/2026**

## Universo pesquisado
Exploração realizada antes da revisão do histórico: **44 momentos brutos**, **21 situações aprofundadas** em 15 macrofamílias. **81%** das situações aprofundadas ficaram fora dos Top 3 dos três dias anteriores.

## TOP 5 DESCOBERTAS NOVAS
| # | Momento | Score | Confiança | Estágio |
|---|---|---:|---|---|
| 1 | Bagagem adicional bloqueada em passagem emitida por parceiro/interline antes do aeroporto | **71/80** | Alta/média | **CANDIDATO A EXPERIMENTO** |
| 2 | Documentação administrativa em análise enquanto uma condição comercial com prazo está prestes a expirar | **69/80** | Alta/média | **CANDIDATO A EXPERIMENTO** |
| 3 | Atendimento da CIN marcado, mas o formato da certidão pode não atender à regra local | **66/80** | Alta/média | **CANDIDATO** |
| 4 | Passageiro não usa a ida, segue por outro meio e precisa preservar o trecho de volta | **65/80** | Alta/média | **SINAL CONFIRMADO** |
| 5 | Certificado profissional parece inconsistente antes de entrar em processo administrativo lento | **64/80** | Média/alta | **SINAL CONFIRMADO** |

## 1. Interline Baggage Purchase Gate
**Família:** viagem/lazer. **Horizonte:** 1–4 semanas.

Cena: bilhete já emitido por programa/OTA, companhia operadora definida, passageiro precisa despachar bagagem e o fluxo de compra não funciona porque o bilhete veio de parceiro.

Existe diferença oficial entre compra antecipada e compra no aeroporto em algumas companhias, portanto há dinheiro real em jogo.

**Alternativa gratuita:** canais oficiais do emissor e da operadora.  
**Vantagem proposta:** reconciliar emissor, operadora, trecho, franquia e canal oficial executável.  
**Microvalor:** em até 10 minutos, identificar a rota oficial disponível e o risco de custo adicional.  
**Hipótese:** o usuário só paga se o preflight encontrar uma rota executável ou economia concreta.  
**Reel:** passagem emitida → bagagem necessária → canais se contradizem → rota oficial antes do aeroporto.  
**MVP:** uma dupla emissor/operadora, com confirmação humana.  
**Instrumentação:** experience_started → itinerary_loaded → baggage_entitlement_reconciled → purchase_channel_found → microvalue_reached → free_support_preferred/paid_solution_preferred → checkout_started → payment_reconciled → baggage_purchased_before_airport → airport_surcharge_avoided.

## 2. Deal Deadline Document Gate
**Família:** administração pessoal + mobilidade. **Horizonte:** hoje/24h.

Cena: compra já escolhida, documentos já enviados e condição comercial com prazo enquanto a análise administrativa continua.

**Alternativa gratuita:** instituição responsável e checklist oficial.  
**Vantagem proposta:** organizar documentos, detectar pendência objetiva e estruturar o pacote, sem prometer aprovação ou liberação.  
**Microvalor:** apontar a pendência antes de o prazo comercial expirar.  
**Hipótese:** um preflight administrativo pode ter valor quando o custo de perder a condição é materialmente maior que o esforço da revisão.  
**Reel:** compra escolhida → prazo hoje → documentos em análise → pendência objetiva encontrada.  
**MVP:** um único fluxo administrativo, com revisão humana assistida.  
**Instrumentação:** experience_started → deal_deadline_loaded → document_pack_loaded → missing_requirement_found → microvalue_reached → free_alternative_preferred/paid_solution_preferred → checkout_started → payment_reconciled → review_completed → deal_preserved.

## 3. CIN Appointment Document Preflight
**Família:** documentação/burocracia civil. **Horizonte:** 2–7 dias.

Cena: atendimento marcado e a pessoa só descobre no balcão que o formato ou dado do documento não atende à regra da unidade.

**Alternativa gratuita:** instruções oficiais do órgão estadual e cartório.  
**Vantagem proposta:** validar documento + unidade/estado + regra local e direcionar à rota oficial correta.  
**Microvalor:** detectar incompatibilidade antes do deslocamento.  
**Hipótese:** só existe valor pago se a ferramenta encontrar um erro concreto que evitaria perder o atendimento.  
**MVP:** começar por um estado com regras públicas bem documentadas.  
**Instrumentação:** experience_started → appointment_loaded → identity_document_uploaded → state_requirements_loaded → mismatch_found → microvalue_reached → free_official_route_preferred/paid_solution_preferred → checkout_started → payment_reconciled → appointment_completed.

## 4. Return-Segment Integrity Guard
**Família:** viagem/mobilidade. **Horizonte:** hoje/24h.

Cena ideal: logo após o passageiro decidir não usar o trecho de ida e chegar ao destino por outro meio, verificar se o retorno precisa de ação para ser preservado.

**Alternativa gratuita:** app/site/call center da companhia.  
**Vantagem proposta:** monitor persistente e alerta acionável. O formato parece mais adequado como feature de OTA/concierge do que como produto avulso.  
**Microvalor:** verificar o retorno e levar o usuário ao canal oficial antes do limite.  
**Instrumentação:** experience_started → itinerary_loaded → outbound_no_show_detected → return_segment_checked → microvalue_reached → official_contact_completed → return_preserved.

## 5. Professional Certificate Acceptance Preflight
**Família:** educação/certificação + carreira/renda. **Horizonte:** hoje–7 dias.

Cena: certificado já emitido e prestes a ser protocolado em RH/órgão, mas há dúvida sobre dados, registro ou campos obrigatórios; a análise posterior pode ser lenta.

**Alternativa gratuita:** registros públicos, regulamento, RH e ChatGPT.  
**Vantagem proposta:** cruzar certificado + registro oficial + ato publicado + exigência do destino.  
**Microvalor:** flagrar divergência objetiva antes do protocolo.  
**Hipótese:** existe valor apenas quando a ferramenta encontra inconsistência verificável.  
**Instrumentação:** experience_started → certificate_uploaded → official_registry_checks_completed → blocking_inconsistency_found → microvalue_reached → free_manual_check_preferred/paid_solution_preferred → checkout_started → payment_reconciled → corrected_document_received → submission_accepted.

## TOP 3 GERAL
| # | Momento | Macrofamília | Score | Estágio |
|---|---|---|---:|---|
| 1 | Sinistro automotivo travado + mobilidade substituta | Seguros/mobilidade | **79/80** | **CANDIDATO A EXPERIMENTO** |
| 2 | Certificate Rescue — operação bloqueada por certificado digital | Documentação/prosumer | **79/80** | **CANDIDATO A EXPERIMENTO** |
| 3 | Vistoria de saída + cobrança contestável | Moradia | **79/80** | **CANDIDATO A EXPERIMENTO** |

Sinistro e Certificate Rescue permanecem em 79. Vistoria de saída sobe de 78 para 79 após novas confirmações da necessidade de reconciliar evidências de entrada, saída, itens, fotos e orçamento.

## Descartados/rebaixados
- Continuidade de celular/câmera/notebook: mecanismo já explorado recentemente, sem hard edge novo.
- Integridade de ingresso: plataforma controla QR/titularidade; sem integração oficial vira checklist.
- CRLV: já aprofundado; estado controlado por sistemas oficiais.
- Roupa entregue no local errado: fonte sem data exata do evento, falhando no Gate de horizonte.
- Situações clínicas, fraude, apostas e investimento especulativo: excluídos por segurança.

## Investigar amanhã
**Interline Baggage Purchase Gate**: mapear emissor/operadora, compra direta com PNR, diferença antecipado/aeroporto e modelo de concierge oficial. Métricas: baggage_purchased_before_airport e airport_surcharge_avoided.

## Primeiro protótipo privado
**Deal Deadline Document Gate**, com escopo estritamente administrativo: checklist, detecção de pendência objetiva e acompanhamento, sem prometer aprovação ou resultado.

Instrumentação: experience_started → deal_deadline_loaded → document_pack_loaded → missing_requirement_found → microvalue_reached → free_alternative_preferred/paid_solution_preferred → checkout_started → payment_reconciled → review_completed → deal_preserved.

**payment_reconciled prova venda. deal_preserved começa a provar o microvalor. Nenhum candidato foi tratado como vencedor.**
