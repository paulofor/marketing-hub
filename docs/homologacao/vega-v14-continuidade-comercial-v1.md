# Vega v14 — candidata de continuidade comercial

## Escopo e alternativas

A autorização #3709 amplia a preparação cumulativa para US$ 20. A retomada real
expôs que o contrato global ainda era v7, a superfície comercial v8 servia v12 e
a v14 aceita existia somente no caminho privado. A fila foi protegida no PR
5661; essa proteção não implementava a entrega comercial.

A oferta proposta por Atena 714 é preservada: primeiro ajuste e Dia 1 gratuitos;
Dias 2 a 7, três materiais e 90 dias de acesso por R$ 67 em pagamento único.
A implementação privada e seus pareceres permanecem históricos. A candidata
comercial tem suas próprias verificações e continua exigindo revisão econômica e aceites.

| Caminho | Benefício | Risco / esforço | Escolha |
| --- | --- | --- | --- |
| Substituir a v12 na raiz do domínio | Menor configuração | Muda a experiência histórica e dificulta recuperação | Recusado |
| Reutilizar a raiz ou uma rota no container v8 | Menor configuração | Contraria o isolamento obrigatório do cânone | Recusado |
| Domínio novo v14, imagem e container próprios | Isolamento completo | Exige DNS e SAN TLS ainda ausentes | Não escolhido |
| Endereço produtivo v9 já reservado, imagem/container/porta próprios | Reutiliza DNS e TLS conferidos e preserva a v8/v12 | Exige novo target e troca somente do upstream reservado | Escolhido |

A superfície produtiva v9 serve a experiência v14, assim como v8 preserva a v12. A candidata não concede ativação comercial, compra ou mídia. Não
reutiliza o parecer privado como aprovação comercial. Não produz vídeo pago.

## Matriz definida antes dos testes

| Percurso | Aceite |
| --- | --- |
| Entrada v14 | Ocasião e combinação obrigatórias; nota opcional; sem e-mail antes do primeiro resultado |
| Validações | Ausências, excesso, campos estranhos e limites 160/300/300 rejeitados sem fila externa |
| Primeiro resultado | Ação aplicável, contexto preservado, regras locais identificadas e nenhuma inferência externa |
| Continuidade | Dia 1 gratuito; Dias 2 a 7 e três materiais protegidos por acesso pago |
| Compra | Somente pagamento simulado local; versão e transação vinculadas; nenhum token QA em produção |
| Acesso | Prazo de 90 dias, recuperação do mesmo progresso, acesso cruzado e expiração bloqueados |
| Observabilidade | Versão e entradas na auditoria; custo incremental de provider zero separado de infraestrutura |
| Segregação | Dados sintéticos identificados; não contam como vendas ou margem realizada |
| Regressão | V12 na raiz preservada; v14 privada e controles terminais preservados |
| Dispositivos | Chromium desktop, iPhone 15 Pro e Pixel 7 emulados, sem overflow e com ações alcançáveis |
| Preparação | Contrato exato elegível na fila; aprovação econômica e comercial continuam independentes |

Custos comerciais serão projeções com fontes e limites; não se exige extrato de
API para Codex. A meta de contribuição da v14 continua pendente de decisão do
usuário. Nenhum percentual histórico ou de outro produto é escolhido por esta
implementação. Autorização de mídia permanece R$ 100 por cinco dias após gates.


## Publicação e limites da prova

A publicação do software usa o target v9 e a porta 5185, com upstream próprio no
proxy proprietário. O endereço v9 já resolve para 163.245.200.7 e possui TLS válido.
A raiz v8/v12 e o caminho privado v14 permanecem preservados. Links de recuperação
v14 apontam à superfície v9; acessos legados conservam a versão concedida.

O smoke de instalação é somente de leitura: comprova domínio, versão, commit,
fingerprint e HTML. O contrato candidato é cadastrado pelo painel após a publicação;
a verificação funcional publicada e os pareceres comerciais são passagens distintas.
O checkout v14 só abre com experimento RUNNING e vínculo idêntico de versão,
experimento, preço e URL. Nenhuma prova local concilia venda real ou aprova margem.

O teste SMTP estrito usa `PDE_TEST_EXPECT_MAIL_SENT=true` no ambiente com Mailpit;
a integração geral também executa sem depender de um servidor externo de e-mail.
Na sandbox, uma sobreposição de sub-redes Docker foi corrigida na topologia temporária;
o SMTP atravessa um relay local para sandbox-mail:1025, sem credencial real.


## Resultado local observado

- 221 testes Java PDE passaram, sem falhas ou testes ignorados.
- 12 cenários novos passaram: entrada/valor, falha e recuperação, compra/entrega/reembolso e trava do checkout, nos três dispositivos.
- As versões v5, v6, v7 e v12 passaram na regressão de contratos e navegação, nos três dispositivos.
- A prova de instalação em leitura passou nos três dispositivos; o proxy TLS local real distinguiu v8/v12 de v9/v14.
- 56 testes do harness Node e 25 do contrato Python passaram; sintaxe e ShellCheck dos scripts envolvidos passaram.
- Após reiniciar somente o backend, MySQL 5.7 restituiu versão v14, progresso 1/7 e orientação concluída. O banco temporário permaneceu ativo; não se simulou recuperação de host ou backup.
- O SMTP de sandbox entregou os links de teste. Transações, e-mails e sessões sintéticos permanecem segregados das métricas humanas.

A primeira rodada encontrou perda de entrada na transição rápida e omissão de duas
ações no frontend. A correção foi implementada, e os cenários afetados passaram.
Na instalação local, a colisão entre sub-redes foi corrigida na topologia temporária;
não se modificou rede ou credencial de produção.

As evidências estruturadas ficam em
`backend/ads-service/src/main/resources/contracts/operational-evidence/vega-commercial-continuity-v1.json`.
Essas provas demonstram funcionamento técnico e recuperação; não comprovam demanda,
receita, margem realizada nem aceitação dos especialistas da candidata comercial.


O CI do primeiro HEAD apontou que a fixture histórica do monitor ainda assumia
quatro superfícies e não fornecia o manifesto da nova v9. A falha foi reproduzida
localmente. O teste corrente passa a conferir todas as superfícies do inventário;
a fixture histórica permanece delimitada à sua publicação e aos legados que contém.
Nenhum `legacyMinimumRevision` foi inventado para contornar o controle. Os 35 testes
de freshness passaram localmente após esse ajuste.


A integração de CI também expôs diferença entre a topologia local usada na primeira
rodada e a configuração versionada: esta não habilitava o retorno de links mágicos
sintéticos, então os testes recebiam `accessUrl=null`, conforme a segurança padrão.
A opção foi habilitada exclusivamente em `docker-compose.local-validation.yml` e
na configuração Playwright local; a produção mantém o padrão falso. Um contrato
preventivo impede exposição dessa opção na topologia produtiva. A correção não
altera autenticação, concessão paga ou envio de e-mail de clientes reais.

Os seis casos afetados foram executados novamente com a topologia versionada
`docker-compose.yml` + `docker-compose.local-validation.yml`, passando nos três
dispositivos. A única sobreposição usada na sandbox abriu a porta do test double
para o Playwright externo à engine; as opções funcionais vieram do arquivo versionado.
