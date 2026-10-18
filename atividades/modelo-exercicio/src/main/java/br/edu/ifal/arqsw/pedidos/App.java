package br.edu.ifal.arqsw.pedidos;

import br.edu.ifal.arqsw.pedidos.web.PedidoController;

/**
 * Ponto de entrada. Depois da refatoracao, este deve ser o UNICO lugar que conhece
 * todas as camadas e as liga entre si (raiz de composicao).
 */
public class App {
    public static void main(String[] args) {
        var controller = new PedidoController();
        System.out.println(controller.post("Ana"));
        System.out.println(controller.postItem(1, "Teclado", 2, 15990));
        System.out.println(controller.postItem(1, "Mouse", 1, 4990));
        System.out.println(controller.postFechar(1));
    }
}
