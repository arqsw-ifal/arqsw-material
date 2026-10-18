# 6.6 Saleor: o código

<span class="altitude solo">Solo</span>

Esta seção pousa no código do Saleor, *tag* `3.23.42` (*commit* `cb24c741`). Os trechos são
literais e acompanhados de *permalink*; os exemplos em Java são didáticos e assim rotulados. A
descida segue o caminho preparado nas altitudes anteriores: o *checkout* como saga
([Seção 5.6](../capitulo-5-10-mil-pes/saleor.md)), a consistência financeira e de estoque como
característica prioritária ([Seção 3.6](../capitulo-3-100-mil-pes/saleor.md)) e a decisão de um
banco único para o núcleo ([Seção 4.6](../capitulo-4-50-mil-pes/saleor.md)).

## 6.6.1 Agregados e fronteiras transacionais

Evans (2003, cap. 6) define o agregado como um conjunto de objetos tratado como unidade para
alteração de dados, acessado exclusivamente pela raiz (Seção 1.9.3), e associa a ele uma regra
de consistência: as invariantes internas ao agregado são garantidas ao fim de cada transação,
ao passo que regras que atravessam agregados não precisam estar sempre atualizadas e podem ser
resolvidas, num prazo especificado, por processamento de eventos, em lote ou por outros
mecanismos. Vernon (2013, cap. 10) transformou a observação em regras: modelar como agregado
apenas as invariantes verdadeiras; preferir agregados pequenos; referenciar outros agregados
pela identidade; e usar consistência eventual fora da fronteira.

Aplicadas ao comércio eletrônico, as regras levam a uma conclusão contraintuitiva: **pedido,
pagamento e estoque não formam um único agregado**. "O total do pedido é a soma das linhas"
pertence ao pedido; "o valor capturado não excede o autorizado", ao pagamento; "a quantidade
alocada não excede o saldo do depósito", ao estoque. O que os liga é um processo, o *checkout*,
e não uma invariante. Se fossem um agregado, toda compra travaria simultaneamente o pedido, o
saldo de cada item e o pagamento durante a chamada ao provedor, que pode durar segundos. A
Listagem 6.6.1 mostra, em Java, o agregado `Pedido` com referência ao pagamento só por
identidade.

**Listagem 6.6.1** – Modelo didático em Java: o agregado `Pedido`, com referências a outros agregados por identidade.

```java
package br.edu.ifal.loja.pedido.dominio;

public class Pedido {                                     // raiz do agregado
    private final IdPedido id;
    private final List<LinhaPedido> linhas;               // objetos de valor internos
    private SituacaoPedido situacao = SituacaoPedido.AGUARDANDO_PAGAMENTO;
    private IdPagamento pagamento;                        // outro agregado: só a identidade
    private final List<EventoDeDominio> eventos = new ArrayList<>();

    Pedido(IdPedido id, List<LinhaPedido> linhas) {
        if (linhas.isEmpty()) throw new IllegalArgumentException("pedido sem linhas");
        this.id = id; this.linhas = List.copyOf(linhas);
    }

    public Dinheiro total() {
        return linhas.stream().map(LinhaPedido::subtotal).reduce(Dinheiro.ZERO, Dinheiro::mais);
    }

    public void confirmarPagamento(IdPagamento idPagamento, Dinheiro valorCapturado) {
        if (situacao != SituacaoPedido.AGUARDANDO_PAGAMENTO)
            throw new IllegalStateException("pedido não aguarda pagamento: " + situacao);
        if (!valorCapturado.equals(total()))
            throw new IllegalArgumentException("valor capturado difere do total");
        this.pagamento = idPagamento;
        this.situacao = SituacaoPedido.CONFIRMADO;
        eventos.add(new PedidoConfirmado(id, total()));   // outros contextos reagem depois
    }

    public void cancelar(MotivoCancelamento motivo) {
        if (situacao == SituacaoPedido.DESPACHADO)
            throw new IllegalStateException("pedido despachado exige devolução");
        this.situacao = SituacaoPedido.CANCELADO;
        eventos.add(new PedidoCancelado(id, motivo));
    }
}
```

Fonte: elaborado pelo autor (2026), com base em Evans (2003, cap. 6) e Vernon (2013, cap. 10).

