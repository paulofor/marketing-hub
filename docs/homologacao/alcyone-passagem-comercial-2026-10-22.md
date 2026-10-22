# Alcyone: passagem para a preparação comercial

## Evidência e escopo

Solicitação #3711: perfil `@produtividade360_`, R$ 100 totais por cinco dias após
homologação. Produto 11, experimento 97, plano 34, cadeia 26, sem ciclo sucessor.
IA conserva US$ 12 cumulativos; vídeos pagos não foram autorizados. A estimativa
conhecida antes da continuação é US$ 11,0193274, não uma fatura conciliada.

O comando existente de preparação tornou o slot `paid-v1` READY/OK, mantendo
PLANNED. A projeção do Processo 4 passou a invalidar o destino privado, mas seu
executor continuou escolhendo a experiência privada. A integração também
permanecia concluída em modo privado. Safira exige corretamente evidência comercial.
O histórico privado aceito e os testes desse caminho descartam ausência de prova:
faltava delimitar a saída da preparação inicial sem ciclo de aprendizado.

## Alternativas

| Caminho | Benefício | Risco/custo | Decisão |
| --- | --- | --- | --- |
| Refazer toda a comunicação privada | Reprocessa os contratos | Repete custos e aceites sem mudar o despacho | Não adotar |
| Aceitar integração privada como comercial | Remove o bloqueio | Confunde preparação com aprovação comercial | Rejeitar |
| Reconhecer a candidata comercial preparada e executar a integração própria | Reutiliza histórico, destino e executores existentes | Requer regressão da fronteira | Adotar |

O reconhecimento depende de primeiro experimento PLANNED, sem predecessor/ciclo,
com destino comercial reconhecido pelo validador existente. Não depende do nome
Alcyone ou dos IDs do caso. Destino inconsistente continua bloqueado, sem retornar
silenciosamente à prova privada. O contrato privado permanece histórico; anúncios
e experiência comercial exigem suas próprias revisões, homologação e autorizações.

## Matriz definida antes da validação

| Área | Critério |
| --- | --- |
| Passagem | Referências original e outra identidade chegam ao executor comercial com o mesmo slot/checkout |
| Integração | Conclusão privada reabre uma vez; prova comercial própria encerra a pendência sem duplicar custo |
| Antes válido | Preparação sem destino comercial e ciclos conservam os controles privados anteriores |
| Negativos | Outro produto, checkout divergente, sucessor, ciclo, STOP/encerramento e candidato não preparado não recebem aprovação |
| Histórico | Orçamento/perfil não refazem a comunicação privada já concluída; materiais comerciais continuam sujeitos a revisão |
| Observabilidade | Evidências comerciais explicitam identidade e modo; publicação e mídia não são concedidas pelo aceite técnico |
| Interface | Conferir controles publicados em desktop e celular Chromium; reaproveitar o ensaio de entrega paga existente |
| Dados e métricas | Testes locais sintéticos, sem pagamento, inferência ou eventos de mercado; vendas continuam desconhecidas |

Hipótese de melhoria do harness: reduzir retrabalho e espera causada pela mistura
de fases. Adotar somente com a matriz preservada; reverter se aprovação privada
liberar mídia ou se a referência de outro produto for aceita. Testes não comprovam
conversão nem margem realizada.

## Resultados

- Roteamento anterior: os dois cenários de passagem falharam, sem erro de preparação
  do teste. Roteamento corrigido: os 11 cenários passaram.
- Backend completo: 5.196 testes, zero falhas/erros, 57 condicionais de outras
  topologias ignorados. A mudança não altera schema nem SQL; os repositories da
  nova matriz são doubles, e os executores/validador de destino/leitor de Safira
  são reais. Os testes existentes com H2 foram executados pela suíte completa.
- Provas comerciais: 16 testes aprovados; pacote de 144 manifestos/625 arquivos
  reconstruído sem invalidar atestações.
- Pacote local: 4.402 classes idênticas às testadas, 777 recursos íntegros e
  catálogo executável de 534 cartões. Diff e comentários de responsabilidade revisados.
- Público administrativo: perfil e orçamento conferidos por tela e banco;
  versão paga preparada em READY/OK pelo comando existente, sem campanha ou
  nova inferência. Navegação da entrega conferida em desktop e celular Chromium.
- A mensagem privada contém “exemplo sem cobrança”; seus bytes e pareceres
  permanecem históricos. A oferta paga de R$ 79 exige comunicação comercial
  própria. Nenhum ativo foi promovido por remover aviso ou trocar status.
- Saldo de IA insuficiente para a última rodada comparável de Plutus, Psique e
  Têmis (cerca de US$ 1,28, antes da comunicação), frente a US$ 0,98 disponíveis.
  Decisão sobre novo teto apresentada; nenhuma ampliação presumida. A execução
  comercial continua condicionada aos insumos, revisões e homologação.

Publicação e conferência do percurso serão registradas nas evidências do PR.
