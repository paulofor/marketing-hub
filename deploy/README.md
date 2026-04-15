# Deploy automatizado

O diretório `deploy/` contém os artefatos de deploy. Atualmente os deploys estão separados: backend/frontend em `191.252.181.168` e video-management em `177.153.62.107`.

## Passos recomendados

1. Gere as imagens (backend, frontend e video-management) e exporte-as para `*.tar`.
2. Copie os arquivos para o servidor (ex.: via `scp`).
3. No host de **backend/frontend** (`191.252.181.168`), execute `deploy/bin/apply.sh`. O script irá:
   - carregar os `tar` encontrados em `/tmp`;
   - atualizar as tags `latest` (quando `IMAGE_TAG` for fornecido);
   - aplicar apenas `backend` e `frontend` no `docker-compose.yml`.

### Variáveis relevantes

| Variável | Descrição | Padrão |
|----------|-----------|--------|
| `VIDEO_MGMT_IMAGE` | Nome da imagem do módulo de vídeo | `marketinghub-video-management` |
| `VIDEO_TAR` | Caminho do tar usado no carregamento automático | `/tmp/video-management-image.tar` |
| `VIDEO_BACKEND_BASE_URL` | URL utilizada pelo módulo de vídeo para conversar com o backend | `http://backend:8000` |
| `VIDEO_JOBS_POLLING_ENABLED` | Ativa o polling automático no módulo de vídeo | `false` |
| `VIDEO_MANAGEMENT_PORT` | Porta exposta externamente | `8095` |

> **Importante:** por padrão o poller fica desativado até que o backend esteja totalmente pronto para entregar jobs reais. Basta exportar `VIDEO_JOBS_POLLING_ENABLED=true` antes de rodar `apply.sh` para habilitar.

Esse fluxo (`apply.sh`) permanece para atualizar somente backend/frontend no host principal.

## Deploy apenas do módulo de vídeo

Quando for necessário atualizar **somente** o serviço `video-management` no host `177.153.62.107`, use o script abaixo.

> Neste cenário, o `video-management` deve apontar para o backend remoto em `191.252.181.168` (porta `8000`).

1. Gere a imagem do módulo de vídeo e exporte para tar:
   ```bash
   docker build -f video-management-service/Dockerfile -t marketinghub-video-management:latest .
   docker save marketinghub-video-management:latest -o /tmp/video-management-image.tar
   ```
2. Copie apenas o tar e os arquivos de deploy para o servidor:
   ```bash
   scp /tmp/video-management-image.tar deploy/bin/apply-video-only.sh deploy/docker-compose.yml <usuario>@177.153.62.107:/tmp/
   ```
3. No servidor, mova os arquivos para o diretório de deploy e rode o apply específico:
   ```bash
   ssh <usuario>@177.153.62.107
   sudo mkdir -p /opt/marketinghub/containers
   sudo mv /tmp/docker-compose.yml /opt/marketinghub/containers/docker-compose.yml
   sudo mv /tmp/apply-video-only.sh /opt/marketinghub/containers/apply-video-only.sh
   sudo chmod +x /opt/marketinghub/containers/apply-video-only.sh
   VIDEO_BACKEND_BASE_URL=http://191.252.181.168:8000 sudo /opt/marketinghub/containers/apply-video-only.sh
   ```

Esse fluxo atualiza só o container `marketinghub-video-management`, preservando `backend` e `frontend` em execução.

## Deploy apenas do módulo OPRM

Quando for necessário atualizar **somente** o serviço `oprm-worker` no host `177.153.62.107`, use o fluxo abaixo.

> Neste cenário, o OPRM deve apontar para o backend remoto em `191.252.181.168` (porta `8000`).

1. Gere a imagem do módulo OPRM e exporte para tar:
   ```bash
   docker build -f oprm/Dockerfile -t marketinghub-oprm:latest oprm
   docker save marketinghub-oprm:latest -o /tmp/oprm-image.tar
   ```
2. Copie o tar e os arquivos de deploy para o servidor:
   ```bash
   scp /tmp/oprm-image.tar deploy/bin/apply-oprm-only.sh deploy/docker-compose.yml <usuario>@177.153.62.107:/tmp/
   ```
3. No servidor, mova os arquivos para o diretório de deploy e rode o apply específico:
   ```bash
   ssh <usuario>@177.153.62.107
   sudo mkdir -p /opt/marketinghub/containers
   sudo mv /tmp/docker-compose.yml /opt/marketinghub/containers/docker-compose.yml
   sudo mv /tmp/apply-oprm-only.sh /opt/marketinghub/containers/apply-oprm-only.sh
   sudo chmod +x /opt/marketinghub/containers/apply-oprm-only.sh
   OPRM_BACKEND_BASE_URL=http://191.252.181.168:8000 sudo /opt/marketinghub/containers/apply-oprm-only.sh
   ```

Esse fluxo atualiza só o container `marketinghub-oprm`, preservando os demais serviços já em execução.

