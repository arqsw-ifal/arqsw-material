package br.edu.ifal.arqsw.loja;

import br.edu.ifal.arqsw.loja.pagamento.aplicacao.ServicoDeCobranca;
import br.edu.ifal.arqsw.loja.pagamento.dominio.ConflitoDeIdempotenciaException;
import br.edu.ifal.arqsw.loja.pagamento.dominio.RespostaDeCobranca;
import br.edu.ifal.arqsw.loja.pedido.aplicacao.ServicoDeConfirmacao;
import br.edu.ifal.arqsw.loja.pedido.dominio.Estoque;
import br.edu.ifal.arqsw.loja.pedido.dominio.StatusPedido;
import br.edu.ifal.arqsw.loja.sdk.xpay.XPayClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Testes de COMPORTAMENTO: a saga de confirmacao e a cobranca idempotente funcionam? */
class ComportamentoTest {

    private XPayClient provedor;
    private Estoque estoque;
    private ServicoDeConfirmacao loja;

    @BeforeEach
    void preparar() {
        provedor = new XPayClient();
        estoque = new Estoque();
        estoque.abastecer("CAMISA-M", 10);
        loja = App.montar(provedor, estoque);
    }

    @Test
    @DisplayName("pedido novo fica pendente")
    void pedidoNovoPendente() {
        long id = loja.criarPedido("CAMISA-M", 2, 15980);
        assertEquals(StatusPedido.PENDENTE, loja.status(id));
    }

    @Test
    @DisplayName("pagamento aprovado confirma o pedido e baixa o estoque")
    void pagamentoAprovado() {
        long id = loja.criarPedido("CAMISA-M", 2, 15980);
        assertEquals(StatusPedido.CONFIRMADO, loja.confirmar(id));
        assertEquals(8, estoque.disponivel("CAMISA-M"));
        assertEquals(1, provedor.cobrancasEfetivadas());
    }

    @Test
    @DisplayName("estoque insuficiente impede a confirmacao sem cobrar")
    void estoqueInsuficiente() {
        long id = loja.criarPedido("CAMISA-M", 11, 15980);
        assertThrows(IllegalStateException.class, () -> loja.confirmar(id));
        assertEquals(0, provedor.chamadas());
        assertEquals(10, estoque.disponivel("CAMISA-M"));
    }

    @Test
    @DisplayName("pedido inexistente gera erro")
    void pedidoInexistente() {
        assertThrows(IllegalArgumentException.class, () -> loja.confirmar(999));
    }

    @Test
    @DisplayName("pagamento recusado cancela o pedido (compensacao)")
    void recusadoCancela() {
        provedor.recusarCobrancas(true);
        long id = loja.criarPedido("CAMISA-M", 2, 15980);
        assertEquals(StatusPedido.CANCELADO, assertDoesNotThrow(() -> loja.confirmar(id)));
        assertEquals(StatusPedido.CANCELADO, loja.status(id));
    }

    @Test
    @DisplayName("pagamento recusado devolve o estoque reservado (compensacao)")
    void recusadoDevolveEstoque() {
        provedor.recusarCobrancas(true);
        long id = loja.criarPedido("CAMISA-M", 2, 15980);
        try {
            loja.confirmar(id);
        } catch (RuntimeException ignorada) {
            // o teste verifica apenas o estoque
        }
        assertEquals(10, estoque.disponivel("CAMISA-M"));
    }

    @Test
    @DisplayName("nova tentativa apos tempo limite nao cobra duas vezes")
    void tempoLimiteNaoDuplica() {
        provedor.perderProximaResposta();
        long id = loja.criarPedido("CAMISA-M", 2, 15980);
        assertEquals(StatusPedido.CONFIRMADO, loja.confirmar(id));
        assertEquals(1, provedor.cobrancasEfetivadas());
    }

    @Test
    @DisplayName("confirmar duas vezes o mesmo pedido nao cobra de novo")
    void confirmarDuasVezes() {
        long id = loja.criarPedido("CAMISA-M", 2, 15980);
        loja.confirmar(id);
        assertEquals(StatusPedido.CONFIRMADO, loja.confirmar(id));
        assertEquals(1, provedor.cobrancasEfetivadas());
        assertEquals(8, estoque.disponivel("CAMISA-M"));
    }

    @Test
    @DisplayName("mesma chave e mesmo valor devolvem o resultado guardado sem chamar o provedor")
    void mesmaChaveMesmoResultado() {
        ServicoDeCobranca cobranca = new ServicoDeCobranca(provedor);
        RespostaDeCobranca primeira = cobranca.cobrar("pedido-42", 5000);
        RespostaDeCobranca repeticao = cobranca.cobrar("pedido-42", 5000);
        assertEquals(primeira, repeticao);
        assertEquals(1, provedor.chamadas());
    }

    @Test
    @DisplayName("mesma chave com outro valor gera conflito de idempotencia")
    void mesmaChaveOutroValor() {
        ServicoDeCobranca cobranca = new ServicoDeCobranca(provedor);
        cobranca.cobrar("pedido-42", 5000);
        assertThrows(ConflitoDeIdempotenciaException.class, () -> cobranca.cobrar("pedido-42", 9000));
    }
}
