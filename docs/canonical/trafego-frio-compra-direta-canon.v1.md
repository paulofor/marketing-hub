# Tráfego frio não vai direto para compra v1

## Regra mandatória

Todo experimento com intenção de compra, identificado por `experimentType = LOW_TICKET_PRODUCT` ou `campaignObjective = SALES`, deve enviar o tráfego frio primeiro para uma página de venda/intermediação publicada e auditada pelo GeraSalesPage v1.

O anúncio não pode apontar diretamente para checkout, link de pagamento ou outro destino de compra.

Para Produto IA `AI_PERSONALIZED_SAMPLE`, o destino intermediário aprovado é o funil-página do Lead Portal vinculado ao experimento. Nesse caso, o GeraSalesPage v1 pode iniciar sem URL de checkout real quando `follow_up_action_url` apontar exatamente para o funil aprovado de coleta da amostra personalizada. A publicação da campanha continua bloqueada até o GeraSalesPage v1 concluir e auditar a página dentro desse funil.

## Critérios obrigatórios

- O GeraSalesPage v1 precisa concluir a etapa `publication-package` e registrar auditoria da página publicada antes da liberação para Facebook Ads.
- `follow_up_action_url` deve ser a URL da página de venda auditada, não a URL de checkout.
- Em `AI_PERSONALIZED_SAMPLE`, `follow_up_action_url` deve ser a URL pública do `LeadPortalFlow` aprovado que receberá a página auditada e o formulário gerenciado de personalização.
- O checkout deve existir apenas como CTA dentro da página, depois de promessa, prova, mecanismo, objeções e percepção de valor.
- A página precisa conter coletores mínimos `page_view`, `page_load_metric`, `section_view_time` e `checkout_click`.
- O backend deve bloquear tanto a liberação manual quanto a fila `/api/facebook-campaigns/experiments-ready` quando essa regra não for cumprida.

## Interpretação de negócio

Tráfego frio direto para compra reduz a chance de educar o visitante, provar valor e medir onde o funil quebra.

A regra protege caixa e aprendizado: primeiro mede atenção e convencimento na página, depois mede intenção de checkout.
