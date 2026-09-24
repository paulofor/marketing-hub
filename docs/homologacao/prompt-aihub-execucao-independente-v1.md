# Prompt AIHUB para execução independente

Data: 24/09/2026. Escopo: botão no detalhe `/business-process-executions/:executionId`.

## Evidência e decisão

A execução #27 foi consultada pela tela e pelo endpoint oficial: definição #90 v7,
referência `product-discovery-cycle:66`, tarefa #477 bloqueada e nenhuma candidata.
O botão não existia. Trata-se de inclusão de ajuda contextual; corrigir ou retentar a pesquisa
histórica não faz parte desta entrega e não foi executado.

O endpoint `GET /api/independent-business-process-executions/{executionId}` já fornece o
contrato necessário, com controller por contexto, serviço canônico e DTOs em records.
O frontend somente formata esse contrato. Não há nova consulta, persistência, endpoint ou
mudança Java necessária para copiar texto. O template compartilhado mantém entrega por PR,
melhoria verificável de agentes, cinco critérios comerciais e limites de gasto.

| Alternativa | Benefício | Risco e esforço | Decisão |
| --- | --- | --- | --- |
| Duplicar o prompt | Isolamento da nova tela | Divergência das regras, manutenção duplicada | Descartada |
| Criar endpoint de prompt | Centralizar texto montado | Novo contrato e deploy backend sem dado novo | Descartada |
| Reutilizar template, cópia e detalhe oficial | Consistência e menor custo de manutenção | Requer regressão dos prompts de produto | Escolhida |

## Matriz definida antes da execução dos testes

| Critério | Aceite |
| --- | --- |
| Caminho feliz | Botão copia instruções e uma única fotografia da execução; prévia idêntica |
| Escopo sem produto | Definição, versão, execução, ciclo e tarefas corretos; vínculos ausentes explícitos |
| Estados | Bloqueado, concluído com lacunas, aguardando entrada, em execução e sem relatório preservam o backend |
| Privacidade | Não copiar prompts, requestKey, entrada arbitrária, resultado/evidência brutos |
| Consumo | Zero conhecido preservado; custo/tokens ausentes não convertidos em zero |
| Falhas | Clipboard negado oferece texto integral selecionável; requisição pendente/erro não permite cópia desatualizada |
| Navegação | Nova execução substitui identidade, dados, prévia e confirmação anterior |
| Integração | Build real e APIs simuladas; cópia não faz POST/PUT/PATCH/DELETE nem chama worker/modelo |
| Dispositivos | Chromium desktop, iPhone 15 Pro e Pixel 7 emulados; HTTP, toque e teclado; sem overflow do componente |
| Regressão | Testes frontend, TypeScript, build, Prettier dos arquivos alterados e diff |
| Publicação | PR/checks/revisões, SHA main, deploy, Watchdog e cópia real em produção |

Dados sintéticos e snapshots somente leitura ficam isolados das métricas de negócio. Não se
mede conversão ou vendas neste teste. Emulação Chromium não equivale a Safari/iOS físico.

## Resultado

Validação local concluída:

- 761 testes em 176 arquivos do frontend aprovados; a rodada focal final passou em 32 testes.
- TypeScript, build Vite, Prettier dos arquivos TypeScript/JavaScript/fixture e `git diff --check`
  aprovados. Permanecem avisos anteriores de tamanho de bundle e API CJS do Vite.
- 36 verificações no build real: seis critérios em desktop/iPhone/Pixel, tanto em HTTP
  inseguro quanto em contexto seguro. Cópia/colagem real, prévia e seleção manual preservam
  texto integral, identidades, acentos e custos desconhecidos. Zero mutações e erros de página.
- Regressão do prompt de produto aprovada nos mesmos seis perfis, incluindo estados, ausência
  de ciclo, contexto incompleto, clipboard negado e retorno ao contexto oficial.
- O primeiro teste de erro esperava uma mensagem diferente da mensagem existente na tela;
  a expectativa foi corrigida sem alterar o comportamento. A regressão de navegador também
  foi atualizada para a abertura legítima “processo corrente no escopo da execução”.
- Tela e MCP confirmaram a identidade histórica: #27, definição #90 v7, ciclo de descoberta
  #66; #26 permanece na definição #52/ciclo #65. Nenhuma execução foi reiniciada.

Reprodução a partir da raiz, após `cd frontend && npm ci && npm run build`:

```text
node infra/testing/independent-execution-prompt/browser.cjs
PROCESS_COPY_KIND=aihub node infra/testing/process-context-copy/browser.cjs
```

O primeiro runner grava capturas, textos realmente colados e `results.json` em
`artifacts/independent-execution-prompt` (ou `PROMPT_TEST_OUTPUT`). Cada runner encerra seu
servidor e navegador. A publicação e a validação final ficam vinculadas ao PR desta entrega.
