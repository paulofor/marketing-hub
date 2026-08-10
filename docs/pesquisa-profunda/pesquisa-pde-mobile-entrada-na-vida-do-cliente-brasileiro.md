# Pesquisa PDE Mobile: a porta de entrada para a vida do cliente brasileiro

Data da pesquisa: 2026-08-10  
Escopo: produtos PDE (Produtos Digitais de Experiência) voltados ao consumidor brasileiro  
Decisão recomendada: **URL mobile-first como entrada, web app instalável como evolução e app
nativo somente quando o comportamento real justificar**.

## Pergunta de negócio

Qual formato permite que um PDE entre mais facilmente na rotina do cliente: aplicativo ou
experiência acessada por URL?

A pergunta não deve ser respondida apenas pela tecnologia. Para o PDE, a melhor porta de
entrada é a que reduz o esforço entre o desejo e o primeiro valor percebido. A instalação
deve ser consequência de valor e recorrência, não uma condição para experimentar o produto.

## Resposta executiva

Para a maioria dos novos PDEs do Marketing Hub, a melhor estratégia é:

1. o cliente chega por um link profundo aberto pelo anúncio, busca, QR Code, mensagem ou
   indicação;
2. recebe uma microexperiência útil imediatamente no navegador, sem instalação obrigatória;
3. cria ou confirma sua identidade somente quando isso for necessário para salvar ou
   continuar o resultado;
4. depois de perceber valor e demonstrar intenção de voltar, recebe a opção de instalar a
   experiência na tela inicial;
5. um aplicativo nativo só é criado quando retenção, frequência ou recursos exclusivos do
   aparelho demonstrarem que ele produzirá valor adicional mensurável.

Portanto, a escolha não é simplesmente **site ou app**. O caminho mais eficiente é uma
**experiência web progressiva**: começa como URL sem atrito e pode ganhar comportamento de
app quando o relacionamento amadurece.

## Evidências sobre o comportamento mobile no Brasil

### O celular é o ambiente principal

