package br.edu.ifal.arqsw.pedidos.dominio;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Entidade e raiz de agregado (Evans, DDD cap. 6): toda alteracao nos itens passa por aqui,
 * e e' aqui que as regras (invariantes) do pedido sao garantidas.
 */
public class Pedido {

    private final long id;
    private final String cliente;
    private final List<ItemPedido> itens = new ArrayList<>();
    private StatusPedido status = StatusPedido.ABERTO;

    public Pedido(long id, String cliente) {
        if (cliente == null || cliente.isBlank()) {
            throw new IllegalArgumentException("cliente obrigatorio");
        }
        this.id = id;
        this.cliente = cliente;
    }

    public long id() { return id; }
    public String cliente() { return cliente; }
    public StatusPedido status() { return status; }
    public List<ItemPedido> itens() { return Collections.unmodifiableList(itens); }

    public void adicionarItem(ItemPedido item) {
        if (status == StatusPedido.FECHADO) {
            throw new IllegalStateException("pedido fechado nao aceita itens");
        }
        itens.add(item);
    }

    public long totalCentavos() {
        return itens.stream().mapToLong(ItemPedido::subtotalCentavos).sum();
    }

    public void fechar() {
        if (itens.isEmpty()) {
            throw new IllegalStateException("pedido vazio nao pode ser fechado");
        }
        status = StatusPedido.FECHADO;
    }
}
