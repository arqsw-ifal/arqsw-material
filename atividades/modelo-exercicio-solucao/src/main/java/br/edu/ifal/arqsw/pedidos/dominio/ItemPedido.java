package br.edu.ifal.arqsw.pedidos.dominio;

/** Value Object: um item do pedido. Imutavel; precos em centavos para evitar erro de ponto flutuante. */
public record ItemPedido(String produto, int quantidade, long precoUnitarioCentavos) {

    public ItemPedido {
        if (produto == null || produto.isBlank()) {
            throw new IllegalArgumentException("produto obrigatorio");
        }
        if (quantidade <= 0) {
            throw new IllegalArgumentException("quantidade deve ser positiva");
        }
        if (precoUnitarioCentavos < 0) {
            throw new IllegalArgumentException("preco nao pode ser negativo");
        }
    }

    public long subtotalCentavos() {
        return quantidade * precoUnitarioCentavos;
    }
}
