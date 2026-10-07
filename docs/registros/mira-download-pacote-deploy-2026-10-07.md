# Mira — download íntegro do pacote de deploy — 07/10/2026

O run `37637394844`, da main `621d60a838a57388457ef3214690135cc7bc6b9a`, construiu
as duas imagens e publicou o artefato imutável `backend-image` (`11490137536`). O produtor
registrou dois arquivos. O download do consumidor terminou sem a mensagem de conclusão;
a transferência encontrou `backend-image.tar`, mas não `process-worker-image.tar`.

A consulta e o download na sandbox confirmaram ambos os arquivos, seus manifests Docker,
tags do mesmo SHA e integridade. O run anterior `37618334762` concluiu esse download e o
deploy. Ambos usaram Node 24; atribuir a falha somente à versão do Node foi descartado.
O limite comprovado é a aceitação de extração incompleta como etapa bem-sucedida. O
workflow não conferia o pacote antes de instalar chaves, copiar descritores e transferir.

| Alternativa | Benefício | Risco/esforço | Decisão |
|---|---|---|---|
| Reexecutar a transferência | Recuperação rápida quando os arquivos existem | Não recupera arquivo local ausente nem previne falso sucesso | Descartada |
| Conferir existência e pedir nova tentativa manual | Evita transferência incompleta | Mantém espera e não detecta arquivo truncado | Descartada |
| Baixar o artefato imutável com verificação e recuperação limitada | Confere origem, digest, ZIP e arquivos antes de disponibilizar o pacote | Pequeno helper reutilizável no workflow existente | Adotada |

## Matriz local definida antes dos testes

- Caso original: ZIP válido com as duas imagens; ausência de worker na primeira tentativa
  deve falhar a validação ou recuperar o mesmo artefato, sem publicar pacote parcial.
- Outro run/revisão/nome de artefato: mesma regra, sem exceções por produto ou ID.
- Caminho válido anterior: pacote backend completo e pacote frontend independente.
- Falhas: ZIP truncado, CRC inválido, digest divergente, arquivo obrigatório ausente/vazio,
  origem divergente, artefato expirado/ambíguo e caminho inseguro nunca são promovidos.
- Recuperação: máximo de três tentativas do mesmo ID; falha persistente preserva o destino
  anterior e termina com erro. Nenhum teste usa SSH, publicação ou consumo de IA.
- Integração: o workflow usa diretórios separados e confere backend/worker antes de SSH;
  testes de contrato existentes executam o novo teste na CI e no preflight de deploy.
- Observabilidade: run, revisão, artefato, tentativa e causa ficam registrados; sucesso
  significa pacote verificado, ainda sujeito à saúde posterior dos serviços publicados.

Mira permanece pausada no processo 54, com mensagem 634 preservada e consumo estimado
de US$ 8,5320024 sob o teto cumulativo original de US$ 10. Esta correção não autoriza gasto.

Referência externa consultada: [releases do toolkit de artefatos](https://github.com/actions/toolkit/blob/main/packages/artifact/RELEASES.md).
Ela documenta correções de timeout e suporte a Node 24; não comprova a causa específica
do transporte neste run. A decisão acima se baseia nos logs e no pacote real.

## Resultados locais

- 14 testes de contrato do downloader e 13 testes do contrato da CI passaram.
- O helper baixou o artefato real 11490137536, conferiu o digest SHA-256 do ZIP e
  entregou ambas as imagens, iguais byte a byte ao download independente anterior.
- Backend: 438492160 bytes, SHA-256
  `9511837b64b04a14a73a1730e0efd2f8e14e5f35b354ed3a8ecae296b35991fc`.
- Worker: 169958400 bytes, SHA-256
  `69f86352e830bfca8dec7aa7efd5acc55bd969a1c19a0690251c9b8db332263b`.
- O teste transacional executou rsync local real nas três sincronizações, preservando
  marcadores, credenciais fictícias e volumes. Espera de saúde e leitura de revisão passaram.
- Os quatro blocos shell alterados e os scripts investigados passaram `bash -n` e ShellCheck.
  As dependências locais rsync e parser YAML foram disponibilizadas na sandbox.
- Nenhuma imagem foi reconstruída manualmente para produção, transferida por SSH pelo
  modelo ou aplicada fora do pipeline. A correção será entregue por PR, revisão e merge.
