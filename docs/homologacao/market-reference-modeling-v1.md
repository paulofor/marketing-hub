# Homologação — modelagem por referências v1

Matriz definida antes da rodada integrada. Dados sintéticos e dependências externas simuladas; nenhuma compra de teste será misturada a evidência comercial.

| Dimensão | Critério de aceite |
|---|---|
| Entrada | Produto existente e novo; 1–5 URLs HTTPS públicas; resultado desejado obrigatório; idempotência; salvar não abre tarefa nem fila |
| Pesquisa | Início explícito abre auditoria e fila uma única vez; STOP recusa; falha preserva pesquisa; Argos registra fontes e contrapontos |
| Passagem | Novo produto preserva materialização canônica; existente não duplica produto. Pesquisa concluída pode ser adotada somente por ciclo do próprio produto, ainda não executado |
| Isolamento | Outro produto, referência antiga, ciclo encerrado, tarefa anterior e tentativa de trocar adoção são recusados ou preservados; não herda orçamento |
| Agentes | Atena compara três alternativas; trace cita fontes persistidas, exige fato, limitação, aprendizado, decisão, entrega e métrica. Contratos históricos continuam válidos |
| Continuação | Contexto é entregue pelas referências oficiais do experimento e construção privada; serviços da cadeia conservam orçamento, gates e passagem entre agentes |
| Persistência | MySQL 5.7 aplica migração e reaplica sem duplicar; dados continuam após reinício; adoção guarda snapshot |
| Interface | Desktop Chromium, iPhone e Pixel emulados: preparar, erros, acompanhar e vínculo; sem rolagem horizontal; UUID funciona em HTTP |
| Observabilidade | Status vem do backend; pesquisa concluída não aparece como produto entregue ou venda; tarefas e fontes navegáveis |
| Métricas | Compra, entrega, reembolso e contribuição continuam nas métricas da cadeia. Hipóteses e projeções explicitamente distintas de resultados |

Não exigir entrevistas, recrutamento ou opiniões solicitadas. Piloto Vega com Whering/Indyx preparado pela UI; execução paga exige autorização aplicável e não é inferida da implementação.

## Ampliação internacional — critérios definidos antes da nova rodada

- Nova preparação deve persistir escopo internacional e destino Brasil; rascunhos anteriores conservam o escopo genérico.
- Aprovação de Atena exige cada referência preparada, sinal classificado e atribuído, trecho literal da evidência persistida e adaptação brasileira completa.
- Recusar trechos inventados, referência de outro produto, repetição e omissão de primeiro valor ou continuidade paga. Ausência de dados permanece UNKNOWN; não exigir comprovação financeira do concorrente para pesquisar.
- Preservar contrato histórico sem modelagem, ADJUST fundamentado, idempotência e STOP.
- API e tela reais com MySQL 5.7: duas identidades, preparação sem gasto, início concorrente, adoção, snapshot e reinício; Chromium desktop/iPhone/Pixel.
- Fontes públicas do piloto: Whering e Indyx; números divulgados pelo vendedor não são demanda brasileira. O piloto produtivo é um rascunho criado pela tela, sem chamadas pagas ou campanhas.

## Resultado local anterior — 25/10/2026

- Backend: 311 testes aprovados, incluindo arquitetura, contexto dos agentes, descoberta, Big Idea, continuidade do ciclo e novas regressões de modelagem.
- Atena: suíte completa, 71 testes aprovados. Argos: 162 testes aprovados. Frontend: 13 testes da tela/menu, tipagem e build aprovados.
- API real com MySQL 5.7: preparo sem tarefa, replay, início concorrente sem duplicação, vinculação ao ciclo, rejeição de outro produto, fontes congeladas e recuperação após reinício aprovados.
- Tela real em Chromium desktop, iPhone 15 Pro e Pixel 7 emulados: campos, erro de URL, gravação, estado do rascunho e ausência de rolagem horizontal aprovados. A regressão remove `Crypto.prototype.randomUUID` para reproduzir a capacidade do HTTP administrativo.
- Depois do ajuste final da passagem de decisão, foram repetidos os testes afetados e a integração de persistência. A rodada visual já aprovada foi preservada.
- A primeira rodada visual detectou a rota no bloco público errado de App; foi corrigida na sandbox antes da publicação. A primeira tentativa de build atingiu o limite local de heap; o mesmo build passou com 4 GiB definidos somente no processo.

