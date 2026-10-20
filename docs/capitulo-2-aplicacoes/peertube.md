# 2.4 YouTube e PeerTube

Esta seção apresenta o terceiro par de sistemas estudados no livro: o YouTube, complementado
pela Netflix, como sistema real, e o PeerTube como gêmeo de código aberto. Ao contrário dos
pares anteriores, em que a dificuldade central é distribuir grandes quantidades de mensagens
pequenas (Seções 2.2 e 2.3), aqui o objeto é o vídeo: um arquivo escrito uma vez e lido muitas
vezes, mas cuja escrita dispara minutos ou horas de processamento e cuja leitura transfere
centenas de megabytes. Essa assimetria faz do **custo** uma força que molda a arquitetura, ao
lado do desempenho e da escalabilidade, e é o fio condutor da análise deste par nos Capítulos 3
a 6.

## 2.4.1 O sistema real: YouTube

O YouTube é a plataforma de vídeo do Google e, em termos de escala, a referência inevitável do
domínio. Xu (2020, cap. 14), que usa o YouTube como estudo de caso de projeto de sistemas,
reúne estatísticas de 2020 segundo as quais a plataforma tinha cerca de 2 bilhões de usuários
ativos por mês, 5 bilhões de vídeos assistidos por dia e 50 milhões de criadores, e respondia
por 37% de todo o tráfego móvel da internet. O autor ressalva que a aparente simplicidade do
produto (os criadores enviam vídeos e os espectadores pressionam o botão de reprodução) encobre
um conjunto considerável de tecnologias, e divide o problema em dois fluxos de natureza muito
diferente.

O primeiro é o fluxo de **publicação**: receber um arquivo grande por uma rede pouco confiável,
convertê-lo (transcodificá-lo) para vários formatos e resoluções, gerar miniaturas e torná-lo
disponível. O segundo é o fluxo de **reprodução**: entregar esses arquivos, em pequenos
pedaços, a bilhões de dispositivos heterogêneos, adaptando a qualidade à banda disponível a
cada instante (XU, 2020, cap. 14). O primeiro fluxo é dominado por processamento; o segundo,
por transferência de dados. Ambos são caros, e a estimativa de custo de Xu, discutida na Seção
3.4, mostra que a entrega por CDN é de longe o item mais caro de um serviço desse tipo.

O YouTube também ilustra que os problemas de escala de um serviço de vídeo não se limitam aos
arquivos. Os metadados (títulos, canais, contadores, comentários) crescem na mesma proporção, e
o Vitess, hoje um projeto de código aberto de *sharding* de MySQL, foi criado no YouTube em 2010
justamente porque o tráfego de pico estava prestes a exceder a capacidade do banco de dados
(VITESS, 2026). No outro extremo da pilha, o custo de transcodificação levou o Google a projetar
um acelerador de *hardware* dedicado, a *video coding unit* (VCU), descrito por Ranganathan *et
al.* (2021) e discutido na Seção 4.4. O YouTube é, portanto, um sistema em que decisões de
arquitetura de software, de dados e de *hardware* são tomadas sob a mesma pressão econômica.

Como o código do YouTube é fechado, o que se sabe sobre a sua arquitetura vem de publicações
técnicas do próprio Google e de reconstruções didáticas como a de Xu (2020). Este livro usa
essas fontes para formular as perguntas, e o gêmeo de código aberto para observar respostas
concretas.

## 2.4.2 Um caso complementar: Netflix

A Netflix não é uma plataforma de vídeo gerado por usuários, mas é o serviço de vídeo cuja
engenharia é mais bem documentada publicamente, e três episódios de sua história são usados ao
longo do livro. O primeiro é a migração para a nuvem: em agosto de 2008, uma corrupção grave de
banco de dados impediu a empresa de enviar DVDs aos assinantes durante três dias, e a conclusão
foi abandonar pontos únicos de falha escalados verticalmente em favor de sistemas distribuídos
na nuvem; a migração durou sete anos e terminou em janeiro de 2016 (IZRAILEVSKY; VLAOVIC;
MESHENBERG, 2016). O segundo é a criação do *Chaos Monkey* e da engenharia do caos, como resposta
à perda de controle sobre a operação na nuvem (RICHARDS; FORD, 2020, p. 88). O terceiro é o
*Open Connect*, a CDN própria da Netflix, iniciada em 2011, cujos servidores são instalados em
pontos de troca de tráfego e dentro das redes dos provedores de acesso (NETFLIX, 2026).

