# IA Generativa no Judiciário Trabalhista: Galileu, Chat-JT e oportunidades de desenvolvimento

**Data:** 13 de maio de 2026  
**Escopo:** resumo técnico e estratégico sobre a IA Galileu, o uso de IA generativa na Justiça do Trabalho, sinais de adoção por outros tribunais e oportunidades de desenvolvimento associadas.

---

## 1. Resumo executivo

O **Galileu** é uma ferramenta de inteligência artificial generativa desenvolvida originalmente pelo **Tribunal Regional do Trabalho da 4ª Região (TRT-4)** e depois autorizada pelo **Conselho Superior da Justiça do Trabalho (CSJT)** para uso nacional na Justiça do Trabalho.

Sua função principal é apoiar magistrados e servidores na **elaboração de minutas de sentenças**, realizando a leitura automática de peças processuais, estruturando informações e sugerindo tópicos organizados da decisão.

A ferramenta não substitui o juiz. O seu papel oficial é **auxiliar a atividade jurisdicional**, com revisão humana. Em termos práticos, ela funciona como um **assistente de gabinete judicial**.

O caso noticiado sobre a inserção de um “comando oculto” em texto branco dentro de uma petição mostra tanto o potencial quanto os riscos desse tipo de tecnologia. A técnica se aproxima de um **prompt injection**, isto é, uma tentativa de inserir instruções escondidas dentro de um documento para influenciar o comportamento de uma IA.

A oportunidade de desenvolvimento não está apenas em “criar um chatbot”. O valor real está em construir sistemas com:

- leitura documental;
- extração e estruturação de dados;
- busca em bases jurídicas confiáveis;
- geração controlada de textos;
- segurança contra manipulação;
- auditoria;
- revisão humana;
- governança de dados.

---

## 2. Contexto: o caso do “código secreto” em petição

A notícia analisada relata que advogadas teriam inserido em uma petição um texto oculto, em **fonte branca sobre fundo branco**, com uma instrução direcionada a sistemas de IA. A ideia técnica por trás disso é simples: o texto pode ficar invisível para humanos, mas continuar presente na camada textual do arquivo, podendo ser lido por ferramentas automáticas.

Esse tipo de técnica é conhecido como **prompt injection**. Em vez de invadir um sistema, o atacante tenta influenciar a IA por meio do próprio conteúdo que ela está lendo.

No caso, a lógica seria:

1. uma petição contém um comando escondido;
2. uma IA lê o documento como parte do fluxo de análise;
3. se o sistema não estiver protegido, pode confundir o comando escondido com uma instrução legítima;
4. o resultado gerado pela IA pode ser enviesado, incompleto ou manipulado.

Do ponto de vista de segurança, a lição é clara: documentos externos, como petições, contratos e anexos, devem ser tratados como **conteúdo não confiável**, e não como fonte de instruções para o modelo.

---

## 3. O que é o Galileu

O Galileu é uma solução de IA generativa voltada à Justiça do Trabalho. Segundo o CSJT, ele foi desenvolvido pelo TRT-4 e autorizado para uso em toda a Justiça do Trabalho.

A ferramenta realiza a leitura automática de **petições iniciais** e **contestações**, organiza as informações de forma estruturada e sugere minutas com os tópicos da sentença em ordem lógica. Em cada item, pode inserir detalhes dos pedidos e argumentos das partes, além de subsídios como jurisprudência do TST, precedentes qualificados e decisões anteriores do próprio magistrado.

Em resumo, o Galileu faz:

| Função | Descrição |
|---|---|
| Leitura de peças processuais | Analisa petições, contestações e documentos relevantes |
| Estruturação do processo | Organiza pedidos, argumentos e temas jurídicos |
| Sugestão de relatórios | Produz resumos e relatórios iniciais |
| Apoio à minuta | Sugere estrutura de decisão e capítulos da sentença |
| Apoio à fundamentação | Usa textos de gabinete, precedentes e jurisprudência |
| Revisão humana | O resultado deve ser analisado e validado por magistrados/servidores |

---

## 4. Como essa IA parece ter sido construída

As informações públicas não revelam a arquitetura completa do Galileu, nem indicam qual modelo de linguagem específico é usado por baixo. Ainda assim, a descrição oficial permite inferir uma arquitetura aproximada.

A estrutura provável é:

