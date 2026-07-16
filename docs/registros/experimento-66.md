# Experimento 66 - MUSA-H001-E004

## Diretriz operacional

- Objetivo comercial: transformar o experimento 66 em uma oferta low-ticket validável, com foco em vendas.
- Produto: `Método MUSA - Presença Elegante em 7 Dias`.
- Direção de produto atual: `PDE - Produto Digital Experiencial`.
- Preço definido: `R$47`.
- Regra de execução: priorizar ações pelo sistema, endpoints e APIs oficiais.
- Uso de banco de dados: apenas leitura/diagnóstico, salvo decisão explícita em contrário.
- Tráfego pago: não liberar enquanto a página de venda não estiver publicada/auditada e o fluxo de checkout/entrega não estiver validado.

## Resposta sobre alteração direta no banco

Até este registro, a orientação operacional é:

- Checkout: criado via API oficial do Mercado Pago.
- Página de venda draft: criada como arquivo local de publicação estática.
- Atualizações do experimento: devem ser feitas pelo backend/sistema quando existir endpoint adequado.
- Banco remoto: usado apenas para validação/leitura, como confirmar bloqueios e ausência de templates ativos.
- Não executar alteração direta manual no banco sem registrar antes neste documento o motivo, a tabela, a mudança e a alternativa pelo sistema que foi tentada.

## Decisão comercial

O experimento 66 não deve ser tratado como pesquisa de mecanismos, PDF isolado ou pacote de arquivos soltos.

A direção escolhida é vender uma experiência guiada com materiais de apoio:

**Método MUSA - Presença Elegante em 7 Dias**

Promessa:

> Monte em 7 dias uma presença mais elegante, marcante e coerente sem depender de luxo caro, compras impulsivas ou transformação radical.

CTA principal:

> Quero meu Kit MUSA por R$47

## Decisão PDE - Produto Digital Experiencial

Data: `2026-07-15`.

Decisão do usuário:

- Trabalhar com o formato 4: **site/experiência orientada + e-book + checklists + templates**.
- Evoluir o FEO para fabricar PDE, não apenas entregáveis.
- A base do produto continua sendo os princípios científicos obtidos no MDS/research, mas traduzidos para transformação prática, emocional e comercial.

### Alternativas avaliadas

1. **E-book bonito com figuras**
   - Potencial estimado: `55% a 65%`.
   - Benefício: simples de fabricar e entregar.
   - Risco: ainda parece conteúdo para ler, com menor sensação de acompanhamento.
   - Aderência ao objetivo de vendas: média.

2. **Kit de entregáveis baixáveis**
   - Potencial estimado: `65% a 75%`.
   - Benefício: tangível, prático e melhor que PDF isolado.
   - Risco: exige disciplina da cliente e pode parecer pasta de arquivos.
   - Aderência ao objetivo de vendas: boa.

3. **Site/experiência orientada**
   - Potencial estimado: `78% a 88%`.
   - Benefício: reduz esforço, guia aplicação, aumenta percepção de acompanhamento.
   - Risco: se vier sem biblioteca de apoio, pode parecer leve demais.
   - Aderência ao objetivo de vendas: alta.

4. **PDE completo: experiência guiada + e-book + checklists + templates**
   - Potencial estimado: `88% a 94%`.
   - Benefício: combina experiência conduzida, prova visual e materiais baixáveis.
   - Risco: exige pipeline mais cuidadoso e gate de qualidade mais forte.
   - Aderência ao objetivo de vendas: melhor.

Escolha aplicada: **alternativa 4**.

### Novo posicionamento do produto

Não vender como:

> Você recebe um PDF e alguns materiais.

Vender como:

> Você entra em uma experiência guiada de 7 dias para construir sua presença elegante, com diagnóstico, missões diárias, exemplos visuais, checklists e materiais prontos.

### Novo padrão esperado do FEO

O FEO deve fabricar um **Produto Digital Experiencial** com:

1. `01-experiencia-guiada/index.html`
   - diagnóstico inicial;
   - jornada de 7 dias;
   - missões diárias;
   - checkpoints;
   - painel de progresso;
   - biblioteca de apoio.

2. `02-ebook-principal.pdf`
   - capa bonita;
   - figuras internas;
   - explicação aplicada dos princípios MDS;
   - guia de uso da experiência.

3. `03-plano-checklists-e-templates.csv`
   - campos preenchíveis;
   - checklist de execução;
   - templates de decisão;
   - evidência de progresso.

4. `imagens/`
   - capa editorial;
   - infográfico de 7 dias;
   - mapa visual do mecanismo;
   - antes/depois conceitual.

5. `README.txt`
   - ordem simples de uso;
   - promessa;
   - aviso de que a experiência é guiada e os arquivos são apoio.

### Regra de qualidade

O PDE não pode expor termos internos como `FEO`, `experimento`, `CTR`, `CPL`, `lead`, `checkout`, `score`, `JSON`, `sha256`, `promessa validada` ou `mecanismo validado` no produto da cliente.

O e-book e a experiência devem transformar a ciência do MDS em:

- decisão guiada;
- microação diária;
- exemplo visual;
- checklist;
- campo preenchível;
- evidência de progresso;
- redução de esforço percebido.

### Implementação local preparada

Mudança feita no módulo executor `feo`:

- o plano do FEO passou a nascer como `PDE - Produto Digital Experiencial`;
- foram adicionados componentes de experiência:
  - diagnóstico guiado;
  - missões de 7 dias;
  - painel de progresso;
  - biblioteca de apoio;
- a montagem final agora gera `01-experiencia-guiada/index.html` como produto principal;
- o e-book virou apoio em `02-ebook-principal.pdf`;
- os checklists/templates viraram apoio em `03-plano-checklists-e-templates.csv`;
- o ZIP final passou a ser `00-metodo-musa-produto-digital-experiencial.zip`;
- o artefato técnico da montagem passou de `FINAL_HTML` para `FINAL_EXPERIENCE_SITE`.

Validações executadas:

- `mvn -f feo/pom.xml test`
  - resultado: `BUILD SUCCESS`;
  - testes: `9`, falhas `0`, erros `0`.
- `mvn -f backend/ads-service/pom.xml -Dtest=FeoFabricacaoV1ServiceTest test`
  - resultado: `BUILD SUCCESS`;
  - testes: `6`, falhas `0`, erros `0`.

Próximo passo operacional para o experimento 66:

1. fazer deploy do backend/worker FEO com a evolução PDE;
2. garantir `OPENAI_API_KEY` configurada no worker;
3. reprocessar o experimento 66 pelo FEO;
4. revisar manualmente o novo ZIP PDE;
5. só depois retomar página de venda/tráfego.

