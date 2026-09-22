# Homologação do Processo 5 de Capella — 22/09/2026

## Escopo e identidades

- Produto #7, Capella / Agenda Cheia Nail Design, tipo `LOW_TICKET_DIGITAL_PRODUCT`.
- Cadeia #18, Processo 5 v8 / definição #82, execução #13, referência `experiment:88`.
- Preparação Quartzo #81 concluída pela execução #14; Psique #474 e Têmis #475 aprovados.
- Publicação comercial vigente #31, HTML SHA-256
  `60038b023e828f44d76fcb993dae1b7b2adebe00f7a4cadf27fcb7b178dbf9c7`.
- Run histórico #1 citava a publicação #27 e o contrato funcional de leads.

## Matriz definida antes dos testes

| Dimensão | Casos | Aceite |
| --- | --- | --- |
| Caminho feliz | Preparação reconciliada, pareceres reutilizados, nova tentativa e quatro gates | Processo chega ao gate humano sem repetir IA nem gastar mídia |
| Versão | Publicação, hash ou impressão Quartzo diferente | Run anterior preservado; nova tentativa obrigatória |
| Validação | Referência completa, token parcial, publicação antiga e evidência ausente | Somente os três tokens exatos são aceitos |
| Falhas e retomada | Gate reprovado, run interrompido e nova publicação durante o preflight | Bloqueio acionável, histórico íntegro e retomada sem duplicar aprovação |
| Integrações | Landing, checkout, pagamento simulado, retorno, briefing, ZIP, e-mail e download | Identidades correlacionadas; nenhuma cobrança real ou SMTP externo |
| Observabilidade | Eventos, deduplicação, separação de QA e hashes | Testes não contam como venda; evidência persistida por gate/run |
| Economia | Preço R$ 67, 5 vendas-alvo, CAC esperado R$ 25, custo integral e teto | Contribuição projetada positiva; gasto continua dependente do aceite humano |
| Interface | Desktop, iPhone 15 Pro e Pixel 7 | Página atual utilizável; botão humano explica gasto, teto e janela reais |

## Critérios operacionais

A preparação e os pareceres comerciais podem ser reutilizados porque a consolidação #342 identifica
a publicação #31 e o contrato atual. A homologação técnica não pode reutilizar o run de agosto:
`RUNNING` descreve sua operação, não a validade dos pixels em setembro.

A tentativa atual deve registrar a referência
`publication:31;page-sha256:60038b023e828f44d76fcb993dae1b7b2adebe00f7a4cadf27fcb7b178dbf9c7;quartzo-fingerprint:<impressão atual>`.
Os demais gates precisam comprovar checkout/entrega simulados, Meta intencionalmente sem ativação e
eventos de QA segregados. Aprovação técnica não autoriza campanha nem comprova venda, satisfação ou
lucro.

## Resultado

Localmente, a suíte completa do backend aprovou 3.404 testes, com 21 cenários condicionais
ignorados, e as regressões específicas comprovaram a rejeição de publicação antiga, token parcial,
tentativa ativa obsoleta e teto de mídia diferente do máximo do plano. No frontend, 741 testes,
typecheck, build e 22 cenários específicos do painel foram aprovados; o único aviso foi o tamanho
de chunks já conhecido do build.

A execução produtiva permanece pendente até revisão, PR e deploy desta correção. Depois da versão
publicada, o pai deve reutilizar a preparação e os pareceres vigentes, criar um novo run de
homologação para a publicação #31 e permanecer no gate humano sem ativar campanha ou gastar mídia.