```text
PJe / documentos processuais
        ↓
leitura automática e extração textual
        ↓
classificação e estruturação dos temas
        ↓
consulta a bases internas e PangeaGab
        ↓
modelo de IA generativa
        ↓
sugestão de relatório, tópicos e minuta
        ↓
revisão humana pelo gabinete
```

### 4.1. Camada de entrada

A entrada do sistema são documentos do processo: petição inicial, contestação, anexos e demais peças. A ferramenta precisa transformar esse material em texto processável.

Essa camada exige:

- extração de texto de PDFs e documentos;
- tratamento de OCR quando há imagem;
- identificação de partes, pedidos, fatos e fundamentos;
- limpeza de ruído textual;
- detecção de conteúdo suspeito, como texto invisível.

### 4.2. Camada de estruturação

Depois da leitura, a IA precisa organizar o processo em elementos jurídicos. Isso inclui:

- pedidos do reclamante;
- argumentos da defesa;
- provas/documentos;
- pontos controvertidos;
- temas jurídicos;
- precedentes aplicáveis;
- estrutura provável da sentença.

### 4.3. Camada de conhecimento

O Galileu se apoia em bases internas. Uma base importante citada é o **PangeaGab**, que reúne textos de gabinete, precedentes qualificados e avaliações de jurisprudência consolidada no TST.

Essa camada é essencial porque reduz o risco de a IA gerar respostas “da cabeça dela”. O sistema passa a usar conteúdo curado, validado e alinhado à prática do tribunal ou do gabinete.

### 4.4. Camada generativa

A IA generativa transforma os dados estruturados e as referências recuperadas em texto: relatório, tópicos, minuta de decisão ou sugestões de fundamentação.

Essa etapa é parecida com um modelo de **RAG jurídico** (*Retrieval-Augmented Generation*): primeiro o sistema busca informações relevantes em bases confiáveis; depois usa um modelo de linguagem para redigir uma resposta.

Essa classificação como RAG é uma **inferência técnica**, não uma descrição oficial integral da arquitetura.

### 4.5. Camada de governança

Para uso institucional, a ferramenta precisa de controles como:

- autenticação;
- controle de acesso;
- logs de uso;
- versionamento;
- rastreabilidade de fontes;
- identificação de uso de IA;
- auditoria;
- capacitação de usuários;
- supervisão humana.

---

## 5. O Galileu usa ChatGPT?

Não encontrei fonte oficial pública dizendo que o Galileu usa **ChatGPT** ou modelos da OpenAI.

O que as fontes oficiais indicam é que o Galileu é uma ferramenta institucional de IA generativa da Justiça do Trabalho, desenvolvida pelo TRT-4 e integrada ao ecossistema tecnológico da Justiça do Trabalho.

Isso não impede, tecnicamente, que sistemas desse tipo usem modelos comerciais, modelos abertos ou modelos próprios. Mas, no caso específico do Galileu, a informação pública disponível não identifica o fornecedor ou modelo de linguagem subjacente.

A Justiça do Trabalho também possui o **Chat-JT**, que é a ferramenta institucional de chat com IA generativa. A orientação institucional parece caminhar para evitar o uso descontrolado de contas pessoais em ferramentas públicas e privilegiar soluções corporativas monitoradas, com governança e proteção de dados.

---

## 6. Somente o TRT-8 usa o Galileu?

Não. O TRT-8 aparece no caso noticiado porque o episódio ocorreu no Pará, mas o Galileu não é exclusivo do TRT-8.

O CSJT informou que o Galileu foi nacionalizado e autorizado para uso em todos os Tribunais Regionais do Trabalho. A tecnologia começou a ser testada no TRT-4 em 2023 e foi depois expandida nacionalmente.

Além disso, representantes de todos os **24 tribunais trabalhistas** participaram de formação nacional de multiplicadores do projeto.

### 6.1. Tribunais citados em fontes públicas

| Tribunal | Informação encontrada |
|---|---|
| **TRT-4 / RS** | Desenvolveu o Galileu. |
| **TRT-2 / SP** | Citado como integrante do projeto-piloto e participante de capacitações. |
| **TRT-14 / RO-AC** | Citado como integrante do projeto-piloto. |
| **TRT-18 / GO** | Citado como integrante do projeto-piloto. |
| **TRT-8 / PA-AP** | Firmou convênio com o TRT-4 para compartilhamento do Galileu e instituiu ferramentas oficiais de IA, incluindo Chat-JT, Galileu, Mídias JT e Degrava. |
| **TRT-16 / MA** | Informou entrada em operação de Galileu e PangeaGab. |
| **TRT-6 / PE** | Abriu curso sobre Galileu e PangeaGab, indicando processo de nacionalização. |
| **TRT-15 / Campinas** | Registros internos mencionam PangeaGab e Galileu para padronização de textos e minutas. |
| **TRT-1 / RJ** | Há registros de curso Galileu/PangeaGab para magistrados e servidores. |
| **TRT-3 / MG** | Há registros de curso de introdução ao Galileu como ferramenta de IA adotada pela Justiça do Trabalho. |

