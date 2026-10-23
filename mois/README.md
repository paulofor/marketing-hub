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
- Executor suspenso; destino reservado para retomada autorizada: `163.245.200.7:8094`. Não há instância ativa. O workflow exige `activate=true`, após PR e conferência operacional.

## Endpoints de domínio disponíveis

- `POST /api/v1/mois/discovery-requests` cria pedido de descoberta.
- `POST /api/v1/mois/discovery-requests/{requestId}/run` executa coleta simplificada.
- `GET /api/v1/mois/insight-reports?requestId=&nicheName=&category=` lista relatórios consolidados com filtros básicos da Sprint 5.
- `GET /api/v1/mois/insight-reports/{reportId}` retorna consolidação acionável (`marketOfferInsightReport`) com padrões, saturação, lacunas e diferenciação.
