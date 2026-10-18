# 6.2 Mastodon: o código

<span class="altitude solo">Solo</span>

Esta seção pousa no código do Mastodon, na *tag* `v4.7.3` (MASTODON, 2026). O roteiro acompanha a
Figura 5.2.1 da [Seção 5.2](../capitulo-5-10-mil-pes/mastodon.md): do *controller* ao *service
object*, do *job* ao serviço de *fanout*, o gerenciador de *feeds*, a leitura da *timeline*, o
*streaming*, a recepção e a entrega federadas e, por fim, os modelos *Active Record* com seus
*callbacks*. Os trechos são **literais e curtos**, com *permalinks* para as linhas citadas; o leitor
pode conferi-los fazendo `git checkout v4.7.3` num clone do repositório. Orientações gerais para
explorar uma base de código desconhecida estão no Apêndice C. A seção termina com a identificação
dos padrões, a subida de verificação e um modelo didático em Java 17.

## 6.2.1 Do *controller* ao *service object*

O ponto de entrada de uma publicação pela API é o método `create` de
[`Api::V1::StatusesController`](https://github.com/mastodon/mastodon/blob/v4.7.3/app/controllers/api/v1/statuses_controller.rb#L30-L53).
O *controller* não contém regra de negócio: monta um *hash* de opções a partir dos parâmetros,
chama `PostStatusService.new.call(...)` e serializa o resultado. Até a chave de idempotência,
enviada pelo cliente no cabeçalho `Idempotency-Key`, é apenas repassada ao serviço.

O [`PostStatusService`](https://github.com/mastodon/mastodon/blob/v4.7.3/app/services/post_status_service.rb)
pré-processa atributos (visibilidade, conteúdo sensível, agendamento), valida mídias, processa
menções e citações, aplica verificações antispam e grava a publicação dentro de
`ApplicationRecord.transaction`. Em seguida, executa o pós-processamento da Listagem 6.2.1.

**Listagem 6.2.1** – Pós-processamento de uma publicação: tudo o que é caro vai para a fila.

```ruby
  def postprocess_status!
    process_hashtags_service.call(@status)
    Trends.tags.register(@status)
    LinkCrawlWorker.perform_async(@status.id)
    DistributionWorker.perform_async(@status.id)
    process_email_subscriptions!
    ActivityPub::DistributionWorker.perform_async(@status.id)
    PollExpirationNotifyWorker.perform_at(@status.poll.expires_at, @status.poll.id) if @status.poll
    ActivityPub::QuoteRequestWorker.perform_async(@status.quote.id) if @status.quote&.quoted_status.present? && !@status.quote&.quoted_status&.local?
  end
```

Fonte: Mastodon,
[`app/services/post_status_service.rb#L172-L181`](https://github.com/mastodon/mastodon/blob/v4.7.3/app/services/post_status_service.rb#L172-L181),
*tag* v4.7.3.

A listagem é a materialização do ADR-M4 da Seção 5.2.8: a requisição HTTP termina assim que a
publicação está gravada e os *jobs* estão enfileirados. O mesmo serviço implementa idempotência com
uma trava no Redis: se o cliente reenviar a requisição com a mesma chave, por exemplo após um
*timeout* de rede, recebe a publicação já criada em vez de criar outra
([`post_status_service.rb#L246-L256`](https://github.com/mastodon/mastodon/blob/v4.7.3/app/services/post_status_service.rb#L246-L256)).
É o princípio de deduplicação que Xu (2020, cap. 10) recomenda para notificações, aplicado na borda
de entrada.

## 6.2.2 O *job* de distribuição e o serviço de *fanout*

O `DistributionWorker`, na Listagem 6.2.2, é um adaptador fino entre a fila e o serviço de *fanout*.

**Listagem 6.2.2** – O *job* que dispara o *fanout* local.

```ruby
class DistributionWorker
  include Sidekiq::Worker
  include Redisable
  include Lockable

  def perform(status_id, options = {})
    with_redis_lock("distribute:#{status_id}") do
      FanOutOnWriteService.new.call(Status.find(status_id), **options.symbolize_keys)
    end
  rescue ActiveRecord::RecordNotFound
    true
  end
end
```

Fonte: Mastodon,
[`app/workers/distribution_worker.rb#L3-L15`](https://github.com/mastodon/mastodon/blob/v4.7.3/app/workers/distribution_worker.rb#L3-L15),
*tag* v4.7.3.

Dois detalhes revelam preocupações de sistemas distribuídos dentro do monólito. A trava
`distribute:<id>` impede que duas execuções do mesmo *job*, por exemplo após uma retentativa,
distribuam a mesma publicação em paralelo. E o tratamento de `RecordNotFound` encerra o *job* sem
erro quando a publicação foi apagada entre o enfileiramento e a execução, situação normal num
sistema assíncrono.

O [`FanOutOnWriteService`](https://github.com/mastodon/mastodon/blob/v4.7.3/app/services/fan_out_on_write_service.rb)
decide **para onde** a publicação vai. Seu método `call` (Listagem 6.2.3) separa três destinos:
destinatários locais, seguidores de *hashtags* e *streams* públicos.

**Listagem 6.2.3** – O método `call` do serviço de *fanout*.

```ruby
  def call(status, options = {})
    @status    = status
    @account   = status.account
    @options   = options

    return if @status.proper.account.suspended?

    warm_payload_cache!

    fan_out_to_local_recipients!
    fan_out_to_public_recipients! if broadcastable?
    fan_out_to_public_streams! if broadcastable?
  end
```

Fonte: Mastodon,
[`app/services/fan_out_on_write_service.rb#L12-L24`](https://github.com/mastodon/mastodon/blob/v4.7.3/app/services/fan_out_on_write_service.rb#L12-L24),
*tag* v4.7.3.

O método `fan_out_to_local_recipients!` escolhe o caminho conforme a visibilidade: publicações
públicas, não listadas e privadas vão para todos os seguidores e listas; limitadas e diretas, apenas
para os mencionados. A Listagem 6.2.4 mostra o coração do *fanout on write*.

**Listagem 6.2.4** – Entrega a todos os seguidores locais, em lotes.

```ruby
  def deliver_to_all_followers!
    @account.followers_for_local_distribution.select(:id).reorder(nil).find_in_batches do |followers|
      FeedInsertWorker.push_bulk(followers) do |follower|
        [@status.id, follower.id, 'home', { 'update' => update? }]
      end
    end
  end
```

Fonte: Mastodon,
[`app/services/fan_out_on_write_service.rb#L101-L107`](https://github.com/mastodon/mastodon/blob/v4.7.3/app/services/fan_out_on_write_service.rb#L101-L107),
*tag* v4.7.3.

O serviço não insere nas *timelines*: enfileira um `FeedInsertWorker` por seguidor, em lotes
(`find_in_batches` e `push_bulk`). O escopo `followers_for_local_distribution`, definido em
[`app/models/concerns/account/interactions.rb#L216-L220`](https://github.com/mastodon/mastodon/blob/v4.7.3/app/models/concerns/account/interactions.rb#L216-L220)
como `followers.local.joins(:user).merge(User.signed_in_recently)`, restringe o *fanout* a seguidores
locais ativos. Não há limiar de celebridade: uma conta com 200 mil seguidores locais ativos gera 200
mil *jobs*. O [`FeedInsertWorker`](https://github.com/mastodon/mastodon/blob/v4.7.3/app/workers/feed_insert_worker.rb)
lê a publicação e o seguidor do banco primário, aplica os filtros do destinatário com a réplica de
leitura e chama `FeedManager.instance.push_to_home`.

## 6.2.3 O gerenciador de *feeds*

O [`FeedManager`](https://github.com/mastodon/mastodon/blob/v4.7.3/app/lib/feed_manager.rb)
encapsula todo o acesso às *timelines* no Redis. Suas primeiras linhas (Listagem 6.2.5) definem os
limites estimados na Seção 3.2.3.

**Listagem 6.2.5** – Limites das *timelines* em memória.

```ruby
class FeedManager
  include Singleton
  include Redisable

  # Maximum number of items stored in a single feed
  MAX_ITEMS = 800

  # Number of items in the feed since last reblog of status
  # before the new reblog will be inserted. Must be <= MAX_ITEMS
  # or the tracking sets will grow forever
  REBLOG_FALLOFF = 80
```

Fonte: Mastodon,
[`app/lib/feed_manager.rb#L5-L15`](https://github.com/mastodon/mastodon/blob/v4.7.3/app/lib/feed_manager.rb#L5-L15),
*tag* v4.7.3.

A Listagem 6.2.6 mostra a inserção na *timeline* inicial: verifica de novo se o seguidor está
ativo, insere, apara a *timeline* e, só se houver um cliente conectado ao *streaming* para aquela
*timeline*, enfileira um *job* que publica a atualização.

**Listagem 6.2.6** – Inserção na *timeline* inicial.

```ruby
  def push_to_home(account, status, update: false)
    return false unless account.user&.signed_in_recently?
    return false unless add_to_feed(:home, account.id, status, aggregate_reblogs: account.user&.aggregates_reblogs?)

    trim(:home, account.id)
    PushUpdateWorker.perform_async(account.id, status.id, "timeline:#{account.id}", { 'update' => update }) if push_update_required?("timeline:#{account.id}")
    true
  end
```

Fonte: Mastodon,
[`app/lib/feed_manager.rb#L76-L83`](https://github.com/mastodon/mastodon/blob/v4.7.3/app/lib/feed_manager.rb#L76-L83),
*tag* v4.7.3.

O método `add_to_feed`
([`feed_manager.rb#L537-L574`](https://github.com/mastodon/mastodon/blob/v4.7.3/app/lib/feed_manager.rb#L537-L574))
executa `redis.zadd(timeline_key, status.id, status.id)`, com o ID como membro e como pontuação, e
trata compartilhamentos (*reblogs*): se o original ou outro compartilhamento dele já está entre os
80 itens mais recentes, o novo compartilhamento não é reinserido. Um comentário no mesmo método
registra o efeito de consistência eventual discutido na caixa-preta da Seção 5.2: um
compartilhamento pode chegar antes do original, por atraso do *worker* que entrega o original, e
nesse caso o original não é reinserido. O método `trim`
([`feed_manager.rb#L399-L423`](https://github.com/mastodon/mastodon/blob/v4.7.3/app/lib/feed_manager.rb#L399-L423))
remove o que passa de `MAX_ITEMS` com `ZREMRANGEBYRANK`: a *timeline* em memória é uma **janela
deslizante** das 800 publicações mais recentes.

## 6.2.4 A leitura da *timeline* e o híbrido por leitor

O [`Api::V1::Timelines::HomeController`](https://github.com/mastodon/mastodon/blob/v4.7.3/app/controllers/api/v1/timelines/home_controller.rb#L11-L23)
obtém as publicações de um `HomeFeed`, subclasse de `Feed`, cujo método de leitura está na Listagem
6.2.7.

**Listagem 6.2.7** – Leitura da *timeline*: uma faixa do conjunto ordenado e uma busca por IDs.

```ruby
  def from_redis(limit, max_id, since_id, min_id)
    max_id = '+inf' if max_id.blank?
    if min_id.blank?
      since_id   = '-inf' if since_id.blank?
      unhydrated = redis.zrevrangebyscore(key, "(#{max_id}", "(#{since_id}", limit: [0, limit], with_scores: true).map { |id| id.first.to_i }
    else
      unhydrated = redis.zrangebyscore(key, "(#{min_id}", "(#{max_id}", limit: [0, limit], with_scores: true).map { |id| id.first.to_i }
    end

    Status.where(id: unhydrated)
  end
```

Fonte: Mastodon,
[`app/models/feed.rb#L22-L32`](https://github.com/mastodon/mastodon/blob/v4.7.3/app/models/feed.rb#L22-L32),
*tag* v4.7.3.

O nome da variável `unhydrated` (não hidratado) coincide com o vocabulário de Xu (2020, cap. 11): o
Redis devolve apenas IDs, e a resposta completa é montada a seguir, numa busca por chave primária.
O limiar que restringe o *fanout* aos usuários ativos está no módulo `User::Activity`, cujo
comentário explicita o compromisso (Listagem 6.2.8).

**Listagem 6.2.8** – O limiar de atividade que restringe o *fanout*.

```ruby
  # The home and list feeds will be stored for this amount of time, and status
  # fan-out to followers will include only people active within this time frame.
  #
  # Lowering the duration may improve performance if many people sign up, but
  # most will not check their feed every day. Raising the duration reduces the
  # amount of background processing that happens when people become active.
  ACTIVE_DURATION = ENV.fetch('USER_ACTIVE_DAYS', 7).to_i.days
```

Fonte: Mastodon,
[`app/models/concerns/user/activity.rb#L6-L12`](https://github.com/mastodon/mastodon/blob/v4.7.3/app/models/concerns/user/activity.rb#L6-L12),
*tag* v4.7.3.

Quando um usuário inativo volta, o modelo `User` chama `regenerate_feed!`, que marca a *timeline*
como em regeneração e enfileira um `RegenerationWorker`
([`app/models/user.rb#L489-L510`](https://github.com/mastodon/mastodon/blob/v4.7.3/app/models/user.rb#L489-L510)).
Esse *job* executa o `PrecomputeFeedService`, que chama `FeedManager#populate_home`
([`feed_manager.rb#L280-L320`](https://github.com/mastodon/mastodon/blob/v4.7.3/app/lib/feed_manager.rb#L280-L320)):
o método percorre as contas seguidas, busca no PostgreSQL as publicações recentes de cada uma, aplica
os filtros e insere o resultado no Redis. Enquanto isso, o controlador responde com `206 Partial
Content`. É o **híbrido por leitor** descrito na Seção 5.2.3.

## 6.2.5 O servidor de *streaming*

O servidor de *streaming*, em
[`streaming/index.js`](https://github.com/mastodon/mastodon/blob/v4.7.3/streaming/index.js), mantém
conexões WebSocket e *Server-Sent Events* e assina canais do Redis como `timeline:<id da conta>`. Do
lado Rails, o `PushUpdateWorker` publica nesse canal a publicação já renderizada para o destinatário
([`app/workers/push_update_worker.rb#L32-L34`](https://github.com/mastodon/mastodon/blob/v4.7.3/app/workers/push_update_worker.rb#L32-L34)).
A Listagem 6.2.9 mostra como o *streaming* informa ao Rails que há alguém ouvindo: grava
periodicamente uma chave `subscribed:<canal>` com expiração, que o método `push_update_required?` da
Listagem 6.2.6 consulta.

**Listagem 6.2.9** – Batimento de assinatura no servidor de *streaming*.

```javascript
  const subscriptionHeartbeat = channels => {
    const interval = 6 * 60;

    const tellSubscribed = () => {
      channels.forEach(channel => redisClient.set(redisNamespaced(`subscribed:${channel}`), '1', 'EX', interval * 3));
    };

    tellSubscribed();

    const heartbeat = setInterval(tellSubscribed, interval * 1000);
```

Fonte: Mastodon,
[`streaming/index.js#L274-L283`](https://github.com/mastodon/mastodon/blob/v4.7.3/streaming/index.js#L274-L283),
*tag* v4.7.3.

Se ninguém está conectado à *timeline* de um seguidor, a publicação é inserida no Redis, mas nenhum
evento de tempo real é gerado. É uma otimização simples, e um exemplo de acoplamento por convenção
entre dois processos em linguagens diferentes: o nome da chave `subscribed:timeline:<id>` é o
contrato.

## 6.2.6 A fronteira federada

### Recepção

A recepção passa por `ActivityPub::InboxesController`, que enfileira o processamento e responde
`202`, e por `ActivityPub::ProcessActivityService`, que escolhe a classe que trata a atividade por
meio da fábrica da Listagem 6.2.10.

**Listagem 6.2.10** – Fábrica de atividades ActivityPub (trecho; o `case` prossegue até a linha 64).

```ruby
  class << self
    def factory(json, account, **)
      klass_for(json)&.new(json, account, **)
    end

    private

    def klass_for(json)
      case json['type']
      when 'Create'
        ActivityPub::Activity::Create
      when 'Announce'
        ActivityPub::Activity::Announce
```

Fonte: Mastodon,
[`app/lib/activitypub/activity.rb#L23-L35`](https://github.com/mastodon/mastodon/blob/v4.7.3/app/lib/activitypub/activity.rb#L23-L35),
*tag* v4.7.3.

Cada subclasse (`Create`, `Follow`, `Delete`, `Like`, `Undo` e outras, 16 arquivos em
`app/lib/activitypub/activity/`) implementa `perform`. A classe `Create`, após criar a publicação
remota, enfileira o mesmo `DistributionWorker` da Listagem 6.2.2, mas só para publicações dentro da
janela de tempo real ou recebidas por entrega direta
([`app/lib/activitypub/activity/create.rb#L87-L93`](https://github.com/mastodon/mastodon/blob/v4.7.3/app/lib/activitypub/activity/create.rb#L87-L93)).
É a convergência dos dois caminhos descrita na Seção 5.2.5. O modelo `Status` exige URI única para
publicações remotas (`validates :uri, uniqueness: true, presence: true, unless: :local?`, em
[`status.rb#L120`](https://github.com/mastodon/mastodon/blob/v4.7.3/app/models/status.rb#L120)),
o que serve de deduplicação no destino.

### Entrega

O `ActivityPub::RawDistributionWorker` define o esqueleto da distribuição (Listagem 6.2.11), e sua
subclasse `ActivityPub::DistributionWorker` redefine as etapas `inboxes`, `payload` e `options`
([`distribution_worker.rb#L22-L40`](https://github.com/mastodon/mastodon/blob/v4.7.3/app/workers/activitypub/distribution_worker.rb#L22-L40)).

**Listagem 6.2.11** – Esqueleto da distribuição federada.

```ruby
  def distribute!
    return if inboxes.empty?

    ActivityPub::DeliveryWorker.push_bulk(inboxes, limit: 1_000) do |inbox_url|
      [payload, source_account_id, inbox_url, options]
    end
  end
```

Fonte: Mastodon,
[`app/workers/activitypub/raw_distribution_worker.rb#L25-L31`](https://github.com/mastodon/mastodon/blob/v4.7.3/app/workers/activitypub/raw_distribution_worker.rb#L25-L31),
*tag* v4.7.3.

A lista de caixas de entrada vem de
[`app/models/account.rb#L490-L493`](https://github.com/mastodon/mastodon/blob/v4.7.3/app/models/account.rb#L490-L493):
para cada conta remota, usa-se a *sharedInbox* quando existe, URLs repetidas são agrupadas e domínios
marcados como indisponíveis são excluídos. A Listagem 6.2.12 mostra a política de retentativa do
`DeliveryWorker`.

**Listagem 6.2.12** – Política de retentativa da entrega federada.

```ruby
  STOPLIGHT_COOL_OFF_TIME = 60
  STOPLIGHT_FAILURE_THRESHOLD = 10

  sidekiq_options queue: 'push', retry: 16, dead: false

  # Unfortunately, we cannot control Sidekiq's jitter, so add our own
  sidekiq_retry_in do |count|
    # This is Sidekiq's default delay
    delay  = (count**4) + 15
    # Our custom jitter, that will be added to Sidekiq's built-in one.
    # Sidekiq's built-in jitter is `rand(10) * (count + 1)`
    jitter = rand(0.5 * (count**4))
    delay + jitter
  end
```

Fonte: Mastodon,
[`app/workers/activitypub/delivery_worker.rb#L8-L21`](https://github.com/mastodon/mastodon/blob/v4.7.3/app/workers/activitypub/delivery_worker.rb#L8-L21),
*tag* v4.7.3.

A espera antes da *n*-ésima retentativa é *n*⁴ + 15 segundos, mais uma variação aleatória
(*jitter*). Somando as esperas das 16 retentativas sem *jitter* (*n* de 0 a 15), obtêm-se cerca de
178.500 segundos, aproximadamente dois dias (cálculo do autor), o valor usado no cenário C2 da Seção
3.2.7. A variação aleatória evita que milhares de entregas que falharam juntas voltem no mesmo
instante e derrubem o destino assim que ele se recupera, o fenômeno de "manada" que o Twitter
relatou entre seus serviços (KRIKORIAN, 2013). A opção `dead: false` descarta o *job* esgotado em vez
de mandá-lo à fila de *jobs* mortos.

O método `perform_request`
([`delivery_worker.rb#L61-L75`](https://github.com/mastodon/mastodon/blob/v4.7.3/app/workers/activitypub/delivery_worker.rb#L61-L75))
envolve a requisição num disjuntor da biblioteca Stoplight (abre após 10 falhas para a mesma caixa e
espera 60 segundos) e classifica as respostas: 2xx é sucesso; códigos 4xx, exceto 401, 408 e 429, e o
código 501 são irrecuperáveis e encerram a entrega
([`app/helpers/json_ld_helper.rb#L373-L379`](https://github.com/mastodon/mastodon/blob/v4.7.3/app/helpers/json_ld_helper.rb#L373-L379));
os demais provocam retentativa. Cada falha registra o dia corrente num conjunto do Redis; com falhas
em 7 dias distintos, o domínio é marcado como indisponível, e um único sucesso limpa o histórico
([`app/lib/delivery_failure_tracker.rb#L6-L24`](https://github.com/mastodon/mastodon/blob/v4.7.3/app/lib/delivery_failure_tracker.rb#L6-L24)).

## 6.2.7 Modelo de domínio, *Active Record* e *callbacks*

O modelo de domínio do Mastodon gira em torno de poucas classes: `Account` (local ou remota,
distinguidas pelo domínio), `User` (credenciais e preferências de uma conta local), `Status`
(publicação, inclusive compartilhamentos e respostas), `Follow`, `Mention`, `MediaAttachment`,
`Poll`, `Notification` e `DomainBlock`. A classe `Status` declara associações como `belongs_to
:account`, `has_many :mentions`, `has_many :media_attachments` e `has_many :reblogs`
([`status.rb#L77-L108`](https://github.com/mastodon/mastodon/blob/v4.7.3/app/models/status.rb#L77-L108)),
validações e *callbacks*. A Listagem 6.2.13 mostra os *callbacks*.

**Listagem 6.2.13** – *Callbacks* de ciclo de vida do modelo `Status`.

```ruby
  after_create_commit :trigger_create_webhooks
  after_update_commit :trigger_update_webhooks

  after_create_commit  :increment_counter_caches
  after_destroy_commit :decrement_counter_caches

  after_create_commit :store_uri, if: :local?
  after_create_commit :update_statistics, if: :local?

  before_validation :prepare_contents, if: :local?
  before_validation :set_reblog
  before_validation :set_conversation
  before_validation :set_local

  around_create Mastodon::Snowflake::Callbacks

  after_create :set_poll_id
  after_create :update_conversation
```

Fonte: Mastodon,
[`app/models/status.rb#L157-L174`](https://github.com/mastodon/mastodon/blob/v4.7.3/app/models/status.rb#L157-L174),
*tag* v4.7.3.

O `around_create Mastodon::Snowflake::Callbacks` gera o ID ordenável no tempo usado como pontuação nas
*timelines*; `increment_counter_caches` atualiza contadores da conta, do original compartilhado e da
conversa; `trigger_create_webhooks` enfileira notificações para integrações externas. Fox e
Patterson (2021, cap. 5) listam como armadilha o excesso de *callbacks* de ciclo de vida, ou a lógica
complexa neles, por tornar difícil seguir o fluxo da aplicação. Observa-se que o Mastodon usa
*callbacks* para efeitos locais e baratos, mas **não** para o *fanout* nem para a federação, que são
chamados explicitamente pelo `PostStatusService` (Listagem 6.2.1). Argumenta-se aqui que é uma
divisão deliberada: os *callbacks* cuidam de invariantes do próprio registro; o *service object*
cuida dos efeitos de negócio, que dependem do contexto da operação (publicação nova, edição,
agendamento ou atividade recebida).

Pela lente do DDD tático (Seção 1.9.3), `Account` e `Status` são **entidades**, com identidade
própria e, no caso das remotas, também global (a URI). `Status` comporta-se como raiz de um agregado
informal, que reúne menções, mídias, enquete e citação, mas o *Active Record* não impede que outras
partes do código alterem diretamente as partes internas, de modo que as invariantes do agregado são
mantidas por convenção, não por encapsulamento. Os *services* misturam o papel de serviço de
aplicação (orquestração, transação, enfileiramento) e de serviço de domínio (regras que envolvem
várias entidades, como a visibilidade de uma citação). Não há repositórios explícitos: os métodos de
classe e escopos do *Active Record* cumprem esse papel.

## 6.2.8 Padrões identificados

A Tabela 6.2.1 registra os padrões encontrados no formato da seção *Known Uses* dos catálogos de
padrões: o padrão, sua intenção segundo a fonte, onde aparece e como o código o realiza.

**Tabela 6.2.1** – Ficha *Known Uses* dos padrões identificados no Mastodon.

| Padrão | Intenção (fonte) | Onde | Como aparece | Consequência observada |
|---|---|---|---|---|
| *Active Record* | objeto que envolve uma linha de tabela e acrescenta lógica de domínio (FOWLER, 2002, cap. 10) | `app/models/status.rb`, `account.rb` | classes herdam de `ApplicationRecord`; atributos são colunas | simplicidade; acoplamento entre objetos e esquema; `status.rb` com mais de 500 linhas |
| *Service Layer* / *Transaction Script* | camada que define as operações da aplicação; procedimento por operação (FOWLER, 2002, cap. 9) | `app/services/*.rb` | `BaseService#call`; `PostStatusService` | controladores e modelos enxutos; lógica procedural por operação |
| *Command* | encapsular uma requisição como objeto, permitindo enfileirá-la (GAMMA *et al.*, 1994) | `app/workers/*.rb` | cada *worker* é um comando serializado na fila, executado por `perform` | retentativa, priorização por fila, execução fora da requisição |
| *Observer* | dependência um-para-muitos com notificação automática (GAMMA *et al.*, 1994) | `status.rb`, L157-L174 | *callbacks* de ciclo de vida | efeitos implícitos em qualquer criação, inclusive em importações e testes |
| *Singleton* | uma única instância com ponto global de acesso (GAMMA *et al.*, 1994) | `app/lib/feed_manager.rb` | `include Singleton`; uso por `FeedManager.instance` | acesso global ao Redis das *timelines*; dificulta substituição em testes |
| *Factory Method* (variação parametrizada) | criar objetos deixando a decisão da classe para outro ponto (GAMMA *et al.*, 1994) | `app/lib/activitypub/activity.rb` | `Activity.factory` escolhe a subclasse pelo campo `type` | novos tipos de atividade exigem nova subclasse e novo ramo no `case` |
| *Template Method* | esqueleto de algoritmo com etapas redefinidas em subclasses (GAMMA *et al.*, 1994) | `raw_distribution_worker.rb` e `distribution_worker.rb` | `distribute!` chama `inboxes`, `payload` e `options`, redefinidos na subclasse | reuso do mecanismo de entrega para vários tipos de atividade |
| *Gateway* | objeto que encapsula o acesso a um sistema externo (FOWLER, 2002, cap. 18) | `app/lib/request.rb` | `Request` assina e envia requisições HTTP | assinatura e políticas de rede centralizadas |
| Disjuntor | interromper chamadas a um destino que falha repetidamente (RICHARDS; FORD, 2020, p. 124) | `delivery_worker.rb` | `Stoplight(...)` envolve a requisição | falhas de uma instância remota não consomem os *workers* |

Fonte: elaborado pelo autor (2026), com base no código da *tag* v4.7.3.

## 6.2.9 Subida de verificação

A subida de verificação (Seção 1.11) parte de um trecho de código e sobe as altitudes perguntando que
componente ele realiza, que decisão concretiza e que característica protege. A Tabela 6.2.2 aplica o
procedimento aos trechos desta seção.

**Tabela 6.2.2** – Subida de verificação para o Mastodon.

| Solo (código) | 10.000 pés (componente) | 50.000 pés (decisão) | 100.000 pés (característica) |
|---|---|---|---|
| `DistributionWorker.perform_async` (Listagem 6.2.1) | *job* de distribuição | processamento assíncrono em Sidekiq (ADR-M1, ADR-M4) | disponibilidade da API sob carga; tempo de resposta da publicação |
| `FeedInsertWorker.push_bulk` (Listagem 6.2.4) | serviço de *fanout* | *fanout on write* em lotes de *jobs* (ADR-M4) | desempenho de leitura (C1) |
| `MAX_ITEMS = 800` e `trim` (Listagem 6.2.5) | gerenciador de *feeds* | Redis como armazenamento derivado e limitado (ADR-M3) | custo de memória; simplicidade operacional |
| `ACTIVE_DURATION` (Listagem 6.2.8) e `RegenerationWorker` | gerenciador de *feeds* | *push* só para ativos, *pull* na volta (ADR-M4) | custo de processamento; elasticidade (C3) |
| `subscriptionHeartbeat` (Listagem 6.2.9) | *streaming* e barramento Redis | *streaming* em processo separado (ADR-M2) | tempo quase real com uso eficiente de recursos |
| `Activity.factory` (Listagem 6.2.10) | fronteira ActivityPub | tradução na fronteira (camada anticorrupção) | interoperabilidade |
| `retry: 16`, `sidekiq_retry_in`, Stoplight (Listagem 6.2.12) | entrega federada | entrega assíncrona com retentativa e disjuntor | interoperabilidade e disponibilidade sob falhas parciais (C2) |
| `after_create_commit` (Listagem 6.2.13) | modelo *Active Record* | invariantes locais em *callbacks*; efeitos de negócio em *services* | manutenibilidade, com risco de acoplamento |

Fonte: elaborado pelo autor (2026).

A cadeia se completa em todos os trechos, indício de que a arquitetura está compreendida. À pergunta
"que característica de 100.000 pés o *job* assíncrono de *fanout* protege?", a resposta é composta:
o **desempenho de leitura**, porque prepara a *timeline* antes de ela ser pedida; a
**disponibilidade** da API, porque a publicação não fica refém do número de seguidores; e a
**simplicidade operacional**, porque o mesmo mecanismo de filas absorve picos sem componentes
adicionais. A única lacuna encontrada é a ausência de tratamento de celebridades no *fanout* local,
que o ADR-M4 registra como consequência aceita e que, no contexto de instâncias pequenas, é razoável.

O altímetro, apresentado na Seção 1.11, percorre essa mesma descida e subida sobre o Mastodon.

<iframe src="../../interativos/altimetro.html" title="Altímetro: do contexto ao código"
  style="width:100%; height:660px; border:1px solid #2A4468; border-radius:10px;"
  loading="lazy"></iframe>

## 6.2.10 Um modelo didático em Java e o exercício

A Listagem 6.2.14 traduz a decisão de *fanout* para Java 17, acrescentando o limiar de celebridade
que o Mastodon não implementa no *fanout* local. O serviço depende apenas de três portas de saída, no
estilo da arquitetura hexagonal (Seção 1.8.2). O código é ilustrativo e simplificado; num sistema
real, `aoPublicar` seria executado por um *job* assíncrono, como o `DistributionWorker`.

**Listagem 6.2.14** – *Fanout* híbrido em Java (ilustrativo e simplificado).

```java
package br.edu.ifal.arqsw.timeline.aplicacao;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Timeline com fanout híbrido: push para a maioria, pull para celebridades. */
public final class ServicoDeTimeline {

    /** Porta de saída para o cache de timelines (no Mastodon, um ZSET do Redis). */
    public interface CacheDeTimelines {
        void inserir(long donoId, long postId);
        List<Long> recentes(long donoId, int limite);
    }

    /** Porta de saída para o grafo social (quem segue quem). */
    public interface GrafoSocial {
        long totalDeSeguidores(long autorId);
        List<Long> seguidoresAtivos(long autorId);
        List<Long> celebridadesSeguidasPor(long usuarioId);
    }

    /** Porta de saída para as publicações recentes de um autor. */
    public interface PostsPorAutor {
        List<Long> recentesDe(long autorId, int limite);
    }

    private final CacheDeTimelines cache;
    private final GrafoSocial grafo;
    private final PostsPorAutor posts;
    private final long limiarCelebridade;

    public ServicoDeTimeline(CacheDeTimelines cache, GrafoSocial grafo,
                             PostsPorAutor posts, long limiarCelebridade) {
        this.cache = cache;
        this.grafo = grafo;
        this.posts = posts;
        this.limiarCelebridade = limiarCelebridade;
    }

    /** Caminho de escrita; num sistema real, executado por um job assíncrono. */
    public void aoPublicar(long autorId, long postId) {
        cache.inserir(autorId, postId); // o autor vê a própria publicação
        if (grafo.totalDeSeguidores(autorId) >= limiarCelebridade) {
            return; // celebridade: nada de fanout; os seguidores buscarão na leitura
        }
        for (long seguidor : grafo.seguidoresAtivos(autorId)) {
            cache.inserir(seguidor, postId); // fanout on write
        }
    }

    /** Caminho de leitura: lista pré-computada + mescla das celebridades (fanout on read). */
    public List<Long> timeline(long usuarioId, int limite) {
        List<Long> ids = new ArrayList<>(cache.recentes(usuarioId, limite));
        for (long celebridade : grafo.celebridadesSeguidasPor(usuarioId)) {
            ids.addAll(posts.recentesDe(celebridade, limite));
        }
        // IDs ordenáveis pelo tempo (como os Snowflake): maior ID = mais recente
        return ids.stream()
                .distinct()
                .sorted(Comparator.reverseOrder())
                .limit(limite)
                .toList();
    }
}
```

Fonte: elaborado pelo autor (2026), com base em Xu (2020, cap. 11).

O serviço **não conhece o banco de dados**: só fala com portas. Essa é a regra verificada pela
atividade com correção automática prevista para o Mastodon em `atividades/README.md`, expressa como
função de aptidão (Apêndice E) com ArchUnit na Listagem 6.2.15. O limiar de celebridade é um
parâmetro, e não uma constante escondida, pela mesma razão que o Mastodon expõe `USER_ACTIVE_DAYS`:
o ponto de corte entre *push* e *pull* depende de medições.

**Listagem 6.2.15** – Função de aptidão: o serviço de *timeline* não acessa o "banco" diretamente.

```java
@Test
@DisplayName("a aplicacao da timeline nao depende do pacote de banco")
void timelineNaoAcessaBanco() {
    noClasses().that().resideInAPackage(BASE + ".aplicacao..")
            .should().dependOnClassesThat().resideInAPackage(BASE + ".banco..")
            .because("a timeline e lida do cache; o banco e detalhe de infraestrutura")
            .check(classes);
}
```

Fonte: elaborado pelo autor (2026), no formato do exercício-modelo de `atividades/modelo-exercicio`.

## Exercícios

1. Localize no repositório, na *tag* v4.7.3, o caminho de uma publicação desde
   `Api::V1::StatusesController#create` até a chamada a `redis.zadd`. Liste os arquivos e métodos
   percorridos e indique quais executam na requisição e quais em *jobs*.
2. Encontre no modelo `Status` um *callback* não discutido na Seção 6.2.7 e explique o risco de
   acoplamento que ele cria. Proponha como movê-lo para um *service object* e discuta o que se ganha
   e o que se perde.
3. Calcule a soma das esperas das 16 retentativas da Listagem 6.2.12, sem *jitter*, e compare com a
   regra de 7 dias do `DeliveryFailureTracker`. Por que as duas janelas são diferentes?
4. Dê exemplos de respostas HTTP que encerram a entrega sem retentar e de respostas que provocam
   retentativa. Justifique, para cada uma, por que retentar seria inútil ou útil.
5. O `FeedManager` é um *Singleton*. Escreva, em Java, uma versão do `ServicoDeTimeline` da Listagem
   6.2.14 que receba o `FeedManager` por injeção de dependência e discuta o efeito sobre os testes.
6. Acrescente à Tabela 6.2.1 um padrão que não foi identificado, com a localização no código e a
   justificativa. Sugestão: procure em `app/policies` ou `app/serializers`.
7. **Atividade com correção automática** (Java 17, JUnit e ArchUnit; ver `atividades/README.md`,
   linha do Mastodon): implemente uma *timeline* com cache, nos modos *fanout on write* e *on read*,
   de modo que o serviço de *timeline* não acesse o "banco" diretamente. Os testes de comportamento
   verificam a ordem e o conteúdo da *timeline*; os de arquitetura verificam regras como a da
   Listagem 6.2.15.

## Termos

`Fanout`
:   Distribuição de uma publicação para as *timelines* de todos os seguidores do autor; no modelo
    *push* (*fanout on write*), feita na escrita; no modelo *pull* (*fanout on read*), na leitura.

`Federação`
:   Arquitetura em que servidores independentes, com administração própria, interoperam por um
    protocolo comum, como o ActivityPub, sem um centro único.

`Hidratação`
:   Montagem da resposta completa a partir de uma lista de identificadores, buscando o conteúdo de
    cada item em caches ou no banco.

`Instância`
:   Servidor Mastodon independente, com seus próprios usuários, administradores, políticas e
    infraestrutura, que participa da rede federada.

`Service Object`
:   Classe sem estado próprio no banco que encapsula uma operação de negócio envolvendo vários
    modelos, em geral exposta por um único método público.

## Referências

FOWLER, M. **Patterns of enterprise application architecture**. Boston: Addison-Wesley, 2002.

FOX, A.; PATTERSON, D. **Engineering software as a service**: an agile approach using cloud
computing. 2. ed., versão 2.0b7. [*S. l.*]: Strawberry Canyon, 2021.

GAMMA, E. *et al.* **Design patterns**: elements of reusable object-oriented software. Reading:
Addison-Wesley, 1994.

KRIKORIAN, R. **New Tweets per second record, and how!** Twitter Engineering Blog, 16 ago. 2013.
Disponível em:
<https://blog.x.com/engineering/en_us/a/2013/new-tweets-per-second-record-and-how>. Acesso em:
18 out. 2026.

MASTODON. **mastodon/mastodon**: repositório de código-fonte, versão v4.7.3. GitHub, 2026.
Disponível em: <https://github.com/mastodon/mastodon/tree/v4.7.3>. Acesso em: 18 out. 2026.

RICHARDS, M.; FORD, N. **Fundamentals of software architecture**: an engineering approach.
Sebastopol: O'Reilly, 2020.

XU, A. **System design interview**: an insider's guide. 2. ed. [*S. l.*]: Independently
published, 2020.
