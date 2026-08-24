# Matriz de homologação — Nova oportunidade PDE com benchmark Rigel v1

## Objetivo

Executar localmente o processo `pde-opportunity-discovery` v4 e decidir se existe uma nova
oportunidade de Produto Digital Experiencial com potencial documentado igual ou superior ao
benchmark interno de Rigel, fixado em 82/100. A nota é um instrumento de priorização; não representa
venda, receita nem validação comercial.

## Gargalo, métrica e decisão

- **Gargalo:** o catálogo ainda não possui uma nova oportunidade que combine evidência de dor,
  intenção de compra, diferenciação, entrega simples e distribuição inicial com qualidade igual ou
  superior a Rigel.
- **Evidência mínima:** exatamente três oportunidades distintas; ao menos dez ofertas pagas e
  deduplicadas no ciclo; duas vias independentes para recorrência, desatendimento e intenção de
  compra da vencedora; fonte científica aderente ao mecanismo quando houver alegação comportamental.
- **Métrica esperada:** vencedora com no mínimo 82/100, decisão `APPROVE` de Argos, Hermes, Dédalo e
  Psique e ausência de bloqueio econômico ou ético.
- **Continuar:** nota maior ou igual a 82, contratos completos, riscos controláveis e teste posterior
  possível sem mídia paga.
- **Ajustar:** sinal comercial plausível, mas uma lacuna de público, mecanismo, prova, canal ou
  economia ainda puder ser resolvida com pesquisa adicional.
- **Parar:** nota menor que 82, evidência fabricada ou duplicada, risco ético não controlável,
  sobreposição material com produto existente ou dependência de promessa sem prova.

## Evolução controlada das alternativas

A exploração inicial comparou reputação local ética, decisão de uma encomenda e cobrança sem
desgaste. As lacunas encontradas levaram a ciclos com fotografia de produto e retomada de estudos,
sem reduzir o benchmark. Antes da rodada final, o dossiê ativo passou a comparar exatamente:

1. **Pedido no Azul:** contribuição de um pedido de delivery já liquidado;
2. **Venda Líquida:** margem real de uma venda já liquidada em marketplace;
3. **Escopo Pago:** contenção e negociação de trabalho extra em projetos freelancer.

As três alternativas finais diferem em público, dor-raiz, dados de entrada, decisão entregue e
microexperiência. Nome ou variação de copy não constitui oportunidade distinta. Fontes de ciclos
anteriores permanecem preservadas em dossiê separado, mas não entram na contagem ativa.

## Matriz ponta a ponta