No Saleor, os modelos correspondentes são `Checkout` e `CheckoutLine` (`saleor/checkout/models.py`),
`Order` e `OrderLine` (`saleor/order/models.py`), `TransactionItem`, `TransactionEvent` e
`Payment` (`saleor/payment/models.py`) e `Stock`, `Allocation` e `Reservation`
(`saleor/warehouse/models.py`). As situações possíveis de um pedido estão em `OrderStatus`
(`saleor/order/__init__.py`): rascunho, não confirmado, não atendido, parcialmente atendido,
atendido, parcialmente devolvido, devolvido, cancelado e expirado. Como se verá, os agregados
do Saleor existem como **fronteiras de bloqueio e de transação**, mas não como objetos que
protegem suas próprias invariantes.

## 6.6.2 A saga do *checkout* em processo

A função `complete_checkout_with_payment`, do fluxo de pagamento legado, converte o *checkout*
em pedido em **três transações sucessivas**: a primeira trava o *checkout*, marca o início da
conversão, valida os dados e reserva o estoque; a segunda processa o pagamento; a terceira cria
o pedido, aloca o estoque e apaga o *checkout*. A Listagem 6.6.2 mostra o início da primeira.

**Listagem 6.6.2** – Código real (Python, Saleor 3.23.42): bloqueio do *checkout* e marcação de processamento.

```python
    with transaction_with_commit_on_errors():
        checkout = Checkout.objects.select_for_update().filter(pk=checkout_pk).first()
        if not checkout:
            order = Order.objects.get_by_checkout_token(checkout_pk)
            return order, False, {}

        checkout.completing_started_at = timezone.now()
        checkout.save(update_fields=["completing_started_at"])
```

