# Vega — experiência de estilo pessoal v15

Complemento da entrega de posicionamento de 25/10/2026. A conferência encontrou uma
lacuna executável: a v14 não recebe preferência de estilo e suas missões ainda
privilegiam estrutura, formalidade e elegância. A Big Idea v2, sozinha, não altera
esse comportamento. A versão histórica continua preservada.

## Decisão e escopo

Comparadas três opções: reescrever v14 (quebra histórico), trocar apenas a copy
(não personaliza) ou entregar v15 isolada (escolhida). O endereço v10 estava reservado,
com TLS válido e sem slot de produto; a preparação criou a candidata #12 pela tela. A nova candidata reutiliza backend,
persistência, acesso e contratos, com imagem/container/porta próprios.

A entrega mantém R$ 67, compra única, Dia 1 gratuito, Dias 2–7 e materiais por 90 dias.
Não cria ciclo, herda aprovação financeira, muda experimento #106 ou inicia mídia.
As regras locais são declaradas e não representam uma chamada de modelo. Sua
personalização precisa de preferência explícita e somente das peças informadas.

## Matriz definida antes dos testes

| Percurso | Critério de aceite |
| --- | --- |
| Primeiro valor | Ocasião, roupa disponível e preferência; resultado antes de e-mail/compra |
| Personalização | Mesmas peças/ocasião, preferências distintas produzem ações e razões pertinentes |
| Autonomia | Manter como está e ainda descobrir o gosto são respostas válidas; sem estética universal |
| Validações | Ausência, categoria desconhecida, excesso, campos estranhos e versão cruzada rejeitados |
| Recuperação | Falha mantém entrada; consulta do resultado não gera outra execução; retomada após reinício |
| Continuidade | Sete dias distintos, escolhas anteriores aproveitadas, fórmula reutilizável e exportação |
| Acesso | Dia 1 gratuito, demais dias/materiais protegidos; expiração/reembolso e acesso cruzado bloqueados |
| Comércio | Sem checkout herdado; compra simulada local e vínculo exato, nenhuma compra real |
| Observabilidade | Versão, preferência, entradas anteriores e resultado auditados; custos separados |
| Segregação | E-mail sandbox e dados QA não contam como vendas, demanda ou margem |
| Regressão | v14 e outras identidades continuam com seus contratos, acesso e resultados |
| Dispositivos | Chromium desktop, iPhone 15 Pro e Pixel 7 emulados; sem overflow e com teclado/rótulos |

## Harness e medida de aprendizado

A melhoria exige prova comportamental de preferência → orientação, não apenas menção
à personalização em prompt. Os agentes recebem a Big Idea v2 e um contrato executável
com entradas e evidências da versão. O teste preventivo contrasta preferências e
preserva o caminho neutro. Conversão, uso e contribuição são hipóteses a medir
posteriormente; testes locais não comprovam ganho comercial.

## Resultado

- 227 testes do backend PDE passaram, incluindo seis regressões de estilo, isolamento,
  autorização, recuperação e sete dias. Quatro testes do contexto de planejamento passaram.
- Nove percursos Playwright da v15 passaram em Chromium desktop, iPhone 15 Pro e Pixel 7,
  com MySQL 5.7 real, SMTP descartável e pagamento simulado local. Dezoito regressões da
  v14 passaram nos mesmos dispositivos; nenhum gateway de pagamento real foi acionado.
- 35 contratos de runtime/publicação e 25 testes do contrato de release passaram.
- Três verificações adicionais da resposta em andamento passaram nos mesmos dispositivos;
  campos bloqueados durante a resposta impedem que uma orientação seja atribuída a outra escolha.
- A matriz Docker de proxy/isolamento e a homologação transacional de publicação e reversão
  passaram na engine local, usando apenas imagens construídas pelos Dockerfiles do repositório.
- Build TypeScript/Vite aprovado. Scripts alterados passaram em `bash -n` e ShellCheck.
- A candidata foi registrada pelo frontend como slot v10, registro #12, CANDIDATE,
  sem `sourceExperimentId` ou contrato publicado. A Big Idea v2 (#5) permanece vigente.
- Os dados sandbox não entram nas sessões humanas/compras. Respostas locais usam
  `MUSA_PERSONAL_STYLE_RULES_V1`, custo incremental de inferência zero, sem afirmar
  custo operacional integral zero. Capturas e recibos locais são evidências técnicas.

A publicação do software será conferida pelo PR e pela identidade de runtime. Revisões
independentes, economia e autorização do próximo experimento permanecem separadas;
nenhuma homologação comercial, campanha ou aumento de vendas é declarado.

## Compatibilidade das provas compartilhadas

Sete manifestos vigentes apontavam hashes de fontes compartilhadas alteradas nesta
entrega. A validação local bloqueou corretamente esses hashes. As atestações
sucessoras registram os arquivos revalidados, preservam os manifestos anteriores e
não publicam outras superfícies nem renovam pareceres. A suíte PDE completa, os
contratos de planejamento, as regressões v14, o proxy de todas as superfícies e os
contratos de release sustentam a compatibilidade. Regras antigas de elegância
continuam restritas às versões antigas; a mudança de foco pertence à v15.

As capturas anexas usam entradas explicitamente simuladas. Elas documentam layout
e interação; não representam avaliação comercial ou cliente real. A implantação
mantém a candidata sem checkout publicado e sem associação automática ao #106.
