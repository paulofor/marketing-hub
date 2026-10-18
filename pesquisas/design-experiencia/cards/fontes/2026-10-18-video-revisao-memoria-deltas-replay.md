# Fonte revisada — memória de mudanças e replay em análise de vídeo

**Fonte externa:** Sun et al. (2026), *DeltaStreamer: Towards Human-Like Agentic Streaming Video Understanding*, arXiv:2610.19849v1, submetido em 14/10/2026. https://arxiv.org/abs/2610.19849

## Evidência encontrada

Os autores propõem uma arquitetura de compreensão de vídeo em fluxo com registro explícito de mudanças entre observações (*deltas*), memória hierárquica rastreável, busca planejada de evidências e reprodução seletiva de quadros anteriores (*replay*). Em benchmarks técnicos, o sistema obteve 66,07 pontos no OVO-Bench e 59,53 no StreamingBench. Em ablação no OVO-Bench, a configuração completa obteve 66,07 e a variante sem replay 63,28. A representação por histórico completo obteve 59,18, contra 66,07 com memória hierárquica e recuperação. Estes são resultados de benchmark, não resultados de experiência humana ou campanhas.

## Hipótese interpretativa — não demonstrada em marketing

Manter a sequência de mudanças e reabrir evidências visuais originais sob demanda pode evitar perda de detalhes relevantes causada por descrições resumidas. A ablação sugere contribuição do replay **no benchmark avaliado**; não demonstra causalidade de melhoria comercial.

## Aplicação possível

Para revisão de vídeos por Apolo e Têmis, registrar timeline de mudanças em objetos, falas, textos, preços e CTAs, armazenar ligações para timestamps e permitir replay dos trechos antes de emitir alertas. Requer controle de privacidade, custo computacional e acesso autorizado ao material.

## Resultado real observado e o que falta testar

**Observado:** pontuação comparativa e ablações técnicas reportadas pelos autores. **Não observado:** melhora em retenção, compreensão, qualidade aprovada por consumidores, confiança, conversão ou faturamento. Teste local proposto: comparar revisão por frames fixos com revisão por deltas+replay, usando avaliadores humanos cegos para mensurar alertas corretos, falsos positivos, latência e custo.
