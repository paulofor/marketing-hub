# Qualidade audiovisual para a cliente — v1

## Evidência e causa

Em 11/10/2026, a tela e o MCP confirmaram a rejeição de Mira #53/#54, experimento #102.
A copy persistida continha “Experimentar Mira em acesso privado”. O briefing #634 misturava
instruções internas e texto final. Os jobs #21256/#21258 combinaram captura única,
redução da página inteira e sobreposição estática nos cortes funcionais. Psique #655
aprovou o criativo estático #654; não há parecer dela sobre esses dois vídeos finais.
A consulta de logs respondeu, mas a janela retida não trouxe esses jobs históricos.
A evidência disponível é o arquivo final, metadados persistidos e o código.

## Alternativas

| Caminho | Benefício | Risco e esforço | Decisão |
| --- | --- | --- | --- |
| Corrigir somente a copy do caso | Baixo esforço | Mantém montagem e ausência de prova final | Insuficiente |
| Regenerar todas as mídias | Maior variedade potencial | Custo adicional e reincidência do contrato | Evitar repetição sem causa corrigida |
| Corrigir produção/revisão existentes e reutilizar fontes compatíveis | Fecha causa compartilhada, preserva auditoria | Exige regressões entre módulos | Adotado |

## Matriz local de aceite

| Dimensão | Caso e resultado exigido |
| --- | --- |
| Copy | Bloquear linguagem interna em fala/legenda/CTA antes de TTS; permitir metadados e ressalvas funcionais |
| Visual | Preservar movimento, manter recortes da prova legíveis e impedir tela longa reduzida como demonstração |
| Revisão | Arquivo final e linha do tempo vinculados; ausência não pode ser aprovação sensorial |
| Recuperação | Rejeição pede correção, não aprovação; histórico e callback preservados sem inferência duplicada |
| Integração | Produtor, finalizador e leitura de ciclo usam contratos reais com dependências locais |
| Regressão | Caso Mira, produto sintético com outros IDs e caminho já válido |
| Dispositivos | Chromium desktop, iPhone 15 Pro e Pixel 7 emulados; ler quadros no tamanho de exibição |
| Áudio | Integridade/duração/sincronismo técnicos separados de escuta perceptual |
| Finanças | Teto de US$ 10 audiovisual cumulativo, custo desconhecido não zero; sem mídia nesta correção |
| Métricas | Testes e simulações segregados; nenhuma alegação de venda, lucro ou conversão |

## Resultados locais

- 14 frames extraídos dos MP4s rejeitados com o capturador real, SHA-256 validado, abertura,
  progressão e encerramento. O armazenamento real do backend aceitou as provas usando S3
  substituído por test double, sem enviar imagens de teste ao storage produtivo.
- Psique: suíte Java completa passou (181 testes declarados, 10 opcionais de outros harnesses
  não habilitados); Íris: 79 declarados, 3 opcionais não habilitados. Captura real deste caso
  foi executada separadamente, não substituída por esses skips.
- Apolo: testes de copy, storyboard, pós-produção e sobreposição passaram; FFmpeg real
  conferiu vídeos 15/30 s, recorte e preservação dos outros planos. Uma nova montagem local
  de 20 s com fonte auditada e captura original passou sem qualquer chamada de provedor.
  Essa candidata local tem apenas legenda: não comprova locução final nem aceite de Psique.
- Backend: regressões de identidade/tenant, revisão por hash, armazenamento, reabertura e
  orientação passaram com Mira e IDs sintéticos diferentes. Parecer de PNG não libera
  vídeo novo; aprovação histórica não é alterada.
- Node: contrato de bytes/hash, ausência de redirect e amostragem temporal passou. O teste
  integra a suíte existente de navegador, sem framework paralelo.
- Prévia de 20 s reproduzida em Chromium desktop, iPhone 15 Pro e Pixel 7 emulados; duração,
  avanço de reprodução e leitura do recorte conferidos. A audição não foi alegada.
- Nenhum gasto, publicação comercial ou métrica humana foi produzido por estes testes.

## Adoção e reversão

Adotar somente após checks do PR, publicação dos módulos e conferência da revisão real.
Se a validação rejeitar uma entrada válida, preservar o job e sua auditoria, corrigir o contrato
com uma fixture de regressão e reproduzir localmente. Não remover o gate nem repetir inferência
para contornar a falha. Rastrear a entrega pelo commit e pela versão do contrato.

