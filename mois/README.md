# MOIS (Market Offer Intelligence Service)

Serviço separado do MOIS, criado na Sprint corretiva A para estabelecer a fundação arquitetural fora do `backend/ads-service`.

## Execução local

```bash
mvn spring-boot:run
```

## Endpoints mínimos da fundação

- Health funcional do módulo: `GET /api/v1/mois/health`
- Health de infraestrutura (Actuator): `GET /actuator/health`

## Porta e URL base

- Porta padrão: `8094` (configurável por `MOIS_PORT`)
- Base URL local: `http://localhost:8094`
- Base URL de deploy atual: `http://177.153.62.107:8094`
