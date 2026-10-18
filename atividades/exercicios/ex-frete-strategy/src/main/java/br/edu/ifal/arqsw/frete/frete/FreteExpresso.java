package br.edu.ifal.arqsw.frete.frete;

import br.edu.ifal.arqsw.frete.dominio.Dinheiro;
import br.edu.ifal.arqsw.frete.dominio.Modalidade;
import br.edu.ifal.arqsw.frete.dominio.Pedido;
import br.edu.ifal.arqsw.frete.dominio.PoliticaDeFrete;

/** Frete expresso: R$ 20,00 + R$ 3,00 por kg iniciado (Listagem 1.2). */
public class FreteExpresso implements PoliticaDeFrete {

    @Override
    public Modalidade modalidade() {
        return Modalidade.EXPRESSO;
    }

    @Override
    public Dinheiro calcular(Pedido p) {
        return Dinheiro.reais(20).mais(Dinheiro.reais(3).vezes(p.pesoKgArredondadoParaCima()));
    }
}
