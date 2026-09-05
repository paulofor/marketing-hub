# Homologação — protótipo privado de Mira v1

Data: 2026-09-04.

## Gargalo e decisão

A atividade #337 entregou somente um contrato textual, embora a cadeia a exibisse como implementação
concluída. Foram comparados: aceitar o documento, adaptar a jornada comercial MUSA e materializar uma
superfície privada segregada no PDE Platform. A terceira alternativa foi escolhida por gerar evidência
funcional sem misturar métricas, publicar oferta ou habilitar cobrança.

## Matriz ponta a ponta

| Dimensão | Caminho feliz | Validação/falha | Evidência esperada |
|---|---|---|---|
| Acesso | token opaco no fragmento + consentimento cria sessão | token ausente/inválido é recusado | segredo não trafega em request/path/query, fragmento é removido; sessão do backend |
| Entrada | faixa, objetivo não clínico e rótulos | lacuna documental ou pedido clínico bloqueia | causa e menor ação, sem rotina inventada |
| Resultado | limpeza precede hidratação conforme rótulos | instrução desconhecida não é inferida | cartões estáveis e aviso não clínico |
| Observabilidade | cinco eventos uma vez por sessão | repetição não duplica evento | trilha `QA_INTERNAL` separada de `PRIVATE_READING` |
| Continuidade | sessão recarrega e retoma o resultado | sessão escolhida pelo cliente é recusada | mesmo checkpoint após nova instância |
| Comercial | checkout de R$ 49 é somente simulado | não existe captura ou provedor de pagamento | `paymentEnabled=false`, gasto zero e produto `PLANNED` |
| Navegadores | Chromium desktop e iPhone 15 Pro | sem overflow ou dependência de hover | capturas e ausência de erro de console/HTTP |
| Privacidade | rota privada não indexável | segredo não segue em `Referer` ou access log | `no-store`, `no-referrer`, `X-Robots-Tag` |

## Critério do gate humano

O gate “Confirmar protótipo privado utilizável” só pode ser preenchido depois que a imagem versionada
for publicada por PR, os dois acessos de leitura estiverem no cofre do deploy e uma rodada interna
segregada comprovar desktop, celular, eventos próprios, retomada e ausência de efeitos comerciais.
QA não conta como leitura humana, preferência, checkout real, venda ou receita.

O deploy direcionado a `v7` deve falhar se `PDE_MIRA_PRIVATE_QA_TOKEN` estiver ausente. Depois de
publicar os containers, o workflow usa esse mesmo segredo somente no Playwright, abre
`/mira-private#access=<token>` em desktop, iPhone 15 Pro e Pixel 7 e comprova a jornada completa sem
enviar o segredo em nenhuma requisição. Os convites `PDE_MIRA_PRIVATE_ACCESS_1` e
`PDE_MIRA_PRIVATE_ACCESS_2` permanecem intactos para as duas leituras humanas posteriores.

## Resultado local após a correção

Em 2026-09-05, a investigação do deploy publicado encontrou três lacunas no contrato operacional:
o segredo de QA existia no GitHub, mas não chegava ao backend; o convite ainda era lido do path; e o
redirecionamento interno de `try_files` removia os cabeçalhos privados da resposta HTML. O fluxo foi
corrigido na origem e o teste passou a fazer parte da mesma imagem Playwright usada na integração.

Após a última correção, duas rodadas completas e consecutivas partiram de volumes vazios. Cada uma
aprovou 153 testes do backend PDE, build TypeScript/Vite, Actionlint compatível com `queue: max`,
contratos de deploy, Compose de produção, MySQL 5.7 e 12 jornadas reais de navegador em Chromium,
iPhone 15 Pro e Pixel 7. Em cada banco limpo, Mira persistiu exatamente cinco eventos
`QA_INTERNAL`, zero `PRIVATE_READING` e zero evento de pagamento; o token de QA não apareceu nos
logs do backend ou do frontend.

## Fechamento do runtime produtivo em 2026-09-05

O primeiro deploy do commit `c7e542ba` publicou corretamente backend e frontend, mas o smoke público
interrompeu antes da jornada de Mira. O descritor do iPhone 15 Pro selecionava WebKit por padrão ao
mesmo tempo que a configuração fornecia um executável Chromium; o processo encerrava antes de abrir
a página, inclusive quando executado isoladamente. O projeto mobile passa a declarar Chromium
explicitamente, preservando viewport, user agent, toque e escala do iPhone. O contrato de deploy
impede a reintrodução dessa combinação incompatível.

Na rodada produtiva seguinte, a jornada passou nos três dispositivos e persistiu exatamente os cinco
eventos `QA_INTERNAL`, sem leitura humana, pagamento, publicação ou gasto. A confirmação pela tela
foi aceita pelo backend, mas a inspeção do console encontrou o padrão HTML de versão incompatível
com a flag `v` usada pelo Chromium atual. O campo passa a expressar o hífen fora da classe de
caracteres, e seu teste de interface protege o contrato. A atividade `prototypeAcceptance` ficou
`COMPLETED`, a experiência de Mira ficou `PRIVATE_PROTOTYPE_READY` e a próxima atividade liberada é
`privateReading1`.
