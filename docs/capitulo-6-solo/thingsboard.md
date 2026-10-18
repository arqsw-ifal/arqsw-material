# 6.7 ThingsBoard: o código

<span class="altitude solo">Solo</span>

## 6.7.1 Versão estudada e mapa do repositório

Todos os trechos desta seção são literais e referem-se à *tag* `v4.3.1.6` do repositório
`thingsboard/thingsboard`, a última da série distribuída sob licença Apache 2.0 na data de redação
(ver [Seção 2.7](../capitulo-2-aplicacoes/thingsboard.md)). Os *permalinks* apontam para essa
*tag*, e os números de linha foram conferidos no arquivo bruto correspondente. A Tabela 6.7.1
relaciona os conceitos das altitudes superiores aos caminhos do repositório.

**Tabela 6.7.1** – Mapa entre conceitos e código no ThingsBoard v4.3.1.6.

| Conceito | Caminho |
|---|---|
| Interface dos nós de regra | `rule-engine/rule-engine-api/src/main/java/org/thingsboard/rule/engine/api/TbNode.java` |
| Contexto e anotação dos nós | `rule-engine/rule-engine-api/.../api/TbContext.java`, `.../api/RuleNode.java` |
| Nós concretos | `rule-engine/rule-engine-components/.../filter/TbMsgTypeFilterNode.java`, `.../filter/TbMsgTypeSwitchNode.java`, `.../telemetry/TbMsgTimeseriesNode.java` |
| Roteamento na cadeia | `application/.../actors/ruleChain/RuleChainActorMessageProcessor.java` |
| Sistema de atores | `common/actor/.../actors/` (`TbActor`, `TbActorMailbox`, `TbActorSystem`) e `application/.../actors/` |
| Transportes e adaptadores | `common/transport/{mqtt,coap,http,lwm2m,snmp}`; `common/transport/mqtt/.../adaptors/` |
| Serviço de transporte e limites | `common/transport/transport-api/.../service/DefaultTransportService.java`, `.../limits/TransportRateLimitService.java` |
| Abstração de fila | `common/cluster-api/.../queue/TbQueueProducer.java`, `TbQueueConsumer.java` |
| Consumo do motor de regras | `application/.../service/queue/ruleengine/TbRuleEngineQueueConsumerManager.java` |
| Estratégias de fila | `application/.../service/queue/processing/` |
| Modelo de dados e mensagem | `common/data/.../Device.java`; `common/message/.../TbMsg.java` |
| Acesso a dados | `dao/.../device/DeviceServiceImpl.java`, `dao/.../model/sql/AbstractDeviceEntity.java`, `dao/.../sql/device/JpaDeviceDao.java` |

Fonte: elaborado pelo autor (2026), a partir da árvore do repositório na *tag* v4.3.1.6.

## 6.7.2 O modelo de domínio

O modelo de entidades do ThingsBoard vive no módulo `common/data`, compartilhado por todos os
demais módulos (ver o "núcleo compartilhado" da [Seção 5.7](../capitulo-5-10-mil-pes/thingsboard.md)).
A Listagem 6.7.1 mostra o início da classe `Device`.

**Listagem 6.7.1** – Início da classe `Device`.

```java
public class Device extends BaseDataWithAdditionalInfo<DeviceId> implements HasLabel, HasTenantId, HasCustomerId, HasOtaPackage, HasVersion, ExportableEntity<DeviceId> {

    private static final long serialVersionUID = 2807343040519543363L;

    private TenantId tenantId;
    private CustomerId customerId;
    @NoXss
    @Length(fieldName = "name")
    private String name;
    @NoXss
    @Length(fieldName = "type")
    private String type;
    @NoXss
    @Length(fieldName = "label")
    private String label;
    private DeviceProfileId deviceProfileId;
```

