# Mira — autorização audiovisual na moeda original

## Aceite e contexto

Solicitação #3332, resposta de 10/10/2026: Mira, produto 10, ciclo 9, experimento 102,
versão `mira-private-candidate-v3`. Teto audiovisual de **US$ 10 no conjunto** para anúncio,
demonstração e revisões; mídia de **R$ 100 no total por cinco dias após homologação**.
Preparação anterior conserva seu teto cumulativo separado. Nenhum teste local consome
provider, IA, pagamento ou mídia reais; fixtures e eventos sintéticos ficam segregados.

Lacuna observada pela tela, código e contrato: Financeiro dos vídeos registra USD;
`CreateRequest` do Estúdio aceita `authorizedBudgetCurrency=USD`, mas a tela exige BRL
e cotação. Converter manualmente permitiria prosseguir, mas distorceria a moeda original;
usar a API isoladamente manteria a lacuna para o operador. Foi escolhida a menor correção:
usar pela tela o contrato já existente de USD e preservar o caminho válido de BRL.

## Matriz definida antes da execução local

| Percurso | Verificação | Estado inicial |
| --- | --- | --- |
| Autorização USD de Mira | Preflight e produção enviam USD exatos, sem cotação fictícia | PASS |
| Outro produto/execução | Mesmo comportamento com identificadores distintos | PASS |
| Caminho antes válido BRL | Conversão conservadora exige fonte/data e nunca aumenta o teto | PASS |
| Troca de moeda | USD não envia cotação residual; BRL exige os próprios campos | PASS |
| Validação | Zero, negativos, valores não finitos e USD com mais de dois decimais bloqueiam envio | PASS |
| Falha de integração | Erro aparece; botão fica bloqueado durante a requisição | PASS |
| Observabilidade | Recibo USD mostra moeda e valor originais, sem câmbio indefinido | PASS |
| Desktop/mobile | Chromium desktop, iPhone 15 Pro e Pixel 7: controles legíveis e operáveis | PASS |
| Segregação/limites | Test doubles, sem chamadas pagas e sem campanha nos testes; teto não se repete por vídeo | PASS |

Testes locais comprovam o comportamento; não comprovam demanda, vendas ou margem.
O registro oficial da autorização e a retomada produtiva são conferidos pela UI, backend e MCP.

## Resultado local e melhoria reutilizável

`AudioVideoStudioPage.test.tsx` e `videoCycleBudget.test.ts`: **47 testes passaram**.
TypeScript e build produtivo passaram. `scripts/test-video-budget-currency-browser.mjs`
executou as três combinações de dispositivo com projetos/produtos distintos, interceptando
todo acesso de API e bloqueando rede externa; os payloads USD e capturas ficaram em
`artifacts/mira-cycle9-authorization-3332/currency-browser`.

Antes: autorização em dólares exigia inventar/converter uma autorização em reais para usar a
tela. Depois: USD passa pelos mesmos endpoints e gates existentes, sem cotação residual.
A melhoria é reutilizável por qualquer produto e reduz um repasse manual observado; não altera
providers, prompts, estratégia, orçamento nem políticas de aprovação. Critério de adoção: caminho
USD funcional e BRL preservado. Reverter se houver alteração de moeda/valor no contrato, falha
de isolamento ou regressão dos gates. Ganho comercial e tempo real até venda ainda não medidos.
