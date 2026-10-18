# 6.3 Mattermost: o código

<span class="altitude solo">Solo</span>

Esta seção pousa no código do servidor Mattermost para verificar onde se materializam as decisões
discutidas nas [Seções 4.3](../capitulo-4-50-mil-pes/mattermost.md) e 5.3. Todos os trechos pertencem à *tag* **v11.11.1** do repositório
`mattermost/mattermost`, publicada em 24 de setembro de 2026, e foram conferidos linha a linha nessa
versão; os *links* apontam para a *tag*, e não para o ramo principal, porque nomes de arquivos e
números de linha mudam entre versões (MATTERMOST, 2026a). Os trechos em Go são literais; os
trechos em Java são didáticos.

## 6.3.1 Mapa do repositório

A Tabela 6.3.1 associa os componentes da [Seção 5.3](../capitulo-5-10-mil-pes/mattermost.md) aos arquivos que os realizam.

**Tabela 6.3.1** – Componentes e arquivos do Mattermost v11.11.1 estudados nesta seção.

| Componente | Arquivo | Papel |
|---|---|---|
| Modelo | `server/public/model/post.go` | Estrutura `Post`, validação e preparação |
| API REST de mensagens | `server/channels/api4/post.go` | *Handler* `createPost` |
| Criação de mensagens | `server/channels/app/post.go` | Deduplicação, *hooks*, gravação e eventos |
| *Hub* de WebSocket | `server/channels/app/platform/web_hub.go` | Registro de conexões, índices e difusão local |
| Conexão WebSocket | `server/channels/app/platform/web_conn.go` | Leitura, escrita, sequência, fila morta, *ping* |
| Publicação e *cluster* | `server/channels/app/platform/cluster.go` e `cluster_handlers.go` | Publicação local e envio aos demais nós |
| Contrato do *cluster* | `server/einterfaces/cluster.go` | Interface implementada fora do núcleo aberto |
| Persistência | `server/channels/store/` e `server/channels/app/platform/service.go` | Interfaces e camadas do *store* |
| API de *plugins* | `server/public/plugin/hooks.go`, `api.go`, `supervisor.go`, `health_check.go` | Contratos, subprocesso e verificação de saúde |
| Comandos de barra | `server/channels/app/command.go` e `server/channels/app/slashcommands/` | Registro e execução |

Fonte: elaborado pelo autor (2026), com base em Mattermost (2026a).

## 6.3.2 Modelo de domínio e DDD tático

O modelo do Mattermost reside em `server/public/model`, como estruturas Go compartilhadas pela
API, pela aplicação, pela persistência e pelos *plugins*. A Listagem 6.3.1 mostra o início da
estrutura `Post`, que representa uma mensagem.

**Listagem 6.3.1** – Código real (Go, Mattermost v11.11.1): início da estrutura `Post`.

```go
type Post struct {
	Id         string `json:"id" xml:"Id"`
	CreateAt   int64  `json:"create_at" xml:"CreateAt"`
	UpdateAt   int64  `json:"update_at" xml:"UpdateAt"`
	EditAt     int64  `json:"edit_at" xml:"EditAt"`
	DeleteAt   int64  `json:"delete_at" xml:"DeleteAt"`
	IsPinned   bool   `json:"is_pinned" xml:"IsPinned"`
	UserId     string `json:"user_id" xml:"UserId"`
	ChannelId  string `json:"channel_id" xml:"ChannelId"`
	RootId     string `json:"root_id" xml:"RootId"`
	OriginalId string `json:"original_id" xml:"OriginalId"`

	Message string `json:"message" xml:"Message"`
```

