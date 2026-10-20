# 2.2 Twitter e Mastodon

Uma rede social de mensagens curtas parece, à primeira vista, um sistema trivial: usuários
escrevem textos de poucas centenas de caracteres e leem os textos de quem seguem. A
funcionalidade cabe em um tutorial de *framework* web. A dificuldade não está na funcionalidade,
mas na **assimetria** entre escrita e leitura e na **distribuição desigual** das relações sociais.
Cada publicação precisa aparecer, em segundos, na *timeline* de todos os seguidores do autor, e
alguns autores têm dezenas de milhões de seguidores. Esta seção apresenta o par de sistemas que
o livro usa para estudar esse problema: o Twitter, hoje X, como sistema real de código fechado, e
o Mastodon como gêmeo de código aberto. As Seções 3.2, 4.2, 5.2 e 6.2 descem, altitude por
altitude, sobre esse par.

## 2.2.1 O sistema real: Twitter/X

O Twitter consolidou o formato de *microblog*: mensagens curtas, públicas por padrão, organizadas
numa *timeline* que reúne as publicações das contas seguidas. Em 2013, a empresa informava receber
mais de 500 milhões de publicações por dia, cerca de 5.700 por segundo em média
(KRIKORIAN, 2013); dez anos depois, o número citado pela própria empresa era da mesma ordem, cerca
de 500 milhões por dia (TWITTER, 2023). Em outubro de 2022, a empresa foi adquirida por Elon Musk
por 44 bilhões de dólares e, em julho de 2023, a marca foi substituída por "X" (GOLLOM, 2023). O
livro usa "Twitter" para os fatos anteriores à mudança de marca e "Twitter/X" quando se refere ao
sistema em geral.

A história técnica do Twitter interessa a um curso de arquitetura porque foi relatada
publicamente por seus engenheiros. Durante anos, o sistema foi uma das maiores instalações Ruby on
Rails do mundo: um monólito no qual, segundo Krikorian (2013), tudo, do gerenciamento das conexões
de banco de dados à renderização das páginas e às APIs públicas, residia em uma única base de
código, com cerca de 200 engenheiros contribuindo. A Copa do Mundo de 2010 expôs os limites dessa
arquitetura: picos de publicações deixavam o serviço indisponível por curtos períodos, e os ganhos
de eficiência obtidos às pressas eram engolidos pelo crescimento. A partir daí, a empresa
decompôs o monólito em serviços na JVM, em torno de publicações, *timelines* e usuários
(KRIKORIAN, 2013). Fox e Patterson (2021, cap. 3) registram o mesmo episódio como exemplo de
microsserviços que surgem pela divisão de um serviço grande: por volta de 2013, o monólito Rails,
apelidado internamente de *MonoRail*, foi dividido em muitos microsserviços escritos
principalmente em Java, Scala e Clojure.

Para a *timeline*, o Twitter adotou a estratégia de pré-computar, no momento da escrita, a lista de
publicações de cada usuário, mantida em memória num *cluster* Redis; para algumas contas muito
seguidas, a mescla era feita no momento da leitura (KRIKORIAN, 2012; HOFF, 2013). Em 2023, ao
publicar o código do seu algoritmo de recomendação, a empresa informou ter abandonado o serviço de
*fanout* que mantinha essas listas havia 12 anos, substituído por um *pipeline* de recomendação
executado a cada leitura (TWITTER, 2023). Esses dois momentos, a pré-computação na escrita e o
retorno à montagem na leitura, são o fio condutor do estudo deste par nos capítulos seguintes.

O Twitter/X é um sistema fechado. Tudo o que o livro afirma sobre ele vem de fontes públicas: o
blog de engenharia da empresa, palestras documentadas e obras de referência. Onde as fontes não
dizem nada, o livro não supõe.

## 2.2.2 O gêmeo de código aberto: Mastodon

### Origem e governança

O Mastodon começou em 2016 como projeto de código aberto de Eugen Rochko, usuário do Twitter
desde 2008 e insatisfeito com o estado e a direção daquela plataforma; o primeiro lançamento
público ocorreu em outubro de 2016, e o apoio inicial veio de financiamento coletivo
(MASTODON, 2026a). O repositório no GitHub foi criado em fevereiro de 2016. O projeto foi
constituído como uma gGmbH, forma alemã de sociedade sem fins lucrativos, em 2021, e hoje é
apoiado também por uma entidade sem fins lucrativos nos Estados Unidos (MASTODON, 2026a). Em
outubro de 2026, o repositório principal reunia mais de 50 mil estrelas e 7 mil *forks* no GitHub,
indicadores aproximados, mas úteis, do tamanho da comunidade.

