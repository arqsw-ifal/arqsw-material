# 2.7 AWS IoT Core e ThingsBoard

## 2.7.1 Um sistema cujos usuários são máquinas

As aplicações apresentadas nas seções anteriores deste capítulo atendem, essencialmente, a
pessoas: quem publica uma mensagem, envia um arquivo ou conclui uma compra. O par estudado nesta
seção atende a máquinas. Sensores de temperatura em uma câmara fria, medidores de energia em uma
subestação, válvulas em uma rede de irrigação e rastreadores em caminhões enviam medições
periódicas, recebem comandos e, eventualmente, perdem a conexão e voltam todos ao mesmo tempo. A
plataforma que os atende conversa com uma frota de dispositivos com pouca memória, bateria
limitada e redes instáveis, cujo comportamento coletivo pode, ele próprio, derrubar a plataforma.

Esse tipo de plataforma é a parte de software de um **sistema ciberfísico**, isto é, de um
sistema em que computação, comunicação e processos físicos se influenciam mutuamente. A Seção 1.2
antecipou essa situação ao distinguir arquitetura de sistemas e arquitetura de software: aqui, o
software recebe da arquitetura de sistemas restrições como a bateria de um sensor, a largura de
banda de uma rede celular de banda estreita e o custo de enviar um técnico a campo para atualizar
um equipamento. O sistema real escolhido é o **AWS IoT Core**, serviço gerenciado da Amazon Web
Services. O gêmeo de código aberto é o **ThingsBoard**, plataforma de Internet das Coisas (IoT)
escrita em Java.

## 2.7.2 O sistema real: AWS IoT Core

O AWS IoT foi anunciado em versão *beta* por Jeff Barr no blog de notícias da AWS em 8 de outubro
de 2015, durante o evento re:Invent, e declarado disponível para uso geral em dezembro do mesmo ano
(BARR, 2015a; BARR, 2015b). O texto de lançamento já apresentava os elementos que permanecem no
serviço atual: um *message broker* acessível por MQTT ou REST, um registro que atribui identidade
única a cada dispositivo (*thing*), "sombras" que representam na nuvem o estado de cada
dispositivo mesmo quando ele perde a conexão por longos períodos, e um motor de regras que
transforma mensagens com base em expressões definidas pelo usuário e as encaminha a outros
serviços da AWS, como DynamoDB, S3, Lambda, SNS, SQS e Kinesis (BARR, 2015a).

A documentação atual descreve o AWS IoT Core como um serviço de nuvem gerenciado que permite que
dispositivos conectados interajam de forma segura com aplicações em nuvem e com outros
dispositivos, processando e roteando mensagens entre eles (AMAZON WEB SERVICES, 2026a). Suas
funções dividem-se em três grupos. As de **mensageria** incluem o *device gateway*, que protege a
comunicação com certificados X.509; o *message broker*, que oferece *publish/subscribe* por MQTT,
MQTT sobre WebSocket e uma interface HTTP REST; e o *rules engine*, que seleciona dados das
mensagens com uma linguagem semelhante a SQL. As de **controle** incluem o registro de
dispositivos, o provisionamento por modelos, os *jobs* para operações remotas, como atualização de
*firmware*, e a autenticação personalizada. As de **dados** incluem o *Device Shadow*, documento
JSON que guarda o estado de cada dispositivo para que aplicações possam interagir com ele mesmo
quando está desconectado (AMAZON WEB SERVICES, 2026a). Ao redor do núcleo, a AWS oferece serviços
complementares, como o AWS IoT Greengrass para processamento na borda e o AWS IoT Device Defender
para auditoria de segurança da frota.

Como o código do AWS IoT Core não é público, este livro o analisa exclusivamente pela
documentação oficial e por textos de engenharia publicados pela própria AWS. Essa limitação é
deliberada e didática: o serviço gerenciado mostra ao arquiteto apenas funções, cotas e preços, e
oculta filas, partições e bancos. O gêmeo de código aberto serve para abrir essa caixa.

## 2.7.3 O gêmeo open source: ThingsBoard

### Origem e evolução

