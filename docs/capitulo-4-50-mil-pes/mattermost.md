# 4.3 Mattermost: *containers* e implantação

<span class="altitude a50">50.000 pés</span>

A conversa com o cliente na [Seção 3.3](../capitulo-3-100-mil-pes/mattermost.md) terminou com quatro conclusões: a entrega em tempo real
exige conexões abertas e, portanto, estado no servidor; a relação entre escrita e entrega é
desequilibrada; a ordem e a unicidade podem ser garantidas por canal; e a soberania de dados impõe
uma restrição de implantação que favorece a simplicidade operacional. Esta seção desce a 50.000
pés para examinar como essas conclusões se materializam nas unidades de execução e de dados do
Mattermost, onde elas são implantadas e por que cada decisão foi tomada. As decisões são
contrastadas, sempre que as fontes públicas o permitem, com as do Slack (THANGUDU, 2023).

## 4.3.1 O problema da comunicação iniciada pelo servidor

O protocolo HTTP foi concebido para um diálogo em que o cliente pergunta e o servidor responde.
Em um sistema de mensagens, o lado do **remetente** se ajusta bem a esse modelo: enviar uma
mensagem é uma requisição comum. O lado do **destinatário**, porém, inverte a iniciativa: é o
servidor que sabe que há uma mensagem nova e precisa avisar o cliente. Xu (2020, cap. 12) descreve
três técnicas para simular uma comunicação iniciada pelo servidor: *polling*, *long polling* e
WebSocket.

No ***polling***, o cliente pergunta periodicamente se há novidades: é trivial, mas desperdiça
recursos com respostas vazias e impõe uma latência média de metade do intervalo entre consultas.
No ***long polling***, o servidor mantém a requisição aberta até haver mensagem ou expirar um tempo
limite. Xu (2020, cap. 12) aponta três problemas: remetente e destinatário podem estar ligados a
servidores diferentes, e um balanceador em rodízio não garante que a mensagem chegue ao servidor
que mantém a requisição pendente; o servidor não sabe bem se o cliente se desconectou; e usuários
pouco ativos continuam a abrir requisições a cada tempo limite. A IETF documentou esses problemas
no RFC 6202, incluindo a sobrecarga de cabeçalhos e a interferência de *proxies* intermediários
(LORETO *et al.*, 2011).

O **WebSocket**, padronizado pelo RFC 6455, resolve o problema na camada de protocolo (FETTE;
MELNIKOV, 2011). A conexão começa como uma requisição HTTP comum, com o cabeçalho
`Upgrade: websocket`; o servidor aceita respondendo com o código `101 Switching Protocols`, e a
partir daí a mesma conexão TCP passa a transportar **quadros** (*frames*) em ambos os sentidos, sem
o custo de um cabeçalho HTTP por mensagem. O protocolo define quadros de dados (texto e binário) e
quadros de controle, entre os quais *ping*, *pong* e fechamento. Os quadros de *ping* e *pong*
servem precisamente para verificar se o outro lado continua acessível, o que será o fundamento da
presença por *heartbeat* ([Seção 5.3](../capitulo-5-10-mil-pes/mattermost.md)). Como o WebSocket usa as portas 80 e 443, ele atravessa a
maioria dos *firewalls* que já admitem HTTP e HTTPS (XU, 2020, cap. 12).

**Tabela 4.3.1** – Comparação entre as técnicas de comunicação iniciada pelo servidor.

| Critério | *Polling* | *Long polling* | WebSocket |
|---|---|---|---|
| Latência de entrega | Até um intervalo inteiro de consulta | Baixa, mais o custo de reabrir a requisição | Mínima: o servidor escreve no *socket* aberto |
| Custo no servidor sem tráfego | Alto: requisições vazias periódicas | Médio: requisições pendentes e reabertas a cada tempo limite | Baixo por mensagem, mas há memória por conexão e *pings* periódicos |
| Detecção de desconexão | Implícita, pela ausência de consultas | Difícil (XU, 2020, cap. 12) | Explícita, por quadros de controle *ping* e *pong* |
| Estado no servidor | Nenhum entre requisições | Requisição pendente | Conexão persistente: o servidor torna-se *stateful* |
| Compatibilidade com infraestrutura HTTP | Total | Alta, com cuidado com tempos limite de *proxies* | Exige *proxies* que suportem `Upgrade` |
| Complexidade | Baixa | Média | Média a alta: reconexão, retomada e balanceamento |

Fonte: elaborado pelo autor (2026), com base em Xu (2020, cap. 12), Loreto *et al.* (2011) e
Fette e Melnikov (2011).