Fonte: Saleor (2026), [`saleor/checkout/complete_checkout.py`, linhas 1871-1878](https://github.com/saleor/saleor/blob/3.23.42/saleor/checkout/complete_checkout.py#L1871-L1878).

O trecho concentra três ideias. O `select_for_update()` emite um `SELECT ... FOR UPDATE` no
PostgreSQL e serializa duas conclusões concorrentes do mesmo carrinho. O campo
`completing_started_at` é um **bloqueio semântico** no sentido de Richardson (2018, cap. 4): um
estado do próprio objeto que sinaliza, para além da transação, que ele está em meio a uma saga.
E o desvio para `get_by_checkout_token` é um mecanismo de **idempotência**: se o *checkout* já
não existe, uma execução anterior o converteu em pedido, e a função devolve esse pedido em vez
de falhar ou criar outro (Listagem 6.6.3).

**Listagem 6.6.3** – Código real (Python, Saleor 3.23.42): recuperação do pedido já criado a partir do *token* do *checkout*.

```python
class OrderQueryset(models.QuerySet["Order"]):
    def get_by_checkout_token(self, token):
        """Return non-draft order with matched checkout token."""
        return self.non_draft().filter(checkout_token=token).first()
```

Fonte: Saleor (2026), [`saleor/order/models.py`, linhas 48-51](https://github.com/saleor/saleor/blob/3.23.42/saleor/order/models.py#L48-L51).

O bloqueio semântico tem prazo de validade, o que impede que uma saga interrompida trave o
carrinho para sempre (Listagem 6.6.4). O prazo vem da configuração
`CHECKOUT_COMPLETION_LOCK_TIME`, cujo valor padrão em `settings.py` é de 3 minutos.

**Listagem 6.6.4** – Código real (Python, Saleor 3.23.42): o bloqueio semântico expira.

```python
    def is_checkout_locked(self) -> bool:
        return bool(
            self.completing_started_at
            and (
                (timezone.now() - self.completing_started_at).seconds
                < settings.CHECKOUT_COMPLETION_LOCK_TIME
            )
        )
```

Fonte: Saleor (2026), [`saleor/checkout/models.py`, linhas 379-386](https://github.com/saleor/saleor/blob/3.23.42/saleor/checkout/models.py#L379-L386).

A decisão mais interessante está entre a primeira e a segunda transação, e o comentário dos
desenvolvedores a explica (Listagem 6.6.5).

**Listagem 6.6.5** – Código real (Python, Saleor 3.23.42): o pagamento é processado fora da transação que reserva o estoque.

```python
    # Process payments out of transaction to unlock stock rows for another user,
    # who potentially can order the same product variants.
    txn = None
    channel_slug = checkout_info.channel.slug
    voucher = checkout_info.voucher
    voucher_code = checkout_info.voucher_code
    if payment:
        with transaction_with_commit_on_errors():
            checkout = (
                Checkout.objects.select_for_update().filter(pk=checkout_pk).first()
            )

            if not checkout:
                order = Order.objects.get_by_checkout_token(checkout_pk)
                return order, False, {}

            payment = Payment.objects.select_for_update().get(id=payment.id)
            txn = _process_payment(
```

Fonte: Saleor (2026), [`saleor/checkout/complete_checkout.py`, linhas 1903-1920](https://github.com/saleor/saleor/blob/3.23.42/saleor/checkout/complete_checkout.py#L1903-L1920).

O comentário justifica, em uma frase, a Seção 6.6.1: se a chamada ao provedor ocorresse dentro
da transação que bloqueia o estoque, outros compradores das mesmas variantes ficariam
bloqueados. Os desenvolvedores **quebraram a transação** e aceitaram estados intermediários
visíveis, isto é, uma saga orquestrada em processo. A reserva de estoque é a etapa compensável;
a cobrança, o pivô; a criação do pedido e a alocação definitiva, as etapas seguintes. O fluxo de
transações, atual, repete o padrão de bloqueio e retorno idempotente em
`create_order_from_checkout` (Listagem 6.6.6).

**Listagem 6.6.6** – Código real (Python, Saleor 3.23.42): o mesmo padrão em `create_order_from_checkout`.

```python
    with transaction.atomic():
        checkout = Checkout.objects.select_for_update().filter(pk=checkout_pk).first()
        if not checkout:
            order = Order.objects.get_by_checkout_token(checkout_pk)
            return order
```

Fonte: Saleor (2026), [`saleor/checkout/complete_checkout.py`, linhas 1620-1624](https://github.com/saleor/saleor/blob/3.23.42/saleor/checkout/complete_checkout.py#L1620-L1624).

## 6.6.3 As compensações

Se o estoque se revelar insuficiente, se um vale-presente não puder ser aplicado ou se o
pagamento falhar, a saga precisa desfazer o que já foi feito. O Saleor concentra as
compensações numa única função (Listagem 6.6.7).

**Listagem 6.6.7** – Código real (Python, Saleor 3.23.42): a função de compensação do *checkout*.

```python
def _complete_checkout_fail_handler(
    checkout_info: "CheckoutInfo",
    manager: "PluginsManager",
    *,
    voucher_code: Optional["VoucherCode"] = None,
    voucher: Optional["Voucher"] = None,
    payment: Payment | None = None,
) -> None:
    """Handle the case when the checkout completion failed.

    - Release the checkout processing indicator.
    - Release the voucher usage.
    - Refund or void the payment.
    """
```

Fonte: Saleor (2026), [`saleor/checkout/complete_checkout.py`, linhas 2025-2037](https://github.com/saleor/saleor/blob/3.23.42/saleor/checkout/complete_checkout.py#L2025-L2037).

As três ações compensam três etapas anteriores: liberar `completing_started_at` desfaz o
bloqueio semântico; liberar o uso do cupom desfaz o incremento do contador, feito numa transação
própria; estornar ou cancelar o pagamento desfaz a cobrança. A última ação está nas linhas
2059-2062, numa chamada a `gateway.payment_refund_or_void` (`saleor/payment/gateway.py`). A
função é chamada inclusive quando a falha ocorre **depois** do pagamento, por falta de estoque
na criação do pedido: o pivô já ocorreu, e a compensação financeira é a única forma de manter
a consistência. É a situação que Richards e Ford (2020, p. 168-169) usaram para contrastar a
arquitetura baseada em serviços com os microsserviços, aqui resolvida dentro de um monólito,
porque a chamada ao provedor externo já impunha uma fronteira transacional.

## 6.6.4 Idempotência em pagamentos

Uma operação é **idempotente** quando executá-la várias vezes produz o mesmo efeito que
executá-la uma vez. Quando uma chamada excede o tempo limite, o cliente não sabe se a operação
foi executada; sem idempotência, a única reação segura é não repetir. Helland (2007) argumenta
que, em sistemas que abandonam transações distribuídas, mensagens podem chegar mais de uma vez,
e por isso seu processamento deve ser idempotente. A documentação da Stripe descreve a técnica
padrão: o cliente gera uma **chave de idempotência** e a envia num cabeçalho; o servidor guarda
o resultado da primeira requisição com aquela chave e o devolve nas repetições, inclusive
quando terminou em erro; parâmetros divergentes com a mesma chave geram erro; as chaves podem
ser descartadas após pelo menos 24 horas; e recomenda-se gerá-las com entropia suficiente,
como UUID versão 4, sem usar dados pessoais (STRIPE, 2026). A API Pix chega ao mesmo resultado
com `PUT /cob/{txid}`, em que o identificador, escolhido pelo recebedor, é único por CPF ou
CNPJ (BANCO CENTRAL DO BRASIL, 2025).

No Saleor, `TransactionItem` e `TransactionEvent` têm um campo `idempotency_key` protegido por
restrição de unicidade (Listagem 6.6.8).

**Listagem 6.6.8** – Código real (Python, Saleor 3.23.42): unicidade da chave de idempotência em `TransactionItem`.

```python
        constraints = [
            models.UniqueConstraint(
                fields=["app_identifier", "idempotency_key"],
                name="unique_transaction_idempotency",
            ),
        ]
```

Fonte: Saleor (2026), [`saleor/payment/models.py`, linhas 216-221](https://github.com/saleor/saleor/blob/3.23.42/saleor/payment/models.py#L216-L221).

A chave é única **por app**: duas *apps* de pagamento podem usar a mesma chave sem conflito. No
evento, a restrição combina `transaction_id` e `idempotency_key` (linhas 283-287). No esquema
GraphQL, a mutação `transactionInitialize` aceita `idempotencyKey`, repassada à *app* para que
ela detecte ações duplicadas, e gera uma chave padrão se nenhuma for fornecida; nos eventos
síncronos de cobrança, reembolso e cancelamento, o esquema registra que as novas tentativas de
entrega do mesmo pedido de ação carregam a mesma chave, e que solicitar a ação de novo
constitui um novo pedido, com nova chave (`saleor/graphql/schema.graphql`). É a distinção entre
**intenção** e **tentativa**. As tentativas não são hipotéticas: a tarefa que entrega os
*webhooks* assíncronos repete automaticamente (Listagem 6.6.9).

**Listagem 6.6.9** – Código real (Python, Saleor 3.23.42): entrega de *webhooks* assíncronos com repetição automática.

```python
@app.task(
    queue=settings.WEBHOOK_CELERY_QUEUE_NAME,
    bind=True,
    retry_backoff=10,
    retry_kwargs={"max_retries": 5},
)
```

Fonte: Saleor (2026), [`saleor/webhook/transport/asynchronous/transport.py`, linhas 804-809](https://github.com/saleor/saleor/blob/3.23.42/saleor/webhook/transport/asynchronous/transport.py#L804-L809).

A Listagem 6.6.10 mostra, em Java, o lado do servidor da técnica, com a mesma proteção por
restrição de unicidade.

**Listagem 6.6.10** – Modelo didático em Java: serviço de cobrança idempotente.

```java
public final class ServicoDeCobranca {
    private final RegistroDeIdempotencia registro;  // tabela com UNIQUE(chave)
    private final ProvedorDePagamento provedor;     // porta da camada anticorrupção

    public ServicoDeCobranca(RegistroDeIdempotencia r, ProvedorDePagamento p) {
        this.registro = r; this.provedor = p;
    }

    public ResultadoCobranca cobrar(Cobranca cobranca) {
        var chave = cobranca.chave();               // derivada do pedido, não da tentativa
        Optional<RegistroDeCobranca> anterior = registro.buscar(chave);
        if (anterior.isPresent()) {
            if (!anterior.get().mesmosParametros(cobranca))
                throw new ConflitoDeIdempotencia(chave);
            return anterior.get().resultado();      // repetição: mesmo resultado, sem cobrar
        }
        registro.reservar(chave, cobranca);         // antes da chamada; UNIQUE resolve corridas
        ResultadoCobranca resultado = provedor.cobrar(cobranca);   // repassa a chave
        registro.concluir(chave, resultado);
        return resultado;
    }
}
```

Fonte: elaborado pelo autor (2026), com base em Stripe (2026) e Helland (2007).

!!! caixa-preta "Cobrança em duplicidade por nova tentativa sem idempotência"
    O cenário é um **modo de falha genérico**, e não o relato de um incidente específico; é o
    problema relatado pelo cliente hipotético da Seção 3.6.4. O comprador clica em "pagar". O
    servidor chama o provedor, que captura o valor, mas a resposta se perde por tempo limite.
    Para a loja, a cobrança "falhou"; o código de nova tentativa repete a chamada, ou o próprio
    comprador clica de novo. Sem saber que se trata da mesma intenção, o provedor cria uma
    segunda cobrança. O financeiro descobre a divergência dias depois, na conciliação.

    **Leitura arquitetural.** O defeito não está na nova tentativa, necessária em sistemas
    distribuídos, mas na ausência de chave de idempotência **por intenção**. A correção tem
    quatro partes: derivar a chave do pedido, e não da tentativa; persisti-la antes da chamada,
    com restrição de unicidade (Listagem 6.6.8); repassá-la ao provedor, que deduplica do lado
    dele; e manter um processo de **reconciliação** periódica, porque nenhum mecanismo de
    idempotência cobre todas as falhas.

## 6.6.5 A camada anticorrupção dos *gateways*

No mecanismo de pagamentos legado, o módulo `saleor/payment/interface.py` define *dataclasses*
que formam a linguagem comum entre o núcleo e qualquer *gateway*: `PaymentData`, com os dados
de entrada, e `GatewayResponse`, com o resultado (Listagem 6.6.11).

**Listagem 6.6.11** – Código real (Python, Saleor 3.23.42): `GatewayResponse`: a linguagem comum entre o núcleo e os provedores.

```python
@dataclass
class GatewayResponse:
    """Dataclass for storing gateway response.

    Used for unifying the representation of gateway response.
    It is required to communicate between Saleor and given payment gateway.
    """

    is_success: bool
    action_required: bool
    kind: str  # use "TransactionKind" class
    amount: Decimal
    currency: str
    transaction_id: str
    error: str | None
```

Fonte: Saleor (2026), [`saleor/payment/interface.py`, linhas 302-316](https://github.com/saleor/saleor/blob/3.23.42/saleor/payment/interface.py#L302-L316).

Cada *plug-in* de *gateway* é um tradutor. O da Stripe (`saleor/payment/gateways/stripe/plugin.py`)
recebe um `PaymentData`, chama a API da Stripe com os conceitos dela (o identificador de um
*payment intent*, o valor em unidades mínimas da moeda) e devolve um `GatewayResponse`; no
estorno (linhas 427-436), o objeto de reembolso da Stripe vira `kind=TransactionKind.REFUND`, e o
erro da Stripe, a mensagem para o usuário. É a estrutura de Evans (2003, cap. 14): interface no
modelo do cliente, fachada sobre a API externa e tradutor entre os dois. Há uma impureza
instrutiva: o campo `raw_response`, que guarda a resposta bruta do provedor, traz o comentário
"@deprecated instead of storing entire response, pick only needed fields" (linha 319), marca de
uma tradução que deixou vazar o modelo externo e que os mantenedores decidiram estancar.

## 6.6.6 De *plug-ins* a *apps*

O `PluginsManager` (`saleor/plugins/manager.py`) percorre, para cada operação extensível, os
*plug-ins* ativos e encadeia seus resultados, passando a cada um o valor produzido pelo
anterior (`previous_value`). Os mesmos métodos trazem um aviso de depreciação (Listagem
6.6.12).

**Listagem 6.6.12** – Código real (Python, Saleor 3.23.42): aviso de depreciação no gerenciador de *plug-ins*.

```python
    # Note: this method is deprecated and will be removed in a future release.
    # Webhook-related functionality will be moved from plugin to core modules.
    def order_created(self, order: "Order", webhooks=None):
        default_value = None
        return self.__run_method_on_plugins(
            "order_created",
            default_value,
            order,
            channel_slug=order.channel.slug,
            webhooks=webhooks,
        )
```

Fonte: Saleor (2026), [`saleor/plugins/manager.py`, linhas 914-923](https://github.com/saleor/saleor/blob/3.23.42/saleor/plugins/manager.py#L914-L923).

O comentário documenta a migração descrita no ADR-002 da Seção 4.6.6: o ponto de extensão sai
de dentro do processo (no estilo microkernel) para fora dele. Os eventos síncronos que
substituem os métodos de *plug-in* estão em `WebhookEventSyncType` (Listagem 6.6.13), e a lista
é, em si, uma destilação: impostos, cobrança e frete são os subdomínios que o núcleo declara
genéricos.

**Listagem 6.6.13** – Código real (Python, Saleor 3.23.42): eventos síncronos: impostos, pagamento e frete delegados a *apps*.

```python
    CHECKOUT_CALCULATE_TAXES = "checkout_calculate_taxes"
    ORDER_CALCULATE_TAXES = "order_calculate_taxes"

    TRANSACTION_CHARGE_REQUESTED = "transaction_charge_requested"
    TRANSACTION_REFUND_REQUESTED = "transaction_refund_requested"
    TRANSACTION_CANCELATION_REQUESTED = "transaction_cancelation_requested"

    SHIPPING_LIST_METHODS_FOR_CHECKOUT = "shipping_list_methods_for_checkout"
```

Fonte: Saleor (2026), [`saleor/webhook/event_types.py`, linhas 876-883](https://github.com/saleor/saleor/blob/3.23.42/saleor/webhook/event_types.py#L876-L883).

## 6.6.7 *Active Record* e *Data Mapper*

Os modelos do Saleor herdam de `django.db.models.Model`: cada classe corresponde a uma tabela,
cada atributo a uma coluna, e cada instância sabe salvar-se (`checkout.save(...)`, na Listagem
6.6.2) e ser buscada por um gerenciador da própria classe (`Checkout.objects`). É o ***Active
Record*** de Fowler (2002, cap. 10): um objeto que envolve uma linha, encapsula o acesso ao
banco e acrescenta lógica de domínio. Fowler o recomenda quando a lógica não é muito complexa e
as classes correspondem diretamente às tabelas; seu principal problema é acoplar o projeto de
objetos ao do banco. O ***Data Mapper*** é uma camada que transfere dados entre objetos e banco
mantendo-os independentes: os objetos de domínio não sabem sequer que há um banco (FOWLER, 2002,
cap. 10). A Listagem 6.6.14 contrasta os dois estilos.

**Listagem 6.6.14** – Modelo didático em Java: *Active Record* e *Data Mapper* para o mesmo conceito de estoque.

```java
// ---- Active Record: o objeto conhece a tabela e se persiste ----
public class Estoque {                                   // espelha a tabela "estoque"
    long id; long idVariante; long idDeposito; int quantidade; int alocado;

    public static Estoque buscar(long idVariante, long idDeposito) { /* SELECT ... */ return null; }
    public void alocar(int n) {
        if (alocado + n > quantidade) throw new IllegalStateException("sem saldo");
        alocado += n;
        salvar();                                        // regra e persistência no mesmo objeto
    }
    public void salvar() { /* UPDATE estoque SET ... */ }
}

// ---- Data Mapper (JPA/Hibernate): o domínio ignora o banco ----
public class SaldoNoDeposito {                           // objeto de domínio puro
    private final Sku sku; private final IdDeposito deposito;
    private int quantidade; private int alocado;
    public void alocar(int n) {
        if (alocado + n > quantidade) throw new IllegalStateException("sem saldo");
        alocado += n;                                    // nenhuma chamada de persistência
    }
}

public interface SaldosNosDepositos {                    // repositório (porta do domínio)
    SaldoNoDeposito doSku(Sku sku, IdDeposito deposito);
    void salvar(SaldoNoDeposito saldo);
}
// A implementação JPA mapeia SaldoNoDeposito para a tabela; o EntityManager atua como
// unidade de trabalho (Unit of Work), gravando as alterações no commit.
```

Fonte: elaborado pelo autor (2026), com base em Fowler (2002, caps. 10 e 11).

O Django não tem unidade de trabalho explícita (FOWLER, 2002, cap. 11): cada `save()` emite
imediatamente um comando SQL, e `transaction.atomic()` apenas delimita a transação. No Saleor,
grande parte das regras não está nos modelos, mas em funções de módulo, o que caracteriza
***Transaction Script*** com *Active Records* como estrutura de dados (FOWLER, 2002, cap. 9).
A invariante "não alocar além do saldo", por exemplo, é garantida pela função
`allocate_stocks` (`saleor/warehouse/management.py`, linha 91 em diante), cuja documentação
informa que ela trava para atualização todos os estoques e alocações das variantes envolvidas
antes de calcular as quantidades, e não pela classe `Stock`. Para um sistema cujo modelo é
largamente isomórfico ao esquema relacional e cujo valor está em consultas eficientes, a
escolha é coerente; o custo é que as invariantes ficam espalhadas e difíceis de localizar.

## 6.6.8 Padrões identificados

A Tabela 6.6.1 registra os padrões encontrados, no formato da seção *Known Uses* dos catálogos.

**Tabela 6.6.1** – Usos conhecidos de padrões no Saleor 3.23.42.

| Padrão (catálogo) | Onde | Papel no Saleor |
|---|---|---|
| *Active Record* (FOWLER, 2002) | todos os `models.py` | modelos do ORM do Django |
| *Transaction Script* (FOWLER, 2002) | `checkout/complete_checkout.py`, `order/actions.py`, `warehouse/management.py` | lógica organizada por operação |
| *Remote Facade* e DTO (FOWLER, 2002) | `saleor/graphql/` | API grossa sobre modelos finos; tipos GraphQL como DTOs |
| *Pessimistic Offline Lock* (FOWLER, 2002) e bloqueio semântico (RICHARDSON, 2018) | `Checkout.completing_started_at`, `is_checkout_locked` | impede conclusões concorrentes entre transações |
| Saga com compensação (GARCIA-MOLINA; SALEM, 1987) | `complete_checkout_with_payment`, `_complete_checkout_fail_handler` | conversão do *checkout* em transações sucessivas |
| Chave de idempotência | `TransactionItem`, `TransactionEvent`, `idempotencyKey` | deduplicação de ações de pagamento |
| Camada anticorrupção (EVANS, 2003) | `payment/interface.py`, *plug-ins* de *gateway*, *apps* | tradução entre o núcleo e os provedores |
| *Adapter* (GAMMA *et al.*, 1994) | `StripeGatewayPlugin` e demais *gateways* | adapta a API do provedor à interface comum |
| *Chain of Responsibility*, variante (GAMMA *et al.*, 1994) | `PluginsManager.__run_method_on_plugins` | encadeia *plug-ins* com `previous_value`; todos os elos executam |
| *Circuit Breaker* | `saleor/webhook/circuit_breaker/` | protege o *checkout* de *apps* síncronas com falha |

Fonte: elaborado pelo autor (2026), com base em Saleor (2026).

## 6.6.9 Subida de verificação

A subida parte da linha 1878 de `complete_checkout.py`,
`checkout.save(update_fields=["completing_started_at"])`, e pergunta, a cada altitude, o que ela
realiza (Seção 1.11).

**Tabela 6.6.2** – Subida de verificação a partir do bloqueio semântico do *checkout*.

| Altitude | Pergunta | Resposta |
|---|---|---|
| <span class="altitude solo">Solo</span> | O que a linha faz? | Grava, numa transação com `SELECT ... FOR UPDATE`, o instante de início da conversão do *checkout* (*Active Record*). |
| <span class="altitude a10">10.000 pés</span> | Que componente ela realiza? | O orquestrador da saga do *checkout*, que divide a conversão em três transações e usa o campo como bloqueio semântico com prazo; a função de compensação o libera (Seção 5.6.3). |
| <span class="altitude a50">50.000 pés</span> | Que decisão ela concretiza? | Manter o núcleo como monólito modular com banco único (ADR-003, Seção 4.6.6), mas retirar a chamada ao provedor, externo e lento, da transação que bloqueia o estoque. |
| <span class="altitude a100">100.000 pés</span> | Que característica ela protege? | A consistência financeira e de estoque (nenhum pedido duplicado, nenhuma cobrança sem pedido ou sem estorno), sem sacrificar a elasticidade nos lançamentos em que muitos compradores disputam os mesmos itens (Seção 3.6.8). |

Fonte: elaborado pelo autor (2026).

A cadeia se completa e responde onde a consistência do pedido é garantida: dentro de cada
etapa, pela transação do PostgreSQL e pelo bloqueio de linhas; entre etapas, pela saga em
processo, com bloqueio semântico, retorno idempotente e compensações. Como as transações
compartilham banco e processo, o núcleo forma **um único *quantum***. Se, em vez dessa linha, o
ponto de partida fosse o campo `raw_response` de `GatewayResponse`, a subida se interromperia a
50.000 pés: nenhuma decisão vigente justifica guardar a resposta bruta do provedor, e o próprio
código a marca como obsoleta. É o caso, previsto na Seção 1.11, de código que sobreviveu à
decisão que o justificava.

## 6.6.10 Atividade com correção automática

A atividade do Saleor está prevista em `atividades/README.md`: **dois contextos delimitados,
Pedido e Pagamento, que só se comunicam pela camada anticorrupção**. O aluno recebe um código
em que o pacote `pedido` usa diretamente classes de `pagamento` e de um SDK fictício de
provedor, e deve refatorá-lo até que: (a) `pedido.dominio` não dependa de `pagamento` nem do
SDK; (b) a comunicação ocorra por uma porta definida em `pedido` e implementada num adaptador
em `pedido.acl`; e (c) a confirmação do pedido funcione como saga com compensação e chave de
idempotência por pedido. Os testes de comportamento (JUnit) verificam a confirmação, a
compensação quando o pagamento é recusado e a ausência de cobrança duplicada em repetições. As
funções de aptidão (ArchUnit) verificam as dependências, por exemplo com uma regra do tipo
`noClasses().that().resideInAPackage("..pedido.dominio..").should().dependOnClassesThat().resideInAPackage("..pagamento..")`,
e que apenas `..pedido.acl..` acessa o outro contexto. A regra materializa a fronteira que o
Saleor deixa implícita (Seção 5.6.7).

## Exercícios

1. Na Listagem 6.6.5, que problema ocorreria se a chamada a `_process_payment` fosse feita
   dentro da primeira transação, que reserva o estoque? Relacione a resposta à regra de
   agregados de Evans.
2. Classifique cada etapa de `complete_checkout_with_payment` como compensável, pivô ou
   retentável, e indique, para cada etapa compensável, a ação correspondente em
   `_complete_checkout_fail_handler`.
3. Explique por que a restrição de unicidade da Listagem 6.6.8 inclui `app_identifier`. O que
   aconteceria se a chave fosse única globalmente?
4. Na Listagem 6.6.10, o que aconteceria se a chave de idempotência fosse gerada com
   `UUID.randomUUID()` a cada chamada de `cobrar`? Descreva um cenário concreto de falha.
5. Reescreva o agregado `Pedido` da Listagem 6.6.1 como *Active Record* e identifique que
   regras migrariam para funções de serviço. Compare com a organização do Saleor.
6. Localize, na *tag* 3.23.42, os módulos de pedido e de pagamento e descreva como se
   comunicam: por importação direta, chave estrangeira, evento ou *webhook*. Que padrão do mapa
   de contextos descreve melhor a relação?
7. **Atividade com correção automática** (Seção 6.6.10): implemente os contextos `pedido` e
   `pagamento` com camada anticorrupção, até obter nota máxima no corretor.
8. Faça a subida de verificação a partir da Listagem 6.6.9 (`retry_kwargs={"max_retries": 5}`)
   até a característica arquitetural que ela protege, e indique que obrigação ela impõe às
   *apps*.

## Termos

`Bloqueio semântico`
:   Estado de um objeto que indica, além da transação de banco, que ele está em uso por uma
    saga em andamento.

`Transação pivô`
:   Etapa de uma saga a partir da qual a saga não é mais desfeita, apenas concluída.

`Chave de idempotência`
:   Identificador de uma intenção de operação, enviado pelo cliente, que permite ao servidor
    reconhecer e deduplicar repetições.

`Active Record`
:   Objeto que envolve uma linha de tabela, encapsula o acesso ao banco e acrescenta lógica de
    domínio a esses dados.

`Data Mapper`
:   Camada que transfere dados entre objetos em memória e o banco, mantendo-os independentes.

## Referências

BANCO CENTRAL DO BRASIL. **bacen/pix-api**: especificações da API Pix em OpenAPI 3.0,
versão 2.10.0. GitHub, 2025. Disponível em: <https://github.com/bacen/pix-api>. Acesso em:
18 out. 2026.

EVANS, E. **Domain-driven design**: tackling complexity in the heart of software. Boston:
Addison-Wesley, 2003.

FOWLER, M. **Patterns of enterprise application architecture**. Boston: Addison-Wesley, 2002.

GAMMA, E. *et al.* **Design patterns**: elements of reusable object-oriented software. Reading:
Addison-Wesley, 1994.

GARCIA-MOLINA, H.; SALEM, K. Sagas. *In*: ACM SIGMOD INTERNATIONAL CONFERENCE ON MANAGEMENT
OF DATA, 1987, San Francisco. **Proceedings** [...]. New York: ACM, 1987. p. 249-259.

HELLAND, P. Life beyond distributed transactions: an apostate's opinion. *In*: CONFERENCE ON
INNOVATIVE DATA SYSTEMS RESEARCH (CIDR), 3., 2007, Asilomar. **Proceedings** [...]. 2007.
p. 132-141.

RICHARDS, M.; FORD, N. **Fundamentals of software architecture**: an engineering approach.
Sebastopol: O'Reilly, 2020.

RICHARDSON, C. **Microservices patterns**: with examples in Java. Shelter Island: Manning,
2018.

SALEOR. **saleor/saleor**: repositório de código-fonte, versão 3.23.42. GitHub, 2026.
Disponível em: <https://github.com/saleor/saleor/tree/3.23.42>. Acesso em: 18 out. 2026.

STRIPE. **Idempotent requests**. Documentação da API da Stripe, 2026. Disponível em:
<https://docs.stripe.com/api/idempotent_requests>. Acesso em: 18 out. 2026.

VERNON, V. **Implementing domain-driven design**. Upper Saddle River: Addison-Wesley, 2013.