## Decisão de plataforma PDE multi-produto

Data: `2026-07-15`.

Decisão do usuário:

- O site do produto deve rodar em Docker.
- Deve ter frontend React e backend Java/Spring Boot/Maven.
- A solução deve ser independente do restante do sistema.
- O motor deve ser reutilizável para diversos produtos, não um front/back diferente para cada produto.

Documento canônico criado:

- `docs/canonical/pde-platform-canon.v1.md`

### Alternativas avaliadas

1. **Front/back separado para cada produto**
   - Benefício: liberdade total de experiência.
   - Risco: custo alto, manutenção difícil e baixa velocidade para escalar vários produtos.
   - Aderência ao objetivo de vendas em escala: baixa.

2. **Um único PDE genérico para todos os produtos**
   - Benefício: velocidade e manutenção simples.
   - Risco: experiência ficar genérica demais se o produto não carregar identidade, missões e materiais próprios.
   - Aderência ao objetivo de vendas em escala: boa.

3. **Motor comum + configuração por produto**
   - Benefício: escala, mantém identidade individual e permite o FEO fabricar muitos produtos.
   - Risco: exige modelagem inicial mais cuidadosa.
   - Aderência ao objetivo de vendas em escala: melhor.

Escolha aplicada: **motor comum + configuração por produto**.

### Implementação inicial criada

Módulo novo:

- `pde-platform/`

Componentes:

- `pde-platform/backend`: backend Java 21, Spring Boot 3 e Maven;
- `pde-platform/frontend`: frontend React 18, Vite e TypeScript;
- `pde-platform/docker-compose.yml`: execução Docker do conjunto;
- produto inicial configurado: `metodo-musa-7-dias`.

Funções já implementadas na v1 local:

- catálogo público do produto PDE;
- endpoint de liberação de acesso para validação local;
- endpoint de webhook Pepper inicial para compra aprovada;
- workspace por token de acesso;
- controle de missões concluídas;
- frontend com hero, diagnóstico, progresso, missões de 7 dias e biblioteca de materiais.

### Validação local da PDE Platform

Validações executadas:

- `mvn -f pde-platform/backend/pom.xml test`
  - resultado: `BUILD SUCCESS`;
  - testes: `2`, falhas `0`, erros `0`.
- `npm install --include=dev`
  - resultado: dependências instaladas;
  - `npm audit --omit=dev`: `0 vulnerabilities`.
- `npm run build`
  - resultado: build React/Vite concluído com sucesso.
- Backend local:
  - URL: `http://localhost:8096`;
  - endpoint validado: `GET /api/pde/products/metodo-musa-7-dias`;
  - endpoint validado: `POST /api/pde/access/dev`;
  - endpoint validado: `GET /api/pde/access/{token}/workspace`;
  - endpoint validado: `POST /api/pde/access/{token}/missions/dia-1-ruido-visual/complete`;
  - progresso após concluir a primeira missão: `14%`.
- Frontend local:
  - URL: `http://localhost:5176`;
  - proxy `/api` validado contra o backend local.

Limitação real de ambiente:

- `docker version` falhou porque o daemon Docker não está disponível em `/var/run/docker.sock`.
- Os arquivos Docker e `docker-compose.yml` foram criados, mas não foi possível executar containers neste ambiente.

Próximos passos antes de uso comercial:

1. Persistir acessos e progresso em banco.
2. Validar assinatura/autenticidade do webhook Pepper com a documentação final da conta.
3. Conectar o FEO para publicar o pacote PDE no catálogo do motor.
4. Configurar domínio/SSL.
5. Validar fluxo real: Pepper compra aprovada -> webhook -> acesso -> experiência -> materiais.

## Entregáveis reais do pacote

O pacote FEO foi tratado como um Kit de Transformação Aplicável, com:

- manifesto e ordem de uso;
- plano rápido de 7 dias;
- checklist de aplicação;
- templates prontos;
- exemplo preenchido;
- preview antes/depois;
- ritual de acompanhamento;
- bônus anti-objeção;
- guia de primeiros resultados;
- anexos práticos.

## Página de venda

Foi criada uma página de venda draft usando os entregáveis reais como base:

- Arquivo: `lead-portal-payments-service/docker/proxy/html/sales-page-exp66.html`
- Oferta: kit digital por `R$47`.
- Checkout conectado: Mercado Pago.
- Status: draft comercial, ainda não liberado para tráfego.

Pontos que a página precisa comunicar claramente:

- o que a cliente recebe;
- como usar o kit;
- por que o método reduz esforço;
- por que não depende de luxo caro;
- quais entregáveis são práticos e aplicáveis;
- qual resultado inicial a cliente deve buscar em 7 dias.

## Checkout Mercado Pago

Checkout criado para o produto:

- Produto: `Método MUSA - Presença Elegante em 7 Dias`
- Valor: `R$47`
- Referência: `marketinghub-experiment-66`
- URL: `https://www.mercadopago.com.br/checkout/v1/redirect?pref_id=133771061-58ed9edb-6f21-44f0-a169-b908daa5b4e8`

Observação: o checkout existe, mas o destino oficial de campanha não deve ser o checkout direto. Para tráfego pago, o destino correto deve ser uma página de venda auditada pelo GeraSalesPage.

## Decisão Pepper como checkout automatizável

Data: `2026-07-15`.

Decisão do usuário:

- Usar a **Pepper** como caminho preferencial para automatizar criação de produto/oferta do experimento 66.
- Manter o **PDE/área de membros dentro do Marketing Hub**, porque a experiência guiada, progresso, uso do MDS/research e melhoria contínua do produto devem continuar sob controle do sistema.
- Não depender de área de membros externa como núcleo do produto.

### Justificativa estratégica

A Pepper é mais aderente ao Marketing Hub do que a Kiwify para a fábrica automática porque a documentação pública informa API para:

- criar produto;
- criar oferta;
- listar/editar produtos e ofertas;
- criar transações PIX;
- obter checkout;
- consultar transações.

Isso permite que o Marketing Hub crie e atualize ofertas em escala, sem depender de preenchimento manual em tela para cada novo produto.

### Alternativas avaliadas

1. **Continuar só com Mercado Pago**
   - Benefício: já existe checkout criado para o experimento 66.
   - Risco: menor aderência ao mercado de infoprodutos, menos recursos comerciais prontos e menor flexibilidade para ofertas digitais em escala.
   - Uso recomendado: fallback operacional e validação rápida.

2. **Usar Kiwify**
   - Benefício: boa plataforma para checkout, área de membros e assinatura.
   - Risco: automação de criação de produto por API é menos clara; depender de login manual reduz escalabilidade do Marketing Hub.
   - Uso recomendado: apenas se operação manual/semi-manual for aceitável.

