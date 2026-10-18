package br.edu.ifal.arqsw.armazenamento.nucleo;

public class ArquivoInexistenteException extends RuntimeException {
    public ArquivoInexistenteException(String caminho) {
        super("arquivo inexistente: " + caminho);
    }
}
