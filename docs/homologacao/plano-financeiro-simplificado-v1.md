# Homologação do plano financeiro simplificado

Matriz definida antes dos testes em 20/09/2026. Dados sintéticos em banco local isolado;
Plutus simulado, sem campanha, pagamento, análise paga ou escrita de teste em produção.

| Área | Critério de aceite |
| --- | --- |
| Caminho feliz | Abrir produto, visualizar somente suporte e IA, salvar, recarregar e conferir escolhas e histórico |
| Sugestões | Quartzo com sete dias, demais tipos com trinta, IA inicialmente Sim; escolhas anteriores preservadas |
| Fontes e cálculos | Reaproveitar revisão da mesma versão e fontes comerciais, manter ausência como nulo; suporte separado de período econômico |
| IA | Sim não inventa tarifa; Não declara ausência de personalização sem apagar investimento inicial; voltar a Sim não reutiliza tarifa zero |
| Validação | Dias vazios, zero, negativos, fracionários ou acima do limite; erro de API sem perder escolhas |
| Integração | Controller, service, persistência MySQL 5.7 e recarga reais; contrato com cálculo e parecer existentes preservado |
| Concorrência e isolamento | Revisão/versionamento desatualizados recusados; fontes de outro produto, versão ou ambiente não adotadas |
| Observabilidade | Origem, escolhas, revisão e pendências persistidas; nenhuma projeção vira lucro realizado; TEST não solicita parecer pago |
| Navegação | Chromium desktop e emulações iPhone 15 Pro/Pixel 7; formulário sem overflow e botões ocupados desabilitados |
| Regressão | Testes do backend/frontend, build, edição avançada e modelos por tipo preservados, revisão do diff |

Alternativas: recolher o formulário completo é barato, mas mantém o trabalho manual;
delegar números a IA acrescenta custo e incerteza; preparar no backend com fontes existentes
reduz esforço e mantém a evidência. A terceira foi adotada. A simplificação não comprova que
as tarifas/custos ainda desconhecidos de Capella já foram confirmados.

## Resultados locais de 20/09/2026

- Backend completo: 3.290 testes aprovados, 19 ignorados por condições preexistentes;
  zero falhas/erros. Os nove testes novos de preparação foram executados integralmente.
- Frontend: 717 testes aprovados em 170 arquivos; typecheck e build aprovados.
- API/JPA/MySQL 5.7: 51 verificações existentes aprovadas, incluindo concorrência de
  oito solicitações para a mesma análise com executor de Plutus simulado.
- Preparação simplificada: 81 verificações em Chromium desktop, iPhone 15 Pro e Pixel 7
  emulados, incluindo falha HTTP, preservação das escolhas, validações e TEST/LIVE.
- Regressão da edição avançada: 68 verificações aprovadas nos três dispositivos.
- Após reiniciar o backend local, as duas revisões permanecem íntegras, com suporte de
  14 dias e escolhas históricas distintas de IA; nenhuma revisão foi sobrescrita.
- OpenAPI validado com SnakeYAML, incluindo todas as referências internas. Diff sem
  erros de whitespace; classes e métodos Java alterados com comentários em português.
- Spotless, empacotamento e verificação do JAR aprovados. Topologia Compose, volumes
  e processos temporários removidos após a homologação.

Evidências locais em `artifacts/financial-plan-simple/`: logs das suítes e matrizes,
capturas `desktop-clean.png` e `iphone-clean.png`. Os scripts reproduzíveis são
`infra/testing/product-financial-plan/preparation-matrix.mjs`, `api-matrix.mjs` e
`browser-matrix.mjs`, com `FinancialPlanLocalApplication` e o Compose desse diretório.
Usar o projeto Compose exclusivo fornecido pela sandbox e removê-lo com
`down --volumes --remove-orphans` ao terminar.

O teste inicial detectou unboxing indevido de tentativas desconhecidas no preparador;
foi corrigido com preservação explícita do inteiro nulo. Os casos de IA inicial e de
retorno de Não para Sim impedem recorrência. As matrizes de navegador também foram
ajustadas para distinguir alertas simulados de outros módulos e abrir a edição avançada.
Não houve chamada paga ou criação de dado de teste em produção. Emulação de iPhone
usa Chromium, sem equivaler a teste em Safari nativo/aparelho físico.

Capella continua com custos sem fonte na revisão original. A entrega simplifica a
entrada e preserva essas lacunas; não comprova margem, aprovação de Plutus ou autorização
para anunciar o produto.
