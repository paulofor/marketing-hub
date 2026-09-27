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
| Linhagem | narração do ativo 47 | SHA-256 fixo, transcrição versionada e custo externo zero |
| Promessa | trecho excluído | nenhuma menção a suporte ou correção técnica no vídeo publicado |
| Oferta | quadro final | duas organizações incluídas por R$ 49, pagamento único |
| Readiness | backend em aquecimento | respostas transitórias podem recuperar dentro de 60 segundos |
| Falha real | backend indisponível | smoke permanece vermelho ao esgotar o limite |
| Navegadores | desktop, iPhone 15 Pro e Pixel 7 | vídeo, landing, CTA e checkout utilizáveis |
| Mídia | experimento 93 | permanece `PLANNED`, sem campanha, orçamento ou gasto |

## Evidência determinística

- vídeo: `d28f786b4d4ce3da3c2f8e0576974bcbddcfc61cb171057f8094067f1067b3b6`;
- poster: `0765609b03abb364c2d562f700506541f3eafcb88f487c82fde4148547fb5637`;
- fonte de áudio aprovada: `19be7c4776e20dae6ed783264495e85d97ee2156fc67075e85f79af639e0ef63`;
- fingerprint da fonte frontend: `f55d889c7772c4722150c99a745508455398be24192d083520e8cf64fb08d572`.

O MP4 e o poster canônicos ficam versionados e o build apenas valida e copia os mesmos bytes. Isso
evita que versões distintas do FFmpeg alterem os hashes entre a sandbox e a imagem de produção.

## Evidências locais concluídas

- build Vite e imagem Docker de Mira aprovaram os hashes, codecs, resolução, duração e fingerprint;
- o player Chromium reproduziu vídeo e áudio em desktop, iPhone 15 Pro e Pixel 7;
- nove jornadas comerciais locais aprovaram valor, preço, acesso pago e políticas nos três perfis;
- nove testes do smoke público aprovaram recuperação transitória, falha persistente e a rota pública
  de Vega v7 nos três perfis;
- contratos de release, seleção de deploy e pacote independente de revisão comercial foram
  validados sem reescrever a homologação v1.

As evidências produtivas de publicação, cadastro do ativo, checkout, slot e ausência de mídia serão
acrescentadas ao relatório operacional da solicitação após o fluxo oficial concluir.
