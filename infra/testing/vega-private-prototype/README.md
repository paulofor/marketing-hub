# Homologação privada de Vega

Matriz: `docs/homologacao/vega-tarefa-380-prototipo-v1.md`.

1. `docker compose -p <projeto exclusivo da sandbox> -f infra/testing/vega-private-prototype/compose.yml up -d --wait`.
2. Compilar os testes do backend e gerar o classpath de teste com `mvn test-compile dependency:build-classpath`.
3. Iniciar `VegaPrivateLocalApplication` com esse classpath. A aplicação fixa porta 18080, banco
   `vega_local`, usuário/senha sintéticos, Liquibase da fixture e proíbe host externo.
   `VEGA_TEST_DB_HOST=sandbox-docker` atende a engine dedicada; o padrão é `127.0.0.1`.
4. Iniciar `mock-openai.mjs` (18081), `serve-built.mjs` (18083) e o worker com backend
   `http://127.0.0.1:18080`, `OPENAI_RESPONSES_URL=http://127.0.0.1:18081/responses`,
   `PDE_INTERNAL_API_TOKEN=vega-local-internal-only`, `OPENAI_API_KEY=synthetic`,
   `VEGA_OPENAI_MODEL=gpt-5-mini` e uma caixa de saída exclusiva sob `artifacts/vega380`.
5. `bash infra/testing/vega-private-prototype/run-round.sh final1` e, depois de qualquer
   correção, duas rodadas completas consecutivas. Reiniciar a aplicação quando o Java mudar.
6. Encerrar processos locais e executar `docker compose -p <mesmo projeto> -f
   infra/testing/vega-private-prototype/compose.yml down --volumes --remove-orphans`.

A fixture usa produto/ciclo/experimento 91004/91002/91092. Somente o cadastro de produto é
simulado; sessão, ciclo, execução, callback, auditoria e eventos usam repositories e MySQL reais.
O modelo é um test double HTTP explícito, com casos de saída inválida e indisponibilidade.
O harness é o mesmo script versionado usado por Psique. iPhone e Pixel são emulações Chromium.
A integração exercita também HUMAN sintético somente no banco local, depois revoga o convite.
Isso não é leitura humana ou prova comercial. Nenhum teste chama pagamento, Meta ou SMTP real.

As imagens de publicação são construídas pelos Dockerfiles do repositório. O pacote comercial de
Psique é preparado pelo mesmo script do CI: `node scripts/build-commercial-review-evidence.mjs .
customer-agent-worker/review-evidence`. `check-images.sh` usa `VEGA_COMPOSE_PROJECT` exclusivo,
`VEGA_LOCAL_HOST` (IP da sandbox) e `VEGA_TEST_DOCKER_HOST` (IP da engine). Ele testa a imagem
de Psique com o worker e o frontend de Vega reais, TLS com certificado sintético e o proxy versionado.
Na engine remota isolada, arquivos são copiados com Compose; não há bind mount dependente
do filesystem do cliente. Evidências e dados temporários usam tmpfs limitado e são exportados
antes de remover o container, protegendo o disco da sandbox. O outbox produtivo continua persistente.
O primeiro comando de cada rodada deve exportar essas três variáveis para `run-round.sh`.
