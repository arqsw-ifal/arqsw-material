# 6.4 PeerTube: o código

<span class="altitude solo">Solo</span>

Esta seção pousa no código do PeerTube. O objetivo é localizar, em trechos literais da *tag*
`v8.3.1`, as decisões discutidas nas Seções 3.4, 4.4 e 5.4: o modelo de domínio e sua máquina de
estados, as cadeias de *middlewares*, a guarda de idempotência do *upload*, o *pipeline* como
fluxo de *jobs*, os padrões *Template Method* e *Strategy* que tornam possíveis os *runners*
remotos e os perfis de codificação extensíveis, e a verificação de integridade da entrega P2P. A
seção termina com a subida de verificação e com a versão didática, em Java 17, do estilo *pipes
and filters*, que é a base do exercício com correção automática.

Todos os trechos foram conferidos na *tag* indicada e são acompanhados de *permalink*. As
listagens estão em TypeScript, a linguagem do projeto.

## 6.4.1 Mapa do repositório

O repositório do PeerTube é um monorrepositório. O código do servidor está em `server/core/`, o
cliente Angular em `client/`, os pacotes compartilhados (modelos, ffmpeg, utilitários) em
`packages/` e as aplicações auxiliares, entre elas o *runner*, em `apps/`. A Tabela 6.4.1
relaciona os conceitos dos capítulos anteriores aos caminhos em que eles se materializam.

**Tabela 6.4.1** – Onde cada conceito está no código do PeerTube (*tag* `v8.3.1`).

| Conceito | Caminho |
|---|---|
| Modelo de vídeo (*Active Record*) | `server/core/models/video/video.ts` |
| Estados do vídeo | `packages/models/src/videos/video-state.enum.ts` e `server/core/lib/video-state.ts` |
| Rotas de *upload* | `server/core/controllers/api/videos/upload.ts` e `server/core/lib/uploadx.ts` |
| Validadores de vídeo | `server/core/middlewares/validators/videos/videos.ts` |
| Adaptador de *middlewares* assíncronos | `server/core/middlewares/async.ts` |
| Fila de *jobs* | `server/core/lib/job-queue/job-queue.ts` e `handlers/` |
| Encadeamento inicial de *jobs* | `server/core/lib/video-jobs.ts` |
| Construtores do DAG | `server/core/lib/transcoding/shared/job-builders/` |
| *Handlers* de *jobs* de *runner* | `server/core/lib/runners/job-handlers/` |
| API dos *runners* | `server/core/controllers/api/runners/jobs.ts` |
| Perfis de codificação | `packages/ffmpeg/src/ffmpeg-default-transcoding-profile.ts` e `server/core/lib/transcoding/default-transcoding-profiles.ts` |
| Comandos ffmpeg de HLS | `packages/ffmpeg/src/ffmpeg-vod.ts` |
| Manifesto mestre e resumos SHA-256 | `server/core/lib/hls.ts` |
| Validação de segmentos P2P | `client/src/standalone/player/src/shared/p2p-media-loader/segment-validator.ts` |
| Caixa de entrada ActivityPub | `server/core/controllers/activitypub/inbox.ts` |

Fonte: elaborado pelo autor (2026), a partir do repositório do PeerTube (*tag* `v8.3.1`).

## 6.4.2 O modelo de domínio

### 6.4.2.1 Entidades e máquina de estados

O centro do modelo de domínio é o vídeo. Em torno dele estão o canal, a conta e o ator
ActivityPub (que dá identidade federada a contas e canais), os arquivos de vídeo, as *playlists*
de *streaming*, as legendas, os capítulos e os comentários, cada um mapeado para uma classe em
`server/core/models/`. Na terminologia do DDD tático (EVANS, 2003; ver Seção 1.9), o vídeo pode ser
interpretado como a raiz de um agregado que inclui seus arquivos e *playlists*: os *handlers*
carregam o vídeo completo (`VideoModel.loadFull`) antes de alterar qualquer arquivo, e o UUID do
vídeo é a identidade usada nos *payloads* de *job*. O código, porém, não impõe essa fronteira:
nada impede que um módulo altere diretamente um arquivo de vídeo sem passar pela raiz.

