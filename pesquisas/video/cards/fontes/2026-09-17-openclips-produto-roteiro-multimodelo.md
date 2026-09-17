# Fonte revisada — OpenClips: produto como fonte canônica para roteiro e roteamento multimodelo

Data da revisão: 2026-09-17

## Evidência observada

Em anúncio de 16/09/2026, a OpenClips informou que seu produto passa a aceitar uma URL de produto como entrada e, a partir dela, extrai imagens, recursos e posicionamento, gera roteiro e storyboard cena a cena e entrega o vídeo final. A empresa afirma que o sistema agrega mais de 45 modelos de vídeo e imagem e roteia cada tomada para o modelo considerado mais adequado ao tipo de cena. A página atual de catálogo informa 54 modelos disponíveis no mesmo composer.

O servidor MCP oficial da OpenClips está ativo em `https://mcp.openclips.ai` e expõe ferramentas como `get product`, `list products`, `get brand`, `list skills`, `load skill`, `list endpoints` e `call api`. Isso mostra que um agente pode recuperar contexto estruturado de produto/marca e acionar a produção sem depender de um prompt isolado digitado manualmente.

Os planos pagos informam licença comercial para os renders. O serviço é proprietário; os modelos subjacentes pertencem a diferentes fornecedores e não há indicação de pesos próprios abertos da OpenClips.

Fontes consultadas:
- https://www.prnewswire.com/news-releases/finally-openclips-turns-a-single-product-link-into-a-production-ready-video-ad-302880391.html
- https://openclips.ai/mcp/
- https://openclips.ai/pricing/
- https://openclips.ai/models/

## Interpretação

A evidência sustenta que uma plataforma de produção pode tratar a página oficial do produto como fonte inicial estruturada para planejar um anúncio e que a seleção do renderer pode ser feita por cena. Ela não demonstra que o roteiro extraído automaticamente é melhor que um briefing humano nem que o roteamento multimodelo aumenta conversão.

## Aplicação possível no Marketing Hub

O videomaker pode receber uma fonte canônica do produto/oferta — página, ficha ou contexto comercial aprovado — e derivar dela um briefing estruturado antes de gerar cenas. A partir desse briefing, o harness pode escolher o modelo por tomada conforme requisitos de produto, diálogo, câmera, custo e duração, mantendo claims e dados comerciais ancorados na fonte aprovada.

## Limites

A OpenClips é evidência de produto e arquitetura fornecida pelo próprio fornecedor. Não foram encontrados testes independentes que comprovem ganho de qualidade, custo total ou conversão. A licença comercial do serviço não elimina a necessidade de conferir restrições específicas de assets, marcas, pessoas e modelos terceiros usados em cada render.
