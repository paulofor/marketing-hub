# Cards com acesso direto à próxima atividade

Execute a partir da raiz do repositório:

```bash
npm --prefix frontend ci --no-audit --no-fund
bash infra/testing/product-next-activity/run-round.sh local
```

O runner executa 87 testes relacionados, TypeScript, build, navegação real no frontend,
Prettier e revisão de whitespace. Requer Node/npm, Chromium e `@playwright/test` resolvível
pelo Node (disponíveis na sandbox). As evidências ficam em
`artifacts/product-next-activity/<rodada>/`.

A extração do hook de leitura também foi conferida pelo teste existente de acompanhamento:

```bash
npm --prefix frontend test -- --run src/api/businessProcess/useProductProcessActivityExecutions.test.tsx
```

O navegador simula GETs oficiais para Vega com segundo ciclo e Rigel sem ciclo. Exercita
início e catálogo, desktop, iPhone 15 Pro e Pixel 7, chegada à âncora da atividade, retorno,
isolamento de produto/ciclo, memória, erro e retentativa, resposta divergente, bloqueio,
conclusão sem continuidade e mudança da atividade orientada. Testes unitários complementam
subprocesso, tarefas pendentes e falha de atualização com cache anterior.

As fixtures são locais e identificadas como QA. Nenhuma sessão comercial, tarefa, campanha,
cobrança ou chamada de IA é criada. Toda escrita e integração externa é bloqueada. O runner
fecha servidor e navegador ao terminar e não cria topologia Docker. A emulação Chromium
não equivale a teste em Safari ou celular físico.

Critérios e evidências: [homologação](../../../docs/homologacao/cards-proxima-atividade-v1.md).
