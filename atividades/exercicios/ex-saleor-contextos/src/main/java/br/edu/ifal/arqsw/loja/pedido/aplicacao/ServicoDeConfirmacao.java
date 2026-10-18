package br.edu.ifal.arqsw.loja.pedido.aplicacao;

import br.edu.ifal.arqsw.loja.pagamento.aplicacao.ServicoDeCobranca;
import br.edu.ifal.arqsw.loja.pagamento.dominio.RespostaDeCobranca;
import br.edu.ifal.arqsw.loja.pedido.dominio.Estoque;
import br.edu.ifal.arqsw.loja.pedido.dominio.Pedido;
import br.edu.ifal.arqsw.loja.pedido.dominio.StatusPedido;
import br.edu.ifal.arqsw.loja.sdk.xpay.XPayTimeoutException;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Caso de uso "confirmar pedido": uma saga em processo, como complete_checkout no Saleor
 * (Secao 6.6.2): reservar estoque (compensavel) -> cobrar (pivo) -> confirmar (retentavel).
 *
 * CODIGO LEGADO: chama o contexto de Pagamento diretamente, conhece excecoes do SDK do
 * provedor e gera uma chave de idempotencia NOVA a cada tentativa. Leia o README.
 */
public class ServicoDeConfirmacao {

    private final ServicoDeCobranca cobranca;
    private final Estoque estoque;
    private final Map<Long, Pedido> pedidos = new HashMap<>();
    private long sequencia = 0;

    public ServicoDeConfirmacao(ServicoDeCobranca cobranca, Estoque estoque) {
        this.cobranca = cobranca;
        this.estoque = estoque;
    }

    public long criarPedido(String sku, int quantidade, long valorCentavos) {
        Pedido pedido = new Pedido(++sequencia, sku, quantidade, valorCentavos);
        pedidos.put(pedido.id(), pedido);
        return pedido.id();
    }

    public StatusPedido status(long pedidoId) {
        return carregar(pedidoId).status();
    }

    public StatusPedido confirmar(long pedidoId) {
        Pedido pedido = carregar(pedidoId);
        // TODO (regra 3): confirmar um pedido JA confirmado nao faz nada (nem reserva, nem cobra).

        estoque.reservar(pedido.sku(), pedido.quantidade());

        RespostaDeCobranca resposta = null;
        String ultimaFalha = null;
        for (int tentativa = 0; tentativa < 3 && resposta == null; tentativa++) {
            try {
                resposta = cobranca.cobrar(UUID.randomUUID().toString(), pedido.valorCentavos());
            } catch (XPayTimeoutException e) {
                ultimaFalha = e.getMessage();   // a resposta se perdeu: tenta de novo
            }
        }
        if (resposta == null) {
            throw new IllegalStateException("provedor indisponivel: " + ultimaFalha);
        }
        if (!resposta.aprovada()) {
            // TODO (regra 1 e 2): compensar -- devolver o estoque reservado, cancelar o pedido
            //                     e devolver CANCELADO, sem lancar excecao.
            throw new IllegalStateException("pagamento recusado: " + resposta.motivoRecusa());
        }
        pedido.registrarPagamento(resposta);
        return pedido.status();
    }

    private Pedido carregar(long pedidoId) {
        Pedido pedido = pedidos.get(pedidoId);
        if (pedido == null) {
            throw new IllegalArgumentException("pedido inexistente: " + pedidoId);
        }
        return pedido;
    }
}
