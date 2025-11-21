# Lead Portal

Aplicação full-stack que permite aos leads enviarem imagens de referência e acompanharem o resultado do processamento.

## Estrutura

- `backend`: API Spring Boot responsável por receber uploads, persistir metadados no banco de dados MySQL e simular o processamento assíncrono.
- `frontend`: Interface React + Vite para envio de formulários e acompanhamento dos resultados.
- `docker-compose.yml`: orquestra o backend, o frontend e um proxy reverso Nginx.
- `docker/proxy`: arquivos de configuração e certificados usados pelo proxy reverso.

## Executando com Docker Compose

O ambiente dockerizado levanta três serviços:

- `backend`: container com a API Spring Boot (porta interna 8080) conectado a um banco de dados MySQL dedicado.
- `frontend`: container Nginx que serve o build estático do Vite (porta interna 80).
- `proxy`: proxy reverso Nginx que publica as portas 80/443 do host e roteia `/api` para o backend.
- `db`: instância MySQL 5.7 com armazenamento persistente em `./data/mysql`.
- Quando o domínio `iahub.sbs` é acessado, o proxy encaminha o tráfego para o **Marketing Hub** em execução diretamente no host
  (frontend na porta `5173` e backend na porta `8080`). Certifique-se de que esses serviços estejam publicados antes de expor o
  proxy; em ambientes Linux o arquivo `docker-compose.yml` já adiciona o mapeamento `host.docker.internal` → `host-gateway`
  para permitir que o container alcance o host.

### Passo a passo

1. Gere (ou substitua) os certificados TLS colocando os arquivos `dev.crt` e `dev.key` em `docker/proxy/certs/`.
   - Para uso local, já há um certificado autoassinado padrão. Para produção, substitua pelos certificados emitidos pela sua autoridade de confiança (por exemplo, Let’s Encrypt). Consulte a seção [Automação de certificados Let’s Encrypt](#automação-de-certificados-lets-encrypt) para um fluxo completo.
2. Na raiz do projeto `lead-portal`, execute:

   ```bash
   docker compose up --build
   ```

3. Acesse `http://localhost` (ou `https://localhost` aceitando o certificado autoassinado) para abrir o frontend. As chamadas a `/api` serão encaminhadas para a API.

### Variáveis úteis

- `VITE_API_URL`: pode ser ajustada no `docker-compose.yml` caso deseje que o frontend consuma a API em outro caminho.
- `SPRING_PROFILES_ACTIVE`: defina no serviço `backend` se precisar ativar perfis específicos do Spring.

### Resolução de 502 Bad Gateway

Quando o proxy Nginx retorna `502 Bad Gateway` ao acessar um fluxo (por exemplo `/flows/diagnostico`), execute as verificações abaixo para isolar a causa:

1. **Confirme se os containers estão de pé e ligados à rede pública**:

   ```bash
   docker compose -f lead-portal/docker-compose.yml ps
   ```

   Os serviços `frontend`, `backend` e `proxy` devem aparecer como `Up`. Se o `frontend` estiver parado ou reiniciando, o Nginx responderá 502 antes de chegar ao backend.

2. **Inspecione os logs do frontend** para identificar falhas de build ou arquivos estáticos ausentes:

   ```bash
   docker compose -f lead-portal/docker-compose.yml logs frontend
   ```

3. **Teste o backend diretamente** para checar se o fluxo existe e se o serviço responde corretamente:

   ```bash
   curl -H "Host:oportunidadebrasil.shop" https://oportunidadebrasil.shop/api/flows/diagnostico
   ```

   Ajuste o host conforme o domínio publicado. O endpoint deve retornar JSON; uma resposta `404` indica que o fluxo não está cadastrado, enquanto exceções do backend aparecem como `500`.

4. **Cheque o banco de dados**. Os fluxos e submissões são gravados no banco MySQL `lead_portal`. É possível inspecionar diretamente com:

   ```bash
   docker compose -f lead-portal/docker-compose.yml exec db mysql -uleadportal -pleadportal -D lead_portal -e "SELECT slug FROM flows;"
   ```

## Executando localmente sem Docker

### Backend

```bash
cd lead-portal/backend
mvn spring-boot:run
```

A API ficará disponível em `http://localhost:8080`.

### Frontend

```bash
cd lead-portal/frontend
npm install
npm run dev
```

A aplicação web ficará acessível em `http://localhost:5173`. Certifique-se de que o backend esteja em execução ou configure a variável `VITE_API_URL` em um arquivo `.env`.

## Automação de certificados Let’s Encrypt

O script `lead-portal/scripts/provision-letsencrypt.sh` automatiza a emissão ou renovação de certificados gratuitos via Let’s Encrypt, copiando automaticamente os artefatos para `docker/proxy/certs/` e recarregando o proxy.

Pré-requisitos na VPS:

- Docker e Docker Compose (v2) instalados.
- DNS público apontando o domínio desejado para o host.
- Portas 80 e 443 liberadas para o servidor.
- Usuário com permissão para criar/atualizar `/etc/letsencrypt` (use `sudo` se necessário).

### Uso

```bash
./lead-portal/scripts/provision-letsencrypt.sh \
  --domain portal.exemplo.com \
  --email admin@exemplo.com
```

Argumentos relevantes:

- `--domain`: pode ser informado várias vezes para incluir subdomínios adicionais no mesmo certificado.
- `--email`: endereço de contato para notificações de renovação (obrigatório pelos termos do Let’s Encrypt).
- `--staging`: utiliza o ambiente de testes do Let’s Encrypt — útil para validar DNS/firewall antes de consumir a cota de produção.
- `--force-renewal`: força a emissão mesmo que o certificado atual ainda esteja válido.
- `--compose-file`: permite apontar para um arquivo `docker-compose` alternativo caso o proxy esteja definido em outro local.

O script executa um container `certbot/certbot`, portanto ele cria (ou reutiliza) as pastas padrão `/etc/letsencrypt`, `/var/lib/letsencrypt` e `/var/log/letsencrypt` no host. Após concluir a emissão, os arquivos `fullchain.pem` e `privkey.pem` são copiados para `docker/proxy/certs/dev.crt` e `docker/proxy/certs/dev.key`, mantendo a compatibilidade com a configuração do Nginx.

> **Dica:** agende o script via `cron` (por exemplo, diariamente com `--force-renewal`) para garantir renovações automáticas. O Certbot só emitirá um novo certificado quando o atual estiver próximo do vencimento.

## Testes

- Backend: `mvn test`
- Frontend: `npm run lint`
