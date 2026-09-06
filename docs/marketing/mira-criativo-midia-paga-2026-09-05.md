# Mira — criativo e aquisição por mídia paga

Decisão solicitada em 2026-09-05. Produto interno `10`, plano comercial `7`.

> **Substituição em 2026-09-06:** o recrutamento e as leituras privadas descritos abaixo são
> históricos e não devem ser executados. A regra vigente é homologação multiagente seguida de um
> experimento comercial próprio. Consulte
> [a revisão atual](revisao-cadeia-validacao-pde-multiagente-2026-09-06.md) e
> [o cânone](../canonical/pde-validacao-multiagente-canon.v1.md).

## Objetivo e mudança operacional

Construir aquisição pelo Instagram Ads que evolua para compras reais e entrega satisfatória.
Preparar criativo e jornada paga agora, em paralelo à validação do produto. Não depender de rede
pessoal, seguidores próprios ou convites que o usuário precise distribuir a conhecidas.

O primeiro investimento já autorizado é um teste gratuito para recrutar duas participantes
qualificadas: R$ 20/dia, teto R$ 100 e pausa ao atingir duas qualificadas ou o teto. A etapa seguinte
é um experimento separado de vendas, após utilidade, entrega, checkout e economia aprovados.
Não transferir silenciosamente orçamento de recrutamento para vendas nem interpretar preço
hipotético cadastrado como oferta liberada.

## Evidência que orienta a decisão

Consulta do frontend, APIs oficiais e MCP em 2026-09-05:

- O plano `7@v6` já declarava Instagram Ads, mas não tinha experimento vinculado.
- A consulta de `experiment` por `product_id=10` retornou zero registros. O contador agregado do
  plano mostrava um experimento criado; esse agregado não comprova a existência de campanha de Mira.
- A biblioteca `GET /api/planning/commercial-plans/7/visual-assets` retornou vazia, portanto não havia
  prova aprovada selecionável no Estúdio de Íris.
- O produto permanecia `PLANNED`, na etapa 3/6, “Protótipo, leituras privadas e aprovação do PDE”.
- O MCP do PDE confirmou cinco eventos `QA_INTERNAL`, de uma única identidade de QA, com
  `SIMULATED_NO_CHARGE`, e nenhuma leitura `PRIVATE_READING`. A classificação está em
  `metadata_json.trafficClass`; `traffic_quality=UNKNOWN` não transforma esses eventos em humanos.
- A mídia e a receita do plano estavam em R$ 0. Havia R$ 0,10 em IA no resumo financeiro do plano;
  esse valor não deve ser chamado de mídia nem de receita.
- O cadastro de Instagram continha somente `@produtividade360_`; as páginas cadastradas eram
  “Produtividade 360” e “50 Termos Tecnologia”. A aderência da identidade anunciante à proposta de
  cuidados pessoais ainda precisa ser resolvida antes da veiculação, sem impedir briefing.

Gargalo operacional: transformar a intenção de usar mídia paga em uma peça demonstrável e uma
jornada mensurável. A existência do protótipo não equivale à existência de prova visual aprovada
na biblioteca, nem a leituras humanas concluídas.

## Alternativas consideradas

| Caminho | Benefício | Risco | Esforço/custo | Decisão |
|---|---|---|---|---|
| Esperar as leituras para preparar toda a comunicação | Usa feedback humano no primeiro briefing | Prolonga a espera quando não há participantes | Baixo esforço imediato; alto tempo de espera | Não escolhido |
| Anunciar compra imediatamente | Testa intenção de pagamento diretamente | Jornada atual é privada e o checkout é simulado; entrega e economia ainda precisam de gate | Alto risco de gastar sem uma oferta entregável | Não escolhido neste orçamento |
| Preparar criativo e jornada paga em paralelo; recrutar e depois testar vendas | Adianta ativos, usa o canal comercial escolhido e mede cada etapa | Exige ligar qualificação, acesso e parada | Menor esforço inicial com peça estática; mídia limitada à autorização vigente | Escolhido |

## Briefing do primeiro criativo

**Estado:** briefing editorial preparado; não é imagem produzida, aprovada ou anúncio publicado.

- **Formato inicial:** imagem vertical 9:16 para Instagram Stories, com texto curto, contraste e CTA
  legíveis no celular. Sem dependência de locução, avatar ou geração de vídeo.
- **Público:** mulheres de 35 a 60 anos que usam ou consideram skincare e querem organizar os produtos
  em uma rotina simples. Qualificação voluntária, sem diagnóstico ou coleta de dados clínicos.
- **Identidade pública:** “Sua rotina, organizada com calma”. Não usar Mira, números de tarefas,
  versões internas ou Marketing Hub como marca do produto.
- **Ideia visual:** captura real e legível da rotina entregue pelo protótipo, com dados de
  demonstração e sem convites, tokens ou informações de participantes. Uma composição discreta
  pode apoiar a cena; não gerar uma interface fictícia como prova.
- **Título:** “Seus produtos de skincare. Uma rotina mais simples.”
- **Texto:** “Participe de um teste gratuito para organizar os produtos que você já tem, conforme as
  orientações dos rótulos. Experiência privada, sem compra e sem orientação médica.”
- **CTA:** “Quero participar do teste”.
- **Mensagem da página:** “Veja se este teste faz sentido para você. Após confirmar sua participação,
  você receberá um acesso individual para experimentar e contar como foi.”
- **Limites:** não prometer rejuvenescimento, cura, eliminação de manchas, tratamento ou segurança
  clínica; não inventar depoimentos, antes/depois, resultado humano, urgência ou prova de aprovação.
  As duas vagas correspondem à capacidade real do teste, não a uma oferta comercial fictícia.

