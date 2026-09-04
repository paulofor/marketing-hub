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
| Acesso | token opaco + consentimento cria sessão | token ausente/inválido é recusado | segredo removido da URL; sessão do backend |
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
