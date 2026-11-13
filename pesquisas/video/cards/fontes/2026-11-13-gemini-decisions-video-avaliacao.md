# Gemini Decision 1: API multimodal para decisões estruturadas sobre vídeo

Verificação: 13/11/2026, Brasil (America/Sao_Paulo).
Anúncio oficial: 11/11/2026.
Fornecedor: Google DeepMind, Estados Unidos.
Status: ATIVO pela Gemini API no Google AI Studio (endpoint Decisions API; v1beta no exemplo oficial). Não está confirmado como GA no Google Cloud / Vertex AI; suporte em Gemini no Google Cloud anunciado como futuro.

## O que mudou

Google anunciou o Gemini Decision 1, identificador `gemini-decision-1`, com a nova Decisions API para decidir entre opções fechadas a partir de entradas que podem incluir texto, imagens, áudio, vídeo e PDF. Diferente de um modelo que descreve livremente um vídeo, retorna probabilidades e pontuações numéricas para alternativas predefinidas: sim/não (`noul` no esquema atual), escolha entre 2 e 255 alternativas e avaliação por rubrica ordenada (`score`).

Uma única entrada multimodal, de até 256 mil tokens, pode alimentar até 1.000 perguntas independentes numa solicitação. O conteúdo compartilhado é cobrado uma só vez por requisição; as instruções e opções das perguntas somam tokens de entrada. A saída não é um resumo textual, e sim decisões e scores de confiança.

## Números publicados e limites

- Preço de lançamento oficial: US$ 0,10 por milhão de tokens de entrada, US$ 0 por tokens de saída; nível sem custo disponível para protótipos no AI Studio. Custos por vídeo dependem da codificação, resolução, duração e tokens efetivamente consumidos; não há valor fixo por segundo de vídeo.
- Latência informada pelo próprio Google: 150–250 ms end-to-end e 50–100 ms de computação mediana nos testes com contextos até 16 mil tokens. NÃO extrapolar automaticamente para vídeo longo com 256 mil tokens ou grande quantidade de quadros.
- Economia anunciada pelo Google: quase 5 vezes menos custo que Gemini 3.5 Flash-Lite com Interactions API no conjunto comparativo do fornecedor. Não é benchmark independente nem avaliação específica de vídeos.
- Suporte: Python, JavaScript e REST no AI Studio; integrações OpenRouter, LangChain, Vercel, LiteLLM, Pydantic AI, Pipecat, LiveKit e OpenCode em implantação, não necessariamente todas disponíveis hoje.
- Modelo e serviço proprietários; pesos abertos não anunciados; licenciamento e tratamento de dados dependem dos termos da API, especialmente para vídeos com pessoas.
- Não gera, edita, dubla ou produz áudio/vídeo. Não faz transcrição livre nem legendagem por si só. Classifica conteúdo conforme questões fechadas, com erros possíveis.

## Comparação operacional

- Gemini 3.7 Flash com agentic video understanding: disponível via Gemini API; pode examinar trechos e produzir respostas explicativas abertas, pesquisar momentos e analisar vídeos longos. Divulgado em 01/09/2026; não é lançamento desta rodada. Mais adequado para localizar eventos no tempo, explicar decisões e resumir.
- Gemini Decision 1: ATIVO na Gemini API; ideal para triagem em alto volume, aprovação por rubrica, classificação do tipo de conteúdo, checagem de critérios de marca e roteamento ao próximo modelo. A API não localiza necessariamente o timestamp exato do problema, pois a saída é fechada.
- Gemini Omni 1.2 Flash: ATIVO GA no Google Cloud desde 05/11/2026; gerador e editor de vídeo com áudio sincronizado. É complementar, não concorrente direto de classificação.
- Kling 4.0 (Kuaishou, China): ATIVO via Runway Dev API; gera/edita vídeos com áudio, modelo proprietário. Complementar, não serviço de classificação.
- Seedance 2.5 (ByteDance, China): ATIVO via Runway Dev API; geração/edição multimodal de até 30 s. Complementar, não serviço de classificação.
- Sora 2 (OpenAI, EUA): DESCONTINUADO; app/web encerrados em 26/04/2026 e API em 24/09/2026; não usar em novos pipelines.

## Por que importa para produção audiovisual

A criação generativa tornou fácil produzir muitas tomadas, mas a triagem e a revisão dos resultados continuam custosas. Uma camada de decisão numérica pode avaliar centenas de variantes por marca, produto, estética, legibilidade da embalagem, coerência com o briefing, presença de elementos não autorizados e possíveis defeitos. O resultado deve servir para priorizar revisão humana e, quando necessário, encaminhar a um modelo de compreensão mais detalhado. Probabilidades precisam de calibração específica por tarefa: não transformar score diretamente em garantia de conformidade legal ou de qualidade.

## Experimento proposto para o Marketing Hub

Selecionar 30 vídeos publicitários já revisados e classificados manualmente; criar rubricas fechadas para produto visível, marca legível, cenas sem artefatos, CTA compreensível, enquadramento adequado (vertical/horizontal) e identidade preservada. Para cada vídeo, registrar tokens, preço, latência, avaliações do Decision 1, falsos positivos/falsos negativos e taxa de concordância com a revisão humana. Encaminhar casos de baixa confiança ao Gemini 3.7 Flash agentic video understanding ou a revisores. Não presumir que 150–250 ms incluam o upload e processamento de qualquer arquivo longo.

## Fontes primárias verificadas

1. Google, lançamento em 11/11/2026: https://blog.google/innovation-and-ai/technology/developers-tools/gemini-decisions-api/
2. Google, agentic video understanding (01/09/2026): https://blog.google/innovation-and-ai/models-and-research/gemini-models/introducing-agentic-video-in-gemini/
3. Google Cloud, Gemini Omni 1.2 GA (05/11/2026): https://cloud.google.com/blog/products/ai-machine-learning/gemini-omni-1-2-flash-generally-available/
4. Runway Dev, Kling 4.0 API: https://dev.runway.com/learn/kling-4-0-api
5. Runway Dev, Seedance 2.5 API: https://dev.runwayml.com/learn/seedance-2-5-api
6. OpenAI, datas de encerramento do Sora: https://help.openai.com/en/articles/20001152-what-to-know-about-the-sora-discontinuation

Não foi executada uma chamada de API e nenhum benchmark audiovisual independente nesta rodada.
