package br.edu.ifal.arqsw.telemetria.regras;

import java.util.List;

/** Filtro (como TbMsgTypeFilterNode): so deixa seguir leituras de uma grandeza. */
public final class SomenteGrandeza extends Regra {

    private final String grandeza;

    public SomenteGrandeza(String grandeza) {
        this.grandeza = grandeza;
    }

    @Override
    protected boolean processar(Leitura leitura, List<String> eventos) {
        return leitura.grandeza().equals(grandeza);
    }
}
