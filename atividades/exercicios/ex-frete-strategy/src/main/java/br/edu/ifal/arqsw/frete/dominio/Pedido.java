package br.edu.ifal.arqsw.frete.dominio;

import java.util.ArrayList;
import java.util.List;

/** Pedido de uma loja virtual: sabe seu total e seu peso. */
public class Pedido {

    private final List<ItemPedido> itens = new ArrayList<>();

    public Pedido adicionarItem(String descricao, int quantidade, Dinheiro precoUnitario, int pesoGramasUnitario) {
        itens.add(new ItemPedido(descricao, quantidade, precoUnitario, pesoGramasUnitario));
        return this;
    }

    public boolean temItens() {
        return !itens.isEmpty();
    }

    public Dinheiro total() {
        return itens.stream().map(ItemPedido::subtotal).reduce(Dinheiro.ZERO, Dinheiro::mais);
    }

    public long pesoGramas() {
        return itens.stream().mapToLong(ItemPedido::pesoGramas).sum();
    }

    /** Peso cobrado pelas transportadoras: cada quilograma iniciado conta inteiro. */
    public long pesoKgArredondadoParaCima() {
        return (pesoGramas() + 999) / 1000;
    }
}
