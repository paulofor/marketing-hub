# Retomada financeira Vega #91 — matriz local

## Aceite definido antes da implementação

| Dimensão | Cenário | Critério |
|---|---|---|
| Caminho completo | Tela → autorização → fila → Meta simulada → callback → tela | Teto acumulado 150; campanha original RUNNING somente após confirmação |
| Proteção | Gasto anterior 27,45; cap 150 | Saldo 122,55; orçamento vitalício 15000 centavos, nunca saldo somado novamente |
| Validações | Sem autorização, motivo curto, teto esgotado, prazo passado, campanha ativa ou vários conjuntos | Bloqueia sem ativar mídia |
| Concorrência | Duplo clique, claims concorrentes, callback repetido/antigo | Um pedido ativo, lease exclusiva e callback idempotente |
| Integrações | Rejeição/falha de confirmação Meta, readback divergente, indisponibilidade backend | Pausa preservada e erro auditável; sem falso sucesso |
| Parada | #91 autorizado vs experimento comum; abaixo/no teto | Exceção somente no autorizado; padrão 25 preservado e pausa em 150 |
| Dados | Retomada vs histórico | Não altera funil, coorte, visitantes, vendas nem identidade; mocks sem produção |
| Persistência | MySQL 5.7 e migração idempotente | Colunas mapeadas, DATETIME explícito, inclusão relativa válida |
| Observabilidade | Pedido, status, autorização, resposta, falha | Persistidos e apresentados na tela; logs sem credenciais |
| Navegadores | Chromium desktop, iPhone 15 Pro e Pixel 7 | Formulário, pendência, sucesso e erro legíveis e operáveis |

## Evidência inicial

Em 20/09/2026, Meta confirmou campanha `120251556536430326` PAUSED,
um conjunto `120251556536530326` com orçamento vitalício 10000, gasto
27,45 e prazo encerrado em 11/09. O Hub registrava USER_STOPPED e um lead no PDE.
O comando antigo mudava apenas o estado do experimento; o sincronizador do worker
mantinha a parada fixa em 25 por zero leads Meta. A autorização atual permite a
exceção individual; o #92 permanece intocado.

## Resultados

- Backend: 3.274 casos nas suítes locais, sem falhas funcionais após os ajustes;
  17 cenários condicionais permaneceram ignorados conforme suas condições existentes.
- Worker: 136 testes sem falha; inclui fila/reserva, orçamento vitalício acumulado,
  cinco sondas segregadas, readback, rejeição Meta ausência de Insights e falha do callback após ativação.
- Frontend: suíte de 710 casos identificou uma expectativa legada de reativação;
  contrato ajustado e 14 testes relacionados aprovados, incluindo o cenário adicional
  de canal direto. Total atual: 712 casos; typecheck e build aprovados.
- MySQL 5.7 local: migração nova aplicada e reaplicada; JPA em `validate` persiste a
  entidade real e confirma exclusão entre duas reservas. Credenciais e schema da fixture são sintéticos.
- Chromium desktop, iPhone 15 Pro e Pixel 7: autorização, pendência, sucesso e falha
  aprovados, sem overflow; imagens e resumo em `artifacts/facebook-resumption`.
- `bash -n`, ShellCheck e validador estático Liquibase aprovados.
- Leitura da Meta confirmou o contrato `account_id` + consulta `act_{id}/currency`;
  `account{currency}` não existe na campanha. A fixture replica o contrato confirmado.
- A execução inicial simultânea excedeu memória e terminou em 137. Foram preservados
  os resultados concluídos e executadas apenas as suítes restantes e os contratos alterados,
  sem publicar código para diagnóstico.

A ampliação mantém R$ 150 como teto acumulado, incluindo R$ 27,45 anteriores.
O cadastro confirmado é `LOGIN_STARTED`, humano, `utm_source=ig`, `utm_medium=paid`,
vinculado à campanha original; nenhum endereço de e-mail foi copiado para evidências.
A retomada operacional ocorre somente após publicação dos módulos pelo PR.

## Reproduzir a fixture física e visual

Usar `docker compose -p <projeto exclusivo da sandbox> -f infra/testing/facebook-resumption/compose.yml up -d --wait`.
Executar `FacebookCampaignResumptionMysqlTest` com `VEGA91_MYSQL_URL` apontando para
`jdbc:mysql://sandbox-docker:13391/resumption_test?useSSL=false&allowPublicKeyRetrieval=true`.
O browser local é `node infra/testing/facebook-resumption/browser.cjs` e usa o componente
React real com dependências HTTP simuladas. Encerrar a topologia com o mesmo projeto
Compose e `down --volumes --remove-orphans`. Os testes de serviço/controller e do worker
são executados pelas suítes Java habituais, sem credenciais externas.