O repositório `thingsboard/thingsboard` foi criado no GitHub em 1º de dezembro de 2016, e a
primeira versão, `v1.0`, foi publicada em 6 de dezembro do mesmo ano (THINGSBOARD, 2026a). Ao
longo de quase uma década, três marcos da história de versões interessam a este livro. A versão
2.2, de novembro de 2018, introduziu a arquitetura de microsserviços, que permite implantar
transportes, núcleo e motor de regras como serviços separados (THINGSBOARD, 2026b). A versão 2.5.2,
de junho de 2020, substituiu a biblioteca Akka por uma implementação própria, em Java puro, do
sistema de atores, conforme as notas de versão (THINGSBOARD, 2026a). A versão 3.0, do mesmo mês,
migrou a interface para Angular e tornou obrigatório o uso de banco SQL para entidades
(THINGSBOARD, 2026a). Em outubro de 2026, o repositório reunia mais de 22 mil estrelas e 6 mil
*forks* no GitHub, o que o coloca entre os projetos de IoT de código aberto mais conhecidos.

### Licença: um ponto de atenção

Até a série 4.3, o código do ThingsBoard é distribuído sob a licença Apache 2.0, e cada arquivo
traz o cabeçalho `SPDX-License-Identifier: Apache-2.0`. A *tag* `v4.4`, publicada em 29 de setembro
de 2026, adota a *Business Source License* 1.1, que permite uso gratuito em produção apenas sob
condições (por exemplo, limites de número de dispositivos e exigência de chave de licença) e prevê
a conversão de cada versão para Apache 2.0 quatro anos após sua publicação (THINGSBOARD, 2026c).
Por essa razão, **este livro adota a *tag* `v4.3.1.6`**, publicada no mesmo dia, que mantém a
licença Apache 2.0 e preserva o caráter de gêmeo de código aberto exigido pelo método. A mudança
de licença é, ela própria, uma lição de arquitetura a 100.000 pés: a sustentabilidade econômica de
um projeto é uma restrição que pode alterar a viabilidade de adotá-lo, e a escolha de uma
dependência envolve também a avaliação de quem a mantém e sob quais termos.

### Tecnologias

A Tabela 2.7.1 reúne a ficha técnica do gêmeo na versão adotada, conferida nos arquivos `pom.xml`
do repositório e na documentação de arquitetura. Ela antecipa as duas escolhas que a Seção 4.7
examina em detalhe: a aceitação das mensagens, feita por transportes específicos de cada
protocolo, é separada do processamento por uma fila, que pode ser em memória ou o Apache Kafka; e
as entidades e as séries temporais podem ficar em bancos distintos, porque crescem e são
consultadas de maneiras muito diferentes.

**Tabela 2.7.1** – Ficha técnica do ThingsBoard v4.3.1.6.

