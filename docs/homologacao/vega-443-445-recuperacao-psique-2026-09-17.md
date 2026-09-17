# Vega — recuperação das tarefas Psique #443 a #445

Data: 17/09/2026

## Resultado

A superfície pública da Vega v12 foi atualizada e comprovada em desktop, iPhone 15 Pro e Pixel 7.
A atividade `Homologar experiência comercial` permanece bloqueada em 5/8 porque o worker publicado
ainda transporta o manifesto v2, cujo prompt excede o limite preventivo antes de abrir o modelo.

## Causas confirmadas

1. A tarefa #443 capturou a imagem antiga da v8 porque o push da `main` construiu a imagem, mas o
   deploy automático recebeu `frontend_version=none`; o smoke validou a v7 e não a superfície Vega.
2. O workflow direcionado `35275809292`, no commit
   `61144aee0eab34e620fd1a73b6d314ceb07589e2`, publicou a v8 e aprovou o smoke público específico.
3. As tarefas #444 e #445 passaram pelo preflight visual e persistiram seis novas capturas cada. A
   primeira dobra contém `Começar meu ajuste gratuito`, R$ 67 em pagamento único, 90 dias de acesso
   e ausência de assinatura ou renovação.
4. As duas tarefas pararam em `NOT_STARTED`, sem modelo e sem custo de IA, com 1.157.676 e
   1.157.691 caracteres. O manifesto v2 repetia integralmente arquivos amplos já atestados por hash.

## Alternativas avaliadas

- Aumentar o teto do prompt: rejeitado, pois apenas mascara um loop conhecido e reduz a margem
  operacional.
- Remover provas: rejeitado, pois enfraquece a auditoria comercial.
- Versionar o transporte: escolhido. O manifesto v3 preserva a v2 como baseline imutável, mantém
  contratos e testes comerciais integrais e usa `ATTESTED_REFERENCE` somente para artefatos amplos
  e redundantes. Pixels continuam anexados diretamente ao modelo.

## Homologação local

- Jornada Vega v12: 36/36 cenários Playwright em desktop, iPhone 15 Pro e Pixel 7.
- Customer Agent/Psique: 118 testes, zero falhas e um ignorado previsto.
- Meta Ad Approver/Têmis: 101 testes, zero falhas e um ignorado previsto.
- Empacotador de evidências: 13 testes, zero falhas; pacote idêntico para Psique e Têmis.
- Spotless, `bash -n`, ShellCheck, JSON e `git diff --check`: aprovados.
- Imagens locais construídas e inspecionadas:
  - `local/customer-agent-worker:vega-444`
  - `local/meta-ad-approver-worker:vega-444`

## Limite operacional

Não foi feita publicação do manifesto v3 nem nova tarefa após #445. Repetir com o worker publicado
reproduziria deterministicamente o mesmo bloqueio. A correção precisa passar por Pull Request e
pipeline antes da próxima retomada; depois disso, a tela deve abrir somente uma nova tentativa e
acompanhar Psique, Têmis e a consolidação até o objetivo persistido.