O episódio que levou o Mastodon ao grande público foi a aquisição do Twitter. Segundo o próprio
projeto, o número de usuários ativos mensais saltou de aproximadamente 300 mil para 2,5 milhões
entre outubro e novembro de 2022 (ROCHKO, 2022). A Seção 4.2 examina o que esse salto significou
para servidores dimensionados para comunidades pequenas.

### Federação

A diferença essencial entre o Mastodon e o Twitter não está na funcionalidade, mas na
organização. O Mastodon não é um serviço, e sim um software que qualquer pessoa ou organização
pode instalar para criar um servidor, chamado **instância**. As instâncias são operadas de forma
completamente independente por pessoas ou organizações diferentes, e conversam entre si por um
protocolo aberto e padronizado, o ActivityPub (MASTODON, 2026b). A documentação chama esse arranjo
de **federação**, uma forma de descentralização situada entre o modelo centralizado e o
totalmente distribuído, e chama de *fediverso* o conjunto de todos os sites que se comunicam por
ActivityPub, incluindo outros softwares como Pixelfed (imagens) e PeerTube (vídeos), estudado na
Seção 2.4 (MASTODON, 2026b).

O ActivityPub é uma recomendação do W3C publicada em 23 de janeiro de 2018. Ele define uma API
cliente-servidor e um protocolo servidor-servidor de federação, no qual um servidor entrega
atividades (criar, curtir, seguir, apagar) fazendo `POST` na caixa de entrada (*inbox*) de atores
hospedados em outros servidores (LEMMER-WEBBER *et al.*, 2018). O padrão é anterior à adoção
massiva do Mastodon, mas foi o Mastodon que o tornou conhecido.

### Licença e tecnologias

O Mastodon é software livre, licenciado sob a GNU Affero General Public License, versão 3
(AGPLv3), conforme o arquivo `LICENSE` e o `README.md` do repositório (MASTODON, 2026c). A AGPL
estende as obrigações da GPL ao uso pela rede: quem modifica o software e o oferece como serviço
precisa disponibilizar o código modificado aos usuários. Para uma rede federada, a escolha é
coerente, pois as instâncias tendem a convergir para versões abertas do mesmo código.

A Tabela 2.2.1 reúne a ficha técnica do gêmeo na versão estudada, com as versões mínimas das
dependências declaradas no `README.md` (MASTODON, 2026c). Dois traços dessa ficha orientam a
leitura dos capítulos seguintes. O primeiro é que o sistema, embora seja um monólito Rails, não é
um processo único: a API de *streaming* é escrita em outra linguagem e executada à parte, e as
tarefas assíncronas rodam em processos Sidekiq separados. O segundo é que o Redis acumula três
papéis, *cache*, fila e canal de *pub/sub*, o que faz dele uma peça central, e não acessória, da
arquitetura, como mostra a Seção 4.2.

**Tabela 2.2.1** – Ficha técnica do Mastodon v4.7.3.

