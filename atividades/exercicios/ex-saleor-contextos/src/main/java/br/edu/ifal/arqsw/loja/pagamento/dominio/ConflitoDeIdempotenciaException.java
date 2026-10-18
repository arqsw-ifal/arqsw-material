package br.edu.ifal.arqsw.loja.pagamento.dominio;

/** A mesma chave de idempotencia foi reutilizada com parametros diferentes. */
public class ConflitoDeIdempotenciaException extends RuntimeException {
    public ConflitoDeIdempotenciaException(String chave) {
        super("chave de idempotencia reutilizada com outros parametros: " + chave);
    }
}
