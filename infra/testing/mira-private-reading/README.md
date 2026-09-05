# Homologação local da leitura privada de Mira

Executar no workspace com Java 21, Maven, npm, Chromium/Playwright e Docker/Compose. Instalar as
dependências dos dois frontends com `npm ci` antes da primeira rodada.

```bash
bash scripts/run-docker-homologation.sh bash infra/testing/mira-private-reading/run-round.sh round-1
bash scripts/run-docker-homologation.sh bash infra/testing/mira-private-reading/run-round.sh round-2
```

Cada rodada executa os testes dos módulos, builds, contrato Actions e navegação em desktop, iPhone
15 Pro e Pixel 7. Usa MySQL 5.7 real e isolado para eventos e o backend PDE real. O backend
administrativo executa controller, consulta autenticada, handler e executor BPM reais com
repositories e predecessor simulados; a UI usa o componente produtivo numa página de teste.
Não representa teste de todos os controllers administrativos nem leitura humana produtiva.

As imagens de teste empacotam o JAR e o frontend construídos localmente; usam a configuração Nginx
versionada. Isso funciona também na engine remota da sandbox, que não enxerga bind mounts locais.
O runner descobre o endereço da engine por `DOCKER_HOST`, usa o projeto Compose exclusivo e remove
containers/volumes ao terminar. O wrapper remove somente suas imagens temporárias etiquetadas.

Evidências ficam em `tmp/mira-reading-<rodada>/`, fora do Git. São esperados oito eventos privados
**sintéticos** (cinco da leitura positiva e três da negativa) e cinco eventos QA, sem efeitos
comerciais. `MIRA_SKIP_SUITE=true` permite iteração diagnóstica e não conta como rodada completa.

Para publicação, o backend principal precisa do mesmo `PDE_INTERNAL_API_TOKEN` do PDE. O workflow
versionado grava esse segredo por stdin SSH num arquivo protegido e o Compose o importa por
configtree; o segredo não compõe a imagem. Os convites humanos existentes permanecem no cofre do
PDE e não são expostos por consultas administrativas públicas.
