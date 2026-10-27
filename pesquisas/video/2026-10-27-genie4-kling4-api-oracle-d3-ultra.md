# Radar de inteligência artificial para vídeo — 27/10/2026

**Filtro:** mudanças significativas confirmadas até 27 de outubro de 2026. Diferenciar evento anunciado, disponibilidade real e local/canal de API. A pesquisa prioriza fontes oficiais e reportagem contemporânea, separando resultados reportados pelo fabricante de benchmarks independentes.

## 1. Kling 4.0 na Runway Dev: API ativa desde 23/10

**Status: ATIVO via API comercial Runway Dev; API direta Kling ainda LIMITADA.** Identificador `kling_4`, gera e edita vídeos de 3–30 segundos com áudio, 720p/1080p, referências de imagem e vídeo. Máximo de 10 imagens/quadros, até 5 vídeos de referência totalizando 30s, segundo combinação aceita pela rota. Sem 4K confirmado para a versão 4.0 Runway. A disponibilidade de API pública via Runway corrige a ideia anterior de que não existia meio geral de automação para a versão 4.0 (a API direta do fabricante permanece diferente).

**Preços promocionais Runway de 20% até 30/11/2026 UTC:** sem vídeo de entrada, US$0,1256/s (720p) e US$0,188/s (1080p); os valores padrão são US$0,157/s e US$0,235/s. Com vídeo de entrada, faturamento considera segundos de saída MAIS o maior entre segundos de vídeo de entrada e segundos de saída, aplicando por parcela US$0,0752/seg 720p ou US$0,1128/seg 1080p na promoção; checar arredondamento. Exemplo 10s sem vídeo de entrada: US$1,256 ou US$1,88.

Comparadores de API atualmente ativa: Runway Gen-4.5 (US$0,12/s) e Wan 3.0 (US$0,10/s em 720p), e Google Veo 3.1 GA no Cloud (ativo). Não inferir que preços iguais compram a mesma qualidade, durações ou recursos.

Fontes: https://docs.dev.runwayml.com/api-details/api_changelog/ ; https://dev.runwayml.com/learn/kling-4-0-api ; https://docs.dev.runwayml.com/guides/pricing/

## 2. Google DeepMind Genie 4: direção audiovisual interativa (27/10)

**Status: LIMITADO — prévia de pesquisa para trusted testers.** Simula mundos com som, música ambiente, diálogos e mudanças em tempo real de câmera, clima ou objetos; duração de cena anunciada de até 2 minutos. Foi apresentado também um protótipo de direção por Gemini 4 Argon, que planeja ações e inspeciona quadros. Não é API de geração comercial pública, nem possui pesos abertos/preço público. O acesso futuro via Project Genie/Google AI Ultra e parceiros API/AI Studio foi anunciado para semanas seguintes, não está comprovado hoje. Ainda ocorrem deriva visual, inconsistência de áudio e texto ilegível dentro da cena.

Concorrência: Runway GWM Worlds 2 (preview limitado) e Tencent Hy-Nomas (pesos de pesquisa/execução própria); diferente de Veo 3.1 GA, que continua uma API de vídeo de produção. Relevância: pré-visualização de cinema e anúncios e futura direção interativa por agentes, não integração imediata no Marketing Hub.

Fonte: TechCrunch, publicado 27/10/2026 https://techcrunch.com/2026/10/27/googles-genie-4-world-model-adds-audio-more-realistic-worlds-and-real-time-steering/

## 3. Oracle AI Media Models: entrada em vídeo gerativo corporativo (27/10)

**Status: LIMITADO — beta para clientes selecionados OCI.** Oracle anunciou modelos próprios separados para texto→vídeo, texto→imagem e edição de imagem por texto; acessíveis via endpoints gerenciados aos clientes selecionados, com governança e customização. Oracle afirma treinamento em conteúdo com direitos liberados. Não há comprovação independente de qualidade ou garantias contratuais universais; nem preços públicos, duração/resolução de vídeo ou pesos disponíveis, então não recomendar como alternativa prontamente acessível aos fabricantes chineses ou Runway. Importante para pipelines corporativos e direitos de mídia.

Fonte: Oracle, publicação oficial 27/10/2026 https://blogs.oracle.com/ai-and-datascience/oracle-ai-media-models

## 4. vLLM Semantic Router d3-ultra: compreensão/decisão multimodal (pesos 25–26/10)

**Status: ATIVO — pesos publicados sob Apache-2.0, inferência própria possível, mas pesado.** Não gera vídeo; recebe imagens, vídeo e texto e pode classificar conceitos, apontar regiões e rastrear elementos. Modelo ~26 bilhões de parâmetros, exige infraestrutura considerável para execução local. Testes internos do fabricante não comprovam superioridade sobre Cloudflare Clef-omni (API ativa, outro sistema para revisão automática). Nenhuma precificação comercial geral do d3-ultra validada. Aplicação: controle de qualidade de vídeos, presença/posição de marca e verificação de itens, com validação de falsos negativos.

Fonte: ficha oficial dos pesos https://huggingface.co/vllm-sr/d3-ultra

## 5. Apple Creator Studio: ferramentas anunciadas em 27/10, lançamento em novembro

**Status: EM TRANSIÇÃO — suíte existente ativa; novos recursos anunciados, ainda não disponíveis em 27/10.** Final Cut Pro receberá ferramentas MotionVFX, rotoscopia, motion tracking, acervo de vídeos e ida/volta com Pixelmator; Logic Pro receberá assistente beta e melhorias de separação de stems. A edição por prompt anunciada para Pixelmator é de IMAGEM e não deve ser descrita como vídeo para vídeo. Atualização prevista para novembro sem dia especificado; US$12,99/mês ou US$129/ano para novos assinantes nos EUA. Não é novo gerador fundacional de vídeo.

Fonte: Apple Newsroom, 27/10/2026 https://www.apple.com/newsroom/2026/10/apple-creator-studio-adds-powerful-new-ai-features-and-even-more-creative-tools/

## Implicações Marketing Hub

1. **Piloto imediato:** `kling_4` Runway por API, 3 roteiros publicitários e 3 conjuntos de referência, comparar fidelidade de produto, áudio, latência e custo por tomada aprovada com Wan 3.0 e Gen-4.5.
2. **Pesquisa:** Genie 4 para arquitetura de agentes diretores; sem integração até abertura operacional e avaliação de licenças.
3. **Monitorar:** Oracle (preço, disponibilidade e benchmark comercial) e d3-ultra (precisão do controle de qualidade e infraestrutura). Apple (novembro).
4. Não apresentar modelos em preview como sistemas de produção imediata; não confundir pesos liberados com liberdade de uso comercial, nem preço de geração bruta com custo por material aprovado.
