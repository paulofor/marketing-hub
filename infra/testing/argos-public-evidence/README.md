# Homologação de Argos com fontes públicas

Não usa modelo, pagamento, busca externa nem pessoas reais. As fontes `.example` são sintéticas.

1. Instalar dependências em `product-discovery-worker` e `frontend`.
2. `node infra/testing/argos-public-evidence/worker-fixture.mjs /tmp/argos-public-worker.json`
3. `ARGOS_PUBLIC_EVIDENCE_FIXTURE=/tmp/argos-public-worker.json mvn -B -f backend/ads-service/pom.xml -Dtest=ProductDiscoveryPublicEvidenceWorkerIntegrationTest test`
4. `npm --prefix frontend run build`
5. `ARGOS_PUBLIC_EVIDENCE_FIXTURE=/tmp/argos-public-worker.json node infra/testing/argos-public-evidence/browser.cjs`

O callback provém do worker, é validado no backend e usado no build frontend com endpoints
simulados. O navegador valida desktop/iPhone/Pixel, POST idempotente, loading, falha, retry,
polling até conclusão e observações com fontes, sem converter lacuna em aprovação.

O Compose deste diretório somente constrói a imagem versionada e confere schemas em container
sem rede. Use o projeto exclusivo da sandbox e remova-o com `down --volumes --remove-orphans`.
A matriz, migração MySQL e resultados estão em `docs/homologacao/argos-public-evidence-v1.md`.
