package br.edu.ifal.arqsw.frete;

import br.edu.ifal.arqsw.frete.dominio.Checkout;
import br.edu.ifal.arqsw.frete.dominio.Dinheiro;
import br.edu.ifal.arqsw.frete.dominio.Modalidade;
import br.edu.ifal.arqsw.frete.dominio.Pedido;

/** Ponto de entrada e fabrica do checkout usado pelos testes. */
public class App {

    /** Monta o checkout da loja com todas as modalidades de frete. */
    public static Checkout novoCheckout() {
        return new Checkout();
    }

    public static void main(String[] args) {
        Checkout checkout = novoCheckout();
        Pedido pedido = new Pedido()
                .adicionarItem("Teclado", 1, Dinheiro.centavos(15990), 900)
                .adicionarItem("Mouse", 1, Dinheiro.centavos(4990), 100);
        for (Modalidade m : new Modalidade[] {Modalidade.ECONOMICO, Modalidade.EXPRESSO}) {
            System.out.println(m + ": frete " + checkout.frete(pedido, m)
                    + ", total " + checkout.totalComFrete(pedido, m));
        }
    }
}
