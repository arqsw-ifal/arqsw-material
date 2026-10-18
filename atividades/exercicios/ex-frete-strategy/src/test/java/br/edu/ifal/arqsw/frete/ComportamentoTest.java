package br.edu.ifal.arqsw.frete;

import br.edu.ifal.arqsw.frete.dominio.Checkout;
import br.edu.ifal.arqsw.frete.dominio.Dinheiro;
import br.edu.ifal.arqsw.frete.dominio.Modalidade;
import br.edu.ifal.arqsw.frete.dominio.Pedido;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Testes de COMPORTAMENTO: o frete e o total sao calculados corretamente? */
class ComportamentoTest {

    private Checkout checkout;

    @BeforeEach
    void preparar() {
        checkout = App.novoCheckout();   // a raiz de composicao monta o checkout
    }

    /** Pedido de um unico item com o preco total e o peso informados. */
    private static Pedido pedido(long totalCentavos, int pesoGramas) {
        return new Pedido().adicionarItem("Produto", 1, Dinheiro.centavos(totalCentavos), pesoGramas);
    }

    @Test
    @DisplayName("econômico: R$ 10,00 + R$ 1,50 por kg")
    void economico() {
        assertEquals(Dinheiro.centavos(1300), checkout.frete(pedido(5000, 2000), Modalidade.ECONOMICO));
    }

    @Test
    @DisplayName("econômico: cada quilograma iniciado conta inteiro (1,2 kg cobra 2 kg)")
    void economicoArredondaPeso() {
        assertEquals(Dinheiro.centavos(1300), checkout.frete(pedido(5000, 1200), Modalidade.ECONOMICO));
    }

    @Test
    @DisplayName("expresso: R$ 20,00 + R$ 3,00 por kg iniciado")
    void expresso() {
        assertEquals(Dinheiro.centavos(2600), checkout.frete(pedido(5000, 1200), Modalidade.EXPRESSO));
    }

    @Test
    @DisplayName("retirada na loja não cobra frete")
    void retirada() {
        assertEquals(Dinheiro.ZERO, checkout.frete(pedido(5000, 3000), Modalidade.RETIRADA));
    }

    @Test
    @DisplayName("econômico é grátis para pedidos a partir de R$ 200,00")
    void economicoGratis() {
        assertEquals(Dinheiro.ZERO, checkout.frete(pedido(20000, 3000), Modalidade.ECONOMICO));
    }

    @Test
    @DisplayName("expresso nunca é grátis, mesmo acima de R$ 200,00")
    void expressoNuncaGratis() {
        assertEquals(Dinheiro.centavos(2300), checkout.frete(pedido(25000, 1000), Modalidade.EXPRESSO));
    }

    @Test
    @DisplayName("total com frete soma o total dos itens e o frete")
    void totalComFrete() {
        Pedido p = new Pedido()
                .adicionarItem("Caneta", 4, Dinheiro.centavos(500), 250)
                .adicionarItem("Caderno", 1, Dinheiro.centavos(3000), 1000);
        // itens: R$ 50,00; peso 2 kg; economico: R$ 13,00
        assertEquals(Dinheiro.centavos(6300), checkout.totalComFrete(p, Modalidade.ECONOMICO));
    }

    @Test
    @DisplayName("pedido sem itens não tem frete calculado")
    void pedidoVazio() {
        assertThrows(IllegalStateException.class, () -> checkout.frete(new Pedido(), Modalidade.EXPRESSO));
    }
}