3. **Usar Pepper + PDE próprio no Marketing Hub**
   - Benefício: maior automação comercial, checkout externo, URL de oferta e controle total da experiência pelo Marketing Hub.
   - Risco: exige integração técnica com token oficial, webhook e mapeamento de status de compra.
   - Uso recomendado: melhor caminho para fábrica automática de produtos digitais.

Escolha aplicada: **alternativa 3**.

### Modelo operacional definido

Fluxo alvo:

1. O FEO gera o PDE do experimento 66.
2. O GeraSalesPage publica a página de venda auditada.
3. O Marketing Hub cria o produto na Pepper:
   - `title`: `Método MUSA - Experiência Guiada de 7 Dias`;
   - `description`: experiência guiada com diagnóstico, missões, exemplos visuais, e-book, checklists e templates;
   - `product_type`: `curso` para pagamento único, ou `recorrencia` quando virar assinatura;
   - `price`: `4700`;
   - `sale_page`: URL pública auditada pelo GeraSalesPage;
   - `cover`: URL pública da capa/mockup do PDE.
4. O Marketing Hub cria uma oferta na Pepper para o produto.
5. A URL da oferta Pepper vira o checkout oficial da página de venda.
6. O webhook da Pepper confirma compra aprovada.
7. O Marketing Hub libera acesso ao PDE/área de membros.
8. O sistema registra venda, origem, experimento, produto, oferta e status de entrega.

### Regra de segurança

Não usar token pessoal de sessão, login/senha ou credencial ampla de navegador.

Para integração Pepper, aceitar apenas:

- token/API key oficial da Pepper;
- preferencialmente revogável;
- preferencialmente com escopo limitado;
- armazenado como variável de ambiente;
- nunca registrado em arquivo, log, relatório ou commit.

### Próximo passo necessário

Antes de implementar a criação automática real na Pepper, obter:

- token oficial da API Pepper;
- confirmação do endpoint de webhooks e eventos de compra aprovada;
- payload real de exemplo da Pepper;
- URL pública final da página de venda auditada;
- URL pública da capa/mockup do PDE.

Enquanto esses itens não existem, o experimento 66 continua usando Mercado Pago como checkout já criado, mas a direção de integração principal passa a ser Pepper.

## Criativos aprovados para teste inicial

Foram definidos 5 criativos/ângulos:

1. `Presença elegante em 7 dias`
2. `Elegância não começa no preço`
3. `Checklist de presença em 12 minutos`
4. `Pare de comprar no impulso`
5. `Quando o visual parece quase certo`

## Público inicial

Segmentação inicial proposta:

- mulheres urbanas;
- moda feminina;
- beleza e autocuidado;
- maquiagem;
- perfumes;
- cabelo;
- skincare;
- imagem pessoal;
- consultoria de estilo;
- compras online;
- consumo consciente;
- lojas de departamento/moda acessível.

IDs/segmentos Meta citados no andamento:

- `Beauty`;
- `Fragrances (cosmetics)`;
- comportamento de consumo qualificado/intermediário-alto no Brasil;
- cargo `Cabeleireira e Maquiadora`.

## Bloqueio atual

O bloqueio principal para liberar tráfego é:

- GeraSalesPage ainda não consegue concluir a página oficial porque falta template ativo de prompt/schema para `sales-page-offer-brief`.

Mensagem registrada no andamento:

> Template ativo de prompt/schema não encontrado para sales-page-offer-brief.

Diagnóstico informado:

- A tabela remota `ai_prompt_schema_template` estava sem templates ativos para `gera-sales-page-v1`.
- Esse diagnóstico deve ser tratado como leitura/validação.
- A correção preferencial é semear/restaurar os templates pelo fluxo controlado do sistema, changelog, seed oficial ou endpoint administrativo adequado, não por edição manual direta sem registro.

## Investigação do próximo passo

Foi verificado no repositório que já existe um recovery versionado para os templates do GeraSalesPage:

- Changeset: `backend/ads-service/src/main/resources/db/changelog/changesets/2026-07-15-gera-sales-page-template-recovery-v7.yaml`
- Include no mestre: `backend/ads-service/src/main/resources/db/changelog/db.changelog-master.yaml`
- Include possui `relativeToChangelogFile: true`.
- O changeset insere/reativa os templates `v7` para todas as etapas:
  - `sales-page-offer-brief`;
  - `sales-page-wireframe`;
  - `sales-page-copy`;
  - `sales-page-visual-plan`;
  - `sales-page-html`;
  - `sales-page-checkout-quality-review`;
  - `sales-page-publication-package`.

Conclusão operacional:

- O caminho correto não é inserir manualmente templates no banco.
- A causa provável é que o Liquibase do ambiente remoto ainda não aplicou esse changeset, aplicou com `MARK_RAN` por precondição, ou o backend remoto em uso não está com esse código/changelog carregado.
- Próximo passo seguro: validar execução do Liquibase/histórico do changeset no ambiente remoto e reaplicar pelo fluxo de deploy/migração do sistema, sem `UPDATE`/`INSERT` manual direto.

## Validação remota via MCP

Consulta somente leitura feita pelo MCP em `DATABASECHANGELOG`:

- Changeset pesquisado: `2026-07-15-gera-sales-page-template-recovery-v7-001`
- Resultado: `0` linhas.

Consulta somente leitura feita pelo MCP em `ai_prompt_schema_template`:

- Filtro: `pipeline_code = 'gera-sales-page-v1'`
- Resultado: `0` linhas.

Conclusão confirmada:

- O ambiente remoto não aplicou o recovery `v7`.
- O bloqueio do GeraSalesPage não é ausência de definição no repositório; é ausência da migração aplicada no ambiente remoto.
- Próxima ação correta: executar o fluxo oficial de migração/deploy do backend para aplicar o Liquibase, depois reiniciar o GeraSalesPage do experimento 66.

## Validação local

Teste executado:

```bash
mvn -f backend/ads-service/pom.xml -Dtest=AiPromptSchemaTemplateChangelogTest test
```

Resultado:

- `Tests run: 2`
- `Failures: 0`
- `Errors: 0`
- `BUILD SUCCESS`

Conclusão:

- O repositório local possui o recovery `v7` protegido por teste.
- A pendência está no ambiente remoto, não na falta de changelog local.

## Próxima ação segura

Antes de mexer em qualquer dado remoto, comparar 3 caminhos:

1. **Inserir templates direto no banco**
   - Benefício: rápido.
   - Risco: contorna rastreabilidade, pode criar divergência entre ambientes.
   - Aderência: baixa, só usar como exceção registrada.

