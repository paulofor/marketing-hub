# Matriz de homologação ChatGPT Ads v1

Critérios definidos antes da execução. Somente identidades e respostas sintéticas na sandbox.

| Área             | Aceite local                                                                           |
| ---------------- | -------------------------------------------------------------------------------------- |
| Caminho completo | Preparar, validar, autorizar, criar pausado, revisar, ativar, medir e pausar           |
| Autorização      | Sem orçamento herdado; moeda, janela, lance e material fixados                         |
| Homologação      | Criativo independente e prova de compra/entrega exigidos; alteração bloqueia           |
| Integração       | Contratos oficiais, micros, URLs, IDs e status verificados com servidor simulado       |
| Recuperação      | Resposta preservada, callback repetido e lease expirado sem criação duplicada          |
| Falhas           | Credencial ausente/inválida, revisão recusada, timeout, resposta incompleta            |
| Métricas         | Desconhecido separado de zero; moeda e período preservados                             |
| Conversão        | Insights não criam compras; relatórios canônicos preservados; simulação não vira venda |
| Interface        | Desktop, iPhone e Pixel em Chromium; sem segredo no navegador                          |
| Persistência     | MySQL 5.7, migração reaplicável e regressão em outro produto                           |
| Publicação       | PR/checks/main/deploy e leitura publicada do contrato                                  |

Limite: sem conta/credenciais de anunciante e autorização específica, nenhuma campanha real será
ativada. A homologação simulada não comprova habilitação da conta nem resultado comercial.

## Resultado local — 28/10/2026

- Backend: 202 testes sem falha nas suítes do canal (21), arquitetura (92), serviço de
  experimentos (54), controller de runs (27) e compatibilidade Quartzo (8).
- Hermes: 42 testes sem falha, incluindo HTTP local, assinaturas de ida e volta, isolamento
  de conta, resposta adulterada, replay de callback e credencial ausente.
- MySQL 5.7: migração aplicada e reaplicada sem duplicação; 13 cenários do serviço com SQL real.
  Topologia exclusiva removida com volumes após a execução.
- Frontend: 39 testes sem falha (canal, criação e detalhes de experimentos), incluindo retorno ao contexto correto.
- Interface: desktop, iPhone 15 Pro e Pixel 7 em Chromium, usando APIs sintéticas e capturas
  segregadas em `artifacts/chatgpt-ads-3783/browser`. As ações sem aceite permanecem bloqueadas.
- Provisionamento: `bash -n`, ShellCheck e fixture SSH local aprovados; segredo segue por stdin,
  sem aparecer em argumentos/logs, e configuração ausente preserva a existente.
- Tipos TypeScript conferidos. Bundle local concluído com a configuração de homologação existente, sem sourcemaps, e tipos verificados separadamente. O build completo com sourcemaps excedeu a memória da sandbox, inclusive na engine local; o CI verificará essa etapa de empacotamento.

Nenhuma conta de anunciante foi conectada ou campanha real usada como teste. O segredo interno
foi cadastrado de forma protegida para os workflows; a chave Advertiser API permanece ausente.
O aceite comercial exige conta habilitada, autorização própria e homologação com fontes reais.

## Aprendizado reutilizável

Antes: o canal e seus estados não existiam; Hermes só tinha contratos de Meta. Depois: preparação,
publicação pausada, revisão externa, ativação e exposição têm estados e evidências distintos.
A recuperação preserva operação/recibo, e uma ativação incerta solicita pausa prioritária.
Regressões usam outro produto, outro ID de conta, cenário antes válido e falhas deliberadas.

Métricas futuras: publicação duplicada deve permanecer zero; medir tempo até primeira exposição,
custo por compra conciliada e contribuição na moeda correta. Os testes comprovam controles, não
melhoria de vendas. Reverter a adoção do canal se conta, orçamento, material ou prova divergirem.

Regressão de contrato: o cadastro pode devolver ID como texto ou número. A navegação normaliza o identificador, preserva o experimento selecionado e exclui outros contextos. A suíte do canal integra o workflow de frontend.

O CI completo encontrou uma asserção histórica que limitava o catálogo a dois canais, apesar
da coluna já ser extensível. A falha foi reproduzida localmente; a regressão agora preserva os
canais existentes e ChatGPT Ads, verificando também que todos cabem no VARCHAR(40). O banco
e os canais históricos permanecem protegidos sem impedir extensões compatíveis. Os dois jobs
que falharam apontaram exclusivamente essa asserção; o ajuste foi validado antes do novo push.
