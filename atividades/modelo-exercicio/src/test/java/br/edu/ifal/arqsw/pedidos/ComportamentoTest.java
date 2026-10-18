package br.edu.ifal.arqsw.pedidos;

import br.edu.ifal.arqsw.pedidos.aplicacao.ServicoDePedidos;
import br.edu.ifal.arqsw.pedidos.infra.PedidoRepositoryEmMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Testes de COMPORTAMENTO: o sistema faz o que deve fazer? */
class ComportamentoTest {

    private ServicoDePedidos servico;

    @BeforeEach
    void preparar() {
        servico = new ServicoDePedidos(new PedidoRepositoryEmMemoria());
    }

    @Test
    @DisplayName("cria pedidos com ids distintos")
    void criaPedidos() {
        long a = servico.criarPedido("Ana");
        long b = servico.criarPedido("Bruno");
        assertNotEquals(a, b);
    }

    @Test
    @DisplayName("pedido novo tem total zero")
    void totalZero() {
        long id = servico.criarPedido("Ana");
        assertEquals(0, servico.totalCentavos(id));
    }

    @Test
    @DisplayName("total soma quantidade x preco de cada item")
    void totalSomaItens() {
        long id = servico.criarPedido("Ana");
        servico.adicionarItem(id, "Teclado", 2, 15990);
        servico.adicionarItem(id, "Mouse", 1, 4990);
        assertEquals(2 * 15990 + 4990, servico.totalCentavos(id));
    }

    @Test
    @DisplayName("quantidade nao positiva e' rejeitada")
    void quantidadeInvalida() {
        long id = servico.criarPedido("Ana");
        assertThrows(IllegalArgumentException.class, () -> servico.adicionarItem(id, "Mouse", 0, 4990));
    }

    @Test
    @DisplayName("pedido vazio nao pode ser fechado")
    void naoFechaVazio() {
        long id = servico.criarPedido("Ana");
        assertThrows(IllegalStateException.class, () -> servico.fechar(id));
    }

    @Test
    @DisplayName("pedido fechado nao aceita novos itens")
    void fechadoNaoAceitaItens() {
        long id = servico.criarPedido("Ana");
        servico.adicionarItem(id, "Mouse", 1, 4990);
        servico.fechar(id);
        assertThrows(IllegalStateException.class, () -> servico.adicionarItem(id, "Cabo", 1, 990));
    }

    @Test
    @DisplayName("pedido inexistente gera erro")
    void pedidoInexistente() {
        assertThrows(IllegalArgumentException.class, () -> servico.totalCentavos(999));
    }
}
