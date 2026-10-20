# 2.5 Dropbox e Nextcloud

Poucos serviços digitais são tão cotidianos quanto a pasta sincronizada. O usuário salva um
documento no computador do trabalho e o encontra, minutos depois, no celular e no *notebook* de
casa. A simplicidade dessa experiência esconde um problema clássico de sistemas distribuídos:
manter várias cópias de um mesmo dado coerentes entre dispositivos que se desconectam, editam
*offline*, voltam a se conectar em ordem imprevisível e, eventualmente, modificam o mesmo arquivo
ao mesmo tempo. Xu (2020, cap. 15) resume o requisito central desses sistemas em uma frase
categórica: a perda de dados é inaceitável. Esta seção apresenta o par estudado ao longo do livro
para esse domínio: o Dropbox, sistema real de código fechado, e o Nextcloud, seu gêmeo de código
aberto.

## 2.5.1 O sistema real: Dropbox e Google Drive

O Dropbox popularizou a ideia de uma pasta no computador que se mantém igual em todos os
dispositivos. Em 2016, a empresa informou ter mais de 500 milhões de cadastros e armazenar mais de
500 PB de dados de usuários (GUPTA, 2016). Sua equipe de engenharia publicou, no Dropbox Tech
Blog, relatos detalhados sobre duas decisões arquiteturais de grande porte, que este livro
utiliza como fontes primárias.

A primeira foi a saída do Amazon S3. Durante anos, os metadados e os servidores web do Dropbox
ficaram em centros de dados administrados pela empresa, enquanto o conteúdo dos arquivos era
armazenado no S3. Em 2013, a empresa iniciou o projeto *Magic Pocket*, um sistema de
armazenamento de blocos próprio, e, em outubro de 2015, passou a servir mais de 90% dos dados dos
usuários a partir dessa infraestrutura (GUPTA, 2016). Cowling (2016) descreve o sistema
resultante: blocos imutáveis de até 4 MB, comprimidos, cifrados, endereçados pelo *hash* SHA-256
do conteúdo e replicados entre zonas geográficas distintas. A decisão é examinada na
[Seção 4.5](../capitulo-4-50-mil-pes/nextcloud.md).

A segunda foi a reescrita do motor de sincronização. Jayakar (2020) relata que o motor antigo,
chamado *Sync Engine Classic*, chegou a um ponto em que correções incrementais não bastavam, e que
a empresa levou cerca de quatro anos para construir e entregar a todos os usuários um novo motor,
de codinome *Nucleus*, escrito em Rust. Goldberg (2020) descreve como o novo motor representa o
estado da conta por meio de três árvores, remota, local e sincronizada, e como foi testado. O
caso é discutido na [Seção 5.5](../capitulo-5-10-mil-pes/nextcloud.md).

O Google Drive aparece neste livro por um motivo diferente: é o sistema que Xu (2020, cap. 15)
escolhe para o estudo de caso de projeto de um serviço de armazenamento e sincronização de
arquivos. O projeto de Xu, explicitamente inspirado em decisões publicadas pelo Dropbox, como o
tamanho máximo de bloco e o uso de *long polling*, serve como "projeto de referência" com o qual o
Nextcloud é comparado nas altitudes seguintes. É importante não confundir esse projeto didático
com a arquitetura real do Google Drive, que não é descrita no livro de Xu.

## 2.5.2 O gêmeo open source: Nextcloud

O Nextcloud é uma plataforma de arquivos e colaboração que organizações instalam em seus próprios
servidores. Ele surgiu em 2016 como uma bifurcação (*fork*) do ownCloud, um projeto de código
aberto iniciado anos antes com o objetivo de oferecer uma alternativa auto-hospedada aos serviços
comerciais de nuvem. A herança é visível no próprio código: os cabeçalhos de licença de arquivos
centrais, como `lib/public/Files/Storage/IStorage.php`, registram direitos autorais de 2016 da
ownCloud, Inc. ao lado dos da Nextcloud GmbH (NEXTCLOUD, 2026a). O produto cresceu de um servidor
de arquivos para uma plataforma com calendário, contatos, bate-papo e edição de documentos,
quase tudo entregue na forma de *apps*.

