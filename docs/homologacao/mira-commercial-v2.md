# Homologação comercial de Mira v2

Data: 2026-09-27

## Decisão audiovisual

Foram comparadas três alternativas antes do cadastro do vídeo no experimento 93:

1. declarar que o vídeo silencioso possuía áudio permitiria avançar rápido, mas falsearia o contrato
   operacional da tela e publicaria um criativo diferente da evidência registrada;
2. gerar uma nova voz paga daria controle total sobre a locução, porém abriria novo ciclo, custo e
   espera sem necessidade, apesar do teto audiovisual de R$ 80;
3. reutilizar somente o trecho comercialmente fiel da voz do ativo 47 já aprovado, eliminar todo o
   visual interno e excluir a frase sobre “correção técnica”.

A terceira alternativa foi escolhida. Ela conserva a qualidade da voz aprovada, custa US$ 0 de
provedor e mantém a promessa exata: desejo reconhecido, três passos, prova real e duas organizações
incluídas por R$ 49.

## Decisão de readiness

Foram comparadas falha imediata, aceitação irrestrita de indisponibilidade e espera limitada. A
falha imediata já produziu falso negativo durante a troca do backend; ignorar a rota permitiria
publicar checkout quebrado. A espera limitada foi escolhida: o smoke tenta a oferta comercial por
até 60 segundos e continua falhando com o último HTTP observado quando a indisponibilidade persiste.

## Matriz de aceite da revisão

| Área | Cenário | Aceite |
|---|---|---|
| Vídeo | build versionado | 15 segundos, 1080×1920, H.264, AAC e `faststart` |
| Streaming | HLS canônico | VOD com manifesto finalizado, segmentos independentes, H.264/AAC e fallback MP4 |
| Linhagem | narração do ativo 47 | SHA-256 fixo, transcrição versionada e custo externo zero |
| Promessa | trecho excluído | nenhuma menção a suporte ou correção técnica no vídeo publicado |
| Oferta | quadro final | duas organizações incluídas por R$ 49, pagamento único |
| Readiness | backend em aquecimento | respostas transitórias podem recuperar dentro de 60 segundos |
| Falha real | backend indisponível | smoke permanece vermelho ao esgotar o limite |
| Navegadores | desktop, iPhone 15 Pro e Pixel 7 | vídeo, landing, CTA e checkout utilizáveis |
| Mídia | experimento 93 | permanece `PLANNED`, sem campanha, orçamento ou gasto |

## Evidência determinística

- vídeo: `e1123f4bf456dcfb459bc2709c5db7800c742178fc6f2b8db165ab552abb6b43`;
- poster: `7964614ac7a5c7e42006473be7c0deabff17f45281bd39ec05f30e9ab6d9a081`;
- manifesto HLS: `a3d3041da52212c9657784ffb440818f994ac04548b84e9d3c703ee0705c64b8`;
- segmentos HLS: `fe9b831708d1561f9abec01705dc42106b9dce54dc2777e1f0a250a5ec0049b1`,
  `cd4e1fdac720cbda99a749d74df4e81ecee4316a32f0ac86a416de179b1dace8` e
  `89df0ef96996105f951691f0768d3144172eabaf1438bada1769a023146bd058`;
- fonte de áudio aprovada: `19be7c4776e20dae6ed783264495e85d97ee2156fc67075e85f79af639e0ef63`;
- fingerprint da fonte frontend: `51b3c454839d49c01147c98d58956326012f0de37572659296c1c25a824d5e52`.

O MP4, o poster, o manifesto e os segmentos HLS canônicos ficam versionados; o build apenas valida e
copia os mesmos bytes. Isso evita que versões distintas do FFmpeg alterem os hashes entre a sandbox e
a imagem de produção.

## Evidências locais concluídas

- build Vite aprovou hashes, codecs, resolução, duração, HLS e fingerprint; a imagem Docker integra a
  validação completa antes do PR;
- o player Chromium reproduziu vídeo e áudio em desktop, iPhone 15 Pro e Pixel 7;
- quinze jornadas comerciais locais aprovaram valor, preço, HLS com fallback MP4, acesso pago e
  políticas nos três perfis;
- a atestação imutável v3 de Mira e a atestação de compatibilidade v14 de Vega preservam as provas
  anteriores e vinculam os hashes atuais sem liberar mídia ou republicar Vega;
- o pacote compartilhado conferiu 223 arquivos em 36 manifestos; 142 testes de Psique e 105 de
  Têmis passaram com as novas atestações;
- nove testes do smoke público aprovaram recuperação transitória, falha persistente e a rota pública
  de Vega v7 nos três perfis;
- contratos de release, seleção de deploy e pacote independente de revisão comercial foram
  validados sem reescrever a homologação v1.

As evidências produtivas de publicação, cadastro do ativo, checkout, slot e ausência de mídia serão
acrescentadas ao relatório operacional da solicitação após o fluxo oficial concluir.