Topologia temporária removida com volumes. Fontes externas, pagamentos e chamadas de modelos foram simulados; custo externo e eventos comerciais de teste: zero. Logs e capturas locais ficam em `artifacts/product-modeling/local` (não são provas de demanda).

## Compatibilidade das provas anteriores

A única atestação vigente afetada foi Mira privada v45, que fixa o hash do consumidor compartilhado de estratégia. A sucessora v46 preserva experiência, publisher e provas históricas, registrando somente compatibilidade com a validação opcional de modelagem. Os 71 testes de Atena incluem contratos e recuperações anteriores; contextos sem modelagem permanecem aceitos. Isso não renova homologação comercial de Mira nem autoriza publicação de outra superfície.

## Decisão de implementação

Comparadas três alternativas: relatório separado (menor esforço, perde passagem), cadeia nova (duplica agentes e governança) e entrada integrada (reutiliza descoberta, planejamento e gates). Escolhida a terceira. Para produto existente, a adoção explícita no ciclo ainda não executado evita substituir seu histórico. Para novo produto, conserva a materialização existente. A vinculação não exige copiar saídas entre agentes.


## Fontes públicas do piloto — consulta em 25/10/2026

- [Whering](https://www.whering.co/): a própria empresa anuncia mais de 10 milhões de usuários e recursos para catalogar roupas, planejar combinações e compartilhar guarda-roupas. Classificação: alegação do vendedor sobre adoção, sem receita ou margem verificadas nesta consulta.
- [Indyx](https://www.myindyx.com/home/): entrada gratuita para organizar o guarda-roupa, recursos premium e serviço de styling. Classificação: oferta observável com passagem gratuita/paga; a existência da oferta não comprova compras, lucro ou adequação brasileira.

Hipótese para Vega: combinações com a própria identidade e poucas peças antes de um cadastro
extenso; uma jornada personalizada de sete dias pode justificar a continuidade paga. Verificar
alternativas locais, português, rotina/clima, acesso móvel, preço e custos em reais. O piloto
não importa marca, imagens, código ou depoimentos dos concorrentes. Sua Big Idea v2 permanece.


## Resultado da ampliação internacional — 25/10/2026

- Backend: 287 testes unitários/arquitetura aprovados; 9 testes de modelagem repetidos após ampliar a prova de passagem da adaptação brasileira. Atena: 72 testes; Argos: 162; frontend: 14 testes, tipagem e build aprovados.
- MySQL 5.7 real: as duas migrações aplicadas, preparo e replay, início concorrente sem duplicar, adoção de duas identidades, contexto internacional e recuperação após reinício aprovados. Topologia temporária removida com volumes.
- Interface real: desktop Chromium, iPhone 15 Pro e Pixel 7 emulados, com URLs inválidas, fallback UUID em HTTP, rascunho sem tarefas e ausência de rolagem horizontal aprovados. Capturas conferidas em `artifacts/product-modeling/local`.
- Contrato internacional: recusa omissões, trecho fabricado, identidade de referência alheia e adaptação incompleta; preserva UNKNOWN, ADJUST, contrato anterior e decisão vigente. A adaptação aprovada acompanha as etapas seguintes e desaparece se a decisão vigente estiver bloqueada.
- Atestação v47 registra somente compatibilidade das fontes compartilhadas de Atena após essa validação. v46 e anteriores permanecem imutáveis; experiência e pareceres comerciais de Mira não foram renovados.
- Custo externo da homologação: zero. Testes com respostas simuladas comprovam contratos e passagem; a qualidade de uma inferência real e o impacto sobre vendas exigem avaliação posterior autorizada.

A falha anterior do workflow Liquibase do PR #5700 foi HTTP 429 do Maven Central ao baixar
o POM pai. Não foi falha de SQL. A nova revisão mantém os scripts existentes e reapresenta
o PR após a homologação local completa; nenhum deploy é usado como teste.
