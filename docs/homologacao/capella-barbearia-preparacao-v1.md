# Capella — preparação de barbearia v1

Escopo autorizado em 03/10/2026: preparar a expansão, testar localmente, registrar nova economia
e preparar um novo ciclo/experimento. Não inclui análise paga, produção paga ou campanha. Capella
continua Quartzo. A candidata `barber-v1` não substitui `nails-v1`, #88 ou #94.

## Matriz de aceite antes dos testes

| Cenário | Evidência exigida | Limite da evidência |
| --- | --- | --- |
| Contrato anterior | Kit de unhas conserva quantidade, identidade, suporte e arquivos | Regressão técnica, não nova aprovação comercial |
| Candidata barbearia | Briefing e repertório de corte/barba; 10 posts, 10 stories, 10 legendas, 5 mensagens e calendário de 7 dias | Imagens sintéticas de QA não comprovam fotografias comerciais |
| Isolamento visual | Biblioteca por profissão, manifesto de segmento, hashes e dez imagens distintas; ausência ou troca bloqueia | Não aprovar o acervo de unhas para outro mercado |
| Entradas e falhas | Perfil desconhecido recusado; textos públicos sanitizados; biblioteca incompleta ou alterada bloqueada antes de envio | Não recorrer a geração paga como fallback |
| Pagamento e histórico | Referência comprada define profissão; candidata sem checkout aprovado não é vendida pelo percurso legado | Sem cobranças, vendas ou reescrita de #88/#94 |
| Integração | HTTP, persistência, download e e-mail simulados, incluindo recuperação e idempotência | SMTP de sandbox/test doubles; nenhum cliente real |
| Interface | Chromium desktop, iPhone 15 Pro e Pixel 7: contexto, campos, botão final e estados oficiais legíveis | Navegadores emulados, não Safari físico |
| Observabilidade | Identidade de perfil no manifesto interno; rastreabilidade da tentativa e bloqueio; custos ausentes explícitos | Custo não medido não vira zero |
| Economia e ciclo | Nova revisão com lacunas reais, sucessor planejado e ciclo próprio; sem orçamento herdado | Janela/amostra comerciais aguardam análise e autorização |
| Rascunho administrativo | Experimento sem meta de conversão inventada; plano novo sem entrevistas por padrão | O preflight continua exigindo a economia e as metas necessárias à venda |

## Alternativas de implementação

1. Trocar textos/acervo do kit atual: pouco esforço, mas mistura versões e altera obrigações.
2. Duplicar o produtor para barbearia: isolamento inicial, mas duplica correções e aumenta manutenção.
3. Reutilizar compositor e contratos por profissão: esforço intermediário, protege históricos e bloqueia
   acervo incompatível. Escolhida; só o contrato de conteúdo e o acervo são especializados.

## Lacuna observada no harness

O produtor e a porta fotográfica assumiam unhas sem transportar a profissão. Trocar o público
do anúncio não adaptaria a entrega; um teste com mock genérico poderia mascarar o uso de fotos
de unhas em barbearia. Perfis versionados e manifesto de acervo por perfil tornam essa diferença
verificável antes de produção. Não criar novo agente: Atena conserva estratégia, Dédalo a entrega,
Plutus a economia e os gates existentes a aprovação.

Também foi observada uma comparação numérica que aceitava `NaN` como nota fotográfica.
A biblioteca agora exige nota finita, hashes distintos e identificação do novo segmento.
As regressões abrangem o caso original e casos válidos, sem exceção por produto ou pagamento.

Na preparação administrativa, a tela impedia salvar um rascunho sem conversão projetada,
embora o endpoint oficial aceite esse campo ausente. A tela passa a preservar o desconhecido;
o preflight não foi alterado. Planos novos também deixaram de trazer cinco conversas solicitadas
como meta padrão, seguindo o cânone de validação voluntária pelo mercado.

A edição financeira avançada copiava a classificação de geração com IA sem permitir conferi-la.
Ela agora permite revisar suporte e IA por cliente no contrato existente. A composição local
do kit não faz chamadas pagas por compra; a criação inicial do acervo permanece custo separado
e desconhecido. A regressão preserva a revisão histórica e os custos sem fonte.

## Pendências comerciais reais

A biblioteca comercial de barbearia ainda precisa de imagens próprias/licenciadas, rastreabilidade
e revisão dos agentes. O teste usa dados e imagens sintéticos segregados. Preço de R$ 67 é hipótese
de manutenção, não novo parecer. Não herdar R$ 13,50, CAC R$ 25, R$ 73,20 nem tetos R$ 100/R$ 125
como economia comprovada da candidata. Registrar produção, suporte, armazenamento, aquisição,
reembolsos e custos dos pareceres; desconhecido permanece pendente. A preparação técnica não
comprova demanda, vendas, utilização, margem nem lucro.

## Resultado da rodada local

- Pagamentos: 61 testes Java aprovados; após ajustar uma chamada que podia anunciar corte a
  quem informou apenas barba, os cinco testes da candidata foram repetidos com sucesso.
- Pós-compra: nove testes JavaScript aprovados, incluindo profissão enviada pelo backend,
  ausência de contrato e estados de recuperação.
- Frontend administrativo: 838 testes aprovados na rodada completa, complementados pelas
  regressões da edição financeira, type-check e build concluídos.
- Aplicativo empacotado: oito cenários aprovados, com fluxo legado completo em três dispositivos,
  bloqueios de pagamento/candidata e formulário da candidata em três dispositivos.
- Preparação administrativa: três cenários de navegador aprovados; cada dispositivo salva
  rascunho sem metas inventadas e plano sem entrevistas, usando somente APIs simuladas.
- Script fotográfico existente: `bash -n` e ShellCheck aprovados; geração não foi executada.

As imagens, e-mails e pagamentos foram sintéticos e segregados em `artifacts/capella-expansion`
(ignorado pelo Git). Nenhuma chamada paga foi usada. Chromium emula dispositivos; não representa
homologação em Safari físico. Acervo comercial, custo integral, parecer financeiro e ciclo
operacional com limites próprios continuam gates explícitos.