2. **Criar/usar seed versionado do sistema**
   - Benefício: rastreável, repetível e alinhado ao produto.
   - Risco: exige mais cuidado técnico.
   - Aderência: alta.

3. **Acionar endpoint administrativo/carga já existente**
   - Benefício: usa sistema e reduz intervenção manual.
   - Risco: depende de existir endpoint/rotina funcional.
   - Aderência: alta se já existir.

Escolha recomendada:

**Primeiro procurar endpoint/rotina existente; se não existir, criar seed versionado ou changelog rastreável. Não alterar direto no banco como primeira opção.**

## Revisão crítica dos entregáveis FEO

Data da revisão: `2026-07-15`.

Motivo:

- O PDF `feo/02-pacote-final-pdf.pdf` foi revisado visualmente e apresentou linguagem técnica de campanha dentro do material que seria entregue à compradora.
- Exemplo crítico visto no PDF: seção `Prova e limites de confiança` com `CTR`, `CPL`, conversão de lead, clique de intenção de compra e critério de pré-venda.

Conclusão objetiva:

**O pacote FEO atual do experimento 66 não está aprovado para entrega ao cliente e não deve ser vendido/publicado como produto final.**

### Evidências do ZIP oficial

Arquivo analisado:

- `lead-portal-payments-service/docker/proxy/html/downloads/experimento-66-entregaveis.zip`

Conteúdo identificado:

- `README.txt`
- `feo/01-pacote-final-html.html`
- `feo/02-pacote-final-pdf.pdf`
- `feo/03-manifesto-entregaveis-csv.csv`
- 31 arquivos em `entregaveis/`
- 2 arquivos em `pacotes/`

Problemas encontrados:

1. **Métrica interna apareceu no produto final**
   - `CTR`, `CPL`, taxa de lead, clique de intenção de compra e critério de pré-venda aparecem dentro do PDF.
   - Isso é dado de validação de campanha, não conteúdo para compradora.

2. **Promessa antiga de pesquisa contaminou o produto**
   - O pacote ainda usa a promessa interna: `Descobrir quais mecanismos de imagem, autocuidado e percepção social podem sustentar uma oferta MUSA de alto valor antes de criar a promessa final`.
   - Para cliente final, a promessa correta deve ser: montar presença elegante em 7 dias sem depender de luxo caro, compras impulsivas ou transformação radical.

3. **O PDF fala como relatório de sistema, não como produto de transformação**
   - Termos como `promessa validada`, `mecanismo validado`, `gate de qualidade comercial`, `Score FEO`, `Fabricado pela FEO v1`, `READY_FOR_PREMIUM_REVIEW` e `contexto validado do experimento` não devem aparecer para a compradora.

4. **Há wrappers técnicos sendo tratados como entregáveis**
   - Arquivos como `029-01-pacote-final-html.html`, `030-02-pacote-final-pdf.html`, `031-03-manifesto-entregaveis-csv.html` e vários `entregaveis-kit-*` mostram JSON, `sha256`, `contentType`, `consumptionOrder` e `Prompt e rastreabilidade`.
   - Isso deve ficar em relatório interno, nunca no pacote comprado.

5. **Os entregáveis individuais estão muito rasos**
   - Vários materiais têm apenas descrição, conteúdo pronto para uso e rastreabilidade.
   - Falta corpo real de execução: instruções passo a passo, campos preenchíveis úteis, exemplo preenchido real, checklist acionável e orientação visual concreta.

6. **Há duplicação e confusão de versão**
   - Existem materiais antigos bons como base (`012` a `016` e `022` a `028`), mas também existem versões embrulhadas pelo FEO (`032` a `047`) que repetem metadados em vez de entregar valor.
   - A compradora receberia arquivos demais sem clareza de ordem e com nomes técnicos.

7. **A linguagem ainda não está emocional/comercial o suficiente**
   - O produto promete elegância acessível, mas o material fala de decisão, evidência, critério e mecanismo.
   - Para vender e reter satisfação, precisa falar de espelho, rotina, roupa, cabelo, pele, perfume, presença, orçamento, reaproveitamento e segurança para sair de casa se sentindo mais alinhada.

### O que é aproveitável

Materiais com boa direção estratégica, mas ainda rasos:

- `012-checklist-12-detalhes-de-elegancia-acessivel.html`
- `013-rotina-30-dias-de-presenca-sofisticada.html`
- `014-guia-de-compras-inteligentes-sem-luxo.html`
- `015-mapa-de-assinatura-pessoal-cabelo-perfume-e-imagem.html`
- `016-mini-auditoria-visual-antes-e-depois.html`
- `022-mds-musa-diagnostico-de-ruido-visual-e-coerencia-de-presenca.html`
- `023-mds-musa-mapa-de-paleta-base-contraste-e-ponto-de-cor.html`
- `024-mds-musa-guia-de-assinatura-olfativa-acessivel-por-ocasiao.html`
- `025-mds-musa-checklist-12-minutos-de-cabelo-pele-unha-roupa-e-acessorios.html`
- `026-mds-musa-matriz-ocasiao-presenca.html`
- `027-mds-musa-plano-7-dias-de-microajustes-de-presenca-elegante.html`
- `028-mds-musa-lista-de-compras-anti-impulso-e-reaproveitamento-elegante.html`

Esses materiais devem virar a base do novo produto, mas precisam ser reescritos como guias completos, não como cards descritivos.

### O que deve ser removido do pacote da cliente

- `CTR`
- `CPL`
- `lead`
- `pré-venda`
- `critério de sucesso`
- `score`
- `gate`
- `FEO`
- `fabricado pela FEO`
- `promessa validada`
- `mecanismo validado`
- `experimento`
- `tráfego`
- `checkout`
- `sha256`
- `contentType`
- `JSON`
- `Prompt e rastreabilidade`
- `READY_FOR_PREMIUM_REVIEW`
- qualquer campo de auditoria, hash, status, template, modelo ou id técnico.

### Alternativas avaliadas

1. **Corrigir apenas o texto do PDF atual**
   - Benefício: mais rápido.
   - Risco: mantém estrutura errada, duplicação e arquivos técnicos.
   - Aderência ao objetivo de venda: baixa.

2. **Remover só os trechos técnicos e publicar**
   - Benefício: reduz o erro mais visível.
   - Risco: o produto continua raso e com baixa percepção de valor.
   - Aderência ao objetivo de venda: média/baixa.

3. **Refazer o pacote da cliente a partir dos materiais bons, separando auditoria interna de entrega final**
   - Benefício: gera produto vendável, claro, aplicável e compatível com R$47.
   - Risco: exige regenerar/revisar FEO e ajustar o montador para não vazar metadados.
   - Aderência ao objetivo de venda: alta.

