# Jaspe v1 — matriz de aceite e evidências

Solicitação: pesquisa de pelo menos dez ideias, redução a três e escolha final, com novo tipo
no Marketing Hub. Escopo implementado: catálogo extensível, base Java reutilizável de concepção
e preflight de arquivos de impressão. Não é uma fábrica produtiva nem uma oferta publicada.

## Matriz definida antes da implementação

| Percurso | Aceite | Validação |
|---|---|---|
| Pesquisa | 12 ideias, fornecedores com fontes, notas justificadas, três finalistas e escolha | Conferência das fontes e cálculo reproduzível |
| Catálogo | JSON compatível com contrato vigente; novo mineral; proposta sem produto/ciclo/campanha | Teste de contrato e formulário existente |
| Concepção | Prompt/schema versionados; não aceita preço, aprovação ou pedido inventado | Validador real do SDK com saídas locais |
| Arquivo | PNG/JPEG real, hash, escopo, dimensões, resolução e perfil do fornecedor | Imagens sintéticas locais; nenhum modelo pago |
| Falhas | Arquivo corrompido, hash trocado, escopo diferente, resolução insuficiente e especificação inválida bloqueiam | Testes determinísticos |
| Reutilização | Segundo cliente/produto e perfil anterior de consultor continuam válidos | Suíte completa do SDK |
| Observabilidade | Resultado inclui escopo, hash, versão, motivos e dimensões; autorização produtiva sempre falsa | Testes de contrato; backend do futuro produto persistirá o resultado |
| Integrações | Fontes oficiais distinguem API pública, acesso comercial e pedido manual | Pesquisa; sem credencial de fabricante, sem pedido externo |
| Métricas | Testes não entram em compras/receita; custos desconhecidos não viram zero | Fixtures sintéticas e revisão do contrato |
| Interface | Cadastro e pesquisa funcionam no Chromium desktop, iPhone 15 Pro e Pixel 7 emulados | Formulário existente com API isolada local |
| Publicação | Revisão, checks, integração e saúde conforme escopo | Bloqueada enquanto faltar comprovação da recuperação de credenciais |

Safari/WebKit e aparelho físico não são comprovados por emulação Chromium. Não há
checkout ou experiência de compra nova nesta entrega; sua homologação pertence à
construção de um produto específico, após escolher fornecedor, SKUs e condições reais.

## Estado inicial observado em 21/11/2026

- Frontend e MCP confirmaram sete tipos; nenhum cobre fabricação física.
- Cadeia 26 está aposentada; v27 é a referência vigente. Nenhuma execução foi migrada.
- Os agentes existentes foram consultados, sem acionar tarefas pagas ou editar seus pareceres.
- `constructionReady` no catálogo significa campos da base completos; não comprova integração,
  qualidade de impressão, vendas ou capacidade de entrega física.
- O SDK já valida schemas e segregação do consultor, mas não confere bytes de um arquivo
  contra tamanho físico e resolução do fabricante. Essa é a lacuna concreta abordada.
- O registro do incidente em `docs/homologacao/financial-cumulative-ceilings-v1.md` e o PR
  #5854 ainda apontam recuperação pendente. O merge anterior do #5814 não comprova revogação.

## Resultado local e cadastro administrativo

- Pesquisa: 12 alternativas com fornecedores/fontes, notas ponderadas e três finalistas; escolha
  de coleção visual local para hospedagens. Cenários financeiros explicitamente fictícios.
- SDK: primeira rodada completa com **88 testes**, zero falhas/erros/skips. Após adicionar bloqueio
  de brief sem catálogo, repetidos somente os **30 testes de fabricação** (21 preflight + 9
  concepção); todos passaram. O conjunto resultante contém 89 casos, incluindo 59 do harness anterior.
- Contrato do cadastro: três testes Node aprovados (limites do DTO, base e aritmética da pesquisa).
- UI local: cadastro, erro simulado 409, preservação do formulário, nova tentativa e busca por mineral
  aprovados em Chromium desktop, iPhone 15 Pro e Pixel 7 emulados. Uma inclusão no double por perfil;
  nenhuma chamada produtiva. O interceptador local inicialmente capturava `/src/api/` do Vite;
  foi restringido a `/api/`, sem alterar a aplicação. Não havia defeito de cadastro no produto.
- Spotless e diff conferidos. Nenhum teste de arquitetura foi alterado. Não houve migração Liquibase,
  nova tabela, novo endpoint ou alteração da aplicação administrativa.
- Pelo formulário produtivo existente foi criado **tipo #8**, código `AI_DESIGNED_MANUFACTURED_PRODUCT`,
  mineral Jaspe, estado `PROPOSED`, zero produtos vinculados. O campo `constructionReady=true`
  confirma preenchimento da base, **não** disponibilidade do SDK no runtime nem fabricação homologada.
- O cadastro não trocou imagem, configuração de serviço ou container; não é deploy. Não criou produto,
  cadeia, ciclo, pedido físico, campanha, cobrança ou inferência paga. Código/SDK seguem sujeitos ao PR.
- Evidências locais em `artifacts/jaspe-v1`, declaradas no manifesto de entrega. Fontes e relatório
  permanecem versionáveis; anexos privados, dependências e credenciais não entram no PR.

## Limites e retomada

Não há credencial nem contrato de fornecedor de impressão informado, e esta solicitação não
contratou amostra/produção. APIs foram pesquisadas em documentação oficial; chamadas autenticadas
sandbox/live e provas físicas não foram executadas. Esta entrega implementa **o tipo e sua base de
construção**, não a loja/fábrica completa de um produto. Cotação, pedido, pagamento, callback,
reimpressão e experiência do comprador exigem execução específica antes de oferta comercial.

Merge/deploy da base continuam bloqueados pelo incidente documentado, até evidência de revogação
nos provedores e substituição no GitHub. Pergunta enviada nesta conversa, sem pedir chaves.
PR/revisão/checks são preparados independentemente. Não tratar PR aberto ou cadastro como
publicação do SDK. Não há agente executando continuação automática deste novo tipo.