## Sequência operacional no Marketing Hub

1. **Prova e direitos:** registrar na biblioteca uma captura fiel do protótipo com dados de
   demonstração, finalidade `PRODUCT_PROOF`, origem e direitos. Dédalo responde pela prova do produto;
   aprovação e pareceres reais devem permanecer auditáveis.
2. **Peça:** Íris recebe a referência aprovada e este briefing pelo Estúdio do plano. O arquivo entra
   como `DRAFT`; Psique avalia compreensão e Têmis avalia a peça real de forma independente.
3. **Jornada de recrutamento:** criar experimento próprio, página curta, consentimento, qualificação,
   acesso individual e confirmação de parada após duas qualificadas ou teto. O link privado isolado
   não substitui a página de anúncio nem essa integração.
4. **Identidade e homologação:** validar a identidade anunciante autorizada e a jornada mobile com
   eventos segregados. Campanha só pode iniciar quando esses controles estiverem funcionais.
5. **Mídia:** usar a autorização existente de R$ 20/dia e R$ 100 exclusivamente no recrutamento. Não
   pedir novamente a mesma autorização; não iniciar produção paga com custo desconhecido nem assumir
   que esses R$ 100 autorizam novas gerações Runway ou outro fornecedor.
6. **Receita:** depois das leituras e dos gates comerciais, abrir experimento de vendas separado.
   Medir compra aprovada, receita líquida, entrega, satisfação, reembolso e CAC. Novo gasto comercial
   depende de autorização específica; nenhum preço foi alterado nesta decisão.

## Métricas e decisões

| Etapa | Métrica real | Continuar | Ajustar ou parar |
|---|---|---|---|
| Criativo | Peça existente, prova e revisões independentes | Mensagem legível e fiel ao produto | Corrigir reprovação antes de mídia |
| Aquisição do teste | Visitas atribuídas, qualificadas distintas e custo por qualificada | Pessoas aderentes chegam e consentem | Cliques sem qualificação pedem revisão de mensagem/página |
| Acesso | Acessos individuais emitidos e experiências iniciadas | Cada qualificada consegue entrar | Falha de acesso ou privacidade pausa mídia |
| Utilidade | Leituras concluídas e respostas próprias, inclusive negativas | Uso e benefício observados | Qualificação sem uso ou leitura negativa exige diagnóstico |
| Limite financeiro | Gasto confirmado e confirmação de pausa | Dentro de R$ 20/dia e R$ 100 | Duas qualificadas, teto ou falha de mensuração interrompem |
| Venda futura | Compras aprovadas, receita, entrega, CAC e reembolso | Compra e entrega satisfatória com margem | Corrigir abandono; priorizar entrega se atrasada |

QA, recrutamento, leitura privada e vendas devem ter eventos e relatórios separados. Nenhum valor
estimado, criação de tarefa, briefing, imagem ou PR conta como venda.

## Verificação desta alteração

Escopo: estratégia, briefing e documentação; não foi criada funcionalidade de campanha. Antes de
salvar, conferir localmente limites textuais e preservação de orçamento, preço, prazo, metas e
vínculos. Depois de salvar pela tela, verificar versão imutável, texto persistido no banco e leitura
em Chromium desktop, iPhone 15 Pro e Pixel 7. Essa verificação não homologa a campanha ainda ausente.

A matriz ponta a ponta de recrutamento continua em
[Mira — recrutamento segregado no Instagram](../homologacao/mira-recrutamento-instagram-v1.md).

### Resultado da execução em 2026-09-06 UTC

- Plano atualizado pelo botão **Salvar planejamento**, com HTTP 200, às `00:02:39 UTC`; contexto
  oficial `commercial-plan:7@v7`, confirmado por consulta MCP às tabelas `commercial_plan` e
  `commercial_plan_version`.
- Dez campos editoriais conferidos após recarregar a tela, preservando orçamento R$ 100, preço
  cadastrado R$ 49, prazo 16/09, metas, status e ausência de vínculo a experimento.
- Objetivo, canal, oferta, prova, métrica, sucesso, parada, bloqueio e próxima ação estão no snapshot
  v7. As seis versões anteriores foram preservadas. Limitação do contrato publicado: `rootCause`
  aparece no plano atual, mas não é campo do snapshot; sua justificativa também está registrada
  neste documento, sem alegar que todos os campos atuais fazem parte da versão imutável.
- Chromium desktop, emulação iPhone 15 Pro e Pixel 7 confirmaram o texto salvo, os dez campos e
  ausência de overflow horizontal. Leitura e edição foram verificadas; não houve nova gravação
  durante a conferência nos três dispositivos.
- Limites textuais, links locais e diff revisados. Nenhum código executável, imagem Docker, preço,
  campanha, convite ou evento de participante foi alterado. Não houve commit, push, PR ou deploy.
- A entrega deste pedido é a estratégia e o briefing persistidos. A biblioteca permanece vazia:
  não foi produzido arquivo visual nem ativada campanha. Prova aprovada, identidade anunciante e
  jornada de qualificação/acesso/parada continuam sendo requisitos concretos da execução da mídia.

Fontes internas: [plano comercial](http://191.252.181.168:5173/planning/7),
[cadeia de valor](http://191.252.181.168:5173/products/10/value-chain-history),
[cânone comercial](../canonical/commercial-planning-canon.v1.md) e
[cânone da leitura privada](../canonical/mira-leitura-privada-canon.v1.md).
