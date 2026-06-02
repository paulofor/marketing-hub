# Template Básico — Endpoints do Backend (estilo Swagger)

> Objetivo: documentar somente o essencial de cada endpoint.

## Local do contrato Swagger/OpenAPI

- Todo arquivo Swagger/OpenAPI deve ser criado e mantido em `docs/swagger`.
- Use nome do módulo ou integração no arquivo, por exemplo `docs/swagger/<modulo>-swagger.yaml`.
- Este template textual pode orientar a escrita, mas o contrato versionado deve ficar na pasta centralizada.

## Informações da API

- **Módulo**: `<MOIS | OPRM | MDS | CORE | ...>`
- **Versão**: `v1`
- **Base URL**: `http://191.252.181.168:8000`
- **Auth**: `<Bearer token | sem auth>`

---

## Índice de Endpoints

| Método | Rota | Resumo |
|---|---|---|
| GET | `/api/modulo/recurso/{id}` | Buscar por ID |
| POST | `/api/modulo/recurso` | Criar recurso |

---

## Template por Endpoint

### `<MÉTODO> <ROTA>`

- **Resumo**: `<descrição curta>`
- **Auth**: `<sim/não>`

### Path Params

| Campo | Tipo | Obrigatório | Exemplo |
|---|---|---|---|
| `id` | `UUID` | Sim | `550e8400-e29b-41d4-a716-446655440000` |

### Query Params

| Campo | Tipo | Obrigatório | Exemplo |
|---|---|---|---|
| `page` | `int` | Não | `0` |
| `size` | `int` | Não | `20` |

### Request Body (quando aplicável)

```json
{
  "campoA": "valor",
  "campoB": 10
}
```

### Responses

- **200 OK**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "status": "SUCCESS"
}
```

- **400 Bad Request**
- **401 Unauthorized**
- **404 Not Found**
- **422 Unprocessable Entity**
- **500 Internal Server Error**

### Exemplo cURL

```bash
curl -X <MÉTODO> 'http://191.252.181.168:8000<ROTA>' \
  -H 'Content-Type: application/json' \
  -H 'Authorization: Bearer <token>' \
  -d '{"campoA":"valor","campoB":10}'
```
