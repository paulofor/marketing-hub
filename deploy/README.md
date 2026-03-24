# Deploy automatizado

O diretório `deploy/` contém tudo que é necessário para subir o stack completo no host dedicado (`177.153.62.107`).

## Passos recomendados

1. Gere as imagens (backend, frontend e video-management) e exporte-as para `*.tar`.
2. Copie os arquivos para o servidor (ex.: via `scp`).
3. No host, execute `deploy/bin/apply.sh`. O script irá:
   - carregar os `tar` encontrados em `/tmp`;
   - atualizar as tags `latest` (quando `IMAGE_TAG` for fornecido);
   - aplicar o `docker-compose.yml` com backend, frontend e o novo módulo de vídeo.

### Variáveis relevantes

| Variável | Descrição | Padrão |
|----------|-----------|--------|
| `VIDEO_MGMT_IMAGE` | Nome da imagem do módulo de vídeo | `marketinghub-video-management` |
| `VIDEO_TAR` | Caminho do tar usado no carregamento automático | `/tmp/video-management-image.tar` |
| `VIDEO_BACKEND_BASE_URL` | URL utilizada pelo módulo de vídeo para conversar com o backend | `http://backend:8000` |
| `VIDEO_JOBS_POLLING_ENABLED` | Ativa o polling automático no módulo de vídeo | `false` |
| `VIDEO_MANAGEMENT_PORT` | Porta exposta externamente | `8095` |

> **Importante:** por padrão o poller fica desativado até que o backend esteja totalmente pronto para entregar jobs reais. Basta exportar `VIDEO_JOBS_POLLING_ENABLED=true` antes de rodar `apply.sh` para habilitar.

Assim garantimos que os três serviços principais (backend, frontend e video-management) possam ser atualizados de forma homogênea no servidor 177.153.62.107 sem etapas manuais extras.
