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
- **Marketing Hub é a fonte de verdade para mudanças comerciais do PDE** quando a experiência estiver em campanha ou experimento ativo.

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
- pacote científico operacional versionado quando o produto usar artigos ou evidências externas para sustentar mecanismo, prova ou orientação por IA.

### Atualização comercial de experiências PDE

Toda mudança comercial em um PDE usado para campanha, experimento ou monitoramento pós-deploy deve ser feita pelo Marketing Hub, no contrato versionado da experiência do produto, e não diretamente no frontend/backend do `pde-platform`.

Entram nessa regra:

- primeira dobra;
- promessa;
- CTA;
- diagnóstico;
- quantidade de opções;
- ordem das etapas;
- copy de paywall;
- materiais de apoio;
- missões;
- qualquer microinteração usada para medir interesse.

O objetivo é preservar a associação entre versão da experiência, campanha, eventos de funil, painel pós-deploy e decisão comercial. Alterações diretas no código do PDE só são permitidas para capacidade técnica genérica do motor, correção de bug, tracking, integração, performance, segurança ou componentes reutilizáveis que não representem uma variação comercial específica do produto.

Cada contrato PDE publicado pelo Marketing Hub deve declarar uma versão comercial explícita da experiência, como `experienceVersion`, `funnelVersion` ou campo equivalente canônico. Essa versão deve mudar sempre que a alteração puder afetar conversão, interesse ou comportamento do usuário. Eventos de analytics enviados pelo frontend PDE devem carregar essa versão nos metadados persistidos para permitir comparar resultados por versão sem misturar tráfego antigo e novo.

O campo canônico para comparação automática é `experienceVersion`. O campo `funnelVersion` agrupa a arquitetura comercial maior do funil. O backend PDE deve persistir `experienceVersion` em coluna própria dos eventos de funil, mantendo os metadados como apoio auditável, para permitir consulta simples por SQL e painel pós-deploy.

Quando uma alteração de PDE for publicada, o relatório/painel deve separar pelo menos:

- produto;
- experimento/campanha quando disponível;
- versão da experiência;
- data/hora de publicação;
- eventos de entrada, clique, login, paywall, checkout e compra;
- decisão comercial tomada para aquela versão.

Se a versão da experiência não estiver disponível nos eventos, a comparação deve ser considerada incompleta: pode indicar tendência por janela de tempo, mas não deve ser usada como prova limpa de melhora ou piora entre formatos.

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
- criar, persistir e expor solicitações de orientação por IA quando a experiência precisar de personalização guiada.
- receber resultados de workers de IA com saída funcional estruturada, payload bruto, modelo, tier, tokens, custo quando houver e erro.

O backend PDE pode acessar dados persistidos diretamente ou por contratos internos definidos para o módulo, desde que preserve a fronteira de produto: o frontend PDE não deve conhecer nem consumir endpoints do backend principal `backend/ads-service`.

### IA direcionada no PDE

A IA no PDE deve funcionar como personalização guiada por etapa, nunca como chat aberto genérico na experiência inicial.

Regras obrigatórias:

- o backend PDE é fonte de verdade de acesso, contexto, pendência, status, resultado e auditoria;
- a chamada OpenAI deve ser executada por worker próprio ou módulo executor, não pelo frontend;
- o worker deve consumir pendências pelo endpoint canônico `pending` do backend PDE;
- prompt operacional e schema JSON de saída devem ficar versionados no worker;
- quando o produto possuir pacote científico operacional, o backend PDE deve entregá-lo no contrato `pending` e o worker deve injetá-lo no prompt como base de plausibilidade, limites e linguagem permitida;
- para chamadas MUSA, o worker deve falhar antes da OpenAI se o pacote científico operacional estiver ausente ou incompleto, evitando orientação genérica sem apoio dos artigos definidos para o produto;
- a resposta deve ser curta, estruturada e diretamente aplicável à missão;
- o frontend deve exibir a orientação como cartão de produto, não como conversa livre;
- toda solicitação deve ser mensurável no funil e associada ao token, produto e missão.

Para o Método MUSA, a Consultora MUSA deve atuar nos 7 dias como orientação guiada por missão: a cliente preenche três sinais ou respostas práticas do dia e recebe um cartão curto, aplicável e coerente com o histórico da jornada. O Dia 1 pode ser usado como amostra gratuita de valor; os Dias 2 a 7 permanecem como parte do acesso completo quando o funil estiver em modo de paywall interno.

Para o Método MUSA, a Consultora MUSA deve usar o `musa-evidence-pack-v1` como bastidor científico. O pacote apoia microações sobre roupa, cor, acabamento, postura, coerência visual e peça-sinal, mas a resposta visível não deve virar citação acadêmica recorrente nem promessa absoluta. A linguagem deve preservar o desejo de presença elegante acessível e evitar afirmações como garantia de elegância, mudança universal de percepção externa ou transformação de personalidade.

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

