# Lead Portal

Aplicação full-stack que permite aos leads enviarem imagens de referência e acompanharem o resultado do processamento.

## Estrutura

- `backend`: API Spring Boot responsável por receber uploads, persistir metadados em memória e simular o processamento assíncrono.
- `frontend`: Interface React + Vite para envio de formulários e acompanhamento dos resultados.
- `docker-compose.yml`: orquestra o backend, o frontend e um proxy reverso Nginx.
- `docker/proxy`: arquivos de configuração e certificados usados pelo proxy reverso.

## Executando com Docker Compose

O ambiente dockerizado levanta três serviços:

- `backend`: container com a API Spring Boot (porta interna 8080).
- `frontend`: container Nginx que serve o build estático do Vite (porta interna 80).
- `proxy`: proxy reverso Nginx que publica as portas 80/443 do host e roteia `/api` para o backend.

### Passo a passo

1. Gere (ou substitua) os certificados TLS colocando os arquivos `dev.crt` e `dev.key` em `docker/proxy/certs/`.
   - Para uso local, já há um certificado autoassinado padrão. Para produção, substitua pelos certificados emitidos pela sua autoridade de confiança (por exemplo, Let’s Encrypt).
2. Na raiz do projeto `lead-portal`, execute:

   ```bash
   docker compose up --build
   ```

3. Acesse `http://localhost` (ou `https://localhost` aceitando o certificado autoassinado) para abrir o frontend. As chamadas a `/api` serão encaminhadas para a API.

### Variáveis úteis

- `VITE_API_URL`: pode ser ajustada no `docker-compose.yml` caso deseje que o frontend consuma a API em outro caminho.
- `SPRING_PROFILES_ACTIVE`: defina no serviço `backend` se precisar ativar perfis específicos do Spring.

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

## Testes

- Backend: `mvn test`
- Frontend: `npm run lint`