Fonte: ThingsBoard, *tag* v4.3.1.6,
[`common/data/src/main/java/org/thingsboard/server/common/data/Device.java#L32-L47`](https://github.com/thingsboard/thingsboard/blob/v4.3.1.6/common/data/src/main/java/org/thingsboard/server/common/data/Device.java#L32-L47).

Três características do modelo merecem registro. A primeira é o uso de **identificadores tipados**:
`DeviceId`, `TenantId`, `CustomerId` e `DeviceProfileId` são classes distintas que envolvem um
UUID, de modo que o compilador impede passar o identificador de um inquilino onde se espera o de
um dispositivo. Na terminologia de Evans (2003, cap. 5), são **objetos de valor**: definidos pelos
atributos, sem identidade própria. A segunda é a presença do `tenantId` em toda entidade relevante,
expressa pela interface `HasTenantId`, que materializa no modelo o isolamento lógico entre
inquilinos discutido na [Seção 3.7](../capitulo-3-100-mil-pes/thingsboard.md). A terceira é que
`Device` é, essencialmente, uma estrutura de dados com validações declarativas (`@NoXss`,
`@Length`), e as regras de negócio sobre dispositivos ficam nos serviços. Fowler (2002) chamaria o
estilo resultante de ***Transaction Script*** organizado por uma ***Service Layer***, e não de
***Domain Model***. Argumenta-se aqui que a escolha é coerente com o problema: a maior parte da
"lógica de negócio" de uma plataforma de IoT é configurada pelos usuários em cadeias de regras, e
não codificada em entidades.

A mensagem que circula pelo motor de regras é a classe `TbMsg`, cujo início está na Listagem 6.7.2.

**Listagem 6.7.2** – Campos da mensagem interna do motor de regras.

```java
public final class TbMsg implements Serializable {

    public static final String EMPTY_JSON_OBJECT = "{}";
    public static final String EMPTY_JSON_ARRAY = "[]";
    public static final String EMPTY_STRING = "";

    private final String queueName;
    private final UUID id;
    private final long ts;
    private final String type;
    private final TbMsgType internalType;
    private final EntityId originator;
    private final CustomerId customerId;
    private final TbMsgMetaData metaData;
    private final TbMsgDataType dataType;
    private final String data;
    private final RuleChainId ruleChainId;
    private final RuleNodeId ruleNodeId;
```

Fonte: ThingsBoard, *tag* v4.3.1.6,
[`common/message/src/main/java/org/thingsboard/server/common/msg/TbMsg.java#L33-L50`](https://github.com/thingsboard/thingsboard/blob/v4.3.1.6/common/message/src/main/java/org/thingsboard/server/common/msg/TbMsg.java#L33-L50).

A classe é `final` e todos os campos são `final`: a mensagem é **imutável**. Isso é condição para o
modelo de atores, porque a mesma instância pode ser lida por vários atores sem sincronização. Uma
"alteração" produz uma nova mensagem por meio de um construtor fluente (padrão *Builder*), que
começa em `msg.copy()`, encadeia chamadas como `.id(UUID.randomUUID())` e `.ruleChainId(entityId)` e
termina em `.build()`, como no método `putToQueue` usado no roteamento da cadeia
([`RuleChainActorMessageProcessor.java#L333-L350`](https://github.com/thingsboard/thingsboard/blob/v4.3.1.6/application/src/main/java/org/thingsboard/server/actors/ruleChain/RuleChainActorMessageProcessor.java#L333-L350)). O campo `originator` identifica a entidade de origem e é a chave de partição
discutida na [Seção 4.7](../capitulo-4-50-mil-pes/thingsboard.md).

## 6.7.3 DDD tático: agregados e invariantes

O projeto não adota DDD explicitamente, mas o código permite identificar fronteiras de
consistência no sentido de Evans (2003, cap. 6), para quem um **agregado** é um conjunto de objetos
tratado como unidade para mudanças de dados, com uma raiz que controla o acesso. A identificação a
seguir é uma **interpretação do autor**.

O par **dispositivo e credenciais** comporta-se como um agregado. O método
`saveDeviceWithCredentials` de `DeviceServiceImpl` é anotado com `@Transactional`
([`DeviceServiceImpl.java#L182-L186`](https://github.com/thingsboard/thingsboard/blob/v4.3.1.6/dao/src/main/java/org/thingsboard/server/dao/device/DeviceServiceImpl.java#L182-L186)),
de modo que o dispositivo e suas credenciais são gravados ou descartados juntos. A invariante
protegida é que nenhum dispositivo exista sem um meio de autenticação, e nenhuma credencial exista
sem dispositivo, requisito diretamente ligado à segurança por dispositivo discutida na Seção 3.7.
A **cadeia de regras**, com seus nós e relações, é outro candidato: o `RuleChainActorMessageProcessor`
mantém as rotas de todos os nós da cadeia em um único mapa (`nodeRoutes`) e o reconstrói por inteiro
quando a cadeia muda, tratando-a como unidade. Já a **telemetria** não pertence ao agregado do
dispositivo: é gravada fora de transações de entidades, em outro banco, por um nó de regra. Essa
separação é a tradução tática do ADR 4.7-C, que separou entidades e séries temporais.

## 6.7.4 Padrões GoF: *Strategy* e *Chain of Responsibility* no motor de regras

A Listagem 6.7.3 reproduz o início da interface que todo nó de regra implementa.

**Listagem 6.7.3** – Início da interface `TbNode`.

```java
public interface TbNode {

    void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException;

    void onMsg(TbContext ctx, TbMsg msg) throws ExecutionException, InterruptedException, TbNodeException;

    default void destroy() {
    }

    default void onPartitionChangeMsg(TbContext ctx, PartitionChangeMsg msg) {
    }
```

Fonte: ThingsBoard, *tag* v4.3.1.6,
[`rule-engine/rule-engine-api/src/main/java/org/thingsboard/rule/engine/api/TbNode.java#L15-L25`](https://github.com/thingsboard/thingsboard/blob/v4.3.1.6/rule-engine/rule-engine-api/src/main/java/org/thingsboard/rule/engine/api/TbNode.java#L15-L25).

O nó não guarda referência ao próximo nó. Ele declara a **relação** pela qual a mensagem sai,
chamando `ctx.tellNext(msg, relationType)` ou `ctx.tellSuccess(msg)` (`TbContext.java`, linhas 103
a 121), e os nós são descobertos pela anotação `@RuleNode`, que declara tipo, nome, classe de
configuração e relações de saída, com padrão `Success` e `Failure`
([`RuleNode.java#L16-L40`](https://github.com/thingsboard/thingsboard/blob/v4.3.1.6/rule-engine/rule-engine-api/src/main/java/org/thingsboard/rule/engine/api/RuleNode.java#L16-L40)).
A Listagem 6.7.4 mostra um nó concreto.

**Listagem 6.7.4** – O nó `TbMsgTypeFilterNode`.

```java
public class TbMsgTypeFilterNode implements TbNode {

    private TbMsgTypeFilterNodeConfiguration config;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        config = TbNodeUtils.convert(configuration, TbMsgTypeFilterNodeConfiguration.class);
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        ctx.tellNext(msg, config.getMessageTypes().contains(msg.getType()) ? TbNodeConnectionType.TRUE : TbNodeConnectionType.FALSE);
    }

}
```

Fonte: ThingsBoard, *tag* v4.3.1.6,
[`rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/filter/TbMsgTypeFilterNode.java#L26-L40`](https://github.com/thingsboard/thingsboard/blob/v4.3.1.6/rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/filter/TbMsgTypeFilterNode.java#L26-L40).

Quando um nó chama `tellNext`, o ator da cadeia resolve as relações de saída. A Listagem 6.7.5
mostra o núcleo dessa decisão.

**Listagem 6.7.5** – Resolução das relações de saída na cadeia de regras.

```java
            List<RuleNodeRelation> relationsByTypes = ruleNodeRelations.stream()
                    .filter(r -> contains(relationTypes, r.getType()))
                    .collect(Collectors.toList());
            int relationsCount = relationsByTypes.size();
            if (relationsCount == 0) {
                log.trace("[{}][{}][{}] No outbound relations to process", tenantId, entityId, msg.getId());
                if (relationTypes.contains(TbNodeConnectionType.FAILURE)) {
                    RuleNodeCtx ruleNodeCtx = nodeActors.get(originatorNodeId);
                    if (ruleNodeCtx != null) {
                        msg.getCallback().onFailure(new RuleNodeException(failureMessage, ruleChainName, ruleNodeCtx.getSelf()));
                    } else {
                        log.debug("[{}] Failure during message processing by Rule Node [{}]. Enable and see debug events for more info", entityId, originatorNodeId.getId());
                        msg.getCallback().onFailure(new RuleEngineException("Failure during message processing by Rule Node [" + originatorNodeId.getId().toString() + "]"));
                    }
                } else {
                    msg.getCallback().onSuccess();
                }
            } else if (relationsCount == 1) {
                for (RuleNodeRelation relation : relationsByTypes) {
                    log.trace("[{}][{}][{}] Pushing message to single target: [{}]", tenantId, entityId, msg.getId(), relation.getOut());
                    pushToTarget(tpi, msg, relation.getOut(), relation.getType());
                }
            } else {
                MultipleTbQueueTbMsgCallbackWrapper callbackWrapper = new MultipleTbQueueTbMsgCallbackWrapper(relationsCount, msg.getCallback());
                log.trace("[{}][{}][{}] Pushing message to multiple targets: [{}]", tenantId, entityId, msg.getId(), relationsByTypes);
                for (RuleNodeRelation relation : relationsByTypes) {
                    EntityId target = relation.getOut();
                    putToQueue(tpi, msg, callbackWrapper, target);
                }
            }
```

Fonte: ThingsBoard, *tag* v4.3.1.6,
[`application/src/main/java/org/thingsboard/server/actors/ruleChain/RuleChainActorMessageProcessor.java#L295-L324`](https://github.com/thingsboard/thingsboard/blob/v4.3.1.6/application/src/main/java/org/thingsboard/server/actors/ruleChain/RuleChainActorMessageProcessor.java#L295-L324).

Há três casos. Sem relações correspondentes, a cadeia termina para aquela mensagem, com falha se a
saída era `Failure` e sucesso caso contrário; uma falha não conectada vira falha do lote, que a
estratégia de processamento da fila decidirá se reprocessa. Com uma relação, a mensagem segue ao
destino. Com várias, a mensagem é recolocada na fila, uma cópia por destino, e um *callback*
composto só considera a original concluída quando todas as cópias terminam. O grafo é, portanto,
um *Chain of Responsibility* com ramificação, cuja conclusão é uma barreira de sincronização; cada
ramificação aumenta o tráfego interno da fila.

**Ficha *Known Uses* – *Strategy*.** *Intenção* (GAMMA *et al.*, 1994, cap. 5): definir uma família
de algoritmos, encapsular cada um e torná-los intercambiáveis. *Participantes no ThingsBoard*: a
estratégia é `TbNode`; as estratégias concretas são os nós de `rule-engine-components`, como
`TbMsgTypeFilterNode` e `TbMsgTimeseriesNode`; o contexto é o `RuleNodeActorMessageProcessor`, que mantém
uma referência a `TbNode` e invoca `onMsg`. *Segundo uso*: `TbRuleEngineSubmitStrategy` e
`TbRuleEngineProcessingStrategy`, selecionadas por fábricas a partir da configuração de cada fila
(Seção 6.7.6). *Consequências observadas*: novos nós sem alterar o motor (princípio aberto-fechado);
custo de muitas classes pequenas e de configuração em JSON.

**Ficha *Known Uses* – *Chain of Responsibility*.** *Intenção* (GAMMA *et al.*, 1994, cap. 5):
evitar acoplar o emissor de uma requisição ao seu receptor, dando a mais de um objeto a chance de
tratá-la; Shvets (2022) acrescenta que cada *handler* decide se processa o pedido ou o passa
adiante. *Participantes no ThingsBoard*: os *handlers* são os nós; o encadeamento não está nos nós,
mas nas relações da cadeia, resolvidas por `RuleChainActorMessageProcessor`. *Variação*: um
*handler* pode ter vários sucessores (Listagem 6.7.5). *Consequências observadas*: cadeias
alteráveis em tempo de execução pela interface; processamento distribuído entre atores;
depuração mais difícil, mitigada por eventos de depuração por nó.

## 6.7.5 Padrão GoF: *Adapter* nos transportes

A Listagem 6.7.6 mostra o início da interface de adaptação do transporte MQTT.

**Listagem 6.7.6** – Início da interface `MqttTransportAdaptor`.

```java
public interface MqttTransportAdaptor {

    ByteBufAllocator ALLOCATOR = new UnpooledByteBufAllocator(false);

    PostTelemetryMsg convertToPostTelemetry(MqttDeviceAwareSessionContext ctx, MqttPublishMessage inbound) throws AdaptorException;

    PostAttributeMsg convertToPostAttributes(MqttDeviceAwareSessionContext ctx, MqttPublishMessage inbound) throws AdaptorException;
```

Fonte: ThingsBoard, *tag* v4.3.1.6,
[`common/transport/mqtt/src/main/java/org/thingsboard/server/transport/mqtt/adaptors/MqttTransportAdaptor.java#L31-L37`](https://github.com/thingsboard/thingsboard/blob/v4.3.1.6/common/transport/mqtt/src/main/java/org/thingsboard/server/transport/mqtt/adaptors/MqttTransportAdaptor.java#L31-L37).

A Listagem 6.7.7 mostra o uso no tratador MQTT. O último argumento é o *callback* que enviará o
`PUBACK`.

**Listagem 6.7.7** – O tratador MQTT adapta a mensagem e a entrega ao serviço de transporte.

```java
            } else if (topicName.equals(MqttTopics.DEVICE_TELEMETRY_SHORT_TOPIC)) {
                TransportProtos.PostTelemetryMsg postTelemetryMsg = payloadAdaptor.convertToPostTelemetry(deviceSessionCtx, mqttMsg);
                transportService.process(deviceSessionCtx.getSessionInfo(), postTelemetryMsg, getMetadata(deviceSessionCtx, topicName),
                        getPubAckCallback(ctx, msgId, postTelemetryMsg));
```

Fonte: ThingsBoard, *tag* v4.3.1.6,
[`common/transport/mqtt/src/main/java/org/thingsboard/server/transport/mqtt/MqttTransportHandler.java#L578-L581`](https://github.com/thingsboard/thingsboard/blob/v4.3.1.6/common/transport/mqtt/src/main/java/org/thingsboard/server/transport/mqtt/MqttTransportHandler.java#L578-L581).

O *callback* de `getPubAckCallback` envia o `PUBACK` em `onSuccess` e encerra a conexão em `onError`
([`MqttTransportHandler.java#L710-L724`](https://github.com/thingsboard/thingsboard/blob/v4.3.1.6/common/transport/mqtt/src/main/java/org/thingsboard/server/transport/mqtt/MqttTransportHandler.java#L710-L724)),
e o `DefaultTransportService` só o invoca quando a fila confirma o recebimento
([`DefaultTransportService.java#L1142-L1159`](https://github.com/thingsboard/thingsboard/blob/v4.3.1.6/common/transport/transport-api/src/main/java/org/thingsboard/server/common/transport/service/DefaultTransportService.java#L1142-L1159)).
O mesmo arquivo do tratador declara, na linha 133, `MAX_SUPPORTED_QOS_LVL = AT_LEAST_ONCE`, a
limitação a QoS 1 discutida na [Seção 3.7](../capitulo-3-100-mil-pes/thingsboard.md).

**Ficha *Known Uses* – *Adapter*.** *Intenção* (GAMMA *et al.*, 1994, cap. 4): converter a interface
de uma classe em outra interface esperada pelos clientes. *Participantes no ThingsBoard*: o alvo é o
contrato de `TransportService`, baseado em `TransportProtos`; os adaptados são as mensagens do Netty
(`MqttPublishMessage`) e do Californium (`Request`); os adaptadores são `JsonMqttAdaptor`,
`ProtoMqttAdaptor` e, no CoAP, as implementações de `CoapTransportAdaptor`, cujo método
`convertToPostTelemetry` devolve o mesmo `TransportProtos.PostTelemetryMsg`
([`CoapTransportAdaptor.java#L15-L17`](https://github.com/thingsboard/thingsboard/blob/v4.3.1.6/common/transport/coap/src/main/java/org/thingsboard/server/transport/coap/adaptors/CoapTransportAdaptor.java#L15-L17)).
*Consequências observadas*: um novo formato de carga útil exige apenas um novo adaptador; o restante
da plataforma ignora protocolos.

## 6.7.6 Atores e filas no código

A Listagem 6.7.8 reproduz o laço de processamento da caixa de correio de um ator.

**Listagem 6.7.8** – O laço de processamento da caixa de correio.

```java
    private void processMailbox() {
        boolean noMoreElements = false;
        for (int i = 0; i < settings.getActorThroughput(); i++) {
            TbActorMsg msg = highPriorityMsgs.poll();
            if (msg == null) {
                msg = normalPriorityMsgs.poll();
            }
            if (msg != null) {
                try {
                    log.trace("[{}] Going to process message: {}", selfId, msg);
                    actor.process(msg);
                } catch (TbRuleNodeUpdateException updateException) {
                    stopReason = TbActorStopReason.INIT_FAILED;
                    destroy(updateException.getCause());
                } catch (Throwable t) {
                    log.debug("[{}] Failed to process message: {}", selfId, msg, t);
                    ProcessFailureStrategy strategy = actor.onProcessFailure(msg, t);
                    if (strategy.isStop()) {
                        system.stop(selfId);
                    }
                }
            } else {
                noMoreElements = true;
                break;
            }
        }
        if (noMoreElements) {
            busy.set(FREE);
            dispatcher.getExecutor().execute(() -> tryProcessQueue(false));
        } else {
            dispatcher.getExecutor().execute(this::processMailbox);
        }
    }
```

Fonte: ThingsBoard, *tag* v4.3.1.6,
[`common/actor/src/main/java/org/thingsboard/server/actors/TbActorMailbox.java#L135-L167`](https://github.com/thingsboard/thingsboard/blob/v4.3.1.6/common/actor/src/main/java/org/thingsboard/server/actors/TbActorMailbox.java#L135-L167).

A exclusão mútua é garantida por um `AtomicBoolean busy`: o laço só é agendado quando
`busy.compareAndSet(FREE, BUSY)` tem sucesso (linhas 119 a 133). Há duas filas, de prioridade alta
e normal, e o limite `getActorThroughput()` faz o ator devolver a *thread* depois de algumas
mensagens, mesmo que haja outras; o padrão é 5, definido na linha 526 de
[`thingsboard.yml`](https://github.com/thingsboard/thingsboard/blob/v4.3.1.6/application/src/main/resources/thingsboard.yml#L526).
É um mecanismo de justiça: um dispositivo que inunda sua caixa de correio não monopoliza o
*dispatcher*. O ator de inquilino cria atores de dispositivo sob demanda, com
`ctx.getOrCreateChildActor(...)`
([`TenantActor.java#L355-L360`](https://github.com/thingsboard/thingsboard/blob/v4.3.1.6/application/src/main/java/org/thingsboard/server/actors/tenant/TenantActor.java#L355-L360)),
e o `DeviceActor` despacha cada tipo de mensagem com um `switch` sobre `msg.getMsgType()`
([`DeviceActor.java#L45-L85`](https://github.com/thingsboard/thingsboard/blob/v4.3.1.6/application/src/main/java/org/thingsboard/server/actors/device/DeviceActor.java#L45-L85)).
Esse `switch` é defensável: o conjunto de tipos é fechado e pertence ao próprio ator, e cada caso
já delega a um processador.

A Listagem 6.7.9 mostra o consumo de um lote pelo motor de regras.

**Listagem 6.7.9** – Processamento de um lote pelo consumidor do motor de regras.

```java
        TbRuleEngineSubmitStrategy submitStrategy = getSubmitStrategy(queue);
        TbRuleEngineProcessingStrategy ackStrategy = getProcessingStrategy(queue);
        submitStrategy.init(msgs);
        while (!stopped && !consumer.isStopped()) {
            TbMsgPackProcessingContext packCtx = new TbMsgPackProcessingContext(queue.getName(), submitStrategy, ackStrategy.isSkipTimeoutMsgs());
            submitStrategy.submitAttempt((id, msg) -> submitMessage(packCtx, id, msg));

            final boolean timeout = !packCtx.await(queue.getPackProcessingTimeout(), TimeUnit.MILLISECONDS);

            TbRuleEngineProcessingResult result = new TbRuleEngineProcessingResult(queue.getName(), timeout, packCtx);
            if (timeout) {
                printFirstOrAll(packCtx, packCtx.getPendingMap(), "Timeout");
            }
            if (!packCtx.getFailedMap().isEmpty()) {
                printFirstOrAll(packCtx, packCtx.getFailedMap(), "Failed");
            }
            packCtx.printProfilerStats();

            TbRuleEngineProcessingDecision decision = ackStrategy.analyze(result);
            if (ctx.isStatsEnabled()) {
                stats.log(result, decision.isCommit());
            }

            packCtx.cleanup();

            if (decision.isCommit()) {
                submitStrategy.stop();
                consumer.commit();
                break;
            } else {
                submitStrategy.update(decision.getReprocessMap());
            }
        }
```

Fonte: ThingsBoard, *tag* v4.3.1.6,
[`application/src/main/java/org/thingsboard/server/service/queue/ruleengine/TbRuleEngineQueueConsumerManager.java#L120-L153`](https://github.com/thingsboard/thingsboard/blob/v4.3.1.6/application/src/main/java/org/thingsboard/server/service/queue/ruleengine/TbRuleEngineQueueConsumerManager.java#L120-L153).

Enquanto o lote não é confirmado, o próximo não é lido: é isso que transforma lentidão do motor de
regras em atraso na fila, e não em perda ou estouro de memória. As estratégias vêm de fábricas que
fazem `switch` sobre o tipo configurado, como `TbRuleEngineProcessingStrategyFactory`, que cria
`SkipStrategy` ou `RetryStrategy` com parâmetros diferentes
([`TbRuleEngineProcessingStrategyFactory.java#L23-L40`](https://github.com/thingsboard/thingsboard/blob/v4.3.1.6/application/src/main/java/org/thingsboard/server/service/queue/processing/TbRuleEngineProcessingStrategyFactory.java#L23-L40)).
Confinado a uma fábrica, o `switch` isola a única decisão que depende do tipo concreto. A estratégia
de reprocessamento contém, nas linhas 117 a 126 do mesmo arquivo, uma pausa que dobra a cada
tentativa até um teto: *backoff* exponencial **sem** *jitter*, aceitável para um único consumidor,
mas inadequado para uma frota, como mostrou a caixa-preta da [Seção 4.7](../capitulo-4-50-mil-pes/thingsboard.md).

## 6.7.7 Padrões de Fowler e o *proxy* do Spring

**Ficha *Known Uses* – *Data Mapper*.** *Intenção* (FOWLER, 2002): uma camada de mapeadores que move
dados entre objetos e banco, mantendo-os independentes entre si. *No ThingsBoard*: a classe de dados
`Device` não conhece o banco; a entidade JPA `DeviceEntity`, derivada de `AbstractDeviceEntity`,
conhece as colunas e converte-se em `Device` pelo método `toDevice`, cujo início está na Listagem
6.7.10; `JpaDeviceDao` usa essa conversão sobre um repositório Spring Data.

**Listagem 6.7.10** – Mapeamento da entidade JPA para a classe de dados.

```java
    protected Device toDevice() {
        Device device = new Device(new DeviceId(getUuid()));
        device.setCreatedTime(createdTime);
        device.setVersion(version);
        if (tenantId != null) {
            device.setTenantId(TenantId.fromUUID(tenantId));
        }
        if (customerId != null) {
            device.setCustomerId(new CustomerId(customerId));
        }
```

Fonte: ThingsBoard, *tag* v4.3.1.6,
[`dao/src/main/java/org/thingsboard/server/dao/model/sql/AbstractDeviceEntity.java#L115-L124`](https://github.com/thingsboard/thingsboard/blob/v4.3.1.6/dao/src/main/java/org/thingsboard/server/dao/model/sql/AbstractDeviceEntity.java#L115-L124).

**Ficha *Known Uses* – *Service Layer*.** *Intenção* (FOWLER, 2002): definir a fronteira da aplicação
com uma camada de serviços que estabelece as operações disponíveis e coordena a resposta a cada uma.
*No ThingsBoard*: `DeviceServiceImpl`, anotada com `@Service`, oferece operações como
`saveDeviceWithCredentials` e demarca transações com `@Transactional`.

**O *proxy* transacional.** O mecanismo do Spring é um ***Proxy*** no sentido de Gamma *et al.*
(1994, cap. 4): o objeto injetado em outros componentes não é a instância de `DeviceServiceImpl`,
mas um representante que abre a transação, delega a chamada e a confirma ou desfaz. Daí decorrem
duas regras práticas válidas para qualquer projeto Spring: uma chamada entre métodos **da mesma
classe** não passa pelo *proxy* e não ganha transação própria; e a anotação só tem efeito sobre
métodos invocados por meio do *bean* gerenciado. Arquiteturalmente, as transações vivem na camada
de entidades, enquanto o caminho da telemetria depende de fila, confirmação e idempotência: dois
modelos de consistência, cada um onde o perfil de acesso o justifica.

## 6.7.8 Subida de verificação

A subida parte de um trecho de código e pergunta que componente ele realiza, que decisão o justifica
e que característica arquitetural essa decisão protege (Seção 1.11). A pergunta é: **como o código
isola um inquilino "barulhento" dos demais?**

**Listagem 6.7.11** – Verificação de limites de taxa no serviço de transporte.

```java
        var rateLimitedPair = rateLimitService.checkLimits(tenantId, gatewayId, deviceId, dataPoints, isGateway);
        if (rateLimitedPair == null) {
            return true;
        } else {
            var rateLimitedEntityType = rateLimitedPair.getFirst();
            if (callback != null) {
                callback.onError(new TbRateLimitsException(rateLimitedEntityType));
            }
```

Fonte: ThingsBoard, *tag* v4.3.1.6,
[`common/transport/transport-api/src/main/java/org/thingsboard/server/common/transport/service/DefaultTransportService.java#L840-L847`](https://github.com/thingsboard/thingsboard/blob/v4.3.1.6/common/transport/transport-api/src/main/java/org/thingsboard/server/common/transport/service/DefaultTransportService.java#L840-L847).

**Solo.** `checkLimits` é chamado no início de cada operação do serviço de transporte. Para
telemetria, o número de pontos é somado antes da verificação, de modo que mensagens com muitas
grandezas consomem mais cota
([`DefaultTransportService.java#L528-L533`](https://github.com/thingsboard/thingsboard/blob/v4.3.1.6/common/transport/transport-api/src/main/java/org/thingsboard/server/common/transport/service/DefaultTransportService.java#L528-L533)).
Excedido o limite, o *callback* recebe `TbRateLimitsException`, e no MQTT a conexão é encerrada sem
`PUBACK`. A interface `TransportRateLimitService` mostra limites por inquilino, *gateway* e
dispositivo e controle por endereço IP após falhas de autenticação (`checkAddress`,
`onAuthSuccess`, `onAuthFailure`)
([`TransportRateLimitService.java#L13-L35`](https://github.com/thingsboard/thingsboard/blob/v4.3.1.6/common/transport/transport-api/src/main/java/org/thingsboard/server/common/transport/limits/TransportRateLimitService.java#L13-L35)).

**10.000 pés.** O componente é o controle de admissão do caminho de ingestão, o primeiro ponto de
controle de fluxo da sequência da [Seção 5.7](../capitulo-5-10-mil-pes/thingsboard.md). Ele age
antes da fila, de modo que a mensagem rejeitada não consome partição, consumidor nem ator. O
isolamento continua adiante pelo limite de *throughput* por ator (Listagem 6.7.8) e pelas partições
por entidade.

**50.000 pés.** A decisão é configurar limites por **perfil de inquilino** e, para quem exige mais,
oferecer **filas isoladas do motor de regras** e instâncias dedicadas (THINGSBOARD, 2026). O
isolamento é escalonado conforme o custo.

**100.000 pés.** A característica protegida é a **disponibilidade percebida por todos os
inquilinos**, que corresponde ao Cenário 3 da [Seção 3.7](../capitulo-3-100-mil-pes/thingsboard.md)
e à viabilidade econômica do modelo multi-inquilino: sem isolamento de desempenho, o operador teria
de dimensionar a plataforma pelo pior inquilino ou implantar uma instância por cliente.

A cadeia se completa. Uma lacuna, porém, aparece na subida: os limites agem no transporte, mas um
inquilino pode gerar carga interna desproporcional com cadeias muito ramificadas (Listagem 6.7.5)
sem exceder seus limites de entrada. Argumenta-se aqui que as filas isoladas existem para esse
caso, e que um ADR explícito sobre "o que conta como uso de um inquilino" tornaria a decisão mais
verificável.

## 6.7.9 Exercício com correção automática

O exercício previsto em `atividades/README.md` para este sistema pede um **motor de regras para
telemetria com *Chain of Responsibility***, em que as regras não dependam do transporte (MQTT ou
HTTP). A Listagem 6.7.12 apresenta o núcleo de uma solução em Java 17, inspirada na separação entre
transportes e motor de regras do ThingsBoard.

**Listagem 6.7.12** – Motor de regras com *Chain of Responsibility*, independente do transporte.

```java
// pacote br.edu.ifal.telemetria.regras -- não importa nada de "transporte"
public record Leitura(String dispositivo, String grandeza, double valor, long instante) {}

public abstract class Regra {
    private Regra proxima;

    public Regra entaoAplicar(Regra proxima) {     // monta a corrente
        this.proxima = proxima;
        return proxima;
    }

    public final void tratar(Leitura leitura, List<String> eventos) {
        boolean seguir = processar(leitura, eventos);  // cada elo decide se continua
        if (seguir && proxima != null) {
            proxima.tratar(leitura, eventos);
        }
    }

    protected abstract boolean processar(Leitura leitura, List<String> eventos);
}

public final class SomenteGrandeza extends Regra {
    private final String grandeza;
    public SomenteGrandeza(String grandeza) { this.grandeza = grandeza; }
    @Override protected boolean processar(Leitura l, List<String> eventos) {
        return l.grandeza().equals(grandeza);          // filtro: interrompe a corrente
    }
}

public final class AlarmeAcimaDe extends Regra {
    private final double limite;
    public AlarmeAcimaDe(double limite) { this.limite = limite; }
    @Override protected boolean processar(Leitura l, List<String> eventos) {
        if (l.valor() > limite) {
            eventos.add("ALARME " + l.dispositivo() + " " + l.grandeza() + "=" + l.valor());
        }
        return true;                                    // ação: deixa seguir
    }
}

// pacote br.edu.ifal.telemetria.transporte -- depende de "regras", nunca o contrário
public final class AdaptadorHttp {
    private final Regra cadeia;
    public AdaptadorHttp(Regra cadeia) { this.cadeia = cadeia; }

    // corpo no formato "sensor-7;temperatura;31.5;1760000000"
    public List<String> receber(String corpo) {
        String[] p = corpo.split(";");
        Leitura l = new Leitura(p[0], p[1], Double.parseDouble(p[2]), Long.parseLong(p[3]));
        List<String> eventos = new ArrayList<>();
        cadeia.tratar(l, eventos);
        return eventos;
    }
}
```

Fonte: elaborado pelo autor (2026). Exemplo didático; cada tipo público ficaria em seu arquivo, com
as importações de `java.util`.

A regra arquitetural é verificada por uma *fitness function* com ArchUnit (Listagem 6.7.13), que
comporá a parte de arquitetura da nota.

**Listagem 6.7.13** – *Fitness function*: regras não dependem do transporte.

```java
@AnalyzeClasses(packages = "br.edu.ifal.telemetria")
class ArquiteturaTest {

    @ArchTest
    static final ArchRule regrasNaoConhecemTransporte =
        noClasses().that().resideInAPackage("..regras..")
            .should().dependOnClassesThat().resideInAPackage("..transporte..")
            .because("o motor de regras deve funcionar com qualquer protocolo");

    @ArchTest
    static final ArchRule todaRegraEstendeRegra =
        classes().that().resideInAPackage("..regras..")
            .and().haveSimpleNameNotEndingWith("Leitura")
            .and().areNotAnonymousClasses()
            .should().beAssignableTo(Regra.class);
}
```

Fonte: elaborado pelo autor (2026).

A comparação com o código real é instrutiva. A Listagem 6.7.12 implementa o *Chain of
Responsibility* clássico, com referência ao sucessor dentro do elo e execução síncrona. O
ThingsBoard troca a referência pelo rótulo da relação, transfere a resolução do sucessor para a
cadeia e torna cada passo uma mensagem a um ator, o que permite alterar cadeias em tempo de
execução, processar nós em paralelo e ramificar mensagens.

## Exercícios

1. Localize, na *tag* v4.3.1.6, duas implementações de `TbNode` além das citadas nesta seção e
   identifique as relações de saída declaradas em suas anotações `@RuleNode`.
2. Na Listagem 6.7.5, o que acontece com o *callback* da mensagem original quando um nó tem três
   relações de saída do mesmo tipo e uma das cópias falha? Que consequência isso tem para uma fila
   configurada para reprocessar falhas indefinidamente?
3. Explique o papel de `getActorThroughput()` na Listagem 6.7.8. Que problema surgiria se o valor
   fosse ilimitado? E se fosse 1?
4. Relacione cada uma das técnicas de prevenção de perda de Richards e Ford (2020, p. 201-202) a um
   trecho de código citado nesta seção. Qual delas deixa de valer com `queue.type: in-memory`?
5. A classe `Device` (Listagem 6.7.1) é uma estrutura de dados, e as regras ficam em serviços.
   Discuta, com base em Fowler (2002) e Evans (2003), se isso é um problema neste sistema.
6. **Atividade com correção automática:** implemente o motor de regras da Seção 6.7.9 com ao menos
   um filtro, uma regra de alarme e dois adaptadores de transporte (MQTT simulado e HTTP), de modo
   que os testes de comportamento e a *fitness function* da Listagem 6.7.13 sejam satisfeitos.
7. Realize uma subida de verificação completa a partir da Listagem 6.7.9.

## Termos

`Backpressure`
:   Contrapressão: mecanismo pelo qual um estágio sobrecarregado faz a sobrecarga se manifestar como
    atraso ou rejeição no estágio anterior, em vez de aceitar trabalho indefinidamente.

`Cadeia de regras`
:   No ThingsBoard, grafo de nós de regra ligados por relações rotuladas que processa as mensagens
    de um inquilino.

`Modelo de atores`
:   Modelo de concorrência em que entidades isoladas, os atores, comunicam-se exclusivamente por
    mensagens assíncronas e processam uma mensagem por vez.

`Jitter`
:   Componente aleatório acrescentado a um intervalo de espera para dessincronizar clientes que
    falharam ao mesmo tempo.

`QoS`
:   Nível de qualidade de serviço de entrega do MQTT: 0 (no máximo uma vez), 1 (pelo menos uma vez)
    ou 2 (exatamente uma vez).

`Multi-tenant`
:   Multi-inquilino: uma única instalação atende vários clientes isolados entre si.

## Referências

EVANS, E. **Domain-driven design**: tackling complexity in the heart of software. Boston:
Addison-Wesley, 2003.

FOWLER, M. **Patterns of enterprise application architecture**. Boston: Addison-Wesley, 2002.

GAMMA, E. *et al.* **Design patterns**: elements of reusable object-oriented software. Reading:
Addison-Wesley, 1994.

RICHARDS, M.; FORD, N. **Fundamentals of software architecture**: an engineering approach.
Sebastopol: O'Reilly, 2020.

SHVETS, A. **Mergulho nos padrões de projeto**. [S. l.]: Refactoring.Guru, 2022. Disponível
em: <https://refactoring.guru/pt-br/design-patterns>. Acesso em: 18 out. 2026.

THINGSBOARD. **Tenant profiles**. ThingsBoard documentation. 2026. Disponível em:
<https://thingsboard.io/docs/user-guide/tenant-profiles/>. Acesso em: 18 out. 2026.
