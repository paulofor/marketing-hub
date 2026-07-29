# Social Media Worker

Executor operacional de midias sociais do Marketing Hub.

## Objetivo comercial

Transformar videos de PDEs e experimentos em aquecimento organico de mercado,
comecando pelo YouTube. O modulo consome pendencias do backend, publica ou
planeja a publicacao e devolve resultado auditavel para relatorios.

## YouTube v1

O YouTube Data API v3 permite publicar videos no canal autenticado por
`videos.insert`, criar playlists e organizar secoes do canal. A API nao cria um
canal novo do zero para uma conta comum; por isso, a primeira versao trabalha
com canal ja conectado/autenticado no Marketing Hub.

Fluxo operacional:

1. Backend expoe pendencias em `/api/social-distribution/publications/pending`.
2. Worker valida a acao solicitada.
3. Worker publica video no canal autenticado ou roda em `dryRun`.
4. Worker reporta sucesso ou bloqueio ao backend para relatorio.

## Variaveis

- `SOCIAL_BACKEND_BASE_URL`: URL do backend.
- `SOCIAL_BACKEND_AUTH_TOKEN`: token interno opcional.
- `SOCIAL_YOUTUBE_DRY_RUN`: mantem publicacao simulada quando `true`.
- `SOCIAL_YOUTUBE_ACCESS_TOKEN`: OAuth token do canal YouTube conectado.
- `SOCIAL_YOUTUBE_DEFAULT_PRIVACY_STATUS`: `private`, `unlisted` ou `public`.

## Limites importantes

- Criacao de canal YouTube deve ser tratada como onboarding/conexao do canal,
  nao como chamada automatica de API.
- Upload publico real exige projeto Google Cloud, OAuth, YouTube Data API v3 e
  possivel auditoria do Google para remover restricao de videos privados em
  projetos nao verificados.
