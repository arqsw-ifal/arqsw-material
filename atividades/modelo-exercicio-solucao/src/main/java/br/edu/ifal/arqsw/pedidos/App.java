package br.edu.ifal.arqsw.pedidos;

import br.edu.ifal.arqsw.pedidos.aplicacao.ServicoDePedidos;
import br.edu.ifal.arqsw.pedidos.infra.PedidoRepositoryEmMemoria;
import br.edu.ifal.arqsw.pedidos.web.PedidoController;

/**
 * Raiz de composicao (composition root): o UNICO lugar que conhece todas as camadas
 * e as "liga" entre si. Num projeto Spring, quem faz isso e' o container de IoC.
 */
public class App {
    public static void main(String[] args) {
        var controller = new PedidoController(new ServicoDePedidos(new PedidoRepositoryEmMemoria()));
        System.out.println(controller.post("Ana"));
        System.out.println(controller.postItem(1, "Teclado", 2, 15990));
        System.out.println(controller.postItem(1, "Mouse", 1, 4990));
        System.out.println(controller.postFechar(1));
    }
}
