package br.edu.ifal.arqsw.pedidos.web;

import br.edu.ifal.arqsw.pedidos.aplicacao.ServicoDePedidos;

/**
 * Camada de apresentacao. Num sistema real seria um @RestController do Spring;
 * aqui, metodos simples que recebem "requisicoes" e devolvem texto.
 * Regra de ouro: o controller so conversa com a camada de aplicacao.
 */
public class PedidoController {

    private final ServicoDePedidos servico;

    public PedidoController(ServicoDePedidos servico) {
        this.servico = servico;
    }

    public String post(String cliente) {
        return "201 Created: pedido " + servico.criarPedido(cliente);
    }

    public String postItem(long pedidoId, String produto, int quantidade, long precoCentavos) {
        servico.adicionarItem(pedidoId, produto, quantidade, precoCentavos);
        return "200 OK: total " + formatar(servico.totalCentavos(pedidoId));
    }

    public String postFechar(long pedidoId) {
        servico.fechar(pedidoId);
        return "200 OK: pedido " + pedidoId + " fechado";
    }

    private static String formatar(long centavos) {
        return String.format("R$ %d,%02d", centavos / 100, centavos % 100);
    }
}
