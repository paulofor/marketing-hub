# Descoberta PDE B2C para Instagram — ciclo de 2026-08-26

## Decisão

O processo local `pde-opportunity-discovery` v5 terminou em **PESQUISAR MAIS**. A vencedora,
**Entrevista sem Branco**, ficou entre 75 e 78/100 nas duas rodadas finais, abaixo do Rigel,
82/100. A nota é priorização interna, não previsão de venda.

Não foi criado produto, experimento, oferta, checkout, campanha ou ativo público. Contatos, compras,
vendas, receita, mídia e publicações permaneceram em zero.

## Gargalo corrigido

O processo anterior conseguia premiar uma oportunidade B2B bem documentada sem confirmar a
adequação ao canal real. A Descoberta agora exige, quando o canal for Instagram:

- comprador pessoa física, cena pessoal e desejo concreto;
- primeiro valor demonstrável no celular em até dez minutos;
- gancho visual específico, sem transformar curiosidade em demanda;
- jornada atribuível de `IMPRESSION` a `CHECKOUT_STARTED`;
- valor autônomo, sem dependência de parceiro, consultor ou operação empresarial;
- rejeição de curso genérico, B2B disfarçado e comunicação manipulativa.

Foram comparadas três alternativas de implementação: apenas mudar o prompt, filtrar B2B antes do
score ou integrar curadoria e gate B2C/Instagram ao processo completo. A terceira foi escolhida por
proteger público, produto, canal e métrica no mesmo contrato.

## Comparação final

| Oportunidade | Rodada 1 | Rodada 2 | Conclusão |
| --- | ---: | ---: | --- |
| Entrevista sem Branco | 75 | 78 | melhor hipótese; pesquisar mais |
| Inglês da Próxima Conversa | 66 | 70 | demanda ampla, mas concorrência e substitutos próximos |
| Antes da Conversa Difícil | 62 | 65 | apelo emocional com risco de segurança e privacidade |
| Rigel | 82 | 82 | benchmark não superado |

## Melhor hipótese: Entrevista sem Branco

**Público:** brasileiros de 18 a 29 anos com entrevista marcada nos próximos sete dias e dificuldade
para transformar a própria experiência em resposta clara.

**Dor:** um levantamento brasileiro com cerca de 21 mil jovens reportou ansiedade ou nervosismo em
41,51%, receio de não demonstrar capacidade em 26,61% e dificuldade para falar de pontos fortes e
fracos em 18,61% ([Observatório do Estado de Goiás](https://oportunidades.go.gov.br/observatorio/ansiedade-antes-de-entrevistas-de-emprego-e-barreira-para-os-jovens/2025/10/)).
O dado quantifica a situação, mas não comprova compra.

**Mecanismo:** a pessoa informa apenas o tipo de vaga e uma pergunta, grava uma resposta de até 90
segundos e visualiza onde faltaram contexto, ação, evidência ou síntese. Depois grava uma segunda
versão. Um estudo com 202 participantes e replicação com 156 candidatos sustenta o treino
estruturado e indica efeito desprezível para prática sem estrutura
([Journal of Vocational Behavior](https://www.sciencedirect.com/science/article/pii/S0001879123000726)).

**Microvalor:** comparar duas tentativas em até oito minutos, sem currículo, nome, envio automático,
resposta inventada ou assistência durante a entrevista.

**Sinal de compra adjacente:** além de Yoodli, Huru e Big Interview, existem ofertas brasileiras
ativas como [Candidatei](https://candidatei.app.br/) a R$ 29,90/mês,
[Treina Entrevista](https://www.treinaentrevista.com.br/) a partir de R$ 19,90 por três simulações e
[Recruta AI](https://recrutaai.ia.br/) a R$ 44,90/mês. Esses preços demonstram ofertas comparáveis,
não transações do nosso formato.

**Instagram:** a Meta anunciou links de produto em Reels no Brasil
([Meta](https://about.fb.com/br/news/2026/03/apresentando-uma-nova-era-de-descoberta-de-produtos-impulsionada-por-ia-e-criadores/)).
O caminho é tecnicamente atribuível, mas a consulta da Ads Library para esta categoria falhou por
falta de permissão da aplicação. Não foi encontrada evidência própria de conversão do gancho.

## Por que não superou Rigel

- intenção de compra ficou em 11/20: há concorrentes e preços brasileiros, mas nenhuma transação
  atribuída ao formato de uma pergunta;
- diferenciação ficou em 5/10: simuladores por voz nacionais já entregam experiência próxima;
- distribuição ficou entre 7 e 8/10: a cena cabe em Reels, porém o gancho ainda não foi testado;
- Psique ficou em 73–74/100: liberar microfone, ouvir a própria voz e recear julgamento ainda
  competem com a promessa;
- ensaio sozinho, feedback de conhecido e simuladores gratuitos continuam alternativas fortes.

## Inspirações utilizadas

Todos os artigos atuais de `pesquisas/ia-aplicada` e `pesquisas/gartner` foram relidos no início de
cada ciclo. Os padrões mais úteis foram microaprendizado mobile, objetivo explícito, interpretação
do dado da própria pessoa e uma próxima ação concreta. O snapshot Hotmart mais recente estava
degradado com títulos-placeholder; por isso foi usado o último snapshot nominal somente como
inspiração. `Novo EiB`, `Serviço LinkedIn Profissional` e ofertas de relacionamento não foram
tratados como vendas, e promessas de controlar o parceiro foram registradas como limite negativo de
copy.

## Execução e aprendizado

- Argos, Hermes, Dédalo e Psique executados localmente em modo Flex com requests, responses, tokens
  e custo auditados por ciclo;
- uma execução exploratória revelou o gate incorreto; após a correção, duas rodadas completas e
  consecutivas terminaram sem falha;
- custo Flex estimado das três execuções: US$ 0,99214820;
- nenhum score foi elevado por temperatura Hotmart, seguidores, alcance, página de preço ou parecer
  favorável;
- a melhor próxima evidência seria um protótipo privado, sem publicação, que meça consentimento de
  voz, conclusão das duas tentativas, melhora percebida e preferência frente ao ensaio gratuito.

Enquanto esses sinais não existirem, Entrevista sem Branco deve permanecer como hipótese de pesquisa
e não consumir a fábrica de produto antes de Rigel.

## Integração Meta preparada localmente

Em 2026-08-26, a Descoberta passou a solicitar por ciclo a cobertura real da categoria no Instagram.
Argos registra país, plataforma e termos no plano; o backend cria ou reutiliza um acompanhamento no
radar MOIS e devolve contagens de anúncios aderentes, ativos, anunciantes e atualidade. Essas métricas
ficam separadas das dez ofertas pagas comparáveis e não são interpretadas como venda.

O preflight operacional confirmou que os tokens atuais possuem `ads_read`, mas a Meta rejeita a
consulta real a `ads_archive` com `code=10` e `error_subcode=2332002`. Por isso, o modo automático
permanece bloqueado até a autorização externa do aplicativo. No Brasil, a coleta continua pelo fluxo
oficial supervisionado da Biblioteca pública; falha de autorização nunca é convertida em ausência de
anúncio ou de mercado.
