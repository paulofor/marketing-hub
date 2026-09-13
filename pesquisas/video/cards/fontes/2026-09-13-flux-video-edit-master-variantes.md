# FLUX Video Edit: edição localizada de vídeo a partir de um master

## Evidência revisada

Em 10 de setembro de 2026, a Black Forest Labs lançou o FLUX Video Edit, um endpoint de vídeo-para-vídeo orientado por prompt. A documentação oficial descreve alterações seletivas de objetos, personagens, cenário, estilo, texto em tela, diálogo e tradução com lip sync. O comportamento declarado preserva duração, enquadramento, movimento de câmera, timing e o áudio de origem quando eles não fazem parte da alteração pedida.

A API aceita MP4 por URL HTTP(S) ou base64, com até 15 segundos e 50 MiB. A saída mantém duração e proporção do vídeo de origem, roda a 24 fps e chega a 720p. A BFL informa cerca de 50 segundos para editar um clipe de 10 segundos e cobra US$ 0,03 por segundo de vídeo de saída, ou US$ 0,30 para um clipe de 10 segundos. O uso pela API inclui direitos comerciais sem licença adicional, segundo a documentação de licenciamento da própria BFL.

A BFL afirma que o modelo é mais rápido e mais econômico do que concorrentes em seus benchmarks internos, mas essa comparação não foi tratada aqui como validação independente.

## Interpretação

O valor técnico não é apenas gerar outro vídeo: é derivar variantes de um master já aprovado preservando a estrutura temporal e alterando somente elementos escolhidos. Isso pode reduzir o volume de regeneração necessário em localização, personalização, mudanças de produto, embalagem, texto ou cenário.

## Aplicação possível no Marketing Hub

Uma peça master pode ser aprovada uma vez e servir como base para variantes de oferta, embalagem, idioma, texto em cena, produto ou cenário. O harness deve manter claims e elementos protegidos como invariantes e liberar somente os campos autorizados para edição.

Isso é uma hipótese de eficiência de produção. Não há evidência nesta fonte de aumento de CTR, checkout, pagamento ou vendas.

## Fontes

- Black Forest Labs, FLUX Video Edit: https://bfl.ai/video-edit
- Black Forest Labs, licenciamento comercial via API: https://help.bfl.ai/articles/9272590838-self-serve-dev-license-overview-pricing
- GIGAZINE, registro do anúncio em 10/09/2026: https://gigazine.net/gsc_news/en/20260911-flux-video-edit/