- Em 2024, 89,1% das pessoas com 10 anos ou mais usaram a Internet; entre os usuários, 98,8%
  acessaram pelo celular. Mensagens chegaram a 90,2%, vídeos a 88,5%, redes sociais a 84,2%
  e serviços bancários a 71,2%. Isso favorece jornadas iniciadas em links compartilháveis e
  desenhadas para a tela pequena. Fonte: [IBGE, PNAD Contínua TIC 2024](https://biblioteca.ibge.gov.br/visualizacao/livros/liv102193_informativo.pdf).
- A TIC Domicílios 2024 encontrou uso do celular por 100% dos usuários de Internet
  pesquisados. Também mostrou que 40% acessavam apenas pelo telefone celular. A dependência
  exclusiva do celular é maior nas classes DE
  (68%) e entre pessoas com menor escolaridade. Fonte: [Cetic.br, TIC Domicílios 2024](https://cetic.br/media/docs/publicacoes/2/pt-br/20251027170648/tic_domicilios_2024_livro_eletronico.pdf).
- A qualidade da conexão não é uniforme. Entre usuários das classes DE, 37% acessavam pelo
  celular apenas via Wi-Fi e 57% combinavam Wi-Fi e rede móvel. A Anatel também registrou
  que falta de franquia e Wi-Fi impediu parte relevante da população de usar serviços
  financeiros, públicos, educacionais e de saúde. Fontes: [Cetic.br, principais resultados](https://www.cetic.br/media/analises/tic_domicilios_2024_principais_resultados.pdf)
  e [Anatel, Conectividade Significativa](https://www.gov.br/anatel/pt-br/assuntos/noticias/anatel-divulga-pesquisa-de-conectividade-significativa-com-foco-em-dispositivos-habilidades-e-franquia-de-dados).

### O brasileiro já compra e contrata serviços digitalmente

- Entre usuários da Internet, 40% pagaram por streaming de filmes ou séries, 35% pediram
  refeições, 44% solicitaram transporte por aplicativo e 10% contrataram cursos pagos pela
  Internet em 2024. Fonte: [Cetic.br, indicador H13](https://www.cetic.br/pt/tics/domicilios/2024/individuos/H13/expandido/).
- Entre quem comprou online, o Pix foi citado por 84% em 2024. O dado favorece uma jornada
  web curta, compatível com checkout ou redirecionamento móvel, sem transformar a instalação
  em obstáculo anterior à compra. Fonte: [Cetic.br, TIC Domicílios 2024](https://cetic.br/media/docs/publicacoes/2/pt-br/20251027170648/tic_domicilios_2024_livro_eletronico.pdf).

### A web pode evoluir para uma presença semelhante a app

- Uma PWA pode ser instalada a partir do navegador, ter ícone na tela inicial, aparecer na
  busca do aparelho e abrir em janela própria, sem exigir inicialmente um pacote de loja.
  Fonte: [web.dev, instalação de PWA](https://web.dev/learn/pwa/installation).
- No iOS 16.4 ou posterior, web apps adicionadas à tela inicial podem receber Web Push e
  usar badges, desde que o usuário conceda permissão. Fonte: [Apple Developer, Web Push](https://developer.apple.com/documentation/usernotifications/sending-web-push-notifications-in-web-apps-and-browsers).
- Existem diferenças entre navegadores e limitações importantes: a instalação pode não
  funcionar dentro dos navegadores internos de redes sociais. O PDE deve funcionar bem pela
  URL antes de depender de instalação e explicar a transição para o navegador quando
  necessário. Fonte: [web.dev, Progressive Web Apps](https://web.dev/learn/pwa/progressive-web-apps).

## Três alternativas avaliadas

| Alternativa | Benefícios | Riscos | Custo/esforço | Aderência ao PDE |
| --- | --- | --- | --- | --- |
| App nativo desde o início | Presença na tela inicial; push e integração profunda com o aparelho; boa experiência para uso frequente | Instalação antes do valor; dependência de lojas e revisões; duas plataformas; aquisição e atualização mais caras | Alto | Baixa para validar um PDE novo; alta apenas quando recorrência e recursos nativos já foram provados |
| Site responsivo por URL | Entrada imediata por anúncio, mensagem, QR Code ou busca; compartilhável; atualização centralizada; uma base multiplataforma | Menor presença recorrente; limitações offline e de recursos; pode parecer apenas uma landing se a experiência for rasa | Baixo a médio | Alta para descoberta, demonstração, compra e primeira entrega |
| URL mobile-first com evolução para PWA | Mantém a entrada sem instalação e permite tela inicial, janela própria, cache e notificações compatíveis após o valor | Compatibilidade varia; instalação no iPhone exige orientação; não substitui todo recurso nativo | Médio | **Muito alta**: combina baixa fricção inicial com continuidade e identidade de produto |

## Decisão recomendada para o portfólio PDE

Adotar **URL primeiro, instalação depois do valor** como padrão inicial.

O PDE deve ser construído como produto mobile-first, e não como uma página desktop adaptada.
A URL pública precisa abrir diretamente na etapa prometida pelo anúncio ou indicação. O
cliente deve compreender a transformação, fornecer a entrada mínima e receber o primeiro
resultado antes de ser convidado a instalar.

A arquitetura deve ficar preparada para PWA, mas o convite de instalação só deve aparecer
em um momento de alta intenção, por exemplo:

- depois de concluir o primeiro diagnóstico ou microresultado;
- ao salvar um plano personalizado;
- ao iniciar uma jornada de vários dias;
- depois da segunda visita ou quando existir benefício claro de retorno;
- quando notificações tiverem utilidade concreta, nunca apenas para marketing.

O app nativo deve ser tratado como expansão de um PDE vencedor, não como requisito para
descobrir se o produto gera valor.

## Quando um app nativo passa a ser a melhor opção

O investimento em iOS/Android deve ser avaliado quando houver evidência de pelo menos um
destes fatores:

- uso recorrente forte, como rotina diária ou várias sessões por semana;
- retenção comprovada e base de clientes suficiente para justificar manutenção;
- valor dependente de câmera avançada, sensores, Bluetooth, execução em segundo plano,
  biometria, integração profunda com saúde ou outro recurso não atendido adequadamente pela
  web;
- necessidade offline extensa e confiável;
- distribuição pela loja produzir confiança ou descoberta relevante para aquele público;
- economia incremental ou aumento de retenção superar o custo permanente das duas lojas.

Push sozinho não é justificativa suficiente, pois web apps instaladas já cobrem parte desse
caso. Preferência interna da equipe também não é evidência de comportamento do cliente.

## Princípios de experiência PDE mobile

- **Valor antes de cadastro pesado:** pedir apenas o dado necessário para produzir a primeira
  transformação; explicar por que qualquer informação adicional é necessária.
- **Um objetivo por tela:** evitar formulários extensos, menus complexos e excesso de texto.
- **Continuidade por link:** cada retorno deve levar à etapa certa da experiência, sem obrigar
  o usuário a reconstruir contexto.
- **Leveza:** otimizar imagens, vídeo, fontes e JavaScript; oferecer fallback quando a rede
  estiver instável e preservar o progresso.
- **Entrada por canais cotidianos:** anúncios, WhatsApp, redes sociais, e-mail, QR Code e busca
  devem abrir links profundos com atribuição preservada.
- **Identidade visível de produto:** a experiência por URL não deve parecer uma landing
  promocional depois da entrada; deve entregar interação, personalização, progresso e
  resultado.
- **Instalação contextual:** apresentar o benefício concreto de instalar, sem bloquear o uso
  pelo navegador.
- **Privacidade e confiança:** permissões, notificações e dados pessoais devem ser solicitados
  no momento de uso e com explicação simples.

## Matriz de validação antes de escolher app

Cada PDE deve medir o funil separando navegador e experiência instalada:

| Etapa | Métrica principal | Sinal para continuar pela URL/PWA | Sinal para avaliar app nativo |
| --- | --- | --- | --- |
| Aquisição | abertura válida por clique e custo por visitante qualificado | links convertem sem perda material | loja aparece repetidamente como origem de confiança ou descoberta |
| Ativação | conclusão do primeiro valor/microresultado | instalação prévia reduz ativação ou não acrescenta valor | recurso nativo é indispensável para completar o valor |
| Retorno | D1, D7, D30 e sessões por usuário | recorrência ainda baixa ou episódica | recorrência alta e estável, com benefício claro de acesso rápido |
| Instalação PWA | convite exibido, aceito e uso posterior | PWA atende o retorno e notificações | limitações da PWA causam abandono comprovado |
| Receita | checkout iniciado, venda aprovada, receita e reembolso | web fecha a venda e entrega satisfatoriamente | app aumenta retenção/receita líquida acima do seu custo total |
| Qualidade | velocidade, erros, perda de progresso e suporte | experiência é estável em navegadores-alvo | falhas são causadas por limites reais da plataforma web |

Os eventos devem ser segregados por produto, versão PDE, experiência, origem, navegador,
sistema operacional e modo `browser`, `standalone/PWA` ou `native`. Eventos de teste devem
ser identificados e excluídos das métricas comerciais.

## Critérios de continuar, ajustar ou parar

- **Continuar com URL/PWA:** quando ativação, compra e retorno evoluírem sem limitação relevante
  da web.
- **Ajustar a experiência:** quando houver tráfego suficiente, mas o abandono se concentrar
  antes do primeiro valor, em login, permissão, instalação, paywall ou checkout.
- **Experimentar app nativo:** quando dados de recorrência e limitações técnicas demonstrarem
  potencial incremental, com hipótese, orçamento e métricas definidos.
- **Parar o investimento em app:** quando a instalação diminuir a ativação, a retenção não
  compensar o custo ou o mesmo valor puder ser entregue com menor esforço por URL/PWA.

Não existe número universal que aprove um app para todos os PDEs. O baseline deve ser a
versão web do próprio produto, e a decisão deve usar comportamento real do seu público.

## Aplicação imediata no Marketing Hub

Para novos PDEs:

1. lançar a primeira experiência como URL mobile-first;
2. instrumentar abertura, primeiro valor, cadastro, retorno, instalação, checkout e venda;
3. testar em Chrome Android, Safari iPhone e navegadores internos dos canais de aquisição;
4. tornar a experiência instalável quando houver jornada recorrente;
5. oferecer instalação somente após valor percebido;
6. revisar a decisão com dados D1, D7 e D30;
7. propor app nativo apenas com evidência de ganho incremental ou requisito técnico real.

Essa estratégia é coerente com a natureza do PDE: a IA permanece como força invisível e o
cliente encontra uma experiência simples, útil e acessível no canal que já está usando.

## Limites desta pesquisa

- Os levantamentos nacionais comprovam centralidade do celular e hábitos digitais, mas não
  medem diretamente a preferência de cada público PDE entre URL, PWA e app nativo.
- Dados agregados do Brasil não substituem testes por nicho, oferta, canal e faixa de renda.
- Compatibilidade de PWA muda por sistema e navegador e deve ser novamente verificada antes
  de implementar capacidades específicas.
- A recomendação é uma estratégia inicial a ser validada por eventos reais, não uma promessa
  de conversão.

## Fontes principais

- [IBGE — PNAD Contínua TIC 2024](https://biblioteca.ibge.gov.br/visualizacao/livros/liv102193_informativo.pdf)
- [Cetic.br — TIC Domicílios 2024](https://cetic.br/media/docs/publicacoes/2/pt-br/20251027170648/tic_domicilios_2024_livro_eletronico.pdf)
- [Cetic.br — microdados e metodologia TIC Domicílios 2024](https://cetic.br/pt/arquivos/domicilios/2024/domicilios/)
- [Anatel — Pesquisa de Conectividade Significativa](https://www.gov.br/anatel/pt-br/assuntos/noticias/anatel-divulga-pesquisa-de-conectividade-significativa-com-foco-em-dispositivos-habilidades-e-franquia-de-dados)
- [web.dev — instalação e capacidades de PWA](https://web.dev/learn/pwa/installation)
- [Apple Developer — Web Push para web apps](https://developer.apple.com/documentation/usernotifications/sending-web-push-notifications-in-web-apps-and-browsers)
