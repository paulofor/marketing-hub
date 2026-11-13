# MUSA consultor — validação local

Reutiliza o MySQL 5.7 da topologia `infra/testing/vega-contextual-style/compose.yml`,
sempre com o projeto Compose exclusivo informado pela sandbox. Execute `VegaStyleLocalApplication`
com o classpath de testes do backend e `VEGA_TEST_DB_HOST=sandbox-docker` quando a engine
não publicar portas no loopback da sandbox. O módulo PDE usa
`MusaConsultorGatewayLocalApplication`, porta 18096; a UI usa
`VEGA_STYLE_TEST_BACKEND=http://127.0.0.1:18096 npm run dev:vega-style -- --port 57188`.

Adicione o ciclo sintético 91022 baseado no ciclo 91002 da fixture existente,
com `product_version=musa-consultor-pwa-v1` e experimento 91115. Não use dados reais.
Rode `node infra/testing/musa-consultor/browser.cjs`. O runner aceita somente
127.0.0.1:57188, insere o token exclusivamente sintético apenas na emissão protegida,
e cria acessos pela interface. ADHERENT/RECOVERY/SAFETY em desktop/iPhone/Pixel
são nove combinações; emulação Chromium não comprova Safari ou hardware físico.

`MUSA_CASES=DESKTOP_1440-SAFETY,...` limita a revalidação aos critérios falhos ou
pendentes; os recibos individuais preservam os casos aprovados. Não repetir a matriz
por quantidade mínima. Saídas padrão em artifacts/musa-private-20261113/browser,
sem chaves de acesso ou cookies. Encerrar a topologia pelo mesmo projeto com
`down --volumes --remove-orphans`. Não reutilizar o runner contra produção.