| Item | Descrição |
|---|---|
| **Linguagem(ns) do servidor/núcleo** | Java 17 (núcleo, transportes e motor de regras); JavaScript sobre Node.js no executor de *scripts* (`tb-js-executor`) da topologia de microsserviços |
| ***Frameworks* principais** | Spring Boot 3.5; Netty (MQTT); Eclipse Californium (CoAP); Eclipse Leshan (LwM2M, sobre Californium); sistema de atores próprio (módulo `common/actor`); JavaScript e TBEL para *scripts* de usuário |
| **Interface e clientes** | interface web em Angular (`tb-web-ui` na topologia de microsserviços), com painéis atualizados por WebSocket; dispositivos como clientes dos transportes |
| **Banco(s) de dados** | PostgreSQL para entidades (obrigatório); séries temporais em PostgreSQL, Cassandra ou TimescaleDB |
| ***Cache*, filas e mensageria** | fila em memória (desenvolvimento) ou Apache Kafka (produção); Valkey ou Redis como *cache* de credenciais, sessões e perfis; Zookeeper para coordenação do *cluster* |
| **Armazenamento de arquivos/objetos** | Não se aplica (ver nota) |
| **Protocolos e APIs expostas** | MQTT (portas 1883 e 8883 com TLS), CoAP e LwM2M (UDP 5683 a 5688), HTTP e SNMP para dispositivos; API REST e WebSocket (porta 8080) para usuários e aplicações |
| **Execução e implantação** | monólito em nó único (Docker Compose com a imagem `thingsboard/tb-node:4.3.1.6` e PostgreSQL, com variante que usa Kafka) ou microsserviços comunicados por Kafka |
| **Licença (da versão adotada)** | Apache 2.0 (a partir da *tag* `v4.4`, *Business Source License* 1.1) |
| **Versão adotada no livro (*tag*)** | [`v4.3.1.6`](https://github.com/thingsboard/thingsboard/tree/v4.3.1.6), publicada em 29 de setembro de 2026 |

Fonte: elaborado pelo autor (2026), com base em ThingsBoard (2026a; 2026b; 2026d; 2026e).

Nota: a documentação de arquitetura não inclui um serviço de armazenamento de arquivos ou de
objetos entre os *containers* de infraestrutura; os dados da plataforma ficam nos bancos e no
*cache* indicados nas linhas anteriores (Seção 4.7).

### Como executar localmente

A documentação oficial de instalação com Docker oferece um arquivo `docker-compose.yml` com dois
serviços: um PostgreSQL e o contêiner `thingsboard-ce`, baseado na imagem
`thingsboard/tb-node:4.3.1.6`, que publica, entre outras, as portas 8080 (interface web e API
REST), 1883 e 8883 (MQTT e MQTT sobre TLS) e o intervalo 5683 a 5688 em UDP (CoAP e LwM2M)
(THINGSBOARD, 2026e). O procedimento documentado tem três passos: criar o diretório e o arquivo
de composição; inicializar o banco com
`docker compose run --rm -e INSTALL_TB=true -e LOAD_DEMO=true thingsboard-ce`, que também carrega
dados de demonstração; e iniciar a plataforma com `docker compose up -d`. A mesma página oferece
uma variante com Kafka no lugar da fila em memória. Essa instalação corresponde à topologia
**monolítica em nó único** discutida na Seção 4.7, e é suficiente para todos os experimentos
deste livro. A coincidência entre a versão da imagem documentada e a *tag* adotada permite que o
leitor execute exatamente o código que será lido na Seção 6.7.

## 2.7.4 Por que este par

O par AWS IoT Core e ThingsBoard foi escolhido por três razões. A primeira é a **mudança de
natureza dos usuários**. Em uma plataforma de IoT, a escalabilidade é medida em mensagens por
segundo e em conexões persistentes, a disponibilidade precisa considerar que a entrada de dados
continua mesmo quando o processamento atrasa, e a segurança envolve milhões de pontos de entrada
físicos. O incidente da botnet Mirai, que em 2016 converteu centenas de milhares de câmeras e
roteadores com senhas de fábrica em armas contra a infraestrutura da internet, mostrou que essa
última característica não é secundária (ANTONAKAKIS *et al.*, 2017).

A segunda razão é que o ThingsBoard oferece **duas topologias de implantação a partir da mesma
base de código**: um monólito, em que transportes, núcleo e motor de regras executam no mesmo
processo Java, e um conjunto de microsserviços comunicados por Kafka (THINGSBOARD, 2026d).
Poucos sistemas permitem discutir, sobre código idêntico, a diferença entre os dois estilos e
contar os quanta arquiteturais de cada um, no sentido de Richards e Ford (2020, cap. 7). Essa
discussão é o núcleo da Seção 4.7.

A terceira é a riqueza de **padrões no código**. O motor de regras organiza o processamento como
um grafo de nós que implementam a mesma interface, o que permite reconhecer *Strategy* e *Chain
of Responsibility*; os transportes convertem protocolos diferentes em uma representação interna
comum, o que ilustra o *Adapter*; e o sistema de atores torna concreto o modelo de concorrência
de Hewitt, Bishop e Steiger (1973), formalizado por Agha (1986).

O dilema arquitetural que o par ilustra pode ser formulado assim: **como aceitar, com segurança,
um fluxo contínuo e imprevisível de mensagens de milhões de dispositivos, processá-lo com regras
configuráveis por cliente e armazená-lo indefinidamente, sem que um cliente, um dispositivo ou um
pico de reconexões comprometa os demais?** As respostas envolvem separar a aceitação do
processamento por meio de filas, escolher garantias de entrega mais fracas e compensá-las com
idempotência, separar entidades de séries temporais em bancos distintos e escalonar mecanismos
de isolamento entre inquilinos conforme o custo. O AWS IoT Core e o ThingsBoard dão respostas
diferentes a algumas dessas questões, e a comparação é instrutiva: o serviço gerenciado elimina a
operação de filas e bancos, mas impõe cotas e cobrança por mensagem; a solução autogerida oferece
controle e custo marginal menor, ao preço de exigir uma equipe capaz de operar sistemas
distribuídos.

## 2.7.5 Ficha *kata*

A Tabela 2.7.2 resume o problema no formato de *kata* arquitetural adotado no livro. Os números
de escala são hipotéticos e servem às estimativas da Seção 3.7.

**Tabela 2.7.2** – Ficha *kata* da plataforma de IoT.

| Item | Conteúdo |
|---|---|
| **Sistema real** | AWS IoT Core (serviço gerenciado; analisado pela documentação oficial) |
| **Gêmeo open source** | [ThingsBoard](https://github.com/thingsboard/thingsboard), *tag* `v4.3.1.6` (Apache 2.0) · [documentação](https://thingsboard.io/docs) |
| **Estilo dominante** | Orientado a eventos com topologia de *broker*; implantável como monólito ou como microsserviços |
| **Descrição** | Plataforma que conecta dispositivos (sensores, medidores, máquinas), coleta telemetria, aplica regras, gera alarmes e alimenta painéis |
| **Usuários** | Indústrias, cidades inteligentes, agronegócio e energia; operadores de planta, integradores, fabricantes de dispositivos; de milhares a milhões de dispositivos por cliente |
| **Requisitos** | Provisionar dispositivos; receber telemetria e atributos; enviar comandos (RPC) e atualizações de *firmware*; executar regras e alarmes; exibir painéis; atender vários clientes isolados (*multi-tenant*) |
| **Contexto adicional** | Dispositivos com pouca bateria e rede instável; protocolos heterogêneos; picos quando muitos dispositivos reconectam ao mesmo tempo; volume de dados que só cresce |

Fonte: elaborado pelo autor (2026).

As próximas seções percorrem o par em altitudes decrescentes. A
[Seção 3.7](../capitulo-3-100-mil-pes/thingsboard.md) reconstrói os requisitos em uma conversa com
o cliente; a [Seção 4.7](../capitulo-4-50-mil-pes/thingsboard.md) examina *containers*, filas,
bancos e topologias; a [Seção 5.7](../capitulo-5-10-mil-pes/thingsboard.md) desce aos componentes
de ingestão, atores e motor de regras; e a [Seção 6.7](../capitulo-6-solo/thingsboard.md) pousa
no código.

## Referências

AGHA, G. **Actors**: a model of concurrent computation in distributed systems. Cambridge: MIT
Press, 1986.

AMAZON WEB SERVICES. **How AWS IoT works**. AWS IoT Core Developer Guide. 2026a. Disponível em:
<https://docs.aws.amazon.com/iot/latest/developerguide/aws-iot-how-it-works.html>. Acesso em:
18 out. 2026.

ANTONAKAKIS, M. *et al.* Understanding the Mirai botnet. *In*: USENIX SECURITY SYMPOSIUM, 26.,
2017, Vancouver. **Proceedings** [...]. Berkeley: USENIX Association, 2017. p. 1093-1110.

BARR, J. **AWS IoT**: cloud services for connected devices. AWS News Blog, 8 out. 2015a.
Disponível em: <https://aws.amazon.com/blogs/aws/aws-iot-cloud-services-for-connected-devices/>.
Acesso em: 18 out. 2026.

BARR, J. **AWS IoT**: now generally available. AWS News Blog, 18 dez. 2015b. Disponível em:
<https://aws.amazon.com/blogs/aws/aws-iot-now-generally-available/>. Acesso em: 18 out. 2026.

HEWITT, C.; BISHOP, P.; STEIGER, R. A universal modular ACTOR formalism for artificial
intelligence. *In*: INTERNATIONAL JOINT CONFERENCE ON ARTIFICIAL INTELLIGENCE (IJCAI), 3., 1973,
Stanford. **Proceedings** [...]. [S. l.: s. n.], 1973. p. 235-245. Disponível em:
<https://www.ijcai.org/Proceedings/73/Papers/027B.pdf>. Acesso em: 18 out. 2026.

RICHARDS, M.; FORD, N. **Fundamentals of software architecture**: an engineering approach.
Sebastopol: O'Reilly, 2020.

THINGSBOARD. **thingsboard/thingsboard**: repositório de código-fonte e notas de versão. GitHub,
2026a. Disponível em: <https://github.com/thingsboard/thingsboard>. Acesso em: 18 out. 2026.

THINGSBOARD. **Microservices architecture**. ThingsBoard documentation. 2026b. Disponível em:
<https://thingsboard.io/docs/reference/architecture/microservices/>. Acesso em: 18 out. 2026.

THINGSBOARD. **LICENSE**: Business Source License 1.1, versão v4.4. GitHub, 2026c. Disponível
em: <https://github.com/thingsboard/thingsboard/blob/v4.4/LICENSE>. Acesso em: 18 out. 2026.

THINGSBOARD. **Architecture overview**. ThingsBoard documentation. 2026d. Disponível em:
<https://thingsboard.io/docs/reference/architecture/>. Acesso em: 18 out. 2026.

THINGSBOARD. **Installing ThingsBoard CE using Docker (Linux, macOS)**. ThingsBoard documentation. 2026e.
Disponível em: <https://thingsboard.io/docs/installation/docker/>. Acesso em: 18 out. 2026.
