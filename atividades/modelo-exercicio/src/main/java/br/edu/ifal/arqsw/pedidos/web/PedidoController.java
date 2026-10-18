package br.edu.ifal.arqsw.pedidos.web;

import br.edu.ifal.arqsw.pedidos.dominio.ItemPedido;
import br.edu.ifal.arqsw.pedidos.dominio.Pedido;
import br.edu.ifal.arqsw.pedidos.infra.PedidoRepositoryEmMemoria;

/**
 * Camada de apresentacao. Num sistema real seria um @RestController do Spring;
 * aqui, metodos simples que recebem "requisicoes" e devolvem texto.
 *
 * CODIGO LEGADO: o controller fala direto com o repositorio global e calcula o total
 * por conta propria ("Smart UI", Evans cap. 4). Leia o README.
 */
public class PedidoController {

    private final PedidoRepositoryEmMemoria repositorio = PedidoRepositoryEmMemoria.instancia();

    public String post(String cliente) {
        Pedido pedido = new Pedido(repositorio.proximoId(), cliente);
        pedido.salvar();
        return "201 Created: pedido " + pedido.id();
    }

    public String postItem(long pedidoId, String produto, int quantidade, long precoCentavos) {
        Pedido pedido = repositorio.buscar(pedidoId).orElseThrow();
        pedido.adicionarItem(new ItemPedido(produto, quantidade, precoCentavos));
        pedido.salvar();
        long total = 0;
        for (ItemPedido item : pedido.itens()) {
            total += item.quantidade() * item.precoUnitarioCentavos();
        }
        return "200 OK: total " + formatar(total);
    }

    public String postFechar(long pedidoId) {
        Pedido pedido = repositorio.buscar(pedidoId).orElseThrow();
        pedido.fechar();
        pedido.salvar();
        return "200 OK: pedido " + pedidoId + " fechado";
    }

    private static String formatar(long centavos) {
        return String.format("R$ %d,%02d", centavos / 100, centavos % 100);
    }
}
