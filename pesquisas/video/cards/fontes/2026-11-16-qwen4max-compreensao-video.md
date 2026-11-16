# Qwen4-Max: entendimento multimodal de vídeo por API

**Lançamento de produção: 16/11/2026; prévia lançada em 11/11/2026. Fornecedor: Alibaba/Qwen (China). Status do identificador `qwen4-max`: ATIVO, API de produção/pay-as-you-go. Pesos NÃO ABERTOS.**

## O que mudou
O Qwen4-Max foi disponibilizado em 16/11 na QwenCloud e Alibaba Cloud Model Studio após cinco dias de prévia. Recebe **texto, imagens e vídeo** e gera **texto estruturado**, inclusive JSON, com raciocínio, function calling e ferramentas. NÃO gera vídeo e não é substituto de Kling, Seedance ou Gemini Omni. O contexto anunciado é de 1 milhão de tokens; isso NÃO significa um milhão de quadros nem qualquer duração fixa garantida, pois representação de vídeo consome tokens.

A documentação confirma implantação em Singapore (escopo internacional), Frankfurt, Virgínia, Tokyo, entre outras regiões; a disponibilidade, tratamento de dados e custo devem ser validados na região efetivamente escolhida. Não houve chamada autenticada de teste.

## Preço de referência da API
QwenCloud e Model Studio: **US$ 1,50 por milhão de tokens de entrada** e **US$ 4,50 por milhão de tokens de saída**; cache implícito US$ 0,075 por milhão na QwenCloud. O custo por vídeo analisado depende de amostragem, resolução, tokenização de frames, resposta e eventual cobrança de ferramentas; NÃO há tarifa universal por minuto. Janela máxima 1M tokens; saídas limitadas por modo, consultar documentação.

## Ciclo de vida
O antigo ID `qwen4-max-preview` continua em **prévia e transição**, apenas por Token Plan, com aposentadoria programada para **25/11/2026 às 24h UTC+8**. A versão de produção é `qwen4-max`: recomenda-se migrar o identificador e testar compatibilidade antes de confiar no encerramento da prévia. Não há pesos oficiais Qwen4-Max publicados; a promessa de versões menores com pesos abertos não significa abertura da versão Max.

## Aplicação audiovisual e comparação
Pode ajudar a identificar ações, personagens, objetos, mudanças de cena, incoerências de briefing e sugerir trechos para edição, respondendo em JSON e integrando com agentes. A alternativa Gemini Decision 1, lançada em 11/11, foca decisões e escores; Qwen4-Max serve a análises e planejamento abertos e detalhados. Sem teste independente comum, não há base para afirmar superioridade em qualidade ou custo por vídeo.

## Fontes primárias
- Changelog de lançamento 16/11: https://docs.qwencloud.com/changelog/models
- QwenCloud catálogo e preços: https://www.qwencloud.com/models/qwen4-max
- Alibaba Cloud PT-BR (modalidades, regiões, contexto e tarifas): https://www.alibabacloud.com/help/pt-br/model-studio/qwen4-max
- Página de preview e encerramento programado: https://docs.modelstudio.console.alibabacloud.com/en/model-studio/qwen4-max-preview

**Limitação desta rodada:** sem chamada à API, validação de qualidade em vídeo real ou teste de precisão em português brasileiro.