Xu (2020, cap. 12) conclui que, sendo o WebSocket bidirecional, não há razão técnica forte para não
usá-lo também no envio, mas observa que as demais funcionalidades, como cadastro, autenticação e
perfil, podem continuar sobre HTTP convencional. O Mattermost segue exatamente essa divisão: a
criação de uma mensagem é uma chamada à API REST (`POST /api/v4/posts`), registrada no
arquivo `server/channels/api4/post.go` (MATTERMOST, 2026a), e o WebSocket
(`/api/v4/websocket`) é usado sobretudo para que o servidor empurre eventos aos clientes. A
configuração de *proxy* recomendada pela documentação trata a rota do WebSocket separadamente,
repassando os cabeçalhos `Upgrade` e `Connection` (MATTERMOST, 2026b). O Slack adota a mesma
separação: o cliente envia a mensagem por uma API da aplicação web, e a recebe por uma conexão
WebSocket persistente com servidores de *gateway* (THANGUDU, 2023).

## 4.3.2 Serviços *stateful* e *stateless*

A adoção do WebSocket divide os serviços em duas classes. Os serviços **sem estado**
(*stateless*) atendem a requisições independentes e escalam pela simples adição de instâncias
atrás de um balanceador, guardando o estado em bancos ou *caches* compartilhados, o que Xu (2020,
cap. 1) chama de camada web sem estado. Os serviços **com estado** (*stateful*) mantêm informação
em memória entre interações; em um sistema de *chat*, o serviço de conexões é *stateful* porque
cada cliente mantém uma conexão persistente com um servidor específico (XU, 2020, cap. 12). As
consequências são profundas: encerrar uma instância provoca uma onda de reconexões; o balanceador
não pode distribuir mensagens em rodízio; e a escala horizontal exige descobrir **onde** cada
usuário está conectado ou difundir os eventos a todos os servidores, que filtram o que lhes
interessa. Este último ponto é a principal diferença entre o Slack e o Mattermost.

O Slack separa as responsabilidades em vários tipos de servidor escritos em Java: servidores de
canal, *stateful*, que guardam em memória o histórico recente de um subconjunto de canais
atribuídos por *hashing* consistente; servidores de *gateway*, também *stateful*, que mantêm as
conexões WebSocket e suas assinaturas, distribuídos por regiões geográficas; servidores de
administração, *stateless*; e servidores de presença. Uma mensagem segue da aplicação web ao
servidor de administração, deste ao servidor de canal responsável e daí a todos os *gateways* que
assinam o canal, que a entregam aos clientes (THANGUDU, 2023).

O Mattermost, ao contrário, é entregue como um **binário único** que acumula os dois papéis: cada
servidor atende à API REST sem estado e, ao mesmo tempo, mantém as conexões WebSocket dos clientes
que nele se conectaram (MATTERMOST, 2026c). Essa escolha é coerente com a restrição de
*self-hosting* discutida na [Seção 3.3.4](../capitulo-3-100-mil-pes/mattermost.md): um único executável é muito mais fácil de instalar e
operar do que uma constelação de serviços especializados. O preço é que o mesmo processo que
responde a uma consulta de histórico pesada também precisa escrever eventos em milhares de
*sockets*, o que impede escalar separadamente as duas cargas.

## 4.3.3 Descoberta de serviços

Quando há vários servidores com estado, é preciso decidir a qual deles cada cliente se conecta.
Xu (2020, cap. 12) atribui essa tarefa a um serviço de **descoberta de serviços** (*service
discovery*), exemplificado pelo Apache ZooKeeper, que registra os servidores de *chat* disponíveis
e indica a cada cliente, após a autenticação, o mais adequado segundo localização e capacidade;
se um servidor cai, o serviço indica outro aos clientes que precisam reconectar-se.

No Slack, os servidores de canal registram-se no Consul, a ferramenta de descoberta de serviços da
empresa, e os anéis de *hashing* consistente são administrados por um componente próprio, o
*Consul Hash Ring Manager* (CHARM); servidores de canal com problemas são substituídos rapidamente,
e um novo servidor fica pronto para receber tráfego em menos de 20 segundos (THANGUDU, 2023). O
*hashing* consistente, tema do capítulo 5 de Xu (2020), é o que permite redistribuir os canais de
um servidor que saiu sem remapear todos os demais.

