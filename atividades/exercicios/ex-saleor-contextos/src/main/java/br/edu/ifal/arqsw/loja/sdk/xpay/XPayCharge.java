package br.edu.ifal.arqsw.loja.sdk.xpay;

/** Modelo do provedor (SDK simulado). NAO ALTERE. status: "succeeded" ou "declined". */
public record XPayCharge(String id, String status, long amountCents, String declineCode) { }
