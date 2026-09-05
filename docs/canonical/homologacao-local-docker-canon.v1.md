# Homologação local Docker — cânone v1

## Objetivo

Evitar esgotamento de disco durante homologações extensas sem apagar imagem produtiva, base reutilizável,
artefato de outra execução ativa ou evidência necessária ao diagnóstico.

## Contrato obrigatório

1. Imagem Docker criada apenas para teste local deve usar o namespace `aihub-homologation/` e os
   rótulos `com.marketinghub.homologation.temporary=true` e
   `com.marketinghub.homologation.session=<sessão>`.
   A sessão faz parte do nome do repositório Docker: deve usar letras minúsculas, números e
   separadores válidos. O timestamp automático usa `t` e `z` minúsculos; maiúsculas são recusadas
   antes do build para evitar `invalid reference format`.
2. O caminho canônico de build é `scripts/docker-build-temporary-image.sh`, executado dentro de
   `scripts/run-docker-homologation.sh`. O wrapper cria e bloqueia a sessão, inicia a coleta periódica e
   faz uma coleta final ao encerrar, inclusive quando o teste falha.
3. A coleta remove somente tags que coincidam com a sessão declarada no rótulo. Imagens recentes,
   sessões ativas, imagens referenciadas por qualquer container, tags externas ao namespace e imagens
   sem metadados íntegros são preservadas.
4. A passagem periódica padrão ocorre a cada dez minutos e alcança somente sessões encerradas com
   imagens de ao menos uma hora. A coleta final da própria sessão não espera essa janela.
5. Coletores concorrentes usam um lock único. Uma segunda passagem deve sair sem erro e sem mutação.
6. O modo `AIHUB_DOCKER_CLEANUP_DRY_RUN=true` deve listar decisões sem remover referências.
7. Cada passagem relata candidatas, referências removidas, sessões ativas, imagens recentes, imagens em
   uso, referências protegidas, metadados inválidos e recusas de remoção.
8. `docker image prune -af`, `docker system prune -af` e `docker builder prune -af` são proibidos para
   homologação: não expressam propriedade nem protegem a janela entre build e uso.
9. O coletor atua somente na engine Docker isolada da sandbox. Não acessa VPS, registry, produção ou
   Docker socket montado em container.
10. Topologias Compose usam o identificador exclusivo entregue pelo ambiente e sempre encerram com
    `docker compose -p <projeto> down --volumes --remove-orphans`. A limpeza de imagens complementa,
    mas não substitui, esse encerramento.

## Uso

Para uma imagem isolada:

```bash
AIHUB_HOMOLOGATION_IMAGE_TAG=round-1 \
  bash scripts/run-docker-homologation.sh \
  bash scripts/docker-build-temporary-image.sh backend \
  -f backend/ads-service/Dockerfile backend/ads-service
```

Para uma matriz com vários builds, o comando passado ao wrapper deve ser um script versionado; todos os
`docker build` temporários desse script devem passar pelo helper. O wrapper exporta
`AIHUB_HOMOLOGATION_SESSION` para os subprocessos.

Uma inspeção sem mutação pode ser executada assim:

```bash
AIHUB_DOCKER_CLEANUP_DRY_RUN=true \
  bash scripts/cleanup-temporary-docker-images.sh once
```

Esses comandos não autorizam remover imagens antigas sem rótulo. O legado deve ser tratado por decisão
explícita e evidência de que nenhuma execução ainda depende dele.