**Licença e comunidade.** O servidor é distribuído sob a licença AGPL-3.0, que exige a
disponibilização do código-fonte também quando o software é oferecido como serviço em rede; o
cliente *desktop* é distribuído sob a GPL-2.0. Em outubro de
2026, o repositório `nextcloud/server` no GitHub reunia mais de 37 mil estrelas e mais de 1.400
contribuidores registrados. O desenvolvimento é coordenado pela Nextcloud GmbH, empresa alemã que
vende suporte e assinaturas empresariais, com participação de uma comunidade ampla de
desenvolvedores independentes e de outras empresas.

**Tecnologias.** A Tabela 2.5.1 reúne a ficha técnica das versões adotadas. Diferentemente dos
pares anteriores, o sistema estudado tem dois artefatos principais, o servidor e o cliente
*desktop*, escritos em linguagens distintas e versionados em repositórios separados. A ficha
também evidencia a principal consequência de ser um **produto** instalado por terceiros: em quase
todas as linhas há alternativas (vários bancos, vários armazenamentos, vários servidores web), e
cabe a cada organização escolher entre elas. O protocolo de sincronização, ao contrário, é um
padrão aberto, o WebDAV, e não uma API própria.

**Tabela 2.5.1** – Ficha técnica do Nextcloud v35.0.2 (servidor) e v35.0.0 (cliente *desktop*).

