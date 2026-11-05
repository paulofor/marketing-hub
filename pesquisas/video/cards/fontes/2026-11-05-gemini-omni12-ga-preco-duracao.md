# Fonte revisada — Gemini Omni 1.2 Flash (05/11/2026)

## O que foi efetivamente lançado
- **Publicação oficial de 05/11/2026** no catálogo Google Cloud Agent Platform: modelo `gemini-omni-1.2-flash`, estágio **GA**, região global e modalidades pagas de uso com API **Interactions**. **GenerateContent API não é suportada** por este SKU; não presumir compatibilidade de endpoints.
- Suporta texto->vídeo, imagem->vídeo, referências multimodais, primeiro/último quadro, edição por prompt, extensão e upscale de vídeo com áudio sincronizado; fala, música e efeitos.
- Geração ou edição: de 3 até 15 segundos por chamada; primeiro/último quadro limitado a 3–10 s. Até 3 vídeos de saída por prompt; 360p, 720p, 1080p ou 4K; proporções 16:9 e 9:16. Entrada para edição até 15 segundos, para compreensão até 10 minutos; embora aceite vídeo, o catálogo **não marca Agentic video understanding como suportado**.
- Modelo proprietário hospedado; nenhum peso aberto publicado. Restrições comerciais e regionais regidas pelo contrato Google Cloud; uso real por conta não testado.

## Preço — comparação com 1.1 Flash
- **Tabela oficial Google Cloud**: vídeo gerado na versão 1.2 **US$12/1M tokens**, ante **US$17,50/1M tokens** da 1.1 (queda de aproximadamente 31,4% na tarifa de saída).
- Taxa publicada: 5792 tokens/s de vídeo 720p com áudio => **US$0,069504/s** em 1.2 versus US$0,10136/s na 1.1; 15 segundos em 720p equivalem a US$1,04 de saída antes de custo de referência/entrada e eventuais outras cobranças. 360p: 1931 tokens/s; 1080p: 8688; 4K: 17376.
- Entrada multimodal permanece US$1,50/1M tokens; texto de saída/raciocínio: US$9/1M.
- Página pública pode ser atualizada; conferir cobrança final no projeto antes de escalar.

## Ciclo de vida e contexto
- Google documenta lançamento GA em **05/11/2026**, com aposentadoria **não antes de 05/11/2027**.
- A documentação oficial **revisada em 05/11/2026** para Veo 3.1 GA e Fast informa aposentadoria **04/05/2027 ou posterior**, diferentemente do alerta de 04/11 que citava 17/11/2026 ou depois. Não há evidência de desligamento em 17/11.
- A versão 1.1 permanece listada nos preços; atualização de catálogo não prova superioridade de qualidade ou equivalência a Kling, Seedance, Wan ou Veo para todas as cenas.

## Fontes primárias
- https://docs.cloud.google.com/gemini-enterprise-agent-platform/models/gemini/omni-1-2-flash
- https://docs.cloud.google.com/gemini-enterprise-agent-platform/models/model-versions
- https://cloud.google.com/model-garden/pricing
- https://docs.cloud.google.com/gemini-enterprise-agent-platform/models/veo/3-1-generate