O Mattermost resolve o problema de forma mais simples, coerente com seu modelo de implantação. Os
clientes não escolhem o servidor: conectam-se a um *proxy* (tipicamente NGINX), que distribui as
conexões entre os nós saudáveis do *cluster* e verifica sua saúde pelo *endpoint*
`/api/v4/system/ping` (MATTERMOST, 2026b). Os nós, por sua vez, precisam descobrir uns aos outros
para trocar eventos. Cada nó registra seu endereço na tabela `ClusterDiscovery` do banco de dados
compartilhado, e os nós com o mesmo nome de *cluster* passam a comunicar-se por um protocolo de
fofoca (*gossip*) na porta 8074, que deve admitir TCP e UDP (MATTERMOST, 2026b). O banco de dados,
que todos os nós já precisam acessar, faz assim o papel de registro de serviços, dispensando um
componente adicional como ZooKeeper ou Consul. Trata-se de mais um exemplo de decisão orientada à
simplicidade operacional.

## 4.3.4 Os *containers* do Mattermost

A Figura 4.3.1 apresenta os *containers* de uma implantação do Mattermost em *cluster* e o caminho
de uma mensagem, da publicação à entrega em tempo real e à notificação *push*. O diagrama foi
reconstruído a partir da documentação de alta disponibilidade (MATTERMOST, 2026b) e do código do
servidor (MATTERMOST, 2026a); a sequência dos passos acompanha a Figura 5.3.2 e a difusão entre os
nós, a Figura 5.3.4.

**Figura 4.3.1** – *Containers* do Mattermost em *cluster* e o caminho de uma mensagem.

![Containers de uma implantação do Mattermost em cluster: clientes, proxy reverso NGINX, dois servidores Mattermost em Go com seus processos de plugins, PostgreSQL, armazenamento de arquivos, busca corporativa, proxy de push e os serviços APNs e FCM, com o fluxo numerado do envio e da entrega de uma mensagem](../assets/diagramas/mattermost-50k.svg)

Passos numerados (envio de uma mensagem e entrega em tempo real):

1. O cliente envia a mensagem, `POST /api/v4/posts` com o `pending_post_id`, ao proxy reverso.
2. O NGINX repassa a requisição a um dos nós saudáveis do *cluster*, aqui o servidor 1.
3. O nó executa o gancho `MessageWillBePosted` nos processos de *plugins*, por RPC; um *plugin*
   pode alterar ou rejeitar a mensagem.
4. O nó grava a mensagem no PostgreSQL. Nenhum cliente é avisado antes dessa gravação, e o
   cliente que publicou recebe a resposta.
5. O nó publica o evento `posted` nos seus *hubs* locais e o envia aos demais nós como mensagem de
   *cluster* em modo confiável, pelo canal de fofoca da porta 8074.
6. Cada nó entrega o evento, pelos seus *hubs* de WebSocket, às conexões que mantém, e a conexão
   WSS do cliente passa pelo proxy reverso. A seta mostra a entrega pelo servidor 2, que recebe a
   mensagem de *cluster* e não a retransmite; o servidor 1 faz o mesmo com as suas conexões.
7. Para os destinatários ausentes, o nó envia o pedido de notificação ao proxy de *push* (HPNS ou
   próprio).
8. O proxy de *push* repassa a notificação, por TLS, ao APNs da Apple ou ao FCM do Google, que a
   entregam ao aparelho. Com as notificações somente com identificador, o aplicativo busca o
   conteúdo no servidor da organização.

As setas finas sem número representam relações que não pertencem a esse fluxo: o proxy reverso
também distribui requisições e conexões ao servidor 2, que tem seus próprios processos de
*plugins*, grava no mesmo PostgreSQL e também envia pedidos ao proxy de *push*; os dois nós usam o
armazenamento compartilhado de arquivos, e o servidor 1 consulta a busca corporativa opcional. A
conexão WSS entre o cliente e o proxy aparece na seta 6, desenhada do nó ao cliente para não se
sobrepor à seta 1.

Fonte: elaborado pelo autor (2026), com base em Mattermost (2026a, 2026b, 2026c, 2026f) e no estilo
de diagrama de Xu (2020). Linhas contínuas: comunicação síncrona; linhas tracejadas: comunicação
assíncrona. Código-fonte da figura: `diagramas/d2/mattermost-50k.d2`.

Os *containers* e suas responsabilidades são os seguintes:

- **Proxy reverso:** termina o TLS, distribui as requisições e as conexões WebSocket entre os nós
  e retira de circulação os nós que não respondem ao *ping* de saúde (MATTERMOST, 2026b).
