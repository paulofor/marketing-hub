# Radar de IA para Vídeo — 15/11/2026

**Janela de verificação:** 14–15 de novembro, contrastada com o relatório de 14/11. **Uma mudança importante confirmada:** a API oficial do **Seedance 2.5 Flash** passou a constar na documentação atualizada da BytePlus/ModelArk para 15/11. O modelo fora anunciado em 12/11; o marco deste relatório é a abertura documentada do acesso direto à API.

## 1. ByteDance / BytePlus: Seedance 2.5 Flash por API própria

**Status: ATIVO/documentado por API ModelArk, sujeito a elegibilidade e região; pesos proprietários.** Identificador: `dreamina-seedance-2-5-flash-261115`. A documentação oficial confirma texto para vídeo, imagem para vídeo, primeiro/último quadro, referências multimodais e geração com áudio. Duração de **4 a 30 segundos**, resolução **480p ou 720p**, 24 fps, até 10 imagens, 5 vídeos e 5 áudios de referência (com limites adicionais). Não suporta 1080p; a versão completa Seedance 2.5 suporta até 1080p.

O preço público de exemplo de um clipe **16:9 de 5 segundos a 720p**, sem vídeo de entrada, é **US$ 0,76 (Flash)** e **US$ 1,156 (completo)**: queda de aproximadamente **34%**. Os preços por milhão de tokens são US$ 7,00 versus US$ 10,70 para 480p/720p. Em uma edição com vídeo de referência, o custo pode ser muito maior: a própria BytePlus mostra **US$ 0,84 a US$ 3,25** para 5 segundos de saída 720p com entrada de 2 a 30 segundos, dada a cobrança pela duração de entrada. Valores de casos específicos; não são uma tarifa universal por segundo.

A ByteDance afirma melhoria de velocidade de quase 2× versus o completo, com qualidade próxima, mas **não houve validação independente nesta rodada**. Não foi feita chamada de API ou análise visual própria. A mudança relevante é operacional/comercial: existe uma opção direta do fornecedor, além dos parceiros.

Fonte primária: https://ai.byteplus.com/ark/region:ap-southeast-1/docs/model-pricing ; https://ai.byteplus.com/ark/region:ap-southeast-1/docs/video-generation-tutorial ; https://docs.byteplus.com/en/docs/ModelArk/seedance-2-5 .

## 2. Comparação com concorrentes e disponibilidade

| Modelo (mercado) | Status em 15/11 | Faixa de geração | Preço de referência para 720p |
|---|---|---|---|
| Seedance 2.5 Flash (China) | **ATIVO, API própria documentada** | 4–30 s; som; até 720p | US$ 0,76/5 s (US$ 0,152/s) |
| Seedance 2.5 completo (China) | **ATIVO por API** | 4–30 s; som; até 1080p | US$ 1,156/5 s (US$ 0,231/s) |
| Kling 4.0 Flash (China) | **ATIVO na Runway Dev API** | 3–20 s; som; 720p | US$ 0,094/s sem vídeo de entrada |
| Gemini Omni 1.2 Flash (EUA) | **ATIVO, GA/API** | 3–15 s por geração; áudio e edição | Aprox. US$ 0,070/s na API Google Cloud |
| Seedance 1.5 Pro (China, BytePlus) | **DESATIVADO em 11/11** | Serviço encerrado | Não aplicável |
| Sora 2 (EUA) | **DESCONTINUADO** | Sem API ativa | Não aplicável |

As tarifas pertencem a canais diferentes e não provam qualidade equivalente. Maior duração e mais referências favorecem algumas cenas no Seedance; preço mais baixo do Omni e Kling favorece outras. Para estimar custo real, considerar vídeo de entrada, tentativas recusadas, créditos e disponibilidade regional.

**Ciclo de vida confirmado:** a BytePlus especifica que `seedance-1-5-pro-251215` foi desligado após 17h de **11/11/2026 (UTC+8)**. Este encerramento na API oficial é distinto do fim previamente acompanhado em parceiros da ElevenLabs. Fonte: https://ai.byteplus.com/ark/region:ap-southeast-1/docs/model-deprecation-notice .

## 3. Outros movimentos monitorados

Há relato secundário de novos formatos 4:3/3:4 para Kling 4.0 em 14/11, porém não foi possível validar o changelog primário acessível nesta rodada. Não tratado como anúncio de grande importância nem recebeu card. Também não identifiquei lançamento aberto com pesos e condições comerciais verificadas no intervalo que justifique novo alerta.

## 4. Implicações para o Marketing Hub

Priorizar teste A/B de produção em escala: 20 cenas curtas sob os mesmos briefs no Seedance Flash e no modelo completo, medindo custo por vídeo final **aprovado**, tempo de fila, consistência do produto/personagem, qualidade de áudio e número de refações. Confrontar com alternativas ativas quando duração e referências permitirem; não comprar infraestrutura ou assumir economias de qualidade sem testes.

## 5. Card e fonte revisada

- Fonte: `pesquisas/video/cards/fontes/2026-11-15-seedance25-flash-api-modelark.md`
- SHA-256 UTF-8 da fonte: `c307c27c5bc2d2d5d8c5d5a947ac994135cc26a0ec1474d473c03febbdb17e52`
- Card: `pesquisas/video/cards/2026-11-15-seedance25-flash-api-modelark.json`

**Verificação:** preços e disponibilidades constatados por documentação do fornecedor; sem acesso autenticado nem execução de inferência nesta rodada. Alterações regionais e contratuais devem ser verificadas antes de uso comercial.
