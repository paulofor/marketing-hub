# PDE Platform Canon v1

## Objetivo

A PDE Platform é o motor reutilizável de **Produto Digital Experiencial** do Marketing Hub.

Ela deve entregar produtos digitais como experiência guiada, não como pacote de arquivos soltos. O objetivo comercial é aumentar valor percebido, reduzir esforço do consumidor e transformar princípios científicos do MDS/research em aplicação prática, simples e vendável.

## Decisão arquitetural

O padrão obrigatório é:

- **um motor PDE multi-produto**;
- **frontend React/Vite independente**;
- **backend Java 21 + Spring Boot 3 + Maven independente**;
- **Docker próprio** para execução isolada;
- **conteúdo por produto em configuração/contrato**, não em front/back exclusivo;
- **checkout externo automatizável**, começando por Pepper como caminho preferencial;
- **Marketing Hub/FEO fabrica o pacote PDE**;
- **PDE Platform entrega experiência, acesso, progresso e materiais**.

Não criar um front/back novo para cada produto, salvo exceção explícita e registrada.

## Responsabilidades

### Marketing Hub/FEO

O FEO fabrica o pacote PDE com:

- nome, promessa, público e transformação;
- diagnóstico;
- missões guiadas;
- exemplos visuais;
- e-book de apoio;
- checklists, templates e materiais baixáveis;
- imagens, capa e infográficos;
- metadados comerciais para página de venda.

### Checkout externo

O checkout externo deve:

- vender o produto/oferta;
- processar pagamento;
- enviar webhook de compra aprovada;
- permitir automação por API sempre que possível.

Pepper é a preferência inicial para automação. Mercado Pago pode ser fallback.

### Backend PDE

O backend PDE deve:

- receber webhooks de compra aprovada;
- liberar acesso por produto e e-mail;
- expor catálogo do produto;
- controlar diagnóstico e progresso;
- registrar missões concluídas;
- expor biblioteca de materiais;
- preparar base para assinatura/continuidade.

### Frontend PDE

O frontend PDE deve:

- apresentar a experiência guiada;
- conduzir a cliente pelo diagnóstico;
- mostrar missões diárias;
- exibir progresso;
- disponibilizar biblioteca de apoio;
- reforçar promessa, transformação e próximos passos.

## Contrato mínimo de produto

Cada produto PDE deve ter, no mínimo:

- `slug`;
- `name`;
- `promise`;
- `audience`;
- `priceLabel`;
- `theme`;
- `diagnostic`;
- `missions`;
- `supportMaterials`;
- `completionOffer`.

## Regra de qualidade comercial

O produto visto pela cliente não pode expor linguagem interna como:

- `FEO`;
- `experimento`;
- `CTR`;
- `CPL`;
- `lead`;
- `checkout`;
- `score`;
- `JSON`;
- `sha256`;
- `promessa validada`;
- `mecanismo validado`.

Os princípios científicos devem aparecer como:

- decisão guiada;
- microação;
- exemplo visual;
- checklist;
- campo preenchível;
- evidência de progresso;
- redução de esforço.

## Experimento 66

O experimento 66 deve usar a PDE Platform como primeira instância:

- Produto: `Método MUSA - Experiência Guiada de 7 Dias`;
- Formato: experiência guiada + e-book + checklists + templates;
- Checkout preferencial futuro: Pepper;
- Fallback existente: Mercado Pago;
- Entrega: área PDE do Marketing Hub.

## Critério de pronto

Um produto PDE só pode ser considerado pronto para tráfego quando:

1. checkout real estiver configurado;
2. webhook de compra aprovada estiver validado;
3. acesso da cliente estiver liberando corretamente;
4. experiência guiada estiver carregando;
5. materiais de apoio estiverem disponíveis;
6. progresso estiver persistindo ou registrado de forma auditável;
7. página de venda apontar para o checkout correto;
8. produto da cliente não expuser termos técnicos internos.
