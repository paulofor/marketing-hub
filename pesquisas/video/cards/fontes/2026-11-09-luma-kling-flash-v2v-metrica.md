# Fonte revisada — Luma: Kling Flash e nova base de cobrança de video-to-video (09/11/2026)

## Fontes
- Página oficial atual, modelos e consumo em créditos: https://lumalabs.ai/pricing
- Comparação externa de capturas da página Luma entre 02 e 09/11/2026: https://pricingsaas.com/companies/lumaai/diffs/2026W46
- Referência alternativa de preço API Kling Flash: https://dev.runwayml.com/

## Fato, data e status
Em comparação de preços observada entre 02 e 09/11/2026 (com registro externo anterior em 07/11), a Luma passou a exibir **Kling 4.0 Flash** e **Kling 4.0 Flash Omni** no marketplace e alterou a redação da cobrança de Video-to-Video. Status: **ATIVO na plataforma Luma**; o Kling 4.0 Flash já era ativo via API da Runway antes desta rodada. A inclusão no marketplace não equivale ao lançamento de um novo modelo fundacional ou à abertura de pesos.

## Tarifas atuais, em créditos Luma
- Kling 4.0 Flash, texto/imagem para vídeo 720p: **32 créditos por segundo gerado**, com áudio.
- Kling 4.0 completo, texto/imagem para vídeo 720p: **53 créditos por segundo gerado**, com áudio. A variante Flash consome cerca de 39,6% menos créditos para o mesmo tempo/resolução; qualidade e recursos não equivalentes.
- Kling 4.0 Flash Omni, Video-to-Video 720p: **21 créditos por segundo da soma de vídeo gerado + referência**.
- Seedance 2.0, Video-to-Video 480p: **15 créditos por segundo da soma vídeo gerado + referência**; tarifa capturada anteriormente era ~30 créditos por segundo, aparentemente com base diferente.
- Planos Luma: Plus US$30/mês (US$25/mês se pago anualmente), 10.000 créditos; Pro US$90/mês (US$75 anual), 40.000 créditos. Créditos de plano não são a mesma unidade econômica que dólares de uma API.

## Comparação justa de custo Video-to-Video
Exemplo com tarifa antiga de 30 créditos/s de **saída** versus atual 15 créditos/s de **saída + entrada**:
- 10s entrada / 10s saída: antigo 300 créditos; novo 300. **Não houve redução efetiva**.
- 5s entrada / 10s saída: antigo 300; novo 225. Economia de 25%.
- 15s entrada / 10s saída: antigo 300; novo 375. Aumento de 25%.
Esses exemplos apenas ilustram a nova base de cobrança; não são cotações de uma requisição executada nem confirmação contratual do formato antigo.

## Aplicação para Marketing Hub
Normalizar preços por custo total da operação (segundos de entrada, saída, resolução, áudio, preço do crédito e refações) antes de escolher Luma, Runway ou API direta. Proposta de teste de dez clipes de 10 segundos em 720p, sem áudio separado, com logging por fornecedor. Evitar chamar a mudança de 'desconto de 50%' sem informar que a base de cálculo mudou.

## Riscos
Sem teste autenticado; preço dos créditos por assinatura não deve ser comparado de forma direta com preço de API por segundo; qualidade ainda não aferida; o status de API Luma para cada variante não foi confirmado.
