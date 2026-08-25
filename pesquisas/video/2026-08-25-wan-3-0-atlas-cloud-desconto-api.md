# IA para vídeo — 2026-08-25

## Mudança relevante: Wan 3.0 fica mais barato via Atlas Cloud

Em 25 de agosto de 2026, a Atlas Cloud colocou o **Wan 3.0** da Alibaba em produção por uma API unificada e anunciou um **desconto de 20% no modelo Standard**, reduzindo o preço inicial de **US$ 0,05/s para US$ 0,04/s**. O modelo **Prime** também recebeu desconto, de **US$ 0,068/s para US$ 0,061/s**.

### Status atual

- **Wan 3.0 / Alibaba Cloud Model Studio:** ativo/oficial.
- **Wan 3.0 / Atlas Cloud:** ativo em API de produção.
- **Wan 3.0 Standard / Atlas:** promoção ativa a partir de US$ 0,04/s.
- **Wan 3.0 Prime / Atlas:** promoção ativa a partir de US$ 0,061/s.
- **Pesos abertos:** não; o Wan 3.0 continua proprietário.

### O que o Wan 3.0 oferece

A implementação da Atlas mantém os principais recursos do Wan 3.0: texto para vídeo, imagem para vídeo e referência para vídeo, duração de 2 a 30 segundos, 480p/720p/1080p, áudio nativo sincronizado, primeiro e último frame, até 10 imagens + 5 vídeos + 5 áudios de referência e, no modo Omni/Deep Thinking, documento ou página web como entrada criativa.

Isso é especialmente interessante para publicidade, demos de produto, vídeos corporativos e social media porque um briefing, PDF, deck ou página de produto pode virar material de referência diretamente para a geração.

### Comparação de custo

A própria Atlas afirma que o valor de **US$ 0,04/s** está abaixo do preço inicial publicado diretamente pela Alibaba, de **US$ 0,05/s em 480p**. Um vídeo de 30 segundos, na tarifa mínima promocional da Atlas, parte de aproximadamente **US$ 1,20**.

Para comparação, o **Veo 3.1 Fast** direto no Google custa atualmente **US$ 0,10/s em 720p com áudio** e **US$ 0,12/s em 1080p com áudio**. A comparação não é perfeitamente equivalente porque a tarifa promocional de US$ 0,04/s da Atlas é divulgada como preço inicial e a página pública não detalha de forma inequívoca o preço promocional por resolução. Portanto, não deve ser interpretada automaticamente como US$ 0,04/s em 1080p.

### Por que isso importa

Essa mudança reforça um ponto importante para uma arquitetura de produção automatizada: **o preço do mesmo modelo pode variar bastante entre provedores**. Não é suficiente escolher o modelo; também vale rotear cada geração para o provedor com melhor combinação de preço, disponibilidade, região, retenção de dados e limites de uso.

Para alto volume, um roteador de modelos/provedores pode comparar dinamicamente Alibaba direto, Atlas, Google/Veo, Seedance e outros endpoints antes de cada job. A Atlas ainda oferece uma única API para centenas de modelos, o que reduz custo de integração, mas seus termos proíbem simples revenda da API sem valor agregado.

### Licença e uso comercial

O Wan 3.0 continua sendo um modelo proprietário. A Atlas permite uso de sua API para uso pessoal ou interno de negócios e deixa claro que o usuário é responsável por revisar direitos de terceiros nos outputs. Os termos do Alibaba Model Studio atribuem o output ao usuário, sujeitos às regras do serviço e às leis aplicáveis. Para produção comercial, ainda é necessário validar direitos sobre marcas, rostos, músicas, documentos e referências enviados ao modelo.

### Observação sobre outras novidades do dia

A Series Entertainment também colocou hoje o **RUN** em beta público, com Game Studio, Story Studio, Video Studio e Adventure Studio. É um movimento interessante de produção de entretenimento assistida por IA, mas ainda faltam documentação técnica suficiente de modelos, API, preços, limites e licença do Video Studio para classificá-lo como uma alternativa madura a Runway, Veo, Seedance, Wan ou LTX.

## Fontes

- Atlas Cloud — Wan 3.0 e promoção: https://www.atlascloud.ai/
- Atlas Cloud — página do Wan 3.0: https://www.atlascloud.ai/models/wan-3.0
- Atlas Cloud — política de uso: https://www.atlascloud.ai/docs/acceptable-use
- Alibaba Cloud Model Studio — Wan 3.0: https://modelstudio.alibabacloud.com/intl/blog/wan3-ai-video-generation-model/
- Google Cloud — preços do Veo: https://cloud.google.com/gemini-enterprise-agent-platform/generative-ai/pricing
- Series Entertainment / Business Wire — lançamento do RUN: https://www.businesswire.com/news/home/20260825044477/en/Series-Entertainment-Launches-RUN-the-AI-Native-Hub-Where-Creators-Build-Ship-and-Earn
