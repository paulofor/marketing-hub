# Radar de IA para Vídeo — 13/11/2026

**Janela:** revisados anúncios novos e mudanças materiais não cobertas no relatório de 12/11, até 13/11/2026 às 14h50 de Brasília. **Avanço selecionado:** Gemini Decisions API, anunciado oficialmente em 11/11 e ainda não registrado no radar. Não foi localizado um novo modelo de geração de vídeo de importância equivalente lançado especificamente em 13/11.

## 1. Gemini Decision 1 — novo estágio de revisão audiovisual em escala

**Status: ATIVO pela Gemini API / Google AI Studio; integração Google Cloud futura.** O Google lançou a Decisions API com `gemini-decision-1` em 11/11/2026. Ela recebe texto, imagens, áudio, vídeo e PDFs e avalia até 1.000 questões fechadas sobre uma única entrada multimodal de até 256 mil tokens. As saídas são probabilidades e notas de rubrica, não resumos textuais.

Capacidades: sim/não, escolha entre 2 e 255 categorias e avaliação ordenada de critérios de qualidade. Exemplos de uso em vídeo: presença e legibilidade da marca, se o produto aparece, adequação visual à peça, consistência de cenário, presença de elementos indesejados e roteamento à etapa seguinte. Para detectar momento exato, explicar o defeito ou produzir uma legenda, usar sistema de compreensão mais aberto como o Gemini 3.7 Flash com agentic video understanding.

| Parâmetro | Informação divulgada pelo Google |
|---|---|
| Preço | US$0,10 / 1 milhão de tokens de entrada; saída sem cobrança |
| Limite por requisição | Até 1.000 perguntas; até 256K tokens de entrada |
| Latência de referência | 150–250 ms end-to-end em testes com até 16K tokens; não extrapolar para vídeos longos |
| Economia no benchmark do fabricante | Quase 5× menos custo que Gemini 3.5 Flash-Lite/Interactions API em testes compartilhados |
| Acesso | Gemini API / Google AI Studio com nível gratuito para protótipos; Google Cloud ainda futuro |
| Pesos/licença | Pesos não publicados; serviço comercial proprietário sob termos de API |
| Geração/edição audiovisual | Não: classifica e pontua conteúdo existente |

Fonte principal: https://blog.google/innovation-and-ai/technology/developers-tools/gemini-decisions-api/

## 2. Comparação e checagem de ciclo de vida

| Sistema e mercado | Status em 13/11/2026 | Papel na produção |
|---|---|---|
| Gemini Decision 1 — Google, EUA | **ATIVO via Gemini API; sem Google Cloud GA confirmado** | Triagem quantitativa de material audiovisual, classificação em escala |
| Gemini 3.7 Flash com agentic video understanding — Google, EUA | **ATIVO via Gemini API** | Entendimento aberto, busca de momentos, explicação e resumos |
| Gemini Omni 1.2 Flash — Google, EUA | **ATIVO GA no Google Cloud/API** | Geração/edição com áudio de até 15s e referência visual |
| Kling 4.0 — Kuaishou, China | **ATIVO via Runway Dev API** | Geração/edição de vídeos com áudio; até 30s |
| Seedance 2.5 — ByteDance, China | **ATIVO via Runway Dev API** | Vídeo multimodal com múltiplas tomadas e áudio; até 30s |
| OpenAI Sora 2 — EUA | **DESCONTINUADO: app 26/04 e API 24/09/2026** | Apenas referência histórica, indisponível para produção |

A Decisions API não é concorrente direta de Kling, Seedance ou Omni: resolve o gargalo de escolher e revisar o material que esses geradores produzem. O Google já oferecia interpretação ampla de vídeos por agentes desde setembro; o diferencial do Decision 1 é o formato de decisões estruturadas, custo e latência declarados. O índice de quase 5 vezes menor custo é comparação do próprio fornecedor, não teste independente. Não houve mudança de preço dos geradores validada especificamente nesta rodada.

Fontes de status:
- https://blog.google/innovation-and-ai/models-and-research/gemini-models/introducing-agentic-video-in-gemini/
- https://cloud.google.com/blog/products/ai-machine-learning/gemini-omni-1-2-flash-generally-available/
- https://dev.runway.com/learn/kling-4-0-api
- https://dev.runwayml.com/learn/seedance-2-5-api
- https://help.openai.com/en/articles/20001152-what-to-know-about-the-sora-discontinuation

## 3. Mercado e outras notícias avaliadas

Foi verificada a publicação de 13/11 da Reverie Movies sobre abordagem API-first. O texto informa API já ativa e aplicativo ChatGPT ainda em revisão, mas não estabelece que a API foi lançada hoje, nem preço e disponibilidade suficiente para tratar a publicação como avanço competitivo significativo neste radar. Site: https://www.reveriemovies.com/updates/api-before-chatgpt-app .

Notícias gerais sobre modelos multimodais de raciocínio que apenas **aceitam** vídeo, sem avanço específico em criação/edição/análise audiovisual confirmado, foram descartadas do destaque desta rodada.

## 4. Aplicação no Marketing Hub

Executar prova de conceito em 30 vídeos de campanha previamente aprovados ou reprovados manualmente, avaliando seis rubricas. Registrar custos reais (entrada multimodal + perguntas), tempos, falsos positivos, falso negativos e coerência com avaliadores humanos. Usar baixa confiança como gatilho para um modelo que explique o defeito ou revisor humano. Comparar tempo e custo por vídeo final aprovado, não apenas preço por token.

## 5. Arquivos e integridade

Card: `pesquisas/video/cards/2026-11-13-gemini-decision1-video-triagem-qa.json`
Fonte revisada: `pesquisas/video/cards/fontes/2026-11-13-gemini-decisions-video-avaliacao.md`
SHA-256 exato do UTF-8 da fonte: `5a3d44719436af4c01237a08a89311e87f843f6483ffc933adca57c3157d4619`

**Limites desta verificação:** não foi executada a API, não houve teste independente de velocidade/qualidade, nem auditoria comercial de termos de licenciamento de vídeos pessoais.
