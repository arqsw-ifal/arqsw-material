package br.edu.ifal.arqsw.frete.dominio;

/** Item de um pedido: preco e peso sao unitarios. */
public record ItemPedido(String descricao, int quantidade, Dinheiro precoUnitario, int pesoGramasUnitario) {

    public ItemPedido {
        if (quantidade <= 0) throw new IllegalArgumentException("quantidade deve ser positiva");
        if (pesoGramasUnitario < 0) throw new IllegalArgumentException("peso nao pode ser negativo");
    }

    public Dinheiro subtotal() {
        return precoUnitario.vezes(quantidade);
    }

    public long pesoGramas() {
        return (long) pesoGramasUnitario * quantidade;
    }
}
