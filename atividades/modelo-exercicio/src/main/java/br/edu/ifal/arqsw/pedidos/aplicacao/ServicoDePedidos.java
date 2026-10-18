package br.edu.ifal.arqsw.pedidos.aplicacao;

import br.edu.ifal.arqsw.pedidos.dominio.ItemPedido;
import br.edu.ifal.arqsw.pedidos.dominio.Pedido;
import br.edu.ifal.arqsw.pedidos.dominio.PedidoRepository;

/**
 * Camada de aplicacao: orquestra os casos de uso. Recebe o repositorio por injecao
 * de dependencia (construtor). Hoje, so os testes usam esta classe: o controller
 * "pula" esta camada. Leia o README.
 */
public class ServicoDePedidos {

    private final PedidoRepository repositorio;

    public ServicoDePedidos(PedidoRepository repositorio) {
        this.repositorio = repositorio;
    }

    public long criarPedido(String cliente) {
        Pedido pedido = new Pedido(repositorio.proximoId(), cliente);
        repositorio.salvar(pedido);
        return pedido.id();
    }

    public void adicionarItem(long pedidoId, String produto, int quantidade, long precoCentavos) {
        Pedido pedido = carregar(pedidoId);
        pedido.adicionarItem(new ItemPedido(produto, quantidade, precoCentavos));
        repositorio.salvar(pedido);
    }

    public long totalCentavos(long pedidoId) {
        return carregar(pedidoId).totalCentavos();
    }

    public void fechar(long pedidoId) {
        Pedido pedido = carregar(pedidoId);
        pedido.fechar();
        repositorio.salvar(pedido);
    }

    private Pedido carregar(long pedidoId) {
        return repositorio.buscar(pedidoId)
                .orElseThrow(() -> new IllegalArgumentException("pedido inexistente: " + pedidoId));
    }
}
