# Experimento 66 - MUSA-H001-E004

## Diretriz operacional

- Objetivo comercial: transformar o experimento 66 em uma oferta low-ticket validável, com foco em vendas.
- Produto: `Método MUSA - Presença Elegante em 7 Dias`.
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

O experimento 66 não deve ser tratado como pesquisa de mecanismos nem como PDF isolado.

A direção escolhida é vender um kit aplicável:

**Método MUSA - Presença Elegante em 7 Dias**

Promessa:

> Monte em 7 dias uma presença mais elegante, marcante e coerente sem depender de luxo caro, compras impulsivas ou transformação radical.

CTA principal:

> Quero meu Kit MUSA por R$47

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

## Pendências para ficar pronto para tráfego

- Restaurar/semear templates ativos do GeraSalesPage v1.
- Rodar novamente o GeraSalesPage para gerar a página oficial.
- Auditar a página publicada.
- Confirmar que o destino da campanha aponta para a página auditada, não para o checkout direto.
- Validar fluxo de compra e entrega do produto digital.
- Só depois liberar Facebook Ads.