Escolha recomendada:

**Alternativa 3.**

### Novo padrão obrigatório para o pacote da cliente

O pacote final do experimento 66 deve conter, no máximo, uma estrutura limpa:

1. `Comece aqui - ordem de uso`
2. `Plano de 7 dias`
3. `Diagnóstico de presença elegante`
4. `Checklist 12 minutos`
5. `Mapa de paleta, contraste e ponto de cor`
6. `Guia de assinatura olfativa acessível`
7. `Matriz ocasião-presença`
8. `Lista de compras anti-impulso`
9. `Mini-auditoria antes/depois`
10. `Bônus: reaproveitamento elegante com orçamento limitado`

Cada arquivo precisa ter:

- promessa em linguagem de compradora;
- para quem é;
- tempo estimado de uso;
- passo a passo;
- campos preenchíveis;
- exemplo preenchido real;
- checklist final;
- erro comum;
- miniresultado esperado.

### Correção de causa-raiz necessária

O problema não é apenas o experimento 66. A causa-raiz está na montagem do FEO:

- o pacote final usa campos internos de validação como se fossem conteúdo de comprador;
- a montagem coloca relatório, auditoria e rastreabilidade dentro do ZIP público;
- o gate de qualidade atual aprova presença de estrutura, mas não bloqueia vazamento de termos técnicos;
- não existe separação forte entre `produto para cliente` e `relatório interno de fabricação`.

Correção sistêmica recomendada antes de liberar venda:

1. Criar separação explícita entre:
   - pacote final da compradora;
   - relatório interno FEO;
   - manifesto técnico/auditável.
2. Adicionar bloqueio de termos proibidos no pacote da compradora.
3. Regenerar o FEO do experimento 66 com a promessa comercial atual.
4. Revisar manualmente o novo ZIP antes de conectar à entrega pós-compra.
5. Só depois retomar GeraSalesPage/tráfego.

Status após revisão:

- Produto comercial: **reprovado para entrega**.
- Página de venda: **não deve ser publicada usando o pacote atual como prova final**.
- Checkout: pode existir, mas não deve receber tráfego enquanto o produto entregue estiver nesse estado.
- Próximo passo: **regenerar/reconstruir o pacote FEO do experimento 66 com fronteira limpa entre cliente e auditoria interna**.

## Correção sistêmica do FEO para imagens e e-books ricos

Data: `2026-07-15`.

Decisão do usuário:

- O FEO deve usar gerador de imagens da OpenAI.
- O pacote final não deve criar HTML para a cliente.
- O produto deve conter itens mais ricos: e-books com capa bonita, infográficos, figuras internas e materiais realmente interessantes.
- A referência de qualidade deve vir dos produtos de sucesso: prova visual do que a pessoa recebe, materiais aplicáveis, redução de esforço e sensação de produto completo.

Alternativas avaliadas:

1. **Limpar apenas o PDF atual**
   - Benefício: rápido.
   - Risco: continua raso, sem imagens e sem corrigir vazamento de metadados.
   - Aderência a vendas: baixa.

2. **Inserir imagens dentro da montagem atual**
   - Benefício: gera visual com menos mudança.
   - Risco: mistura texto, imagem, auditoria e ZIP final na mesma responsabilidade.
   - Aderência a vendas: média.

3. **Criar etapa explícita de geração de ativos visuais dentro do pipeline FEO**
   - Benefício: separa redação, imagens e montagem; permite auditar request/response da OpenAI sem vazar isso para a cliente; força o pacote a nascer com capa, infográfico e figuras internas.
   - Risco: exige ajuste no backend e no worker.
   - Aderência a vendas: alta.

Escolha aplicada: **alternativa 3**.

Mudança preparada no código:

- Nova etapa no FEO: `geracao-ativos-visuais`.
- Sequência do pipeline:
  1. `planejamento-entregaveis`
  2. `redacao-entregaveis`
  3. `geracao-ativos-visuais`
  4. `montagem-pacote`
- Integração OpenAI no módulo executor `feo`, via endpoint oficial de geração de imagens.
- Modelo configurável por ambiente:
  - `FEO_IMAGE_MODEL`, padrão `gpt-image-2`;
  - `FEO_IMAGE_QUALITY`, padrão `high`;
  - `OPENAI_API_KEY`;
  - `OPENAI_BASE_URL`.
- A etapa bloqueia se não houver chave OpenAI, para não entregar pacote pobre sem imagens reais.
- O ZIP público da compradora passa a conter:
  - `01-experiencia-guiada/index.html`;
  - `02-ebook-principal.pdf`;
  - `03-plano-checklists-e-templates.csv`;
  - imagens em `imagens/`;
  - `README.txt`.
- O ZIP público não deve conter:
  - HTML técnico, wrappers internos ou relatório de sistema;
  - relatório de fabricação;
  - manifesto técnico;
  - hashes, JSON, prompts, status, score ou termos internos.

Tipos de imagem exigidos:

- capa editorial do e-book;
- infográfico do plano de 7 dias;
- mapa visual do mecanismo;
- figura conceitual de antes/depois.

Fonte técnica usada para a integração:

- Documentação oficial OpenAI: `gpt-image-2` é modelo de geração/edição de imagens e expõe endpoint `/v1/images/generations`.
- A referência oficial de geração de imagens indica retorno base64 para modelos GPT Image, adequado para armazenar a imagem no pacote final.

Status:

- Código preparado localmente.
- Testes do backend FEO passaram.
- Testes do módulo FEO passaram.
- Ainda falta deploy/reprocessamento do experimento 66 para gerar um novo ZIP real com imagens OpenAI.

### Complemento — MDS como base do produto comercial

Data: `2026-07-15`.

Diretriz reforçada pelo usuário:

- A base dos produtos gerados pelo FEO deve ser os princípios científicos obtidos no MDS/research.
- Esses princípios não devem aparecer como relatório técnico ou teoria crua.
- O FEO deve transformar a base científica em produto comercial de alto impacto, com aplicação prática, percepção de transformação e redução de esforço para a cliente.

Correção aplicada no módulo `feo`:

- O conteúdo público dos entregáveis agora possui campo de `princípio aplicado`.
- A redação dos materiais traduz sinais do MDS em:
  - regra prática;
  - exercício;
  - decisão guiada;
  - exemplo preenchido;
  - comparação antes/depois;
  - critério visual de progresso.
