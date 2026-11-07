# Kling 4.0 Flash — abertura de acesso e preços de geração (07/11/2026)

**Data do acontecimento:** 04/11/2026 (anúncio de abertura geral); **Runway Dev:** adição em 05/11/2026. **Data desta verificação:** 07/11/2026.

## Evidência e status
- **Kling 4.0 Flash / Kuaishou (China): ATIVO**. A versão Flash esteve restrita a assinantes Ultra Yearly desde 28/09; em 04/11 passou a ser anunciada para todos os usuários da interface e API. A disponibilidade da API pela Runway Dev está confirmada oficialmente no changelog de 05/11, identificador `kling_4_flash`. Não foi realizada execução autenticada.
- **Kling 4.0 completo: ATIVO** pela Runway Dev; gera até 30 s, 720p ou 1080p. Flash não substitui o modelo completo para cenas finais com maior resolução ou controle de keyframes.

## Capacidades do Flash confirmadas pela Runway
Vídeos de 3–20 segundos, 720p, áudio incluído; texto-para-vídeo, imagem-para-vídeo, vídeo-para-vídeo com edição/referências; até 10 imagens (incluindo primeiro frame) e 5 vídeos com duração combinada de até 20 segundos (incluindo vídeo-fonte). Limites de clonagem de voz, keyframes intermediários e 4K não devem ser atribuídos ao Flash; documentação de outros canais indica que Flash não inclui keyframes intermediários nem 1080p.

## Preço por canal
Na Runway Dev, um crédito custa **US$0,01**. Flash sem vídeo de entrada: **9,4 créditos por segundo = US$0,094/s**; 10 segundos = US$0,94; 20 segundos = US$1,88. Se houver vídeo de entrada: **6,3 créditos por segundo × (duração de saída + maior entre duração de entrada e saída)**, arredondado; nunca tratar US$0,063 como custo final por segundo de saída. Em 10 segundos de saída e 3 de referência, custo 126 créditos = US$1,26.

Runway Kling 4.0 completo em 720p: **12,56 créditos/s = US$0,1256/s** promocionais até 30/11/2026 UTC. Portanto Flash custa cerca de **25% menos** que o modelo completo em promoção, no mesmo canal e sem vídeo de entrada. Comparado ao preço de lista de US$0,157/s do 4.0, cerca de 40% menos. Confirme preço final e tributação antes de integrar.

## Aplicação
Marketing Hub: usar Flash para rascunhos, variações de criativos e testes de abertura de vídeo; finalizar no 4.0 completo ou Omni 1.2 conforme avaliação de qualidade. Comparar custo **por vídeo aprovado** (incluindo rejeições, regenerações e pós-produção), continuidade visual, qualidade de lip-sync em PT-BR e direitos de uso comercial. Nenhum teste objetivo de qualidade foi feito nesta pesquisa.

## Fontes
- Oficial Runway API changelog, 05/11: https://docs.dev.runwayml.com/api-details/api_changelog/
- Oficial Runway preços/créditos: https://docs.dev.runwayml.com/guides/pricing/
- Contexto sobre anúncio 04/11 e limitações: https://www.llms.blog/posts/kling-4-0-flash-open-to-all-api-price-limits
- Detalhes comparativos de modalidades: https://help.artlist.io/hc/en-us/articles/40075326862237-Kling-4-0

**Restrições:** pesos não abertos; limites e licença comercial variam entre planos e intermediários; sem teste autenticado da API direta da Kling.
