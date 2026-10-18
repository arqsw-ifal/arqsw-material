package br.edu.ifal.arqsw.telemetria.regras;

import java.util.List;

/**
 * Elo de uma Chain of Responsibility (Gamma et al., 1994, cap. 5), como os nos (TbNode)
 * do motor de regras do ThingsBoard (Secao 6.7.4). Cada elo decide se a leitura segue.
 */
public abstract class Regra {

    private Regra proxima;

    /** Monta a corrente: a.entaoAplicar(b).entaoAplicar(c). Devolve o elo recebido. */
    public Regra entaoAplicar(Regra proxima) {
        this.proxima = proxima;
        return proxima;
    }

    public final void tratar(Leitura leitura, List<String> eventos) {
        boolean seguir = processar(leitura, eventos);
        // TODO (regra 1): se o elo deixou seguir e houver proximo elo, repassar a leitura a ele.
    }

    /** @return true para deixar a leitura seguir pela corrente */
    protected abstract boolean processar(Leitura leitura, List<String> eventos);
}