- O e-book passa a explicar que a pesquisa vira transformação prática, sem expor linguagem técnica.
- A montagem do pacote agora bloqueia termos proibidos no material da cliente, como `CTR`, `CPL`, `pré-venda`, `score FEO`, `promessa validada`, `mecanismo validado`, `experimento`, `tráfego`, `checkout`, `sha256`, `JSON` e `READY_FOR_PREMIUM_REVIEW`.

Validação executada:

- `mvn -f feo/pom.xml test`
  - resultado: `BUILD SUCCESS`
  - testes: `9`, falhas: `0`, erros: `0`

Conclusão:

- A causa-raiz começou a ser corrigida no pipeline, não apenas no PDF do experimento 66.
- O próximo ZIP do 66 deve ser regenerado pelo FEO antes de qualquer entrega ao cliente.

## Validação após aplicação do Liquibase

Data da validação: `2026-07-15T03:44:09Z`.

Resultado:

- O changeset `2026-07-15-gera-sales-page-template-recovery-v7-001` apareceu no `DATABASECHANGELOG` remoto com `EXECTYPE=EXECUTED`.
- A tabela `ai_prompt_schema_template` passou a ter `7` templates ativos para `pipeline_code='gera-sales-page-v1'`.
- Todos os templates ativos estão na versão `v7`:
  - `sales-page-offer-brief`;
  - `sales-page-wireframe`;
  - `sales-page-copy`;
  - `sales-page-visual-plan`;
  - `sales-page-html`;
  - `sales-page-checkout-quality-review`;
  - `sales-page-publication-package`.

Validação pelo sistema:

- O endpoint `POST /api/experiments/66/gerasalespage/v1/start` deixou de retornar bloqueio por template ausente.
- O backend criou a execução `a01d8ebb-e836-49cc-aecf-dad904f869a0` para a etapa `sales-page-offer-brief`.
- A execução foi criada usando o template `gera-sales-page-v1:sales-page-offer-brief:v7`.
- O endpoint `GET /api/internal/gerasalespage/v1/sales-page-offer-brief/stage-executions/pending` passou a entregar ao worker o contexto completo do experimento, incluindo checkout, preço, promessa e entregáveis reais do FEO.
- O AI Worker consumiu a execução e o status remoto passou para `AGUARDANDO_RETORNO_OPENAI`.
- Em nova consulta, a etapa `sales-page-offer-brief` apareceu como `CONCLUIDO`, com `openai_model='gpt-5.5'`.
- O backend criou automaticamente a próxima etapa `sales-page-wireframe`, usando `gera-sales-page-v1:sales-page-wireframe:v7`, com status `INICIADO`.

Conclusão:

- O Liquibase deu certo.
- O bloqueio de template do GeraSalesPage foi removido.
- A primeira etapa do GeraSalesPage foi concluída.
- A página oficial ainda não está publicada; o pipeline está em execução na etapa `sales-page-wireframe`.

Observação operacional:

- Houve registros transitórios de `Connection refused` do AI Worker ao acessar `http://191.252.181.168:80`, mas o job foi consumido depois.
- Até esta validação, não foi feita alteração manual direta no banco; as consultas foram somente leitura via MCP.

## Pendências para ficar pronto para tráfego

- Acompanhar a conclusão da etapa `sales-page-wireframe`.
- Acompanhar a conclusão das próximas etapas do GeraSalesPage v1 até `sales-page-publication-package`.
- Auditar a página publicada.
- Confirmar que o destino da campanha aponta para a página auditada, não para o checkout direto.
- Validar fluxo de compra e entrega do produto digital.

## Diagnóstico da revisão de qualidade do GeraSalesPage

Data da validação: `2026-07-15T03:53:30Z`.

Resultado remoto por leitura via MCP:

- `sales-page-offer-brief`: `CONCLUIDO`
- `sales-page-wireframe`: `CONCLUIDO`
- `sales-page-copy`: `CONCLUIDO`
- `sales-page-visual-plan`: `CONCLUIDO`
- `sales-page-html`: `CONCLUIDO`
- `sales-page-checkout-quality-review`: `FALHA`
- `sales-page-publication-package`: ainda não criado

Causa da reprovação:

- a página gerada ainda expôs linguagem interna para a cliente, como `Pacote Final`, `FEO #3` e nome técnico do pacote;
- a página não explicou com clareza como a compradora recebe o produto após o pagamento aprovado.

Conclusão:

- o problema não era mais ausência de checkout, template ou worker parado;
- a causa-raiz era contrato/prompt insuficiente para impedir linguagem interna e ausência de instrução de entrega pós-compra;
- além disso, o serviço de pagamento ainda estava preparado para entrega automática do experimento 51, mas não do experimento 66.

## Correção preparada para fechar entrega e nova geração

Data: `2026-07-15`.

Ações feitas no repositório, sem alteração manual direta no banco:

- adicionado suporte configurável ao produto digital do experimento 66 no `lead-portal-payments-service`;
- configurada referência `marketinghub-experiment-66`;
- configurado nome público `Método MUSA - Presença Elegante em 7 Dias`;
- configurada página de entrega `https://pagamentopalf.site/obrigado-exp66-metodo-musa.html`;
- configurado download `https://pagamentopalf.site/downloads/experimento-66-entregaveis.zip`;
- baixado o ZIP real do backend para `lead-portal-payments-service/docker/proxy/html/downloads/experimento-66-entregaveis.zip`;
- criada página pública de entrega `obrigado-exp66-metodo-musa.html`;
- criado endpoint de reenvio `/api/v1/digital-product-deliveries/exp66/email`;
- criado changeset `2026-07-15-gera-sales-page-exp66-delivery-v8.yaml` para ativar prompts `v8` nas etapas:
  - `sales-page-copy`;
  - `sales-page-html`;
  - `sales-page-checkout-quality-review`;
  - `sales-page-publication-package`.

Objetivo do `v8`:

- impedir termos internos na página pública;
- forçar nomes públicos como `Kit MUSA` e `Método MUSA`;
- explicar entrega digital por e-mail após pagamento aprovado;
- informar página de entrega e ZIP real;
- manter a revisão de qualidade bloqueando página que não explique entrega, checkout, preço e entregáveis reais.

Próximo passo operacional:

- validar testes locais;
- aplicar o Liquibase `v8` no ambiente remoto;
- executar `POST /api/experiments/66/gerasalespage/v1/rebuild`;
- aguardar nova revisão;
- liberar tráfego somente se `sales-page-publication-package` concluir e o readiness remover o bloqueio `GERA_SALES_PAGE`.

## Validação local da correção preparada

Data: `2026-07-15T04:01:03Z`.

Validações executadas:

- `mvn -f backend/ads-service/pom.xml -Dtest=AiPromptSchemaTemplateChangelogTest test`
  - resultado: `BUILD SUCCESS`
  - testes: `3`, falhas: `0`, erros: `0`