| Item | Descrição |
|---|---|
| **Linguagem(ns) do servidor/núcleo** | PHP 8.3 a 8.5 (servidor, na versão 35); C++ (cliente *desktop*); Rust (`notify_push`, opcional) |
| ***Frameworks* principais** | núcleo próprio, estendido por *apps* por meio de uma API pública; sabre/dav (WebDAV, CalDAV e CardDAV); Vue 3 (interface web); Qt (cliente *desktop*) |
| **Interface e clientes** | interface web em JavaScript; cliente *desktop* de sincronização (C++/Qt); aplicativos móveis |
| **Banco(s) de dados** | MariaDB, MySQL ou PostgreSQL; SQLite apenas para testes e instâncias mínimas; Oracle em assinatura empresarial |
| ***Cache*, filas e mensageria** | APCu (*cache* local); Redis (*cache* distribuído e *locks* transacionais); tarefas em segundo plano enfileiradas no banco e executadas pelo `cron.php` |
| **Armazenamento de arquivos/objetos** | primário em disco local ou armazenamento de objetos (S3, Swift, Azure); armazenamentos externos (S3, SMB, SFTP) pelo *app* `files_external` |
| **Protocolos e APIs expostas** | WebDAV (RFC 4918) para arquivos; CalDAV e CardDAV; API OCS; WebSocket opcional com o `notify_push` |
| **Execução e implantação** | Apache 2.4 ou nginx com PHP-FPM; Nextcloud All-in-One (contêineres Docker administrados por um contêiner mestre, ou arquivo Docker Compose do AIO); vários servidores de aplicação atrás de um balanceador |
| **Licença (da versão adotada)** | AGPL-3.0 (servidor); GPL-2.0 (cliente *desktop*) |
| **Versão adotada no livro (*tag*)** | [`v35.0.2`](https://github.com/nextcloud/server/tree/v35.0.2) (`nextcloud/server`); [`v35.0.0`](https://github.com/nextcloud/desktop/tree/v35.0.0) (`nextcloud/desktop`) |

Fonte: elaborado pelo autor (2026), com base em Nextcloud (2026a; 2026b; 2026c) e nas Seções 4.5
e 5.5.

**Como executar localmente.** O método de instalação que o próprio projeto apresenta como oficial
é o **Nextcloud All-in-One** (AIO), um conjunto de contêineres Docker administrado por um
"contêiner mestre", que instala, executa e atualiza os demais por meio de uma interface web
(NEXTCLOUD, 2026c). Em uma máquina Linux sem servidor web prévio, o início rápido documentado é o
seguinte.

**Listagem 2.5.1** – Comandos (shell, Nextcloud): início rápido do Nextcloud All-in-One.

```bash
sudo docker run \
  --init \
  --sig-proxy=false \
  --name nextcloud-aio-mastercontainer \
  --restart always \
  --publish 80:80 \
  --publish 8080:8080 \
  --publish 8443:8443 \
  --volume nextcloud_aio_mastercontainer:/mnt/docker-aio-config \
  --volume /var/run/docker.sock:/var/run/docker.sock:ro \
  ghcr.io/nextcloud-releases/all-in-one:latest
```

Fonte: Nextcloud (2026c).

Depois de iniciado, o assistente web na porta 8080 cria os contêineres do Nextcloud, do banco
PostgreSQL, do Redis e, opcionalmente, de componentes adicionais, como o *backend* de alto
desempenho para arquivos (`notify_push`), o editor de documentos e o antivírus. A documentação do
AIO recomenda, para uso em produção, o arquivo Docker Compose fornecido no repositório em vez do
comando `docker run`, por facilitar atualizações. Para quem deseja apenas explorar o código, uma
alternativa é clonar o repositório e seguir o guia de ambiente de desenvolvimento da
documentação para desenvolvedores.

**Versões adotadas no livro.** Nomes de pastas, classes e arquivos mudam entre versões. Por isso,
este livro fixa as duas *tags* indicadas na Tabela 2.5.1, uma para o servidor e outra para o
cliente *desktop*. Todos os caminhos e trechos de código citados nas Seções 3.5 a 6.5
foram conferidos nessas versões. O AIO instala sempre a versão estável mais recente, que pode ser
posterior; a diferença não altera a arquitetura discutida.

## 2.5.3 Por que este par

O par Dropbox e Nextcloud foi escolhido porque os dois sistemas resolvem o **mesmo problema de
domínio** a partir de **contextos opostos**, e o contraste torna visível o papel do contexto nas
decisões arquiteturais.

O Dropbox é um **serviço** único, operado por uma única empresa para centenas de milhões de
usuários. Ele pode otimizar toda a pilha para um único ambiente, a ponto de construir *hardware* e
software de armazenamento próprios (GUPTA, 2016). Suas características prioritárias são a
durabilidade, a eficiência da sincronização e o custo por *byte*.

O Nextcloud é um **produto** instalado por milhares de organizações diferentes, de universidades e
órgãos públicos a pequenas empresas, cada uma com necessidades, infraestrutura e escala próprias.
Ele precisa funcionar sobre servidores e armazenamentos que o fabricante não controla e permitir
que cada organização escolha as funcionalidades de que precisa. Richards e Ford (2020, p. 149)
observam que o estilo **microkernel** é um ajuste natural para aplicações baseadas em produto,
instaladas no ambiente do cliente. O Nextcloud é um exemplo característico dessa observação: um
núcleo estável estendido por *apps* que obedecem a uma API pública.

O dilema arquitetural que o par ilustra pode ser formulado assim: **construir ou abstrair?** Diante
do armazenamento, o Dropbox construiu um sistema próprio; o Nextcloud abstraiu os armazenamentos
existentes por meio de um contrato comum. Diante das funcionalidades, o Dropbox as integra em um
serviço controlado por uma única equipe; o Nextcloud as delega a *apps* que podem ser escritos por
terceiros. Ambos compartilham, porém, as decisões impostas pelo domínio: separar metadados de
conteúdo, detectar mudanças de forma eficiente e jamais descartar uma edição em caso de conflito.
O par serve ainda para estudar, no código, padrões clássicos em uso real: *Adapter* nos
armazenamentos externos, *Decorator* nos *wrappers* de cota e criptografia, *Plugin* no registro
de *apps* e *Composite* na árvore de arquivos.

## 2.5.4 Ficha *kata*

O formato de ficha *kata* descreve o problema como um exercício de arquitetura, independentemente
da solução existente (RICHARDS; FORD, 2020, cap. 5).

- **Descrição:** armazenamento e sincronização de arquivos entre dispositivos, com histórico de
  versões, compartilhamento e colaboração.
- **Usuários:** pessoas e organizações. O Nextcloud é adotado sobretudo por organizações que
  precisam manter os dados sob seu controle, como universidades, órgãos públicos e empresas
  sujeitas a regras de proteção de dados.
- **Requisitos:** sincronizar pastas entre dispositivos; manter versões anteriores; compartilhar
  por *link* e com outros usuários; permitir edição *offline*; integrar armazenamentos e
  diretórios de usuários já existentes na organização; acrescentar funcionalidades como
  calendário, contatos e edição de documentos.
- **Contexto adicional:** arquivo perdido é inaceitável; usuários editam *offline* e em vários
  dispositivos; cada organização quer um conjunto diferente de funcionalidades e de
  armazenamentos; a instalação e a atualização são feitas por equipes de TI das próprias
  organizações, com recursos variados.

O último item da ficha distingue este sistema dos demais estudados no livro. No Mastodon
(Seção 2.2) e no Mattermost (Seção 2.3), a variabilidade entre instalações é sobretudo de escala.
No Nextcloud, ela é também **funcional**: uma universidade pode precisar integrar o diretório LDAP
institucional e um servidor de arquivos SMB legado, enquanto uma pequena empresa quer apenas uma
pasta compartilhada e um calendário. Essa variabilidade é o principal indutor do estilo
microkernel. A [Seção 3.5](../capitulo-3-100-mil-pes/nextcloud.md) retoma a ficha na forma de uma
conversa com o cliente, da qual emergem os requisitos, as estimativas e as características
prioritárias.

## Referências

COWLING, J. **Inside the Magic Pocket**. Dropbox Tech Blog, 6 maio 2016. Disponível em:
<https://dropbox.tech/infrastructure/inside-the-magic-pocket>. Acesso em: 18 out. 2026.

GOLDBERG, I. **Testing sync at Dropbox**. Dropbox Tech Blog, 20 abr. 2020. Disponível em:
<https://dropbox.tech/infrastructure/-testing-our-new-sync-engine>. Acesso em: 18 out. 2026.

GUPTA, A. **Scaling to exabytes and beyond**. Dropbox Tech Blog, 14 mar. 2016. Disponível em:
<https://dropbox.tech/infrastructure/magic-pocket-infrastructure>. Acesso em: 18 out. 2026.

JAYAKAR, S. **Rewriting the heart of our sync engine**. Dropbox Tech Blog, 9 mar. 2020.
Disponível em: <https://dropbox.tech/infrastructure/rewriting-the-heart-of-our-sync-engine>.
Acesso em: 18 out. 2026.

NEXTCLOUD. **nextcloud/server**: repositório de código-fonte, versão v35.0.2. GitHub, 2026a.
Disponível em: <https://github.com/nextcloud/server/tree/v35.0.2>. Acesso em: 18 out. 2026.

NEXTCLOUD. **System requirements**. Nextcloud 35 Administration Manual, 2026b. Disponível em:
<https://docs.nextcloud.com/server/35/admin_manual/installation/system_requirements.html>.
Acesso em: 18 out. 2026.

NEXTCLOUD. **nextcloud/all-in-one**: Nextcloud All-in-One. GitHub, 2026c. Disponível em:
<https://github.com/nextcloud/all-in-one>. Acesso em: 18 out. 2026.

RICHARDS, M.; FORD, N. **Fundamentals of software architecture**: an engineering approach.
Sebastopol: O'Reilly, 2020.

XU, A. **System design interview**: an insider's guide. 2. ed. [*S. l.*]: Byte Code LLC, 2020.
