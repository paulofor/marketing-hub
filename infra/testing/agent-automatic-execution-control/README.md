# Topologia local do controle PLAY/STOP

Esta topologia sobe MySQL 5.7, backend e frontend isolados para homologar o controle operacional dos
agentes sem acessar banco, campanhas, providers ou credenciais reais. O backend usa Hibernate para
montar o schema efêmero porque o changelog mestre legado pressupõe um banco já evoluído; o novo
changeset deve ser aplicado separadamente em uma base mínima MySQL 5.7 durante a homologação.

Pré-requisitos gerados localmente:

```bash
cd backend/ads-service && mvn -DskipTests package
cd ../../frontend && npm run build
```

Execução:

```bash
docker compose -p mh-agent-control-local \
  -f infra/testing/agent-automatic-execution-control/docker-compose.yml up -d --build
```

Ao concluir:

```bash
docker compose -p mh-agent-control-local \
  -f infra/testing/agent-automatic-execution-control/docker-compose.yml down --volumes --remove-orphans
```
