# Vega v17 — clareza após o primeiro resultado

## Evidência e causa

A homologação #844 passou em nove cenários. Psique #845 reconheceu utilidade na
orientação contextual, mas pediu ajuste: download, confirmação de entendimento,
registro de uso e continuidade tinham peso semelhante. O benefício adicional da
jornada não aparecia junto ao convite. O parecer custou US$ 0,328646 estimados;
permanece preservado, com suas limitações de avaliação sintética.

Comparadas três alternativas: trocar apenas os rótulos, reorganizar as ações
existentes, ou remover feedback/continuidade. Escolhida a reorganização: mantém
os contratos e os sinais sem exigir confirmação de uso para avançar. A tela
destaca salvar a escolha, explica a reflexão como intenção possível, apresenta
os registros reutilizáveis e separa feedback opcional. Condições de R$ 67,
pagamento único, primeiro dia gratuito e ausência de cobrança permanecem.

A mudança da apresentação segue `CHANGE_PER_CYCLE_V1`: v17 e ciclo sucessor
próprios, preservando a v16, o parecer #845 e o ciclo #16. O backend compartilha
somente a capacidade de regras, mantendo sessão, operação, direitos, ciclo e
versão separados. Nenhuma autorização financeira ou aprovação é herdada.
A oferta, o público, o preço e a Big Idea permanecem como referências explícitas.
A nova candidata precisa dos seus próprios aceites, sem reescrever os anteriores.

## Matriz antes da validação

| Caso | Aceite | Ambiente |
|---|---|---|
| Primeiro valor | Orientação persistida, um próximo passo principal e download opcional | Chromium desktop, iPhone 15 Pro e Pixel 7 emulados |
| Continuidade | Benefício concreto e condições acessíveis antes de reflexão/feedback | Mesmos três dispositivos |
| Avanço | Dia 1 conclui com intenção declarada, sem confirmação de uso, download ou compra | Backend real + MySQL 5.7 local |
| Recuperação | Reabrir o dia e recarregar preservam a mesma operação | Mesmos três dispositivos |
| Direitos | Dia 2 continua protegido no trial | Backend real + MySQL local |
| Medição | Nenhum sinal de uso/preferência fabricado; tráfego interno separado | Relatório persistido da sessão |
| Compatibilidade | v15 mantém a apresentação anterior | Contrato histórico, três dispositivos |
| Caminhos anteriores | Harness técnico vigente continua executável | Matriz existente, sem nova inferência |

## Resultado

Nove testes de apresentação e navegação passaram, incluindo sessões reais das versões v16 e v17 no
backend/MySQL e os três dispositivos. O harness técnico passou nos nove cenários de cada versão, incluindo sete dias, recuperação e segurança. Build TypeScript/Vite aprovado; os controles Java incluem isolamento de versões e aceitação dos relatórios reais de Vega e Mira. As capturas do primeiro resultado e das condições foram inspecionadas.
Testes técnicos não comprovam satisfação, compra ou margem. A campanha permanece
dependente das revisões e da autorização do ciclo.

Uma primeira tentativa simultânea dos navegadores excedeu a capacidade de threads
na sandbox (`pthread_create`, EAGAIN); a execução serial passou. Essa limitação de
ambiente não foi confundida com rejeição funcional. O teste de apresentação e os
relatórios persistidos não atribuem uso ou preferência a uma pessoa.

Validação Java final: 247 casos do módulo Psique, sem falhas e dez cenários
opcionais não aplicáveis sem suas fixtures externas; o runner executou 27/27,
incluindo os relatórios reais locais de Vega e Mira. As 28 verificações focadas
do backend e as 28 do publicador passaram. Uma execução ampla foi interrompida
por espera do cliente HTTP local sob limitação artificial de duas CPUs; a
mesma suíte com quatro CPUs efetivas terminou, sem alterar o código do cliente.
