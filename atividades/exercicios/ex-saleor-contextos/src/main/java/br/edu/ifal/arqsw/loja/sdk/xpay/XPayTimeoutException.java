package br.edu.ifal.arqsw.loja.sdk.xpay;

/** A resposta do provedor nao chegou a tempo. A cobranca PODE ter sido feita. NAO ALTERE. */
public class XPayTimeoutException extends RuntimeException {
    public XPayTimeoutException() {
        super("XPay: read timed out");
    }
}
