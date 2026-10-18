package br.edu.ifal.arqsw.loja;

import br.edu.ifal.arqsw.loja.pagamento.aplicacao.ServicoDeCobranca;
import br.edu.ifal.arqsw.loja.pedido.aplicacao.ServicoDeConfirmacao;
import br.edu.ifal.arqsw.loja.pedido.dominio.Estoque;
import br.edu.ifal.arqsw.loja.sdk.xpay.XPayClient;

/**
 * Raiz de composicao: o UNICO lugar que conhece os dois contextos e o SDK, e os liga.
 * Os testes montam a loja por meio de montar(...).
 */
public class App {

    public static ServicoDeConfirmacao montar(XPayClient provedor, Estoque estoque) {
        return new ServicoDeConfirmacao(new ServicoDeCobranca(provedor), estoque);
    }

    public static void main(String[] args) {
        XPayClient provedor = new XPayClient();
        Estoque estoque = new Estoque();
        estoque.abastecer("CAMISA-M", 5);
        ServicoDeConfirmacao loja = montar(provedor, estoque);

        long id = loja.criarPedido("CAMISA-M", 2, 15980);
        System.out.println("pedido " + id + ": " + loja.confirmar(id));
        System.out.println("estoque restante: " + estoque.disponivel("CAMISA-M"));
        System.out.println("cobrancas efetivadas: " + provedor.cobrancasEfetivadas());
    }
}
