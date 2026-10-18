package br.edu.ifal.arqsw.pedidos.dominio;

import br.edu.ifal.arqsw.pedidos.infra.PedidoRepositoryEmMemoria;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Entidade "pedido".
 *
 * CODIGO LEGADO: esta classe sabe se salvar sozinha (estilo Active Record), e para isso
 * depende diretamente da infraestrutura. Leia o README antes de mexer.
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
        // TODO (regra 2): pedido FECHADO nao aceita novos itens -> IllegalStateException
        itens.add(item);
    }

    public long totalCentavos() {
        return itens.stream().mapToLong(ItemPedido::subtotalCentavos).sum();
    }

    public void fechar() {
        // TODO (regra 1): pedido sem itens nao pode ser fechado -> IllegalStateException
        status = StatusPedido.FECHADO;
    }

    /** O pedido se salva sozinho... no repositorio GLOBAL da infraestrutura. */
    public void salvar() {
        PedidoRepositoryEmMemoria.instancia().salvar(this);
    }
}
