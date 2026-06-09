# Avaliação da etapa 6 — redes sociais e comunidades públicas para MEI/autônomo

## Decisão executiva

A etapa 6 foi executada como **avaliação técnica, jurídica e arquitetural**, sem integrar coleta automática de redes sociais ao pipeline principal.

A decisão para produção é: **não habilitar scraping social amplo**. O OPRM só deve usar fontes sociais/comunitárias quando houver mecanismo oficial, estável, permitido e rastreável. Enquanto isso, a etapa opcional `social-behavior-searcher` permanece apenas como desenho aprovado de contrato, sem scheduler ativo e sem persistência de dados pessoais.

Essa decisão protege o objetivo maior do Marketing Hub: gerar vendas com inteligência de mercado confiável, sem criar risco jurídico, instabilidade operacional ou dados contaminados por coleta não permitida.

## Escopo avaliado

A avaliação considerou fontes candidatas para melhorar a leitura de:

- linguagem real usada por MEI/autônomos brasileiros;
- dores práticas do trabalho cotidiano;
- dores emocionais, medos e inseguranças;
- sonhos e objetivos pessoais/profissionais;
- canais e comportamentos de aquisição de clientes;
- sinais recentes de mudança no mercado local.

A avaliação não autoriza pesquisa de produto, oferta, preço, campanha, promessa, mecanismo de venda ou hipótese comercial nessa fase.

## Matriz de permissão e estabilidade

| Fonte candidata | Situação para o OPRM | Motivo operacional | Decisão |
| --- | --- | --- | --- |
| YouTube Data API | Candidata condicionada | Possui API oficial para dados públicos, mas exige credenciais, quotas, aderência aos termos e cuidado com comentários/identificadores. Referências: https://developers.google.com/youtube/terms/api-services-terms-of-service e https://developers.google.com/youtube/terms/developer-policies. | Permitida apenas via API oficial, com agregação e sem persistir identificadores pessoais. |
| Reddit API | Candidata condicionada | Possui termos específicos de API/desenvolvedor e restrições para uso de conteúdo público. Referências: https://redditinc.com/policies/developer-terms e https://redditinc.com/policies/data-api-terms. | Permitida apenas via API oficial, comunidades públicas relevantes e retenção mínima. |
| Google Trends | Candidata segura para tendência agregada | Fonte agregada, útil para direção de interesse e sazonalidade, sem conteúdo pessoal. Referências: https://support.google.com/trends/answer/4365533 e https://support.google.com/trends/answer/4365538. | Permitida como sinal agregado de tendência, com citação da fonte. |
| Reclame Aqui | Candidata restrita | Pode conter relatos públicos úteis, mas envolve dados de consumidores/empresas, moderação e regras próprias de uso. Referências: https://www.reclameaqui.com.br/termos-de-uso e https://www.reclameaqui.com.br/como-funciona/. | Somente leitura manual/curadoria ou integração expressamente autorizada; não fazer scraping automático. |
| Fóruns e comentários em portais brasileiros | Candidata condicionada | Podem trazer linguagem real, mas cada domínio possui termos próprios, robots, limites e risco de dados pessoais. | Avaliar domínio a domínio; coletar apenas trechos curtos e agregados. |
| TikTok | Bloqueada por padrão | Os termos públicos restringem extração automatizada sem aprovação; a Research API é voltada a contexto específico e não deve ser assumida como base comercial estável. Referências: https://www.tiktok.com/legal/page/us/terms-of-service/en e https://developers.tiktok.com/doc/research-api-faq. | Não integrar sem aprovação formal e contrato permitido. |
| Instagram/Facebook/Grupos Meta | Bloqueada por padrão | Coleta automatizada de produtos Meta exige permissão/termos específicos; grupos frequentemente envolvem privacidade e autorização. Referência: https://www.facebook.com/apps/site_scraping_tos_terms.php. | Não integrar sem fonte pública, permissão formal e mecanismo oficial estável. |

## Regra de arquitetura para a etapa opcional `social-behavior-searcher`

