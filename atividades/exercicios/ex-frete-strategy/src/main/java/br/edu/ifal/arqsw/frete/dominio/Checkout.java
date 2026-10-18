package br.edu.ifal.arqsw.frete.dominio;

import br.edu.ifal.arqsw.frete.frete.FreteExpresso;

/**
 * Checkout da loja: calcula o frete e o total a pagar.
 * CODIGO LEGADO: o calculo do frete esta num switch que cresce a cada nova modalidade.
 * A refatoracao para Strategy comecou (veja o caso EXPRESSO), mas parou no meio.
 */
public class Checkout {

    public Checkout() {
    }

    public Dinheiro frete(Pedido pedido, Modalidade modalidade) {
        if (!pedido.temItens()) {
            throw new IllegalStateException("pedido sem itens nao tem frete");
        }
        switch (modalidade) {
            case ECONOMICO:
                // TODO (comportamento): o peso deve ser arredondado para cima (kg iniciado conta inteiro).
                // TODO (comportamento): pedidos a partir de R$ 200,00 tem frete economico gratis.
                long kg = pedido.pesoGramas() / 1000;
                return Dinheiro.reais(10).mais(Dinheiro.centavos(150).vezes(kg));
            case EXPRESSO:
                return new FreteExpresso().calcular(pedido);   // ATALHO: depende da classe concreta
            case RETIRADA:
                // TODO (comportamento): retirada na loja nao cobra frete.
                throw new UnsupportedOperationException("retirada na loja ainda nao implementada");
            default:
                throw new IllegalArgumentException("modalidade desconhecida: " + modalidade);
        }
    }

    public Dinheiro totalComFrete(Pedido pedido, Modalidade modalidade) {
        return pedido.total().mais(frete(pedido, modalidade));
    }
}
