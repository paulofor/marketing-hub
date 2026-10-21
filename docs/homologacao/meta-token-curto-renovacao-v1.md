# Renovação de token curto Meta — 21/10/2026

## Evidência e escopo

Capella #105, produto #7, ciclo #12. A credencial da conta #1 foi salva às
02:56:51 UTC, com vencimento oficial às 04:00 UTC. Às 04:12 UTC, a Meta já
retornava `valid=false`, código 190/subcódigo 463. O banco registrava renovação
habilitada, `NEVER_ATTEMPTED` e nenhuma tentativa para essa credencial.
O worker consulta elegíveis a cada seis horas: a credencial curta pode nascer e
expirar entre duas consultas, mesmo com o agendador exclusivo saudável.
Não atribuir esta recorrência à antiga espera infinita, já corrigida.

## Alternativas

| Alternativa | Benefício | Custo/risco | Decisão |
| --- | --- | --- | --- |
| Operador fazer a extensão toda vez | Nenhum ajuste | Repasse manual e recorrência | Rejeitada |
| Trocar o token ao salvar no backend | Extensão imediata | Move integração/execução para fora do worker canônico | Rejeitada |
| Consultar no worker a cada cinco minutos, espaçando trocas pelos dados persistidos | Detecta credencial nova curta; reaproveita fila e callbacks | Mais GETs leves; precisa limitar trocas | Escolhida |

O executor consulta em cinco minutos. Sem tentativa anterior, tenta a renovação.
Tentativas concluídas conservam o espaçamento de seis horas. Uma renovação bem
sucedida que ainda esteja a até trinta minutos do vencimento pode tentar de novo
após quinze minutos. Falha persistida não é repetida a cada consulta. A troca
manual de token já limpa status/tentativa; não há dependência de ID ou produto.
Não muda orçamento, campanha, autorização, validade oficial nem pareceres.

## Matriz local de aceite (antes de executar)

| Área | Cenário e aceite |
| --- | --- |
| Caminho feliz | Credencial recém-salva de uma hora é encontrada na próxima consulta e gera callback confirmado |
| Regressão original e outra identidade | Contas sintéticas distintas usam seus próprios tokens e IDs; nenhuma exceção por produto |
| Compatibilidade | Token longo já renovado mantém espera de seis horas; passado esse prazo, troca normal |
| Prazo curto retornado | Dentro de trinta minutos do vencimento e após quinze minutos, permite nova tentativa |
| Falha permanente | `FAILED` recente permanece visível e não chama Meta novamente; após seis horas pode tentar |
| Relógio | Horário UTC determinístico; tentativa futura não causa loop |
| Integração | Backend HTTP local, Meta simulada, callbacks reais; nenhum acesso externo ou mídia |
| Observabilidade/segurança | Tentativa e callback correlacionados; nenhum token/segredo no log |
| Isolamento | Agendador exclusivo continua funcionando com fila de campanhas ocupada |
| Recuperação | Falhas transitórias, validade inválida e callback recusado conservam testes existentes |
| Frontend/dispositivos | Nenhuma alteração de tela; leitura publicada de conta e ciclo em Chromium desktop |
| Métricas | Medir tentativa antes do vencimento e bloqueios recorrentes; testes não comprovam vendas |

## Limite externo e adoção

A prevenção não recupera a credencial que a Meta já rejeitou como expirada.
É necessário salvar uma nova credencial pela tela para observar a troca oficial.
Fonte oficial: https://developers.facebook.com/docs/facebook-login/guides/access-tokens/get-long-lived/
A consulta web desta sessão recebeu HTTP 429; o diagnóstico de validade é a
resposta direta da Meta pelo endpoint oficial de diagnóstico do backend.

Adotar após testes, PR revisado, merge, deploy e confirmação da rotina publicada.
Reverter pelo fluxo de PR caso haja aumento de trocas sem respeitar o espaçamento;
nunca desabilitar a renovação ou fabricar validade para destravar campanha.

## Resultado local

- Suíte completa do worker: **192 testes, zero falhas, erros ou ignorados**.
- Regressão do agendamento executada com o código anterior: falhou ao encontrar
  intervalo de seis horas, incompatível com o token de uma hora.
- Restaurada a correção: os dois testes de agendamento/isolamento passaram.
- As novas regressões de transporte usam contas #1/#87, além de #23/#97 e
  contratos antigos #46/#112, todos sintéticos. Nenhuma inferência, compra ou
  chamada real à Meta foi usada como mecanismo de teste.
- Diff e comentários de classe/método revisados. Sem alteração de banco,
  frontend, orçamento, janela, mídia ou estado comercial de Capella.

A confirmação publicada e os links de entrega serão registrados no PR.
