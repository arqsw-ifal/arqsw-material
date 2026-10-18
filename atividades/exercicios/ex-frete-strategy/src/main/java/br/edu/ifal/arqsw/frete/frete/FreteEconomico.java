package br.edu.ifal.arqsw.frete.frete;

import br.edu.ifal.arqsw.frete.dominio.Dinheiro;
import br.edu.ifal.arqsw.frete.dominio.Modalidade;
import br.edu.ifal.arqsw.frete.dominio.Pedido;
import br.edu.ifal.arqsw.frete.dominio.PoliticaDeFrete;

/** Frete economico: R$ 10,00 + R$ 1,50 por kg iniciado. */
public class FreteEconomico implements PoliticaDeFrete {

    @Override
    public Modalidade modalidade() {
        return Modalidade.ECONOMICO;
    }

    @Override
    public Dinheiro calcular(Pedido p) {
        // TODO (refatoracao): trazer para ca' a regra que hoje esta no switch do Checkout.
        throw new UnsupportedOperationException("ainda nao implementado");
    }
}
