# HeyGen Avatar 6 — revisão de fontes, 17/11/2026

**Data do evento:** 17 de novembro de 2026 (anúncio público oficial às 12h07 ET). **Status verificado:** ATIVO no site e na API HeyGen; pesos proprietários, sem distribuição pública dos pesos. Havia acesso antecipado para clientes Enterprise divulgado em 12/11; a mudança desta rodada é o lançamento público.

## O que mudou

A HeyGen lançou Avatar 6, especializado em vídeos de pessoas/apresentadores acionados por áudio e roteiro. O modelo interpreta semântica e entonação para variar gestos, expressões, movimentos de mãos e lipsync em vez de repetir movimentos predefinidos. Permite orientar gestos por instruções textuais e texto no momento da fala. A janela de contexto de movimento anunciada cresceu de cerca de três para trinta segundos. Gera até 2K. O laboratório demonstra 25–28 minutos de produção contínua em exemplos, mas não significa uso gratuito ou comercialmente ilimitado — há limites de plano e API.

Um segundo avanço é edição localizada de palavras faladas mantendo os trechos não editados da filmagem; o relatório mostra comparação em fidelidade da área preservada. Também há demonstração de dois apresentadores no mesmo quadro. É importante distinguir demonstração do produto e suporte de endpoint: a documentação de `POST /v3/videos` limita Avatar 6 a pedidos de um único avatar, sem criação de cenas complexas via esse endpoint. Referência de vídeo de pessoa real está limitada a Enterprise com consentimento; foto de referência é mais amplamente permitida.

## Endpoints e limites documentados

Modelo proprietário `heygen-avatar-6` e variante `heygen-avatar-6-turbo`; ambos **ATIVOS/Documentados**, com: `POST /v3/models/avatars` (imagem/vídeo de referência + áudio, parâmetros `motion_prompt`, `motion_expressiveness`) e `POST /v3/videos` (avatar/look e roteiro/voz ou áudio). Resoluções `1080p` e `2k`; a saída 2K usa até 2880×1620 no endpoint Model API para referência 16:9, mas a Application API documenta 2560×1440 para 16:9; não fixar dimensão sem considerar o endpoint. `4k` não é suportado por esses endpoints.

O serviço exige autorização/consentimento de pessoas reais. O vídeo-reference com movimentos de pessoa real depende de acesso Enterprise específico, enquanto referência ilustrada é aceita mais amplamente. Não foi executada requisição autenticada nesta rodada.

## Qualidade: ler os testes corretamente

No teste interno do próprio HeyGen, em avaliação lado a lado com filmagem real, o avatar foi escolhido como o vídeo real em **42,2%** de 270 julgamentos. A cifra publicitária de **84,4%** é uma métrica definida pelo fornecedor como 2×42,2%, ou a proximidade do limite de 50% de escolhas aleatórias; **não** corresponde a 84,4% das pessoas confundindo os vídeos. Na avaliação individual, **65,1%** dos vídeos com áudio real foram julgados reais; contra **78,9%** das filmagens reais no experimento. A HeyGen publicou comparações favoráveis a seu modelo frente a Seedance 2.5, Wan 3.0, Kling 4.0 e fornecedores de avatares; o desenho das avaliações é próprio e não independente. A maior diferença é no caso específico de apresentadores, não em geração genérica de cinema, VFX ou música.

## Acesso, preço e licença

No site há demonstração e primeira geração gratuita sem cartão. Plano Creator anunciado a **US$29/mês / 600 créditos** (regras regionais variam), porém a tabela de preços de API consultada em 17/11 ainda detalha as gerações Avatar III/IV/V e **não traz preço por minuto específico do Avatar 6**. Não extrapolar tarifa do Avatar V. Os créditos da API são separados dos créditos de assinatura web. Confirmar preço exibido antes de uma integração ou campanha. Não foi comprovada licença de pesos abertos, nem fornecimento de infraestrutura própria.

## Comparação e ciclo de vida

- **HeyGen Avatar 6:** ATIVO por web, API e MCP; solução proprietária para vídeo falado/personificação autorizada. O aumento de realismo é evidência interna do fornecedor.
- **HeyGen Avatar IV/V:** Ainda ATIVOS, mas no changelog Enterprise de novembro a opção de exportar 4K foi retirada de drafts que usam IV/V porque são renderizados em 1080p; 2K permanece. Não confundir com encerramento dos modelos.
- **Synthesia Express-2/3 e Personal Avatars:** plataforma concorrente ATIVA como SaaS; testes atuais independentes em português brasileiro não apresentados nesta rodada. Preferência comercial depende de governança, minutos, edição e treinamento.
- **Meituan LongCat-Video-Avatar 1.6:** ATIVO como modelo aberto MIT para execução própria (lançamento 15/11), não há API hospedada oficial confirmada. Difere do serviço fechado HeyGen pela flexibilidade de implantação, porém exige GPU e validação do lipsync.
- **Kling 4.0, Seedance 2.5 e Wan 3.0:** modelos de geração geral ATIVOS via provedores comerciais. Não equivalem a um avatar personalizado de fala contínua; comparação visual é específica da tarefa.
- **Sora 2:** DESCONTINUADO (app 26/04/2026 e API 24/09/2026). Não recomendar para nova integração.

No ecossistema HeyGen, engines de voz **Google** e **Gemini** estão em TRANSIÇÃO, com encerramento agendado para **20/11** e **30/11/2026**, respectivamente; versões anteriores HeyGen Voice `heygen-voice-0` e Fish S1 encerraram em 15/11. Isso se refere à seleção de vozes **dentro da HeyGen**, não aos serviços Google/Gemini externos. Recomenda-se migrar para HeyGen Voice, Mockingbird ou ElevenLabs v4 conforme elegibilidade.

## Experimento de valor para produção

Testar 12–20 vídeos de 20–60 segundos com roteiros em português brasileiro, incluindo verbos que exigem gestos (apontar, contar, mostrar), diferentes emoções, um vídeo com edição de palavra e uma peça de venda. Medir: lipsync, naturalidade das mãos, estabilidade da identidade, consistência de cenário, necessidade de refações, tempo total e custo por vídeo aprovado; comparar com Avatar V e LongCat, se houver GPU. Usar somente imagem/voz com consentimento explícito.

## Fontes consultadas em 17/11/2026

- Anúncio HeyGen/PRNewswire, 17/11: https://www.prnewswire.com/news-releases/heygen-introduces-avatar-6-an-audio-driven-context-aware-ai-model-that-alleviates-repetitive-or-unnatural-motion-in-ai-generated-video-302942527.html
- Página oficial e exemplo: https://www.heygen.com/models/heygen-avatar-6
- Pesquisa oficial, teste e demos: https://www.heygen.com/research/avatar-6-model
- API de produção e limitações: https://developers.heygen.com/docs/models/avatar-6
- Preços da plataforma: https://help.heygen.com/en/articles/15126059-how-to-use-credits-on-heygen
- Tarifas API: https://help.heygen.com/en/articles/10060327-heygen-api-pricing-credits-explained
- Mudanças/encerramentos HeyGen: https://heygen.noticeable.news/publications/november-2026-enterprise-web-releases
- Confirmação do fim Sora: https://help.openai.com/pt-br/articles/20001152-what-to-know-about-the-sora-discontinuation