Fonte: Mattermost (2026a), [`model/post.go#L137-L149`](https://github.com/mattermost/mattermost/blob/v11.11.1/server/public/model/post.go#L137-L149).

Pela lente do DDD tático (EVANS, 2003), `Post` é uma **entidade**: tem identidade própria (`Id`),
que persiste através de edições, e um ciclo de vida marcado pelos instantes de criação,
atualização, edição e exclusão lógica (`DeleteAt`). A referência ao canal (`ChannelId`) e à
mensagem raiz da *thread* (`RootId`) é feita por identificador, e não por objeto, o que é coerente
com a recomendação de que agregados se refiram uns aos outros pela identidade da raiz. Argumenta-se
aqui que o **canal** é a fronteira de consistência relevante para as mensagens, porque é dentro
dele que se exigem ordem e controle de acesso ([Seção 5.3.6](../capitulo-5-10-mil-pes/mattermost.md)); o código, porém, não implementa o
canal como raiz de agregado que intermedie a criação de mensagens: a criação passa pela camada de
aplicação, que verifica permissões e grava o `Post` diretamente.

As etiquetas de serialização (`json`, `xml`) nas linhas da estrutura revelam o acoplamento do
modelo aos formatos de transporte, discutido na [Seção 5.3.8](../capitulo-5-10-mil-pes/mattermost.md). O comportamento que reside na própria
entidade é de validação e preparação. A Listagem 6.3.2 mostra o método executado antes da
gravação.

**Listagem 6.3.2** – Código real (Go, Mattermost v11.11.1): preparação de uma mensagem antes da gravação.

```go
func (o *Post) PreSave() {
	if o.Id == "" {
		o.Id = NewId()
	}

	o.OriginalId = ""

	if o.CreateAt == 0 {
		o.CreateAt = GetMillis()
	}

	o.UpdateAt = o.CreateAt
	o.PreCommit()
}
```

Fonte: Mattermost (2026a), [`model/post.go#L709-L722`](https://github.com/mattermost/mattermost/blob/v11.11.1/server/public/model/post.go#L709-L722).

O método atribui um identificador novo, preenche o instante de criação quando ausente e sincroniza
o instante de atualização; o método `IsValid`, no mesmo arquivo, verifica identificadores,
instantes e tamanho do texto ([`model/post.go#L500-L520`](https://github.com/mattermost/mattermost/blob/v11.11.1/server/public/model/post.go#L500-L520)).
As regras de negócio mais ricas, como permissões, menções, notificações e *hooks*, estão nos
métodos da camada de aplicação. É o que Fowler (2002) descreve como lógica organizada em
**Transaction Scripts** sobre uma **Service Layer**, com um modelo predominantemente anêmico: uma
escolha consciente de simplicidade, mas que dispersa as invariantes do domínio por vários métodos
de `App`.

## 6.3.3 O *hub*: *Mediator* e *Observer* em produção

A Listagem 6.3.3 mostra como o servidor escolhe o *hub* de um usuário: calcula um *hash* do
identificador com uma semente própria do processo e toma o resto da divisão pelo número de *hubs*,
que, segundo o comentário da função `hubStart`, é igual ao número de processadores, valor que
testes de desempenho indicaram como ideal ([`web_hub.go#L124-L142`](https://github.com/mattermost/mattermost/blob/v11.11.1/server/channels/app/platform/web_hub.go#L124-L142)).

**Listagem 6.3.3** – Código real (Go, Mattermost v11.11.1): seleção do *hub* de um usuário por *hash*.

```go
// GetHubForUserId returns the hub for a given user id.
func (ps *PlatformService) GetHubForUserId(userID string) *Hub {
	if len(ps.hubs) == 0 {
		return nil
	}

	// TODO: check if caching the userID -> hub mapping
	// is worth the memory tradeoff.
	// https://mattermost.atlassian.net/browse/MM-26629.
	var hash maphash.Hash
	hash.SetSeed(ps.hashSeed)
	_, err := hash.WriteString(userID)
	if err != nil {
		ps.logger.Error("Unable to write userID to hash", mlog.String("userID", userID), mlog.Err(err))
	}
	index := hash.Sum64() % uint64(len(ps.hubs))

	return ps.hubs[int(index)]
}
```

Fonte: Mattermost (2026a), [`web_hub.go#L157-L175`](https://github.com/mattermost/mattermost/blob/v11.11.1/server/channels/app/platform/web_hub.go#L157-L175).

Cada *hub*, ao receber um evento, consulta seu índice de conexões para encontrar os destinatários
locais, pelo identificador da conexão, do usuário ou do canal, e entrega o evento a cada um por meio
da função interna da Listagem 6.3.4.

**Listagem 6.3.4** – Código real (Go, Mattermost v11.11.1): entrega de um evento a uma conexão, com descarte do consumidor lento.

```go
broadcast := func(webConn *WebConn) {
	if !connIndex.Has(webConn) {
		return
	}
	if webConn.ShouldSendEvent(msg) {
		select {
		case webConn.send <- h.runBroadcastHooks(msg, webConn, broadcastHooks, broadcastHookArgs):
		default:
			// Don't log the warning if it's an inactive connection.
			if webConn.Active.Load() {
				mlog.Error("webhub.broadcast: cannot send, closing websocket for user",
					mlog.String("user_id", webConn.UserId),
					mlog.String("conn_id", webConn.GetConnectionID()))
			}
			closeAndRemoveConn(connIndex, webConn)
		}
	}
}
```

Fonte: Mattermost (2026a), [`web_hub.go#L725-L742`](https://github.com/mattermost/mattermost/blob/v11.11.1/server/channels/app/platform/web_hub.go#L725-L742).

Três elementos dos padrões aparecem nesse trecho. A chamada `ShouldSendEvent` é o filtro do
assinante: cada conexão decide, com base no alcance do evento e nas permissões da sessão, se o
evento lhe diz respeito. O envio pelo canal `webConn.send` é a notificação do *Observer*, que não
conhece o que a conexão fará com o evento. E o ramo `default` do `select` é a política de
contrapressão (*backpressure*): se o canal está cheio, a conexão é fechada em vez de bloquear o
*hub*. Os produtores de eventos, por sua vez, nunca acessam conexões; eles só chamam `Publish`, o
que caracteriza o *hub* como *Mediator* do ponto de vista de quem publica.

## 6.3.4 Publicação local e no *cluster*

A Listagem 6.3.5 é o ponto de entrada de toda publicação de evento: entrega aos *hubs* locais e,
se há implementação de *cluster*, envia aos demais nós, escolhendo o modo confiável para os
eventos cuja perda é cara.

**Listagem 6.3.5** – Código real (Go, Mattermost v11.11.1): publicação de um evento e envio ao *cluster*.

```go
func (ps *PlatformService) Publish(message *model.WebSocketEvent) {
	if ps.metricsIFace != nil {
		ps.metricsIFace.IncrementWebsocketEvent(message.EventType())
	}

	ps.PublishSkipClusterSend(message)

	if ps.clusterIFace != nil {
		data, err := message.ToJSON()
		if err != nil {
			mlog.Warn("Failed to encode message to JSON", mlog.Err(err))
		}
		cm := &model.ClusterMessage{
			Event:    model.ClusterEventPublish,
			SendType: model.ClusterSendBestEffort,
			Data:     data,
		}

		if message.EventType() == model.WebsocketEventPosted ||
			message.EventType() == model.WebsocketEventPostEdited ||
			message.EventType() == model.WebsocketEventDirectAdded ||
			message.EventType() == model.WebsocketEventGroupAdded ||
			message.EventType() == model.WebsocketEventAddedToTeam ||
			message.GetBroadcast().ReliableClusterSend {
			cm.SendType = model.ClusterSendReliable
		}

		ps.clusterIFace.SendClusterMessage(cm)
	}
}
```

Fonte: Mattermost (2026a), [`cluster.go#L189-L218`](https://github.com/mattermost/mattermost/blob/v11.11.1/server/channels/app/platform/cluster.go#L189-L218).

Do outro lado, o tratador `ClusterPublishHandler` decodifica o evento recebido e chama
`PublishSkipClusterSend`, a mesma função usada para a entrega local, que não reenvia o evento ao
*cluster* ([`cluster_handlers.go#L33-L41`](https://github.com/mattermost/mattermost/blob/v11.11.1/server/channels/app/platform/cluster_handlers.go#L33-L41)).
É essa assimetria que impede que um evento circule indefinidamente entre os nós. O campo
`clusterIFace` é do tipo `einterfaces.ClusterInterface`, cuja implementação não está entre os
diretórios públicos da versão estudada; quando ele é nulo, o servidor simplesmente opera como nó
único ([`einterfaces/cluster.go#L13-L25`](https://github.com/mattermost/mattermost/blob/v11.11.1/server/einterfaces/cluster.go#L13-L25)).

## 6.3.5 Sequência, *heartbeat* e presença

A Listagem 6.3.6 reúne as constantes que governam o *heartbeat* e a fila morta e o ponto da rotina
de escrita em que cada evento recebe seu número de sequência.

**Listagem 6.3.6** – Código real (Go, Mattermost v11.11.1): constantes de temporização e numeração dos eventos por conexão (Go, literal, dois trechos).

```go
writeWaitTime                  = 30 * time.Second
pongWaitTime                   = 100 * time.Second
pingInterval                   = (pongWaitTime * 6) / 10
authCheckInterval              = 5 * time.Second
webConnMemberCacheTime         = 1000 * 60 * 30 // 30 minutes
deadQueueSize                  = 128            // Approximated from /proc/sys/net/core/wmem_default / 2048 (avg msg size)

// ... (trecho da rotina de escrita, writePump)
if evtOk {
	evt = evt.SetSequence(wc.Sequence)
	err = evt.Encode(enc, &buf)
	wc.Sequence++
```

Fonte: Mattermost (2026a), [`web_conn.go#L37-L42`](https://github.com/mattermost/mattermost/blob/v11.11.1/server/channels/app/platform/web_conn.go#L37-L42) e [`web_conn.go#L583-L586`](https://github.com/mattermost/mattermost/blob/v11.11.1/server/channels/app/platform/web_conn.go#L583-L586).

O intervalo de *ping* é 60% do tempo de espera por *pong* (60 de 100 segundos), o que deixa margem
para que um *pong* atrasado chegue antes do prazo. O tamanho da fila morta, 128 eventos, é
justificado no comentário como uma aproximação do *buffer* de escrita padrão do núcleo Linux
dividido pelo tamanho médio de uma mensagem. O tratador de *pong* renova o prazo de leitura e
agenda a verificação de ausência ([`web_conn.go#L450-L461`](https://github.com/mattermost/mattermost/blob/v11.11.1/server/channels/app/platform/web_conn.go#L450-L461)).
Na reconexão, a rotina de escrita procura na fila morta o número informado pelo cliente e, se não
o encontra e houve perda, gera nova identidade de conexão e envia uma mensagem de saudação para
forçar a ressincronização ([`web_conn.go#L526-L558`](https://github.com/mattermost/mattermost/blob/v11.11.1/server/channels/app/platform/web_conn.go#L526-L558)).

A Listagem 6.3.8, adiante, mostra a atualização de presença; antes, a Listagem 6.3.7 apresenta o
mecanismo de idempotência.

## 6.3.6 Idempotência na criação de mensagens

**Listagem 6.3.7** – Código real (Go, Mattermost v11.11.1): início da deduplicação pela chave provisória enviada pelo cliente.

```go
// deduplicateCreatePost attempts to make posting idempotent within a caching window.
func (a *App) deduplicateCreatePost(rctx request.CTX, post *model.Post) (foundPost *model.Post, err *model.AppError) {
	// We rely on the client sending the pending post id across "duplicate" requests. If there
	// isn't one, we can't deduplicate, so allow creation normally.
	if post.PendingPostId == "" {
		return nil, nil
	}
```

Fonte: Mattermost (2026a), [`post.go#L125-L131`](https://github.com/mattermost/mattermost/blob/v11.11.1/server/channels/app/post.go#L125-L131).

O restante da função consulta o *cache* `seenPendingPostIdsCache`. Se a chave nunca foi vista, é
registrada com valor vazio e a criação prossegue; se já está associada a uma mensagem, a mensagem
existente é devolvida ao cliente, "fazendo a chamada parecer idempotente", nas palavras do
comentário; e se outra requisição com a mesma chave ainda está em andamento, o cliente recebe um
erro, pois esperar por ela acrescentaria complexidade ao caminho principal
([`post.go#L132-L170`](https://github.com/mattermost/mattermost/blob/v11.11.1/server/channels/app/post.go#L132-L170)).
O tempo de vida das chaves é de 30 segundos (linha 29 do mesmo arquivo).

**Listagem 6.3.8** – Código real (Go, Mattermost v11.11.1): atualização de presença ao remover a última conexão de um usuário.

```go
conns := connIndex.ForUser(webConn.UserId)
// areAllInactive also returns true if there are no connections,
// which is intentional.
if areAllInactive(conns) {
	userID := webConn.UserId
	h.ProcessAsync(func() {
		// If this is an HA setup, get count for this user
		// from other nodes.
		var clusterCnt int
		var appErr *model.AppError
		if h.platform.Cluster() != nil {
			clusterCnt, appErr = h.platform.Cluster().WebConnCountForUser(userID)
		}
		if appErr != nil {
			mlog.Error("Error in trying to get the webconn count from cluster", mlog.Err(appErr))
			// We take a conservative approach
			// and do not set status to offline in case
			// there's an error, rather than potentially
			// incorrectly setting status to offline.
			return
		}
		// Only set to offline if there are no
		// active connections in other nodes as well.
		if clusterCnt == 0 {
			h.platform.QueueSetStatusOffline(userID, false)
		}
```

Fonte: Mattermost (2026a), [`web_hub.go#L618-L643`](https://github.com/mattermost/mattermost/blob/v11.11.1/server/channels/app/platform/web_hub.go#L618-L643).

O trecho mostra a postura conservadora discutida na [Seção 5.3.7](../capitulo-5-10-mil-pes/mattermost.md): o usuário só é marcado como
*offline* se não houver conexões ativas em nenhum nó, e, se a consulta ao *cluster* falha, o status
não é alterado.

## 6.3.7 Persistência em camadas: *Decorator* sobre *Repository*

A interface `store.Store` agrupa interfaces por tipo de objeto, como `Post()`, `Channel()` e
`User()`, cada uma com operações de coleção como `Save`, `Get` e `Delete`
([`store.go#L25-L37`](https://github.com/mattermost/mattermost/blob/v11.11.1/server/channels/store/store.go#L25-L37)).
A Listagem 6.3.9 mostra como o serviço de plataforma monta a implementação efetiva.

**Listagem 6.3.9** – Código real (Go, Mattermost v11.11.1): composição das camadas do *store*.

```go
searchStore := searchlayer.NewSearchLayer(
	retrylayer.New(ps.sqlStore),
	ps.SearchEngine,
	ps.Config(),
)

ps.AddConfigListener(func(prevCfg, cfg *model.Config) {
	searchStore.UpdateConfig(cfg)
})

lcl, err2 := localcachelayer.NewLocalCacheLayer(
	timerlayer.New(searchStore, ps.metricsIFace),
	ps.metricsIFace,
	ps.clusterIFace,
	ps.cacheProvider,
	ps.Log(),
)
```

Fonte: Mattermost (2026a), [`service.go#L300-L316`](https://github.com/mattermost/mattermost/blob/v11.11.1/server/channels/app/platform/service.go#L300-L316).

A implementação SQL é envolvida, de dentro para fora, por uma camada de novas tentativas
(`retrylayer`), uma camada de busca (`searchlayer`), uma camada de medição de tempo (`timerlayer`)
e uma camada de *cache* local (`localcachelayer`). Todas implementam a mesma interface `Store`, e
cada uma acrescenta uma responsabilidade antes de delegar à seguinte: é o padrão ***Decorator***,
cuja intenção é anexar responsabilidades adicionais a um objeto dinamicamente, como alternativa
flexível à herança (GAMMA *et al.*, 1994). As camadas de novas tentativas e de medição são geradas
automaticamente, como indica o cabeçalho "Code generated by \"make store-layers\"" de seus
arquivos. A camada de *cache* recebe a `ClusterInterface` e registra tratadores para eventos de
invalidação vindos de outros nós, como `ClusterEventInvalidateCacheForReactions`
([`localcachelayer/layer.go#L492-L503`](https://github.com/mattermost/mattermost/blob/v11.11.1/server/channels/store/localcachelayer/layer.go#L492-L503)):
é assim que os *caches* em memória, um dos dois estados que permanecem em cada nó ([Seção 4.3.5](../capitulo-4-50-mil-pes/mattermost.md)),
se mantêm coerentes no *cluster*.

## 6.3.8 *Plugins*: *hooks* e supervisor

A interface `Hooks` tem cerca de cinquenta métodos. Os que terminam em *WillBe*, como
`MessageWillBePosted`, são chamados antes da ação e podem alterá-la ou vetá-la, devolvendo uma
mensagem modificada e uma explicação de rejeição; os que terminam em *HasBeen*, como
`MessageHasBeenPosted`, são chamados depois, sem poder de veto
([`hooks.go#L163`](https://github.com/mattermost/mattermost/blob/v11.11.1/server/public/plugin/hooks.go#L163),
[`hooks.go#L184`](https://github.com/mattermost/mattermost/blob/v11.11.1/server/public/plugin/hooks.go#L184)).
Essas interfaces estão em `server/public/plugin`, sob a licença Apache 2.0, enquanto o núcleo está
sob a AGPL v3.0 (MATTERMOST, 2026a, `LICENSE.txt`). Argumenta-se aqui que a separação de licenças
acompanha a separação arquitetural: o contrato entre núcleo e extensões é publicado sob licença
permissiva, e a fronteira de processo reforça essa separação na execução.

A Listagem 6.3.10 mostra como o supervisor de cada *plugin* configura o cliente da biblioteca
`go-plugin` (HASHICORP, 2026): a saída padrão e a de erros do subprocesso vão para o *log* do
servidor, e o *plugin* tem três segundos para iniciar.

**Listagem 6.3.10** – Código real (Go, Mattermost v11.11.1): configuração do subprocesso de um *plugin*.

```go
clientConfig := &plugin.ClientConfig{
	HandshakeConfig: handshake,
	Plugins:         pluginMap,
	SyncStdout:      wrappedLogger.With(mlog.String("source", "plugin_stdout")).StdLogWriter(),
	SyncStderr:      wrappedLogger.With(mlog.String("source", "plugin_stderr")).StdLogWriter(),
	Logger:          hclogAdaptedLogger,
	StartTimeout:    time.Second * 3,
}
for _, opt := range opts {
	err := opt(&sup, clientConfig)
	if err != nil {
		return nil, errors.Wrap(err, "failed to apply option")
	}
}

sup.client = plugin.NewClient(clientConfig)
```

Fonte: Mattermost (2026a), [`supervisor.go#L117-L132`](https://github.com/mattermost/mattermost/blob/v11.11.1/server/public/plugin/supervisor.go#L117-L132).

Em seguida, o supervisor obtém o cliente RPC, solicita a implementação de `Hooks` e consulta quais
*hooks* o *plugin* implementa, para não fazer chamadas inúteis através da fronteira de processo
(linhas 134 a 159 do mesmo arquivo). A verificação de saúde completa o isolamento: a cada 30
segundos, o servidor faz um *ping* RPC em cada *plugin* ativo; três falhas consecutivas
caracterizam uma falha; o *plugin* é reiniciado e, se acumular três falhas dentro de uma hora, é
desativado ([`health_check.go#L14-L19`](https://github.com/mattermost/mattermost/blob/v11.11.1/server/public/plugin/health_check.go#L14-L19)).
Quando ocorre um evento, o ambiente percorre os *plugins* registrados e invoca o *hook* apenas nos
que estão ativos e o implementam ([`environment.go#L633-L658`](https://github.com/mattermost/mattermost/blob/v11.11.1/server/public/plugin/environment.go#L633-L658)).

## 6.3.9 Comandos de barra como *Command*

**Listagem 6.3.11** – Código real (Go, Mattermost v11.11.1): a interface `CommandProvider` e o registro de comandos.

```go
type CommandProvider interface {
	GetTrigger() string
	GetCommand(a *App, T i18n.TranslateFunc) *model.Command
	DoCommand(a *App, rctx request.CTX, args *model.CommandArgs, message string) *model.CommandResponse
}

var commandProviders = make(map[string]CommandProvider)

func RegisterCommandProvider(newProvider CommandProvider) {
	commandProviders[newProvider.GetTrigger()] = newProvider
}
```

Fonte: Mattermost (2026a), [`command.go#L34-L44`](https://github.com/mattermost/mattermost/blob/v11.11.1/server/channels/app/command.go#L34-L44).

Cada comando embutido se registra na inicialização do pacote `slashcommands`. O comando `/shrug`,
por exemplo, registra-se com `app.RegisterCommandProvider(&ShrugProvider{})` e, em `DoCommand`,
acrescenta um emoticon ao texto e devolve uma resposta a ser publicada no canal
([`command_shrug.go#L20-L45`](https://github.com/mattermost/mattermost/blob/v11.11.1/server/channels/app/slashcommands/command_shrug.go#L20-L45)).
A Listagem 6.3.12 mostra a ordem de resolução de um comando.

**Listagem 6.3.12** – Código real (Go, Mattermost v11.11.1): ordem de resolução de um comando de barra.

```go
// Plugins can override built in and custom commands
cmd, response, appErr := a.tryExecutePluginCommand(rctx, args)
if appErr != nil {
	return nil, appErr
} else if cmd != nil && response != nil {
	response.TriggerId = clientTriggerId
	return a.HandleCommandResponse(rctx, cmd, args, response, true)
}

// Custom commands can override built ins
cmd, response, appErr = a.tryExecuteCustomCommand(rctx, args, trigger, message)
if appErr != nil {
	return nil, appErr
} else if cmd != nil && response != nil {
	response.TriggerId = clientTriggerId
	return a.HandleCommandResponse(rctx, cmd, args, response, false)
}

cmd, response = a.tryExecuteBuiltInCommand(rctx, args, trigger, message)
if cmd != nil && response != nil {
```

Fonte: Mattermost (2026a), [`command.go#L243-L262`](https://github.com/mattermost/mattermost/blob/v11.11.1/server/channels/app/command.go#L243-L262).

O servidor tenta primeiro os comandos de *plugins*, depois os personalizados cadastrados pelos
administradores e, por último, os embutidos; cada etapa só é consultada se a anterior não tratou o
comando. A estrutura lembra uma *Chain of Responsibility* com a cadeia fixada no código, e
expressa uma política de extensibilidade: uma extensão pode substituir um comando do produto sem
alterar seu código, em conformidade com o princípio aberto-fechado (ver [Seção 1.5.4](../capitulo-1-fundamentos/index.md)).

## 6.3.10 Catálogo de padrões identificados

A Tabela 6.3.2 registra os padrões encontrados, no formato da seção *Known Uses* dos catálogos
de padrões: nome, onde ocorre e como a ocorrência se afasta da descrição canônica.

**Tabela 6.3.2** – Padrões identificados no Mattermost v11.11.1 (*Known Uses*).

| Padrão (fonte) | Onde | Papéis | Observação |
|---|---|---|---|
| *Observer* (GAMMA *et al.*, 1994) | `web_hub.go`, `environment.go` | Sujeito: *hub* ou ambiente de *plugins*; observadores: `WebConn` ou *plugins* | Notificação por canal Go, com filtro do assinante (`ShouldSendEvent`) e descarte do observador lento |
| *Mediator* (GAMMA *et al.*, 1994) | `web_hub.go`, `cluster.go` | Mediador: *hub* acessado por `Publish`; colegas: produtores e conexões | Produtores nunca referenciam conexões; Shvets (2022, p. 343) nota que *Mediator* implementado com *Observer* é frequente |
| *Command* (GAMMA *et al.*, 1994) | `command.go`, `slashcommands/` | *Command*: `CommandProvider`; *Invoker*: `App.ExecuteCommand` | Sem desfazer; registro por gatilho em mapa global |
| *Chain of Responsibility* (GAMMA *et al.*, 1994) | `command.go` | Elos: *plugin*, personalizado, embutido | Cadeia fixa no código |
| *Decorator* (GAMMA *et al.*, 1994) | `service.go`, `store/*layer` | Componente: `Store`; decoradores: novas tentativas, busca, tempo, *cache* | Decoradores gerados por código |
| *Proxy* (GAMMA *et al.*, 1994) | `supervisor.go`, `client_rpc*.go` | Representante local de `Hooks` que encaminha chamadas por RPC | Proxy remoto gerado por `make pluginapi` |
| *Service Layer* (FOWLER, 2002) | `server/channels/app` | Fachada de operações da aplicação | Camada larga, usada por REST, WebSocket, comandos e *plugins* |
| *Repository* (FOWLER, 2002) | `server/channels/store` | Interfaces de coleção por tipo | Implementação SQL com camadas decoradoras |
| *Idempotent Receiver* (HOHPE; WOOLF, 2003) | `app/post.go` | Receptor: `deduplicateCreatePost` | Janela de 30 s em *cache* |

Fonte: elaborado pelo autor (2026), com base em Mattermost (2026a).

Sobre a distinção entre os dois primeiros padrões, vale a observação de Gamma *et al.* (1994): o
*Observer* traz o benefício do acoplamento abstrato entre sujeito e observadores e o risco das
**atualizações inesperadas**, pois os observadores não sabem uns dos outros e uma operação
inofensiva no sujeito pode desencadear uma cascata de custo imprevisível. No *hub*, esse risco é
contido justamente pelo descarte do consumidor lento.

## 6.3.11 Subida de verificação

A subida de verificação parte do código e pergunta, a cada altitude, que componente esse código
realiza, que decisão ele concretiza e que característica a decisão protege (ver [Seção 1.11](../capitulo-1-fundamentos/index.md)). A
pergunta escolhida é: **que característica justifica isolar *plugins* em outro processo?** A
Tabela 6.3.3 percorre a cadeia a partir da Listagem 6.3.10.

**Tabela 6.3.3** – Subida de verificação a partir do supervisor de *plugins*.

| Altitude | Pergunta | Resposta |
|---|---|---|
| <span class="altitude solo">Solo</span> | O que o código faz? | `plugin.NewClient` inicia o *plugin* como subprocesso e obtém um cliente RPC; a verificação de saúde faz *ping* a cada 30 s e reinicia ou desativa o *plugin* (Listagem 6.3.10 e Seção 6.3.8) |
| <span class="altitude a10">10.000 pés</span> | Que componente isso realiza? | O ambiente de *plugins*, porta de entrada e saída do núcleo, com contratos `Hooks` e `API` ([Seção 5.3.1](../capitulo-5-10-mil-pes/mattermost.md)) |
| <span class="altitude a50">50.000 pés</span> | Que decisão isso concretiza? | ADR-MM-01: *plugins* em processos separados, na mesma máquina, por RPC ([Seção 4.3.8](../capitulo-4-50-mil-pes/mattermost.md)) |
| <span class="altitude a100">100.000 pés</span> | Que característica a decisão protege? | **Disponibilidade**, por isolamento de falhas: um *panic* no *plugin* não derruba o servidor nem as conexões; **segurança**, por confinamento: o *plugin* só acessa as interfaces e os argumentos que recebe ([Seção 3.3.7](../capitulo-3-100-mil-pes/mattermost.md)) |

Fonte: elaborado pelo autor (2026).

A cadeia se completa, o que, segundo o método, indica que a decisão é intencional e
fundamentada. Ela também expõe o custo aceito: **desempenho**, pois cada *hook* atravessa uma
fronteira de processo, e **simplicidade** para os autores de *plugins*, que, em *cluster*, precisam
guardar estado no armazenamento chave-valor oferecido pela API, e não em memória, porque cada nó
executa sua própria cópia do *plugin* (MATTERMOST, 2026h).

Uma segunda subida, mais curta, parte da Listagem 6.3.5. O código escolhe o modo confiável para
cinco tipos de evento (solo); isso realiza a difusão entre nós do *cluster* (10.000 pés);
concretiza a decisão de difundir eventos a todos os nós em vez de roteá-los, com garantias
diferenciadas por tipo (50.000 pés); e protege a **latência de entrega** dos eventos que importam,
sem pagar o custo da confiabilidade nos eventos efêmeros (100.000 pés).

## 6.3.12 Atividade com correção automática

A atividade do catálogo de exercícios para este sistema (`atividades/README.md`, linha do
Mattermost) pede um *hub* de eventos com *Observer* em que os publicadores não conheçam os
assinantes concretos. A Listagem 6.3.13 é o ponto de partida: recria, em Java 17, a essência do
*hub* estudado. O código é ilustrativo e simplificado: omite concorrência, filas de envio e o
*cluster*.

**Listagem 6.3.13** – Modelo didático em Java: *Hub* de eventos com *Observer*.

```java
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

// Evento imutável: o "alcance" indica a quem ele interessa (como WebsocketBroadcast).
public record Evento(String tipo, String canalId, String usuarioId, String dados) { }

// O que os produtores enxergam: apenas publicar (papel de Mediator).
public interface Publicador {
    void publicar(Evento evento);
}

// O que as conexões implementam: ser notificadas (papel de Observer).
public interface Assinante {
    String usuarioId();
    boolean deveReceber(Evento evento);          // análogo a ShouldSendEvent
    void notificar(Evento evento);
}

public final class HubDeEventos implements Publicador {
    private final List<Assinante> assinantes = new CopyOnWriteArrayList<>();
    private final Map<String, Set<String>> membrosPorCanal = new ConcurrentHashMap<>();

    public void registrar(Assinante a)   { assinantes.add(a); }
    public void remover(Assinante a)     { assinantes.remove(a); }
    public void entrarNoCanal(String canalId, String usuarioId) {
        membrosPorCanal.computeIfAbsent(canalId, c -> ConcurrentHashMap.newKeySet()).add(usuarioId);
    }

    @Override
    public void publicar(Evento e) {
        Set<String> membros = membrosPorCanal.getOrDefault(e.canalId(), Set.of());
        for (Assinante a : assinantes) {
            boolean doCanal = e.canalId() != null && membros.contains(a.usuarioId());
            boolean doUsuario = a.usuarioId().equals(e.usuarioId());
            if ((doCanal || doUsuario) && a.deveReceber(e)) {
                a.notificar(e);                  // o hub não sabe o que o assinante fará
            }
        }
    }
}

// Um produtor: depende só da abstração Publicador.
public final class ServicoDeMensagens {
    private final Publicador publicador;
    public ServicoDeMensagens(Publicador publicador) { this.publicador = publicador; }

    public void publicarMensagem(String canalId, String autor, String texto) {
        // 1. gravar no repositório (omitido); 2. só então anunciar
        publicador.publicar(new Evento("posted", canalId, null, autor + ": " + texto));
    }
}
```

Fonte: elaborado pelo autor (2026), inspirado em Mattermost (2026a, `web_hub.go`).

A correção verifica o comportamento com JUnit e a arquitetura com ArchUnit. A Listagem 6.3.14
mostra as funções de aptidão que garantem que os produtores não conheçam os assinantes concretos
nem o *hub* concreto.

**Listagem 6.3.14** – Modelo didático em Java (ArchUnit): funções de aptidão do exercício.

```java
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "br.edu.ifal.arqsw.hub")
class HubArquiteturaTest {

    // Quem publica eventos não pode depender de nenhuma implementação de Assinante.
    @ArchTest
    static final ArchRule produtoresNaoConhecemAssinantes =
        noClasses().that().resideInAPackage("..produtores..")
            .should().dependOnClassesThat().implement(Assinante.class);

    // Produtores só podem enxergar o hub pela interface Publicador.
    @ArchTest
    static final ArchRule produtoresNaoConhecemOHubConcreto =
        noClasses().that().resideInAPackage("..produtores..")
            .should().dependOnClassesThat().areAssignableTo(HubDeEventos.class);
}
```

Fonte: elaborado pelo autor (2026).

## Exercícios

1. Localize, na *tag* v11.11.1, o código que difunde eventos às conexões WebSocket e o que os envia
   aos demais nós. Faça a subida de verificação de um desses trechos.
2. Explique o funcionamento da fila morta e da numeração de eventos por conexão (Listagem 6.3.6). O
   que acontece quando o número informado pelo cliente não está mais na fila?
3. Mostre, com um diagrama de sequência, como o `pending_post_id` (Listagem 6.3.7) evita mensagens
   duplicadas após um tempo limite na rede móvel. Que falha ocorre se duas requisições com a mesma
   chave chegarem a nós diferentes do *cluster*? Discuta com base no código.
4. A estrutura `Post` (Listagem 6.3.1) é usada pela API, pela aplicação, pela persistência e pelos
   *plugins*. Proponha uma refatoração que separe o modelo de domínio dos formatos de transporte e
   avalie seu custo.
5. Desenhe o diagrama de classes das camadas do *store* (Listagem 6.3.9) e explique por que a
   ordem dos decoradores importa. O que mudaria se o *cache* ficasse abaixo da camada de novas
   tentativas?
6. Escreva um novo `CommandProvider` em Go, ou o equivalente em Java, para um comando `/plantao`
   que responda com o nome do plantonista do canal. Identifique os papéis do padrão *Command*.
7. **Atividade com correção automática:** implemente o *hub* de eventos com *Observer* a partir da
   Listagem 6.3.13, de modo que passe nos testes de comportamento e nas funções de aptidão da
   Listagem 6.3.14 (ver `atividades/README.md`).

## Termos

`Hub de WebSocket`
:   No Mattermost, componente que registra as conexões WebSocket de um subconjunto de usuários e
    lhes entrega os eventos publicados, filtrando-os por usuário, canal ou conexão.

`Fila morta (dead queue)`
:   No Mattermost, *buffer* circular com os últimos eventos enviados a uma conexão, usado para
    reenviá-los quando o cliente se reconecta.

`Idempotência`
:   Propriedade de uma operação cujo efeito é o mesmo quer seja executada uma ou várias vezes.

`Hook`
:   Ponto de extensão no qual o sistema invoca código de uma extensão quando ocorre um evento;
    no Mattermost, *hooks* "WillBe" podem vetar a ação, e *hooks* "HasBeen" apenas reagem a ela.

`Contrapressão (backpressure)`
:   Mecanismo pelo qual um componente sobrecarregado limita o ritmo dos produtores ou descarta
    consumidores lentos para proteger o restante do sistema.

## Referências

EVANS, E. **Domain-driven design**: tackling complexity in the heart of software. Boston:
Addison-Wesley, 2003.

FOWLER, M. **Patterns of enterprise application architecture**. Boston: Addison-Wesley, 2002.

GAMMA, E. *et al.* **Design patterns**: elements of reusable object-oriented software. Reading:
Addison-Wesley, 1994.

HASHICORP. **hashicorp/go-plugin**: Golang plugin system over RPC. GitHub, 2026. Disponível em:
<https://github.com/hashicorp/go-plugin>. Acesso em: 18 out. 2026.

HOHPE, G.; WOOLF, B. **Enterprise integration patterns**: designing, building, and deploying
messaging solutions. Boston: Addison-Wesley, 2003.

MATTERMOST. **mattermost/mattermost**: repositório de código-fonte, versão v11.11.1. GitHub,
2026a. Disponível em: <https://github.com/mattermost/mattermost/tree/v11.11.1>. Acesso em:
18 out. 2026.

MATTERMOST. **High availability** (plugins). Mattermost Developers, 2026h. Disponível em:
<https://developers.mattermost.com/integrate/plugins/components/server/ha/>. Acesso em:
18 out. 2026.

SHVETS, A. **Mergulho nos padrões de projeto**. [*S. l.*]: Refactoring.Guru, 2022. Disponível
em: <https://refactoring.guru/pt-br/design-patterns>. Acesso em: 18 out. 2026.
