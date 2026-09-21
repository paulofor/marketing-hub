# Entrega e atendimento de Capella — homologação

Escopo: cadastrar a decisão comercial de 21/09/2026, transportar o contrato do
produto ao GeraSalesPage e manter as condições acessíveis no pós-compra e no kit.
Não altera preço, mídia nem quantidade. O processo Quartzo só conclui com os gates.

## Matriz definida antes dos testes

| Dimensão | Critério |
| --- | --- |
| Contexto | Produto, versão e condições seguem no `pending` e no JSON entregue ao worker |
| Segregação | Dois produtos sintéticos com prazos diferentes não compartilham condições |
| Ausência | Produto/campos ausentes não geram promessa, contato ou prazo padrão inventado |
| Regressões | Pacote FEO, gate comercial e bloqueio de qualidade continuam válidos |
| Pós-compra | Prazo, canal e direitos aparecem em desktop, iPhone e Pixel mesmo se o pagamento não puder ser confirmado |
| Entrega | ZIP gerado localmente inclui instruções de uso, formatos, suporte e reembolso |
| Falhas | Pagamento inválido, pacote inválido e falha de envio preservam os controles existentes |
| Cadastro | Formulário envia somente alterações autorizadas, com confirmação no backend |
| Conteúdo legado | Editar promessa preserva teto e metas incompletas; mudar verba, período ou metas inválidas continua bloqueado |
| Inicialização | YAML real inicia o serviço de pagamentos com H2 e integrações externas desabilitadas |
| Publicação | Artefato do repositório, SHA servido e workflows conferidos antes da retomada |
| Processo | Psique, Têmis e consolidação comprovam seus objetivos; não simular aceite |
| Isolamento | Sem pagamento real, modelo pago ou SMTP produtivo em testes; não contam como vendas |

Comparação: antes, o `pending` não carregava o produto nem seus contratos de
entrega; os textos históricos do experimento eram as únicas fontes. Depois, os
contratos estruturados acompanham a geração em todas as etapas, sem copiar dados
de outro produto. A mudança é no contexto do agente; não muda modelo nem relaxa gates.

## Resultados locais — 21/09/2026

- Reproduzido o contexto ausente: as duas variantes de contrato falharam com o
  comportamento anterior e passaram com o produto estruturado no `pending`.
- Backend: suíte completa com 3.345 casos, zero falhas/erros, 20 condicionais
  ignorados. Após a correção adicional da edição, a suíte `ExperimentServiceTest`
  foi reexecutada com cinco regressões novas; total dos relatórios 3.350 casos,
  sem falhas/erros. Não foi necessário repetir as suítes não afetadas.
- Frontend: 733 testes aprovados na primeira rodada; após cobrir a edição real do
  formulário, 18 testes pertinentes e a suíte final de 736 testes aprovados.
  Typecheck e build executados. Metas e orçamento legados não se tornam autorização.
- Pagamentos: 46 testes Java aprovados, incluindo ZIP real e inicialização Spring
  com o YAML produtivo; sete testes JavaScript de estados, falhas e envio concorrente.
- O JAR local iniciou saudável. HTML/JS servidos coincidem byte a byte com os
  recursos do artefato; SHA-256 do HTML:
  `6cbecff8ef99ab5a81cb8195db0d91d47f905782b1f4ec3f9e650185d4e8741b`;
  JS: `483eaf2c05f4dfd89849f483372afe237b072ea9510838c8768f1a47af8ecf06`.
- Playwright: cadastro do produto, edição da promessa e quatro situações de
  pós-compra em desktop, iPhone 15 Pro e Pixel 7, usando o recurso servido pelo
  JAR e respostas locais de pagamento. Sem overflow horizontal no pós-compra.
- Contratos de proxy e recuperação passaram, inclusive Docker Compose isolado.
  `bash -n` e ShellCheck passaram nos scripts consultados; SC2016 foi suprimido
  apenas nas verificações que procuram literalmente variáveis em outros scripts.
  Não houve alteração desses scripts. `diff --check` e verificação dos recursos
  externos do JAR backend passaram.

Defeitos adicionais confirmados: formulário exigia novo teto/meta para qualquer
edição, mesmo com campanha parada; YAML de pagamentos tinha indentação inválida
em `image-model` e falhava antes de iniciar. As regressões cobrem ambos. O
pós-compra antes ignorava `ENTREGUE` e anunciava pagamento antes de confirmá-lo.

Alternativas: atribuir verba para destravar cria autorização indevida; alterar só
o banco deixa o defeito recorrente; preservar os valores e validar apenas a área
alterada permite corrigir o conteúdo sem relaxar o gate de publicação. Escolhida
a terceira, comprovada no formulário e no serviço com persistência em memória.

Publicação e aceite de Psique/Têmis permanecem critérios separados a confirmar
após a entrega. Nenhum teste conta como venda. Falha de atendimento humano/SLA
não é comprovada ou descartada por teste de software; custo e latência de uma
homologação comercial concluída ainda não estão disponíveis nesta rodada local.

## Falha real de publicação e correção local

O PR #5288 foi integrado em `9ef3b432964fbf5f561d58b765868d83779f67f7`.
O deploy de pagamentos `35629994566` falhou na autenticação, antes de substituir
o serviço. A causa e o teste preventivo estão em
`LOOP-PAGAMENTOS-CREDENCIAL-PRIORITARIA-RECUSADA`.
Foi validado localmente o helper compartilhado com seleção por autenticação,
incluindo credencial prioritária recusada, quinta candidata válida, configuração
ausente, chave inválida, limpeza e manutenção dos nove publicadores anteriores.
OpenSSH isolado comprovou SSH/SCP/rsync e recusa de identidade divergente;
`bash -n`, ShellCheck e parser YAML aprovados. A topologia temporária foi removida.
Não houve publicação manual por SSH. O resultado remoto ainda depende do deploy.