| Item | Descrição |
|---|---|
| **Linguagem(ns) do servidor/núcleo** | Ruby 3.3 ou superior (aplicação Rails e *workers*); JavaScript sobre Node.js 22 ou superior (servidor de *streaming*) |
| ***Frameworks* principais** | Ruby on Rails, servido pelo Puma; Sidekiq (tarefas assíncronas); React e Redux (interface); FFmpeg 5.1 ou superior (processamento de mídia) |
| **Interface e clientes** | interface web em React e Redux; demais clientes, como aplicativos móveis, acessam a API REST |
| **Banco(s) de dados** | PostgreSQL 14 ou superior |
| ***Cache*, filas e mensageria** | Redis 7.0 ou superior: *cache*, filas do Sidekiq, *timelines* pré-computadas e *pub/sub* para o *streaming* |
| **Armazenamento de arquivos/objetos** | disco local ou armazenamento de objetos compatível com S3, para mídia local e *cache* de mídia remota |
| **Protocolos e APIs expostas** | API REST com autenticação OAuth 2; API de *streaming* por WebSocket e *Server-Sent Events*; ActivityPub para federação (LEMMER-WEBBER *et al.*, 2018) |
| **Execução e implantação** | Docker Compose (`docker-compose.yml` com os serviços `db`, `redis`, `web`, `streaming` e `sidekiq`); instalação a partir do código com três serviços systemd; *charts* Helm mantidos no repositório `mastodon/chart` |
| **Licença (da versão adotada)** | GNU AGPL v3 |
| **Versão adotada no livro (*tag*)** | [`v4.7.3`](https://github.com/mastodon/mastodon/tree/v4.7.3), publicada em 1.º de outubro de 2026 |

Fonte: elaborado pelo autor (2026), com base em Mastodon (2026c; 2026e) e na Seção 4.2.

A versão é fixada porque nomes de pastas, classes e arquivos mudam entre lançamentos. Todas as
listagens de código real das Seções 5.2 e 6.2 foram conferidas na *tag* `v4.7.3` e apontam para
*permalinks* dela.

## 2.2.3 Como executar o Mastodon localmente

O repositório oferece três caminhos, e convém escolher conforme o objetivo.

- **Para estudar o código e executá-lo em modo de desenvolvimento**, a documentação descreve dois
  roteiros: uma máquina virtual pronta, definida num `Vagrantfile` incluído no repositório, e a
  instalação manual das dependências, seguida da preparação do banco e da execução de todos os
  processos com o Foreman (`bin/dev`), que inicia os processos definidos no *Procfile* (MASTODON, 2026d). O repositório
  traz também uma configuração de *Dev Container* (`.devcontainer/compose.yaml`), utilizável em
  editores compatíveis. Ao final, a aplicação fica disponível em `http://localhost:3000`.
- **Para observar a topologia de produção**, o arquivo
  [`docker-compose.yml`](https://github.com/mastodon/mastodon/blob/v4.7.3/docker-compose.yml) da
  *tag* estudada declara cinco serviços: `db` (PostgreSQL 14), `redis` (Redis 7), `web`
  (`bundle exec puma`), `streaming` (`node ./streaming/index.js`) e `sidekiq`
  (`bundle exec sidekiq`), com uma rede interna isolada para banco e Redis. O `README.md` informa
  que o repositório inclui configurações de implantação para Docker e Docker Compose, além de
  outras plataformas (MASTODON, 2026c).
- **Para instalar em produção**, a documentação oficial descreve a instalação a partir do código
  em Ubuntu 24.04 ou Debian 13, com nginx como proxy reverso, certificado Let's Encrypt e três
  serviços systemd: `mastodon-web`, `mastodon-sidekiq` e `mastodon-streaming` (MASTODON, 2026e).

O leitor que pretende acompanhar a Seção 6.2 com o código aberto no editor não precisa executar o
sistema: basta clonar o repositório e fazer `git checkout v4.7.3`. A execução é útil, contudo,
para observar as filas do Sidekiq em funcionamento e as chaves `feed:home:<id>` sendo criadas no
Redis quando uma conta publica, o que a Seção 5.2 explica.

## 2.2.4 Por que este par

O par Twitter/Mastodon foi escolhido como primeiro voo por três razões.

A primeira é que o **problema é universal e fácil de enunciar**. Qualquer estudante usa ou já usou
uma rede social com *timeline*, e a pergunta central, quando e onde montar a lista de publicações
que cada usuário vê, pode ser formulada sem jargão. Ao mesmo tempo, ela conduz diretamente aos
temas clássicos de sistemas em escala: cache, filas, consistência eventual e particionamento. Xu
(2020, cap. 11) dedica um capítulo inteiro ao projeto de um *feed* de notícias, mencionando
explicitamente a *timeline* do Twitter como variação do mesmo problema.

A segunda é que os dois sistemas **respondem de maneira diferente à mesma pressão de escala**. O
Twitter centralizou tudo e, sob crescimento explosivo, trocou o monólito Rails por serviços
distribuídos. O Mastodon permaneceu um monólito Rails em camadas, mas distribuiu a escala de outra
forma: em vez de um único sistema gigantesco, milhares de instâncias pequenas que interoperam.
O par ilustra, assim, o dilema entre **escalar um sistema** e **escalar uma rede de sistemas**, e
mostra que a escolha de estilo decorre das características priorizadas e do contexto
organizacional, como antecipa a primeira lei de Richards e Ford (ver Seção 1.1.2): tudo em
arquitetura é um compromisso.

A terceira é **didática**. O código do Mastodon é organizado segundo as convenções do Rails,
amplamente documentadas, e a decisão central do sistema, distribuir as publicações às *timelines*
no momento da escrita, de forma assíncrona, materializa-se em um punhado de classes com nomes
autoexplicativos, como `FanOutOnWriteService` e `FeedManager`. É um bom primeiro pouso no código.

## 2.2.5 Ficha *kata*

A ficha *kata*, formato de exercício de arquitetura criado por Ted Neward e adaptado por Richards
e Ford (2020, p. 68), descreve o problema em termos de negócio antes de qualquer decisão técnica.
Para este par, ela pode ser redigida como segue.

- **Descrição:** rede social de mensagens curtas. Usuários publicam, seguem outros usuários e leem
  uma *timeline* inicial (*home timeline*) com as publicações de quem seguem, além de *timelines*
  públicas, notificações, menções e mensagens diretas.
- **Usuários:** no Twitter, centenas de milhões de usuários ativos em uma única plataforma. No
  Mastodon, milhares de instâncias independentes, cada uma com algumas dezenas a algumas centenas
  de milhares de contas, que conversam entre si.
- **Requisitos:** publicar (texto, mídia, enquetes); seguir e deixar de seguir; ler a *timeline*
  inicial e as públicas; receber atualizações em tempo quase real; notificar menções e
  interações; buscar; moderar conteúdo, contas e servidores inteiros; migrar a conta para outra
  instância.
- **Contexto adicional:** as leituras superam as escritas em ordens de grandeza; a distribuição de
  seguidores é extremamente desigual (poucos perfis com milhões de seguidores, a maioria com
  poucos); há picos ligados a eventos ao vivo; no Mastodon, cada instância é administrada por
  pessoas ou organizações diferentes, muitas vezes voluntárias, com orçamento modesto, e nenhuma
  delas controla a rede.

O último item do contexto adicional é o que mais diferencia os dois sistemas e, como mostra a
[Seção 3.2](../capitulo-3-100-mil-pes/mastodon.md), altera o conjunto de características
prioritárias.

## Referências

FOX, A.; PATTERSON, D. **Engineering software as a service**: an agile approach using cloud
computing. 2. ed., versão 2.0b7. [*S. l.*]: Strawberry Canyon, 2021.

GOLLOM, M. Twitter was the Holy Grail of branding. Then Elon Musk ditched it. Experts question why.
**CBC News**, 26 jul. 2023. Disponível em:
<https://www.cbc.ca/news/business/twitter-elon-musk-branding-x-1.6917161>. Acesso em: 18 out. 2026.

HOFF, T. **The architecture Twitter uses to deal with 150M active users, 300K QPS, a 22 MB/s
firehose, and send tweets in under 5 seconds**. High Scalability, 8 jul. 2013. Disponível em:
<http://highscalability.com/blog/2013/7/8/the-architecture-twitter-uses-to-deal-with-150m-active-users.html>.
Acesso em: 18 out. 2026.

KRIKORIAN, R. **Timelines at scale**. Palestra apresentada na QCon San Francisco 2012. InfoQ,
2012. Disponível em: <https://www.infoq.com/presentations/Twitter-Timeline-Scalability/>. Acesso
em: 18 out. 2026.

KRIKORIAN, R. **New Tweets per second record, and how!** Twitter Engineering Blog, 16 ago. 2013.
Disponível em:
<https://blog.x.com/engineering/en_us/a/2013/new-tweets-per-second-record-and-how>. Acesso em:
18 out. 2026.

LEMMER-WEBBER, C. *et al.* **ActivityPub**. W3C Recommendation, 23 jan. 2018. Disponível em:
<https://www.w3.org/TR/activitypub/>. Acesso em: 18 out. 2026.

MASTODON. **About**. Mastodon, 2026a. Disponível em: <https://joinmastodon.org/about>. Acesso em:
18 out. 2026.

MASTODON. **What is Mastodon?** Mastodon documentation, 2026b. Disponível em:
<https://docs.joinmastodon.org/>. Acesso em: 18 out. 2026.

MASTODON. **mastodon/mastodon**: repositório de código-fonte, versão v4.7.3. GitHub, 2026c.
Disponível em: <https://github.com/mastodon/mastodon/tree/v4.7.3>. Acesso em: 18 out. 2026.

MASTODON. **Setting up a dev environment**. Mastodon documentation, 2026d. Disponível em:
<https://docs.joinmastodon.org/dev/setup/>. Acesso em: 18 out. 2026.

MASTODON. **Installing from source**. Mastodon documentation, 2026e. Disponível em:
<https://docs.joinmastodon.org/admin/install/>. Acesso em: 18 out. 2026.

RICHARDS, M.; FORD, N. **Fundamentals of software architecture**: an engineering approach.
Sebastopol: O'Reilly, 2020.

ROCHKO, E. **Twitter suspends Mastodon account, prevents sharing links**. Mastodon Blog, 20 dez.
2022. Disponível em:
<https://blog.joinmastodon.org/2022/12/twitter-suspends-mastodon-account-prevents-sharing-links/>.
Acesso em: 18 out. 2026.

TWITTER. **Twitter's recommendation algorithm**. Twitter Engineering Blog, 31 mar. 2023.
Disponível em:
<https://blog.x.com/engineering/en_us/topics/open-source/2023/twitter-recommendation-algorithm>.
Acesso em: 18 out. 2026.

XU, A. **System design interview**: an insider's guide. 2. ed. [*S. l.*]: Independently
published, 2020.