- `mvn -f lead-portal-payments-service/pom.xml -Dtest=DigitalProductPostPurchaseEmailServiceTest test`
  - resultado: `BUILD SUCCESS`
  - testes: `2`, falhas: `0`, erros: `0`
- `mvn -f backend/ads-service/pom.xml -Dliquibase.url=offline:mysql?version=5.7 -Dliquibase.changeLogFile=src/main/resources/db/changelog/db.changelog-master.yaml liquibase:validate`
  - resultado: `BUILD SUCCESS`
  - Liquibase: `No validation errors found`
- validação de include relativo no master:
  - resultado: nenhum `include` relativo sem `relativeToChangelogFile: true`.

Validação remota antes de rebuild:

- `https://pagamentopalf.site/obrigado-exp66-metodo-musa.html`: `404`
- `https://pagamentopalf.site/downloads/experimento-66-entregaveis.zip`: `404`
- `https://pagamentopalf.site/api/v1/digital-product-deliveries/exp66/email`: `404`
- banco remoto ainda possui apenas templates `v7` ativos para `gera-sales-page-v1`; nenhum template `v8` aplicado até esta validação.

Decisão:

- não executar rebuild remoto ainda;
- se o rebuild for executado antes do deploy do serviço de pagamento e antes do Liquibase `v8`, a página pode continuar reprovando ou prometer uma entrega que ainda não existe em produção;
- próximo passo correto: publicar/deployar as alterações do `lead-portal-payments-service` e aplicar o Liquibase `v8`; depois disso, executar o rebuild do GeraSalesPage do experimento 66.
- Só depois liberar Facebook Ads.

## Verificação de aderência aos objetivos da semana 3

Data da validação: `2026-07-15T03:50:00Z`.

Objetivo analisado:

- Confirmar se o experimento 66 está aderente à direção da semana 3: transformar o FEO em produto low-ticket vendável, com página de venda, checkout, prova visual, criativos, público e funil pronto para medir compra real.

Consultas realizadas:

- `GET /api/experiments/66/construction`
- `GET /api/experiments/66/readiness`
- `GET /api/experiments/66/gerasalespage/v1/publications`
- MCP `db_query` somente leitura em `experiment`, `gera_sales_page_stage_execution` e `gera_sales_page_publication_audit`.

Resultado comercial:

- O experimento está posicionado corretamente como venda low-ticket:
  - `experiment_type = LOW_TICKET_PRODUCT`;
  - `campaign_objective = SALES`;
  - preço `R$47`;
  - CTA `Quero meu Kit MUSA por R$47`;
  - métrica principal `Compra aprovada do Kit MUSA`.
- O checkout Mercado Pago está preenchido em `follow_up_action_url`.
- Existem `5` criativos prontos.
- A segmentação está completa.
- A página draft local existe, mas não substitui a página oficial do GeraSalesPage.

Status do GeraSalesPage remoto:

- `sales-page-offer-brief`: `CONCLUIDO`.
- `sales-page-wireframe`: `CONCLUIDO`.
- `sales-page-copy`: `AGUARDANDO_RETORNO_OPENAI`.
- Ainda não há registro em `gera_sales_page_publication_audit` para o experimento 66.
- `GET /api/experiments/66/gerasalespage/v1/publications` retornou lista vazia.

Leitura de readiness:

- `hasCreatives = true`
- `creativeCount = 5`
- `hasCompleteTargeting = true`
- `hasLeadPortalFlow = false`
- bloqueio atual: `GERA_SALES_PAGE`
- mensagem: experimentos com intenção de compra só podem ser liberados quando o GeraSalesPage v1 concluir e auditar a publicação da página de venda.

Conclusão:

- O experimento 66 está aderente aos objetivos da semana 3 no posicionamento, produto, checkout, criativos e público.
- Ainda não está pronto para tráfego porque falta a página oficial publicada e auditada pelo GeraSalesPage.
- A causa do bloqueio não é mais template, checkout, criativo ou público; é apenas a conclusão do pipeline de página de venda e validação do destino final.

Próximas ações:

- Aguardar/conferir retorno da OpenAI para `sales-page-copy`.
- Confirmar avanço automático para `sales-page-visual-plan`, `sales-page-html`, `sales-page-checkout-quality-review` e `sales-page-publication-package`.
- Após publicação, validar se `follow_up_action_url` passou a apontar para a página auditada, mantendo o checkout separado nos botões.
- Testar acesso público da página, clique no checkout e entrega do produto.
- Só liberar tráfego depois desses pontos.

## Correção de criativos aprovados sem imagem

Data: `2026-07-15`.

Sintoma observado:

- Na tela do experimento 66, os 5 criativos apareciam em **Aprovados**, mas todos exibiam `Imagem não disponível`.

Validação pelo sistema:

- Consulta somente leitura via MCP na tabela `creative` confirmou 5 registros do experimento 66:
  - IDs `228`, `229`, `230`, `231`, `232`;
  - todos com `status = READY`;
  - todos com `ad_format = IMAGE`;
  - todos com `image_url = null`;
  - todos com `image_hash = null`.

Conclusão:

- Os criativos não estavam realmente prontos para tráfego.
- O problema não era apenas visual no frontend; o backend permitiu criativo de imagem aprovado sem asset visual.
- Comercialmente, esses 5 criativos devem ser tratados como inválidos até serem regerados com imagem real.

Correção aplicada no código:

- O AI Worker agora bloqueia geração concluída quando a OpenAI/upload não retorna `imageUrl`.
- O cliente de imagem preserva erro HTTP da OpenAI para permitir retry correto.
- A regra de retry dos criativos fica explícita:
  - tentativa 1: Flex;
  - tentativa 2: Flex;
  - tentativa 3: Standard/default.
- O backend passa a rejeitar criativo `READY` de formato `IMAGE` sem `imageUrl`.
- A prontidão/readiness passa a contar apenas criativos `READY` com imagem publicável.
- A aba Construção também deixa de considerar criativo aprovado sem imagem como ativo validado.

Validação:

- `mvn -f ai-worker/pom.xml -Dtest=CreativeImageClientTest,CreativeGenerationServiceTest test`: passou.
- `mvn -f backend/ads-service/pom.xml -Dtest=CreativeServiceTest,ExperimentReadinessServiceTest test`: passou.

Próximo passo operacional:

- Fazer deploy do backend e do AI Worker.
- Regerar os 5 criativos do experimento 66.
- Confirmar novamente no banco/sistema que cada criativo aprovado possui `image_url` real antes de liberar tráfego.

## Critério de exibição dos entregáveis na página de venda

