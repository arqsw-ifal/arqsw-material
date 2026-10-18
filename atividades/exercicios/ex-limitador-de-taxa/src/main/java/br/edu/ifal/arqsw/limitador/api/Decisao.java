package br.edu.ifal.arqsw.limitador.api;

/** Resultado de uma avaliacao: se a requisicao passa, quanto resta e quanto esperar. */
public record Decisao(boolean permitida, long restantes, long esperarMs) {

    public static Decisao permitir(long restantes) {
        return new Decisao(true, restantes, 0);
    }

    public static Decisao negar(long esperarMs) {
        return new Decisao(false, 0, esperarMs);
    }
}
