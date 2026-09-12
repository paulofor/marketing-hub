# Financeiro de vídeos por ciclo

## Escopo e decisão

Solicitação: tela para informar o teto total de produção e revisão do anúncio e da demonstração
na entrada. Nenhuma autorização real foi fornecida nesta solicitação. Toda gravação de teste
deve ocorrer na sandbox.

| Alternativa | Benefício | Risco / esforço | Decisão |
| --- | --- | --- | --- |
| Ampliar o monitor de provedores | Reutiliza a área financeira | Mistura saldo da conta com autorização por experimento; esforço médio | Manter apenas link entre as telas |
| Campo livre no ciclo | Esforço baixo | Não fornece consulta, histórico e limites estruturados claros | Preservar somente compatibilidade histórica |
| Tela dedicada vinculada ao ciclo | Teto compartilhado, rastreabilidade e retorno ao trabalho | Esforço médio; exige contratos e concorrência | Escolhida por resolver o impedimento sem duplicar o ledger |

Fontes: `LearningCycleVideoEvidence.brief`, `LearningCycleService.workLinks`,
`VideoProviderFinancePage`, `docs/homologacao/vega-processo6-recuperacao-ciclo-v1.md`, cânones
dos ciclos e de Apolo/Plutus. O histórico do experimento 92 não autoriza herdar o teto do 91.
Loops consultados: `LOOP-VIDEO-PREFLIGHT-ACIONA-PRODUCAO` e
`LOOP-BPM-VIDEO-GENERICO-SEM-ENTREGA-POR-DESTINO`.

A consulta somente leitura à UI administrativa e ao MySQL pelo MCP confirmou Vega #4,
cadeia #14, ciclo #2, experimento #92, versão `musa-pde-entry-v12-primeiro-ajuste-aplicavel`,
etapa `VIDEO_BRIEF`, revisão 7 e sete eventos. A tela existente oferecia referência livre
de orçamento. A leitura não registrou autorização nem alterou esse contexto.

O contrato novo usa o controller/service canônico de ciclos e o ledger já existente; não exige
migração. A referência do teto vigente é preenchida no briefing e o backend anexa valor, moeda e
escopo oficiais, recusando outra referência. A avaliação independente de Plutus permanece própria.

## Matriz definida antes dos testes

| Área | Critério obrigatório |
| --- | --- |
| Caminho feliz | Selecionar produto/ciclo, conferir contexto, registrar teto, recarregar e consultar recibo e histórico; referência chega ao briefing |
| Validações | Valor positivo com até duas casas, moeda USD explícita, responsável/justificativa e confirmação; ausência de valor não recebe padrão |
| Identidade | Recusar produto, cadeia, experimento, versão ou revisão divergentes; não escolher outro ciclo ao abrir URL inválida |
| Concorrência | Repetição idêntica é idempotente; chave reutilizada com outro conteúdo e revisão antiga são recusadas |
| Histórico | Substituição preserva eventos; versão nova não herda autorização; ciclo encerrado/etapa posterior apenas consultam |
| Integração | Controller/service/repositories reais em API local com MySQL 5.7; fontes adjacentes simuladas; nenhuma rede de provider |
| Observabilidade | Resposta e log com contexto, data, responsável, escopo, recibo e causa de erro; o ciclo não avança ao registrar teto |
| Métricas | Nenhuma campanha, venda, receita, reserva, tarefa paga ou crédito criado pelo formulário; testes isolados do ambiente produtivo |
| Interface | Chromium desktop, iPhone 15 Pro e Pixel 7 emulados; formulário, erro, clique pendente, links de ida/retorno, teclado e ausência de overflow |
| Regressão | Testes relevantes backend/frontend, TypeScript, build, formatação e diff revisados |

Uma rodada completa sem defeitos basta. Após correção revelada pela matriz, duas rodadas completas
consecutivas sem falhas são obrigatórias. Emulação Chromium não representa Safari nativo.

## Resultado

Duas rodadas locais completas, consecutivas e sem falhas aprovaram a implementação.

