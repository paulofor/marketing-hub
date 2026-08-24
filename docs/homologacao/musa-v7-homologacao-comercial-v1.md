# Matriz de homologação comercial — Vega / MUSA v7

## Escopo e decisão inicial

- Produto: `Vega` (`metodo-musa-7-dias`, produto `4`).
- Processo: `pde-commercial-homologation-activation` v4, etapa 5 de 6.
- Versão sob avaliação: `musa-pde-entry-v7-espelho-antes-de-sair`.
- Oferta congelada: pagamento único de R$ 67, acesso por 90 dias, sem renovação automática.
- Canal proposto para a primeira amostra: ativo próprio orgânico e base própria com consentimento.
- Trava: homologação não autoriza publicação, contato, mídia ou gasto.

O gargalo real é a ausência de uma homologação comercial versionada e de um experimento atual
vinculado à Vega. Os experimentos 66 e 67 são históricos encerrados, não possuem run atual e não
podem ser promovidos como evidência de prontidão da v7.

Foram comparadas três alternativas:

1. Reutilizar os experimentos 66/67: menor esforço, mas mistura versões e atribuição encerrada;
   rejeitada.
2. Repetir Construção e Comunicação: reduz incerteza por repetição, mas refaz gates já aprovados e
   não resolve o contrato específico de ativação; rejeitada.
3. Criar o contrato de homologação da v7, consumir as evidências anteriores, executar os gates de
   cliente/revisão independente e provar um run local segregado: maior rastreabilidade e aderência
   ao processo vigente; escolhida.

## Métrica e critérios

- Métrica esperada após ativação futura: cinco vendas líquidas reconciliadas, com contribuição
  positiva por venda.
- Continuar: checkout, acesso, primeira utilização, sete missões, entrega e mensuração permanecem
  íntegros; qualquer aquisição respeita autorização e atribuição.
- Ajustar: há visitas humanas e início da degustação, mas não ocorre momento de valor, paywall ou
  checkout; mudar uma variável por ciclo sem alterar silenciosamente a oferta.
- Parar: falha de privacidade, preço divergente de R$ 67, venda sem acesso/entrega, reembolso não
  reconciliado, dados de QA classificados como humanos, contribuição não positiva ou gasto sem
  autorização.

## Matriz ponta a ponta

| Área | Caminho feliz | Validações e falhas | Evidência exigida |
| --- | --- | --- | --- |
| Identidade | Produto 4, Vega e versão v7 permanecem correlacionados | versão, produto ou preço divergente bloqueia | contrato versionado e API do produto |
| Oferta | promessa, pagamento único de R$ 67 e acesso por 90 dias são compreensíveis | recorrência implícita, promessa garantida ou preço oculto bloqueiam | página, paywall e parecer independente |
| Degustação | quatro escolhas produzem microvalor local antes da compra | percurso neutro, entrada inválida e ausência de valor não viram sucesso | jornada E2E e eventos segregados |
| Checkout | CTA abre o checkout canônico com produto, BRL e R$ 67 | URL insegura, valor/produto divergente ou indisponível bloqueiam | smoke público e contrato de pagamento |
| Pagamento e acesso | pagamento de teste idempotente libera acesso por 90 dias | assinatura inválida, duplicidade, moeda/valor errado e replay bloqueiam | MySQL 5.7, callback e auditoria financeira |
| Entrega | cliente retoma acesso, conclui sete missões e abre materiais protegidos | token ausente/expirado, material público ou etapa perdida bloqueiam | workspace, eventos e proteção dos arquivos |
| Privacidade | coleta categorial mínima, exportação, correção e exclusão funcionam | texto livre persistido, vazamento e exclusão incompleta bloqueiam | API, banco e jornada local |
| Instrumentação | `TASTING_STARTED` até `REFUND_CONFIRMED` têm correlação e deduplicação | QA contado como humano, duplicação ou evento sem origem bloqueiam | resumo analítico antes/depois e banco |
| Canal | canal direto/orgânico fica pronto sem campanha ou orçamento ativo | Meta ou contato é ativado implicitamente | run local e gate de distribuição |
| Observabilidade | health, versão, logs e IDs permitem reconstruir a execução | diagnóstico HTML no lugar de JSON, 5xx ou ausência de correlação bloqueiam | health, diagnóstico, logs e manifesto |
| Economia | custo variável máximo de R$ 20 preserva contribuição positiva | custo desconhecido ou contribuição não positiva bloqueiam | contrato comercial e gate independente |
| Dispositivos | jornada utilizável em Chromium desktop, iPhone 15 Pro e Pixel 7 | overflow, corte, CTA inacessível ou erro de recurso bloqueiam | Playwright e screenshots de falha quando houver |

## Rodadas

Depois da última correção funcional e da normalização de formato, duas rodadas locais completas e
consecutivas terminaram sem falhas. Cada rodada comprovou:

- 1.774 testes do backend principal, com uma ignorada; 116 do backend PDE; 32 de Psique e 58 de
  Têmis, totalizando 1.980 testes Java sem falhas;
- build TypeScript/Vite, Prettier e 16 testes visuais em desktop e mobile;
- nove jornadas ponta a ponta em Chromium desktop, iPhone 15 Pro e Pixel 7;
- duas aplicações, rollback e reaplicação do Liquibase no MySQL 5.7;
- versão e saúde exatas dentro da topologia, e-mail somente em `@sandbox.local`, hashes das
  evidências íntegros e diff sem erro de whitespace.

As jornadas mantiveram compra, acesso, entrega e reembolso de QA na auditoria bruta como
`INTERNAL_QA`, enquanto venda, receita, contato e gasto humanos permaneceram em zero.
