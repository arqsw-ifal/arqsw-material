package br.edu.ifal.arqsw.loja.pedido.dominio;

import br.edu.ifal.arqsw.loja.pagamento.dominio.RespostaDeCobranca;

/**
 * Agregado Pedido do contexto de Pedido.
 *
 * CODIGO LEGADO: o dominio de pedido usa um tipo do contexto de PAGAMENTO
 * (RespostaDeCobranca). Leia o README.
 */
public class Pedido {

    private final long id;
    private final String sku;
    private final int quantidade;
    private final long valorCentavos;
    private StatusPedido status = StatusPedido.PENDENTE;
    private String referenciaPagamento;

    public Pedido(long id, String sku, int quantidade, long valorCentavos) {
        if (quantidade <= 0) {
            throw new IllegalArgumentException("quantidade deve ser positiva");
        }
        if (valorCentavos <= 0) {
            throw new IllegalArgumentException("valor deve ser positivo");
        }
        this.id = id;
        this.sku = sku;
        this.quantidade = quantidade;
        this.valorCentavos = valorCentavos;
    }

    public long id() { return id; }
    public String sku() { return sku; }
    public int quantidade() { return quantidade; }
    public long valorCentavos() { return valorCentavos; }
    public StatusPedido status() { return status; }
    public String referenciaPagamento() { return referenciaPagamento; }

    public void registrarPagamento(RespostaDeCobranca resposta) {
        this.referenciaPagamento = resposta.idTransacao();
        this.status = StatusPedido.CONFIRMADO;
    }

    public void cancelar() {
        this.status = StatusPedido.CANCELADO;
    }
}