Data: `2026-07-15`.

Pergunta do usuário:

- Se a seção interna `Entregáveis vinculados`, com itens como `MDS/MUSA - ...`, será mostrada na página de venda.

Decisão:

- Não mostrar esse bloco cru na página de venda.
- A lista técnica pode existir na administração e na auditoria interna para rastrear quais materiais alimentam o produto.
- A página pública deve mostrar uma seção comercial do tipo **O que você recebe**, com nomes simples, concretos e orientados a benefício.

Exemplo de transformação esperada:

- Interno: `MDS/MUSA - Guia de assinatura olfativa acessível por ocasião`.
- Público: `Guia de assinatura olfativa acessível`.
- Benefício: `Aprenda a escolher perfume e presença sensorial para criar memória sem depender de perfume caro`.

Ajuste aplicado:

- O payload do GeraSalesPage passou a enviar `publicTitle` e `publicDescription` para cada entregável FEO.
- Os prompts v8 do GeraSalesPage foram reforçados para usar `publicTitle/publicDescription` na página pública e bloquear exposição de `MDS/MUSA`, `FEO`, `Pacote Final`, `pipeline`, `stage` e linguagem de produção.

Critério comercial:

- A compradora precisa entender claramente o que recebe e por que aquilo facilita a transformação.
- Ela não deve ver rastreabilidade, origem MDS/research, nomes de pacote interno, arquivos HTML, IDs ou linguagem técnica.

## Objetivo da Semana 3 com PDE

Data: `2026-07-15`.

Decisão registrada no planejamento comercial pelo endpoint do sistema:

- Semana 3 passa a focar o experimento 66 como **PDE completo**, não como pacote de arquivos soltos.
- O objetivo comercial é elevar o potencial estimado de venda para acima de `90%`, condicionado aos gates de produto, página, checkout, criativos, tracking e entrega.

Objetivos gravados para a Semana 3:

1. Publicar o experimento 66 como PDE completo: área guiada de 7 dias, e-book premium, checklists/templates, imagens reais, diagnóstico, progresso e biblioteca de apoio, usando os princípios MDS/research transformados em aplicação comercial simples.
2. Entregar os elementos que aumentam o potencial estimado de venda para acima de `90%`: página de venda com prova visual clara, checkout real Pepper ou fallback Mercado Pago, webhook/acesso validado, entrega digital testada e oferta de `R$47` sem termos técnicos internos.
3. Garantir distribuição vendável antes de liberar tráfego: 3 a 5 criativos aprovados com imagem real, tentativa 1 e 2 em modo Flex e tentativa 3 em Standard, público MUSA salvo e rastreamento `page_view`, tempo de sessão, `checkout_click`, compra e acesso ao PDE.
4. Controlar gasto e aprendizado: liberar tráfego somente depois do PDE, página, checkout, criativos com imagem e tracking passarem no gate; teto adicional de `R$50` a `R$80` e corte se houver 100 acessos válidos sem checkout/interesse ou qualquer falha de entrega/acesso.

Observação:

- A meta `>90%` é tratada como potencial estimado de venda do formato, não garantia de venda.
- Tráfego continua bloqueado até o produto PDE, a página, o checkout, a entrega e os criativos com imagem real passarem nos gates.

## Correção do pacote FEO para linguagem de cliente

Data: `2026-07-16`.

Problema identificado pelo usuário:

- O ZIP oficial do experimento 66 ainda continha linguagem de bastidor e informação técnica de construção da peça.
- O material mencionava termos como `mecanismo`, `pesquisa`, `princípios científicos`, `cliente/comprador` e campos técnicos de planilha.
- Esse tipo de linguagem não serve para entrega final ao cliente porque expõe o processo interno em vez de vender e entregar transformação percebida.

Diagnóstico:

- O pacote FEO estava misturando duas camadas que precisam ficar separadas:
  - auditoria interna: rastreabilidade, origem do raciocínio, mecanismos, pesquisa e campos técnicos;
  - produto público: desejo da cliente, aplicação simples, progresso visível, primeira vitória e facilidade de uso.
- A causa-raiz não era apenas um texto ruim dentro do ZIP; era falta de gate forte na montagem final para impedir vazamento de linguagem técnica no material público.

Alternativas avaliadas:

1. Corrigir manualmente o ZIP do experimento 66.
   - Benefício: resolveria rápido o pacote atual.
   - Risco: o próximo pacote poderia voltar a vazar linguagem técnica.
   - Esforço: baixo.
   - Aderência ao objetivo de escala: baixa, porque não corrige o sistema.
2. Ajustar apenas o prompt de redação do FEO.
   - Benefício: melhora a geração inicial.
   - Risco: se algum artefato técnico chegar à montagem, ainda pode aparecer no produto final.
   - Esforço: médio.
   - Aderência ao objetivo de escala: média.
3. Corrigir a montagem final do FEO e adicionar bloqueio de termos técnicos no ZIP público.
   - Benefício: fecha a causa-raiz e protege os próximos experimentos.
   - Risco: exige reprocessar e validar o pacote final.
   - Esforço: médio.
   - Aderência ao objetivo de escala: alta.

Decisão:

- Seguir a alternativa 3.
- O FEO precisa gerar produto vendável para a cliente final, não relatório interno de construção.
- A linguagem pública deve ser orientada a desejo, facilidade, prazer, progresso e aplicação prática.

Correções aplicadas:

- A copy do e-book/HTML foi ajustada para linguagem direta de cliente.
- A montagem final passou a bloquear termos técnicos no ZIP público.
- A planilha passou a usar rótulos mais naturais:
  - `apoio_para_travar_menos`;
  - `quando_considerar_concluido`;
  - `pontos_de_atencao`.
- O pacote oficial do experimento 66 foi refeito pelo backend/FEO.

Validações registradas:

- Testes do módulo FEO executados com sucesso: `10` testes verdes.
- ZIP oficial baixado por `/api/experiments/66/deliverables.zip`.
- Varredura do ZIP em HTML, CSV e README não encontrou termos técnicos proibidos.
- Execução final FEO:
  - etapa: `montagem-pacote`;
  - `executionId=34`;
  - status: `COMPLETED`;
  - finalização: `2026-07-15T23:54:30Z`.

Conclusão comercial:

- O pacote FEO do experimento 66 passa a estar alinhado com entrega ao cliente final.
- O material deve comunicar transformação e uso, não engenharia interna do produto.
- A regra para próximos pacotes é clara: qualquer conteúdo público precisa parecer produto comprado pela cliente, não documentação de bastidor do Marketing Hub.

Status de PR:

- Nenhum PR foi criado ou preparado nesta etapa.