| Área | Cenário | Evidência esperada | Critério |
| --- | --- | --- | --- |
| Caminho feliz | Argos, Hermes, Dédalo, Psique e gate determinístico processam o mesmo dossiê | resultados estruturados, decisão, fontes, modelo, tokens e custo | todos aprovam e a vencedora atinge 82/100 |
| Pesquisa | fontes públicas, oficiais, acadêmicas, comunidades e ofertas comerciais são consolidadas | URL, tipo, data de acesso, sinal observado e limitação | nenhuma fonte inventada ou sem relação semântica |
| Ofertas | ofertas pagas comparáveis são contabilizadas por domínio e produto | ao menos dez ofertas únicas no ciclo e três por alternativa | repetição de página, diretório ou plano do mesmo produto não aumenta a contagem |
| Comparação | três oportunidades distintas são avaliadas | nomes preservados e oito critérios cuja soma resulta no total | maior total vence; empate usa segurança e depois ordem alfabética |
| Benchmark | vencedora é comparada com Rigel 82/100 | nota auditável, riscos e diferenças explícitas | nota menor que 82 não pode receber `APPROVE` final |
| Jornada | Hermes mapeia decisão assistida | perguntas, objeções, sinais de confiança e três rotas de distribuição | canal inicial atribuível e sem gasto automático |
| Formato | Dédalo compara três formatos por oportunidade | benefício, esforço, risco, custo e valor rápido | microexperiência não depende de integração ou operação contínua |
| Comportamento | Psique testa valor percebido, esforço e arrependimento | linguagem do cliente, objeções, confiança e limite ético | desejo não pode depender de vergonha, medo ou manipulação |
| Economia | intenção de compra é distinguida de uso gratuito e tamanho de mercado | ofertas pagas, faixa observada e hipótese de entrega | preço definitivo fica para Plano Comercial; margem não é inventada |
| Segurança | reputação local usa somente avaliações autênticas | proibição de review gating, avaliação falsa, incentivo e garantia de ranking | qualquer dependência dessas práticas rejeita a oportunidade |
| Falha de agente | resposta fora do schema, alternativa renomeada ou score inconsistente | erro explícito e saída preservada | gate não avança nem corrige silenciosamente a resposta |
| Integração | executor usa prompt e schema versionados e OpenAI Flex | request e response bruto separados do resultado funcional | nenhuma credencial ou JSON duplamente serializado no artefato |
| Observabilidade | cada execução registra correlação, modelo, status, erro, tokens e custo | manifesto local auditável por agente | consumo ausente é desconhecido, nunca convertido em zero |
| Métricas | score, parecer e intenção são segregados de resultado comercial | vendas, receita, gasto, contatos e publicação permanecem zero | somente pagamento reconciliado poderá contar como venda futura |
| Dados de teste | artefatos usam prefixo `LOCAL_QA` | diretório local isolado e nenhum callback produtivo | nenhuma métrica do Marketing Hub é alterada |
| Interface | nesta fase não existe artefato público ou jornada de cliente | dossiê legível e contratos válidos | desktop/mobile serão obrigatórios somente quando a oportunidade for materializada em processo posterior |

## Rodadas

A primeira rodada completa executará testes de contrato, pesquisa consolidada, todos os agentes e o
gate final. Como o executor atual já possui lacunas conhecidas em relação ao processo v4, qualquer
correção reinicia a homologação e exige duas rodadas completas e consecutivas sem falhas após a
última alteração.

## Resultado executado

Cinco defeitos de contrato, evidência ou execução foram encontrados antes das rodadas finais:

- Argos promoveu relatos de assinantes a ofertas pagas e alterou a contagem de 19 para 21. A
  contagem passou a ser calculada pelo executor e copiada literalmente pelo agente.
- Hermes aprovou a viabilidade da jornada quando Argos havia decidido `RESEARCH_MORE`. O prompt e o
  gate agora preservam a hierarquia da evidência e bloqueiam aprovação a jusante.
- A fonte acadêmica de Escopo Pago apontava para o DOI errado. O DOI foi corrigido para o artigo
  realmente usado, as 42 URLs ativas foram verificadas novamente e as rodadas anteriores foram
  descartadas como finais.
- O executor não possuía timeout próprio nem registrava explicitamente a URL do provedor. A chamada
  agora tem limite total configurável, repete somente falhas transitórias e preserva endpoint,
  request e response em artefatos separados.
- O gate final convertia um `REJECT` de agente em `RESEARCH_MORE`. A decisão mais restritiva agora é
  preservada e possui teste de regressão.

Após a última correção, 26 testes de contrato passaram e as duas rodadas completas seguintes
terminaram sem falha técnica:

| Rodada | Vencedora | Score | Psique | Gate final | Efeitos externos |
| --- | --- | ---: | ---: | --- | --- |
| final 1 | Pedido no Azul | 73 | 72 | `RESEARCH_MORE` | zero |
| final 2 | Pedido no Azul | 70 | 72 | `RESEARCH_MORE` | zero |

O gate de negócio parou corretamente: nenhuma oportunidade atingiu Rigel 82 e nenhuma foi cadastrada
como produto. A homologação do executor está concluída; a descoberta termina honestamente em
`PESQUISAR MAIS`, conforme o cânone, com o sinal e suas lacunas registrados em
`docs/marketing/descoberta-oportunidade-pde-2026-08-24.md`.
