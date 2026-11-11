# Dependências dos gates da entrega MUSA — 11/11/2026

O PR #5819 teve três recusas do build de Íris por DNS dos repositórios Ubuntu,
enquanto a mesma imagem foi construída localmente. O gate de imagens também
falhou duas vezes por timeout do mirror Azure. Não são defeitos na oferta nem
justificativa para omitir checks. O backend completo, frontend e revisão de
Têmis passaram no HEAD anterior, mas o HEAD final precisa dos seus próprios checks.

Foram comparados retry, mudança de DNS e dependências versionadas. Retry foi
insuficiente; alterar DNS do runner não trata a dependência de índices mutáveis.
A correção segue o runtime Playwright por digest já usado pelos agentes, com
JRE 21 em estágio separado. A fonte DejaVu 2.37 usada nos cards de Íris é
preservada pela distribuição oficial, com SHA-256 fixado e verificação de fontconfig.
Não há instalação APT na imagem. Licença acompanha as fontes.

O gate de imagens reutiliza ripgrep existente ou instala a distribuição estática
oficial 14.1.1, com SHA-256 fixado, somente no diretório temporário e PATH do job.
A extração seleciona apenas o executável esperado. Hash inválido, rede indisponível
ou arquivo inválido impedem a validação; nenhum check é dispensado. Python 3,
fornecido pelo runner, é conferido antes do preparo. Não há escrita em binários globais.

Validações locais: 21 testes do gate/instalador, download oficial real com integridade
e execução PCRE2, Bash/ShellCheck, testes de Íris (157 casos, cinco dependentes de
integração não aplicável dispensados), build Docker e smoke como usuário sem root,
filesystem somente leitura, sem rede, com Java, Node, Git, Codex e fonte DejaVu.
Os testes não usam credenciais produtivas, inferência, vídeo ou mídia.

Essa melhoria remove uma causa de interrupção dos agentes/gates e não demonstra
crescimento de vendas. Uma falha futura de GitHub/registry continua sendo falha
externa; não habilita bypass, publicação manual ou execução paga.


O mesmo DNS bloqueou o job físico dos processos ao reinstalar fontes opcionais
por `playwright install --with-deps`. Os logs confirmaram as bibliotecas de execução
já instaladas; os gates passam a instalar o Chromium da versão do lock sem refazer
APT. A matriz desktop/mobile local já executou com o Chromium disponibilizado pela
sandbox. A instalação adicional de um browser no workspace ficou aguardando o
controle de capacidade; isso não substitui nem invalida essa matriz executada.

O frontend PDE também reinstalava FFmpeg incondicionalmente. Agora reutiliza
ffmpeg/ffprobe disponíveis ou extrai os binários da imagem estática 7.1.1 por digest
já usada em Têmis. O container de extração nunca é iniciado e é removido ao sair.
Quatro testes cobrem reúso, extração, falha fechada e ausência de APT nos browsers.
A extração oficial e execução dos binários também passaram localmente. Nenhum
vídeo foi gerado por essa validação.

A validação final do bundle detectou também a referência ao workflow PDE em três
atestações históricas vigentes. Um lote de publicação não interrompeu após essa
recusa local como deveria; nenhum merge ou deploy ocorreu. O lote seguinte usa
falha imediata, revalida o bundle antes de commit e preserva cada manifesto anterior.
As sucessoras de compatibilidade alteram apenas a prova do workflow e acrescentam
os testes determinísticos do preparo; não alteram experiências ou autorizações.


A execução real do gerador SVG encontrou uma incompatibilidade adicional: o FFmpeg
estático extraído da imagem 7.1.1 respondia `-version`, mas encerrava com SIGSEGV
no decoder SVG, também reproduzido na sandbox. O preparo agora usa a distribuição
GNU/glibc do FFmpeg 8.1, build imutável `autobuild-2026-11-11-12-39`, da organização
FFmpeg-Builds, com SHA-256 fixado. A seleção anterior foi descartada para este gate;
a imagem original de Têmis não foi alterada. O download e a extração reais passaram.

O preparo só expõe os binários ao job após renderizar a referência SVG real e
conferir o PNG 1080×1920. Seis regressões cobrem também dimensões inválidas e hash
incorreto; o gerador Product UGC completo passou localmente com os binários novos.
Arquivos publicáveis gerados nessa prova foram restaurados aos bytes anteriores;
não houve publicação de peça nem mudança da oferta pela homologação.