### Jornada Persuasiva Interativa do PDE

A Jornada Persuasiva Interativa do PDE deve ser lida como **funil experiencial por estágios comerciais**, não como AIDA simples.

O modelo AIDA pode ser usado como apoio psicológico dentro de cada estágio, mas a unidade principal de análise deve ser o avanço comercial real do consumidor:

1. **Contato com a promessa**: anúncio e primeira dobra fazem a pessoa reconhecer a dor/promessa e aceitar entrar.
2. **Envolvimento diagnóstico**: questionário e plano/amostra gratuita aumentam informação, valor percebido e desejo pela continuidade.
3. **Compromisso de continuidade**: login, cadastro, plano salvo, primeira missão ou ação equivalente transformam interesse em intenção mensurável.
4. **Conversão comercial**: paywall, clique de assinatura, checkout e compra transformam intenção em receita.
5. **Validação pós-compra**: acesso liberado, primeiro uso, missão concluída e materiais abertos confirmam que a promessa vendida começou a ser aplicada.

O contrato `persuasiveJourney` publicado pelo Marketing Hub deve declarar esses estágios de forma versionada, com função comercial, mudança esperada no usuário, seções/eventos rastreados, métrica principal e regra de otimização quando o estágio quebrar. O relatório do experimento deve usar essa jornada para responder em qual estágio a pessoa perdeu confiança, desejo ou disposição de pagar.

### Analytics obrigatório para campanhas PDE

Toda aplicação PDE usada como destino de campanha deve registrar eventos próprios no backend PDE antes de escalar tráfego pago. A medição mínima deve permitir reconstruir o funil por produto, campanha, origem e dispositivo.

Eventos mínimos:

- `PED_ENTRY`;
- `PAGE_VIEW`;
- `PAGE_LOAD`;
- `PAGE_VISIBLE_TIME`;
- `SECTION_VIEW`;
- `PRESENCE_MAP_CHOICE_SELECTED`;
- `DIAGNOSTIC_CHOICE_SELECTED`;
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

### Health check público obrigatório por PDE

Todo PDE produzido para campanha, experimento ou tráfego pago deve publicar um contrato público de health comercial em `GET /pde-health-contract.json`.

Esse contrato deve declarar, no mínimo:

- `slug` do produto;
- `healthPath` da entrada pública que recebe o tráfego;
- `requiredTexts` com pelo menos headline, bloco principal da oferta/diagnóstico e CTA primário;
- `forbiddenTexts` com mensagens de erro técnico ou tela branca que nunca podem aparecer para a cliente.

O pipeline de deploy deve executar o smoke test público depois da publicação usando esse contrato. A validação mínima obrigatória é:

- `GET /healthz` retorna status operacional;
- a página pública responde com HTTP válido;
- o JavaScript principal carrega;
- o primeiro elemento renderizado em `#root` fica visível;
- todos os textos comerciais críticos do contrato aparecem na tela;
- nenhum texto proibido aparece no corpo da página;
- não existem erros fatais de execução capturados pelo navegador.

Um PDE não pode ser considerado pronto para tráfego se o smoke test público falhar, mesmo quando o status HTTP estiver 200. O objetivo é bloquear recorrência de tela branca, assets misturados entre ambientes, bundle JavaScript quebrado ou primeira dobra errada antes de gastar mídia.

### Controle de versões produtivas pelo Marketing Hub

O Marketing Hub deve ser o painel operacional para decidir qual versão PDE recebe tráfego e qual versão cada experimento mede.

GitHub Actions verde não é prova suficiente de publicação produtiva. Antes de liberar tráfego pago ou considerar uma correção publicada, o Marketing Hub deve confirmar:

- URL pública produtiva respondendo;
- versão comercial da experiência PDE publicada;
- slot produtivo correto no card do produto;
- experimento vinculado à versão que será medida;
- jornada principal validada em desktop e mobile;
- eventos aparecendo no painel pós-deploy depois do tráfego real.

### Publicação versionada simultânea de PDE

Um mesmo produto PDE pode ter múltiplas versões produtivas simultâneas para teste controlado, como `v5.clubemusa.com.br` e `v6.clubemusa.com.br`.

Regras obrigatórias:

