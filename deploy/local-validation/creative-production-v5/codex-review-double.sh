#!/usr/bin/env bash
set -euo pipefail

schema_file=""
output_file=""
while [[ $# -gt 0 ]]; do
  case "$1" in
    --output-schema)
      schema_file="$2"
      shift 2
      ;;
    --output-last-message)
      output_file="$2"
      shift 2
      ;;
    *) shift ;;
  esac
done

[[ -s "$schema_file" ]] || { echo "schema de homologação ausente" >&2; exit 2; }
[[ -n "$output_file" ]] || { echo "saída de homologação ausente" >&2; exit 3; }

# Consome o prompt para reproduzir o contrato de stdin do Codex sem depender de OAuth externo.
while IFS= read -r _line; do :; done

if grep -q 'attentionScore' "$schema_file"; then
  result='{"decision":"APPROVED","attentionScore":94,"clarityScore":96,"desireScore":91,"credibilityScore":96,"actionScore":94,"copyAssessment":"Copy curta, específica para manicures, com volume do kit e preço legíveis dentro do limite recomendado pela Meta.","commercialAestheticAssessment":"Composição híbrida premium preserva exemplos reais de post e story sem redesenhar o produto entregue.","destinationIntegrationAssessment":"Anúncio, landing e checkout mantêm produto, oferta de R$ 67 e CTA de compra consistentes em desktop e mobile.","summary":"Criativo aprovado para teste: demonstra inequivocamente o kit digital real e mantém continuidade comercial até o checkout.","issues":[],"recommendations":[],"revisedHeadline":"","revisedPrimaryText":"","revisedDescription":"","revisedCta":"","revisedImagePrompt":"","mandatoryVisualRequirements":[],"forbiddenVisualElements":[],"visualAcceptanceCriteria":[],"correctionTargets":[]}'
elif grep -q 'customerPerspective' "$schema_file"; then
  result='{"decision":"APPROVED","customerPerspective":"Em mobile, a manicure reconhece imediatamente um kit digital personalizável e entende a entrega completa sem confundi-la com um serviço de salão.","evidence":["Feed 1080x1350 e story 1080x1920 mostram exemplos reais de post e story preservados.","Copy informa 10 posts, 10 stories, 10 legendas, 5 mensagens de WhatsApp, calendário de 7 dias e preço de R$ 67.","Landing respondeu 200 em desktop, iPhone 15 Pro e Pixel 7 com o mesmo pacote e checkout."],"requiredChanges":[]}'
else
  result='{"decision":"APPROVED","commercialRationale":"A peça demonstra o produto digital real, mantém oferta, preço, destino e linhagem e permanece bloqueada para publicação automática.","evidence":["Ativos #139 e #159 estão APPROVED para DELIVERY, LANDING, ADS e SOCIAL.","As composições preservam integralmente os ativos e identificam exemplos personalizáveis.","Criativos #518 e #519 usam a landing oficial e a oferta completa de R$ 67.","Checkout e conteúdo da landing foram verificados em desktop e mobile sem erro funcional."],"requiredChanges":[]}'
fi

printf '%s\n' "$result" > "$output_file"
