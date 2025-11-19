# Vitrines

Estrutura inicial para o novo produto de vitrines com frontend em React 18 (Vite + TypeScript) e backend em Java 21 com Spring
Boot 3. Ambos possuem Dockerfiles próprios para gerar imagens que podem ser publicadas no mesmo host que executa o Portal Lead.

## Estrutura

- `frontend`: aplicação SPA construída com Vite, usando TanStack Query para chamadas HTTP e Zustand para estado global.
- `backend`: API Spring Boot com endpoint de saúde em `/vitrines/api/health`.
- Cada pasta possui um `Dockerfile` pronto para gerar as imagens publicáveis.

## Execução local

### Backend

```bash
cd vitrines/backend
mvn spring-boot:run
```

A API fica disponível em `http://localhost:8085/vitrines/api/health`.

### Frontend

```bash
cd vitrines/frontend
npm install
npm run dev
```

A UI fica disponível em `http://localhost:4173`. Configure a variável `VITE_API_BASE_URL` para apontar para o host do backend,
por exemplo `http://localhost:8085/vitrines/api`.

## Builds de imagem (mesmo host do Portal Lead)

Os Dockerfiles foram preparados para gerar imagens independentes. Substitua `<registry>` pelo registro que publica no mesmo host
do Portal Lead.

```bash
# Backend
cd vitrines/backend
docker build -t <registry>/vitrines-backend:latest .

# Frontend
cd ../frontend
docker build -t <registry>/vitrines-frontend:latest .
```

Após publicar as imagens no registro do host, suba os containers apontando o frontend para a API com a variável `VITE_API_BASE_URL`.