- **Servidor Mattermost:** um binário Go que contém a API REST, o *hub* de WebSocket, o serviço de
  notificações, o agendador de tarefas e o gerenciador de *plugins* (MATTERMOST, 2026c). Em
  *cluster*, um dos nós é eleito líder, por um algoritmo do tipo *bully*, para executar tarefas
  agendadas que não devem rodar em duplicidade, como a sincronização com LDAP (MATTERMOST, 2026b).
- **PostgreSQL:** a documentação atual exige PostgreSQL 14 ou superior (MATTERMOST, 2026e). Em
  alta disponibilidade, podem ser configuradas réplicas de leitura, que tanto servem de cópia
  redundante quanto aumentam a capacidade para usuários simultâneos (MATTERMOST, 2026b).
- **Armazenamento de arquivos:** em *cluster*, os anexos devem ficar em armazenamento compartilhado
  compatível com S3 ou em NAS montado em todos os nós. A documentação adverte que manter o
  armazenamento local em cada nó faz a alta disponibilidade funcionar incorretamente e pode
  corromper os arquivos (MATTERMOST, 2026b), o que é uma consequência direta de exigir que os nós
  de aplicação sejam intercambiáveis.
- **Proxy de *push*:** recebe do servidor os pedidos de notificação e os encaminha à Apple e ao
  Google (Seção 4.3.7).
- **Processos de *plugins*:** cada *plugin* com componente de servidor roda como subprocesso de
  cada nó, comunicando-se com ele por RPC (Seção 4.3.8).


## 4.3.5 Implantação: do servidor único ao *cluster*

A documentação descreve duas configurações típicas, representadas na Figura 4.3.2. Na
**implantação de servidor único**, recomendada para começar, o repositório oficial
`mattermost/docker` sobe dois *containers*, `postgres` e `mattermost`, opcionalmente atrás de um
NGINX, e declara como volumes externos ao *container* da aplicação a configuração, os dados
(anexos), os *logs*, os *plugins* de servidor e de cliente e os índices de busca do Bleve
(MATTERMOST, 2026j). O *container* da aplicação é configurado com sistema de arquivos somente de
leitura (`read_only`) e com limite de memória de 4 GB, o que reforça a regra de que todo estado
persistente fica fora dele. Na **implantação em alta disponibilidade**, há vários nós de
aplicação idênticos atrás de *proxies* redundantes, um banco primário com réplicas de leitura e
armazenamento de arquivos compartilhado; a documentação recomenda guardar a configuração no banco,
e não em arquivos `config.json` locais, para que todos os nós a leiam igualmente (MATTERMOST,
2026b). A regra geral é que **todo estado que precise ser compartilhado sai do nó**; o que
permanece em cada nó são as conexões WebSocket e os *caches*, que por isso exigem os mecanismos de
sincronização estudados na [Seção 5.3](../capitulo-5-10-mil-pes/mattermost.md).

**Figura 4.3.2** – Implantação: servidor único com Docker Compose e *cluster* de alta disponibilidade.

![Duas implantações do Mattermost. No servidor único com Docker Compose, um nginx opcional com TLS encaminha ao contêiner mattermost (porta 8065, read_only, 4 GB), que usa o postgres e os volumes de config, data, logs, plugins e bleve-indexes. Na alta disponibilidade, proxies redundantes com health check em /api/v4/system/ping distribuem entre os nós 1, 2 e 3, que trocam gossip assíncrono na porta 8074; os nós acessam o PostgreSQL primário, as réplicas de leitura e o S3 ou NAS compartilhado](../assets/diagramas/mattermost-impl.svg)

Fonte: elaborado pelo autor (2026), com base em Mattermost (2026b, 2026j). Linhas contínuas:
comunicação síncrona; linhas tracejadas: comunicação assíncrona. Código-fonte da figura:
`diagramas/d2/mattermost-impl.d2`.

A diferença entre as duas configurações é quase inteiramente de **infraestrutura**, e não de
código: o mesmo binário funciona como nó único ou como membro de um *cluster*, conforme a
configuração e a licença. Argumenta-se aqui que essa propriedade é a materialização, a 50.000 pés,
da restrição de *self-hosting*: a organização pode começar com um servidor e crescer sem mudar de
produto. O custo é que as decisões de escala do Mattermost são limitadas ao que um binário
homogêneo permite. Não é possível, por exemplo, escalar separadamente a API e as conexões, como o
Slack faz com seus servidores de administração e de *gateway*.

## 4.3.6 Estilo: orientado a eventos com conexões *stateful*

