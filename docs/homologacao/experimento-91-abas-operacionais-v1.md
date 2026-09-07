# Homologação das abas operacionais do experimento 91

## Objetivo e evidência inicial

- Gargalo corrigido: leitura operacional e instrumentação do experimento pago.
- Evidência: experimento `91` e campanha Meta estão `RUNNING`/`ACTIVE`. A Graph API já registrava 53
  impressões, 1 clique e R$ 6,98, enquanto o banco central permanecia em zero por erro HTTP 500 na
  sincronização; a tela mostrava campanha apenas como `Pronto`, run ausente como ação pendente,
  falhas Meta já recuperadas como atuais e recortes globais de dispositivo ao lado de zero sessões
  atribuídas.
- Métrica esperada: todas as áreas visíveis devem ser aplicáveis ao PDE, separar operação atual de
  preparação histórica e usar exclusivamente métricas atribuídas ao experimento.
- Continuar: campanha íntegra e ainda sem volume, aguardando eventos reais.
- Ajustar: erro não recuperado, divergência entre campanha/experimento ou evento atribuído que não
  percorra o funil.
- Parar: gasto real atingir o gate persistido sem avanço comercial ou a medição deixar de ser
  confiável.

## Alternativas avaliadas

| Alternativa | Benefício | Risco/custo | Decisão |
| --- | --- | --- | --- |
| Apenas renomear e reordenar as 14 abas | Mudança rápida | Mantém ferramentas incompatíveis, estados contraditórios e métricas contaminadas | Rejeitada |
| Ocultar as abas incompatíveis com PDE | Reduz ruído | Ainda deixa campanha, erros recuperados e histórico sem hierarquia operacional | Insuficiente |
| Navegação por fase, filtragem por tipo e correção dos agregadores | Coloca decisão atual primeiro, preserva auditoria e elimina leituras falsas | Exige frontend, backend e testes de contrato | Escolhida |

## Matriz de homologação definida antes dos testes

| Dimensão | Cenários obrigatórios | Critério de aprovação |
| --- | --- | --- |
| Caminho feliz | Abrir #91, navegar por Resultados, Campanha Meta, Saúde, Comportamento, Ativos, Planejamento e Auditoria | Conteúdo carrega, campanha/conjunto/anúncio aparecem `ACTIVE` e nenhuma ação de republicação é oferecida |
| Validações e falhas | Campanha ausente, run ausente antes/depois da publicação, integração Meta falha e falha seguida de sucesso | Empty states explicam a causa; run retroativo é bloqueado; apenas falha não recuperada gera atenção técnica |
| Integrações e observabilidade | Endpoints de experimento, readiness, campanha, funil, monitor PDE, processo e vídeo | Respostas são consumidas sem `404` opcional desnecessário e sem erro de console |
| Métricas | Entrega/gasto Meta, dados PDE atribuídos e tentativa Meta recuperada | 53 impressões, 1 clique e R$ 6,98 persistem no cenário corrigido; etapas não são somadas como outcomes; nenhuma venda é inferida |
| Segregação | Histórico global da v7 e origem UTM da campanha #91 | Dispositivo, resolução, qualidade e jornada globais não aparecem ao lado do resumo atribuído |
| Responsividade | Chromium desktop, iPhone 15 Pro e Pixel 7 | Grupos de abas acessíveis por rolagem horizontal, sem corte de comandos nem overflow da página |
| Segurança comercial | Navegação e testes locais | Nenhuma campanha, orçamento, preço, evento, compra ou dado produtivo é alterado |

## Resultado

- A navegação PDE ficou com 12 abas aplicáveis, agrupadas em Operação atual, Ativos,
  Planejamento e Auditoria. Landing tradicional, GeraLanding e teste A/B de página deixaram de
  aparecer no PDE; campanha Meta passou a ter leitura própria e o histórico de preparação deixou
  de parecer uma ação operacional pendente.
- A primeira matriz revelou que o bundle local podia herdar `NODE_ENV=production` e ignorar o proxy
  do Vite. O contrato passou a usar o `MODE` efetivo do Vite; a contagem das rodadas foi reiniciada
  após essa correção.
- Depois da última correção, duas rodadas locais completas e consecutivas passaram. Em cada rodada,
  o frontend concluiu 500 testes em 145 arquivos, typecheck e build produtivo; o backend concluiu
  2.368 testes, sem falha ou erro e com cinco testes explicitamente ignorados.
- As imagens locais de frontend e backend foram construídas duas vezes a partir dos Dockerfiles
  versionados. O frontend respondeu `healthz` e a rota `/experiments/91` dentro do container; o
  backend continha o jar executável íntegro. Nenhum container temporário permaneceu ativo.
- Duas matrizes visuais completas abriram as 12 abas em Chromium desktop, iPhone 15 Pro e Pixel 7:
  72 painéis ao todo, sem resposta HTTP de erro, erro de console, painel vazio, associação ARIA
  inválida, overflow da página ou consulta a GeraLanding/GeraSalesPage/PDE global incompatível. O
  vídeo terminou cada rodada com `readyState=4`, sem iframe cruzado e com analytics de teste
  desativado.
- Às 11:54 BRT, a Graph API mostrava campanha, conjunto e anúncio `ACTIVE`, 65 pessoas alcançadas,
  66 impressões, dois cliques, 23 visualizações de vídeo e R$ 13,22 de gasto. O PDE mantinha duas
  sessões/entradas atribuídas à campanha, sem login, paywall, checkout ou compra.
- A produção não foi alterada durante a homologação. Até o deploy pelo fluxo de PR, o backend atual
  continuará registrando HTTP 500 na sincronização e exibindo Meta zerado, apesar da entrega real.
  Nenhum orçamento, preço, campanha, anúncio, evento produtivo, pagamento ou venda foi criado ou
  alterado pelos testes.

## Alternativas para a falha da primeira sincronização

| Alternativa | Benefício | Risco/custo | Decisão |
| --- | --- | --- | --- |
| Enviar o token interno ao reset atual | Corrige o HTTP 401 rapidamente | Apaga todos os eventos do produto quando a campanha já recebeu tráfego real | Rejeitada |
| Tentar o reset novamente até passar | Mantém o desenho atual | Amplia indisponibilidade, pode duplicar custo e continua destrutivo | Rejeitada |
| Persistir Meta sem reset tardio e separar tráfego por identidade/UTM | Preserva a auditoria e destrava métricas oficiais | Exige contrato e testes entre cockpit e backend | Escolhida |