O ciclo de vida do vídeo é uma máquina de estados explícita. O tipo `VideoState` enumera doze
estados, entre eles `PUBLISHED`, `TO_TRANSCODE`, `TO_MOVE_TO_EXTERNAL_STORAGE`,
`TRANSCODING_FAILED` e os estados das transmissões ao vivo (PEERTUBE, 2026,
[`video-state.enum.ts#L1-L14`](https://github.com/Chocobozzz/PeerTube/blob/v8.3.1/packages/models/src/videos/video-state.enum.ts#L1-L14)).
A Listagem 6.4.1 mostra a função que decide o próximo estado. A ordem das condições codifica o
*pipeline* da Seção 5.4: transcodificar, se habilitado; depois mover para o armazenamento de
objetos, se habilitado; por fim, publicar.

**Listagem 6.4.1** – Código real (TypeScript, PeerTube v8.3.1): regra de transição do estado do vídeo.

```typescript
export function buildNextVideoState (currentState?: VideoStateType) {
  if (currentState === VideoState.PUBLISHED) {
    throw new Error('Video is already in its final state')
  }

  if (
    currentState !== VideoState.TO_EDIT &&
    currentState !== VideoState.TO_TRANSCODE &&
    currentState !== VideoState.TO_MOVE_TO_EXTERNAL_STORAGE &&
    currentState !== VideoState.TO_MOVE_TO_FILE_SYSTEM &&
    CONFIG.TRANSCODING.ENABLED
  ) {
    return VideoState.TO_TRANSCODE
  }

  if (
    currentState !== VideoState.TO_MOVE_TO_EXTERNAL_STORAGE &&
    currentState !== VideoState.TO_MOVE_TO_FILE_SYSTEM &&
    CONFIG.OBJECT_STORAGE.ENABLED
  ) {
    return VideoState.TO_MOVE_TO_EXTERNAL_STORAGE
  }

  return VideoState.PUBLISHED
}
```

Fonte: PeerTube (2026),
[`server/core/lib/video-state.ts#L16-L40`](https://github.com/Chocobozzz/PeerTube/blob/v8.3.1/server/core/lib/video-state.ts#L16-L40).

### 6.4.2.2 *Active Record*

A classe `VideoModel` estende `SequelizeModel`, classe-base dos modelos do projeto construída sobre
a biblioteca `sequelize-typescript`, e tem cerca de 2.400 linhas na
versão estudada (PEERTUBE, 2026, `server/core/models/video/video.ts`). Ela declara as colunas com
decoradores, define consultas estáticas como `loadFull` e contém comportamento de domínio. A
Listagem 6.4.2 mostra um exemplo: ao mudar de estado, o próprio objeto aplica a regra de que a
primeira publicação define as datas `publishedAt` e `firstPublishedAt`, e grava a si mesmo no
banco, dentro da transação recebida.

**Listagem 6.4.2** – Código real (TypeScript, PeerTube v8.3.1): regra de domínio e persistência no mesmo objeto.

```typescript
  async setNewStateAndPublishedAt (options: {
    newState: VideoStateType
    transaction: Transaction
  }) {
    const { newState, transaction } = options

    if (this.state === newState) throw new Error('Cannot use same state ' + newState)

    this.state = newState

    if (this.state === VideoState.PUBLISHED && !this.firstPublishedAt) {
      this.publishedAt = new Date()
      this.firstPublishedAt = this.publishedAt
    }

    await this.save({ transaction })
  }
```

Fonte: PeerTube (2026),
[`server/core/models/video/video.ts#L2364-L2380`](https://github.com/Chocobozzz/PeerTube/blob/v8.3.1/server/core/models/video/video.ts#L2364-L2380).

É o padrão *Active Record* de Fowler (2002): um objeto que carrega dados e comportamento e que
contém a lógica de acesso ao banco. O mesmo arquivo contém, por exemplo, o método
`getBandwidthBits`, que calcula a taxa de bits de um arquivo a partir do tamanho e da duração e é
usado na *playlist* mestre (Listagem 6.4.14). Fowler (2002) observa que o *Active Record* é uma boa
escolha quando a lógica de domínio não é muito complexa e as classes correspondem de perto às
tabelas; a
dimensão da classe `VideoModel` indica que o PeerTube está no limite desse equilíbrio, e a
extração das consultas complexas para construtores separados, como `VideoModelGetQueryBuilder`
(um *Query Object*), é a forma que o projeto encontrou de conter esse crescimento.

## 6.4.3 *Chain of Responsibility*: as cadeias de *middlewares*

A Listagem 6.4.3 mostra a função que monta as rotas do *upload* retomável. Cada rota é uma cadeia:
autenticação, *middlewares* "antes" fornecidos pelo chamador, validador de inicialização,
*middlewares* "depois" e, por fim, a biblioteca `uploadx`, que trata o protocolo. O comentário da
rota `PUT` explica uma sutileza: a biblioteca só chama `next()` quando o arquivo está completo, de
modo que os validadores e o controlador posicionados depois dela executam uma única vez, no fim
do envio. Enquanto as partes chegam, o elo `uploadx.upload` trata a requisição sozinho e responde
com 308; quando a última parte chega, passa a requisição adiante.

**Listagem 6.4.3** – Código real (TypeScript, PeerTube v8.3.1): montagem das rotas do *upload* retomável como cadeias de *middlewares*.

```typescript
  router.post(
    routePath,
    authenticate,
    ...uploadInitBeforeMiddlewares,
    resumableInitValidator,
    ...uploadInitAfterMiddlewares,
    // Prevent next() call, explicitly tell to uploadx it's the end
    (req, res) => uploadx.upload(req, res)
  )

  router.delete(
    routePath,
    authenticate,
    ...uploadDeleteMiddlewares,
    // Prevent next() call, explicitly tell to uploadx it's the end
    (req, res) => uploadx.upload(req, res)
  )

  router.put(
    routePath,
    authenticate,
    uploadx.upload, // uploadx doesn't next() before the file upload completes
    ...uploadedMiddlewares,
    uploadedController
  )
```

Fonte: PeerTube (2026),
[`server/core/lib/uploadx.ts#L71-L95`](https://github.com/Chocobozzz/PeerTube/blob/v8.3.1/server/core/lib/uploadx.ts#L71-L95).

A mesma forma aparece na federação. Antes que o controlador examine qualquer atividade recebida,
a requisição passa por um limitador de taxa, por um validador do cabeçalho de assinatura, pela
verificação criptográfica da assinatura e pela validação estrutural da atividade (Listagem 6.4.4).

**Listagem 6.4.4** – Código real (TypeScript, PeerTube v8.3.1): cadeia de *middlewares* da caixa de entrada ActivityPub.

```typescript
inboxRouter.post(
  '/inbox',
  activityPubRateLimiter,
  signatureValidator,
  asyncMiddleware(checkSignature),
  asyncMiddleware(activityPubValidator),
  inboxController
)
```

Fonte: PeerTube (2026),
[`server/core/controllers/activitypub/inbox.ts#L21-L28`](https://github.com/Chocobozzz/PeerTube/blob/v8.3.1/server/core/controllers/activitypub/inbox.ts#L21-L28).

O Express foi concebido para funções que chamam `next`; funções assíncronas que rejeitam uma
*promise* escapariam do tratamento de erros. O adaptador `asyncMiddleware` embrulha a função,
captura a rejeição e a converte em `next(err)`; quando recebe uma lista de funções, como as
cadeias do `express-validator`, executa-as em sequência e só chama `next()` se todas passarem
(Listagem 6.4.5). É uma cadeia dentro da cadeia, e um uso do padrão *Adapter* (GAMMA *et al.*,
1994) a serviço do *Chain of Responsibility*.

**Listagem 6.4.5** – Código real (TypeScript, PeerTube v8.3.1): adaptador que integra funções assíncronas à cadeia do Express.

```typescript
export function asyncMiddleware (fun: RequestPromiseHandler | RequestPromiseHandler[]) {
  return async (req: Request, res: Response, next: NextFunction) => {
    if (Array.isArray(fun) !== true) {
      return Promise.resolve((fun as RequestHandler)(req, res, next))
        .catch(err => next(err))
    }

    try {
      for (const f of fun) {
        await new Promise<void>((resolve, reject) => {
          return asyncMiddleware(f)(req, res, err => {
            if (err) return reject(err)

            return resolve()
          })
        })
      }

      next()
    } catch (err) {
      next(err)
    }
  }
}
```

Fonte: PeerTube (2026),
[`server/core/middlewares/async.ts#L10-L33`](https://github.com/Chocobozzz/PeerTube/blob/v8.3.1/server/core/middlewares/async.ts#L10-L33).

**Tabela 6.4.2** – Ficha *Known Uses*: *Chain of Responsibility* no PeerTube.

| Campo | Conteúdo |
|---|---|
| **Padrão** | *Chain of Responsibility* (GAMMA *et al.*, 1994, cap. 5) |
| **Intenção** | Evitar acoplar o remetente de uma requisição ao seu receptor, dando a mais de um objeto a oportunidade de tratá-la; encadear os receptores e passar a requisição ao longo da cadeia |
| **Uso conhecido** | Rotas Express de `upload.ts`, `uploadx.ts`, `inbox.ts` e `runners/jobs.ts` |
| **Participantes** | *Handler*: assinatura `(req, res, next)`; *ConcreteHandlers*: `authenticate`, validadores, `checkSignature`, limitadores de taxa; *Client*: o roteador Express |
| **Variação** | Quase todos os elos trabalham e passam adiante; só o último produz a resposta de sucesso |
| **Consequências** | Cada regra transversal fica em um elo reutilizável e a declaração da rota documenta a política de segurança; em troca, a ordem dos elos é significativa e não é verificada pelo compilador |

Fonte: elaborado pelo autor (2026).

## 6.4.4 A guarda de idempotência do *upload*

A Seção 5.4.2 apontou o risco de duplicação quando a resposta à última parte do *upload* se perde e
o cliente repete a requisição. A Listagem 6.4.6 mostra o trecho do validador
`videosAddResumableValidator`, executado depois que o arquivo está completo, que trata esse risco.
Antes de qualquer outra verificação, o validador consulta no Redis se já existe uma sessão de
processamento para aquele `upload_id`. Se existir, recusa a requisição com 503 e um cabeçalho
`Retry-After` de 300 segundos; se não existir, registra a sessão e continua. O controlador, ao
terminar, apaga a sessão e o arquivo temporário (PEERTUBE, 2026,
[`upload.ts#L89-L102`](https://github.com/Chocobozzz/PeerTube/blob/v8.3.1/server/core/controllers/api/videos/upload.ts#L89-L102)).

**Listagem 6.4.6** – Código real (TypeScript, PeerTube v8.3.1): detecção de requisição duplicada no fim do *upload* retomável.

```typescript
    const uploadId = req.query.upload_id
    const sessionExists = await Redis.Instance.doesUploadSessionExist(uploadId)

    if (sessionExists) {
      res.setHeader('Retry-After', 300) // ask to retry after 5 min, knowing the upload_id is kept for up to 15 min after completion

      return res.fail({
        status: HttpStatusCode.SERVICE_UNAVAILABLE_503,
        message: req.t('The upload is already being processed')
      })
    }

    await Redis.Instance.setUploadSession(uploadId)
```

Fonte: PeerTube (2026),
[`server/core/middlewares/validators/videos/videos.ts#L135-L147`](https://github.com/Chocobozzz/PeerTube/blob/v8.3.1/server/core/middlewares/validators/videos/videos.ts#L135-L147).

O trecho é uma implementação compacta do *Idempotent Receiver* por detecção de duplicatas (HOHPE;
WOOLF, 2003). Convém notar o que ele **não** garante: a consulta e o registro da sessão são duas
operações separadas no Redis, e duas requisições exatamente simultâneas poderiam, em tese, passar
ambas pela consulta antes que qualquer uma registrasse a sessão. Uma operação atômica do tipo
"grave se não existir" eliminaria essa janela. A observação ilustra uma regra geral: verificar e
agir em passos separados sobre um estado compartilhado cria condições de corrida, e o analista
deve procurá-las sempre que encontrar uma guarda de idempotência.

## 6.4.5 O *pipeline* como fluxo de *jobs*

A Listagem 6.4.7 mostra a lista de *jobs* montada para cada vídeo recém-criado. Lida como
arquitetura, a lista é um *pipeline* declarado como dado: cada elemento é um filtro, identificado
por um tipo, e o cano entre eles é a fila. A lista é entregue a `createSequentialJobFlow`, que usa
os fluxos do BullMQ, em que um *job* pai só se torna elegível depois que todos os filhos terminam
com sucesso (BULLMQ, 2026); fazendo cada *job* pai do anterior, o PeerTube obtém uma sequência
estrita (PEERTUBE, 2026,
[`job-queue.ts#L415-L433`](https://github.com/Chocobozzz/PeerTube/blob/v8.3.1/server/core/lib/job-queue/job-queue.ts#L415-L433)).

**Listagem 6.4.7** – Código real (TypeScript, PeerTube v8.3.1): os primeiros filtros do *pipeline* de um vídeo recém-criado.

```typescript
  const jobs: (CreateJobTypeAndPayload & CreateJobOptions)[] = [
    {
      type: 'manage-video-torrent' as 'manage-video-torrent',
      payload: {
        videoId: video.id,
        videoFileId: videoFile.id,
        action: 'create'
      }
    },

    await buildLocalStoryboardJobIfNeeded({ video, federate: false }),

    // Before the notify/federate jobs so an auto tag block policy is applied before the video is announced
    // And before the transcoding jobs so a plugin auto tagger analyzes the input file while it is still untouched
    buildNonDuplicatedVideoAutomaticTagsJob({ video, moderation: 'apply' }),

    {
      type: 'notify',
      payload: {
        action: 'new-video',
        videoUUID: video.uuid
      }
    },

    buildNonDuplicatedFederateVideoJob({ video })
  ]
```

Fonte: PeerTube (2026),
[`server/core/lib/video-jobs.ts#L96-L121`](https://github.com/Chocobozzz/PeerTube/blob/v8.3.1/server/core/lib/video-jobs.ts#L96-L121).

Os comentários explicam a ordem: a etiquetagem automática vem antes da notificação e da federação
para que uma política de bloqueio seja aplicada antes de o vídeo ser anunciado, e antes da
transcodificação para que um *plugin* analise o arquivo de entrada ainda intacto. A ordem dos
filtros codifica, portanto, regras de negócio de moderação. Logo abaixo do trecho, a função
acrescenta o *job* `transcoding-job-builder` quando o vídeo está em `TO_TRANSCODE`.

A Listagem 6.4.8 mostra as opções comuns de fila: tentativas por tipo, recuo exponencial a partir
de um minuto, prioridade e deduplicação opcional.

**Listagem 6.4.8** – Código real (TypeScript, PeerTube v8.3.1): opções comuns a todos os *jobs*.

```typescript
  private buildJobOptions (type: JobType, options: CreateJobOptions = {}): JobsOptions {
    return {
      backoff: { delay: 60 * 1000, type: 'exponential' },
      attempts: JOB_ATTEMPTS[type],
      priority: options.priority,
      delay: options.delay,

      deduplication: options.deduplicationId
        ? {
          id: options.deduplicationId,
          keepLastIfActive: options.deduplicationKeepLastIfActive
        }
        : undefined,

      ...this.buildJobRemovalOptions(type)
    }
  }
```

Fonte: PeerTube (2026),
[`server/core/lib/job-queue/job-queue.ts#L459-L475`](https://github.com/Chocobozzz/PeerTube/blob/v8.3.1/server/core/lib/job-queue/job-queue.ts#L459-L475).

## 6.4.6 *Template Method*: construtores de DAG e *handlers* de *runner*

O padrão *Template Method* define o esqueleto de um algoritmo em uma operação, adiando alguns
passos para as subclasses, que os redefinem sem alterar a estrutura do algoritmo; Gamma *et al.*
(1994, cap. 5) observam que ele produz uma estrutura de controle invertida, o "princípio de
Hollywood": é a classe-mãe que chama as operações da subclasse.

O primeiro uso está na classe abstrata `AbstractJobBuilder`. Seu método
`createOptimizeOrMergeAudioJobs` é o algoritmo de construção do DAG da Seção 5.4: decide se o
arquivo é áudio ou vídeo, calcula resolução e taxa de quadros máximas, monta o ramo de HLS na
resolução máxima, calcula as resoluções inferiores e monta seus ramos. Os passos que dependem de
**onde** os *jobs* serão executados são abstratos: `buildOptimizePayload`, `buildHLSJobPayload`,
`buildWebVideoJobPayload`, `buildMergeAudioPayload` e `createJobs` (PEERTUBE, 2026,
[`abstract-job-builder.ts#L14-L129`](https://github.com/Chocobozzz/PeerTube/blob/v8.3.1/server/core/lib/transcoding/shared/job-builders/abstract-job-builder.ts#L14-L129)).
A subclasse `TranscodingJobQueueBuilder` transforma o DAG em fluxos do BullMQ; a subclasse
`TranscodingRunnerJobBuilder` transforma o mesmo DAG em registros de *jobs* de *runner* no
PostgreSQL, ligados ao *job* pai. A escolha é feita por uma pequena função de fábrica (Listagem
6.4.9).

**Listagem 6.4.9** – Código real (TypeScript, PeerTube v8.3.1): escolha da subclasse que materializa o DAG.

```typescript
function getJobBuilder () {
  if (CONFIG.TRANSCODING.REMOTE_RUNNERS.ENABLED === true) {
    return new TranscodingRunnerJobBuilder()
  }

  return new TranscodingJobQueueBuilder()
}
```

Fonte: PeerTube (2026),
[`server/core/lib/transcoding/create-transcoding-job.ts#L28-L34`](https://github.com/Chocobozzz/PeerTube/blob/v8.3.1/server/core/lib/transcoding/create-transcoding-job.ts#L28-L34).

Essa combinação é o que tornou o ADR-PT-02 (Seção 4.4) implementável sem duplicação: o algoritmo
que decide **quais** resoluções gerar e em que ordem existe em um único lugar; apenas a forma de
**despachar** as tarefas varia.

O segundo uso é a hierarquia de *handlers* de *jobs* de *runner*. A classe `AbstractJobHandler`
define o ciclo de vida comum a todo *job* remoto e deixa às subclasses os passos
`specificUpdate`, `specificComplete`, `specificCancel`, `specificAbort` e `specificError`. A
Listagem 6.4.10 mostra o método `complete`, executado quando um *runner* devolve um resultado:
marca o *job* como em conclusão, delega o trabalho específico à subclasse, registra sucesso ou
erro, limpa arquivos temporários e libera os *jobs* dependentes. Para um *job* de HLS, o passo
específico registra o novo arquivo e a *playlist*, verifica se ainda falta algum fluxo e, se for o
caso, move o vídeo para o próximo estado (PEERTUBE, 2026,
[`vod-hls-transcoding-job-handler.ts#L96-L141`](https://github.com/Chocobozzz/PeerTube/blob/v8.3.1/server/core/lib/runners/job-handlers/vod-hls-transcoding-job-handler.ts#L96-L141)).

**Listagem 6.4.10** – Código real (TypeScript, PeerTube v8.3.1): método-modelo que conclui um *job* de *runner*.

```typescript
  async complete (options: {
    runnerJob: MRunnerJob
    resultPayload: SuccessPayload
  }) {
    const { runnerJob, resultPayload } = options

    await logger.withContext([ runnerJob.uuid, runnerJob.type ], async () => {
      runnerJob.state = RunnerJobState.COMPLETING
      await saveInTransactionWithRetries(runnerJob)

      try {
        await this.specificComplete(options)

        runnerJob.state = RunnerJobState.COMPLETED
      } catch (err) {
        logger.error('Cannot complete runner job', { err })

        runnerJob.state = RunnerJobState.ERRORED
        runnerJob.error = err.message
      } finally {
        // specificComplete() moves whatever uploaded file it consumes into permanent storage
        // Remove anything it left behind in tmp directory
        this.cleanupResultPayloadFiles(resultPayload)
      }

      runnerJob.progress = null
      runnerJob.finishedAt = new Date()

      await saveInTransactionWithRetries(runnerJob)

      const [ affectedCount ] = await RunnerJobModel.updateDependantJobsOf(runnerJob)

      if (affectedCount !== 0) PeerTubeSocket.Instance.sendAvailableJobsPingToRunners()
    })
  }
```

Fonte: PeerTube (2026),
[`server/core/lib/runners/job-handlers/abstract-job-handler.ts#L150-L184`](https://github.com/Chocobozzz/PeerTube/blob/v8.3.1/server/core/lib/runners/job-handlers/abstract-job-handler.ts#L150-L184).

O lado de rede desse protocolo está na API dos *runners* (Listagem 6.4.11). O *runner* pede um
*job* disponível e depois aceita um *job* específico. Nenhuma das rotas exige autenticação de
usuário: o *runner* se identifica pelo próprio *token*, verificado pelo elo
`getRunnerFromTokenValidator`. Quando há trabalho novo, a instância apenas avisa os *runners*
conectados por Socket.IO; quem inicia a transferência é sempre o *runner*.

**Listagem 6.4.11** – Código real (TypeScript, PeerTube v8.3.1): rotas pelas quais um *runner* pede e aceita *jobs*.

```typescript
runnerJobsRouter.post(
  '/jobs/request',
  apiRateLimiter,
  requestRunnerJobValidator,
  asyncMiddleware(getRunnerFromTokenValidator),
  asyncMiddleware(requestRunnerJob)
)

runnerJobsRouter.post(
  '/jobs/:jobUUID/accept',
  apiRateLimiter,
  asyncMiddleware(runnerJobGetValidator),
  acceptRunnerJobValidator,
  asyncMiddleware(getRunnerFromTokenValidator),
  asyncMiddleware(acceptRunnerJob)
)
```

Fonte: PeerTube (2026),
[`server/core/controllers/api/runners/jobs.ts#L82-L97`](https://github.com/Chocobozzz/PeerTube/blob/v8.3.1/server/core/controllers/api/runners/jobs.ts#L82-L97).

**Tabela 6.4.3** – Ficha *Known Uses*: *Template Method* no PeerTube.

| Campo | Conteúdo |
|---|---|
| **Padrão** | *Template Method* (GAMMA *et al.*, 1994, cap. 5) |
| **Intenção** | Definir o esqueleto de um algoritmo, adiando passos para subclasses |
| **Uso conhecido 1** | `AbstractJobBuilder.createOptimizeOrMergeAudioJobs`, com as subclasses `TranscodingJobQueueBuilder` e `TranscodingRunnerJobBuilder` |
| **Uso conhecido 2** | `AbstractJobHandler.complete`, `update`, `cancel`, `abort` e `error`, com subclasses por tipo de *job* de *runner* |
| **Participantes** | *AbstractClass*: `AbstractJobBuilder`, `AbstractJobHandler`; *ConcreteClass*: construtores e *handlers* concretos; operações primitivas: `createJobs`, `build*Payload`, `specific*` |
| **Consequências** | O algoritmo de orquestração fica em um só lugar e a introdução dos *runners* não duplicou lógica; em troca, a herança fixa o esqueleto, e uma variação que precise alterar a ordem dos passos exige mudar a classe-mãe |

Fonte: elaborado pelo autor (2026).

## 6.4.7 *Strategy*: perfis de codificação

O padrão *Strategy* define uma família de algoritmos, encapsula cada um e os torna
intercambiáveis (GAMMA *et al.*, 1994, cap. 5; ver Listagem 1.2). No PeerTube, a família é a das
**opções de codificação**: dados um vídeo de entrada, uma resolução e uma taxa de quadros, que
parâmetros passar ao ffmpeg? O tipo `EncoderOptionsBuilder` é a interface da estratégia, e cada
função que o implementa é uma estratégia concreta. A Listagem 6.4.12 mostra a tabela padrão, que
associa a cada codificador e modo uma estratégia chamada `default`.

**Listagem 6.4.12** – Código real (TypeScript, PeerTube v8.3.1): tabela de estratégias de codificação padrão.

```typescript
export function getDefaultAvailableEncoders () {
  return {
    vod: {
      libx264: {
        default: defaultX264VODOptionsBuilder
      },
      aac: {
        default: defaultAACOptionsBuilder
      },
      libfdk_aac: {
        default: defaultLibFDKAACVODOptionsBuilder
      }
    },
    live: {
      libx264: {
        default: defaultX264LiveOptionsBuilder
      },
      aac: {
        default: defaultAACOptionsBuilder
      }
    }
  }
}
```

Fonte: PeerTube (2026),
[`packages/ffmpeg/src/ffmpeg-default-transcoding-profile.ts#L75-L97`](https://github.com/Chocobozzz/PeerTube/blob/v8.3.1/packages/ffmpeg/src/ffmpeg-default-transcoding-profile.ts#L75-L97).

A família é **aberta**. A classe `VideoTranscodingProfilesManager` mantém a tabela e oferece os
métodos `addProfile` e `addEncoderPriority`, expostos aos *plugins* como `addVODProfile`,
`addLiveProfile` e `addVODEncoderPriority` (PEERTUBE, 2026,
[`register-helpers.ts#L341-L360`](https://github.com/Chocobozzz/PeerTube/blob/v8.3.1/server/core/lib/plugins/register-helpers.ts#L341-L360)).
Um *plugin* pode registrar um perfil que use um codificador por *hardware* ou um codec mais
eficiente, e o administrador o seleciona na configuração (`transcoding.profile`), sem alterar o
núcleo. É o princípio aberto-fechado realizado por *Strategy*. A estratégia é uma **função**, e
não uma classe: em linguagens com funções de primeira classe, o padrão frequentemente dispensa a
hierarquia de classes do GoF.

Um contraexemplo delimita o conceito. As "estratégias" de redundância (`most-views`, `trending`,
`recently-added`) têm nome de estratégia, mas são selecionadas por uma cadeia de condicionais no
método `findVideoToDuplicate` de `server/core/lib/schedulers/videos-redundancy-scheduler.ts`. São
estratégias no sentido de **política configurável**, e não do padrão de projeto, porque o conjunto
é fechado e acrescentar uma nova exige modificar o método. O analista deve julgar pela estrutura,
e não pelo nome.

**Tabela 6.4.4** – Ficha *Known Uses*: *Strategy* no PeerTube.

| Campo | Conteúdo |
|---|---|
| **Padrão** | *Strategy* (GAMMA *et al.*, 1994, cap. 5) |
| **Intenção** | Definir uma família de algoritmos, encapsular cada um e torná-los intercambiáveis |
| **Uso conhecido** | Perfis de codificação: `getDefaultAvailableEncoders` e `VideoTranscodingProfilesManager` |
| **Participantes** | *Strategy*: tipo `EncoderOptionsBuilder`; *ConcreteStrategy*: `defaultX264VODOptionsBuilder`, `defaultAACOptionsBuilder` e perfis de *plugins*; *Context*: o construtor de comandos ffmpeg |
| **Consequências** | Novos codificadores entram por *plugin*, sem alterar o núcleo; em troca, a escolha depende de nomes de perfil em texto e de prioridades numéricas, verificadas só em tempo de execução |

Fonte: elaborado pelo autor (2026).

## 6.4.8 HLS, manifesto e integridade P2P

A Listagem 6.4.13 mostra as opções que o PeerTube passa ao ffmpeg em toda transcodificação HLS
sob demanda: segmentos de quatro segundos, *playlist* completa do tipo VOD, segmentos em MP4
fragmentado e um único arquivo por resolução (`single_file`), endereçado por intervalos de bytes.

**Listagem 6.4.13** – Código real (TypeScript, PeerTube v8.3.1): opções comuns do ffmpeg para HLS sob demanda.

```typescript
  private addCommonHLSVODCommandOptions (command: FfmpegCommand, outputPath: string) {
    return command.outputOption('-hls_time 4')
      .outputOption('-hls_list_size 0')
      .outputOption('-hls_playlist_type vod')
      .outputOption('-hls_segment_filename ' + outputPath)
      .outputOption('-hls_segment_type fmp4')
      .outputOption('-f hls')
      .outputOption('-hls_flags single_file')
  }
```

Fonte: PeerTube (2026),
[`packages/ffmpeg/src/ffmpeg-vod.ts#L244-L252`](https://github.com/Chocobozzz/PeerTube/blob/v8.3.1/packages/ffmpeg/src/ffmpeg-vod.ts#L244-L252).

Ao fim de cada tarefa de HLS, o servidor reescreve a *playlist* mestre. A Listagem 6.4.14 mostra a
construção de uma linha `EXT-X-STREAM-INF` por resolução, com os atributos `BANDWIDTH`,
`RESOLUTION`, `FRAME-RATE` e `CODECS` definidos pela RFC 8216 (PANTOS; MAY, 2017). É essa linha
que permite ao reprodutor escolher a variante adequada à banda medida.

**Listagem 6.4.14** – Código real (TypeScript, PeerTube v8.3.1): montagem de uma entrada da *playlist* mestre HLS.

```typescript
        const bandwidth = 'BANDWIDTH=' + video.getBandwidthBits(file)
        const resolution = file.resolution === VideoResolution.H_NOVIDEO
          ? ''
          : `,RESOLUTION=${size?.width || 0}x${size?.height || 0}`

        let line = `#EXT-X-STREAM-INF:${bandwidth}${resolution}`
        if (file.fps) line += ',FRAME-RATE=' + file.fps

        const codecs = await Promise.all([
          getVideoStreamCodec(videoFilePath, probe),
          separatedAudioCodec || getAudioStreamCodec(videoFilePath, probe)
        ])

        line += `,CODECS="${codecs.filter(c => !!c).join(',')}"`
```

Fonte: PeerTube (2026),
[`server/core/lib/hls.ts#L119-L132`](https://github.com/Chocobozzz/PeerTube/blob/v8.3.1/server/core/lib/hls.ts#L119-L132).

No mesmo arquivo, a função `updateSha256VODSegments` lê cada *playlist* de resolução, extrai os
intervalos de bytes dos segmentos, calcula o SHA-256 de cada intervalo e grava o resultado em um
JSON publicado ao lado da *playlist* (PEERTUBE, 2026,
[`hls.ts#L181-L230`](https://github.com/Chocobozzz/PeerTube/blob/v8.3.1/server/core/lib/hls.ts#L181-L230)).
No reprodutor, a classe `SegmentValidator` usa esse arquivo: depois de localizar o resumo esperado
para o arquivo e o intervalo de bytes, calcula o SHA-256 dos dados recebidos com a API Web Crypto
e rejeita o segmento se os valores divergirem (Listagem 6.4.15). Se o segmento ainda não constar do
JSON, situação comum em transmissões ao vivo, o validador espera meio segundo, baixa o JSON de novo
e tenta outra vez, até dez vezes.

**Listagem 6.4.15** – Código real (TypeScript, PeerTube v8.3.1): validação do resumo SHA-256 de um segmento recebido.

```typescript
    const calculatedSha = await this.sha256Hex(data)
    if (calculatedSha !== hashShouldBe) {
      logger.clientError(
        `Hashes does not correspond for segment ${filename}/${range} (expected: ${hashShouldBe} instead of ${calculatedSha})`
      )

      return false
    }

    return true
```

Fonte: PeerTube (2026),
[`client/src/standalone/player/src/shared/p2p-media-loader/segment-validator.ts#L73-L82`](https://github.com/Chocobozzz/PeerTube/blob/v8.3.1/client/src/standalone/player/src/shared/p2p-media-loader/segment-validator.ts#L73-L82).

## 6.4.9 Subida de verificação

A subida de verificação parte de um trecho de código e pergunta, a cada altitude, que componente
ele realiza, que decisão esse componente concretiza e que característica essa decisão protege
(Seção 1.11). Duas subidas são propostas para o PeerTube.

**Primeira subida: por que a transcodificação não roda na requisição de *upload*?** No solo, a
resposta está na Listagem 6.4.7: o vídeo é criado em `TO_TRANSCODE`, a miniatura é gerada, o
cliente recebe a resposta e uma sequência de *jobs* é entregue à fila. A 10.000 pés, isso
corresponde à separação entre a cadeia de *middlewares* do *upload* e os *handlers* da fila,
ligados por um conector assíncrono e persistente (Seção 5.4.3). A 50.000 pés, concretiza o
ADR-PT-01 e torna possível o ADR-PT-02: como o processamento já era assíncrono e descrito como dado,
pôde ser levado para *runners* remotos (Seção 4.4.6). A 100.000 pés, protege o desempenho
percebido pelo professor, a disponibilidade da API e o custo, porque o processamento é dimensionado
separadamente (Seção 3.4.7).

**Segunda subida: por que o reprodutor calcula SHA-256 de cada segmento?** No solo, a Listagem
6.4.15. A 10.000 pés, o componente de validação do reprodutor, cooperando com o `p2p-media-loader`
e com o módulo `hls.ts` do servidor (Seção 5.4.4). A 50.000 pés, a decisão de entregar P2P sobre
HLS (ADR-PT-03) e a combinação de cliente-servidor com P2P. A 100.000 pés, o custo de banda, que o
P2P reduz, e a integridade do conteúdo, que o P2P ameaçaria sem a verificação. A Tabela 6.4.5
resume as duas cadeias.

**Tabela 6.4.5** – Subidas de verificação no PeerTube.

| Altitude | Subida 1: transcodificação assíncrona | Subida 2: validação de segmentos |
|---|---|---|
| Solo | `addVideoJobsAfterCreation` enfileira *jobs* (Listagem 6.4.7) | `SegmentValidator.validate` (Listagem 6.4.15) |
| 10.000 pés | Fila BullMQ e *handlers* idempotentes, separados do controlador | Reprodutor com hls.js, `p2p-media-loader` e validador; `hls.ts` no servidor |
| 50.000 pés | ADR-PT-01 e ADR-PT-02: processamento assíncrono e delegável | ADR-PT-03: P2P sobre HLS |
| 100.000 pés | Desempenho percebido, disponibilidade da API, custo | Custo de banda e integridade do conteúdo |

Fonte: elaborado pelo autor (2026).

Nas duas subidas a cadeia se completa, o que indica que os trechos não são acidentais. Se a
transcodificação ocorresse dentro do controlador, a violação seria detectada na primeira subida: a
resposta ao professor dependeria do tamanho do vídeo, e um único envio grande degradaria a API
para todos.

## 6.4.10 Um *pipeline* de filtros em Java

As seções anteriores mostraram o estilo *pipes and filters* em um sistema real, com filas, estados
e protocolos. Esta seção o reduz à sua essência, em Java 17, como preparação para o exercício com
correção automática deste par, descrito em `atividades/README.md`: um *pipeline* de transcodificação
em que **cada filtro depende apenas da interface `Filtro`**, verificado por ArchUnit.

A Listagem 6.4.16 define o contrato. `Tarefa` é a mensagem que trafega nos canos e é imutável.
`Filtro` é a interface que todos os filtros implementam e corresponde ao "requisito derivado" da
Seção 4.4.3. `Pipeline` é o conector: conhece apenas a interface e encadeia os filtros na ordem
dada. Os filtros concretos não se conhecem.

**Listagem 6.4.16** – Modelo didático em Java: *Pipeline* de transcodificação.

```java
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// Mensagem que trafega nos canos: imutável e pequena (referência ao arquivo, não o conteúdo).
public record Tarefa(String videoId, String caminho, int resolucao, List<String> artefatos) {
    public Tarefa comArtefato(String artefato) {
        var novos = new ArrayList<>(artefatos);
        novos.add(artefato);
        return new Tarefa(videoId, caminho, resolucao, List.copyOf(novos));
    }
}

// Contrato único entre os filtros.
public interface Filtro {
    // Optional.empty() significa que um filtro testador descartou a tarefa.
    Optional<Tarefa> processar(Tarefa entrada);
}

// Testador: só deixa passar vídeos com resolução conhecida.
public final class FiltroValidacao implements Filtro {
    @Override public Optional<Tarefa> processar(Tarefa t) {
        return t.resolucao() > 0 ? Optional.of(t) : Optional.empty();
    }
}

// Transformador: gera a versão em 480p (a chamada ao ffmpeg é omitida).
public final class FiltroTranscodifica480p implements Filtro {
    @Override public Optional<Tarefa> processar(Tarefa t) {
        if (t.resolucao() < 480) return Optional.of(t);      // nada a fazer
        return Optional.of(t.comArtefato(t.videoId() + "-480p.mp4"));
    }
}

// Transformador: gera o manifesto a partir dos artefatos já produzidos.
public final class FiltroManifesto implements Filtro {
    @Override public Optional<Tarefa> processar(Tarefa t) {
        return Optional.of(t.comArtefato(t.videoId() + "-master.m3u8"));
    }
}

// Conector: depende só da abstração. Trocar, remover ou reordenar filtros não altera esta classe.
public final class Pipeline {
    private final List<Filtro> filtros;

    public Pipeline(List<Filtro> filtros) { this.filtros = List.copyOf(filtros); }

    public Optional<Tarefa> executar(Tarefa inicial) {
        Optional<Tarefa> atual = Optional.of(inicial);
        for (Filtro f : filtros) {
            atual = atual.flatMap(f::processar);  // um filtro testador pode interromper o fluxo
        }
        return atual;
    }
}
```

Fonte: elaborado pelo autor (2026).

A Listagem 6.4.17 mostra uma função de aptidão (*fitness function*; ver Apêndice E) em ArchUnit
que verifica a regra central do exercício: nenhuma classe que implementa `Filtro` pode depender de
outra implementação de `Filtro`. A regra impede que um filtro chame diretamente o próximo, o que
transformaria o *pipeline* em uma cadeia rígida de chamadas e eliminaria a possibilidade de
reordenar, substituir ou distribuir os filtros.

**Listagem 6.4.17** – Modelo didático em Java (ArchUnit): filtros não se conhecem.

```java
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "br.edu.ifal.video", importOptions = ImportOption.DoNotIncludeTests.class)
class PipelineArquiteturaTest {

    // Um filtro só pode conhecer a interface Filtro e a mensagem Tarefa, nunca outro filtro.
    @ArchTest
    static final ArchRule filtrosNaoDependemUnsDosOutros =
        noClasses().that().implement(Filtro.class)
            .should().dependOnClassesThat().implement(Filtro.class)
            .allowEmptyShould(true)
            .because("em pipes and filters, os filtros só se conhecem pelo formato da mensagem");
}
```

Fonte: elaborado pelo autor (2026).

Funções de aptidão capturam aproximações das regras arquiteturais. A regra acima não impede, por
exemplo, que um filtro dependa de uma classe utilitária que, por sua vez, chame outro filtro.
Calibrar a regra, acrescentando restrições sobre dependências transitivas, faz parte do exercício.

## Exercícios

1. Explique por que `buildNextVideoState` (Listagem 6.4.1) e `setNewStateAndPublishedAt`
   (Listagem 6.4.2) estão em arquivos diferentes. Que critério de Fowler (2002) separa uma regra
   do *Active Record* de um *Transaction Script*?
2. A Listagem 6.4.6 contém uma possível condição de corrida. Descreva o cenário em que ela se
   manifestaria e proponha uma correção com uma operação atômica do Redis.
3. Localize, na *tag* `v8.3.1`, a cadeia completa de *middlewares* da rota legada de *upload*
   (`/upload`, em `upload.ts`) e classifique cada elo como autenticação, *parsing*, validação ou
   controlador. Compare-a com a da Listagem 6.4.3.
4. Suponha que seja preciso enviar *jobs* de transcodificação a um terceiro destino, um serviço de
   nuvem. Mostre, em pseudocódigo, que classe nova seria criada e que método da Listagem 6.4.9
   mudaria. O que aconteceria se o *Template Method* não tivesse sido usado?
5. As "estratégias" de redundância não implementam o padrão *Strategy* (Seção 6.4.7). Proponha uma
   refatoração que as transforme em estratégias no sentido do GoF e discuta se o benefício
   compensa o custo.
6. Compare a verificação de integridade dos segmentos P2P (Listagem 6.4.15) com a ausência de
   verificação dos resultados dos *runners* (Seção 4.4.8). Proponha um mecanismo que reduza o
   risco de um *runner* malicioso e estime o seu custo.
7. **Atividade com correção automática:** implemente um *pipeline* de transcodificação no estilo
   *pipes and filters* em que cada filtro dependa apenas da interface `Filtro` (Listagem 6.4.16). A
   correção verifica o comportamento com JUnit e a regra de que nenhum filtro depende de outro com
   ArchUnit (Listagem 6.4.17).

## Termos

`Idempotência`
:   Propriedade de uma operação cujo efeito é o mesmo quando executada uma ou várias vezes.

`Manifesto`
:   Arquivo que descreve as variantes e os segmentos de um vídeo para o reprodutor, como a
    *playlist* mestre do HLS ou o MPD do DASH.

`Pipes and filters`
:   Estilo em que dados fluem por uma sequência de filtros independentes conectados por canais
    unidirecionais.

`Runner`
:   No PeerTube, programa externo que pede *jobs* pesados à instância, executa-os e devolve o
    resultado.

`Transcodificação`
:   Conversão de um vídeo para outros formatos, resoluções e taxas de bits.

`Upload retomável`
:   Envio de arquivo em partes, em que o cliente pode consultar o que já foi recebido e continuar
    depois de uma interrupção.

## Referências

BULLMQ. **BullMQ documentation**: prioritized jobs; flows. 2026. Disponível em:
<https://docs.bullmq.io>. Acesso em: 18 out. 2026.

EVANS, E. **Domain-driven design**: tackling complexity in the heart of software. Boston:
Addison-Wesley, 2003.

FOWLER, M. **Patterns of enterprise application architecture**. Boston: Addison-Wesley, 2002.

GAMMA, E. *et al.* **Design patterns**: elements of reusable object-oriented software. Reading:
Addison-Wesley, 1994.

HOHPE, G.; WOOLF, B. **Enterprise integration patterns**: designing, building, and deploying
messaging solutions. Boston: Addison-Wesley, 2003.

PANTOS, R. (ed.); MAY, W. **HTTP Live Streaming**. RFC 8216. [S. l.]: Internet Engineering Task
Force, ago. 2017. Disponível em: <https://www.rfc-editor.org/rfc/rfc8216>. Acesso em: 18 out.
2026.

PEERTUBE. **Chocobozzz/PeerTube**: repositório de código-fonte, versão v8.3.1. GitHub, 2026.
Disponível em: <https://github.com/Chocobozzz/PeerTube/tree/v8.3.1>. Acesso em: 18 out. 2026.
Documentação disponível em: <https://docs.joinpeertube.org>.