O estilo dominante no núcleo do Mattermost é **orientado a eventos**: toda ação relevante, como
publicar uma mensagem, reagir, entrar em um canal ou mudar de status, produz um evento de WebSocket
entregue aos clientes interessados, que atualizam suas interfaces em reação a ele, e não por
consultas periódicas (WASHIZAKI, 2024, cap. 3, seção 5.7). Convém, porém, qualificar a
classificação. Richards e Ford (2020, cap. 14) descrevem a arquitetura orientada a eventos como um
estilo **distribuído**, de processadores desacoplados; no Mattermost, os processadores são
componentes de um mesmo processo, e a persistência é síncrona e transacional. Argumenta-se aqui
que ele é melhor descrito como um **núcleo monolítico com uma camada de difusão orientada a
eventos**, híbrido que os próprios autores reconhecem como frequente, com traços do estilo
**microkernel** na extensão por *plugins* (RICHARDS; FORD, 2020, cap. 12, 14). A Tabela 4.3.2
compara as três arquiteturas discutidas até aqui.

**Tabela 4.3.2** – Comparação entre o modelo de Xu, o Slack e o Mattermost.

| Aspecto | Modelo de Xu (2020, cap. 12) | Slack (THANGUDU, 2023) | Mattermost v11.11.1 |
|---|---|---|---|
| Envio da mensagem | WebSocket ou HTTP | API da aplicação web | API REST |
| Recebimento | WebSocket com servidor de *chat* | WebSocket com *gateway* regional | WebSocket com qualquer nó, via *proxy* |
| Serviços *stateful* | Servidores de *chat* e de presença | Servidores de canal, *gateway* e presença | Cada nó (conexões e *caches*) |
| Serviços *stateless* | API, descoberta, notificação | Servidores de administração, aplicação web | A API REST de cada nó |
| Descoberta | ZooKeeper | Consul e anéis de *hashing* consistente | Tabela `ClusterDiscovery` e *gossip* |
| Roteamento de eventos | Fila de sincronização por destinatário | Servidor de canal envia aos *gateways* assinantes | Difusão a todos os nós, que filtram localmente |
| Armazenamento do histórico | Chave-valor | Não detalhado na fonte | PostgreSQL |

Fonte: elaborado pelo autor (2026), com base nas fontes indicadas.

A linha "roteamento de eventos" contém a diferença mais significativa. O Slack roteia: cada
mensagem vai apenas aos *gateways* que assinam o canal. O Mattermost difunde: cada nó envia o
evento a todos os outros nós, e cada nó entrega o evento às conexões locais interessadas. A
difusão é mais simples e não exige que nenhum componente saiba onde cada usuário está conectado,
mas o tráfego entre nós cresce com o número de nós. Em um *cluster* de poucas máquinas, como o de
uma organização típica, a difusão é adequada; em um serviço multiorganização com milhares de
máquinas, como o Slack, ela seria proibitiva. A mesma decisão de projeto, avaliada em contextos
distintos, recebe respostas diferentes, como prevê a primeira lei.


Quanto ao **quantum arquitetural**, isto é, a unidade implantável de forma independente com alta
coesão funcional e conascência síncrona (RICHARDS; FORD, 2020, p. 92), o Mattermost tem, em
termos práticos, um único quantum: todos os nós executam o mesmo binário, compartilham o mesmo
banco de dados e precisam estar na mesma versão. Richards e Ford (2020, cap. 14) observam que,
mesmo em arquiteturas orientadas a eventos, processadores que compartilham um banco pertencem ao
mesmo quantum. Os *plugins* são uma exceção parcial: podem ser instalados, atualizados e
desativados sem reimplantar o servidor, mas dependem dele para tudo, inclusive para acessar dados.
No Slack, ao contrário, os vários tipos de servidor descritos por Thangudu (2023) sugerem vários
quanta, embora a fonte não detalhe como são implantados.

Sobre os **dados**, a divisão é nítida: mensagens, canais, usuários e configuração ficam no
PostgreSQL, anexos no armazenamento de arquivos, índices de busca no Bleve ou no mecanismo de busca
corporativo, e o estado volátil (conexões, *caches*, presença) na memória de cada nó. A comunicação
entre cliente e servidor é síncrona para comandos (REST) e assíncrona para notificações
(WebSocket); entre os nós, é assíncrona, por mensagens de *cluster*.

## 4.3.7 Notificações *push*

