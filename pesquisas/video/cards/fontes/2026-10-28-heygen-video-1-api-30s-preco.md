# Fonte revisada — HeyGen Video 1: API, duração e tarifa de outubro

**Verificado em:** 2026-10-28. **Histórico:** modelo já divulgado em setembro de 2026; expansão e divulgação de outubro, documentação atualizada em 26/10. **Empresa:** HeyGen (Estados Unidos), variante proprietária baseada e pós-treinada em MiniMax H3 (China). **Status:** ATIVO por API paga e na aplicação/web; testes gratuitos restritos em página pública. **Pesos:** não divulgados para HeyGen Video 1.

## Evidência primária
- Documentação da API: https://developers.heygen.com/docs/models/heygen-video
- Help Center, atualizado 26/10: https://help.heygen.com/en/articles/17272995-introducing-heygen-video-generate-cinematic-clips-from-a-prompt
- Modelo web e oferta: https://www.heygen.com/models/heygen-video-1
- HeyGen Voice Turbo, 21/10: https://www.heygen.com/blog/heygen-voice-turbo

## Funcionamento comprovado pela documentação
Modelo `heygen-video-1`, endpoint `POST /v3/models/videos`. Texto -> vídeo, imagem -> vídeo e referências -> vídeo, diálogo/efeitos/ambiência no vídeo. Formatos MP4/H.264 24fps com áudio AAC estéreo 32kHz. Resolução 480p, 768p, 1080p ou 2K (nomenclatura de tamanho; não inferir resolução nativa). Duração 5–30 segundos somente T2V/I2V com enhancement turbo ou quality; referência -> vídeo e outras configurações continuam limitadas a 5–15 segundos. Via MCP, 5–15 segundos. São três regras distintas, não generalizar 30s para todas as rotas. API requer chave paga.

## Preço
Help Center, atualizado em 26/10, publica promoção até 31/10: T2V/I2V até 15s US$0,01/s 480p; US$0,018/s 768p; US$0,04/s 1080p; US$0,05/s 2K. Para gerações acima de 15s: US$0,045/s 480p, US$0,075/s 768p, US$0,105/s 1080p/2K. Referência->vídeo: US$0,035/s 480p, US$0,05/s 768p; cobranças consideram entrada de vídeo e saída. A página geral menciona 'a partir de US$0,01/s'; isso corresponde à camada 480p e não deve ser generalizado. Para 768p, a tabela atualizada é fonte específica preferida. Preço padrão divulgado para novembro: US$0,02/s 480p e US$0,03/s 768p até 15s; outras faixas sem confirmação estável. A própria divulgação apresenta arredondamentos/variações, confirmar no painel antes da chamada.

## Comparação/limites
Kling 4.0 via Runway (China/EUA) está ativo, permite até 30s e referências com preço promocional US$0,1256/s 720p sem vídeo de entrada, porém formatos/qualidade não diretamente comparáveis. HeyGen se destaca por tarifa muito menor para vídeos curtos com áudio; qualidade e taxa de aprovação exigem teste independente. Condições de uso comercial e restrições sobre material de treinamento/imagem/voz devem ser revisadas no contrato antes de produção. HeyGen Voice Turbo foi disponibilizado sem tarifa de API até 30/11, mas sem garantia de disponibilidade (respostas 429 quando esgotada capacidade), não equivale a áudio ilimitado em produção.

## Aplicação recomendada
Usar referências próprias de produto, criar 10 clips de 8–12s em 768p e comparar com Kling 4 e MiniMax H3 usando o mesmo roteiro, marca, voz e enquadramento, calculando custo por vídeo aprovado e fidelidade de embalagem. Não inferir conversão ou supremacia de qualidade dos resultados promocionais do fabricante.
