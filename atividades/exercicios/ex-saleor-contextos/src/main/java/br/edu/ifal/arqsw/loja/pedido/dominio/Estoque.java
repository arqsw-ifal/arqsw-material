package br.edu.ifal.arqsw.loja.pedido.dominio;

import java.util.HashMap;
import java.util.Map;

/** Estoque disponivel por SKU (simplificado: em memoria). */
public class Estoque {

    private final Map<String, Integer> disponivel = new HashMap<>();

    public void abastecer(String sku, int quantidade) {
        disponivel.merge(sku, quantidade, Integer::sum);
    }

    public int disponivel(String sku) {
        return disponivel.getOrDefault(sku, 0);
    }

    /** Passo compensavel da saga: reserva (baixa) a quantidade. */
    public void reservar(String sku, int quantidade) {
        if (disponivel(sku) < quantidade) {
            throw new IllegalStateException("estoque insuficiente de " + sku);
        }
        disponivel.put(sku, disponivel(sku) - quantidade);
    }

    /** Compensacao da reserva. */
    public void liberar(String sku, int quantidade) {
        abastecer(sku, quantidade);
    }
}
