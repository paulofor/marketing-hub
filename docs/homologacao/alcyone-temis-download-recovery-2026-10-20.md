# Recuperação de provas na revisão criativa — 20/10/2026

## Caso e evidência

Alcyone, produto 11, experimento 97, cadeia 26, processo 121 v10, execução 68:
Íris 770 produziu a peça 1060 e Psique 776 a aprovou. Têmis 778 foi bloqueada
em 20/10 às 05:09:44 UTC por timeout no GET da prova 765/1047. O banco não
registra resposta, modelo executado ou consumo dessa tentativa. Não há parecer
de Têmis a reaplicar. Ausência de custo informado não é custo faturado zero.

A mesma prova já participou da revisão anterior 766. Na conferência atual,
o arquivo 1047 respondeu HTTP 200 em 2,98 s (1.028.380 bytes) e a peça 1060
em 1,53 s (811.330 bytes). Os arquivos não estão ausentes. A retenção atual do
log de Têmis não contém a tarefa 778; o erro persistido é a fonte histórica.
Não há evidência suficiente para atribuir o atraso original ao storage ou ao host.

A lacuna confirmada no harness é a falta de recuperação de leitura transitória
antes da inferência: uma única falha encerrava o processo. Além disso, o executor
baixava novamente uma captura já recebida quando ela reaparecia como origem e
destino, apesar de já deduplicar seus anexos.

## Alternativas

| Opção | Benefício | Risco/esforço | Escolha |
| --- | --- | --- | --- |
| Retomar manualmente sem alterar o executor | Menor mudança imediata | Recorrência continua dependendo do operador | Descartada |
| Ampliar todos os timeouts | Tolera latência maior | Retarda callbacks e não recupera indisponibilidade transitória | Descartada |
| Recuperar somente GETs transitórios antes do modelo e reutilizar bytes da mesma leitura | Reduz interrupções sem inferência duplicada | Exige limite, cancelamento e validação de identidade | Adotada |

## Matriz local de aceite

| Fluxo | Verificação |
| --- | --- |
| Caso 778 e outra identidade | Timeout e 503 seguidos de sucesso; mesmos bytes e hashes entregues |
| Caminho antes válido | Leitura direta, fonte, recorte e destino preservados |
| Indisponibilidade persistente | No máximo três leituras; sem contexto parcial nem inferência |
| Autorização/contrato | 400/401/403/404/409/429/500 sem repetição automática |
| Identidade e cancelamento | Hash divergente, versão incompatível e interrupção continuam bloqueantes |
| Captura repetida | Mesma rota lida uma vez após sucesso; cada referência ainda confere o hash |
| Integração | Servidor HTTP local → anexos → modelo simulado, sem chamada externa ou custo |
| Observabilidade | Tarefa, rota, tentativa e exceção completa; arquivos efêmeros removidos |
| Interface publicada | Chromium desktop, iPhone e Pixel: estado de Têmis e próximo aceite |

Não há alteração de oferta, peça, orçamento, aprovação humana ou autorização
comercial. A melhoria vale para as revisões criativas de qualquer produto.
Sucesso técnico mede recuperação e número de leituras, não vendas ou conversão.
Reverter se houver repetição de inferência, aceite de identidade divergente ou
perda do cancelamento. Indisponibilidade persistente continua exigindo diagnóstico.

## Validação local concluída

- Antes da correção, três regressões falharam: recuperação do caso 778, outra
  execução 991778 e limite de tentativas. Os erros reproduziram o timeout persistido.
- Depois: 19 testes específicos aprovados. A integração com HTTP real local e
  processo Codex simulado comprovou duas leituras (503 → 200), uma auditoria e
  exatamente uma inferência. Falha persistente fez três leituras, zero auditorias
  de modelo e zero inferências.
- Suíte do executor: 135 casos, 134 executados aprovados, zero falhas/erros; uma
  fixture opcional de Vega sem `VEGA_GATE_FLOW_ARTIFACTS` não foi aplicada. Empacotamento
  do JAR, Spotless dos arquivos alterados e handshake MCP aprovados.
- Bundle de provas: 16 testes aprovados, 554 arquivos e 127 manifestos empacotados.
  Validador premium, `bash -n` e ShellCheck do script consultado aprovados.
- Cache restringe-se à rota na mesma revisão e reconfere o hash de cada referência.
  Anexos temporários são removidos e a auditoria distingue leitura de reutilização.

A revisão anterior válida de Psique permanece vinculada à peça 770/1060. A retomada
real depende da publicação verificada e usará os limites já autorizados: US$ 12
cumulativos na preparação de Alcyone, sem mídia nem vídeos pagos. O próximo aceite
humano continua independente. Publicação e resultado real serão registrados nas
evidências do PR, sem alterar pareceres históricos.