Quando o destinatário não está conectado, a mensagem precisa alcançá-lo por outro canal. No
modelo de Xu (2020, cap. 10), o servidor envia a notificação, com o identificador do dispositivo,
ao serviço da Apple (APNs) ou ao Firebase Cloud Messaging (FCM) do Google, que a entregam ao
aparelho; filas por tipo de notificação isolam a falha de um provedor, e mecanismos de nova
tentativa e de deduplicação compensam o fato de que a entrega exatamente uma vez não pode ser
garantida.

O Mattermost introduz um intermediário adicional, o **proxy de notificações *push***. Por padrão,
o servidor de cada organização envia as notificações a um serviço hospedado pela própria
Mattermost, o HPNS (*Hosted Push Notification Service*), que as repassa à Apple e ao Google por
conexões TLS. Para testes, há um serviço gratuito sem garantia de nível de serviço, o TPNS. Segundo
a documentação, ambos só funcionam com os aplicativos pré-compilados distribuídos nas lojas da
Apple e do Google; organizações que compilam seus próprios aplicativos precisam hospedar o próprio
*proxy*, cujo código é aberto (MATTERMOST, 2026f). A documentação registra que o HPNS
não armazena dados das notificações (MATTERMOST, 2026f).

Esse arranjo contém uma tensão com a soberania de dados. Por padrão, o texto da mensagem viaja em
claro, dentro da notificação, até a Apple ou o Google. A resposta do Mattermost é a opção de
notificações **somente com identificador** (*ID-only push notifications*): em vez do texto, a
notificação transporta apenas o identificador da mensagem, e o aplicativo, ao recebê-la, busca o
conteúdo diretamente no servidor da organização por um canal cifrado. A documentação reconhece o
custo dessa escolha: o usuário pode perceber um pequeno atraso até a notificação ser exibida
(MATTERMOST, 2026f). Trata-se de uma troca explícita de latência por privacidade, coerente com a
ordem das características definida na [Seção 3.3.7](../capitulo-3-100-mil-pes/mattermost.md).

## 4.3.8 ADR reconstruído: *plugins* em processos separados

Uma das decisões mais características do Mattermost é a forma de executar extensões no servidor.
Os *plugins* com componente de servidor são programas Go compilados à parte, que o servidor inicia
como **subprocessos** e com os quais se comunica por **chamadas de procedimento remoto** (RPC),
usando a biblioteca `go-plugin` da HashiCorp (MATTERMOST, 2026g). A decisão não está registrada
como ADR no repositório; o registro a seguir é uma **reconstrução** do autor, no formato do
[Apêndice B](../apendice-b-adr/index.md), a partir do código, da documentação para desenvolvedores e da documentação da
biblioteca.

!!! note "ADR-MM-01 (reconstruído) – Executar *plugins* de servidor em processos separados"
    **Status:** aceito (reconstrução a partir de MATTERMOST, 2026a, 2026g; HASHICORP, 2026).

    **Contexto.** O Mattermost precisa admitir extensões escritas por terceiros, de qualidade e
    confiabilidade desconhecidas, que reagem a eventos do servidor (*hooks*) e acessam seus dados
    (API). O servidor guarda as conversas de toda a organização e mantém milhares de conexões
    WebSocket; uma falha fatal (*panic*) no processo derruba todas elas. A linguagem Go oferece
    carregamento dinâmico de bibliotecas com limitações, e o produto é instalado por terceiros em
    ambientes heterogêneos.

    **Decisão.** Cada *plugin* com componente de servidor é distribuído como executável próprio e
    executado como subprocesso do servidor. A comunicação ocorre por RPC sobre uma conexão local,
    por meio da biblioteca `go-plugin`. O servidor expõe ao *plugin* uma interface `API` e invoca
    nele uma interface `Hooks`; nenhum outro acesso à memória do servidor é possível.

    **Alternativas consideradas.** (a) *Plugins* compilados junto com o servidor: exigem
    recompilar e reimplantar o produto a cada extensão. (b) Bibliotecas carregadas dinamicamente
    no mesmo processo: maior desempenho, mas uma falha no *plugin* derruba o servidor e o *plugin*
    tem acesso a toda a memória do processo. (c) *Plugins* como serviços remotos acessados por
    HTTP: isolamento máximo, mas tornam a implantação distribuída, o que contraria a restrição de
    *self-hosting*.

    **Consequências.** Positivas: uma falha fatal no *plugin* não derruba o servidor; o *plugin*
    só enxerga as interfaces e os argumentos que recebe; *plugins* podem ser instalados, ativados,
    reiniciados e desativados sem reiniciar o servidor; o servidor pode verificar periodicamente a
    saúde de cada *plugin* e reiniciá-lo. Negativas: cada chamada de *hook* tem o custo de
    serialização e de comunicação entre processos; os argumentos precisam ser serializáveis; em
    *cluster*, cada nó executa sua própria cópia do *plugin*, o que obriga o *plugin* a não guardar
    estado em memória (MATTERMOST, 2026h).

    **Conformidade.** O servidor só fala com *plugins* por meio das interfaces `API` e `Hooks`
    do pacote `server/public/plugin`, cujo código de transporte RPC é gerado automaticamente.