Os três episódios são examinados na Seção 4.4. Eles foram escolhidos porque ilustram, com fontes
de primeira mão, temas que o YouTube não documenta com o mesmo detalhe: a confiabilidade como
motivação de uma mudança de arquitetura, a governança contínua por funções de aptidão em
produção e a separação entre plano de controle e plano de dados na entrega de vídeo.

## 2.4.3 O gêmeo de código aberto: PeerTube

O PeerTube é uma plataforma de vídeo federada desenvolvida pela associação francesa Framasoft,
publicada sob a licença AGPL-3.0, cuja primeira versão estável, a 1.0, foi lançada em outubro de
2018 (PEERTUBE, 2026). Seu princípio de funcionamento é diferente do YouTube em dois aspectos.
Primeiro, não há uma plataforma única, mas uma rede de **instâncias** independentes, cada uma
mantida por uma organização ou pessoa, que se seguem mutuamente pelo protocolo ActivityPub, o
mesmo usado pelo Mastodon (Seção 2.2). Segundo, os espectadores podem trocar entre si partes do
vídeo que estão assistindo, por conexões par a par (P2P) estabelecidas pelo navegador via
WebRTC, o que reduz a banda consumida do servidor (PEERTUBE, 2026).

A documentação de arquitetura do projeto descreve um servidor que concentra, num único processo,
a API REST, o ActivityPub, um rastreador P2P, as notificações e as tarefas agendadas, entre elas
a transcodificação, protegido por um proxy reverso que termina as conexões TLS e serve arquivos
estáticos (PEERTUBE, 2026). A Tabela 2.4.1 reúne a ficha técnica da versão adotada. Observa-se
nela que uma única linguagem, o TypeScript, cobre servidor e cliente, e que a lista de protocolos
é mais longa do que a dos pares anteriores, porque o PeerTube precisa ao mesmo tempo federar
(ActivityPub), entregar vídeo (HLS), repartir a entrega entre espectadores (WebRTC) e receber
transmissões ao vivo (RTMP). O repositório é um monorrepositório que reúne o servidor
(`server/core/`), o cliente (`client/`), pacotes compartilhados (`packages/`) e aplicações
auxiliares (`apps/`), entre elas o *PeerTube runner*, programa que executa transcodificações em
máquinas remotas.

**Tabela 2.4.1** – Ficha técnica do PeerTube v8.3.1.