| Verificação | Rodada 1 | Rodada 2 |
| --- | ---: | ---: |
| Backend e arquitetura | 271 testes | 271 testes |
| Frontend | 53 testes | 53 testes |
| Integração REST / MySQL 5.7 | 6 grupos | 6 grupos |
| Navegação desktop, iPhone e Pixel emulados | 36 controles | 36 controles |
| Erros de página / chamadas externas da jornada | 0 / 0 | 0 / 0 |
| TypeScript, build, Spotless, Prettier, Swagger e diff | Aprovados | Aprovados |
| Auditoria do teto nos logs da API local | Confirmada | Confirmada |
| Limpeza de containers, rede e volumes de teste | Confirmada | Confirmada |

Não houve teste ignorado nas suítes executadas. Os 22 arquivos de implementação e testes
permaneceram idênticos entre as rodadas, conforme manifesto SHA-256. Evidências brutas e
capturas: `artifacts/video-finance/round1` e `artifacts/video-finance/round2`; resumos em
`artifacts/video-finance/round1-summary.log` e `artifacts/video-finance/round2-summary.log`.
O build emitiu apenas os avisos existentes de bundle grande e API CJS do Vite.

Ensaios preliminares corrigiram a limpeza entre testes de interface, os tipos dos doubles e os
seletores da homologação; o leitor SQL da fixture passou a declarar UTF-8 e o teste de retorno usa
a atividade `rework` do BPM local. A leitura financeira mostra conflitos sem repetir consultas
automaticamente. Esses ajustes ocorreram antes das rodadas finais.

As capturas usam produtos explicitamente identificados como fixtures locais. Avisos globais sobre
Facebook e renovação de tokens vêm dos doubles das contas; não representam diagnóstico produtivo.
Não houve render, contato com provedor pago ou transação comercial. Não foram alterados prompts,
workers, imagens, publicadores ou configuração dos hosts.

## Reproduzir na sandbox

Requisitos: Java 21, Maven, dependências frontend (`npm ci`), Chromium/Playwright, Docker/Compose
e PyYAML no ambiente Python. A topologia usa o projeto exclusivo desta sessão e somente portas
locais da fixture; dados de terceiros, SMTP real e credenciais de providers não participam.

```bash
bash infra/testing/video-finance/run-round.sh round1
bash infra/testing/video-finance/run-round.sh round2
```

Cada rodada cria e remove MySQL, rede e volumes temporários, usa API/backend reais e simula
somente fontes adjacentes. O runner termina a topologia também quando falha. Os recibos, erros,
concorrência, avanço do briefing e invalidação por versão são conferidos contra MySQL 5.7.
O manifesto em `artifacts/video-finance/validated-files.json` preserva o SHA-256 dos arquivos
de implementação e testes, para conferir identidade entre as rodadas.

## Uso após integração e deploy

1. Abrir **Financeiro de vídeos** no menu ou **Financeiro dos vídeos** na etapa do ciclo.
2. Conferir produto, ciclo, experimento e versão; informar o teto em USD, responsável e justificativa.
3. Confirmar o escopo e registrar. O formulário não propõe nem confirma um valor automaticamente.
4. Usar **Continuar no ciclo com este teto**: o briefing recebe a referência oficial. Prosseguir
   com a avaliação de Plutus e com os gates do Estúdio pelos fluxos existentes.

Para Vega, a rota preparada é `/financial/videos?productId=4&chainId=14&cycleId=2`.
Nenhum teto de produção foi autorizado para o produto real nesta entrega.

## Oportunidade comercial

Prioridade alta, esforço baixo de roteiro: demonstrar o primeiro ajuste com itens que a cliente
já possui, preservando a hipótese do ciclo #2. Medir primeiros resultados por início, checkout e
vendas líquidas com contribuição. É hipótese de melhoria; produzir as peças ou registrar o teto
não comprova aumento de receita. A tela reduz a transcrição da referência financeira entre etapas,
sem retirar a revisão independente que controla qualidade e custo.