As consequências positivas correspondem quase literalmente às vantagens que a documentação da
`go-plugin` atribui aos subprocessos: *plugins* não conseguem derrubar o processo hospedeiro e
podem ser relativamente seguros, porque só acessam as interfaces e os argumentos que recebem, e
não todo o espaço de memória do processo; a mesma documentação admite que bibliotecas
compartilhadas teriam desempenho muito superior, custo que, na experiência da empresa, não se
mostrou limitante (HASHICORP, 2026). Richards e Ford (2020, cap. 12) oferecem o pano de fundo: no
estilo microkernel, o acesso remoto aos *plugins* melhora o desacoplamento, mas torna a
arquitetura distribuída e difícil de implantar em produtos instalados no cliente. O Mattermost
situa-se entre os extremos: outro processo, mas a mesma máquina, sob controle do servidor. A
verificação desta decisão no código é feita na subida de verificação da [Seção 6.3.11](../capitulo-6-solo/mattermost.md).


## 4.3.9 Caixa-preta

!!! caixa-preta "Slack, 4 de janeiro de 2021: o primeiro dia útil do ano"
    No primeiro dia útil após as festas de fim de ano, o Slack ficou indisponível para muitos
    usuários durante parte da manhã no horário do Pacífico. Segundo o relatório público, os
    clientes voltaram das férias com *caches* frios e carregaram mais dados do que o habitual na
    primeira conexão; a empresa descreve a passagem do período mais tranquilo do ano para um dos
    dias de maior movimento "literalmente da noite para o dia" (NOLAN, 2021). Os sistemas do
    próprio Slack escalaram, mas os *Transit Gateways* da AWS, componentes de rede que ligavam
    as nuvens privadas virtuais da empresa, não escalaram com a rapidez necessária e passaram a
    descartar pacotes. A perda de pacotes aumentou a latência entre a camada web e seus
    serviços de apoio. O escalonamento automático reagiu primeiro à queda do uso de CPU,
    removendo instâncias, e depois à saturação de *threads*, tentando acrescentar 1.200
    servidores de uma vez; o serviço que configura novas instâncias, operando sobre a mesma
    rede degradada, atingiu o limite de arquivos abertos do Linux e uma cota da AWS. Para
    agravar, os painéis de monitoramento ficaram indisponíveis porque estavam em uma rede virtual
    diferente da de seus bancos de dados e dependiam, eles também, dos *Transit Gateways*. A rede
    voltou ao normal depois que a AWS aumentou manualmente a capacidade dos *gateways* (NOLAN,
    2021).

!!! caixa-preta "Facebook e WhatsApp, 4 de outubro de 2021: quando o DNS some"
    Durante uma manutenção de rotina, um comando destinado a avaliar a capacidade disponível do
    *backbone* global da empresa desconectou involuntariamente todas as conexões desse
    *backbone*; a ferramenta de auditoria que deveria impedir o comando tinha um defeito que a
    impediu de fazê-lo (JANARDHAN, 2021). Sem o *backbone*, os centros de dados ficaram
    isolados. Os servidores de DNS da empresa, projetados para deixar de anunciar suas rotas por
    BGP quando não alcançam os centros de dados, interpretaram a situação como um defeito local e
    retiraram os anúncios. O resultado foi que os servidores de DNS ficaram inalcançáveis, embora
    continuassem funcionando, e o restante da internet deixou de encontrar Facebook, Instagram e
    WhatsApp. As ferramentas internas usadas para diagnosticar falhas dependiam da mesma
    infraestrutura, e foi preciso enviar engenheiros aos centros de dados. Na recuperação, a
    empresa religou os serviços gradualmente, para evitar que o pico de carga e de consumo de
    energia provocasse novas falhas (JANARDHAN, 2021).