Atenção: estar citado em curso, treinamento ou projeto não significa necessariamente o mesmo nível de maturidade de uso em todos os tribunais. Alguns podem estar em produção, outros em capacitação, piloto ou implantação gradual.

---

## 7. O TRT-8 e as ferramentas oficiais de IA

O TRT-8 publicou que instituiu, por meio da Portaria PRESI nº 1.296/2025, o uso oficial das ferramentas:

- **Chat-JT**;
- **Galileu**;
- **Mídias JT**;
- **Degrava**.

Segundo o TRT-8, essas ferramentas se tornaram opções oficiais para magistrados e servidores em todas as fases e instâncias da atividade jurisdicional, com suporte técnico e capacitação institucional.

Isso mostra um movimento importante: a IA não está sendo tratada apenas como experimento isolado, mas como parte do fluxo oficial de trabalho.

---

## 8. Outros tribunais e outras IAs no Judiciário brasileiro

O Galileu é específico da Justiça do Trabalho, mas o Judiciário brasileiro possui várias outras iniciativas de IA.

### 8.1. Panorama nacional

O CNJ informou que **45,8% dos tribunais e conselhos brasileiros** afirmavam utilizar IA generativa em suas operações. Entre os que ainda não utilizavam, **81,3%** planejavam integrar essas ferramentas nos anos seguintes.

Os usos mais comuns estão relacionados a:

- análise de documentos;
- sumarização;
- produção de textos;
- pesquisa de jurisprudência;
- padronização documental;
- detecção de inconsistências;
- redução de tarefas administrativas repetitivas.

### 8.2. STJ Logos

O **Superior Tribunal de Justiça (STJ)** lançou o **STJ Logos**, uma ferramenta de IA generativa voltada aos gabinetes. A ferramenta foi apresentada como apoio para agilizar a análise processual e a elaboração de minutas de decisão.

### 8.3. Victor e outras ferramentas no STF

O **Supremo Tribunal Federal (STF)** tem histórico de uso de IA desde o projeto **Victor**, usado para apoiar a triagem de recursos e a identificação de temas relacionados à repercussão geral.

O STF também desenvolveu outras soluções, como o **VitórIA**, voltado a agrupamento e classificação de processos.

### 8.4. Sinapses/CNJ

A plataforma **Sinapses**, ligada ao CNJ e ao Programa Justiça 4.0, funciona como catálogo, ambiente de treinamento e consumo de modelos de IA para órgãos de Justiça.

Em 2023, havia **150 modelos de IA ativos**, produzidos por **29 tribunais e conselhos**, depositados na plataforma Sinapses.

Esse ponto é estratégico: o Judiciário brasileiro não depende apenas de soluções isoladas. Há uma tentativa de criar um ecossistema nacional de modelos, padrões, reaproveitamento e auditoria.

---

## 9. Governança e regras aplicáveis

A principal norma recente é a **Resolução CNJ nº 615/2025**, que define diretrizes para desenvolvimento, governança, auditoria, monitoramento e uso responsável de IA no Poder Judiciário.

A norma reforça princípios como:

- transparência;
- auditabilidade;
- explicabilidade;
- contestabilidade;
- segurança jurídica;
- segurança da informação;
- proteção de dados;
- supervisão humana;
- prevenção de vieses;
- registro e monitoramento.

A Resolução também prevê que LLMs e sistemas de IA generativa disponíveis na internet podem ser usados por magistrados e servidores como ferramentas de auxílio à gestão ou apoio à decisão, mas preferencialmente por meio de acesso habilitado, disponibilizado e monitorado pelos tribunais.

A norma veda o uso autônomo da IA como instrumento de tomada de decisão judicial sem orientação, interpretação, verificação e revisão humana.

---

## 10. Oportunidades de desenvolvimento

O caso Galileu revela um conjunto de oportunidades relevantes para empresas, startups, equipes de inovação pública e escritórios jurídicos.

### 10.1. Leitura inteligente de documentos

