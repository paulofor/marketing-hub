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
- preparar base para assinatura/continuidade;
- ser a única API consumida pelo frontend PDE;
- acessar banco de dados ou serviços internos quando isso for necessário para entregar a experiência PDE.

O backend PDE pode acessar dados persistidos diretamente ou por contratos internos definidos para o módulo, desde que preserve a fronteira de produto: o frontend PDE não deve conhecer nem consumir endpoints do backend principal `backend/ads-service`.

### Frontend PDE

O frontend PDE deve:

- exibir uma tela inicial de entrada/login antes da área de orientações;
- apresentar uma parte inicial da experiência guiada antes da compra;
- bloquear as partes principais da experiência até a compra do acesso;
- conduzir a cliente pelo diagnóstico;
- mostrar missões diárias;
- exibir progresso;
- disponibilizar biblioteca de apoio;
- reforçar promessa, transformação e próximos passos.

O frontend PDE deve consumir somente endpoints do próprio backend PDE, preferencialmente sob `/api/pde/...`, usando proxy local/deploy apontado para `pde-platform-backend`. É proibido o frontend PDE chamar diretamente endpoints do `backend/ads-service`, hosts do backend principal, endpoints administrativos do Marketing Hub ou APIs internas de outros módulos. Quando a tela PDE precisar de dados que hoje existam no `ads-service` ou em outro repositório, o contrato deve ser criado no backend PDE, e o backend PDE deve fazer a leitura, persistência ou integração necessária.

Essa fronteira deve ser validada automaticamente no CI do PDE frontend antes do build. A validação deve bloquear referências diretas a `ads-service`, ao host do backend principal `191.252.181.168`, à porta `8000` do backend principal ou a endpoints fora do contrato PDE.

### Funil comercial obrigatório Clube MUSA/PDE

O Clube MUSA/PDE deve usar um funil de entrada com login antes da compra e paywall interno.

Fluxo canônico:

```text
Anuncio
→ tela de login do Clube MUSA/PDE
→ entrada no sistema
→ visualizacao da parte inicial gratuita
→ bloqueio das partes mais importantes
→ oferta de compra do acesso
→ checkout
→ compra aprovada
→ liberacao do acesso completo
→ continuidade da experiencia guiada
```

Regras obrigatórias:

- os anúncios devem direcionar para a tela de login do Clube MUSA/PDE, não diretamente para checkout;
- o login libera somente a entrada no sistema e a parte inicial gratuita;
- o preço do acesso pago não deve aparecer em anúncio, experimento ou área pública quando a estratégia comercial for revelar o valor somente no momento em que a usuária solicitar a liberação das funcionalidades pagas dentro da área logada;
- a parte inicial deve gerar percepção de valor, diagnóstico, orientação ou amostra suficiente para criar desejo de continuidade;
- as partes mais importantes do produto devem permanecer bloqueadas até a compra do acesso;
- a compra aprovada libera o acesso completo ao produto PDE comprado;
- é proibido documentar ou implementar fluxo em que qualquer e-mail válido libere acesso completo sem compra;
- é proibido tratar o login como equivalente a compra, assinatura ou liberação total.

O objetivo comercial é transformar o anúncio em entrada de relacionamento, permitir que a lead veja valor dentro do sistema e vender o acesso quando ela quiser continuar nas partes de maior valor.

### Analytics obrigatório para campanhas PDE

Toda aplicação PDE usada como destino de campanha deve registrar eventos próprios no backend PDE antes de escalar tráfego pago. A medição mínima deve permitir reconstruir o funil por produto, campanha, origem e dispositivo.

Eventos mínimos:

- `PED_ENTRY`;
- `PAGE_VIEW`;
- `PAGE_LOAD`;
- `PAGE_VISIBLE_TIME`;
- `SECTION_VIEW`;
- `LOGIN_STARTED`;
- `LOGIN_COMPLETED`;
- `PAYWALL_VIEWED`;
- `SUBSCRIPTION_CLICKED`;
- `CHECKOUT_STARTED`;
- `SUBSCRIPTION_APPROVED`;
- `ACCESS_RELEASED`;
- `FIRST_USE`;
- `MISSION_OPEN`;
- `MISSION_COMPLETED`;
- `MATERIAL_OPEN`.

Metadados mínimos por evento quando disponíveis:

- `visitorId`;
- `sessionId`;
- `utm_source`, `utm_medium`, `utm_campaign`, `utm_content`, `utm_term`;
- URL da página;
- URL de referência;
- tipo de dispositivo;
- tamanho de tela e viewport;
- seção, ação ou material acionado;
- tempo visível quando o evento representar permanência.

O backend PDE deve persistir esses eventos em estrutura consultável e expor resumo agregado para decisão comercial. Logs técnicos não substituem analytics persistido. Antes de liberar nova campanha para o Clube MUSA/PDE, deve ser possível responder no mínimo: quantas pessoas entraram, quantas iniciaram login, quantas concluíram login, quantas viram paywall, quantas clicaram em assinatura, quantas iniciaram checkout, quantas tiveram compra aprovada, quantas receberam acesso e quantas fizeram primeiro uso.

## Contrato mínimo de produto

Cada produto PDE deve ter, no mínimo:

- `slug`;
- `name`;
- `promise`;
- `audience`;
- `priceLabel`, que pode ficar vazio enquanto a estratégia comercial não deve revelar preço antes da solicitação de acesso pago;
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
7. o anúncio apontar para a entrada/login do PDE em `https://clubemusa.com.br`, e o checkout existir somente no paywall interno ou na continuidade bloqueada;
8. produto da cliente não expuser termos técnicos internos.
9. funil e analytics do PED estiverem registrando eventos próprios de entrada, sessão, UTM, paywall, checkout, compra, liberação e ativação.

Para experimentos do tipo `PDE_MEMBERSHIP_SUBSCRIPTION_FUNNEL`, a prontidão de campanha não deve exigir GeraSalesPage v1 como página de venda tradicional. A validação correta é: contrato comercial completo, URL canônica do Clube MUSA/PDE, criativos prontos, segmentação publicável, checkout/webhook/acesso e experiência inicial/paga validados.