Os dois incidentes ilustram falácias da computação distribuída catalogadas por Richards e Ford
(2020, cap. 9): "a rede é confiável", "a largura de banda é infinita" e "há apenas um
administrador", no caso do Slack, em que um componente de rede de terceiros, invisível no diagrama
da aplicação, tornou-se o gargalo; e "a topologia nunca muda", no caso do Facebook. Ambos mostram
ainda o risco de **dependências circulares** entre o sistema e as ferramentas usadas para
repará-lo. Para sistemas de conexões persistentes, a lição específica é a **tempestade de
reconexões**: quando a rede volta, todos os clientes tentam reconectar-se e ressincronizar ao
mesmo tempo, o que torna valiosos os mecanismos de retomada sem recarga completa de estado, como a
fila morta do Mattermost ([Seção 5.3.6](../capitulo-5-10-mil-pes/mattermost.md)). A organização que opera o Mattermost em infraestrutura
própria herda essas preocupações: a soberania vem acompanhada da responsabilidade pela
resiliência.

## Referências

FETTE, I.; MELNIKOV, A. **RFC 6455**: the WebSocket protocol. [*S. l.*]: Internet Engineering
Task Force, 2011. Disponível em: <https://www.rfc-editor.org/rfc/rfc6455>. Acesso em:
18 out. 2026.

HASHICORP. **hashicorp/go-plugin**: Golang plugin system over RPC. GitHub, 2026. Disponível em:
<https://github.com/hashicorp/go-plugin>. Acesso em: 18 out. 2026.

JANARDHAN, S. **More details about the October 4 outage**. Engineering at Meta, 5 out. 2021.
Disponível em: <https://engineering.fb.com/2021/10/05/networking-traffic/outage-details/>.
Acesso em: 18 out. 2026.

LORETO, S. *et al.* **RFC 6202**: known issues and best practices for the use of long polling
and streaming in bidirectional HTTP. [*S. l.*]: Internet Engineering Task Force, 2011.
Disponível em: <https://www.rfc-editor.org/rfc/rfc6202>. Acesso em: 18 out. 2026.

MATTERMOST. **mattermost/mattermost**: repositório de código-fonte, versão v11.11.1. GitHub,
2026a. Disponível em: <https://github.com/mattermost/mattermost/tree/v11.11.1>. Acesso em:
18 out. 2026.

MATTERMOST. **High availability cluster-based deployment**. Mattermost Documentation, 2026b.
Disponível em:
<https://docs.mattermost.com/administration-guide/scale/high-availability-cluster-based-deployment.html>.
Acesso em: 18 out. 2026.

MATTERMOST. **Mattermost Server**. Mattermost Documentation, 2026c. Disponível em:
<https://docs.mattermost.com/product-overview/server>. Acesso em: 18 out. 2026.

MATTERMOST. **Software and hardware requirements**. Mattermost Documentation, 2026e.
Disponível em: <https://docs.mattermost.com/deployment-guide/software-hardware-requirements.html>.
Acesso em: 18 out. 2026.

MATTERMOST. **Push notification server configuration settings**. Mattermost Documentation,
2026f. Disponível em:
<https://docs.mattermost.com/administration-guide/configure/push-notification-server-configuration-settings.html>.
Acesso em: 18 out. 2026.

MATTERMOST. **Server plugins**. Mattermost Developers, 2026g. Disponível em:
<https://developers.mattermost.com/integrate/plugins/components/server/>. Acesso em:
18 out. 2026.

MATTERMOST. **High availability** (plugins). Mattermost Developers, 2026h. Disponível em:
<https://developers.mattermost.com/integrate/plugins/components/server/ha/>. Acesso em:
18 out. 2026.

MATTERMOST. **mattermost/docker**: the official Docker deployment solution for Mattermost.
GitHub, 2026j. Disponível em: <https://github.com/mattermost/docker>. Acesso em: 18 out. 2026.

NOLAN, L. **Slack's outage on January 4th 2021**. Slack Engineering, 1 fev. 2021. Disponível em:
<https://slack.engineering/slacks-outage-on-january-4th-2021/>. Acesso em: 18 out. 2026.

RICHARDS, M.; FORD, N. **Fundamentals of software architecture**: an engineering approach.
Sebastopol: O'Reilly, 2020.

THANGUDU, S. **Real-time messaging**. Slack Engineering, 11 abr. 2023. Disponível em:
<https://slack.engineering/real-time-messaging/>. Acesso em: 18 out. 2026.

WASHIZAKI, H. (ed.). **Guide to the software engineering body of knowledge (SWEBOK Guide)**:
version 4.0. Los Alamitos: IEEE Computer Society, 2024.

XU, A. **System design interview**: an insider's guide. 2. ed. [*S. l.*]: Byte Code LLC, 2020.