- cada versão pública deve ter subdomínio próprio, slot próprio no Marketing Hub e `experienceVersion` própria;
- o deploy produtivo do PDE deve rodar automaticamente em `main` quando houver alteração versionada do `pde-platform`, mantendo `workflow_dispatch` apenas como acionamento manual adicional;
- o mesmo motor pode servir múltiplos subdomínios, desde que frontend e backend resolvam a experiência pelo hostname versionado antes de qualquer override global de runtime;
- nenhum deploy pode ser considerado pronto se `v5` e `v6` entregarem o mesmo `experienceVersion` por engano;
- quando a versão depender de vídeo, o smoke test deve validar que o asset público esperado retorna arquivo real de vídeo, nunca HTML fallback;
- a validação pós-deploy deve cobrir cada subdomínio versionado com health público, renderização, endpoint PDE, diagnóstico público, versão esperada e asset crítico esperado;
- eventos de funil devem persistir `experienceVersion`, permitindo comparar v5 e v6 sem misturar tráfego, criativo ou jornada.

Para o Clube MUSA, a regra operacional atual é:

- `v5.clubemusa.com.br` deve servir `musa-pde-entry-v5-video-explicativo` com `/assets/musa-v5-video-explicativo.mp4`;
- `v6.clubemusa.com.br` deve servir `musa-pde-entry-v6-video-motivacional` com `/assets/musa-v6-video-motivacional.mp4`.

Quando houver hipóteses, criativos ou primeiras dobras concorrentes, a operação deve criar slots produtivos paralelos em vez de depender de ambiente intermediário. A tela de experimento apenas escolhe a versão medida; criação, manutenção e publicação das URLs ficam no fluxo do produto e no pipeline versionado do repositório.

### Slots produtivos versionados do PDE

O Marketing Hub deve permitir múltiplas URLs produtivas de PDE para o mesmo produto quando houver hipóteses, criativos ou primeiras dobras concorrentes em tráfego pago.

O modelo canônico é um **slot produtivo PDE** persistido no backend principal com:

- `slotCode`, como `v1`, `v2` ou código comercial equivalente;
- `productSlug`;
- `domain`, como `v1.clubemusa.com.br`;
- `publicUrl` usada no anúncio;
- `experienceVersion` servida naquele endereço;
- `targetEnvironment` esperado pelo pipeline;
- `status` operacional;
- experimento de origem quando existir.

Slots produtivos existem para separar aprendizado comercial e reduzir risco operacional. Um teste novo não deve obrigar a troca global de `clubemusa.com.br` quando for possível publicar uma variação em subdomínio próprio, mantendo eventos por `experienceVersion`, URL de anúncio explícita e histórico de campanha rastreável.

Quando a versão comercial for numerada, o slot produtivo deve usar o subdomínio correspondente à versão, como `v5.clubemusa.com.br` para a versão 5. O domínio raiz pode existir como entrada institucional, legado ou redirecionamento, mas não deve ser a URL primária de uma campanha que mede uma versão específica.

O Marketing Hub pode cadastrar e acompanhar slots antes da automação completa de infraestrutura. A publicação real continua proibida por SSH manual: o deploy deve ser feito por workflow, Compose, Dockerfile ou pipeline versionados do repositório.

Quando uma mesma imagem/deploy do frontend PDE servir mais de um subdomínio versionado, o hostname público do slot é a fonte decisiva da versão comercial exibida. Overrides globais de runtime podem existir para ambientes não versionados, preview ou rollback operacional, mas não podem fazer `v6.clubemusa.com.br` registrar ou renderizar a experiência da `v5`, nem o inverso. O frontend deve resolver `experienceVersion` e ativo obrigatório de vídeo pelo hostname quando ele corresponder a um slot produtivo versionado conhecido.

O workflow oficial de publicação do `pde-platform` deve validar cada slot produtivo versionado ativo ou pronto, no mínimo `https://v5.clubemusa.com.br` e `https://v6.clubemusa.com.br` enquanto ambos existirem. A validação pós-deploy precisa provar health público, renderização da entrada, contrato público e jornada diagnóstica em cada subdomínio, porque um único smoke test no domínio raiz não comprova teste simultâneo de versões.

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
- `scientificEvidencePack`, quando existir base científica operacional para IA, prova, materiais ou orientação;
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
7. o anúncio apontar para a entrada/login do PDE em slot produtivo versionado aprovado, como `https://v5.clubemusa.com.br` para a versão 5, e o checkout existir somente no paywall interno ou na continuidade bloqueada;
8. produto da cliente não expuser termos técnicos internos.
9. funil e analytics do PED estiverem registrando eventos próprios de entrada, sessão, UTM, paywall, checkout, compra, liberação e ativação.
10. health check público comercial estiver publicado e passando com os textos críticos do PDE.

Para experimentos do tipo `PDE_MEMBERSHIP_SUBSCRIPTION_FUNNEL`, a prontidão de campanha não deve exigir GeraSalesPage v1 como página de venda tradicional. A validação correta é: contrato comercial completo, URL versionada do Clube MUSA/PDE, criativos prontos, segmentação publicável, checkout/webhook/acesso e experiência inicial/paga validados.