Há demanda por sistemas capazes de ler e organizar grandes volumes de documentos jurídicos ou administrativos.

Possíveis produtos:

- leitor de petições;
- leitor de contratos;
- extrator de cláusulas;
- classificador de documentos;
- comparador de versões;
- identificador de pedidos e riscos.

### 10.2. RAG jurídico e bases internas curadas

Ferramentas como Galileu/PangeaGab mostram o valor de bases internas controladas. Empresas e órgãos públicos podem criar repositórios próprios com:

- modelos de documentos;
- pareceres;
- decisões;
- políticas internas;
- jurisprudência;
- entendimentos consolidados;
- histórico de casos.

A oportunidade é criar **IA especializada no conhecimento da organização**, não apenas uma IA genérica.

### 10.3. Copilotos especializados

O mercado tende a migrar de chatbots genéricos para **copilotos de função**.

Exemplos:

| Copiloto | Função |
|---|---|
| Copiloto jurídico | Contratos, petições, pareceres e jurisprudência |
| Copiloto de compliance | Políticas, normas, riscos e auditoria |
| Copiloto de RH | Triagem de documentos, respostas internas, políticas |
| Copiloto financeiro | Relatórios, conciliações e análise de contratos |
| Copiloto de atendimento | Respostas padronizadas e consulta a base interna |
| Copiloto de gabinete | Minutas, relatórios, precedentes e organização de processos |

### 10.4. Segurança contra prompt injection

O caso do texto invisível mostra uma oportunidade clara: criar ferramentas de segurança para documentos que serão lidos por IA.

Funcionalidades possíveis:

- detecção de texto branco sobre fundo branco;
- detecção de texto oculto ou sobreposto;
- detecção de caracteres invisíveis;
- análise de metadados suspeitos;
- separação entre “conteúdo” e “instrução”;
- limpeza de PDFs e DOCX antes da IA;
- alerta de prompt injection;
- quarentena de documentos suspeitos.

Essa pode se tornar uma categoria própria de produto: **firewall documental para IA**.

### 10.5. Auditoria e rastreabilidade

Órgãos públicos e empresas precisarão provar que seus sistemas de IA são seguros, auditáveis e usados de forma responsável.

Oportunidades:

- logs estruturados de uso;
- trilhas de auditoria;
- avaliação de qualidade de respostas;
- testes de viés;
- controle de versões de prompts;
- relatórios de conformidade;
- dashboards de risco;
- registro de uso de IA em documentos.

### 10.6. Treinamento e letramento em IA

A adoção de IA exige capacitação. Há espaço para programas de treinamento sobre:

- boas práticas de prompt;
- limites de modelos generativos;
- revisão crítica de respostas;
- proteção de dados;
- riscos de alucinação;
- prompt injection;
- governança;
- ética e responsabilidade.

### 10.7. Integração com sistemas existentes

A maior oportunidade técnica está na integração com sistemas que as instituições já usam:

- PJe;
- sistemas de gestão documental;
- CRMs;
- ERPs;
- bancos de jurisprudência;
- portais internos;
- bases de conhecimento;
- ferramentas de BI.

A IA ganha valor quando opera dentro do fluxo real de trabalho.

---

## 11. Riscos e cuidados

### 11.1. Alucinação

Modelos generativos podem produzir respostas falsas, imprecisas ou juridicamente incorretas. A mitigação exige bases confiáveis, validação humana e rastreabilidade das fontes.

### 11.2. Prompt injection

Documentos de terceiros podem conter comandos ocultos para manipular a IA. O sistema deve tratar todo conteúdo externo como não confiável.

### 11.3. Vazamento de dados

Processos podem conter dados pessoais, informações sensíveis e documentos sob sigilo. O uso de IA precisa observar LGPD, segredo de justiça e controles internos.

### 11.4. Excesso de confiança

A IA deve apoiar o trabalho humano, não substituir análise crítica. O risco é operadores aceitarem sugestões sem conferência adequada.

### 11.5. Dependência tecnológica

Soluções baseadas em fornecedores externos precisam considerar soberania, continuidade, custo, auditoria, segurança e capacidade de migração.

### 11.6. Viés

Sistemas de IA podem reproduzir padrões discriminatórios ou inconsistentes. É necessário monitoramento contínuo e avaliação de impacto.

---

## 12. Leitura estratégica: por que isso importa

O Galileu indica uma mudança maior: a IA generativa está deixando de ser ferramenta experimental e passando a integrar fluxos oficiais de trabalho.