| Item | Descrição |
|---|---|
| **Linguagem(ns) do servidor/núcleo** | TypeScript sobre Node.js (servidor, cliente e *runner*) |
| ***Frameworks* principais** | Express (servidor); Angular (cliente); BullMQ (fila de *jobs*); ffmpeg (transcodificação); hls.js e `p2p-media-loader` (reprodutor) |
| **Interface e clientes** | cliente web Angular de página única, com reprodutor HLS e P2P; *PeerTube runner* como cliente remoto de transcodificação |
| **Banco(s) de dados** | PostgreSQL, para metadados (imagem `postgres:17-alpine` no arquivo de composição de referência) |
| ***Cache*, filas e mensageria** | Redis como *cache* e como suporte à fila BullMQ (imagem `redis:8-alpine`); Socket.IO para notificações aos navegadores e aos *runners* |
| **Armazenamento de arquivos/objetos** | disco local ou, opcionalmente, armazenamento de objetos compatível com S3 |
| **Protocolos e APIs expostas** | API REST descrita em OpenAPI; ActivityPub; HLS sobre HTTP; P2P por WebRTC, com rastreador via WebSocket; RTMP para transmissões ao vivo |
| **Execução e implantação** | Docker Compose V2 (`support/docker/production/docker-compose.yml`, sete serviços num único *host*, com NGINX como proxy reverso); instalação de produção segundo o guia oficial; *runners* em máquinas remotas |
| **Licença (da versão adotada)** | AGPL-3.0 |
| **Versão adotada no livro (*tag*)** | [`v8.3.1`](https://github.com/Chocobozzz/PeerTube/tree/v8.3.1), publicada em setembro de 2026 |

Fonte: elaborado pelo autor (2026), com base em PeerTube (2026) e nas Seções 4.4 e 5.4.

O registro de mudanças (*changelog*) do projeto permite reconstruir a ordem em que os problemas
arquiteturais foram enfrentados (PEERTUBE, 2026, `CHANGELOG.md`). O *upload* retomável foi
introduzido na versão 3.2.0 (maio de 2021), como resposta à fragilidade dos envios de arquivos
grandes. O suporte a armazenamento de objetos compatível com S3 surgiu, em caráter experimental,
na versão 3.4.0 (setembro de 2021). A transcodificação remota por *runners* chegou na versão
5.2.0 (junho de 2023). E a versão 6.0 removeu do reprodutor o suporte ao WebTorrent, mantendo o
P2P apenas para vídeos em HLS. Cada um desses marcos corresponde a uma decisão arquitetural
reconstruída nas Seções 4.4 e 5.4.

### 2.4.3.1 Comunidade e governança

O desenvolvimento é conduzido pela Framasoft, que o declara no próprio README do repositório,
com contribuições externas por *pull requests*, tradução comunitária pela plataforma Weblate da
associação e um ecossistema de *plugins* e temas (PEERTUBE, 2026). Argumenta-se aqui que essa
configuração tem uma consequência relevante para a arquitetura: muitas decisões são tomadas
pensando em administradores de instâncias com poucos recursos, e não em uma equipe de operações
dedicada. O arquivo oficial de composição, por exemplo, coloca todos os serviços em um único
servidor, e as
funcionalidades que exigem mais infraestrutura (armazenamento de objetos, *runners* remotos,
redundância) são opcionais e desabilitadas por padrão.

### 2.4.3.2 Como executar localmente

A forma oficial de instalação com contêineres usa o Docker Compose V2. A documentação orienta a
baixar dois arquivos do repositório, `support/docker/production/docker-compose.yml` e
`support/docker/production/.env`; a editar o `.env`, substituindo os marcadores de usuário e
senha do PostgreSQL, domínio, endereço de e-mail e segredo da instância; a configurar o servidor
*web* e gerar o primeiro certificado; e, por fim, a executar `docker compose up` (PEERTUBE,
2026). A senha do usuário `root` é exibida nos *logs* na primeira execução e pode ser redefinida
com `docker compose exec -u peertube peertube npm run reset-password -- -u root`.

Dois cuidados são necessários para uso didático. O primeiro é que a documentação baixa os
arquivos do ramo `master`; para reproduzir exatamente o que é analisado neste livro, convém
baixá-los da *tag* adotada, substituindo `master` por `v8.3.1` nas URLs. O segundo é que a
documentação declara não haver imagem Docker para desenvolvimento. Para um teste local, o
próprio arquivo de composição indica, em comentários, que as seções `webserver`, `certbot` e
`webserver-reloader` podem ser comentadas e que a porta 9000 do contêiner `peertube` pode ser
exposta diretamente, ressalvando que essa configuração não é adequada para produção (PEERTUBE,
2026, `support/docker/production/docker-compose.yml`). A Seção 4.4 analisa esse arquivo como
diagrama de implantação.

### 2.4.3.3 Versão adotada

!!! warning "Versão estudada: *tag* `v8.3.1`"
    Todas as análises de código deste livro referem-se à *tag* `v8.3.1` do repositório
    [Chocobozzz/PeerTube](https://github.com/Chocobozzz/PeerTube/tree/v8.3.1), publicada em
    setembro de 2026 e marcada como versão estável. Nomes de pastas, classes e arquivos mudaram
    significativamente entre versões: o código do servidor, por exemplo, migrou de `server/`
    para `server/core/`. Quem consultar o ramo principal deve esperar divergências.

## 2.4.4 Por que este par

O par YouTube–PeerTube foi escolhido por três razões. A primeira é que ele traz para o livro um
estilo arquitetural ainda não estudado nos pares anteriores: o *pipes and filters* (canos e
filtros), que organiza o processamento de cada vídeo como uma sequência, e depois como um grafo,
de transformações independentes (RICHARDS; FORD, 2020, cap. 11). A segunda é que ele permite
estudar a **entrega** de conteúdo como problema arquitetural próprio, com *streaming* adaptativo
por HTTP, CDN e P2P. A terceira é que o código do PeerTube contém exemplos limpos e verificáveis
de padrões clássicos: *Chain of Responsibility* nas cadeias de *middlewares* do Express,
*Template Method* nos construtores de tarefas de transcodificação e *Strategy* nos perfis de
codificação extensíveis por *plugins* (GAMMA *et al.*, 1994).

O dilema arquitetural que o par ilustra pode ser formulado assim: **quem paga pelo vídeo, e como
a arquitetura distribui esse custo?** O YouTube concentra o custo em uma única empresa, que o
reduz com escala, CDN e *hardware* dedicado. O PeerTube não tem essa opção: o custo de cada
instância é pago por uma instituição, muitas vezes sem fins lucrativos. As soluções que o
PeerTube adota (P2P entre espectadores, redundância entre instâncias federadas, descarte do
arquivo original após a transcodificação e *runners* remotos de transcodificação) são, em
grande parte, mecanismos para **transferir** ou **repartir** custo, e não apenas para reduzi-lo.
O SWEBOK v4 lembra que a economia é a ciência da escolha, e não do dinheiro, e que em
organizações sem fins lucrativos o critério de decisão é obter o máximo benefício pelo menor
custo (WASHIZAKI, 2024, cap. 15). O par permite discutir esse critério com código real.

## 2.4.5 Ficha *kata*

A ficha *kata*, formato de exercício proposto por Ted Neward e adaptado por Richards e Ford
(2020, cap. 5), descreve o problema em termos de negócio, sem antecipar soluções técnicas.

!!! abstract "Ficha *kata*: plataforma de vídeo"
    **Descrição.** Plataforma em que usuários enviam vídeos, que são convertidos para várias
    resoluções e assistidos em qualquer dispositivo, sob demanda ou ao vivo.

    **Usuários.** No YouTube, bilhões de espectadores e dezenas de milhões de criadores. No
    PeerTube, instâncias federadas mantidas por instituições, coletivos e canais independentes,
    cada uma com centenas a milhares de usuários, que alcançam espectadores de toda a rede.

    **Requisitos.** *Upload* de arquivos grandes, inclusive por conexões instáveis;
    transcodificação para múltiplas resoluções; reprodução adaptativa em navegadores, celulares e
    televisores; transmissão ao vivo; comentários; moderação; federação com outras instâncias.

    **Contexto adicional.** Armazenamento, banda e CPU de transcodificação são os custos
    dominantes. Poucos vídeos concentram a maior parte das visualizações (cauda longa). No
    PeerTube, quem paga a infraestrutura é o administrador da instância.

A Tabela 2.4.2 resume o plano de voo do par.

**Tabela 2.4.2** – Plano de voo do par YouTube–PeerTube.

| Item | Descrição |
|---|---|
| **Sistema real** | YouTube; Netflix como caso complementar |
| **Gêmeo open source** | [PeerTube](https://github.com/Chocobozzz/PeerTube), *tag* `v8.3.1` · [documentação](https://docs.joinpeertube.org); ficha técnica na Tabela 2.4.1 |
| **Estilo dominante** | *pipes and filters* com fila de *jobs* no processamento; cliente-servidor com CDN e P2P na entrega; federação entre instâncias |
| **Padrões no código** | *Chain of Responsibility*, *Template Method*, *Strategy*; *Active Record* |
| **Exercício automático** | *Pipeline* de transcodificação em Java com ArchUnit (Seção 6.4) |

Fonte: elaborado pelo autor (2026).

## Referências

GAMMA, E. *et al.* **Design patterns**: elements of reusable object-oriented software. Reading:
Addison-Wesley, 1994.

IZRAILEVSKY, Y.; VLAOVIC, S.; MESHENBERG, R. **Completing the Netflix cloud migration**. Netflix,
12 fev. 2016. Disponível em:
<https://about.netflix.com/en/news/completing-the-netflix-cloud-migration>. Acesso em: 18 out.
2026.

NETFLIX. **Open Connect overview**. Los Gatos: Netflix, 2026. Disponível em:
<https://openconnect.netflix.com/Open-Connect-Overview.pdf>. Acesso em: 18 out. 2026.

PEERTUBE. **Chocobozzz/PeerTube**: repositório de código-fonte, versão v8.3.1. GitHub, 2026.
Disponível em: <https://github.com/Chocobozzz/PeerTube/tree/v8.3.1>. Acesso em: 18 out. 2026.
Documentação disponível em: <https://docs.joinpeertube.org>.

RANGANATHAN, P. *et al.* Warehouse-scale video acceleration: co-design and deployment in the
wild. *In*: INTERNATIONAL CONFERENCE ON ARCHITECTURAL SUPPORT FOR PROGRAMMING LANGUAGES AND
OPERATING SYSTEMS, 26., 2021, [*s. l.*]. **Proceedings** [...]. New York: ACM, 2021. p. 600-615.

RICHARDS, M.; FORD, N. **Fundamentals of software architecture**: an engineering approach.
Sebastopol: O'Reilly, 2020.

VITESS. **History**. 2026. Disponível em: <https://vitess.io/docs/overview/history/>. Acesso em:
18 out. 2026.

WASHIZAKI, H. (ed.). **Guide to the software engineering body of knowledge (SWEBOK Guide)**:
version 4.0. Los Alamitos: IEEE Computer Society, 2024.

XU, A. **System design interview**: an insider's guide. 2. ed. [S. l.]: Byte Code LLC, 2020.
