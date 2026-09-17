# Vega #92 — recuperação da jornada comercial

Data: 2026-09-17

## Escopo preservado

- Produto: Vega, ID 4, tipo cadastrado Opala/PDE.
- Cadeia: 14; ciclo: 2; processo: 77 v1; execução: 8.
- Experimento: 92; versão: `musa-pde-entry-v12-primeiro-ajuste-aplicavel`.
- Destino: `https://v8.clubemusa.com.br`.
- Oferta: R$ 67 em pagamento único, checkout Pepper `owm6x`, acesso por 90 dias.

## Estado produtivo confirmado antes da publicação da correção

- O processo permanece com 5 de 8 atividades concluídas. Psique, Têmis e a consolidação final ainda
  não começaram.
- O experimento permanece `PLANNED`; não houve campanha, cobrança, acesso real ou gasto de mídia.
- A v8 serve a imagem da v12 no commit `62b0ae120f0c6f135b72f65cb8bcee7872721d07`, mas sua oferta
  ainda responde HTTP 503 porque a correção do preflight está somente na worktree.
- A prontidão confirma checkout, instrumentação, público e orçamento. Os gates pendentes são entrada
  homologada e criativo aprovado.
- A janela foi alinhada pelo comando oficial para 17–21/09/2026, preservando R$ 20/dia e o teto já
  autorizado de R$ 100, sem solicitar liberação de mídia.
- O criativo #529 preserva o #528 e atende aos limites de copy: título `Seu primeiro ajuste com o que
  já tem`, texto de 111/125 e descrição de 23/25 caracteres. A revisão permanece `FAILED` porque o
  callback produtivo ainda executa o código anterior.
- A telemetria operacional registra a execução de Têmis para o #529 como `COMPLETED`, mas o registro
  canônico ficou `FAILED`, sem modelo ou resposta persistidos; os logs confirmam a falha depois do
  início da revisão. Portanto, essa tentativa não pode ser convertida em aprovação retrospectiva.

## Causas-raiz e decisões

### Candidata PDE

1. Aceitar fallback local: baixo esforço, mas poderia servir contrato antigo e misturar v7/v12.
2. Publicar antes de homologar: desbloquearia o snapshot, mas eliminaria o gate que protege tráfego.
3. Expor preflight interno autenticado e exato: mantém o Hub como fonte de verdade e permite validar
   antes da promoção. Esta alternativa foi escolhida.

O Marketing Hub agora entrega experiência e oferta da candidata somente por produto, slot e versão
exatos, com token interno e estados `CANDIDATE`, `READY` ou `ACTIVE`. O PDE usa esse canal apenas
quando a rota pública recusa a versão explícita; não aceita fallback local.

### Parecer de Têmis

1. Herdar o plano do experimento #91/v7: rápido, porém comercialmente incorreto.
2. Descartar a correção de landing: esconderia uma reprovação real.
3. Preservar parecer e tarefa, delegando correção apenas com plano e subprocesso da mesma versão:
   mantém auditoria e isolamento. Esta alternativa foi escolhida.

O backend deixa de reverter o callback quando não há plano exato. O worker preserva modelo, request,
response, tokens e custo mesmo se o callback falhar, sem converter conteúdo reprovado em sucesso.

### Evidências paralelas

1. Validar só a maior revisão do produto: simples, mas ignora versões paralelas.
2. Reescrever manifestos históricos: faria hashes passarem, mas destruiria a auditoria.
3. Selecionar por produto + versão e criar novas atestações imutáveis: preserva história e valida
   cada candidata vigente. Esta alternativa foi escolhida para Psique, Têmis e o empacotador.

## Correções compartilhadas

- Preflight autenticado da experiência e oferta candidata no backend e no PDE.
- Revalidação de orçamento sem ultrapassar o teto total autorizado.
- Callback de Têmis resiliente à ausência legítima de plano do sucessor.
- Auditoria bruta preservada no fallback do worker.
- Manifestos atuais separados por produto e versão; v7, v12 e Rigel coexistem.
- Harness real inclui v8 candidata, contrato público recusado e preflight interno aceito.
- O teste público respeita o formato do produto: MUSA não precisa renderizar o card de serviço
  assistido antes do primeiro resultado.

## Homologação local

Duas rodadas completas consecutivas foram executadas sem alteração de código entre elas:

- Backend: 3.213 testes, zero falhas e zero erros por rodada; 17 ignorados previstos.
- PDE: 182 testes, zero falhas e zero erros por rodada.
- Têmis: 100 testes por rodada; zero falhas e zero erros; um ignorado previsto.
- Psique: 113 testes por rodada; zero falhas e zero erros; um ignorado previsto.
- MySQL 5.7: candidata v12, contrato publicado e consistência, 3/3 por rodada.
- Navegação: v5, v6, v7 e v8 em desktop, iPhone 15 Pro e Pixel 7; recusas 404/409 preservadas.
- Evidência comercial: 15/15 controles por rodada, incluindo imagem final, navegador, analytics,
  workflows, proxy e hashes empacotados.
- Builds, API boundary, isolamento de publicação, Liquibase estático, Actionlint, Spotless e diff:
  aprovados nas duas rodadas.

Registros locais: `artifacts/vega-92-journey/round-5` e
`artifacts/vega-92-journey/round-6`.

## Limite da homologação e sequência após publicação

Os testes comprovam que a jornada pode ser preparada sem cruzar versões e sem antecipar publicação.
Eles não aprovam o criativo, não comprovam vendas e não autorizam mídia.

Depois do PR e do deploy do backend, PDE, Psique e Têmis:

1. confirmar HTTP 200 da oferta v12 na v8;
2. solicitar uma única nova revisão do criativo #529;
3. se Têmis aprovar, executar a aprovação humana do mesmo ativo;
4. retomar o processo 77 para Psique, Têmis e consolidação;
5. executar a autorização humana de ativação e orçamento;
6. liberar a campanha e mudar para `RUNNING` somente após confirmação externa.

O ganho de vendas ainda é hipótese. As métricas de validação são primeiro resultado por início,
checkout por primeiro resultado, venda líquida, CAC, contribuição pós-CAC, falha entre pagamento e
acesso e satisfação pós-entrega.