A tendência é que organizações criem IAs especializadas para:

- reduzir tempo de análise;
- organizar documentos complexos;
- apoiar decisões;
- padronizar textos;
- preservar conhecimento institucional;
- acelerar atendimento;
- melhorar produtividade;
- reduzir tarefas repetitivas.

A vantagem competitiva estará em combinar:

```text
conhecimento do domínio
+ dados confiáveis
+ integração com sistemas
+ segurança
+ governança
+ boa experiência de uso
+ revisão humana
```

---

## 13. Conclusão

O Galileu não é um caso isolado do TRT-8. Ele faz parte de uma estratégia mais ampla da Justiça do Trabalho para adotar IA generativa em ambiente institucional. O projeto nasceu no TRT-4, foi nacionalizado pelo CSJT e aparece ligado a um ecossistema que inclui Chat-JT, PangeaGab, Mídias JT e Degrava.

No Judiciário brasileiro como um todo, há outras iniciativas relevantes, como STJ Logos, Victor, VitórIA e a plataforma Sinapses.

Do ponto de vista de oportunidades, o principal aprendizado é que o futuro da IA profissional não será apenas “um chat”. O mercado deve caminhar para sistemas especializados, integrados, auditáveis e seguros, capazes de operar sobre documentos complexos e bases internas confiáveis.

O caso do prompt oculto reforça uma segunda lição: quanto mais a IA entra nos fluxos decisórios e documentais, mais importante se torna a segurança do conteúdo que ela lê.

---

# Fontes consultadas

1. CSJT — **Justiça do Trabalho adota nacionalmente ferramenta de IA Galileu para auxiliar a produção de sentenças**  
   https://www.csjt.jus.br/web/csjt/-/justi%C3%A7a-do-trabalho-adota-nacionalmente-ferramenta-de-ia-galileu-para-auxiliar-a-produ%C3%A7%C3%A3o-de-senten%C3%A7as-na-justi%C3%A7a-do-trabalho

2. CSJT — **Sistema Galileu integra IA generativa à Justiça do Trabalho com confiabilidade e transparência**  
   https://www.csjt.jus.br/web/csjt/-/sistema-galileu-integra-ia-generativa-a-justica-do-trabalho-com-confiabilidade-e-transparencia

3. TRT-8 — **Do Oiapoque ao Chuí: TRTs do Rio Grande do Sul, Pará e Amapá se unem para compartilhar ferramentas tecnológicas**  
   https://www.trt8.jus.br/noticias/2025/do-oiapoque-ao-chui-trts-do-rio-grande-do-sul-para-e-amapa-se-unem-para-compartilhar

4. TRT-8 — **TRT-8 estabelece diretrizes para o uso de inteligência artificial no Poder Judiciário**  
   https://www.trt8.jus.br/noticias/2025/trt-8-estabelece-diretrizes-para-o-uso-de-inteligencia-artificial-no-poder-judiciario

5. CNJ — **Resolução CNJ nº 615/2025**  
   https://atos.cnj.jus.br/atos/detalhar/6001

6. CNJ — **IA generativa é utilizada em mais de 45% dos tribunais brasileiros**  
   https://www.cnj.jus.br/ia-generativa-e-utilizada-em-mais-de-45-dos-tribunais-brasileiros/

7. STJ — **Gabinetes conhecem, na prática, funcionamento do STJ Logos**  
   https://www.stj.jus.br/sites/portalp/Paginas/Comunicacao/Noticias/2025/15022025-Gabinetes-conhecem--na-pratica--funcionamento-do-STJ-Logos-.aspx

8. PNUD/CNJ — **Plataforma Sinapses reúne 150 modelos de inteligência artificial**  
   https://www.undp.org/pt/brazil/news/plataforma-sinapses-reune-150-modelos-de-inteligencia-artificial

9. Migalhas — **Juiz multa advogadas que esconderam prompt para enganar IA da Justiça**  
   https://www.migalhas.com.br/quentes/455817/juiz-multa-advogadas-que-esconderam-prompt-para-enganar-ia-da-justica

10. GC Notícias / G1 — **Juiz multa advogadas por inserirem “código secreto” em letra invisível**  
   https://www.gcnoticias.com.br/geral/juiz-multa-advogadas-por-inserirem-codigo-secreto-em-letra-invisivel-para-tentar-enganar-ia-e-sabotar-processo-entenda/249808784
