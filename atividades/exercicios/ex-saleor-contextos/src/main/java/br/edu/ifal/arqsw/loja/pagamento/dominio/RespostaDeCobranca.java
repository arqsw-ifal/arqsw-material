package br.edu.ifal.arqsw.loja.pagamento.dominio;

/** Linguagem do contexto de Pagamento (como GatewayResponse no Saleor). */
public record RespostaDeCobranca(boolean aprovada, String idTransacao, long valorCentavos, String motivoRecusa) { }
