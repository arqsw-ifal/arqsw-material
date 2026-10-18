package br.edu.ifal.arqsw.loja.sdk.xpay;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SDK SIMULADO de um provedor de pagamentos. NAO ALTERE: faz o papel de uma biblioteca
 * externa, com o modelo DELE (charge, amountCents, declineCode...).
 *
 * Como os provedores reais (Stripe, Pix), deduplica pela chave de idempotencia: a mesma chave
 * devolve a mesma cobranca, sem cobrar de novo.
 */
public class XPayClient {

    private final Map<String, XPayCharge> porChave = new HashMap<>();
    private final List<String> chavesRecebidas = new ArrayList<>();
    private int efetivadas;
    private int respostasAPerder;
    private boolean recusar;

    /** Faz as proximas cobrancas serem recusadas ("cartao sem saldo"). */
    public void recusarCobrancas(boolean recusar) {
        this.recusar = recusar;
    }

    /** A proxima cobranca e' FEITA, mas a resposta se perde (XPayTimeoutException). */
    public void perderProximaResposta() {
        respostasAPerder++;
    }

    public XPayCharge charge(long amountCents, String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("idempotencyKey is required");
        }
        chavesRecebidas.add(idempotencyKey);
        XPayCharge charge = porChave.get(idempotencyKey);
        if (charge == null) {
            String id = "ch_" + (porChave.size() + 1);
            charge = recusar
                    ? new XPayCharge(id, "declined", amountCents, "insufficient_funds")
                    : new XPayCharge(id, "succeeded", amountCents, null);
            porChave.put(idempotencyKey, charge);
            if (!recusar) {
                efetivadas++;
            }
        }
        if (respostasAPerder > 0) {
            respostasAPerder--;
            throw new XPayTimeoutException();
        }
        return charge;
    }

    /** Quantas cobrancas o cliente final realmente pagou. */
    public int cobrancasEfetivadas() { return efetivadas; }

    /** Quantas vezes charge(...) foi chamado. */
    public int chamadas() { return chavesRecebidas.size(); }

    public List<String> chavesRecebidas() { return List.copyOf(chavesRecebidas); }
}