Se uma fonte social/comunitária for aprovada no futuro, a implementação deve seguir o mesmo padrão das etapas OPRM NichoCNAE:

1. criar pacote concreto próprio no executor OPRM, por exemplo `com.marketinghub.nichocnae.socialbehaviorsearcher`;
2. manter a etapa plugável e removível, sem dependência direta de outras etapas concretas;
3. consumir apenas contratos persistidos/DTOs oficiais do backend OPRM;
4. concluir/falhar por endpoint próprio do backend OPRM, nunca por acesso direto ao banco;
5. manter scheduler desativável por configuração, com valor padrão desativado até aprovação explícita;
6. registrar logs de ingestão do payload bruto recebido da fonte antes da normalização;
7. registrar também fonte, endpoint/API, termo pesquisado, timestamp, quantidade de itens recebidos e motivo de descarte quando houver bloqueio;
8. não gravar HTML completo, perfil de usuário, nome de usuário, foto, link de perfil, localização precisa, telefone, e-mail, documento, comentário integral desnecessário ou qualquer dado pessoal sensível;
9. persistir somente sinais agregados e curtos: padrões de linguagem, dores, sonhos, medos, canais, comportamento de aquisição e resumo de evidências;
10. tratar qualquer menção a produto/oferta/solução como risco de contaminação da fase, não como recomendação comercial.

## Contrato mínimo de saída permitido

A saída da etapa opcional deve ser agregada e auditável:

```text
sourceType
sourceName
sourceAccessMethod
permissionStatus
stabilityStatus
researchCycleId
cnaeCode
neutralNicheName
searchTerm
collectedAt
rawPayloadLogReference
itemsReadCount
itemsAcceptedCount
itemsDiscardedCount
languagePatternsSummary
behaviorSignalsSummary
operationalPainsSummary
emotionalPainsSummary
dreamsSummary
fearsSummary
channelsSummary
evidenceFreshnessScore
behavioralEvidenceScore
privacyRiskScore
termsComplianceRiskScore
contaminationRiskScore
sourceUrls
```

Nenhum campo acima deve conter JSON dentro de JSON em texto. Quando houver listas, o contrato deve usar arrays estruturados no DTO oficial ou texto normalizado simples, nunca serialização de JSON em campo textual.

## Critérios de aprovação futura de uma fonte

Uma fonte social/comunitária só pode ser habilitada quando todos os itens abaixo forem verdadeiros:

- há mecanismo oficial ou permissão explícita de acesso;
- os termos permitem o uso pretendido pelo OPRM;
- a fonte é estável o suficiente para operação programada;
- existe política de rate limit e retentativa segura;
- os dados coletados são públicos e necessários ao objetivo da pesquisa;
- o pipeline consegue descartar dados pessoais antes de persistir;
- a saída é agregada e curta;
- há logs de ingestão do payload bruto;
- há teste de regressão garantindo ausência de dados pessoais e ausência de avanço para produto/oferta;
- o Swagger/contrato backend foi atualizado antes da ativação;
- a documentação canônica e o registro OPRM foram atualizados.

## Critérios de bloqueio

A fonte deve continuar bloqueada se houver qualquer um destes pontos:

- necessidade de burlar login, captcha, paywall, bloqueio técnico ou política de acesso;
- scraping proibido ou não autorizado;
- dependência de dados de grupos privados, perfis pessoais ou mensagens privadas;
- impossibilidade de remover identificadores pessoais;
- instabilidade que quebre o pipeline principal;
- coleta de conteúdo integral quando bastaria sinal agregado;
- risco de transformar fala de consumidor em promessa comercial ou oferta.

## Conclusão

A etapa 6 melhora o pipeline ao definir uma fronteira objetiva: comunidades públicas podem ajudar a capturar linguagem e comportamento real, mas só entram no OPRM por fontes permitidas, estáveis e com saída agregada. A próxima etapa funcional recomendada é seguir para a segmentação comportamental MEI/autônomo usando as fontes já permitidas e os sinais persistidos atuais; a coleta social automática deve aguardar aprovação fonte a fonte.
