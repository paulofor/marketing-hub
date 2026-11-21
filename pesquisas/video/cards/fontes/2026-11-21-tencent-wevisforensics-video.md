# Fonte revisada — Tencent WeVisForensics: investigação audiovisual por agentes

**Revisão:** 21/11/2026. **Evento:** artigo apresentado em 16/11/2026; código, demo, checkpoints LoRA e guia publicados no repositório em novembro (artefatos confirmados em 19/11/2026). **Origem:** Tencent WeChat Vision, China.

**Status:** ATIVO — código e adapters WeVisForensics-4B disponíveis sob Apache 2.0 para execução própria; dependências e Qwen3.5-4B base devem ser licenciados separadamente. Nenhum endpoint oficial comercial ou preço por minuto documentados.

## Mudança técnica
Em vez de um detector genérico executar uma única classificação, um agente principal examina uma amostra inicial de vídeo e escolhe inspetores para janelas temporais: textura, movimento/física, causalidade, OCR, identificação de falantes e fala. O sistema retorna decisão real/falso com rastros e evidências dos trechos examinados. Adapta o conjunto de inspetores durante treinamento por DTPO.

A Tencent relata **95,2% de acurácia média** em benchmarks próprios (incluindo testes fora da distribuição) versus 86,2% do detector anterior mais forte no agregado. Diferença de **9,0 pontos percentuais**, não redução garantida de erros em vídeos novos. Resultados NÃO reproduzidos nesta rodada. Detector de origem sintética não é prova legal de manipulação; falsos positivos/negativos permanecem.

## Infraestrutura e limitações
Libera ADAPTERS LoRA 4B, não checkpoint autônomo de 4B. Guia oficial mostra execução com GPU de pelo menos 24 GB para vLLM/Transformers e uma opção de múltiplos servidores; exame limitado na demo a janela de até 16 s. Reprodução exige baixar base Qwen3.5-4B e recursos adicionais. Não gera vídeo nem edita, legenda ou dubla. Também não substitui testes de aderência ao briefing; pode informar onde estão artefatos.

## Aplicação ao Marketing Hub
Potencial módulo de auditoria de vídeos gerados, com marcação temporal de problemas visuais, físicos e sonoros. Testar com acervo controlado de clipes próprios em pt-BR e originais reais, rotulados por avaliadores humanos, medindo falsos positivos, explicabilidade e custo por avaliação. Não liberar reprovação automática.

**Fontes primárias (consultadas em 21/11/2026):**
- https://github.com/Tencent/WeVisForensics
- https://arxiv.org/abs/2611.21373
- https://github.com/Tencent/WeVisForensics/blob/main/LICENSE.txt

**Limitação:** resultados dos autores, sem validação independente ou avaliação local.
