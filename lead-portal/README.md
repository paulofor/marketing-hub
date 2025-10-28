# Lead Portal

Aplicação full-stack que permite aos leads enviarem imagens de referência e acompanharem o resultado do processamento.

## Estrutura

- `backend`: API Spring Boot responsável por receber uploads, persistir metadados em memória e simular o processamento assíncrono.
- `frontend`: Interface React + Vite para envio de formulários e acompanhamento dos resultados.

## Executando localmente

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
