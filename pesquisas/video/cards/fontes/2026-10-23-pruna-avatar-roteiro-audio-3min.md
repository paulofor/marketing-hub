# Fonte revisada — avatar falante com voz e custos observáveis (Pruna P-Video-2-Pro-Avatar)

Data de revisão: 2026-10-23.
Fornecedor: Pruna AI (modelo proprietário pela API Pruna; construído sobre MiniMax H3).
Data identificada do anúncio: 2026-10-22. Status: API ativa/documentada.

## Evidências verificadas
1. Página do fornecedor: geração de avatar falante a partir de imagem de retrato e áudio ou roteiro, com movimento labial e facial sincronizado.
2. Documentação primária: retorno MP4 a 24 FPS, duração de 1 a 180 segundos, 50 requisições/minuto. O usuário pode enviar arquivo de fala ou `voice_script`; o áudio fornecido tem prioridade e determina a duração.
3. Preços oficiais por segundo: US$0,02 (480p), US$0,04 (768p), US$0,06 (1080p), US$0,08 (2K) e US$0,12 (4K). **1080p/2K/4K usam upscaling sobre 768p**, logo não significam geração nativa nessas resoluções. No 4K há limite de 125s de áudio.
4. Texto-para-fala integrado: 30 vozes, com dez configurações de idioma ou sotaque descritas na API, incluindo `Portuguese (Brazil)`. A página comercial cita mais idiomas, mas a enumeração da API governa a integração.
5. Não foram identificados pesos desta variante disponíveis para execução própria, nem licença open source. Os direitos comerciais e de personalidade exigem validação contratual; não assumir permissão irrestrita.

## Leitura para o Marketing Hub
A geração de vídeos falados de 30 a 180 segundos por roteiro permite criar explicações, anúncios narrativos ou criativos estilo UGC sem filmagem recorrente. Como a voz pode ser fornecida externamente, o sistema pode separar a decisão sobre a voz do renderizador do rosto. Preço mensurável facilita testes controlados, mas preço por segundo não informa custo por criativo aproveitável.

Hipótese para testar: com retrato autorizado ou sintético, Pruna reduz custo total e tempo de produção de versões narradas de 30–60s em relação a montagem manual e renderização de clipes curtos, mantendo compreensão da mensagem e sincronização adequadas.

Medições sugeridas: custo por vídeo aprovado, latência, sincronismo dos fonemas em português brasileiro, consistência de aparência, alterações involuntárias de cenário e resultado criativo. Fazer primeiro avaliação humana e técnica; não divulgar como pessoa real sem autorização.

## Limites
Os benchmarks de realismo e precisão não foram verificados de forma independente. Os preços podem refletir promoção de lançamento; conferir cobrança efetiva por conta. Serviço é de geração de vídeo, não avatar de conversação full-duplex ou SDK offline.

## Fontes primárias
- https://www.pruna.ai/p-video-2-pro-avatar
- https://docs.api.pruna.ai/guides/models/p-video-2-pro-avatar
- https://docs.pruna.ai/en/stable/docs_pruna_endpoints/performance_models/p-video-2-pro-avatar.html

## Fonte secundária de data de lançamento
- https://www.thereelstack.com/lab/pruna-p-video-2-pro-avatar-minimax-h3-talking-portrait
