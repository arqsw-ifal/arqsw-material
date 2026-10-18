package br.edu.ifal.arqsw.armazenamento.plugin.local;

/** Utilitario de caminhos do plug-in local. */
public final class Caminhos {

    private Caminhos() { }

    /** Remove barras iniciais e repetidas: "/fotos//a.png" -> "fotos/a.png". */
    public static String normalizar(String caminho) {
        String limpo = caminho.replaceAll("/+", "/");
        return limpo.startsWith("/") ? limpo.substring(1) : limpo;
    }
}
