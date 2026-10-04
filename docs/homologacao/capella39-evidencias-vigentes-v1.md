# Homologação da leitura de evidências — execução #39

Data: 04/10/2026. Escopo: Capella #7, Quartzo v1, cadeia #26, processo técnico
#122/v7, execução #39, referência `experiment:88`. Pai declarado #118/v12,
atividade `preflight`; não há ciclo nem tarefa de agente nesta execução.

## Diagnóstico confirmado antes da implementação

- A tela e o MCP mostram três instâncias concluídas (#477–#479) e o run técnico #16.
- O relatório apresenta plano #35, mas APIs e banco vinculam esse plano exclusivamente
  ao experimento #96. O experimento #88 pertence ao plano #2.
- A revisão financeira #9/revisão 5, plano #2/v4, usa contrato v1 e parecer #62.
  Sua validade terminou em 02/10/2026. A revisão #10/barber-v1 não pertence ao #88.
- A publicação #32 conserva o hash de HTML da homologação histórica, mas o validador
  oficial recusa o fingerprint comercial atual. A conclusão BPM histórica continua
  aparecendo como objetivo vigente; não é prova da validade atual.
- O #88 está `INVALIDATED`, com janela encerrada em 29/09, teto de R$ 125 e parada
  em cinco compras. O parecer #62 é limitado a R$ 100 e duas compras. Não alterar
  esses contratos, estender validade ou iniciar análise paga para obter conclusão.

## Matriz definida antes dos testes

| Área | Critério de aceite | Validação local |
| --- | --- | --- |
| Identidade | Referência de experimento seleciona somente seu plano direto/portfólio; ausência não adota outro plano do produto | Caso original e identificadores independentes; planos sem vínculo e referências comerciais |
| Evidência vigente | Conclusão técnica obsoleta deixa de comprovar o objetivo; instância e custo anteriores permanecem intactos | Executor real e motor BPM com fontes locais; fingerprint alterado, ausente, inválido e vigente |
| Recuperação | Nova prova válida permite nova ocorrência pelo comando canônico; repetição idêntica não duplica | Revalidação, idempotência e erro de leitura, sem chamadas pagas |
| Integração | Relatório e controle automático consomem a mesma verdade; bloqueio financeiro impede conclusão e retorno ao pai | Fluxo local com repositories simulados e transações locais; progresso, comando e espera persistida |
| Economia | Parecer vencido identifica revisão e plano corretos; teto acima de Plutus permanece bloqueado | Regressões financeiras e preservação dos resultados válidos de compra/medição |
| Observabilidade | Erros conservam contexto e stack trace; consultas não sobrescrevem histórico | Logs, respostas estruturadas e verificação de ausência de escrita durante leitura |
| Interface | Desktop, iPhone 15 Pro e Pixel 7 apresentam plano, progresso e pendência enviados pelo backend | Frontend local com respostas da validação local; nenhuma API produtiva de escrita |
| Empacotamento | Classes e contratos corrigidos pertencem ao JAR validado | Testes unitários/integração, arquitetura, formatação e build local |
| Isolamento | QA, fontes e identificadores sintéticos não entram em campanhas, compras, receitas ou agentes pagos | Dependências simuladas, nenhuma autorização de mídia e custo comercial desconhecido preservado |

## Alternativas e escolha

| Alternativa segura | Benefício | Risco | Esforço | Aderência e escolha |
| --- | --- | --- | --- | --- |
| Renovar toda a homologação por testes determinísticos | Prova técnica completa | Repete verificações ainda válidas e não resolve a validade econômica | Alto | Viável, reservada para fontes técnicas integralmente alteradas |
| Separar o fingerprint comercial em contratos menores | Invalidação mais específica | Exige outro contrato e pode omitir dependências ainda não demonstradas | Alto | Viável como evolução separada, sem evidência suficiente neste escopo |
| Revalidar fontes canônicas no executor e resolver o plano pela referência | Preserva tentativas, gates e isolamento entre candidatas | Mantém bloqueios legítimos até existir prova atual | Baixo | Escolhida; corrige as duas causas observadas sem revisão paga |

Esta homologação comprova contratos operacionais. Não comprova venda, receita ou margem.
O vencimento financeiro é uma dependência legítima; a entrega de código não renova o parecer.

## Resultados locais e evidências

- Código anterior: as sete novas regressões falharam. Correção: 73 testes direcionados
  aprovados; suíte completa com 3.838 aprovados e 24 skips por condições documentadas.
  Inclui arquitetura, persistência H2, entrega simulada, idempotência e recuperação.
- A fixture do serviço BPM real alimenta o frontend local: seis cenários de
  desktop/iPhone 15 Pro/Pixel 7 aprovados, plano correto, conclusão atual coerente,
  pendências visíveis, sem overflow, erro JavaScript ou chamada de escrita produtiva.
- JAR aprovado: 4.206 classes idênticas às testadas, 744 recursos externos íntegros
  e nove testes do verificador de empacotamento aprovados. Spotless e build passaram.
- Publicação #32: SHA de origem conferido no MySQL e HTML servido; três dispositivos
  carregaram os oito elementos de imagem e os seis CTAs para o mesmo checkout.
  Capturas e bytes servidos estão vinculados por SHA-256. O teste passou a aguardar
  cada imagem lazy no viewport; a passagem rápida inicial deixava decodificação pendente.
- Meta: ambas as campanhas continuam `PAUSED`/`PAUSED`. QA conservou 711 eventos,
  máximo ID 5299. Nenhuma cobrança, chamada de modelo, mídia ou compra real.

Registro: [evidências estruturadas](capella39-evidencias-v1.json). A confirmação
posterior à publicação deve ser vinculada ao PR e ao SHA integrado; estes resultados
locais não declaram antecipadamente que a correção já está publicada.

Reprodução visual, com o frontend local na porta 4173:

```bash
cd backend/ads-service
mvn -Dtest=TechnicalPreflightCurrentEvidenceFlowTest \
  -Dpreflight.output=/tmp/preflight-current-evidence test
cd ../..
PREFLIGHT_PROCESS_EVIDENCE_DIR=/tmp/preflight-current-evidence \
  node backend/ads-service/src/test/resources/preflight-current-evidence-ui.cjs
```

Para consultar a pendência exata, use o plano financeiro do produto #7 com
`revisionId=9`, na edição avançada. A revisão mais recente, #10/barber-v1/plano #35,
não deve substituir a revisão da referência `experiment:88`.
