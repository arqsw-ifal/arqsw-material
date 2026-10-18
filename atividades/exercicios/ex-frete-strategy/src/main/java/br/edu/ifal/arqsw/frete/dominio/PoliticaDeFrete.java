package br.edu.ifal.arqsw.frete.dominio;

/** A estrategia (Strategy): cada modalidade de frete e' uma implementacao desta interface. */
public interface PoliticaDeFrete {

    /** Modalidade atendida por esta politica. */
    Modalidade modalidade();

    /** Valor do frete para o pedido. */
    Dinheiro calcular(Pedido pedido);
}
