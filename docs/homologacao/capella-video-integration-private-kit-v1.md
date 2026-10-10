# Capella — passagem audiovisual do kit privado

## Estado e aceite — 10/10/2026

Solicitação #3335, produto 7, cadeia 26, ciclo 12, experimento 105, candidata
`capella-private-v3`. Preservar US$ 10 para o conjunto audiovisual e revisões;
R$ 100 de mídia por cinco dias somente após a liberação comercial.

A conferência após o PR #5575 encontrou a etapa `VIDEO_APPROVAL`, revisão 8,
ativos 55/56 `READY` com seleção humana `PENDING`, sem tarefa ou campanha ativa.
O destino aceito é `/mh-api/pde/kit/private/v1/prototype`, runtime
`DETERMINISTIC_PRIVATE_KIT_V1`, perfil `nails-v1`, pacote V2 de 36 arquivos.
`LearningCycleVideoBinding.supports` reconhecia apenas `/vega-private` e
`/mira-candidate`. O cliente e o harness do kit não consumiam `videoIntegration`.
Por isso, aprovar as peças não bastaria para a passagem executável desse destino.
O histórico de Mira confirma que o contrato e o verificador compartilhados já
resolvem a integração, reprodução e recuperação em um destino implementado.

Aceite técnico: o kit deve consumir o mesmo recibo privado, preservar identidade
e arquivos, oferecer vídeo opcional e conferir os dois MP4s no harness existente.
O backend continua a integração apenas depois dos dois aceites humanos e exige
homologação e pareceres novos sobre o fingerprint. A implementação local não
aprova as peças reais, renova parecer, cria hipótese ou inicia mídia.

## Alternativas avaliadas

| Caminho | Benefício | Risco e esforço | Escolha |
|---|---|---|---|
| Devolver integração manual | Preparação imediata | Conserva o repasse e não entrega a capacidade faltante | Não adotado |
| Criar outro fluxo audiovisual | Permite desenho específico | Duplica fila, provas e gates; esforço maior | Não adotado |
| Estender o contrato existente ao runtime de kits | Resolve a mesma classe para nails e barber | Mudança pequena em apresentação e harness, com regressões | Adotado |

## Matriz antes dos testes

| Critério | Prova local necessária |
|---|---|
| Caminho feliz | Duas peças aprovadas em fixture geram o recibo do kit e passam à validação, sem liberar comércio |
| Identidade e aprovação | Peça pendente/rejeitada, outro produto/ciclo/experimento/versão ou hash alterado não pode integrar |
| Compatibilidade | Mira/Vega continuam válidos; kit com formato antigo ou runtime/rota divergentes é recusado |
| Apresentação | Sessão apresenta apenas vínculo vigente; ausência ou falha mantém o pacote acessível e registra contexto |
| Vídeo opcional | Player recolhido, sem autoplay, controles, legenda e ação principal independente |
| Falha e retorno | Erro de vídeo permite continuar; copiar/baixar e retomada preservam pacote e primeira tela |
| Proteção e origem da mídia | CSP permite somente self e a origem HTTPS do armazenamento configurado, mantendo scripts e conexões locais |
| Navegadores e dispositivos | Chromium desktop, iPhone 15 Pro e Pixel 7; perfis nails e barber com IDs distintos |
| Integração e observabilidade | Recibo/fingerprint percorrem sessão, harness e validação de resultado; callback não aceita prova anterior |
| Métricas e segregação | Tráfego AGENT_VALIDATION, modelos simulados, zero chamadas externas, sem compra ou prova comercial |

## Aprendizado verificável

Hipótese: uma rota só pode declarar integração automática quando apresentação e
harness implementam seu contrato. Isso reduz a espera técnica depois de uma
decisão humana e preserva tempo até um experimento aceito. Reutilizar a checagem
de bytes, reprodução e recuperação evita nova infraestrutura. A adoção depende
da matriz local; reverter o suporte se permitir mídia divergente ou ocultar gate.
Ainda não existe medição de demanda, conversão, contribuição ou redução real
do tempo de espera após o aceite de Capella.

## Resultado local

- **212 casos de backend**, incluindo 92 de arquitetura, passaram sem falhas ou
  skips: isolamento de sessão, contrato dos dois perfis, aprovações, contexto,
  passagem após callbacks e proteção do CDN. Formatação e diff conferidos.
- **19 testes JavaScript** passaram, incluindo bytes, áudio, reprodução e falha
  nativa. O runner Java passou 22 casos; um teste opcional de evidência histórica
  não fornecida foi ignorado. A prova essencial de kits passou separadamente.
- Backend Spring, MySQL 5.7, compositor real, TLS local e Chromium: **18 percursos**
  nos perfis nails/barber e três dispositivos, seis callbacks de Psique, dois
  fluxos Têmis/gate e nove grupos de controles. Ambos os relatórios aprovados
  conservaram `videoIntegrationFingerprint` e os quatro checks verdadeiros,
  retorno e pacote de 36 arquivos; zero modelos externos ou envio comercial.
- Primeira rodada reprovou a execução dinâmica do verificador pela CSP. A rodada
  corrigida passou, sem `unsafe-eval` ou bypass da proteção do navegador.
- A inspeção do destino real identificou `default-src 'self'` sem `media-src`,
  que impediria os MP4s do CDN. O controller agora deriva somente a origem HTTPS
  do `lead-portal.storage.public-base-url` existente; configuração insegura,
  credenciais e diretiva injetada são recusadas, mantendo as demais restrições.
- Conferência local focada com **os MP4s reais 55/56**: hashes preservados,
  anúncio de 27 s e demonstração de 45 s, reprodução/áudio, caráter opcional e
  recuperação em desktop/iPhone/Pixel. Contexto divergente, ausência e URL
  malformada mantiveram a entrada acessível. Zero gravações externas. O recibo
  desses testes é fictício e segregado; não representa aceite humano dos vídeos.
- `bash -n` e ShellCheck passaram no runner; sintaxe JavaScript e verificações
  de formato passaram. A topologia Compose exclusiva foi removida com volumes.

As provas locais estão em `/tmp/capella3335/private-kit-video-evidence-corrected`
e `/tmp/capella3335/private-kit-real-videos-results.json` nesta sandbox.
Os aceites reais de 55/56 continuam pendentes. Preparação e capacidade técnica
não são publicação comercial, campanha, demanda, contribuição ou lucro.

## Compatibilidade compartilhada

O suporte de Mira e Vega permanece sujeito às mesmas aprovações e fingerprints.
O binding foi revalidado com esses percursos; o verificador nativo preserva os
mesmos critérios e recebeu a regressão de erro sem avaliação dinâmica.
A atestação sucessora `mira-private-candidate-v18` atualiza as fontes
compartilhadas conferidas e referencia a v17 imutável. Não muda a experiência,
produto, ciclo, tarefas ou orçamento de Mira. O seletor de publicação deve
continuar `none`, sem novo deploy de experiência comercial por essa atestação.
