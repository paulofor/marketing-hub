# Renovação automática Meta — 20/10/2026

## Evidência e causa

Capella #105, produto #7, ciclo #12, cadeia #26 permanece PLANNED. A conta #1
conserva renovação automática habilitada. A última renovação persistida é de
20/08/2026; a Meta confirmou vencimento em 19/10/2026 às 22:45:04 UTC e código 190.
O endpoint de elegíveis continua entregando a conta com os insumos necessários.

Logs do Facebook Ads Worker mostram a rotina a cada seis horas até 10/10 às
07:15 UTC. Depois, não há novas passagens. O diagnóstico de threads de 20/10
confirmou `scheduling-1` esperando indefinidamente em `Mono.block()` de
`MetaAudienceBackendClient.listPending`, ocupando o único agendador. O processo
continuou respondendo HTTP; isso não comprovava execução das rotinas.

Defeito adicional confirmado no contrato/teste: a renovação manual gravava 60 dias
mesmo recebendo `expires_in=3600`; a automática também inventava 60 dias quando o
prazo não vinha. Essa divergência foi corrigida sem atribuir a ela a parada do
agendador, cuja causa foi comprovada separadamente.

## Alternativas

| Caminho | Benefício | Risco/esforço | Decisão |
| --- | --- | --- | --- |
| Reiniciar o worker | Recuperação rápida | Repete a falha na próxima conexão sem resposta | Insuficiente |
| Aplicar limites HTTP mantendo uma fila | Corrige a espera infinita com baixo esforço | Trabalhos longos legítimos ainda atrasam renovação | Parte da solução |
| Limites HTTP e agendador exclusivo de renovação | Preserva a renovação mesmo com outra rotina ocupada | Dois agendadores, sem paralelizar campanhas | Escolhido |

Não ampliar o paralelismo das operações de campanha: elas compartilham contexto
de credencial. A renovação publica o novo token no backend; os consumidores o
carregam no próximo ciclo, sem trocar a credencial de uma operação em curso.

## Matriz local de aceite (definida antes dos testes)

| Cenário | Critério |
| --- | --- |
| Consulta de públicos sem resposta | Timeout encerra a espera; próxima consulta funciona |
| Resposta com cabeçalho mas corpo travado | Timeout também cobre a leitura do corpo |
| Agendador operacional ocupado | Renovação executa em sua própria fila; sem concorrência entre campanhas |
| Duas contas/identidades | Renovação e callback preservam IDs e credenciais de cada conta |
| Prazo positivo informado | Backend e worker usam exatamente `expires_in` |
| Prazo ausente, zero ou negativo | Não inventar validade nem substituir token anterior |
| Relógio do host fora de UTC | Elegibilidade e expiração continuam usando a validade oficial UTC |
| Recusa Meta / falha do callback | Falha observável, credencial anterior preservada, sem sucesso fictício |
| Recuperação | Próxima passagem volta a consultar; nenhum anúncio é criado pelo teste |
| Segurança | Logs não contêm tokens ou segredo, inclusive callbacks e exceções |

Mocks HTTP e credenciais sintéticas; sem mídia, tráfego comercial ou inferência.
Não há mudança de frontend. A conferência publicada usa Contas do Facebook e o
ciclo existente; testes técnicos não comprovam venda ou contribuição.

## Limite externo

A documentação oficial exige token ainda válido para a troca. Token expirado
exige nova autenticação do titular; App Secret não substitui consentimento.
A recuperação pontual não desativa a renovação automática nem amplia os tetos.
A credencial alternativa cadastrada contém o mesmo token vencido, confirmado
pelo `debug_token`. O token de ambiente consultado também estava vencido; a outra
credencial de pesquisa pertence a aplicativo diferente e não foi adotada para
anúncios. A troca pelo servidor não é garantia de renovação perpétua; o sistema
usa a validade efetivamente retornada, conforme o contrato oficial.

Fonte: [Meta — Long-lived access tokens](https://developers.facebook.com/docs/facebook-login/guides/access-tokens/get-long-lived/), consultada em 20/10/2026.

## Resultado

Validação local concluída: **173 testes do worker e 24 do backend**, sem falhas
ou testes ignorados. Antes da correção, seis dos oito casos do contrato de
revalidação do backend falharam, reproduzindo a validade inventada e a aceitação
de prazos inválidos; depois passaram. A consulta de públicos sem resposta, a
leitura de corpo travada, o isolamento real dos agendadores, duas contas distintas,
a falha do callback, a recuperação posterior e a ocultação de credenciais foram
exercitados com servidores HTTP locais. Nenhuma chamada Meta paga ou campanha
foi criada pelos testes. Diff e comentários de responsabilidade revisados.

A publicação e a conferência operacional serão vinculadas ao PR. As medidas de
sucesso são renovação antes do vencimento, retomada após indisponibilidade e
redução de bloqueios recorrentes; impacto em vendas ainda não medido. A adoção
exige confirmar o agendador exclusivo em execução e os callbacks persistidos;
uma regressão de transporte ou isolamento exige reversão pelo fluxo de PR.
O vencimento já confirmado exige recuperação da autenticação, separadamente da
correção que previne recorrência.
